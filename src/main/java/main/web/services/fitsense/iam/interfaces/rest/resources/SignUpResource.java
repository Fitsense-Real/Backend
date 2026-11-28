package main.web.services.fitsense.iam.interfaces.rest.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpResource(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 100) String firstName,
        @Size(max = 150) String lastName,
        @Size(max = 64) String timezone,
        boolean acceptsStudyParticipation,
        /** V63: version de las condiciones de uso aceptadas (opcional hasta encender la exigencia). */
        @Size(max = 40) String acceptedTermsVersion,
        /** V63: version de la politica de privacidad aceptada (opcional hasta encender la exigencia). */
        @Size(max = 40) String acceptedPrivacyVersion,
        /**
         * B2.1: version del consentimiento informado de investigacion que la app mostro. Solo con
         * {@code acceptsStudyParticipation=true}; obligatoria con la exigencia encendida.
         */
        @Size(max = 20) String acceptedStudyConsentVersion
) {}
