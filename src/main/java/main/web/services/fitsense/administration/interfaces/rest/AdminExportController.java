package main.web.services.fitsense.administration.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import main.web.services.fitsense.administration.infrastructure.audit.AdminAudit;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * B5.3 (P1-05, P1-09): EXPORTACION SEUDONIMIZADA para el analisis de las 12 semanas, de SOLO LECTURA (transaccion
 * {@code readOnly}: no modifica ningun registro operativo). Una fila por participante y semana medida:
 * <ul>
 *   <li>identidad: solo {@code participant_code} (P-xxx) y el estado en el estudio; nunca correo, nombre, fecha de
 *       nacimiento, notas de salud ni el id interno;</li>
 *   <li>semana: las columnas de {@code weekly_user_metrics} tal como estan (las formulas no cambian), su
 *       {@code calculation_version} y {@code calculated_at} (fecha del PRIMER calculo, ADHERENCIA.md);</li>
 *   <li>el plan medido: su origen ({@code AI}, {@code RULE_ENGINE}, {@code CONSERVED}) y su version.</li>
 * </ul>
 * Solo participantes ({@code is_study_participant}); los retirados se incluyen con {@code study_status=WITHDRAWN}
 * porque que hacer con sus datos es una decision pendiente (P-2): quien analiza filtra. Cada exportacion queda en el
 * registro de auditoria ({@code ADMIN_EXPORT}).
 */
@RestController
@RequestMapping(value = "/api/v1/admin/exports")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administracion · exportacion", description = "Exportacion seudonimizada para el analisis (ADMIN)")
public class AdminExportController {

    static final String CABECERA = "participant_code,study_status,week_number,week_start_date,has_active_plan,"
            + "plan_generation_source,plan_version,scheduled_workouts,valid_workouts,completed_workouts,"
            + "skipped_workouts,weighted_adherence_pct,frequency_adherence_pct,workout_adherence_pct,"
            + "exercise_adherence_pct,total_training_minutes,average_session_rpe,consecutive_skips,"
            + "dominant_skip_reason,risk_level,is_dropout,calculation_version,calculated_at";

    private final NamedParameterJdbcTemplate jdbc;

    public AdminExportController(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping(value = "/weekly-metrics", produces = "text/csv")
    @Transactional(readOnly = true)
    @Operation(summary = "CSV seudonimizado de las semanas medidas de los participantes (solo lectura, auditado)")
    public ResponseEntity<String> semanas(@AuthenticationPrincipal UserDetailsImpl admin,
                                          @RequestParam(required = false)
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                          @RequestParam(required = false)
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var p = new MapSqlParameterSource("desde", from == null ? LocalDate.of(2000, 1, 1) : from)
                .addValue("hasta", to == null ? LocalDate.of(2999, 12, 31) : to);
        var csv = new StringBuilder(CABECERA).append('\n');
        int[] filas = {0};
        jdbc.query("""
                SELECT u.participant_code,
                       CASE WHEN u.withdrawn_at IS NULL THEN 'ENROLLED' ELSE 'WITHDRAWN' END AS study_status,
                       m.week_number, m.week_start_date, m.has_active_plan, w.generation_source, w.plan_version,
                       m.scheduled_workouts, m.valid_workouts, m.completed_workouts, m.skipped_workouts,
                       m.weighted_adherence_pct, m.frequency_adherence_pct, m.workout_adherence_pct,
                       m.exercise_adherence_pct, m.total_training_minutes, m.average_session_rpe,
                       m.consecutive_skips, m.dominant_skip_reason, m.risk_level, m.is_dropout,
                       m.calculation_version, m.calculated_at
                  FROM weekly_user_metrics m
                  JOIN users u ON u.user_id = m.user_id
                  LEFT JOIN weekly_training_plans w ON w.plan_id = m.plan_id
                 WHERE u.is_study_participant AND u.role = 'USER'
                   AND m.week_start_date BETWEEN :desde AND :hasta
                 ORDER BY u.participant_code, m.week_start_date
                """, p, rs -> {
            filas[0]++;
            int n = rs.getMetaData().getColumnCount();
            for (int c = 1; c <= n; c++) {
                if (c > 1) csv.append(',');
                Object v = rs.getObject(c);
                if (v instanceof OffsetDateTime t) v = t.withOffsetSameInstant(ZoneOffset.UTC);
                else if (v instanceof java.sql.Timestamp t) v = t.toInstant();
                csv.append(campo(v));
            }
            csv.append('\n');
        });
        AdminAudit.exported(admin.getUserId(), "weekly-metrics", filas[0]);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"weekly_metrics.csv\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(csv.toString());
    }

    /** RFC 4180; ademas, un valor que empiece por = + - @ se prefija con ' (inyeccion de formulas en hojas). */
    static String campo(Object v) {
        if (v == null) return "";
        String s = v.toString();
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0 && !s.matches("-?\\d+(\\.\\d+)?")) s = "'" + s;
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r"))
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }
}
