package main.web.services.fitsense.planning.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import main.web.services.fitsense.shared.domain.model.valueobjects.ContextoFaltaDeTiempo;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;

/**
 * Los dos campos nuevos son OPCIONALES: un cliente que solo envia {@code reason} sigue
 * funcionando igual (sin respuesta, la falta de tiempo se trata como no informada y la
 * sesion se conserva).
 */
public record SkipWorkoutResource(
        @NotNull(message = "Indica la causa: es lo que permite al sistema ajustar la semana siguiente.")
        @Schema(example = "LACK_OF_TIME", description = "Causa declarada del salto")
        SkipReason reason,

        @Schema(example = "IMPREVISTO", description = "Opcional, solo con LACK_OF_TIME: fue algo puntual "
                + "(IMPREVISTO), ese dia no hubo ningun minuto (SIN_TIEMPO_ESE_DIA) o suele tener menos "
                + "tiempo para este entrenamiento (MENOS_TIEMPO_HABITUAL, con availableMinutes)")
        ContextoFaltaDeTiempo lackOfTimeContext,

        @Min(value = 15, message = "Los minutos disponibles van de 15 a 180.")
        @Max(value = 180, message = "Los minutos disponibles van de 15 a 180.")
        @Schema(example = "30", description = "Opcional, obligatorio con MENOS_TIEMPO_HABITUAL: minutos que "
                + "sueles tener para ESTE entrenamiento (no cambia el perfil)")
        Integer availableMinutes
) {
    public SkipWorkoutResource(SkipReason reason) {
        this(reason, null, null);
    }
}
