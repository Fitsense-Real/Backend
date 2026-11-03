package main.web.services.fitsense.shared.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Perfil local: nunca contra Neon ni Railway, IA apagada")
class LocalProfileGuardTest {

    @Test
    @DisplayName("el perfil local-dev o local activa el bloqueo antes de conectar; sin perfil no actua")
    void perfiles() {
        var guardia = new LocalProfileGuard();
        for (var perfil : new String[]{"local", "local-dev"}) {
            var env = new org.springframework.mock.env.MockEnvironment()
                    .withProperty("spring.datasource.url", "jdbc:postgresql://x.neon.tech/neondb");
            env.setActiveProfiles(perfil);
            assertThrows(IllegalStateException.class, () -> guardia.postProcessEnvironment(env, null), perfil);
        }
        var prod = new org.springframework.mock.env.MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:postgresql://x.neon.tech/neondb");
        assertDoesNotThrow(() -> guardia.postProcessEnvironment(prod, null), "produccion no se ve afectada");
    }

    @Test
    @DisplayName("acepta bases locales")
    void locales() {
        for (var url : new String[]{"jdbc:postgresql://localhost:5432/fitsense",
                "jdbc:postgresql://127.0.0.1:5433/fitsense?sslmode=disable",
                "jdbc:postgresql://host.docker.internal:5432/fitsense", "jdbc:postgresql://postgres:5432/fitsense"})
            assertDoesNotThrow(() -> LocalProfileGuard.comprobar(url, false), url);
    }

    @Test
    @DisplayName("rechaza Neon, Railway, hosts remotos, URLs vacias y la IA encendida")
    void remotos() {
        for (var url : new String[]{
                "jdbc:postgresql://ep-dark-brook-b4emftmn-pooler.c-6.us-east-2.aws.neon.tech/neondb?sslmode=require",
                "jdbc:postgresql://containers-us-west-1.railway.app:6543/railway",
                "jdbc:postgresql://10.0.0.5:5432/fitsense", "", "jdbc:mysql://localhost/fitsense"})
            assertThrows(IllegalStateException.class, () -> LocalProfileGuard.comprobar(url, false), url);
        assertThrows(IllegalStateException.class,
                () -> LocalProfileGuard.comprobar("jdbc:postgresql://localhost:5432/fitsense", true));
    }

    @Test
    @DisplayName("application-local-dev.properties: base local por defecto, IA apagada, sin Neon/Railway ni token real")
    void archivoDelPerfil() throws Exception {
        var p = new Properties();
        try (var in = Files.newInputStream(Path.of("src/main/resources/application-local-dev.properties"))) {
            p.load(in);
        }
        var texto = Files.readString(Path.of("src/main/resources/application-local-dev.properties")).toLowerCase();
        assertFalse(texto.contains("neon.tech") || texto.contains("railway.app") || texto.contains("neondb"));
        assertEquals("false", p.getProperty("fitsense.ai.enabled"));
        assertEquals("", p.getProperty("fitsense.ai.api-token"));
        String porDefecto = p.getProperty("spring.datasource.url").replaceAll("^\\$\\{[A-Z_]+:(.*)}$", "$1");
        assertDoesNotThrow(() -> LocalProfileGuard.comprobar(porDefecto, false), porDefecto);
    }
}
