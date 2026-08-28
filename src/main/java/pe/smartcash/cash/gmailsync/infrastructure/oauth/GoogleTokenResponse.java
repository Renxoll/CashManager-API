package pe.smartcash.cash.gmailsync.infrastructure.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;

/** DTO de cable de {@code https://oauth2.googleapis.com/token}. {@code refreshToken} viene
 * null en la mayoría de los refresh (Google solo lo reemite en el consentimiento inicial). */
record GoogleTokenResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("refresh_token") String refreshToken,
    @JsonProperty("expires_in") long expiresIn,
    @JsonProperty("token_type") String tokenType) {}
