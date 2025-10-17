package com.cineticket.dao.impl;

import com.cineticket.dao.CompraConfiteriaDAO;
import com.cineticket.dao.common.DaoException;
import com.cineticket.modelo.CompraConfiteria;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public class CompraConfiteriaDAOImpl extends BaseDAO implements CompraConfiteriaDAO {

    public CompraConfiteriaDAOImpl() {}

    private static void validar(CompraConfiteria i) {
        if (i.getCompraId() == null)  throw new IllegalArgumentException("compraId requerido");
        if (i.getComboId() == null)   throw new IllegalArgumentException("comboId requerido");
        if (i.getCantidad() == null || i.getCantidad() <= 0)
            throw new IllegalArgumentException("cantidad debe ser > 0");
        if (i.getPrecioUnitario() == null || i.getPrecioUnitario().signum() < 0)
            throw new IllegalArgumentException("precioUnitario inválido");
    }

    @Override
    public Integer crear(CompraConfiteria i) {
        validar(i);
        // subtotal es GENERATED ALWAYS -> no se envía
        String sql = """
            INSERT INTO compra_confiteria (compra_id, combo_id, cantidad, precio_unitario)
            VALUES (?, ?, ?, ?)
        """;
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, i.getCompraId());
            ps.setInt(2, i.getComboId());
            ps.setInt(3, i.getCantidad());
            ps.setBigDecimal(4, i.getPrecioUnitario());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    i.setIdCompraConfiteria(id);
                    return id;
                }
            }
            throw new DaoException("No se recibió ID generado para compra_confiteria.");
        } catch (SQLException e) {
            // 23505: UNIQUE(compra_id, combo_id)
            if ("23505".equals(e.getSQLState())) {
                throw new DaoException("Ya existe ese combo en la compra (único por compra).", e);
            }
            // 23503: FK compra o combo inexistentes
            if ("23503".equals(e.getSQLState())) {
                throw new DaoException("FK inválida: compra/combo inexistente.", e);
            }
            throw new DaoException("Error al crear compra_confiteria", e);
        }
    }

    @Override
    public CompraConfiteria buscarPorId(Integer id) {
        String sql = "SELECT * FROM compra_confiteria WHERE id_compra_confiteria = ?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            throw new DaoException("Error al buscar compra_confiteria por ID", e);
        }
    }

    @Override
    public List<CompraConfiteria> listarPorCompra(Integer compraId) {
        String sql = "SELECT * FROM compra_confiteria WHERE compra_id = ? ORDER BY id_compra_confiteria";
        List<CompraConfiteria> list = new ArrayList<>();
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, compraId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapear(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("Error al listar compra_confiteria por compra", e);
        }
    }

    @Override
    public Map<Integer, Integer> obtenerVentasPorCombo(LocalDateTime inicio, LocalDateTime fin) {
        // Sumamos cantidades por combo filtrando por fecha de la COMPRA
        String sql = """
            SELECT cc.combo_id, SUM(cc.cantidad) AS total
              FROM compra_confiteria cc
              JOIN compra c ON c.id_compra = cc.compra_id
             WHERE c.fecha_hora_compra BETWEEN ? AND ?
             GROUP BY cc.combo_id
        """;
        Map<Integer, Integer> mapa = new LinkedHashMap<>();
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(inicio));
            ps.setTimestamp(2, Timestamp.valueOf(fin));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mapa.put(rs.getInt("combo_id"), rs.getInt("total"));
                }
            }
            return mapa;
        } catch (SQLException e) {
            throw new DaoException("Error al obtener ventas por combo", e);
        }
    }

    // ================== MAPEADOR ==================
    private CompraConfiteria mapear(ResultSet rs) throws SQLException {
        CompraConfiteria i = new CompraConfiteria();
        i.setIdCompraConfiteria(rs.getInt("id_compra_confiteria"));
        i.setCompraId(rs.getInt("compra_id"));
        i.setComboId(rs.getInt("combo_id"));
        i.setCantidad(rs.getInt("cantidad"));
        i.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
        i.setSubtotal(rs.getBigDecimal("subtotal")); // generated
        return i;
    }
}
