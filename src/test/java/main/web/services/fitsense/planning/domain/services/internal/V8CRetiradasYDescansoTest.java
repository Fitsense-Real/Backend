package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V8C · HALLAZGOS H-A1 Y H-A2 DE LA AUDITORIA MEJ RONDA 2 (correccion del 10/11/2026).
 * <p>
 * <b>H-A1.</b> {@code VARIANTE_MAS_SENCILLA_O_CARGA} aceptaba propuestas que solo retiraban ejercicios
 * ({@code if (d.anadidos().isEmpty()) return;}). Su politica autoriza una variante mas sencilla del mismo patron o
 * ajustar la carga, no retirar. Retirar sin reemplazo lo autoriza SEGURIDAD_ANTE_DOLOR, y ACORTAR_O_REORGANIZAR una
 * sola colocacion de relleno.
 * <p>
 * <b>H-A2.</b> Con permiso de dosis, V8C aceptaba un cambio solo del descanso. La politica (instruccion de
 * correccion; V24) dice que el descanso de un ejercicio que se queda no se cambia nunca.
 * <p>
 * Sesion medida S (omitida): A (HORIZONTAL_PUSH, estructural), B (KNEE_DOMINANT, estructural) y C (CORE, relleno),
 * los tres a 3x10 con 90 s de descanso. Sustitutos elegibles: X (HORIZONTAL_PUSH) e Y (KNEE_DOMINANT).
 */
