package com.cineticket.servicio;

import com.cineticket.dao.ComboConfiteriaDAO;
import com.cineticket.excepcion.ValidacionException;
import com.cineticket.modelo.ComboConfiteria;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class ConfiteriaService {

    private final ComboConfiteriaDAO comboDAO;

    public ConfiteriaService(ComboConfiteriaDAO comboDAO) {
        this.comboDAO = Objects.requireNonNull(comboDAO);
    }

    /** Retorna solo combos disponibles para la venta. */
    public List<ComboConfiteria> obtenerCombosDisponibles() {
        // según tu estilo de DAOs (listarActivas/listarDisponibles)
        // si tu DAO se llama listarActivos(), cámbialo aquí:
        return comboDAO.listarDisponibles();
    }

    /** Obtiene un combo por ID (y valida que exista). */
    public ComboConfiteria obtenerCombo(Integer comboId) {
        if (comboId == null) throw new ValidacionException("comboId es requerido.");
        ComboConfiteria c = comboDAO.buscarPorId(comboId);
        if (c == null) throw new ValidacionException("Combo no encontrado.");
        return c;
    }

    /** Subtotal = precio del combo * cantidad (cantidad > 0). */
    public BigDecimal calcularSubtotal(Integer comboId, Integer cantidad) {
        if (comboId == null) throw new ValidacionException("comboId es requerido.");
        if (cantidad == null || cantidad <= 0) {
            throw new ValidacionException("La cantidad debe ser mayor que cero.");
        }
        ComboConfiteria combo = comboDAO.buscarPorId(comboId);
        if (combo == null) throw new ValidacionException("Combo no encontrado.");
        if (!combo.estaDisponible()) {
            throw new ValidacionException("El combo no está disponible.");
        }
        return combo.getPrecio().multiply(BigDecimal.valueOf(cantidad));
    }
}
