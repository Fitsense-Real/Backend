package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.ConservationReport;
import main.web.services.fitsense.planning.domain.model.valueobjects.DerivedWorkout;
import main.web.services.fitsense.planning.domain.model.valueobjects.DosisAutorizada;
import main.web.services.fitsense.planning.domain.model.valueobjects.PasoProgresion;
import main.web.services.fitsense.planning.domain.model.valueobjects.MandatorySlot;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PreviousWeekSummary;
import main.web.services.fitsense.planning.domain.model.valueobjects.ExclusionesDeSeguridad;
import main.web.services.fitsense.planning.domain.model.valueobjects.SustitucionSeguridad;
import main.web.services.fitsense.planning.domain.model.valueobjects.WeekDerivation;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * V8C · VALIDACION DE CONSERVACION.
 * <p>
 * Comprueba que la semana derivada cambio la parte que fallo y SOLO esa parte.
 * Es lo que convierte «el sistema adapta el plan» en algo demostrable, y
 * sustituye al V8 antiguo -el objetivo porcentual de volumen- en el camino de
 * adaptacion localizada.
 * <p>
 * LOS DOS V8 NO PUEDEN DISPARARSE A LA VEZ, y por eso el antiguo no se borra: el
 * antiguo exige {@code targetVolume != null} y este exige al menos una sesion
 * senalada, y la orden nueva no fija volumen objetivo. Cada camino conserva su
 * validacion, y los planes historicos se pueden revalidar con las reglas con las
 * que se generaron. Las etiquetas se distinguen -{@code V8:} y {@code V8C:}-
 * para que una traza de la tesis no sea ambigua.
 * <p>
 * POR QUE RECIBE LA DERIVACION Y NO EL BORRADOR. La correspondencia entre una
 * sesion nueva y su origen tiene que venir CONSTRUIDA. Un {@link PlanDraft} no
 * la lleva: ni la fecha, ni el orden, ni el enfoque identifican una sesion entre
 * semanas, asi que deducirla al final emparejaria mal en silencio y daria por
 * conservado algo que cambio. {@link WeekDerivation} la lleva porque se
 * construye al copiar.
 * <p>
 * QUE NO COMPARA. La carga sugerida ({@code targetLoadKg}) queda fuera: no es
 * dosis, es una sugerencia que se recalcula desde el ultimo peso anotado, y la
 * validacion 11 ya gobierna cuando puede subir o bajar. Exigirla identica aqui
 * chocaria con ella. El orden de los ejercicios dentro de la sesion tampoco
 * cuenta: reorganizar no es cambiar los ejercicios ni la dosis.
 * <p>
 * LO QUE ESTA VALIDACION NO PUEDE COMPROBAR, declarado: el repliegue a los
 * OBJETIVOS SECUNDARIOS DEL USUARIO cuando no hay variante del mismo patron. No
 * existe ningun campo de objetivo secundario en el backend, asi que la regla no
 * es verificable hoy. Mientras no exista, V8C exige el mismo patron de
 * movimiento y no admite el repliegue.
 */
@Service
public class ConservationValidator {

    private static final String DOLOR = "PAIN_OR_DISCOMFORT";

    /**
     * @param derivation la semana nueva, con la correspondencia intacta.
     * @param previous   la semana medida, de donde sale la dosis de referencia.
     * @param adjustment la orden espejada en planning: quien esta senalada y que
     *                   se permite cambiar en ella.
     * @param eligible   el catalogo elegible, por el {@code movementPattern}.
     */
    public ConservationReport validate(WeekDerivation derivation,
                                       PreviousWeekSummary previous,
                                       PlanAdjustment adjustment,
                                       Map<Long, CandidateExercise> eligible) {

        // Sin orden, o con una orden que no senala nada, no hay ADAPTACION que
        // validar: si no hubo baja adherencia NO se adapta nada.
        //
        // Pero que no haya adaptacion no significa que no haya nada que
        // comprobar: la semana conservada tiene que ser de verdad una copia.
        // Esa comprobacion es validateContinuidad, mas abajo, y no esta aqui
        // para no cambiar lo que este metodo hacia.
        return validate(derivation, previous, adjustment, eligible, ExclusionesDeSeguridad.ninguna(), List.of());
    }

    /**
     * Igual, admitiendo las sustituciones OBLIGATORIAS POR SEGURIDAD de
     * {@link SafetySubstitutionPolicy}, y solo esas, despues de comprobarlas.
     *
     * @param causas      las exclusiones con causa de seguridad VERIFICADA,
     *                    calculadas aparte de quien propone las sustituciones:
     *                    una autorizacion sin causa aqui no se admite
     * @param autorizadas las sustituciones que se piden
     */
    public ConservationReport validate(WeekDerivation derivation,
                                       PreviousWeekSummary previous,
                                       PlanAdjustment adjustment,
                                       Map<Long, CandidateExercise> eligible,
                                       ExclusionesDeSeguridad causas,
                                       List<SustitucionSeguridad> autorizadas) {
        return validate(derivation, previous, adjustment, eligible, causas, autorizadas, false);
    }

    /**
     * Igual, y con {@code autorizarDosis} (progresion o V24 por capacidades
     * activas, D-P11) ademas: rechaza toda SUBIDA de dosis de la IA en las
     * senaladas, y emite para V24 las dosis autorizadas de la semana -las
     * reducciones aprobadas y las heredadas con linaje-. Con false, lo de
     * siempre.
     */
    public ConservationReport validate(WeekDerivation derivation,
                                       PreviousWeekSummary previous,
                                       PlanAdjustment adjustment,
                                       Map<Long, CandidateExercise> eligible,
                                       ExclusionesDeSeguridad causas,
                                       List<SustitucionSeguridad> autorizadas,
                                       boolean autorizarDosis) {
        if (adjustment == null || !adjustment.adapta()) return ConservationReport.vacio();

        return revisar(derivation, previous, adjustment, eligible, causas, autorizadas, null, autorizarDosis);
    }

    /**
     * P2-B-1: que una semana CONSERVADA sea de verdad una copia.
     * <p>
     * {@code WeekDerivationBuilder} deja que quien lo llama elija que sesiones
     * sobreviven, asi que una semana sin orden de adaptacion podia perder una
     * sesion -o traerla con otros ejercicios- y publicarse igual como
     * {@code CONSERVED}. Una semana que se declara copia y no lo es es peor que
     * una adaptacion rechazada: nadie la mira.
     * <p>
     * NO ES UN SEGUNDO VALIDADOR. Es el mismo recorrido con una orden vacia:
     * ninguna sesion esta senalada, asi que TODAS pasan por
     * {@code revisarNoSenalada} -que ya compara dia, ejercicios, dosis, enfoque
     * y minutos- y {@code revisarParticion} ya rechaza que una sesion
     * desaparezca sin que nadie lo declare. Lo unico que lo impedia era el
     * corto-circuito de arriba.
     * <p>
     * Sirve tambien al respaldo por fallo de IA, que conserva por el mismo
     * motivo y merece la misma comprobacion.
     */
    public ConservationReport validateContinuidad(WeekDerivation derivation,
                                                  PreviousWeekSummary previous,
                                                  Map<Long, CandidateExercise> eligible) {
        return validateContinuidad(derivation, previous, eligible, ExclusionesDeSeguridad.ninguna(), List.of());
    }

