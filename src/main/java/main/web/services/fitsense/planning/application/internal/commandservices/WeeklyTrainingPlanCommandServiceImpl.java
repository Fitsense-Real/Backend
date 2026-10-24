package main.web.services.fitsense.planning.application.internal.commandservices;

import main.web.services.fitsense.planning.application.internal.outboundservices.acl.*;
import main.web.services.fitsense.planning.domain.exceptions.*;
import main.web.services.fitsense.planning.domain.model.aggregates.PlanAdaptationAttempt;
import main.web.services.fitsense.planning.domain.model.aggregates.WeeklyTrainingPlan;
import main.web.services.fitsense.planning.domain.model.entities.PlannedWorkout;
import main.web.services.fitsense.planning.domain.model.commands.*;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.AdaptationEvidenceMapper;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.generation.ai.PlanInputSnapshot;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.PlanAdaptationAttemptRepository;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.PlannedWorkoutRepository;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.WeeklyTrainingPlanRepository;
import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;
import main.web.services.fitsense.shared.domain.exceptions.ResourceNotFoundException;
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.List;

@Service
public class WeeklyTrainingPlanCommandServiceImpl implements WeeklyTrainingPlanCommandService {

    private static final Logger log = LoggerFactory.getLogger(WeeklyTrainingPlanCommandServiceImpl.class);

    private final WeeklyTrainingPlanRepository planRepository;
    private final PlannedWorkoutRepository plannedWorkoutRepository;
    private final PlanAdaptationAttemptRepository attemptRepository;
    private final AdaptationEvidenceMapper evidenceMapper;
    private final PlanGenerationPipeline pipeline;
    private final PreviousWeekAssembler previousWeekAssembler;
    private final ExternalIamService externalIamService;
    private final ExternalProfilingService externalProfilingService;
    private final ExternalCatalogService externalCatalogService;
    private final ExternalConfigurationService externalConfigurationService;
    private final JsonSupport jsonSupport;

    /**
     * Que hacer con el veredicto de cobertura 4C antes de generar. Propiedad y
     * no despliegue: la cuenta entra siempre, bloquear es otra decision.
     */
    private final main.web.services.fitsense.planning.domain.services.SlotCoverageEnforcement
            coveragePregeneration;

    public WeeklyTrainingPlanCommandServiceImpl(WeeklyTrainingPlanRepository planRepository,
                                                PlannedWorkoutRepository plannedWorkoutRepository,
                                                PlanAdaptationAttemptRepository attemptRepository,
                                                AdaptationEvidenceMapper evidenceMapper,
                                                PlanGenerationPipeline pipeline,
                                                PreviousWeekAssembler previousWeekAssembler,
                                                ExternalIamService externalIamService,
                                                ExternalProfilingService externalProfilingService,
                                                ExternalCatalogService externalCatalogService,
                                                ExternalConfigurationService externalConfigurationService,
                                                JsonSupport jsonSupport,
                                                @org.springframework.beans.factory.annotation.Value(
                                                        "${fitsense.coverage.pregeneration:OBSERVE}")
                                                String coveragePregeneration) {
        this.coveragePregeneration = main.web.services.fitsense.planning.domain.services
                .SlotCoverageEnforcement.parse(coveragePregeneration);
        this.planRepository = planRepository;
        this.plannedWorkoutRepository = plannedWorkoutRepository;
        this.attemptRepository = attemptRepository;
        this.evidenceMapper = evidenceMapper;
        this.pipeline = pipeline;
        this.previousWeekAssembler = previousWeekAssembler;
        this.externalIamService = externalIamService;
        this.externalProfilingService = externalProfilingService;
        this.externalCatalogService = externalCatalogService;
        this.externalConfigurationService = externalConfigurationService;
        this.jsonSupport = jsonSupport;
    }

