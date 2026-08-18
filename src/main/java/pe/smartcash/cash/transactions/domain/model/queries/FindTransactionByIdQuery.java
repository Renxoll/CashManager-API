package pe.smartcash.cash.transactions.domain.model.queries;

import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionId;

public record FindTransactionByIdQuery(TransactionId transactionId) {}