    /** Continuidad admitiendo las sustituciones obligatorias por seguridad comprobadas. */
    public ConservationReport validateContinuidad(WeekDerivation derivation,
                                                  PreviousWeekSummary previous,
                                                  Map<Long, CandidateExercise> eligible,
                                                  ExclusionesDeSeguridad causas,
                                                  List<SustitucionSeguridad> autorizadas) {
        return validateContinuidad(derivation, previous, eligible, causas, autorizadas, null, false);
    }

    /**
     * Continuidad con, a lo sumo, UN PASO DE PROGRESION declarado (V34).
     * <p>
     * La sesion del paso no se compara solo con la semana medida: tiene que ser
     * IDENTICA a {@code paso.contenido()} y, respecto de la semana medida,
     * cambiar SOLO el ejercicio del paso y solo como dice su tipo (sube su
     * dosis; se reincorpora; vuelve atras). Mismo dia, enfoque y minutos; sin
     * sustitucion por seguridad; sin dolor. Si no, es un problema: no se
     * progresa a medias. Las demas sesiones se revisan como siempre.
     *
     * @param autorizarDosis emite las dosis autorizadas para V24 (el paso y las
     *                       heredadas con linaje). Sin paso y con false, lo de
     *                       siempre.
     */
    public ConservationReport validateContinuidad(WeekDerivation derivation,
                                                  PreviousWeekSummary previous,
                                                  Map<Long, CandidateExercise> eligible,
                                                  ExclusionesDeSeguridad causas,
                                                  List<SustitucionSeguridad> autorizadas,
                                                  PasoProgresion paso,
                                                  boolean autorizarDosis) {
        return revisar(derivation, previous, PlanAdjustment.none(), eligible, causas, autorizadas,
                paso, autorizarDosis || paso != null);
    }

    private ConservationReport revisar(WeekDerivation derivation,
                                       PreviousWeekSummary previous,
                                       PlanAdjustment adjustment,
                                       Map<Long, CandidateExercise> eligible,
                                       ExclusionesDeSeguridad causas,
                                       List<SustitucionSeguridad> autorizadas,
                                       PasoProgresion paso,
                                       boolean autorizarDosis) {

        // SI SE ORDENO ADAPTAR, LA FALTA DE DATOS ES UN ERROR, NO UN VISTO
        // BUENO. La version anterior devolvia el informe vacio cuando faltaban la
        // semana medida o la derivacion, incluso con sesiones senaladas: una
        // adaptacion sin datos de origen pasaba como valida. No se puede validar
        // la conservacion de algo que no se sabe de donde viene.
        var faltan = new ArrayList<String>();
        if (derivation == null) faltan.add("la derivacion de la semana");
        if (previous == null || previous.workouts() == null || previous.workouts().isEmpty())
            faltan.add("la semana medida de la que deriva");
        if (!faltan.isEmpty())
            return new ConservationReport(
                    List.of(adjustment.adapta()
                            ? ("V8C: se ordeno adaptar %d sesion(es) y falta %s. Una adaptacion sin "
                            + "datos de origen no se puede validar, y no validarla no es "
                            + "aprobarla.")
                            .formatted(adjustment.targetedSessions().size(),
                                    String.join(" y ", faltan))
                            // Mismo principio sin orden: una semana que se declara
                            // copia y no se puede comparar con el original no queda
                            // aprobada por no poder comprobarla.
                            : ("V8C: se iba a conservar la semana y falta %s. No se puede "
                            + "comprobar que sea copia de nada.")
                            .formatted(String.join(" y ", faltan))),
                    List.of(), List.of(), List.of(), Set.of());

        var problems = new ArrayList<String>();
        var aplicadas = new ArrayList<Long>();
        var sinEfecto = new ArrayList<Long>();
        var conservadas = new ArrayList<Long>();
        var progresadas = new ArrayList<Long>();
        var pasos = new ArrayList<PasoProgresion>();
        var dosisAutorizadas = new LinkedHashSet<DosisAutorizada>();
        var fechasReducibles = new LinkedHashSet<LocalDate>();

        var origenes = indexarSesiones(previous);
        var dosisPorSesion = indexarDosis(previous);
        var dolorPorSesion = indexarDolor(previous);
        var conDolorDeSesion = indexarDolorDeSesion(previous);
        var catalogo = eligible == null ? Map.<Long, CandidateExercise>of() : eligible;

        var admitidas = new ArrayList<SustitucionSeguridad>();
        var vistos = new LinkedHashSet<Long>();
        for (var derivada : derivation.workouts()) {
            Long origen = derivada.sourcePlannedWorkoutId();

            if (!vistos.add(origen)) {
                problems.add(("V8C: dos sesiones de la semana nueva dicen derivar de la %d. "
                        + "La correspondencia es uno a uno.").formatted(origen));
                continue;
            }

            var anterior = origenes.get(origen);
            if (anterior == null) {
                problems.add(("V8C: la sesion del %s dice derivar de la %d, que no esta en la "
                        + "semana medida.").formatted(derivada.workout().scheduledDate(), origen));
                continue;
            }

            var intervencion = adjustment.intervencionDe(origen).orElse(null);
            // SUSTITUCION OBLIGATORIA POR SEGURIDAD: la UNICA excepcion a que una
            // sesion no senalada se conserve igual. Se comprueba cada una y solo
            // las legitimas dejan de contar como diferencia; todo lo demas se
            // revisa exactamente como antes.
            var diferencias = neutralizarSustituciones(origen, derivada,
                    comparar(anterior, dosisPorSesion.getOrDefault(origen, Map.of()), derivada),
                    dosisPorSesion.getOrDefault(origen, Map.of()), catalogo,
                    causas == null ? ExclusionesDeSeguridad.ninguna() : causas,
                    autorizadas == null ? List.of() : autorizadas, admitidas, problems);

            // EL DOLOR SE COMPRUEBA ANTES DE SABER SI LA SESION ESTABA
            // SENALADA, Y SIN MIRAR SU TIPO DE INTERVENCION.
            //
            // Estuvo tres veces en el sitio equivocado. Primero solo dentro de
            // revisarSenalada; luego tambien para las conservadas; y seguia
            // faltando el caso de una sesion senalada POR OTRO MOTIVO -acortar,
            // por ejemplo- que conserva el ejercicio que dolio: como su
            // intervencion no exige revision de seguridad, no se comprobaba.
            //
            // La seguridad no puede depender de que el modulo de adaptacion
            // haya clasificado bien el motivo. Si consta dolor, se comprueba;
            // y si ademas esta senalada por seguridad, se mantiene la
            // comprobacion preventiva aunque no conste en los indices.
            var dolieron = dolorPorSesion.getOrDefault(origen, Set.of());
            boolean dolorDeSesion = conDolorDeSesion.contains(origen);
            boolean preventivo = intervencion != null && intervencion.exigeRevisionDeSeguridad();
            boolean segura = !dolieron.isEmpty() || dolorDeSesion || preventivo
                    ? revisarDolor(origen, derivada, diferencias, dolieron,
                    contextoDelDolor(intervencion, preventivo),
                    dolorDeSesion, preventivo, problems)
                    : true;

            boolean sustituida = admitidas.stream().anyMatch(s -> origen.equals(s.sesionOrigen()));
            var fecha = derivada.workout().scheduledDate();
            var conLinaje = previous.dosisConLinaje().getOrDefault(origen, Set.of());

            if (intervencion == null && paso != null && origen.equals(paso.origen())) {
                // V34: el paso de progresion del ciclo. Estrecho: identico al
                // paso propuesto y, respecto de la semana medida, solo su cambio.
                String motivo = !segura ? "consta dolor en la semana medida"
                        : sustituida ? "la sesion lleva una sustitucion por seguridad"
                        : !identica(paso.contenido(), derivada.workout())
                        ? "la sesion no coincide exactamente con el paso propuesto"
                        : pasoEstrecho(paso, diferencias, dosisPorSesion.getOrDefault(origen, Map.of()),
                        derivada.workout(), anterior.expectedDurationMinutes());
                if (motivo != null) {
                    problems.add("V8C: el paso de progresion de la sesion %d no se admite: %s."
                            .formatted(origen, motivo));
                } else {
                    progresadas.add(origen);
                    pasos.add(paso);
                    for (var e : derivada.workout().exercises()) {
                        if (paso.exerciseId().equals(e.exerciseId())) {
                            // La reincorporacion vuelve con la dosis inicial EXACTA: nada que autorizar.
                            if (paso.tipo() != PasoProgresion.Tipo.REINCORPORACION)
                                dosisAutorizadas.add(DosisAutorizada.de(fecha, e, DosisAutorizada.Origen.PASO));
                        } else if (conLinaje.contains(e.exerciseId())) {
                            dosisAutorizadas.add(DosisAutorizada.de(fecha, e, DosisAutorizada.Origen.HEREDADA));
                        }
                    }
                    // D-P7: un paso que no reduce mantiene la exencion de V20 de
                    // una sesion acortada con una reduccion autorizada.
                    if (paso.noReduce() && previous.reduccionAutorizada().contains(origen) && fecha != null)
                        fechasReducibles.add(fecha);
                }
            } else if (intervencion == null) {
                // Si no es segura NO se clasifica como conservada: una sesion
                // con un problema de seguridad no es una conservacion correcta.
                revisarNoSenalada(origen, anterior, diferencias, problems,
                        segura ? conservadas : new ArrayList<>());
                // D8. La COPIA IDENTICA de una sesion con una reduccion autorizada
                // (ACORTAR aplicado, heredado por copias) conserva su exencion de
                // V20. Solo la copia: una sustitucion por seguridad, cualquier
                // diferencia o una intervencion nueva la interrumpen.
                if (conservadas.contains(origen)
                        && previous.reduccionAutorizada().contains(origen)
                        && admitidas.stream().noneMatch(s -> origen.equals(s.sesionOrigen()))
                        && derivada.workout().scheduledDate() != null)
                    fechasReducibles.add(derivada.workout().scheduledDate());
                // V34: la copia identica conserva las dosis autorizadas con linaje.
                if (autorizarDosis && conservadas.contains(origen))
                    for (var e : derivada.workout().exercises())
                        if (conLinaje.contains(e.exerciseId()))
                            dosisAutorizadas.add(DosisAutorizada.de(fecha, e, DosisAutorizada.Origen.HEREDADA));
            } else {
                if (autorizarDosis)
                    revisarDosisDeLaIa(origen, intervencion, derivada, dosisPorSesion.getOrDefault(origen, Map.of()),
                            conLinaje, problems, dosisAutorizadas);
                revisarSenalada(origen, anterior, derivada, intervencion, diferencias,
                        dolieron, segura, catalogo,
                        problems, aplicadas, sinEfecto);
                if (intervencion.permiteReducirDuracion()
                        && derivada.workout().scheduledDate() != null)
                    fechasReducibles.add(derivada.workout().scheduledDate());
            }
        }

        revisarParticion(previous, derivation, adjustment, vistos, problems);

        if (autorizadas != null)
            autorizadas.stream().filter(s -> !vistos.contains(s.sesionOrigen()))
                    .forEach(s -> problems.add(("V8C: se declara una sustitucion por seguridad en la sesion %d, "
                            + "que no esta en la semana nueva.").formatted(s.sesionOrigen())));

        if (paso != null && !vistos.contains(paso.origen()))
            problems.add("V8C: se declara un paso de progresion en la sesion %d, que no esta en la semana nueva."
                    .formatted(paso.origen()));

        return new ConservationReport(problems, aplicadas, sinEfecto, conservadas,
                fechasReducibles, admitidas, progresadas, pasos, dosisAutorizadas, List.of());
    }

