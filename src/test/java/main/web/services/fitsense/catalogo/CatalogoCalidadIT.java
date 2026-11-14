package main.web.services.fitsense.catalogo;

import main.web.services.fitsense.planning.domain.services.ImpactoPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CONTROLES PERMANENTES DE CALIDAD DEL CATALOGO (auditoria integral 2026-11). Se ejecutan en CI sobre el catalogo que
 * dejan TODAS las migraciones, para que una migracion futura no reintroduzca los defectos corregidos:
 * <ol>
 *   <li>integridad y valores obligatorios de los ejercicios activos;</li>
 *   <li>valores validos de patron, prescripcion, dificultad y equipamiento;</li>
 *   <li>referencias a GIF e imagen y atribucion;</li>
 *   <li>HUELLA del catalogo: clasificacion, dificultad, prescripcion, equipamiento, banderas de seguridad, impacto y
 *       estiramiento explicitos (V51), la firma de las antiguas reglas por nombre y una firma md5 del nombre y las
 *       instrucciones en espanol de cada activo. Cualquier cambio (bandera, patron, equipo...) hace fallar la prueba hasta que se
 *       actualiza {@code src/test/resources/catalogo/huella_catalogo.tsv} EN EL MISMO COMMIT que la migracion: el diff de
 *       ese archivo es la traza revisable del cambio;</li>
 *   <li>coherencia texto/bandera: el nombre y las instrucciones no piden colgarse, suelo, brazos por encima de la cabeza
 *       ni apoyar la rodilla si la bandera no lo refleja, salvo excepciones revisadas y justificadas en
 *       {@code excepciones_texto_bandera.tsv};</li>
 *   <li>sin nombres duplicados nuevos entre los activos.</li>
 * </ol>
 * La revision semantica y visual no se sustituye: ver docs/FitSense/catalogo_integral/PROCESO_REVISION.md.
 * <p>
 * Regenerar la huella (solo con un cambio de catalogo revisado): {@code -Dfitsense.catalogo.huella.capturar=true}.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("Catalogo · controles permanentes de calidad")
class CatalogoCalidadIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired JdbcTemplate jdbc;

    static final Path HUELLA = Path.of("src/test/resources/catalogo/huella_catalogo.tsv");
    static final Path EXCEPCIONES = Path.of("src/test/resources/catalogo/excepciones_texto_bandera.tsv");
    static final Path DUPLICADOS = Path.of("src/test/resources/catalogo/nombres_duplicados_conocidos.tsv");

    static final Set<String> PATRONES = Set.of("HORIZONTAL_PUSH", "HORIZONTAL_PULL", "VERTICAL_PUSH", "VERTICAL_PULL",
            "KNEE_DOMINANT", "HIP_HINGE", "HIP_EXTENSION", "HIP_ABDUCTION_ADDUCTION", "ELBOW_FLEXION", "ELBOW_EXTENSION",
            "SHOULDER_RAISE", "SHOULDER_EXTENSION", "HORIZONTAL_ADDUCTION", "SCAPULAR_ELEVATION", "WRIST_FOREARM",
            "ANKLE_PLANTAR_FLEXION", "TRUNK_FLEXION", "TRUNK_EXTENSION", "TRUNK_ROTATION_LATERAL_FLEXION",
            "CORE_STABILITY", "LOCOMOTION_CARDIO", "MOBILITY", "BALANCE_CONTROL", "OTHER");

    /** Reglas texto/bandera: (codigo, columna de la bandera, patron sobre name_es + instructions_es en minusculas). */
    static final List<Object[]> REGLAS_TEXTO = List.of(
            new Object[]{"COLGARSE", "requires_hanging", Pattern.compile("cu[eé]lga(te|rse)|colg[aá]ndote|colgad[oa]s? de")},
            new Object[]{"SOBRE_LA_CABEZA", "requires_overhead", Pattern.compile(
                    "(brazos?|manos|mancuernas?|barra|pesa) (extendid[oa]s? )?por encima de la cabeza")},
            new Object[]{"SUELO", "requires_floor", Pattern.compile(
                    "(t[uú]mbate|acu[eé]state|si[eé]ntate|arrod[ií]llate|ponte de rodillas)(?:(?!pies)[^.]){0,30}(en|sobre) el suelo"
                            + "|(tumbad[oa]|sentad[oa]|boca (arriba|abajo)) en el suelo|a cuatro patas|esterilla|colchoneta")},
            new Object[]{"RODILLAS", "requires_kneeling", Pattern.compile(
                    "arrod[ií]llate|ponte de rodillas|apoya (una|la) rodilla|rodilla apoyada")});

    @Test
    @DisplayName("1-3 · integridad, valores obligatorios, valores validos, GIF/imagen y atribucion")
    void integridad() {
        var problemas = new ArrayList<String>();
        for (var r : jdbc.queryForList("""
                SELECT e.exercise_id, e.name_es, e.instructions_es, e.instruction_steps_es::text AS pasos, e.gif_path,
                       e.image_path, e.media_attribution, e.difficulty_level, e.default_prescription, e.movement_pattern,
                       e.is_isolation
                  FROM exercises e WHERE e.is_active ORDER BY e.exercise_id""")) {
            long id = ((Number) r.get("exercise_id")).longValue();
            if (vacio(r.get("name_es"))) problemas.add(id + " sin name_es");
            if (vacio(r.get("instructions_es"))) problemas.add(id + " sin instructions_es");
            if (vacio(r.get("pasos")) || !((String) r.get("pasos")).startsWith("[\"")) problemas.add(id + " sin pasos");
            if (r.get("gif_path") == null || !((String) r.get("gif_path")).matches("videos/[0-9A-Za-z-]+\\.gif"))
                problemas.add(id + " gif_path invalido: " + r.get("gif_path"));
            if (r.get("image_path") == null || !((String) r.get("image_path")).matches("images/[0-9A-Za-z-]+\\.jpg"))
                problemas.add(id + " image_path invalido: " + r.get("image_path"));
            if (vacio(r.get("media_attribution"))) problemas.add(id + " sin atribucion");
            int dif = ((Number) r.get("difficulty_level")).intValue();
            if (dif < 1 || dif > 3) problemas.add(id + " dificultad " + dif);
            if (!Set.of("SETS_REPS", "DURATION").contains(r.get("default_prescription")))
                problemas.add(id + " prescripcion " + r.get("default_prescription"));
            if (!PATRONES.contains(r.get("movement_pattern"))) problemas.add(id + " patron " + r.get("movement_pattern"));
            if (r.get("is_isolation") == null) problemas.add(id + " sin is_isolation");
        }
        // GIF e imagen: base del nombre del archivo comun (mismo ejercicio de la fuente) y sin repetir entre activos.
        problemas.addAll(jdbc.queryForList("""
                SELECT exercise_id || ' gif e imagen de ejercicios distintos' FROM exercises WHERE is_active
                   AND split_part(split_part(gif_path, '/', 2), '.', 1) <> split_part(split_part(image_path, '/', 2), '.', 1)""",
                String.class));
        problemas.addAll(jdbc.queryForList("""
                SELECT 'gif repetido: ' || gif_path FROM exercises WHERE is_active GROUP BY gif_path HAVING count(*) > 1""",
                String.class));
        assertEquals(List.of(), problemas);
    }

    @Test
    @DisplayName("2 · equipamiento: el principal esta entre los requisitos; 'body weight' como requisito solo, en el grupo 1")
    void equipamiento() {
        // Convencion de V36-V39: un ejercicio que se muestra como peso corporal pero exige un implemento lleva
        // 'body weight' como grupo 1 (siempre disponible) y el implemento en el grupo 2. Nunca mezclado con otro.
        assertEquals(List.of(), jdbc.queryForList("""
                SELECT r.exercise_id || ' grupo ' || r.group_no FROM exercise_equipment_requirements r
                  JOIN equipment_types t ON t.equipment_id = r.equipment_id
                  JOIN exercises e ON e.exercise_id = r.exercise_id
                 WHERE e.is_active AND t.code = 'body weight'
                   AND (r.group_no <> 1 OR EXISTS (SELECT 1 FROM exercise_equipment_requirements o
                                                    WHERE o.exercise_id = r.exercise_id AND o.group_no = 1
                                                      AND o.equipment_id <> r.equipment_id))
                 ORDER BY 1""", String.class));
        assertEquals(List.of(), jdbc.queryForList("""
                SELECT e.exercise_id || ' principal ' || t.code FROM exercises e
                  JOIN equipment_types t ON t.equipment_id = e.equipment_id
                 WHERE e.is_active AND (
                       (EXISTS (SELECT 1 FROM exercise_equipment_requirements r WHERE r.exercise_id = e.exercise_id)
                        AND NOT EXISTS (SELECT 1 FROM exercise_equipment_requirements r
                                         WHERE r.exercise_id = e.exercise_id AND r.equipment_id = e.equipment_id))
                    OR (t.code <> 'body weight'
                        AND NOT EXISTS (SELECT 1 FROM exercise_equipment_requirements r WHERE r.exercise_id = e.exercise_id)))
                 ORDER BY 1""", String.class));
    }

    @Test
    @DisplayName("4 · huella del catalogo: ningun cambio de clasificacion, equipo o banderas sin trazabilidad")
    void huella() throws Exception {
        var moderado = (Pattern) campo("MODERADO");
        var alto = (Pattern) campo("ALTO_POR_NOMBRE");
        var lineas = new ArrayList<String>();
        lineas.add("id\tzona\tobjetivo\tpatron\tdificultad\tprescripcion\tisolation\trequisitos\tbanderas\timpacto\testiramiento"
                + "\tfirma_nombre\ttextos");
        for (var r : jdbc.queryForList("""
                SELECT e.exercise_id, bp.code AS zona, e.target_muscle, e.movement_pattern, e.difficulty_level,
                       e.default_prescription, e.is_isolation, e.name_es, e.impact_level, e.is_stretch,
                       left(md5(e.name_es || '|' || regexp_replace(e.instructions_es, '[\\t\\r\\n]+', ' ', 'g')), 12) AS textos,
                       coalesce((SELECT string_agg(rq.group_no || ':' || t.code, ',' ORDER BY rq.group_no, t.code)
                                   FROM exercise_equipment_requirements rq
                                   JOIN equipment_types t ON t.equipment_id = rq.equipment_id
                                  WHERE rq.exercise_id = e.exercise_id), '-') AS requisitos,
                       concat_ws(' ', CASE WHEN e.high_impact THEN 'HI' END, CASE WHEN e.requires_floor THEN 'FL' END,
                                 CASE WHEN e.axial_load THEN 'AX' END, CASE WHEN e.requires_kneeling THEN 'KN' END,
                                 CASE WHEN e.requires_hanging THEN 'HG' END, CASE WHEN e.requires_overhead THEN 'OH' END,
                                 CASE WHEN e.requires_inversion THEN 'INV' END) AS banderas
                  FROM exercises e JOIN body_parts bp ON bp.body_part_id = e.body_part_id
                 WHERE e.is_active ORDER BY e.exercise_id""")) {
            String nombre = ((String) r.get("name_es")).toLowerCase(Locale.ROOT);
            String firma = (moderado.matcher(nombre).find() ? "M" : "-") + (alto.matcher(nombre).find() ? "A" : "-")
                    + (nombre.contains("estiramiento") ? "E" : "-");
            String banderas = (String) r.get("banderas");
            lineas.add(String.join("\t", String.valueOf(r.get("exercise_id")), (String) r.get("zona"),
                    (String) r.get("target_muscle"), (String) r.get("movement_pattern"),
                    String.valueOf(r.get("difficulty_level")), (String) r.get("default_prescription"),
                    String.valueOf(r.get("is_isolation")), (String) r.get("requisitos"),
                    banderas.isEmpty() ? "-" : banderas, (String) r.get("impact_level"),
                    String.valueOf(r.get("is_stretch")), firma, (String) r.get("textos")));
        }
        String actual = String.join("\n", lineas) + "\n";
        if (Boolean.getBoolean("fitsense.catalogo.huella.capturar")) {
            Files.writeString(HUELLA, actual, StandardCharsets.UTF_8);
            return;
        }
        var esperado = Files.readString(HUELLA, StandardCharsets.UTF_8);
        if (!esperado.equals(actual)) {
            var a = new LinkedHashSet<>(Arrays.asList(esperado.split("\n")));
            var b = new LinkedHashSet<>(Arrays.asList(actual.split("\n")));
            var quitadas = a.stream().filter(l -> !b.contains(l)).limit(20).toList();
            var nuevas = b.stream().filter(l -> !a.contains(l)).limit(20).toList();
            Files.writeString(Path.of("target", "huella_catalogo_actual.tsv"), actual, StandardCharsets.UTF_8);
            fail("La huella del catalogo cambio. Si el cambio esta revisado, actualiza " + HUELLA
                    + " en el mismo commit que la migracion (-Dfitsense.catalogo.huella.capturar=true).\nantes: "
                    + quitadas + "\ndespues: " + nuevas);
        }
    }

    @Test
    @DisplayName("5 · coherencia texto/bandera (excepciones revisadas y justificadas)")
    void textoContraBandera() throws Exception {
        var excepciones = new HashSet<String>();
        for (var l : Files.readAllLines(EXCEPCIONES, StandardCharsets.UTF_8)) {
            if (l.isBlank() || l.startsWith("id\t")) continue;
            var c = l.split("\t");
            assertTrue(c.length >= 3 && c[2].length() > 15, "excepcion sin justificacion: " + l);
            excepciones.add(c[0] + "\t" + c[1]);
        }
        var hallazgos = new ArrayList<String>();
        var usadas = new HashSet<String>();
        for (var r : jdbc.queryForList("""
                SELECT exercise_id, name_es, instructions_es, requires_hanging, requires_overhead, requires_floor,
                       requires_kneeling FROM exercises WHERE is_active ORDER BY exercise_id""")) {
            String texto = (r.get("name_es") + " . " + r.get("instructions_es")).toLowerCase(Locale.ROOT);
            for (var regla : REGLAS_TEXTO) {
                if (Boolean.TRUE.equals(r.get((String) regla[1]))) continue;
                var m = ((Pattern) regla[2]).matcher(texto);
                if (!m.find()) continue;
                String clave = r.get("exercise_id") + "\t" + regla[0];
                if (excepciones.contains(clave)) usadas.add(clave);
                else hallazgos.add(clave.replace('\t', ' ') + " «" + m.group() + "»");
            }
        }
        assertEquals(List.of(), hallazgos, "texto que pide una accion restringida sin la bandera correspondiente");
        var sobrantes = new TreeSet<>(excepciones);
        sobrantes.removeAll(usadas);
        assertEquals(Set.of(), sobrantes, "excepciones que ya no hacen falta: quitalas del archivo");
    }

    @Test
    @DisplayName("6 · sin nombres duplicados nuevos entre los activos")
    void sinDuplicadosNuevos() throws Exception {
        var conocidos = Files.readAllLines(DUPLICADOS, StandardCharsets.UTF_8).stream()
                .filter(l -> !l.isBlank() && !l.startsWith("nombre\t")).map(l -> l.split("\t")[0])
                .collect(Collectors.toCollection(TreeSet::new));
        var actuales = new TreeSet<>(jdbc.queryForList("""
                SELECT lower(name_es) FROM exercises WHERE is_active GROUP BY lower(name_es) HAVING count(*) > 1""",
                String.class));
        var nuevos = new TreeSet<>(actuales);
        nuevos.removeAll(conocidos);
        assertEquals(Set.of(), nuevos, "nombre_es repetido entre ejercicios activos");
    }

    static boolean vacio(Object o) {
        return o == null || o.toString().isBlank();
    }

    static Object campo(String nombre) throws Exception {
        var f = ImpactoPolicy.class.getDeclaredField(nombre);
        f.setAccessible(true);
        return f.get(null);
    }
}
