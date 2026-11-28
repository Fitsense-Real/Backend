package main.web.services.fitsense.iam.application.internal.consent;

import main.web.services.fitsense.iam.domain.model.aggregates.User;
import main.web.services.fitsense.iam.domain.model.commands.SignUpCommand;
import main.web.services.fitsense.iam.domain.services.UserCommandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Alta de cuenta y sus consentimientos en una sola transaccion: o se guardan ambos o ninguno. */
@Service
public class RegistroConConsentimientos {

    private final UserCommandService usuarios;
    private final ConsentimientoService consentimientos;

    public RegistroConConsentimientos(UserCommandService usuarios, ConsentimientoService consentimientos) {
        this.usuarios = usuarios;
        this.consentimientos = consentimientos;
    }

    @Transactional
    public Optional<User> registrar(SignUpCommand comando, String condiciones, String privacidad) {
        return registrar(comando, condiciones, privacidad, null);
    }

    /**
     * B2.1: con la version del consentimiento informado que la app mostro ({@code acceptedStudyConsentVersion}).
     * Todo se valida antes de crear la cuenta; si algo falla no queda ninguna cuenta a medias.
     */
    @Transactional
    public Optional<User> registrar(SignUpCommand comando, String condiciones, String privacidad, String estudio) {
        consentimientos.validarRegistro(condiciones, privacidad, comando.acceptsStudyParticipation(), estudio);
        var usuario = usuarios.handle(comando);
        usuario.ifPresent(u -> consentimientos.registrarAlta(u.getId(), comando.acceptsStudyParticipation(),
                condiciones, privacidad, estudio));
        return usuario;
    }
}
