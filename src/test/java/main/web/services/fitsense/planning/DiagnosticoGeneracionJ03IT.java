package main.web.services.fitsense.planning;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.CapabilityPrescriptionPolicy;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.domain.services.TrainingPlanGenerator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

/**
 * AUDITORIA POST R/R' · DIAGNOSTICO DE GENERACION INICIAL (solo mide; no cambia nada del sistema).
 * <p>
 * Reproduce un perfil (por defecto el de J03: LOSE_WEIGHT principiante, casa con peso corporal, 4 dias L-M-J-V, 70 min)
 * con el contexto real (catalogo V1-V46, PostgreSQL efimero), ejecuta el MOTOR DE REGLAS y lo valida con el validador de
 * produccion. Despues hace una BUSQUEDA EXHAUSTIVA DE UN PASO sobre cada sesion rechazada: anade cada candidato elegible
 * (con cada dosis admisible de su capacidad, en su serie minima y maxima) o una serie mas a cada ejercicio, y vuelve a
 * validar el plan COMPLETO con el validador real. Si alguna modificacion deja el plan sin problemas, existe un plan
 * valido que el generador no encontro (fallo del algoritmo, no imposibilidad). Despues prueba combinaciones de dos pasos
 * sobre las mejores candidatas.
 * <p>
 * Opt-in: {@code -Dfitsense.diagnostico.generacion=true}. Parametros: {@code -Dfitsense.diagnostico.objetivo},
 * {@code .nivel}, {@code .equipo} (GIMNASIO | CASA_MANCUERNAS_BANDAS | CASA_PESO_CORPORAL), {@code .dias} (3/4/5/6),
 * {@code .minutos}, {@code .salida}. Escribe la evidencia en la carpeta de salida.
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.diagnostico.generacion", matches = "true")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
class DiagnosticoGeneracionJ03IT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanDraftValidator validador;
    @Autowired @Qualifier("ruleBasedTrainingPlanGenerator") TrainingPlanGenerator reglas;
    @Autowired JdbcTemplate jdbc;

    static final SessionDurationEstimator ESTIMADOR = new SessionDurationEstimator();
    static final LocalDate SEMANA = LocalDate.of(2026, 11, 2);
    static final Map<String, List<String>> EQUIPOS = Map.of(
            "GIMNASIO", List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine", "bench", "band",
                    "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine"),
            "CASA_MANCUERNAS_BANDAS", List.of("body weight", "dumbbell", "band"),
            "CASA_PESO_CORPORAL", List.of("body weight"));
    static final Map<Integer, List<Short>> DIAS = Map.of(
            1, List.of((short) 1), 2, List.of((short) 1, (short) 4),
            3, List.of((short) 1, (short) 3, (short) 5),
            4, List.of((short) 1, (short) 2, (short) 4, (short) 5),
            5, List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5),
            6, List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6),
            7, List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6, (short) 7));

    record Resultado(String accion, List<String> problemas) {}

    List<String> problemas(PlanDraft d, PlanGenerationContext ctx, int divisor) {
        try {
            validador.validate(d, ctx, divisor);
            return List.of();
        } catch (InvalidPlanDraftException e) {
            return e.problems();
        }
    }

    static PlanDraft conSesion(PlanDraft d, int i, List<PlanDraft.DraftExercise> ejercicios) {
        var w = new ArrayList<>(d.workouts());
        var o = w.get(i);
        w.set(i, new PlanDraft.DraftWorkout(o.scheduledDate(), o.focus(), o.name(), o.expectedDurationMinutes(),
                List.copyOf(ejercicios)));
        return new PlanDraft(d.source(), d.modelName(), d.planName(), d.declaredTotalVolume(), d.rationale(), w);
    }

    static String csv(Object o) {
        var s = String.valueOf(o).replace("\n", " ");
        return s.contains(",") || s.contains("\"") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }

    @Test
    void diagnostico() throws Exception {
        var objetivo = GoalType.valueOf(System.getProperty("fitsense.diagnostico.objetivo", "LOSE_WEIGHT"));
        var nivel = FitnessLevel.valueOf(System.getProperty("fitsense.diagnostico.nivel", "BEGINNER"));
        String equipo = System.getProperty("fitsense.diagnostico.equipo", "CASA_PESO_CORPORAL");
        int dias = Integer.getInteger("fitsense.diagnostico.dias", 4);
        int minutos = Integer.getInteger("fitsense.diagnostico.minutos", 70);
        var salida = Path.of(System.getProperty("fitsense.diagnostico.salida", "target/diagnostico-generacion"));
        Files.createDirectories(salida);

        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES ('diag-gen@pruebas.invalid', 'x', 'Diag', now(), now())");
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = 'diag-gen@pruebas.invalid'", Long.class);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 2, 21), BiologicalSex.FEMALE,
                new BigDecimal("162"), new BigDecimal("78"), new BigDecimal("70"), nivel, objetivo, "diagnostico",
                "GIMNASIO".equals(equipo) ? TrainingLocation.GYM : TrainingLocation.HOME, (short) dias, DIAS.get(dias),
                (short) minutos, EQUIPOS.get(equipo), List.of(), "Ninguna", true, true, true, true, true));

        var prep = planComandos.prepare(new GenerateWeeklyPlanCommand(u, SEMANA, null, true, null, null));
        var ctx = prep.context();
        int divisor = prep.divisor();
        PrescriptionParams p = ctx.prescription();
        String obj = objetivo.name(), niv = nivel.name();
        long piso = PlanDraftValidator.minimumSessionSeconds(ctx);
        var porId = new HashMap<Long, CandidateExercise>();
        ctx.availableExercises().forEach(c -> porId.put(c.exerciseId(), c));

        var informe = new StringBuilder();
        informe.append("perfil: ").append(obj).append(' ').append(niv).append(' ').append(equipo).append(' ')
                .append(dias).append(" dias ").append(minutos).append(" min\n");
        informe.append("elegibles: ").append(ctx.availableExercises().size()).append(" · piso V20: ").append(piso)
                .append(" s · techo V4: ").append(minutos * 60L * 115 / 100).append(" s\n");

        // Catalogo elegible: capacidades admisibles y coste de cada una.
        var cat = new StringBuilder("id,nombre,grupo,patron,clase,dificultad,tipo,capacidad,series,series_max,reps,"
                + "segundos,descanso,coste_s\n");
        for (var c : ctx.availableExercises())
            for (var cap : CapabilityPrescriptionPolicy.admisibles(c, obj, niv)) {
                var d = CapabilityPrescriptionPolicy.dosis(cap, c, obj, niv, p.repLimits());
                if (d.isEmpty()) continue;
                var x = d.get();
                long coste = x.tipo() == PrescriptionType.DURATION
                        ? (long) x.seriesMax() * (x.segundos() == null ? 0 : x.segundos())
                        : (long) x.seriesMax() * (x.reps() == null ? 0 : x.reps()) * p.secondsPerRepOrDefault();
                coste += (long) (x.seriesMax() - 1) * x.descanso();
                cat.append(String.join(",", "" + c.exerciseId(), csv(c.name()), csv(c.bodyPartCode()),
                        csv(c.movementPattern()), "" + CapabilityPrescriptionPolicy.claseDe(c), "" + c.difficulty(),
                        "" + x.tipo(), "" + cap, "" + x.series(), "" + x.seriesMax(), "" + x.reps(), "" + x.segundos(),
                        "" + x.descanso(), "" + coste)).append('\n');
            }
        Files.writeString(salida.resolve("catalogo_elegible.csv"), cat);

        var borrador = reglas.generate(ctx, List.of());
        var base = problemas(borrador, ctx, divisor);
        informe.append("borrador del motor de reglas: ").append(base.isEmpty() ? "VALIDO" : base.size() + " problemas")
                .append('\n');
        base.forEach(x -> informe.append("  - ").append(x).append('\n'));
        var ses = new StringBuilder("dia,fecha,enfoque,orden,id,nombre,grupo,patron,clase,tipo,series,reps,segundos,"
                + "descanso,segundos_sesion,piso\n");
        for (int i = 0; i < borrador.workouts().size(); i++) {
            var w = borrador.workouts().get(i);
            long s = ESTIMADOR.estimateSeconds(w, p);
            informe.append("sesion ").append(i).append(' ').append(w.scheduledDate()).append(' ').append(w.focus())
                    .append(": ").append(w.exercises().size()).append(" ejercicios, ").append(s).append(" s\n");
            int k = 0;
            for (var e : w.exercises()) {
                var c = porId.get(e.exerciseId());
                ses.append(String.join(",", "" + i, "" + w.scheduledDate(), "" + w.focus(), "" + (++k),
                        "" + e.exerciseId(), csv(c == null ? "?" : c.name()), csv(c == null ? "?" : c.bodyPartCode()),
                        csv(c == null ? "?" : c.movementPattern()),
                        c == null ? "?" : "" + CapabilityPrescriptionPolicy.claseDe(c), "" + e.prescriptionType(),
                        "" + e.plannedSets(), "" + e.plannedReps(), "" + e.plannedDurationSeconds(),
                        "" + e.restSeconds(), "" + s, "" + piso)).append('\n');
            }
        }
        Files.writeString(salida.resolve("borrador_motor.csv"), ses);

        // Busqueda exhaustiva de un paso en cada sesion rechazada (V20, V15 u otra regla de la sesion).
        var falladas = new ArrayList<Integer>();
        for (int i = 0; i < borrador.workouts().size(); i++) {
            String f = borrador.workouts().get(i).scheduledDate().toString();
            if (base.stream().anyMatch(x -> x.contains(f))) falladas.add(i);
        }
        var busq = new StringBuilder("sesion,fecha,accion,id,capacidad,series,segundos_sesion,problemas_de_la_sesion,"
                + "problemas_totales,codigos\n");
        var arreglos = new LinkedHashMap<Integer, List<PlanDraft>>();
        for (int i : falladas) {
            var w = borrador.workouts().get(i);
            String f = w.scheduledDate().toString();
            var usados = new HashSet<Long>();
            w.exercises().forEach(e -> usados.add(e.exerciseId()));
            var intentos = new ArrayList<Map.Entry<String, PlanDraft>>();
            for (var c : ctx.availableExercises()) {
                if (usados.contains(c.exerciseId())) continue;
                for (var cap : CapabilityPrescriptionPolicy.admisibles(c, obj, niv)) {
                    var d = CapabilityPrescriptionPolicy.dosis(cap, c, obj, niv, p.repLimits());
                    if (d.isEmpty()) continue;
                    var variantes = new LinkedHashSet<Short>(List.of(d.get().series(), d.get().seriesMax()));
                    for (short n : variantes) {
                        var dn = d.get().conSeries(n);
                        if (dn.isEmpty()) continue;
                        var ej = new ArrayList<>(w.exercises());
                        ej.add(dn.get().prescribir(c.exerciseId(), null));
                        intentos.add(Map.entry("ANADE|" + c.exerciseId() + "|" + cap + "|" + n, conSesion(borrador, i, ej)));
                    }
                }
            }
            for (int k = 0; k < w.exercises().size(); k++) {
                var e = w.exercises().get(k);
                var ej = new ArrayList<>(w.exercises());
                ej.set(k, new PlanDraft.DraftExercise(e.exerciseId(), e.prescriptionType(),
                        (short) (e.plannedSets() + 1), e.plannedReps(), e.plannedDurationSeconds(), e.targetLoadKg(),
                        e.restSeconds(), e.notes()));
                intentos.add(Map.entry("SERIE_MAS|" + e.exerciseId() + "|-|" + (e.plannedSets() + 1),
                        conSesion(borrador, i, ej)));
            }
            // Sustitucion de UNA colocacion por cualquier candidato elegible con cualquier dosis admisible.
            for (int k = 0; k < w.exercises().size(); k++)
                for (var c : ctx.availableExercises()) {
                    if (usados.contains(c.exerciseId())) continue;
                    for (var cap : CapabilityPrescriptionPolicy.admisibles(c, obj, niv)) {
                        var d = CapabilityPrescriptionPolicy.dosis(cap, c, obj, niv, p.repLimits());
                        if (d.isEmpty()) continue;
                        for (short n : new LinkedHashSet<Short>(List.of(d.get().series(), d.get().seriesMax()))) {
                            var dn = d.get().conSeries(n);
                            if (dn.isEmpty()) continue;
                            var ej = new ArrayList<>(w.exercises());
                            long sale = ej.get(k).exerciseId();
                            ej.set(k, dn.get().prescribir(c.exerciseId(), null));
                            intentos.add(Map.entry("SUSTITUYE_" + sale + "|" + c.exerciseId() + "|" + cap + "|" + n,
                                    conSesion(borrador, i, ej)));
                        }
                    }
                }
            var validos = new ArrayList<PlanDraft>();
            for (var it : intentos) {
                var pr = problemas(it.getValue(), ctx, divisor);
                var deSesion = pr.stream().filter(x -> x.contains(f)).toList();
                long seg = ESTIMADOR.estimateSeconds(it.getValue().workouts().get(i), p);
                var codigos = new TreeSet<String>();
                pr.forEach(x -> codigos.add(x.contains(":") ? x.substring(0, x.indexOf(':')) : "OTRO"));
                var a = it.getKey().split("\\|");
                busq.append(String.join(",", "" + i, f, a[0], a[1], a[2], a[3], "" + seg, "" + deSesion.size(),
                        "" + pr.size(), csv(codigos))).append('\n');
                if (deSesion.isEmpty()) validos.add(it.getValue());
            }
            arreglos.put(i, validos);
            informe.append("sesion ").append(i).append(" (").append(f).append("): ").append(intentos.size())
                    .append(" modificaciones de un paso, ").append(validos.size())
                    .append(" dejan la sesion sin problemas\n");
        }
        Files.writeString(salida.resolve("busqueda_un_paso.csv"), busq);

        // Combinacion: un arreglo por sesion fallada, aplicados a la vez.
        if (!falladas.isEmpty() && arreglos.values().stream().noneMatch(List::isEmpty)) {
            PlanDraft combinado = borrador;
            for (int i : falladas) combinado = conSesion(combinado, i,
                    arreglos.get(i).get(0).workouts().get(i).exercises());
            var pr = problemas(combinado, ctx, divisor);
            informe.append("combinando el primer arreglo de cada sesion: ")
                    .append(pr.isEmpty() ? "PLAN VALIDO (existe solucion que el motor no encontro)" : pr.toString())
                    .append('\n');
            var comb = new StringBuilder("dia,fecha,enfoque,id,nombre,tipo,series,reps,segundos,descanso,segundos_sesion\n");
            for (int i = 0; i < combinado.workouts().size(); i++) {
                var w = combinado.workouts().get(i);
                long s = ESTIMADOR.estimateSeconds(w, p);
                for (var e : w.exercises()) {
                    var c = porId.get(e.exerciseId());
                    comb.append(String.join(",", "" + i, "" + w.scheduledDate(), "" + w.focus(), "" + e.exerciseId(),
                            csv(c == null ? "?" : c.name()), "" + e.prescriptionType(), "" + e.plannedSets(),
                            "" + e.plannedReps(), "" + e.plannedDurationSeconds(), "" + e.restSeconds(), "" + s))
                            .append('\n');
                }
            }
            Files.writeString(salida.resolve("plan_combinado.csv"), comb);
        } else if (!falladas.isEmpty()) {
            informe.append("alguna sesion no tiene arreglo de un paso: se prueban pares\n");
            for (int i : falladas) {
                if (!arreglos.get(i).isEmpty()) continue;
                // pares: las 25 modificaciones de un paso con mas segundos que no anaden problemas fuera de V20
                informe.append("  (pares no implementados aun para la sesion ").append(i).append(")\n");
            }
        }
        // Redistribucion entre dias consecutivos (dos pasos coordinados): se pasa un cardio del dia anterior a la sesion
        // fallada, el dia anterior recupera la colocacion con un ejercicio de fuerza de su enfoque y la sesion fallada
        // anade ademas un ejercicio de fuerza de su enfoque. Todo se valida con el validador real sobre el plan completo.
        var red = new StringBuilder("sesion,dia_previo,cardio_movido,fuerza_previo,fuerza_sesion,problemas,codigos\n");
        PlanDraft redistribuido = borrador;
        int encontrados = 0;
        for (int i : falladas) {
            int j = i - 1;
            if (j < 0) continue;
            var wi = redistribuido.workouts().get(i);
            var wj = redistribuido.workouts().get(j);
            var usadosSemana = new HashSet<Long>();
            redistribuido.workouts().forEach(w -> w.exercises().forEach(e -> usadosSemana.add(e.exerciseId())));
            PlanDraft mejor = null;
            outer:
            for (var cardio : wj.exercises()) {
                var cc = porId.get(cardio.exerciseId());
                if (cc == null || CapabilityPrescriptionPolicy.claseDe(cc) != CapabilityPrescriptionPolicy.Clase.CARDIO) continue;
                for (var fj : ctx.availableExercises()) {
                    if (usadosSemana.contains(fj.exerciseId())) continue;
                    var dj = CapabilityPrescriptionPolicy.dosis(GoalCapabilityPolicy.Capability.STRENGTH, fj, obj, niv, p.repLimits());
                    if (dj.isEmpty()) continue;
                    var ejj = new ArrayList<>(wj.exercises());
                    ejj.remove(cardio);
                    ejj.add(dj.get().prescribir(fj.exerciseId(), null));
                    var pj = conSesion(redistribuido, j, ejj);
                    var prj = problemas(pj, ctx, divisor).stream()
                            .filter(x -> x.contains(wj.scheduledDate().toString())).toList();
                    if (!prj.isEmpty()) {
                        red.append(String.join(",", "" + i, "" + j, "" + cardio.exerciseId(), "" + fj.exerciseId(),
                                "-", "" + prj.size(), csv(prj))).append('\n');
                        continue;
                    }
                    for (var fi : ctx.availableExercises()) {
                        if (usadosSemana.contains(fi.exerciseId()) || fi.exerciseId().equals(fj.exerciseId())) continue;
                        var di = CapabilityPrescriptionPolicy.dosis(GoalCapabilityPolicy.Capability.STRENGTH, fi, obj, niv, p.repLimits());
                        if (di.isEmpty()) continue;
                        var eji = new ArrayList<>(wi.exercises());
                        eji.add(cardio);
                        eji.add(di.get().prescribir(fi.exerciseId(), null));
                        var pij = conSesion(pj, i, eji);
                        var pr = problemas(pij, ctx, divisor);
                        var deDos = pr.stream().filter(x -> x.contains(wi.scheduledDate().toString())
                                || x.contains(wj.scheduledDate().toString())).toList();
                        var codigos = new TreeSet<String>();
                        pr.forEach(x -> codigos.add(x.contains(":") ? x.substring(0, x.indexOf(':')) : "OTRO"));
                        red.append(String.join(",", "" + i, "" + j, "" + cardio.exerciseId(), "" + fj.exerciseId(),
                                "" + fi.exerciseId(), "" + deDos.size(), csv(codigos))).append('\n');
                        if (deDos.isEmpty()) { mejor = pij; break outer; }
                    }
                }
            }
            if (mejor != null) { redistribuido = mejor; encontrados++; }
        }
        Files.writeString(salida.resolve("busqueda_redistribucion.csv"), red);
        var prRed = problemas(redistribuido, ctx, divisor);
        informe.append("redistribucion entre dias consecutivos: ").append(encontrados).append(" de ")
                .append(falladas.size()).append(" sesiones arregladas; plan completo: ")
                .append(prRed.isEmpty() ? "VALIDO (existe solucion que el motor no encontro)" : prRed.toString())
                .append('\n');
        if (prRed.isEmpty()) {
            var comb = new StringBuilder("dia,fecha,enfoque,id,nombre,grupo,tipo,series,reps,segundos,descanso,segundos_sesion\n");
            for (int i = 0; i < redistribuido.workouts().size(); i++) {
                var w = redistribuido.workouts().get(i);
                long s = ESTIMADOR.estimateSeconds(w, p);
                for (var e : w.exercises()) {
                    var c = porId.get(e.exerciseId());
                    comb.append(String.join(",", "" + i, "" + w.scheduledDate(), "" + w.focus(), "" + e.exerciseId(),
                            csv(c == null ? "?" : c.name()), csv(c == null ? "?" : c.bodyPartCode()),
                            "" + e.prescriptionType(), "" + e.plannedSets(), "" + e.plannedReps(),
                            "" + e.plannedDurationSeconds(), "" + e.restSeconds(), "" + s)).append('\n');
                }
            }
            Files.writeString(salida.resolve("plan_valido_encontrado.csv"), comb);
        }
        Files.writeString(salida.resolve("informe.txt"), informe);
        System.out.println("DIAGNOSTICO_GENERACION\n" + informe);
    }
}
