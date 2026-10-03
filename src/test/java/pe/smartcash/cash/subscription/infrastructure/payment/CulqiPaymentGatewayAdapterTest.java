package pe.smartcash.cash.subscription.infrastructure.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;
import pe.smartcash.cash.subscription.domain.exception.PaymentGatewayException;
import pe.smartcash.cash.subscription.domain.model.commands.PayPremiumCommand.ThreeDSecureParameters;
import pe.smartcash.cash.subscription.domain.model.valueobjects.PlanCode;
import pe.smartcash.cash.subscription.domain.model.valueobjects.UserId;
import pe.smartcash.cash.subscription.domain.services.PaymentRequest;
import pe.smartcash.cash.subscription.domain.services.PaymentResult;
import pe.smartcash.cash.subscription.domain.services.VerifiedPaymentEvent;

/**
 * El adaptador contra un Culqi simulado a nivel HTTP: verifica las rutas, cuerpos y cabeceras
 * exactos que se mandan (los del API v2 de Culqi) y cómo se interpreta cada respuesta posible,
 * incluidas las que la documentación muestra (200 + {@code action_code=REVIEW} para 3DS, el
 * objeto de error con {@code type}/{@code user_message}).
 */
class CulqiPaymentGatewayAdapterTest {

  private static final String BASE = "https://api.culqi.test/v2";
  private static final String SECRET = "sk_test_secreta";
  private static final String PLAN = "pln_test_premium0000000000";

