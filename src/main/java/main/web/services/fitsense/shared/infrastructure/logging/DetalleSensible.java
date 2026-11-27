package main.web.services.fitsense.shared.infrastructure.logging;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Map;

/**
 * S-1 (auditoria de preproduccion, P0-01): que los logs NO lleven datos de salud ni de entrenamiento personales.
 * <p>
 * Por omision, los registros de la salida de la IA y del dolor se escriben en forma <b>tecnica</b>: longitudes,
 * recuentos y una huella corta (SHA-256 truncado a 12 hex, de un texto de alta entropia, sin sal: sirve para cotejar
 * con {@code plan_generation_predictions.output} y no permite reconstruir el contenido). El detalle completo (la
 * salida de la IA, los ids de los ejercicios que dolieron, sus derivaciones) solo se escribe con
 * {@code fitsense.logging.sensitive-detail=true}, que <b>impide arrancar con el perfil {@code prod}</b>
 * ({@link DetalleSensibleConfiguracion}): no se puede activar en un entorno con participantes cambiando una variable.
 * <p>
 * Es estatico a proposito: lo usan servicios de dominio que no son beans (validadores) y su valor lo fija una sola
 * vez la configuracion al arrancar. Sin contexto de Spring (pruebas unitarias) vale {@code false}: el modo seguro.
 */
public final class DetalleSensible {

    private static volatile boolean permitido = false;

    private DetalleSensible() {
    }

    /** ¿Se puede escribir el detalle sensible? Solo fuera de {@code prod} y con la propiedad encendida. */
    public static boolean permitido() {
        return permitido;
    }

    /** Lo fija la configuracion al arrancar; las pruebas que comprueban el modo detallado lo restauran despues. */
    public static void fijar(boolean valor) {
        permitido = valor;
    }

    /** El texto completo con detalle permitido; si no, {@code caracteres=N huella=xxxxxxxxxxxx}. */
    public static String texto(String s) {
        if (permitido) return s;
        return resumen(s);
    }

    /** Siempre la forma tecnica, haya o no detalle permitido. */
    public static String resumen(String s) {
        if (s == null) return "caracteres=0";
        return "caracteres=" + s.length() + " huella=" + huella(s);
    }

    /** La coleccion con detalle permitido; si no, solo su tamano ({@code n=3}). */
    public static Object ids(Collection<?> c) {
        if (permitido) return c;
        return "n=" + (c == null ? 0 : c.size());
    }

    /** El mapa con detalle permitido; si no, solo su tamano. */
    public static Object mapa(Map<?, ?> m) {
        if (permitido) return m;
        return "n=" + (m == null ? 0 : m.size());
    }

    /** Un valor cualquiera con detalle permitido; si no, la marca {@code [omitido]}. */
    public static Object valor(Object o) {
        return permitido ? o : "[omitido]";
    }

    /** SHA-256 truncado a 12 hex: identifica sin revelar. */
    public static String huella(String s) {
        try {
            var d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d).substring(0, 12);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
