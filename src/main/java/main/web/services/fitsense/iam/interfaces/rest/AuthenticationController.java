package main.web.services.fitsense.iam.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import main.web.services.fitsense.iam.application.internal.commandservices.LimiteDeAutenticacion;
import main.web.services.fitsense.iam.domain.exceptions.InvalidCredentialsException;
import main.web.services.fitsense.iam.domain.services.UserCommandService;
import main.web.services.fitsense.iam.interfaces.rest.resources.AuthenticatedUserResource;
import main.web.services.fitsense.iam.interfaces.rest.resources.SignInResource;
import main.web.services.fitsense.iam.interfaces.rest.resources.SignUpResource;
import main.web.services.fitsense.iam.interfaces.rest.resources.UserResource;
import main.web.services.fitsense.iam.interfaces.rest.transform.AuthenticatedUserResourceFromEntityAssembler;
import main.web.services.fitsense.iam.interfaces.rest.transform.SignInCommandFromResourceAssembler;
import main.web.services.fitsense.iam.interfaces.rest.transform.SignUpCommandFromResourceAssembler;
import main.web.services.fitsense.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@SecurityRequirements
@RequestMapping(value = "/api/v1/auth", produces = "application/json")
@Tag(name = "Autenticacion", description = "Registro e inicio de sesion")
public class AuthenticationController {

    private final UserCommandService userCommandService;
    private final main.web.services.fitsense.iam.application.internal.consent.RegistroConConsentimientos registro;
    private final LimiteDeAutenticacion limite;

    public AuthenticationController(UserCommandService userCommandService,
                                    main.web.services.fitsense.iam.application.internal.consent.RegistroConConsentimientos registro,
                                    LimiteDeAutenticacion limite) {
        this.userCommandService = userCommandService;
        this.registro = registro;
        this.limite = limite;
    }

    @PostMapping("/register")
    @Operation(summary = "Registra una cuenta y, si corresponde, la inscribe en el estudio",
            description = "429 DEMASIADAS_SOLICITUDES (con Retry-After) si la conexion supera el cupo de registros.")
    public ResponseEntity<UserResource> register(@Valid @RequestBody SignUpResource resource,
                                                 HttpServletRequest peticion) {
        limite.antesDelRegistro(peticion);
        var command = SignUpCommandFromResourceAssembler.toCommandFromResource(resource);
        return registro.registrar(command, resource.acceptedTermsVersion(), resource.acceptedPrivacyVersion(),
                        resource.acceptedStudyConsentVersion())
                .map(UserResourceFromEntityAssembler::toResourceFromEntity)
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @PostMapping("/login")
    @Operation(summary = "Inicia sesion y devuelve el token JWT",
            description = "401 con el mismo mensaje para cualquier fallo. 429 DEMASIADAS_SOLICITUDES (con Retry-After)"
                    + " tras demasiados intentos fallidos con el mismo correo o desde la misma conexion.")
    public ResponseEntity<AuthenticatedUserResource> login(@Valid @RequestBody SignInResource resource,
                                                           HttpServletRequest peticion) {
        limite.antesDelLogin(resource.email(), peticion);
        var command = SignInCommandFromResourceAssembler.toCommandFromResource(resource);
        try {
            var resultado = userCommandService.handle(command)
                    .map(auth -> AuthenticatedUserResourceFromEntityAssembler
                            .toResourceFromEntity(auth.user(), auth.token()))
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
            if (resultado.getStatusCode().is2xxSuccessful()) limite.loginCorrecto(resource.email());
            else limite.falloDeLogin(resource.email());
            return resultado;
        } catch (InvalidCredentialsException | IllegalArgumentException e) {
            // Correo desconocido o mal formado, contrasena incorrecta o cuenta no activa: cuentan igual.
            limite.falloDeLogin(resource.email());
            throw e;
        }
    }
}
