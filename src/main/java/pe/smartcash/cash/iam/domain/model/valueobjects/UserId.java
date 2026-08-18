package pe.smartcash.cash.iam.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** La identidad canónica: IAM es quien la acuña al registrar credenciales nuevas. */
public record UserId(UUID value) {

  public UserId {
    Objects.requireNonNull(value, "value");
  }

  public static UserId newId() {
    return new UserId(UUID.randomUUID());
  }

  public static UserId of(UUID value) {
    return new UserId(value);
  }
}
