package pe.smartcash.cash.shared.infrastructure.ratelimiting;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Rate limiter genérico por clave arbitraria, sobre el mismo {@code ProxyManager<String>}
 * (Lettuce + bucket4j-redis, conectado a Upstash en prod vía {@code SPRING_DATA_REDIS_URL})
 * que ya arma {@code RateLimiterConfig} para {@link RateLimitingFilter} -- un solo cliente
 * Redis para todo el rate limiting del proyecto, sin duplicar la conexión. Se diferencia de
 * {@code RateLimitingFilter} en que no asume nada sobre HTTP/IP/usuario autenticado: quien
 * llama decide qué es la "key" (acá, la dirección de correo destino del webhook de SendGrid,
 * ver {@link WebhookRateLimitingAspect}).
 */
@Service
public class WebhookRateLimiterService {

  private static final String KEY_PREFIX = "rate-limit:webhook-inbound:";

  private final ProxyManager<String> proxyManager;
  private final int capacity;
  private final Duration window;

  WebhookRateLimiterService(
      ProxyManager<String> proxyManager,
      @Value("${app.rate-limit.webhook.capacity}") int capacity,
      @Value("${app.rate-limit.webhook.window}") Duration window) {
    this.proxyManager = proxyManager;
    this.capacity = capacity;
    this.window = window;
  }

  /** @throws RateLimitExceededException si {@code key} ya agotó su cupo de la ventana actual. */
  public void consume(String key) {
    Bucket bucket = proxyManager.builder().build(KEY_PREFIX + key, this::bucketConfiguration);
    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
    if (!probe.isConsumed()) {
      long retryAfterSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000);
      throw new RateLimitExceededException(key, retryAfterSeconds);
    }
  }

  private BucketConfiguration bucketConfiguration() {
    Bandwidth limit = Bandwidth.builder().capacity(capacity).refillIntervally(capacity, window).build();
    return BucketConfiguration.builder().addLimit(limit).build();
  }
}
