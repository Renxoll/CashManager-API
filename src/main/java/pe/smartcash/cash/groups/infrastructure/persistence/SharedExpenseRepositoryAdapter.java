package pe.smartcash.cash.groups.infrastructure.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import pe.smartcash.cash.groups.domain.model.aggregates.SharedExpense;
import pe.smartcash.cash.groups.domain.model.aggregates.SharedExpenseRepository;
import pe.smartcash.cash.groups.domain.model.valueobjects.ExpenseId;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.infrastructure.persistence.jpa.repositories.ExpenseShareJpaRepository;
import pe.smartcash.cash.groups.infrastructure.persistence.jpa.repositories.SharedExpenseJpaRepository;

/**
 * A diferencia del resto de adaptadores del proyecto, este escribe/lee DOS tablas ({@code
 * shared_expenses} + {@code expense_shares}) en cada operación -- el gasto y sus shares son
 * un único agregado, ver el javadoc de {@link SharedExpense}. El caller ({@code
 * GroupCommandServiceImpl}/{@code GroupQueryServiceImpl}) ya corre dentro de un {@code
 * @Transactional}, así que ambas escrituras son atómicas.
 */
@Repository
class SharedExpenseRepositoryAdapter implements SharedExpenseRepository {

  private final SharedExpenseJpaRepository sharedExpenseJpaRepository;
  private final ExpenseShareJpaRepository expenseShareJpaRepository;
  private final SharedExpenseEntityMapper mapper;

  SharedExpenseRepositoryAdapter(
      SharedExpenseJpaRepository sharedExpenseJpaRepository, ExpenseShareJpaRepository expenseShareJpaRepository, SharedExpenseEntityMapper mapper) {
    this.sharedExpenseJpaRepository = sharedExpenseJpaRepository;
    this.expenseShareJpaRepository = expenseShareJpaRepository;
    this.mapper = mapper;
  }

  @Override
  public void save(SharedExpense expense) {
    sharedExpenseJpaRepository.save(mapper.toJpaEntity(expense));
    expenseShareJpaRepository.saveAll(mapper.toShareJpaEntities(expense));
  }

  @Override
  public Optional<SharedExpense> findById(ExpenseId id) {
    return sharedExpenseJpaRepository
        .findById(id.value())
        .map(entity -> mapper.toDomain(entity, expenseShareJpaRepository.findAllByExpenseId(entity.getId())));
  }

  @Override
  public List<SharedExpense> findAllByGroupId(GroupId groupId) {
    var expenseEntities = sharedExpenseJpaRepository.findAllByGroupIdOrderByCreatedAtDesc(groupId.value());
    var expenseIds = expenseEntities.stream().map(SharedExpenseJpaEntity::getId).toList();
    Map<UUID, List<ExpenseShareJpaEntity>> sharesByExpenseId =
        expenseShareJpaRepository.findAllByExpenseIdIn(expenseIds).stream()
            .collect(Collectors.groupingBy(ExpenseShareJpaEntity::getExpenseId));
    return expenseEntities.stream()
        .map(entity -> mapper.toDomain(entity, sharesByExpenseId.getOrDefault(entity.getId(), List.of())))
        .toList();
  }
}
