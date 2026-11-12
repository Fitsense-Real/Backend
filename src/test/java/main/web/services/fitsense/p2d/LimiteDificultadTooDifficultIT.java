package main.web.services.fitsense.p2d;

import main.web.services.fitsense.planning.domain.services.SafetySubstitutionPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VALIDACION FINAL DEL LIMITE DE DIFICULTAD · TOO_DIFFICULT DE EXTREMO A EXTREMO con el limite ENCENDIDO
 * ({@code fitsense.adaptation.difficulty-guard=on}, solo en esta prueba: por defecto sigue apagado). Flujo real de
 * adaptacion, V8C (H1 multiconjunto), estricta con 4D, V20, PostgreSQL efimero; solo la IA de correccion esta simulada.
 * <ul>
 *   <li>11/15: la IA propone un sustituto MAS DIFICIL del mismo patron: rechazado en los tres intentos con el motivo
 *       del limite; la semana se conserva (copia exacta);</li>
 *   <li>la IA propone sustitutos de dificultad menor o igual: se publica (el limite no bloquea lo valido);</li>
 *   <li>13: reduccion de dosis por debajo del minimo de duracion: el limite no la impide, V20 si;</li>
 *   <li>14: dosis fuera de la clase 4D del estructural: el limite no la impide, 4D si.</li>
 * </ul>
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        // Valores historicos fijados explicitamente al activar la configuracion evaluada (11/11/2026): esta
        // prueba se escribio sobre los valores por defecto anteriores y comprueba ese comportamiento.
        "fitsense.planning.policy=PLAN-HIST-1",
        "fitsense.ai.adaptation.prompt-version=v1",
        "fitsense.adaptation.progression.enabled=false",
        "fitsense.adaptation.pain-localized-removal=off",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.difficulty-guard=on"
})
@DisplayName("Limite de dificultad (encendido solo aqui) · TOO_DIFFICULT de extremo a extremo con IA simulada")
class LimiteDificultadTooDifficultIT extends ConservacionPatronH1Base {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    int dificultad(long id) {
        return jdbc.queryForObject("SELECT difficulty_level FROM exercises WHERE exercise_id = ?", Integer.class, id);
    }

    /** Sustitutos ELEGIBLES del mismo patron y zona, MAS DIFICILES que el original, fuera de la semana. */
    List<Long> masDificiles(Participante p, long ejercicio) {
        var elegibles = planComandos.prepare(new main.web.services.fitsense.planning.domain.model.commands
                        .GenerateWeeklyPlanCommand(p.userId(), SEMANA_MEDIDA, null, true, null, null))
                .context().availableExercises().stream().map(c -> c.exerciseId()).collect(Collectors.toSet());
        var fila = jdbc.queryForMap("SELECT movement_pattern, body_part_id, difficulty_level FROM exercises"
                + " WHERE exercise_id = ?", ejercicio);
        return jdbc.queryForList("SELECT exercise_id FROM exercises WHERE is_active AND movement_pattern = ?"
                                + " AND body_part_id = ? AND difficulty_level > ? ORDER BY exercise_id", Long.class,
                        fila.get("movement_pattern"), fila.get("body_part_id"), fila.get("difficulty_level")).stream()
                .filter(id -> !p.ids().contains(id)).filter(elegibles::contains).toList();
    }

    record CasoMayor(Caso c, long mayor) {}

    CasoMayor casoConMayor() {
        for (int i = 0; i < 6; i++) {
            var c = caso();
            var m = masDificiles(c.p(), c.a().id());
            if (!m.isEmpty()) return new CasoMayor(c, m.get(0));
        }
        throw new AssertionError("premisa: ningun caso con un sustituto mas dificil del patron de A");
    }

    @Test
    @DisplayName("configuracion: limite encendido (B), H1 multiconjunto intacto")
    void configuracion() {
        assertTrue(v8c.limitaDificultad());
        assertEquals(SafetySubstitutionPolicy.Estrategia.LIMITE, v8c.estrategiaSustitucion());
        assertEquals(main.web.services.fitsense.planning.domain.services.ConservationValidator.ReglaPatron.MULTICONJUNTO,
                v8c.reglaPatron());
    }

    @Test
    @DisplayName("11/15 la IA propone un sustituto mas dificil: rechazado 3 veces por el limite; la semana se conserva")
    void masDificilRechazadoYConservado() {
        var cm = casoConMayor();
        var c = cm.c();
        assertTrue(dificultad(cm.mayor()) > dificultad(c.a().id()));
        long nuevo = proponer(c, cm.mayor(), c.b().id())
                .orElseThrow(() -> new AssertionError("sin respaldo: " + intento(c.p())));
        var fb = feedbacks(3);
        for (int i = 1; i < 3; i++)
            assertTrue(fb.get(i).toString().contains("limite de dificultad"), fb.get(i).toString());
        assertEquals("CONSERVED", intento(c.p()).get("generation_source"));
        assertEquals(3, ((Number) intento(c.p()).get("ai_calls")).intValue());
        assertCopiaExacta(c.p(), nuevo);
        // RESPALDO conservado: rejection_reason queda NULL (no se rechazo la semana, se conservo); el motivo de cada
        // rechazo viaja en la retroalimentacion a la IA. Se deja constancia del informe persistido.
        System.out.println("TD-11-informe;" + jdbc.queryForObject("SELECT conservation_report::text FROM"
                + " plan_adaptation_attempts WHERE user_id = ? AND new_week_start_date = ?", String.class,
                c.p().userId(), SEMANA_NUEVA));
        System.out.printf("TD-11;usuario=%d;A=%d(dif %d);mayor=%d(dif %d);fuente=%s;llamadas=%s%n", c.p().userId(),
                c.a().id(), dificultad(c.a().id()), cm.mayor(), dificultad(cm.mayor()),
                intento(c.p()).get("generation_source"), intento(c.p()).get("ai_calls"));
    }