    /**
     * El paso cambia, respecto de la semana medida, SOLO su ejercicio y solo
     * como dice su tipo. Devuelve el motivo del rechazo, o null.
     */
    private static String pasoEstrecho(PasoProgresion paso, Diferencias d, Map<Long, Dosis> dosisAnterior,
                                       PlanDraft.DraftWorkout nueva, int minutosAntes) {
        if (d.diaMovido() || d.focoCambiado())
            return "cambia el dia o el enfoque";
        // Los minutos son los que estima el backend para el paso: no bajan en uno
        // que sube, ni suben en una vuelta atras.
        if (d.minutosCambiados() && (paso.tipo() == PasoProgresion.Tipo.VUELTA_ATRAS
                ? d.minutosNuevos() > minutosAntes : d.minutosNuevos() < minutosAntes))
            return "los minutos cambian en sentido contrario al paso";
        Long id = paso.exerciseId();
        return switch (paso.tipo()) {
            case PROGRESION, RECUPERACION -> {
                if (!d.anadidos().isEmpty() || !d.quitados().isEmpty() || !d.dosisCambiada().equals(List.of(id)))
                    yield "debe cambiar solo la dosis del ejercicio %d y cambia %s/%s/%s"
                            .formatted(id, d.anadidos(), d.quitados(), d.dosisCambiada());
                var antes = dosisAnterior.get(id);
                var despues = dosisDe(nueva.exercises().stream().filter(e -> id.equals(e.exerciseId()))
                        .findFirst().orElseThrow());
                yield sube(antes, despues) ? null : "la dosis del ejercicio %d no sube".formatted(id);
            }
            case REINCORPORACION -> !d.quitados().isEmpty() || !d.dosisCambiada().isEmpty()
                    || !d.anadidos().equals(List.of(id))
                    ? "debe solo reincorporar el ejercicio %d".formatted(id) : null;
            case VUELTA_ATRAS -> {
                if (!d.anadidos().isEmpty()) yield "una vuelta atras no anade ejercicios";
                if (d.quitados().equals(List.of(id)) && d.dosisCambiada().isEmpty()) yield null;
                if (!d.quitados().isEmpty() || !d.dosisCambiada().equals(List.of(id)))
                    yield "debe cambiar solo el ejercicio %d".formatted(id);
                var antes = dosisAnterior.get(id);
                var despues = dosisDe(nueva.exercises().stream().filter(e -> id.equals(e.exerciseId()))
                        .findFirst().orElseThrow());
                yield sube(despues, antes) ? null : "la dosis del ejercicio %d no baja".formatted(id);
            }
        };
    }

