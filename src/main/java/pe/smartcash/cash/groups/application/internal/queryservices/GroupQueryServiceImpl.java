package pe.smartcash.cash.groups.application.internal.queryservices;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import pe.smartcash.cash.groups.domain.exception.GroupNotFoundException;
import pe.smartcash.cash.groups.domain.model.aggregates.Group;
import pe.smartcash.cash.groups.domain.model.aggregates.GroupMembership;
import pe.smartcash.cash.groups.domain.model.aggregates.GroupMembershipRepository;
import pe.smartcash.cash.groups.domain.model.aggregates.GroupRepository;
import pe.smartcash.cash.groups.domain.model.aggregates.SharedExpense;
import pe.smartcash.cash.groups.domain.model.aggregates.SharedExpenseRepository;
import pe.smartcash.cash.groups.domain.model.aggregates.Settlement;
import pe.smartcash.cash.groups.domain.model.aggregates.SettlementRepository;
import pe.smartcash.cash.groups.domain.model.queries.FindGroupDetailQuery;
import pe.smartcash.cash.groups.domain.model.queries.FindMyGroupsQuery;
import pe.smartcash.cash.groups.domain.model.queries.FindMyPendingInvitesQuery;
import pe.smartcash.cash.groups.domain.model.valueobjects.ExpenseShare;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.domain.model.valueobjects.MembershipStatus;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;
import pe.smartcash.cash.groups.domain.services.CurrencyBalance;
import pe.smartcash.cash.groups.domain.services.DebtSimplifier;
import pe.smartcash.cash.groups.domain.services.ExpenseDetail;
import pe.smartcash.cash.groups.domain.services.ExpenseShareDetail;
import pe.smartcash.cash.groups.domain.services.GroupDetail;
import pe.smartcash.cash.groups.domain.services.GroupMemberDetail;
import pe.smartcash.cash.groups.domain.services.GroupQueryService;
import pe.smartcash.cash.groups.domain.services.GroupSummary;
import pe.smartcash.cash.groups.domain.services.PendingInviteDetail;
import pe.smartcash.cash.groups.domain.services.SettlementDetail;
import pe.smartcash.cash.groups.domain.services.SuggestedSettlement;
import pe.smartcash.cash.groups.domain.services.SuggestedSettlementDetail;
import pe.smartcash.cash.groups.domain.services.UserDirectory;
import pe.smartcash.cash.groups.infrastructure.persistence.GroupBalanceReadRepository;

@Service
class GroupQueryServiceImpl implements GroupQueryService {

  private final GroupRepository groupRepository;
  private final GroupMembershipRepository membershipRepository;
  private final SharedExpenseRepository sharedExpenseRepository;
  private final SettlementRepository settlementRepository;
  private final GroupBalanceReadRepository balanceReadRepository;
  private final DebtSimplifier debtSimplifier;
  private final UserDirectory userDirectory;

  GroupQueryServiceImpl(
      GroupRepository groupRepository,
      GroupMembershipRepository membershipRepository,
      SharedExpenseRepository sharedExpenseRepository,
      SettlementRepository settlementRepository,
      GroupBalanceReadRepository balanceReadRepository,
      DebtSimplifier debtSimplifier,
      UserDirectory userDirectory) {
    this.groupRepository = groupRepository;
    this.membershipRepository = membershipRepository;
    this.sharedExpenseRepository = sharedExpenseRepository;
    this.settlementRepository = settlementRepository;
    this.balanceReadRepository = balanceReadRepository;
    this.debtSimplifier = debtSimplifier;
    this.userDirectory = userDirectory;
  }

  @Override
  public List<GroupSummary> handle(FindMyGroupsQuery query) {
    return groupRepository.findAllByMemberUserId(query.userId()).stream()
        .map(
            group -> {
              int memberCount = (int) membershipRepository.findAllByGroupId(group.id()).stream().filter(GroupMembership::isAccepted).count();
              List<CurrencyBalance> yourBalances = balanceReadRepository.netBalancesFor(group.id(), query.userId());
              return new GroupSummary(group.id(), group.name(), memberCount, yourBalances);
            })
        .toList();
  }

