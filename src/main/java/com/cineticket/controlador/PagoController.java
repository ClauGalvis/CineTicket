package com.cineticket.controlador;

import com.cineticket.enums.MetodoPago;
import com.cineticket.modelo.ComboConfiteria;
import com.cineticket.modelo.Funcion;
import com.cineticket.modelo.Pelicula;
import com.cineticket.servicio.CompraService;
import com.cineticket.servicio.ConfiteriaService;
import com.cineticket.servicio.CarteleraService;
import com.cineticket.util.AppContext;
import com.cineticket.util.SelectedData;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Controlador de Pago: resumen y confirmación. */
public class PagoController {

    // --- UI ---
    @FXML private RadioButton rbPSE;
    @FXML private RadioButton rbTransferencia;
    @FXML private ToggleGroup toggleGroupMetodo;

    @FXML private Label lblResumenEntradas;
    @FXML private Label lblResumenConfiteria;
    @FXML private Label lblTotalGeneral;
    @FXML private ListView<String> listViewResumen;

    @FXML private Button btnConfirmar;
    @FXML private Button btnCancelar;

    // --- Estado ---
    private Funcion funcion;
    private List<Integer> asientos = new ArrayList<>();
    private Map<Integer, Integer> combos = new LinkedHashMap<>();

    private BigDecimal totalEntradas = BigDecimal.ZERO;
    private BigDecimal totalConfiteria = BigDecimal.ZERO;

    private final NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        if (toggleGroupMetodo == null) toggleGroupMetodo = new ToggleGroup();
        rbPSE.setToggleGroup(toggleGroupMetodo);
        rbTransferencia.setToggleGroup(toggleGroupMetodo);

        // Si no nos pasan datos explícitos, lee SelectedData
        if (SelectedData.getFuncion() != null) this.funcion = SelectedData.getFuncion();
        if (SelectedData.getAsientosSeleccionados() != null)
            this.asientos = new ArrayList<>(SelectedData.getAsientosSeleccionados());
        if (SelectedData.getCombosSeleccionados() != null)
            this.combos = new LinkedHashMap<>(SelectedData.getCombosSeleccionados());

