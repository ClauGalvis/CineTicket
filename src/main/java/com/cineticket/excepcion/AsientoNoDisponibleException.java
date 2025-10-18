package com.cineticket.excepcion;

/**
 * Se lanza cuando un asiento seleccionado ya está ocupado o bloqueado.
 */
public class AsientoNoDisponibleException extends CineTicketException {

    public AsientoNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    public AsientoNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
