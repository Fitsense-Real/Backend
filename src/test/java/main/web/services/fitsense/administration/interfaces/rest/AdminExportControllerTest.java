package main.web.services.fitsense.administration.interfaces.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("B5.3 · CSV: escape RFC 4180 y sin inyeccion de formulas")
class AdminExportControllerTest {

    @Test
    void campos() {
        assertEquals("", AdminExportController.campo(null));
        assertEquals("P-001", AdminExportController.campo("P-001"));
        assertEquals("-12.5", AdminExportController.campo("-12.5"), "un numero negativo no se toca");
        assertEquals("'=1+1", AdminExportController.campo("=1+1"));
        assertEquals("'@SUM(A1)", AdminExportController.campo("@SUM(A1)"));
        assertEquals("\"a,b\"", AdminExportController.campo("a,b"));
        assertEquals("\"di \"\"x\"\"\"", AdminExportController.campo("di \"x\""));
    }
}
