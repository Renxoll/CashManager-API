package pe.smartcash.cash.analytics.application.internal.queryservices;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import pe.smartcash.cash.analytics.domain.model.queries.FindMonthlySummaryQuery;
import pe.smartcash.cash.analytics.domain.services.CurrencySummary;
import pe.smartcash.cash.analytics.domain.services.DashboardQueryService;
import pe.smartcash.cash.analytics.domain.services.MonthlySummary;
import pe.smartcash.cash.analytics.infrastructure.persistence.TransactionReadRepository;

/**
 * "Mes en curso"/"mes anterior" se resuelven en UTC (mismo criterio que el resto de la app,
 * que persiste todo con {@code Clock.systemUTC()} -- ver {@code TransactionDomainConfig}): no
 * hay todavía un concepto de zona horaria por usuario, así que ambos lados quedan
 * consistentes aunque no reflejen exactamente la medianoche local del usuario en Perú.
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
    YearMonth currentMonth = YearMonth.from(clock.instant().atZone(ZoneOffset.UTC));
    YearMonth previousMonth = currentMonth.minusMonths(1);

    Instant previousMonthStart = startOf(previousMonth);
    Instant currentMonthStart = startOf(currentMonth);
    Instant nextMonthStart = startOf(currentMonth.plusMonths(1));

    Map<String, BigDecimal> currentExpenseByCurrency =
        transactionReadRepository.sumProcessedAmountByCurrency(query.userId(), currentMonthStart, nextMonthStart, "EXPENSE");
    Map<String, BigDecimal> previousExpenseByCurrency =
        transactionReadRepository.sumProcessedAmountByCurrency(query.userId(), previousMonthStart, currentMonthStart, "EXPENSE");
    Map<String, BigDecimal> currentIncomeByCurrency =
        transactionReadRepository.sumProcessedAmountByCurrency(query.userId(), currentMonthStart, nextMonthStart, "INCOME");

    // Unión de las 3 monedas con algo de movimiento en cualquiera de las dos ventanas -- si
    // no hay ninguna (usuario recién registrado, sin transacciones todavía), se muestra un
    // único resumen en PEN vacío en vez de una lista vacía sin nada que renderizar.
    Set<String> currencies = new LinkedHashSet<>();
    currencies.addAll(currentExpenseByCurrency.keySet());
    currencies.addAll(previousExpenseByCurrency.keySet());
    currencies.addAll(currentIncomeByCurrency.keySet());
    if (currencies.isEmpty()) {
      currencies.add(DEFAULT_CURRENCY);
    }

    var summaries =
        currencies.stream()
            // PEN primero (moneda principal de la app), el resto alfabético.
            .sorted(Comparator.comparing((String c) -> !c.equals(DEFAULT_CURRENCY)).thenComparing(Comparator.naturalOrder()))
            .map(
                currency ->
                    new CurrencySummary(
                        currency,
                        currentExpenseByCurrency.getOrDefault(currency, BigDecimal.ZERO),
                        previousExpenseByCurrency.getOrDefault(currency, BigDecimal.ZERO),
                        currentIncomeByCurrency.getOrDefault(currency, BigDecimal.ZERO),
                        transactionReadRepository.findCategoryBreakdown(query.userId(), currentMonthStart, nextMonthStart, currency)))
            .toList();

    return new MonthlySummary(summaries);
  }

  private static Instant startOf(YearMonth yearMonth) {
    return yearMonth.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
  }
}
