import com.udla.figuras.modelo.FiguraGeometrica

/** Rombo definido por sus dos diagonales. */
class Rombo extends FiguraGeometrica {

    private final double diagonalMayor
    private final double diagonalMenor

    Rombo(double diagonalMayor, double diagonalMenor) {
        super("Rombo")
        validarPositivo(diagonalMayor, "diagonalMayor")
        validarPositivo(diagonalMenor, "diagonalMenor")
        this.diagonalMayor = diagonalMayor
        this.diagonalMenor = diagonalMenor
    }

    @Override
    double calcularArea() {
        return diagonalMayor * diagonalMenor / 2
    }

    @Override
    double calcularPerimetro() {
        double lado = Math.sqrt(Math.pow(diagonalMayor / 2, 2) + Math.pow(diagonalMenor / 2, 2))
        return 4 * lado
    }
}
