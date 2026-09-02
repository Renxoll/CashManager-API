package pe.smartcash.cash.transactions.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/**
 * Referencia a una categoría propia de un módulo custom (tabla {@code workspace_categories}).
 * Solo se usa cuando la transacción NO está en el módulo General: ahí la categoría la da
 * {@link CategoryCode} vía el catálogo cerrado, no este id.
 */
public record WorkspaceCategoryId(UUID value) {

  public WorkspaceCategoryId {
    Objects.requireNonNull(value, "value");
  }

  public static WorkspaceCategoryId of(UUID value) {
    return new WorkspaceCategoryId(value);
  }
}
