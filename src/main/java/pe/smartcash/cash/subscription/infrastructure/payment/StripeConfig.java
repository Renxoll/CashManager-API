package pe.smartcash.cash.subscription.infrastructure.payment;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

/**
 * El SDK de Stripe se configura vía un campo estático global ({@link Stripe#apiKey}) — así
 * es como funciona la librería oficial, no una elección de este proyecto. Esta es la única
 * clase que lo setea, y lo hace una sola vez al arrancar el contexto.
 */
@Configuration
class StripeConfig {

  private final StripeProperties properties;

  StripeConfig(StripeProperties properties) {
    this.properties = properties;
  }

  @PostConstruct
  void configureStripeApiKey() {
    Stripe.apiKey = properties.secretKey();
  }
}
