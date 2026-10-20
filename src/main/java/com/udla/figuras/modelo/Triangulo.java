package com.udla.figuras.modelo;

public class Triangulo extends FiguraGeometrica {

    private final double lado1;
    private final double lado2;
    private final double lado3;

    public Triangulo(double lado1, double lado2, double lado3) {
        super("Triangulo");
        validarPositivo(lado1, "lado1");
        validarPositivo(lado2, "lado2");
        validarPositivo(lado3, "lado3");
        if (lado1 + lado2 <= lado3 || lado1 + lado3 <= lado2 || lado2 + lado3 <= lado1) {
            throw new IllegalArgumentException("Los lados no forman un triangulo (desigualdad triangular)");
        }
        this.lado1 = lado1;
        this.lado2 = lado2;
        this.lado3 = lado3;
    }

    @Override
    public double calcularPerimetro() {
        return lado1 + lado2 + lado3;
    }

    /** Formula de Heron. */
    @Override
    public double calcularArea() {
        double s = calcularPerimetro() / 2;
        return Math.sqrt(s * (s - lado1) * (s - lado2) * (s - lado3));
    }
}
