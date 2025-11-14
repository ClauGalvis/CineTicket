package com.cineticket.dao.impl;

import com.cineticket.dao.CompraDAO;
import com.cineticket.dao.common.DaoException;
import com.cineticket.enums.EstadoCompra;
import com.cineticket.enums.MetodoPago;
import com.cineticket.modelo.Compra;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC de CompraDAO.
 * Tabla: compra(id_compra, usuario_id, fecha_hora_compra, total_entradas, total_confiteria,
 * total_general [GENERATED ALWAYS], metodo_pago, estado_compra, fecha_cancelacion, ruta_comprobante_pdf)
 */
public class CompraDAOImpl extends BaseDAO implements CompraDAO {

    public CompraDAOImpl() {
    }

    // ===== Helpers =====
    private static String toDbMetodo(MetodoPago m) {
        return m.name();
    }

    private static MetodoPago fromDbMetodo(String s) {
        return MetodoPago.valueOf(s);
    }

    private static String toDbEstado(EstadoCompra e) {
        return e.name();
    }

    private static EstadoCompra fromDbEstado(String s) {
        return EstadoCompra.valueOf(s);
    }

    private static void validar(Compra c) {
        if (c.getUsuarioId() == null) throw new IllegalArgumentException("usuarioId requerido");
        if (c.getTotalEntradas() == null || c.getTotalEntradas().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("totalEntradas inválido");
        if (c.getTotalConfiteria() == null || c.getTotalConfiteria().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("totalConfiteria inválido");
        if (c.getMetodoPago() == null) throw new IllegalArgumentException("metodoPago requerido");
        if (c.getEstadoCompra() == null) throw new IllegalArgumentException("estadoCompra requerido");
    }

    // =================== CRUD / Listados ===================

    @Override
    public Integer crear(Compra c) {
        validar(c);
        // total_general NO se envía (columna GENERATED ALWAYS)
        String sql = """
                    INSERT INTO compra
                      (usuario_id, fecha_hora_compra, total_entradas, total_confiteria,
                       metodo_pago, estado_compra, fecha_cancelacion, ruta_comprobante_pdf)
                    VALUES (?, ?, ?, ?, ?::metodo_pago, ?::estado_compra, ?, ?)
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, c.getUsuarioId());
            // si viene null, delegamos al DEFAULT now(); por portabilidad enviamos valor
            ps.setTimestamp(2, Timestamp.valueOf(
                    c.getFechaHoraCompra() != null ? c.getFechaHoraCompra() : LocalDateTime.now()));
            ps.setBigDecimal(3, c.getTotalEntradas());
            ps.setBigDecimal(4, c.getTotalConfiteria());
            ps.setString(5, toDbMetodo(c.getMetodoPago()));
            ps.setString(6, toDbEstado(c.getEstadoCompra()));
            if (c.getFechaCancelacion() != null) {
                ps.setTimestamp(7, Timestamp.valueOf(c.getFechaCancelacion()));
            } else {
                ps.setNull(7, Types.TIMESTAMP);
            }
            ps.setString(8, c.getRutaComprobantePdf());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    c.setIdCompra(id);
                    return id;
                }
            }
            throw new DaoException("No se recibió ID generado para compra.");
        } catch (SQLException e) {
            // 23503 FK usuario; 23514 checks; etc.
            throw new DaoException("Error al crear compra", e);
        }
    }

    @Override
    public Compra buscarPorId(Integer id) {
        String sql = "SELECT * FROM compra WHERE id_compra = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapearCompra(rs) : null;
            }
        } catch (SQLException e) {
            throw new DaoException("Error al buscar compra por ID", e);
        }
    }

    @Override
    public List<Compra> listarPorUsuario(Integer usuarioId) {
        String sql = "SELECT * FROM compra WHERE usuario_id = ? ORDER BY fecha_hora_compra DESC";
        List<Compra> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapearCompra(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("Error al listar compras por usuario", e);
        }
    }

    @Override
    public boolean actualizar(Compra c) {
        validar(c);
        // total_general NO se actualiza (es generated)
        String sql = """
                    UPDATE compra SET
                      usuario_id = ?, fecha_hora_compra = ?, total_entradas = ?, total_confiteria = ?,
                      metodo_pago = ?::metodo_pago, estado_compra = ?::estado_compra,
                      fecha_cancelacion = ?, ruta_comprobante_pdf = ?
                    WHERE id_compra = ?
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, c.getUsuarioId());
            ps.setTimestamp(2, Timestamp.valueOf(c.getFechaHoraCompra()));
            ps.setBigDecimal(3, c.getTotalEntradas());
            ps.setBigDecimal(4, c.getTotalConfiteria());
            ps.setString(5, toDbMetodo(c.getMetodoPago()));
            ps.setString(6, toDbEstado(c.getEstadoCompra()));
            if (c.getFechaCancelacion() != null) {
                ps.setTimestamp(7, Timestamp.valueOf(c.getFechaCancelacion()));
            } else {
                ps.setNull(7, Types.TIMESTAMP);
            }
            ps.setString(8, c.getRutaComprobantePdf());
            ps.setInt(9, c.getIdCompra());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Error al actualizar compra", e);
        }
    }

    @Override
    public boolean cancelarCompra(Integer idCompra) {
        String sql = """
                    UPDATE compra
                       SET estado_compra = 'CANCELADA'::estado_compra,
                           fecha_cancelacion = now()
                     WHERE id_compra = ?
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCompra);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Error al cancelar compra", e);
        }
    }

    @Override
    public List<Compra> obtenerComprasEntreFechas(LocalDateTime inicio, LocalDateTime fin) {
        String sql = """
                    SELECT * FROM compra
                     WHERE fecha_hora_compra BETWEEN ? AND ?
                     ORDER BY fecha_hora_compra
                """;
        List<Compra> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(inicio));
            ps.setTimestamp(2, Timestamp.valueOf(fin));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapearCompra(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("Error al consultar compras por rango", e);
        }
    }

    // =================== Mapeo ===================

    private Compra mapearCompra(ResultSet rs) throws SQLException {
        Compra c = new Compra();
        c.setIdCompra(rs.getInt("id_compra"));
        c.setUsuarioId(rs.getInt("usuario_id"));
        Timestamp ts = rs.getTimestamp("fecha_hora_compra");
        c.setFechaHoraCompra(ts != null ? ts.toLocalDateTime() : null);
        c.setTotalEntradas(rs.getBigDecimal("total_entradas"));
        c.setTotalConfiteria(rs.getBigDecimal("total_confiteria"));
        c.setTotalGeneral(rs.getBigDecimal("total_general")); // columna generada
        c.setMetodoPago(fromDbMetodo(rs.getString("metodo_pago")));
        c.setEstadoCompra(fromDbEstado(rs.getString("estado_compra")));
        Timestamp tsc = rs.getTimestamp("fecha_cancelacion");
        c.setFechaCancelacion(tsc != null ? tsc.toLocalDateTime() : null);
        c.setRutaComprobantePdf(rs.getString("ruta_comprobante_pdf"));
        return c;
    }
}
