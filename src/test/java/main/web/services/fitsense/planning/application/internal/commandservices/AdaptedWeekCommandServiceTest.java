package main.web.services.fitsense.planning.application.internal.commandservices;

import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.aggregates.WeeklyTrainingPlan;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptationAttempt;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptationDecision;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptedPublication;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptedWeekResult;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdjustmentType;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.ConservationReport;
import main.web.services.fitsense.planning.domain.model.valueobjects.CorrectedSession;
import main.web.services.fitsense.planning.domain.model.valueobjects.DerivedWorkout;
import main.web.services.fitsense.planning.domain.model.valueobjects.GenerationSource;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningProfile;
import main.web.services.fitsense.planning.domain.model.valueobjects.PreparedPlanGeneration;
import main.web.services.fitsense.planning.domain.model.valueobjects.PreviousWeekSummary;
import main.web.services.fitsense.planning.domain.model.valueobjects.SafetyProfile;
import main.web.services.fitsense.planning.domain.model.valueobjects.WeekDerivation;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.planning.domain.services.AdaptationEvidenceMapper;
import main.web.services.fitsense.planning.domain.services.AdaptedWeekProducer;
import main.web.services.fitsense.planning.domain.services.WeekDerivationBuilder;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.generation.ai.AiRetryFeedback;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.interfaces.acl.MeasuredWeekView;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * EL CICLO DE LA SEMANA ADAPTADA.
 * <p>
 * Las tres condiciones que quedaron fijadas al aprobar P2-B-3c-2, cada una con
 * su prueba:
 * <ol>
 *   <li>una orden no es una adaptacion aplicada: la evidencia dice lo que
 *       CAMBIO, no lo que se pidio;
 *   <li>los tres intentos son para obtener y validar una propuesta, NO para
 *       repetir escrituras;
 *   <li>con NO_CHANGE o INSUFFICIENT_DATA no se llama a Replicate.
 * </ol>
 * Mas la que sostiene el reintento entero: que la respuesta rechazada llegue al
 * intento siguiente.
 * <p>
 * Todo con dobles. Lo que se comprueba son las decisiones del orquestador; que
 * Postgres acepte la fila es P2-D.
 */
class AdaptedWeekCommandServiceTest {

    private static final long USUARIO = 7L;
    private static final LocalDate LUNES_MEDIDO = LocalDate.of(2026, 10, 12);
    private static final LocalDate LUNES_NUEVO = LocalDate.of(2026, 10, 19);
    private static final long SESION = 101L;

    private ReplicateSessionFixGenerator fixGenerator;
    private WeekDerivationBuilder derivationBuilder;
    private AdaptedWeekProducer producer;
    private WeeklyTrainingPlanCommandService planCommandService;
    private AdaptationEvidenceMapper evidenceMapper;
    private AdaptationFailureRecorder failureRecorder;
    private AdaptedWeekCommandService servicio;

    @BeforeEach
    void montar() {
        fixGenerator = mock(ReplicateSessionFixGenerator.class);
        derivationBuilder = mock(WeekDerivationBuilder.class);
        producer = mock(AdaptedWeekProducer.class);
        planCommandService = mock(WeeklyTrainingPlanCommandService.class);
        evidenceMapper = mock(AdaptationEvidenceMapper.class);
        failureRecorder = mock(AdaptationFailureRecorder.class);

        when(fixGenerator.isEnabled()).thenReturn(true);
        when(fixGenerator.modelName()).thenReturn("gpt-5-nano");
        when(derivationBuilder.derive(any(), anyList(), anyList())).thenReturn(derivacion());

        servicio = new AdaptedWeekCommandService(fixGenerator, derivationBuilder, producer,
                planCommandService, evidenceMapper, failureRecorder, new JsonSupport());
    }

    // =======================================================================
    // Condicion 3 · sin orden no se llama a la IA
    // =======================================================================

