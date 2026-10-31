package main.web.services.fitsense.orchestation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.*;

/**
 * VALIDACION INDEPENDIENTE DE UNA SEMANA PUBLICADA, CON LAS AUTORIZACIONES DEL HISTORIAL
 * (orden de cierre del MVP, 31/10/2026, §10).
 * <p>
 * Antes el simulador validaba cada semana sin ninguna autorizacion y marcaba INVALIDA toda dosis
 * progresada, reducida o heredada (las 29 «INVALIDA» de la campana). Ahora reconstruye, leyendo las
 * tablas en crudo (V31 {@code source_planned_workout_id} y V32 {@code plan_adaptation_attempts}),
 * de donde viene cada dosis, SIN usar los resolutores del backend:
 * <ul>
 *   <li><b>BASE</b>: la generacion inicial o un ejercicio nuevo; rige la regla de siempre de V24
 *       (incluida la serie adicional condicionada de 4D v3.2/v3.3, que el propio V24 comprueba).</li>
 *   <li><b>PASO</b>: el informe V32 del ciclo registra un paso de ese ejercicio en esa sesion con
 *       exactamente esa dosis (progresion o recuperacion).</li>
 *   <li><b>REINCORPORACION</b>: el paso registrado es una reincorporacion; vuelve con la dosis base,
 *       asi que no necesita autorizacion y V24 la juzga como base.</li>
 *   <li><b>REDUCCION</b>: una adaptacion APLICADA de esa sesion bajo la dosis (ninguna dimension
 *       sube y el descanso es el mismo).</li>
 *   <li><b>HEREDADA</b>: copia identica de una dosis PASO o REDUCCION de un ciclo anterior.</li>
 * </ul>
 * Y las fechas cuya sesion viene de un ACORTAR aplicado, por copias o pasos (exencion de V20, D8).
 * <p>
 * Con esas autorizaciones valida la semana con el validador. Si el linaje es ambiguo (mas de un
 * intento por plan, informe ilegible, ejercicio repetido) el resultado es NO_CONCLUYENTE: nunca se
 * da por buena una dosis cuyo origen no se puede demostrar.
 */
final class ValidacionIndependiente {

    record Ej(long id, String tipo, Short series, Short reps, Integer segundos, Short descanso, java.math.BigDecimal carga) {}

    record Sesion(long id, LocalDate fecha, String foco, int minutos, Long origen, long plan, List<Ej> ejercicios) {}

    /** El resultado, con cuantas dosis necesitaron cada autorizacion. */
    record Resultado(String estado, Map<String, Integer> origenes, int fechasReducibles, List<String> problemas) {
        String texto() {
            var o = new TreeMap<>(origenes);
            o.values().removeIf(n -> n == 0);
            String a = o.isEmpty() ? "" : " " + o + (fechasReducibles > 0 ? " reducibles=" + fechasReducibles : "");
            return switch (estado) {
                case "INVALIDA", "NO_CONCLUYENTE" -> estado + " " + problemas;
                default -> estado + a;
            };
        }
    }

    private final JdbcTemplate jdbc;
    private final ObjectMapper json = new ObjectMapper();

