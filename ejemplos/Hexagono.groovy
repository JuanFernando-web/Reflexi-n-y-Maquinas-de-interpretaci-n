import com.udla.figuras.modelo.FiguraGeometrica

/** Hexagono regular de lado "lado". */
class Hexagono extends FiguraGeometrica {

    private final double lado

    Hexagono(double lado) {
        super("Hexagono")
        validarPositivo(lado, "lado")
        this.lado = lado
    }

    @Override
    double calcularArea() {
        return 3 * Math.sqrt(3) / 2 * lado * lado
    }

    @Override
    double calcularPerimetro() {
        return 6 * lado
    }
}
