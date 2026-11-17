package main.web.services.fitsense.planning.infrastructure.generation.ai;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * V59 · LIBRO DE PREDICCIONES de una operacion de generacion asincrona.
 * <p>
 * Una OPERACION de FitSense (la peticion del usuario: «genera mi primera rutina») puede necesitar varias PREDICCIONES
 * de Replicate (cada intento del pipeline, hasta tres). El libro anota cada prediccion ANTES de enviarla y la
 * confirma con su identificador en cuanto Replicate lo devuelve. Si el proceso muere y la operacion se reanuda, el
 * pipeline se repite desde el principio y cada intento consulta primero su fila:
 * <ul>
 *   <li>{@code SUCCEEDED}: se reutiliza la salida guardada. No se llama a Replicate.</li>
 *   <li>{@code ENVIADA}: se sigue ESA prediccion por su identificador (GET), no se crea otra.</li>
 *   <li>{@code SOLICITANDO}: el envio nunca se confirmo. Replicate pudo aceptarlo (y facturarlo) sin que se guardara
 *   el id, y su API no ofrece una clave de idempotencia verificable: NO se reenvia. Pasa a {@code NO_CONFIRMADA} y el
 *   intento cuenta como fallo del proveedor (el pipeline sigue con su respaldo validado, como hoy).</li>
 *   <li>Cualquier otro final ({@code FAILED}, {@code CANCELED}, {@code TIMEOUT}, {@code ERROR_ENVIO},
 *   {@code NO_CONFIRMADA}): se repite el mismo desenlace sin otra prediccion.</li>
 * </ul>
 * Como el pipeline es determinista (el reintento se construye con la propuesta y los motivos del anterior), la
 * repeticion produce las mismas peticiones; la huella {@code request_hash} lo comprueba y, si la peticion cambio
 * (por ejemplo, el perfil), la fila se DESCARTA y se pide otra.
 * <p>
 * Solo actua dentro de un {@link #durante ambito}: la generacion asincrona lo abre; el endpoint sincrono, el ciclo
 * semanal y la adaptacion no, y siguen exactamente igual.
 */
public interface DiarioDePredicciones {

    enum Estado {
        SOLICITANDO, ENVIADA, SUCCEEDED, FAILED, CANCELED, TIMEOUT, NO_CONFIRMADA, ERROR_ENVIO, DESCARTADA;

        boolean terminal() {
            return this != SOLICITANDO && this != ENVIADA;
        }
    }

    record Entrada(long id, UUID operationId, long userId, int attempt, String requestHash, Estado estado,
                   String predictionId, String output, String error, Long reusedFrom) {}

    /** El intento vigente (no DESCARTADA) de esta operacion. */
    Optional<Entrada> deIntento(UUID operationId, int attempt);

    /**
     * Una prediccion de OTRA operacion del MISMO usuario, con la MISMA peticion, terminada con exito o aun en curso, y
     * pedida hace menos de {@code ventana}. Es lo que evita pagar dos veces cuando una operacion vencio con su
     * prediccion en marcha y el usuario reintenta.
     */
    Optional<Entrada> reutilizable(long userId, String requestHash, UUID excepto, Instant desde);

    /** Copia una prediccion de otra operacion como intento de esta (sin llamar a Replicate). */
    Entrada adoptar(Entrada origen, UUID operationId, int attempt, Instant ahora);

    /**
     * Anota la prediccion ANTES de enviarla. Devuelve el id de la fila. Descarta una fila vigente de este intento con
     * OTRA peticion; si ya hay una con la MISMA (otra ejecucion se adelanto), lanza
     * {@link org.springframework.dao.DuplicateKeyException} y no anota nada.
     */
    long solicitar(UUID operationId, long userId, int attempt, String requestHash, Instant ahora);

    /** Replicate devolvio el identificador. */
    void enviada(long fila, String predictionId, Instant ahora);

    /** Se consulto la prediccion {@code consultas} veces (sondeo). */
    void consultada(long fila, int consultas, Instant ahora);

    void terminada(long fila, Estado estado, String output, String error, Instant ahora);

    /** Recuento de las predicciones de una operacion, para costes y trazas. */
    Recuento recuento(UUID operationId);

    record Recuento(int iniciadas, int reutilizadas, int completadas, int fallidas, int canceladas, int vencidas,
                    int noConfirmadas, int descartadas) {
        public int total() {
            return iniciadas + reutilizadas;
        }
    }

    // ------------------------------------------------------------------ ambito

    /**
     * La operacion en curso en este hilo. {@code antesDeEnviar} se ejecuta al empezar CADA intento (antes de leer el
     * libro, reanudar o enviar) y lanza si este worker ya no es el propietario de la operacion (fencing): un worker que
     * perdio su lease no paga otra prediccion, no sondea y no modifica el libro.
     */
    record Ambito(UUID operationId, long userId, Runnable antesDeEnviar) {}

    static <T> T durante(UUID operationId, long userId, Supplier<T> tarea) {
        return durante(operationId, userId, () -> { }, tarea);
    }

    /** Ejecuta {@code tarea} con el libro activo para esta operacion (el pipeline corre en el mismo hilo). */
    static <T> T durante(UUID operationId, long userId, Runnable antesDeEnviar, Supplier<T> tarea) {
        var anterior = AMBITO.get();
        AMBITO.set(new Ambito(operationId, userId, antesDeEnviar));
        try {
            return tarea.get();
        } finally {
            if (anterior == null) AMBITO.remove();
            else AMBITO.set(anterior);
        }
    }

    static Optional<Ambito> actual() {
        return Optional.ofNullable(AMBITO.get());
    }

    ThreadLocal<Ambito> AMBITO = new ThreadLocal<>();
}
