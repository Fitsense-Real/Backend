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
import main.web.services.fitsense.planning.domain.model.valueobjects.ExerciseEquivalence;
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
 * conjunto), y {@code SafetyExclusionResolver} lo declara causa de seguridad de lo heredado. Incluye
 * las variantes equivalentes (O20): permanentes las de lo confirmado y provisionales las de un dolor
 * de sesion sin identificar ({@link #exclusionesEfectivas}).
 * <p>
 * <b>Identificar despues (O20).</b> Un dolor de sesion sin identificar es un reporte general; la
 * persona puede decir despues que ejercicio fue ({@link #identificar}) y esa exclusion queda confirmada.
 * <p>
 * <b>No caduca.</b> Nada aqui mira fechas para levantar una exclusion registrada: ni semanas, ni
 * cumplimiento, ni ausencia de reportes. Solo {@link #reactivar}, por accion explicita del propio
 * usuario y tras un control de seguridad que la confirmacion no puede saltarse. Lo unico que deja de
 * aplicarse solo es la familia PROVISIONAL de un dolor de sesion sin identificar, que no es una
 * exclusion registrada: el ejercicio de la sesion sigue excluido.
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
        // El evento original es la identidad: ni duplica ni resucita una exclusion ya reactivada. ON CONFLICT DO
        // NOTHING: dos registros simultaneos del mismo evento (omision y ciclo) no chocan ni abortan el ciclo.
        int insertadas = repository.registrarSiNoExiste(plan.getUserId(), e.getExerciseId(), reason.name(),
                plan.getId(), w.getId(), e.getId(), w.getScheduledDate(), via.name());
        if (insertadas == 0) return Optional.empty();
        return Optional.of(new Registro(e.getExerciseId(), reason, w.getId(), w.getScheduledDate()));
    }

    // =====================================================================
    // Aplicar
    // =====================================================================

    /**
     * O20: cuantas semanas con plan, posteriores a un dolor de sesion SIN IDENTIFICAR, mantienen excluida de forma
     * PROVISIONAL la familia equivalente de los ejercicios de esa sesion: la adaptacion o regeneracion inmediata y la
     * semana siguiente. Despues sigue excluido el propio ejercicio de la sesion (hasta que se identifique o se
     * reactive), pero no toda su familia. Se mide en planes, no en dias: sin planes nuevos la ventana no se cierra.
     */
    public static final int SEMANAS_FAMILIA_PROVISIONAL = 2;

    /** De donde viene una exclusion efectiva. */
    public enum Origen {
        /** Registrada para ese ejercicio (marcado con dolor, o de una sesion con dolor sin identificar). */
        DIRECTA,
        /** Derivada: variante equivalente de un ejercicio con exclusion directa. No se guarda. */
        EQUIVALENTE
    }

    /**
     * Una exclusion EFECTIVA, lo que de verdad se resta del conjunto elegible, con su porque.
     *
     * @param confirmada  DIRECTA: la persona marco ese ejercicio o lo identifico despues. EQUIVALENTE: deriva de uno
     *                    confirmado.
     * @param provisional EQUIVALENTE derivada de un dolor de sesion sin identificar: solo durante
     *                    {@link #SEMANAS_FAMILIA_PROVISIONAL} semanas con plan.
     * @param derivaDe    EQUIVALENTE: el ejercicio con exclusion directa del que deriva.
     * @param nivel       EQUIVALENTE: {@code VARIANTE_MISMO_EJERCICIO} o {@code MISMO_GESTO}.
     */
    public record ExclusionEfectiva(Long exerciseId, Origen origen, boolean confirmada, boolean provisional,
                                    Long derivaDe, String nivel, String grupo, String justificacion,
                                    Long sourcePlannedWorkoutId, LocalDate eventDate) {}

    /**
     * Los ejercicios excluidos HOY por dolor para ese usuario. Solo los suyos. Es {@link #exclusionesEfectivas}: lo
     * leen todos los caminos que aplican el dolor (preparacion, SafetyExclusionResolver, PublishedPlanSafetyService y
     * SafeRegenerationService).
     */
    @Transactional(readOnly = true)
    public Set<Long> excluidosActivos(Long userId) {
        var ids = new TreeSet<Long>();
        exclusionesEfectivas(userId).forEach(x -> ids.add(x.exerciseId()));
        return ids;
    }

    /** Solo los ejercicios con una exclusion ACTIVA registrada (sin su familia). */
    @Transactional(readOnly = true)
    public Set<Long> excluidosActivosDirectos(Long userId) {
        var ids = new TreeSet<Long>();
        repository.findByUserIdAndStatusOrderByIdAsc(userId, UserSafetyExclusion.Status.ACTIVA)
                .forEach(x -> ids.add(x.getExerciseId()));
        return ids;
    }

    /**
     * O20 (opcion A y ajuste final del 24/11/2026). Lo excluido hoy y por que:
     * <ul>
     *   <li><b>DIRECTA:</b> todo ejercicio con una exclusion ACTIVA, confirmada o de una sesion con dolor sin
     *   identificar (la regla de V8C: si no se sabe cual dolio, cualquiera pudo ser). Hasta su reactivacion
     *   explicita, como siempre.</li>
     *   <li><b>EQUIVALENTE, permanente:</b> las variantes equivalentes
     *   ({@link main.web.services.fitsense.catalog.domain.services.FamiliaDeMovimientoEquivalente}) de cada ejercicio
     *   CONFIRMADO. Duran lo que su exclusion directa: se recalculan en cada lectura y no se guardan.</li>
     *   <li><b>EQUIVALENTE, provisional:</b> las de los ejercicios de una sesion con dolor SIN IDENTIFICAR, solo
     *   durante {@link #SEMANAS_FAMILIA_PROVISIONAL} semanas con plan. Asi la regeneracion inmediata no reproduce el
     *   problema con otra variante del mismo gesto, y un reporte general no deja excluidas para siempre las familias
     *   de todos los ejercicios de la sesion. Si la persona identifica despues el ejercicio ({@link #identificar}),
     *   su familia pasa a ser permanente.</li>
     * </ul>
     * Si el catalogo no responde, se mantiene como minimo lo directo (falla cerrado) y queda
     * {@code DOLOR_FAMILIA_NO_CALCULADA}.
     */
    @Transactional(readOnly = true)
    public List<ExclusionEfectiva> exclusionesEfectivas(Long userId) {
        var activas = repository.findByUserIdAndStatusOrderByIdAsc(userId, UserSafetyExclusion.Status.ACTIVA);
        if (activas.isEmpty()) return List.of();
        // Una entrada por ejercicio: si alguna de sus filas esta confirmada, cuenta la confirmada (la primera).
        var directas = new TreeMap<Long, UserSafetyExclusion>();
        for (var x : activas)
            directas.merge(x.getExerciseId(), x, (a, b) -> !a.confirmada() && b.confirmada() ? b : a);
        var semanas = planRepository == null ? null : planRepository.semanasConPlan(userId);
        var base = new TreeMap<Long, Boolean>(); // ejercicio directo -> su familia es permanente (confirmada)
        directas.forEach((id, x) -> {
            if (x.confirmada()) base.put(id, true);
            else if (familiaProvisionalVigente(activas, id, semanas)) base.put(id, false);
        });
        var resultado = new ArrayList<ExclusionEfectiva>();
        directas.forEach((id, x) -> resultado.add(new ExclusionEfectiva(id, Origen.DIRECTA, x.confirmada(), false,
                null, null, null, null, x.getSourcePlannedWorkoutId(), x.getEventDate())));
        var derivadas = equivalentesDe(userId, base, directas.keySet());
        resultado.addAll(derivadas.values());
        if (!derivadas.isEmpty())
            log.info("DOLOR_FAMILIA usuario {}: variantes equivalentes excluidas {} (variante<-ejercicio que dolio;"
                    + " provisionales {})", userId,
                    derivadas.values().stream().collect(java.util.stream.Collectors.toMap(ExclusionEfectiva::exerciseId,
                            ExclusionEfectiva::derivaDe, (a, b) -> a, TreeMap::new)),
                    derivadas.values().stream().filter(ExclusionEfectiva::provisional)
                            .map(ExclusionEfectiva::exerciseId).toList());
        resultado.sort(Comparator.comparing(ExclusionEfectiva::exerciseId));
        return List.copyOf(resultado);
    }

    /**
     * La familia de un ejercicio de sesion sin identificar sigue excluida mientras ALGUNA de sus filas activas tenga
     * menos de {@link #SEMANAS_FAMILIA_PROVISIONAL} semanas con plan posteriores al dolor. Sin poder medirlo, si
     * (conservador).
     */
    private static boolean familiaProvisionalVigente(List<UserSafetyExclusion> activas, Long exerciseId,
                                                     List<LocalDate> semanas) {
        if (semanas == null) return true;
        return activas.stream().filter(x -> exerciseId.equals(x.getExerciseId()))
                .anyMatch(x -> semanas.stream().filter(w -> w.isAfter(x.getEventDate())).count()
                        < SEMANAS_FAMILIA_PROVISIONAL);
    }

    /**
     * Las variantes equivalentes de cada ejercicio base, cada una con el ejercicio del que deriva. Si deriva de varios,
     * manda el confirmado (permanente) y despues el menor id. No incluye las que tienen su propia exclusion directa.
     */
    private Map<Long, ExclusionEfectiva> equivalentesDe(Long userId, Map<Long, Boolean> base, Set<Long> directos) {
        var resultado = new TreeMap<Long, ExclusionEfectiva>();
        if (catalog == null || base.isEmpty()) return resultado;
        Map<Long, List<ExerciseEquivalence>> equivalentes;
        try {
            equivalentes = catalog.fetchEquivalentVariants(base.keySet());
        } catch (RuntimeException e) {
            // FALLA CERRADO sobre lo directo: sin catalogo se mantiene, como minimo, la exclusion registrada. Se deja
            // constancia: la familia no se pudo calcular y la variante podria seguir siendo elegible en esta lectura.
            log.error("DOLOR_FAMILIA_NO_CALCULADA usuario {} ejercicios {}: {}", userId, base.keySet(), e.toString());
            return resultado;
        }
        base.forEach((original, permanente) -> equivalentes.getOrDefault(original, List.of()).forEach(eq -> {
            if (eq.exerciseId() == null || directos.contains(eq.exerciseId())) return;
            var nueva = new ExclusionEfectiva(eq.exerciseId(), Origen.EQUIVALENTE, permanente, !permanente, original,
                    eq.level(), eq.groupId(), eq.justification(), null, null);
            resultado.merge(eq.exerciseId(), nueva, (a, b) -> a.provisional() && !b.provisional() ? b : a);
        }));
        return resultado;
    }

    @Transactional(readOnly = true)
    public List<UserSafetyExclusion> historial(Long userId) {
        return repository.findByUserIdOrderByIdAsc(userId);
    }

    // =====================================================================
    // Identificar despues (O20)
    // =====================================================================

    /** Lo que la API devuelve tras identificar. */
    public record Identificacion(Long exerciseId, List<Long> exclusionesConfirmadas, OffsetDateTime identificadaEn,
                                 List<Long> equivalentesExcluidos, List<Long> otrosDeLaSesionSiguenExcluidos,
                                 String nota) {}

    /**
     * O20: la persona dice DESPUES cual de los ejercicios de una sesion con dolor sin identificar fue el que dolio. Su
     * exclusion queda CONFIRMADA y con ella, de forma permanente hasta una reactivacion explicita, su familia
     * equivalente. No reactiva nada: los demas ejercicios de esa sesion siguen excluidos (pudieron doler tambien) y
     * solo vuelven con su propia reactivacion explicita.
     *
     * @param plannedWorkoutId la sesion de la que habla; {@code null}: todas sus filas sin identificar.
     */
    @Transactional
    public Identificacion identificar(Long userId, Long exerciseId, Long plannedWorkoutId, boolean confirmacionExplicita) {
        if (!confirmacionExplicita)
            throw new DomainRuleViolationException(
                    "Para indicar que este ejercicio fue el que te dolio confirmalo de forma explicita.");
        var filas = repository.bloquearActivas(userId, exerciseId).stream()
                .filter(x -> !x.confirmada())
                .filter(x -> plannedWorkoutId == null || plannedWorkoutId.equals(x.getSourcePlannedWorkoutId()))
                .toList();
        // Otro usuario, otra sesion o nada pendiente de identificar: no se revela nada.
        if (filas.isEmpty())
            throw new ResourceNotFoundException("Dolor de sesion sin identificar del ejercicio", exerciseId);
        var ahora = OffsetDateTime.now(ZoneOffset.UTC);
        var ids = new ArrayList<Long>();
        var sesiones = new TreeSet<Long>();
        for (var x : filas) {
            x.identificar(ahora, ("IDENTIFICACION_EXPLICITA_DEL_USUARIO user=%d ejercicio=%d sesion=%d"
                    + " via=POST /api/v1/users/me/pain-exclusions/%d/identification en=%s")
                    .formatted(userId, exerciseId, x.getSourcePlannedWorkoutId(), exerciseId, ahora));
            repository.save(x);
            ids.add(x.getId());
            sesiones.add(x.getSourcePlannedWorkoutId());
        }
        var otros = repository.findByUserIdAndStatusOrderByIdAsc(userId, UserSafetyExclusion.Status.ACTIVA).stream()
                .filter(x -> sesiones.contains(x.getSourcePlannedWorkoutId()) && !exerciseId.equals(x.getExerciseId()))
                .map(UserSafetyExclusion::getExerciseId).distinct().sorted().toList();
        var equivalentes = exclusionesEfectivas(userId).stream()
                .filter(x -> x.origen() == Origen.EQUIVALENTE && exerciseId.equals(x.derivaDe()))
                .map(ExclusionEfectiva::exerciseId).toList();
        log.warn("DOLOR_IDENTIFICADO_DESPUES usuario {} ejercicio {} exclusiones {} equivalentes {} siguen_excluidos {}",
                userId, exerciseId, ids, equivalentes, otros);
        return new Identificacion(exerciseId, ids, ahora, equivalentes, otros,
                "Queda confirmado: este ejercicio y sus variantes equivalentes no vuelven a tus planes hasta que pidas"
                + " reactivarlo. Los demas ejercicios de esa sesion siguen excluidos; si no te dolieron, puedes"
                + " pedir que vuelvan a considerarse uno a uno.");
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
        // Bloqueo de fila: dos reactivaciones simultaneas no se pisan (la segunda ya no ve nada activo).
        var activas = repository.bloquearActivas(userId, exerciseId);
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
