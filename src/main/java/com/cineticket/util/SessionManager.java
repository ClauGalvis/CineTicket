package com.cineticket.util;

import com.cineticket.enums.Rol;
import com.cineticket.modelo.Usuario;

public final class SessionManager {
    private static volatile SessionManager instance;
    private Usuario usuarioActual;

    private SessionManager() { }

    public static SessionManager getInstance() {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) instance = new SessionManager();
            }
        }
        return instance;
    }

    public void setUsuarioActual(Usuario usuario) {
        this.usuarioActual = usuario;
    }

    public Usuario getUsuarioActual() {
        return usuarioActual;
    }

    public boolean isLoggedIn() {
        return usuarioActual != null;
    }

    public boolean esAdministrador() {
        return usuarioActual != null && usuarioActual.getRol() == Rol.ADMIN;
    }

    public void cerrarSesion() {
        this.usuarioActual = null;
    }
}
