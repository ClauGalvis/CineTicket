package com.cineticket.controlador;

import com.cineticket.modelo.Funcion;
import com.cineticket.modelo.Genero;
import com.cineticket.modelo.Pelicula;
import com.cineticket.servicio.CarteleraService;
import com.cineticket.util.AppContext;
import com.cineticket.util.SeleccionPeliculaContext;
import com.cineticket.util.SeleccionFuncionContext;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;



public class DetallePeliculaController {
    @FXML private HBox detalleLayout;

    @FXML private ImageView imgPoster;

    @FXML private Label lblTitulo;
    @FXML private FlowPane generosFlow;
    @FXML private Label lblDuracion;
    @FXML private Label lblClasificacion;
    @FXML private Label lblFechaEstreno;
    @FXML private Label lblSinopsis;

    @FXML private FlowPane diasFlow;
    @FXML private FlowPane horariosFlow;
    @FXML private Button btnSeleccionarAsientos;

    private final CarteleraService carteleraService = AppContext.getCarteleraService();

    /** Funciones agrupadas por día. */
    private final Map<LocalDate, List<Funcion>> funcionesPorDia = new LinkedHashMap<>();

    private final ToggleGroup grupoDias = new ToggleGroup();
    private final ToggleGroup grupoHorarios = new ToggleGroup();

    private static final DateTimeFormatter FECHA_UI =
            DateTimeFormatter.ofPattern("EEE dd MMM", new Locale("es", "CO"));
    private static final DateTimeFormatter HORA_UI =
            DateTimeFormatter.ofPattern("hh:mm a", new Locale("es", "CO"));

    @FXML
    public void initialize() {
        // Ajuste sencillo del espacio entre columnas según el ancho
        if (detalleLayout != null) {
            detalleLayout.widthProperty().addListener((obs, oldW, newW) -> {
                double w = newW.doubleValue();
                if (w < 700) {
                    detalleLayout.setSpacing(16);
                } else {
                    detalleLayout.setSpacing(32);
                }
            });
        }

        Integer peliculaId = SeleccionPeliculaContext.getPeliculaActualId();
        if (peliculaId == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Película no seleccionada");
            alert.setContentText("Por favor selecciona una película desde la cartelera.");
            alert.showAndWait();
            return;
        }

        cargarDetallesPelicula(peliculaId);
        cargarFunciones(peliculaId);
    }

    private void cargarDetallesPelicula(Integer peliculaId) {
        Pelicula p = carteleraService.obtenerDetallesPelicula(peliculaId);

        lblTitulo.setText(p.getTitulo());
        lblDuracion.setText(p.getDuracionFormateada());
        lblClasificacion.setText(p.getClasificacion().name());

        if (p.getFechaEstreno() != null) {
            lblFechaEstreno.setText(p.getFechaEstreno().toString());
        } else {
            lblFechaEstreno.setText("Próximamente");
        }

        String sinopsis = p.getSinopsis();
        if (sinopsis == null || sinopsis.isBlank()) {
            lblSinopsis.setText("Sinopsis no disponible.");
        } else {
            lblSinopsis.setText(sinopsis);
        }

        // Imagen
        try {
            if (p.getImagenUrl() != null) {
                imgPoster.setImage(new Image(p.getImagenUrl(), true));
            } else {
                imgPoster.setImage(new Image(
                        getClass().getResource("/img/no_image.png").toExternalForm()
                ));
            }
        } catch (Exception ex) {
            imgPoster.setImage(new Image(
                    getClass().getResource("/img/no_image.png").toExternalForm()
            ));
        }

        // Géneros como chips
        generosFlow.getChildren().clear();
        for (Genero g : carteleraService.obtenerGenerosDePelicula(peliculaId)) {
            Label chip = new Label(g.getNombreGenero());
            chip.getStyleClass().add("chip");
            generosFlow.getChildren().add(chip);
        }
    }

    private void cargarFunciones(Integer peliculaId) {
        List<Funcion> funciones = carteleraService.obtenerFuncionesPorPelicula(peliculaId);
        funciones.sort(Comparator.comparing(Funcion::getFechaHoraInicio));

        funcionesPorDia.clear();
        for (Funcion f : funciones) {
            LocalDate dia = f.getFechaHoraInicio().toLocalDate();
            funcionesPorDia
                    .computeIfAbsent(dia, d -> new ArrayList<>())
                    .add(f);
        }

        construirBotonesDias();
    }

    private void construirBotonesDias() {
        diasFlow.getChildren().clear();
        grupoDias.getToggles().clear();

        if (funcionesPorDia.isEmpty()) {
            Label lbl = new Label("No hay funciones disponibles para esta película.");
            lbl.getStyleClass().add("detalle-empty-text");
            diasFlow.getChildren().add(lbl);
            btnSeleccionarAsientos.setDisable(true);
            return;
        }

        btnSeleccionarAsientos.setDisable(false);

        for (LocalDate dia : funcionesPorDia.keySet()) {
            ToggleButton btnDia = new ToggleButton(FECHA_UI.format(dia));
            btnDia.getStyleClass().add("day-chip");
            btnDia.setToggleGroup(grupoDias);
            btnDia.setUserData(dia);
            btnDia.setOnAction(ev -> mostrarHorariosParaDia(dia));
            diasFlow.getChildren().add(btnDia);
        }

        // Seleccionar primer día por defecto
        if (!grupoDias.getToggles().isEmpty()) {
            grupoDias.selectToggle(grupoDias.getToggles().get(0));
            LocalDate primerDia = (LocalDate) grupoDias.getSelectedToggle().getUserData();
            mostrarHorariosParaDia(primerDia);
        }
    }

    private void mostrarHorariosParaDia(LocalDate dia) {
        horariosFlow.getChildren().clear();
        grupoHorarios.getToggles().clear();

        List<Funcion> funcionesDia = funcionesPorDia.getOrDefault(dia, Collections.emptyList());
        funcionesDia.sort(Comparator.comparing(Funcion::getFechaHoraInicio));

        for (Funcion f : funcionesDia) {
            LocalDateTime fh = f.getFechaHoraInicio();
            String textoHora = HORA_UI.format(fh);

            ToggleButton btnHora = new ToggleButton(textoHora);
            btnHora.getStyleClass().add("time-chip");
            btnHora.setToggleGroup(grupoHorarios);
            btnHora.setUserData(f);

            horariosFlow.getChildren().add(btnHora);
        }
    }

    private Funcion obtenerFuncionSeleccionada() {
        Toggle selected = grupoHorarios.getSelectedToggle();
        if (selected == null) {
            return null;
        }
        return (Funcion) selected.getUserData();
    }

    @FXML
    private void abrirSeleccionAsientos(ActionEvent e) {
        Funcion funcion = obtenerFuncionSeleccionada();
        if (funcion == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setHeaderText(null);
            alert.setContentText("Por favor selecciona un horario antes de continuar.");
            alert.showAndWait();
            return;
        }

        SeleccionFuncionContext.setFuncionActualId(funcion.getIdFuncion());
        UiRouter.go((Node) e.getSource(), "/fxml/seleccion-asientos.fxml");
    }

    @FXML
    private void volverCartelera(ActionEvent e) {
        UiRouter.go((Node) e.getSource(), "/fxml/cartelera.fxml");
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
