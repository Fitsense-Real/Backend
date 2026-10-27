package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.planning.interfaces.rest.TrainingPlanController;
import main.web.services.fitsense.planning.interfaces.rest.resources.SkipWorkoutResource;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * V35 · PROGRESION Y REINCORPORACION CONDICIONAL, MULTISEMANA, CON EL CICLO REAL.
 * <p>
 * {@link WeeklyCycleService#runFor} sobre PostgreSQL real, con la progresion
 * encendida SOLO en esta prueba ({@code fitsense.adaptation.progression.enabled=true},
 * que activa tambien la V24 por capacidades en la adaptacion, D-P11). La IA de
 * correccion esta simulada y solo hace lo que su intervencion permite (o, en
 * la prueba negativa, intenta subir una dosis). Nada llega a Replicate.
 * <p>
 * Cada semana publicada se comprueba con la consulta de integridad del linaje
 * ({@code linaje_v31_v32.sql}) y las invariantes V31/V32/V33/V35 de la base.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.progression.enabled=true"
})
class ProgresionIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoBean ReplicateSessionFixGenerator ia;

    @Autowired WeeklyCycleService ciclo;
    @Autowired PlanningContextFacade planning;
    @Autowired UserProfileCommandService perfiles;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired TrainingPlanController planes;
    @Autowired UserRepository usuarios;
    @Autowired SessionDurationEstimator estimador;
    @Autowired JdbcTemplate jdbc;

    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final LocalDate S1 = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
    static final List<String> EQUIPAMIENTO = List.of("dumbbell", "barbell", "cable", "leverage machine", "bench", "band");

    enum Dia { HECHO, HECHO_CON_FATIGA, FATIGA, TIEMPO, NADA }

    /** Que hace la IA simulada con una sesion senalada. */
    enum Ia { PERMITIDO, SUBE_DOSIS }

    private Ia modoIa = Ia.PERMITIDO;

    record Ejercicio(long id, String tipo, Short series, Short reps, Integer segundos, Short descanso) {
        int volumen() {
            int s = series == null ? 1 : series;
            return "DURATION".equals(tipo) ? Math.round((float) (s * (segundos == null ? 0 : segundos)) / 30)
                    : s * (reps == null ? 0 : reps);
        }
    }

    record Sesion(long id, LocalDate fecha, String enfoque, int minutos, List<Ejercicio> ejercicios) {
        int volumen() {
            return ejercicios.stream().mapToInt(Ejercicio::volumen).sum();
        }
    }

    @BeforeEach
    void iaSimulada() {
        reset(ia);
        modoIa = Ia.PERMITIDO;
        when(ia.isEnabled()).thenReturn(true);
        when(ia.modelName()).thenReturn("simulado");
        when(ia.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{}", "salida-simulada"));
        when(ia.interpretar(anyString(), any())).thenAnswer(inv -> corregir(inv.getArgument(1)));
    }

    // =======================================================================
    // 1 · Progresion ordinaria en los cuatro objetivos
    // =======================================================================

    @ParameterizedTest(name = "{0}")
    @EnumSource(GoalType.class)
    @DisplayName("dos semanas cumplidas -> UN paso; la semana siguiente copia el paso; despues, otro paso")
    void progresionOrdinaria(GoalType objetivo) throws Exception {
        long u = participante(objetivo, List.of((short) 1, (short) 3, (short) 5));
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var raiz = semana(w1);

        ejecutar(u, w1, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        long w2 = ciclo.runFor(u, S1, S1.plusWeeks(1)).orElseThrow();
        var ev2 = ultimoIntento(u);
        assertEquals("NO_CHANGE", ev2.get("decision"), String.valueOf(ev2));
        assertEquals("{}", ev2.get("progressed_sessions"), "una semana cumplida no basta: " + ev2);
        assertEquals(dosis(raiz), dosis(semana(w2)));

        ejecutar(u, w2, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        clearInvocations(ia);
        long w3 = ciclo.runFor(u, S1.plusWeeks(1), S1.plusWeeks(2)).orElseThrow();
        verify(ia, never()).pedirRespuesta(any(), any(), any(), any(), anyInt());
        var ev3 = ultimoIntento(u);
        var paso1 = comprobarPaso(objetivo, semana(w2), semana(w3), raiz, ev3, "PROGRESION");

        // La semana siguiente: O -> S fue un paso, asi que no hay otro; la copia
        // conserva la dosis progresada (HEREDADA) y V24 la acepta.
        ejecutar(u, w3, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        long w4 = ciclo.runFor(u, S1.plusWeeks(2), S1.plusWeeks(3)).orElseThrow();
        var ev4 = ultimoIntento(u);
        assertEquals("SIN_ORDEN", ev4.get("status"), String.valueOf(ev4));
        var w3s = semana(w3);
        var w4s = semana(w4);
        for (int i = 0; i < w3s.size(); i++)
            if (w3s.get(i).id() == paso1.sesionNueva())
                assertEquals(w3s.get(i).ejercicios(), w4s.get(i).ejercicios(), "la dosis progresada se hereda tal cual");

        ejecutar(u, w4, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        long w5 = ciclo.runFor(u, S1.plusWeeks(3), S1.plusWeeks(4)).orElseThrow();
        var ev5 = ultimoIntento(u);
        if (!"{}".equals(ev5.get("progressed_sessions")))
            comprobarPaso(objetivo, w4s, semana(w5), raiz, ev5, null);

        assertSinViolaciones();
        System.out.println("PROGRESION " + objetivo + " w3=" + ev3.get("progressed_sessions") + " paso=" + paso1
                + " w4=" + ev4.get("progressed_sessions") + " w5=" + ev5.get("progressed_sessions")
                + " volumenes w1..w5=" + List.of(volumen(raiz), volumen(semana(w2)), volumen(w3s), volumen(w4s),
                volumen(semana(w5))));
    }

    // =======================================================================
    // 2 · D-P11 y recuperacion: la reduccion por fatiga se acepta y se recupera
    // =======================================================================

    @Test
    @DisplayName("D-P11: fatiga -> la IA baja 2 repeticiones (V24 por capacidades activa) -> copia -> recuperacion")
    void reduccionPorFatigaYRecuperacion() throws Exception {
        long u = participante(GoalType.GENERAL_FITNESS, List.of((short) 1, (short) 3, (short) 5));
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var referencia = semana(w1);
        ejecutar(u, w1, Dia.HECHO, Dia.HECHO, Dia.FATIGA);
        long w2 = ciclo.runFor(u, S1, S1.plusWeeks(1)).orElseThrow();
        var ev2 = ultimoIntento(u);
        assertEquals("APLICADA", ev2.get("status"), "la reduccion por fatiga tiene que publicarse: " + ev2);
        assertEquals("{REVISAR_CARGA_Y_RECUPERACION}", ev2.get("targeted_interventions"));
        var adaptada = semana(w2);
        assertNotEquals(referencia.get(2).ejercicios(), adaptada.get(2).ejercicios());

        ejecutar(u, w2, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        long w3 = ciclo.runFor(u, S1.plusWeeks(1), S1.plusWeeks(2)).orElseThrow();
        var ev3 = ultimoIntento(u);
        assertEquals("SIN_ORDEN", ev3.get("status"), "la copia de la dosis reducida (HEREDADA) se publica: " + ev3);
        assertEquals(adaptada.get(2).ejercicios(), semana(w3).get(2).ejercicios());
        assertFalse(String.valueOf(ev3.get("progressed_sessions")).contains(String.valueOf(adaptada.get(2).id())),
                "O -> S fue la adaptacion, no una copia: aun no hay dos semanas");

        ejecutar(u, w3, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        long w4 = ciclo.runFor(u, S1.plusWeeks(2), S1.plusWeeks(3)).orElseThrow();
        var ev4 = ultimoIntento(u);
        var viernes3 = semana(w3).get(2);
        var viernes4 = semana(w4).get(2);
        System.out.println("RECUPERACION ref=" + referencia.get(2).ejercicios() + " adaptada="
                + adaptada.get(2).ejercicios() + " w4=" + viernes4.ejercicios() + " ev4=" + ev4.get("progressed_sessions")
                + " " + ev4.get("conservation_report"));
        if (String.valueOf(ev4.get("progressed_sessions")).contains(String.valueOf(viernes3.id()))) {
            assertTrue(informe(ev4).contains("\"tipo\":\"RECUPERACION\""),
                    "el viernes reducido se recupera antes de progresar: " + ev4.get("conservation_report"));
            var cambiado = cambio(viernes3, viernes4);
            var ref = referencia.get(2).ejercicios().stream().filter(e -> e.id() == cambiado.id()).findFirst().orElseThrow();
            assertTrue(cambiado.reps() <= ref.reps(), "la recuperacion nunca supera la referencia");
        }
        assertSinViolaciones();
    }

    // =======================================================================
    // 3 · La IA no puede subir dosis
    // =======================================================================

    @Test
    @DisplayName("la IA intenta subir la dosis de la sesion senalada: V8C la rechaza y no se publica la subida")
    void iaNoSubeDosis() throws Exception {
        long u = participante(GoalType.GENERAL_FITNESS, List.of((short) 1, (short) 3, (short) 5));
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var referencia = semana(w1);
        modoIa = Ia.SUBE_DOSIS;
        ejecutar(u, w1, Dia.HECHO, Dia.HECHO, Dia.FATIGA);
        var w2 = ciclo.runFor(u, S1, S1.plusWeeks(1));
        var ev = ultimoIntento(u);
        System.out.println("IA_SUBE " + ev.get("status") + " " + ev.get("rejection_reason"));
        assertNotEquals("APLICADA", ev.get("status"), String.valueOf(ev));
        w2.ifPresent(plan -> {
            var viernes = semana(plan).get(2);
            for (var e : viernes.ejercicios()) {
                var antes = referencia.get(2).ejercicios().stream().filter(x -> x.id() == e.id()).findFirst();
                antes.ifPresent(a -> assertTrue(noMayor(e, a), "se publico una subida de la IA: " + e + " > " + a));
            }
        });
        assertSinViolaciones();
    }

    // =======================================================================
    // 4 · D-P7 y reincorporacion (GAIN_MUSCLE, viernes acortado por falta de tiempo)
    // =======================================================================

    @Test
    @DisplayName("D-P7 + R2: el viernes acortado bajo el piso progresa con la exencion y, al final, reincorpora")
    void acortadaProgresaYReincorpora() throws Exception {
        long u = participante(GoalType.GAIN_MUSCLE, List.of((short) 1, (short) 3, (short) 5));
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var raiz = semana(w1);
        ejecutar(u, w1, Dia.HECHO, Dia.HECHO, Dia.TIEMPO);
        long plan = ciclo.runFor(u, S1, S1.plusWeeks(1)).orElseThrow();
        var ev = ultimoIntento(u);
        assertEquals("{ACORTAR_O_REORGANIZAR}", ev.get("targeted_interventions"), String.valueOf(ev));
        assertEquals("APLICADA", ev.get("status"), String.valueOf(ev));
        var acortado = semana(plan).get(2);
        int piso = 45 * 70 / 100;
        assertTrue(acortado.minutos() < piso, "premisa: el viernes acortado (" + acortado.minutos()
                + ") queda bajo el piso " + piso);
        var retirado = raiz.get(2).ejercicios().stream()
                .filter(e -> acortado.ejercicios().stream().noneMatch(x -> x.id() == e.id())).findFirst().orElseThrow();
        int base = raiz.get(2).volumen();
        int techo = (int) Math.floor(base * 1.10);

        boolean progresoBajoElPiso = false, reincorporado = false;
        var traza = new ArrayList<String>();
        for (int k = 1; k <= 24 && !reincorporado; k++) {
            ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.HECHO);
            var antes = semana(plan);
            long siguiente = ciclo.runFor(u, S1.plusWeeks(k), S1.plusWeeks(k + 1)).orElseThrow();
            var evk = ultimoIntento(u);
            assertEquals("SIN_ORDEN", evk.get("status"), "semana " + (k + 1) + ": " + evk);
            var despues = semana(siguiente);
            var viernes = despues.get(2);
            assertTrue(viernes.volumen() <= techo, "techo superado: " + viernes.volumen() + " > " + techo);
            if (String.valueOf(evk.get("progressed_sessions")).equals("{" + antes.get(2).id() + "}")) {
                var informe = informe(evk);
                if (informe.contains("\"tipo\":\"REINCORPORACION\"")) {
                    reincorporado = true;
                    var ultimo = viernes.ejercicios().get(viernes.ejercicios().size() - 1);
                    assertEquals(retirado.id(), ultimo.id(), "vuelve el retirado, al final de la sesion");
                    assertEquals(retirado, ultimo, "con su dosis inicial exacta (la de la generacion)");
                    assertTrue(viernes.volumen() <= techo);
                } else {
                    assertTrue(informe.contains("\"estado_reserva\":\"PENDIENTE\""), informe);
                    assertTrue(viernes.volumen() <= techo - retirado.volumen(), "R2: la reserva del retirado se mantiene");
                    if (viernes.minutos() < piso) progresoBajoElPiso = true;
                }
            }
            traza.add("w" + (k + 1) + " v=" + viernes.volumen() + " min=" + viernes.minutos() + " "
                    + evk.get("progressed_sessions"));
            plan = siguiente;
        }
        System.out.println("REINCORPORACION base=" + base + " techo=" + techo + " retirado=" + retirado + " " + traza);
        assertTrue(progresoBajoElPiso, "D-P7: el viernes tenia que progresar por debajo del piso: " + traza);
        assertTrue(reincorporado, "el retirado tenia que volver: " + traza);
        assertSinViolaciones();
    }

    // =======================================================================
    // Comprobaciones
    // =======================================================================

    record Paso(long sesionNueva, long ejercicio, String antes, String despues) {}

    /** Exactamente UN ejercicio de UNA sesion cambia, hacia arriba, dentro de los limites. */
    private Paso comprobarPaso(GoalType objetivo, List<Sesion> medida, List<Sesion> nueva, List<Sesion> raiz,
                               Map<String, Object> ev, String tipo) {
        assertEquals("NO_CHANGE", ev.get("decision"), String.valueOf(ev));
        assertEquals("SIN_ORDEN", ev.get("status"), String.valueOf(ev));
        assertEquals("CONSERVED", ev.get("generation_source"));
        assertEquals(0, ((Number) ev.get("ai_calls")).intValue());
        var progresadas = String.valueOf(ev.get("progressed_sessions"));
        assertNotEquals("{}", progresadas, objetivo + ": tenia que haber un paso. " + ev);
        if (tipo != null)
            assertTrue(informe(ev).contains("\"tipo\":\"" + tipo + "\""),
                    String.valueOf(ev.get("conservation_report")));
        Paso paso = null;
        int cambiadas = 0;
        for (int i = 0; i < medida.size(); i++) {
            var a = medida.get(i);
            var b = nueva.get(i);
            assertEquals(a.id(), origenDe(b.id()), "V31");
            assertEquals(a.fecha().plusWeeks(1), b.fecha());
            assertEquals(a.enfoque(), b.enfoque());
            if (a.ejercicios().equals(b.ejercicios())) {
                assertEquals(a.minutos(), b.minutos());
                continue;
            }
            cambiadas++;
            assertEquals("{" + a.id() + "}", progresadas);
            var x = cambio(a, b);
            var antes = a.ejercicios().stream().filter(e -> e.id() == x.id()).findFirst().orElseThrow();
            boolean unaRep = Objects.equals(x.series(), antes.series()) && x.reps() == antes.reps() + 1;
            boolean unaSerie = x.series() == antes.series() + 1 && Objects.equals(x.reps(), antes.reps());
            assertTrue(unaRep || unaSerie, "+1 repeticion o +1 serie: " + antes + " -> " + x);
            assertEquals(antes.descanso(), x.descanso(), "el descanso no cambia");
            int delta = b.volumen() - a.volumen();
            assertTrue(100.0 * delta / a.volumen() <= 5.0, "paso de la sesion > 5 %: " + delta + "/" + a.volumen());
            assertTrue(b.volumen() <= Math.floor(raiz.get(i).volumen() * 1.10), "techo 1,10 x V_base");
            assertTrue(b.minutos() >= a.minutos(), "los minutos no bajan en un paso");
            paso = new Paso(b.id(), x.id(), antes.toString(), x.toString());
        }
        assertEquals(1, cambiadas, "una modificacion por ciclo");
        return paso;
    }

    /** El unico ejercicio cuya dosis cambio entre dos versiones de la sesion (mismo conjunto de ejercicios). */
    private static Ejercicio cambio(Sesion a, Sesion b) {
        assertEquals(a.ejercicios().stream().map(Ejercicio::id).toList(), b.ejercicios().stream().map(Ejercicio::id).toList(),
                "mismos ejercicios y orden");
        var distintos = new ArrayList<Ejercicio>();
        for (int k = 0; k < a.ejercicios().size(); k++)
            if (!a.ejercicios().get(k).equals(b.ejercicios().get(k))) distintos.add(b.ejercicios().get(k));
        assertEquals(1, distintos.size(), "un solo ejercicio: " + distintos);
        return distintos.get(0);
    }

    /** El informe V32 sin espacios, para buscar claves sin depender del formato de jsonb. */
    private static String informe(Map<String, Object> ev) {
        return String.valueOf(ev.get("conservation_report")).replace(" ", "");
    }

    private static boolean noMayor(Ejercicio e, Ejercicio antes) {
        return n(e.series()) <= n(antes.series()) && n(e.reps()) <= n(antes.reps())
                && n(e.segundos()) <= n(antes.segundos());
    }

    private static int n(Number x) {
        return x == null ? 0 : x.intValue();
    }

    private static List<List<Ejercicio>> dosis(List<Sesion> semana) {
        return semana.stream().map(Sesion::ejercicios).toList();
    }

    private static List<Integer> volumen(List<Sesion> semana) {
        return semana.stream().map(Sesion::volumen).toList();
    }

    private void assertSinViolaciones() throws Exception {
        var texto = Files.readString(Path.of("docs/FitSense/consultas/linaje_v31_v32.sql")).lines()
                .filter(l -> !l.stripLeading().startsWith("--")).collect(Collectors.joining("\n"));
        assertEquals(List.of(), jdbc.queryForList(texto.split(";")[0].strip()));
        // V35 en la base: a lo sumo una progresada por intento, solo SIN_ORDEN y con plan.
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM plan_adaptation_attempts WHERE"
                + " cardinality(progressed_sessions) > 1 OR (cardinality(progressed_sessions) = 1 AND"
                + " (status <> 'SIN_ORDEN' OR resulting_plan_id IS NULL))", Integer.class));
    }

    // =======================================================================
    // La IA simulada
    // =======================================================================

    /**
     * PERMITIDO: REVISAR_CARGA baja dos repeticiones del primer ejercicio por repeticiones con al menos
     * ocho; ACORTAR retira el ultimo relleno de la sesion. SUBE_DOSIS: sube dos repeticiones, lo que nunca se permite.
     */
    private List<CorrectedSession> corregir(PlanGenerationContext ctx) {
        var salida = new ArrayList<CorrectedSession>();
        for (var senalada : ctx.adjustment().targetedSessions()) {
            long id = senalada.plannedWorkoutId();
            var s = sesion(id);
            var ejercicios = new ArrayList<PlanDraft.DraftExercise>();
            boolean hecho = false;
            int quitar = -1;
            if (senalada.intervencion() == TipoIntervencion.ACORTAR_O_REORGANIZAR)
                for (int k = s.ejercicios().size() - 1; k >= 0 && quitar < 0; k--)
                    if (esRelleno(s.ejercicios().get(k).id())) quitar = k;
            for (int k = 0; k < s.ejercicios().size(); k++) {
                var e = s.ejercicios().get(k);
                if (k == quitar) {   // ACORTAR: el ultimo relleno
                    hecho = true;
                    continue;
                }
                Short reps = e.reps();
                if (senalada.intervencion() == TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION && !hecho
                        && "SETS_REPS".equals(e.tipo()) && reps != null && reps >= 8) {
                    reps = (short) (modoIa == Ia.SUBE_DOSIS ? reps + 2 : reps - 2);
                    hecho = true;
                }
                ejercicios.add(new PlanDraft.DraftExercise(e.id(), PrescriptionType.valueOf(e.tipo()), e.series(),
                        reps, e.segundos(), null, e.descanso(), null));
            }
            var fecha = s.fecha().plusWeeks(1);
            int minutos = estimador.estimateMinutes(new PlanDraft.DraftWorkout(fecha,
                    WorkoutFocus.valueOf(s.enfoque()), "x", 0, ejercicios), ctx.prescription());
            salida.add(new CorrectedSession(id, fecha, minutos, ejercicios, "ajuste simulado"));
        }
        return salida;
    }

    private boolean esRelleno(long exerciseId) {
        String patron = jdbc.queryForObject("SELECT movement_pattern FROM exercises WHERE exercise_id = ?",
                String.class, exerciseId);
        return MandatorySlot.ofMovementPattern(String.valueOf(patron)).isEmpty();
    }

    // =======================================================================
    // Material
    // =======================================================================

    private long participante(GoalType objetivo, List<Short> dias) {
        String email = "progresion" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long userId = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(userId, LocalDate.of(1992, 5, 10), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("68"), null, FitnessLevel.INTERMEDIATE,
                objetivo, "prueba de progresion", TrainingLocation.GYM, (short) dias.size(),
                dias, (short) 45, EQUIPAMIENTO, List.of(), "Ninguna", true, true, true, true, true));
        return userId;
    }

    private List<Sesion> semana(long planId) {
        return jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                + " ORDER BY scheduled_date", Long.class, planId).stream().map(this::sesion).toList();
    }

    private Sesion sesion(long id) {
        var f = jdbc.queryForMap("SELECT scheduled_date, focus_code, expected_duration_minutes FROM planned_workouts"
                + " WHERE planned_workout_id = ?", id);
        var ejercicios = jdbc.query("SELECT exercise_id, prescription_type, planned_sets, planned_reps,"
                        + " planned_duration_seconds, rest_seconds FROM planned_workout_exercises"
                        + " WHERE planned_workout_id = ? ORDER BY exercise_order",
                (rs, k) -> new Ejercicio(rs.getLong(1), rs.getString(2), rs.getObject(3, Short.class),
                        rs.getObject(4, Short.class), rs.getObject(5, Integer.class), rs.getObject(6, Short.class)), id);
        return new Sesion(id, ((java.sql.Date) f.get("scheduled_date")).toLocalDate(), (String) f.get("focus_code"),
                ((Number) f.get("expected_duration_minutes")).intValue(), ejercicios);
    }

    private Long origenDe(long plannedWorkoutId) {
        return jdbc.queryForObject("SELECT source_planned_workout_id FROM planned_workouts"
                + " WHERE planned_workout_id = ?", Long.class, plannedWorkoutId);
    }

    private void ejecutar(long u, long planId, Dia... dias) {
        var principal = UserDetailsImpl.build(usuarios.findById(u).orElseThrow());
        var ids = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                + " ORDER BY scheduled_date", Long.class, planId);
        for (int i = 0; i < ids.size(); i++) {
            long w = ids.get(i);
            switch (dias[i]) {
                case HECHO, HECHO_CON_FATIGA -> {
                    long sesion = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
                    var filas = jdbc.queryForList("SELECT planned_exercise_id, prescription_type, planned_sets,"
                            + " planned_reps, planned_duration_seconds FROM planned_workout_exercises"
                            + " WHERE planned_workout_id = ? ORDER BY exercise_order", w);
                    for (int k = 0; k < filas.size(); k++) {
                        var f = filas.get(k);
                        boolean duracion = "DURATION".equals(f.get("prescription_type"));
                        int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
                        int total = duracion ? series * ((Number) f.get("planned_duration_seconds")).intValue()
                                : series * ((Number) f.get("planned_reps")).intValue();
                        boolean fatiga = dias[i] == Dia.HECHO_CON_FATIGA && k == filas.size() - 1;
                        if (fatiga) total = total / 2;
                        sesiones.handle(new RecordSessionExerciseCommand(u, sesion,
                                ((Number) f.get("planned_exercise_id")).longValue(), (short) series,
                                duracion ? null : total, duracion ? total : null, null,
                                fatiga ? SkipReason.FATIGUE : null));
                    }
                    sesiones.handle(new FinishWorkoutSessionCommand(u, sesion, (short) 6, (short) 4, (short) 45));
                }
                case FATIGA -> planes.skip(principal, w, new SkipWorkoutResource(SkipReason.FATIGUE));
                case TIEMPO -> planes.skip(principal, w, new SkipWorkoutResource(SkipReason.LACK_OF_TIME));
                case NADA -> { }
            }
        }
    }

    private Map<String, Object> ultimoIntento(long u) {
        return jdbc.queryForMap("SELECT decision, status, generation_source, ai_calls, resulting_plan_id,"
                + " targeted_interventions::text, applied_sessions::text, no_effect_sessions::text,"
                + " conserved_sessions::text, progressed_sessions::text, conservation_report::text, rejection_reason"
                + " FROM plan_adaptation_attempts WHERE user_id = ? ORDER BY attempt_id DESC LIMIT 1", u);
    }
}
