package main.web.services.fitsense.planning.application.internal.commandservices;

import main.web.services.fitsense.orchestration.infrastructure.locking.CicloSemanalBloqueo;
import main.web.services.fitsense.planning.domain.exceptions.GeneracionEnCursoException;
import main.web.services.fitsense.planning.domain.exceptions.GeneracionSinExclusionException;
import main.web.services.fitsense.planning.domain.model.aggregates.WeeklyTrainingPlan;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * R-5 · LA GENERACION DEL PRIMER PLAN, SIN DUPLICADOS NI CONSUMO EVITABLE (resiliencia de Replicate, 11/11/2026).
 * <p>
 * Antes, {@code POST /users/me/plan/generate} hacia prepare, generate y persist sin exclusion: dos peticiones
 * simultaneas pasaban las dos {@code prepare} (aun no habia plan), pagaban hasta 3 predicciones cada una y la segunda
 * fallaba al persistir. Y una persona podia repetir la peticion sin limite mientras no tuviera plan.
 * <ol>
 *   <li><b>Exclusion entre peticiones e instancias:</b> el mismo bloqueo consultivo de sesion de PostgreSQL, y la
 *       misma clave por persona, que el ciclo semanal ({@link CicloSemanalBloqueo}). Asi tampoco coinciden la peticion
 *       y el ciclo del lunes. Si otro lo tiene: 409, y no se llama a nadie. Si no se puede tomar: 503. Si el proceso
 *       muere, PostgreSQL suelta el bloqueo al cerrarse la conexion.</li>
 *   <li><b>Duplicados:</b> {@code prepare} sigue rechazando la peticion si ya hay plan para esa semana (como antes),
 *       dentro del bloqueo, asi que una peticion repetida no paga nada.</li>
 *   <li><b>Frecuencia:</b> {@link LimiteDeGeneraciones}, solo para las peticiones que llegan a generar.</li>
 * </ol>
 * No cambia que se genera ni como: el pipeline, los validadores y el respaldo son los mismos.
 */
@Service
public class GeneracionInicialService {

    private static final Logger log = LoggerFactory.getLogger(GeneracionInicialService.class);

    private final WeeklyTrainingPlanCommandService commandService;
    private final CicloSemanalBloqueo bloqueo;
    private final LimiteDeGeneraciones limite;

    public GeneracionInicialService(WeeklyTrainingPlanCommandService commandService, CicloSemanalBloqueo bloqueo,
                                    LimiteDeGeneraciones limite) {
        this.commandService = commandService;
        this.bloqueo = bloqueo;
        this.limite = limite;
    }

    public Optional<WeeklyTrainingPlan> generar(GenerateWeeklyPlanCommand command) {
        long userId = command.userId();
        var r = bloqueo.ejecutar(userId, () -> {
            var prepared = commandService.prepare(command);
            limite.consumir(userId);
            var generated = commandService.generate(prepared);
            return commandService.persist(prepared, generated);
        });
        return switch (r.estado()) {
            case EJECUTADO -> r.valor();
            case EN_CURSO -> {
                log.info("GENERACION_EN_CURSO usuario {}: peticion rechazada sin llamar a la IA", userId);
                throw new GeneracionEnCursoException(userId);
            }
            case SIN_BLOQUEO -> throw new GeneracionSinExclusionException(userId);
        };
    }
}
