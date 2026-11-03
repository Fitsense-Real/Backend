package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.exceptions.InsufficientSlotCoverageException;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.SlotCoverage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * La comprobacion PREVIA de cobertura 4C: se ejecuta en {@code prepare()},
 * sobre el conjunto elegible ya filtrado, antes de llamar a cualquier generador.
 * <p>
 * Es una clase aparte y no un metodo privado del servicio de aplicacion por una
 * razon practica: asi se puede ejecutar sin Spring, sin base de datos y sin
 * Replicate, y la prueba del caso «slot vacio» prueba ESTE codigo y no una
 * reimplementacion suya.
 * <p>
 * Aplica el umbral de 4C v4 §6 y nada mas. No toca la dosis, no filtra el
 * conjunto elegible y no cambia que ejercicios se pueden proponer: solo valora
 * y, si el modo lo manda, se niega a seguir.
 */
public final class SlotCoverageGate {

    private static final Logger log = LoggerFactory.getLogger(SlotCoverageGate.class);

    private SlotCoverageGate() {}

    /**
     * @param modo que hacer con el veredicto. Solo los modos ENFORCE_* lanzan.
     * @return la cobertura medida, para registrarla o informarla. {@code null}
     *         si el modo es OFF, porque entonces no se ha medido nada y
     *         devolver un objeto de ceros seria mentir.
     * @throws InsufficientSlotCoverageException si el veredicto es NO FACTIBLE y
     *         el modo bloquea.
     */
    public static SlotCoverage evaluateBeforeGeneration(Long userId,
                                                        List<CandidateExercise> eligible,
                                                        SlotCoverageEnforcement modo) {
        if (modo == null || !modo.mide()) return null;

        var cobertura = SlotCoveragePolicy.ofEligible(eligible);

        log.info("Cobertura 4C previa del usuario {}: {} ({} elegibles, modo {})",
                userId, cobertura.describe(),
                eligible == null ? 0 : eligible.size(), modo);

        switch (cobertura.verdict()) {
            case NO_FACTIBLE -> {
                if (modo.bloquea())
                    throw new InsufficientSlotCoverageException(userId, cobertura);
                log.warn("Usuario {}: Step 4C NO FACTIBLE (sin ejercicios elegibles para {}) y "
                                + "se genera plan igualmente, porque el modo es {}.",
                        userId, cobertura.emptySlots(), modo);
            }
            // FRAGIL NO bloquea en ningun modo: 4C v4 §6 lo declara factible. Se
            // registra porque un slot con una sola opcion significa que el
            // participante repetira el mismo ejercicio toda la semana, y eso hay
            // que saberlo al leer su adherencia.
            case FRAGIL -> log.warn("Usuario {}: cobertura 4C FRAGIL. Slots con menos de {} "
                            + "opciones: {}. El plan es factible, pero el participante "
                            + "repetira ejercicio.",
                    userId, SlotCoverage.MINIMO_ROBUSTO, cobertura.fragileSlots());
            case ROBUSTO -> { /* nada que decir */ }
        }
        return cobertura;
    }
}
