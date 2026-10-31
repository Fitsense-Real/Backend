package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.AdjustmentType;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.DerivedWorkout;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.PreviousWeekSummary;
import main.web.services.fitsense.planning.domain.model.valueobjects.WeekDerivation;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V8C, una prueba por fila de la tabla aprobada.
 *
 * <pre>
 * Sesion no senalada           conservar ejercicios y dosis
 * Senalada por horario         reprogramar sin sustituciones
 * Senalada por dificultad      sustitucion valida del mismo patron
 * Senalada por dolor           el ejercicio que dolio no reaparece
 * Senalada sin cambios         permitida, registrada como sin efecto
 * </pre>
 */
class ConservationValidatorTest {

    private static final LocalDate L1 = LocalDate.of(2026, 10, 12);   // lunes, semana medida
    private static final LocalDate L2 = LocalDate.of(2026, 10, 19);   // lunes, semana nueva

    private final ConservationValidator validator = new ConservationValidator();

    // Catalogo: 740 y 304 comparten patron; 592 y 1101 son de otros patrones.
    private static final Map<Long, CandidateExercise> CATALOGO = catalogo();

    // =======================================================================
    // Sin adaptacion no hay nada que conservar
    // =======================================================================

    @Test
    @DisplayName("Sin sesiones senaladas la validacion no opina: solo se adapta con baja adherencia")
    void sinSenaladasNoOpina() {
        var r = validator.validate(derivarTodoIgual(), semanaDeTres(),
                PlanAdjustment.none(), CATALOGO);

        assertTrue(r.aceptable());
        assertTrue(r.aplicadas().isEmpty());
        assertTrue(r.sinEfecto().isEmpty());
        assertTrue(r.conservadas().isEmpty(), "sin orden de adaptacion no se cuenta nada");
    }

    // =======================================================================
    // Los dos mecanismos no coexisten
    // =======================================================================

    @Test
    @DisplayName("Una orden con objetivo porcentual Y sesiones senaladas no se puede construir")
    void losDosMecanismosNoCoexisten() {
        var e = assertThrows(IllegalArgumentException.class, () -> new PlanAdjustment(
                List.of(AdjustmentType.REDUCE_VOLUME), 400, 380, 420, -20.0, 0.0,
                null, null, null, "baja adherencia", null, null,
                List.of(new PlanAdjustment.TargetedSession(102L,
                        TipoIntervencion.ACORTAR_O_REORGANIZAR))));

        assertTrue(e.getMessage().contains("No se pueden combinar ajustes porcentuales"),
                () -> e.getMessage());
    }

    @Test
    @DisplayName("Cada mecanismo por separado si se puede construir")
    void cadaMecanismoPorSeparado() {
        // Solo porcentual: el camino antiguo, que sigue valiendo.
        var soloPorcentual = new PlanAdjustment(List.of(AdjustmentType.REDUCE_VOLUME),
                400, 380, 420, -20.0, 0.0, null, null, null, "baja adherencia", null, null,
                List.of());
        assertTrue(soloPorcentual.isActive());
        assertFalse(soloPorcentual.adapta());

        // Solo localizada.
        var soloLocalizada = senalando(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR);
        assertFalse(soloLocalizada.isActive());
        assertTrue(soloLocalizada.adapta());

        // Y ninguno.
        assertFalse(PlanAdjustment.none().isActive());
        assertFalse(PlanAdjustment.none().adapta());
    }

    @Test
    @DisplayName("El constructor de doce componentes sigue compilando y no senala nada")
    void constructorDeDoceSigueValiendo() {
        // Es el que usa WeeklyCycleService. Si dejara de valer, habria que tocarlo.
        var orden = new PlanAdjustment(List.of(AdjustmentType.REDUCE_VOLUME),
                400, 380, 420, -20.0, 0.0, null, null, null, "baja adherencia", null, null);

        assertTrue(orden.isActive());
        assertFalse(orden.adapta());
        assertTrue(orden.targetedSessions().isEmpty());
    }

    // =======================================================================
    // Se ordeno adaptar y faltan los datos: error, no visto bueno
    // =======================================================================

    @Test
    @DisplayName("Adaptar sin la semana medida: error, no informe vacio")
    void adaptarSinSemanaMedida() {
        var r = validator.validate(derivarTodoIgual(), null,
                senalando(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR), CATALOGO);

        assertFalse(r.aceptable(), "no validarla no es aprobarla");
        assertTrue(unico(r.problems(), "falta").contains("la semana medida"),
                () -> r.problems().toString());
    }

    @Test
    @DisplayName("Adaptar sin derivacion: error")
    void adaptarSinDerivacion() {
        var r = validator.validate(null, semanaDeTres(),
                senalando(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR), CATALOGO);

        assertFalse(r.aceptable());
        assertTrue(unico(r.problems(), "falta").contains("la derivacion"),
                () -> r.problems().toString());
    }

