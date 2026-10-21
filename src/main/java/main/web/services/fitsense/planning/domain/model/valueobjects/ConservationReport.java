package main.web.services.fitsense.planning.domain.model.valueobjects;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * El resultado de la validacion de conservacion (V8C).
 * <p>
 * NO ES SOLO UNA LISTA DE PROBLEMAS. Distingue tres desenlaces que no son lo
 * mismo, y esa distincion es justamente lo que la tesis tiene que demostrar:
 * <ul>
 *   <li><b>Adaptacion aplicada.</b> La sesion estaba senalada, la IA propuso un
 *       cambio, el backend lo valido y se aplico.</li>
 *   <li><b>Adaptacion sin efecto.</b> La sesion estaba senalada y volvio igual.
 *       Se permite -conservar nunca es inseguro, salvo ante dolor- pero NO cuenta
 *       como adaptacion exitosa. No se inventa que no existieran alternativas:
 *       solo se registra que no hubo cambio.</li>
 *   <li><b>Conservada.</b> La sesion no estaba senalada y quedo identica, que es
 *       lo que se le pidio.</li>
 * </ul>
 * Por eso esto es un valor y no una traza de log: el desenlace hay que
 * persistirlo para la tesis, no leerlo en la consola.
 * <p>
 * PENDIENTE DECLARADO. Mas adelante, al integrar el prompt, cada sesion sin
 * efecto llevara ademas la justificacion explicita de por que la IA decidio
 * conservarla. Hoy ese canal no existe -ni el borrador ni el esquema JSON tienen
 * donde ponerla-, asi que aqui se registra el hecho y no la razon.
 *
 * @param problems   lo que bloquea. Vacio cuando el plan derivado es aceptable.
 * @param aplicadas  identificadores de origen de las sesiones senaladas que
 *                   cambiaron dentro de lo permitido.
 * @param sinEfecto  identificadores de origen de las sesiones senaladas que
 *                   volvieron identicas.
 * @param conservadas identificadores de origen de las sesiones no senaladas que
 *                   quedaron identicas.
 */
public record ConservationReport(List<String> problems,
                                 List<Long> aplicadas,
                                 List<Long> sinEfecto,
                                 List<Long> conservadas,

                                 /**
                                  * Las fechas de las sesiones senaladas cuyo tipo de
                                  * intervencion autoriza acortarlas, y SOLO esas.
                                  * <p>
                                  * V20 exime a estas del piso de duracion del perfil.
                                  * No se puede desactivar el piso para la semana
                                  * entera: relajaria las demas sesiones y, sobre todo,
                                  * {@code PlanInputSnapshot} lee ese mismo piso, asi
                                  * que cambiaria lo que el prompt le dice a la IA.
                                  * <p>
                                  * La fecha sirve de clave porque V12 prohibe dos
                                  * entrenamientos el mismo dia DENTRO de la semana
                                  * generada, que es el unico alcance en que se usa.
                                  */
                                 Set<LocalDate> fechasConDuracionReducible) {

    public ConservationReport {
        problems = problems == null ? List.of() : List.copyOf(problems);
        aplicadas = aplicadas == null ? List.of() : List.copyOf(aplicadas);
        sinEfecto = sinEfecto == null ? List.of() : List.copyOf(sinEfecto);
        conservadas = conservadas == null ? List.of() : List.copyOf(conservadas);
        fechasConDuracionReducible = fechasConDuracionReducible == null
                ? Set.of() : Set.copyOf(fechasConDuracionReducible);
    }

    public static ConservationReport vacio() {
        return new ConservationReport(List.of(), List.of(), List.of(), List.of(), Set.of());
    }

    public boolean aceptable() {
        return problems.isEmpty();
    }

    /**
     * Si alguna sesion senalada cambio de verdad. Una semana entera de
     * adaptaciones sin efecto es un plan valido y una adaptacion que no ocurrio,
     * y las dos cosas son ciertas al mismo tiempo.
     */
    public boolean huboAdaptacionEfectiva() {
        return !aplicadas.isEmpty();
    }

    /** Una linea para la traza, sin sustituir a la persistencia. */
    public String describe() {
        return "V8C: %d aplicadas, %d sin efecto, %d conservadas, %d problemas"
                .formatted(aplicadas.size(), sinEfecto.size(), conservadas.size(), problems.size());
    }
}
