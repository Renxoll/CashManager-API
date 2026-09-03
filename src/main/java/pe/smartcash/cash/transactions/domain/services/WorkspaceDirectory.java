package pe.smartcash.cash.transactions.domain.services;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;

/**
 * Puerto hacia el bounded context Workspaces -- lo implementa un ACL en {@code
 * application.internal.outboundservices.acl}. Transactions nunca importa clases de {@code
 * workspaces.*} directamente; solo conoce este contrato.
 */
public interface WorkspaceDirectory {

  /**
   * Id del módulo "General" del usuario -- destino por defecto de la ingesta automática. Se
   * aprovisiona en el alta de cuenta; si por algún motivo no existiera, el ACL lo crea al
   * vuelo (idempotente).
   */
  UUID defaultWorkspaceId(UserId userId);

  /** {@code true} si el módulo existe y es el "General" del usuario. */
  boolean isDefaultWorkspace(UUID workspaceId, UserId owner);

  /** {@code true} si el módulo existe y pertenece al usuario. */
  boolean isOwnedBy(UUID workspaceId, UserId owner);

  /**
   * Id de la categoría activa cuyo {@code code} coincide (case-insensitive), dentro de un
   * módulo del usuario. Vacío si el módulo no es suyo o el code no existe/está archivado.
   */
  Optional<UUID> categoryId(UUID workspaceId, UserId owner, String categoryCode);

  /** {@code workspaceCategoryId -> vista} para las categorías pedidas (las que no existen se omiten). */
  Map<UUID, WorkspaceCategoryView> describe(Collection<UUID> workspaceCategoryIds);
}
