package main.web.services.fitsense.administration.application.internal.operations;

import main.web.services.fitsense.planning.application.internal.commandservices.AdaptedWeekCommandService;
import main.web.services.fitsense.planning.application.internal.commandservices.PlanGenerationPipeline;
import main.web.services.fitsense.shared.infrastructure.observability.ContadoresOperativos;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
                              long cierresFueraDeVentana,
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

    /**
     * C3 · Una semana medida cuyo cierre NO SE COMPLETO: el participante tuvo plan esa semana y, o no tiene su medicion
     * ({@code SIN_MEDICION}), o se midio pero la semana siguiente no tiene plan ({@code SIN_PLAN_NUEVO}).
     * {@code EN_VENTANA}: todavia dentro de {@code fitsense.weekly-cycle.catch-up-hours} desde el lunes 03:00 local, el
     * planificador la recupera solo. {@code FUERA_DE_VENTANA}: ya no se recupera automaticamente (a proposito: ver
     * 04_CONTINUIDAD_CIERRE_SEMANAL.md) y necesita la decision del equipo. Seudonimizada: el codigo P-xxx, nunca el correo.
     */
    public record CierreAtrasado(String participantCode, String timezone, LocalDate measuredWeekStart,
                                 long hoursSinceDue, String status, boolean newWeekWithoutPlan, boolean measured,
                                 String reason) {}

    /** La semana medida no tiene medicion (el cierre no llego a medir). */
    public static final String SIN_MEDICION = "SIN_MEDICION";
    /** La semana se midio, pero la semana siguiente no tiene plan (el cierre no llego a publicar). */
    public static final String SIN_PLAN_NUEVO = "SIN_PLAN_NUEVO";

    public static final String EN_VENTANA = "EN_VENTANA";
    public static final String FUERA_DE_VENTANA = "FUERA_DE_VENTANA";

    private final JdbcTemplate jdbc;
    private final ContadoresOperativos contadores;
    private final long horasDeRecuperacion;
    private final int semanasHaciaAtras;
    private final ObjectProvider<PlanGenerationPipeline> generacion;
    private final ObjectProvider<AdaptedWeekCommandService> adaptacion;

    public IndicadoresOperativosService(JdbcTemplate jdbc, ContadoresOperativos contadores,
                                        ObjectProvider<PlanGenerationPipeline> generacion,
                                        ObjectProvider<AdaptedWeekCommandService> adaptacion,
                                        @org.springframework.beans.factory.annotation.Value(
                                                "${fitsense.weekly-cycle.catch-up-hours:24}") long horasDeRecuperacion,
                                        @org.springframework.beans.factory.annotation.Value(
                                                "${fitsense.weekly-cycle.overdue-lookback-weeks:13}") int semanasHaciaAtras) {
        if (semanasHaciaAtras < 1 || semanasHaciaAtras > 60)
            throw new IllegalArgumentException("fitsense.weekly-cycle.overdue-lookback-weeks debe estar entre 1 y 60.");
        this.horasDeRecuperacion = horasDeRecuperacion;
        this.semanasHaciaAtras = semanasHaciaAtras;
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
        long fueraDeVentana = cierresAtrasados().stream().filter(c -> FUERA_DE_VENTANA.equals(c.status())).count();
        return new Indicadores(Instant.now(), activos, sinPlan, pendientes, fueraDeVentana, generacionesFallidas,
                adaptacionesFallidas, enCurso, huerfanas, g != null && g.cortacircuitosAbierto(),
                a != null && a.cortacircuitosAbierto(), contadores.todos());
    }

    /**
     * C3 · Cierres incompletos de los participantes activos, en las ultimas {@code overdue-lookback-weeks} semanas: tuvo
     * plan esa semana (alguna version no reemplazada), ya paso su cierre (lunes siguiente, 03:00 locales) y falta la
     * medicion o el plan de la semana siguiente. Una semana sin plan no se espera medir: no aparece (aparece la anterior,
     * como SIN_PLAN_NUEVO). SOLO LECTURA: no cierra, no mide y no genera nada. Una retirada saca a la persona de la lista (sus semanas
     * posteriores no se esperan).
     */
    @Transactional(readOnly = true)
    public List<CierreAtrasado> cierresAtrasados() {
        return jdbc.query("""
                WITH semanas AS (
                    SELECT u.user_id, u.participant_code, u.timezone,
                           (date_trunc('week', now() AT TIME ZONE u.timezone) - make_interval(weeks => k))::date AS semana,
                           now() AT TIME ZONE u.timezone AS ahora_local
                """ + PARTICIPANTES.replace("FROM users u", "FROM users u CROSS JOIN generate_series(1, ?) AS k") + """
                )
                SELECT s.participant_code, s.timezone, s.semana,
                       floor(extract(epoch FROM s.ahora_local - (s.semana + interval '7 days 3 hours')) / 3600)::bigint
                           AS horas,
                       NOT EXISTS (SELECT 1 FROM weekly_training_plans n
                                    WHERE n.user_id = s.user_id AND n.week_start_date = s.semana + 7
                                      AND n.status <> 'REPLACED') AS nueva_sin_plan,
                       EXISTS (SELECT 1 FROM weekly_user_metrics m
                                WHERE m.user_id = s.user_id AND m.week_start_date = s.semana) AS medida
                  FROM semanas s
                 WHERE s.ahora_local > s.semana + interval '7 days 3 hours'
                   AND EXISTS (SELECT 1 FROM weekly_training_plans w
                                WHERE w.user_id = s.user_id AND w.week_start_date = s.semana AND w.status <> 'REPLACED')
                   AND (NOT EXISTS (SELECT 1 FROM weekly_user_metrics m
                                     WHERE m.user_id = s.user_id AND m.week_start_date = s.semana)
                        OR NOT EXISTS (SELECT 1 FROM weekly_training_plans n
                                        WHERE n.user_id = s.user_id AND n.week_start_date = s.semana + 7
                                          AND n.status <> 'REPLACED'))
                 ORDER BY s.semana, s.participant_code
                """, (rs, i) -> {
            long horas = rs.getLong("horas");
            boolean medida = rs.getBoolean("medida");
            return new CierreAtrasado(rs.getString("participant_code"), rs.getString("timezone"),
                    rs.getObject("semana", LocalDate.class), horas,
                    horas < horasDeRecuperacion ? EN_VENTANA : FUERA_DE_VENTANA, rs.getBoolean("nueva_sin_plan"),
                    medida, medida ? SIN_PLAN_NUEVO : SIN_MEDICION);
        }, semanasHaciaAtras);
    }

    public long horasDeRecuperacion() {
        return horasDeRecuperacion;
    }

    private long contar(String sql) {
        Long n = jdbc.queryForObject(sql, Long.class);
        return n == null ? 0 : n;
    }
}
