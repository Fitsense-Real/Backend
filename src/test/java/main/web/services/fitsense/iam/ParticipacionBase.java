package main.web.services.fitsense.iam;

import main.web.services.fitsense.iam.interfaces.acl.IamContextFacade;
import main.web.services.fitsense.planning.application.internal.generation.PlanGenerationOperationService;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Apoyo comun de las pruebas de la politica de participacion (B2.4, P0-04). */
abstract class ParticipacionBase {

    static final String ZONA = "America/Lima";

    @Autowired JdbcTemplate jdbc;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanGenerationOperationService operaciones;
    @Autowired IamContextFacade iam;

    /** Cuenta con perfil: participante, participante retirado o no participante. */
    long cuenta(boolean participante, boolean retirado) {
        String email = "participacion-" + UUID.randomUUID() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, timezone, is_study_participant,"
                        + " consent_granted_at, enrolled_at, withdrawn_at) VALUES (?, 'x', 'Sintetico', ?, ?, ?, ?, ?)",
                email, ZONA, participante, participante ? java.sql.Timestamp.valueOf("2026-11-01 10:00:00") : null,
                participante ? java.sql.Timestamp.valueOf("2026-11-01 10:00:00") : null,
                retirado ? java.sql.Timestamp.valueOf("2026-11-10 10:00:00") : null);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfil(u);
        return u;
    }

    /** Perfil sintetico completo (casa, 3 dias de 45 min, peso corporal). */
    void perfil(long u) {
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 3, 10), BiologicalSex.FEMALE,
                new BigDecimal("164"), new BigDecimal("63"), null, FitnessLevel.BEGINNER, GoalType.GENERAL_FITNESS,
                "Sintetico", TrainingLocation.HOME, (short) 3, List.of((short) 1, (short) 3, (short) 5), (short) 45,
                List.of("body weight"), List.of(), null, true, true, true, false, false));
    }

    /** Generacion inicial asincrona por el servicio real (IA apagada: motor de reglas). */
    String generar(long u) {
        var id = operaciones.request(u).operation().getId();
        operaciones.claimNext();
        operaciones.execute(id);
        return jdbc.queryForObject("SELECT status FROM plan_generation_operations WHERE operation_id = ?",
                String.class, id);
    }
}
