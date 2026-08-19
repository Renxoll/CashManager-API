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
import pe.smartcash.cash.iam.domain.services.RefreshToken;
import pe.smartcash.cash.iam.domain.services.TokenPair;
import pe.smartcash.cash.iam.domain.services.TokenService;

/**
 * HMAC-SHA256 nativo, sin librerías externas de JWT:
 * {@code base64url(type|userId|expiresAtEpochMillis).base64url(firma)}. El {@code type}
 * ("access"/"refresh") es la diferencia clave frente al esquema anterior de un solo token:
 * sin él, un access token de vida corta que un cliente recibe normalmente serviría también
 * como refresh token estructuralmente idéntico, permitiendo mintear pares nuevos indefinidamente
 * a partir de un access token robado sin necesitar jamás el refresh token real. {@code validate}
 * y {@code validateRefreshToken} exigen cada uno su propio tipo — un token del tipo equivocado
 * se trata igual que uno inválido.
 */
@Component
class HmacTokenServiceAdapter implements TokenService {

  private static final String ALGORITHM = "HmacSHA256";
  private static final String TYPE_ACCESS = "access";
  private static final String TYPE_REFRESH = "refresh";

  private final SecretKeySpec key;
  private final Duration accessTtl;
  private final Duration refreshTtl;

  HmacTokenServiceAdapter(
      @Value("${iam.token.secret}") String secret,
      @Value("${iam.token.expiration-minutes:15}") long accessExpirationMinutes,
      @Value("${iam.refresh-token.expiration-days:7}") long refreshExpirationDays) {
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException("iam.token.secret es obligatorio");
    }
    this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    this.accessTtl = Duration.ofMinutes(accessExpirationMinutes);
    this.refreshTtl = Duration.ofDays(refreshExpirationDays);
  }

  @Override
  public TokenPair issue(UserId userId) {
    Instant now = Instant.now();
    Instant accessExpiresAt = now.plus(accessTtl);
    Instant refreshExpiresAt = now.plus(refreshTtl);
    AccessToken accessToken = new AccessToken(buildToken(TYPE_ACCESS, userId, accessExpiresAt), accessExpiresAt);
    RefreshToken refreshToken = new RefreshToken(buildToken(TYPE_REFRESH, userId, refreshExpiresAt), refreshExpiresAt);
    return new TokenPair(accessToken, refreshToken);
  }

  @Override
  public Optional<UserId> validate(String accessTokenValue) {
    return decode(accessTokenValue).filter(decoded -> decoded.type().equals(TYPE_ACCESS)).map(DecodedToken::userId);
  }

  @Override
  public Optional<UserId> validateRefreshToken(String refreshTokenValue) {
    return decode(refreshTokenValue).filter(decoded -> decoded.type().equals(TYPE_REFRESH)).map(DecodedToken::userId);
  }

  @Override
  public Optional<Instant> expiresAt(String tokenValue) {
    return decode(tokenValue).map(DecodedToken::expiresAt);
  }

  private String buildToken(String type, UserId userId, Instant expiresAt) {
    String payload = type + "|" + userId.value() + "|" + expiresAt.toEpochMilli();
    return encode(payload.getBytes(StandardCharsets.UTF_8)) + "." + encode(sign(payload));
  }

  private Optional<DecodedToken> decode(String tokenValue) {
    String[] parts = tokenValue.split("\\.", 2);
    if (parts.length != 2) {
      return Optional.empty();
    }

    byte[] payloadBytes;
    byte[] receivedSignature;
    try {
      payloadBytes = decodeBase64(parts[0]);
      receivedSignature = decodeBase64(parts[1]);
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

    String[] payloadParts = payload.split("\\|", 3);
    if (payloadParts.length != 3) {
      return Optional.empty();
    }
    try {
      Instant expiresAt = Instant.ofEpochMilli(Long.parseLong(payloadParts[2]));
      if (Instant.now().isAfter(expiresAt)) {
        return Optional.empty();
      }
      UserId userId = UserId.of(UUID.fromString(payloadParts[1]));
      return Optional.of(new DecodedToken(payloadParts[0], userId, expiresAt));
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

  private byte[] decodeBase64(String encoded) {
    return Base64.getUrlDecoder().decode(encoded);
  }

  private record DecodedToken(String type, UserId userId, Instant expiresAt) {}
}
