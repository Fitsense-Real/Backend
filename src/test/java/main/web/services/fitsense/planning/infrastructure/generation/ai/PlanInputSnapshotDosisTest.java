package main.web.services.fitsense.planning.infrastructure.generation.ai;

import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.MvpDosePolicy;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GEN-IN-1.10 / P-1.3: lo que el snapshot anade y como se serializa.
 * <p>
 * No construye un {@code PlanGenerationContext}: prueba las dos piezas que
 * deciden el contenido nuevo -{@code etiquetasDe} y {@code tablaDosis}- y la
 * serializacion real con el mismo {@link JsonSupport} que usa el backend, que
 * es donde estaba la duda que no se podia resolver leyendo codigo.
 * <p>
 * Sin Spring ni base de datos: {@code JsonSupport} se instancia a mano.
 */
class PlanInputSnapshotDosisTest {

    private final JsonSupport json = new JsonSupport();

    private static CandidateExercise candidato(String bodyPart, String patron, PrescriptionType tipo) {
        return new CandidateExercise(1L, "ejercicio", bodyPart, "body weight", 1,
                tipo, null, patron);
    }

    // -----------------------------------------------------------------------
    // 1. Las etiquetas por candidato
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Las cuatro clases son alcanzables desde un candidato real")
    void lasCuatroClasesSonAlcanzables() {
        // Estructural estandar: sentadilla.
        var e1 = PlanInputSnapshot.etiquetasDe(
                candidato("upper legs", "KNEE_DOMINANT", PrescriptionType.SETS_REPS), true);
        assertEquals("LOWER_KNEE", e1.slot());
        assertEquals("ESTRUCTURAL_ESTANDAR", e1.claseDosis());

        // Estructural de zona pequena: el caso que la v1 del documento omitia.
        var e2 = PlanInputSnapshot.etiquetasDe(
                candidato("waist", "HIP_EXTENSION", PrescriptionType.SETS_REPS), true);
        assertEquals("LOWER_HIP", e2.slot());
        assertEquals("ESTRUCTURAL_ZONA_PEQUENA", e2.claseDosis());

        // Relleno estandar: un patron que no es ninguno de los cuatro slots.
        var e3 = PlanInputSnapshot.etiquetasDe(
                candidato("upper arms", "ELBOW_FLEXION", PrescriptionType.SETS_REPS), true);
        assertNull(e3.slot(), "Un patron fuera de los cuatro slots no tiene slot");
        assertEquals("RELLENO_ESTANDAR", e3.claseDosis());

        // Relleno de zona pequena: el ejercicio 493, la elevacion de pantorrilla.
        var e4 = PlanInputSnapshot.etiquetasDe(
                candidato("lower legs", "ANKLE_EXTENSION", PrescriptionType.SETS_REPS), true);
        assertNull(e4.slot());
        assertEquals("RELLENO_ZONA_PEQUENA", e4.claseDosis());
    }

    @Test
    @DisplayName("DURATION lleva slot pero NO clase de dosis")
    void durationNoRecibeDosis() {
        // 4D v2 §4 regla 4: no se inventa una dosis de series y repeticiones
        // para un ejercicio de sosten. La politica no mira el tipo, asi que el
        // corte tiene que estar aqui.
        var plancha = PlanInputSnapshot.etiquetasDe(
                candidato("waist", "HORIZONTAL_PUSH", PrescriptionType.DURATION), true);

        assertNull(plancha.claseDosis(), "Si llevara clase, la IA le pondria series y repeticiones");
        assertEquals("UPPER_PUSH", plancha.slot(),
                "El slot SI viaja: 4C cuenta posiciones sin mirar el tipo de prescripcion");
    }

    @Test
    @DisplayName("Sin GAIN_MUSCLE no viaja ninguna etiqueta")
    void soloGainMuscle() {
        var candidato = candidato("upper legs", "KNEE_DOMINANT", PrescriptionType.SETS_REPS);

        var sin = PlanInputSnapshot.etiquetasDe(candidato, false);
        assertNull(sin.slot());
        assertNull(sin.claseDosis());

        // Y el interruptor es el de la politica, no una cadena suelta.
        assertTrue(MvpDosePolicy.aplicaA("GAIN_MUSCLE"));
        assertFalse(MvpDosePolicy.aplicaA("LOSE_WEIGHT"));
        assertFalse(MvpDosePolicy.aplicaA("INCREASE_STRENGTH"));
    }

    @Test
    @DisplayName("Un candidato sin patron recibe relleno, igual que le exigira el validador")
    void sinPatronRecibeRelleno() {
        // Comportamiento DECLARADO, no elegido aqui: 4D v3 da relleno a lo que
        // no tiene patron. La etiqueta debe decir lo mismo que el validador va
        // a exigir; si divergieran, el prompt mentiria.
        var sinPatron = PlanInputSnapshot.etiquetasDe(
                candidato("chest", null, PrescriptionType.SETS_REPS), true);
        assertNull(sinPatron.slot());
        assertEquals("RELLENO_ESTANDAR", sinPatron.claseDosis());
    }

