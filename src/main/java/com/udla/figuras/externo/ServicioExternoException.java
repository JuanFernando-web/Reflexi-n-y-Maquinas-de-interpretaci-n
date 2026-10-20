package com.udla.figuras.externo;

/** El servicio externo no responde o devolvio algo ilegible. */
public class ServicioExternoException extends RuntimeException {
    public ServicioExternoException(String mensaje) {
        super(mensaje);
    }
}
