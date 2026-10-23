package main.web.services.fitsense.planning.domain.model.valueobjects;

import java.time.LocalDate;

/**
 * Una sustitucion OBLIGATORIA POR SEGURIDAD en una semana derivada: un
 * ejercicio heredado de la semana medida que dejo de ser compatible por
 * seguridad con el perfil actual, reemplazado por otro elegible.
 * <p>
 * Es la unica excepcion que V8C admite a «una sesion no senalada se conserva
 * igual», y solo con estos datos: que se quito, que se puso, por que y con que
 * regla. Viaja en {@link ConservationReport} y, con el, en
 * {@code plan_adaptation_attempts.conservation_report} (V32).
 *
 * @param sesionOrigen  la sesion de la semana medida de la que deriva
 * @param quitado       el ejercicio heredado que ya no es compatible
 * @param patronQuitado su movement_pattern, para que V8C compruebe la clase de slot
 * @param puesto        el sustituto, elegible para el perfil actual
 * @param motivo        que restriccion de seguridad lo excluye (p. ej. INC-CAT-1374,
 *                      brazos_arriba, suelo)
 * @param regla         la regla de seleccion aplicada
 * @param dosisAnterior la dosis del quitado
 * @param dosisNueva    la dosis del sustituto, recalculada para sus caracteristicas
 */
public record SustitucionSeguridad(Long sesionOrigen,
                                   LocalDate fecha,
                                   Long quitado,
                                   String patronQuitado,
                                   Long puesto,
                                   String motivo,
                                   String regla,
                                   String dosisAnterior,
                                   String dosisNueva) {

    public SustitucionSeguridad {
        if (sesionOrigen == null || quitado == null || puesto == null)
            throw new IllegalArgumentException("Una sustitucion dice de que sesion, que quita y que pone.");
        if (quitado.equals(puesto))
            throw new IllegalArgumentException("Sustituir un ejercicio por si mismo no es una sustitucion.");
        if (motivo == null || motivo.isBlank())
            throw new IllegalArgumentException("Una sustitucion por seguridad declara su motivo.");
    }
}
