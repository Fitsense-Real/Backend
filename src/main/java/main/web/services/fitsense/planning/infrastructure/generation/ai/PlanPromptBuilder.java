package main.web.services.fitsense.planning.infrastructure.generation.ai;

import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Arma el prompt. La entrada estructurada de 19.1 viaja como JSON dentro del
 * mensaje, tal cual se persiste en input_snapshot, para que lo que se envio y lo
 * que se guardo sean literalmente lo mismo.
 */
@Component
public class PlanPromptBuilder {

    /**
     * Version del texto de reglas. Viaja en input_snapshot.prompt_version.
     * <p>
     * PR-1.3 (GEN-IN-1.10, P-1.3): la regla 16 y el texto del reintento ceden ante
     * clase_dosis. Se pliegan DENTRO de PR-1.3 en vez de subir a PR-1.4 porque
     * ningun plan se genero todavia con la version intermedia: no hay evidencia
     * atada a ella, y el estudio queda con un solo artefacto -el prompt coherente y
     * etiquetado- en lugar de dos que nunca se usaron por separado. Si ya hubiera
     * corrido un plan con PR-1.3, habria que subir.
     * <p>
     * Lo que motivo los dos cambios, medido el 16/10: el reintento ordenaba
     * conservar al menos los ejercicios que ya habia y anadir una serie para llegar
     * al minimo, mientras el rechazo pedia bajar de diez colocaciones a seis y 4D
     * fijaba las series. Ninguna propuesta podia cumplir las dos cosas.
     * <p>
     * Ademas sube la
     * version porque lo que sube es el ARTEFACTO EFECTIVO: el prompt final suma los
     * campos de dosis del snapshot y, con GAIN_MUSCLE, el punto 10 de los
     * principios. Precision de Juan del 16/10, que corrige una premisa mia: la
     * version registra lo que recibio el modelo, no si cambio este codigo.
     * <p>
     * PR-1.2 (GEN-IN-1.8): la regla 15 remite a las cuotas por grupo que ahora
     * trae suggested_split (en los planes 31 y 32 el dia inferior salio con casi
     * todos los ejercicios en upper legs) y la 2 prohibe repetir entrenamientos
     * (el plan 32 devolvio 5 sesiones repitiendo dos).
     * <p>
     * PR-1.1 (GEN-IN-1.7): tras el plan 30. La regla 16 manda partir de
     * target_reps y target_sets, porque la IA tomo min_reps como valor por
     * defecto y puso 3x8 a los 21 ejercicios. La 17 remite a
     * min_exercises_per_session, que el backend calcula. El reintento prohibe
     * corregir quitando ejercicios y recuerda que todas las reglas siguen
     * valiendo: en el plan 30 arreglo V15 y V16 borrando la mitad de cada sesion
     * y rompio V20 en las tres.
     * <p>
     * PR-1.0 (GEN-IN-1.6): primera version con numero. Respecto al texto
     * anterior: las reglas 9, 16 y 21 se unen en una sobre min_reps por
     * ejercicio (la 9 decia todavia "minimo 6 repeticiones" y contradecia a la
     * 16 y la 21); el reintento incluye la propuesta rechazada y va despues de
     * los datos, con instruccion de corregirla y no rehacerla.
     * <p>
     * PR-1.4 (GEN-IN-1.11, 4A v6 + 4D v4): bloque de CAPACIDADES para
     * INCREASE_STRENGTH, LOSE_WEIGHT y GENERAL_FITNESS. Solo aparece cuando
     * {@code constraints.capacidades} no es nulo, asi que el texto de
     * GAIN_MUSCLE no cambia. Describe las mismas reglas que V24 y V25: la dosis
     * exacta de una capacidad por colocacion y las principales factibles
     * siempre presentes.
     * <p>
     * PR-1.5 (GEN-IN-1.12, 24/10/2026): tras el plan 53, que salio en el
     * segundo intento porque el primero omitio CARDIO y MOBILITY (V25). Para
     * los objetivos por capacidades, la REGLA PRIORITARIA va al principio, con
     * las principales factibles de ESTA persona y los candidatos de las que
     * tienen pocos; el orden de construccion es explicito (principales primero,
     * secundarias despues) y hay una comprobacion antes de responder, despues
     * de los datos. Las dosis se citan por codigo (capacidades.dosis). El
     * nombre del plan no menciona correcciones. GAIN_MUSCLE no cambia salvo
     * esa ultima linea del reintento.
     * <p>
     * PR-1.6 (26/10/2026): tras el plan 55 (LOSE_WEIGHT, tres intentos, 1
     * colocacion de CARDIO y 23 de fuerza). La regla prioritaria pide la
     * principal en CADA sesion antes que las secundarias, que es lo que ya hace
     * el motor de reglas (CapabilityWeekBuilder: con LOSE_WEIGHT, dos bloques de
     * CARDIO por sesion si hay ejercicios), y recuerda que un ejercicio puede
     * repetirse en dias no consecutivos. constraints.max_colocaciones_dia ya
     * viaja tambien para estos objetivos, y la comprobacion final incluye ese
     * tope y max_per_body_part, los motivos de los rechazos 1 y 2.
     * <p>
     * PR-1.7 (26/10/2026): tras el plan 58 (INCREASE_STRENGTH: 3 colocaciones
     * de fuerza y 18 de hipertrofia; el motor de reglas pone 12 y 6). Con
     * STRENGTH como principal, cada sesion empieza por los estructurales con
     * dosis de fuerza, priorizando los slots que la semana aun no cubre; 4 por
     * sesion (3 si dura menos de 30 minutos) como referencia, igual que el
     * motor de reglas; la hipertrofia solo despues. La semana debe tener los
     * cuatro slots con dosis de STRENGTH (V25), y la comprobacion final lo pide.
     * <p>
     * Cambiar cualquier texto de este archivo exige subir la version.
     */
    public static final String VERSION = "PR-1.7";

