package main.web.services.fitsense.p2d;

import jakarta.persistence.EntityManager;
import main.web.services.fitsense.planning.domain.exceptions.AiProviderUnavailableException;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptedWeekResult;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdjustmentType;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.CorrectedSession;
import main.web.services.fitsense.planning.domain.model.valueobjects.GeneratedPlan;
import main.web.services.fitsense.planning.domain.model.valueobjects.GenerationSource;
import main.web.services.fitsense.planning.domain.model.valueobjects.MandatorySlot;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.PreparedPlanGeneration;
import main.web.services.fitsense.planning.domain.model.valueobjects.PreviousWeekSummary;
import main.web.services.fitsense.planning.domain.model.valueobjects.MvpDosePolicy;
import main.web.services.fitsense.planning.domain.model.valueobjects.WeekDerivation;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.planning.domain.services.AdaptedWeekProducer;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.SlotCoveragePolicy;
import main.web.services.fitsense.planning.domain.services.WeekDerivationBuilder;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.generation.ai.AiRetryFeedback;
import main.web.services.fitsense.planning.infrastructure.generation.ai.PlanInputSnapshot;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.infrastructure.generation.rules.RuleBasedTrainingPlanGenerator;
import main.web.services.fitsense.planning.infrastructure.generation.ai.SessionFixPromptBuilder;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * P2-D · CORRIDA DE EVIDENCIA · UNA CORRECCION REAL DE REPLICATE. · <b>v16</b>
 *
 * <h2>QUE CAMBIA EN LA v16</h2>
 *
 * <p><b>Cerrojo de gasto para INC-P2D-02:</b> antes de cualquier llamada
 * se verifica contra el prompt REAL que cada DURATION de una sesion senalada
 * con series no nulas incluya sus series, segundos y descanso. Se busca dentro
 * de ESA sesion y se consume cada linea una sola vez para soportar repeticiones.
 * Sin al menos una DURATION verificable el escenario no sirve como evidencia
 * de esta incidencia. No se fija ningun ID ni se modifica produccion.</p>
 *
 * <h2>QUE CAMBIA EN LA v15</h2>
 *
 * <b>La linea «lo que el prompt le ensena» se lee del prompt, no de una
 * copia.</b> La v14 reconstruia a mano la logica de
 * {@code SessionFixPromptBuilder.describir()}, y al aplicar FIX-PR-1.3 esa
 * copia se quedo atras: el acta de las 17:23 seguia anunciando «40 s,
 * descanso 45 s» para el 1077 cuando el builder ya imprimia otra cosa. La
 * linea no probaba nada y encima mentia. Ahora {@link #lineaDelPrompt}
 * extrae el texto literal del prompt construido con el mismo bean que usa el
 * generador, y dice «NO APARECE» cuando el ejercicio no esta.
 *
 * <h2>QUE CAMBIA EN LA v14</h2>
 *
 * <b>El censo se imprime entero antes de decidir.</b> En la v13 vivia dentro
 * del bucle que busca la retirada, asi que al encontrarla el metodo volvia y
 * las colocaciones posteriores no se censaban nunca. La corrida de las 15:48
 * lo enseno: la sesion 95 tiene seis colocaciones, la viable estaba en la
 * posicion 1, y el acta solo trajo las dos primeras —justo sin el 1077, que es
 * la DURATION del caso—. Ahora {@link #censar} recorre la sesion completa y la
 * busqueda decide despues.
 *
 * <h2>QUE CAMBIA EN LA v13</h2>
 *
 * <b>El censo dice la dosis completa, y de las dos fuentes.</b> La corrida
 * pagada del 22/10 devolvio tres veces la retirada correcta y fue rechazada
 * tres veces por «cambia la dosis de [1077]», una colocacion DURATION para la
 * que el modelo mando {@code planned_sets: null}. Ahora cada colocacion
 * aparece con sus cinco campos tal y como los ve V8C
 * -{@code previous.prescriptions()}, que es lo que indexa
 * {@code ConservationValidator.indexarDosis}- junto a los de la derivacion y
 * junto a la linea que el prompt le ensena al modelo.
 * <p>
 * <b>Y se reproduce el fallo sin gastar nada.</b>
 * {@link #reproducirElFalloDelModelo} repite la retirada que acaba de salir
 * publicable, poniendo a null las series de las colocaciones DURATION que se
 * quedan. Si eso la tumba, el rechazo del 22/10 se explica por un dato que el
 * prompt no da y no por una regla que el modelo incumpla. No afirma nada: deja
 * el resultado en el acta.
 *
 * <h2>QUE CAMBIA EN LA v12</h2>
 *
 * <b>Se comprueba que el prompt que va a viajar lleva las reglas 15 y 16.</b>
 * {@link #exigirPromptConLaRetirada} lo construye con el mismo bean y los
 * mismos cuatro argumentos que el generador, y exige los marcadores de las
 * dos reglas sobre el texto normalizado. El cerrojo de la retirada demuestra
 * que existe una salida legal; este demuestra que el modelo va a enterarse de
 * que puede tomarla. Tambien es el cerrojo contra un {@code target} con la
 * clase anterior a {@code FIX-PR-1.2}.
 *
 * <h2>QUE CAMBIA EN LA v11</h2>
 *
 * <b>La sesion senalada se elige por viabilidad, no por orden.</b> El pre-vuelo
 * del 22/10 encontro que la primera sesion sembrada —la 79 del participante 2—
 * tenia sus cinco colocaciones con slot: todas estructurales. La semana SI tenia
 * rellenos, {@code fuera_de_slot=5} de 17 posiciones, pero en las otras dos
 * sesiones. Ahora se recorren las tres, cada una con su propia fase 1, y se
 * elige la primera cuya retirada supera el {@code AdaptedWeekProducer} de
 * verdad; el acta advierte expresamente que es un caso elegido por viabilidad y
 * no una muestra representativa.
 * <p>
 * <b>La retirada simulada va por indice.</b> La v10 construia el resto con
 * {@code filter(e -> !id.equals(e.exerciseId()))}, que quita TODAS las
 * apariciones de ese ejercicio: con el mismo {@code exercise_id} en dos
 * colocaciones retiraba dos, y V8C solo autoriza una.
 * <p>
 * <b>El acta se escribe una sola vez.</b> {@code volcarActa()} creaba un archivo
 * nuevo con marca de tiempo en cada llamada, y la corrida del 22/10 dejo dos:
 * el citado en el mensaje del fallo era el mas corto. Ahora memoriza la ruta y
 * reescribe ese mismo archivo.
 *
 * <h2>QUE CAMBIA EN LA v9</h2>
 *
 * <b>No se paga hasta haber probado gratis que la correccion es posible.</b> La
 * corrida del 21/10 gasto tres peticiones para descubrir un obstaculo que no
 * estaba en el modelo sino en las reglas (INC-P2D-01: para {@code GAIN_MUSCLE}
 * con 4D en {@code ENFORCE}, {@code ACORTAR_O_REORGANIZAR} no tenia ningun
 * movimiento legal). Ahora, antes de la primera llamada:
 * <ul>
 *   <li><b>Se exige el modo que activa V24.</b> Se lee
 *       {@code validador.doseEnforcement()} del bean real y se exige
 *       {@code ENFORCE}: con {@code OBSERVE} V24 mide pero no rechaza, y el
 *       verde no probaria que el conflicto esta resuelto porque no se habria
 *       reproducido.</li>
 *   <li><b>Se exige el objetivo que activa V24.</b> Los candidatos cuyo perfil
 *       no cumple {@code MvpDosePolicy.aplicaA(goalType())} se descartan sin
 *       generar nada: con otro objetivo
 *       {@code PlanInputSnapshot} devuelve {@code SIN_ETIQUETAS} y el prompt no
 *       puede ni identificar los rellenos.</li>
 *   <li><b>Se exige una retirada viable</b> —{@link #retiradaViableEn}—,
 *       simulando cada candidata con el {@code AdaptedWeekProducer} de verdad.
 *       Tener un relleno es necesario y no suficiente.</li>
 *   <li><b>Dos cerrojos de gasto independientes:</b>
 *       {@code -Dfitsense.evidencia.solo.prevuelo=si} para pararse a proposito,
 *       y {@code -Dfitsense.evidencia.pago.confirmado=AUTORIZO}, sin la cual no
 *       se llama a Replicate aunque se olvide la primera.</li>
 * </ul>
 * Tambien se corrige el desenlace: al agotar los intentos se devuelve el
 * respaldo {@code producer.produce(..., null, ...)}, igual que
 * {@code AdaptedWeekCommandService:300}, y no el ultimo resultado rechazado.
 *
 * <h2>QUE CAMBIA EN LA v6</h2>
 *
 * <b>La semana medida ya no se construye a mano: la produce FitSense.</b> El
 * pre-vuelo de la v5 encontro once problemas en mi siembra artesanal —V15 por
 * trabajar un solo grupo muscular, V17 y V20 por declarar 45 minutos con 15 de
 * contenido, V24 por poner 3x10 donde 4D v3 pide 2x10 de relleno—. Acertar todo
 * eso a mano seria escribir una segunda version del motor de prescripcion.
 * <p>
 * Ahora la arma {@link RuleBasedTrainingPlanGenerator} y la guarda
 * {@code persist} de produccion: es valida por construccion, y ademas es una
 * semana que FitSense habria producido de verdad, asi que lo que se le pide
 * corregir a la IA es un caso real y no un montaje. De paso desaparece el
 * calculo de {@code week_number} de la v5, porque {@code persist} lo resuelve
 * con la logica que conoce {@code uq_plan_user_week_version}.
 *
 * <h2>QUE CAMBIA EN LA v5</h2>
 *
 * <b>El numero de semana de la siembra se continua, no se fija en 1.</b>
 * {@code uq_plan_user_week_version} es UNIQUE sobre
 * {@code (user_id, week_number, plan_version)} y NO sobre
 * {@code week_start_date}, asi que el cerrojo de «semanas libres» —que mira
 * fechas— no podia detectarlo: el participante elegido ya tenia un plan con
 * {@code week_number = 1}. Ahora se toma {@code MAX(week_number) + 1} de ese
 * participante, que es lo que esa secuencia significa.
 *
 * <h2>QUE CAMBIA EN LA v4</h2>
 *
 * <b>El participante se descubre, no se elige a mano.</b> La v3 usaba el usuario
 * 1 y la corrida se cayo con «El usuario 1 no esta activo en el estudio». Poner
 * otro identificador a ojo habria sido repetir el error por tercera vez: desde
 * fuera no se puede saber quien cumple todas las condiciones de la fase 1. Ahora
 * se filtran por SQL las de IAM y las dos semanas libres, y se prueba
 * {@code prepare} con cada candidato hasta que uno pasa; el acta registra por que
 * cayo cada descartado, que tambien es un dato de la cohorte.
 *
 * <h2>QUE CAMBIA EN LA v3</h2>
 *
 * <ol>
 *   <li><b>La politica de reintentos ya es la de produccion.</b> La v2
 *       reintentaba cuando el modelo devolvia una lista vacia; produccion no:
 *       pasa la lista vacia a {@code produce}, que la lleva a
 *       SIN_EFECTO/CONSERVED y, si la semana vale, a publicable, y el orquestador
 *       vuelve en cuanto es publicable. <b>Un verde de la v2 podia venir de una
 *       segunda peticion que el backend real nunca habria hecho.</b>
 *   <li><b>El interruptor del gasto exige exactamente {@code si}.</b> Con
 *       {@code matches = ".+"}, un {@code =no} autorizaba las llamadas de pago.
 *   <li><b>La siembra solo usa candidatos {@code SETS_REPS}</b>, porque es lo
 *       que prescribe; uno cuyo tipo por defecto fuera {@code DURATION} quedaria
 *       incoherente con su propio catalogo.
 *   <li>El acta describe con precision que demuestra la igualdad de
 *       proyecciones, que no es lo mismo que comparar el prompt entero.
 * </ol>
 *
 * <h2>ESTO NO ES UNA PRUEBA DE REGRESION, Y LA DISTINCION ES DE FONDO</h2>
 *
 * Lo que aqui se afirma depende de lo que conteste un modelo, no solo del
 * codigo. Asi que no entra en ninguna suite —lleva {@code ...IT} y DOS
 * propiedades de sistema, una exclusiva—, gasta dinero y tiempo real, y <b>el
 * numero de intentos y de rechazos ES DATO REPORTABLE</b>, no ruido que haya que
 * esconder para ver verde. Montarla como prueba repetible seria peor que no
 * tenerla: un rojo por un mal dia del modelo contaminaria la unica evidencia que
 * importa.
 *
 * <h2>QUE CAMBIA EN LA v2</h2>
 *
 * Cuatro defectos de la v1, los dos primeros graves:
 * <ol>
 *   <li><b>La semana sembrada no podia pasar la validacion.</b> Llevaba UN
 *       ejercicio por sesion y {@code PlanDraftValidator} exige
 *       {@code MIN_EXERCISES_PER_SESSION = 2}. La cadena es real y la comprobe:
 *       {@code AdaptedWeekProducer} → {@code estricta.revisar(...)} →
 *       {@code StrictValidationAdapter} → {@code PlanDraftValidator}. Y valida la
 *       semana ENTERA, no solo la sesion corregida, asi que las tres habrian
 *       fallado. <b>Habria gastado hasta tres llamadas de pago para descubrir un
 *       defecto de mi siembra.</b> Ahora van dos ejercicios por sesion y, sobre
 *       todo, hay un PRE-VUELO (ver abajo).
 *   <li><b>Confundia una respuesta valida con una ruptura del contrato.</b>
 *       {@code SessionFixFromJsonAssembler} hace
 *       {@code if (parsed.sessions().isEmpty()) return List.of();}: una lista
 *       vacia esta EN contrato y significa «no propongo cambios». La v1 la
 *       trataba como incumplimiento.
 *       <p>
 *       Y hay mas: el ensamblador YA rechaza una sesion no senalada
 *       —{@code "La sesion %d no estaba senalada"}, con
 *       {@code InvalidPlanDraftException}—. Asi que la igualdad que la v1
 *       comprobaba sobre la lista devuelta <b>no podia detectar nunca</b> la
 *       ruptura que decia vigilar: el ensamblador lanza antes. La evidencia real
 *       del contrato A esta en ESE camino de excepcion, y aqui se registra
 *       aparte.
 *   <li><b>El acta se perdia.</b> {@code volcarActa()} no corria en todos los
 *       caminos de excepcion, asi que un fallo posterior al envio podia tirar la
 *       respuesta de una llamada ya pagada. Ahora va en un {@code finally}.
 *   <li><b>No guardaba el prompt.</b> Solo la proyeccion. Ahora tambien el texto
 *       efectivo (ver abajo).
 * </ol>
 *
 * <h2>EL PRE-VUELO: LA SIEMBRA NO TIENE QUE SER CORRECTA POR ADIVINACION</h2>
 *
 * Antes de cualquier llamada se ejecuta
 * {@code producer.produce(context, derivation, List.of(), elegibles)}: la semana
 * derivada SIN tocar nada. Todos los caminos de {@code produce} desembocan en
 * {@code revisarYCerrar}, que siempre llama a {@code estricta.revisar(...)}, asi
 * que eso ejecuta <b>V5, V15, V16, V17, V20 y la dosis de 4D de verdad</b>,
 * gratis y en milisegundos.
 * <p>
 * Si la semana sembrada no pasa, la corrida se detiene con los problemas exactos
 * y <b>sin haber gastado una sola peticion</b>. No hace falta que yo acierte a
 * ciegas las dosis, la duracion y el reparto muscular: el validador autentico lo
 * dice antes de pagar, y corregir la siembra cuesta cero.
 *
 * <h2>LOS TRES DESENLACES, SEPARADOS</h2>
 *
 * <table border="1">
 *   <tr><th>Desenlace</th><th>Como se reconoce</th><th>Veredicto</th></tr>
 *   <tr><td>Fuera de contrato</td>
 *       <td>{@code InvalidPlanDraftException} cuyos problemas mencionan que la
 *           sesion no estaba senalada</td>
 *       <td>el modelo rompio el contrato A; el ensamblador lo paro</td></tr>
 *   <tr><td>Valida sin efecto</td>
 *       <td>{@code interpretar} devuelve lista VACIA</td>
 *       <td>no propuso cambios; NO es incumplimiento, y produccion NO reintenta</td></tr>
 *   <tr><td>Adaptacion lograda</td>
 *       <td>corrige una sesion senalada y pasa la validacion</td>
 *       <td>lo unico que aprueba esta corrida</td></tr>
 * </table>
 *
 * <h2>EL PROMPT EFECTIVO, Y POR QUE ES COMPROBABLEMENTE EL MISMO</h2>
 *
 * {@code pedirRespuesta} devuelve la proyeccion y la salida, pero no el texto
 * enviado. Para no tocar produccion, el acta lo reconstruye con el MISMO bean y
 * el MISMO metodo que usa el generador —{@code promptBuilder.build(context,
 * derivation, full, feedback)}, las mismas cuatro cosas.
 * <p>
 * LO QUE ESA IGUALDAD DEMUESTRA, Y LO QUE NO. Se compara el
 * {@code snapshotJson} de la reconstruccion con el de la llamada real, y
 * coincidir por casualidad es imposible porque la proyeccion lleva el contexto
 * entero. Pero eso comprueba <b>la proyeccion, no el texto completo del
 * prompt</b>: el prompt se arma de esos mismos cuatro argumentos con un
 * constructor determinista, asi que la garantia es solida por construccion y no
 * por comparacion directa. El acta lo dice con esas palabras, para que nadie lo
 * lea como mas de lo que es.
 *
 * <h2>NO ESCRIBE NI PUBLICA NADA, Y NO POR HABERLO INTERCEPTADO</h2>
 *
 * No se inyecta {@code AdaptedWeekCommandService} ni
 * {@code AdaptationFailureRecorder}: no se puede publicar ni registrar un fallo
 * desde aqui. Interceptar con un doble no servia, porque {@code prepare} y
 * {@code publishAdapted} viven en la MISMA interfaz y silenciar la publicacion
 * dejaria muda la fase 1. <b>No escribir porque no se llama a nada que escriba
 * es una garantia mas fuerte que haber interceptado las escrituras que uno
 * recordaba.</b> Lo unico que se escribe es la siembra, que se borra al final
 * por identificador.
 *
 * <h2>Como se ejecuta</h2>
 * <pre>
 * mvn "-Dtest=CorreccionReplicateEvidenciaIT" test "-Dfitsense.evidencia.replicate=si" ^
 *     "-Dfitsense.pruebas.url=jdbc:postgresql://&lt;host&gt;/neondb?sslmode=require" ^
 *     "-Dfitsense.pruebas.usuario=neondb_owner" "-Dfitsense.pruebas.clave=&lt;clave&gt;" ^
 *     "-Dlogging.level.main.web.services.fitsense.planning.infrastructure.generation.ai=DEBUG"
 * </pre>
 * El DEBUG es lo que captura {@code "Replicate respondio (crudo): {}"}. El token
 * sale de {@code fitsense.ai.api-token}; si ahi esta como {@code ${...}}, una
 * consola limpia lo necesita y entonces se pasa
 * {@code "-Dfitsense.evidencia.replicate.token=<token>"}. El token no se escribe
 * en ningun log ni en el acta.
 */
@EnabledIfSystemProperty(named = "fitsense.pruebas.url", matches = ".+")
// EXACTAMENTE "si", no cualquier valor no vacio: con ".+" un
// -Dfitsense.evidencia.replicate=no habria autorizado el gasto.
@EnabledIfSystemProperty(named = "fitsense.evidencia.replicate", matches = "si")
@SpringBootTest
@DisplayName("P2-D · corrida de evidencia · correccion real de Replicate (v16)")
class CorreccionReplicateEvidenciaIT {

    private static final Logger log =
            LoggerFactory.getLogger(CorreccionReplicateEvidenciaIT.class);

    /**
     * EL PARTICIPANTE NO SE ELIGE A MANO, SE DESCUBRE. Y es la correccion de la
     * v4, despues de que la corrida se cayera con «El usuario 1 no esta activo en
     * el estudio».
     * <p>
     * Elegir otro identificador a ojo habria sido repetir el error: desde aqui no
     * se puede saber quien cumple TODAS las condiciones de la fase 1 -activo,
     * participante con consentimiento y sin retirada, con perfil, con conjunto
     * elegible suficiente y con 4C cubierto-. Replicarlas en SQL seria una
     * segunda version de esas reglas, que es justo lo que se separa de ellas con
     * el tiempo.
     * <p>
     * Asi que se filtran por SQL solo las condiciones de IAM —que son las que
     * {@code prepare} comprueba primero y las unicas estables— y despues se
     * prueba {@code prepare} con cada candidato hasta que uno pasa. Es una
     * consulta de solo lectura de milisegundos, por su propio javadoc, y el acta
     * registra por que se descarto cada uno: eso tambien es un dato de la
     * cohorte.
     */
    private long participante;

    /** Cuantos candidatos se prueban antes de rendirse. */
    private static final int CANDIDATOS = 10;
    private static final LocalDate SEMANA_MEDIDA = LocalDate.of(2026, 12, 14);
    private static final LocalDate SEMANA_NUEVA = LocalDate.of(2026, 12, 21);

    private static final int INTENTOS = 3;


    @DynamicPropertySource
    static void apuntarAPruebas(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", () -> System.getProperty("fitsense.pruebas.url"));
        registro.add("spring.datasource.username", () -> System.getProperty("fitsense.pruebas.usuario"));
        registro.add("spring.datasource.password", () -> System.getProperty("fitsense.pruebas.clave"));
        registro.add("spring.flyway.enabled", () -> "false");
        registro.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registro.add("authorization.jwt.secret",
                () -> "secreto-solo-para-levantar-el-contexto-de-P2-D-no-se-usa!");

        // LA IA ENCENDIDA: es la diferencia con las otras tres clases de P2-D.
        registro.add("fitsense.ai.enabled", () -> "true");

        String token = System.getProperty("fitsense.evidencia.replicate.token");
        if (token != null && !token.isBlank())
            registro.add("fitsense.ai.api-token", () -> token);
    }

    @Autowired private WeeklyTrainingPlanCommandService planCommandService;
    @Autowired private ReplicateSessionFixGenerator fixGenerator;
    @Autowired private SessionFixPromptBuilder promptBuilder;
    @Autowired private WeekDerivationBuilder derivationBuilder;
    @Autowired private AdaptedWeekProducer producer;
    @Autowired private SessionDurationEstimator estimador;
    @Autowired private RuleBasedTrainingPlanGenerator reglas;
    @Autowired private PlanDraftValidator validador;
    @Autowired private EntityManager em;
    @Autowired private PlatformTransactionManager transactionManager;

    private TransactionTemplate tx;

    private final List<Long> planesCreados = new ArrayList<>();
    private boolean entornoVerificado;

    private Long planOrigenId;
    private final List<Long> sesionesOrigen = new ArrayList<>();

    /** Todo lo que hay que dejar por escrito, pase lo que pase. */
    private final StringBuilder acta = new StringBuilder();

    /** La ruta del acta de ESTA corrida. Se fija en el primer volcado. */
    private Path rutaActa;

    // =======================================================================
    // Cerrojos
    // =======================================================================

    @BeforeEach
    void exigirEntornoDePruebas() {
        tx = new TransactionTemplate(transactionManager);
        planesCreados.clear();
        sesionesOrigen.clear();
        planOrigenId = null;
        entornoVerificado = false;
        rutaActa = null;

        var marcas = enTransaccion(() -> em.createNativeQuery(
                        "SELECT entorno FROM fitsense_entorno WHERE entorno = 'PRUEBAS'")
                .getResultList());

        assertFalse(marcas.isEmpty(),
                "Esta base no lleva la marca PRUEBAS en fitsense_entorno. O no es"
                        + " FitSenseTests, o la marca no se creo. NO se escribe nada.");

        // LA COMPROBACION DE SEMANAS LIBRES YA NO VIVE AQUI, y no es un descuido:
        // en este momento todavia no se sabe QUIEN sera el participante. Va
        // dentro de la consulta que lo elige, como dos NOT EXISTS, asi que solo
        // puede salir elegido alguien con las dos semanas vacias. Sigue
        // cumpliendose la regla: esta corrida no borra lo que no creo.

        assertTrue(fixGenerator.isEnabled(),
                "Replicate no esta utilizable: falta fitsense.ai.enabled=true o el"
                        + " token. Si application.properties lo toma del entorno, pasa"
                        + " -Dfitsense.evidencia.replicate.token=<token>. NO se siembra"
                        + " nada.");

        entornoVerificado = true;
    }

    // =======================================================================
    // La corrida
    // =======================================================================

    @Test
    @DisplayName("Replicate corrige SOLO la sesion senalada, y la semana pasa la validacion")
    void unaCorreccionRealDeReplicate() {
        try {
            correr();
        } finally {
            // EN finally, Y ES EL ARREGLO DE UN DEFECTO REAL DE LA v1: un fallo
            // posterior al envio tiraba la respuesta de una llamada ya pagada.
            log.info("ACTA DE LA CORRIDA DE EVIDENCIA: {}", volcarActa());
        }
    }

    private void correr() {
        anotar("=== CORRIDA DE EVIDENCIA v16 · " + java.time.OffsetDateTime.now() + " ===");
        anotar("modelo declarado: " + fixGenerator.modelName());

        // ---------------------------------------------------------------
        // 1 · Quien puede ser el participante, y que le es elegible DE VERDAD
        // ---------------------------------------------------------------
        var semilla = elegirParticipanteYGenerarSemanaMedida();

        // ---------------------------------------------------------------
        // 2 · La semana medida la guarda persist() de produccion
        // ---------------------------------------------------------------
        sembrar(semilla);

        assertTrue(sesionesOrigen.size() >= 2,
                "la semana medida quedo con " + sesionesOrigen.size() + " sesion(es) y"
                        + " hacen falta al menos dos: con una sola, comprobar que las NO"
                        + " senaladas llegan intactas no afirmaria nada");

        // ---------------------------------------------------------------
        // 3 · PRE-VUELO Y CERROJO: que sesion se senala, y se comprueba gratis
        // ---------------------------------------------------------------
        var escenario = elegirEscenarioViable();
        long senalada = escenario.senalada();
        var intactas = escenario.intactas();
        var context = escenario.context();
        var derivation = escenario.derivation();
        var elegibles = escenario.elegibles();

        // MODO SOLO PRE-VUELO. Con -Dfitsense.evidencia.solo.prevuelo=si la
        // corrida termina aqui aunque todo haya salido bien, para poder leer el
        // acta y decidir despues si se autoriza el gasto.
        //
        // Se ABORTA, no se pasa: JUnit lo marca como saltada y no como una
        // corrida de evidencia exitosa, que es lo que seria si devolviera verde.
        if ("si".equals(System.getProperty("fitsense.evidencia.solo.prevuelo"))) {
            anotar("MODO SOLO PRE-VUELO: se detiene antes de pagar. Quita"
                    + " -Dfitsense.evidencia.solo.prevuelo para continuar.");
            Assumptions.abort("solo pre-vuelo: no se llamo a Replicate. Acta en "
                    + volcarActa());
        }

        // SEGUNDA AUTORIZACION, INDEPENDIENTE, Y ES LA QUE AUTORIZA EL GASTO.
        //
        // fitsense.evidencia.replicate=si habilita la CLASE. Si al lanzarla se
        // olvida -Dfitsense.evidencia.solo.prevuelo, un olvido basta para pagar
        // tres peticiones. Asi que el gasto no se autoriza por omision de una
        // bandera, sino por la presencia explicita de otra: hay que escribir
        // AUTORIZO, y no vale "si" ni "true" ni la bandera vacia.
        //
        // Aborta, no falla: el pre-vuelo si se completo y lo que no hubo fue
        // permiso. Marcarlo rojo diria que algo se rompio.
        if (!"AUTORIZO".equals(System.getProperty("fitsense.evidencia.pago.confirmado"))) {
            anotar("PRE-VUELO COMPLETO Y SIN AUTORIZACION DE PAGO. Todo lo gratuito"
                    + " salio bien. Para gastar hace falta anadir"
                    + " -Dfitsense.evidencia.pago.confirmado=AUTORIZO.");
            Assumptions.abort("pre-vuelo completado; no se autorizo ninguna llamada"
                    + " pagada. Acta en " + volcarActa());
        }

        // ---------------------------------------------------------------
        // 4 · Los intentos
        // ---------------------------------------------------------------
        var desenlace = pedirCorreccion(context, derivation, elegibles, senalada);

        // ---------------------------------------------------------------
        // 5 · Lo que se afirma
        // ---------------------------------------------------------------
        anotar("llamadas: " + desenlace.llamadas()
                + " · respuestas sin efecto: " + desenlace.vacias()
                + " · fuera de contrato: " + desenlace.fueraDeContrato());

        var resultado = desenlace.resultado();
        assertNotNull(resultado, "el productor no devolvio resultado");
        anotar("resultado · publicable: " + resultado.publicable()
                + " · motivo: " + resultado.motivo() + " · origen: " + resultado.source());
        anotar("conservacion: " + resultado.conservacion().describe()
                + " · problemas: " + resultado.problemas());

        // El desenlace se explica ANTES de afirmar, para que un rojo diga cual de
        // los tres paso y no solo que no hubo verde.
        if (desenlace.fueraDeContrato())
            anotar("DIAGNOSTICO: el modelo devolvio al menos una sesion que nadie"
                    + " senalo. El ensamblador lo paro, que es lo correcto, pero el"
                    + " contrato A no se respeto por si mismo.");
        else if (desenlace.vacias() == desenlace.llamadas())
            anotar("DIAGNOSTICO: todas las respuestas fueron validas y sin cambios. No"
                    + " es un incumplimiento del contrato: el modelo no propuso nada.");

        assertTrue(resultado.publicable(),
                "la semana propuesta no pasa la validacion tras " + desenlace.llamadas()
                        + " intento(s) · problemas: " + resultado.problemas());

        assertEquals(AdaptedWeekResult.Motivo.CORRECCION_IA, resultado.motivo(),
                "el desenlace es " + resultado.motivo() + " y no una correccion de la"
                        + " IA: un respaldo conservado NO cuenta como adaptacion lograda");

        assertEquals(GenerationSource.AI, resultado.source(), "el origen tenia que ser AI");

        assertFalse(resultado.conservacion().aplicadas().isEmpty(),
                "ninguna sesion quedo aplicada: una lista vacia no es una adaptacion");

        // Solo se toco lo senalado, y las otras dos llegaron intactas. Esta es la
        // mitad del contrato A que el ensamblador NO puede garantizar: el
        // ensamblador comprueba que las sesiones devueltas estaban senaladas; que
        // el resto llegue sin cambios lo dice V8C.
        assertEquals(List.of(senalada), List.copyOf(resultado.conservacion().aplicadas()),
                "se aplico algo distinto de la sesion senalada");

        assertTrue(resultado.conservacion().conservadas().containsAll(intactas),
                "V8C no reporta como conservadas las sesiones que nadie senalo ("
                        + intactas + "): conservadas = "
                        + resultado.conservacion().conservadas());
    }

    // =======================================================================
    // Elegir al participante
    // =======================================================================

    /**
     * Filtra por SQL solo las condiciones de IAM —las que {@code prepare}
     * comprueba primero— y las dos semanas libres, y despues prueba
     * {@code prepare} con cada candidato hasta que uno pasa.
     * <p>
     * Las condiciones de IAM salen de {@code User.isEligibleForPlanGeneration()}:
     * {@code canSignIn()} es {@code status == ACTIVE}, y
     * {@code StudyEnrollment.isActiveParticipant()} es
     * {@code is_study_participant AND consent_granted_at IS NOT NULL AND
     * withdrawn_at IS NULL}. Las demas condiciones de la fase 1 —perfil,
     * conjunto elegible, cobertura 4C— NO se replican aqui a proposito:
     * reescribirlas en SQL crearia una segunda version de esas reglas, que se
     * separaria de la primera en cuanto una cambiara. Se deja que las compruebe
     * el codigo autentico.
     *
     * Y para cada candidato genera tambien la semana medida CON EL MOTOR DE
     * REGLAS, que no cuesta nada y no toca la red. Si el motor no puede armarle
     * una semana, ese candidato tampoco sirve: se descarta ahi.
     *
     * @return la semilla del participante elegido, ya con {@link #participante}
     *         asignado.
     */
    private Semilla elegirParticipanteYGenerarSemanaMedida() {

        // EN QUE MODO ESTA 4D DE VERDAD, LEIDO DEL BEAN REAL.
        //
        // El objetivo del participante es solo la MITAD de la condicion de V24.
        // PlanDraftValidator:170-171 la enciende asi:
        //
        //     if (!doseEnforcement.mide()) return;
        //     if (context.profile() == null || !MvpDosePolicy.aplicaA(...)) return;
        //
        // Y mide() es «this != OFF» mientras bloquea() es «this == ENFORCE»
        // (DoseEnforcement:41-42). Con OBSERVE, V24 mide y anota pero NO rechaza:
        // la retirada del relleno dejaria de ser necesaria y el verde no probaria
        // que el conflicto INC-P2D-01 esta resuelto, porque el conflicto no
        // habria existido en esa corrida. Con OFF ni se mira.
        //
        // Se lee del validador inyectado -doseEnforcement() es publico, linea
        // 91-, se deja escrito en el acta y se exige ENFORCE. Gratis, y antes de
        // consultar la base.
        var modo4D = validador.doseEnforcement();
        anotar("modo 4D del validador real: " + modo4D
                + " (mide " + modo4D.mide() + ", bloquea " + modo4D.bloquea() + ")"
                + " · modo 4C: " + validador.coverageEnforcement());
        assertTrue(modo4D.bloquea(),
                "4D esta en " + modo4D + ", asi que V24 no rechaza nada y esta"
                        + " corrida no demostraria que la adaptacion funciona con V24"
                        + " activa: sin rechazo, la retirada del relleno no hace"
                        + " falta y el conflicto INC-P2D-01 no se reproduce. Pon 4D"
                        + " en ENFORCE antes de gastar ninguna peticion.");

        @SuppressWarnings("unchecked")
        List<Number> candidatos = (List<Number>) enTransaccion(() -> em.createNativeQuery(
                        "SELECT u.user_id FROM users u"
                                + " WHERE u.status = 'ACTIVE'"
                                + "   AND u.is_study_participant = TRUE"
                                + "   AND u.consent_granted_at IS NOT NULL"
                                + "   AND u.withdrawn_at IS NULL"
                                + "   AND NOT EXISTS (SELECT 1 FROM weekly_training_plans p"
                                + "                    WHERE p.user_id = u.user_id"
                                + "                      AND p.week_start_date IN"
                                + "                          (DATE '" + SEMANA_MEDIDA + "',"
                                + "                           DATE '" + SEMANA_NUEVA + "'))"
                                + "   AND NOT EXISTS (SELECT 1 FROM weekly_user_metrics m"
                                + "                    WHERE m.user_id = u.user_id"
                                + "                      AND m.week_start_date ="
                                + "                          DATE '" + SEMANA_MEDIDA + "')"
                                + " ORDER BY u.user_id LIMIT " + CANDIDATOS)
                .getResultList());

        anotar("candidatos activos en el estudio y con las dos semanas libres: "
                + candidatos.size() + " (tope " + CANDIDATOS + ")");

        assertFalse(candidatos.isEmpty(),
                "No hay ningun participante activo en el estudio con las semanas del "
                        + SEMANA_MEDIDA + " y del " + SEMANA_NUEVA + " libres. Hace falta"
                        + " un usuario con status ACTIVE, is_study_participant,"
                        + " consent_granted_at y sin withdrawn_at. NO se siembra nada.");

        for (Number id : candidatos) {
            long candidato = id.longValue();
            participante = candidato;
            try {
                var preparadoMedida = planCommandService.prepare(
                        mandato(ajusteSinSenalar(), SEMANA_MEDIDA));

                // EL OBJETIVO TIENE QUE SER EL QUE ACTIVA 4D, Y SE PREGUNTA ANTES
                // DE GENERAR NADA.
                //
                // Esta evidencia existe para demostrar que la adaptacion funciona
                // con V24 activa. V24 se enciende con dos condiciones
                // (PlanDraftValidator:170-171): doseEnforcement.mide() -que se
                // comprueba una sola vez arriba- y
                // MvpDosePolicy.aplicaA(profile.goalType()). Con otro objetivo la
                // corrida no representa el caso, y ademas el prompt solo puede
                // identificar rellenos por slot para ese objetivo:
                // PlanInputSnapshot:557 devuelve SIN_ETIQUETAS si !dosisFijada.
                //
                // Se pregunta por MvpDosePolicy.aplicaA y NO por el literal
                // "GAIN_MUSCLE": es el mismo predicado que usa produccion, asi que
                // si la constante MvpDosePolicy.OBJETIVO cambiara, la prueba la
                // sigue en vez de quedarse mirando un literal viejo.
                //
                // El nulo se comprueba primero porque PlanDraftValidator:171
                // tambien lo comprueba: context.profile() puede venir nulo y
                // profile().goalType() reventaria con NullPointerException.
                var perfil = preparadoMedida.context().profile();
                if (perfil == null || !MvpDosePolicy.aplicaA(perfil.goalType())) {
                    anotar("candidato " + candidato + " descartado: esta evidencia"
                            + " exige el objetivo que activa 4D ("
                            + MvpDosePolicy.OBJETIVO + ") y su perfil dice "
                            + (perfil == null ? "no tener perfil" : perfil.goalType()));
                    continue;
                }

                var borrador = reglas.generate(preparadoMedida.context(), List.of());

                // PRIMERO EL BORRADOR VACIO, DESPUES validate. Al reves, un
                // borrador nulo entraria en validate y el diagnostico seria una
                // excepcion generica en vez de decir que el generador no produjo
                // nada.
                if (borrador == null || borrador.workouts() == null
                        || borrador.workouts().size() < 2) {
                    anotar("candidato " + candidato + " descartado: el motor de reglas"
                            + " produjo "
                            + (borrador == null || borrador.workouts() == null
                            ? "nada" : borrador.workouts().size() + " sesion(es)")
                            + " y hacen falta al menos dos");
                    continue;
                }

                // SE VALIDA EL BORRADOR, COMO HACE PRODUCCION. PlanGenerationPipeline
                // llama a validator.validate(draft, context, divisor) ANTES de
                // persistir, en sus dos caminos (lineas 124 y 154), porque el motor
                // de reglas TAMBIEN puede producir un borrador invalido. Saltarse
                // este paso era lo que convertia «valida por construccion» en una
                // afirmacion mas fuerte de lo que el propio pipeline supone.
                //
                // validate lanza InvalidPlanDraftException con los problemas; se
                // captura abajo y ese candidato se descarta. Gratis.
                validador.validate(borrador, preparadoMedida.context(),
                        preparadoMedida.divisor());

                // COBERTURA 4C, QUE NI validate NI EL PRE-VUELO PUEDEN VER.
                //
                // 4C corre en modo ENFORCE_AI_ONLY: solo rechaza cuando el plan
                // tiene origen AI. El borrador del motor de reglas es RULE_ENGINE y
                // la semana del pre-vuelo es CONSERVED, asi que los dos pasan
                // aunque falten patrones. El log de la v5 lo dijo literalmente:
                // «Cobertura 4C incumplida ... y NO se rechaza (modo
                // ENFORCE_AI_ONLY, origen CONSERVED)».
                //
                // Pero el resultado de la adaptacion SI tiene origen AI. Asi que
                // una semana base con patrones sin cubrir haria rechazar la
                // correccion por un obstaculo que ya conocemos, despues de pagar.
                //
                // Se usa la MISMA formula del validador (PlanDraftValidator:460-462)
                // llamando a sus mismos metodos estaticos, no una copia del
                // criterio.
                var disponible = SlotCoveragePolicy.ofEligible(
                        preparadoMedida.context().availableExercises());
                var exigibles = SlotCoveragePolicy.requiredOf(disponible);
                var cubierto = SlotCoveragePolicy.ofDraft(borrador,
                        porId(preparadoMedida.context().availableExercises()));
                var faltan = exigibles.stream()
                        .filter(slot -> cubierto.count(slot) == 0).toList();

                if (!faltan.isEmpty()) {
                    anotar("candidato " + candidato + " descartado por COBERTURA 4C:"
                            + " la semana del motor de reglas no cubre " + faltan
                            + " · exigibles " + exigibles
                            + " · cobertura del borrador [" + cubierto.describe() + "]."
                            + " En ENFORCE_AI_ONLY eso no rechaza una semana RULE_ENGINE"
                            + " ni CONSERVED, pero SI rechazaria el resultado AI de la"
                            + " adaptacion: gastar la peticion seria descubrir un"
                            + " obstaculo ya conocido");
                    continue;
                }

                anotar("PARTICIPANTE ELEGIDO: " + candidato + " · elegibles "
                        + preparadoMedida.context().availableExercises().size()
                        + " · el motor de reglas produjo " + borrador.workouts().size()
                        + " sesiones para la semana medida");
                anotar("CRITERIO DE SELECCION cumplido: activo en el estudio, las dos"
                        + " semanas libres, borrador generado por el motor de reglas y"
                        + " ACEPTADO por PlanDraftValidator, y cobertura 4C completa de"
                        + " los patrones exigibles " + exigibles
                        + " [" + cubierto.describe() + "]");
                return new Semilla(preparadoMedida, borrador);

            } catch (InvalidPlanDraftException e) {
                // El motor de reglas armo una semana que el validador rechaza. Es
                // un motivo distinto de que la fase 1 descarte al participante, y
                // conviene que el acta los separe: esto es un hallazgo sobre
                // FitSense, no sobre el participante.
                anotar("candidato " + candidato + " descartado: el MOTOR DE REGLAS"
                        + " produjo una semana que el validador rechaza · "
                        + e.problems());

            } catch (RuntimeException e) {
                // Un candidato puede caer por IAM, por perfil, por conjunto
                // elegible vacio o por 4C. Cualquiera de esas lo descarta, y por
                // que cayo es dato de la cohorte: va al acta.
                anotar("candidato " + candidato + " descartado por la fase 1: "
                        + e.getClass().getSimpleName() + " · " + e.getMessage());
            }
        }

        return fail("ninguno de los " + candidatos.size() + " candidatos sirve. El acta"
                + " dice por que cayo cada uno, y la causa importa: si todos cayeron por"
                + " COBERTURA 4C, el hallazgo no es de esta prueba sino de FitSense —el"
                + " motor de reglas estaria produciendo semanas que el camino de"
                + " adaptacion rechazaria por origen AI—. NO se sembro nada y no se"
                + " gasto ninguna peticion.");
    }

    /**
     * Lo que hace falta para sembrar la semana medida: la fase 1 de ESA semana y
     * el borrador que el motor de reglas produjo para ella.
     */
    private record Semilla(PreparedPlanGeneration preparado, PlanDraft borrador) {}

    // =======================================================================
    // El cerrojo gratuito
    // =======================================================================

    /**
     * El escenario con el que se va a gastar: la sesion senalada, su fase 1, su
     * derivacion y el conjunto elegible.
     */
    private record Escenario(long senalada, List<Long> intactas,
                             PlanGenerationContext context, WeekDerivation derivation,
                             Map<Long, CandidateExercise> elegibles) {}

    /**
     * QUE SESION SE SENALA, Y POR QUE NO VALE TOMAR LA PRIMERA.
     * <p>
     * La v10 fijaba {@code sesionesOrigen.get(0)} y se encontro con que esa
     * sesion —la 79 del participante 2— tenia sus cinco colocaciones con slot:
     * todas estructurales, ninguna retirable. La semana SI tenia rellenos
     * —{@code fuera_de_slot=5} de 17 posiciones— pero en las otras dos sesiones.
     * <p>
     * ASI QUE SE RECORREN LAS TRES. De cada una se deja el censo en el acta y se
     * elige <b>la primera cuya retirada supera el {@code AdaptedWeekProducer} de
     * verdad</b>, no la primera que tenga un relleno: tener relleno es necesario
     * y no suficiente, porque al quitarlo pueden saltar V5 —el minimo de dos
     * ejercicios—, V15, V17, V20, 4D o la cobertura 4C.
     * <p>
     * POR QUE ESTO NO FALSEA LA EVIDENCIA. En produccion la sesion senalada es
     * la que el participante omitio, con su {@code skip_reason}; aqui la omision
     * es sintetica, asi que <i>cual</i> se marca es un parametro libre de la
     * prueba y no una distorsion del comportamiento real. Ademas, una sesion con
     * relleno es el caso realista de {@code LACK_OF_TIME}. Lo que si hay que
     * decir, y el acta lo dice, es que <b>es un caso elegido por viabilidad y no
     * una muestra representativa de todas las sesiones omitidas</b>.
     * <p>
     * CADA CANDIDATA NECESITA SU PROPIA FASE 1. La derivacion y la validacion
     * dependen de cual sea la sesion senalada: simular la retirada en la 80
     * contra un contexto que senala la 79 seria pedirle a V8C que acepte un
     * cambio en una sesion que nadie senalo, y lo rechazaria por el motivo
     * equivocado. {@code prepare} solo lee, asi que repetirlo es gratis: es el
     * mismo patron que ya usa la seleccion de participante.
     * <p>
     * No se publica nada: {@code produce} no escribe. Solo decide.
     */
    private Escenario elegirEscenarioViable() {
        var censo = new ArrayList<String>();

        for (long candidata : sesionesOrigen) {
            anotar("--- CANDIDATA A SENALADA: sesion " + candidata
                    + " del participante " + participante + " ---");

            var context = planCommandService
                    .prepare(mandato(ajusteSenalando(candidata), SEMANA_NUEVA))
                    .context();

            var previa = context.previousWeek();
            assertNotNull(previa, "la fase 1 no ensamblo la semana anterior");
            assertNotNull(previa.workouts(), "la semana anterior llego sin sesiones");
            assertEquals(sesionesOrigen.size(), previa.workouts().size(),
                    "la semana anterior tenia que traer las " + sesionesOrigen.size()
                            + " sesiones sembradas; sin eso la derivacion no representa"
                            + " el caso real");

            var derivation = derivar(context);
            var elegibles = porId(context.availableExercises());
            anotar("derivadas: " + derivation.workouts().size()
                    + " · sin derivar: " + derivation.sourcesWithoutDerived());

            // PRE-VUELO: la semana sin tocar pasa la validacion real.
            //
            // Lista VACIA y no null: null significaria «no hubo respuesta
            // usable» y llevaria al camino de respaldo. Vacia recorre el mismo
            // revisarYCerrar, que es donde vive estricta.revisar y por tanto V5,
            // V15-V17, V20 y la dosis de 4D.
            //
            // Se repite por candidata porque el contexto cambia con la senalada,
            // y si una sesion concreta hiciera fallar el pre-vuelo eso es un dato
            // y no un motivo para abortar la corrida entera.
            var prevuelo = producer.produce(context, derivation, List.of(), elegibles);
            anotar("PRE-VUELO · publicable: " + prevuelo.publicable()
                    + " · motivo: " + prevuelo.motivo()
                    + " · problemas: " + prevuelo.problemas());

            if (!prevuelo.publicable()) {
                censo.add("sesion " + candidata + ": la semana sembrada NO pasa la"
                        + " validacion ni sin tocarla (" + prevuelo.problemas() + ")");
                continue;
            }

            if (retiradaViableEn(context, derivation, elegibles, candidata, censo)) {
                exigirPromptConLaRetirada(context, derivation);

                var intactas = sesionesOrigen.stream()
                        .filter(s -> s != candidata).toList();
                anotar("ESCENARIO ELEGIDO · senalada: " + candidata
                        + " · tienen que llegar intactas: " + intactas);
                anotar("ADVERTENCIA PARA LA TESIS: esta sesion se eligio POR"
                        + " VIABILIDAD —es la primera de las " + sesionesOrigen.size()
                        + " sembradas cuya retirada supera la validacion real—, asi"
                        + " que NO es una muestra representativa de las sesiones que"
                        + " un participante omite. Lo que la corrida demuestra es que"
                        + " la correccion funciona CUANDO la retirada es posible, no"
                        + " que lo sea siempre.");
                return new Escenario(candidata, List.copyOf(intactas),
                        context, derivation, elegibles);
            }
        }

        fail("NINGUNA DE LAS " + sesionesOrigen.size() + " SESIONES SEMBRADAS "
                + sesionesOrigen + " DEL PARTICIPANTE " + participante + " ADMITE UNA"
                + " RETIRADA VIABLE, asi que pedirle al modelo que acorte no tiene"
                + " solucion en esta semana y no se gasta ninguna peticion."
                + " Censo completo: " + censo
                + ". Si en todas las candidatas son estructurales, el motor de reglas"
                + " no coloco ningun relleno en esta semana y este participante no"
                + " sirve para esta corrida; si caen por validacion, el hallazgo es de"
                + " FitSense y no de la prueba. Acta en " + volcarActa());
        throw new IllegalStateException("inalcanzable: fail siempre lanza");
    }

    /**
     * EL PROMPT QUE VA A VIAJAR LLEVA LAS REGLAS 15 Y 16, Y SE COMPRUEBA GRATIS.
     * <p>
     * El cerrojo anterior demuestra que existe una retirada legal. Esto
     * comprueba lo otro que hace falta para que el gasto tenga sentido: que el
     * modelo vaya a <b>enterarse</b> de que puede hacerla. Sin la regla 15 no
     * sabe que esta autorizado a retirar un relleno, y sin la 16 cree que
     * {@code min_session_minutes} le obliga —que es exactamente lo que razono el
     * 21/10 para concluir, con buen criterio, que no tenia salida—.
     * <p>
     * SE CONSTRUYE CON EL MISMO BEAN Y LOS MISMOS CUATRO ARGUMENTOS que usa el
     * generador, asi que lo que se inspecciona es el prompt de verdad y no una
     * copia. {@code build} no toca la red: cuesta cero. Y se comprueba sobre el
     * texto NORMALIZADO, porque las reglas viven en un bloque con sangria y una
     * frase se parte en dos lineas.
     * <p>
     * ES EL CERROJO CONTRA UNA COMPILACION VIEJA: si el {@code target} tuviera
     * la clase anterior a {@code FIX-PR-1.2}, aqui se para antes de pagar en vez
     * de descubrirlo leyendo el acta despues.
     */
    private void exigirPromptConLaRetirada(PlanGenerationContext context,
                                           WeekDerivation derivation) {
        var prompt = promptBuilder.build(context, derivation,
                PlanInputSnapshot.of(context), AiRetryFeedback.none());
        var plano = prompt.text().replaceAll("\\s+", " ");

        anotar("version del prompt: " + SessionFixPromptBuilder.VERSION
                + " · " + prompt.text().length() + " caracteres");

        assertTrue(plano.contains("REGLA EXCLUSIVA DE ACORTAR_O_REORGANIZAR"),
                "el prompt que iba a viajar NO lleva la regla 15, asi que el modelo"
                        + " no sabria que puede retirar un relleno y la peticion se"
                        + " gastaria sin la instruccion que la hace posible. Version"
                        + " del builder: " + SessionFixPromptBuilder.VERSION
                        + " · se esperaba FIX-PR-1.2 o posterior. Si el codigo esta"
                        + " bien, recompila: el target puede tener la clase vieja."
                        + " Acta en " + volcarActa());

        assertTrue(plano.contains("NO TE OBLIGA en una sesion cuyo motivo sea"
                        + " ACORTAR_O_REORGANIZAR"),
                "el prompt NO lleva la regla 16, asi que el modelo creeria que"
                        + " min_session_minutes le obliga tambien en la sesion"
                        + " senalada. Es el razonamiento que produjo el SIN_EFECTO"
                        + " del 21/10. Acta en " + volcarActa());

        assertTrue(plano.contains("eso NO prueba que el ejercicio sea de relleno"),
                "falta el aviso de la regla 15 para los objetivos distintos de"
                        + " GAIN_MUSCLE. Acta en " + volcarActa());

        // Se comprueba el dato faltante de INC-P2D-02 con el mismo prompt REAL.
        // Debe pasar ANTES del cerrojo de gasto y tambien en modo solo prevuelo.
        exigirSeriesDurationEnPrompt(context, prompt.text());
    }

    /**
     * CERROJO AUTOMATICO DE DOSIS DURATION, SIN IDENTIFICADORES FIJOS.
     *
     * Lee previous.prescriptions(), igual que V8C. Para cada sesion senalada
     * toma solo su bloque de prescripciones del prompt REAL (no available_exercises
     * ni otra sesion). Comprueba literalmente los campos con valor: series,
     * segundos y descanso. Cada coincidencia se consume: dos colocaciones
     * iguales exigen dos lineas distintas, y no se acepta un match prestado.
     * Una expectativa por campo es deliberadamente independiente del builder;
     * no se invoca describir() ni se replica todo el prompt.
     */
    private void exigirSeriesDurationEnPrompt(PlanGenerationContext context,
                                              String promptReal) {
        assertNotNull(context.previousWeek(),
                "CERROJO DURATION: no hay semana previa. Sin gasto.");
        assertNotNull(context.previousWeek().prescriptions(),
                "CERROJO DURATION: no hay prescripciones previas. Sin gasto.");
        int verificadas = 0;

        for (var senalada : context.adjustment().targetedSessions()) {
            Long sesionId = senalada.plannedWorkoutId();
            String marcador = "\n- source_planned_workout_id: " + sesionId + "\n";
            int inicio = promptReal.indexOf(marcador);
            assertTrue(inicio >= 0,
                    "CERROJO DURATION: la sesion senalada " + sesionId
                            + " no aparece en el prompt real. Sin gasto.");
            int fin = promptReal.indexOf("\n  QUE PUEDES CAMBIAR AQUI:", inicio);
            assertTrue(fin > inicio,
                    "CERROJO DURATION: bloque incompleto para sesion " + sesionId
                            + ". Sin gasto.");

            var lineas = new ArrayList<>(promptReal.substring(inicio, fin)
                    .lines().map(String::strip)
                    .filter(linea -> linea.startsWith("exercise_id "))
                    .toList());

            for (var p : context.previousWeek().prescriptions()) {
                if (!sesionId.equals(p.plannedWorkoutId())
                        || !"DURATION".equals(p.prescriptionType())
                        || p.sets() == null) continue;

                assertNotNull(p.exerciseId(),
                        "CERROJO DURATION: prescripcion sin exercise_id. Sin gasto.");
                assertNotNull(p.durationSeconds(),
                        "CERROJO DURATION: segundos ausentes para " + p.exerciseId()
                                + ". Sin gasto.");

                String esperado = "exercise_id " + p.exerciseId()
                        + (p.name() == null ? "" : " (" + p.name() + ")")
                        + ": " + p.sets()
                        + (p.sets() == 1 ? " serie de " : " series de ")
                        + p.durationSeconds() + " s"
                        + (p.restSeconds() == null ? ""
                        : ", descanso " + p.restSeconds() + " s");

                int coincidencia = -1;
                for (int i = 0; i < lineas.size(); i++) {
                    String linea = lineas.get(i);
                    // Los campos opcionales de ejecucion se agregan al final.
                    if (linea.equals(esperado)
                            || linea.startsWith(esperado + ",")
                            || linea.startsWith(esperado + " ->")) {
                        coincidencia = i;
                        break;
                    }
                }
                assertTrue(coincidencia >= 0,
                        "CERROJO DURATION: no se encontro '" + esperado
                                + "' en la sesion " + sesionId
                                + " del prompt real. Sin gasto.");
                lineas.remove(coincidencia);
                verificadas++;
                anotar("CERROJO DURATION: sesion " + sesionId + " · "
                        + esperado + " · VERIFICADO EN PROMPT REAL");
            }
        }

        assertTrue(verificadas > 0,
                "CERROJO DURATION: ninguna prescripcion DURATION con series no nulas"
                        + " en sesiones senaladas. Este escenario no verifica"
                        + " INC-P2D-02 y no justifica una peticion pagada.");
        anotar("CERROJO DURATION: " + verificadas
                + " colocacion(es) verificadas en el prompt real; gasto sigue bloqueado"
                + " hasta autorizacion explicita.");
    }

    /**
     * HAY UNA RETIRADA VIABLE EN ESTA SESION, Y SE COMPRUEBA GRATIS.
     * <p>
     * Deja en {@code censo} el motivo de cada candidata descartada y devuelve
     * {@code true} en cuanto una supera {@code AdaptedWeekProducer} con las
     * validaciones reales: {@code publicable()}, motivo {@code CORRECCION_IA} y
     * una reduccion efectiva del contenido Y de la duracion estimada.
     */
    private boolean retiradaViableEn(PlanGenerationContext context,
                                     WeekDerivation derivation,
                                     Map<Long, CandidateExercise> elegibles,
                                     long candidata,
                                     List<String> censo) {

        // SIN PRESCRIPCION NO HAY CORRIDA, Y NO ES UN TECNICISMO.
        //
        // context.prescription() viene de
        // ExternalConfigurationService.prescriptionParams(), que devuelve
        // fetchActive().params().prescription(): puede ser nulo si la version de
        // configuracion vigente no trae ese bloque. Si lo es pasan dos cosas, y
        // las dos arruinan la evidencia:
        //
        //   1. SessionDurationEstimator.estimateMinutes llama
        //      params.transitionSecondsOrDefault() sin comprobar el nulo, asi que
        //      este cerrojo revienta con NullPointerException en vez de decidir.
        //   2. V20 se apaga en silencio -PlanDraftValidator vuelve antes con
        //      "if (prescription == null) return"-, de modo que la simulacion
        //      pasaria por validaciones MAS FLOJAS que las reales y el verde no
        //      probaria lo que dice probar. Es el mismo defecto que aparecio en
        //      SemanaAcortadaPublicableTest.
        //
        // Se para aqui, gratis, con el diagnostico puesto.
        if (context.prescription() == null)
            fail("la configuracion vigente no trae bloque de prescripcion, asi que"
                    + " no hay modelo de duracion: V17 y V20 quedarian apagadas y la"
                    + " simulacion no demostraria nada. Corrige la configuracion"
                    + " antes de pagar ninguna peticion. Acta en " + volcarActa());

        var derivada = derivation.workouts().stream()
                .filter(w -> candidata == w.sourcePlannedWorkoutId())
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "la derivacion no trae la sesion senalada " + candidata));

        var actuales = derivada.workout().exercises();
        int minutosAntes = minutosDe(context, derivada.workout().scheduledDate(), actuales);
        anotar("sesion " + candidata + " · " + actuales.size() + " colocaciones · "
                + minutosAntes + " min estimados");

        // EL CENSO VA ENTERO, Y ANTES DE DECIDIR.
        //
        // En la v13 el censo vivia dentro del bucle que decide, asi que en
        // cuanto una retirada salia viable el metodo volvia y las colocaciones
        // posteriores no se imprimian nunca. La corrida de las 15:48 lo enseno:
        // la sesion 95 tiene SEIS colocaciones, la retirada viable estaba en la
        // posicion 1, y el acta solo trae las dos primeras. Justo el 1077 -la
        // DURATION de todo este asunto- se quedo fuera.
        //
        // El censo es evidencia, no un efecto secundario de la busqueda. Va
        // completo y por su cuenta.
        censar(context, derivation, candidata, actuales, elegibles);

        // SE RECORRE POR INDICE, NO POR exercise_id.
        //
        // La v10 construia el resto con
        // filter(e -> !id.equals(e.exerciseId())), que quita TODAS las
        // apariciones de ese ejercicio: si una sesion repite el mismo
        // exercise_id en dos colocaciones, la simulacion retiraba DOS y V8C solo
        // autoriza UNA. El indice retira exactamente la colocacion elegida.
        for (int i = 0; i < actuales.size(); i++) {
            var colocacion = actuales.get(i);
            Long id = colocacion.exerciseId();
            var elegible = elegibles.get(id);
            String donde = "sesion " + candidata + " pos " + i + " (ejercicio " + id + ")";

            // Sin catalogo no se puede clasificar, y NO se da por relleno: es el
            // mismo criterio que V8C aplica en ConservationValidator.
            if (elegible == null) {
                censo.add(donde + ": no esta en el conjunto elegible");
                continue;
            }

            var slot = MandatorySlot.of(elegible);
            if (slot.isPresent()) {
                censo.add(donde + ": estructural (slot " + slot.get() + ")");
                continue;
            }

            // Es relleno. Ahora la parte que de verdad decide: simular.
            var quedan = new ArrayList<>(actuales);
            quedan.remove(i);
            int minutosDespues = minutosDe(context,
                    derivada.workout().scheduledDate(), quedan);

            var propuesta = new CorrectedSession(candidata,
                    derivada.workout().scheduledDate(), minutosDespues, List.copyOf(quedan),
                    "simulacion del cerrojo, no viaja a ningun sitio");

            var simulado = producer.produce(context, derivation,
                    List.of(propuesta), elegibles);

            boolean reduceDeVerdad = quedan.size() < actuales.size()
                    && minutosDespues < minutosAntes;

            anotar("  pos " + i + " · " + id + " · SIMULADA: publicable "
                    + simulado.publicable()
                    + " · motivo " + simulado.motivo()
                    + " · " + minutosAntes + " -> " + minutosDespues + " min"
                    + " · problemas " + simulado.problemas());

            if (simulado.publicable()
                    && simulado.motivo() == AdaptedWeekResult.Motivo.CORRECCION_IA
                    && reduceDeVerdad) {
                anotar("RETIRADA VIABLE: quitar la colocacion " + i + " (ejercicio "
                        + id + ") de la sesion " + candidata + " deja la semana"
                        + " publicable con una reduccion real de "
                        + (minutosAntes - minutosDespues) + " min ("
                        + minutosAntes + " -> " + minutosDespues + ")."
                        + " El gasto esta justificado.");

                reproducirElFalloDelModelo(context, derivation, elegibles, candidata,
                        derivada.workout().scheduledDate(), quedan, minutosDespues);
                return true;
            }

            censo.add(donde + ": relleno, pero la retirada no vale ("
                    + (simulado.publicable() ? "publicable, motivo " + simulado.motivo()
                    + (reduceDeVerdad ? "" : ", sin reduccion real")
                    : "no publicable: " + simulado.problemas()) + ")");
        }

        anotar("sesion " + candidata + ": ninguna retirada viable aqui.");
        return false;
    }
    /**
     * EL CENSO DE UNA SESION, COLOCACION POR COLOCACION Y DE LAS DOS FUENTES.
     * <p>
     * V8C no compara contra la derivacion: compara contra
     * {@code previous.prescriptions()} —{@code ConservationValidator:621},
     * {@code indexarDosis}— y lo hace con los CINCO campos del record
     * {@code Dosis} (linea 540): tipo, series, reps, segundos y descanso.
     * <p>
     * Hasta FIX-PR-1.2 el prompt omitía las series de DURATION. En
     * FIX-PR-1.3 ya las incluye; ahora el censo lee el texto real del builder.
     * La corrida pagada del 22/10 devolvio {@code planned_sets: null} para el
     * 1077, que en realidad tenia 3, y por eso V8C rechazo la propuesta.
     * <p>
     * Se imprimen las tres cosas —lo que V8C exige, lo que la derivacion trae y
     * lo que el prompt cuenta— para poder afirmar con dato, y no por
     * aritmetica, cual es el campo que difiere. Si la derivacion y las
     * prescripciones discreparan entre si, eso seria un defecto distinto y mas
     * grave: V8C rechazaria incluso una semana que nadie toco.
     */
    private void censar(PlanGenerationContext context, WeekDerivation derivation,
                        long candidata,
                        List<PlanDraft.DraftExercise> actuales,
                        Map<Long, CandidateExercise> elegibles) {

        // SE LEE DEL PROMPT DE VERDAD, NO DE UNA COPIA.
        //
        // La v14 traia aqui un comoLoVeElModelo(...) que reproducia a mano la
        // logica de SessionFixPromptBuilder.describir(). Al aplicar FIX-PR-1.3
        // la copia se quedo atras: el acta de las 17:23 seguia diciendo «40 s,
        // descanso 45 s» para el 1077 cuando el builder ya imprimia otra cosa,
        // de modo que la linea no probaba nada y encima mentia.
        //
        // Ahora se construye el prompt con el mismo bean y los mismos cuatro
        // argumentos que el generador —build no toca la red, cuesta cero— y se
        // extrae de ahi la linea literal de cada colocacion. Lo que el acta
        // ensena es lo que el modelo va a leer, sin intermediarios que puedan
        // caducar.
        String promptReal = promptBuilder.build(context, derivation,
                PlanInputSnapshot.of(context), AiRetryFeedback.none()).text();

        for (int i = 0; i < actuales.size(); i++) {
            var colocacion = actuales.get(i);
            Long id = colocacion.exerciseId();
            var elegible = elegibles.get(id);

            if (elegible == null) {
                anotar("  pos " + i + " · " + id + " · NO ESTA EN EL CATALOGO:"
                        + " no se puede clasificar, y por tanto no se da por relleno");
            } else {
                anotar("  pos " + i + " · " + id
                        + " · slot " + MandatorySlot.of(elegible).map(Enum::name).orElse("null")
                        + " · patron " + elegible.movementPattern());
            }

            var prescrita = prescripcionDe(context, candidata, id);
            String enDerivacion = dosisLegible(colocacion.prescriptionType(),
                    colocacion.plannedSets(), colocacion.plannedReps(),
                    colocacion.plannedDurationSeconds(), colocacion.restSeconds());
            String enV8C = prescrita == null ? "(no esta en previous.prescriptions())"
                    : dosisLegible(prescrita.prescriptionType(), prescrita.sets(),
                    prescrita.reps(), prescrita.durationSeconds(), prescrita.restSeconds());

            anotar("      derivacion     " + enDerivacion);
            anotar("      V8C (previous) " + enV8C
                    + (prescrita == null ? ""
                    : enDerivacion.equals(enV8C) ? "  <- COINCIDEN"
                    : "  <- DISCREPAN, y eso es un defecto aparte"));
            anotar("      lo que el prompt le ensena al modelo: "
                    + lineaDelPrompt(promptReal, id));
        }
    }

    /**
     * LA MISMA RETIRADA, PERO ANULANDO LAS SERIES DE LAS COLOCACIONES DURATION.
     * <p>
     * Es el experimento que separa «el modelo no entendio la regla» de «el
     * prompt no le dio el dato». La corrida pagada del 22/10 devolvio tres
     * veces la retirada correcta del 1261 y tres veces fue rechazada por
     * <i>«cambia la dosis de [1077]»</i>, siendo 1077 una colocacion DURATION
     * para la que el modelo mando {@code planned_sets: null} —porque el prompt
     * nunca le dijo cuantas series tenia—.
     * <p>
     * Aqui se reconstruye exactamente esa propuesta: la retirada que acaba de
     * salir PUBLICABLE, con la unica diferencia de poner a null las series de
     * las DURATION que se quedan. No se nombra ningun identificador: la regla es
     * el tipo de prescripcion, asi que el experimento vale para cualquier
     * participante y cualquier semana.
     * <p>
     * SI LA SIMULACION PASA DE PUBLICABLE A RECHAZADA y el motivo nombra
     * justamente esas colocaciones, el diagnostico queda probado con dato y no
     * por aritmetica. Si pasara igualmente, mi diagnostico es falso y hay que
     * buscar en otro sitio.
     * <p>
     * No afirma nada: solo deja el resultado en el acta. Es una observacion, no
     * una condicion para gastar.
     */
    private void reproducirElFalloDelModelo(PlanGenerationContext context,
                                            WeekDerivation derivation,
                                            Map<Long, CandidateExercise> elegibles,
                                            long candidata,
                                            java.time.LocalDate dia,
                                            List<PlanDraft.DraftExercise> quedan,
                                            int minutos) {

        var conSeriesNulas = new ArrayList<PlanDraft.DraftExercise>();
        var tocadas = new ArrayList<Long>();
        for (var e : quedan) {
            if (e.prescriptionType() == PrescriptionType.DURATION && e.plannedSets() != null) {
                tocadas.add(e.exerciseId());
                conSeriesNulas.add(new PlanDraft.DraftExercise(e.exerciseId(),
                        e.prescriptionType(), null, e.plannedReps(),
                        e.plannedDurationSeconds(), e.targetLoadKg(),
                        e.restSeconds(), e.notes()));
            } else {
                conSeriesNulas.add(e);
            }
        }

        if (tocadas.isEmpty()) {
            anotar("EXPERIMENTO DURATION: esta sesion no deja ninguna colocacion"
                    + " DURATION con series, asi que el fallo del 22/10 no se puede"
                    + " reproducir aqui. No prueba ni refuta nada.");
            return;
        }

        var propuesta = new CorrectedSession(candidata, dia, minutos,
                List.copyOf(conSeriesNulas), "experimento: series DURATION en null");
        var resultado = producer.produce(context, derivation, List.of(propuesta), elegibles);

        anotar("EXPERIMENTO DURATION · la MISMA retirada con las series de "
                + tocadas + " puestas a null —que es lo que el modelo mando el"
                + " 22/10 bajo FIX-PR-1.2, antes de corregir el prompt—:");
        anotar("      publicable " + resultado.publicable()
                + " · motivo " + resultado.motivo()
                + " · problemas " + resultado.problemas());
        // No basta con que la propuesta sea rechazada: el motivo debe ser
        // especificamente V8C por conservar una dosis diferente. Otros
        // validadores podrian rechazar por causas independientes.
        boolean rechazoPorDosisV8C = !resultado.publicable()
                && resultado.problemas() != null
                && resultado.problemas().stream()
                .map(String::valueOf)
                .anyMatch(p -> p.contains("V8C:")
                        && p.contains("Y ADEMAS cambia la dosis de"));

        anotar(rechazoPorDosisV8C
                ? "      CONFIRMADO EN ESTE ESCENARIO: poner a null solo las series"
                + " de DURATION basta para provocar el rechazo V8C por cambio de"
                + " dosis. Revisar arriba los IDs que V8C nombra."
                : resultado.publicable()
                ? "      NO REPRODUCIDO: la retirada sigue siendo publicable;"
                + " plannedSets en null no explica aqui el rechazo del 22/10."
                : "      INCONCLUSO: la retirada fue rechazada, pero no consta el"
                + " rechazo V8C por cambio de dosis esperado. Hay que revisar"
                + " los problemas exactos antes de atribuir la causa a series.");
    }

    /**
     * La prescripcion que V8C usara como «antes» para esa colocacion.
     * <p>
     * Se busca en {@code context.previousWeek().prescriptions()} por
     * {@code plannedWorkoutId} y {@code exerciseId}, que es exactamente como la
     * indexa {@code ConservationValidator.indexarDosis}. Si el mismo ejercicio
     * se repitiera en la sesion, V8C tambien colapsaria las dos en una sola
     * entrada del mapa: esta busqueda hereda esa limitacion a proposito, para
     * que el censo diga lo que V8C ve y no algo mas fino.
     */
    private PreviousWeekSummary.PrescriptionOutcome prescripcionDe(
            PlanGenerationContext context, long sesion, Long ejercicio) {
        var previa = context.previousWeek();
        if (previa == null || previa.prescriptions() == null || ejercicio == null) return null;
        return previa.prescriptions().stream()
                .filter(p -> p.plannedWorkoutId() != null && sesion == p.plannedWorkoutId())
                .filter(p -> ejercicio.equals(p.exerciseId()))
                // Igual que indexarDosis: Map.put conserva la ultima entrada
                // cuando el mismo exercise_id aparece repetido en la sesion.
                .reduce((anterior, ultima) -> ultima)
                .orElse(null);
    }

    /** Los cinco campos que compara el record Dosis de V8C, en una linea. */
    private String dosisLegible(Object tipo, Object series, Object reps,
                                Object segundos, Object descanso) {
        return "tipo " + tipo + " · series " + series + " · reps " + reps
                + " · segundos " + segundos + " · descanso " + descanso;
    }

    /**
     * La linea literal que el prompt le ensena al modelo para esa colocacion.
     * <p>
     * Se busca en el prompt de verdad en vez de reconstruirla: una copia de
     * {@code describir()} aqui dentro se queda atras en cuanto cambia el
     * builder, y eso ya paso —el acta de las 17:23 seguia anunciando el formato
     * viejo del 1077 despues de aplicar FIX-PR-1.3—.
     * <p>
     * Si el ejercicio NO aparece en el prompt, el acta lo dice: es la unica
     * forma de distinguir «se le ensena mal» de «no se le ensena».
     */
    private String lineaDelPrompt(String prompt, Long id) {
        if (id == null) return "(colocacion sin identificador)";

        int i = prompt.indexOf("exercise_id " + id + " (");
        if (i < 0) i = prompt.indexOf("exercise_id " + id + ":");
        if (i < 0) return "(NO APARECE en el prompt)";

        int fin = prompt.indexOf('\n', i);
        return (fin < 0 ? prompt.substring(i) : prompt.substring(i, fin)).trim();
    }

    /** Los minutos que el estimador da a un contenido, igual que el motor de reglas. */
    private int minutosDe(PlanGenerationContext context, java.time.LocalDate dia,
                          List<PlanDraft.DraftExercise> ejercicios) {
        return estimador.estimateMinutes(new PlanDraft.DraftWorkout(dia,
                        WorkoutFocus.FULL_BODY, "medicion", 1, ejercicios),
                context.prescription());
    }

    // =======================================================================
    // Los intentos
    // =======================================================================

    private record Desenlace(AdaptedWeekResult resultado, int llamadas,
                             int vacias, boolean fueraDeContrato) {}

    /**
     * La politica del orquestador: la validacion ENTRA en el bucle, porque una
     * respuesta puede ser JSON impecable y proponer una semana que V8C o 4D
     * rechazan, y si el productor corriera despues el rechazo gastaria la unica
     * oportunidad.
     * <p>
     * Lo que se anade aqui es el acta y la separacion de los tres desenlaces.
     */
    private Desenlace pedirCorreccion(PlanGenerationContext context,
                                      WeekDerivation derivation,
                                      Map<Long, CandidateExercise> elegibles,
                                      long senalada) {
        var full = PlanInputSnapshot.of(context);
        var acumulados = new LinkedHashSet<String>();
        String ultimaPropuesta = null;
        List<String> ultimosMotivos = List.of();
        int llamadas = 0;
        int vacias = 0;
        boolean fueraDeContrato = false;

        for (int intento = 1; intento <= INTENTOS; intento++) {
            llamadas++;
            anotar("--- intento " + intento + " ---");

            String output = null;
            List<CorrectedSession> correcciones;

            var anteriores = new ArrayList<>(acumulados);
            anteriores.removeAll(ultimosMotivos);
            var feedback = intento == 1 ? AiRetryFeedback.none()
                    : new AiRetryFeedback(ultimaPropuesta, ultimosMotivos, anteriores);

            // EL PROMPT EFECTIVO, reconstruido con el mismo bean, el mismo metodo
            // y los mismos cuatro argumentos que usa el generador. La identidad
            // se COMPRUEBA mas abajo comparando la proyeccion.
            var reconstruido = promptBuilder.build(context, derivation, full, feedback);
            anotar("prompt efectivo (" + SessionFixPromptBuilder.VERSION + "):\n"
                    + reconstruido.text());

            try {
                var cruda = fixGenerator.pedirRespuesta(context, derivation, full, feedback, intento);
                output = cruda.output();
                anotar("respuesta cruda:\n" + output);

                // LA PRUEBA DE QUE EL PROMPT ANOTADO ES EL QUE VIAJO. La
                // proyeccion lleva el contexto entero: coincidir por casualidad
                // es imposible.
                assertEquals(reconstruido.snapshotJson(), cruda.snapshotJson(),
                        "la proyeccion reconstruida no es la que viajo, asi que el"
                                + " prompt anotado tampoco lo es y el acta no sirve como"
                                + " evidencia de que se envio");
                anotar("proyeccion enviada IDENTICA a la reconstruida ("
                        + cruda.snapshotJson().length() + " caracteres). El texto del"
                        + " prompt anotado arriba no se compara byte a byte: se arma de"
                        + " estos mismos cuatro argumentos con un constructor"
                        + " determinista, asi que la garantia es por construccion.");

                correcciones = fixGenerator.interpretar(output, context);

            } catch (AiProviderUnavailableException e) {
                anotar("PROVEEDOR NO DISPONIBLE: " + e.problems());
                acumulados.addAll(e.problems());
                break;

            } catch (InvalidPlanDraftException e) {
                // DESENLACE 1 · fuera de contrato, si los problemas dicen que la
                // sesion no estaba senalada. Cualquier otro motivo es un fallo de
                // forma, no del contrato A.
                boolean noSenalada = e.problems().stream()
                        .anyMatch(p -> p.contains("no estaba senalada"));
                if (noSenalada) fueraDeContrato = true;
                anotar((noSenalada ? "FUERA DE CONTRATO (sesion no senalada)"
                        : "LA RESPUESTA NO CUMPLE EL CONTRATO (forma)") + ": " + e.problems());
                anotar("respuesta cruda rechazada:\n" + output);
                ultimaPropuesta = output;
                ultimosMotivos = List.copyOf(e.problems());
                acumulados.addAll(e.problems());
                continue;
            }

            var devueltas = correcciones.stream()
                    .map(CorrectedSession::sourcePlannedWorkoutId).toList();
            anotar("source_planned_workout_id devueltos: " + devueltas
                    + " · senalada: " + senalada);

            // DESENLACE 2 · valida sin efecto. El ensamblador devuelve lista
            // vacia a proposito: esta EN contrato y significa «no propongo
            // cambios».
            //
            // NO SE REINTENTA, Y ESA ES LA CORRECCION DE LA v3. Produccion
            // tampoco reintenta: pasa la lista vacia a produce, que con una
            // orden activa llega a applier.apply sin correcciones, de ahi a
            // huboAdaptacionEfectiva() == false, SIN_EFECTO/CONSERVED y
            // revisarYCerrar; y el orquestador vuelve EN CUANTO es publicable.
            //
            // Que aqui sea publicable no es una suposicion: el PRE-VUELO de esta
            // misma corrida es esa misma llamada -produce con List.of()- y se
            // exige publicable para seguir. Asi que en este escenario produccion
            // se detendria en el primer intento, con la semana conservada.
            //
            // Reintentar habria dado un verde gracias a una segunda peticion que
            // el backend real nunca habria hecho. Si algun dia se quiere saber si
            // un segundo intento lograria la correccion, eso es un experimento
            // aparte y no el comportamiento del MVP.
            if (correcciones.isEmpty()) {
                vacias++;
                anotar("RESPUESTA VALIDA SIN EFECTO: el modelo no propuso ninguna"
                        + " correccion. No es incumplimiento, y produccion NO"
                        + " reintenta: sigue a produce con la lista vacia.");
            }

            // Invariante de cordura, NO la afirmacion que arriesga: el
            // ensamblador ya rechazo cualquier sesion no senalada, asi que esto
            // solo puede fallar si ese guardia dejo de funcionar.
            if (!List.of(senalada).containsAll(devueltas))
                fail("el ensamblador devolvio sesiones no senaladas " + devueltas
                        + ", asi que su guardia de contrato dejo de funcionar");

            var resultado = producer.produce(context, derivation, correcciones, elegibles);
            if (resultado.publicable())
                return new Desenlace(resultado, llamadas, vacias, fueraDeContrato);

            // DESENLACE 3 (parcial) · se entendio y la semana no vale.
            anotar("LA SEMANA PROPUESTA NO PASA: " + resultado.problemas());
            ultimaPropuesta = output;
            ultimosMotivos = List.copyOf(resultado.problemas());
            acumulados.addAll(resultado.problemas());
        }

        anotar("sin correccion usable tras " + llamadas + " intento(s): " + acumulados);
        // null, igual que el orquestador: significa «no hubo respuesta usable» y
        // lleva al camino de respaldo, que comprueba si conservar es SEGURO. Se
        // llama para que el acta registre el desenlace real; NO se publica.
        var sinPropuesta = producer.produce(context, derivation, null, elegibles);
        anotar("desenlace sin propuesta: " + sinPropuesta.motivo()
                + " · " + sinPropuesta.problemas());
        // EL RESPALDO, NO EL ULTIMO RECHAZADO. AdaptedWeekCommandService:300
        // devuelve sinPropuestaUsable(...) al agotar los intentos, que es esta
        // misma llamada con null. Devolver el ultimo resultado rechazado hacia
        // que el acta contara un desenlace que produccion nunca publica.
        return new Desenlace(sinPropuesta, llamadas, vacias, fueraDeContrato);
    }

    // =======================================================================
    // Derivacion, igual que el orquestador
    // =======================================================================

    private WeekDerivation derivar(PlanGenerationContext context) {
        var previa = context.previousWeek();
        var origenes = new ArrayList<Long>();
        var fechas = new ArrayList<LocalDate>();
        for (PreviousWeekSummary.WorkoutOutcome sesion : previa.workouts()) {
            if (sesion.plannedWorkoutId() == null || sesion.scheduledDate() == null) continue;
            origenes.add(sesion.plannedWorkoutId());
            fechas.add(sesion.scheduledDate().plusWeeks(1));
        }
        assertEquals(sesionesOrigen.size(), origenes.size(),
                "la semana anterior trajo sesiones sin identificador o sin fecha, asi"
                        + " que la derivacion no representa el caso real");
        return derivationBuilder.derive(previa, origenes, fechas);
    }

    private static Map<Long, CandidateExercise> porId(List<CandidateExercise> elegibles) {
        var porId = new LinkedHashMap<Long, CandidateExercise>();
        if (elegibles != null) elegibles.forEach(e -> porId.put(e.exerciseId(), e));
        return porId;
    }

    // =======================================================================
    // Siembra y limpieza
    // =======================================================================

    /**
     * LA SEMANA MEDIDA NO SE CONSTRUYE A MANO, LA PRODUCE FITSENSE. Es la
     * correccion de la v6, y sale de los once problemas que el pre-vuelo
     * encontro en la siembra artesanal de la v5:
     * <ul>
     *   <li><b>V15</b>: los seis ejercicios que habia elegido eran todos de
     *       {@code upper legs}, y el enfoque FULL_BODY exige dos grupos
     *       distintos. Mi filtro miraba cada ejercicio por separado, nunca la
     *       variedad del par.
     *   <li><b>V17</b> y <b>V20</b>: declaraba 45 minutos y el contenido duraba
     *       15.
     *   <li><b>V24</b>: ponia 3x10 a todo, y 4D v3 pide 3x10 para la colocacion
     *       estructural y 2x10 para el relleno, con mas reglas por zona pequena y
     *       por tope de colocaciones.
     * </ul>
     * Acertar todo eso a mano seria escribir una segunda version del motor de
     * prescripcion, que se separaria de la primera en cuanto 4D cambiara. Y
     * existe {@link RuleBasedTrainingPlanGenerator}, cuyo trabajo es exactamente
     * producir semanas que pasan esas reglas, sin red y sin coste.
     * <p>
     * Asi que la semana medida la arma el motor de reglas y la guarda
     * {@code persist} de produccion. Dos ventajas sobre la siembra artesanal, y
     * la segunda importa para la tesis:
     * <ol>
     *   <li>es valida POR CONSTRUCCION, no por mi punteria;
     *   <li>es una semana que FitSense habria producido de verdad, asi que lo que
     *       se le pide corregir a la IA es un caso real y no un montaje.
     * </ol>
     * De paso desaparece el calculo de {@code week_number}: {@code persist} lo
     * resuelve con la logica de produccion, que es la que conoce
     * {@code uq_plan_user_week_version}.
     */
    void sembrar(Semilla semilla) {
        var guardado = planCommandService.persist(semilla.preparado(),
                new GeneratedPlan(semilla.borrador(), (short) 1));

        assertTrue(guardado.isPresent(),
                "persist no guardo la semana medida del participante " + participante);

        planOrigenId = guardado.get().getId();
        // Se anota pegado a la creacion: lo que no quede anotado no se borra.
        planesCreados.add(planOrigenId);

        // Los identificadores se releen de la base y no de la entidad devuelta:
        // persist cerro su transaccion, asi que su coleccion de sesiones puede
        // venir perezosa. El orden por fecha es el que la derivacion usa.
        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) enTransaccion(() -> em.createNativeQuery(
                        "SELECT planned_workout_id FROM planned_workouts"
                                + " WHERE plan_id = " + planOrigenId
                                + " ORDER BY scheduled_date, planned_workout_id")
                .getResultList());
        ids.forEach(i -> sesionesOrigen.add(i.longValue()));

        anotar("semana medida persistida por produccion · plan " + planOrigenId
                + " · sesiones " + sesionesOrigen);
    }

    @AfterEach
    void limpiar() {
        if (!entornoVerificado) {
            // JUnit ejecuta @AfterEach aunque @BeforeEach falle. Sin este
            // return, un cerrojo que no pasa acabaria borrando en la base que no
            // se pudo identificar.
            return;
        }

        for (Long plan : planesCreados)
            ejecutar("DELETE FROM plan_adaptation_attempts"
                    + " WHERE source_plan_id = " + plan + " OR resulting_plan_id = " + plan);

        for (Long plan : alReves(planesCreados)) {
            ejecutar("DELETE FROM planned_workout_exercises WHERE planned_workout_id IN"
                    + " (SELECT planned_workout_id FROM planned_workouts"
                    + "  WHERE plan_id = " + plan + ")");
            ejecutar("DELETE FROM planned_workouts WHERE plan_id = " + plan);
        }

        for (Long plan : alReves(planesCreados))
            ejecutar("DELETE FROM weekly_training_plans WHERE plan_id = " + plan);

        planesCreados.clear();
    }

    private static List<Long> alReves(List<Long> ids) {
        var copia = new ArrayList<>(ids);
        java.util.Collections.reverse(copia);
        return copia;
    }

    // =======================================================================
    // Material
    // =======================================================================

    private GenerateWeeklyPlanCommand mandato(PlanAdjustment ajuste, LocalDate semana) {
        return new GenerateWeeklyPlanCommand(participante, semana, ajuste, true,
                new BigDecimal("42.00"), null);
    }

    /** Para la primera fase 1, la que solo lee el conjunto elegible. */
    private PlanAdjustment ajusteSinSenalar() {
        return new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null,
                0.0, 0.0, null, null, null, "lectura de elegibles", null, null, List.of());
    }

    private PlanAdjustment ajusteSenalando(long sesion) {
        return new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null,
                0.0, 0.0, null, null, null, "baja adherencia", null, null,
                List.of(new PlanAdjustment.TargetedSession(
                        sesion, TipoIntervencion.ACORTAR_O_REORGANIZAR)));
    }

    // =======================================================================
    // El acta
    // =======================================================================

    private void anotar(String linea) {
        acta.append(linea).append(System.lineSeparator());
        log.info("[evidencia] {}", linea);
    }

    /**
     * Deja el acta en un archivo y devuelve la ruta. EL TOKEN NO ENTRA AQUI:
     * solo se escribe lo que pasa por {@code anotar}, y ahi no hay credenciales.
     */
    private String volcarActa() {
        if (acta.isEmpty()) return "(acta vacia: no se llego a anotar nada)";
        try {
            // UN SOLO ARCHIVO POR CORRIDA, Y ES EL ARREGLO DE UN DEFECTO DE LA v10.
            //
            // La ruta llevaba la marca de tiempo de CADA llamada, asi que la
            // corrida del 22/10 dejo dos actas: el mensaje del fallo citaba
            // ...13-21-50.586... y el log anunciaba ...13-21-50.590..., y la
            // citada era la mas corta porque se escribio antes. Ahora la ruta se
            // memoriza en el primer volcado y los siguientes REESCRIBEN ese
            // mismo archivo con el contenido completo, que es lo que la
            // evidencia de la tesis necesita: una ruta, un contenido.
            if (rutaActa == null) {
                rutaActa = Path.of("target", "evidencia-replicate-"
                        + java.time.LocalDateTime.now().toString().replace(':', '-')
                        + ".txt");
                Files.createDirectories(rutaActa.getParent());
            }
            Files.writeString(rutaActa, acta.toString(), StandardCharsets.UTF_8);
            return rutaActa.toAbsolutePath().toString();
        } catch (Exception e) {
            return "no se pudo escribir el acta (" + e.getMessage()
                    + "); esta entera en el log, con el prefijo [evidencia]";
        }
    }

    // =======================================================================
    // Utiles
    // =======================================================================

    private <T> T enTransaccion(Supplier<T> paso) {
        return tx.execute(estado -> paso.get());
    }

    long cuenta(String sql) {
        return enTransaccion(() ->
                ((Number) em.createNativeQuery(sql).getSingleResult()).longValue());
    }

    void ejecutar(String sql) {
        enTransaccion(() -> em.createNativeQuery(sql).executeUpdate());
    }
}
