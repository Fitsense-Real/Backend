package main.web.services.fitsense.lectura;

import main.web.services.fitsense.planning.application.internal.commandservices.SafetyExclusionResolver;
import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalConfigurationService;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.*;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.infrastructure.validation.StrictValidationAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;


/**
 * La evaluacion comun de {@link EvaluacionLecturaV24IT} (sobre una copia) y
 * {@link EvaluacionDirectaV24IT} (directa, en solo lectura): para la ultima
 * semana publicada de cada participante, la copia del ciclo siguiente con
 * <b>A</b> (interruptores apagados, la validacion de hoy) y <b>B</b>
 * (progresion encendida). Solo lee: {@code prepare} y los productores no
 * escriben. Informe en {@code target/lectura-v24/informe.md}, sin datos
 * personales.
 */
abstract class EvaluacionV24Base {

    @MockitoBean ReplicateSessionFixGenerator ia;   // nunca se llama a Replicate

    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired WeekDerivationBuilder derivaciones;
    @Autowired SafetyExclusionResolver exclusiones;
    @Autowired SessionDurationEstimator estimador;
    @Autowired ExternalConfigurationService configuracion;
    @Autowired JdbcTemplate jdbc;
    @Value("${fitsense.coverage.enforcement:OBSERVE}") String cobertura;

    protected record Fila(long usuario, LocalDate semana, String objetivo, String a, String b, String detalle) {}

    /** Evalua a todos los participantes con alguna semana publicada. */
    protected List<Fila> evaluarTodos() {
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
        return filas;
    }

    /** La copia de la semana medida con las fechas nuevas, igual que el ciclo. */
    protected WeekDerivation derivar(PlanGenerationContext ctx) {
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

    protected static void escribir(List<Fila> filas, String fuente) throws Exception {
        long total = filas.stream().filter(f -> !f.a().equals("NO_EVALUABLE") && !f.a().equals("-")).count();
        long regresiones = filas.stream().filter(f -> f.a().equals("PUBLICABLE") && f.b().equals("NO_FACTIBLE")).count();
        long mejoras = filas.stream().filter(f -> f.a().equals("NO_FACTIBLE") && f.b().equals("PUBLICABLE")).count();
        var sb = new StringBuilder("# Evaluación en solo lectura · V24 por capacidades\n\n")
                .append(fuente).append("\n\n")
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
