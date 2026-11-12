package main.web.services.fitsense.orchestation;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import main.web.services.fitsense.RelojPruebas;
import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.application.internal.commandservices.AdaptedWeekCommandService;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.LoggerFactory;
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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DIAGNOSTICO DEL RESPALDO MULTISEMANA (orden final de estabilizacion, fase 6). Laboratorio opt-in
 * ({@code -Dfitsense.diagnostico.respaldo=true}), sin Replicate y sin cambios en {@code src/main}.
 * <p>
 * Reproduce las secuencias con dolor de {@link ValidacionMultisemanaIT} («7 dolor» y «10 varios motivos») con los
 * MISMOS perfiles, la MISMA ejecucion de cada dia y la MISMA IA simulada, en varias semanas de inicio (por omision
 * 2026-10-26, 2026-11-02 y 2026-11-09) y con varias replicas (usuarios distintos, es decir, semillas distintas). El
 * reloj lo controla la prueba ({@link RelojPruebas}): ninguna sesion vence.
 * <p>
 * NO AFIRMA la igualdad «RESPALDO => semana identica»: la REGISTRA (columna {@code asercion_actual}) junto con lo que
 * cambio, por que y con que regla. Lo que SI afirma son las invariantes de las politicas ya acordadas, que deben
 * cumplirse en cualquier semilla:
 * <ul>
 *   <li><b>B</b> · las sesiones no senaladas no cambian (ejercicios, orden, dosis, enfoque, minutos);</li>
 *   <li><b>D</b> · lo que dolio no vuelve, ni en esa semana ni en las siguientes;</li>
 *   <li><b>D/SUST-SEG-1</b> · en la sesion del dolor, con RESPALDO, solo cambia el ejercicio excluido; el sustituto
 *       tiene el mismo tipo de prescripcion y la misma clase de slot, va en la misma posicion, no sube series,
 *       repeticiones, segundos ni descanso, y la sesion no sube su trabajo total; la evidencia V32 lleva la nota
 *       {@code REGENERACION_SEGURA} y la sustitucion;</li>
 *   <li><b>A</b> · en las semanas sin motivo (todo hecho), la semana siguiente es identica: el respaldo no deja
 *       variaciones sueltas que se arrastren;</li>
 *   <li><b>E</b> · nunca se publica una semana con violaciones de linaje.</li>
 * </ul>
 * Salida en {@code target/diagnostico-respaldo/} (o en la carpeta de entregables con
 * {@code -Dfitsense.laboratorio.capturar=true}).
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.diagnostico.respaldo", matches = "true")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.progression.enabled=false"
})
class DiagnosticoRespaldoMultisemanaIT {

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
    @Autowired SessionDurationEstimator estimador;
    @Autowired JdbcTemplate jdbc;

    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final List<String> GIMNASIO = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine",
            "bench", "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine");
    static final List<LocalDate> SEMANAS = Arrays.stream(System.getProperty("fitsense.diagnostico.semanas",
            "2026-10-26,2026-11-02,2026-11-09").split(",")).map(String::strip).map(LocalDate::parse).toList();
    static final int REPLICAS = Integer.getInteger("fitsense.diagnostico.replicas", 3);
    static final Path SALIDA = Boolean.getBoolean("fitsense.laboratorio.capturar")
            ? Path.of("docs/FitSense/estabilizacion_ci_y_respaldo/evidencia") : Path.of("target/diagnostico-respaldo");
    static final ObjectMapper JSON = new ObjectMapper();

    enum Dia { HECHO, TIEMPO, PARCIAL_DOLOR }

    record Ej(long id, String tipo, Short series, Short reps, Integer segundos, Short descanso) {
        String dosis() {
            return "DURATION".equals(tipo) ? series + "x" + segundos + "s/" + descanso : series + "x" + reps + "/" + descanso;
        }
    }

    record Secuencia(String nombre, Dia[][] semanas) {}

    static final Dia H = Dia.HECHO;
    static final List<Secuencia> SECUENCIAS = List.of(
            new Secuencia("7 dolor", new Dia[][]{{H, Dia.PARCIAL_DOLOR, H}, {H, H, H}, {H, H, H}}),
            new Secuencia("10 varios motivos", new Dia[][]{{H, Dia.PARCIAL_DOLOR, Dia.TIEMPO}, {H, H, H}}));

    /** Avisos del servicio de adaptacion (los rechazos de cada propuesta de la IA simulada). */
    final Queue<String> avisos = new ConcurrentLinkedQueue<>();

    @Test
    void diagnostico() throws Exception {
        reset(ia);
        when(ia.isEnabled()).thenReturn(true);
        when(ia.modelName()).thenReturn("simulado");
        when(ia.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{}", "salida-simulada"));
        when(ia.interpretar(anyString(), any())).thenAnswer(inv -> corregir(inv.getArgument(1)));
        var appender = new AppenderBase<ILoggingEvent>() {
            @Override
            protected void append(ILoggingEvent e) {
                if (e.getLevel().isGreaterOrEqual(ch.qos.logback.classic.Level.WARN)) avisos.add(e.getFormattedMessage());
            }
        };
        appender.start();
        ((Logger) LoggerFactory.getLogger(AdaptedWeekCommandService.class)).addAppender(appender);

        Files.createDirectories(SALIDA);
        var ciclos = new StringBuilder("semana_inicio;objetivo;secuencia;replica;usuario;ciclo;decision;status;"
                + "intervenciones;sesiones_senaladas;origen;llamadas_ia;nota_regeneracion_segura;sustituciones;"
                + "excluidos_por_dolor;asercion_actual;sesiones_cambiadas;rechazos_ia\n");
        var sesionesCsv = new StringBuilder("semana_inicio;objetivo;secuencia;replica;usuario;ciclo;status;sesion;"
                + "senalada;enfoque;minutos_antes;minutos_despues;antes;despues;posiciones_cambiadas;comprobacion\n");
        var invariantes = new ArrayList<String>();
        var resumen = new TreeMap<String, int[]>();   // semana|status -> [ciclos, asercion falla]

        for (var semana : SEMANAS)
            for (var objetivo : GoalType.values())
                for (var sec : SECUENCIAS)
                    for (int r = 1; r <= REPLICAS; r++)
                        recorrer(semana, objetivo, sec, r, ciclos, sesionesCsv, invariantes, resumen);

        Files.writeString(SALIDA.resolve("ciclos.csv"), ciclos);
        Files.writeString(SALIDA.resolve("sesiones.csv"), sesionesCsv);
        var md = new StringBuilder("# Diagnostico del respaldo multisemana (laboratorio)\n\n| Semana | Status del ciclo con"
                + " dolor y siguientes | Ciclos | La asercion actual fallaria |\n|---|---|---|---|\n");
        resumen.forEach((k, v) -> md.append("| ").append(k.replace("|", " | ")).append(" | ").append(v[0]).append(" | ")
                .append(v[1]).append(" |\n"));
        md.append("\nInvariantes de politica incumplidas: **").append(invariantes.size()).append("**\n\n");
        invariantes.forEach(i -> md.append("- ").append(i).append('\n'));
        Files.writeString(SALIDA.resolve("resumen.md"), md);
        System.out.println(md);
        assertEquals(List.of(), invariantes, "invariantes de politica");
    }

    private void recorrer(LocalDate s1, GoalType objetivo, Secuencia sec, int replica, StringBuilder ciclos,
                          StringBuilder sesionesCsv, List<String> invariantes, Map<String, int[]> resumen)
            throws Exception {
        long u = participante(objetivo);
        long plan = planning.generateWeeklyPlan(u, s1, PlanAdjustment.none(), null, null).orElseThrow();
        var dolieron = new HashSet<Long>();
        for (int k = 0; k < sec.semanas().length; k++) {
            String clave = "%s %s %s r%d u%d c%d".formatted(s1, objetivo, sec.nombre(), replica, u, k + 1);
            var antes = semana(plan);
            var minutosAntes = estructura(plan, 7);
            ejecutar(u, plan, sec.semanas()[k]);
            dolieron.addAll(jdbc.queryForList("SELECT pwe.exercise_id FROM workout_session_exercises wse JOIN"
                    + " planned_workout_exercises pwe ON pwe.planned_exercise_id = wse.planned_exercise_id JOIN"
                    + " planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id WHERE pw.plan_id = ?"
                    + " AND wse.skip_reason = 'PAIN_OR_DISCOMFORT'", Long.class, plan));
            avisos.clear();
            var publicado = ciclo.runFor(u, s1.plusWeeks(k), s1.plusWeeks(k + 1));
            var ev = ultimo(u);
            String status = String.valueOf(ev.get("status"));
            var rechazos = avisos.stream().filter(a -> a.contains("usuario " + u + ":"))
                    .map(a -> a.replace(';', ',').replace('\n', ' ')).collect(Collectors.joining(" || "));
            if (publicado.isEmpty()) {
                // E: sin semana valida no se publica nada; se registra y no se sigue la secuencia.
                ciclos.append(String.join(";", s1.toString(), objetivo.name(), sec.nombre(), "" + replica, "" + u,
                        "" + (k + 1), "" + ev.get("decision"), status, "" + ev.get("targeted_interventions"),
                        "" + ev.get("targeted_sessions"), "" + ev.get("generation_source"), "" + ev.get("ai_calls"),
                        "", "", "", "SIN_PUBLICAR", "", rechazos)).append('\n');
                resumen.computeIfAbsent(s1 + "|" + status, x -> new int[2])[0]++;
                break;
            }
            plan = publicado.get();
            var despues = semana(plan);
            var minutosDespues = estructura(plan, 0);
            var senaladas = senaladas(ev, antes);
            var informe = JSON.readTree(String.valueOf(ev.get("conservation_report")));
            boolean nota = String.valueOf(ev.get("conservation_report")).contains("REGENERACION_SEGURA");
            var sustituciones = sustituciones(informe);
            var excluidos = jdbc.queryForList("SELECT exercise_id FROM user_safety_exclusions WHERE user_id = ?"
                    + " AND status = 'ACTIVA' ORDER BY exercise_id", Long.class, u);

            // La asercion actual de ValidacionMultisemanaIT, evaluada (no afirmada).
            String interv = String.valueOf(ev.get("targeted_interventions"));
            boolean exigeIgualdad = interv.equals("{}") || interv.equals("{CONSERVAR}") || "SIN_EFECTO".equals(status)
                    || "RESPALDO".equals(status);
            boolean identica = true;
            var cambiadas = new ArrayList<Integer>();
            for (int i = 0; i < antes.size(); i++)
                if (!antes.get(i).getValue().equals(despues.get(i).getValue())) { identica = false; cambiadas.add(i); }
            if (!minutosAntes.equals(minutosDespues)) identica = false;
            String asercion = !exigeIgualdad ? "NO_APLICA" : identica ? "PASA" : "FALLA";
            var rs = resumen.computeIfAbsent(s1 + "|" + status, x -> new int[2]);
            rs[0]++;
            if ("FALLA".equals(asercion)) rs[1]++;

            ciclos.append(String.join(";", s1.toString(), objetivo.name(), sec.nombre(), "" + replica, "" + u,
                    "" + (k + 1), "" + ev.get("decision"), status, interv, "" + ev.get("targeted_sessions"),
                    "" + ev.get("generation_source"), "" + ev.get("ai_calls"), nota ? "SI" : "NO",
                    sustituciones.stream().map(x -> x.get("quitado") + "->" + x.get("puesto") + " (" + x.get("dosisAnterior")
                            + " => " + x.get("dosisNueva") + ")").collect(Collectors.joining(" | ")),
                    excluidos.toString(), asercion, cambiadas.toString(), rechazos)).append('\n');

            for (int i = 0; i < antes.size(); i++) {
                var a = antes.get(i).getValue();
                var d = despues.get(i).getValue();
                boolean esSenalada = senaladas.contains(antes.get(i).getKey());
                var posiciones = new ArrayList<Integer>();
                for (int p = 0; p < Math.max(a.size(), d.size()); p++)
                    if (p >= a.size() || p >= d.size() || !a.get(p).equals(d.get(p))) posiciones.add(p);
                var problemas = new ArrayList<String>();
                String mA = minutosAntes.get(i), mD = minutosDespues.get(i);
                if (!esSenalada) {
                    // B: lo no senalado no cambia.
                    if (!a.equals(d)) problemas.add("B: cambia una sesion no senalada");
                    if (!mA.equals(mD)) problemas.add("B: cambian fecha/enfoque/minutos de una no senalada");
                } else if ("RESPALDO".equals(status)) {
                    problemas.addAll(comprobarSustitucion(a, d, dolieron, mA, mD));
                    if (!posiciones.isEmpty() && !nota) problemas.add("D: cambia sin nota REGENERACION_SEGURA en V32");
                    for (int p : posiciones) {
                        var quitado = a.get(p).id();
                        var puesto = d.get(p).id();
                        boolean registrada = sustituciones.stream().anyMatch(x -> Objects.equals(x.get("quitado"), "" + quitado)
                                && Objects.equals(x.get("puesto"), "" + puesto) && "SUST-SEG-1".equals(x.get("regla")));
                        if (!registrada) problemas.add("D: la sustitucion " + quitado + "->" + puesto + " no esta en V32");
                    }
                }
                for (var e : d)
                    if (dolieron.contains(e.id())) problemas.add("D: vuelve el ejercicio que dolio " + e.id());
                for (var pr : problemas) invariantes.add(clave + " sesion " + i + ": " + pr);
                sesionesCsv.append(String.join(";", s1.toString(), objetivo.name(), sec.nombre(), "" + replica, "" + u,
                        "" + (k + 1), status, "" + i, esSenalada ? "SI" : "NO", mA.split("\\|")[1], mA.split("\\|")[2],
                        mD.split("\\|")[2], lista(a), lista(d), posiciones.toString(),
                        problemas.isEmpty() ? "OK" : String.join(" | ", problemas))).append('\n');
            }
            // A: con la semana entera hecha y sin orden, la siguiente es identica (continuidad).
            if (interv.equals("{}") && (!identica))
                invariantes.add(clave + ": A: sin motivo, la semana siguiente no es identica " + cambiadas);
            // E: linaje sin violaciones y una sola progresion como maximo (aqui, ninguna).
            var texto = Files.readString(Path.of("docs/FitSense/consultas/linaje_v31_v32.sql")).lines()
                    .filter(l -> !l.stripLeading().startsWith("--")).collect(Collectors.joining("\n"));
            if (!jdbc.queryForList(texto.split(";")[0].strip()).isEmpty()) invariantes.add(clave + ": E: violaciones de linaje");
        }
    }

    /**
     * SUST-SEG-1 en una sesion senalada por dolor resuelta por el respaldo: misma longitud, mismos ejercicios en las
     * mismas posiciones salvo los excluidos; cada cambio sustituye a un ejercicio que dolio por otro del mismo tipo y
     * clase de slot, sin subir la dosis; la sesion no sube su trabajo total; fecha, enfoque y minutos iguales.
     */
    private List<String> comprobarSustitucion(List<Ej> a, List<Ej> d, Set<Long> dolieron, String mA, String mD) {
        var p = new ArrayList<String>();
        if (!mA.equals(mD)) p.add("D: cambian fecha/enfoque/minutos de la sesion del dolor (" + mA + " -> " + mD + ")");
        if (a.size() != d.size()) { p.add("D: cambia el numero de ejercicios " + a.size() + " -> " + d.size()); return p; }
        long repsA = 0, repsD = 0, segA = 0, segD = 0;
        for (int i = 0; i < a.size(); i++) {
            var x = a.get(i);
            var y = d.get(i);
            int sx = x.series() == null ? 1 : x.series(), sy = y.series() == null ? 1 : y.series();
            repsA += (long) sx * (x.reps() == null ? 0 : x.reps());
            repsD += (long) sy * (y.reps() == null ? 0 : y.reps());
            segA += (long) sx * (x.segundos() == null ? 0 : x.segundos());
            segD += (long) sy * (y.segundos() == null ? 0 : y.segundos());
            if (x.equals(y)) continue;
            if (x.id() == y.id()) { p.add("D: cambia la dosis de " + x.id() + " sin sustituirlo"); continue; }
            if (!dolieron.contains(x.id())) p.add("D: se sustituye " + x.id() + ", que no dolio");
            if (!x.tipo().equals(y.tipo())) p.add("D: el sustituto " + y.id() + " cambia el tipo " + x.tipo() + " -> " + y.tipo());
            var slotX = slot(x.id());
            var slotY = slot(y.id());
            if (!slotX.equals(slotY)) p.add("D: el sustituto " + y.id() + " cambia la clase de slot " + slotX + " -> " + slotY);
            // «Dosis recalculada y nunca mayor» (SUST-SEG-1): ni series, ni repeticiones, ni segundos suben. El
            // descanso puede ser el de la clase del sustituto (4D en GAIN_MUSCLE): se registra en sesiones.csv.
            if (mayor(y.series(), x.series()) || mayor(y.reps(), x.reps()) || mayor(y.segundos(), x.segundos()))
                p.add("D: el sustituto " + y.id() + " sube la dosis: " + x.dosis() + " -> " + y.dosis());
        }
        if (repsD > repsA || segD > segA)
            p.add("D: la sesion del dolor sube el trabajo (reps " + repsA + "->" + repsD + ", seg " + segA + "->" + segD + ")");
        return p;
    }

    private static boolean mayor(Number nuevo, Number antes) {
        return nuevo != null && antes != null && nuevo.longValue() > antes.longValue();
    }

    private String slot(long exerciseId) {
        var patron = jdbc.queryForObject("SELECT movement_pattern FROM exercises WHERE exercise_id = ?", String.class, exerciseId);
        return MandatorySlot.ofMovementPattern(patron).map(Enum::name).orElse("relleno");
    }

    private static List<Map<String, String>> sustituciones(JsonNode informe) {
        var salida = new ArrayList<Map<String, String>>();
        for (var s : informe.path("sustituciones")) {
            var m = new HashMap<String, String>();
            s.fields().forEachRemaining(f -> m.put(f.getKey(), f.getValue().asText()));
            salida.add(m);
        }
        return salida;
    }

    /** Ids de las sesiones senaladas de la semana medida (las de {@code antes}). */
    private Set<Long> senaladas(Map<String, Object> ev, List<Map.Entry<Long, List<Ej>>> antes) {
        var texto = String.valueOf(ev.get("targeted_sessions")).replaceAll("[{}\\s]", "");
        if (texto.isEmpty() || "null".equals(texto)) return Set.of();
        return Arrays.stream(texto.split(",")).map(Long::valueOf).collect(Collectors.toSet());
    }

    private static String lista(List<Ej> l) {
        return l.stream().map(e -> e.id() + ":" + e.dosis()).collect(Collectors.joining(" "));
    }

    // =======================================================================
    // IA simulada: la de ValidacionMultisemanaIT para SEGURIDAD_ANTE_DOLOR (las demas, sin cambios)
    // =======================================================================

    private List<CorrectedSession> corregir(PlanGenerationContext ctx) {
        var salida = new ArrayList<CorrectedSession>();
        for (var senalada : ctx.adjustment().targetedSessions()) {
            long id = senalada.plannedWorkoutId();
            var f = jdbc.queryForMap("SELECT scheduled_date, focus_code FROM planned_workouts WHERE planned_workout_id = ?", id);
            var actuales = jdbc.query("SELECT pwe.exercise_id, pwe.prescription_type, pwe.planned_sets, pwe.planned_reps,"
                            + " pwe.planned_duration_seconds, pwe.rest_seconds FROM planned_workout_exercises pwe"
                            + " WHERE pwe.planned_workout_id = ? ORDER BY pwe.exercise_order",
                    (rs, k) -> new PlanDraft.DraftExercise(rs.getLong(1), PrescriptionType.valueOf(rs.getString(2)),
                            rs.getObject(3, Short.class), rs.getObject(4, Short.class), rs.getObject(5, Integer.class), null,
                            rs.getObject(6, Short.class), null), id);
            int quitar = -1;
            Long sustituto = null;
            if (senalada.intervencion() == TipoIntervencion.SEGURIDAD_ANTE_DOLOR) {
                var dolio = jdbc.queryForList("SELECT pwe.exercise_id FROM workout_session_exercises wse"
                        + " JOIN planned_workout_exercises pwe ON pwe.planned_exercise_id = wse.planned_exercise_id"
                        + " WHERE pwe.planned_workout_id = ? AND wse.skip_reason = 'PAIN_OR_DISCOMFORT'", Long.class, id);
                for (int k = 0; k < actuales.size() && quitar < 0; k++)
                    if (dolio.contains(actuales.get(k).exerciseId())) quitar = k;
                if (quitar >= 0) {
                    long idDolio = actuales.get(quitar).exerciseId();
                    var o = jdbc.queryForMap("SELECT e.movement_pattern, bp.code FROM exercises e JOIN body_parts bp"
                            + " ON bp.body_part_id = e.body_part_id WHERE e.exercise_id = ?", idDolio);
                    var enSesion = actuales.stream().map(PlanDraft.DraftExercise::exerciseId).collect(Collectors.toSet());
                    sustituto = ctx.availableExercises().stream()
                            .filter(c -> !enSesion.contains(c.exerciseId()) && !dolio.contains(c.exerciseId())
                                    && Objects.equals(c.movementPattern(), o.get("movement_pattern"))
                                    && Objects.equals(c.bodyPartCode(), o.get("code")))
                            .map(CandidateExercise::exerciseId).findFirst().orElse(null);
                }
            }
            var lista = new ArrayList<PlanDraft.DraftExercise>();
            for (int k = 0; k < actuales.size(); k++) {
                var e = actuales.get(k);
                if (k == quitar) {
                    if (sustituto != null)
                        lista.add(new PlanDraft.DraftExercise(sustituto, e.prescriptionType(), e.plannedSets(),
                                e.plannedReps(), e.plannedDurationSeconds(), null, e.restSeconds(), null));
                    continue;
                }
                lista.add(new PlanDraft.DraftExercise(e.exerciseId(), e.prescriptionType(), e.plannedSets(), e.plannedReps(),
                        e.plannedDurationSeconds(), null, e.restSeconds(), null));
            }
            var fecha = ((java.sql.Date) f.get("scheduled_date")).toLocalDate().plusWeeks(1);
            int min = estimador.estimateMinutes(new PlanDraft.DraftWorkout(fecha,
                    WorkoutFocus.valueOf((String) f.get("focus_code")), "x", 0, lista), ctx.prescription());
            salida.add(new CorrectedSession(id, fecha, min, lista, "ajuste simulado"));
        }
        return salida;
    }

    // =======================================================================
    // Material (identico a ValidacionMultisemanaIT)
    // =======================================================================

    private long participante(GoalType objetivo) {
        String email = "diag-respaldo" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 1, 15), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("70"), null, FitnessLevel.INTERMEDIATE, objetivo, "sin ia",
                TrainingLocation.GYM, (short) 3, List.of((short) 1, (short) 3, (short) 5), (short) 45, GIMNASIO,
                List.of(), "Ninguna", true, true, true, true, true));
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

    private List<String> estructura(long planId, int dias) {
        return jdbc.queryForList("SELECT (scheduled_date + ?) || '|' || focus_code || '|' || expected_duration_minutes"
                + " FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date", String.class, dias, planId);
    }

    private Map<String, Object> ultimo(long u) {
        return jdbc.queryForMap("SELECT decision, status, generation_source, ai_calls, targeted_interventions::text,"
                + " targeted_sessions::text, conservation_report::text FROM plan_adaptation_attempts WHERE user_id = ?"
                + " ORDER BY attempt_id DESC LIMIT 1", u);
    }

    private void ejecutar(long u, long planId, Dia... dias) {
        RelojPruebas.abrirVentanaDeEjecucion(jdbc, u);
        var ids = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date",
                Long.class, planId);
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
                case TIEMPO -> planes.skip(UserDetailsImpl.build(usuarios.findById(u).orElseThrow()), w,
                        new SkipWorkoutResource(SkipReason.LACK_OF_TIME));
                case PARCIAL_DOLOR -> {
                    long s = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
                    var filas = jdbc.queryForList("SELECT planned_exercise_id, prescription_type, planned_sets,"
                            + " planned_reps, planned_duration_seconds FROM planned_workout_exercises"
                            + " WHERE planned_workout_id = ? ORDER BY exercise_order", w);
                    for (int k = 0; k < filas.size(); k++) {
                        var f = filas.get(k);
                        long pe = ((Number) f.get("planned_exercise_id")).longValue();
                        if (k == filas.size() - 1) {
                            sesiones.handle(new RecordSessionExerciseCommand(u, s, pe, (short) 0, null, null, null,
                                    SkipReason.PAIN_OR_DISCOMFORT));
                            continue;
                        }
                        boolean duracion = "DURATION".equals(f.get("prescription_type"));
                        int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
                        int total = series * ((Number) f.get(duracion ? "planned_duration_seconds" : "planned_reps")).intValue();
                        sesiones.handle(new RecordSessionExerciseCommand(u, s, pe, (short) series,
                                duracion ? null : total, duracion ? total : null, null, null));
                    }
                    sesiones.handle(new FinishWorkoutSessionCommand(u, s, (short) 6, (short) 4, (short) 45));
                }
            }
        }
    }
}
