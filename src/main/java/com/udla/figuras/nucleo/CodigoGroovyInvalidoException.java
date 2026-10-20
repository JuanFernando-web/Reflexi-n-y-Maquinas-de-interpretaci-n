package com.udla.figuras.nucleo;

/** El codigo Groovy no compila, esta prohibido o no define una Figura. */
public class CodigoGroovyInvalidoException extends RuntimeException {
    public CodigoGroovyInvalidoException(String mensaje) {
        super(mensaje);
    }
}
