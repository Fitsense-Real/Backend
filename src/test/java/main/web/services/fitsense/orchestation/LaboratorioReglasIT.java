package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
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
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.planning.interfaces.rest.TrainingPlanController;
import main.web.services.fitsense.planning.interfaces.rest.resources.SkipWorkoutResource;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * LABORATORIO DE REGLAS (investigacion de limites de ejercicios, V20 y 4D). NO es una regresion y NO cambia ninguna
 * regla de produccion: mide alternativas sobre una base PostgreSQL EFIMERA.
 * <ul>
 *   <li><b>A_VIGENTE</b>: motor de reglas y validador de produccion tal cual (V20 70 %, topes 6/7/8).</li>
 *   <li><b>D_PISO_50 / D_SIN_PISO</b>: el MISMO codigo de produccion con el piso de V20 cambiado SOLO en la fila
 *       activa de {@code calculation_configs} de la base efimera (50 % y 0 %). Es el contrafactual mas limpio: no se
 *       toca codigo; los topes de V24 y las dosis siguen igual.</li>
 *   <li><b>B_TOPE_DINAMICO</b>: el borrador vigente, y en los dias bajo el piso de V20 se anaden colocaciones por
 *       encima del tope con la dosis base aprobada, limitadas por: series por grupo y sesion &le; 10, series por grupo
 *       y semana &le; 20, sin repetir ejercicio en el dia ni en el dia pegado, enfoque y V22, techo de V4. Experimental:
 *       solo puede incumplir el tope de V24 (y el presupuesto 4D en GAIN_MUSCLE).</li>
 *   <li><b>C_SIN_TOPE</b>: como B pero sin el limite por grupo y sesion (solo semana y tiempo): mide la redundancia
 *       que aparece si se quita el tope sin control por sesion.</li>
 *   <li><b>E_COMBINADO</b>: el borrador de D_SIN_PISO (tiempo como techo) y, si la semana no llega a un minimo de
 *       volumen por grupo (GAIN_MUSCLE 10 series/semana en los grupos grandes entrenados; resto 4), se anade solo lo
 *       necesario con los limites de B.</li>
 * </ul>
 * Los umbrales del laboratorio (10 por sesion, 20 y 10/4 por semana) son PARAMETROS DE EXPERIMENTO tomados de la
 * revision cientifica (REVISION_CIENTIFICA_REGLAS.md), no reglas propuestas.
 * <p>
 * Opt-in: {@code -Dfitsense.laboratorio=true}; escribe en {@code target/laboratorio/} (o en
 * {@code docs/FitSense/investigacion_reglas_entrenamiento/} con {@code -Dfitsense.laboratorio.capturar=true}).
 * Sin IA (doble apagado, {@code fitsense.ai.enabled=false}); 0 llamadas a Replicate.
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.laboratorio", matches = "true")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Laboratorio · alternativas a los topes de ejercicios, V20 y 4D (base efimera, sin IA)")
class LaboratorioReglasIT {

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
    static final Pattern CODIGO = Pattern.compile("^(V\\d+[A-Z]?)");
    static final Set<String> GRANDES = Set.of("chest", "back", "shoulders", "upper legs");
    static final int SERIES_SESION_GRUPO = 10, SERIES_SEMANA_GRUPO = 20;
    static final String[] VARIANTES = {"A_VIGENTE", "D_PISO_50", "D_SIN_PISO", "B_TOPE_DINAMICO", "C_SIN_TOPE",
            "E_COMBINADO"};
    static int secuencia;

    @BeforeEach
    void iaApagada() {
        when(ia.isEnabled()).thenReturn(false);
        assertTrue(postgres.getJdbcUrl().startsWith("jdbc:postgresql://localhost:"), "solo base efimera");
    }

    /** Cambia el piso de V20 SOLO en la base efimera de esta prueba (fila activa de calculation_configs). */
    void pisoV20(int pct) {
        assertTrue(postgres.getJdbcUrl().startsWith("jdbc:postgresql://localhost:"), "solo base efimera");
        jdbc.update("UPDATE calculation_configs SET params = jsonb_set(params, '{prescription,session_minutes_floor_pct}',"
                + " to_jsonb(?::int), true) WHERE is_active", pct);
    }

