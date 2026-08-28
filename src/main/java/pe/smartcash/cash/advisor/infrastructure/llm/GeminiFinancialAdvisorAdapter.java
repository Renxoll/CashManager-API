package pe.smartcash.cash.advisor.infrastructure.llm;

import io.sentry.Sentry;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pe.smartcash.cash.advisor.domain.exception.AdvisorUnavailableException;
import pe.smartcash.cash.advisor.domain.services.FinancialContext;

/**
 * Proveedor primario del asesor. Mismo {@code RestClient} que ya arma {@code
 * transactions.infrastructure.llm.LlmClientConfig} para Google AI Studio (base-url + API key
 * de {@code app.llm.*}) -- calificado explícitamente por nombre de bean porque desde que
 * existe {@link GrokClientConfig} ya no es el único {@code RestClient} del proyecto. Se lee
 * {@code app.llm.model} directo por {@code @Value} en vez de importar {@code LlmProperties}
 * de transactions: así este adaptador no depende de ninguna clase de infraestructura de otro
 * bounded context, solo del mismo namespace de configuración.
 *
 * <p>No implementa {@link pe.smartcash.cash.advisor.domain.services.AdvisorChatClient}
 * directamente -- el único bean que implementa ese puerto es {@link
 * FallbackAdvisorChatClient}, que compone este adapter con {@link GrokFinancialAdvisorAdapter}
 * como respaldo. El free tier de Gemini tiene un límite diario de solicitudes muy bajo (20/día
 * en el proyecto de este equipo) que se agota con uso normal del chat.
 *
 * <p>A diferencia de {@code OpenAiTransactionExtractionAdapter}, acá no hay {@code
 * response_format=json_schema}: la salida es texto libre para el usuario, no un objeto a
 * parsear, así que tampoco hace falta el reintento de "JSON inválido" de la extracción.
 */
@Component
class GeminiFinancialAdvisorAdapter {

  // Más alto que el 0.0 de la extracción (que busca determinismo puro): acá se busca una
  // respuesta con algo de tono natural/empático, sin dejar de ser concisa y analítica.
  private static final double TEMPERATURE = 0.4;

  private final RestClient llmRestClient;
  private final String model;

  GeminiFinancialAdvisorAdapter(@Qualifier("llmRestClient") RestClient llmRestClient, @Value("${app.llm.model}") String model) {
    this.llmRestClient = llmRestClient;
    this.model = model;
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
      response = llmRestClient.post().uri("/chat/completions").body(request).retrieve().body(ChatCompletionResponse.class);
    } catch (RestClientException httpError) {
      throw reportFailure(new AdvisorUnavailableException("Fallo de comunicación con el proveedor de LLM", httpError));
    }

    if (response == null || response.choices() == null || response.choices().isEmpty()) {
      throw reportFailure(new AdvisorUnavailableException("El proveedor de LLM devolvió una respuesta sin choices"));
    }

    String content = response.choices().get(0).message().content();
    // Un choice presente con content vacío/en blanco no es una respuesta válida -- sin este
    // chequeo pasaría como si lo fuera, y el frontend terminaría mostrando una burbuja de
    // chat vacía sin ningún error visible para el usuario.
    if (content == null || content.isBlank()) {
      throw reportFailure(new AdvisorUnavailableException("El proveedor de LLM devolvió una respuesta vacía"));
    }
    return content;
  }

  private static AdvisorUnavailableException reportFailure(AdvisorUnavailableException ex) {
    Sentry.captureException(ex, scope -> scope.setTag("component", "llm-advisor"));
    return ex;
  }
}
