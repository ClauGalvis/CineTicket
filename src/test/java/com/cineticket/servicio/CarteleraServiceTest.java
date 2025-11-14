package com.cineticket.servicio;

import com.cineticket.dao.FuncionDAO;
import com.cineticket.dao.PeliculaDAO;
import com.cineticket.dao.GeneroDAO;
import com.cineticket.enums.*;
import com.cineticket.excepcion.*;
import com.cineticket.modelo.*;
import com.cineticket.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarteleraServiceTest {

    @Mock PeliculaDAO peliculaDAO;
    @Mock FuncionDAO funcionDAO;
    @Mock GeneroDAO generoDAO;

    @InjectMocks CarteleraService service;

    // ==== helpers de sesión ====

    private void setAdmin() {
        Usuario u = new Usuario();
        u.setRol(Rol.ADMIN);
        SessionManager.getInstance().setUsuarioActual(u);
    }

    private void setUsuarioNormal() {
        Usuario u = new Usuario();
        u.setRol(Rol.USUARIO);
        SessionManager.getInstance().setUsuarioActual(u);
    }

    @AfterEach
    void limpiarSesion() {
        SessionManager.getInstance().cerrarSesion();
    }


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

    // ==== nuevos métodos de consulta ====

    @Test
    void obtenerFuncionesPorFecha_ok() {
        LocalDate hoy = LocalDate.now();
        when(funcionDAO.listarPorFecha(hoy)).thenReturn(List.of(new Funcion()));

        var res = service.obtenerFuncionesPorFecha(hoy);

        assertEquals(1, res.size());
        verify(funcionDAO).listarPorFecha(hoy);
    }

    @Test
    void obtenerFuncionesPorFecha_null_lanza() {
        assertThrows(ValidacionException.class, () -> service.obtenerFuncionesPorFecha(null));
        verifyNoInteractions(funcionDAO);
    }

    @Test
    void buscarPeliculasPorTitulo_ok() {
        when(peliculaDAO.buscarPorTitulo("Avatar")).thenReturn(List.of(new Pelicula()));

        var res = service.buscarPeliculasPorTitulo("Avatar");

        assertEquals(1, res.size());
        verify(peliculaDAO).buscarPorTitulo("Avatar");
    }

    @Test
    void buscarPeliculasPorTitulo_vacio_lanza() {
        assertThrows(ValidacionException.class, () -> service.buscarPeliculasPorTitulo(" "));
        verifyNoMoreInteractions(peliculaDAO);
    }

    @Test
    void obtenerGenerosDePelicula_ok() {
        when(peliculaDAO.obtenerGenerosDePelicula(1))
                .thenReturn(List.of(new Genero(), new Genero()));

        var res = service.obtenerGenerosDePelicula(1);

        assertEquals(2, res.size());
        verify(peliculaDAO).obtenerGenerosDePelicula(1);
    }

    @Test
    void obtenerGenerosDePelicula_null_lanza() {
        assertThrows(ValidacionException.class, () -> service.obtenerGenerosDePelicula(null));
        verifyNoMoreInteractions(peliculaDAO);
    }

    // ==== gestión de películas (ADMIN) ====

    @Test
    void crearPelicula_admin_ok_asignaGeneros() {
        setAdmin();

        Pelicula peli = new Pelicula();
        peli.setTitulo("Inception");
        peli.setDuracionMinutos(120);
        peli.setClasificacion(Clasificacion.T);

        when(peliculaDAO.crear(peli)).thenReturn(10);

        var generoIds = List.of(1, 2, 3);

        Integer id = service.crearPelicula(peli, generoIds);

        assertEquals(10, id);
        verify(peliculaDAO).crear(peli);
        verify(peliculaDAO).asignarGeneros(10, generoIds);
    }


    @Test
    void crearPelicula_sinTitulo_lanzaValidacion() {
        setAdmin();

        Pelicula peli = new Pelicula();
        peli.setDuracionMinutos(90);

        assertThrows(ValidacionException.class,
                () -> service.crearPelicula(peli, List.of(1)));
        verifyNoInteractions(peliculaDAO);
    }

    @Test
    void crearPelicula_noAdmin_lanzaAutenticacion() {
        setUsuarioNormal();
        Pelicula peli = new Pelicula();
        peli.setTitulo("Test");
        peli.setDuracionMinutos(100);

        assertThrows(AutenticacionException.class,
                () -> service.crearPelicula(peli, List.of(1)));

        verifyNoInteractions(peliculaDAO);
    }

    @Test
    void actualizarPelicula_admin_ok() {
        setAdmin();
        Pelicula peli = new Pelicula();
        peli.setIdPelicula(5);
        peli.setTitulo("Editada");
        peli.setDuracionMinutos(100);
        // 🔽 igual aquí
        peli.setClasificacion(Clasificacion.T);

        when(peliculaDAO.actualizar(peli)).thenReturn(true);

        boolean res = service.actualizarPelicula(peli);

        assertTrue(res);
        verify(peliculaDAO).actualizar(peli);
    }

    @Test
    void eliminarPelicula_nullId_lanza() {
        setAdmin();
        assertThrows(ValidacionException.class, () -> service.eliminarPelicula(null));
        verifyNoInteractions(peliculaDAO);
    }

    // ==== gestión de funciones (ADMIN) ====

    private Funcion crearFuncionValida() {
        Funcion f = new Funcion();
        f.setPeliculaId(1);
        f.setSalaId(2);
        f.setFechaHoraInicio(LocalDateTime.of(2025, 1, 1, 18, 0));
        f.setFechaHoraFin(LocalDateTime.of(2025, 1, 1, 20, 0));
        f.setPrecioEntrada(15000.0);
        return f;
    }

    @Test
    void crearFuncion_admin_ok() {
        setAdmin();
        Funcion f = crearFuncionValida();

        when(funcionDAO.verificarDisponibilidadSala(
                f.getSalaId(),
                f.getFechaHoraInicio(),
                f.getFechaHoraFin(),
                null)
        ).thenReturn(true);

        when(funcionDAO.crear(f)).thenReturn(7);

        Integer id = service.crearFuncion(f);

        assertEquals(7, id);
        verify(funcionDAO).crear(f);
    }

    @Test
    void crearFuncion_horarioInvalido_lanza() {
        setAdmin();
        Funcion f = crearFuncionValida();
        // fin antes de inicio
        f.setFechaHoraFin(f.getFechaHoraInicio().minusHours(1));

        assertThrows(ValidacionException.class, () -> service.crearFuncion(f));
        verifyNoMoreInteractions(funcionDAO);
    }

    @Test
    void crearFuncion_salaOcupada_lanza() {
        setAdmin();
        Funcion f = crearFuncionValida();

        when(funcionDAO.verificarDisponibilidadSala(
                f.getSalaId(),
                f.getFechaHoraInicio(),
                f.getFechaHoraFin(),
                null)
        ).thenReturn(false);

        assertThrows(ValidacionException.class, () -> service.crearFuncion(f));
        verify(funcionDAO).verificarDisponibilidadSala(
                f.getSalaId(),
                f.getFechaHoraInicio(),
                f.getFechaHoraFin(),
                null
        );
        verify(funcionDAO, never()).crear(any());
    }

    @Test
    void crearFuncion_noAdmin_lanzaAutenticacion() {
        setUsuarioNormal();
        Funcion f = crearFuncionValida();

        assertThrows(AutenticacionException.class, () -> service.crearFuncion(f));
        verifyNoInteractions(funcionDAO);
    }
}
