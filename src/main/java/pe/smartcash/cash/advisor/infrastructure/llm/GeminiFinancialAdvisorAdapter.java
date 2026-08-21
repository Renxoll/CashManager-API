package pe.smartcash.cash.advisor.infrastructure.llm;

import io.sentry.Sentry;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pe.smartcash.cash.advisor.domain.exception.AdvisorUnavailableException;
import pe.smartcash.cash.advisor.domain.services.AdvisorChatClient;
import pe.smartcash.cash.advisor.domain.services.FinancialContext;

/**
 * Mismo {@code RestClient} que ya arma {@code transactions.infrastructure.llm.LlmClientConfig}
 * para Google AI Studio (base-url + API key de {@code app.llm.*}): es el único bean {@code
 * RestClient} del proyecto, así que se inyecta por tipo sin necesitar wiring nuevo. Se lee
 * {@code app.llm.model} directo por {@code @Value} en vez de importar {@code LlmProperties}
 * de transactions: así este adaptador no depende de ninguna clase de infraestructura de otro
 * bounded context, solo del mismo namespace de configuración.
 *
 * <p>A diferencia de {@code OpenAiTransactionExtractionAdapter}, acá no hay {@code
 * response_format=json_schema}: la salida es texto libre para el usuario, no un objeto a
 * parsear, así que tampoco hace falta el reintento de "JSON inválido" de la extracción.
 */
@Component
class GeminiFinancialAdvisorAdapter implements AdvisorChatClient {

  // Más alto que el 0.0 de la extracción (que busca determinismo puro): acá se busca una
  // respuesta con algo de tono natural/empático, sin dejar de ser concisa y analítica.
  private static final double TEMPERATURE = 0.4;

  private final RestClient llmRestClient;
  private final String model;

  GeminiFinancialAdvisorAdapter(RestClient llmRestClient, @Value("${app.llm.model}") String model) {
    this.llmRestClient = llmRestClient;
    this.model = model;
  }

  @Override
  public String reply(FinancialContext context, String question) {
    ChatCompletionRequest request =
        new ChatCompletionRequest(
            model,
            List.of(
                new ChatMessage("system", AdvisorPrompts.SYSTEM_PROMPT),
                new ChatMessage("user", AdvisorPrompts.userPrompt(context, question))),
            TEMPERATURE);

    ChatCompletionResponse response;
    try {
      response = llmRestClient.post().uri("/chat/completions").body(request).retrieve().body(ChatCompletionResponse.class);
    } catch (RestClientException httpError) {
      throw reportFailure(new AdvisorUnavailableException("Fallo de comunicación con el proveedor de LLM", httpError));
    }

    if (response == null || response.choices() == null || response.choices().isEmpty()) {
      throw reportFailure(new AdvisorUnavailableException("El proveedor de LLM devolvió una respuesta sin choices"));
    }

    return response.choices().get(0).message().content();
  }

  private static AdvisorUnavailableException reportFailure(AdvisorUnavailableException ex) {
    Sentry.captureException(ex, scope -> scope.setTag("component", "llm-advisor"));
    return ex;
  }
}
