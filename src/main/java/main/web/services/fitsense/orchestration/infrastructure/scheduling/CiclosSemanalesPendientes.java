package main.web.services.fitsense.orchestration.infrastructure.scheduling;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * B3 · Que participantes tienen el cierre semanal PENDIENTE: sin la medicion de la semana cerrada
 * ({@code weekly_user_metrics}) o sin plan ACTIVO para la semana nueva. Es una lectura: el orquestador decide a quien
 * relanzar y {@code WeeklyCycleService.ejecutar} hace el resto con su idempotencia de siempre.
 * <p>
 * Lee las tablas de analitica y planificacion directamente porque es una pregunta de ORQUESTACION sobre el resultado
 * conjunto de ambas (las dos filas que deja un cierre completo), no logica de ninguno de los dos contextos.
 */
@Component
public class CiclosSemanalesPendientes {

    private final NamedParameterJdbcTemplate jdbc;

    public CiclosSemanalesPendientes(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Long> de(List<Long> usuarios, LocalDate semanaMedida, LocalDate semanaNueva) {
        if (usuarios.isEmpty()) return List.of();
        return jdbc.queryForList("""
                SELECT u.user_id FROM users u
                 WHERE u.user_id IN (:usuarios)
                   AND (NOT EXISTS (SELECT 1 FROM weekly_user_metrics m
                                     WHERE m.user_id = u.user_id AND m.week_start_date = :medida)
                        OR NOT EXISTS (SELECT 1 FROM weekly_training_plans p
                                        WHERE p.user_id = u.user_id AND p.week_start_date = :nueva
                                          AND p.status = 'ACTIVE'))
                 ORDER BY u.user_id
                """, new MapSqlParameterSource("usuarios", usuarios).addValue("medida", semanaMedida)
                .addValue("nueva", semanaNueva), Long.class);
    }
}
