package pe.smartcash.cash.workspaces.domain.model.commands;

public record UpdateWorkspaceCategoryCommand(
    String workspaceId, String categoryId, String ownerId, String displayName, String icon) {}
