package main.web.services.fitsense.planning.application.internal.generation;

import main.web.services.fitsense.planning.domain.model.aggregates.PlanGenerationOperation;
import main.web.services.fitsense.planning.domain.model.valueobjects.GenerationOperationStatus;
import main.web.services.fitsense.planning.infrastructure.generation.ai.DiarioDePredicciones;
import main.web.services.fitsense.planning.infrastructure.generation.ai.GeneracionInterrumpidaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * V66 · EL SEGUIMIENTO V59 EN LOS CAMINOS DE IA QUE NO LO TENIAN: la generacion SINCRONA del primer plan y el CIERRE
 * SEMANAL (generacion o adaptacion de la semana nueva).
 * <p>
 * No es una arquitectura nueva: usa la MISMA tabla de operaciones, el MISMO propietario con lease, el MISMO fencing y
 * el MISMO libro de predicciones que la generacion inicial asincrona ({@link PlanGenerationOperationService}). Lo unico
 * nuevo es la IDENTIDAD LOGICA ({@code operation_key}): una peticion o un ciclo no tienen una operacion que la app
 * consulte, asi que su identidad es la de lo que se genera ({@link Clave}: tipo, usuario y semanas).
 * <ol>
 *   <li><b>Antes de llamar a la IA</b> se abre (o se ADOPTA) la operacion de esa clave, con este proceso como
 *   propietario: una fila PROCESSING con lease. Una sola por clave en curso ({@code ux_pgo_clave_en_curso}).</li>
 *   <li>El trabajo corre con el libro de ESA operacion ({@link DiarioDePredicciones#durante}): cada prediccion se anota
 *   antes de enviarla y su id en cuanto llega.</li>
 *   <li>Mientras dura, un hilo propio renueva el lease (latido). Si el proceso muere, deja de latir.</li>
 *   <li><b>Reinicio:</b> la siguiente ejecucion de la MISMA clave (la persona repite la peticion; el barrido relanza el
 *   ciclo) encuentra la operacion con el lease vencido y la adopta: el libro reanuda cada prediccion por su id, reutiliza
 *   la salida ya recibida o repite el desenlace registrado. Un envio sin id confirmado (NO_CONFIRMADA) no se reenvia.
 *   Nunca se recupera una prediccion por contenido (PR #88).</li>
 *   <li>Al terminar se cierra la operacion (COMPLETED con su plan, NO_FACTIBLE sin plan, o FAILED con el motivo del
 *   clasificador), solo si sigue siendo de este proceso.</li>
 * </ol>
 * Limites: {@code max-executions} adopciones; despues, FAILED EJECUCION_INTERRUMPIDA. Una huerfana sincrona se puede
 * adoptar durante {@code sync-orphan-seconds}; un ciclo, durante {@code weekly-recovery-hours}. Nada espera para
 * siempre.
 */
@Service
public class SeguimientoOperacionesIa {

    private static final Logger log = LoggerFactory.getLogger(SeguimientoOperacionesIa.class);
    private static final int MAX_MENSAJE = 500;

    /** Los caminos nuevos. La generacion inicial asincrona (FIRST_PLAN) sigue en {@link PlanGenerationOperationService}. */
    public enum Tipo {
        FIRST_PLAN_SYNC(PlanGenerationOperation.KIND_FIRST_PLAN_SYNC),
        WEEKLY_CYCLE(PlanGenerationOperation.KIND_WEEKLY_CYCLE);

        final String kind;

        Tipo(String kind) {
            this.kind = kind;
        }
    }

    /**
     * La identidad logica de lo que se genera. Dos ejecuciones con la misma clave son LA MISMA operacion (la segunda la
     * reanuda); dos usuarios, o dos semanas, nunca comparten clave.
     */
    public record Clave(Tipo tipo, long userId, LocalDate semana, LocalDate semanaMedida) {

        public static Clave primerPlanSincrono(long userId, LocalDate semana) {
            return new Clave(Tipo.FIRST_PLAN_SYNC, userId, semana, null);
        }

        public static Clave cicloSemanal(long userId, LocalDate semanaMedida, LocalDate semanaNueva) {
            return new Clave(Tipo.WEEKLY_CYCLE, userId, semanaNueva, semanaMedida);
        }

        public String texto() {
            return tipo.kind + ":u=" + userId + (semanaMedida == null ? "" : ":medida=" + semanaMedida)
                    + ":semana=" + semana;
        }
    }

    /** Otra ejecucion VIVA (otro hilo u otra instancia) tiene la operacion de esta clave: no se hace nada. */
    public static final class EnCursoEnOtroProceso extends RuntimeException {
        public EnCursoEnOtroProceso(String clave) {
            super("la operacion " + clave + " ya esta en curso en otra ejecucion");
        }
    }

    /** Un cierre semanal interrumpido, para que el barrido lo relance. */
    public record CicloHuerfano(UUID operationId, long userId, LocalDate semanaMedida, LocalDate semanaNueva,
                                int ejecuciones) {}

    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final Duration lease;
    private final int maxEjecuciones;
    private final Duration ventanaCiclo;
    private final Duration ventanaSincrona;
    private final String propietario;
    private final Set<UUID> enCurso = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService latido;
    private DiarioDePredicciones diario;

    public SeguimientoOperacionesIa(JdbcTemplate jdbc,
                                    ObjectProvider<Clock> clock,
                                    @Value("${fitsense.plan-generation.lease-seconds:90}") long leaseSeconds,
                                    @Value("${fitsense.plan-generation.max-executions:3}") int maxEjecuciones,
                                    @Value("${fitsense.ai-operations.weekly-recovery-hours:24}") long ventanaCicloHoras,
                                    @Value("${fitsense.ai-operations.sync-orphan-seconds:1800}") long ventanaSincronaSegundos) {
        this.jdbc = jdbc;
        this.clock = clock.getIfAvailable(Clock::systemUTC);
        this.lease = Duration.ofSeconds(Math.max(1, leaseSeconds));
        this.maxEjecuciones = Math.max(1, maxEjecuciones);
        this.ventanaCiclo = Duration.ofHours(Math.max(1, ventanaCicloHoras));
        this.ventanaSincrona = Duration.ofSeconds(Math.max(1, ventanaSincronaSegundos));
        this.propietario = propietarioDeEsteProceso();
        // El latido en un hilo PROPIO (como el worker V59): el planificador de Spring tiene un solo hilo y lo ocupa el
        // propio cierre semanal; con el latido alli, un ciclo largo dejaria de latir y se tomaria por huerfano.
        this.latido = Executors.newSingleThreadScheduledExecutor(r -> {
            var t = new Thread(r, "operaciones-ia-latido");
            t.setDaemon(true);
            return t;
        });
        long periodo = Math.max(1000, this.lease.toMillis() / 3);
        latido.scheduleWithFixedDelay(this::latir, periodo, periodo, TimeUnit.MILLISECONDS);
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setDiario(DiarioDePredicciones diario) {
        this.diario = diario;
    }

    @jakarta.annotation.PreDestroy
    public void parar() {
        latido.shutdownNow();
    }

    public String propietario() {
        return propietario;
    }

    private static String propietarioDeEsteProceso() {
        String host;
        try {
            host = java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            host = "desconocido";
        }
        if (host.length() > 40) host = host.substring(0, 40);
        return host + "-ia-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private OffsetDateTime ahora() {
        return OffsetDateTime.now(clock);
    }

    // ------------------------------------------------------------------ ejecucion

    /**
     * Ejecuta {@code cuerpo} dentro de la operacion de {@code clave}, con su libro de predicciones.
     *
     * @param planDe  el plan publicado a partir del resultado (null si no hubo): COMPLETED o NO_FACTIBLE
     * @param cuerpo  recibe la GUARDA de fencing: lanza si la operacion ya no es de este proceso. El libro la ejecuta
     *                antes de cada prediccion; el llamador la ejecuta, ademas, dentro de la transaccion de publicacion.
     * @throws EnCursoEnOtroProceso si otra ejecucion viva tiene esta clave (no se hace nada)
     */
    public <T> T ejecutar(Clave clave, Function<T, Long> planDe, Function<Runnable, T> cuerpo) {
        var operacion = reclamar(clave);
        enCurso.add(operacion);
        Runnable guarda = () -> asegurarPropietario(operacion);
        try {
            guarda.run();
            T resultado = DiarioDePredicciones.durante(operacion, clave.userId(), guarda, () -> cuerpo.apply(guarda));
            Long plan = planDe.apply(resultado);
            if (plan != null) cerrar(operacion, GenerationOperationStatus.COMPLETED, plan, null, null, false);
            else cerrar(operacion, GenerationOperationStatus.NO_FACTIBLE, null, "SIN_PLAN",
                    "La ejecucion termino sin publicar un plan.", false);
            return resultado;
        } catch (GeneracionInterrumpidaException | OperationAbandonedException e) {
            // Apagado con una prediccion en curso, o la operacion ya no es nuestra: queda como esta (PROCESSING para
            // que la adopte la siguiente ejecucion de la misma clave, o cerrada por quien la tenga).
            log.warn("OPERACION_IA_SIN_CERRAR operacion={} clave={}: {}", operacion, clave.texto(), e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            var o = GenerationFailureClassifier.classify(e);
            cerrar(operacion, o.status(), null, o.reasonCode(), o.message(), o.retryable());
            throw e;
        } catch (Error e) {
            // Un Error tambien la cierra: si quedara PROCESSING con el lease vivo, la siguiente ejecucion de la misma
            // clave la tomaria por otra en curso.
            var o = GenerationFailureClassifier.classify(e);
            cerrar(operacion, o.status(), null, o.reasonCode(), o.message(), o.retryable());
            throw e;
        } finally {
            enCurso.remove(operacion);
            registrarPredicciones(operacion, clave);
        }
    }

    /** Abre la operacion de la clave, o adopta su huerfana. Atomico frente a otros hilos e instancias. */
    UUID reclamar(Clave clave) {
        var texto = clave.texto();
        for (int intento = 0; intento < 3; intento++) {
            var ahora = ahora();
            // 1. Huerfana (su propietario dejo de latir, o es de ESTE proceso pero ya no la ejecuta nadie, p. ej. tras
            //    un fallo al cerrarla): se adopta con su libro. Atomica: si dos ejecuciones la intentan, solo una gana.
            var adoptada = jdbc.queryForList("""
                    UPDATE plan_generation_operations
                       SET lease_owner = ?, lease_expires_at = ?, started_at = ?, updated_at = ?,
                           executions = executions + 1
                     WHERE operation_key = ? AND status = 'PROCESSING' AND executions < ?
                       AND (lease_expires_at IS NULL OR lease_expires_at < ?
                            OR (lease_owner = ? AND NOT (operation_id = ANY(?))))
                    RETURNING operation_id
                    """, UUID.class, propietario, ahora.plus(lease), ahora, ahora, texto, maxEjecuciones, ahora,
                    propietario, enCurso.toArray(UUID[]::new));
            if (!adoptada.isEmpty()) {
                log.warn("OPERACION_IA_REANUDADA operacion={} clave={} propietario={}: se reanuda con su libro de"
                        + " predicciones (no se pide otra)", adoptada.get(0), texto, propietario);
                return adoptada.get(0);
            }
            // 2. Huerfana sin adopciones restantes: se cierra; esta ejecucion abre una nueva (la pidio alguien).
            var interrumpida = GenerationFailureClassifier.interrupted();
            jdbc.update("""
                    UPDATE plan_generation_operations
                       SET status = 'FAILED', reason_code = ?, message = ?, retryable = TRUE, finished_at = ?,
                           updated_at = ?, lease_owner = NULL, lease_expires_at = NULL
                     WHERE operation_key = ? AND status = 'PROCESSING'
                       AND (lease_expires_at IS NULL OR lease_expires_at < ?) AND executions >= ?
                    """, interrumpida.reasonCode(), interrumpida.message(), ahora, ahora, texto, ahora, maxEjecuciones);
            // 3. En curso y viva: es de otra ejecucion. No se hace nada.
            Integer vivas = jdbc.queryForObject("SELECT count(*) FROM plan_generation_operations WHERE operation_key = ?"
                    + " AND status IN ('PENDING', 'PROCESSING')", Integer.class, texto);
            if (vivas != null && vivas > 0) throw new EnCursoEnOtroProceso(texto);
            // 4. Nueva.
            var id = UUID.randomUUID();
            try {
                jdbc.update("""
                        INSERT INTO plan_generation_operations (operation_id, user_id, kind, status, week_start_date,
                               measured_week_start, operation_key, retryable, executions, created_at, started_at,
                               updated_at, lease_owner, lease_expires_at, first_started_at)
                        VALUES (?, ?, ?, 'PROCESSING', ?, ?, ?, FALSE, 1, ?, ?, ?, ?, ?, ?)
                        """, id, clave.userId(), clave.tipo().kind, clave.semana(), clave.semanaMedida(), texto, ahora,
                        ahora, ahora, propietario, ahora.plus(lease), ahora);
                log.info("OPERACION_IA_ABIERTA operacion={} clave={} propietario={}", id, texto, propietario);
                return id;
            } catch (DuplicateKeyException carrera) {
                // Otra ejecucion abrio la misma clave entre la consulta y el INSERT: se vuelve a mirar.
                log.info("OPERACION_IA_CARRERA clave={}: otra ejecucion la abrio a la vez", texto);
            }
        }
        throw new EnCursoEnOtroProceso(texto);
    }

    /** Fencing: lanza si la operacion ya no es PROCESSING de este proceso. FOR UPDATE dentro de una transaccion. */
    void asegurarPropietario(UUID operacion) {
        var fila = jdbc.queryForList("SELECT status, lease_owner FROM plan_generation_operations"
                + " WHERE operation_id = ? FOR UPDATE", operacion);
        if (fila.isEmpty() || !GenerationOperationStatus.PROCESSING.name().equals(fila.get(0).get("status"))
                || !propietario.equals(fila.get(0).get("lease_owner")))
            throw new OperationAbandonedException("la operacion " + operacion + " ya no es de esta ejecucion (" + fila
                    + "): no se envia ni se publica");
    }

    private void cerrar(UUID operacion, GenerationOperationStatus estado, Long plan, String motivo, String mensaje,
                        boolean reintentable) {
        var ahora = ahora();
        var texto = mensaje == null ? null : mensaje.substring(0, Math.min(MAX_MENSAJE, mensaje.length()));
        int n = jdbc.update("""
                UPDATE plan_generation_operations
                   SET status = ?, plan_id = ?, reason_code = ?, message = ?, retryable = ?, finished_at = ?,
                       updated_at = ?, lease_owner = NULL, lease_expires_at = NULL
                 WHERE operation_id = ? AND status = 'PROCESSING' AND lease_owner = ?
                """, estado.name(), plan, motivo, texto, reintentable, ahora, ahora, operacion, propietario);
        if (n == 0)
            log.warn("OPERACION_IA_NO_CERRADA operacion={} estado={}: ya no era de esta ejecucion", operacion, estado);
        else log.info("OPERACION_IA_CERRADA operacion={} estado={} plan={} motivo={}", operacion, estado, plan, motivo);
    }

    /** Renueva el lease de las operaciones de este proceso. Nunca lanza. */
    void latir() {
        if (enCurso.isEmpty()) return;
        try {
            var ahora = ahora();
            jdbc.update("UPDATE plan_generation_operations SET updated_at = ?, lease_expires_at = ?"
                            + " WHERE status = 'PROCESSING' AND operation_id = ANY(?) AND lease_owner = ?",
                    ahora, ahora.plus(lease), enCurso.toArray(UUID[]::new), propietario);
        } catch (RuntimeException e) {
            log.warn("OPERACION_IA_LATIDO fallo: {}", e.getMessage());
        }
    }

    private void registrarPredicciones(UUID operacion, Clave clave) {
        if (diario == null) return;
        try {
            var r = diario.recuento(operacion);
            log.info("OPERACION_IA_PREDICCIONES operacion={} clave={} iniciadas={} reutilizadas={} completadas={}"
                            + " fallidas={} canceladas={} vencidas={} no_confirmadas={} descartadas={}", operacion,
                    clave.texto(), r.iniciadas(), r.reutilizadas(), r.completadas(), r.fallidas(), r.canceladas(),
                    r.vencidas(), r.noConfirmadas(), r.descartadas());
        } catch (RuntimeException e) {
            log.warn("No se pudo contar las predicciones de la operacion {}: {}", operacion, e.getMessage());
        }
    }

    // ------------------------------------------------------------------ recuperacion

    /** Los cierres semanales interrumpidos que aun se pueden relanzar (lease vencido, adopciones y ventana restantes). */
    public List<CicloHuerfano> ciclosHuerfanos() {
        var ahora = ahora();
        return jdbc.query("""
                        SELECT operation_id, user_id, measured_week_start, week_start_date, executions
                          FROM plan_generation_operations
                         WHERE kind = 'WEEKLY_CYCLE' AND status = 'PROCESSING'
                           AND (lease_expires_at IS NULL OR lease_expires_at < ?) AND executions < ?
                           AND created_at >= ?
                         ORDER BY created_at LIMIT 100
                        """,
                (rs, i) -> new CicloHuerfano(rs.getObject(1, UUID.class), rs.getLong(2),
                        rs.getObject(3, LocalDate.class), rs.getObject(4, LocalDate.class), rs.getInt(5)),
                ahora, maxEjecuciones, ahora.minus(ventanaCiclo));
    }

    /**
     * Cierra las huerfanas que ya no se van a reanudar: sin adopciones restantes (EJECUCION_INTERRUMPIDA), o fuera de su
     * ventana (TIEMPO_EXCEDIDO: un ciclo de mas de {@code weekly-recovery-hours}, una peticion sincrona de mas de
     * {@code sync-orphan-seconds}). Su libro queda como evidencia; nada espera para siempre.
     */
    public int cerrarVencidas() {
        var ahora = ahora();
        var interrumpida = GenerationFailureClassifier.interrupted();
        var excedida = GenerationFailureClassifier.timedOut();
        int sinAdopciones = jdbc.update("""
                UPDATE plan_generation_operations
                   SET status = 'FAILED', reason_code = ?, message = ?, retryable = TRUE, finished_at = ?,
                       updated_at = ?, lease_owner = NULL, lease_expires_at = NULL
                 WHERE kind <> 'FIRST_PLAN' AND status = 'PROCESSING'
                   AND (lease_expires_at IS NULL OR lease_expires_at < ?) AND executions >= ?
                """, interrumpida.reasonCode(), interrumpida.message(), ahora, ahora, ahora, maxEjecuciones);
        int fueraDeVentana = jdbc.update("""
                UPDATE plan_generation_operations
                   SET status = 'FAILED', reason_code = ?, message = ?, retryable = TRUE, finished_at = ?,
                       updated_at = ?, lease_owner = NULL, lease_expires_at = NULL
                 WHERE kind <> 'FIRST_PLAN' AND status = 'PROCESSING'
                   AND (lease_expires_at IS NULL OR lease_expires_at < ?)
                   AND ((kind = 'WEEKLY_CYCLE' AND created_at < ?)
                     OR (kind = 'FIRST_PLAN_SYNC' AND coalesce(first_started_at, started_at) < ?))
                """, excedida.reasonCode(), excedida.message(), ahora, ahora, ahora, ahora.minus(ventanaCiclo),
                ahora.minus(ventanaSincrona));
        if (sinAdopciones + fueraDeVentana > 0)
            log.warn("OPERACION_IA_VENCIDAS {} sin adopciones restantes, {} fuera de su ventana", sinAdopciones,
                    fueraDeVentana);
        return sinAdopciones + fueraDeVentana;
    }
}
