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
@EnabledIfSystemProperty(named = "fitsense.correccion.ef", matches = "true")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.progression.enabled=true"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Correccion funcional · matriz de 960 perfiles con y sin EF-0.1")
class CorreccionFuncionalMatrizIT {

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
            PlanningPolicy.C2D_V4C, PlanningPolicy.EF, PlanningPolicy.C2D_EF, PlanningPolicy.C2D_V4C_EF);
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
                "ms", "vef", "vef_tipos", "pull_sesiones", "pull_sin_traccion", "push_sin_empuje", "legs_sin_pierna",
                "colocaciones_incompatibles", "patrones")).append('\n');
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
                    var sinC2d = problemas(d, ctx.conPolitica(sinC2D(pol)), prep.divisor());
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
        escribir("MATRIZ_COMPARACION_FUNCIONAL.csv", csv.toString());
        System.out.println("EF_RESUMEN " + resumen);
        assertEquals(960, CASOS.size());
    }

    // ================================================================================================ 2 · catalogo

    static final String TEXTO_MATERIAL = "(mancuerna|barra|banda|polea|maquina|máquina|kettlebell|pesa rusa|disco|"
            + "cable|balon medicinal|balón medicinal|anilla|trx|banco|silla|mesa|puerta|step|caja)";

    /**
     * Escaneo sistematico del catalogo ACTIVO: zona frente a patron, tronco fuera de waist, material en el texto con
     * equipo de peso corporal, y los 7 ejercicios senalados. Solo marca; no reclasifica.
     */
    @Test
    @Order(2)
    void catalogo() throws Exception {
        var filas = jdbc.queryForList("SELECT e.exercise_id, e.source_code, replace(coalesce(e.name_es, e.name_en), ',', ' ')"
                + " nombre, bp.code body_part, eq.code equipment, coalesce(e.target_muscle, '-') target,"
                + " coalesce(e.movement_pattern, 'NULL') patron, e.difficulty_level, e.default_prescription,"
                + " lower(coalesce(e.instructions_es, '') || ' ' || coalesce(e.name_es, '') || ' ' || coalesce(e.name_en, '')) texto,"
                + " left(regexp_replace(coalesce(e.instructions_es, ''), '[\\r\\n,\";]+', ' ', 'g'), 260) extracto,"
                + " e.name_en, coalesce((SELECT string_agg(r.group_no || ':' || q.code, '|') FROM exercise_equipment_requirements r JOIN equipment_types q ON q.equipment_id = r.equipment_id"
                + " WHERE r.exercise_id = e.exercise_id), '-') requisitos"
                + " FROM exercises e JOIN body_parts bp ON bp.body_part_id = e.body_part_id JOIN equipment_types eq ON"
                + " eq.equipment_id = e.equipment_id WHERE e.is_active ORDER BY e.exercise_id");
        var sb = new StringBuilder("exercise_id,source_code,nombre,body_part,equipment,target,patron,clase,dificultad,"
                + "prescripcion,senales,material_en_texto,en_7_senalados,excluido_ef,name_en,requisitos_equipo,extracto_instrucciones\n");
        var re = java.util.regex.Pattern.compile(TEXTO_MATERIAL);
        var senalados = Set.of(775L, 1066L, 1259L, 1261L, 1308L, 315L, 320L);
        var resumen = new TreeMap<String, Integer>();
        for (var f : filas) {
            long id = ((Number) f.get("exercise_id")).longValue();
            String bp = (String) f.get("body_part"), pat = (String) f.get("patron"), eq = (String) f.get("equipment");
            var c = new CandidateExercise(id, (String) f.get("nombre"), bp, eq, ((Number) f.get("difficulty_level")).intValue(),
                    PrescriptionType.SETS_REPS, (String) f.get("target"), "NULL".equals(pat) ? null : pat);
            var senales = new ArrayList<String>();
            if (Set.of("HORIZONTAL_PUSH", "VERTICAL_PUSH", "HORIZONTAL_ADDUCTION").contains(pat) && "back".equals(bp))
                senales.add("EMPUJE_CON_ZONA_ESPALDA");
            if (Set.of("HORIZONTAL_PULL", "VERTICAL_PULL").contains(pat) && Set.of("chest", "upper legs", "lower legs").contains(bp))
                senales.add("TRACCION_CON_ZONA_" + bp.toUpperCase(Locale.ROOT).replace(' ', '_'));
            if (Set.of("KNEE_DOMINANT", "HIP_HINGE", "HIP_EXTENSION").contains(pat) && Set.of("chest", "upper arms",
                    "shoulders").contains(bp))
                senales.add("PIERNA_CON_ZONA_" + bp.toUpperCase(Locale.ROOT).replace(' ', '_'));
            if (Set.of("TRUNK_FLEXION", "TRUNK_ROTATION_LATERAL_FLEXION", "CORE_STABILITY").contains(pat) && !"waist".equals(bp))
                senales.add("TRONCO_FUERA_DE_WAIST");
            var m = re.matcher((String) f.get("texto"));
            var material = new TreeSet<String>();
            while (m.find()) material.add(m.group(1));
            if ("body weight".equals(eq) && !material.isEmpty()) senales.add("PESO_CORPORAL_CON_MATERIAL_EN_TEXTO");
            if ("NULL".equals(pat)) senales.add("SIN_PATRON");
            boolean marcado = senalados.contains(id);
            if (senales.isEmpty() && !marcado) continue;
            senales.forEach(x -> resumen.merge(x, 1, Integer::sum));
            sb.append(String.join(",", String.valueOf(id), String.valueOf(f.get("source_code")), "\"" + f.get("nombre") + "\"",
                    bp, eq, "\"" + f.get("target") + "\"", pat, CapabilityPrescriptionPolicy.claseDe(c).name(),
                    String.valueOf(f.get("difficulty_level")), String.valueOf(f.get("default_prescription")),
                    String.join(" ", senales), "\"" + String.join(" ", material) + "\"", String.valueOf(marcado),
                    ElegibilidadFuncional.excluidoGlobal(c).map(x -> "\"" + x + "\"").orElse("-"),
                    "\"" + String.valueOf(f.get("name_en")).replace("\"", "'") + "\"", String.valueOf(f.get("requisitos")),
                    "\"" + String.valueOf(f.get("extracto")).replace("\"", "'") + "\"")).append('\n');
        }
        escribir("ESCANEO_CATALOGO_ACTIVO.csv", sb.toString());
        System.out.println("EF_CATALOGO activos=" + filas.size() + " " + resumen);
    }

    // ================================================================================================ 3 · casos extremos

    @Test
    @Order(3)
    void casosExtremos() throws Exception {
        assertFalse(CASOS.isEmpty());
        var sb = new StringBuilder("perfil,politica,resultado,reglas,fecha,enfoque,seg_sesion,pct_de_M,orden,exercise_id,"
                + "nombre,body_part,patron,clase,slot,compatible_ef,ancla,tipo,series,reps,segundos,descanso\n");
        var eleg = new StringBuilder("perfil,enfoque,candidatos_zona,compatibles_ef,anclas_elegibles,patrones_compatibles\n");
        var alt = new StringBuilder("perfil,politica_base,sesiones,sesiones_sin_ancla,colocaciones_retiradas,"
                + "resultado_sin_incompatibles_EF,reglas\n");
        for (var id : List.of("R0788", "R0792", "R0836", "R0840", "R0800")) {
            var c = CASOS.get(id);
            var porId = porId(c.ctx());
            int m = c.ctx().effectiveSessionMinutes();
            for (var pol : List.of(PlanningPolicy.C2D, PlanningPolicy.C2D_EF)) {
                var r = c.r().get(pol.id());
                var reglas = r.problemas().stream().map(x -> x.substring(0, Math.max(0, x.indexOf(':')))).distinct()
                        .collect(Collectors.joining(" "));
                if (r.draft() == null) {
                    sb.append(String.join(",", id, pol.id(), "SIN_BORRADOR", "\"" + reglas + "\"")).append('\n');
                    continue;
                }
                for (var w : r.draft().workouts()) {
                    long seg = EST.estimateSeconds(w, c.ctx().prescription());
                    int k = 0;
                    for (var e : w.exercises()) {
                        var x = porId.get(e.exerciseId());
                        sb.append(String.join(",", id, pol.id(), r.valido() ? "VALIDO" : "INVALIDO", "\"" + reglas + "\"",
                                w.scheduledDate().toString(), w.focus().name(), String.valueOf(seg),
                                fmt(100.0 * seg / (m * 60.0)), String.valueOf(++k), String.valueOf(e.exerciseId()),
                                "\"" + (x == null ? "?" : x.name().replace("\"", "'")) + "\"",
                                x == null ? "?" : x.bodyPartCode(), x == null || x.movementPattern() == null ? "-" : x.movementPattern(),
                                x == null ? "-" : CapabilityPrescriptionPolicy.claseDe(x).name(),
                                x == null ? "-" : MandatorySlot.of(x).map(Enum::name).orElse("-"),
                                String.valueOf(x != null && ElegibilidadFuncional.compatible(w.focus(), x)),
                                String.valueOf(x != null && ElegibilidadFuncional.esAncla(w.focus(), x)),
                                e.prescriptionType().name(), String.valueOf(e.plannedSets()), String.valueOf(e.plannedReps()),
                                String.valueOf(e.plannedDurationSeconds()), String.valueOf(e.restSeconds()))).append('\n');
                    }
                }
            }
            // Elegibles por enfoque (los del split del perfil).
            var compl = CatalogEligibilityPolicy.complementos(c.ctx().availableExercises(), c.ctx().profile().goalType());
            int max = c.ctx().effectiveMaxDifficulty();
            var enfoques = c.ctx().suggestedSplit().stream().map(x -> x.focus()).distinct().toList();
            for (var f : enfoques) {
                var zona = c.ctx().availableExercises().stream().filter(x -> x.difficulty() <= max)
                        .filter(x -> !compl.contains(x.exerciseId()))
                        .filter(x -> f == WorkoutFocus.FULL_BODY || f.accepts(x.bodyPartCode())).toList();
                var comp = zona.stream().filter(x -> ElegibilidadFuncional.compatible(f, x)).toList();
                var anclas = comp.stream().filter(x -> ElegibilidadFuncional.esAncla(f, x)).count();
                var pats = new TreeMap<String, Integer>();
                comp.forEach(x -> pats.merge(String.valueOf(x.movementPattern()), 1, Integer::sum));
                eleg.append(String.join(",", id, f.name(), String.valueOf(zona.size()), String.valueOf(comp.size()),
                        String.valueOf(anclas), "\"" + pats.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
                                .collect(Collectors.joining(" ")) + "\"")).append('\n');
            }
            // Alternativa minima: el plan de C2-D sin las colocaciones que EF no cuenta. ¿Es valido con menos?
            var base = c.r().get(PlanningPolicy.C2D.id()).draft();
            if (base != null) {
                int retiradas = 0, sinAncla = 0;
                var ws = new ArrayList<PlanDraft.DraftWorkout>();
                for (var w : base.workouts()) {
                    var quedan = w.exercises().stream().filter(e -> {
                        var x = porId.get(e.exerciseId());
                        return x == null || compl.contains(x.exerciseId()) || ElegibilidadFuncional.compatible(w.focus(), x);
                    }).toList();
                    retiradas += w.exercises().size() - quedan.size();
                    if (!ElegibilidadFuncional.ancla(w.focus()).isEmpty() && quedan.stream().map(e -> porId.get(e.exerciseId()))
                            .filter(Objects::nonNull).noneMatch(x -> ElegibilidadFuncional.esAncla(w.focus(), x))) sinAncla++;
                    ws.add(new PlanDraft.DraftWorkout(w.scheduledDate(), w.focus(), w.name(), w.expectedDurationMinutes(), quedan));
                }
                var reducido = new PlanDraft(base.source(), base.modelName(), base.planName(), base.declaredTotalVolume(),
                        base.rationale(), ws);
                var pr = problemas(reducido, c.ctx().conPolitica(PlanningPolicy.C2D_EF), c.div());
                alt.append(String.join(",", id, "PLAN-C2D-0.1", String.valueOf(base.workouts().size()),
                        String.valueOf(sinAncla), String.valueOf(retiradas), pr.isEmpty() ? "VALIDO" : "INVALIDO",
                        "\"" + pr.stream().map(x -> x.substring(0, Math.max(0, x.indexOf(':')))).distinct()
                                .collect(Collectors.joining(" ")) + "\"")).append('\n');
            }
        }
        escribir("CASOS_EXTREMOS_DETALLE.csv", sb.toString());
        escribir("CASOS_EXTREMOS_ELEGIBLES.csv", eleg.toString());
        escribir("CASOS_EXTREMOS_ALTERNATIVA.csv", alt.toString());
        // R0800: series por zona y por sesion, y ejercicios repetidos (separar seleccion de volumen).
        var r800 = new StringBuilder("politica,fecha,enfoque,zona,series,ejercicios,repetidos_en_semana\n");
        var c = CASOS.get("R0800");
        var porId = porId(c.ctx());
        for (var pol : List.of(PlanningPolicy.HISTORICA, PlanningPolicy.C2D, PlanningPolicy.C2D_EF)) {
            var d = c.r().get(pol.id()).draft();
            if (d == null) { r800.append(pol.id()).append(",SIN_BORRADOR\n"); continue; }
            var usos = new HashMap<Long, Integer>();
            d.workouts().forEach(w -> w.exercises().forEach(e -> usos.merge(e.exerciseId(), 1, Integer::sum)));
            for (var w : d.workouts()) {
                var series = new TreeMap<String, Integer>();
                var ej = new TreeMap<String, Integer>();
                var rep = new TreeMap<String, Integer>();
                for (var e : w.exercises()) {
                    var x = porId.get(e.exerciseId());
                    String z = x == null ? "?" : x.bodyPartCode();
                    series.merge(z, e.plannedSets() == null ? 1 : (int) e.plannedSets(), Integer::sum);
                    ej.merge(z, 1, Integer::sum);
                    if (usos.get(e.exerciseId()) > 1) rep.merge(z, 1, Integer::sum);
                }
                for (var z : series.keySet())
                    r800.append(String.join(",", pol.id() + (c.r().get(pol.id()).valido() ? "" : "(INVALIDO)"),
                            w.scheduledDate().toString(), w.focus().name(), z, String.valueOf(series.get(z)),
                            String.valueOf(ej.get(z)), String.valueOf(rep.getOrDefault(z, 0)))).append('\n');
            }
        }
        escribir("AUDITORIA_R0800_DETALLE.csv", r800.toString());
    }

    // ================================================================================================ 4 · LOSE_WEIGHT 0.2

    @Test
    @Order(4)
    void preferenciaLoseWeightCorregida() throws Exception {
        assertFalse(CASOS.isEmpty());
        var gen = AopTestUtils.<RuleBasedTrainingPlanGenerator>getTargetObject(reglas);
        var csv = new StringBuilder("perfil,grupo,nivel,equipo,dias,minutos,resultado_original,resultado_lw02,identico,"
                + "brecha_original,brecha_lw02,grupos_cero_original,grupos_cero_lw02,cardio_trabajo_original,"
                + "cardio_trabajo_lw02,series_fuerza_original,series_fuerza_lw02,reglas_lw02\n");
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
                        String.valueOf(orig.draft() == null ? -1 : RuleBasedTrainingPlanGenerator.brechaGruposGrandes(c.ctx(), orig.draft())),
                        String.valueOf(d == null ? -1 : RuleBasedTrainingPlanGenerator.brechaGruposGrandes(c.ctx(), d)),
                        "\"" + ceros(orig.draft(), porId, compl) + "\"", "\"" + ceros(d, porId, compl) + "\"",
                        fmt(cardio(orig.draft(), porId)), fmt(cardio(d, porId)),
                        String.valueOf(fuerza(orig.draft(), porId, compl)), String.valueOf(fuerza(d, porId, compl)),
                        "\"" + prob.stream().map(x -> x.substring(0, Math.min(4, x.length()))).distinct()
                                .collect(Collectors.joining(" ")) + "\"")).append('\n');
            }
        } finally {
            gen.setPreferirGrupoNoCubiertoLW(false);
        }
        escribir("LOSE_WEIGHT_PREFERENCIA_LW02.csv", csv.toString());
    }

    // Copias exactas de los calculos del laboratorio anterior, para comparar con su CSV.
    static final List<String> GRANDES = List.of("chest", "back", "shoulders", "upper legs");

    static String ceros(PlanDraft d, Map<Long, CandidateExercise> porId, Set<Long> compl) {
        return ImplementacionC2DV4CMatrizIT.ceros(d, porId, compl);
    }

    static int fuerza(PlanDraft d, Map<Long, CandidateExercise> porId, Set<Long> compl) {
        return ImplementacionC2DV4CMatrizIT.fuerza(d, porId, compl);
    }

    static double cardio(PlanDraft d, Map<Long, CandidateExercise> porId) {
        return ImplementacionC2DV4CMatrizIT.cardio(d, porId);
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
                String.valueOf(series), String.valueOf(ms), String.valueOf(r.problemas().stream()
                        .filter(x -> x.startsWith("VEF:")).count()), "\"" + tiposVEF(r.problemas()) + "\""))
                .append(',').append(calidad(d, ctx)).append('\n');
    }

    static String tiposVEF(List<String> problemas) {
        long nf = problemas.stream().filter(x -> x.startsWith("VEF:") && x.contains("NO_FACTIBLE")).count();
        long cand = problemas.stream().filter(x -> x.startsWith("VEF:") && x.contains("candidatos elegibles")).count();
        long inc = problemas.stream().filter(x -> x.startsWith("VEF:") && x.contains("no cuenta como trabajo")).count();
        return "NF=" + nf + " CAND=" + cand + " INC=" + inc;
    }

    /** Calidad funcional de un borrador, con independencia de la politica que lo valido. */
    static String calidad(PlanDraft d, PlanGenerationContext ctx) {
        if (d == null) return "0,0,0,0,0,\"-\"";
        var porId = porId(ctx);
        var compl = CatalogEligibilityPolicy.complementos(ctx.availableExercises(), ctx.profile().goalType());
        int pull = 0, pullSin = 0, pushSin = 0, legsSin = 0, incompatibles = 0;
        var patrones = new TreeMap<String, Integer>();
        for (var w : d.workouts()) {
            var propios = w.exercises().stream().map(e -> porId.get(e.exerciseId())).filter(Objects::nonNull)
                    .filter(c -> !compl.contains(c.exerciseId())).toList();
            for (var c : propios) {
                if (!ElegibilidadFuncional.compatible(w.focus(), c)) incompatibles++;
                patrones.merge(c.movementPattern() == null ? "NULL" : c.movementPattern(), 1, Integer::sum);
            }
            boolean ancla = propios.stream().anyMatch(c -> ElegibilidadFuncional.esAncla(w.focus(), c));
            if (w.focus() == WorkoutFocus.PULL) { pull++; if (!ancla) pullSin++; }
            if (w.focus() == WorkoutFocus.PUSH && !ancla) pushSin++;
            if (w.focus() == WorkoutFocus.LEGS && !ancla) legsSin++;
        }
        return pull + "," + pullSin + "," + pushSin + "," + legsSin + "," + incompatibles + ",\""
                + patrones.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(" "))
                + "\"";
    }

    /** Solo para comparar en el laboratorio (V4-C + EF sin C2-D); no esta en el catalogo ni se puede guardar. */
    static final PlanningPolicy V4_EF_LAB = new PlanningPolicy("LAB-V4-EF", true, false, true, true, "laboratorio");

    static PlanningPolicy sinC2D(PlanningPolicy p) {
        if (p.elegibilidadFuncional()) return p.v4Segundos() ? V4_EF_LAB : PlanningPolicy.EF;
        return p.v4Segundos() ? PlanningPolicy.V4C : PlanningPolicy.HISTORICA;
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
                ? Path.of("docs/FitSense/correccion_elegibilidad_funcional") : Path.of("target/correccion_ef");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(nombre), contenido);
    }
}
