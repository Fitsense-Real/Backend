package main.web.services.fitsense.orchestation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel.INTERMEDIATE;
import static main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType.GENERAL_FITNESS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * P3 (cierre del PR #39): cuando el RESPALDO sustituye por seguridad un ejercicio que dolio (SUST-SEG-1), el texto
 * publicado ({@code adjustment_reason}) tiene que describir ese cambio. Configuracion POR DEFECTO (sin limite ni
 * retirada): es el comportamiento de {@code generation}. Perfil congelado R0521: el 135 duele y SUST-SEG-1 pone el 780.
 * <p>
 * REPRODUCCION DE UN DEFECTO CONFIRMADO (opt-in, {@code -Dfitsense.verificacion.p3=true}): HOY FALLA. El texto publicado
 * es «Reportaste molestias. En una de las sesiones no aplicamos cambios esta semana: se mantiene como estaba.», aunque
 * la sesion cambio (135 -> 780). Es opt-in solo para no dejar CI en rojo mientras la correccion (que cambia el texto
 * con la configuracion por defecto) espera aprobacion; no oculta nada: el fallo esta documentado en
 * docs/FitSense/adaptacion/salida_intermedia_dolor/11_CIERRE_PR39.md. Cuando se apruebe la correccion, quitar la
 * condicion y dejarla permanente.
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.verificacion.p3", matches = "true")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("P3: el texto publicado de un respaldo con sustitucion por seguridad describe el cambio")
class TextoRespaldoSustitucionIT extends RetiradaLocalizadaBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    @DisplayName("R0521: 135 -> 780 por SUST-SEG-1 en el respaldo; el texto no puede decir que la sesion no cambio")
    void textoDescribeLaSustitucion() {
        var e = escenario("R0521", GENERAL_FITNESS, INTERMEDIATE, 6, 30);
        assertEquals("RESPALDO", e.v32().get("status"), e.v32()::toString);
        long plan = e.plan().orElseThrow();
        assertTrue(ejerciciosDe(plan).contains(780L), "premisa: SUST-SEG-1 sustituyo el 135 por el 780");
        assertFalse(ejerciciosDe(plan).contains(EJ));
        String texto = textoPublicado(plan);
        System.out.println("P3_TEXTO " + texto);
        assertFalse(texto.contains("no aplicamos cambios"), "la sesion SI cambio: " + texto);
        assertTrue(texto.contains("Por seguridad, cambiamos o retiramos 1 ejercicio"), texto);
    }
}
