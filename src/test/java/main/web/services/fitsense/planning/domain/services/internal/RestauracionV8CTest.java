package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalConfigurationService;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptedWeekResult;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdjustmentType;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.CorrectedSession;
import main.web.services.fitsense.planning.domain.model.valueobjects.DerivedWorkout;
import main.web.services.fitsense.planning.domain.model.valueobjects.ExclusionesDeSeguridad;
import main.web.services.fitsense.planning.domain.model.valueobjects.GenerationSource;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningProfile;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.PreviousWeekSummary;
import main.web.services.fitsense.planning.domain.model.valueobjects.SafetyProfile;
import main.web.services.fitsense.planning.domain.model.valueobjects.WeekDerivation;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.planning.infrastructure.validation.StrictValidationAdapter;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * V35 · V8C ACEPTA UNA RESTAURACION SOLO SI ES IDENTICA A SU REFERENCIA.
 * <p>
 * La semana medida trae el lunes con una dosis ajustada; la referencia es la
 * misma sesion con la dosis anterior. El productor, V8C y la validacion
 * estricta son los reales. Dominio puro: sin Spring, sin base y sin Replicate.
 */
@DisplayName("V35 · V8C y la restauracion: identica, una por ciclo, nunca con una orden de adaptar")
class RestauracionV8CTest {

    private static final long USUARIO = 7L;
    private static final LocalDate LUNES = LocalDate.of(2026, 11, 2);
    private static final int SESIONES = 3;
    private static final int DIVISOR = 30;
    private static final int MINUTOS_SESION = 45;
    private static final SessionDurationEstimator ESTIMADOR = new SessionDurationEstimator();
    private static final long SENALADA = 601L;

    private final AdaptedWeekProducer productor = new AdaptedWeekProducer(
            new SessionFixApplier(), new ConservationValidator(), estricta());

    /** La referencia del lunes: sus seis colocaciones con 12 repeticiones en vez de 10. */
    private static PlanDraft.DraftWorkout referencia(LocalDate dia) {
        var lista = ejercicios(0, 6).stream().map(e -> new PlanDraft.DraftExercise(e.exerciseId(),
                e.prescriptionType(), e.plannedSets(), (short) 12, null, null, e.restSeconds(), null)).toList();
        return sesionCon(0, dia, lista);
    }

    /** La derivacion con el lunes sustituido por {@code lunes}. */
    private static WeekDerivation conLunes(PlanDraft.DraftWorkout lunes) {
        var base = derivacion().workouts();
        var lista = new ArrayList<DerivedWorkout>();
        lista.add(new DerivedWorkout(SENALADA, lunes));
        lista.addAll(base.subList(1, base.size()));
        return new WeekDerivation(lista, List.of());
    }

    @Test
    @DisplayName("restauracion identica: publicable, SIN_ORDEN, el lunes en restauradas y no en conservadas")
    void identicaSePublica() {
        var ref = referencia(LUNES.plusWeeks(1));
        var r = productor.produce(contexto(PlanAdjustment.none(), Set.of()), conLunes(ref), List.of(), elegibles(),
                ExclusionesDeSeguridad.ninguna(), Map.of(SENALADA, ref));
        assertTrue(r.publicable(), r.problemas().toString());
        assertEquals(AdaptedWeekResult.Motivo.SIN_ORDEN, r.motivo());
        assertEquals(List.of(SENALADA), r.conservacion().restauradas());
        assertFalse(r.conservacion().conservadas().contains(SENALADA));
        assertTrue(r.conservacion().aplicadas().isEmpty(), "una restauracion no es una adaptacion aplicada");
    }

    @Test
    @DisplayName("la derivada difiere de la referencia declarada: V8C la rechaza")
    void noIdenticaSeRechaza() {
        var ref = referencia(LUNES.plusWeeks(1));
        var otra = sesionCon(0, LUNES.plusWeeks(1), ejercicios(0, 5));
        var r = productor.produce(contexto(PlanAdjustment.none(), Set.of()), conLunes(otra), List.of(), elegibles(),
                ExclusionesDeSeguridad.ninguna(), Map.of(SENALADA, ref));
        assertFalse(r.publicable());
        assertTrue(r.problemas().stream().anyMatch(p -> p.contains("no coincide exactamente")), r.problemas().toString());
    }

    @Test
    @DisplayName("otro dia que el de la referencia: V8C la rechaza")
    void otroDiaSeRechaza() {
        var ref = referencia(LUNES.plusWeeks(1));
        var r = productor.produce(contexto(PlanAdjustment.none(), Set.of()),
                conLunes(referencia(LUNES.plusWeeks(1).plusDays(1))), List.of(), elegibles(),
                ExclusionesDeSeguridad.ninguna(), Map.of(SENALADA, ref));
        assertFalse(r.publicable());
    }

