package main.web.services.fitsense.configuracion;

import main.web.services.fitsense.planning.application.internal.commandservices.PlanningPolicySelector;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningPolicy;
import main.web.services.fitsense.planning.infrastructure.generation.ai.PlanPromptBuilder;
import main.web.services.fitsense.planning.infrastructure.generation.ai.SessionFixPromptBuilder;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Configuracion EFECTIVA de un arranque normal del backend: la de {@code application.properties} sin variables de
 * entorno (las propiedades de sistema que surefire y failsafe anaden para aislar las pruebas no participan aqui).
 * Es la configuracion evaluada con Replicate real (docs/FitSense/consolidacion_ia/): PR-1.7, V2.1, PLAN-REV-2.0, IA,
 * progresion, limite de dificultad y retirada localizada, con V8C multiconjunto. Cada valor conserva su interruptor.
 * Sin Spring ni red: no puede llamar a Replicate.
 */
@DisplayName("Configuracion evaluada: valores efectivos por defecto e interruptores")
class ConfiguracionEvaluadaTest {

    private static PropertySourcesPropertyResolver resolver(Map<String, Object> entorno) throws IOException {
        Properties props = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"));
        var fuentes = new MutablePropertySources();
        fuentes.addLast(new MapPropertySource("entorno", entorno));
        fuentes.addLast(new MapPropertySource("application.properties", toMap(props)));
        return new PropertySourcesPropertyResolver(fuentes);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Map<String, Object> toMap(Properties p) {
        return (Map) p;
    }

    private static String valor(PropertySourcesPropertyResolver r, String clave) {
        return r.getRequiredProperty(clave).strip();
    }

    @Test
    void porDefectoEsLaConfiguracionEvaluada() throws IOException {
        var r = resolver(Map.of());
        assertEquals("true", valor(r, "fitsense.ai.enabled"), "IA encendida por defecto");
        assertEquals("v2.1", valor(r, "fitsense.ai.adaptation.prompt-version"));
        assertEquals("PLAN-REV-2.0", valor(r, "fitsense.planning.policy"));
        assertEquals("true", valor(r, "fitsense.planning.policy.allow-candidates"));
        assertEquals("true", valor(r, "fitsense.adaptation.progression.enabled"));
        assertEquals("preferencia", valor(r, "fitsense.adaptation.difficulty-guard"));
        assertEquals("on", valor(r, "fitsense.adaptation.pain-localized-removal"));
        // Sin cambios respecto a lo evaluado.
        assertEquals("multiconjunto", valor(r, "fitsense.adaptation.v8c.pattern-rule"));
        assertEquals("ENFORCE", valor(r, "fitsense.dose.enforcement"));
        assertEquals("ENFORCE", valor(r, "fitsense.capability.enforcement"));
        assertEquals("ENFORCE_AI_ONLY", valor(r, "fitsense.coverage.enforcement"));
        assertEquals("gpt-5-mini", valor(r, "fitsense.ai.model"));
        assertEquals("150", valor(r, "fitsense.ai.timeout-seconds"));
        assertEquals("false", valor(r, "fitsense.adaptation.capability-validation.enabled"),
                "la V24 por capacidades en la adaptacion la enciende la progresion, no esta bandera");
        assertEquals("", r.getProperty("fitsense.ai.api-token", ""), "ningun token en el repositorio");
    }

    @Test
    void losValoresSeleccionanLasImplementacionesEvaluadas() throws IOException {
        var r = resolver(Map.of());
        var selector = new PlanningPolicySelector(valor(r, "fitsense.planning.policy"),
                Boolean.parseBoolean(valor(r, "fitsense.planning.policy.allow-candidates")));
        assertSame(PlanningPolicy.REV2, selector.activa());
        var fix = new SessionFixPromptBuilder(new JsonSupport(), valor(r, "fitsense.ai.adaptation.prompt-version"));
        assertEquals("FIX-PR-2.1-EXP", fix.version());
        assertEquals(SessionFixPromptBuilder.VERSION_V21, fix.version());
        assertEquals("PR-1.7", PlanPromptBuilder.VERSION, "generacion: PR-1.7, sin variantes");
    }

    @Test
    void cadaValorConservaSuInterruptor() throws IOException {
        var r = resolver(Map.of(
                "FITSENSE_AI_ENABLED", "false",
                "FITSENSE_AI_ADAPTATION_PROMPT_VERSION", "v1",
                "FITSENSE_PLANNING_POLICY", "PLAN-HIST-1",
                "FITSENSE_PLANNING_POLICY_ALLOW_CANDIDATES", "false",
                "FITSENSE_PROGRESSION_ENABLED", "false",
                "FITSENSE_DIFFICULTY_GUARD", "off",
                "FITSENSE_PAIN_LOCALIZED_REMOVAL", "off"));
        assertEquals("false", valor(r, "fitsense.ai.enabled"));
        assertEquals("v1", valor(r, "fitsense.ai.adaptation.prompt-version"));
        assertEquals("false", valor(r, "fitsense.adaptation.progression.enabled"));
        assertEquals("off", valor(r, "fitsense.adaptation.difficulty-guard"));
        assertEquals("off", valor(r, "fitsense.adaptation.pain-localized-removal"));
        var selector = new PlanningPolicySelector(valor(r, "fitsense.planning.policy"),
                Boolean.parseBoolean(valor(r, "fitsense.planning.policy.allow-candidates")));
        assertSame(PlanningPolicy.HISTORICA, selector.activa(), "reversion a la historica por variable");
        assertEquals("FIX-PR-1.3",
                new SessionFixPromptBuilder(new JsonSupport(), valor(r, "fitsense.ai.adaptation.prompt-version")).version());
    }

    @Test
    void elPerfilLocalDevSigueSinIa() throws IOException {
        Properties dev = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application-local-dev.properties"));
        assertEquals("false", dev.getProperty("fitsense.ai.enabled").strip());
        assertEquals("", dev.getProperty("fitsense.ai.api-token").strip());
    }
}
