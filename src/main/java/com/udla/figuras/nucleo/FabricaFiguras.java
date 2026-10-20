package com.udla.figuras.nucleo;

import com.udla.figuras.modelo.Figura;
import com.udla.figuras.nucleo.InfoFiguras.ParametroInfo;
import com.udla.figuras.nucleo.InfoFiguras.ResultadoFigura;
import com.udla.figuras.nucleo.InfoFiguras.TipoInfo;
import com.udla.figuras.nucleo.RegistroFiguras.Entrada;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Crea figuras a partir del NOMBRE de la clase y de parametros recibidos en tiempo de ejecucion,
 * usando solo reflexion: busca la clase en el registro, elige el constructor cuyos parametros
 * coinciden con los enviados, convierte los valores al tipo pedido y llama a newInstance().
 * No tiene ningun "if/switch" por tipo de figura, asi que sirve igual para las nativas y para
 * las cargadas con Groovy.
 */
public class FabricaFiguras {

    private static final Map<Class<?>, Class<?>> ENVOLTORIOS = Map.of(
            double.class, Double.class,
            float.class, Float.class,
            int.class, Integer.class,
            long.class, Long.class,
            short.class, Short.class,
            byte.class, Byte.class,
            boolean.class, Boolean.class,
            char.class, Character.class);

    private final RegistroFiguras registro;

    public FabricaFiguras(RegistroFiguras registro) {
        this.registro = registro;
    }

    // ---------------------------------------------------------------- creacion

    /** Ejemplo: crear("Circulo", Map.of("radio", 5)). */
    public Figura crear(String nombreClase, Map<String, ?> parametros) {
        Map<String, ?> params = parametros == null ? Map.of() : parametros;
        Entrada entrada = registro.buscar(nombreClase).orElseThrow(() ->
                new FiguraNoEncontradaException("No existe la figura '" + nombreClase
                        + "'. Disponibles: " + registro.listar().stream().map(Entrada::nombre).toList()));

        ParametrosInvalidosException ultimoError = null;
        for (Constructor<?> constructor : entrada.clase().getConstructors()) {
            if (constructor.getParameterCount() != params.size()) {
                continue;
            }
            Object[] argumentos;
            try {
                argumentos = enlazar(constructor, params);
            } catch (ParametrosInvalidosException ex) {
                ultimoError = ex;
                continue;
            }
            return instanciar(entrada, constructor, argumentos);
        }
        if (ultimoError != null) {
            throw ultimoError;
        }
        throw new ParametrosInvalidosException("Ningun constructor de " + entrada.nombre()
                + " recibe los parametros " + params.keySet() + ". Constructores disponibles: "
                + firmas(entrada));
    }

    /** Crea la figura y calcula area y perimetro llamando a sus metodos. */
    public ResultadoFigura crearConResultado(String nombreClase, Map<String, ?> parametros) {
        Figura figura = crear(nombreClase, parametros);
        Entrada entrada = registro.buscar(nombreClase).orElseThrow();
        double area;
        double perimetro;
        try {
            area = figura.calcularArea();
            perimetro = figura.calcularPerimetro();
        } catch (RuntimeException ex) {
            throw new IllegalStateException("La figura " + entrada.nombre()
                    + " fallo al calcular: " + ex.getMessage(), ex);
        }
        Map<String, Object> echo = new LinkedHashMap<>();
        if (parametros != null) {
            echo.putAll(parametros);
        }
        return new ResultadoFigura(
                figura.getNombre(),
                entrada.clase().getName(),
                entrada.origen().name(),
                echo,
                redondear(area),
                redondear(perimetro),
                String.format(Locale.US, "%s -> area: %.2f, perimetro: %.2f",
                        figura.getNombre(), area, perimetro));
    }

    // ------------------------------------------------------------ inspeccion

    /** Descubre en tiempo de ejecucion como se construye una clase (constructores y parametros). */
    public TipoInfo describir(Entrada entrada) {
        List<List<ParametroInfo>> constructores = Arrays.stream(entrada.clase().getConstructors())
                .map(c -> Arrays.stream(c.getParameters())
                        .map(p -> new ParametroInfo(p.getName(), p.getType().getSimpleName()))
                        .toList())
                .toList();
        List<String> metodos = Arrays.stream(Figura.class.getMethods())
                .map(Method::getName)
                .sorted()
                .toList();
        return new TipoInfo(entrada.nombre(), entrada.clase().getName(),
                entrada.origen().name(), constructores, metodos);
    }

    public List<TipoInfo> describirTodos() {
        return registro.listar().stream().map(this::describir).toList();
    }

    // --------------------------------------------------------------- internos