    @Test
    @DisplayName("dos restauraciones en el mismo ciclo: se rechaza (una modificacion por ciclo)")
    void dosRestauracionesSeRechazan() {
        var lunes = referencia(LUNES.plusWeeks(1));
        var miercolesLista = ejercicios(1, 6).stream().map(e -> new PlanDraft.DraftExercise(e.exerciseId(),
                e.prescriptionType(), e.plannedSets(), (short) 12, null, null, e.restSeconds(), null)).toList();
        var miercoles = sesionCon(1, LUNES.plusDays(2).plusWeeks(1), miercolesLista);
        var base = derivacion().workouts();
        var semana = new WeekDerivation(List.of(new DerivedWorkout(SENALADA, lunes),
                new DerivedWorkout(SENALADA + 1, miercoles), base.get(2)), List.of());
        var r = productor.produce(contexto(PlanAdjustment.none(), Set.of()), semana, List.of(), elegibles(),
                ExclusionesDeSeguridad.ninguna(), Map.of(SENALADA, lunes, SENALADA + 1, miercoles));
        assertFalse(r.publicable());
        assertTrue(r.problemas().stream().anyMatch(p -> p.contains("una por ciclo")), r.problemas().toString());
    }

    @Test
    @DisplayName("con una orden de adaptar no se acepta una restauracion")
    void conOrdenNoSeRestaura() {
        var ref = referencia(LUNES.plusWeeks(1));
        assertThrows(IllegalArgumentException.class, () -> productor.produce(
                contexto(orden(TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION), Set.of()), derivacion(), List.of(),
                elegibles(), ExclusionesDeSeguridad.ninguna(), Map.of(SENALADA + 1, ref)));
    }

    private static StrictValidationAdapter estricta() {
        var configuracion = mock(ExternalConfigurationService.class);
        when(configuracion.durationToRepsDivisor()).thenReturn(DIVISOR);

        var validador = new PlanDraftValidator(ESTIMADOR,
                SlotCoverageEnforcement.ENFORCE_AI_ONLY, DoseEnforcement.ENFORCE);

        return new StrictValidationAdapter(validador, configuracion);
    }

    // =======================================================================
    // La semana medida y su derivacion
    // =======================================================================

    /** Identificador de la sesion i en la semana medida. */
    private static long sesionMedida(int i) {
        return SENALADA + i;
    }

    private static LocalDate diaMedido(int i) {
        return LUNES.plusDays(i * 2L);
    }

    /** La derivada copia cada sesion siete dias despues, con las seis colocaciones. */
    /** Copia identica de la semana medida, siete dias despues: el lunes sigue acortado. */
    private static WeekDerivation derivacion() {
        var derivadas = new ArrayList<DerivedWorkout>();
        for (int i = 0; i < SESIONES; i++)
            derivadas.add(new DerivedWorkout(sesionMedida(i),
                    sesionCon(i, diaMedido(i).plusWeeks(1), ejercicios(i, colocaciones(i)))));
        return new WeekDerivation(List.copyOf(derivadas), List.of());
    }

    /** El lunes viene acortado (5 colocaciones, 30 min, bajo el piso de 31); las demas, completas. */
    private static int colocaciones(int sesion) {
        return 6;
    }

    private static PreviousWeekSummary semanaMedida(Set<Long> reduccionAutorizada) {
        var sesiones = new ArrayList<PreviousWeekSummary.WorkoutOutcome>();
        var prescripciones = new ArrayList<PreviousWeekSummary.PrescriptionOutcome>();

        for (int i = 0; i < SESIONES; i++) {
            var lista = ejercicios(i, colocaciones(i));
            sesiones.add(new PreviousWeekSummary.WorkoutOutcome(
                    sesionMedida(i), diaMedido(i), "FULL_BODY", "COMPLETED", null,
                    true, "COMPLETED", (short) 7, new BigDecimal("100.00"),
                    "FULL_BODY - Sesion " + (i + 1), (short) minutos(lista)));

            for (int j = 0; j < colocaciones(i); j++)
                prescripciones.add(new PreviousWeekSummary.PrescriptionOutcome(
                        sesionMedida(i), diaMedido(i), id(i, j), "ejercicio " + id(i, j),
                        "SETS_REPS", series(j), (short) 10, null, null,
                        series(j), 10 * series(j), null, null,
                        new BigDecimal("100.00"), "COMPLETED", null, "COMPLETED",
                        (short) 90));
        }

        return new PreviousWeekSummary(new BigDecimal("42.00"), new BigDecimal("7.0"),
                null, Map.of(), List.copyOf(sesiones), List.copyOf(prescripciones),
                List.of(), reduccionAutorizada);
    }

