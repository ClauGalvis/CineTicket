package com.cineticket.controlador;

import com.cineticket.dao.AsientoDAO;
import com.cineticket.modelo.Asiento;
import com.cineticket.modelo.Funcion;
import com.cineticket.modelo.Pelicula;
import com.cineticket.servicio.CarteleraService;
import com.cineticket.servicio.ReservaService;
import com.cineticket.util.AppContext;
import com.cineticket.util.SeleccionFuncionContext;
import com.cineticket.util.SelectedData;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class SeleccionAsientosController {

    @FXML private GridPane gridAsientos;
    @FXML private Label lblPelicula;
    @FXML private Label lblFuncion;
    @FXML private Label lblSala;
    @FXML private Label lblAsientosSeleccionados;
    @FXML private Label lblTotal;
    @FXML private Button btnContinuar;

    private final ReservaService reservaService = AppContext.getReservaService();
    private final CarteleraService carteleraService = AppContext.getCarteleraService();
    private final AsientoDAO asientoDAO = AppContext.getAsientoDAO();

    private Funcion funcionSeleccionada;
    private Pelicula peliculaSeleccionada;

    private final List<Integer> asientosSeleccionados = new ArrayList<>();
    private final Set<Integer> asientosOcupados = new HashSet<>();
    private final Map<Integer, Button> botonesPorAsiento = new HashMap<>();

    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        Integer funcionId = SeleccionFuncionContext.getFuncionActualId();

        if (funcionId == null) {
            mostrarError("No hay función seleccionada.");
            UiRouter.go(gridAsientos, "/fxml/cartelera.fxml");
            return;
        }

        // Cargamos función y película desde el servicio
        funcionSeleccionada = carteleraService.obtenerFuncionPorId(funcionId);
        peliculaSeleccionada =
                carteleraService.obtenerDetallesPelicula(funcionSeleccionada.getPeliculaId());

        // Guardamos en SelectedData para compatibilidad con pantallas siguientes
        SelectedData.setFuncion(funcionSeleccionada);
        SelectedData.setPelicula(peliculaSeleccionada);

        // Encabezados
        lblPelicula.setText(peliculaSeleccionada.getTitulo());

        String inicio = fmtDate(funcionSeleccionada.getFechaHoraInicio());
        lblFuncion.setText(inicio);

        lblSala.setText("Sala " + funcionSeleccionada.getSalaId());

        cargarMapaAsientos();
    }

    // === navegación sidebar / topbar ===

    @FXML
    public void volverCartelera(ActionEvent e) {
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

    // === lógica de asientos ===

    private void cargarMapaAsientos() {
        try {
            List<Asiento> asientosSala = asientoDAO.listarPorSala(funcionSeleccionada.getSalaId());

            asientosOcupados.clear();
            asientosOcupados.addAll(
                    reservaService.obtenerAsientosOcupadosPorFuncion(funcionSeleccionada.getIdFuncion())
            );

            gridAsientos.getChildren().clear();
            gridAsientos.getColumnConstraints().clear();
            gridAsientos.getRowConstraints().clear();
            gridAsientos.setHgap(8);
            gridAsientos.setVgap(8);
            gridAsientos.setPadding(new Insets(16));

            int maxFila = asientosSala.stream()
                    .mapToInt(a -> filaLabelToIndex(a.getFila()))
                    .max().orElse(0);
            int maxCol  = asientosSala.stream()
                    .mapToInt(Asiento::getNumero)
                    .max().orElse(0);

            botonesPorAsiento.clear();

            // Encabezados de columnas (números)
            for (int c = 1; c <= maxCol; c++) {
                Label l = new Label(String.valueOf(c));
                l.getStyleClass().add("seat-header");
                GridPane.setHalignment(l, HPos.CENTER);
                gridAsientos.add(l, c, 0);
            }
            // Encabezados de filas (letras)
            for (int r = 1; r <= maxFila; r++) {
                Label l = new Label(indexToFilaLabel(r));
                l.getStyleClass().add("seat-header");
                gridAsientos.add(l, 0, r);
            }

            // Botones de asiento
            for (Asiento a : asientosSala) {
                int row = filaLabelToIndex(a.getFila());
                int col = a.getNumero();

                Button b = crearBotonAsiento(a);
                gridAsientos.add(b, col, row);
                botonesPorAsiento.put(a.getIdAsiento(), b);

                if (asientosOcupados.contains(a.getIdAsiento())) {
                    aplicarEstiloAsiento(b, "ocupado");
                    b.setDisable(true);
                } else {
                    aplicarEstiloAsiento(b, "libre");
                    b.setDisable(false);
                }
            }

            actualizarInterfaz();

        } catch (Exception ex) {
            mostrarError("No se pudo cargar el mapa de asientos.\n" + ex.getMessage());
        }
    }

    private void manejarSeleccionAsiento(Asiento asiento, Button boton) {
        Integer id = asiento.getIdAsiento();
        if (asientosOcupados.contains(id)) return;

        if (asientosSeleccionados.contains(id)) {
            asientosSeleccionados.remove(id);
            aplicarEstiloAsiento(boton, "libre");
        } else {
            if (asientosSeleccionados.size() >= 5) {
                mostrarAdvertencia("Máximo 5 asientos por compra.");
                return;
            }
            asientosSeleccionados.add(id);
            aplicarEstiloAsiento(boton, "seleccionado");
        }
        actualizarInterfaz();
    }

    @FXML
    public void continuar(ActionEvent e) {
        if (!validarSeleccion()) return;

        boolean disponibles = reservaService.verificarDisponibilidadAsientos(
                funcionSeleccionada.getIdFuncion(), asientosSeleccionados
        );
        if (!disponibles) {
            mostrarAdvertencia("Alguno de los asientos ya no está disponible. Se actualizará el mapa.");
            cargarMapaAsientos();
            return;
        }

        SelectedData.setAsientosSeleccionados(new ArrayList<>(asientosSeleccionados));
        UiRouter.go((Node) e.getSource(), "/fxml/confiteria.fxml");
    }


    @FXML
    public void cancelar(ActionEvent e) {
        UiRouter.go((Node) e.getSource(), "/fxml/cartelera.fxml");
    }

    // ---------- helpers UI ----------

    private void actualizarInterfaz() {
        lblAsientosSeleccionados.setText("Asientos seleccionados: " + formatSeleccion()
                + " (" + asientosSeleccionados.size() + "/5)");
        lblTotal.setText("Total: " + formatMoney(calcularTotal()));
        if (btnContinuar != null) {
            btnContinuar.setDisable(asientosSeleccionados.isEmpty());
        }
    }

    private boolean validarSeleccion() {
        if (asientosSeleccionados.isEmpty()) {
            mostrarAdvertencia("Selecciona al menos un asiento.");
            return false;
        }
        return true;
    }

    private BigDecimal calcularTotal() {
        Double precio = funcionSeleccionada.getPrecioEntrada();
        if (precio == null) precio = 0.0;
        return BigDecimal.valueOf(precio)
                .multiply(BigDecimal.valueOf(asientosSeleccionados.size()));
    }

    private Button crearBotonAsiento(Asiento asiento) {
        String etiqueta = asiento.getFila() + asiento.getNumero();
        Button b = new Button(etiqueta);
        b.setMinSize(32, 28);
        b.setPrefSize(32, 28);
        b.getStyleClass().add("seat-button");
        b.setOnAction(evt -> manejarSeleccionAsiento(asiento, b));
        return b;
    }

    private void aplicarEstiloAsiento(Button boton, String estado) {
        boton.getStyleClass().removeAll("asiento-libre", "asiento-ocupado", "asiento-seleccionado");
        switch (estado) {
            case "libre"        -> boton.getStyleClass().add("asiento-libre");
            case "ocupado"      -> boton.getStyleClass().add("asiento-ocupado");
            case "seleccionado" -> boton.getStyleClass().add("asiento-seleccionado");
        }
    }

    // ---------- helpers varios ----------

    private String fmtDate(Object v) {
        if (v == null) return "?";
        if (v instanceof LocalDateTime ldt) return ldt.format(fmt);
        if (v instanceof java.sql.Timestamp ts) return ts.toLocalDateTime().format(fmt);
        return v.toString();
    }

    private String formatSeleccion() {
        if (asientosSeleccionados.isEmpty()) {
            return "—";
        }
        return asientosSeleccionados.stream()
                .map(id -> Optional.ofNullable(botonesPorAsiento.get(id))
                        .map(Button::getText)
                        .orElse("#" + id))
                .collect(Collectors.joining(", "));
    }

    private String formatMoney(BigDecimal v) {
        return "$ " + String.format("%,d", v.longValue());
    }

    /** A->1, B->2, ..., Z->26, AA->27, etc. */
    private int filaLabelToIndex(String label) {
        if (label == null || label.isBlank()) return 1;
        int n = 0;
        String s = label.trim().toUpperCase(Locale.ROOT);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 'A' || c > 'Z') continue;
            n = n * 26 + (c - 'A' + 1);
        }
        return Math.max(1, n);
    }

    private String indexToFilaLabel(int idx) {
        StringBuilder sb = new StringBuilder();
        int n = idx;
        while (n > 0) {
            int rem = (n - 1) % 26;
            sb.insert(0, (char) ('A' + rem));
            n = (n - 1) / 26;
        }
        return sb.toString();
    }

    private void mostrarError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    private void mostrarAdvertencia(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait();
    }
}
