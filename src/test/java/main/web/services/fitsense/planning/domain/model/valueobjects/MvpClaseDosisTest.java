package main.web.services.fitsense.planning.domain.model.valueobjects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P-1.3: las CLASES de dosis, que son lo que viaja al prompt.
 * <p>
 * No repite lo que ya fija {@code MvpDosePolicyTest} con sus 18 pruebas. Aqui
 * se comprueba lo unico que P-1.3 anade: que las cuatro clases existen, que
 * cada una da la dosis que decia la tabla aprobada, y -lo que de verdad
 * importa- que la clase y la dosis NO PUEDEN SEPARARSE, porque si lo hicieran
 * el prompt diria una cosa y el validador exigiria otra.
 * <p>
 * Dominio puro: sin Spring, sin Mockito, sin base de datos.
 */
class MvpClaseDosisTest {

    private static final double TOLERANCIA_MIN = 0.051;

    // -----------------------------------------------------------------------
    // Lo que P-1.3 promete en el documento aprobado
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Las clases son cuatro, porque el slot y la zona son independientes")
    void lasClasesSonCuatro() {
        assertEquals(4, MvpDosePolicy.ClaseDosis.values().length,
                "Si aparece una quinta, la tabla del prompt debe recogerla sola");
        assertEquals(4, MvpDosePolicy.tablaDosis().size());

        // Las dos dimensiones, cada combinacion una sola vez.
        for (boolean relleno : new boolean[] {false, true})
            for (boolean zona : new boolean[] {false, true}) {
                long cuantas = MvpDosePolicy.tablaDosis().stream()
                        .filter(c -> c.relleno() == relleno && c.zonaPequena() == zona)
                        .count();
                assertEquals(1, cuantas,
                        "relleno=" + relleno + " zonaPequena=" + zona);
            }
    }

    @Test
    @DisplayName("La tabla aprobada de P-1.3, fila por fila")
    void laTablaAprobada() {
        // 4D v3: 3x10/90 s estructural, 2 series relleno, +3 reps zona pequena.
        assertDosis(MvpDosePolicy.ClaseDosis.ESTRUCTURAL_ESTANDAR,      3, 10, 90, 6.00);
        assertDosis(MvpDosePolicy.ClaseDosis.ESTRUCTURAL_ZONA_PEQUENA,  3, 13, 90, 6.45);
        assertDosis(MvpDosePolicy.ClaseDosis.RELLENO_ESTANDAR,          2, 10, 90, 4.00);
        assertDosis(MvpDosePolicy.ClaseDosis.RELLENO_ZONA_PEQUENA,      2, 13, 90, 4.30);
    }

    private static void assertDosis(MvpDosePolicy.ClaseDosis clase,
                                    int series, int reps, int descanso, double minutos) {
        var dosis = clase.dosis();
        assertEquals(series, dosis.series(), clase + " series");
        assertEquals(reps, dosis.reps(), clase + " reps");
        assertEquals(descanso, dosis.descansoSegundos(), clase + " descanso");
        assertEquals(minutos, dosis.costeSegundos(false) / 60.0, TOLERANCIA_MIN,
                clase + " coste de §5");
        // El descanso es el de la capacidad en las cuatro, tambien en el relleno.
        assertEquals(MvpDosePolicy.Capacidad.HYPERTROPHY, dosis.capacidad(), clase + " capacidad");
    }

    @Test
    @DisplayName("ESTRUCTURAL_ZONA_PEQUENA es la mas cara, y seis no caben en 45 min")
    void laClaseQueFaltabaEsLaMasCara() {
        double masCara = MvpDosePolicy.tablaDosis().stream()
                .mapToDouble(c -> c.dosis().costeSegundos(false))
                .max().orElseThrow();
        assertEquals(MvpDosePolicy.ClaseDosis.ESTRUCTURAL_ZONA_PEQUENA.dosis().costeSegundos(false),
                masCara, "La fila que la v1 de P-1.3 omitia es precisamente la mas cara");

        // 6 x 6,45 = 38,7 min contra 38,25 utiles: no caben, por 27 segundos.
        double seis = MvpDosePolicy.MAX_COLOC_DIA * masCara;
        assertTrue(seis > MvpDosePolicy.presupuestoDiarioSegundos(45),
                "Un prompt con tres filas habria dejado este caso sin dosis");
        assertEquals(38.7, seis / 60.0, TOLERANCIA_MIN);
    }

