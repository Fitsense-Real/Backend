package main.web.services.fitsense.planning.domain.exceptions;

import main.web.services.fitsense.planning.domain.model.valueobjects.MandatorySlot;
import main.web.services.fitsense.planning.domain.model.valueobjects.SlotCoverage;
import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;

import java.util.List;

/**
 * El conjunto elegible del participante no puede cubrir algun slot obligatorio
 * de 4C: con ese equipamiento, esa edad, ese nivel y esas restricciones no
 * existe ninguna semana que cumpla 4C §5.
 * <p>
 * Se lanza ANTES de llamar a la IA, no despues. Es el valor que el Step 4 aporta
 * y el backend no tenia: decir «no factible» sin gastar tres intentos de
 * generacion y sin entregar al participante un plan que incumple el criterio.
 * <p>
 * NO es un error tecnico. Es un resultado del estudio, y conviene que llegue al
 * participante y al investigador como lo que es: faltan ejercicios para un
 * patron, y la solucion esta en el equipamiento declarado o en el catalogo, no
 * en reintentar.
 */
public class InsufficientSlotCoverageException extends DomainRuleViolationException {

    private final Long userId;
    private final List<MandatorySlot> emptySlots;
    private final transient SlotCoverage coverage;

    public InsufficientSlotCoverageException(Long userId, SlotCoverage coverage) {
        super(("No se puede generar un plan que cumpla Step 4C para el usuario %d: "
                + "no hay ejercicios elegibles para %s. Cobertura medida: %s.")
                .formatted(userId,
                        coverage.emptySlots().stream().map(Enum::name).toList(),
                        coverage.describe()));
        this.userId = userId;
        this.emptySlots = List.copyOf(coverage.emptySlots());
        this.coverage = coverage;
    }

    public Long userId() {
        return userId;
    }

    /** Los slots sin un solo ejercicio elegible. Nunca vacio. */
    public List<MandatorySlot> emptySlots() {
        return emptySlots;
    }

    public SlotCoverage coverage() {
        return coverage;
    }
}
