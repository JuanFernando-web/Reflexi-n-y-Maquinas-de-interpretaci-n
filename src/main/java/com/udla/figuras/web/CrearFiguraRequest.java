package com.udla.figuras.web;

import java.util.Map;

/** Cuerpo de POST /api/figuras. Ejemplo: {"clase":"Circulo","parametros":{"radio":5}} */
public record CrearFiguraRequest(String clase, Map<String, Object> parametros) {
}
