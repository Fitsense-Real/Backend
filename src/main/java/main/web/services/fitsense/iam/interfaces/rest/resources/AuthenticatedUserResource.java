package main.web.services.fitsense.iam.interfaces.rest.resources;

/**
 * {@code role} es informativo para la interfaz (Angular muestra el panel solo a
 * ADMIN). La autorizacion real la decide el backend en cada peticion.
 */
public record AuthenticatedUserResource(Long userId, String email, String fullName, String token,
                                        String role) {}
