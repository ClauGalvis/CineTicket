package com.cineticket.modelo;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.cineticket.enums.MetodoPago;
import com.cineticket.enums.EstadoCompra;

/**
 * Representa una compra realizada por un usuario.
 * Corresponde a la tabla 'compra' en la base de datos.
 *
 * Incluye totales (entradas, confitería y general), método de pago,
 * estado y datos del comprobante PDF.
 *
 * Nota: en BD, total_general es columna generada (total_entradas + total_confiteria).
 * En el VO se mantiene sincronizado mediante utilidades simples.
 *
 * @author Claudia
 * @version 1.0
 */

public class Compra {
    private Integer idCompra;
    private Integer usuarioId;                 // FK → usuario.id_usuario
    private LocalDateTime fechaHoraCompra;     // DEFAULT now()
    private BigDecimal totalEntradas;          // >= 0
    private BigDecimal totalConfiteria;        // >= 0
    private BigDecimal totalGeneral;           // = entradas + confitería (en BD es STORED)
    private MetodoPago metodoPago;
    private EstadoCompra estadoCompra;         // DEFAULT CONFIRMADA
    private LocalDateTime fechaCancelacion;    // null si CONFIRMADA, not null si CANCELADA
    private String rutaComprobantePdf;

    // --- Constructores ---
    /** Constructor por defecto: fecha ahora, estado CONFIRMADA, totales en 0.00. */
    public Compra() {
        this.fechaHoraCompra   = LocalDateTime.now();
        this.totalEntradas     = BigDecimal.ZERO;
        this.totalConfiteria   = BigDecimal.ZERO;
        this.totalGeneral      = BigDecimal.ZERO;
        this.estadoCompra      = EstadoCompra.CONFIRMADA;
    }

    /** Constructor para crear una compra nueva (sin ID aún persistida). */
    public Compra(Integer usuarioId, BigDecimal totalEntradas, BigDecimal totalConfiteria,
                  MetodoPago metodoPago, EstadoCompra estadoCompra, String rutaComprobantePdf) {
        this();
        this.usuarioId = usuarioId;
        this.totalEntradas   = safe(totalEntradas);
        this.totalConfiteria = safe(totalConfiteria);
        this.totalGeneral    = this.totalEntradas.add(this.totalConfiteria);
        this.metodoPago      = metodoPago;
        this.estadoCompra    = (estadoCompra != null) ? estadoCompra : EstadoCompra.CONFIRMADA;
        this.rutaComprobantePdf = rutaComprobantePdf;
    }

    /** Constructor completo: usado al hidratar desde la BD. */
    public Compra(Integer idCompra, Integer usuarioId, LocalDateTime fechaHoraCompra,
                  BigDecimal totalEntradas, BigDecimal totalConfiteria, BigDecimal totalGeneral,
                  MetodoPago metodoPago, EstadoCompra estadoCompra,
                  LocalDateTime fechaCancelacion, String rutaComprobantePdf) {
        this.idCompra = idCompra;
        this.usuarioId = usuarioId;
        this.fechaHoraCompra = fechaHoraCompra;
        this.totalEntradas   = safe(totalEntradas);
        this.totalConfiteria = safe(totalConfiteria);
        // si totalGeneral viene null (no debería), lo recalculamos
        this.totalGeneral    = (totalGeneral != null) ? totalGeneral : this.totalEntradas.add(this.totalConfiteria);
        this.metodoPago = metodoPago;
        this.estadoCompra = (estadoCompra != null) ? estadoCompra : EstadoCompra.CONFIRMADA;
        this.fechaCancelacion = fechaCancelacion;
        this.rutaComprobantePdf = rutaComprobantePdf;
    }

    // --- Metodos de utilidad ---
    /** Recalcula total_general = total_entradas + total_confiteria. */
    public void recalcularTotalGeneral() {
        this.totalGeneral = safe(totalEntradas).add(safe(totalConfiteria));
    }

    /** ¿La compra está confirmada? */
    public boolean estaConfirmada() {
        return estadoCompra == EstadoCompra.CONFIRMADA;
    }

    /** ¿La compra está cancelada? */
    public boolean estaCancelada() {
        return estadoCompra == EstadoCompra.CANCELADA;
    }

    // Helper para evitar nulls en BigDecimal
    private BigDecimal safe(BigDecimal val) { return (val != null) ? val : BigDecimal.ZERO; }

    // --- Getters y Setters
    public Integer getIdCompra() {
        return idCompra;
    }

    public void setIdCompra(Integer idCompra) {
        this.idCompra = idCompra;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Integer usuarioId) {
        this.usuarioId = usuarioId;
    }

    public LocalDateTime getFechaHoraCompra() {
        return fechaHoraCompra;
    }

    public void setFechaHoraCompra(LocalDateTime fechaHoraCompra) {
        this.fechaHoraCompra = fechaHoraCompra;
    }

    public BigDecimal getTotalEntradas() {
        return totalEntradas;
    }

    public void setTotalEntradas(BigDecimal totalEntradas) {
        this.totalEntradas = safe(totalEntradas);
        recalcularTotalGeneral();
    }

    public BigDecimal getTotalConfiteria() {
        return totalConfiteria;
    }

    public void setTotalConfiteria(BigDecimal totalConfiteria) {
        this.totalConfiteria = safe(totalConfiteria);
        recalcularTotalGeneral();
    }

    public BigDecimal getTotalGeneral() {
        return totalGeneral;
    }

    public void setTotalGeneral(BigDecimal totalGeneral) {
        this.totalGeneral = totalGeneral;
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(MetodoPago metodoPago) {
        this.metodoPago = metodoPago;
    }

    public EstadoCompra getEstadoCompra() {
        return estadoCompra;
    }

    public void setEstadoCompra(EstadoCompra estadoCompra) {
        this.estadoCompra = estadoCompra;
    }

    public LocalDateTime getFechaCancelacion() {
        return fechaCancelacion;
    }

    public void setFechaCancelacion(LocalDateTime fechaCancelacion) {
        this.fechaCancelacion = fechaCancelacion;
    }

    public String getRutaComprobantePdf() {
        return rutaComprobantePdf;
    }
    public void setRutaComprobantePdf(String rutaComprobantePdf) {
        this.rutaComprobantePdf = rutaComprobantePdf;
    }




}
