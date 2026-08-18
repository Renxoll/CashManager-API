package pe.smartcash.cash.transactions.interfaces.rest.transform;

import pe.smartcash.cash.transactions.domain.model.commands.IngestBankNotificationCommand;
import pe.smartcash.cash.transactions.interfaces.rest.resources.CreateTransactionResource;

public final class CreateTransactionCommandFromResourceAssembler {

  private CreateTransactionCommandFromResourceAssembler() {}

  public static IngestBankNotificationCommand toCommandFromResource(CreateTransactionResource resource) {
    return new IngestBankNotificationCommand(resource.userId(), resource.rawText());
  }
}
