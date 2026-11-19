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
 * P2-D · PLAN Y EVIDENCIA, O NINGUNO DE LOS DOS. · <b>v2 · 21/10/2026</b>
 * <p>
 * Lo que demuestra, contra PostgreSQL de verdad:
 * <ol>
 *   <li>una semana adaptada se publica CON su fila de evidencia, enlazadas;
 *   <li>si la evidencia no entra, <b>el plan tampoco queda</b>.
 * </ol>
 * El segundo es el que importa. Hasta la primera corrida eso solo estaba probado
 * con dobles, que demuestran el orden de las llamadas y no que la transaccion
 * sea una.
 * <p>
 * EL FALLO QUE SE PROVOCA ES REAL, NO ARTIFICIAL: la evidencia declara un
 * {@code weekly_metric_id} que no existe. {@code fk_paa_metric} NO es diferida,
 * asi que revienta en el INSERT del intento, ya con el plan vaciado a la base
 * por el {@code saveAndFlush}. Es exactamente la situacion que la afirmacion
 * niega que pueda dejar rastro.
 * <p>
 * DE PASO EJERCITA V31: {@code materialize} llama a {@code linkSource} con el
 * identificador de una sesion REAL de la semana sembrada, asi que el linaje
 * entre semanas se comprueba sin una prueba aparte.
 *
 * <h2>QUE CAMBIA EN LA v2, Y QUE NO</h2>
 *
 * <b>NO cambia nada de lo que la v1 demostro.</b> Las dos pruebas, la siembra,
 * el fallo provocado y todas las afirmaciones son las mismas. Lo que se corrige
 * es el mecanismo de limpieza, que era inseguro por dos motivos distintos:
 * <ol>
 *   <li><b>Borraba por {@code WHERE user_id = ...}</b>, o sea TODOS los planes y
 *       TODA la evidencia del participante, no solo lo que la prueba creo.
 *       Mientras el usuario 1 no tenga nada no se nota; en cuanto el ciclo
 *       semanal deje evidencia de verdad, una corrida de pruebas la borraria, y
 *       la evidencia de la tesis no se recupera.
 *   <li><b>JUnit ejecuta {@code @AfterEach} AUNQUE {@code @BeforeEach} falle.</b>
 *       El cerrojo del entorno vive en {@code @BeforeEach}, asi que cuando el
 *       cerrojo RECHAZABA la base, la limpieza corria igual y borraba en esa
 *       misma base. El cerrojo no es que no protegiera: su fallo era lo que
 *       disparaba el borrado. Paso de verdad en la corrida del 21/10 a las
 *       10:27, cuando el guardia de URL fallo en las dos pruebas; no hubo
 *       perdida porque el usuario 1 estaba vacio, o sea que el resultado fue
 *       inocuo por el estado de los datos y no por el diseno.
 * </ol>
 * La v1 no se vuelve a ejecutar. Su evidencia de atomicidad sigue valiendo; su
 * limpieza, no.
 *
 * <h2>LA LIMPIEZA SOLO BORRA LO QUE ESTA EJECUCION CREO</h2>
 *
 * Se anotan los identificadores creados y se borran esos: ni un
 * {@code WHERE user_id} en toda la limpieza. Y {@code entornoVerificado} es lo
 * ultimo que se pone en {@code @BeforeEach} y lo primero que mira
 * {@code limpiar()}.
 * <p>
 * Si alguna corrida se interrumpiera entre escribir y anotar, podria dejar
 * residuo. El tercer cerrojo esta para eso: la siguiente corrida NO lo borra,
 * se niega a empezar y dice que encontro. Lo hallado puede ser residuo o puede
 * ser un dato real, y desde la prueba no se distinguen. <b>Fallar fuerte es
 * recuperable; borrar de mas, no.</b>
 *
 * <h2>Como se ejecuta</h2>
 * <pre>
 * mvn "-Dtest=PublicacionAtomicaIT" test ^
 *     "-Dfitsense.pruebas.url=jdbc:postgresql://&lt;host-de-FitSenseTests&gt;/neondb?sslmode=require" ^
 *     "-Dfitsense.pruebas.usuario=neondb_owner" ^
 *     "-Dfitsense.pruebas.clave=&lt;clave&gt;"
 * </pre>
 *
 * <h2>Los cerrojos</h2>
 * <ol>
 *   <li><b>No corre sola.</b> Se llama {@code ...IT}, asi que queda fuera del
 *       patron por defecto de Surefire, y ademas lleva
 *       {@code @EnabledIfSystemProperty}: sin la propiedad no tiene a donde
 *       conectarse y JUnit la salta.
 *   <li><b>No escribe donde no hay marca.</b> Antes de CADA prueba exige la
 *       fila {@code PRUEBAS} en {@code fitsense_entorno}, una tabla creada A
 *       MANO y no por migracion, que por tanto solo existe donde se creo
 *       adrede.
 *   <li><b>No empieza si sus dos semanas no estan libres</b>, y si no lo estan
 *       no borra nada: avisa.
 * </ol>
 * Hubo un cuarto que comparaba la URL recibida con la de
 * {@code application.properties}, dando por hecho que esa era la de produccion.
 * SE QUITO porque esa suposicion resulto falsa: la aplicacion esta configurada
 * contra el proyecto de pruebas, asi que el guardia bloqueaba justo la URL
 * correcta. Un guardia que se apoya en algo que no se ha verificado protege
 * menos que no tenerlo, porque da confianza sin fundamento.
 * <p>
 * El valor del segundo cerrojo depende de UNA condicion que no se puede
 * comprobar desde dentro de PostgreSQL: que la marca exista SOLO en la base de
 * pruebas.
 */
