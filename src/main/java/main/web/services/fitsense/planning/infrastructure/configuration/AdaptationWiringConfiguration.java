package main.web.services.fitsense.planning.infrastructure.configuration;

import main.web.services.fitsense.planning.domain.services.AdaptationEvidenceMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra las piezas de dominio de la adaptacion que Spring necesita inyectar.
 * <p>
 * POR QUE AQUI Y NO CON {@code @Component} EN LA CLASE.
 * {@link AdaptationEvidenceMapper} es dominio puro: traduce un resultado a una
 * fila de evidencia y no depende de nada del contenedor. Anotarla la ataria a
 * Spring sin ganar nada, y sus pruebas la construyen con {@code new}. El
 * cableado es una decision de infraestructura, asi que vive en infraestructura.
 * <p>
 * {@code proxyBeanMethods = false} porque no hay metodos de bean llamandose
 * entre si: sin proxy, el arranque es algo mas barato y el comportamiento es
 * identico.
 */
@Configuration(proxyBeanMethods = false)
public class AdaptationWiringConfiguration {

    @Bean
    public AdaptationEvidenceMapper adaptationEvidenceMapper() {
        return new AdaptationEvidenceMapper();
    }
}
