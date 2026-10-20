package com.udla.figuras;

import com.udla.figuras.nucleo.CargadorGroovy;
import com.udla.figuras.nucleo.InfoFiguras.ResultadoCarga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/** Al arrancar, compila los .groovy que haya en la carpeta configurada (figuras.groovy.directorio). */
@Component
public class CargaInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CargaInicial.class);

    private final CargadorGroovy cargador;
    private final String directorio;

    public CargaInicial(CargadorGroovy cargador,
                        @Value("${figuras.groovy.directorio:figuras-groovy}") String directorio) {
        this.cargador = cargador;
        this.directorio = directorio;
    }

    @Override
    public void run(ApplicationArguments args) {
        ResultadoCarga resultado = cargador.cargarDirectorio(Path.of(directorio));
        log.info("Figuras Groovy cargadas desde '{}': {}", directorio, resultado.cargadas());
        resultado.errores().forEach((archivo, error) ->
                log.warn("No se pudo cargar {}: {}", archivo, error));
    }
}
