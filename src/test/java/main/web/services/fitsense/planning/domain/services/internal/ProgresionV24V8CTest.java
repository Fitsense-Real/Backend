package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalConfigurationService;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.infrastructure.validation.StrictValidationAdapter;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * V35 · V24 Y V8C CON DOSIS AUTORIZADAS, AISLADAS DE LA GENERACION.
 * <p>
 * {@code GAIN_MUSCLE} (4D: dosis exacta 3x10 estructural, 2x10 relleno). Con el
 * productor, V8C y la validacion estricta reales: dominio puro, sin Spring, sin
 * base de datos y sin Replicate. Lo que V31 + V32 dan en produccion
 * ({@code reduccionAutorizada}, {@code dosisConLinaje}) se pasa aqui a mano; su
 * recorrido se prueba en {@code ProgresionIT}.
 */
@DisplayName("V35 · V24 + V8C: solo la dosis autorizada y en rango; la generacion, intacta")
class ProgresionV24V8CTest {

    private static final long USUARIO = 9L;
    private static final LocalDate LUNES = LocalDate.of(2026, 11, 2);
    private static final int SESIONES = 3;
    private static final int DIVISOR = 30;
    private static final SessionDurationEstimator ESTIMADOR = new SessionDurationEstimator();
    private static final long S0 = 701L;
    /** El primer estructural del lunes. */
    private static final long EJ = 100L;

    private final AdaptedWeekProducer conProgresion = new AdaptedWeekProducer(
            new SessionFixApplier(), new ConservationValidator(), estricta(), true);
    private final AdaptedWeekProducer sinProgresion = new AdaptedWeekProducer(
            new SessionFixApplier(), new ConservationValidator(), estricta());

    // =======================================================================
    // Generacion: V24 original
    // =======================================================================

    @Test
    @DisplayName("generacion: 3x11 en un estructural sigue rechazado por V24 (sin informe de conservacion)")
    void generacionRechazaDosisNoExacta() {
        var semana = semanaNueva(Map.of(EJ, (short) 11), 6);
        var validador = new PlanDraftValidator(ESTIMADOR, SlotCoverageEnforcement.ENFORCE_AI_ONLY,
                DoseEnforcement.ENFORCE);
        var e = assertThrows(InvalidPlanDraftException.class, () -> validador.validate(
                new PlanDraft(GenerationSource.RULE_ENGINE, null, "g", null, "r", semana),
                contexto(PlanAdjustment.none(), medida(Map.of(), 6, Set.of(), Map.of())), DIVISOR));
        assertTrue(e.problems().stream().anyMatch(p -> p.startsWith("V24: el ejercicio " + EJ + " ")), e.problems().toString());
    }

    @Test
    @DisplayName("generacion: aunque el informe trajera autorizaciones, la sobrecarga de 3 argumentos no las ve")
    void generacionSinPermisos() {
        // La generacion valida con validate(draft, ctx, divisor): ConservationReport.vacio().
        assertTrue(ConservationReport.vacio().dosisAutorizadas().isEmpty());
        assertTrue(ConservationReport.vacio().autorizacionDe(LUNES, ej(EJ, 3, 11)).isEmpty());
    }

    // =======================================================================
    // Copia: V24 rechaza lo no autorizado y acepta lo heredado con linaje
    // =======================================================================

    @Test
    @DisplayName("copia de un 3x11 SIN linaje: V24 la rechaza (regla original)")
    void copiaSinLinajeRechazada() {
        var r = conProgresion.produce(contexto(PlanAdjustment.none(), medida(Map.of(EJ, (short) 11), 6, Set.of(),
                Map.of())), copia(Map.of(EJ, (short) 11), 6), List.of(), elegibles());
        assertFalse(r.publicable());
        assertTrue(r.problemas().stream().anyMatch(p -> p.startsWith("V24: el ejercicio " + EJ + " ")), r.problemas().toString());
    }

    @Test
    @DisplayName("copia de un 3x11 con linaje (paso anterior en V32): se publica como HEREDADA")
    void copiaConLinajePublicada() {
        var r = conProgresion.produce(contexto(PlanAdjustment.none(), medida(Map.of(EJ, (short) 11), 6, Set.of(),
                Map.of(S0, Set.of(EJ)))), copia(Map.of(EJ, (short) 11), 6), List.of(), elegibles());
        assertTrue(r.publicable(), r.problemas().toString());
        assertEquals(Optional.of(DosisAutorizada.Origen.HEREDADA),
                r.conservacion().autorizacionDe(LUNES.plusWeeks(1), ej(EJ, 3, 11)));
    }

