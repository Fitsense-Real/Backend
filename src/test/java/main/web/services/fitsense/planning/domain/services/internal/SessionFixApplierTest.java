package main.web.services.fitsense.planning.domain.services.internal;

import main.web.services.fitsense.planning.domain.model.valueobjects.AdjustmentType;
import main.web.services.fitsense.planning.domain.model.valueobjects.CorrectedSession;
import main.web.services.fitsense.planning.domain.model.valueobjects.DerivedWorkout;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.WeekDerivation;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import main.web.services.fitsense.planning.domain.services.SessionFixApplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El empalme del contrato A: la IA corrige solo las sesiones senaladas y la
 * correspondencia de V31 sobrevive.
 */
class SessionFixApplierTest {

    private static final LocalDate L2 = LocalDate.of(2026, 10, 19);   // lunes

    private final SessionFixApplier applier = new SessionFixApplier();

    // =======================================================================
    // Lo que el empalme garantiza por construccion
    // =======================================================================

    @Test
    @DisplayName("Solo cambia la sesion corregida; las demas quedan intactas")
    void soloCambiaLaCorregida() {
        var r = applier.apply(semanaCopiada(),
                List.of(correccion(102L, L2.plusDays(2), 30, ej(304L, 2, 10, 90))),
                senalando(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR));

        assertEquals(3, r.workouts().size());

        // La 102 cambio.
        var corregida = r.from(102L).orElseThrow().workout();
        assertEquals(30, corregida.expectedDurationMinutes());
        assertEquals(List.of(304L), ids(corregida));
        assertEquals((short) 2, corregida.exercises().get(0).plannedSets());

        // Las otras dos, ni un campo.
        assertEquals(45, r.from(101L).orElseThrow().workout().expectedDurationMinutes());
        assertEquals(List.of(740L, 1101L), ids(r.from(101L).orElseThrow().workout()));
        assertEquals(List.of(592L), ids(r.from(103L).orElseThrow().workout()));
    }

    @Test
    @DisplayName("El origen de V31 sobrevive al empalme")
    void elOrigenSobrevive() {
        var r = applier.apply(semanaCopiada(),
                List.of(correccion(102L, L2.plusDays(2), 30, ej(304L, 2, 10, 90))),
                senalando(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR));

        assertEquals(List.of(101L, 102L, 103L),
                r.workouts().stream().map(DerivedWorkout::sourcePlannedWorkoutId).toList(),
                "sin el origen, V8C no puede comparar y V31 no puede persistir el vinculo");
    }

    @Test
    @DisplayName("El ENFOQUE no cambia, porque no viaja en la correccion")
    void elEnfoqueNoViaja() {
        // La IA no tiene forma de cambiarlo: lo pone el empalme desde la copia.
        var r = applier.apply(semanaCopiada(),
                List.of(correccion(102L, L2.plusDays(2), 30, ej(304L, 2, 10, 90))),
                senalando(102L, TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA));

        assertEquals(WorkoutFocus.FULL_BODY, r.from(102L).orElseThrow().workout().focus());
        assertEquals("Cuerpo completo B", r.from(102L).orElseThrow().workout().name(),
                "el nombre tampoco: la sesion sigue siendo la misma sesion");
    }

    @Test
    @DisplayName("Reprogramar: la fecha de la correccion manda")
    void reprogramar() {
        var r = applier.apply(semanaCopiada(),
                List.of(correccion(102L, L2.plusDays(3), 45, ej(304L, 3, 10, 90))),
                senalando(102L, TipoIntervencion.REPROGRAMAR_DIAS));

        assertEquals(L2.plusDays(3), r.from(102L).orElseThrow().workout().scheduledDate());
    }

    @Test
    @DisplayName("Adaptacion ampliada: se corrigen dos y la tercera se conserva")
    void ampliada() {
        var orden = new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null,
                0.0, 0.0, null, null, null, "baja adherencia", null, null,
                List.of(new PlanAdjustment.TargetedSession(101L,
                                TipoIntervencion.ACORTAR_O_REORGANIZAR),
                        new PlanAdjustment.TargetedSession(102L,
                                TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA)));

