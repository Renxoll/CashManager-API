package pe.smartcash.cash.transactions.domain.exception;

import java.util.UUID;

/** El módulo destino de un "mover transacción" no existe o no es del usuario (mismo 404 en
 * ambos casos, sin distinguir). */
public class WorkspaceNotAccessibleException extends RuntimeException {

  public WorkspaceNotAccessibleException(UUID workspaceId) {
    super("Módulo no encontrado: " + workspaceId);
  }
}
