package main.web.services.fitsense.iam.infrastructure.authorization.sfs.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import main.web.services.fitsense.administration.infrastructure.audit.AdminAudit;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 403 con token valido pero sin el rol que pide la ruta (un USER en
 * /api/v1/admin/**). Mismo formato que el 401 de UnauthorizedRequestHandlerEntryPoint.
 */
@Component
public class ForbiddenRequestHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        if (request.getRequestURI().startsWith("/api/v1/admin/"))
            AdminAudit.denied(usuarioActual(), request.getMethod(), request.getRequestURI());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "timestamp", OffsetDateTime.now().toString(),
                "status", 403,
                "error", "Forbidden",
                "message", "No tienes acceso a este recurso."
        ));
    }

    private static Long usuarioActual() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof UserDetailsImpl u ? u.getUserId() : null;
    }
}
