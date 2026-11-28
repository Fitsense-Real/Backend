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
            "43:db/migration/V43__pain_memory_and_safe_regeneration.sql",
            // V44 (version de la politica de planificacion): dos columnas nullable, sin datos.
            "44:db/migration/V44__planning_policy_version.sql",
            // V44_1 (codigos del NO_FACTIBLE): dos columnas NULL y dos CHECK, sin datos.
            "44.1:db/migration/V44_1__adaptation_reason_codes.sql",
            // V44_2 (recuperacion de contrasena): una tabla nueva, sin datos.
            "44.2:db/migration/V44_2__password_reset_tokens.sql",
            // V51 y V53 (cierre del catalogo) anaden columnas Y datos del catalogo: aqui solo sus columnas, sin datos
            // (script de prueba). Con impact_level e is_stretch NULL el motor usa la regla por nombre del catalogo
            // fijado, y requires_suspended_support FALSE es el estado anterior a V53.
            "51:catalogo/esquema_V51_V53_sin_datos.sql",
            // V54 (propuesta: constancia del bloqueo de seguridad al vencer): una columna nullable, sin datos.
            "54:db/migration/V54__vencimiento_con_bloqueo_de_sistema.sql",
            // V55 (propuesta V53-B, permiso «apoyo suspendido»): dos columnas NULL en user_profiles, sin datos.
            "55:db/migration/V55__perfil_permiso_apoyo_suspendido.sql",
            // V57 (propuesta: constancia de la omision impuesta por el sistema): una columna nullable, sin datos.
            "57:db/migration/V57__omision_impuesta_por_el_sistema.sql",
            // V58 (generacion inicial asincrona): una tabla nueva y sus indices, sin datos.
            "58:db/migration/V58__plan_generation_operations.sql",
            // V59 (generacion resiliente: lease y libro de predicciones): solo esquema, sin datos.
            "59:db/migration/V59__generacion_resiliente_lease_y_predicciones.sql",
            // V60 (eliminacion de cuentas): una tabla nueva y la secuencia de P-xxx, sin datos del catalogo.
            "60:db/migration/V60__eliminacion_de_cuentas.sql",
            // V61 (version de credenciales): una columna con valor por omision en users, sin datos.
            "61:db/migration/V61__version_de_token.sql",
            // V62 (solicitud web de eliminacion): una tabla nueva, sin datos.
            "62:db/migration/V62__solicitud_eliminacion_web.sql",
            // V63 (consentimientos): una tabla nueva y la copia de los consentimientos de users; nada del catalogo.
            "63:db/migration/V63__consentimientos.sql",
            // V64 (notificaciones push): cuatro tablas nuevas, sin datos.
            "64:db/migration/V64__notificaciones_push.sql",
            // V65 (O20, identificacion posterior del dolor): dos columnas NULL y un CHECK, sin datos.
            "65:db/migration/V65__dolor_identificacion_posterior.sql",
            // V66 (seguimiento V59 de la generacion sincrona y el cierre semanal): dos columnas, restricciones e
            // indices en plan_generation_operations, sin datos.
            "66:db/migration/V66__operaciones_ia_sincrona_y_ciclo_semanal.sql",
            // V67 (C4, propuesta: historial de recalculos de metricas): una tabla, su funcion y su disparador, sin datos.
            "67:db/migration/V67__historial_de_recalculos_de_metricas.sql");

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
                var script = new ClassPathResource(partes[1]);
                var poblador = new ResourceDatabasePopulator(script);
                // ScriptUtils corta en ';' y no entiende el $$ de una funcion plpgsql (V67): ese script va entero como
                // una sola sentencia; el controlador de PostgreSQL si separa respetando el $$.
                try (var in = script.getInputStream()) {
                    if (new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).contains("$$"))
                        poblador.setSeparator(org.springframework.jdbc.datasource.init.ScriptUtils.EOF_STATEMENT_SEPARATOR);
                } catch (java.io.IOException e) {
                    throw new IllegalStateException(e);
                }
                poblador.execute(flyway.getConfiguration().getDataSource());
                jdbc.update("INSERT INTO public.test_esquema_vigente VALUES (?)", partes[0]);
            }
        };
    }
}
