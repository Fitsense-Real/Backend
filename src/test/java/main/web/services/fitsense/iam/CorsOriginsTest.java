package main.web.services.fitsense.iam;

import main.web.services.fitsense.iam.infrastructure.authorization.sfs.configuration.CorsOrigins;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FITSENSE_CORS_ORIGINS · solo origenes exactos")
class CorsOriginsTest {

    @Test
    @DisplayName("vacio no autoriza ninguno")
    void vacio() {
        assertEquals(List.of(), CorsOrigins.parse(null));
        assertEquals(List.of(), CorsOrigins.parse("  "));
    }

    @Test
    @DisplayName("lista separada por comas, sin repetidos")
    void lista() {
        assertEquals(List.of("http://localhost:4200", "https://fitsense-admin.vercel.app"),
                CorsOrigins.parse(" http://localhost:4200 ,https://fitsense-admin.vercel.app,http://localhost:4200,"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"*", "https://*.vercel.app", "https://panel.example/", "https://panel.example/ruta",
            "panel.example", "ftp://panel.example", "https://user:clave@panel.example", "https://panel.example?x=1"})
    @DisplayName("rechaza comodines y lo que no es un origen")
    void invalidos(String valor) {
        assertThrows(IllegalStateException.class, () -> CorsOrigins.parse(valor));
    }
}
