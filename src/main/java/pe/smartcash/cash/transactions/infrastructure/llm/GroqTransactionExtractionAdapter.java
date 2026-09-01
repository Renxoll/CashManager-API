package pe.smartcash.cash.transactions.infrastructure.llm;

import io.sentry.Sentry;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pe.smartcash.cash.shared.infrastructure.llm.GroqProperties;
import pe.smartcash.cash.transactions.domain.exception.TransactionExtractionFailedException;
import pe.smartcash.cash.transactions.domain.model.valueobjects.CategoryCode;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Merchant;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Money;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionType;
import pe.smartcash.cash.transactions.domain.services.ExtractionResult;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Proveedor de respaldo de extracción (Groq Cloud): solo se invoca desde {@code
 * FallbackTransactionExtractionService} cuando {@link OpenAiTransactionExtractionAdapter}
 * falla. A diferencia del adapter primario, NO usa Structured Outputs
 * (response_format=json_schema): no todos los proveedores OpenAI-compatibles soportan esa
 * extensión, así que acá se confía en la regla 1 de {@link ExtractionPrompts#SYSTEM_PROMPT}
 * ("responde EXCLUSIVAMENTE con JSON") más el mismo reintento-por-JSON-inválido del adapter
 * primario -- es una ruta de respaldo, rara vez se invoca, pero debe seguir siendo
 * resiliente igual.
 */
@Slf4j
@Component
class GroqTransactionExtractionAdapter {

  private final RestClient groqRestClient;
  private final String model;
  private final ObjectMapper objectMapper;

  GroqTransactionExtractionAdapter(@Qualifier("groqRestClient") RestClient groqRestClient, GroqProperties properties, ObjectMapper objectMapper) {
    this.groqRestClient = groqRestClient;
    this.model = properties.model();
    this.objectMapper = objectMapper;
  }

  ExtractionResult extract(String rawText) {
    try {
      return callAndParse(ExtractionPrompts.userPrompt(rawText));
    } catch (JacksonException malformed) {
      log.warn("El proveedor de respaldo devolvió un JSON inválido en el primer intento, reintentando con prompt de corrección");
      String repairPrompt =
          ExtractionPrompts.userPrompt(rawText)
              + "\n\nTu respuesta anterior no era un JSON válido según el esquema. "
              + "Responde de nuevo, EXCLUSIVAMENTE con el JSON correcto.";
      try {
        return callAndParse(repairPrompt);
      } catch (JacksonException stillMalformed) {
        throw reportFailure(new TransactionExtractionFailedException("El proveedor de respaldo no devolvió un JSON válido tras reintento", stillMalformed));
      }
    } catch (RestClientException httpError) {
      throw reportFailure(new TransactionExtractionFailedException("Fallo de comunicación con el proveedor de LLM de respaldo", httpError));
    }
  }

  private static TransactionExtractionFailedException reportFailure(TransactionExtractionFailedException ex) {
    Sentry.captureException(ex, scope -> scope.setTag("component", "llm-extraction-fallback"));
    return ex;
  }

  private ExtractionResult callAndParse(String userContent) {
    ChatCompletionRequest request =
        new ChatCompletionRequest(
            model, List.of(new ChatMessage("system", ExtractionPrompts.SYSTEM_PROMPT), new ChatMessage("user", userContent)), null, 0.0);

    ChatCompletionResponse response =
        groqRestClient.post().uri("/chat/completions").body(request).retrieve().body(ChatCompletionResponse.class);

    if (response == null || response.choices() == null || response.choices().isEmpty()) {
      throw reportFailure(new TransactionExtractionFailedException("El proveedor de LLM de respaldo devolvió una respuesta sin choices"));
    }

    String content = response.choices().get(0).message().content();
    ExtractedTransactionPayload payload = objectMapper.readValue(content, ExtractedTransactionPayload.class);
    // Mismo criterio que el adapter primario: siempre EXPENSE, los ingresos se registran a
    // mano (ver TransactionCommandServiceImpl.handle(RecordManualIncomeCommand)).
    return new ExtractionResult(
        new Money(payload.monto(), payload.moneda()), new Merchant(payload.comercio()), CategoryCode.fromCode(payload.categoria()), TransactionType.EXPENSE);
  }
}
