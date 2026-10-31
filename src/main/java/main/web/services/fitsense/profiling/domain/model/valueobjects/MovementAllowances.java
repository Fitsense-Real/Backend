package main.web.services.fitsense.profiling.domain.model.valueobjects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Los cinco movimientos que el participante declara que PUEDE hacer.
 * <p>
 * Las columnas existen desde V25 con {@code NOT NULL DEFAULT TRUE}, pero hasta
 * hoy no las leia nadie: el perfil no tenia los campos, el recurso REST no los
 * admitia y {@code SafetyProfile.forAgeLevelAndDeclared} —que si existia— nunca
 * se llamaba. Resultado: <b>toda restriccion que el participante declarara se
 * perdia antes de llegar al filtro</b>, y el plan podia incluir justo lo que
 * dijo que no puede hacer. Es el defecto INC-4C-03.
 * <p>
 * SON PERMISOS, NO PROHIBICIONES: {@code true} = puede hacerlo. Se eligio asi
 * en V25 y se respeta aqui para que la columna, el campo y el JSON digan lo
 * mismo y nadie tenga que invertir el sentido mentalmente.
 * <p>
 * La politica por edad y nivel sigue mandando por separado y se COMBINA con
 * esto en {@code SafetyProfile}: basta que uno de los dos lo excluya para que
 * el ejercicio no entre. Declarar {@code true} no levanta una restriccion de
 * edad.
 */
@Embeddable
public record MovementAllowances(
        @Column(name = "allow_high_impact", nullable = false) boolean allowHighImpact,
        @Column(name = "allow_floor_exercises", nullable = false) boolean allowFloorExercises,
        @Column(name = "allow_kneeling_exercises", nullable = false) boolean allowKneelingExercises,
        @Column(name = "allow_hanging_exercises", nullable = false) boolean allowHangingExercises,
        @Column(name = "allow_overhead_exercises", nullable = false) boolean allowOverheadExercises
) {

    /** Lo que V25 pone por omision: ningun perfil pierde ejercicios por esta via. */
    public static MovementAllowances sinLimitaciones() {
        return new MovementAllowances(true, true, true, true, true);
    }

    /**
     * AL CREAR el perfil: lo que no se declara se admite, igual que el
     * {@code DEFAULT TRUE} de la columna. Un cliente que todavia no conoce
     * estos campos crea el perfil como siempre.
     */
    public static MovementAllowances declaradas(Boolean altoImpacto, Boolean suelo,
                                                Boolean arrodillarse, Boolean suspension,
                                                Boolean porEncimaDeLaCabeza) {
        return new MovementAllowances(
                noEsFalso(altoImpacto), noEsFalso(suelo), noEsFalso(arrodillarse),
                noEsFalso(suspension), noEsFalso(porEncimaDeLaCabeza));
    }

    /**
     * AL ACTUALIZAR: lo que no se declara <b>se conserva</b>, no se reinicia.
     * <p>
     * La asimetria con {@link #declaradas} es deliberada y es de seguridad: el
     * resto del perfil se reemplaza entero, pero si estos campos siguieran esa
     * regla, un cliente antiguo que envia el formulario sin ellos BORRARIA una
     * restriccion que el participante habia declarado, sin que nadie lo note.
     * Perder una restriccion por omision no es un error recuperable.
     */
    public MovementAllowances conCambios(Boolean altoImpacto, Boolean suelo,
                                         Boolean arrodillarse, Boolean suspension,
                                         Boolean porEncimaDeLaCabeza) {
        return new MovementAllowances(
                altoImpacto         == null ? allowHighImpact        : altoImpacto,
                suelo               == null ? allowFloorExercises    : suelo,
                arrodillarse        == null ? allowKneelingExercises : arrodillarse,
                suspension          == null ? allowHangingExercises  : suspension,
                porEncimaDeLaCabeza == null ? allowOverheadExercises : porEncimaDeLaCabeza);
    }

    /** true si el participante declaro alguna limitacion. Para el log y las pruebas. */
    public boolean algunaLimitacion() {
        return !(allowHighImpact && allowFloorExercises && allowKneelingExercises
                && allowHangingExercises && allowOverheadExercises);
    }

    private static boolean noEsFalso(Boolean permite) {
        return !Boolean.FALSE.equals(permite);
    }
}
