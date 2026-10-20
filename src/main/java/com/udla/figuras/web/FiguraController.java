package com.udla.figuras.web;

import com.udla.figuras.externo.SincronizadorExterno;
import com.udla.figuras.nucleo.CargadorGroovy;
import com.udla.figuras.nucleo.FabricaFiguras;
import com.udla.figuras.nucleo.InfoFiguras.ResultadoCarga;
import com.udla.figuras.nucleo.InfoFiguras.ResultadoFigura;
import com.udla.figuras.nucleo.InfoFiguras.TipoInfo;
import com.udla.figuras.nucleo.ParametrosInvalidosException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/figuras")
public class FiguraController {

    private final FabricaFiguras fabrica;
    private final CargadorGroovy cargador;
    private final SincronizadorExterno sincronizador;
    private final String directorioGroovy;

    public FiguraController(FabricaFiguras fabrica,
                            CargadorGroovy cargador,
                            SincronizadorExterno sincronizador,
                            @Value("${figuras.groovy.directorio:figuras-groovy}") String directorioGroovy) {
        this.fabrica = fabrica;
        this.cargador = cargador;
        this.sincronizador = sincronizador;
        this.directorioGroovy = directorioGroovy;
    }

    /** Crea una figura a partir del nombre de la clase y los parametros del JSON. */
    @PostMapping
    public ResponseEntity<ResultadoFigura> crear(@RequestBody CrearFiguraRequest peticion) {
        if (peticion == null || peticion.clase() == null || peticion.clase().isBlank()) {
            throw new ParametrosInvalidosException("Falta el campo 'clase'. Ejemplo: "
                    + "{\"clase\":\"Circulo\",\"parametros\":{\"radio\":5}}");
        }
        ResultadoFigura resultado = fabrica.crearConResultado(peticion.clase(), peticion.parametros());
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }

    /** Version rapida para el navegador: GET /api/figuras/crear/Circulo?radio=5 */
    @GetMapping("/crear/{clase}")
    public ResultadoFigura crearRapido(@PathVariable String clase,
                                       @RequestParam Map<String, String> parametros) {
        return fabrica.crearConResultado(clase, parametros);
    }

    /** Figuras disponibles y como se construyen (descubierto con reflexion). */
    @GetMapping("/tipos")
    public List<TipoInfo> tipos() {
        return fabrica.describirTodos();
    }

    /** Compila y registra una o mas figuras escritas en Groovy (cuerpo de la peticion = codigo fuente). */
    @PostMapping(value = "/groovy", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<List<TipoInfo>> cargarGroovy(@RequestBody String codigo) {
        List<TipoInfo> cargadas = cargador.cargar(codigo).stream().map(fabrica::describir).toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(cargadas);
    }

    /** Vuelve a leer la carpeta de figuras Groovy (figuras.groovy.directorio). */
    @PostMapping("/groovy/recargar")
    public ResultadoCarga recargar() {
        return cargador.cargarDirectorio(Path.of(directorioGroovy));
    }

    /** Extra: consulta el servicio externo y carga las figuras nuevas que ofrece. */
    @PostMapping("/externo/sincronizar")
    public ResultadoCarga sincronizarExterno() {
        return sincronizador.sincronizar();
    }
}
