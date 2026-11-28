package main.web.services.fitsense.planning.domain.services.internal;

import main.web.services.fitsense.planning.domain.model.valueobjects.PreviousWeekSummary;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.planning.domain.services.WeekDerivationBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V31: la semana derivada conserva la identidad de cada sesion.
 * <p>
 * Los cuatro escenarios exigidos: semana sin cambios, sesion reprogramada,
 * sesion eliminada y adaptacion ampliada con dos sesiones modificadas.
 */
class WeekDerivationBuilderTest {

    private static final LocalDate L1 = LocalDate.of(2026, 10, 12);   // semana 1
    private static final LocalDate L2 = LocalDate.of(2026, 10, 19);   // semana 2

    private final WeekDerivationBuilder builder = new WeekDerivationBuilder();

    // -----------------------------------------------------------------------
    // 1. Semana sin cambios
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Semana sin cambios: tres sesiones derivadas, ninguna huerfana")
    void semanaSinCambios() {
        var r = builder.derive(semanaDeTres(), List.of(101L, 102L, 103L),
                List.of(L2, L2.plusDays(2), L2.plusDays(4)));

        assertEquals(3, r.workouts().size());
        assertTrue(r.conservaTodas());
        assertEquals(List.of(101L, 102L, 103L),
                r.workouts().stream().map(d -> d.sourcePlannedWorkoutId()).toList());
    }

    @Test
    @DisplayName("La copia conserva la dosis COMPLETA, descanso incluido")
    void laCopiaConservaLaDosis() {
        var r = builder.derive(semanaDeTres(), List.of(101L), List.of(L2));

        var ejercicios = r.workouts().get(0).workout().exercises();
        assertEquals(2, ejercicios.size());

        var primero = ejercicios.get(0);
        assertEquals(740L, primero.exerciseId());
        assertEquals(PrescriptionType.SETS_REPS, primero.prescriptionType());
        assertEquals((short) 3, primero.plannedSets());
        assertEquals((short) 10, primero.plannedReps());
        assertEquals((short) 90, primero.restSeconds(),
                "el descanso es parte de la dosis que 4D exige exacta");
        assertEquals(new BigDecimal("20.00"), primero.targetLoadKg());
    }

    @Test
    @DisplayName("Copia la PRESCRIPCION, no lo que la persona ejecuto")
    void copiaLoPrescritoNoLoEjecutado() {
        // En la semana anterior el ejercicio 740 se prescribio 3x10 y la persona
        // hizo 2 series. La semana derivada tiene que volver a prescribir 3x10:
        // un registro de ejecucion parcial es evidencia, no una prescripcion.
        var r = builder.derive(semanaDeTres(), List.of(101L), List.of(L2));

        assertEquals((short) 3, r.workouts().get(0).workout().exercises().get(0).plannedSets());
    }

    // -----------------------------------------------------------------------
    // 2. Sesion reprogramada
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Sesion reprogramada: cambia la fecha y el origen se conserva igual")
    void sesionReprogramada() {
        // El miercoles pasa a jueves. Es el caso de SCHEDULE_CHANGE.
        var r = builder.derive(semanaDeTres(), List.of(101L, 102L, 103L),
                List.of(L2, L2.plusDays(3), L2.plusDays(4)));

        var segunda = r.workouts().get(1);
        assertEquals(102L, segunda.sourcePlannedWorkoutId(),
                "mover el dia no debe perder de vista de donde viene");
        assertEquals(L2.plusDays(3), segunda.workout().scheduledDate());
        // Y la sesion sigue siendo la misma: mismo enfoque, mismo nombre.
        assertEquals(WorkoutFocus.FULL_BODY, segunda.workout().focus());
        assertEquals("Cuerpo completo B", segunda.workout().name());
    }

    @Test
    @DisplayName("Tres sesiones del mismo enfoque: el orden de origen no se confunde")
    void tresSesionesDelMismoEnfoque() {
        // Las tres son FULL_BODY, que es el caso donde emparejar por enfoque
        // asociaria con la equivocada. Se reordenan a proposito.
        var r = builder.derive(semanaDeTres(), List.of(103L, 101L, 102L),
                List.of(L2, L2.plusDays(2), L2.plusDays(4)));

        assertEquals(List.of(103L, 101L, 102L),
                r.workouts().stream().map(d -> d.sourcePlannedWorkoutId()).toList());
        assertEquals("Cuerpo completo C", r.workouts().get(0).workout().name());
        assertEquals("Cuerpo completo A", r.workouts().get(1).workout().name());
    }

