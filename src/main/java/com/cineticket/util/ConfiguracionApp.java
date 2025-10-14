package com.cineticket.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Clase singleton para gestionar la configuracion de la aplicacion CineTicket.
 * Lee valores desde application.properties y los explone mediante metodos estaticos.
 *
 * @author Claudia Patricia Galvis Jimenez
 * @version 1.0
 */

public final class ConfiguracionApp {
    private static final Logger logger = LoggerFactory.getLogger(ConfiguracionApp.class);
    private static final Properties propiedades = new Properties();
    private static boolean cargado = false;

    // ============================================================================
    // CLAVES DE CONFIGURACION
    // ============================================================================

    private static final String DB_URL = "db.url";
    private static final String DB_USERNAME = "db.username";
    private static final String DB_PASSWORD = "db.password";
    private static final String DB_POOL_MIN = "db.pool.size.min";
    private static final String DB_POOL_MAX = "db.pool.size.max";
    private static final String DB_CONNECTION_TIMEOUT = "db.connection.timeout";
    private static final String DB_IDLE_TIMEOUT = "db.idle.timeout";
    private static final String DB_MAX_LIFETIME = "db.max.lifetime";
    private static final String DB_POOL_NAME = "db.pool.name";

    private static final String BCRYPT_ROUNDS = "bcrypt.rounds";
    private static final String SESSION_TIMEOUT = "session.timeout";

    private static final String PDF_OUTPUT_DIR = "pdf.output.directory";
    private static final String PDF_FILENAME_PREFIX = "pdf.filename.prefix";
    private static final String PDF_INCLUDE_DATE = "pdf.include.date";

    private static final String LOG_LEVEL = "log.level";
    private static final String LOG_FILE_PATH = "log.file.path";

    private static final String APP_NAME = "app.name";
    private static final String APP_VERSION = "app.version";
    private static final String APP_AUTHOR = "app.author";

    private static final String UI_WINDOW_WIDTH = "ui.window.width";
    private static final String UI_WINDOW_HEIGHT = "ui.window.height";
    private static final String UI_WINDOW_RESIZABLE = "ui.window.resizable";
    private static final String UI_WINDOW_MAXIMIZED = "ui.window.maximized";
    private static final String UI_THEME = "ui.theme";

    private static final String BUSINESS_MAX_TICKETS = "business.max.tickets.per.purchase";
    private static final String BUSINESS_CANCEL_TIME = "business.cancel.time.limit";

    // ========================================
    // BLOQUE ESTATICO: CARGA DE CONFIGURACION
    // ========================================
    static {
        cargarPropiedades();
    }

    private ConfiguracionApp() {
        throw new UnsupportedOperationException("Clase de utilidad: no debe instanciarse");
    }

    private static void cargarPropiedades() {
        if (cargado) return;

        try (InputStream input = ConfiguracionApp.class.getClassLoader()
                .getResourceAsStream("application.properties")) {

            if (input == null) {
                logger.error("Archivo application.properties no encontrado en resources/");
                throw new RuntimeException("Archivo de configuración no encontrado");
            }

            propiedades.load(input);
            cargado = true;
            logger.info("Configuración de CineTicket cargada correctamente.");

        } catch (IOException e) {
            logger.error("Error al cargar application.properties", e);
            throw new RuntimeException("Error al leer configuraciones", e);
        }
    }

    // ============================================================================
    // METODOS AUXILIARES PRIVADOS
    // ============================================================================
    private static String obtenerPropiedad(String clave, String valorPorDefecto) {
        String valor = propiedades.getProperty(clave);
        return (valor != null) ? valor.trim() : valorPorDefecto;
    }

    private static int obtenerPropiedadInt(String clave, int valorPorDefecto) {
        try {
            return Integer.parseInt(obtenerPropiedad(clave, String.valueOf(valorPorDefecto)));
        } catch (NumberFormatException e) {
            logger.warn("Valor inválido para {}: usando valor por defecto {}", clave, valorPorDefecto);
            return valorPorDefecto;
        }
    }

    private static long obtenerPropiedadLong(String clave, long valorPorDefecto) {
        try {
            return Long.parseLong(obtenerPropiedad(clave, String.valueOf(valorPorDefecto)));
        } catch (NumberFormatException e) {
            logger.warn("Valor inválido para {}: usando valor por defecto {}", clave, valorPorDefecto);
            return valorPorDefecto;
        }
    }

