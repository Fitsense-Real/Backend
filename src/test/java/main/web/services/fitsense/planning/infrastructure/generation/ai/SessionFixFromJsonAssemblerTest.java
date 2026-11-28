package main.web.services.fitsense.planning.infrastructure.generation.ai;

import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El contrato A, leido de verdad con Jackson.
 * <p>
 * La regla que vigila la mitad de estas pruebas: una respuesta invalida, ausente
 * o incompleta NUNCA se convierte en una lista vacia. Si lo hiciera, el productor
 * conservaria la semana y el fallo del modelo quedaria registrado como
 * adaptacion SIN EFECTO.
 */
class SessionFixFromJsonAssemblerTest {

    private static final Set<Long> SENALADAS = Set.of(901L, 902L);

    private final SessionFixFromJsonAssembler assembler =
            new SessionFixFromJsonAssembler(new JsonSupport());

    // =======================================================================
    // Respuestas validas
    // =======================================================================

    @Test
    @DisplayName("Una sesion corregida se lee completa")
    void unaSesionCorregida() {
        var r = assembler.toCorrections("""
                {"schema_version":"FIX-1.0","rationale":"acorte la del miercoles",
                 "sessions":[
                   {"source_planned_workout_id":902,
                    "scheduled_date":"2026-10-21",
                    "expected_duration_minutes":30,
                    "change_reason":"falta de tiempo",
                    "exercises":[
                      {"exercise_id":304,"prescription_type":"SETS_REPS",
                       "planned_sets":2,"planned_reps":10,"planned_duration_seconds":null,
                       "target_load_kg":20.5,"rest_seconds":90,"notes":null}]}]}
                """, SENALADAS);

        assertEquals(1, r.size());
        var c = r.get(0);
        assertEquals(902L, c.sourcePlannedWorkoutId());
        assertEquals(LocalDate.of(2026, 10, 21), c.scheduledDate());
        assertEquals(30, c.expectedDurationMinutes());
        assertEquals("falta de tiempo", c.rationale());

        assertEquals(1, c.exercises().size());
        var e = c.exercises().get(0);
        assertEquals(304L, e.exerciseId());
        assertEquals(PrescriptionType.SETS_REPS, e.prescriptionType());
        assertEquals((short) 2, e.plannedSets());
        assertEquals((short) 10, e.plannedReps());
        assertEquals((short) 90, e.restSeconds());
        assertEquals(new BigDecimal("20.5"), e.targetLoadKg());
        assertNull(e.plannedDurationSeconds());
    }

    @Test
    @DisplayName("Dos sesiones en UNA sola respuesta: la adaptacion ampliada")
    void dosSesionesEnUnaRespuesta() {
        var r = assembler.toCorrections("""
                {"schema_version":"FIX-1.0","rationale":"dos sesiones",
                 "sessions":[
                   {"source_planned_workout_id":901,"scheduled_date":null,
                    "expected_duration_minutes":null,"change_reason":"carga",
                    "exercises":[{"exercise_id":740,
                      "prescription_type":"SETS_REPS","planned_sets":2,"planned_reps":10,
                      "planned_duration_seconds":null,"target_load_kg":null,
                      "rest_seconds":90,"notes":null}]},
                   {"source_planned_workout_id":902,"scheduled_date":null,
                    "expected_duration_minutes":25,"change_reason":"tiempo",
                    "exercises":[{"exercise_id":304,
                      "prescription_type":"DURATION","planned_sets":null,"planned_reps":null,
                      "planned_duration_seconds":45,"target_load_kg":null,
                      "rest_seconds":60,"notes":"mas suave"}]}]}
                """, SENALADAS);

        assertEquals(List.of(901L, 902L),
                r.stream().map(c -> c.sourcePlannedWorkoutId()).toList());
        assertNull(r.get(0).scheduledDate(), "null en la fecha es legitimo: no la toca");
        assertNull(r.get(0).expectedDurationMinutes());
        assertEquals(PrescriptionType.DURATION, r.get(1).exercises().get(0).prescriptionType());
        assertEquals(45, r.get(1).exercises().get(0).plannedDurationSeconds());
    }

    @Test
    @DisplayName("sessions VACIO es una respuesta: no se corrige nada")
    void sessionsVacio() {
        var r = assembler.toCorrections("""
                {"schema_version":"FIX-1.0","rationale":"no encontre nada mejor","sessions":[]}
                """, SENALADAS);

        assertTrue(r.isEmpty(), "la IA contesto, y contesto que no propone correcciones");
    }

