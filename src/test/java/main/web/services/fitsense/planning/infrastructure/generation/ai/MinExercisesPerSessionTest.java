package main.web.services.fitsense.planning.infrastructure.generation.ai;

import main.web.services.fitsense.planning.domain.model.valueobjects.MvpDosePolicy;
import main.web.services.fitsense.planning.domain.model.valueobjects.TargetPrescription;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GEN-IN-1.10b: {@code min_exercises_per_session} con la dosis de 4D.
 * <p>
 * Fija los dos numeros del hallazgo del 16/10 —el 9 que contradecia al tope y
 * el 5 que no— y, sobre todo, fija el <b>limite de lo que ese 5 promete</b>:
 * Juan lo pidio explicitamente, porque una referencia que se lea como garantia
 * es peor que ninguna.
 * <p>
 * Aritmetica pura: sin Spring, sin Jackson, sin base de datos.
 */
class MinExercisesPerSessionTest {

    // El perfil del primer MVP y los valores por omision de PrescriptionParams.
    private static final int PISO_V20 = 31;   // 45 * 70 / 100, division entera
    private static final int SEG_POR_REP = 3;
    private static final int TRANSICION = 60;
    private static final int WARMUP_MIN = 5;

    private static int minCon(int series, int reps, int descanso) {
        var valor = PlanInputSnapshot.minExercisesPara(PISO_V20, series, reps, descanso,
                SEG_POR_REP, TRANSICION, WARMUP_MIN);
        assertNotNull(valor);
        return valor;
    }

    /** Estimador de V17, el mismo que aplica el validador. */
    private static double estimadorMinutos(int estructurales, int rellenos) {
        int porEstructural = 3 * 10 * SEG_POR_REP + 2 * 90;   // 270 s
        int porRelleno     = 2 * 10 * SEG_POR_REP + 1 * 90;   // 150 s
        int n = estructurales + rellenos;
        return (WARMUP_MIN * 60
                + estructurales * porEstructural
                + rellenos * porRelleno
                + Math.max(0, n - 1) * TRANSICION) / 60.0;
    }

    // -----------------------------------------------------------------------
    // 1. Condición de Juan: el perfil de 45 minutos da 5 y no 9
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Con la dosis de 4D el perfil de 45 minutos da 5, no 9")
    void cincoYNoNueve() {
        var estructural = MvpDosePolicy.ClaseDosis.ESTRUCTURAL_ESTANDAR.dosis();
        int con4D = minCon(estructural.series(), estructural.reps(), estructural.descansoSegundos());

        assertEquals(5, con4D, "La referencia que viaja al prompt con GAIN_MUSCLE");

        // Y el valor anterior, el que contradecia al tope. GAIN_MUSCLE = 4x10/75,
        // con el tope de 2 series para principiantes.
        var objetivo = TargetPrescription.forGoal("GAIN_MUSCLE");
        int antiguo = minCon(objetivo.setsFor("BEGINNER"), objetivo.reps(), objetivo.restSeconds());

        assertEquals(9, antiguo,
                "Era 9 contra un tope de 6: ninguna propuesta podia cumplir los dos");
        assertEquals(2, objetivo.setsFor("BEGINNER"),
                "El 9 venia del tope de 2 series de MAX_SETS_BEGINNER");
    }

    @Test
    @DisplayName("El minimo que se publica no supera el tope de colocaciones")
    void elMinimoNoSuperaElTope() {
        var estructural = MvpDosePolicy.ClaseDosis.ESTRUCTURAL_ESTANDAR.dosis();
        int referencia = minCon(estructural.series(), estructural.reps(),
                estructural.descansoSegundos());

        assertTrue(referencia <= MvpDosePolicy.MAX_COLOC_DIA,
                "El prompt no puede pedir mas ejercicios de los que permite: "
                        + referencia + " contra " + MvpDosePolicy.MAX_COLOC_DIA);

        // Y la zona pequena tampoco lo rompe, aunque cueste mas por colocacion.
        var pequena = MvpDosePolicy.ClaseDosis.ESTRUCTURAL_ZONA_PEQUENA.dosis();
        assertTrue(minCon(pequena.series(), pequena.reps(), pequena.descansoSegundos())
                <= MvpDosePolicy.MAX_COLOC_DIA);
    }

