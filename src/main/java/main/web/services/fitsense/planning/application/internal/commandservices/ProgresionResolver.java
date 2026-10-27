package main.web.services.fitsense.planning.application.internal.commandservices;

import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalConfigurationService;
import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalExecutionService;
import main.web.services.fitsense.planning.domain.model.aggregates.PlanAdaptationAttempt;
import main.web.services.fitsense.planning.domain.model.entities.PlannedWorkout;
import main.web.services.fitsense.planning.domain.model.entities.PlannedWorkoutExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.CapabilityPrescriptionPolicy;
import main.web.services.fitsense.planning.domain.services.ProgresionDosisPolicy;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.PlanAdaptationAttemptRepository;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.PlannedWorkoutRepository;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * V35 · LOS PASOS DE PROGRESION CANDIDATOS DE ESTE CICLO
 * (DISENO_PROGRESION_Y_REINCORPORACION v0.2.1, §3, §5, §12.3 y §13.1).
 * <p>
 * Solo se llama sin orden de adaptar y con decision {@code NO_CHANGE}. Propone,
 * no publica: devuelve los pasos en orden de prioridad y quien llama los
 * revalida (V8C, V1-V25) de uno en uno y publica a lo sumo UNO. Si ninguno pasa,
 * se publica la copia de siempre.
 * <p>
 * Para cada sesion S de la semana medida, con origen O (V31):
 * <ol>
 *   <li><b>Vuelta atras</b> (D-P6): si O→S fue un paso y en S consta fatiga, el
 *       ejercicio del paso vuelve a su dosis anterior. Va primero.</li>
 *   <li><b>Criterio de dos semanas</b> (D-P1, D-P2): S y O completadas, la
 *       adherencia semanal observada de las dos semanas por encima del umbral
 *       bueno, sin fatiga ni dolor en S ni en O, sin dolor en otro uso de sus
 *       ejercicios en la semana medida, la decision que publico S fue
 *       NO_CHANGE y O→S es una copia (no un paso: las dos semanas cuentan desde
 *       el ultimo). Todos sus ejercicios siguen elegibles y sin exclusion.</li>
 *   <li><b>Recuperacion</b>: si una reduccion de dosis de la IA (fatiga o
 *       motivacion) no se recupero, un ejercicio -el primero por debajo de su
 *       referencia- hacia ella, con el paso de la sesion &lt;= recovery_step_pct.</li>
 *   <li><b>Progresion ordinaria</b>: un ejercicio de repeticiones, primero +1
 *       repeticion y +1 serie solo con las repeticiones en su maximo (D-P3), el
 *       de menor cambio relativo en el ejercicio; paso de la sesion &lt;=
 *       progression_increase_pct; volumen &lt;= 1,10 x V_base (D-P5), o
 *       1,10 x V_base - V_ret con la reserva R2 PENDIENTE (D-P12).</li>
 *   <li><b>Reincorporacion</b> (§5): el relleno retirado por ACORTAR, con su
 *       dosis inicial exacta, al final de la sesion, cuando los conservados ya
 *       no pueden progresar dentro de la reserva. Si no se puede publicar, la
 *       reserva queda EN ESPERA DE TIEMPO y se propone despues la progresion
 *       con el techo entero.</li>
 * </ol>
 * Ante un linaje incompleto o ambiguo, ningun paso para esa sesion.
 * <p>
 * LIMITACION DECLARADA: la disponibilidad M por entrenamiento no se modela
 * aqui; el tiempo lo limitan V4 y el presupuesto de 4D en la revalidacion.
 */
@Component
public class ProgresionResolver {

    private static final Logger log = LoggerFactory.getLogger(ProgresionResolver.class);

    /** D-P5 (aprobado provisionalmente): techo = 1,10 x V_base. */
    static final double TECHO = 1.10;

    private static final String COMPLETADA = "COMPLETED";
    private static final String DOLOR = "PAIN_OR_DISCOMFORT";
    private static final String FATIGA = "FATIGUE";
    private static final Set<TipoIntervencion> REDUCEN_DOSIS = Set.of(
            TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION, TipoIntervencion.INVESTIGAR_CAMBIO_MINIMO);

    public static final String PENDIENTE = "PENDIENTE";
    public static final String EN_ESPERA_DE_TIEMPO = "EN_ESPERA_DE_TIEMPO";
    public static final String DESCARTADA = "DESCARTADA";

