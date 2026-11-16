package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * H2 (contrato prompt–V15): la regla de la mitad de V15 en una sola funcion, para que lo que se le comunica a la IA
 * sea exactamente lo que despues verifica el validador.
 * <p>
 * V15 ({@code PlanDraftValidator.validateFocusCoverage}): cuando el enfoque abarca 3 o mas grupos y la sesion tiene 4
 * o mas ejercicios DE TRABAJO (sin contar los complementos de capacidad, {@link CatalogEligibilityPolicy#complementos}),
 * ningun grupo puede tener mas de la mitad (division entera) de esos ejercicios.
 * <p>
 * Esta clase no cambia V15: la replica. {@code ContratoPromptV15IT} comprueba con el validador real que coinciden
 * (si alguien cambia V15, esa prueba avisa).
 */
public final class RepartoV15 {

    private RepartoV15() {
    }

    /** Ejercicios de trabajo a partir de los que aplica la regla de la mitad. */
    public static final int MIN_TRABAJO = 4;
    /** Grupos del enfoque a partir de los que aplica la regla de la mitad. */
    public static final int MIN_GRUPOS = 3;

    /**
     * Cuantos ejercicios del mismo grupo admite V15 en una sesion con {@code nTrabajo} ejercicios de trabajo, o
     * {@code null} si la regla de la mitad no aplica.
     */
    public static Integer topeMismoGrupo(int gruposDelFoco, int nTrabajo) {
        if (gruposDelFoco < MIN_GRUPOS || nTrabajo < MIN_TRABAJO) return null;
        return nTrabajo / 2;
    }

    public static Integer topeMismoGrupo(WorkoutFocus foco, int nTrabajo) {
        return topeMismoGrupo(foco.bodyPartCodes().size(), nTrabajo);
    }

    /**
     * La tabla que se comunica a la IA: para cada numero de ejercicios de trabajo n = 4..maxTrabajo, el tope del
     * mismo grupo. Vacia si la regla no aplica al enfoque o si {@code maxTrabajo < 4}. Las claves son texto para que
     * el JSON sea un objeto ({"4":2,"5":2,...}).
     */
    public static Map<String, Integer> tabla(int gruposDelFoco, int maxTrabajo) {
        if (gruposDelFoco < MIN_GRUPOS || maxTrabajo < MIN_TRABAJO) return Map.of();
        var tabla = new LinkedHashMap<String, Integer>();
        for (int n = MIN_TRABAJO; n <= maxTrabajo; n++) tabla.put(Integer.toString(n), topeMismoGrupo(gruposDelFoco, n));
        return Collections.unmodifiableMap(tabla);
    }
}
