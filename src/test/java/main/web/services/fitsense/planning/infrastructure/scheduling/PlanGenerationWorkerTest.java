package main.web.services.fitsense.planning.infrastructure.scheduling;

import main.web.services.fitsense.planning.application.internal.generation.PlanGenerationOperationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("V58 · worker de generacion inicial")
class PlanGenerationWorkerTest {

    @Test
    void reclamaHastaElLimiteDeConcurrenciaYRecuperaHuerfanas() throws Exception {
        var service = mock(PlanGenerationOperationService.class);
        var a = UUID.randomUUID();
        var b = UUID.randomUUID();
        var c = UUID.randomUUID();
        when(service.claimNext()).thenReturn(Optional.of(a), Optional.of(b), Optional.of(c), Optional.empty());
        var liberar = new CountDownLatch(1);
        var enCurso = new CountDownLatch(2);
        doAnswer(inv -> { enCurso.countDown(); liberar.await(10, TimeUnit.SECONDS); return null; })
                .when(service).execute(org.mockito.ArgumentMatchers.any());

        var worker = new PlanGenerationWorker(service, 2);
        worker.tick();
        assertThat(enCurso.await(5, TimeUnit.SECONDS)).isTrue();
        // Con dos en curso no reclama una tercera: la tercera sigue PENDING para el siguiente ciclo.
        verify(service, times(2)).claimNext();
        verify(service).recoverStale();

        liberar.countDown();
        verify(service, timeout(5000)).execute(a);
        verify(service, timeout(5000)).execute(b);
        Thread.sleep(200);
        worker.tick();
        verify(service, timeout(5000)).execute(c);
        verify(service, atLeastOnce()).recoverStale();
        worker.stop();
    }

    @Test
    void unFalloDelCicloNoDetieneElWorker() {
        var service = mock(PlanGenerationOperationService.class);
        when(service.recoverStale()).thenThrow(new IllegalStateException("base caida"));
        var worker = new PlanGenerationWorker(service, 1);
        worker.tick(); // no lanza
        worker.tick();
        verify(service, times(2)).recoverStale();
        worker.stop();
    }

    @Test
    void lateSoloPorLasOperacionesQueEstaEjecutando() throws Exception {
        var service = mock(PlanGenerationOperationService.class);
        var a = UUID.randomUUID();
        when(service.claimNext()).thenReturn(Optional.of(a), Optional.empty());
        var liberar = new CountDownLatch(1);
        var enCurso = new CountDownLatch(1);
        var terminada = new CountDownLatch(1);
        doAnswer(inv -> { enCurso.countDown(); liberar.await(10, TimeUnit.SECONDS); return null; })
                .when(service).execute(a);
        var worker = new PlanGenerationWorker(service, 2);
        worker.tick();
        assertThat(enCurso.await(5, TimeUnit.SECONDS)).isTrue();

        worker.heartbeat();
        verify(service).heartbeat(java.util.List.of(a));

        doAnswer(inv -> { terminada.countDown(); return 0; }).when(service).heartbeat(java.util.List.of());
        liberar.countDown();
        verify(service, timeout(5000)).execute(a);
        Thread.sleep(200);
        worker.heartbeat();
        assertThat(terminada.await(5, TimeUnit.SECONDS)).as("ya no late por la que termino").isTrue();
        worker.stop();
    }
}
