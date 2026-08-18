package pe.smartcash.cash.iam.infrastructure.tokens;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.smartcash.cash.iam.domain.model.valueobjects.UserId;
import pe.smartcash.cash.iam.domain.services.AccessToken;
import pe.smartcash.cash.iam.domain.services.TokenService;

/**
 * Token propio firmado con HMAC-SHA256, sin librerías externas de JWT:
 * {@code base64url(userId|expiresAtEpochMillis).base64url(firma)}. La firma se codifica en
 * base64url directamente desde los bytes crudos que devuelve {@link Mac#doFinal()} — nunca
 * se le aplica una segunda pasada de Base64 (ese bug hace que {@code validate} jamás vuelva
 * a reproducir la misma firma, porque el string ya no es el que se firmó). Vive detrás del
 * puerto {@link TokenService}, así que migrar a otra librería más adelante es un adaptador
 * nuevo, no un cambio de dominio.
 */
@Component
class HmacTokenServiceAdapter implements TokenService {

  private static final String ALGORITHM = "HmacSHA256";

  private final SecretKeySpec key;
  private final Duration ttl;

  HmacTokenServiceAdapter(
      @Value("${iam.token.secret}") String secret, @Value("${iam.token.expiration-hours:2}") long expirationHours) {
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException("iam.token.secret es obligatorio");
    }
    this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    this.ttl = Duration.ofHours(expirationHours);
  }

  @Override
  public AccessToken issue(UserId userId) {
    Instant expiresAt = Instant.now().plus(ttl);
    String payload = userId.value() + "|" + expiresAt.toEpochMilli();
    String token = encode(payload.getBytes(StandardCharsets.UTF_8)) + "." + encode(sign(payload));
    return new AccessToken(token, expiresAt);
  }

  @Override
  public Optional<UserId> validate(String tokenValue) {
    String[] parts = tokenValue.split("\\.", 2);
    if (parts.length != 2) {
      return Optional.empty();
    }

    byte[] payloadBytes;
    byte[] receivedSignature;
    try {
      payloadBytes = decode(parts[0]);
      receivedSignature = decode(parts[1]);
    } catch (IllegalArgumentException malformedBase64) {
      return Optional.empty();
    }

    String payload = new String(payloadBytes, StandardCharsets.UTF_8);
    byte[] expectedSignature = sign(payload);
    // MessageDigest.isEqual compara en tiempo constante: no delata, vía cuánto tarda la
    // comparación, en qué byte empieza a diferir la firma recibida de la esperada.
    if (!MessageDigest.isEqual(expectedSignature, receivedSignature)) {
      return Optional.empty();
    }

    String[] payloadParts = payload.split("\\|", 2);
    if (payloadParts.length != 2) {
      return Optional.empty();
    }
    try {
      Instant expiresAt = Instant.ofEpochMilli(Long.parseLong(payloadParts[1]));
      if (Instant.now().isAfter(expiresAt)) {
        return Optional.empty();
      }
      return Optional.of(UserId.of(UUID.fromString(payloadParts[0])));
    } catch (RuntimeException malformed) {
      return Optional.empty();
    }
  }

  private byte[] sign(String payload) {
    try {
      Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(key);
      return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException("No se pudo firmar el token", e);
    }
  }

  private String encode(byte[] raw) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
  }

  private byte[] decode(String encoded) {
    return Base64.getUrlDecoder().decode(encoded);
  }
}