    private final PlannedWorkoutRepository plannedWorkoutRepository;
    private final PlanAdaptationAttemptRepository attemptRepository;
    private final ExternalExecutionService externalExecutionService;
    private final ExternalConfigurationService configuracion;
    private final SessionDurationEstimator estimador;
    private final JsonSupport jsonSupport;

    public ProgresionResolver(PlannedWorkoutRepository plannedWorkoutRepository,
                              PlanAdaptationAttemptRepository attemptRepository,
                              ExternalExecutionService externalExecutionService,
                              ExternalConfigurationService configuracion,
                              SessionDurationEstimator estimador,
                              JsonSupport jsonSupport) {
        this.plannedWorkoutRepository = plannedWorkoutRepository;
        this.attemptRepository = attemptRepository;
        this.externalExecutionService = externalExecutionService;
        this.configuracion = configuracion;
        this.estimador = estimador;
        this.jsonSupport = jsonSupport;
    }

    /** Los parametros vigentes (V7/V15), leidos en cada ciclo. */
    record Parametros(double umbralBueno, double pasoOrdinarioPct, double pasoRecuperacionPct, int divisor) {}

    /**
     * @param derivation la copia de la semana medida con las fechas nuevas
     * @param observada  la adherencia semanal observada de la semana medida
     * @return los pasos candidatos, en orden: vueltas atras, recuperaciones,
     *         progresiones y reincorporaciones (con su respaldo EN ESPERA); y
     *         dentro de cada clase, primero la sesion con menos pasos ya dados
     *         y despues por fecha.
     */
    @Transactional(readOnly = true)
    public List<PasoProgresion> candidatos(PlanGenerationContext context, WeekDerivation derivation,
                                           BigDecimal observada, Map<Long, CandidateExercise> elegibles,
                                           ExclusionesDeSeguridad exclusiones) {
        var previa = context.previousWeek();
        if (previa == null || previa.workouts() == null || derivation == null) return List.of();
        var ajuste = configuracion.adjustment();
        var p = new Parametros(
                ajuste == null || ajuste.goodThresholdPct() == null ? 80 : ajuste.goodThresholdPct(),
                ajuste == null || ajuste.progressionIncreasePct() == null ? 5 : ajuste.progressionIncreasePct(),
                ajuste == null || ajuste.recoveryStepPct() == null ? 10 : ajuste.recoveryStepPct(),
                configuracion.durationToRepsDivisor());

        record Candidato(PasoProgresion paso, int pasosPrevios, java.time.LocalDate fecha) {}
        var lista = new ArrayList<Candidato>();
        for (var derivada : derivation.workouts()) {
            var medida = previa.workouts().stream()
                    .filter(w -> derivada.sourcePlannedWorkoutId().equals(w.plannedWorkoutId())).findFirst();
            if (medida.isEmpty()) continue;
            try {
                var pasos = new int[1];
                for (var paso : deLaSesion(context, derivada, medida.get(), observada, elegibles,
                        exclusiones == null ? ExclusionesDeSeguridad.ninguna() : exclusiones, p, pasos))
                    lista.add(new Candidato(paso, pasos[0], derivada.workout().scheduledDate()));
            } catch (RuntimeException e) {
                // Un fallo leyendo el linaje no es motivo para no publicar: esa sesion no progresa.
                log.warn("PROGRESION usuario {} · sesion {} sin candidato: {}", context.userId(),
                        derivada.sourcePlannedWorkoutId(), e.toString());
            }
        }
        // Prioridad; despues la sesion con MENOS pasos en su linaje (para que una
        // sesion temprana no acapare todos los ciclos) y, a igualdad, la fecha.
        var todos = lista.stream()
                .sorted(Comparator.comparingInt((Candidato c) -> prioridad(c.paso()))
                        .thenComparingInt(Candidato::pasosPrevios)
                        .thenComparing(Candidato::fecha, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(Candidato::paso).toList();
        if (!todos.isEmpty())
            log.info("PROGRESION_CANDIDATOS usuario {} · {}", context.userId(), todos.stream()
                    .map(x -> "%s %d/%d".formatted(x.tipo(), x.origen(), x.exerciseId())).toList());
        return todos;
    }

    private static int prioridad(PasoProgresion paso) {
        return switch (paso.tipo()) {
            case VUELTA_ATRAS -> 0;
            case RECUPERACION -> 1;
            case PROGRESION -> EN_ESPERA_DE_TIEMPO.equals(paso.estadoReserva()) ? 4 : 2;
            case REINCORPORACION -> 3;
        };
    }

    // =======================================================================
    // Una sesion
    // =======================================================================

    /** Un paso del linaje: {@code derivada} se publico desde {@code origen} con {@code intento}. */
    private record Tramo(PlannedWorkout derivada, PlannedWorkout origen, PlanAdaptationAttempt intento,
                         EvidenciaV32.Clase clase, EvidenciaV32.Informe informe) {}

    private List<PasoProgresion> deLaSesion(PlanGenerationContext context, DerivedWorkout derivada,
                                            PreviousWeekSummary.WorkoutOutcome medida, BigDecimal observada,
                                            Map<Long, CandidateExercise> elegibles, ExclusionesDeSeguridad exclusiones,
                                            Parametros p, int[] pasosPrevios) {
        var s = plannedWorkoutRepository.findById(medida.plannedWorkoutId()).orElse(null);
        if (s == null) return List.of();
        var linaje = linaje(s);
        if (linaje == null || linaje.isEmpty()) return List.of();   // generacion inicial o linaje roto
        pasosPrevios[0] = (int) linaje.stream().filter(t -> t.clase() == EvidenciaV32.Clase.PROGRESADA).count();
        var ultimo = linaje.get(0);   // O -> S
        var nueva = derivada.workout();
        var previa = context.previousWeek();

        // 1 · Vuelta atras: el paso anterior y fatiga despues.
        if (ultimo.clase() == EvidenciaV32.Clase.PROGRESADA && declara(previa, s.getId(), FATIGA)
                && !declara(previa, s.getId(), DOLOR)) {
            var paso = EvidenciaV32.paso(ultimo.informe(), ultimo.origen().getId()).orElse(null);
            if (paso == null || "VUELTA_ATRAS".equals(paso.tipo())) return List.of();
            return vueltaAtras(s, ultimo.origen(), paso, nueva, context, p).map(List::of).orElse(List.of());
        }

        // 2 · Dos semanas consecutivas de cumplimiento, contadas desde el ultimo paso.
        if (!cumple(s, medida, ultimo, observada, previa, elegibles, exclusiones, p)) return List.of();

        var base = volumen(ejercicios(linaje.get(linaje.size() - 1).origen()), p.divisor());
        int techo = (int) Math.floor(base * TECHO);
        var actuales = nueva.exercises();
        int v = volumen(actuales, p.divisor());

        // 3 · Recuperacion pendiente.
        var referencia = referenciaDeReduccion(linaje);
        if (referencia != null) {
            var rec = recuperacion(s, nueva, referencia, context, elegibles, p, base, techo);
            if (rec.isPresent()) return List.of(rec.get());
        }

        // 4/5 · Progresion ordinaria, con la reserva R2 si se retiro un relleno.
        var retirado = retirado(linaje, s);
        if (retirado == null)
            return progresiones(s, nueva, context, elegibles, p, base, techo, techo, null);

        var cand = elegibles.get(retirado.getExerciseId());
        boolean descartada = cand == null || exclusiones.incluye(retirado.getExerciseId())
                || dolio(previa, retirado.getExerciseId());
        var exacta = descartada ? null : exacta(retirado, cand, context);
        if (descartada || exacta == null)
            return progresiones(s, nueva, context, elegibles, p, base, techo, techo, DESCARTADA);

        int vRet = exacta.volume(p.divisor());
        var conReserva = progresiones(s, nueva, context, elegibles, p, base, techo, techo - vRet, PENDIENTE);
        if (!conReserva.isEmpty()) return conReserva;

        var lista = new ArrayList<PasoProgresion>();
        if (v + vRet <= techo) {
            var con = new ArrayList<>(actuales);
            con.add(exacta);
            lista.add(paso(s, PasoProgresion.Tipo.REINCORPORACION, exacta.exerciseId(), con.size() - 1, null,
                    exacta, v, v + vRet, base, techo, PENDIENTE, nueva, con, context));
        }
        // Si la reincorporacion no cabe (V4, colocaciones, presupuesto) la reserva se libera.
        lista.addAll(progresiones(s, nueva, context, elegibles, p, base, techo, techo, EN_ESPERA_DE_TIEMPO));
        return lista;
    }

    /** El linaje de S hacia atras, de lo mas reciente a la raiz; null si se rompe. */
    private List<Tramo> linaje(PlannedWorkout s) {
        var tramos = new ArrayList<Tramo>();
        var vistas = new HashSet<Long>();
        var actual = s;
        while (vistas.add(actual.getId())) {
            Long origenId = actual.getSourcePlannedWorkoutId();
            if (origenId == null) return tramos;
            var intentos = attemptRepository.findByResultingPlanId(actual.getPlan().getId());
            if (intentos.size() != 1) return null;
            var intento = intentos.get(0);
            var clase = EvidenciaV32.clase(intento, origenId);
            var informe = EvidenciaV32.informe(intento, jsonSupport);
            var origen = plannedWorkoutRepository.findById(origenId).orElse(null);
            if (clase.isEmpty() || informe.isEmpty() || origen == null) return null;
            tramos.add(new Tramo(actual, origen, intento, clase.get(), informe.get()));
            actual = origen;
        }
        return null;
    }

    private boolean cumple(PlannedWorkout s, PreviousWeekSummary.WorkoutOutcome medida, Tramo ultimo,
                           BigDecimal observada, PreviousWeekSummary previa, Map<Long, CandidateExercise> elegibles,
                           ExclusionesDeSeguridad exclusiones, Parametros p) {
        // O -> S es una copia, y la semana que publico S no ordeno nada.
        if (ultimo.clase() != EvidenciaV32.Clase.CONSERVADA
                || EvidenciaV32.sustituida(ultimo.informe(), ultimo.origen().getId())) return false;
        if (ultimo.intento().getDecision() != AdaptationDecision.NO_CHANGE) return false;
        // Adherencia semanal observada de las dos semanas.
        if (!supera(observada, p.umbralBueno()) || !supera(ultimo.intento().getObservedAdherencePct(), p.umbralBueno()))
            return false;
        // S completada sin fatiga ni dolor, ni dolor en otro uso de sus ejercicios.
        if (!medida.recorded() || !COMPLETADA.equals(medida.sessionStatus())) return false;
        if (declara(previa, s.getId(), FATIGA) || declara(previa, s.getId(), DOLOR)) return false;
        for (var e : s.exercisesView()) {
            if (dolio(previa, e.getExerciseId())) return false;
            if (!elegibles.containsKey(e.getExerciseId()) || exclusiones.incluye(e.getExerciseId())) return false;
        }
        // O completada sin fatiga ni dolor.
        var o = externalExecutionService.fetchResultsByWorkout(List.of(ultimo.origen().getId()))
                .get(ultimo.origen().getId());
        if (o == null || !COMPLETADA.equals(o.sessionStatus())) return false;
        if (o.exercises() != null && o.exercises().stream()
                .anyMatch(x -> FATIGA.equals(x.skipReason()) || DOLOR.equals(x.skipReason()))) return false;
        return true;
    }

    private static boolean supera(BigDecimal pct, double umbral) {
        return pct != null && pct.doubleValue() >= umbral;
    }

    /** Si en la semana medida consta ese motivo en la sesion o en alguno de sus ejercicios. */
    private static boolean declara(PreviousWeekSummary previa, Long sesion, String motivo) {
        if (previa.workouts().stream().anyMatch(w -> sesion.equals(w.plannedWorkoutId())
                && motivo.equals(w.skipReason()))) return true;
        return previa.prescriptions().stream().anyMatch(x -> sesion.equals(x.plannedWorkoutId())
                && motivo.equals(x.skipReason()));
    }

    private static boolean dolio(PreviousWeekSummary previa, Long exerciseId) {
        return previa.prescriptions().stream()
                .anyMatch(x -> exerciseId.equals(x.exerciseId()) && DOLOR.equals(x.skipReason()));
    }

    // =======================================================================
    // Referencias
    // =======================================================================

    /** La sesion de antes de la reduccion de dosis mas reciente de la IA, si la hubo. */
    private static PlannedWorkout referenciaDeReduccion(List<Tramo> linaje) {
        for (var t : linaje) {
            if (t.clase() != EvidenciaV32.Clase.APLICADA
                    || t.intento().getStatus() != AdaptationAttempt.Status.APLICADA) continue;
            if (!REDUCEN_DOSIS.contains(EvidenciaV32.intervencion(t.intento(), t.origen().getId()))) continue;
            boolean bajo = t.derivada().exercisesView().stream().anyMatch(d -> t.origen().exercisesView().stream()
                    .anyMatch(o -> Objects.equals(o.getExerciseId(), d.getExerciseId()) && menor(d, o)));
            if (bajo) return t.origen();
        }
        return null;
    }

    /**
     * El relleno retirado por ACORTAR_O_REORGANIZAR en el linaje y que no ha
     * vuelto: uno solo, sin nada anadido ni sustitucion en ese paso. Nunca uno
     * retirado por dolor, sustituido o cambiado por una variante.
     */
    private static PlannedWorkoutExercise retirado(List<Tramo> linaje, PlannedWorkout s) {
        for (var t : linaje) {
            if (t.clase() != EvidenciaV32.Clase.APLICADA
                    || t.intento().getStatus() != AdaptationAttempt.Status.APLICADA
                    || EvidenciaV32.intervencion(t.intento(), t.origen().getId())
                    != TipoIntervencion.ACORTAR_O_REORGANIZAR
                    || EvidenciaV32.sustituida(t.informe(), t.origen().getId())) continue;
            var antes = ids(t.origen());
            var despues = ids(t.derivada());
            var quitados = antes.stream().filter(id -> !despues.contains(id)).toList();
            var anadidos = despues.stream().filter(id -> !antes.contains(id)).toList();
            if (quitados.size() != 1 || !anadidos.isEmpty()) continue;
            if (ids(s).contains(quitados.get(0))) return null;   // ya volvio
            return t.origen().exercisesView().stream()
                    .filter(e -> quitados.get(0).equals(e.getExerciseId())).findFirst().orElse(null);
        }
        return null;
    }

    private static Set<Long> ids(PlannedWorkout w) {
        var ids = new LinkedHashSet<Long>();
        w.exercisesView().forEach(e -> ids.add(e.getExerciseId()));
        return ids;
    }

    // =======================================================================
    // Pasos
    // =======================================================================

    private Optional<PasoProgresion> vueltaAtras(PlannedWorkout s, PlannedWorkout o, EvidenciaV32.Informe.Paso paso,
                                                 PlanDraft.DraftWorkout nueva, PlanGenerationContext context,
                                                 Parametros p) {
        var lista = new ArrayList<>(nueva.exercises());
        int i = posicion(lista, paso.exerciseId());
        if (i < 0) return Optional.empty();
        var e = lista.get(i);
        int v = volumen(lista, p.divisor());
        if ("REINCORPORACION".equals(paso.tipo())) {
            lista.remove(i);
        } else {
            var antes = paso.antes();
            if (antes == null) return Optional.empty();
            lista.set(i, new PlanDraft.DraftExercise(e.exerciseId(), e.prescriptionType(), antes.series(), antes.reps(),
                    antes.segundos(), e.targetLoadKg(), antes.descanso(), e.notes()));
        }
        var contenido = new PlanDraft.DraftWorkout(nueva.scheduledDate(), nueva.focus(), nueva.name(),
                Math.min(nueva.expectedDurationMinutes(), o.getExpectedDurationMinutes()), lista);
        int despues = volumen(lista, p.divisor());
        return Optional.of(new PasoProgresion(s.getId(), PasoProgresion.Tipo.VUELTA_ATRAS,
                e.exerciseId(), i, PasoProgresion.Dosis.de(e),
                "REINCORPORACION".equals(paso.tipo()) ? null : PasoProgresion.Dosis.de(lista.get(i)),
                v, despues, paso.volumenBase() == null ? 0 : paso.volumenBase(),
                paso.techo() == null ? 0 : paso.techo(), null, contenido));
    }

    private Optional<PasoProgresion> recuperacion(PlannedWorkout s, PlanDraft.DraftWorkout nueva,
                                                  PlannedWorkout referencia, PlanGenerationContext context,
                                                  Map<Long, CandidateExercise> elegibles, Parametros p,
                                                  int base, int techo) {
        var actuales = nueva.exercises();
        int v = volumen(actuales, p.divisor());
        for (int i = 0; i < actuales.size(); i++) {
            var e = actuales.get(i);
            var ref = referencia.exercisesView().stream()
                    .filter(r -> Objects.equals(r.getExerciseId(), e.exerciseId())).findFirst().orElse(null);
            if (ref == null || !Objects.equals(ref.getRestSeconds(), e.restSeconds())
                    || ref.getPrescriptionType() != e.prescriptionType()) continue;
            var hacia = haciaReferencia(e, ref, v, p);
            if (hacia == null) continue;
            var lista = new ArrayList<>(actuales);
            lista.set(i, hacia);
            int nv = volumen(lista, p.divisor());
            return Optional.of(paso(s, PasoProgresion.Tipo.RECUPERACION, e.exerciseId(), i, e, hacia, v, nv,
                    base, techo, null, nueva, lista, context));
        }
        return Optional.empty();
    }

    /** Una dimension, hacia la referencia sin superarla, con el paso de la sesion <= recovery_step_pct. */
    private static PlanDraft.DraftExercise haciaReferencia(PlanDraft.DraftExercise e, PlannedWorkoutExercise ref,
                                                          int v, Parametros p) {
        short s = e.plannedSets() == null ? 1 : e.plannedSets();
        short rs = ref.getPlannedSets() == null ? 1 : ref.getPlannedSets();
        PlanDraft.DraftExercise mejor = null;
        if (e.plannedReps() != null && ref.getPlannedReps() != null && e.plannedReps() < ref.getPlannedReps()) {
            for (int r = ref.getPlannedReps(); r > e.plannedReps() && mejor == null; r--) {
                var n = con(e, s, (short) r, e.plannedDurationSeconds());
                if (dentroDelPaso(n, e, v, p.pasoRecuperacionPct(), p.divisor())) mejor = n;
            }
            return mejor;
        }
        if (e.plannedDurationSeconds() != null && ref.getPlannedDurationSeconds() != null
                && e.plannedDurationSeconds() < ref.getPlannedDurationSeconds()) {
            for (int seg = ref.getPlannedDurationSeconds(); seg > e.plannedDurationSeconds() && mejor == null; seg--) {
                var n = con(e, s, e.plannedReps(), seg);
                if (dentroDelPaso(n, e, v, p.pasoRecuperacionPct(), p.divisor())) mejor = n;
            }
            return mejor;
        }
        if (s < rs) {
            var n = con(e, (short) (s + 1), e.plannedReps(), e.plannedDurationSeconds());
            return dentroDelPaso(n, e, v, p.pasoRecuperacionPct(), p.divisor()) ? n : null;
        }
        return null;
    }

    private static boolean dentroDelPaso(PlanDraft.DraftExercise nuevo, PlanDraft.DraftExercise antes, int v,
                                         double pct, int divisor) {
        return v > 0 && 100.0 * (nuevo.volume(divisor) - antes.volume(divisor)) / v <= pct;
    }

    /** Progresion ordinaria: las opciones de un ejercicio, de menor a mayor cambio relativo. */
    private List<PasoProgresion> progresiones(PlannedWorkout s, PlanDraft.DraftWorkout nueva,
                                              PlanGenerationContext context, Map<Long, CandidateExercise> elegibles,
                                              Parametros p, int base, int techo, int tope, String estadoReserva) {
        var actuales = nueva.exercises();
        int v = volumen(actuales, p.divisor());
        record Opcion(int i, PlanDraft.DraftExercise nuevo, double pctEjercicio, double pctSesion) {}
        var opciones = new ArrayList<Opcion>();
        for (int i = 0; i < actuales.size(); i++) {
            var e = actuales.get(i);
            if (e.prescriptionType() != PrescriptionType.SETS_REPS || e.plannedReps() == null) continue;
            var c = elegibles.get(e.exerciseId());
            var rango = rango(e, c, context);
            if (rango == null) continue;
            short s0 = e.plannedSets() == null ? 1 : e.plannedSets();
            PlanDraft.DraftExercise n;
            double pctEj;
            if (e.plannedReps() < rango.repsMax()) {
                n = con(e, s0, (short) (e.plannedReps() + 1), null);
                pctEj = 100.0 / e.plannedReps();
            } else if (s0 < rango.seriesMax()) {
                n = con(e, (short) (s0 + 1), e.plannedReps(), null);
                pctEj = 100.0 / s0;
            } else continue;
            if (!rango.admite(n)) continue;
            opciones.add(new Opcion(i, n, pctEj, v == 0 ? 100 : 100.0 * (n.volume(p.divisor()) - e.volume(p.divisor())) / v));
        }
        opciones.sort(Comparator.comparingDouble(Opcion::pctEjercicio).thenComparingDouble(Opcion::pctSesion)
                .thenComparingInt(Opcion::i));
        var pasos = new ArrayList<PasoProgresion>();
        for (var o : opciones) {
            if (o.pctSesion() > p.pasoOrdinarioPct()) continue;   // no se fuerza
            var lista = new ArrayList<>(actuales);
            lista.set(o.i(), o.nuevo());
            int nv = volumen(lista, p.divisor());
            if (nv > tope) continue;
            pasos.add(paso(s, PasoProgresion.Tipo.PROGRESION, o.nuevo().exerciseId(), o.i(), actuales.get(o.i()),
                    o.nuevo(), v, nv, base, techo, estadoReserva, nueva, lista, context));
        }
        return pasos;
    }

    /** El rango de D-P3 para la dosis actual, o null si no se reconoce su dosis base. */
    private record Rango(MvpDosePolicy.Dosis exigida4D, CapabilityPrescriptionPolicy.Dosis capacidad,
                         short seriesMax, short repsMax, int minReps, int maxReps) {
        boolean admite(PlanDraft.DraftExercise n) {
            return exigida4D != null
                    ? ProgresionDosisPolicy.admite4D(exigida4D, n, DosisAutorizada.Origen.PASO)
                    : ProgresionDosisPolicy.admiteCapacidad(capacidad, n, DosisAutorizada.Origen.PASO, minReps, maxReps);
        }
    }

    private static Rango rango(PlanDraft.DraftExercise e, CandidateExercise c, PlanGenerationContext ctx) {
        if (c == null || ctx.profile() == null) return null;
        String objetivo = ctx.profile().goalType();
        String nivel = ctx.profile().fitnessLevel();
        var limites = ctx.prescription() == null ? null : ctx.prescription().repLimits();
        int minReps = limites == null ? 0 : limites.minRepsFor(c.bodyPartCode(), nivel);
        int maxReps = limites == null || limites.maxReps() == null ? Integer.MAX_VALUE : limites.maxReps();
        if (MvpDosePolicy.aplicaA(objetivo)) {
            var exigida = MvpDosePolicy.dosisPara(MandatorySlot.of(c), c.bodyPartCode()).orElse(null);
            if (exigida == null) return null;
            boolean exacta = Objects.equals(e.plannedSets(), exigida.series())
                    && Objects.equals(e.plannedReps(), exigida.reps())
                    && Objects.equals(e.restSeconds(), exigida.descansoSegundos());
            if (!exacta && !ProgresionDosisPolicy.admite4D(exigida, e, DosisAutorizada.Origen.HEREDADA)) return null;
            short sMax = exigida.relleno()
                    ? (short) Math.max(exigida.series(), CapabilityPrescriptionPolicy.SERIES_MAX_AMPLIACION)
                    : exigida.series();
            return new Rango(exigida, null, sMax, (short) (exigida.reps() + ProgresionDosisPolicy.REPS_EXTRA),
                    minReps, maxReps);
        }
        if (!CapabilityPrescriptionPolicy.gobiernaGeneracion(objetivo)) return null;
        for (var cap : CapabilityPrescriptionPolicy.admisibles(c, objetivo, nivel)) {
            var d = CapabilityPrescriptionPolicy.dosis(cap, c, objetivo, nivel, limites).orElse(null);
            if (d == null || d.reps() == null) continue;
            if (!d.coincideCon(e) && !ProgresionDosisPolicy.admiteCapacidad(d, e, DosisAutorizada.Origen.HEREDADA,
                    minReps, maxReps)) continue;
            if (e.plannedReps() < d.reps()) continue;   // por debajo de la base: eso es recuperacion
            return new Rango(null, d,
                    (short) Math.max(d.seriesMax(), CapabilityPrescriptionPolicy.SERIES_MAX_AMPLIACION),
                    (short) Math.min(maxReps, d.reps() + ProgresionDosisPolicy.REPS_EXTRA), minReps, maxReps);
        }
        return null;
    }

    /** La dosis inicial EXACTA de su clase para el retirado (D-P8), con su carga sugerida de entonces. */
    private static PlanDraft.DraftExercise exacta(PlannedWorkoutExercise x, CandidateExercise c,
                                                 PlanGenerationContext ctx) {
        if (ctx.profile() == null) return null;
        String objetivo = ctx.profile().goalType();
        String nivel = ctx.profile().fitnessLevel();
        var limites = ctx.prescription() == null ? null : ctx.prescription().repLimits();
        if (MvpDosePolicy.aplicaA(objetivo)) {
            var d = MvpDosePolicy.dosisPara(MandatorySlot.of(c), c.bodyPartCode()).orElse(null);
            return d == null ? null : new PlanDraft.DraftExercise(x.getExerciseId(), PrescriptionType.SETS_REPS,
                    d.series(), d.reps(), null, x.getTargetLoadKg(), d.descansoSegundos(), x.getNotes());
        }
        var como = new PlanDraft.DraftExercise(x.getExerciseId(), x.getPrescriptionType(), x.getPlannedSets(),
                x.getPlannedReps(), x.getPlannedDurationSeconds(), x.getTargetLoadKg(), x.getRestSeconds(), x.getNotes());
        int minReps = limites == null ? 0 : limites.minRepsFor(c.bodyPartCode(), nivel);
        int maxReps = limites == null || limites.maxReps() == null ? Integer.MAX_VALUE : limites.maxReps();
        for (var cap : CapabilityPrescriptionPolicy.admisibles(c, objetivo, nivel)) {
            var d = CapabilityPrescriptionPolicy.dosis(cap, c, objetivo, nivel, limites).orElse(null);
            if (d == null) continue;
            if (d.coincideCon(como) || ProgresionDosisPolicy.admiteCapacidad(d, como,
                    DosisAutorizada.Origen.HEREDADA, minReps, maxReps))
                return new PlanDraft.DraftExercise(x.getExerciseId(), d.tipo(), d.series(), d.reps(), d.segundos(),
                        x.getTargetLoadKg(), d.descanso(), x.getNotes());
        }
        return null;
    }

    private PasoProgresion paso(PlannedWorkout s, PasoProgresion.Tipo tipo, Long exerciseId, int posicion,
                                PlanDraft.DraftExercise antes, PlanDraft.DraftExercise despues, int v, int nv,
                                int base, int techo, String estadoReserva, PlanDraft.DraftWorkout nueva,
                                List<PlanDraft.DraftExercise> lista, PlanGenerationContext context) {
        var borrador = new PlanDraft.DraftWorkout(nueva.scheduledDate(), nueva.focus(), nueva.name(), 0, lista);
        int estimados = estimador.estimateMinutes(borrador, context.prescription());
        // Los minutos no bajan en un paso que sube (D-P7): los que estima el estimador, o los de antes.
        var contenido = new PlanDraft.DraftWorkout(nueva.scheduledDate(), nueva.focus(), nueva.name(),
                Math.max(nueva.expectedDurationMinutes(), estimados), List.copyOf(lista));
        return new PasoProgresion(s.getId(), tipo, exerciseId, posicion, PasoProgresion.Dosis.de(antes),
                PasoProgresion.Dosis.de(despues), v, nv, base, techo, estadoReserva, contenido);
    }

    // =======================================================================
    // Utilidades
    // =======================================================================

    private static List<PlanDraft.DraftExercise> ejercicios(PlannedWorkout w) {
        return w.exercisesView().stream()
                .sorted(Comparator.comparing(PlannedWorkoutExercise::getExerciseOrder))
                .map(e -> new PlanDraft.DraftExercise(e.getExerciseId(), e.getPrescriptionType(), e.getPlannedSets(),
                        e.getPlannedReps(), e.getPlannedDurationSeconds(), e.getTargetLoadKg(), e.getRestSeconds(),
                        e.getNotes()))
                .toList();
    }

    private static int volumen(List<PlanDraft.DraftExercise> lista, int divisor) {
        return lista.stream().mapToInt(e -> e.volume(divisor)).sum();
    }

    private static int posicion(List<PlanDraft.DraftExercise> lista, Long exerciseId) {
        for (int i = 0; i < lista.size(); i++)
            if (Objects.equals(lista.get(i).exerciseId(), exerciseId)) return i;
        return -1;
    }

    private static PlanDraft.DraftExercise con(PlanDraft.DraftExercise e, short series, Short reps, Integer segundos) {
        return new PlanDraft.DraftExercise(e.exerciseId(), e.prescriptionType(), series, reps,
                segundos == null ? e.plannedDurationSeconds() : segundos, e.targetLoadKg(), e.restSeconds(), e.notes());
    }

    /** {@code d} es menor que {@code o} en alguna dimension y no mayor en ninguna. */
    private static boolean menor(PlannedWorkoutExercise d, PlannedWorkoutExercise o) {
        int[] a = {n(d.getPlannedSets()), n(d.getPlannedReps()), n(d.getPlannedDurationSeconds())};
        int[] b = {n(o.getPlannedSets()), n(o.getPlannedReps()), n(o.getPlannedDurationSeconds())};
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
