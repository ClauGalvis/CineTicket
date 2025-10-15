package com.cineticket.modelo;

import java.math.BigDecimal;
import com.cineticket.enums.EstadoEntrada;

/**
 * Representa una entrada (boleto) asociada a una compra para una función específica.
 * Corresponde a la tabla 'entrada' en la base de datos.
 *
 * Evita la doble venta a nivel BD con UNIQUE(funcion_id, asiento_id WHERE estado_entrada='ACTIVA').
 * Este VO mantiene datos puros y utilidades ligeras.
 *
 * @author Claudia
 * @version 1.0
 */
public class Entrada {

    private Integer idEntrada;
    private Integer compraId;      // FK → compra.id_compra
    private Integer funcionId;     // FK → funcion.id_funcion
    private Integer asientoId;     // FK → asiento.id_asiento
    private BigDecimal precioUnitario;  // >= 0
    private EstadoEntrada estadoEntrada; // ACTIVA por defecto

    /**
     * Constructor por defecto: estado ACTIVA y precio 0.00.
     */
    public Entrada() {
        this.precioUnitario = BigDecimal.ZERO;
        this.estadoEntrada = EstadoEntrada.ACTIVA;
    }

    /**
     * Constructor para crear una entrada nueva (sin ID aún persistida).
     */
    public Entrada(Integer compraId, Integer funcionId, Integer asientoId,
                   BigDecimal precioUnitario, EstadoEntrada estadoEntrada) {
        this();
        this.compraId = compraId;
        this.funcionId = funcionId;
        this.asientoId = asientoId;
        this.precioUnitario = (precioUnitario != null) ? precioUnitario : BigDecimal.ZERO;
        if (estadoEntrada != null) this.estadoEntrada = estadoEntrada;
    }

    /**
     * Constructor completo: usado al hidratar desde la base de datos.
     */
    public Entrada(Integer idEntrada, Integer compraId, Integer funcionId, Integer asientoId,
                   BigDecimal precioUnitario, EstadoEntrada estadoEntrada) {
        this.idEntrada = idEntrada;
        this.compraId = compraId;
        this.funcionId = funcionId;
        this.asientoId = asientoId;
        this.precioUnitario = (precioUnitario != null) ? precioUnitario : BigDecimal.ZERO;
        this.estadoEntrada = (estadoEntrada != null) ? estadoEntrada : EstadoEntrada.ACTIVA;
    }

    // --- Metodos de utilidad ---
    public boolean estaActiva() {
        return estadoEntrada == EstadoEntrada.ACTIVA;
    }

    public boolean estaCancelada() {
        return estadoEntrada == EstadoEntrada.CANCELADA;
    }

    public boolean estaUtilizada() {
        return estadoEntrada == EstadoEntrada.UTILIZADA;
    }

    // --- Getters y Setters
    public Integer getIdEntrada() {
        return idEntrada;
    }

    public void setIdEntrada(Integer idEntrada) {
        this.idEntrada = idEntrada;
    }

    public Integer getCompraId() {
        return compraId;
    }

    public void setCompraId(Integer compraId) {
        this.compraId = compraId;
    }

    public Integer getFuncionId() {
        return funcionId;
    }

    public void setFuncionId(Integer funcionId) {
        this.funcionId = funcionId;
    }

    public Integer getAsientoId() {
        return asientoId;
    }

    public void setAsientoId(Integer asientoId) {
        this.asientoId = asientoId;
    }


    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = (precioUnitario != null) ? precioUnitario : BigDecimal.ZERO;
    }

    public EstadoEntrada getEstadoEntrada() {
        return estadoEntrada;
    }

    public void setEstadoEntrada(EstadoEntrada estadoEntrada) {
        this.estadoEntrada = estadoEntrada;
    }
}
