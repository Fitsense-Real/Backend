package main.web.services.fitsense.planning.application.internal.commandservices;

import main.web.services.fitsense.planning.application.internal.commandservices.PainMemoryService.ExclusionEfectiva;
import main.web.services.fitsense.planning.application.internal.commandservices.PainMemoryService.Origen;
import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalCatalogService;
import main.web.services.fitsense.planning.domain.model.aggregates.UserSafetyExclusion;
import main.web.services.fitsense.planning.domain.model.aggregates.UserSafetyExclusion.Reason;
import main.web.services.fitsense.planning.domain.model.aggregates.UserSafetyExclusion.RecordedBy;
import main.web.services.fitsense.planning.domain.model.valueobjects.ExerciseEquivalence;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.UserSafetyExclusionRepository;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.WeeklyTrainingPlanRepository;
import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;
import main.web.services.fitsense.shared.domain.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * O20 (opcion A y ajuste final): la exclusion EFECTIVA por dolor, sin base de datos. Lo confirmado excluye su familia
 * equivalente de forma permanente; un dolor de sesion sin identificar excluye sus ejercicios y, solo durante dos
 * semanas con plan, su familia; identificar despues lo confirma; nada se reactiva solo.
 */
@DisplayName("O20: PainMemoryService, exclusiones efectivas (familia permanente y provisional) e identificacion")
class PainMemoryFamiliaTest {

    static final long U = 7L;
    static final LocalDate VIERNES = LocalDate.of(2026, 1, 9);            // sesion con dolor, semana del 5/1
    static final LocalDate S0 = LocalDate.of(2026, 1, 5), S1 = S0.plusWeeks(1), S2 = S0.plusWeeks(2);

    final UserSafetyExclusionRepository repositorio = mock(UserSafetyExclusionRepository.class);
    final WeeklyTrainingPlanRepository planes = mock(WeeklyTrainingPlanRepository.class);
    final ExternalCatalogService catalogo = mock(ExternalCatalogService.class);
    final main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalProfilingService perfiles =
            mock(main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalProfilingService.class);
    final SafetyConflictChecker conflictos = mock(SafetyConflictChecker.class);
    final PainMemoryService dolor = new PainMemoryService(repositorio, planes, null, perfiles, catalogo, conflictos);
    final List<UserSafetyExclusion> filas = new ArrayList<>();

    private UserSafetyExclusion fila(long ejercicio, Reason motivo, long sesion) {
        var x = new UserSafetyExclusion(U, ejercicio, motivo, 1L, sesion, null, VIERNES, RecordedBy.CICLO_SEMANAL);
        filas.add(x);
        // Como el repositorio real: solo las que siguen ACTIVA en el momento de la lectura.
        when(repositorio.findByUserIdAndStatusOrderByIdAsc(U, UserSafetyExclusion.Status.ACTIVA))
                .thenAnswer(inv -> filas.stream().filter(UserSafetyExclusion::activa).toList());
        return x;
    }

    private void semanasConPlan(LocalDate... semanas) {
        when(planes.semanasConPlan(U)).thenReturn(List.of(semanas));
    }

    private static ExerciseEquivalence variante(long id) {
        return new ExerciseEquivalence(id, "VARIANTE_MISMO_EJERCICIO", null, "mismo nombre salvo v. N");
    }

    private static ExerciseEquivalence angulo(long id) {
        return new ExerciseEquivalence(id, "PRECAUCION_OTRO_ANGULO", "PA-004 chest press", "mismo gesto con otro angulo");
    }

    private static ExerciseEquivalence gesto(long id) {
        return new ExerciseEquivalence(id, "MISMO_GESTO", "G2-035 front raise", "mismo gesto (front raise)");
    }

    private Map<Long, ExclusionEfectiva> efectivas() {
        return dolor.exclusionesEfectivas(U).stream()
                .collect(Collectors.toMap(ExclusionEfectiva::exerciseId, Function.identity()));
    }

