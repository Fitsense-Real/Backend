package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.DerivedWorkout;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.PreviousWeekSummary;
import main.web.services.fitsense.planning.domain.model.valueobjects.WeekDerivation;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * V31. Construye la semana derivada COPIANDO la anterior, conservando la
 * identidad de cada sesion.
 * <p>
 * Servicio de dominio puro: recibe el resumen de la semana anterior y las
 * fechas nuevas, y no consulta nada.
 * <p>
 * POR QUE COPIAR Y NO REGENERAR. La correspondencia entre una sesion y su
 * origen tiene que CONSTRUIRSE en el momento de crear la semana derivada. Si en
 * cambio se genera un plan desde cero y luego se intenta emparejar, no hay con
 * que: la fecha cambia entre semanas, {@code display_order} se desplaza en
 * cuanto se quita un dia, el enfoque se repite si hay tres dias
 * {@code FULL_BODY}, y el esquema ni siquiera impide dos entrenamientos el
 * mismo dia. Los tres emparejarian mal en silencio.
 * <p>
 * Esto es tambien lo que significa que la adaptacion opere POR CAMBIOS SOBRE EL
 * PLAN ANTERIOR y no por regeneracion libre de la rutina.
 * <p>
 * QUE COPIA Y QUE NO. Copia la PRESCRIPCION, no la ejecucion: series,
 * repeticiones, duracion, carga objetivo y <b>descanso</b> tal como se
 * prescribieron. El descanso importa especialmente porque es parte de la dosis
 * que 4D exige exacta; copiar series y repeticiones y perder el descanso daria
 * un plan que falla la validacion de dosis.
 * <p>
 * No copia lo que la persona hizo -{@code actualSets}, {@code completionPct}-,
 * que es evidencia de ejecucion y no una prescripcion nueva.
 */
@Service
public class WeekDerivationBuilder {

    /**
     * @param previous        la semana anterior, ya ensamblada.
     * @param sourceIdsToKeep que sesiones anteriores sobreviven, EN ORDEN. Lo
     *                        decide quien llama, no este constructor: cuales
     *                        sobreviven es una decision de adaptacion -una
     *                        reduccion de dias- y aqui solo se preserva la
     *                        identidad.
     * @param newDates        las fechas que van a USARSE, una por sesion
     *                        conservada y en el mismo orden. No son "los dias
     *                        disponibles de la semana": si se conservan dos
     *                        sesiones, aqui van dos fechas. Elegir cuales es
     *                        parte de decidir que sobrevive, y lo hace quien
     *                        llama.
     * @throws IllegalArgumentException si el numero de fechas no coincide
     *                                  EXACTAMENTE con el de sesiones a
     *                                  conservar, o si un origen viene
     *                                  repetido. Las dos cosas son errores de
     *                                  quien llama y conviene verlas antes de
     *                                  persistir, no como un fallo de
     *                                  restriccion de la base.
     */
    public WeekDerivation derive(PreviousWeekSummary previous,
                                 List<Long> sourceIdsToKeep,
                                 List<LocalDate> newDates) {

        var aConservar = sourceIdsToKeep == null ? List.<Long>of() : sourceIdsToKeep;
        var fechas = newDates == null ? List.<LocalDate>of() : newDates;

        // EXACTAMENTE una fecha por sesion conservada. Con "al menos" las
        // fechas sobrantes se ignoraban en silencio, y una fecha sobrante
        // significa que quien llama no decidio bien que dias usar.
        if (fechas.size() != aConservar.size())
            throw new IllegalArgumentException(
                    "Hay %d sesiones a conservar y %d fechas: tiene que haber exactamente una por sesion."
                            .formatted(aConservar.size(), fechas.size()));

        // Dos sesiones derivadas del mismo origen las rechazaria la base por
        // uq_pw_source_per_plan, pero para entonces ya se intento persistir un
        // plan incoherente. Mejor aqui.
        var repetidos = aConservar.stream()
                .filter(id -> java.util.Collections.frequency(aConservar, id) > 1)
                .distinct().toList();
        if (!repetidos.isEmpty())
            throw new IllegalArgumentException(
                    "Estos origenes vienen repetidos y la correspondencia es uno a uno: %s"
                            .formatted(repetidos));

        // Las dos comprobaciones de arriba van ANTES de esto a proposito, para
        // que una entrada incoherente falle igual exista o no semana anterior.
        //
        // Sin semana anterior no hay de donde derivar. Devolver una derivacion
        // vacia solo es correcto si tampoco se pidio conservar nada -la primera
        // semana del participante-; si se pidieron sesiones, callarse seria
        // afirmar que no habia ninguna que conservar, y eso es falso.
        boolean sinSemanaAnterior = previous == null
                || previous.workouts() == null
                || previous.workouts().isEmpty();
        if (sinSemanaAnterior) {
            if (!aConservar.isEmpty())
                throw new IllegalArgumentException(
                        "Se pidio conservar %d sesion(es) y no hay semana anterior de la que derivarlas."
                                .formatted(aConservar.size()));
            return vacia();
        }

        var porId = new LinkedHashMap<Long, PreviousWeekSummary.WorkoutOutcome>();
        previous.workouts().stream()
                .filter(w -> w.plannedWorkoutId() != null)
                .sorted(Comparator.comparing(PreviousWeekSummary.WorkoutOutcome::scheduledDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(w -> porId.put(w.plannedWorkoutId(), w));

        var ejerciciosPorSesion = ejerciciosPorSesion(previous);

        var derivadas = new ArrayList<DerivedWorkout>();
        for (int i = 0; i < aConservar.size(); i++) {
            Long origen = aConservar.get(i);
            var anterior = porId.get(origen);
            if (anterior == null)
                throw new IllegalArgumentException(
                        "Se pidio conservar la sesion %d y no esta en la semana anterior."
                                .formatted(origen));

            derivadas.add(new DerivedWorkout(origen, new PlanDraft.DraftWorkout(
                    fechas.get(i),
                    WorkoutFocus.valueOf(anterior.focusCode()),
                    anterior.workoutName(),
                    anterior.expectedDurationMinutes(),
                    ejerciciosPorSesion.getOrDefault(origen, List.of()))));
        }

        // Las que no se conservan, en el orden en que estaban.
        var sinDerivar = porId.keySet().stream()
                .filter(id -> !aConservar.contains(id))
                .toList();

        return new WeekDerivation(derivadas, sinDerivar);
    }

    /**
     * Las prescripciones agrupadas por sesion, convertidas a ejercicios de
     * borrador. Se agrupan por {@code plannedWorkoutId} y no por fecha, que es
     * justo la clave que no identifica univocamente.
     */
    private Map<Long, List<PlanDraft.DraftExercise>> ejerciciosPorSesion(
            PreviousWeekSummary previous) {

        var porSesion = new LinkedHashMap<Long, List<PlanDraft.DraftExercise>>();
        if (previous.prescriptions() == null) return porSesion;

        for (var p : previous.prescriptions()) {
            if (p.plannedWorkoutId() == null || p.exerciseId() == null) continue;
            porSesion.computeIfAbsent(p.plannedWorkoutId(), k -> new ArrayList<>())
                    .add(new PlanDraft.DraftExercise(
                            p.exerciseId(),
                            p.prescriptionType() == null ? null
                                    : PrescriptionType.valueOf(p.prescriptionType()),
                            p.sets(),
                            p.reps(),
                            p.durationSeconds(),
                            p.loadKg(),
                            p.restSeconds(),
                            null));
        }
        return porSesion;
    }

    private static WeekDerivation vacia() {
        return new WeekDerivation(List.of(), List.of());
    }
}
