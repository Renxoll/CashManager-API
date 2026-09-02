package pe.smartcash.cash.transactions.domain.services;

import java.util.Optional;
import pe.smartcash.cash.transactions.domain.model.commands.IngestBankNotificationCommand;
import pe.smartcash.cash.transactions.domain.model.commands.IngestEmailedTransactionCommand;
import pe.smartcash.cash.transactions.domain.model.commands.MoveTransactionToWorkspaceCommand;
import pe.smartcash.cash.transactions.domain.model.commands.RecordManualIncomeCommand;
import pe.smartcash.cash.transactions.domain.model.commands.RetryFailedTransactionsCommand;
import pe.smartcash.cash.transactions.domain.model.commands.SetInternalTransferCommand;
import pe.smartcash.cash.transactions.domain.model.commands.UpdateTransactionCategoryCommand;
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

  /**
   * Corrección manual de categoría. A diferencia de {@link #handle(IngestEmailedTransactionCommand)},
   * acá SÍ es un error reportable: si el id no existe o no pertenece a {@code
   * requestingUserId}, lanza {@code TransactionNotFoundException} (mismo 404 en ambos casos,
   * no se distingue "no existe" de "no es tuya").
   */
  void handle(UpdateTransactionCategoryCommand command);

  /**
   * Marca o desmarca una transacción como transferencia entre cuentas propias del usuario.
   * Mismo criterio de error que {@link #handle(UpdateTransactionCategoryCommand)}: id
   * inexistente o de otro usuario → {@code TransactionNotFoundException} (404).
   */
  void handle(SetInternalTransferCommand command);

  /**
   * Mueve una transacción a otro módulo del usuario, reasignando su categoría a una válida
   * en el módulo destino (para gastos). Mismo criterio de error 404 que {@link
   * #handle(UpdateTransactionCategoryCommand)} para la transacción y el módulo destino.
   */
  void handle(MoveTransactionToWorkspaceCommand command);

  /** Registro manual de un ingreso -- siempre PROCESSED de una, nunca falla por LLM porque
   * no hay extracción de por medio. */
  TransactionId handle(RecordManualIncomeCommand command);
}
