package main.web.services.fitsense.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Confirmacion de {@code DELETE /api/v1/users/me}: la contrasena actual (reautenticacion) y {@code confirm=true}.
 * Ninguno de los dos se registra en logs.
 */
public record DeleteAccountResource(
        @Schema(description = "Contrasena actual de la cuenta", requiredMode = Schema.RequiredMode.REQUIRED)
        String password,
        @Schema(description = "Debe ser true: confirma que se entiende que la eliminacion es irreversible",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean confirm) {

    @Override
    public String toString() {
        return "DeleteAccountResource[password=***, confirm=" + confirm + "]";
    }
}
