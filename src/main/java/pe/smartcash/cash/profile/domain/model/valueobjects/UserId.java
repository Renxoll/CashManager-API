package pe.smartcash.cash.profile.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) {

  public UserId {
    Objects.requireNonNull(value, "value");
  }

  public static UserId of(UUID value) {
    return new UserId(value);
  }
}
