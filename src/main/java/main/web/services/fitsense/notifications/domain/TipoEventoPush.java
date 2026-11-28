package main.web.services.fitsense.notifications.domain;

/**
 * Los cuatro eventos del contrato push con Mobile. Cada uno corresponde a un proceso que YA existe en el backend y que
 * deja una fila confirmada en la base (ver docs/FitSense/notificaciones_push/EVENTOS_Y_PROCESOS.md):
 * <ul>
 *   <li>{@link #PLAN_GENERATED}: una operacion de generacion inicial asincrona termina en COMPLETED
 *       ({@code plan_generation_operations}).</li>
 *   <li>{@link #PLAN_ADAPTED}: el cierre semanal publica un plan con una adaptacion APLICADA
 *       ({@code plan_adaptation_attempts}).</li>
 *   <li>{@link #WEEKLY_PLAN_READY}: el cierre semanal publica el plan de la semana siguiente sin adaptacion aplicada
 *       (continuidad, progresion, sin efecto o respaldo).</li>
 *   <li>{@link #ACCOUNT_NOTICE}: aviso de seguridad de la cuenta; hoy solo el cambio de contrasena.</li>
 * </ul>
 */
public enum TipoEventoPush {
    PLAN_GENERATED(Categoria.PLANES, PantallaDestino.WEEKLY_PLAN),
    PLAN_ADAPTED(Categoria.PLANES, PantallaDestino.WEEKLY_PLAN),
    WEEKLY_PLAN_READY(Categoria.PLANES, PantallaDestino.WEEKLY_PLAN),
    ACCOUNT_NOTICE(Categoria.CUENTA, PantallaDestino.ACCOUNT_SECURITY);

    /** Las preferencias del usuario se guardan por categoria, no por evento. */
    public enum Categoria { PLANES, CUENTA }

    /** Pantalla de la app que abre la notificacion. Es parte del contrato: Mobile la traduce a su ruta. */
    public enum PantallaDestino { WEEKLY_PLAN, ACCOUNT_SECURITY }

    private final Categoria categoria;
    private final PantallaDestino pantalla;

    TipoEventoPush(Categoria categoria, PantallaDestino pantalla) {
        this.categoria = categoria;
        this.pantalla = pantalla;
    }

    public Categoria categoria() {
        return categoria;
    }

    public PantallaDestino pantalla() {
        return pantalla;
    }
}
