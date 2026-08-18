package pe.smartcash.cash.transactions.infrastructure.llm;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.llm")
public record LlmProperties(String baseUrl, String apiKey, String model, Duration timeout, int maxRetries) {

  public LlmProperties {
    if (timeout == null) {
      timeout = Duration.ofSeconds(8);
    }
    if (maxRetries <= 0) {
      maxRetries = 2;
    }
  }
}
