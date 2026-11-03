package main.web.services.fitsense.planning.infrastructure.generation.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El cortacircuitos. La politica no cambia en este parche, asi que estas
 * pruebas son sobre todo un CERROJO: fijan por escrito lo que ya hacia cuando
 * estaba inline en el pipeline, para que extraerlo no lo haya movido.
 * <p>
 * Lo nuevo que si se comprueba es {@link AirCircuitBreaker#describe()}, que es
 * lo que acaba en la linea IA_NO_INVOCADA, y el campo {@code fallosQueAbrieron}
 * que existe porque al abrir se reinicia el contador.
 * <p>
 * El reloj es falso: la ventana de diez minutos se comprueba sin esperar diez
 * minutos.
 */
class AirCircuitBreakerTest {

    /** Reloj que solo avanza cuando se le dice. */
    private static final class RelojFalso implements java.util.function.Supplier<Instant> {
        private Instant ahora = Instant.parse("2026-10-13T12:00:00Z");
        @Override public Instant get() { return ahora; }
        void avanzarMinutos(long minutos) { ahora = ahora.plusSeconds(minutos * 60); }
    }

    @Test
    @DisplayName("recien creado, la IA esta disponible")
    void disponibleAlPrincipio() {
        var breaker = new AirCircuitBreaker(new RelojFalso());

        assertTrue(breaker.disponible());
        assertFalse(breaker.abierto());
        assertEquals(0, breaker.fallosSeguidos());
        assertEquals(0, breaker.fallosQueAbrieron());
    }

    @Test
    @DisplayName("dos fallos seguidos NO abren el corte")
    void dosFallosNoAbren() {
        var breaker = new AirCircuitBreaker(new RelojFalso());

        assertFalse(breaker.registrarFalloDeProveedor());
        assertFalse(breaker.registrarFalloDeProveedor());

        assertTrue(breaker.disponible(), "con 2 de 3 todavia se intenta la IA");
        assertEquals(2, breaker.fallosSeguidos(),
                "es el estado exacto en que quedo la medicion del 13/10/2026");
    }

    @Test
    @DisplayName("el tercer fallo seguido abre el corte y lo dice")
    void tercerFalloAbre() {
        var breaker = new AirCircuitBreaker(new RelojFalso());

        assertFalse(breaker.registrarFalloDeProveedor());
        assertFalse(breaker.registrarFalloDeProveedor());
        assertTrue(breaker.registrarFalloDeProveedor(),
                "el metodo devuelve true solo en el fallo que abre");

        assertTrue(breaker.abierto());
        assertEquals(AirCircuitBreaker.UMBRAL, breaker.fallosQueAbrieron());
    }

    @Test
    @DisplayName("un exito reinicia el contador: tres fallos NO seguidos no abren")
    void exitoReinicia() {
        var breaker = new AirCircuitBreaker(new RelojFalso());

        breaker.registrarFalloDeProveedor();
        breaker.registrarFalloDeProveedor();
        breaker.registrarRespuestaDelProveedor();
        breaker.registrarFalloDeProveedor();

        assertEquals(1, breaker.fallosSeguidos());
        assertTrue(breaker.disponible(), "el umbral es de fallos SEGUIDOS");
    }

    @Test
    @DisplayName("un borrador rechazado por validacion cuenta como respuesta, no como caida")
    void borradorRechazadoNoEsCaida() {
        var breaker = new AirCircuitBreaker(new RelojFalso());

        breaker.registrarFalloDeProveedor();
        breaker.registrarFalloDeProveedor();
        // El modelo contesto, aunque el plan fuese invalido: es su capacidad,
        // no una caida del proveedor. Esta distincion es de la politica 19.4.
        breaker.registrarRespuestaDelProveedor();

        assertEquals(0, breaker.fallosSeguidos());
        assertTrue(breaker.disponible());
    }

    @Test
    @DisplayName("abierto, sigue abierto durante los diez minutos y se cierra despues")
    void ventanaDeDiezMinutos() {
        var reloj = new RelojFalso();
        var breaker = new AirCircuitBreaker(reloj);

        for (int i = 0; i < AirCircuitBreaker.UMBRAL; i++) breaker.registrarFalloDeProveedor();
        assertTrue(breaker.abierto());

        reloj.avanzarMinutos(9);
        assertTrue(breaker.abierto(), "a los 9 minutos todavia no se reintenta");

        reloj.avanzarMinutos(2);
        assertTrue(breaker.disponible(), "pasados los 10 minutos se vuelve a intentar");
    }

    @Test
    @DisplayName("describe() dice TRES fallos, no cero, justo cuando importa")
    void describeNoDiceCero() {
        var reloj = new RelojFalso();
        var breaker = new AirCircuitBreaker(reloj);

        for (int i = 0; i < AirCircuitBreaker.UMBRAL; i++) breaker.registrarFalloDeProveedor();

        // Al abrir, el contador vivo se reinicia a 0. Si el log leyese ese
        // contador diria "fallos_consecutivos=0" en el unico momento en que
        // hace falta saber que fueron tres. Por eso existe fallosQueAbrieron.
        assertEquals(0, breaker.fallosSeguidos());
        assertEquals("fallos_consecutivos=3 reintento_permitido_desde=2026-10-13T12:10:00Z",
                breaker.describe());
    }

    @Test
    @DisplayName("el estado vive en memoria: un cortacircuitos nuevo arranca limpio")
    void estadoEnMemoria() {
        var reloj = new RelojFalso();
        var abierto = new AirCircuitBreaker(reloj);
        for (int i = 0; i < AirCircuitBreaker.UMBRAL; i++) abierto.registrarFalloDeProveedor();
        assertTrue(abierto.abierto());

        // Equivale a reiniciar la aplicacion, que es como se empieza una
        // medicion desde un estado conocido.
        assertTrue(new AirCircuitBreaker(reloj).disponible());
    }
}
