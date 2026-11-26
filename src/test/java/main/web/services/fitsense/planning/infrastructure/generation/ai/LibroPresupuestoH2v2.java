package main.web.services.fitsense.planning.infrastructure.generation.ai;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;

/**
 * H2-v2 · LIBRO DE PRESUPUESTO de la evaluacion real (opcion A: USD 7,50 en total, compartidos por las dos etapas).
 * <p>
 * Un CSV que solo crece ({@code docs/FitSense/h2_v2/evaluacion_real/libro_presupuesto.csv}), comun a todas las
 * ejecuciones: el limite NO se reinicia entre corridas ni entre etapas.
 * <ul>
 *   <li>{@link #reservar}: ANTES de crear cada prediccion se apunta su reserva conservadora (la cota: 1 token por byte
 *   de entrada + 256, y el tope de salida de 16.000 tokens, a la tarifa de gpt-5-mini). Si la reserva no cabe en el
 *   tope total, o en el de la etapa, no se apunta y la llamada NO se hace;</li>
 *   <li>la reserva cuenta SIEMPRE, aunque la prediccion falle, se cancele o el POST reviente: nunca se devuelve;</li>
 *   <li>{@link #resultado}: despues, una linea sin importe con el id de la prediccion y como acabo.</li>
 * </ul>
 * Es una reserva, no el coste: Replicate no da los tokens de gpt-5-mini; el coste facturado solo se ve en su panel.
 */
public final class LibroPresupuestoH2v2 {

    /** El total autorizado para la evaluacion de H2-v2 (opcion A). Es el de un libro sin cabecera (el de H2-v2). */
    public static final double TOPE_TOTAL_USD = 7.50;
    public static final double USD_ENTRADA = 0.25, USD_SALIDA = 2.00;
    static final String CABECERA = "fecha_utc;tipo;etapa;caso;brazo;intento;reserva_usd;acumulado_usd;prediction_id;detalle";

    private final Path fichero;
    private final double topeTotal;

    /** El libro de H2-v2 (tope USD 7,50, sin cabecera de tope). */
    public LibroPresupuestoH2v2(Path fichero) {
        this(fichero, TOPE_TOTAL_USD);
    }

    /**
     * Un libro con su tope total. Un libro NUEVO apunta el tope en su primera linea ({@code # tope_total_usd=...}); uno
     * que ya existe exige el MISMO tope: el limite se fija al abrir el libro y no se puede subir despues (ni bajar para
     * que parezca que cabe). Un libro sin esa linea es el de H2-v2 y su tope es {@link #TOPE_TOTAL_USD}.
     */
    public LibroPresupuestoH2v2(Path fichero, double topeTotal) {
        this.fichero = fichero;
        this.topeTotal = topeTotal;
        try {
            Files.createDirectories(fichero.toAbsolutePath().getParent());
            if (!Files.exists(fichero)) {
                String cabecera = topeTotal == TOPE_TOTAL_USD ? "" : "# tope_total_usd="
                        + String.format(Locale.ROOT, "%.2f", topeTotal) + "\n";
                Files.writeString(fichero, cabecera + CABECERA + "\n", StandardCharsets.UTF_8);
            }
            var primera = Files.readAllLines(fichero, StandardCharsets.UTF_8).stream().findFirst().orElse("");
            double registrado = primera.startsWith("# tope_total_usd=")
                    ? Double.parseDouble(primera.substring("# tope_total_usd=".length()).trim()) : TOPE_TOTAL_USD;
            if (Math.abs(registrado - topeTotal) > 1e-9)
                throw new IllegalStateException("el libro " + fichero + " se abrio con tope " + registrado
                        + " y se pide " + topeTotal + ": el tope no se cambia");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public double topeTotal() {
        return topeTotal;
    }

    /** La reserva conservadora de una prediccion con {@code bytesEntrada} de prompt e instruccion de sistema. */
    public static double cota(long bytesEntrada, int maxCompletionTokens) {
        return (bytesEntrada + 256) / 1e6 * USD_ENTRADA + maxCompletionTokens / 1e6 * USD_SALIDA;
    }

    /** Lo reservado hasta ahora, en todas las ejecuciones (o solo en una etapa, si no es null). */
    public synchronized double reservado(String etapa) {
        try {
            double t = 0;
            for (var l : Files.readAllLines(fichero, StandardCharsets.UTF_8)) {
                var x = l.split(";", -1);
                if (l.startsWith("#") || x.length < 7 || !x[1].equals("RESERVA")) continue;
                if (etapa != null && !x[2].equals(etapa)) continue;
                t += Double.parseDouble(x[6]);
            }
            return t;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public double saldo() {
        return topeTotal - reservado(null);
    }

    /** Cabe {@code importe} en el tope total y en el de la etapa. */
    public synchronized boolean cabe(String etapa, double topeEtapa, double importe) {
        return reservado(null) + importe <= topeTotal + 1e-9 && reservado(etapa) + importe <= topeEtapa + 1e-9;
    }

    /** Apunta la reserva si cabe; si no, devuelve false y no apunta nada (la llamada no se debe hacer). */
    public synchronized boolean reservar(String etapa, double topeEtapa, String caso, String brazo, int intento,
                                         double cota, String detalle) {
        if (!cabe(etapa, topeEtapa, cota)) return false;
        double acumulado = reservado(null) + cota;
        anotar("RESERVA", etapa, caso, brazo, intento, cota, acumulado, "-", detalle);
        return true;
    }

    /** Como acabo la prediccion de una reserva (sin importe: la reserva ya se desconto y no se devuelve). */
    public synchronized void resultado(String etapa, String caso, String brazo, int intento, String predictionId,
                                       String detalle) {
        anotar("RESULTADO", etapa, caso, brazo, intento, 0, reservado(null), predictionId, detalle);
    }

    private void anotar(String tipo, String etapa, String caso, String brazo, int intento, double importe,
                        double acumulado, String predictionId, String detalle) {
        String linea = String.join(";", Instant.now().toString(), tipo, etapa, caso, brazo, String.valueOf(intento),
                String.format(Locale.ROOT, "%.6f", importe), String.format(Locale.ROOT, "%.6f", acumulado),
                predictionId == null ? "desconocido" : predictionId,
                detalle == null ? "" : detalle.replace(';', ',').replace('\n', ' ')) + "\n";
        // Escritura sincronizada en disco: si el contenedor se reinicia, la reserva ya consta.
        try (var o = new FileOutputStream(fichero.toFile(), true)) {
            o.write(linea.getBytes(StandardCharsets.UTF_8));
            o.getFD().sync();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
