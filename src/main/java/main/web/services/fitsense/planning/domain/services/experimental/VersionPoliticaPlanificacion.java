package main.web.services.fitsense.planning.domain.services.experimental;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * VERSIONADO DE POLITICAS (EXPERIMENTAL, NO CABLEADO; propuesta D-11). Identifica con que reglas se genero y valido un
 * plan, para que una politica nueva no reinterprete retroactivamente planes, adaptaciones, reducciones, progresiones,
 * reincorporaciones ni trazas de seguridad.
 * <p>
 * Solo {@link #HISTORICA} esta activa; las demas son candidatas y {@link #activable()} es false hasta su aprobacion.
 * La traza ({@link #traza}) es lo que se guardaria junto al plan si se aprobara la migracion propuesta.
 */
public record VersionPoliticaPlanificacion(String id, boolean activa, String v20, String v4, String dosis4d,
                                           String tope4d, String modeloE, String descripcion) {

    public static final VersionPoliticaPlanificacion HISTORICA = new VersionPoliticaPlanificacion("PLAN-HIST-1", true,
            "V20 70 % fijo (calculation_configs)", TechoV4Versionado.HISTORICO.name(), "4D v3.3 / 4D v4",
            "V24 6/7/8", "DESACTIVADO", "Politica vigente en generation (afef98a)");

    public static final VersionPoliticaPlanificacion C2_CANDIDATA = new VersionPoliticaPlanificacion("PLAN-C2-0.1", false,
            PisoV20Acotado.c2c().version(), TechoV4Versionado.HISTORICO.name(), "4D v3.3 / 4D v4", "V24 6/7/8",
            "DESACTIVADO", "C2 candidata: V20 acotado solo si V20 es la unica regla incumplida");

    public static final VersionPoliticaPlanificacion V4_CORREGIDA = new VersionPoliticaPlanificacion("PLAN-V4-0.1", false,
            "V20 70 % fijo (calculation_configs)", TechoV4Versionado.SEGUNDOS.name(), "4D v3.3 / 4D v4", "V24 6/7/8",
            "DESACTIVADO", "V4 en segundos estrictos; resto igual a la historica");

    public static final VersionPoliticaPlanificacion MODELO_E = new VersionPoliticaPlanificacion("PLAN-E-0.1", false,
            "sin V20 (E1-E4)", TechoV4Versionado.SEGUNDOS.name(), "4D base", "techos E (sin aprobar)",
            ParametrosModeloE.VERSION, "Laboratorio: solo con parametros E aprobados");

    public static final Map<String, VersionPoliticaPlanificacion> CATALOGO = Map.of(HISTORICA.id, HISTORICA,
            C2_CANDIDATA.id, C2_CANDIDATA, V4_CORREGIDA.id, V4_CORREGIDA, MODELO_E.id, MODELO_E);

    /** Un plan sin version registrada es historico: no se reinterpreta. */
    public static VersionPoliticaPlanificacion deUnPlan(String idRegistrado) {
        if (idRegistrado == null || idRegistrado.isBlank()) return HISTORICA;
        return Optional.ofNullable(CATALOGO.get(idRegistrado))
                .orElseThrow(() -> new IllegalArgumentException("version de politica desconocida: " + idRegistrado));
    }

    /** Ninguna candidata se puede activar desde el codigo: hace falta aprobacion y una version nueva de configuracion. */
    public boolean activable() {
        return activa;
    }

    /** Lo que se guardaria con cada plan: reglas, parametros efectivos y contexto. */
    public Map<String, Object> traza(int pisoV20Usado, double techoV4Minutos, String versionCalculo, String contexto) {
        var m = new LinkedHashMap<String, Object>();
        m.put("politica", id);
        m.put("v20", v20);
        m.put("piso_v20_usado_pct", pisoV20Usado);
        m.put("v4", v4);
        m.put("techo_v4_minutos", techoV4Minutos);
        m.put("dosis", dosis4d);
        m.put("tope", tope4d);
        m.put("modelo_e", modeloE);
        m.put("version_calculo", versionCalculo);
        m.put("contexto", contexto);
        return m;
    }
}
