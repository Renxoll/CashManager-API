package pe.smartcash.cash.shared.infrastructure.observability;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registra {@link CorrelationIdFilter} explícitamente con la precedencia más alta posible:
 * sin este registro manual, Spring Boot lo auto-registraría con orden por defecto
 * ({@code LOWEST_PRECEDENCE}), y correría DESPUÉS del filtro de Spring Security — dejando sin
 * correlation_id los logs de autenticación/autorización, que son justo los que más se
 * necesitan poder correlacionar en un incidente.
 */
@Configuration
class ObservabilityConfig {

  @Bean
  FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration(CorrelationIdFilter filter) {
    FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>(filter);
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    registration.addUrlPatterns("/*");
    return registration;
  }
}
