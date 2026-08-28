package pe.smartcash.cash.gmailsync.infrastructure.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AES-256-GCM para cifrar los tokens OAuth de Gmail en reposo -- nunca se persisten en texto
 * plano (ver GmailConnectionEntityMapper). El IV es aleatorio en cada llamada y viaja
 * concatenado al principio del ciphertext: GCM no necesita que el IV sea secreto, solo
 * único por clave, así que no hace falta guardarlo aparte.
 */
@Component
public class TokenCipher {

  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int IV_LENGTH_BYTES = 12;
  private static final int TAG_LENGTH_BITS = 128;

  private final SecretKeySpec key;
  private final SecureRandom secureRandom = new SecureRandom();

  TokenCipher(@Value("${app.gmail-sync.token-encryption-key}") String base64Key) {
    byte[] decoded = Base64.getDecoder().decode(base64Key);
    if (decoded.length != 32) {
      throw new IllegalStateException(
          "app.gmail-sync.token-encryption-key debe decodificar a 32 bytes (AES-256); tiene " + decoded.length);
    }
    this.key = new SecretKeySpec(decoded, "AES");
  }

  public String encrypt(String plaintext) {
    try {
      byte[] iv = new byte[IV_LENGTH_BYTES];
      secureRandom.nextBytes(iv);
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
      byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
      byte[] combined = new byte[iv.length + ciphertext.length];
      System.arraycopy(iv, 0, combined, 0, iv.length);
      System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
      return Base64.getEncoder().encodeToString(combined);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("No se pudo cifrar el token", e);
    }
  }

  public String decrypt(String encoded) {
    try {
      byte[] combined = Base64.getDecoder().decode(encoded);
      byte[] iv = new byte[IV_LENGTH_BYTES];
      byte[] ciphertext = new byte[combined.length - IV_LENGTH_BYTES];
      System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTES);
      System.arraycopy(combined, IV_LENGTH_BYTES, ciphertext, 0, ciphertext.length);
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
      return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("No se pudo descifrar el token", e);
    }
  }
}
