package main.web.services.fitsense.catalog;

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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Catalogo integral · V49: nombres e instrucciones que no describian el movimiento del GIF.
 * <ul>
 *   <li>cada UPDATE se aplica (el texto anterior coincidia) y deja exactamente el texto nuevo;</li>
 *   <li>la migracion solo toca name_es, instructions_es e instruction_steps_es;</li>
 *   <li>los pasos JSON y el texto plano dicen lo mismo;</li>
 *   <li>ningun nombre nuevo cambia el resultado de las reglas por nombre (ImpactoPolicy y la exclusion por
 *       «estiramiento»), salvo las excepciones documentadas.</li>
 * </ul>
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("Catalogo integral · V49 textos corregidos sin efecto en las reglas por nombre")
class CatalogoIntegralTextosIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired JdbcTemplate jdbc;

    static final Path V49 = Path.of("src/main/resources/db/migration/V49__catalogo_integral_textos.sql");
    /** 1093 «Empujar para correr» pierde «correr»: impacto por nombre MODERADO -> BAJO, coherente con el GIF. */
    static final Set<Long> CAMBIO_DE_IMPACTO_DOCUMENTADO = Set.of(1093L);

    record Fila(long id, String nombreNuevo, String nombreAnterior, String instrucciones, String pasos) {}

    static String lit(Matcher m, int g) {
        return m.group(g) == null ? null : m.group(g).replace("''", "'");
    }

    static List<Fila> filas() throws Exception {
        String sql = Files.readString(V49);
        var update = Pattern.compile("(?s)UPDATE exercises SET (.*?)\\n WHERE exercise_id = (\\d+)(.*?);\\n");
        var nombre = Pattern.compile("name_es = '((?:[^']|'')*)'");
        var instr = Pattern.compile("instructions_es = '((?:[^']|'')*)'");
        var pasos = Pattern.compile("instruction_steps_es = '((?:[^']|'')*)'::jsonb");
        var out = new ArrayList<Fila>();
        for (var m = update.matcher(sql); m.find(); ) {
            var set = m.group(1);
            var n = nombre.matcher(set);
            var i = instr.matcher(set);
            var p = pasos.matcher(set);
            var guardaNombre = nombre.matcher(m.group(3));
            out.add(new Fila(Long.parseLong(m.group(2)), n.find() ? lit(n, 1) : null,
                    guardaNombre.find() ? lit(guardaNombre, 1) : null, i.find() ? lit(i, 1) : null,
                    p.find() ? lit(p, 1) : null));
        }
        return out;
    }

    /**
     * Ids cuyo nombre o instrucciones vuelve a reescribir V52 (cierre del catalogo: segunda revision, verificacion
     * independiente y adjudicacion; detalle en docs/FitSense/catalogo_integral/cierre/adjudicacion/decisiones_v52.tsv).
     * Para ellos el texto final es el de V52, cuya exactitud fija la huella de CatalogoCalidadIT; aqui solo se comprueba
     * que V49 ya no es el ultimo texto. Para el resto, V49 sigue siendo el texto final y se exige exacto.
     */
    static java.util.Set<Long> reescritosPorV52(String campo) throws Exception {
        var sql = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/resources/db/migration/V52__catalogo_cierre_correcciones_verificadas.sql"));
        var ids = new java.util.TreeSet<Long>();
        var m = java.util.regex.Pattern.compile("(?m)^-- (\\d+) [^\\n]*?(?:: | \\| )" + campo + ":").matcher(sql);
        while (m.find()) ids.add(Long.parseLong(m.group(1)));
        return ids;
    }

    @Test
    @DisplayName("las 408 filas se aplican y dejan exactamente el texto nuevo (salvo lo que V52 reescribe despues)")
    void seAplicanTodas() throws Exception {
        var filas = filas();
        var nombresV52 = reescritosPorV52("name_es");
        var instruccionesV52 = reescritosPorV52("instructions_es");
        assertEquals(408, filas.size());
        int nombres = 0, instrucciones = 0;
        for (var f : filas) {
            var actual = jdbc.queryForMap("SELECT name_es, instructions_es, instruction_steps_es::text AS pasos"
                    + " FROM exercises WHERE exercise_id = ?", f.id());
            if (f.nombreNuevo() != null) {
                nombres++;
                if (nombresV52.contains(f.id())) assertNotEquals(f.nombreNuevo(), actual.get("name_es"), "V52 reescribe " + f.id());
                else assertEquals(f.nombreNuevo(), actual.get("name_es"), "nombre de " + f.id());
            }
            if (f.instrucciones() != null && instruccionesV52.contains(f.id())) {
                instrucciones++;
                assertNotEquals(f.instrucciones(), actual.get("instructions_es"), "V52 reescribe " + f.id());
            } else if (f.instrucciones() != null) {
                instrucciones++;
                // V50 cambia el ultimo paso del 1254 al pasar a estiramiento por tiempo (DURATION).
                String esperado = f.id() == 1254L ? f.instrucciones().replace("Repite el número de repeticiones indicado.",
                        "Mantén la duración indicada.") : f.instrucciones();
                assertEquals(esperado, actual.get("instructions_es"), "instrucciones de " + f.id());
                // El JSON de pasos y el texto plano dicen lo mismo.
                var pasos = jdbc.queryForList("SELECT jsonb_array_elements_text(instruction_steps_es)"
                        + " FROM exercises WHERE exercise_id = ?", String.class, f.id());
                assertEquals(esperado, String.join(" ", pasos), "pasos de " + f.id());
                assertTrue(pasos.size() >= 3 && pasos.size() <= 7, "pasos de " + f.id() + ": " + pasos.size());
            }
        }
        assertEquals(81, nombres);
        assertEquals(372, instrucciones);
    }

    @Test
    @DisplayName("solo name_es, instructions_es e instruction_steps_es; sin INSERT, DELETE ni cambios de esquema")
    void soloTextos() throws Exception {
        String sql = Files.readString(V49).replaceAll("--[^\n]*", "").replaceAll("'(?:[^']|'')*'", "''");
        var set = Pattern.compile("(?s)UPDATE exercises SET (.*?)\\n WHERE");
        int n = 0;
        for (var m = set.matcher(sql); m.find(); n++)
            for (var asignacion : m.group(1).split(","))
                assertTrue(asignacion.trim().matches("(name_es|instructions_es|instruction_steps_es) = ''(::jsonb)?"),
                        asignacion);
        assertEquals(408, n);
        assertFalse(sql.toLowerCase(Locale.ROOT).matches("(?s).*\\b(insert|delete|alter|drop|create)\\b.*"));
    }

    @Test
    @DisplayName("ningun nombre nuevo cambia ImpactoPolicy ni la exclusion por «estiramiento» (salvo 1093, documentado)")
    void reglasPorNombreIntactas() throws Exception {
        var moderado = (Pattern) campo("MODERADO");
        var alto = (Pattern) campo("ALTO_POR_NOMBRE");
        for (var f : filas()) {
            if (f.nombreNuevo() == null) continue;
            assertNotNull(f.nombreAnterior(), "sin guarda del nombre anterior: " + f.id());
            String antes = f.nombreAnterior().toLowerCase(Locale.ROOT), despues = f.nombreNuevo().toLowerCase(Locale.ROOT);
            assertEquals(antes.contains("estiramiento"), despues.contains("estiramiento"), "estiramiento: " + f.id());
            if (CAMBIO_DE_IMPACTO_DOCUMENTADO.contains(f.id())) continue;
            assertEquals(moderado.matcher(antes).find(), moderado.matcher(despues).find(), "impacto moderado: " + f.id());
            boolean altoImpacto = Boolean.TRUE.equals(jdbc.queryForObject(
                    "SELECT high_impact FROM exercises WHERE exercise_id = ?", Boolean.class, f.id()));
            // ALTO_POR_NOMBRE solo decide algo cuando el catalogo marca alto impacto y el nombre es moderado.
            if (altoImpacto && moderado.matcher(despues).find())
                assertEquals(alto.matcher(antes).find(), alto.matcher(despues).find(), "impacto alto: " + f.id());
        }
    }

    @Test
    @DisplayName("se conservan las advertencias de seguridad e indicaciones tecnicas del texto anterior")
    void advertenciasConservadas() {
        var esperadas = java.util.Map.of(
                308L, "Antes de cargar tu peso, comprueba que la espaldera está bien fijada a la pared",
                1072L, "Comprueba que el anclaje aguanta antes de cargar tu peso",
                144L, "rodillas alineadas", 1004L, "sin redondear los hombros", 827L, "detente al notar el estiramiento");
        esperadas.forEach((id, frase) -> assertTrue(jdbc.queryForObject(
                "SELECT instructions_es FROM exercises WHERE exercise_id = ?", String.class, id).contains(frase),
                id + " perdio: " + frase));
    }

    static Object campo(String nombre) throws Exception {
        var f = ImpactoPolicy.class.getDeclaredField(nombre);
        f.setAccessible(true);
        return f.get(null);
    }
}
