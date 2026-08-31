package pe.smartcash.cash.advisor.infrastructure.llm;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * {@code RestClient} propio del asesor primario (Gemini), separado del {@code llmRestClient}
 * de {@code transactions.infrastructure.llm.LlmClientConfig} SOLO por el timeout: la
 * extracción de transacciones corre en un worker async con reintentos y quiere fallar
 * rápido ({@code app.llm.timeout}, ~8s), mientras que el chat del asesor usa modelos con
 * razonamiento que sin streaming tardan 10-30s en responder ({@code app.advisor.llm.timeout},
 * ~45s). Base-url y API key salen del mismo namespace {@code app.llm.*} -- se leen por
 * {@code @Value} en vez de importar {@code LlmProperties} de transactions, mismo criterio
 * que {@link GeminiFinancialAdvisorAdapter} con el modelo: este bounded context no depende
 * de ninguna clase de infraestructura de otro, solo del namespace de configuración.
 */
@Configuration
class AdvisorLlmClientConfig {

  @Bean
  RestClient advisorLlmRestClient(
      RestClient.Builder restClientBuilder,
      @Value("${app.llm.base-url}") String baseUrl,
      @Value("${app.llm.api-key}") String apiKey,
      @Value("${app.advisor.llm.timeout}") Duration timeout) {
    HttpClientSettings settings =
        HttpClientSettings.defaults().withConnectTimeout(timeout).withReadTimeout(timeout);

    return restClientBuilder
        .baseUrl(baseUrl)
        .defaultHeader("Authorization", "Bearer " + apiKey)
        .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
        .build();
  }
}
