package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.AdjustmentType;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.ConservationReport;
import main.web.services.fitsense.planning.domain.model.valueobjects.DerivedWorkout;
import main.web.services.fitsense.planning.domain.model.valueobjects.ExclusionesDeSeguridad;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.PreviousWeekSummary;
import main.web.services.fitsense.planning.domain.model.valueobjects.WeekDerivation;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.planning.domain.services.ConservationValidator.ReglaPatron;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * H1 · IMPACTO HISTORICO (opt-in, solo lectura de la evidencia): cada sesion cambiada y publicada de las corridas
 * conservadas (entrada_sesiones.csv, generada por preparar_entrada.py) se vuelve a validar con el V8C REAL con la
 * regla historica (CONJUNTO) y con la candidata (MULTICONJUNTO). Escribe salida_sesiones.csv al lado de la entrada.
 * <p>
 * {@code mvnw test -Dtest=H1ImpactoHistoricoTest -Dh1.impacto.dir=docs/FitSense/adaptacion/validacion_h1_h12/evidencia/h1_impacto}
 */
@DisplayName("H1 · impacto historico: V8C real, conjunto frente a multiconjunto, sobre las sesiones publicadas")
class H1ImpactoHistoricoTest {

    static final Pattern EJ = Pattern.compile("(\\d+):(\\w+):(\\d+)x(\\d+)(s?):r(\\d+)");
    static final LocalDate L1 = LocalDate.of(2026, 11, 2);
    static final long SES = 7L;

    @Test
    void reevaluar() throws IOException {
        String dir = System.getProperty("h1.impacto.dir");
        assumeTrue(dir != null && !dir.isBlank(), "opt-in: -Dh1.impacto.dir=<carpeta>");
        Path base = Path.of(dir);
        var cat = catalogo(Path.of("docs/FitSense/short_paper_ieee/evaluacion_calidad_adaptacion_v2/evidencia/"
                + "catalogo_exercises_v44_2.csv"));
        var lineas = Files.readAllLines(base.resolve("entrada_sesiones.csv"), StandardCharsets.UTF_8);
        var cab = csv(lineas.get(0));
        var out = new StringBuilder("grupo,corrida,semana,usuario,objetivo,sesion,motivo,intervencion,retirados,anadidos,"
                + "aceptada_conjunto,aplicada_conjunto,aceptada_multiconjunto,aplicada_multiconjunto,cambia,"
                + "problemas_conjunto,problemas_multiconjunto\n");
        int cambian = 0;
        for (var linea : lineas.subList(1, lineas.size())) {
            if (linea.isBlank()) continue;
            var v = csv(linea);
            Map<String, String> f = new HashMap<>();
            for (int i = 0; i < cab.size(); i++) f.put(cab.get(i), v.get(i));
            var antes = ejercicios(f.get("antes"));
            var despues = ejercicios(f.get("despues"));
            var t = TipoIntervencion.valueOf(f.get("intervencion"));
            var rc = validar(new ConservationValidator(ReglaPatron.CONJUNTO), t, f.get("motivo"), antes, despues, cat);
            var rm = validar(new ConservationValidator(ReglaPatron.MULTICONJUNTO), t, f.get("motivo"), antes, despues,
                    cat);
            var idsA = antes.stream().map(PlanDraft.DraftExercise::exerciseId).toList();
            var idsD = despues.stream().map(PlanDraft.DraftExercise::exerciseId).toList();
            var ret = idsA.stream().filter(x -> !idsD.contains(x)).map(String::valueOf).toList();
            var ana = idsD.stream().filter(x -> !idsA.contains(x)).map(String::valueOf).toList();
            boolean cambia = rc.aceptable() != rm.aceptable() || !rc.aplicadas().equals(rm.aplicadas());
            if (cambia) cambian++;
            out.append(String.join(",", q(f.get("grupo")), q(f.get("corrida")), f.get("semana"), f.get("usuario"),
                    f.get("objetivo"), f.get("sesion"), f.get("motivo"), f.get("intervencion"), q(String.join(" ", ret)),
                    q(String.join(" ", ana)), String.valueOf(rc.aceptable()), String.valueOf(!rc.aplicadas().isEmpty()),
                    String.valueOf(rm.aceptable()), String.valueOf(!rm.aplicadas().isEmpty()), String.valueOf(cambia),
                    q(String.join(" | ", rc.problems())), q(String.join(" | ", rm.problems())))).append('\n');
        }
        Files.writeString(base.resolve("salida_sesiones.csv"), out.toString(), StandardCharsets.UTF_8);
        System.out.println("H1_IMPACTO sesiones=" + (lineas.size() - 1) + " cambian=" + cambian);
    }

