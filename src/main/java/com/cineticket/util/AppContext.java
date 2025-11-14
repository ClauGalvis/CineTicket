package com.cineticket.util;

import com.cineticket.dao.*;
import com.cineticket.dao.impl.*;
import com.cineticket.servicio.*;
import com.cineticket.servicio.impl.*;

/** Proveedor simple de servicios para la UI. */
public final class AppContext {

    // --- Auth / Usuario
    private static final UsuarioDAO usuarioDAO = new UsuarioDAOImpl();
    private static final AuthService authService = new AuthService(usuarioDAO);

    // --- Cartelera (películas / funciones)
    private static final PeliculaDAO peliculaDAO = new PeliculaDAOImpl();
    private static final FuncionDAO funcionDAO  = new FuncionDAOImpl();
    private static final GeneroDAO generoDAO    = new GeneroDAOImpl();   // ← nuevo
    private static final CarteleraService carteleraService =
            new CarteleraService(peliculaDAO, funcionDAO, generoDAO);   // ← pasa el tercero


    // --- Asientos / Reserva
    private static final AsientoDAO asientoDAO = new AsientoDAOImpl();
    private static final EntradaDAO entradaDAO = new EntradaDAOImpl(); // tu implementación JDBC
    private static final ReservaService reservaService = new ReservaService(entradaDAO, funcionDAO);

    // --- Confitería
    private static final ComboConfiteriaDAO comboDAO = new ComboConfiteriaDAOImpl();
    private static final ConfiteriaService confiteriaService = new ConfiteriaService(comboDAO);

    // --- Compra (incluye PDF)
    private static final CompraDAO compraDAO = new CompraDAOImpl();
    private static final CompraConfiteriaDAO compraConfiteriaDAO = new CompraConfiteriaDAOImpl();
    private static final PDFService pdfService = new PDFServicePDFBox();
    private static final CompraService compraService =
            new CompraService(compraDAO, entradaDAO, compraConfiteriaDAO,
                    funcionDAO, reservaService, confiteriaService, pdfService);

    // --- Reportes
    private static final ReporteService reporteService =
            new ReporteService(compraDAO, entradaDAO, compraConfiteriaDAO,
                    peliculaDAO, funcionDAO);


    private AppContext() {}

    // --- Getters expuestos a la UI ---
    public static AuthService getAuthService() { return authService; }
    public static CarteleraService getCarteleraService() { return carteleraService; }

    public static AsientoDAO getAsientoDAO() { return asientoDAO; }
    public static ReservaService getReservaService() { return reservaService; }

    public static ConfiteriaService getConfiteriaService() { return confiteriaService; }

    public static CompraService getCompraService() { return compraService; }

    public static ReporteService getReporteService() { return reporteService; }

    public static PDFService getPDFService() { return pdfService; }


    // (opcionales, por si alguno los necesita)
    public static FuncionDAO getFuncionDAO() { return funcionDAO; }
    public static EntradaDAO getEntradaDAO() { return entradaDAO; }
    public static ComboConfiteriaDAO getComboDAO() { return comboDAO; }
}
