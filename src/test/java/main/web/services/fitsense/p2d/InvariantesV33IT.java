package main.web.services.fitsense.p2d;

import jakarta.persistence.EntityManager;
import main.web.services.fitsense.planning.domain.model.aggregates.PlanAdaptationAttempt;
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
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.PlanAdaptationAttemptRepository;
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
 * P2-D · LOS DOS CHECK DE V33 RECHAZAN DE VERDAD, Y LOS ARREGLOS VIAJAN
 * INTACTOS.
 * <p>
 * V33 anadio una equivalencia: hay sesiones aplicadas EXACTAMENTE cuando el
 * estado es APLICADA. Son dos restricciones porque son dos direcciones, y las
 * dos serian un dato falso en la tabla que sostiene la evidencia de la tesis:
 * <ul>
 *   <li>{@code ck_paa_aplicada_con_sesiones} — un APLICADA sin ninguna sesion
 *       aplicada afirma una adaptacion que no toco nada;
 *   <li>{@code ck_paa_solo_aplicada_con_sesiones} — un RESPALDO con sesiones
 *       aplicadas atribuye al modelo un cambio en una semana que solo se copio.
 * </ul>
 *
 * <h2>POR QUE SE MODIFICA UNA FILA REAL Y NO SE INSERTA UNA A MANO</h2>
 *
 * Las dos razones importan, y la segunda es la de fondo.
 * <p>
 * <b>Primera: una fila a mano pasaria por 21 restricciones mas y cuatro claves
 * ajenas.</b> Si me equivocara en una sola, PostgreSQL rechazaria la fila por
 * OTRO motivo y la prueba pasaria por la razon equivocada: habria demostrado que
 * la base rechaza algo, no que rechaza ESTO. Partiendo de una fila que publico
 * el propio {@code publishAdapted}, lo unico que la separa de una fila valida es
 * exactamente el dato que se quiere probar.
 * <p>
 * <b>Segunda: el camino del dominio no sirve para probar la base.</b> El
 * constructor compacto de {@code AdaptationAttempt} replica las invariantes de
 * V32 a proposito —su javadoc lo dice: que una fila incoherente falle al armarse
 * y no al confirmar la transaccion—. Construir el intento por ahi probaria el
 * guardia de Java, no el {@code CHECK} de PostgreSQL. Y el {@code CHECK} existe
 * para los casos en que nadie pasa por el dominio: una sentencia a mano, una
 * migracion, un camino futuro que escriba la tabla directamente. Un
 * {@code UPDATE} nativo es el unico que entra por donde el guardia de Java no
 * llega.
 *
 * <h2>SE COMPRUEBA EL NOMBRE DE LA RESTRICCION, NO QUE "ALGO" FALLARA</h2>
 *
 * Dar por buena cualquier excepcion es el error que hace que una prueba de
 * restricciones no demuestre nada: una clave ajena mal puesta o un NOT NULL
 * olvidado tambien lanzan, y el verde taparia que V33 nunca llego a evaluarse.
 * Cada caso exige que el nombre de SU restriccion aparezca en la cadena de
 * causas.
 *
 * <h2>LA LIMPIEZA SOLO BORRA LO QUE ESTA CLASE CREO</h2>
 *
 * Y hay dos motivos distintos para ello, los dos aprendidos de un defecto real
 * en la version anterior de estas pruebas, que borraba por
 * {@code WHERE user_id = ...}:
 * <ol>
 *   <li><b>Un borrado por usuario se lleva lo que no creo.</b> Mientras el
 *       usuario 1 no tenga nada, no se nota; en cuanto el ciclo semanal deje
 *       evidencia de verdad, una corrida de pruebas la borraria. La evidencia de
 *       la tesis no se puede recuperar.
 *   <li><b>JUnit ejecuta {@code @AfterEach} AUNQUE {@code @BeforeEach} falle.</b>
 *       Es decir: si el cerrojo del entorno no pasa —si la base no se pudo
 *       identificar como la de pruebas— la limpieza corria IGUAL, y borraba
 *       justo en la base que se acababa de rechazar. El cerrojo no solo no
 *       protegia: su fallo disparaba el borrado. De ahi
 *       {@code entornoVerificado}, que es lo ultimo que se pone en
 *       {@code @BeforeEach} y lo primero que mira {@code limpiar()}.
 * </ol>
 * Asi que se anotan los identificadores creados y se borran esos. Si alguna
 * corrida dejara residuo, la siguiente NO lo borra: se niega a empezar y dice
 * que encontro, para que se revise a mano. <b>Fallar fuerte es recuperable;
 * borrar de mas, no.</b>
 *
 * <h2>Como se ejecuta</h2>
 * <pre>
 * mvn "-Dtest=InvariantesV33IT" test ^
 *     "-Dfitsense.pruebas.url=jdbc:postgresql://&lt;host-de-FitSenseTests&gt;/neondb?sslmode=require" ^
 *     "-Dfitsense.pruebas.usuario=neondb_owner" ^
 *     "-Dfitsense.pruebas.clave=&lt;clave&gt;"
 * </pre>
 * Usa semanas DISTINTAS de las de {@code PublicacionAtomicaIT} —16 y 23 de
 * noviembre frente al 2 y el 9— para que las dos clases no puedan pisarse.
 */
