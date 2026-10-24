package main.web.services.fitsense.planning.application.internal.commandservices;

import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalConfigurationService;
import main.web.services.fitsense.planning.application.internal.queryservices.PublishedPlanSafetyService;
import main.web.services.fitsense.planning.domain.model.aggregates.WeeklyTrainingPlan;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.entities.PlannedWorkout;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.ConservationValidator;
import main.web.services.fitsense.planning.domain.services.SafetySubstitutionPolicy;
import main.web.services.fitsense.planning.domain.services.StrictPlanValidation;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.WeeklyTrainingPlanRepository;
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * PLANES YA PUBLICADOS · reparacion por seguridad mediante una VERSION NUEVA.
 * <p>
 * Si una semana activa contiene un ejercicio que hoy no es seguro para el
 * participante, se publica la version siguiente de esa semana con ese
 * ejercicio sustituido por SUST-SEG-1 y todo lo demas identico. La version
 * anterior no se toca salvo para pasar a REPLACED, como en cualquier
 * reemplazo: su contenido, sus sesiones y su evidencia quedan como estaban.
 * <p>
 * <b>Solo repara semanas SIN ninguna sesion terminada ni en curso.</b> Una
 * version describe la semana entera: la adaptacion siguiente
 * ({@code PreviousWeekAssembler}), V31 y la evidencia V32 leen SOLO la ultima
 * version. Repararla a mitad de semana obligaria a copiar sesiones ya hechas
 * -duplicandolas en el denominador, sin sus resultados- o a dejarlas fuera de
 * la semana medida. Esa decision no se toma aqui: la semana se queda como
 * esta, con la sesion insegura BLOQUEADA en lectura y en ejecucion
 * ({@link PublishedPlanSafetyService}), y el resultado lo dice.
 * <p>
 * Validacion: la semana entera pasa por V8C (copia exacta salvo las
 * sustituciones con causa verificada) y por la validacion estricta (V1-V24,
 * dosis 4D en ENFORCE). Si no hay sustituto o algo no valida, no se publica
 * nada.
 * <p>
 * IDEMPOTENTE: repetirla sobre una semana ya reparada no encuentra conflictos
 * y no crea otra version. Dos reparaciones simultaneas no pueden dejar dos
 * versiones activas: lo impiden {@code ux_plan_active} y
 * {@code uq_plan_user_week_version}, y la escritura comprueba que la version
 * leida sigue siendo la activa.
 * <p>
 * En fases y sin transaccion envolvente, a proposito: {@code prepare} abre la
 * suya y, si fallara dentro de otra, la dejaria marcada para rollback.
 */
@Service
public class PublishedPlanSafetyRepairService {

    private static final Logger log = LoggerFactory.getLogger(PublishedPlanSafetyRepairService.class);

    public static final String AJUSTE = "SAFETY_REPAIR";
    public static final String ESQUEMA = "SAFETY_REPAIR_V1";

    public enum Estado {
        /** No hay plan activo para esa semana. */
        SIN_PLAN,
        /** La semana ya termino: es historia y no se repara. */
        SEMANA_PASADA,
        /** Ninguna sesion abierta choca con la seguridad. */
        NADA_QUE_REPARAR,
        /** Hay una sesion en curso: se reintenta cuando termine. */
        SESION_EN_CURSO,
        /** Ya hay sesiones terminadas: queda bloqueada, requiere decision (ver clase). */
        SEMANA_EMPEZADA,
        /** No hay sustituto seguro: queda bloqueada y requiere intervencion. */
        SIN_SUSTITUTO,
        /** La semana reparada no pasa la validacion: no se publica. */
        NO_VALIDA,
        /** Otra escritura reemplazo la semana mientras tanto: no se publica nada. */
        CAMBIO_CONCURRENTE,
        /** Version nueva publicada. */
        REPARADA
    }

    public record Resultado(Estado estado, Long planOriginal, Long planNuevo,
                            Map<Long, String> conflictos, List<SustitucionSeguridad> sustituciones,
                            List<String> problemas) {
        public Resultado {
            conflictos = conflictos == null ? Map.of() : Map.copyOf(conflictos);
            sustituciones = sustituciones == null ? List.of() : List.copyOf(sustituciones);
            problemas = problemas == null ? List.of() : List.copyOf(problemas);
        }

        static Resultado de(Estado estado, Long plan, Map<Long, String> conflictos, List<String> problemas) {
            return new Resultado(estado, plan, null, conflictos, List.of(), problemas);
        }
    }

