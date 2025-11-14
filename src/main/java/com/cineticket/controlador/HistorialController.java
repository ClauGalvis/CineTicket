package com.cineticket.controlador;

import com.cineticket.modelo.Compra;
import com.cineticket.modelo.CompraConfiteria;
import com.cineticket.modelo.Entrada;
import com.cineticket.modelo.Usuario;
import com.cineticket.servicio.CompraService;
import com.cineticket.servicio.ConfiteriaService;
import com.cineticket.modelo.ComboConfiteria;
import com.cineticket.util.AppContext;
import com.cineticket.util.SessionManager;
import com.cineticket.excepcion.ValidacionException;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class HistorialController {

    // UI
    @FXML private TableView<FilaHistorial> tablaCompras;
    @FXML private TableColumn<FilaHistorial, String> colPelicula;
    @FXML private TableColumn<FilaHistorial, String> colFecha;
    @FXML private TableColumn<FilaHistorial, String> colHora;
    @FXML private TableColumn<FilaHistorial, String> colAsientos;
    @FXML private TableColumn<FilaHistorial, String> colConfiteria;
    @FXML private TableColumn<FilaHistorial, Void> colCancelar;
    @FXML private TableColumn<FilaHistorial, Void> colDescargar;
    @FXML private Label lblVacio;

    // Servicios
    private final CompraService compraService = AppContext.getCompraService();
    private final ConfiteriaService confiteriaService = AppContext.getConfiteriaService();

    private final DateTimeFormatter fechaFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter horaFmt = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private void initialize() {
        configurarColumnas();
        cargarHistorial();
    }

    private void configurarColumnas() {
        colPelicula.setCellValueFactory(new PropertyValueFactory<>("pelicula"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colHora.setCellValueFactory(new PropertyValueFactory<>("hora"));
        colAsientos.setCellValueFactory(new PropertyValueFactory<>("asientos"));
        colConfiteria.setCellValueFactory(new PropertyValueFactory<>("confiteria"));

        // Botón Cancelar por fila
        colCancelar.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Cancel");

            {
                btn.getStyleClass().add("btn-danger");
                btn.setOnAction(e -> {
                    FilaHistorial fila = getTableView().getItems().get(getIndex());
                    manejarCancelarCompra(fila);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    FilaHistorial fila = getTableView().getItems().get(getIndex());
                    boolean puedeCancelar = fila.getCompra().estaConfirmada();
                    btn.setDisable(!puedeCancelar);
                    setGraphic(btn);
                }
            }
        });

        // Botón Download por fila
        colDescargar.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Download");

            {
                btn.getStyleClass().add("btn-outline");
                btn.setOnAction(e -> {
                    FilaHistorial fila = getTableView().getItems().get(getIndex());
                    manejarDescargarPdf(fila);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void cargarHistorial() {
        Usuario actual = SessionManager.getInstance().getUsuarioActual();
        if (actual == null) {
            mostrarAlerta(Alert.AlertType.ERROR,
                    "Sesión",
                    "No hay sesión activa",
                    "Por favor inicia sesión nuevamente.");
            return;
        }

        List<Compra> compras = compraService.obtenerHistorialCompras(actual.getIdUsuario());
        if (compras == null || compras.isEmpty()) {
            lblVacio.setVisible(true);
            tablaCompras.setItems(FXCollections.observableArrayList());
            return;
        }

        lblVacio.setVisible(false);

        ObservableList<FilaHistorial> filas = FXCollections.observableArrayList();

        for (Compra c : compras) {
            Integer compraId = c.getIdCompra();

            List<Entrada> entradas = compraService.obtenerEntradasDeCompra(compraId);
            List<CompraConfiteria> items = compraService.obtenerConfiteriaDeCompra(compraId);

            String pelicula = obtenerTituloPelicula(entradas);
            LocalDateTime fh = c.getFechaHoraCompra();
            String fecha = fh != null ? fh.format(fechaFmt) : "";
            String hora = fh != null ? fh.format(horaFmt) : "";
            String asientos = construirDescripcionAsientos(entradas);
            String confiteria = construirDescripcionConfiteria(items);

            filas.add(new FilaHistorial(c, pelicula, fecha, hora, asientos, confiteria));
        }

        tablaCompras.setItems(filas);
    }

    /**
     * Por ahora: si tenemos entradas, usamos el ID de función como texto auxiliar.
     * Si luego expones un método en CarteleraService para obtener el título de la película
     * a partir de la función, aquí se cambia por el título real.
     */
    private String obtenerTituloPelicula(List<Entrada> entradas) {
        if (entradas == null || entradas.isEmpty()) {
            return "Compra sin entradas";
        }
        // TODO: reemplazar por búsqueda real de película (Funcion → Pelicula)
        Integer funcionId = entradas.get(0).getFuncionId();
        return "Función #" + funcionId;
    }

    private String construirDescripcionAsientos(List<Entrada> entradas) {
        if (entradas == null || entradas.isEmpty()) {
            return "Sin asientos";
        }
        int n = entradas.size();
        return n == 1 ? "1 asiento" : n + " asientos";
    }

    private String construirDescripcionConfiteria(List<CompraConfiteria> items) {
        if (items == null || items.isEmpty()) {
            return "Ninguno";
        }

        return items.stream()
                .map(ci -> {
                    try {
                        ComboConfiteria combo = confiteriaService.obtenerCombo(ci.getComboId());
                        String nombre = combo != null ? combo.getNombreCombo() : ("Combo " + ci.getComboId());
                        return ci.getCantidad() + "x " + nombre;
                    } catch (Exception e) {
                        return ci.getCantidad() + "x Combo " + ci.getComboId();
                    }
                })
                .collect(Collectors.joining(", "));
    }

    // ==== Acciones por fila ====

    private void manejarCancelarCompra(FilaHistorial fila) {
        Compra compra = fila.getCompra();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancelar compra");
        confirm.setHeaderText("¿Cancelar esta compra?");
        confirm.setContentText("Se liberarán los asientos asociados si la función aún no ha iniciado.");
        confirm.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

        confirm.showAndWait()
                .filter(res -> res == ButtonType.OK)
                .ifPresent(res -> {
                    try {
                        boolean ok = compraService.cancelarCompra(compra.getIdCompra());
                        if (ok) {
                            mostrarAlerta(Alert.AlertType.INFORMATION,
                                    "Compra cancelada",
                                    null,
                                    "La compra se canceló correctamente.");
                            cargarHistorial();
                        } else {
                            mostrarAlerta(Alert.AlertType.WARNING,
                                    "Cancelación incompleta",
                                    null,
                                    "No fue posible cancelar completamente la compra.");
                        }
                    } catch (ValidacionException ex) {
                        mostrarAlerta(Alert.AlertType.WARNING,
                                "No se puede cancelar",
                                null,
                                ex.getMessage());
                    } catch (Exception ex) {
                        mostrarAlerta(Alert.AlertType.ERROR,
                                "Error",
                                "Ocurrió un error al cancelar la compra.",
                                ex.getMessage());
                    }
                });
    }

    private void manejarDescargarPdf(FilaHistorial fila) {
        Compra compra = fila.getCompra();
        Integer compraId = compra.getIdCompra();

        try {
            // 1) regenerar/obtener comprobante
            String rutaOrigen = compraService.generarComprobante(compraId);
            if (rutaOrigen == null || rutaOrigen.isBlank()) {
                mostrarAlerta(Alert.AlertType.ERROR,
                        "Comprobante",
                        null,
                        "No se pudo generar el comprobante PDF.");
                return;
            }

            // 2) FileChooser para guardar
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Guardar comprobante");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF", "*.pdf")
            );
            chooser.setInitialFileName("comprobante_compra_" + compraId + ".pdf");

            Stage stage = (Stage) tablaCompras.getScene().getWindow();
            File destino = chooser.showSaveDialog(stage);
            if (destino == null) {
                return; // usuario canceló
            }

            Files.copy(Path.of(rutaOrigen), destino.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            mostrarAlerta(Alert.AlertType.INFORMATION,
                    "Comprobante guardado",
                    null,
                    "El comprobante se guardó correctamente.");
        } catch (Exception ex) {
            mostrarAlerta(Alert.AlertType.ERROR,
                    "Error al guardar comprobante",
                    null,
                    ex.getMessage());
        }
    }

    // ==== Navegación / sesión ====

    @FXML
    private void irACartelera(ActionEvent event) {
        try {
            Stage stage = (Stage) tablaCompras.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(getClass().getResource("/fxml/cartelera.fxml")));
            stage.setScene(new Scene(loader.load()));
            stage.centerOnScreen();
        } catch (Exception ex) {
            mostrarAlerta(Alert.AlertType.ERROR,
                    "Navegación",
                    "No se pudo abrir la cartelera.",
                    ex.getMessage());
        }
    }

    @FXML
    private void cerrarSesion(ActionEvent event) {
        SessionManager.getInstance().cerrarSesion();
        try {
            Stage stage = (Stage) tablaCompras.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(getClass().getResource("/fxml/login.fxml")));
            stage.setScene(new Scene(loader.load()));
            stage.centerOnScreen();
        } catch (Exception ex) {
            mostrarAlerta(Alert.AlertType.ERROR,
                    "Sesión",
                    "Sesión cerrada, pero no se pudo volver a la pantalla de login.",
                    ex.getMessage());
        }
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo,
                               String header, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(header);
        alert.setContentText(mensaje);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }

    // ==== ViewModel para la tabla ====

    public static class FilaHistorial {
        private final Compra compra;
        private final StringProperty pelicula = new SimpleStringProperty();
        private final StringProperty fecha = new SimpleStringProperty();
        private final StringProperty hora = new SimpleStringProperty();
        private final StringProperty asientos = new SimpleStringProperty();
        private final StringProperty confiteria = new SimpleStringProperty();

        public FilaHistorial(Compra compra,
                             String pelicula,
                             String fecha,
                             String hora,
                             String asientos,
                             String confiteria) {
            this.compra = compra;
            this.pelicula.set(pelicula);
            this.fecha.set(fecha);
            this.hora.set(hora);
            this.asientos.set(asientos);
            this.confiteria.set(confiteria);
        }

        public Compra getCompra() {
            return compra;
        }

        public String getPelicula() {
            return pelicula.get();
        }

        public StringProperty peliculaProperty() {
            return pelicula;
        }

        public String getFecha() {
            return fecha.get();
        }

        public StringProperty fechaProperty() {
            return fecha;
        }

        public String getHora() {
            return hora.get();
        }

        public StringProperty horaProperty() {
            return hora;
        }

        public String getAsientos() {
            return asientos.get();
        }

        public StringProperty asientosProperty() {
            return asientos;
        }

        public String getConfiteria() {
            return confiteria.get();
        }

        public StringProperty confiteriaProperty() {
            return confiteria;
        }
    }
}
