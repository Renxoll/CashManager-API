package pe.smartcash.cash.iam.infrastructure.security;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import pe.smartcash.cash.iam.domain.services.TokenBlacklistService;

/**
 * TTL igual al tiempo de vida restante del token (calculado en el caso de uso, ver
 * {@code IamCommandServiceImpl.handle(LogoutCommand)}): la entrada desaparece sola de Redis
 * exactamente cuando el token habría expirado de todos modos, sin dejar basura acumulándose.
 */
@Component
class RedisTokenBlacklistService implements TokenBlacklistService {

  private static final String KEY_PREFIX = "blacklisted-token:";

  private final StringRedisTemplate redisTemplate;

  RedisTokenBlacklistService(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public void blacklist(String accessTokenValue, Duration ttl) {
    redisTemplate.opsForValue().set(key(accessTokenValue), "revoked", ttl);
  }

  @Override
  public boolean isBlacklisted(String accessTokenValue) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(key(accessTokenValue)));
  }

  private String key(String tokenValue) {
    return KEY_PREFIX + tokenValue;
  }
}
