package pe.smartcash.cash.workspaces.infrastructure.persistence;

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
@Table(name = "workspaces")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceJpaEntity {

  @Id private UUID id;

  @Column(name = "owner_id", nullable = false)
  private UUID ownerId;

  @Column(nullable = false, length = 60)
  private String name;

  @Column(name = "color_hex", nullable = false, length = 7)
  private String colorHex;

  @Column(nullable = false, length = 40)
  private String icon;

  @Column(name = "is_default", nullable = false)
  private boolean isDefault;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "archived_at")
  private Instant archivedAt;
}
