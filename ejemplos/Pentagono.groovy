import com.udla.figuras.modelo.FiguraGeometrica

/** Pentagono regular de lado "lado". Se carga en tiempo de ejecucion y funciona como una clase nativa. */
class Pentagono extends FiguraGeometrica {

    private final double lado

    Pentagono(double lado) {
        super("Pentagono")
        validarPositivo(lado, "lado")
        this.lado = lado
    }

    @Override
    double calcularArea() {
        return Math.sqrt(5 * (5 + 2 * Math.sqrt(5))) / 4 * lado * lado
    }

    @Override
    double calcularPerimetro() {
        return 5 * lado
    }
}