    static ConservationReport validar(ConservationValidator v8c, TipoIntervencion t, String motivo,
                                      List<PlanDraft.DraftExercise> antes, List<PlanDraft.DraftExercise> despues,
                                      Map<Long, CandidateExercise> cat) {
        var previas = new ArrayList<PreviousWeekSummary.PrescriptionOutcome>();
        for (var e : antes)
            previas.add(new PreviousWeekSummary.PrescriptionOutcome(SES, L1, e.exerciseId(), "e" + e.exerciseId(),
                    e.prescriptionType().name(), e.plannedSets(), e.plannedReps(), e.plannedDurationSeconds(), null,
                    null, null, null, null, BigDecimal.ZERO, "SKIPPED", motivo, "SKIPPED", e.restSeconds()));
        var semana = new PreviousWeekSummary(new BigDecimal("33.33"), null, 0, Map.of(), List.of(
                new PreviousWeekSummary.WorkoutOutcome(SES, L1, "FULL_BODY", "SKIPPED", motivo, true, "SKIPPED", null,
                        BigDecimal.ZERO, "S", (short) 45)), previas, List.of());
        var der = new WeekDerivation(List.of(new DerivedWorkout(SES,
                new PlanDraft.DraftWorkout(L1.plusWeeks(1), WorkoutFocus.FULL_BODY, "S", 45, despues))), List.of());
        var orden = new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null, 0.0, 0.0, null, null, null,
                "causa", null, null, List.of(new PlanAdjustment.TargetedSession(SES, t)));
        return v8c.validate(der, semana, orden, cat, ExclusionesDeSeguridad.ninguna(), List.of(), false, 30);
    }

    static List<PlanDraft.DraftExercise> ejercicios(String firma) {
        var l = new ArrayList<PlanDraft.DraftExercise>();
        var m = EJ.matcher(firma);
        while (m.find()) {
            var tipo = PrescriptionType.valueOf(m.group(2));
            short series = Short.parseShort(m.group(3));
            int v = Integer.parseInt(m.group(4));
            l.add(new PlanDraft.DraftExercise(Long.parseLong(m.group(1)), tipo, series,
                    tipo == PrescriptionType.DURATION ? null : (short) v, tipo == PrescriptionType.DURATION ? v : null,
                    null, Short.parseShort(m.group(6)), null));
        }
        assertFalse(l.isEmpty(), firma);
        return l;
    }

    static Map<Long, CandidateExercise> catalogo(Path p) throws IOException {
        var lineas = Files.readAllLines(p, StandardCharsets.UTF_8);
        var cab = csv(lineas.get(0));
        var m = new LinkedHashMap<Long, CandidateExercise>();
        for (var linea : lineas.subList(1, lineas.size())) {
            var v = csv(linea);
            Map<String, String> f = new HashMap<>();
            for (int i = 0; i < cab.size(); i++) f.put(cab.get(i), v.get(i));
            long id = Long.parseLong(f.get("exercise_id"));
            m.put(id, new CandidateExercise(id, f.get("name_en"), f.get("bp"), f.get("eq"),
                    Integer.parseInt(f.get("difficulty_level")), PrescriptionType.valueOf(f.get("default_prescription")),
                    f.get("target_muscle"), f.get("movement_pattern")));
        }
        return m;
    }

    static List<String> csv(String linea) {
        var r = new ArrayList<String>();
        var b = new StringBuilder();
        boolean comillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == '"') {
                if (comillas && i + 1 < linea.length() && linea.charAt(i + 1) == '"') { b.append('"'); i++; }
                else comillas = !comillas;
            } else if (c == ',' && !comillas) { r.add(b.toString()); b.setLength(0); }
            else b.append(c);
        }
        r.add(b.toString());
        return r;
    }

    static String q(String s) {
        return "\"" + (s == null ? "" : s.replace("\"", "\"\"")) + "\"";
    }
}
