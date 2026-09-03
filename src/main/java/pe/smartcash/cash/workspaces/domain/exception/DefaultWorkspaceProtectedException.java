package pe.smartcash.cash.workspaces.domain.exception;

import pe.smartcash.cash.workspaces.domain.model.valueobjects.WorkspaceId;

/** El módulo "General" es el destino por defecto de los gastos leídos automáticamente: no
 * se puede archivar (sí renombrar y personalizar). */
public class DefaultWorkspaceProtectedException extends RuntimeException {

  public DefaultWorkspaceProtectedException(WorkspaceId workspaceId) {
    super("El módulo General no se puede archivar: " + workspaceId.value());
  }
}
