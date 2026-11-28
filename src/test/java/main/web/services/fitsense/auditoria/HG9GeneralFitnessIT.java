package main.web.services.fitsense.auditoria;

import main.web.services.fitsense.RelojPruebas;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.domain.model.valueobjects.GoalCapabilityPolicy.Capability;
import main.web.services.fitsense.planning.domain.services.CapabilityPrescriptionPolicy;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AUDITORIA DE ACTIVACION (11/11/2026) · H-G9 (AUDITORIA_REGLAS_FITSENSE §7): GENERAL_FITNESS con UNA sesion de
 * 30-35 min. Con la configuracion evaluada (PLAN-REV-2.0) la semana de una sola sesion tiene que llevar, en esa
 * sesion, los cuatro slots de V23 y las cuatro capacidades principales de V25 (STRENGTH, MUSCULAR_ENDURANCE, CARDIO,
 * MOBILITY) con su dosis exacta de V24, y V4 de REV2 exige segundos estimados <= M x 60.
 * <p>
 * Para cada perfil se calcula, con el catalogo elegible real y el estimador real, la COTA INFERIOR de duracion de
 * cualquier propuesta que cumpla V23 + V24 + V25 (la combinacion mas barata de dosis y ejercicios; ignora V15, V18 y
 * demas, que solo la pueden subir) y se pasa esa propuesta minima por el validador real. Despues se genera de verdad
 * con la IA apagada (failsafe) para ver que publica el respaldo. Imprime DIAG_HG9. Sin red ni Replicate.
 */
