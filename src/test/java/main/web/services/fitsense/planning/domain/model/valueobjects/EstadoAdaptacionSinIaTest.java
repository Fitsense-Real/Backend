package main.web.services.fitsense.planning.domain.model.valueobjects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R-4 · LA API NO DICE «PROPUESTA INVALIDA» CUANDO LA IA NO PROPUSO NADA (resiliencia de Replicate, 11/11/2026).
 * <p>
 * El resultado se derivaba solo de ai_calls: con una llamada hecha y fallida (Replicate caido, timeout, respuesta
 * ilegible) salia RESPALDO_PROPUESTA_IA_INVALIDA, como si el modelo hubiera propuesto algo que se rechazo. Ahora la
 * evidencia V32 lleva la nota {@code IA_NO_DISPONIBLE:} y el resultado es el RESPALDO_SIN_IA que ya existia: sin
 * estados nuevos, sin migraciones y sin cambiar el contrato de la API.
 */
@DisplayName("R-4 · estados de adaptacion: sin propuesta del proveedor frente a propuesta rechazada")
class EstadoAdaptacionSinIaTest {

    private static final LocalDate L = LocalDate.of(2026, 11, 9);
    private static final Long[] UNA = {15L};

    private static AdaptationOutcome de(String estado, int llamadas, String intervencion, Long[] aplicadas,
                                        Long[] sinEfecto, Long[] conservadas, List<String> correcciones) {
        return AdaptationOutcome.de(1L, L, "NO_FACTIBLE".equals(estado) ? null : 9L, estado, "ADAPT_LOCALIZED",
                llamadas, UNA, new String[]{intervencion}, aplicadas, sinEfecto, conservadas, null, correcciones,
                "NO_FACTIBLE".equals(estado) ? "Sin respuesta de la IA no se puede resolver el dolor" : null);
    }

    private static final String CAIDO = AdaptationOutcome.NOTA_IA_NO_DISPONIBLE
            + "El proveedor de IA no esta disponible: 503 Service Unavailable";

    @Test
    @DisplayName("FALLABA ANTES · 1 llamada y el proveedor no respondio: RESPALDO_SIN_IA, y ai_calls sigue siendo 1")
    void proveedorCaidoNoEsPropuestaInvalida() {
        var o = de("RESPALDO", 1, "VARIANTE_MAS_SENCILLA_O_CARGA", null, null, UNA, List.of(CAIDO));
        assertEquals("RESPALDO_SIN_IA", o.resultado());
        assertEquals(1, o.llamadasIa(), "el coste real (llamadas hechas) no se oculta");
        assertTrue(o.motivos().isEmpty(), "la causa tecnica queda en V32, no se muestra al usuario");
    }

    @Test
    @DisplayName("timeout y respuesta ilegible: tambien RESPALDO_SIN_IA")
    void timeoutYRespuestaIlegible() {
        for (var causa : List.of("El proveedor de IA no esta disponible: starting null",
                "El proveedor de IA no esta disponible: la respuesta de Replicate no se pudo interpretar"))
            assertEquals("RESPALDO_SIN_IA", de("RESPALDO", 1, "ACORTAR_O_REORGANIZAR", null, null, UNA,
                    List.of(AdaptationOutcome.NOTA_IA_NO_DISPONIBLE + causa)).resultado(), causa);
    }

    @Test
    @DisplayName("la IA propuso y todo se rechazo (V8C u otro validador): sigue siendo RESPALDO_PROPUESTA_IA_INVALIDA")
    void propuestaRechazadaNoCambia() {
        assertEquals("RESPALDO_PROPUESTA_IA_INVALIDA",
                de("RESPALDO", 3, "VARIANTE_MAS_SENCILLA_O_CARGA", null, null, UNA, List.of()).resultado());
    }

    @Test
    @DisplayName("IA apagada o cortacircuitos (0 llamadas): RESPALDO_SIN_IA, como antes")
    void sinLlamadas() {
        assertEquals("RESPALDO_SIN_IA",
                de("RESPALDO", 0, "ACORTAR_O_REORGANIZAR", null, null, UNA, List.of()).resultado());
    }

    @Test
    @DisplayName("los demas estados no cambian: aplicada, sin cambios validos, no factible, regeneracion segura")
    void demasEstadosIntactos() {
        assertEquals("ADAPTACION_APLICADA",
                de("APLICADA", 1, "ACORTAR_O_REORGANIZAR", UNA, null, null, List.of()).resultado());
        assertEquals("SIN_CAMBIOS_VALIDOS",
                de("SIN_EFECTO", 1, "ACORTAR_O_REORGANIZAR", null, UNA, null, List.of()).resultado(),
                "la IA respondio sin un cambio aplicable");
        assertEquals("NO_FACTIBLE_POR_SEGURIDAD",
                de("NO_FACTIBLE", 1, "SEGURIDAD_ANTE_DOLOR", null, null, null, List.of(CAIDO)).resultado(),
                "no publicar sigue siendo no publicar, aunque el proveedor cayera");
        assertEquals("REGENERACION_SEGURA_SUSTITUCION",
                de("RESPALDO", 1, "SEGURIDAD_ANTE_DOLOR", null, null, UNA,
                        List.of(CAIDO, AdaptationOutcome.NOTA_REGENERACION_SEGURA + "SUST-SEG-1")).resultado(),
                "la sustitucion por seguridad sigue teniendo prioridad");
        assertEquals("ACORTAMIENTO_IMPOSIBLE",
                de("RESPALDO", 0, "ACORTAR_O_REORGANIZAR", null, null, UNA,
                        List.of("FACTIBILIDAD_PREVIA: no baja de 28 min")).resultado());
    }
}
