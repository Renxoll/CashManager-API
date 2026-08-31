package pe.smartcash.cash.transactions.application.internal.queryservices;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import pe.smartcash.cash.transactions.domain.model.aggregates.Transaction;
import pe.smartcash.cash.transactions.domain.model.aggregates.TransactionPage;
import pe.smartcash.cash.transactions.domain.model.aggregates.TransactionRepository;
import pe.smartcash.cash.transactions.domain.model.queries.FindTransactionByIdQuery;
import pe.smartcash.cash.transactions.domain.model.queries.FindTransactionsByUserQuery;
import pe.smartcash.cash.transactions.domain.services.CategoryCatalog;
import pe.smartcash.cash.transactions.domain.services.CategoryDescriptor;
import pe.smartcash.cash.transactions.domain.services.TransactionDetail;
import pe.smartcash.cash.transactions.domain.services.TransactionDetailPage;
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

  @Override
  public TransactionDetailPage handle(FindTransactionsByUserQuery query) {
    TransactionPage page = transactionRepository.findAllByUserId(query.userId(), query.page(), query.size());
    List<TransactionDetail> items = page.items().stream().map(this::toDetail).toList();
    return new TransactionDetailPage(items, query.page(), query.size(), page.totalElements());
  }

  @Override
  public List<CategoryDescriptor> listCategories() {
    return categoryCatalog.describeAll();
  }

  private TransactionDetail toDetail(Transaction transaction) {
    CategoryDescriptor category = transaction.categoryCode() != null ? categoryCatalog.describe(transaction.categoryCode()) : null;
    return new TransactionDetail(
        transaction.id(),
        transaction.userId(),
        transaction.status(),
        transaction.money(),
        transaction.merchant(),
        category,
        transaction.type(),
        transaction.internalTransfer(),
        transaction.errorMessage(),
        transaction.createdAt());
  }
}
