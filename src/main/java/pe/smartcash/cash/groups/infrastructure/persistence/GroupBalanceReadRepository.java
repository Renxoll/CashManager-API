package pe.smartcash.cash.groups.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;
import pe.smartcash.cash.groups.domain.services.CurrencyBalance;

/**
 * Read model puro (mismo criterio que {@code analytics.TransactionReadRepository}: sin
 * puerto de dominio intermedio, sin invariantes de escritura que proteger, solo proyecta
 * filas): calcula el saldo neto de cada miembro de un grupo, por moneda, sin conversión.
 * Positivo = le deben, negativo = debe. Dos fuentes, con signo opuesto entre sí:
 * {@code shared_expenses}/{@code expense_shares} (pagar un gasto SUMA, tu parte de un gasto
 * RESTA) y {@code settlements} (recibir un pago RESTA -- esa deuda ya se cobró -- y pagar
 * SUMA -- esa deuda ya se saldó).
 */
@Repository
public class GroupBalanceReadRepository {

  private final JdbcClient jdbcClient;

  GroupBalanceReadRepository(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  public Map<UserId, Map<String, BigDecimal>> computeNetBalances(GroupId groupId) {
    List<BalanceRow> rows =
        jdbcClient
            .sql(
                """
                SELECT user_id, currency, SUM(amount) AS balance
                FROM (
                  SELECT se.paid_by_user_id AS user_id, se.currency AS currency, se.amount AS amount
                    FROM shared_expenses se WHERE se.group_id = :groupId
                  UNION ALL
                  SELECT es.user_id AS user_id, se.currency AS currency, -es.amount AS amount
                    FROM expense_shares es JOIN shared_expenses se ON se.id = es.expense_id
                    WHERE se.group_id = :groupId
                  UNION ALL
                  -- Quien RECIBE el pago (to_user_id) ya cobró esa deuda -- resta de su saldo
                  -- (antes le debían, ahora ya no). Quien PAGA (from_user_id) saldó lo que
                  -- debía -- suma a su saldo. Invertido a propósito respecto al efecto de un
                  -- gasto compartido: acá el dinero sale del bolsillo de from_user directo
                  -- hacia to_user, fuera del libro de gastos del grupo.
                  SELECT s.to_user_id AS user_id, s.currency AS currency, -s.amount AS amount
                    FROM settlements s WHERE s.group_id = :groupId
                  UNION ALL
                  SELECT s.from_user_id AS user_id, s.currency AS currency, s.amount AS amount
                    FROM settlements s WHERE s.group_id = :groupId
                ) combined
                GROUP BY user_id, currency
                """)
            .param("groupId", groupId.value())
            .query((rs, rowNum) -> new BalanceRow(rs.getObject("user_id", UUID.class), rs.getString("currency"), rs.getBigDecimal("balance")))
            .list();

    Map<UserId, Map<String, BigDecimal>> balances = new LinkedHashMap<>();
    for (BalanceRow row : rows) {
      balances.computeIfAbsent(UserId.of(row.userId()), key -> new LinkedHashMap<>()).put(row.currency(), row.balance());
    }
    return balances;
  }

  /** Aplana el resultado de {@link #computeNetBalances} a los saldos de un solo miembro. */
  public List<CurrencyBalance> netBalancesFor(GroupId groupId, UserId userId) {
    Map<String, BigDecimal> byCurrency = computeNetBalances(groupId).getOrDefault(userId, Map.of());
    List<CurrencyBalance> balances = new ArrayList<>();
    byCurrency.forEach((currency, amount) -> balances.add(new CurrencyBalance(currency, amount)));
    return balances;
  }

  private record BalanceRow(UUID userId, String currency, BigDecimal balance) {}
}
