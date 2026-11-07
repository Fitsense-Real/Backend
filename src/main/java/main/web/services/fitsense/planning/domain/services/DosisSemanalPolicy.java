package main.web.services.fitsense.planning.domain.services;

import java.util.Set;

/**
 * DOSIS SEMANAL del rediseno P-G1 (orden del 05/11/2026): con {@code sessionMinutes} como MAXIMO disponible, el
 * contenido de la semana lo deciden la cobertura de patrones y una dosis por objetivo, no el reloj.
 * <p>
 * Cada cifra lleva su etiqueta de respaldo ({@code docs/FitSense/rediseno_politicas/02_EVIDENCIA_DOSIS.md}):
 * [SP] salud publica, [ER] evidencia de entrenamiento, [HP] heuristica de producto, [PRO] pendiente de validacion
 * profesional. NINGUNA es una validacion profesional: todas las cifras numericas exactas son [PRO].
 * <ul>
 *   <li>TECHO semanal de series directas por grupo (todas las politicas nuevas, validador V26): impide generar
 *       volumen solo para completar tiempo (caso de hasta 36 series/semana en GAIN_MUSCLE).</li>
 *   <li>TECHO por sesion y grupo: meseta de beneficio por sesion (preprint Remmert 2025: ~11 series
 *       fraccionales) [ER, no revisado por pares] + [HP].</li>
 *   <li>OBJETIVO semanal (solo el diseno A): hasta donde llena el generador si hay tiempo. Nunca se llena por
 *       encima del objetivo para gastar minutos.</li>
 * </ul>
 */
public final class DosisSemanalPolicy {

    private DosisSemanalPolicy() {}

    /** Grupos grandes (los de P1 de recuperacion y los brazos). Las zonas pequenas tienen su propio objetivo. */
    public static final Set<String> GRUPOS_GRANDES = Set.of("chest", "back", "shoulders", "upper legs", "upper arms");
    public static final Set<String> ZONAS_PEQUENAS = Set.of("waist", "lower legs");

    /**
     * Techo semanal de series directas por grupo. [ER] la evidencia se concentra en ~10-20 series/semana y por
     * encima la ganancia adicional es pequena y con pocos datos (Pelland 2026; Aube 2022; Baz-Valle 2022); los
     * principiantes responden con menos volumen. Cifras exactas [HP][PRO].
     */
    public static int techoSeriesSemana(String nivel) {
        return switch (nivel == null ? "" : nivel) {
            case "BEGINNER" -> 12;   // prudencia [SEG/HP]: no hay evidencia de un techo para principiantes
            case "ADVANCED" -> 24;
            default -> 20;
        };
    }

    /** Techo de series directas por grupo en UNA sesion. [ER preprint] + [HP][PRO]. */
    public static int techoSeriesSesion(String nivel) {
        return "BEGINNER".equals(nivel) ? 6 : 10;
    }

    /**
     * Objetivo semanal de series directas por grupo GRANDE (diseno A). GAIN_MUSCLE: ~10 series/semana como dosis
     * que la evidencia asocia a una hipertrofia clara (ACSM 2026 / Currier 2026; Schoenfeld 2017) [ER]; el
     * principiante menos [HP]. Resto de objetivos (trabajo resistido como principal o secundario): unas 4 series
     * por grupo y semana, el minimo eficaz descrito para programas con poco tiempo (Iversen 2021) [ER], que con 2+
     * dias cubre la recomendacion de fortalecimiento de la OMS [SP]. Cifras exactas [PRO].
     */
    public static int objetivoSeriesSemana(String objetivo, String nivel, String grupo) {
        boolean pequena = ZONAS_PEQUENAS.contains(grupo);
        if ("GAIN_MUSCLE".equals(objetivo)) {
            // Inicio de la banda objetivo: principiante 6-10, intermedio 10-16, avanzado 12-20 [ER/HP][PRO].
            int base = switch (nivel == null ? "" : nivel) {
                case "BEGINNER" -> 6;
                case "ADVANCED" -> 12;
                default -> 10;
            };
            return pequena ? base / 2 : base;
        }
        return pequena ? 0 : 4;
    }

    /**
     * Objetivo semanal de minutos de TRABAJO aerobico dentro de las sesiones (diseno A). LOSE_WEIGHT: la OMS
     * recomienda 150-300 min/semana de actividad moderada y el ACSM asocia la perdida de peso a mas de 150 [SP];
     * un principiante empieza por debajo y progresa [SEG/HP]. GENERAL_FITNESS: una parte, porque la sesion
     * tambien lleva fuerza y movilidad [HP]. El resto de objetivos: sin objetivo aerobico (el cardio es
     * complemento). Son minutos de trabajo (series x segundos), no de sesion. Cifras exactas [PRO].
     */
    public static int objetivoAerobicoMinutosSemana(String objetivo, String nivel) {
        return switch (objetivo == null ? "" : objetivo) {
            // Meta 150 min/semana [SP]; el principiante empieza con unas 3 sesiones de 15 min (~45) y progresa
            // [SEG/HP]. Dentro de la semana de generacion no hay progresion: es el punto de partida.
            case "LOSE_WEIGHT" -> "BEGINNER".equals(nivel) ? 45 : 150;
            // La sesion de GENERAL_FITNESS reparte fuerza, cardio y movilidad: una parte del aerobico [HP][PRO].
            case "GENERAL_FITNESS" -> "BEGINNER".equals(nivel) ? 30 : 60;
            default -> 0;
        };
    }
}
