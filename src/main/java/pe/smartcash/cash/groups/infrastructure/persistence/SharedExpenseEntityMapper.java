package pe.smartcash.cash.groups.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import pe.smartcash.cash.groups.domain.model.aggregates.SharedExpense;
import pe.smartcash.cash.groups.domain.model.valueobjects.ExpenseId;
import pe.smartcash.cash.groups.domain.model.valueobjects.ExpenseShare;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.domain.model.valueobjects.Money;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

@Component
class SharedExpenseEntityMapper {

  SharedExpenseJpaEntity toJpaEntity(SharedExpense expense) {
    return SharedExpenseJpaEntity.builder()
        .id(expense.id().value())
        .groupId(expense.groupId().value())
        .description(expense.description())
        .amount(expense.amount().amount())
        .currency(expense.amount().currency())
        .paidByUserId(expense.paidByUserId().value())
        .createdAt(expense.createdAt())
        .build();
  }

  List<ExpenseShareJpaEntity> toShareJpaEntities(SharedExpense expense) {
    String currency = expense.amount().currency();
    return expense.shares().stream()
        .map(
            share ->
                ExpenseShareJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .expenseId(expense.id().value())
                    .userId(share.userId().value())
                    .amount(share.amount().amount())
                    .build())
        .toList();
  }

  SharedExpense toDomain(SharedExpenseJpaEntity entity, List<ExpenseShareJpaEntity> shareEntities) {
    Money amount = new Money(entity.getAmount(), entity.getCurrency());
    List<ExpenseShare> shares =
        shareEntities.stream()
            .map(shareEntity -> new ExpenseShare(UserId.of(shareEntity.getUserId()), new Money(shareEntity.getAmount(), entity.getCurrency())))
            .toList();
    return SharedExpense.rehydrate(
        ExpenseId.of(entity.getId()),
        GroupId.of(entity.getGroupId()),
        entity.getDescription(),
        amount,
        UserId.of(entity.getPaidByUserId()),
        shares,
        entity.getCreatedAt());
  }
}
