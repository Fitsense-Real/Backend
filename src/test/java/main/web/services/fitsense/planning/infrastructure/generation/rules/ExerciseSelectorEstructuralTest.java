package main.web.services.fitsense.planning.infrastructure.generation.rules;

import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Seleccion estructural para {@code GAIN_MUSCLE}, 16/10/2026.
 * <p>
 * Nacio del 422 del usuario 17: el motor de reglas cumplio 4D sin un solo
 * incumplimiento y V20 lo tumbo por UN minuto, porque el reparto por zona
 * eligio 8 estructurales y 10 de relleno en la semana.
 * <p>
 * <b>El catalogo de estas pruebas es SINTETICO.</b> Reproduce el censo de slots
 * del usuario 17 —24 / 13 / 51 / 12 y 156 fuera de slot, 256 en total— pero NO
 * es el catalogo real: no tiene sus zonas corporales ni pasa por los filtros de
 * equipamiento y dificultad. Lo que estas pruebas fijan es el MECANISMO —que el
 * modo prefiere estructurales y arrastra los slots de un dia a otro—, no una
 * prediccion de lo que hara la corrida.
 * <p>
 * Dominio puro: sin Spring, sin base de datos, sin Mockito.
 */
class ExerciseSelectorEstructuralTest {

    private static final int SEMANAS = 200;
    private static final int COLOCACIONES_DIA = 6;   // el tope de 4D v2 §5
    private static final int DIAS = 3;

    private long siguienteId = 0;

    private CandidateExercise ej(String zona, String patron, String musculo) {
        return new CandidateExercise(++siguienteId, "ej" + siguienteId, zona, "body weight", 1,
                PrescriptionType.SETS_REPS, musculo, patron);
    }

    /** Censo de slots del usuario 17: 24 / 13 / 51 / 12 + 156 fuera de slot. */
    private List<CandidateExercise> catalogoDelUsuario17() {
        siguienteId = 0;
        var l = new ArrayList<CandidateExercise>();
        for (int i = 0; i < 24; i++) l.add(ej("upper legs", "KNEE_DOMINANT", null));
        for (int i = 0; i < 13; i++) l.add(ej("upper legs", "HIP_HINGE", null));
        for (int i = 0; i < 51; i++) l.add(ej(i % 2 == 0 ? "chest" : "shoulders", "HORIZONTAL_PUSH", null));
        for (int i = 0; i < 12; i++) l.add(ej("back", "VERTICAL_PULL", null));
        for (int i = 0; i < 90; i++) l.add(ej("waist", "TRUNK_FLEXION", null));
        for (int i = 0; i < 40; i++) l.add(ej("chest", "ISOLATION", null));
        for (int i = 0; i < 26; i++) l.add(ej("back", "TRUNK_EXTENSION", null));
        return l;
    }

    private static long estructurales(Collection<CandidateExercise> seleccion) {
        return seleccion.stream().filter(c -> MandatorySlot.of(c).isPresent()).count();
    }

    /** Una semana completa de tres dias, con el arrastre semanal de slots. */
    private record Semana(int slotsCubiertos, int diasConMenosDeTresEstructurales) {}

    private Semana simularSemana(List<CandidateExercise> catalogo, boolean modo, long semilla) {
        var selector = new ExerciseSelector(catalogo, 5, Set.of(), new Random(semilla));
        selector.preferirEstructurales(modo);

        var pendientes = new LinkedHashSet<>(MandatorySlot.ALL);
        var cubiertos = new HashSet<MandatorySlot>();
        int cortos = 0;

        for (int dia = 0; dia < DIAS; dia++) {
            selector.slotsPendientes(pendientes);
            var seleccion = selector.pick(WorkoutFocus.FULL_BODY, COLOCACIONES_DIA);
            if (estructurales(seleccion) < 3) cortos++;
            seleccion.forEach(c -> MandatorySlot.of(c).ifPresent(slot -> {
                cubiertos.add(slot);
                pendientes.remove(slot);
            }));
        }
        return new Semana(cubiertos.size(), cortos);
    }

