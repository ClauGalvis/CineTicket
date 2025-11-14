package com.cineticket.controlador.admin;

import com.cineticket.excepcion.ValidacionException;
import com.cineticket.modelo.Funcion;
import com.cineticket.modelo.Pelicula;
import com.cineticket.servicio.CarteleraService;
import com.cineticket.util.AppContext;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class NuevaFuncionController {

    @FXML private ComboBox<Pelicula> cmbPelicula;
    @FXML private DatePicker dpFecha;
    @FXML private TextField txtHoraInicio; // formato HH:mm
    @FXML private TextField txtSalaId;
    @FXML private TextField txtPrecio;
    @FXML private Button btnGuardar;

    private final CarteleraService carteleraService = AppContext.getCarteleraService();

    private Runnable onFuncionCreada;

    private final DateTimeFormatter horaFmt = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private void initialize() {
        inicializarPeliculas();
    }

    private void inicializarPeliculas() {
        List<Pelicula> peliculas = carteleraService.obtenerCarteleraCompleta();
        cmbPelicula.setItems(FXCollections.observableArrayList(peliculas));

        cmbPelicula.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Pelicula p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? "" : p.getTitulo());
            }
        });

        cmbPelicula.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Pelicula p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? "" : p.getTitulo());
            }
        });
    }

    public void setOnFuncionCreada(Runnable onFuncionCreada) {
        this.onFuncionCreada = onFuncionCreada;
    }

    @FXML
    private void guardar(ActionEvent e) {
        try {
            Pelicula peli = cmbPelicula.getValue();
            LocalDate fecha = dpFecha.getValue();
            String horaStr = txtHoraInicio.getText();
            String salaStr = txtSalaId.getText();
            String precioStr = txtPrecio.getText();

            if (peli == null) {
                throw new ValidacionException("Debes seleccionar una película.");
            }
            if (fecha == null) {
                throw new ValidacionException("Debes seleccionar una fecha.");
            }
            if (horaStr == null || horaStr.isBlank()) {
                throw new ValidacionException("Debes ingresar la hora de inicio (HH:mm).");
            }
            if (salaStr == null || salaStr.isBlank()) {
                throw new ValidacionException("Debes ingresar el ID de sala.");
            }
            if (precioStr == null || precioStr.isBlank()) {
                throw new ValidacionException("Debes ingresar el precio de la entrada.");
            }

            LocalTime horaInicio;
            try {
                horaInicio = LocalTime.parse(horaStr, horaFmt);
            } catch (DateTimeParseException ex) {
                throw new ValidacionException("La hora debe tener el formato HH:mm, por ejemplo 19:30.");
            }

            Integer salaId;
            try {
                salaId = Integer.valueOf(salaStr);
            } catch (NumberFormatException ex) {
                throw new ValidacionException("El ID de sala debe ser un número entero.");
            }

            Double precio;
            try {
                precio = Double.valueOf(precioStr);
            } catch (NumberFormatException ex) {
                throw new ValidacionException("El precio debe ser un número válido.");
            }

            LocalDateTime inicio = LocalDateTime.of(fecha, horaInicio);
            Integer duracion = peli.getDuracionMinutos();
            if (duracion == null || duracion <= 0) {
                throw new ValidacionException("La duración de la película no es válida.");
            }
            LocalDateTime fin = inicio.plusMinutes(duracion);

            Funcion f = new Funcion();
            f.setPeliculaId(peli.getIdPelicula());
            f.setSalaId(salaId);
            f.setFechaHoraInicio(inicio);
            f.setFechaHoraFin(fin);
            f.setPrecioEntrada(precio);

            carteleraService.crearFuncion(f);

            mostrarInfo("Función creada correctamente.");
            if (onFuncionCreada != null) {
                onFuncionCreada.run();
            }
            cerrarVentana();

        } catch (ValidacionException ve) {
            mostrarError(ve.getMessage());
        } catch (Exception ex) {
            mostrarError("No se pudo crear la función.\n" + ex.getMessage());
        }
    }

    @FXML
    private void cancelar(ActionEvent e) {
        cerrarVentana();
    }

    private void cerrarVentana() {
        Stage stage = (Stage) btnGuardar.getScene().getWindow();
        stage.close();
    }

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
}
