package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptedWeekResult;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdjustmentType;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.ConservationReport;
import main.web.services.fitsense.planning.domain.model.valueobjects.CorrectedSession;
import main.web.services.fitsense.planning.domain.model.valueobjects.DerivedWorkout;
import main.web.services.fitsense.planning.domain.model.valueobjects.GenerationSource;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningProfile;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.PreviousWeekSummary;
import main.web.services.fitsense.planning.domain.model.valueobjects.SafetyProfile;
import main.web.services.fitsense.planning.domain.model.valueobjects.WeekDerivation;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El productor de la semana adaptada.
 * <p>
 * Las cuatro que importan: localizada, ampliada, semana sin cambios y fallo por
 * dolor. En las cuatro se comprueba que NO SE PIERDE LA IDENTIDAD DE ORIGEN,
 * que es la razon de que todo esto lleve {@code DerivedWorkout} y no
 * {@code PlanDraft}.
 * <p>
 * Se usan el {@code SessionFixApplier} y el {@code ConservationValidator}
 * REALES, no imitaciones: lo unico sustituido es la validacion estricta, que
 * necesita el validador entero con su configuracion de Spring.
 */
class AdaptedWeekProducerTest {

    private static final LocalDate L1 = LocalDate.of(2026, 10, 12);   // semana medida
    private static final LocalDate L2 = LocalDate.of(2026, 10, 19);   // semana nueva

    private final EstrictaDePrueba estricta = new EstrictaDePrueba();
    private final AdaptedWeekProducer producer = new AdaptedWeekProducer(
            new SessionFixApplier(), new ConservationValidator(), estricta);

    // =======================================================================
    // 1 · Adaptacion localizada
    // =======================================================================

    @Test
    @DisplayName("Localizada: corrige la senalada, conserva el resto y ninguna pierde su origen")
    void localizada() {
        var orden = senalando(Map.of(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR));

        var r = producer.produce(contexto(orden), derivacion(),
                List.of(correccion(102L, L2.plusDays(2), 35, ej(304L, 2, 10))), catalogo());

        assertTrue(r.publicable(), () -> r.problemas().toString());
        assertEquals(List.of(101L, 102L, 103L), r.origenes(),
                "las tres sesiones siguen diciendo de cual provienen");
        assertEquals(GenerationSource.AI, r.source(), "la IA cambio algo de verdad");
        assertEquals(AdaptedWeekResult.Motivo.CORRECCION_IA, r.motivo());

        // La corregida cambio; las otras dos son las de la derivacion, intactas.
        assertEquals(35, sesion(r, 102L).expectedDurationMinutes());
        assertEquals(List.of(740L), ejerciciosDe(r, 101L), "conservada, sin tocar");
        assertEquals(List.of(592L), ejerciciosDe(r, 103L), "conservada, sin tocar");
    }

    // =======================================================================
    // 2 · Adaptacion ampliada
    // =======================================================================

    @Test
    @DisplayName("Ampliada: dos senaladas en UNA sola pasada, y las dos conservan su origen")
    void ampliada() {
        var orden = senalando(new LinkedHashMap<>(Map.of(
                101L, TipoIntervencion.REPROGRAMAR_DIAS,
                102L, TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA)));

        var r = producer.produce(contexto(orden), derivacion(), List.of(
                correccion(101L, L2.plusDays(1), 45, ej(740L, 3, 10)),
                correccion(102L, L2.plusDays(2), 45, ej(305L, 3, 10))), catalogo());

        assertTrue(r.publicable(), () -> r.problemas().toString());
        assertEquals(List.of(101L, 102L, 103L), r.origenes());
        assertEquals(GenerationSource.AI, r.source());
        assertEquals(AdaptedWeekResult.Motivo.CORRECCION_IA, r.motivo());

        assertEquals(L2.plusDays(1), sesion(r, 101L).scheduledDate(), "se reprogramo el dia");
        assertEquals(List.of(305L), ejerciciosDe(r, 102L), "se sustituyo el ejercicio");
    }

    // =======================================================================
    // 3 · Semana sin cambios
    // =======================================================================

    @Test
    @DisplayName("Sin orden: continuidad con CONSERVED, no regeneracion, y con los origenes")
    void sinCambios() {
        var r = producer.produce(contexto(PlanAdjustment.none()), derivacion(), null, catalogo());

        assertTrue(r.publicable(), () -> r.problemas().toString());
        assertEquals(GenerationSource.CONSERVED, r.source());
        assertEquals(AdaptedWeekResult.Motivo.SIN_ORDEN, r.motivo(),
                "V32 distingue SIN_ORDEN de SIN_EFECTO y de RESPALDO: no se deduce");
        assertEquals(List.of(101L, 102L, 103L), r.origenes(),
                "conservar no es regenerar: siguen siendo las mismas sesiones");
        assertEquals(List.of(740L), ejerciciosDe(r, 101L));
        assertTrue(r.conservacion().aplicadas().isEmpty(), "no se adapto nada");
    }

