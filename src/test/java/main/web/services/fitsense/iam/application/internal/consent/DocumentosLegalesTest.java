package main.web.services.fitsense.iam.application.internal.consent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("B2.2 · documentos legales: solo se publica lo aprobado, con https; vigente por fecha")
class DocumentosLegalesTest {

    static final Clock HOY = Clock.fixed(Instant.parse("2026-12-15T12:00:00Z"), ZoneOffset.UTC);

    static DocumentosLegales.Documento doc(ConsentimientoService.Tipo t, String v, DocumentosLegales.Estado e,
                                            String desde, String url, String aprobacion) {
        var d = new DocumentosLegales.Documento();
        d.setType(t);
        d.setVersion(v);
        d.setTitle("Titulo " + v);
        d.setStatus(e);
        d.setEffectiveFrom(desde == null ? null : LocalDate.parse(desde));
        d.setUrl(url);
        d.setApprovalReference(aprobacion);
        return d;
    }

    static DocumentosLegales con(DocumentosLegales.Documento... ds) {
        var r = new DocumentosLegales();
        r.setItems(new java.util.ArrayList<>(List.of(ds)));
        return r;
    }

    @Test
    @DisplayName("publicado sin referencia de aprobacion, sin https o sin fecha: no arranca")
    void publicadoExigeAprobacion() {
        var T = ConsentimientoService.Tipo.TERMS_OF_SERVICE;
        var P = DocumentosLegales.Estado.PUBLISHED;
        assertThrows(IllegalStateException.class,
                () -> con(doc(T, "T1", P, "2026-12-01", "https://x.invalid/t1", null)).validar());
        assertThrows(IllegalStateException.class,
                () -> con(doc(T, "T1", P, "2026-12-01", "http://x.invalid/t1", "ACTA-1")).validar());
        assertThrows(IllegalStateException.class,
                () -> con(doc(T, "T1", P, null, "https://x.invalid/t1", "ACTA-1")).validar());
        assertThrows(IllegalStateException.class,
                () -> con(doc(T, "T1", P, "2026-12-01", "https://x.invalid/a", "A"),
                        doc(T, "T1", P, "2026-12-02", "https://x.invalid/b", "A")).validar(), "version repetida");
        assertDoesNotThrow(() -> con(doc(T, "T1", DocumentosLegales.Estado.DRAFT, null, null, null)).validar(),
                "un borrador no necesita nada de eso");
    }

    @Test
    @DisplayName("vigente: el publicado mas reciente que ya empezo; ni borradores, ni retirados, ni futuros")
    void vigente() {
        var C = ConsentimientoService.Tipo.STUDY_PARTICIPATION;
        var r = con(doc(C, "CI-1", DocumentosLegales.Estado.PUBLISHED, "2026-11-01", "https://x.invalid/1", "A"),
                doc(C, "CI-2", DocumentosLegales.Estado.PUBLISHED, "2026-12-10", "https://x.invalid/2", "A"),
                doc(C, "CI-3", DocumentosLegales.Estado.PUBLISHED, "2027-01-01", "https://x.invalid/3", "A"),
                doc(C, "CI-B", DocumentosLegales.Estado.DRAFT, null, null, null),
                doc(C, "CI-R", DocumentosLegales.Estado.RETIRED, "2026-12-12", "https://x.invalid/r", "A"));
        r.validar();
        assertEquals("CI-2", r.vigente(C, HOY).orElseThrow().getVersion());
        assertTrue(r.vigente(ConsentimientoService.Tipo.PRIVACY_POLICY, HOY).isEmpty());
        assertEquals(1, r.vigentes(HOY).size());
    }
}
