package main.web.services.fitsense.profiling;

import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.profiling.domain.exceptions.UserProfileLockedException;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.profiling.interfaces.rest.UserProfileController;
import main.web.services.fitsense.profiling.interfaces.rest.resources.UpdateUserProfileResource;
import main.web.services.fitsense.profiling.interfaces.rest.resources.UserProfileResource;
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
import main.web.services.fitsense.shared.interfaces.rest.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regla del estudio (decision del 30/10/2026): lo que la persona declaro al registrarse se mantiene
 * durante todo el estudio. Antes del primer plan el perfil se puede corregir; desde el primer plan,
 * ningun dato cambia (422) y el perfil guardado queda intacto. Cierra el pendiente del cambio de
 * dias o equipamiento a mitad del estudio (VALIDACION_MULTISEMANA.md §4).
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("Perfil fijo durante el estudio: se corrige antes del primer plan, despues no")
class PerfilCongeladoIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired PlanningContextFacade planning;
    @Autowired UserProfileCommandService perfiles;
    @Autowired UserProfileController perfilRest;
    @Autowired UserRepository usuarios;
    @Autowired JdbcTemplate jdbc;

    static final LocalDate S1 = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
    private static int secuencia;

    @Test
    @DisplayName("Antes del primer plan el perfil se corrige; despues cualquier cambio da 422 y nada se guarda")
    void fijoDesdeElPrimerPlan() {
        long u = participante();

        // Antes del primer plan: se puede corregir.
        var corregido = perfilRest.update(principal(u), cambio(u, "71", List.of("body weight", "dumbbell"),
                List.of((short) 1, (short) 3, (short) 5))).getBody();
        assertNotNull(corregido);
        assertEquals(0, new BigDecimal("71").compareTo(corregido.currentWeightKg()));

        planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null)
                .orElseThrow(() -> new AssertionError("sin plan"));
        var antes = perfilRest.get(principal(u)).getBody();

        // Dias y equipamiento (lo que dejaba la semana siguiente sin plan), y tambien solo el peso.
        var ex = assertThrows(UserProfileLockedException.class, () -> perfilRest.update(principal(u),
                cambio(u, "71", List.of("body weight"), List.of((short) 2, (short) 4, (short) 6))));
        assertThrows(UserProfileLockedException.class, () -> perfilRest.update(principal(u),
                cambio(u, "69", List.of("body weight", "dumbbell"), List.of((short) 1, (short) 3, (short) 5))));

        assertEquals(antes, perfilRest.get(principal(u)).getBody(), "el perfil guardado no cambia");
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY,
                new GlobalExceptionHandler().handleDomainRule(ex).getStatusCode());
    }

    private UpdateUserProfileResource cambio(long u, String peso, List<String> equipamiento, List<Short> dias) {
        UserProfileResource p = perfilRest.get(principal(u)).getBody();
        return new UpdateUserProfileResource(
                null, p.heightCm(), new BigDecimal(peso), p.targetWeightKg(),
                FitnessLevel.valueOf(p.fitnessLevel()), GoalType.valueOf(p.goalType()), p.goalText(),
                TrainingLocation.valueOf(p.trainingLocation()), p.daysPerWeek(), dias,
                p.sessionMinutes(), equipamiento, p.blockedExerciseIds(), p.healthNotes(),
                null, null, null, null, null);
    }

    private long participante() {
        String email = "perfil-fijo-" + (++secuencia) + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 1, 15), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("70"), null, FitnessLevel.BEGINNER,
                GoalType.GENERAL_FITNESS, "perfil fijo", TrainingLocation.HOME, (short) 3,
                List.of((short) 1, (short) 3, (short) 5), (short) 45, List.of("body weight", "dumbbell"),
                List.of(), "Ninguna", true, true, true, true, true));
        return u;
    }

    private UserDetailsImpl principal(long u) {
        return UserDetailsImpl.build(usuarios.findById(u).orElseThrow());
    }
}