    record Perfil(String id, GoalType objetivo, FitnessLevel nivel, String equipo, int dias, int minutos,
                  boolean restringido, String grupo) {}

    record Medida(String resultado, String reglas, int sesiones, double ejProm, int ejMax, long segProm,
                  double pctProm, double pctMin, int seriesSemMinGrande, int seriesSemMax, int seriesSesionMax,
                  int seriesTotales, double cardioMin, int repetidos, int pegadosMismoGrupo, long ms) {}

    // =====================================================================================================
    // 1 · MATRIZ DE EXPERIMENTOS (una semana)
    // =====================================================================================================

    @Test
    @Order(1)
    void matrizExperimentos() throws Exception {
        var perfilesLab = new ArrayList<Perfil>();
        int n = 0;
        for (var equipo : FactibilidadDuracionIT.EQUIPOS.keySet())
            for (int dias : new int[]{1, 2, 3, 4, 6, 7})
                for (var objetivo : GoalType.values())
                    for (var nivel : FitnessLevel.values())
                        for (int m : new int[]{30, 45, 60, 75})
                            perfilesLab.add(new Perfil("L%04d".formatted(++n), objetivo, nivel, equipo, dias, m, false,
                                    "BASE"));
        for (var objetivo : GoalType.values())
            for (var nivel : FitnessLevel.values())
                for (int m : new int[]{30, 45, 60, 75})
                    perfilesLab.add(new Perfil("L%04d".formatted(++n), objetivo, nivel, "GIMNASIO", 3, m, true,
                            "RESTRICCIONES"));
        // Sensibilidad (fuera de P-01, solo laboratorio): 90 y 120 con gimnasio, y los CASO A / NO_DEMOSTRADO de 90.
        for (var objetivo : GoalType.values())
            for (var nivel : FitnessLevel.values())
                for (int m : new int[]{90, 120})
                    perfilesLab.add(new Perfil("L%04d".formatted(++n), objetivo, nivel, "GIMNASIO", 3, m, false,
                            "SENSIBILIDAD"));
        for (var nivel : new FitnessLevel[]{FitnessLevel.INTERMEDIATE, FitnessLevel.ADVANCED})
            for (int dias : new int[]{2, 4, 6})
                for (var equipo : FactibilidadDuracionIT.EQUIPOS.keySet())
                    perfilesLab.add(new Perfil("L%04d".formatted(++n), GoalType.GENERAL_FITNESS, nivel, equipo, dias,
                            90, false, "PENDIENTES_INVESTIGACION_30_120"));

        var usuariosPerfil = new LinkedHashMap<Perfil, Long>();
        for (var p : perfilesLab) usuariosPerfil.put(p, crear(p));

        var csv = new StringBuilder(String.join(",", "perfil", "grupo", "user_id", "objetivo", "nivel", "equipo",
                "restricciones", "dias", "minutos", "variante", "resultado", "reglas_incumplidas", "sesiones",
                "ejercicios_prom", "ejercicios_max", "seg_prom", "pct_minutos_prom", "pct_minutos_min",
                "series_sem_min_grupo_grande", "series_sem_max_grupo", "series_sesion_max_grupo", "series_totales_sem",
                "cardio_min_sem", "ejercicios_repetidos_semana", "dias_pegados_mismo_grupo", "ms_generacion",
                "determinista")).append('\n');
        var borradores = new HashMap<String, PlanDraft>();   // perfil -> borrador vigente / sin piso
        var resumen = new TreeMap<String, Integer>();
        for (int piso : new int[]{70, 50, 0}) {
            pisoV20(piso);
            String variante = piso == 70 ? "A_VIGENTE" : piso == 50 ? "D_PISO_50" : "D_SIN_PISO";
            for (var e : usuariosPerfil.entrySet()) {
                var p = e.getKey();
                var prepared = planComandos.prepare(new GenerateWeeklyPlanCommand(e.getValue(), SEMANA, null, true,
                        null, null));
                var ctx = FactibilidadDuracionIT.conMinutos(prepared.context(), p.minutos());
                long t0 = System.nanoTime();
                PlanDraft d;
                try {
                    d = reglas.generate(ctx, List.of());
                } catch (RuntimeException ex) {
                    d = null;
                }
                long ms = (System.nanoTime() - t0) / 1_000_000;
                String det = "-";
                if (piso == 70 && "BASE".equals(p.grupo()) && "GIMNASIO".equals(p.equipo()) && d != null) {
                    var otra = reglas.generate(ctx, List.of());
                    det = String.valueOf(firma(otra).equals(firma(d)));
                }
                var m = medir(ctx, prepared.divisor(), d, Set.of(), ms);
                if (piso == 70) borradores.put(p.id() + "/A", d);
                if (piso == 0) borradores.put(p.id() + "/D0", d);
                fila(csv, p, e.getValue(), variante, m, det);
                resumen.merge(variante + ":" + m.resultado(), 1, Integer::sum);
            }
        }
        // B, C y E: post-proceso de laboratorio sobre los borradores, validado con el validador VIGENTE (piso 70).
        pisoV20(70);
        for (var e : usuariosPerfil.entrySet()) {
            var p = e.getKey();
            var prepared = planComandos.prepare(new GenerateWeeklyPlanCommand(e.getValue(), SEMANA, null, true, null,
                    null));
            var ctx = FactibilidadDuracionIT.conMinutos(prepared.context(), p.minutos());
            for (var variante : new String[]{"B_TOPE_DINAMICO", "C_SIN_TOPE", "E_COMBINADO"}) {
                var base = borradores.get(p.id() + ("E_COMBINADO".equals(variante) ? "/D0" : "/A"));
                long t0 = System.nanoTime();
                var d = base == null ? null : extender(ctx, prepared.divisor(), base, variante);
                long ms = (System.nanoTime() - t0) / 1_000_000;
                // E no exige V20 (tiempo como techo): se valida como D_SIN_PISO, mas el tope.
                Set<String> cambiadas = switch (variante) {
                    case "E_COMBINADO" -> Set.of("V20", "V24_TOPE", "V24_PRESUPUESTO");
                    default -> Set.of("V24_TOPE", "V24_PRESUPUESTO");
                };
                var m = medir(ctx, prepared.divisor(), d, cambiadas, ms);
                fila(csv, p, e.getValue(), variante, m, "-");
                resumen.merge(variante + ":" + m.resultado(), 1, Integer::sum);
            }
        }
        escribir("MATRIZ_EXPERIMENTOS.csv", csv.toString());
        System.out.println("LABORATORIO_RESUMEN " + perfilesLab.size() + " perfiles " + resumen);
    }

