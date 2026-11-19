package main.web.services.fitsense.catalogo;

import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.profiling.infrastructure.persistence.jpa.repositories.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * V53-B · MEDICION (opt-in, solo evidencia, datos sinteticos en la base efimera). La rejilla de 168 perfiles de
 * {@link CatalogoIntegralComparacionIT} (objetivo x nivel x lugar x restriccion, motor de reglas, sin IA) con cada valor
 * del permiso propio «apoyo suspendido». Por perfil: conjunto elegible, cuantos de los 15 de V53 contiene, desenlace y
 * cuantos de los 15 lleva el plan generado. Lo compara {@code docs/FitSense/catalogo_integral/cierre/v53b/medir_v53b.py}.
 * <p>
 * {@code -Dfitsense.v53b.medicion=true [-Dfitsense.v53b.salida=target/v53b]}
 */
abstract class MedicionPermisoApoyoSuspendidoBase {

    static final Set<Long> APOYO = PermisoApoyoSuspendidoIT.APOYO;

    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired UserProfileCommandService perfiles;
    @Autowired UserProfileRepository repositorio;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate tx;

    /** Nombre del modo en la salida. */
    abstract String modo();

    /** Valores del permiso propio que se miden (null = sin responder). */
    abstract List<Boolean> permisos();

    private static String apoyo(java.util.Collection<Long> ids) {
        return ids.stream().filter(APOYO::contains).distinct().sorted().map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    @Test
    void rejilla() throws Exception {
        var sb = new StringBuilder("modo\tperfil\tpermiso\telegibles\tapoyo_elegibles\tdesenlace\tapoyo_en_plan\t"
                + "elegibles_ids\tplan\n");
        int perfilesMedidos = 0;
        boolean primeraPasada = true;
        for (var permiso : permisos()) {
            // Cada pasada con los MISMOS ids de usuario (la semilla del motor depende del usuario y la semana): asi
            // dos pasadas solo difieren por el permiso. Se vacia todo lo que cuelga de users antes de repetirlos.
            if (!primeraPasada) {
                jdbc.execute("TRUNCATE users CASCADE");
                if (jdbc.queryForObject("SELECT (SELECT count(*) FROM user_profiles) + (SELECT count(*) FROM"
                        + " weekly_training_plans)", Long.class) != 0)
                    throw new IllegalStateException("la base no quedo vacia entre pasadas");
            }
            primeraPasada = false;
            long id = 9000;
            for (var objetivo : GoalType.values())
                for (var nivel : FitnessLevel.values())
                    for (var lugar : List.of(TrainingLocation.GYM, TrainingLocation.HOME))
                        for (var r : CatalogoIntegralComparacionIT.RESTRICCIONES) {
                            long u = ++id;
                            jdbc.update("INSERT INTO users (user_id, email, password_hash, first_name,"
                                    + " consent_granted_at, enrolled_at) OVERRIDING SYSTEM VALUE VALUES (?, ?, 'x',"
                                    + " 'V53B', now(), now())", u, "v53b" + u + "@pruebas.invalid");
                            perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1985, 6, 1),
                                    BiologicalSex.MALE, new BigDecimal("180"), new BigDecimal("100"),
                                    new BigDecimal("80"), nivel, objetivo, "medicion v53b", lugar, (short) 4,
                                    List.of((short) 1, (short) 3, (short) 5, (short) 6), (short) 45,
                                    lugar == TrainingLocation.GYM ? CatalogoIntegralComparacionIT.GIMNASIO
                                            : CatalogoIntegralComparacionIT.CASA, List.of(), null,
                                    (Boolean) r[1], (Boolean) r[2], (Boolean) r[3], (Boolean) r[4], (Boolean) r[5]));
                            if (permiso != null)
                                tx.executeWithoutResult(s -> {
                                    var p = repositorio.findById(u).orElseThrow();
                                    p.confirmarApoyoSuspendido(permiso);
                                    repositorio.save(p);
                                });
                            String clave = objetivo + "/" + nivel + "/" + lugar + "/" + r[0];
                            List<Long> elegibles;
                            try {
                                elegibles = planComandos.prepare(GenerateWeeklyPlanCommand.firstPlan(u,
                                                CatalogoIntegralComparacionIT.SEMANA)).context().availableExercises()
                                        .stream().map(c -> c.exerciseId()).sorted().toList();
                            } catch (RuntimeException e) {
                                elegibles = null;
                            }
                            String desenlace, enPlan = "", contenido = "";
                            try {
                                long plan = planning.generateWeeklyPlan(u, CatalogoIntegralComparacionIT.SEMANA,
                                        PlanAdjustment.none(), null, null).orElseThrow();
                                desenlace = "PLAN";
                                enPlan = apoyo(jdbc.queryForList("SELECT pwe.exercise_id FROM"
                                        + " planned_workout_exercises pwe JOIN planned_workouts pw ON"
                                        + " pw.planned_workout_id = pwe.planned_workout_id WHERE pw.plan_id = ?",
                                        Long.class, plan));
                                contenido = String.join(" ", jdbc.queryForList("SELECT pw.scheduled_date || ':' ||"
                                        + " pwe.exercise_order || ':' || pwe.exercise_id || ':' ||"
                                        + " coalesce(pwe.planned_sets,0) || 'x' || coalesce(pwe.planned_reps,0) || '/'"
                                        + " || coalesce(pwe.planned_duration_seconds,0) FROM planned_workout_exercises"
                                        + " pwe JOIN planned_workouts pw ON pw.planned_workout_id ="
                                        + " pwe.planned_workout_id WHERE pw.plan_id = ? ORDER BY pw.scheduled_date,"
                                        + " pwe.exercise_order", String.class, plan));
                            } catch (RuntimeException e) {
                                desenlace = "SIN_PLAN:" + e.getClass().getSimpleName();
                            }
                            perfilesMedidos++;
                            sb.append(String.join("\t", modo(), clave, String.valueOf(permiso),
                                    elegibles == null ? "ERROR" : String.valueOf(elegibles.size()),
                                    elegibles == null ? "" : apoyo(elegibles), desenlace, enPlan,
                                    elegibles == null ? "" : elegibles.stream().map(String::valueOf)
                                            .collect(Collectors.joining(",")), contenido)).append('\n');
                        }
        }
        var dir = Path.of(System.getProperty("fitsense.v53b.salida", "target/v53b"));
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("medicion_" + modo() + ".tsv"), sb.toString(), StandardCharsets.UTF_8);
        System.out.println("MEDICION_V53B modo=" + modo() + " perfiles=" + perfilesMedidos);
    }
}
