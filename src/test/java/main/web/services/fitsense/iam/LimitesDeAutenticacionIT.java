package main.web.services.fitsense.iam;

import com.fasterxml.jackson.databind.ObjectMapper;
import main.web.services.fitsense.iam.application.internal.commandservices.LimiteDeAutenticacion;
import main.web.services.fitsense.shared.infrastructure.ratelimit.LimitePorIp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * B1.2 · P0-02: limites de login, registro y aceptacion de consentimientos, con cupos bajos para probarlos.
 * Las IP distintas se simulan con {@code remoteAddr} (lo que Tomcat deja en {@code getRemoteAddr()} tras resolver el
 * proxy de confianza; eso lo prueba {@link ProxyDeConfianzaLimitesIT}). El reloj de los limites lo mueve la prueba.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.auth-rate-limit.login-per-ip=12",
        "fitsense.auth-rate-limit.register-per-ip=4",
        "fitsense.auth-rate-limit.login-failures-per-account=3",
        "fitsense.auth-rate-limit.window-minutes=15",
        "fitsense.consent.accept-per-ip=2"
})
@Import(LimitesDeAutenticacionIT.Reloj.class)
@DisplayName("P0-02 · limites de login, registro y consentimientos (429 con Retry-After, sin enumerar cuentas)")
class LimitesDeAutenticacionIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    /** Reloj que avanza solo cuando la prueba lo dice (los limites lo reciben como {@code Optional<Clock>}). */
    static final class RelojMovil extends Clock {
        volatile Instant ahora = Instant.now();

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
        void avanzar(Duration d) { ahora = ahora.plus(d); }
    }

    @TestConfiguration
    static class Reloj {
        @Bean
        RelojMovil relojMovil() {
            return new RelojMovil();
        }
    }

    static final ObjectMapper JSON = new ObjectMapper();
    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final AtomicInteger IP = new AtomicInteger(1);
    static final String CLAVE = "Clave-de-prueba-123";

    @Autowired WebApplicationContext contexto;
    @Autowired RelojMovil reloj;
    @Autowired LimitePorIp limitePorIp;
    MockMvc http;

    @BeforeEach
    void http() {
        http = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    /** Una IP nueva para cada prueba: los cupos por IP no se pisan entre pruebas. */
    static String ipNueva() {
        int n = IP.incrementAndGet();
        return "10.20." + (n / 250) + "." + (n % 250 + 1);
    }

    static RequestPostProcessor desde(String ip) {
        return r -> {
            r.setRemoteAddr(ip);
            return r;
        };
    }

    ResultActions registrar(String email, String ip) throws Exception {
        return http.perform(post("/api/v1/auth/register").with(desde(ip)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s","firstName":"Sintetico","timezone":"America/Lima",
                         "acceptsStudyParticipation":false}""".formatted(email, CLAVE)));
    }

    ResultActions login(String email, String clave, String ip) throws Exception {
        return http.perform(post("/api/v1/auth/login").with(desde(ip)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, clave)));
    }

    String cuenta(String ip) throws Exception {
        String email = "limite-" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        registrar(email, ip).andExpect(status().isCreated());
        return email;
    }

    @Test
    @DisplayName("contrasena incorrecta repetida: al cuarto intento 429 con Retry-After, aunque la contrasena sea buena")
    void fallosPorCuenta() throws Exception {
        String ip = ipNueva();
        String email = cuenta(ip);
        for (int i = 0; i < 3; i++) login(email, "otra-clave-999", ip).andExpect(status().isUnauthorized());
        login(email, CLAVE, ipNueva()).andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.details[0]").value("DEMASIADAS_SOLICITUDES"))
                .andExpect(jsonPath("$.message").value(LimiteDeAutenticacion.MENSAJE_CUENTA));
        // Otra cuenta no se ve afectada.
        login(cuenta(ip), CLAVE, ip).andExpect(status().isOk());
    }

    @Test
    @DisplayName("sin enumeracion: un correo inexistente recibe exactamente las mismas respuestas que uno existente")
    void sinEnumeracion() throws Exception {
        String ip = ipNueva();
        String existe = cuenta(ip);
        String noExiste = "nadie-" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        var mensajes = new java.util.ArrayList<String>();
        for (var email : new String[]{existe, noExiste}) {
            for (int i = 0; i < 3; i++) {
                var cuerpo = login(email, "otra-clave-999", ip).andExpect(status().isUnauthorized()).andReturn()
                        .getResponse().getContentAsString();
                mensajes.add(JSON.readTree(cuerpo).get("message").asText());
            }
            var bloqueo = login(email, CLAVE, ip).andExpect(status().isTooManyRequests())
                    .andExpect(header().exists("Retry-After")).andReturn().getResponse().getContentAsString();
            mensajes.add(JSON.readTree(bloqueo).get("message").asText());
            assertFalse(bloqueo.contains(email), "la respuesta no repite el correo");
        }
        assertEquals(mensajes.subList(0, 4), mensajes.subList(4, 8),
                "mismos mensajes y mismo bloqueo, exista o no la cuenta");
        assertEquals(LimiteDeAutenticacion.MENSAJE_CUENTA, mensajes.get(3));
    }

    @Test
    @DisplayName("al pasar la ventana se vuelve a poder iniciar sesion; un login correcto vacia el contador")
    void ventana() throws Exception {
        String ip = ipNueva();
        String email = cuenta(ip);
        login(email, "otra-clave-999", ip).andExpect(status().isUnauthorized());
        login(email, "otra-clave-999", ip).andExpect(status().isUnauthorized());
        login(email, CLAVE, ip).andExpect(status().isOk());
        // El contador volvio a cero: otros dos fallos no bloquean.
        login(email, "otra-clave-999", ip).andExpect(status().isUnauthorized());
        login(email, "otra-clave-999", ip).andExpect(status().isUnauthorized());
        login(email, CLAVE, ip).andExpect(status().isOk());

        for (int i = 0; i < 3; i++) login(email, "otra-clave-999", ip).andExpect(status().isUnauthorized());
        login(email, CLAVE, ip).andExpect(status().isTooManyRequests());
        reloj.avanzar(Duration.ofMinutes(16));
        login(email, CLAVE, ip).andExpect(status().isOk());
    }

    @Test
    @DisplayName("registro masivo: con 4 altas por IP la quinta da 429; desde otra IP se puede registrar")
    void registroMasivo() throws Exception {
        String ip = ipNueva();
        for (int i = 0; i < 4; i++) cuenta(ip);
        registrar("masivo-" + SECUENCIA.incrementAndGet() + "@pruebas.invalid", ip)
                .andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
        cuenta(ipNueva());
    }

    @Test
    @DisplayName("dos IP: el cupo de login de una no afecta a la otra")
    void dosIp() throws Exception {
        String a = ipNueva();
        String b = ipNueva();
        String email = cuenta(ipNueva());
        for (int i = 0; i < 12; i++) login(email, CLAVE, a).andExpect(status().isOk());
        login(email, CLAVE, a).andExpect(status().isTooManyRequests());
        login(email, CLAVE, b).andExpect(status().isOk());
    }

    @Test
    @DisplayName("aceptar consentimientos: cupo por IP (429 al superarlo, antes de tocar la base)")
    void consentimientos() throws Exception {
        String ip = ipNueva();
        String email = cuenta(ip);
        String token = JSON.readTree(login(email, CLAVE, ip).andReturn().getResponse().getContentAsString())
                .get("token").asText();
        for (int i = 0; i < 2; i++)
            http.perform(post("/api/v1/users/me/consents").with(desde(ip)).header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"type\":\"TERMS_OF_SERVICE\",\"documentVersion\":\"v1\"}"))
                    .andExpect(status().isUnprocessableEntity());   // sin documento aprobado configurado
        http.perform(post("/api/v1/users/me/consents").with(desde(ip)).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"TERMS_OF_SERVICE\",\"documentVersion\":\"v1\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("reinicio: los limites viven en memoria; una instancia nueva (otro arranque) empieza sin contadores")
    void reinicio() {
        var antes = new LimiteDeAutenticacion(limitePorIp, 100, 100, 1, 15, Optional.of(reloj));
        antes.falloDeLogin("reinicio@pruebas.invalid");
        var peticion = new org.springframework.mock.web.MockHttpServletRequest();
        peticion.setRemoteAddr(ipNueva());
        assertThrows(LimitePorIp.DemasiadasSolicitudesException.class,
                () -> antes.antesDelLogin("reinicio@pruebas.invalid", peticion));
        var despues = new LimiteDeAutenticacion(new LimitePorIp(100, 15, Optional.of(reloj)), 100, 100, 1, 15,
                Optional.of(reloj));
        assertDoesNotThrow(() -> despues.antesDelLogin("reinicio@pruebas.invalid", peticion),
                "aceptado y documentado (S-4): una sola instancia; un reinicio vacia los contadores");
    }
}
