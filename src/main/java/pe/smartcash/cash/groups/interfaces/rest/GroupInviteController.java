package pe.smartcash.cash.groups.interfaces.rest;

import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.smartcash.cash.groups.domain.model.commands.AcceptInviteCommand;
import pe.smartcash.cash.groups.domain.model.commands.DeclineInviteCommand;
import pe.smartcash.cash.groups.domain.model.queries.FindMyPendingInvitesQuery;
import pe.smartcash.cash.groups.domain.model.valueobjects.MembershipId;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;
import pe.smartcash.cash.groups.domain.services.GroupCommandService;
import pe.smartcash.cash.groups.domain.services.GroupQueryService;
import pe.smartcash.cash.groups.interfaces.rest.resources.PendingInviteResource;
import pe.smartcash.cash.groups.interfaces.rest.transform.GroupResourceFromEntityAssembler;

/**
 * Separado de {@link GroupController} porque opera sobre TUS propias invitaciones a través
 * de todos los grupos, no sobre un grupo puntual -- mismo motivo por el que {@code
 * PendingSenderController} es su propio controller en el contexto transactions.
 */
@RestController
@RequestMapping("/api/v1/groups/invites")
class GroupInviteController {

  private final GroupCommandService groupCommandService;
  private final GroupQueryService groupQueryService;

  GroupInviteController(GroupCommandService groupCommandService, GroupQueryService groupQueryService) {
    this.groupCommandService = groupCommandService;
    this.groupQueryService = groupQueryService;
  }

  @GetMapping
  ResponseEntity<List<PendingInviteResource>> myPendingInvites(@AuthenticationPrincipal String authenticatedUserId) {
    UserId userId = UserId.parse(authenticatedUserId);
    var resources =
        groupQueryService.handle(new FindMyPendingInvitesQuery(userId)).stream().map(GroupResourceFromEntityAssembler::toResource).toList();
    return ResponseEntity.ok(resources);
  }

  @PostMapping("/{membershipId}/accept")
  ResponseEntity<Void> accept(@PathVariable UUID membershipId, @AuthenticationPrincipal String authenticatedUserId) {
    UserId userId = UserId.parse(authenticatedUserId);
    groupCommandService.handle(new AcceptInviteCommand(MembershipId.of(membershipId), userId));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{membershipId}/decline")
  ResponseEntity<Void> decline(@PathVariable UUID membershipId, @AuthenticationPrincipal String authenticatedUserId) {
    UserId userId = UserId.parse(authenticatedUserId);
    groupCommandService.handle(new DeclineInviteCommand(MembershipId.of(membershipId), userId));
    return ResponseEntity.noContent().build();
  }
}
