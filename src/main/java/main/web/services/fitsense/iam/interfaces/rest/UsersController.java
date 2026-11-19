package main.web.services.fitsense.iam.interfaces.rest;

import main.web.services.fitsense.iam.application.internal.deletion.EliminarMiCuenta;
import main.web.services.fitsense.iam.interfaces.rest.resources.AccountDeletionResource;
import main.web.services.fitsense.iam.interfaces.rest.resources.DeleteAccountResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import main.web.services.fitsense.iam.domain.model.commands.WithdrawFromStudyCommand;
import main.web.services.fitsense.iam.domain.model.queries.GetUserByIdQuery;
import main.web.services.fitsense.iam.domain.services.UserCommandService;
import main.web.services.fitsense.iam.domain.services.UserQueryService;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.interfaces.rest.resources.UserResource;
import main.web.services.fitsense.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;
import main.web.services.fitsense.shared.domain.exceptions.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/users", produces = "application/json")
@Tag(name = "Usuarios", description = "Cuenta del usuario autenticado")
public class UsersController {

    private final UserQueryService userQueryService;
    private final UserCommandService userCommandService;
    private final EliminarMiCuenta eliminarMiCuenta;

    public UsersController(UserQueryService userQueryService, UserCommandService userCommandService,
                           EliminarMiCuenta eliminarMiCuenta) {
        this.userQueryService = userQueryService;
        this.userCommandService = userCommandService;
        this.eliminarMiCuenta = eliminarMiCuenta;
    }

    @GetMapping("/me")
    @Operation(summary = "Devuelve la cuenta del usuario del JWT")
    public ResponseEntity<UserResource> me(@AuthenticationPrincipal UserDetailsImpl principal) {
        return userQueryService.handle(new GetUserByIdQuery(principal.getUserId()))
                .map(UserResourceFromEntityAssembler::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", principal.getUserId()));
    }

    @PostMapping("/me/withdraw")
    @Operation(summary = "Retiro voluntario del estudio. No borra los datos ya recolectados.")
    public ResponseEntity<UserResource> withdraw(@AuthenticationPrincipal UserDetailsImpl principal) {
        return userCommandService.handle(new WithdrawFromStudyCommand(principal.getUserId()))
                .map(UserResourceFromEntityAssembler::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", principal.getUserId()));
    }

    /**
     * Elimina la cuenta y todos sus datos (CONTRATO_ELIMINAR_CUENTA.md). 200 si el borrado ya termino; 202 si la cuenta
     * ya quedo desactivada y el borrado se completa automaticamente. En ambos casos el token deja de servir.
     */
    @DeleteMapping("/me")
    @Operation(summary = "Elimina la cuenta del usuario del JWT y todos sus datos (irreversible)")
    public ResponseEntity<AccountDeletionResource> delete(@AuthenticationPrincipal UserDetailsImpl principal,
                                                          @RequestBody DeleteAccountResource body) {
        var e = eliminarMiCuenta.ejecutar(principal.getUserId(), body.password(), body.confirm());
        var recurso = new AccountDeletionResource(e.completada() ? "DELETED" : "PENDING", e.deletionId(),
                e.solicitadaEn(), e.completadaEn(), e.yaRegistrada() ? Boolean.TRUE : null);
        return ResponseEntity.status(e.completada() ? HttpStatus.OK : HttpStatus.ACCEPTED)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(recurso);
    }
}
