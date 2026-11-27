package main.web.services.fitsense.orchestration.infrastructure.scheduling;

import main.web.services.fitsense.iam.interfaces.acl.IamContextFacade;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dispara el ciclo semanal los lunes a las 03:00 hora LOCAL de cada participante.
 * <p>
 * Corre cada hora y pregunta que zonas horarias tienen participantes activos, en
 * vez de un unico cron sobre el reloj del servidor. Con participantes en zonas
 * distintas, un cron global cerraria la semana de unos el domingo por la noche y
 * la de otros el lunes por la tarde, y la adherencia dejaria de ser comparable
 * entre ellos.
 * <p>
 * Las 03:00 y no medianoche: deja margen para las sesiones que empiezan tarde el
 * domingo y para la tarea de vencimiento de las 00:30.
 * <p>
 * <b>B3 · recuperacion de cierres perdidos (P0-05).</b> Antes, si el tick de las 03:00 no llegaba (reinicio o
 * despliegue a esa hora, el hilo del planificador ocupado, un ciclo EN_CURSO o una cohorte de otra zona que tardaba mas
 * de una hora), esa semana quedaba sin cerrar ni plan hasta el lunes siguiente. Ahora:
 * <ul>
 *   <li>a las 03:00 se ejecuta a TODOS los participantes de la zona, exactamente como antes;</li>
 *   <li>en cada tick posterior dentro de {@code fitsense.weekly-cycle.catch-up-hours} (24 por omision: la misma ventana
 *       en que la ampliacion V59/V66 relanza un ciclo interrumpido), se ejecuta solo a los PENDIENTES
 *       ({@link CiclosSemanalesPendientes}): sin la medicion de la semana cerrada o sin plan activo para la nueva;</li>
 *   <li>cada participante se recupera como mucho {@code fitsense.weekly-cycle.catch-up-max-attempts} veces por semana,
 *       para no pagar la IA cada hora si una generacion falla de forma persistente.</li>
 * </ul>
 * No hay mecanismo nuevo de idempotencia: {@link WeeklyCycleService#ejecutar} ya cierra una semana una sola vez, mide
 * una vez por usuario y semana, publica una vez por medicion y devuelve {@code EN_CURSO} si otra ejecucion (otra
 * instancia, otro tick o la recuperacion de V66) tiene el ciclo de ese participante. Fuera de la ventana no se publica
 * una semana tarde. Cubre los ciclos que NUNCA empezaron; los que empezaron y se interrumpieron los relanza V66.
 */
@Component
public class WeeklyCycleScheduler {

    private static final Logger log = LoggerFactory.getLogger(WeeklyCycleScheduler.class);
    private static final int CLOSING_HOUR = 3;

    private final IamContextFacade iamContextFacade;
    private final WeeklyCycleService weeklyCycleService;
    private final CiclosSemanalesPendientes pendientes;
    private final Duration ventana;
    private final int maximoDeRecuperaciones;
    private final Clock clock;
    /** usuario|semana medida -> recuperaciones ya intentadas (en memoria: tras un reinicio se vuelve a intentar). */
    private final ConcurrentHashMap<String, Integer> recuperaciones = new ConcurrentHashMap<>();

    public WeeklyCycleScheduler(IamContextFacade iamContextFacade,
                                WeeklyCycleService weeklyCycleService,
                                CiclosSemanalesPendientes pendientes,
                                @Value("${fitsense.weekly-cycle.catch-up-hours:24}") long horas,
                                @Value("${fitsense.weekly-cycle.catch-up-max-attempts:3}") int maximo,
                                Optional<Clock> clock) {
        if (horas < 0 || horas > 144)
            throw new IllegalArgumentException("fitsense.weekly-cycle.catch-up-hours debe estar entre 0 y 144.");
        if (maximo < 1) throw new IllegalArgumentException("fitsense.weekly-cycle.catch-up-max-attempts >= 1.");
        this.iamContextFacade = iamContextFacade;
        this.weeklyCycleService = weeklyCycleService;
        this.pendientes = pendientes;
        this.ventana = Duration.ofHours(horas);
        this.maximoDeRecuperaciones = maximo;
        this.clock = clock.orElse(Clock.systemUTC());
    }

    @Scheduled(cron = "0 0 * * * *")
    public void runHourly() {
        for (String zoneId : iamContextFacade.fetchActiveTimezones()) {
            ZonedDateTime localNow;
            try {
                localNow = ZonedDateTime.now(clock.withZone(ZoneId.of(zoneId)));
            } catch (RuntimeException e) {
                log.warn("Zona horaria invalida en la base: {}", zoneId);
                continue;
            }

            if (localNow.getDayOfWeek() == DayOfWeek.MONDAY && localNow.getHour() == CLOSING_HOUR) {
                runForZone(zoneId, localNow.toLocalDate(), false);
                continue;
            }
            var lunes = localNow.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            var cierre = lunes.atTime(CLOSING_HOUR, 0).atZone(localNow.getZone());
            if (localNow.isAfter(cierre) && localNow.isBefore(cierre.plus(ventana)))
                runForZone(zoneId, lunes, true);
        }
    }

    private void runForZone(String zoneId, LocalDate localToday, boolean soloPendientes) {
        var newWeek = TrainingWeek.containing(localToday);
        var measuredWeek = newWeek.previous();

        List<Long> participants = iamContextFacade.fetchActiveParticipantIdsInTimezone(zoneId);
        if (soloPendientes) {
            olvidarSemanasAnteriores(measuredWeek.startDate());
            participants = pendientes.de(participants, measuredWeek.startDate(), newWeek.startDate()).stream()
                    .filter(u -> recuperaciones.getOrDefault(clave(u, measuredWeek.startDate()), 0)
                            < maximoDeRecuperaciones)
                    .toList();
            if (participants.isEmpty()) return;
            log.warn("CICLO_SEMANAL_PENDIENTE zona={} semana_medida={} pendientes={}: el cierre de las 03:00 no los"
                    + " completo; se recuperan", zoneId, measuredWeek.startDate(), participants.size());
        } else {
            log.info("Cierre semanal en {}: {} participantes, semana medida {}",
                    zoneId, participants.size(), measuredWeek.startDate());
        }

        for (Long userId : participants) {
            if (soloPendientes) recuperaciones.merge(clave(userId, measuredWeek.startDate()), 1, Integer::sum);
            try {
                var r = weeklyCycleService.ejecutar(userId, measuredWeek.startDate(), newWeek.startDate());
                if (r.estado() != main.web.services.fitsense.orchestration.infrastructure.locking.CicloSemanalBloqueo
                        .Estado.EJECUTADO)
                    log.warn("Ciclo semanal del usuario {} no ejecutado ({}); se reintentara en el siguiente tick"
                            + " dentro de la ventana de recuperacion.", userId, r.estado());
                else if (soloPendientes)
                    log.warn("CICLO_SEMANAL_RECUPERADO usuario={} semana_medida={} plan={}", userId,
                            measuredWeek.startDate(), r.planId().orElse(null));
            } catch (RuntimeException e) {
                // Aislar por participante: un perfil incompleto o un catalogo sin
                // material no puede dejar sin plan al resto de la cohorte.
                log.error("Fallo el ciclo semanal del usuario {}: {}", userId, e.getMessage(), e);
            }
        }
    }

    private static String clave(Long userId, LocalDate semanaMedida) {
        return userId + "|" + semanaMedida;
    }

    private void olvidarSemanasAnteriores(LocalDate semanaMedida) {
        String sufijo = "|" + semanaMedida;
        recuperaciones.keySet().removeIf(k -> !k.endsWith(sufijo));
    }
}
