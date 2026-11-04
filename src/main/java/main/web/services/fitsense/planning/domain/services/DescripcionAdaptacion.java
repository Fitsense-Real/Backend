package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptationOutcome;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptedWeekResult;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.DerivedWorkout;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PreviousWeekSummary;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * EL TEXTO QUE VE LA PERSONA TRAS UNA ADAPTACION, A PARTIR DE LO QUE CAMBIO DE VERDAD (solo presentacion).
 * <p>
 * Antes, {@code adjustment_reason} llevaba el mensaje que la tabla de decision escribe ANTES de adaptar («Nos dijiste
 * que resultaba dificil, asi que bajamos la exigencia»), tambien cuando la IA cambio ejercicios por otros del mismo
 * nivel o cuando no se aplico nada. Aqui el texto se compone con lo publicado, comparando la semana medida con la
 * derivada, y solo afirma lo que el sistema registra:
 * <ul>
 *   <li>{@link Cambio#REDUCCION_DIFICULTAD}: un ejercicio sustituido por otro del mismo patron con MENOR dificultad
 *       REGISTRADA en el catalogo. No se afirma que sea mas facil en la practica: es lo que dice el catalogo;</li>
 *   <li>{@link Cambio#REDUCCION_DOSIS}: el mismo ejercicio con menos series, repeticiones o segundos;</li>
 *   <li>{@link Cambio#SUSTITUCION_LATERAL}: sustituido por otro del mismo patron y la MISMA dificultad registrada;</li>
 *   <li>{@link Cambio#SUSTITUCION_MAYOR_DIFICULTAD}: sustituido por otro del mismo patron con MAYOR dificultad
 *       registrada. Ningun validador lo impide (V8C exige el patron, la elegibilidad solo el nivel maximo de la
 *       persona), asi que puede publicarse y hay que decirlo;</li>
 *   <li>{@link Cambio#SUSTITUCION_SIN_COMPARACION}: sustituido, pero falta la ficha de uno de los dos ejercicios en
 *       el catalogo de la semana (por ejemplo, el retirado ya no es elegible) y no se puede comparar la dificultad;</li>
 *   <li>{@link Cambio#SUSTITUCION_SEGURIDAD}: cambio o retirada en una sesion senalada por dolor (la dificultad no
 *       se compara: el motivo es la seguridad);</li>
 *   <li>{@link Cambio#RETIRADA} y {@link Cambio#REPROGRAMADA}: ejercicio retirado sin reemplazo o sesion movida de
 *       dia;</li>
 *   <li>{@link Cambio#NO_APLICADA}: sesion senalada que no cambio;</li>
 *   <li>{@link Cambio#CONSERVADA_SIN_ALTERNATIVA}: el backend demostro antes de llamar a la IA que no habia ningun
 *       cambio compatible (factibilidad previa) y la sesion se mantiene.</li>
 * </ul>
 * NO DECIDE NADA: no cambia la decision, la validacion, el prompt ni el plan. Solo el texto.
 */
public final class DescripcionAdaptacion {

    public enum Cambio {
        REDUCCION_DIFICULTAD, REDUCCION_DOSIS, SUSTITUCION_LATERAL, SUSTITUCION_MAYOR_DIFICULTAD,
        SUSTITUCION_SIN_COMPARACION, SUSTITUCION_SEGURIDAD, RETIRADA, REPROGRAMADA, NO_APLICADA,
        CONSERVADA_SIN_ALTERNATIVA
    }

    private DescripcionAdaptacion() {
    }

    /** Cuantos ejercicios (o sesiones, en NO_APLICADA y CONSERVADA_SIN_ALTERNATIVA) hay de cada clase. */
    public static Map<Cambio, Integer> clasificar(AdaptedWeekResult resultado, PreviousWeekSummary previa,
                                                  List<CandidateExercise> catalogo, PlanAdjustment orden) {
        var conteo = new EnumMap<Cambio, Integer>(Cambio.class);
        if (orden == null || orden.targetedSessions() == null || orden.targetedSessions().isEmpty()) return conteo;
        var cat = new LinkedHashMap<Long, CandidateExercise>();
        if (catalogo != null) catalogo.forEach(c -> cat.putIfAbsent(c.exerciseId(), c));
        var informe = resultado.conservacion();
        boolean sinAlternativa = informe != null && informe.correccionesDelBackend() != null
                && informe.correccionesDelBackend().stream()
                .anyMatch(c -> c != null && c.startsWith(AdaptationOutcome.NOTA_FACTIBILIDAD));
        var aplicadas = informe == null || informe.aplicadas() == null ? List.<Long>of() : informe.aplicadas();
        // RETIRADA LOCALIZADA POR DOLOR (candidata, solo con fitsense.adaptation.pain-localized-removal=on): la sesion
        // senalada SI cambio (se retiro lo que dolio), aunque no la aplicara la IA. Se describe lo que cambio de verdad
        // en vez de decir que se mantuvo. Sin esa nota (el interruptor apagado) el texto es el de siempre.
        boolean retiradaSegura = informe != null && informe.correccionesDelBackend() != null
                && informe.correccionesDelBackend().stream()
                .anyMatch(c -> c != null && c.startsWith(AdaptedWeekProducer.NOTA_RETIRADA_SEGURA));
        for (var senalada : orden.targetedSessions()) {
            Long id = senalada.plannedWorkoutId();
            var derivada = resultado.semana().stream().filter(d -> id.equals(d.sourcePlannedWorkoutId())).findFirst();
            boolean cambioDeSeguridad = retiradaSegura && derivada.isPresent() && senalada.intervencion() != null
                    && senalada.intervencion().exigeRevisionDeSeguridad()
                    && !mismosEjercicios(id, previa, derivada.get());
            if ((!aplicadas.contains(id) && !cambioDeSeguridad) || derivada.isEmpty()) {
                sumar(conteo, sinAlternativa ? Cambio.CONSERVADA_SIN_ALTERNATIVA : Cambio.NO_APLICADA, 1);
                continue;
            }
            describirSesion(id, senalada.intervencion(), previa, derivada.get(), cat, conteo);
        }
        return conteo;
    }

    private static void describirSesion(Long id, TipoIntervencion intervencion, PreviousWeekSummary previa,
                                        DerivedWorkout derivada, Map<Long, CandidateExercise> cat,
                                        Map<Cambio, Integer> conteo) {
        var antes = new LinkedHashMap<Long, PreviousWeekSummary.PrescriptionOutcome>();
        if (previa != null && previa.prescriptions() != null)
            previa.prescriptions().stream().filter(p -> id.equals(p.plannedWorkoutId()))
                    .forEach(p -> antes.putIfAbsent(p.exerciseId(), p));
        var despues = new LinkedHashMap<Long, PlanDraft.DraftExercise>();
        derivada.workout().exercises().forEach(e -> despues.putIfAbsent(e.exerciseId(), e));

        var sesionAnterior = previa == null || previa.workouts() == null ? null : previa.workouts().stream()
                .filter(w -> id.equals(w.plannedWorkoutId())).findFirst().orElse(null);
        if (sesionAnterior != null && sesionAnterior.scheduledDate() != null
                && derivada.workout().scheduledDate() != null
                && sesionAnterior.scheduledDate().getDayOfWeek() != derivada.workout().scheduledDate().getDayOfWeek())
            sumar(conteo, Cambio.REPROGRAMADA, 1);

        // Mismo ejercicio con menos dosis.
        for (var e : antes.entrySet()) {
            var n = despues.get(e.getKey());
            if (n != null && menorDosis(e.getValue(), n)) sumar(conteo, Cambio.REDUCCION_DOSIS, 1);
        }

        var quitados = new ArrayList<>(antes.keySet().stream().filter(x -> !despues.containsKey(x)).toList());
        var anadidos = despues.keySet().stream().filter(x -> !antes.containsKey(x)).toList();
        if (intervencion != null && intervencion.exigeRevisionDeSeguridad()) {
            int cambios = Math.max(quitados.size(), anadidos.size());
            if (cambios > 0) sumar(conteo, Cambio.SUSTITUCION_SEGURIDAD, cambios);
            return;
        }
        // Cada anadido con un retirado DISTINTO del mismo patron. Se prefiere el retirado de mayor dificultad
        // registrada (la lectura mas favorable que el catalogo permite), pero cada pareja se clasifica por lo que
        // es: menor, igual o mayor. Sin ficha de uno de los dos, no se compara.
        for (var a : anadidos) {
            var ca = cat.get(a);
            Long pareja = null;
            if (ca != null)
                for (var q : quitados) {
                    var cq = cat.get(q);
                    if (cq == null || !Objects.equals(ca.movementPattern(), cq.movementPattern())) continue;
                    if (pareja == null || cq.difficulty() > cat.get(pareja).difficulty()) pareja = q;
                }
            if (pareja == null) {
                sumar(conteo, Cambio.SUSTITUCION_SIN_COMPARACION, 1);
                // Consume un retirado (el primero sin ficha, si lo hay) para no contarlo ademas como retirada.
                var sinFicha = quitados.stream().filter(q -> cat.get(q) == null).findFirst();
                if (sinFicha.isPresent()) quitados.remove(sinFicha.get());
                else if (!quitados.isEmpty()) quitados.remove(0);
                continue;
            }
            int difAntes = cat.get(pareja).difficulty(), difDespues = ca.difficulty();
            sumar(conteo, difDespues < difAntes ? Cambio.REDUCCION_DIFICULTAD
                    : difDespues == difAntes ? Cambio.SUSTITUCION_LATERAL : Cambio.SUSTITUCION_MAYOR_DIFICULTAD, 1);
            quitados.remove(pareja);
        }
        // Lo retirado que no se reemplazo es una retirada, haya o no otras sustituciones en la sesion.
        if (!quitados.isEmpty()) sumar(conteo, Cambio.RETIRADA, quitados.size());
    }

    private static boolean mismosEjercicios(Long id, PreviousWeekSummary previa, DerivedWorkout derivada) {
        if (previa == null || previa.prescriptions() == null) return true;
        var antes = previa.prescriptions().stream().filter(p -> id.equals(p.plannedWorkoutId()))
                .map(PreviousWeekSummary.PrescriptionOutcome::exerciseId).collect(java.util.stream.Collectors.toSet());
        var despues = derivada.workout().exercises().stream().map(PlanDraft.DraftExercise::exerciseId)
                .collect(java.util.stream.Collectors.toSet());
        return antes.equals(despues);
    }

    private static boolean menorDosis(PreviousWeekSummary.PrescriptionOutcome antes, PlanDraft.DraftExercise n) {
        return menor(antes.sets(), n.plannedSets()) || menor(antes.reps(), n.plannedReps())
                || menor(antes.durationSeconds(), n.plannedDurationSeconds());
    }

    private static boolean menor(Number antes, Number despues) {
        return antes != null && despues != null && despues.intValue() < antes.intValue();
    }

    private static void sumar(Map<Cambio, Integer> conteo, Cambio c, int n) {
        conteo.merge(c, n, Integer::sum);
    }

    // =================================================================================================== texto

    /**
     * El texto para {@code adjustment_reason} cuando se ordeno adaptar: una frase con el motivo que la persona registro y
     * una frase por cada clase de cambio publicado. Sustituye al motivo tecnico de la orden («Adherencia observada 66.67 %
     * (cobertura 100.00 %). Entre 50 % y 80 %: se corrige solo la peor sesion.»), que sigue intacto en la orden, en el
     * snapshot y en {@code plan_adaptation_attempts}. Si no hay sesiones senaladas o no se clasifico nada, devuelve el
     * mensaje original sin tocarlo.
     */
    public static String texto(String mensajeOriginal, PlanAdjustment orden, Map<Cambio, Integer> conteo) {
        if (orden == null || orden.targetedSessions() == null || orden.targetedSessions().isEmpty() || conteo.isEmpty())
            return mensajeOriginal;
        var partes = new ArrayList<String>();
        var tipos = orden.targetedSessions().stream().map(PlanAdjustment.TargetedSession::intervencion)
                .filter(Objects::nonNull).distinct().toList();
        partes.add(tipos.size() == 1 ? motivo(tipos.get(0)) : "Revisamos las sesiones que nos indicaste.");

        int n;
        if ((n = conteo.getOrDefault(Cambio.REDUCCION_DIFICULTAD, 0)) > 0)
            partes.add("En %d %s usamos una variante registrada como de menor dificultad.".formatted(n, ejercicios(n)));
        if ((n = conteo.getOrDefault(Cambio.REDUCCION_DOSIS, 0)) > 0)
            partes.add("En %d %s bajamos un poco el trabajo (menos series, repeticiones o tiempo).".formatted(n,
                    ejercicios(n)));
        if ((n = conteo.getOrDefault(Cambio.SUSTITUCION_LATERAL, 0)) > 0)
            partes.add(("Cambiamos %d %s por %s con la misma dificultad registrada: es una alternativa, no "
                    + "necesariamente más fácil.").formatted(n, ejercicios(n), n == 1 ? "otro" : "otros"));
        if ((n = conteo.getOrDefault(Cambio.SUSTITUCION_MAYOR_DIFICULTAD, 0)) > 0)
            partes.add(("Cambiamos %d %s por %s registrado%s como de mayor dificultad.")
                    .formatted(n, ejercicios(n), n == 1 ? "otro" : "otros", n == 1 ? "" : "s"));
        if ((n = conteo.getOrDefault(Cambio.SUSTITUCION_SIN_COMPARACION, 0)) > 0)
            partes.add(("Cambiamos %d %s por %s; no tenemos un dato para comparar su dificultad.")
                    .formatted(n, ejercicios(n), n == 1 ? "otro" : "otros"));
        if ((n = conteo.getOrDefault(Cambio.SUSTITUCION_SEGURIDAD, 0)) > 0)
            partes.add(("Por seguridad, cambiamos o retiramos %d %s. Si la molestia continúa, consulta a un "
                    + "profesional de la salud.").formatted(n, ejercicios(n)));
        if ((n = conteo.getOrDefault(Cambio.RETIRADA, 0)) > 0)
            partes.add("Retiramos %d %s de la sesión.".formatted(n, ejercicios(n)));
        if ((n = conteo.getOrDefault(Cambio.REPROGRAMADA, 0)) > 0)
            partes.add(n == 1 ? "Movimos la sesión a otro día." : "Movimos %d sesiones a otro día.".formatted(n));
        if ((n = conteo.getOrDefault(Cambio.NO_APLICADA, 0)) > 0)
            partes.add(n == 1 ? "En una de las sesiones no aplicamos cambios esta semana: se mantiene como estaba."
                    : "En %d sesiones no aplicamos cambios esta semana: se mantienen como estaban.".formatted(n));
        if ((n = conteo.getOrDefault(Cambio.CONSERVADA_SIN_ALTERNATIVA, 0)) > 0)
            partes.add((n == 1 ? "En una de las sesiones" : "En %d sesiones".formatted(n))
                    + " no encontramos un cambio compatible con tu plan, así que por ahora se mantiene igual.");
        // Ante «demasiado dificil», si nada de lo publicado BAJA lo registrado (dificultad o dosis), se invita a
        // volver a indicarlo: lateral, mayor, sin comparacion, retirada o sin efecto no demuestran una reduccion.
        boolean dificil = tipos.contains(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA);
        if (dificil && conteo.getOrDefault(Cambio.REDUCCION_DIFICULTAD, 0) == 0
                && conteo.getOrDefault(Cambio.REDUCCION_DOSIS, 0) == 0)
            partes.add("Si te sigue resultando difícil, indícalo de nuevo al registrar la sesión.");
        var texto = String.join(" ", partes);
        return texto.length() <= 500 ? texto : texto.substring(0, 497) + "...";
    }

    private static String ejercicios(int n) {
        return n == 1 ? "ejercicio" : "ejercicios";
    }

    private static String motivo(TipoIntervencion t) {
        return switch (t) {
            case VARIANTE_MAS_SENCILLA_O_CARGA -> "Nos dijiste que una sesión te resultó difícil.";
            case REVISAR_CARGA_Y_RECUPERACION -> "Nos contaste que llegabas con cansancio.";
            case INVESTIGAR_CAMBIO_MINIMO -> "Nos dijiste que te costó encontrar motivación.";
            case ACORTAR_O_REORGANIZAR -> "Nos dijiste que te faltó tiempo.";
            case REPROGRAMAR_DIAS -> "Nos dijiste que cambió tu horario.";
            case SEGURIDAD_ANTE_DOLOR -> "Reportaste molestias.";
            default -> "Revisamos la sesión que nos indicaste.";
        };
    }
}
