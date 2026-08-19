package pe.smartcash.cash.transactions.interfaces.rest.resources;

public record RetryFailedTransactionsResource(int retried, int stillFailed) {}
