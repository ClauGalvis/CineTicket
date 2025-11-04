package com.cineticket.servicio;

import com.cineticket.dao.CompraDAO;
import com.cineticket.dao.EntradaDAO;
import com.cineticket.dao.CompraConfiteriaDAO;
import com.cineticket.dao.PeliculaDAO;
import com.cineticket.dao.FuncionDAO;
import com.cineticket.modelo.Compra;
import com.cineticket.modelo.Entrada;
import com.cineticket.modelo.Pelicula;
import com.cineticket.modelo.Funcion;
import com.cineticket.modelo.CompraConfiteria;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de Reportes Administrativos (capa de negocio).
 * - Orquesta DAOs y realiza agregaciones.
 * - No contiene SQL directo.
 * - Retorna estructuras simples para que la UI las grafique.
 */
public class ReporteService {

    private final CompraDAO compraDAO;
    private final EntradaDAO entradaDAO;
    private final CompraConfiteriaDAO compraConfiteriaDAO;
    private final PeliculaDAO peliculaDAO;
    private final FuncionDAO funcionDAO;

    public ReporteService(CompraDAO compraDAO,
                          EntradaDAO entradaDAO,
                          CompraConfiteriaDAO compraConfiteriaDAO,
                          PeliculaDAO peliculaDAO,
                          FuncionDAO funcionDAO) {
        this.compraDAO = Objects.requireNonNull(compraDAO);
        this.entradaDAO = Objects.requireNonNull(entradaDAO);
        this.compraConfiteriaDAO = Objects.requireNonNull(compraConfiteriaDAO);
        this.peliculaDAO = Objects.requireNonNull(peliculaDAO);
        this.funcionDAO = Objects.requireNonNull(funcionDAO);
    }

