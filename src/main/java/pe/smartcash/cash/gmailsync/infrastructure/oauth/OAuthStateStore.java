package pe.smartcash.cash.gmailsync.infrastructure.oauth;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * CSRF del flujo OAuth: Google no puede recibir un Bearer token en el callback (es una
 * navegación de browser, no una llamada autenticada), así que el userId viaja indirecto acá
 * -- un state aleatorio de un solo uso, con TTL corto, que solo este backend puede resolver
 * de vuelta al userId real.
 */
@Component
public class OAuthStateStore {

  private static final Duration TTL = Duration.ofMinutes(10);
  private static final String KEY_PREFIX = "gmail-oauth-state:";

  private final StringRedisTemplate redisTemplate;

  public OAuthStateStore(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public String issue(String userId) {
    String state = UUID.randomUUID().toString();
    redisTemplate.opsForValue().set(KEY_PREFIX + state, userId, TTL);
    return state;
  }

  /** Un solo uso: se borra al leerse, así un state ya canjeado no sirve para un segundo
   * callback (replay). */
  public Optional<String> redeem(String state) {
    String key = KEY_PREFIX + state;
    String userId = redisTemplate.opsForValue().get(key);
    if (userId != null) {
      redisTemplate.delete(key);
    }
    return Optional.ofNullable(userId);
  }
}
