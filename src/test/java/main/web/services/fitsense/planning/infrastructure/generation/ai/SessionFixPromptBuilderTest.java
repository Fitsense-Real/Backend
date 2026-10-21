package main.web.services.fitsense.planning.infrastructure.generation.ai;

import main.web.services.fitsense.planning.domain.model.valueobjects.AdjustmentType;
import main.web.services.fitsense.planning.domain.model.valueobjects.DerivedWorkout;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningProfile;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.PreviousWeekSummary;
import main.web.services.fitsense.planning.domain.model.valueobjects.SafetyProfile;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import main.web.services.fitsense.planning.domain.model.valueobjects.WeekDerivation;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El prompt de adaptacion localizada.
 * <p>
 * Lo que se comprueba no es la redaccion sino lo que NO puede faltar: los
 * identificadores de las sesiones senaladas, la instruccion de su tipo de
 * intervencion -que tiene que salir del mismo sitio que la regla de V8C-, y el
 * aviso de dolor. Si algo de eso se cae, el modelo no puede acertar y la culpa
 * no seria suya.
 */
class SessionFixPromptBuilderTest {

    private static final LocalDate L1 = LocalDate.of(2026, 10, 12);   // semana medida
    private static final LocalDate L2 = LocalDate.of(2026, 10, 19);   // semana nueva

    private final SessionFixPromptBuilder builder = new SessionFixPromptBuilder(new JsonSupport());

    // =======================================================================
    // Lo que no puede faltar
    // =======================================================================

    @Test
    @DisplayName("Lleva el identificador de la sesion senalada, que es la identidad")
    void llevaElIdentificador() {
        var prompt = construir(senalando(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR));

        assertTrue(prompt.contains("source_planned_workout_id: 102"),
                "sin el identificador el modelo no puede decir que corrige");
        assertTrue(prompt.contains("SESIONES SENALADAS (1)"));
    }

    @Test
    @DisplayName("La instruccion sale de TipoIntervencion, no de un texto escrito aparte")
    void laInstruccionSaleDelMismoSitioQueLaRegla() {
        // Es lo que impide que el prompt pida una cosa y V8C permita otra.
        var tipo = TipoIntervencion.ACORTAR_O_REORGANIZAR;
        var prompt = construir(senalando(102L, tipo));

        assertTrue(prompt.contains("QUE PUEDES CAMBIAR AQUI: " + tipo.instruccion()),
                () -> "no esta la instruccion literal del enum");
    }

    @Test
    @DisplayName("Por horario, la instruccion dice que NO sustituya ejercicios")
    void porHorarioNoSustituye() {
        var prompt = construir(senalando(102L, TipoIntervencion.REPROGRAMAR_DIAS));

        assertTrue(plano(prompt).contains("No sustituir ejercicios"), () -> extracto(prompt));
    }

    @Test
    @DisplayName("Lleva la prescripcion anterior con su dosis y como le fue")
    void llevaLaPrescripcionAnterior() {
        var prompt = construir(senalando(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR));

        assertTrue(prompt.contains("exercise_id 304"), () -> extracto(prompt));
        assertTrue(prompt.contains("3x10"), "la dosis, para que sepa de que parte");
        assertTrue(prompt.contains("descanso 90 s"));
        assertTrue(prompt.contains("LACK_OF_TIME"), "y el motivo registrado");
    }

    @Test
    @DisplayName("Lleva la fecha que la sesion tiene AHORA, que es sobre la que decide")
    void llevaLaFechaNueva() {
        var prompt = construir(senalando(102L, TipoIntervencion.REPROGRAMAR_DIAS));

        assertTrue(prompt.contains("fecha que tiene ahora: " + L2.plusDays(2)),
                () -> extracto(prompt));
    }

    // =======================================================================
    // El aviso de dolor
    // =======================================================================

    @Test
    @DisplayName("Con dolor identificado, avisa de QUE ejercicio no puede volver")
    void dolorIdentificado() {
        var prompt = construir(senalando(102L, TipoIntervencion.SEGURIDAD_ANTE_DOLOR),
                semanaConDolorEn(304L));

        assertTrue(prompt.contains("AVISO: dolio [304]"), () -> extracto(prompt));
        assertTrue(plano(prompt).contains("NO puede volver"));
    }