    @Test
    @DisplayName("exercises VACIO se admite: dejar la sesion vacia lo deciden V5, 4C y 4D")
    void exercisesVacio() {
        var r = assembler.toCorrections("""
                {"schema_version":"FIX-1.0","rationale":"x","sessions":[
                   {"source_planned_workout_id":902,"scheduled_date":null,
                    "expected_duration_minutes":null,"change_reason":"vaciada",
                    "exercises":[]}]}
                """, SENALADAS);

        assertEquals(1, r.size());
        assertTrue(r.get(0).exercises().isEmpty());
    }

    @Test
    @DisplayName("Una respuesta envuelta en bloque de codigo se lee igual")
    void conBloqueDeCodigo() {
        var r = assembler.toCorrections("""
                ```json
                {"schema_version":"FIX-1.0","rationale":"x","sessions":[]}
                ```
                """, SENALADAS);

        assertTrue(r.isEmpty());
    }

    // =======================================================================
    // AUSENTE no es VACIO
    // =======================================================================

    @Test
    @DisplayName("sessions AUSENTE lanza: no es que no haya correcciones, es que no hay respuesta")
    void sessionsAusente() {
        var e = assertThrows(InvalidPlanDraftException.class, () -> assembler.toCorrections("""
                {"schema_version":"FIX-1.0","rationale":"se me olvido el arreglo"}
                """, SENALADAS));

        assertTrue(unico(e).contains("no trae el arreglo 'sessions'"), () -> e.problems().toString());
    }

    @Test
    @DisplayName("sessions NULO lanza, igual que ausente")
    void sessionsNulo() {
        var e = assertThrows(InvalidPlanDraftException.class, () -> assembler.toCorrections("""
                {"schema_version":"FIX-1.0","rationale":"x","sessions":null}
                """, SENALADAS));

        assertTrue(unico(e).contains("no trae el arreglo 'sessions'"), () -> e.problems().toString());
    }

    @Test
    @DisplayName("Un JSON roto lanza, no devuelve lista vacia")
    void jsonRoto() {
        var e = assertThrows(InvalidPlanDraftException.class,
                () -> assembler.toCorrections("{esto no es json", SENALADAS));

        assertTrue(unico(e).contains("no es un JSON valido"), () -> e.problems().toString());
    }

    @Test
    @DisplayName("Una respuesta vacia o nula lanza")
    void respuestaVaciaONula() {
        assertThrows(InvalidPlanDraftException.class,
                () -> assembler.toCorrections("", SENALADAS));
        assertThrows(InvalidPlanDraftException.class,
                () -> assembler.toCorrections(null, SENALADAS));
        assertThrows(InvalidPlanDraftException.class,
                () -> assembler.toCorrections("null", SENALADAS));
    }

    @Test
    @DisplayName("exercises AUSENTE en una sesion lanza, y el mensaje dice como pedir vaciarla")
    void exercisesAusente() {
        var e = assertThrows(InvalidPlanDraftException.class, () -> assembler.toCorrections("""
                {"schema_version":"FIX-1.0","rationale":"x","sessions":[
                   {"source_planned_workout_id":902,"scheduled_date":null,
                    "expected_duration_minutes":30,"change_reason":"y"}]}
                """, SENALADAS));

        assertTrue(unico(e).contains("no trae 'exercises'"), () -> e.problems().toString());
        assertTrue(unico(e).contains("arreglo vacio"),
                "el mensaje tiene que decirle como pedir vaciarla de verdad");
    }

    // =======================================================================
    // La respuesta tiene que estar COMPLETA antes de aceptar sessions: []
    // =======================================================================

    @Test
    @DisplayName("Sin schema_version lanza, aunque sessions venga vacio")
    void sinSchemaVersion() {
        // Una respuesta a medias no puede pasar por «no propongo correcciones».
        var e = assertThrows(InvalidPlanDraftException.class, () -> assembler.toCorrections("""
                {"rationale":"x","sessions":[]}
                """, SENALADAS));

        assertTrue(unico(e).contains("no trae 'schema_version'"), () -> e.problems().toString());
    }

    @Test
    @DisplayName("Una schema_version de otro contrato lanza")
    void schemaVersionDeOtroContrato() {
        var e = assertThrows(InvalidPlanDraftException.class, () -> assembler.toCorrections("""
                {"schema_version":"FIX-0.9","rationale":"x","sessions":[]}
                """, SENALADAS));

        assertTrue(unico(e).contains("FIX-0.9"), () -> e.problems().toString());
        assertTrue(unico(e).contains("FIX-1.0"),
                "tiene que decir cual es la version buena");
    }

