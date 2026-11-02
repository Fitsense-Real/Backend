package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.*;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.infrastructure.generation.rules.RuleBasedTrainingPlanGenerator;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.AopTestUtils;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * IMPLEMENTACION CONTROLADA · MATRIZ. Los mismos 960 perfiles, en el mismo orden (mismos user_id y semillas) que los
 * laboratorios anteriores, generados y validados con el GENERADOR y el VALIDADOR REALES bajo cada version de politica:
 * PLAN-HIST-1, PLAN-C2D-0.1, PLAN-V4-0.1 y PLAN-C2D-V4-0.1 (la configuracion del contexto es la historica; la politica
 * se aplica a una copia del contexto, que es como la recibe el validador en produccion). Ademas: auditoria del catalogo
 * de los casos extremos, la preferencia de cobertura de fuerza de LOSE_WEIGHT (desactivada salvo aqui) y el anexo de la
 * muestra profesional. PostgreSQL efimero, sin IA, sin Replicate.
 * Opt-in: {@code -Dfitsense.implementacion.c2d=true}; {@code -Dfitsense.laboratorio.capturar=true} escribe en docs.
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.implementacion.c2d", matches = "true")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.progression.enabled=true"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Implementacion controlada · matriz de 960 perfiles con las politicas reales")
class ImplementacionC2DV4CMatrizIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoBean ReplicateSessionFixGenerator ia;

    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanDraftValidator validador;
    @Autowired RuleBasedTrainingPlanGenerator reglas;
    @Autowired JdbcTemplate jdbc;

    static final SessionDurationEstimator EST = new SessionDurationEstimator();
    static final LocalDate SEMANA = LocalDate.of(2026, 11, 2);
    static final Map<String, List<String>> EQUIPOS = new LinkedHashMap<>();
    static final Map<Integer, List<Short>> DIAS = Map.of(
            1, List.of((short) 1), 2, List.of((short) 1, (short) 4), 3, List.of((short) 1, (short) 3, (short) 5),
            4, List.of((short) 1, (short) 2, (short) 4, (short) 5),
            6, List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6),
            7, List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6, (short) 7));

    static {
        EQUIPOS.put("GIMNASIO", List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine", "bench",
                "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine"));
        EQUIPOS.put("CASA_MANCUERNAS_BANDAS", List.of("body weight", "dumbbell", "band"));
        EQUIPOS.put("CASA_PESO_CORPORAL", List.of("body weight"));
    }

    static final List<PlanningPolicy> POLITICAS = List.of(PlanningPolicy.HISTORICA, PlanningPolicy.C2D,
            PlanningPolicy.V4C, PlanningPolicy.C2D_V4C);
    static int secuencia;
    static final Map<String, Caso> CASOS = new LinkedHashMap<>();

    record Perfil(String id, GoalType objetivo, FitnessLevel nivel, String equipo, int dias, int minutos,
                  boolean restringido, String grupo) {}

    record Resultado(PlanningPolicy politica, PlanDraft draft, List<String> problemas,
                     SaturacionContenidoV20.Resultado c2d) {
        boolean valido() {
            return draft != null && problemas.isEmpty();
        }
    }

    record Caso(Perfil p, long userId, PlanGenerationContext ctx, int div, Map<String, Resultado> r) {}

    @BeforeEach
    void antes() {
        when(ia.isEnabled()).thenReturn(false);
        assertTrue(postgres.getJdbcUrl().startsWith("jdbc:postgresql://localhost:"), "solo base efimera");
    }

    // ================================================================================================ 1 · matriz

    @Test
    @Order(1)
    void matriz() throws Exception {
        var csv = new StringBuilder(String.join(",", "perfil", "grupo", "user_id", "objetivo", "nivel", "equipo",
                "restricciones", "dias", "sesiones", "minutos", "politica", "resultado", "reglas_incumplidas",
                "c2d_estado", "c2d_evaluaciones", "c2d_motivo", "sesiones_bajo_piso_aceptadas", "identico_a_historica",
                "seg_min", "seg_max", "pct_min", "pct_max", "v4_segundos_ok", "ejercicios_max", "series_sem",
                "ms")).append('\n');
        var resumen = new TreeMap<String, Integer>();
        for (var p : perfiles960()) {
            long u = crear(p);
            var prep = planComandos.prepare(new GenerateWeeklyPlanCommand(u, SEMANA, null, true, null, null));
            var base = prep.context();
            assertSame(PlanningPolicy.HISTORICA, base.policy(), "la configuracion del laboratorio es la historica");
            if ("DOLOR_P02".equals(p.grupo())) base = conExclusionesDeDolor(base);
            var mapa = new LinkedHashMap<String, Resultado>();
            PlanDraft hist = null;
            for (var pol : POLITICAS) {
                var ctx = base.conPolitica(pol);
                long t0 = System.nanoTime();
                var d = generar(ctx);
                var prob = problemas(d, ctx, prep.divisor());
                SaturacionContenidoV20.Resultado sat = null;
                if (pol.c2d() && d != null) {
                    var sinC2d = problemas(d, ctx.conPolitica(pol.v4Segundos() ? PlanningPolicy.V4C
                            : PlanningPolicy.HISTORICA), prep.divisor());
                    if (!sinC2d.isEmpty() && sinC2d.stream().allMatch(x -> x.startsWith("V20:")))
                        sat = validador.evaluarC2D(d, ctx, prep.divisor(), ConservationReport.vacio());
                }
                long ms = (System.nanoTime() - t0) / 1_000_000;
                var r = new Resultado(pol, d, prob, sat);
                mapa.put(pol.id(), r);
                if (pol == PlanningPolicy.HISTORICA) hist = d;
                fila(csv, p, u, ctx, r, hist, ms);
                resumen.merge(pol.id() + ":" + (r.valido() ? "VALIDO" : d == null ? "SIN_BORRADOR" : "INVALIDO"), 1,
                        Integer::sum);
            }
            CASOS.put(p.id(), new Caso(p, u, base, prep.divisor(), mapa));
        }
        escribir("RESULTADOS_MATRIZ_C2D.csv", csv.toString());
        System.out.println("IMPL_RESUMEN " + resumen);
        assertEquals(960, CASOS.size());
    }

    // ================================================================================================ 2 · catalogo

    @Test
    @Order(2)
    void auditoriaCatalogo() throws Exception {
        assertFalse(CASOS.isEmpty());
        var usos = new StringBuilder("perfil,politica,fecha,enfoque,seg_sesion,pct_de_M,orden,exercise_id,tipo,series,reps,"
                + "segundos,descanso,clase,slot\n");
        var ids = new TreeSet<Long>();
        for (var id : List.of("R0788", "R0792", "R0836", "R0840", "R0800", "R0804", "R0848", "R0852")) {
            var c = CASOS.get(id);
            var r = c.r().get(PlanningPolicy.C2D.id());
            if (r.draft() == null) continue;
            var porId = porId(c.ctx());
            int m = c.ctx().effectiveSessionMinutes();
            for (var w : r.draft().workouts()) {
                long s = EST.estimateSeconds(w, c.ctx().prescription());
                int k = 0;
                for (var e : w.exercises()) {
                    ids.add(e.exerciseId());
                    var cand = porId.get(e.exerciseId());
                    usos.append(String.join(",", id, r.valido() ? "PLAN-C2D-0.1" : "PLAN-C2D-0.1(INVALIDO)",
                            w.scheduledDate().toString(), w.focus().name(), String.valueOf(s), fmt(100.0 * s / (m * 60.0)),
                            String.valueOf(++k), String.valueOf(e.exerciseId()), e.prescriptionType().name(),
                            String.valueOf(e.plannedSets()), String.valueOf(e.plannedReps()),
                            String.valueOf(e.plannedDurationSeconds()), String.valueOf(e.restSeconds()),
                            cand == null ? "?" : CapabilityPrescriptionPolicy.claseDe(cand).name(),
                            cand == null ? "-" : MandatorySlot.of(cand).map(Enum::name).orElse("-"))).append('\n');
                }
            }
        }
        escribir("USO_EJERCICIOS_CASOS_EXTREMOS.csv", usos.toString());
        escribir("CATALOGO_CASOS_EXTREMOS.csv", catalogo(ids));

        // Alternativas: todo lo elegible de espalda y brazo (biceps) para el PULL sin material, intermedio.
        var c = CASOS.get("R0788");
        var alt = c.ctx().availableExercises().stream()
                .filter(x -> Set.of("back", "upper arms").contains(x.bodyPartCode()))
                .map(CandidateExercise::exerciseId).collect(Collectors.toCollection(TreeSet::new));
        escribir("CATALOGO_ALTERNATIVAS_PULL_PESO_CORPORAL.csv", catalogo(alt));
    }

    String catalogo(Set<Long> ids) {
        if (ids.isEmpty()) return "vacio\n";
        var filas = jdbc.queryForList("SELECT e.exercise_id, e.source_code, e.name_en, e.name_es, bp.code body_part,"
                + " eq.code equipment, e.target_muscle, e.synergist_muscle, array_to_string(e.secondary_muscles, '|')"
                + " secondary_muscles, e.movement_pattern, e.is_isolation, e.difficulty_level, e.default_prescription,"
                + " e.is_active, left(regexp_replace(coalesce(e.instructions_es, ''), '[\\r\\n,\";]+', ' ', 'g'), 400)"
                + " instrucciones FROM exercises e JOIN body_parts bp ON bp.body_part_id = e.body_part_id JOIN"
                + " equipment_types eq ON eq.equipment_id = e.equipment_id WHERE e.exercise_id IN ("
                + ids.stream().map(String::valueOf).collect(Collectors.joining(",")) + ") ORDER BY e.exercise_id");
        var sb = new StringBuilder(String.join(",", filas.get(0).keySet())).append('\n');
        for (var f : filas)
            sb.append(f.values().stream().map(v -> "\"" + String.valueOf(v).replace("\"", "'") + "\"")
                    .collect(Collectors.joining(","))).append('\n');
        return sb.toString();
    }

    // ================================================================================================ 3 · LOSE_WEIGHT

    @Test
    @Order(3)
    void preferenciaCoberturaLoseWeight() throws Exception {
        assertFalse(CASOS.isEmpty());
        var gen = AopTestUtils.<RuleBasedTrainingPlanGenerator>getTargetObject(reglas);
        var csv = new StringBuilder("perfil,grupo,nivel,equipo,dias,minutos,resultado_original,resultado_preferencia,"
                + "identico,grupos_cero_original,grupos_cero_preferencia,cardio_trabajo_original,cardio_trabajo_preferencia,"
                + "series_fuerza_original,series_fuerza_preferencia,reglas_preferencia\n");
        try {
            gen.setPreferirGrupoNoCubiertoLW(true);
            for (var c : CASOS.values()) {
                if (c.p().objetivo() != GoalType.LOSE_WEIGHT) continue;
                var orig = c.r().get(PlanningPolicy.HISTORICA.id());
                var d = generar(c.ctx());
                var prob = problemas(d, c.ctx(), c.div());
                var porId = porId(c.ctx());
                var compl = CatalogEligibilityPolicy.complementos(c.ctx().availableExercises(), "LOSE_WEIGHT");
                csv.append(String.join(",", c.p().id(), c.p().grupo(), c.p().nivel().name(), c.p().equipo(),
                        String.valueOf(c.p().dias()), String.valueOf(c.p().minutos()),
                        orig.valido() ? "VALIDO" : "INVALIDO", d != null && prob.isEmpty() ? "VALIDO" : "INVALIDO",
                        String.valueOf(firma(d).equals(firma(orig.draft()))),
                        "\"" + ceros(orig.draft(), porId, compl) + "\"", "\"" + ceros(d, porId, compl) + "\"",
                        fmt(cardio(orig.draft(), porId)), fmt(cardio(d, porId)),
                        String.valueOf(fuerza(orig.draft(), porId, compl)), String.valueOf(fuerza(d, porId, compl)),
                        "\"" + prob.stream().map(x -> x.substring(0, Math.min(4, x.length()))).distinct()
                                .collect(Collectors.joining(" ")) + "\"")).append('\n');
            }
        } finally {
            gen.setPreferirGrupoNoCubiertoLW(false);
        }
        escribir("LOSE_WEIGHT_PREFERENCIA_COBERTURA.csv", csv.toString());
    }

    static final List<String> GRANDES = List.of("chest", "back", "shoulders", "upper legs");

    static String ceros(PlanDraft d, Map<Long, CandidateExercise> porId, Set<Long> compl) {
        if (d == null) return "SIN_BORRADOR";
        var entrenables = new TreeSet<String>();
        for (var w : d.workouts())
            for (var g : GRANDES) if (w.focus() == WorkoutFocus.FULL_BODY || w.focus().accepts(g)) entrenables.add(g);
        var series = new HashMap<String, Integer>();
        for (var w : d.workouts())
            for (var e : w.exercises()) {
                var c = porId.get(e.exerciseId());
                if (c == null || compl.contains(c.exerciseId()) || e.prescriptionType() != PrescriptionType.SETS_REPS)
                    continue;
                var clase = CapabilityPrescriptionPolicy.claseDe(c);
                if (clase != CapabilityPrescriptionPolicy.Clase.ESTRUCTURAL && clase != CapabilityPrescriptionPolicy.Clase.RESISTIDO)
                    continue;
                series.merge(c.bodyPartCode(), e.plannedSets() == null ? 1 : (int) e.plannedSets(), Integer::sum);
            }
        return entrenables.stream().filter(g -> series.getOrDefault(g, 0) == 0).collect(Collectors.joining(" "));
    }

    static int fuerza(PlanDraft d, Map<Long, CandidateExercise> porId, Set<Long> compl) {
        if (d == null) return 0;
        int n = 0;
        for (var w : d.workouts())
            for (var e : w.exercises()) {
                var c = porId.get(e.exerciseId());
                if (c == null || compl.contains(c.exerciseId())) continue;
                var clase = CapabilityPrescriptionPolicy.claseDe(c);
                if (clase == CapabilityPrescriptionPolicy.Clase.ESTRUCTURAL || clase == CapabilityPrescriptionPolicy.Clase.RESISTIDO)
                    n += e.plannedSets() == null ? 1 : e.plannedSets();
            }
        return n;
    }

    static double cardio(PlanDraft d, Map<Long, CandidateExercise> porId) {
        if (d == null) return 0;
        var bloques = new ArrayList<MetricaAerobica.Bloque>();
        for (var w : d.workouts())
            for (var e : w.exercises()) {
                var c = porId.get(e.exerciseId());
                if (c == null || CapabilityPrescriptionPolicy.claseDe(c) != CapabilityPrescriptionPolicy.Clase.CARDIO) continue;
                bloques.add(new MetricaAerobica.Bloque(e.plannedSets() == null ? 1 : e.plannedSets(),
                        e.plannedDurationSeconds() == null ? 0 : e.plannedDurationSeconds(),
                        e.restSeconds() == null ? 0 : e.restSeconds()));
            }
        return MetricaAerobica.programada(bloques).minutosTrabajoProgramados();
    }

    // ================================================================================================ 4 · anexo muestra

    @Test
    @Order(4)
    void anexoMuestra() throws Exception {
        assertFalse(CASOS.isEmpty());
        var sb = new StringBuilder("perfil,motivo,objetivo,nivel,dias,minutos_configurados,equipo,restricciones,fecha,"
                + "enfoque,seg_sesion,pct_de_M,orden,exercise_id,nombre,body_part,patron,slot,clase,tipo,series,reps,"
                + "segundos,descanso_s\n");
        var elegidos = new LinkedHashMap<String, String>();
        for (var id : List.of("R0788", "R0792", "R0836", "R0840")) elegidos.put(id, "SESION_46_PCT");
        elegidos.put("R0800", "VOLUMEN_CONCENTRADO_FUERZA");
        // LOSE_WEIGHT con grupos grandes sin fuerza: los tres primeros validos con los cuatro grupos a cero y
        // dos con un solo grupo a cero, en orden de perfil (deterministico).
        int cuatro = 0, uno = 0;
        for (var c : CASOS.values()) {
            if (c.p().objetivo() != GoalType.LOSE_WEIGHT) continue;
            var r = c.r().get(PlanningPolicy.HISTORICA.id());
            if (!r.valido()) continue;
            var z = ceros(r.draft(), porId(c.ctx()),
                    CatalogEligibilityPolicy.complementos(c.ctx().availableExercises(), "LOSE_WEIGHT"));
            int n = z.isBlank() ? 0 : z.split(" ").length;
            if (n == 4 && cuatro < 3 && c.p().minutos() >= 45) { elegidos.put(c.p().id(), "LOSE_WEIGHT_SIN_FUERZA_4"); cuatro++; }
            else if (n == 1 && uno < 2) { elegidos.put(c.p().id(), "LOSE_WEIGHT_SIN_FUERZA_1"); uno++; }
        }
        for (var e : elegidos.entrySet()) {
            var c = CASOS.get(e.getKey());
            var r = c.r().get(e.getValue().startsWith("LOSE") ? PlanningPolicy.HISTORICA.id() : PlanningPolicy.C2D.id());
            if (!r.valido()) continue;
            var porId = porId(c.ctx());
            int m = c.ctx().effectiveSessionMinutes();
            for (var w : r.draft().workouts()) {
                long s = EST.estimateSeconds(w, c.ctx().prescription());
                int k = 0;
                for (var x : w.exercises()) {
                    var cand = porId.get(x.exerciseId());
                    sb.append(String.join(",", c.p().id(), e.getValue(), c.p().objetivo().name(), c.p().nivel().name(),
                            String.valueOf(c.p().dias()), String.valueOf(c.p().minutos()), c.p().equipo(),
                            c.p().restringido() || !"BASE".equals(c.p().grupo()) ? "SI" : "NO",
                            w.scheduledDate().toString(), w.focus().name(), String.valueOf(s), fmt(100.0 * s / (m * 60.0)),
                            String.valueOf(++k), String.valueOf(x.exerciseId()),
                            "\"" + (cand == null ? "?" : cand.name().replace("\"", "'")) + "\"",
                            cand == null ? "?" : cand.bodyPartCode(),
                            cand == null || cand.movementPattern() == null ? "-" : cand.movementPattern(),
                            cand == null ? "-" : MandatorySlot.of(cand).map(Enum::name).orElse("-"),
                            cand == null ? "-" : CapabilityPrescriptionPolicy.claseDe(cand).name(),
                            x.prescriptionType().name(), String.valueOf(x.plannedSets()), String.valueOf(x.plannedReps()),
                            String.valueOf(x.plannedDurationSeconds()), String.valueOf(x.restSeconds()))).append('\n');
                }
            }
        }
        escribir("ANEXO_MUESTRA_MATRIZ.csv", sb.toString());
    }

    // ================================================================================================ utilidades

    void fila(StringBuilder csv, Perfil p, long u, PlanGenerationContext ctx, Resultado r, PlanDraft hist, long ms) {
        var d = r.draft();
        int m = ctx.effectiveSessionMinutes();
        long piso = PlanDraftValidator.minimumSessionSeconds(ctx);
        long segMin = 0, segMax = 0;
        int ejMax = 0, bajo = 0, series = 0;
        boolean v4 = true;
        if (d != null) {
            segMin = Long.MAX_VALUE;
            for (var w : d.workouts()) {
                long s = EST.estimateSeconds(w, ctx.prescription());
                segMin = Math.min(segMin, s);
                segMax = Math.max(segMax, s);
                ejMax = Math.max(ejMax, w.exercises().size());
                if (s < piso) bajo++;
                v4 &= s * 100 <= m * 60L * 115;
                for (var e : w.exercises()) series += e.plannedSets() == null ? 1 : e.plannedSets();
            }
            if (d.workouts().isEmpty()) segMin = 0;
        }
        var codigos = r.problemas().stream().map(x -> x.contains(":") ? x.substring(0, x.indexOf(':')) : "OTRO")
                .distinct().sorted().collect(Collectors.joining(" "));
        var sat = r.c2d();
        csv.append(String.join(",", p.id(), p.grupo(), String.valueOf(u), p.objetivo().name(), p.nivel().name(),
                p.equipo(), p.restringido() ? "SI" : "NO", String.valueOf(p.dias()),
                String.valueOf(ctx.effectiveDaysPerWeek()), String.valueOf(p.minutos()), r.politica().id(),
                r.valido() ? "VALIDO" : d == null ? "SIN_BORRADOR" : "INVALIDO", "\"" + codigos + "\"",
                sat == null ? "-" : sat.estado().name(), sat == null ? "0" : String.valueOf(sat.evaluaciones()),
                "\"" + (sat == null ? "-" : sat.motivo().replace("\"", "'")) + "\"",
                String.valueOf(r.valido() ? bajo : 0), String.valueOf(firma(d).equals(firma(hist))),
                String.valueOf(segMin), String.valueOf(segMax), fmt(d == null ? 0 : 100.0 * segMin / (m * 60.0)),
                fmt(d == null ? 0 : 100.0 * segMax / (m * 60.0)), String.valueOf(v4), String.valueOf(ejMax),
                String.valueOf(series), String.valueOf(ms))).append('\n');
    }

    List<Perfil> perfiles960() {
        var lista = new ArrayList<Perfil>();
        int n = 0;
        for (var equipo : EQUIPOS.keySet())
            for (int dias : new int[]{1, 2, 3, 4, 6, 7})
                for (var o : GoalType.values())
                    for (var nv : FitnessLevel.values())
                        for (int m : new int[]{30, 45, 60, 75})
                            lista.add(new Perfil("R%04d".formatted(++n), o, nv, equipo, dias, m, false, "BASE"));
        for (var o : GoalType.values())
            for (var nv : FitnessLevel.values())
                for (int m : new int[]{30, 45, 60, 75}) {
                    lista.add(new Perfil("R%04d".formatted(++n), o, nv, "GIMNASIO", 3, m, true, "RESTRICCIONES"));
                    lista.add(new Perfil("R%04d".formatted(++n), o, nv, "GIMNASIO", 3, m, false, "DOLOR_P02"));
                }
        return lista;
    }

    PlanGenerationContext conExclusionesDeDolor(PlanGenerationContext c) {
        var d = generar(c);
        if (d == null) return c;
        var usos = new HashMap<Long, Integer>();
        d.workouts().forEach(w -> w.exercises().forEach(e -> usos.merge(e.exerciseId(), 1, Integer::sum)));
        var fuera = usos.entrySet().stream().sorted(Map.Entry.<Long, Integer>comparingByValue().reversed()
                .thenComparing(Map.Entry.comparingByKey())).limit(6).map(Map.Entry::getKey).collect(Collectors.toSet());
        var elegibles = c.availableExercises().stream().filter(x -> !fuera.contains(x.exerciseId())).toList();
        return new PlanGenerationContext(c.userId(), c.weekNumber(), c.weekStartDate(), c.weekEndDate(), c.profile(),
                c.adjustment(), c.previousWeek(), elegibles, c.safety(), c.prescription(), c.policy());
    }

    PlanDraft generar(PlanGenerationContext ctx) {
        try {
            return reglas.generate(ctx, List.of());
        } catch (RuntimeException e) {
            return null;
        }
    }

    List<String> problemas(PlanDraft d, PlanGenerationContext ctx, int div) {
        if (d == null) return List.of("SIN_BORRADOR");
        try {
            validador.validate(d, ctx, div);
            return List.of();
        } catch (InvalidPlanDraftException e) {
            return e.problems();
        }
    }

    static Map<Long, CandidateExercise> porId(PlanGenerationContext ctx) {
        return ctx.availableExercises().stream().collect(Collectors.toMap(CandidateExercise::exerciseId, c -> c, (a, b) -> a));
    }

    static String firma(PlanDraft d) {
        return d == null ? "null" : d.workouts().stream().map(w -> w.scheduledDate() + ":" + w.exercises().stream()
                .map(e -> e.exerciseId() + "x" + e.plannedSets() + "x" + e.plannedReps() + "x" + e.plannedDurationSeconds()
                        + "x" + e.restSeconds())
                .collect(Collectors.joining(","))).collect(Collectors.joining("|"));
    }

    static String fmt(double x) {
        return String.format(Locale.ROOT, "%.1f", x);
    }

    long crear(Perfil p) {
        String email = "labe" + (++secuencia) + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'LabE', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        boolean libre = !p.restringido();
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 6, 15), BiologicalSex.FEMALE,
                new BigDecimal("168"), new BigDecimal("68"), null, p.nivel(), p.objetivo(), "laboratorio e",
                "GIMNASIO".equals(p.equipo()) ? TrainingLocation.GYM : TrainingLocation.HOME, (short) p.dias(),
                DIAS.get(p.dias()), (short) p.minutos(), EQUIPOS.get(p.equipo()), List.of(), "Ninguna", libre, libre,
                libre, libre, libre));
        return u;
    }

    static void escribir(String nombre, String contenido) throws Exception {
        var dir = Boolean.getBoolean("fitsense.laboratorio.capturar")
                ? Path.of("docs/FitSense/implementacion_controlada_c2d_v4c") : Path.of("target/implementacion_c2d");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(nombre), contenido);
    }
}
