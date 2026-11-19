package main.web.services.fitsense.planning.infrastructure.generation.ai;

import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El desbordamiento silencioso de la generacion inicial.
 * <p>
 * {@code Integer.shortValue()} truncaba sin avisar, y para una dosis eso es peor
 * que fallar: el truncamiento puede producir un valor que PARECE correcto, de
 * modo que una respuesta incorrecta del modelo se volvia una prescripcion que
 * pasaba la validacion de 4D.
 * <p>
 * Estas pruebas van contra el ensamblador de generacion inicial, no contra el de
 * adaptacion: el defecto estaba en los dos y se arreglo en los dos.
 */
class PlanDraftOverflowTest {

    private final PlanDraftFromJsonAssembler assembler =
            new PlanDraftFromJsonAssembler(new JsonSupport());

    // =======================================================================
    // Los valores que se convertian en dosis creibles
    // =======================================================================

    @Test
    @DisplayName("rest_seconds 65626 NO se convierte en 90, que es el descanso de 4D")
    void descansoQueParecia90() {
        var e = assertThrows(InvalidPlanDraftException.class,
                () -> assembler.toDraft(plan(3, 10, 65626), "prueba"));

        assertTrue(mensaje(e).contains("rest_seconds"), () -> e.problems().toString());
        assertTrue(mensaje(e).contains("65626"),
                "el mensaje tiene que llevar el valor ORIGINAL, no el truncado");
        assertFalse(mensaje(e).contains(" 90 "),
                "y no el valor que habria salido del truncamiento");
    }

    @Test
    @DisplayName("planned_sets 65539 NO se convierte en 3")
    void seriesQueParecian3() {
        var e = assertThrows(InvalidPlanDraftException.class,
                () -> assembler.toDraft(plan(65539, 10, 90), "prueba"));

        assertTrue(mensaje(e).contains("planned_sets"), () -> e.problems().toString());
        assertTrue(mensaje(e).contains("65539"), () -> e.problems().toString());
    }

    @Test
    @DisplayName("planned_reps 100000 NO se convierte en un numero negativo")
    void repeticionesQueQuedabanNegativas() {
        // (short) 100000 == -31072.
        var e = assertThrows(InvalidPlanDraftException.class,
                () -> assembler.toDraft(plan(3, 100000, 90), "prueba"));

        assertTrue(mensaje(e).contains("planned_reps"), () -> e.problems().toString());
        assertTrue(mensaje(e).contains("100000"), () -> e.problems().toString());
    }

    // =======================================================================
    // Y lo que debe seguir entrando
    // =======================================================================

    @Test
    @DisplayName("El limite exacto entra: 32767")
    void elLimiteEntra() {
        // Rechazar de mas seria tan danino como truncar de menos: gastaria un
        // reintento de la IA para nada.
        var draft = assembler.toDraft(plan(3, 10, 32767), "prueba");

        assertEquals((short) 32767,
                draft.workouts().get(0).exercises().get(0).restSeconds());
    }

    @Test
    @DisplayName("Una dosis normal entra sin tocarse")
    void dosisNormal() {
        var draft = assembler.toDraft(plan(3, 10, 90), "prueba");

        var ejercicio = draft.workouts().get(0).exercises().get(0);
        assertEquals((short) 3, ejercicio.plannedSets());
        assertEquals((short) 10, ejercicio.plannedReps());
        assertEquals((short) 90, ejercicio.restSeconds());
    }

    @Test
    @DisplayName("Los nulos siguen siendo nulos: son campos opcionales, no errores")
    void losNulosSiguenSiendoNulos() {
        var draft = assembler.toDraft("""
                {"schema_version":"GEN-OUT-1.0","plan_name":"p","total_volume":30,
                 "rationale":"r","workouts":[
                  {"scheduled_date":"2026-10-19","focus_code":"FULL_BODY",
                   "workout_name":"A","expected_duration_minutes":45,"exercises":[
                     {"exercise_id":304,"exercise_order":1,"prescription_type":"DURATION",
                      "planned_sets":null,"planned_reps":null,
                      "planned_duration_seconds":45,"target_load_kg":null,
                      "rest_seconds":null,"notes":null}]}]}
                """, "prueba");

        var ejercicio = draft.workouts().get(0).exercises().get(0);
        assertNull(ejercicio.plannedSets());
        assertNull(ejercicio.plannedReps());
        assertNull(ejercicio.restSeconds());
        assertEquals(45, ejercicio.plannedDurationSeconds());
    }

    @Test
    @DisplayName("Se acumulan los tres desbordamientos, no solo el primero")
    void seAcumulan() {
        // El reintento recibe la lista entera: corregir uno y volver a fallar en
        // el siguiente gastaria el segundo intento para nada.
        var e = assertThrows(InvalidPlanDraftException.class,
                () -> assembler.toDraft(plan(65539, 100000, 65626), "prueba"));

        assertEquals(3, e.problems().size(), () -> e.problems().toString());
    }

    // -----------------------------------------------------------------------

    private static String mensaje(InvalidPlanDraftException e) {
        return String.join(" | ", e.problems());
    }

    /** Un plan de una sesion y un ejercicio, con la dosis que se quiera probar. */
    private static String plan(int sets, int reps, int rest) {
        return """
                {"schema_version":"GEN-OUT-1.0","plan_name":"p","total_volume":30,
                 "rationale":"r","workouts":[
                  {"scheduled_date":"2026-10-19","focus_code":"FULL_BODY",
                   "workout_name":"A","expected_duration_minutes":45,"exercises":[
                     {"exercise_id":304,"exercise_order":1,"prescription_type":"SETS_REPS",
                      "planned_sets":%d,"planned_reps":%d,"planned_duration_seconds":null,
                      "target_load_kg":null,"rest_seconds":%d,"notes":null}]}]}
                """.formatted(sets, reps, rest);
    }
}
