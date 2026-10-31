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
import org.junit.jupiter.api.DisplayName;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * INVESTIGACION DE FACTIBILIDAD 30-120 MIN (orden de investigacion y cierre del PR #16). No es una regresion: mide.
 * <p>
 * Para cada combinacion (objetivo x nivel x dias x minutos x equipo x restricciones) prepara el contexto real
 * (catalogo V1-V43 en PostgreSQL efimero), ejecuta el MOTOR DE REGLAS tal cual (sin IA: {@code fitsense.ai.enabled=false}
 * y ningun doble de Replicate) y valida el borrador con el validador de produccion (V1-V25). Registra la composicion
 * intentada y los validadores que la rechazan.
 * <p>
 * Ademas calcula una COTA SUPERIOR INDEPENDIENTE de la duracion de cualquier sesion valida: con las dosis aprobadas
 * (4D para GAIN_MUSCLE, {@link CapabilityPrescriptionPolicy} para los otros tres, ambas en su serie maxima admitida),
 * el tope de colocaciones que exige V24 y la formula del estimador (calentamiento + trabajo + descansos +
 * transiciones). Como el estimador es aditivo y cada colocacion suma su coste y una transicion, la sesion mas larga
 * posible usa el tope de colocaciones con los ejercicios elegibles mas caros, sin repetir. Esa cota ignora V15, V23,
 * V25 y el enfoque, que solo pueden bajarla: si queda por debajo del piso de V20, NINGUN plan valido existe (CASO B o
 * C, demostrado por calculo).
 * <p>
 * Opt-in: {@code -Dfitsense.factibilidad=true}. Escribe {@code target/factibilidad/MATRIZ_FACTIBILIDAD_30_120.csv}
 * (o en {@code docs/FitSense/campana_p123/} con {@code -Dfitsense.factibilidad.capturar=true}).
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.factibilidad", matches = "true")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("Investigacion · factibilidad de 30-120 min por objetivo, nivel, dias y equipo (motor de reglas)")
class FactibilidadDuracionIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanDraftValidator validador;
    @Autowired @Qualifier("ruleBasedTrainingPlanGenerator") TrainingPlanGenerator reglas;
    @Autowired JdbcTemplate jdbc;

    static final SessionDurationEstimator ESTIMADOR = new SessionDurationEstimator();
    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final int[] MINUTOS = {30, 60, 75, 90, 105, 120};
    static final LocalDate SEMANA = LocalDate.of(2026, 11, 2);
    static final Pattern CODIGO = Pattern.compile("^(V\\d+[A-Z]?)");

    /** Equipos: los mismos codigos que las referencias. */
    static final Map<String, List<String>> EQUIPOS = new LinkedHashMap<>();
    static {
        EQUIPOS.put("GIMNASIO", List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine", "bench",
                "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine"));
        EQUIPOS.put("CASA_MANCUERNAS_BANDAS", List.of("body weight", "dumbbell", "band"));
        EQUIPOS.put("CASA_PESO_CORPORAL", List.of("body weight"));
    }
    static final Map<Integer, List<Short>> DIAS = Map.of(
            1, List.of((short) 1),
            2, List.of((short) 1, (short) 4),
            3, List.of((short) 1, (short) 3, (short) 5),
            4, List.of((short) 1, (short) 2, (short) 4, (short) 5),
            6, List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6),
            7, List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6, (short) 7));

    record Combinacion(String id, GoalType objetivo, FitnessLevel nivel, int dias, int minutos, String equipo,
                       boolean restringido, String bloqueo) {}

    /** Cota por objetivo/nivel con el gimnasio sin restricciones: separa CASO B (perfil) de CASO C (equipo). */
    final Map<String, Long> cotaGimnasio = new HashMap<>();

    @Test
    void matriz() throws Exception {
        // Semana FIJA: la semilla del motor de reglas es user_id x 1.000.003 + dia de la semana; con la fecha del dia
        // la matriz cambiaria segun cuando se ejecute.
        var semana = SEMANA;
        var combinaciones = new ArrayList<Combinacion>();
        // 1 · Base y expansion: 4 objetivos x 3 niveles x 3 equipos x 1/2/3/4/6 dias x 6 duraciones.
        for (var equipo : EQUIPOS.keySet())
            for (int dias : new int[]{3, 1, 2, 4, 6})
                for (var objetivo : GoalType.values())
                    for (var nivel : FitnessLevel.values())
                        for (int m : MINUTOS)
                            combinaciones.add(new Combinacion(null, objetivo, nivel, dias, m, equipo, false, "-"));
        // 2 · Frecuencia: 7 dias pedidos (tope 4 principiante, 6 el resto).
        for (var objetivo : GoalType.values())
            for (var nivel : FitnessLevel.values())
                combinaciones.add(new Combinacion(null, objetivo, nivel, 7, 60, "GIMNASIO", false, "-"));
        // 3 · Restricciones de seguridad (sin impacto, suelo, rodillas, colgarse ni por encima de la cabeza).
        for (var objetivo : GoalType.values())
            for (var nivel : FitnessLevel.values())
                for (int m : MINUTOS)
                    combinaciones.add(new Combinacion(null, objetivo, nivel, 3, m, "GIMNASIO", true, "-"));
        // 4 · Bloqueo de todo el cardio elegible (CASO C: el equipo/restricciones del usuario).
        for (var objetivo : GoalType.values())
            for (var nivel : FitnessLevel.values())
                for (int m : MINUTOS)
                    combinaciones.add(new Combinacion(null, objetivo, nivel, 3, m, "GIMNASIO", false, "CARDIO"));

        var cabecera = String.join(",", "id", "user_id", "semana", "objetivo", "nivel", "dias_solicitados",
                "sesiones_efectivas", "minutos", "equipo", "restricciones", "bloqueo", "candidatos",
                "candidatos_con_dosis", "tope_colocaciones_v24", "piso_v20_s", "techo_v4_min", "presupuesto_4d_s",
                "cota_max_sesion_s", "cota_max_min", "minutos_max_factibles_por_cota", "cota_composicion",
                "sesiones_borrador", "enfoques", "ejercicios_por_sesion", "segundos_por_sesion",
                "min_declarados_por_sesion", "validadores_rechazo", "resultado", "clasificacion", "causa",
                "detalle_sesiones");
        var csv = new StringBuilder(cabecera).append('\n');
        int n = 0;
        var resumen = new TreeMap<String, Integer>();
        var solo = Arrays.stream(System.getProperty("fitsense.factibilidad.solo", "").split(","))
                .filter(x -> !x.isBlank()).collect(Collectors.toSet());
        for (var c : combinaciones) {
            n++;
            if (!solo.isEmpty() && !solo.contains("F%04d".formatted(n))) {
                // misma secuencia de usuarios que la corrida completa: ids y semillas reproducibles
                usuario();
                continue;
            }
            var fila = evaluar(new Combinacion("F%04d".formatted(n), c.objetivo(), c.nivel(), c.dias(), c.minutos(),
                    c.equipo(), c.restringido(), c.bloqueo()), semana);
            csv.append(fila.linea()).append('\n');
            resumen.merge(fila.clasificacion(), 1, Integer::sum);
        }
        var dir = Boolean.getBoolean("fitsense.factibilidad.capturar")
                ? Path.of("docs/FitSense/campana_p123") : Path.of("target/factibilidad");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("MATRIZ_FACTIBILIDAD_30_120.csv"), csv.toString());
        System.out.println("FACTIBILIDAD_RESUMEN " + n + " " + resumen);
        assertEquals(combinaciones.size(), n);
        assertFalse(resumen.containsKey("ERROR"));
        // Medicion, no regresion: los CASO_A se informan en el CSV y en la consola; no se ocultan con un rojo.
    }

    /**
     * RIESGO DEL PERFIL CONGELADO (fase F): la semilla del motor cambia con el usuario y la semana. Un perfil que
     * obtiene plan una semana queda congelado; si otra semana el motor no encuentra plan, esa semana queda sin plan y
     * el perfil no se puede corregir. Mismos perfiles (gimnasio, 3 dias) con 8 semillas: cuantas semanas sin plan.
     * Opt-in adicional: {@code -Dfitsense.factibilidad.semillas=true}.
     */
    @Test
    @EnabledIfSystemProperty(named = "fitsense.factibilidad.semillas", matches = "true")
    void semillas() throws Exception {
        var csv = new StringBuilder("objetivo,nivel,minutos,semillas,con_plan,sin_plan,validadores_sin_plan\n");
        for (var objetivo : GoalType.values())
            for (var nivel : FitnessLevel.values())
                for (int m : MINUTOS) {
                    int conPlan = 0;
                    var motivos = new TreeSet<String>();
                    for (int k = 0; k < 8; k++) {
                        var semana = SEMANA.plusWeeks(k);
                        long u = usuario();
                        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 6, 15),
                                BiologicalSex.FEMALE, new BigDecimal("168"), new BigDecimal("68"), null, nivel,
                                objetivo, "factibilidad", TrainingLocation.GYM, (short) 3, DIAS.get(3), admitido(m),
                                EQUIPOS.get("GIMNASIO"), List.of(), "Ninguna", true, true, true, true, true));
                        var prepared = planComandos.prepare(new GenerateWeeklyPlanCommand(u, semana, null, true,
                                null, null));
                        try {
                            var ctxM = conMinutos(prepared.context(), m);
                            validador.validate(reglas.generate(ctxM, List.of()), ctxM,
                                    prepared.divisor());
                            conPlan++;
                        } catch (InvalidPlanDraftException e) {
                            e.problems().forEach(pr -> {
                                var mt = CODIGO.matcher(pr.trim());
                                motivos.add(mt.find() ? mt.group(1) : "OTRO");
                            });
                        }
                    }
                    csv.append(String.join(",", objetivo.name(), nivel.name(), String.valueOf(m), "8",
                            String.valueOf(conPlan), String.valueOf(8 - conPlan), q(String.join(" ", motivos))))
                            .append('\n');
                    System.out.println("SEMILLAS " + objetivo + "/" + nivel + "/" + m + " con_plan=" + conPlan + "/8 "
                            + motivos);
                }
        var dir = Boolean.getBoolean("fitsense.factibilidad.capturar")
                ? Path.of("docs/FitSense/campana_p123") : Path.of("target/factibilidad");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("SEMILLAS_30_120.csv"), csv.toString());
    }


    /**
     * LABORATORIO (P-01 vigente: 30-75). Para estudiar duraciones fuera del rango admitido, el perfil se crea con
     * {@code min(minutos, 75)} y el contexto preparado se copia con los minutos del escenario. Nada de esto toca el
     * camino real: prepare() sigue rechazando un perfil fuera de P-01.
     */
    static PlanGenerationContext conMinutos(PlanGenerationContext c, int minutos) {
        var p = c.profile();
        if (p.sessionMinutes() == minutos) return c;
        var perfil = new PlanningProfile(p.userId(), p.age(), p.biologicalSex(), p.heightCm(), p.weightKg(),
                p.targetWeightKg(), p.fitnessLevel(), p.goalType(), p.goalText(), p.healthNotes(), p.trainingLocation(),
                p.daysPerWeek(), p.availableDays(), minutos, p.equipmentCodes(), p.blockedExerciseIds(), p.declaradas());
        return new PlanGenerationContext(c.userId(), c.weekNumber(), c.weekStartDate(), c.weekEndDate(), perfil,
                c.adjustment(), c.previousWeek(), c.availableExercises(), c.safety(), c.prescription());
    }

    static short admitido(int minutos) {
        return (short) Math.min(minutos, main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType
                .MAX_SESSION_MINUTES);
    }

    record Fila(String linea, String clasificacion) {}

    private Fila evaluar(Combinacion c, LocalDate semana) {
        long u = usuario();
        var equipo = EQUIPOS.get(c.equipo());
        var bloqueados = new ArrayList<Long>();
        if ("CARDIO".equals(c.bloqueo()))
            bloqueados.addAll(jdbc.queryForList("SELECT e.exercise_id FROM exercises e JOIN body_parts b"
                    + " ON b.body_part_id = e.body_part_id WHERE e.is_active AND b.code = 'cardio'", Long.class));
        boolean libre = !c.restringido();
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 6, 15), BiologicalSex.FEMALE,
                new BigDecimal("168"), new BigDecimal("68"), null, c.nivel(), c.objetivo(), "factibilidad",
                "GIMNASIO".equals(c.equipo()) ? TrainingLocation.GYM : TrainingLocation.HOME, (short) c.dias(),
                DIAS.get(c.dias()), admitido(c.minutos()), equipo, bloqueados, "Ninguna", libre, libre, libre, libre,
                libre));

        var prepared = planComandos.prepare(new GenerateWeeklyPlanCommand(u, semana, null, true, null, null));
        var ctx = conMinutos(prepared.context(), c.minutos());
        var p = ctx.prescription();
        String obj = c.objetivo().name();
        String niv = c.nivel().name();
        long piso = PlanDraftValidator.minimumSessionSeconds(ctx);
        int techo = (int) Math.floor(c.minutos() * 1.15);
        boolean gm = MvpDosePolicy.aplicaA(obj);
        int tope = gm ? MvpDosePolicy.maxColocaciones(c.minutos()) : CapabilityPrescriptionPolicy.MAX_COLOC_DIA;
        double presupuesto = gm ? MvpDosePolicy.presupuestoDiarioSegundos(c.minutos()) : -1;

        // ---- cota independiente
        var costes = new ArrayList<long[]>();   // {coste, id}
        for (var cand : ctx.availableExercises()) {
            long coste = costeMaximo(cand, obj, niv, p);
            if (coste > 0) costes.add(new long[]{coste, cand.exerciseId()});
        }
        costes.sort((a, b) -> Long.compare(b[0], a[0]));
        int k = Math.min(tope, costes.size());
        long cota = k == 0 ? 0 : (long) p.warmupMinutesOrDefault() * 60 + (long) (k - 1) * p.transitionSecondsOrDefault()
                + costes.subList(0, k).stream().mapToLong(a -> a[0]).sum();
        String composicion = costes.subList(0, k).stream().map(a -> a[1] + ":" + a[0]).collect(Collectors.joining(" "));
        // minutos maximos M tales que ceil(M*60*pct/100) <= cota
        int mMax = 0;
        for (int m = 1; m <= 180; m++) if ((m * 60L * p.floorPct() + 99) / 100 <= cota) mMax = m;
        String claveGim = obj + "/" + niv;
        if ("GIMNASIO".equals(c.equipo()) && !c.restringido() && "-".equals(c.bloqueo()))
            cotaGimnasio.putIfAbsent(claveGim, cota);

        // ---- motor de reglas + validador de produccion (lo mismo que el respaldo del pipeline)
        PlanDraft borrador = null;
        List<String> problemas = List.of();
        String resultado;
        try {
            borrador = reglas.generate(ctx, List.of());
            validador.validate(borrador, ctx, prepared.divisor());
            resultado = "PLAN_VALIDO";
        } catch (InvalidPlanDraftException e) {
            problemas = e.problems();
            resultado = "RECHAZADO";
        } catch (RuntimeException e) {
            problemas = List.of(e.getClass().getSimpleName() + ": " + e.getMessage());
            resultado = "SIN_BORRADOR";
        }

        var codigos = new TreeSet<String>();
        for (var pr : problemas) {
            var mt = CODIGO.matcher(pr.trim());
            codigos.add(mt.find() ? mt.group(1) : "OTRO");
        }
        // sesiones del borrador
        var enfoques = new ArrayList<String>();
        var ejercicios = new ArrayList<String>();
        var segundos = new ArrayList<String>();
        var declarados = new ArrayList<String>();
        var detalle = new ArrayList<String>();
        long maxSesion = 0;
        if (borrador != null)
            for (var w : borrador.workouts()) {
                long s = ESTIMADOR.estimateSeconds(w, p);
                maxSesion = Math.max(maxSesion, s);
                enfoques.add(String.valueOf(w.focus()));
                ejercicios.add(String.valueOf(w.exercises().size()));
                segundos.add(String.valueOf(s));
                declarados.add(String.valueOf(w.expectedDurationMinutes()));
                detalle.add(w.scheduledDate().getDayOfWeek().name().substring(0, 3) + "[" + w.exercises().stream()
                        .map(e -> e.exerciseId() + "=" + e.plannedSets() + "x"
                                + (e.prescriptionType() == PrescriptionType.DURATION ? e.plannedDurationSeconds() + "s"
                                : String.valueOf(e.plannedReps())) + "/" + e.restSeconds())
                        .collect(Collectors.joining(" ")) + "]");
            }

        String clasificacion;
        String causa;
        if ("GIMNASIO".equals(c.equipo()) && !c.restringido() && "-".equals(c.bloqueo()) && cota >= piso)
            obligacionesGimnasio.put(obj + "/" + niv + "/" + c.dias() + "/" + c.minutos(),
                    obligacionesCaben(ctx, obj, niv, ctx.effectiveDaysPerWeek(), tope, piso, p));
        if ("PLAN_VALIDO".equals(resultado)) {
            clasificacion = "VALIDO";
            causa = "-";
        } else if (cota < piso) {
            long ref = cotaGimnasio.getOrDefault(claveGim, Long.MAX_VALUE);
            if (ref < piso) {
                clasificacion = "CASO_B";
                causa = "Cota %d s < piso V20 %d s con el mejor equipo: ninguna sesion valida llega al piso"
                        .formatted(cota, piso);
            } else {
                clasificacion = "CASO_C";
                causa = "Con gimnasio la cota (%d s) alcanza el piso %d s; con %s%s%s queda en %d s"
                        .formatted(ref, piso, c.equipo(), c.restringido() ? "+restricciones" : "",
                                "-".equals(c.bloqueo()) ? "" : "+bloqueo " + c.bloqueo(), cota);
            }
        } else if (!obligacionesCaben(ctx, obj, niv, ctx.effectiveDaysPerWeek(), tope, piso, p)) {
            // La cota simple llega al piso, pero no con lo que la SEMANA obliga a poner (V23 slots, V25 principales,
            // V25 cardio diario de LOSE_WEIGHT): busqueda exhaustiva del reparto de esas piezas entre los dias.
            String clave = obj + "/" + niv + "/" + c.dias() + "/" + c.minutos();
            boolean conGimnasio = !"GIMNASIO".equals(c.equipo()) || c.restringido() || !"-".equals(c.bloqueo())
                    ? obligacionesGimnasio.getOrDefault(clave, false) : false;
            if ("GIMNASIO".equals(c.equipo()) && !c.restringido() && "-".equals(c.bloqueo()))
                obligacionesGimnasio.put(clave, false);
            clasificacion = conGimnasio ? "CASO_C" : "CASO_B";
            causa = ("Cota simple %d s >= piso %d s, pero ningun reparto de las piezas que la semana exige (%s) deja "
                    + "cada dia en el piso: busqueda exhaustiva sobre una relajacion (sin enfoque, V15, V18 ni techo)%s")
                    .formatted(cota, piso, ultimasObligaciones, conGimnasio ? "; con gimnasio si cabe" : "");
        } else {
            // Cota >= piso: o el motor no encuentra un plan que existe (CASO A), o la composicion lo impide.
            // Busqueda local INDEPENDIENTE del constructor, con el validador de produccion como unico juez.
            // Dos verificadores independientes del motor: (1) una semana CONSTRUIDA desde cero con lo admisible mas
            // largo, cubriendo antes lo que la semana exige (slots de 4C, principales de V25); si no pasa, busqueda
            // local desde ella; (2) busqueda local desde el borrador del motor.
            var testigo = Optional.<PlanDraft>empty();
            if (!gm) {
                var construida = construirTestigo(ctx, prepared.divisor(), obj, niv, piso, techo);
                if (Boolean.getBoolean("fitsense.factibilidad.traza"))
                    System.out.println("CONSTRUIDA " + c.id() + " " + puntuar(construida, ctx, prepared.divisor(), piso)
                            .mensajes() + " :: " + describir(construida, p));
                if (puntuar(construida, ctx, prepared.divisor(), piso).problemas() == 0) testigo = Optional.of(construida);
                else testigo = buscarTestigo(ctx, prepared.divisor(), construida, obj, niv, tope, piso);
            }
            if (testigo.isEmpty() && borrador != null)
                testigo = buscarTestigo(ctx, prepared.divisor(), borrador, obj, niv, tope, piso);
            if (testigo.isPresent()) {
                clasificacion = "CASO_A";
                causa = "Existe un plan que pasa V1-V25 y el motor no lo construye. Testigo: " + describir(testigo.get(), p);
            } else {
                clasificacion = "NO_DEMOSTRADO";
                causa = ("Cota %d s >= piso %d s y la busqueda local (%d pasos) no encontro un plan valido; "
                        + "no se demuestra ni existencia ni imposibilidad. Residuo: %s").formatted(cota, piso, PASOS,
                        String.join(" | ", ultimoResiduo) + " Estado final: " + ultimoEstado);
            }
        }

        var linea = String.join(",", c.id(), String.valueOf(u), semana.toString(), obj, niv, String.valueOf(c.dias()),
                String.valueOf(ctx.effectiveDaysPerWeek()), String.valueOf(c.minutos()), c.equipo(),
                c.restringido() ? "SIN_IMPACTO_SUELO_RODILLAS_COLGARSE_CABEZA" : "NINGUNA", c.bloqueo(),
                String.valueOf(ctx.availableExercises().size()), String.valueOf(costes.size()), String.valueOf(tope),
                String.valueOf(piso), String.valueOf(techo),
                presupuesto < 0 ? "NA" : String.valueOf((long) presupuesto), String.valueOf(cota),
                String.format(Locale.ROOT, "%.1f", cota / 60.0), String.valueOf(mMax), q(composicion),
                String.valueOf(borrador == null ? 0 : borrador.workouts().size()), q(String.join(" ", enfoques)),
                q(String.join(" ", ejercicios)), q(String.join(" ", segundos)), q(String.join(" ", declarados)),
                q(String.join(" ", codigos)), resultado, clasificacion, q(causa),
                q(String.join(" ", detalle)));
        System.out.println("FACTIBILIDAD " + c.id() + " " + obj + "/" + niv + "/" + c.dias() + "d/" + c.minutos()
                + "/" + c.equipo() + (c.restringido() ? "/R" : "") + ("-".equals(c.bloqueo()) ? "" : "/B")
                + " cota=" + cota + " piso=" + piso + " max=" + maxSesion + " " + resultado + " " + codigos + " "
                + clasificacion + (problemas.isEmpty() ? "" : " :: " + problemas.get(0)));
        return new Fila(linea, clasificacion);
    }

    /**
     * El mayor coste en segundos (trabajo + descansos entre series, sin transicion) que el validador admite para
     * este ejercicio en este objetivo y nivel: la dosis aprobada de cada capacidad admisible en su serie maxima.
     * 0 si el ejercicio no puede colocarse (sin capacidad del objetivo o sin dosis verificable).
     */
    static long costeMaximo(CandidateExercise c, String objetivo, String nivel, PrescriptionParams p) {
        int spr = p.secondsPerRepOrDefault();
        if (MvpDosePolicy.aplicaA(objetivo)) {
            if (c.defaultPrescription() == PrescriptionType.DURATION) {
                // 4D se abstiene en la unidad DURATION; la dosis que se aplica es la del normalizador (20.4).
                return 3L * 40 + 2L * 45;
            }
            var d = MvpDosePolicy.dosisPara(MandatorySlot.of(c), c.bodyPartCode());
            if (d.isEmpty()) return 0;
            short series = d.get().series();
            for (var f : WorkoutFocus.values())
                series = (short) Math.max(series, MvpDosePolicy.seriesMaxEn(d.get(), f, c.bodyPartCode()));
            return (long) series * d.get().reps() * spr + (long) (series - 1) * d.get().descansoSegundos();
        }
        long mejor = 0;
        var limites = p.repLimits();
        for (var cap : CapabilityPrescriptionPolicy.admisibles(c, objetivo, nivel)) {
            var d = CapabilityPrescriptionPolicy.dosis(cap, c, objetivo, nivel, limites);
            if (d.isEmpty()) continue;
            int s = Math.max(d.get().series(), d.get().seriesMax());
            long trabajo = d.get().tipo() == PrescriptionType.DURATION
                    ? (long) s * (d.get().segundos() == null ? 0 : d.get().segundos())
                    : (long) s * (d.get().reps() == null ? 0 : d.get().reps()) * spr;
            mejor = Math.max(mejor, trabajo + (long) (s - 1) * d.get().descanso());
        }
        return mejor;
    }

    // ------------------------------------------------------------------ cota con obligaciones semanales

    final Map<String, Boolean> obligacionesGimnasio = new HashMap<>();
    String ultimasObligaciones = "";

    record Pieza2(Set<String> cubre, long deficit) {}

    /**
     * Relajacion exacta de lo que la SEMANA obliga a colocar. Cada dia dura como mucho calentamiento + transiciones +
     * tope x c_max; cada pieza obligatoria (un slot de 4C exigible -con FUERZA en INCREASE_STRENGTH-, una capacidad
     * principal factible, el cardio de cada dia en LOSE_WEIGHT) ocupa una colocacion que dura como mucho su mejor
     * coste, asi que resta (c_max - coste) a la holgura de su dia. Una misma colocacion puede cubrir un slot y una
     * capacidad a la vez si existe un ejercicio de ese patron con dosis de esa capacidad. Se prueban TODOS los
     * repartos (busqueda exhaustiva). Ignora el enfoque, V15, V18, el techo y la distincion de ejercicios: solo
     * puede sobrestimar, asi que «no cabe» demuestra que no existe plan valido.
     */
    boolean obligacionesCaben(PlanGenerationContext ctx, String obj, String niv, int dias, int tope, long piso,
                              PrescriptionParams p) {
        boolean gm = MvpDosePolicy.aplicaA(obj);
        long cmax = 0;
        // coste por (slot o "-", capacidad)
        var coste = new HashMap<String, Long>();
        for (var c : ctx.availableExercises()) {
            String slot = MandatorySlot.of(c).map(Enum::name).orElse("-");
            if (gm) {
                long k = costeMaximo(c, obj, niv, p);
                if (k <= 0) continue;
                cmax = Math.max(cmax, k);
                coste.merge(slot + "|HYPERTROPHY", k, Math::max);
                continue;
            }
            for (var cap : CapabilityPrescriptionPolicy.admisibles(c, obj, niv)) {
                var d = CapabilityPrescriptionPolicy.dosis(cap, c, obj, niv, p.repLimits());
                if (d.isEmpty()) continue;
                long k = potencialDe(new Pieza(c, d.get()), p);
                cmax = Math.max(cmax, k);
                coste.merge(slot + "|" + cap.name(), k, Math::max);
            }
        }
        long holgura = (long) p.warmupMinutesOrDefault() * 60 + (long) (tope - 1) * p.transitionSecondsOrDefault()
                + tope * cmax - piso;
        if (holgura < 0) return false;
        final long cm = cmax;
        java.util.function.BiFunction<String, String, Long> mejor = (slot, cap) -> coste.entrySet().stream()
                .filter(e -> (slot == null || e.getKey().startsWith(slot + "|"))
                        && (cap == null || e.getKey().endsWith("|" + cap)))
                .mapToLong(Map.Entry::getValue).max().orElse(-1);

        var requisitos = new ArrayList<String>();
        boolean fuerzaEnSlots = "INCREASE_STRENGTH".equals(obj);
        if (main.web.services.fitsense.planning.domain.services.GoalFeasibilityPolicy.exigeSlotsResistidos(obj))
            for (var slot : main.web.services.fitsense.planning.domain.services.SlotCoveragePolicy.requiredOf(
                    main.web.services.fitsense.planning.domain.services.SlotCoveragePolicy.ofEligible(
                            ctx.availableExercises())))
                requisitos.add("SLOT:" + slot.name());
        var principales = gm ? Set.of(GoalCapabilityPolicy.Capability.HYPERTROPHY)
                : GoalCapabilityPolicy.forGoal(obj).map(GoalCapabilityPolicy.GoalCapabilities::primaries).orElse(Set.of());
        for (var cap : principales)
            if (mejor.apply(null, cap.name()) >= 0) requisitos.add("CAP:" + cap.name());

        // Piezas: un requisito solo, o un slot con una capacidad.
        var piezas = new ArrayList<Pieza2>();
        for (var r : requisitos) {
            long k = r.startsWith("SLOT:") ? mejor.apply(r.substring(5), fuerzaEnSlots ? "STRENGTH" : null)
                    : mejor.apply(null, r.substring(4));
            if (k >= 0) piezas.add(new Pieza2(Set.of(r), cm - k));
        }
        for (var s : requisitos) if (s.startsWith("SLOT:"))
            for (var t : requisitos) if (t.startsWith("CAP:")) {
                if (fuerzaEnSlots && !t.equals("CAP:STRENGTH")) continue;
                long k = mejor.apply(s.substring(5), t.substring(4));
                if (k >= 0) piezas.add(new Pieza2(Set.of(s, t), cm - k));
            }
        long[] libre = new long[dias];
        int[] usados = new int[dias];
        Arrays.fill(libre, holgura);
        // LOSE_WEIGHT: cardio en CADA sesion (V25).
        if ("LOSE_WEIGHT".equals(obj)) {
            long k = mejor.apply(null, "CARDIO");
            if (k < 0) return false;
            for (int i = 0; i < dias; i++) {
                libre[i] -= cm - k;
                usados[i]++;
                if (libre[i] < 0) return false;
            }
            requisitos.remove("CAP:CARDIO");
        }
        ultimasObligaciones = requisitos + " c_max=" + cm + " holgura/dia=" + holgura;
        return repartir(requisitos, new HashSet<>(), piezas, libre, usados, tope);
    }

    private boolean repartir(List<String> requisitos, Set<String> cubiertos, List<Pieza2> piezas, long[] libre,
                             int[] usados, int tope) {
        String r = requisitos.stream().filter(x -> !cubiertos.contains(x)).findFirst().orElse(null);
        if (r == null) return true;
        for (var pz : piezas) {
            if (!pz.cubre().contains(r)) continue;
            var visto = new HashSet<String>();
            for (int d = 0; d < libre.length; d++) {
                if (libre[d] < pz.deficit() || usados[d] >= tope) continue;
                if (!visto.add(libre[d] + "/" + usados[d])) continue;   // dias equivalentes: uno basta
                libre[d] -= pz.deficit();
                usados[d]++;
                var nuevos = new HashSet<>(cubiertos);
                nuevos.addAll(pz.cubre());
                boolean ok = repartir(requisitos, nuevos, piezas, libre, usados, tope);
                libre[d] += pz.deficit();
                usados[d]--;
                if (ok) return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ verificador constructivo

    record Pieza(CandidateExercise c, CapabilityPrescriptionPolicy.Dosis d) {}

    /**
     * Semana construida SIN el motor, para objetivos de capacidad: (1) cada slot de 4C exigible con una colocacion de
     * FUERZA, repartidos entre los dias; (2) cada capacidad principal factible del objetivo al menos una vez (V25; en
     * LOSE_WEIGHT, cardio cada dia); (3) el resto del tope de V24 con lo admisible mas largo del enfoque. Sin repetir
     * en el dia ni en dias pegados (V18), V22 en brazos, la mitad de V15 y los dos grupos; dosis aprobada de
     * CapabilityPrescriptionPolicy, con la serie maxima solo mientras falte para V20 y sin pasar del techo de V4.
     */
    PlanDraft construirTestigo(PlanGenerationContext ctx, int divisor, String obj, String niv, long piso, int techo) {
        var p = ctx.prescription();
        var split = ctx.suggestedSplit();
        var complementos = main.web.services.fitsense.planning.domain.services.CatalogEligibilityPolicy
                .complementos(ctx.availableExercises(), obj);
        // Por cada candidato, su pieza mas larga y su pieza de cada capacidad.
        var piezas = new ArrayList<Pieza>();
        for (var c : ctx.availableExercises())
            for (var cap : CapabilityPrescriptionPolicy.admisibles(c, obj, niv))
                CapabilityPrescriptionPolicy.dosis(cap, c, obj, niv, p.repLimits()).ifPresent(d -> piezas.add(new Pieza(c, d)));
        piezas.sort(Comparator.comparingLong((Pieza x) -> -potencialDe(x, p)).thenComparing(x -> x.c().exerciseId()));
        int n = split.size();
        var dias = new ArrayList<List<Pieza>>();
        for (int i = 0; i < n; i++) dias.add(new ArrayList<>());

        java.util.function.BiPredicate<Integer, Pieza> cabe = (i, x) -> {
            var foco = split.get(i).focus();
            var c = x.c();
            if (dias.get(i).size() >= CapabilityPrescriptionPolicy.MAX_COLOC_DIA) return false;
            if (dias.get(i).stream().anyMatch(y -> y.c().exerciseId().equals(c.exerciseId()))) return false;
            for (int j : new int[]{i - 1, i + 1})
                if (j >= 0 && j < n && Math.abs(split.get(j).date().toEpochDay() - split.get(i).date().toEpochDay()) == 1
                        && dias.get(j).stream().anyMatch(y -> y.c().exerciseId().equals(c.exerciseId()))) return false;
            boolean comp = complementos.contains(c.exerciseId());
            if (!comp && foco != WorkoutFocus.FULL_BODY && !foco.accepts(c.bodyPartCode())) return false;
            if ("upper arms".equals(c.bodyPartCode()) && c.targetMuscle() != null) {
                String m = c.targetMuscle().toLowerCase(Locale.ROOT);
                if (foco == WorkoutFocus.PUSH && m.contains("biceps")) return false;
                if (foco == WorkoutFocus.PULL && m.contains("triceps")) return false;
            }
            if (!comp) {   // la mitad de V15
                var trabajo = new ArrayList<>(dias.get(i).stream().filter(y -> !complementos.contains(y.c().exerciseId()))
                        .map(y -> y.c().bodyPartCode()).toList());
                trabajo.add(c.bodyPartCode());
                if (foco.bodyPartCodes().size() >= 3 && trabajo.size() >= 4) {
                    var cuenta = trabajo.stream().collect(Collectors.groupingBy(g -> g, Collectors.counting()));
                    if (cuenta.values().stream().anyMatch(k -> k > trabajo.size() / 2)) return false;
                }
            }
            return true;
        };

        // (1) slots de 4C con FUERZA
        boolean fuerzaPrincipal = GoalCapabilityPolicy.forGoal(obj).map(g -> g.primaries()
                .contains(GoalCapabilityPolicy.Capability.STRENGTH)).orElse(false);
        if (main.web.services.fitsense.planning.domain.services.GoalFeasibilityPolicy.exigeSlotsResistidos(obj)) {
            var exigibles = main.web.services.fitsense.planning.domain.services.SlotCoveragePolicy.requiredOf(
                    main.web.services.fitsense.planning.domain.services.SlotCoveragePolicy.ofEligible(ctx.availableExercises()));
            int k = 0;
            for (var slot : exigibles) {
                boolean puesto = false;
                for (int t = 0; t < n && !puesto; t++) {
                    int i = (k + t) % n;
                    for (var x : piezas) {
                        if (!MandatorySlot.of(x.c()).equals(Optional.of(slot))) continue;
                        if (fuerzaPrincipal && x.d().capacidad() != GoalCapabilityPolicy.Capability.STRENGTH) continue;
                        if (!cabe.test(i, x)) continue;
                        dias.get(i).add(x);
                        puesto = true;
                        break;
                    }
                }
                k++;
            }
        }
        // (2) principales factibles, una vez en la semana (LOSE_WEIGHT: cardio cada dia)
        var principales = GoalCapabilityPolicy.forGoal(obj).map(GoalCapabilityPolicy.GoalCapabilities::primaries)
                .orElse(Set.of());
        for (var cap : principales) {
            var dePrincipal = piezas.stream().filter(x -> x.d().capacidad() == cap).toList();
            if (dePrincipal.isEmpty()) continue;
            boolean cadaDia = "LOSE_WEIGHT".equals(obj) && cap == GoalCapabilityPolicy.Capability.CARDIO;
            // Al dia con menos piezas obligatorias: repartir lo corto (movilidad, resistencia) entre los dias.
            var orden = java.util.stream.IntStream.range(0, n).boxed()
                    .sorted(Comparator.comparingInt((Integer i) -> dias.get(i).size()).thenComparing(i -> i)).toList();
            for (int i : orden) {
                final int dia = i;
                if (!cadaDia && dias.stream().anyMatch(d -> d.stream().anyMatch(x -> x.d().capacidad() == cap))) break;
                if (dias.get(i).stream().anyMatch(x -> x.d().capacidad() == cap)) continue;
                dePrincipal.stream().filter(x -> cabe.test(dia, x)).findFirst().ifPresent(dias.get(dia)::add);
            }
        }
        // (3) lo mas largo hasta el tope
        for (int i = 0; i < n; i++)
            for (var x : piezas) {
                if (dias.get(i).size() >= CapabilityPrescriptionPolicy.MAX_COLOC_DIA) break;
                if (cabe.test(i, x)) dias.get(i).add(x);
            }
        // dosis: base y, mientras falte para V20, la serie maxima de una en una (sin pasar del techo)
        var workouts = new ArrayList<PlanDraft.DraftWorkout>();
        for (int i = 0; i < n; i++) {
            var ej = new ArrayList<PlanDraft.DraftExercise>();
            for (var x : dias.get(i)) ej.add(x.d().prescribir(x.c().exerciseId(), null));
            var w = new PlanDraft.DraftWorkout(split.get(i).date(), split.get(i).focus(), "Testigo", 0, ej);
            for (int k = 0; k < ej.size() && ESTIMADOR.estimateSeconds(w, p) < piso; k++) {
                var x = dias.get(i).get(k);
                if (x.d().seriesMax() <= x.d().series()) continue;
                var mas = x.d().conSeries(x.d().seriesMax());
                if (mas.isEmpty()) continue;
                var antes = ej.get(k);
                ej.set(k, mas.get().prescribir(x.c().exerciseId(), null));
                w = new PlanDraft.DraftWorkout(w.scheduledDate(), w.focus(), "Testigo", 0, ej);
                if (ESTIMADOR.estimateMinutes(w, p) > techo) {
                    ej.set(k, antes);
                    w = new PlanDraft.DraftWorkout(w.scheduledDate(), w.focus(), "Testigo", 0, ej);
                }
            }
            workouts.add(new PlanDraft.DraftWorkout(w.scheduledDate(), w.focus(), "Testigo", 0, List.copyOf(ej)));
        }
        return recalcular(new PlanDraft(GenerationSource.RULE_ENGINE, null, "Testigo", null, "testigo", workouts),
                p, divisor);
    }

    private static long potencialDe(Pieza x, PrescriptionParams p) {
        var d = x.d();
        int s = Math.max(d.series(), d.seriesMax());
        long trabajo = d.tipo() == PrescriptionType.DURATION ? (long) s * (d.segundos() == null ? 0 : d.segundos())
                : (long) s * (d.reps() == null ? 0 : d.reps()) * p.secondsPerRepOrDefault();
        return trabajo + (long) (s - 1) * d.descanso();
    }

    // ------------------------------------------------------------------ verificador independiente (testigos)

    static final int PASOS = 40;
    static final int MOVIMIENTOS_POR_PASO = 90;
    static final Pattern FECHA = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");

    /**
     * Busqueda local sobre el borrador del motor: en las sesiones que el validador senala, sube series hasta el maximo
     * admitido, anade colocaciones (hasta el tope de V24) o sustituye una colocacion por otro ejercicio elegible, siempre
     * con la dosis APROBADA de una capacidad admisible (nada artificial, ningun descanso extra). Acepta un movimiento
     * solo si baja (problemas, deficit de V20). Duracion declarada = estimada. Devuelve un plan que pasa el validador de
     * produccion completo, o vacio.
     */
    Optional<PlanDraft> buscarTestigo(PlanGenerationContext ctx, int divisor, PlanDraft inicial, String obj, String niv,
                                     int tope, long piso) {
        var p = ctx.prescription();
        var pool = ctx.availableExercises().stream().filter(c -> costeMaximo(c, obj, niv, p) > 0)
                .sorted(Comparator.comparingLong((CandidateExercise c) -> -costeMaximo(c, obj, niv, p))).toList();
        var porId = ctx.availableExercises().stream()
                .collect(Collectors.toMap(CandidateExercise::exerciseId, c -> c, (a, b) -> a));
        var actual = recalcular(inicial, p, divisor);
        var puntos = puntuar(actual, ctx, divisor, piso);
        for (int paso = 0; paso < PASOS && puntos.problemas() > 0; paso++) {
            var senaladas = new HashSet<LocalDate>();
            for (var pr : puntos.mensajes()) {
                var m = FECHA.matcher(pr);
                while (m.find()) senaladas.add(LocalDate.parse(m.group(1)));
            }
            var porGanancia = new ArrayList<Map.Entry<Long, PlanDraft>>();
            var nuevosGrupos = new ArrayList<Map.Entry<Long, PlanDraft>>();     // con la dosis base
            var nuevosGruposMax = new ArrayList<Map.Entry<Long, PlanDraft>>();  // con la serie maxima
            for (int i = 0; i < actual.workouts().size(); i++) {
                var w = actual.workouts().get(i);
                if (!senaladas.isEmpty() && !senaladas.contains(w.scheduledDate())) continue;
                String dia = w.scheduledDate().toString();
                boolean v15 = puntos.mensajes().stream().anyMatch(x -> x.startsWith("V15") && x.contains(dia));
                long base = ESTIMADOR.estimateSeconds(w, p);
                var ids = w.exercises().stream().map(PlanDraft.DraftExercise::exerciseId).collect(Collectors.toSet());
                var grupos = w.exercises().stream().map(e -> porId.containsKey(e.exerciseId())
                        ? porId.get(e.exerciseId()).bodyPartCode() : "?").collect(Collectors.toSet());
                var foco = w.focus();
                var candidatos = new ArrayList<CandidateExercise>();
                var porGrupo = new HashMap<String, Integer>();
                for (var c : pool) {
                    if (ids.contains(c.exerciseId())) continue;
                    boolean enFoco = foco.bodyPartCodes().contains(c.bodyPartCode())
                            || CapabilityPrescriptionPolicy.claseDe(c) == CapabilityPrescriptionPolicy.Clase.CARDIO;
                    if (!enFoco) continue;
                    int n = porGrupo.merge(c.bodyPartCode(), 1, Integer::sum);
                    if (candidatos.size() < 30 || n <= 8) candidatos.add(c);
                }
                for (int k = 0; k < w.exercises().size(); k++) {
                    var e = w.exercises().get(k);
                    var c = porId.get(e.exerciseId());
                    if (c != null) for (var v : variantes(c, obj, niv, foco, p))
                        if (v.plannedSets() > (e.plannedSets() == null ? 0 : e.plannedSets()))
                            porGanancia.add(mover(actual, i, k, v, p, divisor, base, false));
                    for (var c2 : candidatos) {
                        var vs = variantes(c2, obj, niv, foco, p);
                        for (var v : vs) {
                            var mv = mover(actual, i, k, v, p, divisor, base, false);
                            if (v15 && !grupos.contains(c2.bodyPartCode()))
                                (v == vs.get(0) ? nuevosGrupos : nuevosGruposMax).add(mv);
                            else porGanancia.add(mv);
                        }
                    }
                }
                if (w.exercises().size() < tope)
                    for (var c2 : candidatos) for (var v : variantes(c2, obj, niv, foco, p))
                        porGanancia.add(mover(actual, i, -1, v, p, divisor, base, true));
            }
            porGanancia.sort((a, b) -> Long.compare(b.getKey(), a.getKey()));
            nuevosGrupos.sort((a, b) -> Long.compare(b.getKey(), a.getKey()));
            nuevosGruposMax.sort((a, b) -> Long.compare(b.getKey(), a.getKey()));
            var movimientos = new ArrayList<>(nuevosGrupos.subList(0, Math.min(40, nuevosGrupos.size())));
            movimientos.addAll(nuevosGruposMax.subList(0, Math.min(20, nuevosGruposMax.size())));
            movimientos.addAll(porGanancia.subList(0, Math.min(MOVIMIENTOS_POR_PASO, porGanancia.size())));
            Puntos mejor = puntos;
            PlanDraft elegido = null;
            for (var mv : movimientos) {
                var pt = puntuar(mv.getValue(), ctx, divisor, piso);
                if (pt.mejorQue(mejor)) {
                    mejor = pt;
                    elegido = mv.getValue();
                    if (pt.problemas() == 0) break;
                }
            }
            if (elegido == null) {
                if (Boolean.getBoolean("fitsense.factibilidad.traza") && !movimientos.isEmpty())
                    for (var mv : movimientos.subList(0, Math.min(6, movimientos.size())))
                        System.out.println("TESTIGO_ATASCADO candidato=" + puntuar(mv.getValue(), ctx, divisor, piso)
                                .mensajes() + " :: " + describir(mv.getValue(), p));
                break;
            }
            actual = elegido;
            puntos = mejor;
        }
        ultimoResiduo = puntos.mensajes();
        ultimoEstado = describir(actual, p);
        return puntos.problemas() == 0 ? Optional.of(actual) : Optional.empty();
    }

    /** Lo que el validador seguia rechazando al terminar la ultima busqueda (para el informe). */
    List<String> ultimoResiduo = List.of();
    String ultimoEstado = "";

    /**
     * Orden lexicografico: primero los problemas que NO son de duracion (un movimiento que arregla V20 rompiendo V25
     * o V23 no se acepta), luego los de V20 y luego el deficit total de V20 en segundos.
     */
    record Puntos(int problemas, int otros, long deficit, List<String> mensajes) {
        boolean mejorQue(Puntos o) {
            if (otros != o.otros) return otros < o.otros;
            if (problemas != o.problemas) return problemas < o.problemas;
            return deficit < o.deficit;
        }
    }

    private Puntos puntuar(PlanDraft d, PlanGenerationContext ctx, int divisor, long piso) {
        long deficit = 0;
        for (var w : d.workouts()) deficit += Math.max(0, piso - ESTIMADOR.estimateSeconds(w, ctx.prescription()));
        try {
            validador.validate(d, ctx, divisor);
            return new Puntos(0, 0, deficit, List.of());
        } catch (InvalidPlanDraftException e) {
            int otros = (int) e.problems().stream().filter(x -> !x.trim().startsWith("V20")).count();
            return new Puntos(e.problems().size(), otros, deficit, e.problems());
        } catch (RuntimeException e) {
            return new Puntos(Integer.MAX_VALUE, Integer.MAX_VALUE, deficit, List.of(String.valueOf(e)));
        }
    }

    /** Las dosis aprobadas de un ejercicio en este objetivo: la base y, si existe, la serie maxima admitida. */
    static List<PlanDraft.DraftExercise> variantes(CandidateExercise c, String obj, String niv, WorkoutFocus foco,
                                                  PrescriptionParams p) {
        var out = new ArrayList<PlanDraft.DraftExercise>();
        if (MvpDosePolicy.aplicaA(obj)) {
            if (c.defaultPrescription() == PrescriptionType.DURATION) return out;
            var d = MvpDosePolicy.dosisPara(MandatorySlot.of(c), c.bodyPartCode());
            if (d.isEmpty()) return out;
            var x = d.get();
            out.add(new PlanDraft.DraftExercise(c.exerciseId(), PrescriptionType.SETS_REPS, x.series(), x.reps(), null,
                    null, x.descansoSegundos(), null));
            short max = MvpDosePolicy.seriesMaxEn(x, foco, c.bodyPartCode());
            if (max > x.series())
                out.add(new PlanDraft.DraftExercise(c.exerciseId(), PrescriptionType.SETS_REPS, max, x.reps(), null,
                        null, x.descansoSegundos(), null));
            return out;
        }
        for (var cap : CapabilityPrescriptionPolicy.admisibles(c, obj, niv)) {
            var d = CapabilityPrescriptionPolicy.dosis(cap, c, obj, niv, p.repLimits());
            if (d.isEmpty()) continue;
            var x = d.get();
            for (short s : new short[]{x.series(), x.seriesMax()}) {
                var e = new PlanDraft.DraftExercise(c.exerciseId(), x.tipo(), s, x.reps(), x.segundos(), null,
                        x.descanso(), x.nota());
                if (!out.contains(e)) out.add(e);
            }
        }
        return out;
    }

    private static Map.Entry<Long, PlanDraft> mover(PlanDraft d, int i, int k, PlanDraft.DraftExercise nuevo,
                                                   PrescriptionParams p, int divisor, long base, boolean anadir) {
        var w = d.workouts().get(i);
        var ej = new ArrayList<>(w.exercises());
        if (anadir) ej.add(nuevo);
        else ej.set(k, nuevo);
        var nw = new PlanDraft.DraftWorkout(w.scheduledDate(), w.focus(), w.name(), w.expectedDurationMinutes(), ej);
        var ws = new ArrayList<>(d.workouts());
        ws.set(i, nw);
        var r = recalcular(new PlanDraft(d.source(), d.modelName(), d.planName(), d.declaredTotalVolume(),
                d.rationale(), ws), p, divisor);
        return Map.entry(ESTIMADOR.estimateSeconds(r.workouts().get(i), p) - base, r);
    }

    /** Duracion declarada = la que calcula el estimador; volumen declarado = el del contenido. */
    static PlanDraft recalcular(PlanDraft d, PrescriptionParams p, int divisor) {
        var ws = d.workouts().stream().map(w -> new PlanDraft.DraftWorkout(w.scheduledDate(), w.focus(), w.name(),
                ESTIMADOR.estimateMinutes(w, p), w.exercises())).toList();
        var sin = new PlanDraft(d.source(), d.modelName(), d.planName(), null, d.rationale(), ws);
        return new PlanDraft(d.source(), d.modelName(), d.planName(), sin.volume(divisor), d.rationale(), ws);
    }

    static String describir(PlanDraft d, PrescriptionParams p) {
        return d.workouts().stream().map(w -> w.scheduledDate().getDayOfWeek().name().substring(0, 3) + "/" + w.focus()
                + "/" + ESTIMADOR.estimateSeconds(w, p) + "s/" + w.expectedDurationMinutes() + "min[" + w.exercises()
                .stream().map(e -> e.exerciseId() + "=" + e.plannedSets() + "x" + (e.prescriptionType()
                        == PrescriptionType.DURATION ? e.plannedDurationSeconds() + "s" : String.valueOf(e.plannedReps()))
                        + "/" + e.restSeconds()).collect(Collectors.joining(" ")) + "]")
                .collect(Collectors.joining(" "));
    }

    private static String q(String s) {
        return "\"" + s.replace("\"", "'") + "\"";
    }

    private long usuario() {
        String email = "factibilidad" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Factibilidad', now(), now())", email);
        return jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
    }
}