    @Test
    @DisplayName("Con dolor sin identificar, avisa de que no conserve NINGUNO")
    void dolorSinIdentificar() {
        // Es el caso que V8C bloquea: sin saber cual dolio, la unica resolucion
        // comprobable es que no vuelva ninguno.
        var prompt = construir(senalando(102L, TipoIntervencion.SEGURIDAD_ANTE_DOLOR));

        assertTrue(plano(prompt).contains("no consta en que ejercicio"), () -> extracto(prompt));
        assertTrue(plano(prompt).contains("No conserves ninguno"));
    }

    @Test
    @DisplayName("Sin dolor NO aparece el aviso")
    void sinDolorNoHayAviso() {
        var prompt = construir(senalando(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR));

        assertFalse(prompt.contains("AVISO:"),
                "un aviso de dolor donde no lo hubo es ruido que puede desviar la correccion");
    }

    // =======================================================================
    // Lo que el prompt le prohibe, y lo que no le manda
    // =======================================================================

    @Test
    @DisplayName("Le dice que OMITA una sesion en vez de devolverla igual")
    void omitirEnVezDeRepetir() {
        var prompt = construir(senalando(102L, TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA));

        assertTrue(plano(prompt).contains("OMITELA del arreglo"), () -> extracto(prompt));
        assertTrue(plano(prompt).contains("Conservarla es una respuesta legitima"));
    }

    @Test
    @DisplayName("Le dice que el reemplazo sea del mismo movement_pattern, con su excepcion")
    void mismoPatronConSuExcepcion() {
        var prompt = construir(senalando(102L, TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA));

        assertTrue(plano(prompt).contains("MISMO movement_pattern"), () -> extracto(prompt));
        assertTrue(plano(prompt).contains("La unica excepcion es el dolor"),
                "si no, ante dolor insistiria en el patron que molesto");
    }

    @Test
    @DisplayName("Le dice que acortar no es declarar menos minutos")
    void acortarNoEsDeclararMenos() {
        var prompt = construir(senalando(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR));

        assertTrue(plano(prompt).contains("Declarar menos minutos sin quitar trabajo no es acortar"),
                () -> extracto(prompt));
    }

    @Test
    @DisplayName("NO le manda las sesiones conservadas")
    void noMandaLasConservadas() {
        var prompt = construir(senalando(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR));

        // Esta prueba vigila EL BLOQUE, que es su nivel. La fuga por el
        // snapshot la vigila elHistorialDeLasConservadasNoViaja: ahi los
        // identificadores desnudos no sirven, porque 740 y 592 estan
        // legitimamente en available_exercises.
        var bloque = soloElBloque(prompt);

        assertTrue(prompt.contains("SESIONES SENALADAS (1)"), "una senalada, una sola");
        for (var fuga : List.of("101", "103", "740", "592",
                "Cuerpo completo A", "Cuerpo completo C")) {
            assertFalse(bloque.contains(fuga), () -> "se filtro «" + fuga
                    + "» de una sesion conservada al bloque de senaladas: si las ve"
                    + " tiende a tocarlas, y lo que toque de mas lo rechaza V8C\n"
                    + bloque);
        }
        assertTrue(bloque.contains("102") && bloque.contains("304"), () -> bloque);
    }

    /** El bloque de sesiones senaladas, sin las reglas ni los datos de entrada. */
    private static String soloElBloque(String prompt) {
        int i = prompt.indexOf("SESIONES SENALADAS");
        int j = prompt.indexOf("\nDatos de entrada:", i);
        assertTrue(i >= 0 && j > i, "el prompt tiene que traer el bloque y los datos");
        return prompt.substring(i, j);
    }

    // =======================================================================
    // Adaptacion ampliada y reintento
    // =======================================================================

    @Test
    @DisplayName("Ampliada: las dos sesiones, cada una con SU instruccion")
    void ampliada() {
        var orden = new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null,
                0.0, 0.0, null, null, null, "baja adherencia", null, null,
                List.of(new PlanAdjustment.TargetedSession(101L,
                                TipoIntervencion.REPROGRAMAR_DIAS),
                        new PlanAdjustment.TargetedSession(102L,
                                TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA)));

        var prompt = construir(orden);

