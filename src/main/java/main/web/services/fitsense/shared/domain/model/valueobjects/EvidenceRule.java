package main.web.services.fitsense.shared.domain.model.valueobjects;

import java.math.BigDecimal;

/**
 * V30. Cuando una semana tiene evidencia suficiente para decidir una
 * adaptacion. Una sola regla, en un solo sitio.
 * <p>
 * VIVE EN SHARED desde el 17/10/2026. Nacio en analytics, pero la usan tambien
 * el modulo de adaptacion -que es quien decide- y la vista que cruza entre los
 * dos. Dejarla en analytics obligaba a adaptacion a importar del dominio de
 * otro modulo, saltandose la regla de que los modulos se hablan por su ACL; la
 * alternativa era volver a escribir la condicion en adaptacion, que es
 * exactamente lo que esta clase existe para evitar, y de hecho ya paso una vez.
 * <p>
 * Existe porque la condicion la necesitan tres clases -el resultado del
 * calculo, la entidad persistida y la vista que cruza al modulo de
 * adaptacion- y escribirla tres veces es como se desincronizan. De hecho ya
 * estuvo a punto: la primera version comprobaba solo la cobertura y, con una
 * cobertura minima configurada al 0 %, una semana con CERO registros habria
 * pasado el umbral.
 * <p>
 * No confundir con <em>medible</em>. Que una semana sea medible es que llego a
 * generarse y tiene adherencia; que tenga evidencia es que una parte suficiente
 * de ella se registro. Lo primero lo comprueba cada clase por su cuenta, porque
 * lo sabe de distinta forma; lo segundo es esto.
 * <p>
 * El umbral NO vive aqui: entra como parametro porque es una decision
 * metodologica provisional, pendiente de aprobacion, y no debe quedar escrita
 * en el dominio.
 */
public final class EvidenceRule {

    private EvidenceRule() {}

    /**
     * @param observedAdherencePct    cumplimiento sobre lo que tiene registro.
     *                                NULL cuando no hay ni un registro, y por
     *                                eso su ausencia basta para negar evidencia
     *                                por mucho que el umbral sea 0.
     * @param registrationCoveragePct que parte de la semana tiene registro.
     * @param minimumCoveragePct      umbral exigido, de la configuracion.
     */
    public static boolean enoughEvidence(BigDecimal observedAdherencePct,
                                         BigDecimal registrationCoveragePct,
                                         double minimumCoveragePct) {
        return observedAdherencePct != null
                && registrationCoveragePct != null
                && registrationCoveragePct.doubleValue() >= minimumCoveragePct;
    }
}
