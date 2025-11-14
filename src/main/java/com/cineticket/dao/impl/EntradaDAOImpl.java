package com.cineticket.dao.impl;

import com.cineticket.dao.EntradaDAO;
import com.cineticket.dao.common.DaoException;
import com.cineticket.enums.EstadoEntrada;
import com.cineticket.modelo.Entrada;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EntradaDAOImpl extends BaseDAO implements EntradaDAO {

    public EntradaDAOImpl() {
    }

    // ===== Helpers enum =====
    private static String toDbEstado(EstadoEntrada e) {
        return e.name();
    }

    private static EstadoEntrada fromDbEstado(String s) {
        return EstadoEntrada.valueOf(s);
    }

    private static void validar(Entrada e) {
        if (e.getCompraId() == null) throw new IllegalArgumentException("compraId requerido");
        if (e.getFuncionId() == null) throw new IllegalArgumentException("funcionId requerido");
        if (e.getAsientoId() == null) throw new IllegalArgumentException("asientoId requerido");
        if (e.getPrecioUnitario() == null || e.getPrecioUnitario().signum() < 0)
            throw new IllegalArgumentException("precioUnitario inválido");
        if (e.getEstadoEntrada() == null) throw new IllegalArgumentException("estadoEntrada requerido");
    }

    @Override
    public Integer crear(Entrada e) {
        validar(e);
        String sql = """
                    INSERT INTO entrada (compra_id, funcion_id, asiento_id, precio_unitario, estado_entrada)
                    VALUES (?, ?, ?, ?, ?::estado_entrada)
                """;
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, e.getCompraId());
            ps.setInt(2, e.getFuncionId());
            ps.setInt(3, e.getAsientoId());
            ps.setBigDecimal(4, e.getPrecioUnitario());
            ps.setString(5, toDbEstado(e.getEstadoEntrada()));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    e.setIdEntrada(id);
                    return id;
                }
            }
            throw new DaoException("No se recibió ID generado para entrada.");
        } catch (SQLException ex) {
            // 23505: UNIQUE parcial (funcion, asiento) ACTIVA
            if ("23505".equals(ex.getSQLState())) {
                throw new DaoException("Asiento ya vendido (entrada ACTIVA) para esa función.", ex);
            }
            // 23503: FK compra/funcion/asiento
            if ("23503".equals(ex.getSQLState())) {
                throw new DaoException("FK inválida: compra/función/asiento inexistente.", ex);
            }
            throw new DaoException("Error al crear entrada", ex);
        }
    }

    @Override
    public Entrada buscarPorId(Integer id) {
        String sql = "SELECT * FROM entrada WHERE id_entrada = ?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapearEntrada(rs) : null;
            }
        } catch (SQLException ex) {
            throw new DaoException("Error al buscar entrada por ID", ex);
        }
    }

    @Override
    public List<Entrada> listarPorCompra(Integer compraId) {
        String sql = "SELECT * FROM entrada WHERE compra_id = ? ORDER BY id_entrada";
        List<Entrada> list = new ArrayList<>();
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, compraId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapearEntrada(rs));
            }
            return list;
        } catch (SQLException ex) {
            throw new DaoException("Error al listar entradas por compra", ex);
        }
    }

    @Override
    public List<Entrada> listarPorFuncion(Integer funcionId) {
        String sql = "SELECT * FROM entrada WHERE funcion_id = ? ORDER BY id_entrada";
        List<Entrada> list = new ArrayList<>();
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, funcionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapearEntrada(rs));
            }
            return list;
        } catch (SQLException ex) {
            throw new DaoException("Error al listar entradas por función", ex);
        }
    }

    @Override
    public boolean actualizar(Entrada e) {
        validar(e);
        String sql = """
                    UPDATE entrada
                       SET compra_id = ?, funcion_id = ?, asiento_id = ?, precio_unitario = ?, estado_entrada = ?::estado_entrada
                     WHERE id_entrada = ?
                """;
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, e.getCompraId());
            ps.setInt(2, e.getFuncionId());
            ps.setInt(3, e.getAsientoId());
            ps.setBigDecimal(4, e.getPrecioUnitario());
            ps.setString(5, toDbEstado(e.getEstadoEntrada()));
            ps.setInt(6, e.getIdEntrada());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            if ("23505".equals(ex.getSQLState())) {
                throw new DaoException("Actualización viola UNIQUE (función, asiento) ACTIVA.", ex);
            }
            throw new DaoException("Error al actualizar entrada", ex);
        }
    }

    @Override
    public boolean cancelarEntradasDeCompra(Integer compraId) {
        String sql = "UPDATE entrada SET estado_entrada = 'CANCELADA'::estado_entrada WHERE compra_id = ?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, compraId);
            int rows = ps.executeUpdate();
            return rows >= 0; // true incluso si era 0 (idempotente)
        } catch (SQLException ex) {
            throw new DaoException("Error al cancelar entradas de la compra", ex);
        }
    }

    @Override
    public int contarEntradasActivasPorFuncion(Integer funcionId) {
        String sql = "SELECT COUNT(*) FROM entrada WHERE funcion_id = ? AND estado_entrada = 'ACTIVA'";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, funcionId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            throw new DaoException("Error al contar entradas activas por función", ex);
        }
    }

    @Override
    public boolean verificarAsientoDisponible(Integer funcionId, Integer asientoId) {
        String sql = """
                    SELECT COUNT(*) FROM entrada
                     WHERE funcion_id = ? AND asiento_id = ? AND estado_entrada = 'ACTIVA'
                """;
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, funcionId);
            ps.setInt(2, asientoId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) == 0;
            }
        } catch (SQLException ex) {
            throw new DaoException("Error al verificar disponibilidad de asiento", ex);
        }
    }

    // ============== Mapeador ==============
    private Entrada mapearEntrada(ResultSet rs) throws SQLException {
        Entrada e = new Entrada();
        e.setIdEntrada(rs.getInt("id_entrada"));
        e.setCompraId(rs.getInt("compra_id"));
        e.setFuncionId(rs.getInt("funcion_id"));
        e.setAsientoId(rs.getInt("asiento_id"));
        e.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
        e.setEstadoEntrada(fromDbEstado(rs.getString("estado_entrada")));
        return e;
    }
}
