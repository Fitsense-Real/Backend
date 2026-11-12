package main.web.services.fitsense.administration.application.internal.queryservices;

import main.web.services.fitsense.administration.interfaces.rest.resources.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Modelo de lectura de la administracion: SQL de solo lectura sobre lo que ya
 * guardan iam, profiling y analytics. No recalcula nada ni define indicadores:
 * cada cifra es una columna de weekly_user_metrics (formulas en ADHERENCIA.md) o
 * una estadistica descriptiva de esa columna.
 * <p>
 * Poblacion de la analitica: cuentas USER con {@code is_study_participant}, como
 * exige el comentario de la columna (V1). Las cuentas ADMIN y las de prueba del
 * equipo no entran. Los retirados si: sus semanas anteriores al retiro son datos
 * del estudio que se conservan.
 */
@Service
@Transactional(readOnly = true)
public class AdminAnalyticsQueryService {

    /** Los indicadores que se exponen, en orden, con su columna. La observada no: es interna. */
    private static final List<String[]> INDICADORES = List.of(
            new String[]{"OFFICIAL_ADHERENCE", "weighted_adherence_pct"},
            new String[]{"FREQUENCY_ADHERENCE", "frequency_adherence_pct"},
            new String[]{"WORKOUT_ADHERENCE", "workout_adherence_pct"},
            new String[]{"EXERCISE_ADHERENCE", "exercise_adherence_pct"},
            new String[]{"REGISTRATION_COVERAGE", "registration_coverage_pct"});

    private static final List<String> NIVELES = List.of("LOW", "MODERATE", "HIGH", "CRITICAL", "NONE");

    /** Segmentos admitidos y su expresion SQL: nunca se concatena texto del cliente. */
    public enum Segmentacion {
        GOAL_TYPE("p.goal_type"),
        FITNESS_LEVEL("p.fitness_level"),
        TRAINING_LOCATION("p.training_location"),
        RISK_LEVEL("m.risk_level");

        final String expresion;

        Segmentacion(String expresion) {
            this.expresion = expresion;
        }
    }

