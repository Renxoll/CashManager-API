package pe.smartcash.cash.groups.domain.services;

import pe.smartcash.cash.groups.domain.model.commands.AcceptInviteCommand;
import pe.smartcash.cash.groups.domain.model.commands.AddExpenseCommand;
import pe.smartcash.cash.groups.domain.model.commands.CreateGroupCommand;
import pe.smartcash.cash.groups.domain.model.commands.DeclineInviteCommand;
import pe.smartcash.cash.groups.domain.model.commands.InviteMemberCommand;
import pe.smartcash.cash.groups.domain.model.commands.RecordSettlementCommand;
import pe.smartcash.cash.groups.domain.model.valueobjects.ExpenseId;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.domain.model.valueobjects.MembershipId;
import pe.smartcash.cash.groups.domain.model.valueobjects.SettlementId;

/** Contrato de escritura del bounded context: vive en domain, la implementación vive en application. */
public interface GroupCommandService {

  GroupId handle(CreateGroupCommand command);

  MembershipId handle(InviteMemberCommand command);

  void handle(AcceptInviteCommand command);

  void handle(DeclineInviteCommand command);

  ExpenseId handle(AddExpenseCommand command);

  SettlementId handle(RecordSettlementCommand command);
}
