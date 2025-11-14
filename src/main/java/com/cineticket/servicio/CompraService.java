package com.cineticket.servicio;

import com.cineticket.dao.CompraConfiteriaDAO;
import com.cineticket.dao.CompraDAO;
import com.cineticket.dao.EntradaDAO;
import com.cineticket.dao.FuncionDAO;
import com.cineticket.enums.EstadoCompra;
import com.cineticket.enums.EstadoEntrada;
import com.cineticket.enums.MetodoPago;
import com.cineticket.excepcion.ValidacionException;
import com.cineticket.modelo.Compra;
import com.cineticket.modelo.CompraConfiteria;
import com.cineticket.modelo.Entrada;
import com.cineticket.modelo.Funcion;
import com.cineticket.servicio.dto.CompraPreparada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class CompraService {

    private static final Logger log = LoggerFactory.getLogger(CompraService.class);
    private final CompraDAO compraDAO;
    private final EntradaDAO entradaDAO;
    private final CompraConfiteriaDAO compraConfiteriaDAO;
    private final FuncionDAO funcionDAO;
    private final ReservaService reservaService;
    private final ConfiteriaService confiteriaService;
    private final PDFService pdfService;

    public CompraService(CompraDAO compraDAO,
                         EntradaDAO entradaDAO,
                         CompraConfiteriaDAO compraConfiteriaDAO,
                         FuncionDAO funcionDAO,
                         ReservaService reservaService,
                         ConfiteriaService confiteriaService,
                         PDFService pdfService) {
        this.compraDAO = Objects.requireNonNull(compraDAO);
        this.entradaDAO = Objects.requireNonNull(entradaDAO);
        this.compraConfiteriaDAO = Objects.requireNonNull(compraConfiteriaDAO);
        this.funcionDAO = Objects.requireNonNull(funcionDAO);
        this.reservaService = Objects.requireNonNull(reservaService);
        this.confiteriaService = Objects.requireNonNull(confiteriaService);
        this.pdfService = Objects.requireNonNull(pdfService);
        log.debug("CompraService inicializado");
    }

    /** Arma el paquete compra+entradas+combos (aún sin persistir) y calcula totales. */
    public CompraPreparada crearCompra(Integer usuarioId,
                                       Integer funcionId,
                                       List<Integer> asientoIds,
                                       Map<Integer, Integer> combos,
                                       MetodoPago metodoPago) {

        log.debug("Creando compra preparada: usuarioId={}, funcionId={}, asientos={}, combos={}",
                usuarioId,
                funcionId,
                asientoIds != null ? asientoIds.size() : 0,
                combos != null ? combos.size() : 0);

        if (usuarioId == null) throw new ValidacionException("usuarioId es requerido.");
        if (funcionId == null) throw new ValidacionException("funcionId es requerido.");
        if (metodoPago == null) throw new ValidacionException("metodoPago es requerido.");

        // Defensa adicional: máximo 5 asientos
        if (asientoIds == null || asientoIds.isEmpty()) {
            log.warn("Intento de compra sin asientos para usuario {}", usuarioId);
            throw new ValidacionException("Debe seleccionar al menos 1 asiento.");
        }
        if (asientoIds.size() > 5) {
            log.warn("Intento de compra con {} asientos (máximo permitido: 5) para usuario {}",
                    asientoIds.size(), usuarioId);
            throw new ValidacionException("Máximo 5 asientos por transacción.");
        }

        // 1) Verificamos que la función exista
        Funcion f = funcionDAO.buscarPorId(funcionId);
        if (f == null) {
            log.warn("Función {} no encontrada al crear compra para usuario {}", funcionId, usuarioId);
            throw new ValidacionException("Función no encontrada.");
        }

        // 2) Pre-entradas (no persistidas), usando ReservaService
        List<Entrada> entradas = reservaService.reservarAsientos(funcionId, asientoIds);
        entradas.forEach(e -> e.setEstadoEntrada(EstadoEntrada.ACTIVA));

        // 3) Items de confitería (no persistidos)
        List<CompraConfiteria> items = new ArrayList<>();
        if (combos != null && !combos.isEmpty()) {
            for (var entry : combos.entrySet()) {
                Integer comboId = entry.getKey();
                Integer cantidad = entry.getValue();
                // Validamos precio/disp. leyendo ConfiteriaService
                confiteriaService.calcularSubtotal(comboId, cantidad);
                var combo = confiteriaService.obtenerCombo(comboId);

                CompraConfiteria ci = new CompraConfiteria();
                ci.setComboId(combo.getIdCombo());
                ci.setCantidad(cantidad);
                ci.setPrecioUnitario(combo.getPrecio());
                ci.recalcularSubtotal();
                items.add(ci);
            }
        }

        // 4) Crear el objeto Compra (solo totales y metadatos)
        Compra compra = new Compra();
        compra.setUsuarioId(usuarioId);
        compra.setFechaHoraCompra(LocalDateTime.now());
        compra.setMetodoPago(metodoPago);
        compra.setEstadoCompra(EstadoCompra.CONFIRMADA); // o PENDIENTE si prefieres
        compra.setTotalEntradas(BigDecimal.ZERO);
        compra.setTotalConfiteria(BigDecimal.ZERO);

        // 5) Calcular totales en memoria
        calcularTotales(compra, entradas, items);

        log.info("Compra preparada para usuario {} en función {}: entradas={}, combos={}, totalEntradas={}, totalConfiteria={}",
                usuarioId,
                funcionId,
                entradas.size(),
                items.size(),
                compra.getTotalEntradas(),
                compra.getTotalConfiteria());

        return new CompraPreparada(compra, entradas, items);
    }

    /** Confirma la compra en una TRANSACCIÓN: compra → entradas → confitería → PDF → update ruta. */
    public Integer confirmarCompra(CompraPreparada preparada) {
        if (preparada == null) throw new ValidacionException("Compra preparada es requerida.");

        Compra compra = preparada.getCompra();
        List<Entrada> entradas = preparada.getEntradas();
        List<CompraConfiteria> items = preparada.getItemsConfiteria();

        if (entradas == null || entradas.isEmpty()) {
            log.warn("Intento de confirmar compra sin entradas. compraId temporal={}", compra.getIdCompra());
            throw new ValidacionException("La compra no contiene entradas.");
        }

        Integer funcionId = entradas.get(0).getFuncionId();
        log.info("Confirmando compra preparada: usuarioId={}, funcionId={}, entradas={}, combos={}",
                compra.getUsuarioId(),
                funcionId,
                entradas.size(),
                items != null ? items.size() : 0);

        // 1) Persistir compra (ID generado)
        Integer compraId = compraDAO.crear(compra);

        // 2) Persistir cada entrada con ese compraId
        for (Entrada e : entradas) {
            e.setCompraId(compraId);
            e.setEstadoEntrada(EstadoEntrada.ACTIVA);
            entradaDAO.crear(e);
        }

        // 3) Persistir confitería (si hay)
        if (items != null && !items.isEmpty()) {
            for (CompraConfiteria ci : items) {
                ci.setCompraId(compraId);
                compraConfiteriaDAO.crear(ci);
            }
        }

        // 4) Generar comprobante (PDF real) y actualizar ruta
        Map<String, Object> extra = construirExtrasParaPDF(compra, entradas);
        String ruta = pdfService.generarComprobantePDF(compra, entradas, items, extra);
        compra.setRutaComprobantePdf(ruta);
        compraDAO.actualizar(compra);

        log.info("Compra {} confirmada correctamente para usuario {}. PDF en '{}'",
                compraId, compra.getUsuarioId(), ruta);


        return compraId;
    }

    /** Cancela una compra CONFIRMADA si la función aún no ha iniciado.
     *  Efectos en BD (atómico si tus DAO comparten la misma conexión/tx):
     *   - compra: estado_compra=CANCELADA, fecha_cancelacion=now()
     *   - entradas: estado_entrada=CANCELADA
     *  Retorna true si se aplicaron cambios.
     */
    public boolean cancelarCompra(Integer compraId) {
        if (compraId == null) throw new ValidacionException("compraId requerido.");

        log.info("Solicitud de cancelación para compra {}", compraId);

        // 1) Cargar compra
        Compra compra = compraDAO.buscarPorId(compraId);
        if (compra == null) {
            log.warn("Cancelación fallida: compra {} no encontrada", compraId);
            throw new ValidacionException("Compra no encontrada.");
        }

        // 2) Cargar entradas y función asociada
        List<Entrada> entradas = entradaDAO.listarPorCompra(compraId);
        if (entradas == null || entradas.isEmpty()) {
            // No hay entradas (caso raro). Sólo permitir si sigue CONFIRMADA.
            log.warn("Compra {} sin entradas asociadas al intentar cancelar", compraId);

            if (!compra.estaConfirmada()) {
                throw new ValidacionException("La compra no está en estado CONFIRMADA.");
            }
            // Cancelar sólo la compra.
            compra.setEstadoCompra(EstadoCompra.CANCELADA);
            compra.setFechaCancelacion(LocalDateTime.now());
            boolean ok = compraDAO.actualizar(compra);
            if (ok) {
                log.info("Compra {} cancelada (sin entradas asociadas)", compraId);
            } else {
                log.warn("No se pudo actualizar el estado de compra {} a CANCELADA", compraId);
            }
            return ok;
        }

        Integer funcionId = entradas.get(0).getFuncionId();
        if (funcionId == null) throw new ValidacionException("Compra sin función asociada.");
        Funcion funcion = funcionDAO.buscarPorId(funcionId);
        if (funcion == null) {
            log.warn("Función {} asociada a compra {} no encontrada", funcionId, compraId);
            throw new ValidacionException("Función asociada no encontrada.");
        }

        // 3) Reglas de negocio
        validarCompraCancelable(compra, funcion);

        // 4) Persistir cambios (idealmente dentro de una misma transacción)
        // 4.1 Compra -> CANCELADA
        compra.setEstadoCompra(EstadoCompra.CANCELADA);
        compra.setFechaCancelacion(LocalDateTime.now());
        boolean okCompra = compraDAO.actualizar(compra);

        // 4.2 Entradas -> CANCELADA (bulk)
        boolean okEntradas = entradaDAO.cancelarEntradasDeCompra(compraId);

        if (!okCompra || !okEntradas) {
            log.error("Falló la cancelación completa de la compra {} (okCompra={}, okEntradas={})",
                    compraId, okCompra, okEntradas);
            throw new ValidacionException("No fue posible cancelar completamente la compra.");
        }
        log.info("Compra {} cancelada correctamente. Entradas asociadas marcadas como CANCELADAS", compraId);
        return true;
    }

    /**
     * Devuelve el historial de compras de un usuario.
     * Se espera que el DAO la devuelva ordenada por fecha DESC,
     * pero aquí aplicamos un orden defensivo por si acaso.
     */
    public List<Compra> obtenerHistorialCompras(Integer usuarioId) {
        if (usuarioId == null) {
            throw new ValidacionException("usuarioId es requerido para consultar el historial.");
        }

        log.debug("Consultando historial de compras para usuario {}", usuarioId);

        List<Compra> compras = compraDAO.listarPorUsuario(usuarioId);
        if (compras == null || compras.isEmpty()) {
            log.info("Usuario {} no tiene compras registradas", usuarioId);
            return compras; // puede ser null o lista vacía, como prefieras
        }

        // Copia mutable para poder ordenar aunque el DAO devuelva una lista inmutable
        List<Compra> resultado = new ArrayList<>(compras);

        resultado.sort(
                java.util.Comparator.comparing(
                        Compra::getFechaHoraCompra,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())
                ).reversed()
        );

        log.info("Historial de compras obtenido para usuario {}: {} registros",
                usuarioId, resultado.size());

        return resultado;
    }



    /** Regla: sólo compras CONFIRMADAS y con función futura se pueden cancelar. */
    private void validarCompraCancelable(Compra compra, Funcion funcion) {
        if (!compra.estaConfirmada()) {
            log.warn("Intento de cancelar compra {} que no está CONFIRMADA (estado actual={})",
                    compra.getIdCompra(), compra.getEstadoCompra());
            throw new ValidacionException("La compra ya fue cancelada o no está CONFIRMADA.");
        }
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime inicio = funcion.getFechaHoraInicio();
        if (inicio == null) {
            log.warn("Función {} no tiene fecha de inicio definida al validar cancelación", funcion.getIdFuncion());
            throw new ValidacionException("La función no tiene fecha de inicio definida.");
        }
        if (!ahora.isBefore(inicio)) {
            log.warn("Compra {} no se puede cancelar: función {} ya inició o finalizó (inicio={})",
                    compra.getIdCompra(), funcion.getIdFuncion(), inicio);
            throw new ValidacionException("La función ya inició o finalizó; no se puede cancelar.");
        }
    }


    /** Util: suma totales y deja totalGeneral para que lo calcule la BD (STORED). */
    void calcularTotales(Compra compra, List<Entrada> entradas, List<CompraConfiteria> items) {
        BigDecimal totalEntradas = entradas.stream()
                .map(Entrada::getPrecioUnitario)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalConf = (items == null ? BigDecimal.ZERO
                : items.stream()
                .map(CompraConfiteria::getSubtotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        compra.setTotalEntradas(totalEntradas);
        compra.setTotalConfiteria(totalConf);

        log.debug("Totales calculados para compra temporal (usuarioId={}): totalEntradas={}, totalConfiteria={}",
                compra.getUsuarioId(), totalEntradas, totalConf);
        // totalGeneral lo rellena la BD (columna generated) cuando se re-hidrate
    }

    /** Genera el PDF nuevamente para una compra ya confirmada (botón “re-descargar”). */
    public String generarComprobante(Integer compraId) {
        if (compraId == null) throw new ValidacionException("compraId requerido.");

        log.info("Generando comprobante PDF para compra {}", compraId);

        var compra = compraDAO.buscarPorId(compraId);
        if (compra == null) throw new ValidacionException("Compra no encontrada.");

        var entradas = entradaDAO.listarPorCompra(compraId);
        var items = compraConfiteriaDAO.listarPorCompra(compraId);

        Map<String, Object> extra = construirExtrasParaPDF(compra, entradas);
        String ruta = pdfService.generarComprobantePDF(compra, entradas, items, extra);
        compra.setRutaComprobantePdf(ruta);
        compraDAO.actualizar(compra);

        log.info("Comprobante PDF regenerado para compra {} en ruta '{}'", compraId, ruta);

        return ruta;
    }

    // ================== Helpers ==================

    /** Arma un pequeño mapa con datos legibles para el PDF (función legible, cliente si lo tienes, etc.). */
    private Map<String, Object> construirExtrasParaPDF(Compra compra, List<Entrada> entradas) {
        Map<String, Object> extra = new HashMap<>();

        if (entradas != null && !entradas.isEmpty()) {
            Integer funcionId = entradas.get(0).getFuncionId();
            if (funcionId != null) {
                Funcion f = funcionDAO.buscarPorId(funcionId);
                if (f != null) {
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                    String funcionTexto = "Función " + f.getIdFuncion() +
                            " | Sala " + f.getSalaId() +
                            " | " + f.getFechaHoraInicio().format(dtf);
                    extra.put("funcionTexto", funcionTexto);
                    log.debug("Extras para PDF de compra {}: {}", compra.getIdCompra(), funcionTexto);
                }
            }
        }
        return extra;
    }

    /** Devuelve las entradas asociadas a una compra. */
    public List<Entrada> obtenerEntradasDeCompra(Integer compraId) {
        if (compraId == null) {
            throw new ValidacionException("compraId es requerido para consultar las entradas de la compra.");
        }
        log.debug("Consultando entradas de compra {}", compraId);
        return entradaDAO.listarPorCompra(compraId);
    }

    /** Devuelve los ítems de confitería asociados a una compra. */
    public List<CompraConfiteria> obtenerConfiteriaDeCompra(Integer compraId) {
        if (compraId == null) {
            throw new ValidacionException("compraId es requerido para consultar la confitería de la compra.");
        }
        log.debug("Consultando confitería de compra {}", compraId);
        return compraConfiteriaDAO.listarPorCompra(compraId);
    }

}
