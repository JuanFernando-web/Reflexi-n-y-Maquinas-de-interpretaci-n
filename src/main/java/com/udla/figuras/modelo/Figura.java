package com.udla.figuras.modelo;

/**
 * Contrato comun de todas las figuras (Parte 1 del diseno).
 * Las figuras nativas y las cargadas en Groovy en tiempo de ejecucion lo implementan.
 */
public interface Figura {

    String getNombre();

    double calcularArea();

    double calcularPerimetro();
}
