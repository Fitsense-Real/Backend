package main.web.services.fitsense.planning.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * P-02: la accion explicita. {@code confirmNoPain} tiene que ser true: el usuario declara que ya no siente
 * molestias con ese ejercicio y pide que vuelva a considerarse. {@code comment} es opcional y libre; no se
 * pide ni se interpreta como una afirmacion medica.
 */
public record ReactivatePainExclusionResource(@NotNull Boolean confirmNoPain, @Size(max = 300) String comment) {}
