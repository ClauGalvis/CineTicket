package com.cineticket.servicio;

import com.cineticket.dao.ComboConfiteriaDAO;
import com.cineticket.excepcion.ValidacionException;
import com.cineticket.modelo.ComboConfiteria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfiteriaServiceTest {

    @Mock ComboConfiteriaDAO comboDAO;
    @InjectMocks ConfiteriaService service;

    @Test
    void obtenerCombosDisponibles_ok() {
        when(comboDAO.listarDisponibles()).thenReturn(List.of(new ComboConfiteria(), new ComboConfiteria()));
        var res = service.obtenerCombosDisponibles();
        assertEquals(2, res.size());
        verify(comboDAO).listarDisponibles();
    }

    @Test
    void obtenerCombo_ok() {
        ComboConfiteria c = new ComboConfiteria();
        c.setIdCombo(10); c.setNombreCombo("Combo 1");
        when(comboDAO.buscarPorId(10)).thenReturn(c);

        var res = service.obtenerCombo(10);
        assertEquals(10, res.getIdCombo());
        assertEquals("Combo 1", res.getNombreCombo());
    }

    @Test
    void calcularSubtotal_ok() {
        ComboConfiteria c = new ComboConfiteria();
        c.setIdCombo(5);
        c.setPrecio(new BigDecimal("8500"));
        c.setDisponible(true);
        when(comboDAO.buscarPorId(5)).thenReturn(c);

        var subtotal = service.calcularSubtotal(5, 3);
        assertEquals(new BigDecimal("25500"), subtotal);
    }

    @Test
    void calcularSubtotal_comboNoDisponible_lanza() {
        ComboConfiteria c = new ComboConfiteria();
        c.setIdCombo(7);
        c.setPrecio(new BigDecimal("10000"));
        c.setDisponible(false);
        when(comboDAO.buscarPorId(7)).thenReturn(c);

        assertThrows(ValidacionException.class, () -> service.calcularSubtotal(7, 1));
    }

    @Test
    void calcularSubtotal_cantidadInvalida_lanza() {
        assertThrows(ValidacionException.class, () -> service.calcularSubtotal(1, 0));
        assertThrows(ValidacionException.class, () -> service.calcularSubtotal(1, -2));
    }
}
