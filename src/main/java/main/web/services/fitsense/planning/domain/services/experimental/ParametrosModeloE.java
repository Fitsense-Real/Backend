package main.web.services.fitsense.planning.domain.services.experimental;

import java.util.Map;
import java.util.Set;

/**
 * MODELO E (EXPERIMENTAL, DESACTIVADO). Parametros versionados de la planificacion por volumen semanal.
 * <p>
 * <b>Ninguno de estos valores esta aprobado.</b> Son parametros candidatos para el laboratorio
 * ({@code docs/FitSense/auditoria_cientifica_entrenamiento/}); la ruta de generacion de produccion no lee esta clase.
 * Para que una version se use fuera del laboratorio hace falta la aprobacion del responsable, una version nueva de
 * {@code calculation_configs} y su cableado explicito, que no existe.
 * <ul>
 *   <li>{@code seriesSemanaObjetivo}: series directas por grupo grande y semana con frecuencia suficiente
 *       ({@code diasParaObjetivoCompleto}). Candidatos: GAIN_MUSCLE 10 (ACSM 2026: &ge; 10 series/semana para
 *       hipertrofia), resto 4 (dosis minima, Iversen 2021).</li>
 *   <li>{@code seriesSemanaMinimo}: lo que se exige por cada dia disponible hasta llegar al objetivo; evita que con 1-2
 *       dias el objetivo semanal obligue a sesiones de 13-21 ejercicios (laboratorio anterior).</li>
 *   <li>{@code techoSeriesGrupoSesion}: techo BLANDO por grupo y sesion. Procede de un preprint (Remmert 2025, ~11
 *       series fraccionales); no es una norma de seguridad.</li>
 *   <li>{@code techoTiempoFraccion}: la sesion se planifica hasta {@code fraccion x M}. 1,0 = los minutos del perfil.
 *       La tolerancia de V4 (x 1,15) sigue siendo solo una holgura de validacion, no un objetivo.</li>
 * </ul>
 */
public record ParametrosModeloE(
        boolean habilitado,
        String version,
        Map<String, Integer> seriesSemanaObjetivo,
        int seriesSemanaMinimo,
        int diasParaObjetivoCompleto,
        int techoSeriesGrupoSesion,
        int techoSeriesGrupoSemana,
        double techoTiempoFraccion,
        Set<String> gruposGrandes
) {

    public static final String VERSION = "E-0.1-EXPERIMENTAL-SIN-APROBAR";

    public static final Set<String> GRUPOS_GRANDES = Set.of("chest", "back", "shoulders", "upper legs");

    /** Los candidatos del laboratorio, DESACTIVADOS. */
    public static ParametrosModeloE desactivado() {
        return new ParametrosModeloE(false, VERSION,
                Map.of("GAIN_MUSCLE", 10, "INCREASE_STRENGTH", 4, "LOSE_WEIGHT", 4, "GENERAL_FITNESS", 4),
                4, 3, 10, 20, 1.0, GRUPOS_GRANDES);
    }

    /**
     * Objetivo semanal por grupo grande ESCALADO por los dias efectivos: {@code min(objetivo, minimo x dias)}.
     * GAIN_MUSCLE con 1 dia: 4; con 2: 8; con 3 o mas: 10.
     */
    public int objetivoSemanal(String goalType, int diasEfectivos) {
        int objetivo = seriesSemanaObjetivo.getOrDefault(goalType, seriesSemanaMinimo);
        return Math.min(objetivo, seriesSemanaMinimo * Math.max(1, diasEfectivos));
    }

    /** Techo de planificacion en segundos para una sesion de {@code minutos}. */
    public long techoSegundos(int minutos) {
        return (long) Math.floor(minutos * 60L * techoTiempoFraccion);
    }
}