@Testcontainers
@SpringBootTest(properties = "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef")
@DisplayName("Auditoria H-G9 · GENERAL_FITNESS con una sesion de 30-35 min: factibilidad de V4 + V23 + V24 + V25")
class HG9GeneralFitnessIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired UserProfileCommandService perfiles;
    @Autowired SessionDurationEstimator estimador;
    @Autowired PlanDraftValidator validador;
    @Autowired JdbcTemplate jdbc;

    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final LocalDate S1 = RelojPruebas.SEMANA_REFERENCIA;
    static final List<String> GIMNASIO = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine",
            "bench", "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine");
    static final List<Capability> RESISTIDAS = List.of(Capability.STRENGTH, Capability.MUSCULAR_ENDURANCE,
            Capability.HYPERTROPHY);

    record Fila(String nivel, int minutos, long cotaSegundos, long limiteSegundos, List<String> codigos,
                String generacion) {}

    @Test
    void factibilidadPorNivelYMinutos() {
        var filas = new ArrayList<Fila>();
        for (var nivel : List.of(FitnessLevel.BEGINNER, FitnessLevel.INTERMEDIATE, FitnessLevel.ADVANCED))
            for (int minutos : List.of(30, 35, 40, 45))
                filas.add(caso(nivel, minutos));
        filas.forEach(f -> System.out.println("DIAG_HG9 " + f));
        for (var f : filas) {
            boolean cabe = f.cotaSegundos() <= f.limiteSegundos();
            // La cota es un minimo: si no cabe, ninguna propuesta de la IA puede ser valida.
            if (!cabe) assertTrue(f.codigos().stream().anyMatch(c -> c.startsWith("V4")),
                    "sin espacio, la propuesta minima la rechaza V4: " + f);
            assertNotNull(f.generacion(), f.toString());
        }
    }

    private Fila caso(FitnessLevel nivel, int minutos) {
        long u = participante(nivel, minutos);
        var ctx = planComandos.prepare(new GenerateWeeklyPlanCommand(u, S1, null, true, null, null)).context();
        String level = ctx.profile().fitnessLevel();
        var elegibles = ctx.availableExercises();
        var limites = ctx.prescription() == null ? null : ctx.prescription().repLimits();

        // Por slot y capacidad resistida, la colocacion mas barata en segundos.
        var porSlot = new LinkedHashMap<MandatorySlot, Map<Capability, PlanDraft.DraftExercise>>();
        for (var slot : MandatorySlot.ALL) {
            var mejor = new EnumMap<Capability, PlanDraft.DraftExercise>(Capability.class);
            for (var c : elegibles) {
                if (MandatorySlot.of(c).orElse(null) != slot) continue;
                for (var cap : CapabilityPrescriptionPolicy.admisibles(c, "GENERAL_FITNESS", level)) {
                    if (!RESISTIDAS.contains(cap)) continue;
                    var d = CapabilityPrescriptionPolicy.dosis(cap, c, "GENERAL_FITNESS", level, limites);
                    if (d.isEmpty()) continue;
                    var e = d.get().prescribir(c.exerciseId(), null);
                    if (!mejor.containsKey(cap) || segundos(List.of(e), ctx) < segundos(List.of(mejor.get(cap)), ctx))
                        mejor.put(cap, e);
                }
            }
            porSlot.put(slot, mejor);
        }
        var cardio = mejorDe(Capability.CARDIO, elegibles, level, limites, ctx);
        var movilidad = mejorDe(Capability.MOBILITY, elegibles, level, limites, ctx);

        // Combinaciones de capacidad por slot (3^4) con STRENGTH y MUSCULAR_ENDURANCE presentes (V25).
        List<PlanDraft.DraftExercise> minima = null;
        long cota = Long.MAX_VALUE;
        var slots = new ArrayList<>(porSlot.keySet());
        int total = (int) Math.pow(RESISTIDAS.size(), slots.size());
        for (int k = 0; k < total; k++) {
            var lista = new ArrayList<PlanDraft.DraftExercise>();
            var usadas = EnumSet.noneOf(Capability.class);
            int x = k;
            boolean ok = true;
            for (var slot : slots) {
                var cap = RESISTIDAS.get(x % RESISTIDAS.size());
                x /= RESISTIDAS.size();
                var e = porSlot.get(slot).get(cap);
                if (e == null) { ok = false; break; }
                lista.add(e);
                usadas.add(cap);
            }
            if (!ok || !usadas.contains(Capability.STRENGTH) || !usadas.contains(Capability.MUSCULAR_ENDURANCE)) continue;
            if (cardio != null) lista.add(cardio);
            if (movilidad != null) lista.add(movilidad);
            long s = segundos(lista, ctx);
            if (s < cota) { cota = s; minima = lista; }
        }

        var codigos = new ArrayList<String>();
        if (minima != null) {
            var fecha = S1.plusDays(1);
            var w = new PlanDraft.DraftWorkout(fecha, WorkoutFocus.FULL_BODY, "minima",
                    estimador.estimateMinutes(new PlanDraft.DraftWorkout(fecha, WorkoutFocus.FULL_BODY, "x", 0, minima),
                            ctx.prescription()), minima);
            var draft = new PlanDraft(GenerationSource.AI, "cota", "cota", null, "cota", List.of(w));
            try {
                validador.validate(draft, ctx, 30);
            } catch (InvalidPlanDraftException e) {
                for (var p : e.problems()) codigos.add(p.length() > 140 ? p.substring(0, 140) : p);
            }
        }

        String generacion;
        try {
            long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
            var p = jdbc.queryForMap("SELECT generation_source, planning_policy FROM weekly_training_plans WHERE plan_id = ?",
                    plan);
            var ses = jdbc.queryForList("SELECT expected_duration_minutes FROM planned_workouts WHERE plan_id = ?",
                    Integer.class, plan);
            generacion = p.get("generation_source") + "/" + p.get("planning_policy") + " minutos=" + ses;
        } catch (RuntimeException e) {
            generacion = "SIN_PLAN " + e.getClass().getSimpleName() + ": "
                    + String.valueOf(e.getMessage()).substring(0, Math.min(160, String.valueOf(e.getMessage()).length()));
        }
        return new Fila(nivel.name(), minutos, cota, minutos * 60L, codigos, generacion);
    }

    private PlanDraft.DraftExercise mejorDe(Capability cap, List<CandidateExercise> elegibles, String level,
                                            PrescriptionParams.RepLimits limites, PlanGenerationContext ctx) {
        PlanDraft.DraftExercise mejor = null;
        for (var c : elegibles) {
            if (!CapabilityPrescriptionPolicy.admisibles(c, "GENERAL_FITNESS", level).contains(cap)) continue;
            var d = CapabilityPrescriptionPolicy.dosis(cap, c, "GENERAL_FITNESS", level, limites);
            if (d.isEmpty()) continue;
            var e = d.get().prescribir(c.exerciseId(), null);
            if (mejor == null || segundos(List.of(e), ctx) < segundos(List.of(mejor), ctx)) mejor = e;
        }
        return mejor;
    }

    private long segundos(List<PlanDraft.DraftExercise> ejercicios, PlanGenerationContext ctx) {
        return estimador.estimateSeconds(new PlanDraft.DraftWorkout(S1.plusDays(1), WorkoutFocus.FULL_BODY, "x", 0,
                ejercicios), ctx.prescription());
    }

    private long participante(FitnessLevel nivel, int minutos) {
        String email = "auditoria-hg9-" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'HG9', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 6, 15), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("62"), null, nivel, GoalType.GENERAL_FITNESS, "Auditoria H-G9",
                TrainingLocation.GYM, (short) 1, List.of((short) 2), (short) minutos, GIMNASIO, List.of(), "Ninguna",
                true, true, true, true, true));
        return u;
    }
}
