package com.cineticket.controlador;

import com.cineticket.modelo.Pelicula;
import com.cineticket.modelo.Funcion;
import com.cineticket.servicio.CarteleraService;
import com.cineticket.servicio.AuthService;
import com.cineticket.util.AppContext;
import com.cineticket.util.SelectedData;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URL;
import java.util.List;

public class CarteleraController {
    @FXML private ListView<Pelicula> listViewPeliculas;
    @FXML private ListView<Funcion> listViewFunciones;
    @FXML private Label lblTituloPelicula, lblClasificacion, lblDuracion;
    @FXML private TextArea txtSinopsis;
    @FXML private ImageView imgPelicula;
    @FXML private Button btnAdministrar;

    private final CarteleraService carteleraService = AppContext.getCarteleraService();
    private final AuthService authService = AppContext.getAuthService();

    @FXML
    public void initialize() {
        try {
            cargarCartelera();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.WARNING,
                    "No se pudo cargar la cartelera desde BD. Se mostrará una lista vacía.\n" + ex.getMessage(),
                    ButtonType.OK).showAndWait();
            listViewPeliculas.setItems(FXCollections.observableArrayList());
        }

        listViewPeliculas.getSelectionModel().selectedItemProperty()
                .addListener((obs, a, b) -> manejarSeleccionPelicula(b));
        verificarPermisoAdmin();
    }

    public void cargarCartelera() {
        List<Pelicula> peliculas = carteleraService.obtenerCarteleraCompleta();
        listViewPeliculas.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Pelicula item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitulo());
            }
        });
        listViewPeliculas.setItems(FXCollections.observableArrayList(peliculas));
        if (!peliculas.isEmpty()) listViewPeliculas.getSelectionModel().selectFirst();
    }

    public void manejarSeleccionPelicula(Pelicula p) {
        if (p == null) return;
        SelectedData.setPelicula(p);
        mostrarDetallesPelicula(p);
        cargarFunciones(p.getIdPelicula());
    }

    public void manejarSeleccionFuncion(ActionEvent e) {
        var f = listViewFunciones.getSelectionModel().getSelectedItem();
        if (f == null) { showError("Selecciona una función."); return; }
        SelectedData.setFuncion(f);
        UiRouter.go((Node) e.getSource(), "/fxml/seleccion-asientos.fxml");
    }


    public void abrirHistorial(ActionEvent e) {
        showInfo("Historial pendiente");
    }

    public void abrirAdministracion(ActionEvent e) {
        showInfo("Administración pendiente");
    }

    public void cerrarSesion(ActionEvent e) {
        authService.cerrarSesion();
        UiRouter.go((Node) e.getSource(), "/fxml/login.fxml");
    }

    private void mostrarDetallesPelicula(Pelicula p) {
        lblTituloPelicula.setText(nullSafe(p.getTitulo(), "(Sin título)"));
        lblClasificacion.setText("Clasificación: " + nullSafe(
                p.getClasificacion() != null ? p.getClasificacion().name() : null, "T"));
        Integer dur = p.getDuracionMinutos();
        lblDuracion.setText("Duración: " + (dur != null ? dur : 0) + " min");
        String sinop = (p.getSinopsis() != null && !p.getSinopsis().isBlank())
                ? p.getSinopsis() : "Sinopsis no disponible.";
        txtSinopsis.setText(sinop);

        try {
            if (p.getImagenUrl() != null && !p.getImagenUrl().isBlank()) {
                imgPelicula.setImage(new Image(p.getImagenUrl(), true));
            } else {
                imgPelicula.setImage(new Image(getClass()
                        .getResource("/img/default-poster.png").toExternalForm()));
            }
        } catch (Exception ex) {
            imgPelicula.setImage(new Image(getClass()
                    .getResource("/img/default-poster.png").toExternalForm()));
        }
    }

    private void cargarFunciones(Integer peliculaId) {
        List<Funcion> funciones = carteleraService.obtenerFuncionesPorPelicula(peliculaId);
        listViewFunciones.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Funcion item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
            }
        });
        listViewFunciones.setItems(FXCollections.observableArrayList(funciones));
    }

    private void verificarPermisoAdmin() {
        boolean esAdmin = authService.obtenerUsuarioActual() != null &&
                authService.obtenerUsuarioActual().getRol() != null &&
                authService.obtenerUsuarioActual().getRol().name().equals("ADMIN");
        btnAdministrar.setVisible(esAdmin);
    }

    private void showError(String msg) { new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait(); }
    private void showInfo(String msg) { new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait(); }
    private String nullSafe(String val, String def) { return (val == null || val.isBlank()) ? def : val; }
}
