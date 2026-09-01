package pe.smartcash.cash.groups.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

public record SettlementId(UUID value) {

  public SettlementId {
    Objects.requireNonNull(value, "value");
  }

  public static SettlementId newId() {
    return new SettlementId(UUID.randomUUID());
  }

  public static SettlementId of(UUID value) {
    return new SettlementId(value);
  }
}
