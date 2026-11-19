package main.web.services.fitsense.planning.domain.model.valueobjects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contrato de {@link AdaptationOutcome} tras la fase 2.1 del cierre definitivo: los nueve campos de siempre no cambian
 * (nombre, tipo y valor) y los estructurados son aditivos. El codigo nunca se deduce del texto.
 */
@DisplayName("AdaptationOutcome: NO_FACTIBLE estructurado y compatible con la respuesta anterior")
class AdaptationOutcomeContratoTest {

    static final LocalDate L = LocalDate.of(2026, 11, 9);
    static final Long[] UNA = {15L};
    static final ObjectMapper JSON = new ObjectMapper().registerModule(new JavaTimeModule());
    static final List<String> CAMPOS_ANTERIORES = List.of("attemptId", "weekStartDate", "resultingPlanId", "resultado",
            "estado", "decision", "llamadasIa", "motivos", "sesiones");

    private static AdaptationOutcome noFactible(List<String> codigos) {
        return AdaptationOutcome.de(1L, L, null, "NO_FACTIBLE", "ADAPT_LOCALIZED", 0, UNA,
                new String[]{"SEGURIDAD_ANTE_DOLOR"}, null, null, null, null, List.of(),
                "SEGURIDAD: el ejercicio 1373 ... no hay sustituto elegible con la regla SUST-SEG-1", codigos);
    }

    @Test
    @DisplayName("la respuesta anterior se conserva campo a campo; los campos nuevos son aditivos")
    void compatibleConLaRespuestaAnterior() throws Exception {
        var antes = AdaptationOutcome.de(1L, L, null, "NO_FACTIBLE", "ADAPT_LOCALIZED", 0, UNA,
                new String[]{"SEGURIDAD_ANTE_DOLOR"}, null, null, null, null, List.of(), "motivo tecnico");
        var ahora = noFactible(List.of("SIN_SUSTITUTO_SEGURO"));
        JsonNode a = JSON.valueToTree(antes), b = JSON.valueToTree(ahora);
        for (var campo : CAMPOS_ANTERIORES) {
            assertTrue(b.has(campo), "falta el campo anterior " + campo);
            if (!campo.equals("motivos")) assertEquals(a.get(campo), b.get(campo), campo + " no cambia");
        }
        assertEquals("NO_FACTIBLE_POR_SEGURIDAD", b.get("resultado").asText(), "el resultado de siempre");
        var nuevos = new ArrayList<String>();
        b.fieldNames().forEachRemaining(n -> { if (!CAMPOS_ANTERIORES.contains(n)) nuevos.add(n); });
        assertEquals(List.of("estadoPublicacion", "motivoCodigo", "motivoTexto", "codigos", "sesionAfectada",
                "restricciones"), nuevos, "solo se anaden estos campos");
    }

    @Test
    @DisplayName("NO_FACTIBLE con codigo: estado de publicacion, codigo de fondo y texto neutro del catalogo")
    void noFactibleConCodigo() {
        var o = noFactible(List.of("SIN_SUSTITUTO_SEGURO", "SEMANA_NO_VALIDA"));
        assertEquals(AdaptationOutcome.NO_PUBLICADO, o.estadoPublicacion());
        assertEquals("SIN_SUSTITUTO_SEGURO", o.motivoCodigo(), "el de fondo es el primero");
        assertEquals(NoFactibleReason.SIN_SUSTITUTO_SEGURO.texto(), o.motivoTexto());
        assertFalse(o.motivoTexto().contains("1373") || o.motivoTexto().contains("SUST-SEG"),
                "el texto para la persona no expone detalles internos");
        assertEquals(List.of("SIN_SUSTITUTO_SEGURO", "SEMANA_NO_VALIDA"), o.codigos());
    }

    @Test
    @DisplayName("fila anterior a V44_1 (sin codigo): no se deduce nada del texto")
    void filaAnteriorSinCodigo() {
        var o = noFactible(List.of());
        assertNull(o.motivoCodigo(), "aunque el texto diga «SUST-SEG-1», no se inventa el codigo");
        assertNull(o.motivoTexto());
        assertEquals(AdaptationOutcome.NO_PUBLICADO, o.estadoPublicacion(), "eso si consta: no hay plan");
        assertEquals(List.of(), o.codigos());
    }

    @Test
    @DisplayName("un codigo desconocido (de una version posterior) se conserva pero sin texto inventado")
    void codigoDesconocido() {
        var o = noFactible(List.of("ALGO_NUEVO"));
        assertEquals("ALGO_NUEVO", o.motivoCodigo());
        assertNull(o.motivoTexto());
    }

