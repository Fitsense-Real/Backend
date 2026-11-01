package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.GenerationSource;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningPolicy;

import java.time.LocalDate;
import java.util.*;

/**
 * V44 · TRAZA DE LA POLITICA con la que se publica un plan (columna {@code planning_policy_trace}). Dominio puro.
 * <p>
 * Por sesion: segundos estimados (la formula de V17/V20), minutos declarados, % de M, si queda bajo el piso de V20,
 * si estaba exenta por V8C (reduccion autorizada) y si se publico por la EXCEPCION C2-D. Una sesion bajo el piso
 * que no esta exenta solo puede estar publicada por C2-D: el validador la habria rechazado con cualquier otra
 * version.
 */
public final class PlanningPolicyTrace {

    private PlanningPolicyTrace() {}

    public static Map<String, Object> de(PlanGenerationContext ctx, List<PlanDraft.DraftWorkout> sesiones,
                                         GenerationSource origen, String contexto, String versionCalculo,
                                         Set<LocalDate> exentasV8C, SessionDurationEstimator estimador) {
        var politica = ctx.policy();
        int m = ctx.effectiveSessionMinutes();
        long piso = PlanDraftValidator.minimumSessionSeconds(ctx);
        var t = new LinkedHashMap<String, Object>();
        t.put("politica", politica.id());
        t.put("version_calculo", versionCalculo);
        var params = new LinkedHashMap<String, Object>();
        params.put("c2d", politica.c2d());
        params.put("v4", politica.v4Segundos() ? "SEGUNDOS" : "HISTORICO");
        params.put("piso_v20_pct", ctx.prescription() == null ? null : ctx.prescription().floorPct());
        params.put("piso_v20_segundos", piso);
        params.put("tolerancia_v4_pct", PlanningPolicy.TOLERANCIA_V4_PCT);
        params.put("limite_v4", politica.v4Segundos()
                ? Map.of("segundos", PlanningPolicy.limiteV4Segundos(m))
                : Map.of("minutos_declarados", (int) Math.round(m * 1.15)));
        t.put("parametros", params);
        t.put("minutos_disponibles", m);
        t.put("origen", origen == null ? null : origen.name());
        t.put("contexto", contexto);
        var lista = new ArrayList<Map<String, Object>>();
        int excepciones = 0;
        for (var w : sesiones) {
            if (w == null) continue;
            long s = ctx.prescription() == null ? 0 : estimador.estimateSeconds(w, ctx.prescription());
            boolean bajo = piso > 0 && s < piso;
            boolean exenta = exentasV8C != null && exentasV8C.contains(w.scheduledDate());
            boolean c2d = bajo && !exenta && politica.c2d();
            if (c2d) excepciones++;
            var x = new LinkedHashMap<String, Object>();
            x.put("fecha", String.valueOf(w.scheduledDate()));
            x.put("segundos_estimados", s);
            x.put("minutos_declarados", w.expectedDurationMinutes());
            x.put("pct_de_m", m <= 0 ? null : Math.round(1000.0 * s / (m * 60.0)) / 10.0);
            x.put("bajo_piso_v20", bajo);
            x.put("exenta_v8c", exenta);
            x.put("excepcion_c2d", c2d);
            lista.add(x);
        }
        t.put("sesiones", lista);
        t.put("excepciones_c2d", excepciones);
        return t;
    }
}
