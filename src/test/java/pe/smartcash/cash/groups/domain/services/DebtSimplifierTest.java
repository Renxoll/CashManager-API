package pe.smartcash.cash.groups.domain.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

class DebtSimplifierTest {

  private final DebtSimplifier simplifier = new DebtSimplifier();

  private final UserId a = UserId.of(UUID.randomUUID());
  private final UserId b = UserId.of(UUID.randomUUID());
  private final UserId c = UserId.of(UUID.randomUUID());

  @Test
  void shouldSuggestNothingWhenAlreadySettled() {
    List<SuggestedSettlement> result = simplifier.simplify(Map.of(a, BigDecimal.ZERO, b, BigDecimal.ZERO));

    assertThat(result).isEmpty();
  }

  @Test
  void shouldSuggestOneTransferBetweenTwoPeople() {
    List<SuggestedSettlement> result = simplifier.simplify(Map.of(a, new BigDecimal("50"), b, new BigDecimal("-50")));

    assertThat(result).containsExactly(new SuggestedSettlement(b, a, new BigDecimal("50")));
  }

  @Test
  void shouldMinimizeTransfersForAThreeWayImbalance() {
    // A pagó de más (le deben 30), B está justo, C debe 30 -- un solo pago de C a A basta,
    // sin pasar por B.
    List<SuggestedSettlement> result = simplifier.simplify(Map.of(a, new BigDecimal("30"), b, BigDecimal.ZERO, c, new BigDecimal("-30")));

    assertThat(result).containsExactly(new SuggestedSettlement(c, a, new BigDecimal("30")));
  }

  @Test
  void shouldHandleUnevenAmountsAcrossMultipleDebtors() {
    // A le deben 70; B debe 50, C debe 20 -- dos pagos: B->A y C->A.
    List<SuggestedSettlement> result =
        simplifier.simplify(Map.of(a, new BigDecimal("70"), b, new BigDecimal("-50"), c, new BigDecimal("-20")));

    assertThat(result).hasSize(2);
    BigDecimal totalToA = result.stream().filter(s -> s.to().equals(a)).map(SuggestedSettlement::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(totalToA).isEqualByComparingTo("70");
  }
}
