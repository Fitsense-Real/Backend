package main.web.services.fitsense.catalog.domain.model.queries;

import java.util.Set;

/** O20 (opcion A): las variantes de la MISMA familia de movimiento de cada ejercicio pedido (sin incluirlo a el). */
public record GetEquivalentVariantsQuery(Set<Long> exerciseIds) {}
