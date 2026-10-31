package main.web.services.fitsense;

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.util.List;

/**
 * CATALOGO FIJADO, ESQUEMA VIGENTE. Para las pruebas que fijan {@code spring.flyway.target} en un
 * catalogo anterior (V35, V36, V37, V39, V40, V41) para reproducir su evidencia.
 * <p>
 * Hasta V41 las migraciones posteriores a V35 solo cambiaban datos del catalogo, y fijar el target
 * no tocaba el esquema. V42 es la primera que anade columnas que mapean las entidades
 * ({@code planned_workouts.lack_of_time_context} y {@code declared_available_minutes}), y con
 * {@code ddl-auto=validate} esas pruebas ya no arrancaban.
 * <p>
 * Tras migrar hasta el target, aplica las migraciones de SOLO ESQUEMA posteriores, que no tocan el
 * catalogo: el catalogo sigue siendo el fijado y el esquema es el que validan las entidades. Una
 * migracion nueva que cambie datos del catalogo NO va en {@link #SOLO_ESQUEMA}.
 * <p>
 * Uso: {@code @Import(EsquemaVigenteSobreCatalogoAnterior.class)} junto al target fijado.
 */
@TestConfiguration
public class EsquemaVigenteSobreCatalogoAnterior {

    /** Migraciones que solo cambian el esquema, en orden. */
    static final List<String> SOLO_ESQUEMA = List.of("42:db/migration/V42__adaptation_lack_of_time_context.sql",
            // V43 (P-02 y P-03): dos tablas nuevas, sin datos del catalogo.
            "43:db/migration/V43__pain_memory_and_safe_regeneration.sql");

    @Bean
    FlywayMigrationStrategy catalogoFijadoConEsquemaVigente() {
        return flyway -> {
            flyway.migrate();
            var actual = flyway.info().current();
            var jdbc = new JdbcTemplate(flyway.getConfiguration().getDataSource());
            // Una clase puede crear su contexto mas de una vez contra el mismo contenedor: lo ya
            // aplicado queda anotado y no se repite.
            jdbc.execute("CREATE TABLE IF NOT EXISTS public.test_esquema_vigente (version TEXT PRIMARY KEY)");
            for (var m : SOLO_ESQUEMA) {
                var partes = m.split(":", 2);
                if (actual != null && actual.getVersion().isAtLeast(partes[0])) continue;
                if (jdbc.queryForObject("SELECT count(*) FROM public.test_esquema_vigente WHERE version = ?",
                        Integer.class, partes[0]) > 0) continue;
                new ResourceDatabasePopulator(new ClassPathResource(partes[1]))
                        .execute(flyway.getConfiguration().getDataSource());
                jdbc.update("INSERT INTO public.test_esquema_vigente VALUES (?)", partes[0]);
            }
        };
    }
}
