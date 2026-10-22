package main.web.services.fitsense.p2d;

import jakarta.persistence.EntityManager;
import main.web.services.fitsense.planning.domain.model.aggregates.WeeklyTrainingPlan;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptationDecision;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptedPublication;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptedWeekResult;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdjustmentType;
import main.web.services.fitsense.planning.domain.model.valueobjects.ConservationReport;
import main.web.services.fitsense.planning.domain.model.valueobjects.DerivedWorkout;
import main.web.services.fitsense.planning.domain.model.valueobjects.GenerationSource;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PreparedPlanGeneration;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.WeeklyTrainingPlanRepository;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P2-D · EL DISPARADOR DIFERIDO RECHAZA AL COMMIT.
 * <p>
 * {@code tg_paa_coherencia} es el ultimo mecanismo de V32 que ninguna prueba
 * habia tocado, y el unico DIFERIDO de la tabla. Comprueba lo que un
 * {@code CHECK} no puede, porque hay que mirar otras tablas: que la metrica, el
 * plan de origen y el plan resultante pertenezcan al MISMO participante, que el
 * origen sea el de la semana medida, que las sesiones de los cuatro arreglos
 * pertenezcan al plan de origen, y que el origen de generacion no contradiga al
 * del plan publicado.
 * <p>
 * SER DIFERIDO NO ES UN DETALLE: es lo que permite insertar el plan resultante y
 * su evidencia en la misma transaccion sin depender del orden de las sentencias.
 * Y es tambien lo que hace que su rechazo llegue en un momento distinto —al
 * confirmar— que el de las diecinueve restricciones inmediatas.
 *
 * <h2>QUE DEMUESTRA CADA PRUEBA, EN TRES PASOS SEPARADOS</h2>
 *
 * Las tres cosas se comprueban por separado a proposito, porque una sola
 * excepcion al final no distingue entre ellas:
 * <ol>
 *   <li><b>La sentencia PASA.</b> Se guarda el numero de filas que devolvio el
 *       {@code UPDATE}: si alguna de las diecinueve restricciones inmediatas
 *       hubiera rechazado la fila, {@code executeUpdate} habria lanzado y ese
 *       numero no existiria. Que valga 1 es la prueba de que la incoherencia
 *       LLEGO a la base.
 *   <li><b>El COMMIT falla, y lo rechaza el DISPARADOR.</b> La excepcion sale de
 *       {@code tx.execute}, o sea al confirmar y no en la sentencia. Y se exige
 *       que el mensaje sea el del disparador —empieza por {@code V32:}— y que NO
 *       contenga {@code violates check constraint}: sin esa segunda mitad, un
 *       {@code CHECK} que hubiera reventado en el ultimo momento daria el mismo
 *       verde.
 *   <li><b>No queda nada.</b> Se vuelve a consultar la columna y sigue como
 *       estaba.
 * </ol>
 *
 * <h2>POR QUE ESTAS DOS INCOHERENCIAS Y NO OTRAS</h2>
 *
 * La funcion del disparador tiene ocho reglas. Una incoherencia solo sirve para
 * probarla si ninguna de las diecinueve restricciones inmediatas la ataja antes:
 * si la atajara, la prueba quedaria verde sin que el disparador se hubiera
 * llegado a ejecutar. Se leyeron las diecinueve antes de elegir.
 * <ul>
 *   <li><b>{@code generation_source = 'RULE_ENGINE'}</b> sobre un plan publicado
 *       que dice {@code AI}. Solo dos restricciones miran esa columna:
 *       {@code ck_paa_gen_source}, que admite {@code RULE_ENGINE}, y
 *       {@code ck_paa_procedencia_continuidad}, que solo aplica a
 *       {@code RESPALDO} y {@code SIN_ORDEN}. Pasa, y la rechaza la regla v3 del
 *       disparador.
 *   <li><b>{@code conserved_sessions = '{999999999}'}</b>, una sesion que no
 *       existe. Solo {@code ck_paa_conservadas_disjuntas} mira esa columna, y se
 *       cumple porque el identificador es disjunto de las senaladas. <b>Un
 *       arreglo no puede llevar claves ajenas</b>, asi que el disparador es la
 *       UNICA proteccion que hay ahi: es justo el motivo por el que la v3 de V32
 *       lo anadio.
 * </ul>
 * La primera no es una regla cualquiera: protege la proporcion de {@code AI}
 * frente a {@code RULE_ENGINE}, que es un dato reportable de la tesis. Una fila
 * que declara un origen y un plan que declara otro hacen que una de las dos
 * cifras sea falsa, y cual lo sea no se puede averiguar despues.
 *
 * <h2>Como se ejecuta</h2>
 * <pre>
 * mvn "-Dtest=DisparadorDiferidoIT" test ^
 *     "-Dfitsense.pruebas.url=jdbc:postgresql://&lt;host-de-FitSenseTests&gt;/neondb?sslmode=require" ^
 *     "-Dfitsense.pruebas.usuario=neondb_owner" ^
 *     "-Dfitsense.pruebas.clave=&lt;clave&gt;"
 * </pre>
 * Mismos cerrojos y misma limpieza por identificador que las otras dos clases de
 * P2-D, mas uno propio: antes de empezar comprueba en {@code pg_trigger} que el
 * disparador existe y es diferible y diferido. Sin eso, las dos pruebas podrian
 * pasar con un disparador inmediato y nadie notaria que lo que se demostro no es
 * lo que dice el titulo.
 * <p>
 * Usa el <b>30 de noviembre y el 7 de diciembre</b>, libres de las semanas que
 * usan las otras dos clases.
 */