    /** {@code despues} no baja en nada respecto de {@code antes}, sube en algo, mismo tipo y descanso. */
    private static boolean sube(Dosis antes, Dosis despues) {
        if (antes == null || despues == null) return false;
        return java.util.Objects.equals(antes.tipo(), despues.tipo())
                && java.util.Objects.equals(antes.descanso(), despues.descanso())
                && noMenor(antes.series(), despues.series()) && noMenor(antes.reps(), despues.reps())
                && noMenor(antes.segundos(), despues.segundos()) && !antes.equals(despues);
    }

    private static boolean noMenor(Number antes, Number despues) {
        if (antes == null || despues == null) return java.util.Objects.equals(antes, despues);
        return despues.intValue() >= antes.intValue();
    }

    private static boolean mayor(Number antes, Number despues) {
        return antes != null && despues != null && despues.intValue() > antes.intValue();
    }

    /**
     * LA IA NUNCA SUBE DOSIS. En una sesion senalada, toda dosis que la IA deja
     * mayor en series, repeticiones o segundos que la de la semana medida se
     * rechaza, aunque el tipo de intervencion permita cambiar la dosis y aunque
     * caiga en un rango que V24 aceptaria. Las bajadas aprobadas (sin subir
     * nada, mismo tipo y descanso, en una intervencion que permite cambiar la
     * dosis) quedan como REDUCCION para V24; lo que no cambio y tiene linaje,
     * como HEREDADA. Un ejercicio nuevo (sustituto) no recibe nada: dosis exacta.
     */
    private static void revisarDosisDeLaIa(Long origen, TipoIntervencion intervencion, DerivedWorkout derivada,
                                           Map<Long, Dosis> dosisAnterior, Set<Long> conLinaje,
                                           List<String> problems, Set<DosisAutorizada> dosisAutorizadas) {
        var fecha = derivada.workout().scheduledDate();
        for (var e : derivada.workout().exercises()) {
            var antes = dosisAnterior.get(e.exerciseId());
            if (antes == null) continue;
            var despues = dosisDe(e);
            if (antes.equals(despues)) {
                if (conLinaje.contains(e.exerciseId()))
                    dosisAutorizadas.add(DosisAutorizada.de(fecha, e, DosisAutorizada.Origen.HEREDADA));
                continue;
            }
            if (mayor(antes.series(), despues.series()) || mayor(antes.reps(), despues.reps())
                    || mayor(antes.segundos(), despues.segundos())) {
                problems.add(("V8C: la sesion %d sube la dosis del ejercicio %d (%s -> %s). La IA no puede "
                        + "subir dosis: solo el backend progresa.").formatted(origen, e.exerciseId(),
                        resumen(antes), resumen(despues)));
                continue;
            }
            if (intervencion.permiteCambiarDosis() && java.util.Objects.equals(antes.tipo(), despues.tipo())
                    && java.util.Objects.equals(antes.descanso(), despues.descanso()))
                dosisAutorizadas.add(DosisAutorizada.de(fecha, e, DosisAutorizada.Origen.REDUCCION));
        }
    }

    private static String resumen(Dosis d) {
        return "%sx%s/%s".formatted(d.series(), d.reps() != null ? d.reps() + "r" : d.segundos() + "s", d.descanso());
    }

    /** Mismo dia, enfoque, minutos y ejercicios con sus dosis, en el mismo orden. */
    private static boolean identica(PlanDraft.DraftWorkout referencia, PlanDraft.DraftWorkout derivada) {
        return referencia != null && derivada != null
                && java.util.Objects.equals(referencia.scheduledDate(), derivada.scheduledDate())
                && referencia.focus() == derivada.focus()
                && referencia.expectedDurationMinutes() == derivada.expectedDurationMinutes()
                && java.util.Objects.equals(referencia.exercises(), derivada.exercises());
    }

    /**
     * Quita de las diferencias de una sesion las sustituciones obligatorias por
     * seguridad que cumplen TODAS las condiciones; las que no, se rechazan con
     * su motivo y su diferencia sigue contando.
     * <ol>
     *   <li>el quitado estaba en esta sesion de la semana medida;</li>
     *   <li>el quitado tiene una CAUSA DE SEGURIDAD VERIFICADA, calculada aparte
     *       ({@code SafetyConflictPolicy}), y el motivo declarado es esa causa:
     *       un bloqueo, el equipamiento o el filtro funcional no autorizan;</li>
     *   <li>el quitado YA NO es elegible: si lo fuera, no habria motivo;</li>
     *   <li>el puesto es elegible y no estaba en la sesion;</li>
     *   <li>los dos son de la misma clase de slot (4C);</li>
     *   <li>en la semana nueva esta el puesto y no el quitado.</li>
     * </ol>
     * La dosis del puesto NO se compara con la del quitado: se recalcula para el
     * sustituto y la juzgan V16, V17, V20 y V24 en la validacion estricta.
     */
    private Diferencias neutralizarSustituciones(Long origen, DerivedWorkout derivada, Diferencias d,
                                                 Map<Long, Dosis> dosisAnterior,
                                                 Map<Long, CandidateExercise> catalogo,
                                                 ExclusionesDeSeguridad causas,
                                                 List<SustitucionSeguridad> autorizadas,
                                                 List<SustitucionSeguridad> admitidas,
                                                 List<String> problems) {
        var deEsta = autorizadas.stream().filter(s -> origen.equals(s.sesionOrigen())).toList();
        if (deEsta.isEmpty()) return d;

        var anadidos = new ArrayList<>(d.anadidos());
        var quitados = new ArrayList<>(d.quitados());
        var idsNuevos = derivada.workout().exercises().stream()
                .map(PlanDraft.DraftExercise::exerciseId).collect(Collectors.toSet());
        for (var s : deEsta) {
            var puesto = catalogo.get(s.puesto());
            String motivo = null;
            if (!dosisAnterior.containsKey(s.quitado()))
                motivo = "el ejercicio %d no estaba en esa sesion de la semana medida".formatted(s.quitado());
            else if (!causas.incluye(s.quitado()))
                motivo = ("el ejercicio %d no tiene una causa de seguridad verificada (un bloqueo, el "
                        + "equipamiento o el filtro funcional no autorizan a sustituirlo)").formatted(s.quitado());
            else if (!causas.motivos().get(s.quitado()).equals(s.motivo()))
                motivo = "el motivo declarado para %d (%s) no es su causa verificada (%s)"
                        .formatted(s.quitado(), s.motivo(), causas.motivos().get(s.quitado()));
            else if (catalogo.containsKey(s.quitado()))
                motivo = "el ejercicio %d sigue siendo elegible: no hay motivo de seguridad".formatted(s.quitado());
            else if (puesto == null)
                motivo = "el sustituto %d no es elegible".formatted(s.puesto());
            else if (dosisAnterior.containsKey(s.puesto()))
                motivo = "el sustituto %d ya estaba en la sesion".formatted(s.puesto());
            else if (!MandatorySlot.ofMovementPattern(s.patronQuitado()).equals(MandatorySlot.of(puesto)))
                motivo = "el sustituto %d (%s) no es de la misma clase de slot que %d (%s)"
                        .formatted(s.puesto(), puesto.movementPattern(), s.quitado(), s.patronQuitado());
            else if (!idsNuevos.contains(s.puesto()) || idsNuevos.contains(s.quitado()))
                motivo = "la sesion no refleja la sustitucion de %d por %d".formatted(s.quitado(), s.puesto());

            if (motivo != null) {
                problems.add("V8C: la sustitucion por seguridad de la sesion %d no se admite: %s."
                        .formatted(origen, motivo));
                continue;
            }
            anadidos.remove(s.puesto());
            quitados.remove(s.quitado());
            admitidas.add(s);
        }
        return new Diferencias(List.copyOf(anadidos), List.copyOf(quitados), d.dosisCambiada(),
                d.idsAnteriores(), d.idsNuevos(), d.diaMovido(), d.diaNuevo(), d.focoCambiado(), d.focoNuevo(),
                d.minutosCambiados(), d.minutosNuevos());
    }