  @Override
  public GroupDetail handle(FindGroupDetailQuery query) {
    Group group = requireAcceptedMemberAndGroup(query.groupId(), query.requestingUserId());

    List<GroupMembership> memberships = membershipRepository.findAllByGroupId(query.groupId());
    Map<UserId, Map<String, BigDecimal>> balancesByUser = balanceReadRepository.computeNetBalances(query.groupId());

    List<GroupMemberDetail> members =
        memberships.stream()
            .map(
                m ->
                    new GroupMemberDetail(
                        m.id(), m.userId(), displayName(m.userId()), m.status(), toCurrencyBalances(balancesByUser.get(m.userId()))))
            .toList();

    List<ExpenseDetail> expenses = sharedExpenseRepository.findAllByGroupId(query.groupId()).stream().map(this::toExpenseDetail).toList();
    List<SettlementDetail> settlements = settlementRepository.findAllByGroupId(query.groupId()).stream().map(this::toSettlementDetail).toList();
    List<SuggestedSettlementDetail> simplifiedDebts = simplifyDebts(balancesByUser);

    return new GroupDetail(group.id(), group.name(), group.ownerId(), group.createdAt(), members, expenses, settlements, simplifiedDebts);
  }

  @Override
  public List<PendingInviteDetail> handle(FindMyPendingInvitesQuery query) {
    return membershipRepository.findAllByUserIdAndStatus(query.userId(), MembershipStatus.PENDING).stream()
        .map(
            m -> {
              String groupName = groupRepository.findById(m.groupId()).map(Group::name).orElse("Grupo");
              return new PendingInviteDetail(m.id(), m.groupId(), groupName, m.invitedAt());
            })
        .toList();
  }

  private Group requireAcceptedMemberAndGroup(GroupId groupId, UserId requestingUserId) {
    boolean isAcceptedMember =
        membershipRepository.findByGroupIdAndUserId(groupId, requestingUserId).map(GroupMembership::isAccepted).orElse(false);
    if (!isAcceptedMember) {
      throw new GroupNotFoundException(groupId);
    }
    return groupRepository.findById(groupId).orElseThrow(() -> new GroupNotFoundException(groupId));
  }

  private ExpenseDetail toExpenseDetail(SharedExpense expense) {
    List<ExpenseShareDetail> shares = expense.shares().stream().map(this::toExpenseShareDetail).toList();
    return new ExpenseDetail(
        expense.id(),
        expense.description(),
        expense.amount().amount(),
        expense.amount().currency(),
        expense.paidByUserId(),
        displayName(expense.paidByUserId()),
        expense.createdAt(),
        shares);
  }

  private ExpenseShareDetail toExpenseShareDetail(ExpenseShare share) {
    return new ExpenseShareDetail(share.userId(), displayName(share.userId()), share.amount().amount());
  }

  private SettlementDetail toSettlementDetail(Settlement settlement) {
    return new SettlementDetail(
        settlement.id(),
        settlement.fromUserId(),
        displayName(settlement.fromUserId()),
        settlement.toUserId(),
        displayName(settlement.toUserId()),
        settlement.amount().amount(),
        settlement.amount().currency(),
        settlement.createdAt());
  }

  /** Corre {@link DebtSimplifier} por separado en cada moneda presente en el grupo -- sin
   * conversión entre monedas, ver el javadoc de GroupBalanceReadRepository. */
  private List<SuggestedSettlementDetail> simplifyDebts(Map<UserId, Map<String, BigDecimal>> balancesByUser) {
    Map<String, Map<UserId, BigDecimal>> byCurrency = new HashMap<>();
    balancesByUser.forEach(
        (userId, currencyMap) ->
            currencyMap.forEach((currency, amount) -> byCurrency.computeIfAbsent(currency, key -> new HashMap<>()).put(userId, amount)));

    List<SuggestedSettlementDetail> result = new ArrayList<>();
    byCurrency.forEach(
        (currency, balances) -> {
          for (SuggestedSettlement suggestion : debtSimplifier.simplify(balances)) {
            result.add(
                new SuggestedSettlementDetail(
                    suggestion.from(), displayName(suggestion.from()), suggestion.to(), displayName(suggestion.to()), suggestion.amount(), currency));
          }
        });
    return result;
  }

  private List<CurrencyBalance> toCurrencyBalances(Map<String, BigDecimal> byCurrency) {
    if (byCurrency == null) {
      return List.of();
    }
    List<CurrencyBalance> balances = new ArrayList<>();
    byCurrency.forEach((currency, amount) -> balances.add(new CurrencyBalance(currency, amount)));
    return balances;
  }

  private String displayName(UserId userId) {
    return userDirectory.findDisplayName(userId).orElse("Usuario");
  }
}
