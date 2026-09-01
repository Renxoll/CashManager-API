package pe.smartcash.cash.groups.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "shared_expenses")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedExpenseJpaEntity {

  @Id private UUID id;

  @Column(name = "group_id", nullable = false)
  private UUID groupId;

  @Column(name = "description", nullable = false)
  private String description;

  @Column(name = "amount", nullable = false)
  private BigDecimal amount;

  @Column(name = "currency", nullable = false)
  private String currency;

  @Column(name = "paid_by_user_id", nullable = false)
  private UUID paidByUserId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
