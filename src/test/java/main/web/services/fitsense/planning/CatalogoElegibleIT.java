package main.web.services.fitsense.planning;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalCatalogService;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.CatalogEligibilityPolicy;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.infrastructure.generation.rules.RuleBasedTrainingPlanGenerator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Paso 2 con el CATALOGO REAL: PostgreSQL efimero con V1..V33 aplicadas.
 * <p>
 * Para cada perfil de la matriz, el conjunto que devuelve
 * {@link ExternalCatalogService#fetchEligibleFor} se compara, id por id, con un
 * ORACULO SQL escrito aparte desde las reglas -seguridad por edad, nivel y
 * declaradas; dificultad; equipamiento N:M de V23; bloqueos; retenidos
 * INC-4C-03; filtro funcional por objetivo- sin llamar al codigo de produccion
 * que las implementa. Igualdad exacta: nada permitido se pierde y nada
 * incompatible entra.
 * <p>
 * Escribe los conteos antes/despues en {@code target/paso2/conteos.csv}.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
class CatalogoElegibleIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired ExternalCatalogService catalogo;
    @Autowired NamedParameterJdbcTemplate jdbc;

    static final List<String> OBJETIVOS =
            List.of("GAIN_MUSCLE", "INCREASE_STRENGTH", "LOSE_WEIGHT", "GENERAL_FITNESS");
    static final List<String> NIVELES = List.of("BEGINNER", "INTERMEDIATE", "ADVANCED");
    static final List<Integer> EDADES = List.of(30, 62, 72);

    /** Lugar y equipamiento declarado. Peso corporal siempre esta disponible. */
    record Entorno(String nombre, String lugar, List<String> equipamiento) {}

    static final List<String> TODO_EL_EQUIPAMIENTO = List.of("dumbbell", "band", "kettlebell",
            "stability ball", "medicine ball", "rope", "barbell", "ez barbell", "olympic barbell", "cable",
            "leverage machine", "smith machine", "sled machine", "weighted", "assisted", "other", "bench",
            "pull-up bar", "suspension straps", "parallel bars", "gymnastic rings", "bosu ball",
            "balance board", "ab wheel", "step platform", "plyo box", "stationary bike", "treadmill",
            "landmine", "captains chair");

    static final List<Entorno> ENTORNOS = List.of(
            new Entorno("HOME_PESO_CORPORAL", "HOME", List.of()),
            new Entorno("HOME_BASICO", "HOME", List.of("dumbbell", "band")),
            // HOME con todo declarado: el filtro de requires_gym tiene que actuar.
            new Entorno("HOME_TODO_DECLARADO", "HOME", TODO_EL_EQUIPAMIENTO),
            new Entorno("GYM_COMPLETO", "GYM", TODO_EL_EQUIPAMIENTO));

    record Restricciones(String nombre, SafetyProfile.Declaradas declaradas) {}

    static final List<Restricciones> RESTRICCIONES = List.of(
            new Restricciones("SIN", SafetyProfile.Declaradas.sinLimitaciones()),
            // El perfil fragil del censo: sin suelo ni brazos por encima de la cabeza.
            new Restricciones("SIN_SUELO_NI_OVERHEAD", new SafetyProfile.Declaradas(true, false, true, true, false)),
            new Restricciones("TODAS", new SafetyProfile.Declaradas(false, false, false, false, false)));

    static final List<Long> RETENIDOS = List.of(813L, 858L, 861L, 873L, 885L, 1192L, 1298L);

    static final List<String> FILAS = new ArrayList<>();

    @Test
    void elConjuntoElegibleCoincideConElOraculoEnTodaLaMatriz() {
        int casos = 0;
        for (var objetivo : OBJETIVOS)
            for (var entorno : ENTORNOS)
                for (var nivel : NIVELES)
                    for (var edad : EDADES)
                        for (var restr : RESTRICCIONES) {
                            comprobarCaso(objetivo, entorno, nivel, edad, restr, List.of());
                            casos++;
                        }
        assertEquals(4 * 4 * 3 * 3 * 3, casos);
    }

    /** Un bloqueo individual gana siempre, tambien a la admision por capacidad. */
    @Test
    void unBloqueoIndividualGanaALaAdmisionPorCapacidad() {
        var entorno = ENTORNOS.get(3);
        var restr = RESTRICCIONES.get(0);
        var sinBloqueo = comprobarCaso("GENERAL_FITNESS", entorno, "ADVANCED", 30, restr, List.of());
        var complementos = sinBloqueo.stream()
                .filter(c -> CatalogEligibilityPolicy.esComplemento(c, "GENERAL_FITNESS"))
                .map(CandidateExercise::exerciseId).limit(5).toList();
        assertEquals(5, complementos.size());

        var conBloqueo = comprobarCaso("GENERAL_FITNESS", entorno, "ADVANCED", 30, restr, complementos);
        var ids = conBloqueo.stream().map(CandidateExercise::exerciseId).collect(Collectors.toSet());
        complementos.forEach(id -> assertFalse(ids.contains(id), "bloqueado y elegible: " + id));
    }

    /** Lo que se espera del perfil fragil del censo: cardio solo sin impacto. */
    @Test
    void principianteEnCasaRecibeCardioSinAltoImpacto() {
        var elegibles = comprobarCaso("LOSE_WEIGHT", ENTORNOS.get(0), "BEGINNER", 30,
                RESTRICCIONES.get(1), List.of());
        var cardio = elegibles.stream()
                .filter(c -> "LOCOMOTION_CARDIO".equals(c.movementPattern())).map(CandidateExercise::exerciseId)
                .toList();
        assertFalse(cardio.isEmpty(), "LOSE_WEIGHT sin ninguna ruta de cardio");
        var altoImpacto = jdbc.queryForList(
                "select exercise_id from exercises where high_impact and exercise_id in (:ids)",
                Map.of("ids", cardio), Long.class);
        assertEquals(List.of(), altoImpacto);
    }

    private List<CandidateExercise> comprobarCaso(String objetivo, Entorno entorno, String nivel, int edad,
                                                  Restricciones restr, List<Long> bloqueados) {
        var perfil = new PlanningProfile(99L, edad, "FEMALE", new BigDecimal("165"), new BigDecimal("70"),
                null, nivel, objetivo, "prueba", "Ninguna", entorno.lugar(), 3,
                List.of((short) 1, (short) 3, (short) 5), 45, entorno.equipamiento(), bloqueados,
                restr.declaradas());
        var seguridad = SafetyProfile.forAgeLevelAndDeclared(edad, nivel, restr.declaradas());

        var elegibles = catalogo.fetchEligibleFor(perfil, perfil.maxDifficultyLevel(), seguridad);
        var obtenidos = elegibles.stream().map(CandidateExercise::exerciseId).collect(Collectors.toSet());

        var caso = "%s/%s/%s/%d/%s/bloq=%s".formatted(objetivo, entorno.nombre(), nivel, edad,
                restr.nombre(), bloqueados);
        var esperados = oraculo(objetivo, entorno, nivel, edad, restr.declaradas(), bloqueados, false);
        var faltan = new TreeSet<>(esperados); faltan.removeAll(obtenidos);
        var sobran = new TreeSet<>(obtenidos); sobran.removeAll(esperados);
        assertTrue(faltan.isEmpty() && sobran.isEmpty(),
                caso + " faltan (permitidos perdidos)=" + faltan + " sobran (incompatibles)=" + sobran);
        assertEquals(obtenidos.size(), elegibles.size(), caso + ": ids repetidos");

        // Antes del Paso 2: el mismo oraculo con el filtro funcional anterior.
        var antes = oraculo(objetivo, entorno, nivel, edad, restr.declaradas(), bloqueados, true);
        assertTrue(obtenidos.containsAll(antes), caso + ": se perdio algo que ya era elegible");

        var complementos = CatalogEligibilityPolicy.complementos(elegibles, objetivo);
        var nuevos = new HashSet<>(obtenidos); nuevos.removeAll(antes);
        assertEquals(nuevos, complementos, caso + ": lo nuevo debe ser exactamente lo admitido por capacidad");

        long cardio = elegibles.stream().filter(c -> complementos.contains(c.exerciseId())
                && "LOCOMOTION_CARDIO".equals(c.movementPattern())).count();
        // Ruta directa total, ya elegible antes o admitida ahora: es la cifra
        // que el Paso 3 necesita para decir si CARDIO o MOBILITY son factibles.
        long rutaCardio = elegibles.stream().filter(c -> "LOCOMOTION_CARDIO".equals(c.movementPattern())).count();
        long rutaMovilidad = elegibles.stream().filter(c -> "MOBILITY".equals(c.movementPattern())).count();
        if (bloqueados.isEmpty())
            FILAS.add(String.join(",", objetivo, entorno.nombre(), nivel, String.valueOf(edad), restr.nombre(),
                    String.valueOf(antes.size()), String.valueOf(obtenidos.size()), String.valueOf(cardio),
                    String.valueOf(complementos.size() - cardio), String.valueOf(rutaCardio),
                    String.valueOf(rutaMovilidad)));

        // El motor de reglas, con datos reales: misma semana con y sin complementos.
        if (!complementos.isEmpty()) {
            var generador = new RuleBasedTrainingPlanGenerator(30, new SessionDurationEstimator());
            var sin = elegibles.stream().filter(c -> !complementos.contains(c.exerciseId())).toList();
            assertEquals(generador.generate(contexto(perfil, sin), List.of()),
                    generador.generate(contexto(perfil, elegibles), List.of()), caso + ": motor de reglas");
        }
        return elegibles;
    }

    private static PlanGenerationContext contexto(PlanningProfile perfil, List<CandidateExercise> elegibles) {
        var lunes = LocalDate.of(2026, 10, 26);
        return new PlanGenerationContext(perfil.userId(), (short) 1, lunes, lunes.plusDays(6), perfil,
                PlanAdjustment.none(), PreviousWeekSummary.empty(), elegibles, SafetyProfile.none(),
                (PrescriptionParams) null);
    }

    /** Las reglas, reescritas en SQL sin pasar por el codigo que las implementa. */
    private Set<Long> oraculo(String objetivo, Entorno entorno, String nivel, int edad,
                              SafetyProfile.Declaradas d, List<Long> bloqueados, boolean filtroAnterior) {
        int maxDificultad = switch (nivel) { case "BEGINNER" -> 1; case "ADVANCED" -> 3; default -> 2; };
        boolean sinImpacto = edad >= 60 || "BEGINNER".equals(nivel) || Boolean.FALSE.equals(d.allowHighImpact());
        boolean sinSuelo = edad >= 70 || Boolean.FALSE.equals(d.allowFloorExercises());
        boolean sinAxial = edad >= 65;
        boolean sinRodillas = Boolean.FALSE.equals(d.allowKneelingExercises());
        boolean sinSuspension = Boolean.FALSE.equals(d.allowHangingExercises());
        boolean sinOverhead = Boolean.FALSE.equals(d.allowOverheadExercises());
        boolean sinInversion = edad >= 65;

        var disponibles = new ArrayList<String>(entorno.equipamiento());
        disponibles.add("body weight");
        var excluidos = new ArrayList<Long>(RETENIDOS);
        excluidos.addAll(bloqueados);

        boolean cardio = !filtroAnterior && Set.of("LOSE_WEIGHT", "GENERAL_FITNESS").contains(objetivo);
        boolean movilidad = !filtroAnterior && "GENERAL_FITNESS".equals(objetivo);

        var sql = """
                select e.exercise_id
                  from exercises e join body_parts bp on bp.body_part_id = e.body_part_id
                 where e.is_active
                   and e.difficulty_level <= :maxDificultad
                   and e.exercise_id not in (:excluidos)
                   and (not :sinImpacto or not e.high_impact)
                   and (not :sinSuelo or not e.requires_floor)
                   and (not :sinAxial or not e.axial_load)
                   and (not :sinRodillas or not e.requires_kneeling)
                   and (not :sinSuspension or not e.requires_hanging)
                   and (not :sinOverhead or not e.requires_overhead)
                   and (not :sinInversion or not e.requires_inversion)
                   and not exists (
                         select 1 from exercise_equipment_requirements r
                          where r.exercise_id = e.exercise_id
                            and not exists (
                                  select 1 from exercise_equipment_requirements a
                                    join equipment_types t on t.equipment_id = a.equipment_id
                                   where a.exercise_id = r.exercise_id and a.group_no = r.group_no
                                     and t.code in (:disponibles)
                                     and not (:enCasa and t.requires_gym)))
                   and (   (bp.code not in ('neck', 'lower arms', 'cardio')
                            and lower(e.name_es) not like '%estiramiento%')
                        or (:cardio and e.movement_pattern = 'LOCOMOTION_CARDIO')
                        or (:movilidad and e.movement_pattern = 'MOBILITY'))
                """;
        var params = new MapSqlParameterSource()
                .addValue("maxDificultad", maxDificultad).addValue("excluidos", excluidos)
                .addValue("sinImpacto", sinImpacto).addValue("sinSuelo", sinSuelo)
                .addValue("sinAxial", sinAxial).addValue("sinRodillas", sinRodillas)
                .addValue("sinSuspension", sinSuspension).addValue("sinOverhead", sinOverhead)
                .addValue("sinInversion", sinInversion).addValue("disponibles", disponibles)
                .addValue("enCasa", "HOME".equals(entorno.lugar()))
                .addValue("cardio", cardio).addValue("movilidad", movilidad);
        return new HashSet<>(jdbc.queryForList(sql, params, Long.class));
    }

    @AfterAll
    static void escribirConteos() throws IOException {
        var dir = Path.of("target", "paso2");
        Files.createDirectories(dir);
        var lineas = new ArrayList<String>();
        lineas.add("objetivo,entorno,nivel,edad,restricciones,elegibles_antes,elegibles_despues,"
                + "complementos_cardio,complementos_movilidad,ruta_cardio_total,ruta_movilidad_total");
        lineas.addAll(FILAS);
        Files.write(dir.resolve("conteos.csv"), lineas);
    }
}
