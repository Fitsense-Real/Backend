package main.web.services.fitsense.orchestation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link CopiaSinIaIT} con {@code PLAN-REV-2.0} (cierre tecnico, fase 3). Todas las pruebas heredadas se ejecutan tal
 * cual; solo se sustituye la que tiene una premisa de composicion exclusivamente historica.
 * <p>
 * {@link #retiradoNoElegibleNoVuelve}: la historica desactiva en el catalogo el ejercicio RETIRADO por el acortamiento
 * suponiendo que no esta en ninguna otra sesion (asi es en el plan HIST). En el plan REV2 de LOSE_WEIGHT el mismo
 * cardio (253) esta tambien en OTRA sesion conservada. Hasta la fase 4 esa copia fallaba V1 y la semana quedaba
 * NO_FACTIBLE. Con {@code reparaCatalogoInactivo} (REV2) la colocacion inactiva de la otra sesion se sustituye por un
 * equivalente estricto (SUST-CAT-1) o, si no lo hay, la semana no sale y el motivo empieza por «CATALOGO». En ningun caso
 * se publica el ejercicio inactivo, ni vuelve por reincorporacion: la reserva se descarta.
 */
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.progression.enabled=true",
        "fitsense.planning.policy=PLAN-REV-2.0",
        "fitsense.planning.policy.allow-candidates=true"
})
@DisplayName("REV2 · Copia semanal y mantenimiento sin IA: llamadas a la IA por escenario")
class CopiaSinIaRev2IT extends CopiaSinIaIT {

    @Override
    @Test
    @DisplayName("REV2 §6 el retirado deja de ser elegible (catalogo): no vuelve; si sigue en otra sesion, SUST-CAT-1 o"
            + " NO_FACTIBLE explicado; nunca se publica inactivo")
    void retiradoNoElegibleNoVuelve() throws Exception {
        var r = acortarConRetirada();
        long u = r[0], inactivo = r[2];
        boolean enOtraSesion = semana(r[1]).stream().anyMatch(s -> s.getValue().stream().anyMatch(e -> e.id() == inactivo));
        assertEquals(1, jdbc.update("UPDATE exercises SET is_active = false WHERE exercise_id = ?", inactivo));
        var informes = sinReincorporar(u, r[1], 1, 12, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        System.out.println("SIN_IA_REV2 retirado=" + inactivo + " en_otra_sesion=" + enOtraSesion + " informes=" + informes.size());
        // Nunca se publica el ejercicio inactivo despues de desactivarlo.
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM planned_workout_exercises pwe JOIN planned_workouts pw"
                + " ON pw.planned_workout_id = pwe.planned_workout_id JOIN weekly_training_plans p ON p.plan_id = pw.plan_id"
                + " WHERE p.user_id = ? AND p.week_start_date > ? AND pwe.exercise_id = ?", Integer.class, u,
                java.sql.Date.valueOf(S1.plusWeeks(1)), inactivo), "se publico un ejercicio inactivo");
        var primero = jdbc.queryForMap("SELECT status, rejection_reason, conservation_report::text AS informe"
                + " FROM plan_adaptation_attempts WHERE user_id = ? ORDER BY attempt_id LIMIT 1 OFFSET 1", u);
        if (enOtraSesion) {
            String informe = String.valueOf(primero.get("informe")).replace(" ", "");
            boolean sustituido = informe.contains("\"regla\":\"SUST-CAT-1\"") && informe.contains("\"catalogo_inactivo\"");
            boolean explicado = !"SIN_ORDEN".equals(primero.get("status"))
                    && String.valueOf(primero.get("rejection_reason")).startsWith("CATALOGO");
            assertTrue(sustituido || explicado, "ni SUST-CAT-1 ni NO_FACTIBLE explicado: " + primero);
        }
        if (informes.size() > 1 || "SIN_ORDEN".equals(primero.get("status")))
            assertTrue(informes.stream().anyMatch(i -> i.contains("\"estado_reserva\":\"DESCARTADA\"")),
                    "la reserva del inactivo se descarta: " + informes);
    }
}
