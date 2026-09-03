package pe.smartcash.cash.workspaces.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

public record WorkspaceId(UUID value) {

  public WorkspaceId {
    Objects.requireNonNull(value, "value");
  }

  public static WorkspaceId newId() {
    return new WorkspaceId(UUID.randomUUID());
  }

  public static WorkspaceId of(UUID value) {
    return new WorkspaceId(value);
  }

  public static WorkspaceId parse(String raw) {
    try {
      return new WorkspaceId(UUID.fromString(raw));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("workspaceId inválido: " + raw, e);
    }
  }
}
