package com.cineticket.modelo;
import java.time.*;

import com.cineticket.enums.Rol;

/**
 * Representa un usuario del sistema (cliente o administrador).
 * Encapsula los datos personales, credenciales y estado de la cuenta.
 *
 * @author Claudia Patricia
 * @version 1.0
 */
public class Usuario {
    private Integer idUsuario;
    private String nombreCompleto;
    private String correoElectronico;   // case-insensitive en BD
    private String nombreUsuario;       // case-insensitive en BD
    private String contrasenaHash;      // BCrypt
    private Rol rol;
    private LocalDateTime fechaRegistro;
    private boolean activo;

    // --- Constructores ---
    /** Constructor por defecto: se usa al crear un nuevo usuario desde la app */
    public Usuario() {
        this.fechaRegistro = LocalDateTime.now();
        this.activo = true;
    }

    /** Constructor para crear un usuario (nuevo o hidratar desde BD sin ID) */
    public Usuario(String nombreCompleto, String correoElectronico,
                   String nombreUsuario, String contrasenaHash, Rol rol) {
        this();
        this.nombreCompleto = nombreCompleto;
        this.correoElectronico = correoElectronico;
        this.nombreUsuario = nombreUsuario;
        this.contrasenaHash = contrasenaHash;
        this.rol = rol;
    }

    /** Constructor completo (para hidratar desde BD con todos los campos) */
    public Usuario(Integer idUsuario, String nombreCompleto, String correoElectronico,
                   String nombreUsuario, String contrasenaHash, Rol rol,
                   LocalDateTime fechaRegistro, boolean activo) {
        this.idUsuario = idUsuario;
        this.nombreCompleto = nombreCompleto;
        this.correoElectronico = correoElectronico;
        this.nombreUsuario = nombreUsuario;
        this.contrasenaHash = contrasenaHash;
        this.rol = rol;
        this.fechaRegistro = fechaRegistro;
        this.activo = activo;
    }

    // --- Métodos de utilidad ---
    /**
     * Verifica si el usuario tiene rol de administrador.
     * @return true si es ADMIN, false si es USUARIO
     */
    public boolean esAdministrador() {
        return rol == Rol.ADMIN;
    }

    /** Verifica si el usuario está activo */
    public boolean estaActivo() {
        return activo;
    }

    // --- Getters y Setters ---
    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getCorreoElectronico() {
        return correoElectronico;
    }

    public void setCorreoElectronico(String correoElectronico) {
        this.correoElectronico = correoElectronico;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }

    public void setContrasenaHash(String contrasenaHash) {
        this.contrasenaHash = contrasenaHash;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}


