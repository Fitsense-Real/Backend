package main.web.services.fitsense.planning;

import main.web.services.fitsense.planning.application.internal.queryservices.PlanLimitationsService;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.FrecuenciaSemanal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * S-MAX · CONTRATO DE DISPONIBILIDAD Y FRECUENCIA en el ciclo real (generacion + persistencia), con la politica que
 * este activa ({@code fitsense.planning.policy}; se ejecuta con HIST por defecto y con REV2 en la bateria de cierre).
 * Para los 4 objetivos x 3 niveles x 1-7 dias pedidos y tres patrones de disponibilidad (exactos, toda la semana y
 * dias no consecutivos):
 * <ul>
 *   <li>sesiones programadas = {@code min(dias pedidos, tope del nivel, dias disponibles)}: principiante <= 4, resto
 *       <= 6, nadie 7;</li>
 *   <li>todas en dias disponibles, dentro de la semana, una por dia;</li>
 *   <li>en dias consecutivos nunca el mismo enfoque ni dos cuerpos completos (descanso por zonas, V13/P1);</li>
 *   <li>/plan/limitations avisa (FREQUENCY S_MAX) exactamente cuando el tope recorta lo pedido.</li>
 * </ul>
 * Gimnasio y 45 min: en los barridos de 3.780 perfiles las 84 combinaciones de gimnasio y 45 min son validas con las
 * dos politicas, asi que aqui solo se mide la frecuencia.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("S-MAX: sesiones programadas = min(dias pedidos, tope del nivel, dias disponibles), por nivel y objetivo")
class FrecuenciaPorNivelIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired PlanningContextFacade planComandos;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanLimitationsService limitaciones;
    @Autowired JdbcTemplate jdbc;

    static final LocalDate SEMANA = LocalDate.of(2026, 11, 2);
    static final List<String> GYM = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine", "bench",
            "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine");
    static final List<Short> TODA = List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6,
            (short) 7);
    static final Map<Integer, List<Short>> EXACTOS = Map.of(
            1, List.of((short) 1), 2, List.of((short) 1, (short) 4), 3, List.of((short) 1, (short) 3, (short) 5),
            4, List.of((short) 1, (short) 2, (short) 4, (short) 5), 5, TODA.subList(0, 5), 6, TODA.subList(0, 6),
            7, TODA);
    static final List<Short> NO_CONSECUTIVOS = List.of((short) 1, (short) 3, (short) 5, (short) 7);

    int n;

    record Caso(GoalType objetivo, FitnessLevel nivel, int pedidos, String patron, List<Short> disponibles) {}

    static List<Caso> casos() {
        var r = new ArrayList<Caso>();
        for (var o : GoalType.values())
            for (var nv : FitnessLevel.values())
                for (int d = 1; d <= 7; d++) {
                    r.add(new Caso(o, nv, d, "EXACTOS", EXACTOS.get(d)));
                    r.add(new Caso(o, nv, d, "TODA_LA_SEMANA", TODA));
                    if (d <= 4) r.add(new Caso(o, nv, d, "NO_CONSECUTIVOS", NO_CONSECUTIVOS));
                }
        return r;
    }

    @Test
    void contrato() {
        var fallos = new ArrayList<String>();
        int generados = 0, pegados = 0;
        var resumen = new TreeMap<String, Integer>();
        for (var c : casos()) {
            n++;
            String email = "frecuencia" + n + "@pruebas.invalid";
            jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                    + " VALUES (?, 'x', 'Frecuencia', now(), now())", email);
            long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
            perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 6, 15), BiologicalSex.FEMALE,
                    new BigDecimal("168"), new BigDecimal("68"), null, c.nivel(), c.objetivo(), "frecuencia",
                    TrainingLocation.GYM, (short) c.pedidos(), c.disponibles(), (short) 45, GYM, List.of(), "Ninguna",
                    true, true, true, true, true));
            String id = c.objetivo() + "/" + c.nivel() + "/" + c.pedidos() + "/" + c.patron();
            var plan = planComandos.generateWeeklyPlan(u, SEMANA, PlanAdjustment.none(), null, null);
            if (plan.isEmpty()) {
                fallos.add(id + ": sin plan");
                continue;
            }
            generados++;
            var fechas = jdbc.queryForList("SELECT scheduled_date FROM planned_workouts WHERE plan_id = ?"
                    + " ORDER BY scheduled_date", java.sql.Date.class, plan.get()).stream()
                    .map(java.sql.Date::toLocalDate).toList();
            int esperadas = FrecuenciaSemanal.planificadas(c.pedidos(), c.nivel().name(), c.disponibles().size());
            int tope = c.nivel() == FitnessLevel.BEGINNER ? 4 : 6;
            if (fechas.size() != esperadas)
                fallos.add(id + ": " + fechas.size() + " sesiones, esperadas " + esperadas);
            if (fechas.size() > tope) fallos.add(id + ": supera el tope del nivel (" + tope + ")");
            if (fechas.size() >= 7) fallos.add(id + ": 7 sesiones");
            if (new HashSet<>(fechas).size() != fechas.size()) fallos.add(id + ": dos sesiones el mismo dia");
            for (var f : fechas) {
                if (f.isBefore(SEMANA) || f.isAfter(SEMANA.plusDays(6))) fallos.add(id + ": " + f + " fuera de la semana");
                if (!c.disponibles().contains((short) f.getDayOfWeek().getValue()))
                    fallos.add(id + ": " + f + " no es un dia disponible");
            }
            // Descanso entre sesiones: en dias consecutivos nunca el mismo enfoque (V13/P1). La eleccion de fechas no
            // garantiza dias separados (4 de 7 disponibles -> lunes, miercoles, viernes y sabado): la recuperacion la
            // garantiza la alternancia de enfoques. Se registra cuantas semanas tienen dias pegados.
            var enfoques = jdbc.queryForList("SELECT focus_code FROM planned_workouts WHERE plan_id = ?"
                    + " ORDER BY scheduled_date", String.class, plan.get());
            for (int i = 1; i < fechas.size(); i++)
                if (fechas.get(i - 1).plusDays(1).equals(fechas.get(i))) {
                    pegados++;
                    if (enfoques.get(i - 1).equals(enfoques.get(i)) && !"FULL_BODY".equals(enfoques.get(i)))
                        fallos.add(id + ": mismo enfoque " + enfoques.get(i) + " en dias consecutivos " + fechas.get(i));
                    if ("FULL_BODY".equals(enfoques.get(i - 1)) && "FULL_BODY".equals(enfoques.get(i)))
                        fallos.add(id + ": cuerpo completo dos dias seguidos " + fechas.get(i));
                }
            boolean aviso = limitaciones.limitationsFor(u).limitations().stream()
                    .anyMatch(l -> "FREQUENCY".equals(l.type()) && "S_MAX".equals(l.code()));
            if (aviso != (c.pedidos() > tope))
                fallos.add(id + ": aviso de frecuencia " + aviso + " y el tope " + (c.pedidos() > tope ? "recorta" : "no recorta"));
            resumen.merge(c.nivel() + " pedidos=" + c.pedidos() + " -> " + fechas.size(), 1, Integer::sum);
        }
        System.out.println("FRECUENCIA casos=" + n + " generados=" + generados + " pares_de_dias_pegados=" + pegados + " " + resumen);
        assertTrue(fallos.isEmpty(), fallos.size() + " incumplimientos: " + fallos);
        assertEquals(casos().size(), generados);
    }
}
