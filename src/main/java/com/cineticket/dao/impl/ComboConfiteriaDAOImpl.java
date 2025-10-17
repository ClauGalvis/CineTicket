package com.cineticket.dao.impl;

import com.cineticket.dao.ComboConfiteriaDAO;
import com.cineticket.dao.common.DaoException;
import com.cineticket.modelo.ComboConfiteria;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ComboConfiteriaDAOImpl extends BaseDAO implements ComboConfiteriaDAO {

    public ComboConfiteriaDAOImpl() {}

    private static void validar(ComboConfiteria c) {
        if (c.getNombreCombo() == null || c.getNombreCombo().isBlank())
            throw new IllegalArgumentException("nombre_combo requerido");
        if (c.getPrecio() == null || c.getPrecio().signum() < 0)
            throw new IllegalArgumentException("precio inválido");
    }

    @Override
    public Integer crear(ComboConfiteria c) {
        validar(c);
        String sql = """
            INSERT INTO combo_confiteria
              (nombre_combo, descripcion, precio, imagen_url, disponible, categoria)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, c.getNombreCombo());
            ps.setString(2, c.getDescripcion());
            ps.setBigDecimal(3, c.getPrecio());
            ps.setString(4, c.getImagenUrl());
            ps.setBoolean(5, c.isDisponible());
            ps.setString(6, c.getCategoria());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    c.setIdCombo(id);
                    return id;
                }
            }
            throw new DaoException("No se recibió ID generado para combo.");

        } catch (SQLException e) {
            // 23505: nombre único
            if ("23505".equals(e.getSQLState())) {
                throw new DaoException("Ya existe un combo con ese nombre.", e);
            }
            throw new DaoException("Error al crear combo de confitería", e);
        }
    }

    @Override
    public ComboConfiteria buscarPorId(Integer id) {
        String sql = "SELECT * FROM combo_confiteria WHERE id_combo = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            throw new DaoException("Error al buscar combo por ID", e);
        }
    }

    @Override
    public List<ComboConfiteria> listarTodos() {
        String sql = "SELECT * FROM combo_confiteria ORDER BY nombre_combo";
        List<ComboConfiteria> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapear(rs));
            return list;
        } catch (SQLException e) {
            throw new DaoException("Error al listar combos", e);
        }
    }

    @Override
    public List<ComboConfiteria> listarDisponibles() {
        String sql = "SELECT * FROM combo_confiteria WHERE disponible = TRUE ORDER BY nombre_combo";
        List<ComboConfiteria> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapear(rs));
            return list;
        } catch (SQLException e) {
            throw new DaoException("Error al listar combos disponibles", e);
        }
    }

    @Override
    public boolean actualizar(ComboConfiteria c) {
        validar(c);
        String sql = """
            UPDATE combo_confiteria
               SET nombre_combo = ?, descripcion = ?, precio = ?, imagen_url = ?, disponible = ?, categoria = ?
             WHERE id_combo = ?
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, c.getNombreCombo());
            ps.setString(2, c.getDescripcion());
            ps.setBigDecimal(3, c.getPrecio());
            ps.setString(4, c.getImagenUrl());
            ps.setBoolean(5, c.isDisponible());
            ps.setString(6, c.getCategoria());
            ps.setInt(7, c.getIdCombo());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                throw new DaoException("Actualización viola UNIQUE(nombre_combo).", e);
            }
            throw new DaoException("Error al actualizar combo de confitería", e);
        }
    }

    @Override
    public boolean eliminar(Integer id) {
        String sql = "DELETE FROM combo_confiteria WHERE id_combo = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Error al eliminar combo de confitería", e);
        }
    }

    // ============== MAPEADOR ==============
    private ComboConfiteria mapear(ResultSet rs) throws SQLException {
        ComboConfiteria c = new ComboConfiteria();
        c.setIdCombo(rs.getInt("id_combo"));
        c.setNombreCombo(rs.getString("nombre_combo"));
        c.setDescripcion(rs.getString("descripcion"));
        c.setPrecio(rs.getBigDecimal("precio"));
        c.setImagenUrl(rs.getString("imagen_url"));
        c.setDisponible(rs.getBoolean("disponible"));
        c.setCategoria(rs.getString("categoria"));
        return c;
    }
}
