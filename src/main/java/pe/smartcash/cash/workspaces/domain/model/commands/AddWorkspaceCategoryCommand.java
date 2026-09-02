package pe.smartcash.cash.workspaces.domain.model.commands;

public record AddWorkspaceCategoryCommand(String workspaceId, String ownerId, String displayName, String icon) {}
