package main.web.services.fitsense.analytics.domain.services;

import main.web.services.fitsense.analytics.domain.model.queries.GetSessionOutcomesQuery;
import main.web.services.fitsense.analytics.domain.model.valueobjects.SessionOutcome;

import java.util.List;

/**
 * Consulta de lo ocurrido sesion a sesion. Separada del calculo semanal porque
 * no persiste nada: se resuelve en el momento con los umbrales de la
 * configuracion activa, de modo que cambiarlos no obliga a recalcular filas.
 */
public interface SessionOutcomeQueryService {

    /** Vacia si la semana no tiene plan: sin previstos no hay sesiones que juzgar. */
    List<SessionOutcome> handle(GetSessionOutcomesQuery query);
}