        assertTrue(prompt.contains("SESIONES SENALADAS (2)"));
        assertTrue(prompt.contains("source_planned_workout_id: 101"));
        assertTrue(prompt.contains("source_planned_workout_id: 102"));
        assertTrue(prompt.contains(TipoIntervencion.REPROGRAMAR_DIAS.instruccion()));
        assertTrue(prompt.contains(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA.instruccion()));
    }

    @Test
    @DisplayName("El reintento le prohibe anadir sesiones para resolver un motivo")
    void elReintentoNoAnadeSesiones() {
        var prompt = builder.build(contexto(senalando(102L,
                                TipoIntervencion.ACORTAR_O_REORGANIZAR), semanaDeTres()),
                        derivacion(), snapshot(),
                        new AiRetryFeedback("{\"sessions\":[]}",
                                List.of("V8C: la sesion 102 no autoriza sustituir"), List.of()))
                .text();

        assertTrue(prompt.contains("INTENTO ANTERIOR RECHAZADO"));
        assertTrue(prompt.contains("V8C: la sesion 102 no autoriza sustituir"),
                "el motivo concreto, que es lo unico accionable");
        assertTrue(plano(prompt).contains("no anadas ninguna otra"), () -> extracto(prompt));
        assertTrue(plano(prompt).contains("OMITELA del arreglo: conservarla es mejor"),
                "la salida honesta cuando no puede resolverla");
    }

    @Test
    @DisplayName("Sin sesiones senaladas no se puede pedir una correccion")
    void sinSenaladas() {
        var e = assertThrows(IllegalArgumentException.class,
                () -> builder.build(contexto(PlanAdjustment.none(), semanaDeTres()),
                        derivacion(), snapshot(), null));

        assertTrue(e.getMessage().contains("sin sesiones senaladas"));
    }

    @Test
    @DisplayName("Una senalada sin derivada no se pide: no hay fecha que darle")
    void senaladaSinDerivada() {
        // 999 no esta en la derivacion. Sin este guardia el bloque sale sin la
        // linea de la fecha, en silencio, y V8C lo rechaza DESPUES de haber
        // gastado la llamada.
        var e = assertThrows(IllegalArgumentException.class,
                () -> builder.build(
                        contexto(senalando(999L, TipoIntervencion.ACORTAR_O_REORGANIZAR),
                                semanaDeTres()),
                        derivacion(), snapshot(), null));

        assertTrue(e.getMessage().contains("999"), e::getMessage);
        assertTrue(e.getMessage().contains("derivada"), e::getMessage);
    }

    @Test
    @DisplayName("Lo enviado y lo que se persiste son la MISMA cadena")
    void loEnviadoEsLoPersistido() {
        var resultado = builder.build(contexto(senalando(102L,
                        TipoIntervencion.ACORTAR_O_REORGANIZAR), semanaDeTres()),
                derivacion(), snapshot(), null);

        // No «equivalentes»: la misma. Dos serializaciones por separado pueden
        // separarse, y la trazabilidad de la tesis depende de que no lo hagan.
        assertTrue(resultado.text().contains(
                        "Datos de entrada:\n" + resultado.snapshotJson() + "\n"),
                "el JSON devuelto para persistir tiene que ser literalmente el"
                        + " incrustado en el prompt");
    }

    @Test
    @DisplayName("Avisa de que los principios mandan a un bloque que ya no recibe")
    void redirigeLosPrincipios() {
        // El texto de principios es el MISMO de generacion y dos de sus
        // principios mandan leer previous_week, que la proyeccion no lleva. Sin
        // esta nota el prompt se contradice y el modelo decide sobre un bloque
        // inexistente.
        var prompt = construir(senalando(102L, TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA));

        assertTrue(plano(prompt).contains("previous_week"),
                "los principios lo nombran, asi que hay que desmentirlo");
        assertTrue(plano(prompt).contains("NO recibes previous_week"), () -> extracto(prompt));
        assertTrue(plano(prompt).contains("esta en SESIONES SENALADAS"),
                "y decirle donde esta lo que si tiene");
    }

    @Test
    @DisplayName("Distingue el peso PAUTADO del peso USADO")
    void pautadoYUsadoSeparados() {
        // El principio 8 sube la carga sobre el peso que SE USO. Mezclarlos
        // haria que subiera sobre un numero que la persona nunca levanto.
        var semana = semanaConCargas(102L, 304L,
                new BigDecimal("60.0"), new BigDecimal("50.0"));
        var prompt = construir(senalando(102L, TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA),
                semana);

        assertTrue(prompt.contains("pautado 60.0 kg"), () -> extracto(prompt));
        assertTrue(prompt.contains("uso 50.0 kg"), () -> extracto(prompt));
    }

    @Test
    @DisplayName("El prompt lleva la PROYECCION, no el snapshot completo")
    void llevaLaProyeccionNoElSnapshotCompleto() {
        var resultado = builder.build(contexto(senalando(102L,
                        TipoIntervencion.ACORTAR_O_REORGANIZAR), semanaDeTres()),
                derivacion(), snapshot(), null);

        for (var bloque : List.of("previous_week", "suggested_split", "adjustment",
                "reduce_volume_caps", "slots_exigibles", "days_per_week")) {
            assertFalse(resultado.snapshotJson().contains(bloque),
                    () -> "la proyeccion no puede llevar " + bloque);
        }
        assertTrue(resultado.snapshotJson().contains("available_exercises"));
        assertTrue(resultado.snapshotJson().contains(SessionFixPromptBuilder.VERSION),
                "la version de reglas registrada tiene que ser la de ADAPTACION");
    }

    @Test
    @DisplayName("El historial de las conservadas no viaja, aunque su ejercicio si este en el catalogo")
    void elHistorialDeLasConservadasNoViaja() {
        // Tu objecion, literal: no basta con buscar que 740 no aparezca, porque
        // 740 esta LEGITIMAMENTE en available_exercises. Hay que distinguir el
        // catalogo elegible del historial que no queremos mostrar.
        var resultado = builder.build(contexto(senalando(102L,
                        TipoIntervencion.ACORTAR_O_REORGANIZAR), semanaDeTres()),
                derivacion(), snapshot(), null);
        var prompt = resultado.text();

        assertTrue(prompt.contains("740"),
                "740 SI debe estar: es un ejercicio elegible del catalogo. Si esta"
                        + " asercion falla, la prueba ya no distingue catalogo de historial");

        // Las CLAVES se comprueban sobre el JSON, no sobre el prompt: el prompt
        // nombra «previous_week» a proposito, en la nota que le dice al modelo
        // que no lo va a recibir.
        for (var clave : List.of("previous_week", "weighted_adherence_pct",
                "body_part_distribution", "average_session_rpe")) {
            assertFalse(resultado.snapshotJson().contains(clave),
                    () -> "la proyeccion no puede llevar la clave " + clave);
        }

        // Los VALORES se comprueban sobre el prompt entero: no deben estar en
        // ninguna parte, ni en los datos ni en el texto.
        for (var delHistorial : List.of(
                "57.5",        // actual_load_kg de la conservada 101
                "41.25",       // actual_load_kg de la conservada 103
                "2026-10-12",  // fecha de la conservada 101
                "2026-10-16",  // fecha de la conservada 103
                "66.67")) {    // la adherencia ponderada de la semana completa
            assertFalse(prompt.contains(delHistorial),
                    () -> "se filtro «" + delHistorial + "», que solo puede venir del"
                            + " historial de las sesiones conservadas");
        }

        // Y lo de la senalada si esta, para que no pase por estar vacia.
        assertTrue(prompt.contains("source_planned_workout_id: 102"), () -> extracto(prompt));
    }

    // =======================================================================
    // Material
    // =======================================================================

    private String construir(PlanAdjustment orden) {
        return construir(orden, semanaDeTres());
    }

    private String construir(PlanAdjustment orden, PreviousWeekSummary semana) {
        return builder.build(contexto(orden, semana), derivacion(), snapshot(), null).text();
    }

    /**
     * El snapshot de generacion inicial, COMPLETO y con todo lo que no debe
     * viajar: {@code previous_week} con las tres sesiones y sus cargas,
     * {@code adjustment} porcentual, {@code suggested_split},
     * {@code reduce_volume_caps} y {@code slots_exigibles}.
     * <p>
     * Deliberadamente lo usan TODAS las pruebas, no solo la de filtracion.
     * Con un {@code "{}"} vacio -como en la v1- cualquier prueba de «no se
     * filtra» pasaba por no haber nada que filtrar.
     */
    private static PlanInputSnapshot snapshot() {
        var ejercicios = List.of(
                catalogo(740L, "Dominadas", "BACK", "PULL"),
                catalogo(304L, "Press banca", "CHEST", "PUSH_H"),
                catalogo(592L, "Sentadilla", "LEGS", "SQUAT"));

        var previas = List.of(
                previa(L1, "COMPLETED", null, new BigDecimal("100.00"),
                        previo(740L, "Dominadas", new BigDecimal("57.5"))),
                previa(L1.plusDays(2), "SKIPPED", "LACK_OF_TIME", BigDecimal.ZERO,
                        previo(304L, "Press banca", new BigDecimal("30.0"))),
                previa(L1.plusDays(4), "COMPLETED", null, new BigDecimal("100.00"),
                        previo(592L, "Sentadilla", new BigDecimal("41.25"))));

        var constraints = new PlanInputSnapshot.Constraints(
                (short) 2, L2, L2.plusDays(6), 3,
                List.of((short) 1, (short) 3, (short) 5), 45, "GYM", 3,
                12, 10, 3, 31, 2, 120,
                List.of(new PlanInputSnapshot.SuggestedSession(
                        L2, "FULL_BODY", List.of("BACK", "CHEST"), 2, 3)),
                List.of(new PlanInputSnapshot.ReduceVolumeCap(740L, 3, 10, "V19")),
                List.of(new PlanInputSnapshot.DosisPorClase("BASICO", 3, 10, 90, 7.5)),
                4, 38.25, List.of("PULL", "PUSH_H", "SQUAT", "HINGE"));

        var adjustment = new PlanInputSnapshot.Adjustment(
                List.of("NONE"), null, null, null, 0.0, 0.0, 3,
                "baja adherencia", "LACK_OF_TIME", "reparte mejor la semana");

        var previousWeek = new PlanInputSnapshot.PreviousWeek(
                new BigDecimal("66.67"), new BigDecimal("6.5"), 480,
                Map.of("BACK", 1, "CHEST", 1, "LEGS", 1), previas);

        return new PlanInputSnapshot("GEN-IN-SCHEMA", "PRIN-1.0", "GEN-IN-1.6",
                new PlanInputSnapshot.User(30, "M", new BigDecimal("175"),
                        new BigDecimal("75"), new BigDecimal("72"), "INTERMEDIATE",
                        "GAIN_MUSCLE", "ganar musculo", null, List.of("body weight")),
                constraints, adjustment, previousWeek, ejercicios, null);
    }

    private static PlanInputSnapshot.AvailableExercise catalogo(long id, String nombre,
                                                                String zona, String patron) {
        return new PlanInputSnapshot.AvailableExercise(id, nombre, zona, "body weight", 2,
                "SETS_REPS", nombre, 8, patron, patron, "BASICO");
    }

    private static PlanInputSnapshot.PreviousWorkout previa(
            LocalDate fecha, String estado, String motivo, BigDecimal pct,
            PlanInputSnapshot.PreviousExercise... ejercicios) {
        return new PlanInputSnapshot.PreviousWorkout(fecha, "FULL_BODY", estado, motivo,
                true, (short) 7, pct, List.of(ejercicios));
    }

    private static PlanInputSnapshot.PreviousExercise previo(long id, String nombre,
                                                             BigDecimal carga) {
        return new PlanInputSnapshot.PreviousExercise(id, nombre, "SETS_REPS",
                (short) 3, (short) 10, null, (short) 3, 30, null, carga,
                new BigDecimal("100.00"), "COMPLETED", null);
    }

    /**
     * Colapsa los espacios y saltos de linea.
     * <p>
     * Las reglas van en un bloque de texto con sangria, asi que una frase se
     * parte en dos lineas y un contains literal falla aunque el contenido este.
     * Comprobar sobre el texto normalizado evita que cualquier reedicion del
     * prompt rompa pruebas que en realidad siguen cumpliendose.
     */
    private static String plano(String prompt) {
        return prompt.replaceAll("\\s+", " ");
    }

    private static String extracto(String prompt) {
        int i = prompt.indexOf("SESIONES SENALADAS");
        return i < 0 ? prompt : prompt.substring(i, Math.min(prompt.length(), i + 900));
    }

    private static PlanAdjustment senalando(long id, TipoIntervencion tipo) {
        return new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null,
                0.0, 0.0, null, null, null, "baja adherencia", null, null,
                List.of(new PlanAdjustment.TargetedSession(id, tipo)));
    }

    private static PlanGenerationContext contexto(PlanAdjustment orden,
                                                  PreviousWeekSummary semana) {
        return new PlanGenerationContext(1L, (short) 2, L2, L2.plusDays(6),
                perfil(), orden, semana, List.of(), null, null);
    }

    private static PlanningProfile perfil() {
        return new PlanningProfile(1L, 30, "M", new BigDecimal("175"), new BigDecimal("75"),
                new BigDecimal("72"), "INTERMEDIATE", "GAIN_MUSCLE", "ganar musculo", null,
                "GYM", 3, List.of((short) 1, (short) 3, (short) 5), 45,
                List.of("body weight"), List.of(),
                SafetyProfile.Declaradas.sinLimitaciones());
    }

    private static WeekDerivation derivacion() {
        return new WeekDerivation(List.of(
                new DerivedWorkout(101L, sesion(L2, "Cuerpo completo A", ej(740L))),
                new DerivedWorkout(102L, sesion(L2.plusDays(2), "Cuerpo completo B", ej(304L))),
                new DerivedWorkout(103L, sesion(L2.plusDays(4), "Cuerpo completo C", ej(592L)))),
                List.of());
    }

    private static PlanDraft.DraftWorkout sesion(LocalDate fecha, String nombre,
                                                 PlanDraft.DraftExercise... ejercicios) {
        return new PlanDraft.DraftWorkout(fecha, WorkoutFocus.FULL_BODY, nombre, 45,
                List.of(ejercicios));
    }

    private static PlanDraft.DraftExercise ej(long id) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS,
                (short) 3, (short) 10, null, null, (short) 90, null);
    }

    private static PreviousWeekSummary semanaDeTres() {
        return semana(null);
    }

    private static PreviousWeekSummary semanaConDolorEn(long exerciseId) {
        return semana(exerciseId);
    }

    /** La semana de tres, con peso pautado y usado distintos en una sesion. */
    private static PreviousWeekSummary semanaConCargas(long workoutId, long exerciseId,
                                                       BigDecimal pautado,
                                                       BigDecimal usado) {
        var base = semana(null);
        var conCargas = base.prescriptions().stream()
                .map(p -> !(p.plannedWorkoutId() == workoutId && p.exerciseId() == exerciseId)
                        ? p
                        : new PreviousWeekSummary.PrescriptionOutcome(
                        p.plannedWorkoutId(), p.scheduledDate(), p.exerciseId(),
                        p.name(), p.prescriptionType(), p.sets(), p.reps(),
                        p.durationSeconds(), pautado, p.actualSets(),
                        p.actualRepsTotal(), p.actualDurationSeconds(), usado,
                        p.completionPct(), p.exerciseStatus(), p.skipReason(),
                        p.status(), p.restSeconds()))
                .toList();

        return new PreviousWeekSummary(base.weightedAdherencePct(),
                base.averageSessionRpe(), base.totalVolume(), base.bodyPartDistribution(),
                base.workouts(), conCargas, base.exerciseIdsUsedLast7Days());
    }

    private static PreviousWeekSummary semana(Long conDolor) {
        var workouts = List.of(
                outcome(101L, L1, "COMPLETED", null, new BigDecimal("100.00")),
                outcome(102L, L1.plusDays(2), "SKIPPED",
                        conDolor == null ? "LACK_OF_TIME" : "PAIN_OR_DISCOMFORT",
                        BigDecimal.ZERO),
                outcome(103L, L1.plusDays(4), "COMPLETED", null, new BigDecimal("100.00")));

        var prescriptions = List.of(
                prescripcion(101L, 740L, null),
                prescripcion(102L, 304L, conDolor != null && conDolor == 304L
                        ? "PAIN_OR_DISCOMFORT" : "LACK_OF_TIME"),
                prescripcion(103L, 592L, null));

        return new PreviousWeekSummary(new BigDecimal("66.67"), new BigDecimal("6.5"), 480,
                Map.of(), workouts, prescriptions, List.of());
    }

    private static PreviousWeekSummary.WorkoutOutcome outcome(long id, LocalDate fecha,
                                                              String estado, String motivo,
                                                              BigDecimal pct) {
        return new PreviousWeekSummary.WorkoutOutcome(id, fecha, "FULL_BODY", estado,
                motivo, true, estado, (short) 7, pct, "Cuerpo completo", (short) 45);
    }

    private static PreviousWeekSummary.PrescriptionOutcome prescripcion(long workoutId,
                                                                        long exerciseId,
                                                                        String motivo) {
        return new PreviousWeekSummary.PrescriptionOutcome(
                workoutId, L1, exerciseId, "ejercicio " + exerciseId, "SETS_REPS",
                (short) 3, (short) 10, null, null,
                (short) 3, 30, null, null,
                new BigDecimal("100.00"), "COMPLETED", motivo, "COMPLETED", (short) 90);
    }
}