    @Test
    @DisplayName("sustitutos de dificultad menor o igual: se publican con el limite encendido")
    void menorOIgualPublicado() {
        Caso c = null;
        for (int i = 0; i < 6 && c == null; i++) {
            var x = caso();
            if (dificultad(x.a1()) <= dificultad(x.a().id()) && dificultad(x.b1()) <= dificultad(x.b().id())) c = x;
        }
        assertNotNull(c, "premisa: un caso con sustitutos no mas dificiles");
        final Caso k = c;
        long nuevo = proponer(k, k.a1(), k.b1()).orElseThrow(() -> new AssertionError("no publico: " + intento(k.p())));
        assertEquals("AI", intento(k.p()).get("generation_source"));
        assertEquals(1, ((Number) intento(k.p()).get("ai_calls")).intValue());
        var publicada = senaladaPublicada(k, nuevo);
        assertTrue(publicada.ejercicios().stream().anyMatch(e -> e.id() == k.a1()));
        assertNoSenaladasIntactas(k, nuevo);
    }

    /** La «IA» devuelve la sesion senalada con los MISMOS ejercicios y la dosis que diga {@code dosis}. */
    Optional<Long> proponerDosis(Caso c, java.util.function.UnaryOperator<Ejercicio> dosis) {
        org.mockito.Mockito.when(ia.interpretar(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> {
            main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext ctx = inv.getArgument(1);
            var ejercicios = c.sesion().ejercicios().stream().map(dosis).map(Ejercicio::borrador).toList();
            var fecha = c.sesion().fecha().plusWeeks(1);
            int minutos = estimador.estimateMinutes(new main.web.services.fitsense.planning.domain.model.valueobjects
                    .PlanDraft.DraftWorkout(fecha, main.web.services.fitsense.planning.domain.model.valueobjects
                    .WorkoutFocus.valueOf(c.sesion().enfoque()), "x", 0, ejercicios), ctx.prescription());
            return List.of(new main.web.services.fitsense.planning.domain.model.valueobjects.CorrectedSession(
                    c.sesion().id(), fecha, minutos, ejercicios, "correccion simulada"));
        });
        var orden = new main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment(
                List.of(main.web.services.fitsense.planning.domain.model.valueobjects.AdjustmentType.NONE), null, null,
                null, 0.0, 0.0, null, null, null, "TD", null, null,
                List.of(new main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment.TargetedSession(
                        c.sesion().id(), main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion
                        .VARIANTE_MAS_SENCILLA_O_CARGA)));
        return planning.adaptWeeklyPlan(c.p().userId(), SEMANA_NUEVA, orden, new java.math.BigDecimal("42.00"), null,
                new main.web.services.fitsense.planning.interfaces.acl.MeasuredWeekView(c.p().metrica(),
                        c.p().planMedido(), new java.math.BigDecimal("42.00"), new java.math.BigDecimal("100.00"),
                        main.web.services.fitsense.planning.domain.model.valueobjects.AdaptationDecision
                                .ADAPT_LOCALIZED.name()));
    }

    String motivos(List<?> fb) {
        var l = new ArrayList<String>();
        for (var f : fb) l.add(String.valueOf(f));
        return String.join(" || ", l);
    }

    @Test
    @DisplayName("13 reduccion de dosis por debajo del minimo de duracion: el limite no actua, V20 la bloquea")
    void v20Bloquea() {
        var c = caso();
        long nuevo = proponerDosis(c, e -> e.series() == null ? e
                : new Ejercicio(e.id(), e.tipo(), (short) 1, e.reps(), e.segundos(), (short) 30))
                .orElseThrow(() -> new AssertionError("sin respaldo: " + intento(c.p())));
        var fb = motivos(feedbacks(3));
        System.out.println("TD-13;" + intento(c.p()) + ";" + fb);
        assertTrue(fb.contains("V20"), fb);
        assertFalse(fb.contains("limite de dificultad"), fb);
        assertEquals("CONSERVED", intento(c.p()).get("generation_source"));
        assertCopiaExacta(c.p(), nuevo);
    }

    @Test
    @DisplayName("14 dosis del estructural fuera de su clase 4D (repeticiones 10 -> 3): el limite no actua, 4D la bloquea")
    void cuatroDBloquea() {
        var c = caso();
        long nuevo = proponerDosis(c, e -> e.id() == c.a().id()
                ? new Ejercicio(e.id(), e.tipo(), e.series(), (short) 3, e.segundos(), e.descanso()) : e)
                .orElseThrow(() -> new AssertionError("sin respaldo: " + intento(c.p())));
        var fb = motivos(feedbacks(3));
        System.out.println("TD-14;" + intento(c.p()) + ";" + fb);
        assertTrue(fb.contains("4D"), fb);
        assertFalse(fb.contains("limite de dificultad"), fb);
        assertEquals("CONSERVED", intento(c.p()).get("generation_source"));
        assertCopiaExacta(c.p(), nuevo);
    }

}
