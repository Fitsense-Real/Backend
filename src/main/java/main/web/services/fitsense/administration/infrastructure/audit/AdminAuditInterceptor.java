package main.web.services.fitsense.administration.infrastructure.audit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Una linea de auditoria por cada peticion a /api/v1/admin/** que llego al
 * controlador (las rechazadas antes, sin rol, las registra ForbiddenRequestHandler).
 */
public class AdminAuditInterceptor implements HandlerInterceptor {

    private static final String INICIO = AdminAuditInterceptor.class.getName() + ".inicio";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(INICIO, System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        var inicio = request.getAttribute(INICIO);
        long ms = inicio == null ? -1 : (System.nanoTime() - (Long) inicio) / 1_000_000;
        AdminAudit.request(usuarioActual(), request.getMethod(), request.getRequestURI(),
                response.getStatus(), ms);
    }

    static Long usuarioActual() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof UserDetailsImpl u ? u.getUserId() : null;
    }
}
