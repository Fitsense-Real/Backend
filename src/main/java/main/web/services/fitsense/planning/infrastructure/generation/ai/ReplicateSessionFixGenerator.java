package main.web.services.fitsense.planning.infrastructure.generation.ai;

import main.web.services.fitsense.planning.domain.model.valueobjects.CorrectedSession;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.WeekDerivation;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pide a Replicate que CORRIJA sesiones concretas.
 * <p>
 * CLASE APARTE DE {@link ReplicateTrainingPlanGenerator}, Y NO UN METODO SUYO.
 * Dos razones. La primera es la de siempre: son dos preguntas distintas
 * -«disena una semana» y «corrige estas sesiones»- con su prompt, su esquema y
 * su ensamblador propios. La segunda es practica: anadir dos dependencias al
 * generador obligaria a cambiar sus dos constructores, y
 * {@code ReplicateTimingInstrumentationTest} lo construye con cinco argumentos
 * en su montaje. Separarlo no rompe nada.
 * <p>
 * REUTILIZA EL TRANSPORTE. La llamada HTTP, el sondeo de predicciones en curso
 * y la linea {@code REPLICATE_TIMING} son los mismos; lo unico que cambia es la
 * peticion que viaja y como se lee la respuesta. Duplicar el transporte habria
 * duplicado tambien el sondeo, que es la parte delicada.
 * <p>
 * MISMO PAQUETE A PROPOSITO. {@code call}, {@code Peticion} y
 * {@code SessionFixJsonSchema.format} tienen visibilidad de paquete; esta clase
 * vive al lado para usarlos sin ampliar la superficie publica de ninguno.
 * <p>
 * PETICION E INTERPRETACION VAN SEPARADAS, igual que
 * {@link ReplicateTrainingPlanGenerator#requestOutput} y
 * {@link ReplicateTrainingPlanGenerator#toDraft}. El motivo esta explicado en
 * {@link #pedirRespuesta}: no es simetria, es la unica forma de que el reintento
 * conserve la propuesta rechazada.
 */
@Component
public class ReplicateSessionFixGenerator {

    private final ReplicateTrainingPlanGenerator transporte;
    private final SessionFixPromptBuilder promptBuilder;
    private final SessionFixFromJsonAssembler assembler;

    public ReplicateSessionFixGenerator(ReplicateTrainingPlanGenerator transporte,
                                        SessionFixPromptBuilder promptBuilder,
                                        SessionFixFromJsonAssembler assembler) {
        this.transporte = transporte;
        this.promptBuilder = promptBuilder;
        this.assembler = assembler;
    }

    /**
     * La respuesta CRUDA de una peticion de correccion, sin interpretar.
     *
     * @param snapshotJson la proyeccion que viajo en el prompt. Hay que
     *                     persistirla TAL CUAL: recalcularla despues daria el
     *                     snapshot completo, no el que vio el modelo.
     * @param output       lo que entrego el modelo. El coordinador lo guarda
     *                     ANTES de interpretarlo, porque lo necesita para
     *                     {@code AiRetryFeedback.rejectedOutput} tambien -y
     *                     sobre todo- cuando la interpretacion falla.
     */
    public record RawFix(String snapshotJson, String output) {}

    public boolean isEnabled() {
        return transporte.isEnabled();
    }

    /**
     * El modelo que respondio. Lo necesita {@code AdaptedPublication}: una
     * semana con origen AI declara su modelo, y una CONSERVED tiene que
     * declararlo en null -su constructor lo rechaza si no-.
     */
    public String modelName() {
        return transporte.modelName();
    }

    /**
     * Pide las correcciones y devuelve la respuesta SIN interpretar. UNA SOLA
     * peticion, con todas las sesiones senaladas: es el contrato A aprobado.
     * <p>
     * VA SEPARADO DE {@link #interpretar} Y ESO NO ES ESTILO. Si una sola
     * llamada pidiera e interpretara, un JSON invalido se perderia: la
     * interpretacion lanza {@code InvalidPlanDraftException}, que lleva la lista
     * de problemas pero NO la respuesta cruda, asi que el metodo no llegaria
     * nunca a devolverla y el reintento se quedaria sin
     * {@code rejectedOutput}. El modelo recibiria «corrige esto» sin el esto.
     * Justo el caso que mas falta hace que sepa corregir.
     * <p>
     * Es el mismo motivo por el que la generacion inicial parte
     * {@code requestOutput} de {@code toDraft}, y por el que
     * {@code PlanGenerationPipeline} declara {@code String output} FUERA del
     * {@code try}.
     *
     * @param full     el snapshot de generacion inicial. Se PROYECTA dentro del
     *                 constructor del prompt; lo que viaja no es esto.
     * @param feedback el rechazo anterior, o {@code null} en el primer intento.
     * @param attempt  para atar la linea REPLICATE_TIMING a esta peticion.
     * @throws IllegalArgumentException si la orden no senala sesiones. No se
     *         comprueba aqui: lo hace {@code promptBuilder.build}, que ademas
     *         tolera una orden nula. Por eso se construye el prompt ANTES de
     *         leer las sesiones senaladas.
     */
    public RawFix pedirRespuesta(PlanGenerationContext context,
                                 WeekDerivation derivation,
                                 PlanInputSnapshot full,
                                 AiRetryFeedback feedback,
                                 int attempt) {

        // PRIMERO EL PROMPT, Y NO POR ORDEN DE LECTURA. build() ya rechaza una
        // orden nula o sin sesiones senaladas -comprueba adapta(), que es
        // exactamente !targetedSessions.isEmpty()-. Repetir aqui ese guardia
        // habria duplicado la regla y, peor, habria reventado con
        // NullPointerException en el caso que build() sabe nombrar.
        var prompt = promptBuilder.build(context, derivation, full, feedback);

        // El esquema se dimensiona con las senaladas: el modelo no puede
        // devolver mas sesiones de las que se le pidieron. Y la etiqueta de la
        // peticion dice que esto fue una adaptacion, no una semana nueva.
        var output = transporte.call(prompt.text(),
                ReplicateTrainingPlanGenerator.Peticion.deCorrecciones(senaladas(context).size()),
                context.userId(), context.weekNumber(), attempt);

        return new RawFix(prompt.snapshotJson(), output);
    }

    /**
     * Interpreta una respuesta ya recibida.
     * <p>
     * Comprueba los identificadores contra lo SOLICITADO: una sesion inventada
     * se reporta al modelo, con la lista de las que si eran, en vez de reventar
     * mas adelante en el empalme.
     *
     * @return lo que propuso el modelo. {@code toCorrections} nunca devuelve
     *         lista vacia por un fallo -si la respuesta no vale, lanza-, asi que
     *         una lista vacia significa «no propongo nada», que es una
     *         respuesta.
     * @throws main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException
     *         si la respuesta no cumple el contrato. Son fallos DEL MODELO, asi
     *         que viajan como problemas de validacion y llegan al reintento,
     *         que para entonces ya tiene el {@code output} guardado.
     */
    public List<CorrectedSession> interpretar(String output, PlanGenerationContext context) {
        return assembler.toCorrections(output, senaladas(context));
    }

    private static Set<Long> senaladas(PlanGenerationContext context) {
        return context.adjustment().targetedSessions().stream()
                .map(PlanAdjustment.TargetedSession::plannedWorkoutId)
                .collect(Collectors.toSet());
    }
}
