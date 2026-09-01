package pe.smartcash.cash.groups.domain.model.aggregates;

import java.time.Instant;
import java.util.Objects;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

/**
 * Aggregate root del bounded context Groups: un grupo de gasto compartido (viaje,
 * departamento, etc.). Deliberadamente "plano" -- no carga la lista de miembros en memoria,
 * mismo criterio que el resto de agregados de este proyecto (ver {@code Transaction},
 * {@code GmailConnection}): la membresía vive en su propio agregado ({@link
 * GroupMembership}), y las lecturas que necesitan el join (lista de miembros con sus
 * saldos) pasan por un read model, no por este agregado.
 */
public final class Group {

  private final GroupId id;
  private final String name;
  private final UserId ownerId;
  private final Instant createdAt;

  private Group(GroupId id, String name, UserId ownerId, Instant createdAt) {
    this.id = Objects.requireNonNull(id, "id");
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name no puede estar vacío");
    }
    this.name = name;
    this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
  }

  public static Group create(GroupId id, String name, UserId ownerId, Instant createdAt) {
    return new Group(id, name, ownerId, createdAt);
  }

  public static Group rehydrate(GroupId id, String name, UserId ownerId, Instant createdAt) {
    return new Group(id, name, ownerId, createdAt);
  }

  public GroupId id() {
    return id;
  }

  public String name() {
    return name;
  }

  public UserId ownerId() {
    return ownerId;
  }

  public Instant createdAt() {
    return createdAt;
  }
}
