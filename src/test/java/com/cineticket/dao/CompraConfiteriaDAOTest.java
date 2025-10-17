package com.cineticket.dao;

import com.cineticket.dao.impl.CompraConfiteriaDAOImpl;
import com.cineticket.enums.MetodoPago;
import com.cineticket.modelo.CompraConfiteria;
import com.cineticket.util.ConnectionPool;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static java.lang.System.out;

public class CompraConfiteriaDAOTest {

    private static final boolean KEEP_DATA = true; // true -> conserva datos para inspección

    public static void main(String[] args) {
        CompraConfiteriaDAO dao = new CompraConfiteriaDAOImpl();

        Integer compraId = null, combo1 = null, combo2 = null;

        try {
            // === Fixtures: usuario, compra, combos ===
            int usuarioId = ensureUsuario("conf.report@cineticket.com");
            compraId = ensureCompra(usuarioId, new BigDecimal("0.00"), new BigDecimal("0.00"), MetodoPago.PSE);
            combo1 = ensureCombo("Combo Reporte A", new BigDecimal("12000.00"), "Combos");
            combo2 = ensureCombo("Combo Reporte B", new BigDecimal("8000.00"), "Bebidas");

            out.printf("[FIXTURE] compra=%d comboA=%d comboB=%d%n", compraId, combo1, combo2);

            // 1) Crear items (uno por combo)
            CompraConfiteria a = new CompraConfiteria(compraId, combo1, 2, new BigDecimal("12000.00"));
            Integer idA = dao.crear(a);
            CompraConfiteria b = new CompraConfiteria(compraId, combo2, 3, new BigDecimal("8000.00"));
            Integer idB = dao.crear(b);
            out.println("[CREAR] idA=" + idA + " idB=" + idB);

            // 2) Buscar por ID
            out.println("[BUSCAR A] subtotal=" + dao.buscarPorId(idA).getSubtotal());

            // 3) Listar por compra
            List<CompraConfiteria> items = dao.listarPorCompra(compraId);
            out.println("[LISTAR POR COMPRA] total=" + items.size());

            // 4) Reporte ventas por combo (rango amplio)
            LocalDateTime ini = LocalDateTime.now().minusDays(1);
            LocalDateTime fin = LocalDateTime.now().plusDays(1);
            Map<Integer, Integer> ventas = dao.obtenerVentasPorCombo(ini, fin);
            out.println("[VENTAS POR COMBO] " + ventas);

            // 5) Probar UNIQUE(compra, combo)
            try {
                dao.crear(new CompraConfiteria(compraId, combo1, 1, new BigDecimal("12000.00")));
                out.println("[ERROR] Se pudo duplicar combo en la misma compra.");
            } catch (Exception ex) {
                out.println("[OK] Duplicado bloqueado: " + ex.getMessage());
            }

            if (!KEEP_DATA) {
                hardDeleteCompra(compraId); // borra cascada compra_confiteria
                out.println("[CLEANUP] Compra e items eliminados.");
            } else {
                out.println("[KEEP_DATA] Datos conservados para inspección.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ConnectionPool.close();
            out.println("[FIN] CompraConfiteriaDAO smoke test.");
        }
    }

    // ===== Helpers de fixture =====

    private static int ensureUsuario(String email) throws SQLException {
        String sel = "SELECT id_usuario FROM usuario WHERE correo_electronico = ?";
        String ins = """
            INSERT INTO usuario (nombre_completo, correo_electronico, nombre_usuario, contrasena_hash, rol, fecha_registro, activo)
            VALUES ('Usuario Reporte Conf', ?, 'user_conf', 'hash', 'USUARIO'::rol, now(), TRUE)
            RETURNING id_usuario
        """;
        try (Connection c = ConnectionPool.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(sel)) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
            }
            try (PreparedStatement ps = c.prepareStatement(ins)) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
            }
        }
    }

    private static int ensureCompra(int usuarioId, BigDecimal totEnt, BigDecimal totConf, MetodoPago metodo) throws SQLException {
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

    private static int ensureCombo(String nombre, BigDecimal precio, String cat) throws SQLException {
        String sel = "SELECT id_combo FROM combo_confiteria WHERE nombre_combo = ?";
        String ins = "INSERT INTO combo_confiteria (nombre_combo, descripcion, precio, imagen_url, disponible, categoria) VALUES (?, 'desc', ?, NULL, TRUE, ?) RETURNING id_combo";
        try (Connection c = ConnectionPool.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(sel)) {
                ps.setString(1, nombre);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
            }
            try (PreparedStatement ps = c.prepareStatement(ins)) {
                ps.setString(1, nombre);
                ps.setBigDecimal(2, precio);
                ps.setString(3, cat);
                try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
            }
        }
    }

    private static void hardDeleteCompra(Integer compraId) throws SQLException {
        try (Connection c = ConnectionPool.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM compra WHERE id_compra = ?")) {
            ps.setInt(1, compraId);
            ps.executeUpdate();
        }
    }
}
