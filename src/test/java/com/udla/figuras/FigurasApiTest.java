package com.udla.figuras;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas de la API REST completa (con Spring). */
@SpringBootTest
@AutoConfigureMockMvc
class FigurasApiTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void creaCirculoDesdeJson() throws Exception {
        mvc.perform(post("/api/figuras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clase\":\"Circulo\",\"parametros\":{\"radio\":5}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Circulo"))
                .andExpect(jsonPath("$.origen").value("NATIVA"))
                .andExpect(jsonPath("$.area").value(78.5398))
                .andExpect(jsonPath("$.perimetro").value(31.4159));
    }

    @Test
    void creaTrianguloConGetRapido() throws Exception {
        mvc.perform(get("/api/figuras/crear/Triangulo?lado1=3&lado2=4&lado3=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.area").value(6.0))
                .andExpect(jsonPath("$.perimetro").value(12.0));
    }

    @Test
    void listaLasFigurasNativasConSusConstructores() throws Exception {
        mvc.perform(get("/api/figuras/tipos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", hasItem("Circulo")))
                .andExpect(jsonPath("$[*].nombre", hasItem("Rectangulo")))
                .andExpect(jsonPath("$[*].nombre", hasItem("Triangulo")));
    }

    @Test
    void codigosDeErrorSonLosEsperados() throws Exception {
        mvc.perform(post("/api/figuras").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clase\":\"Dodecaedro\",\"parametros\":{}}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/figuras").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clase\":\"Circulo\",\"parametros\":{\"radio\":-3}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("radio")));
        mvc.perform(post("/api/figuras").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parametros\":{\"radio\":3}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cargaUnaFiguraGroovyEnTiempoDeEjecucionYLaUsaComoNativa() throws Exception {
        String cuadrado = """
                import com.udla.figuras.modelo.FiguraGeometrica
                class Cuadrado extends FiguraGeometrica {
                    private final double lado
                    Cuadrado(double lado) { super('Cuadrado'); validarPositivo(lado, 'lado'); this.lado = lado }
                    double calcularArea() { lado * lado }
                    double calcularPerimetro() { 4 * lado }
                }
                """;
        mvc.perform(post("/api/figuras/groovy").contentType(MediaType.TEXT_PLAIN).content(cuadrado))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].nombre").value("Cuadrado"))
                .andExpect(jsonPath("$[0].origen").value("GROOVY"));

        mvc.perform(post("/api/figuras").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clase\":\"Cuadrado\",\"parametros\":{\"lado\":3}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origen").value("GROOVY"))
                .andExpect(jsonPath("$.area").value(9.0))
                .andExpect(jsonPath("$.perimetro").value(12.0));
    }

    @Test
    void rechazaGroovyPeligrosoYNoReemplazaNativas() throws Exception {
        mvc.perform(post("/api/figuras/groovy").contentType(MediaType.TEXT_PLAIN).content("""
                        import com.udla.figuras.modelo.FiguraGeometrica
                        class Mala extends FiguraGeometrica {
                            Mala() { super('Mala'); System.exit(0) }
                            double calcularArea() { 1 }
                            double calcularPerimetro() { 1 }
                        }
                        """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error", containsString("Uso prohibido")));

        mvc.perform(post("/api/figuras/groovy").contentType(MediaType.TEXT_PLAIN).content("""
                        import com.udla.figuras.modelo.FiguraGeometrica
                        class Circulo extends FiguraGeometrica {
                            Circulo() { super('x') }
                            double calcularArea() { 1 }
                            double calcularPerimetro() { 1 }
                        }
                        """))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/figuras/groovy").contentType(MediaType.TEXT_PLAIN).content("class X {{{"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/figuras/crear/Mala")).andExpect(status().isNotFound());
        mvc.perform(get("/api/figuras/tipos")).andExpect(content().string(containsString("Circulo")));
    }
}
