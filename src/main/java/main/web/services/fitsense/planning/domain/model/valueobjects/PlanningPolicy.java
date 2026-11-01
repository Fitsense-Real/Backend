package main.web.services.fitsense.planning.domain.model.valueobjects;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * VERSION DE LA POLITICA DE PLANIFICACION con la que se genera y se valida un plan.
 * <p>
 * Esta rama solo incluye el versionado y V4-C:
 * <ul>
 *   <li>{@link #HISTORICA} ({@code PLAN-HIST-1}): la de siempre. V4 compara los minutos declarados o estimados con
 *       {@code round(M x 1,15)}.</li>
 *   <li>{@link #V4C} ({@code PLAN-V4-0.1}, candidata): V4 se comprueba con los SEGUNDOS ESTIMADOS del contenido
 *       ({@code segundos x 100 <= M x 60 x 115}), declare lo que declare la sesion.</li>
 * </ul>
 * La lista cerrada de la base (V44) admite tambien otras versiones que llegaran con otros cambios; este catalogo
 * solo reconoce las dos anteriores. Un plan sin version registrada es {@link #HISTORICA}. Una version desconocida
 * es un error: no se reinterpreta con otra.
 */
public record PlanningPolicy(String id, boolean candidata, boolean v4Segundos, String descripcion) {

    public static final int TOLERANCIA_V4_PCT = 15;

    public static final PlanningPolicy HISTORICA = new PlanningPolicy("PLAN-HIST-1", false, false,
            "V20 fijo al 70 %, V4 historico (minutos declarados frente a round(M x 1,15)), V24 y 4D vigentes");
    public static final PlanningPolicy V4C = new PlanningPolicy("PLAN-V4-0.1", true, true,
            "Historica + V4 en segundos estimados (115 %)");

    public static final Map<String, PlanningPolicy> CATALOGO;

    static {
        var m = new LinkedHashMap<String, PlanningPolicy>();
        for (var p : List.of(HISTORICA, V4C)) m.put(p.id(), p);
        CATALOGO = Map.copyOf(m);
    }

    /** La politica de un plan ya guardado: null o vacio es la historica; desconocida, error. */
    public static PlanningPolicy deRegistro(String id) {
        if (id == null || id.isBlank()) return HISTORICA;
        var p = CATALOGO.get(id.trim());
        if (p == null) throw new PoliticaDesconocidaException(id);
        return p;
    }

    /** Limite exacto de V4-C en segundos: {@code M x 60 x 1,15}, en aritmetica entera (redondeo hacia abajo). */
    public static long limiteV4Segundos(int minutos) {
        return minutos * 60L * (100 + TOLERANCIA_V4_PCT) / 100;
    }

    /**
     * ¿Excede el techo de V4 una sesion? HISTORICA: minutos ({@code max(1, round(s / 60))}) frente a
     * {@code round(M x 1,15)}, igual que hoy. V4-C: {@code segundos x 100 > M x 60 x 115}.
     */
    public boolean excedeTecho(long segundosEstimados, int minutosDeclaradosOEstimados, int minutosSesion) {
        if (v4Segundos) return segundosEstimados * 100 > minutosSesion * 60L * (100 + TOLERANCIA_V4_PCT);
        return minutosDeclaradosOEstimados > (int) Math.round(minutosSesion * 1.15);
    }

    public static final class PoliticaDesconocidaException extends IllegalStateException {
        public PoliticaDesconocidaException(String id) {
            super("Version de politica de planificacion desconocida: '" + id + "'. No se reinterpreta con otra.");
        }
    }
}
