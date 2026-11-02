package main.web.services.fitsense.planning.application.internal.commandservices;

import main.web.services.fitsense.planning.domain.model.aggregates.PlanAdaptationAttempt;
import main.web.services.fitsense.planning.domain.model.aggregates.WeeklyTrainingPlan;
import main.web.services.fitsense.planning.domain.model.entities.PlannedWorkout;
import main.web.services.fitsense.planning.domain.model.entities.PlannedWorkoutExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptationAttempt;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.PlanAdaptationAttemptRepository;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.PlannedWorkoutRepository;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * D8 · LA EXENCION DE V20 SIGUE A LA SESION MIENTRAS SIGA IDENTICA (27/10/2026).
 * <p>
 * El linaje de un viernes, de la semana medida hacia atras:
 * <pre>
 * 104 (copia)  &lt;- 103 (senalada por fatiga, sin efecto o respaldo)  &lt;- 102 (copia)  &lt;- 101 (ACORTAR aplicado)  &lt;- 100 (generacion)
 * </pre>
 * Es exactamente lo que dejo sin plan cinco semanas en la campana con Replicate
 * real: la copia de 104 perdia la exencion por el paso 103. Cada paso es un plan
 * con su unico intento V32.
 */
@DisplayName("D8 · la herencia sigue tras un sin efecto o respaldo identico; se corta si cambia la sesion, sin D8 o con dolor")
class ReduccionAutorizadaResolverTest {

    private final PlannedWorkoutRepository sesiones = mock(PlannedWorkoutRepository.class);
    private final PlanAdaptationAttemptRepository intentos = mock(PlanAdaptationAttemptRepository.class);
    private final ReduccionAutorizadaResolver resolver = new ReduccionAutorizadaResolver(sesiones, intentos,
            new JsonSupport());

    /** El viernes acortado: tres colocaciones, 26 min (el piso de V20 para 45 min es 31). */
    private static final List<Object[]> ACORTADO = List.of(
            new Object[]{755L, 3, 6, null, 150, null},
            new Object[]{592L, 3, 6, null, 150, null},
            new Object[]{1176L, 3, null, 120, 60, null});

    @Test
    @DisplayName("los cinco casos reales: ACORTAR -> copia -> fatiga con respaldo identico -> copia: se hereda")
    void respaldoIdenticoHereda() {
        cadena(intento(103L, "REVISAR_CARGA_Y_RECUPERACION", AdaptationAttempt.Status.RESPALDO), ACORTADO, 26);
        assertEquals(Set.of(104L), resolver.resolver(List.of(sesiones.findById(104L).orElseThrow())));
    }

    @Test
    @DisplayName("igual con un SIN_EFECTO de la IA (devolvio la sesion identica)")
    void sinEfectoIdenticoHereda() {
        cadena(intento(103L, "REVISAR_CARGA_Y_RECUPERACION", AdaptationAttempt.Status.SIN_EFECTO), ACORTADO, 26);
        assertEquals(Set.of(104L), resolver.resolver(List.of(sesiones.findById(104L).orElseThrow())));
    }

    @Test
    @DisplayName("otra sesion de la misma semana cambio (aplicada): no le quita la herencia a esta")
    void otraSesionCambiadaNoAfecta() {
        var i = intento(103L, "REVISAR_CARGA_Y_RECUPERACION", AdaptationAttempt.Status.APLICADA);
        when(i.getAppliedSessions()).thenReturn(new Long[]{900L});
        when(i.getTargetedSessions()).thenReturn(new Long[]{102L, 900L});
        when(i.getTargetedInterventions()).thenReturn(new String[]{"REVISAR_CARGA_Y_RECUPERACION", "INVESTIGAR_CAMBIO_MINIMO"});
        cadena(i, ACORTADO, 26);
        assertEquals(Set.of(104L), resolver.resolver(List.of(sesiones.findById(104L).orElseThrow())));
    }

    // ----------------------------------------------------------------- negativas

    @Test
    @DisplayName("sin D8 original (el primer paso no es un ACORTAR aplicado) no hay excepcion")
    void sinD8Original() {
        cadena(intento(103L, "REVISAR_CARGA_Y_RECUPERACION", AdaptationAttempt.Status.RESPALDO), ACORTADO, 26);
        // 102 deriva directamente de la generacion: nunca hubo un ACORTAR aplicado.
        var s102 = sesiones.findById(102L).orElseThrow();
        when(s102.getSourcePlannedWorkoutId()).thenReturn(null);
        assertEquals(Set.of(), resolver.resolver(List.of(sesiones.findById(104L).orElseThrow())));
    }

    @Test
    @DisplayName("si cambia la dosis en el paso sin efecto (p. ej. una serie), no se hereda")
    void cambiaLaDosis() {
        cadena(intento(103L, "REVISAR_CARGA_Y_RECUPERACION", AdaptationAttempt.Status.RESPALDO), ACORTADO, 26);
        cambiar(103L, 0, new Object[]{755L, 2, 6, null, 150, null});
        assertEquals(Set.of(), resolver.resolver(List.of(sesiones.findById(104L).orElseThrow())));
    }

