package main.web.services.fitsense.shared.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * P1-06 (auditoria de preproduccion): las guardas de seguridad de los entrenamientos y de la plataforma no pueden
 * relajarse en silencio. Varias se apagan con una variable de entorno, y sus valores por omision en el codigo son mas
 * laxos que los aprobados en {@code application.properties} (p. ej. {@code fitsense.dose.enforcement} vale OBSERVE en
 * el codigo y ENFORCE en la configuracion). Una variable mal puesta en Railway las desactivaria sin aviso.
 * <p>
 * Al arrancar se registra el valor efectivo de cada una ({@code GUARDAS_SEGURIDAD}). Con el perfil {@code prod}, si
 * alguna no tiene su valor aprobado, <b>la aplicacion no arranca</b>, salvo que se declare a proposito en
 * {@code fitsense.security.guard-overrides} (lista de claves separadas por comas): entonces arranca con un WARN por
 * cada una. Fuera de {@code prod} solo se registra (las pruebas y los laboratorios usan otras combinaciones).
 * <p>
 * No cambia ninguna regla: solo comprueba que la configuracion aprobada es la que se esta usando.
 */
@Configuration
public class GuardasDeSeguridadEnProduccion {

    private static final Logger log = LoggerFactory.getLogger(GuardasDeSeguridadEnProduccion.class);

    /** Clave -> valor aprobado (el de application.properties y application-prod.properties a 27/11/2026). */
    public static final Map<String, String> APROBADAS;

    static {
        var m = new LinkedHashMap<String, String>();
        m.put("fitsense.dose.enforcement", "ENFORCE");
        m.put("fitsense.capability.enforcement", "ENFORCE");
        m.put("fitsense.coverage.enforcement", "ENFORCE_AI_ONLY");
        m.put("fitsense.adaptation.difficulty-guard", "preferencia");
        m.put("fitsense.adaptation.pain-localized-removal", "on");
        m.put("fitsense.catalogo.contencion-conservadora", "true");
        m.put("fitsense.security.require-https", "true");
        m.put("fitsense.logging.sensitive-detail", "false");
        m.put("fitsense.api-docs.enabled", "false");
        m.put("fitsense.cycle.manual-run.enabled", "false");
        APROBADAS = java.util.Collections.unmodifiableMap(m);
    }

    /** Valor efectivo cuando la propiedad no esta definida: el del codigo. */
    private static final Map<String, String> POR_OMISION_EN_CODIGO = Map.of(
            "fitsense.dose.enforcement", "OBSERVE",
            "fitsense.capability.enforcement", "ENFORCE",
            "fitsense.coverage.enforcement", "OBSERVE",
            "fitsense.adaptation.difficulty-guard", "off",
            "fitsense.adaptation.pain-localized-removal", "off",
            "fitsense.catalogo.contencion-conservadora", "true",
            "fitsense.security.require-https", "false",
            "fitsense.logging.sensitive-detail", "false",
            "fitsense.api-docs.enabled", "false",
            "fitsense.cycle.manual-run.enabled", "false");

    public GuardasDeSeguridadEnProduccion(Environment entorno) {
        comprobar(entorno);
    }

    /** Devuelve las guardas que difieren del valor aprobado (clave -> valor efectivo). */
    static Map<String, String> comprobar(Environment entorno) {
        var efectivos = new TreeMap<String, String>();
        var distintas = new TreeMap<String, String>();
        APROBADAS.forEach((clave, aprobado) -> {
            String v = entorno.getProperty(clave, POR_OMISION_EN_CODIGO.get(clave));
            v = v == null ? "" : v.trim();
            efectivos.put(clave, v);
            if (!aprobado.toLowerCase(Locale.ROOT).equals(v.toLowerCase(Locale.ROOT))) distintas.put(clave, v);
        });
        boolean prod = entorno.matchesProfiles("prod");
        log.info("GUARDAS_SEGURIDAD perfil_prod={} {}", prod, efectivos);
        if (distintas.isEmpty()) return distintas;

        Set<String> declaradas = Arrays.stream(entorno.getProperty("fitsense.security.guard-overrides", "")
                .split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
        var sinDeclarar = new TreeMap<String, String>();
        distintas.forEach((clave, v) -> {
            if (declaradas.contains(clave))
                log.warn("GUARDA_SEGURIDAD_RELAJADA {}={} (aprobado: {}), declarada en fitsense.security.guard-overrides",
                        clave, v, APROBADAS.get(clave));
            else sinDeclarar.put(clave, v);
        });
        if (prod && !sinDeclarar.isEmpty())
            throw new IllegalStateException("GUARDAS DE SEGURIDAD NO APROBADAS con el perfil prod: " + sinDeclarar
                    + ". Valores aprobados: " + APROBADAS + ". Si es a proposito, declaralas en"
                    + " fitsense.security.guard-overrides.");
        if (!sinDeclarar.isEmpty())
            log.info("GUARDAS_SEGURIDAD distintas de las aprobadas (fuera de prod, solo se registra): {}", sinDeclarar);
        return distintas;
    }
}