    @Test
    @DisplayName("Adaptar con la semana medida vacia: error")
    void adaptarConSemanaMedidaVacia() {
        // PreviousWeekSummary.empty() es el caso de la primera semana, donde no
        // hay adherencia medida y por tanto no deberia haber adaptacion.
        var r = validator.validate(derivarTodoIgual(), PreviousWeekSummary.empty(),
                senalando(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR), CATALOGO);

        assertFalse(r.aceptable());
        assertTrue(unico(r.problems(), "falta").contains("la semana medida"),
                () -> r.problems().toString());
    }

    // =======================================================================
    // Cambiar de dia exige permiso explicito
    // =======================================================================

    @Test
    @DisplayName("Causa no registrada: mover el dia se rechaza, no hay causa que lo justifique")
    void causaNoRegistradaNoMueveElDia() {
        var derivacion = derivar(Map.of(
                101L, sesion(L2, 45, ej(740L, 3, 10, 90), ej(1101L, 2, 13, 90)),
                102L, sesion(L2.plusDays(3), 45, ej(304L, 3, 10, 90)),
                103L, sesion(L2.plusDays(4), 45, ej(592L, 3, 10, 90))));

        var r = validator.validate(derivacion, semanaDeTres(),
                senalando(102L, TipoIntervencion.CAUSA_NO_REGISTRADA), CATALOGO);

        assertFalse(r.aceptable());
        assertTrue(unico(r.problems(), "no autoriza cambiar el dia").contains("de WEDNESDAY a THURSDAY"),
                () -> r.problems().toString());
        assertTrue(r.aplicadas().isEmpty(), "no puede contar como adaptacion aplicada");
    }

    @Test
    @DisplayName("Dificultad: mover el dia tampoco, el dia no era el problema")
    void dificultadNoMueveElDia() {
        var derivacion = derivar(Map.of(
                101L, sesion(L2, 45, ej(740L, 3, 10, 90), ej(1101L, 2, 13, 90)),
                102L, sesion(L2.plusDays(3), 45, ej(740L, 3, 10, 90)),
                103L, sesion(L2.plusDays(4), 45, ej(592L, 3, 10, 90))));

        var r = validator.validate(derivacion, semanaDeTres(),
                senalando(102L, TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA), CATALOGO);

        assertFalse(r.aceptable());
        assertTrue(unico(r.problems(), "no autoriza cambiar el dia").contains("102"),
                () -> r.problems().toString());
    }

    @Test
    @DisplayName("Falta de tiempo: acortar en su mismo dia, sin moverlo")
    void faltaDeTiempoNoMueveElDia() {
        var derivacion = derivar(Map.of(
                101L, sesion(L2, 45, ej(740L, 3, 10, 90), ej(1101L, 2, 13, 90)),
                102L, sesion(L2.plusDays(3), 30, ej(304L, 2, 10, 90)),
                103L, sesion(L2.plusDays(4), 45, ej(592L, 3, 10, 90))));

        var r = validator.validate(derivacion, semanaDeTres(),
                senalando(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR), CATALOGO);

        assertFalse(r.aceptable());
        assertTrue(unico(r.problems(), "no autoriza cambiar el dia").contains("102"),
                "acortar es una cosa y reprogramar otra: " + r.problems());
    }

    // =======================================================================
    // Fila 1 · sesion no senalada: conservar ejercicios y dosis
    // =======================================================================

    @Test
    @DisplayName("No senalada e identica: conservada")
    void noSenaladaIdentica() {
        var r = validator.validate(derivarTodoIgual(), semanaDeTres(),
                senalando(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR), CATALOGO);

        assertTrue(r.aceptable(), () -> "no deberia haber problemas: " + r.problems());
        assertEquals(List.of(101L, 103L), r.conservadas());
    }

    @Test
    @DisplayName("No senalada con un ejercicio anadido: rechazada")
    void noSenaladaConEjercicioAnadido() {
        var derivacion = derivar(Map.of(
                101L, sesion(L2, 45, ej(740L, 3, 10, 90), ej(1101L, 2, 13, 90), ej(592L, 3, 10, 90)),
                102L, sesion(L2.plusDays(2), 45, ej(304L, 3, 10, 90)),
                103L, sesion(L2.plusDays(4), 45, ej(592L, 3, 10, 90))));

        var r = validator.validate(derivacion, semanaDeTres(),
                senalando(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR), CATALOGO);

        assertFalse(r.aceptable());
        assertTrue(unico(r.problems(), "101").contains("aparecen los ejercicios [592]"),
                () -> r.problems().toString());
    }