    @Test
    @DisplayName("Dolor identificado en 537: se excluyen 537 (directa, confirmada) y su familia (permanente)")
    void identificadoExcluyeLaFamilia() {
        fila(537L, Reason.DOLOR_IDENTIFICADO, 50L);
        semanasConPlan(S0);
        when(catalogo.fetchEquivalentVariants(Set.of(537L)))
                .thenReturn(Map.of(537L, List.of(variante(538L), variante(28L), gesto(707L))));
        assertEquals(Set.of(28L, 537L, 538L, 707L), dolor.excluidosActivos(U));
        assertEquals(Set.of(537L), dolor.excluidosActivosDirectos(U), "solo 537 tiene fila: la familia es derivada");
        var e = efectivas();
        assertEquals(Origen.DIRECTA, e.get(537L).origen());
        assertTrue(e.get(537L).confirmada());
        assertEquals(50L, e.get(537L).sourcePlannedWorkoutId());
        assertEquals(Origen.EQUIVALENTE, e.get(707L).origen());
        assertEquals(537L, e.get(707L).derivaDe());
        assertEquals("MISMO_GESTO", e.get(707L).nivel());
        assertEquals("G2-035 front raise", e.get(707L).grupo());
        assertEquals("VARIANTE_MISMO_EJERCICIO", e.get(538L).nivel());
        assertTrue(e.values().stream().noneMatch(ExclusionEfectiva::provisional));
    }

    @Test
    @DisplayName("Lo confirmado no caduca: diez semanas con plan despues, la familia sigue excluida")
    void confirmadoNoCaduca() {
        fila(537L, Reason.DOLOR_IDENTIFICADO, 50L);
        semanasConPlan(java.util.stream.IntStream.range(0, 10).mapToObj(S0::plusWeeks).toArray(LocalDate[]::new));
        when(catalogo.fetchEquivalentVariants(Set.of(537L))).thenReturn(Map.of(537L, List.of(variante(538L))));
        assertEquals(Set.of(537L, 538L), dolor.excluidosActivos(U));
    }

    @Test
    @DisplayName("Dolor de sesion sin identificar: todo excluido; a las dos semanas con plan se pide confirmacion, sin levantar nada")
    void sesionSinIdentificarFamiliaProvisional() {
        fila(537L, Reason.DOLOR_SESION_SIN_IDENTIFICAR, 50L);
        fila(578L, Reason.DOLOR_SESION_SIN_IDENTIFICAR, 50L);
        when(catalogo.fetchEquivalentVariants(Set.of(537L, 578L)))
                .thenReturn(Map.of(537L, List.of(variante(538L)), 578L, List.of(gesto(337L))));
        // La adaptacion o regeneracion inmediata (aun sin plan posterior) y la semana siguiente: familia excluida.
        for (var semanas : List.of(new LocalDate[]{S0}, new LocalDate[]{S0, S1})) {
            semanasConPlan(semanas);
            assertEquals(Set.of(337L, 537L, 538L, 578L), dolor.excluidosActivos(U));
            var e = efectivas();
            assertFalse(e.get(537L).confirmada(), "un reporte general no confirma que ejercicio dolio");
            assertFalse(e.get(537L).provisional(), "la exclusion directa no es provisional: no caduca");
            assertTrue(e.get(538L).provisional());
            assertFalse(e.get(538L).confirmada());
            assertEquals(537L, e.get(538L).derivaDe());
        }
        efectivas().values().forEach(x -> assertFalse(x.requiereConfirmacion(), "aun no se pide: " + x));
        // Cierre final: tras dos semanas con plan NO se levanta nada (ni la familia): se pide confirmacion.
        for (var semanas : List.of(new LocalDate[]{S0, S1, S2}, new LocalDate[]{S0, S1, S2, S2.plusWeeks(5)})) {
            semanasConPlan(semanas);
            assertEquals(Set.of(337L, 537L, 538L, 578L), dolor.excluidosActivos(U), "el vencimiento no reactiva nada");
            efectivas().values().forEach(x -> assertTrue(x.requiereConfirmacion(), "se pide confirmacion: " + x));
        }
    }

