package com.cineticket.servicio;

import com.cineticket.dao.impl.CompraDAOImpl;
import com.cineticket.dao.impl.EntradaDAOImpl;
import com.cineticket.dao.impl.CompraConfiteriaDAOImpl;
import com.cineticket.dao.impl.FuncionDAOImpl;
import com.cineticket.enums.EstadoCompra;
import com.cineticket.enums.EstadoEntrada;
import com.cineticket.enums.MetodoPago;
import com.cineticket.modelo.Compra;
import com.cineticket.modelo.CompraConfiteria;
import com.cineticket.modelo.Entrada;
import com.cineticket.servicio.dto.CompraPreparada;
import com.cineticket.servicio.impl.PDFServicePDFBox;   // ⬅️ ajusta el paquete si difiere

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CompraServiceIT {

    // DAOs REALES
    private final CompraDAOImpl compraDAO = new CompraDAOImpl();
    private final EntradaDAOImpl entradaDAO = new EntradaDAOImpl();
    private final CompraConfiteriaDAOImpl compraConfDAO = new CompraConfiteriaDAOImpl();
    private final FuncionDAOImpl funcionDAO = new FuncionDAOImpl();

    // Servicios “dobles”
    private final ReservaService reservaService = mock(ReservaService.class, withSettings().lenient());
    private final ConfiteriaService confiteriaService = mock(ConfiteriaService.class, withSettings().lenient());

    // PDF REAL (PDFBox)
    private PDFService pdfService;

    private CompraService service;

    @BeforeEach
    void setUp() throws Exception {
        // Carpeta temporal donde se guardarán los PDFs durante el test
        Path outDir = Files.createTempDirectory("pdfs_it_");
        pdfService = new PDFServicePDFBox(outDir.toString()); // ⬅️ ctor que recibe carpeta de salida

        service = new CompraService(
                compraDAO, entradaDAO, compraConfDAO, funcionDAO,
                reservaService, confiteriaService, pdfService
        );
    }

    @Test
    void confirmarCompra_conBDReal_yPDFReal_persiste_yGeneraPDF_ok() throws Exception {
        // ---------- Datos existentes en tu BD (ajusta si cambian) ----------
        final int usuarioId = 11;           // Debe existir en tabla usuario
        final int funcionId = 2;            // PROGRAMADA en tus capturas
        final int asientoLibreId = 182;     // Ese asiento estaba con entrada CANCELADA => hoy disponible
        final int comboId = 10;             // “Combo Grande” disponible
        final BigDecimal precioEntrada = new BigDecimal("18000");
        final BigDecimal precioCombo = new BigDecimal("25000");
        final int cantidadCombo = 2;

        // ---------- Armamos CompraPreparada mínima ----------
        Compra compra = new Compra();
        compra.setUsuarioId(usuarioId);
        compra.setFechaHoraCompra(LocalDateTime.now());
        compra.setMetodoPago(MetodoPago.TRANSFERENCIA);
        compra.setEstadoCompra(EstadoCompra.CONFIRMADA);
        compra.setTotalEntradas(precioEntrada); // 1 entrada
        compra.setTotalConfiteria(precioCombo.multiply(BigDecimal.valueOf(cantidadCombo)));

        Entrada e = new Entrada();
        e.setFuncionId(funcionId);
        e.setAsientoId(asientoLibreId);
        e.setPrecioUnitario(precioEntrada);
        e.setEstadoEntrada(EstadoEntrada.ACTIVA);

        CompraConfiteria item = new CompraConfiteria();
        item.setComboId(comboId);
        item.setCantidad(cantidadCombo);
        item.setPrecioUnitario(precioCombo);
        item.recalcularSubtotal(); // 50.000

        CompraPreparada preparada = new CompraPreparada(
                compra,
                List.of(e),
                List.of(item)
        );

        // ---------- Persistimos y generamos PDF ----------
        Integer compraId = service.confirmarCompra(preparada);
        assertNotNull(compraId, "Debe devolver ID de compra");

        // ---------- Verificaciones en BD ----------
        var compraBD = compraDAO.buscarPorId(compraId);
        assertNotNull(compraBD, "Compra debe existir en BD");

        var entradasBD = entradaDAO.listarPorCompra(compraId);
        assertEquals(1, entradasBD.size(), "Debe persistir 1 entrada");

        var confBD = compraConfDAO.listarPorCompra(compraId);
        assertEquals(1, confBD.size(), "Debe persistir 1 ítem de confitería");

        // ---------- Verificación del PDF ----------
        String rutaPdf = compraBD.getRutaComprobantePdf();
        assertNotNull(rutaPdf, "Ruta de comprobante debe haberse seteado");
        assertTrue(Files.exists(Path.of(rutaPdf)), "El PDF debería existir en disco");

        System.out.println("Compra creada: " + compraId);
        System.out.println("PDF generado: " + rutaPdf);
        // No hacemos cleanup: queda todo en BD y el PDF para inspección manual.
    }
}
