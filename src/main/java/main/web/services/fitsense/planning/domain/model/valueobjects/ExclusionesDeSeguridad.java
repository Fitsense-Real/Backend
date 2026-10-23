package main.web.services.fitsense.planning.domain.model.valueobjects;

import java.util.Map;

/**
 * Los ejercicios HEREDADOS de la semana medida que hoy no son elegibles para el
 * participante SOLO por seguridad: lo serian sin las restricciones de seguridad
 * (banderas por edad, nivel y declaradas, retencion INC-4C-03 y contencion
 * INC-CAT-1374), con el mismo equipamiento, dificultad, bloqueos y filtro
 * funcional.
 * <p>
 * Lo que queda fuera por otra causa -un bloqueo del participante, equipamiento
 * que ya no declara, el filtro funcional- NO esta aqui y no se sustituye.
 *
 * @param originales el ejercicio tal como lo describe el catalogo, para conocer
 *                   su patron, zona y tipo sin que este en el conjunto elegible
 * @param motivos    por ejercicio, que restriccion lo excluye
 */
public record ExclusionesDeSeguridad(Map<Long, CandidateExercise> originales, Map<Long, String> motivos) {

    public ExclusionesDeSeguridad {
        originales = originales == null ? Map.of() : Map.copyOf(originales);
        motivos = motivos == null ? Map.of() : Map.copyOf(motivos);
        if (!motivos.keySet().equals(originales.keySet()))
            throw new IllegalArgumentException("Cada exclusion de seguridad declara su motivo.");
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
