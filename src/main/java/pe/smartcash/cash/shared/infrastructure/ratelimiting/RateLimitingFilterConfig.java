package pe.smartcash.cash.shared.infrastructure.ratelimiting;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Solo los dos endpoints costosos originales quedan detrás del filtro genérico (por
 * usuario/IP): la ingestión de transacciones por JSON y el webhook de Stripe. El resto de la
 * API no paga el round-trip extra a Redis en cada request.
 *
 * <p>Deliberadamente NO incluye {@code /api/v1/transactions/inbound}: ese endpoint lo llama
 * siempre SendGrid (Inbound Parse), nunca el remitente original del correo, así que "por IP"
 * limitaría a TODOS los usuarios juntos contra un mismo cupo de 15/min. Ese endpoint se
 * protege aparte, por {@code to} (el buzón destino), vía {@code @RateLimitByToParam} +
 * {@code WebhookRateLimitingAspect} -- AOP en vez de este filtro, porque a esa altura del
 * pipeline (antes de que Spring MVC resuelva el multipart) todavía no existe un {@code to}
 * que leer.
 */
@Configuration
class RateLimitingFilterConfig {

  @Bean
  FilterRegistrationBean<RateLimitingFilter> rateLimitingFilterRegistration(RateLimitingFilter filter) {
    FilterRegistrationBean<RateLimitingFilter> registration = new FilterRegistrationBean<>(filter);
    registration.addUrlPatterns("/api/v1/transactions/webhook", "/api/v1/subscriptions/stripe-webhook");
    return registration;
  }
}
