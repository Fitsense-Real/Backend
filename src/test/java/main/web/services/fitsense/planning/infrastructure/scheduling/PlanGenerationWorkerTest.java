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
                .when(service).execute(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        var worker = new PlanGenerationWorker(service, 2);
        worker.tick();
        assertThat(enCurso.await(5, TimeUnit.SECONDS)).isTrue();
        // Con dos en curso no reclama una tercera: la tercera sigue PENDING para el siguiente ciclo.
        verify(service, times(2)).claimNext();
        verify(service).recoverStale();

        liberar.countDown();
        verify(service, timeout(5000)).execute(org.mockito.ArgumentMatchers.eq(a), org.mockito.ArgumentMatchers.any());
        verify(service, timeout(5000)).execute(org.mockito.ArgumentMatchers.eq(b), org.mockito.ArgumentMatchers.any());
        Thread.sleep(200);
        worker.tick();
        verify(service, timeout(5000)).execute(org.mockito.ArgumentMatchers.eq(c), org.mockito.ArgumentMatchers.any());
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
                .when(service).execute(org.mockito.ArgumentMatchers.eq(a), org.mockito.ArgumentMatchers.any());
        var worker = new PlanGenerationWorker(service, 2);
        worker.tick();
        assertThat(enCurso.await(5, TimeUnit.SECONDS)).isTrue();

        worker.heartbeat();
        verify(service).heartbeat(java.util.List.of(a));

        doAnswer(inv -> { terminada.countDown(); return 0; }).when(service).heartbeat(java.util.List.of());
        liberar.countDown();
        verify(service, timeout(5000)).execute(org.mockito.ArgumentMatchers.eq(a), org.mockito.ArgumentMatchers.any());
        Thread.sleep(200);
        worker.heartbeat();
        assertThat(terminada.await(5, TimeUnit.SECONDS)).as("ya no late por la que termino").isTrue();
        worker.stop();
    }

    @Test
    void alApagarseNoRegistraLaInterrupcionYDevuelveLaOperacionALaCola() throws Exception {
        var service = mock(PlanGenerationOperationService.class);
        var a = UUID.randomUUID();
        when(service.claimNext()).thenReturn(Optional.of(a), Optional.empty());
        var enCurso = new CountDownLatch(1);
        var vistoAlApagar = new java.util.concurrent.atomic.AtomicBoolean();
        doAnswer(inv -> {
            java.util.function.BooleanSupplier stopping = inv.getArgument(1);
            enCurso.countDown();
            try { Thread.sleep(10_000); } catch (InterruptedException e) { vistoAlApagar.set(stopping.getAsBoolean()); }
            return null;
        }).when(service).execute(org.mockito.ArgumentMatchers.eq(a), org.mockito.ArgumentMatchers.any());
        var worker = new PlanGenerationWorker(service, 1);
        worker.tick();
        assertThat(enCurso.await(5, TimeUnit.SECONDS)).isTrue();

        worker.stop();
        assertThat(vistoAlApagar).as("la ejecucion sabe que es el apagado").isTrue();
        verify(service).requeueOnShutdown(java.util.List.of(a));
        worker.tick();                                       // ya apagado: no reclama mas
        verify(service, times(1)).claimNext();
    }
}
