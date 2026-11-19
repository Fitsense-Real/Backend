package main.web.services.fitsense.planning.infrastructure.generation.ai;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import main.web.services.fitsense.planning.domain.exceptions.AiProviderUnavailableException;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * RENDIMIENTO (12/11/2026) · EL SONDEO: CUANTO SE TARDA EN ENTERARSE DE QUE LA PREDICCION TERMINO, Y EL PLAZO.
 * <p>
 * Reloj y pausas simulados; HTTP simulado ({@link MockRestServiceServer}): ninguna llamada a Replicate.
 * <p>
 * La simulacion usa los 56 {@code predict_time} REALES de las predicciones de generacion de GRA (gpt-5-mini, PR-1.7,
 * {@code PREDICCIONES_Y_COSTOS_REPLICATE.csv}) y el comportamiento REAL del POST medido en la campana de estres: con
 * {@code Prefer: wait} Replicate devolvio la prediccion en curso a los ~20,5 s en las 17 generaciones. Lo que se mide
 * es solo el retraso del sondeo (fin de la prediccion -> el backend lo sabe); no incluye la red.
 */
@DisplayName("Rendimiento · sondeo: retraso tras terminar la prediccion, pausa configurable y plazo del presupuesto")
class ReplicateSondeoRendimientoTest {

    private static final String URL = "https://api.replicate.test/v1/models/openai/gpt-5-structured/predictions";
    private static final String GET = "https://api.replicate.test/v1/predictions/p1";
    private static final String CANCEL = "https://api.replicate.test/v1/predictions/p1/cancel";

    /** predict_time (ms) de las 56 predicciones de generacion de GRA-R1 y GRA-R2. */
    static final long[] PREDICT_MS_GRA = {57881, 38559, 102419, 121223, 120376, 108841, 47475, 49942, 49694, 114835,
            111478, 115110, 70339, 109537, 59430, 74258, 60261, 64802, 139422, 56175, 59948, 57782, 81868, 143856,
            92312, 35651, 72869, 44015, 78697, 58752, 51842, 44364, 131898, 112983, 125607, 46430, 73189, 86647,
            123719, 74575, 144852, 50879, 71850, 97411, 55293, 51198, 57416, 52007, 129242, 106650, 60750, 64403,
            46535, 47606, 54958, 42832};
    /** Lo que Replicate retuvo el POST en la campana de estres (mediana de 17 generaciones). */
    static final long POST_RETENIDO_MS = 20_500;

