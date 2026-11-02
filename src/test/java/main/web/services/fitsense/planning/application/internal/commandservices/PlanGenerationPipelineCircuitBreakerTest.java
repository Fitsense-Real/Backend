package main.web.services.fitsense.planning.application.internal.commandservices;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.domain.exceptions.AiProviderUnavailableException;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.exceptions.PlanGenerationFailedException;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.PlanDraftNormalizer;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.TrainingPlanGenerator;
import main.web.services.fitsense.planning.infrastructure.generation.ai.AirCircuitBreaker;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateTrainingPlanGenerator;
import main.web.services.fitsense.planning.infrastructure.generation.ai.AiRetryFeedback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * El cortacircuitos visto desde el pipeline. Dos cosas que comprobar, y la
 * segunda es la importante:
 * <ol>
 *   <li>abierto, NO se hace ni una llamada al proveedor;</li>
 *   <li>y eso se VE en el log, en WARN. Antes era {@code log.debug} con nivel
 *       por omision INFO, asi que diez generaciones podian salir por el motor
 *       de reglas sin dejar rastro, y una comparacion entre perfiles hecha en
 *       ese estado no valdria nada.</li>
 * </ol>
 * Lo demas de esta clase es un CERROJO sobre la politica 19.4: que la
 * instrumentacion no haya movido los reintentos, ni quien cuenta para el corte,
 * ni el respaldo.
 * <p>
 * NO SE EJECUTO EN EL ENTORNO DE PREPARACION: repo1.maven.org responde 403 y
 * sin Mockito no se puede correr. Hay que pasarla en el IDE.
 */
class PlanGenerationPipelineCircuitBreakerTest {

    private static final long USUARIO = 7L;
    private static final LocalDate LUNES = LocalDate.of(2026, 10, 12);

    /** Reloj que solo avanza cuando se le dice. */
    private static final class RelojFalso implements java.util.function.Supplier<Instant> {
        private Instant ahora = Instant.parse("2026-10-13T12:00:00Z");
        @Override public Instant get() { return ahora; }
        void avanzarMinutos(long minutos) { ahora = ahora.plusSeconds(minutos * 60); }
    }

    private ReplicateTrainingPlanGenerator ia;
    private TrainingPlanGenerator reglas;
    private PlanDraftValidator validador;
    private PlanDraftNormalizer normalizador;
    private AirCircuitBreaker breaker;
    private RelojFalso reloj;
    private PlanGenerationPipeline pipeline;

    private ListAppender<ILoggingEvent> logs;
    private ch.qos.logback.classic.Logger logger;

