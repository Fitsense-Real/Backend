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

    /**
     * H2-v2 (docs/FitSense/h2_v2/). Solo con {@code fitsense.ai.generacion.prompt-version=PR-2.0}; por omision el prompt
     * y la instantanea son exactamente los de {@link #VERSION}. Sobre el texto de PR-1.7:
     * <ul>
     *   <li>regla 15: la regla de la mitad de V15 tal como se verifica (tabla por n de trabajo, sin complementos) y
     *   solo entre los grupos con material de la sesion;</li>
     *   <li>regla 21 (nueva): los techos de V26 de series directas por grupo, por sesion y por semana, con la
     *   orientacion por grupo que sustituye a la de los principios;</li>
     *   <li>con PLAN-REV-2.0: regla 4 sin el +15 % (V4 al 100 %) y sin las frases que piden llenar tiempo (regla 17,
     *   capacidades, fuerza), porque ya no hay minimo de duracion;</li>
     *   <li>una comprobacion final del reparto, despues de los datos, para los cuatro objetivos;</li>
     *   <li>en el reintento: como corregir V15 y V26 sin contradecir las demas reglas.</li>
     * </ul>
     * No cambia ninguna regla del backend. Requiere una evaluacion pareada con la IA real antes de activarse.
     */
    public static final String VERSION_H2V2 = "PR-2.0";

    /**
     * H2-v3 (docs/FitSense/h2_v3/). Solo con {@code fitsense.ai.generacion.prompt-version=PR-2.1}. Es PR-2.0 con las
     * correcciones de su evaluacion real (07 de h2_v2):
     * <ul>
     *   <li>JERARQUIA DE RESTRICCIONES (regla 22): la dosis de V24 se copia tal cual y nunca se recorta para cumplir
     *   otro limite; V26 y V15 se cumplen eligiendo cuantas colocaciones y de que grupo; la cobertura (V23 y V25) no se
     *   pierde al quitar colocaciones; la duracion (regla 4) se ajusta quitando secundarias;</li>
     *   <li>regla 21: la orientacion por grupo deja de ser una cifra a alcanzar. Como en la politica real
     *   (ControlVolumen), solo decide a que grupo dar una colocacion mas, siempre con su dosis completa, y un grupo sin
     *   prioridad no esta prohibido (instantanea: prioridad_series_semana, solo los grupos con valor &gt; 0);</li>
     *   <li>comprobacion final con la dosis y la cobertura antes que el reparto;</li>
     *   <li>reintento sin "baja series": se quitan o mueven colocaciones; pistas de V24, V25 y V23 coherentes.</li>
     * </ul>
     * No cambia ninguna regla del backend. Desactivada por omision hasta aprobar su evaluacion real.
     */
    public static final String VERSION_H2V3 = "PR-2.1";

    /**
     * H2-v4 (docs/FitSense/h2_v4/). Solo con {@code fitsense.ai.generacion.prompt-version=PR-2.2}. Es PR-2.1 compactada
     * para quitarle calculo al modelo, con la misma jerarquia y los mismos limites:
     * <ul>
     *   <li>en available_exercises, cada ejercicio que no cuenta lo dice ({@code "cuenta": "complemento"} o
     *   {@code "sin_series"}) en vez de dos listas de ids que habia que cruzar;</li>
     *   <li>la regla de la mitad como regla (la mitad, redondeando hacia abajo, con 4 o mas de trabajo) y
     *   {@code "regla_mitad": true} en la sesion, en vez de una tabla por sesion; es el calculo exacto de V15;</li>
     *   <li>min_body_parts dicho junto al recuento: cardio y movilidad no cubren grupo (G21, G24);</li>
     *   <li>reglas 15, 21 y 22, comprobacion final y pistas del reintento sin repeticiones.</li>
     * </ul>
     * No cambia ninguna regla del backend. Desactivada por omision hasta aprobar su evaluacion real.
     */
    public static final String VERSION_H2V4 = "PR-2.2";

    private static String normal(String version) {
        var v = version == null ? VERSION : version.trim();
        return v.isEmpty() ? VERSION : v;
    }

    /**
     * Si una version configurada usa el contrato H2 (PR-2.0 o PR-2.1); falla con una desconocida (no se reinterpreta
     * con otra).
     */
    public static boolean esH2v2(String version) {
        var v = normal(version);
        if (v.equals(VERSION)) return false;
        if (v.equals(VERSION_H2V2) || v.equals(VERSION_H2V3) || v.equals(VERSION_H2V4)) return true;
        throw new IllegalArgumentException("fitsense.ai.generacion.prompt-version debe ser " + VERSION + ", "
                + VERSION_H2V2 + ", " + VERSION_H2V3 + " o " + VERSION_H2V4 + ": " + version);
    }

    /** Si una version configurada es la de H2-v4 (PR-2.2). */
    public static boolean esH2v4(String version) {
        return esH2v2(version) && normal(version).equals(VERSION_H2V4);
    }

    /** Si una version configurada es la de H2-v3 (PR-2.1). */
    public static boolean esH2v3(String version) {
        return esH2v2(version) && normal(version).equals(VERSION_H2V3);
    }

    /** La version validada y normalizada (PR-1.7 si viene vacia). */
    public static String versionValida(String version) {
        esH2v2(version);
        return normal(version);
    }

    private String version = VERSION;

    /** H2-v2/H2-v3: PR-1.7 por omision (configuracion evaluada). */
    @org.springframework.beans.factory.annotation.Value("${fitsense.ai.generacion.prompt-version:PR-1.7}")
    void setPromptVersion(String version) {
        this.version = versionValida(version);
    }

    /** El contrato H2 (PR-2.0 o PR-2.1). */
    public boolean h2v2() {
        return !VERSION.equals(version);
    }

    /** H2-v3 (PR-2.1). */
    public boolean h2v3() {
        return VERSION_H2V3.equals(version);
    }

    /** H2-v4 (PR-2.2). */
    public boolean h2v4() {
        return VERSION_H2V4.equals(version);
    }

    /** 2 = PR-2.0, 3 = PR-2.1, 4 = PR-2.2 (solo con h2v2()). */
    private int variante() {
        return h2v4() ? 4 : h2v3() ? 3 : 2;
    }

    /** La version efectiva del texto de reglas. */
    public String version() {
        return version;
    }

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
        var capacidades = porCapacidades ? PlanInputSnapshot.of(context, version).constraints().capacidades() : null;

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
        // H2-v2: la comprobacion del reparto, para los cuatro objetivos, tambien despues de los datos.
        if (h2v2()) prompt.append(H2v2.comprobacionReparto(context, variante()));

        // Reintento (19.4). Va AL FINAL, despues de los datos: es lo ultimo que
        // lee el modelo y lo unico que distingue este intento del anterior.
        if (feedback != null && !feedback.isEmpty()) appendRetry(prompt, feedback);

        return h2v2() ? H2v2.aplicar(prompt.toString(), context, feedback, variante()) : prompt.toString();
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
            b.append(" Ninguna sesion puede pasar de ").append(context.policy().limiteV4SegundosDe(context.profile().sessionMinutes()))
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


    /**
     * H2-v2 (PR-2.0): las sustituciones sobre el texto de PR-1.7. Cada una exige encontrar la frase historica: si alguien
     * cambia PR-1.7, la variante no puede quedarse a medias en silencio (falla y la prueba lo detecta).
     */
    static final class H2v2 {

        private H2v2() {}

        static final java.util.regex.Pattern REGLA4 = java.util.regex.Pattern.compile(
                "4\\.  expected_duration_minutes no puede superar session_minutes mas 15 %\\.");
        static final String REGLA4_MAXIMO = "4.  session_minutes es el MAXIMO de la persona: ninguna sesion puede"
                + " pasar de constraints.max_session_seconds (el backend calcula los segundos con la formula de la"
                + " regla 17), declares los minutos que declares.";

        static final java.util.regex.Pattern REGLA15 = java.util.regex.Pattern.compile(
                "cubre al menos min_body_parts de ellos y no pongas mas de\\s+max_per_body_part ejercicios del mismo"
                        + " body_part\\. Reparte\\s+entre los grupos hasta completar los ejercicios de la sesion:\\s+"
                        + "si un grupo llega a su tope, sigue por otro\\.");
        static final String REGLA15_V2 = "cubre al menos min_body_parts de ellos, eligiendolos entre"
                + " body_parts_con_material (los grupos de esa sesion con ejercicios para esta persona), y no metas"
                + " grupos que no esten en body_parts (salvo FULL_BODY). Los ejercicios de"
                + " constraints.complementos_no_cuentan (cardio y movilidad) no cuentan para nada de esta regla. Llama n"
                + " a los demas ejercicios de la sesion (los de trabajo). Si la sesion trae"
                + " max_per_body_part_por_n_trabajo, ningun body_part puede tener mas ejercicios que el valor de n en esa"
                + " tabla (la mitad, redondeando hacia abajo); sin tabla, esa mitad no aplica. Reparte entre"
                + " body_parts_con_material: si un grupo llega a su tope, sigue por otro de esa lista; si ya no queda"
                + " ninguno, NO anadas mas ejercicios a la sesion: una sesion con menos ejercicios es valida, una que pasa"
                + " un tope no.";

        static final java.util.regex.Pattern REGLA17_LLENAR = java.util.regex.Pattern.compile(
                "Usala para comprobar que cada sesion llega a su minimo:.*?eso solo es posible si clase_dosis es"
                        + " null\\.", java.util.regex.Pattern.DOTALL);
        static final String REGLA17_SIN_MINIMO = "Esta semana NO hay minimo de duracion: session_minutes es un"
                + " maximo. No anadas ejercicios ni series solo para llenar tiempo: el contenido lo deciden la cobertura"
                + " del enfoque, las dosis y los techos de la regla 21. rest_seconds nunca supera"
                + " constraints.max_rest_seconds.";

        /** 4D (GAIN_MUSCLE): la serie adicional solo existia para llegar al minimo de V20; sin minimo V24 la rechaza. */
        static final java.util.regex.Pattern SERIE_ADICIONAL = java.util.regex.Pattern.compile(
                "Unica excepcion: si una clase trae series_max mayor que series,.*?y solo si sin esa serie la sesion no"
                        + " llega a su minimo\\.", java.util.regex.Pattern.DOTALL);
        static final String SIN_SERIE_ADICIONAL = "Sin excepciones: esta semana no hay minimo de duracion, asi que la"
                + " serie adicional (series_max, tambien la del RELLENO_ESTANDAR de upper arms en PULL) NO se admite:"
                + " usa siempre series.";
        static final java.util.regex.Pattern REINTENTO_MINIMO = java.util.regex.Pattern.compile(
                "Si una sesion no llega al minimo de minutos, anadele un\\s+ejercicio en vez de rehacerla; anade series"
                        + " solo si clase_dosis\\s+es null y el ejercicio es SETS_REPS\\.\\s*");

        static final java.util.regex.Pattern REGLA20_FIN = java.util.regex.Pattern.compile(
                "(20\\. En PUSH, de upper arms solo ejercicios de triceps; en PULL, solo\\s+de biceps\\. Mira"
                        + " target_muscle de cada ejercicio\\.\\n)");

        static final java.util.regex.Pattern CAPACIDADES_LLENAR = java.util.regex.Pattern.compile(
                "Despues completa el tiempo con las\\s+secundarias\\.");
        static final String CAPACIDADES_SIN_LLENAR = "Despues, si caben, anade secundarias, sin pasar de"
                + " constraints.max_colocaciones_dia ni de los techos de la regla 21: no hace falta llenar el tiempo.";
        static final java.util.regex.Pattern PRIORITARIA_LLENAR = java.util.regex.Pattern.compile(
                "Solo despues completa el tiempo con las secundarias \\(([^)]*)\\), sin pasar de\\s+"
                        + "constraints\\.max_colocaciones_dia ejercicios por sesion\\.");
        static final String PRIORITARIA_SIN_LLENAR = "Solo despues, si caben, anade secundarias ($1), sin pasar de"
                + " constraints.max_colocaciones_dia ejercicios por sesion ni de los techos de la regla 21. No hace falta"
                + " llenar el tiempo.";
        static final java.util.regex.Pattern FUERZA_LLENAR = java.util.regex.Pattern.compile(
                "2\\. Solo despues, si queda tiempo, completa con HYPERTROPHY\\.");
        static final String FUERZA_SIN_LLENAR = "2. Solo despues, si caben dentro de los techos de la regla 21, anade"
                + " HYPERTROPHY.";
        static final java.util.regex.Pattern COMPROBACION_CAPACIDADES = java.util.regex.Pattern.compile(
                "- En cada sesion, ningun body_part pasa del max_per_body_part de su\\s+sesion en"
                        + " constraints\\.suggested_split\\.");
        static final String COMPROBACION_CAPACIDADES_V2 = "- El reparto de cada sesion cumple la COMPROBACION DEL"
                + " REPARTO de mas abajo.";

        static final java.util.regex.Pattern REINTENTO_REPONER = java.util.regex.Pattern.compile(
                "Si sobran de un grupo muscular,\\s+REEMPLAZALOS por otros del enfoque, nunca los elimines sin"
                        + " reponerlos\\.");
        static final String REINTENTO_REPONER_V2 = "Si sobran de un grupo muscular (V15 o V26), REEMPLAZALOS por"
                + " otros de body_parts_con_material de esa sesion; si ninguno de esos grupos admite mas, quitalos sin"
                + " reponerlos: V15 y V26 lo exigen. Para V26 tambien puedes bajar series dentro de la dosis admitida.";
        static final java.util.regex.Pattern REINTENTO_REGLAS = java.util.regex.Pattern.compile(
                "las reglas 1 a 20 completas");

        static String aplicar(String prompt, PlanGenerationContext context, AiRetryFeedback feedback) {
            return aplicar(prompt, context, feedback, false);
        }

        /** @param v3 H2-v3 (PR-2.1): las mismas sustituciones de PR-2.0 mas la jerarquia de restricciones. */
        static String aplicar(String prompt, PlanGenerationContext context, AiRetryFeedback feedback, boolean v3) {
            return aplicar(prompt, context, feedback, v3 ? 3 : 2);
        }

        /** @param variante 2 = PR-2.0, 3 = PR-2.1, 4 = PR-2.2 (la jerarquia de PR-2.1, compactada). */
        static String aplicar(String prompt, PlanGenerationContext context, AiRetryFeedback feedback, int variante) {
            boolean v3 = variante >= 3;
            boolean sinMinimo = main.web.services.fitsense.planning.domain.services.PlanDraftValidator
                    .minimumSessionSeconds(context) == 0;
            boolean sinLlenado = context.policy() != null && context.policy().duracion()
                    == main.web.services.fitsense.planning.domain.model.valueobjects.PlanningPolicy.Duracion.DOSIS_SEMANAL;
            var volumen = main.web.services.fitsense.planning.domain.services.ContratoReparto.volumen(context);

            var salida = obligatoria(REGLA15, prompt, variante == 4 ? H2v4.REGLA15 : REGLA15_V2, "regla 15");
            if (context.policy() != null && context.policy().v4Maximo())
                salida = obligatoria(REGLA4, salida, REGLA4_MAXIMO, "regla 4");
            if (sinMinimo) {
                salida = obligatoria(REGLA17_LLENAR, salida, REGLA17_SIN_MINIMO, "regla 17");
                salida = opcional(SERIE_ADICIONAL, salida, SIN_SERIE_ADICIONAL);
                if (feedback != null && !feedback.isEmpty()) salida = opcional(REINTENTO_MINIMO, salida, "");
            }
            if (volumen != null) {
                var m = REGLA20_FIN.matcher(salida);
                if (!m.find()) throw new IllegalStateException("H2-v2: no esta la regla 20 de PR-1.7");
                salida = m.replaceFirst(java.util.regex.Matcher.quoteReplacement(m.group(1)
                        + (variante == 4 ? H2v4.reglas(volumen, context)
                        : v3 ? H2v3.regla21(volumen) + H2v3.regla22(context) : regla21(volumen))));
                salida = REINTENTO_REGLAS.matcher(salida).replaceAll(v3 ? "las reglas 1 a 22 completas"
                        : "las reglas 1 a 21 completas");
            }
            if (sinLlenado) {
                salida = opcional(CAPACIDADES_LLENAR, salida, CAPACIDADES_SIN_LLENAR);
                salida = PRIORITARIA_LLENAR.matcher(salida).replaceAll(PRIORITARIA_SIN_LLENAR);
                salida = opcional(FUERZA_LLENAR, salida, FUERZA_SIN_LLENAR);
            }
            salida = opcional(COMPROBACION_CAPACIDADES, salida, COMPROBACION_CAPACIDADES_V2);
            if (feedback != null && !feedback.isEmpty()) {
                if (v3) {
                    salida = opcional(REINTENTO_REPONER, salida, H2v3.REINTENTO_REPONER);
                    salida = opcional(H2v3.REINTENTO_SALVO, salida, H2v3.REINTENTO_SALVO_V3);
                    salida = variante == 4 ? H2v4.conPistas(salida, feedback) : H2v3.conPistas(salida, feedback);
                } else {
                    salida = opcional(REINTENTO_REPONER, salida, REINTENTO_REPONER_V2);
                    salida = conPistas(salida, feedback);
                }
            }
            return salida;
        }

        private static String obligatoria(java.util.regex.Pattern p, String texto, String nuevo, String que) {
            var m = p.matcher(texto);
            if (!m.find()) throw new IllegalStateException("H2-v2: no esta la " + que + " de PR-1.7");
            return m.replaceFirst(java.util.regex.Matcher.quoteReplacement(nuevo));
        }

        private static String opcional(java.util.regex.Pattern p, String texto, String nuevo) {
            return p.matcher(texto).replaceFirst(java.util.regex.Matcher.quoteReplacement(nuevo));
        }

        static String regla21(main.web.services.fitsense.planning.domain.services.ContratoReparto.Volumen v) {
            int porEjercicio = 3;
            return ("21. VOLUMEN POR GRUPO MUSCULAR (el backend lo verifica). Sin contar"
                    + " constraints.complementos_no_cuentan ni constraints.volumen_semanal.no_cuentan, suma los"
                    + " planned_sets de cada body_part:\n"
                    + "    - en CADA sesion, como mucho %d series de un mismo body_part (techo_series_sesion);\n"
                    + "    - en la SEMANA entera, como mucho %d series de un mismo body_part (techo_series_semana).\n"
                    + "    Ejemplo: con %d series por ejercicio caben como mucho %d ejercicios del mismo body_part en una"
                    + " sesion. Si dos sesiones de la semana trabajan el mismo grupo, el total de las dos tampoco pasa de"
                    + " %d. objetivo_series_semana es una orientacion por grupo que sustituye a la de los principios:"
                    + " acercate a ella si cabe, nunca pases un techo para alcanzarla.\n")
                    .formatted(v.techoSesion(), v.techoSemana(), porEjercicio, v.techoSesion() / porEjercicio,
                            v.techoSemana());
        }

        /** La comprobacion del reparto, despues de los datos (lo ultimo que lee el modelo antes de escribir). */
        static String comprobacionReparto(PlanGenerationContext context, boolean v3) {
            return comprobacionReparto(context, v3 ? 3 : 2);
        }

        static String comprobacionReparto(PlanGenerationContext context, int variante) {
            return variante == 4 ? H2v4.comprobacion(context)
                    : variante == 3 ? H2v3.comprobacion(context) : comprobacionReparto(context);
        }

        static String comprobacionReparto(PlanGenerationContext context) {
            var volumen = main.web.services.fitsense.planning.domain.services.ContratoReparto.volumen(context);
            var b = new StringBuilder("""

                    COMPROBACION DEL REPARTO, sesion por sesion, antes de escribir el JSON:
                    - Cuenta n, sus ejercicios de trabajo (sin constraints.complementos_no_cuentan). Si la sesion trae
                      max_per_body_part_por_n_trabajo, ningun body_part pasa del valor de n en esa tabla.
                    - Cubre al menos min_body_parts grupos de body_parts_con_material y no mete grupos fuera de
                      body_parts (salvo FULL_BODY).
                    """);
            if (volumen != null)
                b.append(("- Ningun body_part suma mas de %d series directas en la sesion, ni mas de %d en la semana"
                        + " (regla 21).\n").formatted(volumen.techoSesion(), volumen.techoSemana()));
            b.append("""
                    - Si un grupo llega a su tope y no queda otro de body_parts_con_material, la sesion lleva menos
                      ejercicios: es valido. Pasar un tope no lo es.
                    Si algo falla, corrige la semana antes de responder.
                    """);
            return b.toString();
        }

        /** Las pistas de V15 y V26 en el reintento (PistasDeReintento no las tiene y la adaptacion no se toca). */
        static String conPistas(String texto, AiRetryFeedback feedback) {
            var pistas = new java.util.ArrayList<String>();
            if (feedback.rejectionProblems().stream().anyMatch(m -> m.startsWith("V15")))
                pistas.add("V15: cuenta los ejercicios de trabajo n de esa sesion y usa el valor de n en"
                        + " max_per_body_part_por_n_trabajo; reparte entre body_parts_con_material o pon menos"
                        + " ejercicios.");
            if (feedback.rejectionProblems().stream().anyMatch(m -> m.startsWith("V26")))
                pistas.add("V26: en esa sesion (o en la semana) baja series o quita una colocacion de ese body_part; los"
                        + " techos estan en constraints.volumen_semanal.");
            if (pistas.isEmpty()) return texto;
            var bloque = new StringBuilder();
            pistas.forEach(p -> bloque.append("- ").append(p).append('\n'));
            int i = texto.indexOf("\nDonde esta la solucion de cada motivo:\n");
            if (i >= 0) {
                int fin = texto.indexOf("\n\n", i + 2);
                if (fin < 0) fin = texto.length();
                return texto.substring(0, fin) + "\n" + bloque.toString().stripTrailing() + texto.substring(fin);
            }
            int j = texto.lastIndexOf("\nEl backend la rechazo por estos motivos:\n");
            int k = j < 0 ? -1 : texto.indexOf("\n\n", j + 2);
            if (k < 0) return texto + "\nDonde esta la solucion de cada motivo:\n" + bloque;
            return texto.substring(0, k) + "\n\nDonde esta la solucion de cada motivo:\n" + bloque.toString()
                    .stripTrailing() + texto.substring(k);
        }
    }

    /**
     * H2-v3 (PR-2.1): la jerarquia de restricciones sobre PR-2.0 (docs/FitSense/h2_v3/). Cada frase dice lo que el
     * backend verifica y nada mas: no cambia V8C, V15, V20, V23, V24, V25 ni V26.
     */
    static final class H2v3 {

        private H2v3() {}

        /**
         * Regla 21 de PR-2.1. Los techos de V26 son los mismos; la orientacion por grupo se comunica como lo que es en
         * la politica (ControlVolumen.grupoPendiente): una prioridad para decidir a que grupo dar una colocacion MAS,
         * siempre con su dosis completa, nunca una cifra a igualar ni un limite.
         */
        static String regla21(main.web.services.fitsense.planning.domain.services.ContratoReparto.Volumen v) {
            return ("21. VOLUMEN POR GRUPO MUSCULAR (V26, el backend lo verifica). Sin contar"
                    + " constraints.complementos_no_cuentan ni constraints.volumen_semanal.no_cuentan, suma los"
                    + " planned_sets de cada body_part:\n"
                    + "    - en CADA sesion, como mucho %d series de un mismo body_part (techo_series_sesion);\n"
                    + "    - en la SEMANA entera, como mucho %d series de un mismo body_part (techo_series_semana).\n"
                    + "    Estos techos se cumplen eligiendo CUANTAS colocaciones de cada body_part pones y en que"
                    + " sesiones, nunca cambiando la dosis de una colocacion (regla 22).\n"
                    + "    constraints.volumen_semanal.prioridad_series_semana NO es una cifra que alcanzar ni un limite:"
                    + " solo sirve para decidir, cuando una sesion admite otra colocacion, a que grupo darsela (al que"
                    + " mas lejos este de su prioridad). Quedarte por debajo o por encima es valido mientras no pases un"
                    + " techo. Un body_part que no aparece en prioridad_series_semana no tiene prioridad, pero NO esta"
                    + " prohibido: usalo cuando haga falta para repartir (regla 15) o para cubrir un slot o una"
                    + " capacidad. La orientacion de series por grupo de los PRINCIPIOS DE PRESCRIPCION no se aplica:"
                    + " la sustituye esta.\n").formatted(v.techoSesion(), v.techoSemana());
        }

        /** Regla 22: el orden en que se resuelven los choques. Todas se verifican; ninguna se cumple rompiendo otra. */
        static String regla22(PlanGenerationContext context) {
            boolean porClase = main.web.services.fitsense.planning.domain.model.valueobjects.MvpDosePolicy
                    .aplicaA(context.profile().goalType());
            // Lo mismo que verifica V24: con clase_dosis (GAIN_MUSCLE), las series de la clase sin la serie adicional
            // (sin minimo de duracion no se admite); por capacidades, entre series y series_max del codigo.
            boolean sinMinimo = main.web.services.fitsense.planning.domain.services.PlanDraftValidator
                    .minimumSessionSeconds(context) == 0;
            String dosis = porClase
                    ? "de su clase_dosis en constraints.dosis_por_clase: planned_sets = \"series\" ("
                    + (sinMinimo ? "sin la serie adicional" : "la serie adicional solo con la excepcion de las dosis")
                    + "), planned_reps = \"reps\" y rest_seconds = \"descanso_segundos\", copiados tal cual"
                    : "de uno de sus codigos de \"dosis\" en constraints.capacidades.dosis: planned_sets entre \"series\""
                    + " y \"series_max\" de ese codigo, y \"reps\" (o los segundos) y \"descanso_segundos\" tal cual";
            String techos = porClase
                    ? "quita una colocacion de ese grupo o llevala a otra sesion de la semana que aun tenga margen"
                    : "quita una colocacion de ese grupo, llevala a otra sesion de la semana que aun tenga margen o,"
                    + " si su codigo admite un rango, usa menos series sin bajar de \"series\"";
            return ("22. JERARQUIA DE RESTRICCIONES. Todas se verifican; ninguna se cumple rompiendo otra. Cuando"
                    + " dos parecen chocar, se resuelven en este orden:\n"
                    + "    1) DOSIS (V24): cada colocacion lleva EXACTAMENTE la dosis %s. NUNCA cambies series,"
                    + " repeticiones, segundos ni descanso fuera de esa dosis para cumplir otra regla: una dosis recortada"
                    + " se rechaza.\n"
                    + "    2) TECHOS DE SERIES (V26, regla 21): si un body_part se pasa, %s.\n"
                    + "    3) REPARTO (V15, regla 15): cada sesion cubre al menos min_body_parts grupos de"
                    + " body_parts_con_material y ningun body_part pasa de su mitad. Si uno se pasa o falta un grupo,"
                    + " cambia una colocacion del grupo que mas tiene por otra de un grupo de body_parts_con_material"
                    + " que falte o tenga menos (tenga o no prioridad, y respetando la regla 20); si no queda ninguno,"
                    + " quita la colocacion que se pasa.\n"
                    + "    4) COBERTURA (V23 y V25): al quitar o cambiar una colocacion, la semana sigue cubriendo cada"
                    + " slot de constraints.slots_exigibles%s con una colocacion de su dosis. Quita antes una secundaria"
                    + " o una de un grupo o patron que ya este cubierto.\n"
                    + "    5) DURACION (regla 4): ninguna sesion pasa de constraints.max_session_seconds. Si no cabe,"
                    + " quita una colocacion secundaria; nunca acortes descansos ni dosis.\n"
                    + "    Si no puedes cumplirlas todas a la vez, pon menos colocaciones secundarias. No inventes dosis,"
                    + " grupos ni ejercicios: el backend rechaza lo que no cumple.\n")
                    .formatted(dosis, techos, porClase
                            ? " y la capacidad principal HYPERTROPHY (la cubre una colocacion con la dosis exacta de su"
                            + " clase)"
                            : " y cada capacidad de constraints.capacidades.principales_factibles");
        }

        /** La comprobacion final de PR-2.1: dosis y cobertura antes que el reparto. */
        static String comprobacion(PlanGenerationContext context) {
            var volumen = main.web.services.fitsense.planning.domain.services.ContratoReparto.volumen(context);
            var b = new StringBuilder("""

                    COMPROBACION FINAL, en el orden de la regla 22, antes de escribir el JSON:
                    1) Cada colocacion lleva su dosis exacta (series, reps o segundos y descanso), copiada tal cual:
                       ninguna recortada para que quepa otra cosa.
                    """);
            if (volumen != null)
                b.append(("2) Ningun body_part suma mas de %d series directas en una sesion, ni mas de %d en la semana"
                        + " (regla 21).\n").formatted(volumen.techoSesion(), volumen.techoSemana()));
            b.append("""
                    3) Cuenta n, los ejercicios de trabajo de cada sesion (sin constraints.complementos_no_cuentan). Si la
                       sesion trae max_per_body_part_por_n_trabajo, ningun body_part pasa del valor de n en esa tabla; cubre
                       al menos min_body_parts grupos de body_parts_con_material y no mete grupos fuera de body_parts
                       (salvo FULL_BODY).
                    4) La semana cubre cada slot de constraints.slots_exigibles y cada capacidad principal factible.
                    5) Ninguna sesion pasa de constraints.max_session_seconds.
                    Una sesion con menos ejercicios es valida si cumple 1-5; pasar un techo, recortar una dosis o perder
                    un slot no lo es. Si algo falla, corrige la semana antes de responder.
                    """);
            return b.toString();
        }

        /** Reintento: sin "bajar series". Se quitan o mueven colocaciones; la dosis no se toca. */
        static final String REINTENTO_REPONER = "Si sobran de un grupo muscular (V15 o V26), cambia esas colocaciones"
                + " por otras de body_parts_con_material de esa sesion o llevalas a otra sesion con margen; si no"
                + " cabe en ninguna, quitalas sin reponerlas. NUNCA cambies la dosis de una colocacion para corregir"
                + " (regla 22).";

        static final java.util.regex.Pattern REINTENTO_SALVO = java.util.regex.Pattern.compile(
                "NO quites ejercicios ni sesiones para corregir, SALVO que un motivo\\s+del rechazo exija bajar el"
                        + " numero de ejercicios o las series: en ese\\s+caso quitalos y conserva lo demas\\.");
        static final String REINTENTO_SALVO_V3 = "NO quites ejercicios ni sesiones para corregir, SALVO que un motivo"
                + " del rechazo exija menos colocaciones de un grupo (V15, V26) o menos duracion: en ese caso quita"
                + " colocaciones y conserva lo demas, sin tocar la dosis de ninguna.";

        /** Las pistas de PR-2.1 para el reintento: cada una dice como corregir sin romper otra regla. */
        static String conPistas(String texto, AiRetryFeedback feedback) {
            var motivos = feedback.rejectionProblems();
            var pistas = new java.util.ArrayList<String>();
            if (motivos.stream().anyMatch(m -> m.startsWith("V15")))
                pistas.add("V15: cuenta los ejercicios de trabajo n de esa sesion y usa el valor de n en"
                        + " max_per_body_part_por_n_trabajo; cambia la colocacion que sobra por otra de"
                        + " body_parts_con_material (con su dosis exacta) o pon menos ejercicios.");
            if (motivos.stream().anyMatch(m -> m.startsWith("V26")))
                pistas.add("V26: quita una colocacion de ese body_part o llevala a otra sesion con margen (regla 22,"
                        + " punto 2); no cambies ninguna dosis fuera de lo que admite. Los techos estan en"
                        + " constraints.volumen_semanal.");
            if (motivos.stream().anyMatch(m -> m.startsWith("V24")))
                pistas.add("V24: vuelve a copiar la dosis exacta de cada colocacion (regla 22, punto 1). Si con la"
                        + " dosis completa se pasa un techo o la duracion, quita una colocacion secundaria en vez de"
                        + " recortarla.");
            if (motivos.stream().anyMatch(m -> m.startsWith("V25")))
                pistas.add("V25: anade una colocacion con una dosis de esa capacidad; si no cabe, cambiala por una"
                        + " secundaria, no recortes dosis.");
            if (motivos.stream().anyMatch(m -> m.startsWith("V23")))
                pistas.add("V23: cambia un ejercicio de ese mismo grupo por uno del slot que falta, con la misma"
                        + " dosis; no quites el ultimo ejercicio de otro slot.");
            if (pistas.isEmpty()) return texto;
            var bloque = new StringBuilder();
            pistas.forEach(p -> bloque.append("- ").append(p).append('\n'));
            int i = texto.indexOf("\nDonde esta la solucion de cada motivo:\n");
            if (i >= 0) {
                int fin = texto.indexOf("\n\n", i + 2);
                if (fin < 0) fin = texto.length();
                return texto.substring(0, fin) + "\n" + bloque.toString().stripTrailing() + texto.substring(fin);
            }
            int j = texto.lastIndexOf("\nEl backend la rechazo por estos motivos:\n");
            int k = j < 0 ? -1 : texto.indexOf("\n\n", j + 2);
            if (k < 0) return texto + "\nDonde esta la solucion de cada motivo:\n" + bloque;
            return texto.substring(0, k) + "\n\nDonde esta la solucion de cada motivo:\n" + bloque.toString()
                    .stripTrailing() + texto.substring(k);
        }
    }

    /**
     * H2-v4 (PR-2.2): PR-2.1 compactada. Las mismas cinco prioridades y los mismos limites, con menos texto y sin
     * cruces de listas: lo que no cuenta va marcado en cada ejercicio y la mitad de V15 es una regla, no una tabla.
     * No cambia V8C, V15, V20, V23, V24, V25 ni V26.
     */
    static final class H2v4 {

        private H2v4() {}

        /** Regla 15 (V15), en una pieza: recuento de trabajo, minimo de grupos y la mitad. */
        static final String REGLA15 = "cubre al menos min_body_parts body_parts distintos de body_parts_con_material"
                + " de esa sesion y no metas grupos fuera de body_parts (salvo FULL_BODY). Solo cuentan los ejercicios"
                + " de trabajo: los que traen \"cuenta\": \"complemento\" (cardio y movilidad) no cubren ningun"
                + " grupo ni cuentan para esta regla. Si la sesion trae \"regla_mitad\": true y tiene 4 o mas"
                + " ejercicios de trabajo, ningun body_part pasa de la mitad de ellos, redondeando hacia abajo (4 o 5:"
                + " 2; 6 o 7: 3; 8: 4). Si un grupo llega a su tope, sigue con otro de body_parts_con_material, tenga"
                + " o no prioridad; si no queda ninguno, pon menos ejercicios.";

        /** Reglas 21 (V26 y prioridad) y 22 (orden de los choques), sin repetir la regla 15. */
        static String reglas(main.web.services.fitsense.planning.domain.services.ContratoReparto.Volumen v,
                             PlanGenerationContext context) {
            boolean porClase = main.web.services.fitsense.planning.domain.model.valueobjects.MvpDosePolicy
                    .aplicaA(context.profile().goalType());
            boolean sinMinimo = main.web.services.fitsense.planning.domain.services.PlanDraftValidator
                    .minimumSessionSeconds(context) == 0;
            String dosis = porClase
                    ? "la de su clase_dosis (series" + (sinMinimo ? ", sin la serie adicional" : "")
                    + ", reps y descanso_segundos)"
                    : "una de sus \"dosis\" (series entre series y series_max; reps o segundos y descanso_segundos)";
            String cobertura = porClase ? "y HYPERTROPHY" : "y de capacidades.principales_factibles";
            return ("21. SERIES POR GRUPO (V26). Suma los planned_sets de cada body_part sin los ejercicios que traen"
                    + " \"cuenta\": como mucho %d por sesion y %d por semana. Se cumple eligiendo cuantas colocaciones"
                    + " de cada grupo pones, nunca cambiando una dosis. prioridad_series_semana solo decide a que grupo"
                    + " dar una colocacion mas: no es una cifra que alcanzar, y un grupo que no aparece esta permitido."
                    + " La orientacion de series de los principios no se aplica.\n"
                    + "22. TODAS LAS REGLAS SON OBLIGATORIAS. Si dos chocan, resuelvelo en este orden sin romper"
                    + " ninguna: 1) dosis exacta (V24): %s, copiada tal cual, nunca recortada; 2) series por grupo"
                    + " (regla 21): quita o mueve colocaciones; 3) reparto (regla 15): cambia la colocacion que sobra"
                    + " por otra de un grupo de body_parts_con_material; 4) cobertura (V23, V25): no quites la ultima"
                    + " colocacion de un slot de slots_exigibles %s; 5) duracion: ninguna sesion pasa de"
                    + " max_session_seconds; quita una secundaria. Si aun asi no cabe, pon menos secundarias; no"
                    + " inventes dosis, grupos ni ejercicios.\n").formatted(v.techoSesion(), v.techoSemana(), dosis,
                    cobertura);
        }

        /** La comprobacion final, en un parrafo. */
        static String comprobacion(PlanGenerationContext context) {
            return "\nCOMPROBACION FINAL antes de escribir el JSON: cada colocacion con su dosis exacta; en cada"
                    + " sesion, las series por grupo de la regla 21, min_body_parts grupos de trabajo y la mitad de la"
                    + " regla 15; en la semana, todos los slots y capacidades de la regla 22. Si algo falla, corrige la"
                    + " semana antes de responder.\n";
        }

        /** Las pistas de PR-2.2 para el reintento (las de PR-2.1, sin la tabla y mas cortas). */
        static String conPistas(String texto, AiRetryFeedback feedback) {
            var motivos = feedback.rejectionProblems();
            var pistas = new java.util.ArrayList<String>();
            if (motivos.stream().anyMatch(m -> m.startsWith("V15")))
                pistas.add("V15: cuenta solo los ejercicios de trabajo de esa sesion (sin \"cuenta\": \"complemento\");"
                        + " cambia la colocacion que sobra por otra de body_parts_con_material, con su dosis exacta, o pon"
                        + " menos ejercicios.");
            if (motivos.stream().anyMatch(m -> m.startsWith("V26")))
                pistas.add("V26: quita una colocacion de ese body_part o llevala a otra sesion con margen; no cambies"
                        + " ninguna dosis.");
            if (motivos.stream().anyMatch(m -> m.startsWith("V24")))
                pistas.add("V24: vuelve a copiar la dosis exacta de cada colocacion; si con ella no cabe, quita una"
                        + " secundaria en vez de recortarla.");
            if (motivos.stream().anyMatch(m -> m.startsWith("V25")))
                pistas.add("V25: anade una colocacion con una dosis de esa capacidad, o cambiala por una secundaria.");
            if (motivos.stream().anyMatch(m -> m.startsWith("V23")))
                pistas.add("V23: cambia un ejercicio de ese grupo por uno del slot que falta, con la misma dosis.");
            if (pistas.isEmpty()) return texto;
            var bloque = new StringBuilder();
            pistas.forEach(p -> bloque.append("- ").append(p).append('\n'));
            int i = texto.indexOf("\nDonde esta la solucion de cada motivo:\n");
            if (i >= 0) {
                int fin = texto.indexOf("\n\n", i + 2);
                if (fin < 0) fin = texto.length();
                return texto.substring(0, fin) + "\n" + bloque.toString().stripTrailing() + texto.substring(fin);
            }
            int j = texto.lastIndexOf("\nEl backend la rechazo por estos motivos:\n");
            int k = j < 0 ? -1 : texto.indexOf("\n\n", j + 2);
            if (k < 0) return texto + "\nDonde esta la solucion de cada motivo:\n" + bloque;
            return texto.substring(0, k) + "\n\nDonde esta la solucion de cada motivo:\n" + bloque.toString()
                    .stripTrailing() + texto.substring(k);
        }
    }
}
