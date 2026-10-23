package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.GoalCapabilityPolicy;
import main.web.services.fitsense.planning.domain.model.valueobjects.GoalCapabilityPolicy.Capability;
import main.web.services.fitsense.planning.domain.model.valueobjects.GoalCapabilityPolicy.Condition;
import main.web.services.fitsense.planning.domain.model.valueobjects.GoalCapabilityPolicy.Role;
import main.web.services.fitsense.planning.domain.model.valueobjects.MandatorySlot;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.SlotCoverage;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Paso 3 · FACTIBILIDAD POR OBJETIVO, en dos niveles y SOLO OBSERVACION.
 * <p>
 * <b>Nivel A, pool:</b> para cada capacidad que el objetivo exige (4A v6),
 * ¿hay suficientes ejercicios DISTINTOS y seguros en el conjunto elegible?
 * Umbral de 4C: 0 = no factible, 1-2 = fragil, 3 o mas = robusto. Una ruta que
 * el backend no puede comprobar es {@link Veredicto#NO_VERIFICABLE}, nunca
 * cobertura por suposicion.
 * <p>
 * <b>Nivel B, semana:</b> ¿la semana propuesta CONTIENE cada capacidad
 * principal? Es otra pregunta: un pool robusto de cardio no impide una semana
 * sin cardio. Aqui no se mira dosis, tiempo ni intensidad (Paso 4), asi que lo
 * mejor que puede decir este nivel es «presente, dosis pendiente de 4D», nunca
 * «valida».
 * <p>
 * <b>Reglas:</b>
 * <ul>
 *   <li>Solo las principales deciden el veredicto. Las secundarias se informan
 *       y no lo cambian nunca.</li>
 *   <li>Los cuatro slots resistidos solo se exigen a {@code HYPERTROPHY} y
 *       {@code STRENGTH} cuando son principales: no a {@code LOSE_WEIGHT}.</li>
 *   <li>Un circuito no acredita cardio: no hay representacion verificable de
 *       su organizacion. La ruta existe y es {@code NO_VERIFICABLE}.</li>
 *   <li>Los ejercicios de {@link #CLASIFICACION_EN_REVISION} se cuentan
 *       aparte y no son evidencia de cobertura.</li>
 * </ul>
 * Dominio PURO: no consulta nada, no decide rechazos.
 */
public final class GoalFeasibilityPolicy {

    private GoalFeasibilityPolicy() {}

    /**
     * Incidencia abierta del Paso 2: 21 ejercicios cuyo patron contradice su
     * nombre o su zona, pendientes de validacion con nombre, instrucciones y
     * GIF. Mientras tanto NO cuentan como evidencia de ninguna ruta. No se
     * excluyen del catalogo: eso lo decide la revision.
     * <ul>
     *   <li>LOCOMOTION_CARDIO fuera de la zona cardio: 59, 93, 298, 876, 902,
     *       903, 1117, 1374.</li>
     *   <li>MOBILITY sin «estiramiento» en el nombre: 55, 312, 465, 809, 1192
     *       (ademas retenido), 1194, 1257, 1310.</li>
     *   <li>Zona cardio con patron de fuerza: 54, 853, 1174, 1294, 1295.</li>
     * </ul>
     */
    public static final Set<Long> CLASIFICACION_EN_REVISION = Set.of(
            59L, 93L, 298L, 876L, 902L, 903L, 1117L, 1374L,
            55L, 312L, 465L, 809L, 1192L, 1194L, 1257L, 1310L,
            54L, 853L, 1174L, 1294L, 1295L);

    /** Del peor al mejor: el orden importa para combinar. */
    public enum Veredicto { NO_FACTIBLE, NO_VERIFICABLE, FRAGIL, ROBUSTO }

    /** Mecanismo (4B) por el que una capacidad puede cubrirse. */
    public enum Ruta {
        /** Los cuatro slots de 4C, cada uno con su umbral. */
        SLOTS_RESISTIDOS,
        /**
         * Trabajo resistido de cualquier patron. PROVISIONAL: la resistencia
         * muscular la distingue la dosis (4D), no el ejercicio.
         */
        RESISTENCIA_GENERAL,
        LOCOMOCION_DIRECTA,
        /** Sin representacion verificable: siempre NO_VERIFICABLE. */
        CIRCUITO,
        MOVILIDAD_DIRECTA,
        EQUILIBRIO_DIRECTO
    }

    /** Patrones que no son trabajo resistido. */
    static final Set<String> NO_RESISTIDOS =
            Set.of("LOCOMOTION_CARDIO", "MOBILITY", "BALANCE_CONTROL", "OTHER");

    public static List<Ruta> rutasDe(Capability capacidad) {
        return switch (capacidad) {
            case HYPERTROPHY, STRENGTH -> List.of(Ruta.SLOTS_RESISTIDOS);
            case MUSCULAR_ENDURANCE -> List.of(Ruta.RESISTENCIA_GENERAL);
            case CARDIO -> List.of(Ruta.LOCOMOCION_DIRECTA, Ruta.CIRCUITO);
            case MOBILITY -> List.of(Ruta.MOVILIDAD_DIRECTA);
            case BALANCE -> List.of(Ruta.EQUILIBRIO_DIRECTO);
            // Ni el catalogo ni el plan tienen como acreditar potencia.
            case POWER -> List.of();
        };
    }

    private static boolean perteneceARuta(CandidateExercise c, Ruta ruta) {
        String patron = c.movementPattern() == null ? null : c.movementPattern().trim().toUpperCase(Locale.ROOT);
        if (patron == null) return false;
        return switch (ruta) {
            case SLOTS_RESISTIDOS -> MandatorySlot.of(c).isPresent();
            case RESISTENCIA_GENERAL -> !NO_RESISTIDOS.contains(patron);
            case LOCOMOCION_DIRECTA -> "LOCOMOTION_CARDIO".equals(patron);
            case MOVILIDAD_DIRECTA -> "MOBILITY".equals(patron);
            case EQUILIBRIO_DIRECTO -> "BALANCE_CONTROL".equals(patron);
            case CIRCUITO -> false;
        };
    }

    private static Veredicto porUmbral(int distintos) {
        if (distintos == 0) return Veredicto.NO_FACTIBLE;
        return distintos < SlotCoverage.MINIMO_ROBUSTO ? Veredicto.FRAGIL : Veredicto.ROBUSTO;
    }

    private static Veredicto peor(Veredicto a, Veredicto b) {
        return a.ordinal() <= b.ordinal() ? a : b;
    }

    // =====================================================================
    // Nivel A: pool
    // =====================================================================

    /**
     * @param verificados   ids distintos que cuentan como evidencia (para
     *                      slots: los del slot peor cubierto)
     * @param enRevision    ids distintos de la ruta que estan en
     *                      {@link #CLASIFICACION_EN_REVISION}
     */
    public record RutaEvaluada(Ruta ruta, Veredicto veredicto, int verificados, int enRevision,
                               Map<MandatorySlot, Integer> porSlot, String motivo) {}

    /**
     * @param aplica   false si la condicion la descarta (POWER en principiante);
     *                 entonces {@code veredicto} es null
     */
    public record CapacidadEvaluada(Capability capacidad, Role papel, Condition condicion,
                                    boolean aplica, Veredicto veredicto, List<RutaEvaluada> rutas) {
        public String describe() {
            if (!aplica) return capacidad + "=NO_APLICA";
            return capacidad + "=" + veredicto + rutas.stream()
                    .map(r -> r.ruta() + ":" + r.veredicto()
                            + (r.ruta() == Ruta.SLOTS_RESISTIDOS ? r.porSlot().toString()
                            : r.ruta() == Ruta.CIRCUITO ? "" : "(" + r.verificados()
                            + (r.enRevision() > 0 ? "+" + r.enRevision() + "_en_revision" : "") + ")"))
                    .collect(Collectors.joining(",", "[", "]"));
        }
    }

    public record EvaluacionPool(String objetivo, Veredicto veredicto,
                                 List<CapacidadEvaluada> principales,
                                 List<CapacidadEvaluada> secundarias) {

        /** Principales que no tienen un pool factible y verificado. */
        public List<String> motivos() {
            var motivos = new ArrayList<String>();
            for (var c : principales) {
                if (c.veredicto() == Veredicto.ROBUSTO || c.veredicto() == Veredicto.FRAGIL) continue;
                for (var r : c.rutas())
                    motivos.add(c.capacidad() + "/" + r.ruta() + ": " + r.motivo());
            }
            return motivos;
        }

        public String describe() {
            return "version=%s objetivo=%s veredicto=%s principales=%s secundarias=%s".formatted(
                    GoalCapabilityPolicy.VERSION, objetivo, veredicto,
                    principales.stream().map(CapacidadEvaluada::describe).toList(),
                    secundarias.stream().map(CapacidadEvaluada::describe).toList());
        }
    }

    public static Optional<EvaluacionPool> evaluarPool(String goalType, String fitnessLevel,
                                                       List<CandidateExercise> elegibles) {
        var capacidades = GoalCapabilityPolicy.forGoal(goalType);
        if (capacidades.isEmpty()) return Optional.empty();
        var lista = elegibles == null ? List.<CandidateExercise>of() : elegibles.stream()
                .filter(Objects::nonNull).filter(c -> c.exerciseId() != null).toList();

        var principales = new ArrayList<CapacidadEvaluada>();
        var secundarias = new ArrayList<CapacidadEvaluada>();
        for (var req : capacidades.get().requirements()) {
            boolean aplica = req.mayApplyTo(fitnessLevel);
            CapacidadEvaluada evaluada;
            if (!aplica) {
                evaluada = new CapacidadEvaluada(req.capability(), req.role(), req.condition(), false, null, List.of());
            } else {
                var rutas = rutasDe(req.capability()).stream().map(r -> evaluarRuta(r, lista)).toList();
                var veredicto = rutas.stream().map(RutaEvaluada::veredicto)
                        .max(Comparator.naturalOrder()).orElse(Veredicto.NO_VERIFICABLE);
                // Una ruta inexistente o sin verificar no prueba inviabilidad:
                // NO_FACTIBLE solo si TODAS las rutas son NO_FACTIBLE.
                evaluada = new CapacidadEvaluada(req.capability(), req.role(), req.condition(), true,
                        veredicto, rutas);
            }
            (req.role() == Role.PRIMARY ? principales : secundarias).add(evaluada);
        }
        var global = principales.stream().map(CapacidadEvaluada::veredicto)
                .reduce(Veredicto.ROBUSTO, GoalFeasibilityPolicy::peor);
        return Optional.of(new EvaluacionPool(capacidades.get().goalType(), global,
                List.copyOf(principales), List.copyOf(secundarias)));
    }

    static RutaEvaluada evaluarRuta(Ruta ruta, List<CandidateExercise> elegibles) {
        if (ruta == Ruta.CIRCUITO)
            return new RutaEvaluada(ruta, Veredicto.NO_VERIFICABLE, 0, 0, Map.of(),
                    "el plan no tiene representacion verificable de un circuito: no acredita cardio");

        var verificados = elegibles.stream()
                .filter(c -> !CLASIFICACION_EN_REVISION.contains(c.exerciseId()))
                .filter(c -> perteneceARuta(c, ruta)).toList();
        int enRevision = (int) elegibles.stream()
                .filter(c -> CLASIFICACION_EN_REVISION.contains(c.exerciseId()))
                .filter(c -> perteneceARuta(c, ruta))
                .map(CandidateExercise::exerciseId).distinct().count();

        if (ruta == Ruta.SLOTS_RESISTIDOS) {
            // Ids distintos por slot: el conteo corregido en el Paso 0.
            var cobertura = SlotCoveragePolicy.ofEligible(verificados);
            var porSlot = new EnumMap<MandatorySlot, Integer>(MandatorySlot.class);
            MandatorySlot.ALL.forEach(s -> porSlot.put(s, cobertura.count(s)));
            var veredicto = switch (cobertura.verdict()) {
                case NO_FACTIBLE -> Veredicto.NO_FACTIBLE;
                case FRAGIL -> Veredicto.FRAGIL;
                case ROBUSTO -> Veredicto.ROBUSTO;
            };
            int peor = MandatorySlot.ALL.stream().mapToInt(cobertura::count).min().orElse(0);
            String motivo = veredicto == Veredicto.NO_FACTIBLE
                    ? "slots sin candidatos: " + cobertura.emptySlots()
                    : veredicto == Veredicto.FRAGIL ? "slots con 1-2 candidatos: " + cobertura.fragileSlots()
                    : "robusto";
            return new RutaEvaluada(ruta, veredicto, peor, enRevision, Map.copyOf(porSlot), motivo);
        }

        int distintos = (int) verificados.stream().map(CandidateExercise::exerciseId).distinct().count();
        var veredicto = porUmbral(distintos);
        String motivo = switch (veredicto) {
            case NO_FACTIBLE -> "0 candidatos verificados"
                    + (enRevision > 0 ? " (" + enRevision + " en revision de clasificacion, no cuentan)" : "");
            case FRAGIL -> distintos + " candidatos verificados";
            default -> "robusto";
        };
        return new RutaEvaluada(ruta, veredicto, distintos, enRevision, Map.of(), motivo);
    }

    // =====================================================================
    // Nivel B: semana
    // =====================================================================

    public enum Presencia {
        /** Hay colocaciones verificadas de la ruta. Dosis y tiempo: Paso 4. */
        PRESENTE_PENDIENTE_4D,
        /** Solo hay colocaciones que el backend no puede acreditar. */
        NO_VERIFICABLE,
        AUSENTE
    }

    public enum VeredictoSemana {
        PRINCIPALES_PRESENTES_PENDIENTE_4D,
        PRINCIPAL_NO_VERIFICABLE,
        PRINCIPAL_AUSENTE
    }

    public record PresenciaCapacidad(Capability capacidad, Role papel, boolean aplica, Presencia presencia,
                                     int colocaciones, int dias, String detalle) {
        public String describe() {
            return aplica ? "%s=%s(%d colocaciones, %d dias%s)".formatted(capacidad, presencia, colocaciones,
                    dias, detalle.isEmpty() ? "" : ", " + detalle) : capacidad + "=NO_APLICA";
        }
    }

    public record EvaluacionSemana(String objetivo, VeredictoSemana veredicto,
                                   List<PresenciaCapacidad> principales,
                                   List<PresenciaCapacidad> secundarias) {
        public String describe() {
            return "version=%s objetivo=%s veredicto=%s principales=%s secundarias=%s".formatted(
                    GoalCapabilityPolicy.VERSION, objetivo, veredicto,
                    principales.stream().map(PresenciaCapacidad::describe).toList(),
                    secundarias.stream().map(PresenciaCapacidad::describe).toList());
        }
    }

    public static Optional<EvaluacionSemana> evaluarSemana(String goalType, String fitnessLevel,
                                                           PlanDraft borrador,
                                                           List<CandidateExercise> elegibles) {
        var capacidades = GoalCapabilityPolicy.forGoal(goalType);
        if (capacidades.isEmpty()) return Optional.empty();
        var porId = new HashMap<Long, CandidateExercise>();
        if (elegibles != null) elegibles.stream().filter(Objects::nonNull)
                .filter(c -> c.exerciseId() != null).forEach(c -> porId.putIfAbsent(c.exerciseId(), c));

        // Colocaciones de la semana: (fecha, candidato). Un id fuera del
        // conjunto elegible no acredita nada (V1 ya lo rechaza).
        var colocaciones = new ArrayList<Map.Entry<LocalDate, CandidateExercise>>();
        if (borrador != null && borrador.workouts() != null)
            for (var w : borrador.workouts()) {
                if (w == null || w.exercises() == null) continue;
                for (var e : w.exercises()) {
                    if (e == null) continue;
                    var c = porId.get(e.exerciseId());
                    if (c != null) colocaciones.add(Map.entry(w.scheduledDate(), c));
                }
            }

        var principales = new ArrayList<PresenciaCapacidad>();
        var secundarias = new ArrayList<PresenciaCapacidad>();
        for (var req : capacidades.get().requirements()) {
            var p = req.mayApplyTo(fitnessLevel)
                    ? presencia(req.capability(), req.role(), colocaciones)
                    : new PresenciaCapacidad(req.capability(), req.role(), false, null, 0, 0, "");
            (req.role() == Role.PRIMARY ? principales : secundarias).add(p);
        }
        var veredicto = principales.stream().anyMatch(p -> p.presencia() == Presencia.AUSENTE)
                ? VeredictoSemana.PRINCIPAL_AUSENTE
                : principales.stream().anyMatch(p -> p.presencia() == Presencia.NO_VERIFICABLE)
                ? VeredictoSemana.PRINCIPAL_NO_VERIFICABLE
                : VeredictoSemana.PRINCIPALES_PRESENTES_PENDIENTE_4D;
        return Optional.of(new EvaluacionSemana(capacidades.get().goalType(), veredicto,
                List.copyOf(principales), List.copyOf(secundarias)));
    }

    private static PresenciaCapacidad presencia(Capability capacidad, Role papel,
                                                List<Map.Entry<LocalDate, CandidateExercise>> colocaciones) {
        var rutas = rutasDe(capacidad);
        if (rutas.isEmpty())
            return new PresenciaCapacidad(capacidad, papel, true, Presencia.NO_VERIFICABLE, 0, 0,
                    "sin ruta verificable");

        if (rutas.contains(Ruta.SLOTS_RESISTIDOS)) {
            var verificadas = colocaciones.stream()
                    .filter(e -> !CLASIFICACION_EN_REVISION.contains(e.getValue().exerciseId())).toList();
            var porSlot = new EnumMap<MandatorySlot, Integer>(MandatorySlot.class);
            for (var e : verificadas) MandatorySlot.of(e.getValue()).ifPresent(s -> porSlot.merge(s, 1, Integer::sum));
            var faltan = MandatorySlot.ALL.stream().filter(s -> !porSlot.containsKey(s)).toList();
            var deSlot = verificadas.stream().filter(e -> MandatorySlot.of(e.getValue()).isPresent()).toList();
            return new PresenciaCapacidad(capacidad, papel, true,
                    faltan.isEmpty() ? Presencia.PRESENTE_PENDIENTE_4D : Presencia.AUSENTE,
                    deSlot.size(), (int) deSlot.stream().map(Map.Entry::getKey).distinct().count(),
                    faltan.isEmpty() ? "" : "faltan slots " + faltan);
        }

        var deLaRuta = colocaciones.stream()
                .filter(e -> rutas.stream().anyMatch(r -> perteneceARuta(e.getValue(), r))).toList();
        var verificadas = deLaRuta.stream()
                .filter(e -> !CLASIFICACION_EN_REVISION.contains(e.getValue().exerciseId())).toList();
        int dias = (int) verificadas.stream().map(Map.Entry::getKey).distinct().count();
        if (!verificadas.isEmpty())
            return new PresenciaCapacidad(capacidad, papel, true, Presencia.PRESENTE_PENDIENTE_4D,
                    verificadas.size(), dias, "");
        if (!deLaRuta.isEmpty())
            return new PresenciaCapacidad(capacidad, papel, true, Presencia.NO_VERIFICABLE,
                    deLaRuta.size(), 0, "solo ejercicios en revision de clasificacion");
        return new PresenciaCapacidad(capacidad, papel, true, Presencia.AUSENTE, 0, 0,
                rutas.contains(Ruta.CIRCUITO) ? "sin ruta directa; un circuito no se acredita" : "");
    }

    /**
     * Si el objetivo exige los cuatro slots resistidos como principal. V23 (4C)
     * los exige hoy a TODOS los objetivos; cuando esto es false para un
     * objetivo, V23 y 4A v6 divergen.
     */
    public static boolean exigeSlotsResistidos(String goalType) {
        return GoalCapabilityPolicy.forGoal(goalType)
                .map(g -> g.primaries().stream().anyMatch(c -> rutasDe(c).contains(Ruta.SLOTS_RESISTIDOS)))
                .orElse(true);
    }
}
