package pe.smartcash.cash.analytics.application.internal.queryservices;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import pe.smartcash.cash.analytics.domain.model.queries.FindMonthlySummaryQuery;
import pe.smartcash.cash.analytics.domain.services.CategoryBreakdownEntry;
import pe.smartcash.cash.analytics.domain.services.CurrencySummary;
import pe.smartcash.cash.analytics.domain.services.DashboardQueryService;
import pe.smartcash.cash.analytics.domain.services.MonthlySummary;
import pe.smartcash.cash.analytics.infrastructure.persistence.TransactionReadRepository;

/**
 * "Mes en curso"/"mes anterior" se resuelven en UTC (mismo criterio que el resto de la app).
 * El resumen es siempre de UN módulo: el que pida el cliente por {@code workspaceId}, o el
 * "General" del usuario si no manda ninguno. El desglose por categoría cambia de fuente según
 * el módulo (catálogo cerrado para el General, categorías propias para uno custom) -- ver
 * {@link TransactionReadRepository}.
 */
@Service
class DashboardQueryServiceImpl implements DashboardQueryService {

  private final TransactionReadRepository transactionReadRepository;
  private final Clock clock;

  DashboardQueryServiceImpl(TransactionReadRepository transactionReadRepository, Clock clock) {
    this.transactionReadRepository = transactionReadRepository;
    this.clock = clock;
  }

  private static final String DEFAULT_CURRENCY = "PEN";

  @Override
  public MonthlySummary handle(FindMonthlySummaryQuery query) {
    Optional<UUID> defaultWorkspaceId = transactionReadRepository.findDefaultWorkspaceId(query.userId());
    UUID workspaceId = query.workspaceId() != null ? query.workspaceId() : defaultWorkspaceId.orElse(null);
    if (workspaceId == null) {
      // Usuario sin módulo General todavía (no debería pasar tras la migración V14): un
      // único resumen vacío en PEN en vez de reventar.
      return new MonthlySummary(List.of(new CurrencySummary(DEFAULT_CURRENCY, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of())));
    }
    boolean isDefaultWorkspace = defaultWorkspaceId.map(workspaceId::equals).orElse(false);

    YearMonth currentMonth = YearMonth.from(clock.instant().atZone(ZoneOffset.UTC));
    YearMonth previousMonth = currentMonth.minusMonths(1);

    Instant previousMonthStart = startOf(previousMonth);
    Instant currentMonthStart = startOf(currentMonth);
    Instant nextMonthStart = startOf(currentMonth.plusMonths(1));

    Map<String, BigDecimal> currentExpenseByCurrency =
        transactionReadRepository.sumProcessedAmountByCurrency(query.userId(), workspaceId, currentMonthStart, nextMonthStart, "EXPENSE");
    Map<String, BigDecimal> previousExpenseByCurrency =
        transactionReadRepository.sumProcessedAmountByCurrency(query.userId(), workspaceId, previousMonthStart, currentMonthStart, "EXPENSE");
    Map<String, BigDecimal> currentIncomeByCurrency =
        transactionReadRepository.sumProcessedAmountByCurrency(query.userId(), workspaceId, currentMonthStart, nextMonthStart, "INCOME");

    Set<String> currencies = new LinkedHashSet<>();
    currencies.addAll(currentExpenseByCurrency.keySet());
    currencies.addAll(previousExpenseByCurrency.keySet());
    currencies.addAll(currentIncomeByCurrency.keySet());
    if (currencies.isEmpty()) {
      currencies.add(DEFAULT_CURRENCY);
    }

    UUID effectiveWorkspaceId = workspaceId;
    var summaries =
        currencies.stream()
            .sorted(Comparator.comparing((String c) -> !c.equals(DEFAULT_CURRENCY)).thenComparing(Comparator.naturalOrder()))
            .map(
                currency ->
                    new CurrencySummary(
                        currency,
                        currentExpenseByCurrency.getOrDefault(currency, BigDecimal.ZERO),
                        previousExpenseByCurrency.getOrDefault(currency, BigDecimal.ZERO),
                        currentIncomeByCurrency.getOrDefault(currency, BigDecimal.ZERO),
                        breakdown(query.userId(), effectiveWorkspaceId, isDefaultWorkspace, currentMonthStart, nextMonthStart, currency)))
            .toList();

    return new MonthlySummary(summaries);
  }

  private List<CategoryBreakdownEntry> breakdown(
      UUID userId, UUID workspaceId, boolean isDefaultWorkspace, Instant from, Instant to, String currency) {
    return isDefaultWorkspace
        ? transactionReadRepository.findCategoryBreakdown(userId, workspaceId, from, to, currency)
        : transactionReadRepository.findWorkspaceCategoryBreakdown(userId, workspaceId, from, to, currency);
  }

  private static Instant startOf(YearMonth yearMonth) {
    return yearMonth.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
  }
}
