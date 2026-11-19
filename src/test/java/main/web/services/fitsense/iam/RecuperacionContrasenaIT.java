package main.web.services.fitsense.iam;

import com.fasterxml.jackson.databind.ObjectMapper;
import main.web.services.fitsense.iam.application.internal.outboundservices.notification.PasswordResetNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Recuperacion de contrasena de punta a punta (cierre definitivo, 2.2), con un notificador de PRUEBA que captura el
 * codigo en memoria: el flujo real no tiene proveedor de correo y no envia nada. PostgreSQL efimero.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.password-reset.ttl-minutes=30",
        "fitsense.password-reset.max-requests-per-hour=3"
})
@Import(RecuperacionContrasenaIT.Buzon.class)
@DisplayName("Recuperacion de contrasena: token de un solo uso, caducidad, limite y sin enumeracion")
class RecuperacionContrasenaIT {

    static final Map<String, List<String>> ENVIADOS = new ConcurrentHashMap<>();

    @TestConfiguration
    static class Buzon {
        @Bean
        @Primary
        PasswordResetNotifier buzonDePrueba() {
            return new PasswordResetNotifier() {
                @Override
                public void enviar(String email, String token, OffsetDateTime caduca) {
                    ENVIADOS.computeIfAbsent(email, k -> new CopyOnWriteArrayList<>()).add(token);
                }

                @Override
                public boolean entregaReal() {
                    return false;
                }
            };
        }
    }

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final ObjectMapper JSON = new ObjectMapper();
    static final String CLAVE = "clave-original-123";

    @Autowired WebApplicationContext contexto;
    @Autowired JdbcTemplate jdbc;
    MockMvc http;

    @BeforeEach
    void http() {
        http = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("misma respuesta exista o no el correo; solo se guarda el hash del codigo")
    void sinEnumeracion() throws Exception {
        var email = registrar();
        var existe = solicitar(email).andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        var noExiste = solicitar("nadie-" + SECUENCIA.incrementAndGet() + "@pruebas.invalid")
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        assertEquals(existe, noExiste, "la respuesta no revela si la cuenta existe");

        var token = ultimo(email);
        var hashes = jdbc.queryForList("SELECT token_hash FROM password_reset_tokens t JOIN users u USING (user_id)"
                + " WHERE u.email = ?", String.class, email);
        assertEquals(1, hashes.size());
        assertEquals(64, hashes.get(0).length());
        assertNotEquals(token, hashes.get(0), "el codigo no se guarda en claro");
        assertFalse(jdbc.queryForObject("SELECT count(*) FROM password_reset_tokens WHERE token_hash = ?",
                Integer.class, token) > 0);
        assertTrue(token.length() >= 40, "codigo de 256 bits");
    }

    @Test
    @DisplayName("confirmar cambia la contrasena; el codigo es de un solo uso e invalida los demas")
    void unSoloUso() throws Exception {
        var email = registrar();
        solicitar(email).andExpect(status().isAccepted());
        solicitar(email).andExpect(status().isAccepted());
        var primero = ENVIADOS.get(email).get(0);
        var segundo = ENVIADOS.get(email).get(1);

        confirmar(segundo, "clave-nueva-456").andExpect(status().isNoContent());
        login(email, "clave-nueva-456").andExpect(status().isOk());
        assertFalse(entra(email, CLAVE), "la anterior ya no vale");

        confirmar(segundo, "otra-clave-789").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El codigo de recuperacion no es valido o ha caducado."));
        confirmar(primero, "otra-clave-789").andExpect(status().isBadRequest());
        login(email, "clave-nueva-456").andExpect(status().isOk());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM password_reset_tokens t JOIN users u USING"
                + " (user_id) WHERE u.email = ? AND used_at IS NULL", Integer.class, email), "ninguno pendiente");
    }

    @Test
    @DisplayName("codigo caducado o inventado: 400 con el mismo mensaje; contrasena debil: 400")
    void caducadoEInventado() throws Exception {
        var email = registrar();
        solicitar(email).andExpect(status().isAccepted());
        var token = ultimo(email);
        jdbc.update("UPDATE password_reset_tokens SET created_at = now() - interval '2 hours',"
                + " expires_at = now() - interval '1 minute' WHERE user_id = (SELECT user_id FROM users WHERE email = ?)",
                email);
        confirmar(token, "clave-nueva-456").andExpect(status().isBadRequest());
        confirmar("inventado-" + SECUENCIA.incrementAndGet(), "clave-nueva-456").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El codigo de recuperacion no es valido o ha caducado."));
        confirmar(token, "corta").andExpect(status().isBadRequest());
        login(email, CLAVE).andExpect(status().isOk());
    }

    @Test
    @DisplayName("limite: como mucho 3 solicitudes por hora y cuenta; la respuesta no cambia")
    void limite() throws Exception {
        var email = registrar();
        for (int i = 0; i < 5; i++) solicitar(email).andExpect(status().isAccepted());
        assertEquals(3, ENVIADOS.get(email).size());
        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM password_reset_tokens t JOIN users u USING"
                + " (user_id) WHERE u.email = ?", Integer.class, email));
    }

    @Test
    @DisplayName("cuenta no activa: no se emite codigo")
    void cuentaInactiva() throws Exception {
        var email = registrar();
        jdbc.update("UPDATE users SET status = 'INACTIVE' WHERE email = ?", email);
        solicitar(email).andExpect(status().isAccepted());
        assertNull(ENVIADOS.get(email));
    }

    // ----------------------------------------------------------------- apoyo

    private String registrar() throws Exception {
        String email = "recupera" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        http.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"%s","password":"%s","firstName":"Prueba","lastName":"Recupera",
                 "timezone":"America/Lima","acceptsStudyParticipation":true}
                """.formatted(email, CLAVE))).andExpect(status().isCreated());
        return email;
    }

    private ResultActions solicitar(String email) throws Exception {
        return http.perform(post("/api/v1/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("email", email))));
    }

    private ResultActions confirmar(String token, String clave) throws Exception {
        return http.perform(post("/api/v1/auth/password-reset/confirm").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("token", token, "newPassword", clave))));
    }

    private ResultActions login(String email, String clave) throws Exception {
        return http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("email", email, "password", clave))));
    }

    /**
     * Si el login entra. Sin el PR de errores de la API, un login fallido sale como excepcion (500); con el, 401.
     * Esta prueba solo exige que la contrasena no sirva.
     */
    private boolean entra(String email, String clave) {
        try {
            return login(email, clave).andReturn().getResponse().getStatus() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private String ultimo(String email) {
        var lista = ENVIADOS.get(email);
        assertNotNull(lista, "no se envio codigo a " + email);
        return lista.get(lista.size() - 1);
    }
}