    // =======================================================================
    // Sesion NO senalada: identica. Es la mitad del criterio de exito.
    // =======================================================================

    /**
     * Como llego esta sesion a la comprobacion, para que el mensaje sea cierto.
     * Las tres formas existen y no se pueden contar igual: una sesion acortada
     * que conserva el ejercicio que dolio no «se senalo por dolor».
     */
    private String contextoDelDolor(TipoIntervencion intervencion, boolean preventivo) {
        if (preventivo) return "se senalo por dolor";
        if (intervencion != null)
            return "registro dolor y se senalo por otro motivo (" + intervencion.name() + ")";
        return "registro dolor y no se senalo para adaptarla";
    }

    private void revisarNoSenalada(Long origen, PreviousWeekSummary.WorkoutOutcome anterior,
                                   Diferencias d, List<String> problems, List<Long> conservadas) {
        if (!d.cambio()) {
            conservadas.add(origen);
            return;
        }

        // El dia de la semana SI se compara. No es la fecha -la semana entera se
        // desplaza siete dias por definicion- sino el dia: mover al jueves una
        // sesion que estaba el lunes y que nadie senalo es un cambio que no se
        // pidio. Es el corolario de que reprogramar sea el permiso de una sesion
        // senalada por horario.
        if (d.diaMovido())
            problems.add(("V8C: la sesion %d no estaba senalada y cambio de %s a %s. "
                    + "Solo se reprograma una sesion senalada por horario.")
                    .formatted(origen, anterior.scheduledDate().getDayOfWeek(), d.diaNuevo()));

        if (!d.anadidos().isEmpty())
            problems.add(("V8C: la sesion %d no estaba senalada y aparecen los ejercicios %s. "
                    + "Una sesion que no fallo se conserva igual.")
                    .formatted(origen, d.anadidos()));

        if (!d.quitados().isEmpty())
            problems.add(("V8C: la sesion %d no estaba senalada y desaparecen los ejercicios %s. "
                    + "Una sesion que no fallo se conserva igual.")
                    .formatted(origen, d.quitados()));

        if (!d.dosisCambiada().isEmpty())
            problems.add(("V8C: la sesion %d no estaba senalada y cambia la dosis de %s. "
                    + "Conserva series, repeticiones, segundos y descanso.")
                    .formatted(origen, d.dosisCambiada()));

        if (d.focoCambiado())
            problems.add("V8C: la sesion %d no estaba senalada y cambia de enfoque a %s."
                    .formatted(origen, d.focoNuevo()));

        if (d.minutosCambiados())
            problems.add(("V8C: la sesion %d no estaba senalada y declara %d minutos en vez "
                    + "de %d.").formatted(origen, d.minutosNuevos(), anterior.expectedDurationMinutes()));
    }

    // =======================================================================
    // Sesion senalada: solo lo que autoriza su tipo de intervencion.
    // =======================================================================

    private void revisarSenalada(Long origen, PreviousWeekSummary.WorkoutOutcome anterior,
                                 DerivedWorkout derivada, TipoIntervencion intervencion,
                                 Diferencias d, Set<Long> dolieron,
                                 boolean seguridadResuelta,
                                 Map<Long, CandidateExercise> catalogo,
                                 List<String> problems, List<Long> aplicadas,
                                 List<Long> sinEfecto) {

        // ANTE DOLOR, CONSERVAR NO ES NEUTRO. Volver a prescribir el ejercicio
        // que dolio, solo porque estaba en el plan anterior, es publicar algo
        // potencialmente perjudicial. Se comprueba ANTES que todo lo demas.
        int problemasAntes = problems.size();

        // El dolor ya se comprobo en el recorrido comun, para TODA sesion con
        // dolor registrado y no solo para las senaladas por seguridad.
        if (!seguridadResuelta) return;   // ya hay problema; no se clasifica

        if (d.diaMovido() && !intervencion.permiteReprogramar())
            problems.add(("V8C: la sesion %d esta senalada como %s, que no autoriza cambiar el "
                    + "dia, y pasa de %s a %s. %s")
                    .formatted(origen, intervencion, anterior.scheduledDate().getDayOfWeek(),
                            d.diaNuevo(), intervencion.instruccion()));

        if (!d.anadidos().isEmpty() || !d.quitados().isEmpty())
            revisarSustitucion(origen, intervencion, d, dolieron, catalogo, problems);

        if (!d.dosisCambiada().isEmpty() && !intervencion.permiteCambiarDosis())
            problems.add(("V8C: la sesion %d esta senalada como %s, que no autoriza cambiar la "
                    + "dosis, y cambia la de %s. %s")
                    .formatted(origen, intervencion, d.dosisCambiada(), intervencion.instruccion()));

        if (d.minutosCambiados() && !intervencion.permiteCambiarDosis())
            problems.add(("V8C: la sesion %d esta senalada como %s y declara %d minutos en vez "
                    + "de %d. Lo que se autoriza es mover el dia, no acortarla.")
                    .formatted(origen, intervencion, d.minutosNuevos(),
                            anterior.expectedDurationMinutes()));

        // El enfoque no cambia ni estando senalada: una sesion con otro enfoque
        // no es la misma sesion corregida, es una sesion distinta, y entonces la
        // adaptacion ya no es localizada.
        if (d.focoCambiado())
            problems.add(("V8C: la sesion %d cambia de enfoque %s a %s. Una sesion senalada se "
                    + "corrige, no se reemplaza por otra.")
                    .formatted(origen, anterior.focusCode(), d.focoNuevo()));

        // UNA SESION CON PROBLEMAS NO SE CLASIFICA. Ni aplicada ni sin efecto: su
        // cambio esta rechazado, asi que contarla como adaptacion aplicada seria
        // dar por buena justamente la parte que se rechazo -una sesion movida de
        // dia sin permiso contaba como adaptacion-, y contarla como sin efecto
        // seria decir que no cambio nada cuando si cambio.
        if (problems.size() > problemasAntes) return;

        // Clasificacion. Conservar una sesion senalada esta PERMITIDO -la IA
        // puede no encontrar nada mejor- pero no cuenta como adaptacion: queda
        // registrado que no hubo efecto, sin inventar que no existian
        // alternativas.
        if (d.cambio()) aplicadas.add(origen);
        else sinEfecto.add(origen);
    }

