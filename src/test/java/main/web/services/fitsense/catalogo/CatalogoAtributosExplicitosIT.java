package main.web.services.fitsense.catalogo;

import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.services.CatalogEligibilityPolicy;
import main.web.services.fitsense.planning.domain.services.ImpactoPolicy;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V51 · IMPACTO Y ESTIRAMIENTO EXPLICITOS, con el catalogo real migrado.
 * <ol>
 *   <li>Todo activo tiene los dos atributos (el CHECK de V51 lo exige; aqui se comprueba el dato).</li>
 *   <li>Paridad: los valores fijados por V51 son EXACTAMENTE los que la regla anterior derivaba del nombre, salvo las
 *       correcciones revisadas contra el GIF que migraciones posteriores aplican y que se listan aqui, una a una.</li>
 *   <li>El conjunto elegible entrega los atributos a planificacion: ningun candidato depende del respaldo por nombre.</li>
 * </ol>
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("V51 · impacto y estiramiento explicitos en el catalogo real")
class CatalogoAtributosExplicitosIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired JdbcTemplate jdbc;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired UserProfileCommandService perfiles;

    /**
     * Correcciones revisadas contra el GIF posteriores a V51 (V52, cierre del catalogo): cada una tiene segunda revision,
     * verificacion independiente y, si relaja una restriccion, adjudicacion del coordinador. Evidencia en
     * docs/FitSense/catalogo_integral/cierre/adjudicacion/decisiones_v52.tsv y CORRECCIONES_DEFINITIVAS.md. Fuera de esta
     * lista, el valor explicito debe coincidir con el que V51 congelo: cualquier otra diferencia es un cambio no documentado.
     */
    static final Map<Long, String> IMPACTO_CORREGIDO = Map.ofEntries(
            Map.entry(262L, "BAJO"), Map.entry(267L, "BAJO"), Map.entry(675L, "BAJO"), Map.entry(952L, "BAJO"),
            Map.entry(1069L, "BAJO"), Map.entry(1083L, "ALTO"), Map.entry(1117L, "MODERADO"), Map.entry(1295L, "BAJO"),
            Map.entry(1333L, "ALTO"));
    static final Map<Long, Boolean> ESTIRAMIENTO_CORREGIDO = Map.of(
            312L, true, 455L, false, 773L, false, 1192L, true, 1194L, true);

    /** Valores que V51 congelo (id -> [impacto, estiramiento]), leidos de la propia migracion. */
    static Map<Long, String[]> congeladosV51() throws Exception {
        var res = new org.springframework.core.io.ClassPathResource(
                "db/migration/V51__catalogo_impacto_y_estiramiento_explicitos.sql");
        String sql = new String(res.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        var m = java.util.regex.Pattern.compile("(?m)^\\((\\d+), '(\\w+)', (TRUE|FALSE)\\)").matcher(sql);
        var r = new java.util.HashMap<Long, String[]>();
        while (m.find()) r.put(Long.parseLong(m.group(1)), new String[]{m.group(2), m.group(3)});
        return r;
    }

    @Test
    @DisplayName("todo ejercicio activo tiene impacto y estiramiento explicitos")
    void todosLosActivosTienenValor() {
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM exercises WHERE is_active AND (impact_level IS NULL"
                + " OR is_stretch IS NULL)", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM exercises WHERE high_impact AND impact_level = 'BAJO'",
                Integer.class));
    }

    @Test
    @DisplayName("V51 reproduce la regla por nombre sobre el catalogo V50 (1376 filas)")
    void v51ReproduceLaRegla() throws Exception {
        assertEquals(1376, congeladosV51().size());
    }

    @Test
    @DisplayName("paridad con lo congelado en V51, salvo correcciones revisadas y documentadas")
    void paridadConV51() throws Exception {
        var v51 = congeladosV51();
        var difImpacto = new TreeMap<Long, String>();
        var difEstiramiento = new TreeMap<Long, String>();
        jdbc.query("SELECT exercise_id, name_es, impact_level, is_stretch FROM exercises WHERE is_active", rs -> {
            long id = rs.getLong(1);
            String[] previo = v51.get(id);
            String esperado = IMPACTO_CORREGIDO.getOrDefault(id, previo[0]);
            if (!esperado.equals(rs.getString(3))) difImpacto.put(id, rs.getString(2) + ": esperado=" + esperado + " dato=" + rs.getString(3));
            boolean estEsperado = ESTIRAMIENTO_CORREGIDO.getOrDefault(id, Boolean.parseBoolean(previo[1].toLowerCase()));
            if (estEsperado != rs.getBoolean(4)) difEstiramiento.put(id, rs.getString(2) + ": esperado=" + estEsperado + " dato=" + rs.getBoolean(4));
        });
        assertTrue(difImpacto.isEmpty(), "impacto sin documentar: " + difImpacto);
        assertTrue(difEstiramiento.isEmpty(), "estiramiento sin documentar: " + difEstiramiento);
    }

    @Test
    @DisplayName("el conjunto elegible entrega los atributos explicitos: nada depende del nombre")
    void elConjuntoElegibleLlevaLosAtributos() {
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES ('v51@pruebas.invalid', 'x', 'V51', now(), now())");
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = 'v51@pruebas.invalid'", Long.class);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1985, 6, 1), BiologicalSex.MALE,
                new BigDecimal("180"), new BigDecimal("100"), new BigDecimal("80"), FitnessLevel.ADVANCED,
                GoalType.GENERAL_FITNESS, "x", TrainingLocation.GYM, (short) 4,
                List.of((short) 1, (short) 3, (short) 5, (short) 6), (short) 60,
                List.of("body weight", "dumbbell", "barbell", "cable", "bench", "band", "leverage machine",
                        "pull-up bar", "stationary bike", "treadmill"), List.of(), null, true, true, true, true, true));
        var semana = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
        var candidatos = planComandos.prepare(GenerateWeeklyPlanCommand.firstPlan(u, semana)).context().availableExercises();
        assertFalse(candidatos.isEmpty());
        var sinDato = candidatos.stream().filter(c -> c.impactLevel() == null || c.stretch() == null)
                .map(CandidateExercise::exerciseId).toList();
        assertTrue(sinDato.isEmpty(), "candidatos sin atributo explicito: " + sinDato);
    }
}
