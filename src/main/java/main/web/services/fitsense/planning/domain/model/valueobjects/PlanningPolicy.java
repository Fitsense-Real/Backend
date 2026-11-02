package main.web.services.fitsense.planning.domain.model.valueobjects;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * VERSION DE LA POLITICA DE PLANIFICACION con la que se genera y se valida un plan.
 * <p>
 * Cada componente se activa por separado y solo a traves de una version explicita del catalogo:
 * <ul>
 *   <li>{@link #c2d}: excepcion de V20 por saturacion de contenido (C2-D). La generacion sigue intentando el 70 %;
 *       solo si V20 es la UNICA infraccion y las sesiones cortas estan saturadas, V20 deja de bloquear.</li>
 *   <li>{@link #v4Segundos}: V4-C. El techo se comprueba con los SEGUNDOS ESTIMADOS del contenido
 *       ({@code segundos x 100 <= M x 60 x 115}), no con la duracion declarada.</li>
 * </ul>
 * Combinacion: {@code PLAN-C2D-V4-0.1} es la UNICA version con los dos; elegir {@code PLAN-C2D-0.1} no activa V4-C ni
 * al reves. La elegibilidad funcional ({@link #elegibilidadFuncional}, EF-0.1) tiene sus propias versiones
 * ({@code PLAN-EF-0.1}, {@code PLAN-C2D-EF-0.1}, {@code PLAN-C2D-V4-EF-0.1}); ninguna anterior la incluye. {@code PLAN-C2-0.1} no se usa: en la verificacion anterior designaba la variante C2-C.
 * <p>
 * Un plan sin version registrada es {@link #HISTORICA}. Una version desconocida es un error (no hay retorno
 * silencioso a la historica). El modelo E no tiene version en este catalogo.
 */
public record PlanningPolicy(String id, boolean candidata, boolean c2d, boolean v4Segundos,
                             boolean elegibilidadFuncional, String descripcion) {

    public static final int TOLERANCIA_V4_PCT = 15;

    public static final PlanningPolicy HISTORICA = new PlanningPolicy("PLAN-HIST-1", false, false, false, false,
            "V20 fijo al 70 %, V4 historico (minutos declarados frente a round(M x 1,15)), V24 y 4D vigentes");
    public static final PlanningPolicy C2D = new PlanningPolicy("PLAN-C2D-0.1", true, true, false, false,
            "Historica + excepcion de V20 por saturacion de contenido");
    public static final PlanningPolicy V4C = new PlanningPolicy("PLAN-V4-0.1", true, false, true, false,
            "Historica + V4 en segundos estimados (115 %)");
    public static final PlanningPolicy C2D_V4C = new PlanningPolicy("PLAN-C2D-V4-0.1", true, true, true, false,
            "Historica + C2-D + V4-C (combinacion explicita)");

    /** Historica + elegibilidad funcional (EF-0.1). Version propia: no cambia la interpretacion de las demas. */
    public static final PlanningPolicy EF = new PlanningPolicy("PLAN-EF-0.1", true, false, false, true,
            "Historica + elegibilidad funcional EF-0.1 (patron compatible con el enfoque y ancla obligatoria)");
    public static final PlanningPolicy C2D_EF = new PlanningPolicy("PLAN-C2D-EF-0.1", true, true, false, true,
            "C2-D + elegibilidad funcional EF-0.1");
    public static final PlanningPolicy C2D_V4C_EF = new PlanningPolicy("PLAN-C2D-V4-EF-0.1", true, true, true, true,
            "C2-D + V4-C + elegibilidad funcional EF-0.1");

    public static final Map<String, PlanningPolicy> CATALOGO;

    static {
        var m = new LinkedHashMap<String, PlanningPolicy>();
        for (var p : List.of(HISTORICA, C2D, V4C, C2D_V4C, EF, C2D_EF, C2D_V4C_EF)) m.put(p.id(), p);
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