    @Test
    @DisplayName("No senalada con la dosis cambiada: rechazada, descanso incluido")
    void noSenaladaConDosisCambiada() {
        var derivacion = derivar(Map.of(
                101L, sesion(L2, 45, ej(740L, 3, 10, 120), ej(1101L, 2, 13, 90)),
                102L, sesion(L2.plusDays(2), 45, ej(304L, 3, 10, 90)),
                103L, sesion(L2.plusDays(4), 45, ej(592L, 3, 10, 90))));

        var r = validator.validate(derivacion, semanaDeTres(),
                senalando(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR), CATALOGO);

        assertFalse(r.aceptable());
        assertTrue(unico(r.problems(), "101").contains("cambia la dosis de [740]"),
                "el descanso es parte de la dosis: " + r.problems());
    }

    @Test
    @DisplayName("No senalada movida de dia: rechazada")
    void noSenaladaMovidaDeDia() {
        var derivacion = derivar(Map.of(
                101L, sesion(L2.plusDays(1), 45, ej(740L, 3, 10, 90), ej(1101L, 2, 13, 90)),
                102L, sesion(L2.plusDays(2), 45, ej(304L, 3, 10, 90)),
                103L, sesion(L2.plusDays(4), 45, ej(592L, 3, 10, 90))));

        var r = validator.validate(derivacion, semanaDeTres(),
                senalando(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR), CATALOGO);

        assertFalse(r.aceptable());
        assertTrue(unico(r.problems(), "101").contains("cambio de MONDAY a TUESDAY"),
                () -> r.problems().toString());
    }

    // =======================================================================
    // Fila 2 · senalada por horario: reprogramar, sin sustituciones
    // =======================================================================

    @Test
    @DisplayName("Horario: mover el dia esta permitido y cuenta como adaptacion aplicada")
    void horarioMoverElDia() {
        var derivacion = derivar(Map.of(
                101L, sesion(L2, 45, ej(740L, 3, 10, 90), ej(1101L, 2, 13, 90)),
                102L, sesion(L2.plusDays(3), 45, ej(304L, 3, 10, 90)),
                103L, sesion(L2.plusDays(4), 45, ej(592L, 3, 10, 90))));

        var r = validator.validate(derivacion, semanaDeTres(),
                senalando(102L, TipoIntervencion.REPROGRAMAR_DIAS), CATALOGO);

        assertTrue(r.aceptable(), () -> r.problems().toString());
        assertEquals(List.of(102L), r.aplicadas());
        assertEquals(List.of(101L, 103L), r.conservadas());
    }

    @Test
    @DisplayName("Horario: sustituir un ejercicio NO esta permitido")
    void horarioNoSustituye() {
        var derivacion = derivar(Map.of(
                101L, sesion(L2, 45, ej(740L, 3, 10, 90), ej(1101L, 2, 13, 90)),
                102L, sesion(L2.plusDays(3), 45, ej(740L, 3, 10, 90)),
                103L, sesion(L2.plusDays(4), 45, ej(592L, 3, 10, 90))));

        var r = validator.validate(derivacion, semanaDeTres(),
                senalando(102L, TipoIntervencion.REPROGRAMAR_DIAS), CATALOGO);

        assertFalse(r.aceptable());
        assertTrue(unico(r.problems(), "102").contains("no autoriza sustituir"),
                "el problema era el horario, no los ejercicios: " + r.problems());
    }

    @Test
    @DisplayName("Horario: acortar la sesion tampoco, porque nadie lo pidio")
    void horarioNoAcorta() {
        var derivacion = derivar(Map.of(
                101L, sesion(L2, 45, ej(740L, 3, 10, 90), ej(1101L, 2, 13, 90)),
                102L, sesion(L2.plusDays(3), 45, ej(304L, 2, 10, 90)),
                103L, sesion(L2.plusDays(4), 45, ej(592L, 3, 10, 90))));

        var r = validator.validate(derivacion, semanaDeTres(),
                senalando(102L, TipoIntervencion.REPROGRAMAR_DIAS), CATALOGO);

        assertFalse(r.aceptable());
        assertTrue(unico(r.problems(), "102").contains("no autoriza cambiar la dosis"),
                () -> r.problems().toString());
    }

    @Test
    @DisplayName("Falta de tiempo: acortar SI esta permitido")
    void faltaDeTiempoAcorta() {
        var derivacion = derivar(Map.of(
                101L, sesion(L2, 45, ej(740L, 3, 10, 90), ej(1101L, 2, 13, 90)),
                102L, sesion(L2.plusDays(2), 30, ej(304L, 2, 10, 90)),
                103L, sesion(L2.plusDays(4), 45, ej(592L, 3, 10, 90))));

        var r = validator.validate(derivacion, semanaDeTres(),
                senalando(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR), CATALOGO);

        assertTrue(r.aceptable(), () -> r.problems().toString());
        assertEquals(List.of(102L), r.aplicadas());
    }

    // =======================================================================
    // Fila 3 · senalada por dificultad: sustitucion del mismo patron
    // =======================================================================