    // =====================================================================================================
    // 2 · SIMULACIONES LONGITUDINALES (camino real del ciclo semanal, 6 semanas)
    // =====================================================================================================

    /**
     * Guion fijo por semana: S1 completa; S2 una sesion omitida por dolor (P-02); S3 dos omisiones por motivacion
     * (baja adherencia); S4 completa con RPE 9 (fatiga); S5 y S6 completas. Se ejecuta con el piso vigente (70) y con
     * el piso a 0 (D_SIN_PISO, solo en esta base efimera).
     */
    @Test
    @Order(2)
    void longitudinal() throws Exception {
        var casos = List.of(
                new Perfil("T1", GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, "GIMNASIO", 3, 60, false, "LONG"),
                new Perfil("T2", GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, "GIMNASIO", 3, 75, false, "LONG"),
                new Perfil("T3", GoalType.INCREASE_STRENGTH, FitnessLevel.INTERMEDIATE, "GIMNASIO", 3, 75, false, "LONG"),
                new Perfil("T4", GoalType.LOSE_WEIGHT, FitnessLevel.INTERMEDIATE, "GIMNASIO", 3, 75, false, "LONG"),
                new Perfil("T5", GoalType.GENERAL_FITNESS, FitnessLevel.INTERMEDIATE, "GIMNASIO", 3, 75, false, "LONG"),
                new Perfil("T6", GoalType.GENERAL_FITNESS, FitnessLevel.BEGINNER, "CASA_MANCUERNAS_BANDAS", 3, 45, false,
                        "LONG"));
        var csv = new StringBuilder("caso,piso_v20,objetivo,nivel,minutos,semana,guion,plan_id,origen,sesiones,"
                + "seg_prom,pct_minutos_prom,series_sem_min_grupo_grande,series_totales_sem,exclusiones_activas,"
                + "ultimo_intento,regeneracion_p03\n");
        for (int piso : new int[]{70, 0}) {
            pisoV20(piso);
            for (var c : casos) {
                long u = crear(c);
                Long plan = null;
                try {
                    plan = planning.generateWeeklyPlan(u, SEMANA, PlanAdjustment.none(), null, null).orElse(null);
                } catch (RuntimeException ex) {
                    plan = null;
                }
                for (int s = 1; s <= 6; s++) {
                    var inicio = SEMANA.plusWeeks(s - 1);
                    String guion = switch (s) {
                        case 2 -> "DOLOR";
                        case 3 -> "BAJA_ADHERENCIA";
                        case 4 -> "FATIGA_RPE9";
                        default -> "COMPLETA";
                    };
                    lineaLongitudinal(csv, c, piso, s, guion, u, plan);
                    if (plan != null) ejecutarGuion(u, plan, guion);
                    Long siguiente;
                    try {
                        siguiente = ciclo.runFor(u, inicio, inicio.plusWeeks(1)).orElse(null);
                    } catch (RuntimeException ex) {
                        siguiente = null;
                    }
                    plan = siguiente;
                }
            }
        }
        pisoV20(70);
        escribir("SIMULACIONES_LONGITUDINALES.csv", csv.toString());
    }

