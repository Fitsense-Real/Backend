package main.web.services.fitsense.catalog.application.internal.queryservices;

import main.web.services.fitsense.catalog.domain.model.aggregates.Exercise;
import main.web.services.fitsense.catalog.infrastructure.persistence.jpa.repositories.ApprovedDifficultyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Capa de admision por aprobacion humana: deja pasar solo los ejercicios cuya
 * dificultad revisó y aprobó una persona (V27/V28), y con el nivel que esa
 * persona fijo, no con el de la columna.
 * <p>
 * IMPLEMENTADO Y APAGADO, y esto es lo importante de esta clase.
 * <p>
 * EL MOTIVO, CON NUMEROS. {@code v_exercises_manually_approved} tiene hoy
 * CUATRO filas: las cuatro aprobaciones de LOWER_HIP que escribio la V28.
 * Encender este filtro dejaria el conjunto elegible de cualquier participante en
 * cuatro ejercicios, los cuatro del mismo patron. Ningun plan pasaria las
 * validaciones -ni V5, que pide dos ejercicios por sesion, ni la cobertura de
 * 4C, que pide cuatro patrones- y el resultado seria
 * {@code PlanGenerationFailedException} para todo el mundo.
 * <p>
 * CUANDO SE PUEDE ENCENDER. No cuando el total sea "alto", sino cuando haya
 * aprobaciones para los CUATRO slots de 4C con margen: el umbral de 4C v4 §6
 * pide 3 por slot para llamarlo robusto, asi que 12 aprobaciones bien repartidas
 * valen mas que 200 concentradas. {@code countApprovedByMovementPattern()} da
 * exactamente esa cuenta, y el arranque la escribe en el log.
 * <p>
 * Mientras esta apagado NO hace ninguna consulta: el metodo sale en la primera
 * linea. Encenderlo no es un despliegue, es
 * {@code fitsense.approvals.filter-enabled=true}.
 */
@Service
public class ApprovedDifficultyGate {

    private static final Logger log = LoggerFactory.getLogger(ApprovedDifficultyGate.class);

    private final ApprovedDifficultyRepository repository;
    private final boolean enabled;

    public ApprovedDifficultyGate(
            ApprovedDifficultyRepository repository,
            @Value("${fitsense.approvals.filter-enabled:false}") boolean enabled) {
        this.repository = repository;
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Filtra el conjunto elegible dejando solo los aprobados cuyo nivel
     * aprobado no pase del maximo pedido.
     * <p>
     * Se usa el nivel APROBADO, no {@code exercises.difficulty_level}: es el
     * sentido entero de la V27. Un ejercicio aprobado como nivel 2 no entra en
     * un perfil de nivel 1 aunque la columna diga 1.
     *
     * @return la lista intacta si el filtro esta apagado.
     */
    public List<Exercise> apply(List<Exercise> eligible, int maxDifficultyLevel) {
        if (!enabled) return eligible;

        Map<Long, Short> aprobados = repository.findApprovedLevels();
        var filtrados = eligible.stream()
                .filter(exercise -> {
                    Short nivel = aprobados.get(exercise.getId());
                    return nivel != null && nivel <= maxDifficultyLevel;
                })
                .toList();

        log.info("Filtro de aprobaciones humanas ACTIVO: {} elegibles -> {} aprobados "
                        + "(nivel <= {}). Aprobados por patron: {}",
                eligible.size(), filtrados.size(), maxDifficultyLevel,
                repository.countApprovedByMovementPattern());

        return filtrados;
    }

    /**
     * Lo que se escribe en el log al arrancar, encendido o apagado. Sirve para
     * que el estado del interruptor no sea algo que haya que adivinar leyendo
     * properties en produccion.
     */
    public String estadoParaLog() {
        if (!enabled) {
            return "Filtro de aprobaciones humanas (V27/V28): APAGADO "
                    + "(fitsense.approvals.filter-enabled=false). El conjunto elegible se "
                    + "decide con exercises.difficulty_level.";
        }
        return ("Filtro de aprobaciones humanas (V27/V28): ACTIVO. %d ejercicios aprobados, "
                + "por patron: %s").formatted(
                repository.countApproved(), repository.countApprovedByMovementPattern());
    }
}
