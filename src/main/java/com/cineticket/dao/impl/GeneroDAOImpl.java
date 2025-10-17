package com.cineticket.dao.impl;

import com.cineticket.dao.GeneroDAO;
import com.cineticket.dao.common.DaoException;
import com.cineticket.modelo.Genero;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GeneroDAOImpl extends BaseDAO implements GeneroDAO {

    public GeneroDAOImpl() {}

    @Override
    public Integer crear(Genero g) {
        String sql = """
            INSERT INTO genero (nombre_genero, descripcion, activo)
            VALUES (?, ?, ?)
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, g.getNombreGenero());
            ps.setString(2, g.getDescripcion());
            ps.setBoolean(3, g.isActivo());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    g.setIdGenero(id);
                    return id;
                }
            }
            throw new DaoException("No se recibió ID generado para género.");

        } catch (SQLException e) {
            throw new DaoException("Error al crear género", e);
        }
    }

    @Override
    public Genero buscarPorId(Integer id) {
        String sql = "SELECT * FROM genero WHERE id_genero = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapearGenero(rs) : null;
            }
        } catch (SQLException e) {
            throw new DaoException("Error al buscar género por ID", e);
        }
    }

    @Override
    public List<Genero> listarTodos() {
        String sql = "SELECT * FROM genero ORDER BY nombre_genero";
        List<Genero> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapearGenero(rs));
            return lista;
        } catch (SQLException e) {
            throw new DaoException("Error al listar géneros", e);
        }
    }

    @Override
    public List<Genero> listarActivos() {
        String sql = "SELECT * FROM genero WHERE activo = TRUE ORDER BY nombre_genero";
        List<Genero> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapearGenero(rs));
            return lista;
        } catch (SQLException e) {
            throw new DaoException("Error al listar géneros activos", e);
        }
    }

    @Override
    public boolean actualizar(Genero g) {
        String sql = """
            UPDATE genero
               SET nombre_genero = ?, descripcion = ?, activo = ?
             WHERE id_genero = ?
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, g.getNombreGenero());
            ps.setString(2, g.getDescripcion());
            ps.setBoolean(3, g.isActivo());
            ps.setInt(4, g.getIdGenero());
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DaoException("Error al actualizar género", e);
        }
    }

    @Override
    public Genero buscarPorNombre(String nombre) {
        String sql = "SELECT * FROM genero WHERE nombre_genero = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapearGenero(rs) : null;
            }
        } catch (SQLException e) {
            throw new DaoException("Error al buscar género por nombre", e);
        }
    }

    // ============== MAPEADOR ==============
    private Genero mapearGenero(ResultSet rs) throws SQLException {
        Genero g = new Genero();
        g.setIdGenero(rs.getInt("id_genero"));
        g.setNombreGenero(rs.getString("nombre_genero"));
        g.setDescripcion(rs.getString("descripcion"));
        g.setActivo(rs.getBoolean("activo"));
        return g;
    }
}
