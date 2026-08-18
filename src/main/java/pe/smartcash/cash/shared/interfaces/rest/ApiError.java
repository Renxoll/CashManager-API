package pe.smartcash.cash.shared.interfaces.rest;

import java.time.Instant;

public record ApiError(Instant timestamp, int status, String error, String message) {}