    // -----------------------------------------------------------------------
    // Lo que impide que el prompt y el validador se separen
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("La clase y la dosis salen de la misma linea: dosisPara delega en ClaseDosis")
    void laClaseYLaDosisNoPuedenSepararse() {
        // Todas las entradas que el validador y el snapshot usan de verdad.
        var zonas = java.util.Arrays.asList(
                "upper legs", "lower legs", "waist", "chest", null, " WAIST ");
        var slots = java.util.List.of(
                Optional.of(MandatorySlot.LOWER_KNEE),
                Optional.of(MandatorySlot.LOWER_HIP),
                Optional.of(MandatorySlot.UPPER_PUSH),
                Optional.of(MandatorySlot.UPPER_PULL),
                Optional.<MandatorySlot>empty());

        for (var slot : slots)
            for (var zona : zonas) {
                var clase = MvpDosePolicy.claseDosis(slot, zona);
                var dosis = MvpDosePolicy.dosisPara(slot, zona);
                assertTrue(clase.isPresent() && dosis.isPresent(), "slot=" + slot + " zona=" + zona);
                assertEquals(clase.get().dosis(), dosis.get(),
                        "La dosis de la clase etiquetada debe ser la dosis exigida. "
                                + "slot=" + slot + " zona=" + zona);
            }
    }

    @Test
    @DisplayName("El slot decide las series y la zona las repeticiones, no al reves")
    void cadaDimensionMueveLoSuyo() {
        var estructuralNormal = MvpDosePolicy.claseDosis(
                Optional.of(MandatorySlot.LOWER_KNEE), "upper legs").orElseThrow();
        var estructuralPequena = MvpDosePolicy.claseDosis(
                Optional.of(MandatorySlot.LOWER_KNEE), "waist").orElseThrow();
        var rellenoNormal = MvpDosePolicy.claseDosis(
                Optional.empty(), "upper legs").orElseThrow();

        assertEquals(MvpDosePolicy.ClaseDosis.ESTRUCTURAL_ESTANDAR, estructuralNormal);
        assertEquals(MvpDosePolicy.ClaseDosis.ESTRUCTURAL_ZONA_PEQUENA, estructuralPequena);
        assertEquals(MvpDosePolicy.ClaseDosis.RELLENO_ESTANDAR, rellenoNormal);

        // La zona NO toca las series.
        assertEquals(estructuralNormal.dosis().series(), estructuralPequena.dosis().series());
        // El slot NO toca las repeticiones.
        assertEquals(estructuralNormal.dosis().reps(), rellenoNormal.dosis().reps());
    }

    @Test
    @DisplayName("slot null sigue siendo abstencion; Optional.empty sigue siendo relleno")
    void laAbstencionNoCambio() {
        // Comportamiento de 4D v3 intacto: la rama de abstencion solo se alcanza
        // con null, no con Optional.empty(). Lo fija aqui para que un cambio de
        // criterio tenga que ser deliberado.
        assertTrue(MvpDosePolicy.claseDosis(null, "upper legs").isEmpty());
        assertTrue(MvpDosePolicy.dosisPara(null, "upper legs").isEmpty());

        assertEquals(MvpDosePolicy.ClaseDosis.RELLENO_ESTANDAR,
                MvpDosePolicy.claseDosis(Optional.empty(), "upper legs").orElseThrow());
    }

    @Test
    @DisplayName("Los nombres de las clases son los del documento aprobado")
    void losNombresSonLosAprobados() {
        // La IA los recibe como cadena: un cambio de nombre rompe el contrato
        // del prompt sin que nada mas se queje.
        assertEquals("ESTRUCTURAL_ESTANDAR", MvpDosePolicy.ClaseDosis.ESTRUCTURAL_ESTANDAR.name());
        assertEquals("ESTRUCTURAL_ZONA_PEQUENA", MvpDosePolicy.ClaseDosis.ESTRUCTURAL_ZONA_PEQUENA.name());
        assertEquals("RELLENO_ESTANDAR", MvpDosePolicy.ClaseDosis.RELLENO_ESTANDAR.name());
        assertEquals("RELLENO_ZONA_PEQUENA", MvpDosePolicy.ClaseDosis.RELLENO_ZONA_PEQUENA.name());
    }
}
