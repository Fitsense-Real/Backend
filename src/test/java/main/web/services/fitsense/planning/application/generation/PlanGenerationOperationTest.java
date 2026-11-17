package main.web.services.fitsense.planning.application.generation;

import main.web.services.fitsense.planning.domain.model.aggregates.PlanGenerationOperation;
import main.web.services.fitsense.planning.domain.model.valueobjects.GenerationOperationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("V58 · transiciones de la operacion de generacion")
class PlanGenerationOperationTest {

    private static final OffsetDateTime T0 = OffsetDateTime.parse("2026-11-15T10:00:00-05:00");

    @Test
    void nuevaOperacionEstaPendienteYSinFin() {
        var op = PlanGenerationOperation.pending(7L, LocalDate.of(2026, 11, 16), T0);
        assertThat(op.getStatus()).isEqualTo(GenerationOperationStatus.PENDING);
        assertThat(op.getId()).isNotNull();
        assertThat(op.getFinishedAt()).isNull();
        assertThat(op.getPlanId()).isNull();
    }

    @Test
    void completarExigeElPlanPublicado() {
        var op = PlanGenerationOperation.pending(7L, null, T0);
        assertThatThrownBy(() -> op.complete(null, T0)).isInstanceOf(NullPointerException.class);
        op.complete(42L, T0.plusMinutes(2));
        assertThat(op.getStatus()).isEqualTo(GenerationOperationStatus.COMPLETED);
        assertThat(op.getPlanId()).isEqualTo(42L);
        assertThat(op.getFinishedAt()).isEqualTo(T0.plusMinutes(2));
    }

    @Test
    void unEstadoTerminalNoCambia() {
        var op = PlanGenerationOperation.pending(7L, null, T0);
        op.fail(GenerationOperationStatus.NO_FACTIBLE, "SIN_SEMANA_GENERABLE", "texto", false, T0);
        assertThatThrownBy(() -> op.complete(42L, T0)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> op.fail(GenerationOperationStatus.FAILED, "X", "y", true, T0))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void soloFailedONoFactibleDescribenUnFallo() {
        var op = PlanGenerationOperation.pending(7L, null, T0);
        assertThatThrownBy(() -> op.fail(GenerationOperationStatus.COMPLETED, "X", "y", false, T0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void planYaExistenteQuedaCompletadoSinEjecutar() {
        var op = PlanGenerationOperation.alreadyCompleted(7L, LocalDate.of(2026, 11, 16), 9L, T0);
        assertThat(op.getStatus()).isEqualTo(GenerationOperationStatus.COMPLETED);
        assertThat(op.getPlanId()).isEqualTo(9L);
        assertThat(op.getExecutions()).isZero();
    }
}
