package main.web.services.fitsense.adaptation.application.internal.outboundservices.acl;

import main.web.services.fitsense.analytics.interfaces.acl.AnalyticsContextFacade;
import main.web.services.fitsense.analytics.interfaces.acl.SessionOutcomeView;
import main.web.services.fitsense.analytics.interfaces.acl.WeeklyMetricsView;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Capa anticorrupcion hacia analytics: el disparador del ajuste. */
@Service
public class ExternalAnalyticsService {

    private final AnalyticsContextFacade analyticsContextFacade;

    public ExternalAnalyticsService(AnalyticsContextFacade analyticsContextFacade) {
        this.analyticsContextFacade = analyticsContextFacade;
    }

    public Optional<WeeklyMetricsView> fetchWeek(Long userId, LocalDate weekStartDate) {
        return analyticsContextFacade.fetchWeek(userId, weekStartDate);
    }

    /**
     * Lo que paso en cada sesion de la semana, con las senaladas ya marcadas.
     * <p>
     * Se consulta en el momento y no se persiste: los umbrales que deciden si
     * una sesion fue un problema son los de la configuracion activa, y
     * guardarlos ya resueltos obligaria a recalcular al cambiarlos.
     */
    public List<SessionOutcomeView> fetchSessionOutcomes(Long userId, LocalDate weekStartDate) {
        return analyticsContextFacade.fetchSessionOutcomes(userId, weekStartDate);
    }
}
