package main.web.services.fitsense.planning.application.internal.outboundservices.acl;

import main.web.services.fitsense.catalog.domain.services.ExerciseQueryService;
import main.web.services.fitsense.catalog.infrastructure.persistence.jpa.repositories.BodyPartRepository;
import main.web.services.fitsense.catalog.infrastructure.persistence.jpa.repositories.EquipmentTypeRepository;
import main.web.services.fitsense.catalog.interfaces.acl.CatalogContextFacade;
import main.web.services.fitsense.catalog.interfaces.acl.EligibleExerciseView;
import main.web.services.fitsense.planning.domain.model.valueobjects.ModoApoyoSuspendido;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningProfile;
import main.web.services.fitsense.planning.domain.model.valueobjects.SafetyProfile;
import main.web.services.fitsense.profiling.domain.model.valueobjects.MovementAllowances;
import main.web.services.fitsense.profiling.interfaces.acl.ProfileSnapshot;
import main.web.services.fitsense.profiling.interfaces.acl.ProfilingContextFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * V53-B · las dos capas anticorrupcion con el interruptor nuevo (propuesta, apagado por defecto).
 * <ul>
 *   <li>Apagado: planning llama a la fachada del catalogo con la firma de once argumentos, exactamente como antes, y
 *       el permiso propio del perfil no se traduce.</li>
 *   <li>Opcion B: la exclusion del apoyo suspendido viaja explicita (sobrecarga de doce) y el permiso propio llega a
 *       {@code Declaradas}.</li>
 *   <li>Opcion A y B a la vez: el arranque falla.</li>
 * </ul>
 */
@DisplayName("V53-B · ACL de catalogo y perfil con el permiso «apoyo suspendido»")
class PermisoApoyoSuspendidoAclTest {

    private static EligibleExerciseView vista(long id) {
        return new EligibleExerciseView(id, "ejercicio " + id, "chest", "parallel bars", "pectorals",
                (short) 1, "SETS_REPS", null, null, null, "HORIZONTAL_PUSH");
    }

    private static PlanningProfile perfil() {
        return new PlanningProfile(9L, 41, "MALE", new BigDecimal("180"), new BigDecimal("100"), null,
                "ADVANCED", "GENERAL_FITNESS", "x", "Ninguna", "GYM", 4,
                List.of((short) 1, (short) 4, (short) 5, (short) 7), 60, List.of("body weight", "parallel bars"),
                List.of(), SafetyProfile.Declaradas.sinLimitaciones());
    }