    @Test
    @DisplayName("La IA contesta pero no cambia nada: CONSERVED, no AI")
    void contestaSinCambiarNada() {
        var orden = senalando(Map.of(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR));

        // sessions: [] -> ninguna correccion. No es un fallo: es una respuesta.
        var r = producer.produce(contexto(orden), derivacion(), List.of(), catalogo());

        assertTrue(r.publicable(), () -> r.problemas().toString());
        assertEquals(GenerationSource.CONSERVED, r.source(),
                "lo que se publica es la semana anterior, aunque el modelo respondiera");
        assertEquals(AdaptedWeekResult.Motivo.SIN_EFECTO, r.motivo(),
                "la adaptacion ocurrio y no tuvo efecto: las dos cosas son ciertas");
        assertEquals(List.of(101L, 102L, 103L), r.origenes());
    }

    // =======================================================================
    // 4 · Fallo por dolor
    // =======================================================================

    @Test
    @DisplayName("Fallo de IA con dolor senalado: NO_FACTIBLE, sin plan y con motivo")
    void falloDeIaConDolor() {
        var orden = senalando(Map.of(102L, TipoIntervencion.SEGURIDAD_ANTE_DOLOR));

        // correcciones == null: no hubo respuesta usable.
        var r = producer.produce(contexto(orden), derivacion(), null, catalogo());

        assertFalse(r.publicable());
        assertEquals(AdaptedWeekResult.Estado.NO_FACTIBLE, r.estado());
        assertEquals(AdaptedWeekResult.Motivo.RESPALDO_IA, r.motivo(),
                "el respaldo es un registro propio, distinto del intento rechazado");
        assertTrue(r.semana().isEmpty(), "no se publica nada");
        assertTrue(r.problemas().getFirst().contains("102"), () -> r.problemas().toString());
        assertTrue(r.problemas().getFirst().contains("dolio"),
                "el motivo tiene que decir por que, para la evidencia de V32");
    }

    @Test
    @DisplayName("Fallo de IA SIN dolor: se conserva, que es la continuidad segura")
    void falloDeIaSinDolor() {
        var orden = senalando(Map.of(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR));

        var r = producer.produce(contexto(orden), derivacion(), null, catalogo());

        assertTrue(r.publicable(), () -> r.problemas().toString());
        assertEquals(GenerationSource.CONSERVED, r.source());
        assertEquals(AdaptedWeekResult.Motivo.RESPALDO_IA, r.motivo());
        assertEquals(List.of(101L, 102L, 103L), r.origenes());
    }

    // =======================================================================
    // Que lo conservado sea de verdad una copia
    // =======================================================================

    @Test
    @DisplayName("Sin orden, una sesion que desaparece de la derivacion NO se publica")
    void laContinuidadNoPuedePerderSesiones() {
        // WeekDerivationBuilder deja que quien llama elija que sesiones
        // sobreviven. Antes, sin orden, V8C no entraba y esto se publicaba como
        // CONSERVED sin que nadie lo mirara.
        var mocha = new WeekDerivation(List.of(
                derivada(101L, L2, 740L),
                derivada(103L, L2.plusDays(4), 592L)), List.of());

        var r = producer.produce(contexto(PlanAdjustment.none()), mocha, null, catalogo());

        assertFalse(r.publicable(), () -> "falta la 102 y se publico igual: " + r.semana());
        assertEquals(AdaptedWeekResult.Motivo.SIN_ORDEN, r.motivo());
        assertTrue(r.problemas().toString().contains("102"), () -> r.problemas().toString());
    }

    @Test
    @DisplayName("Sin orden, una sesion con los ejercicios cambiados NO se publica")
    void laContinuidadNoPuedeCambiarEjercicios() {
        var tocada = new WeekDerivation(List.of(
                derivada(101L, L2, 740L),
                derivada(102L, L2.plusDays(2), 305L),   // era 304
                derivada(103L, L2.plusDays(4), 592L)), List.of());

        var r = producer.produce(contexto(PlanAdjustment.none()), tocada, null, catalogo());

        assertFalse(r.publicable(), "una copia que cambia ejercicios no es una copia");
        assertTrue(r.problemas().toString().contains("102"), () -> r.problemas().toString());
    }

