package main.web.services.fitsense.planning.infrastructure.generation.ai;

import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.PreviousWeekSummary;
import main.web.services.fitsense.planning.domain.model.valueobjects.WeekDerivation;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Arma el prompt de ADAPTACION LOCALIZADA: el contrato A.
 * <p>
 * SEPARADO DE {@link PlanPromptBuilder} A PROPOSITO, y sin tocarlo. Son dos
 * preguntas distintas -«diseña una semana» y «corrige estas sesiones»- y
 * mezclarlas obligaria a un solo texto a decir las dos cosas, con el riesgo de
 * que el modelo haga la que no toca.
 * <p>
 * REUTILIZA EL CATALOGO Y EL PERFIL del snapshot de generacion inicial, pero
 * PROYECTADOS por {@link SessionFixInputSnapshot}. No se duplica esa
 * maquinaria -{@code min_reps}, {@code movement_pattern} y {@code clase_dosis}
 * se calculan en un solo sitio- y a la vez el prompt no puede recibir el
 * historial de la semana completa.
 * <p>
 * LO QUE NO SE LE MANDA: el resto del plan. Las sesiones conservadas no viajan,
 * por dos razones. Si las ve, tiende a tocarlas, y lo que toque de mas lo
 * rechazara V8C gastando un reintento. Y gastariamos contexto en algo que se va
 * a descartar: el empalme toma las conservadas de la copia, no de la respuesta.
 * <p>
 * POR ESO ESTA CLASE PROYECTA Y SERIALIZA ELLA MISMA, y recibe el snapshot
 * TIPADO en vez de una cadena JSON. Con una cadena, el requisito dependia de
 * que quien llamara se acordara de recortarla; la v1 recibia
 * {@code inputSnapshotJson} y le pasaba el snapshot ENTERO, con
 * {@code previous_week.workouts} dentro. Ahora no hay forma de pasarselo: no
 * existe el parametro. Es el mismo criterio del esquema de P2-A1, donde
 * {@code focus_code} no viaja porque no esta en el contrato.
 * <p>
 * Y devuelve el prompt JUNTO CON el JSON proyectado, en un solo objeto, para
 * que lo enviado y lo que se persiste como evidencia sean la misma cadena por
 * construccion y no dos serializaciones que puedan separarse.
 */
@Component
public class SessionFixPromptBuilder {

    /**
     * Version del texto de reglas de adaptacion. Viaja en el snapshot junto a la
     * del prompt de generacion, que es otra y se mueve por su cuenta.
     * <p>
     * FIX-PR-1.0: primera version. Cambiar cualquier texto de este archivo exige
     * subirla, igual que en {@link PlanPromptBuilder}: la version registra lo
     * que recibio el modelo.
     */
    public static final String VERSION = "FIX-PR-1.1";

    private final JsonSupport json;

    public SessionFixPromptBuilder(JsonSupport json) {
        this.json = json;
    }

    /**
     * El prompt y los datos exactos que lo acompanaron.
     *
     * @param text         lo que se le manda al modelo.
     * @param snapshotJson la proyeccion serializada, ya incrustada en
     *                     {@code text}. Es ESTA cadena la que hay que persistir
     *                     como evidencia: serializar otra vez por separado
     *                     podria registrar algo distinto de lo enviado.
     */
    public record SessionFixPrompt(String text, String snapshotJson) {}

