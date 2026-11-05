package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * P-C2 (rediseno R/R'): IMPACTO ARTICULAR en tres niveles, independiente de la dificultad numerica del catalogo.
 * <p>
 * Base fisica [ER]: caminar ~1,2 x peso corporal de fuerza vertical, trote lento > 50 % mas de carga que la marcha,
 * saltos 3,3-5,4 x peso corporal; la intensidad pliometrica sube con el apoyo a una pierna, la velocidad y la altura
 * (docs/FitSense/rediseno_politicas/02_EVIDENCIA_DOSIS.md §6). El criterio operativo es [HP] y la etiqueta de cada
 * ejercicio debe revisarla una persona [PRO]: aqui se deriva del dato del catalogo {@code high_impact} (V10) y del
 * nombre, como respaldo hasta que el catalogo tenga un campo {@code impact_level} revisado.
 * <ul>
 *   <li>{@link Nivel#ALTO}: {@code high_impact} del catalogo (saltos, pliometria, olimpicos, sprint), salvo los
 *       gestos de vuelo bajo y bilateral que pasan a MODERADO.</li>
 *   <li>{@link Nivel#MODERADO}: vuelo de baja amplitud, bilateral y repetitivo, o carrera por debajo del sprint
 *       (trote, skipping, jumping jacks, comba).</li>
 *   <li>{@link Nivel#BAJO}: siempre un pie en el suelo o sin cargar el peso corporal.</li>
 *   <li>{@link Nivel#DESCONOCIDO}: sin dato del catalogo; nunca se trata como bajo.</li>
 * </ul>
 */
public final class ImpactoPolicy {

    public enum Nivel { BAJO, MODERADO, ALTO, DESCONOCIDO }

    private static final Pattern MODERADO = Pattern.compile(
            "\\b(trote|trotar|trotando|correr|carrera|corriendo|skipping|rodillas altas|jumping jacks?|"
                    + "piernas separadas|comba|cuerda|saltar la cuerda|tijeras|escaladores? r[aá]pidos?)\\b");
    private static final Pattern ALTO_POR_NOMBRE = Pattern.compile(
            "\\b(salto|saltos|saltando|pliom[eé]trico|burpee|burpees|sprint|esprint|caja|rebote|zancada con salto|"
                    + "a una pierna con salto|patinador)\\b");

    private ImpactoPolicy() {}

    public static Nivel nivel(CandidateExercise c) {
        if (c == null) return Nivel.DESCONOCIDO;
        String nombre = c.name() == null ? "" : c.name().toLowerCase(Locale.ROOT);
        boolean moderadoPorNombre = MODERADO.matcher(nombre).find();
        if (Boolean.TRUE.equals(c.highImpact())) {
            // El catalogo lo marca de alto impacto: queda ALTO salvo un gesto de vuelo bajo y bilateral sin nada
            // que lo agrave en el nombre (un jumping jack si; una zancada con salto no).
            if (moderadoPorNombre && !ALTO_POR_NOMBRE.matcher(nombre.replaceAll("saltos con las piernas separadas",
                    "")).find())
                return Nivel.MODERADO;
            return Nivel.ALTO;
        }
        if (c.highImpact() == null) return Nivel.DESCONOCIDO;
        if (moderadoPorNombre) return Nivel.MODERADO;
        return Nivel.BAJO;
    }

    /**
     * ¿Sube el impacto al cambiar {@code original} por {@code sustituto}? Con un dato desconocido en el sustituto se
     * responde que si (prudencia); con el original desconocido solo se admite un sustituto BAJO.
     */
    public static boolean sube(CandidateExercise original, CandidateExercise sustituto) {
        var o = nivel(original);
        var s = nivel(sustituto);
        if (s == Nivel.DESCONOCIDO) return true;
        if (o == Nivel.DESCONOCIDO) return s != Nivel.BAJO;
        return s.ordinal() > o.ordinal();
    }
}
