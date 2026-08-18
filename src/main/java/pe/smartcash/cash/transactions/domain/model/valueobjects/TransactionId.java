package pe.smartcash.cash.transactions.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

public record TransactionId(UUID value) {

  public TransactionId {
    Objects.requireNonNull(value, "value");
  }

  public static TransactionId newId() {
    return new TransactionId(UUID.randomUUID());
  }

  public static TransactionId of(UUID value) {
    return new TransactionId(value);
  }
}
