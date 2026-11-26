package main.web.services.fitsense.planning.infrastructure.generation.ai;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * H2 · PLAN DE EJECUCION Y REGISTRO de la evaluacion pareada con REPLICAS (opcion C de H2-v4). Sin Spring y sin red:
 * lo usa {@code H2v2PrimeraPropuestaIT} y lo prueban {@code PlanEjecucionH2Test}.
 * <ul>
 *   <li><b>Orden fijado de antemano.</b> Replica 1: los casos en su orden (G01..G26), impar A-B y par B-A, como en las
 *   evaluaciones anteriores. Replica 2: los casos AL REVES (G26..G01) y, en cada caso, el orden de brazos INVERTIDO
 *   respecto de la replica 1. Asi cada perfil tiene una pareja A-B y otra B-A, y las dos replicas de un perfil quedan
 *   separadas en el tiempo (la carga del proveedor no cae igual en las dos).</li>
 *   <li><b>Clave unica</b> {@code <caso>_r<replica>_<brazo>}: nombra la salida, el registro y la reserva.</li>
 *   <li><b>Reanudacion sin duplicar.</b> Una prediccion cuenta como hecha solo si su linea esta en el registro Y su salida
 *   esta guardada; entonces no se vuelve a pedir. El registro se escribe al recibir cada respuesta.</li>
 * </ul>
 */
public final class PlanEjecucionH2 {

    /** Una pareja que ejecutar: el caso, la replica (1..R) y el orden de sus dos brazos. */
    public record Paso(String caso, int replica, List<String> orden) {
        public String clave(String brazo) {
            return PlanEjecucionH2.clave(caso, replica, brazo);
        }
    }

    private PlanEjecucionH2() {}

    public static String clave(String caso, int replica, String brazo) {
        if (replica < 1) throw new IllegalArgumentException("replica >= 1");
        if (!brazo.equals("A") && !brazo.equals("B")) throw new IllegalArgumentException("brazo A o B");
        return caso + "_r" + replica + "_" + brazo;
    }

    /**
     * El plan completo, en el orden en que se ejecuta.
     *
     * @param casos     los ids en su orden (G01..G26)
     * @param replicas  cuantas replicas (1 o 2)
     * @param soloReplica 0 = todas; n = solo esa replica (para partir la corrida sin cambiar el orden)
     */
    public static List<Paso> plan(List<String> casos, int replicas, int soloReplica) {
        if (replicas < 1 || replicas > 2) throw new IllegalArgumentException("1 o 2 replicas");
        var pasos = new ArrayList<Paso>();
        for (int r = 1; r <= replicas; r++) {
            if (soloReplica != 0 && soloReplica != r) continue;
            var orden = new ArrayList<>(casos);
            if (r == 2) Collections.reverse(orden);
            for (var caso : orden) {
                int i = casos.indexOf(caso);
                boolean aPrimero = i % 2 == 0;
                if (r == 2) aPrimero = !aPrimero;
                pasos.add(new Paso(caso, r, aPrimero ? List.of("A", "B") : List.of("B", "A")));
            }
        }
        return pasos;
    }

    /** Registro de predicciones recibidas: {@code caso;replica;brazo;prediction_id;ms;origen}. */
    public static final class Registro {
        static final String CABECERA = "caso;replica;brazo;prediction_id;ms;origen";
        private final Path fichero;
        private final Path salidas;

        public Registro(Path fichero, Path salidas) {
            this.fichero = fichero;
            this.salidas = salidas;
        }

        public Path salida(String clave) {
            return salidas.resolve(clave + ".json");
        }

        /** Las ya hechas: linea en el registro Y salida guardada. Clave -> [caso, replica, brazo, id, ms, origen]. */
        public Map<String, String[]> hechas() {
            var m = new LinkedHashMap<String, String[]>();
            if (!Files.exists(fichero)) return m;
            try {
                for (var l : Files.readAllLines(fichero, StandardCharsets.UTF_8)) {
                    if (l.isBlank() || l.startsWith("caso;")) continue;
                    var x = l.split(";");
                    var k = clave(x[0], Integer.parseInt(x[1]), x[2]);
                    if (Files.exists(salida(k))) m.putIfAbsent(k, x);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return m;
        }

        /** Guarda la salida y apunta la linea, en ese orden (sin salida no cuenta como hecha). */
        public void anotar(String caso, int replica, String brazo, String predictionId, long ms, String origen,
                           String salidaJson) {
            try {
                Files.createDirectories(salidas);
                Files.writeString(salida(clave(caso, replica, brazo)), salidaJson, StandardCharsets.UTF_8);
                if (!Files.exists(fichero)) Files.writeString(fichero, CABECERA + "\n", StandardCharsets.UTF_8);
                Files.writeString(fichero, String.join(";", caso, String.valueOf(replica), brazo,
                        predictionId == null ? "desconocido" : predictionId, String.valueOf(ms), origen) + "\n",
                        StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