    @Test
    @DisplayName("Dificultad: sustituir por el mismo patron de movimiento esta permitido")
    void dificultadMismoPatron() {
        // 304 (HORIZONTAL_PUSH) por 740 (HORIZONTAL_PUSH).
        var derivacion = derivar(Map.of(
                101L, sesion(L2, 45, ej(740L, 3, 10, 90), ej(1101L, 2, 13, 90)),
                102L, sesion(L2.plusDays(2), 45, ej(740L, 3, 10, 90)),
                103L, sesion(L2.plusDays(4), 45, ej(592L, 3, 10, 90))));

        var r = validator.validate(derivacion, semanaDeTres(),
                senalando(102L, TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA), CATALOGO);

        assertTrue(r.aceptable(), () -> r.problems().toString());
        assertEquals(List.of(102L), r.aplicadas());
    }

    @Test
    @DisplayName("Dificultad: sustituir por OTRO patron se rechaza")
    void dificultadOtroPatron() {
        // 592 es KNEE_DOMINANT y el retirado 304 es HORIZONTAL_PUSH.
        var derivacion = derivar(Map.of(
                101L, sesion(L2, 45, ej(740L, 3, 10, 90), ej(1101L, 2, 13, 90)),
                102L, sesion(L2.plusDays(2), 45, ej(592L, 3, 10, 90)),
                103L, sesion(L2.plusDays(4), 45, ej(592L, 3, 10, 90))));

        var r = validator.validate(derivacion, semanaDeTres(),
                senalando(102L, TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA), CATALOGO);

        assertFalse(r.aceptable());
        assertTrue(unico(r.problems(), "102").contains("mismo patron de movimiento"),
                () -> r.problems().toString());
    }

    @Test
    @DisplayName("Dificultad: anadir sin retirar no es una sustitucion")
    void dificultadAnadeSinRetirar() {
        var derivacion = derivar(Map.of(
                101L, sesion(L2, 45, ej(740L, 3, 10, 90), ej(1101L, 2, 13, 90)),
                102L, sesion(L2.plusDays(2), 45, ej(304L, 3, 10, 90), ej(740L, 3, 10, 90)),
                103L, sesion(L2.plusDays(4), 45, ej(592L, 3, 10, 90))));

        var r = validator.validate(derivacion, semanaDeTres(),
                senalando(102L, TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA), CATALOGO);

        assertFalse(r.aceptable());
        assertTrue(unico(r.problems(), "102").contains("sin retirar ninguno"),
                () -> r.problems().toString());
    }

    // =======================================================================
    // Fila 4 · senalada por dolor
    // =======================================================================

    @Test
    @DisplayName("Dolor: volver a prescribir el ejercicio que dolio se rechaza")
    void dolorNoRepropone() {
        // En la semana medida el 304 se marco con PAIN_OR_DISCOMFORT.
        var derivacion = derivarTodoIgual();

        var r = validator.validate(derivacion, semanaConDolorEn(304L),
                senalando(102L, TipoIntervencion.SEGURIDAD_ANTE_DOLOR), CATALOGO);

        assertFalse(r.aceptable());
        assertTrue(unico(r.problems(), "102").contains("vuelve a prescribir [304]"),
                () -> r.problems().toString());
        assertTrue(r.aplicadas().isEmpty(), "no se cuenta como adaptacion");
        assertTrue(r.sinEfecto().isEmpty(), "ni como sin efecto: esta rechazada");
    }

    @Test
    @DisplayName("Dolor: retirar el ejercicio que dolio, SIN reemplazo, esta permitido")
    void dolorRetiraSinReemplazo() {
        // Semana propia: la sesion 102 llevaba 304 -el que dolio- y tambien 740.
        // La derivada se queda solo con 740, asi que es una retirada de verdad y
        // no una sustitucion disfrazada. Con la sesion de un solo ejercicio del
        // material comun esto no se puede probar: quitarlo la dejaria vacia.
        var medida = new PreviousWeekSummary(new BigDecimal("66.67"), new BigDecimal("6.5"), 480,
                Map.of(),
                List.of(outcome(101L, L1, "Cuerpo completo A"),
                        outcome(102L, L1.plusDays(2), "Cuerpo completo B"),
                        outcome(103L, L1.plusDays(4), "Cuerpo completo C")),
                List.of(prescripcion(101L, L1, 740L, 3, 10, 90, null),
                        prescripcion(101L, L1, 1101L, 2, 13, 90, null),
                        prescripcion(102L, L1.plusDays(2), 304L, 3, 10, 90, 304L),
                        prescripcion(102L, L1.plusDays(2), 740L, 3, 10, 90, null),
                        prescripcion(103L, L1.plusDays(4), 592L, 3, 10, 90, null)),
                List.of());

        var derivacion = derivar(Map.of(
                101L, sesion(L2, 45, ej(740L, 3, 10, 90), ej(1101L, 2, 13, 90)),
                102L, sesion(L2.plusDays(2), 30, ej(740L, 3, 10, 90)),
                103L, sesion(L2.plusDays(4), 45, ej(592L, 3, 10, 90))));

        var r = validator.validate(derivacion, medida,
                senalando(102L, TipoIntervencion.SEGURIDAD_ANTE_DOLOR), CATALOGO);

        assertTrue(r.aceptable(), () -> r.problems().toString());
        assertEquals(List.of(102L), r.aplicadas());
        assertEquals(List.of(101L, 103L), r.conservadas());
    }