@EnabledIfSystemProperty(named = "fitsense.pruebas.url", matches = ".+")
@SpringBootTest
@DisplayName("P2-D · tg_paa_coherencia rechaza al COMMIT, no en la sentencia")
class DisparadorDiferidoIT {

    private static final long USUARIO = 1L;
    private static final LocalDate SEMANA_MEDIDA = LocalDate.of(2026, 11, 30);
    private static final LocalDate SEMANA_NUEVA = LocalDate.of(2026, 12, 7);
    private static final long EJERCICIO = 55L;

    /** Una sesion que no existe, para la regla v3 del disparador. */
    private static final long SESION_INEXISTENTE = 999_999_999L;

    @DynamicPropertySource
    static void apuntarAPruebas(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", () -> System.getProperty("fitsense.pruebas.url"));
        registro.add("spring.datasource.username", () -> System.getProperty("fitsense.pruebas.usuario"));
        registro.add("spring.datasource.password", () -> System.getProperty("fitsense.pruebas.clave"));
        registro.add("spring.flyway.enabled", () -> "false");
        registro.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registro.add("fitsense.ai.enabled", () -> "false");
        registro.add("authorization.jwt.secret",
                () -> "secreto-solo-para-levantar-el-contexto-de-P2-D-no-se-usa!");
    }

    @Autowired private WeeklyTrainingPlanCommandService commandService;
    @Autowired private WeeklyTrainingPlanRepository planRepository;
    @Autowired private EntityManager em;
    @Autowired private PlatformTransactionManager transactionManager;

    private TransactionTemplate tx;

    /** SOLO se borra lo que figure aqui, y al reves del orden de creacion. */
    private final List<Long> planesCreados = new ArrayList<>();
    private Long metricaCreadaId;
    private boolean entornoVerificado;

    private Long planOrigenId;
    private Long sesionOrigenId;

    // =======================================================================
    // Los cerrojos
    // =======================================================================

