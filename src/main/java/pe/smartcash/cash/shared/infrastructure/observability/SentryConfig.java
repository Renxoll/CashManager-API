package pe.smartcash.cash.shared.infrastructure.observability;

import io.sentry.Sentry;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Inicialización manual del SDK base de Sentry (no el starter de Spring Boot, ver el
 * comentario en {@code build.gradle}): un único {@code Sentry.init} al arrancar, después del
 * cual {@code Sentry.captureException(...)} funciona desde cualquier punto del código (ver
 * {@code GlobalExceptionHandler}, {@code StripeWebhookController},
 * {@code OpenAiTransactionExtractionAdapter}). Con {@code sentry.dsn} vacío (default en
 * dev/test) el propio SDK queda en modo no-op -- no hace falta un {@code @ConditionalOnProperty}
 * propio para eso.
 */
@Configuration
class SentryConfig {

  private final String dsn;
  private final String environment;
  private final double tracesSampleRate;

  SentryConfig(
      @Value("${sentry.dsn}") String dsn,
      @Value("${sentry.environment}") String environment,
      @Value("${sentry.traces-sample-rate}") double tracesSampleRate) {
    this.dsn = dsn;
    this.environment = environment;
    this.tracesSampleRate = tracesSampleRate;
  }

  @PostConstruct
  void init() {
    Sentry.init(
        options -> {
          options.setDsn(dsn);
          options.setEnvironment(environment);
          options.setTracesSampleRate(tracesSampleRate);
        });
  }
}
