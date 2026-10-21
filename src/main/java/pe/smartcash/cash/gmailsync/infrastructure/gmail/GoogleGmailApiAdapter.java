package pe.smartcash.cash.gmailsync.infrastructure.gmail;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import pe.smartcash.cash.gmailsync.domain.exception.GmailAuthorizationException;
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

    GmailMessageListResponse listResponse;
    try {
      listResponse =
          restClient
              .get()
              .uri(BASE_URL + "/messages?q={q}", query)
              .header("Authorization", "Bearer " + accessToken)
              .retrieve()
              .body(GmailMessageListResponse.class);
    } catch (HttpClientErrorException e) {
      throw translateOrRethrow(e);
    }

    if (listResponse == null || listResponse.messages() == null) {
      return List.of();
    }

    return listResponse.messages().stream()
        .map(ref -> fetchMessage(restClient, accessToken, ref.id()))
        .filter(Objects::nonNull)
        .toList();
  }

  /**
   * 401 (token inválido/expirado que el refresh no arregló) y 403 (típicamente {@code
   * ACCESS_TOKEN_SCOPE_INSUFFICIENT}: el grant no incluye {@code gmail.readonly}) son
   * problemas de autorización que solo el usuario reconectando resuelve. El resto (429, 5xx)
   * se relanza tal cual: son transitorios y el próximo poll reintenta.
   */
  private static RuntimeException translateOrRethrow(HttpClientErrorException e) {
    if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
      return new GmailAuthorizationException("Gmail rechazó el acceso (" + e.getStatusCode() + "): " + e.getStatusText(), e);
    }
    return e;
  }

  private GmailMessage fetchMessage(RestClient restClient, String accessToken, String messageId) {
    GmailFullMessage full;
    try {
      full =
          restClient
              .get()
              .uri(BASE_URL + "/messages/{id}?format=full", messageId)
              .header("Authorization", "Bearer " + accessToken)
              .retrieve()
              .body(GmailFullMessage.class);
    } catch (HttpClientErrorException e) {
      throw translateOrRethrow(e);
    }

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

  /**
   * Los correos de Yape y varios bancos son HTML de marketing con un {@code <head>} y bloques
   * {@code <style>} enormes: quitar solo las etiquetas dejaba todo ese CSS como "texto", y el
   * monto y el comercio quedaban enterrados al final -- o el mensaje excedía lo que el LLM
   * acepta y la extracción fallaba. Por eso se descartan {@code head/style/script} y los
   * comentarios con su contenido, y se decodifican las entidades (un "Cr&eacute;dito" no debe
   * llegar así al LLM ni al parser).
   */
  static String stripHtml(String html) {
    String text =
        html.replaceAll("(?is)<!--.*?-->", " ")
            .replaceAll("(?is)<(head|style|script)\\b[^>]*>.*?</\\1\\s*>", " ")
            .replaceAll("<[^>]+>", " ");
    return decodeEntities(text).replaceAll("\\s+", " ").trim();
  }

  private static final Pattern NUMERIC_ENTITY = Pattern.compile("&#(x?)([0-9a-fA-F]{1,6});");
  private static final Map<String, String> NAMED_ENTITIES =
      Map.ofEntries(
          Map.entry("&nbsp;", " "), Map.entry("&amp;", "&"), Map.entry("&lt;", "<"), Map.entry("&gt;", ">"),
          Map.entry("&quot;", "\""), Map.entry("&apos;", "'"), Map.entry("&aacute;", "á"), Map.entry("&eacute;", "é"),
          Map.entry("&iacute;", "í"), Map.entry("&oacute;", "ó"), Map.entry("&uacute;", "ú"), Map.entry("&ntilde;", "ñ"),
          Map.entry("&Aacute;", "Á"), Map.entry("&Eacute;", "É"), Map.entry("&Iacute;", "Í"), Map.entry("&Oacute;", "Ó"),
          Map.entry("&Uacute;", "Ú"), Map.entry("&Ntilde;", "Ñ"), Map.entry("&iexcl;", "¡"), Map.entry("&iquest;", "¿"));

  private static String decodeEntities(String text) {
    String result = text;
    for (Map.Entry<String, String> entity : NAMED_ENTITIES.entrySet()) {
      result = result.replace(entity.getKey(), entity.getValue());
    }
    Matcher matcher = NUMERIC_ENTITY.matcher(result);
    StringBuilder out = new StringBuilder();
    while (matcher.find()) {
      int codePoint = Integer.parseInt(matcher.group(2), matcher.group(1).isEmpty() ? 10 : 16);
      String replacement = Character.isValidCodePoint(codePoint) ? new String(Character.toChars(codePoint)) : " ";
      matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(out);
    return out.toString();
  }
}
