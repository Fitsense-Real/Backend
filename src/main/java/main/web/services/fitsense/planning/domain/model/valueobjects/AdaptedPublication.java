package main.web.services.fitsense.planning.domain.model.valueobjects;

import java.math.BigDecimal;

/**
 * Lo que hace falta para publicar una semana adaptada, mas alla de lo que ya
 * trae {@link PreparedPlanGeneration}.
 * <p>
 * EXISTE PARA QUE NO SE RECALCULE NADA. Cada campo de aqui es un dato que el
 * publicador NO debe volver a derivar por su cuenta:
 * <ul>
 *   <li>{@code inputSnapshotJson} es <b>la proyeccion JSON incluida en el
 *       prompt</b>, NO el texto completo que recibio Replicate: ese lleva
 *       ademas las reglas, el bloque de sesiones senaladas y el reintento.
 *       Lo que si permite es reconstruirlo, porque la proyeccion emite
 *       {@code prompt_version} y {@code principles_version}. La fase
 *       3 de generacion inicial hace
 *       {@code jsonSupport.write(PlanInputSnapshot.of(context))}, o sea una
 *       SEGUNDA serializacion. Para adaptacion eso registraria el snapshot
 *       completo cuando lo que viajo fue la proyeccion de P2-A2, y la evidencia
 *       diria que el modelo vio algo que no vio.
 *   <li>{@code sourcePlanId} es el plan de la SEMANA ANTERIOR, del que deriva
 *       esta. No es {@code PreparedPlanGeneration.previousActivePlanId}, que es
 *       un plan activo de ESTA misma semana al que se reemplaza. Confundirlos
 *       dejaria el linaje apuntando al sitio equivocado.
 *   <li>{@code modelName} es null cuando la semana se conservo: no la genero
 *       ningun modelo.
 * </ul>
 *
 * @param resultado     lo que produjo el productor. Tiene que ser publicable.
 * @param decision      la decision de adherencia, para la evidencia de V32.
 * @param sourcePlanId  el plan de la semana anterior.
 * @param weeklyMetricId la medicion de la que salio la decision. Tiene que estar
 *                      CONFIRMADA antes de publicar: la evidencia la referencia.
 * @param inputSnapshotJson la proyeccion JSON que se incrusto en el prompt.
 *                      No es el prompt completo; ver arriba.
 * @param modelName     el modelo que respondio, o null si se conservo.
 * @param aiCalls       cuantas veces se llamo a la IA, de 0 a 4.
 * @param replacementReason por que se reemplaza un plan YA PUBLICADO de esta
 *                      misma semana, cuando la decision no ordena adaptar.
 *                      {@code nextVersionOf} exige un motivo no vacio, y con
 *                      NO_CHANGE o INSUFFICIENT_DATA la orden no trae ninguno
 *                      -{@code PlanAdjustment.none()} deja {@code reason} en
 *                      null-, asi que sin esto el reemplazo fallaria. Se exige
 *                      declararlo en vez de inventar uno: reutilizar un motivo
 *                      de baja adherencia que no existe seria registrar algo
 *                      falso. Puede ser null si no hay plan previo que
 *                      reemplazar o si la decision ordena adaptar.
 */
public record AdaptedPublication(AdaptedWeekResult resultado,
                                 AdaptationDecision decision,
                                 Long sourcePlanId,
                                 Long weeklyMetricId,
                                 String inputSnapshotJson,
                                 String modelName,
                                 short aiCalls,
                                 BigDecimal observedAdherencePct,
                                 BigDecimal registrationCoveragePct,
                                 String replacementReason) {

    public AdaptedPublication {
        if (resultado == null)
            throw new IllegalArgumentException("No hay semana que publicar.");
        if (decision == null)
            throw new IllegalArgumentException(
                    "Sin decision no se puede registrar la evidencia del intento.");
        if (sourcePlanId == null)
            throw new IllegalArgumentException(
                    "Una semana adaptada declara de que plan proviene.");
        if (weeklyMetricId == null)
            throw new IllegalArgumentException(
                    "La evidencia referencia la medicion de la semana.");

        // Un modelo nombrado en una semana conservada diria que la genero el,
        // y la proporcion de IA frente a reglas de la tesis se inflaria.
        if (resultado.source() == GenerationSource.CONSERVED && modelName != null)
            throw new IllegalArgumentException(
                    "Una semana CONSERVED no la genero ningun modelo, y declara " + modelName + ".");
    }

    /**
     * El motivo con el que se publica esta version.
     * <p>
     * Si la decision ordena adaptar, es el de la orden. Si no, el declarado para
     * el reemplazo. Puede ser null cuando no hay plan previo: una primera
     * version no reemplaza nada y no tiene que justificarse.
     */
    public String motivoDePublicacion(PlanAdjustment adjustment) {
        return decision.ordenaAdaptar()
                ? (adjustment == null ? null : adjustment.reason())
                : replacementReason;
    }

    /**
     * La etiqueta que va a {@code adjustment_applied}, o {@code null} si no se
     * ordeno adaptar.
     * <p>
     * Sale de la DECISION, no de {@code adjustment.types()}: esa lista pertenece
     * al mecanismo porcentual antiguo y no representa una intervencion
     * localizada. Solo la leen los recursos REST, como texto -comprobado-, asi
     * que registrar el nombre de la decision no rompe a ningun consumidor.
     * <p>
     * NULL CUANDO NO SE ADAPTO, y eso es lo que mantiene el campo coherente.
     * {@code nextVersionOf} escribe la etiqueta directamente, asi que un
     * reemplazo con NO_CHANGE habria dejado {@code adjustment_applied =
     * "NO_CHANGE"} mientras que una primera version sin adaptar lo deja en
     * null: el mismo hecho -no se adapto- registrado de dos maneras. Asi el
     * campo significa una sola cosa: que adaptacion se aplico, o ninguna.
     */
    public String etiquetaDeAjuste() {
        return decision.ordenaAdaptar() ? decision.name() : null;
    }
}
