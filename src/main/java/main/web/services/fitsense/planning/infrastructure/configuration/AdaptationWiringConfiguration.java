package main.web.services.fitsense.planning.infrastructure.configuration;

import main.web.services.fitsense.planning.domain.services.AdaptationEvidenceMapper;
import main.web.services.fitsense.planning.domain.services.AdaptedWeekProducer;
import main.web.services.fitsense.planning.domain.services.ConservationValidator;
import main.web.services.fitsense.planning.domain.services.SessionFixApplier;
import main.web.services.fitsense.planning.domain.services.StrictPlanValidation;
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

    /**
     * El productor de la semana adaptada.
     * <p>
     * AQUI Y NO CON {@code @Component}, por lo mismo que el mapper: es dominio
     * puro y sus pruebas lo construyen con {@code new}. Hasta P2-B-3 no estaba
     * registrado en ningun sitio, y el comentario de la clase explicaba por
     * que: nadie implementaba {@link StrictPlanValidation}, asi que registrarlo
     * habria roto el arranque por una dependencia no satisfecha. Esa condicion
     * se cumplio: {@code StrictValidationAdapter} es la unica implementacion y
     * es un {@code @Component}.
     * <p>
     * SU CONSTRUCTOR SE NIEGA A CONSTRUIRLO si la validacion no bloquea la
     * dosis de 4D, y eso convierte el arranque de Spring en la comprobacion:
     * si alguien dejara el adaptador en OBSERVE, el contexto no levanta en vez
     * de publicar en silencio dosis que 4D rechaza.
     */
    @Bean
    public AdaptedWeekProducer adaptedWeekProducer(SessionFixApplier applier,
                                                   ConservationValidator conservation,
                                                   StrictPlanValidation estricta) {
        return new AdaptedWeekProducer(applier, conservation, estricta);
    }
}
