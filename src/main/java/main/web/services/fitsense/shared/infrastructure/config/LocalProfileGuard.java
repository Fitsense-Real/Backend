package main.web.services.fitsense.shared.infrastructure.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

/**
 * Con el perfil {@code local} o {@code local-dev}, impide arrancar contra una base que no sea local (cierre definitivo, 3.1). Evita que
 * una variable de entorno olvidada (p. ej. una URL de Neon o Railway) haga que el desarrollo o las pruebas manuales
 * escriban en una base compartida. Fuera del perfil local no hace nada.
 * <p>
 * Es un {@link EnvironmentPostProcessor} (registrado en {@code META-INF/spring.factories}) y no un bean: se ejecuta
 * con el entorno ya resuelto y ANTES de crear el contexto, asi que ni el pool de conexiones ni Flyway llegan a
 * abrir una conexion contra la base remota.
 */
public class LocalProfileGuard implements EnvironmentPostProcessor, Ordered {

    static final Set<String> HOSTS_LOCALES = Set.of("localhost", "127.0.0.1", "::1", "[::1]", "host.docker.internal",
            "postgres", "fitsense-local-db");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment entorno, SpringApplication aplicacion) {
        if (!entorno.acceptsProfiles(Profiles.of("local | local-dev"))) return;
        comprobar(entorno.getProperty("spring.datasource.url", ""),
                entorno.getProperty("fitsense.ai.enabled", Boolean.class, false));
    }

    /** Despues de cargar application*.properties y las variables de entorno. */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    static void comprobar(String url, boolean iaActiva) {
        if (iaActiva)
            throw new IllegalStateException("Perfil local: la IA debe estar apagada (fitsense.ai.enabled=false).");
        String host = host(url);
        if (host == null || !HOSTS_LOCALES.contains(host.toLowerCase(Locale.ROOT)))
            throw new IllegalStateException("Perfil local: la base de datos debe ser local y es '" + host
                    + "'. No se arranca contra Neon, Railway ni ningun host remoto con el perfil local.");
    }

    /** El host de una URL JDBC de PostgreSQL ({@code jdbc:postgresql://host:puerto/base?...}). */
    static String host(String url) {
        if (url == null || !url.startsWith("jdbc:postgresql://")) return null;
        try {
            return URI.create(url.substring("jdbc:".length())).getHost();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
