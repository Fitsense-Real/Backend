package main.web.services.fitsense.planning.domain.model.valueobjects;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * LO QUE PASO CON LA ADAPTACION DE UNA SEMANA, PARA EL CLIENTE (orden de cierre del MVP, 31/10/2026, §11-12).
 * <p>
 * Solo TRADUCE lo que V32 ya registra ({@code plan_adaptation_attempts}): no decide nada. Antes la app solo
 * veia {@code generationSource} y {@code adjustmentApplied} -que es el nombre de la DECISION, ADAPT_LOCALIZED
 * o ADAPT_EXPANDED, tambien cuando nada se cambio-, y no podia distinguir una conservacion deliberada de un
 * acortamiento imposible ni de una propuesta de la IA rechazada. {@link #resultado} lo dice sin ambiguedad:
 * <ul>
 *   <li>{@code ADAPTACION_APLICADA}: al menos una sesion senalada cambio y paso V8C y V1-V25.</li>
 *   <li>{@code CONSERVACION_DELIBERADA}: no habia nada que pedir (CONSERVAR o causa sin registrar): misma
 *       semana, sin IA.</li>
 *   <li>{@code SIN_CAMBIOS_VALIDOS}: la IA respondio sin un cambio aplicable; misma semana.</li>
 *   <li>{@code ACORTAMIENTO_IMPOSIBLE} / {@code REPROGRAMACION_IMPOSIBLE} / {@code ADAPTACION_IMPOSIBLE}: el
 *       backend demostro antes de llamar que ninguna propuesta valida cabia (factibilidad previa); misma
 *       semana. {@link #motivos} da la cota (por ejemplo, «no baja de 28 min y la persona declaro 22»).</li>
 *   <li>{@code RESPALDO_PROPUESTA_IA_INVALIDA}: la IA propuso y todas sus propuestas se rechazaron.</li>
 *   <li>{@code RESPALDO_SIN_IA}: no se pudo consultar a la IA (apagada o cortacircuitos).</li>
 *   <li>{@code NO_FACTIBLE_POR_SEGURIDAD} / {@code NO_FACTIBLE}: no se publico ninguna semana (conservar
 *       habria vuelto a publicar lo que dolio, o la semana no pasaba la validacion).</li>
 *   <li>{@code PROGRESION_DEL_BACKEND} / {@code SIN_ADAPTACION}: no habia orden de adaptar.</li>
 * </ul>
 * Ningun resultado de conservacion se presenta como una adaptacion ejecutada.
 */
public record AdaptationOutcome(Long attemptId, LocalDate weekStartDate, Long resultingPlanId, String resultado,
                                String estado, String decision, int llamadasIa, List<String> motivos,
                                List<Sesion> sesiones) {

    /** Una sesion senalada de la semana medida y como quedo. */
    public record Sesion(Long origenPlannedWorkoutId, String intervencion, String resultado) {}

    public static final String NOTA_FACTIBILIDAD = "FACTIBILIDAD_PREVIA: ";
    private static final Set<String> SIN_NADA_QUE_PEDIR = Set.of("CONSERVAR", "CAUSA_NO_REGISTRADA");

    /**
     * @param correcciones {@code correcciones_del_backend} del informe V32 (la factibilidad previa deja ahi su
     *                     motivo con el prefijo {@link #NOTA_FACTIBILIDAD})
     */
    public static AdaptationOutcome de(Long attemptId, LocalDate weekStartDate, Long resultingPlanId, String estado,
                                       String decision, int llamadasIa, Long[] senaladas, String[] intervenciones,
                                       Long[] aplicadas, Long[] sinEfecto, Long[] conservadas, Long[] progresadas,
                                       List<String> correcciones, String rechazo) {
        var imposibles = correcciones == null ? List.<String>of() : correcciones.stream()
                .filter(c -> c != null && c.startsWith(NOTA_FACTIBILIDAD))
                .map(c -> c.substring(NOTA_FACTIBILIDAD.length())).toList();
        var tipos = intervenciones == null ? List.<String>of() : Arrays.asList(intervenciones);
        String resultado = switch (estado == null ? "" : estado) {
            case "APLICADA" -> "ADAPTACION_APLICADA";
            case "SIN_EFECTO" -> !tipos.isEmpty() && SIN_NADA_QUE_PEDIR.containsAll(tipos)
                    ? "CONSERVACION_DELIBERADA" : "SIN_CAMBIOS_VALIDOS";
            case "RESPALDO" -> !imposibles.isEmpty()
                    ? (tipos.contains("ACORTAR_O_REORGANIZAR") && tipos.stream().allMatch(t ->
                    t.equals("ACORTAR_O_REORGANIZAR") || SIN_NADA_QUE_PEDIR.contains(t)) ? "ACORTAMIENTO_IMPOSIBLE"
                    : tipos.contains("REPROGRAMAR_DIAS") && tipos.stream().allMatch(t ->
                    t.equals("REPROGRAMAR_DIAS") || SIN_NADA_QUE_PEDIR.contains(t)) ? "REPROGRAMACION_IMPOSIBLE"
                    : "ADAPTACION_IMPOSIBLE")
                    : llamadasIa > 0 ? "RESPALDO_PROPUESTA_IA_INVALIDA" : "RESPALDO_SIN_IA";
            case "NO_FACTIBLE" -> tipos.contains("SEGURIDAD_ANTE_DOLOR")
                    || (rechazo != null && rechazo.toLowerCase(java.util.Locale.ROOT).contains("dolor"))
                    ? "NO_FACTIBLE_POR_SEGURIDAD" : "NO_FACTIBLE";
            case "SIN_ORDEN" -> progresadas != null && progresadas.length > 0 ? "PROGRESION_DEL_BACKEND" : "SIN_ADAPTACION";
            default -> estado;
        };
        var motivos = "NO_FACTIBLE".equals(estado) || "RECHAZADA".equals(estado)
                ? (rechazo == null ? List.<String>of() : List.of(rechazo)) : imposibles;
        var sesiones = new java.util.ArrayList<Sesion>();
        if (senaladas != null)
            for (int i = 0; i < senaladas.length; i++) {
                Long id = senaladas[i];
                String r = contiene(aplicadas, id) ? "APLICADA" : contiene(sinEfecto, id) ? "SIN_EFECTO"
                        : contiene(conservadas, id) ? "CONSERVADA" : contiene(progresadas, id) ? "PROGRESADA"
                        : "NO_PUBLICADA";
                sesiones.add(new Sesion(id, i < tipos.size() ? tipos.get(i) : null, r));
            }
        return new AdaptationOutcome(attemptId, weekStartDate, resultingPlanId, resultado, estado, decision,
                llamadasIa, motivos, List.copyOf(sesiones));
    }

    private static boolean contiene(Long[] ids, Long id) {
        return ids != null && Arrays.stream(ids).anyMatch(x -> Objects.equals(x, id));
    }
}
