package pe.smartcash.cash.iam.domain.services;

import java.time.Instant;

public record AccessToken(String value, Instant expiresAt) {}