@EnabledIfSystemProperty(named = "fitsense.pruebas.url", matches = ".+")
@SpringBootTest
@DisplayName("P2-D · plan y evidencia, o ninguno de los dos (v2 · 21/10/2026)")
class PublicacionAtomicaIT {

    // SIN webEnvironment = NONE, Y NO ES UN DESCUIDO. Con NONE no hay contexto
    // web, y entonces la autoconfiguracion de Spring Boot no aporta el
    // CorsConfigurationSource que la cadena de filtros de seguridad necesita:
    // el contexto no levanta. El valor por defecto es MOCK, que da contexto web
    // SIN arrancar Tomcat ni ocupar un puerto, que es justo lo que hace falta.

    /** Usuario 1: tiene perfil y NO tiene ningun plan, asi que no choca con ux_plan_active. */
    private static final long USUARIO = 1L;

    /** Semanas sin planes: los 18 que ya hay son todos del 2026-10-12. */
    private static final LocalDate SEMANA_MEDIDA = LocalDate.of(2026, 11, 2);
    private static final LocalDate SEMANA_NUEVA = LocalDate.of(2026, 11, 9);

    /** Ejercicio ACTIVO, de peso corporal y SETS_REPS. */
    private static final long EJERCICIO = 55L;

    @DynamicPropertySource
    static void apuntarAPruebas(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", () -> System.getProperty("fitsense.pruebas.url"));
        registro.add("spring.datasource.username", () -> System.getProperty("fitsense.pruebas.usuario"));
        registro.add("spring.datasource.password", () -> System.getProperty("fitsense.pruebas.clave"));

        // Flyway apagado: la base ya esta en 33 y esta prueba no migra nada.
        registro.add("spring.flyway.enabled", () -> "false");
        // validate se queda: que las entidades cuadren con el esquema de
        // FitSenseTests es parte de lo que se quiere comprobar.
        registro.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        // Sin IA: esta prueba no llama a Replicate.
        registro.add("fitsense.ai.enabled", () -> "false");

        // EL SECRETO JWT, PORQUE application.properties LO TOMA DEL ENTORNO
        // (${JWT_SECRET}) y una ejecucion de Maven desde la consola no lo
        // tiene. No se usa para nada aqui: no hay peticiones HTTP. Pero
        // TokenServiceImpl se construye al levantar el contexto y exige 32
        // bytes para HS256, asi que sin esto la prueba ni empieza.
        //
        // Lleva un '!' a proposito: decodeSecret intenta BASE64 primero y cae a
        // UTF-8 si falla, y un caracter fuera del alfabeto base64 garantiza que
        // se cuenten los bytes del texto y no los de una decodificacion corta.
        registro.add("authorization.jwt.secret",
                () -> "secreto-solo-para-levantar-el-contexto-de-P2-D-no-se-usa!");
    }

