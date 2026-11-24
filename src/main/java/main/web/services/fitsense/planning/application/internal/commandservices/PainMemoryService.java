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
 * En la misma accion puede confirmar cuales de los demas no tuvieron que ver: solo esos se levantan.
 * <p>
 * <b>No caduca.</b> Nada aqui mira fechas para levantar una exclusion: ni semanas, ni cumplimiento,
 * ni ausencia de reportes. Solo {@link #reactivar} o la confirmacion de «no relacionado» de
 * {@link #identificar}, por accion explicita del propio usuario y tras un control de seguridad que la
 * confirmacion no puede saltarse. Pasadas {@link #SEMANAS_PARA_PEDIR_CONFIRMACION} semanas con plan, un
 * dolor de sesion sin identificar pide confirmacion ({@code requiereConfirmacion}); no se levanta.
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
     * O20 (cierre final): cuantas semanas con plan, posteriores a un dolor de sesion SIN IDENTIFICAR, pasan antes de
     * PEDIR a la persona una nueva confirmacion (cual fue, o cuales no tuvieron que ver). Al cumplirse NO se levanta
     * nada: los ejercicios de la sesion, sus equivalentes y sus precauciones por angulo siguen excluidos y pasan a
     * {@code requiereConfirmacion}. Se mide en planes, no en dias.
     */
    public static final int SEMANAS_PARA_PEDIR_CONFIRMACION = 2;

    /** De donde viene una exclusion efectiva. */
    public enum Origen {
        /** Registrada para ese ejercicio (marcado con dolor, o de una sesion con dolor sin identificar). */
        DIRECTA,
        /** Derivada: variante equivalente (nivel 1 o lista explicita) de un ejercicio con exclusion directa. */
        EQUIVALENTE,
        /** Derivada: el mismo gesto con otro angulo. No es equivalente, pero no se da por seguro tras dolor. */
        PRECAUCION
    }

    /**
     * Una exclusion EFECTIVA, lo que de verdad se resta del conjunto elegible, con su porque.
     *
     * @param confirmada           DIRECTA: la persona marco ese ejercicio o lo identifico despues. Derivada: deriva
     *                             de uno confirmado.
     * @param provisional          Derivada de un dolor de sesion sin identificar (no confirmado).
     * @param requiereConfirmacion Dolor de sesion sin identificar con {@link #SEMANAS_PARA_PEDIR_CONFIRMACION}
     *                             semanas con plan cumplidas: sigue excluido y se pide a la persona que confirme.
     * @param derivaDe             Derivada: el ejercicio con exclusion directa del que deriva.
     * @param nivel                Derivada: {@code VARIANTE_MISMO_EJERCICIO}, {@code MISMO_GESTO} o
     *                             {@code PRECAUCION_OTRO_ANGULO}.
     */
    public record ExclusionEfectiva(Long exerciseId, Origen origen, boolean confirmada, boolean provisional,
                                    boolean requiereConfirmacion, Long derivaDe, String nivel, String grupo,
                                    String justificacion, Long sourcePlannedWorkoutId, LocalDate eventDate) {}

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
     * O20 (opcion A, ajuste y cierre final del 24/11/2026). Lo excluido hoy y por que:
     * <ul>
     *   <li><b>DIRECTA:</b> todo ejercicio con una exclusion ACTIVA, confirmada o de una sesion con dolor sin
     *   identificar (la regla de V8C: si no se sabe cual dolio, cualquiera pudo ser). Hasta su reactivacion
     *   explicita, como siempre.</li>
     *   <li><b>EQUIVALENTE y PRECAUCION</b> ({@link main.web.services.fitsense.catalog.domain.services
     *   .FamiliaDeMovimientoEquivalente}): las variantes equivalentes y el mismo gesto con otro angulo de cada ejercicio
     *   con exclusion directa. Duran lo que la exclusion de la que derivan: se recalculan en cada lectura y no se
     *   guardan. Las de un ejercicio CONFIRMADO son permanentes; las de un dolor de sesion SIN IDENTIFICAR son
     *   provisionales, pero NO caducan solas: tras {@link #SEMANAS_PARA_PEDIR_CONFIRMACION} semanas con plan pasan a
     *   {@code requiereConfirmacion} y solo las levanta la persona (identificando el ejercicio y diciendo cuales no
     *   tuvieron que ver, {@link #identificar}, o reactivando).</li>
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
        var base = new TreeMap<Long, Boolean>(); // ejercicio directo -> confirmado (permanente)
        var pedir = new TreeSet<Long>();         // sin identificar, con la confirmacion ya pedida
        directas.forEach((id, x) -> {
            base.put(id, x.confirmada());
            if (!x.confirmada() && confirmacionVencida(activas, id, semanas)) pedir.add(id);
        });
        var resultado = new ArrayList<ExclusionEfectiva>();
        directas.forEach((id, x) -> resultado.add(new ExclusionEfectiva(id, Origen.DIRECTA, x.confirmada(), false,
                pedir.contains(id), null, null, null, null, x.getSourcePlannedWorkoutId(), x.getEventDate())));
        var derivadas = equivalentesDe(userId, base, pedir, directas.keySet());
        resultado.addAll(derivadas.values());
        if (!derivadas.isEmpty())
            log.info("DOLOR_FAMILIA usuario {}: variantes excluidas {} (variante<-ejercicio que dolio; provisionales {};"
                    + " por angulo {}; confirmacion pedida {})", userId,
                    derivadas.values().stream().collect(java.util.stream.Collectors.toMap(ExclusionEfectiva::exerciseId,
                            ExclusionEfectiva::derivaDe, (a, b) -> a, TreeMap::new)),
                    derivadas.values().stream().filter(ExclusionEfectiva::provisional)
                            .map(ExclusionEfectiva::exerciseId).toList(),
                    derivadas.values().stream().filter(x -> x.origen() == Origen.PRECAUCION)
                            .map(ExclusionEfectiva::exerciseId).toList(), pedir);
        resultado.sort(Comparator.comparing(ExclusionEfectiva::exerciseId));
        return List.copyOf(resultado);
    }

    /**
     * Un ejercicio de sesion sin identificar REQUIERE CONFIRMACION cuando todas sus filas activas tienen ya
     * {@link #SEMANAS_PARA_PEDIR_CONFIRMACION} semanas con plan posteriores al dolor. Sin poder medirlo, no se pide
     * todavia (la exclusion sigue igual en los dos casos).
     */
    private static boolean confirmacionVencida(List<UserSafetyExclusion> activas, Long exerciseId,
                                               List<LocalDate> semanas) {
        if (semanas == null) return false;
        return activas.stream().filter(x -> exerciseId.equals(x.getExerciseId()) && !x.confirmada())
                .allMatch(x -> semanas.stream().filter(w -> w.isAfter(x.getEventDate())).count()
                        >= SEMANAS_PARA_PEDIR_CONFIRMACION);
    }

    /**
     * Las variantes equivalentes de cada ejercicio base, cada una con el ejercicio del que deriva. Si deriva de varios,
     * manda el confirmado (permanente) y despues el menor id. No incluye las que tienen su propia exclusion directa.
     */
    private Map<Long, ExclusionEfectiva> equivalentesDe(Long userId, Map<Long, Boolean> base, Set<Long> pedir,
                                                       Set<Long> directos) {
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
            var origen = main.web.services.fitsense.catalog.domain.services.FamiliaDeMovimientoEquivalente
                    .NIVEL_PRECAUCION_ANGULO.equals(eq.level()) ? Origen.PRECAUCION : Origen.EQUIVALENTE;
            var nueva = new ExclusionEfectiva(eq.exerciseId(), origen, permanente, !permanente,
                    !permanente && pedir.contains(original), original, eq.level(), eq.groupId(), eq.justification(),
                    null, null);
            resultado.merge(eq.exerciseId(), nueva, PainMemoryService::prioridad);
        }));
        return resultado;
    }

    /** Si una variante deriva de varios: manda la permanente; despues EQUIVALENTE sobre PRECAUCION; despues la primera. */
    private static ExclusionEfectiva prioridad(ExclusionEfectiva a, ExclusionEfectiva b) {
        if (a.provisional() != b.provisional()) return a.provisional() ? b : a;
        if (a.origen() != b.origen()) return a.origen() == Origen.EQUIVALENTE ? a : b;
        return a;
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
                                 List<Long> equivalentesExcluidos, List<Long> noRelacionadosLevantados,
                                 Map<Long, String> noRelacionadosNoLevantados,
                                 List<Long> otrosDeLaSesionSiguenExcluidos, String nota) {}

    /** Compatibilidad: identificar sin declarar ningun ejercicio como no relacionado. */
    @Transactional
    public Identificacion identificar(Long userId, Long exerciseId, Long plannedWorkoutId, boolean confirmacionExplicita) {
        return identificar(userId, exerciseId, plannedWorkoutId, confirmacionExplicita, Set.of());
    }

    /**
     * O20: la persona dice DESPUES cual de los ejercicios de una sesion con dolor sin identificar fue el que dolio. Su
     * exclusion queda CONFIRMADA y con ella, de forma permanente hasta una reactivacion explicita, sus equivalentes y
     * sus precauciones por angulo.
     * <p>
     * Cierre final: en la misma accion puede confirmar cuales de los DEMAS de esa sesion no tuvieron que ver
     * ({@code noRelacionados}). Solo esos se levantan, cada uno tras el mismo control de seguridad que la
     * reactivacion; si choca con una restriccion vigente, no se levanta y se dice por que. Un no relacionado que es
     * equivalente del confirmado o precaucion suya se levanta como exclusion propia, pero SIGUE fuera por derivar del
     * confirmado. Los que no se nombran siguen excluidos: nada se reactiva sin esa confirmacion.
     *
     * @param plannedWorkoutId la sesion de la que habla; {@code null}: todas sus filas sin identificar.
     * @param noRelacionados   ejercicios de esa sesion que la persona confirma que no tuvieron que ver.
     */
    @Transactional
    public Identificacion identificar(Long userId, Long exerciseId, Long plannedWorkoutId, boolean confirmacionExplicita,
                                      Set<Long> noRelacionados) {
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
        var sesiones = new TreeSet<Long>();
        filas.forEach(x -> sesiones.add(x.getSourcePlannedWorkoutId()));
        var pedidos = noRelacionados == null ? Set.<Long>of() : new TreeSet<>(noRelacionados);
        if (pedidos.contains(exerciseId))
            throw new DomainRuleViolationException("El ejercicio que te dolio no puede ser a la vez no relacionado.");
        // Antes de cambiar nada: cada no relacionado tiene que ser de ESA sesion y seguir sin identificar.
        var porLevantar = new TreeMap<Long, List<UserSafetyExclusion>>();
        for (var id : pedidos) {
            var suyas = repository.bloquearActivas(userId, id).stream()
                    .filter(x -> !x.confirmada() && sesiones.contains(x.getSourcePlannedWorkoutId())).toList();
            if (suyas.isEmpty())
                throw new DomainRuleViolationException(("El ejercicio %d no esta pendiente de confirmar en esa sesion:"
                        + " solo se pueden declarar no relacionados los demas ejercicios de la misma sesion.").formatted(id));
            porLevantar.put(id, suyas);
        }
        var ahora = OffsetDateTime.now(ZoneOffset.UTC);
        var ids = new ArrayList<Long>();
        for (var x : filas) {
            x.identificar(ahora, ("IDENTIFICACION_EXPLICITA_DEL_USUARIO user=%d ejercicio=%d sesion=%d"
                    + " via=POST /api/v1/users/me/pain-exclusions/%d/identification en=%s")
                    .formatted(userId, exerciseId, x.getSourcePlannedWorkoutId(), exerciseId, ahora));
            repository.save(x);
            ids.add(x.getId());
        }
        // No relacionados: el mismo control de seguridad que la reactivacion.
        var levantados = new ArrayList<Long>();
        var noLevantados = new TreeMap<Long, String>();
        if (!porLevantar.isEmpty()) {
            var perfil = profiling.fetchProfile(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Perfil del usuario", userId));
            var seguridad = SafetyProfile.forAgeLevelAndDeclared(perfil.age(), perfil.fitnessLevel(),
                    perfil.declaradas());
            var conflicto = conflictos.motivos(List.copyOf(porLevantar.keySet()), perfil, seguridad);
            porLevantar.forEach((id, suyas) -> {
                if (conflicto.containsKey(id)) {
                    noLevantados.put(id, "choca con una restriccion de seguridad vigente (" + conflicto.get(id) + ")");
                    return;
                }
                var evidencia = ("CONFIRMACION_EXPLICITA_NO_RELACIONADO user=%d ejercicio=%d identificado=%d sesion=%s"
                        + " via=POST /api/v1/users/me/pain-exclusions/%d/identification en=%s; control de seguridad sin"
                        + " conflicto").formatted(userId, id, exerciseId, sesiones, exerciseId, ahora);
                for (var x : suyas) {
                    x.reactivar(ahora, "no relacionado con el dolor de la sesion (identificado: " + exerciseId + ")",
                            evidencia);
                    repository.save(x);
                }
                levantados.add(id);
            });
        }
        var efectivas = exclusionesEfectivas(userId);
        var derivadasDelConfirmado = efectivas.stream().filter(x -> x.origen() != Origen.DIRECTA
                && exerciseId.equals(x.derivaDe())).map(ExclusionEfectiva::exerciseId).toList();
        for (var id : levantados)
            if (derivadasDelConfirmado.contains(id))
                noLevantados.put(id, "su exclusion propia se levanto, pero sigue fuera: es equivalente o precaucion"
                        + " por angulo del ejercicio que te dolio");
        levantados.removeIf(noLevantados::containsKey);
        var otros = repository.findByUserIdAndStatusOrderByIdAsc(userId, UserSafetyExclusion.Status.ACTIVA).stream()
                .filter(x -> sesiones.contains(x.getSourcePlannedWorkoutId()) && !exerciseId.equals(x.getExerciseId()))
                .map(UserSafetyExclusion::getExerciseId).distinct().sorted().toList();
        log.warn("DOLOR_IDENTIFICADO_DESPUES usuario {} ejercicio {} exclusiones {} derivadas {} no_relacionados_levantados"
                + " {} no_levantados {} siguen_excluidos {}", userId, exerciseId, ids, derivadasDelConfirmado, levantados,
                noLevantados.keySet(), otros);
        return new Identificacion(exerciseId, ids, ahora, derivadasDelConfirmado, List.copyOf(levantados),
                java.util.Collections.unmodifiableMap(noLevantados), otros,
                "Queda confirmado: este ejercicio, sus variantes equivalentes y el mismo movimiento con otro angulo no"
                + " vuelven a tus planes hasta que pidas reactivarlo. Los ejercicios que confirmaste como no"
                + " relacionados vuelven a considerarse; los demas de esa sesion siguen excluidos hasta que lo"
                + " confirmes.");
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
