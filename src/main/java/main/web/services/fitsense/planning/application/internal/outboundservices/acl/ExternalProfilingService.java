package main.web.services.fitsense.planning.application.internal.outboundservices.acl;

import main.web.services.fitsense.planning.domain.model.valueobjects.ModoApoyoSuspendido;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningProfile;
import main.web.services.fitsense.profiling.interfaces.acl.ProfilingContextFacade;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Capa anticorrupcion hacia profiling. Traduce ProfileSnapshot al lenguaje de
 * planning: si manana el perfil cambia de forma, el cambio muere aqui.
 */
@Service("planningExternalProfilingService")
public class ExternalProfilingService {

    private final ProfilingContextFacade profilingContextFacade;
    /**
     * V53-B · que restriccion excluye el apoyo suspendido. Por defecto {@link ModoApoyoSuspendido#APAGADO}: el permiso
     * propio del perfil (V55) no se traduce y todo queda como antes. Con los dos interruptores encendidos el bean no
     * se crea y la aplicacion no arranca (significado ambiguo).
     */
    private final ModoApoyoSuspendido modoApoyoSuspendido;

    @org.springframework.beans.factory.annotation.Autowired
    public ExternalProfilingService(ProfilingContextFacade profilingContextFacade,
                                    @Value("${fitsense.seguridad.apoyo-suspendido-en-no-colgarse:false}")
                                    boolean apoyoSuspendidoEnNoColgarse,
                                    @Value("${fitsense.seguridad.permiso-apoyo-suspendido:false}")
                                    boolean permisoApoyoSuspendido) {
        this.profilingContextFacade = profilingContextFacade;
        this.modoApoyoSuspendido = ModoApoyoSuspendido.desdeInterruptores(apoyoSuspendidoEnNoColgarse,
                permisoApoyoSuspendido);
    }

    /** Compatibilidad: los dos interruptores apagados. */
    public ExternalProfilingService(ProfilingContextFacade profilingContextFacade) {
        this(profilingContextFacade, false, false);
    }

    public ModoApoyoSuspendido modoApoyoSuspendido() {
        return modoApoyoSuspendido;
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
                        snapshot.allowances().allowOverheadExercises(),
                        // V53-B: el permiso propio solo se traduce con la opcion B encendida. Con la A o apagado llega
                        // en null y el apoyo suspendido sigue a «no colgarse», como antes de V53-B.
                        modoApoyoSuspendido == ModoApoyoSuspendido.PERMISO_PROPIO
                                ? snapshot.allowSuspendedSupport() : null)
        ));
    }
}
