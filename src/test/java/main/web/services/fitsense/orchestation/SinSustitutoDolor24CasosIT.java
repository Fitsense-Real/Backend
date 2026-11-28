package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

/**
 * LABORATORIO (opt-in): los 22 PERFILES de los 24 casos sin sustituto del barrido (el 135 en sesiones PUSH de casa con
 * mancuernas y bandas), uno a uno y de extremo a extremo: plan real, dolor identificado en el 135, ciclo semanal sin IA
 * y una semana mas. La estrategia NO se fija aqui: la da {@code -Dfitsense.adaptation.difficulty-guard} (off, on,
 * preferencia), para comparar las tres con el mismo codigo. Las invariantes de seguridad de SinSustitutoDolorBase se
 * comprueban en cada perfil; el desenlace se escribe en {@code fitsense.validacion.salida}/flujo_135.csv.
 * <p>
 * {@code mvnw verify -Pintegration -Dit.test=SinSustitutoDolor24CasosIT -Dfitsense.validacion.24=true
 * -Dfitsense.validacion.salida=<dir> -Dfitsense.adaptation.difficulty-guard=on}
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.validacion.24", matches = "true")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("Laboratorio: los 22 perfiles de los 24 casos del 135 sin sustituto, de extremo a extremo")
class SinSustitutoDolor24CasosIT extends SinSustitutoDolorBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    /** La semana del barrido (ValidacionEstrategiasDificultadIT): los planes dependen de la semana. */
    static final java.time.LocalDate SEMANA_DEL_BARRIDO = java.time.LocalDate.of(2026, 11, 2);

    record Perfil(String id, GoalType objetivo, FitnessLevel nivel, int dias, int minutos) {}

    static final GoalType LW = GoalType.LOSE_WEIGHT, GM = GoalType.GAIN_MUSCLE, FU = GoalType.INCREASE_STRENGTH,
            GF = GoalType.GENERAL_FITNESS;
    static final FitnessLevel I = FitnessLevel.INTERMEDIATE, A = FitnessLevel.ADVANCED;

    static final List<Perfil> PERFILES = List.of(
            new Perfil("R0487", LW, I, 6, 60), new Perfil("R0488", LW, I, 6, 75), new Perfil("R0502", GM, A, 6, 45),
            new Perfil("R0503", GM, A, 6, 60), new Perfil("R0515", FU, A, 6, 60), new Perfil("R0521", GF, I, 6, 30),
            new Perfil("R0524", GF, I, 6, 75), new Perfil("R0527", GF, A, 6, 60), new Perfil("R0535", LW, I, 7, 60),
            new Perfil("R0536", LW, I, 7, 75), new Perfil("R0545", GM, I, 7, 30), new Perfil("R0548", GM, I, 7, 75),
            new Perfil("R0559", FU, I, 7, 60), new Perfil("R0560", FU, I, 7, 75), new Perfil("R0561", FU, A, 7, 30),
            new Perfil("R0562", FU, A, 7, 45), new Perfil("R0563", FU, A, 7, 60), new Perfil("R0564", FU, A, 7, 75),
            new Perfil("R0570", GF, I, 7, 45), new Perfil("R0573", GF, A, 7, 30), new Perfil("R0574", GF, A, 7, 45),
            new Perfil("R0576", GF, A, 7, 75));

    @Test
    @DisplayName("ninguno de los 22 perfiles vuelve a recibir el 135; el ciclo termina en todos")
    void veintidosPerfiles() throws Exception {
        for (var p : PERFILES) {
            // La semilla del motor de reglas es userId x 1.000.003 + semana: el perfil Rnnnn del barrido fue el
            // usuario nnnn. Se rellenan usuarios hasta que el siguiente tenga ese id.
            long objetivoId = Long.parseLong(p.id().substring(1));
            long ultimo = jdbc.queryForObject("SELECT COALESCE(MAX(user_id), 0) FROM users", Long.class);
            while (ultimo < objetivoId - 1)
                ultimo = jdbc.queryForObject("INSERT INTO users (email, password_hash, first_name,"
                        + " consent_granted_at, enrolled_at) VALUES (?, 'x', 'Relleno', now(), now()) RETURNING user_id",
                        Long.class, "relleno" + java.util.UUID.randomUUID() + "@pruebas.invalid");
            long u = participante(p.objetivo(), p.nivel(), p.dias(), p.minutos());
            long plan;
            try {
                plan = planning.generateWeeklyPlan(u, SEMANA_DEL_BARRIDO, PlanAdjustment.none(), null, null).orElseThrow();
            } catch (RuntimeException e) {
                // El barrido usa el borrador del motor de reglas SIN validar; el ciclo real lo valida (V1-V25).
                var linea = new java.util.LinkedHashMap<String, Object>();
                linea.put("limite", v8c.limitaDificultad() ? "on" : "off");
                linea.put("estrategia", v8c.estrategiaSustitucion().name());
                linea.put("config", p.id() + " " + List.of(p.objetivo(), p.nivel(), p.dias(), p.minutos()));
                linea.put("decision", "GENERACION_INICIAL_NO_VALIDA (independiente del limite): "
                        + String.valueOf(e.getMessage()).replace(';', ',').replace('\n', ' ').substring(0,
                        Math.min(300, String.valueOf(e.getMessage()).length())));
                escribir(linea);
                continue;
            }
            var sesiones = jdbc.queryForList("SELECT DISTINCT pw.planned_workout_id, pw.scheduled_date FROM planned_workouts pw"
                    + " JOIN planned_workout_exercises pwe ON pwe.planned_workout_id = pw.planned_workout_id WHERE"
                    + " pw.plan_id = ? AND pwe.exercise_id = ? ORDER BY pw.scheduled_date", plan, EJ);
            String etiqueta = p.id() + (u == objetivoId ? "" : " (usuario " + u + ", no " + objetivoId + ")") + " " + List.of(p.objetivo(), p.nivel(), p.dias(), p.minutos())
                    + " sesiones_con_135=" + sesiones.size();
            if (sesiones.isEmpty()) {
                var linea = new java.util.LinkedHashMap<String, Object>();
                linea.put("limite", v8c.limitaDificultad() ? "on" : "off");
                linea.put("estrategia", v8c.estrategiaSustitucion().name());
                linea.put("config", etiqueta);
                linea.put("decision", "NO_REPRODUCIDO: el plan real de esta semana no lleva el 135");
                escribir(linea);
                continue;
            }
            long sesion = ((Number) sesiones.get(0).get("planned_workout_id")).longValue();
            System.out.println("CASO_135 " + recorrer(new Caso(u, etiqueta, plan, sesion), SEMANA_DEL_BARRIDO));
        }
    }
}
