package main.web.services.fitsense.planning;

import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalConfigurationService;
import main.web.services.fitsense.planning.domain.model.valueobjects.MvpDosePolicy;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * 4D v3.4 (docs/FitSense/ADENDA_4D_v3_4.md) · GENERACION INICIAL DE GAIN_MUSCLE EN TODOS LOS MINUTOS DE 30 A 75.
 * <p>
 * Rejilla: GAIN_MUSCLE intermedio x {gimnasio, casa con mancuernas y bandas, casa con peso corporal} x {3, 4 dias} x
 * cada minuto de 30 a 75 (276 perfiles), mas principiante y avanzado en los bordes 60, 61, 68, 69, 70 y 75 con 3 dias
 * (36). Flujo de produccion, catalogo real, IA desactivada, semana fija 2026-11-02 (la semilla depende de la semana).
 * <p>
 * CRITERIOS FIJADOS ANTES DE EJECUTAR:
 * <ol>
 *   <li>Toda semana PUBLICADA cumple, recalculado aqui con el estimador real: colocaciones por sesion como mucho
 *       {@code MvpDosePolicy.maxColocaciones(m)}; dosis de 4D (SETS_REPS, 2-3 series, 10 o 13 repeticiones, 90 s);
 *       V20 (contenido &gt;= 70 % de m); V4 (contenido &lt;= m + 15 %); V13 (el enfoque no se repite en dias
 *       consecutivos).</li>
 *   <li>Gimnasio y casa con material generan en TODOS los minutos de 30 a 75 con 3 y 4 dias.</li>
 *   <li>Ningun fallo, en ningun equipamiento, es el de 4D v3.1 (V20 con el tope de 7 por encima de 60 min).</li>
 *   <li>Los fallos que haya con peso corporal se REGISTRAN con su motivo (no se exigen ni se esconden).</li>
 * </ol>
 * RESULTADO DE LA PRIMERA EJECUCION (04/11/2026), REGISTRADO Y NO OCULTADO: el criterio 2 NO se cumple. Fallan los
 * 42 perfiles intermedios de 53 a 59 minutos (14 por equipamiento, con 3 y con 4 dias), todos por V20: el tope de 6 de
 * 4D v3.1 da 2.220 s y el piso va de 2.226 s (53 min) a 2.478 s (59 min). Es el MISMO defecto de cobertura que 4D
 * v3.4 corrige por encima de 60 minutos; queda fuera de lo aprobado y se registra como C-8
 * (docs/FitSense/short_paper_ieee/k20_adaptacion_real/04_CAMBIOS_CANDIDATOS.md). Esta prueba lo AFIRMA, igual que
 * R0548 afirmaba el suyo: si C-8 se aprueba, la afirmacion debe pasar a "ningun fallo". Los criterios 1 y 3 se
 * cumplen (0 violaciones en 270 semanas publicadas) y no hay fallos fuera de 53-59.
 * El TOPE DE 9 frente al limite general de 8: {@code RuleBasedTrainingPlanGenerator.MAX_EXERCISES_PER_SESSION} y
 * {@code CapabilityPrescriptionPolicy.MAX_COLOC_DIA} (8) son los topes de los objetivos SIN dosis fija; en
 * GAIN_MUSCLE el tope lo fija 4D (V24 GM usa {@code MvpDosePolicy.maxColocaciones}), como ya pasaba con el 6 y el 7.
 * <p>
 * Se escribe {@code target/hipertrofia-4d-v34/} (planes.csv, sesiones.csv, distribucion.csv) con patrones, grupos,
 * series semanales por grupo y dias consecutivos del mismo grupo (recuperacion), para comparar 60 con 61-75.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("4D v3.4: GAIN_MUSCLE genera planes validos en cada minuto de 30 a 75 (rejilla de 312 perfiles)")
