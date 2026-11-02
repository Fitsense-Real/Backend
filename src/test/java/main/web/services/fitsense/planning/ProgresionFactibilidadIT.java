package main.web.services.fitsense.planning;

import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * FACTIBILIDAD DE LA PROGRESION DE VOLUMEN (DISENO_PROGRESION_Y_REINCORPORACION).
 * Investigacion, no una prueba de comportamiento: solo con
 * {@code -Dfitsense.factibilidad=true}.
 * <p>
 * Para cada sesion real que genera el motor de reglas en un barrido de perfiles,
 * prueba los incrementos minimos discretos de UN ejercicio (+1 rep por serie,
 * +2 reps por serie, +1 serie, +5 s por serie en los de duracion), mide cuanto
 * sube el volumen de la sesion y valida la semana con el validador de
 * produccion (V1-V25, 4D en ENFORCE). No cambia ninguna regla: informa que
 * las bloquea hoy.
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.factibilidad", matches = "true")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
class ProgresionFactibilidadIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanDraftValidator validador;
    @Autowired SessionDurationEstimator estimador;
    @Autowired JdbcTemplate jdbc;

    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final LocalDate SEMANA = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
    static final List<String> GIMNASIO = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine",
            "bench", "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine");
    static final List<String> CASA = List.of("body weight", "dumbbell", "band");
    static final int DIVISOR = 30;
    static final Path SALIDA = Path.of("docs/FitSense/evidencia/progresion");

    /** Un incremento candidato sobre un ejercicio. */
    record Paso(String tipo, int ejercicio, PlanDraft.DraftExercise nuevo, int deltaVolumen) {}

    /** Resultado de un paso: cuanto sube y que lo bloquea. */
    record Fila(String perfil, String objetivo, String nivel, int minutos, String lugar, int sesion, int volumen,
                int duracion, String paso, double deltaPct, boolean dentroDeSeries, Set<String> bloqueos) {}

    @Test
    @DisplayName("barrido: incremento minimo por sesion y que lo bloquea con las reglas vigentes")
    void barrido() throws Exception {
        var filas = new ArrayList<Fila>();
        var sinPlan = new ArrayList<String>();
        for (var objetivo : GoalType.values()) {
            for (var nivel : FitnessLevel.values())
                for (int minutos : new int[]{30, 45, 60})
                    for (var lugar : List.of("GYM", "CASA")) {
                        String nombre = objetivo + "/" + nivel + "/" + minutos + "/" + lugar;
                        try {
                            filas.addAll(analizar(nombre, objetivo, nivel, minutos, lugar));
                        } catch (RuntimeException | AssertionError e) {
                            sinPlan.add(nombre + ": " + e.getMessage());
                        }
                    }
        }
        Files.createDirectories(SALIDA);
        Files.writeString(SALIDA.resolve("factibilidad.csv"), csv(filas));
        Files.writeString(SALIDA.resolve("factibilidad.md"), resumen(filas, sinPlan));
        System.out.println(resumen(filas, sinPlan));
    }

    private List<Fila> analizar(String nombre, GoalType objetivo, FitnessLevel nivel, int minutos, String lugar) {
        long u = usuario(nombre, objetivo, nivel, minutos, lugar);
        long plan = planning.generateWeeklyPlan(u, SEMANA, PlanAdjustment.none(), null, null)
                .orElseThrow(() -> new AssertionError("sin plan"));
        var ctx = planComandos.prepare(new GenerateWeeklyPlanCommand(u, SEMANA, null, true, null, null)).context();
        var semana = semana(plan);
        var base = problemas(semana, ctx);   // la semana generada, tal cual

        var filas = new ArrayList<Fila>();
        for (int i = 0; i < semana.size(); i++) {
            var sesion = semana.get(i);
            int volumen = sesion.exercises().stream().mapToInt(e -> e.volume(DIVISOR)).sum();
            int duracion = estimador.estimateMinutes(sesion, ctx.prescription());
            for (var paso : pasos(sesion)) {
                var ejercicios = new ArrayList<>(sesion.exercises());
                ejercicios.set(paso.ejercicio(), paso.nuevo());
                var borrador = new PlanDraft.DraftWorkout(sesion.scheduledDate(), sesion.focus(), sesion.name(), 0,
                        ejercicios);
                var modificada = new PlanDraft.DraftWorkout(sesion.scheduledDate(), sesion.focus(), sesion.name(),
                        estimador.estimateMinutes(borrador, ctx.prescription()), ejercicios);
                var semanaNueva = new ArrayList<>(semana);
                semanaNueva.set(i, modificada);
                var nuevos = new TreeSet<String>();
                for (var p : problemas(semanaNueva, ctx))
                    if (!base.contains(p)) nuevos.add(clasificar(p));
                boolean dentroDeSeries = paso.nuevo().plannedSets() == null
                        || paso.nuevo().plannedSets() <= CapabilityLimits.SERIES_MAX;
                filas.add(new Fila(nombre, objetivo.name(), nivel.name(), minutos, lugar, i, volumen, duracion,
                        paso.tipo(), volumen == 0 ? 0 : 100.0 * paso.deltaVolumen() / volumen, dentroDeSeries, nuevos));
            }
        }
        return filas;
    }

    /** Los incrementos minimos discretos de UN ejercicio. */
    private static List<Paso> pasos(PlanDraft.DraftWorkout sesion) {
        var pasos = new ArrayList<Paso>();
        for (int j = 0; j < sesion.exercises().size(); j++) {
            var e = sesion.exercises().get(j);
            int antes = e.volume(DIVISOR);
            short series = e.plannedSets() == null ? 1 : e.plannedSets();
            if (e.prescriptionType() == PrescriptionType.SETS_REPS && e.plannedReps() != null) {
                for (int extra : new int[]{1, 2}) {
                    var n = con(e, series, (short) (e.plannedReps() + extra), null);
                    pasos.add(new Paso("+" + extra + " rep/serie", j, n, n.volume(DIVISOR) - antes));
                }
                var s = con(e, (short) (series + 1), e.plannedReps(), null);
                pasos.add(new Paso("+1 serie", j, s, s.volume(DIVISOR) - antes));
            } else if (e.prescriptionType() == PrescriptionType.DURATION && e.plannedDurationSeconds() != null) {
                var d = con(e, series, null, e.plannedDurationSeconds() + 5);
                pasos.add(new Paso("+5 s/serie", j, d, d.volume(DIVISOR) - antes));
                var s = con(e, (short) (series + 1), null, e.plannedDurationSeconds());
                pasos.add(new Paso("+1 serie", j, s, s.volume(DIVISOR) - antes));
            }
        }
        pasos.removeIf(p -> p.deltaVolumen() <= 0);
        return pasos;
    }

    private static PlanDraft.DraftExercise con(PlanDraft.DraftExercise e, short series, Short reps, Integer segundos) {
        return new PlanDraft.DraftExercise(e.exerciseId(), e.prescriptionType(), series, reps,
                segundos, e.targetLoadKg(), e.restSeconds(), e.notes());
    }

    private Set<String> problemas(List<PlanDraft.DraftWorkout> semana, PlanGenerationContext ctx) {
        try {
            validador.validate(new PlanDraft(GenerationSource.RULE_ENGINE, null, "factibilidad", null, "r", semana),
                    ctx, DIVISOR);
            return Set.of();
        } catch (InvalidPlanDraftException e) {
            return new HashSet<>(e.problems());
        }
    }

    /** El motivo, agrupado: la regla y, en V24, el tipo de limite. */
    private static String clasificar(String p) {
        String regla = p.contains(":") ? p.substring(0, p.indexOf(':')) : p;
        if (regla.equals("V24")) {
            if (p.contains(" exige ")) return "V24-dosis";
            if (p.contains("colocaciones")) return "V24-tope";
            if (p.contains("presupuesto")) return "V24-presupuesto";
            return "V24-otro";
        }
        return regla;
    }

    private long usuario(String nombre, GoalType objetivo, FitnessLevel nivel, int minutos, String lugar) {
        String email = "factibilidad" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.now().minusYears(34).minusDays(10),
                BiologicalSex.FEMALE, new BigDecimal("165"), new BigDecimal("70"), null, nivel, objetivo, nombre,
                "GYM".equals(lugar) ? TrainingLocation.GYM : TrainingLocation.HOME, (short) 3,
                List.of((short) 1, (short) 3, (short) 5), (short) minutos, "GYM".equals(lugar) ? GIMNASIO : CASA,
                List.of(), "Ninguna", true, true, true, true, true));
        return u;
    }

    private List<PlanDraft.DraftWorkout> semana(long planId) {
        var sesiones = new ArrayList<PlanDraft.DraftWorkout>();
        for (var f : jdbc.queryForList("SELECT planned_workout_id, scheduled_date, focus_code, workout_name,"
                + " expected_duration_minutes FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date", planId)) {
            long id = ((Number) f.get("planned_workout_id")).longValue();
            var ejercicios = jdbc.query("SELECT exercise_id, prescription_type, planned_sets, planned_reps,"
                            + " planned_duration_seconds, target_load_kg, rest_seconds FROM planned_workout_exercises"
                            + " WHERE planned_workout_id = ? ORDER BY exercise_order",
                    (rs, k) -> new PlanDraft.DraftExercise(rs.getLong(1), PrescriptionType.valueOf(rs.getString(2)),
                            rs.getObject(3, Short.class), rs.getObject(4, Short.class), rs.getObject(5, Integer.class),
                            rs.getBigDecimal(6), rs.getObject(7, Short.class), null), id);
            sesiones.add(new PlanDraft.DraftWorkout(((java.sql.Date) f.get("scheduled_date")).toLocalDate(),
                    WorkoutFocus.valueOf((String) f.get("focus_code")), (String) f.get("workout_name"),
                    ((Number) f.get("expected_duration_minutes")).intValue(), ejercicios));
        }
        return sesiones;
    }

    /** El tope de series aprobado (SERIES_MAX_AMPLIACION) sin depender de su visibilidad. */
    static final class CapabilityLimits {
        static final short SERIES_MAX = main.web.services.fitsense.planning.domain.services
                .CapabilityPrescriptionPolicy.SERIES_MAX_AMPLIACION;
    }

    // ------------------------------------------------------------------ informe

    private static String csv(List<Fila> filas) {
        var sb = new StringBuilder("perfil;objetivo;nivel;minutos;lugar;sesion;volumen;duracion;paso;delta_pct;"
                + "dentro_de_series;bloqueos\n");
        for (var f : filas)
            sb.append(String.join(";", f.perfil(), f.objetivo(), f.nivel(), String.valueOf(f.minutos()), f.lugar(),
                    String.valueOf(f.sesion()), String.valueOf(f.volumen()), String.valueOf(f.duracion()), f.paso(),
                    String.format(Locale.ROOT, "%.2f", f.deltaPct()), String.valueOf(f.dentroDeSeries()),
                    String.join("|", f.bloqueos()))).append('\n');
        return sb.toString();
    }

    private static String resumen(List<Fila> filas, List<String> sinPlan) {
        var sb = new StringBuilder("# Factibilidad de la progresion de volumen (simulacion)\n\n");
        var sesiones = filas.stream().collect(Collectors.groupingBy(f -> f.perfil() + "#" + f.sesion(),
                LinkedHashMap::new, Collectors.toList()));
        sb.append("Perfiles sin plan: ").append(sinPlan.size()).append(sinPlan.isEmpty() ? "" : " " + sinPlan)
                .append("\n\nSesiones analizadas: ").append(sesiones.size()).append("\n\n");

        sb.append("## Por objetivo: incremento minimo y que lo permite\n\n")
                .append("| Objetivo | Sesiones | Paso minimo ≤ 5 % | ≤ 10 % | Algun paso ≤ 10 % valido HOY | ")
                .append("Valido si V24 admitiera la dosis (solo V24-dosis bloquea) | Mediana del paso minimo |\n")
                .append("|---|---:|---:|---:|---:|---:|---:|\n");
        for (var objetivo : GoalType.values()) {
            var delObjetivo = sesiones.values().stream().filter(l -> l.get(0).objetivo().equals(objetivo.name())).toList();
            if (delObjetivo.isEmpty()) continue;
            int n = delObjetivo.size();
            long cinco = delObjetivo.stream().filter(l -> minimo(l) <= 5.0).count();
            long diez = delObjetivo.stream().filter(l -> minimo(l) <= 10.0).count();
            long valido = delObjetivo.stream().filter(l -> l.stream().anyMatch(f -> f.deltaPct() <= 10.0
                    && f.dentroDeSeries() && f.bloqueos().isEmpty())).count();
            long conExtension = delObjetivo.stream().filter(l -> l.stream().anyMatch(f -> f.deltaPct() <= 10.0
                    && f.dentroDeSeries() && (f.bloqueos().isEmpty() || f.bloqueos().equals(Set.of("V24-dosis"))))).count();
            var minimos = delObjetivo.stream().mapToDouble(ProgresionFactibilidadIT::minimo).sorted().toArray();
            sb.append(String.format(Locale.ROOT, "| %s | %d | %s | %s | %s | %s | %.1f %% |\n", objetivo, n,
                    pct(cinco, n), pct(diez, n), pct(valido, n), pct(conExtension, n), minimos[minimos.length / 2]));
        }

        sb.append("\n## Por tipo de paso: cuantas veces lo bloquea cada regla\n\n")
                .append("| Objetivo | Paso | Intentos | Validos | Fuera del rango de series | Bloqueos (regla: veces) |\n")
                .append("|---|---|---:|---:|---:|---|\n");
        var porTipo = filas.stream().collect(Collectors.groupingBy(f -> f.objetivo() + "|" + f.paso(), TreeMap::new,
                Collectors.toList()));
        porTipo.forEach((clave, l) -> {
            var conteo = new TreeMap<String, Integer>();
            l.forEach(f -> f.bloqueos().forEach(b -> conteo.merge(b, 1, Integer::sum)));
            long validos = l.stream().filter(f -> f.bloqueos().isEmpty() && f.dentroDeSeries()).count();
            long fuera = l.stream().filter(f -> !f.dentroDeSeries()).count();
            var partes = clave.split("\\|");
            sb.append(String.format(Locale.ROOT, "| %s | %s | %d | %d | %d | %s |\n", partes[0], partes[1], l.size(),
                    validos, fuera, conteo));
        });

        sb.append("\n## Por objetivo y minutos: paso minimo valido hoy\n\n| Objetivo | 30 min | 45 min | 60 min |\n|---|---:|---:|---:|\n");
        for (var objetivo : GoalType.values()) {
            sb.append("| ").append(objetivo);
            for (int m : new int[]{30, 45, 60}) {
                int mm = m;
                var l = sesiones.values().stream().filter(x -> x.get(0).objetivo().equals(objetivo.name())
                        && x.get(0).minutos() == mm).toList();
                long valido = l.stream().filter(x -> x.stream().anyMatch(f -> f.deltaPct() <= 10.0
                        && f.dentroDeSeries() && f.bloqueos().isEmpty())).count();
                sb.append(" | ").append(l.isEmpty() ? "-" : pct(valido, l.size()));
            }
            sb.append(" |\n");
        }
        return sb.toString();
    }

    private static double minimo(List<Fila> sesion) {
        return sesion.stream().mapToDouble(Fila::deltaPct).min().orElse(Double.MAX_VALUE);
    }

    private static String pct(long a, long n) {
        return String.format(Locale.ROOT, "%d (%.0f %%)", a, n == 0 ? 0.0 : 100.0 * a / n);
    }
}
