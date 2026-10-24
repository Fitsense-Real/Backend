package main.web.services.fitsense.planning.domain.model.valueobjects;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * La dosis obligatoria del PRIMER PERFIL DEL MVP, tomada de
 * {@code criterio-4D-alcance-MVP.md} v2 (10/10/2026), que es la referencia
 * metodologica vigente por decision de Juan del 15/10.
 * <p>
 * <b>4D v3, 16/10/2026.</b> Un solo cambio sobre la v2, aprobado por Juan: la
 * excepcion de zona pequena de {@link #EXTRA_REPS_ZONA_PEQUENA}, que resuelve la
 * contradiccion con V16 observada en el ejercicio 493. La v2 se conserva. Todo
 * lo demas -la tabla de §2, el coste de §5, el tope de colocaciones, el 25 % sin
 * denominador- queda igual.
 * <p>
 * SOLO SE APLICA A {@code GAIN_MUSCLE}. Los demas objetivos siguen con
 * {@link TargetPrescription} sin ningun cambio, y {@code MAX_SETS_BEGINNER}
 * sigue vigente para ellos. Retirar el tope de dos series para principiantes no
 * demuestra que tres sean apropiadas para todos: aqui es una politica explicita
 * del primer perfil, no una regla general.
 * <p>
 * LA CAPACIDAD LA ASIGNA EL BACKEND, NO SE LEE DEL BORRADOR. Se deriva del
 * {@code movement_pattern} del catalogo -dato de la V26- a traves de
 * {@link MandatorySlot}. Nunca de las repeticiones que devolvio la IA: eso seria
 * validar la respuesta contra si misma.
 * <p>
 * PRIMER CORTE, con el alcance declarado. 4D v2 §6 dice que
 * {@code MUSCLE_GAIN} es {@code HYPERTROPHY} principal mas {@code STRENGTH}
 * secundaria «al 25 %», pero NO dice 25 % de que: de las colocaciones, de las
 * series o del volumen. Mientras ese denominador no este definido:
 * <ul>
 *   <li>toda colocacion estructural se prescribe como {@code HYPERTROPHY};</li>
 *   <li>{@code STRENGTH} queda DECLARADA en la tabla pero no se asigna, y el
 *       reparto del 25 % es OBJETIVO DE PRESCRIPCION, no condicion de rechazo
 *       -la misma figura que 4D v2 §3.1 uso al degradar la frecuencia por
 *       slot-;</li>
 *   <li>por tanto un plan sin colocaciones de fuerza pasa las validaciones
 *       implementadas, y de el NO se puede afirmar que cumplio ese objetivo.</li>
 * </ul>
 * Queda pendiente de especificacion y no se cambia el criterio congelado en
 * silencio.
 * <p>
 * <b>P-1.3, 16/10/2026.</b> Esta clase pasa a ser tambien la fuente de lo que el
 * PROMPT dice sobre la dosis, por condicion de Juan: «no duplicar las reglas de
 * 4D manualmente dentro del prompt; la informacion debe salir de MvpDosePolicy,
 * para que el validador y la IA utilicen la misma referencia». De ahi
 * {@link ClaseDosis}, que da nombre a cada fila de la tabla sin anadir ninguna
 * regla nueva: la dosis sigue siendo la misma que calculaba
 * {@link #dosisPara(Optional, String)}, que ahora delega en ella.
 */
public final class MvpDosePolicy {

    /** El unico objetivo al que esta politica se aplica hoy. */
    public static final String OBJETIVO = "GAIN_MUSCLE";

    /** 4D v2 §5, DISENO, es D6 y no esta decidida. */
    public static final double SEG_POR_REP = 3.0;

    /** 4D v2 §5: del tiempo de sesion, descontado el calentamiento. */
    public static final double FRACCION_UTIL = 0.85;

    /** 4D v2 §5. Se aplica SIEMPRE, incluso si el tiempo sobra. */
    public static final int MAX_COLOC_DIA = 6;

    /**
     * <b>4D v3.1 (adenda del 24/10/2026, docs/FitSense/ADENDA_4D_v3_1.md).</b>
     * Con sesiones de {@value #MINUTOS_SESION_LARGA} minutos o mas, el tope de
     * colocaciones sube a {@value #MAX_COLOC_DIA_SESION_LARGA}: con seis
     * colocaciones de 4D el contenido no llega al minimo de V20 (70 %), asi que
     * esos perfiles no tenian ninguna semana valida. La dosis y el presupuesto
     * de §5 no cambian y siguen limitando. {@link #MAX_COLOC_DIA} se conserva
     * como la referencia congelada.
     */
    public static final int MAX_COLOC_DIA_SESION_LARGA = 7;
    public static final int MINUTOS_SESION_LARGA = 60;

    /** El tope de colocaciones del dia para una sesion de esos minutos (4D v3.1). */
    public static int maxColocaciones(int sessionMinutes) {
        return sessionMinutes >= MINUTOS_SESION_LARGA ? MAX_COLOC_DIA_SESION_LARGA : MAX_COLOC_DIA;
    }

    /** 4D v2 §6, DISENO, es D22. Objetivo, no condicion de rechazo. */
    public static final double FRAC_SECUNDARIA_OBJETIVO = 0.25;

    /**
     * 4D v3 · EXCEPCION DE ZONA PEQUENA. Repeticiones de mas sobre la base de la
     * capacidad cuando la zona es pequena.
     * <p>
     * POR QUE EXISTE. 4D v2 §2 no tiene dimension de zona, y V16 si: exige un
     * minimo por zona y nivel. Con la base 10 de hipertrofia, un ejercicio de
     * {@code lower legs} queda en 10 y V16 pide 12, asi que NINGUN valor
     * satisface las dos reglas. Observado el 16/10 en el ejercicio 493
     * (elevacion de pantorrilla): V16 lo rechazo a 10, la IA lo subio a 12 y
     * entonces incumplio la dosis. Con la excepcion sale 13, que entra en el
     * 12-30 de V16, y la contradiccion desaparece.
     * <p>
     * PROCEDENCIA: <b>CONVENCION DE DISENO YA EXISTENTE</b>, no umbral validado.
     * El valor y las dos zonas se toman tal cual de
     * {@code RuleBasedTrainingPlanGenerator}, donde estan desde el plan 29 con
     * su motivo escrito -«un ejercicio pequeno necesita mas repeticiones»; «sin
     * esto todo el plan 29 salio a 12»-. 4D v3 no inventa la regla: la absorbe,
     * para que la dosis la decida un solo sitio.
     */
    public static final int EXTRA_REPS_ZONA_PEQUENA = 3;

    /** Las mismas dos de {@code RuleBasedTrainingPlanGenerator.SMALL_ZONES}. */
    public static final Set<String> ZONAS_PEQUENAS = Set.of("lower legs", "waist");

    /**
     * 4D v2 §2: una colocacion de relleno lleva DOS series, con las repeticiones
     * y el descanso de la capacidad principal. Estaba como literal dentro de
     * {@code dosisPara}; se nombra al mover el calculo a {@link ClaseDosis}.
     */
    public static final int SERIES_RELLENO = 2;

    private MvpDosePolicy() {}

    /** Las cuatro filas de la tabla de dosis de 4D v2 §2. */
    public enum Capacidad {
        /** Principal de {@code MUSCLE_GAIN}. Descanso 90 s: EMPIRICO DEBIL, Singer et al. 2024. */
        HYPERTROPHY(3, 10, 90, "2-3"),
        /** Secundaria de {@code MUSCLE_GAIN}. DECLARADA, no asignada en el primer corte. */
        STRENGTH(3, 5, 150, "2-3"),
        /** Fuera del primer perfil. */
        MUSCULAR_ENDURANCE(3, 18, 45, "1-2"),
        /** Fuera del primer perfil, y estructuralmente inalcanzable para principiantes. */
        POWER(3, 4, 150, null);

        private final short series;
        private final short reps;
        private final short descansoSegundos;
        private final String rir;

        Capacidad(int series, int reps, int descansoSegundos, String rir) {
            this.series = (short) series;
            this.reps = (short) reps;
            this.descansoSegundos = (short) descansoSegundos;
            this.rir = rir;
        }

        public short series()           { return series; }
        public short reps()             { return reps; }
        public short descansoSegundos() { return descansoSegundos; }

        /**
         * RIR de 4D v2 §2. NO se comprueba: {@code planned_workout_exercises} no
         * tiene columna para guardarlo, asi que no hay nada contra lo que
         * comparar. Viaja aqui para que la tabla este completa y para que se vea
         * que falta, no para fingir que se valida.
         */
        public String rir() { return rir; }
    }

    /**
     * LAS CLASES DE DOSIS. Una por cada combinacion que esta politica puede
     * producir, con su nombre exacto.
     * <p>
     * POR QUE SON CUATRO Y NO DOS. El slot y la zona son dimensiones
     * INDEPENDIENTES: el slot decide las series -3 estructural, 2 relleno- y la
     * zona decide las repeticiones -+3 si es pequena-. Nombrar solo
     * «estructural» y «relleno» dejaria dos dosis distintas bajo el mismo
     * nombre, y la mas cara de las cuatro -{@code ESTRUCTURAL_ZONA_PEQUENA},
     * 6,45 min- sin fila propia.
     * <p>
     * {@link #dosis()} es EL UNICO SITIO donde se construye una dosis, para que
     * la etiqueta que viaja al prompt y la dosis que exige el validador no
     * puedan separarse: salen de la misma linea de codigo.
     */
    public enum ClaseDosis {
        ESTRUCTURAL_ESTANDAR(false, false),
        ESTRUCTURAL_ZONA_PEQUENA(false, true),
        RELLENO_ESTANDAR(true, false),
        RELLENO_ZONA_PEQUENA(true, true);

        private final boolean relleno;
        private final boolean zonaPequena;

        ClaseDosis(boolean relleno, boolean zonaPequena) {
            this.relleno = relleno;
            this.zonaPequena = zonaPequena;
        }

        public boolean relleno()     { return relleno; }
        public boolean zonaPequena() { return zonaPequena; }

        /** La dosis de esta clase. Mismo calculo que tenia {@code dosisPara}. */
        public Dosis dosis() {
            var principal = Capacidad.HYPERTROPHY;
            short reps = (short) (principal.reps()
                    + (zonaPequena ? EXTRA_REPS_ZONA_PEQUENA : 0));
            short series = relleno ? (short) SERIES_RELLENO : principal.series();
            return new Dosis(series, reps, principal.descansoSegundos(), principal, relleno);
        }

        /**
         * La clase de una combinacion. Recorre los valores en vez de encadenar
         * condiciones: si manana aparece una dimension mas, falla aqui en vez de
         * devolver la clase equivocada en silencio.
         */
        static ClaseDosis de(boolean relleno, boolean zonaPequena) {
            for (var clase : values())
                if (clase.relleno == relleno && clase.zonaPequena == zonaPequena) return clase;
            throw new IllegalStateException(
                    "Combinacion sin clase de dosis: relleno=%s zonaPequena=%s"
                            .formatted(relleno, zonaPequena));
        }
    }

    /** Lo que se exige a una colocacion concreta. */
    public record Dosis(short series, short reps, short descansoSegundos,
                        Capacidad capacidad, boolean relleno) {

        /** 4D v2 §5: el descanso se cobra DESPUES DE CADA serie, tambien la ultima. */
        public double costeSegundos(boolean porLado) {
            double ejecucion = (porLado ? 2 : 1) * reps * SEG_POR_REP;
            return series * (ejecucion + descansoSegundos);
        }

        public String describe() {
            return "%dx%d, descanso %d s (%s)".formatted(series, reps, descansoSegundos,
                    relleno ? "relleno, reps y descanso de " + capacidad.name()
                            : capacidad.name());
        }
    }

    /** Si esta politica gobierna el objetivo del perfil. */
    public static boolean aplicaA(String goalType) {
        return OBJETIVO.equals(goalType);
    }

    /**
     * La dosis exigida a una colocacion, segun el slot que le da el catalogo.
     * <p>
     * Colocacion estructural -uno de los cuatro slots de 4C- va a la capacidad
     * principal. Cualquier otra es RELLENO: 2 series, y las repeticiones y el
     * descanso de la principal dominante, como manda 4D v2 §2.
     * <p>
     * ABSTENCION: devuelve {@code Optional.empty()} SOLO si {@code slot} es
     * {@code null}, no si es {@code Optional.empty()}.
     * <p>
     * <b>Precision del 16/10, al escribir P-1.3.</b> La redaccion anterior de
     * este parrafo decia «empty cuando el ejercicio no tiene patron conocido»,
     * y eso NO es lo que hace el codigo -ni antes ni ahora-.
     * {@code MandatorySlot.of(candidate)} devuelve {@code Optional.empty()} en
     * dos casos distintos, «patron que no es ninguno de los cuatro slots» y
     * «patron nulo», y los dos reciben dosis de RELLENO. Como el validador
     * siempre pasa {@code MandatorySlot.of(...)}, que nunca es {@code null}, la
     * rama de abstencion NO se alcanza desde el: las abstenciones observadas en
     * los planes 12 a 15 vienen del salto de {@code DURATION}, que el validador
     * hace ANTES de llamar aqui -4D v2 §4, regla 4-.
     * <p>
     * Se corrige el texto, no el comportamiento: que un ejercicio sin patron
     * reciba relleno o se abstenga es una decision del criterio 4D, que esta
     * congelado. Queda declarado como pregunta abierta.
     */
    public static Optional<Dosis> dosisPara(Optional<MandatorySlot> slot, String bodyPartCode) {
        return claseDosis(slot, bodyPartCode).map(ClaseDosis::dosis);
    }

    /**
     * La CLASE de dosis de una colocacion: el mismo reparto que
     * {@link #dosisPara(Optional, String)}, antes de resolver los numeros.
     * <p>
     * 4D v3: la zona solo mueve las REPETICIONES. Las series las decide el slot
     * -estructural o relleno- y el descanso es el de la capacidad.
     * <p>
     * Existe para P-1.3: el snapshot necesita el NOMBRE de la fila, no la dosis
     * repetida en cada candidato. Que {@code dosisPara} delegue aqui es lo que
     * garantiza que la etiqueta del prompt y la exigencia del validador no
     * puedan divergir.
     */
    public static Optional<ClaseDosis> claseDosis(Optional<MandatorySlot> slot, String bodyPartCode) {
        if (slot == null) return Optional.empty();
        return Optional.of(ClaseDosis.de(slot.isEmpty(), esZonaPequena(bodyPartCode)));
    }

    /**
     * La tabla completa, recorriendo la politica. Lo que viaja en
     * {@code constraints.dosis_por_clase}: ninguna fila se escribe a mano, asi
     * que una quinta combinacion aparecera sola.
     */
    public static List<ClaseDosis> tablaDosis() {
        return List.of(ClaseDosis.values());
    }

    /** Tolerante con el caso nulo y con las mayusculas del catalogo. */
    public static boolean esZonaPequena(String bodyPartCode) {
        return bodyPartCode != null
                && ZONAS_PEQUENAS.contains(bodyPartCode.trim().toLowerCase(Locale.ROOT));
    }

    /** 4D v2 §5: presupuesto del dia en segundos. */
    public static double presupuestoDiarioSegundos(int sessionMinutes) {
        return sessionMinutes * FRACCION_UTIL * 60.0;
    }

    /** Para el log y los mensajes de rechazo. */
    public static String describeTabla() {
        var h = Capacidad.HYPERTROPHY;
        return ("4D v3 MVP GAIN_MUSCLE: estructural %dx%d descanso %d s · "
                + "relleno 2x%d descanso %d s · zona pequena %s +%d reps · "
                + "maximo %d colocaciones/dia · util %.0f %% del tiempo de sesion")
                .formatted(h.series(), h.reps(), h.descansoSegundos(),
                        h.reps(), h.descansoSegundos(),
                        ZONAS_PEQUENAS.stream().sorted().toList(), EXTRA_REPS_ZONA_PEQUENA,
                        MAX_COLOC_DIA, FRACCION_UTIL * 100);
    }
}
