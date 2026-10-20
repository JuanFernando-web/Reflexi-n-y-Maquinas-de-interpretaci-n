package com.udla.figuras;

import com.udla.figuras.modelo.Circulo;
import com.udla.figuras.modelo.Figura;
import com.udla.figuras.modelo.Rectangulo;
import com.udla.figuras.modelo.Triangulo;
import com.udla.figuras.nucleo.CargadorGroovy;
import com.udla.figuras.nucleo.CodigoGroovyInvalidoException;
import com.udla.figuras.nucleo.FabricaFiguras;
import com.udla.figuras.nucleo.FiguraNoEncontradaException;
import com.udla.figuras.nucleo.InfoFiguras.ResultadoCarga;
import com.udla.figuras.nucleo.InfoFiguras.ResultadoFigura;
import com.udla.figuras.nucleo.InfoFiguras.TipoInfo;
import com.udla.figuras.nucleo.NombreReservadoException;
import com.udla.figuras.nucleo.ParametrosInvalidosException;
import com.udla.figuras.nucleo.RegistroFiguras;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** Pruebas del nucleo sin Spring: reflexion (FabricaFiguras) y carga de Groovy (CargadorGroovy). */
class NucleoFigurasTest {

    private static final Path EJEMPLOS = Path.of("ejemplos");

    private RegistroFiguras registro;
    private FabricaFiguras fabrica;
    private CargadorGroovy cargador;

    @BeforeEach
    void preparar() {
        registro = new RegistroFiguras();
        registro.registrarNativa(Circulo.class);
        registro.registrarNativa(Rectangulo.class);
        registro.registrarNativa(Triangulo.class);
        fabrica = new FabricaFiguras(registro);
        cargador = new CargadorGroovy(registro);
    }

    // ------------------------------------------------------------ reflexion

    @Test
    void creaCirculoPorNombreYCalculaAreaYPerimetro() {
        ResultadoFigura r = fabrica.crearConResultado("Circulo", Map.of("radio", 5));
        assertThat(r.area()).isCloseTo(78.5398, within(1e-4));
        assertThat(r.perimetro()).isCloseTo(31.4159, within(1e-4));
        assertThat(r.origen()).isEqualTo("NATIVA");
    }

    @Test
    void ignoraTildesMayusculasYAceptaNumerosComoTexto() {
        Figura f = fabrica.crear("CÍRCULO", Map.of("RADIO", "2.5"));
        assertThat(f.getNombre()).isEqualTo("Circulo");
        assertThat(f.calcularPerimetro()).isCloseTo(2 * Math.PI * 2.5, within(1e-9));
    }

    @Test
    void enlazaPorNombreDeParametroSinImportarElOrden() {
        Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("altura", 3);
        parametros.put("base", 4);
        assertThat(fabrica.crearConResultado("Rectangulo", parametros).area()).isEqualTo(12.0);
    }

    @Test
    void trianguloUsaHeron() {
        ResultadoFigura r = fabrica.crearConResultado("Triangulo", Map.of("lado1", 3, "lado2", 4, "lado3", 5));
        assertThat(r.area()).isEqualTo(6.0);
        assertThat(r.perimetro()).isEqualTo(12.0);
    }

    @Test
    void rechazaDatosInvalidos() {
        assertThatThrownBy(() -> fabrica.crear("Circulo", Map.of("radio", -1)))
                .isInstanceOf(ParametrosInvalidosException.class).hasMessageContaining("radio");
        assertThatThrownBy(() -> fabrica.crear("Triangulo", Map.of("lado1", 1, "lado2", 1, "lado3", 5)))
                .isInstanceOf(ParametrosInvalidosException.class).hasMessageContaining("triangulo");
        assertThatThrownBy(() -> fabrica.crear("Circulo", Map.of("radio", "abc")))
                .isInstanceOf(ParametrosInvalidosException.class);
        assertThatThrownBy(() -> fabrica.crear("Circulo", Map.of("diametro", 3)))
                .isInstanceOf(ParametrosInvalidosException.class).hasMessageContaining("radio");
        assertThatThrownBy(() -> fabrica.crear("Rectangulo", Map.of("base", 2)))
                .isInstanceOf(ParametrosInvalidosException.class).hasMessageContaining("Constructores");
    }

    @Test
    void claseInexistenteONoFiguraNoSeCrea() {
        assertThatThrownBy(() -> fabrica.crear("Dodecaedro", Map.of()))
                .isInstanceOf(FiguraNoEncontradaException.class);
        assertThatThrownBy(() -> fabrica.crear("java.lang.ProcessBuilder", Map.of()))
                .isInstanceOf(FiguraNoEncontradaException.class);
    }

    @Test
    void describeConstructoresConReflexion() {
        TipoInfo info = fabrica.describir(registro.buscar("Triangulo").orElseThrow());
        assertThat(info.constructores()).hasSize(1);
        assertThat(info.constructores().get(0)).extracting("nombre").containsExactly("lado1", "lado2", "lado3");
        assertThat(info.metodos()).containsExactly("calcularArea", "calcularPerimetro", "getNombre");
    }

    // --------------------------------------------------------------- groovy

