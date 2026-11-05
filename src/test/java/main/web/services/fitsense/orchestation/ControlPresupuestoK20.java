package main.web.services.fitsense.orchestation;

/**
 * Control de presupuesto del arnes K20 (P-H1, aprobada para el arnes de pruebas el 05/11/2026). Logica PURA, sin red,
 * para poder probar todos los casos sin Replicate. Replicate sigue NO autorizado: esta clase no hace llamadas.
 * <ul>
 *   <li>{@link #puedeLlamar}: antes de cada prediccion, tope de predicciones y cota de gasto con la reserva de peor
 *       caso por prediccion (gasto medido + reserva <= tope, y n x reserva <= tope).</li>
 *   <li>{@link #contabilizar}: despues de cada prediccion, coste medido por tokens o reserva, y si se bloquea.</li>
 * </ul>
 */
final class ControlPresupuestoK20 {

    /** Estado FINAL conocido de la prediccion en el proveedor (GET /v1/predictions/{id}). */
    enum EstadoProveedor { SUCCEEDED, FAILED, CANCELED, EN_CURSO, DESCONOCIDO }

    record Contabilidad(double usd, String estado, String bloqueo) {}

    static final String BLOQUEAR = "BLOQUEAR";
    static final String RESERVA_SIN_BLOQUEO = "RESERVA_SIN_BLOQUEO";

    private ControlPresupuestoK20() {}

    static boolean puedeLlamar(int nSiguiente, int maxLlamadas, double usdMedido, double reserva, double maxUsd,
                               String bloqueo) {
        if (bloqueo != null) return false;
        if (nSiguiente > maxLlamadas) return false;
        if (nSiguiente * reserva > maxUsd + 1e-9) return false;
        return usdMedido + reserva <= maxUsd + 1e-9;
    }

    /**
     * @param politica       BLOQUEAR (protocolo congelado) o RESERVA_SIN_BLOQUEO (P-H1)
     * @param fallidaBackend el backend recibio un error (fallo, cancelacion o tiempo agotado) y no una respuesta
     * @param estado         estado final en el proveedor tras consultarlo (y, si seguia en curso, tras pedir su
     *                       cancelacion, que no consume tokens)
     * @param tokensEntrada  null si las metricas no se pudieron leer o estan incompletas
     */
    static Contabilidad contabilizar(String politica, boolean fallidaBackend, EstadoProveedor estado,
                                     Long tokensEntrada, Long tokensSalida, double reserva,
                                     double tarifaEntradaMillon, double tarifaSalidaMillon, String id) {
        if (tokensEntrada != null && tokensSalida != null && tokensEntrada >= 0 && tokensSalida >= 0) {
            double usd = tokensEntrada / 1e6 * tarifaEntradaMillon + tokensSalida / 1e6 * tarifaSalidaMillon;
            if (usd > reserva)
                return new Contabilidad(usd, "RESERVA_SUPERADA",
                        "la prediccion " + id + " costo " + usd + " USD, mas que la reserva: no se hacen mas llamadas");
            return new Contabilidad(usd, "MEDIDO", null);
        }
        // Sin metricas: se carga SIEMPRE la reserva (peor caso acotado).
        boolean terminalSinRespuesta = fallidaBackend
                && (estado == EstadoProveedor.FAILED || estado == EstadoProveedor.CANCELED);
        if (RESERVA_SIN_BLOQUEO.equals(politica) && terminalSinRespuesta)
            return new Contabilidad(reserva, "FALLIDA_SIN_METRICAS_SE_CARGA_LA_RESERVA_SIN_BLOQUEO", null);
        return new Contabilidad(reserva, "SIN_METRICAS_SE_CARGA_LA_RESERVA",
                "sin metricas de la prediccion " + id + " (estado " + estado + "): no se hacen mas llamadas");
    }
}