    // -----------------------------------------------------------------------
    // 3. Sesion eliminada
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Sesion eliminada: la que no se conserva queda declarada, no implicita")
    void sesionEliminada() {
        var r = builder.derive(semanaDeTres(), List.of(101L, 103L),
                List.of(L2, L2.plusDays(3)));

        assertEquals(2, r.workouts().size());
        assertFalse(r.conservaTodas());
        assertEquals(List.of(102L), r.sourcesWithoutDerived(),
                "la validacion de conservacion necesita saber que se elimino por orden");
        assertTrue(r.from(102L).isEmpty());
        assertTrue(r.from(101L).isPresent());
    }

    @Test
    @DisplayName("Menos fechas que sesiones a conservar: falla, no trunca en silencio")
    void menosFechasQueSesiones() {
        var e = assertThrows(IllegalArgumentException.class,
                () -> builder.derive(semanaDeTres(), List.of(101L, 102L, 103L), List.of(L2)));
        assertTrue(e.getMessage().contains("exactamente una por sesion"));
    }

    @Test
    @DisplayName("MAS fechas que sesiones: tambien falla, no se ignoran en silencio")
    void masFechasQueSesiones() {
        // Una fecha sobrante significa que quien llama no decidio bien que dias
        // usar, y antes se descartaba sin avisar.
        var e = assertThrows(IllegalArgumentException.class,
                () -> builder.derive(semanaDeTres(), List.of(101L, 102L),
                        List.of(L2, L2.plusDays(2), L2.plusDays(4))));
        assertTrue(e.getMessage().contains("exactamente una por sesion"));
    }

    @Test
    @DisplayName("Origen repetido: falla aqui, sin esperar a que lo rechace la base")
    void origenRepetido() {
        var e = assertThrows(IllegalArgumentException.class,
                () -> builder.derive(semanaDeTres(), List.of(101L, 101L, 102L),
                        List.of(L2, L2.plusDays(2), L2.plusDays(4))));
        assertTrue(e.getMessage().contains("uno a uno"));
        assertTrue(e.getMessage().contains("101"));
    }

    @Test
    @DisplayName("Conservar una sesion que no existe en la semana anterior: falla")
    void origenInexistente() {
        assertThrows(IllegalArgumentException.class,
                () -> builder.derive(semanaDeTres(), List.of(999L), List.of(L2)));
    }

    // -----------------------------------------------------------------------
    // 4. Adaptacion ampliada con dos sesiones modificadas
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Ampliada: dos sesiones cambian de dia y la tercera se conserva igual")
    void ampliadaConDosModificadas() {
        // Las tres son FULL_BODY y a dos se les mueve la fecha. Es exactamente
        // el caso que la heuristica por fecha y enfoque no podria resolver.
        var r = builder.derive(semanaDeTres(), List.of(101L, 102L, 103L),
                List.of(L2.plusDays(1), L2.plusDays(3), L2.plusDays(4)));

        assertEquals(3, r.workouts().size());
        assertTrue(r.conservaTodas());

        assertEquals(L2.plusDays(1), r.from(101L).orElseThrow().workout().scheduledDate());
        assertEquals(L2.plusDays(3), r.from(102L).orElseThrow().workout().scheduledDate());
        assertEquals(L2.plusDays(4), r.from(103L).orElseThrow().workout().scheduledDate());

        // Y cada una conserva sus propios ejercicios, no los de la vecina.
        assertEquals(List.of(740L, 1101L),
                r.from(101L).orElseThrow().workout().exercises().stream()
                        .map(x -> x.exerciseId()).toList());
        assertEquals(List.of(592L),
                r.from(103L).orElseThrow().workout().exercises().stream()
                        .map(x -> x.exerciseId()).toList());
    }

    // -----------------------------------------------------------------------
    // Casos degenerados
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Sin semana anterior y sin nada que conservar: derivacion vacia")
    void semanaAnteriorVacia() {
        // Es la primera semana del participante: no hay de donde derivar y
        // tampoco se pidio derivar nada.
        assertTrue(builder.derive(null, List.of(), List.of()).workouts().isEmpty());
        assertTrue(builder.derive(PreviousWeekSummary.empty(), List.of(), List.of())
                .workouts().isEmpty());
    }

