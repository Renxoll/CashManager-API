package pe.smartcash.cash.transactions.domain.model.commands;

import pe.smartcash.cash.transactions.domain.model.valueobjects.PendingSenderId;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;

public record ApprovePendingSenderCommand(PendingSenderId pendingSenderId, UserId requestingUserId) {}
