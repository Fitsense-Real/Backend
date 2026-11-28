package main.web.services.fitsense.planning.domain.model.valueobjects;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 4D v3.4 + C-8 (docs/FitSense/ADENDA_4D_v3_4.md): el tope de colocaciones de GAIN_MUSCLE es el maximo entre el de 4D
 * v3.1 (6 / 7) y el MINIMO numero de colocaciones estructurales (3x10, 90 s) cuyo contenido llega al piso de V20. Se comprueba con el
 * estimador REAL y los parametros de MVP-1.8 para TODOS los minutos que admite P-01 (30-75).
 */
@DisplayName("4D v3.4: tope de colocaciones de GAIN_MUSCLE derivado del piso de V20 (30-75 min)")
class MvpDosePolicyTopeTest {

    /** MVP-1.8: piso 70 %, 3 s por repeticion, transicion 60 s, calentamiento 5 min, descanso por omision 60 s. */
    static final PrescriptionParams P = new PrescriptionParams(70, 3, 60, 5, 60, 20, null,
            new PrescriptionParams.RepLimits(6, 30, Map.of()), 10, 180);
    static final SessionDurationEstimator ESTIMADOR = new SessionDurationEstimator();

    static long contenido(int colocaciones) {
        var ej = new ArrayList<PlanDraft.DraftExercise>();
        for (int i = 0; i < colocaciones; i++)
            ej.add(new PlanDraft.DraftExercise((long) i + 1, PrescriptionType.SETS_REPS, (short) 3, (short) 10, null,
                    null, (short) 90, null));
        return ESTIMADOR.estimateSeconds(new PlanDraft.DraftWorkout(LocalDate.of(2026, 11, 2), WorkoutFocus.FULL_BODY,
                "x", 60, ej), P);
    }

    static long piso(int minutos) {
        return (minutos * 60L * 70 + 99) / 100;
    }

    @Test
    @DisplayName("Valores: 6 de 30 a 52 min; 7 de 53 a 60 (C-8; 60 es el de 4D v3.1); 8 de 61 a 68; 9 de 69 a 75")
    void valores() {
        for (int m = 30; m <= 75; m++) {
            int esperado = m <= 52 ? 6 : m <= 60 ? 7 : m <= 68 ? 8 : 9;
            assertEquals(esperado, MvpDosePolicy.maxColocaciones(m), "minutos " + m);
        }
        assertEquals(6, MvpDosePolicy.maxColocaciones(30), "4D v3.1, sin cambios");
        assertEquals(6, MvpDosePolicy.maxColocaciones(45), "4D v3.1, sin cambios");
        assertEquals(6, MvpDosePolicy.maxColocaciones(52), "6 colocaciones = 2.220 s >= 2.184 s (52 min)");
        assertEquals(7, MvpDosePolicy.maxColocaciones(53), "C-8: 6 colocaciones = 2.220 s < 2.226 s (53 min)");
        assertEquals(7, MvpDosePolicy.maxColocaciones(59), "C-8");
        assertEquals(7, MvpDosePolicy.maxColocaciones(60), "4D v3.1, sin cambios");
        assertEquals(8, MvpDosePolicy.maxColocaciones(61));
        assertEquals(8, MvpDosePolicy.maxColocaciones(68), "8 colocaciones = 2.880 s >= 2.856 s (68 min)");
        assertEquals(9, MvpDosePolicy.maxColocaciones(69), "8 colocaciones = 2.880 s < 2.898 s (69 min): hacen falta 9");
        assertEquals(9, MvpDosePolicy.maxColocaciones(70));
        assertEquals(9, MvpDosePolicy.maxColocaciones(75));
    }

    @Test
    @DisplayName("Coherencia con V20 (estimador real): en 30-75 el tope llega al piso; donde lo deriva V20, uno menos no")
    void coherenciaConV20() {
        for (int m = 30; m <= 75; m++) {
            int tope = MvpDosePolicy.maxColocaciones(m);
            assertTrue(contenido(tope) >= piso(m), "minutos " + m + ": " + contenido(tope) + " < " + piso(m));
            if (MvpDosePolicy.topeDerivadoDeV20(m) > MvpDosePolicy.topeV31(m))
                assertTrue(contenido(tope - 1) < piso(m), "minutos " + m + ": el tope no es el minimo");
        }
        assertEquals(2220, contenido(6), "6 colocaciones = 2.220 s (el contenido maximo de 4D v3.1 por debajo de 60)");
        assertTrue(contenido(6) < piso(53), "con 4D v3.1 ya no llegaba a 53 minutos (2.226 s)");
        assertEquals(2550, contenido(7), "7 colocaciones = 2.550 s (el contenido maximo de 4D v3.1 desde 60)");
        assertTrue(contenido(7) < piso(61), "con 4D v3.1 ya no llegaba a 61 minutos (2.562 s)");
        assertEquals(3210, contenido(9), "9 colocaciones = 3.210 s");
    }

    @Test
    @DisplayName("Version del tope en los mensajes de V24: v3.4 donde lo deriva V20 (53-59 y 61-75), v3.1 en el resto")
    void versionDelTope() {
        for (int m = 30; m <= 75; m++)
            assertEquals((m >= 53 && m <= 59) || m > 60 ? "v3.4" : "v3.1", MvpDosePolicy.versionDelTope(m), "minutos " + m);
    }

    @Test
    @DisplayName("Dentro de los limites vigentes: presupuesto de 4D (85 %) y techo de V4 (+15 %) donde manda el tope (53-75 min)")
    void dentroDeLimites() {
        for (int m = 30; m <= 75; m++) {
            int tope = MvpDosePolicy.maxColocaciones(m);
            long trabajoYDescansos = tope * (3L * 10 * 3 + 2 * 90);
            // Por debajo de 53 minutos el tope (6, de 4D v3.1) no es el limite efectivo: el presupuesto del 85 % deja
            // menos colocaciones (el motor lo respeta desde la seleccion, 4D v3.1 §2). Solo se comprueba donde manda el tope.
            if (m < 53) continue;
            assertTrue(trabajoYDescansos <= MvpDosePolicy.presupuestoDiarioSegundos(m),
                    "minutos " + m + ": " + trabajoYDescansos + " > presupuesto " + MvpDosePolicy.presupuestoDiarioSegundos(m));
            assertTrue(contenido(tope) <= m * 60L * 115 / 100, "minutos " + m + ": pasa el techo de V4");
        }
    }

    @Test
    @DisplayName("La dosis no cambia: 3x10 con 90 s; el tope de los demas objetivos sigue siendo el suyo")
    void dosisSinCambios() {
        assertEquals(3, MvpDosePolicy.Capacidad.HYPERTROPHY.series());
        assertEquals(10, MvpDosePolicy.Capacidad.HYPERTROPHY.reps());
        assertEquals(6, MvpDosePolicy.MAX_COLOC_DIA, "referencia congelada de 4D v3");
        assertEquals(7, MvpDosePolicy.MAX_COLOC_DIA_SESION_LARGA, "4D v3.1");
    }
}
