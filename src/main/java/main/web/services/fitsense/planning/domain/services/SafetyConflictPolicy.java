package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.ExerciseSafetyFacts;
import main.web.services.fitsense.planning.domain.model.valueobjects.SafetyProfile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Si un ejercicio choca con la SEGURIDAD de un participante, y por que.
 * <p>
 * Es la unica fuente de las causas que autorizan a quitar un ejercicio ya
 * prescrito: la sustitucion obligatoria (SUST-SEG-1), su excepcion en V8C y el
 * bloqueo de sesiones publicadas. Una causa sale de un dato comprobable -una
 * bandera del catalogo contra una restriccion del perfil, o una retencion o
 * contencion de seguridad con nombre-, nunca de que un id falte en el conjunto
 * elegible. Un bloqueo del participante, el equipamiento o el filtro funcional
 * NO son causas: son preferencias o disponibilidad.
 * <p>
 * Un ejercicio inactivo tampoco es una causa de seguridad por si mismo.
 * Dominio puro.
 */
public final class SafetyConflictPolicy {

    public static final String RETENIDO_INC_4C_03 = "INC-4C-03";
    public static final String CONTENIDO_INC_CAT_1374 = "INC-CAT-1374";
    /** Contencion por texto del catalogo contra bandera (caso 39, {@link ContencionTextoBandera}). */
    public static final String CONTENIDO_INC_CAT_C39 = ContencionTextoBandera.CODIGO;
    public static final String ALTO_IMPACTO = "alto_impacto";
    public static final String SUELO = "suelo";
    public static final String CARGA_AXIAL = "carga_axial";
    public static final String RODILLAS = "rodillas";
    public static final String SUSPENSION = "suspension";
    /**
     * V53 (decision de significado): apoyo suspendido -sin los pies en el suelo, sostenido sobre brazos o antebrazos, o
     * con los pies en correas-. Lo excluye la restriccion «no colgarse»; causa propia para que se distinga en el registro.
     */
    public static final String APOYO_SUSPENDIDO = "apoyo_suspendido";
    public static final String BRAZOS_ARRIBA = "brazos_arriba";
    public static final String INVERSION = "inversion";
    /**
     * P-02: el ejercicio provoco dolor a ESTE usuario y sigue excluido (user_safety_exclusions). Es una
     * causa comprobable -un evento registrado-, no una ausencia del conjunto elegible.
     */
    public static final String DOLOR_REGISTRADO = "dolor_registrado";

    /**
     * Fase 4 del cierre de PLAN-REV-2.0: el ejercicio HEREDADO esta desactivado en el catalogo (is_active = false).
     * NO es una causa de seguridad (un inactivo no se bloquea en la lectura ni en la ejecucion de la semana en curso);
     * es un dato comprobable que, solo con la politica que lo activa ({@code reparaCatalogoInactivo}), autoriza en la
     * semana DERIVADA la sustitucion de esa colocacion por un equivalente estricto. Por eso no esta en {@link #CAUSAS}.
     */
    public static final String CATALOGO_INACTIVO = "catalogo_inactivo";

    /** Todas las causas admitidas. Un motivo fuera de esta lista no autoriza nada. */
    public static final Set<String> CAUSAS = Set.of(RETENIDO_INC_4C_03, CONTENIDO_INC_CAT_1374, CONTENIDO_INC_CAT_C39,
            ALTO_IMPACTO,
            SUELO, CARGA_AXIAL, RODILLAS, SUSPENSION, APOYO_SUSPENDIDO, BRAZOS_ARRIBA, INVERSION, DOLOR_REGISTRADO);

    private SafetyConflictPolicy() {}

    /**
     * Las causas, en orden fijo. Vacio si no hay conflicto de seguridad.
     *
     * @param retenido  si el ejercicio esta retenido por INC-4C-03
     * @param contenido si INC-CAT-1374 lo contiene para este perfil
     */
    public static List<String> causas(ExerciseSafetyFacts f, SafetyProfile s, boolean retenido,
                                      boolean contenido) {
        return causas(f, s, retenido, contenido, false);
    }

    /**
     * Igual, con la contencion INC-CAT-C39 (texto del catalogo contra bandera).
     *
     * @param contenidoC39 si INC-CAT-C39 lo contiene para este perfil
     */
    public static List<String> causas(ExerciseSafetyFacts f, SafetyProfile s, boolean retenido,
                                      boolean contenido, boolean contenidoC39) {
        var causas = new ArrayList<String>();
        if (retenido) causas.add(RETENIDO_INC_4C_03);
        if (contenido) causas.add(CONTENIDO_INC_CAT_1374);
        if (contenidoC39) causas.add(CONTENIDO_INC_CAT_C39);
        if (f == null || s == null) return List.copyOf(causas);
        if (s.excludeHighImpact() && f.highImpact()) causas.add(ALTO_IMPACTO);
        if (s.excludeFloorWork() && f.requiresFloor()) causas.add(SUELO);
        if (s.excludeAxialLoad() && f.axialLoad()) causas.add(CARGA_AXIAL);
        if (s.excludeKneeling() && f.requiresKneeling()) causas.add(RODILLAS);
        if (s.excludeHanging() && f.requiresHanging()) causas.add(SUSPENSION);
        if (s.excludeHanging() && f.requiresSuspendedSupport()) causas.add(APOYO_SUSPENDIDO);
        if (s.excludeOverhead() && f.requiresOverhead()) causas.add(BRAZOS_ARRIBA);
        if (s.excludeInversion() && f.requiresInversion()) causas.add(INVERSION);
        return List.copyOf(causas);
    }

    /** El motivo registrado: las causas unidas por «+». */
    public static String motivo(List<String> causas) {
        return String.join("+", causas);
    }

    /** Lo que autoriza una sustitucion obligatoria en una semana derivada: las causas de seguridad y el inactivo. */
    public static final Set<String> CAUSAS_DE_SUSTITUCION;

    static {
        var todas = new java.util.HashSet<>(CAUSAS);
        todas.add(CATALOGO_INACTIVO);
        CAUSAS_DE_SUSTITUCION = Set.copyOf(todas);
    }

    /** Si un motivo autoriza una sustitucion obligatoria: hecho SOLO de causas de seguridad o del inactivo. */
    public static boolean esMotivoDeSustitucion(String motivo) {
        if (motivo == null || motivo.isBlank()) return false;
        return Arrays.stream(motivo.split("\\+")).allMatch(CAUSAS_DE_SUSTITUCION::contains);
    }

    /** Si el motivo es (solo) la desactivacion en el catalogo: exige el equivalente estricto. */
    public static boolean esCatalogoInactivo(String motivo) {
        return CATALOGO_INACTIVO.equals(motivo);
    }

    /** Si un motivo esta hecho SOLO de causas admitidas, y de al menos una. */
    public static boolean esMotivoDeSeguridad(String motivo) {
        if (motivo == null || motivo.isBlank()) return false;
        return Arrays.stream(motivo.split("\\+")).allMatch(CAUSAS::contains);
    }
}
