package com.cineticket.dao.impl;

import com.cineticket.util.ConnectionPool;

import java.sql.Connection;
import java.sql.SQLException;

/** Clase base con helper para obtener conexiones del pool estático. */
public abstract class BaseDAO {
    protected Connection getConnection() throws SQLException {
        return ConnectionPool.getConnection();
    }
}
