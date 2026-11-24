package main.web.services.fitsense.planning.application.internal.commandservices;

import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalCatalogService;
import main.web.services.fitsense.planning.domain.model.aggregates.UserSafetyExclusion;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.UserSafetyExclusionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** O20 opcion A: la exclusion efectiva por dolor es la registrada mas su familia equivalente (sin base de datos). */
@DisplayName("O20 opcion A: PainMemoryService.excluidosActivos con la familia de movimiento equivalente")
class PainMemoryFamiliaTest {

    final UserSafetyExclusionRepository repositorio = mock(UserSafetyExclusionRepository.class);
    final ExternalCatalogService catalogo = mock(ExternalCatalogService.class);
    final PainMemoryService dolor = new PainMemoryService(repositorio, null, null, null, catalogo, null);

    private void activas(long... ejercicios) {
        var filas = new java.util.ArrayList<UserSafetyExclusion>();
        for (long e : ejercicios) {
            var x = mock(UserSafetyExclusion.class);
            when(x.getExerciseId()).thenReturn(e);
            filas.add(x);
        }
        when(repositorio.findByUserIdAndStatusOrderByIdAsc(7L, UserSafetyExclusion.Status.ACTIVA)).thenReturn(filas);
    }

    @Test
    @DisplayName("Dolor en 537: la exclusion efectiva incluye 538 (v. 2) y el resto de su familia")
    void incluyeLaFamilia() {
        activas(537L);
        when(catalogo.fetchEquivalentVariants(Set.of(537L))).thenReturn(Map.of(537L, Set.of(538L, 28L, 707L)));
        assertEquals(Set.of(28L, 537L, 538L, 707L), dolor.excluidosActivos(7L));
        assertEquals(Set.of(537L), dolor.excluidosActivosDirectos(7L), "solo 537 tiene fila: la familia es derivada");
    }

    @Test
    @DisplayName("Sin exclusiones no se consulta el catalogo")
    void sinExclusiones() {
        activas();
        assertEquals(Set.of(), dolor.excluidosActivos(7L));
        verify(catalogo, never()).fetchEquivalentVariants(any());
    }

    @Test
    @DisplayName("Si el catalogo falla, la exclusion registrada se mantiene (falla cerrado sobre lo directo)")
    void fallaCerrado() {
        activas(537L, 900L);
        when(catalogo.fetchEquivalentVariants(any())).thenThrow(new IllegalStateException("catalogo caido"));
        assertEquals(Set.of(537L, 900L), dolor.excluidosActivos(7L));
    }

    @Test
    @DisplayName("Reactivado lo que dolio (ninguna fila activa), su familia deja de estar excluida")
    void reactivadoNoExcluyeLaFamilia() {
        activas(900L);
        when(catalogo.fetchEquivalentVariants(Set.of(900L))).thenReturn(Map.of());
        assertEquals(Set.of(900L), dolor.excluidosActivos(7L), "537 ya no tiene fila activa: ni 537 ni 538 salen");
    }
}
