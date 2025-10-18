package com.cineticket.controlador;

import com.cineticket.excepcion.AutenticacionException;
import com.cineticket.excepcion.ValidacionException;
import com.cineticket.servicio.AuthService;
import com.cineticket.util.AppContext;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;

/** Controlador mínimo de Login para el flujo básico. */
public class LoginController {
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtContrasena;

    private final AuthService authService = AppContext.getAuthService();

    @FXML
    public void manejarInicioSesion(ActionEvent e) {
        String user = txtUsuario.getText();
        String pass = txtContrasena.getText();
        try {
            authService.iniciarSesion(user, pass);
            UiRouter.go((Node) e.getSource(), "/fxml/cartelera.fxml");
        } catch (ValidacionException | AutenticacionException ex) {
            mostrarError(ex.getMessage());
        } catch (Exception ex) {
            mostrarError("Ocurrió un error al iniciar sesión.");
        }
    }

    @FXML
    public void abrirRegistro(ActionEvent e) {
        UiRouter.go((Node) e.getSource(), "/fxml/registro.fxml");
    }

    private void mostrarError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText("No se pudo iniciar sesión");
        a.showAndWait();
    }
}
