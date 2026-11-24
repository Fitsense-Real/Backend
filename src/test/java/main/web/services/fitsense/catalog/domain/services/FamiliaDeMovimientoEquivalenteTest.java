package main.web.services.fitsense.catalog.domain.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static main.web.services.fitsense.catalog.domain.services.FamiliaDeMovimientoEquivalente.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("O20: familia de movimiento equivalente, nivel 1 por nombre y nivel 2 por lista explicita (sin base de datos)")
class FamiliaDeMovimientoEquivalenteTest {

    @Test
    @DisplayName("Nivel 1: variantes v. N, mayusculas, espacios y duplicados de nombre identico")
    void variantes() {
        assertEquals("dumbbell front raise", clave("dumbbell front raise v. 2"));
        assertEquals("dumbbell front raise", clave("  Dumbbell   Front Raise  "));
        assertEquals("lever shoulder press", clave("lever shoulder press v.3"));
        assertEquals("barbell upright row", clave("barbell upright row V. 10"));
        assertTrue(mismaFamilia("dumbbell front raise", "dumbbell front raise v. 2"));
        assertTrue(mismaFamilia("self assisted inverse leg curl", "self assisted inverse leg curl"));
        assertFalse(mismaFamilia("dumbbell front raise", "dumbbell lateral raise"));
        // «v.» dentro del nombre no es un sufijo de variante.
        assertEquals("v. sit hold", clave("v. sit hold"));
        assertNull(clave(null));
        assertNull(clave("   "));
        assertFalse(mismaFamilia(null, null), "dos ejercicios sin nombre no son de la misma familia");
    }

    @Test
    @DisplayName("Nivel 2, confirmadas: el mismo gesto con otro material, lateralidad o apoyo, segun la lista explicita")
    void mismoGestoConfirmado() {
        // El caso que motiva el nivel 2: dumbbell front raise frente a dumbbell seated front raise.
        assertTrue(mismoGesto("dumbbell front raise", "dumbbell seated front raise"));
        assertTrue(mismoGesto("dumbbell front raise v. 2", "dumbbell seated alternate front raise"), "por su clave");
        assertTrue(mismoGesto("dumbbell front raise", "band front raise"), "otro material");
        assertTrue(mismoGesto("dumbbell front raise", "cable front raise"));
        assertTrue(mismoGesto("Dumbbell Front Raise", "barbell front raise"), "mayusculas");
        assertEquals(NIVEL_MISMO_GESTO, nivel("dumbbell front raise", "dumbbell seated front raise"));
        assertEquals(NIVEL_VARIANTE, nivel("dumbbell front raise", "dumbbell front raise v. 2"));
        var g = grupo("dumbbell seated front raise").orElseThrow();
        assertTrue(g.id().endsWith("front raise"), g.id());
        assertFalse(g.justificacion().isBlank());
        // Otro nombre del mismo gesto: variante ortografica (biceps/bicep) y el mismo gesto con dos patrones registrados.
        assertTrue(mismoGesto("dumbbell biceps curl", "lever bicep curl"));
        assertTrue(mismoGesto("barbell upright row", "cable upright row"));
        assertTrue(equivalentes("barbell upright row v. 3", "dumbbell upright row"));
        // Apoyo sentado/de pie y lateralidad.
        assertTrue(mismoGesto("barbell seated calf raise", "barbell standing calf raise"));
        assertTrue(mismoGesto("dumbbell bent over row", "barbell one arm bent over row"));
    }

