package pe.smartcash.cash.subscription.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import pe.smartcash.cash.RedisTestConfiguration;
import pe.smartcash.cash.iam.domain.model.valueobjects.UserId;
import pe.smartcash.cash.iam.domain.services.TokenService;
import pe.smartcash.cash.subscription.domain.exception.PaymentGatewayException;
import pe.smartcash.cash.subscription.domain.services.PaymentRequest;
import pe.smartcash.cash.subscription.domain.services.PaymentResult;
import pe.smartcash.cash.subscription.domain.services.SubscriptionPaymentGateway;

/**
 * E2E del pago visto desde el frontend: elegir plan, pagar con el token de Culqi (con o sin
 * 3DS), cancelar. Postgres real porque el upgrade FREE -> PREMIUM depende del índice único
 * parcial "una sola suscripción ACTIVE por usuario"; Redis real porque el contexto lo exige al
 * arrancar. Culqi se reemplaza en el puerto ({@link SubscriptionPaymentGateway}); el adaptador
 * HTTP se prueba aparte (CulqiPaymentGatewayAdapterTest y CulqiPaymentFlowIT).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(RedisTestConfiguration.class)
class SubscriptionControllerIT {

  @Container
  @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

  private static final String USER_TOKEN = "token-user";

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;

  @MockitoBean private TokenService tokenService;
  @MockitoBean private SubscriptionPaymentGateway paymentGateway;

  private UUID userId;

  @BeforeEach
  void setUp() {
    jdbcTemplate.update("DELETE FROM subscriptions");
    userId = UUID.randomUUID();
    when(tokenService.validate(USER_TOKEN)).thenReturn(Optional.of(UserId.of(userId)));
  }

  @Test
  void requestsWithoutBearerTokenAreRejectedWith401() throws Exception {
    mockMvc.perform(get("/api/v1/subscriptions/active")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(post("/api/v1/subscriptions/premium").contentType(MediaType.APPLICATION_JSON).content(premiumBody("tkn_test_1", "")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void withoutAnySubscriptionTheActiveEndpointReturns404() throws Exception {
    mockMvc.perform(get("/api/v1/subscriptions/active").header("Authorization", "Bearer " + USER_TOKEN)).andExpect(status().isNotFound());
  }

  @Test
  void choosingFreeActivatesImmediatelyWithoutTheProvider() throws Exception {
    checkoutFree().andExpect(status().isCreated()).andExpect(jsonPath("$.planCode").value("FREE"));

    verify(paymentGateway, never()).subscribe(any());
  }

  @Test
  void thePaidPlanCannotBeActivatedThroughTheFreeEndpoint() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/subscriptions/checkout")
                .header("Authorization", "Bearer " + USER_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"planCode\":\"PREMIUM\"}"))
        .andExpect(status().isBadRequest());

    assertThat(countRows("1 = 1")).isZero();
  }

  @Test
  void payingPremiumWhileOnFreeActivatesItAndClosesFree() throws Exception {
    checkoutFree().andExpect(status().isCreated());
    when(paymentGateway.subscribe(any())).thenReturn(new PaymentResult.Subscribed("sxn_test_1"));

    payPremium(premiumBody("tkn_test_1", ""))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.planCode").value("PREMIUM"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.renewsAt").isNotEmpty());

    assertThat(countRows("plan_code = 'FREE' AND status = 'CANCELED'")).isEqualTo(1);
    assertThat(countRows("plan_code = 'PREMIUM' AND status = 'ACTIVE' AND provider_subscription_id = 'sxn_test_1'")).isEqualTo(1);
  }

  @Test
  void thePayerDataAndTokenReachTheProviderAndCountryDefaultsToPeru() throws Exception {
    when(paymentGateway.subscribe(any())).thenReturn(new PaymentResult.Subscribed("sxn_test_1"));

    payPremium(premiumBody("tkn_test_1", "")).andExpect(status().isCreated());

    ArgumentCaptor<PaymentRequest> request = ArgumentCaptor.forClass(PaymentRequest.class);
    verify(paymentGateway).subscribe(request.capture());
    assertThat(request.getValue().cardTokenId()).isEqualTo("tkn_test_1");
    assertThat(request.getValue().payer().firstName()).isEqualTo("Ana María");
    assertThat(request.getValue().payer().phoneNumber()).isEqualTo("999888777");
    assertThat(request.getValue().payer().countryCode()).isEqualTo("PE");
    assertThat(request.getValue().userId().value()).isEqualTo(userId);
  }

  @Test
  void whenTheBankAsksFor3dsTheResponseIs202AndNothingIsActivated() throws Exception {
    when(paymentGateway.subscribe(any())).thenReturn(new PaymentResult.AuthenticationRequired());

    payPremium(premiumBody("tkn_test_1", "")).andExpect(status().isAccepted()).andExpect(jsonPath("$.status").value("AUTHENTICATION_REQUIRED"));

    assertThat(countRows("1 = 1")).isZero();
  }

  @Test
  void theRetryWith3dsParametersActivates() throws Exception {
    when(paymentGateway.subscribe(any())).thenReturn(new PaymentResult.Subscribed("sxn_test_1"));
    String threeDs =
        """
        ,"authentication3DS":{"eci":"05","xid":"xid-1","cavv":"cavv-1","protocolVersion":"2.1.0","directoryServerTransactionId":"ds-1"}""";

    payPremium(premiumBody("tkn_test_1", threeDs)).andExpect(status().isCreated());

    ArgumentCaptor<PaymentRequest> request = ArgumentCaptor.forClass(PaymentRequest.class);
    verify(paymentGateway).subscribe(request.capture());
    assertThat(request.getValue().authentication3ds().cavv()).isEqualTo("cavv-1");
  }

  @Test
  void aDeclinedCardReturns402WithTheProviderMessage() throws Exception {
    when(paymentGateway.subscribe(any())).thenReturn(new PaymentResult.Declined("Su tarjeta no tiene fondos suficientes."));

    payPremium(premiumBody("tkn_test_1", ""))
        .andExpect(status().isPaymentRequired())
        .andExpect(jsonPath("$.message").value("Su tarjeta no tiene fondos suficientes."));

    assertThat(countRows("1 = 1")).isZero();
  }

  @Test
  void aProviderFailureReturns502() throws Exception {
    when(paymentGateway.subscribe(any())).thenThrow(new PaymentGatewayException("Culqi caído", new RuntimeException()));

    payPremium(premiumBody("tkn_test_1", "")).andExpect(status().isBadGateway());
  }

  @Test
  void payingWhileAlreadyPremiumReturns409() throws Exception {
    when(paymentGateway.subscribe(any())).thenReturn(new PaymentResult.Subscribed("sxn_test_1"));
    payPremium(premiumBody("tkn_test_1", "")).andExpect(status().isCreated());

    payPremium(premiumBody("tkn_test_2", "")).andExpect(status().isConflict());
  }

  @Test
  void payingWithoutAcceptingTheTermsReturns400() throws Exception {
    payPremium(premiumBody("tkn_test_1", "").replace("\"acceptedTerms\":true", "\"acceptedTerms\":false")).andExpect(status().isBadRequest());

    verify(paymentGateway, never()).subscribe(any());
  }

  @Test
  void invalidPayerDataReturns400BeforeCallingTheProvider() throws Exception {
    payPremium(premiumBody("tkn_test_1", "").replace("\"999888777\"", "\"no-es-telefono\"")).andExpect(status().isBadRequest());
    payPremium(premiumBody("tkn_test_1", "").replace("\"Ana María\"", "\"Ana_123\"")).andExpect(status().isBadRequest());
    payPremium(premiumBody("", "")).andExpect(status().isBadRequest());

    verify(paymentGateway, never()).subscribe(any());
  }

  @Test
  void cancelingPremiumCancelsItInTheProviderAndLeavesNoActivePlan() throws Exception {
    when(paymentGateway.subscribe(any())).thenReturn(new PaymentResult.Subscribed("sxn_test_1"));
    payPremium(premiumBody("tkn_test_1", "")).andExpect(status().isCreated());

    mockMvc.perform(delete("/api/v1/subscriptions/active").header("Authorization", "Bearer " + USER_TOKEN)).andExpect(status().isNoContent());

    verify(paymentGateway).cancel("sxn_test_1");
    mockMvc.perform(get("/api/v1/subscriptions/active").header("Authorization", "Bearer " + USER_TOKEN)).andExpect(status().isNotFound());
  }

  @Test
  void ifTheProviderRefusesTheCancellationThePlanStaysActive() throws Exception {
    when(paymentGateway.subscribe(any())).thenReturn(new PaymentResult.Subscribed("sxn_test_1"));
    payPremium(premiumBody("tkn_test_1", "")).andExpect(status().isCreated());
    doThrow(new PaymentGatewayException("Culqi caído", new RuntimeException())).when(paymentGateway).cancel("sxn_test_1");

    mockMvc.perform(delete("/api/v1/subscriptions/active").header("Authorization", "Bearer " + USER_TOKEN)).andExpect(status().isBadGateway());

    mockMvc
        .perform(get("/api/v1/subscriptions/active").header("Authorization", "Bearer " + USER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  private ResultActions checkoutFree() throws Exception {
    return mockMvc.perform(
        post("/api/v1/subscriptions/checkout")
            .header("Authorization", "Bearer " + USER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"planCode\":\"FREE\"}"));
  }

  private ResultActions payPremium(String body) throws Exception {
    return mockMvc.perform(
        post("/api/v1/subscriptions/premium")
            .header("Authorization", "Bearer " + USER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  static String premiumBody(String cardToken, String extra) {
    return """
        {"planCode":"PREMIUM","cardToken":"%s","acceptedTerms":true,
         "payer":{"firstName":"Ana María","lastName":"Pérez","email":"ana@example.com","phoneNumber":"999888777",
                  "address":"Av. Arequipa 123","addressCity":"Lima"}%s}"""
        .formatted(cardToken, extra);
  }

  private int countRows(String where) {
    return jdbcTemplate.queryForObject("SELECT count(*) FROM subscriptions WHERE " + where, Integer.class);
  }
}
