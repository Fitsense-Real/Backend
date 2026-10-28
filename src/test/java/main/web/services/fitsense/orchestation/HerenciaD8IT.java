package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
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
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * D8 · LOS CINCO CASOS DE LA CAMPANA CON REPLICATE REAL, por el ciclo completo
 * (27/10/2026). Configuracion de produccion (progresion apagada).
 * <pre>
 * s2 viernes por falta de tiempo -> ACORTAR aplicado: el viernes queda bajo el piso de V20 (exento, D8)
 * s3 todo hecho                  -> copia (hereda)
 * s4 viernes por fatiga          -> REVISAR_CARGA: la IA sube una serie o lo devuelve igual; en los dos casos, RESPALDO identico
 * s5 miercoles demasiado dificil -> VARIANTE en el miercoles: la copia del viernes tiene que seguir exenta
 * s6 todo hecho                  -> copia
 * </pre>
 * Antes, s5 salia NO_FACTIBLE por V20 en el viernes. Y la herencia no le sirve al
 * dolor: un viernes con dolor que se conserva sigue rechazado.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.progression.enabled=false",
        // V36: el escenario (el viernes acortado bajo el piso de 31) esta calibrado con
        // la semana que genera el catalogo ANTERIOR; V36 cambia la seleccion. La
        // logica de D8 no depende del catalogo. Ver CATALOGO_V36.md, seccion 9.
        "spring.flyway.target=35"
})
@DisplayName("D8 · la herencia sigue tras una senalada sin cambios; el dolor no la aprovecha")
class HerenciaD8IT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoBean ReplicateSessionFixGenerator ia;

    @Autowired WeeklyCycleService ciclo;
    @Autowired PlanningContextFacade planning;
    @Autowired UserProfileCommandService perfiles;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired TrainingPlanController planes;
    @Autowired UserRepository usuarios;
    @Autowired SessionDurationEstimator estimador;
    @Autowired JdbcTemplate jdbc;

    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final LocalDate S1 = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
    static final List<String> EQUIPAMIENTO = List.of("dumbbell", "barbell", "cable", "leverage machine", "bench", "band");
    static final int PISO = 45 * 70 / 100;

    enum Dia { HECHO, TIEMPO, FATIGA, DIFICIL, DOLOR }

    /** Que hace la IA simulada en una senalada que no es ACORTAR. */
    enum Modo { IGUAL, SUBE }

    private Modo modo = Modo.IGUAL;

    @BeforeEach
    void iaSimulada() {
        reset(ia);
        when(ia.isEnabled()).thenReturn(true);
        when(ia.modelName()).thenReturn("simulado");
        when(ia.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{}", "salida-simulada"));
        when(ia.interpretar(anyString(), any())).thenAnswer(inv -> corregir(inv.getArgument(1)));
    }

    @Test
    @DisplayName("ACORTAR -> copia -> fatiga con RESPALDO identico -> la copia del viernes conserva la exencion")
    void respaldoIdenticoConservaD8() throws Exception {
        recorrer(Modo.SUBE, "RESPALDO");
    }

    /**
     * La IA devuelve el viernes identico. Como SENALADA esa semana no tiene la exencion de
     * D8 (V8C solo la da a las copias no senaladas), V20 rechaza la propuesta y el respaldo
     * publica la misma sesion: RESPALDO, con el viernes en no_effect. Es el comportamiento de
     * antes, fuera del alcance de este cambio; lo que se comprueba es que la herencia siga.
     */
    @Test
    @DisplayName("ACORTAR -> copia -> fatiga con la IA devolviendolo igual (acaba en RESPALDO) -> la copia conserva la exencion")
    void propuestaIdenticaConservaD8() throws Exception {
        recorrer(Modo.IGUAL, "RESPALDO");
    }

    @Test
    @DisplayName("el dolor no aprovecha D8: el viernes acortado que dolio, conservado tal cual, no se publica")
    void dolorNoAprovechaD8() throws Exception {
        modo = Modo.IGUAL;
        long u = participante();
        long plan = acortado(u);
        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        plan = ciclo.runFor(u, S1.plusWeeks(1), S1.plusWeeks(2)).orElseThrow();   // copia exenta
        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.DOLOR);
        var r = ciclo.runFor(u, S1.plusWeeks(2), S1.plusWeeks(3));
        assertTrue(r.isEmpty(), "volver a publicar el viernes que dolio no es una solucion segura");
        assertEquals("NO_FACTIBLE", ultimo(u).get("status"));
        assertNotNull(ultimo(u).get("rejection_reason"));
        assertSinViolaciones();
    }

    private void recorrer(Modo enLaFatiga, String estadoFatiga) throws Exception {
        long u = participante();
        long plan = acortado(u);
        int minutos = viernes(plan);

        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.HECHO);                       // s3: copia
        plan = publicar(u, 1, "SIN_ORDEN");
        assertEquals(minutos, viernes(plan));

        modo = enLaFatiga;
        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.FATIGA);                      // s4: fatiga en el viernes
        plan = publicar(u, 2, estadoFatiga);
        assertEquals(minutos, viernes(plan), "el viernes publicado es identico");

        modo = Modo.IGUAL;
        ejecutar(u, plan, Dia.HECHO, Dia.DIFICIL, Dia.HECHO);                     // s5: el caso que fallaba
        plan = publicar(u, 3, "SIN_EFECTO");
        assertEquals(minutos, viernes(plan), "la copia del viernes conserva la exencion de V20");

        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.HECHO);                       // s6: y sigue
        plan = publicar(u, 4, "SIN_ORDEN");
        assertEquals(minutos, viernes(plan));
        assertSinViolaciones();
    }

    /** s2: el viernes omitido por falta de tiempo; ACORTAR lo deja bajo el piso de V20. */
    private long acortado(long u) {
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.TIEMPO);
        long nuevo = publicar(u, 0, "APLICADA");
        assertTrue(viernes(nuevo) < PISO, "premisa: el viernes acortado (" + viernes(nuevo) + ") bajo el piso " + PISO);
        return nuevo;
    }

    private long publicar(long u, int k, String estado) {
        var r = ciclo.runFor(u, S1.plusWeeks(k), S1.plusWeeks(k + 1));
        assertTrue(r.isPresent(), "sin semana publicada en la " + (k + 2) + ": " + ultimo(u));
        assertEquals(estado, ultimo(u).get("status"), "semana " + (k + 2) + ": " + ultimo(u));
        return r.get();
    }

    // =======================================================================
    // IA simulada
    // =======================================================================

    private List<CorrectedSession> corregir(PlanGenerationContext ctx) {
        var salida = new ArrayList<CorrectedSession>();
        for (var senalada : ctx.adjustment().targetedSessions()) {
            long id = senalada.plannedWorkoutId();
            var f = jdbc.queryForMap("SELECT scheduled_date, focus_code, expected_duration_minutes FROM planned_workouts"
                    + " WHERE planned_workout_id = ?", id);
            var ejercicios = jdbc.query("SELECT pwe.exercise_id, pwe.prescription_type, pwe.planned_sets,"
                            + " pwe.planned_reps, pwe.planned_duration_seconds, pwe.rest_seconds, pwe.target_load_kg,"
                            + " e.movement_pattern FROM planned_workout_exercises pwe"
                            + " JOIN exercises e ON e.exercise_id = pwe.exercise_id"
                            + " WHERE pwe.planned_workout_id = ? ORDER BY pwe.exercise_order",
                    (rs, k) -> Map.entry(new PlanDraft.DraftExercise(rs.getLong(1),
                            PrescriptionType.valueOf(rs.getString(2)), rs.getObject(3, Short.class),
                            rs.getObject(4, Short.class), rs.getObject(5, Integer.class), rs.getBigDecimal(7),
                            rs.getObject(6, Short.class), null), String.valueOf(rs.getString(8))), id);
            var lista = new ArrayList<>(ejercicios.stream().map(Map.Entry::getKey).toList());
            var fecha = ((java.sql.Date) f.get("scheduled_date")).toLocalDate().plusWeeks(1);
            int minutos = ((Number) f.get("expected_duration_minutes")).intValue();
            if (senalada.intervencion() == TipoIntervencion.ACORTAR_O_REORGANIZAR) {
                for (int k = ejercicios.size() - 1; k >= 0; k--)
                    if (MandatorySlot.ofMovementPattern(ejercicios.get(k).getValue()).isEmpty()) {
                        lista.remove(k);
                        break;
                    }
                minutos = estimador.estimateMinutes(new PlanDraft.DraftWorkout(fecha,
                        WorkoutFocus.valueOf((String) f.get("focus_code")), "x", 0, lista), ctx.prescription());
            } else if (modo == Modo.SUBE) {
                // Lo que hizo la IA real tras la fatiga: subir una serie. V8C lo rechaza en los 3 intentos.
                var e = lista.get(0);
                lista.set(0, new PlanDraft.DraftExercise(e.exerciseId(), e.prescriptionType(),
                        (short) (e.plannedSets() + 1), e.plannedReps(), e.plannedDurationSeconds(), e.targetLoadKg(),
                        e.restSeconds(), null));
            }
            salida.add(new CorrectedSession(id, fecha, minutos, lista, "propuesta simulada"));
        }
        return salida;
    }

    // =======================================================================
    // Material
    // =======================================================================

    private long participante() {
        String email = "herencia-d8-" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 5, 10), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("68"), null, FitnessLevel.INTERMEDIATE, GoalType.GAIN_MUSCLE,
                "herencia d8", TrainingLocation.GYM, (short) 3, List.of((short) 1, (short) 3, (short) 5), (short) 45,
                EQUIPAMIENTO, List.of(), "Ninguna", true, true, true, true, true));
        return u;
    }

    private int viernes(long plan) {
        return jdbc.queryForObject("SELECT expected_duration_minutes FROM planned_workouts WHERE plan_id = ?"
                + " AND extract(isodow FROM scheduled_date) = 5", Integer.class, plan);
    }

    private Map<String, Object> ultimo(long u) {
        return jdbc.queryForMap("SELECT decision, status, ai_calls, targeted_interventions::text, rejection_reason"
                + " FROM plan_adaptation_attempts WHERE user_id = ? ORDER BY attempt_id DESC LIMIT 1", u);
    }

    private void ejecutar(long u, long planId, Dia... dias) {
        var principal = UserDetailsImpl.build(usuarios.findById(u).orElseThrow());
        var ids = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                + " ORDER BY scheduled_date", Long.class, planId);
        for (int i = 0; i < ids.size(); i++) {
            long w = ids.get(i);
            switch (dias[i]) {
                case HECHO -> {
                    long s = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
                    for (var f : jdbc.queryForList("SELECT planned_exercise_id, prescription_type, planned_sets,"
                            + " planned_reps, planned_duration_seconds FROM planned_workout_exercises"
                            + " WHERE planned_workout_id = ?", w)) {
                        boolean duracion = "DURATION".equals(f.get("prescription_type"));
                        int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
                        int total = series * ((Number) f.get(duracion ? "planned_duration_seconds" : "planned_reps")).intValue();
                        sesiones.handle(new RecordSessionExerciseCommand(u, s, ((Number) f.get("planned_exercise_id")).longValue(),
                                (short) series, duracion ? null : total, duracion ? total : null, null, null));
                    }
                    sesiones.handle(new FinishWorkoutSessionCommand(u, s, (short) 6, (short) 4, (short) 45));
                }
                case TIEMPO -> planes.skip(principal, w, new SkipWorkoutResource(SkipReason.LACK_OF_TIME));
                case FATIGA -> planes.skip(principal, w, new SkipWorkoutResource(SkipReason.FATIGUE));
                case DIFICIL -> planes.skip(principal, w, new SkipWorkoutResource(SkipReason.TOO_DIFFICULT));
                case DOLOR -> planes.skip(principal, w, new SkipWorkoutResource(SkipReason.PAIN_OR_DISCOMFORT));
            }
        }
    }

    private void assertSinViolaciones() throws Exception {
        var texto = Files.readString(Path.of("docs/FitSense/consultas/linaje_v31_v32.sql")).lines()
                .filter(l -> !l.stripLeading().startsWith("--")).collect(Collectors.joining("\n"));
        assertEquals(List.of(), jdbc.queryForList(texto.split(";")[0].strip()));
    }
}
