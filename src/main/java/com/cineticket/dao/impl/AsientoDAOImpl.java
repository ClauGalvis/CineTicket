package com.cineticket.dao.impl;

import com.cineticket.dao.AsientoDAO;
import com.cineticket.dao.common.DaoException;
import com.cineticket.enums.TipoAsiento;
import com.cineticket.modelo.Asiento;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC de AsientoDAO usando ConnectionPool estático.
 * Tabla: asiento(id_asiento, sala_id, fila, numero, tipo_asiento, activo)
 */
public class AsientoDAOImpl extends BaseDAO implements AsientoDAO {

    public AsientoDAOImpl() {
    }

    @Override
    public Integer crear(Asiento a) {
        String sql = """
                    INSERT INTO asiento (sala_id, fila, numero, tipo_asiento, activo)
                    VALUES (?, ?, ?, ?::tipo_asiento, ?)
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, a.getSalaId());
            ps.setString(2, a.getFila());
            ps.setInt(3, a.getNumero());
            ps.setString(4, a.getTipoAsiento().name()); // cast en SQL
            ps.setBoolean(5, a.isActivo());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    a.setIdAsiento(id);
                    return id;
                }
            }
            throw new DaoException("No se recibió ID generado para asiento.");

        } catch (SQLException e) {
            // 23505: violación de unique (sala_id, fila, numero)
            if ("23505".equals(e.getSQLState())) {
                throw new DaoException("Ya existe un asiento con esa (sala, fila, número).", e);
            }
            // 23503: violación de FK (sala inexistente)
            if ("23503".equals(e.getSQLState())) {
                throw new DaoException("Sala no existe (FK sala_id).", e);
            }
            throw new DaoException("Error al crear asiento", e);
        }
    }

    @Override
    public Asiento buscarPorId(Integer id) {
        String sql = "SELECT * FROM asiento WHERE id_asiento = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapearAsiento(rs) : null;
            }
        } catch (SQLException e) {
            throw new DaoException("Error al buscar asiento por ID", e);
        }
    }

    @Override
    public List<Asiento> listarPorSala(Integer salaId) {
        String sql = "SELECT * FROM asiento WHERE sala_id = ? ORDER BY fila, numero";
        List<Asiento> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, salaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearAsiento(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new DaoException("Error al listar asientos por sala", e);
        }
    }

    @Override
    public boolean actualizar(Asiento a) {
        String sql = """
                    UPDATE asiento
                       SET sala_id = ?, fila = ?, numero = ?, tipo_asiento = ?::tipo_asiento, activo = ?
                     WHERE id_asiento = ?
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, a.getSalaId());
            ps.setString(2, a.getFila());
            ps.setInt(3, a.getNumero());
            ps.setString(4, a.getTipoAsiento().name());
            ps.setBoolean(5, a.isActivo());
            ps.setInt(6, a.getIdAsiento());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                throw new DaoException("Actualización viola UNIQUE (sala, fila, número).", e);
            }
            throw new DaoException("Error al actualizar asiento", e);
        }
    }

    @Override
    public Asiento buscarPorSalaFilaNumero(Integer salaId, String fila, Integer numero) {
        String sql = "SELECT * FROM asiento WHERE sala_id = ? AND fila = ? AND numero = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, salaId);
            ps.setString(2, fila);
            ps.setInt(3, numero);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapearAsiento(rs) : null;
            }
        } catch (SQLException e) {
            throw new DaoException("Error al buscar asiento por (sala, fila, número)", e);
        }
    }

    // ============== MAPEADOR ==============
    private Asiento mapearAsiento(ResultSet rs) throws SQLException {
        Asiento a = new Asiento();
        a.setIdAsiento(rs.getInt("id_asiento"));
        a.setSalaId(rs.getInt("sala_id"));
        a.setFila(rs.getString("fila"));
        a.setNumero(rs.getInt("numero"));
        a.setTipoAsiento(TipoAsiento.valueOf(rs.getString("tipo_asiento")));
        a.setActivo(rs.getBoolean("activo"));
        return a;
    }
}
