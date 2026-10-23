package main.web.services.fitsense.planning.application.internal.commandservices;

import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalCatalogService;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.ExclusionesDeSeguridad;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.SafetyProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * P2-D · que ejercicios HEREDADOS de la semana medida quedaron fuera del
 * conjunto elegible SOLO por seguridad, y por que restriccion.
 * <p>
 * Un heredado que no es elegible hoy puede estarlo por motivos muy distintos:
 * un bloqueo del participante, equipamiento que ya no declara, el filtro
 * funcional... o una restriccion de seguridad. Solo este ultimo caso autoriza
 * la sustitucion obligatoria de {@code SafetySubstitutionPolicy}; los demas
 * siguen terminando en V1, como siempre.
 * <p>
 * Criterio: el ejercicio estaria en el conjunto del perfil SIN restricciones de
 * seguridad -{@link ExternalCatalogService#fetchIgnoringSafety}-, con todo lo
 * demas igual. El motivo se identifica levantando una restriccion cada vez.
 * Solo consulta el catalogo si hay algun heredado fuera, y nunca cambia el
 * conjunto elegible.
 */
@Component
public class SafetyExclusionResolver {

    private static final Logger log = LoggerFactory.getLogger(SafetyExclusionResolver.class);

    private final ExternalCatalogService catalog;

    public SafetyExclusionResolver(ExternalCatalogService catalog) {
        this.catalog = catalog;
    }

    public ExclusionesDeSeguridad resolver(PlanGenerationContext context) {
        var previa = context.previousWeek();
        if (previa == null || previa.prescriptions() == null) return ExclusionesDeSeguridad.ninguna();

        var elegibles = context.availableExercises().stream().map(CandidateExercise::exerciseId)
                .collect(Collectors.toSet());
        var heredadosFuera = previa.prescriptions().stream()
                .map(p -> p.exerciseId()).filter(Objects::nonNull)
                .filter(id -> !elegibles.contains(id))
                .collect(Collectors.toCollection(TreeSet::new));
        if (heredadosFuera.isEmpty()) return ExclusionesDeSeguridad.ninguna();

        var perfil = context.profile();
        int dificultad = context.effectiveMaxDifficulty();
        var seguridad = context.safety() == null ? SafetyProfile.none() : context.safety();
        var sinSeguridad = catalog.fetchIgnoringSafety(perfil, dificultad).stream()
                .collect(Collectors.toMap(CandidateExercise::exerciseId, Function.identity(), (a, b) -> a));

        var originales = new LinkedHashMap<Long, CandidateExercise>();
        var motivos = new LinkedHashMap<Long, String>();
        Map<String, Set<Long>> relajados = null;
        for (var id : heredadosFuera) {
            var original = sinSeguridad.get(id);
            if (original == null) continue;   // fuera por otra causa: no es de seguridad
            if (relajados == null) relajados = relajados(context, seguridad);
            var razones = new ArrayList<String>();
            if (ExternalCatalogService.retenidoInc4C03(id)) razones.add("INC-4C-03");
            if (ExternalCatalogService.contenidoIncCat1374(id, perfil, seguridad)) razones.add("INC-CAT-1374");
            relajados.forEach((restriccion, ids) -> { if (ids.contains(id)) razones.add(restriccion); });
            originales.put(id, original);
            motivos.put(id, razones.isEmpty() ? "varias_restricciones_de_seguridad" : String.join("+", razones));
        }
        if (!originales.isEmpty())
            log.info("EXCLUSIONES_SEGURIDAD user={} heredados={} motivos={}", context.userId(),
                    originales.keySet(), motivos);
        return new ExclusionesDeSeguridad(originales, motivos);
    }

    /** Por cada restriccion activa, el conjunto con SOLO esa levantada. */
    private Map<String, Set<Long>> relajados(PlanGenerationContext context, SafetyProfile s) {
        boolean[] f = {s.excludeHighImpact(), s.excludeFloorWork(), s.excludeAxialLoad(), s.excludeKneeling(),
                s.excludeHanging(), s.excludeOverhead(), s.excludeInversion()};
        String[] nombres = {"alto_impacto", "suelo", "carga_axial", "rodillas", "suspension", "brazos_arriba",
                "inversion"};
        var mapa = new LinkedHashMap<String, Set<Long>>();
        for (int i = 0; i < f.length; i++) {
            if (!f[i]) continue;
            var g = f.clone();
            g[i] = false;
            mapa.put(nombres[i], catalog.fetchEligibleForExplanation(context.profile(),
                            context.effectiveMaxDifficulty(), new SafetyProfile(g[0], g[1], g[2], g[3], g[4], g[5], g[6]))
                    .stream().map(CandidateExercise::exerciseId).collect(Collectors.toSet()));
        }
        return mapa;
    }
}
