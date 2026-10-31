package main.web.services.fitsense.planning;

import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.MvpDosePolicy;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.services.CapabilityPrescriptionPolicy;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.domain.services.TrainingPlanGenerator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
 * REGRESION de los CASO A de la investigacion de factibilidad 30-120 (MATRIZ_FACTIBILIDAD_30_120.csv): perfiles para
 * los que EXISTE una semana que pasa V1-V25 (testigo hallado por el verificador independiente de
 * {@link FactibilidadDuracionIT}) y el motor de reglas no la construia.
 * <ul>
 *   <li>F0011 LOSE_WEIGHT intermedio 105 min, F0039 INCREASE_STRENGTH principiante 75, F0063 GENERAL_FITNESS
 *       intermedio 75 y F0351 GENERAL_FITNESS intermedio 75 en 6 dias: con el tope de 8 colocaciones lleno, el
 *       constructor de capacidades solo subia series y nunca cambiaba una colocacion corta por otra admisible mas
 *       larga; V20.</li>
 *   <li>F0313 GAIN_MUSCLE intermedio 30 min en 6 dias: el recorte al presupuesto de 4D quitaba el unico ejercicio
 *       de pierna baja del dia LEGS; V15.</li>
 * </ul>
 * Mismo user_id y misma semana que en la matriz: la semilla del motor es user_id x 1.000.003 + dia, asi que el motor
 * genera exactamente la semana que fallaba. Sin IA y sin Replicate. Se exige el plan COMPLETO con el validador de
 * produccion, la duracion declarada igual a la calculada, el piso de V20 sin herencia D8, el techo de V4 y el tope de
 * colocaciones de V24.
 * <p>
 * F0345, F0705, F0777 y F0921 (GENERAL_FITNESS principiante, 75 min) seguian fallando con la primera correccion: el
 * dia solo veia lo ya construido y no cambiaba la unica resistencia muscular del lunes aunque la semana la tuviera el
 * miercoles. Los corrige el repaso semanal.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("Motor de reglas · CASO A de la investigacion 30-120 (existia un plan valido y no se construia)")
class GeneradorReglasCasoAIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static final LocalDate SEMANA = FactibilidadDuracionIT.SEMANA;
    static final SessionDurationEstimator ESTIMADOR = new SessionDurationEstimator();

    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanDraftValidator validador;
    @Autowired @Qualifier("ruleBasedTrainingPlanGenerator") TrainingPlanGenerator reglas;
    @Autowired JdbcTemplate jdbc;

    @ParameterizedTest(name = "{0} {1}/{2}/{3}d/{4} min/{5}")
    @CsvSource({
            "11,  LOSE_WEIGHT,       INTERMEDIATE, 3, 105, GIMNASIO",
            "39,  INCREASE_STRENGTH, BEGINNER,     3, 75,  GIMNASIO",
            "63,  GENERAL_FITNESS,   INTERMEDIATE, 3, 75,  GIMNASIO",
            "351, GENERAL_FITNESS,   INTERMEDIATE, 6, 75,  GIMNASIO",
            "313, GAIN_MUSCLE,       INTERMEDIATE, 6, 30,  GIMNASIO",
            // Segunda vuelta (repaso semanal 2c): la capacidad que sale sigue en OTRO dia de la semana.
            "345, GENERAL_FITNESS,   BEGINNER,     6, 75,  GIMNASIO",
            "705, GENERAL_FITNESS,   BEGINNER,     6, 75,  CASA_MANCUERNAS_BANDAS",
            "777, GENERAL_FITNESS,   BEGINNER,     3, 75,  CASA_PESO_CORPORAL",
            "921, GENERAL_FITNESS,   BEGINNER,     2, 75,  CASA_PESO_CORPORAL",
            // Tercera vuelta: la fuerza de los cuatro slots concentrada en un dia; otro dia que ya tiene ese patron
            // lo lleva con dosis de fuerza y el dia corto cambia la suya por cardio.
            "64,  GENERAL_FITNESS,   INTERMEDIATE, 3, 90,  GIMNASIO"})
    void construyeElPlanQueExiste(long userId, GoalType objetivo, FitnessLevel nivel, int dias, int minutos,
                                  String equipo) {
        jdbc.update("INSERT INTO users (user_id, email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " OVERRIDING SYSTEM VALUE VALUES (?, ?, 'x', 'Factibilidad', now(), now())", userId,
                "caso-a-" + userId + "@pruebas.invalid");
        perfiles.handle(new CreateUserProfileCommand(userId, LocalDate.of(1990, 6, 15), BiologicalSex.FEMALE,
                new BigDecimal("168"), new BigDecimal("68"), null, nivel, objetivo, "factibilidad",
                "GIMNASIO".equals(equipo) ? TrainingLocation.GYM : TrainingLocation.HOME, (short) dias, FactibilidadDuracionIT.DIAS.get(dias), (short) minutos,
                FactibilidadDuracionIT.EQUIPOS.get(equipo), List.of(), "Ninguna", true, true, true, true, true));

        var prepared = planComandos.prepare(new GenerateWeeklyPlanCommand(userId, SEMANA, null, true, null, null));
        var ctx = prepared.context();
        assertEquals(userId, ctx.userId(), "premisa: misma semilla que en la matriz");
        var borrador = reglas.generate(ctx, List.of());
        try {
            validador.validate(borrador, ctx, prepared.divisor());
        } catch (InvalidPlanDraftException e) {
            fail("el motor no construye el plan que existe: " + e.problems());
        }

        long piso = PlanDraftValidator.minimumSessionSeconds(ctx);
        int techo = (int) Math.floor(minutos * 1.15);
        int tope = MvpDosePolicy.aplicaA(objetivo.name()) ? MvpDosePolicy.maxColocaciones(minutos)
                : CapabilityPrescriptionPolicy.MAX_COLOC_DIA;
        assertEquals(ctx.effectiveDaysPerWeek(), borrador.workouts().size(), "V2: misma frecuencia");
        for (PlanDraft.DraftWorkout w : borrador.workouts()) {
            long s = ESTIMADOR.estimateSeconds(w, ctx.prescription());
            assertEquals(ESTIMADOR.estimateMinutes(w, ctx.prescription()), w.expectedDurationMinutes(),
                    "duracion declarada = calculada");
            assertTrue(s >= piso, w.scheduledDate() + ": " + s + " s bajo el piso V20 " + piso + " (sin D8)");
            assertTrue(w.expectedDurationMinutes() <= techo, "techo V4");
            assertTrue(w.exercises().size() <= tope, "tope de colocaciones V24");
        }
    }
}