    // =====================================================================================================
    // Medicion
    // =====================================================================================================

    Medida medir(PlanGenerationContext ctx, int divisor, PlanDraft d, Set<String> cambiadas, long ms) {
        if (d == null) return new Medida("SIN_BORRADOR", "-", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, ms);
        var p = ctx.prescription();
        var porId = ctx.availableExercises().stream()
                .collect(Collectors.toMap(CandidateExercise::exerciseId, c -> c, (a, b) -> a));
        var complementos = CatalogEligibilityPolicy.complementos(ctx.availableExercises(), ctx.profile().goalType());
        var problemas = new ArrayList<String>();
        try {
            validador.validate(d, ctx, divisor);
        } catch (InvalidPlanDraftException e) {
            problemas.addAll(e.problems());
        }
        var codigos = new TreeSet<String>();
        boolean soloCambiadas = true;
        for (var pr : problemas) {
            String c = codigo(pr);
            codigos.add(c);
            if (!cambiadas.contains(c)) soloCambiadas = false;
        }
        String resultado = problemas.isEmpty() ? "VALIDO_REGLAS_DE_LA_VARIANTE"
                : soloCambiadas ? "EXPERIMENTAL_SOLO_REGLAS_EN_ESTUDIO" : "INVALIDO";
        int n = d.workouts().size();
        double ej = 0, pct = 0, pctMin = Double.MAX_VALUE, cardio = 0;
        int ejMax = 0, sesMax = 0, total = 0, repetidos = 0, pegados = 0;
        long seg = 0;
        int minutos = ctx.effectiveSessionMinutes();
        var semana = new HashMap<String, Integer>();
        var usados = new HashMap<Long, Integer>();
        Set<String> gruposAyer = Set.of();
        LocalDate ayer = null;
        for (var w : d.workouts()) {
            long s = ESTIMADOR.estimateSeconds(w, p);
            seg += s;
            double pc = 100.0 * s / (minutos * 60.0);
            pct += pc;
            pctMin = Math.min(pctMin, pc);
            ej += w.exercises().size();
            ejMax = Math.max(ejMax, w.exercises().size());
            var sesion = new HashMap<String, Integer>();
            for (var e : w.exercises()) {
                usados.merge(e.exerciseId(), 1, Integer::sum);
                var c = porId.get(e.exerciseId());
                int series = e.plannedSets() == null ? 1 : e.plannedSets();
                if (c != null && CapabilityPrescriptionPolicy.claseDe(c) == CapabilityPrescriptionPolicy.Clase.CARDIO) {
                    cardio += series * (e.plannedDurationSeconds() == null ? 0 : e.plannedDurationSeconds()) / 60.0;
                    continue;
                }
                if (c == null || complementos.contains(c.exerciseId()) || e.prescriptionType() != PrescriptionType.SETS_REPS)
                    continue;
                sesion.merge(c.bodyPartCode(), series, Integer::sum);
                semana.merge(c.bodyPartCode(), series, Integer::sum);
                total += series;
            }
            sesMax = Math.max(sesMax, sesion.values().stream().mapToInt(i -> i).max().orElse(0));
            var grupos = sesion.keySet().stream().filter(GRANDES::contains).collect(Collectors.toSet());
            if (ayer != null && ayer.plusDays(1).equals(w.scheduledDate())) {
                var comun = new HashSet<>(grupos);
                comun.retainAll(gruposAyer);
                pegados += comun.size();
            }
            gruposAyer = grupos;
            ayer = w.scheduledDate();
        }
        for (var v : usados.values()) if (v > 1) repetidos += v - 1;
        int minGrande = GRANDES.stream().mapToInt(g -> semana.getOrDefault(g, 0)).min().orElse(0);
        int maxSem = semana.values().stream().mapToInt(i -> i).max().orElse(0);
        return new Medida(resultado, String.join(" ", codigos), n, n == 0 ? 0 : ej / n, ejMax, n == 0 ? 0 : seg / n,
                n == 0 ? 0 : pct / n, n == 0 ? 0 : pctMin, minGrande, maxSem, sesMax, total, cardio, repetidos, pegados,
                ms);
    }

