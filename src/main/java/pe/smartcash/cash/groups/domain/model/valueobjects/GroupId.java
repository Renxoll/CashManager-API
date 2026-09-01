package pe.smartcash.cash.groups.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

public record GroupId(UUID value) {

  public GroupId {
    Objects.requireNonNull(value, "value");
  }

  public static GroupId newId() {
    return new GroupId(UUID.randomUUID());
  }

  public static GroupId of(UUID value) {
    return new GroupId(value);
  }
}
