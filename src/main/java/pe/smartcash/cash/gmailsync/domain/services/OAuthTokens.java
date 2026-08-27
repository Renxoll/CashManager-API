package pe.smartcash.cash.gmailsync.domain.services;

import java.time.Instant;

/** {@code refreshToken} puede venir {@code null}: Google solo lo reemite en el primer
 * consentimiento (o si se fuerza {@code prompt=consent}), no en cada refresh. */
public record OAuthTokens(String accessToken, String refreshToken, Instant accessTokenExpiresAt) {}
