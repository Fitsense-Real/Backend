package main.web.services.fitsense.adaptation.domain.services;

import main.web.services.fitsense.adaptation.domain.model.valueobjects.AdaptationOrder;
import main.web.services.fitsense.adaptation.domain.model.valueobjects.AdaptationOutcome;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/** P-T2/P-T3: message_shown expresa la INTENCION de la decision, nunca un resultado que aun no ocurrio. */
@DisplayName("Mensaje de la intervencion: intencion, sin prometer resultados")
class MensajeDeIntervencionTest {

    static final LocalDate L = LocalDate.of(2026, 11, 2);

    /** Palabras de RESULTADO que un mensaje escrito antes de adaptar no puede afirmar. */
    static final List<String> PROMESAS = List.of("bajamos", "acortamos", "subimos", "reducimos", "cambiamos",
            "más fácil", "mas facil", "sencill", "exigencia");

    static AdaptationOrder.TargetedSession s(long id, TipoIntervencion t) {
        return new AdaptationOrder.TargetedSession(id, L, BigDecimal.ZERO, null, null, t);
    }

    static AdaptationOrder adapta(AdaptationOutcome o, String observada, AdaptationOrder.TargetedSession... ss) {
        return new AdaptationOrder(o, List.of(ss), new BigDecimal(observada), new BigDecimal("100.00"), 3, 3,
                "Adherencia observada " + observada + " % (cobertura 100.00 %). Entre 50 % y 80 %: se corrige solo la"
                        + " peor sesion.");
    }

    static void sinPromesas(String t) {
        var l = t.toLowerCase(Locale.ROOT);
        for (var p : PROMESAS) assertFalse(l.contains(p), () -> "promete un resultado (" + p + "): " + t);
        assertTrue(t.length() <= MensajeDeIntervencion.MAXIMO, t);
    }

    @Test
    @DisplayName("TOO_DIFFICULT: el texto propuesto por el responsable, en intencion")
    void dificil() {
        var t = MensajeDeIntervencion.intencion(adapta(AdaptationOutcome.ADAPT_LOCALIZED, "66.67",
                s(1, TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA)));
        assertEquals("La semana pasada cumpliste el 67 % de lo previsto. Nos dijiste que una sesión te resultó difícil."
                + " Revisaremos qué ajustes son compatibles con tu entrenamiento. Si ningún cambio es compatible con tu"
                + " entrenamiento, la mantendremos como está.", t);
        sinPromesas(t);
    }

    @Test
    @DisplayName("Dolor fuera de la adherencia: seguridad + consulta profesional")
    void dolor() {
        var t = MensajeDeIntervencion.intencion(adapta(AdaptationOutcome.ADAPT_LOCALIZED, "100.00",
                s(1, TipoIntervencion.SEGURIDAD_ANTE_DOLOR)));
        assertTrue(t.contains("Reportaste molestias. Revisaremos por seguridad los ejercicios de esa sesión. Si el dolor"
                + " continúa, consulta a un profesional de la salud."), t);
        sinPromesas(t);
    }

    @Test
    @DisplayName("Ampliada con varios motivos: cuantas sesiones y una frase por motivo, sin repetir")
    void ampliada() {
        var t = MensajeDeIntervencion.intencion(adapta(AdaptationOutcome.ADAPT_EXPANDED, "33.33",
                s(1, TipoIntervencion.ACORTAR_O_REORGANIZAR), s(2, TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION),
                s(3, TipoIntervencion.ACORTAR_O_REORGANIZAR)));
        assertTrue(t.startsWith("La semana pasada cumpliste el 33 % de lo previsto. Revisaremos las 3 sesiones que nos"
                + " indicaste."), t);
        assertEquals(1, t.split("te faltó tiempo", -1).length - 1, t);
        assertTrue(t.contains("Revisaremos si esas sesiones pueden acortarse sin cambiar tus días."), t);
        sinPromesas(t);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TipoIntervencion.class)
    @DisplayName("Ningun tipo de intervencion promete un resultado y todos caben en message_shown")
    void ningunTipoPromete(TipoIntervencion t) {
        sinPromesas(MensajeDeIntervencion.intencion(adapta(AdaptationOutcome.ADAPT_LOCALIZED, "50.00", s(1, t))));
        var muchas = Arrays.stream(TipoIntervencion.values()).map(x -> s(x.ordinal() + 1, x))
                .toArray(AdaptationOrder.TargetedSession[]::new);
        sinPromesas(MensajeDeIntervencion.intencion(adapta(AdaptationOutcome.ADAPT_EXPANDED, "10.00", muchas)));
    }

    @Test
    @DisplayName("Adherencia buena: se mantiene el plan")
    void sinCambio() {
        var o = AdaptationOrder.sinCambios(AdaptationOutcome.NO_CHANGE, new BigDecimal("100.00"),
                new BigDecimal("100.00"), 4, 4, "Adherencia observada 100.00 % sobre un umbral de 80 %: se conserva.");
        assertEquals("La semana pasada cumpliste el 100 % de lo previsto. Es un buen cumplimiento: mantenemos tu plan.",
                MensajeDeIntervencion.intencion(o));
    }

    @Test
    @DisplayName("Registro insuficiente: no muestra un porcentaje que no es fiable y pide registrar")
    void datosInsuficientes() {
        var o = AdaptationOrder.sinCambios(AdaptationOutcome.INSUFFICIENT_DATA, new BigDecimal("50.00"),
                new BigDecimal("25.00"), 1, 4, "Cobertura de registro insuficiente: 1 de 4 sesiones con registro, "
                        + "minimo exigido 50 %.");
        var t = MensajeDeIntervencion.intencion(o);
        assertEquals("No tenemos registros suficientes para ajustar tu plan, así que lo mantenemos. Registra tus sesiones"
                + " para que podamos adaptarlo.", t);
        assertFalse(t.contains("%"), t);
    }

    @Test
    @DisplayName("Baja adherencia sin sesion senalada: se conserva sin adaptar a ciegas")
    void sinSenalada() {
        var o = AdaptationOrder.sinCambios(AdaptationOutcome.INSUFFICIENT_DATA, new BigDecimal("60.00"),
                new BigDecimal("100.00"), 4, 4, "Adherencia observada 60.00 % por debajo del umbral pero ninguna sesion "
                        + "quedo senalada. No se adapta a ciegas.");
        assertEquals("Mantenemos tu plan: no encontramos una sesión concreta que revisar.", MensajeDeIntervencion.intencion(o));
    }

    @Test
    @DisplayName("Sin orden: texto neutro")
    void sinOrden() {
        assertEquals("Esta semana se mantiene tu plan.", MensajeDeIntervencion.intencion(null));
    }
}
