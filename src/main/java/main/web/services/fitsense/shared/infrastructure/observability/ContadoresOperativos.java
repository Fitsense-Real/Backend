package main.web.services.fitsense.shared.infrastructure.observability;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * B5.1 (P1-01): contadores de sucesos operativos desde el arranque (correo enviado/fallido/descartado, inicios de
 * sesion fallidos, limites superados, cierres semanales sin plan). Solo cuentan: no guardan correos, usuarios ni
 * datos de salud. Los lee {@code IndicadoresOperativosService} para el panel y las alertas.
 * <p>
 * En memoria: un reinicio los vuelve a cero (las cifras que deben sobrevivir se calculan desde la base).
 */
@Component
public class ContadoresOperativos {

    public static final String CORREO_ENVIADO = "correo.enviado";
    public static final String CORREO_FALLIDO = "correo.fallido";
    public static final String CORREO_DESCARTADO = "correo.descartado";
    public static final String LOGIN_FALLIDO = "auth.login_fallido";
    public static final String LIMITE_SUPERADO = "auth.limite_superado";
    public static final String CICLO_SIN_PLAN = "ciclo.sin_plan";

    private final ConcurrentHashMap<String, AtomicLong> cuentas = new ConcurrentHashMap<>();

    public void sumar(String suceso) {
        cuentas.computeIfAbsent(suceso, k -> new AtomicLong()).incrementAndGet();
    }

    public long valor(String suceso) {
        var c = cuentas.get(suceso);
        return c == null ? 0 : c.get();
    }

    public Map<String, Long> todos() {
        var r = new TreeMap<String, Long>();
        for (var k : new String[]{CORREO_ENVIADO, CORREO_FALLIDO, CORREO_DESCARTADO, LOGIN_FALLIDO, LIMITE_SUPERADO,
                CICLO_SIN_PLAN}) r.put(k, valor(k));
        cuentas.forEach((k, v) -> r.put(k, v.get()));
        return r;
    }
}
