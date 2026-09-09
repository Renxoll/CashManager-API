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
@Table(name = "group_deletion_requests")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupDeletionRequestJpaEntity {

  @Id private UUID id;

  @Column(name = "group_id", nullable = false)
  private UUID groupId;

  @Column(name = "requested_by", nullable = false)
  private UUID requestedBy;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "requested_at", nullable = false, updatable = false)
  private Instant requestedAt;

  @Column(name = "resolved_at")
  private Instant resolvedAt;
}
