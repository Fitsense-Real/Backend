package main.web.services.fitsense.iam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import main.web.services.fitsense.iam.application.internal.consent.ConsentimientoService;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.domain.exceptions.ParticipacionNoVigenteException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * C1 · OPCION A (decision de producto): Kora solo es para participantes activos y elegibles del estudio. Con la
 * configuracion por omision (registro definitivo apagado, IA apagada: motor de reglas) se comprueba que:
 * <ul>
 *   <li>el registro no inscribe a nadie que no lo pida;</li>
 *   <li>sin participar o tras retirarse no hay plan, ni por la ruta sincrona ni por la asincrona (mismo codigo);</li>
 *   <li>una operacion pendiente, una generacion en curso y el cierre semanal no publican para quien se retiro;</li>
 *   <li>la comprobacion al publicar bloquea la fila de la cuenta: una retirada simultanea espera.</li>
 * </ul>
 * Solo usuarios sinteticos en PostgreSQL de Testcontainers.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.plan-generation.worker.enabled=false",
        "fitsense.auth-rate-limit.register-per-ip=100"
})
@DisplayName("C1 · opcion A: solo participantes activos y elegibles reciben planes, por todas las rutas")
class OpcionAExclusivaIT extends ParticipacionBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static final ObjectMapper JSON = new ObjectMapper();

    @Autowired WebApplicationContext contexto;
    @Autowired ConsentimientoService consentimientos;
    @Autowired WeeklyTrainingPlanCommandService planes;
    @Autowired WeeklyCycleService ciclo;
    @Autowired PlatformTransactionManager transacciones;
    MockMvc http;

    @BeforeEach
    void http() {
        http = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    // ------------------------------------------------------------------ apoyo HTTP

    record Respuesta(int status, JsonNode cuerpo) {
        String codigo() {
            var d = cuerpo == null ? null : cuerpo.get("details");
            return d == null || d.isEmpty() ? null : d.get(0).asText();
        }
    }

    Respuesta llamar(org.springframework.test.web.servlet.RequestBuilder peticion) throws Exception {
        var r = http.perform(peticion).andReturn().getResponse();
        var texto = r.getContentAsString();
        return new Respuesta(r.getStatus(), texto.isBlank() ? null : JSON.readTree(texto));
    }

    Respuesta registrar(String email, String campos) throws Exception {
        return llamar(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"%s","password":"Clave-opcion-a-1","firstName":"Sintetico","timezone":"America/Lima"%s}"""
                .formatted(email, campos)));
    }

    /** Registro por HTTP, perfil por el servicio y JWT. */
    record Cuenta(long id, String jwt) {}

    Cuenta alta(boolean participa) throws Exception {
        String email = "opcion-a-" + UUID.randomUUID() + "@pruebas.invalid";
        assertEquals(201, registrar(email, ",\"acceptsStudyParticipation\":" + participa).status());
        long id = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfil(id);
        var login = llamar(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"Clave-opcion-a-1\"}"));
        return new Cuenta(id, login.cuerpo().get("token").asText());
    }

    Respuesta sincrona(Cuenta c) throws Exception {
        return llamar(post("/api/v1/users/me/plan/generate").header("Authorization", "Bearer " + c.jwt()));
    }

    Respuesta asincrona(Cuenta c) throws Exception {
        return llamar(post("/api/v1/users/me/plan/generation").header("Authorization", "Bearer " + c.jwt()));
    }

    void retirar(Cuenta c) throws Exception {
        assertEquals(200, llamar(post("/api/v1/users/me/withdraw").header("Authorization", "Bearer " + c.jwt()))
                .status());
    }

    int planes(long u) {
        return jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?", Integer.class, u);
    }

    // ------------------------------------------------------------------ pruebas

    @Test
    @DisplayName("el registro no inscribe a nadie: sin el campo, 400 y ninguna cuenta; con false, la cuenta no participa")
    void registroNoInscribe() throws Exception {
        // Contrato real: acceptsStudyParticipation es obligatorio en el JSON (booleano primitivo; Jackson rechaza que
        // falte). Nunca se interpreta su ausencia como una aceptacion.
        var sinCampo = registrar("sin-casilla@pruebas.invalid", "");
        assertEquals(400, sinCampo.status(), String.valueOf(sinCampo.cuerpo()));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM users WHERE email = 'sin-casilla@pruebas.invalid'",
                Integer.class));
        assertEquals(201, registrar("casilla-false@pruebas.invalid", ",\"acceptsStudyParticipation\":false").status());
        for (var e : java.util.List.of("casilla-false@pruebas.invalid")) {
            assertFalse(jdbc.queryForObject("SELECT is_study_participant FROM users WHERE email = ?", Boolean.class, e));
            assertNull(jdbc.queryForObject("SELECT participant_code FROM users WHERE email = ?", String.class, e));
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM user_consents c JOIN users u USING (user_id)"
                    + " WHERE u.email = ? AND c.consent_type = 'STUDY_PARTICIPATION'", Integer.class, e),
                    "ninguna aceptacion de investigacion por omision");
        }
    }

    @Test
    @DisplayName("sin participar: 422 PARTICIPACION_NO_VIGENTE por la ruta sincrona Y por la asincrona; ningun plan")
    void noParticipanteNiSincronaNiAsincrona() throws Exception {
        var c = alta(false);
        var s = sincrona(c);
        var a = asincrona(c);
        assertEquals(422, s.status(), String.valueOf(s.cuerpo()));
        assertEquals(422, a.status(), String.valueOf(a.cuerpo()));
        assertEquals(ParticipacionNoVigenteException.CODIGO, s.codigo());
        assertEquals(ParticipacionNoVigenteException.CODIGO, a.codigo());
        assertEquals(0, planes(c.id()));
        assertFalse(iam.fetchActiveParticipantIdsInTimezone(ZONA).contains(c.id()), "fuera del cierre semanal");
    }

    @Test
    @DisplayName("participante activo: plan por la ruta sincrona (201) y por la asincrona (202 y COMPLETED)")
    void participanteSiGenera() throws Exception {
        var sinc = alta(true);
        assertEquals(201, sincrona(sinc).status());
        assertEquals(1, planes(sinc.id()));

        var asinc = alta(true);
        var r = asincrona(asinc);
        assertEquals(202, r.status(), String.valueOf(r.cuerpo()));
        operaciones.claimNext();
        operaciones.execute(UUID.fromString(r.cuerpo().get("operationId").asText()));
        assertEquals(1, planes(asinc.id()));
        assertTrue(iam.fetchActiveParticipantIdsInTimezone(ZONA).containsAll(java.util.List.of(sinc.id(), asinc.id())));
    }

    @Test
    @DisplayName("retirado: ninguna rutina nueva por ninguna ruta (mismo codigo) y fuera del cierre semanal")
    void retiradoNoGenera() throws Exception {
        var c = alta(true);
        retirar(c);
        assertEquals(ParticipacionNoVigenteException.CODIGO, sincrona(c).codigo());
        assertEquals(ParticipacionNoVigenteException.CODIGO, asincrona(c).codigo());
        assertEquals(0, planes(c.id()));
        assertFalse(iam.fetchActiveParticipantIdsInTimezone(ZONA).contains(c.id()));
    }

    @Test
    @DisplayName("operacion asincrona PENDIENTE y retirada antes del worker: FAILED NO_GENERABLE y ningun plan")
    void pendienteNoPublicaTrasRetirarse() throws Exception {
        var c = alta(true);
        var r = asincrona(c);
        assertEquals(202, r.status());
        retirar(c);
        var id = UUID.fromString(r.cuerpo().get("operationId").asText());
        operaciones.claimNext();
        operaciones.execute(id);
        assertEquals("FAILED:NO_GENERABLE", jdbc.queryForObject("SELECT status || ':' || reason_code FROM"
                + " plan_generation_operations WHERE operation_id = ?", String.class, id));
        assertEquals(0, planes(c.id()));
    }

    @Test
    @DisplayName("retirada ENTRE la preparacion y la publicacion (la IA tardando): no se publica")
    void retiradaDuranteLaGeneracion() throws Exception {
        var c = alta(true);
        var comando = GenerateWeeklyPlanCommand.firstPlan(c.id(), planes.firstPlanWeekStart(c.id()));
        var preparada = planes.prepare(comando);       // fase 1: todavia participa
        var generada = planes.generate(preparada);     // fase 2: "la IA"
        retirar(c);                                    // se retira mientras tanto
        assertThrows(ParticipacionNoVigenteException.class, () -> planes.persist(preparada, generada));
        assertEquals(0, planes(c.id()), "la fase 3 vuelve a comprobar la participacion y no publica");
    }

    @Test
    @DisplayName("cierre semanal de alguien que se retiro: cierra y mide su ultima semana, pero no publica la nueva")
    void cicloNoPublicaParaRetirados() throws Exception {
        var c = alta(true);
        assertEquals(201, sincrona(c).status());
        var semana = jdbc.queryForObject("SELECT week_start_date FROM weekly_training_plans WHERE user_id = ?",
                LocalDate.class, c.id());
        retirar(c);
        var r = ciclo.ejecutar(c.id(), semana, semana.plusWeeks(1));
        assertTrue(r.planId().isEmpty());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ? AND"
                + " week_start_date = ?", Integer.class, c.id(), semana.plusWeeks(1)));
    }

    @Test
    @DisplayName("la comprobacion al publicar bloquea la cuenta: una retirada simultanea espera al commit")
    void retiradaSimultaneaEspera() throws Exception {
        var c = alta(true);
        var tx = new TransactionTemplate(transacciones);
        var retirada = new CompletableFuture<Void>();
        tx.executeWithoutResult(estado -> {
            assertTrue(iam.elegibleAlPublicar(c.id()));
            CompletableFuture.runAsync(() -> {
                try {
                    consentimientos.retirar(c.id(), ConsentimientoService.Tipo.STUDY_PARTICIPATION);
                    retirada.complete(null);
                } catch (RuntimeException e) {
                    retirada.completeExceptionally(e);
                }
            });
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            assertFalse(retirada.isDone(), "la retirada espera a que termine la publicacion en curso");
        });
        retirada.get(15, TimeUnit.SECONDS);
        assertFalse(iam.isEligibleForPlanGeneration(c.id()));
        tx.executeWithoutResult(estado -> assertFalse(iam.elegibleAlPublicar(c.id())));
    }

    @Test
    @DisplayName("la comprobacion al publicar exige la transaccion de la publicacion")
    void sinTransaccionNoSeUsa() throws Exception {
        var c = alta(true);
        assertThrows(IllegalTransactionStateException.class, () -> iam.elegibleAlPublicar(c.id()));
    }

    @Test
    @DisplayName("contrato: version sin participar, y version de un documento no publicado -> 422 con su codigo")
    void codigosSinDocumentos() throws Exception {
        var sinParticipar = registrar("c1@pruebas.invalid",
                ",\"acceptsStudyParticipation\":false,\"acceptedStudyConsentVersion\":\"MVP-1.0\"");
        assertEquals(422, sinParticipar.status());
        assertEquals("VERSION_SIN_PARTICIPACION", sinParticipar.codigo());

        var sinDocumento = registrar("c2@pruebas.invalid",
                ",\"acceptsStudyParticipation\":true,\"acceptedStudyConsentVersion\":\"MVP-1.0\"");
        assertEquals(422, sinDocumento.status());
        assertEquals("DOCUMENTO_NO_PUBLICADO", sinDocumento.codigo(),
                "MVP-1.0 no es un documento aprobado: no se puede aceptar como tal");

        var condiciones = registrar("c3@pruebas.invalid",
                ",\"acceptsStudyParticipation\":true,\"acceptedTermsVersion\":\"T-1.0\"");
        assertEquals("DOCUMENTO_NO_PUBLICADO", condiciones.codigo());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM users WHERE email LIKE 'c_@pruebas.invalid'",
                Integer.class), "ninguna cuenta a medias");
    }

    @Test
    @DisplayName("sin documento aprobado la casilla se guarda como CASILLA_SIN_DOCUMENTO_APROBADO: no es evidencia valida")
    void casillaSinDocumentoNoEsValida() throws Exception {
        var c = alta(true);
        assertEquals("MVP-1.0:CASILLA_SIN_DOCUMENTO_APROBADO", jdbc.queryForObject("SELECT document_version || ':' ||"
                + " affirmative_action FROM user_consents WHERE user_id = ? AND consent_type = 'STUDY_PARTICIPATION'",
                String.class, c.id()));
        assertFalse(consentimientos.consentimientoValido(c.id()));
        assertFalse(consentimientos.exigeConsentimientoValidoParaPlanes(),
                "con el registro definitivo apagado (pruebas) todavia no se exige; encenderlo es otra etapa");
    }

    // ------------------------------------------------------------------ revision final (orden de merge)

    /** Ningun mensaje de error lleva el id interno de la cuenta. */
    static void sinIdInterno(Respuesta r) {
        var texto = String.valueOf(r.cuerpo() == null ? "" : r.cuerpo().get("message"));
        assertFalse(texto.matches("(?is).*usuario\\s+\\d+.*"), texto);
    }

    @Test
    @DisplayName("perfil ausente: asincrona 422 PERFIL_INCOMPLETO, sincrona 404; ningun plan ni operacion")
    void perfilAusente() throws Exception {
        String email = "sin-perfil-" + UUID.randomUUID() + "@pruebas.invalid";
        assertEquals(201, registrar(email, ",\"acceptsStudyParticipation\":true").status());
        var jwt = llamar(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"Clave-opcion-a-1\"}")).cuerpo().get("token").asText();
        long id = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        var c = new Cuenta(id, jwt);
        var a = asincrona(c);
        assertEquals(422, a.status());
        assertEquals("PERFIL_INCOMPLETO", a.codigo());
        var s = sincrona(c);
        assertEquals(404, s.status());
        sinIdInterno(a);
        assertEquals(0, planes(id));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM plan_generation_operations WHERE user_id = ?",
                Integer.class, id));
    }

    @Test
    @DisplayName("cuenta no activa (INACTIVE) aunque participe: no elegible ni al pedir ni al publicar")
    void cuentaNoActivaNoElegible() throws Exception {
        var c = alta(true);
        jdbc.update("UPDATE users SET status = 'INACTIVE' WHERE user_id = ?", c.id());
        assertFalse(iam.isEligibleForPlanGeneration(c.id()));
        new TransactionTemplate(transacciones).executeWithoutResult(e -> assertFalse(iam.elegibleAlPublicar(c.id())));
        assertFalse(iam.fetchActiveParticipantIdsInTimezone(ZONA).contains(c.id()));
    }

    @Test
    @DisplayName("los mensajes de PARTICIPACION_NO_VIGENTE no llevan el id interno, por ninguna ruta")
    void mensajesSinIdInterno() throws Exception {
        var c = alta(false);
        var s = sincrona(c);
        var a = asincrona(c);
        assertEquals(ParticipacionNoVigenteException.CODIGO, s.codigo());
        sinIdInterno(s);
        sinIdInterno(a);
        var comando = GenerateWeeklyPlanCommand.firstPlan(c.id(), planes.firstPlanWeekStart(c.id()));
        var e = assertThrows(ParticipacionNoVigenteException.class, () -> planes.prepare(comando));
        assertFalse(e.getMessage().contains(String.valueOf(c.id())) && e.getMessage().matches("(?is).*usuario.*"));
    }

    @Test
    @DisplayName("reintentos de publicacion tras la retirada: siguen rechazados y no crean planes")
    void reintentosDePublicacion() throws Exception {
        var c = alta(true);
        var comando = GenerateWeeklyPlanCommand.firstPlan(c.id(), planes.firstPlanWeekStart(c.id()));
        var preparada = planes.prepare(comando);
        var generada = planes.generate(preparada);
        retirar(c);
        for (int i = 0; i < 3; i++)
            assertThrows(ParticipacionNoVigenteException.class, () -> planes.persist(preparada, generada));
        assertEquals(ParticipacionNoVigenteException.CODIGO, sincrona(c).codigo(), "y una peticion nueva tambien");
        assertEquals(0, planes(c.id()));
    }

    @Test
    @DisplayName("dos publicaciones concurrentes de un participante activo: una publica y la otra se rechaza")
    void dosPublicacionesConcurrentes() throws Exception {
        var c = alta(true);
        var comando = GenerateWeeklyPlanCommand.firstPlan(c.id(), planes.firstPlanWeekStart(c.id()));
        var p1 = planes.prepare(comando);
        var g1 = planes.generate(p1);
        var p2 = planes.prepare(comando);
        var g2 = planes.generate(p2);
        var salida = new java.util.concurrent.CyclicBarrier(2);
        java.util.function.Supplier<String> publicar1 = () -> intentar(salida, () -> planes.persist(p1, g1));
        java.util.function.Supplier<String> publicar2 = () -> intentar(salida, () -> planes.persist(p2, g2));
        var r1 = CompletableFuture.supplyAsync(publicar1);
        var r2 = CompletableFuture.supplyAsync(publicar2);
        var resultados = java.util.List.of(r1.get(30, TimeUnit.SECONDS), r2.get(30, TimeUnit.SECONDS));
        assertEquals(1, resultados.stream().filter("PUBLICADO"::equals).count(), resultados.toString());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ? AND status ="
                + " 'ACTIVE'", Integer.class, c.id()), "nunca dos planes activos");
    }

    @Test
    @DisplayName("dos publicaciones concurrentes y una retirada simultanea: nunca un plan tras la retirada ni dos planes")
    void dosPublicacionesYRetiradaConcurrentes() throws Exception {
        var c = alta(true);
        var comando = GenerateWeeklyPlanCommand.firstPlan(c.id(), planes.firstPlanWeekStart(c.id()));
        var p1 = planes.prepare(comando);
        var g1 = planes.generate(p1);
        var p2 = planes.prepare(comando);
        var g2 = planes.generate(p2);
        var salida = new java.util.concurrent.CyclicBarrier(3);
        var r1 = CompletableFuture.supplyAsync(() -> intentar(salida, () -> planes.persist(p1, g1)));
        var r2 = CompletableFuture.supplyAsync(() -> intentar(salida, () -> planes.persist(p2, g2)));
        var r3 = CompletableFuture.supplyAsync(() -> intentar(salida, () -> {
            consentimientos.retirar(c.id(), ConsentimientoService.Tipo.STUDY_PARTICIPATION);
            return java.util.Optional.empty();
        }));
        var resultados = java.util.List.of(r1.get(30, TimeUnit.SECONDS), r2.get(30, TimeUnit.SECONDS),
                r3.get(30, TimeUnit.SECONDS));
        long publicados = resultados.subList(0, 2).stream().filter("PUBLICADO"::equals).count();
        assertTrue(publicados <= 1, resultados.toString());
        assertFalse(iam.isEligibleForPlanGeneration(c.id()), "la retirada siempre se aplica");
        if (publicados == 1) {
            // Solo puede haberse publicado ANTES de que la retirada se hiciera efectiva: su transaccion la espero.
            var plan = jdbc.queryForObject("SELECT created_at FROM weekly_training_plans WHERE user_id = ?",
                    java.time.OffsetDateTime.class, c.id());
            var retirada = jdbc.queryForObject("SELECT withdrawn_at FROM users WHERE user_id = ?",
                    java.time.OffsetDateTime.class, c.id());
            assertFalse(plan.isAfter(retirada.plusSeconds(1)), plan + " / " + retirada);
        }
        assertTrue(resultados.subList(0, 2).stream().allMatch(r -> r.equals("PUBLICADO")
                || r.equals("ParticipacionNoVigenteException") || r.equals("PlanCambioDuranteLaGeneracionException")
                || r.equals("PlanAlreadyExistsException") || r.equals("DataIntegrityViolationException")), resultados.toString());
        // Despues de la retirada, nada mas se publica.
        assertThrows(ParticipacionNoVigenteException.class, () -> planes.persist(p1, g1));
    }

    static String intentar(java.util.concurrent.CyclicBarrier salida, java.util.concurrent.Callable<?> accion) {
        try {
            salida.await(10, TimeUnit.SECONDS);
            accion.call();
            return "PUBLICADO";
        } catch (Exception e) {
            return e.getClass().getSimpleName();
        }
    }
}
