package com.udla.figuras.nucleo;

import java.util.List;
import java.util.Map;

/** Objetos de salida (inmutables) que devuelven la fabrica y el cargador. */
public final class InfoFiguras {

    private InfoFiguras() {
    }

    /** Figura ya creada con su area y perimetro calculados en tiempo de ejecucion. */
    public record ResultadoFigura(String nombre,
                                  String clase,
                                  String origen,
                                  Map<String, Object> parametros,
                                  double area,
                                  double perimetro,
                                  String descripcion) {
    }

    public record ParametroInfo(String nombre, String tipo) {
    }

    /** Lo que la reflexion descubre de una clase: constructores y metodos publicos del contrato. */
    public record TipoInfo(String nombre,
                           String clase,
                           String origen,
                           List<List<ParametroInfo>> constructores,
                           List<String> metodos) {
    }

    public record ResultadoCarga(List<String> cargadas, Map<String, String> errores) {
    }
}