    @Test
    @DisplayName("apagado: firma de once argumentos, la de doce no se usa")
    void apagadoUsaLaFirmaDeSiempre() {
        var fachada = Mockito.mock(CatalogContextFacade.class);
        Mockito.when(fachada.fetchEligibleExercises(anyList(), anyBoolean(), anyInt(), anyList(),
                        anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
                        anyBoolean(), anyBoolean(), anyBoolean()))
                .thenReturn(List.of(vista(457)));
        var seguridad = new SafetyProfile(false, false, false, false, true, false, false, false);
        new ExternalCatalogService(fachada).fetchEligibleFor(perfil(), 3, seguridad);
        Mockito.verify(fachada).fetchEligibleExercises(anyList(), anyBoolean(), anyInt(), anyList(),
                anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), eq(true), anyBoolean(), anyBoolean());
        Mockito.verify(fachada, Mockito.never()).fetchEligibleExercises(anyList(), anyBoolean(), anyInt(), anyList(),
                anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
                anyBoolean());
    }

    @Test
    @DisplayName("opcion B: la exclusion del apoyo suspendido viaja explicita, en los dos sentidos")
    void opcionBUsaLaSobrecarga() {
        for (boolean excluye : new boolean[]{true, false}) {
            var fachada = Mockito.mock(CatalogContextFacade.class);
            Mockito.when(fachada.permisoApoyoSuspendidoActivo()).thenReturn(true);
            Mockito.when(fachada.fetchEligibleExercises(anyList(), anyBoolean(), anyInt(), anyList(),
                            anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
                            anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean()))
                    .thenReturn(List.of(vista(457)));
            // «no colgarse» con el permiso propio contrario: el valor que viaja es el del permiso.
            var seguridad = new SafetyProfile(false, false, false, false, !excluye, false, false, excluye);
            new ExternalCatalogService(fachada).fetchEligibleForExplanation(perfil(), 3, seguridad);
            Mockito.verify(fachada).fetchEligibleExercises(anyList(), anyBoolean(), anyInt(), anyList(),
                    anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), eq(!excluye), anyBoolean(), anyBoolean(),
                    eq(excluye));
            Mockito.verify(fachada, Mockito.never()).fetchEligibleExercises(anyList(), anyBoolean(), anyInt(),
                    anyList(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
                    anyBoolean());
        }
    }

    private static ProfileSnapshot snapshot(boolean colgarse, Boolean apoyo) {
        return new ProfileSnapshot(5L, 30, "MALE", new BigDecimal("180"), new BigDecimal("80"), null, "ADVANCED",
                "GENERAL_FITNESS", "x", "GYM", (short) 3, List.of((short) 1, (short) 3, (short) 5), (short) 45, 51,
                3, List.of("parallel bars"), List.of(), null,
                new MovementAllowances(true, true, true, colgarse, true), apoyo);
    }

    private static SafetyProfile.Declaradas traducidas(ExternalProfilingService acl) {
        return acl.fetchProfile(5L).orElseThrow().declaradas();
    }

    @Test
    @DisplayName("perfil: el permiso propio solo se traduce con la opcion B")
    void elPermisoSoloSeTraduceConB() {
        for (Boolean apoyo : new Boolean[]{null, true, false}) {
            var perfiles = Mockito.mock(ProfilingContextFacade.class);
            Mockito.when(perfiles.fetchProfile(5L)).thenReturn(Optional.of(snapshot(false, apoyo)));
            var apagado = traducidas(new ExternalProfilingService(perfiles));
            var a = traducidas(new ExternalProfilingService(perfiles, true, false));
            var b = traducidas(new ExternalProfilingService(perfiles, false, true));
            assertNull(apagado.allowSuspendedSupport());
            assertNull(a.allowSuspendedSupport());
            assertEquals(apoyo, b.allowSuspendedSupport());
            assertEquals(new SafetyProfile.Declaradas(true, true, true, false, true), apagado);
            assertEquals(apagado, a);
            assertEquals(apagado, b.sinPermisoApoyoSuspendido());
        }
        var perfiles = Mockito.mock(ProfilingContextFacade.class);
        assertEquals(ModoApoyoSuspendido.APAGADO, new ExternalProfilingService(perfiles).modoApoyoSuspendido());
        assertEquals(ModoApoyoSuspendido.NO_COLGARSE,
                new ExternalProfilingService(perfiles, true, false).modoApoyoSuspendido());
        assertEquals(ModoApoyoSuspendido.PERMISO_PROPIO,
                new ExternalProfilingService(perfiles, false, true).modoApoyoSuspendido());
    }

    @Test
    @DisplayName("los dos interruptores: ni la fachada ni el ACL de perfil se construyen")
    void dosInterruptoresNoSeConstruyen() {
        var e1 = assertThrows(IllegalStateException.class, () -> new ExternalProfilingService(
                Mockito.mock(ProfilingContextFacade.class), true, true));
        var e2 = assertThrows(IllegalStateException.class, () -> new CatalogContextFacade(
                Mockito.mock(ExerciseQueryService.class), Mockito.mock(BodyPartRepository.class),
                Mockito.mock(EquipmentTypeRepository.class), true, true));
        for (var e : List.of(e1, e2)) {
            assertTrue(e.getMessage().contains("fitsense.seguridad.apoyo-suspendido-en-no-colgarse"), e.getMessage());
            assertTrue(e.getMessage().contains("fitsense.seguridad.permiso-apoyo-suspendido"), e.getMessage());
        }
        assertFalse(new CatalogContextFacade(Mockito.mock(ExerciseQueryService.class),
                Mockito.mock(BodyPartRepository.class), Mockito.mock(EquipmentTypeRepository.class), true)
                .permisoApoyoSuspendidoActivo(), "el constructor de compatibilidad deja la opcion B apagada");
    }

    private static ApplicationContextRunner contexto() {
        return new ApplicationContextRunner()
                .withBean(ProfilingContextFacade.class, () -> Mockito.mock(ProfilingContextFacade.class))
                .withBean(ExerciseQueryService.class, () -> Mockito.mock(ExerciseQueryService.class))
                .withBean(BodyPartRepository.class, () -> Mockito.mock(BodyPartRepository.class))
                .withBean(EquipmentTypeRepository.class, () -> Mockito.mock(EquipmentTypeRepository.class))
                .withBean(ExternalProfilingService.class)
                .withBean(CatalogContextFacade.class);
    }

    @Test
    @DisplayName("arranque: por defecto apagado; con A y B encendidas el contexto no arranca")
    void arranque() {
        contexto().run(ctx -> {
            assertNull(ctx.getStartupFailure());
            assertEquals(ModoApoyoSuspendido.APAGADO, ctx.getBean(ExternalProfilingService.class).modoApoyoSuspendido());
            assertFalse(ctx.getBean(CatalogContextFacade.class).permisoApoyoSuspendidoActivo());
        });
        contexto().withPropertyValues("fitsense.seguridad.permiso-apoyo-suspendido=true").run(ctx -> {
            assertNull(ctx.getStartupFailure());
            assertEquals(ModoApoyoSuspendido.PERMISO_PROPIO,
                    ctx.getBean(ExternalProfilingService.class).modoApoyoSuspendido());
            assertTrue(ctx.getBean(CatalogContextFacade.class).permisoApoyoSuspendidoActivo());
        });
        contexto().withPropertyValues("fitsense.seguridad.permiso-apoyo-suspendido=true",
                "fitsense.seguridad.apoyo-suspendido-en-no-colgarse=true").run(ctx -> {
            var fallo = ctx.getStartupFailure();
            assertNotNull(fallo, "con las dos opciones encendidas no debe arrancar");
            var causa = fallo;
            while (causa.getCause() != null && !(causa instanceof IllegalStateException
                    && causa.getMessage() != null && causa.getMessage().contains("ambigua"))) causa = causa.getCause();
            assertTrue(causa.getMessage().contains("ambigua"), causa.toString());
        });
    }
}
