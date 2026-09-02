package pe.smartcash.cash.workspaces.infrastructure.persistence.jpa.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.smartcash.cash.workspaces.infrastructure.persistence.WorkspaceJpaEntity;

public interface WorkspaceJpaRepository extends JpaRepository<WorkspaceJpaEntity, UUID> {

  List<WorkspaceJpaEntity> findAllByOwnerIdAndArchivedAtIsNullOrderByIsDefaultDescCreatedAtAsc(UUID ownerId);

  Optional<WorkspaceJpaEntity> findByOwnerIdAndIsDefaultTrue(UUID ownerId);

  boolean existsByOwnerIdAndIsDefaultTrue(UUID ownerId);
}
