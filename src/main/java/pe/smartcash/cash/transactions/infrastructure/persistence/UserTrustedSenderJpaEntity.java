package pe.smartcash.cash.transactions.infrastructure.persistence;

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
@Table(name = "user_trusted_senders")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTrustedSenderJpaEntity {

  @Id private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(nullable = false, length = 255)
  private String domain;

  @Column(name = "trusted_at", nullable = false)
  private Instant trustedAt;
}
