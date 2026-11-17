package main.web.services.fitsense.planning.application.generation;

import main.web.services.fitsense.catalog.domain.exceptions.EmptyEligibleSetException;
import main.web.services.fitsense.planning.application.internal.generation.GenerationFailureClassifier;
import main.web.services.fitsense.planning.domain.exceptions.PlanGenerationFailedException;
import main.web.services.fitsense.planning.domain.exceptions.ProfileOutsideGoalDurationException;
import main.web.services.fitsense.planning.domain.model.valueobjects.GenerationOperationStatus;
import main.web.services.fitsense.planning.domain.model.valueobjects.NoFactibleReason;
import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;
import main.web.services.fitsense.shared.domain.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("V58 · clasificacion del resultado de la generacion inicial")
class GenerationFailureClassifierTest {

    @Test
    void sinConjuntoElegibleEsNoFactibleYNoSeReintentaSolo() {
        var o = GenerationFailureClassifier.classify(new EmptyEligibleSetException(7L));
        assertThat(o.status()).isEqualTo(GenerationOperationStatus.NO_FACTIBLE);
        assertThat(o.reasonCode()).isEqualTo(NoFactibleReason.SIN_SEMANA_GENERABLE.name());
        assertThat(o.message()).isEqualTo(NoFactibleReason.SIN_SEMANA_GENERABLE.texto());
        assertThat(o.retryable()).isFalse();
    }

    @Test
    void niLaIaNiElMotorDeReglasLogranUnaSemanaValida() {
        var o = GenerationFailureClassifier.classify(new PlanGenerationFailedException(7L, "Ultimos problemas: x"));
        assertThat(o.status()).isEqualTo(GenerationOperationStatus.NO_FACTIBLE);
        assertThat(o.reasonCode()).isEqualTo(NoFactibleReason.SEMANA_NO_VALIDA.name());
        assertThat(o.message()).doesNotContain("Ultimos problemas", "x");
    }

    @Test
    void duracionFueraDelObjetivoEsNoFactible() {
        var o = GenerationFailureClassifier.classify(new ProfileOutsideGoalDurationException(7L, "GAIN_MUSCLE", 15));
        assertThat(o.status()).isEqualTo(GenerationOperationStatus.NO_FACTIBLE);
        assertThat(o.reasonCode()).isEqualTo(NoFactibleReason.DISPONIBILIDAD_NO_CUMPLIBLE.name());
    }

    @Test
    void reglasDeDominioYPerfilAusenteFallanSinReintento() {
        var regla = GenerationFailureClassifier.classify(new DomainRuleViolationException("El usuario 7 no esta activo"));
        assertThat(regla.status()).isEqualTo(GenerationOperationStatus.FAILED);
        assertThat(regla.retryable()).isFalse();
        assertThat(regla.message()).doesNotContain("usuario 7");
        var perfil = GenerationFailureClassifier.classify(new ResourceNotFoundException("Perfil del usuario", 7L));
        assertThat(perfil.reasonCode()).isEqualTo(GenerationFailureClassifier.PERFIL_INCOMPLETO);
        assertThat(perfil.retryable()).isFalse();
    }

    @Test
    void errorTecnicoEsReintentableYNoFiltraDetalles() {
        var o = GenerationFailureClassifier.classify(new IllegalStateException("Replicate devolvio 503 con token abc"));
        assertThat(o.status()).isEqualTo(GenerationOperationStatus.FAILED);
        assertThat(o.reasonCode()).isEqualTo(GenerationFailureClassifier.ERROR_TECNICO);
        assertThat(o.retryable()).isTrue();
        assertThat(o.message()).doesNotContain("Replicate", "503", "abc");
    }

    @Test
    void ejecucionInterrumpidaEsReintentable() {
        var o = GenerationFailureClassifier.interrupted();
        assertThat(o.status()).isEqualTo(GenerationOperationStatus.FAILED);
        assertThat(o.retryable()).isTrue();
    }
}
