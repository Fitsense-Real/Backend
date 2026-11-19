package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment.TargetedSession;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutStatus;
import main.web.services.fitsense.shared.domain.model.valueobjects.ContextoFaltaDeTiempo;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Falta de tiempo: la intervencion depende de lo declarado (DISENO_ADAPTACION_CONTEXTUAL v0.3)")
class FaltaDeTiempoPolicyTest {

    static final TargetedSession ACORTAR = new TargetedSession(7L, ACORTAR_O_REORGANIZAR);

    private static TargetedSession afinar(TargetedSession s, WorkoutStatus st, SkipReason r, ContextoFaltaDeTiempo c,
                                          Integer m) {
        return FaltaDeTiempoPolicy.afinar(s, st, r, c, m, 33);
    }

    @Test
    @DisplayName("omision sin respuesta, imprevisto o sin tiempo ese dia: CONSERVAR (D1)")
    void conservar() {
        for (var c : new ContextoFaltaDeTiempo[]{null, ContextoFaltaDeTiempo.IMPREVISTO,
                ContextoFaltaDeTiempo.SIN_TIEMPO_ESE_DIA})
            assertEquals(CONSERVAR, afinar(ACORTAR, WorkoutStatus.SKIPPED, SkipReason.LACK_OF_TIME, c, null).intervencion());
    }

    @Test
    @DisplayName("menos tiempo habitual: si cabe, CONSERVAR; si no, acortar con M como tope")
    void menosTiempoHabitual() {
        assertEquals(CONSERVAR, afinar(ACORTAR, WorkoutStatus.SKIPPED, SkipReason.LACK_OF_TIME,
                ContextoFaltaDeTiempo.MENOS_TIEMPO_HABITUAL, 33).intervencion());
        assertEquals(CONSERVAR, afinar(ACORTAR, WorkoutStatus.SKIPPED, SkipReason.LACK_OF_TIME,
                ContextoFaltaDeTiempo.MENOS_TIEMPO_HABITUAL, 40).intervencion());
        var s = afinar(ACORTAR, WorkoutStatus.SKIPPED, SkipReason.LACK_OF_TIME, ContextoFaltaDeTiempo.MENOS_TIEMPO_HABITUAL, 30);
        assertEquals(ACORTAR_O_REORGANIZAR, s.intervencion());
        assertEquals(30, s.minutosDisponibles());
    }

    @Test
    @DisplayName("una sesion parcial por falta de tiempo y las demas causas no cambian")
    void sinCambios() {
        assertSame(ACORTAR, afinar(ACORTAR, WorkoutStatus.PARTIAL, SkipReason.LACK_OF_TIME, null, null));
        var fatiga = new TargetedSession(7L, REVISAR_CARGA_Y_RECUPERACION);
        assertSame(fatiga, afinar(fatiga, WorkoutStatus.SKIPPED, SkipReason.FATIGUE, null, null));
    }

    @Test
    @DisplayName("CONSERVAR y CAUSA_NO_REGISTRADA no se envian a la IA; los minutos solo viajan con un recorte")
    void permisos() {
        assertFalse(CONSERVAR.requiereIA());
        assertFalse(CAUSA_NO_REGISTRADA.requiereIA());
        for (var t : TipoIntervencion.values())
            if (t != CONSERVAR && t != CAUSA_NO_REGISTRADA) assertTrue(t.requiereIA(), t.name());
        assertNull(new TargetedSession(7L, CONSERVAR, 30).minutosDisponibles());
    }
}