    private static boolean obtenerPropiedadBoolean(String clave, boolean valorPorDefecto) {
        return Boolean.parseBoolean(obtenerPropiedad(clave, String.valueOf(valorPorDefecto)));
    }

    // ============================================================================
    // CONFIGURACION DE BASE DE DATOS
    // ============================================================================
    public static String getDbUrl() { return obtenerPropiedad(DB_URL, "jdbc:postgresql://localhost:5433/cineticket"); }
    public static String getDbUsername() { return obtenerPropiedad(DB_USERNAME, "postgres"); }
    public static String getDbPassword() { return obtenerPropiedad(DB_PASSWORD, ""); }
    public static int getDbPoolSizeMin() { return obtenerPropiedadInt(DB_POOL_MIN, 5); }
    public static int getDbPoolSizeMax() { return obtenerPropiedadInt(DB_POOL_MAX, 20); }
    public static long getDbConnectionTimeout() { return obtenerPropiedadLong(DB_CONNECTION_TIMEOUT, 30000L); }
    public static long getDbIdleTimeout() { return obtenerPropiedadLong(DB_IDLE_TIMEOUT, 600000L); }
    public static long getDbMaxLifetime() { return obtenerPropiedadLong(DB_MAX_LIFETIME, 1800000L); }
    public static String getDbPoolName() { return obtenerPropiedad(DB_POOL_NAME, "CineTicketPool"); }

    // ============================================================================
    // SEGURIDAD
    // ============================================================================
    public static int getBcryptRounds() { return obtenerPropiedadInt(BCRYPT_ROUNDS, 12); }
    public static int getSessionTimeout() { return obtenerPropiedadInt(SESSION_TIMEOUT, 30); }

    // ============================================================================
    // PDF
    // ============================================================================
    public static String getPdfOutputDirectory() { return obtenerPropiedad(PDF_OUTPUT_DIR, "./comprobantes/"); }
    public static String getPdfFilenamePrefix() { return obtenerPropiedad(PDF_FILENAME_PREFIX, "comprobante_"); }
    public static boolean getPdfIncludeDate() { return obtenerPropiedadBoolean(PDF_INCLUDE_DATE, true); }

    // ============================================================================
    // LOGGING
    // ============================================================================
    public static String getLogLevel() { return obtenerPropiedad(LOG_LEVEL, "INFO"); }
    public static String getLogFilePath() { return obtenerPropiedad(LOG_FILE_PATH, "./logs/cineticket.log"); }

    // ============================================================================
    // DATOS DE APLICACION
    // ============================================================================
    public static String getAppName() { return obtenerPropiedad(APP_NAME, "CineTicket"); }
    public static String getAppVersion() { return obtenerPropiedad(APP_VERSION, "1.0.0"); }
    public static String getAppAuthor() { return obtenerPropiedad(APP_AUTHOR, "Claudia Patricia Galvis"); }

    // ============================================================================
    // INTERFAZ DE USUARIO
    // ============================================================================
    public static int getUiWindowWidth() { return obtenerPropiedadInt(UI_WINDOW_WIDTH, 1200); }
    public static int getUiWindowHeight() { return obtenerPropiedadInt(UI_WINDOW_HEIGHT, 800); }
    public static boolean getUiWindowResizable() { return obtenerPropiedadBoolean(UI_WINDOW_RESIZABLE, true); }
    public static boolean getUiWindowMaximized() { return obtenerPropiedadBoolean(UI_WINDOW_MAXIMIZED, false); }
    public static String getUiTheme() { return obtenerPropiedad(UI_THEME, "light"); }

    // ============================================================================
    // REGLAS DE NEGOCIO
    // ============================================================================
    public static int getBusinessMaxTicketsPerPurchase() { return obtenerPropiedadInt(BUSINESS_MAX_TICKETS, 5); }
    public static int getBusinessCancelTimeLimit() { return obtenerPropiedadInt(BUSINESS_CANCEL_TIME, 30); }

    // ============================================================================
    // UTILIDADES
    // ============================================================================
    public static void recargarPropiedades() {
        cargado = false;
        cargarPropiedades();
        logger.info("Archivo application.properties recargado.");
    }

    public static Properties obtenerTodasLasPropiedades() {
        return new Properties(propiedades);
    }
}