    @Test
    @DisplayName("con la progresion apagada no hay herencia: la misma copia la rechaza V24")
    void apagadoNoHereda() {
        var r = sinProgresion.produce(contexto(PlanAdjustment.none(), medida(Map.of(EJ, (short) 11), 6, Set.of(),
                Map.of(S0, Set.of(EJ)))), copia(Map.of(EJ, (short) 11), 6), List.of(), elegibles());
        assertFalse(r.publicable());
        assertTrue(r.conservacion().dosisAutorizadas().isEmpty());
    }

    // =======================================================================
    // Paso del ciclo
    // =======================================================================

    @Test
    @DisplayName("paso autorizado 3x10 -> 3x11: V8C lo admite, V24 lo acepta y queda como progresada")
    void pasoAutorizado() {
        var ctx = contexto(PlanAdjustment.none(), medida(Map.of(), 6, Set.of(), Map.of()));
        var paso = paso(PasoProgresion.Tipo.PROGRESION, Map.of(EJ, (short) 11), 6, 3, 11);
        var r = conProgresion.produce(ctx, conPaso(paso), List.of(), elegibles(), ExclusionesDeSeguridad.ninguna(), paso);
        assertTrue(r.publicable(), r.problemas().toString());
        assertEquals(List.of(S0), r.conservacion().progresadas());
        assertEquals(1, r.conservacion().pasos().size());
        assertFalse(r.conservacion().conservadas().contains(S0));
        assertEquals(Optional.of(DosisAutorizada.Origen.PASO),
                r.conservacion().autorizacionDe(LUNES.plusWeeks(1), ej(EJ, 3, 11)));
    }

    @Test
    @DisplayName("progresion arbitraria (3x11 sin paso declarado): V8C la rechaza")
    void progresionArbitraria() {
        var ctx = contexto(PlanAdjustment.none(), medida(Map.of(), 6, Set.of(), Map.of()));
        var r = conProgresion.produce(ctx, copia(Map.of(EJ, (short) 11), 6), List.of(), elegibles());
        assertFalse(r.publicable());
        assertTrue(r.problemas().stream().anyMatch(p -> p.startsWith("V8C: la sesion " + S0)
                && p.contains("cambia la dosis")), r.problemas().toString());
    }

    @Test
    @DisplayName("paso fuera de rango (3x13 > 10+2): V8C lo admite como paso y V24 lo rechaza")
    void pasoFueraDeRango() {
        var ctx = contexto(PlanAdjustment.none(), medida(Map.of(), 6, Set.of(), Map.of()));
        var paso = paso(PasoProgresion.Tipo.PROGRESION, Map.of(EJ, (short) 13), 6, 3, 13);
        var r = conProgresion.produce(ctx, conPaso(paso), List.of(), elegibles(), ExclusionesDeSeguridad.ninguna(), paso);
        assertFalse(r.publicable());
        assertTrue(r.problemas().stream().anyMatch(p -> p.startsWith("V24: el ejercicio " + EJ + " ")), r.problemas().toString());
    }

    @Test
    @DisplayName("estructural de GAIN_MUSCLE con +1 serie (4x10): fuera de D-P3, V24 lo rechaza")
    void estructuralNoSubeSeries() {
        var ctx = contexto(PlanAdjustment.none(), medida(Map.of(), 6, Set.of(), Map.of()));
        var semana = semanaNueva(Map.of(), 6);
        var lista = new ArrayList<>(semana.get(0).exercises());
        lista.set(0, ej(EJ, 4, 10));
        var contenido = new PlanDraft.DraftWorkout(semana.get(0).scheduledDate(), semana.get(0).focus(),
                semana.get(0).name(), semana.get(0).expectedDurationMinutes() + 3, lista);
        var paso = new PasoProgresion(S0, PasoProgresion.Tipo.PROGRESION, EJ, 0, null, null, 0, 1, 0, 0, null, contenido);
        var r = conProgresion.produce(ctx, conPaso(paso), List.of(), elegibles(), ExclusionesDeSeguridad.ninguna(), paso);
        assertFalse(r.publicable());
        assertTrue(r.problemas().stream().anyMatch(p -> p.startsWith("V24: el ejercicio " + EJ + " ")), r.problemas().toString());
    }

