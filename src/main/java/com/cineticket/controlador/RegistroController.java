package com.cineticket.controlador;

import com.cineticket.excepcion.AutenticacionException;
import com.cineticket.excepcion.ValidacionException;
import com.cineticket.servicio.AuthService;
import com.cineticket.util.AppContext;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class RegistroController {

    // Fondo responsive (como en Login)
    @FXML private StackPane rootPane;
    @FXML private ImageView imgBackground;

    // Campos del formulario
    @FXML private TextField txtNombreCompleto;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtContrasena;

    // Mensaje inline dentro de la tarjeta
    @FXML private Label lblMensaje;

    private final AuthService authService = AppContext.getAuthService();

    @FXML
    public void initialize() {

        // Hacer el fondo responsive
        if (imgBackground != null && rootPane != null) {
            imgBackground.fitWidthProperty().bind(rootPane.widthProperty());
            imgBackground.fitHeightProperty().bind(rootPane.heightProperty());
        }

        // Resetear mensaje al escribir
        ocultarMensaje();

        txtNombreCompleto.textProperty().addListener((o, a, b) -> ocultarMensaje());
        txtCorreo.textProperty().addListener((o, a, b) -> ocultarMensaje());
        txtUsuario.textProperty().addListener((o, a, b) -> ocultarMensaje());
        txtContrasena.textProperty().addListener((o, a, b) -> ocultarMensaje());
    }

    @FXML
    public void registrar(ActionEvent e) {
        String nombre = txtNombreCompleto.getText();
        String correo = txtCorreo.getText();
        String user = txtUsuario.getText();
        String pass = txtContrasena.getText();

        try {
            // Registrar usuario
            authService.registrarUsuario(nombre, correo, user, pass);

            // Login automático (manteniendo tu comportamiento original)
            authService.iniciarSesion(user, pass);

            UiRouter.go((Node) e.getSource(), "/fxml/cartelera.fxml");

        } catch (ValidacionException | AutenticacionException ex) {
            mostrarMensaje(ex.getMessage());

        } catch (Exception ex) {
            mostrarMensaje("Ocurrió un error al registrarte.");
            ex.printStackTrace();
        }
    }

    @FXML
    public void volverLogin(ActionEvent e) {
        UiRouter.go((Node) e.getSource(), "/fxml/login.fxml");
    }

    private void mostrarMensaje(String msg) {
        lblMensaje.setText(msg);
        lblMensaje.setVisible(true);
        lblMensaje.setManaged(true);
    }

    private void ocultarMensaje() {
        lblMensaje.setText("");
        lblMensaje.setVisible(false);
        lblMensaje.setManaged(false);
    }
}
