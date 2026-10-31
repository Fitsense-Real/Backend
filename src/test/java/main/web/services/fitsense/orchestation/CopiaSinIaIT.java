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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
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
 * COPIA SEMANAL Y MANTENIMIENTO SIN IA: cuenta las llamadas a la IA de
 * correccion ({@code ReplicateSessionFixGenerator.pedirRespuesta}, la unica que
 * hace el ciclo semanal) y falla si hay alguna donde esta prohibida.
 * <p>
 * Ciclo real ({@link WeeklyCycleService#runFor}) sobre PostgreSQL efimero, con
 * la progresion encendida SOLO aqui. La IA esta simulada: nunca se llama a
 * Replicate. La generacion inicial de cada participante no cuenta (es la
 * generacion inicial, con su propio camino).
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.progression.enabled=true"
})
@DisplayName("Copia semanal y mantenimiento sin IA: llamadas a la IA por escenario")
class CopiaSinIaIT {

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
    static final List<String> GIMNASIO = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine",
            "bench", "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine");

    enum Dia { HECHO, FATIGA, TIEMPO, PARCIAL_TIEMPO, HECHO_CON_DOLOR, NADA }

    record Ej(long id, String tipo, Short series, Short reps, Integer segundos, Short descanso) {}

    @BeforeEach
    void iaSimulada() {
        reset(ia);
        when(ia.isEnabled()).thenReturn(true);
        when(ia.modelName()).thenReturn("simulado");
        when(ia.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{}", "salida-simulada"));
        when(ia.interpretar(anyString(), any())).thenAnswer(inv -> corregir(inv.getArgument(1)));
    }

    // =======================================================================
    // Escenarios
    // =======================================================================

    @Test
    @DisplayName("1, 2, 8, 9: copia y progresion con 0 llamadas; repetir el ciclo no duplica; la copia conserva todo")
    void copiaYProgresion() throws Exception {
        long u = participante(GoalType.GENERAL_FITNESS, FitnessLevel.INTERMEDIATE, 45);
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();

        // 1 · Semana conservada (una sola semana cumplida: no hay paso posible).
        ejecutar(u, w1, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        var historialW1 = historial(w1);
        long w2 = ciclo(u, 0, 0);
        assertEquals("NO_CHANGE", ultimo(u).get("decision"));
        assertEquals("CONSERVED", ultimo(u).get("generation_source"));
        assertEquals("{}", ultimo(u).get("progressed_sessions"));

        // 9 · La copia: mismas dosis y estructura, fechas +7, sesiones nuevas sin registros,
        //     linaje V31, y el historial de la semana medida intacto.
        var a = semana(w1);
        var b = semana(w2);
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) {
            assertEquals(a.get(i).getValue(), b.get(i).getValue(), "mismos ejercicios y dosis, mismo orden");
            assertNotEquals(a.get(i).getKey(), b.get(i).getKey(), "sesion nueva, identificador nuevo");
            assertEquals(a.get(i).getKey(), origen(b.get(i).getKey()), "V31");
        }
        assertEquals(estructura(w1, 7), estructura(w2, 0), "fecha +7, mismo enfoque y minutos");
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM workout_sessions ws JOIN planned_workouts pw"
                + " ON pw.planned_workout_id = ws.planned_workout_id WHERE pw.plan_id = ?", Integer.class, w2),
                "la semana nueva empieza sin registros: mide su propia adherencia");
        assertEquals(List.of("SCHEDULED"), jdbc.queryForList("SELECT DISTINCT status FROM planned_workouts"
                + " WHERE plan_id = ?", String.class, w2));
        assertEquals(historialW1, historial(w1), "el cumplimiento de la semana medida no cambia");

        // 8 · Repetir el mismo ciclo: el mismo plan, sin semana ni evidencia nueva, 0 llamadas.
        int intentos = intentos(u);
        clearInvocations(ia);
        assertEquals(w2, ciclo.runFor(u, S1, S1.plusWeeks(1)).orElseThrow());
        verify(ia, never()).pedirRespuesta(any(), any(), any(), any(), anyInt());
        assertEquals(intentos, intentos(u), "no hay un segundo intento V32 para la misma decision");
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?"
                + " AND week_start_date = ? AND status <> 'REPLACED'", Integer.class, u, S1.plusWeeks(1)));

        // 2 · Progresion autorizada.
        ejecutar(u, w2, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        ciclo(u, 1, 0);
        assertTrue(informe(ultimo(u)).contains("\"tipo\":\"PROGRESION\""), String.valueOf(ultimo(u)));
        assertNotEquals("{}", ultimo(u).get("progressed_sessions"));
        assertSinViolaciones();
    }

    @Test
    @DisplayName("6: datos insuficientes -> se conserva sin cambios y sin IA")
    void datosInsuficientes() throws Exception {
        long u = participante(GoalType.LOSE_WEIGHT, FitnessLevel.INTERMEDIATE, 45);
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        ejecutar(u, w1, Dia.HECHO, Dia.NADA, Dia.NADA);
        long w2 = ciclo(u, 0, 0);
        assertEquals("INSUFFICIENT_DATA", ultimo(u).get("decision"), String.valueOf(ultimo(u)));
        assertEquals("SIN_ORDEN", ultimo(u).get("status"));
        assertEquals("{}", ultimo(u).get("progressed_sessions"));
        var a = semana(w1);
        var b = semana(w2);
        for (int i = 0; i < a.size(); i++) assertEquals(a.get(i).getValue(), b.get(i).getValue());
        assertSinViolaciones();
    }

    @Test
    @DisplayName("7 y 3: la fatiga usa el flujo de IA (1 llamada); la copia y la recuperacion, 0")
    void adaptacionYRecuperacion() throws Exception {
        long u = participante(GoalType.GENERAL_FITNESS, FitnessLevel.INTERMEDIATE, 45);
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        ejecutar(u, w1, Dia.HECHO, Dia.HECHO, Dia.FATIGA);
        long w2 = ciclo(u, 0, 1);   // 7 · adaptacion: una llamada, la propuesta vale a la primera
        assertEquals("APLICADA", ultimo(u).get("status"));
        assertEquals(1, ((Number) ultimo(u).get("ai_calls")).intValue(), "V32 registra la llamada");

        // Repetir el ciclo de la adaptacion tampoco vuelve a llamar.
        clearInvocations(ia);
        assertEquals(w2, ciclo.runFor(u, S1, S1.plusWeeks(1)).orElseThrow());
        verify(ia, never()).pedirRespuesta(any(), any(), any(), any(), anyInt());

        ejecutar(u, w2, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        long w3 = ciclo(u, 1, 0);   // copia de la dosis reducida
        ejecutar(u, w3, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        ciclo(u, 2, 0);             // 3 · recuperacion
        assertTrue(informe(ultimo(u)).contains("\"tipo\":\"RECUPERACION\""), String.valueOf(ultimo(u)));
        assertSinViolaciones();
    }

    @Test
    @DisplayName("4 y 5: mantenimiento, progresion y reincorporacion despues de acortar, siempre con 0 llamadas")
    void reincorporacionYMantenimiento() throws Exception {
        // LOSE_WEIGHT intermedio: tras acortar, el viernes se mantiene, progresa y, sin margen en la
        // reserva, reincorpora el relleno (decision 1(a): primero un paso). Hace falta que +1
        // repeticion quepa en el 5 % (progression_increase_pct) del viernes acortado. A 45 min
        // (dos 3x6 y cardio) es 3/48 = 6,25 % y la sesion se mantiene siempre; hasta V40 cabia
        // porque 1225 (abdominal clasificado como fuerza de cadera, 3x10) ocupaba el viernes.
        // Corregido el dato, el escenario es el de 60 min (3/60 = 5 %). La regla y las aserciones
        // no cambian.
        // Orden de cierre del MVP (31/10/2026): el acortamiento viene de una sesion PARCIAL por falta de
        // tiempo, sin M. Con una omision con M declarada el diseno (§5, fila 5) no deja reincorporar sin
        // una M nueva mayor; eso lo prueba conMNoSeReincorpora. Antes este escenario usaba la omision con
        // M y pasaba porque la comprobacion de M no existia.
        long u = participante(GoalType.LOSE_WEIGHT, FitnessLevel.INTERMEDIATE, 60);
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.PARCIAL_TIEMPO);
        plan = ciclo(u, 0, 1);      // acortar: flujo de IA, 1 llamada
        assertEquals("{ACORTAR_O_REORGANIZAR}", ultimo(u).get("targeted_interventions"));
        assertEquals("APLICADA", ultimo(u).get("status"), String.valueOf(ultimo(u)));

        boolean reincorporado = false, mantenimiento = false;
        int pasosViernes = 0;
        var traza = new ArrayList<String>();
        for (int k = 1; k <= 12 && !reincorporado; k++) {
            ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.HECHO);
            var antes = semana(plan).stream().map(Map.Entry::getValue).toList();
            plan = ciclo(u, k, 0);  // 0 llamadas en todos los ciclos deterministas
            var ev = ultimo(u);
            assertEquals("SIN_ORDEN", ev.get("status"), String.valueOf(ev));
            String tipo = informe(ev).contains("\"tipo\":\"REINCORPORACION\"") ? "REINCORPORACION"
                    : "{}".equals(ev.get("progressed_sessions")) ? "SIN_PASO" : "PASO";
            if (tipo.equals("REINCORPORACION")) {
                reincorporado = true;
                assertTrue(pasosViernes > 0, "1(a): reincorpora solo despues de al menos una progresion: " + traza);
            }
            if (tipo.equals("PASO") && informe(ev).contains("\"tipo\":\"PROGRESION\"")
                    && informe(ev).contains("\"estado_reserva\":\"PENDIENTE\"")) pasosViernes++;
            if (tipo.equals("SIN_PASO")) {
                mantenimiento = true;   // 5 · se mantiene: misma rutina, nada forzado
                assertEquals(antes, semana(plan).stream().map(Map.Entry::getValue).toList());
            }
            traza.add("s" + (k + 2) + " " + tipo);
        }
        System.out.println("SIN_IA reincorporacion/mantenimiento " + traza);
        assertTrue(mantenimiento, "tenia que haber al menos un ciclo de mantenimiento: " + traza);
        assertTrue(reincorporado, "tenia que reincorporarse el relleno: " + traza);
        assertSinViolaciones();
    }

    @Test
    @DisplayName("§5 fila 5: acortada con M declarada, el retirado NO vuelve sin una M nueva; la reserva se libera")
    void conMNoSeReincorpora() throws Exception {
        long u = participante(GoalType.LOSE_WEIGHT, FitnessLevel.INTERMEDIATE, 60);
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.TIEMPO);   // omision con M = minutos - 1
        long m = jdbc.queryForObject("SELECT declared_available_minutes FROM planned_workouts WHERE plan_id = ?"
                + " AND declared_available_minutes IS NOT NULL", Long.class, plan);
        plan = ciclo(u, 0, 1);
        assertEquals("APLICADA", ultimo(u).get("status"), String.valueOf(ultimo(u)));
        var traza = new ArrayList<String>();
        boolean liberada = false;
        for (int k = 1; k <= 12; k++) {
            ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.HECHO);
            plan = ciclo(u, k, 0);
            var inf = informe(ultimo(u));
            assertFalse(inf.contains("\"tipo\":\"REINCORPORACION\""), "con M = " + m + " no se reincorpora: " + inf);
            if (inf.contains("\"estado_reserva\":\"EN_ESPERA_DE_TIEMPO\"")) liberada = true;
            traza.add("s" + (k + 2) + " " + ultimo(u).get("progressed_sessions"));
        }
        System.out.println("SIN_IA con M " + m + ": " + traza);
        assertTrue(liberada, "la progresion de los conservados sigue con la reserva liberada: " + traza);
        assertSinViolaciones();
    }

    // =======================================================================
    // Cuando NO se reincorpora (orden de cierre del MVP, §6). Control positivo: reincorporacionYMantenimiento,
    // mismo perfil y mismo acortamiento, que reincorpora hacia la semana 11 si nada lo impide.
    // =======================================================================

    @Test
    @DisplayName("§6 sin retirada previa: doce semanas cumplidas, progresion y mantenimiento, ninguna reincorporacion")
    void sinRetiradaNoSeReincorpora() throws Exception {
        long u = participante(GoalType.LOSE_WEIGHT, FitnessLevel.INTERMEDIATE, 60);
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        sinReincorporar(u, plan, 0, 12, Dia.HECHO, Dia.HECHO, Dia.HECHO);
    }

    @ParameterizedTest(name = "§6 tras la retirada, cada semana {0} {1} {2}: no se reincorpora")
    @CsvSource({"HECHO, FATIGA, HECHO", "HECHO, NADA, HECHO", "HECHO, HECHO, FATIGA"})
    @DisplayName("§6 adherencia insuficiente, semana sin registro y fatiga en la sesion: se mantiene")
    void incumplimientoNoReincorpora(Dia lunes, Dia miercoles, Dia viernes) throws Exception {
        var r = acortarConRetirada();
        sinReincorporar(r[0], r[1], 1, 12, lunes, miercoles, viernes);
    }

    @Test
    @DisplayName("§6 el ejercicio retirado por DOLOR nunca vuelve solo: doce semanas cumplidas sin reincorporarlo")
    void retiradoPorDolorNoVuelve() throws Exception {
        long u = participante(GoalType.LOSE_WEIGHT, FitnessLevel.INTERMEDIATE, 60);
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var viernes = semana(plan).get(2).getValue();
        long dolio = viernes.get(viernes.size() - 1).id();
        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.HECHO_CON_DOLOR);
        long siguiente = ciclo.runFor(u, S1, S1.plusWeeks(1)).orElseThrow();
        assertEquals("{SEGURIDAD_ANTE_DOLOR}", ultimo(u).get("targeted_interventions"), String.valueOf(ultimo(u)));
        assertEquals("APLICADA", ultimo(u).get("status"), String.valueOf(ultimo(u)));
        assertTrue(semana(siguiente).get(2).getValue().stream().noneMatch(e -> e.id() == dolio), "el que dolio no vuelve");
        sinReincorporar(u, siguiente, 1, 12, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        assertTrue(semana(ultimoPlan).get(2).getValue().stream().noneMatch(e -> e.id() == dolio),
                "doce semanas despues sigue fuera");
    }

    @Test
    @DisplayName("§6 el retirado deja de ser elegible (catalogo): la reserva se descarta y no vuelve")
    void retiradoNoElegibleNoVuelve() throws Exception {
        var r = acortarConRetirada();
        // El perfil esta congelado durante el estudio (equipamiento y restricciones no cambian): lo unico que
        // puede volver inelegible un ejercicio es el catalogo, como la desactivacion de 776 en V37.
        assertEquals(1, jdbc.update("UPDATE exercises SET is_active = false WHERE exercise_id = ?", r[2]));
        var informes = sinReincorporar(r[0], r[1], 1, 12, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        assertTrue(informes.stream().anyMatch(i -> i.contains("\"estado_reserva\":\"DESCARTADA\"")),
                "la reserva queda DESCARTADA y los conservados progresan hasta el techo: " + informes);
    }

    /** El acortamiento del control positivo: {usuario, plan publicado, ejercicio retirado}. */
    private long[] acortarConRetirada() {
        long u = participante(GoalType.LOSE_WEIGHT, FitnessLevel.INTERMEDIATE, 60);
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var antes = semana(w1).get(2).getValue();
        ejecutar(u, w1, Dia.HECHO, Dia.HECHO, Dia.PARCIAL_TIEMPO);
        long w2 = ciclo(u, 0, 1);
        assertEquals("APLICADA", ultimo(u).get("status"), String.valueOf(ultimo(u)));
        var despues = semana(w2).get(2).getValue();
        var retirado = antes.stream().filter(a -> despues.stream().noneMatch(d -> d.id() == a.id())).toList();
        assertEquals(1, retirado.size(), "premisa: una retirada");
        return new long[]{u, w2, retirado.get(0).id()};
    }

    private long ultimoPlan;

    /** n ciclos con esos dias; ninguno puede reincorporar. Devuelve los informes V32. */
    private List<String> sinReincorporar(long u, long plan, int desde, int n, Dia... dias) throws Exception {
        var informes = new ArrayList<String>();
        for (int k = desde; k < desde + n; k++) {
            ejecutar(u, plan, dias);
            var p = ciclo.runFor(u, S1.plusWeeks(k), S1.plusWeeks(k + 1));
            var inf = informe(ultimo(u));
            informes.add(inf);
            assertFalse(inf.contains("\"tipo\":\"REINCORPORACION\""), "semana " + (k + 2) + ": " + inf);
            if (p.isEmpty()) break;   // sin semana publicada (p. ej. sin datos): nada que reincorporar
            plan = p.get();
        }
        ultimoPlan = plan;
        assertSinViolaciones();
        return informes;
    }

    /** Todo completo salvo el ultimo ejercicio, dejado a medias por dolor. */
    private void conDolorEnElUltimo(long u, long w) {
        long s = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
        var filas = jdbc.queryForList("SELECT planned_exercise_id, prescription_type, planned_sets, planned_reps,"
                + " planned_duration_seconds FROM planned_workout_exercises WHERE planned_workout_id = ?"
                + " ORDER BY exercise_order", w);
        for (int k = 0; k < filas.size(); k++) {
            var f = filas.get(k);
            boolean duracion = "DURATION".equals(f.get("prescription_type"));
            int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
            int total = series * ((Number) f.get(duracion ? "planned_duration_seconds" : "planned_reps")).intValue();
            boolean dolor = k == filas.size() - 1;
            if (dolor) total = total / 2;
            sesiones.handle(new RecordSessionExerciseCommand(u, s, ((Number) f.get("planned_exercise_id")).longValue(),
                    (short) series, duracion ? null : total, duracion ? total : null, null,
                    dolor ? SkipReason.PAIN_OR_DISCOMFORT : null));
        }
        sesiones.handle(new FinishWorkoutSessionCommand(u, s, (short) 6, (short) 4, (short) 50));
    }

    @Test
    @DisplayName("5: en el techo se mantiene. Muchas semanas cumplidas: cada sesion se detiene en 1,10 x base, sin IA")
    void techo() throws Exception {
        long u = participante(GoalType.INCREASE_STRENGTH, FitnessLevel.INTERMEDIATE, 45);
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var base = semana(w1).stream().map(e -> volumen(e.getValue())).toList();
        long plan = w1;
        int pasos = 0, sinPaso = 0;
        for (int k = 0; k < 16; k++) {
            ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.HECHO);
            plan = ciclo(u, k, 0);
            if ("{}".equals(ultimo(u).get("progressed_sessions"))) sinPaso++;
            else pasos++;
            var vol = semana(plan).stream().map(e -> volumen(e.getValue())).toList();
            for (int i = 0; i < vol.size(); i++)
                assertTrue(vol.get(i) <= Math.floor(base.get(i) * 1.10), "techo superado en la sesion " + i);
        }
        var fin = semana(plan).stream().map(e -> volumen(e.getValue())).toList();
        // Las ultimas semanas: ninguna sesion puede dar otro paso sin pasar del techo o del 5 %.
        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        long ultimoPlan = ciclo(u, 16, 0);
        System.out.println("SIN_IA techo base=" + base + " final=" + fin + " pasos=" + pasos + " sin paso=" + sinPaso
                + " ultimo=" + ultimo(u).get("progressed_sessions"));
        assertEquals("{}", ultimo(u).get("progressed_sessions"), "en el techo ya no hay pasos: " + fin + " / " + base);
        assertEquals(semana(plan).stream().map(Map.Entry::getValue).toList(),
                semana(ultimoPlan).stream().map(Map.Entry::getValue).toList(), "mantenimiento: la misma rutina");
        assertSinViolaciones();
    }

    // =======================================================================
    // Contador
    // =======================================================================

    /** Un ciclo de la semana k (S1 + k), comprobando el numero exacto de llamadas a la IA. */
    private long ciclo(long u, int k, int llamadasEsperadas) {
        clearInvocations(ia);
        var plan = ciclo.runFor(u, S1.plusWeeks(k), S1.plusWeeks(k + 1));
        int llamadas = (int) Mockito.mockingDetails(ia).getInvocations().stream()
                .filter(i -> i.getMethod().getName().equals("pedirRespuesta")).count();
        assertEquals(llamadasEsperadas, llamadas, "llamadas a la IA en la semana " + (k + 2) + ": " + ultimo(u));
        assertTrue(plan.isPresent(), "sin semana publicada en la " + (k + 2) + ": " + ultimo(u));
        return plan.get();
    }

    // =======================================================================
    // IA simulada: solo lo que permite su intervencion
    // =======================================================================

    private List<CorrectedSession> corregir(PlanGenerationContext ctx) {
        var salida = new ArrayList<CorrectedSession>();
        for (var senalada : ctx.adjustment().targetedSessions()) {
            long id = senalada.plannedWorkoutId();
            var f = jdbc.queryForMap("SELECT scheduled_date, focus_code FROM planned_workouts WHERE planned_workout_id = ?", id);
            var actuales = jdbc.query("SELECT pwe.exercise_id, pwe.prescription_type, pwe.planned_sets, pwe.planned_reps,"
                            + " pwe.planned_duration_seconds, pwe.rest_seconds, e.movement_pattern FROM planned_workout_exercises pwe"
                            + " JOIN exercises e ON e.exercise_id = pwe.exercise_id WHERE pwe.planned_workout_id = ?"
                            + " ORDER BY pwe.exercise_order",
                    (rs, k) -> Map.entry(new PlanDraft.DraftExercise(rs.getLong(1), PrescriptionType.valueOf(rs.getString(2)),
                            rs.getObject(3, Short.class), rs.getObject(4, Short.class), rs.getObject(5, Integer.class), null,
                            rs.getObject(6, Short.class), null), String.valueOf(rs.getString(7))), id);
            int quitar = -1;
            if (senalada.intervencion() == TipoIntervencion.SEGURIDAD_ANTE_DOLOR)
                for (int k = 0; k < actuales.size() && quitar < 0; k++) {
                    long ej = actuales.get(k).getKey().exerciseId();
                    if (ctx.previousWeek().prescriptions().stream().anyMatch(x -> x.plannedWorkoutId().equals(id)
                            && x.exerciseId().equals(ej) && "PAIN_OR_DISCOMFORT".equals(x.skipReason()))) quitar = k;
                }
            if (senalada.intervencion() == TipoIntervencion.ACORTAR_O_REORGANIZAR)
                for (int k = actuales.size() - 1; k >= 0 && quitar < 0; k--)
                    if (MandatorySlot.ofMovementPattern(actuales.get(k).getValue()).isEmpty()) quitar = k;
            var lista = new ArrayList<PlanDraft.DraftExercise>();
            boolean hecho = false;
            boolean seguridad = senalada.intervencion() == TipoIntervencion.SEGURIDAD_ANTE_DOLOR;
            for (int k = 0; k < actuales.size(); k++) {
                var e = actuales.get(k).getKey();
                if (k == quitar && seguridad) {
                    // Seguridad: el que dolio se SUSTITUYE por otro del mismo patron, con su dosis.
                    var original = ctx.availableExercises().stream().filter(c -> c.exerciseId().equals(e.exerciseId()))
                            .findFirst().orElseThrow();
                    var enSesion = actuales.stream().map(x -> x.getKey().exerciseId()).toList();
                    var sustituto = ctx.availableExercises().stream()
                            .filter(c -> !enSesion.contains(c.exerciseId())
                                    && java.util.Objects.equals(c.movementPattern(), original.movementPattern())
                                    && c.defaultPrescription() == original.defaultPrescription()
                                    && java.util.Objects.equals(c.bodyPartCode(), original.bodyPartCode()))
                            .findFirst().orElseThrow(() -> new IllegalStateException("premisa: sin sustituto"));
                    lista.add(new PlanDraft.DraftExercise(sustituto.exerciseId(), e.prescriptionType(), e.plannedSets(),
                            e.plannedReps(), e.plannedDurationSeconds(), null, e.restSeconds(), null));
                    continue;
                }
                if (k == quitar) continue;
                Short series = e.plannedSets(), reps = e.plannedReps();
                if (senalada.intervencion() == TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION && !hecho
                        && e.prescriptionType() == PrescriptionType.SETS_REPS && reps != null
                        && reps - 2 >= minimo(ctx, e.exerciseId())) {
                    reps = (short) (reps - 2);
                    hecho = true;
                }
                lista.add(new PlanDraft.DraftExercise(e.exerciseId(), e.prescriptionType(), series, reps,
                        e.plannedDurationSeconds(), null, e.restSeconds(), null));
            }
            var fecha = ((java.sql.Date) f.get("scheduled_date")).toLocalDate().plusWeeks(1);
            int min = estimador.estimateMinutes(new PlanDraft.DraftWorkout(fecha,
                    WorkoutFocus.valueOf((String) f.get("focus_code")), "x", 0, lista), ctx.prescription());
            salida.add(new CorrectedSession(id, fecha, min, lista, "ajuste simulado"));
        }
        return salida;
    }

    private static int minimo(PlanGenerationContext ctx, long exerciseId) {
        var limites = ctx.prescription() == null ? null : ctx.prescription().repLimits();
        var c = ctx.availableExercises().stream().filter(x -> x.exerciseId() == exerciseId).findFirst();
        return limites == null || c.isEmpty() ? 0 : limites.minRepsFor(c.get().bodyPartCode(), ctx.profile().fitnessLevel());
    }

    // =======================================================================
    // Material
    // =======================================================================

    private long participante(GoalType objetivo, FitnessLevel nivel, int minutos) {
        String email = "sinia" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 1, 15), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("70"), null, nivel, objetivo, "sin ia", TrainingLocation.GYM,
                (short) 3, List.of((short) 1, (short) 3, (short) 5), (short) minutos, GIMNASIO,
                List.of(), "Ninguna", true, true, true, true, true));
        return u;
    }

    /** (planned_workout_id, ejercicios con su dosis) por fecha. */
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

    private static int volumen(List<Ej> l) {
        return l.stream().mapToInt(e -> {
            int s = e.series() == null ? 1 : e.series();
            return "DURATION".equals(e.tipo()) ? Math.round((float) (s * (e.segundos() == null ? 0 : e.segundos())) / 30)
                    : s * (e.reps() == null ? 0 : e.reps());
        }).sum();
    }

    private Long origen(long id) {
        return jdbc.queryForObject("SELECT source_planned_workout_id FROM planned_workouts WHERE planned_workout_id = ?",
                Long.class, id);
    }

    private List<Map<String, Object>> historial(long planId) {
        return jdbc.queryForList("SELECT ws.session_id, ws.status, ws.completion_percentage, ws.counts_toward_adherence,"
                + " pw.status AS pw_status FROM planned_workouts pw LEFT JOIN workout_sessions ws"
                + " ON ws.planned_workout_id = pw.planned_workout_id WHERE pw.plan_id = ? ORDER BY pw.scheduled_date", planId);
    }

    private int intentos(long u) {
        return jdbc.queryForObject("SELECT count(*) FROM plan_adaptation_attempts WHERE user_id = ?", Integer.class, u);
    }

    private Map<String, Object> ultimo(long u) {
        return jdbc.queryForMap("SELECT decision, status, generation_source, ai_calls, targeted_interventions::text,"
                + " progressed_sessions::text, conservation_report::text FROM plan_adaptation_attempts WHERE user_id = ?"
                + " ORDER BY attempt_id DESC LIMIT 1", u);
    }

    private static String informe(Map<String, Object> ev) {
        return String.valueOf(ev.get("conservation_report")).replace(" ", "");
    }

    private void ejecutar(long u, long planId, Dia... dias) {
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
                case FATIGA -> planes.skip(UserDetailsImpl.build(usuarios.findById(u).orElseThrow()), w,
                        new SkipWorkoutResource(SkipReason.FATIGUE));
                case TIEMPO -> saltoPorFaltaDeTiempo(UserDetailsImpl.build(usuarios.findById(u).orElseThrow()), w);
                case PARCIAL_TIEMPO -> parcialPorFaltaDeTiempo(u, w);
                case HECHO_CON_DOLOR -> conDolorEnElUltimo(u, w);
                case NADA -> { }
            }
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

    /**
     * Una sesion EMPEZADA que no se termina por falta de tiempo: el primer ejercicio completo y el
     * resto dejados por LACK_OF_TIME. Es el acortamiento SIN M (FaltaDeTiempoPolicy: en las parciales
     * no se pregunta nada), el unico tras el que el diseno permite reincorporar sin una M nueva (§5).
     */
    private void parcialPorFaltaDeTiempo(long u, long w) {
        long s = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
        var filas = jdbc.queryForList("SELECT planned_exercise_id, prescription_type, planned_sets, planned_reps,"
                + " planned_duration_seconds FROM planned_workout_exercises WHERE planned_workout_id = ?"
                + " ORDER BY exercise_order", w);
        // Un 35 % de la sesion (la media por ejercicio): PARTIAL (30-79,99 %, regla 17.2) y la semana,
        // con las otras dos completas, en un 78 %, por debajo del umbral. Por debajo del 30 % el
        // entrenamiento se cerraria como SKIPPED y, sin contexto, se conservaria (D1).
        double objetivo = 0.35 * filas.size();
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
        sesiones.handle(new FinishWorkoutSessionCommand(u, s, (short) 6, (short) 3, (short) 20));
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