    /** El bloque de capacidades. Publico para poder probar que solo aparece donde toca. */
    public static final String BLOQUE_CAPACIDADES = """

            CAPACIDADES DEL OBJETIVO (constraints.capacidades no es null):
            - Cada ejercicio de available_exercises trae en "dosis" los codigos que
              admite; cada codigo esta definido en constraints.capacidades.dosis. Cada
              ejercicio que pongas debe llevar EXACTAMENTE una de esas dosis: series
              (entre series y series_max), reps o segundos y descanso_segundos tal
              cual. Esa dosis es la que decide que capacidad trabaja: el codigo empieza
              por la capacidad (CARDIO_1 trabaja CARDIO). Una dosis distinta se rechaza.
            - Primero incluye todas las capacidades de principales_factibles: cada una
              al menos una vez en la semana. Despues completa el tiempo con las
              secundarias. Una secundaria nunca puede ocupar el lugar de una principal
              factible.
            - Las de no_factibles no se exigen: el participante no las puede hacer con
              sus condiciones. No las sustituyas por algo inseguro ni por un circuito.
            - Solo puedes usar los ejercicios de available_exercises: los que no
              aportan nada al objetivo ya no estan en la lista.
            """;

    private final JsonSupport jsonSupport;

    public PlanPromptBuilder(JsonSupport jsonSupport) {
        this.jsonSupport = jsonSupport;
    }

