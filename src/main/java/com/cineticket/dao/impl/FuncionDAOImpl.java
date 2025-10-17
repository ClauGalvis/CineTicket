package com.cineticket.dao.impl;

import com.cineticket.dao.FuncionDAO;
import com.cineticket.dao.common.DaoException;
import com.cineticket.enums.EstadoFuncion;
import com.cineticket.modelo.Funcion;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC de FuncionDAO.
 * Tabla: funcion(id_funcion, pelicula_id, sala_id, fecha_hora_inicio, fecha_hora_fin, precio_entrada, estado)
 */
public class FuncionDAOImpl extends BaseDAO implements FuncionDAO {

    public FuncionDAOImpl() {}

    // ===== Helpers enum (BD y Java coinciden: PROGRAMADA, EN_CURSO, FINALIZADA, CANCELADA) =====
    private static String toDbEstado(EstadoFuncion e) { return e.name(); }
    private static EstadoFuncion fromDbEstado(String s) { return EstadoFuncion.valueOf(s); }

    private static void validar(Funcion f) {
        if (f.getPeliculaId() == null) throw new IllegalArgumentException("peliculaId requerido");
        if (f.getSalaId() == null)     throw new IllegalArgumentException("salaId requerido");
        if (f.getFechaHoraInicio() == null || f.getFechaHoraFin() == null)
            throw new IllegalArgumentException("fechas requeridas");
        if (!f.getFechaHoraFin().isAfter(f.getFechaHoraInicio()))
            throw new IllegalArgumentException("fecha_hora_fin debe ser mayor que inicio");
        if (f.getPrecioEntrada() == null || f.getPrecioEntrada() < 0)
            throw new IllegalArgumentException("precioEntrada inválido");
    }

    // =================== CRUD / Listados ===================

    @Override
    public Integer crear(Funcion f) {
        validar(f);
        String sql = """
            INSERT INTO funcion
              (pelicula_id, sala_id, fecha_hora_inicio, fecha_hora_fin, precio_entrada, estado)
            VALUES (?, ?, ?, ?, ?, ?::estado_funcion)
        """;
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, f.getPeliculaId());
            ps.setInt(2, f.getSalaId());
            ps.setObject(3, Timestamp.valueOf(f.getFechaHoraInicio()));
            ps.setObject(4, Timestamp.valueOf(f.getFechaHoraFin()));
            ps.setBigDecimal(5, java.math.BigDecimal.valueOf(f.getPrecioEntrada()));
            ps.setString(6, toDbEstado(f.getEstado()));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    f.setIdFuncion(id);
                    return id;
                }
            }
            throw new DaoException("No se recibió ID generado para función.");
        } catch (SQLException e) {
            // 23xxx pueden ser violaciones del EXCLUDE/índices. Lo traducimos.
            throw new DaoException("Error al crear función (posible solapamiento de horario o FK inválida).", e);
        }
    }

    @Override
    public Funcion buscarPorId(Integer id) {
        String sql = "SELECT * FROM funcion WHERE id_funcion = ?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapearFuncion(rs) : null;
            }
        } catch (SQLException e) {
            throw new DaoException("Error al buscar función por ID", e);
        }
    }

    @Override
    public List<Funcion> listarPorPelicula(Integer peliculaId) {
        String sql = "SELECT * FROM funcion WHERE pelicula_id = ? ORDER BY fecha_hora_inicio";
        List<Funcion> list = new ArrayList<>();
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, peliculaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapearFuncion(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("Error al listar funciones por película", e);
        }
    }

    @Override
    public List<Funcion> listarPorSala(Integer salaId) {
        String sql = "SELECT * FROM funcion WHERE sala_id = ? ORDER BY fecha_hora_inicio";
        List<Funcion> list = new ArrayList<>();
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, salaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapearFuncion(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("Error al listar funciones por sala", e);
        }
    }

    @Override
    public List<Funcion> listarPorFecha(LocalDate fecha) {
        String sql = """
            SELECT * FROM funcion
             WHERE DATE(fecha_hora_inicio) = ?
             ORDER BY fecha_hora_inicio
        """;
        List<Funcion> list = new ArrayList<>();
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, Date.valueOf(fecha));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapearFuncion(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("Error al listar funciones por fecha", e);
        }
    }

    @Override
    public boolean actualizar(Funcion f) {
        validar(f);
        String sql = """
            UPDATE funcion SET
              pelicula_id = ?, sala_id = ?, fecha_hora_inicio = ?, fecha_hora_fin = ?,
              precio_entrada = ?, estado = ?::estado_funcion
            WHERE id_funcion = ?
        """;
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, f.getPeliculaId());
            ps.setInt(2, f.getSalaId());
            ps.setObject(3, Timestamp.valueOf(f.getFechaHoraInicio()));
            ps.setObject(4, Timestamp.valueOf(f.getFechaHoraFin()));
            ps.setBigDecimal(5, java.math.BigDecimal.valueOf(f.getPrecioEntrada()));
            ps.setString(6, toDbEstado(f.getEstado()));
            ps.setInt(7, f.getIdFuncion());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Error al actualizar función (posible solapamiento de horario).", e);
        }
    }

    @Override
    public boolean eliminar(Integer id) {
        // Soft delete: estado = CANCELADA
        String sql = "UPDATE funcion SET estado = 'CANCELADA'::estado_funcion WHERE id_funcion = ?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Error al cancelar función", e);
        }
    }

    // =================== Disponibilidad ===================

    @Override
    public boolean verificarDisponibilidadSala(Integer salaId,
                                               LocalDateTime inicio,
                                               LocalDateTime fin,
                                               Integer funcionIdExcluir) {
        // Usamos OVERLAPS y excluimos CANCELADAS y, opcionalmente, una función dada
        String base = """
            SELECT COUNT(*) FROM funcion
             WHERE sala_id = ?
               AND estado <> 'CANCELADA'::estado_funcion
               AND (fecha_hora_inicio, fecha_hora_fin) OVERLAPS (?, ?)
        """;
        String sql = (funcionIdExcluir != null) ? base + " AND id_funcion <> ?" : base;

        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, salaId);
            ps.setTimestamp(2, Timestamp.valueOf(inicio));
            ps.setTimestamp(3, Timestamp.valueOf(fin));
            if (funcionIdExcluir != null) ps.setInt(4, funcionIdExcluir);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int count = rs.getInt(1);
                return count == 0; // true si NO hay solape
            }
        } catch (SQLException e) {
            throw new DaoException("Error al verificar disponibilidad de sala", e);
        }
    }

    // =================== Mapeador ===================

    private Funcion mapearFuncion(ResultSet rs) throws SQLException {
        Funcion f = new Funcion();
        f.setIdFuncion(rs.getInt("id_funcion"));
        f.setPeliculaId(rs.getInt("pelicula_id"));
        f.setSalaId(rs.getInt("sala_id"));
        f.setFechaHoraInicio(rs.getTimestamp("fecha_hora_inicio").toLocalDateTime());
        f.setFechaHoraFin(rs.getTimestamp("fecha_hora_fin").toLocalDateTime());
        f.setPrecioEntrada(rs.getBigDecimal("precio_entrada").doubleValue());
        f.setEstado(fromDbEstado(rs.getString("estado")));
        return f;
    }
}