@EnabledIfSystemProperty(named = "fitsense.pruebas.url", matches = ".+")
@SpringBootTest
@DisplayName("P2-D · las invariantes de V33 en la base, no en Java")
class InvariantesV33IT {

    private static final long USUARIO = 1L;
    private static final LocalDate SEMANA_MEDIDA = LocalDate.of(2026, 11, 16);
    private static final LocalDate SEMANA_NUEVA = LocalDate.of(2026, 11, 23);
    private static final long EJERCICIO = 55L;

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
    @Autowired private PlanAdaptationAttemptRepository attemptRepository;
    @Autowired private EntityManager em;
    @Autowired private PlatformTransactionManager transactionManager;

    private TransactionTemplate tx;

    /**
     * SOLO se borra lo que figure aqui. En orden de creacion: primero el plan de
     * la semana medida, despues el publicado. La limpieza va al reves, porque la
     * sesion derivada apunta a la de origen con
     * {@code source_planned_workout_id} y esa clave ajena manda.
     */
    private final List<Long> planesCreados = new ArrayList<>();

    /** El unico identificador de {@code weekly_user_metrics} que esta clase crea. */
    private Long metricaCreadaId;

    /**
     * Falso hasta que los dos cerrojos pasan. Lo mira {@code limpiar()} antes de
     * borrar nada: sin esto, un fallo del cerrojo acabaria borrando en la base
     * que el cerrojo no pudo identificar.
     */
    private boolean entornoVerificado;

    private Long planOrigenId;
    private Long sesionOrigenId;

    // =======================================================================
    // Los cerrojos
    // =======================================================================

    @BeforeEach
    void exigirEntornoLimpioDePruebas() {
        tx = new TransactionTemplate(transactionManager);
        planesCreados.clear();
        metricaCreadaId = null;
        planOrigenId = null;
        sesionOrigenId = null;
        entornoVerificado = false;

        // Cerrojo 1: la marca, en una tabla creada A MANO y no por migracion,
        // que por tanto solo existe donde se creo adrede.
        var marcas = enTransaccion(() -> em.createNativeQuery(
                        "SELECT entorno FROM fitsense_entorno WHERE entorno = 'PRUEBAS'")
                .getResultList());

        assertFalse(marcas.isEmpty(),
                "Esta base no lleva la marca PRUEBAS en fitsense_entorno. O no es"
                        + " FitSenseTests, o la marca no se creo. NO se escribe nada.");

        // Cerrojo 2: las dos semanas de esta clase estan libres.
        //
        // Si no lo estan, la prueba NO limpia por su cuenta: avisa y se detiene.
        // Lo que encuentre puede ser residuo de una corrida interrumpida o puede
        // ser un dato real, y desde aqui no se distingue. Borrar por si acaso es
        // lo que convierte un fallo recuperable en una perdida.
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
                        + " usuario " + USUARIO + " ya tienen " + ocupado + " filas (planes,"
                        + " intentos o metricas). Esta prueba NO borra lo que no creo:"
                        + " revisalas a mano y vuelve a ejecutar. Si son residuo de una"
                        + " corrida interrumpida, basta borrar esas filas; si son datos de"
                        + " verdad, cambia las dos fechas de esta clase.");

