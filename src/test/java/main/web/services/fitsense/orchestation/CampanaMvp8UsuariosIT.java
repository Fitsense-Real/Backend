package main.web.services.fitsense.orchestation;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.application.internal.commandservices.AdaptedWeekCommandService;
import main.web.services.fitsense.planning.application.internal.commandservices.PlanGenerationPipeline;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.generation.ai.AiRetryFeedback;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateTrainingPlanGenerator;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.planning.interfaces.rest.TrainingPlanController;
import main.web.services.fitsense.planning.interfaces.rest.resources.SkipWorkoutResource;
import main.web.services.fitsense.profiling.domain.exceptions.UserProfileLockedException;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.profiling.interfaces.rest.UserProfileController;
import main.web.services.fitsense.profiling.interfaces.rest.resources.UpdateUserProfileResource;
import main.web.services.fitsense.profiling.interfaces.rest.resources.UserProfileResource;
import main.web.services.fitsense.shared.domain.model.valueobjects.ContextoFaltaDeTiempo;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;

/**
 * CAMPANA MVP: OCHO USUARIOS SINTETICOS, OCHO SEMANAS, IA REAL (orden del 30/10/2026).
 * <p>
 * Llama a Replicate y cuesta dinero: solo con {@code -Dfitsense.campana.mvp=true}. Con
 * {@code -Dfitsense.campana.ia=false} es un ENSAYO SECO del propio simulador (sin IA, sin coste):
 * sus resultados no son los de la campana.
 * <p>
 * Base PostgreSQL efimera (Testcontainers), nunca Neon. Servicios reales de produccion para
 * generar, registrar (sesiones y omisiones por los mismos comandos y controladores que usa la app),
 * cerrar la semana, decidir, adaptar, validar y publicar. La progresion se enciende SOLO en este
 * contexto aislado. Los espias solo COPIAN lo que entra y sale de Replicate.
 * <p>
 * Calendario: S1 = 2/11/2026 (el lunes siguiente al 30/10/2026); el ciclo de cada semana se
 * ejecuta con su fecha ({@code WeeklyCycleService.runFor}), sin tocar el reloj global.
 * <p>
 * Presupuesto: como mucho {@code fitsense.campana.max-llamadas} (80) llamadas pagadas. Cada
 * operacion RESERVA antes su maximo (tres intentos); si no caben, el usuario se detiene y sus
 * semanas quedan PENDIENTE_PRESUPUESTO. Asi nunca se supera el limite.
 * <p>
 * El perfil es fijo durante el estudio (decision del 30/10/2026): los cambios de disponibilidad,
 * equipamiento o restricciones del guion se INTENTAN por el contrato de la app y se registra la
 * respuesta; no se fuerzan por SQL.
 * <p>
 * Salida en {@code target/campana-mvp/}.
 * <p>
 * MODO FOCO (orden de optimizacion del 31/10/2026, fase F): con
 * {@code -Dfitsense.campana.foco=U01:2,U02:3,...} solo esas operaciones (usuario:semana del ciclo)
 * llaman a Replicate. Las demas REPITEN, sin coste, la salida que dio el modelo en la campana
 * original ({@code fitsense.campana.repeticion}, por omision su {@code llamadas.jsonl}) en el mismo
 * intento, validada por el backend ACTUAL; si la campana original no llego a ese intento, el intento
 * se rechaza sin llamada. Asi el estado previo a cada caso se recrea con las mismas respuestas y la
 * misma adherencia del guion, sin registrar nada a mano. Cada usuario se detiene tras su ultimo caso.
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.campana.mvp", matches = "true")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=${fitsense.campana.ia:true}",
        // El proxy del entorno pone el token real; el backend solo exige que no este vacio.
        "fitsense.ai.api-token=proxy",
        "fitsense.ai.model=gpt-5-mini",
        "fitsense.ai.reasoning-effort=medium",
        "fitsense.ai.prefer-wait-seconds=20",
        "fitsense.adaptation.progression.enabled=true"
})
@DisplayName("Campana MVP: 8 usuarios x 8 semanas con IA real, adaptacion, progresion y reincorporacion")
class CampanaMvp8UsuariosIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoSpyBean ReplicateSessionFixGenerator correcciones;
    @MockitoSpyBean ReplicateTrainingPlanGenerator generador;

    @Autowired WeeklyCycleService ciclo;
    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired PlanDraftValidator validador;
    @Autowired UserProfileCommandService perfiles;
    @Autowired UserProfileController perfilRest;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired TrainingPlanController planes;
    @Autowired UserRepository usuarios;
    @Autowired JdbcTemplate jdbc;

    static final boolean IA = !"false".equals(System.getProperty("fitsense.campana.ia"));
    /** Solo estos usuarios (p. ej. {@code U06}); vacio = los ocho. */
    /** Modo foco: usuario -> semanas cuyo ciclo llama a la IA real. Vacio = campana completa. */
    static final Map<String, SortedSet<Integer>> FOCO = foco(System.getProperty("fitsense.campana.foco", ""));
    static final Set<String> SOLO = FOCO.isEmpty()
            ? Arrays.stream(System.getProperty("fitsense.campana.usuarios", "").split(","))
                    .map(String::strip).filter(x -> !x.isEmpty()).collect(Collectors.toSet())
            : FOCO.keySet();
    static final Path SALIDA = Path.of("target", "campana-mvp", (IA ? "real" : "ensayo-seco")
            + (FOCO.isEmpty() ? "" : "-foco")
            + (SOLO.isEmpty() ? "" : "-" + String.join("-", new TreeSet<>(SOLO))));
    static final Path REPETICION = Path.of(System.getProperty("fitsense.campana.repeticion",
            "docs/FitSense/campana_mvp/evidencia/llamadas.jsonl"));
    static final int MAX_LLAMADAS = Integer.getInteger("fitsense.campana.max-llamadas", 80);
    static final int HILOS = Integer.getInteger("fitsense.campana.hilos", 4);
    static final int SEMANAS = 8;
    static final double USD_ENTRADA = 0.25, USD_SALIDA = 2.00;
    static final LocalDate S1 = LocalDate.of(2026, 11, 2);

    static final List<String> GIMNASIO = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine",
            "bench", "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine");

    // =======================================================================
    // Perfiles y guiones (seccion 2 y 4 de la orden)
    // =======================================================================

    /** Que hace la persona en una sesion concreta de la semana. Las no nombradas, completas. */
    enum Tipo { HECHO, TIEMPO_HABITUAL, IMPREVISTO, PARCIAL, OMITIR, DOLOR_EJERCICIO, DIFICIL }

    record Accion(Tipo tipo, int indice, SkipReason motivo) {
        static Accion en(Tipo t, int i) { return new Accion(t, i, null); }
        static Accion omitir(int i, SkipReason m) { return new Accion(Tipo.OMITIR, i, m); }
        static Accion parcial(int i, SkipReason m) { return new Accion(Tipo.PARCIAL, i, m); }
    }

    /** Intento de cambiar el perfil (rechazado por la regla del estudio si ya hay plan). */
    record CambioPerfil(String descripcion, List<Short> dias, List<String> equipamiento, Boolean suelo) {}

    record Semana(String guion, List<Accion> acciones, boolean sinRegistro, Short rpe, CambioPerfil cambio) {
        static Semana todo(String g) { return new Semana(g, List.of(), false, null, null); }
        static Semana con(String g, Accion... a) { return new Semana(g, List.of(a), false, null, null); }
    }

    record Usuario(String id, GoalType objetivo, FitnessLevel nivel, List<Short> dias, TrainingLocation lugar,
                   List<String> equipamiento, String entorno, int minutos, BiologicalSex sexo, LocalDate nacimiento,
                   String altura, String peso, boolean impacto, boolean suelo, boolean rodillas, boolean colgado,
                   boolean sobreCabeza, List<Semana> guion) {}

    private static List<Short> d(int... dias) {
        var l = new ArrayList<Short>();
        for (int x : dias) l.add((short) x);
        return List.copyOf(l);
    }

    static List<Usuario> usuariosDeLaOrden() {
        var gymRestringido = List.of("body weight", "dumbbell", "cable", "leverage machine", "bench",
                "treadmill", "stationary bike");
        var h = Tipo.HECHO;
        return List.of(
                new Usuario("U01", GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, d(2, 3), TrainingLocation.HOME,
                        List.of("body weight", "dumbbell", "band"), "casa, mancuernas y bandas", 45,
                        BiologicalSex.MALE, LocalDate.of(1994, 3, 12), "176", "78", true, true, true, true, true,
                        List.of(Semana.todo("cumple todas"),
                                Semana.con("omite una por menos tiempo habitual (minutos reales)", Accion.en(Tipo.TIEMPO_HABITUAL, 1)),
                                Semana.todo("intenta cumplir la sesion adaptada"),
                                Semana.todo("cumple todas"),
                                Semana.todo("cumple todas (evaluar progresion)"),
                                Semana.con("dolor en un ejercicio", Accion.en(Tipo.DOLOR_EJERCICIO, 0)),
                                Semana.todo("cumple (seguridad y elegibilidad de progresion)"),
                                Semana.todo("cumple (posible reincorporacion)"))),
                new Usuario("U02", GoalType.LOSE_WEIGHT, FitnessLevel.BEGINNER, d(6, 7), TrainingLocation.HOME,
                        List.of("body weight", "band"), "casa, equipamiento limitado (peso corporal y banda)", 30,
                        BiologicalSex.FEMALE, LocalDate.of(1988, 7, 2), "162", "84", false, true, true, false, true,
                        List.of(Semana.todo("cumplimiento completo"),
                                Semana.con("sesion parcial por fatiga", Accion.parcial(0, SkipReason.FATIGUE)),
                                Semana.con("omision por dificultad excesiva", Accion.omitir(1, SkipReason.TOO_DIFFICULT)),
                                Semana.todo("recupera adherencia"),
                                Semana.con("falta de motivacion", Accion.omitir(0, SkipReason.LACK_OF_MOTIVATION)),
                                new Semana("cumple con RPE registrado (7)", List.of(), false, (short) 7, null),
                                Semana.con("imprevisto aislado de tiempo", Accion.en(Tipo.IMPREVISTO, 1)),
                                Semana.todo("cumple el plan vigente"))),
                new Usuario("U03", GoalType.INCREASE_STRENGTH, FitnessLevel.ADVANCED, d(1, 2, 3, 4), TrainingLocation.GYM,
                        GIMNASIO, "gimnasio completo", 60,
                        BiologicalSex.MALE, LocalDate.of(1990, 11, 23), "182", "88", true, true, true, true, true,
                        List.of(Semana.todo("cumplimiento sostenido"),
                                Semana.todo("cumplimiento sostenido (evaluar progresion)"),
                                Semana.con("dolor en un ejercicio", Accion.en(Tipo.DOLOR_EJERCICIO, 2)),
                                Semana.todo("cumple el plan compatible con seguridad"),
                                new Semana("intenta cambiar disponibilidad a mar, jue, sab y dom", List.of(), false, null,
                                        new CambioPerfil("dias 2,4,6,7", d(2, 4, 6, 7), null, null)),
                                new Semana("intenta cambiar equipamiento (sin barra ni smith)", List.of(), false, null,
                                        new CambioPerfil("sin barbell ni smith machine", null,
                                                GIMNASIO.stream().filter(e -> !e.equals("barbell") && !e.equals("smith machine")).toList(), null)),
                                Semana.con("ejecucion parcial de una sesion", Accion.parcial(1, SkipReason.FATIGUE)),
                                Semana.todo("cumple (verificar linaje)"))),
                new Usuario("U04", GoalType.GENERAL_FITNESS, FitnessLevel.BEGINNER, d(6), TrainingLocation.HOME,
                        List.of("body weight"), "casa, peso corporal", 30,
                        BiologicalSex.FEMALE, LocalDate.of(1979, 5, 30), "158", "67", false, true, true, false, true,
                        List.of(Semana.todo("completa su sesion"),
                                Semana.todo("completa su sesion"),
                                Semana.todo("completa (elegibilidad para progresar)"),
                                new Semana("semana sin registro", List.of(), true, null, null),
                                Semana.con("falta recurrente de tiempo con minutos", Accion.en(Tipo.TIEMPO_HABITUAL, 0)),
                                Semana.todo("ejecuta la sesion resultante"),
                                Semana.con("falta de motivacion", Accion.omitir(0, SkipReason.LACK_OF_MOTIVATION)),
                                Semana.todo("cumplimiento final"))),
                new Usuario("U05", GoalType.GENERAL_FITNESS, FitnessLevel.INTERMEDIATE, d(1, 2, 3, 4, 5, 6, 7),
                        TrainingLocation.GYM, GIMNASIO, "gimnasio con restricciones (sin impacto, sin rodillas, sin sobre la cabeza)", 30,
                        BiologicalSex.FEMALE, LocalDate.of(1985, 9, 9), "168", "63", false, true, false, true, false,
                        List.of(Semana.todo("cumple las programadas"),
                                Semana.todo("cumple las programadas"),
                                Semana.con("dolor en una sesion y falta de tiempo en otra",
                                        Accion.en(Tipo.DOLOR_EJERCICIO, 0), Accion.en(Tipo.TIEMPO_HABITUAL, 1)),
                                Semana.con("cumplimiento parcial de varias sesiones", Accion.parcial(0, SkipReason.FATIGUE),
                                        Accion.parcial(2, SkipReason.LACK_OF_TIME), Accion.parcial(4, SkipReason.FATIGUE)),
                                new Semana("intenta cambiar restricciones (sin suelo)", List.of(), false, null,
                                        new CambioPerfil("allowFloorExercises=false", null, null, false)),
                                Semana.con("fatiga recurrente", Accion.parcial(1, SkipReason.FATIGUE),
                                        Accion.parcial(3, SkipReason.FATIGUE)),
                                Semana.todo("recupera el cumplimiento"),
                                Semana.todo("recupera el cumplimiento (progresion o mantenimiento)"))),
                new Usuario("U06", GoalType.GAIN_MUSCLE, FitnessLevel.ADVANCED, d(1, 3, 4, 6, 7), TrainingLocation.GYM,
                        GIMNASIO, "gimnasio completo", 60,
                        BiologicalSex.MALE, LocalDate.of(1996, 1, 17), "180", "82", true, true, true, true, true,
                        List.of(Semana.todo("cumplimiento sostenido"),
                                Semana.todo("cumplimiento sostenido"),
                                Semana.todo("cumple (posible progresion gradual)"),
                                new Semana("cumple con RPE normal (6)", List.of(), false, (short) 6, null),
                                Semana.con("omision localizada por menos tiempo habitual", Accion.en(Tipo.TIEMPO_HABITUAL, 2)),
                                Semana.todo("cumple el plan adaptado"),
                                Semana.todo("cumple (progresion o reincorporacion)"),
                                Semana.todo("cumple (limites de volumen y mantenimiento)"))),
                new Usuario("U07", GoalType.LOSE_WEIGHT, FitnessLevel.INTERMEDIATE, d(1, 2, 3, 5, 6, 7), TrainingLocation.GYM,
                        gymRestringido, "gimnasio sin barra, smith, kettlebell ni ez", 45,
                        BiologicalSex.FEMALE, LocalDate.of(1991, 4, 4), "170", "79", true, true, true, true, true,
                        List.of(Semana.todo("cumplimiento completo"),
                                Semana.con("omite algunas por cambio de horario", Accion.omitir(1, SkipReason.SCHEDULE_CHANGE),
                                        Accion.omitir(3, SkipReason.SCHEDULE_CHANGE)),
                                Semana.con("fatiga y sesiones parciales", Accion.parcial(0, SkipReason.FATIGUE),
                                        Accion.parcial(2, SkipReason.FATIGUE)),
                                Semana.todo("cumple la planificacion resultante"),
                                Semana.con("omite una por falta de motivacion", Accion.omitir(4, SkipReason.LACK_OF_MOTIVATION)),
                                new Semana("intenta cambiar equipamiento (sin cable)", List.of(), false, null,
                                        new CambioPerfil("sin cable", null,
                                                gymRestringido.stream().filter(e -> !e.equals("cable")).toList(), null)),
                                Semana.todo("recupera el cumplimiento"),
                                Semana.todo("cumple (progresion, mantenimiento y cardio)"))),
                new Usuario("U08", GoalType.INCREASE_STRENGTH, FitnessLevel.BEGINNER, d(1, 3, 5), TrainingLocation.HOME,
                        List.of("body weight", "dumbbell", "band", "bench"), "casa, mancuernas ajustables, bandas y banco", 45,
                        BiologicalSex.MALE, LocalDate.of(2001, 8, 21), "174", "70", true, true, true, true, true,
                        List.of(Semana.todo("cumple todas"),
                                Semana.todo("cumple (elegibilidad para progresar)"),
                                Semana.con("sesion demasiado dificil", Accion.en(Tipo.DIFICIL, 1)),
                                Semana.todo("ejecuta el plan modificado"),
                                Semana.con("dolor en un ejercicio", Accion.en(Tipo.DOLOR_EJERCICIO, 0)),
                                Semana.todo("cumple las compatibles"),
                                Semana.con("menos tiempo habitual para una sesion", Accion.en(Tipo.TIEMPO_HABITUAL, 2)),
                                Semana.todo("cumple (continuidad y progresion real)"))));
    }

    // =======================================================================
    // Registro de lo que entra y sale de Replicate (los espias solo copian)
    // =======================================================================

    static final ThreadLocal<String> ETIQUETA = new ThreadLocal<>();

    record Llamada(String etiqueta, String tipo, long usuario, int intento, long ms, List<String> problemasPrevios,
                   String salida, String error) {}

    final Queue<Llamada> llamadas = new ConcurrentLinkedQueue<>();
    final Queue<String> tiempos = new ConcurrentLinkedQueue<>();
    final Queue<String> eventos = new ConcurrentLinkedQueue<>();
    final AtomicInteger reservadas = new AtomicInteger();
    final AtomicInteger fallosSeguidos = new AtomicInteger();
    final AtomicBoolean abortar = new AtomicBoolean();
    volatile String motivoParada;

    private void espiar() throws Exception {
        cargarOriginales();
        doAnswer(inv -> {
            if (!enFoco()) {
                // El snapshot es el que el backend actual habria enviado: solo la salida es la original.
                var builder = (main.web.services.fitsense.planning.infrastructure.generation.ai.SessionFixPromptBuilder)
                        org.springframework.test.util.ReflectionTestUtils.getField(correcciones, "promptBuilder");
                var prompt = builder.build(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2), inv.getArgument(3));
                return new ReplicateSessionFixGenerator.RawFix(prompt.snapshotJson(),
                        repetir("ADAPTACION", inv.getArgument(4)));
            }
            return registrar("ADAPTACION", ((PlanGenerationContext) inv.getArgument(0)).userId(),
                    inv.getArgument(4), inv.getArgument(3), () -> {
                        var r = (ReplicateSessionFixGenerator.RawFix) inv.callRealMethod();
                        return new Object[]{r, r.output()};
                    });
        }).when(correcciones).pedirRespuesta(any(), any(), any(), any(), anyInt());
        doAnswer(inv -> {
            if (!enFoco()) return repetir("GENERACION", inv.getArgument(2));
            return registrar("GENERACION", ((PlanGenerationContext) inv.getArgument(0)).userId(),
                    inv.getArgument(2), inv.getArgument(1), () -> {
                        var r = (String) inv.callRealMethod();
                        return new Object[]{r, r};
                    });
        }).when(generador).requestOutput(any(), any(), anyInt());

        var appender = new AppenderBase<ILoggingEvent>() {
            @Override
            protected void append(ILoggingEvent e) {
                String t = e.getFormattedMessage();
                if (t.startsWith("REPLICATE_TIMING")) {
                    tiempos.add(t);
                    if (t.contains("resultado=SUCCEEDED")) fallosSeguidos.set(0);
                    else if (fallosSeguidos.incrementAndGet() >= 5) parar("cinco fallos seguidos del proveedor: " + t);
                }
                if (e.getLevel().isGreaterOrEqual(ch.qos.logback.classic.Level.WARN) || t.startsWith("PROGRESION")
                        || t.startsWith("Adaptacion del usuario"))
                    eventos.add(ETIQUETA.get() + " | " + e.getLevel() + " | " + t);
            }
        };
        appender.start();
        for (var c : List.of(ReplicateTrainingPlanGenerator.class, PlanGenerationPipeline.class,
                AdaptedWeekCommandService.class))
            ((Logger) LoggerFactory.getLogger(c)).addAppender(appender);
    }

    @FunctionalInterface
    interface Llamar { Object[] llamar() throws Throwable; }

    private Object registrar(String tipo, Long usuario, Object intento, Object feedback, Llamar real) throws Throwable {
        if (abortar.get()) throw new IllegalStateException("campana detenida: " + motivoParada);
        if (llamadas.size() >= MAX_LLAMADAS) {
            parar("se alcanzo el limite de " + MAX_LLAMADAS + " llamadas");
            throw new IllegalStateException("limite de llamadas");
        }
        var previos = feedback instanceof AiRetryFeedback f ? f.rejectionProblems() : List.<String>of();
        long t0 = System.nanoTime();
        try {
            var r = real.llamar();
            llamadas.add(new Llamada(ETIQUETA.get(), tipo, usuario, (Integer) intento,
                    (System.nanoTime() - t0) / 1_000_000, previos, (String) r[1], null));
            return r[0];
        } catch (Throwable e) {
            llamadas.add(new Llamada(ETIQUETA.get(), tipo, usuario, (Integer) intento,
                    (System.nanoTime() - t0) / 1_000_000, previos, null, e.toString()));
            throw e;
        }
    }

    static Map<String, SortedSet<Integer>> foco(String valor) {
        var m = new TreeMap<String, SortedSet<Integer>>();
        for (var par : valor.split(",")) {
            if (par.isBlank()) continue;
            var x = par.strip().split(":");
            m.computeIfAbsent(x[0], k -> new TreeSet<>()).add(Integer.parseInt(x[1]));
        }
        return m;
    }

    /** La operacion en curso llama a la IA real: siempre fuera del modo foco. */
    static boolean enFoco() {
        if (FOCO.isEmpty()) return true;
        var e = ETIQUETA.get();
        if (e == null) return false;
        var x = e.split(" · s");
        return x.length == 2 && FOCO.getOrDefault(x[0], new TreeSet<>()).contains(Integer.parseInt(x[1]));
    }

    /** Salidas de la campana original por etiqueta|tipo|intento (solo en modo foco). */
    final Map<String, String> originales = new HashMap<>();
    final Queue<String> repeticiones = new ConcurrentLinkedQueue<>();

    private void cargarOriginales() throws Exception {
        if (FOCO.isEmpty()) return;
        var json = new ObjectMapper();
        for (var linea : Files.readAllLines(REPETICION)) {
            if (linea.isBlank()) continue;
            var n = json.readTree(linea);
            if (n.path("salida").isTextual())
                originales.put(n.get("etiqueta").asText() + "|" + n.get("tipo").asText() + "|" + n.get("intento").asInt(),
                        n.get("salida").asText());
        }
    }

    /** La salida original de este intento, o un rechazo sin llamada si la campana no llego a el. */
    private String repetir(String tipo, int intento) {
        var clave = ETIQUETA.get() + "|" + tipo + "|" + intento;
        var salida = originales.get(clave);
        repeticiones.add(clave + (salida == null ? " -> sin salida original (rechazo sin llamada)" : " -> repetida"));
        if (salida == null)
            throw new InvalidPlanDraftException(List.of("REPETICION: la campana original no tuvo el intento "
                    + intento + " de esta operacion; no se llama a la IA fuera del foco."));
        return salida;
    }

    private void parar(String motivo) {
        if (abortar.compareAndSet(false, true)) motivoParada = motivo;
    }

    /** Reserva el maximo de una operacion (3 intentos). Falso si no cabe en el presupuesto. */
    private boolean reservar() {
        if (!IA || !enFoco()) return true;
        while (true) {
            int r = reservadas.get();
            if (r + 3 > MAX_LLAMADAS) return false;
            if (reservadas.compareAndSet(r, r + 3)) return true;
        }
    }

    /** Devuelve lo reservado que no se gasto. */
    private void liberar(int usadas) {
        if (IA && enFoco()) reservadas.addAndGet(usadas - 3);
    }

    private int contar(long u) {
        return (int) llamadas.stream().filter(l -> l.usuario() == u).count();
    }

    // =======================================================================
    // Filas de salida
    // =======================================================================

    /** Una fila por usuario y semana (la matriz principal, 64 filas). */
    record FilaSemana(String usuario, int semana, LocalDate inicio, String guion, Long planId, String origenPlan,
                      String validacionIndependiente, int programadas, String focos, int completas, int parciales,
                      int omitidas, int sinRegistro, Double adherenciaOrden, Double adherenciaBackend,
                      Double adherenciaObservada, String riesgo, String motivos, String cambioPerfil,
                      String decision, String estado, String intervenciones, int llamadasIa, int aiCallsV32,
                      String siguientePlan, String pasos, String bloqueos, String retirados,
                      List<String> violaciones, String nota) {}

    record FilaSesion(String usuario, int semana, long sesion, Long origen, LocalDate fecha, String foco,
                      int minutos, int ejercicios, String accion, String estado, Double cumplimiento,
                      String motivo, String contexto, Integer minutosDeclarados, Short rpe, String dosis) {}

    record FilaPaso(String usuario, int semana, String tipo, Long sesion, Long ejercicio, String antes,
                    String despues, Integer volumenAntes, Integer volumenDespues, Integer volumenBase, Integer techo,
                    Double incrementoPct, String estadoReserva, String resultado) {}

    record Resultado(Usuario usuario, long userId, String inicial, List<FilaSemana> semanas,
                     List<FilaSesion> sesiones, List<FilaPaso> pasos, String corte) {}

    // =======================================================================
    // Campana
    // =======================================================================

    @Test
    void campana() throws Exception {
        Files.createDirectories(SALIDA);
        espiar();
        long inicio = System.currentTimeMillis();
        var lista = usuariosDeLaOrden().stream().filter(u -> SOLO.isEmpty() || SOLO.contains(u.id())).toList();
        var pool = Executors.newFixedThreadPool(HILOS);
        var futuros = new ArrayList<Future<Resultado>>();
        for (int i = 0; i < lista.size(); i++) {
            int n = i + 1;
            var u = lista.get(i);
            futuros.add(pool.submit(() -> recorrer(n, u)));
        }
        var resultados = new ArrayList<Resultado>();
        for (var f : futuros) resultados.add(f.get(8, TimeUnit.HOURS));
        pool.shutdown();
        escribir(resultados, metricasDeReplicate(), System.currentTimeMillis() - inicio);
    }

    private Resultado recorrer(int n, Usuario def) {
        var semanas = new ArrayList<FilaSemana>();
        var filasSesion = new ArrayList<FilaSesion>();
        var filasPaso = new ArrayList<FilaPaso>();
        long u = participante(n, def);
        ETIQUETA.set(def.id() + " · inicial");

        if (!reservar())
            return pendiente(def, u, "PENDIENTE_PRESUPUESTO (generacion inicial)", semanas, filasSesion, filasPaso, 1);
        int antes = contar(u);
        Optional<Long> plan;
        String inicial;
        try {
            plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null);
            inicial = plan.map(p -> origen(p) + ", " + (contar(u) - antes) + " llamadas").orElse("SIN_PLAN");
        } catch (RuntimeException e) {
            plan = Optional.empty();
            inicial = "SIN_PLAN: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        } finally {
            liberar(contar(u) - antes);
        }
        if (plan.isEmpty())
            return pendiente(def, u, "sin plan inicial (" + inicial + ")", semanas, filasSesion, filasPaso, 1);

        for (int k = 0; k < SEMANAS; k++) {
            int numero = k + 1;
            var semana = S1.plusWeeks(k);
            var guion = def.guion().get(k);
            ETIQUETA.set(def.id() + " · s" + numero);
            long planId = plan.get();
            String validacion = validarSemana(u, planId, semana);

            String cambio = guion.cambio() == null ? null : intentarCambio(u, guion.cambio());
            var medidas = sesionesDe(planId);
            var acciones = ejecutar(u, medidas, guion);

            if (abortar.get() || !reservar()) {
                semanas.add(filaSinCiclo(def, numero, semana, guion, planId, validacion, medidas, acciones, cambio,
                        abortar.get() ? "campana detenida: " + motivoParada : "PENDIENTE_PRESUPUESTO"));
                volcarSesiones(def, numero, medidas, acciones, filasSesion);
                return pendiente(def, u, "detenido en la semana " + numero, semanas, filasSesion, filasPaso, numero + 1);
            }
            int llamadasAntes = contar(u);
            int filasV32 = filasV32(u);
            Optional<Long> siguiente;
            String nota = null;
            try {
                siguiente = ciclo.runFor(u, semana, semana.plusWeeks(1));
            } catch (RuntimeException e) {
                siguiente = Optional.empty();
                nota = "EXCEPCION: " + e;
            } finally {
                liberar(contar(u) - llamadasAntes);
            }
            int llamadasCiclo = contar(u) - llamadasAntes;
            volcarSesiones(def, numero, medidas, acciones, filasSesion);
            var fila = evaluar(def, u, numero, semana, guion, planId, validacion, medidas, acciones, cambio,
                    siguiente, llamadasCiclo, filasV32(u) > filasV32, nota, filasPaso);
            semanas.add(fila);
            if (siguiente.isEmpty())
                return new Resultado(def, u, inicial, semanas, filasSesion, filasPaso,
                        "sin semana publicada tras la " + numero);
            plan = siguiente;
            if (!FOCO.isEmpty() && numero == FOCO.get(def.id()).last()) {
                for (int r = numero + 1; r <= SEMANAS; r++)
                    semanas.add(new FilaSemana(def.id(), r, S1.plusWeeks(r - 1), def.guion().get(r - 1).guion(), null,
                            "-", "-", 0, "-", 0, 0, 0, 0, null, null, null, "-", "-", null, "-", "FUERA_DEL_FOCO", "-",
                            0, 0, "-", null, null, null, List.of(), "modo foco: el usuario se detiene tras su ultimo caso"));
                return new Resultado(def, u, inicial, semanas, filasSesion, filasPaso, "fin del foco (semana " + numero + ")");
            }
        }
        return new Resultado(def, u, inicial, semanas, filasSesion, filasPaso, null);
    }

    private Resultado pendiente(Usuario def, long u, String corte, List<FilaSemana> semanas,
                                List<FilaSesion> s, List<FilaPaso> p, int desde) {
        for (int k = desde; k <= SEMANAS; k++)
            semanas.add(new FilaSemana(def.id(), k, S1.plusWeeks(k - 1), def.guion().get(k - 1).guion(), null, "-",
                    "-", 0, "-", 0, 0, 0, 0, null, null, null, "-", "-", null, "-", "NO_EJECUTADA", "-", 0, 0, "-",
                    null, null, null, List.of(), corte));
        return new Resultado(def, u, desde == 1 ? "SIN_PLAN" : "detenido", semanas, s, p, corte);
    }

    // =======================================================================
    // Ejecucion por los servicios reales
    // =======================================================================

    record Hecho(String accion, String motivo, String contexto, Integer minutosDeclarados, Short rpe) {}

    private Map<Long, Hecho> ejecutar(long u, List<Ses> sesionesSemana, Semana guion) {
        var hechos = new LinkedHashMap<Long, Hecho>();
        if (guion.sinRegistro()) {
            for (var s : sesionesSemana) hechos.put(s.id(), new Hecho("SIN_REGISTRO", null, null, null, null));
            return hechos;
        }
        var porIndice = new HashMap<Integer, Accion>();
        for (var a : guion.acciones()) porIndice.put(Math.floorMod(a.indice(), sesionesSemana.size()), a);
        for (int i = 0; i < sesionesSemana.size(); i++) {
            var s = sesionesSemana.get(i);
            var a = porIndice.get(i);
            short rpe = guion.rpe() != null ? guion.rpe() : 6;
            if (a == null || a.tipo() == Tipo.HECHO) {
                hecha(u, s, rpe);
                hechos.put(s.id(), new Hecho("HECHO", null, null, null, rpe));
                continue;
            }
            switch (a.tipo()) {
                case TIEMPO_HABITUAL -> {
                    // Minutos que la persona suele tener para ESTE entrenamiento: menos que la sesion.
                    int m = Math.max(15, (int) Math.round(s.minutos() * 0.7));
                    planes.skip(principal(u), s.id(), new SkipWorkoutResource(SkipReason.LACK_OF_TIME,
                            ContextoFaltaDeTiempo.MENOS_TIEMPO_HABITUAL, m));
                    hechos.put(s.id(), new Hecho("OMITIDA", "LACK_OF_TIME", "MENOS_TIEMPO_HABITUAL", m, null));
                }
                case IMPREVISTO -> {
                    planes.skip(principal(u), s.id(), new SkipWorkoutResource(SkipReason.LACK_OF_TIME,
                            ContextoFaltaDeTiempo.IMPREVISTO, null));
                    hechos.put(s.id(), new Hecho("OMITIDA", "LACK_OF_TIME", "IMPREVISTO", null, null));
                }
                case OMITIR -> {
                    planes.skip(principal(u), s.id(), new SkipWorkoutResource(a.motivo()));
                    hechos.put(s.id(), new Hecho("OMITIDA", a.motivo().name(), null, null, null));
                }
                case PARCIAL -> {
                    parcial(u, s, a.motivo(), (short) 8);
                    hechos.put(s.id(), new Hecho("PARCIAL", a.motivo().name(), null, null, (short) 8));
                }
                case DIFICIL -> {
                    parcial(u, s, SkipReason.TOO_DIFFICULT, (short) 9);
                    hechos.put(s.id(), new Hecho("PARCIAL", "TOO_DIFFICULT", null, null, (short) 9));
                }
                case DOLOR_EJERCICIO -> {
                    dolorEnUnEjercicio(u, s, rpe);
                    hechos.put(s.id(), new Hecho("HECHO_CON_DOLOR", "PAIN_OR_DISCOMFORT", null, null, rpe));
                }
                default -> throw new IllegalStateException(a.tipo().name());
            }
        }
        return hechos;
    }

    private void hecha(long u, Ses s, short rpe) {
        long id = sesiones.handle(new StartWorkoutSessionCommand(u, s.id())).orElseThrow().getId();
        for (var e : s.ejercicios()) completo(u, id, e);
        sesiones.handle(new FinishWorkoutSessionCommand(u, id, rpe, (short) 4, (short) s.minutos()));
    }

    /** La primera mitad de los ejercicios completos; el resto dejado por el motivo. */
    private void parcial(long u, Ses s, SkipReason motivo, short rpe) {
        long id = sesiones.handle(new StartWorkoutSessionCommand(u, s.id())).orElseThrow().getId();
        int hechos = Math.max(1, s.ejercicios().size() / 2);
        for (int i = 0; i < s.ejercicios().size(); i++) {
            var e = s.ejercicios().get(i);
            if (i < hechos) completo(u, id, e);
            else sesiones.handle(new RecordSessionExerciseCommand(u, id, e.pe(), (short) 0, null, null, null, motivo));
        }
        sesiones.handle(new FinishWorkoutSessionCommand(u, id, rpe, (short) 3, (short) Math.max(5, s.minutos() / 2)));
    }

    /** Todo completo salvo el primer ejercicio de fuerza (o el primero), dejado por dolor. */
    private void dolorEnUnEjercicio(long u, Ses s, short rpe) {
        long id = sesiones.handle(new StartWorkoutSessionCommand(u, s.id())).orElseThrow().getId();
        var conDolor = s.ejercicios().stream().filter(e -> "SETS_REPS".equals(e.tipo())).findFirst()
                .orElse(s.ejercicios().get(0));
        for (var e : s.ejercicios()) {
            if (e == conDolor)
                sesiones.handle(new RecordSessionExerciseCommand(u, id, e.pe(), (short) 0, null, null, null,
                        SkipReason.PAIN_OR_DISCOMFORT));
            else completo(u, id, e);
        }
        sesiones.handle(new FinishWorkoutSessionCommand(u, id, rpe, (short) 3, (short) s.minutos()));
    }

    private void completo(long u, long sesion, Ej e) {
        boolean duracion = "DURATION".equals(e.tipo());
        int series = e.series() == null ? 1 : e.series();
        int total = series * (duracion ? (e.segundos() == null ? 0 : e.segundos()) : (e.reps() == null ? 0 : e.reps()));
        sesiones.handle(new RecordSessionExerciseCommand(u, sesion, e.pe(), (short) series,
                duracion ? null : total, duracion ? total : null, e.carga(), null));
    }

    /** Intenta el cambio por el contrato de la app y devuelve lo que respondio el backend. */
    private String intentarCambio(long u, CambioPerfil c) {
        UserProfileResource p = perfilRest.get(principal(u)).getBody();
        var dias = c.dias() != null ? c.dias() : p.availableDays();
        var recurso = new UpdateUserProfileResource(null, p.heightCm(), p.currentWeightKg(), p.targetWeightKg(),
                FitnessLevel.valueOf(p.fitnessLevel()), GoalType.valueOf(p.goalType()), p.goalText(),
                TrainingLocation.valueOf(p.trainingLocation()), (short) Math.min(p.daysPerWeek(), dias.size()), dias,
                p.sessionMinutes(), c.equipamiento() != null ? c.equipamiento() : p.equipmentCodes(),
                p.blockedExerciseIds(), p.healthNotes(), null, c.suelo(), null, null, null);
        try {
            perfilRest.update(principal(u), recurso);
            return "ACEPTADO (" + c.descripcion() + ")";
        } catch (UserProfileLockedException e) {
            return "RECHAZADO_422 (" + c.descripcion() + "): perfil fijo durante el estudio";
        } catch (RuntimeException e) {
            return "ERROR (" + c.descripcion() + "): " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    // =======================================================================
    // Evaluacion de cada semana, independiente de los validadores
    // =======================================================================

    record Ej(long pe, long id, String tipo, Short series, Short reps, Integer segundos, Short descanso, BigDecimal carga) {
        String dosis() {
            return id + ":" + (series == null ? "-" : series) + "x" + (reps != null ? reps + "r" : segundos + "s")
                    + "/" + descanso + "s" + (carga == null ? "" : "@" + carga.stripTrailingZeros().toPlainString() + "kg");
        }
    }

    record Ses(long id, Long origen, LocalDate fecha, String foco, int minutos, List<Ej> ejercicios) {}

    private FilaSemana evaluar(Usuario def, long u, int numero, LocalDate semana, Semana guion, long planId,
                               String validacion, List<Ses> medidas, Map<Long, Hecho> hechos, String cambio,
                               Optional<Long> publicado, int contadas, boolean hayV32, String nota,
                               List<FilaPaso> filasPaso) {
        var violaciones = new ArrayList<String>();
        var cuenta = contarSesiones(medidas);
        var metrica = metrica(u, semana);
        Double backend = metrica == null ? null : num(metrica.get("weighted_adherence_pct"));
        Double observada = metrica == null ? null : num(metrica.get("observed_adherence_pct"));
        String riesgo = metrica == null ? "-" : metrica.get("risk_level") + " (" + metrica.get("risk_score") + ")";
        double orden = medidas.isEmpty() ? 0 : (cuenta[0] + 0.5 * cuenta[1]) * 100.0 / medidas.size();

        if (!hayV32) {
            violaciones.add("el ciclo no registro intento V32" + (publicado.isEmpty() ? " ni publico semana" : ""));
            return new FilaSemana(def.id(), numero, semana, guion.guion(), planId, origen(planId), validacion,
                    medidas.size(), focos(medidas), cuenta[0], cuenta[1], cuenta[2], cuenta[3], orden, backend,
                    observada, riesgo, motivos(hechos), cambio, "-", "SIN_EVIDENCIA", "-", contadas, 0,
                    publicado.map(String::valueOf).orElse("NINGUNO"), null, null, null, violaciones, nota);
        }
        var ev = jdbc.queryForMap("SELECT decision, status, ai_calls, targeted_sessions::text AS senaladas,"
                + " targeted_interventions::text AS intervenciones, progressed_sessions::text AS progresadas,"
                + " conservation_report::text AS informe, rejection_reason FROM plan_adaptation_attempts"
                + " WHERE user_id = ? ORDER BY attempt_id DESC LIMIT 1", u);
        String informe = String.valueOf(ev.get("informe"));
        int aiCalls = ((Number) ev.get("ai_calls")).intValue();
        String decision = (String) ev.get("decision");
        String intervenciones = String.valueOf(ev.get("intervenciones"));
        if (IA && aiCalls != contadas) violaciones.add("ai_calls V32 = " + aiCalls + ", llamadas contadas = " + contadas);
        if (contadas > 3) violaciones.add("mas de tres llamadas en el ciclo: " + contadas);
        if (("NO_CHANGE".equals(decision) || "INSUFFICIENT_DATA".equals(decision)) && contadas > 0)
            violaciones.add("ciclo determinista con " + contadas + " llamadas a la IA");
        if (intervenciones.contains("CONSERVAR") && contadas > 0 && !intervenciones.matches(".*(ACORTAR|SEGURIDAD|REVISAR|VARIANTE|INVESTIGAR|REPROGRAMAR).*"))
            violaciones.add("CONSERVAR con " + contadas + " llamadas a la IA");

        var senaladas = ids((String) ev.get("senaladas"));
        var progresadas = ids((String) ev.get("progresadas"));
        var sustituidas = new HashSet<Long>();
        var m = Pattern.compile("\"sesion_origen\"\\s*:\\s*(\\d+)").matcher(informe);
        while (m.find()) sustituidas.add(Long.parseLong(m.group(1)));

        var retirados = new ArrayList<String>();
        if (publicado.isPresent()) {
            var nuevas = sesionesDe(publicado.get()).stream().filter(s -> s.origen() != null)
                    .collect(Collectors.toMap(Ses::origen, s -> s, (a, b) -> a));
            for (var s : medidas) {
                var nueva = nuevas.get(s.id());
                if (nueva == null) {
                    violaciones.add("la sesion " + s.id() + " no tiene derivada con linaje");
                    continue;
                }
                var idsAntes = s.ejercicios().stream().map(Ej::id).collect(Collectors.toSet());
                var idsDespues = nueva.ejercicios().stream().map(Ej::id).collect(Collectors.toSet());
                idsAntes.stream().filter(x -> !idsDespues.contains(x))
                        .forEach(x -> retirados.add(x + "@" + s.id()));
                if (senaladas.contains(s.id())) {
                    subidas(s, nueva, violaciones);
                    var h = hechos.get(s.id());
                    if (h != null && "MENOS_TIEMPO_HABITUAL".equals(h.contexto()) && h.minutosDeclarados() != null
                            && nueva.minutos() > h.minutosDeclarados() && !s.fecha().plusWeeks(1).equals(nueva.fecha())
                            && !identicas(s, nueva))
                        violaciones.add("sesion acortada " + nueva.id() + " dura " + nueva.minutos()
                                + " min con " + h.minutosDeclarados() + " declarados");
                    if (intervenciones.contains("SEGURIDAD_ANTE_DOLOR")) {
                        var dolio = dolorDe(s.id());
                        if (dolio != null && idsDespues.contains(dolio) && !identicas(s, nueva))
                            violaciones.add("el ejercicio " + dolio + " que dolio sigue en la sesion " + nueva.id());
                        if (volumen(nueva.ejercicios()) > volumen(s.ejercicios()))
                            violaciones.add("ante dolor el volumen sube de " + volumen(s.ejercicios()) + " a "
                                    + volumen(nueva.ejercicios()) + " en la sesion " + s.id());
                    }
                } else if (!progresadas.contains(s.id()) && !sustituidas.contains(s.id()) && !identicas(s, nueva)) {
                    violaciones.add("la sesion NO senalada " + s.id() + " cambio: " + s.ejercicios().stream()
                            .map(Ej::dosis).toList() + " -> " + nueva.ejercicios().stream().map(Ej::dosis).toList());
                }
            }
        }
        var pasos = pasos(def.id(), numero, informe, violaciones, filasPaso);
        return new FilaSemana(def.id(), numero, semana, guion.guion(), planId, origen(planId), validacion,
                medidas.size(), focos(medidas), cuenta[0], cuenta[1], cuenta[2], cuenta[3], orden, backend, observada,
                riesgo, motivos(hechos), cambio, decision, (String) ev.get("status"), intervenciones, contadas,
                aiCalls, publicado.map(String::valueOf).orElse("NINGUNO"), pasos[0], pasos[1],
                retirados.isEmpty() ? null : String.join(" ", retirados), violaciones,
                nota != null ? nota : (String) ev.get("rejection_reason"));
    }

    private FilaSemana filaSinCiclo(Usuario def, int numero, LocalDate semana, Semana guion, long planId,
                                    String validacion, List<Ses> medidas, Map<Long, Hecho> hechos, String cambio,
                                    String nota) {
        var c = contarSesiones(medidas);
        double orden = medidas.isEmpty() ? 0 : (c[0] + 0.5 * c[1]) * 100.0 / medidas.size();
        return new FilaSemana(def.id(), numero, semana, guion.guion(), planId, origen(planId), validacion,
                medidas.size(), focos(medidas), c[0], c[1], c[2], c[3], orden, null, null, "-", motivos(hechos),
                cambio, "-", "SIN_CICLO", "-", 0, 0, "-", null, null, null, List.of(), nota);
    }

    /** Completas (>= 80 %), parciales (30-80 %), omitidas (con causa o por debajo del 30 %) y sin registro. */
    private int[] contarSesiones(List<Ses> medidas) {
        int[] c = new int[4];
        for (var s : medidas) {
            var a = cumplimiento(s.id());
            String estado = jdbc.queryForObject("SELECT status FROM planned_workouts WHERE planned_workout_id = ?",
                    String.class, s.id());
            if (a == null) c["SKIPPED".equals(estado) ? 2 : 3]++;
            else if (a >= 80) c[0]++;
            else if (a >= 30) c[1]++;
            else c[2]++;
        }
        return c;
    }

    private Double cumplimiento(long sesion) {
        var l = jdbc.queryForList("SELECT completion_percentage FROM workout_sessions WHERE planned_workout_id = ?"
                + " AND counts_toward_adherence", sesion);
        return l.isEmpty() ? null : num(l.get(0).get("completion_percentage"));
    }

    private Long dolorDe(long sesion) {
        var l = jdbc.queryForList("SELECT pwe.exercise_id FROM workout_session_exercises wse"
                + " JOIN workout_sessions ws ON ws.session_id = wse.session_id"
                + " JOIN planned_workout_exercises pwe ON pwe.planned_exercise_id = wse.planned_exercise_id"
                + " WHERE ws.planned_workout_id = ? AND wse.skip_reason = 'PAIN_OR_DISCOMFORT'", Long.class, sesion);
        return l.isEmpty() ? null : l.get(0);
    }

    private Map<String, Object> metrica(long u, LocalDate semana) {
        var l = jdbc.queryForList("SELECT * FROM weekly_user_metrics WHERE user_id = ? AND week_start_date = ?",
                u, semana);
        return l.isEmpty() ? null : l.get(0);
    }

    private static String motivos(Map<Long, Hecho> hechos) {
        var t = hechos.values().stream().filter(h -> h.motivo() != null)
                .map(h -> h.motivo() + (h.contexto() == null ? "" : "/" + h.contexto())
                        + (h.minutosDeclarados() == null ? "" : "(" + h.minutosDeclarados() + " min)"))
                .toList();
        return t.isEmpty() ? "-" : String.join(" + ", t);
    }

    private static String focos(List<Ses> l) {
        return l.stream().map(s -> s.fecha().getDayOfWeek().toString().substring(0, 3) + ":" + s.foco())
                .collect(Collectors.joining(" "));
    }

    private void volcarSesiones(Usuario def, int numero, List<Ses> medidas, Map<Long, Hecho> hechos,
                                List<FilaSesion> filas) {
        for (var s : medidas) {
            var h = hechos.getOrDefault(s.id(), new Hecho("-", null, null, null, null));
            String estado = jdbc.queryForObject("SELECT status FROM planned_workouts WHERE planned_workout_id = ?",
                    String.class, s.id());
            filas.add(new FilaSesion(def.id(), numero, s.id(), s.origen(), s.fecha(), s.foco(), s.minutos(),
                    s.ejercicios().size(), h.accion(), estado, cumplimiento(s.id()), h.motivo(), h.contexto(),
                    h.minutosDeclarados(), h.rpe(), s.ejercicios().stream().map(Ej::dosis).collect(Collectors.joining(" "))));
        }
    }

    /** La IA no puede subir ninguna dosis de una sesion senalada (series, repeticiones, segundos, carga). */
    private static void subidas(Ses medida, Ses nueva, List<String> violaciones) {
        var previas = medida.ejercicios().stream().collect(Collectors.toMap(Ej::id, e -> e, (x, y) -> x));
        for (var e : nueva.ejercicios()) {
            var p = previas.get(e.id());
            if (p == null) continue;
            if (mayor(e.series(), p.series()) || mayor(e.reps(), p.reps()) || mayor(e.segundos(), p.segundos())
                    || (e.carga() != null && (p.carga() == null || e.carga().compareTo(p.carga()) > 0)))
                violaciones.add("dosis subida en la sesion senalada " + medida.id() + ": " + p.dosis() + " -> " + e.dosis());
        }
    }

    private static int volumen(List<Ej> l) {
        return l.stream().mapToInt(e -> {
            int series = e.series() == null ? 1 : e.series();
            return "DURATION".equals(e.tipo()) ? Math.round((float) (series * (e.segundos() == null ? 0 : e.segundos())) / 30)
                    : series * (e.reps() == null ? 0 : e.reps());
        }).sum();
    }

    private static boolean mayor(Number a, Number b) {
        return a != null && b != null && a.intValue() > b.intValue();
    }

    private static boolean identicas(Ses a, Ses b) {
        if (a.ejercicios().size() != b.ejercicios().size()) return false;
        for (int i = 0; i < a.ejercicios().size(); i++) {
            var x = a.ejercicios().get(i);
            var y = b.ejercicios().get(i);
            if (x.id() != y.id() || !Objects.equals(x.series(), y.series()) || !Objects.equals(x.reps(), y.reps())
                    || !Objects.equals(x.segundos(), y.segundos()) || !Objects.equals(x.descanso(), y.descanso()))
                return false;
        }
        return true;
    }

    /** Pasos declarados en V32 (y los bloqueados), con las comprobaciones de 5 %, 10 % y techo. */
    private static String[] pasos(String usuario, int semana, String informe, List<String> violaciones,
                                  List<FilaPaso> filas) {
        if (informe == null || informe.equals("null")) return new String[]{null, null};
        try {
            var raiz = new ObjectMapper().readTree(informe);
            var tipos = new ArrayList<String>();
            for (JsonNode p : raiz.path("pasos")) {
                String tipo = p.path("tipo").asText();
                tipos.add(tipo);
                int va = p.path("volumen_antes").asInt(), vd = p.path("volumen_despues").asInt();
                int base = p.path("volumen_base").asInt(), techo = p.path("techo").asInt();
                if (techo > Math.floor(base * 1.10)) violaciones.add("techo " + techo + " > 1,10 x " + base);
                if (!tipo.equals("VUELTA_ATRAS") && vd > techo)
                    violaciones.add(tipo + " deja el volumen " + vd + " por encima del techo " + techo);
                if (tipo.equals("PROGRESION") && (vd - va) * 100.0 > 5.0 * va)
                    violaciones.add("PROGRESION de " + va + " a " + vd + " supera el 5 %");
                if (tipo.equals("RECUPERACION") && (vd - va) * 100.0 > 10.0 * va)
                    violaciones.add("RECUPERACION de " + va + " a " + vd + " supera el 10 %");
                filas.add(new FilaPaso(usuario, semana, tipo, p.path("origen").isMissingNode() ? null : p.path("origen").asLong(),
                        p.path("exercise_id").isMissingNode() ? null : p.path("exercise_id").asLong(),
                        p.path("antes").toString(), p.path("despues").toString(), va, vd, base, techo,
                        va == 0 ? null : (vd - va) * 100.0 / va, p.path("estado_reserva").asText(null), "PUBLICADO"));
            }
            var bloqueos = new ArrayList<String>();
            for (JsonNode b : raiz.path("pasos_bloqueados")) {
                bloqueos.add(b.asText());
                filas.add(new FilaPaso(usuario, semana, "BLOQUEADO", null, null, null, null, null, null, null, null,
                        null, null, b.asText()));
            }
            return new String[]{tipos.isEmpty() ? null : String.join(",", tipos),
                    bloqueos.isEmpty() ? null : String.join(" | ", bloqueos)};
        } catch (Exception e) {
            violaciones.add("informe de conservacion ilegible: " + e.getMessage());
            return new String[]{null, null};
        }
    }

    private static Set<Long> ids(String arr) {
        var s = new LinkedHashSet<Long>();
        if (arr == null) return s;
        for (var p : arr.replaceAll("[{}\\s]", "").split(",")) if (!p.isEmpty()) s.add(Long.parseLong(p));
        return s;
    }

    private List<Ses> sesionesDe(long plan) {
        return jdbc.query("SELECT planned_workout_id, source_planned_workout_id, scheduled_date, focus_code,"
                        + " expected_duration_minutes FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date",
                (rs, i) -> new Ses(rs.getLong(1), rs.getObject(2, Long.class), rs.getDate(3).toLocalDate(),
                        rs.getString(4), rs.getInt(5), ejercicios(rs.getLong(1))), plan);
    }

    private List<Ej> ejercicios(long sesion) {
        return jdbc.query("SELECT planned_exercise_id, exercise_id, prescription_type, planned_sets, planned_reps,"
                        + " planned_duration_seconds, rest_seconds, target_load_kg FROM planned_workout_exercises"
                        + " WHERE planned_workout_id = ? ORDER BY exercise_order",
                (rs, i) -> new Ej(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getObject(4, Short.class),
                        rs.getObject(5, Short.class), rs.getObject(6, Integer.class), rs.getObject(7, Short.class),
                        rs.getBigDecimal(8)), sesion);
    }

    private String origen(long plan) {
        return jdbc.queryForObject("SELECT generation_source FROM weekly_training_plans WHERE plan_id = ?",
                String.class, plan);
    }

    /**
     * Validacion independiente de la semana con TODOS los validadores de la generacion (V1-V25), sin
     * las exenciones de una adaptacion autorizada (p. ej. la duracion de una sesion acortada). Es
     * informativa: lo que publico el backend ya paso V1-V25 y V8C con sus exenciones.
     */
    private String validarSemana(long u, long plan, LocalDate semana) {
        try {
            var borrador = new ArrayList<PlanDraft.DraftWorkout>();
            for (var s : sesionesDe(plan)) {
                var l = s.ejercicios().stream().map(e -> new PlanDraft.DraftExercise(e.id(),
                        PrescriptionType.valueOf(e.tipo()), e.series(), e.reps(), e.segundos(), e.carga(), e.descanso(),
                        null)).toList();
                borrador.add(new PlanDraft.DraftWorkout(s.fecha(), WorkoutFocus.valueOf(s.foco()), "semana", s.minutos(), l));
            }
            var ctx = planComandos.prepare(new GenerateWeeklyPlanCommand(u, semana, null, true, null, null)).context();
            validador.validate(new PlanDraft(GenerationSource.RULE_ENGINE, null, "semana", null, "r", borrador), ctx, 30);
            return "VALIDA";
        } catch (InvalidPlanDraftException e) {
            return "INVALIDA " + e.problems().stream().map(p -> p.length() > 140 ? p.substring(0, 140) : p).toList();
        } catch (RuntimeException e) {
            return "NO_EVALUABLE " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    private int filasV32(long u) {
        return jdbc.queryForObject("SELECT count(*) FROM plan_adaptation_attempts WHERE user_id = ?", Integer.class, u);
    }

    private static Double num(Object o) {
        return o == null ? null : ((Number) o).doubleValue();
    }

    // =======================================================================
    // Material
    // =======================================================================

    private long participante(int n, Usuario d) {
        String email = "mvp-" + d.id().toLowerCase(Locale.ROOT) + "-" + (IA ? "real" : "seco") + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', ?, now(), now())", email, d.id());
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, d.nacimiento(), d.sexo(), new BigDecimal(d.altura()),
                new BigDecimal(d.peso()), null, d.nivel(), d.objetivo(), "campana MVP " + d.id(), d.lugar(),
                (short) d.dias().size(), d.dias(), (short) d.minutos(), d.equipamiento(), List.of(), "Ninguna",
                d.impacto(), d.suelo(), d.rodillas(), d.colgado(), d.sobreCabeza()));
        return u;
    }

    private UserDetailsImpl principal(long u) {
        return UserDetailsImpl.build(usuarios.findById(u).orElseThrow());
    }

    // =======================================================================
    // Metricas de Replicate (consulta gratuita de cada prediccion)
    // =======================================================================

    record Metrica(String id, String usuario, String semana, String intento, String estado, String modelo,
                   String version, long entrada, long salida, double segundos, long totalMs, String resultado) {}

    private List<Metrica> metricasDeReplicate() throws Exception {
        if (!IA) return List.of();
        var lista = new ArrayList<Metrica>();
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        var json = new ObjectMapper();
        Files.createDirectories(SALIDA.resolve("predicciones"));
        var patron = Pattern.compile("user=(\\S+) week=(\\S+) attempt=(\\S+) prediction_id=(\\S+) .*total_ms=(\\d+) resultado=(\\S+)");
        for (var t : tiempos) {
            var x = patron.matcher(t);
            if (!x.find() || x.group(4).equals("desconocido")) continue;
            String id = x.group(4);
            try {
                var r = http.send(HttpRequest.newBuilder(URI.create("https://api.replicate.com/v1/predictions/" + id))
                        .header("Authorization", "Bearer proxy").timeout(Duration.ofSeconds(30)).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                var p = (ObjectNode) json.readTree(r.body());
                p.remove("urls");
                Files.writeString(SALIDA.resolve("predicciones").resolve(id + ".json"), p.toPrettyString());
                var mt = p.path("metrics");
                lista.add(new Metrica(id, x.group(1), x.group(2), x.group(3), p.path("status").asText(),
                        p.path("model").asText(), p.path("version").asText(), mt.path("token_input_count").asLong(),
                        mt.path("token_output_count").asLong(), mt.path("predict_time").asDouble(),
                        Long.parseLong(x.group(5)), x.group(6)));
            } catch (Exception e) {
                lista.add(new Metrica(id, x.group(1), x.group(2), x.group(3), "sin metricas: " + e.getClass().getSimpleName(),
                        "-", "-", 0, 0, 0, Long.parseLong(x.group(5)), x.group(6)));
            }
        }
        return lista;
    }

    // =======================================================================
    // Salida: CSV y JSON; los informes se redactan a partir de aqui
    // =======================================================================

    private static String csv(Object o) {
        if (o == null) return "";
        String s = o instanceof Double dd ? String.format(Locale.ROOT, "%.2f", dd) : String.valueOf(o);
        return s.contains(",") || s.contains("\"") || s.contains("\n") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }

    private static String linea(Object... v) {
        return Arrays.stream(v).map(CampanaMvp8UsuariosIT::csv).collect(Collectors.joining(",")) + "\n";
    }

    private void escribir(List<Resultado> resultados, List<Metrica> metricas, long ms) throws Exception {
        var json = new ObjectMapper().findAndRegisterModules();
        var usuariosCsv = new StringBuilder(linea("usuario", "user_id", "objetivo", "nivel", "dias_disponibles",
                "dias_por_semana_declarados", "lugar", "entorno", "equipamiento", "minutos", "sexo", "nacimiento",
                "altura_cm", "peso_kg", "impacto", "suelo", "rodillas", "colgado", "sobre_cabeza", "plan_inicial", "corte"));
        var semanasCsv = new StringBuilder(linea("usuario", "semana", "inicio", "guion", "plan_id", "origen_plan",
                "validacion_independiente", "programadas", "focos", "completas", "parciales", "omitidas", "sin_registro",
                "adherencia_formula_orden", "adherencia_backend_oficial", "adherencia_backend_observada", "riesgo",
                "motivos", "cambio_perfil", "decision", "estado_v32", "intervenciones", "llamadas_ia_contadas",
                "ai_calls_v32", "plan_siguiente", "pasos", "pasos_bloqueados", "retirados(ejercicio@sesion)",
                "violaciones", "nota"));
        var sesionesCsv = new StringBuilder(linea("usuario", "semana", "sesion", "origen", "fecha", "foco", "minutos",
                "ejercicios", "accion", "estado_final", "cumplimiento_pct", "motivo", "contexto", "minutos_declarados",
                "rpe", "dosis"));
        var pasosCsv = new StringBuilder(linea("usuario", "semana", "tipo", "sesion_origen", "ejercicio", "dosis_antes",
                "dosis_despues", "volumen_antes", "volumen_despues", "volumen_base", "techo", "incremento_pct",
                "estado_reserva", "resultado"));
        for (var r : resultados) {
            var d = r.usuario();
            usuariosCsv.append(linea(d.id(), r.userId(), d.objetivo(), d.nivel(), d.dias(), d.dias().size(), d.lugar(),
                    d.entorno(), d.equipamiento(), d.minutos(), d.sexo(), d.nacimiento(), d.altura(), d.peso(),
                    d.impacto(), d.suelo(), d.rodillas(), d.colgado(), d.sobreCabeza(), r.inicial(), r.corte()));
            for (var f : r.semanas())
                semanasCsv.append(linea(f.usuario(), f.semana(), f.inicio(), f.guion(), f.planId(), f.origenPlan(),
                        f.validacionIndependiente(), f.programadas(), f.focos(), f.completas(), f.parciales(),
                        f.omitidas(), f.sinRegistro(), f.adherenciaOrden(), f.adherenciaBackend(),
                        f.adherenciaObservada(), f.riesgo(), f.motivos(), f.cambioPerfil(), f.decision(), f.estado(),
                        f.intervenciones(), f.llamadasIa(), f.aiCallsV32(), f.siguientePlan(), f.pasos(), f.bloqueos(),
                        f.retirados(), String.join(" | ", f.violaciones()), f.nota()));
            for (var s : r.sesiones())
                sesionesCsv.append(linea(s.usuario(), s.semana(), s.sesion(), s.origen(), s.fecha(), s.foco(),
                        s.minutos(), s.ejercicios(), s.accion(), s.estado(), s.cumplimiento(), s.motivo(), s.contexto(),
                        s.minutosDeclarados(), s.rpe(), s.dosis()));
            for (var p : r.pasos())
                pasosCsv.append(linea(p.usuario(), p.semana(), p.tipo(), p.sesion(), p.ejercicio(), p.antes(),
                        p.despues(), p.volumenAntes(), p.volumenDespues(), p.volumenBase(), p.techo(),
                        p.incrementoPct(), p.estadoReserva(), p.resultado()));
        }
        var llamadasCsv = new StringBuilder(linea("etiqueta", "operacion", "user_id", "intento", "latencia_ms",
                "rechazos_previos", "error"));
        for (var l : llamadas)
            llamadasCsv.append(linea(l.etiqueta(), l.tipo(), l.usuario(), l.intento(), l.ms(),
                    String.join(" | ", l.problemasPrevios()), l.error()));
        var metricasCsv = new StringBuilder(linea("prediction_id", "user_id", "week", "attempt", "estado", "modelo",
                "version", "tokens_entrada", "tokens_salida", "predict_time_s", "total_ms_backend", "resultado",
                "coste_estimado_usd"));
        for (var m : metricas)
            metricasCsv.append(linea(m.id(), m.usuario(), m.semana(), m.intento(), m.estado(), m.modelo(), m.version(),
                    m.entrada(), m.salida(), m.segundos(), m.totalMs(), m.resultado(),
                    m.entrada() / 1e6 * USD_ENTRADA + m.salida() / 1e6 * USD_SALIDA));
        Files.writeString(SALIDA.resolve("USUARIOS_SINTETICOS.csv"), usuariosCsv);
        Files.writeString(SALIDA.resolve("ADHERENCIA_SEMANAL.csv"), semanasCsv);
        Files.writeString(SALIDA.resolve("SESIONES_8_SEMANAS.csv"), sesionesCsv);
        Files.writeString(SALIDA.resolve("PROGRESION_Y_REINCORPORACION.csv"), pasosCsv);
        Files.writeString(SALIDA.resolve("LLAMADAS_IA.csv"), llamadasCsv);
        Files.writeString(SALIDA.resolve("METRICAS_REPLICATE.csv"), metricasCsv);
        try (var w = Files.newBufferedWriter(SALIDA.resolve("llamadas.jsonl"))) {
            for (var l : llamadas) w.write(json.writeValueAsString(l) + "\n");
        }
        Files.write(SALIDA.resolve("eventos.log"), new ArrayList<>(eventos));
        if (!FOCO.isEmpty()) Files.write(SALIDA.resolve("repeticiones.log"), new ArrayList<>(repeticiones));
        Files.write(SALIDA.resolve("tiempos.log"), new ArrayList<>(tiempos));

        // Integridad V31-V34 sobre toda la base efimera.
        var texto = Files.readString(Path.of("docs/FitSense/consultas/linaje_v31_v32.sql")).lines()
                .filter(l -> !l.stripLeading().startsWith("--")).collect(Collectors.joining("\n"));
        var linaje = jdbc.queryForList(texto.split(";")[0].strip());
        var resultadoAdaptadas = jdbc.queryForList(Files.readString(Path.of("docs/FitSense/consultas/resultado_sesion_adaptada.sql"))
                .lines().filter(l -> !l.stripLeading().startsWith("--")).collect(Collectors.joining("\n")).split(";")[0].strip());
        Files.writeString(SALIDA.resolve("resultado_sesiones_adaptadas.json"),
                json.writerWithDefaultPrettyPrinter().writeValueAsString(resultadoAdaptadas));

        long entrada = metricas.stream().mapToLong(Metrica::entrada).sum();
        long salida = metricas.stream().mapToLong(Metrica::salida).sum();
        var resumen = new LinkedHashMap<String, Object>();
        resumen.put("modo", IA ? "IA real" : "ensayo seco (sin IA)");
        resumen.put("duracion_min", ms / 60000);
        resumen.put("llamadas", llamadas.size());
        resumen.put("llamadas_con_error", llamadas.stream().filter(l -> l.error() != null).count());
        resumen.put("predicciones_con_metricas", metricas.size());
        resumen.put("tokens_entrada", entrada);
        resumen.put("tokens_salida", salida);
        resumen.put("coste_estimado_usd", entrada / 1e6 * USD_ENTRADA + salida / 1e6 * USD_SALIDA);
        resumen.put("latencia_media_ms", (long) llamadas.stream().mapToLong(Llamada::ms).average().orElse(0));
        resumen.put("latencia_max_ms", llamadas.stream().mapToLong(Llamada::ms).max().orElse(0));
        resumen.put("motivo_parada", motivoParada);
        resumen.put("violaciones_linaje", linaje.size());
        resumen.put("violaciones_comprobaciones", resultados.stream().flatMap(r -> r.semanas().stream())
                .flatMap(f -> f.violaciones().stream().map(v -> f.usuario() + " s" + f.semana() + ": " + v)).toList());
        resumen.put("cortes", resultados.stream().filter(r -> r.corte() != null)
                .map(r -> r.usuario().id() + ": " + r.corte()).toList());
        Files.writeString(SALIDA.resolve("resumen.json"), json.writerWithDefaultPrettyPrinter().writeValueAsString(resumen));
        System.out.println("CAMPANA_MVP " + json.writeValueAsString(resumen));
    }
}
