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

    /**
     * CONTENCION INC-CAT-1374 (23/10/2026). El 1374 «wind sprints» es, segun su
     * GIF, una elevacion alterna de rodillas COLGADO de una barra, pero su
     * registro dice que no exige colgarse, ni brazos arriba, ni barra
     * (docs/FitSense/REVISION_CATALOGO_CARDIO.md). Con ese dato los filtros de
     * seguridad no lo ven y llega a quien declaro no poder colgarse o no tiene
     * barra.
     * <p>
     * Mientras el dato no se corrija con su migracion, solo llega a quien cumple
     * los requisitos REALES del ejercicio: barra de dominadas declarada y ni
     * colgarse ni brazos arriba restringidos. La exclusion por alto impacto del
     * dato actual se mantiene: es mas conservadora que la corregida.
     * <p>
     * Igual que INC-4C-03, SIN INTERRUPTOR: es seguridad, no medicion. Sale
     * cuando la migracion correctiva este aplicada.
     */
    static final long CONTENIDO_INC_CAT_1374 = 1374L;
    static final String BARRA_DE_DOMINADAS = "pull-up bar";

    /** Si el perfil cumple los requisitos reales del 1374. */
    static boolean admite1374(PlanningProfile profile, SafetyProfile safety) {
        boolean barra = profile.equipmentCodes() != null && profile.equipmentCodes().stream()
                .anyMatch(code -> code != null && BARRA_DE_DOMINADAS.equals(code.trim().toLowerCase(java.util.Locale.ROOT)));
        return barra && !safety.excludeHanging() && !safety.excludeOverhead();
    }

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
        return elegibles(profile, maxDifficultyLevel, safety, true);
    }

    /**
     * Paso 3 · el mismo conjunto, SIN escribir en el log. Solo para recuentos
     * contrafactuales que explican una escasez («con esta restriccion levantada
     * habria N»): su resultado nunca llega a un generador ni a un plan.
     */
    public List<CandidateExercise> fetchEligibleForExplanation(PlanningProfile profile,
                                                               int maxDifficultyLevel,
                                                               SafetyProfile safety) {
        return elegibles(profile, maxDifficultyLevel, safety, false);
    }

    /**
     * P2-D · el conjunto que el perfil tendria SIN las restricciones de
     * seguridad -banderas, retencion INC-4C-03 y contencion INC-CAT-1374-, con
     * todo lo demas igual: equipamiento, dificultad, bloqueos del participante y
     * filtro funcional. Sirve SOLO para saber si un ejercicio heredado quedo
     * fuera por seguridad; nunca llega a un generador ni a un plan.
     */
    public List<CandidateExercise> fetchIgnoringSafety(PlanningProfile profile, int maxDifficultyLevel) {
        return elegibles(profile, maxDifficultyLevel, SafetyProfile.none(), false, false);
    }

    /** Los ejercicios que retiene INC-4C-03. */
    public static boolean retenidoInc4C03(Long exerciseId) {
        return exerciseId != null && RETENIDOS_INC_4C_03.contains(exerciseId);
    }

    /** Si INC-CAT-1374 contiene este ejercicio para este perfil. */
    public static boolean contenidoIncCat1374(Long exerciseId, PlanningProfile profile, SafetyProfile safety) {
        return java.util.Objects.equals(exerciseId, CONTENIDO_INC_CAT_1374) && !admite1374(profile, safety);
    }

    private List<CandidateExercise> elegibles(PlanningProfile profile, int maxDifficultyLevel,
                                              SafetyProfile safety, boolean registrar) {
        return elegibles(profile, maxDifficultyLevel, safety, registrar, true);
    }

    private List<CandidateExercise> elegibles(PlanningProfile profile, int maxDifficultyLevel,
                                              SafetyProfile safety, boolean registrar,
                                              boolean aplicarRetenciones) {
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

        // INC-CAT-1374: contencion de seguridad, tambien antes de todo lo demas.
        boolean contener1374 = aplicarRetenciones && !admite1374(profile, safety);
        boolean contenido = contener1374 && delCatalogo.stream()
                .anyMatch(view -> java.util.Objects.equals(view.exerciseId(), CONTENIDO_INC_CAT_1374));

        var seguros = delCatalogo.stream()
                .filter(view -> !aplicarRetenciones || !RETENIDOS_INC_4C_03.contains(view.exerciseId()))
                .filter(view -> !(contener1374 && java.util.Objects.equals(view.exerciseId(), CONTENIDO_INC_CAT_1374)))
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

        if (!registrar) return candidatos;

        if (contenido)
            log.info("EJERCICIOS_CONTENIDOS user={} motivo=INC-CAT-1374 ids=[1374] (exige barra, colgarse y "
                            + "brazos arriba; el dato del catalogo no lo dice)",
                    profile.userId() == null ? "desconocido" : profile.userId());

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