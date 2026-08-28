package pe.smartcash.cash.transactions.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import pe.smartcash.cash.transactions.domain.services.ExtractionResult;
import pe.smartcash.cash.transactions.domain.services.MerchantCategoryCache;
import pe.smartcash.cash.transactions.domain.services.TransactionExtractionService;
import pe.smartcash.cash.transactions.domain.model.valueobjects.CategoryCode;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Merchant;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Money;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionType;

/**
 * Cubre el camino completo de Feature B por la ruta SendGrid: remitente no confiable -> queda
 * en {@code pending_senders} en vez de descartarse en silencio -> el usuario lo aprueba vía
 * {@code PendingSenderController} -> el mismo remitente reenviado ahora sí ingesta. Mismo
 * patrón Testcontainers + MockMvc que {@code TransactionWebhookControllerIT}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SendGridInboundWebhookControllerIT {

  @Container
  @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

  private static final String BEARER_TOKEN = "token-valido";
  private static final String INBOUND_TOKEN = "dev-only-inbound-token-cambiar-en-produccion";
  private static final String UNTRUSTED_FROM = "Notificaciones <alertas@billetera-nueva.pe>";
  private static final String UNTRUSTED_DOMAIN = "billetera-nueva.pe";

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;

  @MockitoBean private TokenService tokenService;
  @MockitoBean private TransactionExtractionService extractionService;
  @MockitoBean private MerchantCategoryCache merchantCategoryCache;

  private UUID userId;
  private String inboxAddress;

  @BeforeEach
  void setUp() {
    jdbcTemplate.update("DELETE FROM pending_senders");
    jdbcTemplate.update("DELETE FROM user_trusted_senders");
    jdbcTemplate.update("DELETE FROM transactions");
    jdbcTemplate.update("DELETE FROM user_profiles");

    userId = UUID.randomUUID();
    inboxAddress = "alias-" + userId + "@inbox.smartcash.pe";
    jdbcTemplate.update(
        "INSERT INTO user_profiles (id, display_name, inbox_address, created_at, updated_at) VALUES (?, ?, ?, now(), now())",
        userId,
        "Usuario de Test",
        inboxAddress);

    when(tokenService.validate(BEARER_TOKEN)).thenReturn(Optional.of(UserId.of(userId)));
    when(merchantCategoryCache.findCategoryFor(any())).thenReturn(Optional.empty());
    when(extractionService.extract(anyString()))
        .thenReturn(
            new ExtractionResult(new Money(new BigDecimal("10.00"), "PEN"), new Merchant("Comercio"), CategoryCode.COMPRAS, TransactionType.EXPENSE));
  }

  @Test
  void untrustedSenderIsQueuedInsteadOfDiscarded() throws Exception {
    mockMvc
        .perform(
            multipart("/api/v1/transactions/inbound")
                .param("token", INBOUND_TOKEN)
                .param("to", inboxAddress)
                .param("from", UNTRUSTED_FROM)
                .param("text", "Consumo de S/10.00"))
        .andExpect(status().isOk());

    List<Map<String, Object>> pending =
        jdbcTemplate.queryForList("SELECT * FROM pending_senders WHERE user_id = ?", userId);
    assertThat(pending).hasSize(1);
    assertThat(pending.get(0).get("domain")).isEqualTo(UNTRUSTED_DOMAIN);
    assertThat(pending.get(0).get("status")).isEqualTo("PENDING");
    assertThat((Integer) pending.get(0).get("occurrence_count")).isEqualTo(1);

    Integer transactionCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transactions", Integer.class);
    assertThat(transactionCount).isZero();
  }

  @Test
  void anotherEmailFromTheSameUnapprovedDomainJustBumpsTheOccurrenceCount() throws Exception {
    sendUntrustedEmail();
    sendUntrustedEmail();

    List<Map<String, Object>> pending = jdbcTemplate.queryForList("SELECT * FROM pending_senders WHERE user_id = ?", userId);
    assertThat(pending).hasSize(1);
    assertThat((Integer) pending.get(0).get("occurrence_count")).isEqualTo(2);
  }

  @Test
  void approvingThePendingSenderLetsFutureEmailsFromThatDomainIngest() throws Exception {
    sendUntrustedEmail();

    UUID pendingSenderId =
        (UUID) jdbcTemplate.queryForList("SELECT id FROM pending_senders WHERE user_id = ?", userId).get(0).get("id");

    mockMvc
        .perform(post("/api/v1/pending-senders/{id}/approve", pendingSenderId).header("Authorization", "Bearer " + BEARER_TOKEN))
        .andExpect(status().isNoContent());

    Boolean trusted =
        jdbcTemplate.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM user_trusted_senders WHERE user_id = ? AND domain = ?)",
            Boolean.class,
            userId,
            UNTRUSTED_DOMAIN);
    assertThat(trusted).isTrue();

    // El correo que disparó la fila NO se reprocesa (decisión de producto: "solo futuros") --
    // solo un reenvío nuevo, después de aprobar, debe ingestar.
    sendUntrustedEmail();

    Integer transactionCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transactions WHERE user_id = ?", Integer.class, userId);
    assertThat(transactionCount).isEqualTo(1);
  }

  @Test
  void listPendingSendersReturnsOnlyTheAuthenticatedUsersRows() throws Exception {
    sendUntrustedEmail();

    mockMvc
        .perform(get("/api/v1/pending-senders").header("Authorization", "Bearer " + BEARER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].domain").value(UNTRUSTED_DOMAIN));
  }

  @Test
  void rejectingAPendingSenderLeavesFutureEmailsUnqueued() throws Exception {
    sendUntrustedEmail();
    UUID pendingSenderId =
        (UUID) jdbcTemplate.queryForList("SELECT id FROM pending_senders WHERE user_id = ?", userId).get(0).get("id");

    mockMvc
        .perform(post("/api/v1/pending-senders/{id}/reject", pendingSenderId).header("Authorization", "Bearer " + BEARER_TOKEN))
        .andExpect(status().isNoContent());

    // Un dominio ya rechazado no "revive": otro correo del mismo dominio no debe recrear la
    // fila ni sumarle occurrence_count (ver PendingSender.recordAnotherSighting).
    sendUntrustedEmail();

    List<Map<String, Object>> pending = jdbcTemplate.queryForList("SELECT status, occurrence_count FROM pending_senders WHERE user_id = ?", userId);
    assertThat(pending).hasSize(1);
    assertThat(pending.get(0).get("status")).isEqualTo("REJECTED");
    assertThat((Integer) pending.get(0).get("occurrence_count")).isEqualTo(1);
  }

  @Test
  void approvingSomeoneElsesPendingSenderReturns404() throws Exception {
    sendUntrustedEmail();
    UUID pendingSenderId =
        (UUID) jdbcTemplate.queryForList("SELECT id FROM pending_senders WHERE user_id = ?", userId).get(0).get("id");

    UUID otherUserId = UUID.randomUUID();
    when(tokenService.validate("otro-token")).thenReturn(Optional.of(UserId.of(otherUserId)));

    mockMvc
        .perform(post("/api/v1/pending-senders/{id}/approve", pendingSenderId).header("Authorization", "Bearer otro-token"))
        .andExpect(status().isNotFound());
  }

  private void sendUntrustedEmail() throws Exception {
    mockMvc
        .perform(
            multipart("/api/v1/transactions/inbound")
                .param("token", INBOUND_TOKEN)
                .param("to", inboxAddress)
                .param("from", UNTRUSTED_FROM)
                .param("text", "Consumo de S/10.00"))
        .andExpect(status().isOk());
  }
}