    @Test
    @DisplayName("El respaldo por fallo de IA se comprueba igual que la continuidad")
    void elRespaldoTambienSeComprueba() {
        var orden = senalando(Map.of(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR));
        var mocha = new WeekDerivation(List.of(
                derivada(101L, L2, 740L),
                derivada(103L, L2.plusDays(4), 592L)), List.of());

        var r = producer.produce(contexto(orden), mocha, null, catalogo());

        assertFalse(r.publicable(), "conservar mal no es continuidad segura");
        assertEquals(AdaptedWeekResult.Motivo.RESPALDO_IA, r.motivo());
    }

    @Test
    @DisplayName("Un respaldo no puede declararse de origen AI")
    void elRespaldoNoPuedeDeclararseAI() {
        // Si se cuela, la cifra de IA frente a reglas de la tesis queda inflada
        // con semanas en las que la IA no hizo nada.
        var e = assertThrows(IllegalArgumentException.class,
                () -> new AdaptedWeekResult(AdaptedWeekResult.Estado.PUBLICABLE,
                        AdaptedWeekResult.Motivo.RESPALDO_IA,
                        List.of(derivada(101L, L2, 740L)), GenerationSource.AI,
                        ConservationReport.vacio(), List.of()));

        assertTrue(e.getMessage().contains("RESPALDO_IA"), e::getMessage);
    }

    // =======================================================================
    // Dolor registrado en una sesion que NADIE senalo
    // =======================================================================

    @Test
    @DisplayName("INSUFFICIENT_DATA: una sesion con dolor no se repropone tal cual")
    void dolorConDatosInsuficientes() {
        // Sin orden -no hay cobertura para decidir-, la semana se conservaba
        // entera. Pero una de esas sesiones registro dolor, y copiarla es volver
        // a prescribir lo que dolio.
        var r = producer.produce(contexto(PlanAdjustment.none(), conDolorDeSesion(102L)),
                derivacion(), null, catalogo());

        assertFalse(r.publicable(), () -> "se repropuso la sesion con dolor: " + r.semana());
        assertEquals(AdaptedWeekResult.Motivo.SIN_ORDEN, r.motivo());
        assertTrue(r.problemas().toString().contains("102"), () -> r.problemas().toString());
        assertTrue(r.problemas().toString().contains("dolor"), () -> r.problemas().toString());
    }

    @Test
    @DisplayName("Se adapta una sesion y OTRA con dolor queda sin senalar: no se publica")
    void dolorEnUnaNoSenaladaMientrasSeAdaptaOtra() {
        var orden = senalando(Map.of(101L, TipoIntervencion.ACORTAR_O_REORGANIZAR));

        var r = producer.produce(contexto(orden, conDolorDeSesion(103L)), derivacion(),
                List.of(correccion(101L, L2, 35, ej(740L, 2, 10))), catalogo());

        assertFalse(r.publicable(),
                () -> "la 103 dolio, nadie la senalo y se copio igual: " + r.problemas());
        assertTrue(r.problemas().toString().contains("103"), () -> r.problemas().toString());
    }

    @Test
    @DisplayName("Dolor identificado por ejercicio durante continuidad: ese no puede volver")
    void dolorIdentificadoEnContinuidad() {
        // Aqui si consta QUE ejercicio dolio: el 592 de la sesion 103.
        var r = producer.produce(contexto(PlanAdjustment.none(), conDolorDeEjercicio(103L, 592L)),
                derivacion(), null, catalogo());

        assertFalse(r.publicable());
        assertTrue(r.problemas().toString().contains("592"),
                () -> "el mensaje tiene que nombrar el ejercicio: " + r.problemas());
    }

    @Test
    @DisplayName("Senalada por falta de tiempo con dolor registrado: conservar el ejercicio se rechaza")
    void dolorEnUnaSenaladaPorOtroMotivo() {
        // La orden la senalo para ACORTAR, no por seguridad. Antes, la
        // comprobacion de dolor solo corria si la intervencion exigia revision
        // de seguridad, asi que acortar la sesion y conservar el ejercicio que
        // dolio pasaba limpio.
        var orden = senalando(Map.of(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR));

        var r = producer.produce(contexto(orden, conDolorDeEjercicio(102L, 304L)),
                derivacion(),
                List.of(correccion(102L, L2.plusDays(2), 30, ej(304L, 2, 10))), catalogo());

        assertFalse(r.publicable(),
                () -> "se acorto la sesion y se conservo el ejercicio que dolio: " + r.problemas());
        assertTrue(r.problemas().toString().contains("304"), () -> r.problemas().toString());
        assertTrue(r.problemas().toString().contains("ACORTAR_O_REORGANIZAR"),
                () -> "el mensaje tiene que decir que se senalo por OTRO motivo: " + r.problemas());
        assertFalse(r.conservacion().aplicadas().contains(102L),
                "y no puede contar como adaptacion aplicada");
    }

