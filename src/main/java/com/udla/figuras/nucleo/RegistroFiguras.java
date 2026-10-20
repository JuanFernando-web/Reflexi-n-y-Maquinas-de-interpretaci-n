package com.udla.figuras.nucleo;

import com.udla.figuras.modelo.Figura;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Catalogo de las clases de figura disponibles en este momento.
 * Las nativas se registran al arrancar; las de Groovy, cuando se cargan (y pueden reemplazarse).
 */
public class RegistroFiguras {

    public enum Origen { NATIVA, GROOVY }

    public record Entrada(String nombre, Class<? extends Figura> clase, Origen origen) {
    }

    private final Map<String, Entrada> entradas = new ConcurrentHashMap<>();

    public void registrarNativa(Class<? extends Figura> clase) {
        entradas.put(Nombres.normalizar(clase.getSimpleName()),
                new Entrada(clase.getSimpleName(), clase, Origen.NATIVA));
    }

    public Entrada registrarGroovy(Class<? extends Figura> clase) {
        String clave = Nombres.normalizar(clase.getSimpleName());
        Entrada actual = entradas.get(clave);
        if (actual != null && actual.origen() == Origen.NATIVA) {
            throw new NombreReservadoException("'" + clase.getSimpleName()
                    + "' es una figura nativa y no se puede reemplazar con codigo Groovy");
        }
        Entrada nueva = new Entrada(clase.getSimpleName(), clase, Origen.GROOVY);
        entradas.put(clave, nueva);
        return nueva;
    }

    public Optional<Entrada> buscar(String nombre) {
        return Optional.ofNullable(entradas.get(Nombres.normalizar(nombre)));
    }

    public List<Entrada> listar() {
        return entradas.values().stream()
                .sorted(Comparator.comparing(Entrada::nombre))
                .toList();
    }
}