    @Test
    @DisplayName("Dolor: el reemplazo NO tiene que ser del mismo patron")
    void dolorNoExigeMismoPatron() {
        // Forzar el mismo patron ante dolor podria devolver justo lo que molesto.
        var derivacion = derivar(Map.of(
                101L, sesion(L2, 45, ej(740L, 3, 10, 90), ej(1101L, 2, 13, 90)),
                102L, sesion(L2.plusDays(2), 45, ej(592L, 3, 10, 90)),
                103L, sesion(L2.plusDays(4), 45, ej(592L, 3, 10, 90))));

        var r = validator.validate(derivacion, semanaConDolorEn(304L),
                senalando(102L, TipoIntervencion.SEGURIDAD_ANTE_DOLOR), CATALOGO);

        assertTrue(r.aceptable(), () -> r.problems().toString());
        assertEquals(List.of(102L), r.aplicadas());
    }

    @Test
    @DisplayName("Dolor sin saber de que ejercicio: conservar identica se rechaza")
    void dolorSinEjercicioConocido() {
        // El dolor consta a nivel de sesion. Conservar a ciegas es publicar algo
        // potencialmente perjudicial, asi que esa parte queda sin resolver.
        var r = validator.validate(derivarTodoIgual(), semanaDeTres(),
                senalando(102L, TipoIntervencion.SEGURIDAD_ANTE_DOLOR), CATALOGO);

        assertFalse(r.aceptable());
        assertTrue(unico(r.problems(), "102").contains("no adaptable con seguridad"),
                () -> r.problems().toString());
    }

    // =======================================================================
    // Fila 5 · senalada sin cambios: permitida, pero no es una adaptacion
    // =======================================================================

    @Test
    @DisplayName("Senalada sin cambios: se permite y se registra como adaptacion sin efecto")
    void senaladaSinCambios() {
        var r = validator.validate(derivarTodoIgual(), semanaDeTres(),
                senalando(102L, TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA), CATALOGO);

        assertTrue(r.aceptable(), () -> "conservar nunca es inseguro: " + r.problems());
        assertEquals(List.of(102L), r.sinEfecto());
        assertTrue(r.aplicadas().isEmpty());
        assertFalse(r.huboAdaptacionEfectiva(),
                "un plan valido y una adaptacion que no ocurrio, las dos cosas a la vez");
        assertEquals(List.of(101L, 103L), r.conservadas());
    }

    @Test
    @DisplayName("Causa no registrada: no se inventa nada, conservar es lo correcto")
    void causaNoRegistrada() {
        var r = validator.validate(derivarTodoIgual(), semanaDeTres(),
                senalando(102L, TipoIntervencion.CAUSA_NO_REGISTRADA), CATALOGO);

        assertTrue(r.aceptable());
        assertEquals(List.of(102L), r.sinEfecto());
    }

    // =======================================================================
    // Correspondencia y dias perdidos
    // =======================================================================

    @Test
    @DisplayName("Una sesion anterior sin derivada y nadie ordeno reducir dias: rechazada")
    void sesionPerdidaSinOrden() {
        var derivacion = new WeekDerivation(List.of(
                new DerivedWorkout(101L, sesion(L2, 45, ej(740L, 3, 10, 90), ej(1101L, 2, 13, 90))),
                new DerivedWorkout(103L, sesion(L2.plusDays(4), 45, ej(592L, 3, 10, 90)))),
                List.of(102L));

        var r = validator.validate(derivacion, semanaDeTres(),
                senalando(101L, TipoIntervencion.ACORTAR_O_REORGANIZAR), CATALOGO);

        assertFalse(r.aceptable());
        assertTrue(unico(r.problems(), "[102]").contains("nadie ordeno reducir dias"),
                () -> r.problems().toString());
    }

    @Test
    @DisplayName("Con reduccion de dias ordenada, la sesion perdida se acepta")
    void sesionPerdidaConOrden() {
        var derivacion = new WeekDerivation(List.of(
                new DerivedWorkout(101L, sesion(L2, 45, ej(740L, 3, 10, 90), ej(1101L, 2, 13, 90))),
                new DerivedWorkout(103L, sesion(L2.plusDays(4), 45, ej(592L, 3, 10, 90)))),
                List.of(102L));

        var orden = new PlanAdjustment(List.of(AdjustmentType.REDUCE_DAYS), null, null, null,
                0.0, 0.0, null, 2, null, "baja adherencia", null, null,
                List.of(new PlanAdjustment.TargetedSession(101L,
                        TipoIntervencion.ACORTAR_O_REORGANIZAR)));

        var r = validator.validate(derivacion, semanaDeTres(), orden, CATALOGO);

        assertTrue(r.aceptable(), () -> r.problems().toString());
    }

