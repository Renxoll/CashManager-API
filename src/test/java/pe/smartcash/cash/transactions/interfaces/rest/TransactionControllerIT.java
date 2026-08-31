package pe.smartcash.cash.transactions.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import pe.smartcash.cash.iam.domain.model.valueobjects.UserId;
import pe.smartcash.cash.iam.domain.services.TokenService;

/**
 * E2E contra Postgres real (Testcontainers): las transacciones se insertan directo por JDBC
 * ya PROCESSED (no hace falta pasar por el webhook/LLM para probar lectura/edición), mismo
 * patrón de setup que {@code TransactionWebhookControllerIT}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TransactionControllerIT {

  @Container
  @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

  private static final String BEARER_TOKEN = "token-valido";
  private static final String OTHER_BEARER_TOKEN = "token-de-otro-usuario";

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;

  @MockitoBean private TokenService tokenService;

  private UUID ownerUserId;
  private UUID otherUserId;

  @BeforeEach
  void setUp() {
    jdbcTemplate.update("DELETE FROM transactions");
    jdbcTemplate.update("DELETE FROM user_profiles");

    ownerUserId = UUID.randomUUID();
    otherUserId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO user_profiles (id, display_name, inbox_address, created_at, updated_at) VALUES (?, ?, ?, now(), now())",
        ownerUserId,
        "Dueño",
        "alias-" + ownerUserId + "@inbox.smartcash.pe");
    jdbcTemplate.update(
        "INSERT INTO user_profiles (id, display_name, inbox_address, created_at, updated_at) VALUES (?, ?, ?, now(), now())",
        otherUserId,
        "Otro",
        "alias-" + otherUserId + "@inbox.smartcash.pe");

    Mockito.when(tokenService.validate(BEARER_TOKEN)).thenReturn(Optional.of(UserId.of(ownerUserId)));
    Mockito.when(tokenService.validate(OTHER_BEARER_TOKEN)).thenReturn(Optional.of(UserId.of(otherUserId)));
  }

  private UUID seedProcessedTransaction(UUID userId, String categoryCode) {
    UUID transactionId = UUID.randomUUID();
    Long categoryId = jdbcTemplate.queryForObject("SELECT id FROM categories WHERE code = ?", Long.class, categoryCode);
    jdbcTemplate.update(
        "INSERT INTO transactions (id, user_id, category_id, raw_text, amount, currency, merchant, status, extraction_source, created_at, processed_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, 'PROCESSED', 'LLM', now(), now())",
        transactionId,
        userId,
        categoryId,
        "S/24.50 en Starbucks",
        new BigDecimal("24.50"),
        "PEN",
        "Starbucks");
    return transactionId;
  }

  @Test
  void shouldListOnlyTheAuthenticatedUsersTransactions() throws Exception {
    seedProcessedTransaction(ownerUserId, "COMIDA");
    seedProcessedTransaction(otherUserId, "TRANSPORTE");

    mockMvc
        .perform(get("/api/v1/transactions").header("Authorization", "Bearer " + BEARER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.items[0].merchant").value("Starbucks"));
  }

  @Test
  void shouldListAllCategories() throws Exception {
    mockMvc
        .perform(get("/api/v1/transactions/categories").header("Authorization", "Bearer " + BEARER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(8));
  }

  @Test
  void shouldUpdateCategoryOfOwnTransaction() throws Exception {
    UUID transactionId = seedProcessedTransaction(ownerUserId, "COMIDA");

    mockMvc
        .perform(
            patch("/api/v1/transactions/{id}/category", transactionId)
                .header("Authorization", "Bearer " + BEARER_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"categoryCode":"TRANSPORTE"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.categoryCode").value("TRANSPORTE"));

    String storedCode =
        jdbcTemplate.queryForObject(
            "SELECT c.code FROM transactions t JOIN categories c ON c.id = t.category_id WHERE t.id = ?", String.class, transactionId);
    assertThat(storedCode).isEqualTo("TRANSPORTE");
  }

  @Test
  void shouldReturn404WhenUpdatingCategoryOfSomeoneElsesTransaction() throws Exception {
    UUID transactionId = seedProcessedTransaction(otherUserId, "COMIDA");

    mockMvc
        .perform(
            patch("/api/v1/transactions/{id}/category", transactionId)
                .header("Authorization", "Bearer " + BEARER_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"categoryCode":"TRANSPORTE"}
                    """))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldReturn400ForAnInvalidCategoryCode() throws Exception {
    UUID transactionId = seedProcessedTransaction(ownerUserId, "COMIDA");

    mockMvc
        .perform(
            patch("/api/v1/transactions/{id}/category", transactionId)
                .header("Authorization", "Bearer " + BEARER_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"categoryCode":"NO_EXISTE"}
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldMarkAndUnmarkOwnTransactionAsInternalTransfer() throws Exception {
    UUID transactionId = seedProcessedTransaction(ownerUserId, "COMIDA");

    mockMvc
        .perform(
            patch("/api/v1/transactions/{id}/internal-transfer", transactionId)
                .header("Authorization", "Bearer " + BEARER_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"internalTransfer":true}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.internalTransfer").value(true));

    Boolean stored =
        jdbcTemplate.queryForObject("SELECT internal_transfer FROM transactions WHERE id = ?", Boolean.class, transactionId);
    assertThat(stored).isTrue();

    mockMvc
        .perform(
            patch("/api/v1/transactions/{id}/internal-transfer", transactionId)
                .header("Authorization", "Bearer " + BEARER_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"internalTransfer":false}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.internalTransfer").value(false));
  }

  @Test
  void shouldReturn404WhenMarkingSomeoneElsesTransactionAsInternalTransfer() throws Exception {
    UUID transactionId = seedProcessedTransaction(otherUserId, "COMIDA");

    mockMvc
        .perform(
            patch("/api/v1/transactions/{id}/internal-transfer", transactionId)
                .header("Authorization", "Bearer " + BEARER_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"internalTransfer":true}
                    """))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldReturn404ForSomeoneElsesTransactionOnGetById() throws Exception {
    UUID transactionId = seedProcessedTransaction(otherUserId, "COMIDA");

    mockMvc
        .perform(get("/api/v1/transactions/{id}", transactionId).header("Authorization", "Bearer " + BEARER_TOKEN))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldReturnIncomeTransactionWithoutCategory() throws Exception {
    UUID transactionId = seedProcessedIncomeTransaction(ownerUserId);

    mockMvc
        .perform(get("/api/v1/transactions/{id}", transactionId).header("Authorization", "Bearer " + BEARER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("INCOME"))
        .andExpect(jsonPath("$.categoryCode").value(org.hamcrest.Matchers.nullValue()));
  }

  @Test
  void shouldRecordManualIncomeForAuthenticatedUser() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/transactions/income")
                .header("Authorization", "Bearer " + BEARER_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"amount":1500.00,"currency":"pen","source":"Sueldo agosto"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("INCOME"))
        .andExpect(jsonPath("$.amount").value(1500.00))
        .andExpect(jsonPath("$.currency").value("PEN"))
        .andExpect(jsonPath("$.merchant").value("Sueldo agosto"))
        .andExpect(jsonPath("$.categoryCode").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.status").value("PROCESSED"));

    Long count = jdbcTemplate.queryForObject(
        "SELECT count(*) FROM transactions WHERE user_id = ? AND type = 'INCOME' AND extraction_source = 'MANUAL'",
        Long.class,
        ownerUserId);
    assertThat(count).isEqualTo(1L);
  }

  @Test
  void shouldReturn400WhenManualIncomeAmountIsMissing() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/transactions/income")
                .header("Authorization", "Bearer " + BEARER_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"currency":"PEN","source":"Sueldo agosto"}
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400WhenManualIncomeAmountIsNotPositive() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/transactions/income")
                .header("Authorization", "Bearer " + BEARER_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"amount":0,"currency":"PEN","source":"Sueldo agosto"}
                    """))
        .andExpect(status().isBadRequest());
  }

  private UUID seedProcessedIncomeTransaction(UUID userId) {
    UUID transactionId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO transactions (id, user_id, category_id, raw_text, amount, currency, merchant, status, extraction_source, type, created_at, processed_at) "
            + "VALUES (?, ?, NULL, ?, ?, ?, ?, 'PROCESSED', 'LLM', 'INCOME', now(), now())",
        transactionId,
        userId,
        "Se abonó S/1500.00 a tu cuenta",
        new BigDecimal("1500.00"),
        "PEN",
        "Juan Pérez");
    return transactionId;
  }
}
