package com.cineticket.controlador;

import com.cineticket.modelo.Pelicula;
import com.cineticket.modelo.Genero;
import com.cineticket.servicio.CarteleraService;
import com.cineticket.util.AppContext;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.event.ActionEvent;
import com.cineticket.util.SeleccionPeliculaContext;

import java.util.List;

public class CarteleraController {

    @FXML private FlowPane gridPeliculas;

    private final CarteleraService carteleraService = AppContext.getCarteleraService();

    @FXML
    public void initialize() {
        cargarPeliculas();

        gridPeliculas.widthProperty().addListener((obs, oldV, newV) -> {
            ajustarAnchoTarjetas(newV.doubleValue());
        });
    }

    private void ajustarAnchoTarjetas(double anchoFlow) {

        double hgap = gridPeliculas.getHgap();
        double paddingExtra = 40;
        int columnas = 3;

        double anchoTarjeta = (anchoFlow - (hgap * (columnas - 1)) - paddingExtra) / columnas;

        double minAncho = 260;
        double maxAncho = 420;

        if (anchoTarjeta < minAncho) anchoTarjeta = minAncho;
        if (anchoTarjeta > maxAncho) anchoTarjeta = maxAncho;

        for (Node n : gridPeliculas.getChildren()) {
            if (n instanceof VBox card) {
                card.setPrefWidth(anchoTarjeta);
                card.setMaxWidth(anchoTarjeta); // 🔴 fija el ancho

                // primer hijo = imagen
                if (!card.getChildren().isEmpty() && card.getChildren().get(0) instanceof ImageView img) {
                    double imgWidth = anchoTarjeta - 20;
                    if (imgWidth < 0) imgWidth = anchoTarjeta;
                    img.setFitWidth(imgWidth);
                    img.setPreserveRatio(true);

                    // opcional: límite de alto para que no sea un póster kilométrico
                    img.setFitHeight(260);
                }
            }
        }
    }




    private void cargarPeliculas() {
        gridPeliculas.getChildren().clear();

        List<Pelicula> peliculas = carteleraService.obtenerCarteleraCompleta();

        for (Pelicula p : peliculas) {
            Node tarjeta = crearTarjetaPelicula(p);
            gridPeliculas.getChildren().add(tarjeta);
        }

        ajustarAnchoTarjetas(gridPeliculas.getWidth());
    }

    private Node crearTarjetaPelicula(Pelicula peli) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(10));
        card.getStyleClass().add("pelicula-card");

        ImageView img = new ImageView();
        img.setPreserveRatio(true);
        img.setSmooth(true);

        try {
            if (peli.getImagenUrl() != null) {
                img.setImage(new Image(peli.getImagenUrl(), true));
            } else {
                img.setImage(new Image(getClass().getResource("/img/no_image.png").toExternalForm()));
            }
        } catch (Exception ex) {
            img.setImage(new Image(getClass().getResource("/img/no_image.png").toExternalForm()));
        }

        Label lblTitulo = new Label(peli.getTitulo());
        lblTitulo.getStyleClass().add("pelicula-card-title");

        HBox generosBox = new HBox(6);
        List<Genero> generos = carteleraService.obtenerGenerosDePelicula(peli.getIdPelicula());
        for (Genero g : generos) {
            Label chip = new Label(g.getNombreGenero());
            chip.getStyleClass().add("chip");
            generosBox.getChildren().add(chip);
        }

        Label lblDuracion = new Label("Duración: " + peli.getDuracionFormateada());
        lblDuracion.getStyleClass().add("pelicula-card-detail");

        Button btnVer = new Button("Ver detalles");
        btnVer.getStyleClass().add("pelicula-ver-button");
        btnVer.setOnAction(ev -> abrirDetalles(ev, peli.getIdPelicula()));

        card.getChildren().addAll(img, lblTitulo, generosBox, lblDuracion, btnVer);

        return card;
    }


    private void abrirDetalles(ActionEvent e, Integer peliculaId) {
        SeleccionPeliculaContext.setPeliculaActualId(peliculaId);
        UiRouter.go((Node) e.getSource(), "/fxml/detalle_pelicula.fxml");
    }

    @FXML
    public void abrirHistorial(ActionEvent e) {
        UiRouter.go((Node) e.getSource(), "/fxml/historial.fxml");
    }

    @FXML
    public void cerrarSesion(ActionEvent e) {
        UiRouter.go((Node) e.getSource(), "/fxml/login.fxml");
    }
}
