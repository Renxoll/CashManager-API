package pe.smartcash.cash.subscription.domain.model.aggregates;

import java.time.Instant;
import java.util.Objects;
import pe.smartcash.cash.subscription.domain.model.valueobjects.PlanCode;
import pe.smartcash.cash.subscription.domain.model.valueobjects.SubscriptionId;
import pe.smartcash.cash.subscription.domain.model.valueobjects.SubscriptionStatus;
import pe.smartcash.cash.subscription.domain.model.valueobjects.UserId;

/**
 * Aggregate root del bounded context Subscription. Su invariante: solo se puede cancelar
 * una suscripción que esté ACTIVE (no tiene sentido cancelar una ya cancelada o expirada).
 * La regla "un usuario no puede tener dos suscripciones ACTIVE a la vez" es responsabilidad
 * del caso de uso (necesita consultar el repositorio), no del agregado individual.
 */
public final class Subscription {

  private final SubscriptionId id;
  private final UserId userId;
  private final PlanCode planCode;
  private SubscriptionStatus status;
  private final Instant startedAt;
  private final Instant renewsAt;
  private Instant canceledAt;

  private Subscription(
      SubscriptionId id,
      UserId userId,
      PlanCode planCode,
      SubscriptionStatus status,
      Instant startedAt,
      Instant renewsAt,
      Instant canceledAt) {
    this.id = Objects.requireNonNull(id, "id");
    this.userId = Objects.requireNonNull(userId, "userId");
    this.planCode = Objects.requireNonNull(planCode, "planCode");
    this.status = Objects.requireNonNull(status, "status");
    this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
    this.renewsAt = renewsAt;
    this.canceledAt = canceledAt;
  }

  public static Subscription subscribe(SubscriptionId id, UserId userId, PlanCode planCode, Instant now) {
    Instant renewsAt = planCode.term() != null ? now.plus(planCode.term()) : null;
    return new Subscription(id, userId, planCode, SubscriptionStatus.ACTIVE, now, renewsAt, null);
  }

  public static Subscription rehydrate(
      SubscriptionId id,
      UserId userId,
      PlanCode planCode,
      SubscriptionStatus status,
      Instant startedAt,
      Instant renewsAt,
      Instant canceledAt) {
    return new Subscription(id, userId, planCode, status, startedAt, renewsAt, canceledAt);
  }

  public void cancel(Instant now) {
    if (status != SubscriptionStatus.ACTIVE) {
      throw new IllegalStateException("No se puede cancelar una suscripción en estado " + status + " (se esperaba ACTIVE)");
    }
    this.status = SubscriptionStatus.CANCELED;
    this.canceledAt = Objects.requireNonNull(now, "now");
  }

  public SubscriptionId id() {
    return id;
  }

  public UserId userId() {
    return userId;
  }

  public PlanCode planCode() {
    return planCode;
  }

  public SubscriptionStatus status() {
    return status;
  }

  public Instant startedAt() {
    return startedAt;
  }

  public Instant renewsAt() {
    return renewsAt;
  }

  public Instant canceledAt() {
    return canceledAt;
  }
}
