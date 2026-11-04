package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.planning.application.internal.commandservices.SafetyExclusionResolver;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.SafetySubstitutionPolicy;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.domain.services.StrictPlanValidation;
import main.web.services.fitsense.planning.domain.services.WeekDerivationBuilder;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * LABORATORIO (opt-in, SIN cambiar produccion): FACTIBILIDAD DE LA RETIRADA LOCALIZADA ante dolor sin sustituto
 * (orden «salida intermedia segura», fase 3). Los mismos 21 perfiles, semana, ids y semillas del laboratorio de 22
 * perfiles (SinSustitutoDolor24CasosIT). Para cada uno:
 * <ol>
 *   <li>el flujo real con la estrategia de {@code -Dfitsense.adaptation.difficulty-guard} (P-02, respaldo, P-03);</li>
 *   <li>APARTE, sin publicar nada: la semana medida derivada (la misma copia que haria el respaldo), con SUST-SEG-1
 *       real y, para lo excluido por dolor que se queda sin sustituto, SOLO ese ejercicio retirado y los minutos de
 *       esa sesion recalculados por el estimador;</li>
 *   <li>esa semana pasa por los validadores REALES: V8C de continuidad (que hoy no conoce la retirada: sus objeciones
 *       a la retirada se separan de las demas) y la validacion estricta completa (V1-V25, V20, V24/4D, V23...).</li>
 * </ol>
 * Escribe {@code factibilidad_retirada.csv} en {@code fitsense.validacion.salida}.
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.validacion.retirada", matches = "true")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("Laboratorio: factibilidad de retirar solo el ejercicio que dolio (21 perfiles, validadores reales)")
class RetiradaLocalizadaFactibilidadIT extends SinSustitutoDolorBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired WeekDerivationBuilder derivaciones;
    @Autowired SafetyExclusionResolver exclusionesResolver;
    @Autowired StrictPlanValidation estricta;

    static final SessionDurationEstimator ESTIMADOR = new SessionDurationEstimator();
    static final LocalDate MED = SinSustitutoDolor24CasosIT.SEMANA_DEL_BARRIDO;

    @Test
    void factibilidad() throws Exception {
        var salida = Path.of(System.getProperty("fitsense.validacion.salida", "target/retirada"));
        Files.createDirectories(salida);
        var csv = new StringBuilder("perfil;objetivo;nivel;dias;minutos;desenlace_actual;sesiones_con_135;"
                + "sesion_senalada;135_en_no_senalada;slot_135;slot_cubierto_en_otra_sesion;ejercicios_antes;"
                + "ejercicios_despues;minutos_antes;minutos_despues;reps_semana_antes;reps_semana_despues;"
                + "seg_semana_antes;seg_semana_despues;v8c_objeta_solo_la_retirada;v8c_otros;estricta;veredicto\n");
        for (var p : SinSustitutoDolor24CasosIT.PERFILES) {
            long objetivoId = Long.parseLong(p.id().substring(1));
            long ultimo = jdbc.queryForObject("SELECT COALESCE(MAX(user_id), 0) FROM users", Long.class);
            while (ultimo < objetivoId - 1)
                ultimo = jdbc.queryForObject("INSERT INTO users (email, password_hash, first_name, consent_granted_at,"
                        + " enrolled_at) VALUES (?, 'x', 'Relleno', now(), now()) RETURNING user_id", Long.class,
                        "relleno" + UUID.randomUUID() + "@pruebas.invalid");
            long u = participante(p.objetivo(), p.nivel(), p.dias(), p.minutos());
            String base = String.join(";", p.id(), p.objetivo().name(), p.nivel().name(), String.valueOf(p.dias()),
                    String.valueOf(p.minutos()));
            long plan;
            try {
                plan = planning.generateWeeklyPlan(u, MED, PlanAdjustment.none(), null, null).orElseThrow();
            } catch (RuntimeException e) {
                csv.append(base).append(";PLAN_INICIAL_NO_VALIDO;-;-;-;-;-;-;-;-;-;-;-;-;-;-;-;-;NO_APLICA\n");
                continue;
            }
            var con135 = jdbc.queryForList("SELECT DISTINCT pw.planned_workout_id FROM planned_workouts pw JOIN"
                    + " planned_workout_exercises pwe ON pwe.planned_workout_id = pw.planned_workout_id WHERE"
                    + " pw.plan_id = ? AND pwe.exercise_id = ? ORDER BY 1", Long.class, plan, EJ);
            ejecutar(u, plan, con135.get(0));
            var nueva = MED.plusWeeks(1);
            ciclo.runFor(u, MED, nueva);
            var v32 = intento(u, nueva);
            var regen = jdbc.queryForList("SELECT outcome, reason_code FROM safe_regeneration_attempts WHERE user_id = ?",
                    u);
            String actual = v32.get("status") + (regen.isEmpty() ? ""
                    : "/P03_" + regen.get(0).get("outcome") + (regen.get(0).get("reason_code") == null ? ""
                    : "_" + regen.get(0).get("reason_code")));

            // La orden real del ciclo (sesiones e intervenciones senaladas, V32).
            var fila = jdbc.queryForMap("SELECT targeted_sessions::text AS s, targeted_interventions::text AS i FROM"
                    + " plan_adaptation_attempts WHERE user_id = ? AND new_week_start_date = ? ORDER BY attempt_id"
                    + " DESC LIMIT 1", u, nueva);
            var ses = parse((String) fila.get("s"));
            var ints = ((String) fila.get("i")).replaceAll("[{}]", "").split(",");
            var dirigidas = new ArrayList<PlanAdjustment.TargetedSession>();
            for (int i = 0; i < ses.size(); i++)
                dirigidas.add(new PlanAdjustment.TargetedSession(ses.get(i), TipoIntervencion.valueOf(ints[i].trim())));
            var orden = new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null, 0.0, 0.0, null, null, null,
                    "laboratorio", null, null, dirigidas);

            var ctx = planComandos.prepare(new GenerateWeeklyPlanCommand(u, nueva, orden, true, null, null)).context();
            var previa = ctx.previousWeek();
            var origenes = new ArrayList<Long>();
            var fechas = new ArrayList<LocalDate>();
            for (var w : previa.workouts()) {
                if (w.plannedWorkoutId() == null || w.scheduledDate() == null) continue;
                origenes.add(w.plannedWorkoutId());
                fechas.add(w.scheduledDate().plusWeeks(1));
            }
            var derivacion = derivaciones.derive(previa, origenes, fechas);
            var elegibles = ctx.availableExercises().stream().collect(Collectors.toMap(CandidateExercise::exerciseId,
                    c -> c, (a, b) -> a, LinkedHashMap::new));
            var excl = exclusionesResolver.resolver(ctx);
            var reparada = SafetySubstitutionPolicy.reparar(derivacion.workouts(), previa, excl, elegibles, ctx,
                    v8c.estrategiaSustitucion());

            // Retirada: SOLO lo excluido por dolor que siguio sin sustituto; minutos de esa sesion recalculados.
            var senaladasDolor = dirigidas.stream().filter(t -> t.intervencion().exigeRevisionDeSeguridad())
                    .map(PlanAdjustment.TargetedSession::plannedWorkoutId).collect(Collectors.toSet());
            var semana = new ArrayList<DerivedWorkout>();
            var exAntes = new ArrayList<String>();
            var exDespues = new ArrayList<String>();
            var minAntes = new ArrayList<String>();
            var minDespues = new ArrayList<String>();
            boolean enNoSenalada = false;
            for (var d : reparada.semana()) {
                var w = d.workout();
                var quedan = w.exercises().stream().filter(e -> !(excl.incluye(e.exerciseId())
                        && excl.motivos().get(e.exerciseId()).contains("dolor"))).toList();
                if (quedan.size() == w.exercises().size()) {
                    semana.add(d);
                    continue;
                }
                if (!senaladasDolor.contains(d.sourcePlannedWorkoutId())) enNoSenalada = true;
                var corta = new PlanDraft.DraftWorkout(w.scheduledDate(), w.focus(), w.name(), 0, quedan);
                int minutos = ESTIMADOR.estimateMinutes(corta, ctx.prescription());
                exAntes.add(d.sourcePlannedWorkoutId() + ":" + w.exercises().size());
                exDespues.add(d.sourcePlannedWorkoutId() + ":" + quedan.size());
                minAntes.add(d.sourcePlannedWorkoutId() + ":" + w.expectedDurationMinutes());
                minDespues.add(d.sourcePlannedWorkoutId() + ":" + minutos);
                semana.add(new DerivedWorkout(d.sourcePlannedWorkoutId(), new PlanDraft.DraftWorkout(w.scheduledDate(),
                        w.focus(), w.name(), minutos, quedan)));
            }
            var informe = v8c.validateContinuidad(new WeekDerivation(semana, derivacion.sourcesWithoutDerived()),
                    previa, elegibles, excl, reparada.sustituciones());
            var deRetirada = informe.problems().stream().filter(x -> x.contains("desaparecen los ejercicios [" + EJ + "]")
                    || x.contains("minutos en vez de")).toList();
            var otrosV8c = informe.problems().stream().filter(x -> !deRetirada.contains(x)).toList();
            var draft = new PlanDraft(GenerationSource.CONSERVED, null, null, null, "laboratorio",
                    semana.stream().map(DerivedWorkout::workout).toList());
            var limpio = new ConservationReport(List.of(), informe.aplicadas(), informe.sinEfecto(),
                    informe.conservadas(), informe.fechasConDuracionReducible(), informe.sustituciones(),
                    informe.progresadas(), informe.pasos(), informe.dosisAutorizadas(), informe.pasosBloqueados());
            var problemasEstricta = estricta.revisar(draft, ctx, limpio);

            // Cobertura del slot del 135 en el resto de la semana publicada.
            var ficha = excl.originales().get(EJ);
            var slot = ficha == null ? Optional.<MandatorySlot>empty() : MandatorySlot.of(ficha);
            var catalogo = new HashMap<Long, CandidateExercise>(elegibles);
            boolean cubierto = slot.isPresent() && semana.stream().flatMap(d -> d.workout().exercises().stream())
                    .map(e -> catalogo.get(e.exerciseId())).filter(Objects::nonNull)
                    .anyMatch(c -> MandatorySlot.of(c).equals(slot));

            long[] antes = trabajo(previa.prescriptions().stream().map(x -> new long[]{n(x.sets()), n(x.reps()),
                    n(x.durationSeconds())}).toList());
            long[] despues = trabajo(semana.stream().flatMap(d -> d.workout().exercises().stream()).map(e ->
                    new long[]{n(e.plannedSets()), n(e.plannedReps()), n(e.plannedDurationSeconds())}).toList());
            boolean publicable = !enNoSenalada && otrosV8c.isEmpty() && problemasEstricta.isEmpty()
                    && despues[0] <= antes[0] && despues[1] <= antes[1];
            String veredicto = publicable ? "VALIDA"
                    : enNoSenalada ? "RECHAZADA:el_135_tambien_en_una_sesion_no_senalada"
                    : !problemasEstricta.isEmpty() ? "RECHAZADA:" + problemasEstricta.stream()
                    .map(x -> x.split(":")[0]).distinct().collect(Collectors.joining("+"))
                    : !otrosV8c.isEmpty() ? "RECHAZADA:V8C_otros" : "RECHAZADA:trabajo";
            csv.append(String.join(";", base, actual, String.valueOf(con135.size()),
                    senaladasDolor.toString(), String.valueOf(enNoSenalada), slot.map(Enum::name).orElse("relleno"),
                    String.valueOf(cubierto), exAntes.toString(), exDespues.toString(), minAntes.toString(),
                    minDespues.toString(), String.valueOf(antes[0]), String.valueOf(despues[0]),
                    String.valueOf(antes[1]), String.valueOf(despues[1]), String.valueOf(!deRetirada.isEmpty()),
                    limpiar(otrosV8c), limpiar(problemasEstricta), veredicto)).append('\n');
        }
        Files.writeString(salida.resolve("factibilidad_retirada.csv"), csv.toString(), StandardCharsets.UTF_8);
        System.out.println(csv);
    }

    static long n(Number x) {
        return x == null ? 0 : x.longValue();
    }

    /** {repeticiones totales (series x reps), segundos totales (series x segundos)}. */
    static long[] trabajo(List<long[]> filas) {
        long reps = 0, seg = 0;
        for (var f : filas) {
            reps += f[0] * f[1];
            seg += f[0] * f[2];
        }
        return new long[]{reps, seg};
    }

    static List<Long> parse(String pg) {
        var s = pg.replaceAll("[{}]", "").trim();
        if (s.isEmpty()) return List.of();
        return Arrays.stream(s.split(",")).map(String::trim).map(Long::valueOf).toList();
    }

    static String limpiar(List<String> l) {
        return l.isEmpty() ? "-" : String.join(" | ", l).replace(';', ',').replace('\n', ' ');
    }
}
