package pe.smartcash.cash.workspaces.domain.services;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Read-model devuelto por {@link WorkspaceQueryService}: el módulo con sus categorías ya resueltas. */
public record WorkspaceDetail(
    UUID id,
    String name,
    String colorHex,
    String icon,
    boolean isDefault,
    Instant createdAt,
    List<WorkspaceCategoryDetail> categories) {}