    public String build(PlanGenerationContext context, String inputSnapshotJson,
                        AiRetryFeedback feedback) {
        var prompt = new StringBuilder();
        boolean porCapacidades = main.web.services.fitsense.planning.domain.services.CapabilityPrescriptionPolicy
                .gobiernaGeneracion(context.profile().goalType());
        var capacidades = porCapacidades ? PlanInputSnapshot.of(context).constraints().capacidades() : null;

        prompt.append("""
                Eres un entrenador que diseña planes semanales de entrenamiento.
                Devuelve UNICAMENTE el objeto JSON del esquema, sin texto adicional.
                """);
        if (capacidades != null) prompt.append(reglaPrioritaria(capacidades))
                .append(reglaDeFuerza(context, capacidades));

        prompt.append("""

                Reglas que el backend verifica y que invalidan tu propuesta si no se cumplen:
                1.  Usa solo exercise_id presentes en available_exercises. No inventes ninguno.
                2.  Genera exactamente tantos entrenamientos como days_per_week,
                    uno por cada fecha de constraints.suggested_split. Ni uno mas:
                    no repitas un entrenamiento ya escrito.
                3.  Programa solo en fechas cuyo dia de la semana este en available_days,
                    dentro del rango week_start_date a week_end_date, formato AAAA-MM-DD.
                4.  expected_duration_minutes no puede superar session_minutes mas 15 %.
                5.  Cada entrenamiento lleva al menos 2 ejercicios.
                6.  Ningun ejercicio puede superar max_difficulty_level.
                7.  Usa el prescription_type que trae cada ejercicio en
                    available_exercises; el backend rechaza otro distinto.
                    SETS_REPS exige planned_sets y planned_reps.
                    DURATION exige planned_duration_seconds.
                8.  Si adjustment.target_volume no es null, la suma de volumen debe caer entre target_volume_min
                    y target_volume_max. Volumen = planned_sets x planned_reps, o
                    planned_duration_seconds x planned_sets / 30 para los de duracion.
                    La carga NO cuenta como volumen.
                9.  Minimo 2 series por ejercicio SETS_REPS. Las repeticiones, en la regla 16.
                10. Minimo 20 segundos en los ejercicios de duracion.
                11. target_load_kg sigue el principio 8. Sin peso anotado la semana
                    anterior va en null; nunca sube mas de un 10 % sobre el peso
                    usado, y solo si hizo todas las repeticiones.
                12. No pongas dos entrenamientos el mismo dia.
                13. No repitas el mismo focus_code en dias consecutivos. Usa las
                    fechas y enfoques de constraints.suggested_split: ya cumplen
                    esta regla y la 18. Si propones otra division, debe cumplirlas.
                14. Si safety_notes no es null, respetalo al prescribir.
                    available_exercises YA excluye lo prohibido, pero de los que
                    quedan elige y pauta las variantes mas conservadoras: rango de
                    movimiento comodo y sin dolor, menos series, mas descanso.
                15. Cada entrenamiento debe CUBRIR su enfoque, no repetir zona.
                    Un FULL_BODY con seis ejercicios de biceps no es cuerpo
                    completo. Cada sesion de constraints.suggested_split trae sus
                    cuotas ya calculadas: usa body_parts (los grupos que admite),
                    cubre al menos min_body_parts de ellos y no pongas mas de
                    max_per_body_part ejercicios del mismo body_part. Reparte
                    entre los grupos hasta completar los ejercicios de la sesion:
                    si un grupo llega a su tope, sigue por otro.
                16. Las repeticiones las decides tu, ejercicio por ejercicio,
                    con los PRINCIPIOS DE PRESCRIPCION de mas abajo. Cada ejercicio
                    de available_exercises trae su min_reps: planned_reps nunca
                    baja de ese numero ni supera constraints.max_reps. min_reps ya
                    tiene en cuenta la zona y el nivel de la persona: usalo tal
                    cual, no lo recalcules. Ese limite NO es un objetivo: es solo
                    el borde de lo absurdo, y NO es el valor por defecto.
                    Parte de constraints.target_reps y target_sets, la prescripcion
                    del objetivo de esta persona, y apartate de ahi ejercicio por
                    ejercicio segun los principios. No pongas la misma prescripcion
                    a todos los ejercicios.
                    Cuando clase_dosis tenga un valor no nulo, la dosis de esa clase
                    manda sobre target_reps y target_sets: no te apartes de ella. Los demas
                    siguen partiendo de target_reps y target_sets.
                17. La duracion de cada sesion la calcula el backend con el
                    contenido que prescribes y fija expected_duration_minutes con
                    ese valor; no hace falta que la calcules tu. La formula es:
                      minutos = 5 de calentamiento
                              + por ejercicio: series x repeticiones x 3 s
                                               (o series x segundos si es DURATION)
                              + por ejercicio: (series - 1) x descanso
                              + 1 minuto de transicion entre ejercicios
                    Usala para comprobar que cada sesion llega a su minimo: el
                    bloque DURACION CALCULADA de abajo da ese minimo en segundos y
                    lo que cuesta cada dosis.
                    Si constraints.min_session_minutes no es null, cada sesion
                    debe durar al menos eso: la persona reservo ese tiempo. Llega
                    con ejercicios o series, no con descanso: rest_seconds nunca
                    supera constraints.max_rest_seconds.
                    constraints.min_exercises_per_session dice cuantos ejercicios
                    hacen falta para llegar a ese minimo. Se calcula con la dosis
                    de la tabla cuando hay clase_dosis, y con target_sets x
                    target_reps cuando no. Usalo como referencia al armar cada
                    sesion: con menos ejercicios solo llegas subiendo series o
                    repeticiones, y eso solo es posible si clase_dosis es null.
                18. Recuperacion entre dias consecutivos: el mismo exercise_id
                    no se repite en dos dias seguidos, y chest, back, shoulders y
                    upper legs no se trabajan dos dias seguidos. Con dias
                    seguidos alterna tren superior e inferior (o empuje y
                    traccion). waist, upper arms y lower legs si pueden repetirse.
                19. Si adjustment.types incluye REDUCE_VOLUME, cada ejercicio de
                    constraints.reduce_volume_caps no puede superar su max_sets ni
                    su max_reps. reason HOLD: la persona no lo completo o le costo;
                    no lo subas. reason COMPLETED: lo hizo bien; puede progresar
                    un poco, hasta el tope. El volumen total igual debe bajar: si
                    te falta volumen, anade ejercicios nuevos del enfoque en vez de
                    subir los repetidos. Antes de responder, revisa uno por uno los
                    exercise_id de esa lista que uses.
                20. En PUSH, de upper arms solo ejercicios de triceps; en PULL, solo
                    de biceps. Mira target_muscle de cada ejercicio.
                available_exercises viene agrupado por body_part y mezclado dentro
                de cada grupo: NO tomes los primeros de la lista. Lee el body_part
                de cada uno y elige a proposito.

                Y elige pensando en el objetivo del participante (user.goal_type y
                user.goal_text), no solo en lo que cabe: no es lo mismo preparar a
                alguien que quiere perder peso que a quien busca fuerza maxima.

                Un plan que el participante no puede hacer no es solo un plan
                malo: produce un dato falso. Si no encaja con el, su adherencia
                bajara y el sistema lo interpretara como falta de compromiso.
                
                Grupos que admite cada focus_code. Elige el foco ANTES de elegir
                los ejercicios, y no metas grupos que ese foco no admite:
                  FULL_BODY   chest, back, upper legs, shoulders, waist
                  UPPER_BODY  chest, back, shoulders, upper arms
                  LOWER_BODY  upper legs, lower legs, waist
                  PUSH        chest, shoulders, upper arms
                  PULL        back, upper arms
                  LEGS        upper legs, lower legs
                  CORE        waist

                total_volume debe ser la suma real de tus prescripciones: el backend la
                recalcula y rechaza la propuesta si no coincide.

                rationale se le muestra al usuario: escribelo en espanol, en segunda persona,
                breve y concreto. Si adjustment.target_volume no es null, di con
                claridad si el volumen sube, baja o se mantiene y por que (usa
                adjustment.reason). Nunca digas que se mantiene si baja o sube.
                """);

        // Los principios van despues de las reglas verificables y antes de los
        // datos: primero lo que invalida el plan, luego el criterio para elegir
        // dentro de lo valido. El texto y su version viven en
        // PrescriptionPrinciples; la version queda en input_snapshot.
        prompt.append('\n').append(
                PrescriptionPrinciples.textFor(context.profile().goalType()));
        if (porCapacidades)
            prompt.append(BLOQUE_CAPACIDADES);

        prompt.append(duracionCalculada(context));
        prompt.append("\nDatos de entrada:\n").append(inputSnapshotJson).append('\n');

        // La comprobacion va DESPUES de los datos: es lo ultimo que lee el
        // modelo antes de escribir, y lo que el plan 53 no hizo en su primer intento.
        if (capacidades != null) prompt.append(comprobacionFinal(capacidades))
                .append(comprobacionDeFuerza(context, capacidades));

        // Reintento (19.4). Va AL FINAL, despues de los datos: es lo ultimo que
        // lee el modelo y lo unico que distingue este intento del anterior.
        if (feedback != null && !feedback.isEmpty()) appendRetry(prompt, feedback);

        return prompt.toString();
    }

