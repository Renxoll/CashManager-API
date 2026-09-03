package pe.smartcash.cash.workspaces.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import pe.smartcash.cash.workspaces.domain.exception.DefaultWorkspaceProtectedException;
import pe.smartcash.cash.workspaces.domain.exception.DuplicateWorkspaceCategoryException;
import pe.smartcash.cash.workspaces.domain.exception.LastActiveWorkspaceCategoryException;
import pe.smartcash.cash.workspaces.domain.model.valueobjects.CategoryTemplate;
import pe.smartcash.cash.workspaces.domain.model.valueobjects.StarterCategories;
import pe.smartcash.cash.workspaces.domain.model.valueobjects.UserId;
import pe.smartcash.cash.workspaces.domain.model.valueobjects.WorkspaceCategoryId;
import pe.smartcash.cash.workspaces.domain.model.valueobjects.WorkspaceId;

class WorkspaceTest {

  private static final UserId OWNER = UserId.of(java.util.UUID.randomUUID());

  private Workspace customWorkspace() {
    return Workspace.open(
        WorkspaceId.newId(), OWNER, "Empresa", "#22C55E", "briefcase", StarterCategories.DEFAULT, Instant.now());
  }

  @Test
  void defaultWorkspaceStartsWithTheEightSeedCategories() {
    Workspace general = Workspace.openDefault(WorkspaceId.newId(), OWNER, StarterCategories.DEFAULT, Instant.now());

    assertThat(general.isDefault()).isTrue();
    assertThat(general.name()).isEqualTo("General");
    assertThat(general.activeCategories()).hasSize(8);
  }

  @Test
  void customWorkspaceKeepsItsColorAndIcon() {
    Workspace empresa = customWorkspace();

    assertThat(empresa.isDefault()).isFalse();
    assertThat(empresa.colorHex()).isEqualTo("#22C55E");
    assertThat(empresa.icon()).isEqualTo("briefcase");
  }

  @Test
  void invalidColorHexIsRejected() {
    assertThatThrownBy(
            () ->
                Workspace.open(
                    WorkspaceId.newId(), OWNER, "X", "verde", "briefcase", StarterCategories.DEFAULT, Instant.now()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void addingACategoryWithADuplicateCodeIsRejected() {
    Workspace empresa =
        Workspace.open(
            WorkspaceId.newId(),
            OWNER,
            "Empresa",
            "#22C55E",
            "briefcase",
            List.of(new CategoryTemplate("NOMINA", "Nómina", "users")),
            Instant.now());

    assertThatThrownBy(() -> empresa.addCategory(WorkspaceCategoryId.newId(), "NOMINA", "Nómina 2", "users"))
        .isInstanceOf(DuplicateWorkspaceCategoryException.class);
  }

  @Test
  void archivingTheLastActiveCategoryIsRejected() {
    Workspace empresa =
        Workspace.open(
            WorkspaceId.newId(),
            OWNER,
            "Empresa",
            "#22C55E",
            "briefcase",
            List.of(new CategoryTemplate("NOMINA", "Nómina", "users")),
            Instant.now());
    WorkspaceCategoryId only = empresa.activeCategories().get(0).id();

    assertThatThrownBy(() -> empresa.archiveCategory(only)).isInstanceOf(LastActiveWorkspaceCategoryException.class);
  }

  @Test
  void theDefaultWorkspaceCannotBeArchived() {
    Workspace general = Workspace.openDefault(WorkspaceId.newId(), OWNER, StarterCategories.DEFAULT, Instant.now());

    assertThatThrownBy(() -> general.archive(Instant.now())).isInstanceOf(DefaultWorkspaceProtectedException.class);
  }

  @Test
  void aCustomWorkspaceCanBeArchived() {
    Workspace empresa = customWorkspace();

    empresa.archive(Instant.now());

    assertThat(empresa.archived()).isTrue();
  }

  @Test
  void renamingAndRecustomizingUpdateTheWorkspace() {
    Workspace empresa = customWorkspace();

    empresa.rename("Mi Empresa SAC");
    empresa.recustomize("#3B82F6", "building");

    assertThat(empresa.name()).isEqualTo("Mi Empresa SAC");
    assertThat(empresa.colorHex()).isEqualTo("#3B82F6");
    assertThat(empresa.icon()).isEqualTo("building");
  }
}
