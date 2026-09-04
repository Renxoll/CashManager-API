package pe.smartcash.cash.subscription.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pe.smartcash.cash.subscription.domain.model.valueobjects.PlanCode;
import pe.smartcash.cash.subscription.domain.model.valueobjects.SubscriptionStatus;

@Entity
@Table(name = "subscriptions")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionJpaEntity {

  @Id private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "plan_code", nullable = false, length = 20)
  private PlanCode planCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SubscriptionStatus status;

  @Column(name = "started_at", nullable = false, updatable = false)
  private Instant startedAt;

  @Column(name = "renews_at")
  private Instant renewsAt;

  @Column(name = "canceled_at")
  private Instant canceledAt;

  // NULL para FREE (nunca pasa por Stripe) y para suscripciones activadas antes de que este
  // campo existiera. Es el id de la Subscription de Stripe (no el de la Session de Checkout)
  // -- lo necesario para poder cancelarla del lado del proveedor de pagos.
  @Column(name = "stripe_subscription_id")
  private String stripeSubscriptionId;
}