    @Test
    @DisplayName("No equivalentes: mismo patron y musculo con otro movimiento, otro angulo o sinonimo sin confirmar")
    void noEquivalentes() {
        // Mismo patron (SHOULDER_RAISE) y musculo (delts): coincidir en eso NO basta.
        assertFalse(equivalentes("dumbbell front raise", "dumbbell lateral raise"));
        assertFalse(equivalentes("dumbbell front raise", "dumbbell full can lateral raise"));
        assertFalse(equivalentes("dumbbell front raise", "dumbbell lateral to front raise"));
        assertFalse(equivalentes("dumbbell front raise", "dumbbell front raise above head"));
        assertFalse(equivalentes("dumbbell front raise", "dumbbell rear delt raise"));
        // Cambio de angulo o de apoyo del cuerpo: pendiente, no equivalente.
        assertFalse(equivalentes("lever chest press", "lever incline chest press v. 2"));
        assertFalse(equivalentes("barbell bench press", "barbell incline bench press"));
        assertFalse(equivalentes("barbell incline bench press", "barbell decline bench press"));
        assertTrue(mismoGesto("barbell incline bench press", "dumbbell incline bench press"), "mismo angulo, si");
        // Posibles sinonimos con otras palabras: pendientes de revision profesional.
        assertFalse(equivalentes("barbell bench press", "lever chest press"));
        assertFalse(equivalentes("lever shoulder press", "lever military press"));
        // Nucleos genericos y revisados como distintos.
        assertFalse(equivalentes("barbell curl", "dumbbell incline curl"));
        assertFalse(equivalentes("barbell hack squat", "smith hack squat"));
        // Sin nombre, o fuera de la lista: nada.
        assertTrue(grupo("dumbbell lateral to front raise").isEmpty());
        assertFalse(mismoGesto(null, null));
        assertFalse(mismoGesto("dumbbell front raise", "dumbbell front raise v. 2"), "eso es nivel 1, no nivel 2");
    }

    @Test
    @DisplayName("La lista explicita: cada grupo con dos o mas miembros, justificacion y sin miembros en dos grupos")
    void listaExplicita() {
        var grupos = grupos();
        assertTrue(grupos.size() >= 100, "grupos confirmados: " + grupos.size());
        var vistas = new java.util.HashSet<String>();
        for (var g : grupos) {
            assertTrue(g.claves().size() >= 2, g.id());
            assertFalse(g.justificacion().isBlank(), g.id());
            for (var k : g.claves()) {
                assertEquals(k, clave(k), "miembro normalizado en " + g.id());
                assertTrue(vistas.add(k), k + " en dos grupos");
            }
        }
    }

    @Test
    @DisplayName("Una lista ambigua o incompleta no se aplica a medias")
    void listaInvalida() {
        var cab = "grupo;patron;musculo;miembros;justificacion\n";
        assertThrows(IllegalStateException.class, () -> leer(new java.io.StringReader(cab
                + "G1;P;m;a b|c d;j\nG2;P;m;c d|e f;j\n")), "miembro en dos grupos");
        assertThrows(IllegalStateException.class, () -> leer(new java.io.StringReader(cab + "G1;P;m;A b|c d;j\n")),
                "miembro sin normalizar");
        assertThrows(IllegalStateException.class, () -> leer(new java.io.StringReader(cab + "G1;P;m;a b|c d;\n")),
                "sin justificacion");
        assertThrows(IllegalStateException.class, () -> leer(new java.io.StringReader(cab + "G1;P;m;a b;j\n")),
                "un solo miembro");
        assertThrows(IllegalStateException.class, () -> leer(new java.io.StringReader(cab + "G1;P;m;a b|c d v. 2;j\n")),
                "un miembro es una clave, sin v. N");
        assertDoesNotThrow(() -> leer(new java.io.StringReader("# comentario\n" + cab + "G1;P;m;a b|c d;j\n")));
    }