    @Test
    @DisplayName("Con NO_CHANGE no se le pide NADA a Replicate")
    void sinOrdenNoSeLlamaALaIa() {
        devuelveProductor(conservada());
        when(planCommandService.publishAdapted(any(), any()))
                .thenReturn(Optional.of(mock(WeeklyTrainingPlan.class)));

        servicio.adaptar(preparado(PlanAdjustment.none()), medida("NO_CHANGE"));

        verifyNoInteractions(fixGenerator);
    }

    @Test
    @DisplayName("Con INSUFFICIENT_DATA tampoco, y la semana se publica igual")
    void sinEvidenciaTampoco() {
        devuelveProductor(conservada());
        when(planCommandService.publishAdapted(any(), any()))
                .thenReturn(Optional.of(mock(WeeklyTrainingPlan.class)));

        var publicado = servicio.adaptar(preparado(PlanAdjustment.none()),
                medida("INSUFFICIENT_DATA"));

        verifyNoInteractions(fixGenerator);
        assertTrue(publicado.isPresent(),
                "la semana nueva necesita un plan aunque no se cambie nada de el");
    }

    // =======================================================================
    // El reintento · la respuesta rechazada viaja al intento siguiente
    // =======================================================================

    @Test
    @DisplayName("La respuesta rechazada llega al intento siguiente")
    void elOutputCrudoLlegaAlReintento() {
        // Es la razon de que el output se declare FUERA del try. La
        // interpretacion lanza InvalidPlanDraftException, que lleva los
        // problemas pero NO la respuesta cruda: sin esto, el segundo intento
        // recibiria «corrige esto» sin el esto.
        when(fixGenerator.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{\"proyectado\":true}",
                        "{\"sessions\":\"basura\"}"));
        when(fixGenerator.interpretar(any(), any()))
                .thenThrow(new InvalidPlanDraftException(List.of("FIX: sessions no es un arreglo")));

        devuelveProductor(conservada());
        when(planCommandService.publishAdapted(any(), any()))
                .thenReturn(Optional.of(mock(WeeklyTrainingPlan.class)));

        servicio.adaptar(preparado(ordenLocalizada()), medida("ADAPT_LOCALIZED"));

        var feedbacks = ArgumentCaptor.forClass(AiRetryFeedback.class);
        verify(fixGenerator, times(3))
                .pedirRespuesta(any(), any(), any(), feedbacks.capture(), anyInt());

        assertNull(feedbacks.getAllValues().get(0).rejectedOutput(),
                "el primer intento no tiene nada que corregir");
        assertEquals("{\"sessions\":\"basura\"}", feedbacks.getAllValues().get(1).rejectedOutput(),
                "el segundo intento tiene que ver su propia propuesta rechazada");
        assertTrue(feedbacks.getAllValues().get(1).rejectionProblems()
                        .contains("FIX: sessions no es un arreglo"),
                () -> feedbacks.getAllValues().get(1).rejectionProblems().toString());
    }

