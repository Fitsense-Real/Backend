package main.web.services.fitsense.planning.infrastructure.generation.rules;

import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningPolicy;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.planning.domain.services.DosisSemanalPolicy;

import java.util.*;
import java.util.function.Predicate;

/**
 * Control de volumen y de dosis de la semana que se esta construyendo (rediseno P-G1, politicas nuevas).
 * <ul>
 *   <li>Lleva la cuenta de las series DIRECTAS por grupo (solo trabajo resistido: {@code esResistido}) de las
 *       sesiones ya cerradas y de la sesion en curso.</li>
 *   <li>{@link #cabe}: techo semanal y por sesion de {@link DosisSemanalPolicy} (disenos A y B).</li>
 *   <li>{@link #grupoPendiente} y {@link #aerobicoPendiente}: dosis objetivo (solo diseno A), repartida entre las
 *       sesiones de la semana que entrenan ese grupo.</li>
 * </ul>
 * No decide QUE ejercicio entra: eso sigue siendo del selector y de las reglas de siempre (V15, V18, V22, V24...).
 */
final class ControlVolumen {

    private final String objetivo;
    private final String nivel;
    private final boolean porDosis;
    private final Predicate<CandidateExercise> esResistido;
    private final Map<String, Integer> sesionesPorGrupo = new HashMap<>();
    private final int sesiones;
    private final Map<String, Integer> semana = new HashMap<>();
    private long aerobicoSemanaSegundos;

    ControlVolumen(PlanGenerationContext context, List<WorkoutFocus> enfoques, Predicate<CandidateExercise> esResistido) {
        this.objetivo = context.profile().goalType();
        this.nivel = context.profile().fitnessLevel();
        this.porDosis = context.policy().duracion() == PlanningPolicy.Duracion.DOSIS_SEMANAL;
        this.esResistido = esResistido;
        this.sesiones = enfoques.size();
        for (var e : enfoques)
            for (var g : e.bodyPartCodes()) sesionesPorGrupo.merge(g, 1, Integer::sum);
    }

    /** null si la politica no controla volumen (historica). */
    static ControlVolumen de(PlanGenerationContext context, List<WorkoutFocus> enfoques,
                             Predicate<CandidateExercise> esResistido) {
        return context.policy() != null && context.policy().volumenSemanal()
                ? new ControlVolumen(context, enfoques, esResistido) : null;
    }

    boolean porDosis() {
        return porDosis;
    }

    /** Series del grupo en la sesion en curso. */
    static Map<String, Integer> seriesPorGrupo(List<Map.Entry<CandidateExercise, Integer>> dia,
                                               Predicate<CandidateExercise> esResistido) {
        var m = new HashMap<String, Integer>();
        for (var e : dia)
            if (e.getKey() != null && esResistido.test(e.getKey()) && e.getKey().bodyPartCode() != null)
                m.merge(e.getKey().bodyPartCode(), e.getValue(), Integer::sum);
        return m;
    }

    /** ¿Caben {@code series} mas del grupo de {@code c} en la sesion y en la semana? Lo no resistido siempre cabe. */
    boolean cabe(CandidateExercise c, int series, Map<String, Integer> diaPorGrupo) {
        if (c == null || !esResistido.test(c) || c.bodyPartCode() == null) return true;
        String g = c.bodyPartCode();
        int enDia = diaPorGrupo.getOrDefault(g, 0);
        return enDia + series <= DosisSemanalPolicy.techoSeriesSesion(nivel)
                && semana.getOrDefault(g, 0) + enDia + series <= DosisSemanalPolicy.techoSeriesSemana(nivel);
    }

    /** Objetivo de series del grupo para UNA de sus sesiones de la semana (diseno A). */
    int objetivoSesion(String grupo) {
        int objetivoSemana = DosisSemanalPolicy.objetivoSeriesSemana(objetivo, nivel, grupo);
        int n = Math.max(1, sesionesPorGrupo.getOrDefault(grupo, 1));
        return (objetivoSemana + n - 1) / n;
    }

    /**
     * El grupo del enfoque mas lejos de su objetivo de la sesion, entre los que tienen material ({@code conMaterial})
     * y aun caben; vacio si todos lo alcanzaron. Solo diseno A.
     */
    Optional<String> grupoPendiente(WorkoutFocus enfoque, Map<String, Integer> diaPorGrupo, Set<String> conMaterial) {
        if (!porDosis) return Optional.empty();
        String mejor = null;
        int deficit = 0;
        for (var g : enfoque.bodyPartCodes()) {
            if (!conMaterial.contains(g)) continue;
            int falta = objetivoSesion(g) - diaPorGrupo.getOrDefault(g, 0);
            boolean cabeAlgo = diaPorGrupo.getOrDefault(g, 0) + 2 <= DosisSemanalPolicy.techoSeriesSesion(nivel)
                    && semana.getOrDefault(g, 0) + diaPorGrupo.getOrDefault(g, 0) + 2
                    <= DosisSemanalPolicy.techoSeriesSemana(nivel);
            if (falta > deficit && cabeAlgo) {
                deficit = falta;
                mejor = g;
            }
        }
        return Optional.ofNullable(mejor);
    }

    /** ¿Le falta trabajo aerobico a esta sesion para su parte del objetivo semanal? Solo diseno A. */
    boolean aerobicoPendiente(long aerobicoDiaSegundos) {
        if (!porDosis || sesiones == 0) return false;
        long objetivoSemana = DosisSemanalPolicy.objetivoAerobicoMinutosSemana(objetivo, nivel) * 60L;
        if (objetivoSemana == 0) return false;
        long porSesion = (objetivoSemana + sesiones - 1) / sesiones;
        return aerobicoDiaSegundos < porSesion;
    }

    /** Cierra la sesion: suma sus series y su trabajo aerobico a la semana. */
    void cerrarSesion(Map<String, Integer> diaPorGrupo, long aerobicoDiaSegundos) {
        diaPorGrupo.forEach((g, n) -> semana.merge(g, n, Integer::sum));
        aerobicoSemanaSegundos += aerobicoDiaSegundos;
    }

    Map<String, Integer> semana() {
        return Collections.unmodifiableMap(semana);
    }

    long aerobicoSemanaSegundos() {
        return aerobicoSemanaSegundos;
    }
}
