package main.web.services.fitsense.administration.interfaces.rest.resources;

import java.util.List;

public record ParticipantPageResource(int page, int size, long total, List<ParticipantSummaryResource> items) {}
