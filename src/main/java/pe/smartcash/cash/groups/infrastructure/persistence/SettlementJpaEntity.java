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
@Table(name = "settlements")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementJpaEntity {

  @Id private UUID id;

  @Column(name = "group_id", nullable = false)
  private UUID groupId;

  @Column(name = "from_user_id", nullable = false)
  private UUID fromUserId;

  @Column(name = "to_user_id", nullable = false)
  private UUID toUserId;

  @Column(name = "amount", nullable = false)
  private BigDecimal amount;

  @Column(name = "currency", nullable = false)
  private String currency;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
