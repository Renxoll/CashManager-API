package pe.smartcash.cash.advisor.infrastructure.llm;

import io.sentry.Sentry;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pe.smartcash.cash.advisor.domain.exception.AdvisorUnavailableException;
import pe.smartcash.cash.advisor.domain.services.FinancialContext;
import pe.smartcash.cash.shared.infrastructure.llm.GroqProperties;

/**
 * Proveedor de respaldo del asesor (Groq Cloud, mismo dialecto Chat Completions que Gemini):
 * solo se invoca desde {@link FallbackAdvisorChatClient} cuando {@link
 * GeminiFinancialAdvisorAdapter} falla. No implementa {@code AdvisorChatClient} directamente
 * por la misma razón que el adapter primario -- ver esa clase.
 */
@Component
class GroqFinancialAdvisorAdapter {

  // Mismo valor que el adapter primario: se busca el mismo tono en la respuesta de
  // respaldo, no un comportamiento distinto solo porque cambió el proveedor.
  private static final double TEMPERATURE = 0.4;

  private final RestClient groqRestClient;
  private final String model;

  GroqFinancialAdvisorAdapter(@Qualifier("groqRestClient") RestClient groqRestClient, GroqProperties properties) {
    this.groqRestClient = groqRestClient;
    this.model = properties.model();
  }

  String reply(FinancialContext context, String question) {
    ChatCompletionRequest request =
        new ChatCompletionRequest(
            model,
            List.of(
                new ChatMessage("system", AdvisorPrompts.SYSTEM_PROMPT),
                new ChatMessage("user", AdvisorPrompts.userPrompt(context, question))),
            TEMPERATURE);

    ChatCompletionResponse response;
    try {
      response = groqRestClient.post().uri("/chat/completions").body(request).retrieve().body(ChatCompletionResponse.class);
    } catch (RestClientException httpError) {
      throw reportFailure(new AdvisorUnavailableException("Fallo de comunicación con el proveedor de LLM de respaldo", httpError));
    }

    if (response == null || response.choices() == null || response.choices().isEmpty()) {
      throw reportFailure(new AdvisorUnavailableException("El proveedor de LLM de respaldo devolvió una respuesta sin choices"));
    }

    String content = response.choices().get(0).message().content();
    if (content == null || content.isBlank()) {
      throw reportFailure(new AdvisorUnavailableException("El proveedor de LLM de respaldo devolvió una respuesta vacía"));
    }
    return content;
  }

  private static AdvisorUnavailableException reportFailure(AdvisorUnavailableException ex) {
    Sentry.captureException(ex, scope -> scope.setTag("component", "llm-advisor-fallback"));
    return ex;
  }
}
