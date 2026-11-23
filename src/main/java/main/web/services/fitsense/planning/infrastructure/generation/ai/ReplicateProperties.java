package main.web.services.fitsense.planning.infrastructure.generation.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion del proveedor de IA.
 * <p>
 * El nombre del modelo es una propiedad y no una constante: cambiar de
 * gpt-5-nano a gpt-5-mini debe ser tocar la configuracion, no recompilar, para
 * que la eleccion del modelo pueda tratarse como una variable del estudio.
 * <p>
 * enabled arranca en false a proposito: el ciclo completo se prueba con el motor
 * de reglas sin gastar llamadas, y la IA se enciende cuando se quiere medir.
 */
@ConfigurationProperties(prefix = "fitsense.ai")
public record ReplicateProperties(
        boolean enabled,
        String apiToken,
        String baseUrl,
        String model,
        String reasoningEffort,
        int timeoutSeconds,
        int preferWaitSeconds,
        /*
         * R-3 (resiliencia de Replicate, 11/11/2026): limites HTTP explicitos. Antes no habia ninguno: RestClient usaba
         * el cliente del JDK sin limite de conexion ni de lectura, y una conexion colgada en el POST o en un GET de
         * sondeo no tenia fin dentro de la aplicacion.
         */
        int connectTimeoutSeconds,
        int readTimeoutSeconds,
        int pollReadTimeoutSeconds,
        /*
         * RENDIMIENTO (12/11/2026): pausa entre dos consultas del sondeo. Eran 3 s fijos: de media 1,5 s entre que la
         * prediccion termina y el backend se entera (medido: 1,9 s de diferencia mediana entre la espera del backend y
         * el predict_time de Replicate en GRA). Con 1 s, 0,5 s de media. Ver docs/FitSense/rendimiento_generacion.
         */
        int pollIntervalMillis,
        /*
         * G5M (23/11/2026): FORMATO de la peticion. "direct" = openai/gpt-5-mini (o cualquier modelo de texto) con
         * system_prompt, prompt, reasoning_effort, verbosity y max_completion_tokens; el esquema viaja en el
         * system_prompt y la salida se valida igual que siempre. "structured" = openai/gpt-5-structured con model y
         * json_schema (la integracion anterior, conservada solo como configuracion reversible: ese endpoint responde
         * E002 para esta cuenta desde el 17/11/2026). Vacio: se deduce de baseUrl (".../gpt-5-structured/..." =
         * structured; cualquier otro = direct).
         */
        String apiFormat,
        /* Solo en direct: low | medium | high. Por omision low (orden G5M). */
        String verbosity,
        /*
         * Solo en direct: tope de tokens de la respuesta, CONTANDO los de razonamiento (gpt-5). Si se agota, la
         * respuesta llega cortada o vacia: el ensamblador la rechaza y cuenta como intento fallido, nunca se acepta.
         */
        int maxCompletionTokens
) {

    /** Las dos formas de peticion que entiende el cliente. */
    public enum Formato {
        DIRECT, STRUCTURED;

        public String etiqueta() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    /** Endpoint por omision desde G5M (23/11/2026). */
    public static final String URL_GPT5_MINI = "https://api.replicate.com/v1/models/openai/gpt-5-mini/predictions";
    /** Tope de salida por omision: el JSON de una semana completa mas el razonamiento con esfuerzo low. */
    public static final int MAX_COMPLETION_TOKENS_POR_OMISION = 16000;

    /** Por omision, 10 s para conectar. */
    public static final int CONNECT_POR_OMISION = 10;
    /** Lectura del POST: lo que Replicate retiene la peticion (Prefer: wait) mas este margen. */
    public static final int MARGEN_LECTURA_POST = 30;
    /** Lectura de cada GET de sondeo (y de la cancelacion). */
    public static final int LECTURA_SONDEO_POR_OMISION = 30;
    /** Pausa del sondeo por omision (antes, 3000 fijos). */
    public static final int INTERVALO_SONDEO_POR_OMISION_MS = 1000;

    /** La forma anterior, sin los limites HTTP: quedan los de omision. */
    public ReplicateProperties(boolean enabled, String apiToken, String baseUrl, String model,
                               String reasoningEffort, int timeoutSeconds, int preferWaitSeconds) {
        this(enabled, apiToken, baseUrl, model, reasoningEffort, timeoutSeconds, preferWaitSeconds, 0, 0, 0);
    }

    /** La forma de R-3, sin la pausa del sondeo: queda la de omision. */
    public ReplicateProperties(boolean enabled, String apiToken, String baseUrl, String model,
                               String reasoningEffort, int timeoutSeconds, int preferWaitSeconds,
                               int connectTimeoutSeconds, int readTimeoutSeconds, int pollReadTimeoutSeconds) {
        this(enabled, apiToken, baseUrl, model, reasoningEffort, timeoutSeconds, preferWaitSeconds,
                connectTimeoutSeconds, readTimeoutSeconds, pollReadTimeoutSeconds, 0);
    }

    /** La forma anterior a G5M, sin formato, verbosidad ni tope de salida: quedan los de omision. */
    public ReplicateProperties(boolean enabled, String apiToken, String baseUrl, String model,
                               String reasoningEffort, int timeoutSeconds, int preferWaitSeconds,
                               int connectTimeoutSeconds, int readTimeoutSeconds, int pollReadTimeoutSeconds,
                               int pollIntervalMillis) {
        this(enabled, apiToken, baseUrl, model, reasoningEffort, timeoutSeconds, preferWaitSeconds,
                connectTimeoutSeconds, readTimeoutSeconds, pollReadTimeoutSeconds, pollIntervalMillis, null, null, 0);
    }

    /**
     * Con dos constructores, Spring Boot necesita saber con cual enlazar las propiedades: el canonico (R-3; sin esta
     * marca el contexto no arranca, "No default constructor found").
     */
    @org.springframework.boot.context.properties.bind.ConstructorBinding
    public ReplicateProperties {
        // G5M (23/11/2026): por omision gpt-5-mini directo. Antes, gpt-5-structured (E002 desde el 17/11/2026).
        if (baseUrl == null || baseUrl.isBlank()) baseUrl = URL_GPT5_MINI;
        if (model == null || model.isBlank()) model = "gpt-5-nano";
        if (reasoningEffort == null || reasoningEffort.isBlank()) reasoningEffort = "medium";
        if (apiFormat == null || apiFormat.isBlank())
            apiFormat = baseUrl.contains("/gpt-5-structured/") ? "structured" : "direct";
        apiFormat = apiFormat.trim().toLowerCase(java.util.Locale.ROOT);
        if (!apiFormat.equals("direct") && !apiFormat.equals("structured"))
            throw new IllegalArgumentException("fitsense.ai.api-format debe ser direct o structured: " + apiFormat);
        if (verbosity == null || verbosity.isBlank()) verbosity = "low";
        // Acotado, no rechazado (como los limites de abajo): 1.000-128.000.
        if (maxCompletionTokens <= 0) maxCompletionTokens = MAX_COMPLETION_TOKENS_POR_OMISION;
        maxCompletionTokens = Math.max(1000, Math.min(maxCompletionTokens, 128000));
        // 150 s desde el plan 33: una prediccion tardo mas de 90 s, el proveedor
        // quedo marcado como no disponible y la semana la hizo el motor de
        // reglas sin gastar el segundo intento. El intento valido del plan 31
        // habia tardado unos 100 s.
        if (timeoutSeconds <= 0) timeoutSeconds = 150;
        // Cuanto retiene Replicate el POST (cabecera Prefer: wait) antes de
        // devolver la prediccion en curso, que entonces se sondea. 60 s es el
        // maximo de Replicate y el valor de siempre. Se puede BAJAR para pasar por
        // un proxy o balanceador que corte antes las peticiones abiertas: si el
        // POST se corta, la prediccion sigue corriendo (y cobrandose) y nadie la
        // lee. Detectado el 27/10/2026 con un proxy que cortaba a los 30 s.
        if (preferWaitSeconds <= 0 || preferWaitSeconds > 60) preferWaitSeconds = 60;

        // R-3. Se ACOTAN, no se rechazan, igual que los dos limites de arriba: un valor fuera de rango no puede
        // dejar la IA sin arrancar, pero tampoco puede dejar una espera sin fin.
        // Conectar: 1-60 s.
        if (connectTimeoutSeconds <= 0) connectTimeoutSeconds = CONNECT_POR_OMISION;
        connectTimeoutSeconds = Math.min(connectTimeoutSeconds, 60);
        // Leer la respuesta del POST: nunca por debajo de lo que Replicate la retiene a proposito (Prefer: wait), o
        // cortariamos una prediccion que iba bien; hasta 5 minutos.
        if (readTimeoutSeconds <= 0) readTimeoutSeconds = preferWaitSeconds + MARGEN_LECTURA_POST;
        readTimeoutSeconds = Math.max(readTimeoutSeconds, preferWaitSeconds + 2);
        readTimeoutSeconds = Math.min(readTimeoutSeconds, 300);
        // Leer cada GET de sondeo: 2-120 s. El sondeo entero lo limita timeoutSeconds.
        if (pollReadTimeoutSeconds <= 0) pollReadTimeoutSeconds = LECTURA_SONDEO_POR_OMISION;
        pollReadTimeoutSeconds = Math.max(2, Math.min(pollReadTimeoutSeconds, 120));
        // Pausa del sondeo: 250 ms - 5 s. Por debajo, solo mas peticiones a Replicate por casi nada; por encima, mas
        // espera tras terminar la prediccion.
        if (pollIntervalMillis <= 0) pollIntervalMillis = INTERVALO_SONDEO_POR_OMISION_MS;
        pollIntervalMillis = Math.max(250, Math.min(pollIntervalMillis, 5000));
    }

    /**
     * Cota de la espera de UNA llamada (un intento), sin la cancelacion: conectar y leer el POST, el sondeo entero y,
     * como mucho, un GET mas que empiece justo antes del limite (con su conexion y la pausa del sondeo). Es por
     * intento: la generacion puede hacer hasta tres, y la acota {@code fitsense.ai.generate.total-budget-seconds}.
     */
    public long esperaMaximaSegundos() {
        return (long) connectTimeoutSeconds + readTimeoutSeconds
                + timeoutSeconds + (pollIntervalMillis + 999) / 1000 + connectTimeoutSeconds + pollReadTimeoutSeconds;
    }

    /**
     * La de {@link #esperaMaximaSegundos()} mas la cancelacion de R-2 (otra conexion y otra lectura), que se pide
     * cuando la prediccion sigue en curso al dejar de esperarla.
     */
    public long esperaMaximaConCancelacionSegundos() {
        return esperaMaximaSegundos() + connectTimeoutSeconds + pollReadTimeoutSeconds;
    }

    private static final java.util.regex.Pattern MODELO_EN_URL =
            java.util.regex.Pattern.compile("/models/[^/]+/([^/]+)/predictions/?$");

    /**
     * El modelo que de verdad responde, para la traza y el plan publicado. En direct lo fija el endpoint
     * ({@code .../models/openai/gpt-5-mini/predictions} = gpt-5-mini) y no {@code model}, que en ese formato no se
     * envia: si ambos discreparan, el plan diria haber usado un modelo que no se llamo. En structured, {@code model}
     * (el endpoint es el envoltorio y el modelo va en la peticion).
     */
    public String modeloEfectivo() {
        if (formato() == Formato.STRUCTURED) return model;
        var m = MODELO_EN_URL.matcher(baseUrl);
        return m.find() ? m.group(1) : model;
    }

    /** El formato de la peticion (ver {@code apiFormat}). */
    public Formato formato() {
        return Formato.valueOf(apiFormat.toUpperCase(java.util.Locale.ROOT));
    }

    /**
     * Solo se considera utilizable si ademas hay token. Sin esta comprobacion,
     * encender la IA sin exportar la variable produciria un 401 por cada
     * participante durante el cierre del lunes.
     */
    public boolean isUsable() {
        return enabled && apiToken != null && !apiToken.isBlank();
    }
}
