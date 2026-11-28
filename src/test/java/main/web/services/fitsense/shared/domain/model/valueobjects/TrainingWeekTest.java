package main.web.services.fitsense.shared.domain.model.valueobjects;

import main.web.services.fitsense.RelojPruebas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Calculo de semanas (lunes a domingo) con fechas explicitas: no depende del dia en que se ejecuta. */
@DisplayName("TrainingWeek: lunes a domingo, limites de mes y de ano, bisiestos y semana de referencia de las pruebas")
class TrainingWeekTest {

    @ParameterizedTest(name = "{0} -> semana {1}..{2}")
    @CsvSource({
            // cada dia de la semana de referencia
            "2026-11-02,2026-11-02,2026-11-08", "2026-11-04,2026-11-02,2026-11-08", "2026-11-08,2026-11-02,2026-11-08",
            // domingo anterior y lunes siguiente
            "2026-11-01,2026-10-26,2026-11-01", "2026-11-09,2026-11-09,2026-11-15",
            // cambio de mes dentro de la semana
            "2026-09-30,2026-09-28,2026-10-04", "2026-10-02,2026-09-28,2026-10-04",
            // cambio de ano: 2026-12-31 es jueves, 2027-01-03 domingo, 2027-01-04 lunes
            "2026-12-31,2026-12-28,2027-01-03", "2027-01-01,2026-12-28,2027-01-03", "2027-01-03,2026-12-28,2027-01-03",
            "2027-01-04,2027-01-04,2027-01-10",
            // ano que empieza en lunes (2029-01-01) y fin de ano en domingo (2028-12-31)
            "2028-12-31,2028-12-25,2028-12-31", "2029-01-01,2029-01-01,2029-01-07",
            // bisiestos: 2028-02-29 es martes; 2024-02-29 jueves
            "2028-02-29,2028-02-28,2028-03-05", "2028-03-01,2028-02-28,2028-03-05", "2024-02-29,2024-02-26,2024-03-03"})
    void containing(LocalDate dia, LocalDate lunes, LocalDate domingo) {
        var semana = TrainingWeek.containing(dia);
        assertEquals(lunes, semana.startDate());
        assertEquals(domingo, semana.endDate());
        assertEquals(DayOfWeek.MONDAY, semana.startDate().getDayOfWeek());
        assertEquals(DayOfWeek.SUNDAY, semana.endDate().getDayOfWeek());
        assertTrue(semana.covers(dia));
        assertFalse(semana.covers(lunes.minusDays(1)));
        assertFalse(semana.covers(domingo.plusDays(1)));
        assertEquals(lunes, TrainingWeek.mondayOf(dia));
    }

    @Test
    @DisplayName("previous y next atraviesan el cambio de ano sin perder el lunes")
    void anteriorYSiguiente() {
        var fin = TrainingWeek.containing(LocalDate.of(2026, 12, 30));
        assertEquals(new TrainingWeek(LocalDate.of(2027, 1, 4), LocalDate.of(2027, 1, 10)), fin.next());
        assertEquals(new TrainingWeek(LocalDate.of(2026, 12, 21), LocalDate.of(2026, 12, 27)), fin.previous());
        assertEquals(fin, fin.next().previous());
        var w = fin;
        for (int i = 0; i < 60; i++) {
            w = w.next();
            assertEquals(DayOfWeek.MONDAY, w.startDate().getDayOfWeek());
            assertEquals(w.startDate().plusDays(6), w.endDate());
        }
    }

    @Test
    @DisplayName("la semana de referencia de las pruebas es la que antes daba la formula relativa con hoy = 2026-11-01")
    void semanaDeReferencia() {
        assertEquals(LocalDate.of(2026, 11, 2), RelojPruebas.SEMANA_REFERENCIA);
        assertEquals(DayOfWeek.MONDAY, RelojPruebas.SEMANA_REFERENCIA.getDayOfWeek());
        // La formula que usaban las pruebas: TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1),
        // evaluada el ultimo dia en que sus referencias pasaban (domingo 2026-11-01) y cualquier dia de esa semana.
        for (var hoy = LocalDate.of(2026, 10, 26); !hoy.isAfter(LocalDate.of(2026, 11, 1)); hoy = hoy.plusDays(1))
            assertEquals(RelojPruebas.SEMANA_REFERENCIA, TrainingWeek.containing(hoy).startDate().plusWeeks(1), hoy.toString());
        // A partir del lunes 2026-11-02 la formula relativa ya daba otra semana: el origen del fallo.
        assertNotEquals(RelojPruebas.SEMANA_REFERENCIA,
                TrainingWeek.containing(LocalDate.of(2026, 11, 2)).startDate().plusWeeks(1));
    }
}
