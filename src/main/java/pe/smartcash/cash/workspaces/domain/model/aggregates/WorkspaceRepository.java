package pe.smartcash.cash.workspaces.domain.model.aggregates;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import pe.smartcash.cash.workspaces.domain.model.valueobjects.UserId;
import pe.smartcash.cash.workspaces.domain.model.valueobjects.WorkspaceCategoryId;
import pe.smartcash.cash.workspaces.domain.model.valueobjects.WorkspaceId;

public interface WorkspaceRepository {

  void save(Workspace workspace);

  Optional<Workspace> findById(WorkspaceId id);

  /** Módulos no archivados del usuario, con el "General" primero. */
  List<Workspace> findAllActiveByOwner(UserId ownerId);

  Optional<Workspace> findDefaultByOwner(UserId ownerId);

  boolean existsDefaultForOwner(UserId ownerId);

  /**
   * Categorías sueltas por id, sin cargar el módulo entero -- lo usa el ACL de Transactions
   * para resolver nombres/íconos de categorías de módulos custom en sus read-models.
   */
  List<WorkspaceCategory> findCategoriesByIds(Collection<WorkspaceCategoryId> ids);
}
