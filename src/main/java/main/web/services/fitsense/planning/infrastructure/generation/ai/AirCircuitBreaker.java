package main.web.services.fitsense.planning.infrastructure.generation.ai;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * El cortacircuitos del proveedor de IA, sacado de {@code PlanGenerationPipeline}
 * sin cambiarle una coma de comportamiento.
 * <p>
 * POR QUE SE EXTRAE. Estaba inline -un {@code AtomicInteger} y un
 * {@code volatile Instant} del pipeline- y por eso no se podia ni probar ni
 * observar. El 13/10/2026, midiendo tres perfiles, dos generaciones cayeron al
 * motor de reglas por tiempo de espera y el contador quedo en 2 de 3: un tercer
 * fallo seguido habria abierto el cortacircuito y las diez generaciones
 * siguientes habrian salido por reglas SIN QUE EL LOG LO DIJERA, porque el
 * unico aviso estaba en {@code log.debug} y el nivel por omision es INFO. Una
 * comparacion entre perfiles hecha en ese estado no vale nada y nadie se habria
 * enterado.
 * <p>
 * LA POLITICA NO CAMBIA, y se repite aqui para que quede en un sitio:
 * <ul>
 *   <li>tres fallos SEGUIDOS del proveedor abren el corte;</li>
 *   <li>abierto, se va directo al motor de reglas durante diez minutos;</li>
 *   <li>cualquier exito, y tambien un borrador rechazado por validacion,
 *       reinician el contador: un plan invalido es capacidad del modelo, no
 *       una caida del proveedor.</li>
 * </ul>
 * <p>
 * ESTADO EN MEMORIA, a proposito y conviene saberlo: reiniciar la aplicacion lo
 * pone a cero. Es la forma mas barata de empezar una medicion desde un estado
 * conocido.
 * <p>
 * El reloj se inyecta para poder probar la ventana de diez minutos sin esperar
 * diez minutos. En produccion es {@code Instant::now}.
 */
public class AirCircuitBreaker {

    /** Fallos seguidos del proveedor antes de dejar de intentarlo. */
    public static final int UMBRAL = 3;

    /** Cuanto se espera antes de volver a probar con la IA. */
    public static final Duration DESCANSO = Duration.ofMinutes(10);

    private final Supplier<Instant> reloj;
    private final AtomicInteger fallosSeguidos = new AtomicInteger();
    private volatile Instant reabrirDespuesDe = Instant.EPOCH;

    /**
     * Cuantos fallos seguidos abrieron el corte la ultima vez. Se guarda aparte
     * porque al abrir se reinicia el contador, y sin esto el log diria
     * "fallos_consecutivos=0" justo cuando mas importa saber que fueron tres.
     */
    private volatile int fallosQueAbrieron;

    public AirCircuitBreaker() {
        this(Instant::now);
    }

    public AirCircuitBreaker(Supplier<Instant> reloj) {
        this.reloj = reloj;
    }

    /** Si se puede llamar al proveedor. No registra nada: lo hace quien llama. */
    public boolean disponible() {
        return !reloj.get().isBefore(reabrirDespuesDe);
    }

    /** Lo contrario de {@link #disponible()}, para que el log se lea bien. */
    public boolean abierto() {
        return !disponible();
    }

    /**
     * Un fallo del PROVEEDOR: no respondio, respondio mal o agoto el tiempo.
     *
     * @return true si este fallo ha abierto el corte.
     */
    public boolean registrarFalloDeProveedor() {
        int seguidos = fallosSeguidos.incrementAndGet();
        if (seguidos >= UMBRAL) {
            fallosQueAbrieron = seguidos;
            reabrirDespuesDe = reloj.get().plus(DESCANSO);
            fallosSeguidos.set(0);
            return true;
        }
        return false;
    }

    /**
     * El proveedor respondio. Incluye el caso de un borrador RECHAZADO por
     * validacion: el modelo contesto, asi que no es una caida.
     */
    public void registrarRespuestaDelProveedor() {
        fallosSeguidos.set(0);
    }

    /** Fallos seguidos acumulados ahora mismo, sin contar los que ya abrieron. */
    public int fallosSeguidos() {
        return fallosSeguidos.get();
    }

    /** Cuantos fallos abrieron el corte la ultima vez. Cero si nunca se abrio. */
    public int fallosQueAbrieron() {
        return fallosQueAbrieron;
    }

    /** Desde cuando se vuelve a permitir la IA. {@code Instant.EPOCH} si nunca se abrio. */
    public Instant reabrirDespuesDe() {
        return reabrirDespuesDe;
    }

    /** Una linea con el estado, para el log de IA_NO_INVOCADA. */
    public String describe() {
        return "fallos_consecutivos=" + fallosQueAbrieron
                + " reintento_permitido_desde=" + reabrirDespuesDe;
    }
}
