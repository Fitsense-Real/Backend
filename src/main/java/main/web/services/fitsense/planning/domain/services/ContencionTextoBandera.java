package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.SafetyProfile;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * CONTENCION INC-CAT-C39 (13/11/2026) · TEXTO DEL CATALOGO CONTRA BANDERA DE SEGURIDAD.
 * <p>
 * Auditoria del caso real del usuario 39 ({@code docs/FitSense/auditoria_caso_39/}). V37 y V38 relajaron banderas
 * de seguridad ({@code requires_hanging}, {@code requires_overhead}, {@code requires_floor}) por lo que se veia en
 * el GIF («restriccion de mas»), pero NO actualizaron el nombre ni las instrucciones, que siguen describiendo la
 * ejecucion restringida. Algunos ejercicios nunca marcados tienen el mismo defecto. La app muestra el NOMBRE y el
 * EQUIPO (no el GIF) y la IA ve el nombre: quien declaro no colgarse puede recibir «Dominadas con agarre estrecho».
 * <p>
 * Que hace: cada id solo sale del conjunto elegible -motor de reglas e IA, que leen el mismo conjunto- y queda como
 * causa de seguridad en planes publicados ({@link SafetyConflictPolicy#CONTENIDO_INC_CAT_C39}) para los perfiles
 * que declaran alguna de SUS restricciones. Para el resto de perfiles el ejercicio sigue igual: su texto y su GIF
 * describen movimientos que esos perfiles admiten.
 * <p>
 * Que NO hace: no cambia banderas, ni nombres, ni instrucciones, ni dosis, ni patrones, ni dificultad, ni borra
 * filas. No es una regla nueva: aplica las restricciones que el perfil YA declara a lo que el texto le pide hacer.
 * <p>
 * Criterio de entrada, comprobable: el nombre o las instrucciones en espanol PIDEN la accion restringida (colgarse,
 * brazos por encima de la cabeza, tumbarse o sentarse en el suelo) y la bandera dice lo contrario. Que movimiento
 * es el correcto -el del texto o el del GIF- queda PENDIENTE DE REVISION PROFESIONAL; mientras tanto, se contiene.
 * <p>
 * SIN INTERRUPTOR, como INC-4C-03 e INC-CAT-1374: es seguridad, no medicion. Un id sale de aqui solo cuando una
 * migracion auditada alinee texto y bandera con revision profesional.
 * <p>
 * REDUCCION A 8 (catalogo integral, PR #56-#59): la revision visual de los 26 ids originales confirmo que en todos
 * el GIF y la bandera coinciden y lo erroneo era el texto de la fuente. V49 reescribio ese texto segun el GIF, asi
 * que 18 ids salen del registro por completo (316, 1152, 1272, 1309, 1322, 55, 224, 637, 741, 875, 1062, 1063, 1093,
 * 1095, 1156, 1181, 1193, 1311) y 1301, 1303 y 1304 pierden la parte de la contencion cuyo texto ya no lo pide
 * (1301 brazos sobre la cabeza; 1303 y 1304 suelo). Ninguno sale porque haga falta para generar un plan: sale porque
 * su texto ya no contradice su bandera ({@code ContencionTextoBanderaIT}, prueba «liberados»). Siguen contenidos los
 * 8 cuyo MOVIMIENTO, no su texto, queda pendiente de decision profesional
 * ({@code docs/FitSense/catalogo_integral/PROPUESTA_APOYO_SUSPENDIDO.md}):
 * <ul>
 *   <li>colgarse (apoyo suspendido, sin clase en el esquema): 1153, 1301, 1303, 1304, 1306;</li>
 *   <li>brazos sobre la cabeza (curl «alto», posicion del brazo en el limite de la definicion): 131, 400, 448.</li>
 * </ul>
 */
public final class ContencionTextoBandera {

    public static final String CODIGO = "INC-CAT-C39";

    /** Restriccion declarada por el perfil que activa la contencion de un id. */
    public enum Restriccion { COLGARSE, SOBRE_LA_CABEZA, SUELO }

    /** Por que se contiene un id: restricciones que lo activan y la contradiccion verificada. */
    public record Contencion(Set<Restriccion> restricciones, String contradiccion) {}

    private static Contencion c(String contradiccion, Restriccion... r) {
        return new Contencion(Set.copyOf(EnumSet.of(r[0], r)), contradiccion);
    }

    private static final Restriccion COLG = Restriccion.COLGARSE;
    private static final Restriccion CAB = Restriccion.SOBRE_LA_CABEZA;

    /**
     * El registro (8 ids tras la reduccion). Cada entrada cita por que el movimiento -texto ya corregido por V49 y
     * GIF- sigue siendo incompatible o dudoso para quien declara la restriccion.
     */
    static final Map<Long, Contencion> CONTENIDOS = Map.ofEntries(
            // --- Colgarse: apoyo suspendido (el cuerpo sin apoyo en el suelo, sostenido por brazos o antebrazos).
            //     requires_hanging no lo cubre y la decision de clasificarlo es profesional (PROPUESTA_APOYO_SUSPENDIDO).
            Map.entry(1153L, c("Fondos en anillas (GIF): apoyo sobre los brazos extendidos, pies sin apoyo; "
                    + "requires_hanging = no. Apoyo suspendido pendiente de decision profesional", COLG)),
            Map.entry(1301L, c("Plancha con los pies en correas de suspension (GIF): pies suspendidos; V38 quito "
                    + "colgarse. Brazos sobre la cabeza ya no aplica: V49 corrigio el texto", COLG)),
            Map.entry(1303L, c("Silla romana / estacion de fondos (GIF): apoyo en antebrazos con los pies sin apoyo; "
                    + "requires_hanging = no. Suelo ya no aplica: V49 corrigio el texto", COLG)),
            Map.entry(1304L, c("Gemelo del 1303 (GIF: silla romana, apoyo en antebrazos, pies sin apoyo); "
                    + "requires_hanging = no. Suelo ya no aplica: V49 corrigio el texto", COLG)),
            Map.entry(1306L, c("Elevacion de rodillas en silla romana (GIF): apoyo en antebrazos con los pies sin "
                    + "apoyo; nunca marcado como colgarse", COLG)),
            // --- Brazos sobre la cabeza: curl «alto» con el brazo abierto a la altura del hombro y las manos hacia
            //     la cabeza; la posicion queda en el limite de la definicion de requires_overhead.
            Map.entry(131L, c("Curl alto (GIF): brazo a la altura del hombro y manos hacia la cabeza; "
                    + "requires_overhead = no (V38). Limite de la definicion, pendiente profesional", CAB)),
            Map.entry(400L, c("Curl alto en polea (GIF): brazos a la altura del hombro y manos hacia la cabeza; "
                    + "requires_overhead = no. Limite de la definicion, pendiente profesional", CAB)),
            Map.entry(448L, c("Curl alto en polea sentado (GIF): brazos a la altura del hombro y manos hacia la "
                    + "cabeza; requires_overhead = no. Limite de la definicion, pendiente profesional", CAB)));

    /**
     * Los 18 ids que la PR #55 original contenia y que V49 libero por completo: su texto ya describe el GIF y no
     * contradice su bandera. Solo documentacion y pruebas; no filtra nada.
     */
    public static final Set<Long> LIBERADOS_POR_V49 = Set.of(316L, 1152L, 1272L, 1309L, 1322L, 55L, 224L, 637L, 741L,
            875L, 1062L, 1063L, 1093L, 1095L, 1156L, 1181L, 1193L, 1311L);

    private ContencionTextoBandera() {}

    /** Las restricciones del perfil que este registro sabe leer. */
    static Set<Restriccion> declaradas(SafetyProfile s) {
        var r = EnumSet.noneOf(Restriccion.class);
        if (s == null) return r;
        if (s.excludeHanging()) r.add(Restriccion.COLGARSE);
        if (s.excludeOverhead()) r.add(Restriccion.SOBRE_LA_CABEZA);
        if (s.excludeFloorWork()) r.add(Restriccion.SUELO);
        return r;
    }

    /** Si INC-CAT-C39 contiene este ejercicio para un perfil con esta seguridad. */
    public static boolean contiene(Long exerciseId, SafetyProfile seguridad) {
        if (exerciseId == null) return false;
        var c = CONTENIDOS.get(exerciseId);
        if (c == null) return false;
        var d = declaradas(seguridad);
        return c.restricciones().stream().anyMatch(d::contains);
    }

    /** Los ids que se contienen para esta seguridad, ordenados (para el log y las pruebas). */
    public static Set<Long> contenidos(SafetyProfile seguridad) {
        var d = declaradas(seguridad);
        var ids = new TreeSet<Long>();
        CONTENIDOS.forEach((id, c) -> { if (c.restricciones().stream().anyMatch(d::contains)) ids.add(id); });
        return ids;
    }

    /** El registro completo, solo lectura (documentacion y pruebas). */
    public static Map<Long, Contencion> registro() {
        return CONTENIDOS;
    }
}
