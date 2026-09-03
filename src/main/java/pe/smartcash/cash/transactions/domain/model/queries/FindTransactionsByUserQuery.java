package pe.smartcash.cash.transactions.domain.model.queries;

import java.util.UUID;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;

/** {@code workspaceId} null = todos los módulos del usuario; con valor, acota a ese módulo. */
public record FindTransactionsByUserQuery(UserId userId, int page, int size, UUID workspaceId) {}
