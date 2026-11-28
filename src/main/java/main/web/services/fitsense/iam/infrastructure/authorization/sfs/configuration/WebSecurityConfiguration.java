package main.web.services.fitsense.iam.infrastructure.authorization.sfs.configuration;

import main.web.services.fitsense.iam.application.internal.outboundservices.tokens.TokenService;
import main.web.services.fitsense.iam.domain.model.valueobjects.UserRole;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.pipeline.BearerAuthorizationRequestFilter;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.pipeline.ForbiddenRequestHandler;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.pipeline.UnauthorizedRequestHandlerEntryPoint;
import main.web.services.fitsense.iam.infrastructure.hashing.bcrypt.BCryptHashingService;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Autorizacion por rutas, denegando por defecto:
 * <ul>
 *   <li>{@code /api/v1/auth/**} y {@code /actuator/health}: publicas.</li>
 *   <li>{@code /api/v1/admin/**}: ADMIN.</li>
 *   <li>{@code /api/v1/cycle/run-for-me}: nadie, salvo con
 *       {@code fitsense.cycle.manual-run.enabled=true} (solo desarrollo y pruebas).</li>
 *   <li>El resto de {@code /api/v1/**}: USER, y ADMIN por la jerarquia. Que cada
 *       USER vea solo lo suyo lo garantizan los servicios, que filtran por el
 *       usuario del token.</li>
 *   <li>La documentacion interactiva: solo con {@code fitsense.api-docs.enabled=true}.</li>
 *   <li>Cualquier otra ruta: denegada.</li>
 * </ul>
 * Las tareas programadas no pasan por aqui: no son peticiones HTTP.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfiguration {

    private static final String[] PUBLIC_PATHS = {
            "/api/v1/auth/**",
            // Solicitud web de eliminacion: la persona no tiene la app ni recuerda la contrasena. La propiedad del
            // correo se verifica con un token de un solo uso (SolicitudEliminacionWebService).
            "/api/v1/account-deletion/**",
            // B2.2: documentos legales vigentes; la app los muestra antes de registrarse (solo publicados).
            "/api/v1/legal-documents",
            "/actuator/health",
            // El despacho interno de errores: sin esto un 404 o 500 de un usuario
            // anonimo se volveria 401 y ocultaria el error real.
            "/error"
    };

    private static final String[] API_DOCS_PATHS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private static final String MANUAL_CYCLE_PATH = "/api/v1/cycle/run-for-me";

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final BCryptHashingService hashingService;
    private final UnauthorizedRequestHandlerEntryPoint unauthorizedRequestHandlerEntryPoint;
    private final ForbiddenRequestHandler forbiddenRequestHandler;
    private final List<String> corsOrigins;
    private final boolean apiDocsEnabled;
    private final boolean manualCycleEnabled;
    private final boolean requireHttps;

    public WebSecurityConfiguration(UserRepository userRepository,
                                    TokenService tokenService,
                                    BCryptHashingService hashingService,
                                    UnauthorizedRequestHandlerEntryPoint entryPoint,
                                    ForbiddenRequestHandler forbiddenRequestHandler,
                                    @Value("${fitsense.cors.allowed-origins:}") String corsOrigins,
                                    @Value("${fitsense.api-docs.enabled:false}") boolean apiDocsEnabled,
                                    @Value("${fitsense.cycle.manual-run.enabled:false}") boolean manualCycleEnabled,
                                    @Value("${fitsense.security.require-https:false}") boolean requireHttps) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.hashingService = hashingService;
        this.unauthorizedRequestHandlerEntryPoint = entryPoint;
        this.forbiddenRequestHandler = forbiddenRequestHandler;
        this.corsOrigins = CorsOrigins.parse(corsOrigins);
        this.apiDocsEnabled = apiDocsEnabled;
        this.manualCycleEnabled = manualCycleEnabled;
        this.requireHttps = requireHttps;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return hashingService;
    }

    /** ADMIN incluye a USER, en las rutas y en {@code @PreAuthorize}. */
    @Bean
    public static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role(UserRole.ADMIN.name()).implies(UserRole.USER.name())
                .build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(unauthorizedRequestHandlerEntryPoint)
                        .accessDeniedHandler(forbiddenRequestHandler))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Cabeceras de seguridad (PREPARACION_HTTPS_RAILWAY.md). Ademas de las de Spring Security por omision
                // (nosniff, X-Frame-Options DENY, Cache-Control no-store): HSTS de un ano, que solo se envia en
                // peticiones HTTPS (detras del proxy de Railway, con server.forward-headers-strategy=native); sin
                // Referer hacia terceros; y una CSP que prohibe todo contenido activo, porque la API solo devuelve
                // JSON (con Swagger UI activado no se aplica, porque la bloquearia).
                .headers(headers -> {
                    headers.httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true)
                            .maxAgeInSeconds(31_536_000));
                    headers.referrerPolicy(referrer -> referrer.policy(
                            org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER));
                    if (!apiDocsEnabled)
                        headers.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"));
                })
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(PUBLIC_PATHS).permitAll();
                    if (apiDocsEnabled) auth.requestMatchers(API_DOCS_PATHS).permitAll();
                    auth.requestMatchers("/api/v1/admin/**").hasRole(UserRole.ADMIN.name());
                    if (manualCycleEnabled) auth.requestMatchers(MANUAL_CYCLE_PATH).hasRole(UserRole.USER.name());
                    else auth.requestMatchers(MANUAL_CYCLE_PATH).denyAll();
                    auth.requestMatchers("/api/v1/**").hasRole(UserRole.USER.name());
                    auth.anyRequest().denyAll();
                });

        // Se instancia aqui y NO se declara como @Bean: un filtro que ademas es
        // bean lo registra tambien el contenedor de servlets, con lo que cada
        // peticion pasaria dos veces por la validacion del token.
        http.addFilterBefore(
                new BearerAuthorizationRequestFilter(tokenService, userRepository),
                UsernamePasswordAuthenticationFilter.class);
        // En produccion, ninguna peticion de la API por HTTP en claro (el token viajaria sin cifrar). El health check
        // de Railway llega al contenedor por la red interna, sin TLS: queda exento.
        if (requireHttps)
            http.addFilterBefore(new HttpsRequeridoFilter(), BearerAuthorizationRequestFilter.class);
        return http.build();
    }

    /**
     * Solo los origenes exactos de FITSENSE_CORS_ORIGINS. Sin credenciales de
     * navegador: la autenticacion va en la cabecera Authorization (Bearer), no en
     * cookies, asi que {@code allowCredentials} no hace falta y queda en false.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var source = new UrlBasedCorsConfigurationSource();
        if (corsOrigins.isEmpty()) return source;

        var config = new CorsConfiguration();
        config.setAllowedOrigins(corsOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
