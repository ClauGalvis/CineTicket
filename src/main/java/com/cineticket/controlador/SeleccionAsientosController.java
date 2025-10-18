package com.cineticket.controlador;

import com.cineticket.modelo.Asiento;
import com.cineticket.modelo.Funcion;
import com.cineticket.modelo.Pelicula;
import com.cineticket.servicio.ReservaService;
import com.cineticket.util.AppContext;
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

/**
 * Selección de asientos: genera una grilla por sala, marca ocupados, permite seleccionar hasta 5.
 */
public class SeleccionAsientosController {

    @FXML private GridPane gridAsientos;
    @FXML private Label lblPelicula;
    @FXML private Label lblFuncion;
    @FXML private Label lblSala;
    @FXML private Label lblAsientosSeleccionados;
    @FXML private Label lblTotal;

    private final ReservaService reservaService = AppContext.getReservaService();

    private Funcion funcionSeleccionada;
    private final List<Integer> asientosSeleccionados = new ArrayList<>();
    private final Set<Integer> asientosOcupados = new HashSet<>();
    private final Map<Integer, Button> botonesPorAsiento = new HashMap<>();

    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        // Recuperamos lo seleccionado previamente en Cartelera
        this.funcionSeleccionada = SelectedData.getFuncion();
        Pelicula p = SelectedData.getPelicula();

        if (funcionSeleccionada == null) {
            new Alert(Alert.AlertType.ERROR, "No hay función seleccionada.", ButtonType.OK).showAndWait();
            UiRouter.go(gridAsientos, "/fxml/cartelera.fxml");
            return;
        }

        // Encabezados
        lblPelicula.setText(p != null ? p.getTitulo() : "(Película)");

        String inicio = fmtDate(funcionSeleccionada.getFechaHoraInicio());
        String fin    = fmtDate(funcionSeleccionada.getFechaHoraFin());
        lblFuncion.setText(inicio + " - " + fin);

        // Si tu VO no trae objeto Sala, mostramos el id
        lblSala.setText("Sala " + funcionSeleccionada.getSalaId());

        setFuncion(funcionSeleccionada);
    }

    /** Permite recargar si se cambia la función. */
    public void setFuncion(Funcion funcion) {
        this.funcionSeleccionada = funcion;
        cargarMapaAsientos();
    }

    /** 1) Asientos de la sala  2) Ocupados  3) Genera grilla  4) Estilos */
    public void cargarMapaAsientos() {
        try {
            var asientoDAO = AppContext.getAsientoDAO();
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
            gridAsientos.setPadding(new Insets(10));

            int maxFila = asientosSala.stream().mapToInt(a -> filaLabelToIndex(a.getFila())).max().orElse(0);
            int maxCol  = asientosSala.stream().mapToInt(Asiento::getNumero).max().orElse(0);

            botonesPorAsiento.clear();

            // Encabezados
            for (int c = 1; c <= maxCol; c++) {
                Label l = new Label(String.valueOf(c));
                GridPane.setHalignment(l, HPos.CENTER);
                gridAsientos.add(l, c, 0);
            }
            for (int r = 1; r <= maxFila; r++) {
                gridAsientos.add(new Label(indexToFilaLabel(r)), 0, r);
            }

            // Botones
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
            new Alert(Alert.AlertType.ERROR,
                    "No se pudo cargar el mapa de asientos.\n" + ex.getMessage(), ButtonType.OK).showAndWait();
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
                new Alert(Alert.AlertType.WARNING, "Máximo 5 asientos por compra.", ButtonType.OK).showAndWait();
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
            new Alert(Alert.AlertType.WARNING,
                    "Alguno de los asientos ya no está disponible. Se actualizará el mapa.",
                    ButtonType.OK).showAndWait();
            cargarMapaAsientos();
            return;
        }

        // Guardamos para la siguiente pantalla (confitería/confirmación)
        SelectedData.setAsientosSeleccionados(new ArrayList<>(asientosSeleccionados));

        UiRouter.go((Node) e.getSource(), "/fxml/confiteria.fxml");
    }

    @FXML
    public void cancelar(ActionEvent e) {
        UiRouter.go((Node) e.getSource(), "/fxml/cartelera.fxml");
    }

    // ---------- helpers UI ----------

    private void actualizarInterfaz() {
        lblAsientosSeleccionados.setText("Seleccionados: " + formatSeleccion()
                + " (" + asientosSeleccionados.size() + "/5)");
        lblTotal.setText("Total: " + formatMoney(calcularTotal()));
    }

    private boolean validarSeleccion() {
        if (asientosSeleccionados.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Selecciona al menos un asiento.", ButtonType.OK).showAndWait();
            return false;
        }
        return true;
    }

    private BigDecimal calcularTotal() {
        Double precio = funcionSeleccionada.getPrecioEntrada();
        if (precio == null) precio = 0.0;
        return BigDecimal.valueOf(precio).multiply(BigDecimal.valueOf(asientosSeleccionados.size()));
    }

    private Button crearBotonAsiento(Asiento asiento) {
        String etiqueta = asiento.getFila() + asiento.getNumero();
        Button b = new Button(etiqueta);
        b.setMinSize(36, 28);
        b.setPrefSize(36, 28);
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
        return asientosSeleccionados.stream()
                .map(id -> Optional.ofNullable(botonesPorAsiento.get(id)).map(Button::getText).orElse("#"+id))
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
}