    /**
     * Las principales factibles de ESTA persona, nombradas, y los ejercicios
     * de las que tienen pocos candidatos. Va antes de las reglas generales.
     */
    static String reglaPrioritaria(PlanInputSnapshot.CapacidadesObjetivo c) {
        var texto = new StringBuilder("""

                REGLA PRIORITARIA DE ESTA SEMANA
                Capacidades principales factibles para esta persona: %s.
                Las principales son el eje de CADA sesion, no un ejercicio suelto de la semana.
                Construye cada sesion en este orden:
                1. Pon primero, en ESA sesion, al menos una colocacion de cada principal
                   factible, con una dosis de esa capacidad (un codigo de "dosis" que empiece
                   por ella).%s
                2. Solo despues completa el tiempo con las secundarias (%s), sin pasar de
                   constraints.max_colocaciones_dia ejercicios por sesion.
                3. Si no cabe todo, quita secundarias, nunca la principal de esa sesion.
                Un mismo ejercicio puede repetirse en dias que no sean consecutivos.
                Una semana que omite una principal factible se rechaza entera.
                """.formatted(String.join(", ", c.principalesFactibles()),
                c.principalesFactibles().size() == 1 && c.principalesFactibles().contains("CARDIO")
                        ? "\n   Con CARDIO como unica principal, pon dos bloques de CARDIO por sesion si\n"
                        + "   hay ejercicios para ello."
                        : "",
                c.secundarias() == null || c.secundarias().isEmpty() ? "ninguna" : String.join(", ", c.secundarias())));
        if (c.candidatosEscasos() != null && !c.candidatosEscasos().isEmpty()) {
            texto.append("Estas principales tienen pocos ejercicios posibles; elige entre estos exercise_id:\n");
            c.candidatosEscasos().forEach((cap, ids) -> texto.append("  ").append(cap).append(": ")
                    .append(ids).append('\n'));
        }
        if (c.noFactibles() != null && !c.noFactibles().isEmpty())
            texto.append("No son factibles y no se exigen: ").append(String.join(", ", c.noFactibles().keySet()))
                    .append(". No las sustituyas por nada inseguro.\n");
        return texto.toString();
    }

