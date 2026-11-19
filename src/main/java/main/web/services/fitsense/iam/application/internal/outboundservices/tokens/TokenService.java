package main.web.services.fitsense.iam.application.internal.outboundservices.tokens;

public interface TokenService {
    /** Token con version de credenciales 0 (la inicial). */
    default String generateToken(Long userId, String email) {
        return generateToken(userId, email, 0);
    }

    /** @param tokenVersion {@code users.token_version} vigente: cambiar la contrasena la sube y revoca este token */
    String generateToken(Long userId, String email, int tokenVersion);

    /** Version de credenciales del token; 0 si no la lleva (tokens anteriores a V61). */
    int extractTokenVersion(String token);
    Long extractUserId(String token);
    boolean validateToken(String token);
    String getBearerTokenFrom(jakarta.servlet.http.HttpServletRequest request);
}
