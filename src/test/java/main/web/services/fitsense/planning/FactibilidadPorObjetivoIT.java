package main.web.services.fitsense.planning;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.application.internal.commandservices.GoalFeasibilityObserver;
import main.web.services.fitsense.planning.application.internal.commandservices.WeeklyTrainingPlanCommandServiceImpl;
import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalCatalogService;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.model.valueobjects.GoalCapabilityPolicy.Capability;
import main.web.services.fitsense.planning.domain.services.GoalFeasibilityPolicy;
import main.web.services.fitsense.planning.domain.services.GoalFeasibilityPolicy.*;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.infrastructure.generation.rules.RuleBasedTrainingPlanGenerator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static main.web.services.fitsense.planning.CatalogoElegibleIT.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Paso 3 con el CATALOGO REAL (PostgreSQL efimero, V1..V33): para cada perfil
 * de la matriz de {@link CatalogoElegibleIT}, que capacidades principales exige
 * el objetivo, cuantos candidatos reales hay por ruta, cuales faltan y el
 * veredicto del pool; y, sobre la semana del motor de reglas, si la semana
 * contiene cada principal. Nada aqui da un plan por aprobado: la dosis es 4D.
 * <p>
 * Escribe {@code target/paso3/factibilidad.csv}.
 */
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
class FactibilidadPorObjetivoIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired ExternalCatalogService catalogo;
    @Autowired GoalFeasibilityObserver observador;
    @Autowired WeeklyTrainingPlanCommandServiceImpl servicio;
    @Autowired NamedParameterJdbcTemplate jdbc;

    static final List<String> FILAS = new ArrayList<>();

    private static PlanningProfile perfil(String objetivo, Entorno entorno, String nivel, int edad,
                                          Restricciones restr, List<Long> bloqueados) {
        return new PlanningProfile(99L, edad, "FEMALE", new BigDecimal("165"), new BigDecimal("70"), null,
                nivel, objetivo, "prueba", "Ninguna", entorno.lugar(), 3,
                List.of((short) 1, (short) 3, (short) 5), 45, entorno.equipamiento(), bloqueados,
                restr.declaradas());
    }

    private PlanGenerationContext contexto(PlanningProfile p) {
        var seguridad = SafetyProfile.forAgeLevelAndDeclared(p.age(), p.fitnessLevel(), p.declaradas());
        var elegibles = catalogo.fetchEligibleFor(p, p.maxDifficultyLevel(), seguridad);
        var lunes = LocalDate.of(2026, 10, 26);
        return new PlanGenerationContext(p.userId(), (short) 1, lunes, lunes.plusDays(6), p,
                PlanAdjustment.none(), PreviousWeekSummary.empty(), elegibles, seguridad,
                (PrescriptionParams) null);
    }

    record Resultado(EvaluacionPool pool, EvaluacionSemana semana) {}

    private Resultado evaluar(String objetivo, Entorno entorno, String nivel, int edad, Restricciones restr,
                              List<Long> bloqueados) {
        var ctx = contexto(perfil(objetivo, entorno, nivel, edad, restr, bloqueados));
        var pool = GoalFeasibilityPolicy.evaluarPool(objetivo, nivel, ctx.availableExercises()).orElseThrow();

        // Invariantes del veredicto, sobre datos reales.
        var peor = pool.principales().stream().map(CapacidadEvaluada::veredicto)
                .min(Comparator.naturalOrder()).orElseThrow();
        assertEquals(peor, pool.veredicto(), "el veredicto es el de la peor principal");
        if ("LOSE_WEIGHT".equals(objetivo))
            assertTrue(pool.principales().stream().flatMap(c -> c.rutas().stream())
                    .noneMatch(r -> r.ruta() == Ruta.SLOTS_RESISTIDOS), "LOSE_WEIGHT no exige slots");
        pool.principales().stream().filter(c -> c.capacidad() == Capability.CARDIO)
                .forEach(c -> assertEquals(Veredicto.NO_VERIFICABLE, c.rutas().get(1).veredicto()));

        // Nivel B sobre la semana del motor de reglas.
        var borrador = new RuleBasedTrainingPlanGenerator(30, new SessionDurationEstimator())
                .generate(ctx, List.of());
        var semana = GoalFeasibilityPolicy.evaluarSemana(objetivo, nivel, borrador, ctx.availableExercises())
                .orElseThrow();
        return new Resultado(pool, semana);
    }

    @Test
    void censoDeFactibilidadPorObjetivo() {
        // clave sin objetivo -> veredicto por objetivo
        var porPerfil = new LinkedHashMap<String, Map<String, Veredicto>>();
        for (var entorno : ENTORNOS)
            for (var nivel : NIVELES)
                for (var edad : EDADES)
                    for (var restr : RESTRICCIONES)
                        for (var objetivo : OBJETIVOS) {
                            var r = evaluar(objetivo, entorno, nivel, edad, restr, List.of());
                            var clave = String.join("/", entorno.nombre(), nivel, String.valueOf(edad), restr.nombre());
                            porPerfil.computeIfAbsent(clave, k -> new LinkedHashMap<>()).put(objetivo, r.pool().veredicto());
                            fila(objetivo, entorno.nombre(), nivel, edad, restr.nombre(), "", r);
                        }
        assertEquals(108, porPerfil.size());

        // Combinaciones de restricciones con tres veredictos distintos segun el objetivo.
        var tresVeredictos = porPerfil.entrySet().stream()
                .filter(e -> e.getValue().containsValue(Veredicto.ROBUSTO)
                        && e.getValue().containsValue(Veredicto.FRAGIL)
                        && (e.getValue().containsValue(Veredicto.NO_FACTIBLE)
                        || e.getValue().containsValue(Veredicto.NO_VERIFICABLE)))
                .map(e -> e.getKey() + " " + e.getValue()).toList();
        System.out.println("PASO3_TRES_VEREDICTOS " + tresVeredictos.size() + " " + tresVeredictos);
    }

    /**
     * El caso del informe: principiante en casa, solo peso corporal, sin suelo
     * ni brazos arriba, con 776 bloqueado. Si 776 era la unica cadera, el slot
     * queda a cero: no factible para quien exige los slots, con la causa dicha,
     * y sin afectar a LOSE_WEIGHT, que no los exige.
     */
    @Test
    void bloquearLaUltimaCaderaSeDetectaYSeExplica() {
        var entorno = ENTORNOS.get(0);
        var restr = RESTRICCIONES.get(1);
        var sinBloqueo = evaluar("GAIN_MUSCLE", entorno, "BEGINNER", 30, restr, List.of());
        var cadera = sinBloqueo.pool().principales().get(0).rutas().get(0).porSlot().get(MandatorySlot.LOWER_HIP);
        var conBloqueo = evaluar("GAIN_MUSCLE", entorno, "BEGINNER", 30, restr, List.of(776L));
        fila("GAIN_MUSCLE", entorno.nombre(), "BEGINNER", 30, restr.nombre(), "776", conBloqueo);

        if (cadera == 1) {
            assertEquals(Veredicto.NO_FACTIBLE, conBloqueo.pool().veredicto());
            assertTrue(conBloqueo.pool().motivos().toString().contains("LOWER_HIP"), conBloqueo.pool().motivos().toString());
            var ctx = contexto(perfil("GAIN_MUSCLE", entorno, "BEGINNER", 30, restr, List.of(776L)));
            var causas = observador.causasDeEscasez(ctx, conBloqueo.pool());
            assertTrue(causas.stream().anyMatch(c -> c.contains("bloqueos_individuales")), causas.toString());
            System.out.println("PASO3_CASO_776 causas=" + causas);
        } else {
            System.out.println("PASO3_CASO_776 LOWER_HIP sin bloqueo=" + cadera + ": 776 no es la unica cadera");
        }
        var perder = evaluar("LOSE_WEIGHT", entorno, "BEGINNER", 30, restr, List.of(776L));
        fila("LOSE_WEIGHT", entorno.nombre(), "BEGINNER", 30, restr.nombre(), "776", perder);
        assertTrue(perder.pool().motivos().stream().noneMatch(m -> m.contains("LOWER_HIP")));
    }

    /** El observador esta conectado a generate() y escribe sus cuatro lineas. */
    @Test
    void elObservadorEstaConectadoYRegistra(CapturedOutput salida) {
        assertSame(observador, ReflectionTestUtils.getField(servicio, "feasibilityObserver"));
        var ctx = contexto(perfil("LOSE_WEIGHT", ENTORNOS.get(0), "BEGINNER", 72, RESTRICCIONES.get(2), List.of()));
        observador.antesDeGenerar(ctx);
        observador.despuesDeGenerar(ctx, new RuleBasedTrainingPlanGenerator(30, new SessionDurationEstimator())
                .generate(ctx, List.of()));
        var log = salida.getAll();
        assertTrue(log.contains("FACTIBILIDAD_POOL user=99"), log);
        assertTrue(log.contains("DIVERGENCIA_V23 user=99 objetivo=LOSE_WEIGHT"), log);
        assertTrue(log.contains("COBERTURA_SEMANAL user=99"), log);
        if (!log.contains("veredicto=ROBUSTO"))
            assertTrue(log.contains("ESCASEZ user=99"), log);
    }

    private static void fila(String objetivo, String entorno, String nivel, int edad, String restr,
                             String bloqueos, Resultado r) {
        var principales = r.pool().principales().stream()
                .map(c -> c.capacidad() + ":" + c.veredicto() + c.rutas().stream()
                        .filter(x -> x.ruta() != Ruta.CIRCUITO)
                        .map(x -> x.ruta() == Ruta.SLOTS_RESISTIDOS ? MandatorySlot.ALL.stream()
                                .map(sl -> sl + "=" + x.porSlot().get(sl)).collect(Collectors.joining(","))
                                : x.verificados() + (x.enRevision() > 0 ? "+" + x.enRevision() + "rev" : ""))
                        .collect(Collectors.joining("", "(", ")")))
                .collect(Collectors.joining(" "));
        FILAS.add(String.join(";", objetivo, entorno, nivel, String.valueOf(edad), restr, bloqueos,
                principales, String.join(" | ", r.pool().motivos()), r.pool().veredicto().name(),
                r.semana().veredicto().name(),
                r.semana().principales().stream().map(PresenciaCapacidad::describe).collect(Collectors.joining(" "))));
    }

    @AfterAll
    static void escribir() throws IOException {
        var dir = Path.of("target", "paso3");
        Files.createDirectories(dir);
        var lineas = new ArrayList<String>();
        lineas.add("objetivo;entorno;nivel;edad;restricciones;bloqueos;principales(verificados+en_revision);"
                + "motivos;veredicto_pool;veredicto_semana_motor_reglas;presencia_semana");
        lineas.addAll(FILAS);
        Files.write(dir.resolve("factibilidad.csv"), lineas);
    }
}
