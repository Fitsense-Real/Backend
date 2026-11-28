package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.MandatorySlot;

import java.util.Optional;

/**
 * Auditoria del catalogo v46 · P15 alternativa C (EXPERIMENTAL, desactivada por defecto; aprobada como diseno objetivo
 * condicionado a validacion deportiva). Separa dos preguntas que hasta ahora respondia el mismo
 * {@link MandatorySlot#of(CandidateExercise)}:
 * <ul>
 *   <li><b>Cobertura de un patron principal</b> (slots de 4C: V25, cobertura de capacidades, eleccion de colocaciones,
 *       slots pendientes de la semana, equivalencia de slot en sustituciones): con el experimento activado, un ejercicio
 *       con {@code is_isolation=true} NO cubre por si solo ningun patron principal ({@link #slot}).</li>
 *   <li><b>Contribucion a la dosis</b> (prescripcion por slot de {@code MvpDosePolicy}, progresion, series por grupo):
 *       sigue usando {@link MandatorySlot#of}; el aislado conserva la dosis que realmente aporta a su grupo.</li>
 * </ul>
 * Con el experimento desactivado {@link #slot} es exactamente {@link MandatorySlot#of}: ningun plan cambia.
 * Se activa con {@code fitsense.experimental.p15c.aislados-sin-cobertura=true} (ver {@code CoberturaDePatronConfig}).
 */
public final class CoberturaDePatron {

    private static volatile boolean aisladosSinCobertura = false;

    private CoberturaDePatron() {
    }

    /** El slot de 4C que el candidato CUBRE (no el que define su dosis). */
    public static Optional<MandatorySlot> slot(CandidateExercise c) {
        if (aisladosSinCobertura && c != null && Boolean.TRUE.equals(c.isolation())) return Optional.empty();
        return MandatorySlot.of(c);
    }

    public static boolean activa() {
        return aisladosSinCobertura;
    }

    /** Solo para la configuracion de arranque y las pruebas. */
    public static void configurar(boolean activar) {
        aisladosSinCobertura = activar;
    }
}
