package main.web.services.fitsense.planning.infrastructure.validation;

import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalConfigurationService;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.ConservationReport;
import main.web.services.fitsense.planning.domain.model.valueobjects.GenerationSource;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningProfile;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.SafetyProfile;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.domain.services.DoseEnforcement;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.domain.services.SlotCoverageEnforcement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Que el camino de adaptacion RECHACE una dosis incompatible con 4D aunque V24
 * global siga en {@code OBSERVE}.
 * <p>
 * Es la condicion que quedo declarada y sin demostrar desde P2-B-1: hasta ahora
 * {@code bloqueaDosis4D()} era una afirmacion del adaptador y nadie comprobaba
 * que el validador estuviera de verdad en modo bloqueante.
 * <p>
 * La violacion elegida es el TOPE DE COLOCACIONES -siete ejercicios en un dia,
 * y el maximo de 4D v2 §5 son seis- porque no depende de la tabla de dosis ni
 * del catalogo: si cambia el reparto por clase, esta prueba sigue valiendo.
 */
class StrictValidationAdapterTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 10, 12);
    private static final long USUARIO = 1L;

    // =======================================================================
    // Lo que esta prueba existe para demostrar
    // =======================================================================

    @Test
    @DisplayName("Con V24 en OBSERVE el validador normal NO reporta la dosis")
    void enObserveNoSeReporta() {
        // El punto de partida: asi se comporta hoy el validador que inyecta
        // Spring. El incumplimiento se registra en el log y se descarta.
        var normal = validador(DoseEnforcement.OBSERVE);

        var problemas = problemasDe(normal, sieteEnUnDia());

        assertTrue(problemas.stream().noneMatch(p -> p.startsWith("V24:")),
                () -> "en OBSERVE la dosis no deberia llegar a los problemas: " + problemas);
    }

    @Test
    @DisplayName("El adaptador SI la rechaza, con la misma semana y el mismo catalogo")
    void elAdaptadorSiLaRechaza() {
        // CONSTRUCTOR DE PRODUCCION, el que usa Spring. Pasarle un validador ya
        // puesto en ENFORCE probaria que un validador en ENFORCE rechaza -que
        // ya sabemos-, no que ESTE adaptador lo configure asi. Y la cobertura
        // 4C va en OBSERVE a proposito: demuestra que la dosis se endurece sin
        // arrastrar lo demas.
        var adaptador = new StrictValidationAdapter(new SessionDurationEstimator(),
                configuracion(30), "OBSERVE");

        var problemas = adaptador.revisar(sieteEnUnDia(), contexto(), ConservationReport.vacio());

        // UNO, no «alguno». Con «hay algun V24» el escenario podria estar
        // incumpliendo tambien el presupuesto de tiempo y la prueba pasaria sin
        // demostrar cual de las dos reglas la rechaza. El perfil usa 60 minutos
        // justamente para que solo quede el tope.
        var problemasV24 = problemas.stream()
                .filter(p -> p.startsWith("V24:"))
                .toList();

        assertEquals(1, problemasV24.size(),
                () -> "el escenario tiene que incumplir SOLO el tope: " + problemasV24);
        assertTrue(problemasV24.get(0).contains("colocaciones"),
                () -> problemasV24.toString());
    }

    @Test
    @DisplayName("Y lo declara: bloqueaDosis4D consulta al validador, no devuelve true a secas")
    void loDeclaraPreguntandoAlValidador() {
        // Por el constructor real: es el unico que demuestra el cableado.
        assertTrue(new StrictValidationAdapter(new SessionDurationEstimator(),
                        configuracion(30), "OBSERVE").bloqueaDosis4D(),
                "el constructor de produccion tiene que dejar la dosis en ENFORCE");

        assertTrue(new StrictValidationAdapter(validador(DoseEnforcement.ENFORCE),
                configuracion(30)).bloqueaDosis4D());

        // Si alguien lo construyera mal, el productor se niega a arrancar. Esa
        // es la unica razon de que el metodo exista.
        assertFalse(new StrictValidationAdapter(validador(DoseEnforcement.OBSERVE),
                configuracion(30)).bloqueaDosis4D());
        assertFalse(new StrictValidationAdapter(validador(DoseEnforcement.OFF),
                configuracion(30)).bloqueaDosis4D());
    }

    // =======================================================================
    // El puerto devuelve, no lanza
    // =======================================================================

    @Test
    @DisplayName("Tres sesiones de un ejercicio no incumplen el tope de colocaciones")
    void noIncumpleElTopeDeColocaciones() {
        // OJO: esta semana NO es valida -cada sesion tiene un ejercicio y el
        // minimo son dos-, y la prueba no pretende que lo sea. Comprueba una
        // sola cosa: que estar por DEBAJO del tope no dispara V24.
        var adaptador = new StrictValidationAdapter(validador(DoseEnforcement.ENFORCE),
                configuracion(30));

        var problemas = adaptador.revisar(unoPorDia(), contexto(), ConservationReport.vacio());

        assertTrue(problemas.stream().noneMatch(p -> p.startsWith("V24:")),
                () -> "estar por debajo del tope no deberia disparar V24: " + problemas);
    }

    @Test
    @DisplayName("No lanza: devuelve los problemas para que el productor los acumule")
    void noLanza() {
        var adaptador = new StrictValidationAdapter(validador(DoseEnforcement.ENFORCE),
                configuracion(30));

        assertDoesNotThrow(() ->
                adaptador.revisar(sieteEnUnDia(), contexto(), ConservationReport.vacio()));
    }

    @Test
    @DisplayName("Un informe nulo se trata como vacio, no revienta")
    void informeNulo() {
        var adaptador = new StrictValidationAdapter(validador(DoseEnforcement.ENFORCE),
                configuracion(30));

        assertDoesNotThrow(() -> adaptador.revisar(unoPorDia(), contexto(), null));
    }

    @Test
    @DisplayName("El divisor se pide en cada llamada, no se cachea")
    void elDivisorSePideCadaVez() {
        // Puede cambiar en caliente. Cachearlo haria que la adaptacion validara
        // con un valor y la generacion inicial con otro.
        var configuracion = configuracion(30);
        var adaptador = new StrictValidationAdapter(validador(DoseEnforcement.ENFORCE),
                configuracion);

        adaptador.revisar(unoPorDia(), contexto(), ConservationReport.vacio());
        adaptador.revisar(unoPorDia(), contexto(), ConservationReport.vacio());

        org.mockito.Mockito.verify(configuracion, org.mockito.Mockito.times(2))
                .durationToRepsDivisor();
    }

    // =======================================================================
    // Material
    // =======================================================================

    private static PlanDraftValidator validador(DoseEnforcement dosis) {
        return new PlanDraftValidator(new SessionDurationEstimator(),
                SlotCoverageEnforcement.OBSERVE, dosis);
    }

    private static ExternalConfigurationService configuracion(int divisor) {
        var doble = mock(ExternalConfigurationService.class);
        when(doble.durationToRepsDivisor()).thenReturn(divisor);
        return doble;
    }

    private static List<String> problemasDe(PlanDraftValidator validador, PlanDraft draft) {
        try {
            validador.validate(draft, contexto(), 30, ConservationReport.vacio());
            return List.of();
        } catch (InvalidPlanDraftException e) {
            return e.problems();
        }
    }

    /** Siete colocaciones en un dia: el maximo de 4D v2 §5 son seis. */
    private static PlanDraft sieteEnUnDia() {
        var ejercicios = new ArrayList<PlanDraft.DraftExercise>();
        for (long id = 1; id <= 7; id++) ejercicios.add(prescrito(id));
        return borrador(List.of(new PlanDraft.DraftWorkout(LUNES, WorkoutFocus.FULL_BODY,
                "FULL_BODY - Sesion 1", 45, List.copyOf(ejercicios))));
    }

    /** Tres sesiones de un ejercicio: no incumple el tope. */
    private static PlanDraft unoPorDia() {
        var sesiones = new ArrayList<PlanDraft.DraftWorkout>();
        for (int i = 0; i < 3; i++)
            sesiones.add(new PlanDraft.DraftWorkout(LUNES.plusDays(i * 2L),
                    WorkoutFocus.FULL_BODY, "FULL_BODY - Sesion " + (i + 1), 45,
                    List.of(prescrito(i + 1L))));
        return borrador(List.copyOf(sesiones));
    }

    private static PlanDraft borrador(List<PlanDraft.DraftWorkout> sesiones) {
        return new PlanDraft(GenerationSource.AI, "gpt-5-nano", "plan de prueba",
                null, "prueba", sesiones);
    }

    /**
     * La dosis que 4D EXIGE para esta colocacion, para que lo unico que pueda
     * incumplirse sea el tope.
     * <p>
     * {@code HORIZONTAL_PUSH} mapea a {@code UPPER_PUSH}, asi que es
     * ESTRUCTURAL, no relleno. {@code Capacidad.HYPERTROPHY} son 3 series, 10
     * repeticiones y 90 s; y {@code CHEST} no esta en {@code ZONAS_PEQUENAS}
     * -{@code lower legs} y {@code waist}-, asi que no lleva las 3
     * repeticiones extra.
     * <p>
     * La version anterior copiaba el fixture de {@code SlotCoverage4CTest}
     * -2x10 con 75 s- y ahi funciona porque esa suite construye el validador
     * con el constructor de dos argumentos, que deja la dosis en {@code OFF}.
     * Aqui la dosis esta en {@code ENFORCE} y ese descanso incumplia.
     */
    private static PlanDraft.DraftExercise prescrito(long id) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS,
                (short) 3, (short) 10, null, null, (short) 90, null);
    }

    private static PlanGenerationContext contexto() {
        var elegibles = new ArrayList<CandidateExercise>();
        for (long id = 1; id <= 7; id++) elegibles.add(candidato(id));
        return new PlanGenerationContext(USUARIO, (short) 1, LUNES, LUNES.plusDays(6),
                perfil(), PlanAdjustment.none(), null, List.copyOf(elegibles),
                SafetyProfile.none(), (PrescriptionParams) null);
    }

    /** HORIZONTAL_PUSH es un patron real: MandatorySlot lo mapea a UPPER_PUSH. */
    private static CandidateExercise candidato(long id) {
        return new CandidateExercise(id, "ejercicio " + id, "CHEST", "body weight", 1,
                PrescriptionType.SETS_REPS, null, "HORIZONTAL_PUSH");
    }

    /**
     * GAIN_MUSCLE: sin eso, {@code MvpDosePolicy.aplicaA} es falso y V24 ni se
     * mide.
     * <p>
     * SESIONES DE 60 MINUTOS, NO DE 45, Y ES DELIBERADO. 4D permite usar el
     * {@code FRACCION_UTIL} = 85 %, y siete colocaciones estructurales cuestan
     * {@code 7 x 3 x (10 x SEG_POR_REP + 90)} = 2520 s = 42 min. Con 45
     * minutos el presupuesto son 38,25 y el escenario incumpliria DOS reglas
     * -el tope y el tiempo-, asi que la prueba pasaria sin demostrar cual.
     * Con 60 el presupuesto son 51 y solo queda el tope, que es lo que se
     * quiere aislar.
     */
    private static PlanningProfile perfil() {
        return new PlanningProfile(USUARIO, 30, "MALE", new BigDecimal("175.0"),
                new BigDecimal("72.0"), new BigDecimal("75.0"), "BEGINNER",
                "GAIN_MUSCLE", "Ganar masa muscular entrenando en casa", "Ninguna",
                "HOME", 3, List.of((short) 1, (short) 3, (short) 5), 60,
                List.of("body weight", "dumbbell", "band"), List.of(),
                SafetyProfile.Declaradas.sinLimitaciones());
    }
}