    /**
     * @param context    el mismo contexto de generacion. De aqui salen la orden
     *                   -que sesiones se senalaron y que se permite en cada
     *                   una- y la semana medida.
     * @param derivation la copia de la semana anterior con las fechas nuevas.
     *                   De aqui sale la fecha que cada sesion senalada tiene
     *                   AHORA, que es sobre la que el modelo decide.
     * @param full       el snapshot de generacion inicial, TIPADO. Se proyecta
     *                   aqui dentro: lo que viaja es la proyeccion, nunca esto.
     * @param feedback   el rechazo anterior, si lo hubo.
     */
    public SessionFixPrompt build(PlanGenerationContext context,
                                  WeekDerivation derivation,
                                  PlanInputSnapshot full,
                                  AiRetryFeedback feedback) {

        var adjustment = context.adjustment();
        if (adjustment == null || !adjustment.adapta())
            throw new IllegalArgumentException(
                    "No se puede pedir una correccion sin sesiones senaladas.");

        // Una senalada sin derivada no tiene fecha que ofrecerle al modelo. Si
        // se deja pasar, el bloque sale sin la linea de la fecha -en silencio-,
        // el modelo decide a ciegas y V8C lo rechaza despues: una llamada
        // entera gastada por un dato que ya faltaba antes de pedirla.
        var sinDerivada = adjustment.targetedSessions().stream()
                .map(PlanAdjustment.TargetedSession::plannedWorkoutId)
                .filter(id -> derivation.from(id).isEmpty())
                .toList();
        if (!sinDerivada.isEmpty())
            throw new IllegalArgumentException(
                    "Las sesiones senaladas " + sinDerivada + " no tienen copia derivada:"
                            + " no se puede pedir que las corrija sin decirle que fecha tienen.");

        var prompt = new StringBuilder();

        prompt.append("""
                Eres un entrenador que CORRIGE sesiones concretas de un plan semanal
                que ya existe. No disenas una semana nueva.
                Devuelve UNICAMENTE el objeto JSON del esquema, sin texto adicional.

                La persona siguio mal algunas sesiones de su semana anterior. El resto
                de la semana se conserva tal cual y NO te lo mandamos: tu trabajo es
                proponer una version mejor SOLO de las sesiones senaladas mas abajo.

                Reglas que el backend verifica y que invalidan tu propuesta si no se cumplen:
                1.  Devuelve una entrada por cada sesion que corrijas, identificada por
                    su source_planned_workout_id EXACTO, tal como aparece abajo.
                    No lo inventes, no lo cambies y no corrijas ninguna sesion que no
                    este en esa lista.
                2.  Si para una sesion senalada no encuentras una correccion mejor que
                    lo que ya habia, OMITELA del arreglo. No la devuelvas igual.
                    Conservarla es una respuesta legitima; devolverla identica no
                    aporta nada y cuenta lo mismo.
                3.  exercises es la prescripcion COMPLETA de la sesion corregida, no un
                    parche sobre la anterior: lo que no pongas, no estara en la sesion.
                4.  Usa solo exercise_id presentes en available_exercises. No inventes
                    ninguno y no superes max_difficulty_level.
                5.  Usa el prescription_type que trae cada ejercicio en
                    available_exercises. SETS_REPS exige planned_sets y planned_reps;
                    DURATION exige planned_duration_seconds.
                6.  Minimo 2 ejercicios por sesion, 2 series por ejercicio SETS_REPS y
                    20 segundos en los de duracion.
                7.  planned_reps nunca baja del min_reps de ese ejercicio ni supera
                    constraints.max_reps. min_reps ya tiene en cuenta la zona y el
                    nivel: usalo tal cual.
                8.  QUE PUEDES CAMBIAR EN CADA SESION LO DECIDE SU MOTIVO, y esta
                    escrito abajo sesion por sesion. El motivo ORIENTA pero no ordena:
                    una sesion que se omitio por falta de tiempo no demuestra que sus
                    ejercicios sean malos.
                9.  Si sustituyes un ejercicio, el reemplazo debe ser del MISMO
                    movement_pattern que el que quitas. Cada ejercicio de
                    available_exercises trae el suyo.
                    La unica excepcion es el dolor: ahi el ejercicio que dolio NO
                    puede volver, y el reemplazo no tiene que compartir patron.
                10. No cambies el enfoque ni el nombre de la sesion: no viajan en el
                    esquema porque la sesion sigue siendo la misma sesion.
                11. expected_duration_minutes tiene que corresponder al contenido que
                    propones, y no superar session_minutes mas 15 %. Declarar menos
                    minutos sin quitar trabajo no es acortar: el backend recalcula la
                    duracion a partir de los ejercicios y rechaza el desvio.
                12. scheduled_date solo cambia si el motivo de esa sesion lo autoriza.
                    Formato AAAA-MM-DD, dia de la semana dentro de available_days y
                    dentro del rango de la semana nueva.
                13. target_load_kg sigue el principio 8: sin peso anotado la semana
                    anterior va en null, y nunca sube mas de un 10 % sobre el usado.
                14. Si safety_notes no es null, respetalo. available_exercises YA
                    excluye lo prohibido, pero de los que quedan elige las variantes
                    mas conservadoras.

                change_reason y rationale se le muestran al usuario: escribelos en
                espanol, en segunda persona, breves y concretos. Di que cambiaste y por
                que, sin prometer resultados.
                """);

        prompt.append('\n').append(
                PrescriptionPrinciples.textFor(context.profile().goalType()));

        // Los principios son los MISMOS de generacion -no hay un segundo texto
        // que mantener- pero dos de ellos mandan mirar previous_week, y la
        // proyeccion de adaptacion no lo lleva: el principio 8 busca ahi el
        // actual_load_kg y el 9 el desempeno de la semana. Sin esta nota el
        // prompt se contradice a si mismo y el modelo decide sobre un bloque
        // que no existe. Lo que esos principios piden esta en el bloque de
        // sesiones senaladas, por sesion.
        prompt.append("""

                NOTA SOBRE LOS PRINCIPIOS: en una correccion NO recibes
                previous_week. Todo lo que los principios te mandan buscar ahi
                -el peso que se uso y como fue cada sesion- esta en SESIONES
                SENALADAS, sesion por sesion, y solo de las que puedes corregir.
                Si un dato no aparece ahi, tratalo como ausente: no lo supongas.
                """);

        prompt.append(sesionesSenaladas(context, derivation, adjustment));

        var snapshotJson = json.write(SessionFixInputSnapshot.of(full));
        prompt.append("\nDatos de entrada:\n").append(snapshotJson).append('\n');

        // El reintento va al final: es lo ultimo que lee y lo unico que
        // distingue este intento del anterior. Mismo criterio que en
        // PlanPromptBuilder.
        if (feedback != null && !feedback.isEmpty()) appendRetry(prompt, feedback);

        return new SessionFixPrompt(prompt.toString(), snapshotJson);
    }

