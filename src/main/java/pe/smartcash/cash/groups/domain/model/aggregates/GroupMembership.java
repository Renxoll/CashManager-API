package pe.smartcash.cash.groups.domain.model.aggregates;

import java.time.Instant;
import java.util.Objects;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.domain.model.valueobjects.MembershipId;
import pe.smartcash.cash.groups.domain.model.valueobjects.MembershipStatus;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

/**
 * La relación de un usuario con un grupo -- calco exacto del patrón de {@code PendingSender}
 * (PENDING hasta que el invitado decide, con {@code accept}/{@code decline} guardados por
 * estado). Igual que ahí, una vez decidido no se puede volver a decidir: {@link #accept} y
 * {@link #decline} exigen {@code PENDING}.
 */
public final class GroupMembership {

  private final MembershipId id;
  private final GroupId groupId;
  private final UserId userId;
  private final Instant invitedAt;

  private MembershipStatus status;
  private Instant respondedAt;

  private GroupMembership(
      MembershipId id, GroupId groupId, UserId userId, MembershipStatus status, Instant invitedAt, Instant respondedAt) {
    this.id = Objects.requireNonNull(id, "id");
    this.groupId = Objects.requireNonNull(groupId, "groupId");
    this.userId = Objects.requireNonNull(userId, "userId");
    this.status = Objects.requireNonNull(status, "status");
    this.invitedAt = Objects.requireNonNull(invitedAt, "invitedAt");
    this.respondedAt = respondedAt;
  }

  /** Invitación nueva, queda PENDING hasta que el invitado decide. */
  public static GroupMembership invite(MembershipId id, GroupId groupId, UserId userId, Instant invitedAt) {
    return new GroupMembership(id, groupId, userId, MembershipStatus.PENDING, invitedAt, null);
  }

  /** La membresía del creador del grupo: nace ACCEPTED de una, nadie se invita a sí mismo. */
  public static GroupMembership ownerMembership(MembershipId id, GroupId groupId, UserId ownerId, Instant now) {
    return new GroupMembership(id, groupId, ownerId, MembershipStatus.ACCEPTED, now, now);
  }

  /** Reconstrucción desde persistencia: restaura estado sin re-aplicar invariantes de creación. */
  public static GroupMembership rehydrate(
      MembershipId id, GroupId groupId, UserId userId, MembershipStatus status, Instant invitedAt, Instant respondedAt) {
    return new GroupMembership(id, groupId, userId, status, invitedAt, respondedAt);
  }

  public void accept(Instant respondedAt) {
    requireStatus(MembershipStatus.PENDING, "aceptar");
    this.status = MembershipStatus.ACCEPTED;
    this.respondedAt = Objects.requireNonNull(respondedAt, "respondedAt");
  }

  public void decline(Instant respondedAt) {
    requireStatus(MembershipStatus.PENDING, "rechazar");
    this.status = MembershipStatus.DECLINED;
    this.respondedAt = Objects.requireNonNull(respondedAt, "respondedAt");
  }

  private void requireStatus(MembershipStatus expected, String action) {
    if (this.status != expected) {
      throw new IllegalStateException(
          "No se puede %s una invitación en estado %s (se esperaba %s)".formatted(action, status, expected));
    }
  }

  public boolean isAccepted() {
    return status == MembershipStatus.ACCEPTED;
  }

  public MembershipId id() {
    return id;
  }

  public GroupId groupId() {
    return groupId;
  }

  public UserId userId() {
    return userId;
  }

  public MembershipStatus status() {
    return status;
  }

  public Instant invitedAt() {
    return invitedAt;
  }

  public Instant respondedAt() {
    return respondedAt;
  }
}
