package pe.smartcash.cash.groups.domain.services;

import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

/**
 * Servicio de dominio puro (sin I/O, sin anotaciones de framework -- mismo criterio que
 * {@code BankNotificationHeuristicParser}, wireado como {@code @Bean} en {@code
 * GroupsDomainConfig}): dado el saldo neto de cada miembro (positivo = le deben, negativo =
 * debe), calcula el mínimo número de transferencias para saldar el grupo entero.
 *
 * <p>Greedy: en cada paso empareja al mayor acreedor con el mayor deudor y salda el mínimo
 * entre ambos -- es el algoritmo estándar de "simplificación de deudas" (el mismo que usa
 * Splitwise), óptimo en la práctica para el tamaño de grupo típico de esta app.
 */
public final class DebtSimplifier {

  public List<SuggestedSettlement> simplify(Map<UserId, BigDecimal> netBalances) {
    List<Map.Entry<UserId, BigDecimal>> balances = new ArrayList<>();
    for (Map.Entry<UserId, BigDecimal> entry : netBalances.entrySet()) {
      if (entry.getValue().compareTo(BigDecimal.ZERO) != 0) {
        balances.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()));
      }
    }

    List<SuggestedSettlement> settlements = new ArrayList<>();
    while (true) {
      Map.Entry<UserId, BigDecimal> creditor =
          balances.stream().filter(e -> e.getValue().signum() > 0).max(Comparator.comparing(Map.Entry::getValue)).orElse(null);
      Map.Entry<UserId, BigDecimal> debtor =
          balances.stream().filter(e -> e.getValue().signum() < 0).min(Comparator.comparing(Map.Entry::getValue)).orElse(null);
      if (creditor == null || debtor == null) {
        break;
      }

      BigDecimal settleAmount = creditor.getValue().min(debtor.getValue().negate());
      settlements.add(new SuggestedSettlement(debtor.getKey(), creditor.getKey(), settleAmount));

      creditor.setValue(creditor.getValue().subtract(settleAmount));
      debtor.setValue(debtor.getValue().add(settleAmount));
      balances.removeIf(e -> e.getValue().compareTo(BigDecimal.ZERO) == 0);
    }
    return settlements;
  }
}
