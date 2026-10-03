package pe.smartcash.cash.subscription.infrastructure.payment;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import pe.smartcash.cash.subscription.domain.exception.PaymentGatewayException;
import pe.smartcash.cash.subscription.domain.model.commands.PayPremiumCommand.ThreeDSecureParameters;
import pe.smartcash.cash.subscription.domain.model.valueobjects.PlanCode;
import pe.smartcash.cash.subscription.domain.services.PaymentRequest;
import pe.smartcash.cash.subscription.domain.services.PaymentResult;
import pe.smartcash.cash.subscription.domain.services.SubscriptionPaymentGateway;
import pe.smartcash.cash.subscription.domain.services.VerifiedPaymentEvent;

/**
 * Único punto del contexto que conoce el API de Culqi (v2, ver {@code https://apidocs.culqi.com}).
 * Suscribir son tres llamadas en orden: cliente ({@code cus_}), tarjeta ({@code crd_}, a partir
 * del token {@code tkn_} que generó el checkout en el navegador) y suscripción ({@code sxn_})
 * al plan configurado en el CulqiPanel.
 *
 * <p>Las respuestas se leen como {@link JsonNode} y no como DTOs tipados: el API de Culqi
 * mezcla formatos (un 200 con {@code action_code} para pedir 3DS frente a un 201 con la tarjeta;
 * el {@code data} de un evento a veces es un objeto y a veces un string con JSON adentro), y
 * acá solo se leen un puñado de campos.
 */
@Slf4j
@Component
class CulqiPaymentGatewayAdapter implements SubscriptionPaymentGateway {

  private static final String ACTION_CODE_REVIEW = "REVIEW";
  /** Errores causados por lo que ingresó el usuario (tarjeta o datos): se le muestran como rechazo, no como caída. */
  private static final Set<String> USER_ERROR_TYPES = Set.of("card_error", "parameter_error", "invalid_request_error");
  /** Estados de suscripción en Culqi que ya no cobran: 4 = Cancelada, 6 = Vencida. */
  private static final Set<Integer> ENDED_SUBSCRIPTION_STATUSES = Set.of(4, 6);

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final CulqiProperties properties;

  CulqiPaymentGatewayAdapter(@Qualifier("culqiRestClient") RestClient restClient, ObjectMapper objectMapper, CulqiProperties properties) {
    this.restClient = restClient;
    this.objectMapper = objectMapper;
    this.properties = properties;
  }

  @Override
  public PaymentResult subscribe(PaymentRequest request) {
    String planId = planIdFor(request.planCode());

    CulqiResponse customer = findOrCreateCustomer(request);
    if (!customer.isSuccess()) {
      return declinedOrFail(customer, "crear el cliente");
    }
    String customerId = customer.body().path("id").asString();

    CulqiResponse card = post("/cards", cardBody(customerId, request));
    if (ACTION_CODE_REVIEW.equals(card.body().path("action_code").asString(null))) {
      // El antifraude de Culqi exige 3DS. Sin cobro ni tarjeta creada: el frontend autentica y
      // reintenta con el mismo token (válido 5 minutos) más los parámetros 3DS.
      return new PaymentResult.AuthenticationRequired();
    }
    if (!card.isSuccess()) {
      return declinedOrFail(card, "registrar la tarjeta");
    }
    String cardId = card.body().path("id").asString();

    Map<String, Object> subscriptionBody = new LinkedHashMap<>();
    subscriptionBody.put("card_id", cardId);
    subscriptionBody.put("plan_id", planId);
    // El controller ya exige acceptedTerms=true; Culqi pide que conste en la suscripción.
    subscriptionBody.put("tyc", true);
    subscriptionBody.put("metadata", Map.of("user_id", request.userId().value().toString()));
    CulqiResponse subscription = post("/recurrent/subscriptions/create", subscriptionBody);
    if (!subscription.isSuccess()) {
      return declinedOrFail(subscription, "crear la suscripción");
    }
    return new PaymentResult.Subscribed(subscription.body().path("id").asString());
  }

  /**
   * Culqi rechaza crear dos clientes con el mismo email, y un usuario que vuelve a suscribirse
   * (o que reintenta tras 3DS) ya tiene el suyo: se busca primero por email.
   */
  private CulqiResponse findOrCreateCustomer(PaymentRequest request) {
    PaymentRequest.Payer payer = request.payer();
    CulqiResponse existing = get("/customers?email={email}", payer.email());
    JsonNode found = existing.body().path("data").path(0);
    if (existing.isSuccess() && found.hasNonNull("id")) {
      return new CulqiResponse(200, found);
    }
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("first_name", payer.firstName());
    body.put("last_name", payer.lastName());
    body.put("email", payer.email());
    body.put("address", payer.address());
    body.put("address_city", payer.addressCity());
    body.put("country_code", payer.countryCode());
    body.put("phone_number", payer.phoneNumber());
    body.put("metadata", Map.of("user_id", request.userId().value().toString()));
    return post("/customers", body);
  }

