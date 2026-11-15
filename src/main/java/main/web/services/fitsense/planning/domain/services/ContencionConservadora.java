package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningProfile;

import java.util.Optional;
import java.util.Set;

/**
 * INC-CAT-CONS-v1 · CONTENCION CONSERVADORA DEL CIERRE TECNICO DEL CATALOGO (orden de 15/11/2026).
 * <p>
 * No hay validacion profesional del catalogo. Para la version tecnica conservadora
 * ({@code FITSENSE-CATALOGO-TECNICO-CONSERVADOR-v1}) los ejercicios con una incertidumbre MATERIAL que la revision
 * asistida por IA no pudo resolver no se recomiendan en planificaciones NUEVAS (generacion inicial, motor de reglas,
 * IA, sustitutos y regeneracion). No se borran, no cambian sus atributos ni sus dificultades, y no se tocan los planes
 * ya publicados ni el historial: un plan publicado se lee y se ejecuta como hasta ahora (la contencion no entra en
 * {@code PublishedPlanSafetyService}). Es reversible: {@code fitsense.catalogo.contencion-conservadora=false}.
 * <ul>
 *   <li><b>Identidad no determinada</b> ({@link #IDENTIDAD_NO_DETERMINADA}): la segunda revision visual (R2) marco la
 *       identidad del movimiento como AMBIGUA y las instrucciones o el nombre no coinciden con lo que muestra el GIF.
 *       Excluidos para todos. Los ya retenidos por INC-4C-03 (1192, 1294, 1298) siguen alli.</li>
 *   <li><b>Apoyo suspendido</b> ({@link #APOYO_SUSPENDIDO}, los 15 de V53): el cuerpo se sostiene sobre un aparato sin
 *       apoyo de pies. Excluidos salvo confirmacion EXPLICITA del permiso propio (V53-B, {@code allowSuspendedSupport =
 *       true}); un valor NULL no habilita nada. Hoy ningun usuario puede confirmarlo (Flutter no lo pregunta), asi que
 *       en la practica no se recomiendan a nadie. No cambia el significado de «no colgarse» ni activa V53 ni V53-B.</li>
 *   <li><b>Dificultad posiblemente subestimada</b> ({@link #DIFICULTAD_EN_DUDA}): 1113 conserva la dificultad 1
 *       almacenada (dictamen V39), pero R2 y V3 ven nivel 2; no se recomienda a principiantes mientras dure la
 *       discrepancia. La dificultad almacenada no cambia.</li>
 * </ul>
 */
public final class ContencionConservadora {

    public static final String CODIGO = "INC-CAT-CONS-v1";
    public static final String CAUSA_IDENTIDAD = "INC-CAT-CONS-v1:identidad";
    public static final String CAUSA_APOYO_SUSPENDIDO = "INC-CAT-CONS-v1:apoyo_suspendido";
    public static final String CAUSA_DIFICULTAD = "INC-CAT-CONS-v1:dificultad";

    /** R2 identidad AMBIGUA con instrucciones o nombre que no describen el GIF (registro P277-P302, abiertas), salvo 853 (V56). */
    public static final Set<Long> IDENTIDAD_NO_DETERMINADA = Set.of(257L, 261L, 310L, 320L, 427L, 494L, 535L,
            902L, 922L, 930L, 937L, 944L, 994L, 1068L, 1086L, 1124L, 1176L, 1177L, 1311L);
    // 853 sale de la lista: V56 corrige su nombre e instrucciones segun el GIF completo (REVISION_853.md). Su movimiento
    // queda determinado (elevacion alterna de rodilla y media sentadilla, siempre con un pie apoyado).

    /** V53: requires_suspended_support (situacion 2 de V53_DEFINITIVO.md). */
    public static final Set<Long> APOYO_SUSPENDIDO = Set.of(457L, 458L, 459L, 462L, 777L, 920L, 934L, 1069L, 1153L,
            1184L, 1303L, 1304L, 1306L, 1317L, 1335L);

    /** Dificultad almacenada posiblemente por debajo de la necesaria: no a principiantes (P313). */
    public static final Set<Long> DIFICULTAD_EN_DUDA = Set.of(1113L);

    private ContencionConservadora() {
    }

    /** La causa de contencion para este ejercicio y este perfil, o vacio si se puede recomendar. */
    public static Optional<String> causa(Long exerciseId, PlanningProfile profile) {
        if (exerciseId == null) return Optional.empty();
        if (IDENTIDAD_NO_DETERMINADA.contains(exerciseId)) return Optional.of(CAUSA_IDENTIDAD);
        if (APOYO_SUSPENDIDO.contains(exerciseId) && !apoyoSuspendidoConfirmado(profile))
            return Optional.of(CAUSA_APOYO_SUSPENDIDO);
        if (DIFICULTAD_EN_DUDA.contains(exerciseId) && principiante(profile)) return Optional.of(CAUSA_DIFICULTAD);
        return Optional.empty();
    }

    /** Solo un TRUE explicito (confirmado con V53-B) habilita el apoyo suspendido; NULL o false, no. */
    static boolean apoyoSuspendidoConfirmado(PlanningProfile profile) {
        return profile != null && profile.declaradas() != null
                && Boolean.TRUE.equals(profile.declaradas().allowSuspendedSupport());
    }

    /** Sin nivel declarado se trata como principiante: es lo conservador. */
    static boolean principiante(PlanningProfile profile) {
        return profile == null || profile.fitnessLevel() == null || "BEGINNER".equals(profile.fitnessLevel());
    }
}