    @Test
    @DisplayName("Tres intentos y ni uno mas; despues, el productor decide sin propuesta")
    void tresIntentosComoMaximo() {
        when(fixGenerator.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{}", "{}"));
        when(fixGenerator.interpretar(any(), any()))
                .thenThrow(new InvalidPlanDraftException(List.of("FIX: respuesta vacia")));

        devuelveProductor(conservada());
        when(planCommandService.publishAdapted(any(), any()))
                .thenReturn(Optional.of(mock(WeeklyTrainingPlan.class)));

        servicio.adaptar(preparado(ordenLocalizada()), medida("ADAPT_LOCALIZED"));

        verify(fixGenerator, times(3)).pedirRespuesta(any(), any(), any(), any(), anyInt());

        // null, NO lista vacia: «no hubo respuesta usable» no es «no propongo
        // nada». De esa diferencia depende que el productor mire si conservar
        // es seguro.
        verify(producer).produce(any(), any(), eq(null), any());
    }

    @Test
    @DisplayName("Un JSON valido que NO pasa V8C consume un intento y realimenta al modelo")
    void elRechazoDelProductorRealimentaElReintento() {
        // EL FALLO QUE ESTA PRUEBA EXISTE PARA IMPEDIR. La respuesta puede ser
        // JSON impecable y proponer una semana que V8C o 4D rechazan. Si el
        // productor corriera DESPUES del bucle, ese rechazo gastaria la unica
        // oportunidad y quedarian dos sin usar.
        when(fixGenerator.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{\"p\":1}", "{\"primera\":true}"))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{\"p\":2}", "{\"segunda\":true}"));
        when(fixGenerator.interpretar(any(), any())).thenReturn(List.of(correccion()));

        // Primera propuesta: se entiende, y la semana que produce no vale.
        when(producer.produce(any(), any(), any(), any()))
                .thenReturn(AdaptedWeekResult.noFactible(
                        AdaptedWeekResult.Motivo.CORRECCION_IA, ConservationReport.vacio(),
                        List.of("V8C: la sesion 102 cambio un ejercicio que nadie senalo")))
                .thenReturn(corregida());
        when(planCommandService.publishAdapted(any(), any()))
                .thenReturn(Optional.of(mock(WeeklyTrainingPlan.class)));

        var publicado = servicio.adaptar(preparado(ordenLocalizada()), medida("ADAPT_LOCALIZED"));

        assertTrue(publicado.isPresent(), "el segundo intento si pasaba");

        var feedbacks = ArgumentCaptor.forClass(AiRetryFeedback.class);
        verify(fixGenerator, times(2))
                .pedirRespuesta(any(), any(), any(), feedbacks.capture(), anyInt());

        var segundo = feedbacks.getAllValues().get(1);
        assertEquals("{\"primera\":true}", segundo.rejectedOutput(),
                "el segundo intento tiene que ver la propuesta que se rechazo");
        assertTrue(segundo.rejectionProblems()
                        .contains("V8C: la sesion 102 cambio un ejercicio que nadie senalo"),
                () -> "los motivos del PRODUCTOR tienen que llegar al modelo: "
                        + segundo.rejectionProblems());
    }

    @Test
    @DisplayName("Y el intento que acaba publicando aporta SU snapshot, no el del rechazado")
    void elSnapshotEsElDelIntentoBueno() {
        when(fixGenerator.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{\"p\":1}", "{\"primera\":true}"))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{\"p\":2}", "{\"segunda\":true}"));
        when(fixGenerator.interpretar(any(), any())).thenReturn(List.of(correccion()));
        when(producer.produce(any(), any(), any(), any()))
                .thenReturn(AdaptedWeekResult.noFactible(
                        AdaptedWeekResult.Motivo.CORRECCION_IA, ConservationReport.vacio(),
                        List.of("4D: siete colocaciones en un dia")))
                .thenReturn(corregida());
        when(planCommandService.publishAdapted(any(), any()))
                .thenReturn(Optional.of(mock(WeeklyTrainingPlan.class)));

        servicio.adaptar(preparado(ordenLocalizada()), medida("ADAPT_LOCALIZED"));

        var publicacion = ArgumentCaptor.forClass(AdaptedPublication.class);
        verify(planCommandService).publishAdapted(any(), publicacion.capture());

