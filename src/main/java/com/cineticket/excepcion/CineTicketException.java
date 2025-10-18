package com.cineticket.excepcion;

/**
 * Excepción base para el sistema CineTicket.
 * Permite manejar errores de negocio o validación de forma controlada.
 */
public class CineTicketException extends RuntimeException {

    public CineTicketException(String mensaje) {
        super(mensaje);
    }

    public CineTicketException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
