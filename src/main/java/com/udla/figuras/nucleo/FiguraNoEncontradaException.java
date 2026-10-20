package com.udla.figuras.nucleo;

/** La clase pedida no existe en el registro (ni nativa ni cargada en Groovy). */
public class FiguraNoEncontradaException extends RuntimeException {
    public FiguraNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
