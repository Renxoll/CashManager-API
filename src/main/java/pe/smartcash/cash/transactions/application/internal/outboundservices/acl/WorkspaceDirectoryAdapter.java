package pe.smartcash.cash.transactions.application.internal.outboundservices.acl;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;
import pe.smartcash.cash.transactions.domain.services.WorkspaceCategoryView;
import pe.smartcash.cash.transactions.domain.services.WorkspaceDirectory;
import pe.smartcash.cash.workspaces.domain.model.queries.FindWorkspaceByIdQuery;
import pe.smartcash.cash.workspaces.domain.model.valueobjects.WorkspaceId;
import pe.smartcash.cash.workspaces.domain.services.WorkspaceCategoryDetail;
import pe.smartcash.cash.workspaces.domain.services.WorkspaceCommandService;
import pe.smartcash.cash.workspaces.domain.services.WorkspaceDetail;
import pe.smartcash.cash.workspaces.domain.services.WorkspaceQueryService;

/**
 * Anti-Corruption Layer hacia Workspaces: único punto donde Transactions le habla a ese
 * contexto, y solo por su API pública de dominio ({@link WorkspaceQueryService} /
 * {@link WorkspaceCommandService}), nunca importando {@code workspaces...aggregates.*}. El
 * {@code UserId} de Transactions se traduce al de Workspaces en el borde vía {@link #owner}.
 */
@Component
class WorkspaceDirectoryAdapter implements WorkspaceDirectory {

  private final WorkspaceQueryService workspaceQueryService;
  private final WorkspaceCommandService workspaceCommandService;

  WorkspaceDirectoryAdapter(
      WorkspaceQueryService workspaceQueryService, WorkspaceCommandService workspaceCommandService) {
    this.workspaceQueryService = workspaceQueryService;
    this.workspaceCommandService = workspaceCommandService;
  }

  @Override
  public UUID defaultWorkspaceId(UserId userId) {
    return workspaceQueryService
        .findDefault(owner(userId))
        .map(WorkspaceDetail::id)
        .orElseGet(
            () -> {
              // No debería pasar (el módulo General se crea en el alta), pero si falta se
              // aprovisiona al vuelo -- provisionDefaultFor es idempotente.
              workspaceCommandService.provisionDefaultFor(userId.value().toString());
              return workspaceQueryService
                  .findDefault(owner(userId))
                  .map(WorkspaceDetail::id)
                  .orElseThrow(
                      () -> new IllegalStateException("No se pudo aprovisionar el módulo General de " + userId.value()));
            });
  }

  @Override
  public boolean isDefaultWorkspace(UUID workspaceId, UserId owner) {
    return findOwned(workspaceId, owner).map(WorkspaceDetail::isDefault).orElse(false);
  }

  @Override
  public boolean isOwnedBy(UUID workspaceId, UserId owner) {
    return findOwned(workspaceId, owner).isPresent();
  }

  @Override
  public Optional<UUID> categoryId(UUID workspaceId, UserId owner, String categoryCode) {
    if (categoryCode == null || categoryCode.isBlank()) {
      return Optional.empty();
    }
    String normalized = categoryCode.trim().toUpperCase(Locale.ROOT);
    return findOwned(workspaceId, owner).stream()
        .flatMap(w -> w.categories().stream())
        .filter(c -> !c.archived() && c.code().equalsIgnoreCase(normalized))
        .map(WorkspaceCategoryDetail::id)
        .findFirst();
  }

  @Override
  public Map<UUID, WorkspaceCategoryView> describe(Collection<UUID> workspaceCategoryIds) {
    if (workspaceCategoryIds.isEmpty()) {
      return Map.of();
    }
    return workspaceQueryService.describeCategories(workspaceCategoryIds).stream()
        .collect(
            Collectors.toMap(
                WorkspaceCategoryDetail::id,
                c -> new WorkspaceCategoryView(c.code(), c.displayName(), c.icon()),
                (a, b) -> a));
  }

  private Optional<WorkspaceDetail> findOwned(UUID workspaceId, UserId owner) {
    return workspaceQueryService.handle(new FindWorkspaceByIdQuery(WorkspaceId.of(workspaceId), owner(owner)));
  }

  private static pe.smartcash.cash.workspaces.domain.model.valueobjects.UserId owner(UserId userId) {
    return pe.smartcash.cash.workspaces.domain.model.valueobjects.UserId.of(userId.value());
  }
}