    // -----------------------------------------------------------------------
    // 1. Lo que el 422 pedia: cobertura semanal y margen para V20
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Con el modo encendido, las 200 semanas cubren los cuatro slots")
    void cobertura4CSemanal() {
        var catalogo = catalogoDelUsuario17();

        int conModo = 0, sinModo = 0;
        for (long s = 0; s < SEMANAS; s++) {
            if (simularSemana(catalogo, true, s).slotsCubiertos() == 4) conModo++;
            if (simularSemana(catalogo, false, s).slotsCubiertos() == 4) sinModo++;
        }

        assertEquals(SEMANAS, conModo, "El arrastre semanal debe cubrir los cuatro siempre");
        assertTrue(sinModo < SEMANAS * 0.6,
                "Sin el modo la cobertura depende del azar del reparto por zona; salieron "
                        + sinModo + " de " + SEMANAS);
    }

    @Test
    @DisplayName("Ningun dia se queda por debajo de tres estructurales, que es donde falla V20")
    void margenParaV20() {
        // Con seis colocaciones, tres estructurales dan 31,0 minutos exactos
        // contra un piso de 31: pasa, con margen cero. Con dos, 29,0: rechaza.
        var catalogo = catalogoDelUsuario17();

        int cortosConModo = 0, cortosSinModo = 0;
        for (long s = 0; s < SEMANAS; s++) {
            cortosConModo += simularSemana(catalogo, true, s).diasConMenosDeTresEstructurales();
            cortosSinModo += simularSemana(catalogo, false, s).diasConMenosDeTresEstructurales();
        }

        assertEquals(0, cortosConModo, "Ningun dia deberia quedar en riesgo de V20");
        assertTrue(cortosSinModo > SEMANAS * DIAS * 0.10,
                "Sin el modo el riesgo es real y medible; salieron " + cortosSinModo
                        + " de " + (SEMANAS * DIAS));
    }

    // -----------------------------------------------------------------------
    // 2. Los limites que Juan puso
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Un slot sin candidatos no se fuerza ni revienta")
    void noFuerzaUnSlotSinCandidatos() {
        siguienteId = 0;
        var sinCadera = new ArrayList<CandidateExercise>();
        for (int i = 0; i < 10; i++) sinCadera.add(ej("upper legs", "KNEE_DOMINANT", null));
        for (int i = 0; i < 10; i++) sinCadera.add(ej("chest", "HORIZONTAL_PUSH", null));
        for (int i = 0; i < 10; i++) sinCadera.add(ej("back", "VERTICAL_PULL", null));
        for (int i = 0; i < 20; i++) sinCadera.add(ej("waist", "TRUNK_FLEXION", null));

        var selector = new ExerciseSelector(sinCadera, 5, Set.of(), new Random(1));
        selector.preferirEstructurales(true);

        // El generador consulta esto para no pedir lo que no existe.
        assertFalse(selector.haySlot(MandatorySlot.LOWER_HIP));
        assertTrue(selector.haySlot(MandatorySlot.LOWER_KNEE));

        // Y aunque se le pidan los cuatro, no revienta ni devuelve de menos.
        selector.slotsPendientes(MandatorySlot.ALL);
        var seleccion = selector.pick(WorkoutFocus.FULL_BODY, COLOCACIONES_DIA);
        assertEquals(COLOCACIONES_DIA, seleccion.size());
    }

    @Test
    @DisplayName("El relleno entra solo cuando ya no queda estructural")
    void elRellenoEntraCuandoCorresponde() {
        siguienteId = 0;
        var pocos = new ArrayList<CandidateExercise>();
        pocos.add(ej("upper legs", "KNEE_DOMINANT", null));
        pocos.add(ej("chest", "HORIZONTAL_PUSH", null));
        for (int i = 0; i < 50; i++) pocos.add(ej("waist", "TRUNK_FLEXION", null));

        var selector = new ExerciseSelector(pocos, 5, Set.of(), new Random(7));
        selector.preferirEstructurales(true);
        selector.slotsPendientes(MandatorySlot.ALL);

        var seleccion = selector.pick(WorkoutFocus.FULL_BODY, COLOCACIONES_DIA);

        assertEquals(2, estructurales(seleccion),
                "Las dos estructurales que hay tienen que estar las dos");
        assertEquals(COLOCACIONES_DIA, seleccion.size(),
                "El resto se completa con relleno: la sesion no se acorta");
    }

