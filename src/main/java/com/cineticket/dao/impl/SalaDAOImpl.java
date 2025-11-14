package com.cineticket.dao.impl;

import com.cineticket.dao.SalaDAO;
import com.cineticket.dao.common.DaoException;
import com.cineticket.modelo.Sala;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC de SalaDAO usando ConnectionPool estático.
 * Tabla: sala(id_sala, nombre_sala, capacidad_total, filas, columnas, activa)
 */
public class SalaDAOImpl extends BaseDAO implements SalaDAO {

    public SalaDAOImpl() {
    }

    @Override
    public Integer crear(Sala s) {
        // Asegurar consistencia con el CHECK (capacidad_total = filas * columnas)
        int capacidad = (s.getCapacidadTotal() != null)
                ? s.getCapacidadTotal()
                : (s.getFilas() * s.getColumnas());
        s.setCapacidadTotal(capacidad);

        String sql = """
                    INSERT INTO sala (nombre_sala, capacidad_total, filas, columnas, activa)
                    VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, s.getNombreSala());
            ps.setInt(2, s.getCapacidadTotal());
            ps.setInt(3, s.getFilas());
            ps.setInt(4, s.getColumnas());
            ps.setBoolean(5, s.isActiva());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    s.setIdSala(id);
                    return id;
                }
            }
            throw new DaoException("No se recibió ID generado para sala.");

        } catch (SQLException e) {
            throw new DaoException("Error al crear sala", e);
        }
    }

    @Override
    public Sala buscarPorId(Integer id) {
        String sql = "SELECT * FROM sala WHERE id_sala = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapearSala(rs) : null;
            }
        } catch (SQLException e) {
            throw new DaoException("Error al buscar sala por ID", e);
        }
    }

    @Override
    public List<Sala> listarTodas() {
        String sql = "SELECT * FROM sala ORDER BY nombre_sala";
        List<Sala> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapearSala(rs));
            return lista;
        } catch (SQLException e) {
            throw new DaoException("Error al listar salas", e);
        }
    }

    @Override
    public List<Sala> listarActivas() {
        String sql = "SELECT * FROM sala WHERE activa = TRUE ORDER BY nombre_sala";
        List<Sala> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapearSala(rs));
            return lista;
        } catch (SQLException e) {
            throw new DaoException("Error al listar salas activas", e);
        }
    }

    @Override
    public boolean actualizar(Sala s) {
        // Recalcular capacidad por consistencia con el CHECK
        int capacidad = (s.getFilas() * s.getColumnas());
        s.setCapacidadTotal(capacidad);

        String sql = """
                    UPDATE sala
                       SET nombre_sala = ?, capacidad_total = ?, filas = ?, columnas = ?, activa = ?
                     WHERE id_sala = ?
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, s.getNombreSala());
            ps.setInt(2, s.getCapacidadTotal());
            ps.setInt(3, s.getFilas());
            ps.setInt(4, s.getColumnas());
            ps.setBoolean(5, s.isActiva());
            ps.setInt(6, s.getIdSala());
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DaoException("Error al actualizar sala", e);
        }
    }

    @Override
    public Sala buscarPorNombre(String nombre) {
        String sql = "SELECT * FROM sala WHERE nombre_sala = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapearSala(rs) : null;
            }
        } catch (SQLException e) {
            throw new DaoException("Error al buscar sala por nombre", e);
        }
    }

    // ============== MAPEADOR ==============
    private Sala mapearSala(ResultSet rs) throws SQLException {
        Sala s = new Sala();
        s.setIdSala(rs.getInt("id_sala"));
        s.setNombreSala(rs.getString("nombre_sala"));
        s.setCapacidadTotal(rs.getInt("capacidad_total"));
        s.setFilas(rs.getInt("filas"));
        s.setColumnas(rs.getInt("columnas"));
        s.setActiva(rs.getBoolean("activa"));
        return s;
    }
}
