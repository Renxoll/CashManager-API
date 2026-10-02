package pe.smartcash.cash.subscription.infrastructure.payment;

import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

/**
 * Extrae el id de la suscripción ({@code sxn_live_...} / {@code sxn_test_...}) del {@code data}
 * de un evento de Culqi. La documentación de Culqi no publica la forma exacta de {@code data}
 * para cada evento de suscripción (puede ser la suscripción misma o el cargo que la generó), así
 * que se prueban primero los campos conocidos y, si no, se busca el primer valor con el prefijo
 * {@code sxn_}: ese prefijo lo usa Culqi solo para suscripciones.
 */
final class CulqiEventData {

  private static final String SUBSCRIPTION_ID_PREFIX = "sxn_";
  private static final List<String> KNOWN_FIELDS = List.of("id", "subscription_id", "subscriptionId", "subscription");
  private static final int MAX_DEPTH = 5;

  private CulqiEventData() {}

  static String findSubscriptionId(JsonNode data) {
    if (data == null || data.isMissingNode() || data.isNull()) {
      return null;
    }
    for (String field : KNOWN_FIELDS) {
      String candidate = subscriptionIdIn(data.path(field));
      if (candidate != null) {
        return candidate;
      }
    }
    return scan(data, 0);
  }

  private static String subscriptionIdIn(JsonNode node) {
    if (node.isString() && node.asString().startsWith(SUBSCRIPTION_ID_PREFIX)) {
      return node.asString();
    }
    if (node.isObject()) {
      JsonNode id = node.path("id");
      if (id.isString() && id.asString().startsWith(SUBSCRIPTION_ID_PREFIX)) {
        return id.asString();
      }
    }
    return null;
  }

  private static String scan(JsonNode node, int depth) {
    if (depth > MAX_DEPTH) {
      return null;
    }
    if (node.isString()) {
      return node.asString().startsWith(SUBSCRIPTION_ID_PREFIX) ? node.asString() : null;
    }
    if (node.isObject()) {
      for (Map.Entry<String, JsonNode> entry : node.properties()) {
        String found = scan(entry.getValue(), depth + 1);
        if (found != null) {
          return found;
        }
      }
    }
    if (node.isArray()) {
      for (JsonNode element : node) {
        String found = scan(element, depth + 1);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }
}