    @Test
    @DisplayName("un paso que cambia dos ejercicios no es estrecho: V8C lo rechaza")
    void pasoDeDosEjercicios() {
        var ctx = contexto(PlanAdjustment.none(), medida(Map.of(), 6, Set.of(), Map.of()));
        var paso = paso(PasoProgresion.Tipo.PROGRESION, Map.of(EJ, (short) 11, EJ + 1, (short) 11), 6, 3, 11);
        var r = conProgresion.produce(ctx, conPaso(paso), List.of(), elegibles(), ExclusionesDeSeguridad.ninguna(), paso);
        assertFalse(r.publicable());
        assertTrue(r.problemas().stream().anyMatch(p -> p.contains("paso de progresion de la sesion " + S0)),
                r.problemas().toString());
    }

    @Test
    @DisplayName("la derivada no coincide con el paso propuesto: V8C la rechaza")
    void derivadaDistintaDelPaso() {
        var ctx = contexto(PlanAdjustment.none(), medida(Map.of(), 6, Set.of(), Map.of()));
        var paso = paso(PasoProgresion.Tipo.PROGRESION, Map.of(EJ, (short) 11), 6, 3, 11);
        var r = conProgresion.produce(ctx, copia(Map.of(EJ, (short) 12), 6), List.of(), elegibles(),
                ExclusionesDeSeguridad.ninguna(), paso);
        assertFalse(r.publicable());
        assertTrue(r.problemas().stream().anyMatch(p -> p.contains("no coincide exactamente con el paso")),
                r.problemas().toString());
    }

    @Test
    @DisplayName("con una orden de adaptar no se acepta un paso en el mismo ciclo")
    void unaModificacionPorCiclo() {
        var ctx = contexto(orden(TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION, S0 + 1),
                medida(Map.of(), 6, Set.of(), Map.of()));
        var paso = paso(PasoProgresion.Tipo.PROGRESION, Map.of(EJ, (short) 11), 6, 3, 11);
        assertThrows(IllegalArgumentException.class, () -> conProgresion.produce(ctx, conPaso(paso), List.of(),
                elegibles(), ExclusionesDeSeguridad.ninguna(), paso));
    }

    // =======================================================================
    // La IA no sube dosis
    // =======================================================================

    @Test
    @DisplayName("la IA sube la dosis de una sesion senalada (aunque haya linaje): V8C la rechaza")
    void iaNoSubeDosis() {
        var ctx = contexto(orden(TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION, S0),
                medida(Map.of(EJ, (short) 11), 6, Set.of(), Map.of(S0, Set.of(EJ))));
        var lista = new ArrayList<>(ejercicios(0, 6, Map.of(EJ, (short) 12)));
        var correccion = new CorrectedSession(S0, LUNES.plusWeeks(1), minutos(lista), lista, "sube");
        var r = conProgresion.produce(ctx, copia(Map.of(EJ, (short) 11), 6), List.of(correccion), elegibles());
        assertFalse(r.publicable());
        assertTrue(r.problemas().stream().anyMatch(p -> p.contains("La IA no puede subir dosis")), r.problemas().toString());
    }

    // =======================================================================
    // D-P7: una sesion acortada bajo el piso de V20 que progresa
    // =======================================================================

    @Test
    @DisplayName("D-P7: la acortada (5 colocaciones, bajo el piso) progresa y mantiene la exencion de V20")
    void dp7PasoSobreAcortada() {
        assertTrue(minutos(ejercicios(0, 5, Map.of())) < PlanDraftValidator.minimumSessionMinutes(
                contexto(PlanAdjustment.none(), medida(Map.of(), 5, Set.of(S0), Map.of()))), "premisa: bajo el piso");
        var ctx = contexto(PlanAdjustment.none(), medida(Map.of(), 5, Set.of(S0), Map.of()));
        var paso = paso(PasoProgresion.Tipo.PROGRESION, Map.of(EJ, (short) 11), 5, 3, 11);
        var r = conProgresion.produce(ctx, conPaso(paso, 5), List.of(), elegibles(), ExclusionesDeSeguridad.ninguna(), paso);
        assertTrue(r.publicable(), r.problemas().toString());
        assertTrue(r.conservacion().fechasConDuracionReducible().contains(LUNES.plusWeeks(1)));
    }

