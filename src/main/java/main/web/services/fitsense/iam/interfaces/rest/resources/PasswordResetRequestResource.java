package main.web.services.fitsense.iam.interfaces.rest.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequestResource(@NotBlank @Email @Size(max = 254) String email) {}
