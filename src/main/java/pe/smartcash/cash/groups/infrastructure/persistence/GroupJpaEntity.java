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
@Table(name = "groups")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupJpaEntity {

  @Id private UUID id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "owner_id", nullable = false)
  private UUID ownerId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
