package pe.smartcash.cash.analytics.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import pe.smartcash.cash.analytics.domain.services.CategoryBreakdownEntry;

/**
 * Read model puro: lee directo las tablas {@code transactions} / {@code categories} /
 * {@code workspaces} / {@code workspace_categories}, sin pasar por los agregados de esos
 * contextos -- este módulo no escribe nada ni protege invariantes, solo proyecta filas para
 * el dashboard. Todas las consultas están acotadas por {@code workspace_id}: el dashboard
 * siempre muestra UN módulo a la vez (el "General" por defecto, o el que pida el cliente).
 *
 * <p>El desglose por categoría tiene dos formas según el módulo: para el "General" se une a
 * {@code categories} vía {@code category_id} (el catálogo cerrado que conoce el LLM); para
 * un módulo custom se une a {@code workspace_categories} vía {@code workspace_category_id}.
 *
 * <p>{@code Instant} se convierte a {@link OffsetDateTime} justo antes de bindear: es el tipo
 * que el driver de PostgreSQL soporta sin ambigüedad contra columnas {@code timestamptz}.
 */
@Repository
public class TransactionReadRepository {

  private final JdbcClient jdbcClient;

  TransactionReadRepository(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  /** Id del módulo "General" del usuario -- el que usa el dashboard cuando el cliente no pide uno. */
  public Optional<UUID> findDefaultWorkspaceId(UUID userId) {
    return jdbcClient
        .sql("SELECT id FROM workspaces WHERE owner_id = :userId AND is_default")
        .param("userId", userId)
        .query(UUID.class)
        .optional();
  }

  public Map<String, BigDecimal> sumProcessedAmountByCurrency(
      UUID userId, UUID workspaceId, Instant from, Instant to, String type) {
    List<Object[]> rows =
        jdbcClient
            .sql(
                """
                SELECT currency, SUM(amount) AS amount
                FROM transactions
                WHERE status = 'PROCESSED'
                  AND type = :type
                  AND internal_transfer = FALSE
                  AND user_id = :userId
                  AND workspace_id = :workspaceId
                  AND created_at >= :from
                  AND created_at < :to
                GROUP BY currency
                """)
            .param("userId", userId)
            .param("workspaceId", workspaceId)
            .param("from", toOffsetDateTime(from))
            .param("to", toOffsetDateTime(to))
            .param("type", type)
            .query((rs, rowNum) -> new Object[] {rs.getString("currency"), rs.getBigDecimal("amount")})
            .list();

    Map<String, BigDecimal> byCurrency = new LinkedHashMap<>();
    for (Object[] row : rows) {
      byCurrency.put((String) row[0], (BigDecimal) row[1]);
    }
    return byCurrency;
  }

  /** Desglose del módulo "General": categorías del catálogo cerrado ({@code categories}). */
  public List<CategoryBreakdownEntry> findCategoryBreakdown(UUID userId, UUID workspaceId, Instant from, Instant to, String currency) {
    return jdbcClient
        .sql(
            """
            SELECT c.id::text AS category_id,
                   c.display_name AS category_name,
                   SUM(t.amount) AS amount,
                   ROUND(SUM(t.amount) * 100.0 / SUM(SUM(t.amount)) OVER (), 2) AS percentage
            FROM transactions t
            JOIN categories c ON c.id = t.category_id
            WHERE t.status = 'PROCESSED'
              AND t.type = 'EXPENSE'
              AND t.internal_transfer = FALSE
              AND t.user_id = :userId
              AND t.workspace_id = :workspaceId
              AND t.currency = :currency
              AND t.created_at >= :from
              AND t.created_at < :to
            GROUP BY c.id, c.display_name
            ORDER BY amount DESC
            """)
        .param("userId", userId)
        .param("workspaceId", workspaceId)
        .param("from", toOffsetDateTime(from))
        .param("to", toOffsetDateTime(to))
        .param("currency", currency)
        .query(
            (rs, rowNum) ->
                new CategoryBreakdownEntry(
                    rs.getString("category_id"), rs.getString("category_name"), rs.getBigDecimal("amount"), rs.getBigDecimal("percentage")))
        .list();
  }

  /** Desglose de un módulo custom: categorías propias ({@code workspace_categories}). */
  public List<CategoryBreakdownEntry> findWorkspaceCategoryBreakdown(
      UUID userId, UUID workspaceId, Instant from, Instant to, String currency) {
    return jdbcClient
        .sql(
            """
            SELECT wc.id::text AS category_id,
                   wc.display_name AS category_name,
                   SUM(t.amount) AS amount,
                   ROUND(SUM(t.amount) * 100.0 / SUM(SUM(t.amount)) OVER (), 2) AS percentage
            FROM transactions t
            JOIN workspace_categories wc ON wc.id = t.workspace_category_id
            WHERE t.status = 'PROCESSED'
              AND t.type = 'EXPENSE'
              AND t.internal_transfer = FALSE
              AND t.user_id = :userId
              AND t.workspace_id = :workspaceId
              AND t.currency = :currency
              AND t.created_at >= :from
              AND t.created_at < :to
            GROUP BY wc.id, wc.display_name
            ORDER BY amount DESC
            """)
        .param("userId", userId)
        .param("workspaceId", workspaceId)
        .param("from", toOffsetDateTime(from))
        .param("to", toOffsetDateTime(to))
        .param("currency", currency)
        .query(
            (rs, rowNum) ->
                new CategoryBreakdownEntry(
                    rs.getString("category_id"), rs.getString("category_name"), rs.getBigDecimal("amount"), rs.getBigDecimal("percentage")))
        .list();
  }

  private static OffsetDateTime toOffsetDateTime(Instant instant) {
    return instant.atOffset(ZoneOffset.UTC);
  }
}
