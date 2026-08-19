package pe.smartcash.cash.shared.infrastructure.ratelimiting;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Un bucket por identidad (usuario autenticado, o IP si no hay uno) y por endpoint costoso
 * (ver {@code RateLimitingFilterConfig} para el mapeo de rutas). Corre DESPUÉS del filtro de
 * Spring Security (orden por defecto de {@code FilterRegistrationBean}, que cae detrás del
 * {@code springSecurityFilterChain} registrado a orden -100): así {@code
 * SecurityContextHolder} ya tiene la autenticación resuelta cuando este filtro decide la key.
 */
@Component
class RateLimitingFilter extends OncePerRequestFilter {

  private final ProxyManager<String> proxyManager;
  private final int capacity;
  private final Duration window;

  RateLimitingFilter(
      ProxyManager<String> proxyManager,
      @Value("${app.rate-limit.capacity}") int capacity,
      @Value("${app.rate-limit.window}") Duration window) {
    this.proxyManager = proxyManager;
    this.capacity = capacity;
    this.window = window;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String key = resolveKey(request);
    Bucket bucket = proxyManager.builder().build(key, this::bucketConfiguration);
    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

    if (!probe.isConsumed()) {
      long retryAfterSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000);
      response.setStatus(429);
      response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
      response.setContentType("application/json");
      response.getWriter().write(tooManyRequestsBody(request, retryAfterSeconds));
      return;
    }

    response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
    filterChain.doFilter(request, response);
  }

  private BucketConfiguration bucketConfiguration() {
    return BucketConfiguration.builder().addLimit(Bandwidth.classic(capacity, Refill.intervally(capacity, window))).build();
  }

  /**
   * El webhook de Stripe es público (sin Bearer, ver {@code SecurityConfig}) así que ahí no
   * hay más identidad que la IP; el de transacciones sí exige Bearer, así que limitar por
   * usuario evita que dos usuarios detrás del mismo NAT/proxy corporativo compartan cupo.
   */
  private String resolveKey(HttpServletRequest request) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
      return "rate-limit:user:" + authentication.getName();
    }
    return "rate-limit:ip:" + clientIp(request);
  }

  private String clientIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  private String tooManyRequestsBody(HttpServletRequest request, long retryAfterSeconds) {
    return "{\"timestamp\":\"%s\",\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Límite de %d peticiones por minuto excedido, reintenta en %ds\",\"path\":\"%s\"}"
        .formatted(Instant.now(), capacity, retryAfterSeconds, request.getRequestURI());
  }
}
