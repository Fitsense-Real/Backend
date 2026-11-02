package main.web.services.fitsense.planning.domain.model.valueobjects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** La traduccion de V32 para el cliente: ningun resultado conservado parece una adaptacion ejecutada. */
class AdaptationOutcomeTest {

    static final LocalDate L = LocalDate.of(2026, 11, 9);
    static final Long[] UNA = {15L};

    private static AdaptationOutcome de(String estado, int llamadas, String intervencion, Long[] aplicadas,
                                        Long[] sinEfecto, Long[] conservadas, List<String> correcciones, String rechazo) {
        return AdaptationOutcome.de(1L, L, "NO_FACTIBLE".equals(estado) ? null : 9L, estado, "ADAPT_LOCALIZED",
                llamadas, UNA, new String[]{intervencion}, aplicadas, sinEfecto, conservadas, null, correcciones, rechazo);
    }

    @Test
    @DisplayName("las seis situaciones de la orden se distinguen")
    void seisSituaciones() {
        var aplicada = de("APLICADA", 1, "ACORTAR_O_REORGANIZAR", UNA, null, null, List.of(), null);
        var conservar = de("SIN_EFECTO", 0, "CONSERVAR", null, UNA, null, List.of(), null);
        var duracion = de("RESPALDO", 0, "ACORTAR_O_REORGANIZAR", null, null, UNA,
                List.of("FACTIBILIDAD_PREVIA: ACORTAR de la sesion 15: ... no baja de 28 min y la persona declaro 22"), null);
        var fechas = de("RESPALDO", 0, "REPROGRAMAR_DIAS", null, null, UNA,
                List.of("FACTIBILIDAD_PREVIA: REPROGRAMAR de la sesion 15: ninguna otra fecha disponible"), null);
        var invalida = de("RESPALDO", 3, "VARIANTE_MAS_SENCILLA_O_CARGA", null, null, UNA, List.of(), null);
        var seguridad = de("NO_FACTIBLE", 3, "SEGURIDAD_ANTE_DOLOR", null, null, null, List.of(),
                "Sin respuesta de la IA no se puede resolver el dolor de las sesiones [15]");

        assertEquals("ADAPTACION_APLICADA", aplicada.resultado());
        assertEquals("APLICADA", aplicada.sesiones().get(0).resultado());
        assertEquals("CONSERVACION_DELIBERADA", conservar.resultado());
        assertEquals("ACORTAMIENTO_IMPOSIBLE", duracion.resultado());
        assertTrue(duracion.motivos().get(0).contains("no baja de 28 min"), "el cliente ve la cota, sin el prefijo");
        assertEquals("CONSERVADA", duracion.sesiones().get(0).resultado(), "la sesion no se presenta como ajustada");
        assertEquals("REPROGRAMACION_IMPOSIBLE", fechas.resultado());
        assertEquals("RESPALDO_PROPUESTA_IA_INVALIDA", invalida.resultado());
        assertEquals("NO_FACTIBLE_POR_SEGURIDAD", seguridad.resultado());
        assertNull(seguridad.resultingPlanId(), "NO_FACTIBLE no publica semana");
        assertEquals(List.of("Sin respuesta de la IA no se puede resolver el dolor de las sesiones [15]"), seguridad.motivos());
    }

    @Test
    @DisplayName("sin IA disponible, sin cambios validos, progresion y sin orden tambien se distinguen")
    void restoDeEstados() {
        assertEquals("RESPALDO_SIN_IA", de("RESPALDO", 0, "ACORTAR_O_REORGANIZAR", null, null, UNA, List.of(), null).resultado());
        assertEquals("SIN_CAMBIOS_VALIDOS", de("SIN_EFECTO", 1, "ACORTAR_O_REORGANIZAR", null, UNA, null, List.of(), null).resultado());
        var progresion = AdaptationOutcome.de(1L, L, 9L, "SIN_ORDEN", "NO_CHANGE", 0, new Long[0], new String[0],
                null, null, null, new Long[]{15L}, List.of(), null);
        assertEquals("PROGRESION_DEL_BACKEND", progresion.resultado());
        var nada = AdaptationOutcome.de(1L, L, 9L, "SIN_ORDEN", "NO_CHANGE", 0, new Long[0], new String[0],
                null, null, null, new Long[0], List.of(), null);
        assertEquals("SIN_ADAPTACION", nada.resultado());
    }

    @Test
    @DisplayName("P-03: los dos resultados nuevos, sin cambiar los existentes ni el estado V32")
    void resultadosP03() {
        var sustitucion = de("RESPALDO", 3, "SEGURIDAD_ANTE_DOLOR", null, null, UNA,
                List.of("REGENERACION_SEGURA: sesiones [15] resueltas sin IA por sustitucion SUST-SEG-1 de [129->78]"), null);
        assertEquals("REGENERACION_SEGURA_SUSTITUCION", sustitucion.resultado());
        assertEquals("RESPALDO", sustitucion.estado(), "el estado V32 se informa tal cual");

        var noFactible = de("NO_FACTIBLE", 3, "SEGURIDAD_ANTE_DOLOR", null, null, null, List.of(), "dolor sin resolver");
        var reglas = noFactible.conRegeneracion(42L, "REGENERACION_SEGURA_REGLAS", "P-03: semana regenerada");
        assertEquals("REGENERACION_SEGURA_REGLAS", reglas.resultado());
        assertEquals("NO_FACTIBLE", reglas.estado(), "la adaptacion fallida sigue constando");
        assertEquals(42L, reglas.resultingPlanId());
        assertEquals(List.of("dolor sin resolver", "P-03: semana regenerada"), reglas.motivos());
        assertEquals("NO_FACTIBLE_POR_SEGURIDAD", noFactible.resultado(), "sin regeneracion, lo de siempre");

        // Un respaldo sin la nota de P-03 se sigue leyendo como antes.
        assertEquals("RESPALDO_PROPUESTA_IA_INVALIDA", de("RESPALDO", 3, "INVESTIGAR_CAMBIO_MINIMO", null, null, UNA,
                List.of(), null).resultado());
    }

    @Test
    @DisplayName("contrato: los campos del recurso no cambian de nombre ni de orden")
    void camposDelContrato() {
        assertEquals(List.of("attemptId", "weekStartDate", "resultingPlanId", "resultado", "estado", "decision",
                "llamadasIa", "motivos", "sesiones"), java.util.Arrays.stream(AdaptationOutcome.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName).toList());
    }
}
