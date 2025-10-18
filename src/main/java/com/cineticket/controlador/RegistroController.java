package com.cineticket.controlador;

import com.cineticket.excepcion.AutenticacionException;
import com.cineticket.excepcion.ValidacionException;
import com.cineticket.servicio.AuthService;
import com.cineticket.util.AppContext;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/** Controlador mínimo de Registro. Tras registrar, hace login automático y va a cartelera. */
public class RegistroController {

    @FXML private TextField txtNombreCompleto;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtContrasena;

    private final AuthService authService = AppContext.getAuthService();

    @FXML
    public void registrar(ActionEvent e) {
        String nombre = txtNombreCompleto.getText();
        String correo = txtCorreo.getText();
        String user = txtUsuario.getText();
        String pass = txtContrasena.getText();
        try {
            authService.registrarUsuario(nombre, correo, user, pass);
            // login automático
            authService.iniciarSesion(user, pass);
            UiRouter.go(((Button) e.getSource()), "/fxml/cartelera.fxml");
        } catch (ValidacionException | AutenticacionException ex) {
            mostrarError(ex.getMessage());
        } catch (Exception ex) {
            mostrarError("Ocurrió un error al registrarte.");
        }
    }

    @FXML
    public void volverLogin(ActionEvent e) {
        UiRouter.go(((Button) e.getSource()), "/fxml/login.fxml");
    }

    private void mostrarError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText("No se pudo registrar");
        a.showAndWait();
    }
}