    @Test
    @DisplayName("si cambia la carga (incluida la retirada por V11) o el descanso, no se hereda")
    void cambiaCargaODescanso() {
        cadena(intento(103L, "REVISAR_CARGA_Y_RECUPERACION", AdaptationAttempt.Status.RESPALDO), ACORTADO, 26);
        var conCarga = new ArrayList<>(ACORTADO);
        conCarga.set(0, new Object[]{755L, 3, 6, null, 150, new BigDecimal("20.00")});
        poner(102L, conCarga, 26);   // la semana anterior tenia 20 kg; el respaldo la publico sin ellos
        assertEquals(Set.of(), resolver.resolver(List.of(sesiones.findById(104L).orElseThrow())));

        cadena(intento(103L, "REVISAR_CARGA_Y_RECUPERACION", AdaptationAttempt.Status.RESPALDO), ACORTADO, 26);
        cambiar(103L, 1, new Object[]{592L, 3, 6, null, 120, null});
        assertEquals(Set.of(), resolver.resolver(List.of(sesiones.findById(104L).orElseThrow())));
    }

    @Test
    @DisplayName("si cambia el contenido (otro ejercicio, otro orden o otra duracion), se reevalua: no se hereda")
    void cambiaElContenido() {
        cadena(intento(103L, "REVISAR_CARGA_Y_RECUPERACION", AdaptationAttempt.Status.RESPALDO), ACORTADO, 26);
        cambiar(103L, 2, new Object[]{1054L, 3, null, 120, 60, null});
        assertEquals(Set.of(), resolver.resolver(List.of(sesiones.findById(104L).orElseThrow())));

        cadena(intento(103L, "REVISAR_CARGA_Y_RECUPERACION", AdaptationAttempt.Status.RESPALDO), ACORTADO, 26);
        poner(103L, List.of(ACORTADO.get(1), ACORTADO.get(0), ACORTADO.get(2)), 26);
        assertEquals(Set.of(), resolver.resolver(List.of(sesiones.findById(104L).orElseThrow())));

        cadena(intento(103L, "REVISAR_CARGA_Y_RECUPERACION", AdaptationAttempt.Status.RESPALDO), ACORTADO, 26);
        poner(103L, ACORTADO, 22);
        assertEquals(Set.of(), resolver.resolver(List.of(sesiones.findById(104L).orElseThrow())));
    }

    @Test
    @DisplayName("linaje roto (falta el intento V32 de un paso, o la sesion de origen) -> sin excepcion")
    void linajeRoto() {
        cadena(intento(103L, "REVISAR_CARGA_Y_RECUPERACION", AdaptationAttempt.Status.RESPALDO), ACORTADO, 26);
        when(intentos.findByResultingPlanId(1102L)).thenReturn(List.of());
        assertEquals(Set.of(), resolver.resolver(List.of(sesiones.findById(104L).orElseThrow())));

        cadena(intento(103L, "REVISAR_CARGA_Y_RECUPERACION", AdaptationAttempt.Status.RESPALDO), ACORTADO, 26);
        when(sesiones.findById(102L)).thenReturn(Optional.empty());
        assertEquals(Set.of(), resolver.resolver(List.of(sesiones.findById(104L).orElseThrow())));
    }

    @Test
    @DisplayName("una intervencion por dolor no hereda D8, aunque la sesion salga identica")
    void dolorNoHereda() {
        cadena(intento(103L, "SEGURIDAD_ANTE_DOLOR", AdaptationAttempt.Status.SIN_EFECTO), ACORTADO, 26);
        assertEquals(Set.of(), resolver.resolver(List.of(sesiones.findById(104L).orElseThrow())));
    }

    // =======================================================================
    // El linaje
    // =======================================================================

    /** Cada sesion N (101..104) vive en el plan 1000+N y la publico un unico intento. */
    private void cadena(PlanAdaptationAttempt paso103, List<Object[]> ejercicios, int minutos) {
        reset(sesiones, intentos);
        sesion(100L, null, ACORTADO_MAS_RELLENO(), 45);
        sesion(101L, 100L, ejercicios, minutos);
        sesion(102L, 101L, ejercicios, minutos);
        sesion(103L, 102L, ejercicios, minutos);
        sesion(104L, 103L, ejercicios, minutos);
        // Los dobles se crean ANTES de los when(): crearlos dentro de otro when() deja el stub a medias.
        var i101 = acortar101();
        var i102 = copia(102L);
        var i104 = copia(104L);
        when(intentos.findByResultingPlanId(1101L)).thenReturn(List.of(i101));
        when(intentos.findByResultingPlanId(1102L)).thenReturn(List.of(i102));
        when(intentos.findByResultingPlanId(1103L)).thenReturn(List.of(paso103));
        when(intentos.findByResultingPlanId(1104L)).thenReturn(List.of(i104));
    }

