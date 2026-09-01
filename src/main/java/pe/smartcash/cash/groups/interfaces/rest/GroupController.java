package pe.smartcash.cash.groups.interfaces.rest;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.smartcash.cash.groups.domain.model.commands.AddExpenseCommand;
import pe.smartcash.cash.groups.domain.model.commands.CreateGroupCommand;
import pe.smartcash.cash.groups.domain.model.commands.InviteMemberCommand;
import pe.smartcash.cash.groups.domain.model.commands.RecordSettlementCommand;
import pe.smartcash.cash.groups.domain.model.queries.FindGroupDetailQuery;
import pe.smartcash.cash.groups.domain.model.queries.FindMyGroupsQuery;
import pe.smartcash.cash.groups.domain.model.valueobjects.ExpenseId;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.domain.model.valueobjects.MembershipId;
import pe.smartcash.cash.groups.domain.model.valueobjects.SettlementId;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;
import pe.smartcash.cash.groups.domain.services.GroupCommandService;
import pe.smartcash.cash.groups.domain.services.GroupQueryService;
import pe.smartcash.cash.groups.interfaces.rest.resources.AddExpenseResource;
import pe.smartcash.cash.groups.interfaces.rest.resources.CreateGroupResource;
import pe.smartcash.cash.groups.interfaces.rest.resources.ExpenseCreatedResource;
import pe.smartcash.cash.groups.interfaces.rest.resources.GroupCreatedResource;
import pe.smartcash.cash.groups.interfaces.rest.resources.GroupDetailResource;
import pe.smartcash.cash.groups.interfaces.rest.resources.GroupSummaryResource;
import pe.smartcash.cash.groups.interfaces.rest.resources.InviteMemberResource;
import pe.smartcash.cash.groups.interfaces.rest.resources.MembershipCreatedResource;
import pe.smartcash.cash.groups.interfaces.rest.resources.RecordSettlementResource;
import pe.smartcash.cash.groups.interfaces.rest.resources.SettlementCreatedResource;
import pe.smartcash.cash.groups.interfaces.rest.transform.GroupResourceFromEntityAssembler;

@RestController
@RequestMapping("/api/v1/groups")
class GroupController {

  private final GroupCommandService groupCommandService;
  private final GroupQueryService groupQueryService;

  GroupController(GroupCommandService groupCommandService, GroupQueryService groupQueryService) {
    this.groupCommandService = groupCommandService;
    this.groupQueryService = groupQueryService;
  }

  @PostMapping
  ResponseEntity<GroupCreatedResource> create(@AuthenticationPrincipal String authenticatedUserId, @Valid @RequestBody CreateGroupResource resource) {
    UserId userId = UserId.parse(authenticatedUserId);
    GroupId groupId = groupCommandService.handle(new CreateGroupCommand(userId, resource.name().trim()));
    return ResponseEntity.status(HttpStatus.CREATED).body(new GroupCreatedResource(groupId.value(), resource.name().trim()));
  }

  @GetMapping
  ResponseEntity<List<GroupSummaryResource>> myGroups(@AuthenticationPrincipal String authenticatedUserId) {
    UserId userId = UserId.parse(authenticatedUserId);
    var resources =
        groupQueryService.handle(new FindMyGroupsQuery(userId)).stream().map(GroupResourceFromEntityAssembler::toResource).toList();
    return ResponseEntity.ok(resources);
  }

  @GetMapping("/{groupId}")
  ResponseEntity<GroupDetailResource> detail(@PathVariable UUID groupId, @AuthenticationPrincipal String authenticatedUserId) {
    UserId userId = UserId.parse(authenticatedUserId);
    var detail = groupQueryService.handle(new FindGroupDetailQuery(GroupId.of(groupId), userId));
    return ResponseEntity.ok(GroupResourceFromEntityAssembler.toResource(detail));
  }

  @PostMapping("/{groupId}/invites")
  ResponseEntity<MembershipCreatedResource> invite(
      @PathVariable UUID groupId, @AuthenticationPrincipal String authenticatedUserId, @Valid @RequestBody InviteMemberResource resource) {
    UserId userId = UserId.parse(authenticatedUserId);
    MembershipId membershipId = groupCommandService.handle(new InviteMemberCommand(GroupId.of(groupId), userId, resource.email().trim()));
    return ResponseEntity.status(HttpStatus.CREATED).body(new MembershipCreatedResource(membershipId.value()));
  }

  @PostMapping("/{groupId}/expenses")
  ResponseEntity<ExpenseCreatedResource> addExpense(
      @PathVariable UUID groupId, @AuthenticationPrincipal String authenticatedUserId, @Valid @RequestBody AddExpenseResource resource) {
    UserId userId = UserId.parse(authenticatedUserId);
    List<UserId> participants = resource.participantUserIds().stream().map(UserId::of).toList();
    ExpenseId expenseId =
        groupCommandService.handle(
            new AddExpenseCommand(
                GroupId.of(groupId),
                userId,
                resource.description().trim(),
                resource.amount(),
                resource.currency().trim().toUpperCase(),
                UserId.of(resource.paidByUserId()),
                participants));
    return ResponseEntity.status(HttpStatus.CREATED).body(new ExpenseCreatedResource(expenseId.value()));
  }

  @PostMapping("/{groupId}/settlements")
  ResponseEntity<SettlementCreatedResource> recordSettlement(
      @PathVariable UUID groupId, @AuthenticationPrincipal String authenticatedUserId, @Valid @RequestBody RecordSettlementResource resource) {
    UserId userId = UserId.parse(authenticatedUserId);
    SettlementId settlementId =
        groupCommandService.handle(
            new RecordSettlementCommand(
                GroupId.of(groupId), userId, UserId.of(resource.toUserId()), resource.amount(), resource.currency().trim().toUpperCase()));
    return ResponseEntity.status(HttpStatus.CREATED).body(new SettlementCreatedResource(settlementId.value()));
  }
}
