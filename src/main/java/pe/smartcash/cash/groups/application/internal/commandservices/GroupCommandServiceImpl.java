package pe.smartcash.cash.groups.application.internal.commandservices;

import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smartcash.cash.groups.domain.exception.DuplicateMembershipException;
import pe.smartcash.cash.groups.domain.exception.GroupNotFoundException;
import pe.smartcash.cash.groups.domain.exception.InvitedUserNotRegisteredException;
import pe.smartcash.cash.groups.domain.exception.MembershipNotFoundException;
import pe.smartcash.cash.groups.domain.exception.NotAGroupMemberException;
import pe.smartcash.cash.groups.domain.model.aggregates.Group;
import pe.smartcash.cash.groups.domain.model.aggregates.GroupMembership;
import pe.smartcash.cash.groups.domain.model.aggregates.GroupMembershipRepository;
import pe.smartcash.cash.groups.domain.model.aggregates.GroupRepository;
import pe.smartcash.cash.groups.domain.model.aggregates.SharedExpense;
import pe.smartcash.cash.groups.domain.model.aggregates.SharedExpenseRepository;
import pe.smartcash.cash.groups.domain.model.aggregates.Settlement;
import pe.smartcash.cash.groups.domain.model.aggregates.SettlementRepository;
import pe.smartcash.cash.groups.domain.model.commands.AcceptInviteCommand;
import pe.smartcash.cash.groups.domain.model.commands.AddExpenseCommand;
import pe.smartcash.cash.groups.domain.model.commands.CreateGroupCommand;
import pe.smartcash.cash.groups.domain.model.commands.DeclineInviteCommand;
import pe.smartcash.cash.groups.domain.model.commands.InviteMemberCommand;
import pe.smartcash.cash.groups.domain.model.commands.RecordSettlementCommand;
import pe.smartcash.cash.groups.domain.model.valueobjects.ExpenseId;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.domain.model.valueobjects.MembershipId;
import pe.smartcash.cash.groups.domain.model.valueobjects.MembershipStatus;
import pe.smartcash.cash.groups.domain.model.valueobjects.Money;
import pe.smartcash.cash.groups.domain.model.valueobjects.SettlementId;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;
import pe.smartcash.cash.groups.domain.services.GroupCommandService;
import pe.smartcash.cash.groups.domain.services.UserDirectory;

@Service
class GroupCommandServiceImpl implements GroupCommandService {

  private final GroupRepository groupRepository;
  private final GroupMembershipRepository membershipRepository;
  private final SharedExpenseRepository sharedExpenseRepository;
  private final SettlementRepository settlementRepository;
  private final UserDirectory userDirectory;
  private final Clock clock;

  GroupCommandServiceImpl(
      GroupRepository groupRepository,
      GroupMembershipRepository membershipRepository,
      SharedExpenseRepository sharedExpenseRepository,
      SettlementRepository settlementRepository,
      UserDirectory userDirectory,
      Clock clock) {
    this.groupRepository = groupRepository;
    this.membershipRepository = membershipRepository;
    this.sharedExpenseRepository = sharedExpenseRepository;
    this.settlementRepository = settlementRepository;
    this.userDirectory = userDirectory;
    this.clock = clock;
  }

  @Override
  @Transactional
  public GroupId handle(CreateGroupCommand command) {
    GroupId groupId = GroupId.newId();
    Group group = Group.create(groupId, command.name(), command.requestingUserId(), clock.instant());
    groupRepository.save(group);

    GroupMembership ownerMembership = GroupMembership.ownerMembership(MembershipId.newId(), groupId, command.requestingUserId(), clock.instant());
    membershipRepository.save(ownerMembership);

    return groupId;
  }

  @Override
  @Transactional
  public MembershipId handle(InviteMemberCommand command) {
    requireAcceptedMember(command.groupId(), command.requestingUserId());

    UserId inviteeUserId =
        userDirectory.findUserIdByEmail(command.inviteeEmail()).orElseThrow(() -> new InvitedUserNotRegisteredException(command.inviteeEmail()));

    membershipRepository
        .findByGroupIdAndUserId(command.groupId(), inviteeUserId)
        .filter(m -> m.status() == MembershipStatus.PENDING || m.status() == MembershipStatus.ACCEPTED)
        .ifPresent(
            m -> {
              throw new DuplicateMembershipException();
            });

    GroupMembership invite = GroupMembership.invite(MembershipId.newId(), command.groupId(), inviteeUserId, clock.instant());
    membershipRepository.save(invite);
    return invite.id();
  }

  @Override
  @Transactional
  public void handle(AcceptInviteCommand command) {
    GroupMembership membership = requireOwnMembership(command.membershipId(), command.requestingUserId());
    membership.accept(clock.instant());
    membershipRepository.save(membership);
  }

  @Override
  @Transactional
  public void handle(DeclineInviteCommand command) {
    GroupMembership membership = requireOwnMembership(command.membershipId(), command.requestingUserId());
    membership.decline(clock.instant());
    membershipRepository.save(membership);
  }

  @Override
  @Transactional
  public ExpenseId handle(AddExpenseCommand command) {
    requireAcceptedMember(command.groupId(), command.requestingUserId());
    requireAcceptedMember(command.groupId(), command.paidByUserId(), "El pagador");
    for (UserId participant : command.participantUserIds()) {
      requireAcceptedMember(command.groupId(), participant, "Uno de los participantes");
    }

    Money totalAmount = new Money(command.amount(), command.currency());
    SharedExpense expense =
        SharedExpense.splitEqually(
            ExpenseId.newId(),
            command.groupId(),
            command.description(),
            totalAmount,
            command.paidByUserId(),
            command.participantUserIds(),
            clock.instant());
    sharedExpenseRepository.save(expense);
    return expense.id();
  }

  @Override
  @Transactional
  public SettlementId handle(RecordSettlementCommand command) {
    requireAcceptedMember(command.groupId(), command.requestingUserId());
    requireAcceptedMember(command.groupId(), command.toUserId(), "El destinatario del pago");

    Money amount = new Money(command.amount(), command.currency());
    Settlement settlement = Settlement.record(SettlementId.newId(), command.groupId(), command.requestingUserId(), command.toUserId(), amount, clock.instant());
    settlementRepository.save(settlement);
    return settlement.id();
  }

  /** El requester no ve el grupo si no es miembro -- mismo 404 sea que el grupo no exista o
   * que exista pero él no sea parte, ver GroupNotFoundException. */
  private void requireAcceptedMember(GroupId groupId, UserId userId) {
    membershipRepository
        .findByGroupIdAndUserId(groupId, userId)
        .filter(GroupMembership::isAccepted)
        .orElseThrow(() -> new GroupNotFoundException(groupId));
  }

  /** Validación de un tercero mencionado en el request (pagador, participante, destinatario)
   * -- acá SÍ es un 400 reportable, el request en sí está mal armado. */
  private void requireAcceptedMember(GroupId groupId, UserId userId, String who) {
    membershipRepository
        .findByGroupIdAndUserId(groupId, userId)
        .filter(GroupMembership::isAccepted)
        .orElseThrow(() -> new NotAGroupMemberException(who + " no es miembro aceptado de este grupo"));
  }

  private GroupMembership requireOwnMembership(MembershipId membershipId, UserId requestingUserId) {
    return membershipRepository
        .findById(membershipId)
        .filter(m -> m.userId().equals(requestingUserId))
        .orElseThrow(() -> new MembershipNotFoundException(membershipId));
  }
}
