package com.udla.figuras.nucleo;

import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ImportNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;

import java.util.List;
import java.util.Set;

/**
 * Revisa el AST del codigo Groovy antes de generar las clases y rechaza usos peligrosos:
 * System, Runtime, procesos, hilos, archivos, red, reflexion, evaluacion dinamica y
 * llamadas a metodos con nombre calculado.
 *
 * Recorre TODA la clase (constructores, metodos, campos, inicializadores); el SecureASTCustomizer
 * que trae Groovy no revisa los constructores, que es justo donde viven los parametros de una figura.
 *
 * Es una lista negra de buena fe, no un sandbox: solo se debe cargar codigo de confianza.
 */
public class VigilanteGroovy extends CompilationCustomizer {

    private static final Set<String> TIPOS_PROHIBIDOS = Set.of(
            "java.lang.System", "java.lang.Runtime", "java.lang.ProcessBuilder", "java.lang.Process",
            "java.lang.Thread", "java.lang.Class", "java.lang.ClassLoader",
            "groovy.lang.GroovyShell", "groovy.lang.GroovyClassLoader", "groovy.util.Eval");

    private static final List<String> PREFIJOS_PROHIBIDOS = List.of(
            "java.io.", "java.net.", "java.nio.", "java.lang.reflect.", "java.lang.invoke.",
            "javax.script.", "org.codehaus.groovy.", "org.springframework.", "com.udla.figuras.nucleo.");

    private static final Set<String> METODOS_PROHIBIDOS = Set.of(
            "execute", "evaluate", "exec", "getRuntime", "forName", "loadClass", "getClassLoader",
            "getMethod", "getDeclaredMethod", "getDeclaredField", "setAccessible", "invokeMethod");

    public VigilanteGroovy() {
        super(CompilePhase.CANONICALIZATION);
    }

    @Override
    public void call(SourceUnit fuente, GeneratorContext contexto, ClassNode clase) {
        revisarImports(fuente.getAST());
        revisarTipo(clase.getUnresolvedSuperClass(), clase.getLineNumber());
        for (ClassNode interfaz : clase.getUnresolvedInterfaces()) {
            revisarTipo(interfaz, clase.getLineNumber());
        }
        new ClassCodeVisitorSupport() {
            @Override
            protected SourceUnit getSourceUnit() {
                return fuente;
            }

            @Override
            public void visitMethodCallExpression(MethodCallExpression llamada) {
                if (!(llamada.getMethod() instanceof ConstantExpression)) {
                    prohibir("llamada a un metodo con nombre calculado", llamada.getLineNumber());
                }
                String nombre = llamada.getMethodAsString();
                if (nombre != null && METODOS_PROHIBIDOS.contains(nombre)) {
                    prohibir("el metodo " + nombre + "()", llamada.getLineNumber());
                }
                super.visitMethodCallExpression(llamada);
            }

            @Override
            public void visitStaticMethodCallExpression(StaticMethodCallExpression llamada) {
                revisarTipo(llamada.getOwnerType(), llamada.getLineNumber());
                if (METODOS_PROHIBIDOS.contains(llamada.getMethod())) {
                    prohibir("el metodo " + llamada.getMethod() + "()", llamada.getLineNumber());
                }
                super.visitStaticMethodCallExpression(llamada);
            }

            @Override
            public void visitConstructorCallExpression(ConstructorCallExpression llamada) {
                revisarTipo(llamada.getType(), llamada.getLineNumber());
                super.visitConstructorCallExpression(llamada);
            }

            @Override
            public void visitClassExpression(ClassExpression expresion) {
                revisarTipo(expresion.getType(), expresion.getLineNumber());
                super.visitClassExpression(expresion);
            }

            @Override
            public void visitDeclarationExpression(DeclarationExpression declaracion) {
                if (declaracion.isMultipleAssignmentDeclaration()) {
                    declaracion.getTupleExpression().getExpressions().forEach(e ->
                            revisarTipo(e.getType(), declaracion.getLineNumber()));
                } else {
                    revisarTipo(declaracion.getVariableExpression().getOriginType(), declaracion.getLineNumber());
                }
                super.visitDeclarationExpression(declaracion);
            }
        }.visitClass(clase);
    }

    private void revisarImports(ModuleNode modulo) {
        for (ImportNode i : modulo.getImports()) {
            revisarTipo(i.getType(), i.getLineNumber());
        }
        for (ImportNode i : modulo.getStaticImports().values()) {
            revisarTipo(i.getType(), i.getLineNumber());
        }
        for (ImportNode i : modulo.getStaticStarImports().values()) {
            revisarTipo(i.getType(), i.getLineNumber());
        }
        for (ImportNode i : modulo.getStarImports()) {
            String paquete = i.getPackageName();
            if (paquete != null && esPrefijoProhibido(paquete)) {
                prohibir("importar " + paquete + "*", i.getLineNumber());
            }
        }
    }

    private void revisarTipo(ClassNode tipo, int linea) {
        if (tipo == null) {
            return;
        }
        ClassNode base = tipo;
        while (base.isArray()) {
            base = base.getComponentType();
        }
        String nombre = base.getName();
        if (TIPOS_PROHIBIDOS.contains(nombre) || esPrefijoProhibido(nombre)) {
            prohibir("la clase " + nombre, linea);
        }
    }

    private static boolean esPrefijoProhibido(String nombre) {
        for (String prefijo : PREFIJOS_PROHIBIDOS) {
            if (nombre.startsWith(prefijo)) {
                return true;
            }
        }
        return false;
    }

    private static void prohibir(String que, int linea) {
        throw new SecurityException("Uso prohibido en codigo Groovy: " + que
                + (linea > 0 ? " (linea " + linea + ")" : ""));
    }
}
