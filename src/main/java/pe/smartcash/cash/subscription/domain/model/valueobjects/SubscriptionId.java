package pe.smartcash.cash.subscription.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

public record SubscriptionId(UUID value) {

  public SubscriptionId {
    Objects.requireNonNull(value, "value");
  }

  public static SubscriptionId newId() {
    return new SubscriptionId(UUID.randomUUID());
  }

  public static SubscriptionId of(UUID value) {
    return new SubscriptionId(value);
  }
}
