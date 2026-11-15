package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.RelojPruebas;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DOLOR TRAS UNA REDUCCION AUTORIZADA, CON LA CONFIGURACION DE PRODUCCION (orden de 14/11/2026, fase 1-2).
 * <p>
 * PLAN-REV-2.0 (P-A1: ante fatiga, la dosis de mantenimiento, una serie menos en las estructurales), progresion
 * encendida y retirada localizada ante dolor, como en produccion. La IA esta simulada. Escenario: fatiga el viernes,
 * con mantenimiento aplicado; copia que lo hereda; dolor el lunes. Si P-03 nivel 2 regenera con dosis BASE, una
 * colocacion estructural podria volver de 2x10 a 3x10 (restauracion) aunque el total de la semana no suba. Se compara
 * POR COLOCACION ({@link ComparadorDosisPorColocacion}).
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.progression.enabled=true",
        "fitsense.planning.policy=PLAN-REV-2.0",
        "fitsense.planning.policy.allow-candidates=true"
})
@DisplayName("Dolor tras una reduccion autorizada (REV2): ninguna colocacion recupera la dosis reducida sin autorizacion")
class RegeneracionPorColocacionRev2IT {

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
    @Autowired main.web.services.fitsense.planning.domain.services.PlanDraftValidator validador;
    @Autowired main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService planComandos;

    static final LocalDate S1 = RelojPruebas.SEMANA_REFERENCIA;
    static final List<String> EQUIPAMIENTO = List.of("dumbbell", "barbell", "cable", "leverage machine", "bench", "band");

    enum Dia { HECHO, FATIGA, DOLOR }

    /** Que hace la IA simulada en la senalada: devolverla igual o aplicar la dosis de mantenimiento. */
    enum Modo { IGUAL, MANTENIMIENTO }

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

    /** Desenlace de un participante: fatiga (mantenimiento), copia, dolor. */
    record Recorrido(long u, String fatiga, String copia, String dolor, Long medido, Long publicado, String regeneracion,
                     List<Long> excluidos, List<Long> reaparecen, ComparadorDosisPorColocacion.Resultado comparacion,
                     int reducidasEnLaMedida) {}