    /** Codigo de la regla incumplida; V24 se separa en tope de colocaciones y presupuesto de 4D. */
    static String codigo(String problema) {
        var m = CODIGO.matcher(problema.trim());
        String c = m.find() ? m.group(1) : "OTRO";
        if ("V24".equals(c)) {
            if (problema.contains("colocaciones y el maximo")) return "V24_TOPE";
            if (problema.contains("presupuesto")) return "V24_PRESUPUESTO";
        }
        return c;
    }

    // =====================================================================================================
    // Post-proceso de laboratorio (B, C, E)
    // =====================================================================================================

    PlanDraft extender(PlanGenerationContext ctx, int divisor, PlanDraft base, String variante) {
        var p = ctx.prescription();
        String obj = ctx.profile().goalType(), niv = ctx.profile().fitnessLevel();
        long piso = (ctx.effectiveSessionMinutes() * 60L * 70 + 99) / 100;
        int techo = ctx.profile().maxSessionMinutes();
        var porId = ctx.availableExercises().stream()
                .collect(Collectors.toMap(CandidateExercise::exerciseId, c -> c, (a, b) -> a));
        var complementos = CatalogEligibilityPolicy.complementos(ctx.availableExercises(), obj);
        var pool = ctx.availableExercises().stream()
                .filter(c -> !complementos.contains(c.exerciseId()))
                .filter(c -> CapabilityPrescriptionPolicy.claseDe(c) == CapabilityPrescriptionPolicy.Clase.ESTRUCTURAL
                        || CapabilityPrescriptionPolicy.claseDe(c) == CapabilityPrescriptionPolicy.Clase.RESISTIDO)
                .sorted(Comparator.comparing(CandidateExercise::exerciseId)).toList();
        int minimoSemanal = MvpDosePolicy.aplicaA(obj) ? 10 : 4;
        var ws = new ArrayList<>(base.workouts().stream().map(w -> new ArrayList<>(w.exercises())).toList());
        var semana = new HashMap<String, Integer>();
        for (var w : ws) for (var e : w) acumular(semana, porId, complementos, e, 1);
        for (int i = 0; i < ws.size(); i++) {
            var w0 = base.workouts().get(i);
            var dia = ws.get(i);
            var foco = w0.focus();
            var vecinos = new HashSet<Long>();
            for (int j : new int[]{i - 1, i + 1})
                if (j >= 0 && j < ws.size() && Math.abs(base.workouts().get(j).scheduledDate().toEpochDay()
                        - w0.scheduledDate().toEpochDay()) == 1)
                    ws.get(j).forEach(e -> vecinos.add(e.exerciseId()));
            for (int guarda = 0; guarda < 20; guarda++) {
                var draftDia = new PlanDraft.DraftWorkout(w0.scheduledDate(), foco, w0.name(), 0, dia);
                long s = ESTIMADOR.estimateSeconds(draftDia, p);
                boolean falta = switch (variante) {
                    case "E_COMBINADO" -> foco.bodyPartCodes().stream().filter(GRANDES::contains)
                            .anyMatch(g -> semana.getOrDefault(g, 0) < minimoSemanal);
                    default -> s < piso;
                };
                if (!falta) break;
                var sesion = new HashMap<String, Integer>();
                for (var e : dia) acumular(sesion, porId, complementos, e, 1);
                var ids = dia.stream().map(PlanDraft.DraftExercise::exerciseId).collect(Collectors.toSet());
                PlanDraft.DraftExercise elegido = null;
                // Grupo a reforzar: el grande del enfoque con menos series en la semana (B, E); en C, cualquiera.
                var grupos = foco.bodyPartCodes().stream()
                        .sorted(Comparator.comparingInt((String g) -> semana.getOrDefault(g, 0))
                                .thenComparing(g -> g)).toList();
                buscar:
                for (var g : grupos) {
                    for (var c : pool) {
                        if (!g.equals(c.bodyPartCode()) || ids.contains(c.exerciseId())
                                || vecinos.contains(c.exerciseId())) continue;
                        if (!v22(foco, c)) continue;
                        var vs = FactibilidadDuracionIT.variantes(c, obj, niv, foco, p);
                        if (vs.isEmpty()) continue;
                        var v = vs.get(0);
                        int series = v.plannedSets() == null ? 1 : v.plannedSets();
                        if (!"C_SIN_TOPE".equals(variante) && sesion.getOrDefault(g, 0) + series > SERIES_SESION_GRUPO)
                            continue;
                        if (semana.getOrDefault(g, 0) + series > SERIES_SEMANA_GRUPO) continue;
                        var con = new ArrayList<>(dia);
                        con.add(v);
                        if (ESTIMADOR.estimateMinutes(new PlanDraft.DraftWorkout(w0.scheduledDate(), foco, w0.name(), 0,
                                con), p) > techo) continue;
                        elegido = v;
                        break buscar;
                    }
                }
                if (elegido == null) break;
                dia.add(elegido);
                acumular(semana, porId, complementos, elegido, 1);
            }
        }
        var workouts = new ArrayList<PlanDraft.DraftWorkout>();
        for (int i = 0; i < ws.size(); i++) {
            var w0 = base.workouts().get(i);
            workouts.add(new PlanDraft.DraftWorkout(w0.scheduledDate(), w0.focus(), w0.name(), 0, ws.get(i)));
        }
        return FactibilidadDuracionIT.recalcular(new PlanDraft(base.source(), base.modelName(), base.planName(), null,
                base.rationale(), workouts), p, divisor);
    }

