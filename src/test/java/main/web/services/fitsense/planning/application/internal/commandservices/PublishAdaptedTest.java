package main.web.services.fitsense.planning.application.internal.commandservices;

import main.web.services.fitsense.planning.domain.model.aggregates.PlanAdaptationAttempt;
import main.web.services.fitsense.planning.domain.model.aggregates.WeeklyTrainingPlan;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptationAttempt;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptationDecision;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptedPublication;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptedWeekResult;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdjustmentType;
import main.web.services.fitsense.planning.domain.model.valueobjects.ConservationReport;
import main.web.services.fitsense.planning.domain.model.valueobjects.DerivedWorkout;
import main.web.services.fitsense.planning.domain.model.valueobjects.GenerationSource;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanStatus;
import main.web.services.fitsense.planning.domain.model.valueobjects.PreparedPlanGeneration;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.planning.domain.services.AdaptationEvidenceMapper;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.PlanAdaptationAttemptRepository;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.WeeklyTrainingPlanRepository;
import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@code publishAdapted}: la semana adaptada y su evidencia, en una transaccion.
 * <p>
 * ESTA PRUEBA EXISTE PORQUE EL METODO NO TENIA NINGUN LLAMADOR. P2-B-2c se
 * entrego como documento de edicion con la nota «nadie llama a publishAdapted
 * hasta P2-B-3», y el documento se aplico A MEDIAS: entro el refactor de
 * {@code materialize} y el metodo no. Compilaba igual, porque un metodo que no
 * existe y que nadie llama no rompe nada. Las 86 pruebas verdes no podian
 * detectarlo.
 * <p>
 * Asi que el valor de este archivo no esta solo en lo que comprueba: esta en que
 * a partir de ahora {@code publishAdapted} TIENE un llamador. Si vuelve a
 * faltar, no compila.
 * <p>
 * Sin base de datos: los dos repositorios son dobles. Lo que se comprueba aqui
 * son las decisiones del metodo -que publica, que rechaza, en que orden escribe-,
 * no que Postgres acepte las filas. Eso es P2-D.
 */
class PublishAdaptedTest {

    private static final long USUARIO = 7L;
    private static final LocalDate LUNES = LocalDate.of(2026, 10, 19);
    private static final LocalDate DOMINGO = LUNES.plusDays(6);
    private static final long PLAN_GUARDADO = 500L;
    private static final long SESION_SENALADA = 101L;

    private WeeklyTrainingPlanRepository planRepository;
    private PlanAdaptationAttemptRepository attemptRepository;
    private WeeklyTrainingPlanCommandServiceImpl servicio;
    private WeeklyTrainingPlan guardado;

    /**
     * La fila de evidencia tal como la produjo el mapper REAL.
     * <p>
     * Hace falta porque {@code PlanAdaptationAttempt} solo expone seis getters
     * -id, plan, estado, decision, origen y fecha- y ni {@code aiCalls} ni
     * {@code appliedSessions} estan entre ellos. Asi que el mapper se envuelve
     * en vez de simularse: la traduccion sigue siendo la de produccion y el
     * resultado queda a la vista.
     */
    private final java.util.concurrent.atomic.AtomicReference<AdaptationAttempt> evidencia =
            new java.util.concurrent.atomic.AtomicReference<>();

    @BeforeEach
    void montar() {
        planRepository = mock(WeeklyTrainingPlanRepository.class);
        attemptRepository = mock(PlanAdaptationAttemptRepository.class);

        // El plan que devuelve la base ya tiene id; el que entra todavia no.
        // publishAdapted solo le pide getId(), para la fila de evidencia.
        guardado = mock(WeeklyTrainingPlan.class);
        when(guardado.getId()).thenReturn(PLAN_GUARDADO);
        when(planRepository.saveAndFlush(any())).thenReturn(guardado);

        servicio = new WeeklyTrainingPlanCommandServiceImpl(
                planRepository, null, attemptRepository, mapperQueDejaVerLaFila(),
                null, null, null, null, null, null, new JsonSupport(), null, "OBSERVE");
    }