    @Test
    @DisplayName("Identificado despues: la familia de ESE ejercicio pasa a ser permanente; el resto sigue pendiente")
    void identificadoDespuesConfirma() {
        var x = fila(537L, Reason.DOLOR_SESION_SIN_IDENTIFICAR, 50L);
        fila(578L, Reason.DOLOR_SESION_SIN_IDENTIFICAR, 50L);
        semanasConPlan(S0, S1, S2);
        x.identificar(OffsetDateTime.parse("2026-01-20T10:00:00Z"), "IDENTIFICACION_EXPLICITA_DEL_USUARIO prueba");
        when(catalogo.fetchEquivalentVariants(Set.of(537L, 578L)))
                .thenReturn(Map.of(537L, List.of(variante(538L)), 578L, List.of(gesto(337L))));
        assertEquals(Set.of(337L, 537L, 538L, 578L), dolor.excluidosActivos(U));
        var e = efectivas();
        assertTrue(e.get(537L).confirmada());
        assertFalse(e.get(537L).requiereConfirmacion());
        assertFalse(e.get(538L).provisional() || e.get(538L).requiereConfirmacion(), "538: permanente");
        assertFalse(e.get(578L).confirmada());
        assertTrue(e.get(578L).requiereConfirmacion() && e.get(337L).requiereConfirmacion(),
                "578 sigue sin confirmar: excluido con su familia y pendiente de confirmacion");
    }

    @Test
    @DisplayName("Una variante que deriva de uno confirmado y de uno provisional es permanente")
    void confirmadoManda() {
        fila(537L, Reason.DOLOR_IDENTIFICADO, 50L);
        fila(150L, Reason.DOLOR_SESION_SIN_IDENTIFICAR, 51L);
        semanasConPlan(S0);
        when(catalogo.fetchEquivalentVariants(Set.of(150L, 537L)))
                .thenReturn(Map.of(150L, List.of(gesto(707L)), 537L, List.of(gesto(707L))));
        var e = efectivas();
        assertFalse(e.get(707L).provisional());
        assertEquals(537L, e.get(707L).derivaDe());
    }

    @Test
    @DisplayName("Sin exclusiones no se consulta el catalogo")
    void sinExclusiones() {
        when(repositorio.findByUserIdAndStatusOrderByIdAsc(U, UserSafetyExclusion.Status.ACTIVA)).thenReturn(List.of());
        assertEquals(Set.of(), dolor.excluidosActivos(U));
        verify(catalogo, never()).fetchEquivalentVariants(any());
    }

    @Test
    @DisplayName("Si el catalogo falla, la exclusion registrada se mantiene (falla cerrado sobre lo directo)")
    void fallaCerrado() {
        fila(537L, Reason.DOLOR_IDENTIFICADO, 50L);
        fila(900L, Reason.DOLOR_SESION_SIN_IDENTIFICAR, 51L);
        semanasConPlan(S0);
        when(catalogo.fetchEquivalentVariants(any())).thenThrow(new IllegalStateException("catalogo caido"));
        assertEquals(Set.of(537L, 900L), dolor.excluidosActivos(U));
    }

    @Test
    @DisplayName("Reactivado lo que dolio (ninguna fila activa), su familia deja de estar excluida")
    void reactivadoNoExcluyeLaFamilia() {
        fila(900L, Reason.DOLOR_IDENTIFICADO, 50L);
        semanasConPlan(S0);
        when(catalogo.fetchEquivalentVariants(Set.of(900L))).thenReturn(Map.of());
        assertEquals(Set.of(900L), dolor.excluidosActivos(U), "537 ya no tiene fila activa: ni 537 ni 538 salen");
    }

