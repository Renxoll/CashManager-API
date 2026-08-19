package pe.smartcash.cash.iam.domain.services;

import java.time.Instant;

public record RefreshToken(String value, Instant expiresAt) {}