    @Test
    @DisplayName("Sin rationale lanza, aunque sessions venga vacio")
    void sinRationale() {
        var e = assertThrows(InvalidPlanDraftException.class, () -> assembler.toCorrections("""
                {"schema_version":"FIX-1.0","sessions":[]}
                """, SENALADAS));

        assertTrue(unico(e).contains("no trae 'rationale'"), () -> e.problems().toString());
    }

    @Test
    @DisplayName("sessions como OBJETO suelto lanza: Jackson lo aceptaria como lista de uno")
    void sessionsComoObjetoSuelto() {
        // ACCEPT_SINGLE_VALUE_AS_ARRAY esta activado en JsonSupport -hace falta
        // para el 'output' de Replicate-, asi que sin la revision en crudo esto
        // pasaria y el contrato quedaria incumplido sin que nadie se entere.
        var e = assertThrows(InvalidPlanDraftException.class, () -> assembler.toCorrections("""
                {"schema_version":"FIX-1.0","rationale":"x","sessions":
                   {"source_planned_workout_id":902,"scheduled_date":null,
                    "expected_duration_minutes":30,"change_reason":"y","exercises":[]}}
                """, SENALADAS));

        assertTrue(unico(e).contains("tiene que ser un arreglo"), () -> e.problems().toString());
    }

    @Test
    @DisplayName("Un elemento NULO dentro de sessions lanza")
    void elementoNuloEnSessions() {
        var e = assertThrows(InvalidPlanDraftException.class, () -> assembler.toCorrections("""
                {"schema_version":"FIX-1.0","rationale":"x","sessions":[null]}
                """, SENALADAS));

        assertTrue(unico(e).contains("es nulo"), () -> e.problems().toString());
    }

    @Test
    @DisplayName("Un ejercicio NULO dentro de exercises lanza")
    void ejercicioNuloEnExercises() {
        var e = assertThrows(InvalidPlanDraftException.class, () -> assembler.toCorrections("""
                {"schema_version":"FIX-1.0","rationale":"x","sessions":[
                   {"source_planned_workout_id":902,"scheduled_date":null,
                    "expected_duration_minutes":30,"change_reason":"y","exercises":[null]}]}
                """, SENALADAS));

        assertTrue(unico(e).contains("es nulo"), () -> e.problems().toString());
    }

    // =======================================================================
    // Desbordamiento: el defecto que convertia un disparate en una dosis valida
    // =======================================================================

    @Test
    @DisplayName("rest_seconds 65626 NO se convierte en 90: se rechaza")
    void descansoQueSeConvertiriaEn90() {
        // Es el caso peor: (short) 65626 == 90, que es EXACTAMENTE el descanso
        // estructural de 4D. Un valor absurdo pasaria la validacion de dosis.
        var e = assertThrows(InvalidPlanDraftException.class, () -> assembler.toCorrections("""
                {"schema_version":"FIX-1.0","rationale":"x","sessions":[
                   {"source_planned_workout_id":902,"scheduled_date":null,
                    "expected_duration_minutes":30,"change_reason":"y","exercises":[
                      {"exercise_id":304,"prescription_type":"SETS_REPS","planned_sets":3,
                       "planned_reps":10,"planned_duration_seconds":null,"target_load_kg":null,
                       "rest_seconds":65626,"notes":null}]}]}
                """, SENALADAS));

        assertTrue(unico(e).contains("rest_seconds"), () -> e.problems().toString());
        assertTrue(unico(e).contains("65626"),
                "el mensaje tiene que llevar el valor ORIGINAL, no el truncado");
    }

    @Test
    @DisplayName("planned_sets 65539 NO se convierte en 3: se rechaza")
    void seriesQueSeConvertirianEn3() {
        var e = assertThrows(InvalidPlanDraftException.class, () -> assembler.toCorrections("""
                {"schema_version":"FIX-1.0","rationale":"x","sessions":[
                   {"source_planned_workout_id":902,"scheduled_date":null,
                    "expected_duration_minutes":30,"change_reason":"y","exercises":[
                      {"exercise_id":304,"prescription_type":"SETS_REPS","planned_sets":65539,
                       "planned_reps":10,"planned_duration_seconds":null,"target_load_kg":null,
                       "rest_seconds":90,"notes":null}]}]}
                """, SENALADAS));

        assertTrue(unico(e).contains("planned_sets"), () -> e.problems().toString());
        assertTrue(unico(e).contains("65539"), () -> e.problems().toString());
    }

