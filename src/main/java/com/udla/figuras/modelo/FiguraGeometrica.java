package com.udla.figuras.modelo;

/**
 * Clase abstracta base: concentra lo repetido (nombre, validacion y descripcion).
 * Las subclases solo aportan sus atributos y las formulas de area y perimetro.
 */
public abstract class FiguraGeometrica implements Figura {

    private final String nombre;

    protected FiguraGeometrica(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public String getNombre() {
        return nombre;
    }

    protected void validarPositivo(double valor, String campo) {
        if (!(valor > 0) || Double.isInfinite(valor)) {
            throw new IllegalArgumentException(campo + " debe ser un numero mayor que 0");
        }
    }

    public String describir() {
        return String.format(java.util.Locale.US, "%s -> area: %.2f, perimetro: %.2f",
                nombre, calcularArea(), calcularPerimetro());
    }
}
