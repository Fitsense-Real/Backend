package main.web.services.fitsense.planning.domain.model.valueobjects;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * VERSION DE LA POLITICA DE PLANIFICACION con la que se genera y se valida un plan.
 * <ul>
 *   <li>{@link #HISTORICA} ({@code PLAN-HIST-1}): la de siempre. V20 al 70 %, V4 compara los minutos declarados o
 *       estimados con {@code round(M x 1,15)}.</li>
 *   <li>{@link #V4C} ({@code PLAN-V4-0.1}, candidata): V4 se comprueba con los SEGUNDOS ESTIMADOS del contenido
 *       ({@code segundos x 100 <= M x 60 x 115}), declare lo que declare la sesion.</li>
 *   <li>Rediseno controlado tras la auditoria R/R' (orden del 05/11/2026, P-G1 aprobada como direccion de diseno):
 *       {@code sessionMinutes} es el MAXIMO disponible. Dos disenos candidatos de duracion para compararlos
 *       ({@link #DUR_A}, {@link #DUR_B}) y la version integrada ({@link #REV2}). Todas candidatas: exigen
 *       {@code fitsense.planning.policy.allow-candidates=true}.</li>
 * </ul>
 * Cada version es una combinacion EXPLICITA de decisiones; no hay interruptores sueltos. Un plan sin version
 * registrada es {@link #HISTORICA}. Una version desconocida es un error: no se reinterpreta con otra.
 */
public record PlanningPolicy(String id, boolean candidata, boolean v4Segundos, String descripcion,
                             Duracion duracion, boolean v4Maximo, boolean volumenSemanal, boolean pullReal,
                             boolean impacto, boolean alternativasDeterministas, boolean fatigaGM,
                             boolean sustitucionDesmotivacion, boolean reparaCatalogoInactivo) {

    /** Como se trata el tiempo de la sesion. */
    public enum Duracion {
        /** Historica: V20 rechaza por debajo del 70 % de M y el generador llena hasta ahi. */
        PISO_70,
        /**
         * Diseno A: V20 no rechaza; el generador NO llena tiempo. El contenido sale de la cobertura de patrones
         * y de una dosis semanal objetivo por grupo (series) y de minutos aerobicos, con techo; el tiempo solo
         * limita por arriba (V4).
         */
        DOSIS_SEMANAL,
        /**
         * Diseno B: V20 no rechaza; el generador sigue intentando llegar al 70 % de M como objetivo blando, pero
         * se detiene en el techo semanal de volumen por grupo y no repite grupos solo para llenar minutos.
         */
        LLENADO_ACOTADO
    }

    public static final int TOLERANCIA_V4_PCT = 15;

    public static final PlanningPolicy HISTORICA = new PlanningPolicy("PLAN-HIST-1", false, false,
            "V20 fijo al 70 %, V4 historico (minutos declarados frente a round(M x 1,15)), V24 y 4D vigentes",
            Duracion.PISO_70, false, false, false, false, false, false, false, false);
    public static final PlanningPolicy V4C = new PlanningPolicy("PLAN-V4-0.1", true, true,
            "Historica + V4 en segundos estimados (115 %)",
            Duracion.PISO_70, false, false, false, false, false, false, false, false);
    /** Diseno A de P-G1, para comparar: solo cambia la duracion (y V4 al 100 %, el maximo disponible). */
    public static final PlanningPolicy DUR_A = new PlanningPolicy("PLAN-DUR-A-0.1", true, true,
            "P-G1 diseno A: M = maximo; sin piso de V20; contenido por cobertura y dosis semanal con techo; V4 <= 100 %",
            Duracion.DOSIS_SEMANAL, true, true, false, false, false, false, false, false);
    /** Diseno B de P-G1, para comparar: llena hacia el 70 % como objetivo blando, con techo de volumen. */
    public static final PlanningPolicy DUR_B = new PlanningPolicy("PLAN-DUR-B-0.1", true, true,
            "P-G1 diseno B: M = maximo; V20 objetivo blando (no rechaza); techo semanal de volumen; V4 <= 100 %",
            Duracion.LLENADO_ACOTADO, true, true, false, false, false, false, false, false);
    /**
     * Version integrada del rediseno (se fija tras comparar A y B): duracion elegida + P-G3 (sin PULL falsas) +
     * P-C2 (impacto) + D-G3 (alternativas deterministas) + P-A1 (reduccion por fatiga en GM) + P-A2 (una
     * sustitucion ante desmotivacion). P-G2 (V15) POSPUESTA: no forma parte de ninguna version.
     * <p>
     * Cierre tecnico (orden definitiva, fase 4): {@code reparaCatalogoInactivo}. Un ejercicio HEREDADO que se
     * desactivo en el catalogo mientras estaba en un plan publicado ya no deja la semana siguiente NO_FACTIBLE por V1:
     * se sustituye SOLO esa colocacion por un equivalente estricto (mismo slot, patron, grupo y musculo; dificultad e
     * impacto no superiores; elegible para el perfil) o, si no lo hay, la semana no sale y el motivo lo dice. Con la
     * historica, lo de siempre (V1).
     */
    public static final PlanningPolicy REV2 = new PlanningPolicy("PLAN-REV-2.0", true, true,
            "Rediseno R/R': duracion por dosis (A), V4 <= 100 %, techo de volumen, PULL real, impacto, "
                    + "alternativas deterministas, reduccion por fatiga en GM y una sustitucion ante desmotivacion",
            Duracion.DOSIS_SEMANAL, true, true, true, true, true, true, true, true);

    public static final Map<String, PlanningPolicy> CATALOGO;

    static {
        var m = new LinkedHashMap<String, PlanningPolicy>();
        for (var p : List.of(HISTORICA, V4C, DUR_A, DUR_B, REV2)) m.put(p.id(), p);
        CATALOGO = Map.copyOf(m);
    }

    /** La politica de un plan ya guardado: null o vacio es la historica; desconocida, error. */
    public static PlanningPolicy deRegistro(String id) {
        if (id == null || id.isBlank()) return HISTORICA;
        var p = CATALOGO.get(id.trim());
        if (p == null) throw new PoliticaDesconocidaException(id);
        return p;
    }

    /** ¿Rige el piso del 70 % de V20 (version historica de la duracion)? */
    public boolean pisoV20() {
        return duracion == Duracion.PISO_70;
    }

    /** Tolerancia de V4 sobre M: 15 % historico; 0 cuando M es el maximo disponible (P-G1). */
    public int toleranciaV4Pct() {
        return v4Maximo ? 0 : TOLERANCIA_V4_PCT;
    }

    /** Limite exacto de V4-C en segundos: {@code M x 60 x 1,15}, en aritmetica entera (redondeo hacia abajo). */
    public static long limiteV4Segundos(int minutos) {
        return minutos * 60L * (100 + TOLERANCIA_V4_PCT) / 100;
    }

    /** Limite de V4 en segundos con la tolerancia de ESTA version. */
    public long limiteV4SegundosDe(int minutos) {
        return minutos * 60L * (100 + toleranciaV4Pct()) / 100;
    }

    /**
     * ¿Excede el techo de V4 una sesion? HISTORICA: minutos ({@code max(1, round(s / 60))}) frente a
     * {@code round(M x 1,15)}, igual que hoy. V4-C: {@code segundos x 100 > M x 60 x 115}. Con M como maximo
     * (P-G1): {@code segundos > M x 60}.
     */
    public boolean excedeTecho(long segundosEstimados, int minutosDeclaradosOEstimados, int minutosSesion) {
        if (v4Segundos) return segundosEstimados * 100 > minutosSesion * 60L * (100 + toleranciaV4Pct());
        return minutosDeclaradosOEstimados > (int) Math.round(minutosSesion * 1.15);
    }

    public static final class PoliticaDesconocidaException extends IllegalStateException {
        public PoliticaDesconocidaException(String id) {
            super("Version de politica de planificacion desconocida: '" + id + "'. No se reinterpreta con otra.");
        }
    }
}
