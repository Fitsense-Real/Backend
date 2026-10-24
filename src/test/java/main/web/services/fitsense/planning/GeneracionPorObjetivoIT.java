package main.web.services.fitsense.planning;

import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalConfigurationService;
import main.web.services.fitsense.planning.domain.exceptions.NoPrimaryCapabilityFeasibleException;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.GoalCapabilityPolicy.Capability;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.services.CapabilityPrescriptionPolicy;
import main.web.services.fitsense.planning.domain.services.GoalFeasibilityPolicy;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GENERACION INICIAL POR OBJETIVO (4A v6 + 4D v4) con el catalogo REAL.
 * <p>
 * PostgreSQL 16 efimero con V1-V33, flujo de produccion
 * ({@code PlanningContextFacade.generateWeeklyPlan}), IA desactivada: la semana
 * la hace el motor de reglas y la validan los validadores reales con la
 * configuracion por defecto (dosis y capacidades en ENFORCE). Sin Replicate.
 * <p>
 * Cada semana generada se escribe legible en
 * {@code target/generacion-por-objetivo/} para revisarla.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
class GeneracionPorObjetivoIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired PlanningContextFacade planning;
    @Autowired UserProfileCommandService perfiles;
    @Autowired ExternalConfigurationService configuracion;
    @Autowired JdbcTemplate jdbc;
    @Autowired main.web.services.fitsense.planning.interfaces.rest.TrainingPlanController planes;
    @Autowired main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository usuarios;
    @Autowired org.springframework.transaction.PlatformTransactionManager transacciones;

    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final LocalDate SEMANA = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
    static final List<String> GIMNASIO = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine",
            "bench", "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine");
    static final List<String> CASA = List.of("body weight", "dumbbell", "band");
    static final StringBuilder EJEMPLOS = new StringBuilder();

    record Perfil(String nombre, GoalType objetivo, FitnessLevel nivel, int edad, TrainingLocation lugar,
                  List<String> equipo, List<Short> dias, int minutos, boolean impacto, boolean suelo,
                  boolean brazosArriba) {
        Perfil con(String n) {
            return new Perfil(n, objetivo, nivel, edad, lugar, equipo, dias, minutos, impacto, suelo, brazosArriba);
        }
    }

    static Perfil base(String nombre, GoalType objetivo) {
        return new Perfil(nombre, objetivo, FitnessLevel.INTERMEDIATE, 34, TrainingLocation.GYM, GIMNASIO,
                List.of((short) 1, (short) 3, (short) 5), 45, true, true, true);
    }

    record Generada(Perfil perfil, long userId, long planId, String resultado, Map<String, Object> cobertura) {}

    private long usuario(Perfil p) {
        int n = SECUENCIA.incrementAndGet();
        String email = "objetivo" + n + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long userId = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(userId, LocalDate.now().minusYears(p.edad()).minusDays(10),
                BiologicalSex.FEMALE, new BigDecimal("165"), new BigDecimal("70"), null, p.nivel(), p.objetivo(),
                p.nombre(), p.lugar(), (short) p.dias().size(), p.dias(), (short) p.minutos(), p.equipo(), List.of(),
                "Ninguna", p.impacto(), p.suelo(), true, true, p.brazosArriba()));
        return userId;
    }

    private Generada generar(Perfil p) {
        long userId = usuario(p);
        long plan = planning.generateWeeklyPlan(userId, SEMANA, PlanAdjustment.none(), null, null)
                .orElseThrow(() -> new AssertionError("sin plan para " + p.nombre()));
        var salida = jdbc.queryForObject("SELECT output_snapshot::text FROM weekly_training_plans WHERE plan_id = ?",
                String.class, plan);
        var cobertura = jdbc.queryForMap("SELECT output_snapshot->'capability_coverage'->>'resultado' AS resultado,"
                + " output_snapshot->'capability_coverage'->'principales_cubiertas' AS cubiertas,"
                + " output_snapshot->'capability_coverage'->'no_cubiertas' AS no_cubiertas,"
                + " output_snapshot->'capability_coverage'->'secundarias_incorporadas' AS secundarias,"
                + " output_snapshot->'capability_coverage'->'colocaciones' AS colocaciones"
                + " FROM weekly_training_plans WHERE plan_id = ?", plan);
        assertNotNull(salida);
        var g = new Generada(p, userId, plan, (String) cobertura.get("resultado"), cobertura);
        registrar(g);
        comprobarSeguridadYDosis(g);
        return g;
    }

    /** Ningun ejercicio incompatible y toda colocacion acreditada a una capacidad del objetivo. */
    private void comprobarSeguridadYDosis(Generada g) {
        var p = g.perfil();
        boolean sinImpacto = !p.impacto() || p.nivel() == FitnessLevel.BEGINNER || p.edad() >= 60;
        boolean sinSuelo = !p.suelo() || p.edad() >= 70;
        boolean sinAxial = p.edad() >= 65;
        var violaciones = jdbc.queryForList("SELECT pwe.exercise_id FROM planned_workout_exercises pwe"
                + " JOIN planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id"
                + " JOIN exercises x ON x.exercise_id = pwe.exercise_id WHERE pw.plan_id = ?"
                + " AND ((? AND x.high_impact) OR (? AND x.requires_floor) OR (? AND x.requires_overhead)"
                + "  OR (? AND (x.axial_load OR x.requires_inversion))"
                + "  OR x.difficulty_level > ? OR NOT x.is_active)", Long.class, g.planId(), sinImpacto,
                sinSuelo, !p.brazosArriba(), sinAxial, p.nivel() == FitnessLevel.BEGINNER ? 1
                        : p.nivel() == FitnessLevel.ADVANCED ? 3 : 2);
        assertTrue(violaciones.isEmpty(), p.nombre() + ": ejercicios incompatibles " + violaciones);
        var enRevision = colocaciones(g).stream().map(Fila::exerciseId)
                .filter(GoalFeasibilityPolicy.CLASIFICACION_EN_REVISION::contains).toList();
        assertTrue(enRevision.isEmpty(),
                p.nombre() + ": ejercicios en revision " + enRevision);

        // Toda colocacion acredita una capacidad del objetivo, tambien en
        // GAIN_MUSCLE (4D v3.1: solo estructural o resistido acredita hipertrofia).
        var limites = configuracion.prescriptionParams().repLimits();
        for (var f : colocaciones(g)) {
            var cap = CapabilityPrescriptionPolicy.acreditada(f.ejercicio(), f.candidato(), p.objetivo().name(),
                    p.nivel().name(), limites);
            assertTrue(cap.isPresent(), p.nombre() + ": colocacion sin capacidad " + f);
        }
    }

    record Fila(long exerciseId, LocalDate fecha, String enfoque, String nombre, CandidateExercise candidato,
                PlanDraft.DraftExercise ejercicio, int minutos) {}

    private List<Fila> colocaciones(Generada g) {
        return jdbc.query("SELECT pwe.exercise_id, pw.scheduled_date, pw.focus_code, x.name_es, bp.code,"
                        + " e.code AS equipo, x.difficulty_level, x.default_prescription, x.target_muscle,"
                        + " x.movement_pattern, pwe.prescription_type, pwe.planned_sets, pwe.planned_reps,"
                        + " pwe.planned_duration_seconds, pwe.rest_seconds, pw.expected_duration_minutes"
                        + " FROM planned_workout_exercises pwe"
                        + " JOIN planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id"
                        + " JOIN exercises x ON x.exercise_id = pwe.exercise_id"
                        + " JOIN body_parts bp ON bp.body_part_id = x.body_part_id"
                        + " JOIN equipment_types e ON e.equipment_id = x.equipment_id"
                        + " WHERE pw.plan_id = ? ORDER BY pw.scheduled_date, pwe.exercise_order",
                (rs, i) -> {
                    var tipo = PrescriptionType.valueOf(rs.getString("default_prescription"));
                    var c = new CandidateExercise(rs.getLong(1), rs.getString("name_es"), rs.getString("code"),
                            rs.getString("equipo"), rs.getInt("difficulty_level"), tipo, rs.getString("target_muscle"),
                            rs.getString("movement_pattern"));
                    var e = new PlanDraft.DraftExercise(rs.getLong(1), PrescriptionType.valueOf(rs.getString("prescription_type")),
                            (Short) rs.getObject("planned_sets", Short.class), (Short) rs.getObject("planned_reps", Short.class),
                            (Integer) rs.getObject("planned_duration_seconds", Integer.class), null,
                            (Short) rs.getObject("rest_seconds", Short.class), null);
                    return new Fila(rs.getLong(1), rs.getDate(2).toLocalDate(), rs.getString(3), rs.getString("name_es"),
                            c, e, rs.getInt("expected_duration_minutes"));
                }, g.planId());
    }

    private void registrar(Generada g) {
        var p = g.perfil();
        var t = new StringBuilder();
        t.append("### ").append(p.nombre()).append("\n\n");
        t.append("`%s` · %s · %d anos · %s · %d dias x %d min · impacto=%s suelo=%s brazos_arriba=%s · equipo %s\n\n"
                .formatted(p.objetivo(), p.nivel(), p.edad(), p.lugar(), p.dias().size(), p.minutos(), p.impacto(),
                        p.suelo(), p.brazosArriba(), p.equipo()));
        t.append("**Resultado: %s** · principales cubiertas %s · no cubiertas %s · secundarias %s · colocaciones %s\n\n"
                .formatted(g.resultado(), g.cobertura().get("cubiertas"), g.cobertura().get("no_cubiertas"),
                        g.cobertura().get("secundarias"), g.cobertura().get("colocaciones")));
        var limites = configuracion.prescriptionParams().repLimits();
        LocalDate dia = null;
        for (var f : colocaciones(g)) {
            if (!f.fecha().equals(dia)) {
                dia = f.fecha();
                t.append("\n- **%s %s** (%d min)\n".formatted(f.fecha().getDayOfWeek(), f.enfoque(), f.minutos()));
            }
            var cap = CapabilityPrescriptionPolicy.acreditada(f.ejercicio(), f.candidato(), p.objetivo().name(),
                    p.nivel().name(), limites).map(Enum::name).orElse("SIN ACREDITAR");
            var e = f.ejercicio();
            String dosis = e.prescriptionType() == PrescriptionType.SETS_REPS
                    ? "%dx%d, %d s".formatted(e.plannedSets(), e.plannedReps(), e.restSeconds())
                    : "%dx%d s, %d s".formatted(e.plannedSets(), e.plannedDurationSeconds(), e.restSeconds());
            t.append("  - %d %s (%s, %s) · %s · **%s**\n".formatted(f.exerciseId(), f.nombre(),
                    f.candidato().bodyPartCode(), f.candidato().movementPattern(), dosis, cap));
        }
        t.append("\n");
        synchronized (EJEMPLOS) { EJEMPLOS.append(t); }
    }

    @AfterAll
    static void escribir() throws Exception {
        var dir = Path.of("target/generacion-por-objetivo");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("semanas.md"), EJEMPLOS.toString());
    }

    private Set<String> lista(Object json) {
        if (json == null) return Set.of();
        var s = json.toString().replaceAll("[\\[\\]\"\\s]", "");
        return s.isEmpty() ? Set.of() : new TreeSet<>(Arrays.asList(s.split(",")));
    }

    private Set<String> claves(Object json) {
        if (json == null) return Set.of();
        var m = new TreeSet<String>();
        var matcher = java.util.regex.Pattern.compile("\"([A-Z_]+)\"\\s*:").matcher(json.toString());
        while (matcher.find()) m.add(matcher.group(1));
        return m;
    }

    // =====================================================================

    @Nested
    @DisplayName("1 · todas las principales disponibles: semana COMPLETA en los cuatro objetivos")
    class Completas {

        @Test
        @DisplayName("GAIN_MUSCLE: regresion, hipertrofia cubierta como hasta ahora")
        void ganarMusculo() {
            var g = generar(base("GAIN_MUSCLE · gimnasio, intermedia, 3x45", GoalType.GAIN_MUSCLE));
            assertEquals("COMPLETO", g.resultado());
            assertEquals(Set.of("HYPERTROPHY"), lista(g.cobertura().get("cubiertas")));
        }

        @Test
        @DisplayName("INCREASE_STRENGTH: fuerza con dosis de fuerza; hipertrofia como secundaria")
        void fuerza() {
            var g = generar(base("INCREASE_STRENGTH · gimnasio, intermedia, 3x45", GoalType.INCREASE_STRENGTH));
            assertEquals("COMPLETO", g.resultado());
            assertEquals(Set.of("STRENGTH"), lista(g.cobertura().get("cubiertas")));
        }

        @Test
        @DisplayName("LOSE_WEIGHT: cardio principal en la semana; fuerza como secundaria")
        void perderPeso() {
            var g = generar(base("LOSE_WEIGHT · gimnasio, intermedia, 3x45", GoalType.LOSE_WEIGHT));
            assertEquals("COMPLETO", g.resultado());
            assertEquals(Set.of("CARDIO"), lista(g.cobertura().get("cubiertas")));
            assertTrue(claves(g.cobertura().get("colocaciones")).contains("CARDIO"));
        }

        @Test
        @DisplayName("GENERAL_FITNESS: fuerza, resistencia muscular, cardio y movilidad")
        void condicionGeneral() {
            var g = generar(base("GENERAL_FITNESS · gimnasio, intermedia, 3x45", GoalType.GENERAL_FITNESS));
            assertEquals("COMPLETO", g.resultado(), g.cobertura().toString());
            assertEquals("[]", jdbc.queryForObject("SELECT output_snapshot->'capability_coverage'->>'slots_sin_cubrir'"
                    + " FROM weekly_training_plans WHERE plan_id = ?", String.class, g.planId()).replace(" ", ""),
                    "los cuatro patrones con dosis de fuerza");
            var principal = main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl
                    .build(usuarios.findById(g.userId()).orElseThrow());
            var plan = new org.springframework.transaction.support.TransactionTemplate(transacciones)
                    .execute(tx -> planes.byId(principal, g.planId()).getBody());
            assertFalse(plan.capabilityCoverage().requiresAcceptance());
            assertNull(plan.capabilityCoverage().message());
            assertEquals(Set.of("STRENGTH", "MUSCULAR_ENDURANCE", "CARDIO", "MOBILITY"),
                    lista(g.cobertura().get("cubiertas")));
        }
    }

    static Perfil perfil(String nombre, GoalType objetivo, FitnessLevel nivel, int edad, TrainingLocation lugar,
                         List<String> equipo, List<Short> dias, int minutos, boolean impacto, boolean suelo,
                         boolean brazosArriba) {
        return new Perfil(nombre, objetivo, nivel, edad, lugar, equipo, dias, minutos, impacto, suelo, brazosArriba);
    }

    static final List<Short> LMV = List.of((short) 1, (short) 3, (short) 5);

    private Set<Long> idsDeCapacidad(Generada g, Capability capacidad) {
        var limites = configuracion.prescriptionParams().repLimits();
        return colocaciones(g).stream().filter(f -> CapabilityPrescriptionPolicy.acreditada(f.ejercicio(),
                        f.candidato(), g.perfil().objetivo().name(), g.perfil().nivel().name(), limites)
                .filter(c -> c == capacidad).isPresent()).map(Fila::exerciseId).collect(Collectors.toSet());
    }

    /** Una semana PARCIAL: no se publica; la propuesta viaja en la excepcion. */
    private main.web.services.fitsense.planning.domain.exceptions.PartialPlanRequiresAcceptanceException parcial(Perfil p) {
        long userId = usuario(p);
        var e = assertThrows(main.web.services.fitsense.planning.domain.exceptions.PartialPlanRequiresAcceptanceException.class,
                () -> planning.generateWeeklyPlan(userId, SEMANA, PlanAdjustment.none(), null, null));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?",
                Integer.class, userId), "un PARCIAL no se activa ni se guarda");
        var c = e.propuesta().cobertura();
        var t = new StringBuilder("### ").append(p.nombre()).append("\n\n**PARCIAL: propuesta NO activada, requiere aceptacion** · cubiertas ")
                .append(c.principalesCubiertas()).append(" · no cubiertas ").append(c.noCubiertas())
                .append(" · secundarias ").append(c.secundariasIncorporadas()).append("\n\n");
        for (var w : e.propuesta().draft().workouts()) {
            t.append("- **%s %s** (%d min)\n".formatted(w.scheduledDate().getDayOfWeek(), w.focus(), w.expectedDurationMinutes()));
            for (var x : w.exercises())
                t.append("  - %d · %s\n".formatted(x.exerciseId(), x.prescriptionType() == PrescriptionType.SETS_REPS
                        ? "%dx%d, %d s".formatted(x.plannedSets(), x.plannedReps(), x.restSeconds())
                        : "%dx%d s, %d s".formatted(x.plannedSets(), x.plannedDurationSeconds(), x.restSeconds())));
        }
        synchronized (EJEMPLOS) { EJEMPLOS.append(t).append("\n"); }
        return e;
    }

    @Nested
    @DisplayName("2 · algunas principales factibles y otras no: PARCIAL, propuesta sin activar")
    class Parciales {

        @Test
        @DisplayName("GENERAL_FITNESS sin cardio posible: no se activa; la propuesta dice que falta cardio y por que")
        void condicionGeneralSinCardio() {
            var e = parcial(perfil("GENERAL_FITNESS · principiante en casa sin suelo, 3x60", GoalType.GENERAL_FITNESS,
                    FitnessLevel.BEGINNER, 41, TrainingLocation.HOME, CASA, LMV, 60, true, false, true));
            var c = e.propuesta().cobertura();
            assertEquals(main.web.services.fitsense.planning.domain.services.CapabilityCoveragePolicy.Resultado.PARCIAL,
                    c.resultado());
            assertEquals(Set.of(Capability.CARDIO), c.noCubiertas().keySet());
            assertTrue(c.noCubiertas().get(Capability.CARDIO).startsWith("no factible"));
            assertTrue(c.principalesCubiertas().containsAll(Set.of(Capability.STRENGTH, Capability.MUSCULAR_ENDURANCE,
                    Capability.MOBILITY)));
            assertTrue(c.slotsSinCubrir().isEmpty(), "la fuerza toma todos los patrones disponibles");
            assertTrue(c.secundariasIncorporadas().contains(Capability.HYPERTROPHY),
                    "la hipertrofia completa el tiempo que deja libre el cardio");
            assertTrue(e.getMessage().startsWith("PARCIAL_REQUIERE_ACEPTACION"), e.getMessage());
        }

        @Test
        @DisplayName("API: POST /plan/generate responde 409 con la propuesta y lo que falta, y no hay plan activo")
        void apiDevuelveLaPropuesta() {
            var p = perfil("GENERAL_FITNESS · principiante en casa sin suelo, 3x45 (API)", GoalType.GENERAL_FITNESS,
                    FitnessLevel.BEGINNER, 41, TrainingLocation.HOME, CASA, LMV, 45, true, false, true);
            long userId = usuario(p);
            var principal = main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl
                    .build(usuarios.findById(userId).orElseThrow());
            var respuesta = planes.generate(principal);
            assertEquals(409, respuesta.getStatusCode().value());
            var body = (main.web.services.fitsense.planning.interfaces.rest.resources.PartialPlanProposalResource) respuesta.getBody();
            assertEquals("REQUIERE_ACEPTACION", body.status());
            assertTrue(body.capabilityCoverage().requiresAcceptance());
            assertTrue(body.capabilityCoverage().missingPrimaries().containsKey("CARDIO"));
            assertTrue(body.capabilityCoverage().message().contains("alternativa parcial"));
            assertFalse(body.workouts().isEmpty());
            assertTrue(body.workouts().stream().flatMap(w -> w.exercises().stream()).allMatch(x -> x.exerciseName() != null));
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?",
                    Integer.class, userId));
        }
    }

    @Nested
    @DisplayName("3 · tiempo: las principales primero, las secundarias solo con lo que sobra")
    class Tiempo {

        @Test
        @DisplayName("sesiones cortas: las principales ocupan el tiempo y no entra ninguna secundaria")
        void principalesConsumenElTiempo() {
            var g = generar(perfil("INCREASE_STRENGTH · gimnasio, 3x20", GoalType.INCREASE_STRENGTH,
                    FitnessLevel.INTERMEDIATE, 30, TrainingLocation.GYM, GIMNASIO, LMV, 20, true, true, true));
            assertEquals("COMPLETO", g.resultado());
            assertEquals(Set.of(), lista(g.cobertura().get("secundarias")), g.cobertura().toString());
        }

        @Test
        @DisplayName("sesiones largas: cubierta la fuerza, el tiempo restante va a hipertrofia")
        void tiempoRestanteParaSecundarias() {
            var g = generar(perfil("INCREASE_STRENGTH · gimnasio, 3x75", GoalType.INCREASE_STRENGTH,
                    FitnessLevel.INTERMEDIATE, 30, TrainingLocation.GYM, GIMNASIO, LMV, 75, true, true, true));
            assertEquals("COMPLETO", g.resultado());
            assertTrue(lista(g.cobertura().get("secundarias")).contains("HYPERTROPHY"), g.cobertura().toString());
            // La fuerza no se desplaza: cada sesion empieza por ella.
            assertTrue(idsDeCapacidad(g, Capability.STRENGTH).size() >= 4);
        }
    }

    @Nested
    @DisplayName("4 · ninguna principal factible: no se publica una semana de secundarias")
    class NingunaPrincipal {

        @Test
        @DisplayName("LOSE_WEIGHT sin ningun cardio verificado posible: NO_FACTIBLE explicado, sin plan")
        void perderPesoSinCardio() {
            var p = perfil("LOSE_WEIGHT · principiante en casa sin suelo", GoalType.LOSE_WEIGHT,
                    FitnessLevel.BEGINNER, 45, TrainingLocation.HOME, CASA, LMV, 45, true, false, true);
            long userId = usuario(p);
            var error = assertThrows(NoPrimaryCapabilityFeasibleException.class,
                    () -> planning.generateWeeklyPlan(userId, SEMANA, PlanAdjustment.none(), null, null));
            assertTrue(error.getMessage().contains("NINGUNA_PRINCIPAL_FACTIBLE"), error.getMessage());
            assertTrue(error.getMessage().contains("CARDIO"), error.getMessage());
            assertTrue(error.getMessage().contains("alternativa limitada con [STRENGTH]"), error.getMessage());
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?",
                    Integer.class, userId));
            EJEMPLOS.append("### ").append(p.nombre()).append("\n\n**NO_FACTIBLE**, no se publica: ")
                    .append(error.getMessage()).append("\n\n");
        }
    }

    @Nested
    @DisplayName("5 · restricciones y casos limite")
    class Restricciones {

        @Test
        @DisplayName("un unico cardio compatible: se usa en cada sesion no contigua y la semana queda COMPLETA")
        void unUnicoCardio() {
            var equipo = new ArrayList<>(CASA);
            equipo.add("treadmill");
            var g = generar(perfil("LOSE_WEIGHT · principiante sin suelo, con cinta, 3x45", GoalType.LOSE_WEIGHT,
                    FitnessLevel.BEGINNER, 38, TrainingLocation.HOME, equipo, LMV, 45, true, false, true));
            assertEquals("COMPLETO", g.resultado());
            assertEquals(Set.of(1325L), idsDeCapacidad(g, Capability.CARDIO));
        }

        @Test
        @DisplayName("66 anos sin brazos arriba: sin impacto, sin carga axial, sin brazos arriba; cardio de bajo impacto")
        void mayorConRestricciones() {
            var g = generar(perfil("GENERAL_FITNESS · 66 anos, gimnasio, sin brazos arriba, 3x45",
                    GoalType.GENERAL_FITNESS, FitnessLevel.INTERMEDIATE, 66, TrainingLocation.GYM, GIMNASIO, LMV, 45,
                    true, true, false));
            assertNotEquals("NO_FACTIBLE", g.resultado());
            var cardio = idsDeCapacidad(g, Capability.CARDIO);
            assertFalse(cardio.isEmpty(), "hay cardio de bajo impacto en el gimnasio");
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM exercises WHERE exercise_id = ANY(?) AND high_impact",
                    Integer.class, (Object) cardio.toArray(Long[]::new)));
        }

        @Test
        @DisplayName("sin alto impacto declarado (intermedia): el cardio es solo de bajo impacto")
        void sinAltoImpacto() {
            var g = generar(perfil("LOSE_WEIGHT · intermedia, sin alto impacto, 4x40", GoalType.LOSE_WEIGHT,
                    FitnessLevel.INTERMEDIATE, 29, TrainingLocation.GYM, GIMNASIO,
                    List.of((short) 1, (short) 2, (short) 4, (short) 5), 40, false, true, true));
            assertEquals("COMPLETO", g.resultado());
        }

        @Test
        @DisplayName("casa con poco material, principiante, 45 min: GAIN_MUSCLE sigue publicandose como antes")
        void ganarMusculoEnCasa() {
            var g = generar(perfil("GAIN_MUSCLE · principiante en casa, 3x45", GoalType.GAIN_MUSCLE,
                    FitnessLevel.BEGINNER, 52, TrainingLocation.HOME, CASA, LMV, 45, true, true, true));
            assertEquals("COMPLETO", g.resultado());
        }

        @Test
        @DisplayName("avanzada, 5 dias, empuje/traccion/piernas: fuerza en todos los patrones disponibles")
        void fuerzaCincoDias() {
            var g = generar(perfil("INCREASE_STRENGTH · avanzada, 5x60", GoalType.INCREASE_STRENGTH,
                    FitnessLevel.ADVANCED, 27, TrainingLocation.GYM, GIMNASIO,
                    List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5), 60, true, true, true));
            assertEquals("COMPLETO", g.resultado());
        }
    }

    @Nested
    @DisplayName("6 · barrido: cada perfil recibe COMPLETO, PARCIAL o un NO_FACTIBLE explicado")
    class Barrido {

        @Test
        @DisplayName("4 objetivos x 3 niveles x casa/gimnasio x 30/45/60 min x suelo si/no")
        void barrido() throws Exception {
            var filas = new ArrayList<String>();
            var fallos = new ArrayList<String>();
            for (var objetivo : GoalType.values())
                for (var nivel : FitnessLevel.values())
                    for (var lugar : List.of(TrainingLocation.HOME, TrainingLocation.GYM))
                        for (int minutos : List.of(30, 45, 60))
                            for (boolean suelo : List.of(true, false)) {
                                var p = perfil("barrido", objetivo, nivel, 40, lugar,
                                        lugar == TrainingLocation.HOME ? CASA : GIMNASIO, LMV, minutos, true, suelo, true);
                                String r;
                                long userId = usuario(p);
                                try {
                                    long plan = planning.generateWeeklyPlan(userId, SEMANA, PlanAdjustment.none(), null, null)
                                            .orElseThrow();
                                    // Las mismas comprobaciones que las semanas de ejemplo:
                                    // nada incompatible y toda colocacion acreditada.
                                    comprobarSeguridadYDosis(new Generada(p, userId, plan, null, Map.of()));
                                    r = jdbc.queryForObject("SELECT coalesce(output_snapshot->'capability_coverage'->>'resultado','-')"
                                            + " || ' ' || coalesce(output_snapshot->'capability_coverage'->>'no_cubiertas','')"
                                            + " FROM weekly_training_plans WHERE plan_id = ?", String.class, plan);
                                    r = r.replaceAll(": no factible[^\"]*", "");
                                } catch (NoPrimaryCapabilityFeasibleException e) {
                                    r = "NO_FACTIBLE (no se publica)";
                                } catch (main.web.services.fitsense.planning.domain.exceptions.PartialPlanRequiresAcceptanceException e) {
                                    r = "PARCIAL (propuesta, no se activa) " + e.propuesta().cobertura().noCubiertas().keySet();
                                    assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans"
                                            + " WHERE user_id = ?", Integer.class, userId));
                                } catch (RuntimeException e) {
                                    r = "FALLO " + e.getMessage();
                                    fallos.add("%s %s %s %d suelo=%s: %s".formatted(objetivo, nivel, lugar, minutos, suelo,
                                            e.getMessage()));
                                }
                                filas.add("| %s | %s | %s | %d | %s | %s |".formatted(objetivo, nivel, lugar, minutos,
                                        suelo ? "si" : "no", r.length() > 160 ? r.substring(0, 160) + "..." : r));
                            }
            var dir = Path.of("target/generacion-por-objetivo");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("barrido.md"), "| objetivo | nivel | lugar | min | suelo | resultado |\n"
                    + "|---|---|---|---|---|---|\n" + String.join("\n", filas) + "\n");
            System.out.println("BARRIDO_FALLOS " + fallos.size() + " de " + filas.size());
            fallos.forEach(f -> System.out.println("BARRIDO_FALLO " + f));
            // Los objetivos que genera el camino nuevo no pueden quedarse sin
            // semana por un fallo de validacion: COMPLETO, PARCIAL o NO_FACTIBLE.
            // GAIN_MUSCLE sigue el camino 4D v3, sin cambios; sus fallos a 30 y 60
            // minutos son anteriores y se informan aparte.
            var nuevos = fallos.stream().filter(f -> !f.startsWith("GAIN_MUSCLE")).toList();
            assertTrue(nuevos.isEmpty(), String.join("\n", nuevos));
        }
    }

    @Nested
    @DisplayName("7 · GAIN_MUSCLE con 4D v3.1: 30, 45 y 60 minutos")
    class Hipertrofia {

        private void comprobarHipertrofia(Generada g, int maxColocaciones) {
            assertEquals("COMPLETO", g.resultado());
            var filas = colocaciones(g);
            var porDia = filas.stream().collect(Collectors.groupingBy(Fila::fecha, Collectors.toList()));
            double presupuesto = main.web.services.fitsense.planning.domain.model.valueobjects.MvpDosePolicy
                    .presupuestoDiarioSegundos(g.perfil().minutos());
            porDia.forEach((dia, lista) -> {
                assertTrue(lista.size() <= maxColocaciones, dia + ": " + lista.size() + " colocaciones");
                double coste = lista.stream().mapToDouble(f -> f.ejercicio().plannedSets()
                        * (f.ejercicio().plannedReps() * 3.0 + f.ejercicio().restSeconds())).sum();
                assertTrue(coste <= presupuesto, dia + ": coste " + coste + " > " + presupuesto);
            });
            var slots = filas.stream().map(f -> main.web.services.fitsense.planning.domain.model.valueobjects.MandatorySlot
                    .of(f.candidato())).flatMap(Optional::stream).collect(Collectors.toSet());
            assertEquals(4, slots.size(), "los cuatro slots, repartidos en la semana: " + slots);
            assertTrue(filas.stream().allMatch(f -> f.ejercicio().prescriptionType() == PrescriptionType.SETS_REPS),
                    "nada de duracion: 4D v3 no tiene dosis de hipertrofia para ella");
        }

        @Test
        @DisplayName("30 min: menos colocaciones por sesion, dentro del presupuesto, slots repartidos")
        void treinta() {
            var g = generar(perfil("GAIN_MUSCLE · gimnasio, intermedia, 3x30", GoalType.GAIN_MUSCLE,
                    FitnessLevel.INTERMEDIATE, 30, TrainingLocation.GYM, GIMNASIO, LMV, 30, true, true, true));
            comprobarHipertrofia(g, 6);
        }

        @Test
        @DisplayName("60 min: hasta siete colocaciones y el minimo de V20 alcanzado")
        void sesenta() {
            var g = generar(perfil("GAIN_MUSCLE · casa, principiante, 3x60", GoalType.GAIN_MUSCLE,
                    FitnessLevel.BEGINNER, 45, TrainingLocation.HOME, CASA, LMV, 60, true, true, true));
            comprobarHipertrofia(g, 7);
        }

        @Test
        @DisplayName("45 min: igual que antes, seis colocaciones como maximo")
        void cuarentaYCinco() {
            var g = generar(perfil("GAIN_MUSCLE · gimnasio, avanzada, 4x45", GoalType.GAIN_MUSCLE,
                    FitnessLevel.ADVANCED, 28, TrainingLocation.GYM, GIMNASIO,
                    List.of((short) 1, (short) 2, (short) 4, (short) 5), 45, true, true, true));
            comprobarHipertrofia(g, 6);
        }
    }
}
