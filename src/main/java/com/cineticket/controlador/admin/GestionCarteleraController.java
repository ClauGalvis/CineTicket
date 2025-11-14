package com.cineticket.controlador.admin;

import com.cineticket.enums.Clasificacion;
import com.cineticket.excepcion.ValidacionException;
import com.cineticket.modelo.Funcion;
import com.cineticket.modelo.Genero;
import com.cineticket.modelo.Pelicula;
import com.cineticket.servicio.CarteleraService;
import com.cineticket.util.AppContext;
import com.cineticket.util.SessionManager;
import com.cineticket.controlador.UiRouter;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class GestionCarteleraController {

    // ==== UI: Película ====
    @FXML private TextField txtTitulo;
    @FXML private TextField txtDuracion;
    @FXML private TextField txtPosterUrl;
    @FXML private TextArea txtSinopsis;
    @FXML private ComboBox<Genero> cmbGenero;
    @FXML private ComboBox<Clasificacion> cmbClasificacion;

    // ==== UI: Tabla de funciones ====
    @FXML private TableView<FilaFuncion> tablaFunciones;
    @FXML private TableColumn<FilaFuncion, String> colTitulo;
    @FXML private TableColumn<FilaFuncion, String> colFecha;
    @FXML private TableColumn<FilaFuncion, String> colHora;
    @FXML private TableColumn<FilaFuncion, String> colSala;
    @FXML private TableColumn<FilaFuncion, String> colPrecio;
    @FXML private TableColumn<FilaFuncion, Void> colAcciones;
    @FXML private Label lblSinFunciones;

    // ==== Servicios ====
    private final CarteleraService carteleraService = AppContext.getCarteleraService();

    // ==== Estado ====
    private final ObservableList<FilaFuncion> funcionesData = FXCollections.observableArrayList();
    private final DateTimeFormatter fechaFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter horaFmt  = DateTimeFormatter.ofPattern("HH:mm");

    // ==== Ciclo de vida ====

    @FXML
    private void initialize() {
        inicializarClasificaciones();
        inicializarGeneros();
        configurarTablaFunciones();
        cargarFunciones();

        if (tablaFunciones != null) {
            tablaFunciones.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        }
    }

    // ==== Inicialización de combos ====

    private void inicializarClasificaciones() {
        cmbClasificacion.setItems(
                FXCollections.observableArrayList(Clasificacion.values())
        );
    }

    private void inicializarGeneros() {
        List<Genero> generos = carteleraService.obtenerGenerosActivos();
        cmbGenero.setItems(FXCollections.observableArrayList(generos));

        cmbGenero.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Genero g, boolean empty) {
                super.updateItem(g, empty);
                setText(empty || g == null ? "" : g.getNombreGenero());
            }
        });

        cmbGenero.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Genero g, boolean empty) {
                super.updateItem(g, empty);
                setText(empty || g == null ? "" : g.getNombreGenero());
            }
        });
    }

    // ==== Configuración tabla ====

    private void configurarTablaFunciones() {
        if (tablaFunciones == null) {
            return;
        }

        colTitulo.setCellValueFactory(new PropertyValueFactory<>("titulo"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colHora.setCellValueFactory(new PropertyValueFactory<>("hora"));
        colSala.setCellValueFactory(new PropertyValueFactory<>("sala"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));

        tablaFunciones.setItems(funcionesData);

        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnCancelar = new Button("Cancelar");

            {
                btnCancelar.getStyleClass().add("btn-danger");
                btnCancelar.setOnAction(e -> {
                    FilaFuncion fila = getTableView().getItems().get(getIndex());
                    eliminarFuncion(fila);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnCancelar);
            }
        });
    }

    private void cargarFunciones() {
        funcionesData.clear();

        try {
            List<Pelicula> peliculas = carteleraService.obtenerCarteleraCompleta();

            for (Pelicula p : peliculas) {
                List<Funcion> funciones = carteleraService.obtenerFuncionesPorPelicula(p.getIdPelicula());
                for (Funcion f : funciones) {
                    String fecha = f.getFechaHoraInicio().toLocalDate().format(fechaFmt);
                    String hora  = f.getFechaHoraInicio().toLocalTime().format(horaFmt);
                    String sala  = "Sala " + f.getSalaId();
                    String precio = String.format("$ %,d",
                            f.getPrecioEntrada() == null ? 0L : f.getPrecioEntrada().longValue());

                    funcionesData.add(new FilaFuncion(f, p.getTitulo(), fecha, hora, sala, precio));
                }
            }

            lblSinFunciones.setVisible(funcionesData.isEmpty());

        } catch (Exception ex) {
            mostrarError("No se pudieron cargar las funciones.\n" + ex.getMessage());
            lblSinFunciones.setVisible(true);
        }
    }

    // ==== Acciones de película ====

    @FXML
    private void guardarPelicula(ActionEvent e) {
        try {
            String titulo = txtTitulo.getText();
            String duracionStr = txtDuracion.getText();
            Clasificacion clasificacion = cmbClasificacion.getValue();
            Genero generoSel = cmbGenero.getValue();
            String sinopsis = txtSinopsis.getText();
            String posterUrl = txtPosterUrl.getText();

            if (titulo == null || titulo.isBlank()) {
                throw new ValidacionException("El título de la película es obligatorio.");
            }
            if (duracionStr == null || duracionStr.isBlank()) {
                throw new ValidacionException("La duración de la película es obligatoria.");
            }
            if (clasificacion == null) {
                throw new ValidacionException("La clasificación de la película es obligatoria.");
            }

            Integer duracion = Integer.valueOf(duracionStr);

            Pelicula p = new Pelicula();
            p.setTitulo(titulo);
            p.setDuracionMinutos(duracion);
            p.setClasificacion(clasificacion);
            p.setSinopsis(sinopsis);
            p.setImagenUrl(posterUrl);
            p.setFechaEstreno(LocalDate.now());

            List<Integer> generoIds = (generoSel != null)
                    ? List.of(generoSel.getIdGenero())
                    : List.of();

            carteleraService.crearPelicula(p, generoIds);

            mostrarInfo("Película creada correctamente.");
            limpiarFormulario(null);
            cargarFunciones();

        } catch (ValidacionException ve) {
            mostrarError(ve.getMessage());
        } catch (NumberFormatException nfe) {
            mostrarError("La duración debe ser un número entero.");
        } catch (Exception ex) {
            mostrarError("No se pudo guardar la película.\n" + ex.getMessage());
        }
    }

    @FXML
    private void limpiarFormulario(ActionEvent e) {
        txtTitulo.clear();
        txtDuracion.clear();
        txtPosterUrl.clear();
        txtSinopsis.clear();
        cmbGenero.getSelectionModel().clearSelection();
        cmbClasificacion.getSelectionModel().clearSelection();
    }

    // ==== Acciones sobre funciones ====

    private void eliminarFuncion(FilaFuncion fila) {
        Funcion f = fila.getFuncion();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancelar función");
        confirm.setHeaderText("¿Cancelar la función seleccionada?");
        confirm.setContentText("La función dejará de estar disponible para venta de entradas.");
        confirm.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);

        confirm.showAndWait()
                .filter(r -> r == ButtonType.OK)
                .ifPresent(r -> {
                    try {
                        carteleraService.eliminarFuncion(f.getIdFuncion());
                        funcionesData.remove(fila);
                        lblSinFunciones.setVisible(funcionesData.isEmpty());
                    } catch (ValidacionException ve) {
                        mostrarError(ve.getMessage());
                    } catch (Exception ex) {
                        mostrarError("No se pudo cancelar la función.\n" + ex.getMessage());
                    }
                });
    }

    @FXML
    private void abrirDialogoNuevaFuncion(ActionEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/admin/nueva_funcion.fxml")
            );
            Parent root = loader.load();

            NuevaFuncionController controller = loader.getController();
            controller.setOnFuncionCreada(this::cargarFunciones);

            Stage stage = new Stage();
            stage.setTitle("Agregar función");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(((Node) e.getSource()).getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException ex) {
            mostrarError("No se pudo abrir el formulario de nueva función.\n" + ex.getMessage());
        }
    }

    // ==== Navegación desde sidebar / topbar ====

    @FXML
    private void abrirReportesVentas(ActionEvent e) {
        UiRouter.go((Node) e.getSource(), "/fxml/admin/reportes_ventas.fxml");
    }

    @FXML
    private void cerrarSesion(ActionEvent e) {
        SessionManager.getInstance().cerrarSesion();
        UiRouter.go((Node) e.getSource(), "/fxml/login.fxml");
    }

    // ==== Helpers de mensajes ====

    private void mostrarInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Información");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void mostrarError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText("Ha ocurrido un problema");
        a.setContentText(msg);
        a.showAndWait();
    }

    // ==== ViewModel de la tabla de funciones ====

    public static class FilaFuncion {
        private final Funcion funcion;
        private final StringProperty titulo   = new SimpleStringProperty();
        private final StringProperty fecha    = new SimpleStringProperty();
        private final StringProperty hora     = new SimpleStringProperty();
        private final StringProperty sala     = new SimpleStringProperty();
        private final StringProperty precio   = new SimpleStringProperty();

        public FilaFuncion(Funcion funcion,
                           String titulo,
                           String fecha,
                           String hora,
                           String sala,
                           String precio) {
            this.funcion = funcion;
            this.titulo.set(titulo);
            this.fecha.set(fecha);
            this.hora.set(hora);
            this.sala.set(sala);
            this.precio.set(precio);
        }

        public Funcion getFuncion() {
            return funcion;
        }

        public String getTitulo() { return titulo.get(); }
        public StringProperty tituloProperty() { return titulo; }

        public String getFecha() { return fecha.get(); }
        public StringProperty fechaProperty() { return fecha; }

        public String getHora() { return hora.get(); }
        public StringProperty horaProperty() { return hora; }

        public String getSala() { return sala.get(); }
        public StringProperty salaProperty() { return sala; }

        public String getPrecio() { return precio.get(); }
        public StringProperty precioProperty() { return precio; }
    }
}
