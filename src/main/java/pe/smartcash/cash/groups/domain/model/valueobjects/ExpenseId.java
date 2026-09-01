package pe.smartcash.cash.groups.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

public record ExpenseId(UUID value) {

  public ExpenseId {
    Objects.requireNonNull(value, "value");
  }

  public static ExpenseId newId() {
    return new ExpenseId(UUID.randomUUID());
  }

  public static ExpenseId of(UUID value) {
    return new ExpenseId(value);
  }
}
