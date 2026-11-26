package main.web.services.fitsense.planning.infrastructure.generation.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDate;
import java.util.*;

/**
 * H2-v3 · LECTOR LITERAL DE PR-2.1 (solo pruebas, sin IA). Aplica a una propuesta las cinco reglas de la JERARQUIA DE
 * RESTRICCIONES (regla 22) usando UNICAMENTE lo que la instantanea de PR-2.1 le da al modelo (available_exercises,
 * dosis_por_clase o capacidades.dosis, volumen_semanal, suggested_split, slots_exigibles, max_session_seconds):
 * <ol>
 *   <li>DOSIS: cada colocacion con la dosis exacta de su clase o de uno de sus codigos (series dentro del rango);</li>
 *   <li>TECHOS V26: quita colocaciones del grupo que se pasa (por sesion y por semana);</li>
 *   <li>REPARTO V15: cambia la colocacion que sobra por otra de body_parts_con_material, o la quita;</li>
 *   <li>COBERTURA V23: cambia una colocacion del mismo grupo por una del slot que falta;</li>
 *   <li>DURACION: quita secundarias si una sesion pasa de max_session_seconds (formula de la regla 17).</li>
 * </ol>
 * Sirve para comprobar, sin coste, que las instrucciones de PR-2.1 son COHERENTES entre si: si seguirlas al pie de la
 * letra produce una semana que el validador acepta, ninguna obliga a romper otra. No es un generador ni predice lo que
 * hara el modelo; no se usa fuera de las pruebas.
 */
final class AplicadorJerarquiaPR21 {

    private static final ObjectMapper JSON = new ObjectMapper();
    /** Regla 18: grupos que no se trabajan dos dias seguidos. */
    private static final Set<String> RECUPERACION = Set.of("chest", "back", "shoulders", "upper legs");

    record Resultado(String salida, List<String> cambios) {}

    private final JsonNode snap;
    private final JsonNode c;
    private final Map<Long, JsonNode> ex = new LinkedHashMap<>();
    private final Set<Long> complementos = new HashSet<>();
    private final Set<Long> noCuentan = new HashSet<>();
    private final Map<String, JsonNode> sesionPorFecha = new HashMap<>();
    private final List<String> cambios = new ArrayList<>();
    private final boolean porClase;

    AplicadorJerarquiaPR21(String instantaneaPR21) throws Exception {
        snap = JSON.readTree(instantaneaPR21);
        if (!PlanPromptBuilder.VERSION_H2V3.equals(snap.path("prompt_version").asText()))
            throw new IllegalArgumentException("se necesita la instantanea de PR-2.1");
        c = snap.get("constraints");
        snap.get("available_exercises").forEach(e -> ex.put(e.get("exercise_id").asLong(), e));
        c.path("complementos_no_cuentan").forEach(x -> complementos.add(x.asLong()));
        c.path("volumen_semanal").path("no_cuentan").forEach(x -> noCuentan.add(x.asLong()));
        c.get("suggested_split").forEach(s -> sesionPorFecha.put(s.get("scheduled_date").asText(), s));
        porClase = c.hasNonNull("dosis_por_clase");
    }

    Resultado aplicar(String salidaModelo) throws Exception {
        JsonNode raiz = JSON.readTree(salidaModelo);
        if (raiz.isTextual()) raiz = JSON.readTree(raiz.asText());
        var plan = (ObjectNode) raiz;
        var sesiones = new ArrayList<ObjectNode>();
        plan.get("workouts").forEach(w -> sesiones.add((ObjectNode) w));
        // Fuera de available_exercises no hay dosis que copiar: el modelo no puede usarlos.
        for (var w : sesiones) quitarSi(w, e -> !ex.containsKey(id(e)), "fuera de available_exercises");
        for (var w : sesiones) for (var e : ejercicios(w)) dosis(e);
        for (int vuelta = 0; vuelta < 6; vuelta++) {
            int antes = cambios.size();
            techos(sesiones);
            reparto(sesiones);
            cobertura(sesiones);
            duracion(sesiones);
            if (cambios.size() == antes) break;
        }
        return new Resultado(JSON.writeValueAsString(plan), List.copyOf(cambios));
    }

