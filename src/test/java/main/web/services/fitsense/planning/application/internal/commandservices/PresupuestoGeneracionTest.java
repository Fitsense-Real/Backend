package main.web.services.fitsense.planning.application.internal.commandservices;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.domain.exceptions.AiProviderUnavailableException;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.PlanDraftNormalizer;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.TrainingPlanGenerator;
import main.web.services.fitsense.planning.infrastructure.generation.ai.AirCircuitBreaker;
import main.web.services.fitsense.planning.infrastructure.generation.ai.PlazoDeGeneracion;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateTrainingPlanGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RENDIMIENTO (12/11/2026) · PRESUPUESTO TOTAL DE LA FASE DE IA, con reloj falso: ninguna prueba espera de verdad.
 * <p>
 * La IA es un doble que hace avanzar el reloj lo que tardaria la prediccion y rechaza la propuesta (o falla). Lo que
 * se comprueba: cuantos intentos se empiezan, que plazo ve cada llamada, que el respaldo publica siempre y que agotar
 * el presupuesto no cuenta como fallo del proveedor.
 */
@DisplayName("Rendimiento · presupuesto total de la generacion: intentos acotados, respaldo intacto")
class PresupuestoGeneracionTest {

    private static final long USUARIO = 9L;
    private static final LocalDate LUNES = LocalDate.of(2026, 10, 12);

