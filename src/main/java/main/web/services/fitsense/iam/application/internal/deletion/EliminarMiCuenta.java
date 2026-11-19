package main.web.services.fitsense.iam.application.internal.deletion;

import main.web.services.fitsense.iam.application.internal.outboundservices.hashing.HashingService;
import main.web.services.fitsense.iam.domain.exceptions.ConfirmationRequiredException;
import main.web.services.fitsense.iam.domain.exceptions.ReauthenticationFailedException;
import main.web.services.fitsense.iam.domain.exceptions.TooManyReauthenticationAttemptsException;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import org.springframework.stereotype.Service;

/**
 * Eliminacion pedida desde la app por la persona autenticada: exige {@code confirm=true} y la contrasena actual
 * (reautenticacion: un JWT robado o un movil desbloqueado no bastan), con limite de fallos. El borrado lo hace
 * {@link EliminacionDeCuentaService}, el mismo que usara la solicitud web.
 */
@Service
public class EliminarMiCuenta {

    private final UserRepository usuarios;
    private final HashingService hashing;
    private final LimiteDeReautenticacion limite;
    private final EliminacionDeCuentaService eliminacion;

    public EliminarMiCuenta(UserRepository usuarios, HashingService hashing, LimiteDeReautenticacion limite,
                            EliminacionDeCuentaService eliminacion) {
        this.usuarios = usuarios;
        this.hashing = hashing;
        this.limite = limite;
        this.eliminacion = eliminacion;
    }

    public EliminacionDeCuentaService.Eliminacion ejecutar(long userId, String password, Boolean confirm) {
        if (!Boolean.TRUE.equals(confirm))
            throw new ConfirmationRequiredException(
                    "Falta la confirmacion: envia confirm=true para eliminar la cuenta de forma irreversible.");
        // El intento cuenta como fallo desde antes de comprobar la contrasena (reserva atomica): peticiones en
        // paralelo con contrasenas distintas no pueden probar mas de max-failed-reauth en la ventana.
        long espera = limite.reservar(userId);
        if (espera > 0) throw new TooManyReauthenticationAttemptsException(espera);

        var usuario = usuarios.findById(userId)
                .orElseThrow(EliminacionDeCuentaService.CuentaInexistenteException::new);
        if (password == null || password.isEmpty() || !hashing.matches(password, usuario.getPasswordHash()))
            throw new ReauthenticationFailedException();
        limite.limpiar(userId);
        return eliminacion.eliminar(userId, EliminacionDeCuentaService.Origen.APP);
    }
}
