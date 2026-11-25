package main.web.services.fitsense.planning.infrastructure.generation.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalConfigurationService;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.planning.domain.services.ContratoReparto;
import main.web.services.fitsense.planning.domain.services.DosisSemanalPolicy;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.generation.rules.RuleBasedTrainingPlanGenerator;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
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

/**
 * H2-v2 (PR-2.0) · EL CONTRATO DEL PROMPT ES EL DEL VALIDADOR, sin IA y sin coste, con el catalogo real y PLAN-REV-2.0,
 * sobre los 26 perfiles de la evaluacion de H2 (los 24 congelados de la PR #69 y los dos de GAIN_MUSCLE de 5-6 dias):
 * <ol>
 *   <li>PR-1.7 no cambia: la instantanea no trae ningun campo de H2-v2 y el prompt ninguna frase nueva;</li>
 *   <li>PR-2.0 comunica exactamente lo que verifica el backend: la tabla de la mitad de V15, los grupos con material,
 *   los techos de V26 por nivel, y sin las frases que contradicen a PLAN-REV-2.0 (+15 %, llenar tiempo);</li>
 *   <li>las cuotas NO son imposibles: la rutina valida del motor de reglas las cumple en cada sesion;</li>
 *   <li>las cuotas son EXACTAS: pasar una por una unidad produce justo el rechazo V15 o V26 correspondiente.</li>
 * </ol>
 * No cambia V15, V20, V24 ni V26.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("H2-v2 (PR-2.0): el reparto comunicado a la IA es exactamente el que verifican V15 y V26 (26 perfiles)")
class ContratoH2v2IT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired RuleBasedTrainingPlanGenerator reglas;
    @Autowired PlanDraftValidator validador;
    @Autowired ExternalConfigurationService configuracion;
    @Autowired UserProfileCommandService perfiles;
    @Autowired JsonSupport json;
    @Autowired JdbcTemplate jdbc;

    static final LocalDate SEMANA = LocalDate.of(2026, 11, 23);
    static final ObjectMapper MAPPER = new ObjectMapper();
    static final List<String> GIMNASIO = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine",
            "bench", "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine", "pull-up bar");
    static final List<String> CASA = List.of("body weight", "dumbbell", "band");

    record Caso(String id, GoalType objetivo, FitnessLevel nivel, TrainingLocation lugar, List<Short> dias,
                short minutos, boolean restringido) {}

    /** Los mismos casos que H2PrimeraPropuestaIT / EvaluacionPareadaH2IT (sin cambiar los perfiles). */
    static List<Caso> casos() {
        var l = new ArrayList<Caso>();
        int n = 1;
        for (var o : List.of(GoalType.GAIN_MUSCLE, GoalType.LOSE_WEIGHT, GoalType.INCREASE_STRENGTH,
                GoalType.GENERAL_FITNESS)) {
            l.add(new Caso(id(n++), o, FitnessLevel.BEGINNER, TrainingLocation.GYM, dias(1, 3, 5), (short) 45, false));
            l.add(new Caso(id(n++), o, FitnessLevel.BEGINNER, TrainingLocation.HOME, dias(1, 2, 4, 5), (short) 60, false));
            l.add(new Caso(id(n++), o, FitnessLevel.INTERMEDIATE, TrainingLocation.GYM, dias(1, 2, 4, 5), (short) 60, false));
            l.add(new Caso(id(n++), o, FitnessLevel.INTERMEDIATE, TrainingLocation.HOME, dias(1, 3, 5), (short) 45, false));
            l.add(new Caso(id(n++), o, FitnessLevel.INTERMEDIATE, TrainingLocation.GYM, dias(1, 2, 3, 5, 6), (short) 60, true));
            l.add(new Caso(id(n++), o, FitnessLevel.BEGINNER, TrainingLocation.GYM, dias(1, 4, 5, 7), (short) 60, false));
        }
        l.add(new Caso("G25", GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, TrainingLocation.GYM,
                dias(1, 2, 3, 4, 5, 6), (short) 60, false));
        l.add(new Caso("G26", GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, TrainingLocation.HOME,
                dias(1, 2, 3, 5, 6), (short) 60, false));
        return l;
    }

    private static String id(int n) { return "G%02d".formatted(n); }

    private static List<Short> dias(int... d) { return Arrays.stream(d).mapToObj(x -> (short) x).toList(); }

    long participante(Caso c) {
        String email = ("h2v2-" + c.id() + "-" + UUID.randomUUID() + "@pruebas.invalid").toLowerCase();
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Sintetico', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        boolean gym = c.lugar() == TrainingLocation.GYM;
        boolean r = c.restringido();
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 3, 10), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("72"), null, c.nivel(), c.objetivo(), "evaluacion pareada H2",
                c.lugar(), (short) c.dias().size(), c.dias(), c.minutos(), gym ? GIMNASIO : CASA, List.of(),
                r ? "Restricciones declaradas" : "Ninguna", !r, true, !r, !r, !r));
        return u;
    }

    static PlanPromptBuilder builder(JsonSupport json, String version) {
        var b = new PlanPromptBuilder(json);
        b.setPromptVersion(version);
        return b;
    }

    /** Lo que se acumula de todos los casos para comprobar la cobertura al final. */
    static final List<String> FILAS = Collections.synchronizedList(new ArrayList<>());
    static int mutacionesV15, mutacionesV26, planesReglas;

    @TestFactory
    @DisplayName("por perfil: PR-1.7 intacto, PR-2.0 exacto, cuotas factibles y exactas")
    Collection<DynamicTest> porPerfil() {
        return casos().stream().map(c -> DynamicTest.dynamicTest(c.id() + " " + c.objetivo() + " " + c.nivel() + " "
                + c.lugar() + " " + c.dias().size() + "d " + c.minutos() + "min", () -> caso(c))).toList();
    }

    void caso(Caso c) throws Exception {
        long u = participante(c);
        var ctx = planComandos.prepare(GenerateWeeklyPlanCommand.firstPlan(u, SEMANA)).context();
        assertEquals("PLAN-REV-2.0", ctx.policy().id(), "la politica por omision");

        // 1. PR-1.7 intacto.
        var s17 = MAPPER.readTree(json.write(PlanInputSnapshot.of(ctx, false)));
        assertEquals(json.write(PlanInputSnapshot.of(ctx)), json.write(PlanInputSnapshot.of(ctx, false)));
        var texto17 = s17.toString();
        for (var campo : List.of("complementos_no_cuentan", "volumen_semanal", "max_per_body_part_por_n_trabajo",
                "body_parts_con_material"))
            assertFalse(texto17.contains(campo), "PR-1.7 no trae " + campo);
        assertEquals("PR-1.7", s17.get("prompt_version").asText());
        var p17 = builder(json, "PR-1.7").build(ctx, json.write(PlanInputSnapshot.of(ctx)), AiRetryFeedback.none());
        assertEquals(new PlanPromptBuilder(json).build(ctx, json.write(PlanInputSnapshot.of(ctx)),
                AiRetryFeedback.none()), p17, "PR-1.7 por omision");
        assertFalse(p17.contains("21. VOLUMEN POR GRUPO") || p17.contains("COMPROBACION DEL REPARTO")
                || p17.contains("body_parts_con_material"));

        // 2. PR-2.0 exacto.
        var s20 = MAPPER.readTree(json.write(PlanInputSnapshot.of(ctx, true)));
        assertEquals("PR-2.0", s20.get("prompt_version").asText());
        var cons = s20.get("constraints");
        var complementos = ContratoReparto.complementos(ctx);
        var vol = cons.get("volumen_semanal");
        String nivel = ctx.profile().fitnessLevel();
        assertEquals(DosisSemanalPolicy.techoSeriesSesion(nivel), vol.get("techo_series_sesion").asInt());
        assertEquals(DosisSemanalPolicy.techoSeriesSemana(nivel), vol.get("techo_series_semana").asInt());
        assertEquals(complementos.size(), cons.get("complementos_no_cuentan").size());
        int maxTrabajo = cons.get("max_colocaciones_dia").isNull() ? 10 : cons.get("max_colocaciones_dia").asInt();
        for (var ses : cons.get("suggested_split")) {
            var foco = WorkoutFocus.valueOf(ses.get("focus_code").asText());
            var conMaterial = new ArrayList<String>();
            ses.get("body_parts_con_material").forEach(x -> conMaterial.add(x.asText()));
            assertEquals(ContratoReparto.gruposConMaterial(foco, ctx, complementos), conMaterial);
            assertTrue(foco.bodyPartCodes().containsAll(conMaterial));
            assertEquals(Math.min(2, conMaterial.size()), ses.get("min_body_parts").asInt());
            assertTrue(ses.get("max_per_body_part").isNull());
            var tabla = ContratoReparto.tablaMitad(foco.bodyPartCodes().size(), maxTrabajo);
            if (tabla.isEmpty()) assertFalse(ses.has("max_per_body_part_por_n_trabajo"));
            else tabla.forEach((n, t) -> assertEquals(t, ses.get("max_per_body_part_por_n_trabajo").get(n).asInt()));
        }
        var p20 = builder(json, "PR-2.0").build(ctx, json.write(PlanInputSnapshot.of(ctx, true)), AiRetryFeedback.none());
        assertTrue(p20.contains(PlanPromptBuilder.H2v2.REGLA15_V2), "regla 15 nueva");
        assertTrue(p20.contains("como mucho %d series de un mismo body_part (techo_series_sesion)"
                .formatted(DosisSemanalPolicy.techoSeriesSesion(nivel))), "regla 21 con el techo por sesion");
        assertTrue(p20.contains("como mucho %d series de un mismo body_part (techo_series_semana)"
                .formatted(DosisSemanalPolicy.techoSeriesSemana(nivel))), "regla 21 con el techo semanal");
        assertTrue(p20.contains("COMPROBACION DEL REPARTO"));
        assertFalse(p20.contains("mas 15 %"), "PLAN-REV-2.0: V4 al 100 %");
        assertFalse(p20.contains("completa el tiempo") || p20.contains("si queda tiempo, completa"),
                "PLAN-REV-2.0: sin llenar tiempo");
        assertFalse(p20.contains("llega a su minimo"), "PLAN-REV-2.0: sin minimo de duracion");
        assertTrue(p20.indexOf("COMPROBACION DEL REPARTO") > p20.indexOf("Datos de entrada:"), "despues de los datos");
        // El reintento con V15 y V26 lleva sus pistas y repasa las reglas 1 a 21.
        var fb = new AiRetryFeedback("{}", List.of("V15: el entrenamiento del 2026-11-24 dedica 3 de 4 ejercicios a"
                + " upper legs.", "V26: el entrenamiento del 2026-11-24 suma 8 series directas de upper legs"), List.of());
        var reintento = builder(json, "PR-2.0").build(ctx, json.write(PlanInputSnapshot.of(ctx, true)), fb);
        assertTrue(reintento.contains("V15: cuenta los ejercicios de trabajo n"), "pista V15");
        assertTrue(reintento.contains("V26: en esa sesion (o en la semana) baja series"), "pista V26");
        assertTrue(reintento.contains("las reglas 1 a 21 completas"));
        assertTrue(reintento.contains(PlanPromptBuilder.H2v2.REINTENTO_REPONER_V2));
        FILAS.add(String.join(";", c.id(), c.objetivo().name(), c.nivel().name(), c.lugar().name(),
                String.valueOf(c.dias().size()), String.valueOf(c.minutos()), String.valueOf(p17.length()),
                String.valueOf(p20.length())));

        // 3. Las cuotas son factibles: la rutina del motor de reglas, si es valida, las cumple.
        PlanDraft borrador;
        try {
            borrador = reglas.generate(ctx, List.of());
            validador.validate(borrador, ctx, configuracion.durationToRepsDivisor());
        } catch (RuntimeException e) {
            System.out.println("H2V2_CONTRATO " + c.id() + " sin rutina valida del motor de reglas: " + e.getMessage());
            return;
        }
        planesReglas++;
        var elegibles = ctx.availableExercises().stream()
                .collect(Collectors.toMap(CandidateExercise::exerciseId, x -> x, (a, b) -> a));
        var techoSesion = vol.get("techo_series_sesion").asInt();
        var semana = new TreeMap<String, Integer>();
        for (var w : borrador.workouts()) {
            var trabajo = w.exercises().stream().filter(e -> !complementos.contains(e.exerciseId())).toList();
            var porGrupo = trabajo.stream().map(e -> elegibles.get(e.exerciseId()).bodyPartCode())
                    .collect(Collectors.groupingBy(g -> g, Collectors.counting()));
            var tope = ContratoReparto.topeMitad(w.focus().bodyPartCodes().size(), trabajo.size());
            if (tope != null) porGrupo.forEach((g, k) -> assertTrue(k <= tope, c.id() + " " + w.scheduledDate()
                    + " la rutina valida pasa la mitad: " + g));
            var series = new TreeMap<String, Integer>();
            for (var e : w.exercises()) {
                var cand = elegibles.get(e.exerciseId());
                if (e.plannedSets() != null && ContratoReparto.cuentaParaVolumen(cand, complementos))
                    series.merge(cand.bodyPartCode(), (int) e.plannedSets(), Integer::sum);
            }
            series.forEach((g, n) -> {
                assertTrue(n <= techoSesion, c.id() + " rutina valida sobre el techo de sesion: " + g);
                semana.merge(g, n, Integer::sum);
            });
        }
        semana.forEach((g, n) -> assertTrue(n <= vol.get("techo_series_semana").asInt(), c.id() + " semana " + g));

        // 4. Las cuotas son exactas: una unidad mas da el rechazo correspondiente.
        mutarV26(c, ctx, borrador, elegibles, complementos, techoSesion);
        mutarV15(c, ctx, borrador, elegibles, complementos);
    }

    /** Sube las series de un ejercicio que cuenta hasta pasar el techo de sesion por una: V26 de esa sesion y grupo. */
    void mutarV26(Caso c, PlanGenerationContext ctx, PlanDraft d, Map<Long, CandidateExercise> elegibles,
                  Set<Long> complementos, int techo) {
        for (int i = 0; i < d.workouts().size(); i++) {
            var w = d.workouts().get(i);
            for (int j = 0; j < w.exercises().size(); j++) {
                var e = w.exercises().get(j);
                var cand = elegibles.get(e.exerciseId());
                if (e.plannedSets() == null || !ContratoReparto.cuentaParaVolumen(cand, complementos)) continue;
                String g = cand.bodyPartCode();
                int suma = w.exercises().stream().filter(x -> x.plannedSets() != null)
                        .filter(x -> ContratoReparto.cuentaParaVolumen(elegibles.get(x.exerciseId()), complementos))
                        .filter(x -> g.equals(elegibles.get(x.exerciseId()).bodyPartCode()))
                        .mapToInt(x -> x.plannedSets()).sum();
                short nuevas = (short) (e.plannedSets() + techo - suma + 1);
                var problemas = problemas(conEjercicio(d, i, j, new PlanDraft.DraftExercise(e.exerciseId(),
                        e.prescriptionType(), nuevas, e.plannedReps(), e.plannedDurationSeconds(), e.targetLoadKg(),
                        e.restSeconds(), e.notes())), ctx);
                var esperado = "V26: el entrenamiento del %s suma %d series directas de %s y el techo por sesion es %d"
                        .formatted(w.scheduledDate(), techo + 1, g, techo);
                assertTrue(problemas.stream().anyMatch(p -> p.startsWith(esperado)), c.id() + " esperaba «" + esperado
                        + "» y salio " + problemas);
                mutacionesV26++;
                return;
            }
        }
    }

    /**
     * En una sesion donde aplica la regla de la mitad, cambia ejercicios de otros grupos por ejercicios de UN grupo
     * hasta tener tope + 1 de ese grupo: V15 «dedica tope+1 de n» en esa sesion.
     */
    void mutarV15(Caso c, PlanGenerationContext ctx, PlanDraft d, Map<Long, CandidateExercise> elegibles,
                  Set<Long> complementos) {
        for (int i = 0; i < d.workouts().size(); i++) {
            var w = d.workouts().get(i);
            var trabajo = w.exercises().stream().filter(e -> !complementos.contains(e.exerciseId())).toList();
            var tope = ContratoReparto.topeMitad(w.focus().bodyPartCodes().size(), trabajo.size());
            if (tope == null) continue;
            var usados = w.exercises().stream().map(PlanDraft.DraftExercise::exerciseId).collect(Collectors.toSet());
            for (var g : ContratoReparto.gruposConMaterial(w.focus(), ctx, complementos)) {
                var libres = ctx.availableExercises().stream()
                        .filter(x -> g.equals(x.bodyPartCode()) && !complementos.contains(x.exerciseId())
                                && !usados.contains(x.exerciseId())
                                && ContratoReparto.prescribibleParaV15(x, w.focus(), ctx))
                        .map(CandidateExercise::exerciseId).toList();
                long ya = trabajo.stream().filter(e -> g.equals(elegibles.get(e.exerciseId()).bodyPartCode())).count();
                int faltan = (int) (tope + 1 - ya);
                var otros = new ArrayList<Integer>();
                for (int j = 0; j < w.exercises().size(); j++) {
                    var e = w.exercises().get(j);
                    if (!complementos.contains(e.exerciseId()) && !g.equals(elegibles.get(e.exerciseId()).bodyPartCode()))
                        otros.add(j);
                }
                if (faltan <= 0 || libres.size() < faltan || otros.size() < faltan) continue;
                var ejercicios = new ArrayList<>(w.exercises());
                for (int k = 0; k < faltan; k++) {
                    var e = ejercicios.get(otros.get(k));
                    ejercicios.set(otros.get(k), new PlanDraft.DraftExercise(libres.get(k), e.prescriptionType(),
                            e.plannedSets(), e.plannedReps(), e.plannedDurationSeconds(), null, e.restSeconds(), null));
                }
                var mutado = conSesion(d, i, ejercicios);
                var esperado = "V15: el entrenamiento del %s dedica %d de %d ejercicios a %s"
                        .formatted(w.scheduledDate(), tope + 1, trabajo.size(), g);
                var problemas = problemas(mutado, ctx);
                assertTrue(problemas.stream().anyMatch(p -> p.startsWith(esperado)), c.id() + " esperaba «" + esperado
                        + "» y salio " + problemas);
                mutacionesV15++;
                return;
            }
        }
    }

    List<String> problemas(PlanDraft d, PlanGenerationContext ctx) {
        try {
            validador.validate(d, ctx, configuracion.durationToRepsDivisor());
            return List.of();
        } catch (InvalidPlanDraftException e) {
            return e.problems();
        }
    }

    static PlanDraft conEjercicio(PlanDraft d, int i, int j, PlanDraft.DraftExercise nuevo) {
        var ejercicios = new ArrayList<>(d.workouts().get(i).exercises());
        ejercicios.set(j, nuevo);
        return conSesion(d, i, ejercicios);
    }

    static PlanDraft conSesion(PlanDraft d, int i, List<PlanDraft.DraftExercise> ejercicios) {
        var w = d.workouts().get(i);
        var sesiones = new ArrayList<>(d.workouts());
        sesiones.set(i, new PlanDraft.DraftWorkout(w.scheduledDate(), w.focus(), w.name(), w.expectedDurationMinutes(),
                List.copyOf(ejercicios)));
        return new PlanDraft(d.source(), d.modelName(), d.planName(), d.declaredTotalVolume(), d.rationale(),
                List.copyOf(sesiones));
    }

    @AfterAll
    static void resumen() throws Exception {
        System.out.println("H2V2_CONTRATO planes_reglas=" + planesReglas + " mutaciones_v15=" + mutacionesV15
                + " mutaciones_v26=" + mutacionesV26);
        Files.createDirectories(Path.of("target"));
        var filas = new ArrayList<String>();
        filas.add("caso;objetivo;nivel;lugar;dias;minutos;caracteres_prompt_PR-1.7;caracteres_prompt_PR-2.0");
        synchronized (FILAS) {
            filas.addAll(FILAS.stream().sorted().toList());
        }
        Files.write(Path.of("target", "h2v2_tamano_prompts.csv"), filas);
        assertEquals(26, FILAS.size(), "los 26 perfiles");
        assertTrue(planesReglas >= 18, "rutinas validas del motor de reglas: " + planesReglas);
        assertTrue(mutacionesV26 >= 18, "mutaciones V26 comprobadas: " + mutacionesV26);
        assertTrue(mutacionesV15 >= 6, "mutaciones V15 comprobadas: " + mutacionesV15);
    }
}
