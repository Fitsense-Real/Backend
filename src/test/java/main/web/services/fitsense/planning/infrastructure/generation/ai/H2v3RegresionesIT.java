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
 * H2-v3 (PR-2.1) · LAS REGRESIONES DE PR-2.0, SIN COSTE. Usa las 51 salidas REALES de gpt-5-mini de la evaluacion de
 * H2-v2 (docs/FitSense/h2_v2/evaluacion_real/etapa1/salidas, ya pagadas; no se modifican) con el mismo perfil, la misma
 * semana y el mismo contexto:
 * <ol>
 *   <li>REPRODUCE cada rechazo: la salida vuelve a dar exactamente los problemas registrados;</li>
 *   <li>CAUSA en el texto de PR-2.0 (G02, G03, G15, G18 y G19): la orientacion por grupo como cifra que alcanzar
 *   ("acercate"), los ceros de waist y lower legs, y "baja series" en el reintento;</li>
 *   <li>PR-2.1 no contiene nada de eso, comunica la jerarquia y la prioridad exactamente como en la politica
 *   (DosisSemanalPolicy, ControlVolumen), y su reintento con esos mismos motivos no pide recortar dosis;</li>
 *   <li>COHERENCIA: seguir PR-2.1 al pie de la letra ({@link AplicadorJerarquiaPR21}, solo con los datos de su
 *   instantanea) convierte las propuestas rechazadas de G02, G03, G15, G18 y G19 en semanas que el validador acepta, y
 *   no estropea las que ya eran validas (G03 con PR-1.7, la rutina del motor de reglas);</li>
 *   <li>el mismo lector sobre las 51 salidas y las 26 rutinas del motor de reglas, como comparacion preliminar.</li>
 * </ol>
 * No modifica validadores, umbrales, catalogo, perfiles ni resultados historicos.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("H2-v3 (PR-2.1): reproduce las regresiones reales de PR-2.0 y comprueba que la jerarquia es coherente")
class H2v3RegresionesIT {

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

    static final Path SALIDAS = Path.of("docs/FitSense/h2_v2/evaluacion_real/etapa1/salidas");
    static final Path INFORME = Path.of(System.getProperty("fitsense.h2v3.salida", "target/h2v3-regresiones"));
    static final LocalDate SEMANA = LocalDate.of(2026, 11, 23);
    static final List<String> FOCO = List.of("G02", "G03", "G15", "G18", "G19");
    static final ObjectMapper MAPPER = new ObjectMapper();
    static final Map<String, PlanGenerationContext> CTX = new TreeMap<>();
    static final List<String> FILAS = Collections.synchronizedList(new ArrayList<>());

