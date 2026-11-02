package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.RelojPruebas;
import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.planning.interfaces.rest.TrainingPlanController;
import main.web.services.fitsense.planning.interfaces.rest.resources.SkipWorkoutResource;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * D1 (orden de cierre del respaldo, 02/11/2026): el camino RESPALDO, forzado y determinista.
 * <p>
 * La IA simulada contesta, pero su propuesta se rechaza SIEMPRE (fallo controlado: {@code interpretar} lanza
 * {@link InvalidPlanDraftException}). Agotados los 3 intentos, el ciclo entra en el respaldo sin depender de que una
 * semilla haga fallar a la IA simulada de {@link ValidacionMultisemanaIT}. Base propia (PostgreSQL efimero), orden de
 * pruebas fijo y semana {@link RelojPruebas#SEMANA_REFERENCIA}: mismos usuarios, mismos planes, mismo resultado.
 * <ol>
 *   <li><b>B</b> · RESPALDO con dolor y sustituto: solo cambia la posicion del ejercicio que dolio, por SUST-SEG-1;
 *       las demas sesiones y posiciones, identicas; dosis sin subir; V32 con la sustitucion y la nota; exclusion P-02
 *       activa; la semana siguiente (sin motivo) conserva el sustituto y no reincorpora lo que dolio.</li>
 *   <li><b>A</b> · RESPALDO sin dolor: la semana es la medida, sin ninguna excepcion.</li>
 *   <li><b>C</b> · RESPALDO con dolor sin sustituto (el ejercicio 1373 del diagnostico): nada invalido publicado,
 *       NO_FACTIBLE con motivo y la exclusion intacta; no se levanta ninguna restriccion para publicar.</li>
 * </ol>
 * Con otra semana ({@code -Dfitsense.pruebas.semana-referencia}) los planes cambian: las invariantes se comprueban
 * igual sobre el camino que tome cada caso, pero el camino esperado solo se fija para la semana de referencia.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.progression.enabled=false"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Respaldo forzado: con dolor y sustituto, sin dolor, y con dolor sin sustituto")
class RespaldoConDolorIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoBean ReplicateSessionFixGenerator ia;
    /** El reloj lo controla la prueba: sin barrido horario de entrenamientos vencidos. */
    @MockitoBean main.web.services.fitsense.planning.infrastructure.scheduling.OverdueWorkoutScheduler vencidas;

    @Autowired WeeklyCycleService ciclo;
    @Autowired PlanningContextFacade planning;
    @Autowired UserProfileCommandService perfiles;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired TrainingPlanController planes;
    @Autowired UserRepository usuarios;
    @Autowired JdbcTemplate jdbc;
    @Autowired main.web.services.fitsense.planning.application.internal.queryservices.AdaptationOutcomeQueryService adaptaciones;

    static final LocalDate S1 = RelojPruebas.SEMANA_REFERENCIA;
    static final boolean REFERENCIA = S1.equals(LocalDate.of(2026, 11, 2));
    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final List<String> GIMNASIO = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine",
            "bench", "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine");
    /** Del diagnostico (ANALISIS_NO_FACTIBLE_1373): relleno sin sustituto SUST-SEG-1 en FULL_BODY. */
    static final long SIN_SUSTITUTO = 1373L;

    @BeforeEach
    void iaQueSiempreFalla() {
        reset(ia);
        when(ia.isEnabled()).thenReturn(true);
        when(ia.modelName()).thenReturn("simulado");
        when(ia.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{}", "salida-simulada"));
        when(ia.interpretar(anyString(), any())).thenThrow(new InvalidPlanDraftException(
                List.of("PRUEBA: propuesta rechazada de forma controlada para forzar el respaldo")));
    }

    // =======================================================================
    // B · RESPALDO con dolor y sustituto
    // =======================================================================

    @Test
    @Order(1)
    @DisplayName("B: con dolor, el respaldo sustituye solo el ejercicio que dolio (SUST-SEG-1) y conserva el resto")
    void conDolorSustituyeSoloLoQueDolio() {
        long u = participante();
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var ids = sesionesDe(plan);
        // El miercoles: se hace todo salvo el ULTIMO ejercicio, dejado por dolor (como «7 dolor»), salvo que sea el
        // que no tiene sustituto, que es el caso C.
        long miercoles = ids.get(1);
        var ejercicios = ejerciciosDe(miercoles);
        long dolio = ejercicios.get(ejercicios.size() - 1);
        if (REFERENCIA) assertNotEquals(SIN_SUSTITUTO, dolio, "premisa: el caso B no es el del ejercicio sin sustituto");
        ejecutar(u, ids, Map.of(1, dolio));

        var publicado = ciclo.runFor(u, S1, S1.plusWeeks(1));
        var ev = ultimo(u);
        System.out.println("RESPALDO_B dolio=" + dolio + " ev=" + ev);
        assertEquals("ADAPT_LOCALIZED", ev.get("decision"));
        assertEquals("{SEGURIDAD_ANTE_DOLOR}", ev.get("targeted_interventions"));
        assertEquals(3, ((Number) ev.get("ai_calls")).intValue(), "los 3 intentos se agotan antes del respaldo");
        assertTrue(activa(u, dolio), "la exclusion P-02 del ejercicio que dolio queda activa");
        if (publicado.isEmpty()) {
            assertFalse(REFERENCIA, "en la semana de referencia el caso B se publica: " + ev);
            assertEquals(List.of(), InvariantesRespaldo.sinPublicar(jdbc, u, S1.plusWeeks(1), ev, List.of(dolio)));
            return;
        }
        if (REFERENCIA) {
            assertEquals("RESPALDO", ev.get("status"), "camino esperado en la semana de referencia: " + ev);
            assertEquals("CONSERVED", ev.get("generation_source"));
        }
        long nuevo = publicado.get();
        if ("RESPALDO".equals(ev.get("status"))) {
            assertEquals(List.of(), InvariantesRespaldo.semanaConservada(jdbc, plan, nuevo, ev));
            // Exactamente una posicion cambia: la del ejercicio que dolio, en el miercoles.
            var antes = InvariantesRespaldo.sesiones(jdbc, plan);
            var despues = InvariantesRespaldo.sesiones(jdbc, nuevo);
            int cambios = 0;
            for (int i = 0; i < antes.size(); i++)
                for (int p = 0; p < antes.get(i).size(); p++)
                    if (!antes.get(i).get(p).equals(despues.get(i).get(p))) {
                        cambios++;
                        assertEquals(1, i, "solo cambia el miercoles");
                        assertEquals(dolio, antes.get(i).get(p).id(), "solo cambia la posicion del que dolio");
                    }
            assertEquals(1, cambios, "una sola sustitucion");
            assertTrue(String.valueOf(ev.get("conservation_report")).contains("REGENERACION_SEGURA"));
            var api = adaptaciones.forPlan(nuevo).orElseThrow();
            assertEquals("REGENERACION_SEGURA_SUSTITUCION", api.resultado(), "la API lo distingue de una copia");
            // Fase 2.1: publicado, sin codigo de no factible; la sesion afectada es la del dolor.
            assertEquals("PUBLICADO", api.estadoPublicacion());
            assertNull(api.motivoCodigo());
            assertEquals(List.of(), api.codigos());
            assertNull(jdbc.queryForObject("SELECT reason_codes::text FROM plan_adaptation_attempts WHERE attempt_id = ?",
                    String.class, ev.get("attempt_id")), "un estado que publica no registra codigos");
        }
        assertFalse(contiene(nuevo, dolio), "lo que dolio no vuelve");

        // La semana siguiente, todo hecho y sin motivo: continuidad identica, con el sustituto; sin reincorporar.
        ejecutar(u, sesionesDe(nuevo), Map.of());
        var siguiente = ciclo.runFor(u, S1.plusWeeks(1), S1.plusWeeks(2));
        var ev2 = ultimo(u);
        assertTrue(siguiente.isPresent(), "continuidad publicada: " + ev2);
        assertEquals("{}", ev2.get("targeted_interventions"));
        assertEquals(List.of(), InvariantesRespaldo.semanaConservada(jdbc, nuevo, siguiente.get(), ev2));
        assertEquals(InvariantesRespaldo.sesiones(jdbc, nuevo), InvariantesRespaldo.sesiones(jdbc, siguiente.get()),
                "sin motivo, la semana es identica a la anterior (con el sustituto)");
        assertFalse(contiene(siguiente.get(), dolio), "no se reincorpora automaticamente");
        assertTrue(activa(u, dolio));
    }

    // =======================================================================
    // A · RESPALDO sin dolor
    // =======================================================================

    @Test
    @Order(2)
    @DisplayName("A: sin dolor, el respaldo conserva la semana medida sin ninguna excepcion")
    void sinDolorConservaLaSemana() {
        long u = participante();
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var ids = sesionesDe(plan);
        // Lunes y miercoles hechos; el viernes, omitido por fatiga: orden de adaptar sin dolor.
        hacer(u, ids.get(0), -1);
        hacer(u, ids.get(1), -1);
        planes.skip(UserDetailsImpl.build(usuarios.findById(u).orElseThrow()), ids.get(2),
                new SkipWorkoutResource(SkipReason.FATIGUE));

        var publicado = ciclo.runFor(u, S1, S1.plusWeeks(1));
        var ev = ultimo(u);
        System.out.println("RESPALDO_A ev=" + ev);
        assertTrue(publicado.isPresent(), "sin dolor, el respaldo conserva y publica: " + ev);
        assertEquals("ADAPT_LOCALIZED", ev.get("decision"));
        assertFalse(String.valueOf(ev.get("targeted_interventions")).contains("SEGURIDAD"));
        assertEquals("RESPALDO", ev.get("status"));
        assertEquals("CONSERVED", ev.get("generation_source"));
        assertEquals(List.of(), InvariantesRespaldo.semanaConservada(jdbc, plan, publicado.get(), ev));
        assertEquals(InvariantesRespaldo.sesiones(jdbc, plan), InvariantesRespaldo.sesiones(jdbc, publicado.get()),
                "sin dolor y sin exclusiones, ninguna excepcion: copia exacta");
        assertFalse(String.valueOf(ev.get("conservation_report")).contains("REGENERACION_SEGURA"));
    }

    // =======================================================================
    // C · RESPALDO con dolor sin sustituto
    // =======================================================================

    @Test
    @Order(3)
    @DisplayName("C: con dolor y sin sustituto seguro, no se publica nada invalido ni se levanta la exclusion")
    void conDolorSinSustitutoNoPublicaNadaInvalido() {
        // Participantes deterministas hasta que uno lleva el 1373 en el plan (en la semana de referencia, el primero
        // que aparece es siempre el mismo: mismos user_id, mismas semillas).
        long u = -1, plan = -1;
        int sesion = -1;
        for (int intento = 0; intento < 40 && sesion < 0; intento++) {
            u = participante();
            plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
            var ids = sesionesDe(plan);
            for (int i = 0; i < ids.size() && sesion < 0; i++)
                if (ejerciciosDe(ids.get(i)).contains(SIN_SUSTITUTO)) sesion = i;
        }
        Assumptions.assumeTrue(sesion >= 0 || !REFERENCIA, "sin plan con el 1373 en esta semana");
        assertTrue(sesion >= 0, "premisa: en la semana de referencia algun participante lleva el 1373");
        ejecutar(u, sesionesDe(plan), Map.of(sesion, SIN_SUSTITUTO));

        var publicado = ciclo.runFor(u, S1, S1.plusWeeks(1));
        var ev = ultimo(u);
        System.out.println("RESPALDO_C u=" + u + " sesion=" + sesion + " publicado=" + publicado + " ev=" + ev);
        assertTrue(activa(u, SIN_SUSTITUTO), "la exclusion no se levanta para publicar");
        if (publicado.isEmpty()) {
            assertEquals(List.of(), InvariantesRespaldo.sinPublicar(jdbc, u, S1.plusWeeks(1), ev, List.of(SIN_SUSTITUTO)));
            assertTrue(String.valueOf(ev.get("rejection_reason")).contains("SUST-SEG-1"),
                    "el motivo dice que no hay sustituto SUST-SEG-1: " + ev.get("rejection_reason"));
            // Fase 2.1: el codigo lo puso la regla que decidio (columna V44_1), no se deduce del texto.
            var codigos = jdbc.queryForList("SELECT unnest(reason_codes) FROM plan_adaptation_attempts"
                    + " WHERE attempt_id = ?", String.class, ev.get("attempt_id"));
            assertEquals("SIN_SUSTITUTO_SEGURO", codigos.get(0), "codigo de fondo en V32: " + codigos);
            var api = adaptaciones.history(u).get(adaptaciones.history(u).size() - 1);
            assertEquals("NO_FACTIBLE_POR_SEGURIDAD", api.resultado(), "el estado de siempre se conserva");
            assertEquals("NO_PUBLICADO", api.estadoPublicacion());
            assertEquals("SIN_SUSTITUTO_SEGURO", api.motivoCodigo());
            assertNotNull(api.motivoTexto());
            assertEquals("SIN_SUSTITUTO_SEGURO", api.codigos().get(0));
            var w = sesionesDe(plan).get(sesion);
            assertNotNull(api.sesionAfectada(), "la sesion del dolor consta");
            assertEquals(w, api.sesionAfectada().plannedWorkoutId());
            var regenerado = jdbc.queryForList("SELECT reason_code FROM safe_regeneration_attempts WHERE user_id = ?",
                    String.class, u);
            if (!regenerado.isEmpty()) {
                assertNotNull(regenerado.get(0), "P-03 nivel 2 NO_FACTIBLE registra su codigo");
                assertTrue(api.codigos().contains(regenerado.get(0)), "y la API lo lista: " + api.codigos());
                assertTrue(api.restricciones().stream().anyMatch(r -> r.exerciseId() == SIN_SUSTITUTO
                        && "EXCLUSION_DOLOR".equals(r.tipo())), "la restriccion que impidio la semana: " + api);
            }
            System.out.println("RESPALDO_C_API " + api);
        } else {
            // P-03 nivel 2 (regeneracion con reglas, politica aprobada) encontro una semana valida: lo excluido no
            // vuelve. No se exige; se registra para el informe.
            assertFalse(contiene(publicado.get(), SIN_SUSTITUTO), "lo que dolio no vuelve");
        }
        if (REFERENCIA) assertTrue(publicado.isEmpty(), "en la semana de referencia: NO_FACTIBLE sin publicar " + ev);
    }

    // =======================================================================
    // Material
    // =======================================================================

    private long participante() {
        String email = "respaldo-dolor-" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 1, 15), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("70"), null, FitnessLevel.INTERMEDIATE, GoalType.GENERAL_FITNESS,
                "respaldo", TrainingLocation.GYM, (short) 3, List.of((short) 1, (short) 3, (short) 5), (short) 45,
                GIMNASIO, List.of(), "Ninguna", true, true, true, true, true));
        return u;
    }

    private List<Long> sesionesDe(long plan) {
        return jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date",
                Long.class, plan);
    }

    private List<Long> ejerciciosDe(long plannedWorkoutId) {
        return jdbc.queryForList("SELECT exercise_id FROM planned_workout_exercises WHERE planned_workout_id = ?"
                + " ORDER BY exercise_order", Long.class, plannedWorkoutId);
    }

    /** Todas las sesiones hechas; en las indicadas, el ejercicio indicado se deja por dolor. */
    private void ejecutar(long u, List<Long> ids, Map<Integer, Long> dolor) {
        for (int i = 0; i < ids.size(); i++) hacer(u, ids.get(i), dolor.getOrDefault(i, -1L));
    }

    private void hacer(long u, long w, long conDolor) {
        RelojPruebas.abrirVentanaDeEjecucion(jdbc, u);
        long s = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
        for (var f : jdbc.queryForList("SELECT pwe.planned_exercise_id, pwe.exercise_id, pwe.prescription_type,"
                + " pwe.planned_sets, pwe.planned_reps, pwe.planned_duration_seconds FROM planned_workout_exercises pwe"
                + " WHERE pwe.planned_workout_id = ? ORDER BY pwe.exercise_order", w)) {
            long pe = ((Number) f.get("planned_exercise_id")).longValue();
            if (((Number) f.get("exercise_id")).longValue() == conDolor) {
                sesiones.handle(new RecordSessionExerciseCommand(u, s, pe, (short) 0, null, null, null,
                        SkipReason.PAIN_OR_DISCOMFORT));
                continue;
            }
            boolean duracion = "DURATION".equals(f.get("prescription_type"));
            int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
            int total = series * ((Number) f.get(duracion ? "planned_duration_seconds" : "planned_reps")).intValue();
            sesiones.handle(new RecordSessionExerciseCommand(u, s, pe, (short) series, duracion ? null : total,
                    duracion ? total : null, null, null));
        }
        sesiones.handle(new FinishWorkoutSessionCommand(u, s, (short) 6, (short) 4, (short) 45));
    }

    private Map<String, Object> ultimo(long u) {
        return jdbc.queryForMap("SELECT attempt_id, decision, status, generation_source, ai_calls, targeted_interventions::text,"
                + " conservation_report::text, rejection_reason FROM plan_adaptation_attempts WHERE user_id = ?"
                + " ORDER BY attempt_id DESC LIMIT 1", u);
    }

    private boolean activa(long u, long ejercicio) {
        return jdbc.queryForObject("SELECT count(*) FROM user_safety_exclusions WHERE user_id = ? AND exercise_id = ?"
                + " AND status = 'ACTIVA'", Integer.class, u, ejercicio) > 0;
    }

    private boolean contiene(long plan, long ejercicio) {
        return jdbc.queryForObject("SELECT count(*) FROM planned_workout_exercises pwe JOIN planned_workouts pw"
                + " ON pw.planned_workout_id = pwe.planned_workout_id WHERE pw.plan_id = ? AND pwe.exercise_id = ?",
                Integer.class, plan, ejercicio) > 0;
    }
}
