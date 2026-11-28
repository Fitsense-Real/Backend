package main.web.services.fitsense.iam.interfaces.acl;

import main.web.services.fitsense.iam.domain.model.queries.GetUserByIdQuery;
import main.web.services.fitsense.iam.domain.services.UserQueryService;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * Unico punto de entrada al contexto IAM desde otros contextos. Ninguno debe
 * inyectar UserRepository ni el agregado User directamente.
 */
@Service
public class IamContextFacade {

    private final UserQueryService userQueryService;
    private final UserRepository userRepository;
    private final boolean planesExigenParticipacion;

    /**
     * B2.4 (P0-04): {@code fitsense.study.plans-require-participation}.
     * <ul>
     *   <li>{@code true} (por omision, el comportamiento de siempre, OPCION A): solo un participante activo recibe
     *       planes. Requiere que el registro y el consentimiento digan claramente que el servicio es parte del
     *       estudio (decision del equipo y del comite, pendiente).</li>
     *   <li>{@code false} (OPCION B): cualquier cuenta activa recibe planes, participe o no, y la retirada del estudio
     *       no quita el servicio. Los analisis siguen filtrando por {@code is_study_participant}.</li>
     * </ul>
     * No se cambia el valor por omision sin la decision formal: ver 03_CONSENTIMIENTOS_Y_CONTRATOS.md.
     */
    public IamContextFacade(UserQueryService userQueryService, UserRepository userRepository,
                            @org.springframework.beans.factory.annotation.Value(
                                    "${fitsense.study.plans-require-participation:true}") boolean planesExigenParticipacion) {
        this.userQueryService = userQueryService;
        this.userRepository = userRepository;
        this.planesExigenParticipacion = planesExigenParticipacion;
    }

    public boolean existsUser(Long userId) {
        return userRepository.existsById(userId);
    }

    /** Zona horaria del usuario: define el cierre semanal y las fechas locales. */
    @Transactional(readOnly = true)
    public Optional<ZoneId> fetchTimezone(Long userId) {
        return userQueryService.handle(new GetUserByIdQuery(userId))
                .map(user -> user.getTimezone().zoneId());
    }

    @Transactional(readOnly = true)
    public boolean isEligibleForPlanGeneration(Long userId) {
        return userQueryService.handle(new GetUserByIdQuery(userId))
                .map(user -> planesExigenParticipacion ? user.isEligibleForPlanGeneration() : user.canSignIn())
                .orElse(false);
    }

    /** Zonas horarias distintas: la tarea semanal itera por zona, no por reloj del servidor. */
    @Transactional(readOnly = true)
    public List<String> fetchActiveTimezones() {
        return userRepository.findDistinctActiveTimezones();
    }

    /**
     * Cuentas de una zona que reciben plan en el cierre semanal: los participantes activos (opcion A, por omision)
     * o todas las cuentas activas (opcion B, {@code fitsense.study.plans-require-participation=false}).
     */
    @Transactional(readOnly = true)
    public List<Long> fetchActiveParticipantIdsInTimezone(String zone) {
        var cuentas = planesExigenParticipacion ? userRepository.findActiveParticipantsInTimezone(zone)
                : userRepository.findActiveUsersInTimezone(zone);
        return cuentas.stream()
                .map(user -> user.getId())
                .toList();
    }
}