    /**
     * @return true si la seguridad quedo resuelta; false si ya se anoto un
     *         problema por este motivo.
     */
    /**
     * @param contexto      como llego aqui esta sesion, para que el mensaje sea
     *                      cierto: una sesion conservada NO «se senalo por dolor».
     * @param dolorDeSesion si consta DOLOR A NIVEL DE SESION en el registro.
     *                      Va aparte de {@code preventivo} porque significan
     *                      cosas distintas y antes iban fundidos en un solo
     *                      booleano: eso dejaba sin comprobar la coexistencia.
     * @param preventivo    si la sesion se senalo con revision de seguridad. Es
     *                      la cautela de V8C cuando NADA consta en los indices,
     *                      no una afirmacion de que hubo dolor de sesion.
     */
    private boolean revisarDolor(Long origen, DerivedWorkout derivada, Diferencias d,
                                 Set<Long> dolieron, String contexto, boolean dolorDeSesion,
                                 boolean preventivo, List<String> problems) {

        var reaparecen = dolieron.stream().filter(d.idsNuevos()::contains).sorted().toList();
        if (!reaparecen.isEmpty()) {
            problems.add(("V8C: la sesion %d " + contexto + " y vuelve a prescribir %s. "
                    + "Un ejercicio que dolio no se repropone porque estuviera en el plan "
                    + "anterior: o se sustituye por una alternativa segura, o se retira.")
                    .formatted(origen, reaparecen));
            return false;
        }

        // DOLOR DECLARADO A NIVEL DE SESION, SIN SABER QUE EJERCICIO FUE.
        //
        // Aqui la unica resolucion COMPROBABLE es que no vuelva NINGUNO de los
        // ejercicios de aquella sesion: si no sabemos cual dolio, cualquier
        // ejercicio que reaparece puede ser el que dolio.
        //
        // No basta con que la sesion "haya cambiado". Mover el miercoles al
        // jueves cambia la sesion y no resuelve nada; tampoco acortarla, ni
        // bajarle la carga. Fecha, duracion y carga no son resoluciones de
        // seguridad. La version anterior aceptaba exactamente ese caso, porque
        // preguntaba por un cambio cualquiera.
        // COEXISTENCIA: consta dolor de sesion Y ademas algun ejercicio
        // identificado. Antes, con dolieron no vacio, esta rama no se ejecutaba
        // nunca: bastaba con sustituir el identificado y los demas de aquella
        // sesion podian quedarse. Eso solo seria correcto si el dolor de sesion
        // fuera un RESUMEN del especifico, y esa equivalencia no esta
        // establecida en ninguna parte: puede ser una declaracion propia.
        // Mientras no lo este, se trata como declaracion independiente.
        //
        // El caso preventivo no cambia: cuando no consta nada en los indices y
        // la sesion se senalo por seguridad, se generaliza igual que en V8C.
        boolean generalizar = dolorDeSesion || (preventivo && dolieron.isEmpty());

        if (generalizar) {
            var siguenAhi = d.idsAnteriores().stream()
                    .filter(d.idsNuevos()::contains).sorted().toList();
            if (!siguenAhi.isEmpty()) {
                problems.add(("V8C: la sesion %d " + contexto + " y no consta a que ejercicio. "
                        + "Siguen %s de aquella sesion, y cualquiera de ellos pudo ser el que "
                        + "dolio: cambiar fecha, duracion o carga no resuelve eso. O se "
                        + "reemplaza el contenido, o esa parte queda declarada como no "
                        + "adaptable con seguridad.").formatted(origen, siguenAhi));
                return false;
            }
        }

        return true;
    }

