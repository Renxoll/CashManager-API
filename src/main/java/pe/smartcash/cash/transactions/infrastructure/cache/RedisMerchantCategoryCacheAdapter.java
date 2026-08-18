package pe.smartcash.cash.transactions.infrastructure.cache;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import pe.smartcash.cash.transactions.domain.model.valueobjects.CategoryCode;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Merchant;
import pe.smartcash.cash.transactions.domain.services.MerchantCategoryCache;

/**
 * TTL de 30 días: los comercios rara vez cambian de categoría, pero no queremos un cache
 * eterno que ignore correcciones posteriores del catálogo.
 */
@Component
class RedisMerchantCategoryCacheAdapter implements MerchantCategoryCache {

  private static final Duration TTL = Duration.ofDays(30);
  private static final String KEY_PREFIX = "merchant-category:";

  private final StringRedisTemplate redisTemplate;

  RedisMerchantCategoryCacheAdapter(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public Optional<CategoryCode> findCategoryFor(Merchant merchant) {
    return Optional.ofNullable(redisTemplate.opsForValue().get(key(merchant))).map(CategoryCode::fromCode);
  }

  @Override
  public void remember(Merchant merchant, CategoryCode categoryCode) {
    redisTemplate.opsForValue().set(key(merchant), categoryCode.name(), TTL);
  }

  private String key(Merchant merchant) {
    return KEY_PREFIX + merchant.name().trim().toLowerCase(Locale.ROOT);
  }
}
