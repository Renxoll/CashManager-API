package pe.smartcash.cash.transactions.domain.services;

import java.util.List;
import java.util.Optional;
import pe.smartcash.cash.transactions.domain.model.queries.FindTransactionByIdQuery;
import pe.smartcash.cash.transactions.domain.model.queries.FindTransactionsByUserQuery;

/** Contrato de lectura del bounded context: vive en domain, la implementación vive en application. */
public interface TransactionQueryService {

  Optional<TransactionDetail> handle(FindTransactionByIdQuery query);

  TransactionDetailPage handle(FindTransactionsByUserQuery query);

  List<CategoryDescriptor> listCategories();
}
