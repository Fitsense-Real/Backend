package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.application.internal.commandservices.PainMemoryService;
import main.web.services.fitsense.planning.application.internal.queryservices.AdaptationOutcomeQueryService;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateTrainingPlanGenerator;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.planning.interfaces.rest.TrainingPlanController;
import main.web.services.fitsense.planning.interfaces.rest.resources.SkipWorkoutResource;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;
import main.web.services.fitsense.shared.domain.model.valueobjects.ContextoFaltaDeTiempo;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CAMPANA FITSENSE_STRESS_P123_COHORT_02 · 10 usuarios sinteticos NUEVOS (N01-N10) x 10 semanas, con extension
 * focalizada de N01 y N09 hasta S16 para la reincorporacion. Politicas P-01, P-02 y P-03 activas.
 * <p>
 * <b>Sin gasto (P-05).</b> Ninguna llamada a Replicate: la generacion inicial es el motor de reglas y la IA de
 * correccion es un DOBLE con una politica fijada ANTES de ejecutar (ver {@link #iaSimulada}). Todo lo que sale de
 * ese doble se etiqueta SIMULADA. Los servicios, el ciclo semanal, los validadores y la base son los reales.
 * <p>
 * <b>Aislamiento.</b> PostgreSQL efimero de Testcontainers; se comprueba la URL efectiva antes de empezar. La
 * progresion se enciende SOLO en este contexto de prueba.
 * <p>
 * Opt-in: {@code -Dfitsense.campana.p123=true}. Deja la evidencia en {@code docs/FitSense/campana_p123/evidencia}.
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.campana.p123", matches = "true")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.progression.enabled=true"
})
class CampanaP123Cohorte02IT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static final String COHORTE = "FITSENSE_STRESS_P123_COHORT_02";
    static final LocalDate S1 = LocalDate.of(2026, 11, 2);
    static final int SEMANAS = 10;
    static final int EXTENSION = 16;
    static final Path SALIDA = Path.of("docs/FitSense/campana_p123/evidencia");
    static final List<String> GIMNASIO = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine", "bench", "band");
    /** N04: fijados tras inspeccionar el catalogo (campana p123, 31/10/2026). No cambian durante la ejecucion. */
    static final Long[] N04_FIJOS = {};
    static final List<Long> N04_BLOQUEADOS = bloqueadosN04();

    @MockitoBean ReplicateSessionFixGenerator ia;
    @MockitoSpyBean ReplicateTrainingPlanGenerator iaGeneracion;

    @Autowired WeeklyCycleService ciclo;
    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired UserProfileCommandService perfiles;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired TrainingPlanController planes;
    @Autowired UserRepository usuarios;
    @Autowired PainMemoryService dolor;
    @Autowired AdaptationOutcomeQueryService resultados;
    @Autowired PlanDraftValidator validador;
    @Autowired SessionDurationEstimator estimador;
    @Autowired JdbcTemplate jdbc;

    // ------------------------------------------------------------------ guion

    /** Lo que hace la persona en una sesion. */
    enum D { HECHO, PARCIAL_TIEMPO, PARCIAL_FATIGA, CON_FATIGA, DIFICIL, CON_DOLOR, OMITE_MOTIVACION, OMITE_HORARIO,
        OMITE_OTRO, OMITE_TIEMPO_CTX, NADA }

    /** Como responde la IA simulada ante una sesion senalada por dolor. */
    enum Dolor { SUSTITUYE, INVALIDA }

    record Perfil(String id, GoalType objetivo, FitnessLevel nivel, List<Short> dias, int minutos,
                  TrainingLocation lugar, List<String> equipo, List<Long> bloqueados, boolean impacto, boolean suelo,
                  boolean rodillas, boolean suspension, boolean porEncima, Dolor dolor, String nota) {}

    /** El guion: por semana, lo que pasa en cada sesion (si la semana tiene menos sesiones, se recorta). */
    static Map<Integer, List<D>> guion(String id, int sesiones) {
        var g = new TreeMap<Integer, List<D>>();
        for (int s = 1; s <= EXTENSION; s++) g.put(s, todas(sesiones, D.HECHO));
        switch (id) {
            case "N01", "N09" -> g.put(1, conUna(sesiones, D.HECHO, 0, D.PARCIAL_TIEMPO));
            case "N02" -> {
                g.put(3, conDos(sesiones, D.HECHO, 0, D.CON_FATIGA, 1, D.PARCIAL_FATIGA));
                g.put(5, conUna(sesiones, D.HECHO, 0, D.OMITE_OTRO));
            }
            case "N03" -> {
                g.put(2, todas(sesiones, D.DIFICIL));
                g.put(3, conDos(sesiones, D.HECHO, 0, D.PARCIAL_TIEMPO, 1, D.PARCIAL_TIEMPO));
                g.put(4, conDos(sesiones, D.HECHO, 0, D.OMITE_MOTIVACION, 1, D.OMITE_MOTIVACION));
            }
            case "N04" -> {
                g.put(2, conDos(sesiones, D.HECHO, 0, D.OMITE_HORARIO, 1, D.OMITE_HORARIO));
                g.put(5, conUna(sesiones, D.HECHO, 0, D.PARCIAL_TIEMPO));
            }
            case "N05" -> g.put(2, todas(sesiones, D.DIFICIL));
            case "N06" -> g.put(4, conUna(sesiones, D.HECHO, 0, D.CON_DOLOR));
            case "N07" -> {
                g.put(3, todas(sesiones, D.NADA));
                g.put(4, todas(sesiones, D.PARCIAL_TIEMPO));
            }
            case "N08" -> g.put(2, conUna(sesiones, D.HECHO, 0, D.CON_DOLOR));
            case "N10" -> {
                g.put(2, conUna(sesiones, D.HECHO, 0, D.PARCIAL_TIEMPO));
                var baja = todas(sesiones, D.HECHO);
                for (int i = 0; i < Math.min(3, sesiones); i++) baja.set(i, D.OMITE_MOTIVACION);
                g.put(3, baja);
                g.put(4, conUna(sesiones, D.HECHO, 0, D.OMITE_TIEMPO_CTX));
            }
            default -> { }
        }
        return g;
    }

    static List<Perfil> cohorte() {
        short L = 1, M = 2, X = 3, J = 4, V = 5, S = 6, Dm = 7;
        return List.of(
                new Perfil("N01", GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, List.of(M, S), 30,
                        TrainingLocation.GYM, GIMNASIO, List.of(), true, true, true, true, true, Dolor.SUSTITUYE,
                        "candidato principal a retirada y reincorporacion"),
                new Perfil("N02", GoalType.GAIN_MUSCLE, FitnessLevel.ADVANCED, List.of(L, M, J, S, Dm), 60,
                        TrainingLocation.HOME, List.of("body weight", "dumbbell", "band"), List.of(), true, true, true, true, true,
                        Dolor.SUSTITUYE, "casa: peso corporal, mancuernas y bandas"),
                new Perfil("N03", GoalType.LOSE_WEIGHT, FitnessLevel.BEGINNER, List.of(L, M, X, J, V, S, Dm), 30,
                        TrainingLocation.HOME, List.of("body weight", "band"), List.of(), false, true, true, true, true, Dolor.SUSTITUYE,
                        "casa: bandas y peso corporal, sin impacto; 7 dias y tope 4"),
                new Perfil("N04", GoalType.LOSE_WEIGHT, FitnessLevel.INTERMEDIATE, List.of(L, M, X, V, S, Dm), 45,
                        TrainingLocation.GYM, GIMNASIO, N04_BLOQUEADOS, true, true, true, true, true, Dolor.SUSTITUYE,
                        "gimnasio con ejercicios bloqueados"),
                new Perfil("N05", GoalType.INCREASE_STRENGTH, FitnessLevel.BEGINNER, List.of(Dm), 60,
                        TrainingLocation.HOME, List.of("body weight", "dumbbell", "band"), List.of(), true, true, true, true, true,
                        Dolor.SUSTITUYE, "una sesion semanal"),
                new Perfil("N06", GoalType.INCREASE_STRENGTH, FitnessLevel.ADVANCED, List.of(L, X, V, S), 60,
                        TrainingLocation.GYM, GIMNASIO, List.of(), true, true, true, true, false, Dolor.INVALIDA,
                        "sin movimientos por encima de la cabeza; dolor en S4 con IA simulada invalida"),
                new Perfil("N07", GoalType.GENERAL_FITNESS, FitnessLevel.BEGINNER, List.of(X), 30,
                        TrainingLocation.HOME, List.of("body weight"), List.of(), false, true, true, true, true, Dolor.SUSTITUYE,
                        "solo peso corporal, sin impacto; nuevo minimo de 30"),
                new Perfil("N08", GoalType.GENERAL_FITNESS, FitnessLevel.INTERMEDIATE, List.of(L, J, Dm), 45,
                        TrainingLocation.GYM, GIMNASIO, List.of(), true, true, false, false, true, Dolor.SUSTITUYE,
                        "sin rodillas ni suspension; dolor en S2 y reactivacion en S7"),
                new Perfil("N09", GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, List.of(X, Dm), 30,
                        TrainingLocation.GYM, GIMNASIO, List.of(), true, true, true, true, true, Dolor.SUSTITUYE,
                        "segundo candidato a reincorporacion"),
                new Perfil("N10", GoalType.LOSE_WEIGHT, FitnessLevel.INTERMEDIATE, List.of(M, X, J, V, S, Dm), 30,
                        TrainingLocation.GYM, GIMNASIO, List.of(), true, true, true, true, true, Dolor.SUSTITUYE,
                        "frecuencia alta y sesiones cortas"));
    }

    private static List<D> todas(int n, D d) {
        return new ArrayList<>(Collections.nCopies(n, d));
    }

    private static List<D> conUna(int n, D base, int i, D d) {
        var l = todas(n, base);
        if (i < n) l.set(i, d);
        return l;
    }

    private static List<D> conDos(int n, D base, int i, D a, int j, D b) {
        var l = conUna(n, base, i, a);
        if (j < n) l.set(j, b);
        return l;
    }

    // ------------------------------------------------------------------ estado

    final Map<String, Long> ids = new LinkedHashMap<>();
    final Map<String, Perfil> porId = new LinkedHashMap<>();
    final List<String> historial = new ArrayList<>();
    final List<String> generacion = new ArrayList<>();
    final List<String> eventos = new ArrayList<>();
    final Map<Long, Dolor> modoDolor = new HashMap<>();
    int llamadasSimuladas = 0;

    // ------------------------------------------------------------------ campana

    /**
     * INSPECCION PREVIA DEL CATALOGO para fijar los bloqueos de N04 (se ejecuta una vez, antes de la campana):
     * lo que el motor de reglas elegiria para N04 sin bloqueos, con nombres. Los bloqueos se eligen de ahi para
     * que tengan efecto real, y quedan fijos en {@link #N04_FIJOS}.
     */
    @Test
    @EnabledIfSystemProperty(named = "fitsense.campana.p123.inspeccion", matches = "true")
    void inspeccionN04() {
        var n04 = cohorte().get(3);
        long u = usuario("N04_INSPECCION");
        perfil(u, new Perfil("N04_INSPECCION", n04.objetivo(), n04.nivel(), n04.dias(), n04.minutos(), n04.lugar(),
                n04.equipo(), List.of(), true, true, true, true, true, Dolor.SUSTITUYE, ""));
        long p = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        jdbc.queryForList("SELECT w.scheduled_date, x.exercise_id, e.name_en, e.movement_pattern, e.equipment_id"
                + " FROM planned_workouts w JOIN planned_workout_exercises x ON x.planned_workout_id = w.planned_workout_id"
                + " JOIN exercises e ON e.exercise_id = x.exercise_id WHERE w.plan_id = ? ORDER BY w.scheduled_date,"
                + " x.exercise_order", p).forEach(f -> System.out.println("INSPECCION_N04 " + f));
    }

    @Test
    @org.junit.jupiter.api.condition.DisabledIfSystemProperty(named = "fitsense.campana.p123.inspeccion", matches = "true")
    void campana() throws Exception {
        var url = jdbc.queryForObject("SELECT current_database() || '@' || coalesce(host(inet_server_addr()), 'local')", String.class);
        var jdbcUrl = postgres.getJdbcUrl();
        assertTrue(jdbcUrl.startsWith("jdbc:postgresql://localhost:") || jdbcUrl.contains("127.0.0.1"),
                "la campana solo corre sobre el PostgreSQL efimero: " + jdbcUrl);
        assertFalse(jdbcUrl.contains("neon") || jdbcUrl.contains("railway"), jdbcUrl);
        eventos.add("AISLAMIENTO url=" + jdbcUrl.replaceAll("\\?.*", "") + " servidor=" + url);
        iaSimulada();

        // P-01: prueba negativa separada (N07 con 15 min) antes de nada.
        long negativo = usuario("N07_NEG");
        try {
            perfil(negativo, new Perfil("N07_NEG", GoalType.GENERAL_FITNESS, FitnessLevel.BEGINNER, List.of((short) 3), 15,
                    TrainingLocation.HOME, List.of("body weight"), List.of(), false, true, true, true, true, Dolor.SUSTITUYE, ""));
            fail("P-01: GENERAL_FITNESS de 15 min tenia que rechazarse");
        } catch (DomainRuleViolationException e) {
            eventos.add("P01_NEGATIVO N07_NEG 15min RECHAZADO_ANTES_DE_GENERAR: " + e.getMessage());
        }
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM user_profiles WHERE user_id = ?", Integer.class, negativo));

        for (var p : cohorte()) {
            long u = usuario(p.id());
            perfil(u, p);
            ids.put(p.id(), u);
            porId.put(p.id(), p);
            modoDolor.put(u, p.dolor());
            eventos.add("PERFIL " + p.id() + " user_id=" + u + " aceptado por P-01 (" + p.objetivo() + " " + p.minutos() + " min)");
        }

        // Generacion inicial de los diez.
        var plan = new LinkedHashMap<String, Long>();
        for (var e : ids.entrySet()) plan.put(e.getKey(), generar(e.getKey(), e.getValue()));

        var reincorporado = new HashMap<String, Integer>();
        var entrenadoTras = new HashMap<String, Integer>();
        for (int s = 1; s <= EXTENSION; s++) {
            for (var id : ids.keySet()) {
                boolean extension = s > SEMANAS;
                if (extension && !(id.equals("N01") || id.equals("N09"))) continue;
                if (extension && entrenadoTras.containsKey(id)) continue;
                long u = ids.get(id);
                var lunes = S1.plusWeeks(s - 1);
                Long p = plan.get(id);
                if (id.equals("N08") && s == 7) reactivarN08(u);
                String ejecucion = p == null ? "SIN_PLAN" : ejecutar(id, u, p, guion(id, workouts(p).size()).get(s));
                if (p != null && reincorporado.containsKey(id) && !entrenadoTras.containsKey(id)
                        && ejecutoReincorporado(u, p)) entrenadoTras.put(id, s);
                var nuevo = cicloDe(id, u, s, lunes, p, ejecucion, extension);
                plan.put(id, nuevo);
                if (nuevo != null && !reincorporado.containsKey(id) && reincorporacionEn(u)) {
                    reincorporado.put(id, s + 1);
                    eventos.add("REINCORPORACION " + id + " publicada para S" + (s + 1));
                }
            }
        }
        reincorporado.forEach((id, s) -> eventos.add("REINCORPORADO " + id + " S" + s + " entrenado en S"
                + entrenadoTras.getOrDefault(id, -1)));
        eventos.add("LLAMADAS_REPLICATE_REALES=0 (fitsense.ai.enabled=false; generacion por reglas)");
        eventos.add("RESPUESTAS_IA_SIMULADAS=" + llamadasSimuladas);
        verify(iaGeneracion, never()).requestOutput(any(), any(), anyInt());

        volcar();
    }

    // ------------------------------------------------------------------ fases

    private Long generar(String id, long u) {
        int candidatos = planComandos.prepare(GenerateWeeklyPlanCommand.firstPlan(u, S1)).context().availableExercises().size();
        try {
            long p = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
            var f = jdbc.queryForMap("SELECT generation_source, generation_attempts FROM weekly_training_plans WHERE plan_id = ?", p);
            var ws = jdbc.queryForList("SELECT scheduled_date, focus_code, expected_duration_minutes,"
                    + " (SELECT count(*) FROM planned_workout_exercises x WHERE x.planned_workout_id = w.planned_workout_id) n"
                    + " FROM planned_workouts w WHERE plan_id = ? ORDER BY scheduled_date", p);
            String validacion = validarSemana(u, p, S1);
            generacion.add(String.join(";", id, String.valueOf(u), String.valueOf(p), "PUBLICADO",
                    String.valueOf(f.get("generation_source")), String.valueOf(f.get("generation_attempts")),
                    String.valueOf(candidatos), String.valueOf(ws.size()),
                    ws.stream().map(w -> w.get("scheduled_date") + "/" + w.get("focus_code") + "/"
                            + w.get("expected_duration_minutes") + "min/" + w.get("n") + "ej").collect(Collectors.joining(" ")),
                    validacion.replace(";", ",")));
            return p;
        } catch (RuntimeException e) {
            generacion.add(String.join(";", id, String.valueOf(u), "", "SIN_PLAN", "", "", String.valueOf(candidatos),
                    "0", "", (e.getClass().getSimpleName() + ": " + e.getMessage()).replace(";", ",")));
            return null;
        }
    }

    private Long cicloDe(String id, long u, int s, LocalDate lunes, Long p, String ejecucion, boolean extension) {
        int antesV32 = filasV32(u);
        int antesSimuladas = llamadasSimuladas;
        Long nuevo;
        try {
            nuevo = ciclo.runFor(u, lunes, lunes.plusWeeks(1)).orElse(null);
        } catch (RuntimeException e) {
            eventos.add("ERROR_CICLO " + id + " S" + s + " " + e);
            nuevo = null;
        }
        var v32 = filasV32(u) > antesV32 ? jdbc.queryForMap("SELECT attempt_id, decision, status, ai_calls,"
                + " targeted_interventions::text ti, progressed_sessions::text ps, rejection_reason,"
                + " conservation_report::text informe FROM plan_adaptation_attempts WHERE user_id = ?"
                + " ORDER BY attempt_id DESC LIMIT 1", u) : Map.<String, Object>of();
        var api = resultados.history(u);
        var ultimo = api.isEmpty() || v32.isEmpty() ? null : api.get(api.size() - 1);
        var excluidos = dolor.excluidosActivos(u);
        String origen = nuevo == null ? "" : jdbc.queryForObject("SELECT generation_source FROM weekly_training_plans"
                + " WHERE plan_id = ?", String.class, nuevo);
        var ws = nuevo == null ? List.<Map<String, Object>>of() : jdbc.queryForList("SELECT scheduled_date,"
                + " expected_duration_minutes FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date", nuevo);
        boolean publicaExcluido = nuevo != null && !excluidos.isEmpty() && jdbc.queryForObject("SELECT count(*) FROM"
                + " planned_workout_exercises x JOIN planned_workouts w ON w.planned_workout_id = x.planned_workout_id"
                + " WHERE w.plan_id = ? AND x.exercise_id = ANY(?)", Integer.class, nuevo, excluidos.toArray(new Long[0])) > 0;
        String frecuencia = nuevo == null ? "" : frecuencia(u, ws);
        String validacion = nuevo == null ? "" : validarSemana(u, nuevo, lunes.plusWeeks(1));
        historial.add(String.join(";", id, String.valueOf(s), extension ? "EXTENSION" : "PRINCIPAL",
                lunes.toString(), p == null ? "" : String.valueOf(p), ejecucion,
                lunes.plusWeeks(1).toString(), nuevo == null ? "" : String.valueOf(nuevo), origen,
                String.valueOf(ws.size()),
                ws.stream().map(w -> String.valueOf(w.get("expected_duration_minutes"))).collect(Collectors.joining("|")),
                String.valueOf(v32.getOrDefault("decision", "")), String.valueOf(v32.getOrDefault("status", "")),
                ultimo == null ? "" : ultimo.resultado(), String.valueOf(v32.getOrDefault("ti", "")),
                String.valueOf(llamadasSimuladas - antesSimuladas), String.valueOf(v32.getOrDefault("ps", "")),
                excluidos.toString().replace(",", " "), String.valueOf(publicaExcluido), frecuencia,
                validacion.replace(";", ","),
                String.valueOf(v32.getOrDefault("rejection_reason", "")).replace(";", ",").replace("\n", " ")));
        if (publicaExcluido) eventos.add("VIOLACION_P02 " + id + " S" + (s + 1) + " plan " + nuevo);
        return nuevo;
    }

    private void reactivarN08(long u) {
        var activos = new ArrayList<>(dolor.excluidosActivos(u));
        if (activos.isEmpty()) {
            eventos.add("N08 S7 sin exclusiones activas que reactivar");
            return;
        }
        for (var ex : activos) {
            try {
                var r = dolor.reactivar(u, ex, true, "confirmo que ya no siento molestias (simulado)");
                eventos.add("N08 S7 REACTIVACION ejercicio " + ex + " -> REACTIVADA candidato_hoy=" + r.candidatoHoy());
            } catch (DomainRuleViolationException e) {
                eventos.add("N08 S7 REACTIVACION ejercicio " + ex + " -> RECHAZADA " + e.getMessage());
            }
        }
    }

    // ------------------------------------------------------------------ ejecucion

    private String ejecutar(String id, long u, long planId, List<D> dias) {
        var principal = UserDetailsImpl.build(usuarios.findById(u).orElseThrow());
        var ws = workouts(planId);
        var hechos = new ArrayList<String>();
        for (int i = 0; i < ws.size(); i++) {
            var d = i < dias.size() ? dias.get(i) : D.HECHO;
            long w = ws.get(i);
            switch (d) {
                case HECHO -> sesion(u, w, 1.0, null, null, 6);
                case PARCIAL_TIEMPO -> sesion(u, w, 0.35, SkipReason.LACK_OF_TIME, null, 6);
                case PARCIAL_FATIGA -> sesion(u, w, 0.35, SkipReason.FATIGUE, null, 8);
                case CON_FATIGA -> sesion(u, w, 1.0, null, SkipReason.FATIGUE, 8);
                case DIFICIL -> sesion(u, w, 1.0, null, SkipReason.TOO_DIFFICULT, 9);
                case CON_DOLOR -> sesion(u, w, 1.0, null, SkipReason.PAIN_OR_DISCOMFORT, 6);
                case OMITE_MOTIVACION -> planes.skip(principal, w, new SkipWorkoutResource(SkipReason.LACK_OF_MOTIVATION));
                case OMITE_HORARIO -> planes.skip(principal, w, new SkipWorkoutResource(SkipReason.SCHEDULE_CHANGE));
                case OMITE_OTRO -> planes.skip(principal, w, new SkipWorkoutResource(SkipReason.OTHER));
                case OMITE_TIEMPO_CTX -> {
                    int min = jdbc.queryForObject("SELECT expected_duration_minutes FROM planned_workouts"
                            + " WHERE planned_workout_id = ?", Integer.class, w);
                    planes.skip(principal, w, new SkipWorkoutResource(SkipReason.LACK_OF_TIME,
                            ContextoFaltaDeTiempo.MENOS_TIEMPO_HABITUAL, Math.max(15, min - 1)));
                }
                case NADA -> { }
            }
            hechos.add(d.name());
        }
        return String.join("|", hechos);
    }

    /**
     * Una sesion registrada. {@code fraccion} &lt; 1: se hacen los primeros ejercicios hasta esa fraccion y el resto
     * lleva {@code motivoParcial}. {@code marcar}: el PRIMER ejercicio se registra con ese motivo (dolor: 0 hecho;
     * fatiga o dificultad: la mitad).
     */
    private void sesion(long u, long w, double fraccion, SkipReason motivoParcial, SkipReason marcar, int rpe) {
        long s = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
        var filas = jdbc.queryForList("SELECT planned_exercise_id, prescription_type, planned_sets, planned_reps,"
                + " planned_duration_seconds FROM planned_workout_exercises WHERE planned_workout_id = ?"
                + " ORDER BY exercise_order", w);
        double objetivo = fraccion * filas.size();
        for (int k = 0; k < filas.size(); k++) {
            var f = filas.get(k);
            long pe = ((Number) f.get("planned_exercise_id")).longValue();
            boolean duracion = "DURATION".equals(f.get("prescription_type"));
            int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
            int total = series * ((Number) f.get(duracion ? "planned_duration_seconds" : "planned_reps")).intValue();
            double parte = Math.max(0, Math.min(1, objetivo - k));
            SkipReason motivo = parte >= 1 ? null : motivoParcial;
            if (k == 0 && marcar != null) {
                motivo = marcar;
                parte = marcar == SkipReason.PAIN_OR_DISCOMFORT ? 0 : 0.5;
            }
            int hecho = (int) Math.round(total * parte);
            sesiones.handle(new RecordSessionExerciseCommand(u, s, pe, (short) (hecho == 0 ? 0 : series),
                    duracion ? null : hecho, duracion ? hecho : null, null, motivo));
        }
        sesiones.handle(new FinishWorkoutSessionCommand(u, s, (short) rpe, (short) 4, (short) 30));
    }

    private boolean reincorporacionEn(long u) {
        var informe = jdbc.queryForObject("SELECT conservation_report::text FROM plan_adaptation_attempts WHERE"
                + " user_id = ? ORDER BY attempt_id DESC LIMIT 1", String.class, u);
        return informe != null && informe.replace(" ", "").contains("\"tipo\":\"REINCORPORACION\"");
    }

    /** Si la semana ejecutada contenia el ejercicio reincorporado y se completo. */
    private boolean ejecutoReincorporado(long u, long planId) {
        var informes = jdbc.queryForList("SELECT conservation_report::text FROM plan_adaptation_attempts WHERE"
                + " user_id = ? AND conservation_report::text LIKE '%REINCORPORACION%'", String.class, u);
        var ids = new HashSet<Long>();
        var m = java.util.regex.Pattern.compile("\"tipo\"\\s*:\\s*\"REINCORPORACION\"[^}]*?\"exercise_id\"\\s*:\\s*(\\d+)"
                + "|\"exercise_id\"\\s*:\\s*(\\d+)[^}]*?\"tipo\"\\s*:\\s*\"REINCORPORACION\"");
        for (var i : informes) {
            var x = m.matcher(i);
            while (x.find()) ids.add(Long.parseLong(x.group(1) != null ? x.group(1) : x.group(2)));
        }
        if (ids.isEmpty()) return false;
        return jdbc.queryForObject("SELECT count(*) FROM workout_session_exercises wse JOIN workout_sessions ws ON"
                + " ws.session_id = wse.session_id JOIN planned_workout_exercises pwe ON pwe.planned_exercise_id ="
                + " wse.planned_exercise_id JOIN planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id"
                + " WHERE pw.plan_id = ? AND pwe.exercise_id = ANY(?) AND wse.skip_reason IS NULL", Integer.class,
                planId, ids.toArray(new Long[0])) > 0;
    }

    // ------------------------------------------------------------------ IA simulada

    /**
     * POLITICA DE LA IA SIMULADA, fijada antes de ejecutar y no modificada despues:
     * <ul>
     *   <li>ACORTAR_O_REORGANIZAR: retira el ULTIMO relleno (patron sin slot 4C); sin relleno, la sesion igual.</li>
     *   <li>REVISAR_CARGA_Y_RECUPERACION y VARIANTE_MAS_SENCILLA_O_CARGA: -2 repeticiones en el primer ejercicio de
     *       series y repeticiones si no baja del minimo de V16; si no cabe, -1 serie.</li>
     *   <li>SEGURIDAD_ANTE_DOLOR: con {@link Dolor#SUSTITUYE}, cada ejercicio con dolor (o todos los de la sesion si
     *       el dolor no se identifico) por un elegible del MISMO patron y tipo de prescripcion que no este en la
     *       sesion, con la misma dosis; con {@link Dolor#INVALIDA}, una respuesta que no cumple el contrato.</li>
     *   <li>REPROGRAMAR_DIAS y el resto: la sesion igual (sin efecto).</li>
     * </ul>
     */
    private void iaSimulada() {
        when(ia.isEnabled()).thenReturn(true);
        when(ia.modelName()).thenReturn("SIMULADA-campana-p123");
        when(ia.pedirRespuesta(any(), any(), any(), any(), anyInt())).thenAnswer(inv -> {
            llamadasSimuladas++;
            return new ReplicateSessionFixGenerator.RawFix("{}", "SIMULADA");
        });
        when(ia.interpretar(anyString(), any())).thenAnswer(inv -> corregir(inv.getArgument(1)));
    }

    private List<CorrectedSession> corregir(PlanGenerationContext ctx) {
        var salida = new ArrayList<CorrectedSession>();
        var previa = ctx.previousWeek();
        for (var senalada : ctx.adjustment().sesionesParaIA()) {
            long id = senalada.plannedWorkoutId();
            var ejercicios = jdbc.queryForList("SELECT exercise_id, prescription_type, planned_sets, planned_reps,"
                    + " planned_duration_seconds, rest_seconds FROM planned_workout_exercises WHERE planned_workout_id = ?"
                    + " ORDER BY exercise_order", id);
            var w = jdbc.queryForMap("SELECT scheduled_date, focus_code FROM planned_workouts WHERE planned_workout_id = ?", id);
            var lista = new ArrayList<PlanDraft.DraftExercise>();
            for (var e : ejercicios)
                lista.add(new PlanDraft.DraftExercise(((Number) e.get("exercise_id")).longValue(),
                        PrescriptionType.valueOf((String) e.get("prescription_type")), corto(e.get("planned_sets")),
                        corto(e.get("planned_reps")), e.get("planned_duration_seconds") == null ? null
                        : ((Number) e.get("planned_duration_seconds")).intValue(), null, corto(e.get("rest_seconds")), null));
            var tipo = senalada.intervencion();
            if (tipo == TipoIntervencion.SEGURIDAD_ANTE_DOLOR) {
                if (modoDolor.getOrDefault(ctx.userId(), Dolor.SUSTITUYE) == Dolor.INVALIDA)
                    throw new InvalidPlanDraftException(List.of("SIMULADA: respuesta que no cumple el contrato"));
                var dolieron = previa.prescriptions().stream().filter(x -> Long.valueOf(id).equals(x.plannedWorkoutId())
                        && "PAIN_OR_DISCOMFORT".equals(x.skipReason())).map(x -> x.exerciseId()).collect(Collectors.toSet());
                if (dolieron.isEmpty()) lista.forEach(x -> dolieron.add(x.exerciseId()));
                var enSesion = lista.stream().map(PlanDraft.DraftExercise::exerciseId).collect(Collectors.toSet());
                for (int k = 0; k < lista.size(); k++) {
                    var x = lista.get(k);
                    if (!dolieron.contains(x.exerciseId())) continue;
                    String patron = jdbc.queryForObject("SELECT movement_pattern FROM exercises WHERE exercise_id = ?",
                            String.class, x.exerciseId());
                    var sust = ctx.availableExercises().stream().filter(c -> Objects.equals(c.movementPattern(), patron)
                            && !enSesion.contains(c.exerciseId())).map(CandidateExercise::exerciseId).sorted().findFirst();
                    if (sust.isEmpty()) continue;
                    enSesion.add(sust.get());
                    lista.set(k, new PlanDraft.DraftExercise(sust.get(), x.prescriptionType(), x.plannedSets(),
                            x.plannedReps(), x.plannedDurationSeconds(), null, x.restSeconds(), null));
                }
            } else if (tipo == TipoIntervencion.ACORTAR_O_REORGANIZAR) {
                for (int k = lista.size() - 1; k >= 0; k--)
                    if (MandatorySlot.ofMovementPattern(String.valueOf(jdbc.queryForObject("SELECT movement_pattern FROM"
                            + " exercises WHERE exercise_id = ?", String.class, lista.get(k).exerciseId()))).isEmpty()) {
                        lista.remove(k);
                        break;
                    }
            } else if (tipo == TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION
                    || tipo == TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA) {
                for (int k = 0; k < lista.size(); k++) {
                    var x = lista.get(k);
                    if (x.prescriptionType() != PrescriptionType.SETS_REPS || x.plannedReps() == null) continue;
                    int minimo = minimoDeRepeticiones(ctx, x.exerciseId());
                    if (x.plannedReps() - 2 >= minimo)
                        lista.set(k, new PlanDraft.DraftExercise(x.exerciseId(), x.prescriptionType(), x.plannedSets(),
                                (short) (x.plannedReps() - 2), null, null, x.restSeconds(), null));
                    else if (x.plannedSets() != null && x.plannedSets() > 1)
                        lista.set(k, new PlanDraft.DraftExercise(x.exerciseId(), x.prescriptionType(),
                                (short) (x.plannedSets() - 1), x.plannedReps(), null, null, x.restSeconds(), null));
                    else continue;
                    break;
                }
            }
            var fecha = ((java.sql.Date) w.get("scheduled_date")).toLocalDate().plusWeeks(1);
            int minutos = estimador.estimateMinutes(new PlanDraft.DraftWorkout(fecha,
                    WorkoutFocus.valueOf((String) w.get("focus_code")), "x", 0, lista), ctx.prescription());
            salida.add(new CorrectedSession(id, fecha, minutos, lista, "SIMULADA " + tipo));
        }
        return salida;
    }

    private static int minimoDeRepeticiones(PlanGenerationContext ctx, long exerciseId) {
        var limites = ctx.prescription() == null ? null : ctx.prescription().repLimits();
        var c = ctx.availableExercises().stream().filter(x -> x.exerciseId() == exerciseId).findFirst();
        return limites == null || c.isEmpty() ? 0 : limites.minRepsFor(c.get().bodyPartCode(), ctx.profile().fitnessLevel());
    }

    private static Short corto(Object o) {
        return o == null ? null : ((Number) o).shortValue();
    }

    // ------------------------------------------------------------------ verificacion

    private String frecuencia(long u, List<Map<String, Object>> ws) {
        var perfil = jdbc.queryForMap("SELECT fitness_level, days_per_week, available_days::text dias FROM user_profiles"
                + " WHERE user_id = ?", u);
        int tope = "BEGINNER".equals(perfil.get("fitness_level")) ? 4 : 6;
        var disp = Arrays.stream(String.valueOf(perfil.get("dias")).replaceAll("[{}]", "").split(","))
                .map(String::strip).map(Integer::parseInt).collect(Collectors.toSet());
        int esperadas = Math.min(Math.min(((Number) perfil.get("days_per_week")).intValue(), tope), disp.size());
        var fechas = ws.stream().map(w -> ((java.sql.Date) w.get("scheduled_date")).toLocalDate()).toList();
        var problemas = new ArrayList<String>();
        if (fechas.size() > tope) problemas.add("supera tope " + tope);
        if (fechas.size() >= 7) problemas.add("siete");
        if (fechas.size() != esperadas) problemas.add(fechas.size() + "!=" + esperadas);
        if (new HashSet<>(fechas).size() != fechas.size()) problemas.add("fecha repetida");
        if (fechas.stream().anyMatch(f -> !disp.contains(f.getDayOfWeek().getValue()))) problemas.add("dia no disponible");
        return problemas.isEmpty() ? "OK(" + fechas.size() + "/" + tope + ")" : "FALLA " + problemas;
    }

    private String validarSemana(long u, long plan, LocalDate semana) {
        try {
            var ctx = planComandos.prepare(new GenerateWeeklyPlanCommand(u, semana, null, true, null, null)).context();
            return new ValidacionIndependiente(jdbc).validar(plan, ctx, validador, 30).texto();
        } catch (RuntimeException e) {
            return "NO_CONCLUYENTE " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    private int filasV32(long u) {
        return jdbc.queryForObject("SELECT count(*) FROM plan_adaptation_attempts WHERE user_id = ?", Integer.class, u);
    }

    private List<Long> workouts(long planId) {
        return jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date",
                Long.class, planId);
    }

    // ------------------------------------------------------------------ alta

    private long usuario(String id) {
        String email = COHORTE.toLowerCase() + "_" + id.toLowerCase() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', ?, now(), now())", email, id);
        return jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
    }

    private void perfil(long u, Perfil p) {
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 6, 15), BiologicalSex.FEMALE,
                new BigDecimal("166"), new BigDecimal("68"), null, p.nivel(), p.objetivo(), COHORTE + " " + p.id(),
                p.lugar(), (short) p.dias().size(), p.dias(), (short) p.minutos(), p.equipo(), p.bloqueados(), null,
                p.impacto(), p.suelo(), p.rodillas(), p.suspension(), p.porEncima()));
    }

    static List<Long> bloqueadosN04() {
        var s = System.getProperty("fitsense.campana.p123.n04", "");
        return s.isBlank() ? List.of(N04_FIJOS) : Arrays.stream(s.split(",")).map(Long::parseLong).toList();
    }


    // ------------------------------------------------------------------ evidencia

    private void volcar() throws IOException {
        Files.createDirectories(SALIDA);
        Files.write(SALIDA.resolve("historial.csv"), cabecera("usuario;semana;fase;lunes_medida;plan_medido;ejecucion;"
                + "lunes_nueva;plan_nuevo;origen;sesiones;minutos;decision;estado_v32;resultado_api;intervenciones;"
                + "llamadas_ia_simuladas;sesiones_progresadas;excluidos_dolor;publica_excluido;frecuencia;validacion;"
                + "rechazo", historial));
        Files.write(SALIDA.resolve("generacion.csv"), cabecera("usuario;user_id;plan;estado;origen;intentos;"
                + "candidatos;sesiones;detalle;validacion", generacion));
        Files.write(SALIDA.resolve("eventos.log"), eventos);
        var usuariosIds = ids.values().stream().map(String::valueOf).collect(Collectors.joining(","));
        volcarTabla("v32.csv", "SELECT a.user_id, a.attempt_id, a.weekly_metric_id, a.source_plan_id, a.new_week_start_date,"
                + " a.resulting_plan_id, a.decision, a.status, a.generation_source, a.ai_calls, a.targeted_sessions::text,"
                + " a.targeted_interventions::text, a.applied_sessions::text, a.progressed_sessions::text,"
                + " a.rejection_reason, a.conservation_report::text FROM plan_adaptation_attempts a WHERE a.user_id IN ("
                + usuariosIds + ") ORDER BY a.attempt_id");
        volcarTabla("planes.csv", "SELECT p.user_id, p.plan_id, p.week_start_date, p.plan_version, p.status,"
                + " p.generation_source, w.planned_workout_id, w.source_planned_workout_id, w.scheduled_date, w.focus_code,"
                + " w.expected_duration_minutes, w.status ws, w.skip_reason, x.exercise_order, x.exercise_id,"
                + " x.prescription_type, x.planned_sets, x.planned_reps, x.planned_duration_seconds, x.rest_seconds"
                + " FROM weekly_training_plans p JOIN planned_workouts w ON w.plan_id = p.plan_id JOIN"
                + " planned_workout_exercises x ON x.planned_workout_id = w.planned_workout_id WHERE p.user_id IN ("
                + usuariosIds + ") ORDER BY p.user_id, p.week_start_date, p.plan_version, w.scheduled_date, x.exercise_order");
        volcarTabla("metricas.csv", "SELECT user_id, week_start_date, weighted_adherence_pct, observed_adherence_pct,"
                + " registration_coverage_pct, recorded_workouts FROM weekly_user_metrics WHERE user_id IN (" + usuariosIds
                + ") ORDER BY user_id, week_start_date");
        volcarTabla("exclusiones_dolor.csv", "SELECT * FROM user_safety_exclusions WHERE user_id IN (" + usuariosIds
                + ") ORDER BY exclusion_id");
        volcarTabla("regeneraciones.csv", "SELECT * FROM safe_regeneration_attempts WHERE user_id IN (" + usuariosIds
                + ") ORDER BY regeneration_id");
        volcarTabla("perfiles.csv", "SELECT user_id, fitness_level, goal_type, days_per_week, available_days::text,"
                + " session_minutes, training_location, equipment_codes::text, blocked_exercise_ids::text,"
                + " allow_high_impact, allow_floor_exercises, allow_kneeling_exercises, allow_hanging_exercises,"
                + " allow_overhead_exercises FROM user_profiles WHERE user_id IN (" + usuariosIds + ") ORDER BY user_id");
        volcarTabla("nombres_ejercicios.csv", "SELECT exercise_id, name_en, name_es, movement_pattern FROM exercises WHERE exercise_id IN"
                + " (SELECT DISTINCT x.exercise_id FROM planned_workout_exercises x JOIN planned_workouts w ON"
                + " w.planned_workout_id = x.planned_workout_id JOIN weekly_training_plans p ON p.plan_id = w.plan_id"
                + " WHERE p.user_id IN (" + usuariosIds + ")) ORDER BY exercise_id");
        Files.writeString(SALIDA.resolve("ids.csv"), "usuario;user_id\n" + ids.entrySet().stream()
                .map(e -> e.getKey() + ";" + e.getValue()).collect(Collectors.joining("\n")) + "\n");
    }

    private void volcarTabla(String nombre, String sql) throws IOException {
        var filas = jdbc.queryForList(sql);
        var lineas = new ArrayList<String>();
        if (!filas.isEmpty()) lineas.add(String.join(";", filas.get(0).keySet()));
        for (var f : filas)
            lineas.add(f.values().stream().map(v -> v == null ? "" : String.valueOf(v).replace(";", ",")
                    .replace("\n", " ")).collect(Collectors.joining(";")));
        Files.write(SALIDA.resolve(nombre), lineas);
    }

    private static List<String> cabecera(String c, List<String> filas) {
        var l = new ArrayList<String>();
        l.add(c);
        l.addAll(filas);
        return l;
    }
}
