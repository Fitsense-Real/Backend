package main.web.services.fitsense.planning;

import main.web.services.fitsense.planning.application.internal.commandservices.PlanningPolicySelector;
import main.web.services.fitsense.planning.application.internal.queryservices.PlanLimitationsService;
import main.web.services.fitsense.planning.domain.exceptions.FuerzaSinTiempoException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cierre PLAN-REV-2.0, fase 2 (alternativa C) en el ciclo real. INCREASE_STRENGTH intermedio y avanzado, gimnasio y
 * casa con mancuernas, 1 dia de 30 min (los 4 perfiles sin plan del barrido con REV2):
 * <ul>
 *   <li>REV2: no se genera; la persona recibe FUERZA_SIN_TIEMPO con las alternativas (32 min o mas dias: el minimo calculado), tambien en
 *       /plan/limitations antes de generar. Con 32 o 35 min o con 2 dias de 30 min SI hay plan con los 4 patrones.</li>
 *   <li>HIST: nada cambia (con la tolerancia historica del 15 % caben los 4 patrones en 30 min).</li>
 *   <li>Casa con peso corporal (sin traccion: V25 exige 3 slots): genera con las dos politicas.</li>
 * </ul>
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("Fase 2 (C): fuerza sin tiempo para los 4 patrones se informa con alternativas; HIST no cambia")
class FuerzaSinTiempoIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanLimitationsService limitaciones;
    @Autowired PlanningPolicySelector selector;
    @Autowired JdbcTemplate jdbc;

    static final LocalDate SEMANA = LocalDate.of(2026, 11, 2);
    static final List<String> GYM = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine", "bench",
            "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine");
    static final List<String> MANCUERNAS = List.of("body weight", "dumbbell", "band");
    static final java.util.concurrent.atomic.AtomicInteger N = new java.util.concurrent.atomic.AtomicInteger();

    long usuario(FitnessLevel nivel, List<String> equipo, int dias, List<Short> disponibles, int minutos) {
        int n = N.incrementAndGet();
        String email = "fuerza" + n + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Fuerza', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 6, 15), BiologicalSex.FEMALE,
                new BigDecimal("168"), new BigDecimal("68"), null, nivel, GoalType.INCREASE_STRENGTH, "fuerza",
                equipo == GYM ? TrainingLocation.GYM : TrainingLocation.HOME, (short) dias, disponibles,
                (short) minutos, equipo, List.of(), "Ninguna", true, true, true, true, true));
        return u;
    }

    /** null si genera; el mensaje si informa FUERZA_SIN_TIEMPO. */
    String generar(long u) {
        var prep = planComandos.prepare(GenerateWeeklyPlanCommand.firstPlan(u, SEMANA));
        try {
            var g = planComandos.generate(prep);
            assertNotNull(g.draft());
            return null;
        } catch (FuerzaSinTiempoException e) {
            return e.getMessage();
        }
    }

    boolean avisoPrevio(long u) {
        return limitaciones.limitationsFor(u).limitations().stream()
                .anyMatch(l -> "TIME".equals(l.type()) && FuerzaSinTiempoException.CODIGO.equals(l.code()));
    }

    @Test
    void cuatroPerfiles() {
        boolean rev2 = selector.activa().id().equals("PLAN-REV-2.0");
        for (var nivel : List.of(FitnessLevel.INTERMEDIATE, FitnessLevel.ADVANCED))
            for (var equipo : List.of(GYM, MANCUERNAS)) {
                long u = usuario(nivel, equipo, 1, List.of((short) 1), 30);
                String r = generar(u);
                if (rev2) {
                    assertNotNull(r, nivel + " " + equipo + ": con REV2 debe informar en lugar de generar");
                    assertTrue(r.contains("32 minutos") && r.contains("4 patrones") && r.contains("mas dias"), r);
                    assertTrue(avisoPrevio(u), "aviso en /plan/limitations antes de generar");
                } else {
                    assertNull(r, "con la historica no cambia: genera");
                    assertFalse(avisoPrevio(u));
                }
            }
    }

    @Test
    void alternativasDeDisponibilidad() {
        for (var nivel : List.of(FitnessLevel.INTERMEDIATE, FitnessLevel.ADVANCED)) {
            assertNull(generar(usuario(nivel, GYM, 1, List.of((short) 1), 32)), "32 min (el minimo que se indica)");
            assertNull(generar(usuario(nivel, GYM, 1, List.of((short) 1), 35)), "35 min: 4 patrones caben");
            assertNull(generar(usuario(nivel, GYM, 2, List.of((short) 1, (short) 4), 30)), "2 dias de 30 min");
            assertNull(generar(usuario(nivel, List.of("body weight"), 1, List.of((short) 1), 30)),
                    "peso corporal: sin traccion, V25 exige 3 slots y caben");
        }
    }
}
