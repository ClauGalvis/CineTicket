package com.cineticket.servicio;

import com.cineticket.dao.EntradaDAO;
import com.cineticket.dao.FuncionDAO;
import com.cineticket.excepcion.AsientoNoDisponibleException;
import com.cineticket.excepcion.ValidacionException;
import com.cineticket.modelo.Entrada;
import com.cineticket.modelo.Funcion;
import com.cineticket.enums.EstadoFuncion;

import java.math.BigDecimal;
import java.util.*;

public class ReservaService {

    private static final int MAX_ENTRADAS = 5;

    private final EntradaDAO entradaDAO;
    private final FuncionDAO funcionDAO;

    public ReservaService(EntradaDAO entradaDAO, FuncionDAO funcionDAO) {
        this.entradaDAO = Objects.requireNonNull(entradaDAO);
        this.funcionDAO = Objects.requireNonNull(funcionDAO);
    }

    /** IDs de asientos ya ocupados (entradas ACTIVA) para una función. */
    public List<Integer> obtenerAsientosOcupadosPorFuncion(Integer funcionId) {
        if (funcionId == null) throw new ValidacionException("funcionId requerido.");
        return entradaDAO.listarPorFuncion(funcionId).stream()
                .filter(Entrada::estaActiva)            // tu VO expone este helper
                .map(Entrada::getAsientoId)
                .toList();
    }

    /** Verifica que todos los asientos indiquen disponibilidad (ninguna ACTIVA en BD). */
    public boolean verificarDisponibilidadAsientos(Integer funcionId, List<Integer> asientoIds) {
        validarEntradaBasica(funcionId, asientoIds);
        for (Integer asientoId : asientoIds) {
            boolean libre = entradaDAO.verificarAsientoDisponible(funcionId, asientoId);
            if (!libre) return false;
        }
        return true;
    }

    /**
     * Crea objetos Entrada en memoria para confirmar luego en CompraService.
     * Reglas: máximo 5; no duplicados; la función debe estar PROGRAMADA; cada asiento debe estar libre.
     */
    public List<Entrada> reservarAsientos(Integer funcionId, List<Integer> asientoIds) {
        validarEntradaBasica(funcionId, asientoIds);
        if (asientoIds.size() > MAX_ENTRADAS) {
            throw new ValidacionException("Máximo " + MAX_ENTRADAS + " asientos por transacción.");
        }

        Funcion f = funcionDAO.buscarPorId(funcionId);
        if (f == null) throw new ValidacionException("Función no encontrada.");
        if (f.getEstado() != null && f.getEstado() != EstadoFuncion.PROGRAMADA) {
            throw new ValidacionException("La función no admite reservas (estado: " + f.getEstado() + ").");
        }

        // Evitar duplicados en la selección
        Set<Integer> vistos = new HashSet<>();
        List<Entrada> result = new ArrayList<>(asientoIds.size());

        for (Integer asientoId : asientoIds) {
            if (!vistos.add(asientoId)) {
                throw new ValidacionException("Asiento repetido en la selección: " + asientoId);
            }
            if (!entradaDAO.verificarAsientoDisponible(funcionId, asientoId)) {
                throw new AsientoNoDisponibleException("Asiento no disponible: " + asientoId);
            }

            Entrada e = new Entrada();
            e.setFuncionId(funcionId);
            e.setAsientoId(asientoId);

            // Precio de la función viene como Double (ver FuncionDAOImpl)
            Double precioEntrada = f.getPrecioEntrada();
            if (precioEntrada == null) {
                throw new ValidacionException("La función no tiene precio configurado.");
            }
            e.setPrecioUnitario(java.math.BigDecimal.valueOf(precioEntrada));

            // Estado queda por definir al persistir (ACTIVA), aquí no se persiste aún
            result.add(e);
        }
        return result;
    }

    // --- helpers ---
    private void validarEntradaBasica(Integer funcionId, List<Integer> asientoIds) {
        if (funcionId == null) throw new ValidacionException("funcionId requerido.");
        if (asientoIds == null || asientoIds.isEmpty()) {
            throw new ValidacionException("Debe seleccionar al menos 1 asiento.");
        }
    }
}
