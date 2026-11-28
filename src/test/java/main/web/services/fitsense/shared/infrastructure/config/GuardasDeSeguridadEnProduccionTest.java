package main.web.services.fitsense.shared.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("P1-06 · guardas de seguridad: con prod, un valor no aprobado impide arrancar salvo declaracion expresa")
class GuardasDeSeguridadEnProduccionTest {

    /** La configuracion real: application.properties + application-prod.properties. */
    static MockEnvironment prodConLaConfiguracionReal() throws Exception {
        var env = new MockEnvironment();
        env.setActiveProfiles("prod");
        for (var f : new String[]{"application.properties", "application-prod.properties"}) {
            var p = PropertiesLoaderUtils.loadProperties(new ClassPathResource(f));
            for (var k : p.stringPropertyNames()) {
                // ${VAR:valor} -> valor (las variables de entorno no estan definidas en la prueba).
                String v = p.getProperty(k).replaceAll("^\\$\\{[A-Z_]+:(.*)}$", "$1");
                env.setProperty(k, v);
            }
        }
        return env;
    }

    @Test
    @DisplayName("la configuracion aprobada del repositorio arranca con prod")
    void configuracionAprobada() throws Exception {
        var distintas = GuardasDeSeguridadEnProduccion.comprobar(prodConLaConfiguracionReal());
        assertTrue(distintas.isEmpty(), distintas.toString());
    }

    @Test
    @DisplayName("FITSENSE_DOSE_ENFORCEMENT=OBSERVE con prod: no arranca")
    void relajadaNoArranca() throws Exception {
        var env = prodConLaConfiguracionReal();
        env.setProperty("fitsense.dose.enforcement", "OBSERVE");
        var e = assertThrows(IllegalStateException.class, () -> GuardasDeSeguridadEnProduccion.comprobar(env));
        assertTrue(e.getMessage().contains("fitsense.dose.enforcement"), e.getMessage());
    }

    @Test
    @DisplayName("una guarda ausente toma el valor del codigo (mas laxo): con prod, no arranca")
    void ausenteNoArranca() {
        var env = new MockEnvironment();
        env.setActiveProfiles("prod");
        var e = assertThrows(IllegalStateException.class, () -> GuardasDeSeguridadEnProduccion.comprobar(env));
        assertTrue(e.getMessage().contains("fitsense.adaptation.difficulty-guard"), e.getMessage());
    }

    @Test
    @DisplayName("declarada en fitsense.security.guard-overrides: arranca (con WARN)")
    void declaradaArranca() throws Exception {
        var env = prodConLaConfiguracionReal();
        env.setProperty("fitsense.adaptation.pain-localized-removal", "off");
        env.setProperty("fitsense.security.guard-overrides", "fitsense.adaptation.pain-localized-removal");
        var distintas = GuardasDeSeguridadEnProduccion.comprobar(env);
        assertEquals("off", distintas.get("fitsense.adaptation.pain-localized-removal"));
    }

    @Test
    @DisplayName("fuera de prod solo se registra: las pruebas y laboratorios usan otras combinaciones")
    void fueraDeProd() {
        var env = new MockEnvironment();
        env.setProperty("fitsense.dose.enforcement", "OBSERVE");
        assertFalse(GuardasDeSeguridadEnProduccion.comprobar(env).isEmpty());
    }
}