    private AdaptationEvidenceMapper mapperQueDejaVerLaFila() {
        var real = new AdaptationEvidenceMapper();
        var espia = mock(AdaptationEvidenceMapper.class);
        when(espia.map(any(), any(), any(), any())).thenAnswer(llamada -> {
            var fila = real.map(llamada.getArgument(0), llamada.getArgument(1),
                    llamada.getArgument(2), llamada.getArgument(3));
            evidencia.set(fila);
            return fila;
        });
        return espia;
    }

    // =======================================================================
    // Lo que no se publica
    // =======================================================================

    @Test
    @DisplayName("Un NO_FACTIBLE no se publica, y no deja evidencia por este camino")
    void noFactibleNoSePublica() {
        var resultado = AdaptedWeekResult.noFactible(
                AdaptedWeekResult.Motivo.RESPALDO_IA, ConservationReport.vacio(),
                List.of("V8C: la sesion 101 cambio un ejercicio que nadie senalo"));

        var error = assertThrows(DomainRuleViolationException.class,
                () -> servicio.publishAdapted(preparado(ordenLocalizada()),
                        publicacion(resultado, AdaptationDecision.ADAPT_LOCALIZED, "baja adherencia")));

        assertTrue(error.getMessage().contains("NO_FACTIBLE"), error::getMessage);

        // La evidencia de un fallo va por AdaptationFailureRecorder, en su
        // propia transaccion. Si saliera por aqui, se iria con el rollback.
        verifyNoInteractions(attemptRepository);
        verify(planRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Reemplazar un plan activo sin motivo se rechaza, no se inventa uno")
    void reemplazarSinMotivoSeRechaza() {
        // NO_CHANGE no trae motivo: PlanAdjustment.none() deja reason en null, y
        // nextVersionOf rechaza un motivo vacio. Se exige declararlo.
        when(planRepository.findByUserIdAndWeekStartDateAndStatus(USUARIO, LUNES, PlanStatus.ACTIVE))
                .thenReturn(Optional.of(planPrevio()));

        var error = assertThrows(DomainRuleViolationException.class,
                () -> servicio.publishAdapted(preparado(PlanAdjustment.none()),
                        publicacion(conservada(), AdaptationDecision.NO_CHANGE, null)));

        assertTrue(error.getMessage().contains("replacementReason"), error::getMessage);
        verifyNoInteractions(attemptRepository);
    }

    // =======================================================================
    // Lo que se publica
    // =======================================================================

    @Test
    @DisplayName("Publica la semana Y registra la evidencia en la misma llamada")
    void publicaYRegistra() {
        var devuelto = servicio.publishAdapted(preparado(ordenLocalizada()),
                publicacion(corregida(), AdaptationDecision.ADAPT_LOCALIZED, "baja adherencia"));

        assertTrue(devuelto.isPresent());
        assertEquals(PLAN_GUARDADO, devuelto.get().getId());
        verify(attemptRepository, times(1)).save(any(PlanAdaptationAttempt.class));
    }

    @Test
    @DisplayName("Una adaptacion aplicada deja constancia de QUE sesion se aplico")
    void laEvidenciaNombraLaSesionAplicada() {
        // El fixture anterior usaba ConservationReport.vacio(), asi que registraba
        // APLICADA con applied_sessions en vacio: una adaptacion que consta
        // aplicada y no aplico nada. Ni V32 ni AdaptationAttempt lo rechazan.
        // Aqui el informe lleva la sesion 101 en aplicadas, que es lo que de
        // verdad produce el productor cuando huboAdaptacionEfectiva().
        servicio.publishAdapted(preparado(ordenLocalizada()),
                publicacion(corregida(), AdaptationDecision.ADAPT_LOCALIZED, "baja adherencia"));

        var fila = evidencia.get();
        assertNotNull(fila, "el mapper no llego a producir la fila");
        assertEquals(AdaptationAttempt.Status.APLICADA, fila.status());
        assertEquals(List.of(SESION_SENALADA), fila.appliedSessions(),
                "una adaptacion aplicada sin sesiones aplicadas no es evidencia de nada");
        assertTrue(fila.noEffectSessions().isEmpty(),
                () -> "la unica senalada se aplico: " + fila.noEffectSessions());
        assertEquals((short) 1, fila.aiCalls());
    }

    @Test
    @DisplayName("Con llamadas reales a la IA, generation_attempts y ai_calls conservan el numero real")
    void conLlamadasRealesSeConservaElNumero() {
        var captor = ArgumentCaptor.forClass(WeeklyTrainingPlan.class);
        var tres = new AdaptedPublication(corregida(), AdaptationDecision.ADAPT_LOCALIZED, 400L, 300L,
                "{\"prompt_version\":\"FIX-PR-1.1\"}", "gpt-5-nano", (short) 3,
                new BigDecimal("42.00"), new BigDecimal("80.00"), "baja adherencia");

        servicio.publishAdapted(preparado(ordenLocalizada()), tres);

        verify(planRepository).saveAndFlush(captor.capture());
        assertEquals((short) 3, captor.getValue().getGenerationAttempts());
        assertEquals((short) 3, evidencia.get().aiCalls());
    }

    @Test
    @DisplayName("El plan se vacia a la base ANTES del intento: fk_paa_result no es diferida")
    void elPlanSeEscribeAntesDeLaEvidencia() {
        servicio.publishAdapted(preparado(ordenLocalizada()),
                publicacion(corregida(), AdaptationDecision.ADAPT_LOCALIZED, "baja adherencia"));

        // Si se invirtiera, la clave ajena fallaria en el INSERT del intento y no
        // al final de la transaccion, porque esa comprobacion es inmediata.
        var orden = inOrder(planRepository, attemptRepository);
        orden.verify(planRepository).saveAndFlush(any());
        orden.verify(attemptRepository).save(any());
    }

    @Test
    @DisplayName("La etiqueta de ajuste es null cuando la decision no ordena adaptar")
    void sinOrdenNoHayEtiqueta() {
        // Una semana conservada por NO_CHANGE no es una semana ajustada. Marcarla
        // con una etiqueta contaria como adaptacion en la cifra de la tesis.
        var captor = ArgumentCaptor.forClass(WeeklyTrainingPlan.class);

        servicio.publishAdapted(preparado(PlanAdjustment.none()),
                publicacion(conservada(), AdaptationDecision.NO_CHANGE, null));

        verify(planRepository).saveAndFlush(captor.capture());
        assertNull(captor.getValue().getAdjustmentApplied(),
                "NO_CHANGE no adapta: adjustment_applied tiene que quedar en null");

        // Y NO SE LLAMO A LA IA. Si no hubo orden de adaptacion, no hubo peticion
        // al proveedor: ai_calls = 1 pasaria el CHECK de V32 -admite de 0 a 4- y
        // afirmaria una llamada que nunca ocurrio, que es un dato falso en la
        // tabla de la que sale el coste de IA de la tesis.
        var fila = evidencia.get();
        assertNotNull(fila, "el mapper no llego a producir la fila");
        assertEquals(AdaptationAttempt.Status.SIN_ORDEN, fila.status());
        assertEquals((short) 0, fila.aiCalls(),
                "sin orden de adaptacion no se llama a la IA");
        assertTrue(fila.appliedSessions().isEmpty(), () -> fila.appliedSessions().toString());

        // Pero el plan si cuenta un intento de produccion: ck_plan_attempts (V4)
        // exige >= 1, y con 0 la continuidad no se podia publicar.
        assertEquals((short) 1, captor.getValue().getGenerationAttempts(),
                "una continuidad sin IA es un intento determinista de producir la semana");
    }

    @Test
    @DisplayName("El snapshot de salida se etiqueta ADAPTED_WEEK_V1 y lleva el origen de cada sesion")
    void elSnapshotDistingueElFormato() {
        // El de generacion inicial es un PlanDraft y NO lleva de que sesion viene
        // cada una. Sin la etiqueta, los dos formatos son indistinguibles al
        // leerlos despues para la tesis.
        var captor = ArgumentCaptor.forClass(WeeklyTrainingPlan.class);

        servicio.publishAdapted(preparado(ordenLocalizada()),
                publicacion(corregida(), AdaptationDecision.ADAPT_LOCALIZED, "baja adherencia"));

        verify(planRepository).saveAndFlush(captor.capture());
        var snapshot = captor.getValue().getOutputSnapshot();

        assertNotNull(snapshot, "la semana adaptada tiene que dejar su snapshot de salida");
        assertTrue(snapshot.contains("ADAPTED_WEEK_V1"), snapshot);
        assertTrue(snapshot.contains("sourcePlannedWorkoutId") || snapshot.contains("source_planned_workout_id"),
                () -> "el snapshot pierde el linaje de las sesiones: " + snapshot);
    }

    // =======================================================================
    // Material
    // =======================================================================

    private static PlanAdjustment ordenLocalizada() {
        return new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null,
                0.0, 0.0, null, null, null, "baja adherencia", null, null,
                List.of(new PlanAdjustment.TargetedSession(SESION_SENALADA,
                        TipoIntervencion.ACORTAR_O_REORGANIZAR)));
    }

    /**
     * Semana corregida por la IA: origen AI, motivo CORRECCION_IA.
     * <p>
     * CON LA SESION 101 EN APLICADAS, y no con un informe vacio. El productor
     * solo devuelve CORRECCION_IA cuando {@code informe.huboAdaptacionEfectiva()},
     * que es literalmente {@code !aplicadas.isEmpty()}. Un informe vacio con este
     * motivo es un estado que produccion NO puede alcanzar, asi que probar contra
     * el no demuestra nada sobre produccion.
     */
    private static AdaptedWeekResult corregida() {
        var informe = new ConservationReport(List.of(), List.of(SESION_SENALADA),
                List.of(), List.of(), Set.of());

        return new AdaptedWeekResult(AdaptedWeekResult.Estado.PUBLICABLE,
                AdaptedWeekResult.Motivo.CORRECCION_IA, semana(), GenerationSource.AI,
                informe, List.of());
    }

    /** Semana copiada tal cual: origen CONSERVED, motivo SIN_ORDEN. */
    private static AdaptedWeekResult conservada() {
        return new AdaptedWeekResult(AdaptedWeekResult.Estado.PUBLICABLE,
                AdaptedWeekResult.Motivo.SIN_ORDEN, semana(), GenerationSource.CONSERVED,
                ConservationReport.vacio(), List.of());
    }

    private static List<DerivedWorkout> semana() {
        return List.of(new DerivedWorkout(SESION_SENALADA, new PlanDraft.DraftWorkout(
                LUNES, WorkoutFocus.FULL_BODY, "FULL_BODY - Sesion 1", 45,
                List.of(new PlanDraft.DraftExercise(1L, PrescriptionType.SETS_REPS,
                        (short) 3, (short) 10, null, null, (short) 90, null)))));
    }

    private static AdaptedPublication publicacion(AdaptedWeekResult resultado,
                                                  AdaptationDecision decision,
                                                  String motivoDeReemplazo) {
        // EL MODELO VA EN NULL SI LA SEMANA ES CONSERVED, y no por limpieza: el
        // constructor de AdaptedPublication lo rechaza. Nombrar un modelo en una
        // semana que la IA no genero inflaria la proporcion de IA frente a
        // reglas, que es un resultado de la tesis.
        String modelo = resultado.source() == GenerationSource.CONSERVED ? null : "gpt-5-nano";

        // LAS LLAMADAS A LA IA SALEN DE LA DECISION, no de una constante. Sin
        // orden de adaptacion no se pide nada al proveedor, asi que ai_calls = 0.
        // Fijarlo en 1 pasaria el CHECK de V32 -admite de 0 a 4- y dejaria en la
        // tabla una llamada que nunca ocurrio.
        short llamadas = (short) (decision.ordenaAdaptar() ? 1 : 0);

        return new AdaptedPublication(resultado, decision, 400L, 300L,
                "{\"prompt_version\":\"FIX-PR-1.1\"}", modelo, llamadas,
                new BigDecimal("42.00"), new BigDecimal("80.00"), motivoDeReemplazo);
    }

    private static PreparedPlanGeneration preparado(PlanAdjustment ajuste) {
        var command = new GenerateWeeklyPlanCommand(USUARIO, LUNES, ajuste, true,
                new BigDecimal("42.00"), null);
        return new PreparedPlanGeneration(command, null, ajuste, LUNES, DOMINGO,
                30, (short) 2, null, ZoneId.of("America/Lima"));
    }

    /** Un plan ya publicado de la misma semana, para el camino de reemplazo. */
    private static WeeklyTrainingPlan planPrevio() {
        return WeeklyTrainingPlan.firstVersion(USUARIO, (short) 2, LUNES, DOMINGO,
                GenerationSource.AI, "gpt-5-nano", "{}", (short) 1);
    }
}