package pe.smartcash.cash.transactions.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

public record PendingSenderId(UUID value) {

  public PendingSenderId {
    Objects.requireNonNull(value, "value");
  }

  public static PendingSenderId newId() {
    return new PendingSenderId(UUID.randomUUID());
  }

  public static PendingSenderId of(UUID value) {
    return new PendingSenderId(value);
  }
}
