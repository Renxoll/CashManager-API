package pe.smartcash.cash.workspaces.infrastructure.persistence;

import java.util.List;
import pe.smartcash.cash.workspaces.domain.model.aggregates.Workspace;
import pe.smartcash.cash.workspaces.domain.model.aggregates.WorkspaceCategory;
import pe.smartcash.cash.workspaces.domain.model.valueobjects.UserId;
import pe.smartcash.cash.workspaces.domain.model.valueobjects.WorkspaceCategoryId;
import pe.smartcash.cash.workspaces.domain.model.valueobjects.WorkspaceId;

final class WorkspaceEntityMapper {

  private WorkspaceEntityMapper() {}

  static WorkspaceJpaEntity toJpaEntity(Workspace workspace) {
    return WorkspaceJpaEntity.builder()
        .id(workspace.id().value())
        .ownerId(workspace.ownerId().value())
        .name(workspace.name())
        .colorHex(workspace.colorHex())
        .icon(workspace.icon())
        .isDefault(workspace.isDefault())
        .createdAt(workspace.createdAt())
        .archivedAt(workspace.archivedAt())
        .build();
  }

  static List<WorkspaceCategoryJpaEntity> toCategoryJpaEntities(Workspace workspace) {
    return workspace.categories().stream()
        .map(
            c ->
                WorkspaceCategoryJpaEntity.builder()
                    .id(c.id().value())
                    .workspaceId(workspace.id().value())
                    .code(c.code())
                    .displayName(c.displayName())
                    .icon(c.icon())
                    .position(c.position())
                    .archived(c.archived())
                    .build())
        .toList();
  }

  static Workspace toDomain(WorkspaceJpaEntity entity, List<WorkspaceCategoryJpaEntity> categoryEntities) {
    List<WorkspaceCategory> categories =
        categoryEntities.stream()
            .map(
                c ->
                    WorkspaceCategory.rehydrate(
                        WorkspaceCategoryId.of(c.getId()),
                        c.getCode(),
                        c.getDisplayName(),
                        c.getIcon(),
                        c.getPosition(),
                        c.isArchived()))
            .toList();
    return Workspace.rehydrate(
        WorkspaceId.of(entity.getId()),
        UserId.of(entity.getOwnerId()),
        entity.getName(),
        entity.getColorHex(),
        entity.getIcon(),
        entity.isDefault(),
        entity.getCreatedAt(),
        entity.getArchivedAt(),
        categories);
  }
}
