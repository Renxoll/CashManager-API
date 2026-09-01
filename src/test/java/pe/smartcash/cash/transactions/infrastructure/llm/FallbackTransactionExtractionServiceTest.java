package pe.smartcash.cash.transactions.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import pe.smartcash.cash.shared.infrastructure.llm.GrokProperties;
import pe.smartcash.cash.transactions.domain.exception.TransactionExtractionFailedException;
import pe.smartcash.cash.transactions.domain.services.ExtractionResult;
import tools.jackson.databind.ObjectMapper;

/** Mismo patrón que {@code advisor.infrastructure.llm.FallbackAdvisorChatClientTest} -- cada
 * proveedor se respalda con su propio {@code MockRestServiceServer}. */
class FallbackTransactionExtractionServiceTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private OpenAiTransactionExtractionAdapter openAiAdapter(RestClient.Builder builder) {
    return new OpenAiTransactionExtractionAdapter(builder.build(), new LlmProperties("http://gemini.test", "test-key", "gemini-3.6-flash", Duration.ofSeconds(8), 2), objectMapper);
  }

  private GrokTransactionExtractionAdapter grokAdapter(RestClient.Builder builder) {
    return new GrokTransactionExtractionAdapter(
        builder.build(), new GrokProperties("http://grok.test", "test-key", "grok-4.6", Duration.ofSeconds(8)), objectMapper);
  }

  @Test
  void usesPrimaryWhenItSucceeds() {
    RestClient.Builder geminiBuilder = RestClient.builder().baseUrl("http://gemini.test");
    MockRestServiceServer geminiServer = MockRestServiceServer.bindTo(geminiBuilder).build();
    geminiServer
        .expect(requestTo("http://gemini.test/chat/completions"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                """
                {"choices":[{"message":{"role":"assistant","content":"{\\"monto\\":6.50,\\"moneda\\":\\"PEN\\",\\"comercio\\":\\"Feel Good Villa\\",\\"categoria\\":\\"COMIDA\\"}"}}]}
                """,
                MediaType.APPLICATION_JSON));

    // Sin ninguna expectativa configurada en el server de Grok: si el fallback llegara a
    // invocarse cuando no debía, esta llamada fallaría con un error de conexión real.
    RestClient.Builder grokBuilder = RestClient.builder().baseUrl("http://grok.test");

    FallbackTransactionExtractionService service = new FallbackTransactionExtractionService(openAiAdapter(geminiBuilder), grokAdapter(grokBuilder));

    ExtractionResult result = service.extract("Consumo de S/6.50 en Feel Good Villa");

    assertThat(result.merchant().name()).isEqualTo("Feel Good Villa");
    geminiServer.verify();
  }

  @Test
  void fallsBackToGrokWhenPrimaryFails() {
    RestClient.Builder geminiBuilder = RestClient.builder().baseUrl("http://gemini.test");
    MockRestServiceServer geminiServer = MockRestServiceServer.bindTo(geminiBuilder).build();
    geminiServer.expect(requestTo("http://gemini.test/chat/completions")).andExpect(method(HttpMethod.POST)).andRespond(withServerError());

    RestClient.Builder grokBuilder = RestClient.builder().baseUrl("http://grok.test");
    MockRestServiceServer grokServer = MockRestServiceServer.bindTo(grokBuilder).build();
    grokServer
        .expect(requestTo("http://grok.test/chat/completions"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                """
                {"choices":[{"message":{"role":"assistant","content":"{\\"monto\\":6.50,\\"moneda\\":\\"PEN\\",\\"comercio\\":\\"Feel Good Villa\\",\\"categoria\\":\\"COMIDA\\"}"}}]}
                """,
                MediaType.APPLICATION_JSON));

    FallbackTransactionExtractionService service = new FallbackTransactionExtractionService(openAiAdapter(geminiBuilder), grokAdapter(grokBuilder));

    ExtractionResult result = service.extract("Consumo de S/6.50 en Feel Good Villa");

    assertThat(result.merchant().name()).isEqualTo("Feel Good Villa");
    geminiServer.verify();
    grokServer.verify();
  }

  @Test
  void propagatesFallbackFailureWithPrimaryFailureSuppressedWhenBothFail() {
    RestClient.Builder geminiBuilder = RestClient.builder().baseUrl("http://gemini.test");
    MockRestServiceServer geminiServer = MockRestServiceServer.bindTo(geminiBuilder).build();
    geminiServer.expect(requestTo("http://gemini.test/chat/completions")).andExpect(method(HttpMethod.POST)).andRespond(withServerError());

    RestClient.Builder grokBuilder = RestClient.builder().baseUrl("http://grok.test");
    MockRestServiceServer grokServer = MockRestServiceServer.bindTo(grokBuilder).build();
    grokServer.expect(requestTo("http://grok.test/chat/completions")).andExpect(method(HttpMethod.POST)).andRespond(withServerError());

    FallbackTransactionExtractionService service = new FallbackTransactionExtractionService(openAiAdapter(geminiBuilder), grokAdapter(grokBuilder));

    assertThatThrownBy(() -> service.extract("Consumo de S/6.50 en Feel Good Villa"))
        .isInstanceOf(TransactionExtractionFailedException.class)
        .satisfies(ex -> assertThat(ex.getSuppressed()).hasSize(1).allMatch(TransactionExtractionFailedException.class::isInstance));
  }
}
