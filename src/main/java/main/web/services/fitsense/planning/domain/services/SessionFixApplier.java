package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.CorrectedSession;
import main.web.services.fitsense.planning.domain.model.valueobjects.DerivedWorkout;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.WeekDerivation;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * EL EMPALME. Aplica las correcciones de la IA sobre la copia de la semana
 * anterior, emparejando por {@code sourcePlannedWorkoutId}.
 * <p>
 * Es el corazon del contrato A y el sitio donde la adaptacion localizada se
 * vuelve real: la derivacion trae las sesiones conservadas y aqui se sustituyen
 * SOLO las senaladas. Servicio de dominio puro: no consulta nada.
 * <p>
 * POR QUE AQUI NO SE VALIDA NADA DE V8C. Esto no comprueba que el cambio sea
 * legitimo -eso es V8C-, sino que la correspondencia no se pierda. Lo que si
 * garantiza por CONSTRUCCION, y por eso importa:
 * <ul>
 *   <li><b>El enfoque y el nombre no cambian.</b> No viajan en
 *       {@link CorrectedSession}: se toman de la copia. La regla de V8C «una
 *       sesion senalada se corrige, no se reemplaza por otra» deja de depender
 *       de que la validacion la pille.</li>
 *   <li><b>Una sesion no senalada no se puede tocar.</b> Una correccion sobre
 *       una sesion que la orden no senalo se RECHAZA aqui, antes de llegar a la
 *       validacion.</li>
 *   <li><b>El origen se conserva.</b> La sesion corregida sigue derivando de la
 *       misma sesion anterior, asi que V8C puede compararlas y V31 puede
 *       persistir el vinculo.</li>
 * </ul>
 * NO EXIGE QUE VENGAN TODAS. Si la IA devuelve dos de las tres senaladas, la
 * tercera se queda conservada y V8C la registrara como adaptacion SIN EFECTO.
 * Conservar una sesion senalada esta permitido; lo que no esta permitido es
 * inventar que cambio.
 */
@Service
public class SessionFixApplier {

    /**
     * @param base         la semana copiada de la anterior, con la
     *                     correspondencia intacta.
     * @param correcciones lo que devolvio la IA. Puede venir vacia: entonces la
     *                     semana queda conservada tal cual.
     * @param adjustment   la orden, para saber que sesiones estaban senaladas.
     * @throws IllegalArgumentException si una correccion no se puede situar, si
     *                                  viene repetida, o si toca una sesion que
     *                                  nadie senalo. Las tres son defectos del
     *                                  productor o respuestas fuera de contrato,
     *                                  no planes malos: no son problemas de
     *                                  validacion, son errores.
     */
    public WeekDerivation apply(WeekDerivation base,
                                List<CorrectedSession> correcciones,
                                PlanAdjustment adjustment) {

        if (base == null)
            throw new IllegalArgumentException(
                    "No se puede aplicar una correccion sin la semana derivada.");

        // UNA RESPUESTA AUSENTE NO ES UNA RESPUESTA VACIA.
        //
        //   []    la IA contesto, y contesto que no propone ninguna correccion.
        //         La semana queda conservada y V8C lo registra SIN EFECTO.
        //   null  no hubo respuesta valida. El intento FALLO, y le toca el
        //         respaldo seguro, no una semana conservada.
        //
        // Colapsar las dos era el defecto: un fallo de la IA se habria
        // registrado como adaptacion sin efecto, que es la lectura mas
        // enganosa posible de la evidencia -un fallo contado como un intento
        // legitimo que no cambio nada-.
        if (correcciones == null)
            throw new IllegalArgumentException(
                    "Respuesta de adaptacion ausente; no equivale a ninguna correccion.");

        var aplicar = correcciones;
        if (aplicar.isEmpty()) return base;

        if (adjustment == null || !adjustment.adapta())
            throw new IllegalArgumentException(
                    "Llegaron %d correcciones y la orden no senala ninguna sesion."
                            .formatted(aplicar.size()));

        var porOrigen = new java.util.LinkedHashMap<Long, CorrectedSession>();
        for (var correccion : aplicar) {
            Long origen = correccion.sourcePlannedWorkoutId();

            if (porOrigen.put(origen, correccion) != null)
                throw new IllegalArgumentException(
                        ("Llegaron dos correcciones para la sesion %d. La IA corrige cada "
                                + "sesion senalada una vez.").formatted(origen));

            if (!adjustment.esSenalada(origen))
                throw new IllegalArgumentException(
                        ("Llego una correccion para la sesion %d, que la orden NO senalo. "
                                + "Solo se corrige lo que fallo.").formatted(origen));
        }

        var presentes = new LinkedHashSet<Long>();
        base.workouts().forEach(d -> presentes.add(d.sourcePlannedWorkoutId()));

        var ausentes = porOrigen.keySet().stream()
                .filter(id -> !presentes.contains(id))
                .sorted().toList();
        if (!ausentes.isEmpty())
            throw new IllegalArgumentException(
                    ("Llegaron correcciones para las sesiones %s y no estan en la semana "
                            + "derivada. Sin la copia no hay con que compararlas.")
                            .formatted(ausentes));

        var resultado = new ArrayList<DerivedWorkout>(base.workouts().size());
        for (var derivada : base.workouts()) {
            var correccion = porOrigen.get(derivada.sourcePlannedWorkoutId());
            resultado.add(correccion == null ? derivada : empalmar(derivada, correccion));
        }

        return new WeekDerivation(resultado, base.sourcesWithoutDerived());
    }

