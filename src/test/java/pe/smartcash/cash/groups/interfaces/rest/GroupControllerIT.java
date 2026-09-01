package pe.smartcash.cash.groups.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
 * E2E contra Postgres real (Testcontainers), mismo patrón que {@code TransactionControllerIT}.
 * Se siembra tanto {@code credentials} (para que las invitaciones por email resuelvan una
 * cuenta real, ver {@code IamQueryServiceImpl}) como {@code user_profiles} (para el nombre a
 * mostrar de cada miembro, ver {@code UserProfileQueryService}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class GroupControllerIT {

  @Container
  @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

  private static final String OWNER_TOKEN = "token-owner";
  private static final String INVITEE_TOKEN = "token-invitee";
  private static final String STRANGER_TOKEN = "token-stranger";

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;

  @MockitoBean private TokenService tokenService;

  private UUID ownerUserId;
  private UUID inviteeUserId;
  private UUID strangerUserId;
  private String inviteeEmail;

  @BeforeEach
  void setUp() {
    jdbcTemplate.update("DELETE FROM expense_shares");
    jdbcTemplate.update("DELETE FROM shared_expenses");
    jdbcTemplate.update("DELETE FROM settlements");
    jdbcTemplate.update("DELETE FROM group_memberships");
    jdbcTemplate.update("DELETE FROM groups");
    jdbcTemplate.update("DELETE FROM user_profiles");
    jdbcTemplate.update("DELETE FROM credentials");

    ownerUserId = UUID.randomUUID();
    inviteeUserId = UUID.randomUUID();
    strangerUserId = UUID.randomUUID();
    inviteeEmail = "invitee-" + inviteeUserId + "@example.com";

    seedUser(ownerUserId, "Dueño", "dueno-" + ownerUserId + "@example.com");
    seedUser(inviteeUserId, "Invitado", inviteeEmail);
    seedUser(strangerUserId, "Ajeno", "ajeno-" + strangerUserId + "@example.com");

    Mockito.when(tokenService.validate(OWNER_TOKEN)).thenReturn(Optional.of(UserId.of(ownerUserId)));
    Mockito.when(tokenService.validate(INVITEE_TOKEN)).thenReturn(Optional.of(UserId.of(inviteeUserId)));
    Mockito.when(tokenService.validate(STRANGER_TOKEN)).thenReturn(Optional.of(UserId.of(strangerUserId)));
  }

  private void seedUser(UUID userId, String displayName, String email) {
    jdbcTemplate.update(
        "INSERT INTO credentials (id, email, hashed_password, created_at) VALUES (?, ?, 'x', now())", userId, email);
    jdbcTemplate.update(
        "INSERT INTO user_profiles (id, display_name, inbox_address, created_at, updated_at) VALUES (?, ?, ?, now(), now())",
        userId,
        displayName,
        "alias-" + userId + "@inbox.smartcash.pe");
  }

  private UUID createGroupAsOwner() throws Exception {
    var response =
        mockMvc
            .perform(
                post("/api/v1/groups")
                    .header("Authorization", "Bearer " + OWNER_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"name":"Viaje a Cusco"}
                        """))
            .andExpect(status().isCreated())
            .andReturn();
    return UUID.fromString(JsonPath.read(response.getResponse().getContentAsString(), "$.groupId").toString());
  }

  @Test
  void shouldCreateAGroupAndAutoAcceptTheCreator() throws Exception {
    UUID groupId = createGroupAsOwner();

    mockMvc
        .perform(get("/api/v1/groups").header("Authorization", "Bearer " + OWNER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].groupId").value(groupId.toString()))
        .andExpect(jsonPath("$[0].memberCount").value(1));
  }

  @Test
  void shouldInviteARegisteredUserByEmail() throws Exception {
    UUID groupId = createGroupAsOwner();

    mockMvc
        .perform(
            post("/api/v1/groups/{groupId}/invites", groupId)
                .header("Authorization", "Bearer " + OWNER_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + inviteeEmail + "\"}"))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get("/api/v1/groups/invites").header("Authorization", "Bearer " + INVITEE_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].groupId").value(groupId.toString()))
        .andExpect(jsonPath("$[0].groupName").value("Viaje a Cusco"));
  }

  @Test
  void shouldReturn400WhenInvitingAnUnregisteredEmail() throws Exception {
    UUID groupId = createGroupAsOwner();

    mockMvc
        .perform(
            post("/api/v1/groups/{groupId}/invites", groupId)
                .header("Authorization", "Bearer " + OWNER_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"nadie-tiene-esta-cuenta@example.com"}
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn409WhenInvitingTheSamePersonTwice() throws Exception {
    UUID groupId = createGroupAsOwner();
    String body = "{\"email\":\"" + inviteeEmail + "\"}";

    mockMvc
        .perform(post("/api/v1/groups/{groupId}/invites", groupId).header("Authorization", "Bearer " + OWNER_TOKEN).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated());
    mockMvc
        .perform(post("/api/v1/groups/{groupId}/invites", groupId).header("Authorization", "Bearer " + OWNER_TOKEN).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isConflict());
  }

  private UUID inviteAndAccept(UUID groupId) throws Exception {
    var response =
        mockMvc
            .perform(
                post("/api/v1/groups/{groupId}/invites", groupId)
                    .header("Authorization", "Bearer " + OWNER_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + inviteeEmail + "\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    UUID membershipId =
        UUID.fromString(JsonPath.read(response.getResponse().getContentAsString(), "$.membershipId").toString());

    mockMvc.perform(post("/api/v1/groups/invites/{id}/accept", membershipId).header("Authorization", "Bearer " + INVITEE_TOKEN)).andExpect(status().isNoContent());
    return membershipId;
  }

  @Test
  void shouldAllowTheInviteeToAcceptAndSeeTheGroup() throws Exception {
    UUID groupId = createGroupAsOwner();
    inviteAndAccept(groupId);

    mockMvc
        .perform(get("/api/v1/groups/{groupId}", groupId).header("Authorization", "Bearer " + INVITEE_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.members.length()").value(2));
  }

  @Test
  void shouldReturn404WhenAStrangerTriesToViewTheGroup() throws Exception {
    UUID groupId = createGroupAsOwner();

    mockMvc
        .perform(get("/api/v1/groups/{groupId}", groupId).header("Authorization", "Bearer " + STRANGER_TOKEN))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldSplitAnExpenseEquallyAndPersistShares() throws Exception {
    UUID groupId = createGroupAsOwner();
    inviteAndAccept(groupId);

    mockMvc
        .perform(
            post("/api/v1/groups/{groupId}/expenses", groupId)
                .header("Authorization", "Bearer " + OWNER_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"description":"Hotel","amount":100.00,"currency":"PEN","paidByUserId":"%s","participantUserIds":["%s","%s"]}
                    """
                        .formatted(ownerUserId, ownerUserId, inviteeUserId)))
        .andExpect(status().isCreated());

    var shareAmounts =
        jdbcTemplate.queryForList(
            "SELECT es.user_id, es.amount FROM expense_shares es JOIN shared_expenses se ON se.id = es.expense_id WHERE se.group_id = ?", groupId);
    assertThat(shareAmounts).hasSize(2);

    mockMvc
        .perform(get("/api/v1/groups/{groupId}", groupId).header("Authorization", "Bearer " + OWNER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.members[?(@.userId=='" + ownerUserId + "')].balances[0].amount").value(50.0))
        .andExpect(jsonPath("$.members[?(@.userId=='" + inviteeUserId + "')].balances[0].amount").value(-50.0));
  }

  @Test
  void shouldRecordASettlementAndZeroOutTheBalance() throws Exception {
    UUID groupId = createGroupAsOwner();
    inviteAndAccept(groupId);

    mockMvc
        .perform(
            post("/api/v1/groups/{groupId}/expenses", groupId)
                .header("Authorization", "Bearer " + OWNER_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"description":"Hotel","amount":100.00,"currency":"PEN","paidByUserId":"%s","participantUserIds":["%s","%s"]}
                    """
                        .formatted(ownerUserId, ownerUserId, inviteeUserId)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/v1/groups/{groupId}/settlements", groupId)
                .header("Authorization", "Bearer " + INVITEE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"toUserId":"%s","amount":50.00,"currency":"PEN"}
                    """.formatted(ownerUserId)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get("/api/v1/groups/{groupId}", groupId).header("Authorization", "Bearer " + OWNER_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.members[?(@.userId=='" + ownerUserId + "')].balances[0].amount").value(0.0))
        .andExpect(jsonPath("$.members[?(@.userId=='" + inviteeUserId + "')].balances[0].amount").value(0.0))
        .andExpect(jsonPath("$.simplifiedDebts.length()").value(0));
  }
}