    /**
     * FASE 1. Todo lo que hay que leer para poder generar: elegibilidad, perfil,
     * conjunto de ejercicios, semana previa y configuracion. Son consultas:
     * milisegundos.
     */
    @Override
    @Transactional(readOnly = true)
    public PreparedPlanGeneration prepare(GenerateWeeklyPlanCommand command) {
        if (!externalIamService.isEligibleForPlanGeneration(command.userId()))
            throw new DomainRuleViolationException(
                    "El usuario %d no esta activo en el estudio: no se le generan planes."
                            .formatted(command.userId()));

        var profile = externalProfilingService.fetchProfile(command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Perfil del usuario", command.userId()));

        var week = TrainingWeek.containing(command.weekStartDate());
        var previousActive = planRepository.findByUserIdAndWeekStartDateAndStatus(
                command.userId(), week.startDate(), PlanStatus.ACTIVE);

        if (previousActive.isPresent() && !command.replaceExisting())
            throw new PlanAlreadyExistsException(command.userId(), week.startDate());

        int divisor = externalConfigurationService.durationToRepsDivisor();
        var adjustment = command.adjustment() == null ? PlanAdjustment.none() : command.adjustment();

        // LOWER_DIFFICULTY baja el filtro un nivel antes de pedir el conjunto
        // elegible: es un cambio de QUE ejercicios existen, no de como se
        // prescriben, asi que debe aplicarse en la consulta al catalogo.
        int maxDifficulty = adjustment.maxDifficultyLevel() != null
                ? Math.max(1, adjustment.maxDifficultyLevel())
                : profile.maxDifficultyLevel();

        // Restricciones por edad y nivel MAS las que el participante declaro.
        // Se aplican EN LA CONSULTA: la IA no llega a ver los ejercicios que no
        // le corresponden, asi que no puede proponerlos aunque ignore las
        // instrucciones del prompt.
        //
        // INC-4C-03: hasta el 16/10 esta linea llamaba a forAgeAndLevel, que
        // ignora lo declarado y pasa false literal en arrodillarse, suspension
        // y por encima de la cabeza. Las cinco columnas allow_* existian desde
        // V25 y no las leia nadie: toda restriccion que el participante
        // declarara se perdia antes de llegar al filtro.
        var safety = SafetyProfile.forAgeLevelAndDeclared(
                profile.age(), profile.fitnessLevel(), profile.declaradas());

        var eligible = externalCatalogService.fetchEligibleFor(profile, maxDifficulty, safety);
        if (eligible.isEmpty())
            throw new main.web.services.fitsense.catalog.domain.exceptions.EmptyEligibleSetException(
                    command.userId());

        // COMPROBACION PREVIA DE COBERTURA 4C.
        //
        // Va AQUI y no antes: se valora el conjunto que de verdad le queda al
        // participante, despues de equipamiento, dificultad efectiva, bloqueos
        // y las restricciones de SafetyProfile. Contarlo sobre el catalogo
        // entero mediria un conjunto que nadie va a recibir.
        //
        // Es lo unico que el Step 4 aporta y el backend no tenia: decir
        // "no factible" ANTES de gastar tres intentos de IA mas el motor de
        // reglas. No cambia la dosis ni toca el conjunto elegible.
        //
        // La decision vive en SlotCoverageGate, que es dominio puro: asi se
        // prueba sin Spring, sin base y sin Replicate.
        main.web.services.fitsense.planning.domain.services.SlotCoverageGate
                .evaluateBeforeGeneration(command.userId(), eligible, coveragePregeneration);

        short weekNumber = resolveWeekNumber(command.userId(), previousActive.orElse(null));
        var previousWeek = previousWeekAssembler.assemble(
                command.userId(), week.previous().startDate(), divisor,
                command.previousWeekAdherencePct(), command.previousWeekAverageRpe());

        var context = new PlanGenerationContext(command.userId(), weekNumber,
                week.startDate(), week.endDate(), profile, adjustment, previousWeek,
                eligible, safety, externalConfigurationService.prescriptionParams());

        return new PreparedPlanGeneration(command, context, adjustment,
                week.startDate(), week.endDate(), divisor, weekNumber,
                previousActive.map(WeeklyTrainingPlan::getId).orElse(null),
                externalIamService.timezoneOf(command.userId()));
    }

    /**
     * FASE 2. SIN @Transactional, y no es un olvido.
     * <p>
     * pipeline.run hace hasta dos llamadas HTTP a Replicate de ~90 s cada una y
     * no toca la base de datos. Dentro de una transaccion, esos minutos cuentan
     * como idle-in-transaction: Postgres termina la conexion y se pierde todo lo
     * escrito antes en ella, incluidos el cierre de semana y las metricas. Con
     * 30 participantes agota ademas el pool de HikariCP, que por defecto son 10
     * conexiones, y la tarea semanal falla en cascada.
     * <p>
     * Que el metodo viva aqui y no dentro del pipeline es deliberado: asi la
     * frontera transaccional de la generacion queda visible en un solo archivo.
     */
    @Override
    public GeneratedPlan generate(PreparedPlanGeneration prepared) {
        if (feasibilityObserver != null) feasibilityObserver.antesDeGenerar(prepared.context());
        var result = pipeline.run(prepared.context(), prepared.divisor());
        if (feasibilityObserver != null)
            feasibilityObserver.despuesDeGenerar(prepared.context(), result.draft());
        return new GeneratedPlan(result.draft(), result.attempts(), result.cobertura());
    }

    /**
     * Paso 3 · factibilidad por objetivo, SOLO OBSERVACION y solo en la
     * generacion inicial: el camino de adaptacion no pasa por generate().
     * <p>
     * Por setter y opcional, no por constructor, para no tocar las pruebas que
     * construyen este servicio a mano (P2-D): ahi queda en null y no observa.
     */
    private GoalFeasibilityObserver feasibilityObserver;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setFeasibilityObserver(GoalFeasibilityObserver feasibilityObserver) {
        this.feasibilityObserver = feasibilityObserver;
    }

    /**
     * FASE 3. Materializa el borrador y lo guarda. Transaccional y corta.
     */
    @Override
    @Transactional
    public Optional<WeeklyTrainingPlan> persist(PreparedPlanGeneration prepared,
                                                GeneratedPlan generated) {
        var command = prepared.command();
        var draft = generated.draft();
        var adjustment = prepared.adjustment();

        // 4A v6: una semana PARCIAL es una alternativa que requiere aceptacion.
        // No se guarda ni se activa; se devuelve la propuesta con sus motivos.
        if (generated.cobertura() != null && generated.cobertura().esParcial())
            throw new PartialPlanRequiresAcceptanceException(command.userId(), generated);

        // Se relee el plan previo en vez de arrastrar la entidad de la fase 1:
        // entre fase y fase pasaron ~90 s sin sesion de persistencia abierta. Y
        // se vuelve a comprobar la precondicion, porque en ese hueco pudo
        // aparecer un plan activo que en la fase 1 no existia.
        var previousActive = planRepository.findByUserIdAndWeekStartDateAndStatus(
                command.userId(), prepared.weekStartDate(), PlanStatus.ACTIVE);

        if (previousActive.isPresent() && !command.replaceExisting())
            throw new PlanAlreadyExistsException(command.userId(), prepared.weekStartDate());

        var snapshotJson = jsonSupport.write(PlanInputSnapshot.of(prepared.context()));

        var plan = previousActive
                .map(previous -> WeeklyTrainingPlan.nextVersionOf(previous, draft.source(),
                        draft.modelName(), snapshotJson, generated.attempts(),
                        adjustment.types().get(0).name(), draft.rationale()))
                .orElseGet(() -> WeeklyTrainingPlan.firstVersion(command.userId(),
                        prepared.weekNumber(), prepared.weekStartDate(), prepared.weekEndDate(),
                        draft.source(), draft.modelName(), snapshotJson, generated.attempts()));

        // firstVersion no recibe el ajuste porque una primera version normalmente
        // no lo tiene. Pero la semana nueva del lunes SI llega con orden y sin
        // plan previo que reemplazar, asi que entraba por esa rama y los dos
        // campos quedaban en null aunque el ajuste se hubiera aplicado.
        //
        // El dato tambien esta en user_interventions, pero tenerlo aqui permite
        // filtrar planes por tipo de ajuste sin un JOIN.
        if (adjustment.isActive())
            plan.recordAdjustment(adjustment.types().get(0).name(), draft.rationale());

        materialize(plan, draft, prepared.zone());
        // GEN-OUT-1.1: el borrador y, junto a el, la cobertura de capacidades
        // (4A v6): si la semana es COMPLETA o PARCIAL y que principal falta. Es
        // la constancia de que un plan parcial no se publico como completo.
        plan.attachOutputSnapshot(generated.cobertura() == null ? jsonSupport.write(draft)
                : jsonSupport.write(new java.util.LinkedHashMap<>(java.util.Map.of(
                "schema_version", "GEN-OUT-1.1",
                "draft", draft,
                "capability_coverage", generated.cobertura()))));

        // Reemplazar y vaciar el contexto ANTES de guardar el nuevo:
        // ux_plan_active solo admite un ACTIVE por usuario y semana.
        previousActive.ifPresent(previous -> {
            previous.markReplaced();
            planRepository.saveAndFlush(previous);
        });

        var saved = planRepository.save(plan);
        log.info("Plan {} generado para el usuario {} por {} en {} intento(s)",
                saved.getId(), command.userId(), draft.source(), generated.attempts());
        return Optional.of(saved);
    }

    /**
     * FASE 3 DE LA ADAPTACION. La semana adaptada y su evidencia, en una sola
     * transaccion: si el plan no se publica, no queda constancia de que se
     * publicara.
     */
    @Override
    @Transactional
    public Optional<WeeklyTrainingPlan> publishAdapted(PreparedPlanGeneration prepared,
                                                       AdaptedPublication publicacion) {
        var resultado = publicacion.resultado();

        // Un NO_FACTIBLE aqui seria publicar lo que el productor rechazo.
        if (!resultado.publicable())
            throw new DomainRuleViolationException(
                    ("El productor devolvio %s y no hay semana que publicar. La evidencia de"
                            + " un fallo va por el registrador, en su propia transaccion.")
                            .formatted(resultado.estado()));

        var command = prepared.command();

        // Se relee el plan previo, por lo mismo que en persist: entre fases
        // pasaron minutos sin sesion de persistencia abierta.
        var previousActive = planRepository.findByUserIdAndWeekStartDateAndStatus(
                command.userId(), prepared.weekStartDate(), PlanStatus.ACTIVE);

        if (previousActive.isPresent() && !command.replaceExisting())
            throw new PlanAlreadyExistsException(command.userId(), prepared.weekStartDate());

        var adjustment = prepared.adjustment();
        var etiqueta = publicacion.etiquetaDeAjuste();
        var motivo = publicacion.motivoDePublicacion(adjustment);

        // REEMPLAZAR UN PLAN YA PUBLICADO EXIGE DECIR POR QUE.
        // nextVersionOf rechaza un motivo vacio, y con NO_CHANGE o
        // INSUFFICIENT_DATA la orden no trae ninguno: PlanAdjustment.none()
        // deja reason en null. Se exige declararlo en vez de inventarlo.
        if (previousActive.isPresent() && (motivo == null || motivo.isBlank()))
            throw new DomainRuleViolationException(
                    ("Hay un plan activo del %s y la decision %s no trae motivo. Reemplazar"
                            + " una version ya publicada exige declarar por que, en"
                            + " replacementReason.")
                            .formatted(prepared.weekStartDate(), publicacion.decision()));

        // UN SOLO VALOR DE ORIGEN para el plan y para la evidencia. El
        // disparador de V32 compara los dos y rechaza el intento si difieren.
        var source = resultado.source();

        // generation_attempts cuenta los intentos de PRODUCIR la semana, y
        // ck_plan_attempts (V4) exige al menos uno. Una continuidad sin IA
        // -NO_CHANGE, INSUFFICIENT_DATA, respaldo con la IA apagada o el
        // cortacircuitos abierto- es un intento determinista: con aiCalls = 0
        // la publicacion fallaba y la semana quedaba sin plan y sin evidencia.
        // ai_calls de la evidencia V32 sigue siendo el numero REAL de llamadas.
        short generationAttempts = (short) Math.max(1, publicacion.aiCalls());

        var plan = previousActive
                .map(previous -> WeeklyTrainingPlan.nextVersionOf(previous, source,
                        publicacion.modelName(), publicacion.inputSnapshotJson(),
                        generationAttempts, etiqueta, motivo))
                .orElseGet(() -> WeeklyTrainingPlan.firstVersion(command.userId(),
                        prepared.weekNumber(), prepared.weekStartDate(), prepared.weekEndDate(),
                        source, publicacion.modelName(), publicacion.inputSnapshotJson(),
                        generationAttempts));

        // POR ordenaAdaptar(), NO POR isActive(). isActive() mira targetVolume,
        // que en una orden localizada es null, asi que una adaptacion se
        // publicaba con adjustment_applied en null. Y la etiqueta sale de la
        // decision, no de types().get(0), que pertenece al mecanismo porcentual.
        if (publicacion.decision().ordenaAdaptar())
            plan.recordAdjustment(etiqueta, motivo);

        // La sobrecarga que conserva el linaje: linkSource por sesion.
        materialize(plan, resultado.semana(), prepared.zone());

        // Se etiqueta el formato para distinguirlo del snapshot de generacion
        // inicial, que es un PlanDraft y no lleva el origen de cada sesion.
        plan.attachOutputSnapshot(jsonSupport.write(java.util.Map.of(
                "schema_version", "ADAPTED_WEEK_V1",
                "derived_workouts", resultado.semana())));

        previousActive.ifPresent(previous -> {
            previous.markReplaced();
            planRepository.saveAndFlush(previous);
        });

        // saveAndFlush, no save: la evidencia referencia el plan por clave
        // ajena (fk_paa_result) y esa comprobacion NO es diferida, asi que la
        // fila del plan tiene que existir antes de insertar el intento.
        var saved = planRepository.saveAndFlush(plan);

        var intento = evidenceMapper.map(resultado, adjustment, publicacion.decision(),
                new AdaptationEvidenceMapper.PublicationFacts(
                        command.userId(), publicacion.weeklyMetricId(),
                        publicacion.sourcePlanId(), prepared.weekStartDate(),
                        saved.getId(), publicacion.aiCalls(),
                        publicacion.observedAdherencePct(),
                        publicacion.registrationCoveragePct(),
                        jsonSupport.write(resultado.conservacion())));

        attemptRepository.save(PlanAdaptationAttempt.desde(intento));

        log.info("Semana adaptada {} publicada para el usuario {} · origen {} · motivo {} · {}",
                saved.getId(), command.userId(), source, resultado.motivo(),
                resultado.conservacion().describe());
        return Optional.of(saved);
    }

    @Override
    @Transactional
    public void handle(StartPlannedWorkoutCommand command) {
        var workout = requireWorkout(command.plannedWorkoutId());
        workout.markInProgress();
        plannedWorkoutRepository.save(workout);
    }

    @Override
    @Transactional
    public void handle(RecordWorkoutOutcomeCommand command) {
        var workout = requireWorkout(command.plannedWorkoutId());
        workout.markOutcome(command.outcome(), command.skipReason());
        plannedWorkoutRepository.save(workout);
    }

    @Override
    @Transactional
    public void handle(SkipPlannedWorkoutCommand command) {
        var workout = requireWorkout(command.plannedWorkoutId());

        if (!workout.getPlan().getUserId().equals(command.userId()))
            throw new PlannedWorkoutNotFoundException(command.plannedWorkoutId());
        if (command.reason() == null)
            throw new DomainRuleViolationException(
                    "Indica por que no vas a hacer este entrenamiento: sin la causa el sistema no puede ajustar.");

        workout.markSkipped(command.reason());
        plannedWorkoutRepository.save(workout);
    }

    @Override
    @Transactional
    public void handle(ClosePlanWeekCommand command) {
        planRepository.findByUserIdAndWeekStartDateAndStatus(
                        command.userId(), command.weekStartDate(), PlanStatus.ACTIVE)
                .ifPresent(plan -> {
                    plan.markCompleted();
                    planRepository.save(plan);
                });
    }

    /**
     * Tarea diaria de 00:30. El skip_reason queda NULL a proposito: se le
     * pregunta al usuario despues. Rellenarlo con OTHER seria inventar un dato
     * que luego alimenta el modificador de 18.3.
     */
    @Override
    @Transactional
    public int handle(ExpireOverdueWorkoutsCommand command) {
        var overdue = plannedWorkoutRepository.findOverdue(command.now());
        overdue.forEach(workout -> workout.markOutcome(WorkoutStatus.SKIPPED, null));
        plannedWorkoutRepository.saveAll(overdue);
        if (!overdue.isEmpty())
            log.info("Marcados {} entrenamientos vencidos como SKIPPED", overdue.size());
        return overdue.size();
    }

    // ------------------------------------------------------------------ helpers

    private main.web.services.fitsense.planning.domain.model.entities.PlannedWorkout requireWorkout(
            Long plannedWorkoutId) {
        return plannedWorkoutRepository.findById(plannedWorkoutId)
                .orElseThrow(() -> new PlannedWorkoutNotFoundException(plannedWorkoutId));
    }

    /**
     * week_number es la semana del participante en el estudio, no del calendario:
     * empieza en 1 con su primer plan. Un reemplazo conserva el numero, porque
     * sigue siendo la misma semana.
     */
    private short resolveWeekNumber(Long userId, WeeklyTrainingPlan previousActive) {
        if (previousActive != null) return previousActive.getWeekNumber();
        return planRepository.findFirstByUserIdOrderByWeekNumberDescPlanVersionDesc(userId)
                .map(plan -> (short) (plan.getWeekNumber() + 1))
                .orElse((short) 1);
    }

    private void materialize(WeeklyTrainingPlan plan, PlanDraft draft, ZoneId zone) {
        for (var workout : draft.workouts()) {
            materializar(plan, workout, zone);
        }
    }

    /**
     * V31: crea cada sesion adaptada junto con su ID de origen.
     * No aplanar a PlanDraft porque se perderia sourcePlannedWorkoutId.
     * Esta sobrecarga queda inactiva hasta conectar P2-B-2c/P2-B-3.
     */
    private void materialize(WeeklyTrainingPlan plan, List<DerivedWorkout> semana, ZoneId zone) {
        for (var derivada : semana) {
            materializar(plan, derivada.workout(), zone)
                    .linkSource(derivada.sourcePlannedWorkoutId());
        }
    }

    /**
     * Un solo camino para las prescripciones de generacion inicial y adaptacion.
     */
    private PlannedWorkout materializar(WeeklyTrainingPlan plan,
                                        PlanDraft.DraftWorkout draftWorkout,
                                        ZoneId zone) {
        // expires_at es 23:59 hora LOCAL del usuario: es lo que define que
        // entrenamientos siguen siendo exigibles.
        var expiresAt = draftWorkout.scheduledDate()
                .atTime(LocalTime.of(23, 59))
                .atZone(zone)
                .toOffsetDateTime();

        var workout = plan.addWorkout(draftWorkout.scheduledDate(), draftWorkout.focus(),
                draftWorkout.name(), draftWorkout.expectedDurationMinutes(), expiresAt);

        for (var draftExercise : draftWorkout.exercises()) {
            workout.addExercise(draftExercise.exerciseId(), draftExercise.prescriptionType(),
                    draftExercise.plannedSets(), draftExercise.plannedReps(),
                    draftExercise.plannedDurationSeconds(), draftExercise.targetLoadKg(),
                    draftExercise.restSeconds(), draftExercise.notes());
        }
        return workout;
    }
}
