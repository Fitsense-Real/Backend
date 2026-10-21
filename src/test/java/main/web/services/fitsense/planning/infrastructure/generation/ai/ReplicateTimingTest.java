package main.web.services.fitsense.planning.infrastructure.generation.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateTiming.Resultado;

import static org.junit.jupiter.api.Assertions.*;

/**
 * La linea REPLICATE_TIMING. Se comprueba el FORMATO porque es lo que se va a
 * leer y filtrar durante la serie, y porque hay dos reglas que no se pueden
 * romper sin invalidar la medicion:
 * <ul>
 *   <li>un id de prediccion que no se recibio sale como "desconocido" y NO se
 *       inventa;</li>
 *   <li>las duraciones nunca salen negativas, pase lo que pase con el reloj.</li>
 * </ul>
 */
class ReplicateTimingTest {

    private static ReplicateTiming timing(String id, long postMs, long pollingMs,
                                          int requests, Resultado resultado) {
        return new ReplicateTiming(7L, (short) 1, 1, id, postMs, pollingMs, requests, resultado);
    }

    @Test
    @DisplayName("el total es la suma de los dos tramos")
    void totalEsLaSuma() {
        assertEquals(210_000, timing("abc", 60_000, 150_000, 50, Resultado.TIMEOUT).totalMs(),
                "60 + 150 s es el presupuesto real que descubrimos el 13/10/2026");
    }

    @Test
    @DisplayName("formato completo, con claves estables para poder filtrarlo")
    void formatoCompleto() {
        assertEquals("REPLICATE_TIMING user=7 week=1 attempt=1 prediction_id=mc7xdrmr "
                        + "post_ms=61000 polling_ms=98000 polling_requests=33 "
                        + "total_ms=159000 resultado=SUCCEEDED",
                timing("mc7xdrmr", 61_000, 98_000, 33, Resultado.SUCCEEDED).describe());
    }

    @Test
    @DisplayName("sin id de prediccion dice 'desconocido' y no inventa nada")
    void sinIdNoInventa() {
        var sinId = timing(null, 1_200, 0, 0, Resultado.POST_ERROR);

        assertTrue(sinId.describe().contains("prediction_id=desconocido"));
        assertFalse(sinId.describe().contains("null"), "nunca se escribe 'null' en el log");
    }

    @Test
    @DisplayName("un id en blanco se trata igual que ausente")
    void idEnBlancoEsDesconocido() {
        assertTrue(timing("   ", 1_200, 0, 0, Resultado.POST_ERROR)
                .describe().contains("prediction_id=desconocido"));
    }

    @Test
    @DisplayName("sin usuario ni intento, 'desconocido' en lugar de un numero falso")
    void sinUsuarioNiIntento() {
        var huerfano = new ReplicateTiming(null, (short) 0, 0, "abc",
                100, 0, 0, Resultado.SUCCEEDED);

        assertTrue(huerfano.describe().contains("user=desconocido"));
        assertTrue(huerfano.describe().contains("attempt=desconocido"));
    }

    @Test
    @DisplayName("el POST que vuelve resuelto deja el tramo de sondeo en cero")
    void sinSondeo() {
        var directo = timing("mc7xdrmr", 42_500, 0, 0, Resultado.SUCCEEDED);

        assertTrue(directo.describe().contains("polling_ms=0"));
        assertTrue(directo.describe().contains("polling_requests=0"));
        assertEquals(42_500, directo.totalMs());
    }

    @Test
    @DisplayName("msDesde nunca devuelve negativo")
    void msDesdeNuncaNegativo() {
        assertEquals(0, ReplicateTiming.msDesde(1_000_000_000L, 999_000_000L),
                "un delta negativo se recorta a cero en lugar de publicarse");
        assertEquals(1_500, ReplicateTiming.msDesde(0L, 1_500_000_000L));
        assertEquals(0, ReplicateTiming.msDesde(0L, 999_999L), "menos de 1 ms es 0 ms");
    }

    @Test
    @DisplayName("los seis resultados posibles estan y se nombran igual en el log")
    void losSeisResultados() {
        for (var r : Resultado.values())
            assertTrue(timing("abc", 1, 1, 1, r).describe().endsWith("resultado=" + r.name()));

        assertEquals(6, Resultado.values().length,
                "SUCCEEDED, TIMEOUT, NOT_SUCCEEDED, POST_ERROR, POLL_ERROR e "
                        + "INTERRUPTED: si se anade uno, hay que decidir que significa "
                        + "para el cortacircuitos");
    }

    @Test
    @DisplayName("TIMEOUT es el UNICO que habla del tiempo de espera configurado")
    void soloTimeoutHablaDelLimite() {
        // Juan lo encontro el 13/10/2026 leyendo awaitCompletion: un GET fallido
        // y una interrupcion tambien dejan la prediccion en pending, y los tres
        // se escribian como TIMEOUT. Un fallo de red a los 3 s quedaba igual que
        // una espera de 150 s, y sobre eso no se puede decidir nada.
        assertNotEquals(Resultado.TIMEOUT, Resultado.POLL_ERROR);
        assertNotEquals(Resultado.TIMEOUT, Resultado.INTERRUPTED);

        var fallaElSondeo = timing("mc7xdrmr", 4, 3_012, 1, Resultado.POLL_ERROR);
        assertTrue(fallaElSondeo.describe().contains("resultado=POLL_ERROR"));
        assertTrue(fallaElSondeo.describe().contains("polling_requests=1"),
                "una sola consulta: por eso NO puede ser un limite de 150 s agotado");
    }

    @Test
    @DisplayName("INTERRUPTED existe porque la serie reinicia el backend entre perfiles")
    void interruptedNoEsTimeout() {
        var apagando = timing("mc7xdrmr", 4, 3_001, 1, Resultado.INTERRUPTED);

        assertTrue(apagando.describe().endsWith("resultado=INTERRUPTED"),
                "apagar la aplicacion a media generacion no es un fallo del proveedor");
    }

    @Test
    @DisplayName("no lleva prompt, ni token, ni dato del participante")
    void noLlevaDatosSensibles() {
        // El registro se compone solo de identificadores y numeros, a proposito.
        var campos = ReplicateTiming.class.getRecordComponents();
        assertEquals(8, campos.length);
        for (var campo : campos)
            assertFalse(campo.getName().toLowerCase().matches(".*(prompt|token|email|name).*"),
                    "campo inesperado en un registro que se publica: " + campo.getName());
    }
}