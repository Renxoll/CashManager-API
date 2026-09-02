package pe.smartcash.cash.workspaces.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import pe.smartcash.cash.workspaces.domain.model.aggregates.Workspace;
import pe.smartcash.cash.workspaces.domain.model.aggregates.WorkspaceRepository;
import pe.smartcash.cash.workspaces.domain.model.valueobjects.UserId;
import pe.smartcash.cash.workspaces.domain.model.valueobjects.WorkspaceId;
import pe.smartcash.cash.workspaces.infrastructure.persistence.jpa.repositories.WorkspaceCategoryJpaRepository;
import pe.smartcash.cash.workspaces.infrastructure.persistence.jpa.repositories.WorkspaceJpaRepository;

/**
 * Escribe/lee DOS tablas ({@code workspaces} + {@code workspace_categories}) en cada
 * operación -- el módulo y sus categorías son un único agregado. Las categorías archivadas
 * se conservan como filas (archived = true), nunca se borran: las transacciones de ese
 * módulo referencian su {@code code}. El caller ({@code WorkspaceCommandServiceImpl}) ya
 * corre dentro de un {@code @Transactional}, así que ambas escrituras son atómicas.
 */
@Repository
class WorkspaceRepositoryAdapter implements WorkspaceRepository {

  private final WorkspaceJpaRepository workspaceJpaRepository;
  private final WorkspaceCategoryJpaRepository categoryJpaRepository;

  WorkspaceRepositoryAdapter(
      WorkspaceJpaRepository workspaceJpaRepository, WorkspaceCategoryJpaRepository categoryJpaRepository) {
    this.workspaceJpaRepository = workspaceJpaRepository;
    this.categoryJpaRepository = categoryJpaRepository;
  }

  @Override
  public void save(Workspace workspace) {
    workspaceJpaRepository.save(WorkspaceEntityMapper.toJpaEntity(workspace));
    categoryJpaRepository.saveAll(WorkspaceEntityMapper.toCategoryJpaEntities(workspace));
  }

  @Override
  public Optional<Workspace> findById(WorkspaceId id) {
    return workspaceJpaRepository
        .findById(id.value())
        .map(entity -> WorkspaceEntityMapper.toDomain(entity, categoryJpaRepository.findAllByWorkspaceId(entity.getId())));
  }

  @Override
  public List<Workspace> findAllActiveByOwner(UserId ownerId) {
    var workspaceEntities =
        workspaceJpaRepository.findAllByOwnerIdAndArchivedAtIsNullOrderByIsDefaultDescCreatedAtAsc(ownerId.value());
    var ids = workspaceEntities.stream().map(WorkspaceJpaEntity::getId).toList();
    var categoriesByWorkspace =
        categoryJpaRepository.findAllByWorkspaceIdIn(ids).stream()
            .collect(java.util.stream.Collectors.groupingBy(WorkspaceCategoryJpaEntity::getWorkspaceId));
    return workspaceEntities.stream()
        .map(
            entity ->
                WorkspaceEntityMapper.toDomain(
                    entity, categoriesByWorkspace.getOrDefault(entity.getId(), List.of())))
        .toList();
  }

  @Override
  public Optional<Workspace> findDefaultByOwner(UserId ownerId) {
    return workspaceJpaRepository
        .findByOwnerIdAndIsDefaultTrue(ownerId.value())
        .map(entity -> WorkspaceEntityMapper.toDomain(entity, categoryJpaRepository.findAllByWorkspaceId(entity.getId())));
  }

  @Override
  public boolean existsDefaultForOwner(UserId ownerId) {
    return workspaceJpaRepository.existsByOwnerIdAndIsDefaultTrue(ownerId.value());
  }
}
