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
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
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
import static org.mockito.Mockito.when;

/**
 * Verifica las premisas de la restauracion del diseno v0.3 (§3.2) sobre el
 * sistema ACTUAL, sin implementar nada: que cada derivacion publicada por el
 * ciclo tiene un unico intento V32 y que cada paso del linaje V31 se clasifica
 * en un unico tipo. Ejecuta {@code docs/FitSense/consultas/linaje_v31_v32.sql},
 * la misma consulta de solo lectura prevista para la base real.
 * <p>
 * Varias semanas encadenadas por el ciclo de produccion, con todos los
 * desenlaces que publican o no publican: APLICADA, SIN_ORDEN (NO_CHANGE e
 * INSUFFICIENT_DATA), SIN_EFECTO, RESPALDO y NO_FACTIBLE. Solo se simula la IA
 * de correccion, como en {@link AdaptacionCompletaIT}. Nada llega a Replicate.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
class LinajeIntegridadIT {

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
    static final Path CONSULTA = Path.of("docs/FitSense/consultas/linaje_v31_v32.sql");

    enum Dia { HECHO, OMITIDO, HORARIO, DOLOR, NADA }

    @Test
    @DisplayName("el linaje V31 + V32 que publica el ciclo actual se clasifica sin ambiguedad; sin evidencia, se detecta")
    void linaje() throws Exception {
        configurarIa(true);

        // A: cuatro semanas encadenadas. APLICADA (reprogramada) -> SIN_ORDEN (NO_CHANGE)
        //    -> SIN_ORDEN (INSUFFICIENT_DATA). Reprogramar no cambia la duracion.
        long a = participante(GoalType.GAIN_MUSCLE, List.of((short) 1, (short) 3, (short) 5, (short) 6));
        long a1 = planning.generateWeeklyPlan(a, S1, PlanAdjustment.none(), null, null).orElseThrow();
        ejecutar(a, a1, Dia.HECHO, Dia.HECHO, Dia.HORARIO);
        long a2 = ciclo.runFor(a, S1, S1.plusWeeks(1)).orElseThrow();
        ejecutar(a, a2, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        long a3 = ciclo.runFor(a, S1.plusWeeks(1), S1.plusWeeks(2)).orElseThrow();
        ejecutar(a, a3, Dia.HECHO, Dia.NADA, Dia.NADA);
        long a4 = ciclo.runFor(a, S1.plusWeeks(2), S1.plusWeeks(3)).orElseThrow();

        // B: fuerza sin relleno -> SIN_EFECTO.
        long b = participante(GoalType.INCREASE_STRENGTH);
        long b1 = planning.generateWeeklyPlan(b, S1, PlanAdjustment.none(), null, null).orElseThrow();
        ejecutar(b, b1, Dia.HECHO, Dia.HECHO, Dia.OMITIDO);
        ciclo.runFor(b, S1, S1.plusWeeks(1)).orElseThrow();

        // E: D8. Acortar por falta de tiempo deja el viernes bajo el piso de V20 (exento esa
        //    semana). Sus copias heredan la exencion: tres ciclos seguidos de copia se publican.
        long e = participante(GoalType.GAIN_MUSCLE);
        long e1 = planning.generateWeeklyPlan(e, S1, PlanAdjustment.none(), null, null).orElseThrow();
        ejecutar(e, e1, Dia.HECHO, Dia.HECHO, Dia.OMITIDO);
        long e2 = ciclo.runFor(e, S1, S1.plusWeeks(1)).orElseThrow();
        int piso = 45 * 70 / 100;   // V20: 70 % de 45, en minutos enteros
        int acortado = duracionViernes(e2);
        assertTrue(acortado < piso, "premisa: el viernes acortado (" + acortado + ") queda bajo el piso " + piso);
        long previo = e2;
        var copias = new ArrayList<Long>();
        for (int k = 1; k <= 3; k++) {
            ejecutar(e, previo, Dia.HECHO, Dia.HECHO, Dia.HECHO);
            var siguiente = ciclo.runFor(e, S1.plusWeeks(k), S1.plusWeeks(k + 1));
            var ultimo = jdbc.queryForList("SELECT decision, status, rejection_reason FROM plan_adaptation_attempts"
                    + " WHERE user_id = ? ORDER BY attempt_id DESC LIMIT 1", e);
            assertTrue(siguiente.isPresent(), "la copia " + k + " del viernes acortado no se publico: " + ultimo);
            assertEquals("SIN_ORDEN", ultimo.get(0).get("status"));
            assertEquals(acortado, duracionViernes(siguiente.get()), "la copia " + k + " no conserva el acortado");
            copias.add(siguiente.get());
            previo = siguiente.get();
        }
        System.out.println("LINAJE d8_copias planes=" + copias + " viernes=" + acortado + " min (piso " + piso + ")");

        // C: sin IA -> RESPALDO. D: dolor sin IA -> P-03 (31/10/2026): sustitucion determinista SUST-SEG-1 en el
        // mismo ciclo, RESPALDO con plan; antes NO_FACTIBLE sin plan. Su linaje V31/V32 tambien se audita aqui.
        configurarIa(false);
        long c = participante(GoalType.LOSE_WEIGHT);
        long c1 = planning.generateWeeklyPlan(c, S1, PlanAdjustment.none(), null, null).orElseThrow();
        ejecutar(c, c1, Dia.HECHO, Dia.HECHO, Dia.OMITIDO);
        ciclo.runFor(c, S1, S1.plusWeeks(1)).orElseThrow();
        long d = participante(GoalType.GAIN_MUSCLE);
        long d1 = planning.generateWeeklyPlan(d, S1, PlanAdjustment.none(), null, null).orElseThrow();
        ejecutar(d, d1, Dia.HECHO, Dia.HECHO, Dia.DOLOR);
        long d2 = ciclo.runFor(d, S1, S1.plusWeeks(1)).orElseThrow();
        var dolieron = jdbc.queryForList("SELECT pwe.exercise_id FROM planned_workout_exercises pwe JOIN planned_workouts pw"
                + " ON pw.planned_workout_id = pwe.planned_workout_id WHERE pw.plan_id = ? AND pw.skip_reason ="
                + " 'PAIN_OR_DISCOMFORT'", Long.class, d1);
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM planned_workout_exercises pwe JOIN planned_workouts pw"
                + " ON pw.planned_workout_id = pwe.planned_workout_id WHERE pw.plan_id = ? AND pwe.exercise_id = ANY(?)",
                Integer.class, d2, dolieron.toArray(new Long[0])), "P-03 no republica lo que dolio");

        var estados = jdbc.queryForList("SELECT user_id, status FROM plan_adaptation_attempts ORDER BY attempt_id")
                .stream().map(r -> r.get("user_id") + ":" + r.get("status")).toList();
        System.out.println("LINAJE intentos " + estados);
        assertTrue(estados.containsAll(List.of(a + ":APLICADA", a + ":SIN_ORDEN", b + ":SIN_EFECTO",
                c + ":RESPALDO", d + ":RESPALDO", e + ":APLICADA", e + ":SIN_ORDEN")), estados.toString());
        assertFalse(estados.contains(e + ":NO_FACTIBLE"), "D8: el acortado no debe dejar semanas sin plan");

        // 1. Sin violaciones; todos los pasos clasificados.
        var consultas = consultas();
        var violaciones = jdbc.queryForList(consultas[0]);
        var clasificacion = jdbc.queryForList(consultas[1]);
        System.out.println("LINAJE violaciones " + violaciones);
        System.out.println("LINAJE tipos " + clasificacion.stream()
                .collect(Collectors.groupingBy(r -> String.valueOf(r.get("tipo")), TreeMap::new, Collectors.counting())));
        assertEquals(List.of(), violaciones);
        assertTrue(clasificacion.stream().noneMatch(r -> "SIN_CLASIFICAR".equals(r.get("tipo"))));
        assertTrue(clasificacion.stream().anyMatch(r -> String.valueOf(r.get("tipo")).startsWith("ADAPTADA:")));

        // 2. El recorrido hacia atras desde la semana 4 de A llega a la adaptacion de la semana 2
        //    pasando solo por copias, y su origen es la sesion de la semana 1.
        var tipoPorDerivada = new HashMap<Long, String>();
        var origenDe = new HashMap<Long, Long>();
        clasificacion.forEach(r -> {
            tipoPorDerivada.put(((Number) r.get("derivada")).longValue(), String.valueOf(r.get("tipo")));
            origenDe.put(((Number) r.get("derivada")).longValue(), ((Number) r.get("origen")).longValue());
        });
        var adaptadaEnA2 = clasificacion.stream()
                .filter(r -> ((Number) r.get("plan_derivado")).longValue() == a2
                        && String.valueOf(r.get("tipo")).startsWith("ADAPTADA:"))
                .map(r -> ((Number) r.get("derivada")).longValue()).findFirst().orElseThrow();
        long sesionEnA4 = jdbc.queryForObject("SELECT w4.planned_workout_id FROM planned_workouts w4"
                + " JOIN planned_workouts w3 ON w3.planned_workout_id = w4.source_planned_workout_id"
                + " WHERE w4.plan_id = ? AND w3.source_planned_workout_id = ?", Long.class, a4, adaptadaEnA2);
        var camino = new ArrayList<String>();
        long actual = sesionEnA4;
        while (tipoPorDerivada.containsKey(actual)) {
            camino.add(actual + "=" + tipoPorDerivada.get(actual));
            if (!tipoPorDerivada.get(actual).equals("COPIA")) break;
            actual = origenDe.get(actual);
        }
        long referencia = origenDe.get(actual);
        System.out.println("LINAJE recorrido " + camino + " -> referencia " + referencia);
        assertEquals(adaptadaEnA2, actual, "el recorrido no llega a la adaptacion");
        assertEquals(a1, jdbc.queryForObject("SELECT plan_id FROM planned_workouts WHERE planned_workout_id = ?",
                Long.class, referencia), "la referencia no esta en la semana 1");

        // 3. Una derivacion sin evidencia V32 se detecta (y por tanto no se restauraria).
        jdbc.update("DELETE FROM plan_adaptation_attempts WHERE resulting_plan_id = ?", a3);
        var sinEvidencia = jdbc.queryForList(consultas[0]);
        System.out.println("LINAJE sin evidencia " + sinEvidencia);
        assertTrue(sinEvidencia.stream().anyMatch(r -> "PLAN_DERIVADO_SIN_INTENTO_UNICO".equals(r.get("violacion"))
                && ((Number) r.get("id")).longValue() == a3), sinEvidencia.toString());

        // F: D8 sin evidencia. Sobre E, tras sus tres copias, falta el intento V32 que publico el
        //    acortado (tres semanas atras): el linaje no se puede clasificar, no hay exencion y V20
        //    rechaza la copia siguiente.
        configurarIa(true);
        assertEquals(1, jdbc.update("DELETE FROM plan_adaptation_attempts WHERE resulting_plan_id = ?", e2));
        ejecutar(e, previo, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        assertTrue(ciclo.runFor(e, S1.plusWeeks(4), S1.plusWeeks(5)).isEmpty(),
                "sin evidencia V32 no debia heredarse la exencion");
        var motivoF = jdbc.queryForObject("SELECT rejection_reason FROM plan_adaptation_attempts WHERE user_id = ?"
                + " ORDER BY attempt_id DESC LIMIT 1", String.class, e);
        System.out.println("LINAJE d8_sin_evidencia " + motivoF);
        assertTrue(motivoF.startsWith("V20"), motivoF);
    }

    private int duracionViernes(long planId) {
        return jdbc.queryForObject("SELECT expected_duration_minutes FROM planned_workouts WHERE plan_id = ?"
                + " AND extract(isodow FROM scheduled_date) = 5", Integer.class, planId);
    }

    private void configurarIa(boolean habilitada) {
        org.mockito.Mockito.reset(ia);
        when(ia.isEnabled()).thenReturn(habilitada);
        when(ia.modelName()).thenReturn("simulado");
        when(ia.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{}", "salida-simulada"));
        when(ia.interpretar(anyString(), any())).thenAnswer(inv -> corregir(inv.getArgument(1)));
    }

    /**
     * La IA simulada, solo con lo que permite cada intervencion: REPROGRAMAR_DIAS mueve la sesion
     * al primer dia disponible libre de la semana; ACORTAR retira el ultimo relleno; el resto, igual.
     */
    private List<CorrectedSession> corregir(PlanGenerationContext ctx) {
        var salida = new ArrayList<CorrectedSession>();
        for (var senalada : ctx.adjustment().targetedSessions()) {
            long id = senalada.plannedWorkoutId();
            var f = jdbc.queryForMap("SELECT scheduled_date, focus_code FROM planned_workouts"
                    + " WHERE planned_workout_id = ?", id);
            var ejercicios = jdbc.query("SELECT pwe.exercise_id, pwe.prescription_type, pwe.planned_sets,"
                            + " pwe.planned_reps, pwe.planned_duration_seconds, pwe.rest_seconds, e.movement_pattern"
                            + " FROM planned_workout_exercises pwe JOIN exercises e ON e.exercise_id = pwe.exercise_id"
                            + " WHERE pwe.planned_workout_id = ? ORDER BY pwe.exercise_order",
                    (rs, k) -> Map.entry(new PlanDraft.DraftExercise(rs.getLong(1),
                            PrescriptionType.valueOf(rs.getString(2)), rs.getObject(3, Short.class),
                            rs.getObject(4, Short.class), rs.getObject(5, Integer.class), null,
                            rs.getObject(6, Short.class), null), String.valueOf(rs.getString(7))), id);
            var lista = new ArrayList<>(ejercicios);
            var intervencion = senalada.intervencion();
            if (intervencion == main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion
                    .ACORTAR_O_REORGANIZAR)
                for (int k = lista.size() - 1; k >= 0; k--)
                    if (MandatorySlot.ofMovementPattern(lista.get(k).getValue()).isEmpty()) {
                        lista.remove(k);
                        break;
                    }
            var fecha = ((java.sql.Date) f.get("scheduled_date")).toLocalDate().plusWeeks(1);
            if (intervencion == main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion
                    .REPROGRAMAR_DIAS) {
                var lunes = TrainingWeek.containing(fecha).startDate();
                var ocupados = jdbc.queryForList("SELECT w.scheduled_date FROM planned_workouts w JOIN planned_workouts o"
                        + " ON o.plan_id = w.plan_id WHERE o.planned_workout_id = ?", java.sql.Date.class, id)
                        .stream().map(x -> x.toLocalDate().plusWeeks(1)).toList();
                for (short dia : ctx.profile().availableDays()) {
                    var candidata = lunes.plusDays(dia - 1);
                    if (!ocupados.contains(candidata)) {
                        fecha = candidata;
                        break;
                    }
                }
            }
            var borrador = lista.stream().map(Map.Entry::getKey).toList();
            int minutos = estimador.estimateMinutes(new PlanDraft.DraftWorkout(fecha,
                    WorkoutFocus.valueOf((String) f.get("focus_code")), "x", 0, borrador), ctx.prescription());
            salida.add(new CorrectedSession(id, fecha, minutos, borrador, "retira un relleno"));
        }
        return salida;
    }

    private long participante(GoalType objetivo) {
        return participante(objetivo, List.of((short) 1, (short) 3, (short) 5));
    }

    private long participante(GoalType objetivo, List<Short> dias) {
        String email = "linaje" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long userId = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(userId, LocalDate.of(1992, 5, 10), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("68"), null, FitnessLevel.INTERMEDIATE, objetivo,
                "prueba de linaje", TrainingLocation.GYM, (short) 3,
                dias, (short) 45, EQUIPAMIENTO,
                List.of(), "Ninguna", true, true, true, true, true));
        return userId;
    }

    private void ejecutar(long u, long planId, Dia... dias) {
        var principal = UserDetailsImpl.build(usuarios.findById(u).orElseThrow());
        var ids = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                + " ORDER BY scheduled_date", Long.class, planId);
        for (int i = 0; i < ids.size(); i++) {
            long w = ids.get(i);
            switch (dias[i]) {
                case HECHO -> {
                    long sesion = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
                    for (var f : jdbc.queryForList("SELECT planned_exercise_id, prescription_type, planned_sets,"
                            + " planned_reps, planned_duration_seconds FROM planned_workout_exercises"
                            + " WHERE planned_workout_id = ? ORDER BY exercise_order", w)) {
                        boolean duracion = "DURATION".equals(f.get("prescription_type"));
                        int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
                        sesiones.handle(new RecordSessionExerciseCommand(u, sesion,
                                ((Number) f.get("planned_exercise_id")).longValue(), (short) series,
                                duracion ? null : series * ((Number) f.get("planned_reps")).intValue(),
                                duracion ? series * ((Number) f.get("planned_duration_seconds")).intValue() : null,
                                null, null));
                    }
                    sesiones.handle(new FinishWorkoutSessionCommand(u, sesion, (short) 6, (short) 4, (short) 45));
                }
                case OMITIDO -> saltoPorFaltaDeTiempo(principal, w);
                case HORARIO -> planes.skip(principal, w, new SkipWorkoutResource(SkipReason.SCHEDULE_CHANGE));
                case DOLOR -> planes.skip(principal, w, new SkipWorkoutResource(SkipReason.PAIN_OR_DISCOMFORT));
                case NADA -> { }
            }
        }
    }

    /** Las dos consultas del archivo: violaciones y clasificacion. */
    private static String[] consultas() throws Exception {
        var texto = Files.readString(CONSULTA).lines()
                .filter(l -> !l.stripLeading().startsWith("--"))
                .collect(Collectors.joining("\n"));
        var partes = Arrays.stream(texto.split(";")).map(String::strip).filter(s -> !s.isEmpty()).toArray(String[]::new);
        assertEquals(2, partes.length, "el archivo debe tener dos consultas");
        return partes;
    }

    /**
     * ADAPT-2 (DISENO_ADAPTACION_CONTEXTUAL v0.3): una omision por falta de tiempo SIN contexto ya
     * no se acorta (D1). Este escenario prueba lo que pasa tras un recorte, asi que la persona
     * declara que suele tener menos tiempo del que dura la sesion (un minuto menos).
     */
    private org.springframework.http.ResponseEntity<Void> saltoPorFaltaDeTiempo(
            main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl principal, long w) {
        int minutos = jdbc.queryForObject("SELECT expected_duration_minutes FROM planned_workouts"
                + " WHERE planned_workout_id = ?", Integer.class, w);
        return planes.skip(principal, w, new SkipWorkoutResource(SkipReason.LACK_OF_TIME,
                main.web.services.fitsense.shared.domain.model.valueobjects.ContextoFaltaDeTiempo.MENOS_TIEMPO_HABITUAL,
                Math.max(15, minutos - 1)));
    }
}
