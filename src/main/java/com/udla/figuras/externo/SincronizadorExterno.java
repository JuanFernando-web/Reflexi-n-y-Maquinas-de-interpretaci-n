package com.udla.figuras.externo;

import com.udla.figuras.nucleo.CargadorGroovy;
import com.udla.figuras.nucleo.InfoFiguras.ResultadoCarga;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pide las figuras al servicio externo y las compila con el mismo cargador de Groovy. */
@Component
public class SincronizadorExterno {

    private final ClienteServicioExterno cliente;
    private final CargadorGroovy cargador;

    public SincronizadorExterno(ClienteServicioExterno cliente, CargadorGroovy cargador) {
        this.cliente = cliente;
        this.cargador = cargador;
    }

    public ResultadoCarga sincronizar() {
        List<String> cargadas = new ArrayList<>();
        Map<String, String> errores = new LinkedHashMap<>();
        for (FiguraExterna figura : cliente.obtenerFiguras()) {
            String etiqueta = figura.nombre() == null ? "(sin nombre)" : figura.nombre();
            try {
                cargador.cargar(figura.codigo()).forEach(e -> cargadas.add(e.nombre()));
            } catch (RuntimeException ex) {
                errores.put(etiqueta, ex.getMessage());
            }
        }
        return new ResultadoCarga(cargadas, errores);
    }
}
