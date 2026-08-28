package pe.smartcash.cash.gmailsync.infrastructure.gmail;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * Solo prueba la construcción del query string (métodos de paquete {@code buildQuery}/{@code
 * buildCandidateQuery}), sin llamar a Gmail real -- este bounded context no tiene
 * infraestructura de test de integración hoy, y armar una contra Gmail real no es viable acá.
 */
class GoogleGmailApiAdapterQueryTest {

  private final GoogleGmailApiAdapter adapter = new GoogleGmailApiAdapter(RestClient.builder());

  @Test
  void buildQueryOrsTrustedDomainsAndAppendsSince() {
    Instant since = Instant.ofEpochSecond(1_700_000_000L);

    String query = adapter.buildQuery(since, Set.of("bcp.com.pe", "bbva.pe"));

    assertThat(query).contains("from:bcp.com.pe").contains("from:bbva.pe").contains(" OR ").endsWith("after:1700000000");
  }

  @Test
  void buildQueryOmitsSinceWhenNull() {
    String query = adapter.buildQuery(null, Set.of("bcp.com.pe"));

    assertThat(query).doesNotContain("after:");
  }

  @Test
  void buildCandidateQueryExcludesTrustedDomainsAndIncludesKeywords() {
    Instant since = Instant.ofEpochSecond(1_700_000_000L);

    String query = adapter.buildCandidateQuery(since, Set.of("bcp.com.pe", "bbva.pe"));

    assertThat(query)
        .contains("-from:bcp.com.pe")
        .contains("-from:bbva.pe")
        .contains("cargo")
        .contains("\"compra por\"")
        .endsWith("after:1700000000");
    // La búsqueda de candidatos nunca debe poder matchear un dominio ya confiable -- si
    // "from:bcp.com.pe" (sin el guion) apareciera acá, la query dejaría de ser disjunta con
    // buildQuery y un mismo correo podría ingestarse dos veces por dos caminos distintos.
    assertThat(query).doesNotContain(" from:bcp.com.pe");
  }

  @Test
  void buildCandidateQueryWithNoExcludedDomainsIsStillValid() {
    String query = adapter.buildCandidateQuery(null, Set.of());

    assertThat(query).doesNotContain("-from:").contains("cargo");
  }
}
