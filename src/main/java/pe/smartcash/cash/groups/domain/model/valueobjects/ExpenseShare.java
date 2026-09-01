package pe.smartcash.cash.groups.domain.model.valueobjects;

import java.util.Objects;

/** La parte de un gasto compartido que le corresponde a un miembro puntual -- ver
 * {@code SharedExpense.splitEqually}. */
public record ExpenseShare(UserId userId, Money amount) {

  public ExpenseShare {
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(amount, "amount");
  }
}
