package com.cineticket.excepcion;

/**
 * Se lanza cuando las credenciales del usuario son inválidas
 * o cuando intenta acceder sin una sesión válida.
 */
public class AutenticacionException extends CineTicketException {

    public AutenticacionException(String mensaje) {
        super(mensaje);
    }

    public AutenticacionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
