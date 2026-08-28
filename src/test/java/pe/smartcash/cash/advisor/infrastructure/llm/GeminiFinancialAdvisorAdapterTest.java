package pe.smartcash.cash.advisor.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import pe.smartcash.cash.advisor.domain.exception.AdvisorUnavailableException;
import pe.smartcash.cash.advisor.domain.services.FinancialContext;

/**
 * {@code MockRestServiceServer} (no un mock a mano de la cadena fluida de {@code RestClient})
 * porque los stubs de Mockito con {@code RETURNS_DEEP_STUBS} sobre una API fluida tan
 * genérica como {@code RestClient} resultaron poco confiables en la práctica -- esta es la
 * forma oficialmente soportada de testear un consumidor de {@code RestClient}.
 */
class GeminiFinancialAdvisorAdapterTest {

  private final FinancialContext context = new FinancialContext(BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, List.of());

  private RestClient.Builder newBuilder() {
    return RestClient.builder().baseUrl("http://llm.test");
  }

  @Test
  void shouldThrowWhenContentIsBlank() {
    RestClient.Builder builder = newBuilder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo("http://llm.test/chat/completions"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                """
                {"choices":[{"message":{"role":"assistant","content":"   "}}]}
                """,
                MediaType.APPLICATION_JSON));

    GeminiFinancialAdvisorAdapter adapter = new GeminiFinancialAdvisorAdapter(builder.build(), "gemini-3.6-flash");

    assertThatThrownBy(() -> adapter.reply(context, "dame recomendaciones de inversión")).isInstanceOf(AdvisorUnavailableException.class);
  }

  @Test
  void shouldReturnContentWhenNonBlank() {
    RestClient.Builder builder = newBuilder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo("http://llm.test/chat/completions"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                """
                {"choices":[{"message":{"role":"assistant","content":"Gastaste S/10 este mes."}}]}
                """,
                MediaType.APPLICATION_JSON));

    GeminiFinancialAdvisorAdapter adapter = new GeminiFinancialAdvisorAdapter(builder.build(), "gemini-3.6-flash");

    assertThat(adapter.reply(context, "cuanto gasté")).isEqualTo("Gastaste S/10 este mes.");
  }
}
