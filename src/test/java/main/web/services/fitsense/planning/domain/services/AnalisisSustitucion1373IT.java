package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.RelojPruebas;
import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.application.internal.commandservices.PlanGenerationPipeline;
import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalCatalogService;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * D3 (orden de cierre del respaldo, 02/11/2026): POR QUE SUST-SEG-1 NO SUSTITUYE EL 1373. Laboratorio opt-in
 * ({@code -Dfitsense.analisis.1373=true}), solo lectura sobre su propia base efimera, sin Replicate.
 * <p>
 * Reproduce el caso C de {@code RespaldoConDolorIT} (mismo perfil, misma semana, IA que siempre falla) y, con el
 * contexto real de la semana nueva, aplica UNO A UNO los filtros de {@link SafetySubstitutionPolicy#elegir}: cuantos
 * candidatos deja cada uno, cuales fallan SOLO uno (los «casi») y lo mismo sobre el catalogo activo entero (sin el
 * filtro del perfil), para separar una limitacion del catalogo de una de la politica. Salida en
 * {@code target/analisis-1373/}.
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.analisis.1373", matches = "true")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.progression.enabled=false"
})
class AnalisisSustitucion1373IT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoBean ReplicateSessionFixGenerator ia;
    @MockitoBean main.web.services.fitsense.planning.infrastructure.scheduling.OverdueWorkoutScheduler vencidas;

    @Autowired WeeklyCycleService ciclo;
    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyTrainingPlanCommandService comandos;
    @Autowired ExternalCatalogService catalogo;
    @Autowired UserProfileCommandService perfiles;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlanGenerationPipeline pipeline;
    @Autowired PlanDraftValidator validador;

    static final LocalDate S1 = RelojPruebas.SEMANA_REFERENCIA;
    static final long OBJETIVO = 1373L;
    static final List<String> GIMNASIO = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine",
            "bench", "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine");
    static final Path SALIDA = Path.of("target", "analisis-1373");

    record Filtro(String nombre, Predicate<CandidateExercise> pasa) {}

    @BeforeEach
    void iaQueSiempreFalla() {
        reset(ia);
        when(ia.isEnabled()).thenReturn(true);
        when(ia.modelName()).thenReturn("simulado");
        when(ia.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{}", "salida-simulada"));
        when(ia.interpretar(anyString(), any())).thenThrow(new InvalidPlanDraftException(List.of("PRUEBA: rechazo")));
    }

    @Test
    void porQueNoHaySustituto() throws Exception {

        // Todos los participantes deterministas (hasta 40) cuyo plan lleva el 1373, hasta 5 casos: cada uno con su
        // desenlace real y el embudo de SUST-SEG-1 con su contexto.
        var md = new StringBuilder("# Analisis de sustitucion SUST-SEG-1 del ejercicio 1373\n\n");
        var csv = new StringBuilder("caso;universo;exercise_id;nombre;zona;equipo;dificultad;tipo;patron;slot;"
                + "complemento;objetivo_muscular;falla\n");
        int casos = 0, sinSustituto = 0;
        for (int i = 1; i <= 40 && casos < 5; i++) {
            long u = participante(i);
            long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
            long sesion = -1;
            for (long w : sesionesDe(plan))
                if (ejerciciosDe(w).contains(OBJETIVO)) { sesion = w; break; }
            if (sesion < 0) continue;
            casos++;
            var ses = jdbc.queryForMap("SELECT scheduled_date, focus_code FROM planned_workouts WHERE planned_workout_id = ?", sesion);
            var enfoque = WorkoutFocus.valueOf((String) ses.get("focus_code"));
            var fecha = ((java.sql.Date) ses.get("scheduled_date")).toLocalDate();
            var enSesion = new HashSet<>(ejerciciosDe(sesion));

            for (long w : sesionesDe(plan)) hacer(u, w, w == sesion ? OBJETIVO : -1);
            var publicado = ciclo.runFor(u, S1, S1.plusWeeks(1));
            var ev = jdbc.queryForMap("SELECT status, generation_source, conservation_report::text AS informe,"
                    + " rejection_reason FROM plan_adaptation_attempts WHERE user_id = ? ORDER BY attempt_id DESC LIMIT 1", u);

            // El contexto real de la semana nueva (replaceExisting: solo se prepara, no se persiste nada).
            var contexto = comandos.prepare(new GenerateWeeklyPlanCommand(u, S1.plusWeeks(1), PlanAdjustment.none(),
                    true, null, null)).context();
            String objetivo = contexto.profile().goalType();
            var elegibles = contexto.availableExercises().stream()
                    .collect(Collectors.toMap(CandidateExercise::exerciseId, c -> c, (a, b) -> a, LinkedHashMap::new));
            var original = catalogo.fetchAsCandidates(Set.of(OBJETIVO)).get(OBJETIVO);
            assertNotNull(original);
            var nueva = fecha.plusWeeks(1);
            var contiguos = new HashSet<Long>();
            for (long w : sesionesDe(plan)) {
                var f = jdbc.queryForObject("SELECT scheduled_date FROM planned_workouts WHERE planned_workout_id = ?",
                        java.sql.Date.class, w).toLocalDate().plusWeeks(1);
                if (f.equals(nueva.minusDays(1)) || f.equals(nueva.plusDays(1))) contiguos.addAll(ejerciciosDe(w));
            }
            var slot = MandatorySlot.of(original);
            boolean complemento = CatalogEligibilityPolicy.esComplemento(original, objetivo);
            var filtros = List.of(
                    new Filtro("F1 distinto del original", c -> !c.exerciseId().equals(original.exerciseId())),
                    new Filtro("F2 misma clase de slot (" + slot.map(Enum::name).orElse("relleno") + ")",
                            c -> MandatorySlot.of(c).equals(slot)),
                    new Filtro("F3 mismo tipo (" + original.defaultPrescription() + ")",
                            c -> c.defaultPrescription() == original.defaultPrescription()),
                    new Filtro("F4 misma condicion de complemento (" + complemento + ")",
                            c -> CatalogEligibilityPolicy.esComplemento(c, objetivo) == complemento),
                    new Filtro("F5 zona que admite " + enfoque + " o la misma (" + original.bodyPartCode() + ")",
                            c -> enfoque.accepts(c.bodyPartCode()) || Objects.equals(c.bodyPartCode(), original.bodyPartCode())),
                    new Filtro("F6 brazo admitido (V22)", c -> admiteBrazo(enfoque, c)),
                    new Filtro("F7 no en revision", c -> !GoalFeasibilityPolicy.enRevision(c)),
                    new Filtro("F8 no en la sesion ni en un dia contiguo",
                            c -> !enSesion.contains(c.exerciseId()) && !contiguos.contains(c.exerciseId())));
            var elegido = SafetySubstitutionPolicy.elegir(original, enfoque, elegibles, enSesion, contiguos, contexto);
            if (elegido.isEmpty()) sinSustituto++;

            String caso = "C" + casos;
            md.append("## Caso ").append(caso).append(": usuario ").append(u).append(", sesion ").append(fecha)
                    .append(" (").append(enfoque).append(")\n\n");
            md.append("- Desenlace del ciclo: publicado=").append(publicado.isPresent()).append(", estado V32=")
                    .append(ev.get("status")).append(", origen=").append(ev.get("generation_source")).append("\n");
            md.append("- Motivo V32: ").append(ev.get("rejection_reason")).append("\n");
            md.append("- Sustituciones V32: ").append(sustituciones(String.valueOf(ev.get("informe")))).append("\n");
            md.append("- Ejercicios de la sesion: ").append(enSesion).append("; contiguos: ").append(contiguos).append("\n");
            md.append("- `SafetySubstitutionPolicy.elegir` con el contexto real: ")
                    .append(elegido.map(c -> "**" + describir(c, objetivo) + "**").orElse("**sin sustituto**")).append("\n\n");
            // D2: que habrian hecho P-03 nivel 2 y la variante localizada en este caso (prototipo de medicion).
            var preparada = comandos.prepare(new GenerateWeeklyPlanCommand(u, S1.plusWeeks(1), PlanAdjustment.none(),
                    true, null, null));
            var medidasCaso = medidas(plan);
            try {
                var regen = pipeline.runDeterministic(preparada.context(), preparada.divisor()).draft();
                long[] tMed = trabajo(medidasCaso), tN2 = trabajo(regen.workouts());
                var mixta = new ArrayList<PlanDraft.DraftWorkout>();
                var nuevaSes = porFecha(regen, nueva);
                for (var m : medidasCaso) mixta.add(m.scheduledDate().equals(nueva) && nuevaSes != null ? nuevaSes : m);
                long[] tMix = trabajo(mixta);
                long[] tSesMed = trabajo(medidasCaso.stream().filter(m -> m.scheduledDate().equals(nueva)).toList());
                long[] tSesNueva = trabajo(nuevaSes == null ? List.of() : List.of(nuevaSes));
                String problemas = "ninguno";
                try {
                    validador.validate(new PlanDraft(GenerationSource.RULE_ENGINE, null, null, null, "prototipo", mixta),
                            preparada.context(), preparada.divisor());
                } catch (InvalidPlanDraftException e) {
                    problemas = String.join(" | ", e.problems());
                }
                boolean sinExcluido = mixta.stream().flatMap(w -> w.exercises().stream()).noneMatch(e -> e.exerciseId() == OBJETIVO);
                md.append("- **P-03 nivel 2 (semana entera con reglas):** repeticiones ").append(tMed[0]).append(" -> ")
                        .append(tN2[0]).append(", segundos ").append(tMed[1]).append(" -> ").append(tN2[1])
                        .append(tN2[0] <= tMed[0] && tN2[1] <= tMed[1] ? " (dentro del trabajo medido)" : " (**sube el trabajo: rechazada**)")
                        .append("\n");
                md.append("- **Variante localizada (solo ").append(nueva).append(" regenerada):** V1-V25: ").append(problemas)
                        .append("; sin el 1373: ").append(sinExcluido).append("; sesion: repeticiones ").append(tSesMed[0])
                        .append(" -> ").append(tSesNueva[0]).append(", segundos ").append(tSesMed[1]).append(" -> ")
                        .append(tSesNueva[1]).append("; semana: repeticiones ").append(tMed[0]).append(" -> ").append(tMix[0])
                        .append(", segundos ").append(tMed[1]).append(" -> ").append(tMix[1]).append("; ejercicios de la sesion: ")
                        .append(nuevaSes == null ? "-" : nuevaSes.exercises().stream().map(e -> e.exerciseId() + " " + (e.prescriptionType()
                                == PrescriptionType.DURATION ? e.plannedSets() + "x" + e.plannedDurationSeconds() + "s"
                                : e.plannedSets() + "x" + e.plannedReps())).collect(Collectors.joining(", ")))
                        .append("\n\n");
            } catch (RuntimeException e) {
                md.append("- Regeneracion con reglas sin semana valida: ").append(e.getMessage()).append("\n\n");
            }
            if (casos == 1) md.append("**Original:** ").append(describir(original, objetivo)).append("\n\n");
            md.append("### Embudo sobre los elegibles del perfil (").append(elegibles.size()).append(")\n\n");
            embudo(md, csv, caso, "ELEGIBLES", elegibles.values(), filtros, objetivo);
            if (casos == 1) {
                var activos = new LinkedHashSet<>(jdbc.queryForList("SELECT exercise_id FROM exercises WHERE is_active"
                        + " ORDER BY exercise_id", Long.class));
                var catalogoActivo = catalogo.fetchAsCandidates(activos).values().stream()
                        .sorted(Comparator.comparing(CandidateExercise::exerciseId)).toList();
                md.append("\n### Embudo sobre el catalogo activo entero, sin filtro de perfil (")
                        .append(catalogoActivo.size()).append(")\n\n");
                embudo(md, csv, caso, "CATALOGO", catalogoActivo, filtros, objetivo);
            }
            md.append("\n");
        }
        md.insert(md.indexOf("\n\n") + 2, "Casos analizados: " + casos + "; sin sustituto SUST-SEG-1: " + sinSustituto + "\n\n");
        Files.createDirectories(SALIDA);
        Files.writeString(SALIDA.resolve("resumen.md"), md);
        Files.writeString(SALIDA.resolve("candidatos.csv"), csv);
        System.out.println(md);
        assertTrue(casos > 0, "ningun plan con el 1373");
    }

    /**
     * D2 · PROTOTIPO DE MEDICION (solo pruebas; no implementa nada en src/main). Para semanas con dolor en el ultimo
     * ejercicio del miercoles, compara lo que hace hoy P-03 nivel 2 (semana entera regenerada con reglas, el mismo
     * {@code runDeterministic} que usa {@code SafeRegenerationService}) con una variante LOCALIZADA: las sesiones
     * medidas tal cual (+7 dias) y SOLO el miercoles tomado de esa regeneracion, validando la semana mixta con el
     * validador real (V1-V25) y con las reglas de dolor (nada excluido, sin subir el trabajo de la sesion ni de la
     * semana). Mide; no decide.
     */
    @Test
    void prototipoP03Localizado() throws Exception {
        var csv = new StringBuilder("objetivo;usuario;desenlace_ciclo;nivel1_sust_seg_1;n2_valida;n2_sesiones_cambiadas;"
                + "n2_ejercicios_cambiados;n2_trabajo_ok;loc_valida_v1_v25;loc_sin_excluidos;loc_trabajo_sesion_ok;"
                + "loc_trabajo_semana_ok;loc_ejercicios_cambiados;loc_problemas\n");
        var resumen = new TreeMap<String, int[]>();
        int n = 0;
        for (var meta : GoalType.values())
            for (int i = 1; i <= 10; i++) {
                long u = participante(("proto-" + meta + "-" + i).toLowerCase(Locale.ROOT), meta);
                long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
                var ids = sesionesDe(plan);
                if (ids.size() < 2) continue;
                long miercoles = ids.get(1);
                var ej = ejerciciosDe(miercoles);
                long dolio = ej.get(ej.size() - 1);
                for (long w : ids) hacer(u, w, w == miercoles ? dolio : -1);
                var publicado = ciclo.runFor(u, S1, S1.plusWeeks(1));
                var ev = jdbc.queryForMap("SELECT status, generation_source, conservation_report::text AS informe"
                        + " FROM plan_adaptation_attempts WHERE user_id = ? ORDER BY attempt_id DESC LIMIT 1", u);
                String desenlace = ev.get("status") + (publicado.isPresent() ? "/publicado" : "/sin_publicar");
                boolean nivel1 = String.valueOf(ev.get("informe")).contains("REGENERACION_SEGURA");

                var preparada = comandos.prepare(new GenerateWeeklyPlanCommand(u, S1.plusWeeks(1), PlanAdjustment.none(),
                        true, null, null));
                var ctx = preparada.context();
                var medidas = medidas(plan);
                PlanDraft regenerada = null;
                try {
                    regenerada = pipeline.runDeterministic(ctx, preparada.divisor()).draft();
                } catch (RuntimeException e) {
                    // sin semana valida con reglas: n2 y la variante localizada se quedan sin material
                }
                String fila;
                if (regenerada == null) {
                    fila = String.join(";", meta.name(), "" + u, desenlace, "" + nivel1, "false", "", "", "", "false",
                            "", "", "", "", "regeneracion con reglas sin semana valida");
                } else {
                    // Nivel 2 actual
                    int sesCambiadas = 0, ejCambiados = 0;
                    for (var m : medidas) {
                        var r = porFecha(regenerada, m.scheduledDate());
                        var a = m.exercises().stream().map(PlanDraft.DraftExercise::exerciseId).toList();
                        var b = r == null ? List.<Long>of() : r.exercises().stream().map(PlanDraft.DraftExercise::exerciseId).toList();
                        if (!a.equals(b)) sesCambiadas++;
                        ejCambiados += (int) a.stream().filter(x -> !b.contains(x)).count();
                    }
                    long[] tMedida = trabajo(medidas), tN2 = trabajo(regenerada.workouts());
                    boolean n2Trabajo = tN2[0] <= tMedida[0] && tN2[1] <= tMedida[1];
                    // Variante localizada: las medidas, salvo el miercoles regenerado.
                    var fechaMie = medidas.get(1).scheduledDate();
                    var mieNuevo = porFecha(regenerada, fechaMie);
                    var mixta = new ArrayList<PlanDraft.DraftWorkout>();
                    for (var m : medidas) mixta.add(m.scheduledDate().equals(fechaMie) && mieNuevo != null ? mieNuevo : m);
                    var excluidos = new HashSet<>(jdbc.queryForList("SELECT exercise_id FROM user_safety_exclusions"
                            + " WHERE user_id = ? AND status = 'ACTIVA'", Long.class, u));
                    boolean sinExcluidos = mixta.stream().flatMap(w -> w.exercises().stream())
                            .noneMatch(e -> excluidos.contains(e.exerciseId()));
                    long[] tSesMed = trabajo(List.of(medidas.get(1))), tSesNueva = trabajo(mieNuevo == null ? List.of() : List.of(mieNuevo));
                    boolean sesionOk = mieNuevo != null && tSesNueva[0] <= tSesMed[0] && tSesNueva[1] <= tSesMed[1];
                    long[] tMixta = trabajo(mixta);
                    boolean semanaOk = tMixta[0] <= tMedida[0] && tMixta[1] <= tMedida[1];
                    var aMie = medidas.get(1).exercises().stream().map(PlanDraft.DraftExercise::exerciseId).toList();
                    var bMie = mieNuevo == null ? List.<Long>of() : mieNuevo.exercises().stream().map(PlanDraft.DraftExercise::exerciseId).toList();
                    int locCambiados = (int) aMie.stream().filter(x -> !bMie.contains(x)).count();
                    String problemas = "";
                    try {
                        validador.validate(new PlanDraft(GenerationSource.RULE_ENGINE, null, null, null, "prototipo",
                                mixta), ctx, preparada.divisor());
                    } catch (InvalidPlanDraftException e) {
                        problemas = String.join(" | ", e.problems()).replace(';', ',');
                    }
                    fila = String.join(";", meta.name(), "" + u, desenlace, "" + nivel1, "true", sesCambiadas + "/"
                                    + medidas.size(), "" + ejCambiados, "" + n2Trabajo, "" + problemas.isEmpty(), "" + sinExcluidos,
                            "" + sesionOk, "" + semanaOk, "" + locCambiados, problemas);
                    var r = resumen.computeIfAbsent(meta.name(), k -> new int[8]);
                    r[0]++;
                    if (n2Trabajo) r[1]++;
                    r[2] += sesCambiadas;
                    if (problemas.isEmpty() && sinExcluidos && sesionOk && semanaOk) r[3]++;
                    if (problemas.isEmpty()) r[4]++;
                    if (sesionOk) r[5]++;
                    r[6] += medidas.size();
                }
                csv.append(fila).append('\n');
                n++;
            }
        var md = new StringBuilder("# Prototipo de medicion: P-03 nivel 2 frente a una variante localizada\n\n")
                .append("| Objetivo | Casos con regeneracion | N2 dentro del trabajo medido | Sesiones cambiadas por N2 |"
                        + " Localizada valida (V1-V25 + dolor + trabajo) | Localizada pasa V1-V25 | Localizada: sesion sin mas trabajo |\n"
                        + "|---|---|---|---|---|---|---|\n");
        resumen.forEach((k, v) -> md.append("| ").append(k).append(" | ").append(v[0]).append(" | ").append(v[1])
                .append(" | ").append(v[2]).append("/").append(v[6]).append(" | ").append(v[3]).append(" | ").append(v[4])
                .append(" | ").append(v[5]).append(" |\n"));
        Files.createDirectories(SALIDA);
        Files.writeString(SALIDA.resolve("prototipo_localizado.csv"), csv);
        Files.writeString(SALIDA.resolve("prototipo_localizado.md"), md);
        System.out.println(md);
        assertTrue(n > 0);
    }

    private List<PlanDraft.DraftWorkout> medidas(long plan) {
        var salida = new ArrayList<PlanDraft.DraftWorkout>();
        for (var w : jdbc.queryForList("SELECT planned_workout_id, scheduled_date, focus_code, workout_name,"
                + " expected_duration_minutes FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date", plan)) {
            long id = ((Number) w.get("planned_workout_id")).longValue();
            var ej = jdbc.query("SELECT exercise_id, prescription_type, planned_sets, planned_reps,"
                            + " planned_duration_seconds, rest_seconds FROM planned_workout_exercises WHERE planned_workout_id = ?"
                            + " ORDER BY exercise_order",
                    (rs, k) -> new PlanDraft.DraftExercise(rs.getLong(1), PrescriptionType.valueOf(rs.getString(2)),
                            rs.getObject(3, Short.class), rs.getObject(4, Short.class), rs.getObject(5, Integer.class),
                            null, rs.getObject(6, Short.class), null), id);
            salida.add(new PlanDraft.DraftWorkout(((java.sql.Date) w.get("scheduled_date")).toLocalDate().plusWeeks(1),
                    WorkoutFocus.valueOf((String) w.get("focus_code")), String.valueOf(w.get("workout_name")),
                    ((Number) w.get("expected_duration_minutes")).intValue(), ej));
        }
        return salida;
    }

    private static PlanDraft.DraftWorkout porFecha(PlanDraft d, LocalDate fecha) {
        return d.workouts().stream().filter(w -> fecha.equals(w.scheduledDate())).findFirst().orElse(null);
    }

    private static long[] trabajo(List<PlanDraft.DraftWorkout> ws) {
        long r = 0, sg = 0;
        for (var w : ws)
            for (var e : w.exercises()) {
                int series = e.plannedSets() == null ? 1 : e.plannedSets();
                r += (long) series * (e.plannedReps() == null ? 0 : e.plannedReps());
                sg += (long) series * (e.plannedDurationSeconds() == null ? 0 : e.plannedDurationSeconds());
            }
        return new long[]{r, sg};
    }

    private static String sustituciones(String informe) {
        int i = informe.indexOf("\"sustituciones\"");
        if (i < 0) return "[]";
        int j = informe.indexOf(']', i);
        return informe.substring(i, j + 1);
    }

    private void embudo(StringBuilder md, StringBuilder csv, String caso, String universo, Collection<CandidateExercise> todos,
                        List<Filtro> filtros, String objetivo) {
        md.append("| Filtro | Quedan |\n|---|---|\n");
        var quedan = new ArrayList<>(todos);
        for (var f : filtros) {
            quedan.removeIf(c -> !f.pasa().test(c));
            md.append("| ").append(f.nombre()).append(" | ").append(quedan.size()).append(" |\n");
        }
        // Los «casi»: fallan exactamente un filtro.
        var porFiltro = new LinkedHashMap<String, List<CandidateExercise>>();
        for (var c : todos) {
            var fallan = filtros.stream().filter(f -> !f.pasa().test(c)).map(Filtro::nombre).toList();
            if (fallan.size() == 1) porFiltro.computeIfAbsent(fallan.get(0), k -> new ArrayList<>()).add(c);
            if (fallan.size() <= 2)
                csv.append(String.join(";", caso, universo, String.valueOf(c.exerciseId()), limpio(c.name()),
                        String.valueOf(c.bodyPartCode()), String.valueOf(c.equipmentCode()),
                        String.valueOf(c.difficulty()), String.valueOf(c.defaultPrescription()),
                        String.valueOf(c.movementPattern()), MandatorySlot.of(c).map(Enum::name).orElse("relleno"),
                        String.valueOf(CatalogEligibilityPolicy.esComplemento(c, objetivo)),
                        String.valueOf(c.targetMuscle()), String.join(" + ", fallan))).append('\n');
        }
        md.append("\n**Candidatos que fallan UN solo filtro:**\n\n");
        if (porFiltro.isEmpty()) md.append("- ninguno\n");
        porFiltro.forEach((f, l) -> md.append("- ").append(f).append(": ").append(l.size()).append(" (")
                .append(l.stream().limit(12).map(c -> c.exerciseId() + " " + limpio(c.name()) + " [" + c.bodyPartCode()
                        + ", " + c.defaultPrescription() + ", " + c.movementPattern() + "]").collect(Collectors.joining("; ")))
                .append(l.size() > 12 ? "; ..." : "").append(")\n"));
    }

    private static String describir(CandidateExercise c, String objetivo) {
        return "%d «%s»: zona %s, equipo %s, dificultad %d, tipo %s, patron %s, slot %s, complemento %s, musculo %s"
                .formatted(c.exerciseId(), c.name(), c.bodyPartCode(), c.equipmentCode(), c.difficulty(),
                        c.defaultPrescription(), c.movementPattern(), MandatorySlot.of(c).map(Enum::name).orElse("relleno"),
                        CatalogEligibilityPolicy.esComplemento(c, objetivo), c.targetMuscle());
    }

    private static String limpio(String s) { return s == null ? "" : s.replace(';', ','); }

    /** Igual que el filtro privado de SafetySubstitutionPolicy (V22). */
    private static boolean admiteBrazo(WorkoutFocus enfoque, CandidateExercise c) {
        if (enfoque != WorkoutFocus.PUSH && enfoque != WorkoutFocus.PULL) return true;
        if (!"upper arms".equals(c.bodyPartCode()) || c.targetMuscle() == null) return true;
        String prohibido = enfoque == WorkoutFocus.PUSH ? "biceps" : "triceps";
        return !c.targetMuscle().toLowerCase(Locale.ROOT).contains(prohibido);
    }

    private long participante(int n) {
        return participante("analisis-1373-" + n, GoalType.GENERAL_FITNESS);
    }

    private long participante(String nombre, GoalType meta) {
        String email = nombre + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 1, 15), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("70"), null, FitnessLevel.INTERMEDIATE, meta,
                "analisis", TrainingLocation.GYM, (short) 3, List.of((short) 1, (short) 3, (short) 5), (short) 45,
                GIMNASIO, List.of(), "Ninguna", true, true, true, true, true));
        return u;
    }

    private List<Long> sesionesDe(long plan) {
        return jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date",
                Long.class, plan);
    }

    private List<Long> ejerciciosDe(long w) {
        return jdbc.queryForList("SELECT exercise_id FROM planned_workout_exercises WHERE planned_workout_id = ?"
                + " ORDER BY exercise_order", Long.class, w);
    }

    private void hacer(long u, long w, long conDolor) {
        RelojPruebas.abrirVentanaDeEjecucion(jdbc, u);
        long s = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
        for (var f : jdbc.queryForList("SELECT planned_exercise_id, exercise_id, prescription_type, planned_sets,"
                + " planned_reps, planned_duration_seconds FROM planned_workout_exercises WHERE planned_workout_id = ?"
                + " ORDER BY exercise_order", w)) {
            long pe = ((Number) f.get("planned_exercise_id")).longValue();
            if (((Number) f.get("exercise_id")).longValue() == conDolor) {
                sesiones.handle(new RecordSessionExerciseCommand(u, s, pe, (short) 0, null, null, null,
                        SkipReason.PAIN_OR_DISCOMFORT));
                continue;
            }
            boolean duracion = "DURATION".equals(f.get("prescription_type"));
            int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
            int total = series * ((Number) f.get(duracion ? "planned_duration_seconds" : "planned_reps")).intValue();
            sesiones.handle(new RecordSessionExerciseCommand(u, s, pe, (short) series, duracion ? null : total,
                    duracion ? total : null, null, null));
        }
        sesiones.handle(new FinishWorkoutSessionCommand(u, s, (short) 6, (short) 4, (short) 45));
    }
}
