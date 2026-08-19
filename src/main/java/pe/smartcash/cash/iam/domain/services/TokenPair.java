package pe.smartcash.cash.iam.domain.services;

/** Lo que devuelve tanto sign-in como /refresh: un access token de vida corta y su refresh token asociado. */
public record TokenPair(AccessToken accessToken, RefreshToken refreshToken) {}
