package main.web.services.fitsense.analytics.domain.model.queries;

import java.time.LocalDate;

/** Lo que paso en cada sesion de una semana, para poder senalar la que fallo. */
public record GetSessionOutcomesQuery(Long userId, LocalDate weekStartDate) {}