    @Test
    @DisplayName("Un valor que al truncar queda NEGATIVO tambien se rechaza")
    void valorQueQuedariaNegativo() {
        // (short) 100000 == -31072. Un descanso negativo.
        var e = assertThrows(InvalidPlanDraftException.class, () -> assembler.toCorrections("""
                {"schema_version":"FIX-1.0","rationale":"x","sessions":[
                   {"source_planned_workout_id":902,"scheduled_date":null,
                    "expected_duration_minutes":30,"change_reason":"y","exercises":[
                      {"exercise_id":304,"prescription_type":"SETS_REPS","planned_sets":3,
                       "planned_reps":100000,"planned_duration_seconds":null,
                       "target_load_kg":null,"rest_seconds":90,"notes":null}]}]}
                """, SENALADAS));

        assertTrue(unico(e).contains("planned_reps"), () -> e.problems().toString());
    }

    @Test
    @DisplayName("El limite exacto del rango SI entra: 32767")
    void elLimiteEntra() {
        // Rechazar de mas seria tan malo como truncar: gastaria un reintento.
        var r = assembler.toCorrections("""
                {"schema_version":"FIX-1.0","rationale":"x","sessions":[
                   {"source_planned_workout_id":902,"scheduled_date":null,
                    "expected_duration_minutes":30,"change_reason":"y","exercises":[
                      {"exercise_id":304,"prescription_type":"SETS_REPS","planned_sets":3,
                       "planned_reps":10,"planned_duration_seconds":null,"target_load_kg":null,
                       "rest_seconds":32767,"notes":null}]}]}
                """, SENALADAS);

        assertEquals((short) 32767, r.get(0).exercises().get(0).restSeconds());
    }

    // =======================================================================
    // Identificadores: es lo que sostiene la correspondencia de V31
    // =======================================================================

    @Test
    @DisplayName("Sin source_planned_workout_id lanza")
    void sinOrigen() {
        var e = assertThrows(InvalidPlanDraftException.class, () -> assembler.toCorrections("""
                {"schema_version":"FIX-1.0","rationale":"x","sessions":[
                   {"scheduled_date":null,"expected_duration_minutes":30,"change_reason":"y",
                    "exercises":[]}]}
                """, SENALADAS));

        assertTrue(unico(e).contains("no trae source_planned_workout_id"),
                () -> e.problems().toString());
    }

    @Test
    @DisplayName("Un identificador que no estaba senalado se reporta al MODELO, no revienta")
    void origenNoSenalado() {
        // El empalme tambien lo rechazaria, pero ahi seria una excepcion de
        // programacion y el reintento no se enteraria de que corrigio de mas.
        var e = assertThrows(InvalidPlanDraftException.class, () -> assembler.toCorrections("""
                {"schema_version":"FIX-1.0","rationale":"x","sessions":[
                   {"source_planned_workout_id":777,"scheduled_date":null,
                    "expected_duration_minutes":30,"change_reason":"y","exercises":[]}]}
                """, SENALADAS));

        assertTrue(unico(e).contains("La sesion 777 no estaba senalada"),
                () -> e.problems().toString());
        assertTrue(unico(e).contains("[901, 902]"),
                "y le dice cuales eran, para que el reintento pueda acertar");
    }

    @Test
    @DisplayName("La misma sesion dos veces se reporta")
    void origenRepetido() {
        var e = assertThrows(InvalidPlanDraftException.class, () -> assembler.toCorrections("""
                {"schema_version":"FIX-1.0","rationale":"x","sessions":[
                   {"source_planned_workout_id":902,"scheduled_date":null,
                    "expected_duration_minutes":30,"change_reason":"y","exercises":[]},
                   {"source_planned_workout_id":902,"scheduled_date":null,
                    "expected_duration_minutes":40,"change_reason":"z","exercises":[]}]}
                """, SENALADAS));

        assertTrue(unico(e).contains("viene dos veces"), () -> e.problems().toString());
    }

