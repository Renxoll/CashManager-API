package pe.smartcash.cash.workspaces.domain.model.commands;

public record CreateWorkspaceCommand(String ownerId, String name, String colorHex, String icon) {}