    @Test
    @DisplayName("Sin semana anterior pero pidiendo conservar sesiones: falla, no devuelve vacio")
    void sinSemanaAnteriorPidiendoConservar() {
        // Devolver una derivacion vacia aqui seria afirmar que no habia nada que
        // conservar, y eso es falso: lo que falta es la semana anterior.
        var conNula = assertThrows(IllegalArgumentException.class,
                () -> builder.derive(null, List.of(101L, 102L, 103L),
                        List.of(L2, L2.plusDays(2), L2.plusDays(4))));
        assertTrue(conNula.getMessage().contains("no hay semana anterior"));

        var conVacia = assertThrows(IllegalArgumentException.class,
                () -> builder.derive(PreviousWeekSummary.empty(), List.of(101L), List.of(L2)));
        assertTrue(conVacia.getMessage().contains("no hay semana anterior"));
    }

    @Test
    @DisplayName("Entrada incoherente falla igual exista o no semana anterior")
    void lasValidacionesVanPrimero() {
        // Fechas que no cuadran, sin semana anterior: el error que importa es el
        // de las fechas, no el de la semana que falta.
        var e = assertThrows(IllegalArgumentException.class,
                () -> builder.derive(null, List.of(101L), List.of(L2, L2.plusDays(1))));
        assertTrue(e.getMessage().contains("exactamente una por sesion"));

        // Y un origen repetido tambien, sin depender de que haya semana anterior.
        var r = assertThrows(IllegalArgumentException.class,
                () -> builder.derive(null, List.of(101L, 101L), List.of(L2, L2.plusDays(1))));
        assertTrue(r.getMessage().contains("uno a uno"));
    }

    @Test
    @DisplayName("No conservar ninguna: las tres quedan declaradas como sin derivar")
    void noConservarNinguna() {
        var r = builder.derive(semanaDeTres(), List.of(), List.of());

        assertTrue(r.workouts().isEmpty());
        assertEquals(List.of(101L, 102L, 103L), r.sourcesWithoutDerived());
    }

    @Test
    @DisplayName("Una sesion derivada sin origen no se puede construir")
    void derivadaSinOrigen() {
        assertThrows(IllegalArgumentException.class,
                () -> new main.web.services.fitsense.planning.domain.model.valueobjects
                        .DerivedWorkout(null, null));
    }

    // -----------------------------------------------------------------------
    // Una semana de tres sesiones FULL_BODY, con ejercicios distintos cada una
    // -----------------------------------------------------------------------

    private static PreviousWeekSummary semanaDeTres() {
        var workouts = List.of(
                outcome(101L, L1, "Cuerpo completo A"),
                outcome(102L, L1.plusDays(2), "Cuerpo completo B"),
                outcome(103L, L1.plusDays(4), "Cuerpo completo C"));

        var prescriptions = List.of(
                // 101: dos ejercicios. El 740 se prescribio 3x10 y se hicieron 2 series.
                prescripcion(101L, L1, 740L, (short) 3, (short) 10, (short) 90,
                        new BigDecimal("20.00"), (short) 2),
                prescripcion(101L, L1, 1101L, (short) 2, (short) 13, (short) 90, null, (short) 2),
                prescripcion(102L, L1.plusDays(2), 304L, (short) 3, (short) 10, (short) 90,
                        null, (short) 3),
                prescripcion(103L, L1.plusDays(4), 592L, (short) 3, (short) 10, (short) 90,
                        new BigDecimal("12.50"), (short) 3));

        return new PreviousWeekSummary(new BigDecimal("66.67"), new BigDecimal("6.5"), 480,
                Map.of(), workouts, prescriptions, List.of());
    }

    private static PreviousWeekSummary.WorkoutOutcome outcome(long id, LocalDate fecha, String nombre) {
        return new PreviousWeekSummary.WorkoutOutcome(id, fecha, "FULL_BODY", "COMPLETED",
                null, true, "COMPLETED", (short) 6, new BigDecimal("100.00"), nombre, (short) 45);
    }

    private static PreviousWeekSummary.PrescriptionOutcome prescripcion(
            long workoutId, LocalDate fecha, long exerciseId, short series, short reps,
            short descanso, BigDecimal carga, short seriesHechas) {
        return new PreviousWeekSummary.PrescriptionOutcome(
                workoutId, fecha, exerciseId, "ejercicio " + exerciseId, "SETS_REPS",
                series, reps, null, carga,
                seriesHechas, seriesHechas * reps, null, carga,
                new BigDecimal("100.00"), "COMPLETED", null, "COMPLETED", descanso);
    }
}
