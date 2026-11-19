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
        consentimientos.validarRegistro(condiciones, privacidad);
        var usuario = usuarios.handle(comando);
        usuario.ifPresent(u -> consentimientos.registrarAlta(u.getId(), comando.acceptsStudyParticipation(),
                condiciones, privacidad));
        return usuario;
    }
}