    /**
     * El bloque que hace que esto sea adaptacion localizada y no generacion.
     * <p>
     * Por cada sesion senalada: su identificador, la fecha que tiene ahora, lo
     * que se le prescribio, como le fue, y en una linea que se le permite
     * cambiar. Esa linea sale de {@code TipoIntervencion.instruccion()}, de modo
     * que lo que se le PIDE al modelo y lo que V8C le PERMITE salen del mismo
     * sitio y no pueden separarse.
     */
    private String sesionesSenaladas(PlanGenerationContext context,
                                     WeekDerivation derivation,
                                     PlanAdjustment adjustment) {

        var previous = context.previousWeek();
        var porSesion = prescripcionesPorSesion(previous);
        var resultados = resultadosPorSesion(previous);

        var bloque = new StringBuilder("\nSESIONES SENALADAS (");
        bloque.append(adjustment.targetedSessions().size()).append("):\n");

        for (var senalada : adjustment.targetedSessions()) {
            Long id = senalada.plannedWorkoutId();
            var intervencion = senalada.intervencion();
            var resultado = resultados.get(id);

            bloque.append("\n- source_planned_workout_id: ").append(id).append('\n');

            if (resultado != null) {
                bloque.append("  era el ").append(resultado.scheduledDate())
                        .append(" (").append(resultado.focusCode()).append(")\n");
                bloque.append("  como le fue: ");
                if (!resultado.recorded())
                    bloque.append("sin registro\n");
                else {
                    bloque.append(resultado.completionPct() == null ? "sin dato"
                            : resultado.completionPct() + " % completada");
                    if (resultado.skipReason() != null)
                        bloque.append(", motivo registrado: ").append(resultado.skipReason());
                    if (resultado.sessionRpe() != null)
                        bloque.append(", esfuerzo percibido ").append(resultado.sessionRpe())
                                .append(" sobre 10");
                    bloque.append('\n');
                }
            }

            derivation.from(id).ifPresent(derivada -> bloque.append("  fecha que tiene ahora: ")
                    .append(derivada.workout().scheduledDate()).append('\n'));

            var prescritos = porSesion.getOrDefault(id, List.of());
            if (!prescritos.isEmpty()) {
                bloque.append("  lo que se le prescribio:\n");
                for (var p : prescritos) bloque.append("    ").append(describir(p)).append('\n');
            }

            bloque.append("  QUE PUEDES CAMBIAR AQUI: ")
                    .append(intervencion.instruccion()).append('\n');

            if (intervencion.exigeRevisionDeSeguridad()) {
                var dolieron = prescritos.stream()
                        .filter(p -> "PAIN_OR_DISCOMFORT".equals(p.skipReason()))
                        .map(PreviousWeekSummary.PrescriptionOutcome::exerciseId)
                        .toList();
                bloque.append(dolieron.isEmpty()
                        ? "  AVISO: hubo dolor y no consta en que ejercicio. No conserves "
                        + "ninguno de los de arriba.\n"
                        : "  AVISO: dolio " + dolieron + ". Ese ejercicio NO puede volver.\n");
            }
        }

        return bloque.toString();
    }

