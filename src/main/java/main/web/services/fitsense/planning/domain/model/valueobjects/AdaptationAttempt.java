package main.web.services.fitsense.planning.domain.model.valueobjects;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Una fila de {@code plan_adaptation_attempts} (V32), antes de persistirse.
 * <p>
 * QUE REGISTRA. Lo que OCURRIO respecto de la orden original, no lo que V8C
 * valido. Son dos perspectivas distintas y las dos hacen falta: el
 * {@code ConservationReport} documenta que comprobo el motor y viaja entero en
 * {@code conservationReportJson}; estas listas documentan el desenlace.
 * <p>
 * LAS INVARIANTES SON LAS DE V32, EN JAVA. Cada comprobacion del constructor
 * corresponde a una restriccion real de la tabla, y estan aqui para que un
 * error se vea al construir el objeto y no al confirmar la transaccion, que es
 * donde lo veria el disparador diferido:
 * <ul>
 *   <li>{@code ck_paa_desenlace_subconjunto} — aplicadas y sin efecto son
 *       subconjunto de las senaladas.
 *   <li>{@code ck_paa_aplicada_o_sin_efecto} — y no se solapan entre si.
 *   <li>{@code ck_paa_aplicada_con_sesiones} y
 *       {@code ck_paa_solo_aplicada_con_sesiones} (V33) — APLICADA si y SOLO si
 *       hay sesiones aplicadas. Sin la primera mitad, una fila puede constar
 *       aplicada sin haber aplicado nada; sin la segunda, un RESPALDO o un
 *       SIN_ORDEN pueden constar con sesiones aplicadas, o sea un plan
 *       conservado que afirma haber cambiado sesiones.
 *   <li>{@code ck_paa_conservadas_disjuntas} — conservadas NO se solapa con
 *       senaladas. Una sesion senalada que no cambio es SIN EFECTO, no
 *       conservada: «conservada» es la que nadie senalo.
 *   <li>{@code ck_paa_plan_segun_estado} — con plan o sin plan segun el estado.
 *   <li>{@code ck_paa_gen_source} y {@code ck_paa_procedencia_continuidad}.
 *   <li>{@code ck_paa_estado_segun_decision}.
 *   <li>{@code ck_paa_senaladas_coherentes} y {@code ck_paa_intervenciones}.
 * </ul>
 */
