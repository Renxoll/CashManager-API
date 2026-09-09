package pe.smartcash.cash.groups.domain.model.commands;

import java.math.BigDecimal;
import java.util.List;
import pe.smartcash.cash.groups.domain.model.valueobjects.ExpenseId;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

/** Corrige un gasto ya registrado. Solo lo permite quien pagó el gasto o el owner del grupo. */
public record EditExpenseCommand(
    GroupId groupId,
    ExpenseId expenseId,
    UserId requestingUserId,
    String description,
    BigDecimal amount,
    String currency,
    UserId paidByUserId,
    List<UserId> participantUserIds) {}
