package pe.smartcash.cash.groups.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "group_deletion_approvals")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupDeletionApprovalJpaEntity {

  @Id private UUID id;

  @Column(name = "request_id", nullable = false)
  private UUID requestId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "approved_at", nullable = false)
  private Instant approvedAt;
}
