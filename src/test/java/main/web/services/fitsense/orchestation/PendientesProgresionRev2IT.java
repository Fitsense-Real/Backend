package main.web.services.fitsense.orchestation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * La bateria de {@link PendientesProgresionIT} con {@code PLAN-REV-2.0} (cierre tecnico, fase 3). Todas las pruebas
 * heredadas se ejecutan tal cual con REV2; solo se sustituye la que tiene una PREMISA EXCLUSIVAMENTE HISTORICA.
 * <p>
 * {@link #sinProgresionNoReincorpora}: la historica supone que un LOSE_WEIGHT principiante de 30 min «no puede dar
 * ningun paso» tras acortar, porque con el piso del 70 % de V20 sus sesiones van al limite de V4. Con REV2 (duracion por
 * dosis, sin piso) hay margen y la progresion encendida da pasos PROGRESION legitimos. La propiedad que la prueba
 * protege sigue siendo la misma y se comprueba aqui: no se reincorpora sin una PROGRESION previa en esa sesion
 * (regla 1(a)), ningun paso pasa del techo (1,10 x base) y la sesion acortada no recupera ejercicios por otra via.
 */
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.progression.enabled=true",
        "fitsense.planning.policy=PLAN-REV-2.0",
        "fitsense.planning.policy.allow-candidates=true"
})
@DisplayName("REV2 · Pendientes de la progresion: recuperacion de series, carga y ciclos simultaneos")
class PendientesProgresionRev2IT extends PendientesProgresionIT {

    static final ObjectMapper JSON = new ObjectMapper();

    @Override
    @org.junit.jupiter.params.ParameterizedTest(name = "{0}")
    @org.junit.jupiter.params.provider.ValueSource(strings = {"GYM", "CASA"})
    @DisplayName("REV2 1(a): LOSE_WEIGHT / BEGINNER / 30 tras acortar: pasos PROGRESION dentro del techo; no reincorpora"
            + " sin PROGRESION previa; el acortado no recupera ejercicios por otra via")
    void sinProgresionNoReincorpora(String lugar) throws Exception {
        long u = participante(GoalType.LOSE_WEIGHT, FitnessLevel.BEGINNER, 30, lugar);
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var antes = semana(plan).get(2).getValue();
        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.TIEMPO);
        plan = ciclo(u, 0, -1);
        assertEquals("APLICADA", ultimo(u).get("status"), String.valueOf(ultimo(u)));
        var acortado = semana(plan).get(2).getValue();
        var idsAcortado = acortado.stream().map(Ej::id).collect(Collectors.toSet());
        var retirados = antes.stream().map(Ej::id).filter(id -> !idsAcortado.contains(id)).collect(Collectors.toSet());
        var traza = new ArrayList<String>();
        boolean huboProgresion = false;
        for (int k = 1; k <= 8; k++) {
            ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.HECHO);
            plan = ciclo(u, k, 0);
            var ev = ultimo(u);
            assertEquals("SIN_ORDEN", ev.get("status"));
            JsonNode pasos = JSON.readTree(String.valueOf(ev.get("conservation_report"))).path("pasos");
            for (var p : pasos) {
                String tipo = p.path("tipo").asText();
                // Con la progresion encendida y todo cumplido solo caben PROGRESION y, tras ella, REINCORPORACION.
                assertTrue(Set.of("PROGRESION", "REINCORPORACION").contains(tipo), "paso inesperado: " + p);
                if ("REINCORPORACION".equals(tipo))
                    assertTrue(huboProgresion, "1(a): no se reincorpora sin una PROGRESION posterior al acortado: " + traza);
                if ("PROGRESION".equals(tipo)) huboProgresion = true;
                assertTrue(p.path("volumen_despues").asInt() <= p.path("techo").asInt(), "techo superado: " + p);
            }
            traza.add("s" + (k + 2) + " " + ev.get("progressed_sessions"));
        }
        System.out.println("PENDIENTE_REV2 sin_progresion " + lugar + " " + traza);
        var viernes = semana(plan).get(2).getValue().stream().map(Ej::id).collect(Collectors.toSet());
        assertTrue(viernes.containsAll(idsAcortado), "no se pierde ningun ejercicio del acortado: " + viernes);
        var nuevos = new HashSet<>(viernes);
        nuevos.removeAll(idsAcortado);
        assertTrue(retirados.containsAll(nuevos), "solo podria volver lo retirado (por REINCORPORACION): " + nuevos);
        assertSinViolaciones();
    }
}
