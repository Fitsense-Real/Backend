package main.web.services.fitsense.revision;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integridad y ceguera del material de revision profesional. Lee los CSV con un parser RFC 4180 (comillas dobles,
 * comas y saltos de linea dentro de un campo) y comprueba que cada fila tiene exactamente las columnas de la cabecera,
 * que los codigos de la muestra, las fichas, la plantilla y la clave interna coinciden sin duplicados ni huerfanos, y
 * que el paquete del evaluador no revela la politica, el estado ni el motivo de seleccion.
 */
@DisplayName("Paquete del evaluador: estructura CSV, codigos y ceguera")
class PaqueteEvaluadorTest {

    static final Path PAQUETE = Path.of("docs/FitSense/revision_profesional/paquete_evaluador");
    static final Path CLAVE = Path.of("docs/FitSense/cierre_final_pr17/CLAVE_INTERNA_MUESTRA.csv");
    static final List<String> REVELADORES = List.of("PLAN-", "C2D", "C2-D", "EF-0", "LW-PREF", "REP-SUST", "NOFACT",
            "NO_FACTIBLE", "VALIDO", "INVALIDO", "SATURAD", "POLITICA", "POLÍTICA", "CLAVE", "MOTIVO_SELECCION",
            "CASO_EXTREMO", "CONTROL_SIN", "EF_ANTES", "EF_DESPUES");

    /** Parser RFC 4180 minimo: comillas dobles, comillas escapadas ("") y separadores dentro de comillas. */
    static List<List<String>> leer(Path p) throws Exception {
        String s = Files.readString(p);
        var filas = new ArrayList<List<String>>();
        var fila = new ArrayList<String>();
        var campo = new StringBuilder();
        boolean comillas = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (comillas) {
                if (c == '"' && i + 1 < s.length() && s.charAt(i + 1) == '"') { campo.append('"'); i++; }
                else if (c == '"') comillas = false;
                else campo.append(c);
            } else if (c == '"') comillas = true;
            else if (c == ',') { fila.add(campo.toString()); campo.setLength(0); }
            else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < s.length() && s.charAt(i + 1) == '\n') i++;
                fila.add(campo.toString()); campo.setLength(0);
                filas.add(fila); fila = new ArrayList<>();
            } else campo.append(c);
        }
        assertFalse(comillas, p + ": comillas sin cerrar");
        if (campo.length() > 0 || !fila.isEmpty()) { fila.add(campo.toString()); filas.add(fila); }
        return filas;
    }

    static List<Map<String, String>> tabla(Path p) throws Exception {
        var filas = leer(p);
        assertFalse(filas.isEmpty(), p + " vacio");
        var cab = filas.get(0);
        var out = new ArrayList<Map<String, String>>();
        for (int i = 1; i < filas.size(); i++) {
            var f = filas.get(i);
            assertEquals(cab.size(), f.size(), p.getFileName() + " fila " + (i + 1) + " tiene " + f.size()
                    + " columnas y la cabecera " + cab.size() + ": " + f);
            var m = new LinkedHashMap<String, String>();
            for (int j = 0; j < cab.size(); j++) m.put(cab.get(j), f.get(j));
            out.add(m);
        }
        return out;
    }

    @Test
    void estructuraYCodigos() throws Exception {
        var muestra = tabla(PAQUETE.resolve("MUESTRA_PROFESIONAL_CIEGA.csv"));
        var fichas = tabla(PAQUETE.resolve("FICHAS_NO_FACTIBLE.csv"));
        var plantilla = tabla(PAQUETE.resolve("PLANTILLA_RESPUESTAS_EVALUADOR.csv"));
        var clave = tabla(CLAVE);

        var planes = muestra.stream().map(r -> r.get("codigo")).collect(Collectors.toCollection(TreeSet::new));
        var codFichas = fichas.stream().map(r -> r.get("ficha")).toList();
        assertEquals(34, planes.size(), "34 planes completos");
        assertEquals(List.of("D01", "D02"), codFichas, "dos fichas, en orden");
        var todos = new TreeSet<>(planes);
        todos.addAll(codFichas);
        assertEquals(36, todos.size(), "36 codigos unicos");

        var codPlantilla = plantilla.stream().map(r -> r.get("codigo")).toList();
        assertEquals(codPlantilla.size(), new HashSet<>(codPlantilla).size(), "plantilla sin duplicados");
        assertEquals(todos, new TreeSet<>(codPlantilla), "plantilla = planes + fichas (sin huerfanos)");
        var codClave = clave.stream().map(r -> r.get("codigo")).toList();
        assertEquals(codClave.size(), new HashSet<>(codClave).size(), "clave sin duplicados");
        assertEquals(todos, new TreeSet<>(codClave), "clave = planes + fichas (sin huerfanos)");

        // D01 y D02: cada valor en su columna
        for (var f : fichas) {
            assertTrue(f.get("sesion_requerida").startsWith("PULL (espalda y biceps), dos veces"), f.toString());
            assertTrue(f.get("minutos_configurados").matches("\\d+"), f.toString());
            assertFalse(f.get("ejercicios_disponibles_en_la_zona").isBlank(), f.toString());
        }
        // Planes completos: cada ejercicio con nombre, descripcion, patron, musculo, dosis y descanso
        for (var r : muestra) {
            for (var c : List.of("nombre", "descripcion", "patron", "musculo_principal", "series", "descanso_s",
                    "enfoque", "duracion_estimada_min", "sesion", "orden"))
                assertFalse(r.get(c) == null || r.get(c).isBlank() || "null".equals(r.get(c)), r.get("codigo") + " sin " + c);
            String dosis = "DURATION".equals(r.get("tipo")) ? r.get("segundos") : r.get("repeticiones");
            assertTrue(dosis != null && dosis.matches("\\d+"), r.get("codigo") + " sin dosis");
        }
    }

    @Test
    void paqueteCiegoYSinClave() throws Exception {
        try (var archivos = Files.list(PAQUETE)) {
            for (var p : archivos.toList()) {
                assertFalse(p.getFileName().toString().toUpperCase(Locale.ROOT).contains("CLAVE"),
                        "la clave no va en el paquete del evaluador");
                if (p.toString().endsWith(".csv")) {
                    String texto = Files.readString(p).toUpperCase(Locale.ROOT);
                    for (var w : REVELADORES) assertFalse(texto.contains(w), p.getFileName() + " revela " + w);
                }
            }
        }
        String instrucciones = Files.readString(PAQUETE.resolve("INSTRUCCIONES_EVALUADOR.md")).toUpperCase(Locale.ROOT);
        for (var w : List.of("PLAN-", "C2D", "C2-D", "EF-0", "LW-PREF", "REP-SUST", "NOFACT", "CLAVE_INTERNA",
                "SATURAD"))
            assertFalse(instrucciones.contains(w), "las instrucciones revelan " + w);
    }
}
