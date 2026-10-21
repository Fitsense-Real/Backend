package main.web.services.fitsense.planning.infrastructure.generation.ai;

import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * La proyeccion de entrada para adaptacion.
 * <p>
 * Lo que de verdad protege estas pruebas no es que hoy falte
 * {@code previous_week}: es que no pueda APARECER manana. Las dos primeras
 * comprueban el conjunto EXACTO de claves, asi que anadir un campo al snapshot
 * general no lo cuela aqui, y anadirlo aqui rompe la prueba hasta que alguien
 * lo ponga en la lista a proposito.
 */
class SessionFixInputSnapshotTest {

    private final JsonSupport json = new JsonSupport();

    private static final LocalDate L2 = LocalDate.of(2026, 10, 19);

    // =======================================================================
    // La lista blanca, comprobada como conjunto exacto
    // =======================================================================

    @Test
    @DisplayName("Las claves de primer nivel son EXACTAMENTE las admitidas")
    void clavesDePrimerNivelExactas() {
        var claves = clavesDe(json.write(SessionFixInputSnapshot.of(completo())));

        assertEquals(Set.of("schema_version", "principles_version", "prompt_version",
                        "user", "constraints", "available_exercises", "safety_notes"),
                claves,
                "si esto falla, alguien anadio o quito un bloque: decidelo a proposito");
    }

    @Test
    @DisplayName("Las claves de constraints son EXACTAMENTE las admitidas")
    void clavesDeConstraintsExactas() {
        var raiz = leer(json.write(SessionFixInputSnapshot.of(completo())));
        @SuppressWarnings("unchecked")
        var constraints = (Map<String, Object>) raiz.get("constraints");

        assertEquals(Set.of("week_number", "week_start_date", "week_end_date",
                        "available_days", "session_minutes", "training_location",
                        "max_difficulty_level", "max_reps", "target_reps", "target_sets",
                        "min_session_minutes", "min_exercises_per_session",
                        "max_rest_seconds", "dosis_por_clase", "max_colocaciones_dia",
                        "minutos_utiles_dia"),
                constraints.keySet(),
                "suggested_split, days_per_week, reduce_volume_caps y slots_exigibles"
                        + " se quedan fuera a proposito");
    }

    // =======================================================================
    // Lo que NO viaja
    // =======================================================================

    @Test
    @DisplayName("No viaja previous_week, ni el bloque ni sus agregados")
    void nadaDePreviousWeek() {
        var jsonTexto = json.write(SessionFixInputSnapshot.of(completo()));

        for (var clave : List.of("previous_week", "weighted_adherence_pct",
                "body_part_distribution", "average_session_rpe", "total_volume")) {
            assertFalse(jsonTexto.contains(clave), () -> clave + " no puede viajar");
        }
        // Y los datos, no solo las claves.
        assertFalse(jsonTexto.contains("57.5"), "la carga usada en una conservada");
        assertFalse(jsonTexto.contains("2026-10-12"), "la fecha de una conservada");
    }

    @Test
    @DisplayName("No viaja el mecanismo porcentual ni la division de la semana")
    void nadaDeLaSemanaCompleta() {
        var jsonTexto = json.write(SessionFixInputSnapshot.of(completo()));

        for (var clave : List.of("adjustment", "distribution_hint", "load_change_pct",
                "suggested_split", "reduce_volume_caps", "slots_exigibles",
                "days_per_week")) {
            assertFalse(jsonTexto.contains(clave), () -> clave + " no puede viajar");
        }
    }

    // =======================================================================
    // Lo que SI viaja, y con que version
    // =======================================================================

    @Test
    @DisplayName("La version de reglas registrada es la de ADAPTACION, no la de generacion")
    void laVersionEsLaDeAdaptacion() {
        var proyeccion = SessionFixInputSnapshot.of(completo());

        // El snapshot completo traia GEN-IN-1.6. Registrar esa version en una
        // adaptacion diria que el modelo recibio las reglas de generacion
        // inicial, que es falso, y la trazabilidad de la tesis depende de esto.
        assertEquals(SessionFixPromptBuilder.VERSION, proyeccion.promptVersion());
        assertNotEquals("GEN-IN-1.6", proyeccion.promptVersion());
        assertEquals(SessionFixInputSnapshot.VERSION, proyeccion.schemaVersion());
        assertEquals("PRIN-1.0", proyeccion.principlesVersion(),
                "la version de principios SI es la misma: el texto es el mismo");
    }

