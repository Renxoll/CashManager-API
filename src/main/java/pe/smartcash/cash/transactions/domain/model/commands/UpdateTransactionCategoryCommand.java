package pe.smartcash.cash.transactions.domain.model.commands;

import pe.smartcash.cash.transactions.domain.model.valueobjects.CategoryCode;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionId;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;

public record UpdateTransactionCategoryCommand(TransactionId transactionId, UserId requestingUserId, CategoryCode newCategoryCode) {}