    ValidacionIndependiente(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    Resultado validar(long planId, PlanGenerationContext ctx, PlanDraftValidator validador, int divisor) {
        var dosis = new LinkedHashSet<DosisAutorizada>();
        var reducibles = new HashSet<LocalDate>();
        var origenes = new TreeMap<String, Integer>();
        var dudas = new ArrayList<String>();
        var borrador = new ArrayList<PlanDraft.DraftWorkout>();
        for (var s : sesiones(planId)) {
            var lista = new ArrayList<PlanDraft.DraftExercise>();
            for (var e : s.ejercicios()) {
                var d = new PlanDraft.DraftExercise(e.id(), PrescriptionType.valueOf(e.tipo()), e.series(), e.reps(),
                        e.segundos(), e.carga(), e.descanso(), null);
                lista.add(d);
                String o = origen(s, e, dudas);
                origenes.merge(o, 1, Integer::sum);
                switch (o) {
                    case "PASO" -> dosis.add(DosisAutorizada.de(s.fecha(), d, DosisAutorizada.Origen.PASO));
                    case "REDUCCION" -> dosis.add(DosisAutorizada.de(s.fecha(), d, DosisAutorizada.Origen.REDUCCION));
                    case "HEREDADA" -> dosis.add(DosisAutorizada.de(s.fecha(), d, DosisAutorizada.Origen.HEREDADA));
                    default -> { }
                }
            }
            if (reducible(s, dudas)) reducibles.add(s.fecha());
            borrador.add(new PlanDraft.DraftWorkout(s.fecha(), WorkoutFocus.valueOf(s.foco()), "semana", s.minutos(), lista));
        }
        if (!dudas.isEmpty()) return new Resultado("NO_CONCLUYENTE", origenes, reducibles.size(), dudas);
        var informe = new ConservationReport(List.of(), List.of(), List.of(), List.of(), reducibles, List.of(),
                List.of(), List.of(), dosis, List.of());
        try {
            validador.validate(new PlanDraft(GenerationSource.RULE_ENGINE, null, "semana", null, "r", borrador),
                    ctx, divisor, informe);
            boolean conAutorizacion = !dosis.isEmpty() || !reducibles.isEmpty();
            return new Resultado(conAutorizacion ? "VALIDA_CON_AUTORIZACIONES" : "VALIDA", origenes,
                    reducibles.size(), List.of());
        } catch (InvalidPlanDraftException e) {
            return new Resultado("INVALIDA", origenes, reducibles.size(), e.problems().stream()
                    .map(p -> p.length() > 160 ? p.substring(0, 160) : p).toList());
        }
    }

    // =======================================================================
    // De donde viene cada dosis
    // =======================================================================

    private String origen(Sesion publicada, Ej e, List<String> dudas) {
        var actual = publicada;
        boolean primera = true;
        for (int vueltas = 0; vueltas < 200; vueltas++) {
            if (actual.origen() == null) return "BASE";
            var intento = intento(actual.plan());
            if (intento == null) {
                dudas.add("plan " + actual.plan() + " sin un unico intento V32");
                return "AMBIGUO";
            }
            var anterior = sesion(actual.origen());
            var previos = anterior.ejercicios().stream().filter(x -> x.id() == e.id()).toList();
            if (previos.size() > 1) {
                dudas.add("ejercicio " + e.id() + " repetido en la sesion " + anterior.id());
                return "AMBIGUO";
            }
            var informe = leer(intento.get("conservation_report"), dudas);
            if (informe == null) return "AMBIGUO";
            // Un paso registrado de ESTE ejercicio en ESTA sesion con exactamente esta dosis.
            for (var p : informe.path("pasos")) {
                if (p.path("origen").asLong() != anterior.id() || p.path("exercise_id").asLong() != e.id()) continue;
                if (!misma(p.path("despues"), e)) continue;
                String tipo = p.path("tipo").asText();
                if ("REINCORPORACION".equals(tipo)) return "REINCORPORACION";
                if ("PROGRESION".equals(tipo) || "RECUPERACION".equals(tipo) || "VUELTA_ATRAS".equals(tipo))
                    return primera ? "PASO" : "HEREDADA";
            }
            if (previos.size() == 1 && igual(previos.get(0), e)) {   // copia de esta dosis: hacia atras
                actual = anterior;
                primera = false;
                continue;
            }
            if (previos.isEmpty()) return "BASE";                       // nuevo en esta sesion (sustituto)
            if (aplicada(intento, anterior.id()) && baja(e, previos.get(0)))
                return primera ? "REDUCCION" : "HEREDADA";
            return "SIN_AUTORIZACION";
        }
        dudas.add("linaje demasiado largo o circular desde " + publicada.id());
        return "AMBIGUO";
    }

    /** La sesion viene de un ACORTAR aplicado por copias o pasos que no reducen (D8, D-P7). */
    private boolean reducible(Sesion s, List<String> dudas) {
        var actual = s;
        for (int vueltas = 0; vueltas < 200 && actual.origen() != null; vueltas++) {
            var intento = intento(actual.plan());
            if (intento == null) return false;
            long origen = actual.origen();
            if (contiene(intento.get("applied_sessions"), origen))
                return "ACORTAR_O_REORGANIZAR".equals(intervencion(intento, origen));
            boolean copia = contiene(intento.get("conserved_sessions"), origen)
                    || contiene(intento.get("progressed_sessions"), origen);
            if (!copia) return false;
            actual = sesion(origen);
        }
        return false;
    }

    // =======================================================================
    // Lectura cruda
    // =======================================================================

    private List<Sesion> sesiones(long planId) {
        return jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date",
                Long.class, planId).stream().map(this::sesion).toList();
    }

