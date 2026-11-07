package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.planning.application.internal.commandservices.AdaptedWeekCommandService;
import main.web.services.fitsense.planning.application.internal.commandservices.PlanningPolicySelector;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FASE 7 del cierre de PLAN-REV-2.0: TRANSICIONES ENTRE POLITICAS. Ciclo real sobre PostgreSQL efimero, sin Replicate.
 * <p>
 * Un cambio de configuracion ({@code fitsense.planning.policy}) se simula sustituyendo el selector del servicio de
 * planificacion entre dos ciclos, como haria un reinicio con otra configuracion. Lo que se comprueba:
 * <ul>
 *   <li>la politica que queda registrada en cada plan publicado ({@code weekly_training_plans.planning_policy}, V44);</li>
 *   <li>que la semana siguiente de un usuario en curso se valida con la politica de SU LINAJE, no con la de la
 *       configuracion (si no, un REV2 corto suspende V20 al 70 % con HIST y un HIST largo suspende V4 al 100 % o V27
 *       con REV2: el usuario se queda sin plan);</li>
 *   <li>que la configuracion sigue decidiendo la de los usuarios NUEVOS (generacion inicial);</li>
 *   <li>mantenimiento P-A1 -> cambio a HIST -> continuidad y recuperacion (L9);</li>
 *   <li>progresion previa -> interruptor apagado: la dosis con linaje se hereda y no hay pasos nuevos.</li>
 * </ul>
 * Hereda de {@link RedisenoFatigaGmIT} su material (y sus dos pruebas, que se repiten con REV2).
 */
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.planning.policy=PLAN-REV-2.0",
        "fitsense.planning.policy.allow-candidates=true",
        // Contexto propio: con las mismas propiedades que RedisenoFatigaGmIT, Spring reutilizaria su contexto en cache,
        // conectado a su contenedor ya detenido (el contenedor estatico se reinicia para esta clase).
        "fitsense.prueba.contexto=transiciones"
})
@DisplayName("Fase 7 · transiciones entre politicas (HIST <-> REV2, mantenimiento, interruptor de progresion)")
class TransicionesPoliticaIT extends RedisenoFatigaGmIT {

    @Autowired WeeklyTrainingPlanCommandService planService;
    @Autowired AdaptedWeekCommandService adaptacion;

    @AfterEach
    void restaurar() {
        configurar("PLAN-REV-2.0");
        progresion(false);
    }

    /** Lo que haria un reinicio con otra configuracion: otro selector para los planes que se generen despues. */
    void configurar(String politica) {
        Object objetivo = AopTestUtils.getTargetObject(planService);
        ReflectionTestUtils.setField(objetivo, "policySelector",
                new PlanningPolicySelector(politica, true));
    }

    void progresion(boolean encendida) {
        Object objetivo = AopTestUtils.getTargetObject(adaptacion);
        ReflectionTestUtils.setField(objetivo, "progresionHabilitada", encendida);
    }

