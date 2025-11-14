package com.cineticket.controlador;

import com.cineticket.enums.*;
import com.cineticket.excepcion.AutenticacionException;
import com.cineticket.excepcion.ValidacionException;
import com.cineticket.modelo.*;
import com.cineticket.servicio.AuthService;
import com.cineticket.util.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class LoginController {

    @FXML private StackPane rootPane;
    @FXML private ImageView imgBackground;

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtContrasena;
    @FXML private Label lblMensaje;

    private final AuthService authService = AppContext.getAuthService();

    @FXML
    public void initialize() {
        // Fondo responsive
        if (imgBackground != null && rootPane != null) {
            imgBackground.fitWidthProperty().bind(rootPane.widthProperty());
            imgBackground.fitHeightProperty().bind(rootPane.heightProperty());
        }

        ocultarMensaje();
        txtUsuario.textProperty().addListener((obs, o, n) -> ocultarMensaje());
        txtContrasena.textProperty().addListener((obs, o, n) -> ocultarMensaje());
    }

    @FXML
    public void manejarInicioSesion(ActionEvent e) {
        String user = txtUsuario.getText();
        String pass = txtContrasena.getText();

        try {
            authService.iniciarSesion(user, pass);

            // Recuperamos el usuario logueado
            Usuario u = SessionManager.getInstance().getUsuarioActual();

            // Si es administrador, a la pantalla admin
            if (u != null && u.getRol() == Rol.ADMIN) {
                UiRouter.go((Node) e.getSource(), "/fxml/admin/gestion_cartelera.fxml");
                return;
            }

            // Si es usuario normal → cartelera
            UiRouter.go((Node) e.getSource(), "/fxml/cartelera.fxml");

        } catch (ValidacionException | AutenticacionException ex) {
            mostrarMensaje(ex.getMessage());

        } catch (Exception ex) {
            mostrarMensaje("Ocurrió un error al iniciar sesión.");
            ex.printStackTrace();
        }
    }


    @FXML
    public void abrirRegistro(ActionEvent e) {
        UiRouter.go((Node) e.getSource(), "/fxml/registro.fxml");
    }

    private void mostrarMensaje(String msg) {
        if (lblMensaje != null) {
            lblMensaje.setText(msg);
            lblMensaje.setVisible(true);
            lblMensaje.setManaged(true);
        } else {
            // Fallback por si algo sale mal
            Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
            a.setHeaderText("No se pudo iniciar sesión");
            a.showAndWait();
        }
    }

    private void ocultarMensaje() {
        if (lblMensaje != null) {
            lblMensaje.setText("");
            lblMensaje.setVisible(false);
            lblMensaje.setManaged(false);
        }
    }
}
