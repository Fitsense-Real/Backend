package main.web.services.fitsense.planning;

import main.web.services.fitsense.planning.domain.exceptions.PlanGenerationFailedException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.MandatorySlot;
import main.web.services.fitsense.planning.domain.model.valueobjects.MvpDosePolicy;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.planning.domain.services.CapabilityPrescriptionPolicy;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.generation.rules.RuleBasedTrainingPlanGenerator;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
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
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
/**
 * 4D v3.2 EN V24 (decision del 31/10/2026, opcion B): la serie adicional del relleno de zona
 * pequena (2x13 -> 3x13) solo se admite si sin ella la sesion no llega al minimo de V20, y ninguna
 * otra clase gana series. Validador de produccion (4D en ENFORCE) sobre el plan que publica el motor
 * para U06 (GAIN_MUSCLE avanzado, gimnasio, 5 dias, 60 min), catalogo vigente, sin IA.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("4D v3.2: V24 admite la serie adicional del relleno de zona pequena solo cuando hace falta")
class Adenda4Dv32IT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired RuleBasedTrainingPlanGenerator motor;
    @Autowired PlanDraftValidator validador;
    @Autowired SessionDurationEstimator estimador;
    @Autowired UserProfileCommandService perfiles;
    @Autowired JdbcTemplate jdbc;

    static final LocalDate S1 = LocalDate.of(2026, 11, 2);

    @Test
    @DisplayName("la 3x13 necesaria pasa; quitarla rompe V20; una de mas, o en otra clase, la rechaza V24")
    void serieAdicionalSoloSiHaceFalta() {
        long u = participante();
        var ctx = planComandos.prepare(new GenerateWeeklyPlanCommand(u, S1, null, true, null, null)).context();
        var borrador = motor.generate(ctx, List.of());
        assertEquals(List.of(), problemas(borrador, ctx), "el plan del motor es valido");
        Map<Long, CandidateExercise> elegibles = ctx.availableExercises().stream()
                .collect(Collectors.toMap(CandidateExercise::exerciseId, Function.identity(), (a, b) -> a));
        long piso = PlanDraftValidator.minimumSessionSeconds(ctx);

        // 1 · Hay al menos una 3x13 y es de relleno de zona pequena; quitarla deja la sesion bajo V20.
        int sesion = -1, pos = -1;
        for (int i = 0; i < borrador.workouts().size() && sesion < 0; i++)
            for (int k = 0; k < borrador.workouts().get(i).exercises().size(); k++) {
                var e = borrador.workouts().get(i).exercises().get(k);
                var d = dosis(elegibles, e);
                if (e.plannedSets() > d.series()) {
                    assertTrue(d.ampliable(), e.exerciseId() + " sube series sin ser zona pequena: " + d.describe());
                    sesion = i;
                    pos = k;
                    break;
                }
            }
        assertTrue(sesion >= 0, "U06 necesita al menos una serie adicional");
        var sin = cambiar(borrador, sesion, pos, (short) 2);
        assertTrue(problemas(sin, ctx).stream().anyMatch(p -> p.startsWith("V20")),
                "sin la serie adicional la sesion no llega al piso: " + problemas(sin, ctx));

        // 2 · En una sesion que ya llega al piso, una 3x13 de zona pequena que sobra: V24 la rechaza.
        boolean probada = false;
        for (int i = 0; i < borrador.workouts().size() && !probada; i++) {
            var w = borrador.workouts().get(i);
            if (estimador.estimateSeconds(w, ctx.prescription()) < piso) continue;
            for (int k = 0; k < w.exercises().size(); k++) {
                var e = w.exercises().get(k);
                var d = dosis(elegibles, e);
                if (!d.ampliable() || e.plannedSets() != d.series()) continue;
                var conMas = cambiar(borrador, i, k, d.seriesMax());
                if (estimador.estimateSeconds(conMas.workouts().get(i), ctx.prescription()) > ctx.profile().maxSessionMinutes() * 60L)
                    continue;
                var p = problemas(conMas, ctx);
                assertTrue(p.stream().anyMatch(x -> x.startsWith("V24") && x.contains("serie adicional de 4D v3.2")
                        && x.contains("ejercicio " + e.exerciseId())), p.toString());
                probada = true;
                break;
            }
        }
        assertTrue(probada, "hay una sesion con un relleno de zona pequena a 2x13 que ya llega al piso");

        // 3 · Ninguna otra clase gana series: un estructural 3x10 -> 4x10 sigue siendo incumplimiento.
        for (int i = 0; i < borrador.workouts().size(); i++)
            for (int k = 0; k < borrador.workouts().get(i).exercises().size(); k++) {
                var e = borrador.workouts().get(i).exercises().get(k);
                var d = dosis(elegibles, e);
                if (d.ampliable() || d.relleno()) continue;
                var p = problemas(cambiar(borrador, i, k, (short) (d.series() + 1)), ctx);
                assertTrue(p.stream().anyMatch(x -> x.startsWith("V24") && x.contains("4D v2 exige")
                        && x.contains("ejercicio " + e.exerciseId())), p.toString());
                return;
            }
        fail("sin estructural en el plan");
    }

    private MvpDosePolicy.Dosis dosis(Map<Long, CandidateExercise> elegibles, PlanDraft.DraftExercise e) {
        var c = elegibles.get(e.exerciseId());
        return MvpDosePolicy.dosisPara(MandatorySlot.of(c), c.bodyPartCode()).orElseThrow();
    }

    private static PlanDraft cambiar(PlanDraft p, int sesion, int pos, short series) {
        var sesiones = new ArrayList<>(p.workouts());
        var w = sesiones.get(sesion);
        var ej = new ArrayList<>(w.exercises());
        var e = ej.get(pos);
        ej.set(pos, new PlanDraft.DraftExercise(e.exerciseId(), e.prescriptionType(), series, e.plannedReps(),
                e.plannedDurationSeconds(), e.targetLoadKg(), e.restSeconds(), e.notes()));
        sesiones.set(sesion, new PlanDraft.DraftWorkout(w.scheduledDate(), w.focus(), w.name(),
                w.expectedDurationMinutes(), ej));
        return new PlanDraft(p.source(), p.modelName(), p.planName(), p.declaredTotalVolume(), p.rationale(), sesiones);
    }

    private List<String> problemas(PlanDraft draft, PlanGenerationContext ctx) {
        try {
            validador.validate(draft, ctx, 30);
            return List.of();
        } catch (InvalidPlanDraftException e) {
            return e.problems();
        }
    }

    private long participante() {
        String email = "adenda432-" + System.nanoTime() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1996, 1, 17), BiologicalSex.MALE,
                new BigDecimal("180"), new BigDecimal("82"), null, FitnessLevel.ADVANCED, GoalType.GAIN_MUSCLE,
                "U06", TrainingLocation.GYM, (short) 5,
                List.of((short) 1, (short) 3, (short) 4, (short) 6, (short) 7), (short) 60,
                GeneracionReferenciaBase.GIMNASIO, List.of(), "Ninguna", true, true, true, true, true));
        return u;
    }
}
