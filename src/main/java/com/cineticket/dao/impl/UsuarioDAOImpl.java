package com.cineticket.dao.impl;

import com.cineticket.dao.UsuarioDAO;
import com.cineticket.dao.common.DaoException;
import com.cineticket.enums.Rol;
import com.cineticket.modelo.Usuario;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC de UsuarioDAO usando ConnectionPool estático.
 */
public class UsuarioDAOImpl extends BaseDAO implements UsuarioDAO {

    // Constructor sin dependencias (pool es estático)
    public UsuarioDAOImpl() { }

    @Override
    public void crear(Usuario usuario) {
        String sql = """
            INSERT INTO usuario (nombre_completo, correo_electronico, nombre_usuario,
                                 contrasena_hash, rol, fecha_registro, activo)
            VALUES (?, ?, ?, ?, ?::rol, ?, ?)
        """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, usuario.getNombreCompleto());
            ps.setString(2, usuario.getCorreoElectronico());
            ps.setString(3, usuario.getNombreUsuario());
            ps.setString(4, usuario.getContrasenaHash());
            ps.setString(5, usuario.getRol().name());
            ps.setTimestamp(6, Timestamp.valueOf(usuario.getFechaRegistro()));
            ps.setBoolean(7, usuario.isActivo());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) usuario.setIdUsuario(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new DaoException("Error al crear usuario", e);
        }
    }

    @Override
    public Optional<Usuario> buscarPorId(int idUsuario) {
        String sql = "SELECT * FROM usuario WHERE id_usuario = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Error al buscar usuario por ID", e);
        }
    }

    @Override
    public Optional<Usuario> buscarPorNombreUsuario(String nombreUsuario) {
        String sql = "SELECT * FROM usuario WHERE nombre_usuario = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombreUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Error al buscar usuario por nombre de usuario", e);
        }
    }

    @Override
    public Optional<Usuario> buscarPorCorreo(String correo) {
        String sql = "SELECT * FROM usuario WHERE correo_electronico = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, correo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Error al buscar usuario por correo", e);
        }
    }

    @Override
    public List<Usuario> listarTodos() {
        String sql = "SELECT * FROM usuario ORDER BY id_usuario";
        List<Usuario> usuarios = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) usuarios.add(mapear(rs));
            return usuarios;
        } catch (SQLException e) {
            throw new DaoException("Error al listar usuarios", e);
        }
    }

    @Override
    public void actualizar(Usuario usuario) {
        String sql = """
            UPDATE usuario
               SET nombre_completo = ?, correo_electronico = ?, nombre_usuario = ?,
                   contrasena_hash = ?, rol = ?::rol, activo = ?
             WHERE id_usuario = ?
        """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usuario.getNombreCompleto());
            ps.setString(2, usuario.getCorreoElectronico());
            ps.setString(3, usuario.getNombreUsuario());
            ps.setString(4, usuario.getContrasenaHash());
            ps.setString(5, usuario.getRol().name());
            ps.setBoolean(6, usuario.isActivo());
            ps.setInt(7, usuario.getIdUsuario());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Error al actualizar usuario", e);
        }
    }

    @Override
    public boolean existeNombreUsuario(String nombreUsuario) {
        String sql = "SELECT 1 FROM usuario WHERE nombre_usuario = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombreUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DaoException("Error al verificar nombre de usuario", e);
        }
    }

    @Override
    public boolean existeCorreo(String correo) {
        String sql = "SELECT 1 FROM usuario WHERE correo_electronico = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, correo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DaoException("Error al verificar correo electrónico", e);
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setIdUsuario(rs.getInt("id_usuario"));
        u.setNombreCompleto(rs.getString("nombre_completo"));
        u.setCorreoElectronico(rs.getString("correo_electronico"));
        u.setNombreUsuario(rs.getString("nombre_usuario"));
        u.setContrasenaHash(rs.getString("contrasena_hash"));
        u.setRol(Rol.valueOf(rs.getString("rol")));
        u.setFechaRegistro(rs.getObject("fecha_registro", LocalDateTime.class));
        u.setActivo(rs.getBoolean("activo"));
        return u;
    }
}