    // -----------------------------------------------------------------------
    // 2. La tabla
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("La tabla sale de recorrer la politica, con los costes de §5")
    void laTabla() {
        var tabla = PlanInputSnapshot.tablaDosis();
        assertEquals(4, tabla.size());

        // Mismo orden y mismos nombres que el enum: nada escrito a mano.
        assertEquals(MvpDosePolicy.tablaDosis().stream().map(Enum::name).toList(),
                tabla.stream().map(PlanInputSnapshot.DosisPorClase::clase).toList());

        var porClase = tabla.stream().collect(
                java.util.stream.Collectors.toMap(PlanInputSnapshot.DosisPorClase::clase, f -> f));

        assertFila(porClase.get("ESTRUCTURAL_ESTANDAR"), 3, 10, 90, 6.00);
        assertFila(porClase.get("ESTRUCTURAL_ZONA_PEQUENA"), 3, 13, 90, 6.45);
        assertFila(porClase.get("RELLENO_ESTANDAR"), 2, 10, 90, 4.00);
        assertFila(porClase.get("RELLENO_ZONA_PEQUENA"), 2, 13, 90, 4.30);
    }

    private static void assertFila(PlanInputSnapshot.DosisPorClase fila,
                                   int series, int reps, int descanso, double minutos) {
        assertNotNull(fila);
        assertEquals(series, fila.series(), fila.clase() + " series");
        assertEquals(reps, fila.reps(), fila.clase() + " reps");
        assertEquals(descanso, fila.descansoSegundos(), fila.clase() + " descanso");
        assertEquals(minutos, fila.costeMinutos(), 0.051, fila.clase() + " coste");
    }

    @Test
    @DisplayName("Seis colocaciones de la clase mas cara no caben en el presupuesto de 45 min")
    void elLimiteDelPresupuesto() {
        double masCara = PlanInputSnapshot.tablaDosis().stream()
                .mapToDouble(PlanInputSnapshot.DosisPorClase::costeMinutos)
                .max().orElseThrow();
        double utiles = MvpDosePolicy.presupuestoDiarioSegundos(45) / 60.0;

        assertEquals(38.25, utiles, 0.001, "El 85 % de 45 minutos");
        assertTrue(MvpDosePolicy.MAX_COLOC_DIA * masCara > utiles,
                "Si la suma del coste_minutos pudiera llegar al tope sin pasarse, "
                        + "el tope y el presupuesto serian la misma regla");
    }

    // -----------------------------------------------------------------------
    // 3. Compatibilidad, una prueba por direccion
    // -----------------------------------------------------------------------

    /**
     * DIRECCION 1.10 sobre 1.9: el lector de hoy ante un snapshot antiguo.
     * <p>
     * Esto es lo que ocurre al reabrir un plan guardado antes del 16/10. Lo que
     * prueba: no revienta, los campos nuevos salen nulos y los antiguos siguen
     * intactos. Lo que NO prueba: que ninguna comprobacion futura dependa de
     * esos nulos; eso no es una propiedad del formato.
     */
    @Test
    @DisplayName("Compatibilidad: el lector 1.10 lee un snapshot 1.9 y los campos nuevos salen nulos")
    void lector110SobreSnapshot19() {
        String snapshot19 = """
                {"schema_version":"GEN-IN-1.9","principles_version":"P-1.2",
                 "prompt_version":"PR-1.2","safety_notes":"nada",
                 "available_exercises":[
                   {"exercise_id":493,"name":"calf raise","body_part":"lower legs",
                    "equipment":"body weight","difficulty":1,
                    "prescription_type":"SETS_REPS","target_muscle":"calves",
                    "min_reps":12,"movement_pattern":"ANKLE_EXTENSION"}]}
                """;

        var leido = json.read(snapshot19, PlanInputSnapshot.class);

        assertEquals("GEN-IN-1.9", leido.schemaVersion(), "La version del plan antiguo se conserva");
        assertEquals(1, leido.availableExercises().size());

        var ejercicio = leido.availableExercises().get(0);
        assertEquals(Long.valueOf(493L), ejercicio.exerciseId());
        assertEquals("lower legs", ejercicio.bodyPart());
        assertEquals(Integer.valueOf(12), ejercicio.minReps());
        assertEquals("ANKLE_EXTENSION", ejercicio.movementPattern());

        assertNull(ejercicio.slot(), "Campo de 1.10 ausente en 1.9");
        assertNull(ejercicio.claseDosis(), "Campo de 1.10 ausente en 1.9");
    }

