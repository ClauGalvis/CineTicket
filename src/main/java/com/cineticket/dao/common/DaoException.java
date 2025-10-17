package com.cineticket.dao.common;

/**
 * Excepción base para errores en la capa DAO.
 * Envuelve SQLException y permite mensajes controlados.
 *
 * @author Claudia
 * @version 1.0
 */
public class DaoException extends RuntimeException {
    public DaoException(String message, Throwable cause) {
        super(message, cause);
    }

    public DaoException(String message) {
        super(message);
    }
}
