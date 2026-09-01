package pe.smartcash.cash.analytics.interfaces.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
 * Confirma que el desglose por ingreso/gasto no se contamina entre sí -- una fila INCOME
 * seedeada junto a una EXPENSE no debe aparecer en {@code totalSpent} ni en el desglose por
 * categoría, y viceversa. Mismo patrón Testcontainers que el resto de los IT de transactions.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class DashboardControllerIT {

  @Container
  @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

  private static final String BEARER_TOKEN = "token-valido";

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;

  @MockitoBean private TokenService tokenService;

  private UUID userId;

  @BeforeEach
  void setUp() {
    jdbcTemplate.update("DELETE FROM transactions");
    jdbcTemplate.update("DELETE FROM user_profiles");

    userId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO user_profiles (id, display_name, inbox_address, created_at, updated_at) VALUES (?, ?, ?, now(), now())",
        userId,
        "Usuario de Test",
        "alias-" + userId + "@inbox.smartcash.pe");
    Mockito.when(tokenService.validate(BEARER_TOKEN)).thenReturn(Optional.of(UserId.of(userId)));

    Long categoryId = jdbcTemplate.queryForObject("SELECT id FROM categories WHERE code = 'COMIDA'", Long.class);
    jdbcTemplate.update(
        "INSERT INTO transactions (id, user_id, category_id, raw_text, amount, currency, merchant, status, extraction_source, type, created_at, processed_at) "
            + "VALUES (?, ?, ?, 'S/50 en restaurante', 50.00, 'PEN', 'Restaurante', 'PROCESSED', 'LLM', 'EXPENSE', now(), now())",
        UUID.randomUUID(),
        userId,
        categoryId);
    jdbcTemplate.update(
        "INSERT INTO transactions (id, user_id, category_id, raw_text, amount, currency, merchant, status, extraction_source, type, created_at, processed_at) "
            + "VALUES (?, ?, NULL, 'Se abonó S/1500', 1500.00, 'PEN', 'Empleador', 'PROCESSED', 'LLM', 'INCOME', now(), now())",
        UUID.randomUUID(),
        userId);
  }

  @Test
  void monthlySummarySeparatesIncomeFromExpense() throws Exception {
    mockMvc
        .perform(get("/api/v1/analytics/monthly-summary").header("Authorization", "Bearer " + BEARER_TOKEN))
        .andExpect(status().isOk())
        // Números planos (no BigDecimal) a propósito: JsonPath parsea números JSON como
        // Double, y comparar contra un BigDecimal (incluso con comparesEqualTo) revienta con
        // ClassCastException porque Comparable.compareTo exige el mismo tipo en ambos lados.
        .andExpect(jsonPath("$.currencies.length()").value(1))
        .andExpect(jsonPath("$.currencies[0].currency").value("PEN"))
        .andExpect(jsonPath("$.currencies[0].totalSpent").value(50.0))
        .andExpect(jsonPath("$.currencies[0].totalIncome").value(1500.0))
        .andExpect(jsonPath("$.currencies[0].breakdown.length()").value(1))
        .andExpect(jsonPath("$.currencies[0].breakdown[0].categoryName").value("Comida"))
        .andExpect(jsonPath("$.currencies[0].breakdown[0].amount").value(50.0));
  }

  @Test
  void monthlySummaryKeepsCurrenciesSeparate() throws Exception {
    Long categoryId = jdbcTemplate.queryForObject("SELECT id FROM categories WHERE code = 'SERVICIOS'", Long.class);
    jdbcTemplate.update(
        "INSERT INTO transactions (id, user_id, category_id, raw_text, amount, currency, merchant, status, extraction_source, type, created_at, processed_at) "
            + "VALUES (?, ?, ?, 'Netflix $9.99', 9.99, 'USD', 'Netflix', 'PROCESSED', 'LLM', 'EXPENSE', now(), now())",
        UUID.randomUUID(),
        userId,
        categoryId);

    mockMvc
        .perform(get("/api/v1/analytics/monthly-summary").header("Authorization", "Bearer " + BEARER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currencies.length()").value(2))
        // PEN va primero (moneda principal de la app), USD después -- ver DashboardQueryServiceImpl.
        .andExpect(jsonPath("$.currencies[0].currency").value("PEN"))
        .andExpect(jsonPath("$.currencies[0].totalSpent").value(50.0))
        .andExpect(jsonPath("$.currencies[1].currency").value("USD"))
        .andExpect(jsonPath("$.currencies[1].totalSpent").value(9.99))
        .andExpect(jsonPath("$.currencies[1].totalIncome").value(0.0))
        .andExpect(jsonPath("$.currencies[1].breakdown[0].categoryName").value("Servicios"));
  }
}