@DisplayName("V8C · H-A1 (retiradas no autorizadas) y H-A2 (cambios de descanso)")
class V8CRetiradasYDescansoTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 11, 2);
    private static final long S = 961L;
    private static final long OTRA = 962L;
    private static final long A = 1L, B = 2L, C = 3L;   // A y B estructurales, C relleno
    private static final long X = 11L, Y = 12L;         // sustitutos del patron de A y de B
    private static final long Q = 20L;                  // el de la otra sesion

    private final ConservationValidator v8c = new ConservationValidator();

    // ================================================================== H-A1
    @Nested
    @DisplayName("H-A1 · negativas: la propuesta se rechaza")
    class RetiradasNoAutorizadas {

        @Test
        @DisplayName("VARIANTE retira un ejercicio estructural sin reemplazo -> rechazada")
        void retiraEstructural() {
            var r = validar(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA, "TOO_DIFFICULT", ej(B), ej(C));
            assertRechazadaPor(r, "retira 1 ejercicio(s) [" + A + "]");
        }

        @Test
        @DisplayName("VARIANTE retira varios ejercicios -> rechazada")
        void retiraVarios() {
            var r = validar(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA, "TOO_DIFFICULT", ej(B));
            assertRechazadaPor(r, "retira 2 ejercicio(s)");
        }

        @Test
        @DisplayName("VARIANTE sustituye A por X del mismo patron Y ADEMAS retira C -> rechazada")
        void sustitucionConRetirada() {
            var r = validar(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA, "TOO_DIFFICULT", ej(X), ej(B));
            assertRechazadaPor(r, "retira 2 ejercicio(s)");
        }

        @Test
        @DisplayName("VARIANTE retira el relleno C sin reemplazo (lo que solo autoriza ACORTAR) -> rechazada")
        void retiraRellenoEnVariante() {
            var r = validar(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA, "TOO_DIFFICULT", ej(A), ej(B));
            assertRechazadaPor(r, "no retirar ejercicios");
        }

        @Test
        @DisplayName("FATIGA (REVISAR_CARGA) retira el relleno C: la intervencion no lo permite -> rechazada")
        void retiraEnFatiga() {
            var r = validar(TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION, "FATIGUE", ej(A), ej(B));
            assertRechazadaPor(r, "no autoriza sustituir");
        }

        @Test
        @DisplayName("ACORTAR retira el estructural A (solo vale una de relleno) -> rechazada")
        void acortarRetiraEstructural() {
            var r = validar(TipoIntervencion.ACORTAR_O_REORGANIZAR, "LACK_OF_TIME", ej(B), ej(C));
            assertRechazadaPor(r, "ESTRUCTURAL");
        }

        @Test
        @DisplayName("ACORTAR retira dos ejercicios -> rechazada")
        void acortarRetiraDos() {
            var r = validar(TipoIntervencion.ACORTAR_O_REORGANIZAR, "LACK_OF_TIME", ej(A));
            assertRechazadaPor(r, "retira 2 ejercicios");
        }
    }

    @Nested
    @DisplayName("H-A1 · positivas: lo autorizado sigue funcionando")
    class RetiradasYSustitucionesAutorizadas {

        @Test
        @DisplayName("VARIANTE sustituye A por X, del mismo patron -> aceptada y aplicada")
        void sustitucionMismoPatron() {
            var r = validar(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA, "TOO_DIFFICULT", ej(X), ej(B), ej(C));
            assertAplicada(r);
        }

        @Test
        @DisplayName("VARIANTE sustituye A por X y B por Y -> aceptada")
        void dosSustituciones() {
            var r = validar(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA, "TOO_DIFFICULT", ej(X), ej(Y), ej(C));
            assertAplicada(r);
        }

        @Test
        @DisplayName("VARIANTE sustituye A por X y baja la dosis de B (3x10 -> 2x10, mismo descanso) -> aceptada")
        void sustitucionYBajadaDeDosis() {
            var r = validar(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA, "TOO_DIFFICULT", ej(X), ej(B, 2, 10, 90),
                    ej(C));
            assertAplicada(r);
        }

        @Test
        @DisplayName("VARIANTE sustituye por un patron distinto -> sigue rechazada (regla de patron intacta)")
        void patronDistinto() {
            var r = validar(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA, "TOO_DIFFICULT", ej(Y), ej(B), ej(C));
            assertRechazadaPor(r, "mismo patron");
        }

        @Test
        @DisplayName("ACORTAR retira UNA colocacion de relleno con el resto intacto -> aceptada")
        void acortarRetiraRelleno() {
            var r = validar(TipoIntervencion.ACORTAR_O_REORGANIZAR, "LACK_OF_TIME", ej(A), ej(B));
            assertAplicada(r);
        }

        @Test
        @DisplayName("DOLOR retira sin reemplazo (retira 3, anade 2, menos volumen) -> aceptada")
        void dolorRetiraSinReemplazo() {
            var r = validar(TipoIntervencion.SEGURIDAD_ANTE_DOLOR, "PAIN_OR_DISCOMFORT", ej(X), ej(Y));
            assertAplicada(r);
        }

        @Test
        @DisplayName("DOLOR sustituye uno por uno con el mismo volumen -> aceptada")
        void dolorSustituye() {
            var r = validar(TipoIntervencion.SEGURIDAD_ANTE_DOLOR, "PAIN_OR_DISCOMFORT", ej(X), ej(Y), ej(20L + 1));
            assertAplicada(r);
        }

        @Test
        @DisplayName("VARIANTE que vuelve igual -> sin efecto, sin problemas")
        void varianteSinCambios() {
            var r = validar(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA, "TOO_DIFFICULT", ej(A), ej(B), ej(C));
            assertTrue(r.aceptable(), r.problems().toString());
            assertEquals(List.of(S), r.sinEfecto());
        }
    }

    // ================================================================== H-A2
    @Nested
    @DisplayName("H-A2 · negativas: un cambio de descanso de un ejercicio que se queda se rechaza")
    class CambiosDeDescanso {

        @Test
        @DisplayName("ACORTAR cambia solo el descanso de B (90 -> 60 s) -> rechazada")
        void soloDescansoEnAcortar() {
            var r = validar(TipoIntervencion.ACORTAR_O_REORGANIZAR, "LACK_OF_TIME", ej(A), ej(B, 3, 10, 60), ej(C));
            assertRechazadaPor(r, "cambia el descanso del ejercicio " + B);
        }

        @Test
        @DisplayName("FATIGA cambia solo el descanso de B (90 -> 120 s) -> rechazada")
        void soloDescansoEnFatiga() {
            var r = validar(TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION, "FATIGUE", ej(A), ej(B, 3, 10, 120), ej(C));
            assertRechazadaPor(r, "cambia el descanso del ejercicio " + B);
        }

        @Test
        @DisplayName("VARIANTE cambia solo el descanso de A -> rechazada")
        void soloDescansoEnVariante() {
            var r = validar(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA, "TOO_DIFFICULT", ej(A, 3, 10, 45), ej(B),
                    ej(C));
            assertRechazadaPor(r, "cambia el descanso del ejercicio " + A);
        }

        @Test
        @DisplayName("ACORTAR baja la dosis de B (3x10 -> 2x10) Y le cambia el descanso -> rechazada")
        void bajadaValidaConDescansoCambiado() {
            var r = validar(TipoIntervencion.ACORTAR_O_REORGANIZAR, "LACK_OF_TIME", ej(A), ej(B, 2, 10, 60), ej(C));
            assertRechazadaPor(r, "cambia el descanso del ejercicio " + B);
        }

        @Test
        @DisplayName("MOTIVACION (INVESTIGAR) cambia el descanso -> rechazada")
        void descansoEnMotivacion() {
            var r = validar(TipoIntervencion.INVESTIGAR_CAMBIO_MINIMO, "LACK_OF_MOTIVATION", ej(A), ej(B),
                    ej(C, 3, 10, 30));
            assertRechazadaPor(r, "cambia el descanso del ejercicio " + C);
        }
    }

    @Nested
    @DisplayName("H-A2 · positivas: las adaptaciones que respetan los descansos siguen funcionando")
    class DescansosRespetados {

        @Test
        @DisplayName("ACORTAR baja la dosis de B (3x10 -> 2x10) con el mismo descanso -> aceptada")
        void bajadaConMismoDescanso() {
            var r = validar(TipoIntervencion.ACORTAR_O_REORGANIZAR, "LACK_OF_TIME", ej(A), ej(B, 2, 10, 90), ej(C));
            assertAplicada(r);
        }

        @Test
        @DisplayName("FATIGA baja repeticiones de A (3x10 -> 3x8) con el mismo descanso -> aceptada")
        void fatigaBajaReps() {
            var r = validar(TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION, "FATIGUE", ej(A, 3, 8, 90), ej(B), ej(C));
            assertAplicada(r);
        }

        @Test
        @DisplayName("VARIANTE sustituye A por X con la prescripcion propia del sustituto (60 s) -> aceptada")
        void sustitutoConSuPropioDescanso() {
            var r = validar(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA, "TOO_DIFFICULT", ej(X, 3, 10, 60), ej(B),
                    ej(C));
            assertAplicada(r);
        }

        @Test
        @DisplayName("DOLOR sustituye con otro descanso en el sustituto -> aceptada")
        void dolorSustitutoConOtroDescanso() {
            var r = validar(TipoIntervencion.SEGURIDAD_ANTE_DOLOR, "PAIN_OR_DISCOMFORT", ej(X, 3, 10, 60),
                    ej(Y, 3, 10, 60));
            assertAplicada(r);
        }

        @Test
        @DisplayName("una sesion NO senalada sigue sin poder cambiar el descanso (regla anterior intacta)")
        void noSenaladaIntacta() {
            var derivada = new WeekDerivation(List.of(
                    new DerivedWorkout(S, sesion(0, List.of(ej(A), ej(B), ej(C)))),
                    new DerivedWorkout(OTRA, sesion(2, List.of(ej(Q, 3, 10, 60))))), List.of());
            var r = v8c.validate(derivada, previa("TOO_DIFFICULT"),
                    orden(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA), catalogo(),
                    ExclusionesDeSeguridad.ninguna(), List.of(), false, 30);
            assertFalse(r.aceptable());
        }
    }

    // =======================================================================

    private static void assertRechazadaPor(ConservationReport r, String fragmento) {
        assertFalse(r.aceptable(), "deberia rechazarse");
        assertTrue(r.problems().stream().anyMatch(p -> p.contains(fragmento)), r.problems().toString());
        assertFalse(r.aplicadas().contains(S), "una sesion rechazada no cuenta como aplicada");
    }

    private static void assertAplicada(ConservationReport r) {
        assertTrue(r.aceptable(), r.problems().toString());
        assertEquals(List.of(S), r.aplicadas());
    }

    private ConservationReport validar(TipoIntervencion t, String motivo, PlanDraft.DraftExercise... enS) {
        var derivada = new WeekDerivation(List.of(
                new DerivedWorkout(S, sesion(0, List.of(enS))),
                new DerivedWorkout(OTRA, sesion(2, List.of(ej(Q))))), List.of());
        return v8c.validate(derivada, previa(motivo), orden(t), catalogo(), ExclusionesDeSeguridad.ninguna(),
                List.of(), false, 30);
    }

    private static PlanDraft.DraftExercise ej(long id) {
        return ej(id, 3, 10, 90);
    }

    private static PlanDraft.DraftExercise ej(long id, int series, int reps, int descanso) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS, (short) series, (short) reps, null, null,
                (short) descanso, null);
    }

    private static PlanDraft.DraftWorkout sesion(int dia, List<PlanDraft.DraftExercise> l) {
        return new PlanDraft.DraftWorkout(LUNES.plusDays(dia).plusWeeks(1), WorkoutFocus.FULL_BODY, "Sesion", 40, l);
    }

    private static PreviousWeekSummary previa(String motivo) {
        var sesiones = List.of(
                new PreviousWeekSummary.WorkoutOutcome(S, LUNES, "FULL_BODY", "SKIPPED", motivo, true, "SKIPPED",
                        null, BigDecimal.ZERO, "Sesion", (short) 40),
                new PreviousWeekSummary.WorkoutOutcome(OTRA, LUNES.plusDays(2), "FULL_BODY", "COMPLETED", null, true,
                        "COMPLETED", (short) 6, new BigDecimal("100.00"), "Sesion", (short) 40));
        var prescritas = List.of(presc(S, LUNES, A), presc(S, LUNES, B), presc(S, LUNES, C),
                presc(OTRA, LUNES.plusDays(2), Q));
        return new PreviousWeekSummary(new BigDecimal("66.67"), new BigDecimal("6.0"), null, Map.of(), sesiones,
                prescritas, List.of());
    }

    private static PreviousWeekSummary.PrescriptionOutcome presc(long sesion, LocalDate dia, long id) {
        return new PreviousWeekSummary.PrescriptionOutcome(sesion, dia, id, "ejercicio " + id, "SETS_REPS",
                (short) 3, (short) 10, null, null, (short) 3, 30, null, null, new BigDecimal("100.00"),
                "COMPLETED", null, "COMPLETED", (short) 90);
    }

    private static Map<Long, CandidateExercise> catalogo() {
        var m = new LinkedHashMap<Long, CandidateExercise>();
        m.put(A, cand(A, "HORIZONTAL_PUSH"));
        m.put(B, cand(B, "KNEE_DOMINANT"));
        m.put(C, cand(C, "CORE"));
        m.put(Q, cand(Q, "HORIZONTAL_PUSH"));
        m.put(X, cand(X, "HORIZONTAL_PUSH"));
        m.put(Y, cand(Y, "KNEE_DOMINANT"));
        m.put(21L, cand(21L, "CORE"));
        return m;
    }

    private static CandidateExercise cand(long id, String patron) {
        return new CandidateExercise(id, "ej" + id, "chest", "body weight", 1, PrescriptionType.SETS_REPS,
                "pectorals", patron);
    }

    private static PlanAdjustment orden(TipoIntervencion t) {
        return new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null, 0.0, 0.0, null, null, null,
                "causa", null, null, List.of(new PlanAdjustment.TargetedSession(S, t)));
    }
}