    @Test
    @DisplayName("Un campo mal nombrado llega como nulo y se atrapa igual")
    void campoMalNombrado() {
        // FAIL_ON_UNKNOWN_PROPERTIES esta desactivado en JsonSupport, asi que
        // 'session_id' se ignora y el obligatorio queda nulo. Lo importante es
        // que no pase en silencio.
        var e = assertThrows(InvalidPlanDraftException.class, () -> assembler.toCorrections("""
                {"schema_version":"FIX-1.0","rationale":"x","sessions":[
                   {"session_id":902,"scheduled_date":null,
                    "expected_duration_minutes":30,"change_reason":"y","exercises":[]}]}
                """, SENALADAS));

        assertTrue(unico(e).contains("no trae source_planned_workout_id"),
                () -> e.problems().toString());
    }

    // =======================================================================
    // Contenido invalido: problemas del modelo, para el reintento
    // =======================================================================

    @Test
    @DisplayName("Fecha mal formada: problema del modelo, con la sesion y el formato")
    void fechaMalFormada() {
        var e = assertThrows(InvalidPlanDraftException.class, () -> assembler.toCorrections("""
                {"schema_version":"FIX-1.0","rationale":"x","sessions":[
                   {"source_planned_workout_id":902,"scheduled_date":"21/10/2026",
                    "expected_duration_minutes":30,"change_reason":"y","exercises":[]}]}
                """, SENALADAS));

        assertTrue(unico(e).contains("scheduled_date invalido en la sesion 902"),
                () -> e.problems().toString());
        assertTrue(unico(e).contains("AAAA-MM-DD"));
    }

    @Test
    @DisplayName("prescription_type inventado: problema del modelo, con el ejercicio")
    void tipoInventado() {
        var e = assertThrows(InvalidPlanDraftException.class, () -> assembler.toCorrections("""
                {"schema_version":"FIX-1.0","rationale":"x","sessions":[
                   {"source_planned_workout_id":902,"scheduled_date":null,
                    "expected_duration_minutes":30,"change_reason":"y","exercises":[
                      {"exercise_id":304,"prescription_type":"LO_QUE_SEA",
                       "planned_sets":2,"planned_reps":10,"planned_duration_seconds":null,
                       "target_load_kg":null,"rest_seconds":90,"notes":null}]}]}
                """, SENALADAS));

        assertTrue(unico(e).contains("prescription_type invalido en el ejercicio 304 de la sesion 902"),
                () -> e.problems().toString());
    }

    @Test
    @DisplayName("Un ejercicio sin exercise_id se reporta")
    void ejercicioSinId() {
        var e = assertThrows(InvalidPlanDraftException.class, () -> assembler.toCorrections("""
                {"schema_version":"FIX-1.0","rationale":"x","sessions":[
                   {"source_planned_workout_id":902,"scheduled_date":null,
                    "expected_duration_minutes":30,"change_reason":"y","exercises":[
                      {"prescription_type":"SETS_REPS","planned_sets":2,
                       "planned_reps":10,"planned_duration_seconds":null,"target_load_kg":null,
                       "rest_seconds":90,"notes":null}]}]}
                """, SENALADAS));

        assertTrue(unico(e).contains("no trae exercise_id"), () -> e.problems().toString());
    }

    @Test
    @DisplayName("Se acumulan TODOS los problemas, no solo el primero")
    void seAcumulanLosProblemas() {
        // El reintento recibe la lista entera: corregir uno y volver a fallar en
        // el siguiente gastaria el segundo intento para nada.
        var e = assertThrows(InvalidPlanDraftException.class, () -> assembler.toCorrections("""
                {"schema_version":"FIX-1.0","rationale":"x","sessions":[
                   {"source_planned_workout_id":777,"scheduled_date":null,
                    "expected_duration_minutes":30,"change_reason":"y","exercises":[]},
                   {"source_planned_workout_id":902,"scheduled_date":"ayer",
                    "expected_duration_minutes":30,"change_reason":"y","exercises":[]}]}
                """, SENALADAS));

        assertEquals(2, e.problems().size(), () -> e.problems().toString());
    }

    @Test
    @DisplayName("Sin sesiones solicitadas, cualquier identificador sobra")
    void sinSolicitadas() {
        var e = assertThrows(InvalidPlanDraftException.class, () -> assembler.toCorrections("""
                {"schema_version":"FIX-1.0","rationale":"x","sessions":[
                   {"source_planned_workout_id":902,"scheduled_date":null,
                    "expected_duration_minutes":30,"change_reason":"y","exercises":[]}]}
                """, Set.of()));

        assertTrue(unico(e).contains("no estaba senalada"), () -> e.problems().toString());
    }

    // -----------------------------------------------------------------------

    private static String unico(InvalidPlanDraftException e) {
        return String.join(" | ", e.problems());
    }
}
