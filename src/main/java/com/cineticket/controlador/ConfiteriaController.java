package com.cineticket.controlador;

import com.cineticket.modelo.ComboConfiteria;
import com.cineticket.servicio.ConfiteriaService;
import com.cineticket.util.AppContext;
import com.cineticket.util.SelectedData;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Confitería con tarjetas tipo mockup. */
public class ConfiteriaController {

    @FXML private FlowPane gridCombos;
    @FXML private ListView<String> listViewCarrito;
    @FXML private Label lblTotalConfiteria;
    @FXML private Button btnContinuar;

    private final ConfiteriaService confiteriaService = AppContext.getConfiteriaService();

    /** comboId -> cantidad (orden de agregado) */
    private final Map<Integer, Integer> carrito = new LinkedHashMap<>();
    private final ObservableList<String> carritoTexto = FXCollections.observableArrayList();

    private static final String DEFAULT_COMBO_IMG = "/img/default-combo.png";

    @FXML
    public void initialize() {
        cargarTarjetasCombos();

        listViewCarrito.setItems(carritoTexto);

        // Doble clic en el resumen para eliminar un ítem del carrito
        listViewCarrito.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2) {
                int idx = listViewCarrito.getSelectionModel().getSelectedIndex();
                if (idx >= 0 && idx < carrito.size()) {
                    Integer comboId = carrito.keySet().stream().toList().get(idx);
                    carrito.remove(comboId);
                    actualizarCarrito();
                }
            }
        });

        actualizarCarrito();
    }

    private void cargarTarjetasCombos() {
        gridCombos.getChildren().clear();
        List<ComboConfiteria> combos = confiteriaService.obtenerCombosDisponibles();

        for (ComboConfiteria combo : combos) {
            Node card = crearTarjetaCombo(combo);
            gridCombos.getChildren().add(card);
        }
    }

    /** Crea una tarjetita tipo mockup para un combo. */
    private Node crearTarjetaCombo(ComboConfiteria c) {
        VBox card = new VBox(8);
        card.getStyleClass().add("combo-card");
        card.setPadding(new Insets(10));

        // Imagen
        ImageView img = new ImageView();
        img.setFitWidth(220);
        img.setFitHeight(120);
        img.setPreserveRatio(true);

        try {
            String url = c.getImagenUrl();
            if (url != null && !url.isBlank()) {
                img.setImage(new Image(url, true));
            } else {
                img.setImage(new Image(DEFAULT_COMBO_IMG));
            }
        } catch (Exception ex) {
            img.setImage(new Image(DEFAULT_COMBO_IMG));
        }

        // Título, descripción y precio
        Label lblNombre = new Label(nullSafe(c.getNombreCombo(), "(Sin nombre)"));
        lblNombre.getStyleClass().add("combo-title");

        Label lblDesc = new Label(nullSafe(c.getDescripcion(), ""));
        lblDesc.getStyleClass().add("combo-desc");
        lblDesc.setWrapText(true);

        Label lblPrecio = new Label("$ " + (c.getPrecio() == null
                ? "0"
                : c.getPrecio().toPlainString()));
        lblPrecio.getStyleClass().add("combo-price");

        // Controles de cantidad + botón
        Spinner<Integer> spinner = new Spinner<>();
        SpinnerValueFactory<Integer> vf =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10, 0);
        spinner.setValueFactory(vf);
        spinner.setPrefWidth(70);

        Label lblCant = new Label("Cant:");
        lblCant.getStyleClass().add("detalle-meta-label");

        Button btnAgregar = new Button("Agregar");
        btnAgregar.getStyleClass().add("primary-button");

        btnAgregar.setOnAction(ev -> {
            Integer comboId = c.getIdCombo();
            int cant = spinner.getValue() == null ? 0 : spinner.getValue();

            if (cant <= 0) {
                carrito.remove(comboId);          // 0 = quitar del carrito
            } else {
                carrito.put(comboId, cant);       // actualizar cantidad exacta
            }
            actualizarCarrito();
        });

        HBox controls = new HBox(8, lblCant, spinner, btnAgregar);
        controls.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(img, lblNombre, lblDesc, lblPrecio, controls);
        return card;
    }

    private void actualizarCarrito() {
        carritoTexto.clear();

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Integer,Integer> entry : carrito.entrySet()) {
            ComboConfiteria c = confiteriaService.obtenerCombo(entry.getKey());
            int cant = entry.getValue();
            BigDecimal precio = c.getPrecio() == null ? BigDecimal.ZERO : c.getPrecio();
            BigDecimal sub = precio.multiply(BigDecimal.valueOf(cant));
            total = total.add(sub);
            carritoTexto.add(c.getNombreCombo() + " x" + cant + "  -  $ " + sub.toPlainString());
        }

        lblTotalConfiteria.setText("Total confitería: $ " + total.toPlainString());
        if (btnContinuar != null) {
            btnContinuar.setDisable(carrito.isEmpty());
        }
    }

    @FXML
    private void continuar(ActionEvent e) {
        if (carrito.isEmpty()) {
            mostrarAlerta("Debes agregar al menos un combo de confitería antes de continuar.");
            return;
        }

        SelectedData.setCombosSeleccionados(new LinkedHashMap<>(carrito));
        UiRouter.go((Node) e.getSource(), "/fxml/pago.fxml");
    }

    // --- navegación sidebar / topbar ---

    @FXML
    private void volverCartelera(ActionEvent e) {
        UiRouter.go((Node) e.getSource(), "/fxml/cartelera.fxml");
    }

    @FXML
    private void abrirHistorial(ActionEvent e) {
        UiRouter.go((Node) e.getSource(), "/fxml/historial.fxml");
    }

    @FXML
    private void cerrarSesion(ActionEvent e) {
        UiRouter.go((Node) e.getSource(), "/fxml/login.fxml");
    }

    // --- helpers ---

    private void mostrarAlerta(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setHeaderText("Confitería obligatoria");
        a.setContentText(msg);
        a.showAndWait();
    }



    private static String nullSafe(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }
}
