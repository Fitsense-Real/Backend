package main.web.services.fitsense.planning.domain.model.valueobjects;

import java.time.LocalDate;

/**
 * UNA RETIRADA LOCALIZADA POR DOLOR (candidata, {@code fitsense.adaptation.pain-localized-removal}, APAGADA por
 * defecto): un ejercicio heredado de la semana medida, excluido por DOLOR REGISTRADO (P-02), que SUST-SEG-1 no pudo
 * sustituir, se retira de su sesion SIN poner nada en su lugar. Todo lo demas de la sesion se conserva.
 * <p>
 * Es la segunda excepcion, junto a {@link SustitucionSeguridad}, que V8C admite a «una sesion se conserva igual» en la
 * continuidad, y solo si V8C comprueba que: lo retirado estaba en esa sesion; su causa verificada es el dolor; ya no es
 * elegible; no esta en la sesion nueva; no se anade nada ni cambia ninguna otra dosis; los minutos no suben; y, si era
 * una colocacion estructural, su slot sigue cubierto en otra sesion de la semana. La validacion estricta (V1-V25, V20,
 * V24/4D) juzga despues la semana entera, como siempre.
 * <p>
 * NO SE PERSISTE como campo propio: no forma parte de {@link ConservationReport} ni del JSON de V32. La traza queda en
 * {@code correcciones_del_backend} (nota {@code RETIRADA_SEGURA}).
 *
 * @param sesionOrigen  la sesion de la semana medida de la que deriva
 * @param fecha         la fecha de la sesion nueva
 * @param quitado       el ejercicio retirado
 * @param patronQuitado su movement_pattern (para el slot)
 * @param motivo        su causa verificada (contiene {@code dolor_registrado})
 * @param dosisAnterior la dosis que tenia
 * @param minutosAntes  minutos declarados de la sesion antes de retirarlo
 * @param minutosDespues minutos de la sesion sin el, calculados por el estimador
 */
public record RetiradaSeguridad(Long sesionOrigen,
                                LocalDate fecha,
                                Long quitado,
                                String patronQuitado,
                                String motivo,
                                String dosisAnterior,
                                int minutosAntes,
                                int minutosDespues) {

    public RetiradaSeguridad {
        if (sesionOrigen == null || quitado == null)
            throw new IllegalArgumentException("Una retirada dice de que sesion y que ejercicio retira.");
        if (motivo == null || motivo.isBlank())
            throw new IllegalArgumentException("Una retirada por seguridad declara su motivo.");
    }
}