        var r = applier.apply(semanaCopiada(),
                List.of(correccion(101L, L2, 30, ej(740L, 2, 10, 90)),
                        correccion(102L, L2.plusDays(2), 45, ej(740L, 3, 10, 90))),
                orden);

        assertEquals(List.of(740L), ids(r.from(101L).orElseThrow().workout()));
        assertEquals(List.of(740L), ids(r.from(102L).orElseThrow().workout()));
        assertEquals(List.of(592L), ids(r.from(103L).orElseThrow().workout()),
                "la no senalada sigue igual");
    }

    // =======================================================================
    // Conservar una senalada esta permitido
    // =======================================================================

    @Test
    @DisplayName("Sin correcciones la semana queda conservada, no falla")
    void sinCorrecciones() {
        var base = semanaCopiada();
        var r = applier.apply(base, List.of(),
                senalando(102L, TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA));

        assertSame(base, r, "no hay nada que empalmar");
    }

    @Test
    @DisplayName("Si la IA corrige dos de tres, la tercera queda declarada sin corregir")
    void senaladasSinCorregir() {
        var orden = new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null,
                0.0, 0.0, null, null, null, "baja adherencia", null, null,
                List.of(new PlanAdjustment.TargetedSession(101L,
                                TipoIntervencion.ACORTAR_O_REORGANIZAR),
                        new PlanAdjustment.TargetedSession(102L,
                                TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA),
                        new PlanAdjustment.TargetedSession(103L,
                                TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION)));

        var correcciones = List.of(correccion(101L, L2, 30, ej(740L, 2, 10, 90)),
                correccion(103L, L2.plusDays(4), 45, ej(592L, 2, 10, 90)));

        assertEquals(List.of(102L), applier.senaladasSinCorregir(correcciones, orden),
                "V8C la registrara como adaptacion sin efecto");

        // Y el empalme no se queja: conservar una senalada esta permitido.
        var r = applier.apply(semanaCopiada(), correcciones, orden);
        assertEquals(List.of(304L), ids(r.from(102L).orElseThrow().workout()));
    }

    @Test
    @DisplayName("Sin orden de adaptacion no hay sesiones sin corregir, ni con respuesta nula")
    void sinOrdenNoHaySenaladas() {
        // Sin orden no hay nada sobre lo que el nulo signifique algo.
        assertTrue(applier.senaladasSinCorregir(List.of(), PlanAdjustment.none()).isEmpty());
        assertTrue(applier.senaladasSinCorregir(null, null).isEmpty());
        assertTrue(applier.senaladasSinCorregir(null, PlanAdjustment.none()).isEmpty());
    }

    @Test
    @DisplayName("Con orden activa y respuesta AUSENTE: falla, no devuelve todas como no corregidas")
    void senaladasSinCorregirConRespuestaAusente() {
        // Devolver todas las senaladas haria que el productor clasificara el
        // intento como adaptacion sin efecto en vez de como fallo: la misma
        // confusion de apply, por otra puerta.
        var e = assertThrows(IllegalArgumentException.class,
                () -> applier.senaladasSinCorregir(null,
                        senalando(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR)));

        assertTrue(e.getMessage().contains("ausente"), () -> e.getMessage());
    }

    @Test
    @DisplayName("Con orden activa y respuesta VACIA: todas las senaladas quedan sin corregir")
    void senaladasSinCorregirConRespuestaVacia() {
        // La IA contesto que no propone nada. Las tres quedan sin corregir, y
        // eso es un dato, no un fallo.
        var orden = new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null,
                0.0, 0.0, null, null, null, "baja adherencia", null, null,
                List.of(new PlanAdjustment.TargetedSession(101L,
                                TipoIntervencion.ACORTAR_O_REORGANIZAR),
                        new PlanAdjustment.TargetedSession(103L,
                                TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION)));

        assertEquals(List.of(101L, 103L), applier.senaladasSinCorregir(List.of(), orden));
    }

    // =======================================================================
    // Respuestas fuera de contrato: son errores, no planes malos
    // =======================================================================

    @Test
    @DisplayName("Una correccion sobre una sesion NO senalada se rechaza aqui")
    void correccionSobreNoSenalada() {
        var e = assertThrows(IllegalArgumentException.class, () -> applier.apply(
                semanaCopiada(),
                List.of(correccion(101L, L2, 30, ej(740L, 2, 10, 90))),
                senalando(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR)));

        assertTrue(e.getMessage().contains("NO senalo"), () -> e.getMessage());
    }

    @Test
    @DisplayName("Dos correcciones para la misma sesion se rechazan")
    void dosCorreccionesParaLaMisma() {
        var e = assertThrows(IllegalArgumentException.class, () -> applier.apply(
                semanaCopiada(),
                List.of(correccion(102L, L2.plusDays(2), 30, ej(304L, 2, 10, 90)),
                        correccion(102L, L2.plusDays(2), 40, ej(304L, 3, 10, 90))),
                senalando(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR)));

        assertTrue(e.getMessage().contains("dos correcciones"), () -> e.getMessage());
    }

    @Test
    @DisplayName("Una correccion para una sesion que no esta en la derivacion se rechaza")
    void correccionSinCopia() {
        // La 999 esta senalada pero no sobrevivio a la derivacion.
        var e = assertThrows(IllegalArgumentException.class, () -> applier.apply(
                semanaCopiada(),
                List.of(correccion(999L, L2, 30, ej(740L, 2, 10, 90))),
                senalando(999L, TipoIntervencion.ACORTAR_O_REORGANIZAR)));

        assertTrue(e.getMessage().contains("[999]"), () -> e.getMessage());
    }

    @Test
    @DisplayName("Correcciones sin orden de adaptacion se rechazan")
    void correccionesSinOrden() {
        var e = assertThrows(IllegalArgumentException.class, () -> applier.apply(
                semanaCopiada(),
                List.of(correccion(102L, L2.plusDays(2), 30, ej(304L, 2, 10, 90))),
                PlanAdjustment.none()));

        assertTrue(e.getMessage().contains("no senala ninguna"), () -> e.getMessage());
    }

    @Test
    @DisplayName("Una correccion sin decir cual corrige no se puede construir")
    void correccionSinOrigen() {
        assertThrows(IllegalArgumentException.class,
                () -> new CorrectedSession(null, L2, 30, List.of(), null));
    }

    // =======================================================================
    // Ausente NO es vacio: son dos respuestas distintas
    // =======================================================================

    @Test
    @DisplayName("Respuesta AUSENTE: falla, no se convierte en semana conservada")
    void respuestaAusente() {
        // Si una respuesta ausente pasara por aqui, la semana quedaria
        // conservada y V8C la registraria como adaptacion SIN EFECTO: un fallo
        // de la IA contado como un intento legitimo que no cambio nada.
        var e = assertThrows(IllegalArgumentException.class, () -> applier.apply(
                semanaCopiada(), null,
                senalando(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR)));

        assertTrue(e.getMessage().contains("ausente"), () -> e.getMessage());
    }

    @Test
    @DisplayName("Respuesta VACIA: valida, la semana queda conservada")
    void respuestaVacia() {
        // La IA contesto, y contesto que no propone nada. Es legitimo.
        var base = semanaCopiada();
        var r = applier.apply(base, List.of(),
                senalando(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR));

        assertSame(base, r, "[] y null no pueden tratarse igual");
    }

    @Test
    @DisplayName("exercises AUSENTE: prescripcion incompleta, respuesta invalida")
    void ejerciciosAusentes() {
        // Tratarlo como lista vacia convertia un campo olvidado en una orden de
        // vaciar la sesion.
        var e = assertThrows(IllegalArgumentException.class,
                () -> new CorrectedSession(102L, L2, 30, null, "sin ejercicios"));

        assertTrue(e.getMessage().contains("exercises"), () -> e.getMessage());
    }

    @Test
    @DisplayName("exercises VACIO: peticion explicita, la deciden V5, 4C y 4D")
    void ejerciciosVacios() {
        // El empalme la admite; dejar una sesion sin ejercicios no es asunto
        // suyo sino de las validaciones de contenido.
        var r = applier.apply(semanaCopiada(),
                List.of(new CorrectedSession(102L, L2.plusDays(2), 20, List.of(), "vaciada")),
                senalando(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR));

        assertTrue(r.from(102L).orElseThrow().workout().exercises().isEmpty());
        assertEquals(List.of(740L, 1101L), ids(r.from(101L).orElseThrow().workout()),
                "y las demas sesiones no se tocan");
    }

    // =======================================================================
    // Huecos de la respuesta: caen a la copia, no a nulo
    // =======================================================================

    @Test
    @DisplayName("Sin fecha ni duracion en la correccion, se usan las de la copia")
    void huecosCaenALaCopia() {
        var r = applier.apply(semanaCopiada(),
                List.of(new CorrectedSession(102L, null, null,
                        List.of(ej(304L, 2, 10, 90)), "mas corta")),
                senalando(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR));

        var corregida = r.from(102L).orElseThrow().workout();
        assertEquals(L2.plusDays(2), corregida.scheduledDate(),
                "una fecha nula no la rechaza ninguna validacion con un mensaje util");
        assertEquals(45, corregida.expectedDurationMinutes());
        assertEquals(List.of(304L), ids(corregida));
    }

    @Test
    @DisplayName("Las sesiones eliminadas por orden siguen declaradas tras el empalme")
    void lasEliminadasSiguenDeclaradas() {
        var base = new WeekDerivation(List.of(
                new DerivedWorkout(101L, sesion(L2, 45, "Cuerpo completo A",
                        ej(740L, 3, 10, 90), ej(1101L, 2, 13, 90))),
                new DerivedWorkout(102L, sesion(L2.plusDays(2), 45, "Cuerpo completo B",
                        ej(304L, 3, 10, 90)))),
                List.of(103L));

        var r = applier.apply(base,
                List.of(correccion(102L, L2.plusDays(2), 30, ej(304L, 2, 10, 90))),
                senalando(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR));

        assertEquals(List.of(103L), r.sourcesWithoutDerived(),
                "V8C necesita distinguir eliminada por orden de desaparecida");
    }

    // =======================================================================
    // Material
    // =======================================================================

    private static List<Long> ids(PlanDraft.DraftWorkout w) {
        return w.exercises().stream().map(PlanDraft.DraftExercise::exerciseId).toList();
    }

    private static PlanAdjustment senalando(long id, TipoIntervencion tipo) {
        return new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null,
                0.0, 0.0, null, null, null, "baja adherencia", null, null,
                List.of(new PlanAdjustment.TargetedSession(id, tipo)));
    }

    private static WeekDerivation semanaCopiada() {
        return new WeekDerivation(List.of(
                new DerivedWorkout(101L, sesion(L2, 45, "Cuerpo completo A",
                        ej(740L, 3, 10, 90), ej(1101L, 2, 13, 90))),
                new DerivedWorkout(102L, sesion(L2.plusDays(2), 45, "Cuerpo completo B",
                        ej(304L, 3, 10, 90))),
                new DerivedWorkout(103L, sesion(L2.plusDays(4), 45, "Cuerpo completo C",
                        ej(592L, 3, 10, 90)))),
                List.of());
    }

    private static PlanDraft.DraftWorkout sesion(LocalDate fecha, int minutos, String nombre,
                                                 PlanDraft.DraftExercise... ejercicios) {
        return new PlanDraft.DraftWorkout(fecha, WorkoutFocus.FULL_BODY, nombre, minutos,
                List.of(ejercicios));
    }

    private static CorrectedSession correccion(long origen, LocalDate fecha, int minutos,
                                               PlanDraft.DraftExercise... ejercicios) {
        return new CorrectedSession(origen, fecha, minutos, List.of(ejercicios), "prueba");
    }

    private static PlanDraft.DraftExercise ej(long id, int series, int reps, int descanso) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS,
                (short) series, (short) reps, null, null, (short) descanso, null);
    }
}
