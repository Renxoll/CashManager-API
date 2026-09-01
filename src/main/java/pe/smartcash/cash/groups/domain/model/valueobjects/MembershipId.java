package pe.smartcash.cash.groups.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

public record MembershipId(UUID value) {

  public MembershipId {
    Objects.requireNonNull(value, "value");
  }

  public static MembershipId newId() {
    return new MembershipId(UUID.randomUUID());
  }

  public static MembershipId of(UUID value) {
    return new MembershipId(value);
  }
}
