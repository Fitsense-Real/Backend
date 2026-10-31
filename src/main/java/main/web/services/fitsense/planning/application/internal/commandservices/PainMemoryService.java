package main.web.services.fitsense.planning.application.internal.commandservices;

import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalCatalogService;
import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalExecutionService;
import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalProfilingService;
import main.web.services.fitsense.planning.domain.model.aggregates.UserSafetyExclusion;
import main.web.services.fitsense.planning.domain.model.aggregates.UserSafetyExclusion.Reason;
import main.web.services.fitsense.planning.domain.model.aggregates.UserSafetyExclusion.RecordedBy;
import main.web.services.fitsense.planning.domain.model.aggregates.WeeklyTrainingPlan;
import main.web.services.fitsense.planning.domain.model.entities.PlannedWorkout;
import main.web.services.fitsense.planning.domain.model.entities.PlannedWorkoutExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanStatus;
import main.web.services.fitsense.planning.domain.model.valueobjects.SafetyProfile;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.UserSafetyExclusionRepository;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.WeeklyTrainingPlanRepository;
import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;
import main.web.services.fitsense.shared.domain.exceptions.ResourceNotFoundException;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * P-02 · MEMORIA PERSISTENTE DEL DOLOR.
 * <p>
 * <b>Registrar.</b> Un ejercicio marcado con dolor ({@code PAIN_OR_DISCOMFORT} en su resultado) queda
 * excluido para ese usuario. Si el dolor consta en la sesion sin decir que ejercicio fue, se excluyen
 * PROVISIONALMENTE todos los de esa sesion, que es la misma regla de V8C: si no se sabe cual dolio,
 * cualquiera pudo ser. Se registra al omitir la sesion por dolor y, de nuevo, al cerrar el ciclo
 * sobre la semana medida (lo que la persona marco ejercicio a ejercicio llega por ejecucion). Las dos
 * vias son idempotentes por el evento original.
 * <p>
 * <b>Aplicar.</b> {@link #excluidosActivos} se resta del conjunto elegible en TODA preparacion
 * (generacion inicial, regeneracion, adaptacion, progresion y reincorporacion parten de ese
 * conjunto), y {@code SafetyExclusionResolver} lo declara causa de seguridad de lo heredado.
 * <p>
 * <b>No caduca.</b> Nada aqui mira fechas para levantar una exclusion: ni semanas, ni cumplimiento,
 * ni ausencia de reportes. Solo {@link #reactivar}, por accion explicita del propio usuario y tras
 * un control de seguridad que la confirmacion no puede saltarse.
 */
@Service
public class PainMemoryService {

    private static final Logger log = LoggerFactory.getLogger(PainMemoryService.class);
    private static final String DOLOR = SkipReason.PAIN_OR_DISCOMFORT.name();

    private final UserSafetyExclusionRepository repository;
    private final WeeklyTrainingPlanRepository planRepository;
    private final ExternalExecutionService execution;
    private final ExternalProfilingService profiling;
    private final ExternalCatalogService catalog;
    private final SafetyConflictChecker conflictos;

    public PainMemoryService(UserSafetyExclusionRepository repository, WeeklyTrainingPlanRepository planRepository,
                             ExternalExecutionService execution, ExternalProfilingService profiling,
                             ExternalCatalogService catalog, SafetyConflictChecker conflictos) {
        this.repository = repository;
        this.planRepository = planRepository;
        this.execution = execution;
        this.profiling = profiling;
        this.catalog = catalog;
        this.conflictos = conflictos;
    }

    // =====================================================================
    // Registrar
    // =====================================================================

    /** Una exclusion nueva, para la traza del ciclo. */
    public record Registro(Long exerciseId, Reason reason, Long plannedWorkoutId, LocalDate eventDate) {}

    /**
     * Registra el dolor de la semana que empieza en {@code weekStart} (la version vigente o cerrada de
     * su plan). Idempotente. Transaccion propia: lo registrado no se pierde aunque la generacion que
     * viene despues falle.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Registro> registrarSemana(Long userId, LocalDate weekStart) {
        var plan = planRepository.findByUserIdAndWeekStartDateOrderByPlanVersionAsc(userId, weekStart).stream()
                .filter(p -> p.getStatus() != PlanStatus.REPLACED)
                .reduce((a, b) -> b).orElse(null);
        if (plan == null) return List.of();
        return registrarPlan(plan, RecordedBy.CICLO_SEMANAL);
    }

    private List<Registro> registrarPlan(WeeklyTrainingPlan plan, RecordedBy via) {
        var workouts = plan.workoutsView();
        var resultados = execution.fetchResultsByWorkout(workouts.stream().map(PlannedWorkout::getId).toList());
        var nuevos = new ArrayList<Registro>();
        for (var w : workouts) {
            var porPlanificado = new LinkedHashMap<Long, PlannedWorkoutExercise>();
            w.exercisesView().forEach(e -> porPlanificado.put(e.getId(), e));
            var r = resultados.get(w.getId());
            var identificados = r == null || r.exercises() == null ? List.<PlannedWorkoutExercise>of()
                    : r.exercises().stream().filter(x -> DOLOR.equals(x.skipReason()))
                    .map(x -> porPlanificado.get(x.plannedExerciseId())).filter(Objects::nonNull).toList();
            for (var e : identificados)
                registrar(plan, w, e, Reason.DOLOR_IDENTIFICADO, via).ifPresent(nuevos::add);
            boolean dolorDeSesion = w.getSkipReason() == SkipReason.PAIN_OR_DISCOMFORT;
            if (dolorDeSesion && identificados.isEmpty())
                for (var e : porPlanificado.values())
                    registrar(plan, w, e, Reason.DOLOR_SESION_SIN_IDENTIFICAR, via).ifPresent(nuevos::add);
        }
        if (!nuevos.isEmpty())
            log.warn("DOLOR_REGISTRADO usuario {} plan {} via {}: {}", plan.getUserId(), plan.getId(), via,
                    nuevos.stream().map(Registro::exerciseId).toList());
        return nuevos;
    }

    /**
     * Al OMITIR una sesion por dolor (sin decir que ejercicio): sus ejercicios quedan excluidos en el
     * mismo momento, dentro de la transaccion de la omision.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<Registro> registrarOmision(PlannedWorkout workout) {
        if (workout.getSkipReason() != SkipReason.PAIN_OR_DISCOMFORT) return List.of();
        var nuevos = new ArrayList<Registro>();
        for (var e : workout.exercisesView())
            registrar(workout.getPlan(), workout, e, Reason.DOLOR_SESION_SIN_IDENTIFICAR, RecordedBy.OMISION)
                    .ifPresent(nuevos::add);
        if (!nuevos.isEmpty())
            log.warn("DOLOR_REGISTRADO usuario {} sesion {} via OMISION: {}", workout.getPlan().getUserId(),
                    workout.getId(), nuevos.stream().map(Registro::exerciseId).toList());
        return nuevos;
    }

    private Optional<Registro> registrar(WeeklyTrainingPlan plan, PlannedWorkout w, PlannedWorkoutExercise e,
                                         Reason reason, RecordedBy via) {
        if (e.getExerciseId() == null) return Optional.empty();
        // El evento original es la identidad: ni duplica ni resucita una exclusion ya reactivada.
        if (repository.existsByUserIdAndExerciseIdAndSourcePlannedWorkoutId(plan.getUserId(), e.getExerciseId(),
                w.getId())) return Optional.empty();
        repository.save(new UserSafetyExclusion(plan.getUserId(), e.getExerciseId(), reason, plan.getId(),
                w.getId(), e.getId(), w.getScheduledDate(), via));
        return Optional.of(new Registro(e.getExerciseId(), reason, w.getId(), w.getScheduledDate()));
    }

    // =====================================================================
    // Aplicar
    // =====================================================================

    /** Los ejercicios excluidos HOY por dolor para ese usuario. Solo los suyos. */
    @Transactional(readOnly = true)
    public Set<Long> excluidosActivos(Long userId) {
        var ids = new TreeSet<Long>();
        repository.findByUserIdAndStatusOrderByIdAsc(userId, UserSafetyExclusion.Status.ACTIVA)
                .forEach(x -> ids.add(x.getExerciseId()));
        return ids;
    }

    @Transactional(readOnly = true)
    public List<UserSafetyExclusion> historial(Long userId) {
        return repository.findByUserIdOrderByIdAsc(userId);
    }

    // =====================================================================
    // Reactivar
    // =====================================================================

    /** Lo que la API devuelve tras reactivar. */
    public record Reactivacion(Long exerciseId, List<Long> exclusionesReactivadas, OffsetDateTime reactivadaEn,
                               boolean candidatoHoy, String nota) {}

    /**
     * REACTIVACION EXPLICITA. El usuario confirma que ya no siente molestias con ese ejercicio y pide
     * que vuelva a CONSIDERARSE. No se le pide ni se guarda ninguna afirmacion medica.
     * <p>
     * La confirmacion no levanta ninguna restriccion de seguridad: si el ejercicio choca hoy con el
     * perfil (banderas del catalogo frente a lo declarado, retenciones INC-4C-03 o INC-CAT-1374), se
     * rechaza. Reactivar NO es reincorporar: el ejercicio vuelve a ser candidato y sigue sujeto a
     * todos los validadores; puede no aparecer nunca.
     */
    @Transactional
    public Reactivacion reactivar(Long userId, Long exerciseId, boolean confirmacionExplicita, String motivo) {
        if (!confirmacionExplicita)
            throw new DomainRuleViolationException(
                    "Para volver a considerar este ejercicio confirma de forma explicita que lo pides tu.");
        var activas = repository.findByUserIdAndExerciseIdAndStatus(userId, exerciseId, UserSafetyExclusion.Status.ACTIVA);
        // Otro usuario, o un ejercicio que no esta excluido: no se revela nada.
        if (activas.isEmpty()) throw new ResourceNotFoundException("Exclusion por dolor activa del ejercicio", exerciseId);

        var perfil = profiling.fetchProfile(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil del usuario", userId));
        var seguridad = SafetyProfile.forAgeLevelAndDeclared(perfil.age(), perfil.fitnessLevel(), perfil.declaradas());
        var conflicto = conflictos.motivos(List.of(exerciseId), perfil, seguridad);
        if (!conflicto.isEmpty())
            throw new DomainRuleViolationException(("REACTIVACION_RECHAZADA_POR_SEGURIDAD: el ejercicio %d choca con"
                    + " una restriccion de seguridad vigente (%s). Tu confirmacion no puede levantarla.")
                    .formatted(exerciseId, conflicto.get(exerciseId)));

        var ahora = OffsetDateTime.now(ZoneOffset.UTC);
        var evidencia = "CONFIRMACION_EXPLICITA_DEL_USUARIO user=%d ejercicio=%d via=POST /api/v1/users/me/pain-exclusions/%d/reactivation en=%s; control de seguridad sin conflicto"
                .formatted(userId, exerciseId, exerciseId, ahora);
        var ids = new ArrayList<Long>();
        for (var x : activas) {
            x.reactivar(ahora, motivo, evidencia);
            repository.save(x);
            ids.add(x.getId());
        }
        boolean candidato = catalog.fetchEligibleFor(perfil, perfil.maxDifficultyLevel(), seguridad).stream()
                .anyMatch(c -> exerciseId.equals(c.exerciseId()));
        log.warn("DOLOR_REACTIVADO usuario {} ejercicio {} exclusiones {} candidato_hoy={}", userId, exerciseId, ids,
                candidato);
        return new Reactivacion(exerciseId, ids, ahora, candidato, candidato
                ? "Vuelve a considerarse en las proximas semanas. No se anade de inmediato: entra solo si una"
                + " adaptacion, una regeneracion o una reincorporacion lo eligen y pasa todas las reglas."
                : "La exclusion por dolor se levanto, pero hoy no es candidato por tu perfil (equipamiento,"
                + " nivel, bloqueos o restricciones). Sigue sin prescribirse.");
    }
}