    @Test
    @DisplayName("publicado: sin codigo de no factible; la factibilidad previa da DISPONIBILIDAD_NO_CUMPLIBLE")
    void publicados() {
        var conservada = AdaptationOutcome.de(1L, L, 9L, "SIN_ORDEN", "CONSERVE", 0, null, null, null, null, null,
                null, List.of(), null);
        assertEquals(AdaptationOutcome.PUBLICADO, conservada.estadoPublicacion());
        assertNull(conservada.motivoCodigo());
        var acortar = AdaptationOutcome.de(1L, L, 9L, "RESPALDO", "ADAPT_LOCALIZED", 0, UNA,
                new String[]{"ACORTAR_O_REORGANIZAR"}, null, null, UNA, null,
                List.of("FACTIBILIDAD_PREVIA: no baja de 28 min y la persona declaro 22"), null);
        assertEquals("ACORTAMIENTO_IMPOSIBLE", acortar.resultado());
        assertEquals(AdaptationOutcome.PUBLICADO, acortar.estadoPublicacion());
        assertEquals("DISPONIBILIDAD_NO_CUMPLIBLE", acortar.motivoCodigo());
        assertNotNull(acortar.motivoTexto());
    }

    @Test
    @DisplayName("P-03 nivel 2: NO_FACTIBLE suma su codigo detras; publicado borra el codigo de fondo")
    void regeneracion() {
        var base = noFactible(List.of("SIN_SUSTITUTO_SEGURO"));
        var fallida = base.conRegeneracion(null, base.resultado(), "P-03: sin alternativa", "TRABAJO_SUPERIOR_AL_PERMITIDO");
        assertEquals(List.of("SIN_SUSTITUTO_SEGURO", "TRABAJO_SUPERIOR_AL_PERMITIDO"), fallida.codigos());
        assertEquals("SIN_SUSTITUTO_SEGURO", fallida.motivoCodigo());
        assertEquals(AdaptationOutcome.NO_PUBLICADO, fallida.estadoPublicacion());
        var publicada = base.conRegeneracion(77L, "REGENERACION_SEGURA_REGLAS", "P-03: regenerada", null);
        assertEquals(AdaptationOutcome.PUBLICADO, publicada.estadoPublicacion());
        assertNull(publicada.motivoCodigo(), "hubo semana: no es un no factible");
        assertEquals("REGENERACION_SEGURA_REGLAS", publicada.resultado());
    }

    @Test
    @DisplayName("detalle: sesion afectada y restricciones")
    void detalle() {
        var o = noFactible(List.of("SIN_SUSTITUTO_SEGURO")).conDetalle(
                new AdaptationOutcome.SesionAfectada(15L, L.minusDays(4), "FULL_BODY"),
                List.of(new AdaptationOutcome.Restriccion(AdaptationOutcome.Restriccion.EXCLUSION_DOLOR, 1373L)));
        assertEquals(15L, o.sesionAfectada().plannedWorkoutId());
        assertEquals(1373L, o.restricciones().get(0).exerciseId());
        assertEquals("SIN_SUSTITUTO_SEGURO", o.motivoCodigo(), "el detalle no cambia el codigo");
    }

    @Test
    @DisplayName("invariantes: un resultado publicable o un estado que publica no llevan codigo de no factible")
    void invariantes() {
        assertThrows(IllegalArgumentException.class, () -> new AdaptedWeekResult(AdaptedWeekResult.Estado.PUBLICABLE,
                AdaptedWeekResult.Motivo.SIN_ORDEN, List.of(), GenerationSource.CONSERVED, ConservationReport.vacio(),
                List.of(), List.of(NoFactibleReason.SEMANA_NO_VALIDA)));
        var nf = AdaptedWeekResult.noFactible(AdaptedWeekResult.Motivo.RESPALDO_IA, ConservationReport.vacio(),
                List.of("x"), List.of(NoFactibleReason.DOLOR_SIN_RESOLUCION_SEGURA));
        assertEquals(List.of(NoFactibleReason.DOLOR_SIN_RESOLUCION_SEGURA), nf.codigos());
        assertEquals(List.of(), AdaptedWeekResult.noFactible(AdaptedWeekResult.Motivo.RESPALDO_IA,
                ConservationReport.vacio(), List.of("x")).codigos(), "la forma anterior sigue valiendo");
    }

    @Test
    @DisplayName("catalogo: cada codigo tiene texto propio, neutro y sin identificadores internos")
    void catalogo() {
        for (var r : NoFactibleReason.values()) {
            assertNotNull(r.texto());
            assertFalse(r.texto().matches(".*\\d{3,}.*"), r + " no expone ids");
            assertFalse(r.texto().contains("V8C") || r.texto().contains("SUST-SEG") || r.texto().contains("P-03"),
                    r + " no expone reglas internas");
            assertEquals(r, NoFactibleReason.of(r.name()));
        }
        assertNull(NoFactibleReason.of("NO_EXISTE"));
    }
}