    @Test
    @DisplayName("Senalada por otro motivo, con dolor, y SUSTITUYE el ejercicio: se acepta")
    void dolorResueltoEnUnaSenaladaQueSustituye() {
        // El limite por el otro lado: si el ejercicio que dolio NO vuelve, la
        // comprobacion de dolor no tiene nada que objetar. Si esta prueba
        // falla, la proteccion se volvio un bloqueo indiscriminado.
        var orden = senalando(Map.of(102L, TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA));

        var r = producer.produce(contexto(orden, conDolorDeEjercicio(102L, 304L)),
                derivacion(),
                List.of(correccion(102L, L2.plusDays(2), 45, ej(305L, 3, 10))), catalogo());

        assertTrue(r.publicable(), () -> r.problemas().toString());
        assertEquals(List.of(305L), ejerciciosDe(r, 102L));
    }

    @Test
    @DisplayName("Coexistencia: dolor de sesion Y en un ejercicio; sustituir solo ese no basta")
    void dolorGeneralYEspecificoALaVez() {
        // La 102 tenia 304 y 306. Consta dolor DE SESION y ademas dolor en 304.
        // Sustituir 304 y conservar 306 solo seria seguro si el dolor de sesion
        // fuera un resumen del especifico, y esa equivalencia no esta
        // establecida: puede ser una declaracion propia.
        var orden = senalando(Map.of(102L, TipoIntervencion.SEGURIDAD_ANTE_DOLOR));

        var r = producer.produce(contexto(orden, dolorGeneralYEn(102L, 304L)),
                derivacionConDosEjercicios(),
                List.of(correccion(102L, L2.plusDays(2), 45, ej(305L, 3, 10), ej(306L, 3, 10))),
                catalogo());

        assertFalse(r.publicable(),
                () -> "se sustituyo 304 pero 306 sigue, y el dolor de sesion no se resolvio: "
                        + r.problemas());
        assertTrue(r.problemas().toString().contains("306"), () -> r.problemas().toString());
    }

    @Test
    @DisplayName("Coexistencia resuelta: no vuelve ninguno de los dos, se acepta")
    void dolorGeneralYEspecificoResuelto() {
        // El limite por el otro lado: si no vuelve NINGUNO, no queda nada que
        // pudiera ser el que dolio.
        var orden = senalando(Map.of(102L, TipoIntervencion.SEGURIDAD_ANTE_DOLOR));

        var r = producer.produce(contexto(orden, dolorGeneralYEn(102L, 304L)),
                derivacionConDosEjercicios(),
                List.of(correccion(102L, L2.plusDays(2), 45, ej(305L, 3, 10), ej(592L, 3, 10))),
                catalogo());

        assertTrue(r.publicable(), () -> r.problemas().toString());
    }

    @Test
    @DisplayName("Una sesion insegura NO se clasifica como conservada")
    void laInseguraNoCuentaComoConservada() {
        // Si contara, la evidencia de V32 diria que se conservo correctamente
        // una sesion que de hecho bloqueo la semana por seguridad.
        var r = producer.produce(contexto(PlanAdjustment.none(), conDolorDeSesion(102L)),
                derivacion(), null, catalogo());

        assertFalse(r.publicable());
        assertFalse(r.conservacion().conservadas().contains(102L),
                () -> "la 102 tiene un problema de seguridad y figura como conservada: "
                        + r.conservacion().conservadas());
        assertEquals(List.of(101L, 103L), r.conservacion().conservadas(),
                "las otras dos si se conservaron bien");
    }

    @Test
    @DisplayName("Sin dolor registrado, la continuidad no se estorba")
    void sinDolorLaContinuidadSigue() {
        var r = producer.produce(contexto(PlanAdjustment.none()), derivacion(), null, catalogo());

        assertTrue(r.publicable(), () -> r.problemas().toString());
        assertEquals(3, r.conservacion().conservadas().size(),
                "las tres se clasifican como conservadas");
    }

    // =======================================================================
    // Las condiciones que no pueden relajarse
    // =======================================================================

    @Test
    @DisplayName("No se construye con una validacion que no bloquee la dosis 4D")
    void exigeBloquearDosis4D() {
        var floja = new EstrictaDePrueba();
        floja.bloquea = false;

        var e = assertThrows(IllegalArgumentException.class,
                () -> new AdaptedWeekProducer(new SessionFixApplier(),
                        new ConservationValidator(), floja));

        assertTrue(e.getMessage().contains("OBSERVE"), e::getMessage);
    }