    private static boolean fuerzaPrincipal(PlanGenerationContext context, PlanInputSnapshot.CapacidadesObjetivo c) {
        return "INCREASE_STRENGTH".equals(context.profile().goalType())
                && c.principalesFactibles() != null && c.principalesFactibles().contains("STRENGTH");
    }

    /** INCREASE_STRENGTH: la estructura con la que construye el motor de reglas. */
    static String reglaDeFuerza(PlanGenerationContext context, PlanInputSnapshot.CapacidadesObjetivo c) {
        if (!fuerzaPrincipal(context, c)) return "";
        int porSesion = context.effectiveSessionMinutes() < 30 ? 3 : 4;
        return """
                Con STRENGTH como principal, en CADA sesion:
                1. Empieza por ejercicios estructurales (los que traen "slot") con una dosis STRENGTH,
                   dando prioridad a los slots LOWER_KNEE, LOWER_HIP, UPPER_PUSH y UPPER_PULL que la
                   semana todavia no cubre con fuerza. Referencia: %d por sesion.
                2. Solo despues, si queda tiempo, completa con HYPERTROPHY.
                La semana tiene que cubrir los cuatro slots con dosis STRENGTH: un slot trabajado solo
                con dosis de HYPERTROPHY no cuenta como cubierto.
                """.formatted(porSesion);
    }

