package com.cineticket.dao;

import com.cineticket.dao.impl.UsuarioDAOImpl;
import com.cineticket.enums.Rol;
import com.cineticket.modelo.Usuario;
import com.cineticket.util.ConnectionPool;

import java.util.List;
import java.util.Optional;

public class UsuarioDAOTest {
    public static void main(String[] args) {
        UsuarioDAO dao = new UsuarioDAOImpl();

        String sufijo = String.valueOf(System.currentTimeMillis());

        Usuario nuevo = new Usuario(
                "Claudia Galvis " + sufijo,
                "clau" + sufijo + "@mail.com",
                "clau" + sufijo,
                "hash_demo_123",
                Rol.ADMIN
        );

        dao.crear(nuevo);
        System.out.println("[CREAR] ID generado: " + nuevo.getIdUsuario());

        Optional<Usuario> porId = dao.buscarPorId(nuevo.getIdUsuario());
        if (porId.isPresent()) {
            Usuario u = porId.get();
            System.out.println("[BUSCAR POR ID] " + u.getNombreCompleto());

            if (u.esAdministrador()) {
                System.out.println("→ El usuario " + u.getNombreUsuario() + " es ADMIN.");
            } else {
                System.out.println("→ El usuario " + u.getNombreUsuario() + " es USUARIO GENERAL.");
            }
        } else {
            System.out.println("[BUSCAR POR ID] no encontrado");
        }

        Optional<Usuario> porUser = dao.buscarPorNombreUsuario(nuevo.getNombreUsuario());
        System.out.println("[BUSCAR POR USER] " + porUser.map(Usuario::getNombreUsuario).orElse("no"));

        Optional<Usuario> porMail = dao.buscarPorCorreo(nuevo.getCorreoElectronico());
        System.out.println("[BUSCAR POR MAIL] " + porMail.map(Usuario::getCorreoElectronico).orElse("no"));

        boolean existeUser = dao.existeNombreUsuario(nuevo.getNombreUsuario());
        boolean existeMail = dao.existeCorreo(nuevo.getCorreoElectronico());
        System.out.println("[EXISTE] user=" + existeUser + " mail=" + existeMail);

        nuevo.setNombreCompleto("Claudia Actualizada " + sufijo);
        dao.actualizar(nuevo);
        System.out.println("[ACTUALIZAR] OK");

        List<Usuario> todos = dao.listarTodos();
        System.out.println("[LISTAR] total=" + todos.size());
        todos.forEach(u ->
                System.out.println(" - " + u.getIdUsuario() + " | " + u.getNombreUsuario() + " | " + u.getRol())
        );

        ConnectionPool.close(); // cierra el pool al finalizar
        System.out.println("[FIN] Smoke test UsuarioDAO completado.");
    }
}
