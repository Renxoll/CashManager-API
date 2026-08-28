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

/** Mismo patrón de test que {@link GeminiFinancialAdvisorAdapterTest} -- ver esa clase para
 * la justificación de usar {@code MockRestServiceServer}. */
class GrokFinancialAdvisorAdapterTest {

  private final FinancialContext context = new FinancialContext(BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, List.of());
  private final GrokProperties properties = new GrokProperties("http://grok.test", "test-key", "grok-4.6", Duration.ofSeconds(8));

  private RestClient.Builder newBuilder() {
    return RestClient.builder().baseUrl("http://grok.test");
  }

  @Test
  void shouldThrowWhenContentIsBlank() {
    RestClient.Builder builder = newBuilder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo("http://grok.test/chat/completions"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                """
                {"choices":[{"message":{"role":"assistant","content":"   "}}]}
                """,
                MediaType.APPLICATION_JSON));

    GrokFinancialAdvisorAdapter adapter = new GrokFinancialAdvisorAdapter(builder.build(), properties);

    assertThatThrownBy(() -> adapter.reply(context, "dame recomendaciones de inversión")).isInstanceOf(AdvisorUnavailableException.class);
  }

  @Test
  void shouldReturnContentWhenNonBlank() {
    RestClient.Builder builder = newBuilder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo("http://grok.test/chat/completions"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                """
                {"choices":[{"message":{"role":"assistant","content":"Gastaste S/10 este mes."}}]}
                """,
                MediaType.APPLICATION_JSON));

    GrokFinancialAdvisorAdapter adapter = new GrokFinancialAdvisorAdapter(builder.build(), properties);

    assertThat(adapter.reply(context, "cuanto gasté")).isEqualTo("Gastaste S/10 este mes.");
  }
}