    /** Lo leido de la version activa, para trabajar fuera de la transaccion. */
    private record Leido(Long planId, short version, GenerationSource origen, List<DerivedWorkout> semana,
                         Map<Long, String> conflictos, Map<Long, String> causaPorEjercicio) {}

    private final WeeklyTrainingPlanRepository planRepository;
    private final WeeklyTrainingPlanCommandService commandService;
    private final PreviousWeekAssembler assembler;
    private final SafetyExclusionResolver resolver;
    private final PublishedPlanSafetyService safety;
    private final ConservationValidator v8c;
    private final StrictPlanValidation estricta;
    private final ExternalConfigurationService configuracion;
    private final JsonSupport json;
    private final TransactionTemplate escritura;
    private final TransactionTemplate lectura;

    public PublishedPlanSafetyRepairService(WeeklyTrainingPlanRepository planRepository,
                                            WeeklyTrainingPlanCommandService commandService,
                                            PreviousWeekAssembler assembler,
                                            SafetyExclusionResolver resolver,
                                            PublishedPlanSafetyService safety,
                                            ConservationValidator v8c,
                                            StrictPlanValidation estricta,
                                            ExternalConfigurationService configuracion,
                                            JsonSupport json,
                                            PlatformTransactionManager transacciones) {
        this.planRepository = planRepository;
        this.commandService = commandService;
        this.assembler = assembler;
        this.resolver = resolver;
        this.safety = safety;
        this.v8c = v8c;
        this.estricta = estricta;
        this.configuracion = configuracion;
        this.json = json;
        this.escritura = new TransactionTemplate(transacciones);
        this.lectura = new TransactionTemplate(transacciones);
        this.lectura.setReadOnly(true);
    }

    public Resultado reparar(Long userId, LocalDate cualquierDiaDeLaSemana, LocalDate hoy) {
        var week = TrainingWeek.containing(cualquierDiaDeLaSemana);

        // FASE 1 · leer y decidir si hay algo que reparar.
        var fase1 = lectura.execute(tx -> leer(userId, week, hoy));
        if (fase1 instanceof Resultado r) return registrar(userId, week, r);
        var leido = (Leido) fase1;

        // FASE 2 · el contexto de hoy (conjunto elegible, seguridad, dosis) y la
        // semana tal como esta publicada, que hace de semana de referencia.
        PlanGenerationContext contexto;
        try {
            var preparada = commandService.prepare(new GenerateWeeklyPlanCommand(userId, week.startDate(),
                    PlanAdjustment.none(), true, null, null));
            int divisor = configuracion.durationToRepsDivisor();
            var publicada = lectura.execute(tx -> assembler.assemble(userId, week.startDate(), divisor, null, null));
            var c = preparada.context();
            contexto = new PlanGenerationContext(c.userId(), c.weekNumber(), c.weekStartDate(), c.weekEndDate(),
                    c.profile(), c.adjustment(), publicada, c.availableExercises(), c.safety(), c.prescription());
        } catch (RuntimeException e) {
            return registrar(userId, week, Resultado.de(Estado.NO_VALIDA, leido.planId(), leido.conflictos(),
                    List.of("No se pudo preparar el contexto de hoy: " + e.getMessage())));
        }

        var elegibles = contexto.availableExercises().stream()
                .collect(Collectors.toMap(CandidateExercise::exerciseId, Function.identity(), (a, b) -> a,
                        LinkedHashMap::new));
        var exclusiones = resolver.resolver(contexto);

        // FASE 3 · sustituir y validar la semana entera.
        var reparada = SafetySubstitutionPolicy.reparar(leido.semana(), contexto.previousWeek(), exclusiones,
                elegibles, contexto);
        if (!reparada.problemas().isEmpty())
            return registrar(userId, week, Resultado.de(Estado.SIN_SUSTITUTO, leido.planId(), leido.conflictos(),
                    reparada.problemas()));

        var problemas = new ArrayList<String>();
        // Todo lo que se marco como inseguro tiene que haber salido.
        var quedan = reparada.semana().stream().flatMap(d -> d.workout().exercises().stream())
                .map(PlanDraft.DraftExercise::exerciseId).filter(leido.causaPorEjercicio()::containsKey)
                .collect(Collectors.toCollection(TreeSet::new));
        if (!quedan.isEmpty())
            problemas.add("SEGURIDAD: siguen en la semana %s, que hoy no son seguros %s."
                    .formatted(quedan, leido.causaPorEjercicio()));
        var informe = v8c.validateContinuidad(new WeekDerivation(reparada.semana(), List.of()),
                contexto.previousWeek(), elegibles, exclusiones, reparada.sustituciones());
        problemas.addAll(informe.problems());
        problemas.addAll(estricta.revisar(new PlanDraft(leido.origen(), null, null, null, informe.describe(),
                reparada.semana().stream().map(DerivedWorkout::workout).toList()), contexto, informe));
        if (!problemas.isEmpty())
            return registrar(userId, week, Resultado.de(Estado.NO_VALIDA, leido.planId(), leido.conflictos(),
                    problemas));

        // FASE 4 · publicar la version nueva, en una transaccion corta.
        try {
            var nuevo = escritura.execute(tx -> publicar(userId, week, leido, reparada.semana(),
                    reparada.sustituciones()));
            if (nuevo == null)
                return registrar(userId, week, Resultado.de(Estado.CAMBIO_CONCURRENTE, leido.planId(),
                        leido.conflictos(), List.of("La version %d ya no es la activa.".formatted(leido.version()))));
            return registrar(userId, week, new Resultado(Estado.REPARADA, leido.planId(), nuevo,
                    leido.conflictos(), reparada.sustituciones(), List.of()));
        } catch (DataIntegrityViolationException e) {
            return registrar(userId, week, Resultado.de(Estado.CAMBIO_CONCURRENTE, leido.planId(),
                    leido.conflictos(), List.of("Otra version se publico a la vez: " + e.getMostSpecificCause().getMessage())));
        }
    }

