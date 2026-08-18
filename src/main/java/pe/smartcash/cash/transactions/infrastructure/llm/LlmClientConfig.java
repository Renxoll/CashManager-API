package pe.smartcash.cash.transactions.infrastructure.llm;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
class LlmClientConfig {

  @Bean
  RestClient llmRestClient(RestClient.Builder restClientBuilder, LlmProperties properties) {
    HttpClientSettings settings =
        HttpClientSettings.defaults().withConnectTimeout(properties.timeout()).withReadTimeout(properties.timeout());

    return restClientBuilder
        .baseUrl(properties.baseUrl())
        .defaultHeader("Authorization", "Bearer " + properties.apiKey())
        .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
        .build();
  }
}
