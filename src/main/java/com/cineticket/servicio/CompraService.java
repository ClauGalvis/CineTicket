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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CompraService {

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
    }

    /** Arma el paquete compra+entradas+combos (aún sin persistir) y calcula totales. */
    public CompraPreparada crearCompra(Integer usuarioId,
                                       Integer funcionId,
                                       List<Integer> asientoIds,
                                       Map<Integer, Integer> combos,
                                       MetodoPago metodoPago) {

        if (usuarioId == null) throw new ValidacionException("usuarioId es requerido.");
        if (funcionId == null) throw new ValidacionException("funcionId es requerido.");
        if (metodoPago == null) throw new ValidacionException("metodoPago es requerido.");

        // Defensa adicional: máximo 5 asientos
        if (asientoIds == null || asientoIds.isEmpty()) {
            throw new ValidacionException("Debe seleccionar al menos 1 asiento.");
        }
        if (asientoIds.size() > 5) {
            throw new ValidacionException("Máximo 5 asientos por transacción.");
        }

        // 1) Verificamos que la función exista
        Funcion f = funcionDAO.buscarPorId(funcionId);
        if (f == null) throw new ValidacionException("Función no encontrada.");

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

        return new CompraPreparada(compra, entradas, items);
    }

    /** Confirma la compra en una TRANSACCIÓN: compra → entradas → confitería → PDF → update ruta. */
    public Integer confirmarCompra(CompraPreparada preparada) {
        if (preparada == null) throw new ValidacionException("Compra preparada es requerida.");

        Compra compra = preparada.getCompra();
        List<Entrada> entradas = preparada.getEntradas();
        List<CompraConfiteria> items = preparada.getItemsConfiteria();

        if (entradas == null || entradas.isEmpty()) {
            throw new ValidacionException("La compra no contiene entradas.");
        }

        // 1) Persistir compra (ID generado)
        Integer compraId = compraDAO.crear(compra);

        // 2) Persistir cada entrada con ese compraId
        for (Entrada e : entradas) {
            e.setCompraId(compraId);
            e.setEstadoEntrada(EstadoEntrada.ACTIVA);
            entradaDAO.crear(e);
        }

        // 3) Persistir confitería (si hay)
        for (CompraConfiteria ci : items) {
            ci.setCompraId(compraId);
            compraConfiteriaDAO.crear(ci);
        }

        // 4) Generar comprobante (PDF real) y actualizar ruta
        Map<String, Object> extra = construirExtrasParaPDF(compra, entradas);
        String ruta = pdfService.generarComprobantePDF(compra, entradas, items, extra);
        compra.setRutaComprobantePdf(ruta);
        compraDAO.actualizar(compra);

        return compraId;
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
        // totalGeneral lo rellena la BD (columna generated) cuando se re-hidrate
    }

    /** Genera el PDF nuevamente para una compra ya confirmada (botón “re-descargar”). */
    public String generarComprobante(Integer compraId) {
        if (compraId == null) throw new ValidacionException("compraId requerido.");
        var compra = compraDAO.buscarPorId(compraId);
        if (compra == null) throw new ValidacionException("Compra no encontrada.");

        var entradas = entradaDAO.listarPorCompra(compraId);
        var items = compraConfiteriaDAO.listarPorCompra(compraId);

        Map<String, Object> extra = construirExtrasParaPDF(compra, entradas);
        String ruta = pdfService.generarComprobantePDF(compra, entradas, items, extra);
        compra.setRutaComprobantePdf(ruta);
        compraDAO.actualizar(compra);
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
                }
            }
        }
        return extra;
    }
}