        assertEquals("{\"p\":2}", publicacion.getValue().inputSnapshotJson(),
                "se persiste la proyeccion que vio el modelo en el intento que valio");
        assertEquals((short) 2, publicacion.getValue().aiCalls(),
                "dos llamadas: la rechazada tambien se pago");
    }

    @Test
    @DisplayName("Tres rechazos del productor: se deja decidir SIN propuesta, y es la cuarta llamada")
    void tresRechazosDelProductorAgotanElBucle() {
        when(fixGenerator.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{}", "{}"));
        when(fixGenerator.interpretar(any(), any())).thenReturn(List.of(correccion()));

        var rechazo = AdaptedWeekResult.noFactible(
                AdaptedWeekResult.Motivo.CORRECCION_IA, ConservationReport.vacio(),
                List.of("V8C: no cambio la sesion senalada"));
        // Los tres intentos rechazados y, despues, el productor decidiendo sin
        // propuesta: ahi es donde comprueba si conservar es SEGURO.
        when(producer.produce(any(), any(), any(), any()))
                .thenReturn(rechazo).thenReturn(rechazo).thenReturn(rechazo)
                .thenReturn(conservada());
        when(planCommandService.publishAdapted(any(), any()))
                .thenReturn(Optional.of(mock(WeeklyTrainingPlan.class)));

        servicio.adaptar(preparado(ordenLocalizada()), medida("ADAPT_LOCALIZED"));

        verify(fixGenerator, times(3)).pedirRespuesta(any(), any(), any(), any(), anyInt());
        verify(producer, times(4)).produce(any(), any(), any(), any());
        // null, NO lista vacia: de esa diferencia depende que se compruebe la
        // seguridad antes de conservar.
        verify(producer).produce(any(), any(), eq(null), any());

        var publicacion = ArgumentCaptor.forClass(AdaptedPublication.class);
        verify(planCommandService).publishAdapted(any(), publicacion.capture());
        assertEquals((short) 3, publicacion.getValue().aiCalls(),
                "tres llamadas gastadas, aunque la semana acabe conservada");
        assertNull(publicacion.getValue().modelName(),
                "la conservo el respaldo, no un modelo");
    }

    @Test
    @DisplayName("Si conservar NO es seguro tras agotar los intentos, no se publica nada")
    void siConservarNoEsSeguroNoSePublica() {
        when(fixGenerator.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{}", "{}"));
        when(fixGenerator.interpretar(any(), any())).thenReturn(List.of(correccion()));

        var rechazo = AdaptedWeekResult.noFactible(
                AdaptedWeekResult.Motivo.CORRECCION_IA, ConservationReport.vacio(),
                List.of("V8C: no cambio la sesion senalada"));
        when(producer.produce(any(), any(), any(), any()))
                .thenReturn(rechazo).thenReturn(rechazo).thenReturn(rechazo)
                .thenReturn(AdaptedWeekResult.noFactible(
                        AdaptedWeekResult.Motivo.RESPALDO_IA, ConservationReport.vacio(),
                        List.of("Conservar no es seguro: la sesion 101 causo dolor")));
        when(evidenceMapper.map(any(), any(), any(), any()))
                .thenReturn(mock(AdaptationAttempt.class));

        var publicado = servicio.adaptar(preparado(ordenLocalizada()), medida("ADAPT_LOCALIZED"));

        assertTrue(publicado.isEmpty(),
                "republicar la sesion que dolio seria peor que no publicar");
        verify(planCommandService, never()).publishAdapted(any(), any());
        verify(failureRecorder).registrar(any());
    }

    // =======================================================================
    // Condicion 2 · los intentos no son para reintentar escrituras
    // =======================================================================

    @Test
    @DisplayName("Un fallo al PUBLICAR no se reintenta como si fuera de la IA")
    void unFalloDePublicacionNoSeReintenta() {
        when(fixGenerator.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{\"p\":1}", "{\"s\":[]}"));
        when(fixGenerator.interpretar(any(), any())).thenReturn(List.of(correccion()));
        devuelveProductor(corregida());
        when(planCommandService.publishAdapted(any(), any()))
                .thenThrow(new RuntimeException("la base dijo que no"));

        assertThrows(RuntimeException.class,
                () -> servicio.adaptar(preparado(ordenLocalizada()), medida("ADAPT_LOCALIZED")));

        verify(fixGenerator, times(1)).pedirRespuesta(any(), any(), any(), any(), anyInt());
        verify(planCommandService, times(1)).publishAdapted(any(), any());
    }

    // =======================================================================
    // El fallo se registra fuera de la publicacion
    // =======================================================================

    @Test
    @DisplayName("Un NO_FACTIBLE no publica y SI deja evidencia, por el registrador")
    void noFactibleRegistraFuera() {
        when(fixGenerator.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{}", "{}"));
        when(fixGenerator.interpretar(any(), any())).thenReturn(List.of(correccion()));

        devuelveProductor(AdaptedWeekResult.noFactible(
                AdaptedWeekResult.Motivo.CORRECCION_IA, ConservationReport.vacio(),
                List.of("V8C: la sesion 101 cambio un ejercicio que nadie senalo")));

        var intento = mock(AdaptationAttempt.class);
        when(evidenceMapper.map(any(), any(), any(), any())).thenReturn(intento);
        when(failureRecorder.registrar(any())).thenReturn(true);

        var publicado = servicio.adaptar(preparado(ordenLocalizada()), medida("ADAPT_LOCALIZED"));

        assertTrue(publicado.isEmpty());
        verify(planCommandService, never()).publishAdapted(any(), any());
        verify(failureRecorder).registrar(intento);
    }

    @Test
    @DisplayName("Y el fallo registra las llamadas que SI se gastaron")
    void elFalloRegistraSusLlamadas() {
        // ai_calls es evidencia de la tesis: tres intentos gastados que acaban
        // en nada siguen siendo tres llamadas al proveedor.
        when(fixGenerator.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{}", "{}"));
        when(fixGenerator.interpretar(any(), any()))
                .thenThrow(new InvalidPlanDraftException(List.of("FIX: no vale")));

        devuelveProductor(AdaptedWeekResult.noFactible(
                AdaptedWeekResult.Motivo.RESPALDO_IA, ConservationReport.vacio(),
                List.of("Conservar no es seguro: la sesion 101 causo dolor")));
        when(evidenceMapper.map(any(), any(), any(), any()))
                .thenReturn(mock(AdaptationAttempt.class));

        servicio.adaptar(preparado(ordenLocalizada()), medida("ADAPT_LOCALIZED"));

        var hechos = ArgumentCaptor.forClass(AdaptationEvidenceMapper.PublicationFacts.class);
        verify(evidenceMapper).map(any(), any(), any(), hechos.capture());

        assertEquals((short) 3, hechos.getValue().aiCalls(),
                "se gastaron tres intentos y la evidencia tiene que decirlo");
        assertNull(hechos.getValue().resultingPlanId(), "no se publico ningun plan");
    }

    // =======================================================================
    // Condicion 1 · la evidencia dice lo aplicado, no lo pedido
    // =======================================================================

    @Test
    @DisplayName("Se ordeno adaptar y la IA no cambio nada: se publica CONSERVED, sin modelo")
    void laOrdenNoEsUnaAdaptacionAplicada() {
        // ADAPT_LOCALIZED dice que se PIDIO. Si el informe de conservacion no
        // registra ninguna sesion aplicada, el productor devuelve SIN_EFECTO y
        // origen CONSERVED. La publicacion tiene que reflejar eso: ningun
        // modelo generó esa semana, aunque se le preguntara a uno.
        when(fixGenerator.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{\"p\":1}", "{\"s\":[]}"));
        when(fixGenerator.interpretar(any(), any())).thenReturn(List.of(correccion()));

        devuelveProductor(new AdaptedWeekResult(AdaptedWeekResult.Estado.PUBLICABLE,
                AdaptedWeekResult.Motivo.SIN_EFECTO, semana(), GenerationSource.CONSERVED,
                ConservationReport.vacio(), List.of()));
        when(planCommandService.publishAdapted(any(), any()))
                .thenReturn(Optional.of(mock(WeeklyTrainingPlan.class)));

        servicio.adaptar(preparado(ordenLocalizada()), medida("ADAPT_LOCALIZED"));

        var publicacion = ArgumentCaptor.forClass(AdaptedPublication.class);
        verify(planCommandService).publishAdapted(any(), publicacion.capture());

        assertEquals(AdaptationDecision.ADAPT_LOCALIZED, publicacion.getValue().decision(),
                "la decision registra lo que se PIDIO");
        assertNull(publicacion.getValue().modelName(),
                "una semana CONSERVED no la genero ningun modelo, aunque se preguntara");
        assertEquals((short) 1, publicacion.getValue().aiCalls(),
                "la llamada se hizo y cuenta, aunque no cambiara nada");
    }

    @Test
    @DisplayName("Cuando SI cambio algo, la publicacion declara el modelo")
    void cuandoCorrigeSeDeclaraElModelo() {
        when(fixGenerator.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{\"p\":1}", "{\"s\":[]}"));
        when(fixGenerator.interpretar(any(), any())).thenReturn(List.of(correccion()));
        devuelveProductor(corregida());
        when(planCommandService.publishAdapted(any(), any()))
                .thenReturn(Optional.of(mock(WeeklyTrainingPlan.class)));

        servicio.adaptar(preparado(ordenLocalizada()), medida("ADAPT_LOCALIZED"));

        var publicacion = ArgumentCaptor.forClass(AdaptedPublication.class);
        verify(planCommandService).publishAdapted(any(), publicacion.capture());

        assertEquals("gpt-5-nano", publicacion.getValue().modelName());
        assertEquals("{\"p\":1}", publicacion.getValue().inputSnapshotJson(),
                "se persiste la proyeccion que VIAJO, no el snapshot completo");
    }

    // =======================================================================
    // La derivacion
    // =======================================================================

    @Test
    @DisplayName("Se copian TODAS las sesiones anteriores, una semana mas tarde")
    void laDerivacionDesplazaSieteDias() {
        devuelveProductor(conservada());
        when(planCommandService.publishAdapted(any(), any()))
                .thenReturn(Optional.of(mock(WeeklyTrainingPlan.class)));

        servicio.adaptar(preparado(PlanAdjustment.none()), medida("NO_CHANGE"));

        var origenes = ArgumentCaptor.forClass(List.class);
        var fechas = ArgumentCaptor.forClass(List.class);
        verify(derivationBuilder).derive(any(), origenes.capture(), fechas.capture());

        assertEquals(List.of(SESION, 102L), origenes.getValue(),
                "se conservan todas: la adaptacion corrige sesiones, no las elimina");
        assertEquals(List.of(LUNES_MEDIDO.plusWeeks(1), LUNES_MEDIDO.plusDays(2).plusWeeks(1)),
                fechas.getValue(),
                "se desplazan siete dias para mantener el patron de dias de la persona");
    }

    @Test
    @DisplayName("Sin semana anterior no se adapta: eso es generacion inicial")
    void sinSemanaAnterior() {
        var contexto = new PlanGenerationContext(USUARIO, (short) 1, LUNES_NUEVO,
                LUNES_NUEVO.plusDays(6), perfil(), PlanAdjustment.none(), null,
                List.of(candidato()), SafetyProfile.none(), (PrescriptionParams) null);

        var prepared = new PreparedPlanGeneration(
                new GenerateWeeklyPlanCommand(USUARIO, LUNES_NUEVO, PlanAdjustment.none(), true,
                        null, null),
                contexto, PlanAdjustment.none(), LUNES_NUEVO, LUNES_NUEVO.plusDays(6),
                30, (short) 1, null, ZoneId.of("America/Lima"));

        var error = assertThrows(IllegalStateException.class,
                () -> servicio.adaptar(prepared, medida("NO_CHANGE")));

        assertTrue(error.getMessage().contains("generacion inicial"), error::getMessage);
        verifyNoInteractions(producer);
    }

    // =======================================================================
    // Material
    // =======================================================================

    private void devuelveProductor(AdaptedWeekResult resultado) {
        when(producer.produce(any(), any(), any(), any())).thenReturn(resultado);
    }

    private static MeasuredWeekView medida(String decision) {
        return new MeasuredWeekView(300L, 400L,
                new BigDecimal("42.00"), new BigDecimal("100.00"), decision);
    }

    private static PlanAdjustment ordenLocalizada() {
        return new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null,
                0.0, 0.0, null, null, null, "baja adherencia", null, null,
                List.of(new PlanAdjustment.TargetedSession(SESION,
                        TipoIntervencion.ACORTAR_O_REORGANIZAR)));
    }

    private static PreparedPlanGeneration preparado(PlanAdjustment ajuste) {
        var contexto = new PlanGenerationContext(USUARIO, (short) 2, LUNES_NUEVO,
                LUNES_NUEVO.plusDays(6), perfil(), ajuste, semanaAnterior(),
                List.of(candidato()), SafetyProfile.none(), (PrescriptionParams) null);

        return new PreparedPlanGeneration(
                new GenerateWeeklyPlanCommand(USUARIO, LUNES_NUEVO, ajuste, true,
                        new BigDecimal("42.00"), null),
                contexto, ajuste, LUNES_NUEVO, LUNES_NUEVO.plusDays(6),
                30, (short) 2, null, ZoneId.of("America/Lima"));
    }

    /** Dos sesiones, lunes y miercoles de la semana medida. */
    private static PreviousWeekSummary semanaAnterior() {
        return new PreviousWeekSummary(new BigDecimal("42.00"), null, 120, java.util.Map.of(),
                List.of(sesionAnterior(SESION, LUNES_MEDIDO),
                        sesionAnterior(102L, LUNES_MEDIDO.plusDays(2))),
                List.of(), List.of());
    }

    private static PreviousWeekSummary.WorkoutOutcome sesionAnterior(long id, LocalDate dia) {
        return new PreviousWeekSummary.WorkoutOutcome(id, dia, "FULL_BODY", "EXPIRED",
                "LACK_OF_TIME", true, "SKIPPED", null, BigDecimal.ZERO,
                "FULL_BODY - Sesion", (short) 45);
    }

    private static AdaptedWeekResult corregida() {
        return new AdaptedWeekResult(AdaptedWeekResult.Estado.PUBLICABLE,
                AdaptedWeekResult.Motivo.CORRECCION_IA, semana(), GenerationSource.AI,
                new ConservationReport(List.of(), List.of(SESION), List.of(), List.of(),
                        java.util.Set.of()),
                List.of());
    }

    private static AdaptedWeekResult conservada() {
        return new AdaptedWeekResult(AdaptedWeekResult.Estado.PUBLICABLE,
                AdaptedWeekResult.Motivo.SIN_ORDEN, semana(), GenerationSource.CONSERVED,
                ConservationReport.vacio(), List.of());
    }

    private static WeekDerivation derivacion() {
        return new WeekDerivation(semana(), List.of());
    }

    private static List<DerivedWorkout> semana() {
        return List.of(new DerivedWorkout(SESION, new PlanDraft.DraftWorkout(
                LUNES_NUEVO, WorkoutFocus.FULL_BODY, "FULL_BODY - Sesion 1", 45,
                List.of(new PlanDraft.DraftExercise(1L, PrescriptionType.SETS_REPS,
                        (short) 3, (short) 10, null, null, (short) 90, null)))));
    }

    private static CorrectedSession correccion() {
        // exercises va en lista VACIA y no en null: null es «prescripcion
        // incompleta» y el constructor lo rechaza. La fecha y la duracion si
        // son opcionales y caen a las de la copia.
        return new CorrectedSession(SESION, null, null, List.of(),
                "Se acorta la sesion que no dio tiempo a terminar.");
    }

    private static CandidateExercise candidato() {
        return new CandidateExercise(1L, "sentadilla", "UPPER LEGS", "body weight", 1,
                PrescriptionType.SETS_REPS, null, "KNEE_DOMINANT");
    }

    private static PlanningProfile perfil() {
        return new PlanningProfile(USUARIO, 30, "MALE", new BigDecimal("175.0"),
                new BigDecimal("72.0"), new BigDecimal("75.0"), "BEGINNER",
                "GAIN_MUSCLE", "Ganar masa muscular en casa", "Ninguna",
                "HOME", 3, List.of((short) 1, (short) 3, (short) 5), 45,
                List.of("body weight"), List.of(), SafetyProfile.Declaradas.sinLimitaciones());
    }
}
