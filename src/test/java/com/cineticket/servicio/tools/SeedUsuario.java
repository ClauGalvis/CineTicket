package com.cineticket.servicio.tools;

import com.cineticket.dao.UsuarioDAO;
import com.cineticket.dao.impl.UsuarioDAOImpl;
import com.cineticket.modelo.Usuario;
import com.cineticket.servicio.AuthService;

public class SeedUsuario {
    public static void main(String[] args) {
        // Cambia estos valores por los que quieras crear
        String nombreCompleto = "Claudia Galvis";
        String correo         = "clau@ejemplo.com";
        String nombreUsuario  = "clau";
        String contrasena     = "Abcdef12"; // será hasheada con BCrypt

        UsuarioDAO usuarioDAO = new UsuarioDAOImpl();
        AuthService auth = new AuthService(usuarioDAO);

        try {
            Usuario u = auth.registrarUsuario(nombreCompleto, correo, nombreUsuario, contrasena);
            System.out.println("✅ Usuario creado. ID=" + u.getIdUsuario());
            System.out.println("Hash guardado = " + u.getContrasenaHash()); // debe empezar por $2
        } catch (RuntimeException e) {
            System.err.println("❌ No se pudo crear el usuario: " + e.getMessage());
        }
    }
}