    @Test
    @DisplayName("Un problema de la validacion estricta impide publicar")
    void problemaEstrictoNoPublica() {
        estricta.problemas = List.of("V24: el ejercicio 304 del 2026-10-21 lleva 2x10");
        var orden = senalando(Map.of(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR));

        var r = producer.produce(contexto(orden), derivacion(),
                List.of(correccion(102L, L2.plusDays(2), 35, ej(304L, 2, 10))), catalogo());

        assertFalse(r.publicable());
        assertTrue(r.semana().isEmpty());
        assertTrue(r.problemas().getFirst().contains("V24"));
    }

    @Test
    @DisplayName("Un problema de V8C impide publicar, igual que uno estricto")
    void problemaDeV8CNoPublica() {
        // ACORTAR_O_REORGANIZAR no autoriza sustituir ejercicios. Si la IA
        // sustituye igual, lo tiene que atrapar V8C — no la validacion
        // estricta, que aqui devuelve vacio a proposito.
        estricta.problemas = List.of();
        var orden = senalando(Map.of(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR));

        var r = producer.produce(contexto(orden), derivacion(),
                List.of(correccion(102L, L2.plusDays(2), 45, ej(305L, 3, 10))), catalogo());

        assertFalse(r.publicable(),
                () -> "V8C tenia que rechazar la sustitucion no autorizada: " + r.problemas());
        assertTrue(r.semana().isEmpty());
        assertFalse(r.problemas().isEmpty(), "y el motivo tiene que llegar a la evidencia");
        assertEquals(r.conservacion().problems(), r.problemas(),
                "los problemas de V8C se arrastran enteros, no se resumen");
    }

    @Test
    @DisplayName("Sin orden no se aplica nada, aunque lleguen correcciones")
    void sinOrdenNoSeAplicaNada() {
        // Estado que no deberia ocurrir -sin orden no se llama a la IA-, y el
        // guardia es justo lo que hace que no ocurra. Si se cayera, unas
        // correcciones sueltas modificarian una semana que nadie autorizo tocar.
        var r = producer.produce(contexto(PlanAdjustment.none()), derivacion(),
                List.of(correccion(102L, L2.plusDays(2), 20, ej(305L, 2, 8))), catalogo());

        assertTrue(r.publicable(), () -> r.problemas().toString());
        assertEquals(GenerationSource.CONSERVED, r.source());
        assertEquals(List.of(304L), ejerciciosDe(r, 102L),
                "la correccion se ignora: sin orden no hay nada autorizado");
        assertEquals(45, sesion(r, 102L).expectedDurationMinutes());
    }

    @Test
    @DisplayName("La semana conservada TAMBIEN se valida")
    void laConservadaTambienSeValida() {
        // Era valida cuando se publico, pero las reglas pueden haberse
        // endurecido. Publicar sin mirar daria por bueno algo que hoy no lo es.
        estricta.problemas = List.of("V1: el ejercicio 740 ya no esta en available_exercises.");

        var r = producer.produce(contexto(PlanAdjustment.none()), derivacion(), null, catalogo());

        assertFalse(r.publicable(), "conservar no exime de validar");
        assertTrue(r.problemas().getFirst().contains("V1"));
    }

    @Test
    @DisplayName("El borrador que ven los validadores lleva las MISMAS sesiones")
    void elBorradorLlevaLaSemana() {
        var orden = senalando(Map.of(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR));

        producer.produce(contexto(orden), derivacion(),
                List.of(correccion(102L, L2.plusDays(2), 35, ej(304L, 2, 10))), catalogo());

        assertNotNull(estricta.visto, "la validacion estricta tiene que ejecutarse");
        assertEquals(3, estricta.visto.workouts().size(),
                "el borrador es la semana entera, no solo la corregida");
        assertEquals(35, estricta.visto.workouts().stream()
                .filter(w -> w.scheduledDate().equals(L2.plusDays(2)))
                .findFirst().orElseThrow().expectedDurationMinutes());
    }

    @Test
    @DisplayName("Sin derivacion no se adapta: la primera semana es generacion inicial")
    void sinDerivacion() {
        var e = assertThrows(IllegalArgumentException.class,
                () -> producer.produce(contexto(PlanAdjustment.none()), null, null, catalogo()));

        assertTrue(e.getMessage().contains("primera semana"), e::getMessage);
    }

    @Test
    @DisplayName("Publicable con problemas es imposible de construir")
    void publicableConProblemasEsImposible() {
        var e = assertThrows(IllegalArgumentException.class,
                () -> new AdaptedWeekResult(AdaptedWeekResult.Estado.PUBLICABLE,
                        AdaptedWeekResult.Motivo.CORRECCION_IA,
                        List.of(derivada(101L, L2, 740L)), GenerationSource.AI,
                        ConservationReport.vacio(), List.of("algo falla")));

        assertTrue(e.getMessage().contains("contradiccion"), e::getMessage);
    }