    @Test
    @DisplayName("Cierre final: otro angulo no es equivalente, pero es PRECAUCION; mismo angulo, otro material, no")
    void precaucionOtroAngulo() {
        assertFalse(equivalentes("lever chest press", "lever incline chest press v. 2"), "sigue sin ser equivalencia");
        assertEquals(NIVEL_PRECAUCION_ANGULO, nivel("lever incline chest press v. 2", "lever chest press"));
        assertEquals(NIVEL_PRECAUCION_ANGULO, nivel("lever chest press", "lever decline chest press"), "simetrico");
        assertEquals(NIVEL_PRECAUCION_ANGULO, nivel("barbell bench press", "barbell incline bench press"));
        assertEquals(NIVEL_MISMO_GESTO, nivel("barbell incline bench press", "dumbbell incline bench press"),
                "mismo angulo: equivalencia confirmada, no precaucion");
        assertTrue(precaucionPorAngulo("lever chest press", "cable seated chest press").isEmpty(), "mismo angulo");
        assertTrue(clavesPrecaucionAngulo("lever incline chest press v. 2").contains("lever chest press"));
        assertFalse(clavesPrecaucionAngulo("lever incline chest press v. 2").contains("dumbbell one arm incline chest press"),
                "lo del mismo angulo es su familia, no su precaucion");
        // Lo pendiente que no es de angulo sigue sin ningun efecto: sinonimos, genericos y revisados como distintos.
        assertNull(nivel("barbell bench press", "lever chest press"));
        assertNull(nivel("barbell curl", "dumbbell incline curl"));
        assertNull(nivel("barbell hack squat", "smith hack squat"));
        assertNull(nivel("dumbbell front raise", "dumbbell lateral raise"));
    }

    @Test
    @DisplayName("Lista de precauciones valida (37 filas, cada una con dos angulos o mas) y una ambigua se rechaza")
    void listaPrecauciones() {
        assertEquals(37, filasPrecaucion().size());
        for (var f : filasPrecaucion()) {
            assertTrue(f.subgrupos().size() >= 2, f.id());
            assertFalse(f.justificacion().isBlank(), f.id());
        }
        var cab = "fila;patron;musculo;subgrupos;justificacion\n";
        assertThrows(IllegalStateException.class, () -> leerPrecauciones(new java.io.StringReader(cab
                + "PA-1;P;m;sin angulo=a b;j\n")), "un solo angulo");
        assertThrows(IllegalStateException.class, () -> leerPrecauciones(new java.io.StringReader(cab
                + "PA-1;P;m;sin angulo=a b^incline=a b;j\n")), "miembro en dos angulos");
        assertThrows(IllegalStateException.class, () -> leerPrecauciones(new java.io.StringReader(cab
                + "PA-1;P;m;sin angulo=a b^incline=c d;\n")), "sin justificacion");
        assertDoesNotThrow(() -> leerPrecauciones(new java.io.StringReader(cab + "PA-1;P;m;sin angulo=a b^incline=c d;j\n")));
    }

    @Test
    @DisplayName("Punto 1: ninguna de las 53 diferencias pendientes se convirtio en equivalencia")
    void pendientesSinConvertir() throws Exception {
        var filas = java.nio.file.Files.readAllLines(
                java.nio.file.Path.of("docs/FitSense/seguridad_o20/evidencia/equivalencias_pendientes.csv"));
        assertEquals(54, filas.size(), "cabecera y 53 pendientes");
        int comprobados = 0;
        for (var linea : filas.subList(1, filas.size())) {
            // Los ejercicios de la fila: «id nombre | id nombre ...».
            var nombres = new java.util.ArrayList<String>();
            var m = java.util.regex.Pattern.compile("(?<![\\w/])\\d+ ([^|\"]+)").matcher(linea);
            while (m.find()) nombres.add(m.group(1).strip());
            boolean angulo = linea.contains("entre angulos");
            for (int a = 0; a < nombres.size(); a++)
                for (int b = a + 1; b < nombres.size(); b++) {
                    var x = nombres.get(a);
                    var y = nombres.get(b);
                    if (!angulo) {
                        // Generico, revisado o sinonimo: o no tienen relacion, o la tienen por un grupo confirmado de
                        // su mismo angulo (la fila agrupa todo el nucleo); nunca por la lista de precauciones.
                        assertNotEquals(NIVEL_PRECAUCION_ANGULO, nivel(x, y), x + " / " + y);
                        continue;
                    }
                    var mismoAngulo = precaucionPorAngulo(x, y).isEmpty();
                    if (!mismoAngulo) {
                        assertFalse(equivalentes(x, y), "pendiente por angulo convertida en equivalencia: " + x + " / " + y);
                        assertEquals(NIVEL_PRECAUCION_ANGULO, nivel(x, y), x + " / " + y);
                        comprobados++;
                    }
                }
        }
        assertTrue(comprobados > 100, "pares entre angulos comprobados: " + comprobados);
    }
}
