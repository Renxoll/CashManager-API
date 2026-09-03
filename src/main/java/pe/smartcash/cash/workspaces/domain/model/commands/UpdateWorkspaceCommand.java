package pe.smartcash.cash.workspaces.domain.model.commands;

/** Renombrar y/o personalizar un módulo. Cualquiera de los tres campos puede venir null:
 * null = "no tocar ese atributo". */
public record UpdateWorkspaceCommand(String workspaceId, String ownerId, String name, String colorHex, String icon) {}
