package pe.smartcash.cash.transactions.domain.services;

import java.util.Optional;
import pe.smartcash.cash.transactions.domain.model.commands.IngestBankNotificationCommand;
import pe.smartcash.cash.transactions.domain.model.commands.IngestEmailedTransactionCommand;
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

  /**
   * A diferencia de {@link #handle(IngestBankNotificationCommand)}, acá SÍ puede no crearse
   * ninguna transacción (remitente no identificado como banco, o dirección de ingesta
   * desconocida) — por eso {@code Optional} en vez de lanzar: el caller (el webhook de
   * SendGrid) igual responde 200, no hay un cliente HTTP esperando un error puntual.
   */
  Optional<TransactionId> handle(IngestEmailedTransactionCommand command);

  RetryFailedTransactionsResult handle(RetryFailedTransactionsCommand command);
}
