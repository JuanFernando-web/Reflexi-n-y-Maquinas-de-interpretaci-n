package com.udla.figuras;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** EXTRA: la aplicacion consume el servicio externo (simulado) y usa las figuras recibidas. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ServicioExternoTest {

    @Autowired
    private TestRestTemplate http;

    @Test
    @SuppressWarnings("unchecked")
    void sincronizaFigurasDelServicioExternoYLasUsa() {
        ResponseEntity<Map> sync = http.postForEntity("/api/figuras/externo/sincronizar", null, Map.class);
        assertThat(sync.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<String>) sync.getBody().get("cargadas")).containsExactlyInAnyOrder("Hexagono", "Rombo");
        assertThat((Map<String, String>) sync.getBody().get("errores")).isEmpty();

        ResponseEntity<Map> hexagono = http.getForEntity("/api/figuras/crear/Hexagono?lado=1", Map.class);
        assertThat(hexagono.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) hexagono.getBody().get("area")).doubleValue()).isEqualTo(2.5981);
        assertThat(((Number) hexagono.getBody().get("perimetro")).doubleValue()).isEqualTo(6.0);
        assertThat(hexagono.getBody().get("origen")).isEqualTo("GROOVY");
    }
}