    PlanGenerationContext contexto(H2v2PrimeraPropuestaIT.Caso c) {
        return CTX.computeIfAbsent(c.id(), k -> {
            String email = ("h2v3-" + c.id() + "@pruebas.invalid").toLowerCase();
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

    PlanPromptBuilder builder(String version) {
        var b = new PlanPromptBuilder(json);
        b.setPromptVersion(version);
        return b;
    }

    String prompt(PlanGenerationContext ctx, String version, AiRetryFeedback fb) {
        return builder(version).build(ctx, json.write(PlanInputSnapshot.of(ctx, version)), fb);
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

    static String salida(String caso, String brazo) throws Exception {
        return Files.readString(SALIDAS.resolve(caso + "_" + brazo + ".json"), StandardCharsets.UTF_8);
    }

    static List<String> registrados(String caso, String brazo) throws Exception {
        var t = Files.readString(SALIDAS.resolve(caso + "_" + brazo + "_problemas.txt"), StandardCharsets.UTF_8);
        return t.isBlank() ? List.of() : t.lines().toList();
    }

    static final Pattern CODIGO = Pattern.compile("^(V\\d+[A-Z]?)\\b");

    static String codigos(List<String> p) {
        return p.stream().map(x -> {
            var m = CODIGO.matcher(x.strip());
            return m.find() ? m.group(1) : "OTRO";
        }).collect(Collectors.groupingBy(x -> x, TreeMap::new, Collectors.counting())).toString();
    }

    // ------------------------------------------------------------------ 1-4: los cinco casos

    @Test
    @Order(1)
    @DisplayName("G02, G03, G15, G18, G19: reproduce el rechazo de PR-2.0, la causa en su texto y la correccion de PR-2.1")
    void casosDeLaRegresion() throws Exception {
        for (var id : FOCO) {
            var ctx = contexto(caso(id));
            var salidaB = salida(id, "B");
            // 1. Reproduce: los mismos problemas que en la evaluacion real.
            var p = problemas(salidaB, ctx);
            assertEquals(registrados(id, "B"), p, id + ": la salida real de PR-2.0 vuelve a dar sus mismos problemas");
            assertFalse(p.isEmpty(), id);

            // 2. La causa en el texto de PR-2.0.
            var p20 = prompt(ctx, PlanPromptBuilder.VERSION_H2V2, AiRetryFeedback.none());
            assertTrue(p20.contains("acercate a ella si cabe"), id + ": PR-2.0 pide acercarse a la orientacion");
            var retry20 = prompt(ctx, PlanPromptBuilder.VERSION_H2V2, new AiRetryFeedback(salidaB, p, List.of()));
            assertTrue(retry20.contains("tambien puedes bajar series") || retry20.contains("baja series"),
                    id + ": el reintento de PR-2.0 invita a bajar series");

            // 3. PR-2.1: sin lo anterior, con la jerarquia y la prioridad de la politica.
            var p21 = prompt(ctx, PlanPromptBuilder.VERSION_H2V3, AiRetryFeedback.none());
            for (var frase : List.of("acercate a ella", "objetivo_series_semana", "baja series", "bajar series"))
                assertFalse(p21.contains(frase), id + ": PR-2.1 no dice '" + frase + "'");
            assertTrue(p21.contains("22. JERARQUIA DE RESTRICCIONES"), id);
            assertTrue(p21.contains("NO es una cifra que alcanzar ni un limite"), id);
            assertTrue(p21.contains("COMPROBACION FINAL, en el orden de la regla 22"), id);
            comprobarPrioridad(id, ctx);
            var retry21 = prompt(ctx, PlanPromptBuilder.VERSION_H2V3, new AiRetryFeedback(salidaB, p, List.of()));
            for (var frase : List.of("baja series", "bajar series", "tambien puedes bajar"))
                assertFalse(retry21.contains(frase), id + ": el reintento de PR-2.1 no dice '" + frase + "'");
            assertTrue(retry21.contains("NUNCA cambies la dosis de una colocacion"), id);
            assertTrue(retry21.contains("las reglas 1 a 22 completas"), id);
            if (p.stream().anyMatch(x -> x.startsWith("V24")))
                assertTrue(retry21.contains("V24: vuelve a copiar la dosis exacta"), id);
            if (p.stream().anyMatch(x -> x.startsWith("V25")))
                assertTrue(retry21.contains("V25: anade una colocacion con una dosis de esa capacidad"), id);
            if (p.stream().anyMatch(x -> x.startsWith("V15")))
                assertTrue(retry21.contains("V15: cuenta los ejercicios de trabajo n"), id);
            if (p.stream().anyMatch(x -> x.startsWith("V23")))
                assertTrue(retry21.contains("V23: cambia un ejercicio de ese mismo grupo"), id);

            // 4. Coherencia: siguiendo PR-2.1 al pie de la letra, la propuesta rechazada pasa el validador.
            var lector = new AplicadorJerarquiaPR21(json.write(PlanInputSnapshot.of(ctx, PlanPromptBuilder.VERSION_H2V3)));
            var r = lector.aplicar(salidaB);
            var tras = problemas(r.salida(), ctx);
            FILAS.add("FOCO;" + id + ";B;" + codigos(p) + ";" + codigos(tras) + ";" + r.cambios().size() + ";"
                    + String.join(" | ", r.cambios()));
            guardar(id + "_B_tras_PR21.json", r.salida());
            assertEquals(List.of(), tras, id + ": PR-2.1 aplicada al pie de la letra no deja rechazos. Cambios: "
                    + r.cambios());
        }
        // G03 con PR-1.7 era valida: PR-2.1 no exige nada que la invalide (no le cambia ninguna dosis).
        var ctx = contexto(caso("G03"));
        assertEquals(List.of(), problemas(salida("G03", "A"), ctx));
        var r = new AplicadorJerarquiaPR21(json.write(PlanInputSnapshot.of(ctx, PlanPromptBuilder.VERSION_H2V3)))
                .aplicar(salida("G03", "A"));
        assertEquals(List.of(), r.cambios(), "la propuesta valida de G03 ya cumple PR-2.1: " + r.cambios());
        assertEquals(List.of(), problemas(r.salida(), ctx));
    }

    /** La prioridad de PR-2.1 es la orientacion de DosisSemanalPolicy sin los ceros, con los techos de V26. */
    void comprobarPrioridad(String id, PlanGenerationContext ctx) throws Exception {
        JsonNode v = MAPPER.readTree(json.write(PlanInputSnapshot.of(ctx, PlanPromptBuilder.VERSION_H2V3)))
                .get("constraints").get("volumen_semanal");
        String obj = ctx.profile().goalType(), nivel = ctx.profile().fitnessLevel();
        assertEquals(DosisSemanalPolicy.techoSeriesSesion(nivel), v.get("techo_series_sesion").asInt(), id);
        assertEquals(DosisSemanalPolicy.techoSeriesSemana(nivel), v.get("techo_series_semana").asInt(), id);
        assertFalse(v.has("objetivo_series_semana"), id);
        var prioridad = v.get("prioridad_series_semana");
        prioridad.fields().forEachRemaining(e -> {
            assertTrue(e.getValue().asInt() > 0, id + ": sin ceros");
            assertEquals(DosisSemanalPolicy.objetivoSeriesSemana(obj, nivel, e.getKey()), e.getValue().asInt(), id);
        });
        // Los grupos con 0 en la politica no aparecen (no son una prohibicion).
        ctx.availableExercises().stream().map(x -> x.bodyPartCode()).filter(Objects::nonNull).distinct()
                .filter(g -> DosisSemanalPolicy.objetivoSeriesSemana(obj, nivel, g) == 0)
                .forEach(g -> assertFalse(prioridad.has(g), id + ": " + g + " con 0 no se envia"));
    }

    // ------------------------------------------------------------------ 5: comparacion preliminar

    @Test
    @Order(2)
    @DisplayName("Los 26 perfiles: las 51 salidas reales y las rutinas del motor de reglas con el lector literal de PR-2.1")
    void todosLosPerfiles() throws Exception {
        int reglasValidas = 0, reglasIntactas = 0, salidas = 0;
        var siguenRechazadas = new ArrayList<String>();
        for (var c : H2v2PrimeraPropuestaIT.casos()) {
            var ctx = contexto(c);
            var snapshot21 = json.write(PlanInputSnapshot.of(ctx, PlanPromptBuilder.VERSION_H2V3));
            // El texto de PR-2.1 en los 26 perfiles (4 objetivos; 3, 4, 5 y 6 dias).
            var p21 = prompt(ctx, PlanPromptBuilder.VERSION_H2V3, AiRetryFeedback.none());
            for (var frase : List.of("acercate a ella", "objetivo_series_semana", "baja series", "bajar series"))
                assertFalse(p21.contains(frase), c.id() + ": PR-2.1 no dice '" + frase + "'");
            for (var frase : List.of("22. JERARQUIA DE RESTRICCIONES", "1) DOSIS (V24)", "2) TECHOS DE SERIES (V26",
                    "3) REPARTO (V15", "4) COBERTURA (V23 y V25)", "5) DURACION (regla 4)",
                    "COMPROBACION FINAL, en el orden de la regla 22"))
                assertTrue(p21.contains(frase), c.id() + ": PR-2.1 dice '" + frase + "'");
            comprobarPrioridad(c.id(), ctx);
            // El contrato de V15 (tabla de la mitad, grupos con material) y los techos de V26 son los de PR-2.0, que
            // ContratoH2v2IT comprueba exactos (una unidad de mas da el rechazo); PR-1.7 no cambia.
            var s20 = MAPPER.readTree(json.write(PlanInputSnapshot.of(ctx, PlanPromptBuilder.VERSION_H2V2))).get("constraints");
            var s21 = MAPPER.readTree(snapshot21).get("constraints");
            assertEquals(s20.get("suggested_split"), s21.get("suggested_split"), c.id());
            assertEquals(s20.get("volumen_semanal").get("techo_series_sesion"), s21.get("volumen_semanal").get("techo_series_sesion"));
            assertEquals(s20.get("volumen_semanal").get("techo_series_semana"), s21.get("volumen_semanal").get("techo_series_semana"));
            assertEquals(s20.get("volumen_semanal").get("no_cuentan"), s21.get("volumen_semanal").get("no_cuentan"));
            assertEquals(s20.get("dosis_por_clase"), s21.get("dosis_por_clase"), c.id());
            assertEquals(s20.get("capacidades"), s21.get("capacidades"), c.id());
            // PR-2.1 no exige nada que la rutina valida del motor de reglas no cumpla.
            try {
                var d = reglas.generate(ctx, List.of());
                validador.validate(d, ctx, configuracion.durationToRepsDivisor());
                reglasValidas++;
                var r = new AplicadorJerarquiaPR21(snapshot21).aplicar(desdeBorrador(d));
                if (r.cambios().isEmpty()) reglasIntactas++;
                FILAS.add("REGLAS;" + c.id() + ";-;{};" + codigos(problemas(r.salida(), ctx)) + ";" + r.cambios().size()
                        + ";" + String.join(" | ", r.cambios()));
                assertEquals(List.of(), problemas(r.salida(), ctx), c.id() + ": la rutina del motor sigue valida");
            } catch (InvalidPlanDraftException e) {
                FILAS.add("REGLAS;" + c.id() + ";-;" + codigos(e.problems()) + ";-;0;sin rutina valida");
            }
            for (var b : List.of("A", "B")) {
                if (!Files.exists(SALIDAS.resolve(c.id() + "_" + b + ".json"))) continue;
                var s = salida(c.id(), b);
                var antes = problemas(s, ctx);
                assertEquals(registrados(c.id(), b), antes, c.id() + b + ": reproduce lo registrado");
                var r = new AplicadorJerarquiaPR21(snapshot21).aplicar(s);
                var tras = problemas(r.salida(), ctx);
                FILAS.add("SALIDA;" + c.id() + ";" + b + ";" + codigos(antes) + ";" + codigos(tras) + ";"
                        + r.cambios().size() + ";" + String.join(" | ", r.cambios()));
                salidas++;
                if (!tras.isEmpty()) siguenRechazadas.add(c.id() + b + " " + tras);
            }
        }
        System.out.println("H2V3_REGLAS validas=" + reglasValidas + " intactas_con_PR21=" + reglasIntactas);
        assertEquals(reglasValidas, reglasIntactas, "toda rutina valida del motor ya cumple la jerarquia de PR-2.1");
        System.out.println("H2V3_SALIDAS reales=" + salidas + " rechazadas_tras_PR21=" + siguenRechazadas.size());
        assertEquals(51, salidas, "las 51 salidas reales de la evaluacion de H2-v2");
        // Seguir PR-2.1 al pie de la letra deja validas TODAS: ninguna instruccion obliga a romper otra regla.
        assertEquals(List.of(), siguenRechazadas);
    }

    @AfterAll
    static void informe() throws Exception {
        Files.createDirectories(INFORME);
        var sb = new StringBuilder("tipo;caso;brazo;codigos_antes;codigos_tras_PR21;cambios;detalle\n");
        FILAS.stream().sorted().forEach(f -> sb.append(f.replace('\n', ' ')).append('\n'));
        Files.writeString(INFORME.resolve("lector_literal_PR21.csv"), sb.toString());
    }

    void guardar(String nombre, String contenido) throws Exception {
        Files.createDirectories(INFORME.resolve("salidas"));
        Files.writeString(INFORME.resolve("salidas").resolve(nombre), contenido);
    }

    /** Un borrador del motor de reglas en el formato de salida de la IA. */
    String desdeBorrador(main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft d) throws Exception {
        var sesiones = new ArrayList<Map<String, Object>>();
        for (var w : d.workouts()) {
            var ej = new ArrayList<Map<String, Object>>();
            int o = 1;
            for (var e : w.exercises()) {
                var m = new LinkedHashMap<String, Object>();
                m.put("exercise_id", e.exerciseId());
                m.put("exercise_order", o++);
                m.put("prescription_type", e.prescriptionType().name());
                m.put("planned_sets", e.plannedSets());
                m.put("planned_reps", e.plannedReps());
                m.put("planned_duration_seconds", e.plannedDurationSeconds());
                m.put("target_load_kg", e.targetLoadKg());
                m.put("rest_seconds", e.restSeconds());
                m.put("notes", e.notes());
                ej.add(m);
            }
            var s = new LinkedHashMap<String, Object>();
            s.put("scheduled_date", w.scheduledDate().toString());
            s.put("focus_code", w.focus().name());
            s.put("workout_name", w.name());
            s.put("expected_duration_minutes", w.expectedDurationMinutes());
            s.put("exercises", ej);
            sesiones.add(s);
        }
        var p = new LinkedHashMap<String, Object>();
        p.put("schema_version", main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft.SCHEMA_VERSION);
        p.put("plan_name", "Motor de reglas");
        p.put("total_volume", 0);
        p.put("rationale", "Rutina del motor de reglas en formato de salida.");
        p.put("workouts", sesiones);
        return MAPPER.writeValueAsString(p);
    }
}
