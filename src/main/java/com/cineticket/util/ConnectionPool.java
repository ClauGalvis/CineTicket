package com.cineticket.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Gestiona el pool de conexiones JDBC usando HikariCP.
 * Centraliza la configuración obtenida desde ConfiguracionApp.
 *
 * @author Claudia Patricia Galvis Jimenez
 * @version 1.0
 */
public final class ConnectionPool {

    private static final Logger log = LoggerFactory.getLogger(ConnectionPool.class);
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(ConfiguracionApp.getDbUrl());
        config.setUsername(ConfiguracionApp.getDbUsername());
        config.setPassword(ConfiguracionApp.getDbPassword());
        config.setMinimumIdle(ConfiguracionApp.getDbPoolSizeMin());
        config.setMaximumPoolSize(ConfiguracionApp.getDbPoolSizeMax());
        config.setConnectionTimeout(ConfiguracionApp.getDbConnectionTimeout());
        config.setIdleTimeout(ConfiguracionApp.getDbIdleTimeout());
        config.setMaxLifetime(ConfiguracionApp.getDbMaxLifetime());
        config.setPoolName(ConfiguracionApp.getDbPoolName());

        dataSource = new HikariDataSource(config);
        log.info("Pool de conexiones HikariCP inicializado correctamente: {}", config.getPoolName());
    }

    private ConnectionPool() {
        // Evita instanciación
    }

    /**
     * Retorna una conexion activa del pool.
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Cierra todas las conexiones del pool.
     */
    public static void close() {
        if (dataSource != null) {
            dataSource.close();
            log.info("Pool de conexiones cerrado correctamente.");
        }
    }
}
