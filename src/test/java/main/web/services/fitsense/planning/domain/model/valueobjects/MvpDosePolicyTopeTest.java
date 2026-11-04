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
 * 4D v3.4 (docs/FitSense/ADENDA_4D_v3_4.md): el tope de colocaciones de GAIN_MUSCLE por encima de 60 minutos es el
 * MINIMO numero de colocaciones estructurales (3x10, 90 s) cuyo contenido llega al piso de V20. Se comprueba con el
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
    @DisplayName("Valores: 6 hasta 59 min; 7 a 60 (4D v3.1, sin cambios); 8 de 61 a 68; 9 de 69 a 75")
    void valores() {
        for (int m = 30; m <= 75; m++) {
            int esperado = m < 60 ? 6 : m == 60 ? 7 : m <= 68 ? 8 : 9;
            assertEquals(esperado, MvpDosePolicy.maxColocaciones(m), "minutos " + m);
        }
        assertEquals(7, MvpDosePolicy.maxColocaciones(60));
        assertEquals(8, MvpDosePolicy.maxColocaciones(61));
        assertEquals(8, MvpDosePolicy.maxColocaciones(68), "8 colocaciones = 2.880 s >= 2.856 s (68 min)");
        assertEquals(9, MvpDosePolicy.maxColocaciones(69), "8 colocaciones = 2.880 s < 2.898 s (69 min): hacen falta 9");
        assertEquals(9, MvpDosePolicy.maxColocaciones(70));
        assertEquals(9, MvpDosePolicy.maxColocaciones(75));
    }

    @Test
    @DisplayName("Coherencia con V20 (estimador real): desde 60 min el tope llega al piso y uno menos no llega (61-75)")
    void coherenciaConV20() {
        for (int m = 60; m <= 75; m++) {
            int tope = MvpDosePolicy.maxColocaciones(m);
            assertTrue(contenido(tope) >= piso(m), "minutos " + m + ": " + contenido(tope) + " < " + piso(m));
            if (m > 60) assertTrue(contenido(tope - 1) < piso(m), "minutos " + m + ": el tope no es el minimo");
        }
        assertEquals(2550, contenido(7), "7 colocaciones = 2.550 s (el contenido maximo de 4D v3.1)");
        assertTrue(contenido(7) < piso(61), "con 4D v3.1 ya no llegaba a 61 minutos (2.562 s)");
        assertEquals(3210, contenido(9), "9 colocaciones = 3.210 s");
    }

    @Test
    @DisplayName("Dentro de los limites vigentes: presupuesto de 4D (85 %) y techo de V4 (+15 %) para 30-75 min")
    void dentroDeLimites() {
        for (int m = 30; m <= 75; m++) {
            int tope = MvpDosePolicy.maxColocaciones(m);
            long trabajoYDescansos = tope * (3L * 10 * 3 + 2 * 90);
            assertTrue(trabajoYDescansos <= MvpDosePolicy.presupuestoDiarioSegundos(m) || m < 60,
                    "minutos " + m + ": " + trabajoYDescansos + " > presupuesto " + MvpDosePolicy.presupuestoDiarioSegundos(m));
            assertTrue(contenido(tope) <= m * 60L * 115 / 100 || m < 60, "minutos " + m + ": pasa el techo de V4");
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