    private void revisarSustitucion(Long origen, TipoIntervencion intervencion, Diferencias d,
                                    Set<Long> dolieron, Map<Long, CandidateExercise> catalogo,
                                    List<String> problems) {

        if (!intervencion.permiteSustituirEjercicio()) {

            // ANADIR SIGUE PROHIBIDO SIEMPRE, y sin permiso de retirada tampoco
            // se puede quitar: es el comportamiento de antes, intacto para las
            // seis intervenciones que no son ACORTAR_O_REORGANIZAR.
            if (!d.anadidos().isEmpty() || !intervencion.permiteQuitarEjercicio()) {
                problems.add(("V8C: la sesion %d esta senalada como %s, que no autoriza sustituir "
                        + "ejercicios (anade %s, quita %s). %s")
                        .formatted(origen, intervencion, d.anadidos(), d.quitados(),
                                intervencion.instruccion()));
                return;
            }

            // Solo hay retiradas y la intervencion las autoriza (INC-P2D-01).
            // Dos limites, y los dos se comprueban aqui porque V24 NO los cubre.

            // UNA, Y SOLO UNA. Acortar es quitar trabajo, no vaciar la sesion.
            if (d.quitados().size() > 1) {
                problems.add(("V8C: la sesion %d esta senalada como %s y retira %d ejercicios "
                        + "(%s). Se autoriza retirar UNA sola colocacion de relleno. %s")
                        .formatted(origen, intervencion, d.quitados().size(), d.quitados(),
                                intervencion.instruccion()));
                return;
            }

            Long retirado = d.quitados().get(0);
            var candidato = catalogo.get(retirado);

            // SIN CATALOGO NO SE PUEDE CLASIFICAR, Y NO SE DA POR RELLENO.
            // MandatorySlot.of(null) devuelve Optional.empty(), que es
            // exactamente lo que significa «es de relleno»: sin esta rama, un
            // ejercicio que no este en el conjunto elegible pasaria por relleno
            // sin que nadie lo haya comprobado. Es el mismo criterio que ya usa
            // la rama de patron desconocido de mas abajo.
            if (candidato == null) {
                problems.add(("V8C: la sesion %d retira el ejercicio %d y no consta en el "
                        + "conjunto elegible, asi que no se puede comprobar que sea una "
                        + "colocacion de relleno.").formatted(origen, retirado));
                return;
            }

            // SOLO RELLENO. El criterio no se reinventa: es el de
            // MvpDosePolicy, que decide con relleno = MandatorySlot.of(x).isEmpty()
            // -su linea 260-. Un ejercicio con slot obligatorio es ESTRUCTURAL y
            // sostiene la cobertura 4C de la semana.
            //
            // HACE FALTA COMPROBARLO AQUI, y no basta con V24: la clase de dosis
            // depende del slot del ejercicio -del catalogo-, NO de su posicion en
            // la sesion. Al retirar un estructural, el relleno que queda NO pasa a
            // estructural, asi que sus dosis siguen siendo validas y V24 no se
            // entera de la perdida.
            var slot = MandatorySlot.of(candidato);
            if (slot.isPresent()) {
                problems.add(("V8C: la sesion %d esta senalada como %s y retira el ejercicio %d, "
                        + "que es una colocacion ESTRUCTURAL (patron %s, slot %s). Solo se "
                        + "autoriza retirar una de relleno: una estructural sostiene la "
                        + "cobertura de la semana. %s")
                        .formatted(origen, intervencion, retirado,
                                candidato.movementPattern(), slot.get(),
                                intervencion.instruccion()));
                return;
            }

            // AL RETIRAR, LO QUE QUEDA SE CONSERVA TAL CUAL.
            //
            // Sin esta rama el permiso nuevo abriria una puerta que no se pidio:
            // ACORTAR_O_REORGANIZAR lleva permiteCambiarDosis = true, y
            // revisarSenalada solo objeta la dosis cuando ese permiso es false.
            // Asi que una propuesta podria retirar un relleno Y ADEMAS recortar
            // las series de otro, y V8C no diria nada. Con GAIN_MUSCLE V24
            // probablemente lo atajaria; con los demas objetivos, nada.
            //
            // El permiso de dosis se mantiene INTACTO cuando no hay retirada:
            // este metodo solo se llama si hay anadidos o retiradas, asi que una
            // propuesta que solo cambie la dosis no pasa por aqui y se sigue
            // autorizando como antes.
            if (!d.dosisCambiada().isEmpty()) {
                problems.add(("V8C: la sesion %d esta senalada como %s y retira el ejercicio %d "
                        + "Y ADEMAS cambia la dosis de %s. Al retirar una colocacion, las que "
                        + "quedan se conservan tal cual: una cosa o la otra. %s")
                        .formatted(origen, intervencion, retirado, d.dosisCambiada(),
                                intervencion.instruccion()));
                return;
            }

            // Una sola retirada, de relleno, con el resto intacto: autorizada. Lo
            // que quede sigue pasando por V5, V15, V17, V20, V23 y V24 como
            // siempre.
            return;
        }

        if (d.anadidos().isEmpty()) return;   // solo se retiraron: nada que emparejar

        // EL MISMO PATRON DE MOVIMIENTO. Se exige para la sustitucion por
        // dificultad, que es de donde sale la regla: una variante mas sencilla
        // del mismo patron. NO se exige ante dolor, donde forzar el mismo patron
        // podria devolver justo lo que molesto; ahi la seguridad la da el filtro
        // de elegibilidad -el ejercicio esta en available_exercises porque paso
        // las restricciones declaradas, y V1 lo comprueba- mas la regla de que el
        // ejercicio que dolio no reaparece.
        if (intervencion.exigeRevisionDeSeguridad()) return;

        var patronesQuitados = new LinkedHashSet<String>();
        for (var quitado : d.quitados()) {
            String patron = patron(quitado, catalogo);
            if (patron == null) {
                problems.add(("V8C: la sesion %d retira el ejercicio %d y no consta su patron de "
                        + "movimiento, asi que no se puede comprobar que el reemplazo sea del "
                        + "mismo patron.").formatted(origen, quitado));
                return;
            }
            patronesQuitados.add(patron);
        }

        if (patronesQuitados.isEmpty()) {
            problems.add(("V8C: la sesion %d anade los ejercicios %s sin retirar ninguno. Una "
                    + "sustitucion reemplaza; anadir trabajo nuevo no es corregir la parte que "
                    + "fallo.").formatted(origen, d.anadidos()));
            return;
        }

        for (var anadido : d.anadidos()) {
            String patron = patron(anadido, catalogo);
            if (patron == null) {
                problems.add(("V8C: la sesion %d anade el ejercicio %d y no consta su patron de "
                        + "movimiento.").formatted(origen, anadido));
                continue;
            }
            if (!patronesQuitados.contains(patron))
                problems.add(("V8C: la sesion %d sustituye por el ejercicio %d, de patron %s, y "
                        + "los retirados son de patron %s. La sustitucion tiene que ser del mismo "
                        + "patron de movimiento.")
                        .formatted(origen, anadido, patron, patronesQuitados));
        }
    }

    private static String patron(Long exerciseId, Map<Long, CandidateExercise> catalogo) {
        var candidato = catalogo.get(exerciseId);
        return candidato == null ? null : candidato.movementPattern();
    }

    // =======================================================================
    // Sesiones anteriores que no tienen derivada
    // =======================================================================

    /**
     * CADA SESION DE LA SEMANA MEDIDA APARECE EXACTAMENTE UNA VEZ: o tiene
     * derivada, o consta como eliminacion autorizada. Nunca en las dos listas, y
     * nunca en ninguna.
     * <p>
     * LA LISTA {@code sourcesWithoutDerived} NO SE CREE, SE CONTRASTA. La version
     * anterior empezaba con {@code if (sinDerivar.isEmpty()) return;}, y eso
     * aceptaba el peor caso posible: una semana medida de 101, 102 y 103, una
     * derivada con 101 y 103, y la lista de eliminadas vacia. La 102 desaparecia
     * y la validacion de conservacion daba el visto bueno. Confiar en el dato que
     * se esta validando es justo lo que un validador no puede hacer: la
     * derivacion puede venir de cualquier productor, y si la IA deja caer una
     * sesion al reemplazar la senalada, la lista tampoco se actualiza.
     */
    private void revisarParticion(PreviousWeekSummary previous, WeekDerivation derivation,
                                  PlanAdjustment adjustment, Set<Long> conDerivada,
                                  List<String> problems) {

        var todas = new LinkedHashSet<Long>();
        if (previous.workouts() != null)
            previous.workouts().stream()
                    .filter(w -> w.plannedWorkoutId() != null)
                    .forEach(w -> todas.add(w.plannedWorkoutId()));

        var sinDerivar = new LinkedHashSet<>(derivation.sourcesWithoutDerived());

        var enAmbas = sinDerivar.stream().filter(conDerivada::contains).sorted().toList();
        if (!enAmbas.isEmpty())
            problems.add(("V8C: las sesiones %s figuran a la vez con derivada y como eliminadas. "
                    + "Una sesion es una cosa o la otra.").formatted(enAmbas));

        // EL DEFECTO: ni derivada, ni eliminacion declarada. Simplemente falta.
        var desaparecidas = todas.stream()
                .filter(id -> !conDerivada.contains(id) && !sinDerivar.contains(id))
                .sorted().toList();
        if (!desaparecidas.isEmpty())
            problems.add(("V8C: las sesiones %s de la semana medida no tienen derivada y tampoco "
                    + "constan como eliminadas: desaparecieron sin que nadie lo declarara.")
                    .formatted(desaparecidas));

        var inventadas = sinDerivar.stream().filter(id -> !todas.contains(id)).sorted().toList();
        if (!inventadas.isEmpty())
            problems.add(("V8C: se declaran como eliminadas las sesiones %s, que no estan en la "
                    + "semana medida.").formatted(inventadas));

        if (sinDerivar.isEmpty()) return;

        // Y la eliminacion, cuando la hay, tiene que estar autorizada.
        if (adjustment.forcedDaysPerWeek() == null) {
            problems.add(("V8C: las sesiones %s se quedan sin derivada y nadie ordeno reducir "
                    + "dias. Una sesion de la semana anterior no desaparece por su cuenta.")
                    .formatted(sinDerivar));
            return;
        }

        int esperadas = Math.max(0, todas.size() - adjustment.forcedDaysPerWeek());
        if (sinDerivar.size() != esperadas)
            problems.add(("V8C: se ordenaron %d dias sobre %d anteriores, asi que deberian "
                    + "quedar %d sesiones sin derivada y quedan %d (%s).")
                    .formatted(adjustment.forcedDaysPerWeek(), todas.size(), esperadas,
                            sinDerivar.size(), sinDerivar));
    }

