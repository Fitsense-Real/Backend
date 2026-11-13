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
    private static final Restriccion SUE = Restriccion.SUELO;

    /** El registro. Cada entrada cita la contradiccion verificada en el catalogo (ver CORRECCIONES_SEGURIDAD.md). */
    static final Map<Long, Contencion> CONTENIDOS = Map.ofEntries(
            // --- Colgarse / brazos arriba: el texto pide colgarse, la bandera (relajada en V38) dice que no
            Map.entry(316L, c("Nombre «Dominadas con agarre estrecho» e instrucciones «Cuelgate de una barra de "
                    + "dominadas»; V38 quito colgarse y brazos arriba por el GIF (remo invertido)", COLG, CAB)),
            Map.entry(1301L, c("Instrucciones «Cuelgate de una barra de dominadas con los brazos completamente "
                    + "extendidos»; V38 quito colgarse y brazos arriba y paso el equipo a correas", COLG, CAB)),
            Map.entry(1306L, c("Instrucciones «Cuelgate de las barras paralelas»; nunca marcado como colgarse "
                    + "(GIF: apoyo en antebrazos, silla romana)", COLG)),
            Map.entry(1153L, c("Instrucciones «Comienza colgandote de las anillas»; no marcado como colgarse "
                    + "(fondos en anillas: apoyo suspendido)", COLG)),
            // --- Apoyo en paralelas / estacion de fondos con texto de otro ejercicio (pendiente profesional)
            Map.entry(1303L, c("Equipo «parallel bars» (GIF: apoyo en la estacion de fondos) e instrucciones de "
                    + "suelo «Tumbate sobre tu espalda»; V38 quito suelo", COLG, SUE)),
            Map.entry(1304L, c("Gemelo del 1303: equipo «parallel bars» (GIF: estacion de fondos) e instrucciones "
                    + "«Tumbate sobre tu espalda»; V38 quito suelo", COLG, SUE)),
            Map.entry(1152L, c("Instrucciones de fondos en paralelas «Colocate entre dos barras paralelas con los "
                    + "brazos completamente extendidos»; catalogo y GIF: en el suelo, sin paralelas (V38)", COLG)),
            // --- Brazos por encima de la cabeza: el texto lo pide, la bandera dice que no
            Map.entry(131L, c("Nombre «…sobre la cabeza…» e instrucciones «brazo completamente extendido por "
                    + "encima de la cabeza»; V38 quito brazos arriba", CAB)),
            Map.entry(400L, c("Nombre «Curl por encima de la cabeza en cable»; bandera brazos arriba = no", CAB)),
            Map.entry(448L, c("Nombre «Curl por encima de la cabeza sentado en cable»; bandera brazos arriba = no",
                    CAB)),
            Map.entry(1272L, c("Instrucciones «Recibe la barra por encima de la cabeza»; bandera brazos arriba = no",
                    CAB)),
            Map.entry(1309L, c("Instrucciones «Eleva lentamente los brazos por encima de la cabeza»; V38 quito "
                    + "brazos arriba (GIF: a la altura del hombro)", CAB)),
            Map.entry(1322L, c("Instrucciones «Extiende un brazo por encima de la cabeza»; V38 quito brazos arriba "
                    + "(GIF: brazo cruzado ante el pecho)", CAB)),
            // --- Suelo: V38 quito suelo por el GIF (banco, de pie, taburete) y las instrucciones siguen en el suelo
            Map.entry(55L, c("Instrucciones «Sientate en el suelo»; V38 quito suelo (GIF: de pie)", SUE)),
            Map.entry(224L, c("Instrucciones «Tumbate boca arriba en una esterilla»; V38 quito suelo (GIF: banco)",
                    SUE)),
            Map.entry(637L, c("Instrucciones «Tumbate boca arriba… pies apoyados en el suelo»; V38 quito suelo "
                    + "(GIF: banco)", SUE)),
            Map.entry(741L, c("Instrucciones «Tumbate de lado»; V38 quito suelo (GIF: banco)", SUE)),
            Map.entry(875L, c("Instrucciones «Tumbate boca arriba… levanta las piernas del suelo»; V38 quito suelo "
                    + "(GIF: banco)", SUE)),
            Map.entry(1062L, c("Instrucciones «Tumbate sobre tu espalda… levanta las piernas del suelo»; V38 quito "
                    + "suelo (GIF: banco)", SUE)),
            Map.entry(1063L, c("Instrucciones «Tumbate sobre tu espalda… pies apoyados en el suelo»; V38 quito "
                    + "suelo (GIF: banco declinado)", SUE)),
            Map.entry(1093L, c("Instrucciones «Comienza en posicion de flexion… baja el pecho hacia el suelo»; V38 "
                    + "quito suelo (GIF: de pie elevando rodillas)", SUE)),
            Map.entry(1095L, c("Instrucciones «Comienza en posicion de flexion… baja el pecho hacia el suelo»; V38 "
                    + "quito suelo (GIF: de pie)", SUE)),
            Map.entry(1156L, c("Instrucciones «Sientate en el suelo con las piernas extendidas»; V38 quito suelo "
                    + "(GIF: asiento inclinado)", SUE)),
            Map.entry(1181L, c("Instrucciones «Sientate en el suelo con la espalda contra una pared»; V38 quito "
                    + "suelo (GIF: sentadilla isometrica contra la pared)", SUE)),
            Map.entry(1193L, c("Instrucciones «Sientate en el suelo con las piernas extendidas»; V38 quito suelo "
                    + "(GIF: taburete)", SUE)),
            Map.entry(1311L, c("Instrucciones «Tumbate de lado»; V38 quito suelo (GIF: de pie)", SUE)));

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