    static void acumular(Map<String, Integer> m, Map<Long, CandidateExercise> porId, Set<Long> complementos,
                         PlanDraft.DraftExercise e, int signo) {
        var c = porId.get(e.exerciseId());
        if (c == null || complementos.contains(c.exerciseId()) || e.prescriptionType() != PrescriptionType.SETS_REPS)
            return;
        if (CapabilityPrescriptionPolicy.claseDe(c) == CapabilityPrescriptionPolicy.Clase.CARDIO) return;
        m.merge(c.bodyPartCode(), signo * (e.plannedSets() == null ? 1 : e.plannedSets()), Integer::sum);
    }

    static boolean v22(WorkoutFocus foco, CandidateExercise c) {
        if (!"upper arms".equals(c.bodyPartCode()) || c.targetMuscle() == null) return true;
        String m = c.targetMuscle().toLowerCase(Locale.ROOT);
        if (foco == WorkoutFocus.PUSH) return !m.contains("biceps");
        if (foco == WorkoutFocus.PULL) return !m.contains("triceps");
        return true;
    }

    static String firma(PlanDraft d) {
        return d == null ? "null" : d.workouts().stream().map(w -> w.scheduledDate() + ":" + w.exercises().stream()
                .map(e -> e.exerciseId() + "x" + e.plannedSets() + "x" + e.plannedReps() + "x"
                        + e.plannedDurationSeconds()).collect(Collectors.joining(","))).collect(Collectors.joining("|"));
    }