    /**
     * DIRECCION 1.9 sobre 1.10: un lector antiguo ante un snapshot de hoy.
     * <p>
     * No existe la clase de 1.9, asi que no se puede instanciar su lector. Lo
     * que SI se puede probar es el MECANISMO del que esa direccion depende:
     * que el ObjectMapper de JsonSupport ignora las propiedades que no conoce
     * -{@code FAIL_ON_UNKNOWN_PROPERTIES} en false-. Se prueba metiendo una
     * propiedad que ninguna version conoce, que es la misma situacion en la que
     * un lector de 1.9 ve {@code clase_dosis}.
     * <p>
     * Lo que NO prueba: que un binario antiguo de FitSense, con su propio
     * ObjectMapper, se comporte igual. Si esa configuracion cambiara, esta
     * direccion se rompe y esta prueba es la que lo dice.
     */
    @Test
    @DisplayName("Compatibilidad: un campo desconocido no rompe la lectura (el mecanismo de 1.9 sobre 1.10)")
    void mecanismoDeLector19SobreSnapshot110() {
        String conCampoFuturo = """
                {"schema_version":"GEN-IN-1.11","principles_version":"P-1.3",
                 "prompt_version":"PR-1.3","campo_de_una_version_futura":{"a":1},
                 "available_exercises":[
                   {"exercise_id":1,"name":"squat","body_part":"upper legs",
                    "prescription_type":"SETS_REPS","movement_pattern":"KNEE_DOMINANT",
                    "slot":"LOWER_KNEE","clase_dosis":"ESTRUCTURAL_ESTANDAR",
                    "otro_campo_futuro":"x"}]}
                """;

        // Si reventara, la prueba falla aqui mismo: es la misma senal.
        var leido = json.read(conCampoFuturo, PlanInputSnapshot.class);
        assertEquals("LOWER_KNEE", leido.availableExercises().get(0).slot());
        assertEquals("ESTRUCTURAL_ESTANDAR", leido.availableExercises().get(0).claseDosis());
    }

    // -----------------------------------------------------------------------
    // 4. Lo que de verdad se envia
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Los nombres de las claves nuevas son los del documento aprobado")
    void losNombresDeLasClaves() {
        var snapshot = new PlanInputSnapshot("GEN-IN-1.10", "P-1.3", "PR-1.3",
                null, constraintsConDosis(), null, null,
                List.of(new PlanInputSnapshot.AvailableExercise(1L, "squat", "upper legs",
                        "body weight", 1, "SETS_REPS", null, 8,
                        "KNEE_DOMINANT", "LOWER_KNEE", "ESTRUCTURAL_ESTANDAR")),
                null);

        String texto = json.write(snapshot);

        // Si alguna clave cambia de nombre, el punto 10 de P-1.3 nombra un
        // campo que no existe y nada mas se queja.
        for (String clave : List.of("\"slot\"", "\"clase_dosis\"", "\"dosis_por_clase\"",
                "\"max_colocaciones_dia\"", "\"minutos_utiles_dia\"", "\"slots_exigibles\"",
                "\"coste_minutos\"", "\"descanso_segundos\""))
            assertTrue(texto.contains(clave), "Falta la clave " + clave + " en:\n" + texto);

        assertTrue(texto.contains("\"ESTRUCTURAL_ZONA_PEQUENA\""),
                "La cuarta fila tiene que viajar");
    }

    /**
     * El tamano del prompt: desde GEN-IN-1.12 los nulos de cada ejercicio NO
     * viajan. Hasta 1.11 cada candidato llevaba sus claves en null, y con 842
     * candidatos eso eran decenas de miles de caracteres sin informacion. Un
     * lector de 1.11 lee igual el snapshot nuevo: la clave ausente es null.
     */
    @Test
    @DisplayName("GEN-IN-1.12: los campos nulos de un ejercicio no se envian y se leen como null")
    void losNulosNoViajan() {
        var sinDosis = new PlanInputSnapshot.AvailableExercise(1L, "squat", "upper legs",
                "body weight", 1, "SETS_REPS", null, 8, "KNEE_DOMINANT", null, null);

        String texto = json.write(new PlanInputSnapshot("GEN-IN-1.12", "P-1.3", "PR-1.5",
                null, null, null, null, List.of(sinDosis), null));

        assertFalse(texto.contains("\"slot\""), "en:\n" + texto);
        assertFalse(texto.contains("\"clase_dosis\""), "en:\n" + texto);
        assertFalse(texto.contains("\"target_muscle\""), "en:\n" + texto);
        var leido = json.read(texto, PlanInputSnapshot.class).availableExercises().get(0);
        assertNull(leido.slot());
        assertEquals("KNEE_DOMINANT", leido.movementPattern());
    }

    private static PlanInputSnapshot.Constraints constraintsConDosis() {
        return new PlanInputSnapshot.Constraints((short) 1,
                java.time.LocalDate.of(2026, 10, 19), java.time.LocalDate.of(2026, 10, 25),
                3, List.of((short) 1, (short) 3, (short) 5), 45, "HOME", 1,
                30, 10, 3, 31, 5, 120, List.of(), null,
                PlanInputSnapshot.tablaDosis(),
                MvpDosePolicy.MAX_COLOC_DIA,
                MvpDosePolicy.presupuestoDiarioSegundos(45) / 60.0,
                List.of("LOWER_KNEE", "LOWER_HIP", "UPPER_PUSH", "UPPER_PULL"));
    }
}
