package pe.smartcash.cash.subscription.infrastructure.payment;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pe.smartcash.cash.subscription.domain.exception.PaymentGatewayException;
import pe.smartcash.cash.subscription.domain.model.valueobjects.PlanCode;
import pe.smartcash.cash.subscription.domain.services.PaymentRequest;
import pe.smartcash.cash.subscription.domain.services.PaymentResult;
import pe.smartcash.cash.subscription.domain.services.SubscriptionPaymentGateway;
import pe.smartcash.cash.subscription.domain.services.VerifiedPaymentEvent;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Único punto del contexto que conoce el API de suscripciones de Mercado Pago
 * ({@code /preapproval}, ver {@code https://www.mercadopago.com.pe/developers}). Activo solo con
 * {@code app.payments.provider=mercadopago}.
 *
 * <p>Suscribir es una sola llamada: {@code POST /preapproval} con el {@code card_token_id} que
 * generó MercadoPago.js en el navegador y {@code status=authorized}. El {@code external_reference}
 * lleva el id de usuario.
 *
 * <p>Los webhooks de Mercado Pago solo traen el tipo y el id del recurso. Como el puerto recibe
 * un único {@code eventId}, acá es {@code "<tipo>:<id>"} (lo arma {@link #eventId}); el estado
 * real se vuelve a leer con nuestro Access Token, igual que con Culqi.
 *
 * <p>3DS: el checkout de Mercado Pago lo resuelve en el navegador al tokenizar, por eso este
 * adaptador nunca responde {@code AuthenticationRequired}.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.payments.provider", havingValue = "mercadopago")
class MercadoPagoPaymentGatewayAdapter implements SubscriptionPaymentGateway {

  static final String TYPE_PREAPPROVAL = "subscription_preapproval";
  static final String TYPE_AUTHORIZED_PAYMENT = "subscription_authorized_payment";

  private static final String DEFAULT_DECLINED_MESSAGE = "No se pudo procesar la tarjeta. Revisa los datos e intenta de nuevo.";

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final MercadoPagoProperties properties;

  MercadoPagoPaymentGatewayAdapter(
      @Qualifier("mercadoPagoRestClient") RestClient restClient, ObjectMapper objectMapper, MercadoPagoProperties properties) {
    this.restClient = restClient;
    this.objectMapper = objectMapper;
    this.properties = properties;
  }

  /** Id de evento que viaja por el puerto: tipo de notificación + id del recurso. */
  static String eventId(String type, String dataId) {
    return type + ":" + dataId;
  }

  @Override
  public PaymentResult subscribe(PaymentRequest request) {
    if (request.planCode() != PlanCode.PREMIUM) {
      throw new IllegalArgumentException("El plan " + request.planCode() + " no se cobra con Mercado Pago");
    }
    Map<String, Object> body = new LinkedHashMap<>();
    if (hasText(properties.premiumPlanId())) {
      body.put("preapproval_plan_id", properties.premiumPlanId());
    }
    body.put("reason", "Luki Premium");
    body.put("external_reference", request.userId().value().toString());
    body.put("payer_email", request.payer().email());
    body.put("card_token_id", request.cardTokenId());
    if (properties.premiumAmount() != null && properties.premiumAmount().signum() > 0) {
      body.put("auto_recurring", autoRecurring(properties.premiumAmount()));
    }
    body.put("back_url", properties.backUrl());
    body.put("status", "authorized");

    // Reintentar con el mismo token no crea una segunda suscripción.
    String idempotencyKey =
        UUID.nameUUIDFromBytes((request.userId().value() + ":" + request.cardTokenId()).getBytes(StandardCharsets.UTF_8)).toString();
    MpResponse response = call(() -> restClient
        .post()
        .uri("/preapproval")
        .header("X-Idempotency-Key", idempotencyKey)
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .exchange((req, res) -> read(res)));

    if (response.isSuccess()) {
      String status = response.body().path("status").asString("");
      String id = response.body().path("id").asString(null);
      if ("authorized".equals(status) && id != null) {
        return new PaymentResult.Subscribed(id);
      }
      // "pending" u otro: la tarjeta no quedó autorizada y no se cobrará.
      log.warn("Mercado Pago creó la suscripción {} con estado {} (se esperaba authorized)", id, status);
      return new PaymentResult.Declined(DEFAULT_DECLINED_MESSAGE);
    }
    // 400/422 son por la tarjeta o los datos que mandó el usuario; el resto (401/403: credenciales,
    // 429, 5xx) es nuestra falla o del proveedor.
    if (response.status() == 400 || response.status() == 422) {
      log.info("Mercado Pago rechazó la suscripción (HTTP {}): {}", response.status(), response.body());
      return new PaymentResult.Declined(DEFAULT_DECLINED_MESSAGE);
    }
    throw new PaymentGatewayException(
        "Mercado Pago falló al crear la suscripción (HTTP " + response.status() + "): " + response.body(), null);
  }

  private Map<String, Object> autoRecurring(BigDecimal amount) {
    Map<String, Object> recurring = new LinkedHashMap<>();
    recurring.put("frequency", 1);
    recurring.put("frequency_type", "months");
    recurring.put("transaction_amount", amount);
    recurring.put("currency_id", properties.currency());
    return recurring;
  }

  @Override
  public void cancel(String providerSubscriptionId) {
    MpResponse response = call(() -> restClient
        .put()
        .uri("/preapproval/{id}", providerSubscriptionId)
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("status", "cancelled"))
        .exchange((req, res) -> read(res)));
    if (response.isSuccess()) {
      return;
    }
    // Idempotente: si ya estaba cancelada, no es un error. Se confirma leyendo su estado.
    MpResponse current = get("/preapproval/{id}", providerSubscriptionId);
    if (current.isSuccess() && "cancelled".equals(current.body().path("status").asString(""))) {
      return;
    }
    throw new PaymentGatewayException(
        "Mercado Pago no canceló la suscripción " + providerSubscriptionId + " (HTTP " + response.status() + "): " + response.body(), null);
  }

  @Override
  public Optional<VerifiedPaymentEvent> verifyEvent(String eventId) {
    int separator = eventId.indexOf(':');
    if (separator <= 0 || separator == eventId.length() - 1) {
      return Optional.empty();
    }
    String type = eventId.substring(0, separator);
    String dataId = eventId.substring(separator + 1);
    return switch (type) {
      case TYPE_PREAPPROVAL -> verifyPreapproval(eventId, dataId);
      case TYPE_AUTHORIZED_PAYMENT -> verifyAuthorizedPayment(eventId, dataId);
      default -> Optional.empty();
    };
  }

  /** Solo la baja importa: las altas ya las activa {@link #subscribe}. */
  private Optional<VerifiedPaymentEvent> verifyPreapproval(String eventId, String preapprovalId) {
    MpResponse response = fetch("/preapproval/{id}", preapprovalId);
    if (response.status() == 404) {
      log.warn("Webhook de Mercado Pago con una suscripción que no existe: {}", preapprovalId);
      return Optional.empty();
    }
    if ("cancelled".equals(response.body().path("status").asString(""))) {
      return Optional.of(new VerifiedPaymentEvent(eventId, VerifiedPaymentEvent.Type.SUBSCRIPTION_CANCELED, preapprovalId));
    }
    return Optional.empty();
  }

  /** Cobro de un período: {@code processed} + pago aprobado es éxito; {@code recycling} o rechazado, falla (se reintenta). */
  private Optional<VerifiedPaymentEvent> verifyAuthorizedPayment(String eventId, String authorizedPaymentId) {
    MpResponse response = fetch("/authorized_payments/{id}", authorizedPaymentId);
    if (response.status() == 404) {
      log.warn("Webhook de Mercado Pago con un cobro que no existe: {}", authorizedPaymentId);
      return Optional.empty();
    }
    String preapprovalId = response.body().path("preapproval_id").asString(null);
    if (preapprovalId == null) {
      log.warn("Cobro {} de Mercado Pago sin preapproval_id", authorizedPaymentId);
      return Optional.empty();
    }
    String status = response.body().path("status").asString("");
    String paymentStatus = response.body().path("payment").path("status").asString("");
    VerifiedPaymentEvent.Type type = null;
    if ("processed".equals(status) && "approved".equals(paymentStatus)) {
      type = VerifiedPaymentEvent.Type.CHARGE_SUCCEEDED;
    } else if ("recycling".equals(status) || "rejected".equals(paymentStatus)) {
      type = VerifiedPaymentEvent.Type.CHARGE_FAILED;
    }
    return type == null ? Optional.empty() : Optional.of(new VerifiedPaymentEvent(eventId, type, preapprovalId));
  }

  /** GET que distingue 404 (evento inexistente) de una falla real del proveedor (502). */
  private MpResponse fetch(String uri, String id) {
    MpResponse response = get(uri, id);
    if (!response.isSuccess() && response.status() != 404) {
      throw new PaymentGatewayException("No se pudo verificar " + id + " en Mercado Pago (HTTP " + response.status() + "): " + response.body(), null);
    }
    return response;
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private MpResponse get(String uri, Object... uriVariables) {
    return call(() -> restClient.get().uri(uri, uriVariables).exchange((req, res) -> read(res)));
  }

  private MpResponse read(RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response) throws IOException {
    int status = response.getStatusCode().value();
    byte[] bytes = response.getBody().readAllBytes();
    JsonNode body;
    try {
      body = bytes.length == 0 ? objectMapper.missingNode() : objectMapper.readTree(bytes);
    } catch (RuntimeException notJson) {
      body = objectMapper.missingNode();
    }
    return new MpResponse(status, body);
  }

  private static MpResponse call(java.util.function.Supplier<MpResponse> request) {
    try {
      return request.get();
    } catch (RestClientException e) {
      throw new PaymentGatewayException("No se pudo conectar con Mercado Pago", e);
    }
  }

  private record MpResponse(int status, JsonNode body) {

    boolean isSuccess() {
      return status >= 200 && status < 300;
    }
  }
}
