package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.AdjustmentType;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.ConservationReport;
import main.web.services.fitsense.planning.domain.model.valueobjects.DerivedWorkout;
import main.web.services.fitsense.planning.domain.model.valueobjects.MandatorySlot;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.PreviousWeekSummary;
import main.web.services.fitsense.planning.domain.model.valueobjects.WeekDerivation;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * INC-P2D-01 · RETIRAR UNA COLOCACION DE RELLENO SE AUTORIZA; RETIRAR UNA
 * ESTRUCTURAL, NO. Y AL RETIRAR, LO QUE QUEDA SE CONSERVA TAL CUAL.
 *
 * <h2>Por que existe</h2>
 *
 * Con {@code GAIN_MUSCLE} y 4D en {@code ENFORCE},
 * {@code ACORTAR_O_REORGANIZAR} se quedaba sin NINGUNA operacion que redujera el
 * trabajo de la sesion: V8C le concede cambiar la dosis
 * ({@code permiteCambiarDosis = true}) y V24 se lo retira al exigir la dosis
 * exacta de 4D. Se descubrio ejecutando el modelo de verdad, que se abstuvo y
 * declaro que no era posible: tenia razon.
 *
 * <h2>Por que V24 no bastaba para proteger lo estructural</h2>
 *
 * LA CLASE DE DOSIS DEPENDE DEL SLOT DEL EJERCICIO, NO DE SU POSICION EN LA
 * SESION. {@code MvpDosePolicy} decide con
 * {@code relleno = MandatorySlot.of(candidato).isEmpty()} (su linea 260), y el
 * slot sale del {@code movement_pattern} del catalogo. Al retirar una colocacion
 * estructural, la de relleno que queda <b>NO</b> pasa a estructural: su dosis
 * exigible no cambia, sigue siendo valida, y V24 no se entera de que la semana
 * perdio un patron obligatorio.
 * <p>
 * De ahi {@link #retirarEstructuralSeRechaza()}: un estructural retirado cuyas
 * dosis restantes son validas, que V24 aceptaria y V8C tiene que rechazar.
 *
 * <h2>ALCANCE: ESTO PRUEBA V8C, NO QUE LA SEMANA SEA PUBLICABLE</h2>
 *
 * Las pruebas de esta clase ejercitan {@code ConservationValidator} y nada mas.
 * Un verde demuestra que el permiso nuevo autoriza y prohibe lo que debe, <b>no
 * que una sesion acortada pase V24 ni el resto de {@code PlanDraftValidator}</b>.
 * Eso es una prueba aparte, con una semana valida y las validaciones estrictas de
 * produccion.
 *
 * <h2>Dominio puro</h2>
 *
 * Sin Spring, sin base de datos y sin Replicate: {@code ConservationValidator} no
 * tiene constructor explicito y no depende de nada. Esta clase habria encontrado
 * INC-P2D-01 sin gastar una sola peticion de pago.
 */
@DisplayName("V8C · retirar relleno si, estructural no, y sin tocar lo que queda (INC-P2D-01)")
class ConservationValidatorQuitarRellenoTest {

    private final ConservationValidator v8c = new ConservationValidator();

    private static final long SESION = 501L;
    private static final LocalDate DIA_ANTERIOR = LocalDate.of(2026, 11, 2);
    private static final LocalDate DIA_NUEVO = DIA_ANTERIOR.plusWeeks(1);

    /** ESTRUCTURALES: su patron mapea a un MandatorySlot. */
    private static final long ESTRUCTURAL_A = 10L;
    private static final long ESTRUCTURAL_B = 11L;
    /** RELLENOS: su patron NO mapea a ningun MandatorySlot. Hacen falta DOS. */
    private static final long RELLENO_A = 20L;
    private static final long RELLENO_B = 21L;
    /** Existe en el catalogo, pero no en la semana anterior: sirve para «anadir». */
    private static final long AJENO = 99L;

    /** La semana anterior: dos estructurales y dos rellenos. */
    private static final List<Long> ANTERIORES =
            List.of(ESTRUCTURAL_A, ESTRUCTURAL_B, RELLENO_A, RELLENO_B);

    // =======================================================================
    // Lo que se autoriza
    // =======================================================================

    @Test
    @DisplayName("Retirar UNA colocacion de relleno queda autorizado")
    void retirarRellenoSeAutoriza() {
        // Se va RELLENO_A y quedan los otros tres con su dosis intacta.
        var informe = validar(List.of(ESTRUCTURAL_A, ESTRUCTURAL_B, RELLENO_B));

        assertTrue(informe.problems().isEmpty(),
                "retirar una colocacion de relleno tenia que quedar autorizado y V8C"
                        + " objeto: " + informe.problems());

        assertEquals(List.of(SESION), informe.aplicadas(),
                "la sesion tenia que contar como ADAPTADA: se le quito trabajo de"
                        + " verdad. Informe: " + informe.describe());
    }

    @Test
    @DisplayName("Cambiar SOLO la dosis, sin retirar nada, sigue autorizado como antes")
    void cambiarSoloDosisSigueAutorizado() {
        // ACORTAR_O_REORGANIZAR lleva permiteCambiarDosis = true, y el parche NO
        // se lo quita. Sin retirada, revisarSustitucion ni se llama: la
        // congelacion de dosis se exige solo en combinacion con una retirada.
        var informe = validar(ANTERIORES, TipoIntervencion.ACORTAR_O_REORGANIZAR,
                ESTRUCTURAL_B);

        assertTrue(informe.problems().isEmpty(),
                "recortar la dosis sin retirar nada tenia que seguir autorizado y V8C"
                        + " objeto: " + informe.problems());
    }

    // =======================================================================
    // Lo que NO se autoriza
    // =======================================================================

    @Test
    @DisplayName("Retirar una colocacion ESTRUCTURAL se rechaza, aunque las dosis que quedan sean validas")
    void retirarEstructuralSeRechaza() {
        // Se va ESTRUCTURAL_B. Quedan un estructural y dos rellenos, cada uno con
        // la dosis que le corresponde por su slot: V24 no tendria nada que
        // objetar. Es el caso que demuestra por que la comprobacion vive en V8C.
        var informe = validar(List.of(ESTRUCTURAL_A, RELLENO_A, RELLENO_B));

        assertFalse(informe.problems().isEmpty(),
                "retirar una colocacion estructural tenia que rechazarse");

        assertTrue(unido(informe.problems()).contains("ESTRUCTURAL"),
                "lo rechazo algo que no es la regla de relleno. Problemas: "
                        + informe.problems());

        assertTrue(unido(informe.problems()).contains(String.valueOf(ESTRUCTURAL_B)),
                "el mensaje tiene que decir QUE ejercicio se intento retirar."
                        + " Problemas: " + informe.problems());

        assertFalse(informe.aplicadas().contains(SESION),
                "una sesion rechazada no puede constar como adaptada");
    }

    @Test
    @DisplayName("Retirar DOS colocaciones de relleno se rechaza: el tope es una")
    void retirarDosRellenosSeRechaza() {
        // LOS DOS RETIRADOS SON DE RELLENO DE VERDAD -RELLENO_A y RELLENO_B-, que
        // es el caso que esta prueba anuncia. La version anterior de esta clase
        // retiraba un estructural y un relleno, asi que comprobaba el tope pero
        // no lo que decia su nombre.
        var informe = validar(List.of(ESTRUCTURAL_A, ESTRUCTURAL_B));

        assertFalse(informe.problems().isEmpty(), "retirar dos tenia que rechazarse");
        assertTrue(unido(informe.problems()).contains("UNA sola colocacion"),
                "el motivo tenia que ser el tope de una retirada, no el tipo."
                        + " Problemas: " + informe.problems());
    }

    @Test
    @DisplayName("Retirar un estructural y un relleno a la vez se rechaza por el tope")
    void retirarMixtoSeRechazaPorElTope() {
        // Se van ESTRUCTURAL_B y RELLENO_B. El tope se comprueba ANTES de
        // clasificar, asi que el motivo es la cantidad. Queda escrito para que
        // nadie lo lea como «el estructural colo».
        var informe = validar(List.of(ESTRUCTURAL_A, RELLENO_A));

        assertFalse(informe.problems().isEmpty(), "retirar dos tenia que rechazarse");
        assertTrue(unido(informe.problems()).contains("UNA sola colocacion"),
                "el tope se evalua primero. Problemas: " + informe.problems());
    }

    @Test
    @DisplayName("Retirar un relleno Y ADEMAS cambiar la dosis de otro se rechaza")
    void retirarYCambiarDosisSeRechaza() {
        // Se va RELLENO_A y se recorta la dosis de ESTRUCTURAL_B. Es la condicion
        // que faltaba en la primera version del parche: al retirar, lo que queda
        // se conserva tal cual.
        var informe = validar(List.of(ESTRUCTURAL_A, ESTRUCTURAL_B, RELLENO_B),
                TipoIntervencion.ACORTAR_O_REORGANIZAR, ESTRUCTURAL_B);

        assertFalse(informe.problems().isEmpty(),
                "retirar y ademas cambiar la dosis tenia que rechazarse");
        assertTrue(unido(informe.problems()).contains("se conservan tal cual"),
                "el motivo tenia que ser la combinacion de retirada y dosis."
                        + " Problemas: " + informe.problems());
    }

    @Test
    @DisplayName("Retirar y ADEMAS anadir se rechaza: anadir nunca se autorizo")
    void retirarYAnadirSeRechaza() {
        // Se va RELLENO_A y entra AJENO. El permiso es para RETIRAR, no para
        // sustituir con otro nombre.
        var informe = validar(List.of(ESTRUCTURAL_A, ESTRUCTURAL_B, RELLENO_B, AJENO));

        assertFalse(informe.problems().isEmpty(), "anadir tenia que rechazarse");
        assertTrue(unido(informe.problems()).contains("no autoriza sustituir"),
                "el motivo tenia que ser el de siempre, sin cambios. Problemas: "
                        + informe.problems());
    }

    // =======================================================================
    // Que las otras seis intervenciones no cambiaron
    // =======================================================================

    @Test
    @DisplayName("Con REVISAR_CARGA_Y_RECUPERACION retirar sigue prohibido")
    void otraIntervencionNoGanaElPermiso() {
        // Tambien lleva permiteSustituirEjercicio = false, asi que es la que
        // podria haberse contagiado del permiso nuevo si se hubiera implementado
        // como bandera del constructor en vez de un derivado.
        var informe = validar(List.of(ESTRUCTURAL_A, ESTRUCTURAL_B, RELLENO_B),
                TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION, null);

        assertFalse(informe.problems().isEmpty(),
                "solo ACORTAR_O_REORGANIZAR tiene el permiso nuevo");
        assertTrue(unido(informe.problems()).contains("no autoriza sustituir"),
                "tenia que recibir el mensaje de antes, intacto. Problemas: "
                        + informe.problems());
    }

    @Test
    @DisplayName("El permiso es de ACORTAR_O_REORGANIZAR y de ninguna otra")
    void soloUnaIntervencionLoTiene() {
        for (var tipo : TipoIntervencion.values())
            assertEquals(tipo == TipoIntervencion.ACORTAR_O_REORGANIZAR,
                    tipo.permiteQuitarEjercicio(),
                    "permiteQuitarEjercicio() mal para " + tipo);
    }

    /**
     * La premisa de la que depende toda la clase: el criterio de relleno es el de
     * {@code MvpDosePolicy}, {@code MandatorySlot.of(x).isEmpty()}. Si el catalogo
     * de prueba no cumpliera esto, las demas pruebas pasarian por el motivo
     * equivocado.
     */
    @Test
    @DisplayName("La premisa: el catalogo de prueba clasifica como se espera")
    void laPremisaDelCatalogo() {
        var catalogo = catalogo();
        for (long id : List.of(ESTRUCTURAL_A, ESTRUCTURAL_B))
            assertTrue(MandatorySlot.of(catalogo.get(id)).isPresent(),
                    id + " tenia que ser estructural");
        for (long id : List.of(RELLENO_A, RELLENO_B))
            assertTrue(MandatorySlot.of(catalogo.get(id)).isEmpty(),
                    id + " tenia que ser de relleno");
    }

    // =======================================================================
    // El material
    // =======================================================================

    private ConservationReport validar(List<Long> derivados) {
        return validar(derivados, TipoIntervencion.ACORTAR_O_REORGANIZAR, null);
    }

    /**
     * @param derivados        los ejercicios que quedan en la semana derivada
     * @param conDosisCambiada el ejercicio al que se le recorta una serie, o
     *                         {@code null} si todas las dosis se conservan
     */
    private ConservationReport validar(List<Long> derivados, TipoIntervencion intervencion,
                                       Long conDosisCambiada) {
        return v8c.validate(derivacion(derivados, conDosisCambiada), semanaAnterior(),
                orden(intervencion), catalogo());
    }

    /**
     * La semana derivada: la MISMA sesion, el MISMO dia desplazado una semana, el
     * MISMO enfoque y los MISMOS minutos declarados. Lo unico que cambia es la
     * lista de ejercicios -y, si se pide, la dosis de uno-, para que el motivo de
     * un rechazo no pueda ser otra cosa.
     */
    private WeekDerivation derivacion(List<Long> ejercicios, Long conDosisCambiada) {
        var draft = new ArrayList<PlanDraft.DraftExercise>();
        for (Long id : ejercicios)
            draft.add(dosisDe(id, id.equals(conDosisCambiada)));

        return new WeekDerivation(List.of(new DerivedWorkout(SESION,
                new PlanDraft.DraftWorkout(DIA_NUEVO, WorkoutFocus.FULL_BODY,
                        "FULL_BODY - Sesion 1", 45, List.copyOf(draft)))), List.of());
    }

    /**
     * Cada ejercicio conserva su dosis entre semanas: el estructural 3x10 y el
     * relleno 2x10, como pide 4D para GAIN_MUSCLE. Con {@code recortada} se le
     * quita una serie, que es lo que hace que aparezca en {@code dosisCambiada}.
     */
    private PlanDraft.DraftExercise dosisDe(Long id, boolean recortada) {
        short series = seriesDe(id);
        if (recortada) series--;
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS,
                series, (short) 10, null, null, (short) 90, null);
    }

    private static short seriesDe(Long id) {
        return id == RELLENO_A || id == RELLENO_B ? (short) 2 : (short) 3;
    }

    private PreviousWeekSummary semanaAnterior() {
        var sesion = new PreviousWeekSummary.WorkoutOutcome(
                SESION, DIA_ANTERIOR, "FULL_BODY", "COMPLETED", null,
                true, "COMPLETED", (short) 7, new BigDecimal("100.00"),
                "FULL_BODY - Sesion 1", (short) 45);

        var prescripciones = new ArrayList<PreviousWeekSummary.PrescriptionOutcome>();
        for (Long id : ANTERIORES) {
            short series = seriesDe(id);
            prescripciones.add(new PreviousWeekSummary.PrescriptionOutcome(
                    SESION, DIA_ANTERIOR, id, "Ejercicio " + id, "SETS_REPS",
                    series, (short) 10, null, null,
                    series, 10 * series, null, null,
                    new BigDecimal("100.00"), "COMPLETED", null, "COMPLETED",
                    (short) 90));
        }

        return new PreviousWeekSummary(new BigDecimal("42.00"), new BigDecimal("7.0"),
                null, Map.of(), List.of(sesion), prescripciones, List.of());
    }

    /** Una orden que senala ESA sesion, con la intervencion que se quiera probar. */
    private PlanAdjustment orden(TipoIntervencion intervencion) {
        return new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null,
                0.0, 0.0, null, null, null, "falta de tiempo", null, null,
                List.of(new PlanAdjustment.TargetedSession(SESION, intervencion)));
    }

    /**
     * El conjunto elegible. Los patrones son los de {@code MandatorySlot}:
     * {@code KNEE_DOMINANT} y {@code HORIZONTAL_PUSH} mapean a un slot;
     * {@code CARRY} y {@code ISOMETRIC_HOLD} no mapean a ninguno, asi que son
     * relleno. Lo comprueba {@link #laPremisaDelCatalogo()}.
     */
    private Map<Long, CandidateExercise> catalogo() {
        var porId = new LinkedHashMap<Long, CandidateExercise>();
        porId.put(ESTRUCTURAL_A, new CandidateExercise(ESTRUCTURAL_A, "Sentadilla",
                "upper legs", "BODY_WEIGHT", 2, PrescriptionType.SETS_REPS,
                "quadriceps", "KNEE_DOMINANT"));
        porId.put(ESTRUCTURAL_B, new CandidateExercise(ESTRUCTURAL_B, "Flexion",
                "chest", "BODY_WEIGHT", 2, PrescriptionType.SETS_REPS,
                "pectorals", "HORIZONTAL_PUSH"));
        porId.put(RELLENO_A, new CandidateExercise(RELLENO_A, "Paseo del granjero",
                "waist", "BODY_WEIGHT", 2, PrescriptionType.SETS_REPS,
                "abs", "CARRY"));
        porId.put(RELLENO_B, new CandidateExercise(RELLENO_B, "Plancha",
                "waist", "BODY_WEIGHT", 2, PrescriptionType.SETS_REPS,
                "abs", "ISOMETRIC_HOLD"));
        porId.put(AJENO, new CandidateExercise(AJENO, "Remo", "back", "BODY_WEIGHT",
                2, PrescriptionType.SETS_REPS, "upper back", "HORIZONTAL_PULL"));
        return porId;
    }

    private static String unido(List<String> problemas) {
        return String.join(" | ", problemas);
    }
}
