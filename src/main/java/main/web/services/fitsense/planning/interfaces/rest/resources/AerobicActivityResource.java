package main.web.services.fitsense.planning.interfaces.rest.resources;

/**
 * Actividad aerobica de la semana (opcional; solo con {@code fitsense.metricas.aerobica.enabled=true}).
 * Programado frente a ejecutado y externo; sin equivalencias si la intensidad es desconocida.
 */
public record AerobicActivityResource(double programmedWorkMinutes, double programmedBlockMinutes,
                                      Double executedMinutes, Double externalMinutes, String intensity,
                                      Double equivalentMinutes, String message) {}
