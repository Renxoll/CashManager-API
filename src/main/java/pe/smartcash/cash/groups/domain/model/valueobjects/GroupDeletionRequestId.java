package pe.smartcash.cash.groups.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

public record GroupDeletionRequestId(UUID value) {

  public GroupDeletionRequestId {
    Objects.requireNonNull(value, "value");
  }

  public static GroupDeletionRequestId newId() {
    return new GroupDeletionRequestId(UUID.randomUUID());
  }

  public static GroupDeletionRequestId of(UUID value) {
    return new GroupDeletionRequestId(value);
  }
}