    @Test
    @DisplayName("Identificar exige confirmacion explicita, una fila sin identificar y no reactiva el resto")
    void identificar() {
        var x = fila(537L, Reason.DOLOR_SESION_SIN_IDENTIFICAR, 50L);
        fila(578L, Reason.DOLOR_SESION_SIN_IDENTIFICAR, 50L);
        semanasConPlan(S0);
        when(repositorio.bloquearActivas(U, 537L)).thenReturn(List.of(x));
        when(catalogo.fetchEquivalentVariants(any())).thenReturn(Map.of(537L, List.of(variante(538L))));
        assertThrows(DomainRuleViolationException.class, () -> dolor.identificar(U, 537L, 50L, false));
        assertThrows(ResourceNotFoundException.class, () -> dolor.identificar(U, 537L, 99L, true), "otra sesion");
        var r = dolor.identificar(U, 537L, 50L, true);
        assertTrue(x.confirmada());
        assertNotNull(x.getIdentificationEvidence());
        assertEquals(List.of(538L), r.equivalentesExcluidos());
        assertEquals(List.of(578L), r.otrosDeLaSesionSiguenExcluidos());
        assertEquals(UserSafetyExclusion.Status.ACTIVA, filas.get(1).getStatus(), "578 no se reactiva");
        verify(repositorio).save(x);
        assertThrows(ResourceNotFoundException.class, () -> dolor.identificar(U, 537L, 50L, true), "ya confirmada");
        assertThrows(DomainRuleViolationException.class,
                () -> x.identificar(OffsetDateTime.now(), "otra vez"), "la entidad tampoco confirma dos veces");
        var identificada = fila(600L, Reason.DOLOR_IDENTIFICADO, 52L);
        assertThrows(DomainRuleViolationException.class, () -> identificada.identificar(OffsetDateTime.now(), "x"));
    }

    @Test
    @DisplayName("Precaucion por angulo: el mismo gesto con otro angulo sale como PRECAUCION, no como equivalente")
    void precaucionPorAngulo() {
        fila(992L, Reason.DOLOR_IDENTIFICADO, 50L);
        semanasConPlan(S0);
        when(catalogo.fetchEquivalentVariants(Set.of(992L)))
                .thenReturn(Map.of(992L, List.of(variante(991L), angulo(988L), angulo(1026L))));
        assertEquals(Set.of(988L, 991L, 992L, 1026L), dolor.excluidosActivos(U), "988 plano no se da por seguro");
        var e = efectivas();
        assertEquals(Origen.PRECAUCION, e.get(988L).origen());
        assertEquals("PRECAUCION_OTRO_ANGULO", e.get(988L).nivel());
        assertEquals(992L, e.get(988L).derivaDe());
        assertTrue(e.get(988L).confirmada() && !e.get(988L).provisional(), "dura lo que la exclusion confirmada");
        assertEquals(Origen.EQUIVALENTE, e.get(991L).origen());
    }

    @Test
    @DisplayName("Si una variante es equivalente de uno y precaucion de otro, cuenta como equivalente")
    void equivalenteMandaSobrePrecaucion() {
        fila(992L, Reason.DOLOR_IDENTIFICADO, 50L);
        fila(632L, Reason.DOLOR_IDENTIFICADO, 51L);
        semanasConPlan(S0);
        when(catalogo.fetchEquivalentVariants(Set.of(632L, 992L)))
                .thenReturn(Map.of(632L, List.of(angulo(988L)), 992L, List.of(gesto(988L))));
        assertEquals(Origen.EQUIVALENTE, efectivas().get(988L).origen());
    }

    private void perfilSinConflictos() {
        var perfil = mock(main.web.services.fitsense.planning.domain.model.valueobjects.PlanningProfile.class);
        when(perfil.age()).thenReturn(35);
        when(perfil.fitnessLevel()).thenReturn("INTERMEDIATE");
        when(perfiles.fetchProfile(U)).thenReturn(java.util.Optional.of(perfil));
        when(conflictos.motivos(any(), any(), any())).thenReturn(Map.of());
    }