    // =======================================================================
    // Correccion 4 (27/10/2026): la duracion la decide el contenido
    // =======================================================================

    @Test
    @DisplayName("C4 · mismos ejercicios y dosis, solo otros minutos -> SIN_EFECTO y la duracion validada")
    void soloMinutosEsSinEfecto() {
        for (var t : List.of(TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION, TipoIntervencion.ACORTAR_O_REORGANIZAR,
                TipoIntervencion.INVESTIGAR_CAMBIO_MINIMO)) {
            var r = producer.produce(contexto(senalando(Map.of(102L, t))), derivacion(),
                    List.of(correccion(102L, L2.plusDays(2), 30, ej(304L, 3, 10))), catalogo());
            assertTrue(r.publicable(), () -> r.problemas().toString());
            assertEquals(List.of(102L), r.conservacion().sinEfecto(), t + ": los minutos solos no son adaptacion");
            assertTrue(r.conservacion().aplicadas().isEmpty(), t.name());
            assertEquals(GenerationSource.CONSERVED, r.source());
            assertEquals(45, sesion(r, 102L).expectedDurationMinutes(), "no se publica un tiempo inventado");
            assertTrue(r.conservacion().correccionesDelBackend().stream().anyMatch(c -> c.startsWith("DURACION")),
                    r.conservacion().correccionesDelBackend().toString());
        }
    }

    @Test
    @DisplayName("C4 · menos series autorizadas, duracion coherente -> APLICADA con su duracion")
    void menosSeriesEsAplicada() {
        var r = producer.produce(contexto(senalando(Map.of(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR))),
                derivacion(), List.of(correccion(102L, L2.plusDays(2), 32, ej(304L, 2, 10))), catalogo());
        assertTrue(r.publicable(), () -> r.problemas().toString());
        assertEquals(List.of(102L), r.conservacion().aplicadas());
        assertEquals(32, sesion(r, 102L).expectedDurationMinutes());
        assertTrue(r.conservacion().correccionesDelBackend().isEmpty());
    }

    @Test
    @DisplayName("C4 · menos repeticiones autorizadas -> APLICADA")
    void menosRepeticionesEsAplicada() {
        var r = producer.produce(contexto(senalando(Map.of(102L, TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION))),
                derivacion(), List.of(correccion(102L, L2.plusDays(2), 40, ej(304L, 3, 8))), catalogo());
        assertTrue(r.publicable(), () -> r.problemas().toString());
        assertEquals(List.of(102L), r.conservacion().aplicadas());
        assertEquals(40, sesion(r, 102L).expectedDurationMinutes());
    }

    @Test
    @DisplayName("C4 · menos descanso en una intervencion que permite cambiar la dosis -> adaptacion real")
    void menosDescansoEsAplicada() {
        var r = producer.produce(contexto(senalando(Map.of(102L, TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION))),
                derivacion(), List.of(correccion(102L, L2.plusDays(2), 40, new PlanDraft.DraftExercise(304L,
                        PrescriptionType.SETS_REPS, (short) 3, (short) 10, null, null, (short) 60, null))), catalogo());
        assertTrue(r.publicable(), () -> r.problemas().toString());
        assertEquals(List.of(102L), r.conservacion().aplicadas());
    }

    @Test
    @DisplayName("C4 · cambiar el descanso donde la intervencion no permite tocar la dosis se sigue rechazando")
    void descansoSinPermisoRechazado() {
        var r = producer.produce(contexto(senalando(Map.of(102L, TipoIntervencion.REPROGRAMAR_DIAS))),
                derivacion(), List.of(correccion(102L, L2.plusDays(2), 40, new PlanDraft.DraftExercise(304L,
                        PrescriptionType.SETS_REPS, (short) 3, (short) 10, null, null, (short) 60, null))), catalogo());
        assertFalse(r.publicable());
        assertTrue(r.conservacion().aplicadas().isEmpty());
    }

    @Test
    @DisplayName("C4 · el respaldo y la copia no tocan minutos (no hay propuesta de la IA)")
    void continuidadSinCambiosDeMinutos() {
        var r = producer.produce(contexto(PlanAdjustment.none()), derivacion(), null, catalogo());
        assertTrue(r.publicable(), () -> r.problemas().toString());
        assertEquals(45, sesion(r, 102L).expectedDurationMinutes());
        assertTrue(r.conservacion().correccionesDelBackend().isEmpty());
    }

    // =======================================================================
    // Material
    // =======================================================================

    /** La validacion estricta, sustituida: necesita el validador con Spring. */
    private static final class EstrictaDePrueba implements StrictPlanValidation {
        private List<String> problemas = List.of();
        private boolean bloquea = true;
        private PlanDraft visto;

