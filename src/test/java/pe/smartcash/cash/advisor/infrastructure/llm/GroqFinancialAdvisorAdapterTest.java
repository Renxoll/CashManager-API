package pe.smartcash.cash.advisor.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
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
import pe.smartcash.cash.shared.infrastructure.llm.GroqProperties;

/** Mismo patrón de test que {@link GeminiFinancialAdvisorAdapterTest} -- ver esa clase para
 * la justificación de usar {@code MockRestServiceServer}. */
class GroqFinancialAdvisorAdapterTest {

  private final FinancialContext context = new FinancialContext(BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, List.of());
  private final GroqProperties properties = new GroqProperties("http://groq.test", "test-key", "llama-3.3-70b-versatile", Duration.ofSeconds(8));

  private RestClient.Builder newBuilder() {
    return RestClient.builder().baseUrl("http://groq.test");
  }

  @Test
  void shouldThrowWhenContentIsBlank() {
    RestClient.Builder builder = newBuilder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo("http://groq.test/chat/completions"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                """
                {"choices":[{"message":{"role":"assistant","content":"   "}}]}
                """,
                MediaType.APPLICATION_JSON));

    GroqFinancialAdvisorAdapter adapter = new GroqFinancialAdvisorAdapter(builder.build(), properties);

    assertThatThrownBy(() -> adapter.reply(context, "dame recomendaciones de inversión")).isInstanceOf(AdvisorUnavailableException.class);
  }

  @Test
  void shouldReturnContentWhenNonBlank() {
    RestClient.Builder builder = newBuilder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo("http://groq.test/chat/completions"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                """
                {"choices":[{"message":{"role":"assistant","content":"Gastaste S/10 este mes."}}]}
                """,
                MediaType.APPLICATION_JSON));

    GroqFinancialAdvisorAdapter adapter = new GroqFinancialAdvisorAdapter(builder.build(), properties);

    assertThat(adapter.reply(context, "cuanto gasté")).isEqualTo("Gastaste S/10 este mes.");
  }
}
