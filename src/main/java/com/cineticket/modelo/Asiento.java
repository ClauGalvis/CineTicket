package com.cineticket.modelo;

import com.cineticket.enums.TipoAsiento;

/**
 * Representa un asiento físico dentro de una sala.
 * Corresponde a la tabla 'asiento' en la base de datos.
 *
 * Responsable de encapsular ubicación (fila, número), tipo y estado.
 *
 * @author Claudia Patricia
 * @version 1.0
 */
public class Asiento {
    private Integer idAsiento;
    private Integer salaId;     // FK sala.id_sala
    private String fila;
    private Integer numero;
    private TipoAsiento tipoAsiento;
    private boolean activo;

    // --- Constructores ---
    /** Constructor por defecto: asiento activo tipo REGULAR. */
    public Asiento() {
        this.tipoAsiento = TipoAsiento.REGULAR;
        this.activo = true;
    }

    /** Constructor para crear un asiento nuevo (sin ID aún persistido). */
    public Asiento(Integer salaId, String fila, Integer numero, TipoAsiento tipoAsiento) {
        this();
        this.salaId = salaId;
        this.fila = fila;
        this.numero = numero;
        if (tipoAsiento != null) {
            this.tipoAsiento = tipoAsiento;
        }
    }

    /** Constructor completo: usado para hidratar desde la base de datos. */
    public Asiento(Integer idAsiento, Integer salaId, String fila, Integer numero,
                   TipoAsiento tipoAsiento, boolean activo) {
        this.idAsiento = idAsiento;
        this.salaId = salaId;
        this.fila = fila;
        this.numero = numero;
        this.tipoAsiento = tipoAsiento != null ? tipoAsiento : TipoAsiento.REGULAR;
        this.activo = activo;
    }

    // --- Metodos de utilidad ---
    /** Identificador legible del asiento, por ejemplo "A5" */
    public String getIdentificador() {
        return (fila != null ? fila : "") + (numero != null ? numero : "");
    }

    /**
     * Es asiento VIP? */
    public boolean esVIP() {
        return tipoAsiento == TipoAsiento.VIP;
    }

    /** Es asiento preferencial? */
    public boolean esPreferencial() {
        return tipoAsiento == TipoAsiento.PREFERENCIAL;
    }

    /** Esta activo? */
    public boolean estaActivo() {
        return activo;
    }

    // --- Getters y Setters ---
    public Integer getIdAsiento() {
        return idAsiento;
    }

    public void setIdAsiento(Integer idAsiento) {
        this.idAsiento = idAsiento;
    }

    public Integer getSalaId() {
        return salaId;
    }

    public void setSalaId(Integer salaId) {
        this.salaId = salaId;
    }

    public String getFila() {
        return fila;
    }

    public void setFila(String fila) {
        this.fila = fila;
    }

    public Integer getNumero() {
        return numero;
    }

    public void setNumero(Integer numero) {
        this.numero = numero;
    }

    public TipoAsiento getTipoAsiento() {
        return tipoAsiento;
    }

    public void setTipoAsiento(TipoAsiento tipoAsiento) {
        this.tipoAsiento = tipoAsiento;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public static class Entrada {
    }
}
