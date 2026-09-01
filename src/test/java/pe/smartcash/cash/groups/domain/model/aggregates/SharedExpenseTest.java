package pe.smartcash.cash.groups.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.smartcash.cash.groups.domain.model.valueobjects.ExpenseId;
import pe.smartcash.cash.groups.domain.model.valueobjects.ExpenseShare;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.domain.model.valueobjects.Money;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

class SharedExpenseTest {

  private final GroupId groupId = GroupId.of(UUID.randomUUID());
  private final UserId userA = UserId.of(UUID.randomUUID());
  private final UserId userB = UserId.of(UUID.randomUUID());
  private final UserId userC = UserId.of(UUID.randomUUID());

  @Test
  void shouldSplitEvenlyWhenDivisionIsExact() {
    SharedExpense expense =
        SharedExpense.splitEqually(
            ExpenseId.newId(), groupId, "Hotel", new Money(new BigDecimal("100.00"), "PEN"), userA, List.of(userA, userB), Instant.now());

    assertThat(expense.shares()).containsExactly(new ExpenseShare(userA, new Money(new BigDecimal("50.00"), "PEN")), new ExpenseShare(
        userB, new Money(new BigDecimal("50.00"), "PEN")));
  }

  @Test
  void shouldDistributeRemainderCentsToFirstParticipantsInOrder() {
    SharedExpense expense =
        SharedExpense.splitEqually(
            ExpenseId.newId(), groupId, "Taxi", new Money(new BigDecimal("10.00"), "PEN"), userA, List.of(userA, userB, userC), Instant.now());

    assertThat(expense.shares())
        .containsExactly(
            new ExpenseShare(userA, new Money(new BigDecimal("3.34"), "PEN")),
            new ExpenseShare(userB, new Money(new BigDecimal("3.33"), "PEN")),
            new ExpenseShare(userC, new Money(new BigDecimal("3.33"), "PEN")));

    BigDecimal sum = expense.shares().stream().map(share -> share.amount().amount()).reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(sum).isEqualByComparingTo("10.00");
  }

  @Test
  void shouldRejectEmptyParticipants() {
    assertThatThrownBy(
            () ->
                SharedExpense.splitEqually(
                    ExpenseId.newId(), groupId, "Hotel", new Money(new BigDecimal("100.00"), "PEN"), userA, List.of(), Instant.now()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectDuplicateParticipants() {
    assertThatThrownBy(
            () ->
                SharedExpense.splitEqually(
                    ExpenseId.newId(),
                    groupId,
                    "Hotel",
                    new Money(new BigDecimal("100.00"), "PEN"),
                    userA,
                    List.of(userA, userB, userA),
                    Instant.now()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectNonPositiveAmount() {
    assertThatThrownBy(
            () ->
                SharedExpense.splitEqually(
                    ExpenseId.newId(), groupId, "Hotel", new Money(BigDecimal.ZERO, "PEN"), userA, List.of(userA, userB), Instant.now()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectBlankDescription() {
    assertThatThrownBy(
            () ->
                SharedExpense.splitEqually(
                    ExpenseId.newId(), groupId, "  ", new Money(new BigDecimal("10.00"), "PEN"), userA, List.of(userA, userB), Instant.now()))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
