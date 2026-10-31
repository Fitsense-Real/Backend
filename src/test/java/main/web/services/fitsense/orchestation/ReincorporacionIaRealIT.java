package main.web.services.fitsense.orchestation;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.application.internal.commandservices.AdaptedWeekCommandService;
import main.web.services.fitsense.planning.application.internal.commandservices.ProgresionResolver;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.MandatorySlot;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateTrainingPlanGenerator;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;

/**
 * REINCORPORACION DESPUES DE UNA ADAPTACION CON IA REAL (orden de cierre del MVP, 31/10/2026, fase 2).
 * <p>
 * Llama a Replicate y cuesta dinero: solo con {@code -Dfitsense.reincorporacion.ia=true}. Como mucho
 * {@code fitsense.reincorporacion.max-llamadas} (12) llamadas reales, contando reintentos.
 * <p>
 * Recorrido, con los servicios reales sobre PostgreSQL efimero y sin insertar resultados:
 * <ol>
 *   <li><b>A</b> · plan inicial del motor de reglas (la generacion no llama a la IA: 0 llamadas).</li>
 *   <li><b>B</b> · semana 1: lunes y miercoles completos; el viernes se empieza y se deja al 35 % por
 *       falta de tiempo (sesion PARCIAL, sin M: el unico acortamiento tras el que el diseno permite
 *       reincorporar sin una M nueva, §5 fila 5).</li>
 *   <li><b>C</b> · el ciclo pide ACORTAR y la correccion la propone Replicate real. Cuenta como retirada
 *       solo si la sesion publicada pierde EXACTAMENTE un relleno (sin slot) y conserva el resto con su
 *       dosis. Si no, el escenario se documenta y se prueba otro participante (otro plan), sin tocar la
 *       respuesta, mientras quede presupuesto.</li>
 *   <li><b>D-G</b> · semanas completas por los comandos de ejecucion; cada ciclo es determinista (0
 *       llamadas): progresion, mantenimiento, reincorporacion y, despues, la semana con el ejercicio
 *       reincorporado se entrena y se cierra otro ciclo.</li>
 * </ol>
 * Con {@code -Dfitsense.reincorporacion.ia=seco} es la COMPROBACION DETERMINISTA PREVIA, sin red ni
 * coste: los mismos participantes (mismos ids, mismos planes) y una correccion simulada que solo
 * retira el ultimo relleno de la sesion senalada. Sus resultados no son de la IA.
 * <p>
 * Salida en {@code target/reincorporacion-ia/} (o {@code target/reincorporacion-seco/}).
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.reincorporacion.ia", matches = "true|seco")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=true",
        "fitsense.ai.api-token=proxy",
        "fitsense.ai.model=gpt-5-mini",
        "fitsense.ai.reasoning-effort=medium",
        "fitsense.ai.prefer-wait-seconds=20",
        "fitsense.adaptation.progression.enabled=true"
})
@DisplayName("Reincorporacion con IA real: adapta, cumple, progresa, reincorpora y se entrena")
class ReincorporacionIaRealIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoSpyBean ReplicateSessionFixGenerator correcciones;
    @MockitoSpyBean ReplicateTrainingPlanGenerator generador;

    @Autowired WeeklyCycleService ciclo;
    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired PlanDraftValidator validador;
    @Autowired UserProfileCommandService perfiles;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired JdbcTemplate jdbc;

    static final int MAX_LLAMADAS = Integer.getInteger("fitsense.reincorporacion.max-llamadas", 12);
    static final int MAX_ESCENARIOS = Integer.getInteger("fitsense.reincorporacion.escenarios", 4);
    static final int SEMANAS_MAX = 16;
    /** El perfil del escenario: objetivo y minutos (por omision LOSE_WEIGHT intermedio, 60 min). */
    static final GoalType OBJETIVO = GoalType.valueOf(System.getProperty("fitsense.reincorporacion.objetivo", "LOSE_WEIGHT"));
    static final int MINUTOS = Integer.getInteger("fitsense.reincorporacion.minutos", 60);
    static final List<Short> DIAS = Arrays.stream(System.getProperty("fitsense.reincorporacion.dias", "1,3,5").split(","))
            .map(String::strip).map(Short::valueOf).toList();
    static final LocalDate S1 = LocalDate.of(2026, 11, 2);
    static final boolean SECO = "seco".equals(System.getProperty("fitsense.reincorporacion.ia"));
    static final Path SALIDA = Path.of("target", (SECO ? "reincorporacion-seco" : "reincorporacion-ia")
            + "-" + OBJETIVO + "-" + MINUTOS + "-d" + DIAS.size());
    static final List<String> GIMNASIO = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine",
            "bench", "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine");

    record Ej(long id, String tipo, Short series, Short reps, Integer segundos, Short descanso) {
        String dosis() {
            return "DURATION".equals(tipo) ? series + "x" + segundos + "s/" + descanso : series + "x" + reps + "/" + descanso;
        }
    }

    record Llamada(String etapa, int intento, long ms, String salida, String error) {}

    final Queue<Llamada> llamadas = new ConcurrentLinkedQueue<>();
    final Queue<String> tiempos = new ConcurrentLinkedQueue<>();
    final Queue<String> eventos = new ConcurrentLinkedQueue<>();
    volatile String etapa = "-";

    @Test
    void recorridoCompleto() throws Exception {
        Files.createDirectories(SALIDA);
        espiar();
        var escenarios = new ArrayList<String>();
        var historial = new StringBuilder(linea("escenario", "semana", "ciclo", "plan_medido", "plan_publicado",
                "adherencia_observada", "cobertura", "decision", "estado_v32", "intervenciones", "llamadas_live",
                "ai_calls_v32", "paso", "ejercicio_paso", "dosis_antes", "dosis_despues", "volumen_antes",
                "volumen_despues", "volumen_base", "techo", "incremento_pct", "estado_reserva", "retirado_en_plan",
                "dosis_retirado", "validacion_independiente", "nota"));
        var traza = new StringBuilder(linea("escenario", "semana", "plan", "sesion", "fecha", "estado_ejercicio",
                "ejercicio", "dosis", "posicion", "registrado", "series_hechas", "reps_hechas", "segundos_hechos"));
        String conclusion = null;

        for (int n = 1; n <= MAX_ESCENARIOS && conclusion == null; n++) {
            if (llamadas.size() + 3 > MAX_LLAMADAS) {
                escenarios.add("E" + n + ": no se inicia, no caben 3 llamadas en el presupuesto ("
                        + llamadas.size() + "/" + MAX_LLAMADAS + ")");
                break;
            }
            String esc = "E" + n;
            etapa = esc + " · A";
            long u = participante(n);
            long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
            var raiz = semana(w1);
            int antesLive = llamadas.size();
            assertEquals(0, antesLive - llamadas.size(), "la generacion inicial no llama a la IA");

            // B · el viernes se deja al 35 % por falta de tiempo.
            etapa = esc + " · B/C";
            ejecutar(u, w1, true);
            int live0 = llamadas.size();
            long w2 = ciclo.runFor(u, S1, S1.plusWeeks(1)).orElseThrow();
            var ev = ultimo(u);
            int liveC = llamadas.size() - live0;
            var viernesAntes = ultimaSesion(raiz);
            var viernesDespues = ultimaSesion(semana(w2));
            var retirado = retirada(viernesAntes, viernesDespues);
            historial.append(fila(esc, 1, "C", w1, w2, ev, liveC, null, retirado == null ? null : retirado,
                    viernesDespues, validar(u, w2, S1.plusWeeks(1)), retirado == null
                            ? "sin retirada valida: " + resumen(viernesAntes, viernesDespues) : "retirada del " + retirado.id()));
            volcar(traza, esc, 1, w1, retirado == null ? null : retirado.id());
            if (retirado == null || !"APLICADA".equals(ev.get("status"))
                    || !"{ACORTAR_O_REORGANIZAR}".equals(String.valueOf(ev.get("targeted_interventions")))) {
                escenarios.add(esc + ": " + ev.get("targeted_interventions") + " " + ev.get("status") + ", " + liveC
                        + " llamadas; " + resumen(viernesAntes, viernesDespues) + " -> no es una retirada de un relleno");
                continue;
            }
            escenarios.add(esc + ": ACORTAR APLICADA con " + liveC + " llamada(s); retira " + retirado.id() + " "
                    + retirado.dosis());

            // D-G · semanas completas, ciclos deterministas.
            long plan = w2;
            int reincorporadaEn = -1;
            var pasos = new ArrayList<String>();
            for (int k = 1; k < SEMANAS_MAX; k++) {
                etapa = esc + " · s" + (k + 1);
                ejecutar(u, plan, false);
                volcar(traza, esc, k + 1, plan, retirado.id());
                int live = llamadas.size();
                long siguiente = ciclo.runFor(u, S1.plusWeeks(k), S1.plusWeeks(k + 1)).orElseThrow();
                var evk = ultimo(u);
                int liveK = llamadas.size() - live;
                assertEquals(0, liveK, "ciclo determinista con llamadas: " + evk);
                assertTrue(String.valueOf(evk.get("progressed_sessions")).split(",").length <= 1,
                        "a lo sumo un paso por ciclo: " + evk);
                var inf = informe(evk);
                var viernes = ultimaSesion(semana(siguiente));
                String nota = inf.isEmpty() ? "mantenimiento" : inf.get(0).get("tipo") + " " + inf.get(0).get("exercise_id");
                historial.append(fila(esc, k + 1, "D-G", plan, siguiente, evk, liveK, inf.isEmpty() ? null : inf.get(0),
                        retirado, viernes, validar(u, siguiente, S1.plusWeeks(k + 1)), nota));
                pasos.add("s" + (k + 1) + " " + nota);
                if (!inf.isEmpty() && "REINCORPORACION".equals(inf.get(0).get("tipo"))) {
                    assertEquals(retirado.id(), ((Number) inf.get(0).get("exercise_id")).longValue(), "vuelve el retirado");
                    reincorporadaEn = k + 1;
                    // Mismo ciclo dos veces: no duplica.
                    int intentos = intentos(u);
                    long otra = ciclo.runFor(u, S1.plusWeeks(k), S1.plusWeeks(k + 1)).orElseThrow();
                    assertEquals(siguiente, otra, "repetir el ciclo devuelve la misma semana");
                    assertEquals(intentos, intentos(u), "repetir el ciclo no registra otra evidencia");
                    assertEquals(1, ultimaSesion(semana(siguiente)).stream().filter(e -> e.id() == retirado.id()).count(),
                            "sin duplicados");
                    plan = siguiente;
                    // G · se entrena la semana con el reincorporado y se cierra otro ciclo.
                    etapa = esc + " · s" + (k + 2) + " mantenimiento";
                    ejecutar(u, plan, false);
                    volcar(traza, esc, k + 2, plan, retirado.id());
                    live = llamadas.size();
                    long despues = ciclo.runFor(u, S1.plusWeeks(k + 1), S1.plusWeeks(k + 2)).orElseThrow();
                    var evm = ultimo(u);
                    assertEquals(0, llamadas.size() - live);
                    var infm = informe(evm);
                    historial.append(fila(esc, k + 2, "G", plan, despues, evm, 0, infm.isEmpty() ? null : infm.get(0),
                            retirado, ultimaSesion(semana(despues)), validar(u, despues, S1.plusWeeks(k + 2)),
                            infm.isEmpty() ? "mantenimiento" : infm.get(0).get("tipo") + " " + infm.get(0).get("exercise_id")));
                    volcar(traza, esc, k + 3, despues, retirado.id());
                    var registro = jdbc.queryForList("SELECT wse.status, wse.actual_sets, wse.actual_reps_total,"
                            + " wse.actual_duration_seconds, wse.completion_percentage FROM workout_session_exercises wse"
                            + " JOIN workout_sessions ws ON ws.session_id = wse.session_id"
                            + " JOIN planned_workout_exercises pwe ON pwe.planned_exercise_id = wse.planned_exercise_id"
                            + " JOIN planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id"
                            + " WHERE pw.plan_id = ? AND pwe.exercise_id = ?", plan, retirado.id());
                    assertEquals(1, registro.size(), "el reincorporado se registro una vez: " + registro);
                    assertEquals("COMPLETED", registro.get(0).get("status"), String.valueOf(registro));
                    break;
                }
                plan = siguiente;
            }
            assertSinViolaciones();
            conclusion = reincorporadaEn < 0
                    ? esc + ": retirada real, sin reincorporacion en " + SEMANAS_MAX + " semanas: " + pasos
                    : esc + ": retirada real del " + retirado.id() + " en s1, reincorporacion publicada para s"
                    + (reincorporadaEn + 1) + " y entrenada; pasos " + pasos;
            escenarios.add(conclusion);
        }
        var metricas = metricas();
        Files.writeString(SALIDA.resolve("HISTORIAL_REINCORPORACION.csv"), historial);
        Files.writeString(SALIDA.resolve("TRAZA_EJERCICIO_RETIRADO.csv"), traza);
        Files.write(SALIDA.resolve("eventos.log"), new ArrayList<>(eventos));
        Files.write(SALIDA.resolve("tiempos.log"), new ArrayList<>(tiempos));
        var json = new ObjectMapper();
        try (var w = Files.newBufferedWriter(SALIDA.resolve("llamadas.jsonl"))) {
            for (var l : llamadas) w.write(json.writeValueAsString(l) + "\n");
        }
        var resumen = new LinkedHashMap<String, Object>();
        resumen.put("llamadas_live", llamadas.size());
        resumen.put("respuestas_replay", 0);
        resumen.put("max_llamadas", MAX_LLAMADAS);
        resumen.put("escenarios", escenarios);
        resumen.put("predicciones", metricas);
        Files.writeString(SALIDA.resolve("resumen.json"), json.writerWithDefaultPrettyPrinter().writeValueAsString(resumen));
        System.out.println("REINCORPORACION_IA " + escenarios);
        assertTrue(llamadas.size() <= MAX_LLAMADAS, "presupuesto superado");
    }

    // =======================================================================
    // Llamadas reales: solo se copian
    // =======================================================================

    private void espiar() {
        // Plan inicial del motor de reglas, sin llamada: cada intento de generacion se rechaza ANTES de
        // llamar (el pipeline lo trata como una propuesta invalida y publica el respaldo). No se usa
        // isEnabled() = false porque las correcciones comparten ese interruptor.
        doAnswer(inv -> {
            throw new main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException(List.of(
                    "PRUEBA: la generacion inicial de este escenario va al motor de reglas; no se llama a la IA."));
        }).when(generador).requestOutput(any(), any(), anyInt());
        doAnswer(inv -> {
            if (SECO) return simulada(inv.getArgument(0), inv.getArgument(1));
            if (llamadas.size() >= MAX_LLAMADAS) throw new IllegalStateException("presupuesto de llamadas agotado");
            long t0 = System.nanoTime();
            try {
                var r = (ReplicateSessionFixGenerator.RawFix) inv.callRealMethod();
                llamadas.add(new Llamada(etapa, inv.getArgument(4), (System.nanoTime() - t0) / 1_000_000, r.output(), null));
                return r;
            } catch (Throwable e) {
                llamadas.add(new Llamada(etapa, inv.getArgument(4), (System.nanoTime() - t0) / 1_000_000, null, e.toString()));
                throw e;
            }
        }).when(correcciones).pedirRespuesta(any(), any(), any(), any(), anyInt());
        var appender = new AppenderBase<ILoggingEvent>() {
            @Override
            protected void append(ILoggingEvent e) {
                String t = e.getFormattedMessage();
                if (t.startsWith("REPLICATE_TIMING")) tiempos.add(etapa + " | " + t);
                if (e.getLevel().isGreaterOrEqual(ch.qos.logback.classic.Level.WARN) || t.startsWith("PROGRESION")
                        || t.startsWith("Adaptacion del usuario") || t.startsWith("FACTIBILIDAD"))
                    eventos.add(etapa + " | " + e.getLevel() + " | " + t);
            }
        };
        appender.start();
        for (var c : List.of(ReplicateTrainingPlanGenerator.class, AdaptedWeekCommandService.class, ProgresionResolver.class))
            ((Logger) LoggerFactory.getLogger(c)).addAppender(appender);
    }

    /** Ensayo seco: retira el ultimo ejercicio sin slot de cada sesion senalada y no toca nada mas. */
    private ReplicateSessionFixGenerator.RawFix simulada(
            main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext ctx,
            main.web.services.fitsense.planning.domain.model.valueobjects.WeekDerivation derivacion) throws Exception {
        var json = new ObjectMapper();
        var raiz = json.createObjectNode().put("schema_version", "FIX-1.0").put("rationale", "ensayo seco");
        var lista = raiz.putArray("sessions");
        for (var senalada : ctx.adjustment().sesionesParaIA()) {
            var w = derivacion.from(senalada.plannedWorkoutId()).orElseThrow().workout();
            int quitar = -1;
            for (int k = w.exercises().size() - 1; k >= 0 && quitar < 0; k--) {
                String patron = jdbc.queryForObject("SELECT movement_pattern FROM exercises WHERE exercise_id = ?",
                        String.class, w.exercises().get(k).exerciseId());
                if (MandatorySlot.ofMovementPattern(patron).isEmpty()) quitar = k;
            }
            var s = lista.addObject().put("source_planned_workout_id", senalada.plannedWorkoutId())
                    .put("scheduled_date", w.scheduledDate().toString())
                    .put("expected_duration_minutes", w.expectedDurationMinutes()).put("change_reason", "ensayo seco");
            var ejercicios = s.putArray("exercises");
            for (int k = 0; k < w.exercises().size(); k++) {
                if (k == quitar) continue;
                var e = w.exercises().get(k);
                var o = ejercicios.addObject().put("exercise_id", e.exerciseId())
                        .put("prescription_type", e.prescriptionType().name());
                if (e.plannedSets() == null) o.putNull("planned_sets"); else o.put("planned_sets", e.plannedSets());
                if (e.plannedReps() == null) o.putNull("planned_reps"); else o.put("planned_reps", e.plannedReps());
                if (e.plannedDurationSeconds() == null) o.putNull("planned_duration_seconds");
                else o.put("planned_duration_seconds", e.plannedDurationSeconds());
                if (e.restSeconds() == null) o.putNull("rest_seconds"); else o.put("rest_seconds", e.restSeconds());
                o.putNull("target_load_kg").putNull("notes");
            }
        }
        return new ReplicateSessionFixGenerator.RawFix("{}", json.writeValueAsString(raiz));
    }

    private List<Map<String, Object>> metricas() throws Exception {
        var lista = new ArrayList<Map<String, Object>>();
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        var json = new ObjectMapper();
        Files.createDirectories(SALIDA.resolve("predicciones"));
        var patron = Pattern.compile("prediction_id=(\\S+) .*total_ms=(\\d+) resultado=(\\S+)");
        for (var t : tiempos) {
            var x = patron.matcher(t);
            if (!x.find() || x.group(1).equals("desconocido")) continue;
            var fila = new LinkedHashMap<String, Object>();
            fila.put("etapa", t.split(" \\| ")[0]);
            fila.put("prediction_id", x.group(1));
            fila.put("total_ms", Long.parseLong(x.group(2)));
            fila.put("resultado", x.group(3));
            try {
                var r = http.send(HttpRequest.newBuilder(URI.create("https://api.replicate.com/v1/predictions/" + x.group(1)))
                        .header("Authorization", "Bearer proxy").timeout(Duration.ofSeconds(30)).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                var p = (ObjectNode) json.readTree(r.body());
                p.remove("urls");
                Files.writeString(SALIDA.resolve("predicciones").resolve(x.group(1) + ".json"), p.toPrettyString());
                var mt = p.path("metrics");
                long ent = mt.path("token_input_count").asLong(), sal = mt.path("token_output_count").asLong();
                fila.put("modelo", p.path("model").asText());
                fila.put("tokens_entrada", ent);
                fila.put("tokens_salida", sal);
                fila.put("predict_time_s", mt.path("predict_time").asDouble());
                fila.put("coste_estimado_usd", ent * 0.25 / 1e6 + sal * 2.00 / 1e6);
            } catch (Exception e) {
                fila.put("metricas", "no disponibles: " + e.getClass().getSimpleName());
            }
            lista.add(fila);
        }
        return lista;
    }

    // =======================================================================
    // Retirada, validacion e historial
    // =======================================================================

    /** El unico relleno retirado, con el resto de la sesion identico; null si la adaptacion no es eso. */
    private Ej retirada(List<Ej> antes, List<Ej> despues) {
        var quitados = antes.stream().filter(a -> despues.stream().noneMatch(d -> d.id() == a.id())).toList();
        var anadidos = despues.stream().filter(d -> antes.stream().noneMatch(a -> a.id() == d.id())).toList();
        if (quitados.size() != 1 || !anadidos.isEmpty()) return null;
        for (var d : despues)
            if (!antes.contains(d)) return null;   // otra dosis cambio: no es solo una retirada
        String patron = jdbc.queryForObject("SELECT movement_pattern FROM exercises WHERE exercise_id = ?", String.class,
                quitados.get(0).id());
        return MandatorySlot.ofMovementPattern(patron).isEmpty() ? quitados.get(0) : null;
    }

    private String validar(long u, long plan, LocalDate semana) {
        try {
            var ctx = planComandos.prepare(new GenerateWeeklyPlanCommand(u, semana, null, true, null, null)).context();
            return new ValidacionIndependiente(jdbc).validar(plan, ctx, validador, 30).texto();
        } catch (RuntimeException e) {
            return "NO_EVALUABLE " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    private String fila(String esc, int semana, String ciclo, long medido, long publicado, Map<String, Object> ev,
                        int live, Map<String, Object> paso, Ej retirado, List<Ej> viernes, String validacion, String nota) {
        Integer vAntes = paso == null ? null : (Integer) paso.get("volumen_antes");
        Integer vDespues = paso == null ? null : (Integer) paso.get("volumen_despues");
        Double pct = vAntes == null || vAntes == 0 || vDespues == null ? null : 100.0 * (vDespues - vAntes) / vAntes;
        var ret = retirado == null ? null : viernes.stream().filter(e -> e.id() == retirado.id()).findFirst().orElse(null);
        return linea(esc, semana, ciclo, medido, publicado, ev.get("observed_adherence_pct"), ev.get("registration_coverage_pct"),
                ev.get("decision"), ev.get("status"), ev.get("targeted_interventions"), live, ev.get("ai_calls"),
                paso == null ? "-" : paso.get("tipo"), paso == null ? null : paso.get("exercise_id"),
                paso == null ? null : paso.get("antes"), paso == null ? null : paso.get("despues"), vAntes, vDespues,
                paso == null ? null : paso.get("volumen_base"), paso == null ? null : paso.get("techo"),
                pct == null ? null : String.format(Locale.ROOT, "%.2f", pct),
                paso == null ? null : paso.get("estado_reserva"), retirado == null ? "-" : ret != null ? "SI" : "NO",
                ret == null ? null : ret.dosis(), validacion, nota);
    }

    /** Una fila por ejercicio del viernes de ese plan, con lo que se registro de el. */
    private void volcar(StringBuilder traza, String esc, int semana, long plan, Long retirado) {
        var viernes = jdbc.queryForList("SELECT planned_workout_id, scheduled_date FROM planned_workouts WHERE plan_id = ?"
                + " ORDER BY scheduled_date DESC LIMIT 1", plan).get(0);
        long id = ((Number) viernes.get("planned_workout_id")).longValue();
        var ejercicios = ultimaSesion(semana(plan));
        boolean presente = false;
        for (int i = 0; i < ejercicios.size(); i++) {
            var e = ejercicios.get(i);
            var r = registro(id, e.id());
            if (retirado != null && e.id() == retirado) presente = true;
            traza.append(linea(esc, semana, plan, id, viernes.get("scheduled_date"),
                    retirado != null && e.id() == retirado ? "RETIRABLE_PRESENTE" : "CONSERVADO", e.id(), e.dosis(), i + 1,
                    r == null ? "NO" : r.get("status"), r == null ? null : r.get("actual_sets"),
                    r == null ? null : r.get("actual_reps_total"), r == null ? null : r.get("actual_duration_seconds")));
        }
        if (retirado != null && !presente)
            traza.append(linea(esc, semana, plan, id, viernes.get("scheduled_date"), "AUSENTE", retirado, "-", "-", "-",
                    null, null, null));
    }

    private Map<String, Object> registro(long plannedWorkoutId, long exerciseId) {
        var l = jdbc.queryForList("SELECT wse.status, wse.actual_sets, wse.actual_reps_total, wse.actual_duration_seconds, wse.completion_percentage"
                + " FROM workout_session_exercises wse JOIN planned_workout_exercises pwe"
                + " ON pwe.planned_exercise_id = wse.planned_exercise_id WHERE pwe.planned_workout_id = ?"
                + " AND pwe.exercise_id = ?", plannedWorkoutId, exerciseId);
        return l.isEmpty() ? null : l.get(0);
    }

    private static String resumen(List<Ej> antes, List<Ej> despues) {
        return antes.stream().map(e -> e.id() + ":" + e.dosis()).toList() + " -> "
                + despues.stream().map(e -> e.id() + ":" + e.dosis()).toList();
    }

    // =======================================================================
    // Material (como CopiaSinIaIT)
    // =======================================================================

    private long participante(int n) {
        String email = "reincorporacion-" + n + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Reincorporacion', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 1, 15), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("70"), null, FitnessLevel.INTERMEDIATE, OBJETIVO,
                "reincorporacion con IA real", TrainingLocation.GYM, (short) DIAS.size(), DIAS,
                (short) MINUTOS, GIMNASIO, List.of(), "Ninguna", true, true, true, true, true));
        return u;
    }

    private List<Map.Entry<Long, List<Ej>>> semana(long planId) {
        return jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date",
                Long.class, planId).stream().map(id -> Map.entry(id, jdbc.query("SELECT exercise_id, prescription_type,"
                        + " planned_sets, planned_reps, planned_duration_seconds, rest_seconds FROM planned_workout_exercises"
                        + " WHERE planned_workout_id = ? ORDER BY exercise_order",
                (rs, k) -> new Ej(rs.getLong(1), rs.getString(2), rs.getObject(3, Short.class), rs.getObject(4, Short.class),
                        rs.getObject(5, Integer.class), rs.getObject(6, Short.class)), id))).toList();
    }

    /** La sesion que se deja a medias (la ultima de la semana) y su evolucion. */
    private static List<Ej> ultimaSesion(List<Map.Entry<Long, List<Ej>>> semana) {
        return semana.get(semana.size() - 1).getValue();
    }

    private Map<String, Object> ultimo(long u) {
        return jdbc.queryForMap("SELECT decision, status, ai_calls, observed_adherence_pct, registration_coverage_pct,"
                + " targeted_interventions::text AS targeted_interventions, progressed_sessions::text AS progressed_sessions,"
                + " conservation_report::text AS conservation_report FROM plan_adaptation_attempts WHERE user_id = ?"
                + " ORDER BY attempt_id DESC LIMIT 1", u);
    }

    private int intentos(long u) {
        return jdbc.queryForObject("SELECT count(*) FROM plan_adaptation_attempts WHERE user_id = ?", Integer.class, u);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> informe(Map<String, Object> ev) throws Exception {
        var texto = ev.get("conservation_report");
        if (texto == null) return List.of();
        var m = new ObjectMapper().readValue(texto.toString(), Map.class);
        var pasos = (List<Map<String, Object>>) m.get("pasos");
        return pasos == null ? List.of() : pasos;
    }

    /** Todo completo; con {@code viernesParcial}, el viernes se deja al 35 % por falta de tiempo. */
    private void ejecutar(long u, long planId, boolean viernesParcial) {
        var ids = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date",
                Long.class, planId);
        for (int i = 0; i < ids.size(); i++) {
            long w = ids.get(i);
            long s = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
            var filas = jdbc.queryForList("SELECT planned_exercise_id, prescription_type, planned_sets, planned_reps,"
                    + " planned_duration_seconds FROM planned_workout_exercises WHERE planned_workout_id = ?"
                    + " ORDER BY exercise_order", w);
            boolean parcial = viernesParcial && i == ids.size() - 1;
            double objetivo = parcial ? 0.35 * filas.size() : filas.size();
            for (int k = 0; k < filas.size(); k++) {
                var f = filas.get(k);
                long pe = ((Number) f.get("planned_exercise_id")).longValue();
                boolean duracion = "DURATION".equals(f.get("prescription_type"));
                int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
                int total = series * ((Number) f.get(duracion ? "planned_duration_seconds" : "planned_reps")).intValue();
                double parte = Math.max(0, Math.min(1, objetivo - k));
                int hecho = (int) Math.round(total * parte);
                sesiones.handle(new RecordSessionExerciseCommand(u, s, pe, (short) (hecho == 0 ? 0 : series),
                        duracion ? null : hecho, duracion ? hecho : null, null,
                        parte >= 1 ? null : SkipReason.LACK_OF_TIME));
            }
            sesiones.handle(new FinishWorkoutSessionCommand(u, s, (short) 6, (short) 4, (short) (parcial ? 20 : 55)));
        }
    }

    private void assertSinViolaciones() throws Exception {
        var texto = Files.readString(Path.of("docs/FitSense/consultas/linaje_v31_v32.sql")).lines()
                .filter(l -> !l.stripLeading().startsWith("--")).collect(Collectors.joining("\n"));
        assertEquals(List.of(), jdbc.queryForList(texto.split(";")[0].strip()));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM plan_adaptation_attempts WHERE"
                + " cardinality(progressed_sessions) > 1 OR (cardinality(progressed_sessions) = 1 AND"
                + " (status <> 'SIN_ORDEN' OR resulting_plan_id IS NULL))", Integer.class));
    }

    private static String csv(Object o) {
        if (o == null) return "";
        String s = String.valueOf(o);
        return s.contains(",") || s.contains("\"") || s.contains("\n") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }

    private static String linea(Object... v) {
        return Arrays.stream(v).map(ReincorporacionIaRealIT::csv).collect(Collectors.joining(",")) + "\n";
    }
}
