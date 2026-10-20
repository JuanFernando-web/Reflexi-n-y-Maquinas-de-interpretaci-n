package com.udla.figuras.nucleo;

import com.udla.figuras.modelo.Figura;
import com.udla.figuras.nucleo.InfoFiguras.ResultadoCarga;
import com.udla.figuras.nucleo.RegistroFiguras.Entrada;
import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Compila codigo Groovy en tiempo de ejecucion y registra las clases que implementan Figura.
 * Despues de cargadas, la fabrica las trata exactamente igual que a las nativas.
 *
 * Cada carga usa su propio GroovyClassLoader, asi se puede volver a cargar (reemplazar)
 * una figura Groovy sin reiniciar la aplicacion.
 *
 * Seguridad: el codigo se ejecuta en la JVM de la aplicacion. {@link VigilanteGroovy} aplica una
 * lista negra basica (System, Runtime, procesos, archivos, red, reflexion, execute()...) para
 * evitar errores obvios, pero NO es un sandbox: solo se debe cargar codigo de confianza.
 */
public class CargadorGroovy {

    private final RegistroFiguras registro;
    private final ClassLoader padre;

    public CargadorGroovy(RegistroFiguras registro) {
        this(registro, Figura.class.getClassLoader());
    }

    public CargadorGroovy(RegistroFiguras registro, ClassLoader padre) {
        this.registro = registro;
        this.padre = padre;
    }

    /** Compila el codigo y registra cada clase concreta que implemente Figura. */
    public List<Entrada> cargar(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new CodigoGroovyInvalidoException("El codigo Groovy esta vacio");
        }
        GroovyClassLoader cargador = new GroovyClassLoader(padre, configuracion());
        try {
            cargador.parseClass(codigo);
        } catch (CompilationFailedException | SecurityException ex) {
            throw new CodigoGroovyInvalidoException("El codigo Groovy fue rechazado: "
                    + limpiar(ex.getMessage()));
        }

        List<Class<?>> figuras = new ArrayList<>();
        for (Class<?> clase : cargador.getLoadedClasses()) {
            if (Figura.class.isAssignableFrom(clase)
                    && !clase.isInterface()
                    && !Modifier.isAbstract(clase.getModifiers())
                    && Modifier.isPublic(clase.getModifiers())
                    && clase.getEnclosingClass() == null) {
                figuras.add(clase);
            }
        }
        if (figuras.isEmpty()) {
            throw new CodigoGroovyInvalidoException(
                    "El codigo compila pero no define ninguna clase concreta que implemente Figura "
                            + "(extienda FiguraGeometrica o implemente Figura)");
        }

        List<Entrada> registradas = new ArrayList<>();
        for (Class<?> clase : figuras) {
            registradas.add(registro.registrarGroovy(clase.asSubclass(Figura.class)));
        }
        return registradas;
    }

    /** Carga todos los .groovy de una carpeta. Un archivo con error no impide cargar los demas. */
    public ResultadoCarga cargarDirectorio(Path directorio) {
        List<String> cargadas = new ArrayList<>();
        Map<String, String> errores = new LinkedHashMap<>();
        if (directorio == null || !Files.isDirectory(directorio)) {
            return new ResultadoCarga(cargadas, errores);
        }
        List<Path> archivos;
        try (Stream<Path> stream = Files.list(directorio)) {
            archivos = stream.filter(p -> p.getFileName().toString().endsWith(".groovy"))
                    .sorted()
                    .toList();
        } catch (IOException ex) {
            errores.put(directorio.toString(), ex.getMessage());
            return new ResultadoCarga(cargadas, errores);
        }
        for (Path archivo : archivos) {
            try {
                cargar(Files.readString(archivo)).forEach(e -> cargadas.add(e.nombre()));
            } catch (IOException | RuntimeException ex) {
                errores.put(archivo.getFileName().toString(), ex.getMessage());
            }
        }
        return new ResultadoCarga(cargadas, errores);
    }

    // ---------------------------------------------------------------- internos

    private CompilerConfiguration configuracion() {
        CompilerConfiguration config = new CompilerConfiguration();
        config.setParameters(true); // conserva los nombres de los parametros para la reflexion
        config.addCompilationCustomizers(new VigilanteGroovy());
        return config;
    }

    private static String limpiar(String mensaje) {
        if (mensaje == null) {
            return "(sin detalle)";
        }
        String[] lineas = mensaje.strip().split("\\R");
        return lineas.length > 6 ? String.join(" | ", List.of(lineas).subList(0, 6)) + " ..." : String.join(" | ", lineas);
    }
}
