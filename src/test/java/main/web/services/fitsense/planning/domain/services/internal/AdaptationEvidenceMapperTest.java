package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptationAttempt;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptationDecision;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptedWeekResult;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdjustmentType;
import main.web.services.fitsense.planning.domain.model.valueobjects.ConservationReport;
import main.web.services.fitsense.planning.domain.model.valueobjects.DerivedWorkout;
import main.web.services.fitsense.planning.domain.model.valueobjects.GenerationSource;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * La traduccion a evidencia de V32.
 * <p>
 * Lo que vigilan estas pruebas es una distincion que la base de datos ya
 * impone y que es facil de perder: una sesion SENALADA que no cambio es SIN
 * EFECTO, no conservada. «Conservada» es la que nadie senalo.
 */
class AdaptationEvidenceMapperTest {

    private static final LocalDate L2 = LocalDate.of(2026, 10, 19);

    private final AdaptationEvidenceMapper mapper = new AdaptationEvidenceMapper();

    // =======================================================================
    // 1 · Respaldo con una sesion senalada: el caso que la base rechazo
    // =======================================================================

    @Test
    @DisplayName("Respaldo: la senalada va a SIN EFECTO, no a conservadas")
    void respaldoConSenalada() {
        // El productor valido la continuidad con una orden VACIA, asi que su
        // informe clasifica las tres como conservadas, incluida la 102.
        var informe = new ConservationReport(List.of(), List.of(), List.of(),
                List.of(101L, 102L, 103L), Set.of());
        var resultado = publicable(AdaptedWeekResult.Motivo.RESPALDO_IA,
                GenerationSource.CONSERVED, informe);

        var fila = mapper.map(resultado, senalando(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR),
                AdaptationDecision.ADAPT_LOCALIZED, hechos(2L));

        assertEquals(AdaptationAttempt.Status.RESPALDO, fila.status());
        assertEquals(List.of(102L), fila.targetedSessions());
        assertEquals(List.of(), fila.appliedSessions(), "un respaldo no aplica nada");
        assertEquals(List.of(102L), fila.noEffectSessions(),
                "la senalada no cambio: eso es sin efecto");
        assertEquals(List.of(101L, 103L), fila.conservedSessions(),
                "conservadas son las que NADIE senalo");
        assertEquals(GenerationSource.CONSERVED, fila.generationSource());
    }

    @Test
    @DisplayName("Las senaladas salen de la ORDEN, no del informe de continuidad")
    void lasSenaladasSalenDeLaOrden() {
        // El informe del respaldo no sabe nada de senaladas: tiene las listas
        // de adaptacion vacias. Si se dedujeran de ahi, saldria cero.
        var informe = new ConservationReport(List.of(), List.of(), List.of(),
                List.of(101L, 102L, 103L), Set.of());
        var resultado = publicable(AdaptedWeekResult.Motivo.RESPALDO_IA,
                GenerationSource.CONSERVED, informe);

        var fila = mapper.map(resultado, senalando(102L, TipoIntervencion.SEGURIDAD_ANTE_DOLOR),
                AdaptationDecision.ADAPT_LOCALIZED, hechos(2L));

        assertFalse(fila.targetedSessions().isEmpty(),
                "deducirlas del informe daria cero y la evidencia diria que no se ordeno nada");
        assertEquals(List.of("SEGURIDAD_ANTE_DOLOR"), fila.targetedInterventions());
    }

    // =======================================================================
    // 2 · Semana sin orden
    // =======================================================================

    @Test
    @DisplayName("Sin orden: todas conservadas, ninguna senalada, estado SIN_ORDEN")
    void sinOrden() {
        var informe = new ConservationReport(List.of(), List.of(), List.of(),
                List.of(101L, 102L, 103L), Set.of());
        var resultado = publicable(AdaptedWeekResult.Motivo.SIN_ORDEN,
                GenerationSource.CONSERVED, informe);

        var fila = mapper.map(resultado, PlanAdjustment.none(),
                AdaptationDecision.INSUFFICIENT_DATA, hechos(2L));

        assertEquals(AdaptationAttempt.Status.SIN_ORDEN, fila.status());
        assertEquals(List.of(), fila.targetedSessions());
        assertEquals(List.of(101L, 102L, 103L), fila.conservedSessions());
        assertEquals(List.of(), fila.noEffectSessions());
    }

