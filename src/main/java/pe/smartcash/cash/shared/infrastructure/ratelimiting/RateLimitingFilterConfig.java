package pe.smartcash.cash.shared.infrastructure.ratelimiting;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Solo los dos endpoints costosos declarados en el enunciado quedan detrás del filtro: la
 * ingestión de transacciones (dispara al LLM en el worker async) y el webhook de Stripe
 * (público). El resto de la API no paga el round-trip extra a Redis en cada request.
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
