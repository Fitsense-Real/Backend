package main.web.services.fitsense.iam.infrastructure.authorization.sfs.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Con {@code fitsense.security.require-https=true} (perfil prod), rechaza con 403 cualquier peticion que no haya
 * llegado por HTTPS, sin procesarla: ni la autentica ni la pasa al controlador. Detras del proxy de Railway, "llego por
 * HTTPS" lo decide {@code X-Forwarded-Proto}, que Tomcat solo acepta de un proxy de confianza
 * ({@code server.forward-headers-strategy=native}, {@code server.tomcat.remoteip.internal-proxies}).
 * <p>
 * No redirige: una app o un script que reciba un 301 podria repetir la peticion, con el token, por HTTP. Exento:
 * {@code /actuator/health}, que Railway consulta dentro de su red sin TLS y no devuelve datos.
 */
class HttpsRequeridoFilter extends OncePerRequestFilter {

    static final String CUERPO = "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"Esta API solo acepta HTTPS.\","
            + "\"details\":[\"HTTPS_REQUERIDO\"]}";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.isSecure() || "/actuator/health".equals(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(CUERPO);
    }
}
