package pe.smartcash.cash.workspaces.interfaces.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 * E2E contra Postgres real (Testcontainers): el módulo "General" se siembra directo por JDBC
 * (en el alta real lo crea el handler del evento de IAM). Cubre CRUD de módulos y de sus
 * categorías, aislamiento entre usuarios y las invariantes del agregado expuestas como
 * códigos HTTP.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class WorkspaceControllerIT {

  @Container
  @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

  private static final String BEARER_TOKEN = "token-valido";
  private static final String OTHER_BEARER_TOKEN = "token-de-otro";

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;

  @MockitoBean private TokenService tokenService;

  private UUID ownerUserId;
  private UUID otherUserId;
  private UUID generalWorkspaceId;

  @BeforeEach
  void setUp() {
    jdbcTemplate.update("DELETE FROM workspace_categories");
    jdbcTemplate.update("DELETE FROM workspaces");
    jdbcTemplate.update("DELETE FROM user_profiles");

    ownerUserId = UUID.randomUUID();
    otherUserId = UUID.randomUUID();
    seedUser(ownerUserId, "Dueño");
    seedUser(otherUserId, "Otro");
    generalWorkspaceId = seedDefaultWorkspace(ownerUserId);
    seedDefaultWorkspace(otherUserId);

    Mockito.when(tokenService.validate(BEARER_TOKEN)).thenReturn(Optional.of(UserId.of(ownerUserId)));
    Mockito.when(tokenService.validate(OTHER_BEARER_TOKEN)).thenReturn(Optional.of(UserId.of(otherUserId)));
  }

  private void seedUser(UUID id, String name) {
    jdbcTemplate.update(
        "INSERT INTO user_profiles (id, display_name, inbox_address, created_at, updated_at) VALUES (?, ?, ?, now(), now())",
        id,
        name,
        "alias-" + id + "@inbox.smartcash.pe");
  }

  private UUID seedDefaultWorkspace(UUID ownerId) {
    UUID workspaceId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO workspaces (id, owner_id, name, color_hex, icon, is_default, created_at) VALUES (?, ?, 'General', '#8B5CF6', 'wallet', TRUE, now())",
        workspaceId,
        ownerId);
    jdbcTemplate.update(
        "INSERT INTO workspace_categories (id, workspace_id, code, display_name, icon, position) VALUES (?, ?, 'COMIDA', 'Comida', 'utensils', 0)",
        UUID.randomUUID(),
        workspaceId);
    jdbcTemplate.update(
        "INSERT INTO workspace_categories (id, workspace_id, code, display_name, icon, position) VALUES (?, ?, 'OTROS', 'Otros', 'circle-help', 1)",
        UUID.randomUUID(),
        workspaceId);
    return workspaceId;
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }

  @Test
  void listsOnlyTheOwnersWorkspacesWithGeneralFirst() throws Exception {
    mockMvc
        .perform(get("/api/v1/workspaces").header("Authorization", bearer(BEARER_TOKEN)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("General"))
        .andExpect(jsonPath("$[0].isDefault").value(true))
        .andExpect(jsonPath("$[0].categories.length()").value(2));
  }

  @Test
  void createsACustomWorkspaceSeededWithStarterCategories() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/workspaces")
                .header("Authorization", bearer(BEARER_TOKEN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Empresa\",\"colorHex\":\"#22C55E\",\"icon\":\"briefcase\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Empresa"))
        .andExpect(jsonPath("$.colorHex").value("#22C55E"))
        .andExpect(jsonPath("$.isDefault").value(false))
        .andExpect(jsonPath("$.categories.length()").value(8));
  }

  @Test
  void renamesAndRecustomizesAWorkspace() throws Exception {
    mockMvc
        .perform(
            patch("/api/v1/workspaces/{id}", generalWorkspaceId)
                .header("Authorization", bearer(BEARER_TOKEN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Personal\",\"colorHex\":\"#3B82F6\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Personal"))
        .andExpect(jsonPath("$.colorHex").value("#3B82F6"));
  }

  @Test
  void archivingTheDefaultWorkspaceIs409() throws Exception {
    mockMvc
        .perform(delete("/api/v1/workspaces/{id}", generalWorkspaceId).header("Authorization", bearer(BEARER_TOKEN)))
        .andExpect(status().isConflict());
  }

  @Test
  void addsRenamesAndArchivesAModuleCategory() throws Exception {
    String created =
        mockMvc
            .perform(
                post("/api/v1/workspaces/{id}/categories", generalWorkspaceId)
                    .header("Authorization", bearer(BEARER_TOKEN))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"displayName\":\"Nómina de empleados\",\"icon\":\"users\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.categories[?(@.code=='NOMINA_DE_EMPLEADOS')]").exists())
            .andReturn()
            .getResponse()
            .getContentAsString();

    java.util.List<String> ids =
        com.jayway.jsonpath.JsonPath.read(created, "$.categories[?(@.code=='NOMINA_DE_EMPLEADOS')].id");
    UUID newCategoryId = UUID.fromString(ids.get(0));

    mockMvc
        .perform(
            patch("/api/v1/workspaces/{w}/categories/{c}", generalWorkspaceId, newCategoryId)
                .header("Authorization", bearer(BEARER_TOKEN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"Planilla\",\"icon\":\"users\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.categories[?(@.code=='NOMINA_DE_EMPLEADOS')].displayName").value(hasItem("Planilla")));

    mockMvc
        .perform(
            delete("/api/v1/workspaces/{w}/categories/{c}", generalWorkspaceId, newCategoryId)
                .header("Authorization", bearer(BEARER_TOKEN)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.categories[?(@.code=='NOMINA_DE_EMPLEADOS')].archived").value(hasItem(true)));
  }

  @Test
  void aUserCannotSeeOrTouchAnotherUsersWorkspace() throws Exception {
    mockMvc
        .perform(get("/api/v1/workspaces/{id}", generalWorkspaceId).header("Authorization", bearer(OTHER_BEARER_TOKEN)))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(
            patch("/api/v1/workspaces/{id}", generalWorkspaceId)
                .header("Authorization", bearer(OTHER_BEARER_TOKEN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Hackeado\"}"))
        .andExpect(status().isNotFound());
  }
}
