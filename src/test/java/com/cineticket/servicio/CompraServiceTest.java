package com.cineticket.servicio;

import com.cineticket.dao.CompraConfiteriaDAO;
import com.cineticket.dao.CompraDAO;
import com.cineticket.dao.EntradaDAO;
import com.cineticket.dao.FuncionDAO;
import com.cineticket.enums.EstadoCompra;
import com.cineticket.enums.EstadoEntrada;
import com.cineticket.enums.MetodoPago;
import com.cineticket.excepcion.*;
import com.cineticket.modelo.Compra;
import com.cineticket.modelo.CompraConfiteria;
import com.cineticket.modelo.Entrada;
import com.cineticket.modelo.Funcion;
import com.cineticket.modelo.ComboConfiteria;
import com.cineticket.servicio.dto.CompraPreparada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompraServiceTest {

    @Mock CompraDAO compraDAO;
    @Mock EntradaDAO entradaDAO;
    @Mock CompraConfiteriaDAO compraConfiteriaDAO;
    @Mock FuncionDAO funcionDAO;
    @Mock ReservaService reservaService;
    @Mock ConfiteriaService confiteriaService;

    // Usamos una implementación dummy directa en lugar de @Mock para evitar NPE en el constructor
    PDFService pdfService;

    CompraService service;

    @BeforeEach
    void setUp() {
        pdfService = new PDFService() {
            @Override
            public String generarComprobantePDF(
                    Compra compra,
                    List<Entrada> entradas,
                    List<CompraConfiteria> items,
                    Map<String, Object> extras) {
                // no escribe nada; solo devuelve una ruta “válida”
                return "tmp/comprobante_" + (compra.getIdCompra() != null ? compra.getIdCompra() : "prep") + ".pdf";
            }

            @Override
            public boolean guardarComprobante(Compra compra, String rutaDestino) {
                return false;
            }
        };

        service = new CompraService(
                compraDAO, entradaDAO, compraConfiteriaDAO,
                funcionDAO, reservaService, confiteriaService, pdfService
        );
    }

    @Test
    void crearCompra_calculaTotales_ok() {
        // --- Arrange ---
        int usuarioId = 11;
        int funcionId = 2;

        // La función existe (el precio no lo usa CompraService directamente, pero dejamos algo coherente)
        Funcion f = new Funcion();
        f.setIdFuncion(funcionId);
        f.setPrecioEntrada(18000.0);
        when(funcionDAO.buscarPorId(funcionId)).thenReturn(f);

        // ReservaService nos devuelve 2 pre-entradas (con precio ya cargado)
        Entrada e1 = new Entrada(); e1.setFuncionId(funcionId); e1.setAsientoId(7); e1.setPrecioUnitario(new BigDecimal("18000"));
        Entrada e2 = new Entrada(); e2.setFuncionId(funcionId); e2.setAsientoId(8); e2.setPrecioUnitario(new BigDecimal("18000"));
        when(reservaService.reservarAsientos(funcionId, List.of(7, 8))).thenReturn(List.of(e1, e2));

        // Confitería: combo 10 x 2 unidades a $25.000 c/u ⇒ subtotal 50.000
        ComboConfiteria combo = new ComboConfiteria();
        combo.setIdCombo(10);
        combo.setPrecio(new BigDecimal("25000"));
        when(confiteriaService.obtenerCombo(10)).thenReturn(combo);
        when(confiteriaService.calcularSubtotal(10, 2)).thenReturn(new BigDecimal("50000"));

        // --- Act ---
        CompraPreparada prep = service.crearCompra(
                usuarioId,
                funcionId,
                List.of(7, 8),
                Map.of(10, 2),
                MetodoPago.TRANSFERENCIA
        );

        // --- Assert ---
        assertNotNull(prep);
        assertNotNull(prep.getCompra());
        assertEquals(2, prep.getEntradas().size());
        assertEquals(1, prep.getItemsConfiteria().size());

        // Totales
        assertEquals(new BigDecimal("36000"), prep.getCompra().getTotalEntradas());   // 18.000 * 2
        assertEquals(new BigDecimal("50000"), prep.getCompra().getTotalConfiteria()); // 25.000 * 2

        // Estado/metadatos de la compra creada
        assertEquals(EstadoCompra.CONFIRMADA, prep.getCompra().getEstadoCompra());
        assertNotNull(prep.getCompra().getFechaHoraCompra());
    }

    @Test
    void calcularTotales_sumaBien() {
        // Entradas: 3 * 10.000 = 30.000
        Entrada e1 = new Entrada(); e1.setPrecioUnitario(new BigDecimal("10000"));
        Entrada e2 = new Entrada(); e2.setPrecioUnitario(new BigDecimal("10000"));
        Entrada e3 = new Entrada(); e3.setPrecioUnitario(new BigDecimal("10000"));

        // Confitería: (2 * 8.000) + (1 * 12.000) = 28.000
        CompraConfiteria c1 = new CompraConfiteria(); c1.setCantidad(2); c1.setPrecioUnitario(new BigDecimal("8000"));  c1.recalcularSubtotal();
        CompraConfiteria c2 = new CompraConfiteria(); c2.setCantidad(1); c2.setPrecioUnitario(new BigDecimal("12000")); c2.recalcularSubtotal();

        Compra compra = new Compra();
        compra.setUsuarioId(99);
        compra.setMetodoPago(MetodoPago.PSE);
        compra.setFechaHoraCompra(LocalDateTime.now());
        compra.setEstadoCompra(EstadoCompra.CONFIRMADA);

        // Ejecutamos el método package-private
        service.calcularTotales(compra, List.of(e1, e2, e3), List.of(c1, c2));

        assertEquals(new BigDecimal("30000"), compra.getTotalEntradas());
        assertEquals(new BigDecimal("28000"), compra.getTotalConfiteria());
    }

    @Test
    void confirmarCompra_persisteTodoYGeneraPDF_ok() {
        // Compra preparada mínima
        Compra compra = new Compra();
        compra.setUsuarioId(11);
        compra.setMetodoPago(MetodoPago.TRANSFERENCIA);
        compra.setEstadoCompra(EstadoCompra.CONFIRMADA);
        compra.setFechaHoraCompra(LocalDateTime.now());
        compra.setTotalEntradas(new BigDecimal("36000"));
        compra.setTotalConfiteria(new BigDecimal("50000"));

        Entrada e1 = new Entrada(); e1.setFuncionId(2); e1.setAsientoId(7); e1.setPrecioUnitario(new BigDecimal("18000")); e1.setEstadoEntrada(EstadoEntrada.ACTIVA);
        Entrada e2 = new Entrada(); e2.setFuncionId(2); e2.setAsientoId(8); e2.setPrecioUnitario(new BigDecimal("18000")); e2.setEstadoEntrada(EstadoEntrada.ACTIVA);

        CompraConfiteria item = new CompraConfiteria(); item.setComboId(10); item.setCantidad(2); item.setPrecioUnitario(new BigDecimal("25000")); item.recalcularSubtotal();

        CompraPreparada preparada = new CompraPreparada(compra, List.of(e1, e2), List.of(item));

        // Mocks de persistencia
        when(compraDAO.crear(any(Compra.class))).thenAnswer(inv -> {
            Compra c = inv.getArgument(0);
            c.setIdCompra(123); // simula PK
            return 123;
        });
        when(compraDAO.actualizar(any(Compra.class))).thenReturn(true);
        when(entradaDAO.crear(any(Entrada.class))).thenReturn(1);
        when(compraConfiteriaDAO.crear(any(CompraConfiteria.class))).thenReturn(1);

        // --- Act ---
        Integer id = service.confirmarCompra(preparada);

        // --- Assert ---
        assertEquals(123, id);

        // Verificamos que cada capa fue llamada como esperamos
        verify(compraDAO).crear(compra);
        verify(entradaDAO, times(2)).crear(any(Entrada.class));
        verify(compraConfiteriaDAO).crear(any(CompraConfiteria.class));
        verify(compraDAO).actualizar(compra);

        // La ruta del PDF quedó seteada
        assertNotNull(compra.getRutaComprobantePdf());
        assertTrue(compra.getRutaComprobantePdf().startsWith("tmp/comprobante_"));
    }

    // =============== Tests cancelarCompra ===============

    @Test
    void cancelarCompra_ok_funcionFutura_cancelaCompraYEntradas() {
        // --- Arrange ---
        Integer compraId = 123;

        // Compra CONFIRMADA
        Compra compra = new Compra();
        compra.setIdCompra(compraId);
        compra.setEstadoCompra(EstadoCompra.CONFIRMADA);

        // Entrada asociada a función
        Entrada e = new Entrada();
        e.setCompraId(compraId);
        e.setFuncionId(5);

        // Función en el futuro (se puede cancelar)
        Funcion funcion = new Funcion();
        funcion.setIdFuncion(5);
        funcion.setFechaHoraInicio(LocalDateTime.now().plusHours(2));
        funcion.setFechaHoraFin(LocalDateTime.now().plusHours(4));

        when(compraDAO.buscarPorId(compraId)).thenReturn(compra);
        when(entradaDAO.listarPorCompra(compraId)).thenReturn(List.of(e));
        when(funcionDAO.buscarPorId(5)).thenReturn(funcion);
        when(compraDAO.actualizar(any(Compra.class))).thenReturn(true);
        when(entradaDAO.cancelarEntradasDeCompra(compraId)).thenReturn(true);

        ArgumentCaptor<Compra> compraCaptor = ArgumentCaptor.forClass(Compra.class);

        // --- Act ---
        boolean result = service.cancelarCompra(compraId);

        // --- Assert ---
        assertTrue(result);
        verify(compraDAO).actualizar(compraCaptor.capture());
        verify(entradaDAO).cancelarEntradasDeCompra(compraId);

        Compra actualizada = compraCaptor.getValue();
        assertEquals(EstadoCompra.CANCELADA, actualizada.getEstadoCompra());
        assertNotNull(actualizada.getFechaCancelacion());
    }

    @Test
    void cancelarCompra_yaCancelada_lanzaValidacionException() {
        // --- Arrange ---
        Integer compraId = 200;

        Compra compra = new Compra();
        compra.setIdCompra(compraId);
        compra.setEstadoCompra(EstadoCompra.CANCELADA); // ya cancelada

        Entrada e = new Entrada();
        e.setCompraId(compraId);
        e.setFuncionId(9);

        Funcion funcion = new Funcion();
        funcion.setIdFuncion(9);
        funcion.setFechaHoraInicio(LocalDateTime.now().plusHours(1)); // irrelevante, igual falla por estado

        when(compraDAO.buscarPorId(compraId)).thenReturn(compra);
        when(entradaDAO.listarPorCompra(compraId)).thenReturn(List.of(e));
        when(funcionDAO.buscarPorId(9)).thenReturn(funcion);

        // --- Act & Assert ---
        assertThrows(ValidacionException.class, () -> service.cancelarCompra(compraId));

        verify(compraDAO, never()).actualizar(any());
        verify(entradaDAO, never()).cancelarEntradasDeCompra(anyInt());
    }

    @Test
    void cancelarCompra_funcionYaInicio_lanzaValidacionException() {
        // --- Arrange ---
        Integer compraId = 300;

        Compra compra = new Compra();
        compra.setIdCompra(compraId);
        compra.setEstadoCompra(EstadoCompra.CONFIRMADA);

        Entrada e = new Entrada();
        e.setCompraId(compraId);
        e.setFuncionId(7);

        Funcion funcion = new Funcion();
        funcion.setIdFuncion(7);
        funcion.setFechaHoraInicio(LocalDateTime.now().minusMinutes(10)); // ya inició
        funcion.setFechaHoraFin(LocalDateTime.now().plusHours(1));

        when(compraDAO.buscarPorId(compraId)).thenReturn(compra);
        when(entradaDAO.listarPorCompra(compraId)).thenReturn(List.of(e));
        when(funcionDAO.buscarPorId(7)).thenReturn(funcion);

        // --- Act & Assert ---
        assertThrows(ValidacionException.class, () -> service.cancelarCompra(compraId));

        verify(compraDAO, never()).actualizar(any());
        verify(entradaDAO, never()).cancelarEntradasDeCompra(anyInt());
    }

    @Test
    void cancelarCompra_sinEntradas_actualizaSoloCompra() {
        // --- Arrange ---
        Integer compraId = 400;

        Compra compra = new Compra();
        compra.setIdCompra(compraId);
        compra.setEstadoCompra(EstadoCompra.CONFIRMADA);

        when(compraDAO.buscarPorId(compraId)).thenReturn(compra);
        when(entradaDAO.listarPorCompra(compraId)).thenReturn(List.of()); // sin entradas
        when(compraDAO.actualizar(any(Compra.class))).thenReturn(true);

        ArgumentCaptor<Compra> compraCaptor = ArgumentCaptor.forClass(Compra.class);

        // --- Act ---
        boolean result = service.cancelarCompra(compraId);

        // --- Assert ---
        assertTrue(result);
        verify(compraDAO).actualizar(compraCaptor.capture());
        verify(entradaDAO, never()).cancelarEntradasDeCompra(anyInt());

        Compra actualizada = compraCaptor.getValue();
        assertEquals(EstadoCompra.CANCELADA, actualizada.getEstadoCompra());
        assertNotNull(actualizada.getFechaCancelacion());
    }

    @Test
    void cancelarCompra_compraNoExiste_lanzaValidacionException() {
        // --- Arrange ---
        Integer compraId = 999;
        when(compraDAO.buscarPorId(compraId)).thenReturn(null);

        // --- Act & Assert ---
        assertThrows(ValidacionException.class, () -> service.cancelarCompra(compraId));

        verify(entradaDAO, never()).listarPorCompra(anyInt());
        verify(compraDAO, never()).actualizar(any());
    }

}