        // ULTIMA LINEA A PROPOSITO: a partir de aqui, y solo a partir de aqui,
        // limpiar() tiene permiso para borrar.
        entornoVerificado = true;
    }

    // =======================================================================
    // Los dos CHECK de V33
    // =======================================================================

    @Test
    @DisplayName("Un APLICADA sin sesiones aplicadas lo rechaza ck_paa_aplicada_con_sesiones")
    void aplicadaSinSesionesEsRechazada() {
        long intento = publicarIntentoReal();

        // PREMISA EXPLICITA. Si la fila publicada no tuviera exactamente una
        // sesion aplicada, el UPDATE de abajo no estaria probando lo que dice, y
        // conviene que eso falle aqui con un mensaje claro en vez de pasar por
        // casualidad.
        assertEquals(1, cuenta("SELECT cardinality(applied_sessions)"
                        + " FROM plan_adaptation_attempts WHERE attempt_id = " + intento),
                "la fila publicada tenia que traer una sesion aplicada");

        var error = assertThrows(RuntimeException.class, () -> ejecutar(
                "UPDATE plan_adaptation_attempts SET applied_sessions = '{}'::BIGINT[]"
                        + " WHERE attempt_id = " + intento));

        assertTrue(causas(error).contains("ck_paa_aplicada_con_sesiones"),
                "lo rechazo algo que NO es la restriccion de V33, asi que V33 no"
                        + " quedo demostrada. Cadena de causas: " + causas(error));

        // Y la fila sigue como estaba: el rechazo no la dejo a medias.
        assertEquals(1, cuenta("SELECT cardinality(applied_sessions)"
                + " FROM plan_adaptation_attempts WHERE attempt_id = " + intento));
    }

    @Test
    @DisplayName("Un RESPALDO con sesiones aplicadas lo rechaza ck_paa_solo_aplicada_con_sesiones")
    void respaldoConSesionesEsRechazado() {
        long intento = publicarIntentoReal();

        // El origen pasa a CONSERVED EN LA MISMA SENTENCIA, y no es un adorno:
        // ck_paa_procedencia_continuidad exige que un RESPALDO declare origen
        // CONSERVED, asi que sin esto reventaria POR ESA restriccion y V33 no se
        // llegaria a evaluar. Dejar las dos columnas coherentes es lo que obliga
        // a que el unico defecto de la fila sea el que se quiere probar.
        //
        // El disparador tg_paa_coherencia si tiene algo que objetar -el plan
        // publicado dice AI y el intento diria CONSERVED-, pero es DIFERIDO y
        // dispara al COMMIT, mientras que un CHECK es inmediato y dispara en la
        // sentencia. El CHECK llega primero, que es justo lo que se comprueba.
        var error = assertThrows(RuntimeException.class, () -> ejecutar(
                "UPDATE plan_adaptation_attempts"
                        + " SET status = 'RESPALDO', generation_source = 'CONSERVED'"
                        + " WHERE attempt_id = " + intento));

        assertTrue(causas(error).contains("ck_paa_solo_aplicada_con_sesiones"),
                "lo rechazo algo que NO es la restriccion de V33, asi que V33 no"
                        + " quedo demostrada. Cadena de causas: " + causas(error));

        assertEquals("APLICADA", unaFila("SELECT status FROM plan_adaptation_attempts"
                + " WHERE attempt_id = " + intento)[0]);
    }

    // =======================================================================
    // Los arreglos
    // =======================================================================

    @Test
    @DisplayName("Los arreglos bigint[] y varchar(40)[] van y vuelven sin deformarse")
    void losArreglosViajanIntactos() {
        long intento = publicarIntentoReal();

        // IDA. Lo que quedo en la columna es un ARREGLO de PostgreSQL y no un
        // texto que se le parezca: cardinality y el acceso por indice solo
        // funcionan sobre un arreglo de verdad. Es el riesgo real del mapeo
        // SqlTypes.ARRAY, y no se ve desde Java.
        var fila = unaFila("SELECT cardinality(targeted_sessions), targeted_sessions[1],"
                + " cardinality(targeted_interventions), targeted_interventions[1],"
                + " cardinality(applied_sessions), applied_sessions[1],"
                + " cardinality(no_effect_sessions), cardinality(conserved_sessions)"
                + " FROM plan_adaptation_attempts WHERE attempt_id = " + intento);

        assertNotNull(fila, "no quedo la fila de evidencia");
        assertEquals(1, ((Number) fila[0]).intValue(), "senaladas");
        assertEquals(sesionOrigenId, ((Number) fila[1]).longValue());
        assertEquals(1, ((Number) fila[2]).intValue(), "intervenciones");
        assertEquals("ACORTAR_O_REORGANIZAR", fila[3]);
        assertEquals(1, ((Number) fila[4]).intValue(), "aplicadas");
        assertEquals(sesionOrigenId, ((Number) fila[5]).longValue());
        assertEquals(0, ((Number) fila[6]).intValue(), "sin efecto");
        assertEquals(0, ((Number) fila[7]).intValue(), "conservadas");

        // VUELTA por el camino de produccion. findByUserIdOrderByIdAsc es el que
        // el repositorio documenta como "la lectura de la tesis", y al hidratar
        // la entidad Hibernate tiene que convertir los cinco arreglos de la base
        // a Long[] y String[]. Si ese mapeo estuviera mal, esta llamada
        // reventaria: por eso basta con que devuelva la fila.
        //
        // SE BUSCA EL INTENTO POR SU IDENTIFICADOR, y no se exige que la lista
        // tenga una sola fila. Esa tabla ACUMULA a proposito -no tiene clave
        // unica- y en cuanto el ciclo semanal deje evidencia de verdad del
        // usuario 1, exigir una sola fila haria fallar esta prueba por algo que
        // no tiene nada que ver con los arreglos.
        //
        // em.clear() para que la lectura salga de PostgreSQL y no del cache de
        // primer nivel. Esta transaccion es nueva y el cache ya estaria vacio,
        // pero una prueba que se apoya en eso se vuelve falsa el dia que alguien
        // cambie como se envuelven los pasos.
        var filas = enTransaccion(() -> {
            em.clear();
            return attemptRepository.findByUserIdOrderByIdAsc(USUARIO);
        });

        PlanAdaptationAttempt leido = filas.stream()
                .filter(f -> f.getId() != null && f.getId() == intento)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "la lectura de la tesis no trajo el intento " + intento
                                + "; devolvio " + filas.size() + " filas"));

        assertEquals("APLICADA", leido.getStatus().name());
        assertEquals(GenerationSource.AI, leido.getGenerationSource());
        assertNotNull(leido.getCreatedAt(), "created_at lo pone la base y tiene que volver");

        // NOTA HONESTA SOBRE EL ALCANCE: la entidad es de SOLO ESCRITURA y no
        // expone los cinco arreglos, asi que aqui no se comparan sus valores
        // despues de hidratar. Lo que queda demostrado es que la escritura
        // produjo arreglos reales con los valores correctos -arriba, contra la
        // columna- y que la lectura de produccion los hidrata sin romperse. Para
        // comparar tambien los valores de vuelta habria que anadir getters a la
        // entidad, y una prueba no cambia produccion para poder observarse.
    }

    // =======================================================================
    // La fila real de la que parten los tres casos
    // =======================================================================

    /**
     * Siembra la semana medida y publica una adaptacion de verdad con
     * {@code publishAdapted}. Devuelve el {@code attempt_id} de la evidencia.
     * <p>
     * Es el mismo camino que ya quedo demostrado en {@code PublicacionAtomicaIT},
     * asi que si esto falla el problema no esta en V33.
     */
    private long publicarIntentoReal() {
        sembrar();

        var publicado = commandService.publishAdapted(preparado(), publicacion(metricaCreadaId));
        assertTrue(publicado.isPresent(), "no se publico ningun plan");

        Long planNuevo = publicado.get().getId();
        // SE ANOTA ANTES DE CUALQUIER OTRA COSA: lo que no quede anotado aqui no
        // se borra despues, asi que la anotacion va pegada a la creacion.
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

    /**
     * SOLO los identificadores que esta ejecucion creo. Ni un
     * {@code WHERE user_id = ...} en toda la limpieza.
     * <p>
     * El orden lo dictan las claves ajenas: los intentos referencian la metrica
     * y los planes, la metrica referencia el plan de origen, y la sesion
     * derivada apunta a la de origen con {@code source_planned_workout_id}. De
     * ahi que los planes se recorran AL REVES del orden de creacion.
     */
    @AfterEach
    void limpiar() {
        if (!entornoVerificado) {
            // JUnit ejecuta @AfterEach aunque @BeforeEach haya fallado. Sin este
            // return, un cerrojo que no pasa acabaria borrando en la base que no
            // se pudo identificar: el fallo del cerrojo DISPARARIA el borrado.
            return;
        }

        // 1. Los intentos: referencian metrica y planes, asi que van primero.
        for (Long plan : planesCreados)
            ejecutar("DELETE FROM plan_adaptation_attempts"
                    + " WHERE source_plan_id = " + plan + " OR resulting_plan_id = " + plan);

        // 2. La metrica: referencia el plan de origen.
        if (metricaCreadaId != null)
            ejecutar("DELETE FROM weekly_user_metrics"
                    + " WHERE weekly_metric_id = " + metricaCreadaId);

        // 3. Sesiones y ejercicios, del ultimo plan creado al primero.
        for (Long plan : alReves(planesCreados)) {
            ejecutar("DELETE FROM planned_workout_exercises WHERE planned_workout_id IN"
                    + " (SELECT planned_workout_id FROM planned_workouts"
                    + "  WHERE plan_id = " + plan + ")");
            ejecutar("DELETE FROM planned_workouts WHERE plan_id = " + plan);
        }

        // 4. Los planes, en el mismo orden inverso.
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
            // Con UNA sola columna, JPA devuelve el valor suelto y no un arreglo.
            return primera instanceof Object[] columnas
                    ? columnas : new Object[]{primera};
        });
    }

    void ejecutar(String sql) {
        enTransaccion(() -> em.createNativeQuery(sql).executeUpdate());
    }

    /**
     * Toda la cadena de causas en un solo texto. El nombre de la restriccion lo
     * trae el mensaje del driver de PostgreSQL, varias capas por debajo de la
     * excepcion que JPA deja salir, asi que mirar solo la de arriba no sirve.
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