    @Test
    @DisplayName("Identificar y confirmar no relacionados: solo esos se levantan; el resto de la sesion sigue excluido")
    void identificarConNoRelacionados() {
        var x = fila(537L, Reason.DOLOR_SESION_SIN_IDENTIFICAR, 50L);
        var y = fila(578L, Reason.DOLOR_SESION_SIN_IDENTIFICAR, 50L);
        var z = fila(150L, Reason.DOLOR_SESION_SIN_IDENTIFICAR, 50L);
        var otraSesion = fila(600L, Reason.DOLOR_SESION_SIN_IDENTIFICAR, 51L);
        semanasConPlan(S0);
        perfilSinConflictos();
        when(repositorio.bloquearActivas(U, 537L)).thenReturn(List.of(x));
        when(repositorio.bloquearActivas(U, 578L)).thenReturn(List.of(y));
        when(repositorio.bloquearActivas(U, 150L)).thenReturn(List.of(z));
        when(repositorio.bloquearActivas(U, 600L)).thenReturn(List.of(otraSesion));
        when(catalogo.fetchEquivalentVariants(any())).thenAnswer(inv -> {
            Set<Long> ids = inv.getArgument(0);
            var r = new java.util.HashMap<Long, List<ExerciseEquivalence>>();
            if (ids.contains(537L)) r.put(537L, List.of(variante(538L), gesto(150L)));
            return r;
        });
        // Un no relacionado de OTRA sesion, o el propio identificado: se rechaza sin cambiar nada.
        assertThrows(DomainRuleViolationException.class, () -> dolor.identificar(U, 537L, 50L, true, Set.of(600L)));
        assertThrows(DomainRuleViolationException.class, () -> dolor.identificar(U, 537L, 50L, true, Set.of(537L)));
        assertFalse(x.confirmada(), "nada cambio");
        // 578 y 150 no relacionados: 578 se levanta; 150 (equivalente de 537) se levanta como fila pero sigue fuera.
        // Si no se nombran, no se levanta nada (ni 600, de otra sesion).
        var r = dolor.identificar(U, 537L, 50L, true, Set.of(578L, 150L));
        assertTrue(x.confirmada());
        assertEquals(UserSafetyExclusion.Status.REACTIVADA, y.getStatus());
        assertTrue(y.getReactivationEvidence().startsWith("CONFIRMACION_EXPLICITA_NO_RELACIONADO"));
        assertEquals(List.of(578L), r.noRelacionadosLevantados());
        assertTrue(r.noRelacionadosNoLevantados().get(150L).contains("sigue fuera"), r.toString());
        assertEquals(UserSafetyExclusion.Status.ACTIVA, otraSesion.getStatus(), "otra sesion: sin confirmacion, nada");
    }

    @Test
    @DisplayName("Un no relacionado que choca con una restriccion vigente no se levanta, y se dice por que")
    void noRelacionadoConConflicto() {
        var x = fila(537L, Reason.DOLOR_SESION_SIN_IDENTIFICAR, 50L);
        var y = fila(578L, Reason.DOLOR_SESION_SIN_IDENTIFICAR, 50L);
        semanasConPlan(S0);
        perfilSinConflictos();
        when(conflictos.motivos(any(), any(), any())).thenReturn(Map.of(578L, "brazos por encima de la cabeza"));
        when(repositorio.bloquearActivas(U, 537L)).thenReturn(List.of(x));
        when(repositorio.bloquearActivas(U, 578L)).thenReturn(List.of(y));
        var r = dolor.identificar(U, 537L, 50L, true, Set.of(578L));
        assertEquals(UserSafetyExclusion.Status.ACTIVA, y.getStatus());
        assertTrue(r.noRelacionadosLevantados().isEmpty());
        assertTrue(r.noRelacionadosNoLevantados().get(578L).contains("restriccion"));
    }
}
