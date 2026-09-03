package pe.smartcash.cash.transactions.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/**
 * Referencia al módulo ("workspace") en el que vive la transacción, vista desde este
 * bounded context. Deliberadamente NO es el mismo tipo que
 * {@code workspaces.domain.model.valueobjects.WorkspaceId}: la traducción entre contextos
 * ocurre en el ACL ({@code WorkspaceDirectory}), no compartiendo la clase.
 */
public record WorkspaceId(UUID value) {

  public WorkspaceId {
    Objects.requireNonNull(value, "value");
  }

  public static WorkspaceId of(UUID value) {
    return new WorkspaceId(value);
  }
}
