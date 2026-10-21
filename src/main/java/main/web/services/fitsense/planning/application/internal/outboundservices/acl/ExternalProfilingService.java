package main.web.services.fitsense.planning.application.internal.outboundservices.acl;

import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningProfile;
import main.web.services.fitsense.profiling.interfaces.acl.ProfilingContextFacade;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Capa anticorrupcion hacia profiling. Traduce ProfileSnapshot al lenguaje de
 * planning: si manana el perfil cambia de forma, el cambio muere aqui.
 */
@Service("planningExternalProfilingService")
public class ExternalProfilingService {

    private final ProfilingContextFacade profilingContextFacade;

    public ExternalProfilingService(ProfilingContextFacade profilingContextFacade) {
        this.profilingContextFacade = profilingContextFacade;
    }

    public Optional<PlanningProfile> fetchProfile(Long userId) {
        return profilingContextFacade.fetchProfile(userId).map(snapshot -> new PlanningProfile(
                snapshot.userId(),
                snapshot.age(),
                snapshot.biologicalSex(),
                snapshot.heightCm(),
                snapshot.currentWeightKg(),
                snapshot.targetWeightKg(),
                snapshot.fitnessLevel(),
                snapshot.goalType(),
                snapshot.goalText(),
                snapshot.healthNotes(),
                snapshot.trainingLocation(),
                snapshot.daysPerWeek(),
                snapshot.availableDays(),
                snapshot.sessionMinutes(),
                snapshot.equipmentCodes(),
                snapshot.blockedExerciseIds(),
                // El ACL traduce: profiling habla de "permisos del perfil" y
                // planning de "lo declarado" para SafetyProfile. Son el mismo
                // dato con el nombre de cada contexto.
                new main.web.services.fitsense.planning.domain.model.valueobjects
                        .SafetyProfile.Declaradas(
                        snapshot.allowances().allowHighImpact(),
                        snapshot.allowances().allowFloorExercises(),
                        snapshot.allowances().allowKneelingExercises(),
                        snapshot.allowances().allowHangingExercises(),
                        snapshot.allowances().allowOverheadExercises())
        ));
    }
}