        @Override
        public List<String> revisar(PlanDraft draft, PlanGenerationContext context,
                                    ConservationReport conservacion) {
            this.visto = draft;
            return problemas;
        }

        @Override
        public boolean bloqueaDosis4D() {
            return bloquea;
        }
    }

    private static PlanDraft.DraftWorkout sesion(AdaptedWeekResult r, long origen) {
        return r.semana().stream()
                .filter(d -> d.sourcePlannedWorkoutId().equals(origen))
                .findFirst().orElseThrow(() -> new AssertionError("falta el origen " + origen))
                .workout();
    }

    private static List<Long> ejerciciosDe(AdaptedWeekResult r, long origen) {
        return sesion(r, origen).exercises().stream()
                .map(PlanDraft.DraftExercise::exerciseId).toList();
    }

    private static PlanAdjustment senalando(Map<Long, TipoIntervencion> senaladas) {
        var lista = new ArrayList<PlanAdjustment.TargetedSession>();
        senaladas.forEach((id, tipo) -> lista.add(new PlanAdjustment.TargetedSession(id, tipo)));
        lista.sort((a, b) -> Long.compare(a.plannedWorkoutId(), b.plannedWorkoutId()));
        return new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null,
                0.0, 0.0, null, null, null, "baja adherencia", null, null, lista);
    }

    private static PlanGenerationContext contexto(PlanAdjustment orden) {
        return contexto(orden, semanaMedida());
    }

    private static PlanGenerationContext contexto(PlanAdjustment orden,
                                                  PreviousWeekSummary semana) {
        return new PlanGenerationContext(1L, (short) 2, L2, L2.plusDays(6),
                perfil(), orden, semana, List.of(), null, null);
    }

    /** Dolor declarado EN LA SESION, sin decir que ejercicio fue. */
    private static PreviousWeekSummary conDolorDeSesion(long workoutId) {
        var base = semanaMedida();
        var workouts = base.workouts().stream()
                .map(w -> w.plannedWorkoutId() != workoutId ? w
                        : new PreviousWeekSummary.WorkoutOutcome(w.plannedWorkoutId(),
                        w.scheduledDate(), w.focusCode(), "SKIPPED",
                        "PAIN_OR_DISCOMFORT", true, w.sessionStatus(), w.sessionRpe(),
                        w.completionPct(), w.workoutName(), w.expectedDurationMinutes()))
                .toList();
        return new PreviousWeekSummary(base.weightedAdherencePct(), base.averageSessionRpe(),
                base.totalVolume(), base.bodyPartDistribution(), workouts,
                base.prescriptions(), base.exerciseIdsUsedLast7Days());
    }

    /** La 102 con DOS ejercicios, para poder sustituir uno y conservar el otro. */
    private static WeekDerivation derivacionConDosEjercicios() {
        return new WeekDerivation(List.of(
                derivada(101L, L2, 740L),
                new DerivedWorkout(102L, new PlanDraft.DraftWorkout(L2.plusDays(2),
                        WorkoutFocus.FULL_BODY, "Cuerpo completo", 45,
                        List.of(ej(304L, 3, 10), ej(306L, 3, 10)))),
                derivada(103L, L2.plusDays(4), 592L)), List.of());
    }

    /** Dolor DE SESION y ADEMAS en un ejercicio concreto de esa misma sesion. */
    private static PreviousWeekSummary dolorGeneralYEn(long workoutId, long exerciseId) {
        var conGeneral = conDolorDeSesion(workoutId);
        var prescriptions = new ArrayList<>(conGeneral.prescriptions().stream()
                .map(p -> !(p.plannedWorkoutId() == workoutId && p.exerciseId() == exerciseId) ? p
                        : new PreviousWeekSummary.PrescriptionOutcome(p.plannedWorkoutId(),
                        p.scheduledDate(), p.exerciseId(), p.name(), p.prescriptionType(),
                        p.sets(), p.reps(), p.durationSeconds(), p.loadKg(),
                        p.actualSets(), p.actualRepsTotal(), p.actualDurationSeconds(),
                        p.actualLoadKg(), p.completionPct(), p.exerciseStatus(),
                        "PAIN_OR_DISCOMFORT", p.status(), p.restSeconds()))
                .toList());
        // El segundo ejercicio de la 102, que NO dolio pero pertenece a la
        // sesion con dolor general.
        prescriptions.add(prescripcion(workoutId, 306L));

        return new PreviousWeekSummary(conGeneral.weightedAdherencePct(),
                conGeneral.averageSessionRpe(), conGeneral.totalVolume(),
                conGeneral.bodyPartDistribution(), conGeneral.workouts(),
                List.copyOf(prescriptions), conGeneral.exerciseIdsUsedLast7Days());
    }

    /** Dolor identificado EN UN EJERCICIO concreto. */
    private static PreviousWeekSummary conDolorDeEjercicio(long workoutId, long exerciseId) {
        var base = semanaMedida();
        var prescriptions = base.prescriptions().stream()
                .map(p -> !(p.plannedWorkoutId() == workoutId && p.exerciseId() == exerciseId) ? p
                        : new PreviousWeekSummary.PrescriptionOutcome(p.plannedWorkoutId(),
                        p.scheduledDate(), p.exerciseId(), p.name(), p.prescriptionType(),
                        p.sets(), p.reps(), p.durationSeconds(), p.loadKg(),
                        p.actualSets(), p.actualRepsTotal(), p.actualDurationSeconds(),
                        p.actualLoadKg(), p.completionPct(), p.exerciseStatus(),
                        "PAIN_OR_DISCOMFORT", p.status(), p.restSeconds()))
                .toList();
        return new PreviousWeekSummary(base.weightedAdherencePct(), base.averageSessionRpe(),
                base.totalVolume(), base.bodyPartDistribution(), base.workouts(),
                prescriptions, base.exerciseIdsUsedLast7Days());
    }

    private static PlanningProfile perfil() {
        return new PlanningProfile(1L, 30, "M", new BigDecimal("175"), new BigDecimal("75"),
                new BigDecimal("72"), "INTERMEDIATE", "GAIN_MUSCLE", "ganar musculo", null,
                "GYM", 3, List.of((short) 1, (short) 3, (short) 5), 45,
                List.of("body weight"), List.of(),
                SafetyProfile.Declaradas.sinLimitaciones());
    }

    private static WeekDerivation derivacion() {
        return new WeekDerivation(List.of(
                derivada(101L, L2, 740L),
                derivada(102L, L2.plusDays(2), 304L),
                derivada(103L, L2.plusDays(4), 592L)), List.of());
    }

    private static DerivedWorkout derivada(long origen, LocalDate fecha, long ejercicio) {
        return new DerivedWorkout(origen, new PlanDraft.DraftWorkout(fecha,
                WorkoutFocus.FULL_BODY, "Cuerpo completo", 45, List.of(ej(ejercicio, 3, 10))));
    }

    private static PlanDraft.DraftExercise ej(long id, int series, int reps) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS,
                (short) series, (short) reps, null, null, (short) 90, null);
    }

    private static CorrectedSession correccion(long origen, LocalDate fecha, int minutos,
                                               PlanDraft.DraftExercise... ejercicios) {
        return new CorrectedSession(origen, fecha, minutos, List.of(ejercicios), "porque si");
    }

    private static Map<Long, CandidateExercise> catalogo() {
        var mapa = new LinkedHashMap<Long, CandidateExercise>();
        for (long id : new long[] {740L, 304L, 305L, 306L, 592L}) mapa.put(id, candidato(id));
        return mapa;
    }

    private static CandidateExercise candidato(long id) {
        return new CandidateExercise(id, "ejercicio " + id, "CHEST", "body weight", 2,
                PrescriptionType.SETS_REPS, "pectoral", "PUSH_H");
    }

    private static PreviousWeekSummary semanaMedida() {
        var workouts = List.of(
                outcome(101L, L1, "COMPLETED", null, new BigDecimal("100.00")),
                outcome(102L, L1.plusDays(2), "SKIPPED", "LACK_OF_TIME", BigDecimal.ZERO),
                outcome(103L, L1.plusDays(4), "COMPLETED", null, new BigDecimal("100.00")));

        var prescriptions = List.of(
                prescripcion(101L, 740L), prescripcion(102L, 304L), prescripcion(103L, 592L));

        return new PreviousWeekSummary(new BigDecimal("66.67"), new BigDecimal("6.5"), 480,
                Map.of(), workouts, prescriptions, List.of());
    }

    private static PreviousWeekSummary.WorkoutOutcome outcome(long id, LocalDate fecha,
                                                              String estado, String motivo,
                                                              BigDecimal pct) {
        return new PreviousWeekSummary.WorkoutOutcome(id, fecha, "FULL_BODY", estado,
                motivo, true, estado, (short) 7, pct, "Cuerpo completo", (short) 45);
    }

    private static PreviousWeekSummary.PrescriptionOutcome prescripcion(long workoutId,
                                                                        long exerciseId) {
        return new PreviousWeekSummary.PrescriptionOutcome(workoutId, L1, exerciseId,
                "ejercicio " + exerciseId, "SETS_REPS", (short) 3, (short) 10, null, null,
                (short) 3, 30, null, null, new BigDecimal("100.00"), "COMPLETED", null,
                "COMPLETED", (short) 90);
    }
}