    private Object[] enlazar(Constructor<?> constructor, Map<String, ?> params) {
        Parameter[] formales = constructor.getParameters();
        Object[] argumentos = new Object[formales.length];
        boolean hayNombres = Arrays.stream(formales).allMatch(Parameter::isNamePresent);

        if (hayNombres) {
            Map<String, Object> porNombre = new HashMap<>();
            params.forEach((k, v) -> porNombre.put(Nombres.simplificar(k), v));
            for (int i = 0; i < formales.length; i++) {
                String clave = Nombres.simplificar(formales[i].getName());
                if (!porNombre.containsKey(clave)) {
                    throw new ParametrosInvalidosException("Falta el parametro '" + formales[i].getName()
                            + "'. Se esperaba: " + firma(constructor));
                }
                argumentos[i] = convertir(porNombre.get(clave), formales[i].getType(), formales[i].getName());
            }
        } else {
            // Sin nombres en el .class: se usa el orden en que llegaron los parametros.
            int i = 0;
            for (Object valor : params.values()) {
                argumentos[i] = convertir(valor, formales[i].getType(), "arg" + i);
                i++;
            }
        }
        return argumentos;
    }

    private Figura instanciar(Entrada entrada, Constructor<?> constructor, Object[] argumentos) {
        try {
            return (Figura) constructor.newInstance(argumentos);
        } catch (InvocationTargetException ex) {
            Throwable causa = ex.getCause();
            if (causa instanceof IllegalArgumentException) {
                throw new ParametrosInvalidosException(causa.getMessage());
            }
            throw new IllegalStateException("No se pudo construir " + entrada.nombre()
                    + ": " + (causa == null ? ex.getMessage() : causa.toString()), causa);
        } catch (InstantiationException | IllegalAccessException ex) {
            throw new IllegalStateException("No se pudo construir " + entrada.nombre(), ex);
        }
    }

    private Object convertir(Object valor, Class<?> destino, String campo) {
        if (valor == null) {
            if (destino.isPrimitive()) {
                throw new ParametrosInvalidosException("El parametro '" + campo + "' no puede ser nulo");
            }
            return null;
        }
        Class<?> tipo = destino.isPrimitive() ? ENVOLTORIOS.get(destino) : destino;

        if (tipo == String.class) {
            return String.valueOf(valor);
        }
        if (tipo == Boolean.class) {
            if (valor instanceof Boolean) {
                return valor;
            }
            String texto = String.valueOf(valor).trim();
            if (texto.equalsIgnoreCase("true") || texto.equalsIgnoreCase("false")) {
                return Boolean.parseBoolean(texto);
            }
            throw new ParametrosInvalidosException("El parametro '" + campo + "' debe ser true o false");
        }
        if (esNumerico(tipo)) {
            try {
                BigDecimal numero = valor instanceof Number n
                        ? new BigDecimal(n.toString())
                        : new BigDecimal(String.valueOf(valor).trim());
                if (tipo == Double.class) return numero.doubleValue();
                if (tipo == Float.class) return numero.floatValue();
                if (tipo == Integer.class) return numero.intValueExact();
                if (tipo == Long.class) return numero.longValueExact();
                if (tipo == Short.class) return numero.shortValueExact();
                if (tipo == Byte.class) return numero.byteValueExact();
                return numero; // BigDecimal
            } catch (NumberFormatException | ArithmeticException ex) {
                throw new ParametrosInvalidosException("El parametro '" + campo + "' debe ser "
                        + destino.getSimpleName() + " y se recibio '" + valor + "'");
            }
        }
        if (tipo.isInstance(valor)) {
            return valor;
        }
        throw new ParametrosInvalidosException("El parametro '" + campo + "' espera "
                + destino.getSimpleName() + " y se recibio " + valor.getClass().getSimpleName());
    }

    private static boolean esNumerico(Class<?> tipo) {
        return tipo == Double.class || tipo == Float.class || tipo == Integer.class
                || tipo == Long.class || tipo == Short.class || tipo == Byte.class
                || tipo == BigDecimal.class;
    }

    private static double redondear(double valor) {
        return Math.round(valor * 10_000d) / 10_000d;
    }

    private static List<String> firmas(Entrada entrada) {
        List<String> firmas = new ArrayList<>();
        for (Constructor<?> c : entrada.clase().getConstructors()) {
            firmas.add(firma(c));
        }
        return firmas;
    }

    private static String firma(Constructor<?> constructor) {
        StringBuilder sb = new StringBuilder(constructor.getDeclaringClass().getSimpleName()).append('(');
        Parameter[] ps = constructor.getParameters();
        for (int i = 0; i < ps.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(ps[i].getType().getSimpleName()).append(' ').append(ps[i].getName());
        }
        return sb.append(')').toString();
    }
}