    private static final String DESDE_METRICAS = """
             FROM weekly_user_metrics m
             JOIN users u ON u.user_id = m.user_id
             LEFT JOIN user_profiles p ON p.user_id = m.user_id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public AdminAnalyticsQueryService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---------------------------------------------------------------- analitica

    public AnalyticsOverviewResource overview(AnalyticsFilters f) {
        f.validate();
        var inscripcion = jdbc.queryForObject("""
                SELECT count(*) AS inscritos,
                       count(*) FILTER (WHERE status = 'ACTIVE' AND withdrawn_at IS NULL) AS activos,
                       count(*) FILTER (WHERE withdrawn_at IS NOT NULL) AS retirados
                  FROM users WHERE is_study_participant AND role = 'USER'
                """, Map.of(), (rs, i) -> new AnalyticsOverviewResource.Enrollment(
                rs.getLong("inscritos"), rs.getLong("activos"), rs.getLong("retirados")));

        var p = new MapSqlParameterSource();
        String where = where(f, p);
        var medicion = jdbc.queryForObject("""
                SELECT count(DISTINCT m.user_id) AS participantes,
                       count(*) AS semanas,
                       count(*) FILTER (WHERE m.has_active_plan) AS con_plan,
                       count(m.weighted_adherence_pct) AS con_adherencia,
                       round(avg(m.weighted_adherence_pct), 2) AS media,
                       round((percentile_cont(0.5) WITHIN GROUP (ORDER BY m.weighted_adherence_pct))::numeric, 2) AS mediana,
                       round(avg(m.registration_coverage_pct), 2) AS cobertura
                """ + DESDE_METRICAS + where, p, (rs, i) -> new Object[]{
                new AnalyticsOverviewResource.Measurement(rs.getLong("participantes"), rs.getLong("semanas"),
                        rs.getLong("con_plan"), rs.getLong("con_adherencia")),
                new AnalyticsOverviewResource.Summary(rs.getBigDecimal("media"), rs.getBigDecimal("mediana")),
                rs.getBigDecimal("cobertura")});

        var niveles = vacios();
        long[] totales = new long[2];
        jdbc.query("""
                SELECT coalesce(l.risk_level, 'NONE') AS nivel, count(*) AS n,
                       count(*) FILTER (WHERE l.is_dropout) AS abandonos
                  FROM (SELECT DISTINCT ON (m.user_id) m.risk_level, m.is_dropout
                """ + DESDE_METRICAS + where + """
                         ORDER BY m.user_id, m.week_start_date DESC) l
                 GROUP BY 1
                """, p, rs -> {
            niveles.put(rs.getString("nivel"), rs.getLong("n"));
            totales[0] += rs.getLong("n");
            totales[1] += rs.getLong("abandonos");
        });

        return new AnalyticsOverviewResource(f, inscripcion,
                (AnalyticsOverviewResource.Measurement) medicion[0],
                (AnalyticsOverviewResource.Summary) medicion[1],
                (BigDecimal) medicion[2],
                new AnalyticsOverviewResource.LatestWeek(totales[0], niveles, totales[1]));
    }

    public AdherenceDistributionResource adherence(AnalyticsFilters f) {
        f.validate();
        var p = new MapSqlParameterSource();
        String where = where(f, p);
        var indicadores = new ArrayList<AdherenceDistributionResource.Indicator>();
        for (String[] ind : INDICADORES) {
            String c = "m." + ind[1];
            indicadores.add(jdbc.queryForObject("""
                    SELECT count(%1$s) AS n,
                           count(DISTINCT m.user_id) FILTER (WHERE %1$s IS NOT NULL) AS participantes,
                           round(avg(%1$s), 2) AS media,
                           round((percentile_cont(0.5) WITHIN GROUP (ORDER BY %1$s))::numeric, 2) AS mediana,
                           round((percentile_cont(0.25) WITHIN GROUP (ORDER BY %1$s))::numeric, 2) AS p25,
                           round((percentile_cont(0.75) WITHIN GROUP (ORDER BY %1$s))::numeric, 2) AS p75,
                           min(%1$s) AS minimo, max(%1$s) AS maximo
                    """.formatted(c) + DESDE_METRICAS + where, p,
                    (rs, i) -> new AdherenceDistributionResource.Indicator(ind[0], ind[1],
                            rs.getLong("n"), rs.getLong("participantes"), rs.getBigDecimal("media"),
                            rs.getBigDecimal("mediana"), rs.getBigDecimal("p25"), rs.getBigDecimal("p75"),
                            rs.getBigDecimal("minimo"), rs.getBigDecimal("maximo"))));
        }
        return new AdherenceDistributionResource(f, indicadores);
    }

    public AdherenceTrendResource trends(AnalyticsFilters f) {
        f.validate();
        var p = new MapSqlParameterSource();
        var semanas = jdbc.query("""
                SELECT m.week_start_date AS semana,
                       count(DISTINCT m.user_id) AS participantes,
                       count(*) FILTER (WHERE m.has_active_plan) AS con_plan,
                       round(avg(m.weighted_adherence_pct), 2) AS media,
                       round((percentile_cont(0.5) WITHIN GROUP (ORDER BY m.weighted_adherence_pct))::numeric, 2) AS mediana,
                       round(avg(m.registration_coverage_pct), 2) AS cobertura,
                       count(*) FILTER (WHERE m.risk_level = 'LOW') AS low,
                       count(*) FILTER (WHERE m.risk_level = 'MODERATE') AS moderate,
                       count(*) FILTER (WHERE m.risk_level = 'HIGH') AS high,
                       count(*) FILTER (WHERE m.risk_level = 'CRITICAL') AS critical,
                       count(*) FILTER (WHERE m.risk_level IS NULL) AS sin_nivel,
                       count(*) FILTER (WHERE m.is_dropout) AS abandonos
                """ + DESDE_METRICAS + where(f, p) + """
                 GROUP BY m.week_start_date
                 ORDER BY m.week_start_date
                """, p, (rs, i) -> {
            var niveles = new LinkedHashMap<String, Long>();
            niveles.put("LOW", rs.getLong("low"));
            niveles.put("MODERATE", rs.getLong("moderate"));
            niveles.put("HIGH", rs.getLong("high"));
            niveles.put("CRITICAL", rs.getLong("critical"));
            niveles.put("NONE", rs.getLong("sin_nivel"));
            return new AdherenceTrendResource.Week(rs.getObject("semana", LocalDate.class),
                    rs.getLong("participantes"), rs.getLong("con_plan"), rs.getBigDecimal("media"),
                    rs.getBigDecimal("mediana"), rs.getBigDecimal("cobertura"), niveles,
                    rs.getLong("abandonos"));
        });
        return new AdherenceTrendResource(f, semanas);
    }

    public SegmentsResource segments(AnalyticsFilters f, Segmentacion by) {
        f.validate();
        var p = new MapSqlParameterSource();
        var segmentos = jdbc.query("""
                SELECT coalesce(%s, 'NONE') AS valor,
                       count(DISTINCT m.user_id) AS participantes,
                       count(*) FILTER (WHERE m.has_active_plan) AS con_plan,
                       round(avg(m.weighted_adherence_pct), 2) AS media,
                       round((percentile_cont(0.5) WITHIN GROUP (ORDER BY m.weighted_adherence_pct))::numeric, 2) AS mediana,
                       round(avg(m.registration_coverage_pct), 2) AS cobertura
                """.formatted(by.expresion) + DESDE_METRICAS + where(f, p) + """
                 GROUP BY 1
                 ORDER BY 1
                """, p, (rs, i) -> new SegmentsResource.Segment(rs.getString("valor"),
                rs.getLong("participantes"), rs.getLong("con_plan"), rs.getBigDecimal("media"),
                rs.getBigDecimal("mediana"), rs.getBigDecimal("cobertura")));
        return new SegmentsResource(f, by.name(), segmentos);
    }

    // ---------------------------------------------------------- participantes

    /** Cuentas USER (las ADMIN no son participantes), con su ultima semana medida. */
    public ParticipantPageResource participants(String status, String studyStatus, int page, int size) {
        var p = new MapSqlParameterSource();
        var where = new StringBuilder(" WHERE u.role = 'USER'");
        if (status != null) {
            where.append(" AND u.status = :status");
            p.addValue("status", status);
        }
        if (studyStatus != null) where.append(switch (studyStatus) {
            case "ENROLLED" -> " AND u.is_study_participant AND u.withdrawn_at IS NULL";
            case "WITHDRAWN" -> " AND u.is_study_participant AND u.withdrawn_at IS NOT NULL";
            case "NOT_PARTICIPANT" -> " AND NOT u.is_study_participant";
            default -> throw new IllegalArgumentException(
                    "studyStatus debe ser ENROLLED, WITHDRAWN o NOT_PARTICIPANT.");
        });

        long total = jdbc.queryForObject("SELECT count(*) FROM users u" + where, p, Long.class);
        p.addValue("limite", size).addValue("desde", (long) page * size);
        var items = jdbc.query("""
                SELECT u.user_id, u.participant_code, u.status, u.is_study_participant, u.enrolled_at,
                       u.withdrawn_at, p.goal_type, p.fitness_level, p.training_location,
                       l.week_start_date, l.weighted_adherence_pct, l.risk_level, l.is_dropout
                  FROM users u
                  LEFT JOIN user_profiles p ON p.user_id = u.user_id
                  LEFT JOIN LATERAL (SELECT m.week_start_date, m.weighted_adherence_pct, m.risk_level, m.is_dropout
                                       FROM weekly_user_metrics m WHERE m.user_id = u.user_id
                                      ORDER BY m.week_start_date DESC LIMIT 1) l ON true
                """ + where + " ORDER BY u.user_id LIMIT :limite OFFSET :desde", p,
                (rs, i) -> new ParticipantSummaryResource(rs.getLong("user_id"),
                        rs.getString("participant_code"), rs.getString("status"), estadoDeEstudio(rs),
                        rs.getObject("enrolled_at", OffsetDateTime.class),
                        rs.getObject("withdrawn_at", OffsetDateTime.class),
                        rs.getString("goal_type"), rs.getString("fitness_level"),
                        rs.getString("training_location"),
                        rs.getObject("week_start_date", LocalDate.class),
                        rs.getBigDecimal("weighted_adherence_pct"), rs.getString("risk_level"),
                        (Boolean) rs.getObject("is_dropout")));
        return new ParticipantPageResource(page, size, total, items);
    }

    public Optional<ParticipantDetailResource> participant(long participantId) {
        return jdbc.query("""
                SELECT u.user_id, u.participant_code, u.first_name, u.last_name, u.email, u.timezone,
                       u.status, u.is_study_participant, u.withdrawn_at, u.consent_version, u.enrolled_at,
                       u.last_login_at, u.created_at, p.user_id AS perfil, p.goal_type, p.fitness_level,
                       p.training_location, p.days_per_week, p.session_minutes
                  FROM users u LEFT JOIN user_profiles p ON p.user_id = u.user_id
                 WHERE u.user_id = :id AND u.role = 'USER'
                """, Map.of("id", participantId), (rs, i) -> new ParticipantDetailResource(
                rs.getLong("user_id"), rs.getString("participant_code"), rs.getString("first_name"),
                rs.getString("last_name"), rs.getString("email"), rs.getString("timezone"),
                rs.getString("status"), estadoDeEstudio(rs), rs.getString("consent_version"),
                rs.getObject("enrolled_at", OffsetDateTime.class),
                rs.getObject("withdrawn_at", OffsetDateTime.class),
                rs.getObject("last_login_at", OffsetDateTime.class),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("perfil") == null ? null : new ParticipantDetailResource.Profile(
                        rs.getString("goal_type"), rs.getString("fitness_level"),
                        rs.getString("training_location"), (Short) corto(rs, "days_per_week"),
                        (Short) corto(rs, "session_minutes")))).stream().findFirst();
    }

    public Optional<ParticipantAdherenceResource> participantAdherence(long participantId, LocalDate from,
                                                                       LocalDate to) {
        AnalyticsFilters.period(from, to).validate();
        var codigo = jdbc.query("SELECT participant_code FROM users WHERE user_id = :id AND role = 'USER'",
                Map.of("id", participantId), (rs, i) -> Optional.ofNullable(rs.getString(1)));
        if (codigo.isEmpty()) return Optional.empty();

        var p = new MapSqlParameterSource("id", participantId);
        var where = new StringBuilder(" WHERE m.user_id = :id");
        if (from != null) where.append(" AND m.week_start_date >= :from");
        if (to != null) where.append(" AND m.week_start_date <= :to");
        p.addValue("from", from).addValue("to", to);
        var semanas = jdbc.query("""
                SELECT m.week_start_date, m.week_number, m.has_active_plan, m.scheduled_workouts,
                       m.completed_workouts, m.skipped_workouts, m.recorded_workouts,
                       m.weighted_adherence_pct, m.frequency_adherence_pct, m.workout_adherence_pct,
                       m.exercise_adherence_pct, m.registration_coverage_pct, m.consecutive_skips,
                       m.dominant_skip_reason, m.risk_level, m.is_dropout
                  FROM weekly_user_metrics m
                """ + where + " ORDER BY m.week_start_date", p,
                (rs, i) -> new ParticipantAdherenceResource.Week(
                        rs.getObject("week_start_date", LocalDate.class), rs.getInt("week_number"),
                        rs.getBoolean("has_active_plan"), rs.getInt("scheduled_workouts"),
                        rs.getInt("completed_workouts"), rs.getInt("skipped_workouts"),
                        (Integer) entero(rs, "recorded_workouts"), rs.getBigDecimal("weighted_adherence_pct"),
                        rs.getBigDecimal("frequency_adherence_pct"), rs.getBigDecimal("workout_adherence_pct"),
                        rs.getBigDecimal("exercise_adherence_pct"), rs.getBigDecimal("registration_coverage_pct"),
                        rs.getInt("consecutive_skips"), rs.getString("dominant_skip_reason"),
                        rs.getString("risk_level"), rs.getBoolean("is_dropout")));
        return Optional.of(new ParticipantAdherenceResource(participantId, codigo.get(0).orElse(null),
                from, to, semanas));
    }

    // ---------------------------------------------------------------- apoyo

    private static String where(AnalyticsFilters f, MapSqlParameterSource p) {
        var w = new StringBuilder(" WHERE u.is_study_participant AND u.role = 'USER'");
        if (f.from() != null) {
            w.append(" AND m.week_start_date >= :from");
            p.addValue("from", f.from());
        }
        if (f.to() != null) {
            w.append(" AND m.week_start_date <= :to");
            p.addValue("to", f.to());
        }
        if (f.goalType() != null) {
            w.append(" AND p.goal_type = :goal");
            p.addValue("goal", f.goalType().name());
        }
        if (f.fitnessLevel() != null) {
            w.append(" AND p.fitness_level = :level");
            p.addValue("level", f.fitnessLevel().name());
        }
        if (f.trainingLocation() != null) {
            w.append(" AND p.training_location = :location");
            p.addValue("location", f.trainingLocation().name());
        }
        return w.append('\n').toString();
    }

    private static Map<String, Long> vacios() {
        var m = new LinkedHashMap<String, Long>();
        NIVELES.forEach(n -> m.put(n, 0L));
        return m;
    }

    private static String estadoDeEstudio(ResultSet rs) throws SQLException {
        if (!rs.getBoolean("is_study_participant")) return "NOT_PARTICIPANT";
        return rs.getObject("withdrawn_at") == null ? "ENROLLED" : "WITHDRAWN";
    }

    private static Object corto(ResultSet rs, String columna) throws SQLException {
        var v = rs.getObject(columna);
        return v == null ? null : ((Number) v).shortValue();
    }

    private static Object entero(ResultSet rs, String columna) throws SQLException {
        var v = rs.getObject(columna);
        return v == null ? null : ((Number) v).intValue();
    }
}
