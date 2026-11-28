package main.web.services.fitsense.catalogo;

import main.web.services.fitsense.catalog.domain.services.ExerciseQueryService;
import main.web.services.fitsense.catalog.infrastructure.persistence.jpa.repositories.BodyPartRepository;
import main.web.services.fitsense.catalog.infrastructure.persistence.jpa.repositories.EquipmentTypeRepository;
import main.web.services.fitsense.catalog.interfaces.acl.CatalogContextFacade;
import main.web.services.fitsense.catalog.interfaces.acl.EligibleExerciseView;
import main.web.services.fitsense.planning.domain.model.valueobjects.ExerciseSafetyFacts;
import main.web.services.fitsense.planning.domain.model.valueobjects.SafetyProfile;
import main.web.services.fitsense.planning.domain.services.SafetyConflictPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V53 · apoyo suspendido. La migracion solo anade el DATO; el COMPORTAMIENTO queda apagado hasta la aprobacion del
 * propietario ("No actives el cambio hasta nuestra aprobacion del comportamiento final").
 * <ul>
 *   <li>Apagado (por defecto): «no colgarse» significa exactamente lo mismo que antes de V53. Ni la elegibilidad ni los
 *       hechos de seguridad que leen los validadores cambian.</li>
 *   <li>Encendido ({@code fitsense.seguridad.apoyo-suspendido-en-no-colgarse=true}): «no colgarse» excluye ademas los
 *       15 ejercicios con apoyo suspendido sobre un aparato sin soporte de pies, y los validadores lo informan con la
 *       causa propia {@code apoyo_suspendido}. Sin la restriccion, nada cambia.</li>
 * </ul>
 * El encendido se prueba con una segunda instancia de la fachada sobre los mismos beans: el contexto de Spring de la
 * prueba conserva el valor por defecto.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("V53 · el apoyo suspendido queda tras un interruptor apagado por defecto")
class ApoyoSuspendidoInterruptorIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    /** Los 15 de V53 (1301 no cumple el criterio conservador: manos en el suelo y pies en correas). */
    static final Set<Long> APOYO = Set.of(457L, 458L, 459L, 462L, 777L, 920L, 934L, 1069L, 1153L, 1184L, 1303L,
            1304L, 1306L, 1317L, 1335L);

    static final List<String> TODO = List.of("dumbbell", "band", "kettlebell", "stability ball", "medicine ball",
            "rope", "barbell", "ez barbell", "olympic barbell", "cable", "leverage machine", "smith machine",
            "sled machine", "weighted", "assisted", "other", "bench", "pull-up bar", "suspension straps",
            "parallel bars", "gymnastic rings", "bosu ball", "balance board", "ab wheel", "step platform", "plyo box",
            "stationary bike", "treadmill", "landmine", "captains chair", "body weight");

    @Autowired CatalogContextFacade porDefecto;
    @Autowired ExerciseQueryService consultas;
    @Autowired BodyPartRepository zonas;
    @Autowired EquipmentTypeRepository equipos;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate tx;

    private CatalogContextFacade encendida() {
        return new CatalogContextFacade(consultas, zonas, equipos, true);
    }

    private Set<Long> elegibles(CatalogContextFacade f, boolean sinColgarse) {
        return tx.execute(s -> f.fetchEligibleExercises(TODO, false, 3, List.of(), false, false, false, false,
                sinColgarse, false, false)).stream().map(EligibleExerciseView::exerciseId)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Test
    @DisplayName("el dato: exactamente los 15 ejercicios tienen requires_suspended_support (sin 1301)")
    void elDato() {
        var marcados = new TreeSet<>(jdbc.queryForList(
                "SELECT exercise_id FROM exercises WHERE requires_suspended_support", Long.class));
        assertEquals(new TreeSet<>(APOYO), marcados);
        assertFalse(marcados.contains(1301L));
    }

    @Test
    @DisplayName("apagado: «no colgarse» no excluye el apoyo suspendido y los validadores no lo ven")
    void apagadoNoCambiaElSignificado() {
        var sinColgarse = elegibles(porDefecto, true);
        var colgantes = new TreeSet<>(jdbc.queryForList(
                "SELECT exercise_id FROM exercises WHERE is_active AND requires_hanging", Long.class));
        var todos = elegibles(porDefecto, false);
        // Apagado, «no colgarse» quita exactamente lo que cuelga (requires_hanging), como antes de V53.
        var esperado = new TreeSet<>(todos);
        esperado.removeAll(colgantes);
        assertEquals(esperado, sinColgarse);
        // Los activos de V53 que no cuelgan siguen elegibles con «no colgarse».
        var activos = activosDeApoyo();
        activos.removeAll(colgantes);
        assertFalse(activos.isEmpty());
        assertTrue(sinColgarse.containsAll(activos), "apagado no debe excluir: " + activos);

        var flags = tx.execute(s -> porDefecto.fetchSafetyFlags(APOYO));
        flags.values().forEach(v -> assertFalse(v.requiresSuspendedSupport(), "apagado: " + v.exerciseId()));
    }

    @Test
    @DisplayName("encendido: «no colgarse» excluye ademas los 15; sin la restriccion no cambia nada")
    void encendidoExcluyeSoloConLaRestriccion() {
        var f = encendida();
        assertEquals(elegibles(porDefecto, false), elegibles(f, false), "sin «no colgarse» el encendido no cambia nada");

        var antes = elegibles(porDefecto, true);
        var despues = elegibles(f, true);
        var quitados = new TreeSet<>(antes);
        quitados.removeAll(despues);
        assertTrue(antes.containsAll(despues), "el encendido no anade nada");
        assertTrue(APOYO.containsAll(quitados), "solo salen ejercicios de V53: " + quitados);
        var activos = activosDeApoyo();
        activos.retainAll(antes);
        assertEquals(activos, quitados, "salen todos los de V53 que eran elegibles");

        var flags = tx.execute(s -> f.fetchSafetyFlags(APOYO));
        assertEquals(APOYO, flags.keySet());
        flags.values().forEach(v -> assertTrue(v.requiresSuspendedSupport(), "encendido: " + v.exerciseId()));
    }

    @Test
    @DisplayName("validador: la causa apoyo_suspendido solo aparece encendido y con «no colgarse»")
    void causaDelValidador() {
        var sinColgarse = SafetyProfile.forAgeLevelAndDeclared(30, "ADVANCED",
                new SafetyProfile.Declaradas(true, true, true, false, true));
        var libre = SafetyProfile.forAgeLevelAndDeclared(30, "ADVANCED", SafetyProfile.Declaradas.sinLimitaciones());
        var apagado = hechos(porDefecto, 1317L);
        var encendido = hechos(encendida(), 1317L);
        assertFalse(SafetyConflictPolicy.causas(apagado, sinColgarse, false, false)
                .contains(SafetyConflictPolicy.APOYO_SUSPENDIDO));
        assertTrue(SafetyConflictPolicy.causas(encendido, sinColgarse, false, false)
                .contains(SafetyConflictPolicy.APOYO_SUSPENDIDO));
        assertTrue(SafetyConflictPolicy.causas(encendido, libre, false, false).isEmpty());
    }

    @Test
    @DisplayName("el script de impacto sobre permisos guardados corre sobre el esquema V1-V53 y es de solo lectura")
    void scriptDeImpactoDeSoloLectura() throws Exception {
        var sql = java.nio.file.Files.readString(java.nio.file.Path.of(
                "docs/FitSense/catalogo_integral/cierre/v53/impacto_permisos_v53_solo_lectura.sql"));
        var sentencias = Arrays.stream(sql.replaceAll("(?m)^\\s*--.*$", "").split(";"))
                .map(String::strip).filter(s -> !s.isEmpty()).toList();
        assertEquals("BEGIN TRANSACTION READ ONLY", sentencias.get(0));
        assertEquals("ROLLBACK", sentencias.get(sentencias.size() - 1));
        var consultas = sentencias.subList(1, sentencias.size() - 1);
        assertEquals(2, consultas.size());
        for (var c : consultas) {
            var u = c.toUpperCase(Locale.ROOT);
            assertTrue(u.startsWith("WITH") || u.startsWith("SELECT"), c);
            for (var w : List.of("INSERT ", "UPDATE ", "DELETE ", "ALTER ", "DROP ", "TRUNCATE ", "CREATE "))
                assertFalse(u.contains(w), "escribe: " + w);
        }
        // Dentro de una transaccion READ ONLY, como el propietario lo ejecutaria: cualquier escritura fallaria.
        tx.execute(s -> {
            jdbc.execute("SET TRANSACTION READ ONLY");
            var resumen = jdbc.queryForList(consultas.get(0));
            assertEquals(4, resumen.size());
            jdbc.queryForList(consultas.get(1));
            return null;
        });
    }

    private ExerciseSafetyFacts hechos(CatalogContextFacade f, long id) {
        var v = tx.execute(s -> f.fetchSafetyFlags(Set.of(id))).get(id);
        return new ExerciseSafetyFacts(v.exerciseId(), v.active(), v.highImpact(), v.requiresFloor(), v.axialLoad(),
                v.requiresKneeling(), v.requiresHanging(), v.requiresOverhead(), v.requiresInversion(),
                v.requiresSuspendedSupport());
    }

    private TreeSet<Long> activosDeApoyo() {
        return new TreeSet<>(jdbc.queryForList(
                "SELECT exercise_id FROM exercises WHERE is_active AND requires_suspended_support", Long.class));
    }
}
