package pe.smartcash.cash.subscription.infrastructure.payment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Cliente HTTP dedicado a Mercado Pago; solo existe si es el proveedor activo ({@code app.payments.provider}). */
@Configuration
@ConditionalOnProperty(name = "app.payments.provider", havingValue = "mercadopago")
class MercadoPagoClientConfig {

  @Bean
  RestClient mercadoPagoRestClient(RestClient.Builder restClientBuilder, MercadoPagoProperties properties) {
    HttpClientSettings settings =
        HttpClientSettings.defaults().withConnectTimeout(properties.timeout()).withReadTimeout(properties.timeout());

    return restClientBuilder
        .clone()
        .baseUrl(properties.apiBaseUrl())
        .defaultHeader("Authorization", "Bearer " + properties.accessToken())
        .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
        .build();
  }
}