    @BeforeEach
    void montar() {
        ia = mock(ReplicateTrainingPlanGenerator.class);
        reglas = mock(TrainingPlanGenerator.class);
        validador = mock(PlanDraftValidator.class);
        normalizador = mock(PlanDraftNormalizer.class);
        reloj = new RelojFalso();
        breaker = new AirCircuitBreaker(reloj);
        pipeline = new PlanGenerationPipeline(ia, reglas, validador, normalizador, breaker);

        when(ia.isEnabled()).thenReturn(true);
        when(normalizador.normalize(any(), any())).thenAnswer(
                inv -> new PlanDraftNormalizer.Result(inv.getArgument(0), List.of()));
        when(reglas.generate(any(), anyList())).thenReturn(borrador(GenerationSource.RULE_ENGINE));

        logger = ((LoggerContext) LoggerFactory.getILoggerFactory())
                .getLogger(PlanGenerationPipeline.class);
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    void soltarLog() {
        logger.detachAppender(logs);
    }

    private List<String> avisos(Level nivel) {
        return logs.list.stream()
                .filter(e -> e.getLevel() == nivel)
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }

    private void abrirElCorte() {
        for (int i = 0; i < AirCircuitBreaker.UMBRAL; i++) breaker.registrarFalloDeProveedor();
        assertTrue(breaker.abierto(), "precondicion de la prueba");
    }

    // ---------------------------------------------------------------- 5 · corte

    @Test
    @DisplayName("5 · cortacircuito abierto: CERO llamadas al proveedor")
    void abiertoNoLlamaAlProveedor() {
        abrirElCorte();

        var resultado = pipeline.run(contexto(), 30);

        verify(ia, never()).requestOutput(any(), any());
        verify(ia, never()).requestOutput(any(), any(), anyInt());
        verify(reglas).generate(any(), anyList());
        assertEquals(GenerationSource.RULE_ENGINE, resultado.draft().source());
        assertEquals(1, resultado.attempts(),
                "solo el intento del respaldo: no se gastan los tres de la IA");
    }

    @Test
    @DisplayName("5 · y se VE: IA_NO_INVOCADA en WARN, con el motivo y el destino")
    void abiertoLoDiceEnWarn() {
        abrirElCorte();

        pipeline.run(contexto(), 30);

        var linea = avisos(Level.WARN).stream()
                .filter(m -> m.startsWith("IA_NO_INVOCADA"))
                .findFirst();
        assertTrue(linea.isPresent(),
                "sin esta linea, una generacion que se salta la IA es invisible");
        assertTrue(linea.get().contains("motivo=CIRCUIT_BREAKER_OPEN"));
        assertTrue(linea.get().contains("destino=RULE_ENGINE"));
        assertTrue(linea.get().contains("user=" + USUARIO),
                "tiene que poder atarse a la generacion concreta");
        assertTrue(linea.get().contains("fallos_consecutivos=3"),
                "tres, no cero: al abrir se reinicia el contador vivo");
        assertTrue(linea.get().contains("reintento_permitido_desde=2026-10-13T12:10:00Z"));
    }

    @Test
    @DisplayName("pasados los diez minutos se vuelve a llamar a la IA")
    void tras10MinutosSeReintenta() throws Exception {
        abrirElCorte();
        reloj.avanzarMinutos(11);

        when(ia.requestOutput(any(), any(), anyInt())).thenReturn("{}");
        when(ia.toDraft(anyString())).thenReturn(borrador(GenerationSource.AI));

        var resultado = pipeline.run(contexto(), 30);

        verify(ia).requestOutput(any(), any(), eq(1));
        assertEquals(GenerationSource.AI, resultado.draft().source());
        assertTrue(avisos(Level.WARN).stream().noneMatch(m -> m.startsWith("IA_NO_INVOCADA")));
    }

    // -------------------------------------------- 6 · la politica no se movio

    @Test
    @DisplayName("6 · el numero de intento llega al generador, para atar los tiempos al plan")
    void elIntentoLlegaAlGenerador() {
        when(ia.requestOutput(any(), any(), anyInt())).thenReturn("{}");
        when(ia.toDraft(anyString())).thenReturn(borrador(GenerationSource.AI));
        // Dos rechazos de validacion y a la tercera pasa.
        doThrow(new InvalidPlanDraftException(List.of("V1: algo")))
                .doThrow(new InvalidPlanDraftException(List.of("V2: otra cosa")))
                .doNothing()
                .when(validador).validate(any(), any(), anyInt());

        var resultado = pipeline.run(contexto(), 30);

        var orden = inOrder(ia);
        orden.verify(ia).requestOutput(any(), any(), eq(1));
        orden.verify(ia).requestOutput(any(), any(), eq(2));
        orden.verify(ia).requestOutput(any(), any(), eq(3));
        assertEquals(3, resultado.attempts(), "la politica 19.4 sigue siendo de tres intentos");
    }

    @Test
    @DisplayName("6 · un borrador rechazado NO cuenta para el corte: es capacidad, no caida")
    void rechazoNoCuentaParaElCorte() {
        when(ia.requestOutput(any(), any(), anyInt())).thenReturn("{}");
        when(ia.toDraft(anyString())).thenReturn(borrador(GenerationSource.AI));
        doThrow(new InvalidPlanDraftException(List.of("V1: algo")))
                .when(validador).validate(any(), any(), anyInt());

        assertThrows(PlanGenerationFailedException.class, () -> pipeline.run(contexto(), 30));

        assertEquals(0, breaker.fallosSeguidos(),
                "tres rechazos de validacion seguidos y el corte sigue cerrado");
        assertTrue(breaker.disponible());
    }

    @Test
    @DisplayName("6 · una caida del proveedor rompe el bucle y no gasta los tres intentos")
    void caidaRompeElBucle() {
        when(ia.requestOutput(any(), any(), anyInt()))
                .thenThrow(new AiProviderUnavailableException("504"));

        var resultado = pipeline.run(contexto(), 30);

        verify(ia, times(1)).requestOutput(any(), any(), anyInt());
        assertEquals(2, resultado.attempts(), "un intento de IA mas el del respaldo");
        assertEquals(1, breaker.fallosSeguidos());
        assertEquals(GenerationSource.RULE_ENGINE, resultado.draft().source());
    }

    @Test
    @DisplayName("6 · el contador intermedio tambien se ve: 1 de 3, 2 de 3")
    void elContadorIntermedioSeVe() {
        when(ia.requestOutput(any(), any(), anyInt()))
                .thenThrow(new AiProviderUnavailableException("504"));

        pipeline.run(contexto(), 30);
        pipeline.run(contexto(), 30);

        // En la medicion del 13/10/2026 el contador quedo en 2 de 3 y no habia
        // forma de saberlo leyendo el log.
        var intermedios = avisos(Level.WARN).stream()
                .filter(m -> m.contains("de " + AirCircuitBreaker.UMBRAL + " seguidos"))
                .toList();
        assertEquals(2, intermedios.size());
        assertTrue(intermedios.get(0).contains("1 de 3"));
        assertTrue(intermedios.get(1).contains("2 de 3"));
    }

    @Test
    @DisplayName("6 · el respaldo recibe los motivos acumulados, como antes")
    void elRespaldoRecibeLosMotivos() {
        when(ia.requestOutput(any(), any(), anyInt())).thenReturn("{}");
        when(ia.toDraft(anyString())).thenReturn(borrador(GenerationSource.AI));
        doThrow(new InvalidPlanDraftException(List.of("V23: falta LOWER_HIP")))
                .doThrow(new InvalidPlanDraftException(List.of("V4: duracion")))
                .doThrow(new InvalidPlanDraftException(List.of("V15: grupos")))
                .doNothing()
                .when(validador).validate(any(), any(), anyInt());

        // Sin ArgumentCaptor a proposito: su firma forClass cambio entre versiones
        // de Mockito y no quiero que una prueba del cortacircuitos dependa de eso.
        var motivos = new java.util.concurrent.atomic.AtomicReference<List<String>>();
        when(reglas.generate(any(), anyList())).thenAnswer(inv -> {
            motivos.set(inv.getArgument(1));
            return borrador(GenerationSource.RULE_ENGINE);
        });

        pipeline.run(contexto(), 30);

        assertEquals(List.of("V23: falta LOWER_HIP", "V4: duracion", "V15: grupos"),
                motivos.get(), "en orden y sin repetir, igual que antes del parche");
    }

    @Test
    @DisplayName("6 · si el respaldo tampoco vale, la semana se queda sin plan")
    void respaldoQueTampocoVale() {
        when(ia.isEnabled()).thenReturn(false);
        doThrow(new InvalidPlanDraftException(List.of("V23: falta LOWER_HIP")))
                .when(validador).validate(any(), any(), anyInt());

        assertThrows(PlanGenerationFailedException.class, () -> pipeline.run(contexto(), 30));
        assertFalse(avisos(Level.ERROR).isEmpty());
    }

    @Test
    @DisplayName("6 · con la IA apagada no hay IA_NO_INVOCADA: no es lo mismo que un corte")
    void iaApagadaNoEsCorte() {
        when(ia.isEnabled()).thenReturn(false);

        pipeline.run(contexto(), 30);

        verify(ia, never()).requestOutput(any(), any(), anyInt());
        assertTrue(avisos(Level.WARN).stream().noneMatch(m -> m.startsWith("IA_NO_INVOCADA")),
                "apagada a proposito y cortada por fallos son dos estados distintos");
    }

    // ------------------------------------------------------------- utilidades

    private static PlanGenerationContext contexto() {
        return new PlanGenerationContext(USUARIO, (short) 1, LUNES, LUNES.plusDays(6),
                perfil(), PlanAdjustment.none(), null, List.of(ej(1L), ej(2L)),
                SafetyProfile.none(), (PrescriptionParams) null);
    }

    private static CandidateExercise ej(long id) {
        return new CandidateExercise(id, "ejercicio " + id, "chest", "body weight", 1,
                PrescriptionType.SETS_REPS, null, "HORIZONTAL_PUSH");
    }

    private static PlanningProfile perfil() {
        return new PlanningProfile(USUARIO, 30, "MALE", new BigDecimal("175.0"),
                new BigDecimal("72.0"), new BigDecimal("75.0"), "BEGINNER",
                "GAIN_MUSCLE", "Ganar masa muscular entrenando en casa", "Ninguna",
                "HOME", 2, List.of((short) 1, (short) 3), 45,
                List.of("body weight", "dumbbell", "band"), List.of(), SafetyProfile.Declaradas.sinLimitaciones());
    }

    private static PlanDraft borrador(GenerationSource origen) {
        var ejercicios = new ArrayList<PlanDraft.DraftExercise>();
        ejercicios.add(new PlanDraft.DraftExercise(1L, PrescriptionType.SETS_REPS,
                (short) 2, (short) 10, null, null, (short) 75, null));
        return new PlanDraft(origen, origen == GenerationSource.AI ? "gpt-5-nano" : null,
                "plan de prueba", null, "prueba",
                List.of(new PlanDraft.DraftWorkout(LUNES, WorkoutFocus.FULL_BODY,
                        "FULL_BODY - Sesion 1", 45, ejercicios)));
    }
}