package pe.smartcash.cash.gmailsync.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/**
 * Referencia al dueño de la conexión, vista desde este bounded context. Deliberadamente NO
 * es el mismo tipo que {@code iam.domain.model.UserId} u otros -- cada contexto modela su
 * propia noción de "usuario" y solo se traducen en el borde (ver infrastructure.acl).
 */
public record UserId(UUID value) {

  public UserId {
    Objects.requireNonNull(value, "value");
  }

  public static UserId of(UUID value) {
    return new UserId(value);
  }
}