    @Test
    @DisplayName("Dos sesiones nuevas desde el mismo origen: rechazadas")
    void dosDesdeElMismoOrigen() {
        var derivacion = new WeekDerivation(List.of(
                new DerivedWorkout(101L, sesion(L2, 45, ej(740L, 3, 10, 90), ej(1101L, 2, 13, 90))),
                new DerivedWorkout(101L, sesion(L2.plusDays(2), 45, ej(740L, 3, 10, 90)))),
                List.of());

        var r = validator.validate(derivacion, semanaDeTres(),
                senalando(101L, TipoIntervencion.ACORTAR_O_REORGANIZAR), CATALOGO);

        assertFalse(r.aceptable());
        assertTrue(unico(r.problems(), "uno a uno").contains("dicen derivar de la 101"),
                () -> r.problems().toString());
    }

    @Test
    @DisplayName("Un origen que no esta en la semana medida: rechazado")
    void origenInexistente() {
        var derivacion = new WeekDerivation(List.of(
                new DerivedWorkout(999L, sesion(L2, 45, ej(740L, 3, 10, 90)))),
                List.of(101L, 102L, 103L));

        var r = validator.validate(derivacion, semanaDeTres(),
                senalando(999L, TipoIntervencion.ACORTAR_O_REORGANIZAR), CATALOGO);

        assertFalse(r.aceptable());
        assertTrue(r.problems().stream().anyMatch(p -> p.contains("no esta en la semana medida")),
                () -> r.problems().toString());
    }

    @Test
    @DisplayName("Una sesion que desaparece sin declararse: rechazada aunque la lista venga vacia")
    void sesionDesaparecidaSinDeclarar() {
        // El caso peor: la 102 no tiene derivada y tampoco consta como eliminada.
        // La lista de eliminadas viene VACIA, asi que creerla seria dar el visto
        // bueno a una sesion desaparecida.
        var derivacion = new WeekDerivation(List.of(
                new DerivedWorkout(101L, sesion(L2, 45, ej(740L, 3, 10, 90), ej(1101L, 2, 13, 90))),
                new DerivedWorkout(103L, sesion(L2.plusDays(4), 45, ej(592L, 3, 10, 90)))),
                List.of());

        var r = validator.validate(derivacion, semanaDeTres(),
                senalando(101L, TipoIntervencion.ACORTAR_O_REORGANIZAR), CATALOGO);

        assertFalse(r.aceptable());
        assertTrue(unico(r.problems(), "desaparecieron").contains("[102]"),
                () -> r.problems().toString());
    }

    @Test
    @DisplayName("Una sesion con derivada Y declarada como eliminada: rechazada")
    void sesionEnLasDosListas() {
        var derivacion = new WeekDerivation(List.of(
                new DerivedWorkout(101L, sesion(L2, 45, ej(740L, 3, 10, 90), ej(1101L, 2, 13, 90))),
                new DerivedWorkout(102L, sesion(L2.plusDays(2), 45, ej(304L, 3, 10, 90))),
                new DerivedWorkout(103L, sesion(L2.plusDays(4), 45, ej(592L, 3, 10, 90)))),
                List.of(102L));

        var r = validator.validate(derivacion, semanaDeTres(),
                senalando(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR), CATALOGO);

        assertFalse(r.aceptable());
        assertTrue(unico(r.problems(), "a la vez").contains("[102]"),
                () -> r.problems().toString());
    }

    @Test
    @DisplayName("Declarar eliminada una sesion que no existe: rechazada")
    void eliminadaInexistente() {
        var derivacion = new WeekDerivation(List.of(
                new DerivedWorkout(101L, sesion(L2, 45, ej(740L, 3, 10, 90), ej(1101L, 2, 13, 90))),
                new DerivedWorkout(102L, sesion(L2.plusDays(2), 45, ej(304L, 3, 10, 90))),
                new DerivedWorkout(103L, sesion(L2.plusDays(4), 45, ej(592L, 3, 10, 90)))),
                List.of(777L));

        var r = validator.validate(derivacion, semanaDeTres(),
                senalando(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR), CATALOGO);

        assertFalse(r.aceptable());
        // Da DOS problemas y los dos son ciertos: no existe, y ademas nadie
        // ordeno reducir dias. No se exige uno solo.
        assertTrue(r.problems().stream()
                        .anyMatch(p -> p.contains("[777]")
                                && p.contains("no estan en la semana medida")),
                () -> r.problems().toString());
    }