    private static final class Reloj extends Clock {
        Instant ahora = Instant.parse("2026-11-12T10:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    private ReplicateTrainingPlanGenerator ia;
    private TrainingPlanGenerator reglas;
    private PlanDraftValidator validador;
    private Reloj reloj;
    private AirCircuitBreaker breaker;
    private final List<Instant> plazosVistos = new ArrayList<>();
    private final List<Instant> inicios = new ArrayList<>();
    private ListAppender<ILoggingEvent> logs;
    private ch.qos.logback.classic.Logger logger;

    @BeforeEach
    void preparar() {
        ia = mock(ReplicateTrainingPlanGenerator.class);
        reglas = mock(TrainingPlanGenerator.class);
        validador = mock(PlanDraftValidator.class);
        reloj = new Reloj();
        breaker = new AirCircuitBreaker();
        when(ia.isEnabled()).thenReturn(true);
        when(reglas.generate(any(), anyList())).thenReturn(borrador());
        logger = ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger(PlanGenerationPipeline.class);
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    void soltar() {
        logger.detachAppender(logs);
    }

    private PlanGenerationPipeline pipeline(int presupuesto, int minimo) {
        return new PlanGenerationPipeline(ia, reglas, validador, mock(PlanDraftNormalizer.class), breaker, reloj,
                presupuesto, minimo, true);
    }

    /** Cada prediccion tarda {@code segundos} y su propuesta se rechaza. */
    private void iaQueTardaYSeRechaza(long... segundos) {
        var i = new int[]{0};
        when(ia.requestOutput(any(), any(), anyInt())).thenAnswer(inv -> {
            inicios.add(reloj.ahora);
            plazosVistos.add(PlazoDeGeneracion.actual());
            reloj.ahora = reloj.ahora.plusSeconds(segundos[Math.min(i[0]++, segundos.length - 1)]);
            throw new InvalidPlanDraftException(List.of("V15: propuesta rechazada"));
        });
    }

    private List<String> lineas(String prefijo) {
        return logs.list.stream().map(ILoggingEvent::getFormattedMessage).filter(m -> m.startsWith(prefijo)).toList();
    }

    @Test
    @DisplayName("predicciones de 100 s: caben los tres intentos (como antes) y cada llamada ve el mismo plazo")
    void tresIntentosQueCaben() {
        iaQueTardaYSeRechaza(100);
        var inicio = reloj.ahora;

        var r = pipeline(360, 40).run(contexto(), 30);

        verify(ia, times(3)).requestOutput(any(), any(), anyInt());
        assertEquals(4, r.attempts(), "3 de la IA + el respaldo");
        assertEquals(GenerationSource.RULE_ENGINE, r.draft().source());
        assertTrue(plazosVistos.stream().allMatch(p -> p.equals(inicio.plusSeconds(360))), plazosVistos.toString());
        assertNull(PlazoDeGeneracion.actual(), "el plazo no se queda en el hilo");
    }

    @Test
    @DisplayName("predicciones de 170 s: el tercer intento ya no se empieza (quedarian 20 s); antes se esperaban 510 s")
    void tercerIntentoSinTiempo() {
        iaQueTardaYSeRechaza(170);
        var inicio = reloj.ahora;

        var r = pipeline(360, 40).run(contexto(), 30);

        verify(ia, times(2)).requestOutput(any(), any(), anyInt());
        assertEquals(3, r.attempts(), "2 de la IA + el respaldo");
        assertEquals(GenerationSource.RULE_ENGINE, r.draft().source(), "el respaldo publica igual");
        assertEquals(340, Duration.between(inicio, reloj.ahora).toSeconds(), "espera total 340 s, no 510 s");
        var aviso = lineas("IA_PRESUPUESTO_AGOTADO");
        assertEquals(1, aviso.size());
        assertTrue(aviso.get(0).contains("intento=3") && aviso.get(0).contains("quedan_s=20"), aviso.get(0));
        assertTrue(lineas("GENERACION_TIEMPOS").get(0).contains("motivo_respaldo=PRESUPUESTO"));
    }

    @Test
    @DisplayName("comparacion: con el presupuesto maximo (30 min) el mismo caso hace los tres intentos")
    void sinPresupuestoEfectivoComoAntes() {
        iaQueTardaYSeRechaza(170);
        var r = pipeline(1800, 40).run(contexto(), 30);
        verify(ia, times(3)).requestOutput(any(), any(), anyInt());
        assertEquals(4, r.attempts());
    }

    @Test
    @DisplayName("la prediccion que se corta por presupuesto no cuenta como fallo del proveedor; una caida real si")
    void presupuestoNoAbreElCorte() {
        when(ia.requestOutput(any(), any(), anyInt())).thenAnswer(inv -> {
            reloj.ahora = reloj.ahora.plusSeconds(400);   // el sondeo agoto el plazo y se cancelo (R-2)
            throw new AiProviderUnavailableException("TIMEOUT");
        });
        pipeline(360, 40).run(contexto(), 30);
        assertEquals(0, breaker.fallosSeguidos(), "agotar el presupuesto no es una caida del proveedor");
        assertTrue(lineas("GENERACION_TIEMPOS").get(0).contains("motivo_respaldo=PRESUPUESTO"));

        logs.list.clear();
        doAnswer(inv -> {
            reloj.ahora = reloj.ahora.plusSeconds(5);
            throw new AiProviderUnavailableException("503");
        }).when(ia).requestOutput(any(), any(), anyInt());
        pipeline(360, 40).run(contexto(), 30);
        assertEquals(1, breaker.fallosSeguidos(), "una caida real sigue contando");
        assertTrue(lineas("GENERACION_TIEMPOS").get(0).contains("motivo_respaldo=PROVEEDOR"));
    }

    @Test
    @DisplayName("respuesta valida al primer intento: un intento, origen IA, y la linea de tiempos lo dice")
    void validaALaPrimera() {
        when(ia.requestOutput(any(), any(), anyInt())).thenAnswer(inv -> {
            reloj.ahora = reloj.ahora.plusSeconds(60);
            return "{}";
        });
        when(ia.toDraft(anyString())).thenReturn(borrador(GenerationSource.AI));
        var normalizador = mock(PlanDraftNormalizer.class);
        when(normalizador.normalize(any(), any())).thenAnswer(inv -> new PlanDraftNormalizer.Result(inv.getArgument(0), List.of()));
        var p = new PlanGenerationPipeline(ia, reglas, validador, normalizador, breaker, reloj, 360, 40, true);

        var r = p.run(contexto(), 30);

        assertEquals(1, r.attempts());
        assertEquals(GenerationSource.AI, r.draft().source());
        var linea = lineas("GENERACION_TIEMPOS").get(0);
        assertTrue(linea.contains("origen=IA") && linea.contains("intentos_ia=1") && linea.contains("motivo_respaldo=-"),
                linea);
    }

    @Test
    @DisplayName("valores fuera de rango se acotan: 60 s - 30 min; el minimo por intento, 10-150 s y <= total")
    void acotado() {
        assertEquals(60, pipeline(5, 40).presupuesto().toSeconds());
        assertEquals(1800, pipeline(99999, 40).presupuesto().toSeconds());
        assertEquals(360, pipeline(0, 0).presupuesto().toSeconds());
        assertEquals(40, pipeline(0, 0).minimoPorIntento().toSeconds());
        assertEquals(10, pipeline(360, 1).minimoPorIntento().toSeconds());
        assertEquals(60, pipeline(60, 150).minimoPorIntento().toSeconds());
    }

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
                "GAIN_MUSCLE", "Ganar masa", "Ninguna",
                "HOME", 2, List.of((short) 1, (short) 3), 45,
                List.of("body weight", "dumbbell", "band"), List.of(), SafetyProfile.Declaradas.sinLimitaciones());
    }

    private static PlanDraft borrador() {
        return borrador(GenerationSource.RULE_ENGINE);
    }

    private static PlanDraft borrador(GenerationSource origen) {
        var ejercicios = new ArrayList<PlanDraft.DraftExercise>();
        ejercicios.add(new PlanDraft.DraftExercise(1L, PrescriptionType.SETS_REPS,
                (short) 2, (short) 10, null, null, (short) 75, null));
        return new PlanDraft(origen, null, "plan de prueba", null, "prueba",
                List.of(new PlanDraft.DraftWorkout(LUNES, WorkoutFocus.FULL_BODY, "FULL_BODY - Sesion 1", 45,
                        ejercicios)));
    }
}
