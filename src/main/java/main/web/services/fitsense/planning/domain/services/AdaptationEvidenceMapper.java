package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptationAttempt;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptationDecision;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptedWeekResult;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Traduce lo que produjo el productor a una fila de evidencia de V32.
 * <p>
 * POR QUE AQUI Y NO EN EL PRODUCTOR. Son dos preguntas distintas. V8C y el
 * productor responden «¿que sesiones cambiaron y es valido?»; V32 responde
 * «¿que le paso al participante respecto de lo que se ordeno?». La segunda
 * necesita la ORDEN ORIGINAL, que el informe de conservacion no lleva.
 * <p>
 * EL INFORME DE V8C NO SE TOCA. Viaja entero en {@code conservationReportJson}.
 * Alterarlo para que cuadre con V32 destruiria la otra mitad de la auditoria:
 * el informe documenta que valido el motor, estas listas documentan el
 * desenlace, y las dos tienen que poder leerse por separado.
 * <p>
 * LAS SENALADAS SALEN DE LA ORDEN, NUNCA DEL INFORME. En la ruta de respaldo el
 * productor valida la continuidad con una orden vacia, asi que su informe
 * clasifica TODAS las sesiones como conservadas, incluida la que si estaba
 * senalada. Deducir las senaladas de ese informe daria cero, y la evidencia
 * diria que no se ordeno nada.
 */
public class AdaptationEvidenceMapper {

    /**
     * Los datos que no estan en el resultado ni en la orden y solo conoce quien
     * publica.
     *
     * @param resultingPlanId el plan que se publico, o {@code null} si no se
     *                        publico ninguno. Tiene que concordar con el estado
     *                        del resultado: si el productor dijo NO_FACTIBLE y
     *                        aqui llega un plan, algo se publico que no debia, y
     *                        esto falla en vez de descartarlo en silencio.
     *                        <p>
     *                        EL ORIGEN NO VIAJA AQUI. Se toma de
     *                        {@code resultado.source()}, y quien publica tiene
     *                        que grabar en {@code weekly_training_plans} EXACTAMENTE
     *                        ese mismo valor, en la misma transaccion: el
     *                        disparador de V32 compara los dos y rechaza el
     *                        intento si no coinciden.
     */
    public record PublicationFacts(Long userId,
                                   Long weeklyMetricId,
                                   Long sourcePlanId,
                                   LocalDate newWeekStartDate,
                                   Long resultingPlanId,
                                   short aiCalls,
                                   BigDecimal observedAdherencePct,
                                   BigDecimal registrationCoveragePct,
                                   String conservationReportJson) {}

