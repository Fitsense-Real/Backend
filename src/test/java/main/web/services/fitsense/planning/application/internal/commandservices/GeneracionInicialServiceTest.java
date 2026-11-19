package main.web.services.fitsense.planning.application.internal.commandservices;

import main.web.services.fitsense.orchestration.infrastructure.locking.CicloSemanalBloqueo;
import main.web.services.fitsense.planning.domain.exceptions.DemasiadasGeneracionesException;
import main.web.services.fitsense.planning.domain.exceptions.GeneracionEnCursoException;
import main.web.services.fitsense.planning.domain.exceptions.GeneracionSinExclusionException;
import main.web.services.fitsense.planning.domain.exceptions.PlanAlreadyExistsException;
import main.web.services.fitsense.planning.domain.exceptions.PlanGenerationFailedException;
import main.web.services.fitsense.planning.domain.model.aggregates.WeeklyTrainingPlan;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.PreparedPlanGeneration;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.shared.interfaces.rest.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * R-5 · el primer plan con exclusion y limite, con dobles: lo que se comprueba es QUIEN llega a generar (y por tanto
 * a poder llamar a Replicate). La exclusion real entre hilos e instancias la prueba {@code GeneracionInicialConcurrenteIT}.
 */
@DisplayName("R-5 · generacion inicial: duplicados, exclusion, limite y errores, sin consumo evitable")
class GeneracionInicialServiceTest {

    private static final long U = 7L;
    private static final GenerateWeeklyPlanCommand CMD = GenerateWeeklyPlanCommand.firstPlan(U, LocalDate.of(2026, 11, 9));

    private WeeklyTrainingPlanCommandService comandos;
    private CicloSemanalBloqueo bloqueo;
    private GeneracionInicialService servicio;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void montar() {
        comandos = mock(WeeklyTrainingPlanCommandService.class);
        bloqueo = mock(CicloSemanalBloqueo.class);
        when(bloqueo.ejecutar(anyLong(), any())).thenAnswer(inv -> new CicloSemanalBloqueo.Resultado<>(
                CicloSemanalBloqueo.Estado.EJECUTADO, ((Supplier<Object>) inv.getArgument(1)).get()));
        when(comandos.prepare(any())).thenReturn(mock(PreparedPlanGeneration.class));
        when(comandos.persist(any(), any())).thenReturn(Optional.of(mock(WeeklyTrainingPlan.class)));
        servicio = new GeneracionInicialService(comandos, bloqueo, new LimiteDeGeneraciones(5, Clock.systemUTC()));
    }

    @Test
    @DisplayName("solicitud normal: prepara, genera una vez y persiste")
    void normal() {
        assertTrue(servicio.generar(CMD).isPresent());
        verify(comandos, times(1)).generate(any());
        verify(comandos, times(1)).persist(any(), any());
    }

    @Test
    @DisplayName("FALLABA ANTES · otra generacion en curso (otra peticion o el ciclo): 409 y no se genera ni se llama a la IA")
    void enCurso() {
        doReturn(new CicloSemanalBloqueo.Resultado<>(CicloSemanalBloqueo.Estado.EN_CURSO, null))
                .when(bloqueo).ejecutar(anyLong(), any());
        assertThrows(GeneracionEnCursoException.class, () -> servicio.generar(CMD));
        verifyNoInteractions(comandos);
    }

    @Test
    @DisplayName("sin bloqueo de exclusion (base no disponible): 503 y no se genera")
    void sinBloqueo() {
        doReturn(new CicloSemanalBloqueo.Resultado<>(CicloSemanalBloqueo.Estado.SIN_BLOQUEO, null))
                .when(bloqueo).ejecutar(anyLong(), any());
        assertThrows(GeneracionSinExclusionException.class, () -> servicio.generar(CMD));
        verifyNoInteractions(comandos);
    }

    @Test
    @DisplayName("ya hay plan: la peticion repetida se rechaza en prepare, sin generar y sin gastar cupo")
    void planYaExistente() {
        when(comandos.prepare(any())).thenThrow(new PlanAlreadyExistsException(U, LocalDate.of(2026, 11, 9)));
        for (int i = 0; i < 10; i++)
            assertThrows(PlanAlreadyExistsException.class, () -> servicio.generar(CMD));
        verify(comandos, never()).generate(any());
        doReturn(mock(PreparedPlanGeneration.class)).when(comandos).prepare(any());
        assertDoesNotThrow(() -> servicio.generar(CMD), "las rechazadas no consumieron el limite");
    }

    @Test
    @DisplayName("diez peticiones que fallan al generar: solo 5 llegan a generar; las demas, 429")
    void diezFallidas() {
        when(comandos.generate(any())).thenThrow(new PlanGenerationFailedException(U, "sin plan valido"));
        int llegan = 0, limitadas = 0;
        for (int i = 0; i < 10; i++) {
            try {
                servicio.generar(CMD);
            } catch (PlanGenerationFailedException e) {
                llegan++;
            } catch (DemasiadasGeneracionesException e) {
                limitadas++;
            }
        }
        assertEquals(5, llegan);
        assertEquals(5, limitadas);
        verify(comandos, times(5)).generate(any());
    }

    @Test
    @DisplayName("error durante la generacion: se propaga y la siguiente peticion se atiende (el bloqueo se suelta)")
    void errorDuranteLaGeneracion() {
        when(comandos.generate(any())).thenThrow(new IllegalStateException("fallo")).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> servicio.generar(CMD));
        assertTrue(servicio.generar(CMD).isPresent());
        verify(bloqueo, times(2)).ejecutar(eq(U), any());
    }

    @Test
    @DisplayName("codigos HTTP: 409 en curso, 503 sin exclusion, 429 con Retry-After")
    void codigosHttp() {
        var h = new GlobalExceptionHandler();
        assertEquals(HttpStatus.CONFLICT, h.handleGeneracionEnCurso(new GeneracionEnCursoException(U)).getStatusCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                h.handleGeneracionSinExclusion(new GeneracionSinExclusionException(U)).getStatusCode());
        var r = h.handleDemasiadasGeneraciones(new DemasiadasGeneracionesException(1200));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, r.getStatusCode());
        assertEquals("1200", r.getHeaders().getFirst("Retry-After"));
    }
}
