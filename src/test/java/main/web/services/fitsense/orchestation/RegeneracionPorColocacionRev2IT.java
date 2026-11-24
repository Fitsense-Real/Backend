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
    @Autowired main.web.services.fitsense.planning.application.internal.commandservices.PainMemoryService dolorEfectivo;
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
                     int reducidasEnLaMedida, int trabajoTrasDolor) {}

    Recorrido recorrer(long id) {
        long u = participante(id);
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        modo = Modo.MANTENIMIENTO;
        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.FATIGA);
        var r1 = ciclo.runFor(u, S1, S1.plusWeeks(1));
        String fatiga = estado(u);
        if (r1.isEmpty()) return new Recorrido(u, fatiga, null, null, null, null, null, List.of(), List.of(), null, 0, 0);
        modo = Modo.IGUAL;
        ejecutar(u, r1.get(), Dia.HECHO, Dia.HECHO, Dia.HECHO);
        var r2 = ciclo.runFor(u, S1.plusWeeks(1), S1.plusWeeks(2));
        String copia = estado(u);
        if (r2.isEmpty()) return new Recorrido(u, fatiga, copia, null, null, null, null, List.of(), List.of(), null, 0, 0);
        long medido = r2.get();
        int reducidas = jdbc.queryForObject("SELECT count(*) FROM planned_workouts pw JOIN planned_workout_exercises pwe"
                + " ON pwe.planned_workout_id = pw.planned_workout_id WHERE pw.plan_id = ? AND pwe.planned_sets = 2"
                + " AND pwe.planned_reps = 10", Integer.class, medido);
        ejecutar(u, medido, Dia.DOLOR, Dia.HECHO, Dia.HECHO);
        var r3 = ciclo.runFor(u, S1.plusWeeks(2), S1.plusWeeks(3));
        var excluidos = jdbc.queryForList("SELECT exercise_id FROM user_safety_exclusions WHERE user_id = ? AND status ="
                + " 'ACTIVA'", Long.class, u);
        var reg = jdbc.queryForList("SELECT outcome FROM safe_regeneration_attempts WHERE user_id = ?", String.class, u);
        // P-DOLOR-PUB: en las sesiones de la semana medida posteriores al dolor, ningun trabajo registrado en lo que dolio.
        int trabajoTrasDolor = excluidos.isEmpty() ? 0 : jdbc.queryForObject("SELECT count(*) FROM"
                + " workout_session_exercises wse JOIN planned_workout_exercises pwe ON pwe.planned_exercise_id ="
                + " wse.planned_exercise_id JOIN planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id"
                + " WHERE pw.plan_id = ? AND pw.scheduled_date > (SELECT min(scheduled_date) FROM planned_workouts WHERE"
                + " plan_id = ?) AND coalesce(wse.actual_sets, 0) > 0 AND pwe.exercise_id = ANY(?::bigint[])", Integer.class,
                medido, medido, "{" + String.join(",", excluidos.stream().map(String::valueOf).toList()) + "}");
        List<Long> reaparecen = List.of();
        ComparadorDosisPorColocacion.Resultado c = null;
        if (r3.isPresent()) {
            reaparecen = jdbc.queryForList("SELECT DISTINCT pwe.exercise_id FROM planned_workouts pw JOIN"
                    + " planned_workout_exercises pwe ON pwe.planned_workout_id = pw.planned_workout_id WHERE pw.plan_id = ?",
                    Long.class, r3.get()).stream().filter(excluidos::contains).toList();
            c = ComparadorDosisPorColocacion.comparar(colocaciones(medido), colocaciones(r3.get()), Set.copyOf(excluidos));
        }
        return new Recorrido(u, fatiga, copia, estado(u), medido, r3.orElse(null), reg.toString(), excluidos, reaparecen,
                c, reducidas, trabajoTrasDolor);
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
        var r = sinContencion(() -> comprobar(recorrer(9_710)));
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
        var r = sinContencion(() -> recorrer(9_703));
        assertEquals("APLICADA", r.fatiga(), "premisa: el mantenimiento se aplico");
        assertEquals("SIN_ORDEN", r.copia(), "premisa: la copia lo hereda");
        assertTrue(r.reducidasEnLaMedida() > 0, "premisa: la semana medida lleva colocaciones reducidas");
        assertEquals("NO_FACTIBLE", r.dolor(), r.toString());
        assertNull(r.publicado(), "no se publica una semana con mas trabajo que la medida: " + r);
        assertEquals("[NO_FACTIBLE]", r.regeneracion(), "P-03 nivel 2 tampoco publica");
        assertFalse(r.excluidos().isEmpty(), "la exclusion por dolor sigue activa");
        assertEquals(0, r.trabajoTrasDolor(), "caso 8 (NO_FACTIBLE): nada de lo que dolio se hizo en la semana medida");
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

    /**
     * HALL-GM-01 con la version candidata completa (contencion INC-CAT-CONS-v1 y P-DOLOR-PUB encendidas). 9717 es uno de
     * los 5 de 40 participantes (9701-9740) cuyo desenlace es NO_FACTIBLE con la contencion: el sustituto del ejercicio
     * que dolio no puede recibir mas trabajo que el quitado, V24 rechaza la adaptacion y P-03 nivel 2 tampoco publica
     * porque subiria el trabajo. Nada se publica, la semana medida queda intacta y nada de lo que dolio se hizo despues.
     */
    @Test
    @DisplayName("HALL-GM-01 con la contencion conservadora: sin alternativa segura, NO_FACTIBLE y nada publicado")
    void halGm01ConContencion() {
        var r = recorrer(9_717);
        assertEquals("APLICADA", r.fatiga(), "premisa: el mantenimiento se aplico");
        assertEquals("SIN_ORDEN", r.copia(), "premisa: la copia lo hereda");
        assertTrue(r.reducidasEnLaMedida() > 0, "premisa: la semana medida lleva colocaciones reducidas");
        assertEquals("NO_FACTIBLE", r.dolor(), r.toString());
        assertNull(r.publicado(), r.toString());
        assertEquals("[NO_FACTIBLE]", r.regeneracion(), "P-03 nivel 2 tampoco publica");
        assertFalse(r.excluidos().isEmpty(), "la exclusion por dolor sigue activa");
        assertEquals(0, r.trabajoTrasDolor(), "caso 8 (NO_FACTIBLE): nada de lo que dolio se hizo en la semana medida");
        var traza = jdbc.queryForMap("SELECT resulting_plan_id, adaptation_rejection, validation_problems FROM"
                + " safe_regeneration_attempts WHERE user_id = ?", r.u());
        assertNull(traza.get("resulting_plan_id"));
        assertTrue(String.valueOf(traza.get("adaptation_rejection")).contains("V24"), traza.toString());
        assertTrue(String.valueOf(traza.get("validation_problems")).contains("sube el trabajo"), traza.toString());
        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?", Long.class, r.u()));
        assertEquals(r.reducidasEnLaMedida(), colocaciones(r.medido()).stream()
                .filter(c -> c.series() == 2 && c.reps() == 10).count(), "la semana medida no se toca");
    }

    @Autowired main.web.services.fitsense.planning.application.internal.queryservices.PublishedPlanSafetyService publicados;
    @Autowired main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalCatalogService catalogo;

    /**
     * 9703 y 9710 reproducen HALL-GM-01 sobre el conjunto elegible de la PR #65, anterior a la contencion
     * INC-CAT-CONS-v1: con la contencion el participante recibe otros ejercicios y la premisa (que sustituto sale y con
     * que dosis) cambia. El mecanismo que prueban no depende del catalogo, asi que se ejecutan con la contencion
     * apagada; el equivalente con la contencion encendida es {@link #halGm01ConContencion()}.
     */
    <T> T sinContencion(java.util.function.Supplier<T> prueba) {
        Object destino = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(catalogo);
        org.springframework.test.util.ReflectionTestUtils.setField(destino, "contencionConservadora", false);
        try {
            return prueba.get();
        } finally {
            org.springframework.test.util.ReflectionTestUtils.setField(destino, "contencionConservadora", true);
        }
    }
    @Autowired org.springframework.transaction.PlatformTransactionManager transacciones;

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
        List<Long> conRepetido = List.of();
        for (long id = 9_750; id < 9_800 && plan < 0; id++) {
            long candidato = participante(id);
            long p = planning.generateWeeklyPlan(candidato, S1, PlanAdjustment.none(), null, null).orElseThrow();
            // Premisa: dos sesiones posteriores al lunes repiten algun ejercicio del lunes (una se empieza antes del
            // dolor y la otra sigue programada).
            var posteriores = jdbc.queryForList("SELECT wb.planned_workout_id FROM planned_workouts wb WHERE wb.plan_id = ?"
                    + " AND wb.scheduled_date > (SELECT min(scheduled_date) FROM planned_workouts WHERE plan_id = ?)"
                    + " AND EXISTS (SELECT 1 FROM planned_workout_exercises b JOIN planned_workout_exercises a ON"
                    + " a.exercise_id = b.exercise_id JOIN planned_workouts wa ON wa.planned_workout_id = a.planned_workout_id"
                    + " WHERE b.planned_workout_id = wb.planned_workout_id AND wa.plan_id = ? AND wa.scheduled_date ="
                    + " (SELECT min(scheduled_date) FROM planned_workouts WHERE plan_id = ?)) ORDER BY wb.scheduled_date",
                    Long.class, p, p, p, p);
            if (posteriores.size() >= 2) {
                u = candidato;
                plan = p;
                conRepetido = posteriores;
            }
        }
        assertTrue(plan > 0, "premisa: algun participante 9750-9799 repite ejercicios del lunes en dos sesiones posteriores");
        final long usuario = u, planId = plan;
        var antes = colocaciones(plan);
        RelojPruebas.abrirVentanaDeEjecucion(jdbc, u);
        var principal = UserDetailsImpl.build(usuarios.findById(u).orElseThrow());
        var ids = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ? ORDER BY"
                + " scheduled_date", Long.class, plan);
        final long enCurso = conRepetido.get(0), programada = conRepetido.get(1);

        // Caso 5: una sesion ya empezada cuando llega el dolor.
        long s = sesiones.handle(new StartWorkoutSessionCommand(u, enCurso)).orElseThrow().getId();
        planes.skip(principal, ids.get(0), new SkipWorkoutResource(SkipReason.PAIN_OR_DISCOMFORT));
        var excluidos = jdbc.queryForList("SELECT exercise_id FROM user_safety_exclusions WHERE user_id = ? AND status ="
                + " 'ACTIVA'", Long.class, u);
        assertFalse(excluidos.isEmpty(), "premisa: P-02 registra la exclusion");
        // O20: lo que P-DOLOR-PUB marca es la exclusion EFECTIVA: lo registrado y sus variantes equivalentes (aqui,
        // provisionales: dolor de sesion sin identificar).
        var efectivos = dolorEfectivo.excluidosActivos(u);
        assertTrue(efectivos.containsAll(excluidos), "la exclusion efectiva contiene lo registrado");
        String lista = "{" + String.join(",", efectivos.stream().map(String::valueOf).toList()) + "}";

        // Casos 1, 2 y 3: se marcan exactamente las prescripciones cuyo ejercicio dolio, con la causa del dolor, en las
        // sesiones abiertas y en la omitida (el reporte retroactivo tampoco puede anotar trabajo en lo que dolio);
        // ningun otro ejercicio (los que no se repiten, los demas de la misma sesion) se marca.
        var esperado = new java.util.TreeMap<Long, String>();
        jdbc.queryForList("SELECT pwe.planned_exercise_id FROM planned_workout_exercises pwe JOIN planned_workouts pw ON"
                + " pw.planned_workout_id = pwe.planned_workout_id WHERE pw.plan_id = ? AND pw.status IN ('SCHEDULED',"
                + " 'IN_PROGRESS', 'SKIPPED') AND pwe.exercise_id = ANY(?::bigint[])", Long.class, plan, lista)
                .forEach(pe -> esperado.put(pe, "dolor_registrado"));
        var marcado = new java.util.TreeMap<Long, String>();
        for (long w : ids) marcado.putAll(planning.fetchPlannedWorkout(w).orElseThrow().safetyConflicts());
        assertEquals(esperado, marcado, "solo lo que dolio, y solo en sesiones que admiten registros");

        var filas = jdbc.queryForList("SELECT planned_exercise_id, exercise_id, prescription_type, planned_sets,"
                + " planned_reps, planned_duration_seconds FROM planned_workout_exercises WHERE planned_workout_id = ?"
                + " ORDER BY exercise_order", enCurso);
        assertTrue(filas.size() > 1, "premisa: sesion con varios ejercicios");
        var vistaEnCurso = planning.fetchPlannedWorkout(enCurso).orElseThrow();
        assertEquals("IN_PROGRESS", vistaEnCurso.status());
        assertFalse(vistaEnCurso.blockedForSafety(), "caso 3: un ejercicio con dolor no bloquea la sesion entera");
        Long omitidoEnCurso = null;
        for (var f : filas) {
            long pe = ((Number) f.get("planned_exercise_id")).longValue();
            if (marcado.containsKey(pe)) {
                var error = assertThrows(main.web.services.fitsense.execution.domain.exceptions.WorkoutNotAvailableException.class,
                        () -> sesiones.handle(new RecordSessionExerciseCommand(usuario, s, pe, (short) 2, 20, null, null, null)));
                assertTrue(error.getMessage().contains("dolor_registrado"), error.getMessage());
                // Caso 10: el reintento del mismo registro con trabajo se rechaza igual; el de omitido se puede repetir.
                assertThrows(main.web.services.fitsense.execution.domain.exceptions.WorkoutNotAvailableException.class,
                        () -> sesiones.handle(new RecordSessionExerciseCommand(usuario, s, pe, (short) 2, 20, null, null, null)));
                for (int i = 0; i < 2; i++)
                    assertTrue(sesiones.handle(new RecordSessionExerciseCommand(u, s, pe, (short) 0, 0, null, null,
                            SkipReason.OTHER)).isPresent(), "se puede registrar como omitido");
                if (omitidoEnCurso == null) omitidoEnCurso = pe;
            } else {
                boolean duracion = "DURATION".equals(f.get("prescription_type"));
                int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
                int total = series * ((Number) f.get(duracion ? "planned_duration_seconds" : "planned_reps")).intValue();
                assertTrue(sesiones.handle(new RecordSessionExerciseCommand(u, s, pe, (short) series,
                        duracion ? null : total, duracion ? total : null, null, null)).isPresent(),
                        "caso 3: los demas ejercicios de la sesion se registran con normalidad");
            }
        }
        assertNotNull(omitidoEnCurso, "premisa: la sesion en curso repite un ejercicio del lunes");
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM workout_session_exercises WHERE session_id = ? AND"
                + " planned_exercise_id = ?", Integer.class, s, omitidoEnCurso), "caso 10: el reintento no duplica");
        sesiones.handle(new FinishWorkoutSessionCommand(u, s, (short) 6, (short) 4, (short) 45));

        // Caso 4: terminada, es historia: no se marca y lo registrado queda como se registro.
        var terminada = planning.fetchPlannedWorkout(enCurso).orElseThrow();
        assertFalse(java.util.Set.of("SCHEDULED", "IN_PROGRESS").contains(terminada.status()), terminada.status());
        assertEquals(Map.of(), terminada.safetyConflicts());
        assertEquals(Map.of("status", "SKIPPED", "skip_reason", "OTHER"), jdbc.queryForMap("SELECT status, skip_reason"
                + " FROM workout_session_exercises WHERE session_id = ? AND planned_exercise_id = ?", s, omitidoEnCurso));
        // V57 (propuesta): la fila deja constancia de que la omision la impuso el sistema, y las demas no.
        assertEquals("dolor_registrado", jdbc.queryForObject("SELECT system_block_cause FROM workout_session_exercises"
                + " WHERE session_id = ? AND planned_exercise_id = ?", String.class, s, omitidoEnCurso));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM workout_session_exercises WHERE session_id = ? AND"
                + " planned_exercise_id <> ? AND system_block_cause IS NOT NULL", Integer.class, s, omitidoEnCurso),
                "las prescripciones sin marca no llevan causa del sistema");
        // Trazabilidad con OTHER: mientras la exclusion esta ACTIVA el ejercicio solo admite el registro como omitido,
        // asi que toda omision suya posterior a la exclusion la impuso el sistema. Se reconstruye sin ambiguedad.
        assertEquals(List.of(omitidoEnCurso), jdbc.queryForList("SELECT wse.planned_exercise_id FROM"
                + " workout_session_exercises wse JOIN workout_sessions ws ON ws.session_id = wse.session_id JOIN"
                + " planned_workout_exercises pwe ON pwe.planned_exercise_id = wse.planned_exercise_id JOIN"
                + " user_safety_exclusions x ON x.user_id = ws.user_id AND x.exercise_id = pwe.exercise_id AND x.status ="
                + " 'ACTIVA' AND x.recorded_at <= wse.created_at WHERE ws.user_id = ? AND wse.status = 'SKIPPED'"
                + " ORDER BY 1", Long.class, u).stream().distinct().toList(), "omision impuesta por el dolor registrado");

        // Caso 6: sesion futura todavia programada: marcada, no bloqueada, aviso en la API, se puede empezar.
        Long marcadoFutura = null;
        for (var e : marcado.entrySet())
            if (jdbc.queryForObject("SELECT planned_workout_id FROM planned_workout_exercises WHERE planned_exercise_id = ?",
                    Long.class, e.getKey()) == programada) {
                marcadoFutura = e.getKey();
                break;
            }
        assertNotNull(marcadoFutura, "premisa: la sesion programada repite un ejercicio del lunes");
        var vista = planning.fetchPlannedWorkout(programada).orElseThrow();
        assertEquals("SCHEDULED", vista.status());
        assertEquals("dolor_registrado", vista.safetyConflicts().get(marcadoFutura), vista.safetyConflicts().toString());
        assertFalse(vista.blockedForSafety(), "solo por dolor la sesion no se bloquea entera");
        var recurso = new org.springframework.transaction.support.TransactionTemplate(transacciones).execute(tx ->
                planes.byId(principal, planId).getBody());
        var sesionApi = recurso.workouts().stream().filter(w -> w.plannedWorkoutId() == programada).findFirst()
                .orElseThrow();
        assertFalse(sesionApi.safetyBlocked(), "la app no debe mostrarla bloqueada");
        assertEquals(main.web.services.fitsense.planning.interfaces.rest.transform
                .WeeklyTrainingPlanResourceFromEntityAssembler.AVISO_DOLOR, sesionApi.safetyBlockMessage());
        long s2 = sesiones.handle(new StartWorkoutSessionCommand(u, programada)).orElseThrow().getId();
        final long pe = marcadoFutura;
        var error = assertThrows(main.web.services.fitsense.execution.domain.exceptions.WorkoutNotAvailableException.class,
                () -> sesiones.handle(new RecordSessionExerciseCommand(usuario, s2, pe, (short) 2, 20, null, null, null)));
        assertTrue(error.getMessage().contains("dolor_registrado"), error.getMessage());
        assertTrue(sesiones.handle(new RecordSessionExerciseCommand(u, s2, pe, (short) 0, 0, null, null,
                SkipReason.PAIN_OR_DISCOMFORT)).isPresent(), "se puede registrar como omitido");

        // Sin sustitutos, dosis ni sesiones nuevas; ninguna exclusion duplicada.
        assertEquals(antes, colocaciones(plan), "el plan publicado no cambia: sin sustitutos ni dosis nuevas");
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?", Long.class, u));
        assertEquals(excluidos, jdbc.queryForList("SELECT exercise_id FROM user_safety_exclusions WHERE user_id = ? AND"
                + " status = 'ACTIVA'", Long.class, u), "caso 10: leer y reintentar no anade ni quita exclusiones");
    }

    private Recorrido comprobar(Recorrido r) {
        assertEquals("APLICADA", r.fatiga(), "premisa: el mantenimiento se aplico");
        assertEquals("SIN_ORDEN", r.copia(), "premisa: la copia lo hereda");
        assertTrue(r.reducidasEnLaMedida() > 0, "premisa: la semana medida lleva colocaciones reducidas");
        assertNotNull(r.publicado(), "la semana con dolor se publica: " + r);
        assertFalse(r.excluidos().isEmpty(), "el dolor deja exclusiones activas");
        assertEquals(0, r.trabajoTrasDolor(), "caso 7: nada de lo que dolio se hizo despues en la semana medida");
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
        var salida = new StringBuilder("user_id;fatiga;copia;dolor;publicado;regeneracion;reducidas_medida;reaparecen;aumentos;trabajo_tras_dolor;detalle\n");
        int desde = Integer.getInteger("fitsense.colocacion.desde", 9_801), hasta = Integer.getInteger("fitsense.colocacion.hasta", 9_840);
        for (long id = desde; id <= hasta; id++) {
            var r = recorrer(id);
            salida.append(id).append(';').append(r.fatiga()).append(';').append(r.copia()).append(';').append(r.dolor())
                    .append(';').append(r.publicado() != null).append(';').append(r.regeneracion()).append(';')
                    .append(r.reducidasEnLaMedida()).append(';').append(r.reaparecen()).append(';')
                    .append(r.comparacion() == null ? "-" : r.comparacion().aumentos().size()).append(';')
                    .append(r.trabajoTrasDolor()).append(';')
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