  private MockRestServiceServer culqi;
  private CulqiPaymentGatewayAdapter adapter;
  private final UserId userId = UserId.of(UUID.randomUUID());

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder().baseUrl(BASE).defaultHeader("Authorization", "Bearer " + SECRET);
    culqi = MockRestServiceServer.bindTo(builder).build();
    adapter = new CulqiPaymentGatewayAdapter(
        builder.build(), JsonMapper.builder().build(), new CulqiProperties(BASE, SECRET, PLAN, Duration.ofSeconds(5)));
  }

  @Test
  void subscribingCreatesCustomerCardAndSubscriptionInThatOrder() {
    culqi.expect(requestTo(BASE + "/customers?email=ana%40example.com"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Authorization", "Bearer " + SECRET))
        .andRespond(json("""
            {"data":[],"total":0}"""));
    culqi.expect(requestTo(BASE + "/customers"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(jsonPath("$.first_name").value("Ana"))
        .andExpect(jsonPath("$.last_name").value("Pérez"))
        .andExpect(jsonPath("$.email").value("ana@example.com"))
        .andExpect(jsonPath("$.phone_number").value("999888777"))
        .andExpect(jsonPath("$.address").value("Av. Arequipa 123"))
        .andExpect(jsonPath("$.address_city").value("Lima"))
        .andExpect(jsonPath("$.country_code").value("PE"))
        .andExpect(jsonPath("$.metadata.user_id").value(userId.value().toString()))
        .andRespond(json(HttpStatus.CREATED, """
            {"object":"customer","id":"cus_test_1"}"""));
    culqi.expect(requestTo(BASE + "/cards"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(jsonPath("$.customer_id").value("cus_test_1"))
        .andExpect(jsonPath("$.token_id").value("tkn_test_1"))
        .andExpect(jsonPath("$.authentication_3DS").doesNotExist())
        .andRespond(json(HttpStatus.CREATED, """
            {"object":"card","id":"crd_test_1","customer_id":"cus_test_1"}"""));
    culqi.expect(requestTo(BASE + "/recurrent/subscriptions/create"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(jsonPath("$.card_id").value("crd_test_1"))
        .andExpect(jsonPath("$.plan_id").value(PLAN))
        .andExpect(jsonPath("$.tyc").value(true))
        .andExpect(jsonPath("$.metadata.user_id").value(userId.value().toString()))
        .andRespond(json(HttpStatus.CREATED, """
            {"id":"sxn_test_1","customer_id":"cus_test_1","plan_id":"%s","status":1}""".formatted(PLAN)));

    PaymentResult result = adapter.subscribe(request(null));

    assertThat(result).isEqualTo(new PaymentResult.Subscribed("sxn_test_1"));
    culqi.verify();
  }

  @Test
  void anExistingCustomerWithTheSameEmailIsReusedInsteadOfCreatingAnother() {
    culqi.expect(requestTo(BASE + "/customers?email=ana%40example.com"))
        .andRespond(json("""
            {"data":[{"object":"customer","id":"cus_test_existente","email":"ana@example.com"}]}"""));
    culqi.expect(requestTo(BASE + "/cards"))
        .andExpect(jsonPath("$.customer_id").value("cus_test_existente"))
        .andRespond(json(HttpStatus.CREATED, """
            {"object":"card","id":"crd_test_1"}"""));
    culqi.expect(requestTo(BASE + "/recurrent/subscriptions/create")).andRespond(json(HttpStatus.CREATED, """
        {"id":"sxn_test_1","status":1}"""));

    assertThat(adapter.subscribe(request(null))).isEqualTo(new PaymentResult.Subscribed("sxn_test_1"));
    culqi.verify();
  }

  @Test
  void whenCulqiAsksToReviewTheCardItRequires3dsAndCreatesNoSubscription() {
    givenExistingCustomer();
    culqi.expect(requestTo(BASE + "/cards")).andRespond(json("""
        {"user_message":"El usuario necesita autenticarse","action_code":"REVIEW"}"""));

    assertThat(adapter.subscribe(request(null))).isInstanceOf(PaymentResult.AuthenticationRequired.class);
    culqi.verify();
  }

  @Test
  void theRetryAfter3dsSendsTheAuthenticationParametersWithTheCard() {
    givenExistingCustomer();
    culqi.expect(requestTo(BASE + "/cards"))
        .andExpect(jsonPath("$.token_id").value("tkn_test_1"))
        .andExpect(jsonPath("$.authentication_3DS.eci").value("05"))
        .andExpect(jsonPath("$.authentication_3DS.xid").value("xid-1"))
        .andExpect(jsonPath("$.authentication_3DS.cavv").value("cavv-1"))
        .andExpect(jsonPath("$.authentication_3DS.protocolVersion").value("2.1.0"))
        .andExpect(jsonPath("$.authentication_3DS.directoryServerTransactionId").value("ds-1"))
        .andRespond(json(HttpStatus.CREATED, """
            {"object":"card","id":"crd_test_1"}"""));
    culqi.expect(requestTo(BASE + "/recurrent/subscriptions/create")).andRespond(json(HttpStatus.CREATED, """
        {"id":"sxn_test_1","status":1}"""));

    PaymentResult result = adapter.subscribe(request(new ThreeDSecureParameters("05", "xid-1", "cavv-1", "2.1.0", "ds-1")));

    assertThat(result).isEqualTo(new PaymentResult.Subscribed("sxn_test_1"));
    culqi.verify();
  }

  @Test
  void aCardErrorIsADeclineWithTheMessageCulqiWroteForTheCardholder() {
    givenExistingCustomer();
    culqi.expect(requestTo(BASE + "/cards")).andRespond(json(HttpStatus.PAYMENT_REQUIRED, """
        {"object":"error","type":"card_error","code":"card_declined","decline_code":"insufficient_funds",
         "merchant_message":"La tarjeta no tiene fondos suficientes.","user_message":"Su tarjeta no tiene fondos suficientes."}"""));

    assertThat(adapter.subscribe(request(null))).isEqualTo(new PaymentResult.Declined("Su tarjeta no tiene fondos suficientes."));
  }

  @Test
  void invalidPayerDataIsADeclineSoTheUserCanFixIt() {
    culqi.expect(requestTo(BASE + "/customers?email=ana%40example.com")).andRespond(json("""
        {"data":[]}"""));
    culqi.expect(requestTo(BASE + "/customers")).andRespond(json(HttpStatus.BAD_REQUEST, """
        {"object":"error","type":"parameter_error","merchant_message":"El campo address_city es inválido",
         "user_message":"Revisa la ciudad ingresada."}"""));

    assertThat(adapter.subscribe(request(null))).isEqualTo(new PaymentResult.Declined("Revisa la ciudad ingresada."));
  }

  @Test
  void anInvalidSecretKeyIsAGatewayFailureNotADecline() {
    culqi.expect(requestTo(BASE + "/customers?email=ana%40example.com")).andRespond(json("""
        {"data":[]}"""));
    culqi.expect(requestTo(BASE + "/customers")).andRespond(json(HttpStatus.UNAUTHORIZED, """
        {"object":"error","type":"authentication_error","merchant_message":"Llave inválida"}"""));

    assertThatThrownBy(() -> adapter.subscribe(request(null))).isInstanceOf(PaymentGatewayException.class).hasMessageContaining("Llave inválida");
  }

  @Test
  void aCulqiServerErrorWhileCreatingTheSubscriptionIsAGatewayFailure() {
    givenExistingCustomer();
    culqi.expect(requestTo(BASE + "/cards")).andRespond(json(HttpStatus.CREATED, """
        {"object":"card","id":"crd_test_1"}"""));
    culqi.expect(requestTo(BASE + "/recurrent/subscriptions/create")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

    assertThatThrownBy(() -> adapter.subscribe(request(null))).isInstanceOf(PaymentGatewayException.class);
  }

  @Test
  void onlyThePremiumPlanIsChargedThroughCulqi() {
    PaymentRequest free = new PaymentRequest(userId, PlanCode.FREE, "tkn_test_1", payer(), null);

    assertThatThrownBy(() -> adapter.subscribe(free)).isInstanceOf(IllegalArgumentException.class);
    culqi.verify();
  }

  // ---- cancel ----

  @Test
  void cancelingDeletesTheSubscriptionInCulqi() {
    culqi.expect(requestTo(BASE + "/recurrent/subscriptions/sxn_test_1"))
        .andExpect(method(HttpMethod.DELETE))
        .andRespond(json("""
            {"id":"sxn_test_1","delete":true}"""));

    adapter.cancel("sxn_test_1");

    culqi.verify();
  }

  @Test
  void cancelingASubscriptionCulqiAlreadyEndedIsNotAnError() {
    culqi.expect(requestTo(BASE + "/recurrent/subscriptions/sxn_test_1"))
        .andExpect(method(HttpMethod.DELETE))
        .andRespond(json(HttpStatus.BAD_REQUEST, """
            {"object":"error","type":"invalid_request_error","merchant_message":"La suscripción ya fue cancelada"}"""));
    culqi.expect(requestTo(BASE + "/recurrent/subscriptions/sxn_test_1"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(json("""
            {"id":"sxn_test_1","status":4}"""));

    adapter.cancel("sxn_test_1");

    culqi.verify();
  }

  @Test
  void ifCulqiDoesNotCancelAnActiveSubscriptionItFails() {
    culqi.expect(requestTo(BASE + "/recurrent/subscriptions/sxn_test_1"))
        .andExpect(method(HttpMethod.DELETE))
        .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
    culqi.expect(requestTo(BASE + "/recurrent/subscriptions/sxn_test_1"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(json("""
            {"id":"sxn_test_1","status":3}"""));

    assertThatThrownBy(() -> adapter.cancel("sxn_test_1")).isInstanceOf(PaymentGatewayException.class);
  }

  // ---- verifyEvent ----

  @Test
  void aChargeSucceededEventIsReadBackFromCulqi() {
    culqi.expect(requestTo(BASE + "/events/evt_test_1"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Authorization", "Bearer " + SECRET))
        .andRespond(json("""
            {"object":"event","id":"evt_test_1","type":"subscription.charge.succeeded",
             "data":{"id":"sxn_test_1","status":3,"active_card":"crd_test_1"}}"""));

    assertThat(adapter.verifyEvent("evt_test_1"))
        .contains(new VerifiedPaymentEvent("evt_test_1", VerifiedPaymentEvent.Type.CHARGE_SUCCEEDED, "sxn_test_1"));
  }

  @Test
  void eventDataSentAsAJsonStringIsParsedToo() {
    culqi.expect(requestTo(BASE + "/events/evt_test_1")).andRespond(json("""
        {"object":"event","id":"evt_test_1","type":"subscription.cancel.succeeded",
         "data":"{\\"object\\":\\"subscription\\",\\"id\\":\\"sxn_test_1\\",\\"status\\":4}"}"""));

    assertThat(adapter.verifyEvent("evt_test_1"))
        .contains(new VerifiedPaymentEvent("evt_test_1", VerifiedPaymentEvent.Type.SUBSCRIPTION_CANCELED, "sxn_test_1"));
  }

  @Test
  void aChargeEventCarryingTheSubscriptionInsideItsMetadataIsRecognized() {
    culqi.expect(requestTo(BASE + "/events/evt_test_1")).andRespond(json("""
        {"object":"event","id":"evt_test_1","type":"subscription.charge.failed",
         "data":{"object":"charge","id":"chr_test_1","metadata":{"subscription":"sxn_test_1"}}}"""));

    assertThat(adapter.verifyEvent("evt_test_1"))
        .contains(new VerifiedPaymentEvent("evt_test_1", VerifiedPaymentEvent.Type.CHARGE_FAILED, "sxn_test_1"));
  }

  @Test
  void anEventThatDoesNotExistInCulqiIsIgnored() {
    culqi.expect(requestTo(BASE + "/events/evt_inventado")).andRespond(json(HttpStatus.NOT_FOUND, """
        {"object":"error","type":"resource_error","merchant_message":"No existe"}"""));

    assertThat(adapter.verifyEvent("evt_inventado")).isEmpty();
  }

  @Test
  void eventTypesThatDoNotAffectSubscriptionsAreIgnored() {
    culqi.expect(requestTo(BASE + "/events/evt_test_1")).andRespond(json("""
        {"object":"event","id":"evt_test_1","type":"customer.creation.succeeded","data":{"id":"cus_test_1"}}"""));

    assertThat(adapter.verifyEvent("evt_test_1")).isEmpty();
  }

  @Test
  void ifCulqiFailsWhileVerifyingTheWebhookFailsSoCulqiRetries() {
    culqi.expect(requestTo(BASE + "/events/evt_test_1")).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

    assertThatThrownBy(() -> adapter.verifyEvent("evt_test_1")).isInstanceOf(PaymentGatewayException.class);
  }

  @Test
  void anEventIdIsSentEncodedSoItCannotReachOtherEndpoints() {
    culqi.expect(requestTo(Matchers.startsWith(BASE + "/events/..%2Fcharges"))).andRespond(json(HttpStatus.NOT_FOUND, "{}"));

    assertThat(adapter.verifyEvent("../charges")).isEmpty();
    culqi.verify();
  }

  private void givenExistingCustomer() {
    culqi.expect(requestTo(BASE + "/customers?email=ana%40example.com")).andRespond(json("""
        {"data":[{"object":"customer","id":"cus_test_1"}]}"""));
  }

  private PaymentRequest request(ThreeDSecureParameters threeDs) {
    return new PaymentRequest(userId, PlanCode.PREMIUM, "tkn_test_1", payer(), threeDs);
  }

  private static PaymentRequest.Payer payer() {
    return new PaymentRequest.Payer("Ana", "Pérez", "ana@example.com", "999888777", "Av. Arequipa 123", "Lima", "PE");
  }

  private static org.springframework.test.web.client.ResponseCreator json(String body) {
    return withSuccess(body, MediaType.APPLICATION_JSON);
  }

  private static org.springframework.test.web.client.ResponseCreator json(HttpStatus status, String body) {
    return withStatus(status).contentType(MediaType.APPLICATION_JSON).body(body);
  }
}