    @Test
    @DisplayName("V22 sigue mandando en modo estructural: PUSH no lleva biceps")
    void v22NoSeRelaja() {
        siguienteId = 0;
        var brazos = new ArrayList<CandidateExercise>();
        for (int i = 0; i < 5; i++) brazos.add(ej("chest", "HORIZONTAL_PUSH", null));
        for (int i = 0; i < 5; i++) brazos.add(ej("shoulders", "VERTICAL_PUSH", null));
        for (int i = 0; i < 20; i++) brazos.add(ej("upper arms", "ELBOW_FLEXION", "biceps"));
        for (int i = 0; i < 3; i++) brazos.add(ej("upper arms", "ELBOW_EXTENSION", "triceps"));

        int conBiceps = 0;
        for (long s = 0; s < SEMANAS; s++) {
            var selector = new ExerciseSelector(brazos, 5, Set.of(), new Random(s));
            selector.preferirEstructurales(true);
            selector.slotsPendientes(MandatorySlot.ALL);
            for (var c : selector.pick(WorkoutFocus.PUSH, COLOCACIONES_DIA))
                if ("biceps".equals(c.targetMuscle())) conBiceps++;
        }
        assertEquals(0, conBiceps, "El modo no abre ninguna puerta que V22 cierra");
    }

    @Test
    @DisplayName("Con el modo apagado, los slots pendientes se ignoran")
    void losOtrosCuatroObjetivosNoCambian() {
        siguienteId = 0;
        var pocos = new ArrayList<CandidateExercise>();
        pocos.add(ej("upper legs", "KNEE_DOMINANT", null));
        pocos.add(ej("chest", "HORIZONTAL_PUSH", null));
        for (int i = 0; i < 50; i++) pocos.add(ej("waist", "TRUNK_FLEXION", null));

        // Misma semilla, misma entrada. Lo unico que cambia es el modo.
        var conModo = new ExerciseSelector(pocos, 5, Set.of(), new Random(7));
        conModo.preferirEstructurales(true);
        conModo.slotsPendientes(MandatorySlot.ALL);

        var sinModo = new ExerciseSelector(pocos, 5, Set.of(), new Random(7));
        sinModo.slotsPendientes(MandatorySlot.ALL);   // se le pasan y debe ignorarlos

        assertNotEquals(conModo.pick(WorkoutFocus.FULL_BODY, COLOCACIONES_DIA),
                sinModo.pick(WorkoutFocus.FULL_BODY, COLOCACIONES_DIA),
                "Si fueran iguales, el modo no estaria haciendo nada");
    }

    // -----------------------------------------------------------------------
    // 3. Las dos piezas que usa el generador
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("pickAdditional con el filtro estructural no devuelve relleno")
    void pickAdditionalEstructural() {
        var catalogo = catalogoDelUsuario17();
        var selector = new ExerciseSelector(catalogo, 5, Set.of(), new Random(3));
        selector.preferirEstructurales(true);

        var sesion = new ArrayList<>(selector.pick(WorkoutFocus.FULL_BODY, 4));
        for (int i = 0; i < 20; i++) {
            var extra = selector.pickAdditional(WorkoutFocus.FULL_BODY, sesion, true,
                    ExerciseSelector.ESTRUCTURAL);
            if (extra.isEmpty()) break;
            assertTrue(MandatorySlot.of(extra.get()).isPresent(),
                    "El filtro no deberia dejar pasar relleno");
            assertFalse(sesion.contains(extra.get()), "Ni repetir lo que ya esta");
            sesion.add(extra.get());
        }
    }

    @Test
    @DisplayName("La sustitucion devuelve una estructural que no esta ya en la sesion")
    void sustitucionParaElRepasoDeV20() {
        siguienteId = 0;
        var catalogo = new ArrayList<CandidateExercise>();
        for (int i = 0; i < 6; i++) catalogo.add(ej("upper legs", "KNEE_DOMINANT", null));
        for (int i = 0; i < 6; i++) catalogo.add(ej("chest", "HORIZONTAL_PUSH", null));
        for (int i = 0; i < 30; i++) catalogo.add(ej("waist", "TRUNK_FLEXION", null));

        var selector = new ExerciseSelector(catalogo, 5, Set.of(), new Random(11));
        var sesion = new ArrayList<CandidateExercise>();
        sesion.add(catalogo.get(0));                       // estructural
        sesion.add(catalogo.get(6));                       // estructural
        var relleno = catalogo.get(12);                    // relleno, el que se sustituye
        sesion.add(relleno);

        var reemplazo = selector.estructuralQueSustituyaA(WorkoutFocus.FULL_BODY, sesion, relleno);

        assertTrue(reemplazo.isPresent(), "Hay estructurales libres en el catalogo");
        assertTrue(MandatorySlot.of(reemplazo.get()).isPresent());
        assertNotEquals(relleno, reemplazo.get());
        assertFalse(sesion.contains(reemplazo.get()), "No puede ser una que ya estaba");
    }
}