    private static List<Object[]> ACORTADO_MAS_RELLENO() {
        var l = new ArrayList<>(ACORTADO);
        l.add(new Object[]{1063L, 2, 13, null, 90, null});
        return l;
    }

    private void sesion(long id, Long origen, List<Object[]> ejercicios, int minutos) {
        var plan = mock(WeeklyTrainingPlan.class);
        when(plan.getId()).thenReturn(1000L + id);
        var l = ejercicios(ejercicios);
        var w = mock(PlannedWorkout.class);
        when(w.getId()).thenReturn(id);
        when(w.getSourcePlannedWorkoutId()).thenReturn(origen);
        when(w.getPlan()).thenReturn(plan);
        when(w.getFocusCode()).thenReturn(WorkoutFocus.FULL_BODY);
        when(w.getExpectedDurationMinutes()).thenReturn((short) minutos);
        when(w.exercisesView()).thenReturn(l);
        when(sesiones.findById(id)).thenReturn(Optional.of(w));
    }

    private void poner(long id, List<Object[]> ejercicios, int minutos) {
        var w = sesiones.findById(id).orElseThrow();
        var l = ejercicios(ejercicios);
        when(w.exercisesView()).thenReturn(l);
        when(w.getExpectedDurationMinutes()).thenReturn((short) minutos);
    }

    private void cambiar(long id, int posicion, Object[] nuevo) {
        var l = new ArrayList<>(ACORTADO);
        l.set(posicion, nuevo);
        poner(id, l, 26);
    }

    private static List<PlannedWorkoutExercise> ejercicios(List<Object[]> filas) {
        var l = new ArrayList<PlannedWorkoutExercise>();
        short orden = 1;
        for (var f : filas) {
            var e = mock(PlannedWorkoutExercise.class);
            boolean duracion = f[3] != null;
            when(e.getExerciseId()).thenReturn((Long) f[0]);
            when(e.getExerciseOrder()).thenReturn(orden++);
            when(e.getPrescriptionType()).thenReturn(duracion ? PrescriptionType.DURATION : PrescriptionType.SETS_REPS);
            when(e.getPlannedSets()).thenReturn(((Integer) f[1]).shortValue());
            when(e.getPlannedReps()).thenReturn(f[2] == null ? null : ((Integer) f[2]).shortValue());
            when(e.getPlannedDurationSeconds()).thenReturn((Integer) f[3]);
            when(e.getRestSeconds()).thenReturn(((Integer) f[4]).shortValue());
            when(e.getTargetLoadKg()).thenReturn((BigDecimal) f[5]);
            l.add(e);
        }
        return l;
    }

    // =======================================================================
    // La evidencia V32
    // =======================================================================

    private static PlanAdaptationAttempt acortar101() {
        // El intento que publico 101: ACORTAR aplicado sobre su origen 100.
        var i = vacio(AdaptationAttempt.Status.APLICADA);
        when(i.getTargetedSessions()).thenReturn(new Long[]{100L});
        when(i.getTargetedInterventions()).thenReturn(new String[]{"ACORTAR_O_REORGANIZAR"});
        when(i.getAppliedSessions()).thenReturn(new Long[]{100L});
        return i;
    }

    private static PlanAdaptationAttempt copia(long sesion) {
        // Copia: la sesion de origen (sesion - 1) quedo conservada.
        var i = vacio(AdaptationAttempt.Status.SIN_ORDEN);
        when(i.getConservedSessions()).thenReturn(new Long[]{sesion - 1});
        return i;
    }

    /** El intento que publico 103 senalando su origen 102 con esa intervencion, sin efecto. */
    private static PlanAdaptationAttempt intento(long sesion, String intervencion, AdaptationAttempt.Status estado) {
        var i = vacio(estado);
        when(i.getTargetedSessions()).thenReturn(new Long[]{sesion - 1});
        when(i.getTargetedInterventions()).thenReturn(new String[]{intervencion});
        when(i.getNoEffectSessions()).thenReturn(new Long[]{sesion - 1});
        return i;
    }

    private static PlanAdaptationAttempt vacio(AdaptationAttempt.Status estado) {
        var i = mock(PlanAdaptationAttempt.class);
        when(i.getStatus()).thenReturn(estado);
        when(i.getConservationReport()).thenReturn("{}");
        when(i.getAppliedSessions()).thenReturn(new Long[0]);
        when(i.getNoEffectSessions()).thenReturn(new Long[0]);
        when(i.getConservedSessions()).thenReturn(new Long[0]);
        when(i.getProgressedSessions()).thenReturn(new Long[0]);
        when(i.getTargetedSessions()).thenReturn(new Long[0]);
        when(i.getTargetedInterventions()).thenReturn(new String[0]);
        return i;
    }
}
