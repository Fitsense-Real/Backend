package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalConfigurationService;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptedWeekResult;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdjustmentType;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.CorrectedSession;
import main.web.services.fitsense.planning.domain.model.valueobjects.DerivedWorkout;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * INC-P2D-01 · EL RECORRIDO INTEGRADO, CON LOS DOS VALIDADORES DE VERDAD.
 *
 * <h2>El hueco que cierra</h2>
 *
 * Las otras dos clases dejaban un supuesto en cada lado:
 * <ul>
 *   <li>{@code ConservationValidatorQuitarRellenoTest} comprueba que V8C
 *       autoriza la retirada, pero no mira la validacion estricta;
 *   <li>{@code SemanaAcortadaPublicableTest} comprueba la validacion estricta,
 *       pero con un {@code ConservationReport} <b>construido a mano</b>.
 * </ul>
 * Nadie comprobaba lo del medio: que las fechas que V8C mete en
 * {@code fechasConDuracionReducible} sean las que el productor pasa al
 * validador. Si esa correspondencia se rompiera, las dos clases seguirian en
 * verde y la adaptacion real fallaria.
 * <p>
 * {@code AdaptedWeekProducerTest}, que ya existe, usa un DOBLE de
 * {@link StrictPlanValidation}: prueba la orquestacion del productor, no la
 * validacion. Aqui se montan los dos reales.
 *
 * <h2>Las tres piezas, sin dobles</h2>
 *
 * <pre>
 * new AdaptedWeekProducer(new SessionFixApplier(),
 *                         new ConservationValidator(),      // V8C de verdad
 *                         new StrictValidationAdapter(validador, configuracion))
 * </pre>
 * con el {@code PlanDraftValidator} en {@code DoseEnforcement.ENFORCE} y
 * {@code SlotCoverageEnforcement.ENFORCE_AI_ONLY}, que es el modo de produccion.
 * El unico doble es {@code ExternalConfigurationService}, que solo aporta el
 * divisor de repeticiones.
 * <p>
 * {@code AdaptedWeekProducer} rechaza en su constructor una validacion que no
 * bloquee la dosis de 4D, asi que montarlo ya demuestra que esta en ENFORCE.
 *
 * <h2>El escenario</h2>
 *
 * Tres sesiones de seis colocaciones —cuatro estructurales y dos rellenos—, la
 * del lunes senalada como {@code ACORTAR_O_REORGANIZAR}. La correccion retira
 * <b>un relleno</b>: 33 min estimados pasan a 30, por debajo del piso de 31
 * (70 % de 45). O sea que la semana solo puede publicarse si la exencion de V20
 * que produce V8C llega de verdad al validador.
 * <p>
 * Es exactamente el caso que la corrida real de Replicate no pudo resolver,
 * porque entonces ninguna operacion reducia el trabajo.
 *
 * <h2>Dominio puro</h2>
 *
 * Sin Spring, sin base de datos y sin Replicate.
 */
@DisplayName("INC-P2D-01 · V8C y la validacion estricta, juntas y de verdad")
class AdaptacionAcortaIntegradaTest {

    private static final long USUARIO = 7L;
    /** La semana medida arranca el lunes 2026-11-02; la derivada, siete dias despues. */
    private static final LocalDate LUNES = LocalDate.of(2026, 11, 2);
    private static final int SESIONES = 3;
    private static final int DIVISOR = 30;
    private static final int MINUTOS_SESION = 45;

    private static final SessionDurationEstimator ESTIMADOR = new SessionDurationEstimator();

    /** La sesion senalada es la primera: 601 en la semana medida. */
    private static final long SENALADA = 601L;

    private final AdaptedWeekProducer productor = new AdaptedWeekProducer(
            new SessionFixApplier(), new ConservationValidator(), estricta());

    // =======================================================================
    // Lo que cierra el incidente
    // =======================================================================

