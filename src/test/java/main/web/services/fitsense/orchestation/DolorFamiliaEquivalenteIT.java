package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.RelojPruebas;
import main.web.services.fitsense.catalog.domain.services.FamiliaDeMovimientoEquivalente;
import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.application.internal.commandservices.PainMemoryService;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.GenerationSource;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * O20 · OPCION A (orden de seguridad del 24/11/2026): tras dolor, ni el ejercicio que dolio NI NINGUNA VARIANTE DE SU
 * FAMILIA DE MOVIMIENTO EQUIVALENTE vuelve a un plan mientras su exclusion siga activa.
 * <p>
 * El caso real (evaluacion gpt-5-mini, corrida G5M_MEDIUM_P, O20): dolor en 537 «dumbbell front raise» en la sesion
 * del viernes; 537 estaba TAMBIEN en la sesion del lunes, conservada; SUST-SEG-1 lo sustituyo por 538 «dumbbell front
 * raise v. 2», el mismo gesto. Aqui se reproduce esa forma exacta sobre un plan real: el plan lo genera el motor de
 * reglas y se le coloca 537 en dos sesiones (lo que hizo la IA en O20), se registra el dolor en una y se ejecuta el
 * CICLO SEMANAL REAL. La IA de adaptacion se simula SIEMPRE RECHAZADA, para que decida el camino determinista de
 * seguridad (SUST-SEG-1, retirada localizada, NO_FACTIBLE, P-03); sin Replicate.
 * <p>
 * No cambia ningun validador: V8C, V24, V1 y los demas son los de {@code generation}.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("O20 opcion A: tras dolor, ninguna variante equivalente del ejercicio vuelve mientras dure la exclusion")
class DolorFamiliaEquivalenteIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoBean ReplicateSessionFixGenerator ia;
    @MockitoBean main.web.services.fitsense.planning.infrastructure.scheduling.OverdueWorkoutScheduler vencidas;

    @Autowired WeeklyCycleService ciclo;
    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired PlanDraftValidator validador;
    @Autowired PainMemoryService dolor;
    @Autowired UserProfileCommandService perfiles;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired JdbcTemplate jdbc;
    @Autowired main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalCatalogService catalogo;

    static final LocalDate S1 = RelojPruebas.SEMANA_REFERENCIA;
    static final AtomicInteger SECUENCIA = new AtomicInteger();
    /** «dumbbell front raise» y «dumbbell front raise v. 2» (catalogo FITSENSE-CATALOGO-TECNICO-CONSERVADOR-v1.1). */
    static final long FRONT_RAISE = 537L, FRONT_RAISE_V2 = 538L;
    /** «dumbbell lateral raise»: mismo patron (SHOULDER_RAISE), OTRO movimiento. Sustituto legitimo. */
    static final long LATERAL_RAISE = 578L;
    static final List<String> CASA = List.of("body weight", "dumbbell", "band");

    @BeforeEach
    void iaSiempreRechazada() {
        reset(ia);
        when(ia.isEnabled()).thenReturn(true);
        when(ia.modelName()).thenReturn("simulado");
        when(ia.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{}", "salida-simulada"));
        when(ia.interpretar(anyString(), any())).thenThrow(new InvalidPlanDraftException(
                List.of("PRUEBA: propuesta rechazada para que decida el camino determinista de seguridad")));
    }

    // ===================================================================================================== catalogo

    @Test
    @Order(1)
    @DisplayName("Premisa del catalogo: la familia de 537 son sus variantes y el mismo gesto; 578 y 539 no")
    void premisaCatalogo() {
        var nombres = nombresEn(Set.of(FRONT_RAISE, FRONT_RAISE_V2, LATERAL_RAISE));
        assertEquals("dumbbell front raise", nombres.get(FRONT_RAISE));
        assertEquals("dumbbell front raise v. 2", nombres.get(FRONT_RAISE_V2));
        assertTrue(FamiliaDeMovimientoEquivalente.mismaFamilia(nombres.get(FRONT_RAISE), nombres.get(FRONT_RAISE_V2)));
        assertFalse(FamiliaDeMovimientoEquivalente.mismaFamilia(nombres.get(FRONT_RAISE), nombres.get(LATERAL_RAISE)));
        // Nivel 1: 538 (v. 2) y el duplicado historico inactivo 28. Nivel 2 (grupo confirmado «front raise» de la lista
        // explicita): las elevaciones frontales con otro material o postura. NO: las laterales (578, 539), «lateral to
        // front raise» (579) ni «front raise above head» (746), que comparten patron y musculo y son otro movimiento.
        var f = familia(FRONT_RAISE);
        assertTrue(f.containsAll(Set.of(FRONT_RAISE, FRONT_RAISE_V2, 28L, 150L, 334L, 703L, 707L)), f.toString());
        assertTrue(f.contains(99L), "band front raise: otro material");
        assertFalse(f.contains(LATERAL_RAISE) || f.contains(539L) || f.contains(579L) || f.contains(746L), f.toString());
        var porque = catalogo.fetchEquivalentVariants(Set.of(FRONT_RAISE)).get(FRONT_RAISE).stream()
                .collect(Collectors.toMap(e -> e.exerciseId(), e -> e));
        assertEquals(FamiliaDeMovimientoEquivalente.NIVEL_VARIANTE, porque.get(FRONT_RAISE_V2).level());
        assertEquals(FamiliaDeMovimientoEquivalente.NIVEL_MISMO_GESTO, porque.get(707L).level(), "seated front raise");
        assertTrue(porque.get(707L).groupId().endsWith("front raise") && !porque.get(707L).justification().isBlank());
        assertEquals(f, familiaSegunBackend(FRONT_RAISE), "el backend calcula la misma familia que la regla");
        // Recuento de familias de nivel 1 con mas de un ejercicio ACTIVO (el informe de la evaluacion: 29). Se fija para
        // que un cambio de catalogo obligue a revisar esta prueba.
        assertEquals(29, familiasActivas().size(), familiasActivas().toString());
    }

    @Test
    @Order(7)
    @DisplayName("Inventario de familias del catalogo activo (para revision): cada familia segun el backend")
    void inventario() throws Exception {
        var filas = new ArrayList<String>();
        filas.add("exercise_id;name_en;movement_pattern;target_muscle;equivalentes_activos;nivel_y_grupo");
        var activos = jdbc.queryForList("SELECT exercise_id, name_en, movement_pattern, target_muscle FROM exercises"
                + " WHERE is_active ORDER BY exercise_id");
        var ids = new TreeSet<Long>();
        activos.forEach(f -> ids.add(((Number) f.get("exercise_id")).longValue()));
        var detalle = catalogo.fetchEquivalentVariants(ids);
        var variantes = new HashMap<Long, Set<Long>>();
        detalle.forEach((id, l) -> variantes.put(id, l.stream().map(e -> e.exerciseId())
                .collect(Collectors.toCollection(TreeSet::new))));
        int conEquivalentes = 0;
        for (var f : activos) {
            long id = ((Number) f.get("exercise_id")).longValue();
            var eq = new TreeSet<>(variantes.getOrDefault(id, Set.of()));
            eq.retainAll(ids);
            if (eq.isEmpty()) continue;
            conEquivalentes++;
            filas.add(String.join(";", String.valueOf(id), String.valueOf(f.get("name_en")),
                    String.valueOf(f.get("movement_pattern")), String.valueOf(f.get("target_muscle")), eq.toString(),
                    detalle.get(id).stream().filter(e -> ids.contains(e.exerciseId()))
                            .map(e -> e.exerciseId() + "=" + (e.groupId() == null ? e.level() : e.groupId()))
                            .collect(Collectors.joining(","))));
            // La relacion es simetrica: si A excluye a B tras dolor, B excluye a A.
            for (var otro : eq)
                assertTrue(variantes.getOrDefault(otro, Set.of()).contains(id), id + " <-> " + otro + " no es simetrico");
        }
        java.nio.file.Files.createDirectories(java.nio.file.Path.of("target"));
        java.nio.file.Files.write(java.nio.file.Path.of("target", "familias_equivalentes.csv"), filas);
        System.out.println("INVENTARIO_FAMILIAS activos=" + ids.size() + " con_equivalentes=" + conEquivalentes);
        assertTrue(conEquivalentes > 0);
        // Trazabilidad de la lista explicita: cada miembro de cada grupo confirmado existe en el catalogo activo.
        var claves = new HashSet<String>();
        activos.forEach(f -> claves.add(FamiliaDeMovimientoEquivalente.clave((String) f.get("name_en"))));
        for (var g : FamiliaDeMovimientoEquivalente.grupos())
            for (var k : g.claves()) assertTrue(claves.contains(k), g.id() + ": «" + k + "» no esta en el catalogo activo");
    }

    // ===================================================================================================== elegibles

    @Test
    @Order(2)
    @DisplayName("Con dolor en 537, ni 537 ni 538 son elegibles; 578 (otro movimiento) si. Sin dolor, los dos lo eran")
    void elegiblesSinLaFamilia() {
        long u = participante(false);
        var antes = elegibles(u);
        assertTrue(antes.containsKey(FRONT_RAISE) && antes.containsKey(FRONT_RAISE_V2),
                "premisa: sin dolor, el perfil puede hacer 537 y 538");
        registrarDolorDirecto(u, FRONT_RAISE);

        var despues = elegibles(u);
        assertFalse(despues.containsKey(FRONT_RAISE), "lo que dolio no es elegible (P-02, como antes)");
        assertFalse(despues.containsKey(FRONT_RAISE_V2), "OPCION A: su variante equivalente tampoco");
        var familia = familia(FRONT_RAISE);
        for (var id : familia) assertFalse(despues.containsKey(id), "miembro de la familia elegible: " + id);
        assertTrue(despues.containsKey(LATERAL_RAISE), "un movimiento distinto del mismo patron sigue disponible");
        var salen = new TreeSet<>(antes.keySet());
        salen.removeAll(despues.keySet());
        var esperadas = new TreeSet<>(familia);
        esperadas.retainAll(antes.keySet());
        assertEquals(esperadas, salen, "solo salen los miembros de la familia que eran elegibles: nada mas cambia");
        assertEquals(Set.of(FRONT_RAISE), dolor.excluidosActivosDirectos(u), "solo 537 tiene exclusion registrada");
        assertEquals(familia(FRONT_RAISE), dolor.excluidosActivos(u),
                "la exclusion efectiva es la familia entera (537, 538 y el duplicado historico inactivo 28), derivada");
    }

    @Test
    @Order(3)
    @DisplayName("Simetrico: con dolor en 538 (la variante), 537 tampoco es elegible")
    void simetrico() {
        long u = participante(false);
        registrarDolorDirecto(u, FRONT_RAISE_V2);
        var e = elegibles(u);
        assertFalse(e.containsKey(FRONT_RAISE) || e.containsKey(FRONT_RAISE_V2));
    }

    @Test
    @Order(4)
    @DisplayName("Una propuesta (IA o no) que trae la variante la rechaza V1, sin tocar el validador")
    void lavarianteNoSeCuela() {
        long u = participante(false);
        registrarDolorDirecto(u, FRONT_RAISE);
        var ctx = contexto(u);
        var borrador = new PlanDraft(GenerationSource.AI, null, "prueba", null, "r", List.of(new PlanDraft.DraftWorkout(
                S1, WorkoutFocus.FULL_BODY, "con la variante", 30, List.of(new PlanDraft.DraftExercise(FRONT_RAISE_V2,
                PrescriptionType.SETS_REPS, (short) 2, (short) 10, null, null, (short) 90, null)))));
        var e = assertThrows(InvalidPlanDraftException.class, () -> validador.validate(borrador, ctx, 30));
        assertTrue(e.problems().stream().anyMatch(p -> p.startsWith("V1") && p.contains(String.valueOf(FRONT_RAISE_V2))),
                e.problems().toString());
    }

    // ======================================================================================== ciclo real, caso O20

    @Test
    @Order(5)
    @DisplayName("O20 real: dolor en 537 (viernes) con 537 tambien el lunes: el ciclo no publica 537 ni 538 en ninguna sesion")
    void cicloO20SinRestricciones() {
        cicloO20(false);
    }

    @Test
    @Order(6)
    @DisplayName("O20 con restricciones personales (sin suelo, sin brazos por encima, sin impacto): el sustituto las respeta")
    void cicloO20ConRestricciones() {
        cicloO20(true);
    }

    private void cicloO20(boolean restringido) {
        long u = participante(restringido);
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var ids = sesionesDe(plan);
        assertTrue(ids.size() >= 3, "premisa: tres sesiones");
        // La forma de O20: 537 en la primera sesion (que se conservara) y en la ultima (donde dolera). Se coloca en el
        // ultimo puesto de cada una, con la dosis de relleno de O20 (2x10, 90 s).
        colocar(ids.get(0), FRONT_RAISE);
        colocar(ids.get(ids.size() - 1), FRONT_RAISE);
        var familia = familia(FRONT_RAISE);

        // Semana 1: todo hecho, salvo 537 en la ultima sesion, dejado por dolor.
        for (int i = 0; i < ids.size(); i++) hacer(u, ids.get(i), i == ids.size() - 1 ? FRONT_RAISE : -1);
        var semana2 = ciclo.runFor(u, S1, S1.plusWeeks(1));
        var intento = ultimo(u);
        System.out.println("O20_FAMILIA restringido=" + restringido + " publicado=" + semana2 + " intento=" + intento);
        assertTrue(activa(u, FRONT_RAISE), "la exclusion de 537 queda activa");
        comprobarSinFamilia(u, semana2, familia, intento, restringido);

        // Semana 2 (si hubo plan): todo hecho. Nada de la familia reaparece por continuidad, progresion o
        // reincorporacion mientras la exclusion siga activa.
        if (semana2.isPresent()) {
            for (var w : sesionesDe(semana2.get())) hacer(u, w, -1);
            var semana3 = ciclo.runFor(u, S1.plusWeeks(1), S1.plusWeeks(2));
            comprobarSinFamilia(u, semana3, familia, ultimo(u), restringido);
        }

        // Reactivacion EXPLICITA de lo que dolio: la familia vuelve a ser elegible (no antes).
        assertFalse(elegibles(u).containsKey(FRONT_RAISE_V2), "antes de reactivar, 538 sigue fuera");
        dolor.reactivar(u, FRONT_RAISE, true, "prueba: ya no hay molestias");
        var trasReactivar = elegibles(u);
        assertTrue(trasReactivar.containsKey(FRONT_RAISE) && trasReactivar.containsKey(FRONT_RAISE_V2),
                "con la exclusion reactivada por la persona, 537 y su variante vuelven a ser elegibles");
    }

    /**
     * El plan publicado (si lo hay) no contiene ningun miembro de la familia; cada sustitucion registrada pone un
     * ejercicio de OTRA familia, elegible y compatible con las restricciones. Si no hay plan, el intento queda
     * registrado con su causa (mecanismo seguro existente) y no se publico nada inseguro.
     */
    private void comprobarSinFamilia(long u, Optional<Long> plan, Set<Long> familia, Map<String, Object> intento,
                                     boolean restringido) {
        for (var p : jdbc.queryForList("SELECT plan_id FROM weekly_training_plans WHERE user_id = ?", Long.class, u)) {
            String estado = jdbc.queryForObject("SELECT status FROM weekly_training_plans WHERE plan_id = ?",
                    String.class, p);
            if (!"ACTIVE".equals(estado)) continue;
            var presentes = new HashSet<>(ejerciciosDelPlan(p));
            presentes.retainAll(familia);
            assertTrue(presentes.isEmpty(), "el plan activo " + p + " contiene la familia de lo que dolio: " + presentes);
        }
        String informe = String.valueOf(intento.get("conservation_report"));
        var puestos = new ArrayList<Long>();
        var m = java.util.regex.Pattern.compile("\"puesto\":\\s*(\\d+)").matcher(informe);
        while (m.find()) puestos.add(Long.parseLong(m.group(1)));
        for (var puesto : puestos) {
            assertFalse(familia.contains(puesto), "SUST-SEG-1 puso una variante equivalente: " + puesto);
            if (restringido) {
                var f = jdbc.queryForMap("SELECT requires_floor, requires_overhead, high_impact FROM exercises"
                        + " WHERE exercise_id = ?", puesto);
                assertEquals(Boolean.FALSE, f.get("requires_floor"), "sustituto " + puesto + " exige suelo");
                assertEquals(Boolean.FALSE, f.get("requires_overhead"), "sustituto " + puesto + " exige brazos arriba");
                assertEquals(Boolean.FALSE, f.get("high_impact"), "sustituto " + puesto + " es de alto impacto");
            }
        }
        if (plan.isEmpty()) {
            assertNotNull(intento.get("status"), "sin plan, el intento queda registrado");
            assertTrue(Set.of("NO_FACTIBLE", "SIN_EFECTO", "SIN_ORDEN", "APLICADA").contains(
                    String.valueOf(intento.get("status"))), "estado registrado: " + intento);
        }
    }

    // ================================================================= diferencias de nombre, postura y material

    @Test
    @Order(8)
    @DisplayName("Nombre, postura y material segun el backend: confirmadas las del mismo gesto; el angulo, precaucion; el sinonimo, nada")
    void diferenciasDeNombrePosturaYMaterial() {
        record Par(String a, String b, String nivel) {}
        var pares = List.of(
                new Par("dumbbell front raise", "dumbbell seated front raise", "MISMO_GESTO"),      // postura
                new Par("dumbbell front raise", "band front raise", "MISMO_GESTO"),                 // material
                new Par("dumbbell front raise", "dumbbell front raise v. 2", "VARIANTE_MISMO_EJERCICIO"),
                new Par("dumbbell biceps curl", "lever bicep curl", "MISMO_GESTO"),                 // nombre (ortografia)
                new Par("barbell upright row", "cable upright row", "MISMO_GESTO"),                 // dos patrones
                new Par("dumbbell bent over row", "barbell one arm bent over row", "MISMO_GESTO"),  // lateralidad
                new Par("dumbbell front raise", "dumbbell lateral raise", null),                    // mismo musculo
                new Par("dumbbell front raise", "dumbbell full can lateral raise", null),
                new Par("lever chest press", "lever incline chest press v. 2", "PRECAUCION_OTRO_ANGULO"), // angulo
                new Par("barbell bench press", "lever chest press", null),                          // sinonimo dudoso
                new Par("barbell hack squat", "smith hack squat", null));                           // revisado: distinto
        for (var p : pares) {
            long a = idActivo(p.a()), b = idActivo(p.b());
            var eq = catalogo.fetchEquivalentVariants(Set.of(a)).getOrDefault(a, List.of()).stream()
                    .filter(e -> e.exerciseId() == b).findFirst();
            assertEquals(p.nivel(), eq.map(e -> e.level()).orElse(null), p.a() + " / " + p.b());
            var vuelta = catalogo.fetchEquivalentVariants(Set.of(b)).getOrDefault(b, List.of()).stream()
                    .anyMatch(e -> e.exerciseId() == a);
            assertEquals(p.nivel() != null, vuelta, "simetrico: " + p.b() + " / " + p.a());
        }
    }

    // ================================================================= dolor de sesion sin identificar (ciclo real)

    @Test
    @Order(9)
    @DisplayName("Dolor de sesion sin identificar: excluye la sesion y, provisionalmente, sus familias; nada se reactiva solo")
    void dolorDeSesionSinIdentificar() {
        long u = participante(false);
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var ids = sesionesDe(plan);
        colocar(ids.get(0), FRONT_RAISE);
        colocar(ids.get(ids.size() - 1), FRONT_RAISE);
        var familia = familia(FRONT_RAISE);
        var ultima = ids.get(ids.size() - 1);
        var deLaSesion = new TreeSet<>(jdbc.queryForList("SELECT exercise_id FROM planned_workout_exercises WHERE"
                + " planned_workout_id = ?", Long.class, ultima));

        // Semana 1: todo hecho salvo la ultima sesion, OMITIDA por dolor sin decir que ejercicio.
        for (int i = 0; i < ids.size() - 1; i++) hacer(u, ids.get(i), -1);
        planComandos.handle(new main.web.services.fitsense.planning.domain.model.commands.SkipPlannedWorkoutCommand(u,
                ultima, SkipReason.PAIN_OR_DISCOMFORT));
        assertEquals(deLaSesion, dolor.excluidosActivosDirectos(u), "se excluyen todos los de la sesion (regla V8C)");
        var efectivas = porEjercicio(u);
        for (var id : deLaSesion) {
            assertEquals(PainMemoryService.Origen.DIRECTA, efectivas.get(id).origen());
            assertFalse(efectivas.get(id).confirmada(), "un reporte general no confirma cual dolio");
            assertEquals(ultima, efectivas.get(id).sourcePlannedWorkoutId(), "queda la sesion para identificarlo");
        }
        assertTrue(efectivas.get(FRONT_RAISE_V2).provisional(), "la variante de 537 sale, de forma provisional");

        // Adaptacion inmediata (ciclo real): ni la sesion ni las familias de sus ejercicios vuelven.
        var semana2 = ciclo.runFor(u, S1, S1.plusWeeks(1));
        var intento = ultimo(u);
        System.out.println("O20_SESION publicado=" + semana2 + " intento=" + intento);
        var excluidosAhora = dolor.excluidosActivos(u);
        assertTrue(excluidosAhora.containsAll(familia), "familia de 537 excluida en la regeneracion inmediata");
        comprobarSinFamilia(u, semana2, excluidosAhora, intento, false);
        long planS2 = semana2.orElseGet(() ->
                planning.generateWeeklyPlan(u, S1.plusWeeks(1), PlanAdjustment.none(), null, null).orElseThrow());

        // Semana siguiente: aun provisional (una sola semana con plan despues del dolor).
        assertTrue(dolor.excluidosActivos(u).containsAll(familia));
        long planS3 = planning.generateWeeklyPlan(u, S1.plusWeeks(2), PlanAdjustment.none(), null, null).orElseThrow();
        var enS3 = new HashSet<>(ejerciciosDelPlan(planS3));
        enS3.retainAll(familia);
        assertTrue(enS3.isEmpty(), "la semana siguiente tampoco trae la familia: " + enS3);

        // Cierre final: con dos semanas con plan despues del dolor NO se levanta nada (ni la sesion ni sus familias):
        // se pide una nueva confirmacion. El vencimiento no reactiva en silencio.
        var tras = elegibles(u);
        assertFalse(tras.containsKey(FRONT_RAISE_V2), "538 sigue fuera: el vencimiento no lo reactiva");
        for (var id : deLaSesion) assertFalse(tras.containsKey(id), "sigue excluido sin confirmacion: " + id);
        assertEquals(deLaSesion, dolor.excluidosActivosDirectos(u));
        var pendientes = porEjercicio(u);
        for (var id : deLaSesion) assertTrue(pendientes.get(id).requiereConfirmacion(), "se pide confirmacion: " + id);
        assertTrue(pendientes.get(FRONT_RAISE_V2).requiereConfirmacion() && pendientes.get(FRONT_RAISE_V2).provisional());

        // La persona identifica despues que fue 537 y confirma que UNO de los demas no tuvo que ver; otro no lo nombra.
        var otros = new TreeSet<>(deLaSesion);
        otros.remove(FRONT_RAISE);
        otros.removeAll(familia);
        assertTrue(otros.size() >= 2, "premisa: al menos dos ejercicios mas en la sesion: " + otros);
        long noRelacionado = otros.first(), sinNombrar = otros.last();
        var r = dolor.identificar(u, FRONT_RAISE, ultima, true, Set.of(noRelacionado));
        System.out.println("O20_IDENTIFICACION " + r);
        assertTrue(r.equivalentesExcluidos().contains(FRONT_RAISE_V2), r.toString());
        assertEquals(List.of(noRelacionado), r.noRelacionadosLevantados(), r.toString());
        assertFalse(dolor.excluidosActivosDirectos(u).contains(noRelacionado), "levantado por confirmacion explicita");
        assertTrue(dolor.excluidosActivosDirectos(u).contains(sinNombrar), "sin confirmacion: sigue excluido");
        assertTrue(porEjercicio(u).get(sinNombrar).requiereConfirmacion());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM user_safety_exclusions WHERE user_id = ? AND"
                + " exercise_id = ? AND status = 'REACTIVADA' AND reactivation_evidence LIKE"
                + " 'CONFIRMACION_EXPLICITA_NO_RELACIONADO%'", Integer.class, u, noRelacionado), "con constancia");
        if (!dolor.excluidosActivos(u).contains(noRelacionado))
            assertTrue(elegibles(u).containsKey(noRelacionado), "el no relacionado vuelve a ser candidato");
        var identificada = elegibles(u);
        for (var id : familia) assertFalse(identificada.containsKey(id), "confirmado: familia fuera " + id);
        assertTrue(porEjercicio(u).get(FRONT_RAISE).confirmada());
        assertFalse(porEjercicio(u).get(FRONT_RAISE_V2).provisional(), "la familia del confirmado es permanente");
        // Cuatro semanas despues sigue igual: lo confirmado no caduca.
        planning.generateWeeklyPlan(u, S1.plusWeeks(3), PlanAdjustment.none(), null, null).orElseThrow();
        assertTrue(dolor.excluidosActivos(u).containsAll(familia));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM user_safety_exclusions WHERE user_id = ? AND"
                + " exercise_id = ? AND identified_at IS NOT NULL AND identification_evidence IS NOT NULL AND reason ="
                + " 'DOLOR_SESION_SIN_IDENTIFICAR'", Integer.class, u, FRONT_RAISE), "el motivo original se conserva");

        // Solo la reactivacion explicita y autorizada devuelve lo confirmado y su familia.
        dolor.reactivar(u, FRONT_RAISE, true, "prueba: ya no hay molestias");
        var reactivada = elegibles(u);
        assertTrue(reactivada.containsKey(FRONT_RAISE) && reactivada.containsKey(FRONT_RAISE_V2));
        assertFalse(reactivada.containsKey(sinNombrar), "el no nombrado sigue fuera: " + sinNombrar);
        assertTrue(planS2 > 0);
    }

    @Test
    @Order(10)
    @DisplayName("Cambio de angulo tras dolor: lever chest press no se da por seguro tras dolor en lever incline chest press")
    void cambioDeAngulo() {
        long inclinado = idActivo("lever incline chest press v. 2"), plano = idActivo("lever chest press");
        var relacion = catalogo.fetchEquivalentVariants(Set.of(inclinado)).get(inclinado).stream()
                .filter(e -> e.exerciseId() == plano).findFirst().orElseThrow();
        assertEquals(FamiliaDeMovimientoEquivalente.NIVEL_PRECAUCION_ANGULO, relacion.level(), "no es equivalencia");
        long u = participante(false);
        registrarDolorDirecto(u, inclinado);
        var e = porEjercicio(u);
        assertEquals(PainMemoryService.Origen.PRECAUCION, e.get(plano).origen());
        assertTrue(dolor.excluidosActivos(u).contains(plano), "el plano no se prescribe ni sustituye mientras dure");
        dolor.reactivar(u, inclinado, true, "prueba");
        assertFalse(dolor.excluidosActivos(u).contains(plano), "vuelve solo con la reactivacion de lo que dolio");
    }

    // ===================================================================================================== material

    private long participante(boolean restringido) {
        String email = "dolor-familia-" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        // El perfil de O20 (cohorte O): INCREASE_STRENGTH, intermedio, casa con mancuerna y banda, L-X-V, 45 min.
        // Restringido: sin alto impacto, sin suelo, sin brazos por encima de la cabeza (las demas permitidas).
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 12, 1), BiologicalSex.MALE,
                new BigDecimal("178"), new BigDecimal("78"), null, FitnessLevel.INTERMEDIATE, GoalType.INCREASE_STRENGTH,
                "dolor-familia", TrainingLocation.HOME, (short) 3, List.of((short) 1, (short) 3, (short) 5), (short) 45,
                CASA, List.of(), restringido ? "Restricciones declaradas" : "Ninguna", !restringido, !restringido, true,
                true, !restringido));
        return u;
    }

    private Map<Long, CandidateExercise> elegibles(long u) {
        return contexto(u).availableExercises().stream()
                .collect(Collectors.toMap(CandidateExercise::exerciseId, c -> c, (a, b) -> a));
    }

    /** La preparacion real de la semana S1 (sustituyendo el plan si ya lo hay): el conjunto elegible de hoy. */
    private main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext contexto(long u) {
        return planComandos.prepare(new GenerateWeeklyPlanCommand(u, S1, null, true, null, null)).context();
    }

    /**
     * La fila que escribe el ciclo semanal al registrar dolor (P-02), sin pasar por una semana entera. La exclusion
     * cita su sesion de origen (NOT NULL en V43): la primera del plan del participante, que se genera si no lo tiene.
     */
    private void registrarDolorDirecto(long u, long ejercicio) {
        var planes = jdbc.queryForList("SELECT plan_id FROM weekly_training_plans WHERE user_id = ? ORDER BY plan_id",
                Long.class, u);
        long plan = planes.isEmpty() ? planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null)
                .orElseThrow() : planes.get(0);
        long sesion = sesionesDe(plan).get(0);
        jdbc.update("INSERT INTO user_safety_exclusions (user_id, exercise_id, reason, source_plan_id,"
                + " source_planned_workout_id, event_date, recorded_by) VALUES (?, ?, 'DOLOR_IDENTIFICADO', ?, ?, ?,"
                + " 'CICLO_SEMANAL')", u, ejercicio, plan, sesion, S1);
    }

    private void colocar(long plannedWorkoutId, long ejercicio) {
        long ultimo = jdbc.queryForObject("SELECT planned_exercise_id FROM planned_workout_exercises WHERE"
                + " planned_workout_id = ? ORDER BY exercise_order DESC LIMIT 1", Long.class, plannedWorkoutId);
        jdbc.update("UPDATE planned_workout_exercises SET exercise_id = ?, prescription_type = 'SETS_REPS',"
                + " planned_sets = 2, planned_reps = 10, planned_duration_seconds = NULL, target_load_kg = NULL,"
                + " rest_seconds = 90 WHERE planned_exercise_id = ?", ejercicio, ultimo);
    }

    private List<Long> sesionesDe(long plan) {
        return jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ? ORDER BY"
                + " scheduled_date", Long.class, plan);
    }

    private List<Long> ejerciciosDelPlan(long plan) {
        return jdbc.queryForList("SELECT pwe.exercise_id FROM planned_workout_exercises pwe JOIN planned_workouts pw"
                + " ON pw.planned_workout_id = pwe.planned_workout_id WHERE pw.plan_id = ?", Long.class, plan);
    }

    private void hacer(long u, long w, long conDolor) {
        RelojPruebas.abrirVentanaDeEjecucion(jdbc, u);
        long s = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
        for (var f : jdbc.queryForList("SELECT pwe.planned_exercise_id, pwe.exercise_id, pwe.prescription_type,"
                + " pwe.planned_sets, pwe.planned_reps, pwe.planned_duration_seconds FROM planned_workout_exercises pwe"
                + " WHERE pwe.planned_workout_id = ? ORDER BY pwe.exercise_order", w)) {
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

    private Map<String, Object> ultimo(long u) {
        var l = jdbc.queryForList("SELECT attempt_id, decision, status, generation_source, ai_calls,"
                + " conservation_report::text AS conservation_report, rejection_reason FROM plan_adaptation_attempts"
                + " WHERE user_id = ? ORDER BY attempt_id DESC LIMIT 1", u);
        return l.isEmpty() ? Map.of() : l.get(0);
    }

    private boolean activa(long u, long ejercicio) {
        return jdbc.queryForObject("SELECT count(*) FROM user_safety_exclusions WHERE user_id = ? AND exercise_id = ?"
                + " AND status = 'ACTIVA'", Integer.class, u, ejercicio) > 0;
    }

    private long idActivo(String nombreEn) {
        return jdbc.queryForObject("SELECT min(exercise_id) FROM exercises WHERE LOWER(name_en) = ? AND is_active",
                Long.class, nombreEn);
    }

    private Map<Long, PainMemoryService.ExclusionEfectiva> porEjercicio(long u) {
        return dolor.exclusionesEfectivas(u).stream()
                .collect(Collectors.toMap(PainMemoryService.ExclusionEfectiva::exerciseId, x -> x));
    }

    private Map<Long, String> nombresEn(Set<Long> ids) {
        var r = new HashMap<Long, String>();
        for (var id : ids)
            r.put(id, jdbc.queryForObject("SELECT name_en FROM exercises WHERE exercise_id = ?", String.class, id));
        return r;
    }

    /**
     * La familia calculada recorriendo TODO el catalogo con la regla del dominio (nivel 1 por nombre y nivel 2 por la
     * lista explicita), sin usar la consulta
     * del backend: es la referencia independiente con la que se comprueba lo publicado.
     */
    private Set<Long> familia(long ejercicio) {
        var o = jdbc.queryForMap("SELECT name_en, movement_pattern, target_muscle FROM exercises WHERE exercise_id = ?",
                ejercicio);
        var r = new TreeSet<Long>();
        r.add(ejercicio);
        for (var f : jdbc.queryForList("SELECT exercise_id, name_en FROM exercises"))
            if (FamiliaDeMovimientoEquivalente.equivalentes((String) o.get("name_en"), (String) f.get("name_en")))
                r.add(((Number) f.get("exercise_id")).longValue());
        return r;
    }

    /** La familia tal como la calcula el backend (consulta del catalogo), con el propio ejercicio. */
    private Set<Long> familiaSegunBackend(long ejercicio) {
        var r = new TreeSet<Long>();
        catalogo.fetchEquivalentVariants(Set.of(ejercicio)).getOrDefault(ejercicio, List.of()).stream()
                .filter(e -> !FamiliaDeMovimientoEquivalente.NIVEL_PRECAUCION_ANGULO.equals(e.level()))
                .forEach(e -> r.add(e.exerciseId()));
        r.add(ejercicio);
        return r;
    }

    private Map<String, List<Long>> familiasActivas() {
        var porClave = new TreeMap<String, List<Long>>();
        for (var f : jdbc.queryForList("SELECT exercise_id, name_en FROM exercises WHERE is_active"))
            porClave.computeIfAbsent(FamiliaDeMovimientoEquivalente.clave((String) f.get("name_en")),
                    k -> new ArrayList<>()).add(((Number) f.get("exercise_id")).longValue());
        porClave.values().removeIf(l -> l.size() < 2);
        return porClave;
    }
}