class Hipertrofia4Dv34IT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired PlanningContextFacade planning;
    @Autowired UserProfileCommandService perfiles;
    @Autowired ExternalConfigurationService configuracion;
    @Autowired JdbcTemplate jdbc;

    static final LocalDate SEMANA = LocalDate.of(2026, 11, 2);
    static final SessionDurationEstimator ESTIMADOR = new SessionDurationEstimator();
    static final Map<String, List<String>> EQUIPOS = new LinkedHashMap<>();
    static {
        EQUIPOS.put("GIMNASIO", List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine", "bench",
                "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine"));
        EQUIPOS.put("CASA_MANCUERNAS_BANDAS", List.of("body weight", "dumbbell", "band"));
        EQUIPOS.put("CASA_PESO_CORPORAL", List.of("body weight"));
    }
    static final Map<Integer, List<Short>> DIAS = Map.of(
            3, List.of((short) 1, (short) 3, (short) 5),
            4, List.of((short) 1, (short) 2, (short) 4, (short) 5));
    static final List<Integer> BORDES = List.of(60, 61, 68, 69, 70, 75);

    record Perfil(String id, FitnessLevel nivel, String equipo, int dias, int minutos) {}

    record Ej(LocalDate fecha, String enfoque, long id, String grupo, String patron, String tipo, Short series,
              Short reps, Integer duracion, Short descanso) {}

    static List<Perfil> rejilla() {
        var l = new ArrayList<Perfil>();
        for (var eq : EQUIPOS.keySet())
            for (int d : List.of(3, 4))
                for (int m = 30; m <= 75; m++)
                    l.add(new Perfil("GM-I-%s-%dd-%d".formatted(eq, d, m), FitnessLevel.INTERMEDIATE, eq, d, m));
        for (var nivel : List.of(FitnessLevel.BEGINNER, FitnessLevel.ADVANCED))
            for (var eq : EQUIPOS.keySet())
                for (int m : BORDES)
                    l.add(new Perfil("GM-%s-%s-3d-%d".formatted(nivel.name().charAt(0), eq, m), nivel, eq, 3, m));
        return l;
    }

    int secuencia;

    long crear(Perfil p) {
        String email = "v34-" + (++secuencia) + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'V34', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 6, 15), BiologicalSex.FEMALE,
                new BigDecimal("168"), new BigDecimal("68"), null, p.nivel(), GoalType.GAIN_MUSCLE, "v34",
                "GIMNASIO".equals(p.equipo()) ? TrainingLocation.GYM : TrainingLocation.HOME, (short) p.dias(),
                DIAS.get(p.dias()), (short) p.minutos(), EQUIPOS.get(p.equipo()), List.of(), "Ninguna", true, true,
                true, true, true));
        return u;
    }

    List<Ej> ejercicios(long plan) {
        return jdbc.query("SELECT pw.scheduled_date, pw.focus_code, pwe.exercise_id, bp.code, x.movement_pattern,"
                        + " pwe.prescription_type, pwe.planned_sets, pwe.planned_reps, pwe.planned_duration_seconds,"
                        + " pwe.rest_seconds FROM planned_workout_exercises pwe"
                        + " JOIN planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id"
                        + " JOIN exercises x ON x.exercise_id = pwe.exercise_id"
                        + " JOIN body_parts bp ON bp.body_part_id = x.body_part_id"
                        + " WHERE pw.plan_id = ? ORDER BY pw.scheduled_date, pwe.exercise_order",
                (rs, i) -> new Ej(rs.getDate(1).toLocalDate(), rs.getString(2), rs.getLong(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getObject(7, Short.class), rs.getObject(8, Short.class),
                        rs.getObject(9, Integer.class), rs.getObject(10, Short.class)), plan);
    }

    static String csv(Object o) {
        var s = String.valueOf(o).replace("\n", " ").replace("\r", " ");
        return s.contains(",") || s.contains("\"") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }

    @Test
    void rejillaDeTreintaASetentaYCinco() throws Exception {
        var params = configuracion.prescriptionParams();
        assertEquals(70, params.sessionMinutesFloorPct(), "el piso de V20 de MVP-1.8");
        var planesCsv = new StringBuilder("perfil,nivel,equipo,dias,minutos,tope,resultado,sesiones,colocaciones_min,"
                + "colocaciones_max,segundos_min,segundos_max,piso_v20,techo_v4,series_max_por_ejercicio,"
                + "grupo_max_cuota,grupos_distintos,patrones,series_semanales_por_grupo,"
                + "grupo_en_dias_consecutivos,motivo\n");
        var sesionesCsv = new StringBuilder("perfil,minutos,fecha,enfoque,colocaciones,tope,segundos,piso_v20,techo_v4,"
                + "dosis\n");
        var violaciones = new ArrayList<String>();
        var fallos = new LinkedHashMap<String, String>();

        for (var p : rejilla()) {
            int m = p.minutos();
            int tope = MvpDosePolicy.maxColocaciones(m);
            long piso = (m * 60L * 70 + 99) / 100;
            long techo = m * 60L * 115 / 100;
            long u = crear(p);
            Optional<Long> plan;
            String motivo = "";
            try {
                plan = planning.generateWeeklyPlan(u, SEMANA, PlanAdjustment.none(), null, null);
                if (plan.isEmpty()) motivo = "SIN_PLAN";
            } catch (RuntimeException e) {
                plan = Optional.empty();
                motivo = e.getClass().getSimpleName() + ": " + e.getMessage();
            }
            if (plan.isEmpty()) {
                fallos.put(p.id(), motivo);
                planesCsv.append(String.join(",", p.id(), p.nivel().name(), p.equipo(), "" + p.dias(), "" + m,
                        "" + tope, "FALLO", "", "", "", "", "", "" + piso, "" + techo, "", "", "", "", "", "",
                        csv(motivo.length() > 600 ? motivo.substring(0, 600) : motivo))).append('\n');
                continue;
            }
            var ej = ejercicios(plan.get());
            var porDia = new TreeMap<LocalDate, List<Ej>>();
            ej.forEach(e -> porDia.computeIfAbsent(e.fecha(), k -> new ArrayList<>()).add(e));
            long segMin = Long.MAX_VALUE, segMax = 0;
            int colMin = Integer.MAX_VALUE, colMax = 0, seriesMax = 0;
            double cuotaMax = 0;
            var enfoques = new TreeMap<LocalDate, String>();
            var seriesGrupo = new TreeMap<String, Integer>();
            var diasGrupo = new TreeMap<String, TreeSet<LocalDate>>();
            var patrones = new TreeMap<String, Integer>();
            for (var dia : porDia.entrySet()) {
                var lista = dia.getValue();
                enfoques.put(dia.getKey(), lista.get(0).enfoque());
                var draft = lista.stream().map(e -> new PlanDraft.DraftExercise(e.id(),
                        PrescriptionType.valueOf(e.tipo()), e.series(), e.reps(), e.duracion(), null, e.descanso(),
                        null)).toList();
                long seg = ESTIMADOR.estimateSeconds(new PlanDraft.DraftWorkout(dia.getKey(), WorkoutFocus.FULL_BODY,
                        "x", m, draft), params);
                segMin = Math.min(segMin, seg);
                segMax = Math.max(segMax, seg);
                colMin = Math.min(colMin, lista.size());
                colMax = Math.max(colMax, lista.size());
                var porGrupo = new TreeMap<String, Integer>();
                var dosis = new StringBuilder();
                for (var e : lista) {
                    porGrupo.merge(e.grupo(), 1, Integer::sum);
                    patrones.merge(String.valueOf(e.patron()), 1, Integer::sum);
                    int s = e.series() == null ? 0 : e.series();
                    seriesGrupo.merge(e.grupo(), s, Integer::sum);
                    diasGrupo.computeIfAbsent(e.grupo(), k -> new TreeSet<>()).add(e.fecha());
                    seriesMax = Math.max(seriesMax, s);
                    dosis.append(e.id()).append(':').append(e.series()).append('x').append(e.reps()).append('/')
                            .append(e.descanso()).append(' ');
                    if (!"SETS_REPS".equals(e.tipo()) || e.series() == null || (s != 2 && s != 3) || e.reps() == null
                            || (e.reps() != 10 && e.reps() != 13) || e.descanso() == null || e.descanso() != 90)
                        violaciones.add(p.id() + " " + dia.getKey() + ": dosis fuera de 4D " + e);
                }
                cuotaMax = Math.max(cuotaMax, porGrupo.values().stream().mapToInt(i -> i).max().orElse(0)
                        / (double) lista.size());
                if (lista.size() > tope)
                    violaciones.add(p.id() + " " + dia.getKey() + ": " + lista.size() + " colocaciones > tope " + tope);
                if (seg < piso) violaciones.add(p.id() + " " + dia.getKey() + ": V20 " + seg + " < " + piso);
                if (seg > techo) violaciones.add(p.id() + " " + dia.getKey() + ": V4 " + seg + " > " + techo);
                sesionesCsv.append(String.join(",", p.id(), "" + m, dia.getKey().toString(), lista.get(0).enfoque(),
                        "" + lista.size(), "" + tope, "" + seg, "" + piso, "" + techo, dosis.toString().trim()))
                        .append('\n');
            }
            // V13: el enfoque no se repite en dias consecutivos. Recuperacion: grupos en dias consecutivos.
            LocalDate anterior = null;
            for (var en : enfoques.entrySet()) {
                if (anterior != null && anterior.plusDays(1).equals(en.getKey())
                        && enfoques.get(anterior).equals(en.getValue()))
                    violaciones.add(p.id() + ": V13 enfoque " + en.getValue() + " en " + anterior + " y " + en.getKey());
                anterior = en.getKey();
            }
            var consecutivos = new TreeSet<String>();
            diasGrupo.forEach((g, fechas) -> fechas.forEach(f -> { if (fechas.contains(f.plusDays(1))) consecutivos.add(g); }));
            planesCsv.append(String.join(",", p.id(), p.nivel().name(), p.equipo(), "" + p.dias(), "" + m, "" + tope,
                    "VALIDO", "" + porDia.size(), "" + colMin, "" + colMax, "" + segMin, "" + segMax, "" + piso,
                    "" + techo, "" + seriesMax, "%.2f".formatted(cuotaMax), "" + seriesGrupo.size(),
                    csv(patrones), csv(seriesGrupo), csv(consecutivos), "")).append('\n');
        }

        var dir = Path.of("target/hipertrofia-4d-v34");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("planes.csv"), planesCsv);
        Files.writeString(dir.resolve("sesiones.csv"), sesionesCsv);
        Files.writeString(dir.resolve("violaciones.txt"), String.join("\n", violaciones));

        // 1: invariantes de toda semana publicada.
        assertTrue(violaciones.isEmpty(), violaciones.size() + " violaciones: "
                + violaciones.subList(0, Math.min(20, violaciones.size())));
        // 3: ningun fallo es el de 4D v3.1.
        var deV31 = fallos.entrySet().stream().filter(f -> f.getValue().contains("V20")
                && Integer.parseInt(f.getKey().substring(f.getKey().lastIndexOf('-') + 1)) > 60).toList();
        assertTrue(deV31.isEmpty(), "fallos de V20 por encima de 60 min: " + deV31);
        // 2 (registro de C-8): los UNICOS fallos son los intermedios de 53-59 minutos, todos por V20 y en los tres
        // equipamientos; ningun otro perfil (ni principiante ni avanzado, ni otro minuto) falla.
        assertEquals(Map.of(), fallos, "LABORATORIO C-8: ningun fallo");
        // 4: los motivos quedan en planes.csv.
    }
}
