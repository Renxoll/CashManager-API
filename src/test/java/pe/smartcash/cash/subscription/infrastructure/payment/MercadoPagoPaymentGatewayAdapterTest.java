package pe.smartcash.cash.subscription.infrastructure.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;
import pe.smartcash.cash.subscription.domain.exception.PaymentGatewayException;
import pe.smartcash.cash.subscription.domain.model.valueobjects.PlanCode;
import pe.smartcash.cash.subscription.domain.model.valueobjects.UserId;
import pe.smartcash.cash.subscription.domain.services.PaymentRequest;
import pe.smartcash.cash.subscription.domain.services.PaymentResult;
import pe.smartcash.cash.subscription.domain.services.VerifiedPaymentEvent;
import tools.jackson.databind.json.JsonMapper;

/** El adaptador contra un Mercado Pago simulado a nivel HTTP (rutas, cuerpos y estados que se interpretan). */
class MercadoPagoPaymentGatewayAdapterTest {

  private static final String BASE = "https://api.mercadopago.test";
  private static final String TOKEN = "TEST-token-secreto";
  private static final String PLAN = "plan_test_premium";

  private MockRestServiceServer mp;
  private MercadoPagoPaymentGatewayAdapter adapter;
  private final UserId userId = UserId.of(UUID.randomUUID());

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder().baseUrl(BASE).defaultHeader("Authorization", "Bearer " + TOKEN);
    mp = MockRestServiceServer.bindTo(builder).build();
    adapter = new MercadoPagoPaymentGatewayAdapter(
        builder.build(),
        JsonMapper.builder().build(),
        new MercadoPagoProperties(BASE, TOKEN, PLAN, new BigDecimal("19.90"), "PEN", "https://tuluki.com/dashboard", "secreto", Duration.ofSeconds(5)));
  }

  @Test
  void subscribingCreatesAnAuthorizedPreapprovalWithTheCardToken() {
    mp.expect(requestTo(BASE + "/preapproval"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer " + TOKEN))
        .andExpect(header("X-Idempotency-Key", Matchers.not(Matchers.emptyString())))
        .andExpect(jsonPath("$.preapproval_plan_id").value(PLAN))
        .andExpect(jsonPath("$.card_token_id").value("tok_1"))
        .andExpect(jsonPath("$.payer_email").value("ana@example.com"))
        .andExpect(jsonPath("$.external_reference").value(userId.value().toString()))
        .andExpect(jsonPath("$.status").value("authorized"))
        .andExpect(jsonPath("$.back_url").value("https://tuluki.com/dashboard"))
        .andExpect(jsonPath("$.auto_recurring.transaction_amount").value(19.9))
        .andExpect(jsonPath("$.auto_recurring.currency_id").value("PEN"))
        .andExpect(jsonPath("$.auto_recurring.frequency_type").value("months"))
        .andRespond(json(HttpStatus.CREATED, """
            {"id":"2c93808","status":"authorized"}"""));

    assertThat(adapter.subscribe(request())).isEqualTo(new PaymentResult.Subscribed("2c93808"));
    mp.verify();
  }

  @Test
  void aPendingPreapprovalIsNotTreatedAsPaid() {
    mp.expect(requestTo(BASE + "/preapproval")).andRespond(json(HttpStatus.CREATED, """
        {"id":"2c93808","status":"pending"}"""));

    assertThat(adapter.subscribe(request())).isInstanceOf(PaymentResult.Declined.class);
  }

  @Test
  void aBadRequestIsADeclinedCard() {
    mp.expect(requestTo(BASE + "/preapproval")).andRespond(json(HttpStatus.BAD_REQUEST, """
        {"message":"Card token service not found","status":400}"""));

    assertThat(adapter.subscribe(request())).isInstanceOf(PaymentResult.Declined.class);
  }

  @Test
  void invalidCredentialsAreAGatewayFailureNotACardDecline() {
    mp.expect(requestTo(BASE + "/preapproval")).andRespond(json(HttpStatus.UNAUTHORIZED, """
        {"message":"invalid access token"}"""));

    assertThatThrownBy(() -> adapter.subscribe(request())).isInstanceOf(PaymentGatewayException.class);
  }

  @Test
  void aNonPremiumPlanIsRejected() {
    PaymentRequest free = new PaymentRequest(userId, PlanCode.FREE, "tok_1", payer(), null);
    assertThatThrownBy(() -> adapter.subscribe(free)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void cancelSendsTheCancelledStatus() {
    mp.expect(requestTo(BASE + "/preapproval/2c93808"))
        .andExpect(method(HttpMethod.PUT))
        .andExpect(jsonPath("$.status").value("cancelled"))
        .andRespond(json("""
            {"id":"2c93808","status":"cancelled"}"""));

    adapter.cancel("2c93808");
    mp.verify();
  }

  @Test
  void cancelIsIdempotentWhenItWasAlreadyCancelled() {
    mp.expect(requestTo(BASE + "/preapproval/2c93808")).andExpect(method(HttpMethod.PUT)).andRespond(json(HttpStatus.BAD_REQUEST, "{}"));
    mp.expect(requestTo(BASE + "/preapproval/2c93808")).andExpect(method(HttpMethod.GET)).andRespond(json("""
        {"id":"2c93808","status":"cancelled"}"""));

    adapter.cancel("2c93808");
  }

  @Test
  void cancelFailsWhenMercadoPagoRefusesAndItIsStillActive() {
    mp.expect(requestTo(BASE + "/preapproval/2c93808")).andExpect(method(HttpMethod.PUT)).andRespond(json(HttpStatus.INTERNAL_SERVER_ERROR, "{}"));
    mp.expect(requestTo(BASE + "/preapproval/2c93808")).andExpect(method(HttpMethod.GET)).andRespond(json("""
        {"id":"2c93808","status":"authorized"}"""));

    assertThatThrownBy(() -> adapter.cancel("2c93808")).isInstanceOf(PaymentGatewayException.class);
  }

  @Test
  void aCancelledPreapprovalEventMapsToSubscriptionCanceled() {
    mp.expect(requestTo(BASE + "/preapproval/2c93808")).andRespond(json("""
        {"id":"2c93808","status":"cancelled"}"""));

    assertThat(adapter.verifyEvent("subscription_preapproval:2c93808"))
        .contains(new VerifiedPaymentEvent("subscription_preapproval:2c93808", VerifiedPaymentEvent.Type.SUBSCRIPTION_CANCELED, "2c93808"));
  }

  @Test
  void anAuthorizedPreapprovalEventHasNoEffect() {
    mp.expect(requestTo(BASE + "/preapproval/2c93808")).andRespond(json("""
        {"id":"2c93808","status":"authorized"}"""));

    assertThat(adapter.verifyEvent("subscription_preapproval:2c93808")).isEmpty();
  }

  @Test
  void aProcessedApprovedChargeMapsToChargeSucceeded() {
    mp.expect(requestTo(BASE + "/authorized_payments/99")).andRespond(json("""
        {"id":99,"preapproval_id":"2c93808","status":"processed","payment":{"status":"approved"}}"""));

    assertThat(adapter.verifyEvent("subscription_authorized_payment:99"))
        .contains(new VerifiedPaymentEvent("subscription_authorized_payment:99", VerifiedPaymentEvent.Type.CHARGE_SUCCEEDED, "2c93808"));
  }

  @Test
  void aRecyclingChargeMapsToChargeFailed() {
    mp.expect(requestTo(BASE + "/authorized_payments/99")).andRespond(json("""
        {"id":99,"preapproval_id":"2c93808","status":"recycling","payment":{"status":"rejected"}}"""));

    assertThat(adapter.verifyEvent("subscription_authorized_payment:99"))
        .hasValueSatisfying(event -> assertThat(event.type()).isEqualTo(VerifiedPaymentEvent.Type.CHARGE_FAILED));
  }

  @Test
  void aMissingResourceIsIgnored() {
    mp.expect(requestTo(BASE + "/authorized_payments/404")).andRespond(json(HttpStatus.NOT_FOUND, "{}"));
    assertThat(adapter.verifyEvent("subscription_authorized_payment:404")).isEmpty();
  }

  @Test
  void aServerErrorWhileVerifyingIsAGatewayFailure() {
    mp.expect(requestTo(BASE + "/authorized_payments/500")).andRespond(json(HttpStatus.INTERNAL_SERVER_ERROR, "{}"));
    assertThatThrownBy(() -> adapter.verifyEvent("subscription_authorized_payment:500")).isInstanceOf(PaymentGatewayException.class);
  }

  @Test
  void malformedOrUnknownEventIdsAreIgnoredWithoutCallingMercadoPago() {
    assertThat(adapter.verifyEvent("sin-separador")).isEmpty();
    assertThat(adapter.verifyEvent("payment:123")).isEmpty();
    assertThat(adapter.verifyEvent("subscription_preapproval:")).isEmpty();
    mp.verify();
  }

  private PaymentRequest request() {
    return new PaymentRequest(userId, PlanCode.PREMIUM, "tok_1", payer(), null);
  }

  private static PaymentRequest.Payer payer() {
    return new PaymentRequest.Payer("Ana", "Pérez", "ana@example.com", "999888777", "Av. Arequipa 123", "Lima", "PE");
  }

  private static ResponseCreator json(String body) {
    return withSuccess(body, MediaType.APPLICATION_JSON);
  }

  private static ResponseCreator json(HttpStatus status, String body) {
    return withStatus(status).contentType(MediaType.APPLICATION_JSON).body(body);
  }
}
