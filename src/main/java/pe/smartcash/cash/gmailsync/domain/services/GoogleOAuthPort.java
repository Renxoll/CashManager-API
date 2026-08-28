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

  /** Email real de la cuenta de Google detrás del access token (requiere el scope
   * {@code email}/{@code openid}). {@code null} si Google no lo devuelve o falla la
   * llamada -- no es motivo para abortar la conexión, ver {@code GmailOAuthFlowServiceImpl}. */
  String fetchEmail(String accessToken);

  /** Revoca el token en Google (best-effort, ver {@code GmailConnectionCommandServiceImpl}). */
  void revoke(String token);
}
