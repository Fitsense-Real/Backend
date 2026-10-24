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
    public static final String ALTO_IMPACTO = "alto_impacto";
    public static final String SUELO = "suelo";
    public static final String CARGA_AXIAL = "carga_axial";
    public static final String RODILLAS = "rodillas";
    public static final String SUSPENSION = "suspension";
    public static final String BRAZOS_ARRIBA = "brazos_arriba";
    public static final String INVERSION = "inversion";

    /** Todas las causas admitidas. Un motivo fuera de esta lista no autoriza nada. */
    public static final Set<String> CAUSAS = Set.of(RETENIDO_INC_4C_03, CONTENIDO_INC_CAT_1374, ALTO_IMPACTO,
            SUELO, CARGA_AXIAL, RODILLAS, SUSPENSION, BRAZOS_ARRIBA, INVERSION);

    private SafetyConflictPolicy() {}

    /**
     * Las causas, en orden fijo. Vacio si no hay conflicto de seguridad.
     *
     * @param retenido  si el ejercicio esta retenido por INC-4C-03
     * @param contenido si INC-CAT-1374 lo contiene para este perfil
     */
    public static List<String> causas(ExerciseSafetyFacts f, SafetyProfile s, boolean retenido,
                                      boolean contenido) {
        var causas = new ArrayList<String>();
        if (retenido) causas.add(RETENIDO_INC_4C_03);
        if (contenido) causas.add(CONTENIDO_INC_CAT_1374);
        if (f == null || s == null) return List.copyOf(causas);
        if (s.excludeHighImpact() && f.highImpact()) causas.add(ALTO_IMPACTO);
        if (s.excludeFloorWork() && f.requiresFloor()) causas.add(SUELO);
        if (s.excludeAxialLoad() && f.axialLoad()) causas.add(CARGA_AXIAL);
        if (s.excludeKneeling() && f.requiresKneeling()) causas.add(RODILLAS);
        if (s.excludeHanging() && f.requiresHanging()) causas.add(SUSPENSION);
        if (s.excludeOverhead() && f.requiresOverhead()) causas.add(BRAZOS_ARRIBA);
        if (s.excludeInversion() && f.requiresInversion()) causas.add(INVERSION);
        return List.copyOf(causas);
    }

    /** El motivo registrado: las causas unidas por «+». */
    public static String motivo(List<String> causas) {
        return String.join("+", causas);
    }

    /** Si un motivo esta hecho SOLO de causas admitidas, y de al menos una. */
    public static boolean esMotivoDeSeguridad(String motivo) {
        if (motivo == null || motivo.isBlank()) return false;
        return Arrays.stream(motivo.split("\\+")).allMatch(CAUSAS::contains);
    }
}
