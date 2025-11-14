package com.cineticket.controlador.admin;

import com.cineticket.excepcion.ValidacionException;
import com.cineticket.servicio.ReporteService;
import com.cineticket.util.AppContext;
import com.cineticket.util.SessionManager;
import com.cineticket.controlador.UiRouter;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ReportesVentasController {

    private static final Logger log = LoggerFactory.getLogger(ReportesVentasController.class);

    // ==== Filtros ====
    @FXML private DatePicker dpInicio;
    @FXML private DatePicker dpFin;
    @FXML private Label lblRangoSeleccionado;

    // ==== KPIs ====
    @FXML private Label lblTotalIngresos;
    @FXML private Label lblTotalEntradas;
    @FXML private Label lblTotalCombos;
    @FXML private Label lblPrecioPromedio;

    // ==== Charts ====
    @FXML private BarChart<String, Number> chartIngresosPorDia;
    @FXML private LineChart<String, Number> chartEntradasPorDia;
    @FXML private PieChart chartTopPeliculas;

    // ==== Servicios ====
    private final ReporteService reporteService = AppContext.getReporteService();

    private final DateTimeFormatter fechaEjeFmt = DateTimeFormatter.ofPattern("dd/MM");
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    @FXML
    private void initialize() {
        // Rango por defecto: últimos 7 días
        LocalDate hoy = LocalDate.now();
        LocalDate hace7 = hoy.minusDays(6);
        dpInicio.setValue(hace7);
        dpFin.setValue(hoy);

        inicializarCharts();
        aplicarFiltros(null);
    }

    private void inicializarCharts() {
        if (chartIngresosPorDia != null) {
            chartIngresosPorDia.setLegendVisible(false);
        }
        if (chartEntradasPorDia != null) {
            chartEntradasPorDia.setLegendVisible(false);
        }
        if (chartTopPeliculas != null) {
            chartTopPeliculas.setLegendVisible(true);
        }
    }

    // ==== Acciones UI ====

    @FXML
    private void aplicarFiltros(ActionEvent e) {
        try {
            LocalDate inicio = dpInicio.getValue();
            LocalDate fin = dpFin.getValue();

            if (inicio == null && fin == null) {
                fin = LocalDate.now();
                inicio = fin.minusDays(6);
            } else if (inicio == null) {
                inicio = fin.minusDays(6);
            } else if (fin == null) {
                fin = inicio;
            }

            if (inicio.isAfter(fin)) {
                // Intercambiar si el usuario los puso al revés
                LocalDate tmp = inicio;
                inicio = fin;
                fin = tmp;
            }

            actualizarReportes(inicio, fin);

        } catch (Exception ex) {
            log.error("Error al aplicar filtros de reporte", ex);
            mostrarError("No se pudieron generar los reportes.\n" + ex.getMessage());
        }
    }

    private void actualizarReportes(LocalDate inicio, LocalDate fin) {
        // ===== Agregados generales =====
        BigDecimal totalIngresos = BigDecimal.ZERO;
        BigDecimal totalIngresosEntradas = BigDecimal.ZERO;
        int totalEntradas = 0;
        int totalCombos = 0;

        XYChart.Series<String, Number> serieIngresos = new XYChart.Series<>();
        XYChart.Series<String, Number> serieEntradas = new XYChart.Series<>();

        LocalDate fecha = inicio;
        while (!fecha.isAfter(fin)) {
            Map<String, Object> repDia = reporteService.generarReporteVentasPorDia(fecha);

            int entradasDia = (Integer) repDia.get("totalEntradas");
            int combosDia   = (Integer) repDia.get("totalCombos");
            BigDecimal ingresosDia      = (BigDecimal) repDia.get("ingresosTotales");
            BigDecimal ingresosEntDia   = (BigDecimal) repDia.get("ingresosEntradas");

            totalEntradas += entradasDia;
            totalCombos   += combosDia;
            totalIngresos = totalIngresos.add(ingresosDia);
            totalIngresosEntradas = totalIngresosEntradas.add(ingresosEntDia);

            String etiqueta = fechaEjeFmt.format(fecha);
            serieIngresos.getData().add(new XYChart.Data<>(etiqueta, ingresosDia.doubleValue()));
            serieEntradas.getData().add(new XYChart.Data<>(etiqueta, entradasDia));

            fecha = fecha.plusDays(1);
        }

        // ===== KPIs =====
        lblTotalIngresos.setText(formatearMoneda(totalIngresos));
        lblTotalEntradas.setText(String.valueOf(totalEntradas));
        lblTotalCombos.setText(String.valueOf(totalCombos));

        BigDecimal precioPromedio = BigDecimal.ZERO;
        if (totalEntradas > 0) {
            precioPromedio = totalIngresosEntradas
                    .divide(BigDecimal.valueOf(totalEntradas), 2, RoundingMode.HALF_UP);
        }
        lblPrecioPromedio.setText(formatearMoneda(precioPromedio));

        if (inicio.equals(fin)) {
            lblRangoSeleccionado.setText("Para el día " + inicio);
        } else {
            lblRangoSeleccionado.setText("Del " + inicio + " al " + fin);
        }

        // ===== Charts diarios =====
        chartIngresosPorDia.getData().setAll(serieIngresos);
        chartEntradasPorDia.getData().setAll(serieEntradas);

        // ===== Top películas =====
        var top = reporteService.obtenerTopPeliculas(5, inicio, fin);
        var pieData = FXCollections.<PieChart.Data>observableArrayList();
        for (Map<String, Object> row : top) {
            String titulo = (String) row.get("titulo");
            Number entradas = (Number) row.get("entradasVendidas");
            pieData.add(new PieChart.Data(titulo, entradas.doubleValue()));
        }
        chartTopPeliculas.setData(pieData);
    }

    private String formatearMoneda(BigDecimal valor) {
        if (valor == null) {
            valor = BigDecimal.ZERO;
        }
        return currencyFormat.format(valor);
    }

    // ==== Navegación sidebar / topbar ====

    @FXML
    private void irGestionCartelera(ActionEvent e) {
        UiRouter.go((Node) e.getSource(), "/fxml/admin/gestion_cartelera.fxml");
    }

    @FXML
    private void cerrarSesion(ActionEvent e) {
        SessionManager.getInstance().cerrarSesion();
        UiRouter.go((Node) e.getSource(), "/fxml/login.fxml");
    }

    // ==== Helpers mensajes ====

    private void mostrarError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText("Ha ocurrido un problema");
        a.setContentText(msg);
        a.showAndWait();
    }
}
