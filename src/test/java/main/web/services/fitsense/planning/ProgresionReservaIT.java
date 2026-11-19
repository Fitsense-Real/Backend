package main.web.services.fitsense.planning;

import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.DoseEnforcement;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.domain.services.SlotCoverageEnforcement;
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
import java.util.function.Predicate;

/**
 * REVISION PUNTUAL DE LA v0.2 DE PROGRESION: reserva de volumen, impacto por
 * ejercicio, rangos propuestos de V24 y efecto de activar V24 por capacidades
 * en P2-D. Investigacion: solo con {@code -Dfitsense.factibilidad=true}.
 * <p>
 * Sesiones reales del motor de reglas (72 perfiles, 216 sesiones). La
 * extension de V24 se SIMULA: un problema de V24 sobre un ejercicio cuya dosis
 * nueva esta dentro del rango propuesto (D-P3) se trata como autorizado; todo
 * lo demas lo decide el validador de produccion tal cual.
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.factibilidad", matches = "true")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
class ProgresionReservaIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanDraftValidator validador;            // el de la generacion: V24 por capacidades en ENFORCE
    @Autowired SessionDurationEstimator estimador;
    @Autowired JdbcTemplate jdbc;

    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final LocalDate SEMANA = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
    static final List<String> GIMNASIO = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine",
            "bench", "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine");
    static final List<String> CASA = List.of("body weight", "dumbbell", "band");
    static final int DIVISOR = 30;
    static final double TECHO = 1.10;          // D-P5 provisional
    static final double PASO_ORDINARIO = 5.0;  // progression_increase_pct
    static final short SERIES_MAX = main.web.services.fitsense.planning.domain.services
            .CapabilityPrescriptionPolicy.SERIES_MAX_AMPLIACION;

    record Rango(short seriesMin, short seriesMax, short repsMin, short repsMax) {
        boolean contiene(PlanDraft.DraftExercise e) {
            if (e.prescriptionType() != PrescriptionType.SETS_REPS || e.plannedReps() == null) return false;
            short s = e.plannedSets() == null ? 1 : e.plannedSets();
            return s >= seriesMin && s <= seriesMax && e.plannedReps() >= repsMin && e.plannedReps() <= repsMax;
        }
    }

    record Paso(int ejercicio, PlanDraft.DraftExercise nuevo, double pctEjercicio, double pctSesion, String tipo) {}

    /** Resultado de una sesion en cada politica. */
    record Res(String objetivo, int minutos, boolean conRelleno, int vBase, int vRet,
               int pasosP, String bloqueoP, double pctEjercicioMax, double pctSesionMax,
               boolean ingenuaBloqueada, int pasosR1, boolean r1Ok, String r1Motivo,
               int pasosR2, boolean r2Ok, String r2Motivo, int totalR2,
               boolean reduccionRechazadaConCapacidades, boolean reduccionRechazadaEnP2d,
               boolean cortaBajoPiso, int pasosR2SinD8) {}

    @Test
    @DisplayName("reserva de volumen, impacto por ejercicio y V24 por capacidades en P2-D")
    void revision() throws Exception {
        var p2d = new PlanDraftValidator(estimador, SlotCoverageEnforcement.parse("ENFORCE_AI_ONLY"),
                DoseEnforcement.ENFORCE);   // como StrictValidationAdapter: capacidades OFF
        var resultados = new ArrayList<Res>();
        var fallos = new ArrayList<String>();
        for (var objetivo : GoalType.values())
            for (var nivel : FitnessLevel.values())
                for (int minutos : new int[]{30, 45, 60})
                    for (var lugar : List.of("GYM", "CASA")) {
                        try {
                            resultados.addAll(perfil(objetivo, nivel, minutos, lugar, p2d));
                        } catch (RuntimeException | AssertionError e) {
                            fallos.add(objetivo + "/" + nivel + "/" + minutos + "/" + lugar + ": " + e.getMessage());
                        }
                    }
        var salida = Path.of("docs/FitSense/evidencia/progresion");
        Files.createDirectories(salida);
        var informe = informe(resultados, fallos);
        Files.writeString(salida.resolve("reserva_y_v24.md"), informe);
        System.out.println(informe);
    }

    private List<Res> perfil(GoalType objetivo, FitnessLevel nivel, int minutos, String lugar, PlanDraftValidator p2d) {
        long u = usuario(objetivo, nivel, minutos, lugar);
        long plan = planning.generateWeeklyPlan(u, SEMANA, PlanAdjustment.none(), null, null).orElseThrow();
        var ctx = planComandos.prepare(new GenerateWeeklyPlanCommand(u, SEMANA, null, true, null, null)).context();
        var candidatos = new HashMap<Long, CandidateExercise>();
        ctx.availableExercises().forEach(c -> candidatos.put(c.exerciseId(), c));
        var semana = semana(plan);
        var base = problemas(validador, semana, ctx);
        boolean gm = objetivo == GoalType.GAIN_MUSCLE;

        var salida = new ArrayList<Res>();
        for (int i = 0; i < semana.size(); i++) {
            var s0 = semana.get(i);
            int vBase = volumen(s0.exercises());
            var rangos = new ArrayList<Rango>();
            for (var e : s0.exercises()) rangos.add(rango(e, candidatos.get(e.exerciseId()), gm));
            int iSesion = i;

            // P · progresion ordinaria sin adaptaciones, hasta el techo o el primer bloqueo.
            var p = progresar(semana, iSesion, s0.exercises(), rangos, (int) Math.floor(vBase * TECHO), ctx, base,
                    Set.of(), candidatos);

            // R · un relleno retirado (el ultimo sin slot de 4C), si lo hay.
            int jRet = -1;
            for (int j = s0.exercises().size() - 1; j >= 0; j--) {
                var c = candidatos.get(s0.exercises().get(j).exerciseId());
                if (c != null && MandatorySlot.of(c).isEmpty()) { jRet = j; break; }
            }
            boolean conRelleno = jRet >= 0;
            int vRet = 0, pasosR1 = 0, pasosR2 = 0, totalR2 = 0;
            boolean ingenua = false, r1Ok = false, r2Ok = false, bajoPiso = false;
            int pasosSinD8 = 0;
            String r1Motivo = "-", r2Motivo = "-";
            if (conRelleno) {
                var retirado = s0.exercises().get(jRet);
                vRet = retirado.volume(DIVISOR);
                var conservados = new ArrayList<>(s0.exercises());
                conservados.remove(jRet);
                var rangosC = new ArrayList<>(rangos);
                rangosC.remove(jRet);
                var fechaCorta = Set.of(s0.scheduledDate());   // D8: la acortada queda exenta de V20

                // Ingenua: progresar hasta el techo sin reservar; reincorporar exigiendo total <= referencia.
                var sinReserva = progresar(semana, iSesion, conservados, rangosC, (int) Math.floor(vBase * TECHO), ctx,
                        base, fechaCorta, candidatos);
                ingenua = sinReserva.pasos() > 0 && volumen(sinReserva.ejercicios()) + vRet > vBase;

                // R1: reserva estricta. Conservados <= referencia - retirado; total final <= referencia.
                var r1 = progresar(semana, iSesion, conservados, rangosC, vBase - vRet, ctx, base, fechaCorta, candidatos);
                pasosR1 = r1.pasos();
                var fin1 = reincorporar(semana, iSesion, r1.ejercicios(), retirado, jRet, ctx, base, candidatos);
                r1Ok = fin1 == null && volumen(r1.ejercicios()) + vRet <= vBase;
                r1Motivo = fin1 == null ? "-" : fin1;

                // R2: reserva dentro del techo. Conservados <= techo - retirado; total final <= techo.
                int techo = (int) Math.floor(vBase * TECHO);
                var r2 = progresar(semana, iSesion, conservados, rangosC, techo - vRet, ctx, base, fechaCorta, candidatos);
                pasosR2 = r2.pasos();
                // D-P7: la acortada, bajo el piso de V20? y cuantos pasos se dan SIN heredar la exencion.
                var corta = conSesion(semana, iSesion, conservados, ctx).get(iSesion);
                bajoPiso = corta.expectedDurationMinutes() < PlanDraftValidator.minimumSessionMinutes(ctx);
                pasosSinD8 = progresar(semana, iSesion, conservados, rangosC, techo - vRet, ctx, base, Set.of(),
                        candidatos).pasos();
                var fin2 = reincorporar(semana, iSesion, r2.ejercicios(), retirado, jRet, ctx, base, candidatos);
                totalR2 = volumen(r2.ejercicios()) + vRet;
                r2Ok = fin2 == null && totalR2 <= techo;
                r2Motivo = fin2 == null ? "-" : fin2;
            }

            // Reduccion por fatiga (-2 reps a un ejercicio con >= 8): con capacidades ON y con el validador de P2-D.
            boolean redCap = false, redP2d = false;
            for (int j = 0; j < s0.exercises().size(); j++) {
                var e = s0.exercises().get(j);
                if (e.prescriptionType() != PrescriptionType.SETS_REPS || e.plannedReps() == null || e.plannedReps() < 8)
                    continue;
                var lista = new ArrayList<>(s0.exercises());
                lista.set(j, con(e, e.plannedSets(), (short) (e.plannedReps() - 2)));
                var semanaR = conSesion(semana, iSesion, lista, ctx);
                redCap = problemas(validador, semanaR, ctx).stream().anyMatch(x -> !base.contains(x) && x.startsWith("V24"));
                redP2d = problemas(p2d, semanaR, ctx).stream().anyMatch(x -> !base.contains(x) && x.startsWith("V24"));
                break;
            }
            salida.add(new Res(objetivo.name(), minutos, conRelleno, vBase, vRet, p.pasos(), p.bloqueo(),
                    p.pctEjercicioMax(), p.pctSesionMax(), ingenua, pasosR1, r1Ok, r1Motivo, pasosR2, r2Ok, r2Motivo,
                    totalR2, redCap, redP2d, bajoPiso, pasosSinD8));
        }
        return salida;
    }

    /** Rango propuesto (D-P3) a partir de la dosis inicial exacta. */
    private static Rango rango(PlanDraft.DraftExercise e, CandidateExercise c, boolean gm) {
        short s = e.plannedSets() == null ? 1 : e.plannedSets();
        short r = e.plannedReps() == null ? 0 : e.plannedReps();
        boolean estructural = gm && c != null && MandatorySlot.of(c).isPresent();
        short sMax = estructural ? s : (short) Math.max(s, SERIES_MAX);
        return new Rango(s, sMax, r, (short) Math.min(30, r + 2));
    }

    record Progreso(List<PlanDraft.DraftExercise> ejercicios, int pasos, String bloqueo, double pctEjercicioMax,
                    double pctSesionMax) {}

    /**
     * Pasos sucesivos de UN ejercicio: el de menor cambio relativo en el ejercicio (repeticiones antes que
     * series), con el paso de la sesion <= 5 %, sin pasar del techo, y la semana valida.
     */
    private Progreso progresar(List<PlanDraft.DraftWorkout> semana, int i, List<PlanDraft.DraftExercise> inicial,
                               List<Rango> rangos, int techo, PlanGenerationContext ctx, Set<String> base,
                               Set<LocalDate> exentasV20, Map<Long, CandidateExercise> candidatos) {
        var actual = new ArrayList<>(inicial);
        int pasos = 0;
        double maxEj = 0, maxSes = 0;
        String bloqueo = "techo";
        for (int vuelta = 0; vuelta < 40; vuelta++) {
            int v = volumen(actual);
            var opciones = new ArrayList<Paso>();
            for (int j = 0; j < actual.size(); j++) {
                var e = actual.get(j);
                if (e.prescriptionType() != PrescriptionType.SETS_REPS || e.plannedReps() == null) continue;
                var r = rangos.get(j);
                short s = e.plannedSets() == null ? 1 : e.plannedSets();
                if (e.plannedReps() < r.repsMax()) {
                    var n = con(e, s, (short) (e.plannedReps() + 1));
                    opciones.add(new Paso(j, n, 100.0 / e.plannedReps(), 100.0 * (n.volume(DIVISOR) - e.volume(DIVISOR)) / v, "+1 rep"));
                } else if (s < r.seriesMax()) {
                    var n = con(e, (short) (s + 1), e.plannedReps());
                    opciones.add(new Paso(j, n, 100.0 / s, 100.0 * (n.volume(DIVISOR) - e.volume(DIVISOR)) / v, "+1 serie"));
                }
            }
            opciones.sort(Comparator.comparingDouble(Paso::pctEjercicio).thenComparingDouble(Paso::pctSesion));
            Paso elegido = null;
            String motivo = opciones.isEmpty() ? "sin margen en el rango" : null;
            for (var o : opciones) {
                if (o.pctSesion() > PASO_ORDINARIO) { motivo = "paso > 5 %"; continue; }
                var prueba = new ArrayList<>(actual);
                prueba.set(o.ejercicio(), o.nuevo());
                if (volumen(prueba) > techo) { motivo = "techo"; continue; }
                var nuevos = nuevos(semana, i, prueba, ctx, base, exentasV20, autorizados(prueba, inicial, rangos));
                if (!nuevos.isEmpty()) { motivo = String.join(",", nuevos); continue; }
                elegido = o;
                break;
            }
            if (elegido == null) { bloqueo = motivo == null ? "techo" : motivo; break; }
            actual.set(elegido.ejercicio(), elegido.nuevo());
            pasos++;
            maxEj = Math.max(maxEj, elegido.pctEjercicio());
            maxSes = Math.max(maxSes, elegido.pctSesion());
        }
        return new Progreso(actual, pasos, bloqueo, maxEj, maxSes);
    }

    /** Reincorpora el retirado con su dosis inicial exacta, en su posicion. Null si la semana es valida. */
    private String reincorporar(List<PlanDraft.DraftWorkout> semana, int i, List<PlanDraft.DraftExercise> conservados,
                                PlanDraft.DraftExercise retirado, int j, PlanGenerationContext ctx, Set<String> base,
                                Map<Long, CandidateExercise> candidatos) {
        var lista = new ArrayList<>(conservados);
        lista.add(Math.min(j, lista.size()), retirado);
        var original = semana.get(i).exercises();
        var permitidos = new HashSet<Long>();
        for (var e : lista)
            if (original.stream().noneMatch(o -> o.equals(e))) permitidos.add(e.exerciseId());
        var nuevos = nuevos(semana, i, lista, ctx, base, Set.of(), permitidos);
        return nuevos.isEmpty() ? null : String.join(",", nuevos);
    }

    /** Los ejercicios cuya dosis cambio dentro de su rango: su problema de V24-dosis se trata como autorizado. */
    private static Set<Long> autorizados(List<PlanDraft.DraftExercise> actual, List<PlanDraft.DraftExercise> inicial,
                                         List<Rango> rangos) {
        var ids = new HashSet<Long>();
        for (int j = 0; j < actual.size(); j++)
            if (!actual.get(j).equals(inicial.get(j)) && rangos.get(j).contiene(actual.get(j)))
                ids.add(actual.get(j).exerciseId());
        return ids;
    }

    private Set<String> nuevos(List<PlanDraft.DraftWorkout> semana, int i, List<PlanDraft.DraftExercise> lista,
                               PlanGenerationContext ctx, Set<String> base, Set<LocalDate> exentasV20,
                               Set<Long> autorizados) {
        var fecha = semana.get(i).scheduledDate();
        var res = new TreeSet<String>();
        Predicate<String> autorizado = x -> x.startsWith("V24: el ejercicio ")
                && autorizados.stream().anyMatch(id -> x.startsWith("V24: el ejercicio " + id + " "));
        for (var x : problemas(validador, conSesion(semana, i, lista, ctx), ctx)) {
            if (base.contains(x) || autorizado.test(x)) continue;
            if (x.startsWith("V20") && exentasV20.contains(fecha) && x.contains(fecha.toString())) continue;
            res.add(clasificar(x));
        }
        return res;
    }

    private List<PlanDraft.DraftWorkout> conSesion(List<PlanDraft.DraftWorkout> semana, int i,
                                                   List<PlanDraft.DraftExercise> lista, PlanGenerationContext ctx) {
        var s = semana.get(i);
        var borrador = new PlanDraft.DraftWorkout(s.scheduledDate(), s.focus(), s.name(), 0, lista);
        var nueva = new ArrayList<>(semana);
        nueva.set(i, new PlanDraft.DraftWorkout(s.scheduledDate(), s.focus(), s.name(),
                estimador.estimateMinutes(borrador, ctx.prescription()), lista));
        return nueva;
    }

    private static Set<String> problemas(PlanDraftValidator v, List<PlanDraft.DraftWorkout> semana,
                                         PlanGenerationContext ctx) {
        try {
            v.validate(new PlanDraft(GenerationSource.RULE_ENGINE, null, "reserva", null, "r", semana), ctx, DIVISOR);
            return Set.of();
        } catch (InvalidPlanDraftException e) {
            return new HashSet<>(e.problems());
        }
    }

    private static String clasificar(String p) {
        String regla = p.contains(":") ? p.substring(0, p.indexOf(':')) : p;
        if (regla.equals("V24")) {
            if (p.contains("presupuesto")) return "V24-presupuesto";
            if (p.contains("colocaciones")) return "V24-tope";
            return "V24-dosis";
        }
        return regla;
    }

    private static int volumen(List<PlanDraft.DraftExercise> l) {
        return l.stream().mapToInt(e -> e.volume(DIVISOR)).sum();
    }

    private static PlanDraft.DraftExercise con(PlanDraft.DraftExercise e, Short series, Short reps) {
        return new PlanDraft.DraftExercise(e.exerciseId(), e.prescriptionType(), series, reps,
                e.plannedDurationSeconds(), e.targetLoadKg(), e.restSeconds(), e.notes());
    }

    private long usuario(GoalType objetivo, FitnessLevel nivel, int minutos, String lugar) {
        String email = "reserva" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.now().minusYears(34).minusDays(10),
                BiologicalSex.FEMALE, new BigDecimal("165"), new BigDecimal("70"), null, nivel, objetivo, "reserva",
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

    // ------------------------------------------------------------------ informe

    private static String informe(List<Res> rs, List<String> fallos) {
        var sb = new StringBuilder("# Revision puntual de la progresion: reserva, impacto por ejercicio y V24\n\n");
        sb.append("Sesiones: ").append(rs.size()).append(" · perfiles sin plan: ").append(fallos.size())
                .append(fallos.isEmpty() ? "" : " " + fallos).append("\n\n");

        sb.append("## 1. Progresion ordinaria (sin adaptaciones), techo base x 1,10, paso <= 5 %, V24 ampliada (D-P3)\n\n")
                .append("| Objetivo | Sesiones | Con >= 1 paso | Pasos medios hasta parar | Llegan al techo | Max. cambio en el ejercicio | Max. paso en la sesion | Motivos de parada |\n")
                .append("|---|---:|---:|---:|---:|---:|---:|---|\n");
        for (var o : GoalType.values()) {
            var l = rs.stream().filter(r -> r.objetivo().equals(o.name())).toList();
            if (l.isEmpty()) continue;
            var motivos = new TreeMap<String, Integer>();
            l.forEach(r -> motivos.merge(r.bloqueoP(), 1, Integer::sum));
            sb.append(String.format(Locale.ROOT, "| %s | %d | %s | %.1f | %s | %.1f %% | %.1f %% | %s |\n", o, l.size(),
                    pct(l.stream().filter(r -> r.pasosP() > 0).count(), l.size()),
                    l.stream().mapToInt(Res::pasosP).average().orElse(0),
                    pct(l.stream().filter(r -> r.bloqueoP().equals("techo")).count(), l.size()),
                    l.stream().mapToDouble(Res::pctEjercicioMax).max().orElse(0),
                    l.stream().mapToDouble(Res::pctSesionMax).max().orElse(0), motivos));
        }

        sb.append("\n## 2. Sesiones con un relleno retirado: reserva de volumen\n\n")
                .append("| Objetivo | Con relleno | Ingenua: progresar y luego reincorporar <= referencia BLOQUEADA | ")
                .append("R1 (reserva estricta): pasos medios / reincorporacion valida | R2 (reserva en el techo): pasos medios / reincorporacion valida | Motivos de fallo R2 |\n")
                .append("|---|---:|---:|---|---|---|\n");
        for (var o : GoalType.values()) {
            var l = rs.stream().filter(r -> r.objetivo().equals(o.name()) && r.conRelleno()).toList();
            if (l.isEmpty()) { sb.append("| ").append(o).append(" | 0 | - | - | - | - |\n"); continue; }
            var motivos = new TreeMap<String, Integer>();
            l.stream().filter(r -> !r.r2Ok()).forEach(r -> motivos.merge(r.r2Motivo(), 1, Integer::sum));
            sb.append(String.format(Locale.ROOT, "| %s | %d | %s | %.1f / %s | %.1f / %s | %s |\n", o, l.size(),
                    pct(l.stream().filter(Res::ingenuaBloqueada).count(), l.size()),
                    l.stream().mapToInt(Res::pasosR1).average().orElse(0),
                    pct(l.stream().filter(Res::r1Ok).count(), l.size()),
                    l.stream().mapToInt(Res::pasosR2).average().orElse(0),
                    pct(l.stream().filter(Res::r2Ok).count(), l.size()), motivos));
        }

        sb.append("\n## 2b. D-P7: progresion de una sesion acortada bajo el piso de V20\n\n")
                .append("| Objetivo | Acortadas | Bajo el piso de V20 | Pasos medios con D-P7 (herencia de D8 en el paso) | Pasos medios sin D-P7 | Acortadas bajo el piso que no progresan sin D-P7 |\n")
                .append("|---|---:|---:|---:|---:|---:|\n");
        for (var o : GoalType.values()) {
            var l = rs.stream().filter(r -> r.objetivo().equals(o.name()) && r.conRelleno()).toList();
            if (l.isEmpty()) continue;
            var bajo = l.stream().filter(Res::cortaBajoPiso).toList();
            sb.append(String.format(Locale.ROOT, "| %s | %d | %s | %.1f | %.1f | %s |\n", o, l.size(),
                    pct(bajo.size(), l.size()),
                    bajo.stream().mapToInt(Res::pasosR2).average().orElse(0),
                    bajo.stream().mapToInt(Res::pasosR2SinD8).average().orElse(0),
                    pct(bajo.stream().filter(r -> r.pasosR2() > 0 && r.pasosR2SinD8() == 0).count(), bajo.size())));
        }

        sb.append("\n## 3. Reduccion por fatiga (-2 repeticiones) frente a V24 por capacidades\n\n")
                .append("| Objetivo | Sesiones probadas | Rechazada por V24 con capacidades ON (generacion) | Rechazada por V24 en P2-D hoy (capacidades OFF) |\n")
                .append("|---|---:|---:|---:|\n");
        for (var o : GoalType.values()) {
            var l = rs.stream().filter(r -> r.objetivo().equals(o.name())).toList();
            sb.append(String.format(Locale.ROOT, "| %s | %d | %s | %s |\n", o, l.size(),
                    pct(l.stream().filter(Res::reduccionRechazadaConCapacidades).count(), l.size()),
                    pct(l.stream().filter(Res::reduccionRechazadaEnP2d).count(), l.size())));
        }
        return sb.toString();
    }

    private static String pct(long a, long n) {
        return String.format(Locale.ROOT, "%d (%.0f %%)", a, n == 0 ? 0.0 : 100.0 * a / n);
    }
}
