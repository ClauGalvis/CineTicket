package com.cineticket.controlador;

import com.cineticket.modelo.ComboConfiteria;
import com.cineticket.servicio.ConfiteriaService;
import com.cineticket.util.AppContext;
import com.cineticket.util.SelectedData;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Confitería (mínimo para el flujo principal). */
public class ConfiteriaController {

    @FXML private ListView<ComboConfiteria> listViewCombos;
    @FXML private Label lblNombreCombo;
    @FXML private Label lblDescripcion;
    @FXML private Label lblPrecio;
    @FXML private ImageView imgCombo;

    @FXML private Spinner<Integer> spinnerCantidad;
    @FXML private Button btnAgregar;

    @FXML private ListView<String> listViewCarrito; // mostramos líneas legibles
    @FXML private Label lblTotalConfiteria;

    @FXML private Button btnContinuar;

    private final ConfiteriaService confiteriaService = AppContext.getConfiteriaService();

    /** comboId -> cantidad (orden de agregado) */
    private final Map<Integer, Integer> carrito = new LinkedHashMap<>();
    private final ObservableList<String> carritoTexto = FXCollections.observableArrayList();

    private static final String DEFAULT_COMBO_IMG = "/img/default-combo.png";

    @FXML
    public void initialize() {
        cargarCombos();

        SpinnerValueFactory<Integer> vf = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1);
        spinnerCantidad.setValueFactory(vf);

        listViewCombos.getSelectionModel().selectedItemProperty().addListener((obs, old, combo) -> {
            if (combo != null) manejarSeleccionCombo(combo);
        });

        listViewCombos.setCellFactory(ignored -> new ListCell<>() {
            @Override protected void updateItem(ComboConfiteria c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? "" : c.getNombreCombo());
            }
        });

        if (!listViewCombos.getItems().isEmpty()) {
            listViewCombos.getSelectionModel().selectFirst();
        }

        listViewCarrito.setItems(carritoTexto);
        actualizarCarrito();
    }

    private void cargarCombos() {
        List<ComboConfiteria> combos = confiteriaService.obtenerCombosDisponibles();
        listViewCombos.setItems(FXCollections.observableArrayList(combos));
    }

    private void manejarSeleccionCombo(ComboConfiteria combo) {
        mostrarDetalleCombo(combo);
    }

    private void mostrarDetalleCombo(ComboConfiteria c) {
        lblNombreCombo.setText(nullSafe(c.getNombreCombo(), "(Sin nombre)"));
        lblDescripcion.setText(nullSafe(c.getDescripcion(), "Sin descripción."));
        lblPrecio.setText("$ " + (c.getPrecio() == null ? "0" : c.getPrecio().toPlainString()));

        String url = c.getImagenUrl();
        Image img;
        try {
            if (url != null && !url.isBlank()) img = new Image(url, true);
            else img = new Image(DEFAULT_COMBO_IMG, true);
        } catch (Exception ex) {
            img = new Image(DEFAULT_COMBO_IMG, true);
        }
        imgCombo.setImage(img);
    }

    @FXML
    private void agregarAlCarrito(ActionEvent e) {
        ComboConfiteria seleccionado = listViewCombos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;

        int cant = spinnerCantidad.getValue() == null ? 1 : spinnerCantidad.getValue();
        carrito.merge(seleccionado.getIdCombo(), cant, Integer::sum);
        actualizarCarrito();
    }

    private void removerDelCarrito(Integer comboId) {
        if (comboId == null) return;
        carrito.remove(comboId);
        actualizarCarrito();
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

        listViewCarrito.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2) {
                int idx = listViewCarrito.getSelectionModel().getSelectedIndex();
                if (idx >= 0 && idx < carrito.size()) {
                    Integer comboId = carrito.keySet().stream().toList().get(idx);
                    removerDelCarrito(comboId);
                }
            }
        });
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
