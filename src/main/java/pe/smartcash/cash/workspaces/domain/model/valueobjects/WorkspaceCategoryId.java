package pe.smartcash.cash.workspaces.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

public record WorkspaceCategoryId(UUID value) {

  public WorkspaceCategoryId {
    Objects.requireNonNull(value, "value");
  }

  public static WorkspaceCategoryId newId() {
    return new WorkspaceCategoryId(UUID.randomUUID());
  }

  public static WorkspaceCategoryId of(UUID value) {
    return new WorkspaceCategoryId(value);
  }

  public static WorkspaceCategoryId parse(String raw) {
    try {
      return new WorkspaceCategoryId(UUID.fromString(raw));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("workspaceCategoryId inválido: " + raw, e);
    }
  }
}
