package pe.smartcash.cash.transactions.domain.model.queries;

import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;

public record FindPendingSendersByUserQuery(UserId userId) {}
