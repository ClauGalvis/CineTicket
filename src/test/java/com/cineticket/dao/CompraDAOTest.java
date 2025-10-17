package com.cineticket.dao;

import com.cineticket.dao.impl.CompraDAOImpl;
import com.cineticket.enums.EstadoCompra;
import com.cineticket.enums.MetodoPago;
import com.cineticket.modelo.Compra;
import com.cineticket.util.ConnectionPool;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

public class CompraDAOTest {

    private static final boolean KEEP_DATA = true; // true -> conserva la compra para inspección

    public static void main(String[] args) {
        CompraDAO dao = new CompraDAOImpl();

        try {
            int usuarioId = ensureUsuarioDemo();

            // 1) CREAR
            Compra c = new Compra(
                    usuarioId,
                    new BigDecimal("25000.00"),
                    new BigDecimal("12000.50"),
                    MetodoPago.PSE,
                    EstadoCompra.CONFIRMADA,
                    "files/comp_123.pdf"
            );
            Integer id = dao.crear(c);
            System.out.println("[CREAR] id=" + id);

            // 2) BUSCAR ID
            Compra byId = dao.buscarPorId(id);
            System.out.println("[BUSCAR ID] total_general=" + byId.getTotalGeneral());

            // 3) LISTAR POR USUARIO
            List<Compra> hist = dao.listarPorUsuario(usuarioId);
            System.out.println("[HISTORIAL USUARIO] total=" + hist.size());

            // 4) ACTUALIZAR (cambiar totales y método)
            c.setTotalEntradas(new BigDecimal("30000.00"));
            c.setTotalConfiteria(new BigDecimal("5000.00"));
            c.setMetodoPago(MetodoPago.TRANSFERENCIA);
            c.setRutaComprobantePdf("files/comp_123_v2.pdf");
            boolean upd = dao.actualizar(c);
            System.out.println("[ACTUALIZAR] ok=" + upd);

            // 5) RANGO DE FECHAS
            LocalDateTime desde = LocalDateTime.now().minusDays(1);
            LocalDateTime hasta = LocalDateTime.now().plusDays(1);
            System.out.println("[ENTRE FECHAS] total=" + dao.obtenerComprasEntreFechas(desde, hasta).size());

            // 6) CANCELAR
            boolean cancel = dao.cancelarCompra(id);
            System.out.println("[CANCELAR] ok=" + cancel);

            if (!KEEP_DATA) {
                hardDeleteCompra(id);
                System.out.println("[CLEANUP] Compra eliminada.");
            } else {
                System.out.println("[KEEP_DATA] Compra conservada para inspección.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ConnectionPool.close();
            System.out.println("[FIN] CompraDAO smoke test.");
        }
    }

    // ================= Helpers =================

    /** Crea (o devuelve) un usuario básico para probar compras. */
    private static int ensureUsuarioDemo() throws SQLException {
        String email = "compras.demo@cineticket.com";
        String sel = "SELECT id_usuario FROM usuario WHERE correo_electronico = ?";
        String ins = """
            INSERT INTO usuario
              (nombre_completo, correo_electronico, nombre_usuario, contrasena_hash, rol, fecha_registro, activo)
            VALUES
              ('Usuario Compras Demo', ?, 'compras_demo', 'hash_demo', 'USUARIO'::rol, now(), TRUE)
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
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        }
    }

    private static void hardDeleteCompra(int id) throws SQLException {
        try (Connection c = ConnectionPool.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM compra WHERE id_compra = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
