package main.web.services.fitsense.orchestation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
 * Rama de evaluacion K20: la correccion P3 esta aplicada en DescripcionAdaptacion y la prueba es PERMANENTE (antes
 * opt-in, porque fallaba; ver docs/FitSense/adaptacion/salida_intermedia_dolor/11_CIERRE_PR39.md).
 */
@Testcontainers
// ESCENARIO CONGELADO sobre el catalogo V50: los perfiles y planes de esta prueba se eligieron con ese catalogo (el
// plan lleva el 135, el viernes acortado...). Lo que se prueba es el mecanismo de orquestacion, no el catalogo; con
// V52 el motor elige otros ejercicios y la premisa del escenario deja de darse. El esquema es el vigente (columnas de
// V51-V53 sin datos: regla por nombre del catalogo fijado).
@org.springframework.context.annotation.Import(main.web.services.fitsense.EsquemaVigenteSobreCatalogoAnterior.class)
@SpringBootTest(properties = {
        "spring.flyway.target=50",
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        // Valores historicos fijados explicitamente al activar la configuracion evaluada (11/11/2026): esta
        // prueba se escribio sobre los valores por defecto anteriores y comprueba ese comportamiento.
        "fitsense.planning.policy=PLAN-HIST-1",
        "fitsense.ai.adaptation.prompt-version=v1",
        "fitsense.adaptation.progression.enabled=false",
        "fitsense.adaptation.difficulty-guard=off",
        "fitsense.adaptation.pain-localized-removal=off",
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