    @BeforeEach
    void exigirEntornoYDisparadorDiferido() {
        tx = new TransactionTemplate(transactionManager);
        planesCreados.clear();
        metricaCreadaId = null;
        planOrigenId = null;
        sesionOrigenId = null;
        entornoVerificado = false;

        // Cerrojo 1: la marca.
        var marcas = enTransaccion(() -> em.createNativeQuery(
                        "SELECT entorno FROM fitsense_entorno WHERE entorno = 'PRUEBAS'")
                .getResultList());

        assertFalse(marcas.isEmpty(),
                "Esta base no lleva la marca PRUEBAS en fitsense_entorno. O no es"
                        + " FitSenseTests, o la marca no se creo. NO se escribe nada.");

        // Cerrojo 2: las dos semanas de esta clase estan libres, y si no lo
        // estan NO se borra nada: se avisa.
        long ocupado = cuenta(
                "SELECT (SELECT COUNT(*) FROM weekly_training_plans"
                        + "         WHERE user_id = " + USUARIO
                        + "           AND week_start_date IN (DATE '" + SEMANA_MEDIDA + "',"
                        + "                                   DATE '" + SEMANA_NUEVA + "'))"
                        + "     + (SELECT COUNT(*) FROM plan_adaptation_attempts"
                        + "         WHERE user_id = " + USUARIO
                        + "           AND new_week_start_date = DATE '" + SEMANA_NUEVA + "')"
                        + "     + (SELECT COUNT(*) FROM weekly_user_metrics"
                        + "         WHERE user_id = " + USUARIO
                        + "           AND week_start_date = DATE '" + SEMANA_MEDIDA + "')");

        assertEquals(0, ocupado,
                "Las semanas del " + SEMANA_MEDIDA + " y del " + SEMANA_NUEVA + " del"
                        + " usuario " + USUARIO + " ya tienen " + ocupado + " filas. Esta"
                        + " prueba NO borra lo que no creo: revisalas a mano y vuelve a"
                        + " ejecutar.");

        // Cerrojo 3, PROPIO DE ESTA CLASE: el disparador existe y es DIFERIBLE y
        // DIFERIDO. Sin esta comprobacion, las dos pruebas de abajo podrian
        // pasar contra un disparador inmediato y el verde diria algo que no es:
        // el titulo de la clase afirma que rechaza al COMMIT.
        assertEquals(1, cuenta(
                        "SELECT COUNT(*) FROM pg_trigger"
                                + " WHERE tgrelid = 'plan_adaptation_attempts'::regclass"
                                + "   AND tgname = 'tg_paa_coherencia'"
                                + "   AND tgdeferrable AND tginitdeferred"),
                "tg_paa_coherencia no existe, o no es diferible y diferido en esta"
                        + " base. Lo que esta clase afirma demostrar no se puede demostrar"
                        + " asi.");

        entornoVerificado = true;
    }

    // =======================================================================
    // Las dos incoherencias
    // =======================================================================

    @Test
    @DisplayName("Un origen que contradice al plan publicado lo rechaza el disparador al COMMIT")
    void origenQueContradiceAlPlanPublicado() {
        long intento = publicarIntentoReal();

        // El plan publicado dice AI -lo escribio publishAdapted-; el intento
        // pasara a decir RULE_ENGINE. Es coherente consigo mismo y falso.
        assertEquals("AI", unaFila("SELECT generation_source FROM plan_adaptation_attempts"
                + " WHERE attempt_id = " + intento)[0]);

        rechazaAlConfirmar(
                "UPDATE plan_adaptation_attempts SET generation_source = 'RULE_ENGINE'"
                        + " WHERE attempt_id = " + intento,
                "el intento declara origen");

        // Paso 3: no quedo nada.
        assertEquals("AI", unaFila("SELECT generation_source FROM plan_adaptation_attempts"
                        + " WHERE attempt_id = " + intento)[0],
                "el rollback del COMMIT fallido tenia que dejar la fila como estaba");
    }

    @Test
    @DisplayName("Una sesion que no es del plan de origen la rechaza el disparador al COMMIT")
    void sesionAjenaAlPlanDeOrigen() {
        long intento = publicarIntentoReal();

        assertEquals(0, cuenta("SELECT cardinality(conserved_sessions)"
                        + " FROM plan_adaptation_attempts WHERE attempt_id = " + intento),
                "la fila publicada no tenia que traer sesiones conservadas");

        // UN ARREGLO NO PUEDE LLEVAR CLAVES AJENAS. Sin el disparador, esta fila
        // -que declara conservada una sesion que no existe- se guardaria sin
        // mas, y al consultarla meses despues no se distinguiria de una
        // correcta.
        rechazaAlConfirmar(
                "UPDATE plan_adaptation_attempts"
                        + " SET conserved_sessions = ARRAY[" + SESION_INEXISTENTE + "]::BIGINT[]"
                        + " WHERE attempt_id = " + intento,
                "no pertenecen al plan de origen");

        assertEquals(0, cuenta("SELECT cardinality(conserved_sessions)"
                        + " FROM plan_adaptation_attempts WHERE attempt_id = " + intento),
                "el rollback del COMMIT fallido tenia que dejar la fila como estaba");
    }

    // =======================================================================
    // El nucleo de la comprobacion
    // =======================================================================

