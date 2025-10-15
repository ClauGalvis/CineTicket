package com.cineticket.modelo;

import java.math.BigDecimal;

/**
 * Representa un combo o producto de confitería disponible en el sistema CineTicket.
 * Corresponde directamente a la tabla 'combo_confiteria' en la base de datos.
 *
 * Incluye datos básicos como nombre, descripción, precio, disponibilidad e imagen.
 *
 * @author Claudia
 * @version 1.0
 */
public class ComboConfiteria {

    private Integer idCombo;
    private String nombreCombo;
    private String descripcion;
    private BigDecimal precio;
    private String imagenUrl;
    private boolean disponible;
    private String categoria; // Ej.: "Combos", "Snacks", "Bebidas"

    /**
     * Constructor por defecto: combo disponible y precio 0.00.
     */
    public ComboConfiteria() {
        this.disponible = true;
        this.precio = BigDecimal.ZERO;
    }

    /**
     * Constructor para crear un nuevo combo (sin ID aún persistido).
     */
    public ComboConfiteria(String nombreCombo, String descripcion,
                           BigDecimal precio, String imagenUrl, boolean disponible, String categoria) {
        this.nombreCombo = nombreCombo;
        this.descripcion = descripcion;
        this.precio = (precio != null) ? precio : BigDecimal.ZERO;
        this.imagenUrl = imagenUrl;
        this.disponible = disponible;
        this.categoria = categoria;
    }

    /**
     * Constructor completo: usado al hidratar desde la base de datos.
     */
    public ComboConfiteria(Integer idCombo, String nombreCombo, String descripcion,
                           BigDecimal precio, String imagenUrl, boolean disponible, String categoria) {
        this.idCombo = idCombo;
        this.nombreCombo = nombreCombo;
        this.descripcion = descripcion;
        this.precio = (precio != null) ? precio : BigDecimal.ZERO;
        this.imagenUrl = imagenUrl;
        this.disponible = disponible;
        this.categoria = categoria;
    }

    // --- Métodos de utilidad ---

    /**
     * Indica si el combo está disponible para la venta.
     */
    public boolean estaDisponible() {
        return disponible;
    }

    /**
     * Cambia la disponibilidad del combo (por ejemplo, si se agota).
     */
    public void cambiarDisponibilidad(boolean disponible) {
        this.disponible = disponible;
    }

    // --- Getters y Setters
    public Integer getIdCombo() {
        return idCombo;
    }

    public void setIdCombo(Integer idCombo) {
        this.idCombo = idCombo;
    }

    public String getNombreCombo() {
        return nombreCombo;
    }

    public void setNombreCombo(String nombreCombo) {
        this.nombreCombo = nombreCombo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = (precio != null) ? precio : BigDecimal.ZERO;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }
}
