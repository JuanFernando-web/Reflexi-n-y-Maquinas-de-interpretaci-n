package com.udla.figuras.externo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Servicio externo SIMULADO para demostrar el extra sin depender de internet.
 * Publica dos figuras escritas en Groovy. En una entrega real se apunta
 * figuras.externo.url a cualquier servicio que devuelva el mismo JSON.
 */
@RestController
@RequestMapping("/mock-externo")
public class ServicioExternoSimulado {

    private static final String HEXAGONO = """
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
""";

    private static final String ROMBO = """
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
""";

    @GetMapping("/figuras")
    public List<FiguraExterna> figuras() {
        return List.of(
                new FiguraExterna("Hexagono", HEXAGONO),
                new FiguraExterna("Rombo", ROMBO));
    }
}
