package com.udla.figuras.nucleo;

import java.text.Normalizer;
import java.util.Locale;

/** Normaliza nombres para que "Círculo", "circulo" y "CIRCULO" apunten a la misma clase. */
public final class Nombres {

    private Nombres() {
    }

    public static String normalizar(String nombre) {
        if (nombre == null) {
            return "";
        }
        String simple = nombre.trim();
        int punto = simple.lastIndexOf('.');
        if (punto >= 0) {
            simple = simple.substring(punto + 1);
        }
        return simplificar(simple);
    }

    /** Quita tildes y mayusculas sin tocar el resto del texto (sirve para nombres de parametros). */
    public static String simplificar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
