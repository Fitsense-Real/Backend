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
        // Valores historicos fijados explicitamente al activar la configuracion evaluada (11/11/2026): esta
        // prueba se escribio sobre los valores por defecto anteriores y comprueba ese comportamiento.
        "fitsense.planning.policy=PLAN-HIST-1",
        "fitsense.ai.adaptation.prompt-version=v1",
        "fitsense.adaptation.difficulty-guard=off",
        "fitsense.adaptation.pain-localized-removal=off",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.progression.enabled=false"
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
    @Autowired main.web.services.fitsense.planning.domain.services.PlanDraftValidator validador;
    @Autowired main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService planComandos;

    /**
     * Semana y participantes FIJOS (orden de 14/11/2026 sobre las regresiones de V52.1). El motor de reglas siembra con
     * {@code userId x 1.000.003 + weekStartDate}: con la semana calculada desde hoy y {@code user_id} autoincremental,
     * el escenario dependia del dia de ejecucion y del orden de los metodos de la clase. Se usa la semana de referencia
     * de las pruebas ({@link main.web.services.fitsense.RelojPruebas}) y cada caso crea a sus participantes con un
     * {@code user_id} explicito.
     */
    static final LocalDate S1 = main.web.services.fitsense.RelojPruebas.SEMANA_REFERENCIA;
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
        recorrer(Modo.SUBE, "RESPALDO", 8_101, 8_102, 8_103, 8_104, 8_105, 8_106, 8_107, 8_108, 8_109, 8_110);
    }

    /**
     * La IA devuelve el viernes identico. Antes de C-2 (orden de cierre del PR #16), como SENALADA esa semana
     * no tenia la exencion de D8: V20 rechazaba la propuesta tres veces y el respaldo publicaba la misma sesion
     * (RESPALDO). Con C-2 la propuesta identica hereda D8 y se publica en la primera llamada: SIN_EFECTO.
     */
    @Test
    @DisplayName("ACORTAR -> copia -> fatiga con la IA devolviendolo igual (C-2: SIN_EFECTO) -> la copia conserva la exencion")
    void propuestaIdenticaConservaD8() throws Exception {
        recorrer(Modo.IGUAL, "SIN_EFECTO", 8_201, 8_202, 8_203, 8_204, 8_205, 8_206, 8_207, 8_208, 8_209, 8_210);
    }

    @Test
    @DisplayName("el dolor no aprovecha D8: el viernes acortado que dolio, conservado tal cual, no se publica")
    void dolorNoAprovechaD8() throws Exception {
        modo = Modo.IGUAL;
        // Participante FIJO (9001): con la semana de referencia, P-03 nivel 2 subiria el trabajo antes y despues de
        // V52.1 (456 -> 490 y 456 -> 496). Exploracion: -Dfitsense.d8.exploracion=true -Dfitsense.d8.desde=9001.
        var a = acortado(9_001);
        long u = a.u();
        long plan = a.plan();
        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        plan = ciclo.runFor(u, S1.plusWeeks(1), S1.plusWeeks(2)).orElseThrow();   // copia exenta
        long medido = plan;
        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.DOLOR);
        var r = ciclo.runFor(u, S1.plusWeeks(2), S1.plusWeeks(3));
        // P-03: la IA (que devuelve el viernes igual) se rechaza tres veces por el dolor. El nivel 1 (sustitucion)
        // no pasa V20 en el viernes acortado. El nivel 2 tampoco publica: regenerar con dosis base devolveria el
        // trabajo que ACORTAR retiro (450 -> 486 repeticiones) en plena semana de dolor, y ante dolor no se anade
        // trabajo (auditoria del nivel 2, orden de cierre del PR #16). Queda NO_FACTIBLE, con su motivo.
        assertTrue(r.isEmpty(), "volver a publicar el viernes que dolio no es una solucion segura");
        assertEquals("NO_FACTIBLE", ultimo(u).get("status"));
        assertTrue(String.valueOf(jdbc.queryForObject("SELECT validation_problems FROM safe_regeneration_attempts WHERE"
                + " user_id = ? AND outcome = 'NO_FACTIBLE'", String.class, u)).contains("sube el trabajo"));
        assertTrue(jdbc.queryForObject("SELECT count(*) FROM user_safety_exclusions WHERE user_id = ? AND status ="
                + " 'ACTIVA'", Integer.class, u) > 0, "la exclusion por dolor se conserva");
        assertSinViolaciones();
    }

    /**
     * El otro desenlace legitimo de P-03 nivel 2 ante el mismo dolor (orden de 14/11/2026). Con el participante 9004
     * la semana regenerada con el motor de reglas NO sube el trabajo de la semana medida, y P-03 la publica; ocurre
     * igual antes de V52.1 (486 -> 476 repeticiones) que despues (476 -> 476). Se comprueba, de forma independiente
     * del servicio, todo lo que P-03 promete: la IA no publico; publica solo P-03 nivel 2; ningun ejercicio excluido
     * por dolor reaparece (el viernes que dolio no vuelve); no hay mas trabajo ni mas dosis por ejercicio que en la
     * semana medida; la semana valida entera con el validador SIN ninguna autorizacion (todas las dosis son BASE: sin
     * progresion, recuperacion, reincorporacion ni herencia) y sin la exencion de V20 de D8.
     */
    @Test
    @DisplayName("el dolor no aprovecha D8: si P-03 regenera, la semana nueva no trae lo que dolio ni mas trabajo, y valida sin exenciones")
    void dolorConRegeneracionSegura() throws Exception {
        modo = Modo.IGUAL;
        var a = acortado(9_004);
        long u = a.u();
        long plan = a.plan();
        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        plan = ciclo.runFor(u, S1.plusWeeks(1), S1.plusWeeks(2)).orElseThrow();   // copia exenta
        long medido = plan;
        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.DOLOR);
        var r = ciclo.runFor(u, S1.plusWeeks(2), S1.plusWeeks(3));

        assertTrue(r.isPresent(), "premisa del caso: P-03 nivel 2 publica para el participante 9004");
        long regenerado = r.get();
        // 1. La IA no publico (su intento sigue NO_FACTIBLE); publica SOLO P-03 nivel 2, con su fila.
        assertEquals("NO_FACTIBLE", ultimo(u).get("status"));
        var excluidos = jdbc.queryForList("SELECT exercise_id FROM user_safety_exclusions WHERE user_id = ? AND status ="
                + " 'ACTIVA'", Long.class, u);
        assertFalse(excluidos.isEmpty(), "la exclusion por dolor se conserva");
        var fila = jdbc.queryForMap("SELECT outcome, resulting_plan_id, measured_plan_id FROM safe_regeneration_attempts"
                + " WHERE user_id = ?", u);
        assertEquals("PUBLICADO", fila.get("outcome"));
        assertEquals(regenerado, ((Number) fila.get("resulting_plan_id")).longValue());
        assertEquals(medido, ((Number) fila.get("measured_plan_id")).longValue());
        // 2. Nada de lo excluido por dolor reaparece: el viernes que dolio no vuelve.
        var publicados = jdbc.queryForList("SELECT DISTINCT pwe.exercise_id FROM planned_workouts pw JOIN"
                + " planned_workout_exercises pwe ON pwe.planned_workout_id = pw.planned_workout_id WHERE pw.plan_id = ?",
                Long.class, regenerado);
        assertEquals(List.of(), publicados.stream().filter(excluidos::contains).toList(), "reaparece lo que dolio");
        // 3. Ante dolor no se anade trabajo: ni en total (P-03) ni en la dosis de ningun ejercicio comun (V8C).
        var antes = trabajo(medido).split(":");
        var despues = trabajo(regenerado).split(":");
        assertTrue(Long.parseLong(despues[0]) <= Long.parseLong(antes[0])
                        && Long.parseLong(despues[1]) <= Long.parseLong(antes[1]),
                "mas trabajo que la semana medida: " + trabajo(medido) + " -> " + trabajo(regenerado));
        assertEquals(List.of(), jdbc.queryForList("SELECT n.exercise_id FROM (" + DOSIS + ") n JOIN (" + DOSIS + ") m"
                + " ON m.exercise_id = n.exercise_id WHERE n.series > m.series OR n.reps > m.reps OR n.segundos > m.segundos",
                Long.class, regenerado, medido), "un ejercicio comun sube su dosis");
        // 4. La semana valida entera SIN autorizaciones: dosis BASE (sin progresion, recuperacion, reincorporacion ni
        //    herencia) y V20 sin la exencion de D8 (ninguna fecha reducible).
        var ctx = planComandos.prepare(new main.web.services.fitsense.planning.domain.model.commands
                .GenerateWeeklyPlanCommand(u, S1.plusWeeks(3), null, true, null, null)).context();
        var independiente = new ValidacionIndependiente(jdbc).validar(regenerado, ctx, validador, 30);
        assertEquals("VALIDA", independiente.estado(), independiente.texto());
        assertEquals(java.util.Set.of("BASE"), independiente.origenes().keySet(), independiente.texto());
        assertEquals(0, independiente.fechasReducibles());
        assertTrue(viernes(regenerado) >= PISO, "el viernes regenerado no es el acortado exento: " + viernes(regenerado));
        assertSinViolaciones();
    }

    /** Dosis por ejercicio de un plan (la mayor si se repite): series, repeticiones y segundos. */
    static final String DOSIS = "SELECT pwe.exercise_id, max(coalesce(pwe.planned_sets, 1)) AS series,"
            + " max(coalesce(pwe.planned_reps, 0)) AS reps, max(coalesce(pwe.planned_duration_seconds, 0)) AS segundos"
            + " FROM planned_workouts pw JOIN planned_workout_exercises pwe ON pwe.planned_workout_id = pw.planned_workout_id"
            + " WHERE pw.plan_id = ? GROUP BY pwe.exercise_id";

    /**
     * EXPLORACION (opt-in, -Dfitsense.d8.exploracion=true): para cada user_id de un rango, el desenlace del caso de dolor
     * y el conjunto elegible del perfil. Sirve para elegir de forma determinista los participantes de cada caso y para
     * comparar el catalogo antes y despues de V52.1. No afirma nada.
     */
    @Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "fitsense.d8.exploracion", matches = "true")
    void exploracionDolor() throws Exception {
        var salida = new StringBuilder("user_id;premisa;viernes;desenlace;regeneracion;trabajo_medido;trabajo_regenerado;"
                + "excluidos;reaparecen;validacion\n");
        // Por defecto un rango que no choca con los participantes fijos de las pruebas; el analisis de la orden de
        // 14/11/2026 se hizo con -Dfitsense.d8.desde=9001 -Dfitsense.d8.hasta=9040, ejecutando SOLO este metodo.
        int desde = Integer.getInteger("fitsense.d8.desde", 9_501), hasta = Integer.getInteger("fitsense.d8.hasta", 9_540);
        for (long id = desde; id <= hasta; id++) {
            modo = Modo.IGUAL;
            long u = participante(id);
            long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
            ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.TIEMPO);
            var r0 = ciclo.runFor(u, S1, S1.plusWeeks(1));
            if (r0.isEmpty() || viernes(r0.get()) >= PISO) {
                salida.append(id).append(";NO;").append(r0.map(this::viernes).orElse(-1)).append(";;;;;;;\n");
                continue;
            }
            plan = r0.get();
            ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.HECHO);
            plan = ciclo.runFor(u, S1.plusWeeks(1), S1.plusWeeks(2)).orElseThrow();
            long medido = plan;
            ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.DOLOR);
            var r = ciclo.runFor(u, S1.plusWeeks(2), S1.plusWeeks(3));
            var reg = jdbc.queryForList("SELECT outcome, validation_problems FROM safe_regeneration_attempts WHERE user_id = ?", u);
            var excluidos = jdbc.queryForList("SELECT exercise_id FROM user_safety_exclusions WHERE user_id = ? AND status = 'ACTIVA'", Long.class, u);
            String reaparecen = "", tReg = "", validacion = "";
            if (r.isPresent()) {
                var ej = jdbc.queryForList("SELECT DISTINCT pwe.exercise_id FROM planned_workouts pw JOIN planned_workout_exercises pwe"
                        + " ON pwe.planned_workout_id = pw.planned_workout_id WHERE pw.plan_id = ?", Long.class, r.get());
                reaparecen = ej.stream().filter(excluidos::contains).toList().toString();
                tReg = trabajo(r.get());
                var ctx = planComandos.prepare(new main.web.services.fitsense.planning.domain.model.commands
                        .GenerateWeeklyPlanCommand(u, S1.plusWeeks(3), null, true, null, null)).context();
                validacion = new ValidacionIndependiente(jdbc).validar(r.get(), ctx, validador, 30).texto();
            }
            salida.append(id).append(";SI;").append(viernes(medido)).append(';').append(r.isPresent() ? "PUBLICADO" : "NO_PUBLICADO")
                    .append(';').append(reg.isEmpty() ? "-" : reg.get(0).get("outcome") + " " + String.valueOf(reg.get(0).get("validation_problems")).replace(';', ','))
                    .append(';').append(trabajo(medido)).append(';').append(tReg).append(';').append(excluidos).append(';')
                    .append(reaparecen).append(';').append(validacion.replace(';', ',')).append('\n');
        }
        // Conjunto elegible del perfil SIN historial (sin exclusiones por dolor ni ejercicios recientes): participante nuevo.
        long limpio = participante(9_999);
        var elegibles = planComandos.prepare(new main.web.services.fitsense.planning.domain.model.commands
                .GenerateWeeklyPlanCommand(limpio, S1, null, true, null, null)).context().availableExercises().stream()
                .map(c -> c.exerciseId() + ":" + c.movementPattern() + ":" + c.bodyPartCode()).sorted().toList();
        var f = Path.of(System.getProperty("fitsense.d8.salida", "target/d8_exploracion"));
        Files.createDirectories(f);
        Files.writeString(f.resolve("desenlaces.csv"), salida.toString());
        Files.writeString(f.resolve("elegibles.txt"), String.join("\n", elegibles));
    }

    /** Repeticiones y segundos totales planificados (series x repeticiones, series x segundos), como P-03 nivel 2. */
    private String trabajo(long plan) {
        return jdbc.queryForObject("SELECT coalesce(sum(coalesce(pwe.planned_sets, 1) * coalesce(pwe.planned_reps, 0)), 0)"
                + " || ':' || coalesce(sum(coalesce(pwe.planned_sets, 1) * coalesce(pwe.planned_duration_seconds, 0)), 0)"
                + " FROM planned_workouts pw JOIN planned_workout_exercises pwe ON pwe.planned_workout_id = pw.planned_workout_id"
                + " WHERE pw.plan_id = ?", String.class, plan);
    }

    private void recorrer(Modo enLaFatiga, String estadoFatiga, long... candidatos) throws Exception {
        var a = acortado(candidatos);
        long u = a.u();
        long plan = a.plan();
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
        // Campana p123: el instrumento independiente tambien sigue el linaje por una senalada SIN EFECTO.
        long publicado = plan;
        var ctx = planComandos.prepare(new main.web.services.fitsense.planning.domain.model.commands
                .GenerateWeeklyPlanCommand(u, S1.plusWeeks(4), null, true, null, null)).context();
        var independiente = new ValidacionIndependiente(jdbc).validar(publicado, ctx, validador, 30);
        assertNotEquals("INVALIDA", independiente.estado(), independiente.texto());

        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.HECHO);                       // s6: y sigue
        plan = publicar(u, 4, "SIN_ORDEN");
        assertEquals(minutos, viernes(plan));
        assertSinViolaciones();
    }

    record Acortado(long u, long plan) {}

    /**
     * s2: el viernes omitido por falta de tiempo; ACORTAR lo deja bajo el piso de V20.
     * La premisa depende de la semana generada: si el viernes no tiene rellenos
     * suficientes para bajar del piso, el escenario no se da con ese participante y
     * se prueba con otro (busqueda de precondicion, como en CatalogoV36IT).
     */
    private Acortado acortado(long... candidatos) {
        var vistos = new ArrayList<Integer>();
        for (long candidato : candidatos) {
            long u = participante(candidato);
            long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
            ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.TIEMPO);
            long nuevo = publicar(u, 0, "APLICADA");
            if (viernes(nuevo) < PISO) return new Acortado(u, nuevo);
            vistos.add(viernes(nuevo));
        }
        return fail("premisa: ningun viernes acortado bajo el piso " + PISO + " en " + Arrays.toString(candidatos)
                + ": " + vistos);
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

    /** Participante con {@code user_id} explicito: la semilla del motor de reglas no depende del orden de la clase. */
    private long participante(long u) {
        String email = "herencia-d8-" + u + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (user_id, email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " OVERRIDING SYSTEM VALUE VALUES (?, ?, 'x', 'Prueba', now(), now())", u, email);
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
        main.web.services.fitsense.RelojPruebas.abrirVentanaDeEjecucion(jdbc, u);
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
                case TIEMPO -> saltoPorFaltaDeTiempo(principal, w);
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
