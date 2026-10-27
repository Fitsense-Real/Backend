package main.web.services.fitsense.lectura;

import main.web.services.fitsense.planning.application.internal.commandservices.SafetyExclusionResolver;
import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalConfigurationService;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.*;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.infrastructure.validation.StrictValidationAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * EVALUACION EN SOLO LECTURA DE LOS PLANES EXISTENTES FRENTE A LA V24 POR
 * CAPACIDADES (antes de encender la progresion en Neon).
 * <p>
 * <b>No escribe en la fuente.</b> {@link CopiaDeLectura} la abre en solo
 * lectura (comprobado) y copia los datos a un PostgreSQL efimero local; todo lo
 * demas ocurre en esa copia. No llama a Replicate.
 * <p>
 * Para cada participante, su ultima semana publicada es la semana medida y se
 * simula la COPIA del ciclo siguiente ({@code NO_CHANGE}, sin paso) con:
 * <ul>
 *   <li><b>A</b> · los interruptores apagados: la validacion de hoy;</li>
 *   <li><b>B</b> · progresion encendida: V24 por capacidades en P2-D con las
 *       dosis heredadas de linaje y las reducciones autorizadas.</li>
 * </ul>
 * Lo que importa son las semanas que <b>A publica y B no</b>: encender la
 * progresion las dejaria {@code NO_FACTIBLE}. El informe solo lleva
 * identificadores, objetivo y codigos de regla; ningun dato personal.
 * <p>
 * Uso (lo ejecuta la persona responsable, con credenciales de solo lectura):
 * <pre>
 * mvn -o verify -Pintegration -Dtest=NoUnit -Dsurefire.failIfNoSpecifiedTests=false \
 *   -Dit.test=EvaluacionLecturaV24IT \
 *   -Dfitsense.lectura.url='jdbc:postgresql://HOST/DB?sslmode=require' \
 *   -Dfitsense.lectura.usuario=USUARIO -Dfitsense.lectura.clave=CLAVE
 * </pre>
 * Informe: {@code target/lectura-v24/informe.md}.
 */
@EnabledIfSystemProperty(named = "fitsense.lectura.url", matches = ".+")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        // Solo para que el ensamblador calcule las dosis con linaje en la copia; los
        // productores A y B se construyen aqui con sus propios interruptores.
        "fitsense.adaptation.progression.enabled=true"
})
@DisplayName("Lectura · planes existentes frente a la V24 por capacidades (A: hoy, B: progresion encendida)")
class EvaluacionLecturaV24IT {

    @ServiceConnection
    static PostgreSQLContainer copia = new PostgreSQLContainer("postgres:16-alpine");

    static CopiaDeLectura.Resultado copiado;

    static {
        String url = System.getProperty("fitsense.lectura.url");
        if (url != null && !url.isBlank()) {
            copia.start();
            try {
                copiado = CopiaDeLectura.copiar(url, System.getProperty("fitsense.lectura.usuario"),
                        System.getProperty("fitsense.lectura.clave"), copia.getJdbcUrl(), copia.getUsername(),
                        copia.getPassword());
            } catch (Exception e) {
                throw new IllegalStateException("No se pudo copiar la fuente en solo lectura: " + e, e);
            }
        }
    }

    @MockitoBean ReplicateSessionFixGenerator ia;   // nunca se llama a Replicate

    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired WeekDerivationBuilder derivaciones;
    @Autowired SafetyExclusionResolver exclusiones;
    @Autowired SessionDurationEstimator estimador;
    @Autowired ExternalConfigurationService configuracion;
    @Autowired JdbcTemplate jdbc;
    @Value("${fitsense.coverage.enforcement:OBSERVE}") String cobertura;

    record Fila(long usuario, LocalDate semana, String objetivo, String a, String b, String detalle) {}

