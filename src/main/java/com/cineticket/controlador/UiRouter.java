package com.cineticket.controlador;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Utilidad para cambiar entre escenas (FXML) dentro de la misma ventana.
 * Incluye manejo de errores detallado para detectar problemas de carga.
 */
public class UiRouter {

    /**
     * Cambia la escena actual por la especificada.
     *
     * @param source   Cualquier nodo perteneciente a la escena actual (por ejemplo, un botón)
     * @param fxmlPath Ruta del archivo FXML (por ejemplo "/fxml/cartelera.fxml")
     */
    public static void go(Node source, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(UiRouter.class.getResource(fxmlPath));
            if (loader.getLocation() == null) {
                throw new IllegalStateException("FXML no encontrado: " + fxmlPath);
            }

            Parent root = loader.load();
            Stage stage = (Stage) source.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();

        } catch (Exception e) {
            System.err.println("❌ Error al cargar FXML: " + fxmlPath);
            e.printStackTrace();
        }
    }
}
