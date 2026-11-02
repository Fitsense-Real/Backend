package main.web.services.fitsense.planning.interfaces.rest.transform;

import main.web.services.fitsense.planning.domain.services.MetricaAerobica;
import main.web.services.fitsense.planning.interfaces.rest.resources.PlannedWorkoutResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Contrato de duracion estimada y metrica aerobica (opcionales, sin juicio ni promesas)")
class DuracionYAerobicaAssemblerTest {

    static final PlannedWorkoutResource W = new PlannedWorkoutResource(1L, LocalDate.of(2026, 11, 2), "FULL_BODY", "x",
            (short) 43, (short) 1, "PENDING", null, null, List.of(), false, null);

    static final List<String> PROHIBIDAS = List.of("optim", "suficiente", "anade", "agrega", "rellena", "incumpl",
            "perder", "garantiza", "todos", "complet", "admite", "maximo", "máximo", "óptim", "añade");

    @Test
    void motivos() {
        var c2d = DuracionYAerobicaAssembler.duracion(W, Map.of("segundos_estimados", 2550, "excepcion_c2d", true,
                "exenta_v8c", false), 75, "PLAN-C2D-0.1", null);
        assertEquals(75, c2d.availableMinutes());
        assertEquals(43, c2d.estimatedMinutes());
        assertEquals(32, c2d.differenceMinutes());
        assertEquals("DURACION_POR_CONTENIDO", c2d.reason());
        assertEquals("Tu rutina tiene una duración estimada de 43 minutos. Dispones de hasta 75 minutos para entrenar."
                + " La duración se calculó según los ejercicios disponibles y las restricciones de tu plan.",
                c2d.message(), "mensaje neutral: no afirma suficiencia ni completitud");
        var red = DuracionYAerobicaAssembler.duracion(W, Map.of("segundos_estimados", 1500, "excepcion_c2d", false,
                "exenta_v8c", true), 45, "PLAN-HIST-1", "REDUCE_VOLUME");
        assertEquals("REDUCCION_AUTORIZADA", red.reason());
        var aco = DuracionYAerobicaAssembler.duracion(W, Map.of("segundos_estimados", 1500, "excepcion_c2d", false,
                "exenta_v8c", true), 45, "PLAN-HIST-1", "ACORTAR_O_REORGANIZAR");
        assertEquals("SESION_ACORTADA", aco.reason());
        var normal = DuracionYAerobicaAssembler.duracion(W, Map.of("segundos_estimados", 2500, "excepcion_c2d", false,
                "exenta_v8c", false), 45, "PLAN-HIST-1", null);
        assertEquals("DENTRO_DEL_TIEMPO", normal.reason());
        var sin = DuracionYAerobicaAssembler.duracion(W, null, null, "PLAN-HIST-1", null);
        assertEquals("SIN_DATOS", sin.reason());
        assertNull(sin.availableMinutes(), "sin traza no se inventa la disponibilidad");
        for (var r : List.of(c2d, red, aco, normal, sin))
            for (var p : PROHIBIDAS) assertFalse(r.message().toLowerCase().contains(p), p + " en " + r.message());
    }

    @Test
    void aerobica() {
        var m = MetricaAerobica.programada(List.of(new MetricaAerobica.Bloque(3, 90, 60),
                new MetricaAerobica.Bloque(3, 120, 60)));
        assertEquals(10.5, m.minutosTrabajoProgramados(), 1e-9, "3x90 + 3x120 s");
        assertEquals(14.5, m.minutosBloqueProgramados(), 1e-9, "mas 4 pausas de 60 s");
        assertNull(m.minutosEjecutados());
        assertNull(m.minutosEquivalentes(), "intensidad desconocida: sin equivalentes");
        assertNull(m.conRegistro(10.0, 20.0, MetricaAerobica.Intensidad.DESCONOCIDA).minutosEquivalentes());
        assertEquals(60.0, m.conRegistro(10.0, 20.0, MetricaAerobica.Intensidad.VIGOROSA).minutosEquivalentes(), 1e-9);
        assertTrue(m.aviso().contains("11 minutos") || m.aviso().contains("10 minutos"),
                "el aviso usa el TRABAJO, no las pausas: " + m.aviso());
        for (var p : PROHIBIDAS) assertFalse(m.aviso().toLowerCase().contains(p), p);
    }
}