    /**
     * Reporte de ventas por día (fecha calendario).
     * Retorna:
     *  - "fecha": LocalDate
     *  - "totalCompras": int
     *  - "totalEntradas": int
     *  - "totalCombos": int
     *  - "ingresosEntradas": BigDecimal
     *  - "ingresosConfiteria": BigDecimal
     *  - "ingresosTotales": BigDecimal
     *  - "porHora": Map<Integer, BigDecimal>  (ingresos totales por hora)
     */
    public Map<String, Object> generarReporteVentasPorDia(LocalDate fecha) {
        Objects.requireNonNull(fecha, "fecha requerida");
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.plusDays(1).atStartOfDay().minusNanos(1);

        List<Compra> compras = compraDAO.obtenerComprasEntreFechas(inicio, fin);

        int totalCompras = compras.size();
        int totalEntradas = 0;
        int totalCombos = 0;

        BigDecimal ingresosEntradas = BigDecimal.ZERO;
        BigDecimal ingresosConf = BigDecimal.ZERO;

        // Para barras por hora
        Map<Integer, BigDecimal> porHora = new LinkedHashMap<>();
        for (int h = 0; h < 24; h++) porHora.put(h, BigDecimal.ZERO);

        for (Compra c : compras) {
            // Entradas de la compra
            List<Entrada> entradas = entradaDAO.listarPorCompra(c.getIdCompra());
            totalEntradas += entradas.size();
            ingresosEntradas = ingresosEntradas.add(nullSafe(c.getTotalEntradas()));

            // Confitería de la compra
            List<CompraConfiteria> items = compraConfiteriaDAO.listarPorCompra(c.getIdCompra());
            totalCombos += items.stream().mapToInt(CompraConfiteria::getCantidad).sum();
            ingresosConf = ingresosConf.add(nullSafe(c.getTotalConfiteria()));

            // Por hora
            int h = c.getFechaHoraCompra() != null ? c.getFechaHoraCompra().getHour() : 0;
            porHora.put(h, porHora.get(h).add(nullSafe(c.getTotalGeneral())));
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("fecha", fecha);
        out.put("totalCompras", totalCompras);
        out.put("totalEntradas", totalEntradas);
        out.put("totalCombos", totalCombos);
        out.put("ingresosEntradas", ingresosEntradas.setScale(2, RoundingMode.HALF_UP));
        out.put("ingresosConfiteria", ingresosConf.setScale(2, RoundingMode.HALF_UP));
        out.put("ingresosTotales", ingresosEntradas.add(ingresosConf).setScale(2, RoundingMode.HALF_UP));
        out.put("porHora", porHora);
        return out;
    }

    /**
     * Reporte por película en un rango [fechaInicio, fechaFin].
     * Retorna:
     *  - "peliculaId": int
     *  - "titulo": String
     *  - "entradasVendidas": int
     *  - "funcionesAfectadas": Set<Integer> (IDs de función, útil si quieres ver “cuántas funciones”)
     *  - "ingresosAproxEntradas": BigDecimal  (suma de precio_unitario de entradas)
     */
    public Map<String, Object> generarReporteVentasPorPelicula(Integer peliculaId,
                                                               LocalDate fechaInicio,
                                                               LocalDate fechaFin) {
        Objects.requireNonNull(peliculaId, "peliculaId requerido");
        Objects.requireNonNull(fechaInicio, "fechaInicio requerida");
        Objects.requireNonNull(fechaFin, "fechaFin requerida");

        Pelicula peli = peliculaDAO.buscarPorId(peliculaId);
        if (peli == null) {
            throw new IllegalArgumentException("Película no encontrada: " + peliculaId);
        }

        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.plusDays(1).atStartOfDay().minusNanos(1);

        List<Compra> compras = compraDAO.obtenerComprasEntreFechas(inicio, fin);

        int entradasVendidas = 0;
        BigDecimal ingresos = BigDecimal.ZERO;
        Set<Integer> funciones = new HashSet<>();

        for (Compra c : compras) {
            List<Entrada> entradas = entradaDAO.listarPorCompra(c.getIdCompra());
            for (Entrada e : entradas) {
                Funcion f = funcionDAO.buscarPorId(e.getFuncionId());
                if (f != null && Objects.equals(f.getPeliculaId(), peliculaId)) {
                    entradasVendidas += 1;
                    ingresos = ingresos.add(nullSafe(e.getPrecioUnitario()));
                    funciones.add(f.getIdFuncion());
                }
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("peliculaId", peliculaId);
        out.put("titulo", peli.getTitulo());
        out.put("entradasVendidas", entradasVendidas);
        out.put("funcionesAfectadas", funciones);
        out.put("ingresosAproxEntradas", ingresos.setScale(2, RoundingMode.HALF_UP));
        return out;
    }

    /**
     * Ventas de confitería por combo en un rango.
     * Retorna:
     *  - "ventasPorCombo": Map<Integer(comboId), Integer(cantidadVendida)>
     *  - "totalCombos": int
     */
    public Map<String, Object> generarReporteVentasConfiteria(LocalDate fechaInicio, LocalDate fechaFin) {
        Objects.requireNonNull(fechaInicio, "fechaInicio requerida");
        Objects.requireNonNull(fechaFin, "fechaFin requerida");

        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.plusDays(1).atStartOfDay().minusNanos(1);

        Map<Integer, Integer> ventas = compraConfiteriaDAO.obtenerVentasPorCombo(inicio, fin);
        int total = ventas.values().stream().mapToInt(Integer::intValue).sum();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ventasPorCombo", ventas);
        out.put("totalCombos", total);
        return out;
    }

    /**
     * Top N películas por entradas vendidas en el rango.
     * Retorna lista ordenada desc con elementos:
     *  - "peliculaId", "titulo", "entradasVendidas", "ingresosAproxEntradas"
     */
    public List<Map<String, Object>> obtenerTopPeliculas(int limite,
                                                         LocalDate fechaInicio,
                                                         LocalDate fechaFin) {
        if (limite <= 0) limite = 5;
        Objects.requireNonNull(fechaInicio);
        Objects.requireNonNull(fechaFin);

        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.plusDays(1).atStartOfDay().minusNanos(1);

        List<Compra> compras = compraDAO.obtenerComprasEntreFechas(inicio, fin);

        // peliculaId -> {entradas, ingresos}
        Map<Integer, PelAgg> agg = new HashMap<>();

        for (Compra c : compras) {
            for (Entrada e : entradaDAO.listarPorCompra(c.getIdCompra())) {
                Funcion f = funcionDAO.buscarPorId(e.getFuncionId());
                if (f == null || f.getPeliculaId() == null) continue;
                int pId = f.getPeliculaId();
                PelAgg a = agg.computeIfAbsent(pId, k -> new PelAgg());
                a.entradas++;
                a.ingresos = a.ingresos.add(nullSafe(e.getPrecioUnitario()));
            }
        }

        // Cargar títulos
        Map<Integer, String> titulos = new HashMap<>();
        for (Integer pId : agg.keySet()) {
            Pelicula p = peliculaDAO.buscarPorId(pId);
            titulos.put(pId, p != null ? p.getTitulo() : ("Película " + pId));
        }

        return agg.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().entradas, e1.getValue().entradas))
                .limit(limite)
                .map(e -> {
                    int pId = e.getKey();
                    PelAgg a = e.getValue();
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("peliculaId", pId);
                    row.put("titulo", titulos.get(pId));
                    row.put("entradasVendidas", a.entradas);
                    row.put("ingresosAproxEntradas", a.ingresos.setScale(2, RoundingMode.HALF_UP));
                    return row;
                })
                .collect(Collectors.toList());
    }

    /**
     * Top N combos por cantidad vendida en el rango.
     * Retorna lista ordenada desc con elementos:
     *  - "comboId", "cantidad"
     */
    public List<Map<String, Object>> obtenerTopCombos(int limite,
                                                      LocalDate fechaInicio,
                                                      LocalDate fechaFin) {
        if (limite <= 0) limite = 5;
        Objects.requireNonNull(fechaInicio);
        Objects.requireNonNull(fechaFin);

        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.plusDays(1).atStartOfDay().minusNanos(1);

        Map<Integer, Integer> ventas = compraConfiteriaDAO.obtenerVentasPorCombo(inicio, fin);

        return ventas.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(limite)
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("comboId", e.getKey());
                    row.put("cantidad", e.getValue());
                    return row;
                })
                .collect(Collectors.toList());
    }

    /**
     * Suma total de ingresos (entradas + confitería) en el rango.
     * Usa total_general de la tabla compra (columna GENERATED ALWAYS).
     */
    public BigDecimal calcularIngresosTotales(LocalDate fechaInicio, LocalDate fechaFin) {
        Objects.requireNonNull(fechaInicio, "fechaInicio requerida");
        Objects.requireNonNull(fechaFin, "fechaFin requerida");

        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.plusDays(1).atStartOfDay().minusNanos(1);

        List<Compra> compras = compraDAO.obtenerComprasEntreFechas(inicio, fin);
        BigDecimal total = BigDecimal.ZERO;
        for (Compra c : compras) {
            total = total.add(nullSafe(c.getTotalGeneral()));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    // ==== Helpers internos ====

    private static BigDecimal nullSafe(BigDecimal bd) {
        return bd == null ? BigDecimal.ZERO : bd;
    }

    private static class PelAgg {
        int entradas = 0;
        BigDecimal ingresos = BigDecimal.ZERO;
    }
}
