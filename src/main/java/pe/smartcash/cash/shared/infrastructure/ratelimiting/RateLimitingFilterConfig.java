package pe.smartcash.cash.shared.infrastructure.ratelimiting;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Solo los dos endpoints costosos originales quedan detrás del filtro genérico (por
 * usuario/IP): la ingestión de transacciones por JSON y el webhook de Stripe. El resto de la
 * API no paga el round-trip extra a Redis en cada request.
 *
 * <p><b>Deliberadamente NO incluye {@code /api/v1/transactions/inbound}:</b> ese endpoint lo
 * llama siempre SendGrid (Inbound Parse), nunca el remitente original del correo, así que
 * "por IP" limitaría a TODOS los usuarios juntos contra un mismo cupo de 15/min en vez de
 * limitar a cada uno por separado. Ese endpoint necesita su propia key (por {@code to}, el
 * buzón destino) leída del multipart en la capa de aplicación, no en este filtro genérico —
 * pendiente como su propio cambio, no se resuelve acá para no enviar un rate limit que se ve
 * bien pero en producción bloquea a todos los usuarios entre sí.
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
