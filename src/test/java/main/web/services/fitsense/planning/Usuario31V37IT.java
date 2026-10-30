package main.web.services.fitsense.planning;

import main.web.services.fitsense.EsquemaVigenteSobreCatalogoAnterior;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Usuario 31 con el catalogo vigente (V37): mismo perfil, sin relajar ninguna
 * restriccion. 776 esta desactivado y 1320 marcado como de suelo; los tres
 * rechazos de Replicate se repiten igual (son de seleccion de la IA), el plan 63
 * ya no es publicable y el motor de reglas publica un plan sin ningun ejercicio
 * que el perfil excluye segun la revision visual (bloque A de correcciones_v37.tsv).
 * <p>
 * Desde el 30/10/2026 V38 y V39 son oficiales: esta prueba es evidencia del catalogo V37 y
 * se fija en {@code spring.flyway.target=37} (con V39, 301 exige espaldera y deja de ser elegible).
 */
@Testcontainers
@Import(EsquemaVigenteSobreCatalogoAnterior.class)
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "spring.flyway.target=37"
})
class Usuario31V37IT extends Usuario31Base {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    @DisplayName("V37: mismos rechazos de la IA; plan 63 rechazado por 776 y 1320; el motor publica sin prohibidos")
    void catalogoV37() throws Exception {
        var r = informe("v37");
        assertEquals("v37 elegibles: 776=NO 1320=NO 1185=si 309=si 1103=si 301=si", r.get(0));
        rechazosDeLaIa(r, 11);
        contiene(r.get(4), "V1: el ejercicio 776 no esta en available_exercises",
                "V1: el ejercicio 1320 no esta en available_exercises");
        assertTrue(r.get(5).startsWith("v37 RULE_ENGINE: | SATURDAY UPPER_BODY"), r.get(5));
        // Nada de lo que este perfil excluye (suelo, suspension, impacto) segun la revision visual.
        var prohibidos = new java.util.HashSet<String>();
        for (var l : java.nio.file.Files.readAllLines(java.nio.file.Path.of(
                "docs/FitSense/auditoria_catalogo/v37/correcciones_v37.tsv"))) {
            var c = l.split("\t");
            if (c[3].startsWith("A") && c[6].equals("true")
                    && (c[4].equals("requires_floor") || c[4].equals("requires_hanging")))
                prohibidos.add(c[0]);
        }
        for (var id : r.get(5).replaceAll("\\[[^]]*]", "").split("[ |]+"))
            assertFalse(prohibidos.contains(id), "el plan incluye " + id + ": " + r.get(5));
    }
}
