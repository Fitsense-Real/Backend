package main.web.services.fitsense.rendimiento;

import main.web.services.fitsense.RelojPruebas;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.*;
import main.web.services.fitsense.planning.infrastructure.generation.rules.RuleBasedTrainingPlanGenerator;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AUDITORIA DE LA PR #54 (12/11/2026) · ¿PUEDE LA COMPROBACION PREVIA DESCARTAR UNA PROPUESTA VALIDA?
 * <p>
 * Verificacion INDEPENDIENTE de {@link FactibilidadGeneracionIA}: el unico juez es el validador real
 * ({@link PlanDraftValidator}, el que decide si una propuesta de la IA se publica), con el catalogo, el contexto y el
 * estimador reales. Para cada perfil de una sesion se generan cientos de borradores "como de la IA" (el plan del motor
 * de reglas y sus variantes, construcciones aleatorias que cubren lo exigido, mutaciones de los aceptados y dosis
 * perturbadas fuera de las exactas) y se comprueba, para TODO borrador que el validador acepta:
 * <pre>   cota_inferior &lt;= duracion(P)</pre>
 * y que ningun perfil marcado inviable tiene un borrador aceptado. La busqueda no demuestra ausencia universal de
 * contraejemplos: los busca. Ademas: perfiles historicos con IA real (GRA, CONF-IA, campanas MVP y de estres) y el
 * analisis de H-G9 frente al motor de reglas.
 * <p>
 * Laboratorio opt-in ({@code -Dfitsense.auditoria.factibilidad=true}); escribe CSV en
 * {@code fitsense.auditoria.salida} (por omision {@code target/auditoria-factibilidad}). Sin Replicate.
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.auditoria.factibilidad", matches = "true")
@SpringBootTest(properties = "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef")
@DisplayName("Auditoria PR #54 · comprobacion previa frente al validador real, historicos y H-G9")
class AuditoriaFactibilidadIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired WeeklyTrainingPlanCommandService planes;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanDraftValidator validador;
    @Autowired SessionDurationEstimator estimador;
    @Autowired @Qualifier("ruleBasedTrainingPlanGenerator") TrainingPlanGenerator reglas;
    @Autowired JdbcTemplate jdbc;

    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final LocalDate S1 = RelojPruebas.SEMANA_REFERENCIA;
    static final List<String> GIMNASIO = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine",
            "bench", "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine");
    static final List<String> CASA = List.of("body weight");
    static final int BORRADORES = Integer.getInteger("fitsense.auditoria.borradores", 400);
    static final Path SALIDA = Path.of(System.getProperty("fitsense.auditoria.salida", "target/auditoria-factibilidad"));

    record Perfil(String id, GoalType objetivo, FitnessLevel nivel, TrainingLocation lugar, List<String> equipo,
                  int minutos, short dia, boolean impacto, boolean suelo, boolean rodillas, boolean colgado,
                  boolean sobreCabeza) {
        Perfil(String id, GoalType o, FitnessLevel n, TrainingLocation l, List<String> e, int m) {
            this(id, o, n, l, e, m, (short) 2, true, true, true, true, true);
        }
    }

    record Fila(String id, String objetivo, String nivel, String lugar, int minutos, String alcance, long cota,
                long limite, String decision, int probados, int aceptados, long minAceptada, int dosisFueraAceptadas,
                String nota) {
        String csv() {
            return String.join(",", id, objetivo, nivel, lugar, String.valueOf(minutos), alcance, String.valueOf(cota),
                    String.valueOf(limite), decision, String.valueOf(probados), String.valueOf(aceptados),
                    minAceptada == Long.MAX_VALUE ? "" : String.valueOf(minAceptada),
                    String.valueOf(dosisFueraAceptadas), "\"" + nota.replace("\"", "'") + "\"");
        }
    }

    @Test
    void auditar() throws IOException {
        Files.createDirectories(SALIDA);
        var filas = new ArrayList<Fila>();

        // 1 · Rejilla: cuatro objetivos x tres niveles x gimnasio/casa x 30-75 min, una sesion.
        var rejilla = new ArrayList<Perfil>();
        for (var o : GoalType.values())
            for (var n : FitnessLevel.values())
                for (var l : List.of(TrainingLocation.GYM, TrainingLocation.HOME))
                    for (int m = 30; m <= 75; m += 5)
                        rejilla.add(new Perfil("R", o, n, l, l == TrainingLocation.GYM ? GIMNASIO : CASA, m));
        // 2 · Umbral: GENERAL_FITNESS intermedio/avanzado minuto a minuto alrededor de la cota (2265 s = 37,75 min).
        for (var n : List.of(FitnessLevel.INTERMEDIATE, FitnessLevel.ADVANCED))
            for (var l : List.of(TrainingLocation.GYM, TrainingLocation.HOME))
                for (int m = 30; m <= 45; m++)
                    if (m % 5 != 0) rejilla.add(new Perfil("U", GoalType.GENERAL_FITNESS, n, l,
                            l == TrainingLocation.GYM ? GIMNASIO : CASA, m));
        // 3 · Historicos de una sesion con IA real (los de dos o mas sesiones quedan fuera del alcance por codigo).
        var historicos = List.of(
                new Perfil("O10", GoalType.LOSE_WEIGHT, FitnessLevel.ADVANCED, TrainingLocation.HOME, CASA, 30,
                        (short) 6, true, true, true, true, true),
                new Perfil("O11", GoalType.INCREASE_STRENGTH, FitnessLevel.ADVANCED, TrainingLocation.HOME,
                        List.of("body weight", "dumbbell", "band"), 30, (short) 3, true, true, true, true, true),
                new Perfil("O11-F35", GoalType.INCREASE_STRENGTH, FitnessLevel.ADVANCED, TrainingLocation.HOME,
                        List.of("body weight", "dumbbell", "band"), 35, (short) 3, true, true, true, true, true),
                new Perfil("MVP-U04", GoalType.GENERAL_FITNESS, FitnessLevel.BEGINNER, TrainingLocation.HOME, CASA,
                        30, (short) 6, false, true, true, false, true),
                new Perfil("ESTRES-U05", GoalType.INCREASE_STRENGTH, FitnessLevel.BEGINNER, TrainingLocation.HOME,
                        List.of("body weight", "dumbbell", "band"), 45, (short) 3, true, true, true, true, true),
                new Perfil("ESTRES-U07", GoalType.GENERAL_FITNESS, FitnessLevel.BEGINNER, TrainingLocation.HOME, CASA,
                        15, (short) 6, false, true, true, false, true));
        var todos = new ArrayList<Perfil>(rejilla);
        todos.addAll(historicos);

        var hg9 = new ArrayList<String>();
        for (var p : todos) {
            var fila = auditar(p, hg9);
            filas.add(fila);
            System.out.println("AUD54 " + fila.csv());
        }

        var csv = new StringBuilder("id,objetivo,nivel,lugar,minutos,alcance,cota_s,limite_s,decision,borradores,"
                + "aceptados_por_el_validador,duracion_minima_aceptada_s,dosis_fuera_de_exactas_aceptadas,nota\n");
        filas.forEach(f -> csv.append(f.csv()).append('\n'));
        Files.writeString(SALIDA.resolve("AUDITORIA_COTA_FRENTE_AL_VALIDADOR.csv"), csv);
        Files.writeString(SALIDA.resolve("HG9_MOTOR_DE_REGLAS.txt"), String.join("\n", hg9) + "\n");

        long violaciones = filas.stream().filter(f -> f.cota() >= 0 && f.minAceptada() < f.cota()).count();
        long inviablesConAceptado = filas.stream().filter(f -> f.decision().equals("INVIABLE") && f.aceptados() > 0).count();
        long fueraAceptadas = filas.stream().mapToLong(Fila::dosisFueraAceptadas).sum();
        System.out.println("AUD54_RESUMEN perfiles=" + filas.size()
                + " evaluados=" + filas.stream().filter(f -> !f.alcance().equals("SIN_GENERACION")).count()
                + " en_alcance=" + filas.stream().filter(f -> f.alcance().equals("EN_ALCANCE")).count()
                + " inviables=" + filas.stream().filter(f -> f.decision().equals("INVIABLE")).count()
                + " con_borrador_aceptado=" + filas.stream().filter(f -> f.aceptados() > 0).count()
                + " borradores=" + filas.stream().mapToInt(Fila::probados).sum()
                + " aceptados=" + filas.stream().mapToInt(Fila::aceptados).sum()
                + " violaciones_cota=" + violaciones + " inviables_con_aceptado=" + inviablesConAceptado
                + " dosis_fuera_de_exactas_aceptadas=" + fueraAceptadas);
        assertEquals(0, violaciones, "la cota supero la duracion de un borrador aceptado por el validador");
        assertEquals(0, inviablesConAceptado, "un perfil marcado inviable tiene un borrador aceptado");
    }

    private Fila auditar(Perfil p, List<String> hg9) {
        long u;
        PlanGenerationContext ctx;
        try {
            u = participante(p);
            ctx = planes.prepare(GenerateWeeklyPlanCommand.firstPlan(u, S1)).context();
        } catch (RuntimeException e) {
            String m = e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage());
            return new Fila(p.id(), p.objetivo().name(), p.nivel().name(), p.lugar().name(), p.minutos(),
                    "SIN_GENERACION", -1, -1, "NO_APLICA", 0, 0, Long.MAX_VALUE, 0,
                    m.substring(0, Math.min(140, m.length())));
        }
        var calculo = FactibilidadGeneracionIA.calcular(ctx, validador.coverageEnforcement(),
                validador.capabilityEnforcement());
        var decision = FactibilidadGeneracionIA.evaluar(ctx, validador.coverageEnforcement(),
                validador.capabilityEnforcement());
        String alcance = calculo.isPresent() ? "EN_ALCANCE"
                : ctx.effectiveDaysPerWeek() != 1 ? "FUERA_VARIAS_SESIONES" : "FUERA_DE_ALCANCE";
        long cota = calculo.map(FactibilidadGeneracionIA.Inviabilidad::segundosMinimos).orElse(-1L);
        long limite = calculo.map(FactibilidadGeneracionIA.Inviabilidad::limiteSegundos).orElse(-1L);

        var r = new Random(Objects.hash(p.objetivo(), p.nivel(), p.lugar(), p.minutos(), p.id()));
        var resultado = buscar(ctx, r);
        String nota = "";
        if (decision.isPresent()) {
            nota = analizarRespaldo(ctx, p, cota, hg9);
        }
        return new Fila(p.id(), p.objetivo().name(), p.nivel().name(), p.lugar().name(), p.minutos(), alcance, cota,
                limite, decision.isPresent() ? "INVIABLE" : "SE_LLAMA_A_LA_IA", resultado.probados,
                resultado.aceptados, resultado.minAceptada, resultado.dosisFueraAceptadas, nota);
    }

    static final class Resultado {
        int probados, aceptados, dosisFueraAceptadas;
        long minAceptada = Long.MAX_VALUE;
    }

    /** Busqueda de borradores que el validador real acepte como propuesta de la IA. */
    private Resultado buscar(PlanGenerationContext ctx, Random r) {
        var res = new Resultado();
        String objetivo = ctx.profile().goalType(), nivel = ctx.profile().fitnessLevel();
        var limites = ctx.prescription() == null ? null : ctx.prescription().repLimits();
        LocalDate fecha = fechaDisponible(ctx);
        WorkoutFocus foco = WorkoutFocus.FULL_BODY;

        // Colocaciones: cada ejercicio elegible con cada dosis exacta admisible, en todas sus series permitidas.
        var colocaciones = new ArrayList<PlanDraft.DraftExercise>();
        var porSlot = new HashMap<MandatorySlot, List<PlanDraft.DraftExercise>>();
        var porCapacidad = new HashMap<GoalCapabilityPolicy.Capability, List<PlanDraft.DraftExercise>>();
        var candidatos = new LinkedHashMap<Long, CandidateExercise>();
        ctx.availableExercises().forEach(c -> candidatos.putIfAbsent(c.exerciseId(), c));
        for (var c : candidatos.values())
            for (var cap : CapabilityPrescriptionPolicy.admisibles(c, objetivo, nivel)) {
                var d = CapabilityPrescriptionPolicy.dosis(cap, c, objetivo, nivel, limites);
                if (d.isEmpty()) continue;
                for (short n = d.get().series(); n <= d.get().seriesMax(); n++) {
                    var e = d.get().conSeries(n).orElseThrow().prescribir(c.exerciseId(), null);
                    colocaciones.add(e);
                    porCapacidad.computeIfAbsent(cap, k -> new ArrayList<>()).add(e);
                    CoberturaDePatron.slot(c).ifPresent(s -> porSlot.computeIfAbsent(s, k -> new ArrayList<>()).add(e));
                }
            }

        var borradores = new ArrayList<List<PlanDraft.DraftExercise>>();
        // a) El plan del motor de reglas y sus variantes deterministas.
        try {
            var d = reglas.generate(ctx, List.of());
            if (!d.workouts().isEmpty()) {
                foco = d.workouts().get(0).focus();
                borradores.add(d.workouts().get(0).exercises());
            }
        } catch (RuntimeException ignorado) {
            // sin plan base
        }
        if (reglas instanceof RuleBasedTrainingPlanGenerator rb)
            for (int k = 1; k <= 4; k++)
                try {
                    var d = rb.generarVariante(ctx, List.of(), k);
                    if (!d.workouts().isEmpty()) borradores.add(d.workouts().get(0).exercises());
                } catch (RuntimeException ignorado) {
                    // siguiente
                }
        var aceptados = new ArrayList<List<PlanDraft.DraftExercise>>();
        var exigidas = new ArrayList<>(CapabilityCoveragePolicy.evaluar(objetivo, nivel,
                new PlanDraft(GenerationSource.AI, "a", "a", null, "a", List.of()), candidatos.values(), limites)
                .omitidas());
        var slots = SlotCoveragePolicy.requiredOf(SlotCoveragePolicy.ofEligible(ctx.availableExercises()));

        int i = 0;
        while (res.probados < BORRADORES && !colocaciones.isEmpty()) {
            List<PlanDraft.DraftExercise> lista;
            if (i < borradores.size()) {
                lista = borradores.get(i);
            } else if (!aceptados.isEmpty() && r.nextInt(3) == 0) {
                // c) Mutar un aceptado: cambiar, quitar o anadir una colocacion.
                lista = new ArrayList<>(aceptados.get(r.nextInt(aceptados.size())));
                int op = r.nextInt(3);
                if (op == 0 && !lista.isEmpty()) lista.set(r.nextInt(lista.size()), colocaciones.get(r.nextInt(colocaciones.size())));
                else if (op == 1 && lista.size() > 2) lista.remove(r.nextInt(lista.size()));
                else lista.add(colocaciones.get(r.nextInt(colocaciones.size())));
            } else if (!aceptados.isEmpty() && r.nextInt(4) == 0) {
                // d) Dosis fuera de las exactas (repeticiones, segundos o descanso cambiados): no deberia pasar.
                lista = new ArrayList<>(aceptados.get(r.nextInt(aceptados.size())));
                int k = r.nextInt(lista.size());
                var e = lista.get(k);
                lista.set(k, new PlanDraft.DraftExercise(e.exerciseId(), e.prescriptionType(), e.plannedSets(),
                        e.plannedReps() == null ? null : (short) Math.max(1, e.plannedReps() - 1),
                        e.plannedDurationSeconds() == null ? null : Math.max(5, e.plannedDurationSeconds() - 5),
                        e.targetLoadKg(), e.restSeconds() == null ? null : (short) Math.max(0, e.restSeconds() - 15),
                        e.notes()));
                if (aceptar(ctx, fecha, foco, lista, res, true)) aceptados.add(lista);
                i++;
                continue;
            } else {
                // b) Construccion aleatoria: una colocacion por capacidad exigida, los slots exigidos (no siempre:
                //    con pocas colocaciones V23 no aplica) y 0-2 extras.
                lista = new ArrayList<>();
                for (var cap : exigidas) {
                    var op = porCapacidad.getOrDefault(cap, List.of());
                    if (!op.isEmpty()) lista.add(op.get(r.nextInt(op.size())));
                }
                if (r.nextInt(10) < 7)
                    for (var s : slots) {
                        var op = porSlot.getOrDefault(s, List.of());
                        if (!op.isEmpty()) lista.add(op.get(r.nextInt(op.size())));
                    }
                for (int x = r.nextInt(3); x > 0; x--) lista.add(colocaciones.get(r.nextInt(colocaciones.size())));
                Collections.shuffle(lista, r);
            }
            i++;
            if (aceptar(ctx, fecha, foco, lista, res, false)) aceptados.add(lista);
        }
        return res;
    }

    private boolean aceptar(PlanGenerationContext ctx, LocalDate fecha, WorkoutFocus foco,
                            List<PlanDraft.DraftExercise> ejercicios, Resultado res, boolean fueraDeExactas) {
        if (ejercicios.isEmpty()) return false;
        res.probados++;
        var base = new PlanDraft.DraftWorkout(fecha, foco, "auditoria", 0, ejercicios);
        int minutos = estimador.estimateMinutes(base, ctx.prescription());
        var w = new PlanDraft.DraftWorkout(fecha, foco, "auditoria", minutos, ejercicios);
        var draft = new PlanDraft(GenerationSource.AI, "auditoria", "auditoria", null, "auditoria", List.of(w));
        try {
            validador.validate(draft, ctx, 30);
        } catch (InvalidPlanDraftException e) {
            return false;
        }
        res.aceptados++;
        if (fueraDeExactas) res.dosisFueraAceptadas++;
        res.minAceptada = Math.min(res.minAceptada, estimador.estimateSeconds(w, ctx.prescription()));
        return true;
    }

    /** H-G9: que publica el motor de reglas, si pasa como RULE_ENGINE, y por que no pasaria como IA. */
    private String analizarRespaldo(PlanGenerationContext ctx, Perfil p, long cota, List<String> salida) {
        var sinV23 = FactibilidadGeneracionIA.calcular(ctx, SlotCoverageEnforcement.OBSERVE,
                validador.capabilityEnforcement());
        var linea = new StringBuilder();
        linea.append(p.id()).append(' ').append(p.objetivo()).append(' ').append(p.nivel()).append(' ')
                .append(p.lugar()).append(' ').append(p.minutos()).append(" min · cota con V23 ").append(cota)
                .append(" s · cota SIN V23 ").append(sinV23.map(FactibilidadGeneracionIA.Inviabilidad::segundosMinimos)
                        .orElse(-1L)).append(" s · techo V4 ").append(ctx.policy().limiteV4SegundosDe(p.minutos()))
                .append(" s");
        try {
            var d = reglas.generate(ctx, List.of());
            var w = d.workouts().get(0);
            linea.append(" · motor de reglas: ").append(w.exercises().size()).append(" colocaciones, ")
                    .append(estimador.estimateSeconds(w, ctx.prescription())).append(" s");
            var cubiertos = SlotCoveragePolicy.ofDraft(d, ctx.availableExercises().stream()
                    .collect(java.util.stream.Collectors.toMap(CandidateExercise::exerciseId, c -> c, (a, b) -> a)));
            linea.append(", slots ").append(cubiertos.describe());
            linea.append(" · valida como RULE_ENGINE: ").append(codigos(d, ctx));
            var comoIa = new PlanDraft(GenerationSource.AI, d.modelName(), d.planName(), d.declaredTotalVolume(),
                    d.rationale(), d.workouts());
            linea.append(" · el mismo plan como IA: ").append(codigos(comoIa, ctx));
        } catch (RuntimeException e) {
            linea.append(" · motor de reglas: ").append(e.getClass().getSimpleName());
        }
        salida.add(linea.toString());
        System.out.println("AUD54_HG9 " + linea);
        return "motor de reglas analizado (HG9_MOTOR_DE_REGLAS.txt)";
    }

    private String codigos(PlanDraft d, PlanGenerationContext ctx) {
        try {
            validador.validate(d, ctx, 30);
            return "PASA";
        } catch (InvalidPlanDraftException e) {
            var c = new TreeSet<String>();
            for (var m : e.problems()) c.add(m.contains(":") ? m.substring(0, m.indexOf(':')) : m);
            return "RECHAZA " + c;
        }
    }

    private static LocalDate fechaDisponible(PlanGenerationContext ctx) {
        for (var f = ctx.weekStartDate(); !f.isAfter(ctx.weekEndDate()); f = f.plusDays(1))
            if (ctx.profile().availableDays().contains((short) f.getDayOfWeek().getValue())) return f;
        return ctx.weekStartDate();
    }

    private long participante(Perfil p) {
        String email = "auditoria-54-" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Auditoria', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 6, 15), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("62"), null, p.nivel(), p.objetivo(), "Auditoria", p.lugar(),
                (short) 1, List.of(p.dia()), (short) p.minutos(), p.equipo(), List.of(), "Ninguna", p.impacto(),
                p.suelo(), p.rodillas(), p.colgado(), p.sobreCabeza()));
        return u;
    }
}
