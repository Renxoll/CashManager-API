package pe.smartcash.cash.gmailsync.infrastructure.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.google-oauth")
public record GoogleOAuthProperties(String clientId, String clientSecret, String redirectUri, String scope) {}