    // ------------------------------------------------------------------ 1) dosis

    private void dosis(ObjectNode e) {
        var cand = ex.get(id(e));
        if (porClase) {
            var clase = cand.path("clase_dosis").asText(null);
            if (clase == null) return;
            for (var d : c.get("dosis_por_clase"))
                if (d.get("clase").asText().equals(clase)) {
                    fijar(e, d.get("series").asInt(), d.path("reps"), null, d.get("descanso_segundos").asInt());
                    return;
                }
            return;
        }
        var codigos = new ArrayList<JsonNode>();
        cand.path("dosis").forEach(cod -> {
            for (var d : c.path("capacidades").path("dosis"))
                if (d.get("codigo").asText().equals(cod.asText())) codigos.add(d);
        });
        if (codigos.isEmpty()) return;
        JsonNode elegida = null;
        Integer reps = e.hasNonNull("planned_reps") ? e.get("planned_reps").asInt() : null;
        Integer seg = e.hasNonNull("planned_duration_seconds") ? e.get("planned_duration_seconds").asInt() : null;
        Integer desc = e.hasNonNull("rest_seconds") ? e.get("rest_seconds").asInt() : null;
        for (var d : codigos)
            if (igual(d, reps, seg) && Objects.equals(desc, d.get("descanso_segundos").asInt())) { elegida = d; break; }
        if (elegida == null) for (var d : codigos) if (igual(d, reps, seg)) { elegida = d; break; }
        boolean fuerza = false;
        for (var p : c.path("capacidades").path("principales_factibles")) fuerza |= p.asText().equals("STRENGTH");
        if (elegida == null && fuerza && cand.hasNonNull("slot"))
            for (var d : codigos) if (d.get("capacidad").asText().equals("STRENGTH")) { elegida = d; break; }
        if (elegida == null) elegida = codigos.get(0);
        int s = e.hasNonNull("planned_sets") ? e.get("planned_sets").asInt() : elegida.get("series").asInt();
        s = Math.max(elegida.get("series").asInt(), Math.min(elegida.get("series_max").asInt(), s));
        fijar(e, s, elegida.path("reps"), elegida.path("segundos"), elegida.get("descanso_segundos").asInt());
    }

    private static boolean igual(JsonNode d, Integer reps, Integer seg) {
        return d.hasNonNull("reps") ? Objects.equals(reps, d.get("reps").asInt())
                : Objects.equals(seg, d.path("segundos").asInt());
    }

    private void fijar(ObjectNode e, int series, JsonNode reps, JsonNode segundos, int descanso) {
        String antes = dosisDe(e);
        e.put("planned_sets", series);
        if (reps != null && !reps.isMissingNode() && !reps.isNull()) {
            e.put("planned_reps", reps.asInt());
            e.putNull("planned_duration_seconds");
        } else if (segundos != null && !segundos.isMissingNode() && !segundos.isNull()) {
            e.put("planned_duration_seconds", segundos.asInt());
            e.putNull("planned_reps");
        }
        e.put("rest_seconds", descanso);
        if (!antes.equals(dosisDe(e))) cambios.add("dosis " + id(e) + ": " + antes + " -> " + dosisDe(e));
    }

    private static String dosisDe(JsonNode e) {
        return e.path("planned_sets").asText() + "x" + (e.hasNonNull("planned_reps") ? e.get("planned_reps").asText()
                : e.path("planned_duration_seconds").asText() + "s") + "/" + e.path("rest_seconds").asText();
    }

    // ------------------------------------------------------------------ 2) techos V26

    private void techos(List<ObjectNode> sesiones) {
        var vol = c.path("volumen_semanal");
        if (vol.isMissingNode() || vol.isNull()) return;
        int techoSesion = vol.get("techo_series_sesion").asInt(), techoSemana = vol.get("techo_series_semana").asInt();
        for (var w : sesiones)
            for (String g; (g = grupoSobre(series(w), techoSesion)) != null; )
                if (!quitarUna(w, g, sesiones, "V26 sesion")) break;
        for (String g; (g = grupoSobre(semana(sesiones), techoSemana)) != null; ) {
            String grupo = g;
            var w = sesiones.stream().max(Comparator.comparingInt(x -> series(x).getOrDefault(grupo, 0))).orElseThrow();
            if (!quitarUna(w, grupo, sesiones, "V26 semana")) break;
        }
    }

