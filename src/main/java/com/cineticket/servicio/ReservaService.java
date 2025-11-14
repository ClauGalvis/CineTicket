package com.cineticket.servicio;

import com.cineticket.dao.EntradaDAO;
import com.cineticket.dao.FuncionDAO;
import com.cineticket.excepcion.AsientoNoDisponibleException;
import com.cineticket.excepcion.ValidacionException;
import com.cineticket.modelo.Entrada;
import com.cineticket.modelo.Funcion;
import com.cineticket.enums.EstadoFuncion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.math.BigDecimal;
import java.util.*;

public class ReservaService {

    private static final Logger log = LoggerFactory.getLogger(ReservaService.class);

    private static final int MAX_ENTRADAS = 5;

    private final EntradaDAO entradaDAO;
    private final FuncionDAO funcionDAO;

    public ReservaService(EntradaDAO entradaDAO, FuncionDAO funcionDAO) {
        this.entradaDAO = Objects.requireNonNull(entradaDAO);
        this.funcionDAO = Objects.requireNonNull(funcionDAO);
        log.debug("ReservaService inicializado");
    }

    /** IDs de asientos ya ocupados (entradas ACTIVA) para una función. */
    public List<Integer> obtenerAsientosOcupadosPorFuncion(Integer funcionId) {
        if (funcionId == null) throw new ValidacionException("funcionId requerido.");

        log.debug("Obteniendo asientos ocupados para función {}", funcionId);

        List<Integer> ocupados = entradaDAO.listarPorFuncion(funcionId).stream()
                .filter(Entrada::estaActiva)
                .map(Entrada::getAsientoId)
                .toList();

        log.info("Función {} tiene {} asientos ocupados", funcionId, ocupados.size());
        return ocupados;
    }

    /** Verifica que todos los asientos indiquen disponibilidad (ninguna ACTIVA en BD). */
    public boolean verificarDisponibilidadAsientos(Integer funcionId, List<Integer> asientoIds) {
        validarEntradaBasica(funcionId, asientoIds);

        log.debug("Verificando disponibilidad de {} asientos para función {}",
                asientoIds.size(), funcionId);

        for (Integer asientoId : asientoIds) {
            boolean libre = entradaDAO.verificarAsientoDisponible(funcionId, asientoId);
            if (!libre) return false;
        }

        log.info("Todos los asientos solicitados están disponibles para función {}", funcionId);
        return true;
    }

    /**
     * Crea objetos Entrada en memoria para confirmar luego en CompraService.
     * Reglas: máximo 5; no duplicados; la función debe estar PROGRAMADA; cada asiento debe estar libre.
     */
    public List<Entrada> reservarAsientos(Integer funcionId, List<Integer> asientoIds) {
        validarEntradaBasica(funcionId, asientoIds);
        if (asientoIds.size() > MAX_ENTRADAS) {
            log.warn("Intento de reservar {} asientos para función {} (máx={})",
                    asientoIds.size(), funcionId, MAX_ENTRADAS);
            throw new ValidacionException("Máximo " + MAX_ENTRADAS + " asientos por transacción.");
        }

        Funcion f = funcionDAO.buscarPorId(funcionId);
        if (f == null) {
            log.warn("Función {} no encontrada al reservar asientos", funcionId);
            throw new ValidacionException("Función no encontrada.");
        }

        if (f.getEstado() != null && f.getEstado() != EstadoFuncion.PROGRAMADA) {
            log.warn("Intento de reservar en función {} con estado {}", funcionId, f.getEstado());
            throw new ValidacionException("La función no admite reservas (estado: " + f.getEstado() + ").");
        }

        log.info("Reservando {} asientos para función {}", asientoIds.size(), funcionId);

        // Evitar duplicados en la selección
        Set<Integer> vistos = new HashSet<>();
        List<Entrada> result = new ArrayList<>(asientoIds.size());

        for (Integer asientoId : asientoIds) {
            if (!vistos.add(asientoId)) {
                log.warn("Asiento repetido en la selección: {} (función {})", asientoId, funcionId);
                throw new ValidacionException("Asiento repetido en la selección: " + asientoId);
            }
            if (!entradaDAO.verificarAsientoDisponible(funcionId, asientoId)) {
                log.info("Asiento {} no disponible al intentar reservar para función {}", asientoId, funcionId);
                throw new AsientoNoDisponibleException("Asiento no disponible: " + asientoId);
            }

            Entrada e = new Entrada();
            e.setFuncionId(funcionId);
            e.setAsientoId(asientoId);

            // Precio de la función viene como Double (ver FuncionDAOImpl)
            Double precioEntrada = f.getPrecioEntrada();
            if (precioEntrada == null) {
                log.error("Función {} no tiene precio configurado al reservar asientos", funcionId);
                throw new ValidacionException("La función no tiene precio configurado.");
            }
            e.setPrecioUnitario(java.math.BigDecimal.valueOf(precioEntrada));

            // Estado queda por definir al persistir (ACTIVA), aquí no se persiste aún
            result.add(e);
        }

        log.info("Se prepararon {} entradas en memoria para función {}",
                result.size(), funcionId);

        return result;
    }

    // --- helpers ---
    private void validarEntradaBasica(Integer funcionId, List<Integer> asientoIds) {
        if (funcionId == null) throw new ValidacionException("funcionId requerido.");
        if (asientoIds == null || asientoIds.isEmpty()) {
            log.warn("Intento de operación de reserva/verificación sin asientos para función {}",
                    funcionId);
            throw new ValidacionException("Debe seleccionar al menos 1 asiento.");
        }
    }
}
