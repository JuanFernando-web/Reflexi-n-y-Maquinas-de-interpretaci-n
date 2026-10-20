package com.udla.figuras.nucleo;

/** Los parametros no corresponden a ningun constructor o el constructor los rechazo. */
public class ParametrosInvalidosException extends RuntimeException {
    public ParametrosInvalidosException(String mensaje) {
        super(mensaje);
    }
}
