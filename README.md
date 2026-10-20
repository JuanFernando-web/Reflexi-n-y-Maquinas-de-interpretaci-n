# Figuras geométricas con reflexión y Groovy (Spring Boot)

ISWZ2202 - Diseño y Arquitectura de Software · Actividad: *Reflexión e investigación de código en tiempo de ejecución* (Parte 2)

Aplicación Java 17 + Spring Boot 3 construida sobre el diseño de la Parte 1 (interfaz `Figura`, clase abstracta `FiguraGeometrica`, y las clases `Circulo`, `Rectangulo`, `Triangulo`).

| Requisito de la actividad | Dónde está |
| --- | --- |
| Instanciar cualquier figura a partir del nombre de la clase y parámetros en tiempo de ejecución | `nucleo/FabricaFiguras` (reflexión: `getConstructors`, `Parameter`, `newInstance`) |
| Área y perímetro calculados dinámicamente | `FabricaFiguras.crearConResultado` llama `calcularArea()` y `calcularPerimetro()` del objeto creado |
| Solo las figuras básicas son nativas; el resto se carga y compila en tiempo de ejecución | `nucleo/CargadorGroovy` (`GroovyClassLoader`) |
| Una clase Groovy (`Pentagono`) funciona como una nativa | `ejemplos/Pentagono.groovy` y el registro común `nucleo/RegistroFiguras` |
| Extra: servicio externo que entrega figuras nuevas | `externo/ClienteServicioExterno` + `SincronizadorExterno` (con un servicio simulado incluido) |

## Cómo ejecutarla

Requisitos: JDK 17 o superior y Maven 3.9+.

```bash
mvn spring-boot:run          # arranca en http://localhost:8080
mvn test                     # pruebas unitarias y de integración
mvn package && java -jar target/figuras-app-1.0.0.jar
```

Abre <http://localhost:8080> para usar la interfaz web (crear figuras, pegar código Groovy, sincronizar el servicio externo), o usa los `curl` de abajo.

## Uso de la API

**Crear una figura por nombre de clase** (el ejemplo del enunciado: `("Circulo", radio=5)`):

```bash
curl -X POST localhost:8080/api/figuras -H "Content-Type: application/json" \
     -d '{"clase":"Circulo","parametros":{"radio":5}}'
```
```json
{"nombre":"Circulo","clase":"com.udla.figuras.modelo.Circulo","origen":"NATIVA",
 "parametros":{"radio":5},"area":78.5398,"perimetro":31.4159,
 "descripcion":"Circulo -> area: 78.54, perimetro: 31.42"}
```

Atajo para el navegador: `GET /api/figuras/crear/Triangulo?lado1=3&lado2=4&lado3=5`.
Los parámetros se enlazan por **nombre** (sin importar mayúsculas ni el orden) y los nombres de clase ignoran tildes y mayúsculas (`"Círculo"` = `"circulo"`).

**Ver qué figuras existen y cómo se construyen** (descubierto por reflexión):

```bash
curl localhost:8080/api/figuras/tipos
```

**Cargar `Pentagono` en Groovy sin reiniciar** y usarlo como una figura nativa:

```bash
curl -i localhost:8080/api/figuras/crear/Pentagono?lado=2        # 404: todavía no existe

curl -X POST localhost:8080/api/figuras/groovy \
     -H "Content-Type: text/plain; charset=utf-8" --data-binary @ejemplos/Pentagono.groovy

curl -X POST localhost:8080/api/figuras -H "Content-Type: application/json" \
     -d '{"clase":"Pentagono","parametros":{"lado":2}}'           # area 6.8819, perimetro 10.0
```

Volver a enviar el código reemplaza la figura Groovy anterior (cada carga usa su propio `GroovyClassLoader`).
Las figuras nativas no se pueden reemplazar (responde `409`).

**Cargar una carpeta de figuras al arrancar:** copia los `.groovy` a `figuras-groovy/` (propiedad `figuras.groovy.directorio`); se compilan al iniciar y con `POST /api/figuras/groovy/recargar`.