    private Object leer(Long userId, TrainingWeek week, LocalDate hoy) {
        var plan = planRepository.findByUserIdAndWeekStartDateAndStatus(userId, week.startDate(), PlanStatus.ACTIVE)
                .orElse(null);
        if (plan == null) return Resultado.de(Estado.SIN_PLAN, null, Map.of(), List.of());
        if (plan.getWeekEndDate().isBefore(hoy))
            return Resultado.de(Estado.SEMANA_PASADA, plan.getId(), Map.of(), List.of());

        var conflictos = safety.conflictos(plan);
        if (conflictos.isEmpty()) return Resultado.de(Estado.NADA_QUE_REPARAR, plan.getId(), Map.of(), List.of());

        if (plan.workoutsView().stream().anyMatch(w -> w.getStatus() == WorkoutStatus.IN_PROGRESS))
            return Resultado.de(Estado.SESION_EN_CURSO, plan.getId(), conflictos, List.of(
                    "Hay una sesion en curso; la reparacion se reintenta cuando termine."));
        if (plan.workoutsView().stream().anyMatch(w -> w.getStatus().isFinished()))
            return Resultado.de(Estado.SEMANA_EMPEZADA, plan.getId(), conflictos, List.of(
                    "La semana ya tiene sesiones terminadas: una version nueva duplicaria o perderia esas sesiones "
                            + "(la adaptacion y V32 leen solo la ultima version). La sesion insegura queda "
                            + "bloqueada; requiere decision."));

        var causaPorEjercicio = new TreeMap<Long, String>();
        var semana = new ArrayList<DerivedWorkout>();
        for (var w : plan.workoutsView()) {
            for (var e : w.exercisesView())
                if (conflictos.containsKey(e.getId())) causaPorEjercicio.put(e.getExerciseId(), conflictos.get(e.getId()));
            semana.add(new DerivedWorkout(w.getId(), borrador(w)));
        }
        return new Leido(plan.getId(), plan.getPlanVersion(), plan.getGenerationSource(), semana, conflictos,
                causaPorEjercicio);
    }

