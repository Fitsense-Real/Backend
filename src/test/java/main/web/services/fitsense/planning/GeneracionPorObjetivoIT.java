package main.web.services.fitsense.planning;

import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalConfigurationService;
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
        var g = cobertura(p, userId, plan);
        registrar(g);
        comprobarSeguridadYDosis(g);
        return g;
    }

    /**
     * Que capacidades lleva la semana publicada, calculado en la prueba con la
     * misma politica (la generacion ya no lo registra): resultado DESCRIPTIVO
     * COMPLETO / PARCIAL / SOLO_SECUNDARIAS.
     */
    private Generada cobertura(Perfil p, long userId, long plan) {
        var g0 = new Generada(p, userId, plan, null, Map.of());
        var limites = configuracion.prescriptionParams().repLimits();
        var principales = main.web.services.fitsense.planning.domain.model.valueobjects.GoalCapabilityPolicy
                .forGoal(p.objetivo().name()).orElseThrow().primaries();
        var colocaciones = new TreeMap<String, Integer>();
        var slotsAcreditados = new TreeSet<String>();
        var estructural = principales.stream().filter(c -> c == Capability.STRENGTH || c == Capability.HYPERTROPHY)
                .findFirst().orElse(null);
        for (var f : colocaciones(g0)) {
            var cap = CapabilityPrescriptionPolicy.acreditada(f.ejercicio(), f.candidato(), p.objetivo().name(),
                    p.nivel().name(), limites);
            if (cap.isEmpty()) continue;
            colocaciones.merge(cap.get().name(), 1, Integer::sum);
            if (cap.get() == estructural)
                main.web.services.fitsense.planning.domain.model.valueobjects.MandatorySlot.of(f.candidato())
                        .ifPresent(sl -> slotsAcreditados.add(sl.name()));
        }
        var cubiertas = principales.stream().map(Enum::name).filter(colocaciones::containsKey).sorted().toList();
        var faltan = principales.stream().map(Enum::name).filter(c -> !colocaciones.containsKey(c)).sorted().toList();
        var secundarias = colocaciones.keySet().stream().filter(c -> principales.stream().noneMatch(x -> x.name().equals(c)))
                .toList();
        var slotsSin = estructural == null ? List.<String>of()
                : main.web.services.fitsense.planning.domain.model.valueobjects.MandatorySlot.ALL.stream().map(Enum::name)
                .filter(sl -> !slotsAcreditados.contains(sl)).toList();
        String resultado = faltan.isEmpty() ? "COMPLETO" : cubiertas.isEmpty() ? "SOLO_SECUNDARIAS" : "PARCIAL";
        var mapa = new LinkedHashMap<String, Object>();
        mapa.put("cubiertas", cubiertas.toString());
        mapa.put("no_cubiertas", faltan.stream().map(c -> "\"" + c + "\": 0").collect(Collectors.joining(", ", "{", "}")));
        mapa.put("secundarias", secundarias.toString());
        mapa.put("colocaciones", colocaciones.entrySet().stream().map(e -> "\"" + e.getKey() + "\": " + e.getValue())
                .collect(Collectors.joining(", ", "{", "}")));
        mapa.put("slots_sin_cubrir", slotsSin.toString());
        return new Generada(p, userId, plan, resultado, mapa);
    }

    private long planesDe(long userId) {
        return jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ? AND status = 'ACTIVE'",
                Long.class, userId);
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
                .filter(id -> jdbc.queryForObject("SELECT movement_pattern, default_prescription FROM exercises"
                        + " WHERE exercise_id = ?", (rs, i) -> GoalFeasibilityPolicy.enRevision(id,
                        rs.getString(1), main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType
                                .valueOf(rs.getString(2))), id))
                .toList();
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
            assertEquals("[]", g.cobertura().get("slots_sin_cubrir"), "los cuatro patrones con dosis de fuerza");
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

    @Nested
    @DisplayName("2 · algunas principales factibles y otras no: se publica con las disponibles y secundarias")
    class Parciales {

        @Test
        @DisplayName("GENERAL_FITNESS sin cardio posible: se publica automaticamente; fuerza, resistencia y movilidad, sin cardio")
        void condicionGeneralSinCardio() {
            var g = generar(perfil("GENERAL_FITNESS · principiante en casa sin suelo, 3x60", GoalType.GENERAL_FITNESS,
                    FitnessLevel.BEGINNER, 41, TrainingLocation.HOME, CASA, LMV, 60, true, false, true));
            assertEquals(1, planesDe(g.userId()), "publicado y activo, sin aceptacion");
            assertEquals("PARCIAL", g.resultado());
            assertEquals(Set.of("CARDIO"), claves(g.cobertura().get("no_cubiertas")));
            assertEquals(Set.of("MOBILITY", "MUSCULAR_ENDURANCE", "STRENGTH"), lista(g.cobertura().get("cubiertas")));
            assertTrue(idsDeCapacidad(g, Capability.CARDIO).isEmpty(), "no se inventa cardio");
            assertEquals("[]", g.cobertura().get("slots_sin_cubrir"), "la fuerza toma todos los patrones disponibles");
            assertTrue(lista(g.cobertura().get("secundarias")).contains("HYPERTROPHY"),
                    "la hipertrofia completa el tiempo que deja libre el cardio");
        }

        @Test
        @DisplayName("API: POST /plan/generate publica (201) el plan sin pedir aceptacion")
        void apiPublica() {
            var p = perfil("GENERAL_FITNESS · principiante en casa sin suelo, 3x45 (API)", GoalType.GENERAL_FITNESS,
                    FitnessLevel.BEGINNER, 41, TrainingLocation.HOME, CASA, LMV, 45, true, false, true);
            long userId = usuario(p);
            var principal = main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl
                    .build(usuarios.findById(userId).orElseThrow());
            var respuesta = planes.generate(principal);
            assertEquals(201, respuesta.getStatusCode().value());
            assertEquals("ACTIVE", respuesta.getBody().status());
            assertEquals(1, planesDe(userId));
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
    @DisplayName("4 · ninguna principal factible: semana segura con las secundarias")
    class NingunaPrincipal {

        @Test
        @DisplayName("LOSE_WEIGHT sin ningun cardio posible: se publica una semana de fuerza, sin cardio inventado")
        void perderPesoSinCardio() {
            var g = generar(perfil("LOSE_WEIGHT · principiante en casa sin suelo", GoalType.LOSE_WEIGHT,
                    FitnessLevel.BEGINNER, 45, TrainingLocation.HOME, CASA, LMV, 45, true, false, true));
            assertEquals(1, planesDe(g.userId()));
            assertEquals("SOLO_SECUNDARIAS", g.resultado());
            assertEquals(Set.of("STRENGTH"), claves(g.cobertura().get("colocaciones")), "todo es fuerza, la secundaria");
            assertTrue(idsDeCapacidad(g, Capability.CARDIO).isEmpty());
        }

        @Test
        @DisplayName("sin ningun ejercicio compatible: error de generacion sencillo, sin plan")
        void sinEjerciciosCompatibles() {
            var p = perfil("LOSE_WEIGHT · sin ejercicios", GoalType.LOSE_WEIGHT, FitnessLevel.BEGINNER, 45,
                    TrainingLocation.HOME, CASA, LMV, 45, true, false, true);
            long userId = usuario(p);
            // Todo el catalogo bloqueado: no hay nada que prescribir.
            jdbc.update("UPDATE user_profiles SET blocked_exercise_ids = (SELECT array_agg(exercise_id) FROM exercises)"
                    + " WHERE user_id = ?", userId);
            var error = assertThrows(RuntimeException.class,
                    () -> planning.generateWeeklyPlan(userId, SEMANA, PlanAdjustment.none(), null, null));
            assertNotNull(error.getMessage());
            assertEquals(0, planesDe(userId));
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
                                    var g = cobertura(p, userId, plan);
                                    r = g.resultado() + (g.resultado().equals("COMPLETO") ? ""
                                            : " sin " + claves(g.cobertura().get("no_cubiertas")));
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
            assertTrue(fallos.isEmpty(), String.join("\n", fallos));
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