    /**
     * Ejecuta la sentencia y confirma, y comprueba las dos primeras cosas: que
     * la sentencia PASO y que lo que fallo fue el COMMIT, por obra del
     * disparador.
     *
     * @param sentencia      el {@code UPDATE} que introduce la incoherencia
     * @param trozoDelMensaje un fragmento del mensaje que levanta la funcion del
     *                        disparador, para no conformarse con que «algo» falle
     */
    private void rechazaAlConfirmar(String sentencia, String trozoDelMensaje) {
        // Se anota FUERA del lambda para poder leerlo despues de la excepcion.
        int[] filas = {-1};

        var error = assertThrows(RuntimeException.class,
                () -> tx.execute(estado -> {
                    filas[0] = em.createNativeQuery(sentencia).executeUpdate();
                    // Si se llega aqui, ninguna de las diecinueve restricciones
                    // inmediatas rechazo la fila. El COMMIT ocurre al salir.
                    return null;
                }),
                "el disparador tenia que rechazar la transaccion al confirmar");

        // PASO 1: la sentencia paso. Si un CHECK hubiera reventado,
        // executeUpdate habria lanzado y filas[0] seguiria en -1.
        assertEquals(1, filas[0],
                "la sentencia no llego a ejecutarse, asi que la rechazo una"
                        + " restriccion inmediata y NO el disparador diferido. El"
                        + " disparador no se llego a ejecutar y la prueba no demuestra"
                        + " nada. Cadena de causas: " + causas(error));

        // PASO 2a: el mensaje es el del disparador.
        String cadena = causas(error);
        assertTrue(cadena.contains(trozoDelMensaje),
                "fallo al confirmar, pero el mensaje no es el que levanta"
                        + " fn_intento_coherente(). Se esperaba el fragmento '"
                        + trozoDelMensaje + "'. Cadena de causas: " + cadena);

        // PASO 2b: y NO es una restriccion inmediata que haya reventado tarde.
        // Sin esta mitad, un CHECK daria el mismo verde.
        assertFalse(cadena.contains("violates check constraint"),
                "lo rechazo un CHECK y no el disparador. Cadena de causas: " + cadena);
    }

    // =======================================================================
    // La fila real de la que parten las dos pruebas
    // =======================================================================

    private long publicarIntentoReal() {
        sembrar();

        var publicado = commandService.publishAdapted(preparado(), publicacion(metricaCreadaId));
        assertTrue(publicado.isPresent(), "no se publico ningun plan");

        Long planNuevo = publicado.get().getId();
        planesCreados.add(planNuevo);

        var fila = unaFila("SELECT attempt_id FROM plan_adaptation_attempts"
                + " WHERE resulting_plan_id = " + planNuevo);
        assertNotNull(fila, "se publico el plan y no quedo su fila de evidencia");
        return ((Number) fila[0]).longValue();
    }

    void sembrar() {
        enTransaccion(() -> {
            var origen = WeeklyTrainingPlan.firstVersion(USUARIO, (short) 1,
                    SEMANA_MEDIDA, SEMANA_MEDIDA.plusDays(6),
                    GenerationSource.AI, "gpt-5-nano", "{\"siembra\":true}", (short) 1);

            var sesion = origen.addWorkout(SEMANA_MEDIDA, WorkoutFocus.FULL_BODY,
                    "FULL_BODY - Sesion 1", 45,
                    SEMANA_MEDIDA.atTime(LocalTime.of(23, 59))
                            .atZone(ZoneId.of("America/Lima")).toOffsetDateTime());
            sesion.addExercise(EJERCICIO, PrescriptionType.SETS_REPS,
                    (short) 3, (short) 10, null, null, (short) 90, null);

            var guardado = planRepository.saveAndFlush(origen);
            planOrigenId = guardado.getId();
            planesCreados.add(planOrigenId);
            sesionOrigenId = guardado.workoutsView().get(0).getId();

            em.createNativeQuery(
                            "INSERT INTO weekly_user_metrics"
                                    + " (user_id, plan_id, week_number, week_start_date,"
                                    + "  week_end_date, has_active_plan, calculation_version,"
                                    + "  weighted_adherence_pct)"
                                    + " VALUES (?1, ?2, 1, ?3, ?4, TRUE, 'MVP-1.8', 42.00)")
                    .setParameter(1, USUARIO)
                    .setParameter(2, planOrigenId)
                    .setParameter(3, SEMANA_MEDIDA)
                    .setParameter(4, SEMANA_MEDIDA.plusDays(6))
                    .executeUpdate();

            metricaCreadaId = ((Number) em.createNativeQuery(
                            "SELECT weekly_metric_id FROM weekly_user_metrics"
                                    + " WHERE user_id = ?1 AND week_start_date = ?2")
                    .setParameter(1, USUARIO)
                    .setParameter(2, SEMANA_MEDIDA)
                    .getSingleResult()).longValue();

            return null;
        });
    }

