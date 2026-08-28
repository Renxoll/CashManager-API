package pe.smartcash.cash.gmailsync.infrastructure.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;

/** DTO de cable de {@code https://www.googleapis.com/oauth2/v3/userinfo}. Requiere el scope
 * {@code email}/{@code openid} (ver {@code app.google-oauth.scope}). */
record GoogleUserInfoResponse(String email, @JsonProperty("email_verified") Boolean emailVerified, String sub) {}
