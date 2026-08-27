package pe.smartcash.cash.transactions.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import pe.smartcash.cash.iam.domain.model.valueobjects.UserId;
import pe.smartcash.cash.iam.domain.services.TokenService;
import pe.smartcash.cash.transactions.domain.exception.TransactionExtractionFailedException;
import pe.smartcash.cash.transactions.domain.model.valueobjects.CategoryCode;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Merchant;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Money;
import pe.smartcash.cash.transactions.domain.services.ExtractionResult;
import pe.smartcash.cash.transactions.domain.services.MerchantCategoryCache;
import pe.smartcash.cash.transactions.domain.services.TransactionExtractionService;

/**
 * E2E del webhook de ingesta contra un Postgres real (Testcontainers) con las migraciones
 * de Flyway aplicadas de punta a punta. {@code TokenService} y {@code TransactionExtractionService}
 * se mockean porque son los dos puertos que el enunciado marca como "llamadas externas" a
 * aislar (IAM y el LLM); {@code MerchantCategoryCache} también se mockea, pero por una razón
 * distinta y puramente técnica: es Redis, y el flujo feliz llama a {@code remember(...)}
 * incondicionalmente tras cualquier extracción exitosa por LLM — sin mockearlo, este test
 * necesitaría un SEGUNDO contenedor (Redis) solo para no romper con una excepción de conexión
 * en el happy path. Mockearlo evita esa dependencia extra sin perder cobertura del
 * controller: el cacheo en Redis ya tiene su propio adaptador y no es lo que este test
 * ejercita.
 *
 * <p>La categorización corre en el worker async (ver {@code TransactionCommandServiceImpl.on
 * (TransactionReceived)}), fuera del hilo del request: el POST siempre responde 202 con la
 * fila todavía PENDING, y las aserciones de PROCESSED/FAILED hacen poll de la fila hasta que
 * el listener la actualiza (o vencen el timeout).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TransactionWebhookControllerIT {

  @Container
  @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

  private static final String BEARER_TOKEN = "token-valido";

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;

  @MockitoBean private TokenService tokenService;
  @MockitoBean private TransactionExtractionService extractionService;
  @MockitoBean private MerchantCategoryCache merchantCategoryCache;

  private UUID validUserId;

  @BeforeEach
  void setUp() {
    // transactions primero: tiene FK hacia user_profiles, así que hay que vaciarla antes
    // de poder limpiar la tabla padre sin violar la constraint.
    jdbcTemplate.update("DELETE FROM transactions");
    jdbcTemplate.update("DELETE FROM user_profiles");

    validUserId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO user_profiles (id, display_name, inbox_address, created_at, updated_at) VALUES (?, ?, ?, now(), now())",
        validUserId,
        "Usuario de Test",
        "alias-" + validUserId + "@inbox.smartcash.pe");

    when(tokenService.validate(BEARER_TOKEN)).thenReturn(Optional.of(UserId.of(validUserId)));
    // Cache miss por defecto: fuerza a que la extracción siempre pase por el LLM mockeado,
    // sin importar si el texto matchea el parser heurístico de notificaciones bancarias.
    when(merchantCategoryCache.findCategoryFor(any())).thenReturn(Optional.empty());
  }

  @Test
  void shouldProcessTransactionSuccessfully() throws Exception {
    when(extractionService.extract(anyString()))
        .thenReturn(new ExtractionResult(new Money(new BigDecimal("24.50"), "PEN"), new Merchant("Starbucks"), CategoryCode.COMIDA));

    mockMvc
        .perform(
            post("/api/v1/transactions/webhook")
                .header("Authorization", "Bearer " + BEARER_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"userId":"%s","rawText":"S/24.50 en Starbucks"}
                    """
                        .formatted(validUserId)))
        .andExpect(status().isAccepted());

    Map<String, Object> saved = awaitTransactionStatus(validUserId, "PROCESSED");
    assertThat((BigDecimal) saved.get("amount")).isEqualByComparingTo("24.50");
    assertThat(saved.get("currency")).isEqualTo("PEN");
    assertThat(saved.get("merchant")).isEqualTo("Starbucks");
    assertThat(saved.get("raw_text")).isEqualTo("S/24.50 en Starbucks");
  }

  @Test
  void shouldReturn202AndEventuallySaveFailedStateWhenLLMFails() throws Exception {
    when(extractionService.extract(anyString()))
        .thenThrow(new TransactionExtractionFailedException("El LLM no devolvió un JSON válido tras reintento"));

    String rawText = "Notificación de consumo en un comercio no identificado";

    mockMvc
        .perform(
            post("/api/v1/transactions/webhook")
                .header("Authorization", "Bearer " + BEARER_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"userId":"%s","rawText":"%s"}
                    """
                        .formatted(validUserId, rawText)))
        .andExpect(status().isAccepted());

    Map<String, Object> saved = awaitTransactionStatus(validUserId, "FAILED");
    assertThat(saved.get("raw_text")).isEqualTo(rawText);
  }

  /**
   * El worker async corre en el {@code ThreadPoolTaskExecutor} de {@code AsyncConfig}, no en
   * el hilo del test: sin este poll, leer la fila inmediatamente después del POST casi
   * siempre la encontraría todavía PENDING.
   */
  private Map<String, Object> awaitTransactionStatus(UUID userId, String expectedStatus) throws InterruptedException {
    Instant deadline = Instant.now().plusSeconds(5);
    while (Instant.now().isBefore(deadline)) {
      List<Map<String, Object>> rows =
          jdbcTemplate.queryForList("SELECT status, amount, currency, merchant, raw_text FROM transactions WHERE user_id = ?", userId);
      if (!rows.isEmpty() && expectedStatus.equals(rows.get(0).get("status"))) {
        return rows.get(0);
      }
      Thread.sleep(50);
    }
    throw new AssertionError("La transacción de " + userId + " no llegó a " + expectedStatus + " a tiempo");
  }

  @Test
  void shouldReturn404WhenProfileDoesNotExist() throws Exception {
    UUID unknownUserId = UUID.randomUUID();

    mockMvc
        .perform(
            post("/api/v1/transactions/webhook")
                .header("Authorization", "Bearer " + BEARER_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"userId":"%s","rawText":"S/24.50 en Starbucks"}
                    """
                        .formatted(unknownUserId)))
        .andExpect(status().isNotFound());

    Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transactions", Integer.class);
    assertThat(count).isZero();
  }
}
