package main.web.services.fitsense.planning.domain.services;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * Diagnostico ESTRUCTURADO de una semana que no se puede generar porque falta un patron principal de una sesion
 * (regla VEF de EF-0.1). Lo construye la API a partir de los problemas del validador, sin exponer su texto tecnico.
 * <p>
 * Solo describe lo que el validador demostro: que patron falta, que el catalogo elegible con el equipamiento
 * declarado no tiene ninguno y, si existe, el material que el laboratorio verifico que lo resuelve (nunca una compra
 * obligatoria). No cambia el equipamiento ni la division: eso lo decide la persona en su perfil.
 */
public final class DiagnosticoNoFactible {

    private DiagnosticoNoFactible() {}

    public static final String VERSION = "NOFACT-0.1";

    /**
     * Material con el que el laboratorio de cierre (MATRIZ_ALTERNATIVAS_48.csv) obtuvo una semana valida con traccion
     * real para TODOS los perfiles sin traccion. Se ofrece como opcion "si ya lo tienes", nunca como compra.
     */
    public static final List<String> MATERIAL_TRACCION_VERIFICADO = List.of("band", "dumbbell", "pull-up bar",
            "suspension straps", "wall bars", "anchor point");

    public record Diagnostico(String status, String reason, List<String> missingPatterns, boolean equipmentRestriction,
                              List<String> optionalEquipmentCodes, String message, String version) {}

    /** El diagnostico si algun problema es un VEF NO_FACTIBLE (falta el patron principal y no hay candidatos). */
    public static Optional<Diagnostico> de(List<String> problemas) {
        if (problemas == null) return Optional.empty();
        var razones = new LinkedHashSet<String>();
        var patrones = new LinkedHashSet<String>();
        for (var p : problemas) {
            if (p == null || !p.startsWith("VEF:") || !p.contains("NO_FACTIBLE")) continue;
            if (p.contains("entrenamiento PULL")) {
                razones.add("PULL_NO_DISPONIBLE");
                patrones.addAll(ElegibilidadFuncional.TRACCION.stream().sorted().toList());
            } else if (p.contains("entrenamiento PUSH")) {
                razones.add("PUSH_NO_DISPONIBLE");
                patrones.addAll(ElegibilidadFuncional.EMPUJE.stream().sorted().toList());
            } else if (p.contains("entrenamiento LEGS")) {
                razones.add("PIERNA_NO_DISPONIBLE");
                patrones.addAll(ElegibilidadFuncional.PIERNA.stream().sorted().toList());
            }
        }
        if (razones.isEmpty()) return Optional.empty();
        boolean soloTraccion = razones.size() == 1 && razones.contains("PULL_NO_DISPONIBLE");
        String reason = razones.size() == 1 ? razones.iterator().next() : "PATRONES_NO_DISPONIBLES";
        var mensaje = new StringBuilder(soloTraccion
                ? "Con el equipamiento que indicaste no encontramos ejercicios de tracción (remos o dominadas) para"
                + " las sesiones de espalda de esta semana, así que no generamos una rutina incompleta."
                : "Con el equipamiento que indicaste faltan ejercicios básicos para algunas sesiones de esta semana,"
                + " así que no generamos una rutina incompleta.");
        List<String> opcional = new ArrayList<>();
        if (soloTraccion) {
            opcional.addAll(MATERIAL_TRACCION_VERIFICADO);
            mensaje.append(" Si ya tienes una banda elástica, mancuernas, una barra de dominadas, correas de suspensión,"
                    + " una espaldera o un punto de anclaje firme, puedes añadirlo en tu perfil. No necesitas comprar"
                    + " nada.");
        }
        return Optional.of(new Diagnostico("NO_FACTIBLE", reason, List.copyOf(patrones), true, List.copyOf(opcional),
                mensaje.toString(), VERSION));
    }
}
