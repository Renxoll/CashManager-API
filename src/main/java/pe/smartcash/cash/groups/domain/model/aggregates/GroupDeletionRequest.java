package pe.smartcash.cash.groups.domain.model.aggregates;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import pe.smartcash.cash.groups.domain.model.valueobjects.DeletionRequestStatus;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupDeletionRequestId;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

/**
 * La propuesta de borrar un grupo, que solo se ejecuta cuando TODOS los miembros aceptados
 * están de acuerdo. Sigue el mismo patrón de estado explícito que {@link GroupMembership}
 * (PENDING hasta que se resuelve, sin volver atrás una vez resuelto): {@link #approve},
 * {@link #cancel} y {@link #markCompleted} exigen {@code PENDING}.
 *
 * <p>El que la abre ya cuenta como que aprobó (ver {@link #open}). Cuando
 * {@link #isApprovedByAll} da true, el command service borra el grupo en cascada y marca
 * esta solicitud {@code COMPLETED}.
 */
public final class GroupDeletionRequest {

  private final GroupDeletionRequestId id;
  private final GroupId groupId;
  private final UserId requestedBy;
  private final Instant requestedAt;
  private final Set<UserId> approvals;

  private DeletionRequestStatus status;
  private Instant resolvedAt;

  private GroupDeletionRequest(
      GroupDeletionRequestId id,
      GroupId groupId,
      UserId requestedBy,
      Instant requestedAt,
      Set<UserId> approvals,
      DeletionRequestStatus status,
      Instant resolvedAt) {
    this.id = Objects.requireNonNull(id, "id");
    this.groupId = Objects.requireNonNull(groupId, "groupId");
    this.requestedBy = Objects.requireNonNull(requestedBy, "requestedBy");
    this.requestedAt = Objects.requireNonNull(requestedAt, "requestedAt");
    this.approvals = new LinkedHashSet<>(Objects.requireNonNull(approvals, "approvals"));
    this.status = Objects.requireNonNull(status, "status");
    this.resolvedAt = resolvedAt;
  }

  /** Nueva propuesta: el que la abre queda como primer aprobador. */
  public static GroupDeletionRequest open(
      GroupDeletionRequestId id, GroupId groupId, UserId requestedBy, Instant now) {
    Set<UserId> approvals = new LinkedHashSet<>();
    approvals.add(Objects.requireNonNull(requestedBy, "requestedBy"));
    return new GroupDeletionRequest(id, groupId, requestedBy, now, approvals, DeletionRequestStatus.PENDING, null);
  }

  /** Reconstrucción desde persistencia. */
  public static GroupDeletionRequest rehydrate(
      GroupDeletionRequestId id,
      GroupId groupId,
      UserId requestedBy,
      Instant requestedAt,
      Set<UserId> approvals,
      DeletionRequestStatus status,
      Instant resolvedAt) {
    return new GroupDeletionRequest(id, groupId, requestedBy, requestedAt, approvals, status, resolvedAt);
  }

  public void approve(UserId userId, Instant now) {
    requirePending("aprobar");
    Objects.requireNonNull(now, "now");
    approvals.add(Objects.requireNonNull(userId, "userId"));
  }

  public void cancel(Instant now) {
    requirePending("cancelar");
    this.status = DeletionRequestStatus.CANCELLED;
    this.resolvedAt = Objects.requireNonNull(now, "now");
  }

  public void markCompleted(Instant now) {
    requirePending("completar");
    this.status = DeletionRequestStatus.COMPLETED;
    this.resolvedAt = Objects.requireNonNull(now, "now");
  }

  /** true si todos los ids dados ya aprobaron -- el caller pasa los miembros ACCEPTED del grupo. */
  public boolean isApprovedByAll(Set<UserId> acceptedMemberIds) {
    return !acceptedMemberIds.isEmpty() && approvals.containsAll(acceptedMemberIds);
  }

  private void requirePending(String action) {
    if (this.status != DeletionRequestStatus.PENDING) {
      throw new IllegalStateException(
          "No se puede %s una solicitud de borrado en estado %s".formatted(action, status));
    }
  }

  public GroupDeletionRequestId id() {
    return id;
  }

  public GroupId groupId() {
    return groupId;
  }

  public UserId requestedBy() {
    return requestedBy;
  }

  public Instant requestedAt() {
    return requestedAt;
  }

  public Set<UserId> approvedBy() {
    return Set.copyOf(approvals);
  }

  public DeletionRequestStatus status() {
    return status;
  }

  public Instant resolvedAt() {
    return resolvedAt;
  }
}
