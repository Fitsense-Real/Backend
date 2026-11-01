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
@EnabledIfSystemProperty(named = "fitsense.cierre.pr17", matches = "true")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.progression.enabled=true"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Cierre PR #17 · 48 perfiles, reparacion por sustitucion, EF x LW-PREF-0.2, muestra ciega")
class CierrePR17LabIT {

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
            PlanningPolicy.EF, PlanningPolicy.C2D_EF, PlanningPolicy.C2D_V4C_EF);
    static int secuencia;

    record Perfil(String id, GoalType objetivo, FitnessLevel nivel, String equipo, int dias, int minutos,
                  boolean restringido, String grupo) {}

    record Resultado(PlanningPolicy politica, PlanDraft draft, List<String> problemas,
                     SaturacionContenidoV20.Resultado c2d) {
        boolean valido() {
            return draft != null && problemas.isEmpty();
        }
    }

    record Caso(Perfil p, long userId, PlanGenerationContext ctx, int div, Map<String, Resultado> r) {}

    static final Map<String, Caso> CASOS = new LinkedHashMap<>();

    @BeforeEach
    void antes() {
        when(ia.isEnabled()).thenReturn(false);
        assertTrue(postgres.getJdbcUrl().startsWith("jdbc:postgresql://localhost:"), "solo base efimera");
    }

    RuleBasedTrainingPlanGenerator gen() {
        return AopTestUtils.<RuleBasedTrainingPlanGenerator>getTargetObject(reglas);
    }

    Resultado evaluar(PlanGenerationContext ctx, PlanningPolicy pol, int div) {
        var d = generar(ctx);
        var prob = problemas(d, ctx, div);
        SaturacionContenidoV20.Resultado sat = null;
        if (pol.c2d() && d != null) {
            var sinC2d = problemas(d, ctx.conPolitica(sinC2D(pol)), div);
            if (!sinC2d.isEmpty() && sinC2d.stream().allMatch(x -> x.startsWith("V20:")))
                sat = validador.evaluarC2D(d, ctx, div, ConservationReport.vacio());
        }
        return new Resultado(pol, d, prob, sat);
    }

    // ============================================================================================ 1 · base (960)

    @Test
    @Order(1)
    void base() throws Exception {
        var resumen = new TreeMap<String, Integer>();
        for (var p : perfiles960()) {
            long u = crear(p, EQUIPOS.get(p.equipo()), p.dias());
            var prep = planComandos.prepare(new GenerateWeeklyPlanCommand(u, SEMANA, null, true, null, null));
            var base = prep.context();
            assertSame(PlanningPolicy.HISTORICA, base.policy());
            if ("DOLOR_P02".equals(p.grupo())) base = conExclusionesDeDolor(base);
            var mapa = new LinkedHashMap<String, Resultado>();
            for (var pol : POLITICAS) {
                var r = evaluar(base.conPolitica(pol), pol, prep.divisor());
                mapa.put(pol.id(), r);
                resumen.merge(pol.id() + ":" + (r.valido() ? "VALIDO" : "INVALIDO"), 1, Integer::sum);
            }
            CASOS.put(p.id(), new Caso(p, u, base, prep.divisor(), mapa));
        }
        System.out.println("PR17_BASE " + resumen);
        assertEquals(960, CASOS.size());
    }

    // ============================================================================================ 2 · los 48

    static final List<String> MATERIAL = List.of("band", "pull-up bar", "suspension straps", "anchor point",
            "wall bars", "dumbbell");

    List<Caso> los48() {
        return CASOS.values().stream().filter(c -> c.r().get(PlanningPolicy.C2D.id()).valido()
                && !c.r().get(PlanningPolicy.C2D_EF.id()).valido()
                && c.r().get(PlanningPolicy.C2D_EF.id()).problemas().stream()
                .anyMatch(x -> x.startsWith("VEF:") && x.contains("NO_FACTIBLE"))).toList();
    }

    @Test
    @Order(2)
    void alternativas48() throws Exception {
        var lista = los48();
        System.out.println("PR17_48 " + lista.size());
        var csv = new StringBuilder("perfil,objetivo,nivel,dias,minutos,equipo_declarado,division,patron_faltante,"
                + "alternativa,equipo_anadido,dias_alternativa,division_alternativa,anclas_pull_elegibles,"
                + "anclas_pull_patrones,traccion_colocaciones,traccion_elegidas,resultado,reglas,seg_min,seg_max,pct_min,"
                + "series_sem,v4_ok,clasificacion\n");
        for (var c : lista) {
            var p = c.p();
            var division = c.ctx().suggestedSplit().stream().map(x -> x.focus().name()).distinct()
                    .collect(Collectors.joining("/"));
            // A · material adicional declarado (uno cada vez)
            for (var m : MATERIAL) {
                var equipo = new ArrayList<>(EQUIPOS.get(p.equipo()));
                equipo.add(m);
                fila48(csv, c, division, "A_MATERIAL", m, p.dias(), equipo);
            }
            // B · otra distribucion (menos dias = otra division), mismo material
            fila48(csv, c, division, "B_DIVISION", "-", 4, EQUIPOS.get(p.equipo()));
            fila48(csv, c, division, "B_DIVISION", "-", 3, EQUIPOS.get(p.equipo()));
        }
        escribir("MATRIZ_ALTERNATIVAS_48.csv", csv.toString());
    }

    void fila48(StringBuilder csv, Caso c, String division, String alternativa, String material, int dias,
                List<String> equipo) throws Exception {
        var p = c.p();
        long u = crear(p, equipo, dias);
        var prep = planComandos.prepare(new GenerateWeeklyPlanCommand(u, SEMANA, null, true, null, null));
        var ctx = prep.context().conPolitica(PlanningPolicy.C2D_EF);
        var r = evaluar(ctx, PlanningPolicy.C2D_EF, prep.divisor());
        int max = ctx.effectiveMaxDifficulty();
        var compl = CatalogEligibilityPolicy.complementos(ctx.availableExercises(), ctx.profile().goalType());
        var anclas = ctx.availableExercises().stream().filter(x -> x.difficulty() <= max)
                .filter(x -> !compl.contains(x.exerciseId()))
                .filter(x -> ElegibilidadFuncional.esAncla(WorkoutFocus.PULL, x)).toList();
        var pats = anclas.stream().map(CandidateExercise::movementPattern).collect(Collectors.toCollection(TreeSet::new));
        var porId = porId(ctx);
        var traccion = new ArrayList<String>();
        long segMin = 0, segMax = 0;
        int series = 0;
        boolean v4 = true;
        int mm = ctx.effectiveSessionMinutes();
        if (r.draft() != null) {
            segMin = Long.MAX_VALUE;
            for (var w : r.draft().workouts()) {
                long s = EST.estimateSeconds(w, ctx.prescription());
                segMin = Math.min(segMin, s);
                segMax = Math.max(segMax, s);
                v4 &= s * 100 <= mm * 60L * 115;
                for (var e : w.exercises()) {
                    series += e.plannedSets() == null ? 1 : e.plannedSets();
                    var x = porId.get(e.exerciseId());
                    if (x != null && ElegibilidadFuncional.TRACCION.contains(String.valueOf(x.movementPattern()))
                            && !compl.contains(x.exerciseId()))
                        traccion.add(x.exerciseId() + ":" + w.focus() + ":" + e.plannedSets() + "x" + e.plannedReps());
                }
            }
        }
        var division2 = ctx.suggestedSplit().stream().map(x -> x.focus().name()).distinct().collect(Collectors.joining("/"));
        var reglas = r.problemas().stream().map(x -> x.contains(":") ? x.substring(0, x.indexOf(':')) : "OTRO")
                .distinct().sorted().collect(Collectors.joining(" "));
        String clasif;
        if (r.valido() && !traccion.isEmpty()) clasif = "ALTERNATIVA_REAL";
        else if (r.valido()) clasif = "VALIDO_SIN_TRACCION_OCULTA_CARENCIA";
        else if (anclas.isEmpty()) clasif = "NO_FACTIBLE_SIN_TRACCION_ELEGIBLE";
        else clasif = "NO_FACTIBLE_CON_TRACCION_ELEGIBLE";
        csv.append(String.join(",", p.id(), p.objetivo().name(), p.nivel().name(), String.valueOf(p.dias()),
                String.valueOf(p.minutos()), p.equipo(), division, "HORIZONTAL_PULL|VERTICAL_PULL", alternativa, material,
                String.valueOf(dias), division2, String.valueOf(anclas.size()), "\"" + String.join(" ", pats) + "\"",
                String.valueOf(traccion.size()), "\"" + String.join(" ", traccion) + "\"",
                r.valido() ? "VALIDO" : r.draft() == null ? "SIN_BORRADOR" : "INVALIDO", "\"" + reglas + "\"",
                String.valueOf(segMin), String.valueOf(segMax), fmt(r.draft() == null ? 0 : 100.0 * segMin / (mm * 60.0)),
                String.valueOf(series), String.valueOf(v4), clasif)).append('\n');
    }

    // ============================================================================================ 3 · REP-SUST-0.1

    @Test
    @Order(3)
    void reparacionSustitucion() throws Exception {
        var g = gen();
        var det = new StringBuilder("perfil,politica,paso,resultado_sin_reparacion,c2d_estado,movimiento,ejercicio_original,"
                + "nombre_original,patron_original,clase_original,dosis_original,seg_ejercicio_original,ejercicio_sustituto,"
                + "nombre_sustituto,patron_sustituto,clase_sustituto,equipo_sustituto,dosis_sustituto,seg_ejercicio_sustituto,"
                + "fecha,seg_sesion_antes,seg_sesion_despues,pct_antes,pct_despues,reglas_despues,ef_compatible\n");
        for (var id : List.of("R0040", "R0328", "R0616", "R0808")) {
            var c = CASOS.get(id);
            for (var pol : List.of(PlanningPolicy.HISTORICA, PlanningPolicy.C2D, PlanningPolicy.C2D_EF)) {
                var ctx = c.ctx().conPolitica(pol);
                var r = c.r().get(pol.id());
                var actual = r.draft();
                var sinC2D = ctx.conPolitica(new PlanningPolicy(pol.id(), pol.candidata(), false, pol.v4Segundos(),
                        pol.elegibilidadFuncional(), pol.descripcion()));
                for (int paso = 1; paso <= 6 && actual != null; paso++) {
                    var pr = problemas(actual, sinC2D, c.div());
                    if (pr.isEmpty() || !pr.stream().allMatch(x -> x.startsWith("V20:"))) break;
                    var s = validador.evaluarC2D(actual, sinC2D, c.div(), ConservationReport.vacio());
                    if (!"SUSTITUIR".equals(s.movimiento()) || s.mejorado() == null) {
                        det.append(String.join(",", id, pol.id(), String.valueOf(paso), r.valido() ? "VALIDO" : "INVALIDO",
                                s.estado().name(), "\"" + s.motivo() + "\"")).append('\n');
                        break;
                    }
                    fila(det, c, ctx, pol, paso, r, s, actual, s.mejorado());
                    actual = s.mejorado();
                }
            }
        }
        escribir("REPARACION_SUSTITUCION_DETALLE.csv", det.toString());

        // Impacto en los 960 con la reparacion encendida: que cambia y si algo empeora.
        var imp = new StringBuilder("perfil,objetivo,nivel,equipo,dias,minutos,politica,resultado_sin,resultado_con,identico,"
                + "c2d_sin,c2d_con,reglas_con\n");
        var resumen = new TreeMap<String, Integer>();
        try {
            g.setReparacionSustitucionV20(true);
            for (var c : CASOS.values())
                for (var pol : List.of(PlanningPolicy.HISTORICA, PlanningPolicy.C2D, PlanningPolicy.C2D_EF)) {
                    var sin = c.r().get(pol.id());
                    var con = evaluar(c.ctx().conPolitica(pol), pol, c.div());
                    boolean igual = firma(sin.draft()).equals(firma(con.draft()));
                    String t = (sin.valido() ? "V" : "I") + "->" + (con.valido() ? "V" : "I") + (igual ? "" : "*");
                    resumen.merge(pol.id() + ":" + t, 1, Integer::sum);
                    if (!igual || sin.valido() != con.valido())
                        imp.append(String.join(",", c.p().id(), c.p().objetivo().name(), c.p().nivel().name(),
                                c.p().equipo(), String.valueOf(c.p().dias()), String.valueOf(c.p().minutos()), pol.id(),
                                sin.valido() ? "VALIDO" : "INVALIDO", con.valido() ? "VALIDO" : "INVALIDO",
                                String.valueOf(igual), sin.c2d() == null ? "-" : sin.c2d().estado().name(),
                                con.c2d() == null ? "-" : con.c2d().estado().name(),
                                "\"" + con.problemas().stream().map(x -> x.contains(":") ? x.substring(0, x.indexOf(':')) : "OTRO")
                                        .distinct().collect(Collectors.joining(" ")) + "\"")).append('\n');
                }
        } finally {
            g.setReparacionSustitucionV20(false);
        }
        escribir("REPARACION_SUSTITUCION_IMPACTO.csv", imp.toString());
        System.out.println("PR17_REPARACION " + resumen);
    }

    void fila(StringBuilder det, Caso c, PlanGenerationContext ctx, PlanningPolicy pol, int paso, Resultado r,
              SaturacionContenidoV20.Resultado s, PlanDraft antes, PlanDraft despues) {
        var porId = porId(ctx);
        int m = ctx.effectiveSessionMinutes();
        for (int i = 0; i < antes.workouts().size(); i++) {
            var wa = antes.workouts().get(i);
            var wd = despues.workouts().get(i);
            if (firma(new PlanDraft(antes.source(), null, null, null, null, List.of(wa)))
                    .equals(firma(new PlanDraft(antes.source(), null, null, null, null, List.of(wd))))) continue;
            for (int j = 0; j < wa.exercises().size(); j++) {
                var ea = wa.exercises().get(j);
                var ed = wd.exercises().get(j);
                if (Objects.equals(ea.exerciseId(), ed.exerciseId())) continue;
                var xa = porId.get(ea.exerciseId());
                var xd = porId.get(ed.exerciseId());
                long sa = EST.estimateSeconds(wa, ctx.prescription()), sd = EST.estimateSeconds(wd, ctx.prescription());
                var pr = problemas(despues, ctx, c.div());
                det.append(String.join(",", c.p().id(), pol.id(), String.valueOf(paso), r.valido() ? "VALIDO" : "INVALIDO",
                        s.estado().name(), "\"" + s.motivo() + "\"", String.valueOf(ea.exerciseId()),
                        "\"" + (xa == null ? "?" : xa.name()) + "\"", String.valueOf(xa == null ? "-" : xa.movementPattern()),
                        xa == null ? "-" : CapabilityPrescriptionPolicy.claseDe(xa).name(),
                        dosis(ea), String.valueOf(segEj(ea, ctx)), String.valueOf(ed.exerciseId()),
                        "\"" + (xd == null ? "?" : xd.name()) + "\"", String.valueOf(xd == null ? "-" : xd.movementPattern()),
                        xd == null ? "-" : CapabilityPrescriptionPolicy.claseDe(xd).name(),
                        xd == null ? "-" : xd.equipmentCode(), dosis(ed), String.valueOf(segEj(ed, ctx)),
                        wa.scheduledDate().toString(), String.valueOf(sa), String.valueOf(sd), fmt(100.0 * sa / (m * 60.0)),
                        fmt(100.0 * sd / (m * 60.0)), "\"" + pr.stream().map(x -> x.contains(":") ? x.substring(0, x.indexOf(':')) : "OTRO")
                                .distinct().collect(Collectors.joining(" ")) + "\"",
                        String.valueOf(xd != null && ElegibilidadFuncional.compatible(wd.focus(), xd)))).append('\n');
            }
        }
    }

    static String dosis(PlanDraft.DraftExercise e) {
        return e.prescriptionType() + " " + e.plannedSets() + "x" + (e.plannedReps() != null ? e.plannedReps()
                : e.plannedDurationSeconds() + "s") + " desc " + e.restSeconds();
    }

    static long segEj(PlanDraft.DraftExercise e, PlanGenerationContext ctx) {
        var w = new PlanDraft.DraftWorkout(SEMANA, WorkoutFocus.FULL_BODY, "x", 1, List.of(e));
        return EST.estimateSeconds(w, ctx.prescription());
    }

    // ============================================================================================ 4 · EF x LW-PREF-0.2

    @Test
    @Order(4)
    void efPorLw() throws Exception {
        var g = gen();
        record Conf(String id, PlanningPolicy pol, boolean lw) {}
        var confs = List.of(new Conf("1_HIST", PlanningPolicy.HISTORICA, false), new Conf("2_HIST_LW", PlanningPolicy.HISTORICA, true),
                new Conf("3_EF", PlanningPolicy.EF, false), new Conf("4_EF_LW", PlanningPolicy.EF, true),
                new Conf("5_C2D_EF", PlanningPolicy.C2D_EF, false), new Conf("6_C2D_EF_LW", PlanningPolicy.C2D_EF, true),
                new Conf("7a_C2D_V4_EF", PlanningPolicy.C2D_V4C_EF, false), new Conf("7_C2D_V4_EF_LW", PlanningPolicy.C2D_V4C_EF, true));
        var csv = new StringBuilder("perfil,nivel,equipo,dias,minutos,configuracion,politica,lw,resultado,reglas,brecha,"
                + "grupos_cero,cardio_trabajo_min,series_fuerza,incompatibles_ef,v15,identico_a_sin_lw,dosis_identicas_a_sin_lw,"
                + "clasificacion\n");
        var resumen = new TreeMap<String, Integer>();
        for (var c : CASOS.values()) {
            if (c.p().objetivo() != GoalType.LOSE_WEIGHT) continue;
            var porId = porId(c.ctx());
            var compl = CatalogEligibilityPolicy.complementos(c.ctx().availableExercises(), "LOSE_WEIGHT");
            Map<String, Resultado> sinLw = new HashMap<>();
            for (var cf : confs) {
                Resultado r;
                try {
                    g.setPreferirGrupoNoCubiertoLW(cf.lw());
                    r = evaluar(c.ctx().conPolitica(cf.pol()), cf.pol(), c.div());
                } finally {
                    g.setPreferirGrupoNoCubiertoLW(false);
                }
                if (!cf.lw()) sinLw.put(cf.pol().id(), r);
                var ref = cf.lw() ? sinLw.get(cf.pol().id()) : r;
                var ctx = c.ctx().conPolitica(cf.pol());
                int brecha = r.draft() == null ? -1 : RuleBasedTrainingPlanGenerator.brechaGruposGrandes(ctx, r.draft());
                int brechaRef = ref.draft() == null ? -1 : RuleBasedTrainingPlanGenerator.brechaGruposGrandes(ctx, ref.draft());
                int inc = 0;
                if (r.draft() != null)
                    for (var w : r.draft().workouts())
                        for (var e : w.exercises()) {
                            var x = porId.get(e.exerciseId());
                            if (x != null && !compl.contains(x.exerciseId()) && !ElegibilidadFuncional.compatible(w.focus(), x)) inc++;
                        }
                long v15 = r.problemas().stream().filter(x -> x.startsWith("V15:")).count();
                boolean igual = firma(r.draft()).equals(firma(ref.draft()));
                boolean dosisIguales = dosisMultiset(r.draft()).equals(dosisMultiset(ref.draft()));
                String clasif;
                if (!cf.lw()) clasif = "REFERENCIA";
                else if (brechaRef <= 0) clasif = igual ? "SIN_BRECHA_SIN_CAMBIO" : "REGRESION_CAMBIO_SIN_BRECHA";
                else if (igual) clasif = "CON_BRECHA_SIN_ALTERNATIVA";
                else if ((ref.valido() && !r.valido()) || inc > 0 || cardio(r.draft(), porId) < cardio(ref.draft(), porId)
                        || v15 > ref.problemas().stream().filter(x -> x.startsWith("V15:")).count() || brecha > brechaRef)
                    clasif = "REGRESION";
                else if (brecha == 0) clasif = "CORREGIDO";
                else clasif = "MEJORADO_PARCIAL";
                resumen.merge(cf.id() + ":" + clasif + ":" + (r.valido() ? "V" : "I"), 1, Integer::sum);
                csv.append(String.join(",", c.p().id(), c.p().nivel().name(), c.p().equipo(), String.valueOf(c.p().dias()),
                        String.valueOf(c.p().minutos()), cf.id(), cf.pol().id(), String.valueOf(cf.lw()),
                        r.valido() ? "VALIDO" : "INVALIDO", "\"" + r.problemas().stream()
                                .map(x -> x.contains(":") ? x.substring(0, x.indexOf(':')) : "OTRO").distinct()
                                .collect(Collectors.joining(" ")) + "\"", String.valueOf(brecha),
                        "\"" + ceros(r.draft(), porId, compl) + "\"", fmt(cardio(r.draft(), porId)),
                        String.valueOf(fuerza(r.draft(), porId, compl)), String.valueOf(inc), String.valueOf(v15),
                        String.valueOf(igual), String.valueOf(dosisIguales), clasif)).append('\n');
            }
        }
        escribir("MATRIZ_EF_LW02.csv", csv.toString());
        System.out.println("PR17_EF_LW " + resumen);
    }

    /** Las dosis de las colocaciones (sin el id del ejercicio): la preferencia cambia QUE ejercicio, no la dosis. */
    static List<String> dosisMultiset(PlanDraft d) {
        if (d == null) return List.of();
        return d.workouts().stream().flatMap(w -> w.exercises().stream())
                .map(e -> e.prescriptionType() + ":" + e.plannedSets() + ":" + e.plannedReps() + ":"
                        + e.plannedDurationSeconds() + ":" + e.restSeconds()).sorted().toList();
    }

    static String ceros(PlanDraft d, Map<Long, CandidateExercise> porId, Set<Long> compl) {
        return ImplementacionC2DV4CMatrizIT.ceros(d, porId, compl);
    }

    static int fuerza(PlanDraft d, Map<Long, CandidateExercise> porId, Set<Long> compl) {
        return ImplementacionC2DV4CMatrizIT.fuerza(d, porId, compl);
    }

    static double cardio(PlanDraft d, Map<Long, CandidateExercise> porId) {
        return ImplementacionC2DV4CMatrizIT.cardio(d, porId);
    }

    // ============================================================================================ 5 · uso de ejercicios

    static final Set<Long> MARCADOS = Set.of(54L, 65L, 68L, 231L, 254L, 303L, 304L, 305L, 307L, 310L, 315L, 318L, 320L,
            465L, 504L, 505L, 775L, 808L, 853L, 873L, 875L, 880L, 882L, 883L, 899L, 901L, 904L, 905L, 908L, 939L, 973L,
            1066L, 1072L, 1077L, 1102L, 1103L, 1114L, 1136L, 1146L, 1152L, 1177L, 1188L, 1195L, 1196L, 1259L, 1261L,
            1294L, 1298L, 1308L, 1313L, 1314L, 1320L);

    @Test
    @Order(5)
    void usoEjercicios() throws Exception {
        var pols = List.of(PlanningPolicy.HISTORICA, PlanningPolicy.C2D, PlanningPolicy.C2D_EF);
        var uso = new TreeMap<Long, int[]>();
        var perfiles = new TreeMap<Long, Set<String>>();
        var elegibleEn = new TreeMap<Long, Integer>();
        for (var c : CASOS.values()) {
            for (var x : c.ctx().availableExercises())
                if (MARCADOS.contains(x.exerciseId())) elegibleEn.merge(x.exerciseId(), 1, Integer::sum);
            for (int k = 0; k < pols.size(); k++) {
                var r = c.r().get(pols.get(k).id());
                if (!r.valido()) continue;
                for (var w : r.draft().workouts())
                    for (var e : w.exercises())
                        if (MARCADOS.contains(e.exerciseId())) {
                            uso.computeIfAbsent(e.exerciseId(), z -> new int[3])[k]++;
                            perfiles.computeIfAbsent(e.exerciseId(), z -> new TreeSet<>()).add(c.p().id() + "@" + k);
                        }
            }
        }
        var sb = new StringBuilder("exercise_id,perfiles_donde_es_elegible,colocaciones_hist_validos,colocaciones_c2d_validos,"
                + "colocaciones_c2d_ef_validos,perfiles_c2d,perfiles_c2d_ef\n");
        for (var id : new TreeSet<>(MARCADOS)) {
            var u = uso.getOrDefault(id, new int[3]);
            var ps = perfiles.getOrDefault(id, Set.of());
            sb.append(String.join(",", String.valueOf(id), String.valueOf(elegibleEn.getOrDefault(id, 0)),
                    String.valueOf(u[0]), String.valueOf(u[1]), String.valueOf(u[2]),
                    String.valueOf(ps.stream().filter(x -> x.endsWith("@1")).count()),
                    String.valueOf(ps.stream().filter(x -> x.endsWith("@2")).count()))).append('\n');
        }
        escribir("USO_EJERCICIOS_MARCADOS.csv", sb.toString());
    }

    // ============================================================================================ 6 · muestra ciega

    record Ficha(String perfil, PlanningPolicy pol, boolean lw, String motivo) {}

    @Test
    @Order(6)
    void muestraCiega() throws Exception {
        var g = gen();
        var fichas = new ArrayList<Ficha>();
        for (var id : List.of("R0788", "R0792", "R0836", "R0840", "R0800"))
            fichas.add(new Ficha(id, PlanningPolicy.C2D, false, "CASO_EXTREMO_PULL_SIN_TRACCION (publicado por C2-D; NO_FACTIBLE con EF)"));
        // LOSE_WEIGHT: 3 con brecha sin alternativa (original), 3 con LW-PREF-0.2 aplicada (antes y despues)
        int brechaN = 0, lwN = 0;
        for (var c : CASOS.values()) {
            if (c.p().objetivo() != GoalType.LOSE_WEIGHT || !c.r().get("PLAN-HIST-1").valido()) continue;
            var d0 = c.r().get("PLAN-HIST-1").draft();
            int b = RuleBasedTrainingPlanGenerator.brechaGruposGrandes(c.ctx(), d0);
            if (b == 0) continue;
            Resultado conLw;
            try {
                g.setPreferirGrupoNoCubiertoLW(true);
                conLw = evaluar(c.ctx(), PlanningPolicy.HISTORICA, c.div());
            } finally {
                g.setPreferirGrupoNoCubiertoLW(false);
            }
            boolean cambia = !firma(conLw.draft()).equals(firma(d0));
            if (!cambia && brechaN < 3 && c.p().minutos() >= 45) {
                fichas.add(new Ficha(c.p().id(), PlanningPolicy.HISTORICA, false, "LW_BRECHA_FUERZA_SIN_ALTERNATIVA"));
                brechaN++;
            } else if (cambia && conLw.valido() && lwN < 3 && c.p().minutos() >= 45) {
                fichas.add(new Ficha(c.p().id(), PlanningPolicy.HISTORICA, false, "LW_BRECHA_ANTES_DE_LW02"));
                fichas.add(new Ficha(c.p().id(), PlanningPolicy.HISTORICA, true, "LW_DESPUES_DE_LW02"));
                lwN++;
            }
        }
        // EF regenerados (antes C2-D / despues C2-D-EF): 4, uno por objetivo
        var vistos = new HashSet<GoalType>();
        for (var c : CASOS.values()) {
            var a = c.r().get("PLAN-C2D-0.1");
            var b = c.r().get("PLAN-C2D-EF-0.1");
            if (!a.valido() || !b.valido() || firma(a.draft()).equals(firma(b.draft())) || !vistos.add(c.p().objetivo())) continue;
            fichas.add(new Ficha(c.p().id(), PlanningPolicy.C2D, false, "EF_ANTES (colocaciones no computables)"));
            fichas.add(new Ficha(c.p().id(), PlanningPolicy.C2D_EF, false, "EF_DESPUES"));
        }
        // C2-D saturado con EF (sesion bajo el 70 %): 4
        int sat = 0;
        for (var c : CASOS.values()) {
            var b = c.r().get("PLAN-C2D-EF-0.1");
            if (sat < 4 && b.valido() && b.c2d() != null && b.c2d().saturada()
                    && fichas.stream().noneMatch(f -> f.perfil().equals(c.p().id()))) {
                if (sat > 0 && c.p().objetivo() == CASOS.get(fichas.get(fichas.size() - 1).perfil()).p().objetivo()) continue;
                fichas.add(new Ficha(c.p().id(), PlanningPolicy.C2D_EF, false, "C2D_SATURADA_SESION_CORTA"));
                sat++;
            }
        }
        // Control: planes historicos normales, uno por objetivo y equipo
        var ctl = new HashSet<String>();
        for (var c : CASOS.values()) {
            var h = c.r().get("PLAN-HIST-1");
            String k = c.p().objetivo() + "|" + c.p().equipo();
            if (fichas.size() >= 30) break;
            if (!h.valido() || !"BASE".equals(c.p().grupo()) || c.p().minutos() < 45 || !ctl.add(k)) continue;
            if (!firma(h.draft()).equals(firma(c.r().get("PLAN-C2D-EF-0.1").draft()))) continue;
            fichas.add(new Ficha(c.p().id(), PlanningPolicy.HISTORICA, false, "CONTROL_SIN_ANOMALIA"));
        }
        // Planes completos con datos del catalogo
        var codigos = new ArrayList<String>();
        var rnd = new Random(1717);
        var pool = new ArrayList<Integer>();
        for (int i = 1; i <= 99; i++) pool.add(i);
        Collections.shuffle(pool, rnd);
        var cat = catalogo();
        var csv = new StringBuilder("codigo,objetivo,nivel,dias_disponibles,minutos_configurados,equipamiento,restricciones,"
                + "sesion,fecha_relativa,enfoque,duracion_estimada_min,orden,exercise_id,nombre,descripcion,patron,musculo_principal,"
                + "musculos_secundarios,tipo,series,repeticiones,segundos,descanso_s\n");
        var clave = new StringBuilder("codigo,perfil,politica,lw_pref_02,motivo_seleccion,estado_plan,problema_investigado,resultado_comparacion\n");
        int n = 0;
        for (var f : fichas) {
            var c = CASOS.get(f.perfil());
            Resultado r;
            if (f.lw()) {
                try {
                    g.setPreferirGrupoNoCubiertoLW(true);
                    r = evaluar(c.ctx().conPolitica(f.pol()), f.pol(), c.div());
                } finally {
                    g.setPreferirGrupoNoCubiertoLW(false);
                }
            } else r = c.r().get(f.pol().id());
            if (r.draft() == null) continue;
            String codigo = "P%02d".formatted(pool.get(n++));
            codigos.add(codigo);
            var ctx = c.ctx().conPolitica(f.pol());
            int s = 0;
            for (var w : r.draft().workouts()) {
                s++;
                long seg = EST.estimateSeconds(w, ctx.prescription());
                int k = 0;
                for (var e : w.exercises()) {
                    var x = cat.get(e.exerciseId());
                    csv.append(String.join(",", codigo, c.p().objetivo().name(), c.p().nivel().name(),
                            String.valueOf(c.p().dias()), String.valueOf(c.p().minutos()),
                            "\"" + String.join(" | ", EQUIPOS.get(c.p().equipo())) + "\"",
                            c.p().restringido() ? "\"sin impacto alto, sin suelo, sin rodillas, sin colgarse, sin brazos por encima de la cabeza\""
                                    : "DOLOR_P02".equals(c.p().grupo()) ? "\"6 ejercicios excluidos por dolor previo\"" : "ninguna",
                            String.valueOf(s), "dia " + (w.scheduledDate().toEpochDay() - SEMANA.toEpochDay() + 1),
                            w.focus().name(), fmt(seg / 60.0), String.valueOf(++k), String.valueOf(e.exerciseId()),
                            q(x == null ? "?" : x[0]), q(x == null ? "" : x[1]), x == null ? "-" : x[2], q(x == null ? "" : x[3]),
                            q(x == null ? "" : x[4]), e.prescriptionType().name(), String.valueOf(e.plannedSets()),
                            String.valueOf(e.plannedReps()), String.valueOf(e.plannedDurationSeconds()),
                            String.valueOf(e.restSeconds()))).append('\n');
                }
            }
            clave.append(String.join(",", codigo, c.p().id(), f.pol().id(), String.valueOf(f.lw()), q(f.motivo()),
                    r.valido() ? "VALIDO" : "INVALIDO (" + r.problemas().stream().map(x -> x.substring(0, Math.max(0, x.indexOf(':'))))
                            .distinct().collect(Collectors.joining(" ")) + ")",
                    q(f.motivo().split(" ")[0]), q(estadoEf(c)))).append('\n');
        }
        escribir("MUESTRA_CIEGA_PLANES_MATRIZ.csv", csv.toString());
        escribir("CLAVE_MUESTRA_PLANES_MATRIZ.csv", clave.toString());
        // Fichas de diagnostico NO_FACTIBLE: lo que habia disponible para la sesion PULL
        var diag = new StringBuilder("ficha,objetivo,nivel,dias_disponibles,minutos_configurados,equipamiento,sesion_requerida,"
                + "ejercicios_disponibles_en_la_zona,patrones_disponibles,decision_del_sistema\n");
        int dn = 0;
        for (var id : List.of("R0788", "R0800")) {
            var c = CASOS.get(id);
            var compl = CatalogEligibilityPolicy.complementos(c.ctx().availableExercises(), c.ctx().profile().goalType());
            var zona = c.ctx().availableExercises().stream().filter(x -> x.difficulty() <= c.ctx().effectiveMaxDifficulty())
                    .filter(x -> !compl.contains(x.exerciseId())).filter(x -> WorkoutFocus.PULL.accepts(x.bodyPartCode()))
                    .filter(x -> !"upper arms".equals(x.bodyPartCode()) || x.targetMuscle() == null || !x.targetMuscle().contains("triceps"))
                    .toList();
            diag.append(String.join(",", "D%02d".formatted(++dn), c.p().objetivo().name(), c.p().nivel().name(),
                    String.valueOf(c.p().dias()), String.valueOf(c.p().minutos()), q(String.join(" | ", EQUIPOS.get(c.p().equipo()))),
                    "PULL (espalda y biceps), dos veces por semana",
                    q(zona.stream().map(x -> x.exerciseId() + " " + x.name()).collect(Collectors.joining(" ; "))),
                    q(zona.stream().map(x -> String.valueOf(x.movementPattern())).sorted().distinct().collect(Collectors.joining(" "))),
                    q("No se genera la semana: ningun ejercicio de traccion (remo o dominada) disponible con ese material"))).append('\n');
        }
        escribir("FICHAS_NO_FACTIBLE.csv", diag.toString());
        // Catalogo completo de lo usado (para fichas de ciclos L6 y L8, en el ensamblado final)
        var full = new StringBuilder("exercise_id,nombre,descripcion,patron,musculo_principal,musculos_secundarios\n");
        for (var e : cat.entrySet())
            full.append(String.join(",", String.valueOf(e.getKey()), q(e.getValue()[0]), q(e.getValue()[1]), e.getValue()[2],
                    q(e.getValue()[3]), q(e.getValue()[4]))).append('\n');
        Files.createDirectories(Path.of("target/cierre_pr17"));
        Files.writeString(Path.of("target/cierre_pr17/catalogo_completo.csv"), full.toString());
        System.out.println("PR17_MUESTRA fichas=" + fichas.size() + " codigos=" + codigos.size());
    }

    // ============================================================================================ 7 · familias EF

    @Test
    @Order(7)
    void familiasEf() throws Exception {
        var cuenta = new TreeMap<String, int[]>();
        for (var c : CASOS.values()) {
            var porId = porId(c.ctx());
            var compl = CatalogEligibilityPolicy.complementos(c.ctx().availableExercises(), c.ctx().profile().goalType());
            for (var pol : List.of(PlanningPolicy.C2D, PlanningPolicy.C2D_EF)) {
                var r = c.r().get(pol.id());
                if (!r.valido()) continue;
                int k = pol == PlanningPolicy.C2D ? 0 : 1;
                for (var w : r.draft().workouts())
                    for (var e : w.exercises()) {
                        var x = porId.get(e.exerciseId());
                        if (x == null) continue;
                        String clave = String.join("|", w.focus().name(), String.valueOf(x.movementPattern()),
                                CapabilityPrescriptionPolicy.claseDe(x).name(), compl.contains(x.exerciseId()) ? "COMPLEMENTO" : "PROPIO",
                                String.valueOf(ElegibilidadFuncional.compatible(w.focus(), x)));
                        cuenta.computeIfAbsent(clave, z -> new int[2])[k]++;
                    }
            }
        }
        var sb = new StringBuilder("enfoque,patron,clase,rol,compatible_ef,colocaciones_c2d,colocaciones_c2d_ef\n");
        cuenta.forEach((k, v) -> sb.append(k.replace('|', ',')).append(',').append(v[0]).append(',').append(v[1]).append('\n'));
        escribir("FAMILIAS_EF_USO.csv", sb.toString());
    }

    static String estadoEf(Caso c) {
        var b = c.r().get("PLAN-C2D-EF-0.1");
        return "C2D-EF: " + (b.valido() ? "VALIDO" : "NO_FACTIBLE/INVALIDO") + "; HIST: "
                + (c.r().get("PLAN-HIST-1").valido() ? "VALIDO" : "INVALIDO");
    }

    static String q(String s) {
        return "\"" + (s == null ? "" : s.replace("\"", "'").replace("\n", " ").replace("\r", " ")) + "\"";
    }

    Map<Long, String[]> catalogo() {
        var m = new TreeMap<Long, String[]>();
        for (var f : jdbc.queryForList("SELECT e.exercise_id, coalesce(e.name_es, e.name_en) n, coalesce(e.instructions_es, '') i,"
                + " coalesce(e.movement_pattern, '-') p, coalesce(e.target_muscle, '-') t,"
                + " coalesce(array_to_string(e.secondary_muscles, ', '), '') s FROM exercises e WHERE e.is_active"))
            m.put(((Number) f.get("exercise_id")).longValue(), new String[]{(String) f.get("n"), (String) f.get("i"),
                    (String) f.get("p"), (String) f.get("t"), (String) f.get("s")});
        return m;
    }

    long crear(Perfil p, List<String> equipo, int dias) {
        String email = "labe" + (++secuencia) + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'LabE', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        boolean libre = !p.restringido();
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 6, 15), BiologicalSex.FEMALE,
                new BigDecimal("168"), new BigDecimal("68"), null, p.nivel(), p.objetivo(), "laboratorio e",
                "GIMNASIO".equals(p.equipo()) ? TrainingLocation.GYM : TrainingLocation.HOME, (short) dias,
                DIAS.get(dias), (short) p.minutos(), equipo, List.of(), "Ninguna", libre, libre, libre, libre, libre));
        return u;
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
                ? Path.of("docs/FitSense/cierre_final_pr17") : Path.of("target/cierre_pr17");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(nombre), contenido);
    }
}
