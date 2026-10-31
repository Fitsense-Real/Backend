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
 * D8 · LA COPIA DE UNA SESION ACORTADA CON AUTORIZACION HEREDA LA EXENCION DE V20.
 * <p>
 * Defecto (VERIFICACION_DISENO_V03.md): un ACORTAR_O_REORGANIZAR aplicado deja
 * la sesion bajo el piso de V20, exenta esa semana; al copiarla la semana
 * siguiente perdia la exencion y la semana quedaba NO_FACTIBLE aunque la
 * persona lo hubiera cumplido todo.
 * <p>
 * Aqui la semana medida ya trae el lunes acortado (5 colocaciones, 30 min; el
 * piso es 31) y la semana nueva la copia identica, con el productor, V8C y la
 * validacion estricta reales. {@code reduccionAutorizada} es lo que calcula
 * {@code ReduccionAutorizadaResolver} con V31 + V32; su recorrido se prueba en
 * {@code LinajeIntegridadIT}.
 * <p>
 * Dominio puro: sin Spring, sin base de datos y sin Replicate.
 */
@DisplayName("D8 · la copia de una sesion acortada con autorizacion se publica; sin ella, rige V20")
class HerenciaReduccionV20Test {

    private static final long USUARIO = 7L;
    private static final LocalDate LUNES = LocalDate.of(2026, 11, 2);
    private static final int SESIONES = 3;
    private static final int DIVISOR = 30;
    private static final int MINUTOS_SESION = 45;
    private static final SessionDurationEstimator ESTIMADOR = new SessionDurationEstimator();
    /** El lunes acortado: 601 en la semana medida. */
    private static final long SENALADA = 601L;

    private final AdaptedWeekProducer productor = new AdaptedWeekProducer(
            new SessionFixApplier(), new ConservationValidator(), estricta());

    @Test
    @DisplayName("premisa: el lunes acortado estima 30 min y el piso de V20 es 31")
    void premisa() {
        assertEquals(30, minutos(ejercicios(0, 5)));
        assertEquals(31, PlanDraftValidator.minimumSessionMinutes(contexto(PlanAdjustment.none(), Set.of())));
    }

    @Test
    @DisplayName("con la reduccion autorizada heredada, la copia se publica sin cambiar nada")
    void copiaAutorizadaSePublica() {
        var resultado = productor.produce(contexto(PlanAdjustment.none(), Set.of(SENALADA)), derivacion(),
                List.of(), elegibles());
        assertTrue(resultado.publicable(), "la copia del acortado tenia que publicarse: " + resultado.problemas());
        assertEquals(AdaptedWeekResult.Motivo.SIN_ORDEN, resultado.motivo());
        assertEquals(GenerationSource.CONSERVED, resultado.source());
        assertEquals(Set.of(diaCopia(0)), resultado.conservacion().fechasConDuracionReducible(),
                "la exencion es solo para el lunes copiado");
        assertTrue(resultado.conservacion().conservadas().containsAll(
                List.of(SENALADA, SENALADA + 1, SENALADA + 2)));
    }

    @Test
    @DisplayName("sin reduccion autorizada (o con un linaje sin evidencia), la misma copia la rechaza V20")
    void sinAutorizacionRigeV20() {
        var resultado = productor.produce(contexto(PlanAdjustment.none(), Set.of()), derivacion(),
                List.of(), elegibles());
        assertFalse(resultado.publicable());
        assertTrue(resultado.problemas().stream().anyMatch(p -> p.startsWith("V20")
                && p.contains(diaCopia(0).toString())), resultado.problemas().toString());
        assertEquals(Set.of(), resultado.conservacion().fechasConDuracionReducible());
    }

    @Test
    @DisplayName("la autorizacion de OTRA sesion no exime al lunes")
    void autorizacionDeOtraSesion() {
        var resultado = productor.produce(contexto(PlanAdjustment.none(), Set.of(SENALADA + 1)), derivacion(),
                List.of(), elegibles());
        assertFalse(resultado.publicable());
        assertTrue(resultado.problemas().stream().anyMatch(p -> p.startsWith("V20")), resultado.problemas().toString());
    }

    @Test
    @DisplayName("una intervencion nueva distinta sobre el lunes interrumpe la herencia: V20 rige")
    void intervencionNuevaInterrumpe() {
        var resultado = productor.produce(
                contexto(orden(TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION), Set.of(SENALADA)),
                derivacion(), List.of(), elegibles());
        assertFalse(resultado.publicable(), "con otra intervencion el lunes no hereda la exencion");
        assertTrue(resultado.problemas().stream().anyMatch(p -> p.startsWith("V20")), resultado.problemas().toString());
        assertFalse(resultado.conservacion().fechasConDuracionReducible().contains(diaCopia(0)));
    }

    @Test
    @DisplayName("la herencia solo exime de V20: V24 sigue rigiendo en la copia")
    void soloV20() {
        // Con la reduccion autorizada el resultado es publicable, y el DOSIS_4D
        // del validador sigue midiendo; aqui se comprueba que el informe exime
        // solo fechas, no reglas: V24 se aplica a todas las sesiones.
        var resultado = productor.produce(contexto(PlanAdjustment.none(), Set.of(SENALADA)), derivacion(),
                List.of(), elegibles());
        assertTrue(resultado.problemas().stream().noneMatch(p -> p.startsWith("V24")));
        assertTrue(resultado.publicable());
    }

    private static LocalDate diaCopia(int i) {
        return diaMedido(i).plusWeeks(1);
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
        return sesion == 0 ? 5 : 6;
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
                "GAIN_MUSCLE", "Ganar masa muscular entrenando en casa", "Ninguna",
                "HOME", SESIONES, List.of((short) 1, (short) 3, (short) 5),
                MINUTOS_SESION, List.of("body weight", "dumbbell", "band"), List.of(),
                SafetyProfile.Declaradas.sinLimitaciones());
    }
}
