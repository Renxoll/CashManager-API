package pe.smartcash.cash.transactions.domain.services;

/** Resumen del lote: cuántas FAILED se recuperaron a PROCESSED y cuántas siguen FAILED. */
public record RetryFailedTransactionsResult(int retried, int stillFailed) {}
