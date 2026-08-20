package pe.smartcash.cash.advisor.interfaces.rest.resources;

import java.time.Instant;

public record ChatMessageResponse(String reply, Instant timestamp) {}
