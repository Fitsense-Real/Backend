package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.infrastructure.generation.rules.RuleBasedTrainingPlanGenerator;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.BufferedWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.Mockito.when;

/**
 * LABORATORIO (opt-in, sin Replicate): IMPACTO DEL LIMITE DE DIFICULTAD sobre los planes reales de los 960 perfiles
 * de la matriz (los mismos de ComparacionPerfilesIT, PLAN-HIST-1, motor de reglas).
 * <p>
 * Para CADA ejercicio colocado se simula que dolio y se pregunta a SUST-SEG-1 (la sustitucion determinista por
 * seguridad, con los elegibles reales del perfil, el enfoque y los dias contiguos) que sustituto elige SIN limite (hoy)
 * y CON limite (candidato). Y se mide la EXPOSICION en VARIANTE: cuantas alternativas elegibles del mismo patron tiene
 * de dificultad menor, igual y mayor (la IA solo esta obligada por el prompt a no subir; ningun validador lo impide).
 * <p>
 * {@code mvnw verify -Pintegration -Dit.test=ImpactoLimiteDificultadIT -Dfitsense.limite.dificultad.dir=<dir>}
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.limite.dificultad.dir", matches = ".+")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("Laboratorio: impacto del limite de dificultad en SUST-SEG-1 y exposicion en VARIANTE (960 perfiles)")
class ImpactoLimiteDificultadIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoBean ReplicateSessionFixGenerator ia;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired UserProfileCommandService perfiles;
    @Autowired RuleBasedTrainingPlanGenerator reglas;
    @Autowired JdbcTemplate jdbc;

    static final LocalDate SEMANA = LocalDate.of(2026, 11, 2);
    static final Map<String, List<String>> EQUIPOS = new LinkedHashMap<>();
    static final Map<Integer, List<Short>> DIAS = Map.of(
            1, List.of((short) 1), 2, List.of((short) 1, (short) 4), 3, List.of((short) 1, (short) 3, (short) 5),
            4, List.of((short) 1, (short) 2, (short) 4, (short) 5),
            6, List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6),
            7, List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6, (short) 7));

    static {
        EQUIPOS.put("GIMNASIO", List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine", "bench",
                "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine"));
        EQUIPOS.put("CASA_MANCUERNAS_BANDAS", List.of("body weight", "dumbbell", "band"));
        EQUIPOS.put("CASA_PESO_CORPORAL", List.of("body weight"));
    }

    record Perfil(String id, GoalType objetivo, FitnessLevel nivel, String equipo, int dias, int minutos,
                  boolean restringido, String grupo) {}

    static int secuencia;

    @Test
    void medir() throws Exception {
        when(ia.isEnabled()).thenReturn(false);
        var dir = Path.of(System.getProperty("fitsense.limite.dificultad.dir"));
        Files.createDirectories(dir);
        try (BufferedWriter out = Files.newBufferedWriter(dir.resolve("colocaciones_limite.csv"))) {
            out.write("perfil,grupo,objetivo,nivel,equipo,dias,minutos,fecha,enfoque,exercise_id,patron,dificultad,"
                    + "sust_sin_limite,dif_sin_limite,sust_con_limite,dif_con_limite,alt_menor,alt_igual,alt_mayor\n");
            for (var p : perfiles960()) {
                long u = crear(p);
                var prep = planComandos.prepare(new GenerateWeeklyPlanCommand(u, SEMANA, null, true, null, null));
                var ctx = prep.context().conPolitica(PlanningPolicy.deRegistro("PLAN-HIST-1"));
                PlanDraft d;
                try {
                    d = reglas.generate(ctx, List.of());
                } catch (RuntimeException e) {
                    continue;
                }
                var porId = ctx.availableExercises().stream()
                        .collect(Collectors.toMap(CandidateExercise::exerciseId, c -> c, (a, b) -> a, LinkedHashMap::new));
                var idsPorFecha = new HashMap<LocalDate, Set<Long>>();
                for (var w : d.workouts())
                    idsPorFecha.computeIfAbsent(w.scheduledDate(), k -> new HashSet<>())
                            .addAll(w.exercises().stream().map(PlanDraft.DraftExercise::exerciseId).toList());
                for (var w : d.workouts()) {
                    var enSesion = w.exercises().stream().map(PlanDraft.DraftExercise::exerciseId)
                            .collect(Collectors.toSet());
                    var contiguos = new HashSet<Long>();
                    contiguos.addAll(idsPorFecha.getOrDefault(w.scheduledDate().minusDays(1), Set.of()));
                    contiguos.addAll(idsPorFecha.getOrDefault(w.scheduledDate().plusDays(1), Set.of()));
                    for (var e : w.exercises()) {
                        var original = porId.get(e.exerciseId());
                        if (original == null) continue;
                        var elegibles = new LinkedHashMap<>(porId);
                        elegibles.remove(original.exerciseId());   // el que dolio deja de ser elegible
                        var sin = SafetySubstitutionPolicy.elegir(original, w.focus(), elegibles, enSesion, contiguos,
                                ctx, false);
                        var con = SafetySubstitutionPolicy.elegir(original, w.focus(), elegibles, enSesion, contiguos,
                                ctx, true);
                        int menor = 0, igual = 0, mayor = 0;
                        for (var c : elegibles.values()) {
                            if (enSesion.contains(c.exerciseId())
                                    || !Objects.equals(c.movementPattern(), original.movementPattern())) continue;
                            if (c.difficulty() < original.difficulty()) menor++;
                            else if (c.difficulty() == original.difficulty()) igual++;
                            else mayor++;
                        }
                        out.write(String.join(",", p.id(), p.grupo(), p.objetivo().name(), p.nivel().name(), p.equipo(),
                                String.valueOf(p.dias()), String.valueOf(p.minutos()), String.valueOf(w.scheduledDate()),
                                String.valueOf(w.focus()), String.valueOf(original.exerciseId()),
                                String.valueOf(original.movementPattern()), String.valueOf(original.difficulty()),
                                sin.map(c -> String.valueOf(c.exerciseId())).orElse(""),
                                sin.map(c -> String.valueOf(c.difficulty())).orElse(""),
                                con.map(c -> String.valueOf(c.exerciseId())).orElse(""),
                                con.map(c -> String.valueOf(c.difficulty())).orElse(""),
                                String.valueOf(menor), String.valueOf(igual), String.valueOf(mayor)) + "\n");
                    }
                }
            }
        }
    }

    List<Perfil> perfiles960() {
        var lista = new ArrayList<Perfil>();
        int n = 0;
        for (var equipo : EQUIPOS.keySet())
            for (int dias : new int[]{1, 2, 3, 4, 6, 7})
                for (var o : GoalType.values())
                    for (var nv : FitnessLevel.values())
                        for (int m : new int[]{30, 45, 60, 75})
                            lista.add(new Perfil("R%04d".formatted(++n), o, nv, equipo, dias, m, false, "BASE"));
        for (var o : GoalType.values())
            for (var nv : FitnessLevel.values())
                for (int m : new int[]{30, 45, 60, 75}) {
                    lista.add(new Perfil("R%04d".formatted(++n), o, nv, "GIMNASIO", 3, m, true, "RESTRICCIONES"));
                    lista.add(new Perfil("R%04d".formatted(++n), o, nv, "GIMNASIO", 3, m, false, "DOLOR_P02"));
                }
        return lista;
    }

    long crear(Perfil p) {
        String email = "labdif" + (++secuencia) + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'LabD', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        boolean libre = !p.restringido();
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 6, 15), BiologicalSex.FEMALE,
                new BigDecimal("168"), new BigDecimal("68"), null, p.nivel(), p.objetivo(), "laboratorio d",
                "GIMNASIO".equals(p.equipo()) ? TrainingLocation.GYM : TrainingLocation.HOME, (short) p.dias(),
                DIAS.get(p.dias()), (short) p.minutos(), EQUIPOS.get(p.equipo()), List.of(), "Ninguna", libre, libre,
                libre, libre, libre));
        return u;
    }
}
