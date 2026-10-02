package pe.smartcash.cash.subscription.infrastructure.payment;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Cliente HTTP dedicado a Culqi, mismo patrón que {@code GroqClientConfig}. */
@Configuration
class CulqiClientConfig {

  @Bean
  RestClient culqiRestClient(RestClient.Builder restClientBuilder, CulqiProperties properties) {
    HttpClientSettings settings =
        HttpClientSettings.defaults().withConnectTimeout(properties.timeout()).withReadTimeout(properties.timeout());

    return restClientBuilder
        .clone()
        .baseUrl(properties.apiBaseUrl())
        .defaultHeader("Authorization", "Bearer " + properties.secretKey())
        .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
        .build();
  }
}
