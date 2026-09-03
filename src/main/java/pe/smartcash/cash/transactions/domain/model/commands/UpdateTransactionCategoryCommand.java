package pe.smartcash.cash.transactions.domain.model.commands;

import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionId;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;

/**
 * {@code categoryCode} llega como texto crudo: en el módulo General el caso de uso lo
 * valida contra {@code CategoryCode} (catálogo cerrado); en un módulo custom lo resuelve
 * contra las categorías de ese módulo vía el ACL de Workspaces.
 */
public record UpdateTransactionCategoryCommand(
    TransactionId transactionId, UserId requestingUserId, String categoryCode) {}
