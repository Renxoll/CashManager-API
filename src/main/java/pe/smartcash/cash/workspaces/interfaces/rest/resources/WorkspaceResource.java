package pe.smartcash.cash.workspaces.interfaces.rest.resources;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkspaceResource(
    UUID id,
    String name,
    String colorHex,
    String icon,
    boolean isDefault,
    Instant createdAt,
    List<WorkspaceCategoryResource> categories) {}
