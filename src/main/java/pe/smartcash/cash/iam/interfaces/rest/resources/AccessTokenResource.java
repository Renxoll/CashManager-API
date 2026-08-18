package pe.smartcash.cash.iam.interfaces.rest.resources;

import java.time.Instant;

public record AccessTokenResource(String accessToken, Instant expiresAt) {}
