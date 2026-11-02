package main.web.services.fitsense.catalog.domain.model.aggregates;

import jakarta.persistence.*;
import lombok.Getter;
import main.web.services.fitsense.catalog.domain.model.valueobjects.PrescriptionType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Catalogo de solo lectura. Se importa una vez y no se edita nunca desde la API:
 * por eso no extiende de los agregados auditables (no hay escritura que auditar)
 * y created_at se mapea como no insertable.
 * <p>
 * Solo el subconjunto con is_active = true es elegible por el generador.
 */
@Getter
@Entity
@Table(name = "exercises")
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exercise_id")
    private Long id;

    @Column(name = "source_code", nullable = false, length = 20)
    private String sourceCode;

    @Column(name = "name_en", nullable = false)
    private String nameEn;

    @Column(name = "name_es")
    private String nameEs;

    @Column(name = "body_part_id", nullable = false)
    private Short bodyPartId;

    @Column(name = "equipment_id", nullable = false)
    private Short equipmentId;

    @Column(name = "target_muscle", nullable = false, length = 60)
    private String targetMuscle;

    @Column(name = "synergist_muscle", length = 60)
    private String synergistMuscle;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "secondary_muscles", columnDefinition = "varchar(60)[]")
    private String[] secondaryMuscles;

    @Column(name = "instructions_es")
    private String instructionsEs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "instruction_steps_es", columnDefinition = "jsonb")
    private String instructionStepsEs;

    @Column(name = "instructions_en")
    private String instructionsEn;

    @Column(name = "image_path")
    private String imagePath;

    @Column(name = "gif_path")
    private String gifPath;

    /** Credito obligatorio de las imagenes y GIF. Se devuelve siempre en la API. */
    @Column(name = "media_attribution", length = 200)
    private String mediaAttribution;

    @Column(name = "difficulty_level", nullable = false)
    private Short difficultyLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_prescription", nullable = false, length = 25)
    private PrescriptionType defaultPrescription;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    /**
     * Banderas de seguridad de V10. Se filtran en la consulta del conjunto
     * elegible y no como instruccion en el prompt: cuando la consecuencia es una
     * lesion, no se confia en que el modelo obedezca un texto.
     */
    @Column(name = "high_impact", nullable = false)
    private boolean highImpact;

    /** Exige bajar al suelo y levantarse. Es barrera de movilidad, no de fuerza. */
    @Column(name = "requires_floor", nullable = false)
    private boolean requiresFloor;

    /** Carga sobre columna o cuello. La posicion invertida va aparte, en requiresInversion. */
    @Column(name = "axial_load", nullable = false)
    private boolean axialLoad;

    /*
     * Banderas de V25. Existian en la tabla desde el 28/09 y este agregado no
     * las mapeaba, asi que la consulta del conjunto elegible no podia filtrarlas
     * y el participante recibia ejercicios que habia declarado no poder hacer.
     *
     * requiresInversion se separa de axialLoad a proposito: el comentario de
     * GetEligibleExercisesQuery prometia que excludeAxialLoad retiraba "carga
     * axial e invertidos", y el SQL solo comprobaba axial_load. Medido sobre el
     * catalogo activo: de los 33 ejercicios con requires_inversion, NINGUNO
     * tiene axial_load, y 28 no los retira ningun otro filtro.
     */

    /** Exige apoyar las rodillas en el suelo. Restriccion declarada por el perfil. */
    @Column(name = "requires_kneeling", nullable = false)
    private boolean requiresKneeling;

    /** Exige suspenderse de una barra o anillas. Restriccion declarada por el perfil. */
    @Column(name = "requires_hanging", nullable = false)
    private boolean requiresHanging;

    /** Exige llevar los brazos por encima de la cabeza. Restriccion declarada por el perfil. */
    @Column(name = "requires_overhead", nullable = false)
    private boolean requiresOverhead;

    /** Posicion con la cabeza por debajo del corazon (declinado, invertido). */
    @Column(name = "requires_inversion", nullable = false)
    private boolean requiresInversion;

    /*
     * Clasificacion del Step 3A, migracion V26. Mismo caso que las banderas de
     * V25: las columnas estaban en la tabla desde el 22/09 y este agregado no
     * las mapeaba, asi que ni la cobertura de 4C ni la distincion compuesto /
     * aislamiento podian usarse desde la aplicacion.
     *
     * SOLO LECTURA. insertable = false, updatable = false: quien escribe estas
     * dos columnas es la migracion V26, con su procedencia por etiqueta
     * (HUMANO_RONDA_D3.1, HUMANO_RONDA_D3.2, DETECTOR_D3.2_VERIFICADO,
     * HUMANO_READJUDICADO) y su concordancia medida. La aplicacion no las
     * recalcula ni las corrige: si una etiqueta esta mal, se abre una migracion,
     * no un UPDATE desde el backend.
     */

    /**
     * Patron principal de movimiento, una sola clase por ejercicio. 24 clases
     * cerradas, definidas en reglas-patrones-v2.md y con CHECK en V26
     * ({@code ck_exercises_movement_pattern}).
     * <p>
     * {@code NULL} solo en ejercicios inactivos: V26 anade
     * {@code ck_exercises_activo_clasificado}, que obliga a que todo activo
     * tenga patron. El conjunto elegible solo trae activos, asi que en
     * planificacion no deberia llegar nunca nulo; se trata como nulo posible de
     * todas formas, porque una restriccion de base no es una excusa para que el
     * codigo reviente si cambia.
     */
    @Column(name = "movement_pattern", length = 50, insertable = false, updatable = false)
    private String movementPattern;

    /**
     * TRUE si el ejercicio mueve una sola articulacion. En tronco, movilidad,
     * equilibrio y OTHER no aplica y V26 guarda FALSE (reglas-patrones-v2.md
     * §8). No lo usa la cobertura 4C; se mapea aqui porque es la otra mitad del
     * Step 3A y dejarla sin mapear invita a repetir este mismo hallazgo.
     */
    @Column(name = "is_isolation", insertable = false, updatable = false)
    private Boolean isolation;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Exercise() {
        // JPA
    }

    public List<String> secondaryMusclesAsList() {
        return secondaryMuscles == null ? List.of() : Arrays.asList(secondaryMuscles);
    }

    /** Nombre para mostrar. Cae al ingles si la traduccion falta (solo posible si no esta activo). */
    public String displayName() {
        return nameEs != null ? nameEs : nameEn;
    }
}
