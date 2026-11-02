package main.web.services.fitsense;

import org.springframework.jdbc.core.JdbcTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Tiempo controlado por las pruebas. No cambia nada del backend: solo fija la semana de los escenarios y abre la
 * ventana de ejecucion de las sesiones que una prueba va a hacer.
 * <p>
 * POR QUE. El motor de reglas siembra su aleatoriedad con {@code userId x 1.000.003 + weekStartDate.toEpochDay()}
 * (semilla estable por usuario y semana, a proposito). Las referencias de varias pruebas (planes esperados, catalogo
 * del usuario 31, secuencias multisemana) se capturaron con la semana que empieza el 2026-11-02. Calcular esa semana
 * con {@code LocalDate.now()} cambia la semilla cada lunes y rompe las referencias sin que cambie nada del codigo.
 * <p>
 * EJECUCION. Una sesion solo se puede abrir antes de {@code expires_at} (23:59 local del dia programado), comparado
 * con el reloj real. Con una semana fija, en cuanto esa semana queda en el pasado las sesiones ya no se podrian
 * abrir. {@link #abrirVentanaDeEjecucion} lleva {@code expires_at} al futuro para los entrenamientos del usuario de
 * la prueba: deja la misma condicion que cuando la semana era futura (nada vencido) y no toca ejercicios, dosis,
 * fechas ni estados.
 */
public final class RelojPruebas {

    /** Lunes de la semana con la que se capturaron las referencias. */
    public static final LocalDate SEMANA_REFERENCIA = LocalDate.of(2026, 11, 2);

    static {
        if (SEMANA_REFERENCIA.getDayOfWeek() != DayOfWeek.MONDAY)
            throw new IllegalStateException("la semana de referencia debe empezar en lunes");
    }

    private RelojPruebas() {}

    /**
     * Ningun entrenamiento del usuario vence antes de una semana desde ahora. Devuelve las filas actualizadas.
     * Llamarlo justo antes de abrir sesiones de una semana fija.
     */
    public static int abrirVentanaDeEjecucion(JdbcTemplate jdbc, long userId) {
        return jdbc.update("UPDATE planned_workouts pw SET expires_at = GREATEST(pw.expires_at, now() + interval '7 days')"
                + " FROM weekly_training_plans p WHERE pw.plan_id = p.plan_id AND p.user_id = ?", userId);
    }
}