    private Sesion sesion(long id) {
        var f = jdbc.queryForMap("SELECT planned_workout_id, scheduled_date, focus_code, expected_duration_minutes,"
                + " source_planned_workout_id, plan_id FROM planned_workouts WHERE planned_workout_id = ?", id);
        var ejercicios = jdbc.query("SELECT exercise_id, prescription_type, planned_sets, planned_reps,"
                        + " planned_duration_seconds, rest_seconds, target_load_kg FROM planned_workout_exercises"
                        + " WHERE planned_workout_id = ? ORDER BY exercise_order",
                (rs, k) -> new Ej(rs.getLong(1), rs.getString(2), rs.getObject(3, Short.class),
                        rs.getObject(4, Short.class), rs.getObject(5, Integer.class), rs.getObject(6, Short.class),
                        rs.getBigDecimal(7)), id);
        return new Sesion(id, ((java.sql.Date) f.get("scheduled_date")).toLocalDate(), (String) f.get("focus_code"),
                ((Number) f.get("expected_duration_minutes")).intValue(),
                f.get("source_planned_workout_id") == null ? null : ((Number) f.get("source_planned_workout_id")).longValue(),
                ((Number) f.get("plan_id")).longValue(), ejercicios);
    }

    private Map<String, Object> intento(long planId) {
        var filas = jdbc.queryForList("SELECT applied_sessions, conserved_sessions, progressed_sessions,"
                + " targeted_sessions, targeted_interventions, conservation_report::text AS conservation_report"
                + " FROM plan_adaptation_attempts WHERE resulting_plan_id = ?", planId);
        return filas.size() == 1 ? filas.get(0) : null;
    }

    private JsonNode leer(Object texto, List<String> dudas) {
        if (texto == null) return json.createObjectNode();
        try {
            return json.readTree(texto.toString());
        } catch (Exception e) {
            dudas.add("informe V32 ilegible: " + e.getMessage());
            return null;
        }
    }

    private static boolean contiene(Object array, long id) {
        if (array == null) return false;
        try {
            for (Object x : (Object[]) ((java.sql.Array) array).getArray())
                if (x != null && ((Number) x).longValue() == id) return true;
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException(e);
        }
        return false;
    }

    private static String intervencion(Map<String, Object> intento, long origen) {
        try {
            var sesiones = (Object[]) ((java.sql.Array) intento.get("targeted_sessions")).getArray();
            var tipos = (Object[]) ((java.sql.Array) intento.get("targeted_interventions")).getArray();
            for (int i = 0; i < sesiones.length && i < tipos.length; i++)
                if (sesiones[i] != null && ((Number) sesiones[i]).longValue() == origen) return String.valueOf(tipos[i]);
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private static boolean aplicada(Map<String, Object> intento, long origen) {
        return contiene(intento.get("applied_sessions"), origen);
    }

    private static boolean misma(JsonNode d, Ej e) {
        return eq(d.path("series"), e.series()) && eq(d.path("reps"), e.reps()) && eq(d.path("segundos"), e.segundos())
                && eq(d.path("descanso"), e.descanso());
    }

    private static boolean eq(JsonNode n, Number v) {
        return n.isMissingNode() || n.isNull() ? v == null : v != null && n.asLong() == v.longValue();
    }

    private static boolean igual(Ej a, Ej b) {
        return a.tipo().equals(b.tipo()) && Objects.equals(a.series(), b.series()) && Objects.equals(a.reps(), b.reps())
                && Objects.equals(a.segundos(), b.segundos()) && Objects.equals(a.descanso(), b.descanso());
    }

    /** {@code d} baja a {@code o} en alguna dimension, no sube en ninguna y conserva el descanso. */
    private static boolean baja(Ej d, Ej o) {
        if (!Objects.equals(d.descanso(), o.descanso()) || !d.tipo().equals(o.tipo())) return false;
        int[] a = {n(d.series()), n(d.reps()), n(d.segundos())};
        int[] b = {n(o.series()), n(o.reps()), n(o.segundos())};
        boolean alguna = false;
        for (int i = 0; i < 3; i++) {
            if (a[i] > b[i]) return false;
            if (a[i] < b[i]) alguna = true;
        }
        return alguna;
    }

    private static int n(Number x) {
        return x == null ? 0 : x.intValue();
    }
}