    private static PlanDraft.DraftWorkout borrador(PlannedWorkout w) {
        return new PlanDraft.DraftWorkout(w.getScheduledDate(), w.getFocusCode(), w.getWorkoutName(),
                w.getExpectedDurationMinutes(), w.exercisesView().stream()
                .map(e -> new PlanDraft.DraftExercise(e.getExerciseId(), e.getPrescriptionType(), e.getPlannedSets(),
                        e.getPlannedReps(), e.getPlannedDurationSeconds(), e.getTargetLoadKg(), e.getRestSeconds(),
                        e.getNotes()))
                .toList());
    }

    private Long publicar(Long userId, TrainingWeek week, Leido leido, List<DerivedWorkout> semana,
                         List<SustitucionSeguridad> sustituciones) {
        var anterior = planRepository.findByUserIdAndWeekStartDateAndStatus(userId, week.startDate(), PlanStatus.ACTIVE)
                .orElse(null);
        // Solo sobre la MISMA version que se leyo y valido.
        if (anterior == null || !anterior.getId().equals(leido.planId())
                || anterior.getPlanVersion() != leido.version()
                || anterior.workoutsView().stream().anyMatch(w -> !PublishedPlanSafetyService.abierta(w)))
            return null;

        var origenes = anterior.workoutsView().stream()
                .collect(Collectors.toMap(PlannedWorkout::getId, Function.identity()));
        var nuevo = WeeklyTrainingPlan.nextVersionOf(anterior, anterior.getGenerationSource(),
                anterior.getModelName(), anterior.getInputSnapshot(), (short) 1, AJUSTE, motivo(sustituciones));

        var copias = new ArrayList<Map<String, Object>>();
        for (var derivada : semana) {
            var original = origenes.get(derivada.sourcePlannedWorkoutId());
            var d = derivada.workout();
            var copia = nuevo.addWorkout(d.scheduledDate(), d.focus(), d.name(), d.expectedDurationMinutes(),
                    original.getExpiresAt());
            for (var e : d.exercises())
                copia.addExercise(e.exerciseId(), e.prescriptionType(), e.plannedSets(), e.plannedReps(),
                        e.plannedDurationSeconds(), e.targetLoadKg(), e.restSeconds(), e.notes());
            // El linaje V31 apunta a la semana ANTERIOR; se conserva el de la
            // sesion reemplazada. La correspondencia con ella va en el snapshot.
            if (original.getSourcePlannedWorkoutId() != null)
                copia.linkSource(original.getSourcePlannedWorkoutId());
            copias.add(Map.of("from_planned_workout_id", original.getId(), "display_order",
                    (int) copia.getDisplayOrder()));
        }
        nuevo.attachOutputSnapshot(json.write(new LinkedHashMap<>(Map.of(
                "schema_version", ESQUEMA,
                "repaired_plan_id", anterior.getId(),
                "repaired_plan_version", anterior.getPlanVersion(),
                "rule", SafetySubstitutionPolicy.REGLA,
                "conflicts", leido.conflictos(),
                "substitutions", sustituciones,
                "workouts", copias,
                "repaired_at", OffsetDateTime.now().toString()))));

        anterior.markReplaced();
        planRepository.saveAndFlush(anterior);
        return planRepository.saveAndFlush(nuevo).getId();
    }

    private static String motivo(List<SustitucionSeguridad> sustituciones) {
        var texto = "Correccion de seguridad (%s): %s".formatted(SafetySubstitutionPolicy.REGLA,
                sustituciones.stream().map(s -> "%d por %d en %s (%s)".formatted(s.quitado(), s.puesto(), s.fecha(),
                        s.motivo())).collect(Collectors.joining("; ")));
        return texto.length() <= 500 ? texto : texto.substring(0, 497) + "...";
    }

    private static Resultado registrar(Long userId, TrainingWeek week, Resultado r) {
        log.info("REPARACION_SEGURIDAD user={} semana={} estado={} plan={} nuevo={} conflictos={} sustituciones={} "
                        + "problemas={}", userId, week.startDate(), r.estado(), r.planOriginal(), r.planNuevo(),
                r.conflictos(), r.sustituciones().size(), r.problemas());
        return r;
    }
}
