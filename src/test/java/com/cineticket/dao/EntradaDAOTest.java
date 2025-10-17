package com.cineticket.dao;

import com.cineticket.dao.impl.EntradaDAOImpl;
import com.cineticket.enums.EstadoEntrada;
import com.cineticket.enums.MetodoPago;
import com.cineticket.enums.TipoAsiento;
import com.cineticket.modelo.Entrada;
import com.cineticket.util.ConnectionPool;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

import static java.lang.System.out;

public class EntradaDAOTest {

    private static final boolean KEEP_DATA = true; // true -> conserva datos para inspección

    public static void main(String[] args) {
        EntradaDAO dao = new EntradaDAOImpl();

        Integer usuarioId = null, compraId = null, salaId = null, asientoId = null, funcionId = null;

        try {
            // ==== FIXTURES MÍNIMOS ====
            usuarioId = ensureUsuario("entradas.demo@cineticket.com");
            salaId    = ensureSala("Sala Entradas Test", 5, 5);
            asientoId = ensureAsiento(salaId, "B", 3, TipoAsiento.REGULAR);
            int peliculaId = ensurePelicula("Peli Entradas Test");
            funcionId = ensureFuncion(peliculaId, salaId,
                    LocalDateTime.now().plusHours(5).withSecond(0).withNano(0),
                    LocalDateTime.now().plusHours(7).withSecond(0).withNano(0),
                    18000.0);
            compraId  = ensureCompra(usuarioId, new BigDecimal("18000.00"), BigDecimal.ZERO, MetodoPago.PSE);

            out.printf("[FIXTURE] usuario=%d sala=%d asiento=%d funcion=%d compra=%d%n",
                    usuarioId, salaId, asientoId, funcionId, compraId);

            // 1) Verificar disponibilidad
            boolean libre1 = dao.verificarAsientoDisponible(funcionId, asientoId);
            out.println("[DISPONIBILIDAD INICIAL] libre=" + libre1);

            // 2) CREAR entrada
            Entrada e = new Entrada(compraId, funcionId, asientoId,
                    new BigDecimal("18000.00"), EstadoEntrada.ACTIVA);
            Integer id = dao.crear(e);
            out.println("[CREAR] id=" + id);

            // 3) Buscar por ID
            Entrada byId = dao.buscarPorId(id);
            out.println("[BUSCAR ID] estado=" + byId.getEstadoEntrada());

            // 4) Listar por compra/función
            List<Entrada> porCompra  = dao.listarPorCompra(compraId);
            List<Entrada> porFuncion = dao.listarPorFuncion(funcionId);
            out.println("[LISTAR POR COMPRA] total=" + porCompra.size());
            out.println("[LISTAR POR FUNCION] total=" + porFuncion.size());

            // 5) Contar activas por función
            out.println("[CONTAR ACTIVAS] total=" + dao.contarEntradasActivasPorFuncion(funcionId));

            // 6) Verificar disponibilidad (debe ser false ahora)
            boolean libre2 = dao.verificarAsientoDisponible(funcionId, asientoId);
            out.println("[DISPONIBILIDAD POST-VENTA] libre=" + libre2);

            // 7) Intentar vender el mismo asiento activo (debería fallar UNIQUE)
            try {
                dao.crear(new Entrada(compraId, funcionId, asientoId, new BigDecimal("18000.00"), EstadoEntrada.ACTIVA));
                out.println("[ERROR] Se pudo vender doble (no debería)");
            } catch (Exception ex) {
                out.println("[OK] Doble venta bloqueada: " + ex.getMessage());
            }

            // 8) Actualizar: marcar UTILIZADA
            e.setEstadoEntrada(EstadoEntrada.UTILIZADA);
            boolean upd = dao.actualizar(e);
            out.println("[ACTUALIZAR] ok=" + upd);

            // 9) Cancelar todas las entradas de la compra (marcar CANCELADA)
            boolean cancel = dao.cancelarEntradasDeCompra(compraId);
            out.println("[CANCELAR ENTRADAS DE COMPRA] ok=" + cancel);

            if (!KEEP_DATA) {
                hardCleanup(compraId, funcionId, asientoId, salaId);
                out.println("[CLEANUP] Datos eliminados.");
            } else {
                out.println("[KEEP_DATA] Datos conservados para inspección.");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            ConnectionPool.close();
            out.println("[FIN] EntradaDAO smoke test.");
        }
    }

    // ===== Helpers de FIXTURE/CLEANUP =====

    private static int ensureUsuario(String email) throws SQLException {
        String sel = "SELECT id_usuario FROM usuario WHERE correo_electronico = ?";
        String ins = """
            INSERT INTO usuario (nombre_completo, correo_electronico, nombre_usuario, contrasena_hash, rol, fecha_registro, activo)
            VALUES ('Usuario Entradas', ?, 'entradas_user', 'hash', 'USUARIO'::rol, now(), TRUE)
            RETURNING id_usuario
        """;
        try (Connection c = ConnectionPool.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(sel)) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
            try (PreparedStatement ps = c.prepareStatement(ins)) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
            }
        }
    }

    private static int ensureSala(String nombre, int filas, int cols) throws SQLException {
        String sel = "SELECT id_sala FROM sala WHERE nombre_sala = ?";
        String ins = "INSERT INTO sala (nombre_sala, capacidad_total, filas, columnas, activa) VALUES (?, ?, ?, ?, TRUE) RETURNING id_sala";
        try (Connection c = ConnectionPool.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(sel)) {
                ps.setString(1, nombre);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
            }
            try (PreparedStatement ps = c.prepareStatement(ins)) {
                ps.setString(1, nombre);
                ps.setInt(2, filas * cols);
                ps.setInt(3, filas);
                ps.setInt(4, cols);
                try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
            }
        }
    }

    private static int ensureAsiento(int salaId, String fila, int numero, TipoAsiento tipo) throws SQLException {
        String sel = "SELECT id_asiento FROM asiento WHERE sala_id = ? AND fila = ? AND numero = ?";
        String ins = "INSERT INTO asiento (sala_id, fila, numero, tipo_asiento, activo) VALUES (?, ?, ?, ?::tipo_asiento, TRUE) RETURNING id_asiento";
        try (Connection c = ConnectionPool.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(sel)) {
                ps.setInt(1, salaId); ps.setString(2, fila); ps.setInt(3, numero);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
            }
            try (PreparedStatement ps = c.prepareStatement(ins)) {
                ps.setInt(1, salaId); ps.setString(2, fila); ps.setInt(3, numero);
                ps.setString(4, tipo.name());
                try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
            }
        }
    }

    private static int ensurePelicula(String titulo) throws SQLException {
        String sel = "SELECT id_pelicula FROM pelicula WHERE titulo = ?";
        String ins = """
            INSERT INTO pelicula (titulo, duracion_minutos, clasificacion, sinopsis, imagen_url, fecha_estreno, activa)
            VALUES (?, 120, '12+'::clasificacion, 'Demo', NULL, CURRENT_DATE, TRUE)
            RETURNING id_pelicula
        """;
        try (Connection c = ConnectionPool.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(sel)) {
                ps.setString(1, titulo);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
            }
            try (PreparedStatement ps = c.prepareStatement(ins)) {
                ps.setString(1, titulo);
                try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
            }
        }
    }

    private static int ensureFuncion(int peliculaId, int salaId, LocalDateTime ini, LocalDateTime fin, double precio) throws SQLException {
        String ins = """
            INSERT INTO funcion (pelicula_id, sala_id, fecha_hora_inicio, fecha_hora_fin, precio_entrada, estado)
            VALUES (?, ?, ?, ?, ?, 'PROGRAMADA'::estado_funcion)
            RETURNING id_funcion
        """;
        try (Connection c = ConnectionPool.getConnection();
             PreparedStatement ps = c.prepareStatement(ins)) {
            ps.setInt(1, peliculaId);
            ps.setInt(2, salaId);
            ps.setTimestamp(3, Timestamp.valueOf(ini));
            ps.setTimestamp(4, Timestamp.valueOf(fin));
            ps.setBigDecimal(5, new java.math.BigDecimal(String.valueOf(precio)));
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    private static int ensureCompra(int usuarioId, java.math.BigDecimal totEnt, java.math.BigDecimal totConf, MetodoPago metodo) throws SQLException {
        String ins = """
            INSERT INTO compra (usuario_id, fecha_hora_compra, total_entradas, total_confiteria, metodo_pago, estado_compra, fecha_cancelacion, ruta_comprobante_pdf)
            VALUES (?, now(), ?, ?, ?::metodo_pago, 'CONFIRMADA'::estado_compra, NULL, NULL)
            RETURNING id_compra
        """;
        try (Connection c = ConnectionPool.getConnection();
             PreparedStatement ps = c.prepareStatement(ins)) {
            ps.setInt(1, usuarioId);
            ps.setBigDecimal(2, totEnt);
            ps.setBigDecimal(3, totConf);
            ps.setString(4, metodo.name());
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    private static void hardCleanup(Integer compraId, Integer funcionId, Integer asientoId, Integer salaId) throws SQLException {
        try (Connection c = ConnectionPool.getConnection()) {
            // entradas se borran por cascade al borrar compra
            try (PreparedStatement d1 = c.prepareStatement("DELETE FROM compra WHERE id_compra = ?")) {
                d1.setInt(1, compraId); d1.executeUpdate();
            }
            try (PreparedStatement d2 = c.prepareStatement("DELETE FROM funcion WHERE id_funcion = ?")) {
                d2.setInt(1, funcionId); d2.executeUpdate();
            }
            try (PreparedStatement d3 = c.prepareStatement("DELETE FROM asiento WHERE id_asiento = ?")) {
                d3.setInt(1, asientoId); d3.executeUpdate();
            }
            try (PreparedStatement d4 = c.prepareStatement("DELETE FROM sala WHERE id_sala = ?")) {
                d4.setInt(1, salaId); d4.executeUpdate();
            }
        }
    }
}
