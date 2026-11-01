package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.FactibilidadDuracionIT;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.*;
import main.web.services.fitsense.planning.domain.services.experimental.*;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.infrastructure.generation.experimental.ConstructorSemanaModeloE;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.planning.interfaces.rest.TrainingPlanController;
import main.web.services.fitsense.planning.interfaces.rest.resources.SkipWorkoutResource;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.ContextoFaltaDeTiempo;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * VERIFICACION DE POLITICAS (orden de cierre). Sobre PostgreSQL EFIMERO, sin IA, sin Replicate y sin tocar la ruta
 * de produccion. Reproduce el laboratorio revisado (mismos 960 perfiles en el MISMO orden, de modo que los user_id y las
 * semillas coinciden) y compara:
 * <ul>
 *   <li>C1 vigente; C2-A (minimo 50 %), C2-B (55 %), C2-C (55 % GAIN_MUSCLE, 65 % el resto): se REGENERA con piso
 *       menor solo si V20 es la unica regla incumplida ({@link PisoV20Acotado}).</li>
 *   <li>C2-E: piso del 70 % intacto; se busca otra composicion anadiendo un ejercicio con su dosis base aprobada o una
 *       serie (hasta 3) a las sesiones cortas, aceptando un cambio solo si el validador VIGENTE no encuentra ningun
 *       problema distinto de V20. No cambia dosis, descansos ni topes.</li>
 *   <li>C2-D: el mejor esfuerzo (C2-E) se acepta si la sesion esta SATURADA: ningun cambio admisible mas.</li>
 * </ul>
 * Ademas mide V4 en sus tres interpretaciones, la metrica aerobica programada, la cobertura de fuerza de LOSE_WEIGHT,
 * prepara la muestra ciega para revision profesional y ejecuta un longitudinal de 10 semanas con progresion.
 * Opt-in: {@code -Dfitsense.verificacion.politicas=true}; {@code -Dfitsense.laboratorio.capturar=true} escribe en docs.
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.verificacion.politicas", matches = "true")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.progression.enabled=true"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Verificacion de politicas · C2-A..E, V4, cardio, muestra profesional y longitudinal")
class VerificacionPoliticasIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoBean ReplicateSessionFixGenerator ia;

    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanDraftValidator validador;
    @Autowired @Qualifier("ruleBasedTrainingPlanGenerator") TrainingPlanGenerator reglas;
    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyCycleService ciclo;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired TrainingPlanController planes;
    @Autowired UserRepository usuarios;
    @Autowired JdbcTemplate jdbc;

    static final SessionDurationEstimator ESTIMADOR = new SessionDurationEstimator();
    static final LocalDate SEMANA = FactibilidadDuracionIT.SEMANA;
    static final List<String> GRANDES = List.of("chest", "back", "shoulders", "upper legs");
    static int secuencia;

    /** Lo que el metodo 1 deja para los siguientes (mismo contexto de Spring y misma base). */
    static final Map<String, Caso> CASOS = new LinkedHashMap<>();

    record Perfil(String id, GoalType objetivo, FitnessLevel nivel, String equipo, int dias, int minutos,
                  boolean restringido, String grupo) {}

    record Resultado(String camino, PlanDraft draft, List<String> problemas, int piso, PlanGenerationContext ctx,
                     int cambios, boolean saturado) {
        boolean valido() {
            return draft != null && problemas.isEmpty();
        }
    }

    record Caso(Perfil p, long userId, PlanGenerationContext ctx, int div, Map<String, Resultado> caminos) {}

    @BeforeEach
    void antes() {
        when(ia.isEnabled()).thenReturn(false);
        assertTrue(postgres.getJdbcUrl().startsWith("jdbc:postgresql://localhost:"), "solo base efimera");
    }

    // ============================================================================================ 1 · matriz

    @Test
    @Order(1)
    void matrizC2() throws Exception {
        var lista = perfiles960();
        var csv = new StringBuilder(String.join(",", "perfil", "grupo", "user_id", "objetivo", "nivel", "equipo",
                "restricciones", "dias", "sesiones", "minutos", "camino", "resultado", "reglas_incumplidas", "piso_v20_pct",
                "saturado", "cambios_c2e", "identico_c1", "seg_min", "seg_max", "pct_min_sesion", "pct_max_sesion",
                "pct_prom", "ejercicios_min", "ejercicios_max", "series_totales_sem", "series_chest", "series_back",
                "series_shoulders", "series_upper_legs", "min_grupo_grande_entrenable", "max_series_grupo_sesion",
                "slots_cubiertos", "colocaciones_complemento", "repeticiones_de_ejercicio_en_semana",
                "cardio_trabajo_min", "cardio_bloque_min", "v4_historico_ok", "v4_truncado_ok", "v4_segundos_ok",
                "declarado_coincide", "ms")).append('\n');
        var resumen = new TreeMap<String, Integer>();
        var variantes = new LinkedHashMap<String, PisoV20Acotado>();
        variantes.put("C2A", PisoV20Acotado.c2a());
        variantes.put("C2B", PisoV20Acotado.c2b());
        variantes.put("C2C", PisoV20Acotado.c2c());
        for (var p : lista) {
            long u = crear(p);
            var prepared = planComandos.prepare(new GenerateWeeklyPlanCommand(u, SEMANA, null, true, null, null));
            var ctx = prepared.context();
            if ("DOLOR_P02".equals(p.grupo())) ctx = conExclusionesDeDolor(ctx);
            int div = prepared.divisor();
            var caminos = new LinkedHashMap<String, Resultado>();

            long t0 = System.nanoTime();
            var d1 = generar(ctx);
            var c1 = new Resultado("C1", d1, problemas(d1, ctx, div), 70, ctx, 0, false);
            caminos.put("C1", c1);
            long ms1 = (System.nanoTime() - t0) / 1_000_000;
            fila(csv, p, u, c1, d1, ms1);

            boolean ajuste = ctx.adjustment() != null && ctx.adjustment().isActive();
            for (var v : variantes.entrySet()) {
                t0 = System.nanoTime();
                var r = c1;
                if (d1 != null && v.getValue().puedeActuar(c1.problemas(), ajuste, true))
                    for (int piso : v.getValue().secuencia(ctx.profile().goalType())) {
                        var ctx2 = LaboratorioModeloEIT.conPiso(ctx, piso);
                        var d2 = generar(ctx2);
                        r = new Resultado(v.getKey(), d2, problemas(d2, ctx2, div), piso, ctx2, 0, false);
                        if (r.valido() || !PisoV20Acotado.soloV20(r.problemas())) break;
                    }
                r = new Resultado(v.getKey(), r.draft(), r.problemas(), r.piso(), r.ctx(), 0, false);
                caminos.put(v.getKey(), r);
                fila(csv, p, u, r, d1, (System.nanoTime() - t0) / 1_000_000);
            }

            // C2-E y C2-D
            t0 = System.nanoTime();
            var e = mejorar(c1, ctx, div);
            caminos.put("C2E", e);
            long msE = (System.nanoTime() - t0) / 1_000_000;
            fila(csv, p, u, e, d1, msE);
            Resultado dd;
            if (e.valido()) dd = new Resultado("C2D", e.draft(), List.of(), 70, ctx, e.cambios(), e.saturado());
            else if (e.draft() != null && PisoV20Acotado.soloV20(e.problemas()) && e.saturado())
                dd = new Resultado("C2D", e.draft(), List.of(), pctMinSesion(e.draft(), ctx), ctx, e.cambios(), true);
            else dd = new Resultado("C2D", e.draft(), e.problemas(), 70, ctx, e.cambios(), e.saturado());
            caminos.put("C2D", dd);
            fila(csv, p, u, dd, d1, msE);

            for (var r : caminos.values()) resumen.merge(r.camino() + ":" + clase(r), 1, Integer::sum);
            CASOS.put(p.id(), new Caso(p, u, ctx, div, caminos));
        }
        escribir("MATRIZ_C2_VERIFICADA.csv", csv.toString());
        System.out.println("VERIF_RESUMEN " + lista.size() + " perfiles " + resumen);
        assertEquals(960, lista.size());
    }

    // ============================================================================================ 2 · detalle

    /** Contenido de los planes recuperados, los 8 discrepantes, la muestra ciega y la referencia de menor tiempo. */
    @Test
    @Order(2)
    void detalleYMuestra() throws Exception {
        assertFalse(CASOS.isEmpty(), "requiere el metodo 1 en la misma ejecucion");
        var det = new StringBuilder(cabeceraDetalle()).append('\n');
        var recuperados = CASOS.values().stream().filter(c -> !c.caminos().get("C1").valido())
                .filter(c -> c.caminos().values().stream().anyMatch(Resultado::valido)).toList();
        for (var c : recuperados)
            for (var k : List.of("C1", "C2A", "C2C", "C2E", "C2D"))
                detalle(det, c.p().id(), k, c.caminos().get(k), c.ctx());
        // referencia experimental: mismo perfil con la mayor duracion menor que M cuyo plan vigente es valido
        var ref = new StringBuilder("perfil,minutos_configurados,minutos_referencia,resultado_referencia,seg_max,"
                + "series_totales_sem\n");
        var vistos = new HashSet<String>();
        for (var c : recuperados) {
            String clave = c.p().objetivo() + "|" + c.p().nivel() + "|" + c.p().equipo() + "|" + c.p().dias()
                    + "|" + c.p().minutos() + "|" + c.p().grupo();
            if (!vistos.add(clave) || !"BASE".equals(c.p().grupo())) continue;
            for (int m = c.p().minutos() - 5; m >= 30; m -= 5) {
                var q = new Perfil(c.p().id() + "@" + m, c.p().objetivo(), c.p().nivel(), c.p().equipo(), c.p().dias(), m,
                        false, "REFERENCIA");
                long u = crear(q);
                var prep = planComandos.prepare(new GenerateWeeklyPlanCommand(u, SEMANA, null, true, null, null));
                var d = generar(prep.context());
                var r = new Resultado("REF" + m, d, problemas(d, prep.context(), prep.divisor()), 70, prep.context(), 0,
                        false);
                if (!r.valido()) continue;
                var porId = porId(prep.context());
                var vol = VolumenSemanal.medir(d, porId, complementos(prep.context()));
                ref.append(String.join(",", c.p().id(), String.valueOf(c.p().minutos()), String.valueOf(m), "VALIDO",
                        String.valueOf(segMax(d, prep.context())),
                        String.valueOf(vol.porSemana().values().stream().mapToInt(i -> i).sum()))).append('\n');
                detalle(det, c.p().id(), "REF" + m, r, prep.context());
                break;
            }
        }
        escribir("PLANES_RECUPERADOS_DETALLE.csv", det.toString());
        escribir("REFERENCIA_MENOR_TIEMPO.csv", ref.toString());

        // ---------------------------------------------------------------- muestra ciega para revision profesional
        var elegidos = new ArrayList<String[]>(); // {perfil, camino, motivo}
        var objetivos = GoalType.values();
        var niveles = FitnessLevel.values();
        int[] mins = {30, 45, 60, 75};
        int[] dias = {2, 3, 4, 6};
        var equipos = new ArrayList<>(FactibilidadDuracionIT.EQUIPOS.keySet());
        int k = 0;
        for (var o : objetivos)
            for (var n : niveles) {
                int m = mins[k % 4], dd = dias[(k / 2) % 4];
                String eq = equipos.get(k % equipos.size());
                k++;
                var c = buscar(o, n, eq, dd, m, "BASE");
                if (c == null) continue;
                String camino = c.caminos().get("C1").valido() ? "C1" : "C2D";
                elegidos.add(new String[]{c.p().id(), camino, "BASE_" + o + "_" + n});
            }
        for (var id : List.of("GM75", "R0800", "R0436", "R0787")) {
            Caso c = id.equals("GM75") ? buscar(GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, "GIMNASIO", 3, 75, "BASE")
                    : CASOS.get(id);
            if (c != null) {
                // Solo planes VALIDOS (publicables): si C2-C no lo recupera, la variante regenerada es C2-A.
                String regenerado = c.caminos().get("C2C").valido() ? "C2C" : "C2A";
                if (c.caminos().get(regenerado).valido())
                    elegidos.add(new String[]{c.p().id(), regenerado, "C2_RECUPERADO_REGENERADO"});
                if (c.caminos().get("C2D").valido())
                    elegidos.add(new String[]{c.p().id(), "C2D", "C2_RECUPERADO_MEJOR_ESFUERZO"});
            }
        }
        for (var o : List.of(GoalType.LOSE_WEIGHT, GoalType.GENERAL_FITNESS)) {
            var c = buscar(o, FitnessLevel.INTERMEDIATE, "GIMNASIO", 3, 45, "RESTRICCIONES");
            if (c != null) elegidos.add(new String[]{c.p().id(), "C1", "RESTRICCIONES"});
        }
        CASOS.values().stream().filter(c -> c.p().objetivo() == GoalType.LOSE_WEIGHT && c.caminos().get("C1").valido())
                .sorted(Comparator.comparingDouble(c -> MetricaAerobica.programada(c.caminos().get("C1").draft(),
                        porId(c.ctx())).minutosBloqueProgramados()))
                .limit(2).forEach(c -> elegidos.add(new String[]{c.p().id(), "C1", "LOSE_WEIGHT_CARDIO_BAJO"}));
        CASOS.values().stream().filter(c -> c.caminos().get("C1").valido())
                .sorted(Comparator.comparingInt((Caso c) -> -seriesTotales(c.caminos().get("C1").draft(), c.ctx())))
                .limit(2).forEach(c -> elegidos.add(new String[]{c.p().id(), "C1", "VOLUMEN_ALTO"}));
        for (var o : List.of(GoalType.INCREASE_STRENGTH, GoalType.GENERAL_FITNESS)) {
            var c = buscar(o, FitnessLevel.BEGINNER, "CASA_PESO_CORPORAL", 3, 30, "BASE");
            if (c != null) elegidos.add(new String[]{c.p().id(), c.caminos().get("C1").valido() ? "C1" : "C2D",
                    "SESION_CORTA"});
        }
        // orden ciego determinista
        var orden = new ArrayList<>(elegidos);
        Collections.shuffle(orden, new Random(20261101L));
        // Ciega: sin camino, resultado ni piso (revelarian que plan recupera C2).
        var muestra = new StringBuilder("codigo_muestra,objetivo,nivel,dias,minutos_configurados,equipo,restricciones,"
                + cabeceraDetalle().split(",", 5)[4]).append('\n');
        var clave = new StringBuilder("codigo_muestra,perfil,camino,motivo_seleccion\n");
        int i = 0;
        for (var e : orden) {
            var c = CASOS.get(e[0]);
            var r = c.caminos().get(e[1]);
            if (r == null || !r.valido()) continue;
            String cod = "M%02d".formatted(++i);
            clave.append(String.join(",", cod, e[0], e[1], e[2])).append('\n');
            var tmp = new StringBuilder();
            detalle(tmp, "X", "Y", r, c.ctx());
            String prefijo = String.join(",", cod, c.p().objetivo().name(), c.p().nivel().name(),
                    String.valueOf(c.p().dias()), String.valueOf(c.p().minutos()), c.p().equipo(),
                    c.p().restringido() || !"BASE".equals(c.p().grupo()) ? "SI" : "NO");
            for (var linea : tmp.toString().split("\n"))
                if (!linea.isBlank()) muestra.append(prefijo).append(',').append(linea.split(",", 5)[4]).append('\n');
        }
        escribir("MUESTRA_REVISION_PROFESIONAL.csv", muestra.toString());
        escribir("CLAVE_MUESTRA_NO_ENTREGAR_AL_EVALUADOR.csv", clave.toString());
    }

    // ============================================================================================ 3 · LOSE_WEIGHT

    @Test
    @Order(3)
    void fuerzaYCardioLoseWeight() throws Exception {
        assertFalse(CASOS.isEmpty(), "requiere el metodo 1 en la misma ejecucion");
        var csv = new StringBuilder("perfil,grupo,nivel,equipo,dias,sesiones,minutos,restricciones,camino,cardio_trabajo_min,"
                + "cardio_bloque_min,series_fuerza_sem,grupos_grandes_entrenables,grupos_grandes_con_cero,"
                + "alternativa_valida_para_cero,motivo_primer_rechazo\n");
        for (var c : CASOS.values()) {
            if (c.p().objetivo() != GoalType.LOSE_WEIGHT) continue;
            var r = c.caminos().get("C1");
            if (!r.valido()) continue;
            var porId = porId(c.ctx());
            var met = MetricaAerobica.programada(r.draft(), porId);
            var vol = VolumenSemanal.medir(r.draft(), porId, complementos(c.ctx()));
            var entrenables = entrenables(r.draft());
            var ceros = entrenables.stream().filter(g -> vol.semanal(g) == 0).toList();
            String alt = "-", motivo = "-";
            if (!ceros.isEmpty()) {
                var intento = anadirGrupo(r.draft(), c.ctx(), c.div(), ceros.get(0));
                alt = intento == null ? "NO" : intento.isEmpty() ? "SI" : "NO";
                motivo = intento == null ? "sin candidato admisible del grupo o sin hueco (techo/tope)"
                        : intento.isEmpty() ? "-" : intento.get(0).replace(',', ';').replace('"', '\'');
                if (motivo.length() > 160) motivo = motivo.substring(0, 160);
            }
            csv.append(String.join(",", c.p().id(), c.p().grupo(), c.p().nivel().name(), c.p().equipo(),
                    String.valueOf(c.p().dias()), String.valueOf(c.ctx().effectiveDaysPerWeek()),
                    String.valueOf(c.p().minutos()), c.p().restringido() || !"BASE".equals(c.p().grupo()) ? "SI" : "NO",
                    "C1", fmt(met.minutosTrabajoProgramados()), fmt(met.minutosBloqueProgramados()),
                    String.valueOf(vol.porSemana().values().stream().mapToInt(i -> i).sum()),
                    "\"" + String.join(" ", entrenables) + "\"", "\"" + String.join(" ", ceros) + "\"", alt,
                    "\"" + motivo + "\"")).append('\n');
        }
        escribir("LOSE_WEIGHT_FUERZA_Y_CARDIO.csv", csv.toString());
    }

    /**
     * Anade al dia con hueco un ejercicio del grupo con su dosis base aprobada y valida con TODO el validador vigente
     * (70 %). null si no hay candidato que quepa; si no, los problemas (vacio = alternativa valida).
     */
    List<String> anadirGrupo(PlanDraft d, PlanGenerationContext ctx, int div, String grupo) {
        var porId = porId(ctx);
        var compl = complementos(ctx);
        List<String> mejor = null;
        for (int i = 0; i < d.workouts().size(); i++) {
            var w = d.workouts().get(i);
            if (!(w.focus() == WorkoutFocus.FULL_BODY || w.focus().accepts(grupo))) continue;
            if (w.exercises().size() >= CapabilityPrescriptionPolicy.MAX_COLOC_DIA) continue;
            var vecinos = vecinos(d, i);
            for (var c : ctx.availableExercises().stream().sorted(Comparator.comparing(CandidateExercise::exerciseId)).toList()) {
                if (!grupo.equals(c.bodyPartCode()) || compl.contains(c.exerciseId())) continue;
                if (c.difficulty() > ctx.effectiveMaxDifficulty() || vecinos.contains(c.exerciseId())) continue;
                if (w.exercises().stream().anyMatch(x -> x.exerciseId().equals(c.exerciseId()))) continue;
                var dosis = ConstructorSemanaModeloE.dosisBase(c, ctx.profile().goalType(), ctx.profile().fitnessLevel(),
                        ctx.prescription());
                if (dosis == null) continue;
                var nuevo = conEjercicio(d, i, dosis, ctx, div);
                var r = problemas(nuevo, ctx, div);
                if (r.isEmpty()) return List.of();
                if (mejor == null) mejor = r;
            }
        }
        return mejor;
    }

    // ============================================================================================ 4 · longitudinal

    /**
     * 10 semanas por el ciclo REAL con progresion ENCENDIDA: S1-S2 completas, S3 dolor (P-02), S4 ACORTAR (falta de
     * tiempo habitual con la mitad de M en la primera sesion), S5 RPE 9, S6 baja adherencia (solo una sesion hecha),
     * S7-S10 completas. El piso de la configuracion activa de la base efimera se fija por caso (aproximacion de C2:
     * en produccion C2 solo actuaria cuando V20 es la unica regla incumplida).
     */
    @Test
    @Order(4)
    void longitudinal() throws Exception {
        var casos = List.of(
                new Perfil("L1", GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, "GIMNASIO", 3, 60, false, "70"),
                new Perfil("L2", GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, "GIMNASIO", 3, 75, false, "70"),
                new Perfil("L3", GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, "GIMNASIO", 3, 75, false, "55"),
                new Perfil("L4", GoalType.INCREASE_STRENGTH, FitnessLevel.INTERMEDIATE, "CASA_PESO_CORPORAL", 6, 75, false, "70"),
                new Perfil("L5", GoalType.INCREASE_STRENGTH, FitnessLevel.INTERMEDIATE, "CASA_PESO_CORPORAL", 6, 75, false, "55"),
                new Perfil("L6", GoalType.LOSE_WEIGHT, FitnessLevel.BEGINNER, "CASA_MANCUERNAS_BANDAS", 4, 75, false, "60"),
                new Perfil("L7", GoalType.GENERAL_FITNESS, FitnessLevel.ADVANCED, "GIMNASIO", 4, 60, false, "70"));
        var csv = new StringBuilder("caso,piso_v20,objetivo,nivel,equipo,dias,minutos,semana,guion,plan_id,origen,sesiones,"
                + "series_totales_sem,reps_totales_sem,seg_duracion_sem,max_seg_sesion,exclusiones_activas,intento_status,"
                + "intervenciones,regeneracion_p03\n");
        for (var c : casos) {
            pisoBase(Integer.parseInt(c.grupo()));
            long u = crear(c);
            Long plan;
            try {
                plan = planning.generateWeeklyPlan(u, SEMANA, PlanAdjustment.none(), null, null).orElse(null);
            } catch (RuntimeException ex) {
                plan = null;
            }
            for (int s = 1; s <= 10; s++) {
                var inicio = SEMANA.plusWeeks(s - 1);
                String guion = switch (s) {
                    case 3 -> "DOLOR";
                    case 4 -> "ACORTAR";
                    case 5 -> "FATIGA_RPE9";
                    case 6 -> "BAJA_ADHERENCIA";
                    default -> "COMPLETA";
                };
                linea(csv, c, s, guion, u, plan);
                if (plan != null) guion(u, plan, guion, c.minutos());
                try {
                    plan = ciclo.runFor(u, inicio, inicio.plusWeeks(1)).orElse(null);
                } catch (RuntimeException ex) {
                    plan = null;
                }
            }
        }
        pisoBase(70);
        escribir("LONGITUDINAL_VERIFICACION.csv", csv.toString());
    }

    void guion(long u, long plan, String guion, int minutos) {
        var ws = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ? ORDER BY"
                + " scheduled_date", Long.class, plan);
        var user = UserDetailsImpl.build(usuarios.findById(u).orElseThrow());
        for (int k = 0; k < ws.size(); k++) {
            long w = ws.get(k);
            switch (guion) {
                case "DOLOR" -> {
                    if (k == 0) planes.skip(user, w, new SkipWorkoutResource(SkipReason.PAIN_OR_DISCOMFORT));
                    else hacer(u, w, (short) 6);
                }
                case "ACORTAR" -> {
                    if (k == 0) planes.skip(user, w, new SkipWorkoutResource(SkipReason.LACK_OF_TIME,
                            ContextoFaltaDeTiempo.MENOS_TIEMPO_HABITUAL, Math.max(15, minutos / 2)));
                    else hacer(u, w, (short) 6);
                }
                case "FATIGA_RPE9" -> hacer(u, w, (short) 9);
                case "BAJA_ADHERENCIA" -> {
                    if (k == 0) hacer(u, w, (short) 6);
                    else planes.skip(user, w, new SkipWorkoutResource(SkipReason.LACK_OF_MOTIVATION));
                }
                default -> hacer(u, w, (short) 6);
            }
        }
    }

    void hacer(long u, long w, short rpe) {
        long sesion = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
        for (var f : jdbc.queryForList("SELECT planned_exercise_id, prescription_type, planned_sets, planned_reps,"
                + " planned_duration_seconds FROM planned_workout_exercises WHERE planned_workout_id = ?"
                + " ORDER BY exercise_order", w)) {
            long pe = ((Number) f.get("planned_exercise_id")).longValue();
            int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
            boolean dur = "DURATION".equals(f.get("prescription_type"));
            int reps = f.get("planned_reps") == null ? 0 : ((Number) f.get("planned_reps")).intValue();
            int seg = f.get("planned_duration_seconds") == null ? 0 : ((Number) f.get("planned_duration_seconds")).intValue();
            sesiones.handle(new RecordSessionExerciseCommand(u, sesion, pe, (short) series, dur ? null : series * reps,
                    dur ? series * seg : null, null, null));
        }
        sesiones.handle(new FinishWorkoutSessionCommand(u, sesion, rpe, (short) 4, (short) 45));
    }

    void linea(StringBuilder csv, Perfil c, int s, String guion, long u, Long plan) {
        String origen = "-", st = "-", inter = "-", regen = "-";
        int n = 0, series = 0, reps = 0, seg = 0, maxSeg = 0;
        if (plan != null) {
            origen = jdbc.queryForObject("SELECT generation_source FROM weekly_training_plans WHERE plan_id = ?",
                    String.class, plan);
            n = jdbc.queryForObject("SELECT count(*) FROM planned_workouts WHERE plan_id = ?", Integer.class, plan);
            var t = jdbc.queryForMap("SELECT COALESCE(SUM(COALESCE(pwe.planned_sets,1)),0) s, COALESCE(SUM(CASE WHEN"
                    + " pwe.prescription_type = 'SETS_REPS' THEN COALESCE(pwe.planned_sets,1) * COALESCE(pwe.planned_reps,0)"
                    + " ELSE 0 END),0) r, COALESCE(SUM(CASE WHEN pwe.prescription_type = 'DURATION' THEN"
                    + " COALESCE(pwe.planned_sets,1) * COALESCE(pwe.planned_duration_seconds,0) ELSE 0 END),0) d"
                    + " FROM planned_workout_exercises pwe JOIN planned_workouts pw ON pw.planned_workout_id ="
                    + " pwe.planned_workout_id WHERE pw.plan_id = ?", plan);
            series = ((Number) t.get("s")).intValue();
            reps = ((Number) t.get("r")).intValue();
            seg = ((Number) t.get("d")).intValue();
            Integer mx = jdbc.queryForObject("SELECT MAX(expected_duration_minutes) FROM planned_workouts WHERE plan_id = ?",
                    Integer.class, plan);
            maxSeg = mx == null ? 0 : mx * 60;
        }
        var a = jdbc.queryForList("SELECT status, array_to_string(targeted_interventions, ' ') i FROM"
                + " plan_adaptation_attempts WHERE user_id = ? ORDER BY attempt_id DESC LIMIT 1", u);
        if (!a.isEmpty()) {
            st = String.valueOf(a.get(0).get("status"));
            inter = String.valueOf(a.get(0).get("i"));
        }
        var r = jdbc.queryForList("SELECT outcome FROM safe_regeneration_attempts WHERE user_id = ? ORDER BY"
                + " regeneration_id DESC LIMIT 1", String.class, u);
        if (!r.isEmpty()) regen = r.get(0);
        int excl = jdbc.queryForObject("SELECT count(*) FROM user_safety_exclusions WHERE user_id = ? AND status ="
                + " 'ACTIVA'", Integer.class, u);
        csv.append(String.join(",", c.id(), c.grupo(), c.objetivo().name(), c.nivel().name(), c.equipo(),
                String.valueOf(c.dias()), String.valueOf(c.minutos()), "S" + s, guion,
                plan == null ? "SIN_PLAN" : String.valueOf(plan), origen, String.valueOf(n), String.valueOf(series),
                String.valueOf(reps), String.valueOf(seg), String.valueOf(maxSeg), String.valueOf(excl), st,
                "\"" + inter + "\"", regen)).append('\n');
    }

    void pisoBase(int pct) {
        assertTrue(postgres.getJdbcUrl().startsWith("jdbc:postgresql://localhost:"), "solo base efimera");
        jdbc.update("UPDATE calculation_configs SET params = jsonb_set(params, '{prescription,session_minutes_floor_pct}',"
                + " to_jsonb(?::int), true) WHERE is_active", pct);
    }

    // ============================================================================================ 5 · diagnostico

    /**
     * Por que no llegan al piso los casos discrepantes: en cada sesion corta del plan vigente, que pasa con cada
     * candidato (anadir con dosis base) y con cada serie adicional. Cuenta el motivo de descarte o el primer codigo
     * distinto de V20 que devuelve el validador vigente.
     */
    @Test
    @Order(5)
    void diagnosticoDiscrepantes() throws Exception {
        assertFalse(CASOS.isEmpty(), "requiere el metodo 1 en la misma ejecucion");
        var csv = new StringBuilder("perfil,objetivo,nivel,equipo,dias,minutos,fecha,enfoque,ejercicios,tope,seg,minimo_70_s,"
                + "pct_de_M,candidatos_totales,ya_en_dia,dia_pegado_V18,fuera_de_enfoque,sin_dosis_base,supera_techo,"
                + "rechazos_validador,aceptables,series_posibles,series_rechazadas,elegibles_del_enfoque\n");
        for (var id : List.of("R0800", "R0804", "R0848", "R0852", "R0436", "R0532", "R0724", "R0820", "R0116", "R0788",
                "R0787")) {
            var c = CASOS.get(id);
            if (c == null) continue;
            var d = c.caminos().get("C1").draft();
            if (d == null) continue;
            var ctx = c.ctx();
            long minimo = PlanDraftValidator.minimumSessionSeconds(ctx);
            int tope = MvpDosePolicy.aplicaA(ctx.profile().goalType())
                    ? MvpDosePolicy.maxColocaciones(ctx.effectiveSessionMinutes()) : CapabilityPrescriptionPolicy.MAX_COLOC_DIA;
            long techo = ctx.profile().maxSessionMinutes() * 60L + 29;
            var pool = ctx.availableExercises().stream().filter(x -> x.difficulty() <= ctx.effectiveMaxDifficulty())
                    .sorted(Comparator.comparing(CandidateExercise::exerciseId)).toList();
            for (int i = 0; i < d.workouts().size(); i++) {
                var w = d.workouts().get(i);
                long seg = ESTIMADOR.estimateSeconds(w, ctx.prescription());
                if (seg >= minimo) continue;
                var vecinos = vecinos(d, i);
                int enDia = 0, pegado = 0, enfoque = 0, sinDosis = 0, superaTecho = 0, aceptables = 0, delEnfoque = 0;
                var rechazos = new TreeMap<String, Integer>();
                for (var cand : pool) {
                    var clase = CapabilityPrescriptionPolicy.claseDe(cand);
                    boolean resistido = clase == CapabilityPrescriptionPolicy.Clase.ESTRUCTURAL
                            || clase == CapabilityPrescriptionPolicy.Clase.RESISTIDO;
                    boolean enfocado = !resistido || w.focus() == WorkoutFocus.FULL_BODY || w.focus().accepts(cand.bodyPartCode());
                    if (enfocado) delEnfoque++;
                    if (w.exercises().stream().anyMatch(x -> x.exerciseId().equals(cand.exerciseId()))) { enDia++; continue; }
                    if (vecinos.contains(cand.exerciseId())) { pegado++; continue; }
                    if (!enfocado) { enfoque++; continue; }
                    var dosis = ConstructorSemanaModeloE.dosisBase(cand, ctx.profile().goalType(),
                            ctx.profile().fitnessLevel(), ctx.prescription());
                    if (dosis == null) { sinDosis++; continue; }
                    var nuevo = conEjercicio(d, i, dosis, ctx, c.div());
                    if (ESTIMADOR.estimateSeconds(nuevo.workouts().get(i), ctx.prescription()) > techo) { superaTecho++; continue; }
                    var rr = problemas(nuevo, ctx, c.div()).stream().filter(x -> !x.startsWith("V20:")).toList();
                    if (rr.isEmpty()) aceptables++;
                    else rechazos.merge(LaboratorioModeloEIT.codigo(rr.get(0)), 1, Integer::sum);
                }
                int seriesPosibles = 0;
                var seriesRech = new TreeMap<String, Integer>();
                for (int j = 0; j < w.exercises().size(); j++) {
                    var e = w.exercises().get(j);
                    if (e.prescriptionType() != PrescriptionType.SETS_REPS || e.plannedSets() == null || e.plannedSets() >= 3) {
                        seriesRech.merge("YA_3_SERIES_O_DURACION", 1, Integer::sum);
                        continue;
                    }
                    var mas = new PlanDraft.DraftExercise(e.exerciseId(), e.prescriptionType(), (short) (e.plannedSets() + 1),
                            e.plannedReps(), e.plannedDurationSeconds(), e.targetLoadKg(), e.restSeconds(), e.notes());
                    var rr = problemas(conReemplazo(d, i, j, mas, ctx, c.div()), ctx, c.div()).stream()
                            .filter(x -> !x.startsWith("V20:")).toList();
                    if (rr.isEmpty()) seriesPosibles++;
                    else seriesRech.merge(LaboratorioModeloEIT.codigo(rr.get(0)), 1, Integer::sum);
                }
                csv.append(String.join(",", id, c.p().objetivo().name(), c.p().nivel().name(), c.p().equipo(),
                        String.valueOf(c.p().dias()), String.valueOf(c.p().minutos()), w.scheduledDate().toString(),
                        w.focus().name(), String.valueOf(w.exercises().size()), String.valueOf(tope), String.valueOf(seg),
                        String.valueOf(minimo), fmt(100.0 * seg / (ctx.effectiveSessionMinutes() * 60.0)),
                        String.valueOf(pool.size()), String.valueOf(enDia), String.valueOf(pegado), String.valueOf(enfoque),
                        String.valueOf(sinDosis), String.valueOf(superaTecho), "\"" + rechazos + "\"", String.valueOf(aceptables),
                        String.valueOf(seriesPosibles), "\"" + seriesRech + "\"", String.valueOf(delEnfoque))).append('\n');
            }
        }
        escribir("DIAGNOSTICO_DISCREPANTES.csv", csv.toString());
    }

    // ============================================================================================ C2-E

    /**
     * Busca otra composicion con el piso del 70 % intacto. Solo actua si V20 es la unica regla incumplida. En cada
     * vuelta prueba, en la primera sesion por debajo del piso, anadir un ejercicio (por id ascendente, dosis base
     * aprobada) y despues subir una serie (hasta 3) a un ejercicio de series y repeticiones; acepta el primer cambio que
     * alarga la sesion sin que el validador vigente encuentre ningun problema distinto de V20. Si ninguna sesion corta
     * admite cambios, la semana esta saturada.
     */
    Resultado mejorar(Resultado c1, PlanGenerationContext ctx, int div) {
        var d = c1.draft();
        if (d == null || c1.valido() || !PisoV20Acotado.soloV20(c1.problemas()))
            return new Resultado("C2E", d, c1.problemas(), 70, ctx, 0, false);
        long minimo = PlanDraftValidator.minimumSessionSeconds(ctx);
        int tope = MvpDosePolicy.aplicaA(ctx.profile().goalType())
                ? MvpDosePolicy.maxColocaciones(ctx.effectiveSessionMinutes()) : CapabilityPrescriptionPolicy.MAX_COLOC_DIA;
        long techo = ctx.profile().maxSessionMinutes() * 60L + 29;
        var porId = porId(ctx);
        var pool = ctx.availableExercises().stream().filter(c -> c.difficulty() <= ctx.effectiveMaxDifficulty())
                .sorted(Comparator.comparing(CandidateExercise::exerciseId)).toList();
        int cambios = 0;
        boolean saturado = false;
        List<String> r = c1.problemas();
        for (int vuelta = 0; vuelta < 80; vuelta++) {
            boolean cambio = false;
            for (int i = 0; i < d.workouts().size() && !cambio; i++) {
                var w = d.workouts().get(i);
                long seg = ESTIMADOR.estimateSeconds(w, ctx.prescription());
                if (seg >= minimo) continue;
                var vecinos = vecinos(d, i);
                if (w.exercises().size() < tope)
                    for (var c : pool) {
                        if (vecinos.contains(c.exerciseId())) continue;
                        if (w.exercises().stream().anyMatch(x -> x.exerciseId().equals(c.exerciseId()))) continue;
                        var clase = CapabilityPrescriptionPolicy.claseDe(c);
                        if ((clase == CapabilityPrescriptionPolicy.Clase.ESTRUCTURAL
                                || clase == CapabilityPrescriptionPolicy.Clase.RESISTIDO)
                                && !(w.focus() == WorkoutFocus.FULL_BODY || w.focus().accepts(c.bodyPartCode()))) continue;
                        var dosis = ConstructorSemanaModeloE.dosisBase(c, ctx.profile().goalType(),
                                ctx.profile().fitnessLevel(), ctx.prescription());
                        if (dosis == null) continue;
                        var nuevo = conEjercicio(d, i, dosis, ctx, div);
                        if (ESTIMADOR.estimateSeconds(nuevo.workouts().get(i), ctx.prescription()) > techo) continue;
                        var rr = problemas(nuevo, ctx, div);
                        if (rr.stream().allMatch(x -> x.startsWith("V20:"))) {
                            d = nuevo;
                            r = rr;
                            cambio = true;
                            break;
                        }
                    }
                if (!cambio)
                    for (int j = 0; j < w.exercises().size() && !cambio; j++) {
                        var e = w.exercises().get(j);
                        if (e.prescriptionType() != PrescriptionType.SETS_REPS || e.plannedSets() == null
                                || e.plannedSets() >= 3) continue;
                        var mas = new PlanDraft.DraftExercise(e.exerciseId(), e.prescriptionType(),
                                (short) (e.plannedSets() + 1), e.plannedReps(), e.plannedDurationSeconds(),
                                e.targetLoadKg(), e.restSeconds(), e.notes());
                        var nuevo = conReemplazo(d, i, j, mas, ctx, div);
                        if (ESTIMADOR.estimateSeconds(nuevo.workouts().get(i), ctx.prescription()) > techo) continue;
                        var rr = problemas(nuevo, ctx, div);
                        if (rr.stream().allMatch(x -> x.startsWith("V20:"))) {
                            d = nuevo;
                            r = rr;
                            cambio = true;
                        }
                    }
            }
            if (!cambio) {
                saturado = !r.isEmpty();
                break;
            }
            cambios++;
            if (r.isEmpty()) break;
        }
        return new Resultado("C2E", d, r, 70, ctx, cambios, saturado);
    }

    PlanDraft conEjercicio(PlanDraft d, int i, PlanDraft.DraftExercise e, PlanGenerationContext ctx, int div) {
        var ws = new ArrayList<>(d.workouts());
        var w = ws.get(i);
        var ex = new ArrayList<>(w.exercises());
        ex.add(e);
        ws.set(i, new PlanDraft.DraftWorkout(w.scheduledDate(), w.focus(), w.name(), 0, ex));
        return rearmar(d, ws, ctx, div);
    }

    PlanDraft conReemplazo(PlanDraft d, int i, int j, PlanDraft.DraftExercise e, PlanGenerationContext ctx, int div) {
        var ws = new ArrayList<>(d.workouts());
        var w = ws.get(i);
        var ex = new ArrayList<>(w.exercises());
        ex.set(j, e);
        ws.set(i, new PlanDraft.DraftWorkout(w.scheduledDate(), w.focus(), w.name(), 0, ex));
        return rearmar(d, ws, ctx, div);
    }

    PlanDraft rearmar(PlanDraft d, List<PlanDraft.DraftWorkout> ws, PlanGenerationContext ctx, int div) {
        var con = ws.stream().map(x -> new PlanDraft.DraftWorkout(x.scheduledDate(), x.focus(), x.name(),
                ESTIMADOR.estimateMinutes(x, ctx.prescription()), x.exercises())).toList();
        var sin = new PlanDraft(d.source(), d.modelName(), d.planName(), null, d.rationale(), con);
        return new PlanDraft(d.source(), d.modelName(), d.planName(), sin.volume(div), d.rationale(), con);
    }

    static Set<Long> vecinos(PlanDraft d, int i) {
        var out = new HashSet<Long>();
        var w0 = d.workouts().get(i);
        for (int j : new int[]{i - 1, i + 1})
            if (j >= 0 && j < d.workouts().size() && Math.abs(d.workouts().get(j).scheduledDate().toEpochDay()
                    - w0.scheduledDate().toEpochDay()) == 1)
                d.workouts().get(j).exercises().forEach(e -> out.add(e.exerciseId()));
        return out;
    }

    // ============================================================================================ utilidades

    List<Perfil> perfiles960() {
        var lista = new ArrayList<Perfil>();
        int n = 0;
        for (var equipo : FactibilidadDuracionIT.EQUIPOS.keySet())
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

    Caso buscar(GoalType o, FitnessLevel n, String eq, int dias, int m, String grupo) {
        return CASOS.values().stream().filter(c -> c.p().objetivo() == o && c.p().nivel() == n && c.p().equipo().equals(eq)
                && c.p().dias() == dias && c.p().minutos() == m && c.p().grupo().equals(grupo)).findFirst().orElse(null);
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
                c.adjustment(), c.previousWeek(), elegibles, c.safety(), c.prescription());
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

    static String clase(Resultado r) {
        if (r.draft() == null) return "SIN_BORRADOR";
        return r.problemas().isEmpty() ? "VALIDO" : "INVALIDO";
    }

    static Map<Long, CandidateExercise> porId(PlanGenerationContext ctx) {
        return ctx.availableExercises().stream().collect(Collectors.toMap(CandidateExercise::exerciseId, c -> c, (a, b) -> a));
    }

    static Set<Long> complementos(PlanGenerationContext ctx) {
        return CatalogEligibilityPolicy.complementos(ctx.availableExercises(), ctx.profile().goalType());
    }

    static Set<String> entrenables(PlanDraft d) {
        var out = new TreeSet<String>();
        for (var w : d.workouts())
            for (var g : GRANDES)
                if (w.focus() == WorkoutFocus.FULL_BODY || w.focus().accepts(g)) out.add(g);
        return out;
    }

    static long segMax(PlanDraft d, PlanGenerationContext ctx) {
        return d.workouts().stream().mapToLong(w -> ESTIMADOR.estimateSeconds(w, ctx.prescription())).max().orElse(0);
    }

    static int pctMinSesion(PlanDraft d, PlanGenerationContext ctx) {
        int m = ctx.effectiveSessionMinutes();
        return (int) Math.floor(d.workouts().stream().mapToLong(w -> ESTIMADOR.estimateSeconds(w, ctx.prescription()))
                .min().orElse(0) * 100.0 / (m * 60.0));
    }

    static int seriesTotales(PlanDraft d, PlanGenerationContext ctx) {
        return VolumenSemanal.medir(d, porId(ctx), complementos(ctx)).porSemana().values().stream().mapToInt(i -> i).sum();
    }

    static String fmt(double x) {
        return String.format(Locale.ROOT, "%.1f", x);
    }

    void fila(StringBuilder csv, Perfil p, long u, Resultado r, PlanDraft c1, long ms) {
        var ctx = r.ctx();
        var d = r.draft();
        int m = ctx.effectiveSessionMinutes();
        var porId = porId(ctx);
        var compl = complementos(ctx);
        long segMin = 0, segMaxV = 0;
        double pctProm = 0, cardioT = 0, cardioB = 0;
        int ejMin = 0, ejMax = 0, total = 0, minGrande = 0, maxSesion = 0, coloComp = 0, repetidos = 0;
        var porGrupo = new TreeMap<String, Integer>();
        var slots = new TreeSet<String>();
        boolean v4h = true, v4t = true, v4s = true, coincide = true;
        if (d != null) {
            segMin = Long.MAX_VALUE;
            ejMin = Integer.MAX_VALUE;
            var usos = new HashMap<Long, Integer>();
            for (var w : d.workouts()) {
                long s = ESTIMADOR.estimateSeconds(w, ctx.prescription());
                segMin = Math.min(segMin, s);
                segMaxV = Math.max(segMaxV, s);
                pctProm += 100.0 * s / (m * 60.0);
                ejMin = Math.min(ejMin, w.exercises().size());
                ejMax = Math.max(ejMax, w.exercises().size());
                v4h &= TechoV4Versionado.HISTORICO.admite(s, m);
                v4t &= TechoV4Versionado.TRUNCADO.admite(s, m);
                v4s &= TechoV4Versionado.SEGUNDOS.admite(s, m);
                coincide &= w.expectedDurationMinutes() == ESTIMADOR.estimateMinutes(w, ctx.prescription());
                for (var e : w.exercises()) {
                    usos.merge(e.exerciseId(), 1, Integer::sum);
                    var c = porId.get(e.exerciseId());
                    if (c == null) continue;
                    if (compl.contains(c.exerciseId())) coloComp++;
                    MandatorySlot.of(c).ifPresent(sl -> slots.add(sl.name()));
                }
            }
            if (d.workouts().isEmpty()) {
                segMin = 0;
                ejMin = 0;
            }
            pctProm /= Math.max(1, d.workouts().size());
            repetidos = usos.values().stream().mapToInt(x -> x - 1).sum();
            var vol = VolumenSemanal.medir(d, porId, compl);
            porGrupo.putAll(vol.porSemana());
            total = vol.porSemana().values().stream().mapToInt(i -> i).sum();
            maxSesion = vol.maximoPorGrupoYSesion();
            minGrande = entrenables(d).stream().mapToInt(vol::semanal).min().orElse(0);
            var met = MetricaAerobica.programada(d, porId);
            cardioT = met.minutosTrabajoProgramados();
            cardioB = met.minutosBloqueProgramados();
        }
        var codigos = r.problemas().stream().map(LaboratorioModeloEIT::codigo).distinct().sorted()
                .collect(Collectors.joining(" "));
        csv.append(String.join(",", p.id(), p.grupo(), String.valueOf(u), p.objetivo().name(), p.nivel().name(),
                p.equipo(), p.restringido() ? "SI" : "NO", String.valueOf(p.dias()),
                String.valueOf(ctx.effectiveDaysPerWeek()), String.valueOf(p.minutos()), r.camino(), clase(r),
                "\"" + codigos + "\"", String.valueOf(r.piso()), String.valueOf(r.saturado()), String.valueOf(r.cambios()),
                String.valueOf(LaboratorioModeloEIT.firma(d).equals(LaboratorioModeloEIT.firma(c1))),
                String.valueOf(segMin), String.valueOf(segMaxV),
                fmt(d == null ? 0 : 100.0 * segMin / (m * 60.0)), fmt(d == null ? 0 : 100.0 * segMaxV / (m * 60.0)),
                fmt(pctProm), String.valueOf(ejMin), String.valueOf(ejMax), String.valueOf(total),
                String.valueOf(porGrupo.getOrDefault("chest", 0)), String.valueOf(porGrupo.getOrDefault("back", 0)),
                String.valueOf(porGrupo.getOrDefault("shoulders", 0)), String.valueOf(porGrupo.getOrDefault("upper legs", 0)),
                String.valueOf(minGrande), String.valueOf(maxSesion), "\"" + String.join(" ", slots) + "\"",
                String.valueOf(coloComp), String.valueOf(repetidos), fmt(cardioT), fmt(cardioB), String.valueOf(v4h),
                String.valueOf(v4t), String.valueOf(v4s), String.valueOf(coincide), String.valueOf(ms))).append('\n');
    }

    static String cabeceraDetalle() {
        return "perfil,camino,resultado,piso_v20_pct,fecha,enfoque,seg_sesion,pct_de_M,orden,exercise_id,nombre,body_part,"
                + "patron,slot,clase,complemento,tipo,series,reps,segundos,descanso_s";
    }

    void detalle(StringBuilder out, String perfil, String camino, Resultado r, PlanGenerationContext ctx) {
        if (r == null || r.draft() == null) return;
        var porId = porId(ctx);
        var compl = complementos(ctx);
        int m = ctx.effectiveSessionMinutes();
        for (var w : r.draft().workouts()) {
            long s = ESTIMADOR.estimateSeconds(w, r.ctx().prescription());
            int k = 0;
            for (var e : w.exercises()) {
                var c = porId.get(e.exerciseId());
                String nombre = c == null ? "?" : c.name().replace(',', ' ').replace('"', '\'');
                out.append(String.join(",", perfil, camino, clase(r), String.valueOf(r.piso()),
                        w.scheduledDate().toString(), w.focus().name(), String.valueOf(s), fmt(100.0 * s / (m * 60.0)),
                        String.valueOf(++k), String.valueOf(e.exerciseId()), "\"" + nombre + "\"",
                        c == null ? "?" : c.bodyPartCode(), c == null || c.movementPattern() == null ? "-" : c.movementPattern(),
                        c == null ? "-" : MandatorySlot.of(c).map(Enum::name).orElse("-"),
                        c == null ? "-" : CapabilityPrescriptionPolicy.claseDe(c).name(),
                        c != null && compl.contains(c.exerciseId()) ? "SI" : "NO", e.prescriptionType().name(),
                        String.valueOf(e.plannedSets()), String.valueOf(e.plannedReps()),
                        String.valueOf(e.plannedDurationSeconds()), String.valueOf(e.restSeconds()))).append('\n');
            }
        }
    }

    long crear(Perfil p) {
        String email = "verif" + (++secuencia) + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Verif', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        boolean libre = !p.restringido();
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 6, 15), BiologicalSex.FEMALE,
                new BigDecimal("168"), new BigDecimal("68"), null, p.nivel(), p.objetivo(), "verificacion",
                "GIMNASIO".equals(p.equipo()) ? TrainingLocation.GYM : TrainingLocation.HOME, (short) p.dias(),
                FactibilidadDuracionIT.DIAS.get(p.dias()), (short) p.minutos(),
                FactibilidadDuracionIT.EQUIPOS.get(p.equipo()), List.of(), "Ninguna", libre, libre, libre, libre, libre));
        return u;
    }

    void escribir(String nombre, String contenido) throws Exception {
        var dir = Boolean.getBoolean("fitsense.laboratorio.capturar")
                ? Path.of("docs/FitSense/cierre_verificacion_politicas") : Path.of("target/verificacion_politicas");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(nombre), contenido);
    }
}
