package pe.smartcash.cash.advisor.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import pe.smartcash.cash.advisor.domain.exception.AdvisorUnavailableException;
import pe.smartcash.cash.advisor.domain.services.FinancialContext;
import pe.smartcash.cash.shared.infrastructure.llm.GrokProperties;

/**
 * Cada proveedor se respalda con su propio {@code MockRestServiceServer} (mismo patrón que
 * {@link GeminiFinancialAdvisorAdapterTest}/{@link GrokFinancialAdvisorAdapterTest}) en vez de
 * mockear las clases directamente -- no hay precedente de mocking de clases concretas en este
 * proyecto, y esto ejercita el flujo real de fallback extremo a extremo.
 */
class FallbackAdvisorChatClientTest {

  private final FinancialContext context = new FinancialContext(BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, List.of());

  private GeminiFinancialAdvisorAdapter geminiAdapter(RestClient.Builder builder) {
    return new GeminiFinancialAdvisorAdapter(builder.build(), "gemini-3.6-flash");
  }

  private GrokFinancialAdvisorAdapter grokAdapter(RestClient.Builder builder) {
    return new GrokFinancialAdvisorAdapter(builder.build(), new GrokProperties("http://grok.test", "test-key", "grok-4.6", Duration.ofSeconds(8)));
  }

  @Test
  void usesPrimaryWhenItSucceeds() {
    RestClient.Builder geminiBuilder = RestClient.builder().baseUrl("http://gemini.test");
    MockRestServiceServer geminiServer = MockRestServiceServer.bindTo(geminiBuilder).build();
    geminiServer
        .expect(requestTo("http://gemini.test/chat/completions"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess("""
            {"choices":[{"message":{"role":"assistant","content":"respuesta de Gemini"}}]}
            """, MediaType.APPLICATION_JSON));

    // Sin ninguna expectativa configurada en el server de Grok: si el fallback llegara a
    // invocarse cuando no debía, esta llamada fallaría con un error de conexión real.
    RestClient.Builder grokBuilder = RestClient.builder().baseUrl("http://grok.test");

    FallbackAdvisorChatClient client = new FallbackAdvisorChatClient(geminiAdapter(geminiBuilder), grokAdapter(grokBuilder));

    assertThat(client.reply(context, "cuanto gasté")).isEqualTo("respuesta de Gemini");
    geminiServer.verify();
  }

  @Test
  void fallsBackToGrokWhenPrimaryFails() {
    RestClient.Builder geminiBuilder = RestClient.builder().baseUrl("http://gemini.test");
    MockRestServiceServer geminiServer = MockRestServiceServer.bindTo(geminiBuilder).build();
    geminiServer
        .expect(requestTo("http://gemini.test/chat/completions"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withServerError());

    RestClient.Builder grokBuilder = RestClient.builder().baseUrl("http://grok.test");
    MockRestServiceServer grokServer = MockRestServiceServer.bindTo(grokBuilder).build();
    grokServer
        .expect(requestTo("http://grok.test/chat/completions"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess("""
            {"choices":[{"message":{"role":"assistant","content":"respuesta de Grok"}}]}
            """, MediaType.APPLICATION_JSON));

    FallbackAdvisorChatClient client = new FallbackAdvisorChatClient(geminiAdapter(geminiBuilder), grokAdapter(grokBuilder));

    assertThat(client.reply(context, "cuanto gasté")).isEqualTo("respuesta de Grok");
    geminiServer.verify();
    grokServer.verify();
  }

  @Test
  void propagatesFallbackFailureWithPrimaryFailureSuppressedWhenBothFail() {
    RestClient.Builder geminiBuilder = RestClient.builder().baseUrl("http://gemini.test");
    MockRestServiceServer geminiServer = MockRestServiceServer.bindTo(geminiBuilder).build();
    geminiServer
        .expect(requestTo("http://gemini.test/chat/completions"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withServerError());

    RestClient.Builder grokBuilder = RestClient.builder().baseUrl("http://grok.test");
    MockRestServiceServer grokServer = MockRestServiceServer.bindTo(grokBuilder).build();
    grokServer
        .expect(requestTo("http://grok.test/chat/completions"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withServerError());

    FallbackAdvisorChatClient client = new FallbackAdvisorChatClient(geminiAdapter(geminiBuilder), grokAdapter(grokBuilder));

    assertThatThrownBy(() -> client.reply(context, "cuanto gasté"))
        .isInstanceOf(AdvisorUnavailableException.class)
        .satisfies(ex -> assertThat(ex.getSuppressed()).hasSize(1).allMatch(AdvisorUnavailableException.class::isInstance));
  }
}
