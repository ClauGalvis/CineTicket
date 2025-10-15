package com.cineticket.modelo;

import java.time.LocalDateTime;
import com.cineticket.enums.EstadoFuncion;

/**
 * Representa una función de cine (proyección programada) dentro del sistema CineTicket.
 * Corresponde a la tabla 'funcion' en la base de datos.
 *
 * Incluye la relación con película y sala, horarios y precio de entrada.
 *
 * @author Claudia Patricia
 * @version 1.0
 */
public class Funcion {
    private Integer idFuncion;
    private Integer peliculaId;       // FK → pelicula.id_pelicula
    private Integer salaId;           // FK → sala.id_sala
    private LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;
    private Double precioEntrada;
    private EstadoFuncion estado;

    // --- Constructores ---
    /** Constructor por defecto: función programada por defecto. */
    public Funcion() {
        this.estado = EstadoFuncion.PROGRAMADA;
    }

    /** Constructor para crear una nueva función (sin ID aún persistida). */
    public Funcion(Integer peliculaId, Integer salaId,
                   LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin,
                   Double precioEntrada, EstadoFuncion estado) {
        this.peliculaId = peliculaId;
        this.salaId = salaId;
        this.fechaHoraInicio = fechaHoraInicio;
        this.fechaHoraFin = fechaHoraFin;
        this.precioEntrada = precioEntrada;
        this.estado = estado != null ? estado : EstadoFuncion.PROGRAMADA;
    }

    /** Constructor completo: usado al hidratar desde la base de datos. */
    public Funcion(Integer idFuncion, Integer peliculaId, Integer salaId,
                   LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin,
                   Double precioEntrada, EstadoFuncion estado) {
        this.idFuncion = idFuncion;
        this.peliculaId = peliculaId;
        this.salaId = salaId;
        this.fechaHoraInicio = fechaHoraInicio;
        this.fechaHoraFin = fechaHoraFin;
        this.precioEntrada = precioEntrada;
        this.estado = estado != null ? estado : EstadoFuncion.PROGRAMADA;
    }

    // --- Métodos de utilidad ---

    /** Indica si la función está actualmente programada. */
    public boolean estaProgramada() {
        return estado == EstadoFuncion.PROGRAMADA;
    }

    /** Indica si la función está en curso (ya inició, pero no ha finalizado). */
    public boolean estaEnCurso() {
        LocalDateTime ahora = LocalDateTime.now();
        return estado == EstadoFuncion.EN_CURSO &&
                ahora.isAfter(fechaHoraInicio) &&
                ahora.isBefore(fechaHoraFin);
    }

    /** Indica si la función ha finalizado. */
    public boolean haFinalizado() {
        return estado == EstadoFuncion.FINALIZADA ||
                LocalDateTime.now().isAfter(fechaHoraFin);
    }

    /** Indica si se pueden realizar reservas para esta función. */
    public boolean puedeReservar() {
        return estado == EstadoFuncion.PROGRAMADA &&
                LocalDateTime.now().isBefore(fechaHoraInicio);
    }

    /** Devuelve la duración de la función en minutos. */
    public long getDuracionMinutos() {
        return java.time.Duration.between(fechaHoraInicio, fechaHoraFin).toMinutes();
    }

    // --- Getters y Setters ---
    public Integer getIdFuncion() {
        return idFuncion;
    }

    public void setIdFuncion(Integer idFuncion) {
        this.idFuncion = idFuncion;
    }

    public Integer getPeliculaId() {
        return peliculaId;
    }

    public void setPeliculaId(Integer peliculaId) {
        this.peliculaId = peliculaId;
    }

    public Integer getSalaId() {
        return salaId;
    }

    public void setSalaId(Integer salaId) {
        this.salaId = salaId;
    }

    public LocalDateTime getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    public void setFechaHoraInicio(LocalDateTime fechaHoraInicio) {
        this.fechaHoraInicio = fechaHoraInicio;
    }

    public LocalDateTime getFechaHoraFin() {
        return fechaHoraFin;
    }

    public void setFechaHoraFin(LocalDateTime fechaHoraFin) {
        this.fechaHoraFin = fechaHoraFin;
    }

    public Double getPrecioEntrada() {
        return precioEntrada;
    }

    public void setPrecioEntrada(Double precioEntrada) {
        this.precioEntrada = precioEntrada;
    }

    public EstadoFuncion getEstado() {
        return estado;
    }

    public void setEstado(EstadoFuncion estado) {
        this.estado = estado;
    }
}
