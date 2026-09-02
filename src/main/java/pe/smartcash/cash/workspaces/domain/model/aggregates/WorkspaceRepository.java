package pe.smartcash.cash.workspaces.domain.model.aggregates;

import java.util.List;
import java.util.Optional;
import pe.smartcash.cash.workspaces.domain.model.valueobjects.UserId;
import pe.smartcash.cash.workspaces.domain.model.valueobjects.WorkspaceId;

public interface WorkspaceRepository {

  void save(Workspace workspace);

  Optional<Workspace> findById(WorkspaceId id);

  /** Módulos no archivados del usuario, con el "General" primero. */
  List<Workspace> findAllActiveByOwner(UserId ownerId);

  Optional<Workspace> findDefaultByOwner(UserId ownerId);

  boolean existsDefaultForOwner(UserId ownerId);
}