    @Autowired private WeeklyTrainingPlanCommandService commandService;
    @Autowired private WeeklyTrainingPlanRepository planRepository;
    @Autowired private EntityManager em;
    @Autowired private PlatformTransactionManager transactionManager;

    /**
     * CADA PASO AUXILIAR EN SU PROPIA TRANSACCION, Y NO {@code @Transactional}
     * EN LA PRUEBA. Dos motivos, y los dos son de fondo:
     * <ul>
     *   <li>una anotacion sobre un metodo de esta misma clase no hace nada: se
     *       invoca a si misma y el proxy de Spring no interviene;
     *   <li>envolver la prueba entera seria peor. {@code publishAdapted} se
     *       uniria a esa transaccion en vez de abrir la suya, y entonces lo que
     *       se comprobaria es el rollback de la prueba, no el del metodo. La
     *       afirmacion que se quiere demostrar desapareceria.
     * </ul>
     * Asi, siembra, limpieza y consultas confirman por su cuenta, y lo que se
     * observa despues de {@code publishAdapted} es lo que de verdad quedo.
     */
    private TransactionTemplate tx;

    /**
     * v2 · SOLO se borra lo que figure aqui. En orden de creacion: primero el
     * plan de la semana medida, despues el publicado. La limpieza va al reves,
     * porque la sesion derivada apunta a la de origen con
     * {@code source_planned_workout_id} y esa clave ajena manda.
     */
    private final List<Long> planesCreados = new ArrayList<>();

    /**
     * v2 · Falso hasta que los cerrojos pasan. Lo mira {@code limpiar()} antes
     * de borrar nada.
     */
    private boolean entornoVerificado;

    private Long planOrigenId;
    private Long sesionOrigenId;
    private Long metricaId;

    // =======================================================================
    // Los cerrojos
    // =======================================================================

