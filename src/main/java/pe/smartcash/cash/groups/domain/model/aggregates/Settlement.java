package pe.smartcash.cash.groups.domain.model.aggregates;

import java.time.Instant;
import java.util.Objects;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.domain.model.valueobjects.Money;
import pe.smartcash.cash.groups.domain.model.valueobjects.SettlementId;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

/** El registro de que un miembro le pagó a otro dentro de un grupo -- reduce el saldo entre
 * ambos, ver {@code GroupBalanceReadRepository}. */
public final class Settlement {

  private final SettlementId id;
  private final GroupId groupId;
  private final UserId fromUserId;
  private final UserId toUserId;
  private final Money amount;
  private final Instant createdAt;

  private Settlement(SettlementId id, GroupId groupId, UserId fromUserId, UserId toUserId, Money amount, Instant createdAt) {
    this.id = Objects.requireNonNull(id, "id");
    this.groupId = Objects.requireNonNull(groupId, "groupId");
    this.fromUserId = Objects.requireNonNull(fromUserId, "fromUserId");
    this.toUserId = Objects.requireNonNull(toUserId, "toUserId");
    if (fromUserId.equals(toUserId)) {
      throw new IllegalArgumentException("No se puede registrar un pago de un usuario a sí mismo");
    }
    this.amount = Objects.requireNonNull(amount, "amount");
    if (amount.amount().signum() <= 0) {
      throw new IllegalArgumentException("El monto del pago debe ser positivo");
    }
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
  }

  public static Settlement record(SettlementId id, GroupId groupId, UserId fromUserId, UserId toUserId, Money amount, Instant createdAt) {
    return new Settlement(id, groupId, fromUserId, toUserId, amount, createdAt);
  }

  public static Settlement rehydrate(
      SettlementId id, GroupId groupId, UserId fromUserId, UserId toUserId, Money amount, Instant createdAt) {
    return new Settlement(id, groupId, fromUserId, toUserId, amount, createdAt);
  }

  public SettlementId id() {
    return id;
  }

  public GroupId groupId() {
    return groupId;
  }

  public UserId fromUserId() {
    return fromUserId;
  }

  public UserId toUserId() {
    return toUserId;
  }

  public Money amount() {
    return amount;
  }

  public Instant createdAt() {
    return createdAt;
  }
}
