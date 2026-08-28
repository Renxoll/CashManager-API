package pe.smartcash.cash.gmailsync.infrastructure.oauth;

import io.sentry.Sentry;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import pe.smartcash.cash.gmailsync.domain.services.GoogleOAuthPort;
import pe.smartcash.cash.gmailsync.domain.services.OAuthTokens;

@Slf4j
@Component
class GoogleOAuthPortAdapter implements GoogleOAuthPort {

  private static final String AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
  private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
  private static final String USERINFO_ENDPOINT = "https://www.googleapis.com/oauth2/v3/userinfo";
  private static final String REVOKE_ENDPOINT = "https://oauth2.googleapis.com/revoke";

  private final RestClient restClient;
  private final GoogleOAuthProperties properties;

  GoogleOAuthPortAdapter(RestClient.Builder restClientBuilder, GoogleOAuthProperties properties) {
    this.restClient = restClientBuilder.build();
    this.properties = properties;
  }

  /**
   * {@code access_type=offline} pide un refresh token (si no, la sesión solo dura lo que
   * dura el access token); {@code prompt=consent} fuerza a Google a reemitirlo siempre,
   * necesario en modo Testing donde re-conectar es algo que va a pasar cada 7 días.
   */
  @Override
  public String buildAuthorizationUrl(String state) {
    return UriComponentsBuilder.fromUriString(AUTH_ENDPOINT)
        .queryParam("client_id", properties.clientId())
        .queryParam("redirect_uri", properties.redirectUri())
        .queryParam("response_type", "code")
        .queryParam("scope", properties.scope())
        .queryParam("access_type", "offline")
        .queryParam("prompt", "consent")
        .queryParam("state", state)
        .build()
        .toUriString();
  }

  @Override
  public OAuthTokens exchangeCode(String authorizationCode) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("code", authorizationCode);
    form.add("client_id", properties.clientId());
    form.add("client_secret", properties.clientSecret());
    form.add("redirect_uri", properties.redirectUri());
    form.add("grant_type", "authorization_code");
    return callTokenEndpoint(form);
  }

  @Override
  public OAuthTokens refresh(String refreshToken) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("refresh_token", refreshToken);
    form.add("client_id", properties.clientId());
    form.add("client_secret", properties.clientSecret());
    form.add("grant_type", "refresh_token");
    return callTokenEndpoint(form);
  }

  private OAuthTokens callTokenEndpoint(MultiValueMap<String, String> form) {
    GoogleTokenResponse response = restClient.post().uri(TOKEN_ENDPOINT).body(form).retrieve().body(GoogleTokenResponse.class);
    if (response == null || response.accessToken() == null) {
      throw new IllegalStateException("Google no devolvió un access_token válido");
    }
    Instant expiresAt = Instant.now().plusSeconds(response.expiresIn());
    return new OAuthTokens(response.accessToken(), response.refreshToken(), expiresAt);
  }

  /**
   * No lanza si falla -- {@code null} es una respuesta válida acá (ver el contrato en {@code
   * GoogleOAuthPort}): el email es un dato secundario para mostrar en el panel de cuentas,
   * no algo que deba tumbar el flujo de conexión si Google no lo devuelve.
   */
  @Override
  public String fetchEmail(String accessToken) {
    try {
      GoogleUserInfoResponse response =
          restClient.get().uri(USERINFO_ENDPOINT).header("Authorization", "Bearer " + accessToken).retrieve().body(GoogleUserInfoResponse.class);
      return response != null ? response.email() : null;
    } catch (RestClientException e) {
      log.warn("No se pudo obtener el email de la cuenta de Google conectada: {}", e.getMessage());
      Sentry.captureException(e, scope -> scope.setTag("component", "gmail-oauth-userinfo"));
      return null;
    }
  }

  @Override
  public void revoke(String token) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("token", token);
    restClient.post().uri(REVOKE_ENDPOINT).body(form).retrieve().toBodilessEntity();
  }
}