    public AdaptationAttempt map(AdaptedWeekResult resultado,
                                 PlanAdjustment orden,
                                 AdaptationDecision decision,
                                 PublicationFacts hechos) {

        if (resultado == null)
            throw new IllegalArgumentException("No hay resultado que registrar.");
        if (decision == null)
            throw new IllegalArgumentException("Sin decision no se puede registrar el intento.");
        if (hechos == null)
            throw new IllegalArgumentException("Faltan los datos de publicacion.");

        // El productor decide si hay semana publicable; la persistencia decide
        // si la publico. Si no concuerdan, es un error de integracion entre los
        // dos y no un dato que registrar: descartarlo en silencio ocultaria un
        // plan publicado que el productor habia rechazado.
        if (resultado.publicable() != (hechos.resultingPlanId() != null))
            throw new IllegalArgumentException(
                    ("El productor dijo %s y la publicacion %s un plan. Esos dos hechos"
                            + " tienen que concordar antes de registrar evidencia.")
                            .formatted(resultado.estado(),
                                    hechos.resultingPlanId() == null ? "no trae" : "trae"));

        var senaladas = senaladasDe(orden);
        var intervenciones = intervencionesDe(orden);
        var status = estado(resultado, decision);

        // NADA PUBLICADO, NINGUNA SESION CON DESENLACE. Aunque V8C hubiera
        // encontrado una correccion local valida, si otra sesion tumbo la
        // semana entera no le llego nada al participante. V32 es evidencia de
        // resultados reales, no de propuestas.
        if (!status.publico())
            return new AdaptationAttempt(hechos.userId(), hechos.weeklyMetricId(),
                    hechos.sourcePlanId(), hechos.newWeekStartDate(), null,
                    decision, status, null, hechos.aiCalls(),
                    senaladas, intervenciones, List.of(), List.of(), List.of(),
                    hechos.conservationReportJson(), motivo(resultado),
                    hechos.observedAdherencePct(), hechos.registrationCoveragePct());

        // V34 (§12.4): la sesion publicada del paso es EXACTAMENTE la propuesta.
        // Si no, no se registra nada, y la transaccion de publicacion se deshace.
        for (var paso : resultado.conservacion().pasos()) {
            var publicada = resultado.semana().stream()
                    .filter(d -> paso.origen().equals(d.sourcePlannedWorkoutId())).toList();
            if (paso.contenido() == null || publicada.size() != 1
                    || !paso.contenido().equals(publicada.get(0).workout()))
                throw new IllegalArgumentException(("El paso de progresion de la sesion %d no coincide con lo que"
                        + " se publica.").formatted(paso.origen()));
        }

        // Aplicadas: SOLO cuando la IA cambio algo de verdad y se publico. En
        // un respaldo o en una semana sin efecto no se aplico ninguna, aunque
        // el informe de continuidad las clasifique como conservadas.
        var aplicadas = resultado.motivo() == AdaptedWeekResult.Motivo.CORRECCION_IA
                ? List.copyOf(resultado.conservacion().aplicadas())
                : List.<Long>of();

        // Sin efecto: las senaladas que no se aplicaron. Se calcula restando
        // sobre la ORDEN, asi que una sola formula vale para APLICADA,
        // SIN_EFECTO y RESPALDO.
        var sinEfecto = restar(senaladas, aplicadas);

        // Conservadas: las que NADIE senalo. El informe de continuidad incluye
        // tambien las senaladas, y en V32 eso seria una contradiccion.
        var conservadas = restar(resultado.conservacion().conservadas(), senaladas);

        return new AdaptationAttempt(hechos.userId(), hechos.weeklyMetricId(),
                hechos.sourcePlanId(), hechos.newWeekStartDate(), hechos.resultingPlanId(),
                decision, status, resultado.source(), hechos.aiCalls(),
                senaladas, intervenciones, aplicadas, sinEfecto, conservadas,
                hechos.conservationReportJson(), null,
                hechos.observedAdherencePct(), hechos.registrationCoveragePct(),
                List.copyOf(resultado.conservacion().progresadas()));
    }

    /**
     * El par {@code Motivo} x {@code Estado} del productor cubre los seis
     * estados de V32 sin huecos ni solapes.
     */
    private AdaptationAttempt.Status estado(AdaptedWeekResult resultado,
                                            AdaptationDecision decision) {
        boolean publicable = resultado.publicable();
        var status = switch (resultado.motivo()) {
            case SIN_ORDEN -> publicable
                    ? AdaptationAttempt.Status.SIN_ORDEN
                    : AdaptationAttempt.Status.NO_FACTIBLE;
            case CORRECCION_IA -> publicable
                    ? AdaptationAttempt.Status.APLICADA
                    : AdaptationAttempt.Status.RECHAZADA;
            case SIN_EFECTO -> publicable
                    ? AdaptationAttempt.Status.SIN_EFECTO
                    : AdaptationAttempt.Status.RECHAZADA;
            case RESPALDO_IA -> publicable
                    ? AdaptationAttempt.Status.RESPALDO
                    : AdaptationAttempt.Status.NO_FACTIBLE;
        };

        // Un desacuerdo entre la decision y el camino que tomo el productor es
        // un error de programacion, no un dato que registrar: significa que se
        // llamo al productor con una orden que no corresponde a la decision.
        if (decision.ordenaAdaptar() != (resultado.motivo() != AdaptedWeekResult.Motivo.SIN_ORDEN))
            throw new IllegalArgumentException(
                    "La decision %s no concuerda con el camino %s que tomo el productor."
                            .formatted(decision, resultado.motivo()));

        return status;
    }

    private String motivo(AdaptedWeekResult resultado) {
        return resultado.problemas().isEmpty()
                ? "Sin motivo registrado."
                : String.join(" | ", resultado.problemas());
    }

    private List<Long> senaladasDe(PlanAdjustment orden) {
        if (orden == null) return List.of();
        return orden.targetedSessions().stream()
                .map(PlanAdjustment.TargetedSession::plannedWorkoutId).toList();
    }

    private List<String> intervencionesDe(PlanAdjustment orden) {
        if (orden == null) return List.of();
        return orden.targetedSessions().stream()
                .map(s -> s.intervencion().name()).toList();
    }

    private List<Long> restar(List<Long> de, List<Long> quitar) {
        var resto = new LinkedHashSet<>(de == null ? List.<Long>of() : de);
        resto.removeAll(quitar);
        return new ArrayList<>(resto);
    }
}
