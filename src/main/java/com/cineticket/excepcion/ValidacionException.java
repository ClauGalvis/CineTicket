package com.cineticket.excepcion;

/**
 * Se lanza cuando fallan validaciones de datos de entrada
 * (campos vacíos, formatos inválidos, reglas de negocio, etc.).
 */
public class ValidacionException extends CineTicketException {

    public ValidacionException(String mensaje) {
        super(mensaje);
    }

    public ValidacionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
