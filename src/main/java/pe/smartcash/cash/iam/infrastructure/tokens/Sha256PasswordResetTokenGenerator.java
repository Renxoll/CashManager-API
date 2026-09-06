package pe.smartcash.cash.iam.infrastructure.tokens;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;
import pe.smartcash.cash.iam.domain.services.PasswordResetTokenGenerator;

/**
 * Token crudo = 32 bytes de {@link SecureRandom} en base64url sin padding (~43 chars, va en
 * el {@code ?token=} del enlace). Lo que se guarda es su SHA-256 en hex: alcanza para un
 * lookup por igualdad y, como el token crudo tiene 256 bits de entropía, no hace falta
 * salt ni un hash lento tipo BCrypt (no hay nada que "adivinar" por fuerza bruta).
 */
@Component
class Sha256PasswordResetTokenGenerator implements PasswordResetTokenGenerator {

  private static final int RAW_TOKEN_BYTES = 32;

  private final SecureRandom secureRandom = new SecureRandom();

  @Override
  public GeneratedToken generate() {
    byte[] raw = new byte[RAW_TOKEN_BYTES];
    secureRandom.nextBytes(raw);
    String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    return new GeneratedToken(rawToken, hash(rawToken));
  }

  @Override
  public String hash(String rawToken) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      // SHA-256 es parte del JRE estándar: si falta, algo mucho más grave está roto.
      throw new IllegalStateException("SHA-256 no disponible en esta JVM", e);
    }
  }
}
