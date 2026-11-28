package main.web.services.fitsense.iam.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Mismas reglas de contrasena que el registro (8-72). */
public record PasswordResetConfirmResource(@NotBlank @Size(max = 200) String token,
                                           @NotBlank @Size(min = 8, max = 72) String newPassword) {}
