package pe.smartcash.cash.workspaces.domain.exception;

import pe.smartcash.cash.workspaces.domain.model.valueobjects.WorkspaceId;

/** Un módulo siempre necesita al menos una categoría activa para poder clasificar sus
 * gastos: no se puede archivar la última. */
public class LastActiveWorkspaceCategoryException extends RuntimeException {

  public LastActiveWorkspaceCategoryException(WorkspaceId workspaceId) {
    super("El módulo %s no puede quedarse sin categorías activas".formatted(workspaceId.value()));
  }
}
