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
 *   <li>{@code RESPALDO_SIN_IA}: la IA no devolvio ninguna propuesta: no se pudo consultar (apagada o
 *       cortacircuitos) o se consulto y el proveedor no respondio (caido, timeout, respuesta ilegible; R-4).</li>
 *   <li>{@code NO_FACTIBLE_POR_SEGURIDAD} / {@code NO_FACTIBLE}: no se publico ninguna semana (conservar
 *       habria vuelto a publicar lo que dolio, o la semana no pasaba la validacion).</li>
 *   <li>{@code PROGRESION_DEL_BACKEND} / {@code SIN_ADAPTACION}: no habia orden de adaptar.</li>
 * </ul>
 * Ningun resultado de conservacion se presenta como una adaptacion ejecutada.
 * <p>
 * <b>Campos estructurados (fase 2.1 del cierre definitivo, aditivos y compatibles).</b> Los nueve campos de siempre
 * no cambian. Se anaden:
 * <ul>
 *   <li>{@link #estadoPublicacion}: {@code PUBLICADO} o {@code NO_PUBLICADO} (si el ciclo dejo una semana).</li>
 *   <li>{@link #motivoCodigo} y {@link #motivoTexto}: el {@link NoFactibleReason} de fondo y su texto neutro. Solo
 *       cuando no se publico nada, o cuando la factibilidad previa conservo la semana. Null en las filas anteriores
 *       a V44_1: el codigo no se deduce del texto.</li>
 *   <li>{@link #codigos}: todos los codigos registrados, el de fondo primero (adaptacion y, si hubo, P-03 nivel 2).</li>
 *   <li>{@link #sesionAfectada}: la sesion senalada por dolor de la semana medida, si la hubo.</li>
 *   <li>{@link #restricciones}: las exclusiones por dolor que P-03 tuvo que respetar, si constan en su traza.</li>
 * </ul>
 * {@link #attemptId} es el identificador de seguimiento (la fila V32).
 */
public record AdaptationOutcome(Long attemptId, LocalDate weekStartDate, Long resultingPlanId, String resultado,
                                String estado, String decision, int llamadasIa, List<String> motivos,
                                List<Sesion> sesiones,
                                String estadoPublicacion, String motivoCodigo, String motivoTexto,
                                List<String> codigos, SesionAfectada sesionAfectada,
                                List<Restriccion> restricciones) {

    /** Una sesion senalada de la semana medida y como quedo. */
    public record Sesion(Long origenPlannedWorkoutId, String intervencion, String resultado) {}

    /** La sesion de la semana medida que motivo la intervencion de seguridad. */
    public record SesionAfectada(Long plannedWorkoutId, LocalDate fecha, String enfoque) {}

    /** Una restriccion vigente que la semana nueva tenia que respetar (hoy solo exclusiones por dolor, P-02). */
    public record Restriccion(String tipo, Long exerciseId) {
        public static final String EXCLUSION_DOLOR = "EXCLUSION_DOLOR";
    }

    public static final String PUBLICADO = "PUBLICADO";
    public static final String NO_PUBLICADO = "NO_PUBLICADO";
    private static final Set<String> SIN_DISPONIBILIDAD =
            Set.of("ACORTAMIENTO_IMPOSIBLE", "REPROGRAMACION_IMPOSIBLE", "ADAPTACION_IMPOSIBLE");

    public AdaptationOutcome {
        motivos = motivos == null ? List.of() : List.copyOf(motivos);
        sesiones = sesiones == null ? List.of() : List.copyOf(sesiones);
        codigos = codigos == null ? List.of() : List.copyOf(codigos);
        restricciones = restricciones == null ? List.of() : List.copyOf(restricciones);
    }

    /** Forma anterior (nueve campos): deriva los estructurados de lo que ya consta. */
    public AdaptationOutcome(Long attemptId, LocalDate weekStartDate, Long resultingPlanId, String resultado,
                             String estado, String decision, int llamadasIa, List<String> motivos,
                             List<Sesion> sesiones) {
        this(attemptId, weekStartDate, resultingPlanId, resultado, estado, decision, llamadasIa, motivos, sesiones,
                List.of());
    }

    private AdaptationOutcome(Long attemptId, LocalDate weekStartDate, Long resultingPlanId, String resultado,
                              String estado, String decision, int llamadasIa, List<String> motivos,
                              List<Sesion> sesiones, List<String> codigos) {
        this(attemptId, weekStartDate, resultingPlanId, resultado, estado, decision, llamadasIa, motivos, sesiones,
                resultingPlanId != null ? PUBLICADO : NO_PUBLICADO,
                codigoDeFondo(resultingPlanId, resultado, codigos),
                texto(codigoDeFondo(resultingPlanId, resultado, codigos)), codigos, null, List.of());
    }

    /**
     * El codigo de fondo: el primero registrado si no se publico nada; DISPONIBILIDAD_NO_CUMPLIBLE si la
     * factibilidad previa conservo la semana; si no, ninguno.
     */
    private static String codigoDeFondo(Long resultingPlanId, String resultado, List<String> codigos) {
        if (resultingPlanId == null && codigos != null && !codigos.isEmpty()) return codigos.get(0);
        if (resultado != null && SIN_DISPONIBILIDAD.contains(resultado))
            return NoFactibleReason.DISPONIBILIDAD_NO_CUMPLIBLE.name();
        return null;
    }

    private static String texto(String codigo) {
        var r = NoFactibleReason.of(codigo);
        return r == null ? null : r.texto();
    }

    /** Con la sesion afectada y las restricciones (las resuelve quien tiene acceso a los repositorios). */
    public AdaptationOutcome conDetalle(SesionAfectada sesion, List<Restriccion> vigentes) {
        return new AdaptationOutcome(attemptId, weekStartDate, resultingPlanId, resultado, estado, decision,
                llamadasIa, motivos, sesiones, estadoPublicacion, motivoCodigo, motivoTexto, codigos, sesion,
                vigentes);
    }

    public static final String NOTA_FACTIBILIDAD = "FACTIBILIDAD_PREVIA: ";
    /**
     * R-4 (resiliencia de Replicate, 11/11/2026): se llamo al proveedor y no devolvio ninguna respuesta. Lo deja
     * {@code AdaptedWeekCommandService} en {@code correcciones_del_backend}, con la causa tecnica detras.
     */
    public static final String NOTA_IA_NO_DISPONIBLE = "IA_NO_DISPONIBLE: ";
    /** P-03, nivel 1: igual que {@code AdaptedWeekProducer.NOTA_REGENERACION_SEGURA}. */
    public static final String NOTA_REGENERACION_SEGURA = "REGENERACION_SEGURA: ";

    /** P-03, nivel 2: un NO_FACTIBLE por dolor cuya semana se regenero con reglas en el mismo ciclo. */
    public AdaptationOutcome conRegeneracion(Long planRegenerado, String resultadoRegeneracion, String motivo) {
        return conRegeneracion(planRegenerado, resultadoRegeneracion, motivo, null);
    }

    /** Igual, con el codigo del NO_FACTIBLE del nivel 2 (null si publico o si la fila es anterior a V44_1). */
    public AdaptationOutcome conRegeneracion(Long planRegenerado, String resultadoRegeneracion, String motivo,
                                             String codigoRegeneracion) {
        var m = new java.util.ArrayList<>(motivos);
        if (motivo != null) m.add(motivo);
        var c = new java.util.ArrayList<>(codigos);
        if (codigoRegeneracion != null && !c.contains(codigoRegeneracion)) c.add(codigoRegeneracion);
        return new AdaptationOutcome(attemptId, weekStartDate, planRegenerado, resultadoRegeneracion, estado, decision,
                llamadasIa, List.copyOf(m), sesiones, List.copyOf(c));
    }
    private static final Set<String> SIN_NADA_QUE_PEDIR = Set.of("CONSERVAR", "CAUSA_NO_REGISTRADA");

    /**
     * @param correcciones {@code correcciones_del_backend} del informe V32 (la factibilidad previa deja ahi su
     *                     motivo con el prefijo {@link #NOTA_FACTIBILIDAD})
     */
    public static AdaptationOutcome de(Long attemptId, LocalDate weekStartDate, Long resultingPlanId, String estado,
                                       String decision, int llamadasIa, Long[] senaladas, String[] intervenciones,
                                       Long[] aplicadas, Long[] sinEfecto, Long[] conservadas, Long[] progresadas,
                                       List<String> correcciones, String rechazo) {
        return de(attemptId, weekStartDate, resultingPlanId, estado, decision, llamadasIa, senaladas, intervenciones,
                aplicadas, sinEfecto, conservadas, progresadas, correcciones, rechazo, List.of());
    }

    /** Con los codigos {@link NoFactibleReason} registrados en V32 (V44_1). */
    public static AdaptationOutcome de(Long attemptId, LocalDate weekStartDate, Long resultingPlanId, String estado,
                                       String decision, int llamadasIa, Long[] senaladas, String[] intervenciones,
                                       Long[] aplicadas, Long[] sinEfecto, Long[] conservadas, Long[] progresadas,
                                       List<String> correcciones, String rechazo, List<String> codigos) {
        var imposibles = correcciones == null ? List.<String>of() : correcciones.stream()
                .filter(c -> c != null && c.startsWith(NOTA_FACTIBILIDAD))
                .map(c -> c.substring(NOTA_FACTIBILIDAD.length())).toList();
        var tipos = intervenciones == null ? List.<String>of() : Arrays.asList(intervenciones);
        boolean regeneracionSegura = correcciones != null && correcciones.stream()
                .anyMatch(c -> c != null && c.startsWith(NOTA_REGENERACION_SEGURA));
        boolean iaNoDisponible = correcciones != null && correcciones.stream()
                .anyMatch(c -> c != null && c.startsWith(NOTA_IA_NO_DISPONIBLE));
        String resultado = switch (estado == null ? "" : estado) {
            case "APLICADA" -> "ADAPTACION_APLICADA";
            case "RESPALDO" -> regeneracionSegura ? "REGENERACION_SEGURA_SUSTITUCION"
                    : respaldo(imposibles, tipos, iaNoDisponible ? 0 : llamadasIa);
            case "SIN_EFECTO" -> !tipos.isEmpty() && SIN_NADA_QUE_PEDIR.containsAll(tipos)
                    ? "CONSERVACION_DELIBERADA" : "SIN_CAMBIOS_VALIDOS";
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
                llamadasIa, motivos, List.copyOf(sesiones), codigos == null ? List.of() : List.copyOf(codigos));
    }

    private static String respaldo(List<String> imposibles, List<String> tipos, int llamadasIa) {
        return !imposibles.isEmpty()
                    ? (tipos.contains("ACORTAR_O_REORGANIZAR") && tipos.stream().allMatch(t ->
                    t.equals("ACORTAR_O_REORGANIZAR") || SIN_NADA_QUE_PEDIR.contains(t)) ? "ACORTAMIENTO_IMPOSIBLE"
                    : tipos.contains("REPROGRAMAR_DIAS") && tipos.stream().allMatch(t ->
                    t.equals("REPROGRAMAR_DIAS") || SIN_NADA_QUE_PEDIR.contains(t)) ? "REPROGRAMACION_IMPOSIBLE"
                    : "ADAPTACION_IMPOSIBLE")
                    : llamadasIa > 0 ? "RESPALDO_PROPUESTA_IA_INVALIDA" : "RESPALDO_SIN_IA";
    }

    private static boolean contiene(Long[] ids, Long id) {
        return ids != null && Arrays.stream(ids).anyMatch(x -> Objects.equals(x, id));
    }
}
