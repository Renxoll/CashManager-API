package pe.smartcash.cash.transactions.infrastructure.persistence;

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
import pe.smartcash.cash.transactions.domain.model.valueobjects.PendingSenderStatus;

@Entity
@Table(name = "pending_senders")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingSenderJpaEntity {

  @Id private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "from_address", nullable = false, length = 255)
  private String fromAddress;

  @Column(nullable = false, length = 255)
  private String domain;

  @Column(name = "sample_snippet", length = 500)
  private String sampleSnippet;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PendingSenderStatus status;

  @Column(name = "occurrence_count", nullable = false)
  private int occurrenceCount;

  @Column(name = "first_seen_at", nullable = false)
  private Instant firstSeenAt;

  @Column(name = "last_seen_at", nullable = false)
  private Instant lastSeenAt;

  @Column(name = "decided_at")
  private Instant decidedAt;
}
