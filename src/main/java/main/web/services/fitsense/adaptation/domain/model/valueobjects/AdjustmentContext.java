package main.web.services.fitsense.adaptation.domain.model.valueobjects;

import main.web.services.fitsense.analytics.interfaces.acl.SessionOutcomeView;

import java.math.BigDecimal;
import java.util.List;

/**
 * Insumos de la decision. Todo llega por parametro para que la tabla sea
 * reproducible: dados los mismos numeros, la misma orden, siempre.
 */
public record AdjustmentContext(
        BigDecimal weightedAdherencePct,
        String dominantSkipReason,
        int previousWeekVolume,
        int baselineWeekVolume,
        int previousDaysPerWeek,
        int previousSessionMinutes,
        int previousMaxDifficulty,

        /**
         * Adherencia de la semana ANTERIOR a la medida. Permite exigir dos
         * semanas seguidas de cumplimiento antes de progresar, en lugar de
         * subir el volumen por calendario.
         * <p>
         * NULL cuando no existe esa semana (la primera del participante) o
         * cuando no fue medible. En ambos casos no se progresa: sin evidencia
         * de desempeno sostenido, la opcion conservadora.
         */
        BigDecimal previousWeekAdherencePct,

        /**
         * RPE promedio de la semana medida. Hoy solo se usa para bloquear la
         * progresion cuando falta; el techo por RPE alto esta desactivado
         * mientras la escala no este instrumentada.
         */
        BigDecimal averageSessionRpe,

        // ---- Lo que anade la adaptacion localizada (17/10/2026).
        //
        // Hasta aqui el contexto era TODO agregados: un porcentaje de
        // adherencia, un RPE medio y un dominantSkipReason ya reducido a una
        // sola causa para la semana entera. Con eso el decisor no podia senalar
        // que componente fallo, porque no sabia que existian componentes.
        //
        // No se duplica el detalle por EJERCICIO: el prompt ya se lo manda a la
        // IA. El backend senala la sesion y el porque; la IA mira dentro.

        /** Lo que paso en cada sesion, con las senaladas ya marcadas. */
        List<SessionOutcomeView> sessionOutcomes,

        /** De V30: cuantos previstos tienen registro, y cuantos habia. */
        int recordedWorkouts,
        int scheduledWorkouts,

        /**
         * De V30: cumplimiento sobre lo que SI tiene registro. Es la que decide,
         * no la ponderada: la ponderada mete las sesiones sin registro como
         * ceros, asi que quien cumplio el 100 % de lo que registro puede
         * aparecer con un 66 % y recibir una adaptacion que no necesita.
         */
        BigDecimal observedAdherencePct,

        /** De V30: que parte de la semana tiene registro. */
        BigDecimal registrationCoveragePct
) {}