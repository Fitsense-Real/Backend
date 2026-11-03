package main.web.services.fitsense.planning.application.internal.commandservices;

import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalCatalogService;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.ExclusionesDeSeguridad;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * P2-D · que ejercicios HEREDADOS de la semana medida dejaron de ser
 * compatibles por SEGURIDAD, y con que causa.
 * <p>
 * Un heredado que no es elegible hoy puede estarlo por motivos muy distintos:
 * un bloqueo del participante, equipamiento que ya no declara, el filtro
 * funcional... o una restriccion de seguridad. Solo este ultimo caso autoriza
 * la sustitucion obligatoria de {@code SafetySubstitutionPolicy}; los demas
 * siguen terminando en V1, como siempre.
 * <p>
 * CRITERIO: no basta con que el id falte del conjunto elegible. Hace falta una
 * causa VERIFICADA por {@link SafetyConflictChecker}: una bandera del catalogo
 * que choca con una restriccion actual del perfil, la retencion INC-4C-03 o la
 * contencion INC-CAT-1374. Sin causa, no hay exclusion de seguridad.
 */
@Component
public class SafetyExclusionResolver {

    private static final Logger log = LoggerFactory.getLogger(SafetyExclusionResolver.class);

    private final ExternalCatalogService catalog;
    private final SafetyConflictChecker conflictos;

    /** P-02. Null en las pruebas que construyen el resolver a mano: entonces, lo de siempre. */
    private final PainMemoryService painMemory;

    public SafetyExclusionResolver(ExternalCatalogService catalog, SafetyConflictChecker conflictos) {
        this(catalog, conflictos, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public SafetyExclusionResolver(ExternalCatalogService catalog, SafetyConflictChecker conflictos,
                                   PainMemoryService painMemory) {
        this.catalog = catalog;
        this.conflictos = conflictos;
        this.painMemory = painMemory;
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

        var motivos = new LinkedHashMap<>(conflictos.motivos(heredadosFuera, context.profile(), context.safety()));
        // P-02: un heredado que dolio y sigue excluido tiene una causa verificada (el evento registrado).
        // Con ella entra en la sustitucion obligatoria SUST-SEG-1 y en su excepcion de V8C; sin ella, V1.
        if (painMemory != null) {
            var dolor = painMemory.excluidosActivos(context.userId());
            for (var id : heredadosFuera)
                if (dolor.contains(id))
                    motivos.merge(id, main.web.services.fitsense.planning.domain.services.SafetyConflictPolicy
                            .DOLOR_REGISTRADO, (a, b) -> a + "+" + b);
        }
        if (motivos.isEmpty()) return ExclusionesDeSeguridad.ninguna();

        var descritos = catalog.fetchAsCandidates(motivos.keySet());
        var originales = new LinkedHashMap<Long, CandidateExercise>();
        var conMotivo = new LinkedHashMap<Long, String>();
        motivos.forEach((id, motivo) -> {
            var original = descritos.get(id);
            if (original == null) return;   // sin descripcion no se le puede buscar sustituto
            originales.put(id, original);
            conMotivo.put(id, motivo);
        });
        if (!originales.isEmpty())
            log.info("EXCLUSIONES_SEGURIDAD user={} heredados={} motivos={}", context.userId(),
                    originales.keySet(), conMotivo);
        return new ExclusionesDeSeguridad(originales, conMotivo);
    }
}
