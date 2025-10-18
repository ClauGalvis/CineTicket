package com.cineticket.servicio;

import com.cineticket.dao.FuncionDAO;
import com.cineticket.dao.PeliculaDAO;
import com.cineticket.excepcion.ValidacionException;
import com.cineticket.modelo.Funcion;
import com.cineticket.modelo.Pelicula;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarteleraServiceTest {

    @Mock PeliculaDAO peliculaDAO;
    @Mock FuncionDAO funcionDAO;

    @InjectMocks CarteleraService service;

    @Test
    void obtenerCarteleraCompleta_ok() {
        when(peliculaDAO.listarActivas()).thenReturn(List.of(new Pelicula(), new Pelicula()));
        var res = service.obtenerCarteleraCompleta();
        assertEquals(2, res.size());
        verify(peliculaDAO).listarActivas();
    }

    @Test
    void obtenerFuncionesPorPelicula_ok() {
        when(funcionDAO.listarPorPelicula(7)).thenReturn(List.of(new Funcion()));
        var res = service.obtenerFuncionesPorPelicula(7);
        assertEquals(1, res.size());
        verify(funcionDAO).listarPorPelicula(7);
    }

    @Test
    void obtenerFuncionesPorPelicula_null_lanza() {
        assertThrows(ValidacionException.class, () -> service.obtenerFuncionesPorPelicula(null));
        verifyNoInteractions(funcionDAO);
    }

    @Test
    void obtenerDetallesPelicula_ok() {
        Pelicula p = new Pelicula(); p.setIdPelicula(5); p.setTitulo("Matrix");
        when(peliculaDAO.buscarPorId(5)).thenReturn(p);
        var res = service.obtenerDetallesPelicula(5);
        assertEquals("Matrix", res.getTitulo());
        verify(peliculaDAO).buscarPorId(5);
    }

    @Test
    void obtenerDetallesPelicula_noExiste_lanza() {
        when(peliculaDAO.buscarPorId(99)).thenReturn(null);
        assertThrows(ValidacionException.class, () -> service.obtenerDetallesPelicula(99));
    }
}
