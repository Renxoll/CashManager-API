package pe.smartcash.cash.groups.domain.model.commands;

import java.math.BigDecimal;
import java.util.List;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

public record AddExpenseCommand(
    GroupId groupId,
    UserId requestingUserId,
    String description,
    BigDecimal amount,
    String currency,
    UserId paidByUserId,
    List<UserId> participantUserIds) {}
