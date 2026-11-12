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
        int pollReadTimeoutSeconds
) {

    /** Por omision, 10 s para conectar. */
    public static final int CONNECT_POR_OMISION = 10;
    /** Lectura del POST: lo que Replicate retiene la peticion (Prefer: wait) mas este margen. */
    public static final int MARGEN_LECTURA_POST = 30;
    /** Lectura de cada GET de sondeo (y de la cancelacion). */
    public static final int LECTURA_SONDEO_POR_OMISION = 30;

    /** La forma anterior, sin los limites HTTP: quedan los de omision. */
    public ReplicateProperties(boolean enabled, String apiToken, String baseUrl, String model,
                               String reasoningEffort, int timeoutSeconds, int preferWaitSeconds) {
        this(enabled, apiToken, baseUrl, model, reasoningEffort, timeoutSeconds, preferWaitSeconds, 0, 0, 0);
    }

    /**
     * Con dos constructores, Spring Boot necesita saber con cual enlazar las propiedades: el canonico (R-3; sin esta
     * marca el contexto no arranca, "No default constructor found").
     */
    @org.springframework.boot.context.properties.bind.ConstructorBinding
    public ReplicateProperties {
        if (baseUrl == null || baseUrl.isBlank())
            baseUrl = "https://api.replicate.com/v1/models/openai/gpt-5-structured/predictions";
        if (model == null || model.isBlank()) model = "gpt-5-nano";
        if (reasoningEffort == null || reasoningEffort.isBlank()) reasoningEffort = "medium";
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
    }

    /**
     * Cota de la espera total de UNA llamada, para documentarla y probarla: conectar y leer el POST, el sondeo
     * entero y, como mucho, un GET mas que empiece justo antes del limite (con su conexion y la pausa de 3 s).
     */
    public long esperaMaximaSegundos() {
        return (long) connectTimeoutSeconds + readTimeoutSeconds
                + timeoutSeconds + 3 + connectTimeoutSeconds + pollReadTimeoutSeconds;
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
