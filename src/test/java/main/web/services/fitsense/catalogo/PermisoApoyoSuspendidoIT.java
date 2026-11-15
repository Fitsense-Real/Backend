package main.web.services.fitsense.catalogo;

import main.web.services.fitsense.catalog.domain.services.ExerciseQueryService;
import main.web.services.fitsense.catalog.infrastructure.persistence.jpa.repositories.BodyPartRepository;
import main.web.services.fitsense.catalog.infrastructure.persistence.jpa.repositories.EquipmentTypeRepository;
import main.web.services.fitsense.catalog.interfaces.acl.CatalogContextFacade;
import main.web.services.fitsense.planning.application.internal.commandservices.SafetyConflictChecker;
import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalCatalogService;
import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalProfilingService;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.ModoApoyoSuspendido;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningProfile;
import main.web.services.fitsense.planning.domain.model.valueobjects.SafetyProfile;
import main.web.services.fitsense.planning.domain.services.SafetyConflictPolicy;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.profiling.infrastructure.persistence.jpa.repositories.UserProfileRepository;
import main.web.services.fitsense.profiling.interfaces.acl.ProfilingContextFacade;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V53-B · permiso propio «apoyo suspendido» (propuesta, opcion B de cierre/V53_DEFINITIVO.md §2).
 * <p>
 * El contexto de Spring arranca con {@code fitsense.seguridad.permiso-apoyo-suspendido=true} (opcion B). Las
 * referencias APAGADA y opcion A se construyen a mano sobre los mismos beans, como en
 * {@link ApoyoSuspendidoInterruptorIT}: fachada, ACL de catalogo, ACL de perfil y comprobador de conflictos.
 * Todo con perfiles sinteticos creados en la base efimera; el permiso se escribe con el metodo de dominio
 * {@code UserProfile.confirmarApoyoSuspendido}, el unico que lo toca.
 * <p>
 * Se mide sobre el conjunto elegible de planning ({@code ExternalCatalogService}), que incluye INC-4C-03, INC-CAT-1374
 * e INC-CAT-C39: asi se ve que la opcion B no levanta ninguna contencion existente.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.seguridad.permiso-apoyo-suspendido=true",
        // V53-B se mide frente al catalogo SIN la contencion INC-CAT-CONS-v1 (15/11/2026), que es una capa aparte: con
        // ella, nadie recibe los 15 de apoyo suspendido salvo con el permiso confirmado. La combinacion se comprueba en
        // conContencionConservadora().
        "fitsense.catalogo.contencion-conservadora=false"
})
@DisplayName("V53-B · permiso propio «apoyo suspendido» (opcion B encendida en este contexto)")
class PermisoApoyoSuspendidoIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    /** Los 15 de V53. */
    static final Set<Long> APOYO = Set.of(457L, 458L, 459L, 462L, 777L, 920L, 934L, 1069L, 1153L, 1184L, 1303L,
            1304L, 1306L, 1317L, 1335L);
    /** Los 10 que hoy (apagado) siguen elegibles con «no colgarse» (V53_DEFINITIVO.md §1). */
    static final Set<Long> DIEZ = Set.of(457L, 458L, 459L, 462L, 777L, 920L, 934L, 1184L, 1317L, 1335L);
    /** Los 5 que ya estan fuera con «no colgarse»: 1069 por requires_hanging, el resto por INC-CAT-C39. */
    static final Set<Long> CINCO = Set.of(1069L, 1153L, 1303L, 1304L, 1306L);

    static final List<String> TODO = ApoyoSuspendidoInterruptorIT.TODO;
    static final AtomicLong IDS = new AtomicLong(8800);

    @Autowired CatalogContextFacade fachadaB;
    @Autowired ExternalCatalogService catalogoB;
    @Autowired ExternalProfilingService perfilB;
    @Autowired SafetyConflictChecker comprobadorB;
    @Autowired ProfilingContextFacade profiling;
    @Autowired ExerciseQueryService consultas;
    @Autowired BodyPartRepository zonas;
    @Autowired EquipmentTypeRepository equipos;
    @Autowired UserProfileCommandService perfiles;
    @Autowired UserProfileRepository repositorio;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate tx;

    /** Una combinacion de los tres interruptores de planning (fachada, ACL de catalogo, ACL de perfil). */
    record Modo(ExternalProfilingService perfil, ExternalCatalogService catalogo, SafetyConflictChecker comprobador) {}

    /** Los ACL construidos a mano tambien sin INC-CAT-CONS-v1 (su valor por defecto es encendida). */
    private static ExternalCatalogService sinContencion(ExternalCatalogService catalogo) {
        org.springframework.test.util.ReflectionTestUtils.setField(catalogo, "contencionConservadora", false);
        return catalogo;
    }

    private Modo apagado() {
        var catalogo = sinContencion(new ExternalCatalogService(new CatalogContextFacade(consultas, zonas, equipos, false, false)));
        return new Modo(new ExternalProfilingService(profiling, false, false), catalogo,
                new SafetyConflictChecker(catalogo));
    }

    private Modo opcionA() {
        var catalogo = sinContencion(new ExternalCatalogService(new CatalogContextFacade(consultas, zonas, equipos, true)));
        return new Modo(new ExternalProfilingService(profiling, true, false), catalogo,
                new SafetyConflictChecker(catalogo));
    }

    private Modo opcionB() {
        return new Modo(perfilB, catalogoB, comprobadorB);
    }

    /** Un perfil sintetico; {@code apoyo} null = sin responder (como cualquier perfil creado hoy). */
    private long usuario(boolean colgarse, Boolean apoyo) {
        long u = IDS.incrementAndGet();
        jdbc.update("INSERT INTO users (user_id, email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " OVERRIDING SYSTEM VALUE VALUES (?, ?, 'x', 'V53B', now(), now())", u, "v53b" + u + "@pruebas.invalid");
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 3, 1), BiologicalSex.FEMALE,
                new BigDecimal("168"), new BigDecimal("64"), null, FitnessLevel.ADVANCED, GoalType.GAIN_MUSCLE,
                "v53b", TrainingLocation.GYM, (short) 4, List.of((short) 1, (short) 2, (short) 4, (short) 5),
                (short) 60, TODO, List.of(), null, true, true, true, colgarse, true)).orElseThrow();
        if (apoyo != null)
            tx.executeWithoutResult(s -> {
                var p = repositorio.findById(u).orElseThrow();
                p.confirmarApoyoSuspendido(apoyo);
                repositorio.save(p);
            });
        return u;
    }

    private record Lectura(PlanningProfile perfil, SafetyProfile seguridad, TreeSet<Long> elegibles) {}

    private Lectura leer(Modo m, long u) {
        return tx.execute(s -> {
            var perfil = m.perfil().fetchProfile(u).orElseThrow();
            var seguridad = SafetyProfile.forAgeLevelAndDeclared(perfil.age(), perfil.fitnessLevel(),
                    perfil.declaradas());
            var elegibles = m.catalogo().fetchEligibleForExplanation(perfil, 3, seguridad).stream()
                    .map(CandidateExercise::exerciseId).collect(Collectors.toCollection(TreeSet::new));
            return new Lectura(perfil, seguridad, elegibles);
        });
    }

    private static TreeSet<Long> deApoyo(Set<Long> elegibles) {
        var r = new TreeSet<>(elegibles);
        r.retainAll(APOYO);
        return r;
    }

    private static TreeSet<Long> menos(Set<Long> a, Set<Long> b) {
        var r = new TreeSet<>(a);
        r.removeAll(b);
        return r;
    }

    private TreeSet<Long> colgantes() {
        return new TreeSet<>(jdbc.queryForList(
                "SELECT exercise_id FROM exercises WHERE is_active AND requires_hanging", Long.class));
    }

    @Test
    @DisplayName("V55: dos columnas NULL; crear el perfil no las rellena y no hay backfill")
    void esquema() {
        long u = usuario(false, null);
        var fila = jdbc.queryForMap("SELECT allow_hanging_exercises, allow_suspended_support,"
                + " suspended_support_confirmed_at FROM user_profiles WHERE user_id = ?", u);
        assertEquals(Boolean.FALSE, fila.get("allow_hanging_exercises"));
        assertNull(fila.get("allow_suspended_support"));
        assertNull(fila.get("suspended_support_confirmed_at"));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM user_profiles WHERE allow_suspended_support IS NOT"
                + " NULL AND suspended_support_confirmed_at IS NULL", Integer.class), "todo valor lleva confirmacion");
        for (var col : List.of("allow_suspended_support", "suspended_support_confirmed_at"))
            assertEquals("YES", jdbc.queryForObject("SELECT is_nullable FROM information_schema.columns WHERE"
                    + " table_name = 'user_profiles' AND column_name = ?", String.class, col));
        long c = usuario(false, true);
        var conf = jdbc.queryForMap("SELECT allow_suspended_support, suspended_support_confirmed_at FROM user_profiles"
                + " WHERE user_id = ?", c);
        assertEquals(Boolean.TRUE, conf.get("allow_suspended_support"));
        assertNotNull(conf.get("suspended_support_confirmed_at"));
    }

    @Test
    @DisplayName("con INC-CAT-CONS-v1 encendida: sin el permiso confirmado ninguno de los 15; confirmado, vuelven los 10")
    void conContencionConservadora() {
        Object destino = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(catalogoB);
        org.springframework.test.util.ReflectionTestUtils.setField(destino, "contencionConservadora", true);
        try {
            for (boolean colgarse : new boolean[]{true, false})
                for (Boolean apoyo : new Boolean[]{null, false})
                    assertEquals(Set.of(), deApoyo(leer(opcionB(), usuario(colgarse, apoyo)).elegibles()),
                            "sin permiso confirmado, ninguno de apoyo suspendido: colgarse=" + colgarse + " permiso=" + apoyo);
            assertEquals(new TreeSet<>(DIEZ), deApoyo(leer(opcionB(), usuario(false, true)).elegibles()),
                    "confirmado con «no colgarse»: los 10, ni 1069 ni los C39");
        } finally {
            org.springframework.test.util.ReflectionTestUtils.setField(destino, "contencionConservadora", false);
        }
    }

    @Test
    @DisplayName("apagado: el conjunto elegible es el de hoy y el permiso guardado se ignora")
    void apagadoIdenticoAHoy() {
        var off = apagado();
        // «Hoy»: la fachada con el constructor anterior a V53-B y el ACL de perfil de un argumento.
        var hoyCatalogo = sinContencion(new ExternalCatalogService(new CatalogContextFacade(consultas, zonas, equipos, false)));
        var hoy = new Modo(new ExternalProfilingService(profiling), hoyCatalogo, new SafetyConflictChecker(hoyCatalogo));
        for (boolean colgarse : new boolean[]{true, false}) {
            var referencia = leer(hoy, usuario(colgarse, null)).elegibles();
            for (Boolean apoyo : new Boolean[]{null, true, false})
                assertEquals(referencia, leer(off, usuario(colgarse, apoyo)).elegibles(),
                        "apagado, colgarse=" + colgarse + " permiso=" + apoyo);
        }
        // Lo que hoy recibe «no colgarse» de los 15: exactamente los 10 de V53_DEFINITIVO.md.
        assertEquals(new TreeSet<>(DIEZ), deApoyo(leer(off, usuario(false, null)).elegibles()));
        // Sin «no colgarse», hoy llegan todos los activos de los 15.
        var activos = new TreeSet<>(jdbc.queryForList(
                "SELECT exercise_id FROM exercises WHERE is_active AND requires_suspended_support", Long.class));
        assertEquals(activos, deApoyo(leer(off, usuario(true, null)).elegibles()));
    }

    @Test
    @DisplayName("B, «no colgarse» sin responder: hereda y salen los 10 (igual que la opcion A)")
    void bSinResponderHereda() {
        var hoy = leer(apagado(), usuario(false, null)).elegibles();
        var b = leer(opcionB(), usuario(false, null));
        assertTrue(b.seguridad().excludeSuspendedSupport());
        assertEquals(Set.of(), deApoyo(b.elegibles()));
        assertEquals(menos(hoy, DIEZ), b.elegibles(), "solo salen los 10, nada mas");
        assertEquals(leer(opcionA(), usuario(false, null)).elegibles(), b.elegibles(),
                "sin responder, B es la opcion A");
        // Sin «no colgarse» y sin responder, B no cambia nada.
        assertEquals(leer(apagado(), usuario(true, null)).elegibles(), leer(opcionB(), usuario(true, null)).elegibles());
    }

    @Test
    @DisplayName("B, «no colgarse» con el permiso confirmado: vuelven los 10; 1069 (HG) y C39 siguen fuera")
    void bConfirmadoNoLevantaNadaMas() {
        var b = leer(opcionB(), usuario(false, true));
        assertFalse(b.seguridad().excludeSuspendedSupport());
        assertTrue(b.seguridad().excludeHanging(), "«no colgarse» sigue igual");
        assertEquals(new TreeSet<>(DIEZ), deApoyo(b.elegibles()));
        for (var id : CINCO) assertFalse(b.elegibles().contains(id), "sigue fuera: " + id);
        assertTrue(Collections.disjoint(b.elegibles(), colgantes()), "requires_hanging sigue fuera");
        assertEquals(leer(apagado(), usuario(false, null)).elegibles(), b.elegibles(),
                "confirmado que si, el conjunto es el de hoy para «no colgarse»");
    }

    @Test
    @DisplayName("B, puede colgarse pero no apoyo suspendido: salen los 15; lo que cuelga sigue")
    void bSoloApoyo() {
        var hoy = leer(apagado(), usuario(true, null)).elegibles();
        var b = leer(opcionB(), usuario(true, false));
        assertFalse(b.seguridad().excludeHanging());
        assertTrue(b.seguridad().excludeSuspendedSupport());
        assertEquals(Set.of(), deApoyo(b.elegibles()));
        assertEquals(menos(hoy, APOYO), b.elegibles(), "solo salen los 15");
        var colgantesHoy = new TreeSet<>(hoy);
        colgantesHoy.retainAll(colgantes());
        colgantesHoy.removeAll(APOYO);
        assertFalse(colgantesHoy.isEmpty());
        assertTrue(b.elegibles().containsAll(colgantesHoy), "requires_hanging sigue elegible");
    }

    @Test
    @DisplayName("planes publicados: la causa apoyo_suspendido sale para el 457 solo cuando B lo excluye")
    void causaEnPlanesPublicados() {
        record Caso(boolean colgarse, Boolean apoyo, boolean causa) {}
        for (var c : List.of(new Caso(false, null, true), new Caso(false, true, false), new Caso(false, false, true),
                new Caso(true, null, false), new Caso(true, true, false), new Caso(true, false, true))) {
            long u = usuario(c.colgarse(), c.apoyo());
            var b = leer(opcionB(), u);
            var motivosB = tx.execute(s -> opcionB().comprobador().motivos(List.of(457L, 1069L), b.perfil(),
                    b.seguridad()));
            assertEquals(c.causa(), SafetyConflictPolicy.APOYO_SUSPENDIDO.equals(motivosB.get(457L)),
                    c + " -> " + motivosB);
            // 1069 cuelga: con «no colgarse» sigue bloqueado por suspension, confirme o no el permiso.
            if (!c.colgarse()) assertTrue(motivosB.get(1069L).contains(SafetyConflictPolicy.SUSPENSION), "" + motivosB);
            // El mismo caso apagado: ninguna causa nueva.
            var off = leer(apagado(), u);
            var motivosOff = tx.execute(s -> apagado().comprobador().motivos(List.of(457L), off.perfil(),
                    off.seguridad()));
            assertTrue(motivosOff.isEmpty(), "apagado: " + motivosOff);
        }
    }

    @Test
    @DisplayName("el script de alcance de V53-B es de solo lectura y corre sobre el esquema V1-V55")
    void scriptDeAlcanceDeSoloLectura() throws Exception {
        var sql = java.nio.file.Files.readString(java.nio.file.Path.of(
                "docs/FitSense/catalogo_integral/cierre/v53b/impacto_permisos_v53b_solo_lectura.sql"));
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
        long sinResponder = usuario(false, null);
        long confirmado = usuario(false, true);
        long soloApoyo = usuario(true, false);
        tx.execute(s -> {
            jdbc.execute("SET TRANSACTION READ ONLY");
            var resumen = jdbc.queryForList(consultas.get(0)).stream()
                    .collect(Collectors.toMap(f -> (String) f.get("medida"), f -> ((Number) f.get("n")).longValue()));
            assertEquals(9, resumen.size());
            var excluidos = new TreeSet<>(jdbc.queryForList("SELECT user_id FROM user_profiles WHERE"
                    + " coalesce(allow_suspended_support, allow_hanging_exercises) = false", Long.class));
            assertTrue(excluidos.contains(sinResponder) && excluidos.contains(soloApoyo));
            assertFalse(excluidos.contains(confirmado));
            assertEquals(excluidos.size(), resumen.get("perfiles_excluidos_con_B"));
            jdbc.queryForList(consultas.get(1));
            return null;
        });
        // La regla del script es la misma que resuelve planning con la opcion B.
        for (long u : new long[]{sinResponder, confirmado, soloApoyo}) {
            boolean enScript = Boolean.FALSE.equals(jdbc.queryForObject("SELECT coalesce(allow_suspended_support,"
                    + " allow_hanging_exercises) FROM user_profiles WHERE user_id = ?", Boolean.class, u));
            assertEquals(enScript, leer(opcionB(), u).seguridad().excludeSuspendedSupport(), "usuario " + u);
        }
    }

    @Test
    @DisplayName("el modo del contexto es la opcion B y la fachada entrega el hecho")
    void modoDelContexto() {
        assertEquals(ModoApoyoSuspendido.PERMISO_PROPIO, perfilB.modoApoyoSuspendido());
        assertTrue(fachadaB.permisoApoyoSuspendidoActivo());
        var flags = tx.execute(s -> fachadaB.fetchSafetyFlags(APOYO));
        flags.values().forEach(v -> assertTrue(v.requiresSuspendedSupport(), "B: " + v.exerciseId()));
        var flagsOff = tx.execute(s -> new CatalogContextFacade(consultas, zonas, equipos, false, false)
                .fetchSafetyFlags(APOYO));
        flagsOff.values().forEach(v -> assertFalse(v.requiresSuspendedSupport(), "apagado: " + v.exerciseId()));
    }
}
