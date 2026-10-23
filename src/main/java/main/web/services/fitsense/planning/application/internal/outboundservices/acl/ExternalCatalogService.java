package main.web.services.fitsense.planning.application.internal.outboundservices.acl;

import main.web.services.fitsense.catalog.interfaces.acl.CatalogContextFacade;
import main.web.services.fitsense.catalog.interfaces.acl.EligibleExerciseView;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningProfile;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.SafetyProfile;
import main.web.services.fitsense.planning.domain.services.CatalogEligibilityPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Capa anticorrupcion hacia el catalogo: el conjunto elegible de 20.3. */
@Service
public class ExternalCatalogService {

    private static final Logger log = LoggerFactory.getLogger(ExternalCatalogService.class);

    /**
     * RETENIDOS POR INC-4C-03. Fuera de la seleccion automatica hasta revisar
     * su dificultad, sus requisitos de equipamiento y su unidad.
     * <p>
     * TRES, de la auditoria del bloque 7 del 15/10. Elegibles para una
     * principiante de 72 anos, los tres con {@code difficulty_level = 1},
     * declarados peso corporal y con unidad SETS_REPS pese a describir
     * posiciones sostenidas:
     * <ul>
     *   <li>813 Bandera: sujetarse a un poste vertical y sostener el cuerpo.</li>
     *   <li>1192 Skin the cat: colgarse de una barra y pasar las piernas por
     *       encima de la cabeza.</li>
     *   <li>1298 Maltes en apertura: sostenerse sobre un par de anillas.</li>
     * </ul>
     * CUATRO MAS, que 4D v2 §4 ya daba por fuera del primer perfil «hasta
     * revisar su ejecucion» y que seguian entrando porque nada implementaba esa
     * decision: 858 front lever, 861 full planche, 873 front lever reps y
     * 885 handstand. Con 813 son los cinco casos de instruccion contradictoria
     * de esa seccion.
     * <p>
     * NO LLEVA INTERRUPTOR, y es deliberado. Un filtro de seguridad con
     * propiedad para apagarlo se queda apagado; los modos configurables de este
     * proyecto (4C, aprobaciones) son para MEDIR, no para retener. Quitar un id
     * de aqui es una decision revisada, no un cambio de configuracion.
     * <p>
     * TEMPORAL. Sale cuando la revision controlada devuelva nivel, requisitos y
     * unidad verificados para cada uno. No sustituye a corregir el dato.
     */
    private static final Set<Long> RETENIDOS_INC_4C_03 =
            Set.of(813L, 858L, 861L, 873L, 885L, 1192L, 1298L);

    private final CatalogContextFacade catalogContextFacade;

    public ExternalCatalogService(CatalogContextFacade catalogContextFacade) {
        this.catalogContextFacade = catalogContextFacade;
    }

    /**
     * Las restricciones de seguridad se aplican EN LA CONSULTA, no en el prompt.
     * La IA no llega a ver los ejercicios que no le corresponden al participante,
     * asi que no puede proponerlos aunque ignore las instrucciones.
     */
    public List<CandidateExercise> fetchEligibleFor(PlanningProfile profile,
                                                    int maxDifficultyLevel,
                                                    SafetyProfile safety) {
        var delCatalogo = catalogContextFacade.fetchEligibleExercises(
                profile.equipmentCodes(),
                profile.excludesGymOnlyEquipment(),
                maxDifficultyLevel,
                profile.blockedExerciseIds(),
                safety.excludeHighImpact(),
                safety.excludeFloorWork(),
                safety.excludeAxialLoad(),
                safety.excludeKneeling(),
                safety.excludeHanging(),
                safety.excludeOverhead(),
                safety.excludeInversion());

        // INC-4C-03, antes de cualquier otra cosa: un ejercicio retenido no
        // llega al generador ni al prompt, pase lo que pase aguas abajo.
        // Se cuenta aparte en vez de dentro del filtro para que el flujo de
        // abajo siga sin efectos colaterales.
        var retenidos = delCatalogo.stream()
                .map(EligibleExerciseView::exerciseId)
                .filter(RETENIDOS_INC_4C_03::contains)
                .sorted()
                .toList();

        var seguros = delCatalogo.stream()
                .filter(view -> !RETENIDOS_INC_4C_03.contains(view.exerciseId()))
                .map(view -> new CandidateExercise(
                        view.exerciseId(),
                        view.nameEs(),
                        view.bodyPartCode(),
                        view.equipmentCode(),
                        view.difficultyLevel(),
                        view.isDurationBased() ? PrescriptionType.DURATION : PrescriptionType.SETS_REPS,
                        view.targetMuscle(),
                        // V26, para la cobertura de los cuatro slots de 4C. Se
                        // pasa tal cual: la traduccion a slots es de planning.
                        view.movementPattern()))
                .toList();

        // Paso 2: filtro FUNCIONAL, despues de seguridad, dificultad,
        // equipamiento, bloqueos y retenidos. Zonas fuera de enfoque y
        // estiramientos (hallazgo M) siguen fuera, salvo que su patron sea la
        // ruta de una capacidad del objetivo (4A v6): cardio para LOSE_WEIGHT y
        // GENERAL_FITNESS, movilidad para GENERAL_FITNESS. La decision es de
        // CatalogEligibilityPolicy, la misma que leen el motor de reglas y V15.
        var candidatos = seguros.stream()
                .filter(c -> CatalogEligibilityPolicy.decidir(c, profile.goalType()).admite())
                .toList();

        // Visible siempre, como EJERCICIOS_RETENIDOS: un filtro que no se ve
        // cuesta una medicion entera.
        log.info("CATALOGO_FUNCIONAL user={} objetivo={} seguros={} elegibles={} {}",
                profile.userId() == null ? "desconocido" : profile.userId(), profile.goalType(),
                seguros.size(), candidatos.size(),
                CatalogEligibilityPolicy.informe(seguros, profile.goalType()).describe());

        // Visible a proposito. INC-4C-03 existe porque un filtro invisible
        // -el del cortacircuitos- costo una medicion entera.
        if (!retenidos.isEmpty())
            log.info("EJERCICIOS_RETENIDOS user={} motivo=INC-4C-03 ids={} elegibles_restantes={}",
                    profile.userId() == null ? "desconocido" : profile.userId(),
                    retenidos, candidatos.size());

        return candidatos;
    }

    public Map<Long, String> fetchNames(Set<Long> exerciseIds) {
        return catalogContextFacade.fetchNames(exerciseIds);
    }

    /** Detalle con media, para pintar el plan en la app. */
    public Map<Long, EligibleExerciseView> fetchDetails(Set<Long> exerciseIds) {
        return catalogContextFacade.fetchDetails(exerciseIds);
    }
}