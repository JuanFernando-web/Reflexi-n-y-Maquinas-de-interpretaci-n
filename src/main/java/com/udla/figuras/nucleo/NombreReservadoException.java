package com.udla.figuras.nucleo;

/** Se intento reemplazar una figura nativa con codigo Groovy. */
public class NombreReservadoException extends RuntimeException {
    public NombreReservadoException(String mensaje) {
        super(mensaje);
    }
}
