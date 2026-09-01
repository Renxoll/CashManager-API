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
@Table(name = "group_memberships")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMembershipJpaEntity {

  @Id private UUID id;

  @Column(name = "group_id", nullable = false)
  private UUID groupId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "invited_at", nullable = false, updatable = false)
  private Instant invitedAt;

  @Column(name = "responded_at")
  private Instant respondedAt;
}
