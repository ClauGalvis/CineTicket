package com.cineticket.servicio;

import com.cineticket.dao.CompraConfiteriaDAO;
import com.cineticket.dao.CompraDAO;
import com.cineticket.dao.EntradaDAO;
import com.cineticket.dao.FuncionDAO;
import com.cineticket.dao.PeliculaDAO;
import com.cineticket.enums.Rol;
import com.cineticket.excepcion.AutenticacionException;
import com.cineticket.modelo.Compra;
import com.cineticket.modelo.CompraConfiteria;
import com.cineticket.modelo.Entrada;
import com.cineticket.modelo.Funcion;
import com.cineticket.modelo.Pelicula;
import com.cineticket.modelo.Usuario;
import com.cineticket.util.SessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock CompraDAO compraDAO;
    @Mock EntradaDAO entradaDAO;
    @Mock CompraConfiteriaDAO compraConfiteriaDAO;
    @Mock PeliculaDAO peliculaDAO;
    @Mock FuncionDAO funcionDAO;

    ReporteService service;

    @BeforeEach
    void setUp() {
        service = new ReporteService(
                compraDAO, entradaDAO, compraConfiteriaDAO, peliculaDAO, funcionDAO
        );
        // Dejamos la sesión limpia y luego seteamos admin
        SessionManager.getInstance().cerrarSesion();
        Usuario admin = new Usuario();
        admin.setRol(Rol.ADMIN);
        SessionManager.getInstance().setUsuarioActual(admin);
    }

    @AfterEach
    void limpiarSesion() {
        SessionManager.getInstance().cerrarSesion();
    }

    // ======================= Helpers de fixtures =======================

    private Compra compra(int id, int usuarioId, LocalDateTime fecha, String totEntradas, String totConf, String totGeneral) {
        Compra c = new Compra();
        c.setIdCompra(id);
        c.setUsuarioId(usuarioId);
        c.setFechaHoraCompra(fecha);
        c.setTotalEntradas(new BigDecimal(totEntradas));
        c.setTotalConfiteria(new BigDecimal(totConf));
        c.setTotalGeneral(new BigDecimal(totGeneral));
        return c;
    }

    private Entrada entrada(int compraId, int funcionId, int asientoId, String precio) {
        Entrada e = new Entrada();
        e.setCompraId(compraId);
        e.setFuncionId(funcionId);
        e.setAsientoId(asientoId);
        e.setPrecioUnitario(new BigDecimal(precio));
        return e;
    }

    private Funcion funcion(int id, int peliculaId) {
        Funcion f = new Funcion();
        f.setIdFuncion(id);
        f.setPeliculaId(peliculaId);
        return f;
    }

    private Pelicula pelicula(int id, String titulo) {
        Pelicula p = new Pelicula();
        p.setIdPelicula(id);
        p.setTitulo(titulo);
        return p;
    }

    private CompraConfiteria item(int compraId, int comboId, int cantidad, String precioUnitario) {
        CompraConfiteria i = new CompraConfiteria();
        i.setCompraId(compraId);
        i.setComboId(comboId);
        i.setCantidad(cantidad);
        i.setPrecioUnitario(new BigDecimal(precioUnitario));
        // subtotal es GENERATED en BD; para el servicio no es necesario
        return i;
    }

    // ======================= Tests =======================

    @Test
    void generarReporteVentasPorDia_ok() {
        LocalDate dia = LocalDate.of(2025, 10, 31);
        LocalDateTime h09 = dia.atTime(9, 10);
        LocalDateTime h14 = dia.atTime(14, 30);
        LocalDateTime h21 = dia.atTime(21, 45);

        // 3 compras ese día
        Compra c1 = compra(1, 101, h09, "36000", "0", "36000");
        Compra c2 = compra(2, 102, h14, "18000", "25000", "43000");
        Compra c3 = compra(3, 103, h21, "0", "12000", "12000");

        when(compraDAO.obtenerComprasEntreFechas(
                eq(dia.atStartOfDay()),
                eq(dia.plusDays(1).atStartOfDay().minusNanos(1))
        )).thenReturn(List.of(c1, c2, c3));

        // Entradas por compra
        when(entradaDAO.listarPorCompra(1)).thenReturn(List.of(
                entrada(1, 10, 7, "18000"),
                entrada(1, 10, 8, "18000")
        ));
        when(entradaDAO.listarPorCompra(2)).thenReturn(List.of(
                entrada(2, 11, 1, "18000")
        ));
        when(entradaDAO.listarPorCompra(3)).thenReturn(Collections.emptyList());

        // Confitería por compra (solo cuenta cantidades)
        when(compraConfiteriaDAO.listarPorCompra(1)).thenReturn(Collections.emptyList());
        when(compraConfiteriaDAO.listarPorCompra(2)).thenReturn(List.of(
                item(2, 20, 2, "12500")
        ));
        when(compraConfiteriaDAO.listarPorCompra(3)).thenReturn(List.of(
                item(3, 22, 1, "12000")
        ));

        Map<String, Object> rep = service.generarReporteVentasPorDia(dia);

        assertEquals(dia, rep.get("fecha"));
        assertEquals(3, rep.get("totalCompras"));
        assertEquals(3, rep.get("totalEntradas")); // 2 + 1 + 0
        assertEquals(3, rep.get("totalCombos"));   // 0 + 2 + 1

        assertEquals(new BigDecimal("54000.00"), rep.get("ingresosEntradas"));   // 36k + 18k + 0
        assertEquals(new BigDecimal("37000.00"), rep.get("ingresosConfiteria")); // 0 + 25k + 12k
        assertEquals(new BigDecimal("91000.00"), rep.get("ingresosTotales"));    // 54k + 37k

        @SuppressWarnings("unchecked")
        Map<Integer, BigDecimal> porHora = (Map<Integer, BigDecimal>) rep.get("porHora");
        assertEquals(new BigDecimal("36000"), porHora.get(9));
        assertEquals(new BigDecimal("43000"), porHora.get(14));
        assertEquals(new BigDecimal("12000"), porHora.get(21));
    }

    @Test
    void generarReporteVentasPorPelicula_ok() {
        int peliId = 100;
        LocalDate ini = LocalDate.of(2025, 10, 1);
        LocalDate fin = LocalDate.of(2025, 10, 31);

        // Película y funciones
        when(peliculaDAO.buscarPorId(peliId)).thenReturn(pelicula(peliId, "Interestelar 2"));

        Funcion fA = funcion(50, peliId);
        Funcion fB = funcion(51, 999); // otra película

        when(funcionDAO.buscarPorId(50)).thenReturn(fA);
        when(funcionDAO.buscarPorId(51)).thenReturn(fB);

        // Compras en el rango
        Compra c1 = compra(1, 10, ini.atTime(10, 0), "18000", "0", "18000");
        Compra c2 = compra(2, 10, ini.plusDays(5).atTime(20, 0), "36000", "0", "36000");
        when(compraDAO.obtenerComprasEntreFechas(
                eq(ini.atStartOfDay()),
                eq(fin.plusDays(1).atStartOfDay().minusNanos(1))
        )).thenReturn(List.of(c1, c2));

        // Entradas por compra:
        // c1: 1 entrada de la película (funcion 50)
        when(entradaDAO.listarPorCompra(1)).thenReturn(List.of(
                entrada(1, 50, 1, "18000")
        ));
        // c2: 1 entrada de la película (50) y 1 de otra (51)
        when(entradaDAO.listarPorCompra(2)).thenReturn(List.of(
                entrada(2, 50, 2, "18000"),
                entrada(2, 51, 3, "20000")
        ));

        Map<String, Object> rep = service.generarReporteVentasPorPelicula(peliId, ini, fin);

        assertEquals(peliId, rep.get("peliculaId"));
        assertEquals("Interestelar 2", rep.get("titulo"));
        assertEquals(2, rep.get("entradasVendidas")); // solo las con funcion->peliculaId=100
        assertEquals(new BigDecimal("36000.00"), rep.get("ingresosAproxEntradas"));

        @SuppressWarnings("unchecked")
        Set<Integer> funciones = (Set<Integer>) rep.get("funcionesAfectadas");
        assertTrue(funciones.contains(50));
        assertEquals(1, funciones.size()); // Solo la 50
    }

    @Test
    void generarReporteVentasConfiteria_ok() {
        LocalDate ini = LocalDate.of(2025, 10, 1);
        LocalDate fin = LocalDate.of(2025, 10, 31);

        // comboId -> cantidad
        Map<Integer, Integer> ventas = new LinkedHashMap<>();
        ventas.put(10, 5);
        ventas.put(11, 2);
        ventas.put(12, 7);

        when(compraConfiteriaDAO.obtenerVentasPorCombo(
                eq(ini.atStartOfDay()),
                eq(fin.plusDays(1).atStartOfDay().minusNanos(1))
        )).thenReturn(ventas);

        Map<String, Object> rep = service.generarReporteVentasConfiteria(ini, fin);
        assertEquals(14, rep.get("totalCombos")); // 5+2+7

        @SuppressWarnings("unchecked")
        Map<Integer, Integer> out = (Map<Integer, Integer>) rep.get("ventasPorCombo");
        assertEquals(3, out.size());
        assertEquals(5, out.get(10));
        assertEquals(7, out.get(12));
    }

    @Test
    void obtenerTopPeliculas_ok() {
        LocalDate ini = LocalDate.of(2025, 10, 1);
        LocalDate fin = LocalDate.of(2025, 10, 31);

        // Compras en el rango
        Compra c1 = compra(1, 10, ini.atTime(10, 0), "36000", "0", "36000");
        Compra c2 = compra(2, 11, ini.atTime(12, 0), "54000", "0", "54000");
        when(compraDAO.obtenerComprasEntreFechas(
                eq(ini.atStartOfDay()),
                eq(fin.plusDays(1).atStartOfDay().minusNanos(1))
        )).thenReturn(List.of(c1, c2));

        // Entradas por compra (mapean a funciones/películas)
        // c1: 2 entradas funcion 50 -> película 100
        when(entradaDAO.listarPorCompra(1)).thenReturn(List.of(
                entrada(1, 50, 1, "18000"),
                entrada(1, 50, 2, "18000")
        ));
        // c2: 1 entrada funcion 50 (película 100), 2 entradas funcion 51 (película 101)
        when(entradaDAO.listarPorCompra(2)).thenReturn(List.of(
                entrada(2, 50, 1, "18000"),
                entrada(2, 51, 2, "20000"),
                entrada(2, 51, 3, "20000")
        ));

        // Funciones -> películas
        when(funcionDAO.buscarPorId(50)).thenReturn(funcion(50, 100));
        when(funcionDAO.buscarPorId(51)).thenReturn(funcion(51, 101));

        // Títulos
        when(peliculaDAO.buscarPorId(100)).thenReturn(pelicula(100, "Película A"));
        when(peliculaDAO.buscarPorId(101)).thenReturn(pelicula(101, "Película B"));

        var top = service.obtenerTopPeliculas(5, ini, fin);
        assertEquals(2, top.size());

        // Película 100: 3 entradas → 54,000
        // Película 101: 2 entradas → 40,000
        assertEquals(100, top.get(0).get("peliculaId"));
        assertEquals(3, top.get(0).get("entradasVendidas"));
        assertEquals(new BigDecimal("54000.00"), top.get(0).get("ingresosAproxEntradas"));

        assertEquals(101, top.get(1).get("peliculaId"));
        assertEquals(2, top.get(1).get("entradasVendidas"));
        assertEquals(new BigDecimal("40000.00"), top.get(1).get("ingresosAproxEntradas"));
    }

    @Test
    void calcularIngresosTotales_ok() {
        LocalDate ini = LocalDate.of(2025, 10, 10);
        LocalDate fin = LocalDate.of(2025, 10, 10);

        Compra c1 = compra(1, 1, ini.atStartOfDay().plusHours(9),  "18000", "12000", "30000");
        Compra c2 = compra(2, 2, ini.atStartOfDay().plusHours(12), "36000", "0",     "36000");
        when(compraDAO.obtenerComprasEntreFechas(
                eq(ini.atStartOfDay()),
                eq(fin.plusDays(1).atStartOfDay().minusNanos(1))
        )).thenReturn(List.of(c1, c2));

        var total = service.calcularIngresosTotales(ini, fin);
        assertEquals(new BigDecimal("66000.00"), total);
    }

    // ============== Tests de seguridad (ADMIN requerido) ==============

    @Test
    void generarReporteVentasPorDia_noAdmin_lanzaAutenticacion() {
        // Usuario normal
        SessionManager.getInstance().cerrarSesion();
        Usuario u = new Usuario();
        u.setRol(Rol.USUARIO);
        SessionManager.getInstance().setUsuarioActual(u);

        assertThrows(AutenticacionException.class,
                () -> service.generarReporteVentasPorDia(LocalDate.now()));

        verifyNoInteractions(compraDAO, entradaDAO, compraConfiteriaDAO, peliculaDAO, funcionDAO);
    }

    @Test
    void generarReporteVentasPorDia_sinSesion_lanzaAutenticacion() {
        SessionManager.getInstance().cerrarSesion();

        assertThrows(AutenticacionException.class,
                () -> service.generarReporteVentasPorDia(LocalDate.now()));

        verifyNoInteractions(compraDAO, entradaDAO, compraConfiteriaDAO, peliculaDAO, funcionDAO);
    }
}