    @Test
    @DisplayName("D-P7: el mismo paso sin reduccion autorizada en el linaje: V20 lo rechaza")
    void dp7SinReduccionRigeV20() {
        var ctx = contexto(PlanAdjustment.none(), medida(Map.of(), 5, Set.of(), Map.of()));
        var paso = paso(PasoProgresion.Tipo.PROGRESION, Map.of(EJ, (short) 11), 5, 3, 11);
        var r = conProgresion.produce(ctx, conPaso(paso, 5), List.of(), elegibles(), ExclusionesDeSeguridad.ninguna(), paso);
        assertFalse(r.publicable());
        assertTrue(r.problemas().stream().anyMatch(p -> p.startsWith("V20")), r.problemas().toString());
    }

    @Test
    @DisplayName("D-P7: una vuelta atras no hereda la exencion")
    void dp7VueltaAtrasNoHereda() {
        var ctx = contexto(PlanAdjustment.none(), medida(Map.of(EJ, (short) 11), 5, Set.of(S0), Map.of(S0, Set.of(EJ))));
        var semana = semanaNueva(Map.of(), 5);
        var contenido = semana.get(0);
        var paso = new PasoProgresion(S0, PasoProgresion.Tipo.VUELTA_ATRAS, EJ, 0, null, null, 31, 30, 0, 0, null,
                contenido);
        var r = conProgresion.produce(ctx, conPaso(paso, 5), List.of(), elegibles(), ExclusionesDeSeguridad.ninguna(), paso);
        assertFalse(r.conservacion().fechasConDuracionReducible().contains(LUNES.plusWeeks(1)));
    }

    // =======================================================================
    // Material
    // =======================================================================

    private static StrictValidationAdapter estricta() {
        var configuracion = mock(ExternalConfigurationService.class);
        when(configuracion.durationToRepsDivisor()).thenReturn(DIVISOR);
        return new StrictValidationAdapter(new PlanDraftValidator(ESTIMADOR, SlotCoverageEnforcement.ENFORCE_AI_ONLY,
                DoseEnforcement.ENFORCE), configuracion);
    }

    private static PasoProgresion paso(PasoProgresion.Tipo tipo, Map<Long, Short> reps, int colocacionesLunes,
                                       int series, int repsEj) {
        var semana = semanaNueva(reps, colocacionesLunes);
        return new PasoProgresion(S0, tipo, EJ, 0, new PasoProgresion.Dosis((short) 3, (short) 10, null, (short) 90),
                new PasoProgresion.Dosis((short) series, (short) repsEj, null, (short) 90), 1, 2, 1, 2, null,
                semana.get(0));
    }

    private static WeekDerivation conPaso(PasoProgresion paso) {
        return conPaso(paso, 6);
    }

    private static WeekDerivation conPaso(PasoProgresion paso, int colocacionesLunes) {
        var base = copia(Map.of(), colocacionesLunes);
        return new WeekDerivation(base.workouts().stream()
                .map(d -> d.sourcePlannedWorkoutId().equals(paso.origen())
                        ? new DerivedWorkout(d.sourcePlannedWorkoutId(), paso.contenido()) : d).toList(), List.of());
    }

    private static WeekDerivation copia(Map<Long, Short> repsLunes, int colocacionesLunes) {
        var semana = semanaNueva(repsLunes, colocacionesLunes);
        var derivadas = new ArrayList<DerivedWorkout>();
        for (int i = 0; i < SESIONES; i++) derivadas.add(new DerivedWorkout(S0 + i, semana.get(i)));
        return new WeekDerivation(derivadas, List.of());
    }

    private static List<PlanDraft.DraftWorkout> semanaNueva(Map<Long, Short> repsLunes, int colocacionesLunes) {
        var semana = new ArrayList<PlanDraft.DraftWorkout>();
        for (int i = 0; i < SESIONES; i++) {
            var lista = ejercicios(i, i == 0 ? colocacionesLunes : 6, i == 0 ? repsLunes : Map.of());
            semana.add(new PlanDraft.DraftWorkout(dia(i).plusWeeks(1), WorkoutFocus.FULL_BODY, "Sesion " + (i + 1),
                    i == 0 && !repsLunes.isEmpty() ? Math.max(minutos(ejercicios(i, colocacionesLunes, Map.of())),
                            minutos(lista)) : minutos(lista), lista));
        }
        return semana;
    }

