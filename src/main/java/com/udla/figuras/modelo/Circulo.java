package com.udla.figuras.modelo;

public class Circulo extends FiguraGeometrica {

    private final double radio;

    public Circulo(double radio) {
        super("Circulo");
        validarPositivo(radio, "radio");
        this.radio = radio;
    }

    @Override
    public double calcularArea() {
        return Math.PI * radio * radio;
    }

    @Override
    public double calcularPerimetro() {
        return 2 * Math.PI * radio;
    }
}
