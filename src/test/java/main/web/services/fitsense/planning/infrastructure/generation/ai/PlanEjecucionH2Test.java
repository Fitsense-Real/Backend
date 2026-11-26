package main.web.services.fitsense.planning.infrastructure.generation.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class PlanEjecucionH2Test {

    static final List<String> CASOS = java.util.stream.IntStream.rangeClosed(1, 26)
            .mapToObj(i -> "G%02d".formatted(i)).toList();

    @TempDir Path dir;

    @Test
    void cadaPrediccionTieneUnaClaveUnica() {
        var plan = PlanEjecucionH2.plan(CASOS, 2, 0);
        assertEquals(52, plan.size(), "26 perfiles x 2 replicas");
        var claves = plan.stream().flatMap(p -> p.orden().stream().map(p::clave)).toList();
        assertEquals(104, claves.size());
        assertEquals(104, new HashSet<>(claves).size(), "perfil + replica + brazo, sin repetir");
        assertEquals("G01_r1_A", PlanEjecucionH2.clave("G01", 1, "A"));
        assertThrows(IllegalArgumentException.class, () -> PlanEjecucionH2.clave("G01", 0, "A"));
        assertThrows(IllegalArgumentException.class, () -> PlanEjecucionH2.clave("G01", 1, "C"));
    }

    @Test
    void elOrdenEstaEquilibrado() {
        var plan = PlanEjecucionH2.plan(CASOS, 2, 0);
        // Replica 1 en orden y replica 2 al reves.
        assertEquals("G01", plan.get(0).caso());
        assertEquals(1, plan.get(0).replica());
        assertEquals("G26", plan.get(26).caso());
        assertEquals(2, plan.get(26).replica());
        assertEquals("G01", plan.get(51).caso());
        // La replica 1 conserva el orden historico: impar A-B, par B-A.
        assertEquals(List.of("A", "B"), plan.get(0).orden());
        assertEquals(List.of("B", "A"), plan.get(1).orden());
        // En cada perfil, una pareja A-B y otra B-A.
        var porCaso = plan.stream().collect(Collectors.groupingBy(PlanEjecucionH2.Paso::caso));
        porCaso.forEach((c, l) -> {
            assertEquals(2, l.size(), c);
            assertNotEquals(l.get(0).orden(), l.get(1).orden(), c + ": orden invertido en la replica 2");
        });
        long aPrimero = plan.stream().filter(p -> p.orden().get(0).equals("A")).count();
        assertEquals(26, aPrimero, "la mitad de las parejas empiezan por A");
    }

    @Test
    void sePuedePartirPorReplicaSinCambiarElOrden() {
        var todo = PlanEjecucionH2.plan(CASOS, 2, 0);
        var r1 = PlanEjecucionH2.plan(CASOS, 2, 1);
        var r2 = PlanEjecucionH2.plan(CASOS, 2, 2);
        var unido = new ArrayList<>(r1);
        unido.addAll(r2);
        assertEquals(todo, unido);
    }

    @Test
    void laReanudacionNoRepiteLoYaRecibido() throws Exception {
        var reg = new PlanEjecucionH2.Registro(dir.resolve("registro_replicas.csv"), dir.resolve("salidas"));
        assertTrue(reg.hechas().isEmpty());
        reg.anotar("G01", 1, "A", "p1", 1000, "REAL", "{\"x\":1}");
        reg.anotar("G01", 1, "B", "p2", 1000, "REAL", "{\"x\":2}");
        var hechas = reg.hechas();
        assertEquals(Set.of("G01_r1_A", "G01_r1_B"), hechas.keySet());
        assertEquals("p2", hechas.get("G01_r1_B")[3]);
        // Una linea sin su salida no cuenta como hecha (se volveria a pedir; nunca se da por buena sin datos).
        Files.writeString(dir.resolve("registro_replicas.csv"), "G02;2;A;p3;1000;REAL\n",
                java.nio.file.StandardOpenOption.APPEND);
        assertFalse(reg.hechas().containsKey("G02_r2_A"));
        // Un plan reanudado solo pide lo que falta.
        var faltan = PlanEjecucionH2.plan(List.of("G01", "G02"), 2, 0).stream()
                .flatMap(p -> p.orden().stream().map(p::clave)).filter(k -> !reg.hechas().containsKey(k)).toList();
        assertEquals(6, faltan.size());
        assertFalse(faltan.contains("G01_r1_A"));
        assertFalse(faltan.contains("G01_r1_B"));
    }
}
