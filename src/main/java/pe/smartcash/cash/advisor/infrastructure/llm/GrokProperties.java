package pe.smartcash.cash.advisor.infrastructure.llm;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Proveedor de respaldo del asesor (xAI Grok, dialecto Chat Completions compatible con
 * OpenAI): se usa solo cuando Gemini falla, ver {@link FallbackAdvisorChatClient}. */
@ConfigurationProperties(prefix = "app.llm.fallback")
public record GrokProperties(String baseUrl, String apiKey, String model, Duration timeout) {

  public GrokProperties {
    if (timeout == null) {
      timeout = Duration.ofSeconds(8);
    }
  }
}