    @Test
    void pentagonoGroovyFuncionaComoClaseNativa() throws IOException {
        assertThatThrownBy(() -> fabrica.crear("Pentagono", Map.of("lado", 2)))
                .isInstanceOf(FiguraNoEncontradaException.class);

        cargador.cargar(Files.readString(EJEMPLOS.resolve("Pentagono.groovy")));

        ResultadoFigura r = fabrica.crearConResultado("Pentagono", Map.of("lado", 2));
        assertThat(r.origen()).isEqualTo("GROOVY");
        assertThat(r.area()).isCloseTo(6.8819, within(1e-4));
        assertThat(r.perimetro()).isEqualTo(10.0);
        assertThatThrownBy(() -> fabrica.crear("Pentagono", Map.of("lado", 0)))
                .isInstanceOf(ParametrosInvalidosException.class).hasMessageContaining("lado");
    }

    @Test
    void sePuedeReemplazarUnaFiguraGroovySinReiniciar() throws IOException {
        String codigo = Files.readString(EJEMPLOS.resolve("Pentagono.groovy"));
        cargador.cargar(codigo);
        cargador.cargar(codigo.replace("5 * lado", "5 * lado * 2"));
        assertThat(fabrica.crearConResultado("Pentagono", Map.of("lado", 2)).perimetro()).isEqualTo(20.0);
    }

    @Test
    void cargaTodaUnaCarpeta() {
        ResultadoCarga r = cargador.cargarDirectorio(EJEMPLOS);
        assertThat(r.errores()).isEmpty();
        assertThat(r.cargadas()).containsExactlyInAnyOrder("Hexagono", "Pentagono", "Rombo");
        assertThat(fabrica.crearConResultado("Rombo", Map.of("diagonalMayor", 8, "diagonalMenor", 6)).perimetro())
                .isEqualTo(20.0);
    }

    @Test
    void noSePuedeReemplazarUnaFiguraNativa() {
        String codigo = """
                import com.udla.figuras.modelo.FiguraGeometrica
                class Circulo extends FiguraGeometrica {
                    Circulo() { super('x') }
                    double calcularArea() { 1 }
                    double calcularPerimetro() { 1 }
                }
                """;
        assertThatThrownBy(() -> cargador.cargar(codigo)).isInstanceOf(NombreReservadoException.class);
    }

    @Test
    void rechazaCodigoVacioQueNoCompilaOQueNoEsFigura() {
        assertThatThrownBy(() -> cargador.cargar("  ")).isInstanceOf(CodigoGroovyInvalidoException.class);
        assertThatThrownBy(() -> cargador.cargar("class X {{{")).isInstanceOf(CodigoGroovyInvalidoException.class);
        assertThatThrownBy(() -> cargador.cargar("class Perro { }"))
                .isInstanceOf(CodigoGroovyInvalidoException.class).hasMessageContaining("Figura");
    }

    @Test
    void bloqueaCodigoPeligrosoEnConstructorMetodoYBloqueStatic() {
        String[] peligrosos = {
                "System.exit(0)", "Runtime.getRuntime().exec('ls')", "'ls'.execute()",
                "java.lang.System.exit(0)", "new File('/etc/passwd').text", "Class.forName('java.lang.Runtime')",
                "new ProcessBuilder('ls').start()", "Thread.sleep(10)", "new GroovyShell().evaluate('1')"
        };
        String[] plantillas = {
                // el codigo peligroso va en el constructor, en un metodo y en un bloque static
                "class Mala extends com.udla.figuras.modelo.FiguraGeometrica {\n"
                        + "  Mala() { super('Mala'); %s }\n"
                        + "  double calcularArea() { 1 }\n  double calcularPerimetro() { 1 }\n}",
                "class Mala extends com.udla.figuras.modelo.FiguraGeometrica {\n"
                        + "  Mala() { super('Mala') }\n"
                        + "  double calcularArea() { %s; 1 }\n  double calcularPerimetro() { 1 }\n}",
                "class Mala extends com.udla.figuras.modelo.FiguraGeometrica {\n"
                        + "  static { %s }\n  Mala() { super('Mala') }\n"
                        + "  double calcularArea() { 1 }\n  double calcularPerimetro() { 1 }\n}"
        };
        // Control: con un cuerpo inofensivo la misma plantilla SI carga; asi se sabe que
        // los rechazos de abajo se deben al vigilante y no a un error de sintaxis.
        for (String plantilla : plantillas) {
            assertThat(cargador.cargar(plantilla.formatted("int x = 1"))).hasSize(1);
        }
        registro = new RegistroFiguras();
        registro.registrarNativa(Circulo.class);
        cargador = new CargadorGroovy(registro);

        for (String plantilla : plantillas) {
            for (String peligroso : peligrosos) {
                assertThatThrownBy(() -> cargador.cargar(plantilla.formatted(peligroso)))
                        .as(peligroso)
                        .isInstanceOf(CodigoGroovyInvalidoException.class)
                        .hasMessageContaining("Uso prohibido");
            }
        }
        assertThat(registro.buscar("Mala")).isEmpty();
    }

    @Test
    void permiteMatematicasYColeccionesNormales() {
        cargador.cargar("""
                import com.udla.figuras.modelo.FiguraGeometrica
                class Suma extends FiguraGeometrica {
                    Suma() { super('Suma') }
                    double calcularArea() { [1, 2, 3].collect { Math.pow(it, 2) }.sum() }
                    double calcularPerimetro() { Math.sqrt(4) }
                }
                """);
        assertThat(fabrica.crearConResultado("Suma", Map.of()).area()).isEqualTo(14.0);
    }
}
