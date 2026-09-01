package pe.smartcash.cash.groups.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "expense_shares")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseShareJpaEntity {

  @Id private UUID id;

  @Column(name = "expense_id", nullable = false)
  private UUID expenseId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "amount", nullable = false)
  private BigDecimal amount;
}
