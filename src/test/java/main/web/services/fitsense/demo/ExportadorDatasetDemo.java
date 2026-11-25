package main.web.services.fitsense.demo;

import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Exporta las filas del usuario del generador a los scripts 01–05 del dataset demostrativo.
 * <p>
 * Cada script es UN bloque {@code DO} (atomico: o entra entero o no entra nada) que:
 * <ol>
 *   <li>busca la cuenta por correo (registrada antes por la API; el script no crea usuarios ni contrasenas);</li>
 *   <li>si ese paso ya se aplico, avisa y no cambia nada (idempotente);</li>
 *   <li>exige el paso anterior, la migracion V65 y el mismo catalogo (id y {@code source_code} de cada ejercicio);</li>
 *   <li>aborta si algun id reservado ya existe (no pisa filas de nadie);</li>
 *   <li>inserta las filas con sus ids reservados y las fechas trasladadas a la semana real en Lima.</li>
 * </ol>
 * Las fechas y el user_id se escriben como marcadores ({@code @@Dn@@} = lunes de W-3 + n dias; {@code @@U@@} = la
 * cuenta) en el texto de las sentencias, incluidos los JSON de trazabilidad; el bloque los sustituye y ejecuta.
 */
class ExportadorDatasetDemo {
    final JdbcTemplate jdbc; final long u; final long usuarioGenerador; final LocalDate w0; final LocalDate w3; final String email;
    final List<String> avisos = new ArrayList<>();
    long inicialId;

    ExportadorDatasetDemo(JdbcTemplate jdbc, long u, long usuarioGenerador, LocalDate w0, String email) {
        this.jdbc = jdbc; this.u = u; this.usuarioGenerador = usuarioGenerador; this.w0 = w0; this.w3 = w0.minusWeeks(3);
        this.email = email;
    }

    void describir(Consumer<String> out) {
        var fks = jdbc.queryForList("SELECT tc.table_name, kcu.column_name, ccu.table_name AS ref_table, ccu.column_name AS ref_col"
                + " FROM information_schema.table_constraints tc JOIN information_schema.key_column_usage kcu"
                + " ON tc.constraint_name = kcu.constraint_name AND tc.table_schema = kcu.table_schema"
                + " JOIN information_schema.constraint_column_usage ccu ON ccu.constraint_name = tc.constraint_name"
                + " WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_schema = 'public' ORDER BY 1, 2");
        fks.forEach(f -> out.accept("FK " + f));
        for (var t : jdbc.queryForList("SELECT table_name FROM information_schema.tables WHERE table_schema='public'"
                + " AND table_type='BASE TABLE' ORDER BY 1", String.class)) {
            var cols = jdbc.queryForList("SELECT column_name, data_type, is_identity, column_default, is_nullable FROM"
                    + " information_schema.columns WHERE table_schema='public' AND table_name=? ORDER BY ordinal_position", t);
            boolean conUser = cols.stream().anyMatch(c -> "user_id".equals(c.get("column_name")));
            long n = conUser ? jdbc.queryForObject("SELECT count(*) FROM " + t + " WHERE user_id = ?", Long.class, u) : -1;
            out.accept("TABLA " + t + " filas_usuario=" + n + " " + cols);
        }
    }

    // ------------------------------------------------------------------ seleccion de filas

    /** Una tabla, su clave y el filtro (WHERE) de las filas que van a un script. */
    record Parte(String tabla, String clave, String filtro, Object... args) {}

    static final String PW_DE_PLANES = "plan_id = ANY(?::bigint[])";
    static final String PWE_DE_PLANES = "planned_workout_id IN (SELECT planned_workout_id FROM planned_workouts"
            + " WHERE plan_id = ANY(?::bigint[]))";
    static final String SE_DE_PLANES = "session_id IN (SELECT session_id FROM workout_sessions WHERE plan_id = ANY(?::bigint[]))";

