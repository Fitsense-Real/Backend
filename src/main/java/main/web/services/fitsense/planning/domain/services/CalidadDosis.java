package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;

import java.util.*;

/**
 * Rediseno P-G1: distingue una sesion CORTA pero razonable de una INSUFICIENTE y compara la dosis semanal con su
 * objetivo, para no prometer lo que la dosis no respalda. No rechaza nada: informa (traza del plan y metricas).
 * <ul>
 *   <li>Sesion RAZONABLE: al menos 3 ejercicios de trabajo y 6 series efectivas (resistidas + bloques de cardio).
 *       Criterio de sesion corta eficaz: 3-5 multiarticulares y 6-12 series efectivas (Iversen 2021) [ER] + [HP];
 *       cifras [PRO]. CORTA si ademas dura menos de la mitad del maximo disponible.</li>
 *   <li>Semana: series directas por grupo grande con material frente al objetivo de DosisSemanalPolicy y minutos de
 *       trabajo aerobico frente a su objetivo. Por debajo: {@code DOSIS_LIMITADA} (la disponibilidad, el material o
 *       la politica no permiten la dosis objetivo; el mensaje al usuario no debe prometer resultados).</li>
 * </ul>
 */
public final class CalidadDosis {

    public enum ClaseSesion { INSUFICIENTE, CORTA_RAZONABLE, COMPLETA }

    public static final int MIN_EJERCICIOS = 3;
    public static final int MIN_SERIES_EFECTIVAS = 6;

    private CalidadDosis() {}

    public static Map<String, Object> evaluar(PlanGenerationContext ctx, List<PlanDraft.DraftWorkout> sesiones,
                                              SessionDurationEstimator estimador) {
        var porId = new HashMap<Long, CandidateExercise>();
        ctx.availableExercises().forEach(c -> porId.put(c.exerciseId(), c));
        var comp = CatalogEligibilityPolicy.complementos(ctx.availableExercises(), ctx.profile().goalType());
        String obj = ctx.profile().goalType(), nivel = ctx.profile().fitnessLevel();
        var semana = new TreeMap<String, Integer>();
        long aerobico = 0;
        var clases = new ArrayList<Map<String, Object>>();
        for (var w : sesiones) {
            int trabajo = 0, efectivas = 0;
            for (var e : w.exercises()) {
                var c = porId.get(e.exerciseId());
                if (c == null) continue;
                var clase = CapabilityPrescriptionPolicy.claseDe(c);
                int series = e.plannedSets() == null ? 0 : e.plannedSets();
                if (clase == CapabilityPrescriptionPolicy.Clase.CARDIO) {
                    trabajo++;
                    efectivas += series;
                    if (e.plannedDurationSeconds() != null) aerobico += (long) series * e.plannedDurationSeconds();
                } else if (!comp.contains(c.exerciseId()) && (clase == CapabilityPrescriptionPolicy.Clase.ESTRUCTURAL
                        || clase == CapabilityPrescriptionPolicy.Clase.RESISTIDO)) {
                    trabajo++;
                    efectivas += series;
                    if (c.bodyPartCode() != null) semana.merge(c.bodyPartCode(), series, Integer::sum);
                }
            }
            Long s = ctx.prescription() == null ? null : estimador.estimateSeconds(w, ctx.prescription());
            ClaseSesion clase = trabajo < MIN_EJERCICIOS || efectivas < MIN_SERIES_EFECTIVAS ? ClaseSesion.INSUFICIENTE
                    : s != null && s * 2 < ctx.profile().sessionMinutes() * 60L ? ClaseSesion.CORTA_RAZONABLE
                    : ClaseSesion.COMPLETA;
            var x = new LinkedHashMap<String, Object>();
            x.put("fecha", String.valueOf(w.scheduledDate()));
            x.put("ejercicios_trabajo", trabajo);
            x.put("series_efectivas", efectivas);
            x.put("clase", clase.name());
            clases.add(x);
        }
        var conMaterial = new TreeSet<String>();
        ctx.availableExercises().stream().filter(c -> !comp.contains(c.exerciseId()))
                .map(CandidateExercise::bodyPartCode).filter(DosisSemanalPolicy.GRUPOS_GRANDES::contains)
                .forEach(conMaterial::add);
        var bajo = new ArrayList<String>();
        for (var g : conMaterial)
            if (semana.getOrDefault(g, 0) < DosisSemanalPolicy.objetivoSeriesSemana(obj, nivel, g)) bajo.add(g);
        long objetivoAerobico = DosisSemanalPolicy.objetivoAerobicoMinutosSemana(obj, nivel) * 60L;
        boolean aerobicoBajo = objetivoAerobico > 0 && aerobico < objetivoAerobico;
        var r = new LinkedHashMap<String, Object>();
        r.put("series_por_grupo", semana);
        r.put("grupos_bajo_objetivo", bajo);
        r.put("aerobico_trabajo_minutos", aerobico / 60);
        r.put("aerobico_objetivo_minutos", objetivoAerobico / 60);
        r.put("sesiones", clases);
        r.put("estado", !bajo.isEmpty() || aerobicoBajo ? "DOSIS_LIMITADA" : "DOSIS_OBJETIVO");
        return r;
    }
}
