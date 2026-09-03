package pe.smartcash.cash.workspaces.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Tabla hija de {@code workspaces}. El {@code workspace_id} se guarda como UUID plano (no una
 * relación JPA): el agregado {@link pe.smartcash.cash.workspaces.domain.model.aggregates.Workspace}
 * ya gestiona la colección como un todo, y el adaptador la carga/guarda con dos consultas
 * explícitas -- mismo criterio que {@code SharedExpenseRepositoryAdapter}.
 */
@Entity
@Table(name = "workspace_categories")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceCategoryJpaEntity {

  @Id private UUID id;

  @Column(name = "workspace_id", nullable = false)
  private UUID workspaceId;

  @Column(nullable = false, length = 40)
  private String code;

  @Column(name = "display_name", nullable = false, length = 60)
  private String displayName;

  @Column(length = 40)
  private String icon;

  @Column(nullable = false)
  private int position;

  @Column(nullable = false)
  private boolean archived;
}
