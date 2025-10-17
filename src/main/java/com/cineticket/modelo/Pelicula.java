package com.cineticket.modelo;

import com.cineticket.enums.Clasificacion;
import java.time.LocalDate;

/**
 * Representa una película dentro del sistema CineTicket.
 * Contiene información básica y de clasificación para ser mostrada en la cartelera.
 *
 * @author Claudia Patricia
 * @version 1.0
 */
public class Pelicula {
    private Integer idPelicula;
    private String titulo;
    private Integer duracionMinutos;
    private Clasificacion clasificacion;
    private String sinopsis;
    private String imagenUrl;
    private LocalDate fechaEstreno;
    private boolean activa;

    // --- Constructores ---
    /** Constructor por defecto: crea una película activa por defecto */
    public Pelicula() {
        this.activa = true;
    }

    /** Constructor para crear una nueva película (sin ID aún persistido) */
    public Pelicula(String titulo, Integer duracionMinutos, Clasificacion clasificacion,
                    String sinopsis, String imagenUrl, LocalDate fechaEstreno) {
        this();
        this.titulo = titulo;
        this.duracionMinutos = duracionMinutos;
        this.clasificacion = clasificacion;
        this.sinopsis = sinopsis;
        this.imagenUrl = imagenUrl;
        this.fechaEstreno = fechaEstreno;
    }

    /** Constructor completo: se usa al hidratar desde la base de datos */
    public Pelicula(Integer idPelicula, String titulo, Integer duracionMinutos,
                    Clasificacion clasificacion, String sinopsis,
                    String imagenUrl, LocalDate fechaEstreno, boolean activa) {
        this.idPelicula = idPelicula;
        this.titulo = titulo;
        this.duracionMinutos = duracionMinutos;
        this.clasificacion = clasificacion;
        this.sinopsis = sinopsis;
        this.imagenUrl = imagenUrl;
        this.fechaEstreno = fechaEstreno;
        this.activa = activa;
    }

    // --- Métodos de utilidad ---
    /** Devuelve la duración formateada como "2h 15min" */
    public String getDuracionFormateada() {
        int horas = duracionMinutos / 60;
        int minutos = duracionMinutos % 60;
        return String.format("%dh %02dmin", horas, minutos);
    }

    /** Indica si la película está activa en cartelera */
    public boolean estaActiva() {
        return activa;
    }

    // --- Getters y Setters ---
    public Integer getIdPelicula() {
        return idPelicula;
    }

    public void setIdPelicula(Integer idPelicula) {
        this.idPelicula = idPelicula;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public Integer getDuracionMinutos() {
        return duracionMinutos;
    }

    public void setDuracionMinutos(Integer duracionMinutos) {
        this.duracionMinutos = duracionMinutos;
    }

    public Clasificacion getClasificacion() {
        return clasificacion;
    }

    public void setClasificacion(Clasificacion clasificacion) {
        this.clasificacion = clasificacion;
    }

    public String getSinopsis() {
        return sinopsis;
    }

    public void setSinopsis(String sinopsis) {
        this.sinopsis = sinopsis;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public LocalDate getFechaEstreno() {
        return fechaEstreno;
    }

    public void setFechaEstreno(LocalDate fechaEstreno) {
        this.fechaEstreno = fechaEstreno;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }
}