    // =====================================================================================================
    // Longitudinal: ejecucion del guion y registro
    // =====================================================================================================

    void ejecutarGuion(long u, long plan, String guion) {
        var ws = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                + " ORDER BY scheduled_date", Long.class, plan);
        for (int k = 0; k < ws.size(); k++) {
            long w = ws.get(k);
            switch (guion) {
                case "DOLOR" -> {
                    if (k == 0) planes.skip(UserDetailsImpl.build(usuarios.findById(u).orElseThrow()), w,
                            new SkipWorkoutResource(SkipReason.PAIN_OR_DISCOMFORT));
                    else hacer(u, w, (short) 6);
                }
                case "BAJA_ADHERENCIA" -> {
                    if (k < 2) planes.skip(UserDetailsImpl.build(usuarios.findById(u).orElseThrow()), w,
                            new SkipWorkoutResource(SkipReason.LACK_OF_MOTIVATION));
                    else hacer(u, w, (short) 6);
                }
                case "FATIGA_RPE9" -> hacer(u, w, (short) 9);
                default -> hacer(u, w, (short) 6);
            }
        }
    }

    void hacer(long u, long plannedWorkoutId, short rpe) {
        long sesion = sesiones.handle(new StartWorkoutSessionCommand(u, plannedWorkoutId)).orElseThrow().getId();
        for (var f : jdbc.queryForList("SELECT planned_exercise_id, prescription_type, planned_sets, planned_reps,"
                + " planned_duration_seconds FROM planned_workout_exercises WHERE planned_workout_id = ?"
                + " ORDER BY exercise_order", plannedWorkoutId)) {
            long pe = ((Number) f.get("planned_exercise_id")).longValue();
            int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
            boolean duracion = "DURATION".equals(f.get("prescription_type"));
            int reps = f.get("planned_reps") == null ? 0 : ((Number) f.get("planned_reps")).intValue();
            int segundos = f.get("planned_duration_seconds") == null ? 0
                    : ((Number) f.get("planned_duration_seconds")).intValue();
            sesiones.handle(new RecordSessionExerciseCommand(u, sesion, pe, (short) series,
                    duracion ? null : series * reps, duracion ? series * segundos : null, null, null));
        }
        sesiones.handle(new FinishWorkoutSessionCommand(u, sesion, rpe, (short) 4, (short) 45));
    }

    void lineaLongitudinal(StringBuilder csv, Perfil c, int piso, int s, String guion, long u, Long plan) {
        String origen = "-", ultimo = "-", regen = "-";
        int sesionesN = 0, minGrande = 0, total = 0;
        double segProm = 0, pct = 0;
        if (plan != null) {
            origen = jdbc.queryForObject("SELECT generation_source FROM weekly_training_plans WHERE plan_id = ?",
                    String.class, plan);
            var filas = jdbc.queryForList("SELECT pw.planned_workout_id, pw.expected_duration_minutes FROM"
                    + " planned_workouts pw WHERE pw.plan_id = ?", plan);
            sesionesN = filas.size();
            segProm = filas.stream().mapToInt(f -> ((Number) f.get("expected_duration_minutes")).intValue())
                    .average().orElse(0) * 60;
            pct = 100.0 * segProm / (c.minutos() * 60.0);
            var series = jdbc.queryForList("SELECT bp.code AS g, SUM(COALESCE(pwe.planned_sets, 1)) AS s FROM"
                    + " planned_workout_exercises pwe JOIN planned_workouts pw ON pw.planned_workout_id ="
                    + " pwe.planned_workout_id JOIN exercises e ON e.exercise_id = pwe.exercise_id JOIN body_parts bp"
                    + " ON bp.body_part_id = e.body_part_id WHERE pw.plan_id = ? AND pwe.prescription_type ="
                    + " 'SETS_REPS' GROUP BY bp.code", plan);
            var m = new HashMap<String, Integer>();
            for (var f : series) m.put((String) f.get("g"), ((Number) f.get("s")).intValue());
            minGrande = GRANDES.stream().mapToInt(g -> m.getOrDefault(g, 0)).min().orElse(0);
            total = m.values().stream().mapToInt(i -> i).sum();
        }
        var intentos = jdbc.queryForList("SELECT status FROM plan_adaptation_attempts WHERE user_id = ? ORDER BY"
                + " attempt_id DESC LIMIT 1", String.class, u);
        if (!intentos.isEmpty()) ultimo = intentos.get(0);
        var r = jdbc.queryForList("SELECT outcome FROM safe_regeneration_attempts WHERE user_id = ? ORDER BY"
                + " regeneration_id DESC LIMIT 1", String.class, u);
        if (!r.isEmpty()) regen = r.get(0);
        int excl = jdbc.queryForObject("SELECT count(*) FROM user_safety_exclusions WHERE user_id = ? AND"
                + " status = 'ACTIVA'", Integer.class, u);
        csv.append(String.join(",", c.id(), String.valueOf(piso), c.objetivo().name(), c.nivel().name(),
                String.valueOf(c.minutos()), "S" + s, guion, plan == null ? "SIN_PLAN" : String.valueOf(plan), origen,
                String.valueOf(sesionesN), String.format(Locale.ROOT, "%.0f", segProm),
                String.format(Locale.ROOT, "%.1f", pct), String.valueOf(minGrande), String.valueOf(total),
                String.valueOf(excl), ultimo, regen)).append('\n');
    }

    // =====================================================================================================

    long crear(Perfil p) {
        String email = "laboratorio" + (++secuencia) + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Laboratorio', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        boolean libre = !p.restringido();
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 6, 15), BiologicalSex.FEMALE,
                new BigDecimal("168"), new BigDecimal("68"), null, p.nivel(), p.objetivo(), "laboratorio",
                "GIMNASIO".equals(p.equipo()) ? TrainingLocation.GYM : TrainingLocation.HOME, (short) p.dias(),
                FactibilidadDuracionIT.DIAS.get(p.dias()), FactibilidadDuracionIT.admitido(p.minutos()),
                FactibilidadDuracionIT.EQUIPOS.get(p.equipo()), List.of(), "Ninguna", libre, libre, libre, libre,
                libre));
        return u;
    }

    void fila(StringBuilder csv, Perfil p, long u, String variante, Medida m, String det) {
        csv.append(String.join(",", p.id(), p.grupo(), String.valueOf(u), p.objetivo().name(), p.nivel().name(),
                p.equipo(), p.restringido() ? "SI" : "NO", String.valueOf(p.dias()), String.valueOf(p.minutos()),
                variante, m.resultado(), "\"" + m.reglas() + "\"", String.valueOf(m.sesiones()),
                String.format(Locale.ROOT, "%.2f", m.ejProm()), String.valueOf(m.ejMax()), String.valueOf(m.segProm()),
                String.format(Locale.ROOT, "%.1f", m.pctProm()), String.format(Locale.ROOT, "%.1f", m.pctMin()),
                String.valueOf(m.seriesSemMinGrande()), String.valueOf(m.seriesSemMax()),
                String.valueOf(m.seriesSesionMax()), String.valueOf(m.seriesTotales()),
                String.format(Locale.ROOT, "%.1f", m.cardioMin()), String.valueOf(m.repetidos()),
                String.valueOf(m.pegadosMismoGrupo()), String.valueOf(m.ms()), det)).append('\n');
    }

    void escribir(String nombre, String contenido) throws Exception {
        var dir = Boolean.getBoolean("fitsense.laboratorio.capturar")
                ? Path.of("docs/FitSense/investigacion_reglas_entrenamiento") : Path.of("target/laboratorio");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(nombre), contenido);
    }
}