    @Test
    void evaluar() throws Exception {
        assertFalse(copiado.filas().isEmpty(), "la copia esta vacia");

        var hoy = new AdaptedWeekProducer(new SessionFixApplier(), new ConservationValidator(),
                new StrictValidationAdapter(estimador, configuracion, cobertura, false, false));
        var conProgresion = new AdaptedWeekProducer(new SessionFixApplier(), new ConservationValidator(),
                new StrictValidationAdapter(estimador, configuracion, cobertura, false, true), true);

        var filas = new ArrayList<Fila>();
        var ultimas = jdbc.queryForList("SELECT p.user_id, max(p.week_start_date) AS semana FROM weekly_training_plans p"
                + " WHERE p.status <> 'REPLACED' GROUP BY p.user_id ORDER BY p.user_id");
        for (var u : ultimas) {
            long usuario = ((Number) u.get("user_id")).longValue();
            var semana = ((Date) u.get("semana")).toLocalDate();
            String objetivo = jdbc.queryForList("SELECT goal_type FROM user_profiles WHERE user_id = ?",
                    String.class, usuario).stream().findFirst().orElse("?");
            try {
                var ctx = planComandos.prepare(new GenerateWeeklyPlanCommand(usuario, semana.plusWeeks(1),
                        null, true, null, null)).context();
                var derivacion = derivar(ctx);
                if (derivacion == null) {
                    filas.add(new Fila(usuario, semana, objetivo, "-", "-", "sin semana medida que copiar"));
                    continue;
                }
                var elegibles = new LinkedHashMap<Long, CandidateExercise>();
                ctx.availableExercises().forEach(c -> elegibles.put(c.exerciseId(), c));
                ExclusionesDeSeguridad excl;
                try {
                    excl = exclusiones.resolver(ctx);
                } catch (RuntimeException e) {
                    excl = ExclusionesDeSeguridad.ninguna();
                }
                var ra = hoy.produce(ctx, derivacion, List.of(), elegibles, excl);
                var rb = conProgresion.produce(ctx, derivacion, List.of(), elegibles, excl);
                var soloB = new ArrayList<>(rb.problemas());
                soloB.removeAll(ra.problemas());
                long heredadas = rb.conservacion().dosisAutorizadas().size();
                filas.add(new Fila(usuario, semana, objetivo, estado(ra), estado(rb),
                        (heredadas > 0 ? heredadas + " dosis heredadas con linaje. " : "")
                                + resumen(soloB)));
            } catch (RuntimeException e) {
                filas.add(new Fila(usuario, semana, objetivo, "NO_EVALUABLE", "NO_EVALUABLE",
                        e.getClass().getSimpleName() + ": " + recortar(String.valueOf(e.getMessage()))));
            }
        }
        escribir(filas);
        // Comprobacion de la propia copia: mismas filas que se leyeron.
        for (var t : List.of("weekly_training_plans", "planned_workouts", "planned_workout_exercises"))
            assertEquals(copiado.filas().get(t), jdbc.queryForObject("SELECT count(*) FROM " + t, Integer.class), t);
    }

    /** La copia de la semana medida con las fechas nuevas, igual que el ciclo. */
    private WeekDerivation derivar(PlanGenerationContext ctx) {
        var previa = ctx.previousWeek();
        if (previa == null || previa.workouts() == null || previa.workouts().isEmpty()) return null;
        var origenes = new ArrayList<Long>();
        var fechas = new ArrayList<LocalDate>();
        for (var s : previa.workouts()) {
            if (s.plannedWorkoutId() == null || s.scheduledDate() == null) continue;
            origenes.add(s.plannedWorkoutId());
            fechas.add(s.scheduledDate().plusWeeks(1));
        }
        return derivaciones.derive(previa, origenes, fechas);
    }

    private static String estado(AdaptedWeekResult r) {
        return r.publicable() ? "PUBLICABLE" : "NO_FACTIBLE";
    }

    /** Solo el codigo de regla y los ids: «V24: el ejercicio 1102 del 2026-11-09 ...» -> «V24 ej. 1102». */
    private static String resumen(List<String> problemas) {
        var codigos = new TreeMap<String, Integer>();
        for (var p : problemas) {
            String regla = p.contains(":") ? p.substring(0, p.indexOf(':')) : p;
            var m = java.util.regex.Pattern.compile("ejercicio (\\d+)").matcher(p);
            codigos.merge(regla + (m.find() ? " ej. " + m.group(1) : ""), 1, Integer::sum);
        }
        return codigos.isEmpty() ? "" : "Solo con B: " + codigos.keySet();
    }

    private static String recortar(String s) {
        return s.length() > 160 ? s.substring(0, 160) + "..." : s;
    }

    private void escribir(List<Fila> filas) throws Exception {
        long total = filas.stream().filter(f -> !f.a().equals("NO_EVALUABLE") && !f.a().equals("-")).count();
        long regresiones = filas.stream().filter(f -> f.a().equals("PUBLICABLE") && f.b().equals("NO_FACTIBLE")).count();
        long mejoras = filas.stream().filter(f -> f.a().equals("NO_FACTIBLE") && f.b().equals("PUBLICABLE")).count();
        var sb = new StringBuilder("# Evaluación en solo lectura · V24 por capacidades\n\n")
                .append("Fuente: Flyway ").append(copiado.versionFuente()).append(" · tablas copiadas: ")
                .append(copiado.filas().size()).append(" · filas de planes: ")
                .append(copiado.filas().get("weekly_training_plans")).append("\n\n")
                .append("| Participantes evaluados | A y B publican | **A publica, B no** | A no, B sí | Ninguno |\n")
                .append("|---:|---:|---:|---:|---:|\n")
                .append("| ").append(total).append(" | ")
                .append(filas.stream().filter(f -> f.a().equals("PUBLICABLE") && f.b().equals("PUBLICABLE")).count())
                .append(" | **").append(regresiones).append("** | ").append(mejoras).append(" | ")
                .append(filas.stream().filter(f -> f.a().equals("NO_FACTIBLE") && f.b().equals("NO_FACTIBLE")).count())
                .append(" |\n\n| Usuario | Semana medida | Objetivo | A (hoy) | B (progresión) | Detalle |\n|---|---|---|---|---|---|\n");
        for (var f : filas)
            sb.append("| ").append(f.usuario()).append(" | ").append(f.semana()).append(" | ").append(f.objetivo())
                    .append(" | ").append(f.a()).append(" | ").append(f.b()).append(" | ").append(f.detalle()).append(" |\n");
        var salida = Path.of("target", "lectura-v24", "informe.md");
        Files.createDirectories(salida.getParent());
        Files.writeString(salida, sb.toString());
        System.out.println(sb);
    }
}
