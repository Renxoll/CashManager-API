package pe.smartcash.cash.shared.infrastructure.ratelimiting;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Endpoints costosos o abusables detrás del filtro genérico (por usuario/IP): la ingestión
 * de transacciones por JSON y el "pedir enlace de restablecimiento" (envía un correo ->
 * vector de spam/enumeración por fuerza). El resto de la API no paga el round-trip extra a
 * Redis en cada request.
 *
 * <p>Deliberadamente NO incluye {@code /api/v1/transactions/inbound}: ese endpoint lo llama
 * siempre SendGrid (Inbound Parse), nunca el remitente original del correo, así que "por IP"
 * limitaría a TODOS los usuarios juntos contra un mismo cupo de 15/min. Ese endpoint se
 * protege aparte, por {@code to} (el buzón destino), vía {@code @RateLimitByToParam} +
 * {@code WebhookRateLimitingAspect} -- AOP en vez de este filtro, porque a esa altura del
 * pipeline (antes de que Spring MVC resuelva el multipart) todavía no existe un {@code to}
 * que leer.
 *
 * <p>Tampoco incluye {@code /api/v1/subscriptions/culqi-webhook}, por la misma razón: Culqi
 * procesa todas las suscripciones del día en un batch y manda un evento por cobro desde sus
 * propias IPs, así que un cupo por IP rechazaría eventos legítimos apenas haya unos cuantos
 * suscriptores. Lo protegen el token de la URL y la relectura de cada evento en el API de Culqi.
 */
@Configuration
class RateLimitingFilterConfig {

  @Bean
  FilterRegistrationBean<RateLimitingFilter> rateLimitingFilterRegistration(RateLimitingFilter filter) {
    FilterRegistrationBean<RateLimitingFilter> registration = new FilterRegistrationBean<>(filter);
    registration.addUrlPatterns("/api/v1/transactions/webhook", "/api/v1/iam/password-reset/request");
    return registration;
  }
}
