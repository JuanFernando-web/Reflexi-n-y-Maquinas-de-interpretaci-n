package com.udla.figuras.web;

import com.udla.figuras.externo.ServicioExternoException;
import com.udla.figuras.nucleo.CodigoGroovyInvalidoException;
import com.udla.figuras.nucleo.FiguraNoEncontradaException;
import com.udla.figuras.nucleo.NombreReservadoException;
import com.udla.figuras.nucleo.ParametrosInvalidosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/** Convierte las excepciones del dominio en respuestas JSON con el codigo HTTP adecuado. */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(FiguraNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> noEncontrada(FiguraNoEncontradaException ex) {
        return respuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ParametrosInvalidosException.class)
    public ResponseEntity<Map<String, Object>> parametros(ParametrosInvalidosException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(CodigoGroovyInvalidoException.class)
    public ResponseEntity<Map<String, Object>> groovyInvalido(CodigoGroovyInvalidoException ex) {
        return respuesta(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(NombreReservadoException.class)
    public ResponseEntity<Map<String, Object>> reservado(NombreReservadoException ex) {
        return respuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ServicioExternoException.class)
    public ResponseEntity<Map<String, Object>> externo(ServicioExternoException ex) {
        return respuesta(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    /** Fallos al construir o calcular una figura (por ejemplo, un error dentro de codigo Groovy). */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> fallo(IllegalStateException ex) {
        log.error("Fallo al crear o calcular una figura", ex);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    private static ResponseEntity<Map<String, Object>> respuesta(HttpStatus estado, String mensaje) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("estado", estado.value());
        cuerpo.put("error", mensaje);
        return ResponseEntity.status(estado).body(cuerpo);
    }
}
