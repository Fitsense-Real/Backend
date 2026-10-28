package main.web.services.fitsense.iam.infrastructure.authorization.sfs.configuration;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

/**
 * Origenes de navegador autorizados ({@code FITSENSE_CORS_ORIGINS}), separados por
 * comas y exactos: esquema, host y puerto, sin ruta ni barra final. Vacio no
 * autoriza ninguno, que es lo correcto para la app Flutter (no es un navegador y
 * no envia Origin).
 * <p>
 * Se rechaza al arrancar cualquier comodin y cualquier valor que no sea un origen,
 * para que un error de configuracion no termine abriendo el backend.
 */
public final class CorsOrigins {

    private CorsOrigins() {}

    public static List<String> parse(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(o -> !o.isEmpty())
                .map(CorsOrigins::validar)
                .distinct()
                .toList();
    }

    private static String validar(String origen) {
        if (origen.contains("*"))
            throw new IllegalStateException("FITSENSE_CORS_ORIGINS no admite comodines: " + origen);
        URI uri;
        try {
            uri = URI.create(origen);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Origen CORS invalido: " + origen, e);
        }
        boolean esquema = "https".equals(uri.getScheme()) || "http".equals(uri.getScheme());
        boolean soloOrigen = uri.getHost() != null
                && (uri.getRawPath() == null || uri.getRawPath().isEmpty())
                && uri.getRawQuery() == null && uri.getRawFragment() == null && uri.getUserInfo() == null;
        if (!esquema || !soloOrigen)
            throw new IllegalStateException(
                    "Origen CORS invalido (se espera esquema://host[:puerto], sin ruta): " + origen);
        return origen;
    }
}