    private Map<String, Integer> series(ObjectNode w) {
        var m = new TreeMap<String, Integer>();
        for (var e : ejercicios(w)) {
            long i = id(e);
            if (complementos.contains(i) || noCuentan.contains(i)) continue;
            m.merge(grupo(i), e.path("planned_sets").asInt(), Integer::sum);
        }
        return m;
    }

    private Map<String, Integer> semana(List<ObjectNode> sesiones) {
        var m = new TreeMap<String, Integer>();
        sesiones.forEach(w -> series(w).forEach((g, n) -> m.merge(g, n, Integer::sum)));
        return m;
    }

    private static String grupoSobre(Map<String, Integer> m, int techo) {
        return m.entrySet().stream().filter(x -> x.getValue() > techo).map(Map.Entry::getKey).findFirst().orElse(null);
    }

    // ------------------------------------------------------------------ 3) reparto V15

    private void reparto(List<ObjectNode> sesiones) {
        for (var w : sesiones) {
            var s = sesionPorFecha.get(w.get("scheduled_date").asText());
            if (s == null) continue;
            for (int guarda = 0; guarda < 10 && s.hasNonNull("max_per_body_part_por_n_trabajo"); guarda++) {
                var trabajo = ejercicios(w).stream().filter(e -> !complementos.contains(id(e))).toList();
                var tope = s.get("max_per_body_part_por_n_trabajo").get(String.valueOf(trabajo.size()));
                if (tope == null) break;
                var cuenta = new TreeMap<String, Integer>();
                trabajo.forEach(e -> cuenta.merge(grupo(id(e)), 1, Integer::sum));
                var sobra = cuenta.entrySet().stream().filter(x -> x.getValue() > tope.asInt()).map(Map.Entry::getKey)
                        .findFirst().orElse(null);
                if (sobra == null) break;
                sustituir(w, s, sobra, cuenta, tope.asInt(), sesiones, "V15 reparto");
            }
            // Minimo de grupos: si faltan, el grupo que mas tiene cede una colocacion a uno que falte.
            for (int guarda = 0; guarda < 4; guarda++) {
                var trabajo = ejercicios(w).stream().filter(e -> !complementos.contains(id(e))).toList();
                var cuenta = new TreeMap<String, Integer>();
                trabajo.forEach(e -> cuenta.merge(grupo(id(e)), 1, Integer::sum));
                var material = new ArrayList<String>();
                s.get("body_parts_con_material").forEach(g -> material.add(g.asText()));
                long cubiertos = material.stream().filter(cuenta::containsKey).count();
                if (cubiertos >= s.path("min_body_parts").asInt(0) || cuenta.isEmpty()) break;
                var mas = cuenta.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
                if (cuenta.get(mas) < 2) break;
                var tabla = s.path("max_per_body_part_por_n_trabajo").get(String.valueOf(trabajo.size()));
                int antes = cambios.size();
                sustituir(w, s, mas, cuenta, tabla == null ? Integer.MAX_VALUE : tabla.asInt(), sesiones, "V15 grupos");
                if (cambios.size() == antes) break;
            }
        }
    }

    /** Cambia una colocacion de {@code sobra} por una de un grupo con material que falte o tenga menos, o la quita. */
    private void sustituir(ObjectNode w, JsonNode s, String sobra, Map<String, Integer> cuenta, int tope,
                           List<ObjectNode> sesiones, String motivo) {
                var quitar = elegirParaQuitar(w, sobra, sesiones);
                var destinos = new ArrayList<String>();
                s.get("body_parts_con_material").forEach(g -> destinos.add(g.asText()));
                destinos.removeIf(g -> g.equals(sobra) || cuenta.getOrDefault(g, 0) + 1 > tope);
                destinos.sort(Comparator.comparingInt(g -> cuenta.getOrDefault(g, 0)));
                ObjectNode nuevo = null;
                for (var g : destinos) {
                    nuevo = candidato(w, g, sesiones, null);
                    if (nuevo != null) break;
                }
                if (nuevo != null) {
                    reemplazar(w, quitar, nuevo, motivo);
                } else if (motivo.equals("V15 reparto")) {
                    quitarNodo(w, quitar, "V15 sin grupo con material");
                }
    }