    private static final class Reloj extends Clock {
        Instant ahora = Instant.parse("2026-11-12T10:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    private ListAppender<ILoggingEvent> logs;
    private ch.qos.logback.classic.Logger logger;

    @BeforeEach
    void capturar() {
        logger = ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger(ReplicateTrainingPlanGenerator.class);
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    void soltar() {
        logger.detachAppender(logs);
    }

    private static String prediccion(String status) {
        return "{\"id\":\"p1\",\"status\":\"" + status + "\",\"output\":"
                + ("succeeded".equals(status) ? "{\"json_output\":{\"ok\":1}}" : "null")
                + ",\"urls\":{\"get\":\"" + GET + "\",\"cancel\":\"" + CANCEL + "\"}}";
    }

    private static ReplicateProperties propiedades(int intervaloMs) {
        return new ReplicateProperties(true, "token-de-prueba", URL, "gpt-5-mini", "medium", 150, 60, 0, 0, 0,
                intervaloMs);
    }

    /** Resultado de una prediccion simulada: cuando lo supo el backend y cuantas consultas hizo. */
    record Simulada(long finPrediccionMs, long sabidoMs, int consultas) {
        long retrasoMs() { return sabidoMs - finPrediccionMs; }
    }

    /** Una prediccion que termina a los {@code predictMs}; el POST vuelve a los 20,5 s (o antes si ya termino). */
    private Simulada simular(long predictMs, int intervaloMs) {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var reloj = new Reloj();
        var inicio = reloj.ahora;
        var consultas = new AtomicInteger();
        server.expect(requestTo(URL)).andExpect(method(HttpMethod.POST)).andRespond(req -> {
            long retenido = Math.min(predictMs, POST_RETENIDO_MS);
            reloj.ahora = inicio.plusMillis(retenido);
            return withSuccess(prediccion(predictMs <= retenido ? "succeeded" : "processing"),
                    MediaType.APPLICATION_JSON).createResponse(req);
        });
        server.expect(ExpectedCount.manyTimes(), requestTo(GET)).andExpect(method(HttpMethod.GET)).andRespond(req -> {
            consultas.incrementAndGet();
            boolean termino = !reloj.ahora.isBefore(inicio.plusMillis(predictMs));
            return withSuccess(prediccion(termino ? "succeeded" : "processing"), MediaType.APPLICATION_JSON)
                    .createResponse(req);
        });
        var g = new ReplicateTrainingPlanGenerator(propiedades(intervaloMs), null, null, new JsonSupport(), builder)
                .conTiempoDePrueba(reloj, d -> reloj.ahora = reloj.ahora.plus(d));
        assertEquals("{\"ok\":1}", g.call("prompt", 1, 7L, (short) 1, 1));
        return new Simulada(predictMs, Duration.between(inicio, reloj.ahora).toMillis(), consultas.get());
    }

    private static double media(List<Simulada> s, java.util.function.ToLongFunction<Simulada> f) {
        return s.stream().mapToLong(f).average().orElse(0);
    }

    private static long percentil(List<Simulada> s, java.util.function.ToLongFunction<Simulada> f, double p) {
        var v = s.stream().mapToLong(f).sorted().toArray();
        return v[(int) Math.ceil(p * v.length) - 1];
    }

    @Test
    @DisplayName("56 predicciones reales de GRA: el retraso medio del sondeo baja de ~1,5 s (3 s) a ~0,5 s (1 s)")
    void retrasoConTiemposReales() {
        var antes = new ArrayList<Simulada>();
        var despues = new ArrayList<Simulada>();
        for (long t : PREDICT_MS_GRA) {
            antes.add(simular(t, 3000));
            despues.add(simular(t, 1000));
        }
        for (var par : List.of(List.of("3000 ms (antes)", antes), List.of("1000 ms (ahora)", despues))) {
            @SuppressWarnings("unchecked") var s = (List<Simulada>) par.get(1);
            System.out.printf("DIAG_SONDEO intervalo=%s n=%d retraso_medio_ms=%.0f retraso_p95_ms=%d retraso_max_ms=%d"
                            + " total_p50_ms=%d total_p95_ms=%d consultas_media=%.1f%n", par.get(0), s.size(),
                    media(s, Simulada::retrasoMs), percentil(s, Simulada::retrasoMs, 0.95),
                    s.stream().mapToLong(Simulada::retrasoMs).max().orElse(0),
                    percentil(s, Simulada::sabidoMs, 0.50), percentil(s, Simulada::sabidoMs, 0.95),
                    media(s, Simulada::consultas));
        }
        double ahorro = media(antes, Simulada::retrasoMs) - media(despues, Simulada::retrasoMs);
        assertTrue(antes.stream().allMatch(s -> s.retrasoMs() >= 0 && s.retrasoMs() < 3000));
        assertTrue(despues.stream().allMatch(s -> s.retrasoMs() >= 0 && s.retrasoMs() < 1000));
        assertTrue(ahorro > 700, "ahorro medio por prediccion: " + ahorro + " ms");
    }

    @Test
    @DisplayName("la pausa configurada llega al sondeo: 1 s por omision, 3 s si se configura asi")
    void pausaConfigurable() {
        for (int ms : new int[]{0, 3000}) {
            var builder = RestClient.builder();
            var server = MockRestServiceServer.bindTo(builder).build();
            server.expect(requestTo(URL)).andRespond(withSuccess(prediccion("processing"), MediaType.APPLICATION_JSON));
            server.expect(requestTo(GET)).andRespond(withSuccess(prediccion("succeeded"), MediaType.APPLICATION_JSON));
            var pausas = new ArrayList<Duration>();
            var g = new ReplicateTrainingPlanGenerator(propiedades(ms), null, null, new JsonSupport(), builder)
                    .conTiempoDePrueba(new Reloj(), pausas::add);
            g.call("prompt", 1, 7L, (short) 1, 1);
            assertEquals(List.of(Duration.ofMillis(ms == 0 ? 1000 : 3000)), pausas);
        }
        assertEquals(250, propiedades(10).pollIntervalMillis(), "por debajo de 250 ms se acota");
        assertEquals(5000, propiedades(60000).pollIntervalMillis(), "por encima de 5 s se acota");
    }

    @Test
    @DisplayName("Prefer: wait se acota al plazo que queda (y vuelve a 60 s sin plazo)")
    void preferAcotadoAlPlazo() {
        var reloj = new Reloj();
        for (var caso : List.of(new Object[]{null, "wait=60"}, new Object[]{25L, "wait=25"},
                new Object[]{-5L, "wait=1"}, new Object[]{300L, "wait=60"})) {
            var builder = RestClient.builder();
            var server = MockRestServiceServer.bindTo(builder).build();
            server.expect(requestTo(URL)).andExpect(header("Prefer", (String) caso[1]))
                    .andRespond(withSuccess(prediccion("succeeded"), MediaType.APPLICATION_JSON));
            var g = new ReplicateTrainingPlanGenerator(propiedades(1000), null, null, new JsonSupport(), builder)
                    .conTiempoDePrueba(reloj, d -> reloj.ahora = reloj.ahora.plus(d));
            var plazo = caso[0] == null ? null : reloj.ahora.plusSeconds((Long) caso[0]);
            PlazoDeGeneracion.durante(plazo, () -> g.call("prompt", 1, 7L, (short) 1, 1));
            server.verify();
        }
    }

    @Test
    @DisplayName("el plazo corta el sondeo antes de los 150 s, se cancela la prediccion (R-2) y queda registrado")
    void plazoCortaYCancela() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var reloj = new Reloj();
        server.expect(requestTo(URL)).andRespond(withSuccess(prediccion("processing"), MediaType.APPLICATION_JSON));
        server.expect(ExpectedCount.times(10), requestTo(GET))
                .andRespond(withSuccess(prediccion("processing"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(CANCEL)).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(prediccion("canceled"), MediaType.APPLICATION_JSON));
        var g = new ReplicateTrainingPlanGenerator(propiedades(1000), null, null, new JsonSupport(), builder)
                .conTiempoDePrueba(reloj, d -> reloj.ahora = reloj.ahora.plus(d));
        var inicio = reloj.ahora;

        assertThrows(AiProviderUnavailableException.class,
                () -> PlazoDeGeneracion.durante(inicio.plusSeconds(10), () -> g.call("prompt", 1, 7L, (short) 1, 1)));

        server.verify();
        assertEquals(10, Duration.between(inicio, reloj.ahora).toSeconds(), "10 s de plazo, no 150 s de sondeo");
        var mensajes = logs.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertTrue(mensajes.stream().anyMatch(m -> m.startsWith("REPLICATE_PRESUPUESTO")), mensajes.toString());
        assertTrue(mensajes.stream().anyMatch(m -> m.startsWith("REPLICATE_CANCEL") && m.contains("resultado=CANCELADA")),
                mensajes.toString());
        assertNull(PlazoDeGeneracion.actual());
    }

    @Test
    @DisplayName("sin plazo, el sondeo sigue limitado a sus 150 s como antes")
    void sinPlazoComoAntes() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var reloj = new Reloj();
        server.expect(requestTo(URL)).andRespond(withSuccess(prediccion("processing"), MediaType.APPLICATION_JSON));
        server.expect(ExpectedCount.times(150), requestTo(GET))
                .andRespond(withSuccess(prediccion("processing"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(CANCEL)).andRespond(withSuccess(prediccion("canceled"), MediaType.APPLICATION_JSON));
        var g = new ReplicateTrainingPlanGenerator(propiedades(1000), null, null, new JsonSupport(), builder)
                .conTiempoDePrueba(reloj, d -> reloj.ahora = reloj.ahora.plus(d));
        var inicio = reloj.ahora;
        assertThrows(AiProviderUnavailableException.class, () -> g.call("prompt", 1, 7L, (short) 1, 1));
        server.verify();
        assertEquals(150, Duration.between(inicio, reloj.ahora).toSeconds());
        assertTrue(logs.list.stream().map(ILoggingEvent::getFormattedMessage)
                .noneMatch(m -> m.startsWith("REPLICATE_PRESUPUESTO")));
    }

    @Test
    @DisplayName("la espera maxima por llamada se documenta por intento, con y sin la cancelacion")
    void esperaMaximaPorIntento() {
        var p = propiedades(0);
        // 10 + 90 + 150 + 1 (pausa) + 10 + 30
        assertEquals(291, p.esperaMaximaSegundos());
        assertEquals(331, p.esperaMaximaConCancelacionSegundos(), "+ la cancelacion de R-2 (10 + 30)");
        assertEquals(293, propiedades(3000).esperaMaximaSegundos(), "con la pausa anterior de 3 s, los 293 de R-3");
        assertTrue(Arrays.stream(PREDICT_MS_GRA).allMatch(t -> t < 150_000), "ninguna prediccion real supero 150 s");
    }
}
