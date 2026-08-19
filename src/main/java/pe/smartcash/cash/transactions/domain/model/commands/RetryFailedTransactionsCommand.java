package pe.smartcash.cash.transactions.domain.model.commands;

/** Sin parámetros a propósito: reprocesa todas las transacciones FAILED existentes, no una en particular. */
public record RetryFailedTransactionsCommand() {}
