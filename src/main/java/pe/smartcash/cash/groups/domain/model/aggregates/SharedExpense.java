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
 *
 * <p>Los campos corregibles ({@code description}, {@code amount}, {@code paidByUserId},
 * {@code shares}) son mutables vía {@link #revise}: sirve para arreglar un typo en el nombre
 * o un monto mal tipeado. {@code id}, {@code groupId} y {@code createdAt} nunca cambian.
 */
public final class SharedExpense {

  private final ExpenseId id;
  private final GroupId groupId;
  private String description;
  private Money amount;
  private UserId paidByUserId;
  private List<ExpenseShare> shares;
  private final Instant createdAt;
  private Instant updatedAt;

  private SharedExpense(
      ExpenseId id,
      GroupId groupId,
      String description,
      Money amount,
      UserId paidByUserId,
      List<ExpenseShare> shares,
      Instant createdAt,
      Instant updatedAt) {
    this.id = Objects.requireNonNull(id, "id");
    this.groupId = Objects.requireNonNull(groupId, "groupId");
    this.description = requireDescription(description);
    this.amount = requirePositive(amount);
    this.paidByUserId = Objects.requireNonNull(paidByUserId, "paidByUserId");
    this.shares = List.copyOf(shares);
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    this.updatedAt = updatedAt;
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
    return new SharedExpense(
        id,
        groupId,
        description,
        requirePositive(totalAmount),
        paidByUserId,
        splitEqually(totalAmount, participantUserIds),
        createdAt,
        null);
  }

  /** Reconstrucción desde persistencia: los shares ya vienen calculados, no se recalculan. */
  public static SharedExpense rehydrate(
      ExpenseId id,
      GroupId groupId,
      String description,
      Money amount,
      UserId paidByUserId,
      List<ExpenseShare> shares,
      Instant createdAt,
      Instant updatedAt) {
    return new SharedExpense(id, groupId, description, amount, paidByUserId, shares, createdAt, updatedAt);
  }

  /**
   * Corrige los datos del gasto (typo en la descripción, monto mal tipeado, pagador o
   * participantes equivocados) y recalcula los shares con el mismo reparto equitativo que
   * {@link #splitEqually}. Aplica las mismas invariantes que el alta.
   */
  public void revise(
      String description, Money amount, UserId paidByUserId, List<UserId> participantUserIds, Instant updatedAt) {
    this.description = requireDescription(description);
    this.amount = requirePositive(amount);
    this.paidByUserId = Objects.requireNonNull(paidByUserId, "paidByUserId");
    this.shares = splitEqually(amount, participantUserIds);
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
  }

  private static List<ExpenseShare> splitEqually(Money totalAmount, List<UserId> participantUserIds) {
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
    return shares;
  }

  private static String requireDescription(String description) {
    if (description == null || description.isBlank()) {
      throw new IllegalArgumentException("description no puede estar vacío");
    }
    return description;
  }

  private static Money requirePositive(Money amount) {
    Objects.requireNonNull(amount, "amount");
    if (amount.amount().signum() <= 0) {
      throw new IllegalArgumentException("El monto del gasto debe ser positivo");
    }
    return amount;
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

  public Instant updatedAt() {
    return updatedAt;
  }
}
