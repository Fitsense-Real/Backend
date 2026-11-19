package main.web.services.fitsense.planning.application.internal.commandservices;

import main.web.services.fitsense.planning.domain.model.aggregates.PlanAdaptationAttempt;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptationAttempt;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.PlanAdaptationAttemptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Registra en V32 un intento que NO publico plan, en su propia transaccion.
 * <p>
 * ES UNA CLASE APARTE Y NO UN METODO DEL SERVICIO. Spring aplica
 * {@code @Transactional} por proxy, asi que una llamada a un metodo del MISMO
 * bean no pasa por el proxy y {@code REQUIRES_NEW} no se activaria: la
 * escritura caeria dentro de la transaccion que esta fallando y se iria con
 * ella. Es un error que funciona en una prueba unitaria y falla en produccion,
 * callado.
 * <p>
 * POR QUE TRANSACCION PROPIA. Si la publicacion se deshace, una evidencia
 * escrita en esa misma transaccion se deshace tambien, y nos quedamos sin
 * registro justo del caso que mas importa: el que fallo.
 * <p>
 * POR QUE {@link TransactionTemplate} Y NO {@code @Transactional}. Con la
 * anotacion, el COMMIT lo hace el proxy DESPUES de que el metodo devuelva, asi
 * que un fallo al confirmar escapa a cualquier try/catch de dentro: el metodo
 * ya habria devuelto {@code true}. Y no es un caso rebuscado, porque
 * {@code tg_paa_coherencia} es DEFERRABLE INITIALLY DEFERRED: dispara
 * EXACTAMENTE en el COMMIT. Se comprobo contra la tabla real —el {@code INSERT}
 * devuelve {@code INSERT 0 1} sin error y el rechazo llega al confirmar—.
 * Con la plantilla, el COMMIT ocurre dentro de {@code execute(...)}, o sea
 * dentro del try.
 * <p>
 * NO SIRVE PARA LOS EXITOS, Y LO IMPIDE. Una transaccion nueva NO ve el plan
 * que la de fuera todavia no ha confirmado, asi que la clave ajena
 * {@code fk_paa_result} y el disparador que compara el origen con el del plan
 * fallarian. La evidencia de un intento que SI publico tiene que escribirse en
 * la misma transaccion que el plan.
 */
@Component
public class AdaptationFailureRecorder {

    private static final Logger log = LoggerFactory.getLogger(AdaptationFailureRecorder.class);

    private final PlanAdaptationAttemptRepository repository;
    private final TransactionTemplate transaccionNueva;

    public AdaptationFailureRecorder(PlanAdaptationAttemptRepository repository,
                                     PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.transaccionNueva = new TransactionTemplate(transactionManager);
        this.transaccionNueva.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Escribe la evidencia del fallo. NO LANZA NUNCA.
     * <p>
     * Se la llama desde el camino de error, asi que una excepcion aqui
     * sustituiria el fallo real -el que hay que diagnosticar- por un fallo del
     * registro. Lo que se pierde si esto no escribe es una fila de evidencia; lo
     * que se perderia si lanzara es saber por que fallo la semana.
     *
     * @return true solo si la fila quedo CONFIRMADA. No «guardada»: confirmada.
     */
    public boolean registrar(AdaptationAttempt intento) {
        if (intento == null) {
            log.error("V32: se pidio registrar un fallo sin intento.");
            return false;
        }

        // Un intento que publico plan no puede registrarse aqui: ver el javadoc
        // de la clase. Es un error de programacion, pero NO se lanza, porque
        // estamos en el camino de error y taparia el fallo original.
        if (intento.status().publico()) {
            log.error("V32: el intento {} publico el plan {} y se mando al registrador de"
                            + " fallos. La evidencia de un exito va en la MISMA transaccion"
                            + " que el plan, porque una transaccion nueva no ve un plan sin"
                            + " confirmar. No se registra.",
                    intento.status(), intento.resultingPlanId());
            return false;
        }

        try {
            // saveAndFlush saca el INSERT ya, asi que una restriccion inmediata
            // falla aqui con la pila util. Las DIFERIDAS siguen disparando al
            // confirmar, y confirmar es lo ultimo que hace execute(...): por eso
            // los dos fallos caen dentro de este try.
            //
            // Si otra ejecucion del ciclo ya PUBLICO esta medicion (la que entro
            // cuando esta perdio el bloqueo de sesion), un fallo tardio de esta
            // no es un hecho de la semana: no se registra. La fila de la medicion
            // se bloquea para ver lo que la otra confirmo.
            Long[] publicado = {null};
            Long id = transaccionNueva.execute(estado -> {
                repository.bloquearMedicion(intento.weeklyMetricId());
                publicado[0] = repository.planPublicadoDeLaMedicion(intento.weeklyMetricId()).orElse(null);
                if (publicado[0] != null) return null;
                return repository.saveAndFlush(PlanAdaptationAttempt.desde(intento)).getId();
            });

            if (publicado[0] != null) {
                log.warn("V32: la medicion {} del usuario {} ya publico el plan {} en otra ejecucion del ciclo;"
                                + " el intento {} de esta no se registra.",
                        intento.weeklyMetricId(), intento.userId(), publicado[0], intento.status());
                return false;
            }

            if (id == null) {
                log.error("V32: la transaccion del intento {} del usuario {} termino sin"
                                + " identificador. No se puede dar por registrado.",
                        intento.status(), intento.userId());
                return false;
            }

            log.info("V32: intento {} confirmado como fila {} · usuario {} · semana {} ·"
                            + " decision {} · senaladas {} · motivo: {}",
                    intento.status(), id, intento.userId(), intento.newWeekStartDate(),
                    intento.decision(), intento.targetedSessions(), intento.rejectionReason());
            return true;

        } catch (RuntimeException e) {
            // Si la fila no entro, esto es lo unico que queda de que el intento
            // ocurrio. NO es la evidencia de V32 ni la sustituye: son los campos
            // que permiten reconstruir a mano que paso, no la fila completa.
            log.error("V32: no se pudo registrar el intento. Lo que se intentaba guardar:"
                            + " usuario {} · semana {} · decision {} · estado {} ·"
                            + " senaladas {} · intervenciones {} · motivo: {}."
                            + " Fallo del registro: {}",
                    intento.userId(), intento.newWeekStartDate(), intento.decision(),
                    intento.status(), intento.targetedSessions(),
                    intento.targetedInterventions(), intento.rejectionReason(),
                    e.toString(), e);
            return false;
        }
    }
}
