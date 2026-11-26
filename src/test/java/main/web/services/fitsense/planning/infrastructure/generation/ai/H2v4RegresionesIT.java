package main.web.services.fitsense.planning.infrastructure.generation.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalConfigurationService;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.services.DosisSemanalPolicy;
import main.web.services.fitsense.planning.domain.services.PlanDraftNormalizer;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.generation.rules.RuleBasedTrainingPlanGenerator;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * H2-v4 (PR-2.2) · SIN COSTE. Usa las salidas REALES ya pagadas de gpt-5-mini (no se modifican):
 * docs/FitSense/h2_v2/evaluacion_real/etapa1/salidas (51: PR-1.7 y PR-2.0) y
 * docs/FitSense/h2_v3/evaluacion_real/etapa1/salidas (52: PR-1.7 y PR-2.1), con el mismo perfil, semana y contexto.
 * <ol>
 *   <li>G17, G21 y G24 (los rechazos de PR-2.1): se reproducen; la causa en PR-2.1 (listas de ids que cruzar, tabla
 *   de la mitad); PR-2.2 dice el recuento junto al minimo de grupos y marca lo que no cuenta en cada ejercicio;</li>
 *   <li>G02, G03, G15, G18 y G19 (los de PR-2.0): siguen corregidos en el texto de PR-2.2;</li>
 *   <li>el CONTRATO de PR-2.2 es el de PR-2.1 sin perder nada: mismas marcas que las listas, la mitad exacta de la
 *   tabla, mismos techos, prioridad, dosis y capacidades; PR-2.2 mas corto que PR-2.1 en los 26 perfiles;</li>
 *   <li>siguiendo PR-2.2 al pie de la letra ({@link AplicadorJerarquiaPR21} con su instantanea) las 103 salidas reales
 *   y las 26 rutinas del motor de reglas quedan validas; el motor, sin cambios.</li>
 * </ol>
 * Las salidas corregidas por el lector NO son generaciones de la IA: solo muestran que las instrucciones se pueden
 * cumplir a la vez. No modifica validadores, umbrales, catalogo, perfiles ni resultados historicos.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("H2-v4 (PR-2.2): G17, G21, G24 y las regresiones de PR-2.0; contrato igual a PR-2.1 y mas corto")
