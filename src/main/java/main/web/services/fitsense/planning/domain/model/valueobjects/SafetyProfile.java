package main.web.services.fitsense.planning.domain.model.valueobjects;

/**
 * Restricciones de seguridad derivadas de la edad del participante.
 * <p>
 * Se calculan aqui y no en el perfil porque son una politica del generador, no
 * un dato del usuario: si manana se decide que el corte de impacto son 55 anos
 * y no 60, cambia esta clase y no hay que migrar ningun perfil.
 * <p>
 * POR QUE EXISTE
 * El conjunto elegible filtraba por equipamiento, ubicacion, dificultad y
 * bloqueos, pero no por edad. Nivel de condicion fisica y edad no son lo mismo:
 * una persona de 68 anos puede ser INTERMEDIATE y aun asi no convenirle un
 * burpee.
 * <p>
 * Y no es solo seguridad. Si el plan no encaja con la persona, la adherencia
 * baja, y el sistema interpreta esa caida como falta de compromiso: reduce
 * volumen cuando el problema era la seleccion. El ajuste responde a la senal
 * equivocada.
 * <p>
 * ATENCION: los cortes de edad son criterio conservador, no una guia clinica
 * validada. Conviene contrastarlos con el asesor antes del piloto.
 */
public record SafetyProfile(
        boolean excludeHighImpact,
        boolean excludeFloorWork,
        boolean excludeAxialLoad,
        boolean excludeKneeling,
        boolean excludeHanging,
        boolean excludeOverhead,
        boolean excludeInversion
) {

    /** Desde esta edad se retiran saltos, pliometria y levantamientos olimpicos. */
    public static final int EDAD_SIN_IMPACTO = 60;

    /** Desde esta edad se retira tambien lo que exige bajar al suelo y levantarse. */
    public static final int EDAD_SIN_SUELO = 70;

    /** Desde esta edad se retira ademas la carga axial y los invertidos. */
    public static final int EDAD_SIN_CARGA_AXIAL = 65;

    public static SafetyProfile forAge(int age) {
        return forAgeAndLevel(age, null);
    }

    /**
     * Lo que el participante declara que NO puede hacer. Son las cinco columnas
     * allow_* que V25 anadio a user_profiles el 28/09 y que Java no leia.
     * <p>
     * V25 fija la regla, literal: las declaraciones "se suman a la politica
     * automatica; nunca la relajan". Por eso aqui se combinan con OR y no hay
     * forma de que un allow_* en true vuelva a admitir lo que la edad retiro.
     * <p>
     * Un true en cada campo significa PUEDE. Ausente -null- se interpreta como
     * puede, que es el DEFAULT TRUE de la migracion.
     */
    public record Declaradas(
            Boolean allowHighImpact,
            Boolean allowFloorExercises,
            Boolean allowKneelingExercises,
            Boolean allowHangingExercises,
            Boolean allowOverheadExercises
    ) {
        public static Declaradas sinLimitaciones() {
            return new Declaradas(true, true, true, true, true);
        }

        private static boolean prohibe(Boolean permite) {
            return Boolean.FALSE.equals(permite);
        }
    }

    /**
     * La politica por edad y nivel MAS lo que el participante declaro.
     * <p>
     * requires_inversion no tiene columna allow_* en V25 ni corte propio por
     * edad: se retira con la misma edad que la carga axial, porque es la
     * restriccion documentada mas cercana. Queda declarado como criterio de
     * diseno, no como dato del perfil.
     */
    public static SafetyProfile forAgeLevelAndDeclared(int age, String fitnessLevel,
                                                       Declaradas declaradas) {
        var d = declaradas == null ? Declaradas.sinLimitaciones() : declaradas;
        var politica = forAgeAndLevel(age, fitnessLevel);
        return new SafetyProfile(
                politica.excludeHighImpact() || Declaradas.prohibe(d.allowHighImpact()),
                politica.excludeFloorWork()  || Declaradas.prohibe(d.allowFloorExercises()),
                politica.excludeAxialLoad(),
                Declaradas.prohibe(d.allowKneelingExercises()),
                Declaradas.prohibe(d.allowHangingExercises()),
                Declaradas.prohibe(d.allowOverheadExercises()),
                age >= EDAD_SIN_CARGA_AXIAL);
    }

    /**
     * Restricciones por edad y, desde el plan 30, tambien por nivel: un
     * principiante no recibe saltos ni pliometria (high_impact), entrene donde
     * entrene. El plan 30 abrio la semana de una principiante con "Salto
     * profundo desde flexion inclinada", que el catalogo marca como alto impacto
     * y hasta ahora solo se filtraba a partir de los 60 anos.
     * <p>
     * Criterio de diseno, no dato del catalogo: ACSM 2009 recomienda introducir
     * pliometria cuando ya existe una base de fuerza. El suelo y la carga axial
     * siguen dependiendo solo de la edad.
     */
    public static SafetyProfile forAgeAndLevel(int age, String fitnessLevel) {
        return new SafetyProfile(
                age >= EDAD_SIN_IMPACTO || "BEGINNER".equals(fitnessLevel),
                age >= EDAD_SIN_SUELO,
                age >= EDAD_SIN_CARGA_AXIAL,
                false, false, false,
                age >= EDAD_SIN_CARGA_AXIAL);
    }

    public static SafetyProfile none() {
        return new SafetyProfile(false, false, false, false, false, false, false);
    }

    public boolean hasRestrictions() {
        return excludeHighImpact || excludeFloorWork || excludeAxialLoad
                || excludeKneeling || excludeHanging || excludeOverhead
                || excludeInversion;
    }

    /**
     * Texto para el prompt. El filtro ya retiro los ejercicios, pero decirselo
     * a la IA evita que proponga variantes agresivas de los que si quedan: un
     * "wall sit" profundo o una sentadilla completa siguen siendo elegibles y
     * el modelo puede moderar la prescripcion.
     */
    public String describe() {
        if (!hasRestrictions()) return null;
        var partes = new StringBuilder("El participante tiene restricciones de seguridad. ");
        if (excludeHighImpact)
            partes.append("Evita cualquier movimiento explosivo o de impacto, aunque el ejercicio lo permita. ");
        if (excludeAxialLoad)
            partes.append("Evita cargar peso sobre la columna y el cuello. ");
        if (excludeInversion)
            partes.append("Evita las posiciones con la cabeza por debajo del corazon. ");
        if (excludeKneeling)
            partes.append("Evita apoyar las rodillas en el suelo. ");
        if (excludeHanging)
            partes.append("Evita suspenderse de una barra. ");
        if (excludeOverhead)
            partes.append("Evita llevar los brazos por encima de la cabeza. ");
        if (excludeFloorWork)
            partes.append("Evita ejercicios que obliguen a bajar al suelo y levantarse. ");
        partes.append("Prefiere rangos de movimiento moderados y progresion conservadora.");
        return partes.toString();
    }
}