public record AdaptationAttempt(Long userId,
                                Long weeklyMetricId,
                                Long sourcePlanId,
                                LocalDate newWeekStartDate,
                                Long resultingPlanId,
                                AdaptationDecision decision,
                                Status status,
                                GenerationSource generationSource,
                                short aiCalls,
                                List<Long> targetedSessions,
                                List<String> targetedInterventions,
                                List<Long> appliedSessions,
                                List<Long> noEffectSessions,
                                List<Long> conservedSessions,
                                String conservationReportJson,
                                String rejectionReason,
                                BigDecimal observedAdherencePct,
                                BigDecimal registrationCoveragePct) {

    /** Los seis valores de {@code ck_paa_status}, con sus nombres exactos. */
    public enum Status {
        APLICADA, SIN_EFECTO, RECHAZADA, RESPALDO, NO_FACTIBLE, SIN_ORDEN;

        /** Si este estado publico un plan. V32 lo ata a {@code resulting_plan_id}. */
        public boolean publico() {
            return this != RECHAZADA && this != NO_FACTIBLE;
        }
    }

    public AdaptationAttempt {
        targetedSessions = copia(targetedSessions);
        targetedInterventions = targetedInterventions == null
                ? List.of() : List.copyOf(targetedInterventions);
        appliedSessions = copia(appliedSessions);
        noEffectSessions = copia(noEffectSessions);
        conservedSessions = copia(conservedSessions);

        exigir(userId != null, "el participante");
        exigir(weeklyMetricId != null, "la medicion de la semana");
        exigir(sourcePlanId != null, "el plan del que proviene");
        exigir(newWeekStartDate != null, "la fecha de la semana nueva");
        exigir(decision != null, "la decision de adaptacion");
        exigir(status != null, "el estado");

        // ck_paa_senaladas_coherentes
        if (decision.ordenaAdaptar() == targetedSessions.isEmpty())
            throw new IllegalArgumentException(decision.ordenaAdaptar()
                    ? "Se ordeno adaptar y no consta ninguna sesion senalada."
                    : "No se ordeno adaptar y constan sesiones senaladas: " + targetedSessions);

        // ck_paa_senaladas_unicas. La base solo la exige en targeted_sessions,
        // pero un identificador repetido no significa nada en ninguna de las
        // cuatro listas, y detectarlo aqui cuesta lo mismo.
        sinRepetidos(targetedSessions, "senaladas");
        sinRepetidos(appliedSessions, "aplicadas");
        sinRepetidos(noEffectSessions, "sin efecto");
        sinRepetidos(conservedSessions, "conservadas");

        // ck_paa_intervenciones
        if (targetedInterventions.size() != targetedSessions.size())
            throw new IllegalArgumentException(
                    "Hay %d sesiones senaladas y %d intervenciones: cada sesion lleva la suya."
                            .formatted(targetedSessions.size(), targetedInterventions.size()));

        // ck_paa_estado_segun_decision
        boolean coherente = decision.ordenaAdaptar()
                ? status != Status.SIN_ORDEN
                : status == Status.SIN_ORDEN || status == Status.NO_FACTIBLE;
        if (!coherente)
            throw new IllegalArgumentException(
                    "La decision " + decision + " no puede terminar en " + status + ".");

        // ck_paa_plan_segun_estado
        if (status.publico() != (resultingPlanId != null))
            throw new IllegalArgumentException(status.publico()
                    ? "El estado " + status + " publica un plan y no consta cual."
                    : "El estado " + status + " no publica nada y consta un plan: " + resultingPlanId);

        // ck_paa_resultado_distinto
        if (resultingPlanId != null && resultingPlanId.equals(sourcePlanId))
            throw new IllegalArgumentException(
                    "El plan publicado no puede ser el mismo del que proviene.");

        // ck_paa_gen_source
        if ((resultingPlanId != null) != (generationSource != null))
            throw new IllegalArgumentException(
                    "El origen se declara exactamente cuando hay plan publicado.");

        // ck_paa_procedencia_continuidad
        if ((status == Status.RESPALDO || status == Status.SIN_ORDEN)
                && generationSource != GenerationSource.CONSERVED)
            throw new IllegalArgumentException(
                    "Un " + status + " conserva la semana anterior, asi que su origen es"
                            + " CONSERVED y no " + generationSource + ".");

        // ck_paa_motivo_si_fallo
        if (!status.publico() && (rejectionReason == null || rejectionReason.isBlank()))
            throw new IllegalArgumentException(
                    "Un " + status + " sin motivo no sirve como evidencia.");

        // ck_paa_desenlace_subconjunto
        var fuera = new LinkedHashSet<>(appliedSessions);
        fuera.addAll(noEffectSessions);
        fuera.removeAll(targetedSessions);
        if (!fuera.isEmpty())
            throw new IllegalArgumentException(
                    "Las sesiones " + fuera + " tienen desenlace y no estaban senaladas.");

        // ck_paa_aplicada_o_sin_efecto
        var ambas = new LinkedHashSet<>(appliedSessions);
        ambas.retainAll(noEffectSessions);
        if (!ambas.isEmpty())
            throw new IllegalArgumentException(
                    "Las sesiones " + ambas + " constan aplicadas y sin efecto a la vez.");

        // ck_paa_aplicada_con_sesiones + ck_paa_solo_aplicada_con_sesiones (V33).
        //
        // APLICADA SI Y SOLO SI HAY SESIONES APLICADAS, en los dos sentidos.
        // Es lo que ya hace el mapper -APLICADA sale unicamente de
        // CORRECCION_IA publicable, y applied_sessions solo se puebla en ese
        // camino-, pero nadie lo exigia: ni los diecisiete CHECK de arriba ni
        // V32. Toda la garantia descansaba en una linea de AdaptedWeekProducer,
        // la que mira huboAdaptacionEfectiva().
        //
        // Las dos mitades importan por separado. Sin la primera, una fila
        // APLICADA puede no haber aplicado nada. Sin la segunda, un RESPALDO o
        // un SIN_ORDEN pueden constar con sesiones aplicadas, o sea un plan
        // conservado que afirma haber cambiado sesiones. Las dos falsean el
        // recuento de adaptaciones efectivamente aplicadas.
        boolean conAplicadas = !appliedSessions.isEmpty();
        if ((status == Status.APLICADA) != conAplicadas)
            throw new IllegalArgumentException(status == Status.APLICADA
                    ? "Un APLICADA sin sesiones aplicadas no es una adaptacion aplicada."
                    : "El estado " + status + " consta con sesiones aplicadas "
                    + appliedSessions + ": solo APLICADA las tiene.");

        // ck_paa_conservadas_disjuntas
        var solapan = new LinkedHashSet<>(conservedSessions);
        solapan.retainAll(targetedSessions);
        if (!solapan.isEmpty())
            throw new IllegalArgumentException(
                    "Las sesiones " + solapan + " constan conservadas y senaladas. Una sesion"
                            + " senalada que no cambio es SIN EFECTO, no conservada.");

        // Nada publicado, nada que registrar por sesion.
        if (!status.publico()
                && !(appliedSessions.isEmpty() && noEffectSessions.isEmpty()
                && conservedSessions.isEmpty()))
            throw new IllegalArgumentException(
                    "Un " + status + " no publico nada, asi que ninguna sesion tuvo desenlace.");

        // ck_paa_ai_calls
        if (aiCalls < 0 || aiCalls > 4)
            throw new IllegalArgumentException("Llamadas a la IA fuera de rango: " + aiCalls);
    }

    private static void sinRepetidos(List<Long> ids, String lista) {
        if (new LinkedHashSet<>(ids).size() != ids.size())
            throw new IllegalArgumentException(
                    "Hay identificadores repetidos en las sesiones " + lista + ": " + ids);
    }

    private static List<Long> copia(List<Long> ids) {
        return ids == null ? List.of() : List.copyOf(ids);
    }

    private static void exigir(boolean condicion, String que) {
        if (!condicion)
            throw new IllegalArgumentException("Un intento de adaptacion tiene que decir " + que + ".");
    }
}