package pe.smartcash.cash.groups.domain.model.aggregates;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import pe.smartcash.cash.groups.domain.model.valueobjects.ExpenseId;
import pe.smartcash.cash.groups.domain.model.valueobjects.ExpenseShare;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.domain.model.valueobjects.Money;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

/**
 * Un gasto compartido dentro de un grupo, con sus {@link ExpenseShare} ya calculados al
 * crearse -- a diferencia de {@code Group}, este agregado SÍ carga sus hijos en memoria
 * (los shares) porque son indivisibles del gasto: no existen fuera de su ciclo de vida ni se
 * mutan de forma independiente (ver {@code SharedExpenseRepository}, que persiste ambas
 * tablas en el mismo {@code save}).
 */
public final class SharedExpense {

  private final ExpenseId id;
  private final GroupId groupId;
  private final String description;
  private final Money amount;
  private final UserId paidByUserId;
  private final List<ExpenseShare> shares;
  private final Instant createdAt;

  private SharedExpense(
      ExpenseId id,
      GroupId groupId,
      String description,
      Money amount,
      UserId paidByUserId,
      List<ExpenseShare> shares,
      Instant createdAt) {
    this.id = Objects.requireNonNull(id, "id");
    this.groupId = Objects.requireNonNull(groupId, "groupId");
    if (description == null || description.isBlank()) {
      throw new IllegalArgumentException("description no puede estar vacío");
    }
    this.description = description;
    this.amount = Objects.requireNonNull(amount, "amount");
    if (amount.amount().signum() <= 0) {
      throw new IllegalArgumentException("El monto del gasto debe ser positivo");
    }
    this.paidByUserId = Objects.requireNonNull(paidByUserId, "paidByUserId");
    this.shares = List.copyOf(shares);
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
  }

  /**
   * Divide {@code totalAmount} en partes iguales entre {@code participantUserIds}, truncando
   * a 2 decimales y repartiendo los centavos sobrantes de a uno entre los primeros
   * participantes de la lista (en el orden recibido) -- así la suma de los shares cierra
   * EXACTO contra el total, nunca queda un centavo "perdido" por el redondeo. Ejemplo: S/10
   * entre 3 personas → S/3.34, S/3.33, S/3.33.
   */
  public static SharedExpense splitEqually(
      ExpenseId id,
      GroupId groupId,
      String description,
      Money totalAmount,
      UserId paidByUserId,
      List<UserId> participantUserIds,
      Instant createdAt) {
    Objects.requireNonNull(totalAmount, "totalAmount");
    if (participantUserIds == null || participantUserIds.isEmpty()) {
      throw new IllegalArgumentException("Un gasto compartido necesita al menos un participante");
    }
    Set<UserId> distinct = new HashSet<>(participantUserIds);
    if (distinct.size() != participantUserIds.size()) {
      throw new IllegalArgumentException("Los participantes no pueden repetirse");
    }

    int n = participantUserIds.size();
    BigDecimal total = totalAmount.amount();
    BigDecimal base = total.divide(BigDecimal.valueOf(n), 2, RoundingMode.DOWN);
    BigDecimal remainder = total.subtract(base.multiply(BigDecimal.valueOf(n)));
    long extraCentsToDistribute = remainder.movePointRight(2).longValueExact();

    List<ExpenseShare> shares = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      BigDecimal shareAmount = i < extraCentsToDistribute ? base.add(new BigDecimal("0.01")) : base;
      shares.add(new ExpenseShare(participantUserIds.get(i), new Money(shareAmount, totalAmount.currency())));
    }

    return new SharedExpense(id, groupId, description, totalAmount, paidByUserId, shares, createdAt);
  }

  /** Reconstrucción desde persistencia: los shares ya vienen calculados, no se recalculan. */
  public static SharedExpense rehydrate(
      ExpenseId id,
      GroupId groupId,
      String description,
      Money amount,
      UserId paidByUserId,
      List<ExpenseShare> shares,
      Instant createdAt) {
    return new SharedExpense(id, groupId, description, amount, paidByUserId, shares, createdAt);
  }

  public ExpenseId id() {
    return id;
  }

  public GroupId groupId() {
    return groupId;
  }

  public String description() {
    return description;
  }

  public Money amount() {
    return amount;
  }

  public UserId paidByUserId() {
    return paidByUserId;
  }

  public List<ExpenseShare> shares() {
    return shares;
  }

  public Instant createdAt() {
    return createdAt;
  }
}