    @Test
    @DisplayName("Dolor sin ejercicio conocido: mover el dia NO es una resolucion de seguridad")
    void dolorMoverElDiaNoResuelve() {
        // La sesion cambia -se mueve del miercoles al jueves- pero sus ejercicios
        // son los mismos, y no se sabe cual dolio. No esta resuelto.
        var derivacion = derivar(Map.of(
                101L, sesion(L2, 45, ej(740L, 3, 10, 90), ej(1101L, 2, 13, 90)),
                102L, sesion(L2.plusDays(3), 45, ej(304L, 3, 10, 90)),
                103L, sesion(L2.plusDays(4), 45, ej(592L, 3, 10, 90))));

        var r = validator.validate(derivacion, semanaDeTres(),
                senalando(102L, TipoIntervencion.SEGURIDAD_ANTE_DOLOR), CATALOGO);

        assertFalse(r.aceptable());
        assertTrue(unico(r.problems(), "pudo ser el que").contains("[304]"),
                () -> r.problems().toString());
    }

    @Test
    @DisplayName("Dolor sin ejercicio conocido: acortar o bajar la carga tampoco resuelve")
    void dolorAcortarNoResuelve() {
        var derivacion = derivar(Map.of(
                101L, sesion(L2, 45, ej(740L, 3, 10, 90), ej(1101L, 2, 13, 90)),
                102L, sesion(L2.plusDays(2), 25, ej(304L, 2, 8, 90)),
                103L, sesion(L2.plusDays(4), 45, ej(592L, 3, 10, 90))));

        var r = validator.validate(derivacion, semanaDeTres(),
                senalando(102L, TipoIntervencion.SEGURIDAD_ANTE_DOLOR), CATALOGO);

        assertFalse(r.aceptable());
        assertTrue(unico(r.problems(), "pudo ser el que").contains("[304]"),
                () -> r.problems().toString());
    }

    // =======================================================================
    // La exencion del piso de duracion es POR SESION, no por semana
    // =======================================================================

    @Test
    @DisplayName("Solo la sesion acortada por falta de tiempo queda exenta del piso de V20")
    void exencionDelPisoSoloEnLaAcortada() {
        var derivacion = derivar(Map.of(
                101L, sesion(L2, 45, ej(740L, 3, 10, 90), ej(1101L, 2, 13, 90)),
                102L, sesion(L2.plusDays(2), 30, ej(304L, 2, 10, 90)),
                103L, sesion(L2.plusDays(4), 45, ej(592L, 3, 10, 90))));

        var r = validator.validate(derivacion, semanaDeTres(),
                senalando(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR), CATALOGO);

        assertTrue(r.aceptable(), () -> r.problems().toString());
        assertEquals(java.util.Set.of(L2.plusDays(2)), r.fechasConDuracionReducible(),
                "solo la senalada: desactivar el piso de la semana entera relajaria las demas "
                        + "y cambiaria lo que el prompt le dice a la IA");
    }

    @Test
    @DisplayName("Por dificultad u horario NO se exime del piso: nadie autorizo acortar")
    void sinExencionPorDificultadNiHorario() {
        var porDificultad = validator.validate(derivarTodoIgual(), semanaDeTres(),
                senalando(102L, TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA), CATALOGO);
        assertTrue(porDificultad.fechasConDuracionReducible().isEmpty());

        var derivacionMovida = derivar(Map.of(
                101L, sesion(L2, 45, ej(740L, 3, 10, 90), ej(1101L, 2, 13, 90)),
                102L, sesion(L2.plusDays(3), 45, ej(304L, 3, 10, 90)),
                103L, sesion(L2.plusDays(4), 45, ej(592L, 3, 10, 90))));
        var porHorario = validator.validate(derivacionMovida, semanaDeTres(),
                senalando(102L, TipoIntervencion.REPROGRAMAR_DIAS), CATALOGO);
        assertTrue(porHorario.fechasConDuracionReducible().isEmpty());
    }

    @Test
    @DisplayName("El enfoque no cambia ni estando senalada")
    void elEnfoqueNoCambia() {
        var derivacion = derivar(Map.of(
                101L, sesion(L2, 45, ej(740L, 3, 10, 90), ej(1101L, 2, 13, 90)),
                102L, new PlanDraft.DraftWorkout(L2.plusDays(2), WorkoutFocus.PUSH,
                        "Cuerpo completo B", 45, List.of(ej(304L, 3, 10, 90))),
                103L, sesion(L2.plusDays(4), 45, ej(592L, 3, 10, 90))));

        var r = validator.validate(derivacion, semanaDeTres(),
                senalando(102L, TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA), CATALOGO);

        assertFalse(r.aceptable());
        assertTrue(unico(r.problems(), "enfoque").contains("se corrige, no se reemplaza"),
                () -> r.problems().toString());
    }

    // =======================================================================
    // Material
    // =======================================================================

    private static String unico(List<String> problems, String fragmento) {
        var coincidencias = problems.stream().filter(p -> p.contains(fragmento)).toList();
        assertEquals(1, coincidencias.size(),
                "se esperaba un solo problema con \"" + fragmento + "\" y hay "
                        + coincidencias.size() + ": " + problems);
        return coincidencias.get(0);
    }