    // -----------------------------------------------------------------------
    // 2. El matiz de Juan: lo que el 5 NO promete
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("El 5 solo se alcanza con cinco estructurales: con un relleno ya cae")
    void laReferenciaEsOptimista() {
        assertTrue(estimadorMinutos(5, 0) >= PISO_V20,
                "5 estructurales llegan al piso: " + estimadorMinutos(5, 0));
        assertEquals(31.5, estimadorMinutos(5, 0), 0.001);

        assertTrue(estimadorMinutos(4, 1) < PISO_V20,
                "Con un solo relleno de cinco, V20 rechaza: " + estimadorMinutos(4, 1));
        assertEquals(29.5, estimadorMinutos(4, 1), 0.001);
    }

    @Test
    @DisplayName("Con seis colocaciones hacen falta tres estructurales, y el margen es cero")
    void elLimiteConSeisColocaciones() {
        assertEquals(31.0, estimadorMinutos(3, 3), 0.001);
        assertTrue(estimadorMinutos(3, 3) >= PISO_V20,
                "V20 rechaza con estimada < piso, y 31 < 31 es falso: pasa, con margen cero");

        assertEquals(29.0, estimadorMinutos(2, 4), 0.001);
        assertTrue(estimadorMinutos(2, 4) < PISO_V20, "Con dos estructurales ya rechaza");
    }

    @Test
    @DisplayName("Un dia todo de relleno pediria 8, por encima del tope: caso insatisfacible")
    void elCasoInsatisfacible() {
        var relleno = MvpDosePolicy.ClaseDosis.RELLENO_ESTANDAR.dosis();
        int conRelleno = minCon(relleno.series(), relleno.reps(), relleno.descansoSegundos());

        assertEquals(8, conRelleno);
        assertTrue(conRelleno > MvpDosePolicy.MAX_COLOC_DIA,
                "Declarado a proposito: no se arregla publicando otro numero. "
                        + "Lo resuelve el selector con patrones, no este campo");

        // Y se confirma por el otro lado: seis rellenos no llegan al piso.
        assertTrue(estimadorMinutos(0, 6) < PISO_V20,
                "Seis rellenos dan " + estimadorMinutos(0, 6) + " min");
    }

    // -----------------------------------------------------------------------
    // 3. Condición de Juan: los otros cuatro objetivos no cambian
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Los otros cuatro objetivos conservan exactamente el calculo anterior")
    void losOtrosCuatroNoCambian() {
        // Mismo piso y mismos parametros; solo cambia el objetivo. Si alguno de
        // estos numeros se mueve, el parche se filtro donde no debia.
        assertEquals(9, conObjetivo("LOSE_WEIGHT"));
        assertEquals(8, conObjetivo("INCREASE_STRENGTH"));
        assertEquals(9, conObjetivo("GENERAL_FITNESS"));

        // Y la politica solo gobierna GAIN_MUSCLE.
        assertTrue(MvpDosePolicy.aplicaA("GAIN_MUSCLE"));
        assertFalse(MvpDosePolicy.aplicaA("LOSE_WEIGHT"));
        assertFalse(MvpDosePolicy.aplicaA("INCREASE_STRENGTH"));
        assertFalse(MvpDosePolicy.aplicaA("GENERAL_FITNESS"));
    }

    private static int conObjetivo(String goalType) {
        var objetivo = TargetPrescription.forGoal(goalType);
        return minCon(objetivo.setsFor("BEGINNER"), objetivo.reps(), objetivo.restSeconds());
    }

    @Test
    @DisplayName("La aritmetica no cambio: el suelo de 2 y la guarda del divisor cero siguen")
    void laAritmeticaNoCambio() {
        // Suelo de 2, de la version anterior: con una dosis carisima, 1 bastaria.
        assertEquals(2, minCon(10, 100, 300));

        // Guarda del divisor: sin transicion y sin coste por ejercicio, null.
        assertNull(PlanInputSnapshot.minExercisesPara(PISO_V20, 0, 0, 0, 3, 0, WARMUP_MIN));
    }
}
