package pe.smartcash.cash.gmailsync.domain.services;

/** Puerto hacia el endpoint OAuth2 de Google; la implementación real (HTTP) vive en
 * infrastructure.oauth. */
public interface GoogleOAuthPort {

  /** Intercambia el {@code code} del callback por el primer par de tokens. */
  OAuthTokens exchangeCode(String authorizationCode);

  /** Renueva un access token vencido usando el refresh token guardado. */
  OAuthTokens refresh(String refreshToken);

  /** URL de consentimiento a la que se redirige al usuario, con el {@code state} (CSRF)
   * ya embebido. */
  String buildAuthorizationUrl(String state);
}
