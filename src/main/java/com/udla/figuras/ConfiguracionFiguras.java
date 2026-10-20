package com.udla.figuras;

import com.udla.figuras.modelo.Figura;
import com.udla.figuras.nucleo.CargadorGroovy;
import com.udla.figuras.nucleo.FabricaFiguras;
import com.udla.figuras.nucleo.RegistroFiguras;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

/**
 * Conecta el nucleo (que no depende de Spring) con el contenedor.
 * Las figuras nativas se DESCUBREN por reflexion: se escanea el paquete del modelo buscando
 * clases concretas que implementen Figura, asi agregar una figura nativa no obliga a tocar esta clase.
 */
@Configuration
public class ConfiguracionFiguras {

    private static final String PAQUETE_MODELO = "com.udla.figuras.modelo";

    @Bean
    public RegistroFiguras registroFiguras() {
        RegistroFiguras registro = new RegistroFiguras();
        ClassPathScanningCandidateComponentProvider escaner = new ClassPathScanningCandidateComponentProvider(false);
        escaner.addIncludeFilter(new AssignableTypeFilter(Figura.class));
        for (BeanDefinition definicion : escaner.findCandidateComponents(PAQUETE_MODELO)) {
            try {
                Class<?> clase = ClassUtils.forName(definicion.getBeanClassName(), getClass().getClassLoader());
                registro.registrarNativa(clase.asSubclass(Figura.class));
            } catch (ClassNotFoundException | LinkageError ex) {
                throw new IllegalStateException("No se pudo cargar la figura " + definicion.getBeanClassName(), ex);
            }
        }
        return registro;
    }

    @Bean
    public FabricaFiguras fabricaFiguras(RegistroFiguras registro) {
        return new FabricaFiguras(registro);
    }

    @Bean
    public CargadorGroovy cargadorGroovy(RegistroFiguras registro) {
        return new CargadorGroovy(registro, getClass().getClassLoader());
    }
}
