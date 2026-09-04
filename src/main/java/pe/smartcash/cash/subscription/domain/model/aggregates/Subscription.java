package pe.smartcash.cash.subscription.domain.model.aggregates;

import java.time.Instant;
import java.util.Objects;
import pe.smartcash.cash.subscription.domain.model.valueobjects.PlanCode;
import pe.smartcash.cash.subscription.domain.model.valueobjects.SubscriptionId;
import pe.smartcash.cash.subscription.domain.model.valueobjects.SubscriptionStatus;
import pe.smartcash.cash.subscription.domain.model.valueobjects.UserId;

/**
 * Aggregate root del bounded context Subscription. Su invariante: solo se puede cancelar
 * (o expirar, o renovar) una suscripción que esté ACTIVE. La regla "un usuario no puede
 * tener dos suscripciones ACTIVE a la vez" es responsabilidad del caso de uso (necesita
 * consultar el repositorio), no del agregado individual.
 */
public final class Subscription {

  private final SubscriptionId id;
  private final UserId userId;
  private final PlanCode planCode;
  private SubscriptionStatus status;
  private final Instant startedAt;
  private Instant renewsAt;
  private Instant canceledAt;
  private final String stripeSubscriptionId;

  private Subscription(
      SubscriptionId id,
      UserId userId,
      PlanCode planCode,
      SubscriptionStatus status,
      Instant startedAt,
      Instant renewsAt,
      Instant canceledAt,
      String stripeSubscriptionId) {
    this.id = Objects.requireNonNull(id, "id");
    this.userId = Objects.requireNonNull(userId, "userId");
    this.planCode = Objects.requireNonNull(planCode, "planCode");
    this.status = Objects.requireNonNull(status, "status");
    this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
    this.renewsAt = renewsAt;
    this.canceledAt = canceledAt;
    this.stripeSubscriptionId = stripeSubscriptionId;
  }

  /** Alta de un plan que no pasa por Stripe (hoy, solo FREE): no hay nada que cancelar del lado del proveedor de pagos. */
  public static Subscription subscribe(SubscriptionId id, UserId userId, PlanCode planCode, Instant now) {
    return subscribe(id, userId, planCode, now, null);
  }

  /**
   * Alta confirmada por un pago. {@code stripeSubscriptionId} es el id que Stripe asigna a la
   * suscripción creada por el Checkout (no el de la sesión) -- es lo que hace falta para poder
   * cancelarla después vía la API de Stripe, así que se guarda desde el momento de la
   * activación, no se reconstruye más tarde.
   */
  public static Subscription subscribe(SubscriptionId id, UserId userId, PlanCode planCode, Instant now, String stripeSubscriptionId) {
    Instant renewsAt = planCode.term() != null ? now.plus(planCode.term()) : null;
    return new Subscription(id, userId, planCode, SubscriptionStatus.ACTIVE, now, renewsAt, null, stripeSubscriptionId);
  }

  public static Subscription rehydrate(
      SubscriptionId id,
      UserId userId,
      PlanCode planCode,
      SubscriptionStatus status,
      Instant startedAt,
      Instant renewsAt,
      Instant canceledAt,
      String stripeSubscriptionId) {
    return new Subscription(id, userId, planCode, status, startedAt, renewsAt, canceledAt, stripeSubscriptionId);
  }

  /** Cancelación decidida por el usuario desde la app (propaga a Stripe -- ver {@code SubscriptionCommandServiceImpl}). */
  public void cancel(Instant now) {
    endAsTerminal(SubscriptionStatus.CANCELED, now);
  }

  /**
   * La suscripción terminó del lado de Stripe sin que el usuario la cancelara acá -- reintentos
   * de cobro agotados ({@code customer.subscription.deleted}). Distinto de {@link #cancel} para
   * poder diferenciar en el futuro "el usuario se fue" de "dejamos de poder cobrarle", aunque
   * hoy ambos casos liberan el cupo de "una suscripción ACTIVE por usuario" igual.
   */
  public void expire(Instant now) {
    endAsTerminal(SubscriptionStatus.EXPIRED, now);
  }

  private void endAsTerminal(SubscriptionStatus terminalStatus, Instant now) {
    if (status != SubscriptionStatus.ACTIVE) {
      throw new IllegalStateException(
          "No se puede pasar a " + terminalStatus + " una suscripción en estado " + status + " (se esperaba ACTIVE)");
    }
    this.status = terminalStatus;
    this.canceledAt = Objects.requireNonNull(now, "now");
  }

  /**
   * Renovación automática confirmada por Stripe ({@code invoice.paid}) al cobrar el ciclo
   * siguiente -- solo mueve {@code renewsAt} hacia adelante, no reactiva una suscripción que
   * ya haya terminado (eso lo cubre {@code SubscriptionCommandServiceImpl}, no el agregado).
   */
  public void renew(Instant now) {
    if (status != SubscriptionStatus.ACTIVE) {
      throw new IllegalStateException("No se puede renovar una suscripción en estado " + status + " (se esperaba ACTIVE)");
    }
    if (planCode.term() == null) {
      // No debería pasar nunca: solo PREMIUM (con term) pasa por Stripe y puede recibir
      // invoice.paid. Si llega, es un evento inconsistente -- mejor reventar que renovar mal.
      throw new IllegalStateException("El plan " + planCode + " no tiene vigencia que renovar");
    }
    this.renewsAt = Objects.requireNonNull(now, "now").plus(planCode.term());
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

  /** {@code null} para suscripciones que nunca pasaron por Stripe (FREE). */
  public String stripeSubscriptionId() {
    return stripeSubscriptionId;
  }
}
