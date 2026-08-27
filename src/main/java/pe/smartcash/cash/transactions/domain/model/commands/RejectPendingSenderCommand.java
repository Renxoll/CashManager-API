package pe.smartcash.cash.transactions.domain.model.commands;

import pe.smartcash.cash.transactions.domain.model.valueobjects.PendingSenderId;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;

public record RejectPendingSenderCommand(PendingSenderId pendingSenderId, UserId requestingUserId) {}
