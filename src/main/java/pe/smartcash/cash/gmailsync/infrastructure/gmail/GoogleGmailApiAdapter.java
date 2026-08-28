package pe.smartcash.cash.gmailsync.infrastructure.gmail;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pe.smartcash.cash.gmailsync.domain.services.GmailMessage;
import pe.smartcash.cash.gmailsync.domain.services.GmailMessagePort;

/**
 * Se usa {@code format=full} (JSON con el body en base64url) en vez de {@code format=raw}
 * (RFC822 crudo) a propósito: parsear MIME multipart a mano sería reinventar una librería
 * entera. El JSON de Gmail ya trae el árbol de {@code parts} resuelto, solo hay que
 * recorrerlo buscando la primera parte {@code text/plain} (o {@code text/html} como
 * fallback, con un strip de tags simple) -- mismo criterio "sin dependencia nueva si se
 * puede evitar" que el adaptador del LLM.
 */
@Component
class GoogleGmailApiAdapter implements GmailMessagePort {

  private static final String BASE_URL = "https://gmail.googleapis.com/gmail/v1/users/me";

  // Heurístico de términos transaccionales en español para descubrir posibles notificaciones
  // de banco/billetera de dominios todavía no confiables (ver findCandidateMessagesSince).
  // Ajustable -- no es una garantía de precisión, solo acota el ruido frente a "toda la
  // bandeja" (el candidato de todos modos nunca se ingesta directo, solo se encola).
  private static final List<String> CANDIDATE_KEYWORDS =
      List.of("S/", "cargo", "consumo", "abono", "transferencia", "compra por", "notificación de operación");

  private final RestClient.Builder restClientBuilder;

  GoogleGmailApiAdapter(RestClient.Builder restClientBuilder) {
    this.restClientBuilder = restClientBuilder;
  }

  @Override
  public List<GmailMessage> findMatchingMessagesSince(String accessToken, Instant since, Set<String> senderDomains) {
    return search(accessToken, buildQuery(since, senderDomains));
  }

  @Override
  public List<GmailMessage> findCandidateMessagesSince(String accessToken, Instant since, Set<String> excludeDomains) {
    return search(accessToken, buildCandidateQuery(since, excludeDomains));
  }

  private List<GmailMessage> search(String accessToken, String query) {
    RestClient restClient = restClientBuilder.build();

    GmailMessageListResponse listResponse =
        restClient
            .get()
            .uri(BASE_URL + "/messages?q={q}", query)
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .body(GmailMessageListResponse.class);

    if (listResponse == null || listResponse.messages() == null) {
      return List.of();
    }

    return listResponse.messages().stream()
        .map(ref -> fetchMessage(restClient, accessToken, ref.id()))
        .filter(Objects::nonNull)
        .toList();
  }

  private GmailMessage fetchMessage(RestClient restClient, String accessToken, String messageId) {
    GmailFullMessage full =
        restClient
            .get()
            .uri(BASE_URL + "/messages/{id}?format=full", messageId)
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .body(GmailFullMessage.class);

    if (full == null || full.payload() == null) {
      return null;
    }
    String from = findHeader(full.payload(), "From");
    String text = extractText(full.payload());
    if (from == null || text == null || text.isBlank()) {
      return null;
    }
    return new GmailMessage(from, text);
  }

  // Visibilidad de paquete (no private) a propósito: permite testear la construcción del
  // query string con un test unitario acotado, sin necesitar credenciales ni red real.
  String buildQuery(Instant since, Set<String> senderDomains) {
    String fromClause = senderDomains.stream().map(domain -> "from:" + domain).collect(Collectors.joining(" OR ", "(", ")"));
    return withSince(fromClause, since);
  }

  /**
   * Términos transaccionales OR'd, excluyendo a nivel de query los dominios ya globalmente
   * confiables ({@code -from:dominio}) -- así esta búsqueda y {@link #buildQuery} nunca
   * pueden matchear el mismo mensaje, sin necesitar una columna de deduplicación por
   * message-id.
   */
  String buildCandidateQuery(Instant since, Set<String> excludeDomains) {
    String keywordClause = CANDIDATE_KEYWORDS.stream().map(this::quoteIfNeeded).collect(Collectors.joining(" OR ", "(", ")"));
    String exclusions = excludeDomains.stream().map(domain -> "-from:" + domain).collect(Collectors.joining(" "));
    String query = exclusions.isBlank() ? keywordClause : keywordClause + " " + exclusions;
    return withSince(query, since);
  }

  private String quoteIfNeeded(String keyword) {
    return keyword.contains(" ") ? "\"" + keyword + "\"" : keyword;
  }

  private String withSince(String query, Instant since) {
    return since != null ? query + " after:" + since.getEpochSecond() : query;
  }

  private String findHeader(GmailMessagePayload payload, String name) {
    if (payload.headers() == null) {
      return null;
    }
    return payload.headers().stream().filter(h -> name.equalsIgnoreCase(h.name())).map(GmailHeader::value).findFirst().orElse(null);
  }

  private String extractText(GmailMessagePayload payload) {
    String plain = findPart(payload, "text/plain");
    if (plain != null) {
      return plain;
    }
    String html = findPart(payload, "text/html");
    return html != null ? stripHtml(html) : null;
  }

  private String findPart(GmailMessagePayload payload, String mimeTypePrefix) {
    if (payload.mimeType() != null
        && payload.mimeType().startsWith(mimeTypePrefix)
        && payload.body() != null
        && payload.body().data() != null) {
      return decodeBase64Url(payload.body().data());
    }
    if (payload.parts() != null) {
      for (GmailMessagePayload part : payload.parts()) {
        String found = findPart(part, mimeTypePrefix);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private String decodeBase64Url(String data) {
    return new String(Base64.getUrlDecoder().decode(data), StandardCharsets.UTF_8);
  }

  private String stripHtml(String html) {
    return html.replaceAll("<[^>]+>", " ").replaceAll("&nbsp;", " ").replaceAll("\\s+", " ").trim();
  }
}
