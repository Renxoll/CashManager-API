package pe.smartcash.cash.workspaces.interfaces.rest;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.smartcash.cash.workspaces.domain.exception.WorkspaceNotFoundException;
import pe.smartcash.cash.workspaces.domain.model.queries.FindWorkspaceByIdQuery;
import pe.smartcash.cash.workspaces.domain.model.queries.FindWorkspacesByOwnerQuery;
import pe.smartcash.cash.workspaces.domain.model.valueobjects.UserId;
import pe.smartcash.cash.workspaces.domain.model.valueobjects.WorkspaceId;
import pe.smartcash.cash.workspaces.domain.services.WorkspaceCommandService;
import pe.smartcash.cash.workspaces.domain.services.WorkspaceQueryService;
import pe.smartcash.cash.workspaces.interfaces.rest.resources.CreateWorkspaceResource;
import pe.smartcash.cash.workspaces.interfaces.rest.resources.UpdateWorkspaceResource;
import pe.smartcash.cash.workspaces.interfaces.rest.resources.WorkspaceCategoryPayload;
import pe.smartcash.cash.workspaces.interfaces.rest.resources.WorkspaceResource;
import pe.smartcash.cash.workspaces.interfaces.rest.transform.WorkspaceCommandFromResourceAssembler;
import pe.smartcash.cash.workspaces.interfaces.rest.transform.WorkspaceResourceFromEntityAssembler;

/**
 * El {@code userId} sale del principal ya autenticado por {@code BearerTokenAuthenticationFilter}
 * de IAM (mismo criterio que el resto de controllers): nunca de un path/query param, así un
 * cliente no puede leer ni tocar los módulos de otro usuario.
 */
@RestController
@RequestMapping("/api/v1/workspaces")
class WorkspaceController {

  private final WorkspaceCommandService commandService;
  private final WorkspaceQueryService queryService;

  WorkspaceController(WorkspaceCommandService commandService, WorkspaceQueryService queryService) {
    this.commandService = commandService;
    this.queryService = queryService;
  }

  @GetMapping
  ResponseEntity<List<WorkspaceResource>> list(@AuthenticationPrincipal String userId) {
    var resources =
        queryService.handle(new FindWorkspacesByOwnerQuery(UserId.parse(userId))).stream()
            .map(WorkspaceResourceFromEntityAssembler::toResourceFromEntity)
            .toList();
    return ResponseEntity.ok(resources);
  }

  @GetMapping("/{workspaceId}")
  ResponseEntity<WorkspaceResource> getById(@PathVariable UUID workspaceId, @AuthenticationPrincipal String userId) {
    return ResponseEntity.ok(fetch(workspaceId, userId));
  }

  @PostMapping
  ResponseEntity<WorkspaceResource> create(
      @AuthenticationPrincipal String userId, @Valid @RequestBody CreateWorkspaceResource resource) {
    WorkspaceId id = commandService.handle(WorkspaceCommandFromResourceAssembler.toCreateCommand(userId, resource));
    return ResponseEntity.status(HttpStatus.CREATED).body(fetch(id.value(), userId));
  }

  @PatchMapping("/{workspaceId}")
  ResponseEntity<WorkspaceResource> update(
      @PathVariable UUID workspaceId,
      @AuthenticationPrincipal String userId,
      @Valid @RequestBody UpdateWorkspaceResource resource) {
    commandService.handle(
        WorkspaceCommandFromResourceAssembler.toUpdateCommand(workspaceId.toString(), userId, resource));
    return ResponseEntity.ok(fetch(workspaceId, userId));
  }

  @DeleteMapping("/{workspaceId}")
  ResponseEntity<Void> archive(@PathVariable UUID workspaceId, @AuthenticationPrincipal String userId) {
    commandService.handle(WorkspaceCommandFromResourceAssembler.toArchiveCommand(workspaceId.toString(), userId));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{workspaceId}/categories")
  ResponseEntity<WorkspaceResource> addCategory(
      @PathVariable UUID workspaceId,
      @AuthenticationPrincipal String userId,
      @Valid @RequestBody WorkspaceCategoryPayload payload) {
    commandService.handle(
        WorkspaceCommandFromResourceAssembler.toAddCategoryCommand(workspaceId.toString(), userId, payload));
    return ResponseEntity.status(HttpStatus.CREATED).body(fetch(workspaceId, userId));
  }

  @PatchMapping("/{workspaceId}/categories/{categoryId}")
  ResponseEntity<WorkspaceResource> updateCategory(
      @PathVariable UUID workspaceId,
      @PathVariable UUID categoryId,
      @AuthenticationPrincipal String userId,
      @Valid @RequestBody WorkspaceCategoryPayload payload) {
    commandService.handle(
        WorkspaceCommandFromResourceAssembler.toUpdateCategoryCommand(
            workspaceId.toString(), categoryId.toString(), userId, payload));
    return ResponseEntity.ok(fetch(workspaceId, userId));
  }

  @DeleteMapping("/{workspaceId}/categories/{categoryId}")
  ResponseEntity<WorkspaceResource> archiveCategory(
      @PathVariable UUID workspaceId, @PathVariable UUID categoryId, @AuthenticationPrincipal String userId) {
    commandService.handle(
        WorkspaceCommandFromResourceAssembler.toArchiveCategoryCommand(
            workspaceId.toString(), categoryId.toString(), userId));
    return ResponseEntity.ok(fetch(workspaceId, userId));
  }

  private WorkspaceResource fetch(UUID workspaceId, String userId) {
    return queryService
        .handle(new FindWorkspaceByIdQuery(WorkspaceId.of(workspaceId), UserId.parse(userId)))
        .map(WorkspaceResourceFromEntityAssembler::toResourceFromEntity)
        .orElseThrow(() -> new WorkspaceNotFoundException(WorkspaceId.of(workspaceId)));
  }
}