    @Test
    @DisplayName("El catalogo y el perfil viajan intactos, sin recalcularse")
    void catalogoYPerfilIntactos() {
        var completo = completo();
        var proyeccion = SessionFixInputSnapshot.of(completo);

        // Misma instancia: no se rehace el barajado por grupo muscular ni se
        // recalculan min_reps, movement_pattern, slot ni clase_dosis.
        assertSame(completo.availableExercises(), proyeccion.availableExercises());
        assertSame(completo.user(), proyeccion.user());
        assertEquals("cuidado con la rodilla", proyeccion.safetyNotes());
    }

    @Test
    @DisplayName("Los limites que la correccion necesita siguen ahi")
    void losLimitesNecesariosSiguen() {
        var c = SessionFixInputSnapshot.of(completo()).constraints();

        assertEquals(L2, c.weekStartDate(), "la regla de la fecha los necesita");
        assertEquals(L2.plusDays(6), c.weekEndDate());
        assertEquals(45, c.sessionMinutes(), "la regla del +15 % lo necesita");
        assertEquals(31, c.minSessionMinutes());
        assertEquals(2, c.minExercisesPerSession());
        assertEquals(12, c.maxReps());
        assertEquals(1, c.dosisPorClase().size(), "4D lo necesita");
        assertEquals(38.25, c.minutosUtilesDia());
    }

    // =======================================================================
    // Entradas imposibles
    // =======================================================================

    @Test
    @DisplayName("Sin datos de entrada no se proyecta nada")
    void sinDatos() {
        var e = assertThrows(IllegalArgumentException.class,
                () -> SessionFixInputSnapshot.of(null));
        assertTrue(e.getMessage().contains("proyectar"), e::getMessage);
    }

    @Test
    @DisplayName("Sin restricciones no se puede pedir una correccion")
    void sinRestricciones() {
        var sinLimites = new PlanInputSnapshot("S", "P", "GEN-IN-1.6",
                null, null, null, null, List.of(), null);

        var e = assertThrows(IllegalArgumentException.class,
                () -> SessionFixInputSnapshot.of(sinLimites));
        assertTrue(e.getMessage().contains("limites"), e::getMessage);
    }

    // =======================================================================
    // Material
    // =======================================================================

    private Map<String, Object> leer(String texto) {
        @SuppressWarnings("unchecked")
        var mapa = (Map<String, Object>) json.read(texto, Map.class);
        return mapa;
    }

    private Set<String> clavesDe(String texto) {
        return leer(texto).keySet();
    }

    /** El snapshot de generacion inicial, con TODO lo que no debe proyectarse. */
    private static PlanInputSnapshot completo() {
        var constraints = new PlanInputSnapshot.Constraints(
                (short) 2, L2, L2.plusDays(6), 3,
                List.of((short) 1, (short) 3, (short) 5), 45, "GYM", 3,
                12, 10, 3, 31, 2, 120,
                List.of(new PlanInputSnapshot.SuggestedSession(
                        L2, "FULL_BODY", List.of("BACK", "CHEST"), 2, 3)),
                List.of(new PlanInputSnapshot.ReduceVolumeCap(740L, 3, 10, "V19")),
                List.of(new PlanInputSnapshot.DosisPorClase("BASICO", 3, 10, 90, 7.5)),
                4, 38.25, List.of("PULL", "PUSH_H", "SQUAT", "HINGE"));

        var previousWeek = new PlanInputSnapshot.PreviousWeek(
                new BigDecimal("66.67"), new BigDecimal("6.5"), 480,
                Map.of("BACK", 1, "CHEST", 1),
                List.of(new PlanInputSnapshot.PreviousWorkout(
                        LocalDate.of(2026, 10, 12), "FULL_BODY", "COMPLETED", null,
                        true, (short) 7, new BigDecimal("100.00"),
                        List.of(new PlanInputSnapshot.PreviousExercise(740L, "Dominadas",
                                "SETS_REPS", (short) 3, (short) 10, null, (short) 3, 30,
                                null, new BigDecimal("57.5"), new BigDecimal("100.00"),
                                "COMPLETED", null)))));

        return new PlanInputSnapshot("GEN-IN-SCHEMA", "PRIN-1.0", "GEN-IN-1.6",
                new PlanInputSnapshot.User(30, "M", new BigDecimal("175"),
                        new BigDecimal("75"), new BigDecimal("72"), "INTERMEDIATE",
                        "GAIN_MUSCLE", "ganar musculo", null, List.of("body weight")),
                constraints,
                new PlanInputSnapshot.Adjustment(List.of("NONE"), null, null, null,
                        0.0, 0.0, 3, "baja adherencia", "LACK_OF_TIME",
                        "reparte mejor la semana"),
                previousWeek,
                List.of(new PlanInputSnapshot.AvailableExercise(740L, "Dominadas", "BACK",
                        "body weight", 2, "SETS_REPS", "dorsal", 8, "PULL", "PULL",
                        "BASICO")),
                "cuidado con la rodilla");
    }
}
