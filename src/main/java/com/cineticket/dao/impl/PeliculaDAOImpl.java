package com.cineticket.dao.impl;

import com.cineticket.dao.PeliculaDAO;
import com.cineticket.dao.common.DaoException;
import com.cineticket.enums.Clasificacion;
import com.cineticket.modelo.Genero;
import com.cineticket.modelo.Pelicula;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PeliculaDAOImpl extends BaseDAO implements PeliculaDAO {

    public PeliculaDAOImpl() { }

    // ===================== Helpers de mapeo ENUM =====================
    private static String toDbClasificacion(Clasificacion c) {
        return switch (c) {
            case T -> "T";
            case SIETE_MAS -> "7+";
            case DOCE_MAS -> "12+";
            case QUINCE_MAS -> "15+";
            case DIECIOCHO_MAS -> "18+";
        };
    }

    private static Clasificacion fromDbClasificacion(String db) {
        return switch (db) {
            case "T"   -> Clasificacion.T;
            case "7+"  -> Clasificacion.SIETE_MAS;
            case "12+" -> Clasificacion.DOCE_MAS;
            case "15+" -> Clasificacion.QUINCE_MAS;
            case "18+" -> Clasificacion.DIECIOCHO_MAS;
            default -> throw new IllegalArgumentException("Clasificación desconocida: " + db);
        };
    }

    // ===================== CRUD =====================
    @Override
    public Integer crear(Pelicula p) {
        String sql = """
            INSERT INTO pelicula (titulo, duracion_minutos, clasificacion, sinopsis, imagen_url, fecha_estreno, activa)
            VALUES (?, ?, ?::clasificacion, ?, ?, ?, ?)
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, p.getTitulo());
            ps.setInt(2, p.getDuracionMinutos());
            ps.setString(3, toDbClasificacion(p.getClasificacion())); // 👈 mapeo a label de BD
            ps.setString(4, p.getSinopsis());
            ps.setString(5, p.getImagenUrl());
            if (p.getFechaEstreno() != null) {
                ps.setObject(6, Date.valueOf(p.getFechaEstreno()));
            } else {
                ps.setNull(6, Types.DATE);
            }
            ps.setBoolean(7, p.isActiva());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    p.setIdPelicula(id);
                    return id;
                }
            }
            throw new DaoException("No se recibió ID generado para película.");
        } catch (SQLException e) {
            throw new DaoException("Error al crear película", e);
        }
    }

    @Override
    public Pelicula buscarPorId(Integer id) {
        String sql = "SELECT * FROM pelicula WHERE id_pelicula = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapearPelicula(rs) : null;
            }
        } catch (SQLException e) {
            throw new DaoException("Error al buscar película por ID", e);
        }
    }

    @Override
    public List<Pelicula> listarTodas() {
        String sql = "SELECT * FROM pelicula ORDER BY titulo";
        List<Pelicula> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapearPelicula(rs));
            return lista;
        } catch (SQLException e) {
            throw new DaoException("Error al listar películas", e);
        }
    }

    @Override
    public List<Pelicula> listarActivas() {
        String sql = "SELECT * FROM pelicula WHERE activa = TRUE ORDER BY fecha_estreno DESC NULLS LAST";
        List<Pelicula> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapearPelicula(rs));
            return lista;
        } catch (SQLException e) {
            throw new DaoException("Error al listar películas activas", e);
        }
    }

    @Override
    public boolean actualizar(Pelicula p) {
        String sql = """
            UPDATE pelicula
               SET titulo = ?, duracion_minutos = ?, clasificacion = ?::clasificacion,
                   sinopsis = ?, imagen_url = ?, fecha_estreno = ?, activa = ?
             WHERE id_pelicula = ?
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.getTitulo());
            ps.setInt(2, p.getDuracionMinutos());
            ps.setString(3, toDbClasificacion(p.getClasificacion())); // 👈 mapeo a label de BD
            ps.setString(4, p.getSinopsis());
            ps.setString(5, p.getImagenUrl());
            if (p.getFechaEstreno() != null) {
                ps.setObject(6, Date.valueOf(p.getFechaEstreno()));
            } else {
                ps.setNull(6, Types.DATE);
            }
            ps.setBoolean(7, p.isActiva());
            ps.setInt(8, p.getIdPelicula());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Error al actualizar película", e);
        }
    }

    @Override
    public boolean eliminar(Integer id) {
        String sql = "UPDATE pelicula SET activa = FALSE WHERE id_pelicula = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Error al eliminar (lógica) película", e);
        }
    }

    @Override
    public List<Pelicula> buscarPorTitulo(String titulo) {
        String sql = "SELECT * FROM pelicula WHERE titulo ILIKE ? ORDER BY titulo";
        List<Pelicula> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + titulo + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearPelicula(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new DaoException("Error en búsqueda por título", e);
        }
    }

    // ===================== N:M GÉNEROS =====================
    @Override
    public boolean asignarGeneros(Integer peliculaId, List<Integer> generoIds) {
        if (generoIds == null || generoIds.isEmpty()) return true;

        String delete = "DELETE FROM pelicula_genero WHERE pelicula_id = ?";
        String insert = "INSERT INTO pelicula_genero (pelicula_id, genero_id) VALUES (?, ?)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psDel = conn.prepareStatement(delete)) {
                psDel.setInt(1, peliculaId);
                psDel.executeUpdate();
            }
            try (PreparedStatement psIns = conn.prepareStatement(insert)) {
                for (Integer gId : generoIds) {
                    psIns.setInt(1, peliculaId);
                    psIns.setInt(2, gId);
                    psIns.addBatch();
                }
                psIns.executeBatch();
            }
            conn.commit();
            conn.setAutoCommit(true);
            return true;
        } catch (SQLException e) {
            throw new DaoException("Error al asignar géneros a la película", e);
        }
    }

    @Override
    public List<Genero> obtenerGenerosDePelicula(Integer peliculaId) {
        String sql = """
        SELECT g.id_genero, g.nombre_genero, g.descripcion, g.activo
          FROM genero g
          JOIN pelicula_genero pg ON g.id_genero = pg.genero_id
         WHERE pg.pelicula_id = ?
         ORDER BY g.nombre_genero
    """;
        List<Genero> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, peliculaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Genero g = new Genero();
                    g.setIdGenero(rs.getInt("id_genero"));
                    g.setNombreGenero(rs.getString("nombre_genero")); // <- aquí el cambio
                    g.setDescripcion(rs.getString("descripcion"));
                    g.setActivo(rs.getBoolean("activo"));
                    lista.add(g);
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new DaoException("Error al obtener géneros de la película", e);
        }
    }

    // ===================== MAPEADOR =====================
    private Pelicula mapearPelicula(ResultSet rs) throws SQLException {
        Pelicula p = new Pelicula();
        p.setIdPelicula(rs.getInt("id_pelicula"));
        p.setTitulo(rs.getString("titulo"));
        p.setDuracionMinutos(rs.getInt("duracion_minutos"));
        p.setClasificacion(fromDbClasificacion(rs.getString("clasificacion"))); // 👈 mapeo desde BD
        p.setSinopsis(rs.getString("sinopsis"));
        p.setImagenUrl(rs.getString("imagen_url"));
        p.setFechaEstreno(rs.getObject("fecha_estreno", LocalDate.class));
        p.setActiva(rs.getBoolean("activa"));
        return p;
    }
}
