package com.udla.figuras.externo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;

/**
 * EXTRA: consume un servicio HTTP externo que publica figuras nuevas.
 *
 * Contrato esperado (GET, JSON):
 *   [ {"nombre": "Hexagono", "codigo": "class Hexagono extends FiguraGeometrica { ... }"}, ... ]
 *
 * La URL se configura con figuras.externo.url. Si no se define, se usa el servicio simulado que trae
 * esta misma aplicacion (/mock-externo/figuras) para poder demostrar el flujo sin internet.
 */
@Component
public class ClienteServicioExterno {

    private final RestClient cliente;
    private final String urlConfigurada;
    private final Environment entorno;

    public ClienteServicioExterno(@Value("${figuras.externo.url:}") String urlConfigurada, Environment entorno) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(5));
        fabrica.setReadTimeout(Duration.ofSeconds(10));
        this.cliente = RestClient.builder().requestFactory(fabrica).build();
        this.urlConfigurada = urlConfigurada;
        this.entorno = entorno;
    }

    public String urlActual() {
        if (urlConfigurada != null && !urlConfigurada.isBlank()) {
            return urlConfigurada;
        }
        // local.server.port existe cuando el servidor web ya arranco (tambien con puerto aleatorio en pruebas)
        String puerto = entorno.getProperty("local.server.port", entorno.getProperty("server.port", "8080"));
        return "http://localhost:" + puerto + "/mock-externo/figuras";
    }

    public List<FiguraExterna> obtenerFiguras() {
        String url = urlActual();
        try {
            List<FiguraExterna> figuras = cliente.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<FiguraExterna>>() {
                    });
            return figuras == null ? List.of() : figuras;
        } catch (RestClientException ex) {
            throw new ServicioExternoException("No se pudo consultar el servicio externo " + url
                    + ": " + ex.getMessage());
        }
    }
}
