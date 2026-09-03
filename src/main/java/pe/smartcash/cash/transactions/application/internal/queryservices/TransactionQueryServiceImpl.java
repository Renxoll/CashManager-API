package pe.smartcash.cash.transactions.application.internal.queryservices;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import pe.smartcash.cash.transactions.domain.model.aggregates.Transaction;
import pe.smartcash.cash.transactions.domain.model.aggregates.TransactionPage;
import pe.smartcash.cash.transactions.domain.model.aggregates.TransactionRepository;
import pe.smartcash.cash.transactions.domain.model.queries.FindTransactionByIdQuery;
import pe.smartcash.cash.transactions.domain.model.queries.FindTransactionsByUserQuery;
import pe.smartcash.cash.transactions.domain.model.valueobjects.WorkspaceId;
import pe.smartcash.cash.transactions.domain.services.CategoryCatalog;
import pe.smartcash.cash.transactions.domain.services.CategoryDescriptor;
import pe.smartcash.cash.transactions.domain.services.ResolvedCategory;
import pe.smartcash.cash.transactions.domain.services.TransactionDetail;
import pe.smartcash.cash.transactions.domain.services.TransactionDetailPage;
import pe.smartcash.cash.transactions.domain.services.TransactionQueryService;
import pe.smartcash.cash.transactions.domain.services.WorkspaceCategoryView;
import pe.smartcash.cash.transactions.domain.services.WorkspaceDirectory;

@Service
class TransactionQueryServiceImpl implements TransactionQueryService {

  private final TransactionRepository transactionRepository;
  private final CategoryCatalog categoryCatalog;
  private final WorkspaceDirectory workspaceDirectory;

  TransactionQueryServiceImpl(
      TransactionRepository transactionRepository,
      CategoryCatalog categoryCatalog,
      WorkspaceDirectory workspaceDirectory) {
    this.transactionRepository = transactionRepository;
    this.categoryCatalog = categoryCatalog;
    this.workspaceDirectory = workspaceDirectory;
  }

  @Override
  public Optional<TransactionDetail> handle(FindTransactionByIdQuery query) {
    return transactionRepository
        .findById(query.transactionId())
        .map(t -> toDetail(t, describeCustomCategories(List.of(t))));
  }

  @Override
  public TransactionDetailPage handle(FindTransactionsByUserQuery query) {
    TransactionPage page =
        query.workspaceId() != null
            ? transactionRepository.findAllByUserIdAndWorkspaceId(
                query.userId(), WorkspaceId.of(query.workspaceId()), query.page(), query.size())
            : transactionRepository.findAllByUserId(query.userId(), query.page(), query.size());
    Map<UUID, WorkspaceCategoryView> customCategories = describeCustomCategories(page.items());
    List<TransactionDetail> items = page.items().stream().map(t -> toDetail(t, customCategories)).toList();
    return new TransactionDetailPage(items, query.page(), query.size(), page.totalElements());
  }

  @Override
  public List<CategoryDescriptor> listCategories() {
    return categoryCatalog.describeAll();
  }

  private Map<UUID, WorkspaceCategoryView> describeCustomCategories(List<Transaction> transactions) {
    var ids =
        transactions.stream()
            .map(Transaction::workspaceCategoryId)
            .filter(java.util.Objects::nonNull)
            .map(pe.smartcash.cash.transactions.domain.model.valueobjects.WorkspaceCategoryId::value)
            .distinct()
            .toList();
    return workspaceDirectory.describe(ids);
  }

  private TransactionDetail toDetail(Transaction transaction, Map<UUID, WorkspaceCategoryView> customCategories) {
    ResolvedCategory category = resolveCategory(transaction, customCategories);
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
        transaction.createdAt(),
        transaction.workspaceId() != null ? transaction.workspaceId().value() : null);
  }

  private ResolvedCategory resolveCategory(Transaction transaction, Map<UUID, WorkspaceCategoryView> customCategories) {
    if (transaction.categoryCode() != null) {
      CategoryDescriptor descriptor = categoryCatalog.describe(transaction.categoryCode());
      return new ResolvedCategory(descriptor.code().name(), descriptor.displayName(), descriptor.icon());
    }
    if (transaction.workspaceCategoryId() != null) {
      WorkspaceCategoryView view = customCategories.get(transaction.workspaceCategoryId().value());
      if (view != null) {
        return new ResolvedCategory(view.code(), view.displayName(), view.icon());
      }
    }
    return null;
  }
}