    static String comprobacionDeFuerza(PlanGenerationContext context, PlanInputSnapshot.CapacidadesObjetivo c) {
        if (!fuerzaPrincipal(context, c)) return "";
        return """
                - Cada uno de LOWER_KNEE, LOWER_HIP, UPPER_PUSH y UPPER_PULL tiene al menos una colocacion
                  con dosis STRENGTH en la semana.
                """;
    }

    static String comprobacionFinal(PlanInputSnapshot.CapacidadesObjetivo c) {
        return """

                ANTES DE ESCRIBIR EL JSON, COMPRUEBA:
                - Cada sesion tiene al menos una colocacion de cada una de %s, con una dosis
                  de esa capacidad copiada tal cual de constraints.capacidades.dosis.
                - Ninguna secundaria ocupa el lugar de una principal factible.
                - Cada ejercicio lleva una dosis que su lista "dosis" admite.
                - Ninguna sesion pasa de constraints.max_colocaciones_dia ejercicios.
                - En cada sesion, ningun body_part pasa del max_per_body_part de su
                  sesion en constraints.suggested_split.
                Si algo falla, corrige la semana antes de responder.
                """.formatted(String.join(", ", c.principalesFactibles()));
    }

    private void appendRetry(StringBuilder prompt, AiRetryFeedback feedback) {
        boolean conPropuesta = feedback.rejectedOutput() != null && !feedback.rejectedOutput().isBlank();

        if (conPropuesta) {
            prompt.append("""

                    INTENTO ANTERIOR RECHAZADO
                    Esta fue tu propuesta anterior:
                    """).append(feedback.rejectedOutput().strip()).append('\n');
        } else {
            prompt.append("\nINTENTO ANTERIOR RECHAZADO\n");
        }

        if (!feedback.rejectionProblems().isEmpty()) {
            prompt.append("\nEl backend la rechazo por estos motivos:\n");
            feedback.rejectionProblems().forEach(problem -> prompt.append("- ").append(problem).append('\n'));
        }
        if (!feedback.earlierProblems().isEmpty()) {
            prompt.append("\nErrores de intentos anteriores que tampoco puedes repetir:\n");
            feedback.earlierProblems().forEach(problem -> prompt.append("- ").append(problem).append('\n'));
        }
        var pistas = PistasDeReintento.para(feedback.rejectionProblems(), false);
        if (!pistas.isEmpty()) {
            prompt.append("\nDonde esta la solucion de cada motivo:\n");
            pistas.forEach(pista -> prompt.append("- ").append(pista).append('\n'));
        }

        prompt.append(conPropuesta ? """

                Devuelve la propuesta COMPLETA corregida, partiendo de la anterior:
                cambia solo lo necesario para resolver cada motivo y conserva lo que
                ya estaba bien. Si un motivo nombra un exercise_id, corrige ese
                ejercicio. Si una sesion no llega al minimo de minutos, anadele un
                ejercicio en vez de rehacerla; anade series solo si clase_dosis
                es null y el ejercicio es SETS_REPS.
                NO quites ejercicios ni sesiones para corregir, SALVO que un motivo
                del rechazo exija bajar el numero de ejercicios o las series: en ese
                caso quitalos y conserva lo demas. Fuera de ese caso, cada sesion
                conserva al menos los ejercicios que ya tenia. Si sobran de un grupo muscular,
                REEMPLAZALOS por otros del enfoque, nunca los elimines sin reponerlos.
                La lista de motivos solo nombra lo que fallo esta vez: TODAS las
                reglas siguen valiendo. Antes de responder, repasa tu nueva propuesta
                contra las reglas 1 a 20 completas, no solo contra los motivos.
                plan_name y rationale no mencionan el rechazo ni la correccion: el
                participante solo ve el plan final.
                """ : """

                Corrige cada motivo. Todas las reglas siguen valiendo, no solo las que
                aparecen en la lista. Antes de responder, repasa tu propuesta contra
                las reglas 1 a 20 completas.
                """);
    }

