package main.web.services.fitsense.catalog.domain.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;

import java.io.Serializable;
import java.util.Objects;

/**
 * Requisito de equipamiento de un ejercicio. Tabla de V23, poblada por V23 y
 * corregida por V24 con los 214 ejercicios revisados a mano.
 * <p>
 * EXISTIA EN LA BASE Y NO EN JAVA. Por eso el conjunto elegible filtraba por
 * exercises.equipment_id, el implemento PRINCIPAL, del que la propia V23 dice
 * "ya no filtra nada".
 * <p>
 * SEMANTICA, confirmada sobre los datos: grupos DISTINTOS se combinan con AND
 * -hacen falta todos-; los implementos del MISMO grupo se combinan con OR
 * -basta uno-; un ejercicio sin filas no exige nada y es peso corporal.
 * Sobre las 560 filas de los cuatro slots obligatorios, equipment_id = 1
 * ('body weight') no aparece ni una vez como requisito, lo que confirma que
 * "sin filas" es la forma de decir peso corporal.
 * <p>
 * Solo lectura, igual que el resto del catalogo: se importa por migracion y no
 * se edita desde la API.
 */
@Getter
@Entity
@Table(name = "exercise_equipment_requirements")
@IdClass(ExerciseEquipmentRequirement.Key.class)
public class ExerciseEquipmentRequirement {

    @Id
    @Column(name = "exercise_id", nullable = false)
    private Long exerciseId;

    /** Los grupos se combinan con AND. Empieza en 1 (ck_eer_group_positive). */
    @Id
    @Column(name = "group_no", nullable = false)
    private Short groupNo;

    /** Dentro de un mismo grupo, las opciones se combinan con OR. */
    @Id
    @Column(name = "equipment_id", nullable = false)
    private Short equipmentId;

    protected ExerciseEquipmentRequirement() {
        // JPA
    }

    /** Clave compuesta: coincide con PRIMARY KEY (exercise_id, group_no, equipment_id). */
    @Getter
    public static class Key implements Serializable {
        private Long exerciseId;
        private Short groupNo;
        private Short equipmentId;

        public Key() {
        }

        public Key(Long exerciseId, Short groupNo, Short equipmentId) {
            this.exerciseId = exerciseId;
            this.groupNo = groupNo;
            this.equipmentId = equipmentId;
        }

        @Override
        public boolean equals(Object otro) {
            if (this == otro) return true;
            if (!(otro instanceof Key clave)) return false;
            return Objects.equals(exerciseId, clave.exerciseId)
                    && Objects.equals(groupNo, clave.groupNo)
                    && Objects.equals(equipmentId, clave.equipmentId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(exerciseId, groupNo, equipmentId);
        }
    }
}
