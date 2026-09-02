package pe.smartcash.cash.workspaces.domain.model.commands;

public record ArchiveWorkspaceCategoryCommand(String workspaceId, String categoryId, String ownerId) {}
