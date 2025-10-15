package com.cineticket.modelo;

/**
 * Representa una sala de proyección dentro del sistema CineTicket.
 * Contiene la configuración básica y la capacidad de asientos.
 *
 * Corresponde directamente a la tabla 'sala' en la base de datos.
 *
 * @author Claudia Patricia
 * @version 1.0
 */
public class Sala {
    private Integer idSala;
    private String nombreSala;
    private Integer capacidadTotal;
    private Integer filas;
    private Integer columnas;
    private boolean activa;

    // --- Constructores ---
    /** Constructor por defecto: crea una sala activa. */
    public Sala() {
        this.activa = true;
    }

    /** Constructor para crear una nueva sala (sin ID, aún no persistida). */
    public Sala(String nombreSala, Integer filas, Integer columnas) {
        this();
        this.nombreSala = nombreSala;
        this.filas = filas;
        this.columnas = columnas;
        this.capacidadTotal = filas * columnas;
    }

    /** Constructor completo: se usa al hidratar desde la base de datos. */
    public Sala(Integer idSala, String nombreSala, Integer capacidadTotal,
                Integer filas, Integer columnas, boolean activa) {
        this.idSala = idSala;
        this.nombreSala = nombreSala;
        this.capacidadTotal = capacidadTotal;
        this.filas = filas;
        this.columnas = columnas;
        this.activa = activa;
    }

    // --- Metodos de utilidad ---
    /** Calcula la capacidad total (filas x columnas) */
    public int calcularCapacidad() {
        return filas * columnas;
    }

    /** Indica si la sala esta activa y disponible para funciones */
    public boolean estaActiva() {
        return activa;
    }

    // --- Getters y Setters ---
    public Integer getIdSala() {
        return idSala;
    }

    public void setIdSala(Integer idSala) {
        this.idSala = idSala;
    }

    public String getNombreSala() {
        return nombreSala;
    }

    public void setNombreSala(String nombreSala) {
        this.nombreSala = nombreSala;
    }

    public Integer getCapacidadTotal() {
        return capacidadTotal;
    }

    public void setCapacidadTotal(Integer capacidadTotal) {
        this.capacidadTotal = capacidadTotal;
    }

    public Integer getFilas() {
        return filas;
    }

    public void setFilas(Integer filas) {
        this.filas = filas;
    }

    public Integer getColumnas() {
        return columnas;
    }

    public void setColumnas(Integer columnas) {
        this.columnas = columnas;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }
}
