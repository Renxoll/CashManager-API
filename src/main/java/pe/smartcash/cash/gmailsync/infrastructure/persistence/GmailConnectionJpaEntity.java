package pe.smartcash.cash.gmailsync.infrastructure.persistence;

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

/** access_token/refresh_token viajan ya cifrados acá (ver TokenCipher en
 * GmailConnectionEntityMapper) -- esta clase nunca ve texto plano. */
@Entity
@Table(name = "gmail_connections")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GmailConnectionJpaEntity {

  @Id private UUID id;

  @Column(name = "user_id", nullable = false, unique = true)
  private UUID userId;

  @Column(name = "access_token", nullable = false, columnDefinition = "text")
  private String accessToken;

  @Column(name = "refresh_token", nullable = false, columnDefinition = "text")
  private String refreshToken;

  @Column(name = "access_token_expires_at", nullable = false)
  private Instant accessTokenExpiresAt;

  @Column(name = "last_synced_at")
  private Instant lastSyncedAt;

  @Column(name = "connected_at", nullable = false, updatable = false)
  private Instant connectedAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