    Recorrido recorrer(long id) {
        long u = participante(id);
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        modo = Modo.MANTENIMIENTO;
        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.FATIGA);
        var r1 = ciclo.runFor(u, S1, S1.plusWeeks(1));
        String fatiga = estado(u);
        if (r1.isEmpty()) return new Recorrido(u, fatiga, null, null, null, null, null, List.of(), List.of(), null, 0);
        modo = Modo.IGUAL;
        ejecutar(u, r1.get(), Dia.HECHO, Dia.HECHO, Dia.HECHO);
        var r2 = ciclo.runFor(u, S1.plusWeeks(1), S1.plusWeeks(2));
        String copia = estado(u);
        if (r2.isEmpty()) return new Recorrido(u, fatiga, copia, null, null, null, null, List.of(), List.of(), null, 0);
        long medido = r2.get();
        int reducidas = jdbc.queryForObject("SELECT count(*) FROM planned_workouts pw JOIN planned_workout_exercises pwe"
                + " ON pwe.planned_workout_id = pw.planned_workout_id WHERE pw.plan_id = ? AND pwe.planned_sets = 2"
                + " AND pwe.planned_reps = 10", Integer.class, medido);
        ejecutar(u, medido, Dia.DOLOR, Dia.HECHO, Dia.HECHO);
        var r3 = ciclo.runFor(u, S1.plusWeeks(2), S1.plusWeeks(3));
        var excluidos = jdbc.queryForList("SELECT exercise_id FROM user_safety_exclusions WHERE user_id = ? AND status ="
                + " 'ACTIVA'", Long.class, u);
        var reg = jdbc.queryForList("SELECT outcome FROM safe_regeneration_attempts WHERE user_id = ?", String.class, u);
        List<Long> reaparecen = List.of();
        ComparadorDosisPorColocacion.Resultado c = null;
        if (r3.isPresent()) {
            reaparecen = jdbc.queryForList("SELECT DISTINCT pwe.exercise_id FROM planned_workouts pw JOIN"
                    + " planned_workout_exercises pwe ON pwe.planned_workout_id = pw.planned_workout_id WHERE pw.plan_id = ?",
                    Long.class, r3.get()).stream().filter(excluidos::contains).toList();
            c = ComparadorDosisPorColocacion.comparar(colocaciones(medido), colocaciones(r3.get()), Set.copyOf(excluidos));
        }
        return new Recorrido(u, fatiga, copia, estado(u), medido, r3.orElse(null), reg.toString(), excluidos, reaparecen,
                c, reducidas);
    }

    /**
     * 9701: el caso tipico. Mantenimiento ante fatiga el viernes (3x10 -> 2x10 en las estructurales), copia que lo hereda
     * y dolor el lunes: la semana publicada no trae lo que dolio, ninguna colocacion sube (en particular, ninguna
     * reducida vuelve a 3x10: no hay restauracion automatica), el total no sube y la semana valida con sus
     * autorizaciones (la reduccion heredada) segun la validacion independiente.
     */
    @Test
    @DisplayName("mantenimiento por fatiga, copia y dolor: nada de lo que dolio, ninguna restauracion ni subida por colocacion")
    void dolorTrasMantenimiento() {
        comprobar(recorrer(9_701));
    }

    /**
     * 9710: el sustituto del ejercicio que dolio el lunes (375, a 3x10, la dosis de su capacidad) es un ejercicio que la
     * semana medida tiene REDUCIDO el viernes (2x10). No es una restauracion: ocupa el puesto del retirado (misma fecha,
     * misma posicion) y el viernes sigue a 2x10. Sin la correspondencia de sustitucion, la regla conservadora lo
     * marcaria; con ella, se compara con lo que sustituye.
     */
    @Test
    @DisplayName("el sustituto existe reducido otro dia: ocupa el puesto del retirado y la colocacion reducida no se restaura")
    void sustitutoQueExisteReducidoOtroDia() {
        var r = comprobar(recorrer(9_710));
        assertFalse(r.comparacion().sustituciones().isEmpty(), r.comparacion().describir());
        var sinRetirados = ComparadorDosisPorColocacion.comparar(colocaciones(r.medido()), colocaciones(r.publicado()));
        assertFalse(sinRetirados.sinAumentos(), "premisa: sin la correspondencia de sustitucion se marcaria");
    }

    /**
     * 9703 · HALL-GM-01. El ejercicio que dolio el viernes estaba a mantenimiento (2x10). Antes, su sustituto (118)
     * recibia la base de su clase (3x10) y se publicaba una semana con mas trabajo (442 -> 452) y la reduccion
     * deshecha. Ahora el sustituto no supera el trabajo del quitado, V24 rechaza esa dosis (el 118 no tiene ninguna
     * autorizacion), P-03 nivel 2 tampoco puede regenerar sin subir el trabajo, y el desenlace es NO_FACTIBLE: no se
     * publica nada, la semana medida queda intacta y la exclusion por dolor sigue activa.
     */
    @Test
    @DisplayName("HALL-GM-01: el sustituto del ejercicio a mantenimiento no recibe la base; sin alternativa segura, NO_FACTIBLE")
    void sustitutoDeMantenimientoSinMasTrabajo() {
        var r = recorrer(9_703);
        assertEquals("APLICADA", r.fatiga(), "premisa: el mantenimiento se aplico");
        assertEquals("SIN_ORDEN", r.copia(), "premisa: la copia lo hereda");
        assertTrue(r.reducidasEnLaMedida() > 0, "premisa: la semana medida lleva colocaciones reducidas");
        assertEquals("NO_FACTIBLE", r.dolor(), r.toString());
        assertNull(r.publicado(), "no se publica una semana con mas trabajo que la medida: " + r);
        assertEquals("[NO_FACTIBLE]", r.regeneracion(), "P-03 nivel 2 tampoco publica");
        assertFalse(r.excluidos().isEmpty(), "la exclusion por dolor sigue activa");
        var traza = jdbc.queryForMap("SELECT measured_plan_id, resulting_plan_id, adaptation_rejection,"
                + " validation_problems FROM safe_regeneration_attempts WHERE user_id = ?", r.u());
        assertEquals(r.medido(), ((Number) traza.get("measured_plan_id")).longValue());
        assertNull(traza.get("resulting_plan_id"));
        // El rechazo de la adaptacion es V24 sobre el sustituto con la dosis del quitado (no una dosis inventada).
        assertTrue(String.valueOf(traza.get("adaptation_rejection")).contains("V24"), traza.toString());
        assertTrue(String.valueOf(traza.get("validation_problems")).contains("sube el trabajo"), traza.toString());
        long planes = jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?", Long.class, r.u());
        assertEquals(3, planes, "solo los tres planes previos: generado, mantenimiento y copia (la medida)");
        var medidoHoy = colocaciones(r.medido());
        assertEquals(r.reducidasEnLaMedida(), medidoHoy.stream().filter(c -> c.series() == 2 && c.reps() == 10).count(),
                "la semana medida no se toca");
    }

    @Autowired main.web.services.fitsense.planning.application.internal.queryservices.PublishedPlanSafetyService publicados;

    /**
     * P-DOLOR-PUB (cierre tecnico conservador). El lunes se omite por dolor (P-02 excluye sus ejercicios). Los que se
     * repiten en las sesiones todavia ABIERTAS de la misma semana quedan marcados con {@code dolor_registrado}: la
     * sesion se puede empezar (no se bloquea entera), pero ese ejercicio solo se registra como omitido. No se anade
     * ningun sustituto ni ninguna dosis: el plan publicado no cambia.
     */
    @Test
    @DisplayName("P-DOLOR-PUB: el ejercicio que dolio queda marcado en las sesiones abiertas y solo se registra como omitido")
    void dolorMarcadoEnSesionesAbiertas() {
        long u = -1, plan = -1;
        for (long id = 9_750; id < 9_770 && plan < 0; id++) {
            long candidato = participante(id);
            long p = planning.generateWeeklyPlan(candidato, S1, PlanAdjustment.none(), null, null).orElseThrow();
            // Premisa: algun ejercicio del lunes se repite en otra sesion de la semana.
            int repetidos = jdbc.queryForObject("SELECT count(*) FROM planned_workout_exercises a JOIN planned_workouts wa"
                    + " ON wa.planned_workout_id = a.planned_workout_id JOIN planned_workout_exercises b ON b.exercise_id ="
                    + " a.exercise_id JOIN planned_workouts wb ON wb.planned_workout_id = b.planned_workout_id WHERE"
                    + " wa.plan_id = ? AND wb.plan_id = ? AND wb.scheduled_date > wa.scheduled_date AND wa.scheduled_date ="
                    + " (SELECT min(scheduled_date) FROM planned_workouts WHERE plan_id = ?)", Integer.class, p, p, p);
            if (repetidos > 0) {
                u = candidato;
                plan = p;
            }
        }
        assertTrue(plan > 0, "premisa: algun participante 9750-9769 repite un ejercicio del lunes");
        final long usuario = u;
        var antes = colocaciones(plan);
        RelojPruebas.abrirVentanaDeEjecucion(jdbc, u);
        var principal = UserDetailsImpl.build(usuarios.findById(u).orElseThrow());
        var ids = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ? ORDER BY"
                + " scheduled_date", Long.class, plan);
        planes.skip(principal, ids.get(0), new SkipWorkoutResource(SkipReason.PAIN_OR_DISCOMFORT));
        var excluidos = jdbc.queryForList("SELECT exercise_id FROM user_safety_exclusions WHERE user_id = ? AND status ="
                + " 'ACTIVA'", Long.class, u);
        assertFalse(excluidos.isEmpty(), "premisa: P-02 registra la exclusion");
        long abierta = -1;
        Long marcado = null;
        for (long w : ids.subList(1, ids.size())) {
            var repetidos = jdbc.queryForList("SELECT planned_exercise_id FROM planned_workout_exercises WHERE"
                    + " planned_workout_id = ? AND exercise_id = ANY(?::bigint[])", Long.class, w,
                    "{" + String.join(",", excluidos.stream().map(String::valueOf).toList()) + "}");
            if (!repetidos.isEmpty()) {
                abierta = w;
                marcado = repetidos.get(0);
                break;
            }
        }
        assertNotNull(marcado, "premisa: algun ejercicio del lunes se repite en otra sesion abierta");
        var vista = planning.fetchPlannedWorkout(abierta).orElseThrow();
        assertEquals("dolor_registrado", vista.safetyConflicts().get(marcado), vista.safetyConflicts().toString());
        assertFalse(vista.blockedForSafety(), "solo por dolor la sesion no se bloquea entera");
        long s = sesiones.handle(new StartWorkoutSessionCommand(u, abierta)).orElseThrow().getId();
        final long pe = marcado;
        var error = assertThrows(main.web.services.fitsense.execution.domain.exceptions.WorkoutNotAvailableException.class,
                () -> sesiones.handle(new RecordSessionExerciseCommand(usuario, s, pe, (short) 2, 20, null, null, null)));
        assertTrue(error.getMessage().contains("dolor_registrado"), error.getMessage());
        assertTrue(sesiones.handle(new RecordSessionExerciseCommand(u, s, pe, (short) 0, 0, null, null,
                SkipReason.PAIN_OR_DISCOMFORT)).isPresent(), "se puede registrar como omitido");
        assertEquals(antes, colocaciones(plan), "el plan publicado no cambia: sin sustitutos ni dosis nuevas");
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?", Long.class, u));
    }

    private Recorrido comprobar(Recorrido r) {
        assertEquals("APLICADA", r.fatiga(), "premisa: el mantenimiento se aplico");
        assertEquals("SIN_ORDEN", r.copia(), "premisa: la copia lo hereda");
        assertTrue(r.reducidasEnLaMedida() > 0, "premisa: la semana medida lleva colocaciones reducidas");
        assertNotNull(r.publicado(), "la semana con dolor se publica: " + r);
        assertFalse(r.excluidos().isEmpty(), "el dolor deja exclusiones activas");
        assertEquals(List.of(), r.reaparecen(), "reaparece lo que dolio");
        assertTrue(r.comparacion().sinAumentos(), r.comparacion().describir());
        assertTrue(r.comparacion().trabajoTotalNoSube(), r.comparacion().describir());
        var ctx = planComandos.prepare(new main.web.services.fitsense.planning.domain.model.commands
                .GenerateWeeklyPlanCommand(r.u(), S1.plusWeeks(3), null, true, null, null)).context();
        var independiente = new ValidacionIndependiente(jdbc).validar(r.publicado(), ctx, validador, 30);
        assertTrue(independiente.estado().startsWith("VALIDA"), independiente.texto());
        return r;
    }

    /**
     * EXPLORACION (opt-in, -Dfitsense.colocacion.exploracion=true): desenlaces por participante. No afirma nada. Rango por
     * omision 9801-9840, disjunto de los participantes fijos; para recorrer 9701-9740 (-Dfitsense.colocacion.desde/hasta)
     * hay que ejecutarla sola (-Dit.test=RegeneracionPorColocacionRev2IT#exploracion).
     */
    @Test
    @EnabledIfSystemProperty(named = "fitsense.colocacion.exploracion", matches = "true")
    void exploracion() throws Exception {
        var salida = new StringBuilder("user_id;fatiga;copia;dolor;publicado;regeneracion;reducidas_medida;reaparecen;aumentos;detalle\n");
        int desde = Integer.getInteger("fitsense.colocacion.desde", 9_801), hasta = Integer.getInteger("fitsense.colocacion.hasta", 9_840);
        for (long id = desde; id <= hasta; id++) {
            var r = recorrer(id);
            salida.append(id).append(';').append(r.fatiga()).append(';').append(r.copia()).append(';').append(r.dolor())
                    .append(';').append(r.publicado() != null).append(';').append(r.regeneracion()).append(';')
                    .append(r.reducidasEnLaMedida()).append(';').append(r.reaparecen()).append(';')
                    .append(r.comparacion() == null ? "-" : r.comparacion().aumentos().size()).append(';')
                    .append(r.comparacion() == null ? "" : r.comparacion().describir().replace(';', ',')).append('\n');
        }
        var f = Path.of(System.getProperty("fitsense.colocacion.salida", "target/colocacion"));
        Files.createDirectories(f);
        Files.writeString(f.resolve("rev2_reduccion_y_dolor.csv"), salida.toString());
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
            if (modo == Modo.MANTENIMIENTO && senalada.intervencion() == TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION) {
                // P-A1: una serie menos SOLO en las estructurales de 3 series (3x10 -> 2x10); el relleno no baja.
                for (int k = 0; k < lista.size(); k++) {
                    var e = lista.get(k);
                    if (MandatorySlot.ofMovementPattern(ejercicios.get(k).getValue()).isPresent()
                            && e.plannedSets() != null && e.plannedSets() >= 3)
                        lista.set(k, new PlanDraft.DraftExercise(e.exerciseId(), e.prescriptionType(),
                                (short) (e.plannedSets() - 1), e.plannedReps(), e.plannedDurationSeconds(),
                                e.targetLoadKg(), e.restSeconds(), null));
                }
                minutos = estimador.estimateMinutes(new PlanDraft.DraftWorkout(fecha,
                        WorkoutFocus.valueOf((String) f.get("focus_code")), "x", 0, lista), ctx.prescription());
            }
            salida.add(new CorrectedSession(id, fecha, minutos, lista, "propuesta simulada"));
        }
        return salida;
    }

    // =======================================================================
    // Material
    // =======================================================================

    private long participante(long u) {
        jdbc.update("INSERT INTO users (user_id, email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " OVERRIDING SYSTEM VALUE VALUES (?, ?, 'x', 'Prueba', now(), now())", u, "colocacion-" + u + "@pruebas.invalid");
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 5, 10), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("68"), null, FitnessLevel.INTERMEDIATE, GoalType.GAIN_MUSCLE,
                "colocacion", TrainingLocation.GYM, (short) 3, List.of((short) 1, (short) 3, (short) 5), (short) 45,
                EQUIPAMIENTO, List.of(), "Ninguna", true, true, true, true, true));
        return u;
    }

    private String estado(long u) {
        return jdbc.queryForObject("SELECT status FROM plan_adaptation_attempts WHERE user_id = ? ORDER BY attempt_id DESC"
                + " LIMIT 1", String.class, u);
    }

    List<ComparadorDosisPorColocacion.Colocacion> colocaciones(long plan) {
        return jdbc.query("SELECT pw.scheduled_date, pwe.exercise_id, coalesce(pwe.planned_sets, 1),"
                        + " coalesce(pwe.planned_reps, 0), coalesce(pwe.planned_duration_seconds, 0), pwe.exercise_order FROM planned_workouts pw"
                        + " JOIN planned_workout_exercises pwe ON pwe.planned_workout_id = pw.planned_workout_id"
                        + " WHERE pw.plan_id = ? AND pw.status <> 'REPLACED' ORDER BY pw.scheduled_date, pwe.exercise_order",
                (rs, k) -> new ComparadorDosisPorColocacion.Colocacion(rs.getDate(1).toLocalDate(), rs.getLong(2),
                        rs.getInt(3), rs.getInt(4), rs.getInt(5), rs.getInt(6)), plan);
    }

    private void ejecutar(long u, long planId, Dia... dias) {
        RelojPruebas.abrirVentanaDeEjecucion(jdbc, u);
        var principal = UserDetailsImpl.build(usuarios.findById(u).orElseThrow());
        var ids = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                + " ORDER BY scheduled_date", Long.class, planId);
        for (int i = 0; i < ids.size(); i++) {
            long w = ids.get(i);
            switch (dias[i]) {
                case HECHO -> {
                    long s = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
                    // P-DOLOR-PUB: lo que la sesion marca por dolor registrado solo se registra como omitido.
                    var marcados = planning.fetchPlannedWorkout(w).map(v -> v.safetyConflicts().keySet())
                            .orElse(Set.of());
                    for (var f : jdbc.queryForList("SELECT planned_exercise_id, prescription_type, planned_sets,"
                            + " planned_reps, planned_duration_seconds FROM planned_workout_exercises"
                            + " WHERE planned_workout_id = ?", w)) {
                        if (marcados.contains(((Number) f.get("planned_exercise_id")).longValue())) {
                            sesiones.handle(new RecordSessionExerciseCommand(u, s,
                                    ((Number) f.get("planned_exercise_id")).longValue(), (short) 0, 0, null, null,
                                    SkipReason.OTHER));
                            continue;
                        }
                        boolean duracion = "DURATION".equals(f.get("prescription_type"));
                        int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
                        int total = series * ((Number) f.get(duracion ? "planned_duration_seconds" : "planned_reps")).intValue();
                        sesiones.handle(new RecordSessionExerciseCommand(u, s, ((Number) f.get("planned_exercise_id")).longValue(),
                                (short) series, duracion ? null : total, duracion ? total : null, null, null));
                    }
                    sesiones.handle(new FinishWorkoutSessionCommand(u, s, (short) 6, (short) 4, (short) 45));
                }
                case FATIGA -> planes.skip(principal, w, new SkipWorkoutResource(SkipReason.FATIGUE));
                case DOLOR -> planes.skip(principal, w, new SkipWorkoutResource(SkipReason.PAIN_OR_DISCOMFORT));
            }
        }
    }
}
