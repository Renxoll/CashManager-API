package pe.smartcash.cash.iam.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** Identidad de una emisión concreta de token de restablecimiento (no es el token en sí). */
public record PasswordResetTokenId(UUID value) {

  public PasswordResetTokenId {
    Objects.requireNonNull(value, "value");
  }

  public static PasswordResetTokenId newId() {
    return new PasswordResetTokenId(UUID.randomUUID());
  }

  public static PasswordResetTokenId of(UUID value) {
    return new PasswordResetTokenId(value);
  }
}
