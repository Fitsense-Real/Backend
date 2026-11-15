package main.web.services.fitsense.planning.domain.model.valueobjects;

/**
 * Que restriccion del perfil excluye el APOYO SUSPENDIDO sobre un aparato sin soporte de pies
 * ({@code exercises.requires_suspended_support}, V53: fondos en paralelas, barra o anillas; silla del capitan;
 * muscle-up). Las dos opciones de {@code docs/FitSense/catalogo_integral/cierre/V53_DEFINITIVO.md} §2 son propuestas
 * PENDIENTES DE APROBACION y estan apagadas por defecto.
 * <ul>
 *   <li>{@link #APAGADO} (por defecto): el atributo no tiene efecto en ningun sitio. Comportamiento identico al
 *       anterior a V53.</li>
 *   <li>{@link #NO_COLGARSE} (opcion A, {@code fitsense.seguridad.apoyo-suspendido-en-no-colgarse=true}): «no
 *       colgarse» excluye tambien el apoyo suspendido.</li>
 *   <li>{@link #PERMISO_PROPIO} (opcion B, {@code fitsense.seguridad.permiso-apoyo-suspendido=true}): lo decide el
 *       permiso propio {@code user_profiles.allow_suspended_support} (V55). Sin responder (NULL) hereda «no colgarse»;
 *       «no colgarse» no cambia de significado.</li>
 * </ul>
 * Las dos opciones dan a la misma restriccion significados distintos: encender las dos a la vez es una configuracion
 * ambigua y la aplicacion NO ARRANCA ({@link #desdeInterruptores}).
 */
public enum ModoApoyoSuspendido {
    APAGADO,
    NO_COLGARSE,
    PERMISO_PROPIO;

    public static final String PROPIEDAD_OPCION_A = "fitsense.seguridad.apoyo-suspendido-en-no-colgarse";
    public static final String PROPIEDAD_OPCION_B = "fitsense.seguridad.permiso-apoyo-suspendido";

    /**
     * El modo que fijan los dos interruptores.
     *
     * @throws IllegalStateException si los dos estan encendidos: el significado seria ambiguo.
     */
    public static ModoApoyoSuspendido desdeInterruptores(boolean opcionA, boolean opcionB) {
        if (opcionA && opcionB) throw new IllegalStateException(mensajeAmbiguo());
        if (opcionB) return PERMISO_PROPIO;
        if (opcionA) return NO_COLGARSE;
        return APAGADO;
    }

    /** El mensaje del arranque fallido; publico para que la fachada del catalogo diga exactamente lo mismo. */
    public static String mensajeAmbiguo() {
        return "Configuracion ambigua del apoyo suspendido: " + PROPIEDAD_OPCION_A + "=true (opcion A, «no colgarse» "
                + "lo excluye) y " + PROPIEDAD_OPCION_B + "=true (opcion B, permiso propio) son excluyentes. "
                + "Enciende como mucho una de las dos.";
    }

    /** Si el atributo {@code requires_suspended_support} tiene algun efecto (A o B). */
    public boolean activo() {
        return this != APAGADO;
    }
}
