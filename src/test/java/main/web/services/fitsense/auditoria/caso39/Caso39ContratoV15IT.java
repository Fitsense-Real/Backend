package main.web.services.fitsense.auditoria.caso39;

import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.planning.domain.services.CatalogEligibilityPolicy;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.TrainingPlanGenerator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.generation.ai.PlanInputSnapshot;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AUDITORIA CASO 39 · H2 · CONTRATO PROMPT-V15. SOLO INVESTIGACION: no cambia el producto ni llama a Replicate.
 * <p>
 * 1. {@code sondaV15}: mide con el VALIDADOR REAL, sobre el contexto real del usuario 39, cuantos ejercicios de un
 *    mismo grupo admite V15 en una sesion segun el numero de ejercicios de trabajo (complementos aparte). Es la
 *    referencia contra la que se compara lo que el prompt comunica.
 * 2. {@code rejilla}: para una rejilla de perfiles (4 objetivos x 3 niveles x dias x minutos x lugar) construye la
 *    instantanea REAL que recibe la IA y cuenta las contradicciones del contrato actual (min > max, tope comunicado
 *    por encima del que V15 admite, regla aplicable sin tope comunicado) y las del diseno propuesto (tabla por
 *    numero de ejercicios de trabajo, calculada con la misma definicion que V15).
 * <p>
 * Opt-in: {@code -Dfitsense.auditoria.caso39.contrato=true}; la politica con {@code -Dfitsense.planning.policy}.
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.auditoria.caso39.contrato", matches = "true")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false"
})
@DisplayName("Auditoria caso 39 · H2 · contrato prompt-V15 (sin Replicate)")
class Caso39ContratoV15IT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanDraftValidator validador;
    @Autowired @Qualifier("ruleBasedTrainingPlanGenerator") TrainingPlanGenerator reglas;
    @Autowired JdbcTemplate jdbc;

    static final LocalDate SEMANA = LocalDate.of(2026, 11, 16);
    static final AtomicInteger SEQ = new AtomicInteger(1000);
    static final Path SALIDA = Path.of(System.getProperty("fitsense.auditoria.caso39.salida", "target/caso39-contrato"));
    static final String POLITICA = System.getProperty("fitsense.planning.policy", "PLAN-REV-2.0");

    static final List<String> EQUIPO_39 = List.of("assisted", "band", "barbell", "bench", "body weight", "cable",
            "captains chair", "dumbbell", "ez barbell", "ghd bench", "gymnastic rings", "hyperextension bench",
            "landmine", "leverage machine", "olympic barbell", "other", "parallel bars", "preacher bench",
            "pull-up bar", "rope", "sled machine", "smith machine", "squat rack", "stationary bike", "step platform",
            "suspension straps", "treadmill", "vertical pole", "weighted");
    static final List<String> CASA = List.of("body weight", "dumbbell", "band");

    /** El tope que V15 aplica (PlanDraftValidator.validateFocusCoverage): un grupo no puede pasar de n/2. */
    static Integer topeV15(WorkoutFocus foco, int nTrabajo) {
        if (foco.bodyPartCodes().size() < 3 || nTrabajo < 4) return null;   // la regla de la mitad no aplica
        return nTrabajo / 2;
    }

    // ---------------------------------------------------------------------------------------------------------
    @Test
    @DisplayName("sonda: el validador real admite floor(n/2) del mismo grupo con n ejercicios de trabajo")
    void sondaV15() throws Exception {
        Files.createDirectories(SALIDA);
        long u = usuario(39L, GoalType.GENERAL_FITNESS, FitnessLevel.BEGINNER, TrainingLocation.GYM, EQUIPO_39,
                (short) 4, List.of((short) 1, (short) 4, (short) 5, (short) 7), (short) 60);
        var ctx = planComandos.prepare(GenerateWeeklyPlanCommand.firstPlan(u, SEMANA)).context();
        var base = reglas.generate(ctx, List.of());
        var compl = CatalogEligibilityPolicy.complementos(ctx.availableExercises(), ctx.profile().goalType());
        var porGrupo = ctx.availableExercises().stream().filter(c -> !compl.contains(c.exerciseId()))
                .filter(c -> c.defaultPrescription() == PrescriptionType.SETS_REPS)
                .collect(Collectors.groupingBy(CandidateExercise::bodyPartCode, TreeMap::new, Collectors.toList()));
        var cardio = ctx.availableExercises().stream().filter(c -> compl.contains(c.exerciseId())
                && "cardio".equals(c.bodyPartCode())).findFirst().orElseThrow();
        var movilidad = ctx.availableExercises().stream().filter(c -> compl.contains(c.exerciseId())
                && !"cardio".equals(c.bodyPartCode())).findFirst().orElseThrow();

        var sb = new StringBuilder("politica;fecha;enfoque;n_trabajo;complementos;mismo_grupo;grupo;v15_mitad_rechaza;tope_funcion\n");
        for (int idx = 0; idx < base.workouts().size(); idx++) {
            var w = base.workouts().get(idx);
            var grupos = w.focus().bodyPartCodes();
            String dominante = grupos.get(0);
            for (int n = 2; n <= 6; n++) {
                for (int k = 1; k <= n; k++) {
                    var ej = new ArrayList<PlanDraft.DraftExercise>();
                    var usados = new HashSet<Long>();
                    for (int i = 0; i < k; i++) ej.add(serie(siguiente(porGrupo.get(dominante), usados)));
                    int g = 1;
                    while (ej.size() < n) {
                        var otro = grupos.get(g % grupos.size()); g++;
                        if (otro.equals(dominante)) continue;
                        var c = siguienteOpcional(porGrupo.getOrDefault(otro, List.of()), usados);
                        if (c != null) ej.add(serie(c));
                    }
                    ej.add(duracion(cardio, 90));
                    ej.add(duracion(movilidad, 30));
                    var semana = new ArrayList<>(base.workouts());
                    semana.set(idx, new PlanDraft.DraftWorkout(w.scheduledDate(), w.focus(), w.name(),
                            w.expectedDurationMinutes(), ej));
                    var problemas = problemas(new PlanDraft(base.source(), null, base.planName(), null,
                            base.rationale(), semana), ctx);
                    boolean rechaza = problemas.stream().anyMatch(p -> p.startsWith("V15: el entrenamiento del "
                            + w.scheduledDate()) && p.contains(" dedica "));
                    sb.append(POLITICA).append(';').append(w.scheduledDate()).append(';').append(w.focus()).append(';')
                            .append(n).append(";2;").append(k).append(';').append(dominante).append(';')
                            .append(rechaza).append(';').append(topeV15(w.focus(), n)).append('\n');
                    Integer tope = topeV15(w.focus(), n);
                    assertEquals(tope != null && k > tope, rechaza,
                            "la funcion debe predecir exactamente a V15: " + w.focus() + " n=" + n + " k=" + k);
                }
            }
        }
        Files.writeString(SALIDA.resolve("sonda_v15_" + POLITICA + ".csv"), sb.toString(), StandardCharsets.UTF_8);
    }

    // ---------------------------------------------------------------------------------------------------------
    @Test
    @DisplayName("rejilla: contradicciones del contrato actual frente al diseno propuesto")
    void rejilla() throws Exception {
        Files.createDirectories(SALIDA);
        var sb = new StringBuilder("politica;objetivo;nivel;lugar;dias;minutos;fecha;enfoque;max_coloc_dia;"
                + "complementos_obligatorios;n_trabajo_max;min_ex_actual;tope_actual;tope_v15_en_n_max;"
                + "tope_v15_en_n4;C1_min_mayor_max;C2_tope_permite_rechazo;C3_regla_sin_tope;tabla_propuesta;"
                + "propuesta_contradice\n");
        var resumen = new TreeMap<String, Integer>();
        var objetivos = List.of(GoalType.GENERAL_FITNESS, GoalType.LOSE_WEIGHT, GoalType.GAIN_MUSCLE,
                GoalType.INCREASE_STRENGTH);
        int perfilesRechazados = 0;
        for (var objetivo : objetivos)
            for (var nivel : FitnessLevel.values())
                for (var lugar : List.of(TrainingLocation.GYM, TrainingLocation.HOME))
                    for (short dias : new short[]{2, 3, 4, 5})
                        for (short minutos : new short[]{30, 45, 60, 75}) {
                            var diasDisp = List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6,
                                    (short) 7).subList(0, Math.max(dias, 2) + (dias >= 4 ? 2 : 1));
                            long u;
                            PlanGenerationContext ctx;
                            try {
                                u = usuario(null, objetivo, nivel, lugar, lugar == TrainingLocation.GYM ? EQUIPO_39 : CASA,
                                        dias, diasDisp, minutos);
                                ctx = planComandos.prepare(GenerateWeeklyPlanCommand.firstPlan(u, SEMANA)).context();
                            } catch (RuntimeException e) {
                                perfilesRechazados++;
                                continue;
                            }
                            var snap = PlanInputSnapshot.of(ctx).constraints();
                            Integer maxColoc = snap.maxColocacionesDia();
                            Integer minEx = snap.minExercisesPerSession();
                            int k = complementosObligatorios(snap);
                            resumen.merge("perfiles", 1, Integer::sum);
                            for (var s : snap.suggestedSplit()) {
                                var foco = WorkoutFocus.valueOf(s.focusCode());
                                // Sin tope declarado (GAIN_MUSCLE fuera de 4D) se usa 8, el de V24 por capacidades.
                                int maxDia = maxColoc == null ? 8 : maxColoc;
                                int nMax = Math.max(0, maxDia - k);
                                Integer topeMax = topeV15(foco, nMax);
                                Integer tope4 = topeV15(foco, 4);
                                boolean c1 = minEx != null && maxColoc != null && minEx > maxColoc;
                                boolean c2 = s.maxPerBodyPart() != null && topeMax != null && s.maxPerBodyPart() > topeMax;
                                boolean c3 = s.maxPerBodyPart() == null && topeMax != null;
                                // Diseno: tabla n -> floor(n/2) para n = 4..maxDia, misma funcion que V15
                                var tabla = new TreeMap<Integer, Integer>();
                                for (int n = 4; n <= maxDia; n++) if (topeV15(foco, n) != null) tabla.put(n, topeV15(foco, n));
                                boolean propuestaContradice = tabla.entrySet().stream()
                                        .anyMatch(e -> !Objects.equals(e.getValue(), topeV15(foco, e.getKey())));
                                resumen.merge("sesiones", 1, Integer::sum);
                                if (c1) resumen.merge("C1_min_mayor_max", 1, Integer::sum);
                                if (c2) resumen.merge("C2_tope_permite_rechazo", 1, Integer::sum);
                                if (c3) resumen.merge("C3_regla_sin_tope", 1, Integer::sum);
                                if (topeMax != null) resumen.merge("sesiones_con_regla_mitad", 1, Integer::sum);
                                if (propuestaContradice) resumen.merge("propuesta_contradice", 1, Integer::sum);
                                sb.append(POLITICA).append(';').append(objetivo).append(';').append(nivel).append(';')
                                        .append(lugar).append(';').append(dias).append(';').append(minutos).append(';')
                                        .append(s.scheduledDate()).append(';').append(foco).append(';').append(maxColoc)
                                        .append(';').append(k).append(';').append(nMax).append(';').append(minEx)
                                        .append(';').append(s.maxPerBodyPart()).append(';').append(topeMax).append(';')
                                        .append(tope4).append(';').append(c1).append(';').append(c2).append(';')
                                        .append(c3).append(';').append(tabla).append(';').append(propuestaContradice)
                                        .append('\n');
                            }
                        }
        resumen.put("perfiles_no_generables_P01", perfilesRechazados);
        Files.writeString(SALIDA.resolve("contrato_v15_rejilla_" + POLITICA + ".csv"), sb.toString(), StandardCharsets.UTF_8);
        Files.writeString(SALIDA.resolve("contrato_v15_resumen_" + POLITICA + ".txt"),
                resumen.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining("\n")),
                StandardCharsets.UTF_8);
        assertEquals(0, resumen.getOrDefault("propuesta_contradice", 0));
    }

    // ---------------------------------------------------------------------------------------------------------
    @Test
    @DisplayName("catalogo activo: nombres, instrucciones y banderas (para buscar contradicciones texto-bandera)")
    void catalogoBanderasTexto() throws Exception {
        Files.createDirectories(SALIDA);
        var filas = jdbc.queryForList("""
                SELECT e.exercise_id, e.source_code, e.name_en, e.name_es, bp.code AS body_part, eq.code AS equipment,
                       e.movement_pattern, e.difficulty_level, e.requires_hanging, e.requires_overhead,
                       e.requires_floor, e.requires_kneeling, e.requires_inversion, e.high_impact,
                       replace(replace(coalesce(e.instructions_es,''), ';', ','), E'\n', ' ') AS instructions_es
                  FROM exercises e JOIN body_parts bp ON bp.body_part_id = e.body_part_id
                  JOIN equipment_types eq ON eq.equipment_id = e.equipment_id
                 WHERE e.is_active ORDER BY e.exercise_id""");
        Files.writeString(SALIDA.resolve("catalogo_activo_banderas_texto.csv"), Caso39ReproduccionIT.csv(filas),
                StandardCharsets.UTF_8);
        assertFalse(filas.isEmpty());
    }

    /** Principales que el prompt obliga a poner en CADA sesion y que solo se cubren con complementos. */
    static int complementosObligatorios(PlanInputSnapshot.Constraints c) {
        if (c.capacidades() == null || c.capacidades().principalesFactibles() == null) return 0;
        return (int) c.capacidades().principalesFactibles().stream()
                .filter(p -> p.equals("CARDIO") || p.equals("MOBILITY")).count();
    }

    // ---------------------------------------------------------------------------------------------------------

    private long usuario(Long id, GoalType objetivo, FitnessLevel nivel, TrainingLocation lugar, List<String> equipo,
                         short dias, List<Short> disponibles, short minutos) {
        long u = id != null ? id : SEQ.incrementAndGet();
        jdbc.update("INSERT INTO users (user_id, email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " OVERRIDING SYSTEM VALUE VALUES (?, ?, 'x', 'Contrato', now(), now())", u, "c" + u + "@pruebas.invalid");
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1985, 6, 1), BiologicalSex.MALE,
                new BigDecimal("180"), new BigDecimal("100"), new BigDecimal("80"), nivel, objetivo, "Contrato",
                lugar, dias, disponibles, minutos, equipo, List.of(), null, true, true, true, false, false));
        return u;
    }

    private List<String> problemas(PlanDraft d, PlanGenerationContext ctx) {
        try {
            validador.validate(d, ctx, 30);
            return List.of();
        } catch (InvalidPlanDraftException e) {
            return e.problems();
        }
    }

    private static CandidateExercise siguiente(List<CandidateExercise> pool, Set<Long> usados) {
        var c = siguienteOpcional(pool, usados);
        if (c == null) throw new IllegalStateException("sin candidatos suficientes");
        return c;
    }

    private static CandidateExercise siguienteOpcional(List<CandidateExercise> pool, Set<Long> usados) {
        for (var c : pool) if (usados.add(c.exerciseId())) return c;
        return null;
    }

    private static PlanDraft.DraftExercise serie(CandidateExercise c) {
        return new PlanDraft.DraftExercise(c.exerciseId(), PrescriptionType.SETS_REPS, (short) 3, (short) 10, null,
                null, (short) 90, null);
    }

    private static PlanDraft.DraftExercise duracion(CandidateExercise c, int segundos) {
        return new PlanDraft.DraftExercise(c.exerciseId(), PrescriptionType.DURATION, (short) 2, null, segundos,
                null, (short) 30, null);
    }
}
