package main.web.services.fitsense.iam.interfaces.acl;

import main.web.services.fitsense.iam.application.internal.consent.ConsentimientoService;
import main.web.services.fitsense.iam.domain.model.queries.GetUserByIdQuery;
import main.web.services.fitsense.iam.domain.services.UserQueryService;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
    private final ConsentimientoService consentimientos;
    private final JdbcTemplate jdbc;

    /**
     * B2.4 (P0-04): {@code fitsense.study.plans-require-participation}.
     * <ul>
     *   <li>{@code true} (por omision): <b>OPCION A, la decision de producto para este estudio</b> (C1). Kora solo es
     *       para participantes: solo un participante activo y elegible recibe planes. Con el registro definitivo
     *       ({@code fitsense.consent.enforce-on-signup=true}) hace falta ademas un consentimiento informado valido
     *       ({@link ConsentimientoService#consentimientoValido}). La aprobacion del protocolo y del consentimiento
     *       informado sigue pendiente.</li>
     *   <li>{@code false} (OPCION B): cualquier cuenta activa recibe planes. <b>Fuera del alcance de este estudio y
     *       desactivada</b>; el codigo se conserva porque no supone un riesgo mientras la propiedad valga true.</li>
     * </ul>
     * Ver docs/FitSense/integracion_final_estudio/01_OPCION_A_Y_ELEGIBILIDAD.md.
     */
    public IamContextFacade(UserQueryService userQueryService, UserRepository userRepository,
                            @org.springframework.beans.factory.annotation.Value(
                                    "${fitsense.study.plans-require-participation:true}") boolean planesExigenParticipacion,
                            ConsentimientoService consentimientos, JdbcTemplate jdbc) {
        this.userQueryService = userQueryService;
        this.userRepository = userRepository;
        this.planesExigenParticipacion = planesExigenParticipacion;
        this.consentimientos = consentimientos;
        this.jdbc = jdbc;
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
                .orElse(false) && consentimientoSuficiente(userId);
    }

    /**
     * C1 · LA ELEGIBILIDAD SE VUELVE A COMPROBAR AL PUBLICAR, dentro de la transaccion que publica. Entre la fase 1 (que
     * ya la comprueba) y la publicacion pasan hasta minutos de IA: una retirada del estudio en ese hueco no puede acabar
     * en un plan nuevo. La fila de la cuenta se lee {@code FOR SHARE}: una retirada simultanea (que la actualiza) espera
     * a que esta publicacion termine, o esta espera a la retirada y la ve. No hay un orden en el que se publique para
     * alguien ya retirado.
     * <p>
     * Exige una transaccion abierta (la de la publicacion): fuera de ella el bloqueo no protegeria nada.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean elegibleAlPublicar(Long userId) {
        var fila = jdbc.query("SELECT status = 'ACTIVE' AS activa, is_study_participant AND consent_granted_at IS NOT NULL"
                + " AND withdrawn_at IS NULL AS participa FROM users WHERE user_id = ? FOR SHARE", (rs, i) ->
                new boolean[]{rs.getBoolean("activa"), rs.getBoolean("participa")}, userId);
        if (fila.isEmpty()) return false;
        boolean elegible = planesExigenParticipacion ? fila.get(0)[0] && fila.get(0)[1] : fila.get(0)[0];
        return elegible && consentimientoSuficiente(userId);
    }

    /** Opcion A con el registro definitivo: la casilla no basta, hace falta el consentimiento informado valido. */
    private boolean consentimientoSuficiente(Long userId) {
        return !consentimientos.exigeConsentimientoValidoParaPlanes() || consentimientos.consentimientoValido(userId);
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
                .filter(this::consentimientoSuficiente)
                .toList();
    }
}