    private static PlanAdjustment senalando(long plannedWorkoutId, TipoIntervencion tipo) {
        return new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null,
                0.0, 0.0, null, null, null, "baja adherencia", null, null,
                List.of(new PlanAdjustment.TargetedSession(plannedWorkoutId, tipo)));
    }

    /** La semana nueva copiada tal cual: tres sesiones, misma dosis, mismo dia. */
    private static WeekDerivation derivarTodoIgual() {
        return derivar(Map.of(
                101L, sesion(L2, 45, ej(740L, 3, 10, 90), ej(1101L, 2, 13, 90)),
                102L, sesion(L2.plusDays(2), 45, ej(304L, 3, 10, 90)),
                103L, sesion(L2.plusDays(4), 45, ej(592L, 3, 10, 90))));
    }

    private static WeekDerivation derivar(Map<Long, PlanDraft.DraftWorkout> porOrigen) {
        var derivadas = new ArrayList<DerivedWorkout>();
        new java.util.TreeMap<>(porOrigen).forEach((origen, sesion) ->
                derivadas.add(new DerivedWorkout(origen, sesion)));
        return new WeekDerivation(derivadas, List.of());
    }

    private static PlanDraft.DraftWorkout sesion(LocalDate fecha, int minutos,
                                                 PlanDraft.DraftExercise... ejercicios) {
        String nombre = switch (fecha.getDayOfWeek()) {
            case MONDAY -> "Cuerpo completo A";
            case WEDNESDAY -> "Cuerpo completo B";
            default -> "Cuerpo completo C";
        };
        return new PlanDraft.DraftWorkout(fecha, WorkoutFocus.FULL_BODY, nombre, minutos,
                List.of(ejercicios));
    }

    private static PlanDraft.DraftExercise ej(long id, int series, int reps, int descanso) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS,
                (short) series, (short) reps, null, null, (short) descanso, null);
    }

    private static PreviousWeekSummary semanaDeTres() {
        return semana(null);
    }

    /** La misma semana, con un ejercicio marcado PAIN_OR_DISCOMFORT. */
    private static PreviousWeekSummary semanaConDolorEn(long exerciseId) {
        return semana(exerciseId);
    }

    private static PreviousWeekSummary semana(Long conDolor) {
        var workouts = List.of(
                outcome(101L, L1, "Cuerpo completo A"),
                outcome(102L, L1.plusDays(2), "Cuerpo completo B"),
                outcome(103L, L1.plusDays(4), "Cuerpo completo C"));

        var prescriptions = List.of(
                prescripcion(101L, L1, 740L, 3, 10, 90, conDolor),
                prescripcion(101L, L1, 1101L, 2, 13, 90, conDolor),
                prescripcion(102L, L1.plusDays(2), 304L, 3, 10, 90, conDolor),
                prescripcion(103L, L1.plusDays(4), 592L, 3, 10, 90, conDolor));

        return new PreviousWeekSummary(new BigDecimal("66.67"), new BigDecimal("6.5"), 480,
                Map.of(), workouts, prescriptions, List.of());
    }

    private static PreviousWeekSummary.WorkoutOutcome outcome(long id, LocalDate fecha,
                                                              String nombre) {
        return new PreviousWeekSummary.WorkoutOutcome(id, fecha, "FULL_BODY", "COMPLETED",
                null, true, "COMPLETED", (short) 6, new BigDecimal("100.00"), nombre, (short) 45);
    }

    private static PreviousWeekSummary.PrescriptionOutcome prescripcion(
            long workoutId, LocalDate fecha, long exerciseId, int series, int reps,
            int descanso, Long conDolor) {
        String motivo = conDolor != null && conDolor == exerciseId ? "PAIN_OR_DISCOMFORT" : null;
        return new PreviousWeekSummary.PrescriptionOutcome(
                workoutId, fecha, exerciseId, "ejercicio " + exerciseId, "SETS_REPS",
                (short) series, (short) reps, null, null,
                (short) series, series * reps, null, null,
                new BigDecimal("100.00"), "COMPLETED", motivo, "COMPLETED", (short) descanso);
    }

    private static Map<Long, CandidateExercise> catalogo() {
        var mapa = new LinkedHashMap<Long, CandidateExercise>();
        mapa.put(740L, candidato(740L, "HORIZONTAL_PUSH"));
        mapa.put(304L, candidato(304L, "HORIZONTAL_PUSH"));
        mapa.put(592L, candidato(592L, "KNEE_DOMINANT"));
        mapa.put(1101L, candidato(1101L, "CORE_STABILITY"));
        return mapa;
    }

    private static CandidateExercise candidato(long id, String patron) {
        return new CandidateExercise(id, "ejercicio " + id, "chest", "body weight", 2,
                PrescriptionType.SETS_REPS, null, patron);
    }
}
