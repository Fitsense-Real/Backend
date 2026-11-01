package main.web.services.fitsense.planning.interfaces.rest.resources;

/**
 * V44 · Disponibilidad frente a duracion estimada de una sesion (opcional; solo con
 * {@code fitsense.api.duracion-estimada.enabled=true}).
 *
 * @param availableMinutes   minutos que la persona tiene para entrenar (perfil congelado; NO cambia)
 * @param estimatedMinutes   duracion estimada del contenido (la formula de V17/V20), redondeada al minuto
 * @param differenceMinutes  availableMinutes - estimatedMinutes
 * @param policy             version de politica con la que se genero y valido el plan
 * @param reason             DENTRO_DEL_TIEMPO, DURACION_POR_CONTENIDO (C2-D; antes ESTRUCTURA_COMPLETA, sin
 *                           consumidores), REDUCCION_AUTORIZADA (adaptacion), SESION_ACORTADA (falta de tiempo) o
 *                           SIN_DATOS (plan anterior a V44)
 * @param message            texto para el usuario; nunca dice que la sesion es optima ni sugiere rellenar tiempo
 */
public record DurationInfoResource(Integer availableMinutes, Integer estimatedMinutes, Integer differenceMinutes,
                                   String policy, String reason, String message) {}
