package pe.smartcash.cash.transactions.domain.services;

import java.util.Optional;
import pe.smartcash.cash.transactions.domain.model.queries.FindTransactionByIdQuery;

/** Contrato de lectura del bounded context: vive en domain, la implementación vive en application. */
public interface TransactionQueryService {

  Optional<TransactionDetail> handle(FindTransactionByIdQuery query);
}
