package pe.smartcash.cash.groups.domain.model.commands;

import java.math.BigDecimal;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

/** {@code requestingUserId} es siempre {@code fromUserId} -- uno registra lo que ÉL pagó. */
public record RecordSettlementCommand(GroupId groupId, UserId requestingUserId, UserId toUserId, BigDecimal amount, String currency) {}