    /** Sin estado: el mismo calculo de V17 y V20. */
    private static final main.web.services.fitsense.planning.domain.services.SessionDurationEstimator ESTIMADOR =
            new main.web.services.fitsense.planning.domain.services.SessionDurationEstimator();

    /**
     * Orden de optimizacion (31/10/2026), B2: el minimo de V20 en segundos y lo que cuesta cada dosis
     * de 4D (GAIN_MUSCLE) en el mismo calculo que el validador: un ejercicio dentro de una sesion
     * suma su trabajo, sus descansos y una transicion. 10 de los rechazos de las 48 predicciones eran
     * V20, con faltas de 6 a 378 s, y 3 eran series adicionales de 4D v3.2 que sobraban.
     */
    static String duracionCalculada(PlanGenerationContext context) {
        if (context.prescription() == null) return "";
        long piso = main.web.services.fitsense.planning.domain.services.PlanDraftValidator.minimumSessionSeconds(context);
        var b = new StringBuilder("\nDURACION CALCULADA (backend): ");
        if (piso > 0) b.append("cada sesion debe sumar al menos ").append(piso).append(" s");
        else b.append("sin minimo de duracion en esta semana");
        var p = context.prescription();
        b.append("; el calentamiento cuenta ").append(p.warmupMinutesOrDefault() * 60)
                .append(" s y cada ejercicio despues del primero ").append(p.transitionSecondsOrDefault())
                .append(" s de transicion.");
        // V44 · V4-C: solo con esa politica (la historica recibe exactamente el texto de siempre).
        if (context.policy().v4Segundos() && context.profile() != null)
            b.append(" Ninguna sesion puede pasar de ").append(main.web.services.fitsense.planning.domain.model
                            .valueobjects.PlanningPolicy.limiteV4Segundos(context.profile().sessionMinutes()))
                    .append(" s (constraints.max_session_seconds): el backend los calcula a partir de los ejercicios"
                            + " y rechaza la sesion aunque declares menos minutos.");
        if (context.profile() != null && main.web.services.fitsense.planning.domain.model.valueobjects.MvpDosePolicy
                .aplicaA(context.profile().goalType())) {
            b.append(" Segundos de trabajo y descanso de cada dosis de constraints.dosis_por_clase:");
            for (var clase : main.web.services.fitsense.planning.domain.model.valueobjects.MvpDosePolicy.tablaDosis()) {
                var d = clase.dosis();
                b.append(' ').append(clase.name()).append(' ').append(d.series()).append('x').append(d.reps())
                        .append('=').append(segundos(d.series(), d.reps(), d.descansoSegundos(), p)).append(" s");
                if (d.ampliable()) b.append(" (").append(d.seriesMax()).append('x').append(d.reps()).append('=')
                        .append(segundos(d.seriesMax(), d.reps(), d.descansoSegundos(), p)).append(" s)");
                b.append(';');
            }
        }
        return b.append('\n').toString();
    }

    private static long segundos(int series, int reps, int descanso,
                                 main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams p) {
        var e = new main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft.DraftExercise(1L,
                main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType.SETS_REPS,
                (short) series, (short) reps, null, null, (short) descanso, null);
        var w = new main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft.DraftWorkout(
                java.time.LocalDate.of(2026, 1, 5), main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus.FULL_BODY,
                "x", 60, java.util.List.of(e));
        return ESTIMADOR.estimateSeconds(w, p) - (long) p.warmupMinutesOrDefault() * 60;
    }

}