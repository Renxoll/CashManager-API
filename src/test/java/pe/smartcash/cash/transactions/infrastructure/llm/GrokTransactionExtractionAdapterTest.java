package pe.smartcash.cash.transactions.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import pe.smartcash.cash.shared.infrastructure.llm.GrokProperties;
import pe.smartcash.cash.transactions.domain.exception.TransactionExtractionFailedException;
import tools.jackson.databind.ObjectMapper;

/** Mismo patrón {@code MockRestServiceServer} que {@code GeminiFinancialAdvisorAdapterTest} --
 * ver esa clase para la justificación. */
class GrokTransactionExtractionAdapterTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final GrokProperties properties = new GrokProperties("http://grok.test", "test-key", "grok-4.6", Duration.ofSeconds(8));

  private RestClient.Builder newBuilder() {
    return RestClient.builder().baseUrl("http://grok.test");
  }

  @Test
  void shouldExtractFromAValidJsonResponse() {
    RestClient.Builder builder = newBuilder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo("http://grok.test/chat/completions"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                """
                {"choices":[{"message":{"role":"assistant","content":"{\\"monto\\":6.50,\\"moneda\\":\\"PEN\\",\\"comercio\\":\\"Feel Good Villa\\",\\"categoria\\":\\"COMIDA\\"}"}}]}
                """,
                MediaType.APPLICATION_JSON));

    GrokTransactionExtractionAdapter adapter = new GrokTransactionExtractionAdapter(builder.build(), properties, objectMapper);

    var result = adapter.extract("Realizaste un consumo de S/ 6.50 con tu Tarjeta de Débito BCP en Feel Good Villa.");

    assertThat(result.money().amount()).isEqualByComparingTo("6.50");
    assertThat(result.merchant().name()).isEqualTo("Feel Good Villa");
  }

  @Test
  void shouldThrowWhenNoChoicesComeBack() {
    RestClient.Builder builder = newBuilder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo("http://grok.test/chat/completions"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess("""
            {"choices":[]}
            """, MediaType.APPLICATION_JSON));

    GrokTransactionExtractionAdapter adapter = new GrokTransactionExtractionAdapter(builder.build(), properties, objectMapper);

    assertThatThrownBy(() -> adapter.extract("texto cualquiera")).isInstanceOf(TransactionExtractionFailedException.class);
  }
}