class H2v4RegresionesIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired RuleBasedTrainingPlanGenerator reglas;
    @Autowired PlanDraftFromJsonAssembler ensamblador;
    @Autowired PlanDraftNormalizer normalizador;
    @Autowired PlanDraftValidator validador;
    @Autowired ExternalConfigurationService configuracion;
    @Autowired UserProfileCommandService perfiles;
    @Autowired JsonSupport json;
    @Autowired JdbcTemplate jdbc;

    static final Path SALIDAS_V2 = Path.of("docs/FitSense/h2_v2/evaluacion_real/etapa1/salidas");
    static final Path SALIDAS_V3 = Path.of("docs/FitSense/h2_v3/evaluacion_real/etapa1/salidas");
    static final Path INFORME = Path.of(System.getProperty("fitsense.h2v4.salida", "target/h2v4-regresiones"));
    static final LocalDate SEMANA = LocalDate.of(2026, 11, 23);
    static final String V21 = PlanPromptBuilder.VERSION_H2V3, V22 = PlanPromptBuilder.VERSION_H2V4;
    static final ObjectMapper MAPPER = new ObjectMapper();
    static final Map<String, PlanGenerationContext> CTX = new TreeMap<>();
    static final List<String> FILAS = Collections.synchronizedList(new ArrayList<>());
    static final List<String> PROHIBIDAS = List.of("acercate a ella", "objetivo_series_semana", "baja series",
            "bajar series", "tambien puedes bajar", "complementos_no_cuentan", "max_per_body_part_por_n_trabajo",
            "volumen_semanal.no_cuentan");

    PlanGenerationContext contexto(H2v2PrimeraPropuestaIT.Caso c) {
        return CTX.computeIfAbsent(c.id(), k -> {
            String email = ("h2v4-" + c.id() + "@pruebas.invalid").toLowerCase();
            jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                    + " VALUES (?, 'x', 'Sintetico', now(), now())", email);
            long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
            boolean gym = c.lugar() == TrainingLocation.GYM;
            boolean r = c.restringido();
            perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 3, 10), BiologicalSex.FEMALE,
                    new BigDecimal("165"), new BigDecimal("72"), null, c.nivel(), c.objetivo(), "evaluacion pareada H2",
                    c.lugar(), (short) c.dias().size(), c.dias(), c.minutos(),
                    gym ? H2v2PrimeraPropuestaIT.GIMNASIO : H2v2PrimeraPropuestaIT.CASA, List.of(),
                    r ? "Restricciones declaradas" : "Ninguna", !r, true, !r, !r, !r));
            return planComandos.prepare(GenerateWeeklyPlanCommand.firstPlan(u, SEMANA)).context();
        });
    }

    static H2v2PrimeraPropuestaIT.Caso caso(String id) {
        return H2v2PrimeraPropuestaIT.casos().stream().filter(x -> x.id().equals(id)).findFirst().orElseThrow();
    }

    String prompt(PlanGenerationContext ctx, String version, AiRetryFeedback fb) {
        var b = new PlanPromptBuilder(json);
        b.setPromptVersion(version);
        return b.build(ctx, json.write(PlanInputSnapshot.of(ctx, version)), fb);
    }

    List<String> problemas(String salida, PlanGenerationContext ctx) {
        try {
            var d = normalizador.normalize(ensamblador.toDraft(salida, "gpt-5-mini"), ctx).draft();
            validador.validate(d, ctx, configuracion.durationToRepsDivisor());
            return List.of();
        } catch (InvalidPlanDraftException e) {
            return e.problems();
        }
    }

    static String leer(Path dir, String caso, String brazo) throws Exception {
        return Files.readString(dir.resolve(caso + "_" + brazo + ".json"), StandardCharsets.UTF_8);
    }

    static List<String> registrados(Path dir, String caso, String brazo) throws Exception {
        var t = Files.readString(dir.resolve(caso + "_" + brazo + "_problemas.txt"), StandardCharsets.UTF_8);
        return t.isBlank() ? List.of() : t.lines().toList();
    }

    static final Pattern CODIGO = Pattern.compile("^(V\\d+[A-Z]?)\\b");

    static String codigos(List<String> p) {
        return p.stream().map(x -> {
            var m = CODIGO.matcher(x.strip());
            return m.find() ? m.group(1) : "OTRO";
        }).collect(Collectors.groupingBy(x -> x, TreeMap::new, Collectors.counting())).toString();
    }

    void sinFrasesProhibidas(String id, String texto) {
        for (var f : PROHIBIDAS) assertFalse(texto.contains(f), id + ": PR-2.2 no dice '" + f + "'");
    }

    // ------------------------------------------------------------------ 1-2: los casos

    @Test
    @Order(1)
    @DisplayName("G17, G21, G24 (PR-2.1) y G02, G03, G15, G18, G19 (PR-2.0): reproducidos y corregidos por PR-2.2")
    void casos() throws Exception {
        record Caso(String id, Path dir, String version) {}
        var lista = List.of(new Caso("G17", SALIDAS_V3, V21), new Caso("G21", SALIDAS_V3, V21),
                new Caso("G24", SALIDAS_V3, V21), new Caso("G02", SALIDAS_V2, PlanPromptBuilder.VERSION_H2V2),
                new Caso("G03", SALIDAS_V2, PlanPromptBuilder.VERSION_H2V2),
                new Caso("G15", SALIDAS_V2, PlanPromptBuilder.VERSION_H2V2),
                new Caso("G18", SALIDAS_V2, PlanPromptBuilder.VERSION_H2V2),
                new Caso("G19", SALIDAS_V2, PlanPromptBuilder.VERSION_H2V2));
        for (var k : lista) {
            var ctx = contexto(caso(k.id()));
            var salida = leer(k.dir(), k.id(), "B");
            var p = problemas(salida, ctx);
            assertEquals(registrados(k.dir(), k.id(), "B"), p, k.id() + ": se reproduce el rechazo de " + k.version());
            assertFalse(p.isEmpty());
            var p22 = prompt(ctx, V22, AiRetryFeedback.none());
            sinFrasesProhibidas(k.id(), p22);
            for (var f : List.of("Solo cuentan los ejercicios de trabajo", "\"cuenta\": \"complemento\"",
                    "\"regla_mitad\": true", "redondeando hacia abajo", "tenga o no prioridad",
                    "21. SERIES POR GRUPO (V26)", "22. TODAS LAS REGLAS SON OBLIGATORIAS", "1) dosis exacta (V24)",
                    "nunca recortada", "4) cobertura (V23, V25)", "5) duracion", "COMPROBACION FINAL"))
                assertTrue(p22.contains(f), k.id() + ": PR-2.2 dice '" + f + "'");
            var retry = prompt(ctx, V22, new AiRetryFeedback(salida, p, List.of()));
            sinFrasesProhibidas(k.id() + " reintento", retry);
            assertTrue(retry.contains("NUNCA cambies la dosis de una colocacion"), k.id());
            assertTrue(retry.contains("las reglas 1 a 22 completas"), k.id());
            if (p.stream().anyMatch(x -> x.startsWith("V15")))
                assertTrue(retry.contains("V15: cuenta solo los ejercicios de trabajo de esa sesion"), k.id());
            if (p.stream().anyMatch(x -> x.startsWith("V24")))
                assertTrue(retry.contains("V24: vuelve a copiar la dosis exacta"), k.id());
            if (p.stream().anyMatch(x -> x.startsWith("V25")))
                assertTrue(retry.contains("V25: anade una colocacion con una dosis de esa capacidad"), k.id());
            if (p.stream().anyMatch(x -> x.startsWith("V23")))
                assertTrue(retry.contains("V23: cambia un ejercicio de ese grupo por uno del slot que falta"), k.id());
            var r = new AplicadorJerarquiaPR21(json.write(PlanInputSnapshot.of(ctx, V22))).aplicar(salida);
            var tras = problemas(r.salida(), ctx);
            FILAS.add("CASO;" + k.id() + ";B " + k.version() + ";" + codigos(p) + ";" + codigos(tras) + ";"
                    + r.cambios().size() + ";" + String.join(" | ", r.cambios()));
            guardar(k.id() + "_B_" + k.version() + "_tras_PR22.json", r.salida());
            assertEquals(List.of(), tras, k.id() + ": siguiendo PR-2.2 no quedan rechazos. Cambios: " + r.cambios());
        }
        // G21 y G24: en PR-2.1, waist y lower legs (prioridad 0) no aparecen en la prioridad; en PR-2.2 tampoco, pero
        // la sesion inferior los nombra entre body_parts_con_material y la regla dice que cardio y movilidad no cuentan.
        for (var id : List.of("G21", "G24")) {
            var ctx = contexto(caso(id));
            var s22 = MAPPER.readTree(json.write(PlanInputSnapshot.of(ctx, V22))).get("constraints");
            boolean inferior = false;
            for (var ses : s22.get("suggested_split"))
                if (ses.get("focus_code").asText().equals("LOWER_BODY")) {
                    inferior = true;
                    var mat = new ArrayList<String>();
                    ses.get("body_parts_con_material").forEach(g -> mat.add(g.asText()));
                    assertTrue(mat.containsAll(List.of("upper legs", "lower legs", "waist")), id + " " + mat);
                    assertEquals(2, ses.get("min_body_parts").asInt(), id);
                    assertTrue(ses.get("regla_mitad").asBoolean(), id);
                }
            assertTrue(inferior, id);
        }
    }

    // ------------------------------------------------------------------ 3-4: contrato y coherencia

    @Test
    @Order(2)
    @DisplayName("26 perfiles: contrato de PR-2.2 = PR-2.1 (marcas = listas, mitad = tabla), mas corto; 103 salidas reales")
    void todosLosPerfiles() throws Exception {
        int reglasValidas = 0, reglasIntactas = 0, salidas = 0;
        var siguenRechazadas = new ArrayList<String>();
        for (var c : H2v2PrimeraPropuestaIT.casos()) {
            var ctx = contexto(c);
            var p21 = prompt(ctx, V21, AiRetryFeedback.none());
            var p22 = prompt(ctx, V22, AiRetryFeedback.none());
            sinFrasesProhibidas(c.id(), p22);
            assertTrue(p22.length() < p21.length(), c.id() + ": PR-2.2 mas corto que PR-2.1 (" + p22.length() + " vs "
                    + p21.length() + ")");
            var j21 = MAPPER.readTree(json.write(PlanInputSnapshot.of(ctx, V21)));
            var j22 = MAPPER.readTree(json.write(PlanInputSnapshot.of(ctx, V22)));
            assertEquals(V22, j22.get("prompt_version").asText());
            var c21 = j21.get("constraints");
            var c22 = j22.get("constraints");
            assertFalse(c22.has("complementos_no_cuentan"), c.id());
            // Marcas = listas.
            var comp = new TreeSet<Long>();
            c21.path("complementos_no_cuentan").forEach(x -> comp.add(x.asLong()));
            var sin = new TreeSet<Long>();
            c21.path("volumen_semanal").path("no_cuentan").forEach(x -> sin.add(x.asLong()));
            var marcaComp = new TreeSet<Long>();
            var marcaSin = new TreeSet<Long>();
            j22.get("available_exercises").forEach(e -> {
                var m = e.path("cuenta").asText(null);
                if ("complemento".equals(m)) marcaComp.add(e.get("exercise_id").asLong());
                else if ("sin_series".equals(m)) marcaSin.add(e.get("exercise_id").asLong());
                else assertNull(m, c.id() + ": marca desconocida " + m);
            });
            var ofrecidos = new TreeSet<Long>();
            j22.get("available_exercises").forEach(e -> ofrecidos.add(e.get("exercise_id").asLong()));
            comp.retainAll(ofrecidos);
            sin.retainAll(ofrecidos);
            assertEquals(comp, marcaComp, c.id() + ": complementos marcados = lista de PR-2.1");
            assertEquals(sin, marcaSin, c.id() + ": sin_series marcados = no_cuentan de PR-2.1");
            // Mismos ejercicios ofrecidos, en el mismo orden, con los mismos datos salvo la marca.
            assertEquals(j21.get("available_exercises").size(), j22.get("available_exercises").size(), c.id());
            for (int i = 0; i < j21.get("available_exercises").size(); i++) {
                var a = (com.fasterxml.jackson.databind.node.ObjectNode) j22.get("available_exercises").get(i).deepCopy();
                a.remove("cuenta");
                assertEquals(j21.get("available_exercises").get(i), a, c.id() + " ejercicio " + i);
            }
            // La mitad = la tabla; el resto de la sesion igual.
            for (int i = 0; i < c21.get("suggested_split").size(); i++) {
                var s21 = c21.get("suggested_split").get(i);
                var s22 = (com.fasterxml.jackson.databind.node.ObjectNode) c22.get("suggested_split").get(i).deepCopy();
                var tabla = s21.path("max_per_body_part_por_n_trabajo");
                assertEquals(!tabla.isMissingNode() && !tabla.isNull(), s22.path("regla_mitad").asBoolean(false), c.id());
                if (!tabla.isMissingNode() && !tabla.isNull())
                    tabla.fields().forEachRemaining(e -> assertEquals(Integer.parseInt(e.getKey()) / 2,
                            e.getValue().asInt(), c.id() + ": la tabla es la mitad redondeando hacia abajo"));
                s22.remove("regla_mitad");
                var s21b = (com.fasterxml.jackson.databind.node.ObjectNode) s21.deepCopy();
                s21b.remove("max_per_body_part_por_n_trabajo");
                assertEquals(s21b, s22, c.id() + " sesion " + i);
            }
            var v21 = (com.fasterxml.jackson.databind.node.ObjectNode) c21.get("volumen_semanal").deepCopy();
            v21.remove("no_cuentan");
            assertEquals(v21, c22.get("volumen_semanal"), c.id() + ": techos y prioridad iguales");
            for (var campo : List.of("dosis_por_clase", "capacidades", "slots_exigibles", "max_session_seconds",
                    "max_colocaciones_dia", "available_days", "max_rest_seconds"))
                assertEquals(c21.get(campo), c22.get(campo), c.id() + " " + campo);
            assertEquals(j21.get("safety_notes"), j22.get("safety_notes"), c.id());

            var snapshot22 = json.write(PlanInputSnapshot.of(ctx, V22));
            try {
                var d = reglas.generate(ctx, List.of());
                validador.validate(d, ctx, configuracion.durationToRepsDivisor());
                reglasValidas++;
                var r = new AplicadorJerarquiaPR21(snapshot22).aplicar(new H2v3RegresionesIT().desdeBorrador(d));
                if (r.cambios().isEmpty()) reglasIntactas++;
                assertEquals(List.of(), problemas(r.salida(), ctx), c.id());
            } catch (InvalidPlanDraftException e) {
                FILAS.add("REGLAS;" + c.id() + ";-;" + codigos(e.problems()) + ";-;0;sin rutina valida");
            }
            for (var fuente : List.of(Map.entry("H2-v2", SALIDAS_V2), Map.entry("H2-v3", SALIDAS_V3)))
                for (var b : List.of("A", "B")) {
                    if (!Files.exists(fuente.getValue().resolve(c.id() + "_" + b + ".json"))) continue;
                    var s = leer(fuente.getValue(), c.id(), b);
                    var antes = problemas(s, ctx);
                    assertEquals(registrados(fuente.getValue(), c.id(), b), antes, c.id() + b + " " + fuente.getKey());
                    var r = new AplicadorJerarquiaPR21(snapshot22).aplicar(s);
                    var tras = problemas(r.salida(), ctx);
                    salidas++;
                    FILAS.add("SALIDA;" + c.id() + ";" + fuente.getKey() + " " + b + ";" + codigos(antes) + ";"
                            + codigos(tras) + ";" + r.cambios().size() + ";" + String.join(" | ", r.cambios()));
                    if (!tras.isEmpty()) siguenRechazadas.add(c.id() + b + " " + fuente.getKey() + " " + tras);
                }
        }
        System.out.println("H2V4_REGLAS validas=" + reglasValidas + " intactas=" + reglasIntactas + " salidas=" + salidas
                + " rechazadas_tras_PR22=" + siguenRechazadas.size());
        assertEquals(reglasValidas, reglasIntactas, "toda rutina valida del motor ya cumple PR-2.2");
        assertEquals(103, salidas, "51 salidas de H2-v2 y 52 de H2-v3");
        assertEquals(List.of(), siguenRechazadas);
    }

    @AfterAll
    static void informe() throws Exception {
        Files.createDirectories(INFORME);
        var sb = new StringBuilder("tipo;caso;origen;codigos_antes;codigos_tras_PR22;cambios;detalle\n");
        FILAS.stream().sorted().forEach(f -> sb.append(f.replace('\n', ' ')).append('\n'));
        Files.writeString(INFORME.resolve("lector_literal_PR22.csv"), sb.toString());
    }

    void guardar(String nombre, String contenido) throws Exception {
        Files.createDirectories(INFORME.resolve("salidas"));
        Files.writeString(INFORME.resolve("salidas").resolve(nombre), contenido);
    }
}