    /**
     * La sesion corregida: fecha, duracion y ejercicios de la IA; enfoque y
     * nombre de la copia.
     * <p>
     * La fecha y la duracion caen a lo de la copia cuando la IA no las manda, en
     * vez de quedar nulas: un borrador con una fecha nula no lo rechaza ninguna
     * validacion con un mensaje util, revienta antes.
     */
    private DerivedWorkout empalmar(DerivedWorkout derivada, CorrectedSession correccion) {
        var copia = derivada.workout();

        return new DerivedWorkout(derivada.sourcePlannedWorkoutId(),
                new PlanDraft.DraftWorkout(
                        correccion.scheduledDate() == null
                                ? copia.scheduledDate() : correccion.scheduledDate(),
                        copia.focus(),
                        copia.name(),
                        correccion.expectedDurationMinutes() == null
                                ? copia.expectedDurationMinutes()
                                : correccion.expectedDurationMinutes(),
                        correccion.exercises()));
    }

    /**
     * Las sesiones senaladas que la IA NO corrigio. V8C las registrara como
     * adaptacion sin efecto, y el prompt del siguiente intento puede insistir en
     * ellas.
     */
    public List<Long> senaladasSinCorregir(List<CorrectedSession> correcciones,
                                           PlanAdjustment adjustment) {
        // El orden de estas dos comprobaciones importa.
        //
        // SIN ORDEN no hay sesiones que investigar, asi que la lista vacia es la
        // respuesta correcta aunque las correcciones vengan nulas: no hay nada
        // sobre lo que el nulo signifique algo.
        if (adjustment == null || !adjustment.adapta()) return List.of();

        // CON ORDEN ACTIVA, el nulo vuelve a significar lo que significa en
        // apply: no hubo respuesta. Y aqui es mas peligroso, porque devolveria
        // TODAS las senaladas como no corregidas y el productor clasificaria el
        // intento como una adaptacion sin efecto en vez de como un fallo. Es la
        // misma confusion de antes, por otra puerta.
        if (correcciones == null)
            throw new IllegalArgumentException("Respuesta de adaptacion ausente.");

        var corregidas = new LinkedHashSet<Long>();
        correcciones.forEach(c -> corregidas.add(c.sourcePlannedWorkoutId()));

        return adjustment.targetedSessions().stream()
                .map(PlanAdjustment.TargetedSession::plannedWorkoutId)
                .filter(id -> !corregidas.contains(id))
                .sorted()
                .toList();
    }
}
