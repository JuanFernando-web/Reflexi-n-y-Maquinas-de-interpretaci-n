package com.udla.figuras.modelo;

public class Rectangulo extends FiguraGeometrica {

    private final double base;
    private final double altura;

    public Rectangulo(double base, double altura) {
        super("Rectangulo");
        validarPositivo(base, "base");
        validarPositivo(altura, "altura");
        this.base = base;
        this.altura = altura;
    }

    @Override
    public double calcularArea() {
        return base * altura;
    }

    @Override
    public double calcularPerimetro() {
        return 2 * (base + altura);
    }
}