  private static Map<String, Object> cardBody(String customerId, PaymentRequest request) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("customer_id", customerId);
    body.put("token_id", request.cardTokenId());
    ThreeDSecureParameters threeDs = request.authentication3ds();
    if (threeDs != null) {
      Map<String, Object> auth = new LinkedHashMap<>();
      putIfPresent(auth, "eci", threeDs.eci());
      putIfPresent(auth, "xid", threeDs.xid());
      putIfPresent(auth, "cavv", threeDs.cavv());
      putIfPresent(auth, "protocolVersion", threeDs.protocolVersion());
      putIfPresent(auth, "directoryServerTransactionId", threeDs.directoryServerTransactionId());
      body.put("authentication_3DS", auth);
    }
    return body;
  }

  @Override
  public void cancel(String providerSubscriptionId) {
    CulqiResponse response = delete("/recurrent/subscriptions/{id}", providerSubscriptionId);
    if (response.isSuccess()) {
      return;
    }
    // Idempotente: si Culqi no la canceló porque ya estaba terminada (o el usuario reintenta),
    // no es un error. Se confirma leyendo su estado en vez de interpretar el mensaje de error.
    CulqiResponse current = get("/recurrent/subscriptions/{id}", providerSubscriptionId);
    if (current.isSuccess() && ENDED_SUBSCRIPTION_STATUSES.contains(current.body().path("status").asInt(-1))) {
      return;
    }
    throw new PaymentGatewayException(
        "Culqi no canceló la suscripción " + providerSubscriptionId + ": " + merchantMessage(response), null);
  }

  @Override
  public Optional<VerifiedPaymentEvent> verifyEvent(String eventId) {
    CulqiResponse response = get("/events/{id}", eventId);
    if (response.status() == 404) {
      log.warn("Webhook de Culqi con un evento que no existe en Culqi: {}", eventId);
      return Optional.empty();
    }
    if (!response.isSuccess()) {
      throw new PaymentGatewayException("No se pudo verificar el evento " + eventId + " en Culqi: " + merchantMessage(response), null);
    }
    String type = response.body().path("type").asString("");
    VerifiedPaymentEvent.Type mapped =
        switch (type) {
          case "subscription.charge.succeeded" -> VerifiedPaymentEvent.Type.CHARGE_SUCCEEDED;
          case "subscription.charge.failed" -> VerifiedPaymentEvent.Type.CHARGE_FAILED;
          case "subscription.cancel.succeeded" -> VerifiedPaymentEvent.Type.SUBSCRIPTION_CANCELED;
          default -> null;
        };
    if (mapped == null) {
      return Optional.empty();
    }
    String subscriptionId = CulqiEventData.findSubscriptionId(parseData(response.body().path("data")));
    if (subscriptionId == null) {
      log.warn("Evento {} de Culqi ({}) sin id de suscripción reconocible", eventId, type);
      return Optional.empty();
    }
    return Optional.of(new VerifiedPaymentEvent(eventId, mapped, subscriptionId));
  }

  /** El {@code data} de un evento llega como objeto al consultarlo, pero la doc lo describe como string con JSON. */
  private JsonNode parseData(JsonNode data) {
    if (!data.isString()) {
      return data;
    }
    try {
      return objectMapper.readTree(data.asString());
    } catch (RuntimeException notJson) {
      return objectMapper.missingNode();
    }
  }

  private String planIdFor(PlanCode planCode) {
    if (planCode != PlanCode.PREMIUM) {
      throw new IllegalArgumentException("El plan " + planCode + " no se cobra con Culqi");
    }
    return properties.premiumPlanId();
  }

  /**
   * Un error de Culqi por la tarjeta o por los datos del usuario es un rechazo que se le muestra
   * (con el {@code user_message} que redacta Culqi); cualquier otro (llave inválida, Culqi
   * caído, límite de API) es una falla nuestra o del proveedor: 502.
   */
  private static PaymentResult declinedOrFail(CulqiResponse response, String step) {
    String errorType = response.body().path("type").asString("");
    if (response.status() >= 400 && response.status() < 500 && USER_ERROR_TYPES.contains(errorType)) {
      String userMessage = response.body().path("user_message").asString(null);
      return new PaymentResult.Declined(userMessage != null ? userMessage : "No se pudo procesar la tarjeta. Revisa los datos e intenta de nuevo.");
    }
    throw new PaymentGatewayException(
        "Culqi falló al " + step + " (HTTP " + response.status() + ", " + errorType + "): " + merchantMessage(response), null);
  }

  private static String merchantMessage(CulqiResponse response) {
    return response.body().path("merchant_message").asString(response.body().toString());
  }

  private static void putIfPresent(Map<String, Object> map, String key, String value) {
    if (value != null) {
      map.put(key, value);
    }
  }

  private CulqiResponse get(String uri, Object... uriVariables) {
    return call(() -> restClient.get().uri(uri, uriVariables).exchange((request, response) -> read(response)));
  }

  private CulqiResponse post(String uri, Object body) {
    return call(() -> restClient.post().uri(uri).contentType(MediaType.APPLICATION_JSON).body(body).exchange((request, response) -> read(response)));
  }

  private CulqiResponse delete(String uri, Object... uriVariables) {
    return call(() -> restClient.delete().uri(uri, uriVariables).exchange((request, response) -> read(response)));
  }

  private CulqiResponse read(RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response) throws IOException {
    int status = response.getStatusCode().value();
    byte[] bytes = response.getBody().readAllBytes();
    JsonNode body;
    try {
      body = bytes.length == 0 ? objectMapper.missingNode() : objectMapper.readTree(bytes);
    } catch (RuntimeException notJson) {
      body = objectMapper.missingNode();
    }
    return new CulqiResponse(status, body);
  }

  private static CulqiResponse call(java.util.function.Supplier<CulqiResponse> request) {
    try {
      return request.get();
    } catch (RestClientException e) {
      // Timeout, DNS, conexión rechazada: Culqi no respondió.
      throw new PaymentGatewayException("No se pudo conectar con Culqi", e);
    }
  }

  private record CulqiResponse(int status, JsonNode body) {

    boolean isSuccess() {
      return status >= 200 && status < 300;
    }
  }
}