    long participanteCon(GoalType objetivo, FitnessLevel nivel, TrainingLocation lugar, List<Short> dias, int minutos,
                         List<String> equipo) {
        String email = "transicion" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 3, 3), BiologicalSex.MALE,
                new BigDecimal("176"), new BigDecimal("78"), null, nivel, objetivo, "transicion", lugar,
                (short) dias.size(), dias, (short) minutos, equipo, List.of(), "Ninguna", true, true, true, true, true));
        return u;
    }

    /** n ciclos con todo hecho; devuelve las politicas de los planes publicados (o NO_FACTIBLE con el motivo). */
    List<String> ciclos(long u, long plan, int desde, int n) {
        var r = new ArrayList<String>();
        for (int k = desde; k < desde + n; k++) {
            ejecutar(u, plan, dias(semana(plan).size(), -1, Dia.HECHO));
            var p = ciclo.runFor(u, S1.plusWeeks(k), S1.plusWeeks(k + 1));
            if (p.isEmpty()) {
                r.add("SIN_PLAN:" + ultimoIntento(u).get("rejection_reason"));
                break;
            }
            plan = p.get();
            r.add(politica(plan));
        }
        return r;
    }

    static final List<String> GIMNASIO = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine",
            "bench", "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine");
    static final List<Short> CINCO = List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);

    @Test
    @DisplayName("HIST -> REV2: el usuario en curso sigue con HIST (su linaje); la semana sale; el nuevo usuario, REV2")
    void historicaARev2() throws Exception {
        configurar("PLAN-HIST-1");
        // GENERAL_FITNESS intermedio, 5 dias de 31 min: con HIST las sesiones pasan de M (tolerancia del 15 %).
        long u = participanteCon(GoalType.GENERAL_FITNESS, FitnessLevel.INTERMEDIATE, TrainingLocation.GYM, CINCO, 31,
                GIMNASIO);
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        assertEquals("PLAN-HIST-1", politica(w1));
        configurar("PLAN-REV-2.0");
        var politicas = ciclos(u, w1, 0, 3);
        System.out.println("TRANSICION hist->rev2 " + politicas);
        assertEquals(List.of("PLAN-HIST-1", "PLAN-HIST-1", "PLAN-HIST-1"), politicas,
                "el cambio de configuracion no reinterpreta ni deja sin plan al usuario en curso");
        assertEquals("PLAN-HIST-1", politica(w1), "el plan publicado conserva su politica");
        long nuevo = participanteCon(GoalType.GENERAL_FITNESS, FitnessLevel.INTERMEDIATE, TrainingLocation.GYM, CINCO,
                31, GIMNASIO);
        assertEquals("PLAN-REV-2.0", politica(planning.generateWeeklyPlan(nuevo, S1, PlanAdjustment.none(), null, null)
                .orElseThrow()), "la configuracion decide la de los usuarios nuevos");
        assertSinViolaciones();
    }

    @Test
    @DisplayName("P17: allow-candidates=false impide asignar REV2 a usuarios NUEVOS; los usuarios REV2 en curso siguen con su linaje")
    void p17CandidatasDesactivadas() throws Exception {
        // Usuario en curso con REV2 (configuracion candidata autorizada).
        long u = participanteCon(GoalType.INCREASE_STRENGTH, FitnessLevel.BEGINNER, TrainingLocation.HOME,
                List.of((short) 1, (short) 2, (short) 4, (short) 5), 64, List.of("body weight"));
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        assertEquals("PLAN-REV-2.0", politica(w1));
        // La configuracion vuelve a la historica SIN candidatas (lo que haria un reinicio con allow-candidates=false).
        Object objetivo = AopTestUtils.getTargetObject(planService);
        ReflectionTestUtils.setField(objetivo, "policySelector", new PlanningPolicySelector("PLAN-HIST-1", false));
        var politicas = ciclos(u, w1, 0, 3);
        System.out.println("TRANSICION P17 " + politicas);
        assertEquals(List.of("PLAN-REV-2.0", "PLAN-REV-2.0", "PLAN-REV-2.0"), politicas,
                "sin migrar ni bloquear: el usuario REV2 sigue con su linaje y no se queda sin plan");
        long nuevo = participanteCon(GoalType.INCREASE_STRENGTH, FitnessLevel.BEGINNER, TrainingLocation.HOME,
                List.of((short) 1, (short) 2, (short) 4, (short) 5), 64, List.of("body weight"));
        assertEquals("PLAN-HIST-1", politica(planning.generateWeeklyPlan(nuevo, S1, PlanAdjustment.none(), null, null)
                .orElseThrow()), "ninguna asignacion nueva de la candidata");
        assertThrows(IllegalStateException.class, () -> new PlanningPolicySelector("PLAN-REV-2.0", false),
                "configurar REV2 sin allow-candidates no arranca");
        assertSinViolaciones();
    }

    @Test
    @DisplayName("REV2 -> HIST: sesiones cortas (sin piso del 70 %) siguen con REV2; la semana sale")
    void rev2AHistorica() throws Exception {
        // INCREASE_STRENGTH principiante, casa, 4 dias de 64 min: con REV2 sesiones de ~19 min (< 70 % de M).
        long u = participanteCon(GoalType.INCREASE_STRENGTH, FitnessLevel.BEGINNER, TrainingLocation.HOME,
                List.of((short) 1, (short) 2, (short) 4, (short) 5), 64, List.of("body weight"));
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        assertEquals("PLAN-REV-2.0", politica(w1));
        configurar("PLAN-HIST-1");
        var politicas = ciclos(u, w1, 0, 3);
        System.out.println("TRANSICION rev2->hist " + politicas);
        assertEquals(List.of("PLAN-REV-2.0", "PLAN-REV-2.0", "PLAN-REV-2.0"), politicas,
                "con HIST, V20 rechazaria estas sesiones: el linaje decide");
        long nuevo = participanteCon(GoalType.INCREASE_STRENGTH, FitnessLevel.BEGINNER, TrainingLocation.HOME,
                List.of((short) 1, (short) 2, (short) 4, (short) 5), 64, List.of("body weight"));
        assertEquals("PLAN-HIST-1", politica(planning.generateWeeklyPlan(nuevo, S1, PlanAdjustment.none(), null, null)
                .orElseThrow()));
        assertSinViolaciones();
    }

    @Test
    @DisplayName("REV2 mantenimiento (P-A1) -> cambio a HIST -> continuidad y recuperacion gradual (L9)")
    void mantenimientoACambioHistorica() throws Exception {
        long u = participante();
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var referencia = semana(w1);
        int fatigada = referencia.size() - 1;
        ejecutar(u, w1, dias(referencia.size(), fatigada, Dia.FATIGA));
        long w2 = ciclo(u, 0);
        assertEquals("APLICADA", ultimoIntento(u).get("status"), String.valueOf(ultimoIntento(u)));
        assertFalse(reducidas(referencia.get(fatigada), semana(w2).get(fatigada)).isEmpty(), "premisa: mantenimiento");
        configurar("PLAN-HIST-1");
        var tipos = new ArrayList<String>();
        long plan = w2;
        for (int k = 1; k <= 6; k++) {
            ejecutar(u, plan, dias(referencia.size(), -1, Dia.HECHO));
            var p = ciclo.runFor(u, S1.plusWeeks(k), S1.plusWeeks(k + 1));
            assertTrue(p.isPresent(), "la copia en mantenimiento no se queda sin plan al cambiar a HIST: "
                    + ultimoIntento(u));
            plan = p.get();
            assertEquals("PLAN-REV-2.0", politica(plan));
            if (informe(ultimoIntento(u)).contains("\"tipo\":\"RECUPERACION\"")) tipos.add("RECUPERACION@" + (k + 2));
        }
        System.out.println("TRANSICION mantenimiento->hist pasos " + tipos);
        assertFalse(tipos.isEmpty(), "la recuperacion gradual sigue (REV2, progresion apagada)");
        assertSinViolaciones();
    }

    @Test
    @DisplayName("progresion previa -> interruptor apagado: la dosis progresada se hereda (HEREDADA) y no hay pasos nuevos")
    void progresionPreviaInterruptorApagado() throws Exception {
        progresion(true);
        long u = participanteCon(GoalType.INCREASE_STRENGTH, FitnessLevel.INTERMEDIATE, TrainingLocation.GYM,
                List.of((short) 1, (short) 3, (short) 5), 45, GIMNASIO);
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        int k = 0;
        boolean progreso = false;
        for (; k < 6 && !progreso; k++) {
            ejecutar(u, plan, dias(semana(plan).size(), -1, Dia.HECHO));
            plan = ciclo(u, k);
            progreso = !"{}".equals(ultimoIntento(u).get("progressed_sessions"));
        }
        assertTrue(progreso, "premisa: un paso de PROGRESION con la progresion encendida");
        var progresada = semana(plan);
        progresion(false);
        for (int j = 0; j < 3; j++, k++) {
            ejecutar(u, plan, dias(semana(plan).size(), -1, Dia.HECHO));
            plan = ciclo(u, k);
            var ev = ultimoIntento(u);
            assertEquals("{}", ev.get("progressed_sessions"), "apagada: sin pasos nuevos " + ev);
            assertEquals(progresada.stream().map(Sesion::ejercicios).toList(),
                    semana(plan).stream().map(Sesion::ejercicios).toList(), "la dosis con linaje se hereda tal cual");
        }
        System.out.println("TRANSICION progresion->apagada heredada=" + progresada.stream().map(Sesion::ejercicios).toList());
        assertSinViolaciones();
    }
}
