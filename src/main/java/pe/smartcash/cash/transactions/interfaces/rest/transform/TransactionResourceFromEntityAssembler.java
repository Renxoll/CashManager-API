package pe.smartcash.cash.transactions.interfaces.rest.transform;

import pe.smartcash.cash.transactions.domain.services.TransactionDetail;
import pe.smartcash.cash.transactions.interfaces.rest.resources.TransactionResource;

public final class TransactionResourceFromEntityAssembler {

  private TransactionResourceFromEntityAssembler() {}

  public static TransactionResource toResourceFromEntity(TransactionDetail detail) {
    return new TransactionResource(
        detail.id().value(),
        detail.status().name(),
        detail.money() != null ? detail.money().amount() : null,
        detail.money() != null ? detail.money().currency() : null,
        detail.merchant() != null ? detail.merchant().name() : null,
        detail.category() != null ? detail.category().displayName() : null);
  }
}
