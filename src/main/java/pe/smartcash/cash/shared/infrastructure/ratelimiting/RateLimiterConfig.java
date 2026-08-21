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
import org.springframework.boot.data.redis.autoconfigure.DataRedisConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cliente Lettuce dedicado a Bucket4j, separado del {@code LettuceConnectionFactory} que
 * autoconfigura spring-data-redis para el {@code StringRedisTemplate} (cache de comercios,
 * blacklist de tokens): bucket4j-redis necesita hablarle directo a una
 * {@link StatefulRedisConnection} con un codec de valor {@code byte[]} (así serializa el
 * estado del bucket), no la que expone spring-data-redis para sus propios templates. Se arma
 * a partir de {@link DataRedisConnectionDetails} (la misma que resuelve el
 * {@code LettuceConnectionFactory} autoconfigurado, poblada por Spring Boot desde
 * docker-compose en dev o desde {@code SPRING_DATA_REDIS_URL} en prod/Upstash) en vez de
 * {@code DataRedisProperties}, que no refleja el puerto mapeado por docker-compose.
 */
@Configuration
class RateLimiterConfig {

  @Bean(destroyMethod = "shutdown")
  RedisClient bucket4jRedisClient(DataRedisConnectionDetails connectionDetails) {
    return RedisClient.create(resolveUri(connectionDetails));
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

  private RedisURI resolveUri(DataRedisConnectionDetails connectionDetails) {
    DataRedisConnectionDetails.Standalone standalone = connectionDetails.getStandalone();
    RedisURI.Builder builder = RedisURI.Builder.redis(standalone.getHost(), standalone.getPort());
    if (connectionDetails.getPassword() != null) {
      builder.withPassword(connectionDetails.getPassword().toCharArray());
    }
    if (connectionDetails.getSslBundle() != null) {
      builder.withSsl(true);
    }
    return builder.build();
  }
}