**Extra - servicio externo:**

```bash
curl -X POST localhost:8080/api/figuras/externo/sincronizar
curl "localhost:8080/api/figuras/crear/Hexagono?lado=1"           # ya existe: vino del servicio externo
```

El cliente espera un `GET` que devuelva `[{"nombre":"Hexagono","codigo":"class Hexagono extends FiguraGeometrica {...}"}]`.
Se configura con `figuras.externo.url` en `application.properties`. Si no se define, usa el servicio simulado de esta misma aplicación (`/mock-externo/figuras`, que publica `Hexagono` y `Rombo`), de modo que la demostración funciona sin internet.

### Códigos de respuesta

| Código | Cuándo |
| --- | --- |
| 201 / 200 | Figura creada / consulta correcta |
| 400 | Parámetros faltantes, mal escritos o inválidos (radio negativo, lados que no forman un triángulo) |
| 404 | La clase pedida no existe |
| 409 | Se intentó reemplazar una figura nativa con Groovy |
| 422 | El código Groovy no compila, no define una `Figura` o usa algo prohibido |
| 502 | El servicio externo no respondió |

## Cómo funciona

1. **Registro** (`RegistroFiguras`): catálogo `nombre normalizado -> clase`. Las nativas se descubren al arrancar escaneando el paquete `modelo` en busca de clases concretas que implementen `Figura` (`ConfiguracionFiguras`); agregar una figura nativa no requiere tocar la fábrica.
2. **Fábrica** (`FabricaFiguras`): busca la clase por nombre, elige el constructor cuyo número y nombres de parámetros coinciden con los recibidos, convierte los valores (`5`, `"5"`, `5.0` → `double`) y llama `newInstance`. No hay `switch` por tipo de figura, por eso funciona igual con clases nativas y de Groovy. Los nombres de los parámetros se conservan con la opción `-parameters` de Java (`pom.xml`) y `CompilerConfiguration.setParameters(true)` de Groovy.
3. **Cargador Groovy** (`CargadorGroovy`): compila el texto con `GroovyClassLoader` (cuyo padre es el cargador de la aplicación, por eso el código Groovy puede extender `FiguraGeometrica`), registra cada clase concreta que implemente `Figura` y la deja disponible para la fábrica.
4. **Polimorfismo**: la fábrica y el controlador solo conocen la interfaz `Figura`; el área y el perímetro los calcula cada clase.

## Seguridad (importante)

Cargar código en tiempo de ejecución significa ejecutarlo dentro de la JVM de la aplicación. `VigilanteGroovy` revisa el árbol sintáctico y rechaza (422) usos obviamente peligrosos: `System`, `Runtime`, procesos, hilos, archivos, red, reflexión, `execute()`/`evaluate()` y llamadas con nombre calculado, tanto en constructores como en métodos, campos y bloques `static`. **Es una lista negra de buena fe, no un sandbox**: solo cargue código de fuentes en las que confíe (y proteja `/api/figuras/groovy` y la URL del servicio externo si la aplicación se expone fuera de una red local).

## Estructura

```
src/main/java/com/udla/figuras
├── modelo/    Figura, FiguraGeometrica, Circulo, Rectangulo, Triangulo   (diseño de la Parte 1)
├── nucleo/    RegistroFiguras, FabricaFiguras, CargadorGroovy, VigilanteGroovy, excepciones
├── web/       FiguraController, ManejadorErrores, CrearFiguraRequest
├── externo/   ClienteServicioExterno, SincronizadorExterno, ServicioExternoSimulado
├── ConfiguracionFiguras.java, CargaInicial.java, FigurasApplication.java
ejemplos/      Pentagono.groovy, Hexagono.groovy, Rombo.groovy
figuras-groovy/  carpeta que se compila al arrancar
```

`nucleo` y `modelo` no dependen de Spring (solo de Groovy), así que se pueden probar con JUnit sin levantar el contenedor (`NucleoFigurasTest`).
