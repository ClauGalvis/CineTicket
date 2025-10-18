package com.cineticket.servicio;

import com.cineticket.dao.EntradaDAO;
import com.cineticket.dao.FuncionDAO;
import com.cineticket.enums.EstadoEntrada;
import com.cineticket.enums.EstadoFuncion;
import com.cineticket.excepcion.ValidacionException;
import com.cineticket.excepcion.AsientoNoDisponibleException;
import com.cineticket.modelo.Entrada;
import com.cineticket.modelo.Funcion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock EntradaDAO entradaDAO;
    @Mock FuncionDAO funcionDAO;

    @InjectMocks ReservaService service;

    private Funcion funcionProgramada() {
        Funcion f = new Funcion();
        f.setIdFuncion(2);
        f.setSalaId(8);
        f.setPeliculaId(26);
        f.setFechaHoraInicio(LocalDateTime.now().plusHours(2));
        f.setFechaHoraFin(LocalDateTime.now().plusHours(4));
        f.setPrecioEntrada(18000.0);                 // DAO la mapea como double
        f.setEstado(EstadoFuncion.PROGRAMADA);
        return f;
    }

    @Test
    void ocupados_ok() {
        Entrada e = new Entrada();
        e.setAsientoId(5);
        e.setEstadoEntrada(EstadoEntrada.ACTIVA);    // ✅ usar el enum

        when(entradaDAO.listarPorFuncion(2)).thenReturn(List.of(e));

        var res = service.obtenerAsientosOcupadosPorFuncion(2);
        assertEquals(List.of(5), res);
    }

    @Test
    void verificarDisponibilidad_ok() {
        when(entradaDAO.verificarAsientoDisponible(2, 7)).thenReturn(true);
        when(entradaDAO.verificarAsientoDisponible(2, 8)).thenReturn(true);
        assertTrue(service.verificarDisponibilidadAsientos(2, List.of(7, 8)));
    }

    @Test
    void reservar_ok() {
        when(funcionDAO.buscarPorId(2)).thenReturn(funcionProgramada());
        when(entradaDAO.verificarAsientoDisponible(2, 7)).thenReturn(true);
        when(entradaDAO.verificarAsientoDisponible(2, 8)).thenReturn(true);

        var entradas = service.reservarAsientos(2, List.of(7, 8));
        assertEquals(2, entradas.size());
        assertEquals(2, entradas.get(0).getFuncionId());
        assertEquals(7, entradas.get(0).getAsientoId());
        assertEquals(new BigDecimal("18000.0"), entradas.get(0).getPrecioUnitario());
    }

    @Test
    void reservar_asientoOcupado_lanza() {
        when(funcionDAO.buscarPorId(2)).thenReturn(funcionProgramada());
        when(entradaDAO.verificarAsientoDisponible(2, 7)).thenReturn(false);

        assertThrows(AsientoNoDisponibleException.class,
                () -> service.reservarAsientos(2, List.of(7)));
    }

    @Test
    void reservar_masDeCinco_lanza() {
        assertThrows(ValidacionException.class,
                () -> service.reservarAsientos(2, List.of(1,2,3,4,5,6)));

        // Asegura que no tocó DAO alguno (corta por la validación de límite)
        verifyNoInteractions(funcionDAO, entradaDAO);
    }
}
