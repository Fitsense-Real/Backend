package main.web.services.fitsense.planning.infrastructure.generation.ai;

/**
 * Los tramos de una llamada a Replicate, medidos y separados.
 * <p>
 * POR QUE. El 13/10/2026 Juan encontro que {@code fitsense.ai.timeout-seconds}
 * no es el tiempo total de espera: el POST lleva cabecera
 * {@code Prefer: wait=60} y Replicate mantiene esa peticion abierta hasta 60 s;
 * solo si vuelve en curso empieza el sondeo, y ESE es el que dura 150 s. El
 * presupuesto real es 60 + 150 = 210 s, y las tres duraciones del panel lo
 * confirmaron: 159 s entro, 256 s y 284 s no.
 * <p>
 * Mientras el log solo dijera "seguia en curso tras 150 s" no habia forma de
 * saber cuanto se fue en cada tramo, y por lo tanto tampoco de decidir si
 * conviene subir el limite o recortar el prompt. Esta clase existe para
 * responder esa pregunta con numeros.
 * <p>
 * TIEMPO MONOTONICO. Las duraciones se miden con {@code System.nanoTime()} y no
 * con el reloj de pared: un ajuste de hora del sistema -NTP, cambio de horario-
 * no debe producir una duracion negativa ni inflada en una medicion que se va a
 * publicar.
 * <p>
 * NO LLEVA NI PROMPT, NI TOKEN, NI DATO DEL PARTICIPANTE. Solo identificadores
 * y numeros.
 */
public record ReplicateTiming(
        /** Usuario y semana de la generacion, para atar el tiempo a su plan. */
        Long userId,
        short weekNumber,
        /**
         * Intento dentro de ESA generacion. Un mismo plan puede provocar varios,
         * y sin esto dos lineas de tiempo del mismo usuario son indistinguibles.
         * 0 cuando quien llama no lo sabe.
         */
        int attempt,
        /**
         * Id de la prediccion. {@code null} si el POST fallo antes de recibirlo:
         * entonces se escribe "desconocido" y NO se inventa nada.
         */
        String predictionId,
        long postMs,
        long pollingMs,
        int pollingRequests,
        Resultado resultado
) {

    /**
     * Por que son seis y no cuatro. Hasta el 13/10/2026 TRES caminos distintos
     * se registraban como TIMEOUT, porque todos dejaban la prediccion en estado
     * "pending": agotar el limite, que fallara una consulta de sondeo, y que se
     * interrumpiera el hilo. Un fallo de red a los tres segundos quedaba escrito
     * igual que una espera de 150 s, y sobre esa confusion no se puede decidir
     * si conviene ampliar el limite. Juan lo encontro leyendo awaitCompletion.
     */
    public enum Resultado {
        /** El modelo entrego una propuesta. */
        SUCCEEDED,
        /**
         * SE AGOTO EL LIMITE con la prediccion todavia viva. Es el unico caso
         * que dice algo sobre el tiempo de espera configurado.
         */
        TIMEOUT,
        /** Respondio, pero con un estado que no es exito (failed, canceled...). */
        NOT_SUCCEEDED,
        /** El POST no llego a completarse: red, 4xx, 5xx. */
        POST_ERROR,
        /**
         * Una consulta de sondeo fallo, o la prediccion llego en curso y sin
         * urls.get al que preguntar. NO es un timeout: puede haber ocurrido a
         * los tres segundos.
         */
        POLL_ERROR,
        /**
         * El hilo se interrumpio durante el sondeo: la aplicacion se esta
         * parando. Importa tenerlo aparte justo en una serie donde el backend se
         * reinicia entre perfiles.
         */
        INTERRUPTED
    }

    public long totalMs() {
        return postMs + pollingMs;
    }

    /** Lo que va al log, en una linea y con claves estables para poder filtrarlo. */
    public String describe() {
        return ("REPLICATE_TIMING user=%s week=%d attempt=%s prediction_id=%s "
                + "post_ms=%d polling_ms=%d polling_requests=%d total_ms=%d resultado=%s")
                .formatted(
                        userId == null ? "desconocido" : userId,
                        weekNumber,
                        attempt <= 0 ? "desconocido" : String.valueOf(attempt),
                        predictionId == null || predictionId.isBlank()
                                ? "desconocido" : predictionId,
                        postMs, pollingMs, pollingRequests, totalMs(), resultado);
    }

    /** Milisegundos entre dos marcas de {@code System.nanoTime()}, nunca negativos. */
    public static long msDesde(long nanoInicio, long nanoFin) {
        long delta = nanoFin - nanoInicio;
        return delta < 0 ? 0 : delta / 1_000_000L;
    }
}