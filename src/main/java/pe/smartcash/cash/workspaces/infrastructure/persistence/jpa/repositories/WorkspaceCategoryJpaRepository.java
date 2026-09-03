package pe.smartcash.cash.workspaces.infrastructure.persistence.jpa.repositories;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.smartcash.cash.workspaces.infrastructure.persistence.WorkspaceCategoryJpaEntity;

public interface WorkspaceCategoryJpaRepository extends JpaRepository<WorkspaceCategoryJpaEntity, UUID> {

  List<WorkspaceCategoryJpaEntity> findAllByWorkspaceId(UUID workspaceId);

  List<WorkspaceCategoryJpaEntity> findAllByWorkspaceIdIn(List<UUID> workspaceIds);

  List<WorkspaceCategoryJpaEntity> findAllByIdIn(List<UUID> ids);
}
