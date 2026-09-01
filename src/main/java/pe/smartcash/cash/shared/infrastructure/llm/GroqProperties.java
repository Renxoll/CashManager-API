package pe.smartcash.cash.shared.infrastructure.llm;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Proveedor de respaldo (Groq Cloud -- groq.com, NO confundir con Grok de xAI --, dialecto
 * Chat Completions compatible con OpenAI): entra en juego solo cuando el proveedor primario
 * (Gemini, {@code app.llm.*}) falla. Elegido a propósito por tener un tier gratuito real (sin
 * tarjeta ni saldo que cargar), a diferencia de la API de xAI. Vive en {@code shared} porque
 * lo usan dos bounded contexts distintos: {@code advisor} (ver {@code
 * FallbackAdvisorChatClient}) y {@code transactions} (ver {@code
 * FallbackTransactionExtractionService}) -- ninguno es dueño exclusivo de "hablar con Groq".
 */
@ConfigurationProperties(prefix = "app.llm.fallback")
public record GroqProperties(String baseUrl, String apiKey, String model, Duration timeout) {

  public GroqProperties {
    if (timeout == null) {
      timeout = Duration.ofSeconds(8);
    }
  }
}
