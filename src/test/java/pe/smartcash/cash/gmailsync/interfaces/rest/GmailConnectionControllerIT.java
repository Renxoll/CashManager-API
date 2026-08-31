package pe.smartcash.cash.gmailsync.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
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
import pe.smartcash.cash.gmailsync.infrastructure.crypto.TokenCipher;
import pe.smartcash.cash.iam.domain.model.valueobjects.UserId;
import pe.smartcash.cash.iam.domain.services.TokenService;

/**
 * E2E contra Postgres real (Testcontainers). {@code GoogleOAuthPort} se mockea (llamada
 * externa real a Google) -- lo único que este IT ejercita es el controller/ownership sobre
 * filas seedeadas directo en la BD, mismo patrón que el resto de los IT del proyecto.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class GmailConnectionControllerIT {

  @Container
  @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

  private static final String BEARER_TOKEN = "token-valido";
  private static final String OTHER_BEARER_TOKEN = "token-de-otro-usuario";

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private TokenCipher tokenCipher;

  @MockitoBean private TokenService tokenService;
  @MockitoBean private pe.smartcash.cash.gmailsync.domain.services.GoogleOAuthPort googleOAuthPort;
  @MockitoBean private pe.smartcash.cash.gmailsync.domain.services.GmailMessagePort gmailMessagePort;

  private UUID ownerUserId;
  private UUID otherUserId;

  @BeforeEach
  void setUp() {
    jdbcTemplate.update("DELETE FROM gmail_connections");
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

    // Sin correos nuevos en Gmail: el sync recorre las conexiones pero no ingesta nada.
    Mockito.when(gmailMessagePort.findMatchingMessagesSince(any(), any(), anySet())).thenReturn(List.of());
    Mockito.when(gmailMessagePort.findCandidateMessagesSince(any(), any(), anySet())).thenReturn(List.of());
  }

  private UUID seedConnection(UUID userId, String email) {
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO gmail_connections (id, user_id, email, access_token, refresh_token, access_token_expires_at, connected_at, updated_at) "
            + "VALUES (?, ?, ?, ?, ?, now() + interval '1 hour', now(), now())",
        id,
        userId,
        email,
        tokenCipher.encrypt("access-token"),
        tokenCipher.encrypt("refresh-token"));
    return id;
  }

  @Test
  void shouldListOnlyTheAuthenticatedUsersConnections() throws Exception {
    seedConnection(ownerUserId, "duenio@gmail.com");
    seedConnection(otherUserId, "otro@gmail.com");

    mockMvc
        .perform(get("/api/v1/gmail/connections").header("Authorization", "Bearer " + BEARER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].email").value("duenio@gmail.com"));
  }

  @Test
  void shouldListMultipleConnectionsForTheSameUser() throws Exception {
    seedConnection(ownerUserId, "primero@gmail.com");
    seedConnection(ownerUserId, "segundo@gmail.com");

    mockMvc
        .perform(get("/api/v1/gmail/connections").header("Authorization", "Bearer " + BEARER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void shouldDisconnectOwnConnection() throws Exception {
    UUID id = seedConnection(ownerUserId, "duenio@gmail.com");

    mockMvc
        .perform(delete("/api/v1/gmail/connections/{id}", id).header("Authorization", "Bearer " + BEARER_TOKEN))
        .andExpect(status().isNoContent());

    Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM gmail_connections WHERE id = ?", Integer.class, id);
    assertThat(count).isZero();
  }

  @Test
  void shouldSyncOnlyTheAuthenticatedUsersConnections() throws Exception {
    seedConnection(ownerUserId, "duenio@gmail.com");
    seedConnection(otherUserId, "otro@gmail.com");

    mockMvc
        .perform(post("/api/v1/gmail/connections/sync").header("Authorization", "Bearer " + BEARER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.connectionsSynced").value(1))
        .andExpect(jsonPath("$.transactionsIngested").value(0))
        .andExpect(jsonPath("$.pendingSendersRegistered").value(0))
        .andExpect(jsonPath("$.syncedAt").exists());

    Mockito.verify(gmailMessagePort).findMatchingMessagesSince(any(), any(), anySet());
  }

  @Test
  void shouldReturnZeroCountsWhenUserHasNoConnections() throws Exception {
    mockMvc
        .perform(post("/api/v1/gmail/connections/sync").header("Authorization", "Bearer " + BEARER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.connectionsSynced").value(0));
  }

  @Test
  void shouldReturn404WhenDisconnectingSomeoneElsesConnection() throws Exception {
    UUID id = seedConnection(otherUserId, "otro@gmail.com");

    mockMvc
        .perform(delete("/api/v1/gmail/connections/{id}", id).header("Authorization", "Bearer " + BEARER_TOKEN))
        .andExpect(status().isNotFound());

    Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM gmail_connections WHERE id = ?", Integer.class, id);
    assertThat(count).isEqualTo(1);
  }
}
