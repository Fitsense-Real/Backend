package main.web.services.fitsense.adaptation.domain.services;

import main.web.services.fitsense.adaptation.domain.model.valueobjects.AdaptationOrder;
import main.web.services.fitsense.adaptation.domain.model.valueobjects.AdaptationOutcome;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;

import java.util.ArrayList;
import java.util.Objects;

/**
 * EL MENSAJE DE LA INTERVENCION ({@code user_interventions.message_shown}): LO QUE SE DECIDIO, NO LO QUE PASARA
 * (P-T2/P-T3, integracion MVP).
 * <p>
 * Se escribe al decidir, antes de saber si la IA propondra algo valido, si V8C/V20/V24 lo aceptaran o si la sesion se
 * conservara. Por eso solo puede expresar INTENCION («revisaremos...») y nunca un resultado («bajamos...»). El
 * resultado lo cuenta {@code adjustment_reason} del plan publicado (DescripcionAdaptacion), con lo que cambio de
 * verdad: los dos campos no se contradicen porque no hablan del mismo momento.
 * <p>
 * Antes, {@code message_shown} guardaba el motivo TECNICO de la orden («Adherencia observada 66.67 % (cobertura
 * 100.00 %). Entre 50 % y 80 %: se corrige solo la peor sesion.»). Ese motivo NO SE TOCA: sigue siendo el
 * {@code reason} de la decision, el que llega a planning y al snapshot. Solo cambia el texto para la persona.
 * <p>
 * NO DECIDE NADA y no lee nada que la orden no tenga: no cambia la orden, la intervencion, la adherencia ni el prompt.
 */
public final class MensajeDeIntervencion {

    /** El limite de {@code message_shown} (V6: VARCHAR(500) NOT NULL). */
    public static final int MAXIMO = 500;

    private MensajeDeIntervencion() {
    }

    public static String intencion(AdaptationOrder orden) {
        if (orden == null || orden.outcome() == null)
            return "Esta semana se mantiene tu plan.";
        var partes = new ArrayList<String>();
        // Con datos insuficientes el porcentaje no es fiable (por eso no se adapta): no se muestra.
        if (orden.observedAdherencePct() != null && orden.outcome() != AdaptationOutcome.INSUFFICIENT_DATA)
            partes.add("La semana pasada cumpliste el %.0f %% de lo previsto."
                    .formatted(orden.observedAdherencePct().doubleValue()));
        partes.add(switch (orden.outcome()) {
            case NO_CHANGE -> "Es un buen cumplimiento: mantenemos tu plan.";
            // Los dos casos de AdaptationOrderDecider: sin sesiones senaladas pese a la baja adherencia (el
            // desacuerdo que se resuelve conservando) o, si no, registro insuficiente.
            case INSUFFICIENT_DATA -> orden.reason() != null && orden.reason().contains("ninguna sesion quedo senalada")
                    ? "Mantenemos tu plan: no encontramos una sesión concreta que revisar."
                    : "No tenemos registros suficientes para ajustar tu plan, así que lo mantenemos. Registra tus "
                    + "sesiones para que podamos adaptarlo.";
            case ADAPT_LOCALIZED, ADAPT_EXPANDED -> revision(orden);
        });
        var texto = String.join(" ", partes);
        return texto.length() <= MAXIMO ? texto : texto.substring(0, MAXIMO - 3) + "...";
    }

    private static String revision(AdaptationOrder orden) {
        var tipos = orden.sessions().stream().map(AdaptationOrder.TargetedSession::intervencion)
                .filter(Objects::nonNull).distinct().toList();
        int n = orden.sessions().size();
        var frases = new ArrayList<String>();
        if (n > 1) frases.add("Revisaremos las %d sesiones que nos indicaste.".formatted(n));
        for (var t : tipos) frases.add(frase(t, n));
        if (orden.outcome() == AdaptationOutcome.ADAPT_LOCALIZED && n == 1 && tipos.isEmpty())
            frases.add("Revisaremos la sesión que nos indicaste.");
        frases.add("Si ningún cambio es compatible con tu entrenamiento, la mantendremos como está.");
        return String.join(" ", frases);
    }

    private static String frase(TipoIntervencion t, int n) {
        String sesion = n == 1 ? "esa sesión" : "esas sesiones";
        return switch (t) {
            case VARIANTE_MAS_SENCILLA_O_CARGA -> "Nos dijiste que una sesión te resultó difícil. Revisaremos qué "
                    + "ajustes son compatibles con tu entrenamiento.";
            case SEGURIDAD_ANTE_DOLOR -> "Reportaste molestias. Revisaremos por seguridad los ejercicios de " + sesion
                    + ". Si el dolor continúa, consulta a un profesional de la salud.";
            case ACORTAR_O_REORGANIZAR -> "Nos dijiste que te faltó tiempo. Revisaremos si " + sesion
                    + (n == 1 ? " puede" : " pueden") + " acortarse sin cambiar tus días.";
            case REVISAR_CARGA_Y_RECUPERACION -> "Nos contaste que llegabas con cansancio. Revisaremos la carga de "
                    + sesion + ".";
            case INVESTIGAR_CAMBIO_MINIMO -> "Nos dijiste que te costó encontrar motivación. Buscaremos un cambio "
                    + "pequeño en " + sesion + ".";
            case REPROGRAMAR_DIAS -> "Nos dijiste que cambió tu horario. Revisaremos el día de " + sesion + ".";
            case CAUSA_NO_REGISTRADA -> "Una sesión no se completó y no registraste el motivo: si nos lo cuentas, "
                    + "podremos ajustarla.";
            case CONSERVAR -> "Mantenemos " + sesion + " como estaba.";
        };
    }
}