    // =======================================================================
    // Material comun
    // =======================================================================

    private static List<PlanDraft.DraftExercise> ejercicios(int sesion, int colocaciones) {
        var lista = new ArrayList<PlanDraft.DraftExercise>();
        for (int j = 0; j < colocaciones; j++)
            lista.add(prescrito(id(sesion, j), esRelleno(j)));
        return List.copyOf(lista);
    }

    /** Los minutos los calcula el estimador, igual que hace el motor de reglas. */
    private static int minutos(List<PlanDraft.DraftExercise> ejercicios) {
        return ESTIMADOR.estimateMinutes(new PlanDraft.DraftWorkout(LUNES,
                WorkoutFocus.FULL_BODY, "medicion", 1, ejercicios), parametros());
    }

    private static PlanDraft.DraftWorkout sesionCon(int indice, LocalDate dia,
                                                    List<PlanDraft.DraftExercise> ejercicios) {
        return new PlanDraft.DraftWorkout(dia, WorkoutFocus.FULL_BODY,
                "FULL_BODY - Sesion " + (indice + 1), minutos(ejercicios), ejercicios);
    }

    private static boolean esRelleno(int posicion) {
        return posicion >= 4;
    }

    private static short series(int posicion) {
        return esRelleno(posicion) ? (short) 2 : (short) 3;
    }

    private static PlanDraft.DraftExercise prescrito(long id, boolean relleno) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS,
                relleno ? (short) 2 : (short) 3, (short) 10, null, null,
                (short) 90, null);
    }

    private static long id(int sesion, int posicion) {
        return 100L + sesion * 10L + posicion;
    }

    /** Cuatro patrones obligatorios y dos que no mapean a ningun slot. */
    private static final List<String> PATRONES = List.of(
            "KNEE_DOMINANT", "HORIZONTAL_PUSH", "HORIZONTAL_PULL", "HIP_HINGE",
            "ISOMETRIC_HOLD", "CARRY");

    /**
     * Partes que satisfacen las dos reglas de V15 en los tres escenarios: al
     * menos dos grupos distintos, y ninguno por encima de la mitad de la sesion
     * ({@code PlanDraftValidator:417}). Ninguna esta en {@code ZONAS_PEQUENAS},
     * asi que la dosis de 4D no cambia.
     */
    private static final List<String> PARTES = List.of(
            "upper legs", "chest", "back", "upper legs", "shoulders", "chest");

    private static Map<Long, CandidateExercise> elegibles() {
        var porId = new LinkedHashMap<Long, CandidateExercise>();
        for (int s = 0; s < SESIONES; s++)
            for (int j = 0; j < PATRONES.size(); j++)
                porId.put(id(s, j), new CandidateExercise(id(s, j), "ejercicio " + id(s, j),
                        PARTES.get(j), "body weight", 1, PrescriptionType.SETS_REPS,
                        null, PATRONES.get(j)));
        return porId;
    }

    /** Todos los componentes nulos: se usan los valores por omision. */
    private static PrescriptionParams parametros() {
        return new PrescriptionParams(null, null, null, null, null, null,
                null, null, null, null);
    }

    /** Una orden que senala el lunes con otra intervencion. */
    private static PlanAdjustment orden(TipoIntervencion intervencion) {
        return new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null,
                0.0, 0.0, null, null, null, "otra causa", null, null,
                List.of(new PlanAdjustment.TargetedSession(SENALADA, intervencion)));
    }

    /**
     * {@code GAIN_MUSCLE} es obligatorio: sin eso V24 ni se mide y el productor
     * no estaria probando la dosis. Sesiones de 45 minutos, para que el piso de
     * V20 sean los 31 del hallazgo real.
     */
    private static PlanGenerationContext contexto(PlanAdjustment ajuste, Set<Long> reduccionAutorizada) {
        return new PlanGenerationContext(USUARIO, (short) 2,
                LUNES.plusWeeks(1), LUNES.plusWeeks(1).plusDays(6),
                perfil(), ajuste, semanaMedida(reduccionAutorizada),
                List.copyOf(elegibles().values()), SafetyProfile.none(), parametros());
    }

    private static PlanningProfile perfil() {
        return new PlanningProfile(USUARIO, 30, "MALE", new BigDecimal("175.0"),
                new BigDecimal("72.0"), new BigDecimal("75.0"), "BEGINNER",
                "GENERAL_FITNESS", "Mejorar la condicion general", "Ninguna",
                "HOME", SESIONES, List.of((short) 1, (short) 3, (short) 5),
                MINUTOS_SESION, List.of("body weight", "dumbbell", "band"), List.of(),
                SafetyProfile.Declaradas.sinLimitaciones());
    }
}
