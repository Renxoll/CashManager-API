package pe.smartcash.cash.transactions.application.internal.queryservices;

import java.util.Optional;
import org.springframework.stereotype.Service;
import pe.smartcash.cash.transactions.domain.model.aggregates.Transaction;
import pe.smartcash.cash.transactions.domain.model.aggregates.TransactionRepository;
import pe.smartcash.cash.transactions.domain.model.queries.FindTransactionByIdQuery;
import pe.smartcash.cash.transactions.domain.services.CategoryCatalog;
import pe.smartcash.cash.transactions.domain.services.CategoryDescriptor;
import pe.smartcash.cash.transactions.domain.services.TransactionDetail;
import pe.smartcash.cash.transactions.domain.services.TransactionQueryService;

@Service
class TransactionQueryServiceImpl implements TransactionQueryService {

  private final TransactionRepository transactionRepository;
  private final CategoryCatalog categoryCatalog;

  TransactionQueryServiceImpl(TransactionRepository transactionRepository, CategoryCatalog categoryCatalog) {
    this.transactionRepository = transactionRepository;
    this.categoryCatalog = categoryCatalog;
  }

  @Override
  public Optional<TransactionDetail> handle(FindTransactionByIdQuery query) {
    return transactionRepository.findById(query.transactionId()).map(this::toDetail);
  }

  private TransactionDetail toDetail(Transaction transaction) {
    CategoryDescriptor category = transaction.categoryCode() != null ? categoryCatalog.describe(transaction.categoryCode()) : null;
    return new TransactionDetail(
        transaction.id(), transaction.status(), transaction.money(), transaction.merchant(), category, transaction.errorMessage());
  }
}