    @Test
    @DisplayName("Retirar un relleno produce una semana PUBLICABLE por el camino completo")
    void retirarUnRellenoProduceSemanaPublicable() {
        var resultado = productor.produce(contexto(), derivacion(),
                List.of(correccion(5)), elegibles());

        assertTrue(resultado.publicable(),
                "la semana acortada tenia que ser publicable por el camino completo y"
                        + " los problemas fueron: " + resultado.problemas());

        assertEquals(AdaptedWeekResult.Motivo.CORRECCION_IA, resultado.motivo(),
                "retirar una colocacion ES un cambio efectivo, asi que el motivo tenia"
                        + " que ser CORRECCION_IA y no " + resultado.motivo()
                        + ". Informe: " + resultado.conservacion().describe());

        assertEquals(GenerationSource.AI, resultado.source(),
                "una semana que la IA cambio de verdad se declara AI, no CONSERVED");

        assertEquals(List.of(SENALADA), List.copyOf(resultado.conservacion().aplicadas()),
                "la aplicada tenia que ser la senalada y solo ella. Informe: "
                        + resultado.conservacion().describe());

        assertTrue(resultado.conservacion().conservadas().containsAll(
                        List.of(SENALADA + 1, SENALADA + 2)),
                "las dos sesiones que nadie senalo tenian que constar conservadas."
                        + " Informe: " + resultado.conservacion().describe());

        // LA CORRESPONDENCIA QUE NINGUNA OTRA PRUEBA MIRABA: V8C autoriza
        // reducir la duracion del lunes derivado y SOLO de ese. Esta linea es la
        // que convierte el supuesto en comprobacion: en
        // SemanaAcortadaPublicableTest esa fecha la escribia yo a mano, y aqui la
        // produce el validador de conservacion.
        //
        // Y como la prueba pasa por el adaptador real, el verde demuestra ademas
        // que esa autorizacion basta para superar V20 con 30 minutos.
        assertEquals(Set.of(diaMedido(0).plusWeeks(1)),
                resultado.conservacion().fechasConDuracionReducible(),
                "V8C tenia que eximir el lunes derivado y ninguna otra fecha");

        // EL CONTENIDO REAL DE LA SEMANA, no solo el informe de V8C.
        //
        // Sin esto, el verde podria venir de algo que cambiara unicamente la
        // duracion declarada sin retirar el ejercicio: el informe diria
        // «aplicada» y la semana seguiria pesando lo mismo. Lo que la tesis
        // afirma es una REDUCCION REAL del entrenamiento.
        assertEquals(SESIONES, resultado.semana().size(),
                "la semana adaptada tenia que conservar sus tres sesiones");

        var acortada = resultado.semana().stream()
                .filter(s -> Long.valueOf(SENALADA).equals(s.sourcePlannedWorkoutId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "la semana adaptada no trae la sesion senalada " + SENALADA));

        // Los CINCO que quedan, con sus dosis intactas: el relleno retirado es la
        // unica diferencia.
        assertEquals(ejercicios(0, 5), acortada.workout().exercises(),
                "la sesion acortada tenia que quedar con los cinco ejercicios"
                        + " esperados y sus dosis sin tocar");

        // Los 30 y los 33 van literales a proposito, y no calculados con el
        // estimador: SemanaAcortadaPublicableTest.laPremisaDeLaAritmetica ya los
        // ancla a la formula real, asi que aqui son una comprobacion y no una
        // tautologia.
        assertEquals(30, acortada.workout().expectedDurationMinutes(),
                "cinco colocaciones declaran 30 minutos");

        for (int i = 1; i < SESIONES; i++) {
            long origen = sesionMedida(i);
            var conservada = resultado.semana().stream()
                    .filter(s -> Long.valueOf(origen).equals(s.sourcePlannedWorkoutId()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "la semana adaptada no trae la sesion " + origen));

            assertEquals(ejercicios(i, 6), conservada.workout().exercises(),
                    "la sesion " + origen + " no la senalo nadie: tenia que llegar"
                            + " identica, con sus seis colocaciones");
            assertEquals(33, conservada.workout().expectedDurationMinutes(),
                    "seis colocaciones declaran 33 minutos");
        }
    }

    @Test
    @DisplayName("Una respuesta identica a la semana derivada termina en SIN_EFECTO")
    void sinCambiosProduceSinEfecto() {
        // La correccion devuelve las seis colocaciones EN EL MISMO ORDEN: es una
        // respuesta identica a la derivada, no una reordenacion.
        //
        // Y NO DEMUESTRA QUE V8C NIEGUE LA EXENCION. ConservationValidator:204
        // añade la fecha a fechasReducibles en cuanto la intervencion es
        // ACORTAR_O_REORGANIZAR, cambie algo o no. Lo que esta prueba demuestra
        // es que con 33 minutos la sesion supera el piso de 31 por si misma, asi
        // que la exencion esta registrada y no hace falta usarla. Son dos cosas
        // distintas y el nombre anterior las confundia.
        var resultado = productor.produce(contexto(), derivacion(),
                List.of(correccion(6)), elegibles());

        assertTrue(resultado.publicable(),
                "la semana sin acortar tenia que pasar: " + resultado.problemas());

        // Seis colocaciones iguales a las derivadas: la IA no cambio nada.
        assertEquals(AdaptedWeekResult.Motivo.SIN_EFECTO, resultado.motivo(),
                "sin cambios reales el motivo es SIN_EFECTO y fue " + resultado.motivo());
    }

    @Test
    @DisplayName("Retirar un ESTRUCTURAL lo rechaza V8C dentro del camino completo")
    void retirarUnEstructuralSeRechaza() {
        var resultado = productor.produce(contexto(), derivacion(),
                List.of(correccionSinEstructural()), elegibles());

        assertFalse(resultado.publicable(),
                "retirar una colocacion estructural tenia que rechazarse");

        assertTrue(String.join(" | ", resultado.problemas()).contains("ESTRUCTURAL"),
                "el motivo tenia que ser la regla de relleno de V8C y fue: "
                        + resultado.problemas());

        assertEquals(AdaptedWeekResult.Estado.NO_FACTIBLE, resultado.estado(),
                "un rechazo deja el resultado en NO_FACTIBLE");
    }

