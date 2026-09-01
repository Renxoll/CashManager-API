package pe.smartcash.cash.shared.infrastructure.llm;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Proveedor de respaldo (xAI Grok, dialecto Chat Completions compatible con OpenAI): entra
 * en juego solo cuando el proveedor primario (Gemini, {@code app.llm.*}) falla. Vive en
 * {@code shared} porque lo usan dos bounded contexts distintos: {@code advisor} (ver {@code
 * FallbackAdvisorChatClient}) y {@code transactions} (ver {@code
 * FallbackTransactionExtractionService}) -- ninguno es dueño exclusivo de "hablar con Grok".
 */
@ConfigurationProperties(prefix = "app.llm.fallback")
public record GrokProperties(String baseUrl, String apiKey, String model, Duration timeout) {

  public GrokProperties {
    if (timeout == null) {
      timeout = Duration.ofSeconds(8);
    }
  }
}
