package pe.smartcash.cash.iam.interfaces.rest.resources;

import java.time.Instant;

public record TokenPairResource(
    String accessToken, Instant accessTokenExpiresAt, String refreshToken, Instant refreshTokenExpiresAt) {}
