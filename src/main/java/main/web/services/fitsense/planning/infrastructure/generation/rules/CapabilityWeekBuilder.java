package main.web.services.fitsense.planning.infrastructure.generation.rules;

import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.model.valueobjects.GoalCapabilityPolicy.Capability;
import main.web.services.fitsense.planning.domain.services.CapabilityCoveragePolicy;
import main.web.services.fitsense.planning.domain.services.CapabilityPrescriptionPolicy;
import main.web.services.fitsense.planning.domain.services.CapabilityPrescriptionPolicy.Clase;
import main.web.services.fitsense.planning.domain.services.CatalogEligibilityPolicy;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.domain.services.WeeklySplitPlanner;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * La semana POR CAPACIDADES (4A v6 + 4D v4) de INCREASE_STRENGTH, LOSE_WEIGHT y
 * GENERAL_FITNESS. GAIN_MUSCLE no pasa por aqui.
 * <p>
 * <b>El orden es la regla de 4A v6:</b>
 * <ol>
 *   <li><b>Principales factibles primero</b>, en cada sesion:
 *     <ul>
 *       <li>INCREASE_STRENGTH: trabajo estructural (los slots de 4C que la semana
 *           aun no cubre van antes) con dosis de FUERZA.</li>
 *       <li>LOSE_WEIGHT: dos bloques de CARDIO, o uno si no hay mas que uno
 *           utilizable ese dia.</li>
 *       <li>GENERAL_FITNESS: estructurales con dosis de FUERZA para los slots
 *           que faltan, una colocacion de RESISTENCIA MUSCULAR, un bloque de
 *           CARDIO y uno de MOVILIDAD.</li>
 *     </ul></li>
 *   <li><b>Despues, el tiempo que queda hasta el minimo de V20</b> se completa
 *       con SECUNDARIAS: hipertrofia (INCREASE_STRENGTH, GENERAL_FITNESS) o
 *       fuerza (LOSE_WEIGHT). Si una principal no es factible, su tiempo lo
 *       llenan las secundarias sin desplazar a las demas principales. El
 *       equilibrio de GENERAL_FITNESS entra solo si cabe (oportunista).</li>
 *   <li><b>Si la sesion pasa del techo</b>, se quita primero lo secundario y
 *       despues lo principal que ya este representado otro dia de la semana;
 *       nunca la unica colocacion de una principal.</li>
 * </ol>
 * La dosis de cada colocacion la da {@link CapabilityPrescriptionPolicy}, la
 * misma que exige V24 y acredita V25. Solo se usan ejercicios VERIFICADOS: un
 * ejercicio en revision de clasificacion o sin capacidad del objetivo no se
 * elige. No se relaja ninguna restriccion: todo sale del conjunto elegible.
 * <p>
 * Las reglas de coherencia de siempre se respetan igual: el trabajo resistido
 * lo elige {@link ExerciseSelector} (V15, V18, V22, topes de grupos pequenos),
 * nada se repite del dia anterior si los dias estan pegados (V18) y un
 * complemento de cardio o movilidad solo trabaja zonas que la propia sesion ya
 * entrena o que no necesitan recuperacion.
 */
class CapabilityWeekBuilder {

    interface Cierre {
        PlanDraft aplicar(PlanDraft draft, PlanGenerationContext context, String justificacion);
    }

    /** Las mismas cuatro de V18. */
    private static final Set<String> RECUPERACION = Set.of("chest", "back", "shoulders", "upper legs");

    private record Colocacion(CandidateExercise candidato, PlanDraft.DraftExercise ejercicio,
                              Capability capacidad, boolean principal) {}

    private final PlanGenerationContext context;
    private final SessionDurationEstimator estimador;
    private final Random random;
    private final String objetivo;
    private final String nivel;
    private final main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams.RepLimits limites;
    private final Set<Capability> principales;
    private final Set<Capability> factibles;
    private final Set<Long> recientes;
    private final Set<Long> complementos;
    private final Map<Capability, Integer> enSemana = new EnumMap<>(Capability.class);
    private final Set<Long> usadosEnSemana = new HashSet<>();
    /** El trabajo resistido prescribible del objetivo: de aqui sale el cambio de {@link #asegurarGrupos}. */
    private List<CandidateExercise> resistidos = List.of();

