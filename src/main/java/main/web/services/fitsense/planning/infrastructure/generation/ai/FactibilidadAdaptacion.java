package main.web.services.fitsense.planning.infrastructure.generation.ai;

import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.MandatorySlot;
import main.web.services.fitsense.planning.domain.model.valueobjects.MvpDosePolicy;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.PreviousWeekSummary;
import main.web.services.fitsense.planning.domain.model.valueobjects.WeekDerivation;
import main.web.services.fitsense.planning.domain.services.CapabilityPrescriptionPolicy;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * PROMPT DE ADAPTACION V2 (EXPERIMENTAL, apagado por defecto) · FACTIBILIDAD PREVIA DE UNA SESION SENALADA.
 * <p>
 * Lo que el backend ya sabe de una sesion senalada y el modelo no puede calcular: cuanto puede
 * retirar antes de chocar con V20 y cuanto retira cada cambio de dosis permitido. Nace del
 * diagnostico de la etapa C (experimento_prompt_v2/01-03): 5 de 5 rechazos fueron V20 por
 * retirar de 1,3 a 27 veces el margen, y en 3 casos deshacer uno de sus propios cambios bastaba.
 * <p>
 * NO ES UNA REGLA NUEVA NI UN VALIDADOR. No decide nada: describe. Todo sale de codigo existente:
 * <ul>
 *   <li>duracion: {@link SessionDurationEstimator#estimateSeconds}, el mismo calculo de V17/V20,
 *       aplicado a una COPIA temporal de la sesion con un solo cambio (comprobado);</li>
 *   <li>minimo: {@link PlanDraftValidator#minimumSessionSeconds}, el mismo piso de V20 (comprobado);</li>
 *   <li>pisos de dosis: V9 ({@link PlanDraftValidator#MIN_SETS}, {@link PlanDraftValidator#MIN_REPS},
 *       {@link PlanDraftValidator#MIN_DURATION_SECONDS}) y el min_reps de V16
 *       ({@code RepLimits.minRepsFor}), los mismos que ya viajan en el prompt;</li>
 *   <li>GAIN_MUSCLE: 4D no admite reducciones ({@code ProgresionDosisPolicy.admite4D} con REDUCCION);</li>
 *   <li>que permite cada intervencion: {@link TipoIntervencion}, lo mismo que juzga V8C;</li>
 *   <li>sustituto con la misma dosis: la dosis 4D de su slot ({@link MvpDosePolicy#dosisPara}) o las
 *       dosis de sus capacidades admisibles ({@link CapabilityPrescriptionPolicy}), las de V24.</li>
 * </ul>
 * Lo que NO comprueba, declarado: la combinacion de varios cambios a la vez frente a V24/V25 y el
 * resto de V1-V25 sobre la semana entera. Eso lo sigue haciendo el validador final, obligatorio.
 */
public final class FactibilidadAdaptacion {

    private static final SessionDurationEstimator ESTIMADOR = new SessionDurationEstimator();

    /** Un cambio de dosis sobre UN ejercicio de la sesion, con lo que retira (calculado sobre una copia). */
    public record Opcion(Long exerciseId, String antes, String despues, long segundosRetirados, boolean cabe) {}

    /** Un sustituto admitido (misma lista que el prompt V1) y si admite la misma dosis del reemplazado. */
    public record Sustituto(Long reemplaza, Long exerciseId, boolean mismaDosis) {}

    /**
     * @param margen          segundos que se pueden retirar; null si el minimo no obliga en esta sesion.
     * @param dosisReducible  si la intervencion y la politica de dosis admiten bajar la dosis aqui.
     * @param pisoDeRepeticiones por ejercicio SETS_REPS, el minimo de repeticiones que admite.
     * @param cargasReducibles ejercicios con carga prescrita que la intervencion permite bajar.
     */
    public record Sesion(Long plannedWorkoutId, TipoIntervencion intervencion, long duracion, long minimo,
                         Long margen, boolean dosisReducible, String porQueNoSeReduce,
                         Map<Long, String> dosisActual, Map<Long, Integer> pisoDeRepeticiones,
                         List<Opcion> opciones, List<Sustituto> sustitutos, List<Long> cargasReducibles) {

        public boolean hayCambioFactible() {
            return opciones.stream().anyMatch(Opcion::cabe)
                    || sustitutos.stream().anyMatch(Sustituto::mismaDosis)
                    || !cargasReducibles.isEmpty();
        }
    }

    private FactibilidadAdaptacion() {
    }

    /** La factibilidad de cada sesion senalada que va a la IA, en el orden de la orden. */
    public static List<Sesion> calcular(PlanGenerationContext context, WeekDerivation derivation) {
        var adjustment = context.adjustment();
        if (adjustment == null) return List.of();
        var lista = new ArrayList<Sesion>();
        for (var s : adjustment.sesionesParaIA()) {
            var sesion = calcular(context, derivation, s);
            if (sesion != null) lista.add(sesion);
        }
        return lista;
    }

    static Sesion calcular(PlanGenerationContext context, WeekDerivation derivation,
                           PlanAdjustment.TargetedSession senalada) {
        var params = context.prescription();
        var propia = derivation.from(senalada.plannedWorkoutId()).map(d -> d.workout()).orElse(null);
        if (params == null || propia == null || propia.exercises() == null || propia.exercises().isEmpty())
            return null;

        var intervencion = senalada.intervencion();
        long duracion = ESTIMADOR.estimateSeconds(propia, params);
        long minimo = PlanDraftValidator.minimumSessionSeconds(context);
        Long margen = intervencion.permiteReducirDuracion() || minimo <= 0 ? null : duracion - minimo;

        String objetivo = context.profile() == null ? null : context.profile().goalType();
        String nivel = context.profile() == null ? null : context.profile().fitnessLevel();
        boolean gm = MvpDosePolicy.aplicaA(objetivo);
        boolean dosisReducible = intervencion.permiteCambiarDosis() && !gm;
        String porQue = !intervencion.permiteCambiarDosis() ? "la intervencion no permite cambiar la dosis"
                : gm ? "en GAIN_MUSCLE la dosis 4D no admite reducciones" : null;

        var porId = context.availableExercises() == null ? Map.<Long, CandidateExercise>of()
                : context.availableExercises().stream().collect(Collectors.toMap(CandidateExercise::exerciseId,
                Function.identity(), (x, y) -> x));
        var limites = params.repLimits();

        var dosisActual = new LinkedHashMap<Long, String>();
        var pisos = new LinkedHashMap<Long, Integer>();
        var opciones = new ArrayList<Opcion>();
        for (int i = 0; i < propia.exercises().size(); i++) {
            var e = propia.exercises().get(i);
            dosisActual.put(e.exerciseId(), dosis(e));
            if (!dosisReducible) continue;
            var c = porId.get(e.exerciseId());
            if (e.prescriptionType() == PrescriptionType.SETS_REPS && e.plannedSets() != null && e.plannedReps() != null) {
                int piso = PlanDraftValidator.MIN_REPS;
                if (limites != null && c != null) piso = Math.max(piso, limites.minRepsFor(c.bodyPartCode(), nivel));
                pisos.put(e.exerciseId(), piso);
                if (e.plannedReps() - 1 >= piso)
                    opciones.add(opcion(propia, i, conDosis(e, e.plannedSets(), (short) (e.plannedReps() - 1), null),
                            duracion, margen, params));
                if (e.plannedSets() - 1 >= PlanDraftValidator.MIN_SETS)
                    opciones.add(opcion(propia, i, conDosis(e, (short) (e.plannedSets() - 1), e.plannedReps(), null),
                            duracion, margen, params));
            } else if (e.prescriptionType() == PrescriptionType.DURATION && e.plannedDurationSeconds() != null) {
                if (e.plannedDurationSeconds() - 10 >= PlanDraftValidator.MIN_DURATION_SECONDS)
                    opciones.add(opcion(propia, i, conDosis(e, e.plannedSets(), null, e.plannedDurationSeconds() - 10),
                            duracion, margen, params));
                if (e.plannedSets() != null && e.plannedSets() - 1 >= PlanDraftValidator.MIN_SETS)
                    opciones.add(opcion(propia, i, conDosis(e, (short) (e.plannedSets() - 1), null,
                            e.plannedDurationSeconds()), duracion, margen, params));
            }
        }

        var sustitutos = new ArrayList<Sustituto>();
        if (intervencion.permiteSustituirEjercicio())
            sustitutosAdmitidos(context, derivation, senalada.plannedWorkoutId(), propia).forEach((reemplazado, ids) -> {
                var actual = propia.exercises().stream().filter(x -> reemplazado.equals(x.exerciseId())).findFirst()
                        .orElse(null);
                for (var id : ids)
                    sustitutos.add(new Sustituto(reemplazado, id,
                            actual != null && admiteMismaDosis(porId.get(id), actual, objetivo, nivel, limites)));
            });

        var cargas = new ArrayList<Long>();
        if (intervencion.permiteReducirCarga() && context.previousWeek() != null
                && context.previousWeek().prescriptions() != null)
            context.previousWeek().prescriptions().stream()
                    .filter(p -> senalada.plannedWorkoutId().equals(p.plannedWorkoutId()))
                    .filter(p -> p.loadKg() != null && p.loadKg().signum() > 0)
                    .map(PreviousWeekSummary.PrescriptionOutcome::exerciseId)
                    .filter(id -> propia.exercises().stream().anyMatch(x -> id.equals(x.exerciseId())))
                    .forEach(cargas::add);

        return new Sesion(senalada.plannedWorkoutId(), intervencion, duracion, minimo, margen, dosisReducible, porQue,
                dosisActual, pisos, opciones, sustitutos, cargas);
    }

    /**
     * Los sustitutos admitidos, con el MISMO filtro y orden que imprime el prompt V1 en
     * «SUSTITUTOS ADMITIDOS»: mismo patron, elegibles, sin los de la sesion ni los de los dias
     * contiguos ni los que dolieron, dificultad no mayor, ordenados por id, hasta 6. Una prueba
     * comprueba que las dos listas coinciden.
     */
    static Map<Long, List<Long>> sustitutosAdmitidos(PlanGenerationContext context, WeekDerivation derivation,
                                                     Long id, PlanDraft.DraftWorkout propia) {
        var resultado = new LinkedHashMap<Long, List<Long>>();
        if (context.availableExercises() == null) return resultado;
        var enSesion = propia.exercises().stream().map(PlanDraft.DraftExercise::exerciseId).collect(Collectors.toSet());
        Set<Long> contiguos = derivation.workouts().stream()
                .filter(d -> !id.equals(d.sourcePlannedWorkoutId()))
                .filter(d -> Math.abs(d.workout().scheduledDate().toEpochDay() - propia.scheduledDate().toEpochDay()) == 1)
                .flatMap(d -> d.workout().exercises().stream().map(PlanDraft.DraftExercise::exerciseId))
                .collect(Collectors.toSet());
        Set<Long> dolieron = context.previousWeek() == null || context.previousWeek().prescriptions() == null ? Set.of()
                : context.previousWeek().prescriptions().stream()
                .filter(p -> id.equals(p.plannedWorkoutId()) && "PAIN_OR_DISCOMFORT".equals(p.skipReason()))
                .map(PreviousWeekSummary.PrescriptionOutcome::exerciseId).collect(Collectors.toSet());
        var porId = context.availableExercises().stream().collect(Collectors.toMap(CandidateExercise::exerciseId,
                c -> c, (x, y) -> x));
        for (var e : propia.exercises()) {
            var actual = porId.get(e.exerciseId());
            if (actual == null || actual.movementPattern() == null) continue;
            resultado.put(e.exerciseId(), context.availableExercises().stream()
                    .filter(c -> actual.movementPattern().equals(c.movementPattern()))
                    .filter(c -> !enSesion.contains(c.exerciseId()) && !contiguos.contains(c.exerciseId())
                            && !dolieron.contains(c.exerciseId()))
                    .filter(c -> c.difficulty() <= actual.difficulty())
                    .map(CandidateExercise::exerciseId)
                    .sorted().limit(6).toList());
        }
        return resultado;
    }

    /** Si V24 admite al sustituto con exactamente la dosis (tipo, series, reps o segundos, descanso) del reemplazado. */
    static boolean admiteMismaDosis(CandidateExercise sustituto, PlanDraft.DraftExercise actual, String objetivo,
                                    String nivel, main.web.services.fitsense.configuration.domain.model.valueobjects
                                            .PrescriptionParams.RepLimits limites) {
        if (sustituto == null || actual == null || actual.prescriptionType() == null) return false;
        var tipo = sustituto.defaultPrescription() == null ? PrescriptionType.SETS_REPS : sustituto.defaultPrescription();
        if (tipo != actual.prescriptionType()) return false;
        if (MvpDosePolicy.aplicaA(objetivo)) {
            if (tipo != PrescriptionType.SETS_REPS) return false;
            return MvpDosePolicy.dosisPara(MandatorySlot.of(sustituto), sustituto.bodyPartCode())
                    .filter(d -> Objects.equals((int) d.series(), toInt(actual.plannedSets()))
                            && Objects.equals((int) d.reps(), toInt(actual.plannedReps()))
                            && Objects.equals((int) d.descansoSegundos(), toInt(actual.restSeconds())))
                    .isPresent();
        }
        return CapabilityPrescriptionPolicy.admisibles(sustituto, objetivo, nivel).stream()
                .map(cap -> CapabilityPrescriptionPolicy.dosis(cap, sustituto, objetivo, nivel, limites))
                .flatMap(java.util.Optional::stream)
                .anyMatch(d -> d.tipo() == actual.prescriptionType()
                        && Objects.equals((int) d.series(), toInt(actual.plannedSets()))
                        && Objects.equals(d.reps() == null ? null : (int) d.reps(), toInt(actual.plannedReps()))
                        && Objects.equals(d.segundos(), actual.plannedDurationSeconds())
                        && Objects.equals((int) d.descanso(), toInt(actual.restSeconds())));
    }

    private static Integer toInt(Short s) {
        return s == null ? null : (int) s;
    }

    private static Opcion opcion(PlanDraft.DraftWorkout propia, int indice, PlanDraft.DraftExercise cambiado,
                                 long duracion, Long margen,
                                 main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams params) {
        var copia = new ArrayList<>(propia.exercises());
        var original = copia.set(indice, cambiado);
        var temporal = new PlanDraft.DraftWorkout(propia.scheduledDate(), propia.focus(), propia.name(),
                propia.expectedDurationMinutes(), copia);
        long retira = duracion - ESTIMADOR.estimateSeconds(temporal, params);
        return new Opcion(original.exerciseId(), dosis(original), dosis(cambiado), retira,
                margen == null || retira <= margen);
    }

    private static PlanDraft.DraftExercise conDosis(PlanDraft.DraftExercise e, Short series, Short reps, Integer segundos) {
        return new PlanDraft.DraftExercise(e.exerciseId(), e.prescriptionType(), series,
                e.prescriptionType() == PrescriptionType.SETS_REPS ? reps : e.plannedReps(),
                e.prescriptionType() == PrescriptionType.DURATION ? segundos : e.plannedDurationSeconds(),
                e.targetLoadKg(), e.restSeconds(), e.notes());
    }

    /** «3x10» para repeticiones; «3 series de 90 s» para duracion (no «3x90», que se lee como repeticiones). */
    static String dosis(PlanDraft.DraftExercise e) {
        if (e.prescriptionType() == PrescriptionType.DURATION)
            return (e.plannedSets() == null ? "" : e.plannedSets() + (e.plannedSets() == 1 ? " serie de " : " series de "))
                    + e.plannedDurationSeconds() + " s";
        return e.plannedSets() + "x" + e.plannedReps();
    }

    // ------------------------------------------------------------------ texto

    /** El objetivo de cada intervencion, en palabras para el modelo. */
    static String objetivo(TipoIntervencion t) {
        return switch (t) {
            case REVISAR_CARGA_Y_RECUPERACION ->
                    "bajar algo la carga de trabajo de esta sesion por fatiga, sin dejarla por debajo del minimo";
            case VARIANTE_MAS_SENCILLA_O_CARGA ->
                    "que la sesion resulte mas facil de ejecutar: variantes mas sencillas del mismo patron y/o algo menos de dosis";
            case INVESTIGAR_CAMBIO_MINIMO -> "un cambio pequeno de dosis que facilite mantener la rutina";
            default -> t.instruccion();
        };
    }

    /**
     * V2.1: el mismo bloque A-F de V2 con indicaciones de pertinencia. Solo describe con datos del catalogo elegible
     * (dificultad registrada, musculo objetivo); no decide nada y el validador sigue siendo la autoridad.
     */
    static String textoV21(Sesion s, List<CandidateExercise> elegibles) {
        var porId = elegibles == null ? Map.<Long, CandidateExercise>of() : elegibles.stream()
                .collect(Collectors.toMap(CandidateExercise::exerciseId, Function.identity(), (x, y) -> x));
        String base = texto(s);
        var b = new StringBuilder();
        if (s.intervencion() == TipoIntervencion.INVESTIGAR_CAMBIO_MINIMO) {
            // El cambio minimo es UNO: la invitacion de V2 a combinar cambios se sustituye.
            base = base.replaceAll("(?m)^      Puedes combinar cambios.*\\n", "");
            if (s.hayCambioFactible())
                b.append("    G. CAMBIO MINIMO (V2.1): aplica UNA sola opcion [cabe] de D, sobre UN solo ejercicio.")
                        .append(" No cambies mas ejercicios.\n");
        } else if (s.intervencion() == TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA) {
            b.append("    G. MAS FACIL (V2.1): dificultad registrada y musculo objetivo de cada ejercicio, y sus")
                    .append(" sustitutos de MENOR dificultad del MISMO musculo que admiten la misma dosis:\n");
            boolean alguno = false;
            for (var id : s.dosisActual().keySet()) {
                var c = porId.get(id);
                if (c == null) continue;
                var faciles = s.sustitutos().stream().filter(Sustituto::mismaDosis)
                        .filter(x -> x.reemplaza().equals(id))
                        .map(x -> porId.get(x.exerciseId())).filter(Objects::nonNull)
                        .filter(x -> x.difficulty() < c.difficulty())
                        .filter(x -> Objects.equals(x.targetMuscle(), c.targetMuscle()))
                        .map(x -> x.exerciseId() + " (dif " + x.difficulty() + ")").toList();
                alguno |= !faciles.isEmpty();
                b.append("      - ").append(id).append(" (dif ").append(c.difficulty()).append(", ")
                        .append(c.targetMuscle() == null ? "musculo sin dato" : c.targetMuscle()).append("): ")
                        .append(faciles.isEmpty() ? "ninguno" : String.join(", ", faciles)).append('\n');
            }
            b.append(alguno
                    ? "      Usa uno o dos de estos (con la dosis del reemplazado) o baja la dosis en D. Cambia lo justo.\n"
                    : "      No hay ninguna variante de menor dificultad registrada del mismo musculo: para que resulte"
                    + " mas facil, BAJA LA DOSIS con las opciones [cabe] de D. Un sustituto de la misma dificultad"
                    + " no lo hace mas facil.\n");
        } else if (s.intervencion() == TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION && s.hayCambioFactible()) {
            b.append("    G. CANSANCIO (V2.1): baja trabajo con las opciones [cabe] de D (preferiblemente series o")
                    .append(" segundos) en uno a tres ejercicios. No sustituyas.\n");
        }
        return base + b;
    }

    /** El bloque A-F de una sesion, con los valores reales. */
    static String texto(Sesion s) {
        var b = new StringBuilder();
        b.append("  FACTIBILIDAD (V2; la calcula el backend con el mismo calculo de duracion que V20; el validador")
                .append(" final sigue decidiendo):\n");
        b.append("    A. OBJETIVO: ").append(objetivo(s.intervencion())).append(".\n");
        b.append("    B. ALCANCE: solo esta sesion (").append(s.plannedWorkoutId()).append("), en su fecha. ")
                .append(s.intervencion().permiteSustituirEjercicio()
                        ? "Puedes sustituir ejercicios, solo por los SUSTITUTOS ADMITIDOS."
                        : "No sustituyas ni retires ejercicios.").append('\n');
        b.append("    C. LIMITES: dura ").append(s.duracion()).append(" s");
        if (s.margen() == null) b.append("; en esta sesion el minimo de duracion no obliga.\n");
        else b.append("; minimo ").append(s.minimo()).append(" s; puedes retirar como MAXIMO ").append(s.margen())
                .append(" s en total (quedarse justo en el minimo vale).\n");

        b.append("    D. OPCIONES");
        if (!s.dosisReducible() && s.sustitutos().isEmpty() && s.cargasReducibles().isEmpty()) {
            b.append(": ninguna. ").append(s.porQueNoSeReduce() == null ? "" : "No se puede bajar la dosis: "
                    + s.porQueNoSeReduce() + ". ").append("No hay carga prescrita que bajar.\n");
        } else {
            b.append(" COMPROBADAS (cada linea es UN solo cambio sobre lo prescrito y lo que retira;")
                    .append(" [cabe] = por si solo no baja del minimo):\n");
            if (!s.dosisReducible() && s.porQueNoSeReduce() != null)
                b.append("      - dosis: no se puede bajar (").append(s.porQueNoSeReduce()).append(").\n");
            var porEjercicio = new LinkedHashMap<Long, List<Opcion>>();
            s.opciones().forEach(o -> porEjercicio.computeIfAbsent(o.exerciseId(), k -> new ArrayList<>()).add(o));
            s.dosisActual().forEach((id, actual) -> {
                if (!s.dosisReducible()) return;
                var ops = porEjercicio.getOrDefault(id, List.of());
                b.append("      - ").append(id).append(" (").append(actual).append("): ");
                if (ops.isEmpty()) b.append("ya esta en su minimo de dosis; no se puede bajar");
                else b.append(ops.stream().map(o -> o.despues() + " retira " + o.segundosRetirados() + " s "
                        + (o.cabe() ? "[cabe]" : "[NO cabe]")).collect(Collectors.joining("; ")));
                var piso = s.pisoDeRepeticiones().get(id);
                if (piso != null) b.append(" (repeticiones minimas ").append(piso).append(')');
                b.append('\n');
            });
            if (s.dosisReducible() && s.margen() != null)
                b.append("      Puedes combinar cambios (cada repeticion menos, o cada 10 s menos, retira lo mismo que")
                        .append(" la primera) mientras la SUMA de lo retirado no pase de ").append(s.margen())
                        .append(" s.\n");
            if (!s.sustitutos().isEmpty()) {
                var mismas = new LinkedHashMap<Long, List<Long>>();
                s.sustitutos().stream().filter(Sustituto::mismaDosis)
                        .forEach(x -> mismas.computeIfAbsent(x.reemplaza(), k -> new ArrayList<>()).add(x.exerciseId()));
                if (mismas.isEmpty())
                    b.append("      - sustitutos: ninguno de la lista admite la misma dosis; un sustituto con otra dosis")
                            .append(" cambia la duracion y debe caber en C.\n");
                else {
                    b.append("      - sustitutos que admiten la MISMA dosis que el ejercicio que reemplazan (la duracion")
                            .append(" no cambia; ponles esa misma dosis): ");
                    b.append(mismas.entrySet().stream().map(en -> en.getKey() + " -> " + en.getValue())
                            .collect(Collectors.joining("; "))).append(".\n");
                }
            }
            if (!s.cargasReducibles().isEmpty())
                b.append("      - carga: puedes bajar target_load_kg (no cambia la duracion) en ")
                        .append(s.cargasReducibles()).append(".\n");
        }

        b.append("    E. PROHIBIDO: subir series, repeticiones o segundos por encima de lo prescrito; cambiar")
                .append(" descansos; anadir ejercicios");
        if (!s.intervencion().permiteSustituirEjercicio()) b.append("; sustituir o retirar ejercicios");
        b.append(". Volver hacia la dosis prescrita, sin pasarla, NO es subir.\n");

        b.append("    F. SI NO HAY SOLUCION: ");
        if (s.hayCambioFactible())
            b.append("hay opciones validas en D. Omite esta sesion solo si ninguna sirve al objetivo.\n");
        else
            b.append("el backend no encuentra ningun cambio permitido y efectivo en esta sesion. OMITELA del")
                    .append(" arreglo: es la respuesta correcta.\n");
        return b.toString();
    }

}