    // =======================================================================
    // Comparacion
    // =======================================================================

    /** La dosis PRESCRITA de un ejercicio. La carga queda fuera a proposito. */
    private record Dosis(String tipo, Short series, Short reps, Integer segundos, Short descanso) {}

    private record Diferencias(List<Long> anadidos, List<Long> quitados, List<Long> dosisCambiada,
                               Set<Long> idsAnteriores, Set<Long> idsNuevos,
                               boolean diaMovido, Object diaNuevo,
                               boolean focoCambiado, Object focoNuevo,
                               boolean minutosCambiados, int minutosNuevos) {

        boolean cambio() {
            return !anadidos.isEmpty() || !quitados.isEmpty() || !dosisCambiada.isEmpty()
                    || diaMovido || focoCambiado || minutosCambiados;
        }
    }

    private Diferencias comparar(PreviousWeekSummary.WorkoutOutcome anterior,
                                 Map<Long, Dosis> dosisAnterior, DerivedWorkout derivada) {

        var nueva = derivada.workout();
        var dosisNueva = new LinkedHashMap<Long, Dosis>();
        for (var ejercicio : nueva.exercises()) {
            if (ejercicio.exerciseId() == null) continue;
            dosisNueva.put(ejercicio.exerciseId(), dosisDe(ejercicio));
        }

        var anadidos = dosisNueva.keySet().stream()
                .filter(id -> !dosisAnterior.containsKey(id)).sorted().toList();
        var quitados = dosisAnterior.keySet().stream()
                .filter(id -> !dosisNueva.containsKey(id)).sorted().toList();
        var dosisCambiada = dosisAnterior.keySet().stream()
                .filter(dosisNueva::containsKey)
                .filter(id -> !dosisAnterior.get(id).equals(dosisNueva.get(id)))
                .sorted().toList();

        var diaAnterior = anterior.scheduledDate() == null ? null
                : anterior.scheduledDate().getDayOfWeek();
        var diaNuevo = nueva.scheduledDate() == null ? null : nueva.scheduledDate().getDayOfWeek();
        boolean diaMovido = diaAnterior != null && diaNuevo != null && diaAnterior != diaNuevo;

        String focoNuevo = nueva.focus() == null ? null : nueva.focus().name();
        boolean focoCambiado = anterior.focusCode() != null && focoNuevo != null
                && !anterior.focusCode().equals(focoNuevo);

        boolean minutosCambiados =
                nueva.expectedDurationMinutes() != anterior.expectedDurationMinutes();

        return new Diferencias(anadidos, quitados, dosisCambiada,
                new LinkedHashSet<>(dosisAnterior.keySet()),
                new LinkedHashSet<>(dosisNueva.keySet()), diaMovido, diaNuevo,
                focoCambiado, focoNuevo, minutosCambiados, nueva.expectedDurationMinutes());
    }

    private static Dosis dosisDe(PlanDraft.DraftExercise ejercicio) {
        return new Dosis(
                ejercicio.prescriptionType() == null ? null : ejercicio.prescriptionType().name(),
                ejercicio.plannedSets(),
                ejercicio.plannedReps(),
                ejercicio.plannedDurationSeconds(),
                ejercicio.restSeconds());
    }

    private static Dosis dosisDe(PreviousWeekSummary.PrescriptionOutcome prescrita) {
        return new Dosis(prescrita.prescriptionType(), prescrita.sets(), prescrita.reps(),
                prescrita.durationSeconds(), prescrita.restSeconds());
    }

    // =======================================================================
    // Indices de la semana medida
    // =======================================================================

    private Map<Long, PreviousWeekSummary.WorkoutOutcome> indexarSesiones(
            PreviousWeekSummary previous) {
        var porId = new LinkedHashMap<Long, PreviousWeekSummary.WorkoutOutcome>();
        if (previous.workouts() == null) return porId;
        previous.workouts().stream()
                .filter(w -> w.plannedWorkoutId() != null)
                .forEach(w -> porId.put(w.plannedWorkoutId(), w));
        return porId;
    }

    private Map<Long, Map<Long, Dosis>> indexarDosis(PreviousWeekSummary previous) {
        var porSesion = new LinkedHashMap<Long, Map<Long, Dosis>>();
        if (previous.prescriptions() == null) return porSesion;
        for (var p : previous.prescriptions()) {
            if (p.plannedWorkoutId() == null || p.exerciseId() == null) continue;
            porSesion.computeIfAbsent(p.plannedWorkoutId(), k -> new TreeMap<>())
                    .put(p.exerciseId(), dosisDe(p));
        }
        return porSesion;
    }

    /**
     * Los ejercicios que la persona marco con dolor, por sesion. Es el motivo
     * registrado del EJERCICIO, no el de la sesion: cuando el dolor consta a
     * nivel de sesion no se sabe cual fue, y ese caso se trata aparte.
     */
    /**
     * Las sesiones cuyo DOLOR consta a nivel de sesion, sin decir que ejercicio.
     * <p>
     * {@link #indexarDolor} solo mira {@code PrescriptionOutcome.skipReason}, asi
     * que un dolor declarado en la sesion y no en un ejercicio no aparecia en
     * ningun indice. Con eso, una sesion con dolor que nadie senalo se copiaba
     * tal cual y volvia a prescribir lo mismo.
     */
    private Set<Long> indexarDolorDeSesion(PreviousWeekSummary previous) {
        var conDolor = new LinkedHashSet<Long>();
        if (previous.workouts() == null) return conDolor;
        for (var w : previous.workouts())
            if (w.plannedWorkoutId() != null && DOLOR.equals(w.skipReason()))
                conDolor.add(w.plannedWorkoutId());
        return conDolor;
    }

    private Map<Long, Set<Long>> indexarDolor(PreviousWeekSummary previous) {
        var porSesion = new LinkedHashMap<Long, Set<Long>>();
        if (previous.prescriptions() == null) return porSesion;
        for (var p : previous.prescriptions()) {
            if (p.plannedWorkoutId() == null || p.exerciseId() == null) continue;
            if (DOLOR.equals(p.skipReason()))
                porSesion.computeIfAbsent(p.plannedWorkoutId(), k -> new LinkedHashSet<>())
                        .add(p.exerciseId());
        }
        return porSesion;
    }
}