    // ------------------------------------------------------------------ 4) cobertura V23

    private void cobertura(List<ObjectNode> sesiones) {
        var exigidos = new ArrayList<String>();
        c.path("slots_exigibles").forEach(x -> exigidos.add(x.asText()));
        for (var slot : exigidos) {
            if (slotsCubiertos(sesiones).contains(slot)) continue;
            String grupoSlot = ex.values().stream().filter(e -> slot.equals(e.path("slot").asText(null)))
                    .map(e -> e.get("body_part").asText()).findFirst().orElse(null);
            if (grupoSlot == null) continue;
            boolean hecho = false;
            for (var w : sesiones) {
                if (hecho) break;
                for (var e : ejercicios(w)) {
                    if (!grupoSlot.equals(grupo(id(e)))) continue;
                    String suyo = ex.get(id(e)).path("slot").asText(null);
                    if (suyo != null && cuantosConSlot(sesiones, suyo) <= 1) continue;
                    var nuevo = candidato(w, grupoSlot, sesiones, slot);
                    if (nuevo == null) continue;
                    reemplazar(w, e, nuevo, "V23 " + slot);
                    hecho = true;
                    break;
                }
            }
        }
    }

    private Set<String> slotsCubiertos(List<ObjectNode> sesiones) {
        var s = new TreeSet<String>();
        for (var w : sesiones) for (var e : ejercicios(w)) {
            var slot = ex.get(id(e)).path("slot").asText(null);
            if (slot != null) s.add(slot);
        }
        return s;
    }

    private int cuantosConSlot(List<ObjectNode> sesiones, String slot) {
        int n = 0;
        for (var w : sesiones) for (var e : ejercicios(w)) if (slot.equals(ex.get(id(e)).path("slot").asText(null))) n++;
        return n;
    }

    // ------------------------------------------------------------------ 5) duracion

    private void duracion(List<ObjectNode> sesiones) {
        if (!c.hasNonNull("max_session_seconds")) return;
        long max = c.get("max_session_seconds").asLong();
        for (var w : sesiones)
            while (segundos(w) > max) {
                var e = ejercicios(w).stream().filter(x -> !ex.get(id(x)).hasNonNull("slot"))
                        .reduce((a, b) -> b).orElse(null);
                if (e == null) break;
                quitarNodo(w, e, "duracion");
            }
    }

    /** Regla 17: 5 min + por ejercicio trabajo + (series - 1) x descanso + 1 min entre ejercicios. */
    static long segundos(ObjectNode w) {
        long t = 300;
        int n = 0;
        for (var e : w.get("exercises")) {
            int s = e.path("planned_sets").asInt();
            long trabajo = e.hasNonNull("planned_reps") ? (long) s * e.get("planned_reps").asInt() * 3
                    : (long) s * e.path("planned_duration_seconds").asInt();
            t += trabajo + (long) Math.max(0, s - 1) * e.path("rest_seconds").asInt();
            n++;
        }
        return t + Math.max(0, n - 1) * 60L;
    }

    // ------------------------------------------------------------------ utilidades

