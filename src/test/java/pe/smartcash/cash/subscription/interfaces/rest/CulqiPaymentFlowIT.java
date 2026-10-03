package pe.smartcash.cash.subscription.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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

/**
 * Pago y webhooks de punta a punta con el adaptador REAL de Culqi: lo único falso es Culqi
 * mismo, reemplazado por un servidor HTTP local que responde como su API v2. Así se prueba el
 * cableado de verdad (bean {@code culqiRestClient}, llave en la cabecera, serialización JSON de
 * Spring Boot, base URL configurable) además de la lógica.
 *
 * <p>Los webhooks se mandan como los manda Culqi y se comprueba lo central del diseño: solo
 * se cree lo que devuelve {@code GET /v2/events/{id}} de Culqi, nunca el cuerpo recibido.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(RedisTestConfiguration.class)
class CulqiPaymentFlowIT {

  @Container
  @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

  private static final String SECRET_KEY = "sk_test_para_it";
  private static final String PLAN_ID = "pln_test_premium_para_it00";
  private static final String WEBHOOK_TOKEN = "token-webhook-para-it";
  private static final String USER_TOKEN = "token-user";

  private static final FakeCulqi culqi = FakeCulqi.start();

  @DynamicPropertySource
  static void culqiProperties(DynamicPropertyRegistry registry) {
    registry.add("app.culqi.api-base-url", () -> culqi.baseUrl() + "/v2");
    registry.add("app.culqi.secret-key", () -> SECRET_KEY);
    registry.add("app.culqi.premium-plan-id", () -> PLAN_ID);
    registry.add("app.culqi.webhook-token", () -> WEBHOOK_TOKEN);
  }

  @AfterAll
  static void stopCulqi() {
    culqi.stop();
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;

  @MockitoBean private TokenService tokenService;

  private UUID userId;

  @BeforeEach
  void setUp() {
    jdbcTemplate.update("DELETE FROM subscriptions");
    culqi.reset();
    userId = UUID.randomUUID();
    when(tokenService.validate(USER_TOKEN)).thenReturn(Optional.of(UserId.of(userId)));
  }

  // ---- Pago ----

  @Test
  void payingCreatesCustomerCardAndSubscriptionInCulqiAndActivatesPremium() throws Exception {
    culqi.respond("GET /v2/customers", 200, "{\"data\":[]}");
    culqi.respond("POST /v2/customers", 201, "{\"object\":\"customer\",\"id\":\"cus_test_e2e\"}");
    culqi.respond("POST /v2/cards", 201, "{\"object\":\"card\",\"id\":\"crd_test_e2e\"}");
    culqi.respond("POST /v2/recurrent/subscriptions/create", 201, "{\"id\":\"sxn_test_e2e\",\"status\":1}");

    payPremium(SubscriptionControllerIT.premiumBody("tkn_test_e2e", ""))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.planCode").value("PREMIUM"));

    assertThat(countRows("status = 'ACTIVE' AND provider_subscription_id = 'sxn_test_e2e'")).isEqualTo(1);
    assertThat(culqi.requests()).extracting(FakeCulqi.Request::route)
        .containsExactly("GET /v2/customers", "POST /v2/customers", "POST /v2/cards", "POST /v2/recurrent/subscriptions/create");
    assertThat(culqi.requests()).allSatisfy(r -> assertThat(r.authorization()).isEqualTo("Bearer " + SECRET_KEY));
    assertThat(culqi.lastBodyOf("POST /v2/cards")).contains("\"token_id\":\"tkn_test_e2e\"").contains("\"customer_id\":\"cus_test_e2e\"");
    assertThat(culqi.lastBodyOf("POST /v2/recurrent/subscriptions/create"))
        .contains("\"plan_id\":\"" + PLAN_ID + "\"")
        .contains("\"tyc\":true")
        .contains(userId.toString());
  }

  @Test
  void theFirstAttemptAsksFor3dsAndTheRetryWithTheSameTokenActivates() throws Exception {
    culqi.respond("GET /v2/customers", 200, "{\"data\":[{\"id\":\"cus_test_e2e\"}]}");
    culqi.respond("POST /v2/cards", 200, "{\"user_message\":\"El usuario necesita autenticarse\",\"action_code\":\"REVIEW\"}");
    culqi.respond("POST /v2/cards", 201, "{\"object\":\"card\",\"id\":\"crd_test_e2e\"}");
    culqi.respond("POST /v2/recurrent/subscriptions/create", 201, "{\"id\":\"sxn_test_e2e\",\"status\":1}");

    payPremium(SubscriptionControllerIT.premiumBody("tkn_test_e2e", ""))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.status").value("AUTHENTICATION_REQUIRED"));
    assertThat(countRows("1 = 1")).isZero();

    String threeDs =
        ",\"authentication3DS\":{\"eci\":\"05\",\"xid\":\"xid-1\",\"cavv\":\"cavv-1\",\"protocolVersion\":\"2.1.0\",\"directoryServerTransactionId\":\"ds-1\"}";
    payPremium(SubscriptionControllerIT.premiumBody("tkn_test_e2e", threeDs)).andExpect(status().isCreated());

    assertThat(culqi.lastBodyOf("POST /v2/cards")).contains("\"authentication_3DS\"").contains("\"cavv\":\"cavv-1\"");
    assertThat(countRows("status = 'ACTIVE' AND provider_subscription_id = 'sxn_test_e2e'")).isEqualTo(1);
  }

  @Test
  void aCardDeclinedByCulqiReturns402WithItsMessage() throws Exception {
    culqi.respond("GET /v2/customers", 200, "{\"data\":[{\"id\":\"cus_test_e2e\"}]}");
    culqi.respond(
        "POST /v2/cards",
        402,
        "{\"object\":\"error\",\"type\":\"card_error\",\"user_message\":\"Su tarjeta fue rechazada por el banco.\"}");

    payPremium(SubscriptionControllerIT.premiumBody("tkn_test_e2e", ""))
        .andExpect(status().isPaymentRequired())
        .andExpect(jsonPath("$.message").value("Su tarjeta fue rechazada por el banco."));

    assertThat(countRows("1 = 1")).isZero();
  }

  // ---- Webhooks ----

  @Test
  void aVerifiedChargeSucceededWebhookRenewsThePlan() throws Exception {
    insertPremium(Instant.now().minus(Duration.ofDays(1)));
    culqi.respond("GET /v2/events/evt_test_1", 200, event("evt_test_1", "subscription.charge.succeeded"));

    webhook(WEBHOOK_TOKEN, "{\"object\":\"event\",\"id\":\"evt_test_1\",\"type\":\"subscription.charge.succeeded\"}")
        .andExpect(status().isOk());

    Timestamp renewsAt = jdbcTemplate.queryForObject("SELECT renews_at FROM subscriptions WHERE user_id = ?", Timestamp.class, userId);
    assertThat(renewsAt.toInstant()).isAfter(Instant.now().plus(Duration.ofDays(29)));
  }

  @Test
  void aVerifiedCancelWebhookExpiresThePlan() throws Exception {
    insertPremium(Instant.now().plus(Duration.ofDays(10)));
    culqi.respond("GET /v2/events/evt_test_1", 200, event("evt_test_1", "subscription.cancel.succeeded"));

    webhook(WEBHOOK_TOKEN, "{\"id\":\"evt_test_1\"}").andExpect(status().isOk());

    assertThat(countRows("user_id = '" + userId + "' AND status = 'EXPIRED'")).isEqualTo(1);
  }

  @Test
  void theWebhookBodyIsNeverTrustedOnlyWhatCulqiSaysAboutTheEvent() throws Exception {
    // Alguien con el token manda un "cancel" con el id de un evento que en Culqi es un cobro exitoso.
    insertPremium(Instant.now().plus(Duration.ofDays(10)));
    culqi.respond("GET /v2/events/evt_test_1", 200, event("evt_test_1", "subscription.charge.succeeded"));

    webhook(WEBHOOK_TOKEN, "{\"id\":\"evt_test_1\",\"type\":\"subscription.cancel.succeeded\",\"data\":\"{\\\"id\\\":\\\"sxn_test_e2e\\\"}\"}")
        .andExpect(status().isOk());

    assertThat(countRows("user_id = '" + userId + "' AND status = 'ACTIVE'")).isEqualTo(1);
  }

  @Test
  void anEventThatDoesNotExistInCulqiChangesNothing() throws Exception {
    insertPremium(Instant.now().plus(Duration.ofDays(10)));
    culqi.respond("GET /v2/events/evt_inventado", 404, "{\"object\":\"error\",\"type\":\"resource_error\"}");

    webhook(WEBHOOK_TOKEN, "{\"id\":\"evt_inventado\",\"type\":\"subscription.cancel.succeeded\"}").andExpect(status().isOk());

    assertThat(countRows("user_id = '" + userId + "' AND status = 'ACTIVE'")).isEqualTo(1);
  }

  @Test
  void aWebhookWithAWrongTokenIsRejectedWithoutAskingCulqi() throws Exception {
    webhook("token-adivinado", "{\"id\":\"evt_test_1\"}").andExpect(status().isForbidden());
    mockMvc
        .perform(post("/api/v1/subscriptions/culqi-webhook").contentType(MediaType.APPLICATION_JSON).content("{\"id\":\"evt_test_1\"}"))
        .andExpect(status().isForbidden());

    assertThat(culqi.requests()).isEmpty();
  }

  @Test
  void aWebhookWithoutEventIdIsRejected() throws Exception {
    webhook(WEBHOOK_TOKEN, "{\"type\":\"subscription.cancel.succeeded\"}").andExpect(status().isBadRequest());

    assertThat(culqi.requests()).isEmpty();
  }

  @Test
  void ifCulqiCannotBeReachedTheWebhookFailsSoCulqiRetries() throws Exception {
    culqi.respond("GET /v2/events/evt_test_1", 503, "");

    webhook(WEBHOOK_TOKEN, "{\"id\":\"evt_test_1\"}").andExpect(status().isBadGateway());
  }

  private static String event(String id, String type) {
    return """
        {"object":"event","id":"%s","type":"%s","creation_date":1700000000000,
         "data":{"object":"subscription","id":"sxn_test_e2e","status":3}}"""
        .formatted(id, type);
  }

  private ResultActions payPremium(String body) throws Exception {
    return mockMvc.perform(
        post("/api/v1/subscriptions/premium")
            .header("Authorization", "Bearer " + USER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private ResultActions webhook(String token, String body) throws Exception {
    return mockMvc.perform(
        post("/api/v1/subscriptions/culqi-webhook").param("token", token).contentType(MediaType.APPLICATION_JSON).content(body));
  }

  private void insertPremium(Instant renewsAt) {
    jdbcTemplate.update(
        "INSERT INTO subscriptions (id, user_id, plan_code, status, started_at, renews_at, provider_subscription_id)"
            + " VALUES (?, ?, 'PREMIUM', 'ACTIVE', now(), ?, 'sxn_test_e2e')",
        UUID.randomUUID(),
        userId,
        Timestamp.from(renewsAt));
  }

  private int countRows(String where) {
    return jdbcTemplate.queryForObject("SELECT count(*) FROM subscriptions WHERE " + where, Integer.class);
  }

  /**
   * Culqi falso: responde por "MÉTODO /ruta" (sin query string) con la cola de respuestas
   * programada por cada test, en orden, y registra lo que recibe. Una ruta sin respuesta
   * programada responde 404, igual que un recurso inexistente en Culqi.
   */
  static final class FakeCulqi {

    record Request(String route, String authorization, String body) {}

    private record Response(int status, String body) {}

    private final HttpServer server;
    private final Map<String, Deque<Response>> responses = new ConcurrentHashMap<>();
    private final List<Request> received = new CopyOnWriteArrayList<>();

    private FakeCulqi(HttpServer server) {
      this.server = server;
    }

    static FakeCulqi start() {
      try {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        FakeCulqi fake = new FakeCulqi(server);
        server.createContext("/", exchange -> {
          String route = exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath();
          String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          fake.received.add(new Request(route, exchange.getRequestHeaders().getFirst("Authorization"), body));
          Deque<Response> queue = fake.responses.get(route);
          Response response = queue == null || queue.isEmpty() ? new Response(404, "{\"object\":\"error\"}") : queue.size() > 1 ? queue.poll() : queue.peek();
          byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(response.status(), bytes.length == 0 ? -1 : bytes.length);
          if (bytes.length > 0) {
            exchange.getResponseBody().write(bytes);
          }
          exchange.close();
        });
        server.start();
        return fake;
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    String baseUrl() {
      return "http://localhost:" + server.getAddress().getPort();
    }

    /** Encola una respuesta; la última de cada ruta se repite para los llamados siguientes. */
    void respond(String route, int status, String body) {
      responses.computeIfAbsent(route, r -> new ArrayDeque<>()).add(new Response(status, body));
    }

    List<Request> requests() {
      return received;
    }

    String lastBodyOf(String route) {
      return received.stream().filter(r -> r.route().equals(route)).reduce((a, b) -> b).map(Request::body).orElseThrow();
    }

    void reset() {
      responses.clear();
      received.clear();
    }

    void stop() {
      server.stop(0);
    }
  }
}
