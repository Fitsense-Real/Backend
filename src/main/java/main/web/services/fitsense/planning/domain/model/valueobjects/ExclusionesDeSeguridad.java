package main.web.services.fitsense.planning.domain.model.valueobjects;

import java.util.Map;

/**
 * Los ejercicios HEREDADOS que hoy no son elegibles para el participante y
 * tienen una CAUSA DE SEGURIDAD VERIFICADA ({@code SafetyConflictPolicy}): una
 * bandera del catalogo contra una restriccion actual del perfil, la retencion
 * INC-4C-03 o la contencion INC-CAT-1374.
 * <p>
 * Lo que queda fuera por otra causa -un bloqueo del participante, equipamiento
 * que ya no declara, el filtro funcional- NO esta aqui y no se sustituye. Un
 * motivo que no este hecho solo de causas admitidas se rechaza al construir.
 *
 * @param originales el ejercicio tal como lo describe el catalogo, para conocer
 *                   su patron, zona y tipo sin que este en el conjunto elegible
 * @param motivos    por ejercicio, sus causas unidas por «+»
 */
public record ExclusionesDeSeguridad(Map<Long, CandidateExercise> originales, Map<Long, String> motivos) {

    public ExclusionesDeSeguridad {
        originales = originales == null ? Map.of() : Map.copyOf(originales);
        motivos = motivos == null ? Map.of() : Map.copyOf(motivos);
        if (!motivos.keySet().equals(originales.keySet()))
            throw new IllegalArgumentException("Cada exclusion de seguridad declara su motivo.");
        motivos.forEach((id, motivo) -> {
            if (!main.web.services.fitsense.planning.domain.services.SafetyConflictPolicy.esMotivoDeSeguridad(motivo))
                throw new IllegalArgumentException(
                        "La exclusion de %d no tiene una causa de seguridad admitida: %s".formatted(id, motivo));
        });
    }

    public static ExclusionesDeSeguridad ninguna() {
        return new ExclusionesDeSeguridad(Map.of(), Map.of());
    }

    public boolean incluye(Long exerciseId) {
        return exerciseId != null && originales.containsKey(exerciseId);
    }

    public boolean vacias() {
        return originales.isEmpty();
    }
}
