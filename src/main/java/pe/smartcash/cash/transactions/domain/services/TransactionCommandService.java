package pe.smartcash.cash.transactions.domain.services;

import pe.smartcash.cash.transactions.domain.model.commands.IngestBankNotificationCommand;
import pe.smartcash.cash.transactions.domain.model.commands.RetryFailedTransactionsCommand;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionId;

/**
 * Contrato de escritura del bounded context: vive en domain, la implementación
 * ({@code TransactionCommandServiceImpl}) vive en application. Devuelve solo el id de la
 * transacción resultante (haya quedado PROCESSED o FAILED); el detalle completo se obtiene
 * después vía {@link TransactionQueryService}.
 */
public interface TransactionCommandService {

  TransactionId handle(IngestBankNotificationCommand command);

  RetryFailedTransactionsResult handle(RetryFailedTransactionsCommand command);
}