    CapabilityWeekBuilder(PlanGenerationContext context, SessionDurationEstimator estimador, long semilla) {
        this.context = context;
        this.estimador = estimador;
        this.random = new Random(semilla);
        this.objetivo = context.profile().goalType();
        this.nivel = context.profile().fitnessLevel();
        this.limites = context.prescription() == null ? null : context.prescription().repLimits();
        this.principales = GoalCapabilityPolicy.forGoal(objetivo)
                .map(GoalCapabilityPolicy.GoalCapabilities::primaries).orElse(Set.of());
        var f = CapabilityCoveragePolicy.factibilidad(objetivo, nivel, context.availableExercises(), limites);
        this.factibles = f.values().stream().filter(CapabilityCoveragePolicy.Factibilidad::factible)
                .map(CapabilityCoveragePolicy.Factibilidad::capacidad)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Capability.class)));
        this.recientes = context.previousWeek() == null ? Set.of()
                : new HashSet<>(context.previousWeek().exerciseIdsUsedLast7Days());
        this.complementos = CatalogEligibilityPolicy.complementos(context.availableExercises(), objetivo);
    }

    private boolean principalFactible(Capability c) {
        return principales.contains(c) && factibles.contains(c);
    }

    private boolean asignable(Capability c) {
        return CapabilityPrescriptionPolicy.asignables(objetivo, nivel).contains(c) && factibles.contains(c);
    }

    PlanDraft construir(Cierre cierre) {
        var split = context.suggestedSplit();
        int minutosSesion = context.effectiveSessionMinutes();
        int techo = context.profile().maxSessionMinutes();
        int piso = PlanDraftValidator.minimumSessionMinutes(context);
        int maxDificultad = context.effectiveMaxDifficulty();

        var verificados = context.availableExercises().stream()
                .filter(c -> c.difficulty() <= maxDificultad)
                // Solo lo que se puede PRESCRIBIR con una capacidad del objetivo:
                // un isometrico no tiene dosis de fuerza ni de hipertrofia.
                .filter(c -> CapabilityPrescriptionPolicy.admisibles(c, objetivo, nivel).stream()
                        .anyMatch(cap -> CapabilityPrescriptionPolicy.dosis(cap, c, objetivo, nivel, limites).isPresent()))
                .toList();
        resistidos = verificados.stream()
                .filter(c -> !complementos.contains(c.exerciseId()))
                .filter(c -> {
                    var clase = CapabilityPrescriptionPolicy.claseDe(c);
                    return clase == Clase.ESTRUCTURAL || clase == Clase.RESISTIDO;
                })
                .toList();
        var cardio = deClase(verificados, Clase.CARDIO, Capability.CARDIO);
        var movilidad = deClase(verificados, Clase.MOVILIDAD, Capability.MOBILITY);
        var equilibrio = deClase(verificados, Clase.EQUILIBRIO, Capability.BALANCE);

        var selector = new ExerciseSelector(resistidos, maxDificultad, recientes, random);
        selector.preferirEstructurales(true);
        var slotsPorCubrir = new LinkedHashSet<MandatorySlot>();
        MandatorySlot.ALL.stream().filter(selector::haySlot).forEach(slotsPorCubrir::add);

        var sesiones = new ArrayList<PlanDraft.DraftWorkout>();
        Set<Long> idsDiaAnterior = Set.of();
        Set<String> gruposDiaAnterior = Set.of();
        LocalDate fechaAnterior = null;
        for (var sesion : split) {
            var fecha = sesion.date();
            var enfoque = sesion.focus();
            boolean pegado = fechaAnterior != null && fechaAnterior.plusDays(1).equals(fecha);
            var bloqueados = pegado ? idsDiaAnterior : Set.<Long>of();
            var gruposBloqueados = pegado ? gruposDiaAnterior : Set.<String>of();

            var dia = new ArrayList<Colocacion>();
            var resistidosDia = new ArrayList<CandidateExercise>();
            selector.block(bloqueados);
            selector.slotsPendientes(slotsPorCubrir);
            var pendientesAlEmpezar = new LinkedHashSet<>(slotsPorCubrir);

            // 1 · PRINCIPALES.
            int nResistidos = resistidosPrincipales(minutosSesion);
            if (nResistidos > 0) {
                var elegidos = new ArrayList<>(selector.pick(enfoque, nResistidos));
                // INCREASE_STRENGTH: la cobertura de grupos del selector puede
                // meter un ejercicio no estructural, que solo daria hipertrofia
                // (secundaria). Se cambia por una estructural si V15 lo permite:
                // la secundaria no ocupa el sitio de la principal.
                if ("INCREASE_STRENGTH".equals(objetivo))
                    for (int k = 0; k < elegidos.size(); k++)
                        if (CapabilityPrescriptionPolicy.claseDe(elegidos.get(k)) != Clase.ESTRUCTURAL) {
                            var cambio = selector.estructuralQueSustituyaA(enfoque, elegidos, elegidos.get(k));
                            if (cambio.isEmpty()) continue;
                            var con = new ArrayList<>(elegidos);
                            con.set(k, cambio.get());
                            // Sin perder grupos del enfoque (V15: al menos dos).
                            if (grupos(con) >= grupos(elegidos)) elegidos.set(k, cambio.get());
                        }
                var capacidades = capacidadesResistidasPrincipales(elegidos, slotsPorCubrir);
                for (int k = 0; k < elegidos.size(); k++) {
                    var c = elegidos.get(k);
                    var colocacion = colocar(c, capacidades.get(k), true);
                    if (colocacion == null) continue;
                    dia.add(colocacion);
                    resistidosDia.add(c);
                    if (cubreSlotPendiente(colocacion)) MandatorySlot.of(c).ifPresent(slotsPorCubrir::remove);
                }
            }
            if (principalFactible(Capability.CARDIO)) {
                int bloques = "LOSE_WEIGHT".equals(objetivo) ? 2 : 1;
                for (int b = 0; b < bloques; b++) {
                    var c = elegirComplemento(cardio, enfoque, dia, bloqueados, gruposBloqueados);
                    if (c.isEmpty()) break;
                    dia.add(colocar(c.get(), Capability.CARDIO, true));
                }
            }
            if (principalFactible(Capability.MOBILITY)) {
                elegirComplemento(movilidad, enfoque, dia, bloqueados, gruposBloqueados)
                        .ifPresent(c -> dia.add(colocar(c, Capability.MOBILITY, true)));
            }

            // 1b · En la ULTIMA sesion, la FUERZA principal completa los patrones
            // que la semana aun no tiene, aunque el piso ya este cubierto: es
            // incorporar la principal entera, no relleno. Las sesiones anteriores
            // ya eligen primero los slots pendientes (asi la fuerza se reparte por
            // la semana); esto solo garantiza el cierre. Solo si cabe bajo el techo.
            boolean ultimaSesion = sesion.equals(split.get(split.size() - 1));
            if (ultimaSesion && principalFactible(Capability.STRENGTH) && !"LOSE_WEIGHT".equals(objetivo))
                for (var slot : new ArrayList<>(slotsPorCubrir)) {
                    if (dia.size() >= CapabilityPrescriptionPolicy.MAX_COLOC_DIA) break;
                    var actuales = new ArrayList<>(resistidosDia);
                    var extra = selector.pickAdditional(enfoque, actuales, true, ExerciseSelector.deSlot(slot));
                    if (extra.isEmpty()) continue;
                    var col = colocar(extra.get(), Capability.STRENGTH, true);
                    if (col == null) continue;
                    var con = new ArrayList<>(dia);
                    con.add(col);
                    if (minutos(fecha, enfoque, con) > techo) continue;
                    dia.add(col);
                    resistidosDia.add(extra.get());
                    slotsPorCubrir.remove(slot);
                }

            // 2 · SECUNDARIAS, hasta el piso de V20 y sin pasar del techo.
            if (piso > 0) {
                while (dia.size() < CapabilityPrescriptionPolicy.MAX_COLOC_DIA && minutos(fecha, enfoque, dia) < piso) {
                    var extra = siguienteRelleno(selector, enfoque, resistidosDia, cardio, dia, bloqueados,
                            gruposBloqueados, slotsPorCubrir);
                    if (extra == null) break;
                    var con = new ArrayList<>(dia);
                    con.add(extra);
                    if (minutos(fecha, enfoque, con) > techo) break;
                    dia.add(extra);
                    var clase = CapabilityPrescriptionPolicy.claseDe(extra.candidato());
                    if (!complementos.contains(extra.candidato().exerciseId())
                            && (clase == Clase.ESTRUCTURAL || clase == Clase.RESISTIDO)) {
                        resistidosDia.add(extra.candidato());
                        if (cubreSlotPendiente(extra)) MandatorySlot.of(extra.candidato()).ifPresent(slotsPorCubrir::remove);
                    }
                }
            }
            // Si con el tope de colocaciones no se llega al piso, se suben series
            // hasta 3 (la ampliacion que admite la politica), nunca descansos.
            if (piso > 0 && minutos(fecha, enfoque, dia) < piso) ampliarSeries(fecha, enfoque, dia, piso, techo);

            // Equilibrio: oportunista, una vez por semana y solo si cabe.
            if (asignable(Capability.BALANCE) && enSemana.getOrDefault(Capability.BALANCE, 0) == 0
                    && dia.size() < CapabilityPrescriptionPolicy.MAX_COLOC_DIA) {
                var c = elegirComplemento(equilibrio, enfoque, dia, bloqueados, gruposBloqueados);
                if (c.isPresent()) {
                    var con = new ArrayList<>(dia);
                    var col = colocar(c.get(), Capability.BALANCE, false);
                    con.add(col);
                    if (col != null && minutos(fecha, enfoque, con) <= techo && respetaMitad(enfoque, con))
                        dia.add(col);
                }
            }

            // 3 · TECHO: primero lo secundario, despues lo principal repetido.
            recortar(fecha, enfoque, dia, techo);

            // 4 · GRUPOS DEL ENFOQUE. Solo si la sesion se quedo con un unico
            // grupo de trabajo pudiendo tener dos: se cambia una colocacion de
            // un grupo repetido por otra del grupo que falta.
            asegurarGrupos(fecha, enfoque, dia, bloqueados, piso, techo);

            // Un slot se da por cubierto al COLOCAR, pero el recorte puede quitar
            // esa colocacion. Se recalcula con lo que queda: si no, el slot
            // quedaba como cubierto sin estarlo y no se volvia a pedir (sesiones
            // de 20 minutos de INCREASE_STRENGTH sin UPPER_PUSH con fuerza).
            slotsPorCubrir.clear();
            slotsPorCubrir.addAll(pendientesAlEmpezar);
            dia.stream().filter(this::cubreSlotPendiente)
                    .forEach(col -> MandatorySlot.of(col.candidato()).ifPresent(slotsPorCubrir::remove));

            dia.forEach(col -> {
                enSemana.merge(col.capacidad(), 1, Integer::sum);
                usadosEnSemana.add(col.candidato().exerciseId());
            });
            var ordenada = ordenar(dia);
            sesiones.add(new PlanDraft.DraftWorkout(fecha, enfoque, nombre(enfoque), minutosSesion,
                    ordenada.stream().map(Colocacion::ejercicio).toList()));

            idsDiaAnterior = dia.stream().map(col -> col.candidato().exerciseId()).collect(Collectors.toSet());
            gruposDiaAnterior = dia.stream().map(col -> col.candidato().bodyPartCode())
                    .filter(RECUPERACION::contains).collect(Collectors.toSet());
            fechaAnterior = fecha;
        }

        var borrador = new PlanDraft(GenerationSource.RULE_ENGINE, null,
                "Semana %d".formatted(context.weekNumber()), null, null, sesiones);
        String justificacion = "Plan generado por el motor de reglas segun tu objetivo, nivel, equipamiento "
                + "y restricciones.";
        return cierre.aplicar(borrador, context, justificacion);
    }

    // --------------------------------------------------------------- reglas

    /**
     * V15 pide al menos dos grupos distintos del enfoque cuando el conjunto
     * elegible los tiene. Las sesiones de una division de 5-6 dias (PUSH, PULL,
     * LEGS) llevan 2-3 colocaciones resistidas, y la seleccion por patron de 4C
     * podia llenarlas con un solo grupo: un PULL de remo, remo y bisagra (solo
     * {@code back}) o un LEGS de sentadilla y bisagra (solo {@code upper legs}).
     * Era la causa de las 12 semanas sin respaldo de la evaluacion del
     * 28/10/2026 (EVALUACION_GENERACION_4_6_DIAS.md).
     * <p>
     * Solo actua cuando la sesion incumple ese minimo, asi que una semana que
     * ya lo cumplia sale identica. Cambia UNA colocacion de un grupo repetido
     * (antes las que no cubren un patron de fuerza unico de la sesion) por un
     * ejercicio del grupo que falta que:
     * <ul>
     *   <li>sea trabajo resistido prescribible para el objetivo y el nivel, con
     *       una dosis de sus capacidades (sin capacidad del objetivo no entra:
     *       no se pone un ejercicio solo para cubrir la cuota);</li>
     *   <li>pertenezca al enfoque y respete V22 (en PULL, biceps; en PUSH,
     *       triceps);</li>
     *   <li>no repita el dia anterior si los dias estan pegados (V18) ni algo
     *       que la sesion ya tiene;</li>
     *   <li>deje la sesion bajo el techo, con la mitad de V15 y sin bajar del
     *       piso de V20 (si baja, se suben series como en el paso 2).</li>
     * </ul>
     * Si ningun ejercicio cumple todo, no se fuerza nada: la sesion queda como
     * estaba y el validador dira por que.
     */
    private void asegurarGrupos(LocalDate fecha, WorkoutFocus enfoque, List<Colocacion> dia,
                                Set<Long> bloqueados, int piso, int techo) {
        if (enfoque == WorkoutFocus.FULL_BODY) return;   // ya lo resuelve el selector
        var trabajo = dia.stream().filter(col -> !complementos.contains(col.candidato().exerciseId())).toList();
        var cubiertos = trabajo.stream().map(col -> col.candidato().bodyPartCode())
                .filter(enfoque::accepts).collect(Collectors.toSet());
        if (trabajo.size() < 2 || cubiertos.size() >= 2) return;

        var enDia = dia.stream().map(col -> col.candidato().exerciseId()).collect(Collectors.toSet());
        for (var grupo : enfoque.bodyPartCodes()) {
            if (cubiertos.contains(grupo)) continue;
            var candidatos = resistidos.stream()
                    .filter(c -> grupo.equals(c.bodyPartCode()))
                    .filter(c -> ExerciseSelector.allowedIn(enfoque, c))
                    .filter(c -> !bloqueados.contains(c.exerciseId()) && !enDia.contains(c.exerciseId()))
                    .sorted(Comparator.comparing(CandidateExercise::exerciseId))
                    .toList();
            if (candidatos.isEmpty()) continue;
            // Lo no usado esta semana ni la anterior primero, como el resto del generador.
            var ordenados = new ArrayList<CandidateExercise>();
            for (var preferencia : List.<java.util.function.Predicate<CandidateExercise>>of(
                    c -> !usadosEnSemana.contains(c.exerciseId()) && !recientes.contains(c.exerciseId()),
                    c -> !usadosEnSemana.contains(c.exerciseId()),
                    c -> true)) {
                var opciones = new ArrayList<>(candidatos.stream().filter(preferencia)
                        .filter(c -> !ordenados.contains(c)).toList());
                Collections.shuffle(opciones, random);
                ordenados.addAll(opciones);
            }
            for (int k : quitablesPorGrupo(dia)) {
                var quitado = dia.get(k);
                for (var c : ordenados) {
                    var col = colocar(c, quitado.capacidad(), quitado.principal());
                    if (col == null) continue;
                    var con = new ArrayList<>(dia);
                    con.set(k, col);
                    if (minutos(fecha, enfoque, con) > techo || !respetaMitad(enfoque, con)) continue;
                    if (piso > 0 && minutos(fecha, enfoque, con) < piso) ampliarSeries(fecha, enfoque, con, piso, techo);
                    if (piso > 0 && minutos(fecha, enfoque, con) < piso) continue;
                    dia.clear();
                    dia.addAll(con);
                    return;
                }
            }
        }
    }

    /**
     * Las colocaciones de trabajo que se pueden cambiar sin dejar su grupo
     * vacio, de la ultima a la primera: primero las que no son fuerza que cubre
     * un patron de 4C, despues el resto.
     */
    private List<Integer> quitablesPorGrupo(List<Colocacion> dia) {
        var porGrupo = new HashMap<String, Integer>();
        dia.stream().filter(col -> !complementos.contains(col.candidato().exerciseId()))
                .forEach(col -> porGrupo.merge(col.candidato().bodyPartCode(), 1, Integer::sum));
        var primero = new ArrayList<Integer>();
        var despues = new ArrayList<Integer>();
        for (int k = dia.size() - 1; k >= 0; k--) {
            var col = dia.get(k);
            if (complementos.contains(col.candidato().exerciseId())) continue;
            if (porGrupo.getOrDefault(col.candidato().bodyPartCode(), 0) < 2) continue;
            boolean fuerzaDeSlot = col.capacidad() == Capability.STRENGTH && MandatorySlot.of(col.candidato()).isPresent();
            (fuerzaDeSlot ? despues : primero).add(k);
        }
        primero.addAll(despues);
        return primero;
    }

    /**
     * Si una colocacion da el slot por cubierto en la semana. Con la fuerza
     * como principal, solo una colocacion con dosis de FUERZA lo cubre: si no,
     * una hipertrofia o una resistencia ocuparian el patron que le falta.
     */
    private boolean cubreSlotPendiente(Colocacion col) {
        return !principalFactible(Capability.STRENGTH) || col.capacidad() == Capability.STRENGTH;
    }

    /** Cuantas colocaciones resistidas PRINCIPALES abre cada sesion. */
    private int resistidosPrincipales(int minutosSesion) {
        boolean corta = minutosSesion < 30;
        return switch (objetivo) {
            case "INCREASE_STRENGTH" -> principalFactible(Capability.STRENGTH) ? (corta ? 3 : 4) : 0;
            case "GENERAL_FITNESS" -> (principalFactible(Capability.STRENGTH) || principalFactible(Capability.MUSCULAR_ENDURANCE))
                    ? (corta ? 2 : 3) : 0;
            default -> 0;   // LOSE_WEIGHT: lo resistido es secundario
        };
    }

    /**
     * La capacidad PRINCIPAL de cada colocacion resistida de la sesion.
     * <ul>
     *   <li>INCREASE_STRENGTH: estructural a fuerza; el resto, hipertrofia.</li>
     *   <li>GENERAL_FITNESS: cada estructural que cubre un slot que la semana
     *       aun no tiene va a FUERZA, para que los cuatro patrones lleven dosis
     *       de fuerza. Una colocacion por sesion va a RESISTENCIA MUSCULAR: la
     *       que no cubre slot pendiente, o la ultima si todas lo cubren y la
     *       semana aun no tiene ninguna.</li>
     * </ul>
     */
    private List<Capability> capacidadesResistidasPrincipales(List<CandidateExercise> elegidos,
                                                              Set<MandatorySlot> pendientes) {
        var caps = new ArrayList<Capability>();
        if ("INCREASE_STRENGTH".equals(objetivo)) {
            elegidos.forEach(c -> caps.add(CapabilityPrescriptionPolicy.claseDe(c) == Clase.ESTRUCTURAL
                    ? Capability.STRENGTH : Capability.HYPERTROPHY));
            return caps;
        }
        boolean me = principalFactible(Capability.MUSCULAR_ENDURANCE);
        boolean fuerza = principalFactible(Capability.STRENGTH);
        var aun = new HashSet<>(pendientes);
        for (var c : elegidos) {
            var slot = MandatorySlot.of(c).orElse(null);
            boolean cubreSlot = fuerza && CapabilityPrescriptionPolicy.claseDe(c) == Clase.ESTRUCTURAL
                    && slot != null && aun.remove(slot);
            caps.add(cubreSlot ? Capability.STRENGTH : me ? Capability.MUSCULAR_ENDURANCE : Capability.STRENGTH);
        }
        boolean hayMe = caps.contains(Capability.MUSCULAR_ENDURANCE);
        if (me && !hayMe && caps.size() >= 2 && enSemana.getOrDefault(Capability.MUSCULAR_ENDURANCE, 0) == 0)
            caps.set(caps.size() - 1, Capability.MUSCULAR_ENDURANCE);
        if (fuerza && !caps.contains(Capability.STRENGTH) && enSemana.getOrDefault(Capability.STRENGTH, 0) == 0)
            for (int k = 0; k < elegidos.size(); k++)
                if (CapabilityPrescriptionPolicy.claseDe(elegidos.get(k)) == Clase.ESTRUCTURAL) {
                    caps.set(k, Capability.STRENGTH);
                    break;
                }
        return caps;
    }

    /** Lo siguiente para llegar al piso: secundarias; si no quedan, mas de la principal. */
    private Colocacion siguienteRelleno(ExerciseSelector selector, WorkoutFocus enfoque,
                                        List<CandidateExercise> resistidosDia, List<CandidateExercise> cardio,
                                        List<Colocacion> dia, Set<Long> bloqueados, Set<String> gruposBloqueados,
                                        Set<MandatorySlot> pendientes) {
        // Antes que cualquier secundaria: los slots que la semana aun no tiene
        // con dosis de FUERZA, cuando la fuerza es principal. Una hipertrofia
        // no ocupa el patron que le falta a la fuerza.
        if (principalFactible(Capability.STRENGTH) && !"LOSE_WEIGHT".equals(objetivo)) {
            var actuales = new ArrayList<>(resistidosDia);
            for (var slot : pendientes) {
                var extra = selector.pickAdditional(enfoque, actuales, true, ExerciseSelector.deSlot(slot))
                        .or(() -> selector.pickAdditional(enfoque, actuales, false, ExerciseSelector.deSlot(slot)));
                if (extra.isPresent()) {
                    var col = colocar(extra.get(), Capability.STRENGTH, true);
                    if (col != null) return col;
                }
            }
        }
        Capability secundaria = switch (objetivo) {
            case "LOSE_WEIGHT" -> Capability.STRENGTH;
            default -> Capability.HYPERTROPHY;
        };
        if (asignable(secundaria)) {
            var filtro = secundaria == Capability.STRENGTH ? ExerciseSelector.ESTRUCTURAL : ExerciseSelector.CUALQUIERA;
            var actuales = new ArrayList<>(resistidosDia);
            // Primero un slot que la semana aun no tiene: el trabajo estructural
            // rota por los cuatro patrones en vez de repetir dos.
            Optional<CandidateExercise> deSlot = Optional.empty();
            for (var slot : pendientes)
                if (deSlot.isEmpty()) deSlot = selector.pickAdditional(enfoque, actuales, true, ExerciseSelector.deSlot(slot));
            var extra = deSlot
                    .or(() -> selector.pickAdditional(enfoque, actuales, true, ExerciseSelector.ESTRUCTURAL))
                    .filter(filtro)
                    .or(() -> selector.pickAdditional(enfoque, actuales, true, filtro))
                    .or(() -> selector.pickAdditional(enfoque, actuales, false, filtro));
            if (extra.isPresent()) {
                var col = colocar(extra.get(), secundaria, false);
                if (col != null) return col;
            }
        }
        // Sin secundaria posible: mas de una principal resistida o de cardio.
        if (principalFactible(Capability.MUSCULAR_ENDURANCE)) {
            var actuales = new ArrayList<>(resistidosDia);
            var extra = selector.pickAdditional(enfoque, actuales, true)
                    .or(() -> selector.pickAdditional(enfoque, actuales, false));
            if (extra.isPresent()) {
                var col = colocar(extra.get(), Capability.MUSCULAR_ENDURANCE, true);
                if (col != null) return col;
            }
        }
        if (principalFactible(Capability.STRENGTH) && "INCREASE_STRENGTH".equals(objetivo)) {
            var actuales = new ArrayList<>(resistidosDia);
            var extra = selector.pickAdditional(enfoque, actuales, false, ExerciseSelector.ESTRUCTURAL);
            if (extra.isPresent()) {
                var col = colocar(extra.get(), Capability.STRENGTH, true);
                if (col != null) return col;
            }
        }
        if (principalFactible(Capability.CARDIO)) {
            var c = elegirComplemento(cardio, enfoque, dia, bloqueados, gruposBloqueados);
            if (c.isPresent()) return colocar(c.get(), Capability.CARDIO, true);
        }
        return null;
    }

    private Colocacion colocar(CandidateExercise c, Capability preferida, boolean principal) {
        var dosis = CapabilityPrescriptionPolicy.dosis(preferida, c, objetivo, nivel, limites)
                .filter(d -> CapabilityPrescriptionPolicy.admisibles(c, objetivo, nivel).contains(preferida));
        if (dosis.isEmpty()) {
            // El tipo del ejercicio no admite esa capacidad (p. ej. fuerza en un
            // isometrico): la primera admisible del objetivo que si se pueda.
            for (var alternativa : CapabilityPrescriptionPolicy.admisibles(c, objetivo, nivel)) {
                dosis = CapabilityPrescriptionPolicy.dosis(alternativa, c, objetivo, nivel, limites);
                if (dosis.isPresent()) break;
            }
        }
        if (dosis.isEmpty()) return null;
        var d = dosis.get();
        var papel = CapabilityPrescriptionPolicy.papel(objetivo, d.capacidad());
        boolean esPrincipal = papel.map(p -> p == GoalCapabilityPolicy.Role.PRIMARY).orElse(principal);
        return new Colocacion(c, d.prescribir(c.exerciseId(), cargaSugerida(c, d)), d.capacidad(), esPrincipal);
    }

    /** Principio 8: el ultimo peso anotado, nunca mas. Sin dato, null. */
    private java.math.BigDecimal cargaSugerida(CandidateExercise c, CapabilityPrescriptionPolicy.Dosis d) {
        if (d.tipo() != PrescriptionType.SETS_REPS) return null;
        boolean bajar = context.adjustment() != null && context.adjustment().clearsLoad();
        if (bajar || context.previousWeek() == null) return null;
        return context.previousWeek().lastLoadFor(c.exerciseId())
                .map(PreviousWeekSummary.LoadReference::loadKg).orElse(null);
    }

    private List<CandidateExercise> deClase(List<CandidateExercise> verificados, Clase clase, Capability cap) {
        return verificados.stream()
                .filter(c -> CapabilityPrescriptionPolicy.claseDe(c) == clase)
                .filter(c -> CapabilityPrescriptionPolicy.dosis(cap, c, objetivo, nivel, limites).isPresent())
                .sorted(Comparator.comparing(CandidateExercise::exerciseId))
                .toList();
    }

    /**
     * Un complemento (cardio, movilidad, equilibrio) para la sesion. No se
     * repite del dia anterior (V18), no esta ya en la sesion, y si su zona
     * necesita recuperacion solo entra si la sesion ya la entrena y el dia
     * anterior no la trabajo. Un ejercicio que cuenta para V15 (no es
     * complemento) solo entra si el enfoque admite su zona y no rompe la regla
     * de la mitad, ni la de V22 (biceps en PULL, triceps en PUSH). Prefiere lo
     * que no se uso esta semana ni la anterior.
     */
    private Optional<CandidateExercise> elegirComplemento(List<CandidateExercise> pool, WorkoutFocus enfoque,
                                                         List<Colocacion> dia, Set<Long> bloqueados,
                                                         Set<String> gruposBloqueados) {
        var enDia = dia.stream().map(col -> col.candidato().exerciseId()).collect(Collectors.toSet());
        var validos = pool.stream()
                .filter(c -> !bloqueados.contains(c.exerciseId()) && !enDia.contains(c.exerciseId()))
                .filter(c -> !RECUPERACION.contains(c.bodyPartCode())
                        || (enfoque.accepts(c.bodyPartCode()) && !gruposBloqueados.contains(c.bodyPartCode())))
                .filter(c -> complementos.contains(c.exerciseId())
                        || enfoque == WorkoutFocus.FULL_BODY || enfoque.accepts(c.bodyPartCode()))
                .filter(c -> complementos.contains(c.exerciseId()) || respetaMitad(enfoque, conCandidato(dia, c)))
                .toList();
        if (validos.isEmpty()) return Optional.empty();
        for (var preferencia : List.<java.util.function.Predicate<CandidateExercise>>of(
                c -> !usadosEnSemana.contains(c.exerciseId()) && !recientes.contains(c.exerciseId()),
                c -> !usadosEnSemana.contains(c.exerciseId()),
                c -> true)) {
            var opciones = validos.stream().filter(preferencia).toList();
            if (opciones.isEmpty()) continue;
            var elegido = opciones.get(random.nextInt(opciones.size()));
            if (ExerciseSelector.allowedIn(enfoque, elegido)) return Optional.of(elegido);
            // V22 para TODO lo que entra, complementos incluidos: V15 no los
            // cuenta, pero V22 si, y rechaza un estiramiento de triceps en PULL
            // (PULL solo admite biceps, PUSH solo triceps). Se vuelve a elegir
            // solo en ese caso, para que una sesion que ya era valida salga igual.
            var admitidas = opciones.stream().filter(c -> ExerciseSelector.allowedIn(enfoque, c)).toList();
            if (!admitidas.isEmpty()) return Optional.of(admitidas.get(random.nextInt(admitidas.size())));
        }
        return Optional.empty();
    }

    private static long grupos(List<CandidateExercise> lista) {
        return lista.stream().map(CandidateExercise::bodyPartCode).distinct().count();
    }

    private List<Colocacion> conCandidato(List<Colocacion> dia, CandidateExercise c) {
        var con = new ArrayList<>(dia);
        con.add(new Colocacion(c, null, null, false));
        return con;
    }

    /** V15: con 3+ grupos y 4+ ejercicios de trabajo, ninguno pasa de la mitad. */
    private boolean respetaMitad(WorkoutFocus enfoque, List<Colocacion> dia) {
        var trabajo = dia.stream().map(Colocacion::candidato)
                .filter(c -> !complementos.contains(c.exerciseId())).toList();
        if (enfoque.bodyPartCodes().size() < 3 || trabajo.size() < 4) return true;
        var porGrupo = trabajo.stream().collect(Collectors.groupingBy(CandidateExercise::bodyPartCode,
                Collectors.counting()));
        return porGrupo.values().stream().allMatch(n -> n <= trabajo.size() / 2);
    }

    /**
     * Quita colocaciones hasta caber en el techo. Orden: secundarias, despues
     * principales que ya estan representadas en el dia o en la semana; nunca la
     * unica colocacion de una principal. Entre las que se pueden quitar, primero
     * las de un grupo repetido en la sesion, para no dejarla con un solo grupo
     * (V15).
     */
    private void recortar(LocalDate fecha, WorkoutFocus enfoque, List<Colocacion> dia, int techo) {
        while (dia.size() > 2 && minutos(fecha, enfoque, dia) > techo) {
            int quitar = elegirParaQuitar(dia, k -> !dia.get(k).principal());
            if (quitar < 0) quitar = elegirParaQuitar(dia, k -> {
                var cap = dia.get(k).capacidad();
                long enElDia = dia.stream().filter(col -> col.capacidad() == cap).count();
                return enElDia > 1 || enSemana.getOrDefault(cap, 0) > 0;
            });
            if (quitar < 0) return;   // solo quedan principales unicas: V4 decidira
            dia.remove(quitar);
        }
    }

    private int elegirParaQuitar(List<Colocacion> dia, java.util.function.IntPredicate quitable) {
        int sinRepetir = -1;
        for (int k = dia.size() - 1; k >= 0; k--) {
            if (!quitable.test(k)) continue;
            var c = dia.get(k).candidato();
            boolean trabajo = !complementos.contains(c.exerciseId());
            long mismoGrupo = dia.stream().map(Colocacion::candidato)
                    .filter(o -> !complementos.contains(o.exerciseId()))
                    .filter(o -> Objects.equals(o.bodyPartCode(), c.bodyPartCode())).count();
            if (!trabajo || mismoGrupo > 1) return k;
            if (sinRepetir < 0) sinRepetir = k;
        }
        // Solo si quitarla no deja la sesion con un unico grupo de trabajo.
        if (sinRepetir < 0) return -1;
        final int candidata = sinRepetir;
        long gruposRestantes = java.util.stream.IntStream.range(0, dia.size()).filter(k -> k != candidata)
                .mapToObj(k -> dia.get(k).candidato()).filter(o -> !complementos.contains(o.exerciseId()))
                .map(CandidateExercise::bodyPartCode).distinct().count();
        return gruposRestantes >= 2 ? candidata : -1;
    }

    /** Sube series, de una en una y de la primera a la ultima, hasta el piso o el techo. */
    private void ampliarSeries(LocalDate fecha, WorkoutFocus enfoque, List<Colocacion> dia, int piso, int techo) {
        boolean cambio = true;
        while (cambio && minutos(fecha, enfoque, dia) < piso) {
            cambio = false;
            for (int k = 0; k < dia.size() && minutos(fecha, enfoque, dia) < piso; k++) {
                var col = dia.get(k);
                var e = col.ejercicio();
                var dosis = CapabilityPrescriptionPolicy.dosis(col.capacidad(), col.candidato(), objetivo, nivel, limites);
                if (dosis.isEmpty() || e.plannedSets() == null) continue;
                var mas = dosis.get().conSeries((short) (e.plannedSets() + 1));
                if (mas.isEmpty()) continue;
                var nueva = new Colocacion(col.candidato(), mas.get().prescribir(e.exerciseId(), e.targetLoadKg()),
                        col.capacidad(), col.principal());
                var con = new ArrayList<>(dia);
                con.set(k, nueva);
                if (minutos(fecha, enfoque, con) > techo) continue;
                dia.set(k, nueva);
                cambio = true;
            }
        }
    }

    /** Resistido (fuerza antes), equilibrio, cardio y al final movilidad. */
    private List<Colocacion> ordenar(List<Colocacion> dia) {
        return dia.stream().sorted(Comparator.comparingInt(col -> switch (col.capacidad()) {
            case STRENGTH, POWER -> 0;
            case HYPERTROPHY -> 1;
            case MUSCULAR_ENDURANCE -> 2;
            case BALANCE -> 3;
            case CARDIO -> 4;
            case MOBILITY -> 5;
        })).toList();
    }

    private int minutos(LocalDate fecha, WorkoutFocus enfoque, List<Colocacion> dia) {
        if (context.prescription() == null) return 0;
        return estimador.estimateMinutes(new PlanDraft.DraftWorkout(fecha, enfoque, nombre(enfoque), 0,
                dia.stream().map(Colocacion::ejercicio).toList()), context.prescription());
    }

    private static String nombre(WorkoutFocus focus) {
        return switch (focus) {
            case FULL_BODY -> "Cuerpo completo";
            case UPPER_BODY -> "Tren superior";
            case LOWER_BODY -> "Tren inferior";
            case PUSH -> "Empuje";
            case PULL -> "Traccion";
            case LEGS -> "Piernas";
            case CORE -> "Core";
        };
    }
}