    // =======================================================================
    // Las piezas reales
    // =======================================================================

    /**
     * La validacion estricta de produccion: {@code PlanDraftValidator} con la
     * dosis de 4D en {@code ENFORCE} y la cobertura en
     * {@code ENFORCE_AI_ONLY}, envuelto en el adaptador real.
     * <p>
     * El unico doble de toda la clase es {@code ExternalConfigurationService},
     * que solo aporta el divisor.
     */
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
    private static WeekDerivation derivacion() {
        var derivadas = new ArrayList<DerivedWorkout>();
        for (int i = 0; i < SESIONES; i++)
            derivadas.add(new DerivedWorkout(sesionMedida(i),
                    sesionCon(i, diaMedido(i).plusWeeks(1), ejercicios(i, 6))));
        return new WeekDerivation(List.copyOf(derivadas), List.of());
    }

    /**
     * La correccion de la sesion senalada, con {@code colocaciones} ejercicios.
     * Con 5 se retira uno de los dos rellenos; con 6 no se retira nada.
     * <p>
     * CONSERVA EL DIA DERIVADO: {@code ACORTAR_O_REORGANIZAR} no autoriza
     * moverlo, y si lo moviera el rechazo seria por eso y no por lo que se quiere
     * probar.
     */
    private static CorrectedSession correccion(int colocaciones) {
        var lista = ejercicios(0, colocaciones);
        return new CorrectedSession(SENALADA, diaMedido(0).plusWeeks(1),
                minutos(lista), lista, "menos tiempo disponible");
    }

    /** Igual, pero el que se retira es el ESTRUCTURAL de la posicion 0. */
    private static CorrectedSession correccionSinEstructural() {
        var lista = new ArrayList<PlanDraft.DraftExercise>();
        for (int j = 1; j < 6; j++) lista.add(prescrito(id(0, j), esRelleno(j)));
        return new CorrectedSession(SENALADA, diaMedido(0).plusWeeks(1),
                minutos(List.copyOf(lista)), List.copyOf(lista), "menos tiempo disponible");
    }

    private static PreviousWeekSummary semanaMedida() {
        var sesiones = new ArrayList<PreviousWeekSummary.WorkoutOutcome>();
        var prescripciones = new ArrayList<PreviousWeekSummary.PrescriptionOutcome>();

        for (int i = 0; i < SESIONES; i++) {
            var lista = ejercicios(i, 6);
            sesiones.add(new PreviousWeekSummary.WorkoutOutcome(
                    sesionMedida(i), diaMedido(i), "FULL_BODY", "COMPLETED", null,
                    true, "COMPLETED", (short) 7, new BigDecimal("100.00"),
                    "FULL_BODY - Sesion " + (i + 1), (short) minutos(lista)));

            for (int j = 0; j < 6; j++)
                prescripciones.add(new PreviousWeekSummary.PrescriptionOutcome(
                        sesionMedida(i), diaMedido(i), id(i, j), "ejercicio " + id(i, j),
                        "SETS_REPS", series(j), (short) 10, null, null,
                        series(j), 10 * series(j), null, null,
                        new BigDecimal("100.00"), "COMPLETED", null, "COMPLETED",
                        (short) 90));
        }

        return new PreviousWeekSummary(new BigDecimal("42.00"), new BigDecimal("7.0"),
                null, Map.of(), List.copyOf(sesiones), List.copyOf(prescripciones),
                List.of());
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

    /** La orden que senala la sesion del lunes, y nada mas. */
    private static PlanAdjustment orden() {
        return new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null,
                0.0, 0.0, null, null, null, "menos tiempo disponible", null, null,
                List.of(new PlanAdjustment.TargetedSession(
                        SENALADA, TipoIntervencion.ACORTAR_O_REORGANIZAR)));
    }

    /**
     * {@code GAIN_MUSCLE} es obligatorio: sin eso V24 ni se mide y el productor
     * no estaria probando la dosis. Sesiones de 45 minutos, para que el piso de
     * V20 sean los 31 del hallazgo real.
     */
    private static PlanGenerationContext contexto() {
        return new PlanGenerationContext(USUARIO, (short) 2,
                LUNES.plusWeeks(1), LUNES.plusWeeks(1).plusDays(6),
                perfil(), orden(), semanaMedida(),
                List.copyOf(elegibles().values()), SafetyProfile.none(), parametros());
    }

    private static PlanningProfile perfil() {
        return new PlanningProfile(USUARIO, 30, "MALE", new BigDecimal("175.0"),
                new BigDecimal("72.0"), new BigDecimal("75.0"), "BEGINNER",
                "GAIN_MUSCLE", "Ganar masa muscular entrenando en casa", "Ninguna",
                "HOME", SESIONES, List.of((short) 1, (short) 3, (short) 5),
                MINUTOS_SESION, List.of("body weight", "dumbbell", "band"), List.of(),
                SafetyProfile.Declaradas.sinLimitaciones());
    }
}