    @AfterEach
    void limpiar() {
        if (!entornoVerificado) {
            // JUnit ejecuta @AfterEach aunque @BeforeEach falle. Sin este
            // return, un cerrojo que no pasa acabaria borrando en la base que no
            // se pudo identificar.
            return;
        }

        for (Long plan : planesCreados)
            ejecutar("DELETE FROM plan_adaptation_attempts"
                    + " WHERE source_plan_id = " + plan + " OR resulting_plan_id = " + plan);

        if (metricaCreadaId != null)
            ejecutar("DELETE FROM weekly_user_metrics"
                    + " WHERE weekly_metric_id = " + metricaCreadaId);

        for (Long plan : alReves(planesCreados)) {
            ejecutar("DELETE FROM planned_workout_exercises WHERE planned_workout_id IN"
                    + " (SELECT planned_workout_id FROM planned_workouts"
                    + "  WHERE plan_id = " + plan + ")");
            ejecutar("DELETE FROM planned_workouts WHERE plan_id = " + plan);
        }

        for (Long plan : alReves(planesCreados))
            ejecutar("DELETE FROM weekly_training_plans WHERE plan_id = " + plan);

        planesCreados.clear();
        metricaCreadaId = null;
    }

    private static List<Long> alReves(List<Long> ids) {
        var copia = new ArrayList<>(ids);
        java.util.Collections.reverse(copia);
        return copia;
    }

    // =======================================================================
    // Material
    // =======================================================================

    private PreparedPlanGeneration preparado() {
        var ajuste = new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null,
                0.0, 0.0, null, null, null, "baja adherencia", null, null,
                List.of(new PlanAdjustment.TargetedSession(
                        sesionOrigenId, TipoIntervencion.ACORTAR_O_REORGANIZAR)));

        var command = new GenerateWeeklyPlanCommand(USUARIO, SEMANA_NUEVA, ajuste, true,
                new BigDecimal("42.00"), null);

        return new PreparedPlanGeneration(command, null, ajuste, SEMANA_NUEVA,
                SEMANA_NUEVA.plusDays(6), 30, (short) 2, null, ZoneId.of("America/Lima"));
    }

    private AdaptedPublication publicacion(Long metrica) {
        var informe = new ConservationReport(List.of(), List.of(sesionOrigenId),
                List.of(), List.of(), java.util.Set.of());

        var resultado = new AdaptedWeekResult(AdaptedWeekResult.Estado.PUBLICABLE,
                AdaptedWeekResult.Motivo.CORRECCION_IA, semanaDerivada(),
                GenerationSource.AI, informe, List.of());

        return new AdaptedPublication(resultado, AdaptationDecision.ADAPT_LOCALIZED,
                planOrigenId, metrica, "{\"proyeccion\":true}", "gpt-5-nano", (short) 1,
                new BigDecimal("42.00"), new BigDecimal("100.00"), "baja adherencia");
    }

    private List<DerivedWorkout> semanaDerivada() {
        return List.of(new DerivedWorkout(sesionOrigenId, new PlanDraft.DraftWorkout(
                SEMANA_NUEVA, WorkoutFocus.FULL_BODY, "FULL_BODY - Sesion 1", 45,
                List.of(new PlanDraft.DraftExercise(EJERCICIO, PrescriptionType.SETS_REPS,
                        (short) 3, (short) 8, null, null, (short) 90, null)))));
    }

    // =======================================================================
    // Utiles
    // =======================================================================

    private <T> T enTransaccion(Supplier<T> paso) {
        return tx.execute(estado -> paso.get());
    }

    long cuenta(String sql) {
        return enTransaccion(() ->
                ((Number) em.createNativeQuery(sql).getSingleResult()).longValue());
    }

    Object[] unaFila(String sql) {
        return enTransaccion(() -> {
            var filas = em.createNativeQuery(sql).getResultList();
            if (filas.isEmpty()) return null;
            Object primera = filas.get(0);
            return primera instanceof Object[] columnas
                    ? columnas : new Object[]{primera};
        });
    }

    void ejecutar(String sql) {
        enTransaccion(() -> em.createNativeQuery(sql).executeUpdate());
    }

    /**
     * Toda la cadena de causas en un solo texto. El mensaje que levanta la
     * funcion del disparador lo trae el driver de PostgreSQL, varias capas por
     * debajo de la excepcion que Spring deja salir.
     */
    private static String causas(Throwable error) {
        var texto = new StringBuilder();
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            texto.append(causa.getClass().getSimpleName())
                    .append(": ").append(causa.getMessage()).append(" || ");
            if (causa.getCause() == causa) break;
        }
        return texto.toString();
    }
}