    private static PreviousWeekSummary medida(Map<Long, Short> repsLunes, int colocacionesLunes,
                                              Set<Long> reduccion, Map<Long, Set<Long>> linaje) {
        var sesiones = new ArrayList<PreviousWeekSummary.WorkoutOutcome>();
        var prescripciones = new ArrayList<PreviousWeekSummary.PrescriptionOutcome>();
        for (int i = 0; i < SESIONES; i++) {
            var lista = ejercicios(i, i == 0 ? colocacionesLunes : 6, i == 0 ? repsLunes : Map.of());
            int min = i == 0 && !repsLunes.isEmpty()
                    ? Math.max(minutos(ejercicios(i, colocacionesLunes, Map.of())), minutos(lista)) : minutos(lista);
            sesiones.add(new PreviousWeekSummary.WorkoutOutcome(S0 + i, dia(i), "FULL_BODY", "COMPLETED", null, true,
                    "COMPLETED", (short) 6, new BigDecimal("100.00"), "Sesion " + (i + 1), (short) min));
            for (var e : lista)
                prescripciones.add(new PreviousWeekSummary.PrescriptionOutcome(S0 + i, dia(i), e.exerciseId(),
                        "ejercicio " + e.exerciseId(), "SETS_REPS", e.plannedSets(), e.plannedReps(), null, null,
                        e.plannedSets(), e.plannedSets() * e.plannedReps(), null, null, new BigDecimal("100.00"),
                        "COMPLETED", null, "COMPLETED", e.restSeconds()));
        }
        return new PreviousWeekSummary(new BigDecimal("100.00"), new BigDecimal("6.0"), null, Map.of(),
                sesiones, prescripciones, List.of(), reduccion, linaje);
    }

    private static List<PlanDraft.DraftExercise> ejercicios(int sesion, int colocaciones, Map<Long, Short> reps) {
        var lista = new ArrayList<PlanDraft.DraftExercise>();
        for (int j = 0; j < colocaciones; j++) {
            long id = 100L + sesion * 10L + j;
            lista.add(ej(id, j >= 4 ? 2 : 3, reps.getOrDefault(id, (short) 10)));
        }
        return List.copyOf(lista);
    }

    private static PlanDraft.DraftExercise ej(long id, int series, int reps) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS, (short) series, (short) reps, null, null,
                (short) 90, null);
    }

    private static LocalDate dia(int i) {
        return LUNES.plusDays(i * 2L);
    }

    private static int minutos(List<PlanDraft.DraftExercise> lista) {
        return ESTIMADOR.estimateMinutes(new PlanDraft.DraftWorkout(LUNES, WorkoutFocus.FULL_BODY, "m", 1, lista),
                parametros());
    }

    private static final List<String> PATRONES = List.of("KNEE_DOMINANT", "HORIZONTAL_PUSH", "HORIZONTAL_PULL",
            "HIP_HINGE", "ISOMETRIC_HOLD", "CARRY");
    private static final List<String> PARTES = List.of("upper legs", "chest", "back", "upper legs", "shoulders",
            "chest");

    private static Map<Long, CandidateExercise> elegibles() {
        var porId = new LinkedHashMap<Long, CandidateExercise>();
        for (int s = 0; s < SESIONES; s++)
            for (int j = 0; j < PATRONES.size(); j++) {
                long id = 100L + s * 10L + j;
                porId.put(id, new CandidateExercise(id, "ejercicio " + id, PARTES.get(j), "body weight", 1,
                        PrescriptionType.SETS_REPS, null, PATRONES.get(j)));
            }
        return porId;
    }

    private static PrescriptionParams parametros() {
        return new PrescriptionParams(null, null, null, null, null, null, null, null, null, null);
    }

    private static PlanAdjustment orden(TipoIntervencion intervencion, long sesion) {
        return new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null, 0.0, 0.0, null, null, null,
                "causa", null, null, List.of(new PlanAdjustment.TargetedSession(sesion, intervencion)));
    }

    private static PlanGenerationContext contexto(PlanAdjustment ajuste, PreviousWeekSummary previa) {
        return new PlanGenerationContext(USUARIO, (short) 2, LUNES.plusWeeks(1), LUNES.plusWeeks(1).plusDays(6),
                new PlanningProfile(USUARIO, 30, "MALE", new BigDecimal("175.0"), new BigDecimal("72.0"),
                        new BigDecimal("75.0"), "BEGINNER", "GAIN_MUSCLE", "Ganar masa", "Ninguna", "HOME", SESIONES,
                        List.of((short) 1, (short) 3, (short) 5), 45, List.of("body weight", "dumbbell", "band"),
                        List.of(), SafetyProfile.Declaradas.sinLimitaciones()),
                ajuste, previa, List.copyOf(elegibles().values()), SafetyProfile.none(), parametros());
    }
}