    @BeforeEach
    void exigirEntornoDePruebas() {
        tx = new TransactionTemplate(transactionManager);
        planesCreados.clear();
        planOrigenId = null;
        sesionOrigenId = null;
        metricaId = null;
        entornoVerificado = false;

        var marcas = enTransaccion(() -> em.createNativeQuery(
                        "SELECT entorno FROM fitsense_entorno WHERE entorno = 'PRUEBAS'")
                .getResultList());

        assertFalse(marcas.isEmpty(),
                "Esta base no lleva la marca PRUEBAS en fitsense_entorno. O no es"
                        + " FitSenseTests, o la marca no se creo. NO se escribe nada.");

        // v2 · Las dos semanas de esta clase estan libres. Si no lo estan, la
        // prueba NO limpia por su cuenta: avisa y se detiene.
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
    // Lo que se demuestra
    // =======================================================================

    @Test
    @DisplayName("La semana adaptada se publica CON su evidencia, y quedan enlazadas")
    void publicaPlanYEvidenciaJuntos() {
        sembrar();

        var publicado = commandService.publishAdapted(preparado(), publicacion(metricaId));

        assertTrue(publicado.isPresent(), "no se publico ningun plan");
        Long planNuevo = publicado.get().getId();
        // v2 · Se anota pegado a la creacion: lo que no quede anotado no se
        // borra despues.
        planesCreados.add(planNuevo);

        // La fila del plan existe y es la version 2 de esa semana.
        assertEquals(1, cuenta(
                "SELECT COUNT(*) FROM weekly_training_plans WHERE plan_id = " + planNuevo));

        // Y la evidencia existe, apunta a ESE plan y proviene del sembrado.
        var evidencia = unaFila(
                "SELECT resulting_plan_id, source_plan_id, status, generation_source,"
                        + " applied_sessions, targeted_sessions, ai_calls"
                        + " FROM plan_adaptation_attempts WHERE resulting_plan_id = " + planNuevo);

        assertNotNull(evidencia, "se publico el plan y no quedo su fila de evidencia");
        assertEquals(planNuevo, ((Number) evidencia[0]).longValue());
        assertEquals(planOrigenId, ((Number) evidencia[1]).longValue());
        assertEquals("APLICADA", evidencia[2]);
        assertEquals("AI", evidencia[3]);

        // V31: la sesion derivada declara de cual proviene.
        assertEquals(1, cuenta(
                "SELECT COUNT(*) FROM planned_workouts"
                        + " WHERE plan_id = " + planNuevo
                        + " AND source_planned_workout_id = " + sesionOrigenId));
    }

    @Test
    @DisplayName("Si la evidencia no entra, el plan TAMPOCO queda")
    void siLaEvidenciaFallaNoQuedaElPlan() {
        sembrar();

        // Una medicion que no existe. fk_paa_metric no es diferida, asi que el
        // INSERT del intento falla DESPUES de que el plan se vacio a la base.
        long metricaInexistente = 999_999_999L;

        assertThrows(RuntimeException.class,
                () -> commandService.publishAdapted(preparado(), publicacion(metricaInexistente)),
                "la clave ajena tenia que rechazar la evidencia");

        // LO QUE SE VIENE A DEMOSTRAR: ni plan, ni evidencia.
        //
        // v2 · Esta consulta mira la semana entera del participante y no un
        // identificador, y ahora es MAS fuerte que antes, no mas debil: el
        // tercer cerrojo ya comprobo que esa semana estaba vacia al empezar, asi
        // que un cero aqui solo puede significar que el rollback se llevo el
        // plan. Antes, un plan preexistente en esa semana habria hecho fallar
        // esta afirmacion por un motivo que no tiene nada que ver.
        assertEquals(0, cuenta(
                        "SELECT COUNT(*) FROM weekly_training_plans"
                                + " WHERE user_id = " + USUARIO
                                + " AND week_start_date = DATE '" + SEMANA_NUEVA + "'"),
                "quedo un plan adaptado sin su evidencia: la transaccion no es una");

        assertEquals(0, cuenta(
                "SELECT COUNT(*) FROM plan_adaptation_attempts"
                        + " WHERE source_plan_id = " + planOrigenId));

        // Y el plan de la semana medida sigue intacto: el rollback no se lleva
        // por delante lo que ya estaba.
        assertEquals(1, cuenta(
                "SELECT COUNT(*) FROM weekly_training_plans WHERE plan_id = " + planOrigenId));
    }

    // =======================================================================
    // Siembra y limpieza
    // =======================================================================

    /**
     * La semana medida: un plan con una sesion y un ejercicio, mas su fila de
     * metricas. Se crea con el dominio real, no con SQL, para que la prueba
     * use el mismo camino que produccion.
     */
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

            // La fila de metricas, con las SEIS columnas que la exigen sin valor
            // por defecto -user_id, week_number, las dos fechas, has_active_plan
            // y calculation_version- mas el plan y la adherencia, que hacen
            // falta para ck_wum_null_when_no_plan. Las demas tienen DEFAULT y
            // las que anadio V30 admiten nulo.
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

            metricaId = ((Number) em.createNativeQuery(
                            "SELECT weekly_metric_id FROM weekly_user_metrics"
                                    + " WHERE user_id = ?1 AND week_start_date = ?2")
                    .setParameter(1, USUARIO)
                    .setParameter(2, SEMANA_MEDIDA)
                    .getSingleResult()).longValue();

            return null;
        });
    }

    /**
     * v2 · SOLO los identificadores que esta ejecucion creo. Ni un
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
            // Paso de verdad el 21/10 a las 10:27.
            return;
        }

        // 1. Los intentos: referencian metrica y planes, asi que van primero.
        for (Long plan : planesCreados)
            ejecutar("DELETE FROM plan_adaptation_attempts"
                    + " WHERE source_plan_id = " + plan + " OR resulting_plan_id = " + plan);

        // 2. La metrica: referencia el plan de origen.
        if (metricaId != null)
            ejecutar("DELETE FROM weekly_user_metrics WHERE weekly_metric_id = " + metricaId);

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
        metricaId = null;
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

        // context va en null: publishAdapted no lo usa. Lo que necesita es el
        // mandato, el ajuste, las fechas y la zona.
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
}
