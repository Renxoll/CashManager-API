package pe.smartcash.cash.groups.interfaces.rest.transform;

import java.util.List;
import pe.smartcash.cash.groups.domain.services.CurrencyBalance;
import pe.smartcash.cash.groups.domain.services.ExpenseDetail;
import pe.smartcash.cash.groups.domain.services.ExpenseShareDetail;
import pe.smartcash.cash.groups.domain.services.GroupDetail;
import pe.smartcash.cash.groups.domain.services.GroupMemberDetail;
import pe.smartcash.cash.groups.domain.services.GroupSummary;
import pe.smartcash.cash.groups.domain.services.PendingInviteDetail;
import pe.smartcash.cash.groups.domain.services.SettlementDetail;
import pe.smartcash.cash.groups.domain.services.SuggestedSettlementDetail;
import pe.smartcash.cash.groups.interfaces.rest.resources.CurrencyBalanceResource;
import pe.smartcash.cash.groups.interfaces.rest.resources.ExpenseResource;
import pe.smartcash.cash.groups.interfaces.rest.resources.ExpenseShareResource;
import pe.smartcash.cash.groups.interfaces.rest.resources.GroupDetailResource;
import pe.smartcash.cash.groups.interfaces.rest.resources.GroupMemberResource;
import pe.smartcash.cash.groups.interfaces.rest.resources.GroupSummaryResource;
import pe.smartcash.cash.groups.interfaces.rest.resources.PendingInviteResource;
import pe.smartcash.cash.groups.interfaces.rest.resources.SettlementResource;
import pe.smartcash.cash.groups.interfaces.rest.resources.SuggestedSettlementResource;

public final class GroupResourceFromEntityAssembler {

  private GroupResourceFromEntityAssembler() {}

  public static GroupSummaryResource toResource(GroupSummary summary) {
    return new GroupSummaryResource(summary.groupId().value(), summary.name(), summary.memberCount(), toBalanceResources(summary.yourBalances()));
  }

  public static GroupDetailResource toResource(GroupDetail detail) {
    return new GroupDetailResource(
        detail.groupId().value(),
        detail.name(),
        detail.ownerId().value(),
        detail.createdAt(),
        detail.members().stream().map(GroupResourceFromEntityAssembler::toResource).toList(),
        detail.expenses().stream().map(GroupResourceFromEntityAssembler::toResource).toList(),
        detail.settlements().stream().map(GroupResourceFromEntityAssembler::toResource).toList(),
        detail.simplifiedDebts().stream().map(GroupResourceFromEntityAssembler::toResource).toList());
  }

  public static PendingInviteResource toResource(PendingInviteDetail detail) {
    return new PendingInviteResource(detail.membershipId().value(), detail.groupId().value(), detail.groupName(), detail.invitedAt());
  }

  private static GroupMemberResource toResource(GroupMemberDetail detail) {
    return new GroupMemberResource(
        detail.membershipId().value(), detail.userId().value(), detail.displayName(), detail.status().name(), toBalanceResources(detail.balances()));
  }

  private static ExpenseResource toResource(ExpenseDetail detail) {
    return new ExpenseResource(
        detail.expenseId().value(),
        detail.description(),
        detail.amount(),
        detail.currency(),
        detail.paidByUserId().value(),
        detail.paidByDisplayName(),
        detail.createdAt(),
        detail.shares().stream().map(GroupResourceFromEntityAssembler::toResource).toList());
  }

  private static ExpenseShareResource toResource(ExpenseShareDetail detail) {
    return new ExpenseShareResource(detail.userId().value(), detail.displayName(), detail.amount());
  }

  private static SettlementResource toResource(SettlementDetail detail) {
    return new SettlementResource(
        detail.settlementId().value(),
        detail.fromUserId().value(),
        detail.fromDisplayName(),
        detail.toUserId().value(),
        detail.toDisplayName(),
        detail.amount(),
        detail.currency(),
        detail.createdAt());
  }

  private static SuggestedSettlementResource toResource(SuggestedSettlementDetail detail) {
    return new SuggestedSettlementResource(
        detail.fromUserId().value(), detail.fromDisplayName(), detail.toUserId().value(), detail.toDisplayName(), detail.amount(), detail.currency());
  }

  private static List<CurrencyBalanceResource> toBalanceResources(List<CurrencyBalance> balances) {
    return balances.stream().map(b -> new CurrencyBalanceResource(b.currency(), b.amount())).toList();
  }
}
