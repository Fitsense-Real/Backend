package main.web.services.fitsense;

import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.jdbc.datasource.AbstractDriverBasedDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * SEGUNDA CAPA del aislamiento de pruebas: ningun contexto de Spring levantado
 * por una prueba puede conectarse a la base de PRODUCCION, venga la URL de
 * donde venga.
 * <p>
 * La primera capa -las propiedades que fija Maven en surefire- depende de la
 * precedencia de Spring, y se probo que no basta: {@code SPRING_APPLICATION_JSON}
 * le gana a las propiedades de sistema, y {@code SPRING_FLYWAY_URL} o
 * {@code SPRING_DATASOURCE_HIKARI_JDBC_URL} abren una conexion por otra via.
 * Esta capa no mira propiedades sueltas: mira los beans {@link DataSource} y
 * {@link Flyway} ya configurados, antes de que se use ninguno, y aborta el
 * arranque si apuntan al host de produccion.
 * <p>
 * El host de produccion NO esta copiado aqui: se lee del
 * {@code application.properties} de la aplicacion, asi que si cambia, la
 * guardia lo sigue.
 * <p>
 * Se registra en {@code src/test/resources/META-INF/spring.factories} y, por
 * tanto, solo existe en el classpath de pruebas: unitarias e integracion,
 * incluidas las IT de P2-D, cuya base de pruebas es otro host y no se ve
 * afectada.
 */
public class BaseDeProduccionBloqueada
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Pattern HOST = Pattern.compile("//([^/:?;]+)");

    private static final String[] PROPIEDADES = {
            "spring.datasource.url", "spring.datasource.hikari.jdbc-url", "spring.flyway.url"};

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        var produccion = hostDeProduccion();
        if (produccion.isEmpty()) return;
        String host = produccion.get();

        // Lo que ya se ve en el entorno, antes de crear ningun bean.
        for (var propiedad : PROPIEDADES)
            comprobar(host, propiedad, context.getEnvironment().getProperty(propiedad));

        // Lo que de verdad se va a usar, venga de donde venga.
        context.getBeanFactory().addBeanPostProcessor(new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String nombre) {
                if (bean instanceof DataSource ds)
                    comprobar(host, "DataSource '" + nombre + "'", urlDe(ds));
                if (bean instanceof Flyway flyway) {
                    comprobar(host, "Flyway url", flyway.getConfiguration().getUrl());
                    comprobar(host, "Flyway dataSource",
                            urlDe(flyway.getConfiguration().getDataSource()));
                }
                return bean;
            }
        });
    }

    /** Host de {@code spring.datasource.url} en el application.properties de la aplicacion. */
    static Optional<String> hostDeProduccion() {
        try {
            var url = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"))
                    .getProperty("spring.datasource.url");
            return hostDe(url);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    static Optional<String> hostDe(String jdbcUrl) {
        if (jdbcUrl == null) return Optional.empty();
        var m = HOST.matcher(jdbcUrl);
        return m.find() ? Optional.of(m.group(1).toLowerCase(Locale.ROOT)) : Optional.empty();
    }

    static void comprobar(String hostProduccion, String origen, String url) {
        if (hostDe(url).filter(hostProduccion::equals).isPresent())
            throw new IllegalStateException(("BASE DE PRODUCCION BLOQUEADA EN PRUEBAS: %s apunta a %s. "
                    + "Las pruebas usan Testcontainers o la base de pruebas declarada con "
                    + "-Dfitsense.pruebas.url, nunca la de application.properties.")
                    .formatted(origen, hostProduccion));
    }

    private static String urlDe(DataSource ds) {
        if (ds instanceof HikariDataSource hikari) return hikari.getJdbcUrl();
        if (ds instanceof AbstractDriverBasedDataSource driver) return driver.getUrl();
        return null;
    }
}
