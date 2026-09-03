package pe.smartcash.cash.transactions.domain.model.commands;

import java.util.UUID;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionId;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;

/**
 * Mueve una transacción a otro módulo. {@code categoryCode} es obligatorio para un GASTO
 * (una categoría válida en el módulo destino) e ignorado para un INGRESO.
 */
public record MoveTransactionToWorkspaceCommand(
    TransactionId transactionId, UserId requestingUserId, UUID targetWorkspaceId, String categoryCode) {}
