package pe.smartcash.cash.groups.interfaces.rest;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.smartcash.cash.groups.domain.model.commands.ApproveGroupDeletionCommand;
import pe.smartcash.cash.groups.domain.model.commands.CancelGroupDeletionCommand;
import pe.smartcash.cash.groups.domain.model.commands.RequestGroupDeletionCommand;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;
import pe.smartcash.cash.groups.domain.services.GroupCommandService;

/**
 * Borrado de un grupo por consenso: separado de {@link GroupController} porque es un flujo de
 * varios pasos (proponer → aprobar → cancelar) sobre un recurso propio, mismo criterio que
 * {@link GroupInviteController}. El estado de la solicitud PENDING se lee en el detalle del
 * grupo ({@code GET /api/v1/groups/{groupId}} → {@code deletionRequest}).
 */
@RestController
@RequestMapping("/api/v1/groups/{groupId}/deletion-request")
class GroupDeletionController {

  private final GroupCommandService groupCommandService;

  GroupDeletionController(GroupCommandService groupCommandService) {
    this.groupCommandService = groupCommandService;
  }

  @PostMapping
  ResponseEntity<Void> request(@PathVariable UUID groupId, @AuthenticationPrincipal String authenticatedUserId) {
    UserId userId = UserId.parse(authenticatedUserId);
    groupCommandService.handle(new RequestGroupDeletionCommand(GroupId.of(groupId), userId));
    return ResponseEntity.status(HttpStatus.ACCEPTED).build();
  }

  @PostMapping("/approve")
  ResponseEntity<Void> approve(@PathVariable UUID groupId, @AuthenticationPrincipal String authenticatedUserId) {
    UserId userId = UserId.parse(authenticatedUserId);
    groupCommandService.handle(new ApproveGroupDeletionCommand(GroupId.of(groupId), userId));
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping
  ResponseEntity<Void> cancel(@PathVariable UUID groupId, @AuthenticationPrincipal String authenticatedUserId) {
    UserId userId = UserId.parse(authenticatedUserId);
    groupCommandService.handle(new CancelGroupDeletionCommand(GroupId.of(groupId), userId));
    return ResponseEntity.noContent().build();
  }
}
