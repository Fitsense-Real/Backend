package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningPolicy;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;

/**
 * LO QUE UNA INTERVENCION PERMITE CON UNA VERSION DE POLITICA (rediseno R/R', 05/11/2026).
 * <p>
 * {@link TipoIntervencion} sigue siendo la fuente de los permisos de siempre y no cambia (vive en {@code shared} y no
 * conoce la politica). Esto solo anade lo que la politica autoriza de forma explicita y que V8C, el prompt y la
 * factibilidad tienen que decir igual:
 * <ul>
 *   <li>P-A2 ({@link PlanningPolicy#sustitucionDesmotivacion()}): {@code INVESTIGAR_CAMBIO_MINIMO} admite UNA
 *       sustitucion lateral estricta.</li>
 *   <li>P-A1 ({@link PlanningPolicy#fatigaGM()}): {@code REVISAR_CARGA_Y_RECUPERACION} en {@code GAIN_MUSCLE} admite
 *       la dosis de mantenimiento (una serie menos en las estructurales).</li>
 * </ul>
 * Con la politica historica (o null) todo es exactamente lo de antes.
 */
public final class PermisosIntervencion {

    private PermisosIntervencion() {
    }

    /** P-A2: una sola sustitucion lateral ante desmotivacion. */
    public static boolean sustitucionUnica(TipoIntervencion t, PlanningPolicy politica) {
        return t == TipoIntervencion.INVESTIGAR_CAMBIO_MINIMO && politica != null && politica.sustitucionDesmotivacion();
    }

    /** Si la sesion admite sustituir (con la regla de siempre o con P-A2). */
    public static boolean permiteSustituir(TipoIntervencion t, PlanningPolicy politica) {
        return t.permiteSustituirEjercicio() || sustitucionUnica(t, politica);
    }

    /** P-A1: dosis de mantenimiento de 4D ante fatiga. */
    public static boolean mantenimientoGM(TipoIntervencion t, PlanningPolicy politica) {
        return t == TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION && politica != null && politica.fatigaGM();
    }

    /**
     * El sustituto cumple la regla estricta de P-A2 respecto del original: mismo patron, mismo grupo, musculo objetivo
     * compatible, dificultad e impacto no superiores. Lo mismo que comprueba V8C (aqui solo para listar opciones).
     */
    public static boolean equivalenteEstricto(CandidateExercise original, CandidateExercise sustituto) {
        if (original == null || sustituto == null || original.movementPattern() == null) return false;
        if (!original.movementPattern().equals(sustituto.movementPattern())) return false;
        if (!java.util.Objects.equals(original.bodyPartCode(), sustituto.bodyPartCode())) return false;
        if (original.targetMuscle() != null && sustituto.targetMuscle() != null
                && !original.targetMuscle().equalsIgnoreCase(sustituto.targetMuscle())) return false;
        if (sustituto.difficulty() > original.difficulty()) return false;
        return !ImpactoPolicy.sube(original, sustituto);
    }

    /** La instruccion del prompt: la de siempre salvo donde la politica cambia el permiso. */
    public static String instruccion(TipoIntervencion t, PlanningPolicy politica) {
        if (sustitucionUnica(t, politica))
            return "Investigar que dificulta mantener la rutina y proponer un cambio pequeno y justificado. Lo"
                    + " preferente es SUSTITUIR COMO MAXIMO UN ejercicio por otro equivalente de la lista (mismo patron"
                    + " y grupo, dificultad e impacto no superiores): variar un ejercicio puede ayudar a la motivacion;"
                    + " quitar una repeticion no cambia la experiencia. Solo si la lista no ofrece ninguno, baja un poco"
                    + " la dosis. Nunca las dos cosas en la misma sesion. No retirar ejercicios, y no cambiar el dia.";
        return t.instruccion();
    }
}
