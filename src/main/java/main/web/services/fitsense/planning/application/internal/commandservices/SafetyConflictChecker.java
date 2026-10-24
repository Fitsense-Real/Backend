package main.web.services.fitsense.planning.application.internal.commandservices;

import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalCatalogService;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningProfile;
import main.web.services.fitsense.planning.domain.model.valueobjects.SafetyProfile;
import main.web.services.fitsense.planning.domain.services.SafetyConflictPolicy;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Que ejercicios, de los pedidos, chocan HOY con la seguridad del
 * participante, y con que causa. Lee las banderas del catalogo y aplica
 * {@link SafetyConflictPolicy}; no consulta el conjunto elegible.
 */
@Component
public class SafetyConflictChecker {

    private final ExternalCatalogService catalog;

    public SafetyConflictChecker(ExternalCatalogService catalog) {
        this.catalog = catalog;
    }

    /** Por ejercicio en conflicto, su motivo. Los que no chocan no aparecen. */
    public Map<Long, String> motivos(Collection<Long> exerciseIds, PlanningProfile perfil, SafetyProfile seguridad) {
        if (exerciseIds == null || exerciseIds.isEmpty()) return Map.of();
        var ids = new TreeSet<Long>();
        exerciseIds.stream().filter(Objects::nonNull).forEach(ids::add);
        var s = seguridad == null ? SafetyProfile.none() : seguridad;
        var hechos = catalog.fetchSafetyFacts(ids);
        var motivos = new LinkedHashMap<Long, String>();
        for (var id : ids) {
            var causas = SafetyConflictPolicy.causas(hechos.get(id), s,
                    ExternalCatalogService.retenidoInc4C03(id),
                    ExternalCatalogService.contenidoIncCat1374(id, perfil, s));
            if (!causas.isEmpty()) motivos.put(id, SafetyConflictPolicy.motivo(causas));
        }
        return motivos;
    }
}
