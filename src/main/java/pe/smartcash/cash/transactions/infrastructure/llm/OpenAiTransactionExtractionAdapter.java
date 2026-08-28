package pe.smartcash.cash.transactions.infrastructure.llm;

import io.sentry.Sentry;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pe.smartcash.cash.transactions.domain.exception.TransactionExtractionFailedException;
import pe.smartcash.cash.transactions.domain.model.valueobjects.CategoryCode;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Merchant;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Money;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionType;
import pe.smartcash.cash.transactions.domain.services.ExtractionResult;
import pe.smartcash.cash.transactions.domain.services.TransactionExtractionService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Adaptador contra el dialecto Chat Completions con Structured Outputs
 * (response_format=json_schema, strict=true). Si el LLM devuelve algo que no parsea como
 * JSON válido, se reintenta UNA vez con un prompt de corrección explícito antes de
 * rendirse; nunca se reintenta indefinidamente ni se deja pasar basura al dominio.
 */
@Slf4j
@Component
class OpenAiTransactionExtractionAdapter implements TransactionExtractionService {

  private static final String SCHEMA_JSON =
      """
      {
        "type": "object",
        "properties": {
          "monto": {"type": "number"},
          "moneda": {"type": "string", "pattern": "^[A-Z]{3}$"},
          "comercio": {"type": "string"},
          "categoria": {
            "type": ["string", "null"],
            "enum": ["COMIDA","TRANSPORTE","ENTRETENIMIENTO","SALUD","COMPRAS","SERVICIOS","EDUCACION","OTROS", null]
          },
          "tipo": {"type": "string", "enum": ["GASTO","INGRESO"]}
        },
        "required": ["monto","moneda","comercio","categoria","tipo"],
        "additionalProperties": false
      }
      """;

  private final RestClient llmRestClient;
  private final LlmProperties properties;
  private final ObjectMapper objectMapper;
  private final JsonNode responseSchema;

  OpenAiTransactionExtractionAdapter(RestClient llmRestClient, LlmProperties properties, ObjectMapper objectMapper) {
    this.llmRestClient = llmRestClient;
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.responseSchema = parseSchema(objectMapper);
  }

  @Override
  public ExtractionResult extract(String rawText) {
    try {
      return callAndParse(ExtractionPrompts.userPrompt(rawText));
    } catch (JacksonException malformed) {
      log.warn("El LLM devolvió un JSON inválido en el primer intento, reintentando con prompt de corrección");
      String repairPrompt =
          ExtractionPrompts.userPrompt(rawText)
              + "\n\nTu respuesta anterior no era un JSON válido según el esquema. "
              + "Responde de nuevo, EXCLUSIVAMENTE con el JSON correcto.";
      try {
        return callAndParse(repairPrompt);
      } catch (JacksonException stillMalformed) {
        throw reportFailure(new TransactionExtractionFailedException("El LLM no devolvió un JSON válido tras reintento", stillMalformed));
      }
    } catch (RestClientException httpError) {
      throw reportFailure(new TransactionExtractionFailedException("Fallo de comunicación con el proveedor de LLM", httpError));
    }
  }

  /**
   * El caller (ver {@code TransactionCommandServiceImpl.resolveExtraction}) atrapa esta
   * excepción para persistir la transacción como FAILED sin volver a lanzarla, así que sin
   * esta captura explícita el fallo de LLM nunca llegaría a Sentry.
   */
  private static TransactionExtractionFailedException reportFailure(TransactionExtractionFailedException ex) {
    Sentry.captureException(ex, scope -> scope.setTag("component", "llm"));
    return ex;
  }

  private ExtractionResult callAndParse(String userContent) {
    ChatCompletionRequest request =
        new ChatCompletionRequest(
            properties.model(),
            List.of(
                new ChatMessage("system", ExtractionPrompts.SYSTEM_PROMPT),
                new ChatMessage("user", userContent)),
            new ResponseFormat("json_schema", new JsonSchemaSpec("extracted_transaction", responseSchema, true)),
            0.0);

    ChatCompletionResponse response =
        llmRestClient.post().uri("/chat/completions").body(request).retrieve().body(ChatCompletionResponse.class);

    if (response == null || response.choices() == null || response.choices().isEmpty()) {
      throw reportFailure(new TransactionExtractionFailedException("El proveedor de LLM devolvió una respuesta sin choices"));
    }

    String content = response.choices().get(0).message().content();
    ExtractedTransactionPayload payload = objectMapper.readValue(content, ExtractedTransactionPayload.class);
    CategoryCode categoryCode = payload.categoria() != null ? CategoryCode.fromCode(payload.categoria()) : null;
    return new ExtractionResult(
        new Money(payload.monto(), payload.moneda()), new Merchant(payload.comercio()), categoryCode, TransactionType.fromCode(payload.tipo()));
  }

  private static JsonNode parseSchema(ObjectMapper objectMapper) {
    try {
      return objectMapper.readTree(SCHEMA_JSON);
    } catch (JacksonException e) {
      throw new IllegalStateException("Schema JSON embebido inválido", e);
    }
  }
}
