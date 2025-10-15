package com.cineticket.modelo;

/**
 * Representa un genero cinematografico (animacion, accion, comedia, etc.)
 */
public class Genero {
    private Integer idGenero;
    private String nombreGenero;
    private String descripcion;
    private boolean activo;

    // --- Constructores ---
    /** Constructor por defecto: usado al crear un nuevo género */
    public Genero() {
        this.activo = true;
    }

    /** Constructor para crear un género nuevo (sin ID, aún no persistido) */
    public Genero(String nombreGenero, String descripcion) {
        this();
        this.nombreGenero = nombreGenero;
        this.descripcion = descripcion;
    }

    /** Constructor completo: usado para hidratar desde BD */
    public Genero(Integer idGenero, String nombreGenero, String descripcion, boolean activo) {
        this.idGenero = idGenero;
        this.nombreGenero = nombreGenero;
        this.descripcion = descripcion;
        this.activo = activo;
    }

    // --- Getters y Setters ---
    public Integer getIdGenero() {
        return idGenero;
    }

    public void setIdGenero(Integer idGenero) {
        this.idGenero = idGenero;
    }

    public String getNombreGenero() {
        return nombreGenero;
    }

    public void setNombreGenero(String nombreGenero) {
        this.nombreGenero = nombreGenero;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