    // =======================================================================
    // 3 · Adaptacion parcialmente efectiva
    // =======================================================================

    @Test
    @DisplayName("Parcialmente efectiva: una aplicada, la otra sin efecto, el resto conservadas")
    void parcialmenteEfectiva() {
        var informe = new ConservationReport(List.of(), List.of(101L), List.of(102L),
                List.of(103L), Set.of());
        var resultado = publicable(AdaptedWeekResult.Motivo.CORRECCION_IA,
                GenerationSource.AI, informe);

        var orden = new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null,
                0.0, 0.0, null, null, null, "baja adherencia", null, null,
                List.of(new PlanAdjustment.TargetedSession(101L, TipoIntervencion.REPROGRAMAR_DIAS),
                        new PlanAdjustment.TargetedSession(102L,
                                TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA)));

        var fila = mapper.map(resultado, orden, AdaptationDecision.ADAPT_EXPANDED, hechos(2L));

        assertEquals(AdaptationAttempt.Status.APLICADA, fila.status());
        assertEquals(List.of(101L), fila.appliedSessions());
        assertEquals(List.of(102L), fila.noEffectSessions(),
                "senalada y no aplicada: sin efecto, se calcula restando sobre la orden");
        assertEquals(List.of(103L), fila.conservedSessions());
        assertEquals(GenerationSource.AI, fila.generationSource());
    }

    // =======================================================================
    // 4 · Rechazo sin plan publicado
    // =======================================================================

    @Test
    @DisplayName("Rechazada: ninguna sesion con desenlace, aunque V8C aplicara una")
    void rechazadaSinPlan() {
        // V8C encontro una correccion local valida en la 101. Pero la 102 tumbo
        // la semana, no se publico nada, y al participante no le llego ninguna.
        var informe = new ConservationReport(List.of("V24: dosis incompatible en la 102"),
                List.of(101L), List.of(), List.of(103L), Set.of());
        var resultado = AdaptedWeekResult.noFactible(AdaptedWeekResult.Motivo.CORRECCION_IA,
                informe, List.of("V24: dosis incompatible en la 102"));

        var orden = new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null,
                0.0, 0.0, null, null, null, "baja adherencia", null, null,
                List.of(new PlanAdjustment.TargetedSession(101L, TipoIntervencion.REPROGRAMAR_DIAS),
                        new PlanAdjustment.TargetedSession(102L,
                                TipoIntervencion.ACORTAR_O_REORGANIZAR)));

        var fila = mapper.map(resultado, orden, AdaptationDecision.ADAPT_LOCALIZED, sinPlan());

        assertEquals(AdaptationAttempt.Status.RECHAZADA, fila.status());
        assertNull(fila.resultingPlanId());
        assertNull(fila.generationSource());
        assertEquals(List.of(), fila.appliedSessions(),
                "V8C la dio por aplicada, pero no se publico: no le llego al participante");
        assertEquals(List.of(), fila.noEffectSessions());
        assertEquals(List.of(), fila.conservedSessions());
        assertTrue(fila.rejectionReason().contains("V24"));
        assertEquals(List.of(101L, 102L), fila.targetedSessions(),
                "lo senalado SI se registra: es lo que se intento");
    }

    @Test
    @DisplayName("No factible por dolor sin IA: NO_FACTIBLE con su motivo")
    void noFactiblePorDolor() {
        var resultado = AdaptedWeekResult.noFactible(AdaptedWeekResult.Motivo.RESPALDO_IA,
                ConservationReport.vacio(),
                List.of("Sin respuesta de la IA no se puede resolver el dolor de [102]"));

        var fila = mapper.map(resultado, senalando(102L, TipoIntervencion.SEGURIDAD_ANTE_DOLOR),
                AdaptationDecision.ADAPT_LOCALIZED, sinPlan());

        assertEquals(AdaptationAttempt.Status.NO_FACTIBLE, fila.status());
        assertTrue(fila.rejectionReason().contains("dolor"));
    }

    // =======================================================================
    // Las invariantes de V32, en Java
    // =======================================================================

    @Test
    @DisplayName("Una conservada que ademas esta senalada no se puede construir")
    void conservadaYSenalada() {
        var e = assertThrows(IllegalArgumentException.class, () -> new AdaptationAttempt(
                1L, 1L, 1L, L2, 2L, AdaptationDecision.ADAPT_LOCALIZED,
                AdaptationAttempt.Status.RESPALDO, GenerationSource.CONSERVED, (short) 1,
                List.of(102L), List.of("ACORTAR_O_REORGANIZAR"),
                List.of(), List.of(), List.of(102L), null, null, null, null));

        assertTrue(e.getMessage().contains("SIN EFECTO"), e::getMessage);
    }

    @Test
    @DisplayName("Un RESPALDO no puede declarar origen AI")
    void respaldoConOrigenAI() {
        var e = assertThrows(IllegalArgumentException.class, () -> new AdaptationAttempt(
                1L, 1L, 1L, L2, 2L, AdaptationDecision.ADAPT_LOCALIZED,
                AdaptationAttempt.Status.RESPALDO, GenerationSource.AI, (short) 1,
                List.of(102L), List.of("ACORTAR_O_REORGANIZAR"),
                List.of(), List.of(102L), List.of(), null, null, null, null));

        assertTrue(e.getMessage().contains("CONSERVED"), e::getMessage);
    }

    @Test
    @DisplayName("Un estado sin plan no puede llevar plan, y al reves")
    void planSegunEstado() {
        assertThrows(IllegalArgumentException.class, () -> new AdaptationAttempt(
                1L, 1L, 1L, L2, 2L, AdaptationDecision.ADAPT_LOCALIZED,
                AdaptationAttempt.Status.RECHAZADA, GenerationSource.AI, (short) 1,
                List.of(102L), List.of("ACORTAR_O_REORGANIZAR"),
                List.of(), List.of(), List.of(), null, "motivo", null, null));

        assertThrows(IllegalArgumentException.class, () -> new AdaptationAttempt(
                1L, 1L, 1L, L2, null, AdaptationDecision.NO_CHANGE,
                AdaptationAttempt.Status.SIN_ORDEN, null, (short) 0,
                List.of(), List.of(), List.of(), List.of(), List.of(),
                null, null, null, null));
    }

    @Test
    @DisplayName("Una decision que no concuerda con el camino del productor es un error")
    void decisionIncoherente() {
        var resultado = publicable(AdaptedWeekResult.Motivo.SIN_ORDEN,
                GenerationSource.CONSERVED, ConservationReport.vacio());

        var e = assertThrows(IllegalArgumentException.class,
                () -> mapper.map(resultado, senalando(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR),
                        AdaptationDecision.ADAPT_LOCALIZED, hechos(2L)));

        assertTrue(e.getMessage().contains("no concuerda"), e::getMessage);
    }

    @Test
    @DisplayName("Un identificador senalado repetido no se puede construir")
    void senaladaRepetida() {
        var e = assertThrows(IllegalArgumentException.class, () -> new AdaptationAttempt(
                1L, 1L, 1L, L2, 2L, AdaptationDecision.ADAPT_LOCALIZED,
                AdaptationAttempt.Status.RESPALDO, GenerationSource.CONSERVED, (short) 1,
                List.of(102L, 102L), List.of("ACORTAR_O_REORGANIZAR", "ACORTAR_O_REORGANIZAR"),
                List.of(), List.of(102L), List.of(), null, null, null, null));

        assertTrue(e.getMessage().contains("repetidos"), e::getMessage);
    }

    @Test
    @DisplayName("El estado del productor y la publicacion tienen que concordar")
    void estadoYPublicacionConcuerdan() {
        // NO_FACTIBLE con un plan publicado: alguien publico lo que el productor
        // habia rechazado. Descartarlo en silencio ocultaria el error.
        var rechazado = AdaptedWeekResult.noFactible(AdaptedWeekResult.Motivo.CORRECCION_IA,
                ConservationReport.vacio(), List.of("V24: dosis"));

        var e = assertThrows(IllegalArgumentException.class,
                () -> mapper.map(rechazado, senalando(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR),
                        AdaptationDecision.ADAPT_LOCALIZED, hechos(2L)));
        assertTrue(e.getMessage().contains("concordar"), e::getMessage);

        // Y al reves: publicable sin plan.
        var publicado = publicable(AdaptedWeekResult.Motivo.RESPALDO_IA,
                GenerationSource.CONSERVED,
                new ConservationReport(List.of(), List.of(), List.of(), List.of(101L), Set.of()));

        var e2 = assertThrows(IllegalArgumentException.class,
                () -> mapper.map(publicado, senalando(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR),
                        AdaptationDecision.ADAPT_LOCALIZED, sinPlan()));
        assertTrue(e2.getMessage().contains("concordar"), e2::getMessage);
    }

    @Test
    @DisplayName("El informe de V8C viaja entero, sin retocarse")
    void elInformeViajaEntero() {
        var informe = new ConservationReport(List.of(), List.of(), List.of(),
                List.of(101L, 102L, 103L), Set.of());
        var resultado = publicable(AdaptedWeekResult.Motivo.RESPALDO_IA,
                GenerationSource.CONSERVED, informe);

        var fila = mapper.map(resultado, senalando(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR),
                AdaptationDecision.ADAPT_LOCALIZED, hechos(2L));

        // El informe sigue diciendo que la 102 se conservo, que es lo que V8C
        // comprobo. La fila dice que estuvo sin efecto, que es lo que paso
        // respecto de la orden. Las dos cosas son ciertas y las dos se guardan.
        assertTrue(resultado.conservacion().conservadas().contains(102L),
                "el informe original no se toca");
        assertFalse(fila.conservedSessions().contains(102L),
                "y la evidencia lo registra respecto de la orden");
        assertEquals("{\"informe\":\"intacto\"}", fila.conservationReportJson());
    }

    // =======================================================================
    // Material
    // =======================================================================

    private static AdaptedWeekResult publicable(AdaptedWeekResult.Motivo motivo,
                                                GenerationSource source,
                                                ConservationReport informe) {
        return new AdaptedWeekResult(AdaptedWeekResult.Estado.PUBLICABLE, motivo,
                List.of(derivada(101L), derivada(102L), derivada(103L)),
                source, informe, List.of());
    }

    private static DerivedWorkout derivada(long origen) {
        return new DerivedWorkout(origen, new PlanDraft.DraftWorkout(L2,
                WorkoutFocus.FULL_BODY, "Cuerpo completo", 45,
                List.of(new PlanDraft.DraftExercise(740L, PrescriptionType.SETS_REPS,
                        (short) 3, (short) 10, null, null, (short) 90, null))));
    }

    // =======================================================================
    // V33 · APLICADA si y SOLO si hay sesiones aplicadas
    // =======================================================================
    //
    // Las tres pruebas de abajo -dos negativas y el camino positivo- son las
    // UNICAS que ejercitan el guardia nuevo. Las cinco construcciones directas
    // que ya habia en este archivo pasan List.of() en aplicadas sin ser
    // APLICADA, asi que demuestran que el guardia no estorba, no que funcione.
    //
    // Cada fixture cumple las otras dieciseis invariantes a proposito, para que
    // la unica razon posible del fallo sea la que la prueba dice. Y por eso se
    // afirma sobre el MENSAJE: si disparara otro guardia antes, el texto no
    // coincidiria y la prueba lo diria.

    @Test
    @DisplayName("V33 · APLICADA sin sesiones aplicadas no se puede construir")
    void aplicadaSinSesionesAplicadas() {
        // Todo lo demas cuadra: ADAPT_LOCALIZED ordena adaptar, la 102 esta
        // senalada con su intervencion, hay plan publicado distinto del de
        // origen, el origen se declara, y la 102 consta SIN EFECTO -que es
        // coherente: se senalo y no cambio-. Lo unico incoherente es afirmar
        // APLICADA sin que ninguna sesion se aplicara.
        var e = assertThrows(IllegalArgumentException.class, () -> new AdaptationAttempt(
                1L, 1L, 1L, L2, 2L, AdaptationDecision.ADAPT_LOCALIZED,
                AdaptationAttempt.Status.APLICADA, GenerationSource.AI, (short) 1,
                List.of(102L), List.of("ACORTAR_O_REORGANIZAR"),
                List.of(), List.of(102L), List.of(), null, null, null, null));

        assertTrue(e.getMessage().contains("APLICADA sin sesiones aplicadas"),
                () -> "fallo por otro motivo: " + e.getMessage());
    }

    @Test
    @DisplayName("V33 · Un RESPALDO con sesiones aplicadas tampoco: solo APLICADA las tiene")
    void respaldoConSesionesAplicadas() {
        // Un respaldo conserva la semana anterior. Si ademas constara con
        // sesiones aplicadas, estaria afirmando que un plan conservado cambio
        // sesiones. El origen va en CONSERVED porque lo exige
        // ck_paa_procedencia_continuidad, y la 101 esta senalada porque lo
        // exige ck_paa_desenlace_subconjunto: asi el unico guardia que puede
        // disparar es el de V33.
        var e = assertThrows(IllegalArgumentException.class, () -> new AdaptationAttempt(
                1L, 1L, 1L, L2, 2L, AdaptationDecision.ADAPT_LOCALIZED,
                AdaptationAttempt.Status.RESPALDO, GenerationSource.CONSERVED, (short) 1,
                List.of(101L), List.of("ACORTAR_O_REORGANIZAR"),
                List.of(101L), List.of(), List.of(), null, null, null, null));

        assertTrue(e.getMessage().contains("solo APLICADA las tiene"),
                () -> "fallo por otro motivo: " + e.getMessage());
    }

    @Test
    @DisplayName("V33 · Y la combinacion valida sigue construyendose")
    void aplicadaConSuSesionSeConstruye() {
        // El contrapunto. Sin esto, las dos de arriba pasarian igual si el
        // guardia estuviera mal escrito y rechazara TODO.
        assertDoesNotThrow(() -> new AdaptationAttempt(
                1L, 1L, 1L, L2, 2L, AdaptationDecision.ADAPT_LOCALIZED,
                AdaptationAttempt.Status.APLICADA, GenerationSource.AI, (short) 1,
                List.of(101L), List.of("ACORTAR_O_REORGANIZAR"),
                List.of(101L), List.of(), List.of(), null, null, null, null));
    }

    private static PlanAdjustment senalando(long id, TipoIntervencion tipo) {
        return new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null,
                0.0, 0.0, null, null, null, "baja adherencia", null, null,
                List.of(new PlanAdjustment.TargetedSession(id, tipo)));
    }

    private static AdaptationEvidenceMapper.PublicationFacts hechos(Long planPublicado) {
        return new AdaptationEvidenceMapper.PublicationFacts(1L, 1L, 1L, L2, planPublicado,
                (short) 1, new BigDecimal("66.67"), new BigDecimal("100.00"),
                "{\"informe\":\"intacto\"}");
    }

    private static AdaptationEvidenceMapper.PublicationFacts sinPlan() {
        return new AdaptationEvidenceMapper.PublicationFacts(1L, 1L, 1L, L2, null,
                (short) 2, new BigDecimal("66.67"), new BigDecimal("100.00"),
                "{\"informe\":\"intacto\"}");
    }
}