    static String arr(Collection<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(",", "{", "}"));
    }

    List<Parte> rutinas(List<Long> planes) {
        String a = arr(planes);
        return List.of(new Parte("weekly_training_plans", "plan_id", "plan_id = ANY(?::bigint[])", a),
                new Parte("planned_workouts", "planned_workout_id", PW_DE_PLANES, a),
                new Parte("planned_workout_exercises", "planned_exercise_id", PWE_DE_PLANES, a));
    }

    List<Parte> ejecuciones(List<Long> planes) {
        String a = arr(planes);
        return List.of(new Parte("workout_sessions", "session_id", "plan_id = ANY(?::bigint[])", a),
                new Parte("workout_session_exercises", "session_exercise_id", SE_DE_PLANES, a),
                new Parte("weekly_user_metrics", "weekly_metric_id", "plan_id = ANY(?::bigint[])", a),
                new Parte("user_safety_exclusions", "exclusion_id", "source_plan_id = ANY(?::bigint[])", a));
    }

    List<Parte> adaptaciones(List<Long> resultantes) {
        String a = arr(resultantes);
        return List.of(new Parte("plan_adaptation_attempts", "attempt_id", "resulting_plan_id = ANY(?::bigint[])", a),
                new Parte("safe_regeneration_attempts", "regeneration_id", "resulting_plan_id = ANY(?::bigint[])", a),
                new Parte("user_interventions", "intervention_id", "resulting_plan_id = ANY(?::bigint[])", a));
    }

    // ------------------------------------------------------------------ escritura

    void escribir(Path salida, Map<Long, Integer> planes) {
        var porScript = new TreeMap<Integer, List<Long>>();
        planes.forEach((p, n) -> porScript.computeIfAbsent(n, k -> new ArrayList<>()).add(p));
        porScript.values().forEach(Collections::sort);
        List<Long> p2 = porScript.get(2), p4 = porScript.get(4), p5 = porScript.get(5);
        long inicial = p2.get(0);
        inicialId = inicial;

        var exportadas = new HashMap<String, Set<Long>>();
        var scripts = new LinkedHashMap<String, String>();

        scripts.put("01_USUARIO_DEMO.sql", script01(exportadas));

        var partes02 = new ArrayList<>(rutinas(p2));
        scripts.put("02_RUTINA_INICIAL.sql", bloque("02", "Rutina inicial (semana W-3)", partes02, exportadas,
                "IF NOT EXISTS (SELECT 1 FROM user_profiles WHERE user_id = v_user) THEN\n"
                        + "    RAISE EXCEPTION 'DEMO 02: falta el perfil. Ejecute antes 01_USUARIO_DEMO.sql.';\n  END IF;\n"
                        + "  IF EXISTS (SELECT 1 FROM weekly_training_plans WHERE user_id = v_user) THEN\n"
                        + "    RAISE EXCEPTION 'DEMO 02: la cuenta % ya tiene planes que no son del dataset. Use una cuenta nueva.', v_user;\n"
                        + "  END IF;\n"
                        + "  IF EXISTS (SELECT 1 FROM workout_sessions WHERE user_id = v_user) THEN\n"
                        + "    RAISE EXCEPTION 'DEMO 02: la cuenta % ya tiene sesiones. Use una cuenta nueva.', v_user;\n  END IF;",
                true, null));

        scripts.put("03_HISTORIAL_ADHERENCIA.sql", bloque("03", "Ejecuciones y adherencia de la semana W-3",
                ejecuciones(p2), exportadas, requiere("plan_id", "weekly_training_plans", inicial, "02_RUTINA_INICIAL.sql"),
                false, null));

        var partes04 = new ArrayList<Parte>();
        partes04.addAll(rutinas(p4));
        partes04.addAll(ejecuciones(p4));
        partes04.addAll(adaptaciones(p4));
        long metricaInicial = unico("SELECT weekly_metric_id FROM weekly_user_metrics WHERE plan_id = ?", inicial);
        scripts.put("04_ADAPTACIONES.sql", bloque("04", "Adaptaciones historicas (W-2 y W-1) y sus ejecuciones",
                partes04, exportadas, requiere("weekly_metric_id", "weekly_user_metrics", metricaInicial,
                        "03_HISTORIAL_ADHERENCIA.sql"), false, null));

        var partes05 = new ArrayList<Parte>();
        partes05.addAll(rutinas(p5));
        partes05.addAll(ejecuciones(p5));
        partes05.addAll(adaptaciones(p5));
        long ultimoPlan04 = p4.get(p4.size() - 1);
        long metrica04 = unico("SELECT weekly_metric_id FROM weekly_user_metrics WHERE plan_id = ?", ultimoPlan04);
        scripts.put("05_RUTINA_VIGENTE.sql", bloque("05", "Rutina vigente (semana en curso, W0)", partes05, exportadas,
                requiere("weekly_metric_id", "weekly_user_metrics", metrica04, "04_ADAPTACIONES.sql"), false,
                "IF (SELECT count(*) FROM weekly_training_plans WHERE user_id = v_user AND status = 'ACTIVE') <> 1 THEN\n"
                        + "    RAISE EXCEPTION 'DEMO 05: la cuenta debe quedar con exactamente un plan ACTIVE.';\n  END IF;\n"
                        + "  IF NOT EXISTS (SELECT 1 FROM weekly_training_plans WHERE user_id = v_user AND status = 'ACTIVE'\n"
                        + "                 AND (now() AT TIME ZONE 'America/Lima')::date BETWEEN week_start_date AND week_end_date) THEN\n"
                        + "    RAISE EXCEPTION 'DEMO 05: el plan vigente no cubre la semana actual en Lima (02 se cargo otra semana). Ejecute 07_ELIMINAR_DEMO.sql y cargue 01-05 seguidos.';\n  END IF;"));

        comprobarQueTodoSeExporto(exportadas);
        try {
            Files.createDirectories(salida);
            for (var e : scripts.entrySet())
                Files.writeString(salida.resolve(e.getKey()), e.getValue(), StandardCharsets.UTF_8);
            var m = new StringBuilder("# Manifiesto del dataset demostrativo (generado)\n");
            m.append("semana_generacion_w0=").append(w0).append('\n');
            m.append("semana_generacion_w3=").append(w3).append('\n');
            exportadas.forEach((t, ids) -> m.append(t).append(" filas=").append(ids.size()).append(" ids=")
                    .append(new TreeSet<>(ids)).append('\n'));
            avisos.forEach(a -> m.append("AVISO ").append(a).append('\n'));
            Files.writeString(salida.resolve("MANIFIESTO_GENERADO.txt"), m.toString(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    long unico(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    String requiere(String col, String tabla, long id, String script) {
        return "IF NOT EXISTS (SELECT 1 FROM " + tabla + " WHERE " + col + " = " + id + " AND user_id = v_user) THEN\n"
                + "    RAISE EXCEPTION 'DEMO: falta el paso anterior. Ejecute antes " + script + ".';\n  END IF;";
    }

    /** Toda fila del usuario en las tablas de datos debe haber ido a algun script (nada se queda fuera). */
    void comprobarQueTodoSeExporto(Map<String, Set<Long>> exportadas) {
        var consultas = new LinkedHashMap<String, String>();
        consultas.put("weekly_training_plans", "SELECT plan_id FROM weekly_training_plans WHERE user_id = ?");
        consultas.put("planned_workouts", "SELECT pw.planned_workout_id FROM planned_workouts pw JOIN weekly_training_plans p"
                + " ON p.plan_id = pw.plan_id WHERE p.user_id = ?");
        consultas.put("planned_workout_exercises", "SELECT pwe.planned_exercise_id FROM planned_workout_exercises pwe JOIN"
                + " planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id JOIN weekly_training_plans p"
                + " ON p.plan_id = pw.plan_id WHERE p.user_id = ?");
        consultas.put("workout_sessions", "SELECT session_id FROM workout_sessions WHERE user_id = ?");
        consultas.put("workout_session_exercises", "SELECT wse.session_exercise_id FROM workout_session_exercises wse JOIN"
                + " workout_sessions s ON s.session_id = wse.session_id WHERE s.user_id = ?");
        consultas.put("weekly_user_metrics", "SELECT weekly_metric_id FROM weekly_user_metrics WHERE user_id = ?");
        consultas.put("user_safety_exclusions", "SELECT exclusion_id FROM user_safety_exclusions WHERE user_id = ?");
        consultas.put("plan_adaptation_attempts", "SELECT attempt_id FROM plan_adaptation_attempts WHERE user_id = ?");
        consultas.put("safe_regeneration_attempts", "SELECT regeneration_id FROM safe_regeneration_attempts WHERE user_id = ?");
        consultas.put("user_interventions", "SELECT intervention_id FROM user_interventions WHERE user_id = ?");
        for (var e : consultas.entrySet()) {
            var todas = new TreeSet<>(jdbc.queryForList(e.getValue(), Long.class, u));
            todas.removeAll(exportadas.getOrDefault(e.getKey(), Set.of()));
            if (!todas.isEmpty()) throw new IllegalStateException("filas sin exportar en " + e.getKey() + ": " + todas);
        }
        // Tablas que el dataset no lleva: deben estar vacias para el usuario del generador (si no, algo se omitiria).
        for (var t : List.of("plan_generation_operations", "plan_generation_predictions", "notification_events",
                "notification_deliveries", "push_devices", "notification_preferences", "password_reset_tokens",
                "account_deletion_tokens", "account_deletions")) {
            long n = jdbc.queryForObject("SELECT count(*) FROM " + t + " WHERE user_id = ?", Long.class, u);
            if (n > 0) avisos.add(t + " tiene " + n + " filas del generador y el dataset no las lleva");
        }
    }

    // ------------------------------------------------------------------ 01

    String script01(Map<String, Set<Long>> exportadas) {
        var fila = filas("user_profiles", "user_id", "user_id = ?", u);
        exportadas.put("user_profiles", Set.of(u));
        var sb = new StringBuilder();
        sb.append(cabecera("01", "Perfil del usuario demo (GAIN_MUSCLE, INTERMEDIATE, 5 dias, 60 min, gimnasio)"));
        sb.append("""
                -- Requisito: la cuenta ya existe, creada por la API (POST /api/v1/auth/register) con el correo de abajo,
                -- zona horaria America/Lima y SIN participacion en el estudio. Este script NO crea usuarios, contrasenas
                -- ni tokens; solo anade el perfil (lo mismo que deja POST /api/v1/users/me/profile).
                DO $demo$
                DECLARE
                  v_email CONSTANT text := '@EMAIL@';
                  v_user bigint;
                  v_estado text; v_zona text; v_participa boolean; v_retirado timestamptz;
                  v_w3 date := date_trunc('week', now() AT TIME ZONE 'America/Lima')::date - 21;
                  v_sql text;
                  i int;
                BEGIN
                  IF NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = '65' AND success) THEN
                    RAISE EXCEPTION 'DEMO 01: la base no tiene la migracion V65 aplicada; el dataset se preparo para ese esquema.';
                  END IF;
                  SELECT user_id, status, timezone, is_study_participant, withdrawn_at
                    INTO v_user, v_estado, v_zona, v_participa, v_retirado
                    FROM users WHERE lower(email) = lower(v_email);
                  IF v_user IS NULL THEN
                    RAISE EXCEPTION 'DEMO 01: no existe la cuenta %. Registrela antes por la API (ver README_EJECUCION.md).', v_email;
                  END IF;
                  IF v_estado <> 'ACTIVE' THEN
                    RAISE EXCEPTION 'DEMO 01: la cuenta % no esta ACTIVE (estado %).', v_user, v_estado;
                  END IF;
                  IF v_zona <> 'America/Lima' THEN
                    RAISE EXCEPTION 'DEMO 01: la cuenta tiene zona % y el dataset se calcula en America/Lima.', v_zona;
                  END IF;
                  IF v_participa AND v_retirado IS NULL THEN
                    -- Un participante activo entra en el cierre semanal de los lunes, que generaria un plan nuevo (con IA
                    -- si esta activada: coste en Replicate) y alteraria la demo.
                    RAISE EXCEPTION 'DEMO 01: la cuenta participa en el estudio. Registrela con acceptsStudyParticipation=false.';
                  END IF;
                  IF EXISTS (SELECT 1 FROM user_profiles WHERE user_id = v_user) THEN
                    IF EXISTS (SELECT 1 FROM user_profiles WHERE user_id = v_user AND goal_type = 'GAIN_MUSCLE'
                               AND fitness_level = 'INTERMEDIATE' AND days_per_week = 5 AND session_minutes = 60
                               AND training_location = 'GYM') THEN
                      RAISE NOTICE 'DEMO 01: el perfil de la cuenta % ya existe y coincide. No se cambia nada.', v_user;
                      RETURN;
                    END IF;
                    RAISE EXCEPTION 'DEMO 01: la cuenta % ya tiene un perfil distinto del de la demo. Use una cuenta nueva.', v_user;
                  END IF;
                """.replace("@EMAIL@", email));
        sb.append("  v_sql := $q$\n").append(insert("user_profiles", fila)).append("$q$;\n");
        sb.append(sustitucion());
        sb.append("""
                  EXECUTE v_sql;
                  RAISE NOTICE 'DEMO 01 aplicado: perfil de la cuenta %', v_user;
                END
                $demo$;
                """);
        return sb.toString();
    }

    // ------------------------------------------------------------------ 02–05

    String bloque(String n, String titulo, List<Parte> partes, Map<String, Set<Long>> exportadas, String requisitos,
                  boolean anclaPorCalendario, String comprobacionFinal) {
        var datos = new StringBuilder();
        var colisiones = new StringBuilder();
        var ejercicios = new TreeMap<Long, String>();
        String marcaTabla = null; long marcaId = -1;
        var resumen = new ArrayList<String>();
        for (var p : partes) {
            var filas = filas(p.tabla(), p.clave(), p.filtro(), p.args());
            if (filas.isEmpty()) continue;
            var ids = new TreeSet<Long>();
            for (var f : filas) ids.add(Long.parseLong(f.get(p.clave()).valor()));
            var prev = exportadas.computeIfAbsent(p.tabla(), k -> new HashSet<>());
            for (Long id : ids) if (!prev.add(id)) throw new IllegalStateException("fila exportada dos veces: " + p.tabla() + " " + id);
            if (marcaTabla == null) { marcaTabla = p.tabla(); marcaId = ids.first(); }
            datos.append(insert(p.tabla(), filas));
            colisiones.append("  IF EXISTS (SELECT 1 FROM ").append(p.tabla()).append(" WHERE ").append(p.clave())
                    .append(" = ANY('").append(arr(ids)).append("'::bigint[])) THEN\n    RAISE EXCEPTION 'DEMO ").append(n)
                    .append(": ya existen filas con ids reservados en ").append(p.tabla())
                    .append(" (de otra cuenta). No se carga nada.';\n  END IF;\n");
            resumen.add(p.tabla() + "=" + ids.size());
            for (var f : filas) {
                var e = f.get("exercise_id");
                if (e != null && e.valor() != null) ejercicios.put(Long.parseLong(e.valor()), null);
            }
        }
        if (marcaTabla == null) throw new IllegalStateException("script " + n + " vacio");
        for (var e : ejercicios.keySet())
            ejercicios.put(e, jdbc.queryForObject("SELECT source_code FROM exercises WHERE exercise_id = ?", String.class, e));

        var sb = new StringBuilder(cabecera(n, titulo));
        sb.append("-- Filas: ").append(String.join(", ", resumen)).append("\n");
        sb.append("DO $demo$\nDECLARE\n  v_email CONSTANT text := '").append(email).append("';\n  v_user bigint;\n  v_w3 date;\n")
                .append("  v_sql text;\n  v_falta text;\n  i int;\nBEGIN\n");
        sb.append("  IF NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = '65' AND success) THEN\n")
                .append("    RAISE EXCEPTION 'DEMO ").append(n).append(": la base no tiene la migracion V65 aplicada.';\n  END IF;\n");
        sb.append("  SELECT user_id INTO v_user FROM users WHERE lower(email) = lower(v_email);\n")
                .append("  IF v_user IS NULL THEN\n    RAISE EXCEPTION 'DEMO ").append(n)
                .append(": no existe la cuenta %. Registrela por la API y ejecute 01.', v_email;\n  END IF;\n");
        sb.append("  -- Idempotencia: si este paso ya se aplico a la cuenta, no se cambia nada.\n")
                .append("  IF EXISTS (SELECT 1 FROM ").append(marcaTabla).append(" WHERE ").append(clave(marcaTabla))
                .append(" = ").append(marcaId).append(") THEN\n");
        sb.append("    IF EXISTS (SELECT 1 FROM ").append(marcaTabla).append(" WHERE ").append(clave(marcaTabla))
                .append(" = ").append(marcaId).append(" AND ").append(usuarioDe(marcaTabla)).append(") THEN\n")
                .append("      RAISE NOTICE 'DEMO ").append(n).append(": ya aplicado a la cuenta %. No se cambia nada.', v_user;\n")
                .append("      RETURN;\n    END IF;\n    RAISE EXCEPTION 'DEMO ").append(n)
                .append(": el id reservado ").append(marcaId).append(" de ").append(marcaTabla)
                .append(" pertenece a otra cuenta. No se carga nada.';\n  END IF;\n");
        sb.append("  ").append(requisitos).append("\n");
        if (anclaPorCalendario)
            sb.append("  -- Ancla: W0 es la semana en curso en Lima; el historial empieza tres lunes antes.\n")
                    .append("  v_w3 := date_trunc('week', now() AT TIME ZONE 'America/Lima')::date - 21;\n");
        else
            sb.append("  -- Ancla: la misma semana W-3 con la que se cargo 02 (todas las fechas quedan coherentes).\n")
                    .append("  SELECT week_start_date INTO v_w3 FROM weekly_training_plans WHERE plan_id = ")
                    .append(inicialId)
                    .append(" AND user_id = v_user;\n");
        sb.append("  -- Ids reservados libres.\n").append(colisiones);
        if (!ejercicios.isEmpty()) {
            sb.append("  -- Mismo catalogo: cada ejercicio existe con el mismo source_code que en el generador.\n");
            sb.append("  SELECT string_agg(v.id::text, ', ') INTO v_falta FROM (VALUES ");
            sb.append(ejercicios.entrySet().stream().map(e -> "(" + e.getKey() + "," + lit(e.getValue()) + ")")
                    .collect(Collectors.joining(", ")));
            sb.append(") AS v(id, codigo)\n    LEFT JOIN exercises e ON e.exercise_id = v.id"
                    + " WHERE e.source_code IS DISTINCT FROM v.codigo;\n");
            sb.append("  IF v_falta IS NOT NULL THEN\n    RAISE EXCEPTION 'DEMO ").append(n)
                    .append(": el catalogo no coincide (ejercicios %). No se carga nada.', v_falta;\n  END IF;\n");
        }
        sb.append("  v_sql := $q$\n").append(datos).append("$q$;\n");
        sb.append(sustitucion());
        sb.append("  EXECUTE v_sql;\n");
        if (comprobacionFinal != null) sb.append("  ").append(comprobacionFinal).append("\n");
        sb.append("  RAISE NOTICE 'DEMO ").append(n).append(" aplicado a la cuenta % (").append(String.join(", ", resumen))
                .append(")', v_user;\nEND\n$demo$;\n");
        return sb.toString();
    }

    String clave(String tabla) {
        return switch (tabla) {
            case "weekly_training_plans" -> "plan_id";
            case "workout_sessions" -> "session_id";
            default -> throw new IllegalArgumentException(tabla);
        };
    }

    String usuarioDe(String tabla) {
        return "user_id = v_user";
    }

    String sustitucion() {
        long min = -35, max = ChronoUnit.DAYS.between(w3, w0) + 70;
        return "  -- Marcadores: @@Dn@@ = lunes de W-3 + n dias; @@U@@ = la cuenta.\n"
                + "  FOR i IN " + min + ".." + max + " LOOP\n"
                + "    v_sql := replace(v_sql, '@@D' || i || '@@', to_char(v_w3 + i, 'YYYY-MM-DD'));\n  END LOOP;\n"
                + "  v_sql := replace(v_sql, '@@U@@', v_user::text);\n"
                + "  IF position('@@' IN v_sql) > 0 THEN\n    RAISE EXCEPTION 'DEMO: marcador sin sustituir';\n  END IF;\n";
    }

    String cabecera(String n, String titulo) {
        return "-- =====================================================================================================\n"
                + "-- DATASET DEMOSTRATIVO FITSENSE / KORA · " + n + " · " + titulo + "\n"
                + "-- Generado por DatasetDemoKoraGeneradorIT con los flujos reales del backend (sin IA, sin Replicate).\n"
                + "-- Semana de generacion: W0 = " + w0 + " (las fechas se trasladan a la semana real al ejecutar).\n"
                + "-- Un solo bloque DO: si algo falla, no se aplica nada. Idempotente: repetirlo no duplica filas.\n"
                + "-- No crea ni modifica usuarios, contrasenas, tokens, catalogo ni esquema.\n"
                + "-- =====================================================================================================\n";
    }

    // ------------------------------------------------------------------ filas y valores

    record Valor(String valor, String tipo) {}

    record Columna(String nombre, String tipo, boolean identidad, boolean timestamptz) {}

    List<Columna> columnas(String tabla) {
        return jdbc.query("SELECT a.attname, format_type(a.atttypid, a.atttypmod) AS tipo, a.attidentity <> '' AS identidad"
                        + " FROM pg_attribute a WHERE a.attrelid = ('public.' || ?)::regclass AND a.attnum > 0"
                        + " AND NOT a.attisdropped AND a.attgenerated = '' ORDER BY a.attnum",
                (rs, i) -> new Columna(rs.getString(1), rs.getString(2), rs.getBoolean(3),
                        rs.getString(2).equals("timestamp with time zone")), tabla);
    }

    List<LinkedHashMap<String, Valor>> filas(String tabla, String clave, String filtro, Object... args) {
        var cols = columnas(tabla);
        String sel = cols.stream().map(c -> c.timestamptz()
                        ? "to_char(" + c.nombre() + " AT TIME ZONE 'UTC', 'YYYY-MM-DD HH24:MI:SS.US') AS " + c.nombre()
                        : c.nombre() + "::text AS " + c.nombre())
                .collect(Collectors.joining(", "));
        var res = new ArrayList<LinkedHashMap<String, Valor>>();
        jdbc.query("SELECT " + sel + " FROM " + tabla + " WHERE " + filtro + " ORDER BY " + clave, rs -> {
            var m = new LinkedHashMap<String, Valor>();
            for (var c : cols) m.put(c.nombre(), new Valor(rs.getString(c.nombre()), c.tipo()));
            res.add(m);
        }, args);
        return res;
    }

    String insert(String tabla, List<LinkedHashMap<String, Valor>> filas) {
        var cols = columnas(tabla);
        boolean identidad = cols.stream().anyMatch(Columna::identidad);
        var sb = new StringBuilder("INSERT INTO ").append(tabla).append(" (")
                .append(cols.stream().map(Columna::nombre).collect(Collectors.joining(", "))).append(")\n");
        if (identidad) sb.append("OVERRIDING SYSTEM VALUE\n");
        sb.append("VALUES\n");
        for (int i = 0; i < filas.size(); i++) {
            var f = filas.get(i);
            sb.append("  (").append(cols.stream().map(c -> valor(c, f.get(c.nombre()))).collect(Collectors.joining(", ")))
                    .append(i == filas.size() - 1 ? ");\n" : "),\n");
        }
        return sb.toString();
    }

    String valor(Columna c, Valor v) {
        if (v.valor() == null) return "NULL";
        if (c.nombre().equals("user_id")) {
            if (Long.parseLong(v.valor()) != usuarioGenerador) throw new IllegalStateException("fila de otro usuario");
            return "@@U@@";
        }
        String t = marcar(v.valor());
        if (c.timestamptz()) return "'" + t + "+00'::timestamptz";
        return switch (c.tipo()) {
            case "bigint", "integer", "smallint" -> t;
            case "boolean" -> t.equals("true") ? "TRUE" : "FALSE";
            default -> lit(t) + "::" + c.tipo();
        };
    }

    static String lit(String s) {
        return "'" + s.replace("'", "''") + "'";
    }

    static final Pattern FECHA = Pattern.compile("(?<!\\d)(\\d{4}-\\d{2}-\\d{2})(?!\\d)");

    /** Fechas de la ventana de la demo -> marcador relativo a W-3; el user_id del generador -> marcador de la cuenta. */
    String marcar(String s) {
        if (s.contains("@@") || s.contains("$q$")) throw new IllegalStateException("texto con marcador reservado: " + s);
        s = s.replaceAll("(?<!\\d)" + usuarioGenerador + "(?!\\d)", "@@U@@");
        Matcher m = FECHA.matcher(s);
        var sb = new StringBuilder();
        while (m.find()) {
            String r = m.group(1);
            try {
                LocalDate d = LocalDate.parse(r);
                long n = ChronoUnit.DAYS.between(w3, d);
                if (n >= -35 && n <= ChronoUnit.DAYS.between(w3, w0) + 70) r = "@@D" + n + "@@";
            } catch (DateTimeParseException ignorada) {
                // no es una fecha: se deja tal cual
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(r));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
