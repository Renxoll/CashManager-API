package pe.smartcash.cash.shared.infrastructure.ratelimiting;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import java.time.Duration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cliente Lettuce dedicado a Bucket4j, separado del {@code LettuceConnectionFactory} que
 * autoconfigura spring-data-redis para el {@code StringRedisTemplate} (cache de comercios,
 * blacklist de tokens): bucket4j-redis necesita hablarle directo a una
 * {@link StatefulRedisConnection} con un codec de valor {@code byte[]} (así serializa el
 * estado del bucket), no la que expone spring-data-redis para sus propios templates. Se arma
 * a partir de {@link DataRedisProperties} (las mismas que ya resuelve Spring Boot desde
 * docker-compose en dev o desde {@code SPRING_DATA_REDIS_URL} en prod/Upstash) para no
 * duplicar configuración de conexión en dos lugares.
 */
@Configuration
class RateLimiterConfig {

  @Bean(destroyMethod = "shutdown")
  RedisClient bucket4jRedisClient(DataRedisProperties redisProperties) {
    return RedisClient.create(resolveUri(redisProperties));
  }

  @Bean(destroyMethod = "close")
  StatefulRedisConnection<String, byte[]> bucket4jRedisConnection(RedisClient redisClient) {
    return redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
  }

  @Bean
  ProxyManager<String> rateLimitProxyManager(StatefulRedisConnection<String, byte[]> connection) {
    return LettuceBasedProxyManager.builderFor(connection)
        // Sin esto las keys de bucket vivirían para siempre en Redis: se limpian solas poco
        // después de la última vez que se les pidió un token, igual que el TTL del cache de
        // comercios (ver RedisMerchantCategoryCacheAdapter).
        .withExpirationStrategy(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(5)))
        .build();
  }

  private RedisURI resolveUri(DataRedisProperties redisProperties) {
    // Prod (Upstash/Render) trae la URI completa con credenciales embebidas via
    // spring.data.redis.url (ver application-prod.properties); dev/test la arma sola desde
    // host/port porque spring-boot-docker-compose no puebla esa property, solo host/port.
    if (redisProperties.getUrl() != null && !redisProperties.getUrl().isBlank()) {
      return RedisURI.create(redisProperties.getUrl());
    }
    RedisURI.Builder builder = RedisURI.Builder.redis(redisProperties.getHost(), redisProperties.getPort());
    if (redisProperties.getPassword() != null) {
      builder.withPassword(redisProperties.getPassword().toCharArray());
    }
    if (redisProperties.getSsl().isEnabled()) {
      builder.withSsl(true);
    }
    return builder.build();
  }
}
