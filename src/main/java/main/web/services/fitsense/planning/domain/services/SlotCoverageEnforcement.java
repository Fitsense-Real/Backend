package main.web.services.fitsense.planning.domain.services;

/**
 * Como se trata un incumplimiento de cobertura 4C. Separa la IMPLEMENTACION de
 * su HABILITACION: el codigo entra entero y lo que decide si bloquea es una
 * propiedad, no un despliegue.
 * <p>
 * Se configura con {@code fitsense.coverage.enforcement}.
 */
public enum SlotCoverageEnforcement {

    /**
     * No se mira. Deja el sistema exactamente como estaba antes de este cambio.
     * Es la salida de emergencia si la cobertura rompe algo en produccion a
     * mitad del piloto.
     */
    OFF,

    /**
     * Se cuenta y se registra, pero NO se rechaza. Sirve para medir con
     * participantes reales cuantos planes incumplirian 4C antes de decidir
     * bloquearlos. Es lo que hay que usar durante al menos una ronda.
     */
    OBSERVE,

    /**
     * Se rechaza el borrador de la IA, que tiene tres
     * intentos y recibe el motivo; NO se rechaza el del motor de reglas, que no
     * tiene reintento y cuyo rechazo deja la semana sin plan.
     * <p>
     * LA RAZON, MEDIDA. El motor de reglas elige por {@code body_part} y no sabe
     * de patrones de movimiento. Con el conjunto elegible REAL del perfil
     * HOME/BEGINNER del plan 3 genera 90 semanas, y las que cubren los cuatro
     * slots son:
     * <pre>
     *   sin la V29 (259 elegibles, 24/13/51/12) : 20 de 90   22,2 %
     *   con la V29 (230 elegibles, 24/13/45/11) : 30 de 90   33,3 %
     * </pre>
     * Rechazar tambien su borrador significa que, cada vez que Replicate falle,
     * entre dos y cuatro de cada cinco participantes se quedan SIN PLAN y su
     * adherencia sera NULL. El propio pipeline llama a eso perdida de datos
     * irrecuperable.
     * <p>
     * Dicho de otra forma: el rechazo sirve para ENSENAR a la IA, no para
     * castigar al respaldo. Cuando el selector de reglas sepa de patrones, este
     * modo deja de ser necesario y se pasa a
     * {@link #ENFORCE_WHEN_SATISFIABLE}.
     * <p>
     * ES EL PRIMER PASO RECOMENDADO, NO EL VALOR POR OMISION. Lo que se entrega
     * arranca en {@link #OBSERVE}: la implementacion entra entera y empezar a
     * rechazar planes es una decision del estudio, no un efecto de haber
     * escrito la cuenta.
     */
    ENFORCE_AI_ONLY,

    /**
     * Se rechaza cualquier borrador -IA y reglas- si cumplirlo era posible: el
     * slot tiene ejercicios elegibles y la semana tiene posiciones suficientes.
     * Si no era posible, se registra como en OBSERVE.
     * <p>
     * Es lo que pide 4C §5 sin matices, y es tambien lo que hoy deja al 66,7 %
     * de las semanas de respaldo sin plan (ver {@link #ENFORCE_AI_ONLY}). No
     * activarlo hasta que el motor de reglas cubra los slots por construccion.
     */
    ENFORCE_WHEN_SATISFIABLE,

    /**
     * Se rechaza siempre que falte un slot cubierto por el conjunto elegible,
     * aunque la semana no tenga posiciones para todos.
     * <p>
     * NO USAR sin haber medido antes en OBSERVE. Con dos sesiones de dos
     * ejercicios -lo que deja REDUCE_VOLUME con REDUCE_DAYS- cuatro slots no
     * caben, y este modo convierte ese caso en una semana sin plan.
     */
    ENFORCE_ALWAYS;

    public boolean mide() {
        return this != OFF;
    }

    public boolean bloquea() {
        return this == ENFORCE_AI_ONLY
                || this == ENFORCE_WHEN_SATISFIABLE
                || this == ENFORCE_ALWAYS;
    }

    /**
     * Si este modo bloquea el borrador de un generador concreto.
     * <p>
     * Es el unico sitio donde el origen del borrador cambia la decision, y esta
     * aqui para que se vea en una linea en vez de repartido por el validador.
     */
    public boolean bloqueaA(
            main.web.services.fitsense.planning.domain.model.valueobjects.GenerationSource origen) {
        if (!bloquea()) return false;
        if (this != ENFORCE_AI_ONLY) return true;
        // Sin origen declarado se asume lo prudente: no bloquear.
        return origen == main.web.services.fitsense.planning.domain.model
                .valueobjects.GenerationSource.AI;
    }

    /**
     * Tolerante con el valor de la propiedad. Ante la duda NO bloquea: ni un
     * valor ausente ni uno mal escrito deben empezar a rechazar planes por su
     * cuenta. Quien quiera bloquear lo escribe.
     */
    public static SlotCoverageEnforcement parse(String valor) {
        if (valor == null || valor.isBlank()) return OBSERVE;
        try {
            return valueOf(valor.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return OBSERVE;
        }
    }
}
