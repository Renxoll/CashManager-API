package pe.smartcash.cash.workspaces.domain.exception;

import pe.smartcash.cash.workspaces.domain.model.valueobjects.WorkspaceId;

public class DuplicateWorkspaceCategoryException extends RuntimeException {

  public DuplicateWorkspaceCategoryException(WorkspaceId workspaceId, String code) {
    super("El módulo %s ya tiene una categoría con el código %s".formatted(workspaceId.value(), code));
  }
}
