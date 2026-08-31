package pe.smartcash.cash.analytics.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import pe.smartcash.cash.analytics.domain.services.CategoryBreakdownEntry;

/**
 * Read model puro: lee directo las tablas {@code transactions}/{@code categories}, sin pasar
 * por {@code TransactionRepository} ni por el agregado {@code Transaction} del contexto
 * transactions — este módulo no escribe nada ni protege invariantes, solo proyecta filas para
 * el dashboard, así que el rodeo por el agregado de escritura no aporta nada y sí cuesta
 * (carga entidades completas para sumarlas en memoria en vez de que lo haga Postgres).
 *
 * <p>{@code Instant} se convierte a {@link OffsetDateTime} justo antes de bindear: es el tipo
 * que el driver de PostgreSQL soporta sin ambigüedad contra columnas {@code timestamptz}
 * (ver columnas {@code created_at} en V1__init_schema.sql).
 *
 * <p>Pública (a diferencia del resto de los adaptadores de infraestructura del proyecto): no
 * hay un puerto de dominio intermedio que la oculte -- este read model no tiene agregado ni
 * invariantes que proteger, así que {@code DashboardQueryServiceImpl} la usa directo.
 */
@Repository
public class TransactionReadRepository {

  private final JdbcClient jdbcClient;

  TransactionReadRepository(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  /**
   * {@code COALESCE} a cero: sin transacciones en el rango, SUM natural de SQL da NULL.
   * {@code type} es {@code "EXPENSE"} o {@code "INCOME"} (mismo criterio que {@code status}
   * arriba: este read model no importa el enum del contexto transactions, solo pasa el valor
   * como string -- no hay agregado que proteger acá, ver javadoc de la clase).
   */
  public BigDecimal sumProcessedAmount(UUID userId, Instant from, Instant to, String type) {
    return jdbcClient
        .sql(
            """
            SELECT COALESCE(SUM(amount), 0)
            FROM transactions
            WHERE status = 'PROCESSED'
              AND type = :type
              AND internal_transfer = FALSE
              AND user_id = :userId
              AND created_at >= :from
              AND created_at < :to
            """)
        .param("userId", userId)
        .param("from", toOffsetDateTime(from))
        .param("to", toOffsetDateTime(to))
        .param("type", type)
        .query(BigDecimal.class)
        .single();
  }

  /**
   * El porcentaje se calcula acá, en SQL, con una window function sobre el propio resultado
   * agrupado ({@code SUM(SUM(amount)) OVER ()} = el total del mes, visible en cada fila sin
   * una segunda consulta ni un round-trip aparte): Postgres hace la división antes de que la
   * fila salga de la base de datos, nunca en memoria del lado de la aplicación.
   */
  public List<CategoryBreakdownEntry> findCategoryBreakdown(UUID userId, Instant from, Instant to) {
    return jdbcClient
        .sql(
            """
            SELECT c.id AS category_id,
                   c.display_name AS category_name,
                   SUM(t.amount) AS amount,
                   ROUND(SUM(t.amount) * 100.0 / SUM(SUM(t.amount)) OVER (), 2) AS percentage
            FROM transactions t
            JOIN categories c ON c.id = t.category_id
            WHERE t.status = 'PROCESSED'
              AND t.type = 'EXPENSE'
              AND t.internal_transfer = FALSE
              AND t.user_id = :userId
              AND t.created_at >= :from
              AND t.created_at < :to
            GROUP BY c.id, c.display_name
            ORDER BY amount DESC
            """)
        .param("userId", userId)
        .param("from", toOffsetDateTime(from))
        .param("to", toOffsetDateTime(to))
        .query(
            (rs, rowNum) ->
                new CategoryBreakdownEntry(
                    rs.getLong("category_id"), rs.getString("category_name"), rs.getBigDecimal("amount"), rs.getBigDecimal("percentage")))
        .list();
  }

  private static OffsetDateTime toOffsetDateTime(Instant instant) {
    return instant.atOffset(ZoneOffset.UTC);
  }
}
