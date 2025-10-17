package com.cineticket.dao;

import com.cineticket.modelo.Usuario;
import java.util.List;
import java.util.Optional;

/**
 * DAO para gestión de usuarios.
 * Define las operaciones CRUD y consultas personalizadas.
 */
public interface UsuarioDAO {

    void crear(Usuario usuario);

    Optional<Usuario> buscarPorId(int idUsuario);

    Optional<Usuario> buscarPorNombreUsuario(String nombreUsuario);

    Optional<Usuario> buscarPorCorreo(String correo);

    List<Usuario> listarTodos();

    void actualizar(Usuario usuario);

    boolean existeNombreUsuario(String nombreUsuario);

    boolean existeCorreo(String correo);
}
