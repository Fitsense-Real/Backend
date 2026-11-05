package main.web.services.fitsense.planning.infrastructure.configuration;

import main.web.services.fitsense.planning.domain.services.CoberturaDePatron;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Auditoria del catalogo v46 · P15-C EXPERIMENTAL (rama aislada). Por defecto desactivado: ningun plan cambia.
 * Cada arranque de contexto fija el valor desde la propiedad (un contexto con el experimento no contamina al siguiente).
 */
@Configuration
public class CoberturaDePatronConfig {

    private static final Logger log = LoggerFactory.getLogger(CoberturaDePatronConfig.class);

    public CoberturaDePatronConfig(
            @Value("${fitsense.experimental.p15c.aislados-sin-cobertura:false}") boolean aisladosSinCobertura) {
        CoberturaDePatron.configurar(aisladosSinCobertura);
        if (aisladosSinCobertura)
            log.warn("EXPERIMENTO P15-C ACTIVO: los ejercicios aislados no cubren por si solos un patron principal");
    }
}