        mostrarResumen();
    }

    /** Opción alterna si prefieres inyectar datos desde el router. */
    public void setDatosCompra(Funcion funcion,
                               List<Integer> asientos,
                               Map<Integer, Integer> combos) {
        this.funcion = funcion;
        this.asientos = (asientos == null) ? new ArrayList<>() : new ArrayList<>(asientos);
        this.combos = (combos == null) ? new LinkedHashMap<>() : new LinkedHashMap<>(combos);
        mostrarResumen();
    }

    /** Llena listView y labels de totales. */
    private void mostrarResumen() {
        listViewResumen.getItems().clear();

        if (funcion == null || asientos == null || asientos.isEmpty()) {
            alerta(Alert.AlertType.WARNING, "No hay datos de compra. Regresando a cartelera.");
            UiRouter.go(listViewResumen, "/fxml/cartelera.fxml");
            return;
        }

        // --- Película (obtener título por ID) ---
        CarteleraService cartelera = AppContext.getCarteleraService();
        Pelicula peli = null;
        String titulo = "(Película)";
        try {
            peli = cartelera.obtenerDetallesPelicula(funcion.getPeliculaId());
            if (peli != null && peli.getTitulo() != null) titulo = peli.getTitulo();
        } catch (Exception ignored) { /* si falla, dejamos el placeholder */ }

        // --- Entradas ---
        Double precio = funcion.getPrecioEntrada();
        if (precio == null) precio = 0.0;
        totalEntradas = BigDecimal.valueOf(precio).multiply(BigDecimal.valueOf(asientos.size()));

        listViewResumen.getItems().add("Película: " + titulo);
        listViewResumen.getItems().add("Función: Sala " + funcion.getSalaId() +
                " | " + funcion.getFechaHoraInicio().format(dtf));
        listViewResumen.getItems().add("Entradas:");
        for (Integer idAsiento : asientos) {
            listViewResumen.getItems().add(" - Asiento " + idAsiento + "  " + nf.format(precio));
        }
        lblResumenEntradas.setText(asientos.size() + " entradas - " + nf.format(totalEntradas));

        // --- Confitería ---
        totalConfiteria = BigDecimal.ZERO;
        if (combos != null && !combos.isEmpty()) {
            ConfiteriaService confService = AppContext.getConfiteriaService();
            listViewResumen.getItems().add("Confitería:");
            int items = 0;
            for (var entry : combos.entrySet()) {
                Integer comboId = entry.getKey();
                Integer cantidad = entry.getValue();
                ComboConfiteria combo = confService.obtenerCombo(comboId);
                BigDecimal subtotal = combo.getPrecio().multiply(BigDecimal.valueOf(cantidad));
                totalConfiteria = totalConfiteria.add(subtotal);
                items += cantidad;

                listViewResumen.getItems().add(" - " + combo.getNombreCombo() +
                        " x" + cantidad + "  " + nf.format(subtotal));
            }
            lblResumenConfiteria.setText(items + " combos - " + nf.format(totalConfiteria));
        } else {
            lblResumenConfiteria.setText("0 combos - " + nf.format(0));
        }

        // --- Total general ---
        BigDecimal totalGeneral = totalEntradas.add(totalConfiteria);
        lblTotalGeneral.setText("TOTAL: " + nf.format(totalGeneral));
    }

    // === Acciones ===

    @FXML
    private void confirmarCompra(ActionEvent e) {
        MetodoPago metodo = metodoSeleccionado();
        if (metodo == null) {
            alerta(Alert.AlertType.WARNING, "Debes seleccionar un método de pago.");
            return;
        }

        try {
            // Usuario en sesión
            Integer usuarioId = Optional.ofNullable(AppContext.getAuthService().obtenerUsuarioActual())
                    .map(u -> u.getIdUsuario())
                    .orElseThrow(() -> new IllegalStateException("No hay usuario autenticado."));

            CompraService compraService = AppContext.getCompraService();

            // Armar compra en memoria
            var preparada = compraService.crearCompra(
                    usuarioId,
                    funcion.getIdFuncion(),
                    asientos,
                    combos,
                    metodo
            );

            // Simulación de pago ok
            if (!procesarPago()) {
                return;
            }

            // Persistir compra (POR AHORA sin generar PDF aquí)
            Integer compraId = compraService.confirmarCompra(preparada);

            // Limpiamos datos temporales
            SelectedData.clear();

            // Mensaje breve
            alerta(Alert.AlertType.INFORMATION,
                    "¡Compra confirmada!\n\n" +
                            "Puedes consultar el detalle y generar/ver tu comprobante " +
                            "desde la sección 'Mis compras'.");

            // Redirigir a historial de compras
            UiRouter.go((Node) e.getSource(), "/fxml/historial.fxml");

        } catch (Exception ex) {
            alerta(Alert.AlertType.ERROR, "No se pudo completar la compra.\n" + ex.getMessage());
        }
    }


    @FXML
    private void cancelar(ActionEvent e) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Cancelar y volver a la cartelera?", ButtonType.NO, ButtonType.YES);
        a.setHeaderText("Cancelar pago");
        var resp = a.showAndWait();
        if (resp.isPresent() && resp.get() == ButtonType.YES) {
            UiRouter.go((Node) e.getSource(), "/fxml/cartelera.fxml");
        }
    }

    @FXML
    public void abrirHistorial(ActionEvent e) {
        UiRouter.go((Node) e.getSource(), "/fxml/historial.fxml");
    }

    // === Helpers ===

    private MetodoPago metodoSeleccionado() {
        Toggle t = toggleGroupMetodo.getSelectedToggle();
        if (t == rbPSE) return MetodoPago.PSE;
        if (t == rbTransferencia) return MetodoPago.TRANSFERENCIA;
        return null;
    }

    private boolean procesarPago() {
        // MVP: simulación instantánea
        return true;
    }

    /** Copia el PDF generado por backend a una ubicación elegida por el usuario. */
    private void generarYGuardarComprobante(String rutaOrigen) {
        if (rutaOrigen == null || rutaOrigen.isBlank()) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar comprobante PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        fc.setInitialFileName("Comprobante_CineTicket.pdf");

        File destino = fc.showSaveDialog(listViewResumen.getScene().getWindow());
        if (destino == null) return;

        try {
            Files.copy(Path.of(rutaOrigen), destino.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            alerta(Alert.AlertType.ERROR, "No se pudo guardar el PDF.\n" + ex.getMessage());
        }
    }

    private void mostrarConfirmacion(String rutaPDF) {
        Alert a = new Alert(Alert.AlertType.INFORMATION,
                "¡Compra confirmada!\n\n" +
                        "Tu comprobante fue generado.\n" +
                        (rutaPDF != null ? "Ruta interna: " + rutaPDF : ""),
                ButtonType.OK);
        a.setHeaderText("Compra exitosa");
        a.showAndWait();
    }

    private void alerta(Alert.AlertType tipo, String msg) {
        Alert a = new Alert(tipo, msg, ButtonType.OK);
        a.setHeaderText("Mensaje");
        a.showAndWait();
    }
}