    /** Una linea legible por ejercicio prescrito, con su dosis. */
    private String describir(PreviousWeekSummary.PrescriptionOutcome p) {
        var linea = new StringBuilder();
        linea.append("exercise_id ").append(p.exerciseId());
        if (p.name() != null) linea.append(" (").append(p.name()).append(')');

        if ("DURATION".equals(p.prescriptionType()))
            linea.append(": ").append(p.durationSeconds()).append(" s");
        else
            linea.append(": ").append(p.sets()).append("x").append(p.reps());

        if (p.restSeconds() != null) linea.append(", descanso ").append(p.restSeconds()).append(" s");

        // Pautado y usado van separados y etiquetados. El principio 8 pide el
        // peso que SE USO para decidir si subir, y mezclarlo con el pautado
        // haria que subiera sobre un numero que la persona nunca levanto.
        if (p.loadKg() != null) linea.append(", pautado ").append(p.loadKg()).append(" kg");
        if (p.actualLoadKg() != null) linea.append(", uso ").append(p.actualLoadKg()).append(" kg");

        if (p.completionPct() != null) linea.append(" -> ").append(p.completionPct()).append(" %");
        if (p.skipReason() != null) linea.append(", motivo: ").append(p.skipReason());

        return linea.toString();
    }

    private Map<Long, List<PreviousWeekSummary.PrescriptionOutcome>> prescripcionesPorSesion(
            PreviousWeekSummary previous) {
        var porSesion = new LinkedHashMap<Long, List<PreviousWeekSummary.PrescriptionOutcome>>();
        if (previous == null || previous.prescriptions() == null) return porSesion;
        for (var p : previous.prescriptions()) {
            if (p.plannedWorkoutId() == null) continue;
            porSesion.computeIfAbsent(p.plannedWorkoutId(), k -> new ArrayList<>()).add(p);
        }
        return porSesion;
    }

    private Map<Long, PreviousWeekSummary.WorkoutOutcome> resultadosPorSesion(
            PreviousWeekSummary previous) {
        var porId = new LinkedHashMap<Long, PreviousWeekSummary.WorkoutOutcome>();
        if (previous == null || previous.workouts() == null) return porId;
        previous.workouts().stream()
                .filter(w -> w.plannedWorkoutId() != null)
                .forEach(w -> porId.put(w.plannedWorkoutId(), w));
        return porId;
    }

    private void appendRetry(StringBuilder prompt, AiRetryFeedback feedback) {
        boolean conPropuesta = feedback.rejectedOutput() != null
                && !feedback.rejectedOutput().isBlank();

        if (conPropuesta) {
            prompt.append("""

                    INTENTO ANTERIOR RECHAZADO
                    Esta fue tu propuesta anterior:
                    """).append(feedback.rejectedOutput().strip()).append('\n');
        } else {
            prompt.append("\nINTENTO ANTERIOR RECHAZADO\n");
        }

        if (!feedback.rejectionProblems().isEmpty()) {
            prompt.append("\nEl backend la rechazo por estos motivos:\n");
            feedback.rejectionProblems()
                    .forEach(problem -> prompt.append("- ").append(problem).append('\n'));
        }
        if (!feedback.earlierProblems().isEmpty()) {
            prompt.append("\nErrores de intentos anteriores que tampoco puedes repetir:\n");
            feedback.earlierProblems()
                    .forEach(problem -> prompt.append("- ").append(problem).append('\n'));
        }

        prompt.append("""

                Corrige cada motivo sobre tu propuesta anterior, sin rehacerla. Sigues
                corrigiendo SOLO las sesiones senaladas: no anadas ninguna otra para
                resolver un motivo. Si un motivo dice que una sustitucion no vale,
                cambia ese ejercicio y conserva el resto de la sesion.
                Si no puedes resolver una sesion sin romper otra regla, OMITELA del
                arreglo: conservarla es mejor que devolver algo invalido.
                La lista de motivos solo nombra lo que fallo esta vez: todas las reglas
                siguen valiendo. Antes de responder, repasa tu propuesta contra las
                reglas 1 a 14 completas.
                """);
    }
}
