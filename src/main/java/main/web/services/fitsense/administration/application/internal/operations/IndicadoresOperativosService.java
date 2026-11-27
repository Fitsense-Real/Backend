package main.web.services.fitsense.administration.application.internal.operations;

import main.web.services.fitsense.planning.application.internal.commandservices.AdaptedWeekCommandService;
import main.web.services.fitsense.planning.application.internal.commandservices.PlanGenerationPipeline;
import main.web.services.fitsense.shared.infrastructure.observability.ContadoresOperativos;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

/**
 * B5.1 (P1-01): indicadores operativos del backend, para el panel ({@code GET /api/v1/admin/operations}) y las
 * alertas ({@link VigilanteOperativo}). Son RECUENTOS: ningun identificador de persona, correo ni dato de salud.
 * Las cifras que deben sobrevivir a un reinicio salen de la base; las de sucesos (correo, autenticacion) son desde el
 * arranque ({@link ContadoresOperativos}).
 */
@Service
public class IndicadoresOperativosService {

    /** Instantanea de los indicadores; {@code contadores} son desde el arranque. */
    public record Indicadores(Instant calculadoEn,
                              long participantesActivos,
                              long participantesSinPlanVigente,
                              long cierresPendientesSemanaAnterior,
                              long generacionesFallidas24h,
                              long adaptacionesFallidas7d,
                              long operacionesEnCurso,
                              long operacionesHuerfanas,
                              boolean cortacircuitosGeneracionAbierto,
                              boolean cortacircuitosAdaptacionAbierto,
                              Map<String, Long> contadores) {}

    /** Participantes del estudio que deberian tener plan (activos, no retirados, con perfil). */
    static final String PARTICIPANTES = """
            FROM users u
            WHERE u.status = 'ACTIVE' AND u.role = 'USER' AND u.is_study_participant AND u.withdrawn_at IS NULL
              AND EXISTS (SELECT 1 FROM user_profiles p WHERE p.user_id = u.user_id)
            """;

    private final JdbcTemplate jdbc;
    private final ContadoresOperativos contadores;
    private final ObjectProvider<PlanGenerationPipeline> generacion;
    private final ObjectProvider<AdaptedWeekCommandService> adaptacion;

    public IndicadoresOperativosService(JdbcTemplate jdbc, ContadoresOperativos contadores,
                                        ObjectProvider<PlanGenerationPipeline> generacion,
                                        ObjectProvider<AdaptedWeekCommandService> adaptacion) {
        this.jdbc = jdbc;
        this.contadores = contadores;
        this.generacion = generacion;
        this.adaptacion = adaptacion;
    }

    @Transactional(readOnly = true)
    public Indicadores calcular() {
        long activos = contar("SELECT count(*) " + PARTICIPANTES);
        // Sin plan ACTIVO que cubra HOY en su zona horaria.
        long sinPlan = contar("SELECT count(*) " + PARTICIPANTES + """
                  AND NOT EXISTS (SELECT 1 FROM weekly_training_plans w
                                   WHERE w.user_id = u.user_id AND w.status = 'ACTIVE'
                                     AND (now() AT TIME ZONE u.timezone)::date
                                         BETWEEN w.week_start_date AND w.week_end_date)
                """);
        // Pasado el lunes 04:00 local: tuvo plan la semana anterior y no tiene su medicion (cierre no completado).
        long pendientes = contar("SELECT count(*) " + PARTICIPANTES + """
                  AND (now() AT TIME ZONE u.timezone)
                      > date_trunc('week', now() AT TIME ZONE u.timezone) + interval '4 hours'
                  AND EXISTS (SELECT 1 FROM weekly_training_plans w
                               WHERE w.user_id = u.user_id AND w.status <> 'REPLACED'
                                 AND w.week_start_date = (date_trunc('week', now() AT TIME ZONE u.timezone)
                                                          - interval '7 days')::date)
                  AND NOT EXISTS (SELECT 1 FROM weekly_user_metrics m
                                   WHERE m.user_id = u.user_id
                                     AND m.week_start_date = (date_trunc('week', now() AT TIME ZONE u.timezone)
                                                              - interval '7 days')::date)
                """);
        long generacionesFallidas = contar("SELECT count(*) FROM plan_generation_operations WHERE status IN"
                + " ('FAILED', 'NO_FACTIBLE') AND finished_at > now() - interval '24 hours'");
        long adaptacionesFallidas = contar("SELECT count(*) FROM plan_adaptation_attempts WHERE status IN"
                + " ('RECHAZADA', 'NO_FACTIBLE') AND created_at > now() - interval '7 days'");
        long enCurso = contar("SELECT count(*) FROM plan_generation_operations WHERE status IN"
                + " ('PENDING', 'PROCESSING')");
        long huerfanas = contar("SELECT count(*) FROM plan_generation_operations WHERE status = 'PROCESSING'"
                + " AND lease_expires_at < now()");
        var g = generacion.getIfAvailable();
        var a = adaptacion.getIfAvailable();
        return new Indicadores(Instant.now(), activos, sinPlan, pendientes, generacionesFallidas,
                adaptacionesFallidas, enCurso, huerfanas, g != null && g.cortacircuitosAbierto(),
                a != null && a.cortacircuitosAbierto(), contadores.todos());
    }

    private long contar(String sql) {
        Long n = jdbc.queryForObject(sql, Long.class);
        return n == null ? 0 : n;
    }
}
