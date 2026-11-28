package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.ConservationReport;
import main.web.services.fitsense.planning.domain.model.valueobjects.GenerationSource;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningProfile;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.SafetyProfile;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * INC-P2D-01 · UNA SEMANA ACORTADA ES PUBLICABLE, no solo autorizada por V8C.
 *
 * <h2>Lo que anade a {@code ConservationValidatorQuitarRellenoTest}</h2>
 *
 * Aquella clase demuestra que V8C autoriza retirar una colocacion de relleno y
 * prohibe retirar una estructural. <b>No demuestra que la semana resultante pase
 * la validacion estricta</b>: un permiso de V8C no sirve de nada si despues V24,
 * V17 o V20 rechazan la semana.
 *
 * <h2>TRES ERRORES DE LA PRIMERA VERSION, Y COMO SE CORRIGEN</h2>
 *
 * <ol>
 *   <li><b>V20 estaba apagada.</b> El contexto llevaba
 *       {@code PrescriptionParams = null}, y
 *       {@code validateMinimumDurationAndRest} empieza con
 *       {@code if (prescription == null) return;}. Asi que la prueba que decia
 *       comprobar el rechazo de V20 no podia: habria salido roja por no encontrar
 *       ningun problema. Ahora los parametros van NO nulos, con todos sus
 *       componentes a {@code null} para usar los valores por omision, que estan
 *       guardados contra nulo en los dos sitios que los leen.
 *   <li><b>La cobertura 4C es SEMANAL, no diaria.</b>
 *       {@code SlotCoveragePolicy.ofDraft(...)} suma el borrador entero, asi que
 *       quitar el patron de rodilla solo del lunes no rompe nada: el miercoles y
 *       el viernes lo siguen cubriendo. Ahora se quita de LAS TRES sesiones.
 *   <li><b>La duracion se calculaba con la formula equivocada.</b> V24 usa el
 *       coste de 4D; V17 y V20 usan {@link SessionDurationEstimator}, que ademas
 *       suma calentamiento y transiciones y cuenta el descanso solo ENTRE series.
 *       No son el mismo numero.
 * </ol>
 *
 * <h2>La aritmetica, con la formula del estimador</h2>
 *
 * {@code segundos = calentamiento + Σ[trabajo + (series−1) × descanso]
 * + (n−1) × transicion}, con los valores por omision: calentamiento 5 min,
 * {@code secondsPerRep} 3 s, transicion 60 s.
 * <ul>
 *   <li>estructural 3x10/90 → {@code 90 + 180 = 270 s};
 *   <li>relleno 2x10/90 → {@code 60 + 90 = 150 s}.
 * </ul>
 * Cada sesion lleva <b>cuatro estructurales y dos rellenos</b>:
 * <ul>
 *   <li><b>antes:</b> {@code 1380 + 300 + 300 = 1980 s} → <b>33 min</b>;
 *   <li><b>despues</b> de retirar un relleno:
 *       {@code 1230 + 240 + 300 = 1770 s} → <b>30 min</b>.
 * </ul>
 * El piso de V20 son <b>31 min</b> (70 % de 45, el mismo numero del hallazgo
 * real). Por eso <b>la semana original pasa sin ninguna exencion y la acortada la
 * necesita de verdad</b>, que es lo que esta clase tiene que demostrar.
 * <p>
 * Para V24 el coste de 4D es otro —estructural 6 min, relleno 4— y da 32 min
 * antes y 28 despues, los dos por debajo del presupuesto de 38,25 min (85 % de
 * 45). Seis colocaciones es exactamente el tope de 4D, no lo pasa.
 * <p>
 * LOS MINUTOS DECLARADOS NO SE INVENTAN: los calcula el propio
 * {@code SessionDurationEstimator}, que es lo que hace el motor de reglas en
 * produccion para declarar su duracion. Asi V17 no puede ser nunca el motivo de
 * un rechazo aqui.
 * <p>
 * Y {@link #laPremisaDeLaAritmetica()} comprueba esos numeros contra el estimador
 * de verdad: si cambiara, falla ahi con un mensaje claro en vez de dejar que las
 * demas pruebas pasen por el motivo equivocado.
 *
 * <h2>Alcance</h2>
 *
 * Esto ejercita {@link PlanDraftValidator} con un informe de exencion construido
 * a mano. <b>No ejercita el recorrido integrado
 * {@code ConservationValidator → AdaptedWeekProducer}</b>, donde el informe lo
 * produce V8C. Esa es la pieza siguiente.
 * <p>
 * Dominio puro: sin Spring, sin base de datos y sin Replicate.
 */
@DisplayName("INC-P2D-01 · la semana acortada pasa la validacion estricta")
class SemanaAcortadaPublicableTest {

    private static final long USUARIO = 7L;
    /** 2026-11-02 es lunes; las sesiones van lunes, miercoles y viernes. */
    private static final LocalDate LUNES = LocalDate.of(2026, 11, 2);
    private static final int SESIONES = 3;
    private static final int DIVISOR = 30;
    private static final int MINUTOS_SESION = 45;
    /** 70 % de 45, el mismo piso del hallazgo real. */
    private static final int PISO_V20 = 31;

    /** La sesion que se acorta es la del lunes. */
    private static final LocalDate DIA_ACORTADO = LUNES;

    private static final SessionDurationEstimator ESTIMADOR = new SessionDurationEstimator();

    // =======================================================================
    // La premisa, comprobada y no supuesta
    // =======================================================================

    @Test
    @DisplayName("La premisa: 6 colocaciones estiman 33 min y 5 estiman 30, con el piso en 31")
    void laPremisaDeLaAritmetica() {
        int antes = ESTIMADOR.estimateMinutes(sesion(0, LUNES, 6), parametros());
        int despues = ESTIMADOR.estimateMinutes(sesion(0, LUNES, 5), parametros());

        // IGUALDADES EXACTAS, no desigualdades: si el estimador cambiara de
        // formula, un ">=" seguiria pasando con otros numeros y el escenario
        // dejaria de ser el que el javadoc describe sin que nadie se enterara.
        assertEquals(33, antes,
                "6 colocaciones tenian que estimar 33 min: 300 de calentamiento,"
                        + " 4x270 + 2x150 de trabajo y descanso, y 5x60 de transiciones.");

        assertEquals(30, despues,
                "5 colocaciones tenian que estimar 30 min, por DEBAJO del piso: es lo"
                        + " que hace que la exencion de V20 sea necesaria y no decorativa.");

        assertEquals(PISO_V20, PlanDraftValidator.minimumSessionMinutes(contexto()),
                "el piso tenia que ser el mismo del hallazgo real: 70 % de "
                        + MINUTOS_SESION + " minutos.");

        assertTrue(antes >= PISO_V20 && despues < PISO_V20,
                "los dos numeros tienen que caer a cada lado del piso: " + antes
                        + " y " + despues + " con piso " + PISO_V20);
    }

    // =======================================================================
    // Lo que se viene a demostrar
    // =======================================================================

    @Test
    @DisplayName("La semana SIN acortar es valida sin ninguna exencion")
    void laSemanaSinAcortarEsValidaSinExencion() {
        var problemas = problemas(semana(false), ConservationReport.vacio());

        assertTrue(problemas.isEmpty(),
                "la semana sin acortar tenia que pasar sin exenciones: " + problemas);
    }

    @Test
    @DisplayName("Retirado un relleno, la semana pasa V24 en ENFORCE y V20 con la exencion")
    void laSemanaAcortadaEsPublicable() {
        var problemas = problemas(semana(true), exencion(DIA_ACORTADO));

        assertTrue(problemas.isEmpty(),
                "la semana acortada tenia que ser publicable y la validacion estricta"
                        + " objeto: " + problemas);
    }

    @Test
    @DisplayName("Sin la exencion de V20, la misma semana acortada se rechaza")
    void sinLaExencionV20Rechaza() {
        // Es la otra mitad del hallazgo de la corrida real: el piso sale del
        // PERFIL y no de lo declarado, asi que no se esquiva acortando la
        // duracion declarada. Lo unico que lo levanta es la exencion por sesion.
        var problemas = problemas(semana(true), ConservationReport.vacio());

        assertFalse(problemas.isEmpty(), "sin la exencion, V20 tenia que rechazar");
        assertTrue(unido(problemas).contains("V20"),
                "el motivo tenia que ser V20 y fue: " + problemas);
    }

    @Test
    @DisplayName("Sin el patron de rodilla en TODA la semana, 4C rechaza")
    void sinUnPatronEnTodaLaSemana4CRechaza() {
        // 4C ES SEMANAL: SlotCoveragePolicy.ofDraft suma el borrador entero, asi
        // que para perder un patron hay que quitarlo de las tres sesiones. La
        // primera version lo quitaba solo del lunes y por eso no demostraba nada.
        //
        // Las tres sesiones quedan con cinco colocaciones -28 min estimados-, por
        // debajo del piso, asi que se exime a las tres: aqui se quiere aislar 4C
        // y no mezclarlo con V20.
        var problemas = problemas(semanaSinRodilla(),
                exencion(LUNES, LUNES.plusDays(2), LUNES.plusDays(4)));

        assertFalse(problemas.isEmpty(),
                "perder un patron obligatorio en toda la semana tenia que rechazarse");
        assertTrue(unido(problemas).contains("4C") || unido(problemas).contains("obertura"),
                "el motivo tenia que ser la cobertura y fue: " + problemas);
    }

    // =======================================================================
    // El material
    // =======================================================================

    /**
     * El validador del camino de adaptacion: dosis de 4D en {@code ENFORCE} y
     * cobertura 4C en {@code ENFORCE_AI_ONLY}, que es el modo con el que corre
     * produccion. El borrador declara origen {@code AI} para que
     * {@code ENFORCE_AI_ONLY} llegue a rechazar.
     */
    private static PlanDraftValidator validador() {
        return new PlanDraftValidator(ESTIMADOR,
                SlotCoverageEnforcement.ENFORCE_AI_ONLY, DoseEnforcement.ENFORCE);
    }

    private static List<String> problemas(PlanDraft draft, ConservationReport informe) {
        try {
            validador().validate(draft, contexto(), DIVISOR, informe);
            return List.of();
        } catch (InvalidPlanDraftException e) {
            return e.problems();
        }
    }

    /** El informe que produce V8C para las sesiones autorizadas a acortarse. */
    private static ConservationReport exencion(LocalDate... fechas) {
        return new ConservationReport(List.of(), List.of(), List.of(), List.of(),
                Set.of(fechas));
    }

    // -----------------------------------------------------------------------
    // Las semanas
    // -----------------------------------------------------------------------

    /**
     * @param acortada si la sesion del lunes va con cinco colocaciones en vez de
     *                 seis, o sea sin uno de sus dos rellenos
     */
    private static PlanDraft semana(boolean acortada) {
        var sesiones = new ArrayList<PlanDraft.DraftWorkout>();
        for (int i = 0; i < SESIONES; i++) {
            LocalDate dia = LUNES.plusDays(i * 2L);
            boolean esta = acortada && dia.equals(DIA_ACORTADO);
            sesiones.add(sesion(i, dia, esta ? 5 : 6));
        }
        return borrador(List.copyOf(sesiones));
    }

    /**
     * Las TRES sesiones sin su colocacion de {@code KNEE_DOMINANT}: la semana
     * pierde el slot {@code LOWER_KNEE} de verdad.
     */
    private static PlanDraft semanaSinRodilla() {
        var sesiones = new ArrayList<PlanDraft.DraftWorkout>();
        for (int i = 0; i < SESIONES; i++) {
            LocalDate dia = LUNES.plusDays(i * 2L);
            var ejercicios = new ArrayList<PlanDraft.DraftExercise>();
            // Se salta la posicion 0, que es la de rodilla.
            for (int j = 1; j < 6; j++) ejercicios.add(prescrito(id(i, j), esRelleno(j)));
            sesiones.add(sesionCon(i, dia, List.copyOf(ejercicios)));
        }
        return borrador(List.copyOf(sesiones));
    }

    /**
     * Una sesion con {@code colocaciones} ejercicios. Los cuatro estructurales
     * van primero y los dos rellenos al final, asi que pedir cinco es
     * exactamente «sin uno de los rellenos».
     */
    private static PlanDraft.DraftWorkout sesion(int indice, LocalDate dia, int colocaciones) {
        var ejercicios = new ArrayList<PlanDraft.DraftExercise>();
        for (int j = 0; j < colocaciones; j++)
            ejercicios.add(prescrito(id(indice, j), esRelleno(j)));
        return sesionCon(indice, dia, List.copyOf(ejercicios));
    }

    /**
     * LOS MINUTOS DECLARADOS LOS CALCULA EL ESTIMADOR, igual que hace el motor de
     * reglas en produccion. Inventarlos era el tercer error de la primera
     * version: V24 y V17 no usan la misma formula.
     */
    private static PlanDraft.DraftWorkout sesionCon(int indice, LocalDate dia,
                                                    List<PlanDraft.DraftExercise> ejercicios) {
        var provisional = new PlanDraft.DraftWorkout(dia, WorkoutFocus.FULL_BODY,
                "FULL_BODY - Sesion " + (indice + 1), 1, ejercicios);
        int declarados = ESTIMADOR.estimateMinutes(provisional, parametros());

        return new PlanDraft.DraftWorkout(dia, WorkoutFocus.FULL_BODY,
                "FULL_BODY - Sesion " + (indice + 1), declarados, ejercicios);
    }

    private static PlanDraft borrador(List<PlanDraft.DraftWorkout> sesiones) {
        return new PlanDraft(GenerationSource.AI, "gpt-5-nano", "semana acortada",
                null, "INC-P2D-01", sesiones);
    }

    /** Las posiciones 4 y 5 son los dos rellenos. */
    private static boolean esRelleno(int posicion) {
        return posicion >= 4;
    }

    /**
     * La dosis que 4D EXIGE: 3x10 con 90 s para una colocacion estructural y 2x10
     * con 90 s para una de relleno. Ninguna parte corporal esta en
     * {@code ZONAS_PEQUENAS} —{@code lower legs} y {@code waist}—, asi que no hay
     * repeticiones extra.
     */
    private static PlanDraft.DraftExercise prescrito(long id, boolean relleno) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS,
                relleno ? (short) 2 : (short) 3, (short) 10, null, null,
                (short) 90, null);
    }

    // -----------------------------------------------------------------------
    // El contexto
    // -----------------------------------------------------------------------

    /** Identificadores distintos por sesion: ninguna repite ejercicio en la semana. */
    private static long id(int sesion, int posicion) {
        return 100L + sesion * 10L + posicion;
    }

    /**
     * Cuatro patrones obligatorios y dos que no mapean a ningun
     * {@code MandatorySlot}. El orden importa: {@link #esRelleno(int)} trata las
     * posiciones 4 y 5 como relleno.
     */
    private static final List<String> PATRONES = List.of(
            "KNEE_DOMINANT", "HORIZONTAL_PUSH", "HORIZONTAL_PULL", "HIP_HINGE",
            "ISOMETRIC_HOLD", "CARRY");

    /**
     * Partes corporales que satisfacen LAS DOS reglas de V15 en los tres
     * escenarios:
     * <ul>
     *   <li>FULL_BODY pide al menos dos grupos distintos de
     *       {@code [chest, back, upper legs, shoulders, waist]}, y aqui hay cuatro;
     *   <li>y <b>ningun grupo puede acaparar mas de la mitad de la sesion</b>
     *       ({@code PlanDraftValidator:417}, {@code cuantos > size() / 2}).
     * </ul>
     * ESA SEGUNDA REGLA ES LA QUE OBLIGA A {@code shoulders} EN LA POSICION 4.
     * Con {@code chest} ahi, la prueba de 4C —que retira el de rodilla y deja
     * cinco— tendria tres de pecho, y {@code 3 > 5 / 2 = 2} dispararia V15 ademas
     * de la cobertura. No invalidaria la comprobacion, porque busca el mensaje de
     * cobertura, pero añadiria un fallo que no viene al caso.
     * <p>
     * {@code shoulders} no esta en {@code ZONAS_PEQUENAS}, asi que la dosis de 4D
     * no cambia.
     */
    private static final List<String> PARTES = List.of(
            "upper legs", "chest", "back", "upper legs", "shoulders", "chest");

    /**
     * Todos los componentes a {@code null} para usar los valores por omision
     * —piso 70 %, 3 s por repeticion, transicion 60 s, calentamiento 5 min,
     * tolerancia 20 %, descanso maximo 180 s—. {@code byGoal} y
     * {@code repLimits} nulos son seguros: los dos sitios que los leen estan
     * guardados contra nulo.
     * <p>
     * LO QUE NO PUEDE SER NULO ES EL PROPIO OBJETO: con el nulo,
     * {@code validateMinimumDurationAndRest} vuelve en su primera linea y V20 y
     * V21 no se ejecutan. Era el primer error de la version anterior.
     */
    private static PrescriptionParams parametros() {
        return new PrescriptionParams(null, null, null, null, null, null,
                null, null, null, null);
    }

    private static PlanGenerationContext contexto() {
        var elegibles = new ArrayList<CandidateExercise>();
        for (int s = 0; s < SESIONES; s++)
            for (int j = 0; j < PATRONES.size(); j++)
                elegibles.add(new CandidateExercise(id(s, j), "ejercicio " + id(s, j),
                        PARTES.get(j), "body weight", 1, PrescriptionType.SETS_REPS,
                        null, PATRONES.get(j)));

        return new PlanGenerationContext(USUARIO, (short) 2, LUNES, LUNES.plusDays(6),
                perfil(), PlanAdjustment.none(), null, List.copyOf(elegibles),
                SafetyProfile.none(), parametros());
    }

    /**
     * {@code GAIN_MUSCLE} es obligatorio: sin eso {@code MvpDosePolicy.aplicaA}
     * es falso y V24 ni se mide, con lo que la prueba no demostraria nada sobre
     * la dosis.
     * <p>
     * Sesiones de 45 minutos, las del participante 2 de la corrida real, para que
     * el piso de V20 sean los mismos 31 minutos del hallazgo.
     */
    private static PlanningProfile perfil() {
        return new PlanningProfile(USUARIO, 30, "MALE", new BigDecimal("175.0"),
                new BigDecimal("72.0"), new BigDecimal("75.0"), "BEGINNER",
                "GAIN_MUSCLE", "Ganar masa muscular entrenando en casa", "Ninguna",
                "HOME", SESIONES, List.of((short) 1, (short) 3, (short) 5),
                MINUTOS_SESION, List.of("body weight", "dumbbell", "band"), List.of(),
                SafetyProfile.Declaradas.sinLimitaciones());
    }

    private static String unido(List<String> problemas) {
        return String.join(" | ", problemas);
    }
}
