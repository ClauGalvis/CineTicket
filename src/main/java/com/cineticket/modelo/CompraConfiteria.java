package com.cineticket.modelo;

import java.math.BigDecimal;

/**
 * Representa un ítem de confitería dentro de una compra:
 * el combo seleccionado, su cantidad y el precio unitario al momento de la compra.
 * Corresponde a la tabla 'compra_confiteria' en la base de datos.
 *
 * Nota: en BD, 'subtotal' es columna generada (cantidad * precio_unitario).
 * En el VO se mantiene sincronizado mediante utilidades simples.
 *
 * ON DELETE CASCADE en compra_id se gestiona en BD y DAO (no en este VO).
 *
 * @author Claudia
 * @version 1.0
 */
public class CompraConfiteria {

    private Integer idCompraConfiteria;
    private Integer compraId;        // FK → compra.id_compra
    private Integer comboId;         // FK → combo_confiteria.id_combo
    private Integer cantidad;        // > 0
    private BigDecimal precioUnitario; // >= 0
    private BigDecimal subtotal;       // cantidad * precioUnitario (en BD: STORED)

    /**
     * Constructor por defecto: cantidad 1, precio 0.00, subtotal 0.00.
     */
    public CompraConfiteria() {
        this.cantidad = 1;
        this.precioUnitario = BigDecimal.ZERO;
        this.subtotal = BigDecimal.ZERO;
    }

    /**
     * Constructor para crear un ítem nuevo (sin ID aún persistido).
     */
    public CompraConfiteria(Integer compraId, Integer comboId, Integer cantidad, BigDecimal precioUnitario) {
        this();
        this.compraId = compraId;
        this.comboId = comboId;
        this.cantidad = (cantidad != null && cantidad > 0) ? cantidad : 1;
        this.precioUnitario = (precioUnitario != null) ? precioUnitario : BigDecimal.ZERO;
        recalcularSubtotal();
    }

    /**
     * Constructor completo: usado al hidratar desde la base de datos.
     */
    public CompraConfiteria(Integer idCompraConfiteria, Integer compraId, Integer comboId,
                            Integer cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {
        this.idCompraConfiteria = idCompraConfiteria;
        this.compraId = compraId;
        this.comboId = comboId;
        this.cantidad = (cantidad != null && cantidad > 0) ? cantidad : 1;
        this.precioUnitario = (precioUnitario != null) ? precioUnitario : BigDecimal.ZERO;
        // si subtotal viene null (no debería), lo recalculamos
        this.subtotal = (subtotal != null) ? subtotal : calcularSubtotal();
    }

    // --- Metodos de utilidad ---
    /** Recalcula subtotal = cantidad * precioUnitario. */
    public void recalcularSubtotal() {
        this.subtotal = calcularSubtotal();
    }

    private BigDecimal calcularSubtotal() {
        BigDecimal cant = BigDecimal.valueOf(this.cantidad != null ? this.cantidad : 0);
        return this.precioUnitario.multiply(cant);
    }

    // --- Getters y Setters
    public Integer getIdCompraConfiteria() {
        return idCompraConfiteria;
    }

    public void setIdCompraConfiteria(Integer idCompraConfiteria) {
        this.idCompraConfiteria = idCompraConfiteria;
    }

    public Integer getCompraId() {
        return compraId;
    }

    public void setCompraId(Integer compraId) {
        this.compraId = compraId;
    }

    public Integer getComboId() {
        return comboId;
    }

    public void setComboId(Integer comboId) {
        this.comboId = comboId;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = (cantidad != null && cantidad > 0) ? cantidad : 1;
        recalcularSubtotal();
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = (precioUnitario != null) ? precioUnitario : BigDecimal.ZERO;
        recalcularSubtotal();
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}