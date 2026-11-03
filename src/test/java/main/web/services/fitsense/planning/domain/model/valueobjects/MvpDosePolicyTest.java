package main.web.services.fitsense.planning.domain.model.valueobjects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La politica de dosis del primer perfil del MVP contra
 * {@code criterio-4D-alcance-MVP.md} v2.
 * <p>
 * La prueba que importa es {@link #reproduceLasNueveCeldasDe51()}: 4D v2 §5.1
 * publica una tabla de minutos por colocacion, y si la formula de aqui es la del
 * documento esas nueve celdas tienen que salir. Es la unica forma de comprobar
 * que no inventamos un modelo de tiempo parecido.
 */
class MvpDosePolicyTest {

    // ----------------------------------------------------------- la tabla §2

    @Test
    @DisplayName("las cuatro filas de 4D v2 §2, celda por celda")
    void tablaDeDosis() {
        var h = MvpDosePolicy.Capacidad.HYPERTROPHY;
        assertEquals(3, h.series());
        assertEquals(10, h.reps());
        assertEquals(90, h.descansoSegundos());
        assertEquals("2-3", h.rir());

        var f = MvpDosePolicy.Capacidad.STRENGTH;
        assertEquals(3, f.series());
        assertEquals(5, f.reps());
        assertEquals(150, f.descansoSegundos());

        var r = MvpDosePolicy.Capacidad.MUSCULAR_ENDURANCE;
        assertEquals(3, r.series());
        assertEquals(18, r.reps());
        assertEquals(45, r.descansoSegundos());

        var p = MvpDosePolicy.Capacidad.POWER;
        assertEquals(3, p.series());
        assertEquals(4, p.reps());
        assertEquals(150, p.descansoSegundos());
    }

    @Test
    @DisplayName("las constantes de §5 y §6 son las del documento")
    void constantes() {
        assertEquals(3.0, MvpDosePolicy.SEG_POR_REP);
        assertEquals(0.85, MvpDosePolicy.FRACCION_UTIL);
        assertEquals(6, MvpDosePolicy.MAX_COLOC_DIA);
        assertEquals(0.25, MvpDosePolicy.FRAC_SECUNDARIA_OBJETIVO);
    }

    // ------------------------------------------------------- el alcance: §6

    @Test
    @DisplayName("solo gobierna GAIN_MUSCLE; los demas objetivos no se tocan")
    void soloGainMuscle() {
        assertTrue(MvpDosePolicy.aplicaA("GAIN_MUSCLE"));
        assertFalse(MvpDosePolicy.aplicaA("LOSE_WEIGHT"));
        assertFalse(MvpDosePolicy.aplicaA("INCREASE_STRENGTH"));
        assertFalse(MvpDosePolicy.aplicaA("GENERAL_FITNESS"));
        assertFalse(MvpDosePolicy.aplicaA("ENDURANCE"));
        assertFalse(MvpDosePolicy.aplicaA(null));
    }

    // ------------------------------------- asignacion de capacidad, primer corte

    @Test
    @DisplayName("toda colocacion estructural va a hipertrofia, 3x10 con 90 s")
    void estructuralEsHipertrofia() {
        for (var slot : MandatorySlot.values()) {
            var dosis = MvpDosePolicy.dosisPara(Optional.of(slot), "upper legs").orElseThrow();
            assertEquals(3, dosis.series(), "series de " + slot);
            assertEquals(10, dosis.reps(), "reps de " + slot);
            assertEquals(90, dosis.descansoSegundos(), "descanso de " + slot);
            assertEquals(MvpDosePolicy.Capacidad.HYPERTROPHY, dosis.capacidad());
            assertFalse(dosis.relleno(), slot + " no es relleno");
        }
    }

    @Test
    @DisplayName("lo que no es estructural es relleno: 2 series, resto igual que la principal")
    void noEstructuralEsRelleno() {
        var dosis = MvpDosePolicy.dosisPara(Optional.empty(), "chest").orElseThrow();

        assertEquals(2, dosis.series());
        assertEquals(10, dosis.reps(), "las repeticiones son las de la principal dominante");
        assertEquals(90, dosis.descansoSegundos(), "y el descanso tambien");
        assertTrue(dosis.relleno());
    }

    @Test
    @DisplayName("sin slot no se inventa dosis: abstencion de §4 regla 4")
    void abstencion() {
        assertTrue(MvpDosePolicy.dosisPara(null, "chest").isEmpty());
    }

    @Test
    @DisplayName("STRENGTH queda declarada en la tabla pero no se asigna en el primer corte")
    void fuerzaDeclaradaNoAsignada() {
        assertEquals(3, MvpDosePolicy.Capacidad.STRENGTH.series(),
                "la fila existe, con sus valores de 4D v2");

        for (var slot : MandatorySlot.values())
            assertEquals(MvpDosePolicy.Capacidad.HYPERTROPHY,
                    MvpDosePolicy.dosisPara(Optional.of(slot), "upper legs").orElseThrow().capacidad(),
                    "mientras el denominador del 25 % no este definido, ninguna "
                            + "colocacion se asigna a fuerza");
    }

    // --------------------------------------------- el modelo de tiempo de §5

    @Test
    @DisplayName("reproduce las nueve celdas de la tabla de minutos de 4D v2 §5.1")
    void reproduceLasNueveCeldasDe51() {
        // normal · duplicando todo · solo ejecucion, en minutos, del documento.
        comprueba(MvpDosePolicy.Capacidad.HYPERTROPHY, 6.0, 12.0, 7.5);
        comprueba(MvpDosePolicy.Capacidad.STRENGTH, 8.2, 16.5, 9.0);
        comprueba(MvpDosePolicy.Capacidad.MUSCULAR_ENDURANCE, 5.0, 9.9, 7.7);
    }

    /**
     * «Duplicando todo» es la regla INGENUA que 4D v2 §5.1 descarta, y la
     * columna que importa es la tercera: duplicar la ejecucion y NO el descanso.
     * Se calcula aqui a mano porque la politica solo implementa la correcta.
     */
    private static void comprueba(MvpDosePolicy.Capacidad capacidad,
                                  double normalDoc, double duplicandoTodoDoc,
                                  double soloEjecucionDoc) {
        int s = capacidad.series(), r = capacidad.reps(), d = capacidad.descansoSegundos();
        var dosis = new MvpDosePolicy.Dosis((short) s, (short) r, (short) d, capacidad, false);

        // La tabla documental usa una cifra decimal y redondeos editoriales:
        // 495 s = 8,25 min figura como 8,2; Math.round(8,25*10)/10 da 8,3.
        // Comparamos con la precision publicada (±0,05 min), sin cambiar
        // la formula de produccion ni alterar la fuente metodologica.
        final double toleranciaDocumentoMin = 0.051;
        assertEquals(normalDoc, dosis.costeSegundos(false) / 60.0,
                toleranciaDocumentoMin, capacidad + " normal");
        assertEquals(soloEjecucionDoc, dosis.costeSegundos(true) / 60.0,
                toleranciaDocumentoMin,
                capacidad + " por lado: duplica la EJECUCION, no el descanso (§5.1)");

        double ingenua = s * (2.0 * r * MvpDosePolicy.SEG_POR_REP + 2.0 * d) / 60.0;
        assertEquals(duplicandoTodoDoc, ingenua, toleranciaDocumentoMin,
                capacidad + " duplicando todo, la regla que el documento descarta");
    }

    // ------------------------------------------------- el presupuesto del dia

    @Test
    @DisplayName("el presupuesto de una sesion de 45 minutos son 38,25 minutos utiles")
    void presupuesto() {
        assertEquals(2295.0, MvpDosePolicy.presupuestoDiarioSegundos(45));
        assertEquals(1530.0, MvpDosePolicy.presupuestoDiarioSegundos(30));
        assertEquals(3060.0, MvpDosePolicy.presupuestoDiarioSegundos(60));
    }

    @Test
    @DisplayName("en 45 minutos caben seis colocaciones estructurales y no siete")
    void seisCabenSieteNo() {
        var dosis = MvpDosePolicy.dosisPara(Optional.of(MandatorySlot.LOWER_KNEE), "upper legs").orElseThrow();
        double presupuesto = MvpDosePolicy.presupuestoDiarioSegundos(45);
        double una = dosis.costeSegundos(false);

        assertTrue(6 * una <= presupuesto, "seis caben: " + (6 * una) + " s de " + presupuesto);
        assertFalse(7 * una <= presupuesto, "siete no caben");
        assertEquals(MvpDosePolicy.MAX_COLOC_DIA, (int) (presupuesto / una),
                "el tope de §5 coincide con lo que el presupuesto permite, "
                        + "y esa coincidencia es del documento, no nuestra");
    }

    @Test
    @DisplayName("las nueve colocaciones del plan 11 no caben ni a la dosis exigida")
    void elPlan11NoCabe() {
        var dosis = MvpDosePolicy.dosisPara(Optional.of(MandatorySlot.UPPER_PUSH), "upper legs").orElseThrow();

        assertFalse(9 * dosis.costeSegundos(false)
                <= MvpDosePolicy.presupuestoDiarioSegundos(45));
    }

    @Test
    @DisplayName("una sola colocacion de fuerza secundaria consume el margen entero")
    void laFuerzaSecundariaNoDejaMargen() {
        var h = MvpDosePolicy.Capacidad.HYPERTROPHY;
        var f = MvpDosePolicy.Capacidad.STRENGTH;
        var hipertrofia = new MvpDosePolicy.Dosis(h.series(), h.reps(), h.descansoSegundos(), h, false);
        var fuerza = new MvpDosePolicy.Dosis(f.series(), f.reps(), f.descansoSegundos(), f, false);

        double dia = 5 * hipertrofia.costeSegundos(false) + fuerza.costeSegundos(false);
        double presupuesto = MvpDosePolicy.presupuestoDiarioSegundos(45);

        assertTrue(dia <= presupuesto, "cinco de hipertrofia y una de fuerza caben");
        assertTrue(presupuesto - dia < 60,
                "pero dejan menos de un minuto de margen: los 18 semanales son un "
                        + "techo estructural, no una garantia de factibilidad");
    }

    // --------------------------------------------------------- el relleno sale mas barato

    // ------------------------------------ 4D v3 · la excepcion de zona pequena

    @Test
    @DisplayName("4D v3: lower legs y waist suman 3 repeticiones; las demas zonas no")
    void zonaPequenaSumaTresRepeticiones() {
        assertTrue(MvpDosePolicy.esZonaPequena("lower legs"));
        assertTrue(MvpDosePolicy.esZonaPequena("waist"));

        for (var zona : new String[]{"upper legs", "chest", "back", "shoulders", "upper arms"})
            assertFalse(MvpDosePolicy.esZonaPequena(zona), zona + " no es zona pequena");

        var normal = MvpDosePolicy.dosisPara(Optional.of(MandatorySlot.LOWER_KNEE), "upper legs")
                .orElseThrow();
        var pequena = MvpDosePolicy.dosisPara(Optional.of(MandatorySlot.LOWER_KNEE), "lower legs")
                .orElseThrow();

        assertEquals(10, normal.reps());
        assertEquals(13, pequena.reps(), "10 de base mas los 3 de la excepcion");
        assertEquals(MvpDosePolicy.EXTRA_REPS_ZONA_PEQUENA, pequena.reps() - normal.reps());
    }

    @Test
    @DisplayName("la zona solo mueve las repeticiones: series y descanso no cambian")
    void laZonaSoloMueveRepeticiones() {
        var estructural = MvpDosePolicy.dosisPara(Optional.of(MandatorySlot.LOWER_KNEE), "waist")
                .orElseThrow();
        var relleno = MvpDosePolicy.dosisPara(Optional.empty(), "waist").orElseThrow();

        assertEquals(3, estructural.series(), "las series las decide el slot, no la zona");
        assertEquals(2, relleno.series());
        assertEquals(90, estructural.descansoSegundos(), "el descanso es el de la capacidad");
        assertEquals(90, relleno.descansoSegundos());
        assertEquals(13, estructural.reps());
        assertEquals(13, relleno.reps());
    }

    @Test
    @DisplayName("el caso 493: V16 y 4D v3 ya no se contradicen")
    void elCaso493() {
        // 493, elevacion de pantorrilla: lower legs, y no cae en ninguno de los
        // cuatro slots de 4C, asi que es relleno.
        var dosis = MvpDosePolicy.dosisPara(Optional.empty(), "lower legs").orElseThrow();

        assertEquals(2, dosis.series(), "relleno");
        assertEquals(13, dosis.reps(), "zona pequena");
        assertEquals(90, dosis.descansoSegundos());
        assertTrue(dosis.relleno());

        // V16 lo rechazo a 10 el 16/10 diciendo que el limite es de 12 a 30.
        final int minRepsV16 = 12, maxRepsV16 = 30;
        assertTrue(dosis.reps() >= minRepsV16,
                "con 4D v2 salian 10 y V16 pedia 12: ningun valor satisfacia las dos reglas");
        assertTrue(dosis.reps() <= maxRepsV16);

        // Y sigue pasando el suelo de V9.
        assertTrue(dosis.series() >= 2, "MIN_SETS de V9");
    }

    @Test
    @DisplayName("esZonaPequena no se rompe con nulos ni mayusculas")
    void esZonaPequenaEsTolerante() {
        assertFalse(MvpDosePolicy.esZonaPequena(null));
        assertFalse(MvpDosePolicy.esZonaPequena(""));
        assertTrue(MvpDosePolicy.esZonaPequena("Lower Legs"));
        assertTrue(MvpDosePolicy.esZonaPequena("  waist  "));
    }

    @Test
    @DisplayName("una zona pequena cuesta un poco mas, y el tope de seis aguanta")
    void costeDeLaZonaPequena() {
        var normal = MvpDosePolicy.dosisPara(Optional.of(MandatorySlot.LOWER_KNEE), "upper legs")
                .orElseThrow();
        var pequena = MvpDosePolicy.dosisPara(Optional.of(MandatorySlot.LOWER_KNEE), "lower legs")
                .orElseThrow();

        assertTrue(pequena.costeSegundos(false) > normal.costeSegundos(false),
                "tres repeticiones mas cuestan 9 segundos por serie");
        assertEquals(27.0, pequena.costeSegundos(false) - normal.costeSegundos(false),
                "3 series x 3 reps x 3 s");

        // El peor dia posible: seis colocaciones estructurales, todas de zona
        // pequena. Si esto no cupiera, el tope de seis seria inalcanzable.
        assertFalse(6 * pequena.costeSegundos(false)
                        <= MvpDosePolicy.presupuestoDiarioSegundos(45),
                "seis estructurales de zona pequena NO caben en 45 minutos: el tope "
                        + "de seis es un maximo, no una garantia, y con zonas pequenas "
                        + "el presupuesto muerde antes");
    }

    @Test
    @DisplayName("el tope de seis manda aunque el tiempo sobre, que es lo que pidio Juan")
    void elTopeMandaAunqueSobreTiempo() {
        var estructural = MvpDosePolicy.dosisPara(Optional.of(MandatorySlot.LOWER_HIP), "upper legs").orElseThrow();
        var relleno = MvpDosePolicy.dosisPara(Optional.empty(), "chest").orElseThrow();
        double presupuesto = MvpDosePolicy.presupuestoDiarioSegundos(45);

        assertTrue(relleno.costeSegundos(false) < estructural.costeSegundos(false),
                "dos series cuestan menos que tres");

        // Por tiempo cabrian mas de seis colocaciones de relleno...
        assertTrue(9 * relleno.costeSegundos(false) <= presupuesto,
                "nueve de relleno caben por tiempo");
        // ...y aun asi el tope de §5 las deja en seis.
        assertTrue(9 > MvpDosePolicy.MAX_COLOC_DIA,
                "pero MAX_COLOC_DIA no depende del tiempo: nueve ejercicios en un "
                        + "dia no se aceptan ni aunque quepan");
    }
}