    /** Un ejercicio del grupo (y del slot, si se pide) que no rompa la regla 18 ni se repita en la sesion. */
    private ObjectNode candidato(ObjectNode w, String grupo, List<ObjectNode> sesiones, String slot) {
        var fecha = LocalDate.parse(w.get("scheduled_date").asText());
        var usados = new HashSet<Long>();
        var vecinos = new HashSet<Long>();
        boolean grupoEnVecino = false;
        for (var o : sesiones) {
            var f = LocalDate.parse(o.get("scheduled_date").asText());
            for (var e : ejercicios(o)) {
                if (o == w) usados.add(id(e));
                else if (Math.abs(f.toEpochDay() - fecha.toEpochDay()) == 1) {
                    vecinos.add(id(e));
                    if (grupo.equals(grupo(id(e)))) grupoEnVecino = true;
                }
            }
        }
        if (RECUPERACION.contains(grupo) && grupoEnVecino) return null;
        for (var cand : ex.values()) {
            long i = cand.get("exercise_id").asLong();
            if (!grupo.equals(cand.get("body_part").asText()) || usados.contains(i) || vecinos.contains(i)) continue;
            if (complementos.contains(i) || !cand.path("prescription_type").asText().equals("SETS_REPS")) continue;
            if (slot != null && !slot.equals(cand.path("slot").asText(null))) continue;
            if (porClase && !cand.hasNonNull("clase_dosis")) continue;
            // Regla 20: en PUSH, de upper arms solo triceps; en PULL, solo biceps.
            String foco = w.path("focus_code").asText(), musculo = cand.path("target_muscle").asText("");
            if (grupo.equals("upper arms") && foco.equals("PUSH") && musculo.contains("biceps")) continue;
            if (grupo.equals("upper arms") && foco.equals("PULL") && musculo.contains("triceps")) continue;
            var nuevo = JSON.createObjectNode();
            nuevo.put("exercise_id", i);
            nuevo.put("prescription_type", "SETS_REPS");
            nuevo.putNull("planned_sets");
            int antes = cambios.size();
            dosis(nuevo);
            while (cambios.size() > antes) cambios.remove(cambios.size() - 1);
            if (!nuevo.hasNonNull("planned_sets")) continue;
            return nuevo;
        }
        return null;
    }

    private ObjectNode elegirParaQuitar(ObjectNode w, String grupo, List<ObjectNode> sesiones) {
        ObjectNode ultimo = null;
        for (var e : ejercicios(w)) {
            if (!grupo.equals(grupo(id(e)))) continue;
            String slot = ex.get(id(e)).path("slot").asText(null);
            if (slot == null || cuantosConSlot(sesiones, slot) > 1) ultimo = e;
        }
        if (ultimo != null) return ultimo;
        return ejercicios(w).stream().filter(e -> grupo.equals(grupo(id(e)))).reduce((a, b) -> b).orElseThrow();
    }

    private boolean quitarUna(ObjectNode w, String grupo, List<ObjectNode> sesiones, String motivo) {
        if (ejercicios(w).stream().noneMatch(e -> grupo.equals(grupo(id(e))))) return false;
        quitarNodo(w, elegirParaQuitar(w, grupo, sesiones), motivo);
        return true;
    }

    private void quitarNodo(ObjectNode w, JsonNode e, String motivo) {
        var arr = (ArrayNode) w.get("exercises");
        for (int i = 0; i < arr.size(); i++)
            if (arr.get(i) == e) {
                arr.remove(i);
                cambios.add(motivo + ": quita " + id(e) + " del " + w.get("scheduled_date").asText());
                renumerar(arr);
                return;
            }
    }

    private void reemplazar(ObjectNode w, JsonNode viejo, ObjectNode nuevo, String motivo) {
        var arr = (ArrayNode) w.get("exercises");
        for (int i = 0; i < arr.size(); i++)
            if (arr.get(i) == viejo) {
                nuevo.put("exercise_order", viejo.path("exercise_order").asInt(i + 1));
                arr.set(i, nuevo);
                cambios.add(motivo + ": " + id(viejo) + " -> " + id(nuevo) + " el " + w.get("scheduled_date").asText());
                return;
            }
    }

    private void quitarSi(ObjectNode w, java.util.function.Predicate<JsonNode> p, String motivo) {
        for (var e : List.copyOf(ejercicios(w))) if (p.test(e)) quitarNodo(w, e, motivo);
    }

    private static void renumerar(ArrayNode arr) {
        for (int i = 0; i < arr.size(); i++) ((ObjectNode) arr.get(i)).put("exercise_order", i + 1);
    }

    private static List<ObjectNode> ejercicios(JsonNode w) {
        var l = new ArrayList<ObjectNode>();
        w.get("exercises").forEach(e -> l.add((ObjectNode) e));
        return l;
    }

    private static long id(JsonNode e) {
        return e.get("exercise_id").asLong();
    }

    private String grupo(long id) {
        var e = ex.get(id);
        return e == null ? "?" : e.get("body_part").asText();
    }
}
